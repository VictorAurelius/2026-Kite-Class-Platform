package com.kiteclass.core.module.childprotection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.module.childprotection.entity.ChildProtectionAuditLog;
import com.kiteclass.core.module.childprotection.repository.ChildProtectionAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Default {@link ChildProtectionAuditService} implementation.
 *
 * <p>Hashes via JDK {@code MessageDigest("SHA-256")}, no third-party crypto
 * dependency. Canonicalisation uses Jackson with
 * {@code ORDER_MAP_ENTRIES_BY_KEYS} so two semantically-identical payload
 * maps produce byte-identical JSON regardless of insertion order.
 *
 * <p>The genesis {@code prev_hash} is the literal 64-char zero string —
 * deterministic + matches "no entry" for verification routines.
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@Service
@Slf4j
public class ChildProtectionAuditServiceImpl implements ChildProtectionAuditService {

    /** {@code "0".repeat(64)} — used for the first entry of every chain. */
    public static final String GENESIS_PREV_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String SHA_256 = "SHA-256";

    private final ChildProtectionAuditLogRepository repository;

    /**
     * Stable Jackson mapper — sorts map keys for canonicalisation.
     */
    private final ObjectMapper canonicalMapper;

    public ChildProtectionAuditServiceImpl(ChildProtectionAuditLogRepository repository) {
        this.repository = repository;
        this.canonicalMapper = new ObjectMapper();
        this.canonicalMapper.findAndRegisterModules();
        this.canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Override
    @Transactional
    public ChildProtectionAuditLog append(
            String entityType,
            Long entityId,
            String action,
            Long actorId,
            Map<String, Object> payload) {

        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("entityId is required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }

        UUID tenantId = TenantContext.isSet() ? TenantContext.getCurrentTenant() : null;
        if (tenantId == null) {
            throw new IllegalStateException(
                    "TenantContext required for child-protection audit append");
        }

        String prevHash = repository.findHead(tenantId, entityType)
                .map(ChildProtectionAuditLog::getContentHash)
                .orElse(GENESIS_PREV_HASH);

        Instant now = Instant.now();
        Map<String, Object> safePayload = payload == null
                ? Map.of()
                : new TreeMap<>(payload);

        // Embed the actor + timestamp + entity refs in the payload so the
        // hash binds them too. Caller payload may also include them; the
        // safe map wins under TreeMap merge semantics — overwriting keeps
        // a single deterministic source of truth.
        TreeMap<String, Object> hashedPayload = new TreeMap<>(safePayload);
        hashedPayload.put("entityType", entityType);
        hashedPayload.put("entityId", entityId);
        hashedPayload.put("action", action);
        hashedPayload.put("actorId", actorId);
        hashedPayload.put("occurredAt", now.toString());
        hashedPayload.put("instanceId", tenantId.toString());

        String payloadJson = canonicalize(hashedPayload);
        String contentHash = sha256Hex(prevHash + payloadJson);

        ChildProtectionAuditLog entry = ChildProtectionAuditLog.builder()
                .instanceId(tenantId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorId(actorId)
                .occurredAt(now)
                .prevHash(prevHash)
                .contentHash(contentHash)
                .payloadJson(payloadJson)
                .build();

        ChildProtectionAuditLog saved = repository.save(entry);
        log.info("Audit log appended: chain={}/{} entityId={} action={} hash={}…",
                tenantId, entityType, entityId, action, contentHash.substring(0, 8));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChildProtectionAuditLog> findByEntity(String entityType, Long entityId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return repository.findByEntity(tenantId, entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyChainIntegrity(String entityType) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return verifyChain(tenantId, entityType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyChain(UUID instanceId, String entityType) {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId is required");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        List<ChildProtectionAuditLog> chain =
                repository.findChainAscending(instanceId, entityType);
        String expectedPrev = GENESIS_PREV_HASH;
        for (ChildProtectionAuditLog entry : chain) {
            if (!expectedPrev.equals(entry.getPrevHash())) {
                log.warn("Chain break at id={} expected prev={} got={}",
                        entry.getId(), expectedPrev, entry.getPrevHash());
                return false;
            }
            String recomputed = sha256Hex(entry.getPrevHash() + entry.getPayloadJson());
            if (!recomputed.equals(entry.getContentHash())) {
                log.warn("Hash mismatch at id={} stored={} recomputed={}",
                        entry.getId(), entry.getContentHash(), recomputed);
                return false;
            }
            expectedPrev = entry.getContentHash();
        }
        return true;
    }

    private String canonicalize(Map<String, Object> payload) {
        try {
            return canonicalMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Failed to serialize child-protection audit payload", ex);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_256);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated in every JDK 17+ — this is unreachable in
            // practice; rethrowing as IllegalStateException makes the
            // contract explicit.
            throw new IllegalStateException("SHA-256 not available on this JDK", ex);
        }
    }
}
