package com.kitehub.email.service;

import com.kitehub.email.config.SESConfig;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for SESEmailService.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SESEmailServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    private SESEmailService sesEmailService;
    private SESConfig.SESProperties sesProperties;

    @BeforeEach
    void setUp() {
        sesProperties = new SESConfig.SESProperties();
        sesProperties.setRegion("ap-southeast-1");
        sesProperties.setFromEmail("noreply@kiteclass.com");
        sesProperties.setFromName("KiteClass Platform");
        sesProperties.setMockMode(true); // Mock mode for testing

        sesEmailService = new SESEmailService(sesProperties, null, null, templateEngine);
    }

    @Test
    void testSendEmail_MockMode() {
        // Given
        String to = "test@example.com";
        String subject = "Test Email";
        String htmlBody = "<h1>Test</h1>";

        // When
        EmailResponse response = sesEmailService.sendEmail(to, subject, htmlBody);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("MOCK");
        assertThat(response.getMessageId()).startsWith("mock-");
        assertThat(response.getSentAt()).isNotNull();
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void testSendTemplatedEmail_MockMode() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("organizationName", "Test Org");
        variables.put("trialDays", 14);

        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Welcome to KiteClass")
                .templateName("welcome")
                .variables(variables)
                .build();

        String renderedHtml = "<h1>Welcome Test Org</h1>";
        when(templateEngine.process(eq("emails/welcome"), any(Context.class)))
                .thenReturn(renderedHtml);

        // When
        EmailResponse response = sesEmailService.sendTemplatedEmail(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("MOCK");
        assertThat(response.getMessageId()).isNotNull();
    }

    @Test
    void testTemplateRendering() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");

        String expectedHtml = "<p>Hello John</p>";
        when(templateEngine.process(eq("emails/test"), any(Context.class)))
                .thenReturn(expectedHtml);

        // When
        EmailRequest request = EmailRequest.builder()
                .to("john@example.com")
                .subject("Test")
                .templateName("test")
                .variables(variables)
                .build();

        EmailResponse response = sesEmailService.sendTemplatedEmail(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("MOCK");
    }
}
