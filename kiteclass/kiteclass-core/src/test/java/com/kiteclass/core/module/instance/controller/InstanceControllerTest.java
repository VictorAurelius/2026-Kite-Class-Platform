package com.kiteclass.core.module.instance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.instance.dto.InitiateInstanceRequest;
import com.kiteclass.core.module.instance.entity.FrontendInstance;
import com.kiteclass.core.module.instance.entity.FrontendInstanceStatus;
import com.kiteclass.core.module.instance.repository.FrontendInstanceRepository;
import com.kiteclass.core.module.instance.service.InstanceLifecycleService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstanceController.class)
@AutoConfigureMockMvc
@Import({InstanceControllerTest.TestSecurityConfig.class, InstanceControllerTest.MockConfig.class})
@ActiveProfiles("test")
class InstanceControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
        public InstanceLifecycleService lifecycle() {
            return Mockito.mock(InstanceLifecycleService.class);
        }

        @Bean
        @Primary
        public FrontendInstanceRepository repository() {
            return Mockito.mock(FrontendInstanceRepository.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstanceLifecycleService lifecycle;

    @Autowired
    private FrontendInstanceRepository repository;

    private FrontendInstance fakeInstance(long id, FrontendInstanceStatus status) {
        FrontendInstance i = FrontendInstance.builder()
                .tenantSlug("t-1").slug("acme").status(status).retryCount(0).brandingVersion(1)
                .build();
        i.setId(id);
        return i;
    }

    @Test
    void initiate_returns_201_with_instance() throws Exception {
        when(lifecycle.initiate(anyString(), anyString()))
                .thenReturn(fakeInstance(10L, FrontendInstanceStatus.INITIALIZING));

        InitiateInstanceRequest req = new InitiateInstanceRequest("t-1", "acme");

        mockMvc.perform(post("/api/v1/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("INITIALIZING"));
    }

    @Test
    void initiate_rejects_invalid_slug() throws Exception {
        InitiateInstanceRequest req = new InitiateInstanceRequest("t-1", "Bad Slug!");

        mockMvc.perform(post("/api/v1/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_returns_instance_when_found() throws Exception {
        when(repository.findById(10L))
                .thenReturn(Optional.of(fakeInstance(10L, FrontendInstanceStatus.DEPLOYED)));

        mockMvc.perform(get("/api/v1/instances/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEPLOYED"));
    }

    @Test
    void markBrandingCompleted_passes_url_to_service() throws Exception {
        when(lifecycle.markBrandingCompleted(anyLong(), any()))
                .thenReturn(fakeInstance(10L, FrontendInstanceStatus.DEPLOYED));

        mockMvc.perform(post("/api/v1/instances/10/branding-completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frontendUrl\":\"https://acme.kitehub.me\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEPLOYED"));
    }

    @Test
    void markFailed_requires_reason() throws Exception {
        mockMvc.perform(post("/api/v1/instances/10/failed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retry_returns_200() throws Exception {
        when(lifecycle.retry(10L))
                .thenReturn(fakeInstance(10L, FrontendInstanceStatus.INITIALIZING));

        mockMvc.perform(post("/api/v1/instances/10/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INITIALIZING"));
    }
}
