package com.kitehub.subscription.consent.immutable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Immutable consent + hash chain endpoints — Wave br-4 Bucket B (GAP-353b).
 *
 * <p>3 endpoints theo wave plan §3.2 (route prefix {@code /api/v1/consent/v2} để
 * tránh conflict với Wave 25 visitor_id path tại {@code /api/v1/consent/record}):
 * <ul>
 *   <li>{@code POST /api/v1/consent/v2/record} — INSERT new immutable row + hash chain</li>
 *   <li>{@code GET  /api/v1/consent/v2/{userId}} — return history + validate chain</li>
 *   <li>{@code POST /api/v1/consent/v2/withdraw} — INSERT row mới với analytics+marketing=false
 *       (PDPL Art 14 "rút lại sự đồng ý dễ dàng như cho đồng ý")</li>
 * </ul>
 *
 * <p>Different từ {@link com.kitehub.subscription.consent.controller.ConsentController}
 * (Wave 25 Bucket A) — pre-login banner còn dùng path cũ visitor_id-based; path này
 * cho post-login authenticated consent capture với immutability + hash chain.
 *
 * <p>IDOR fix Wave beta-readiness-8 Bucket A (GAP-737): every mutation / read path
 * is guarded by {@link ConsentAuthorizationBean#canAccessUser(Long)} so a logged-in
 * user can only touch their own consent rows. Platform admins keep cross-user access
 * for PDPL DSAR / audit operations.</p>
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b (IDOR guard Wave beta-readiness-8 GAP-737)
 */
@RestController
@RequestMapping("/api/v1/consent/v2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consent v2 (Immutable + hash chain)",
        description = "Immutable PDPL consent + SHA-256 hash chain (GAP-353b Wave br-4 Bucket B)")
public class ImmutableConsentController {

    private final ConsentService consentService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Record consent — INSERT immutable row với hash chain",
            description = "Append-only. RLS blocks UPDATE. SERIALIZABLE isolation cho concurrent safety.")
    @PostMapping("/record")
    @PreAuthorize("@consentAuthz.canAccessUser(#request.userId)")
    public ResponseEntity<ConsentResponseDto> record(
            @Valid @RequestBody ConsentRequestDto request,
            HttpServletRequest httpRequest) {

        String ip = resolveIp(request.getIpAddress(), httpRequest);
        String ua = resolveUserAgent(request.getUserAgent(), httpRequest);

        ConsentRecordImmutable saved = consentService.recordConsent(
                request.getUserId(),
                request.getTenantId(),
                request.getGranted(),
                ip,
                ua);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(saved));
    }

    @Operation(summary = "Get consent history cho user + validate hash chain integrity")
    @GetMapping("/{userId}")
    @PreAuthorize("@consentAuthz.canAccessUser(#userId)")
    public ResponseEntity<ConsentHistoryDto> history(@PathVariable("userId") Long userId) {
        try {
            List<ConsentRecordImmutable> rows = consentService.findHistory(userId);
            if (rows.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No consent records for user=" + userId);
            }
            return ResponseEntity.ok(ConsentHistoryDto.builder()
                    .userId(userId)
                    .records(rows.stream().map(this::toDto).toList())
                    .chainValid(true)
                    .build());
        } catch (ConsentService.ConsentChainIntegrityException ex) {
            log.error("Hash chain integrity violation user={}: {}", userId, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Consent chain integrity violation — manual audit required", ex);
        }
    }

    @Operation(summary = "Withdraw consent — INSERT row mới với analytics+marketing=false",
            description = "PDPL Art 14 'rút lại sự đồng ý dễ dàng như cho đồng ý'. NOT a flag flip.")
    @PostMapping("/withdraw")
    @PreAuthorize("@consentAuthz.canAccessUser(#request.userId)")
    public ResponseEntity<ConsentResponseDto> withdraw(
            @Valid @RequestBody ConsentWithdrawRequestDto request,
            HttpServletRequest httpRequest) {

        String ip = resolveIp(request.getIpAddress(), httpRequest);
        String ua = resolveUserAgent(request.getUserAgent(), httpRequest);

        ConsentRecordImmutable saved = consentService.withdrawConsent(
                request.getUserId(),
                request.getTenantId(),
                ip,
                ua);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // ----------------------------- DTOs -----------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentRequestDto {
        @NotNull
        private Long userId;
        private Long tenantId;
        @NotNull
        private Map<String, Boolean> granted;
        private String ipAddress;
        private String userAgent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentWithdrawRequestDto {
        @NotNull
        private Long userId;
        private Long tenantId;
        private String ipAddress;
        private String userAgent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentResponseDto {
        private Long id;
        private Long userId;
        private Long tenantId;
        private Map<String, Boolean> granted;
        private String prevHash;
        private String currentHash;
        private String ipAddress;
        private OffsetDateTime signedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentHistoryDto {
        private Long userId;
        private List<ConsentResponseDto> records;
        private boolean chainValid;
    }

    // ----------------------------- helpers -----------------------------

    private ConsentResponseDto toDto(ConsentRecordImmutable row) {
        return ConsentResponseDto.builder()
                .id(row.getId())
                .userId(row.getUserId())
                .tenantId(row.getTenantId())
                .granted(deserializeGranted(row.getGranted()))
                .prevHash(row.getPrevHash())
                .currentHash(row.getCurrentHash())
                .ipAddress(row.getIpAddress())
                .signedAt(row.getSignedAt())
                .build();
    }

    private Map<String, Boolean> deserializeGranted(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception ex) {
            log.warn("Cannot deserialize granted JSON, returning raw map: {}", ex.getMessage());
            return Map.of();
        }
    }

    private static String resolveIp(String requestIp, HttpServletRequest httpRequest) {
        if (requestIp != null && !requestIp.isBlank()) {
            return truncate(requestIp, 45);
        }
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return truncate((comma > 0 ? forwarded.substring(0, comma) : forwarded).trim(), 45);
        }
        return truncate(httpRequest.getRemoteAddr(), 45);
    }

    private static String resolveUserAgent(String requestUa, HttpServletRequest httpRequest) {
        if (requestUa != null && !requestUa.isBlank()) {
            return truncate(requestUa, 4096);
        }
        String header = httpRequest.getHeader("User-Agent");
        return truncate(header == null ? "unknown" : header, 4096);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
