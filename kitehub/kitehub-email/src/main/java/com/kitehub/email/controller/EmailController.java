package com.kitehub.email.controller;

import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.service.EmailSender;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Email service REST controller.
 *
 * <p>SLO Tier C (write — sync SES dispatch wraps async delivery).
 * See {@code documents/05-guides/api-performance-slo.md}.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/emails")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Internal email sending API (SMTP/SES)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-c", "controller", "email"})
public class EmailController {

    /**
     * Provider-selected email channel ({@code @Primary} {@link EmailSender} = the
     * {@code email.provider}-routing {@code EmailProviderRouter}). Injecting the
     * interface — NOT the concrete {@code SESEmailService} — is what makes
     * {@code email.provider=resend} actually reach Resend in production (GAP-788).
     */
    private final EmailSender emailSender;

    /**
     * Send email (internal API).
     *
     * Note: This API should only be called by other KiteHub services.
     * In production, use service-to-service authentication.
     *
     * @param request Email request
     * @return Email response
     */
    @PostMapping("/send")
    public ResponseEntity<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        log.info("Received email send request for: {}", request.getTo());

        EmailResponse response;

        if (request.getTemplateName() != null) {
            // Send templated email
            response = emailSender.sendTemplatedEmail(request);
        } else if (request.getHtmlBody() != null) {
            // Send plain HTML email
            response = emailSender.sendEmail(
                    request.getTo(),
                    request.getSubject(),
                    request.getHtmlBody()
            );
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(response);
    }
}
