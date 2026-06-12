package com.kiteclass.core.module.branding.controller;

import com.kiteclass.core.module.branding.dto.BrandingPackage;
import com.kiteclass.core.module.branding.service.BrandingPackageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrandingPackageController.class)
@AutoConfigureMockMvc
@Import({BrandingPackageControllerTest.TestSecurityConfig.class, BrandingPackageControllerTest.MockConfig.class})
@ActiveProfiles("test")
class BrandingPackageControllerTest {

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
        public BrandingPackageService service() {
            return Mockito.mock(BrandingPackageService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandingPackageService service;

    private BrandingPackage pkg() {
        return new BrandingPackage(10L, "t-1", "acme",
                "https://acme.kitehub.me", 7, Instant.parse("2026-04-14T00:00:00Z"),
                List.of(new BrandingPackage.AssetEntry("LOGO", "STATIC",
                        "s3://bucket/logo.png", null)));
    }

    @Test
    void get_returns_package_with_etag() throws Exception {
        when(service.getByInstanceId(10L)).thenReturn(pkg());

        MvcResult result = mockMvc.perform(get("/api/v1/branding/10/package"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertThat(etag).startsWith("W/\"v7-");
    }

    @Test
    void get_returns_304_when_etag_matches() throws Exception {
        BrandingPackage p = pkg();
        when(service.getByInstanceId(anyLong())).thenReturn(p);
        String expectedEtag = BrandingPackageController.buildEtag(p);

        mockMvc.perform(get("/api/v1/branding/10/package")
                        .header("If-None-Match", expectedEtag))
                .andExpect(status().isNotModified());
    }

    @Test
    void get_serves_200_when_etag_stale() throws Exception {
        when(service.getByInstanceId(anyLong())).thenReturn(pkg());

        mockMvc.perform(get("/api/v1/branding/10/package")
                        .header("If-None-Match", "W/\"v1-stale\""))
                .andExpect(status().isOk());
    }
}
