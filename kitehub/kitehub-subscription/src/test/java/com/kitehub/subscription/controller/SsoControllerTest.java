package com.kitehub.subscription.controller;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.config.SecurityConfig;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.service.JwtKeyService;
import com.kitehub.subscription.service.SsoCodeService;
import com.kitehub.subscription.service.SsoCodeService.SsoPrincipal;
import com.kitehub.subscription.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for {@link SsoController} (GAP-1138, ADR-040 Option A).
 *
 * <p>Covers the security contract: issue-code requires a valid access-token Bearer
 * (refresh / malformed / missing → 401), exchange consumes single-use code (invalid
 * → 401), and the CSRF guard (form-encoded body → 415 because {@code consumes=json}).</p>
 */
@WebMvcTest(controllers = SsoController.class)
@Import(SecurityConfig.class)
@DisplayName("SsoController — cross-product SSO (GAP-1138)")
class SsoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SsoCodeService ssoCodeService;
    @MockitoBean private JwtKeyService jwtKeyService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private Jws<Claims> jwsWith(String subject, String email, String role, String type) {
        Claims claims = Jwts.claims()
            .subject(subject)
            .add("email", email)
            .add("role", role)
            .add("type", type)
            .build();
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        return jws;
    }

    // ───────────────────────── issue-code ─────────────────────────

    @Test
    @DisplayName("POST /issue-code — 200 + code for a valid access-token Bearer")
    void issueCode_returns200ForValidAccessToken() throws Exception {
        Jws<Claims> jws = jwsWith(USER_ID.toString(), "owner@test.vn", "OWNER", "access");
        when(jwtKeyService.parse("good-token")).thenReturn(jws);
        when(ssoCodeService.issueCode(eq(USER_ID), eq("owner@test.vn"), eq("OWNER")))
            .thenReturn("ONE-TIME-CODE");
        when(ssoCodeService.ttlSeconds()).thenReturn(60L);

        mockMvc.perform(post("/api/v1/auth/sso/issue-code")
                .with(csrf())
                .header("Authorization", "Bearer good-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("ONE-TIME-CODE"))
            .andExpect(jsonPath("$.expiresIn").value(60));

        verify(ssoCodeService).issueCode(USER_ID, "owner@test.vn", "OWNER");
    }

    @Test
    @DisplayName("POST /issue-code — 401 when the Authorization header is missing")
    void issueCode_rejectsMissingHeader() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sso/issue-code").with(csrf()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("SSO_UNAUTHORIZED"));

        verify(ssoCodeService, never()).issueCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /issue-code — 401 when the token signature/format is invalid")
    void issueCode_rejectsInvalidToken() throws Exception {
        when(jwtKeyService.parse("bad-token")).thenThrow(new JwtException("invalid signature"));

        mockMvc.perform(post("/api/v1/auth/sso/issue-code")
                .with(csrf())
                .header("Authorization", "Bearer bad-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("SSO_UNAUTHORIZED"));

        verify(ssoCodeService, never()).issueCode(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /issue-code — 401 when a refresh token is presented (only access tokens allowed)")
    void issueCode_rejectsRefreshToken() throws Exception {
        Jws<Claims> jws = jwsWith(USER_ID.toString(), "owner@test.vn", "OWNER", "refresh");
        when(jwtKeyService.parse("refresh-token")).thenReturn(jws);

        mockMvc.perform(post("/api/v1/auth/sso/issue-code")
                .with(csrf())
                .header("Authorization", "Bearer refresh-token"))
            .andExpect(status().isUnauthorized());

        verify(ssoCodeService, never()).issueCode(any(), anyString(), anyString());
    }

    // ───────────────────────── exchange ─────────────────────────

    @Test
    @DisplayName("POST /exchange — 200 + minted KH session for a valid one-time code")
    void exchange_returns200ForValidCode() throws Exception {
        when(ssoCodeService.consumeCode("ONE-TIME-CODE"))
            .thenReturn(Optional.of(new SsoPrincipal(USER_ID, "owner@test.vn", "OWNER")));
        when(tokenService.generateAccessToken(USER_ID, "owner@test.vn", "OWNER"))
            .thenReturn("kh-access-jwt");
        when(tokenService.generateRefreshToken(USER_ID)).thenReturn("kh-refresh-jwt");
        User user = mock(User.class);
        when(user.getName()).thenReturn("Trần Thị Hồng");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/sso/exchange")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ONE-TIME-CODE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("kh-access-jwt"))
            .andExpect(jsonPath("$.refreshToken").value("kh-refresh-jwt"))
            .andExpect(jsonPath("$.user.id").value(USER_ID.toString()))
            .andExpect(jsonPath("$.user.email").value("owner@test.vn"))
            .andExpect(jsonPath("$.user.name").value("Trần Thị Hồng"))
            .andExpect(jsonPath("$.user.role").value("OWNER"));

        // Single-use: the code was consumed exactly once.
        verify(ssoCodeService).consumeCode("ONE-TIME-CODE");
    }

    @Test
    @DisplayName("POST /exchange — falls back to email for name when the user row is absent")
    void exchange_nameFallsBackToEmail() throws Exception {
        when(ssoCodeService.consumeCode("CODE2"))
            .thenReturn(Optional.of(new SsoPrincipal(USER_ID, "staff@test.vn", "STAFF")));
        when(tokenService.generateAccessToken(USER_ID, "staff@test.vn", "STAFF"))
            .thenReturn("a");
        when(tokenService.generateRefreshToken(USER_ID)).thenReturn("r");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/sso/exchange")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CODE2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.name").value("staff@test.vn"));
    }

    @Test
    @DisplayName("POST /exchange — 401 for an invalid / expired / already-used code (replay rejected)")
    void exchange_rejectsInvalidCode() throws Exception {
        when(ssoCodeService.consumeCode("used-or-expired")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/sso/exchange")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"used-or-expired\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("SSO_UNAUTHORIZED"));

        verify(tokenService, never()).generateAccessToken(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /exchange — 415 for a form-encoded body (CSRF guard: only application/json accepted)")
    void exchange_rejectsNonJsonContentType() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sso/exchange")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("code=forged-by-cross-site-form"))
            .andExpect(status().isUnsupportedMediaType());

        verify(ssoCodeService, never()).consumeCode(anyString());
    }
}
