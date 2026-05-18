package com.kitehub.admin.controller;

import com.kitehub.admin.dto.InstanceSummary;
import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for {@link AdminInstancesController} — verifies HTTP 200 + JSON shape
 * for the canonical {@code /api/v1/admin/instances} path (Wave 92 Bucket D — fixes Wave 90
 * walkthrough 404 sub-finding).
 *
 * <p>Pure unit-level test using Mockito stubs — avoids full Spring context overhead per
 * existing {@link AdminControllerPaginationTest} pattern. Integration test với real DB
 * via Testcontainers covered separately by {@link AdminControllerTest} (legacy paths).</p>
 */
class AdminInstancesControllerTest {

    private InstanceRepository instanceRepository;
    private SubscriptionRepository subscriptionRepository;
    private AdminInstancesController controller;

    @BeforeEach
    void setUp() {
        instanceRepository = mock(InstanceRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        controller = new AdminInstancesController(instanceRepository, subscriptionRepository);
    }

    @Test
    void listInstances_returnsHttp200AndPagedShape() {
        Instance instance = buildInstance("Test Org", "testorg", InstanceStatus.ACTIVE);
        Page<Instance> page = new PageImpl<>(List.of(instance), PageRequest.of(0, 20), 1);
        when(instanceRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(subscriptionRepository.findActiveByInstanceId(any(UUID.class))).thenReturn(Optional.empty());

        ResponseEntity<Page<InstanceSummary>> response = controller.listInstances(PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getOrganizationName()).isEqualTo("Test Org");
        assertThat(response.getBody().getContent().get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void listInstances_emptyResult_returnsHttp200WithEmptyPage() {
        Page<Instance> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(instanceRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        ResponseEntity<Page<InstanceSummary>> response = controller.listInstances(PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalElements()).isZero();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getInstance_existingId_returnsHttp200WithSummary() {
        UUID id = UUID.randomUUID();
        Instance instance = buildInstance("Detail Org", "detailorg", InstanceStatus.TRIAL);
        instance.setId(id);
        when(instanceRepository.findById(id)).thenReturn(Optional.of(instance));
        when(subscriptionRepository.findActiveByInstanceId(id)).thenReturn(Optional.empty());

        ResponseEntity<InstanceSummary> response = controller.getInstance(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrganizationName()).isEqualTo("Detail Org");
        assertThat(response.getBody().getStatus()).isEqualTo("TRIAL");
    }

    @Test
    void getInstance_unknownId_throwsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(instanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getInstance(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Instance not found");
    }

    @Test
    void clampPageable_nullInput_returnsDefaultSize20() {
        Pageable result = AdminInstancesController.clampPageable(null);
        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    void clampPageable_oversized_clampsTo100() {
        Pageable input = PageRequest.of(2, 5_000, Sort.by("createdAt"));
        Pageable result = AdminInstancesController.clampPageable(input);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getSort()).isEqualTo(Sort.by("createdAt"));
    }

    @Test
    void constants_match_legacy_admin_controller() {
        // Defense: v1 stub must keep page-size limits aligned with legacy AdminController.
        assertThat(AdminInstancesController.DEFAULT_PAGE_SIZE).isEqualTo(AdminController.DEFAULT_PAGE_SIZE);
        assertThat(AdminInstancesController.MAX_PAGE_SIZE).isEqualTo(AdminController.MAX_PAGE_SIZE);
    }

    private Instance buildInstance(String name, String subdomain, InstanceStatus status) {
        Instance instance = new Instance();
        instance.setId(UUID.randomUUID());
        instance.setOrganizationName(name);
        instance.setSubdomain(subdomain);
        instance.setStatus(status);
        instance.setOwnerId(UUID.randomUUID());
        instance.setDatabaseUrl("jdbc:postgresql://localhost:5432/" + subdomain);
        instance.setCreatedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        return instance;
    }
}
