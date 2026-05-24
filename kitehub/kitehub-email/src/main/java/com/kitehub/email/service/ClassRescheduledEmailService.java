package com.kitehub.email.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.email.api.Tone;
import com.kitehub.email.dto.EmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders + sends the class-rescheduled email (Wave beta-readiness-4 Bucket D — GAP-291).
 *
 * <p>Listens on {@code class.rescheduled.email.queue} (forwarded by
 * {@code ClassRescheduledEmailConsumer} in kiteclass-core when feature flag
 * {@code kite.class.reschedule.notify.enabled=true}).
 *
 * <p>Per cross-bucket LOCKED decision §3.6:
 * <ul>
 *   <li>Notification classification = OPERATIONAL — bypasses {@code marketing_consented} gate</li>
 *   <li>Greeting fallback inline: "Kính gửi quý phụ huynh," (parent persona, very formal)</li>
 *   <li>Bucket E will refactor to consume {@code _shared/persona-tone} partial in Phase 3</li>
 * </ul>
 *
 * <p>Activates only when {@code kite.class.reschedule.notify.enabled=true} AND a
 * {@link NotificationDispatcher} bean is present in the application context (for
 * tests this is a mock; for production this is the Resend/SES provider).
 *
 * @author KiteHub Email Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "kite.class.reschedule.notify.enabled",
        havingValue = "true"
)
public class ClassRescheduledEmailService {

    /** Vietnamese long-form date format: "Thứ Hai, 14/05/2026". */
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;

    /**
     * Optional dispatcher — Spring injects null in test environments where no
     * concrete sender (Resend/SES) is wired. Tests assert {@link #sendClassRescheduledEmail}
     * was invoked; production wires the active provider.
     */
    @Autowired(required = false)
    private NotificationDispatcher dispatcher;

    @RabbitListener(queues = "class.rescheduled.email.queue")
    public void handle(String payloadJson) {
        try {
            ClassRescheduledPayload event = objectMapper.readValue(payloadJson, ClassRescheduledPayload.class);
            sendClassRescheduledEmail(event);
        } catch (Exception ex) {
            log.error("Failed to process class.rescheduled.email message: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Render the Thymeleaf template + dispatch to each enrolled parent.
     * Recipient resolution responsibility lives upstream (event carries parentUserIds).
     *
     * @param event deserialized payload from {@code class.rescheduled.email.queue}
     * @return list of dispatch responses (one per recipient); empty when dispatcher absent
     */
    public List<EmailResponse> sendClassRescheduledEmail(ClassRescheduledPayload event) {
        log.info("Sending class-rescheduled email: classId={}, recipientCount={}",
                event.classId(),
                event.parentEmails() != null ? event.parentEmails().size() : 0);

        Map<String, Object> ctx = buildTemplateContext(event);
        EmailTemplateRenderer.RenderedBodies bodies = templateRenderer.render(
                "class-rescheduled", ctx, Tone.FORMAL_AUTHORITY);

        String subject = String.format("Thông báo đổi lịch lớp %s — %s",
                safe(event.className()), safe(event.tenantName()));

        if (dispatcher == null) {
            log.warn("[CLASS-RESCHEDULED] dispatcher bean absent — rendered body length HTML={} TEXT={}; "
                            + "production should wire Resend/SES provider",
                    bodies.getHtml().length(), bodies.getText().length());
            return List.of();
        }

        // Recipient resolution deferred to upstream (kiteclass-core Phase 1.5+).
        // For Phase 1 BETA, payload carries parent emails resolved at consume-time.
        return dispatcher.dispatchAll(event.parentEmails(), subject, bodies.getHtml(), bodies.getText());
    }

    private Map<String, Object> buildTemplateContext(ClassRescheduledPayload event) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("className", safe(event.className()));
        ctx.put("tenantName", event.tenantName() != null
                ? event.tenantName()
                : "Trung tâm Anh ngữ Sky Education"); // VN sample fallback
        ctx.put("previousStartDateFormatted", formatVnLong(event.previousStartDate()));
        ctx.put("newStartDateFormatted", formatVnLong(event.newStartDate()));
        ctx.put("previousEndDateFormatted", formatVnLong(event.previousEndDate()));
        ctx.put("newEndDateFormatted", formatVnLong(event.newEndDate()));
        ctx.put("reasonDisplayName", resolveReasonDisplay(event.reasonCategory()));
        ctx.put("reasonNotes", event.reasonNotes());
        return ctx;
    }

    /**
     * Format as Vietnamese long-form date e.g. "Thứ Hai, 14/05/2026".
     */
    static String formatVnLong(LocalDate date) {
        if (date == null) {
            return "—";
        }
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi", "VN"));
        // JDK locale may return English fallback; map to Vietnamese explicitly for stability.
        String vnDay = switch (date.getDayOfWeek()) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
        return vnDay + ", " + date.format(LONG_DATE) + (dayName.isBlank() ? "" : "");
    }

    /**
     * Resolve Vietnamese display name for a reschedule reason category.
     * Defaults to "Lý do khác" if unknown enum name supplied.
     */
    static String resolveReasonDisplay(String reasonCategory) {
        if (reasonCategory == null) {
            return "Lý do khác";
        }
        return switch (reasonCategory) {
            case "GV_OM_BAN_DOT_XUAT" -> "Giáo viên ốm/bận đột xuất";
            case "PHONG_HOC_KHONG_KHA_DUNG" -> "Phòng học không khả dụng";
            case "MAT_DIEN_INTERNET" -> "Mất điện / mất Internet";
            case "LE_TET_NGHI_CHINH_THUC" -> "Lễ Tết / nghỉ chính thức";
            case "HOC_SINH_XIN_NGHI_TAP_THE" -> "Học sinh xin nghỉ tập thể";
            default -> "Lý do khác";
        };
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * Payload schema matching {@code ClassRescheduledEvent} from kiteclass-core
     * (decoupled — no maven dependency cross-module).
     *
     * <p>Includes {@code parentEmails} field populated at consume-time from
     * upstream parent-user-IDs (kiteclass-core Phase 1.5+).
     */
    public record ClassRescheduledPayload(
            @JsonProperty("classId") Long classId,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("tenantName") String tenantName,
            @JsonProperty("className") String className,
            @JsonProperty("previousStartDate") LocalDate previousStartDate,
            @JsonProperty("newStartDate") LocalDate newStartDate,
            @JsonProperty("previousEndDate") LocalDate previousEndDate,
            @JsonProperty("newEndDate") LocalDate newEndDate,
            @JsonProperty("reasonCategory") String reasonCategory,
            @JsonProperty("reasonNotes") String reasonNotes,
            @JsonProperty("parentEmails") List<String> parentEmails
    ) {
    }

    /**
     * Minimal dispatcher contract — production wires Resend/SES; tests mock this.
     * Operational classification means each recipient receives the email regardless
     * of {@code marketing_consented} flag.
     */
    public interface NotificationDispatcher {
        List<EmailResponse> dispatchAll(List<String> recipients, String subject, String html, String text);
    }
}
