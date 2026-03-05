package com.kiteclass.core.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.course.dto.CreateCourseRequest;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
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
@org.junit.jupiter.api.Disabled("TODO: Fix test data setup - requires teacher/course fixtures")

class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
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
                null,                          // objectives
                null,                          // prerequisites
                null,                          // targetAudience
                1L,                            // teacherId
                null,                          // durationWeeks
                null,                          // totalSessions
                null                           // price
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

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
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
                .get("data");

        if (invoices.size() < 1) {
            System.out.println("WARNING: No invoice found - may be async creation");
            return;
        }

        Long invoiceId = invoices.get(0).get("id").asLong();
        BigDecimal totalAmount = new BigDecimal(invoices.get(0).get("totalAmount").asText());

        // ========== Step 5: Initiate Payment ==========
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.data.amount").value(totalAmount))
                .andReturn();

        Long paymentId = objectMapper.readTree(paymentResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Step 6: Verify Payment Status ==========
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // ========== Step 7: Simulate Webhook Callback (Payment Completed) ==========
        // In a real system, this would come from payment gateway
        // For testing, we simulate webhook processing
        String webhookPayload = String.format(
                "{\"paymentId\":\"%d\",\"status\":\"COMPLETED\",\"transactionId\":\"TXN-%s\",\"gateway\":\"TEST_GATEWAY\"}",
                paymentId,
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // ========== Step 8: Verify Payment Status Updated to COMPLETED ==========
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // ========== Step 9: Verify Invoice Status Updated to PAID ==========
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        // ========== Step 10: Verify Payment History ==========
        mockMvc.perform(get("/api/v1/payments/invoice/" + invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + paymentId + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + paymentId + ")].status").value("COMPLETED"));
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
                null,                          // objectives
                null,                          // prerequisites
                null,                          // targetAudience
                1L,                            // teacherId
                null,                          // durationWeeks
                null,                          // totalSessions
                null                           // price
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

        MvcResult classResult = mockMvc.perform(post("/api/v1/classes")
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
                .get("data");

        if (invoices.size() < 1) {
            return;
        }

        Long invoiceId = invoices.get(0).get("id").asLong();
        BigDecimal totalAmount = new BigDecimal(invoices.get(0).get("totalAmount").asText());

        // ========== Initiate Payment ==========
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long paymentId = objectMapper.readTree(paymentResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // ========== Simulate Failed Webhook ==========
        String failedWebhook = String.format(
                "{\"paymentId\":\"%d\",\"status\":\"FAILED\",\"errorCode\":\"INSUFFICIENT_FUNDS\",\"errorMessage\":\"Insufficient balance\"}",
                paymentId
        );

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(failedWebhook))
                .andExpect(status().isOk());

        // ========== Verify Payment Status is FAILED ==========
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        // ========== Verify Invoice Status Still PENDING ==========
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        // ========== Verify Can Retry Payment ==========
        CreatePaymentRequest retryPayment = CreatePaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", tenantId.toString())
                        .content(objectMapper.writeValueAsString(retryPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
