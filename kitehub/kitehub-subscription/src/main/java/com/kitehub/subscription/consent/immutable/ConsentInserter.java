package com.kitehub.subscription.consent.immutable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

/**
 * Atomic insert primitive for {@link ConsentService} hash-chain INSERT path.
 *
 * <p>Extracted vào component riêng để Spring's {@code @Transactional} proxy
 * fire khi {@link ConsentService} gọi (cross-bean call). Self-call cùng class
 * sẽ bypass proxy → mất isolation. Pattern theo Spring docs §17.6.
 *
 * <p>SERIALIZABLE isolation + REQUIRES_NEW propagation: mỗi attempt là its own
 * physical txn — serialization failure ở attempt N cleanly rollback và attempt
 * N+1 starts fresh; parent retry-loop trong ConsentService thấy
 * {@code ConcurrencyFailureException} từ Postgres → backoff + retry.
 *
 * @since Wave beta-readiness-4 Bucket B — GAP-353b
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentInserter {

    private final ConsentRecordImmutableRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public ConsentRecordImmutable insertOnce(
            Long userId,
            Long tenantId,
            Map<String, Boolean> sanitized,
            String ipAddress,
            String userAgent) {

        String grantedJson = serializeCanonical(sanitized);
        String prevHash = userId == null ? null :
                repository.findFirstByUserIdOrderBySignedAtDesc(userId)
                        .map(ConsentRecordImmutable::getCurrentHash)
                        .orElse(null);

        // Normalize to UTC + microsecond precision — Postgres TIMESTAMPTZ stores UTC
        // at microsecond precision; computing hash với local-zone Java OffsetDateTime
        // would diverge from recomputed hash after DB round-trip (returned as UTC).
        OffsetDateTime signedAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        String currentHash = computeHash(prevHash, userId, tenantId, grantedJson,
                ipAddress, userAgent, signedAt);

        ConsentRecordImmutable row = ConsentRecordImmutable.builder()
                .userId(userId)
                .tenantId(tenantId)
                .granted(grantedJson)
                .prevHash(prevHash)
                .currentHash(currentHash)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .signedAt(signedAt)
                .build();

        ConsentRecordImmutable saved = repository.saveAndFlush(row);
        log.info("Consent recorded user={} tenant={} prevHash={} currentHash={}",
                userId, tenantId,
                prevHash == null ? "<null/chain-head>" : prevHash.substring(0, 8),
                currentHash.substring(0, 8));
        return saved;
    }

    static String computeHash(
            String prevHash,
            Long userId,
            Long tenantId,
            String grantedJson,
            String ipAddress,
            String userAgent,
            OffsetDateTime signedAt) {
        // Always normalize to UTC + microseconds before stringifying — eliminates
        // local-zone vs Postgres-UTC drift trong recomputation.
        OffsetDateTime canonical = signedAt
                .withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        StringBuilder body = new StringBuilder();
        body.append(prevHash == null ? "" : prevHash);
        body.append("|user=").append(userId == null ? "" : userId);
        body.append("|tenant=").append(tenantId == null ? "" : tenantId);
        body.append("|granted=").append(grantedJson);
        body.append("|ip=").append(ipAddress);
        body.append("|ua=").append(userAgent);
        body.append("|at=").append(canonical.toString());
        return sha256Hex(body.toString());
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String serializeCanonical(Map<String, Boolean> granted) {
        try {
            return objectMapper.writeValueAsString(new TreeMap<>(granted));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot serialize granted JSON", ex);
        }
    }
}
