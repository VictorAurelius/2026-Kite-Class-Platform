package com.kitehub.email.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.email.dto.EmailRequest;
import com.kitehub.email.dto.EmailResponse;
import com.kitehub.email.service.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for EmailController.
 *
 * @since 1.0
 */
@WebMvcTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void testSendTemplatedEmail() throws Exception {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("organizationName", "Test Org");

        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Welcome")
                .templateName("welcome")
                .variables(variables)
                .build();

        EmailResponse mockResponse = EmailResponse.builder()
                .messageId("mock-123")
                .status("MOCK")
                .sentAt(LocalDateTime.now())
                .build();

        when(emailSender.sendTemplatedEmail(any(EmailRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("mock-123"))
                .andExpect(jsonPath("$.status").value("MOCK"));
    }

    @Test
    void testSendHtmlEmail() throws Exception {
        // Given
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .htmlBody("<h1>Test</h1>")
                .build();

        EmailResponse mockResponse = EmailResponse.builder()
                .messageId("mock-456")
                .status("MOCK")
                .sentAt(LocalDateTime.now())
                .build();

        when(emailSender.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("mock-456"))
                .andExpect(jsonPath("$.status").value("MOCK"));
    }

    @Test
    void testSendEmail_BadRequest_NoBody() throws Exception {
        // Given - request with neither templateName nor htmlBody
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .subject("Test")
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
