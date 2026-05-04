package com.kiteclass.core.module.childprotection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.exception.GlobalExceptionHandler;
import com.kiteclass.core.module.childprotection.controller.VettingController;
import com.kiteclass.core.module.childprotection.dto.VettingCreateRequest;
import com.kiteclass.core.module.childprotection.dto.VettingTransitionRequest;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.service.VettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice integration test for {@link VettingController} — verifies RBAC
 * gating (only SAFEGUARDING_OFFICER may read/write) plus the happy paths
 * for create / read / transition / soft delete.
 *
 * <p>Uses {@link WebMvcTest} + mocked service layer so the test stays fast
 * (no Postgres / TestContainers boot). State-machine semantics are
 * exhaustively covered in {@code VettingServiceTest}; storage stub in
 * {@code VettingDocumentStorageStubTest}.
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@WebMvcTest(VettingController.class)
@AutoConfigureMockMvc
@Import({
        VettingIntegrationTest.TestSecurityConfig.class,
        VettingIntegrationTest.MockConfig.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
@DisplayName("Vetting Integration — Phase 1B foundation")
class VettingIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        VettingService vettingService() {
            return Mockito.mock(VettingService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private VettingService vettingService;
    @Autowired private ObjectMapper objectMapper;

    private static final String OFFICER_ROLE = "SAFEGUARDING_OFFICER";
    private static final String TEACHER_ROLE = "TEACHER";

    @Test
    @DisplayName("GET /api/v1/vettings — SAFEGUARDING_OFFICER → 200 with list")
    void list_authorised_returns200() throws Exception {
        Vetting v = sample(1L, VettingStatus.PENDING);
        when(vettingService.findAll(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(v)));

        mockMvc.perform(get("/api/v1/vettings")
                        .header("X-User-Roles", OFFICER_ROLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/vettings — TEACHER role → 403 VETTING_RBAC_DENIED")
    void list_teacher_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/vettings")
                        .header("X-User-Roles", TEACHER_ROLE))
                .andExpect(status().isForbidden());

        verify(vettingService, never()).findAll(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/vettings — missing role header → 403")
    void list_missingHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/vettings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/vettings — SAFEGUARDING_OFFICER → 201 created in PENDING")
    void create_authorised_returns201() throws Exception {
        Vetting created = sample(7L, VettingStatus.PENDING);
        created.setTeacherId(100L);
        when(vettingService.create(eq(100L), any(), any(), any())).thenReturn(created);

        VettingCreateRequest req = new VettingCreateRequest(100L, "LLTP-1", null, null);
        mockMvc.perform(post("/api/v1/vettings")
                        .header("X-User-Roles", OFFICER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/vettings — TEACHER → 403, service never called")
    void create_teacher_returns403() throws Exception {
        VettingCreateRequest req = new VettingCreateRequest(100L, "LLTP-1", null, null);
        mockMvc.perform(post("/api/v1/vettings")
                        .header("X-User-Roles", TEACHER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(vettingService, never()).create(any(), any(), any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/vettings/{id}/transition — SAFEGUARDING_OFFICER advances state")
    void transition_authorised_returns200() throws Exception {
        Vetting after = sample(1L, VettingStatus.SUBMITTED);
        when(vettingService.transition(eq(1L), eq(VettingStatus.SUBMITTED), eq(50L)))
                .thenReturn(after);

        VettingTransitionRequest req = new VettingTransitionRequest(VettingStatus.SUBMITTED);
        mockMvc.perform(patch("/api/v1/vettings/{id}/transition", 1L)
                        .header("X-User-Roles", OFFICER_ROLE)
                        .header("X-User-Reference-Id", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/vettings/{id} — SAFEGUARDING_OFFICER → 204")
    void softDelete_authorised_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/vettings/{id}", 1L)
                        .header("X-User-Roles", OFFICER_ROLE))
                .andExpect(status().isNoContent());
        verify(vettingService).softDelete(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/vettings/{id} — TEACHER → 403")
    void softDelete_teacher_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/vettings/{id}", 1L)
                        .header("X-User-Roles", TEACHER_ROLE))
                .andExpect(status().isForbidden());
        verify(vettingService, never()).softDelete(any());
    }

    @Test
    @DisplayName("RBAC: comma-separated multi-role header containing SAFEGUARDING_OFFICER → 200")
    void multiRole_includesOfficer_allowed() throws Exception {
        when(vettingService.findAll(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/vettings")
                        .header("X-User-Roles", "TEACHER, SAFEGUARDING_OFFICER, COUNSELOR"))
                .andExpect(status().isOk());
    }

    private static Vetting sample(Long id, VettingStatus status) {
        Vetting v = Vetting.builder()
                .teacherId(100L)
                .status(status)
                .build();
        v.setId(id);
        return v;
    }
}
