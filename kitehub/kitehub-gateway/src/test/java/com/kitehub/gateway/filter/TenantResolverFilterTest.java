package com.kitehub.gateway.filter;

import com.kitehub.gateway.repository.InstanceRepository;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for TenantResolverGatewayFilterFactory.
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class TenantResolverGatewayFilterFactoryTest {

    @Mock
    private InstanceRepository instanceRepository;

    @Mock
    private GatewayFilterChain filterChain;

    private TenantResolverGatewayFilterFactory tenantResolverFilter;

    private Instance activeInstance;

    @BeforeEach
    void setUp() {
        tenantResolverFilter = new TenantResolverGatewayFilterFactory(instanceRepository, ".kitehub.me");

        // Create active instance
        activeInstance = new Instance();
        activeInstance.setId(UUID.randomUUID());
        activeInstance.setSubdomain("customer1");
        activeInstance.setOrganizationName("Customer 1 Org");
        activeInstance.setOwnerId(UUID.randomUUID());
        activeInstance.setTier(PricingTier.BASIC);
        activeInstance.setStatus(InstanceStatus.ACTIVE);
        activeInstance.setDatabaseUrl("jdbc:postgresql://localhost:5432/customer1");
        activeInstance.setDatabaseUsername("customer1_user");
        activeInstance.setDatabasePassword("encrypted_password");
        activeInstance.setCreatedAt(LocalDateTime.now());
        activeInstance.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testTenantResolver_ValidSubdomain_Success() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://customer1.kitehub.me/api/v1/students")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(instanceRepository.findBySubdomain("customer1")).thenReturn(Optional.of(activeInstance));

        // Capture modified exchange
        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            String tenantId = modifiedExchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            assertThat(tenantId).isEqualTo(activeInstance.getId().toString());
            return Mono.empty();
        });

        // When
        Mono<Void> result = tenantResolverFilter.apply(new TenantResolverGatewayFilterFactory.Config())
                .filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testTenantResolver_InstanceNotFound_Returns404() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://nonexistent.kitehub.me/api/v1/students")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        lenient().when(instanceRepository.findBySubdomain("nonexistent")).thenReturn(Optional.empty());
        lenient().when(instanceRepository.findByCustomDomain("nonexistent.kitehub.me")).thenReturn(Optional.empty());

        // When
        Mono<Void> result = tenantResolverFilter.apply(new TenantResolverGatewayFilterFactory.Config())
                .filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testTenantResolver_InstanceSuspended_Returns503() {
        // Given
        activeInstance.setStatus(InstanceStatus.SUSPENDED);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://customer1.kitehub.me/api/v1/students")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(instanceRepository.findBySubdomain("customer1")).thenReturn(Optional.of(activeInstance));

        // When
        Mono<Void> result = tenantResolverFilter.apply(new TenantResolverGatewayFilterFactory.Config())
                .filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void testTenantResolver_CustomBaseDomain_Success() {
        // Given - filter with custom base domain
        TenantResolverGatewayFilterFactory customFilter =
                new TenantResolverGatewayFilterFactory(instanceRepository, ".myschool.io");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://customer1.myschool.io/api/v1/students")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(instanceRepository.findBySubdomain("customer1")).thenReturn(Optional.of(activeInstance));

        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            String tenantId = modifiedExchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            assertThat(tenantId).isEqualTo(activeInstance.getId().toString());
            return Mono.empty();
        });

        // When
        Mono<Void> result = customFilter.apply(new TenantResolverGatewayFilterFactory.Config())
                .filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testTenantResolver_CustomDomain_Success() {
        // Given
        activeInstance.setCustomDomain("school.example.com");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("http://school.example.com/api/v1/students")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        lenient().when(instanceRepository.findBySubdomain("school.example.com")).thenReturn(Optional.empty());
        lenient().when(instanceRepository.findByCustomDomain("school.example.com")).thenReturn(Optional.of(activeInstance));

        // Capture modified exchange
        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            String tenantId = modifiedExchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            assertThat(tenantId).isEqualTo(activeInstance.getId().toString());
            return Mono.empty();
        });

        // When
        Mono<Void> result = tenantResolverFilter.apply(new TenantResolverGatewayFilterFactory.Config())
                .filter(exchange, filterChain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}
