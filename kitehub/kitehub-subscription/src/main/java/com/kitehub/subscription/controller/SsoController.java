package com.kitehub.subscription.controller;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.dto.LoginResponse;
import com.kitehub.subscription.dto.SsoExchangeRequest;
import com.kitehub.subscription.dto.SsoIssueCodeResponse;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.service.JwtKeyService;
import com.kitehub.subscription.service.SsoCodeService;
import com.kitehub.subscription.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Cross-product SSO KiteHub → KiteClass (ADR-040 Option A, GAP-1138).
 *
 * <p>Two endpoints under the already-public {@code /api/v1/auth/sso/**} surface
 * (whitelisted in both {@code JwtAuthenticationGatewayFilter.isPublicPath} and
 * {@code SecurityConfig} {@code /api/v1/auth/** permitAll}):</p>
 *
 * <ul>
 *   <li>{@code POST /issue-code} — the authenticated KiteHub owner/staff exchanges
 *       their Bearer access token for a short-lived one-time code. Because the
 *       gateway treats {@code /api/v1/auth/**} as public it does NOT validate the
 *       JWT nor inject {@code X-User-*}; this endpoint therefore validates the
 *       Bearer token itself via the shared {@link JwtKeyService} (HS512 signature +
 *       expiry). A missing/invalid/refresh token → 401.</li>
 *   <li>{@code POST /exchange} — KiteClass redeems the one-time code (single-use,
 *       consumed atomically) for a freshly-minted KiteHub JWT scoped to the user's
 *       current tenant + tier. The exchange is public (the code IS the credential).
 *       CSRF guard: {@code consumes = application/json} rejects cross-site form
 *       auto-submits (415) — see {@link SsoExchangeRequest}.</li>
 * </ul>
 *
 * <p>Token validation downstream is unchanged: the minted JWT is HS512-signed with
 * the shared {@code JWT_SECRET} so the gateway {@code TenantHeaderGuardFilter}
 * verifies it + injects {@code X-Tenant-Id} (ADR-039 precedent).</p>
 *
 * @since GAP-1138 (Wave RBAC-SSO 1)
 */
@RestController
@RequestMapping("/api/v1/auth/sso")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cross-Product SSO", description = "KiteHub → KiteClass one-time-code SSO (ADR-040)")
public class SsoController {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";

    private final SsoCodeService ssoCodeService;
    private final JwtKeyService jwtKeyService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    /**
     * Mint a one-time SSO exchange code for the authenticated KiteHub user.
     *
     * @param authHeader the {@code Authorization: Bearer <KH-JWT>} header
     * @return 200 with {@link SsoIssueCodeResponse}; 401 if the token is
     *         missing / malformed / not an access token / signature-invalid
     */
    @PostMapping("/issue-code")
    public ResponseEntity<?> issueCode(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized("Yêu cầu Bearer token hợp lệ để phát mã SSO");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        Claims claims;
        try {
            claims = jwtKeyService.parse(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("SSO issue-code rejected — invalid token: {}", ex.getMessage());
            return unauthorized("Token không hợp lệ hoặc đã hết hạn");
        }

        // Reject refresh tokens — only an access token represents an active session.
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            return unauthorized("Chỉ access token mới được phát mã SSO");
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return unauthorized("Token thiếu subject hợp lệ");
        }

        String code = ssoCodeService.issueCode(
            userId,
            claims.get(CLAIM_EMAIL, String.class),
            claims.get(CLAIM_ROLE, String.class));

        return ResponseEntity.ok(new SsoIssueCodeResponse(code, ssoCodeService.ttlSeconds()));
    }

    /**
     * Redeem a one-time SSO code for a fresh KiteHub session (access + refresh JWT).
     *
     * <p>{@code consumes = application/json} is the CSRF guard: a cross-site form
     * auto-submit cannot set this content type, so a forged request gets 415 before
     * reaching the handler. The code is consumed atomically (single-use) — a replay
     * finds nothing → 401.</p>
     *
     * @param request JSON body carrying the one-time {@code code}
     * @return 200 with {@link LoginResponse} (KH-minted access + refresh + user);
     *         401 if the code is invalid / expired / already consumed
     */
    @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exchange(@Valid @RequestBody SsoExchangeRequest request) {
        return ssoCodeService.consumeCode(request.code())
            .map(principal -> {
                String accessToken = tokenService.generateAccessToken(
                    principal.userId(), principal.email(), principal.role());
                String refreshToken = tokenService.generateRefreshToken(principal.userId());

                // Name is the only field not carried in the code; resolve from DB
                // (best-effort — fall back to email if the user row is gone).
                String name = userRepository.findById(principal.userId())
                    .map(User::getName)
                    .orElse(principal.email());

                LoginResponse body = LoginResponse.builder()
                    .user(LoginResponse.UserInfo.builder()
                        .id(principal.userId())
                        .email(principal.email())
                        .name(name)
                        .role(principal.role())
                        .build())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
                return ResponseEntity.ok((Object) body);
            })
            .orElseGet(() -> unauthorized("Mã SSO không hợp lệ hoặc đã hết hạn"));
    }

    private static ResponseEntity<Object> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "SSO_UNAUTHORIZED", "message", message));
    }
}
