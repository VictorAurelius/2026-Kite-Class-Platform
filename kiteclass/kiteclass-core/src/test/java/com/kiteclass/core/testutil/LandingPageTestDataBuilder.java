package com.kiteclass.core.testutil;

import com.kiteclass.core.module.marketing.dto.request.UpdateLandingPageRequest;
import com.kiteclass.core.module.marketing.entity.LandingPage;

import java.util.List;
import java.util.UUID;

/**
 * Test data builder for LandingPage-related objects.
 *
 * @since 2.10
 */
public class LandingPageTestDataBuilder {

    /**
     * Creates a default LandingPage entity for testing.
     *
     * @return LandingPage with default test data
     */
    public static LandingPage createDefaultLandingPage() {
        LandingPage landingPage = new LandingPage();
        landingPage.setId(1L);
        landingPage.setInstanceId(UUID.randomUUID());
        landingPage.setCenterName("Trung tâm Anh ngữ Sky Education");
        landingPage.setZaloUrl("https://zalo.me/0901234567");
        landingPage.setHeroTitle("Welcome to Test Learning Center");
        landingPage.setHeroSubtitle("Quality education for everyone");
        landingPage.setHeroImageUrl("https://example.com/hero.jpg");
        landingPage.setHeroImages(List.of("https://example.com/hero-1.jpg", "https://example.com/hero-2.jpg"));
        landingPage.setTeacherBio("Experienced teacher with 10+ years");
        landingPage.setLogoUrl("https://example.com/logo.png");
        landingPage.setTagline("Learn, Grow, Succeed");
        landingPage.setPrimaryColor("#3B82F6");
        landingPage.setSecondaryColor("#8B5CF6");
        landingPage.setContactEmail("contact@example.com");
        landingPage.setContactPhone("0123456789");
        landingPage.setAddress("123 Test Street, Hanoi");
        landingPage.setFacebookUrl("https://facebook.com/test");
        landingPage.setYoutubeUrl("https://youtube.com/test");
        landingPage.setInstagramUrl("https://instagram.com/test");
        landingPage.setDeleted(false);
        return landingPage;
    }

    /**
     * Creates a LandingPage with custom instance ID.
     *
     * @param instanceId the tenant instance ID
     * @return LandingPage with specified instance ID
     */
    public static LandingPage createLandingPageWithInstanceId(UUID instanceId) {
        LandingPage landingPage = createDefaultLandingPage();
        landingPage.setInstanceId(instanceId);
        return landingPage;
    }

    /**
     * Creates a default UpdateLandingPageRequest for testing.
     *
     * @return UpdateLandingPageRequest with default test data
     */
    public static UpdateLandingPageRequest createDefaultUpdateRequest() {
        return UpdateLandingPageRequest.builder()
                .heroTitle("Updated Welcome Title")
                .heroSubtitle("Updated subtitle")
                .primaryColor("#FF5733")
                .secondaryColor("#C70039")
                .contactEmail("updated@example.com")
                .contactPhone("0987654321")
                .build();
    }
}
