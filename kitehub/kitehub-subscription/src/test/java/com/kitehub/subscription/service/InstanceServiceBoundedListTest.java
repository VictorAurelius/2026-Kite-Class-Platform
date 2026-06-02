package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.config.MultiTenantDataSourceConfig;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-432 (Wave 41 Bucket C) regression test for
 * {@link InstanceService#listAllInstances(Pageable)}.
 *
 * <p>Asserts the bounded {@code findByDeletedFalse(Pageable)} repository
 * method is invoked and that the legacy unbounded {@code findAll()} path
 * is never reached.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstanceService GAP-432 bounded list tests")
class InstanceServiceBoundedListTest {

    @Mock private InstanceRepository instanceRepository;
    @Mock private DatabaseProvisioningService databaseProvisioningService;
    @Mock private MultiTenantDataSourceConfig dataSourceConfig;
    @Mock private TokenService tokenService;
    @Mock private TrialConfig trialConfig;
    @Mock private com.kitehub.subscription.client.EmailServiceClient emailServiceClient;
    @Mock private com.kitehub.subscription.tenant.TenantSlugNormalizer tenantSlugNormalizer;

    @InjectMocks private InstanceService instanceService;

    private Instance sampleInstance() {
        Instance i = new Instance();
        i.setId(UUID.randomUUID());
        i.setSubdomain("demo");
        i.setOrganizationName("Demo");
        i.setStatus(InstanceStatus.TRIAL);
        i.setTier(PricingTier.FREE);
        i.setOwnerId(UUID.randomUUID());
        return i;
    }

    @Test
    @DisplayName("listAllInstances delegates to bounded findByDeletedFalse(Pageable) (not findAll)")
    void listAllInstances_usesBoundedQuery() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<Instance> stub = new PageImpl<>(List.of(sampleInstance()), pageable, 1L);
        when(instanceRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(stub);

        Page<InstanceResponse> result = instanceService.listAllInstances(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(instanceRepository).findByDeletedFalse(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        // Critical invariant: legacy unbounded findAll() must never be reached.
        verify(instanceRepository, never()).findAll();
    }

    @Test
    @DisplayName("Empty page returns empty content cleanly")
    void emptyPage() {
        Pageable pageable = PageRequest.of(0, 50);
        when(instanceRepository.findByDeletedFalse(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        Page<InstanceResponse> result = instanceService.listAllInstances(pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }
}
