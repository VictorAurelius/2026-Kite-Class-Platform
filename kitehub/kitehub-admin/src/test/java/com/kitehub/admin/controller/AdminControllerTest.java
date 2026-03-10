package com.kitehub.admin.controller;

import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.admin.service.AnalyticsService;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for AdminController.
 *
 * @since 1.0
 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private InstanceRepository instanceRepository;

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    @Test
    void testGetDashboard() throws Exception {
        // Given
        Map<String, Long> instancesByStatus = new HashMap<>();
        instancesByStatus.put("ACTIVE", 5L);
        instancesByStatus.put("TRIAL", 3L);

        Map<String, Long> instancesByTier = new HashMap<>();
        instancesByTier.put("BASIC", 4L);
        instancesByTier.put("PREMIUM", 2L);

        Map<String, BigDecimal> revenueByTier = new HashMap<>();
        revenueByTier.put("BASIC", new BigDecimal("2000000"));
        revenueByTier.put("PREMIUM", new BigDecimal("3000000"));

        DashboardStats mockStats = DashboardStats.builder()
                .totalInstances(8L)
                .instancesByStatus(instancesByStatus)
                .instancesByTier(instancesByTier)
                .mrr(new BigDecimal("5000000"))
                .arr(new BigDecimal("60000000"))
                .churnRate(10.5)
                .conversionRate(65.0)
                .newSignupsLast30Days(12L)
                .totalActiveUsers(150L)
                .revenueByTier(revenueByTier)
                .calculatedAt(LocalDateTime.now())
                .build();

        when(analyticsService.getDashboardStats()).thenReturn(mockStats);

        // When & Then
        mockMvc.perform(get("/api/platform/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstances").value(8))
                .andExpect(jsonPath("$.mrr").value(5000000))
                .andExpect(jsonPath("$.arr").value(60000000))
                .andExpect(jsonPath("$.churnRate").value(10.5))
                .andExpect(jsonPath("$.conversionRate").value(65.0))
                .andExpect(jsonPath("$.newSignupsLast30Days").value(12))
                .andExpect(jsonPath("$.totalActiveUsers").value(150));
    }

    @Test
    void testGetAllInstances() throws Exception {
        // Given
        Instance instance1 = new Instance();
        instance1.setId(UUID.randomUUID());
        instance1.setOrganizationName("Org 1");
        instance1.setSubdomain("org1");
        instance1.setStatus("ACTIVE");
        instance1.setOwnerEmail("owner1@example.com");
        instance1.setCreatedAt(LocalDateTime.now());

        Instance instance2 = new Instance();
        instance2.setId(UUID.randomUUID());
        instance2.setOrganizationName("Org 2");
        instance2.setSubdomain("org2");
        instance2.setStatus("TRIAL");
        instance2.setOwnerEmail("owner2@example.com");
        instance2.setCreatedAt(LocalDateTime.now());

        when(instanceRepository.findAll()).thenReturn(Arrays.asList(instance1, instance2));
        when(subscriptionRepository.findByInstanceId(any())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/platform/admin/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].organizationName").value("Org 1"))
                .andExpect(jsonPath("$[0].subdomain").value("org1"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].organizationName").value("Org 2"))
                .andExpect(jsonPath("$[1].status").value("TRIAL"));
    }

    @Test
    void testSuspendInstance() throws Exception {
        // Given
        UUID instanceId = UUID.randomUUID();

        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setOrganizationName("Test Org");
        instance.setSubdomain("testorg");
        instance.setStatus("ACTIVE");
        instance.setOwnerEmail("test@example.com");
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> {
            Instance saved = i.getArgument(0);
            saved.setStatus("SUSPENDED");
            return saved;
        });
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(patch("/api/platform/admin/instances/{id}/suspend", instanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instanceId.toString()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void testActivateInstance() throws Exception {
        // Given
        UUID instanceId = UUID.randomUUID();

        Instance instance = new Instance();
        instance.setId(instanceId);
        instance.setOrganizationName("Test Org");
        instance.setSubdomain("testorg");
        instance.setStatus("SUSPENDED");
        instance.setOwnerEmail("test@example.com");
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(Instance.class))).thenAnswer(i -> {
            Instance saved = i.getArgument(0);
            saved.setStatus("ACTIVE");
            return saved;
        });
        when(subscriptionRepository.findByInstanceId(instanceId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(patch("/api/platform/admin/instances/{id}/activate", instanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(instanceId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testGetRevenue() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        RevenueReport mockReport = RevenueReport.builder()
                .period("MONTHLY")
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(new BigDecimal("10000000"))
                .mrr(new BigDecimal("5000000"))
                .projectedArr(new BigDecimal("60000000"))
                .churnImpact(new BigDecimal("500000"))
                .build();

        when(analyticsService.getRevenueReport(eq("MONTHLY"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(mockReport);

        // When & Then
        mockMvc.perform(get("/api/platform/admin/revenue")
                        .param("period", "MONTHLY")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("MONTHLY"))
                .andExpect(jsonPath("$.totalRevenue").value(10000000))
                .andExpect(jsonPath("$.mrr").value(5000000))
                .andExpect(jsonPath("$.projectedArr").value(60000000));
    }

    @Test
    void testGetAllSubscriptions() throws Exception {
        // Given
        Subscription subscription1 = new Subscription();
        subscription1.setId(UUID.randomUUID());
        subscription1.setInstanceId(UUID.randomUUID());
        subscription1.setTier(PricingTier.BASIC);
        subscription1.setBillingCycle(BillingCycle.MONTHLY);
        subscription1.setPriceVnd(500000L);
        subscription1.setStatus(SubscriptionStatus.ACTIVE);

        Subscription subscription2 = new Subscription();
        subscription2.setId(UUID.randomUUID());
        subscription2.setInstanceId(UUID.randomUUID());
        subscription2.setTier(PricingTier.PREMIUM);
        subscription2.setBillingCycle(BillingCycle.MONTHLY);
        subscription2.setPriceVnd(1500000L);
        subscription2.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findAll()).thenReturn(Arrays.asList(subscription1, subscription2));

        // When & Then
        mockMvc.perform(get("/api/platform/admin/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tier").value("BASIC"))
                .andExpect(jsonPath("$[0].priceVnd").value(500000))
                .andExpect(jsonPath("$[1].tier").value("PREMIUM"))
                .andExpect(jsonPath("$[1].priceVnd").value(1500000));
    }
}
