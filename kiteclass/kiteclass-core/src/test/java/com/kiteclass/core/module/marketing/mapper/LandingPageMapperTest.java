package com.kiteclass.core.module.marketing.mapper;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.dto.response.LandingPageResponse;
import com.kiteclass.core.module.marketing.entity.LandingPage;
import com.kiteclass.core.testutil.LandingPageTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LandingPageMapper}.
 *
 * @since 2.10
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
class LandingPageMapperTest {

    @Autowired
    private LandingPageMapper landingPageMapper;

    @Test
    void toResponse_shouldMapAllFields() {
        // Given
        LandingPage landingPage = LandingPageTestDataBuilder.createDefaultLandingPage();

        // When
        LandingPageResponse response = landingPageMapper.toResponse(landingPage);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(landingPage.getId());
        assertThat(response.getCenterName()).isEqualTo(landingPage.getCenterName());
        assertThat(response.getZaloUrl()).isEqualTo(landingPage.getZaloUrl());
        assertThat(response.getHeroTitle()).isEqualTo(landingPage.getHeroTitle());
        assertThat(response.getHeroSubtitle()).isEqualTo(landingPage.getHeroSubtitle());
        assertThat(response.getHeroImageUrl()).isEqualTo(landingPage.getHeroImageUrl());
        // GAP-826: carousel banner list maps element-for-element.
        assertThat(response.getHeroImages()).isEqualTo(landingPage.getHeroImages());
        assertThat(response.getTeacherBio()).isEqualTo(landingPage.getTeacherBio());
        assertThat(response.getLogoUrl()).isEqualTo(landingPage.getLogoUrl());
        assertThat(response.getTagline()).isEqualTo(landingPage.getTagline());
        assertThat(response.getPrimaryColor()).isEqualTo(landingPage.getPrimaryColor());
        assertThat(response.getSecondaryColor()).isEqualTo(landingPage.getSecondaryColor());
        assertThat(response.getContactEmail()).isEqualTo(landingPage.getContactEmail());
        assertThat(response.getContactPhone()).isEqualTo(landingPage.getContactPhone());
        assertThat(response.getAddress()).isEqualTo(landingPage.getAddress());
        assertThat(response.getFacebookUrl()).isEqualTo(landingPage.getFacebookUrl());
        assertThat(response.getYoutubeUrl()).isEqualTo(landingPage.getYoutubeUrl());
        assertThat(response.getInstagramUrl()).isEqualTo(landingPage.getInstagramUrl());
    }

    @Test
    void updateEntity_shouldIgnoreNullFields() {
        // Given
        LandingPage landingPage = LandingPageTestDataBuilder.createDefaultLandingPage();
        String originalHeroTitle = landingPage.getHeroTitle();
        String originalTagline = landingPage.getTagline();

        UpdateLandingPageRequest request = UpdateLandingPageRequest.builder()
                .primaryColor("#FF0000")  // Update
                .secondaryColor(null)      // Ignore null
                .contactEmail("newemail@example.com")  // Update
                .heroTitle(null)           // Ignore null
                .build();

        // When
        landingPageMapper.updateEntity(landingPage, request);

        // Then
        assertThat(landingPage.getPrimaryColor()).isEqualTo("#FF0000");
        assertThat(landingPage.getContactEmail()).isEqualTo("newemail@example.com");
        // Null fields should not be updated
        assertThat(landingPage.getHeroTitle()).isEqualTo(originalHeroTitle);
        assertThat(landingPage.getTagline()).isEqualTo(originalTagline);
    }
}
