package com.kiteclass.core.module.settings.versioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.module.branding.events.BrandingEventPublisher;
import com.kiteclass.core.module.branding.events.BrandingUpdatedEvent;
import com.kiteclass.core.module.settings.entity.Branding;
import com.kiteclass.core.module.settings.entity.BrandingVersion;
import com.kiteclass.core.module.settings.repository.BrandingRepository;
import com.kiteclass.core.module.settings.repository.BrandingVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * {@link BrandingVersionService} implementation — see interface javadoc.
 *
 * @since Wave 4 (GAP-033p)
 */
@Slf4j
@Service
public class BrandingVersionServiceImpl implements BrandingVersionService {

    private final BrandingVersionRepository versionRepository;
    private final BrandingRepository brandingRepository;
    private final ObjectMapper objectMapper;
    private final BrandingEventPublisher brandingEventPublisher;

    public BrandingVersionServiceImpl(
            BrandingVersionRepository versionRepository,
            BrandingRepository brandingRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) BrandingEventPublisher brandingEventPublisher) {
        this.versionRepository = versionRepository;
        this.brandingRepository = brandingRepository;
        this.objectMapper = objectMapper;
        this.brandingEventPublisher = brandingEventPublisher;
    }

    @Override
    @Transactional
    public BrandingVersion snapshot(Branding branding, Long rollbackOf) {
        UUID instanceId = branding.getInstanceId();

        // Deactivate previous active version (partial unique index enforces singleton).
        versionRepository.findActiveByInstanceId(instanceId).ifPresent(prev -> {
            prev.setActive(false);
            versionRepository.save(prev);
        });

        int nextVersionNumber = safeNext(instanceId);

        BrandingVersion record = BrandingVersion.builder()
                .versionNumber(nextVersionNumber)
                .snapshotJson(serialize(branding))
                .rollbackOf(rollbackOf)
                .active(true)
                .build();
        record.setInstanceId(instanceId);

        BrandingVersion saved = versionRepository.save(record);
        log.info("Snapshotted branding version {} for instance {} (rollbackOf={})",
                saved.getVersionNumber(), instanceId, rollbackOf);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandingVersion> listVersions(UUID instanceId, Pageable pageable) {
        return versionRepository.findByInstanceIdOrderByVersionNumberDesc(instanceId, pageable);
    }

    @Override
    @Transactional
    public BrandingVersion rollback(UUID instanceId, Integer versionNumber) {
        BrandingVersion target = versionRepository
                .findByInstanceIdAndVersionNumber(instanceId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Branding version not found: instance=" + instanceId
                                + " version=" + versionNumber));

        Branding current = brandingRepository.findByInstanceIdAndDeletedFalse(instanceId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot rollback — no branding row exists for instance " + instanceId));

        applySnapshot(current, target.getSnapshotJson());
        brandingRepository.save(current);

        BrandingVersion snapshot = snapshot(current, target.getId());

        if (brandingEventPublisher != null) {
            brandingEventPublisher.publishUpdated(new BrandingUpdatedEvent(
                    current.getId(),
                    instanceId.toString(),
                    snapshot.getVersionNumber(),
                    Instant.now()));
        }
        return snapshot;
    }

    private int safeNext(UUID instanceId) {
        Integer max = versionRepository.maxVersionNumber(instanceId);
        return (max == null ? 0 : max) + 1;
    }

    private String serialize(Branding branding) {
        try {
            return objectMapper.writeValueAsString(BrandingSnapshot.from(branding));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize branding snapshot", ex);
        }
    }

    private void applySnapshot(Branding target, String snapshotJson) {
        try {
            BrandingSnapshot snap = objectMapper.readValue(snapshotJson, BrandingSnapshot.class);
            snap.applyTo(target);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize branding snapshot", ex);
        }
    }
}
