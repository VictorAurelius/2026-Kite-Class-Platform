package com.kiteclass.core.wave02;

import com.kiteclass.core.module.academicyear.entity.AcademicYear;
import com.kiteclass.core.module.academicyear.entity.AcademicYearStatus;
import com.kiteclass.core.module.branding.classifier.AIFallbackClassifier;
import com.kiteclass.core.module.branding.classifier.ClassificationContext;
import com.kiteclass.core.module.branding.classifier.CustomAIRequestClassifier;
import com.kiteclass.core.module.branding.classifier.DefaultTemplateClassifier;
import com.kiteclass.core.module.branding.classifier.ResourceRequest;
import com.kiteclass.core.module.branding.classifier.StaticAssetClassifier;
import com.kiteclass.core.module.branding.classifier.TemplateMatchClassifier;
import com.kiteclass.core.module.branding.entity.ResourceCategory;
import com.kiteclass.core.module.branding.entity.ResourceType;
import com.kiteclass.core.module.branding.service.ResourceRoutingService;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.k12.entity.HomeroomClass;
import com.kiteclass.core.module.role.entity.Permission;
import com.kiteclass.core.module.role.entity.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 2 cross-module smoke test — exercises entities from all 5 sub-PRs together
 * (academic year / K-12 / role / instance lifecycle / resource classification) to
 * prove the data model composes cleanly. No DB / no Spring context — verifies
 * domain-level wiring only.
 *
 * @since Wave 2 Sub-PR 2.7
 */
class Wave02DataModelIntegrationTest {

    @Test
    void k12_scenario_composes_across_modules() {
        // 1) Academic year (Sub-PR 2.2)
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 5, 31))
                .status(AcademicYearStatus.CURRENT)
                .build();

        // 2) HomeroomClass references academic year (Sub-PR 2.3)
        HomeroomClass tenA1 = HomeroomClass.builder()
                .academicYear(year)
                .grade("10")
                .section("A1")
                .homeroomTeacherId(101L)
                .capacity(40)
                .currentEnrolled(35)
                .build();

        // 3) Role hierarchy for GVCN permission (Sub-PR 2.4)
        Permission gradeEdit = Permission.builder().name("GRADE_EDIT_OWN").build();
        Role gvcn = Role.builder()
                .name("HOMEROOM_TEACHER")
                .level(5)
                .permissions(new HashSet<>(Set.of(gradeEdit)))
                .build();

        assertThat(tenA1.getAcademicYear()).isEqualTo(year);
        assertThat(gvcn.hasPermission("GRADE_EDIT_OWN")).isTrue();
        assertThat(gvcn.hasPermission("UNKNOWN")).isFalse();
    }

    @Test
    void provisioning_and_classification_run_together() {
        // Tenant signup happy path
        // 4) Instance lifecycle (Sub-PR 2.5)
        FrontendInstance instance = FrontendInstance.builder()
                .tenantSlug("t-abc")
                .slug("acme-school")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0)
                .brandingVersion(0)
                .build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);

        // 5) Resource classifier chain (Sub-PR 2.6) — used during GENERATING state
        ResourceRoutingService routing = new ResourceRoutingService(
                List.of(
                        new StaticAssetClassifier(),
                        new CustomAIRequestClassifier(),
                        new TemplateMatchClassifier(),
                        new AIFallbackClassifier(),
                        new DefaultTemplateClassifier()
                ),
                List.of(),
                null);
        ResourceCategory cat = routing.classify(
                ResourceRequest.builder().type(ResourceType.BANNER).customRequested(false).build(),
                ClassificationContext.builder()
                        .hasStaticAsset(false)
                        .hasMatchingTemplate(true)
                        .hasAIQuota(true)
                        .build()
        );

        // Template-first philosophy (BR-RES-005)
        assertThat(cat).isEqualTo(ResourceCategory.TEMPLATE);

        // Complete provisioning
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);
        assertThat(instance.getStatus()).isEqualTo(FrontendInstanceStatus.DEPLOYED);
        assertThat(instance.getBrandingVersion()).isEqualTo(1);
    }

    @Test
    void full_wave2_scenario_smoke() {
        // End-to-end: K-12 tenant's frontend is provisioned and a banner is routed.
        AcademicYear year = AcademicYear.builder()
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 5, 31))
                .status(AcademicYearStatus.CURRENT)
                .build();

        HomeroomClass tenA1 = HomeroomClass.builder()
                .academicYear(year).grade("10").section("A1").capacity(40).currentEnrolled(35).build();

        Role principal = Role.builder().name("PRINCIPAL").level(2).build();
        Role gvcn = Role.builder().name("HOMEROOM_TEACHER").level(5).parent(principal).build();

        FrontendInstance instance = FrontendInstance.builder()
                .tenantSlug("t-abc").slug("acme")
                .status(FrontendInstanceStatus.NOT_STARTED)
                .retryCount(0).brandingVersion(0).build();
        instance.transitionTo(FrontendInstanceStatus.INITIALIZING);
        instance.transitionTo(FrontendInstanceStatus.GENERATING);
        instance.transitionTo(FrontendInstanceStatus.DEPLOYED);

        assertThat(year.getName()).isEqualTo("2026-2027");
        assertThat(tenA1.getAcademicYear().getStatus()).isEqualTo(AcademicYearStatus.CURRENT);
        assertThat(gvcn.getParent()).isEqualTo(principal);
        assertThat(instance.getStatus()).isEqualTo(FrontendInstanceStatus.DEPLOYED);
    }
}
