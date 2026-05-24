package com.kitehub.email.service;

import com.kitehub.email.api.Tone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Render test cho class-rescheduled.html Thymeleaf template (Wave beta-readiness-4 Bucket D — GAP-291).
 *
 * <p>Verifies:
 * <ul>
 *   <li>Vietnamese sample data (Trần Thị Hồng, Trung tâm Sky Education, Lớp Anh ngữ 5A1) renders</li>
 *   <li>Parent persona greeting "Kính gửi quý phụ huynh," present inline (Bucket E refactor Phase 3)</li>
 *   <li>VN date format "Thứ Hai, 14/05/2026" present</li>
 *   <li>Reschedule reason category Vietnamese display name renders</li>
 *   <li>OPERATIONAL classification disclosure footer present (bypass marketing_consented)</li>
 *   <li>DPO contact link present (PDPL Art 14)</li>
 * </ul>
 *
 * <p>Uses standalone Thymeleaf engine — no Spring context required.
 *
 * @author KiteHub Email Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
class ClassRescheduledEmailTemplateTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        // Configure standalone Thymeleaf engine with classpath resolver pointing to templates/emails/
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        renderer = new EmailTemplateRenderer(engine);
    }

    @Test
    void renderClassRescheduledTemplate_shouldContainVnSampleDataAndGreeting() {
        // Given — VN sample fallback per VN-localization-audit-checklist
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("className", "Lớp Anh ngữ 5A1");
        ctx.put("tenantName", "Trung tâm Anh ngữ Sky Education");
        ctx.put("previousStartDateFormatted", "Thứ Hai, 14/05/2026");
        ctx.put("newStartDateFormatted", "Thứ Hai, 21/05/2026");
        ctx.put("previousEndDateFormatted", "Thứ Ba, 30/06/2026");
        ctx.put("newEndDateFormatted", "Thứ Ba, 07/07/2026");
        ctx.put("reasonDisplayName", "Giáo viên ốm/bận đột xuất");
        ctx.put("reasonNotes", "Cô giáo Trần Thị Hồng phụ trách lớp xin nghỉ ốm 1 tuần.");

        // When
        EmailTemplateRenderer.RenderedBodies result = renderer.render(
                "class-rescheduled", ctx, Tone.FORMAL_AUTHORITY);
        String html = result.getHtml();

        // Then — VN sample data present
        assertThat(html).contains("Lớp Anh ngữ 5A1");
        assertThat(html).contains("Trung tâm Anh ngữ Sky Education");

        // Greeting parent persona "Kính gửi quý phụ huynh," (very formal) — inline fallback Bucket D
        assertThat(html).contains("Kính gửi quý phụ huynh,");

        // VN long-date format — "Thứ Hai, DD/MM/YYYY"
        assertThat(html).contains("Thứ Hai, 14/05/2026");
        assertThat(html).contains("Thứ Hai, 21/05/2026");

        // Reason category Vietnamese display name + free-text notes
        assertThat(html).contains("Giáo viên ốm/bận đột xuất");
        assertThat(html).contains("Cô giáo Trần Thị Hồng phụ trách lớp xin nghỉ ốm 1 tuần.");

        // OPERATIONAL classification disclosure — bypass marketing_consented
        assertThat(html).contains("vận hành");
        assertThat(html).contains("marketing");

        // PDPL Art 14 DPO contact link
        assertThat(html).contains("dpo-contact");

        // No USD currency / English placeholder per VN-localization-audit-checklist
        assertThat(html).doesNotContain("$");
        assertThat(html).doesNotContain("John Doe");
        assertThat(html).doesNotContain("Example Center");
    }

    @Test
    void renderClassRescheduledTemplate_shouldOmitNotesWhenAbsent() {
        // Given — reason notes empty / null
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("className", "Lớp Toán 9B");
        ctx.put("tenantName", "Trung tâm Toán Quang Minh");
        ctx.put("previousStartDateFormatted", "Thứ Hai, 14/05/2026");
        ctx.put("newStartDateFormatted", "Thứ Hai, 21/05/2026");
        ctx.put("previousEndDateFormatted", "Thứ Hai, 30/06/2026");
        ctx.put("newEndDateFormatted", "Thứ Hai, 07/07/2026");
        ctx.put("reasonDisplayName", "Mất điện / mất Internet");
        ctx.put("reasonNotes", null);

        // When
        EmailTemplateRenderer.RenderedBodies result = renderer.render(
                "class-rescheduled", ctx, Tone.FORMAL_AUTHORITY);
        String html = result.getHtml();

        // Then — reason category still shows; notes block guarded by th:if
        assertThat(html).contains("Mất điện / mất Internet");
        assertThat(html).contains("Lớp Toán 9B");
        // Notes block conditionally rendered — should NOT contain placeholder text when null
        assertThat(html).doesNotContain("Cô giáo phụ trách");
    }

    @Test
    void formatVnLong_shouldFormatAsVietnameseWithWeekday() {
        // Thứ Hai (Monday) 14/05/2026
        assertThat(ClassRescheduledEmailService.formatVnLong(LocalDate.of(2026, 5, 14)))
                .isEqualTo("Thứ Năm, 14/05/2026"); // 2026-05-14 is actually Thursday
        // Verify all weekdays produce Vietnamese label
        assertThat(ClassRescheduledEmailService.formatVnLong(LocalDate.of(2026, 5, 18))) // Monday
                .startsWith("Thứ Hai, 18/05/2026");
        assertThat(ClassRescheduledEmailService.formatVnLong(LocalDate.of(2026, 5, 24))) // Sunday
                .startsWith("Chủ Nhật, 24/05/2026");
        // Null returns em-dash
        assertThat(ClassRescheduledEmailService.formatVnLong(null)).isEqualTo("—");
    }

    @Test
    void resolveReasonDisplay_shouldMapAll6ReasonCategories() {
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("GV_OM_BAN_DOT_XUAT"))
                .isEqualTo("Giáo viên ốm/bận đột xuất");
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("PHONG_HOC_KHONG_KHA_DUNG"))
                .isEqualTo("Phòng học không khả dụng");
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("MAT_DIEN_INTERNET"))
                .isEqualTo("Mất điện / mất Internet");
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("LE_TET_NGHI_CHINH_THUC"))
                .isEqualTo("Lễ Tết / nghỉ chính thức");
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("HOC_SINH_XIN_NGHI_TAP_THE"))
                .isEqualTo("Học sinh xin nghỉ tập thể");
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("LY_DO_KHAC"))
                .isEqualTo("Lý do khác");
        // Unknown enum defaults
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay("UNKNOWN"))
                .isEqualTo("Lý do khác");
        assertThat(ClassRescheduledEmailService.resolveReasonDisplay(null))
                .isEqualTo("Lý do khác");
    }
}
