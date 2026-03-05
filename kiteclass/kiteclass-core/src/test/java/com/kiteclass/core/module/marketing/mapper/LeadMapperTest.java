package com.kiteclass.core.module.marketing.mapper;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateLeadRequest;
import com.kiteclass.core.module.marketing.dto.response.LeadResponse;
import com.kiteclass.core.module.marketing.entity.Lead;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import com.kiteclass.core.testutil.LeadTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LeadMapper}.
 *
 * @since 2.10
 */
@SpringBootTest
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@ActiveProfiles("test")
class LeadMapperTest {

    @Autowired
    private LeadMapper leadMapper;

    @Test
    void toResponse_shouldMapAllFields() {
        // Given
        Lead lead = LeadTestDataBuilder.createDefaultLead();

        // When
        LeadResponse response = leadMapper.toResponse(lead);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(lead.getId());
        assertThat(response.getEmail()).isEqualTo(lead.getEmail());
        assertThat(response.getName()).isEqualTo(lead.getName());
        assertThat(response.getPhone()).isEqualTo(lead.getPhone());
        assertThat(response.getSource()).isEqualTo(lead.getSource());
        assertThat(response.getStatus()).isEqualTo(lead.getStatus());
        assertThat(response.getMessage()).isEqualTo(lead.getMessage());
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void toEntity_shouldMapCreateRequestCorrectly() {
        // Given
        CreateLeadRequest request = LeadTestDataBuilder.createDefaultCreateRequest();

        // When
        Lead lead = leadMapper.toEntity(request);

        // Then
        assertThat(lead).isNotNull();
        assertThat(lead.getEmail()).isEqualTo(request.getEmail());
        assertThat(lead.getName()).isEqualTo(request.getName());
        assertThat(lead.getPhone()).isEqualTo(request.getPhone());
        assertThat(lead.getSource()).isEqualTo(request.getSource());
        assertThat(lead.getMessage()).isEqualTo(request.getMessage());
        // Status defaults to NEW (set in entity)
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
    }

    @Test
    void updateEntity_shouldIgnoreNullFields() {
        // Given
        Lead lead = LeadTestDataBuilder.createDefaultLead();
        String originalEmail = lead.getEmail();
        LeadStatus originalStatus = lead.getStatus();

        UpdateLeadRequest request = UpdateLeadRequest.builder()
                .name("Updated Name")      // Update
                .phone("0999999999")       // Update
                .email(null)               // Ignore null
                .message(null)             // Ignore null
                .build();

        // When
        leadMapper.updateEntity(lead, request);

        // Then
        assertThat(lead.getName()).isEqualTo("Updated Name");
        assertThat(lead.getPhone()).isEqualTo("0999999999");
        // Null and protected fields should not be updated
        assertThat(lead.getEmail()).isEqualTo(originalEmail);
        assertThat(lead.getStatus()).isEqualTo(originalStatus);
    }
}
