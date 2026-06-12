package com.kiteclass.core.module.marketing.service.impl;

import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.module.marketing.mapper.LandingPageMapper;
import com.kiteclass.core.module.marketing.repository.LandingPageRepository;
import com.kiteclass.core.module.marketing.service.LandingPageContentSanitizer;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link LandingPageServiceImpl#applyDeployedBranding} (GAP-1213).
 *
 * <p>Verifies the consumer-facing apply path: idempotency on {@code brandingVersion} (stale
 * event skipped) + theme colours/logo applied + version bumped when newer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LandingPageServiceImpl.applyDeployedBranding")
class LandingPageApplyDeployedBrandingTest {

    @Mock
    private LandingPageRepository landingPageRepository;
    @Mock
    private LandingPageMapper landingPageMapper;
    @Mock
    private BrandingRepository brandingRepository;
    @Mock
    private LandingPageContentSanitizer contentSanitizer;

    private LandingPageServiceImpl service() {
        return new LandingPageServiceImpl(
                landingPageRepository, landingPageMapper, brandingRepository, contentSanitizer, null);
    }

    private LandingPage existing(UUID tenant, int version) {
        LandingPage lp = new LandingPage();
        lp.setInstanceId(tenant);
        lp.setBrandingVersion(version);
        return lp;
    }

    @Test
    @DisplayName("newer version applies the theme colours + logo + bumps version")
    void appliesNewerVersion() {
        UUID tenant = UUID.randomUUID();
        LandingPage lp = existing(tenant, 0);
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenant)).thenReturn(Optional.of(lp));
        when(landingPageRepository.save(any(LandingPage.class))).thenAnswer(i -> i.getArgument(0));

        boolean changed = service().applyDeployedBranding(
                tenant, "#112233", "#445566", "https://cdn/logo.svg", 1);

        assertThat(changed).isTrue();
        assertThat(lp.getPrimaryColor()).isEqualTo("#112233");
        assertThat(lp.getSecondaryColor()).isEqualTo("#445566");
        assertThat(lp.getLogoUrl()).isEqualTo("https://cdn/logo.svg");
        assertThat(lp.getBrandingVersion()).isEqualTo(1);
        verify(landingPageRepository).save(lp);
    }

    @Test
    @DisplayName("stale/duplicate version is skipped — no save")
    void skipsStaleVersion() {
        UUID tenant = UUID.randomUUID();
        LandingPage lp = existing(tenant, 5);
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenant)).thenReturn(Optional.of(lp));

        boolean changed = service().applyDeployedBranding(
                tenant, "#000000", "#111111", "https://cdn/x.svg", 5);

        assertThat(changed).isFalse();
        assertThat(lp.getPrimaryColor()).isEqualTo("#3B82F6"); // entity default, unchanged
        verify(landingPageRepository, never()).save(any());
    }

    @Test
    @DisplayName("invalid hex colour is ignored (not persisted)")
    void ignoresInvalidHex() {
        UUID tenant = UUID.randomUUID();
        LandingPage lp = existing(tenant, 0);
        when(landingPageRepository.findByInstanceIdAndDeletedFalse(tenant)).thenReturn(Optional.of(lp));
        when(landingPageRepository.save(any(LandingPage.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(brandingRepository.findByInstanceIdAndDeletedFalse(any())).thenReturn(Optional.empty());

        service().applyDeployedBranding(tenant, "not-a-hex", "#445566", null, 1);

        assertThat(lp.getPrimaryColor()).isEqualTo("#3B82F6"); // unchanged (invalid ignored)
        assertThat(lp.getSecondaryColor()).isEqualTo("#445566"); // valid applied
    }
}
