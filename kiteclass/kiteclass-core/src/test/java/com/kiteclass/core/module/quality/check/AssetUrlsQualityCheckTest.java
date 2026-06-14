package com.kiteclass.core.module.quality.check;

import com.kiteclass.core.module.branding.repository.BrandingResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AssetUrlsQualityCheck} (GAP-1362).
 *
 * <p>Verifies the check computes its score from COUNT queries and never falls back to a
 * full {@code findAll()} materialisation.
 */
@ExtendWith(MockitoExtension.class)
class AssetUrlsQualityCheckTest {

    @Mock
    private BrandingResourceRepository repository;

    @InjectMocks
    private AssetUrlsQualityCheck check;

    @Test
    void noResources_passesWithFullScore() {
        when(repository.countActiveResources()).thenReturn(0L);

        AssetUrlsQualityCheck.Result result = check.run(null);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getScore()).isEqualTo(100);
        verify(repository, never()).findAll();
    }

    @Test
    void allResourcesHaveStorageUrl_passes() {
        when(repository.countActiveResources()).thenReturn(4L);
        when(repository.countActiveResourcesMissingStorageUrl()).thenReturn(0L);

        AssetUrlsQualityCheck.Result result = check.run(null);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getScore()).isEqualTo(100);
        verify(repository, never()).findAll();
    }

    @Test
    void someResourcesMissingStorageUrl_failsWithProportionalScore() {
        when(repository.countActiveResources()).thenReturn(4L);
        when(repository.countActiveResourcesMissingStorageUrl()).thenReturn(1L);

        AssetUrlsQualityCheck.Result result = check.run(null);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getScore()).isEqualTo(75); // 100 * (4-1) / 4
        assertThat(result.getDetail()).contains("1 resource(s) missing storage_url");
        verify(repository, never()).findAll();
    }
}
