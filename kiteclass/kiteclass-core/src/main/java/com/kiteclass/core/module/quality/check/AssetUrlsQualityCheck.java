package com.kiteclass.core.module.quality.check;

import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Check #3 — no broken asset URLs.
 *
 * <p>Scaffold: counts resources with null/blank storage_url as "broken". Real check
 * would HEAD-request each URL to verify 200 OK; deferred to follow-up that wires a
 * pooled HTTP client.
 *
 * <p>GAP-1362: ratio is computed via COUNT queries instead of {@code findAll()} +
 * in-memory stream so the check never materialises the full {@code BrandingResource}
 * set into heap.
 *
 * @since 3.25.0 (Wave 4 Sub-PR 4.5)
 */
@Component
@RequiredArgsConstructor
public class AssetUrlsQualityCheck implements QualityCheck {

    public static final String NAME = "asset-urls-reachable";

    private final BrandingResourceRepository repository;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Result run(FrontendInstance instance) {
        long total = repository.countActiveResources();
        if (total == 0) {
            return Result.pass(NAME, 100);
        }
        long broken = repository.countActiveResourcesMissingStorageUrl();
        int score = (int) (100L * (total - broken) / total);
        if (broken == 0) {
            return Result.pass(NAME, score);
        }
        return Result.fail(NAME, score, broken + " resource(s) missing storage_url");
    }
}
