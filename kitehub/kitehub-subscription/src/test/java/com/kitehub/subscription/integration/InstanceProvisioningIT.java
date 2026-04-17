package com.kitehub.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.repository.InstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for instance provisioning flow.
 * Tests: create → get → update → list → delete lifecycle.
 *
 * @since 1.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Instance Provisioning Flow IT")
class InstanceProvisioningIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstanceRepository instanceRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        instanceRepository.deleteAll();
    }

    @Test
    @DisplayName("Full instance lifecycle: create → get → update → delete")
    void fullInstanceLifecycle() throws Exception {
        // 1. Create instance
        CreateInstanceRequest createRequest = CreateInstanceRequest.builder()
            .subdomain("lifecycle-test")
            .organizationName("Lifecycle Test School")
            .ownerId(UUID.randomUUID())
            .tier(PricingTier.BASIC)
            .build();

        MvcResult createResult = mockMvc.perform(post("/api/platform/instances")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(InstanceStatus.TRIAL.toString()))
            .andReturn();

        InstanceResponse created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), InstanceResponse.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTrialExpiresAt()).isNotNull();

        // 2. Get by ID
        mockMvc.perform(get("/api/platform/instances/{id}", created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subdomain").value("lifecycle-test"));

        // 3. Get by subdomain
        mockMvc.perform(get("/api/platform/instances/subdomain/{sub}", "lifecycle-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationName").value("Lifecycle Test School"));

        // 4. Update
        UpdateInstanceRequest updateRequest = new UpdateInstanceRequest();
        updateRequest.setOrganizationName("Updated School Name");

        mockMvc.perform(patch("/api/platform/instances/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationName").value("Updated School Name"));

        // 5. List all
        mockMvc.perform(get("/api/platform/instances"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        // 6. Delete
        mockMvc.perform(delete("/api/platform/instances/{id}", created.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Create multiple instances for same owner")
    void createMultipleInstancesForSameOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();

        for (int i = 1; i <= 3; i++) {
            CreateInstanceRequest request = CreateInstanceRequest.builder()
                .subdomain("owner-test-" + i)
                .organizationName("School " + i)
                .ownerId(ownerId)
                .tier(PricingTier.BASIC)
                .build();

            mockMvc.perform(post("/api/platform/instances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/platform/instances/owner/{ownerId}", ownerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
    }
}
