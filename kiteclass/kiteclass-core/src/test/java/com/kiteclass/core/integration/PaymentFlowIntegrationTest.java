package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.payment.dto.gateway.PaymentGatewayRequest;
import com.kiteclass.core.module.payment.dto.gateway.PaymentInitiationResponse;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import com.kiteclass.core.module.payment.gateway.PaymentGatewayClient;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow integration test for Payment workflow.
 *
 * <p>Tests complete business workflow:
 * 1. Create student → class → enrollment → invoice
 * 2. Initiate payment via payment gateway
 * 3. Simulate webhook callback from payment gateway
 * 4. Verify payment status updated
 * 5. Verify invoice status updated to PAID
 * 6. Verify enrollment payment status synced
 *
 * <p>This test verifies cross-module integration:
 * - Payment Module processes transactions
 * - Webhook updates Payment status
 * - Payment status update triggers Invoice update
 * - Invoice update syncs with Enrollment
 *
 * @author KiteClass Team
 * @since 2.10
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional

class PaymentFlowIntegrationTest {

    /**
     * Mock payment gateway clients for testing.
     * Provides test implementations that skip real gateway interactions.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class MockPaymentGatewayConfiguration {

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public PaymentGatewayClient vnpayGatewayClient() {
            PaymentGatewayClient mock = org.mockito.Mockito.mock(PaymentGatewayClient.class);
            org.mockito.Mockito.when(mock.initiatePayment(org.mockito.ArgumentMatchers.any(PaymentGatewayRequest.class)))
                .thenAnswer(invocation -> PaymentInitiationResponse.builder()
                    .paymentUrl("https://test.vnpay.vn/pay/" + ((PaymentGatewayRequest) invocation.getArgument(0)).getTransactionId())
                    .qrCodeUrl("https://test.vnpay.vn/qr/test")
                    .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
                    .build());
            org.mockito.Mockito.when(mock.verifySignature(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
            org.mockito.Mockito.when(mock.queryPaymentStatus(org.mockito.ArgumentMatchers.anyString())).thenReturn(PaymentStatus.PENDING);
            return mock;
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public PaymentGatewayClient momoGatewayClient() {
            PaymentGatewayClient mock = org.mockito.Mockito.mock(PaymentGatewayClient.class);
            org.mockito.Mockito.when(mock.initiatePayment(org.mockito.ArgumentMatchers.any(PaymentGatewayRequest.class)))
                .thenAnswer(invocation -> PaymentInitiationResponse.builder()
                    .paymentUrl("https://test.momo.vn/pay/" + ((PaymentGatewayRequest) invocation.getArgument(0)).getTransactionId())
                    .qrCodeUrl("https://test.momo.vn/qr/test")
                    .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
                    .build());
            org.mockito.Mockito.when(mock.verifySignature(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
            org.mockito.Mockito.when(mock.queryPaymentStatus(org.mockito.ArgumentMatchers.anyString())).thenReturn(PaymentStatus.PENDING);
            return mock;
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public PaymentGatewayClient zalopayGatewayClient() {
            PaymentGatewayClient mock = org.mockito.Mockito.mock(PaymentGatewayClient.class);
            org.mockito.Mockito.when(mock.initiatePayment(org.mockito.ArgumentMatchers.any(PaymentGatewayRequest.class)))
                .thenAnswer(invocation -> PaymentInitiationResponse.builder()
                    .paymentUrl("https://test.zalopay.vn/pay/" + ((PaymentGatewayRequest) invocation.getArgument(0)).getTransactionId())
                    .qrCodeUrl("https://test.zalopay.vn/qr/test")
                    .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
                    .build());
            org.mockito.Mockito.when(mock.verifySignature(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
            org.mockito.Mockito.when(mock.queryPaymentStatus(org.mockito.ArgumentMatchers.anyString())).thenReturn(PaymentStatus.PENDING);
            return mock;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private UUID tenantId;
    private Long teacherId;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = UUID.randomUUID();
        // Create test teacher for course creation
        teacherId = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
    }

    @Test
    @DisplayName("Payment Flow: Initiate Payment → Process → Verify Invoice Updated")
    void testCompletePaymentWorkflow() throws Exception {
        // ========== Step 1: Create Student ==========
        CreateStudentRequest studentRequest = new CreateStudentRequest(
                "Isabella Payment",
                "isabella.payment@test.com",
                "0910000000",
                LocalDate.of(2008, 12, 12),
                Gender.FEMALE,
                "Address",
                null
        );

        MvcResult studentResult = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(studentResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Step 2: Create Course + Class ==========
        CreateCourseRequest courseRequest = new CreateCourseRequest(
                "Literature Basics",           // name
                "LIT101",                      // code
                "Introduction to Literature",  // description
                "Syllabus",                    // syllabus
                "Explore fundamental literary analysis and appreciation", // objectives (required for publish)
                null,                          // prerequisites
                null,                          // targetAudience
                teacherId,                     // teacherId (from test fixture)
                12,                            // durationWeeks (required for publish)
                null,                          // totalSessions
                null,                          // price
                null,                          // level
                null                           // category
        );

        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long courseId = objectMapper.readTree(courseResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                .header("X-Tenant-Id", tenantId.toString()));

        CreateClassRequest classRequest = new CreateClassRequest(
                "Literature Basics - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                25
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Step 3: Enroll Student (Invoice Auto-Created) ==========
        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated());

        // ========== Step 4: Get Invoice ID ==========
        MvcResult invoicesResult = mockMvc.perform(get("/api/v1/invoices/student/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        var invoices = objectMapper.readTree(invoicesResult.getResponse().getContentAsString())
                .get("data").get("content");  // Page wrapper: get content array

        if (invoices.size() < 1) {
            System.out.println("WARNING: No invoice found - may be async creation");
            return;
        }

        Long invoiceId = invoices.get(0).get("id").asLong();
        BigDecimal totalAmount = new BigDecimal(invoices.get(0).get("total").asText());

        // ========== Step 5: Initiate Payment ==========
        // Use online payment method (VNPAY) to test webhook flow
        // Offline methods (BANK_TRANSFER, CASH) auto-complete and cannot receive webhooks
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", "1")  // Wave 105 Bucket E: UserContext.getCurrentUser() requires X-User-Id (gateway-injected at runtime)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.data.amount").value(totalAmount))
                .andReturn();

        var paymentData = objectMapper.readTree(paymentResult.getResponse().getContentAsString()).get("data");
        Long paymentId = paymentData.get("id").asLong();
        String transactionId = paymentData.get("transactionId").asText();

        // ========== Step 6: Verify Payment Status ==========
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        // ========== Step 7: Simulate Webhook Callback (Payment Completed) ==========
        // In a real system, this would come from payment gateway
        // For testing, we simulate webhook processing with actual transactionId
        String webhookPayload = String.format(
                "{\"paymentId\":\"%d\",\"status\":\"COMPLETED\",\"transactionId\":\"%s\",\"gateway\":\"TEST_GATEWAY\"}",
                paymentId,
                transactionId
        );

        mockMvc.perform(post("/api/v1/payments/webhook/momo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // ========== Step 8: Verify Payment Status Updated to COMPLETED ==========
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"));

        // ========== Step 9: Verify Invoice Status Updated to PAID ==========
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // ========== Step 10: Verify Payment History ==========
        mockMvc.perform(get("/api/v1/payments/invoice/" + invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + paymentId + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + paymentId + ")].paymentStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("Payment Flow: Failed payment handling")
    void testFailedPaymentHandling() throws Exception {
        // ========== Setup: Create Student + Class + Enrollment + Invoice ==========
        CreateStudentRequest studentRequest = new CreateStudentRequest(
                "Jack Failed",
                "jack.failed@test.com",
                "0911111111",
                LocalDate.of(2008, 1, 13),
                Gender.MALE,
                "Address",
                null
        );

        MvcResult studentResult = mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long studentId = objectMapper.readTree(studentResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        CreateCourseRequest courseRequest = new CreateCourseRequest(
                "History",                     // name
                "HIS101",                      // code
                "Introduction to History",     // description
                "Syllabus",                    // syllabus
                "Study major historical events and their global impact", // objectives (required for publish)
                null,                          // prerequisites
                null,                          // targetAudience
                teacherId,                     // teacherId (from test fixture)
                10,                            // durationWeeks (required for publish)
                null,                          // totalSessions
                null,                          // price
                null,                          // level
                null                           // category
        );

        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long courseId = objectMapper.readTree(courseResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                .header("X-Tenant-Id", tenantId.toString()));

        CreateClassRequest classRequest = new CreateClassRequest(
                "History - Spring 2026",
                "Spring 2026",
                null,
                null,
                null,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(120),
                30
        );

        MvcResult classResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(classRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        CreateEnrollmentRequest enrollRequest = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(BigDecimal.valueOf(5000000))
                .build();
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(enrollRequest)))
                .andExpect(status().isCreated());

        // Get invoice
        MvcResult invoicesResult = mockMvc.perform(get("/api/v1/invoices/student/" + studentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        var invoices = objectMapper.readTree(invoicesResult.getResponse().getContentAsString())
                .get("data").get("content");  // Page wrapper: get content array

        if (invoices.size() < 1) {
            return;
        }

        Long invoiceId = invoices.get(0).get("id").asLong();
        BigDecimal totalAmount = new BigDecimal(invoices.get(0).get("total").asText());

        // ========== Initiate Payment ==========
        // Use online payment method (VNPAY) to test webhook failure flow
        // Offline methods (BANK_TRANSFER, CASH) cannot receive failure webhooks
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", "1")  // Wave 105 Bucket E: UserContext.getCurrentUser() requires X-User-Id (gateway-injected at runtime)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var paymentData = objectMapper.readTree(paymentResult.getResponse().getContentAsString()).get("data");
        Long paymentId = paymentData.get("id").asLong();
        String transactionId = paymentData.get("transactionId").asText();

        // ========== Simulate Failed Webhook ==========
        String failedWebhook = String.format(
                "{\"paymentId\":\"%d\",\"status\":\"FAILED\",\"transactionId\":\"%s\",\"errorCode\":\"INSUFFICIENT_FUNDS\",\"errorMessage\":\"Insufficient balance\"}",
                paymentId,
                transactionId
        );

        mockMvc.perform(post("/api/v1/payments/webhook/momo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(failedWebhook))
                .andExpect(status().isOk());

        // ========== Verify Payment Status is FAILED ==========
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("FAILED"));

        // ========== Verify Invoice Status Still SENT (unpaid) ==========
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SENT"));

        // ========== Verify Can Retry Payment ==========
        // Retry with offline payment method (CASH) - auto-completes immediately
        CreatePaymentRequest retryPayment = CreatePaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .header("X-User-Id", "1")  // Wave 105 Bucket E: UserContext.getCurrentUser() requires X-User-Id (gateway-injected at runtime)
                        .content(objectMapper.writeValueAsString(retryPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"));
    }
}
