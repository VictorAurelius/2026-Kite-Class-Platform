package com.kitehub.subscription.tenant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Tenant slug normalization pipeline (GAP-535 Wave 77 Bucket D).
 *
 * <p>Wave 77 outside-in failure-mode matrix F2 (P0): P2 Center owner pastes
 * {@code 'Trường Mầm Non "Hoa Mai"'} from Word/Zalo into the signup form.
 * Without normalization the slug pipeline mishandles:
 * <ul>
 *   <li>Smart quotes {@code U+2018-U+201D} from Word/Zalo copy-paste</li>
 *   <li>Vietnamese diacritics ({@code ư}, {@code ạ}, {@code ầ}) without NFC normalize</li>
 *   <li>Slug collision with another tenant of the same display name</li>
 * </ul></p>
 *
 * <p>The pipeline below is intentionally deterministic + server-side only —
 * the client form may pre-fill a preview but the canonical slug is always
 * computed here. Collision recovery is performed by the caller (typically
 * {@code InstanceService}) by appending {@code -1}/{@code -2} suffixes until
 * the unique constraint stops firing, capped at 10 attempts.</p>
 *
 * <p>Worked examples (also covered by unit tests):
 * <table>
 *   <tr><th>Input</th><th>Output</th></tr>
 *   <tr><td>{@code Trường Mầm Non "Hoa Mai"}</td><td>{@code truong-mam-non-hoa-mai}</td></tr>
 *   <tr><td>{@code "Hoa Mai"} (smart quotes)</td><td>{@code hoa-mai}</td></tr>
 *   <tr><td>{@code àáảãạ}</td><td>{@code aaaaa}</td></tr>
 *   <tr><td>{@code ABC Học Đường &amp; Cộng Sự}</td><td>{@code abc-hoc-duong-cong-su}</td></tr>
 * </table></p>
 *
 * @since Wave 77 — GAP-535
 */
@Component
public class TenantSlugNormalizer {

    /** Hard cap on slug length (matches instances.slug VARCHAR(120)). */
    public static final int MAX_SLUG_LENGTH = 120;

    /**
     * Normalize a display name to its canonical slug form.
     *
     * <p>Pipeline:
     * <ol>
     *   <li>NFC normalize so combining characters fold into precomposed form</li>
     *   <li>Strip smart quotes {@code U+2018}, {@code U+2019}, {@code U+201C},
     *       {@code U+201D} (Word/Zalo copy-paste residue)</li>
     *   <li>Vietnamese {@code đ}/{@code Đ} → {@code d} BEFORE stripAccents (which
     *       doesn't handle stroke-bar chars)</li>
     *   <li>{@link StringUtils#stripAccents(String)} — flattens remaining
     *       diacritics ({@code ư}→{@code u}, {@code ạ}→{@code a}, {@code ầ}→{@code a})</li>
     *   <li>Lowercase</li>
     *   <li>Replace non-alphanumeric runs with single {@code -}</li>
     *   <li>Trim leading/trailing {@code -}</li>
     *   <li>Truncate to {@link #MAX_SLUG_LENGTH}</li>
     * </ol></p>
     *
     * @param displayName the user-supplied display name (organization_name)
     * @return the normalized slug, or empty string if input is null / contains
     *         no slug-able characters (caller must reject empty per AC)
     */
    public String normalize(String displayName) {
        if (displayName == null) {
            return "";
        }

        // Step 1: NFC normalize (compose canonical form so subsequent codepoint
        // strips operate on the precomposed character class).
        String s = Normalizer.normalize(displayName, Normalizer.Form.NFC);

        // Step 2: Strip smart quotes (Word/Zalo copy-paste artefacts). Range
        // covers U+2018 ‘ , U+2019 ’ , U+201A ‚ , U+201B ‛ , U+201C “ ,
        // U+201D ” , U+201E „ , U+201F ‟ — all variants the user may paste.
        s = s.replaceAll("[‘-‟]", "");

        // Step 3: Vietnamese stroke-bar consonants — stripAccents leaves these
        // alone, so handle explicitly.
        s = s.replace('đ', 'd').replace('Đ', 'd');

        // Step 4: Flatten remaining diacritics via Apache Commons.
        s = StringUtils.stripAccents(s);

        // Step 5-7: lowercase + non-alphanumeric → dash + trim.
        s = s.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");

        // Step 8: truncate to column max.
        if (s.length() > MAX_SLUG_LENGTH) {
            s = s.substring(0, MAX_SLUG_LENGTH);
            // After hard truncate, trim again in case the cut landed inside a dash run.
            s = s.replaceAll("-+$", "");
        }

        return s;
    }

    /**
     * Build the n-th collision-recovery candidate by appending {@code -<n>}.
     *
     * <p>Caller uses this to probe the unique index. Suffix is applied
     * AFTER truncation so the recovered slug stays within column max — if
     * appending would exceed {@link #MAX_SLUG_LENGTH}, trim the base before
     * appending the suffix.</p>
     *
     * @param base the normalized base slug (output of {@link #normalize})
     * @param suffix the collision counter (must be &ge; 1)
     * @return base with {@code -<suffix>} appended, within length cap
     */
    public String withCollisionSuffix(String base, int suffix) {
        if (suffix < 1) {
            throw new IllegalArgumentException("collision suffix must be >= 1, got " + suffix);
        }
        String tail = "-" + suffix;
        if (base.length() + tail.length() <= MAX_SLUG_LENGTH) {
            return base + tail;
        }
        // Reserve room for the tail by trimming base.
        int allowedBase = MAX_SLUG_LENGTH - tail.length();
        if (allowedBase < 1) {
            // pathological — tail alone is too long; should never happen for sane suffix counts.
            throw new IllegalStateException(
                    "collision suffix '" + tail + "' would exhaust slug length cap " + MAX_SLUG_LENGTH);
        }
        String trimmed = base.substring(0, allowedBase).replaceAll("-+$", "");
        return trimmed + tail;
    }
}
