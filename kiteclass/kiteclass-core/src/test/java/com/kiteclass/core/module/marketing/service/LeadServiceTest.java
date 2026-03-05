package com.kiteclass.core.module.marketing.service;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.response.LeadResponse;
import com.kiteclass.core.module.marketing.entity.Lead;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import com.kiteclass.core.module.marketing.mapper.LeadMapper;
import com.kiteclass.core.module.marketing.repository.LeadRepository;
import com.kiteclass.core.module.marketing.service.impl.LeadServiceImpl;
import com.kiteclass.core.testutil.LeadTestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LeadServiceImpl}.
 *
 * @since 2.10
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadMapper leadMapper;

    @InjectMocks
    private LeadServiceImpl leadService;

    private Lead lead;
    private LeadResponse leadResponse;
    private CreateLeadRequest createRequest;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        lead = LeadTestDataBuilder.createDefaultLead();
        lead.setInstanceId(tenantId);
        leadResponse = LeadResponse.builder()
                .id(lead.getId())
                .email(lead.getEmail())
                .name(lead.getName())
                .status(lead.getStatus())
                .build();
        createRequest = LeadTestDataBuilder.createDefaultCreateRequest();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createLead_shouldCreateSuccessfully() {
        // Given
        when(leadRepository.findByEmailAndInstanceIdAndDeletedFalse(anyString(), any())).thenReturn(Optional.empty());
        when(leadMapper.toEntity(any(CreateLeadRequest.class))).thenReturn(lead);
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        // When
        LeadResponse result = leadService.createLead(createRequest, tenantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(lead.getEmail());
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void createLead_duplicateEmail_throwsValidationException() {
        // Given - BR-MKT-002: Lead email must be unique per tenant
        when(leadRepository.findByEmailAndInstanceIdAndDeletedFalse(anyString(), any())).thenReturn(Optional.of(lead));

        // When & Then
        assertThatThrownBy(() -> leadService.createLead(createRequest, tenantId))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("LEAD_EMAIL_ALREADY_EXISTS"));

        verify(leadRepository, never()).save(any());
    }

    @Test
    void getLeadById_shouldReturnLead() {
        // Given
        when(leadRepository.findByIdAndInstanceIdAndDeletedFalse(anyLong(), any())).thenReturn(Optional.of(lead));
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        // When
        LeadResponse result = leadService.getLeadById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(lead.getId());
    }

    @Test
    void getLeadById_notFound_throwsEntityNotFoundException() {
        // Given
        when(leadRepository.findByIdAndInstanceIdAndDeletedFalse(anyLong(), any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leadService.getLeadById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(e -> assertThat(e.getMessage()).containsIgnoringCase("LEAD_NOT_FOUND"));
    }

    @Test
    void updateLeadStatus_shouldUpdateSuccessfully() {
        // Given
        when(leadRepository.findByIdAndInstanceIdAndDeletedFalse(anyLong(), any())).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(leadResponse);

        // When
        LeadResponse result = leadService.updateLeadStatus(1L, LeadStatus.CONTACTED);

        // Then
        assertThat(result).isNotNull();
        verify(leadRepository).save(lead);
    }

    @Test
    void deleteLead_shouldSoftDelete() {
        // Given
        when(leadRepository.findByIdAndInstanceIdAndDeletedFalse(anyLong(), any())).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);

        // When
        leadService.deleteLead(1L);

        // Then
        verify(leadRepository).save(lead);
        assertThat(lead.getDeleted()).isTrue();
    }
}
