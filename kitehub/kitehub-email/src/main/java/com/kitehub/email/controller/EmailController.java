package com.kitehub.email.controller;

import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.service.SESEmailService;
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
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/emails")
@RequiredArgsConstructor
public class EmailController {

    private final SESEmailService sesEmailService;

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
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody EmailRequest request) {
        log.info("Received email send request for: {}", request.getTo());

        EmailResponse response;

        if (request.getTemplateName() != null) {
            // Send templated email
            response = sesEmailService.sendTemplatedEmail(request);
        } else if (request.getHtmlBody() != null) {
            // Send plain HTML email
            response = sesEmailService.sendEmail(
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
