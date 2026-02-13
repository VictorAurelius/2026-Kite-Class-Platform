package com.kiteclass.gateway.security;

import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Security context repository for JWT authentication.
 *
 * <p>Loads authentication from JWT token in Authorization header.
 * Used by Spring Security WebFlux to establish security context.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // We don't save context (stateless JWT authentication)
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Validate token and extract claims
            if (!jwtTokenProvider.isAccessToken(token)) {
                log.warn("Invalid token type (not an access token)");
                return Mono.empty();
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);

            // Create UserPrincipal
            UserPrincipal principal = UserPrincipal.builder()
                    .id(userId)
                    .email(email)
                    .roles(roles)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

            // Create authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );

            // Return security context
            return Mono.just(new SecurityContextImpl(authentication));

        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return Mono.empty();
        }
    }
}
