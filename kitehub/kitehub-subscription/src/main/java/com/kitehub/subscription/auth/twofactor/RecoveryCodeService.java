package com.kitehub.subscription.auth.twofactor;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates and verifies single-use recovery codes for 2FA (GAP-516).
 *
 * <p>Plain codes use an 8-char alphabet that excludes visually-confusing
 * characters ({@code 0/o/1/l}, per BR-AUTH-007). Each code is stored as a
 * bcrypt hash; verification iterates the user's still-active codes and matches
 * the candidate against each hash. On match, the row is marked used and a flag
 * is returned to the caller.</p>
 *
 * @since 1.0.0 (Wave 72b GAP-516)
 */
@Service
@RequiredArgsConstructor
public class RecoveryCodeService {

    /** Alphabet excludes 0/o/1/l per BR-AUTH-007. 32 chars total. */
    private static final char[] ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789".toCharArray();
    /** Number of codes emitted per enrollment / regenerate. */
    public static final int CODES_PER_USER = 10;
    /** Length of each plain-text code. */
    public static final int CODE_LENGTH = 8;

    private final RecoveryCodeRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom rng = new SecureRandom();

    /**
     * Generate ten plaintext codes + their bcrypt hashes (caller persists the
     * hashes and returns the plaintext to the user ONCE).
     */
    public List<GeneratedCode> generate() {
        List<GeneratedCode> out = new ArrayList<>(CODES_PER_USER);
        for (int i = 0; i < CODES_PER_USER; i++) {
            String plain = randomCode();
            out.add(new GeneratedCode(plain, passwordEncoder.encode(plain)));
        }
        return out;
    }

    /**
     * Issue + persist a fresh batch of {@value #CODES_PER_USER} codes for
     * {@code userId}. Caller is responsible for invalidating any previous codes
     * before calling (see {@link #regenerate(UUID)}).
     *
     * @return the plaintext codes — the only chance to surface them.
     */
    @Transactional
    public List<String> issueForUser(UUID userId) {
        List<GeneratedCode> generated = generate();
        List<RecoveryCode> rows = new ArrayList<>(generated.size());
        for (GeneratedCode gc : generated) {
            rows.add(RecoveryCode.builder()
                .userId(userId)
                .codeHash(gc.hash())
                .createdAt(LocalDateTime.now())
                .build());
        }
        repository.saveAll(rows);
        return generated.stream().map(GeneratedCode::plain).toList();
    }

    /**
     * Invalidate every active code for the user, then issue a new batch.
     * Returns the plaintext for the FE to display once.
     */
    @Transactional
    public RegenerateResult regenerate(UUID userId) {
        int invalidated = repository.markAllUsed(userId, LocalDateTime.now());
        List<String> plain = issueForUser(userId);
        return new RegenerateResult(plain, invalidated);
    }

    /**
     * Attempt to consume a candidate plaintext recovery code. If a still-active
     * row matches the bcrypt hash, it is marked used and the result is success.
     *
     * @return success flag + remaining unused-code count after consumption.
     */
    @Transactional
    public VerifyResult verifyAndConsume(UUID userId, String candidate) {
        List<RecoveryCode> active = repository.findByUserIdAndUsedAtIsNullOrderByIdAsc(userId);
        for (RecoveryCode rc : active) {
            if (passwordEncoder.matches(candidate, rc.getCodeHash())) {
                rc.setUsedAt(LocalDateTime.now());
                repository.save(rc);
                long remaining = repository.countByUserIdAndUsedAtIsNull(userId);
                return new VerifyResult(true, remaining);
            }
        }
        long remaining = repository.countByUserIdAndUsedAtIsNull(userId);
        return new VerifyResult(false, remaining);
    }

    private String randomCode() {
        char[] buf = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            buf[i] = ALPHABET[rng.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }

    public record GeneratedCode(String plain, String hash) { }
    public record RegenerateResult(List<String> plainCodes, int invalidatedCount) { }
    public record VerifyResult(boolean success, long codesRemaining) { }
}
