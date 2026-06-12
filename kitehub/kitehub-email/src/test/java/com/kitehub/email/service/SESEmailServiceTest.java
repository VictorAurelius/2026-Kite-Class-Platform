package com.kitehub.email.service;

import com.kitehub.email.client.BrandingClient;
import com.kitehub.email.config.SESConfig;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.dto.TenantBranding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Mock
    private BrandingClient brandingClient;

    private SESEmailService sesEmailService;
    private SESConfig.SESProperties sesProperties;

    @BeforeEach
    void setUp() {
        sesProperties = new SESConfig.SESProperties();
        sesProperties.setRegion("ap-southeast-1");
        sesProperties.setFromEmail("noreply@kitehub.me");
        sesProperties.setFromName("KiteClass Platform");
        sesProperties.setMockMode(true); // Mock mode for testing

        sesEmailService = new SESEmailService(sesProperties, null, null, templateEngine, brandingClient,
                new com.kitehub.email.service.EmailTemplateRenderer(templateEngine));
        ReflectionTestUtils.setField(sesEmailService, "brandingEnabled", true);
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

    @Test
    void sendTemplatedEmail_injectsTenantBranding_whenInstanceIdProvided() {
        TenantBranding tenantBranding = TenantBranding.builder()
                .displayName("ABC Education")
                .logoUrl("https://cdn.test/logo.png")
                .primaryColor("#112233")
                .secondaryColor("#445566")
                .accentColor("#778899")
                .build();
        when(brandingClient.fetchBranding(eq(42L), anyString())).thenReturn(tenantBranding);
        when(templateEngine.process(eq("emails/welcome"), any(Context.class))).thenReturn("<html/>");

        EmailRequest request = EmailRequest.builder()
                .to("user@abc.edu")
                .subject("Welcome")
                .templateName("welcome")
                .variables(new HashMap<>())
                .instanceId(42L)
                .tenantId("tenant-abc")
                .build();

        sesEmailService.sendTemplatedEmail(request);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/welcome"), contextCaptor.capture());
        Object injected = contextCaptor.getValue().getVariable("branding");
        assertThat(injected).isInstanceOf(TenantBranding.class);
        assertThat(((TenantBranding) injected).getDisplayName()).isEqualTo("ABC Education");
    }

    @Test
    void sendTemplatedEmail_fallsBackToDefaults_whenNoTenantContext() {
        when(templateEngine.process(eq("emails/welcome"), any(Context.class))).thenReturn("<html/>");

        EmailRequest request = EmailRequest.builder()
                .to("user@example.com")
                .subject("Welcome")
                .templateName("welcome")
                .variables(new HashMap<>())
                .build();

        sesEmailService.sendTemplatedEmail(request);

        verify(brandingClient, never()).fetchBranding(anyLong(), anyString());

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/welcome"), contextCaptor.capture());
        Object injected = contextCaptor.getValue().getVariable("branding");
        assertThat(injected).isInstanceOf(TenantBranding.class);
        // Default branding preserves legacy palette so pre-Wave-4 emails look the same.
        assertThat(((TenantBranding) injected).getDisplayName()).isEqualTo("KiteClass");
        assertThat(((TenantBranding) injected).getPrimaryColor()).isEqualTo("#667eea");
    }
}
