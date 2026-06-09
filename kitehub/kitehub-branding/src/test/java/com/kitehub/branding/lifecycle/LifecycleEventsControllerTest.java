package com.kitehub.branding.lifecycle;

import com.kitehub.branding.lifecycle.entity.BrandingInstanceState;
import com.kitehub.branding.lifecycle.entity.BrandingLifecycleEvent;
import com.kitehub.branding.lifecycle.repository.BrandingInstanceStateRepository;
import com.kitehub.branding.lifecycle.repository.BrandingLifecycleEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LifecycleEventsController.class)
@AutoConfigureMockMvc(addFilters = false)
class LifecycleEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandingLifecycleEventRepository eventRepo;

    @MockitoBean
    private BrandingInstanceStateRepository stateRepo;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        Mockito.reset(eventRepo, stateRepo);
        instanceId = UUID.randomUUID();
    }

    @Test
    void getEventsReturnsChronologicalEvents() throws Exception {
        BrandingLifecycleEvent ev = BrandingLifecycleEvent.builder()
            .id(UUID.randomUUID())
            .instanceId(instanceId)
            .eventType("state-change")
            .fromState(LifecycleState.INITIALIZING)
            .toState(LifecycleState.GENERATING)
            .actorKind("system")
            .actorId("saga:provisioning")
            .metadataJson("{\"brandingVersion\":1}")
            .occurredAt(LocalDateTime.now())
            .build();

        when(eventRepo.findByInstanceIdSince(eq(instanceId), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(ev));

        mockMvc.perform(get("/api/v1/branding/instances/{id}/lifecycle/events", instanceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instanceId").value(instanceId.toString()))
            .andExpect(jsonPath("$.events", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$.events[0].type").value("state-change"))
            .andExpect(jsonPath("$.events[0].fromState").value("INITIALIZING"))
            .andExpect(jsonPath("$.events[0].toState").value("GENERATING"))
            .andExpect(jsonPath("$.events[0].actor.kind").value("system"))
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void getEventsReturnsEmptyListForUnknownInstance() throws Exception {
        when(eventRepo.findByInstanceIdSince(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/branding/instances/{id}/lifecycle/events", UUID.randomUUID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void getEventsRespectsLimitClamp() throws Exception {
        when(eventRepo.findByInstanceIdSince(any(), any(), any())).thenReturn(List.of());

        // limit=999 should be clamped to 200; assert no error.
        mockMvc.perform(get("/api/v1/branding/instances/{id}/lifecycle/events?limit=999", instanceId))
            .andExpect(status().isOk());
    }

    // ---- GAP-1108: deploy-status summary -----------------------------------

    @Test
    void getDeployStatusReturnsDeployedWithFrontendUrl() throws Exception {
        BrandingInstanceState state = BrandingInstanceState.builder()
            .instanceId(instanceId)
            .state(LifecycleState.DEPLOYED)
            .brandingVersion(1)
            .regenerateCount(0)
            .build();
        when(stateRepo.findById(instanceId)).thenReturn(Optional.of(state));

        BrandingLifecycleEvent marker = BrandingLifecycleEvent.builder()
            .id(UUID.randomUUID())
            .instanceId(instanceId)
            .eventType("deploy-completed")
            .actorKind("system")
            .actorId("mock-provisioning")
            .metadataJson("{\"frontendUrl\":\"https://toan-master.kiteclass.vn\","
                + "\"templateId\":\"sky-wave\",\"slug\":\"toan-master\",\"mock\":true}")
            .occurredAt(LocalDateTime.now())
            .build();
        when(eventRepo.findByInstanceIdSince(eq(instanceId), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(marker));

        mockMvc.perform(get("/api/v1/branding/instances/{id}/deploy-status", instanceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instanceId").value(instanceId.toString()))
            .andExpect(jsonPath("$.state").value("DEPLOYED"))
            .andExpect(jsonPath("$.deployed").value(true))
            .andExpect(jsonPath("$.frontendUrl").value("https://toan-master.kiteclass.vn"))
            .andExpect(jsonPath("$.templateId").value("sky-wave"))
            .andExpect(jsonPath("$.slug").value("toan-master"))
            .andExpect(jsonPath("$.brandingVersion").value(1));
    }

    @Test
    void getDeployStatusReturnsNotDeployedWhenNoStateRow() throws Exception {
        when(stateRepo.findById(instanceId)).thenReturn(Optional.empty());
        when(eventRepo.findByInstanceIdSince(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/branding/instances/{id}/deploy-status", instanceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deployed").value(false))
            .andExpect(jsonPath("$.state").doesNotExist())
            .andExpect(jsonPath("$.frontendUrl").doesNotExist());
    }
}
