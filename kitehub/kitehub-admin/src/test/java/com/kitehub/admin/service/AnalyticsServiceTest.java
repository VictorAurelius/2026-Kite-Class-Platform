package com.kitehub.admin.service;

import com.kitehub.admin.dto.DashboardStats;
import com.kitehub.admin.dto.RevenueReport;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.entity.Subscription;
import com.kitehub.platform.domain.enums.BillingCycle;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.platform.domain.enums.SubscriptionStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for AnalyticsService.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private List<Instance> mockInstances;
    private List<Subscription> mockSubscriptions;

    @BeforeEach
    void setUp() {
        // Create mock instances
        Instance instance1 = new Instance();
        instance1.setId(UUID.randomUUID());
        instance1.setOrganizationName("Org 1");
        instance1.setSubdomain("org1");
        instance1.setStatus(InstanceStatus.ACTIVE);
        instance1.setCreatedAt(LocalDateTime.now().minusDays(10));

        Instance instance2 = new Instance();
        instance2.setId(UUID.randomUUID());
        instance2.setOrganizationName("Org 2");
        instance2.setSubdomain("org2");
        instance2.setStatus(InstanceStatus.TRIAL);
        instance2.setCreatedAt(LocalDateTime.now().minusDays(5));

        Instance instance3 = new Instance();
        instance3.setId(UUID.randomUUID());
        instance3.setOrganizationName("Org 3");
        instance3.setSubdomain("org3");
        instance3.setStatus(InstanceStatus.SUSPENDED);
        instance3.setCreatedAt(LocalDateTime.now().minusDays(60));

        mockInstances = Arrays.asList(instance1, instance2, instance3);

        // Create mock subscriptions
        Subscription subscription1 = new Subscription();
        subscription1.setId(UUID.randomUUID());
        subscription1.setInstanceId(instance1.getId());
        subscription1.setTier(PricingTier.BASIC);
        subscription1.setBillingCycle(BillingCycle.MONTHLY);
        subscription1.setPriceVnd(500000L);
        subscription1.setStatus(SubscriptionStatus.ACTIVE);
        subscription1.setStartedAt(LocalDateTime.now().minusDays(30));
        subscription1.setExpiresAt(LocalDateTime.now().plusDays(30));

        Subscription subscription2 = new Subscription();
        subscription2.setId(UUID.randomUUID());
        subscription2.setInstanceId(instance2.getId());
        subscription2.setTier(PricingTier.PREMIUM);
        subscription2.setBillingCycle(BillingCycle.MONTHLY);
        subscription2.setPriceVnd(1500000L);
        subscription2.setStatus(SubscriptionStatus.ACTIVE);
        subscription2.setStartedAt(LocalDateTime.now().minusDays(20));
        subscription2.setExpiresAt(LocalDateTime.now().plusDays(40));

        mockSubscriptions = Arrays.asList(subscription1, subscription2);

        // Mock repository responses
        when(instanceRepository.findAll()).thenReturn(mockInstances);
        when(subscriptionRepository.findAll()).thenReturn(mockSubscriptions);
    }

    @Test
    void testGetDashboardStats() {
        // When
        DashboardStats stats = analyticsService.getDashboardStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalInstances()).isEqualTo(3L);

        // Instances by status
        assertThat(stats.getInstancesByStatus())
                .containsEntry("ACTIVE", 1L)
                .containsEntry("TRIAL", 1L)
                .containsEntry("SUSPENDED", 1L);

        // Instances by tier
        assertThat(stats.getInstancesByTier())
                .containsEntry("BASIC", 1L)
                .containsEntry("PREMIUM", 1L);

        // MRR = 500k + 1.5M = 2M
        assertThat(stats.getMrr()).isEqualByComparingTo(new BigDecimal("2000000"));

        // ARR = MRR × 12 = 24M
        assertThat(stats.getArr()).isEqualByComparingTo(new BigDecimal("24000000"));

        // Churn rate = 1/3 = 33.33%
        assertThat(stats.getChurnRate()).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.01));

        // New signups last 30 days = 2 (instance1, instance2)
        assertThat(stats.getNewSignupsLast30Days()).isEqualTo(2L);

        // Total active users (1 active instance × 10)
        assertThat(stats.getTotalActiveUsers()).isEqualTo(10L);

        assertThat(stats.getCalculatedAt()).isNotNull();
    }

    @Test
    void testGetRevenueReport() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        // When
        RevenueReport report = analyticsService.getRevenueReport("MONTHLY", startDate, endDate);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getPeriod()).isEqualTo("MONTHLY");
        assertThat(report.getStartDate()).isEqualTo(startDate);
        assertThat(report.getEndDate()).isEqualTo(endDate);

        // Total revenue = 500k + 1.5M = 2M
        assertThat(report.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("2000000"));

        // Revenue by tier
        assertThat(report.getRevenueByTier()).hasSize(2);
        assertThat(report.getRevenueByTier()).extracting("tier")
                .containsExactlyInAnyOrder("BASIC", "PREMIUM");

        // MRR and projected ARR
        assertThat(report.getMrr()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(report.getProjectedArr()).isEqualByComparingTo(new BigDecimal("24000000"));

        // Daily revenue data
        assertThat(report.getDailyRevenue()).isNotEmpty();
        assertThat(report.getDailyRevenue().get(0).getDate()).isEqualTo(startDate);
    }

    @Test
    void testGetDashboardStats_EmptyData() {
        // Given - empty data
        when(instanceRepository.findAll()).thenReturn(Arrays.asList());
        when(subscriptionRepository.findAll()).thenReturn(Arrays.asList());

        // When
        DashboardStats stats = analyticsService.getDashboardStats();

        // Then
        assertThat(stats.getTotalInstances()).isEqualTo(0L);
        assertThat(stats.getMrr()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getChurnRate()).isEqualTo(0.0);
        assertThat(stats.getConversionRate()).isEqualTo(0.0);
        assertThat(stats.getNewSignupsLast30Days()).isEqualTo(0L);
    }

    @Test
    void testConversionRate_Calculation() {
        // When
        DashboardStats stats = analyticsService.getDashboardStats();

        // Then
        // Conversion rate = active / (active + trial) = 1 / (1 + 1) = 50%
        assertThat(stats.getConversionRate()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.01));
    }
}
