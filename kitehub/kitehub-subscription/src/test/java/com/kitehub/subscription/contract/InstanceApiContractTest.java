package com.kitehub.subscription.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.controller.InstanceController;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.service.InstancePurgeService;
import com.kitehub.subscription.service.InstanceService;
import com.kitehub.subscription.service.TrialService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Contract Tests for InstanceController.
 * <p>
 * Verifies that API responses conform to the documented schema (field names, types, structure).
 * These tests do NOT verify business logic — only that the JSON contract is stable.
 * <p>
 * If a test fails, it means a breaking API change was introduced that would affect consumers
 * (KiteHub Frontend, KiteClass cross-service calls).
 *
 * @since 1.0.0
 */
@WebMvcTest(InstanceController.class)
@Import(com.kitehub.subscription.config.SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("Instance API Contract Tests")
class InstanceApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InstanceService instanceService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private TrialService trialService;

    @MockitoBean
    private InstancePurgeService instancePurgeService;

    // GAP-938 (this PR): AdminApiKeyInterceptor đã xóa (dead code post Wave 79 default-deny).
    // Trước đây MockitoBean để @WebMvcTest tải đủ WebMvcConfig; nay không còn lớp đó nên field cũng remove.

    private static final UUID INSTANCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private InstanceResponse sampleResponse() {
        return InstanceResponse.builder()
                .id(INSTANCE_ID)
                .subdomain("demo-school")
                .customDomain("school.example.com")
                .organizationName("Demo School")
                .ownerId(OWNER_ID)
                .contactEmail("admin@demo.com")
                .tier(PricingTier.BASIC)
                .status(InstanceStatus.TRIAL)
                .trialStartedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .trialExpiresAt(LocalDateTime.of(2026, 1, 15, 0, 0))
                .trialDaysLeft(14L)
                .isActive(true)
                .isOnTrial(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("GET /api/platform/instances/{id}")
    class GetInstanceById {

        @Test
        @DisplayName("Response schema: has all required fields with correct types")
        void responseSchema_hasAllRequiredFields() throws Exception {
            when(instanceService.getInstanceById(INSTANCE_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/platform/instances/{id}", INSTANCE_ID))
                    .andExpect(status().isOk())
                    // String fields
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.subdomain").isString())
                    .andExpect(jsonPath("$.organizationName").isString())
                    // Enum fields (serialized as strings)
                    .andExpect(jsonPath("$.status").isString())
                    .andExpect(jsonPath("$.tier").isString())
                    // Boolean fields
                    .andExpect(jsonPath("$.isActive").isBoolean())
                    .andExpect(jsonPath("$.isOnTrial").isBoolean())
                    // Nullable string fields (present when set)
                    .andExpect(jsonPath("$.customDomain").isString())
                    .andExpect(jsonPath("$.contactEmail").isString())
                    // UUID fields (serialized as strings)
                    .andExpect(jsonPath("$.ownerId").isString())
                    // Timestamp fields (serialized as strings)
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    // Trial fields
                    .andExpect(jsonPath("$.trialStartedAt").exists())
                    .andExpect(jsonPath("$.trialExpiresAt").exists())
                    .andExpect(jsonPath("$.trialDaysLeft").isNumber());
        }

        @Test
        @DisplayName("404 response: uses RFC 7807 ProblemDetail format")
        void notFoundResponse_usesProblemDetailFormat() throws Exception {
            when(instanceService.getInstanceById(INSTANCE_ID))
                    .thenThrow(new EntityNotFoundException("Instance not found"));

            mockMvc.perform(get("/api/platform/instances/{id}", INSTANCE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").isString());
        }
    }

    @Nested
    @DisplayName("GET /api/platform/instances")
    class ListInstances {

        @Test
        @DisplayName("Response schema: returns paged envelope with content array (GAP-432)")
        void responseSchema_returnsPagedEnvelope() throws Exception {
            Page<InstanceResponse> page = new PageImpl<>(List.of(sampleResponse()));
            when(instanceService.listAllInstances(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/platform/instances"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").isString())
                    .andExpect(jsonPath("$.content[0].subdomain").isString())
                    .andExpect(jsonPath("$.content[0].status").isString())
                    .andExpect(jsonPath("$.content[0].tier").isString())
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @DisplayName("Response schema: empty paged envelope when no instances (GAP-432)")
        void responseSchema_emptyEnvelope() throws Exception {
            when(instanceService.listAllInstances(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/platform/instances"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/platform/instances")
    class CreateInstance {

        @Test
        @DisplayName("Response schema: 201 with all required fields")
        void responseSchema_createdWithRequiredFields() throws Exception {
            when(instanceService.createTrialInstance(any(CreateInstanceRequest.class)))
                    .thenReturn(sampleResponse());

            CreateInstanceRequest request = CreateInstanceRequest.builder()
                    .subdomain("new-school")
                    .organizationName("New School")
                    .ownerId(OWNER_ID)
                    .tier(PricingTier.BASIC)
                    .build();

            mockMvc.perform(post("/api/platform/instances")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.subdomain").isString())
                    .andExpect(jsonPath("$.organizationName").isString())
                    .andExpect(jsonPath("$.status").isString())
                    .andExpect(jsonPath("$.tier").isString())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Validation error: 400 with ProblemDetail format")
        void validationError_returnsProblemDetail() throws Exception {
            // Missing required fields
            String invalidBody = "{}";

            mockMvc.perform(post("/api/platform/instances")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").isString());
        }
    }

    @Nested
    @DisplayName("GET /api/platform/instances/subdomain/{subdomain}")
    class GetInstanceBySubdomain {

        @Test
        @DisplayName("Response schema: same structure as get-by-id")
        void responseSchema_sameAsGetById() throws Exception {
            when(instanceService.getInstanceBySubdomain("demo-school"))
                    .thenReturn(sampleResponse());

            mockMvc.perform(get("/api/platform/instances/subdomain/{subdomain}", "demo-school"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.subdomain").isString())
                    .andExpect(jsonPath("$.organizationName").isString())
                    .andExpect(jsonPath("$.status").isString())
                    .andExpect(jsonPath("$.tier").isString());
        }
    }

    @Nested
    @DisplayName("DELETE /api/platform/instances/{id}")
    class DeleteInstance {

        @Test
        @DisplayName("Response: 204 No Content with empty body")
        void response_noContentEmptyBody() throws Exception {
            doNothing().when(instanceService).deleteInstance(INSTANCE_ID);

            mockMvc.perform(delete("/api/platform/instances/{id}", INSTANCE_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 when instance not found")
        void notFound_returnsProblemDetail() throws Exception {
            doThrow(new EntityNotFoundException("Instance not found"))
                    .when(instanceService).deleteInstance(INSTANCE_ID);

            mockMvc.perform(delete("/api/platform/instances/{id}", INSTANCE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Not Found"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/platform/instances/owner/{ownerId}")
    class GetInstancesByOwner {

        @Test
        @DisplayName("Response schema: returns array of instances")
        void responseSchema_returnsArray() throws Exception {
            when(instanceService.getInstancesByOwner(OWNER_ID))
                    .thenReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/platform/instances/owner/{ownerId}", OWNER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").isString())
                    .andExpect(jsonPath("$[0].ownerId").isString());
        }
    }
}
