package com.kitehub.subscription.betastatus.controller;

import com.kitehub.subscription.betastatus.dto.BetaStatusResponse;
import com.kitehub.subscription.betastatus.service.BetaStatusService;
import com.kitehub.subscription.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link BetaStatusController} (Wave 78 GAP-539).
 *
 * <p>Verifies the public endpoint returns 200 with the expected response shape
 * even when no authentication is presented.</p>
 */
@WebMvcTest(controllers = BetaStatusController.class)
@Import(SecurityConfig.class)
@DisplayName("BetaStatusController")
class BetaStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BetaStatusService service;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET — 200 anonymous with markdown payload + currentStatus")
    void anonymousReturns200() throws Exception {
        BetaStatusResponse response = new BetaStatusResponse(
                "2026-05-14-v1",
                OffsetDateTime.parse("2026-05-14T07:00:00Z"),
                "# Trạng thái Beta KiteHub\n\nHệ thống hoạt động bình thường.\n",
                "OPERATIONAL",
                List.of()
        );
        when(service.getStatus()).thenReturn(response);

        mockMvc.perform(get("/api/v1/beta-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2026-05-14-v1"))
                .andExpect(jsonPath("$.currentStatus").value("OPERATIONAL"))
                .andExpect(jsonPath("$.contentMarkdown").exists())
                .andExpect(jsonPath("$.knownIssues").isArray());
    }
}
