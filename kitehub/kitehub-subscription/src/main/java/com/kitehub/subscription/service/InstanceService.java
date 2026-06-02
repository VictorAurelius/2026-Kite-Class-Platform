package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.platform.domain.enums.InstanceStatus;
import com.kitehub.platform.domain.enums.PricingTier;
import com.kitehub.subscription.dto.CreateInstanceRequest;
import com.kitehub.subscription.dto.CursorPage;
import com.kitehub.subscription.dto.InstanceResponse;
import com.kitehub.subscription.dto.RegisterInstanceRequest;
import com.kitehub.subscription.dto.RegisterInstanceResponse;
import com.kitehub.subscription.dto.UpdateInstanceRequest;
import com.kitehub.subscription.config.TrialConfig;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.tenant.TenantSlugNormalizer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing instances.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final DatabaseProvisioningService databaseProvisioningService;
    private final com.kitehub.subscription.config.MultiTenantDataSourceConfig dataSourceConfig;
    private final TokenService tokenService;
    private final TrialConfig trialConfig;
    private final com.kitehub.subscription.client.EmailServiceClient emailServiceClient;
    private final TenantSlugNormalizer tenantSlugNormalizer;

    /** Max collision-recovery attempts before throwing 409 (GAP-823 Wave local-doable-9). */
    private static final int MAX_SLUG_COLLISION_RETRIES = 10;

    /**
     * Generate unique normalized slug from organization name (GAP-823).
     *
     * <p>Pipeline:
     * <ol>
     *   <li>Normalize organization name via {@link TenantSlugNormalizer#normalize}
     *       (NFC + strip smart quotes + Vietnamese đ→d + stripAccents + lowercase
     *       + non-alphanumeric → dash).</li>
     *   <li>Empty slug after normalize → reject as IllegalArgumentException
     *       (caller must supply a name with at least one slug-able character).</li>
     *   <li>Probe uniqueness via {@link InstanceRepository#existsBySlugAndDeletedFalse}.
     *       If taken, append {@code -1}/{@code -2}/... suffix up to
     *       {@link #MAX_SLUG_COLLISION_RETRIES} attempts.</li>
     *   <li>Exhaust → 409-class IllegalStateException so the controller layer
     *       can render a user-facing message.</li>
     * </ol>
     *
     * @param organizationName user-supplied display name (with diacritics)
     * @return unique normalized slug ready to persist
     * @throws IllegalArgumentException if normalized base is empty
     * @throws IllegalStateException    if 10 retries exhausted without finding a free slug
     */
    public String generateUniqueSlug(String organizationName) {
        String base = tenantSlugNormalizer.normalize(organizationName);
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException(
                "Organization name must contain at least one slug-able character: " + organizationName);
        }
        if (!instanceRepository.existsBySlugAndDeletedFalse(base)) {
            return base;
        }
        for (int suffix = 1; suffix <= MAX_SLUG_COLLISION_RETRIES; suffix++) {
            String candidate = tenantSlugNormalizer.withCollisionSuffix(base, suffix);
            if (!instanceRepository.existsBySlugAndDeletedFalse(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
            "Slug collision recovery exhausted after " + MAX_SLUG_COLLISION_RETRIES
                + " attempts for base: " + base);
    }

    /**
     * Create a new trial instance.
     *
     * @param request create instance request
     * @return created instance response
     */
    // Reserved subdomains that cannot be used for tenant instances
    private static final Set<String> RESERVED_SUBDOMAINS = Set.of(
        "admin", "api", "www", "mail", "ftp", "smtp",
        "test", "staging", "dev", "demo", "app",
        "billing", "support", "help", "docs",
        "status", "cdn", "assets", "static",
        "ns1", "ns2", "mx", "pop", "imap",
        "dashboard", "portal", "login", "register"
    );

    /**
     * Validate that a subdomain is not reserved.
     *
     * @param subdomain subdomain to validate
     * @throws IllegalArgumentException if subdomain is reserved
     */
    private void validateSubdomainNotReserved(String subdomain) {
        if (RESERVED_SUBDOMAINS.contains(subdomain.toLowerCase())) {
            throw new IllegalArgumentException("Subdomain '" + subdomain + "' is reserved");
        }
    }

    public InstanceResponse createTrialInstance(CreateInstanceRequest request) {
        log.info("Creating trial instance for subdomain: {}", request.getSubdomain());

        // Validate subdomain is not reserved
        validateSubdomainNotReserved(request.getSubdomain());

        // Validate trial limit: each owner can only use trial once
        if (request.getOwnerId() != null) {
            if (instanceRepository.existsByOwnerIdAndTrialStartedAtIsNotNull(request.getOwnerId())) {
                throw new IllegalArgumentException(
                        "Mỗi tài khoản chỉ được dùng thử 1 lần. Vui lòng nâng cấp gói để tạo thêm.");
            }
        }

        // Validate subdomain uniqueness
        if (instanceRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists: " + request.getSubdomain());
        }

        // Validate custom domain (only PREMIUM/ENTERPRISE)
        if (request.getCustomDomain() != null && !request.getTier().allowsCustomDomain()) {
            throw new IllegalArgumentException("Custom domain is only available for PREMIUM and ENTERPRISE tiers");
        }

        // Create instance entity
        Instance instance = new Instance();
        instance.setSubdomain(request.getSubdomain());
        instance.setOrganizationName(request.getOrganizationName());
        // GAP-823: generate normalized slug from organization name (collision recovery).
        instance.setSlug(generateUniqueSlug(request.getOrganizationName()));
        instance.setOwnerId(request.getOwnerId());
        instance.setContactEmail(request.getContactEmail());
        instance.setTier(request.getTier());
        instance.setCustomDomain(request.getCustomDomain());

        // Set temporary placeholder credentials
        instance.setDatabaseUrl("pending");
        instance.setDatabaseUsername("pending");
        instance.setDatabasePassword("pending");

        // Start trial with configurable duration
        instance.startTrial(trialConfig.getDurationDays());

        // Save instance first (generates ID)
        Instance saved = instanceRepository.save(instance);

        // Provision database for the instance
        try {
            databaseProvisioningService.provisionDatabase(saved.getId());
            log.info("Database provisioned for instance: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to provision database for instance: {}", saved.getId(), e);
            // Continue - database credentials will be set to pending
        }

        log.info("Created trial instance: {} (expires: {})", saved.getId(), saved.getTrialExpiresAt());

        return toResponse(saved);
    }

    /**
     * Create a PENDING instance (no DB provisioned, awaiting email verification).
     *
     * @param subdomain Instance subdomain
     * @param organizationName Organization name
     * @param ownerId Owner user ID
     * @param contactEmail Contact email
     * @return Created instance response (status=PENDING)
     */
    @Transactional
    public InstanceResponse createPendingInstance(String subdomain, String organizationName,
                                                   UUID ownerId, String contactEmail) {
        log.info("Creating PENDING instance for subdomain: {}", subdomain);

        // Validate subdomain is not reserved
        validateSubdomainNotReserved(subdomain);

        if (instanceRepository.existsBySubdomainAndDeletedFalse(subdomain)) {
            throw new IllegalArgumentException("Subdomain already exists: " + subdomain);
        }

        // Check trial limit: each owner can only use trial once
        if (instanceRepository.existsByOwnerIdAndTrialStartedAtIsNotNull(ownerId)) {
            throw new IllegalArgumentException(
                    "Mỗi tài khoản chỉ được dùng thử 1 lần. Vui lòng nâng cấp gói để tạo thêm.");
        }

        Instance instance = new Instance();
        instance.setSubdomain(subdomain);
        instance.setOrganizationName(organizationName);
        // GAP-823: generate normalized slug from organization name (collision recovery).
        instance.setSlug(generateUniqueSlug(organizationName));
        instance.setOwnerId(ownerId);
        instance.setContactEmail(contactEmail);
        instance.setTier(PricingTier.FREE);
        instance.setStatus(InstanceStatus.PENDING);
        instance.setDatabaseUrl("pending");
        instance.setDatabaseUsername("pending");
        instance.setDatabasePassword("pending");

        Instance saved = instanceRepository.save(instance);
        log.info("Created PENDING instance: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * Activate a PENDING instance: start trial + provision database.
     *
     * @param instanceId Instance ID
     */
    @Transactional
    public void activatePendingInstance(UUID instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + instanceId));

        if (instance.getStatus() != InstanceStatus.PENDING) {
            log.warn("Instance {} is not PENDING (status: {}), skipping activation", instanceId, instance.getStatus());
            return;
        }

        // Start trial with configurable duration
        instance.startTrial(trialConfig.getDurationDays());
        instanceRepository.save(instance);

        // Provision database
        try {
            databaseProvisioningService.provisionDatabase(instanceId);
            log.info("Database provisioned for activated instance: {}", instanceId);
        } catch (Exception e) {
            log.error("Failed to provision database for instance: {}", instanceId, e);
        }

        log.info("Activated PENDING instance: {} → TRIAL", instanceId);

        // Send welcome email
        try {
            emailServiceClient.sendWelcomeEmail(
                instanceId,
                instance.getContactEmail(),
                instance.getOrganizationName(),
                trialConfig.getDurationDays(),
                instance.getTrialExpiresAt().toLocalDate().toString()
            );
        } catch (Exception e) {
            log.error("Failed to send welcome email for instance: {}", instanceId, e);
        }
    }

    /**
     * Register a new trial instance with owner (self-service registration).
     *
     * @param request registration request
     * @return registration response with user info and tokens
     */
    public RegisterInstanceResponse registerInstance(RegisterInstanceRequest request) {
        log.info("Registering new instance for subdomain: {}, email: {}", request.getSubdomain(), request.getOwnerEmail());

        // Validate subdomain is not reserved
        validateSubdomainNotReserved(request.getSubdomain());

        // Validate subdomain uniqueness
        if (instanceRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists: " + request.getSubdomain());
        }

        // Validate email uniqueness (check if owner email already has an instance)
        if (instanceRepository.existsByContactEmailAndDeletedFalse(request.getOwnerEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getOwnerEmail());
        }

        // Validate trial limit: each email can only register trial once (TR-07)
        if (instanceRepository.existsByContactEmailAndTrialStartedAtIsNotNull(request.getOwnerEmail())) {
            throw new IllegalArgumentException(
                    "Mỗi tài khoản chỉ được dùng thử 1 lần. Vui lòng nâng cấp gói để tạo thêm.");
        }

        // Generate owner ID
        UUID ownerId = UUID.randomUUID();

        // Create instance entity
        Instance instance = new Instance();
        instance.setSubdomain(request.getSubdomain());
        instance.setOrganizationName(request.getOrganizationName());
        // GAP-823 Wave local-doable-9 Bucket B: wire TenantSlugNormalizer.
        // Generate unique normalized slug from VN-diacritic organization name +
        // collision recovery (-1/-2/... up to 10 retries → 409). Slug is the
        // canonical URL-routing form; subdomain stays as user-supplied identifier.
        instance.setSlug(generateUniqueSlug(request.getOrganizationName()));
        instance.setOwnerId(ownerId);
        instance.setContactEmail(request.getOwnerEmail());
        instance.setTier(PricingTier.FREE); // Always FREE for trial registration

        // Set temporary placeholder credentials
        instance.setDatabaseUrl("pending");
        instance.setDatabaseUsername("pending");
        instance.setDatabasePassword("pending");

        // Start trial with configurable duration
        instance.startTrial(trialConfig.getDurationDays());

        // Save instance first (generates ID)
        Instance saved = instanceRepository.save(instance);

        // Provision database for the instance
        try {
            databaseProvisioningService.provisionDatabase(saved.getId());
            log.info("Database provisioned for instance: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to provision database for instance: {}", saved.getId(), e);
            // Continue - database credentials will be set to pending
        }

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(ownerId, request.getOwnerEmail(), "OWNER");
        String refreshToken = tokenService.generateRefreshToken(ownerId);

        // Build user info
        RegisterInstanceResponse.UserInfo userInfo = RegisterInstanceResponse.UserInfo.builder()
            .id(ownerId)
            .email(request.getOwnerEmail())
            .name(request.getOrganizationName())
            .role("OWNER")
            .build();

        log.info("Registered trial instance: {} for owner: {}", saved.getId(), ownerId);

        return RegisterInstanceResponse.builder()
            .user(userInfo)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .instance(toResponse(saved))
            .build();
    }

    /**
     * Get instance by ID.
     *
     * @param id instance UUID
     * @return instance response
     */
    @Transactional(readOnly = true)
    public InstanceResponse getInstanceById(UUID id) {
        Instance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        if (instance.isDeleted()) {
            throw new EntityNotFoundException("Instance not found: " + id);
        }

        return toResponse(instance);
    }

    /**
     * Get instance by subdomain.
     *
     * @param subdomain subdomain
     * @return instance response
     */
    @Transactional(readOnly = true)
    public InstanceResponse getInstanceBySubdomain(String subdomain) {
        Instance instance = instanceRepository.findBySubdomainAndDeletedFalse(subdomain)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + subdomain));

        return toResponse(instance);
    }

    /**
     * List instances (non-deleted), paginated.
     *
     * <p><strong>GAP-432 (Wave 41 Bucket C):</strong> the prior implementation
     * called {@code instanceRepository.findAll()} and filtered deleted rows in
     * Java — full-table scan plus client-side filter. Now uses the indexed
     * {@link InstanceRepository#findByDeletedFalse(Pageable)} query. Callers
     * MUST supply a {@link Pageable} (controller defaults to size 50, max 200).</p>
     *
     * @param pageable page request (size, page, sort)
     * @return page of instance responses
     */
    @Transactional(readOnly = true)
    public Page<InstanceResponse> listAllInstances(Pageable pageable) {
        return instanceRepository.findByDeletedFalse(pageable)
            .map(this::toResponse);
    }

    /**
     * Keyset-paginate non-deleted instances after the given cursor — Wave 85
     * Bucket D D-AC1. Use when the instance count is projected to exceed ~1M
     * (avoids OFFSET cliff). Order fixed {@code id ASC}.
     *
     * @param cursorId UUID of the last instance from the prior page (null = first page)
     * @param size     page size (caller-capped to 1..200)
     * @return cursor page with content + nextCursor + hasNext
     */
    @Transactional(readOnly = true)
    public CursorPage<InstanceResponse> listInstancesByCursor(UUID cursorId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Instance> rows = instanceRepository.findAfterCursor(cursorId, pageable);

        boolean hasNext = rows.size() > size;
        List<Instance> trimmed = hasNext ? rows.subList(0, size) : rows;
        List<InstanceResponse> content = trimmed.stream()
            .map(this::toResponse)
            .toList();

        String nextCursor = (hasNext && !trimmed.isEmpty())
            ? CursorPage.encodeCursor(trimmed.get(trimmed.size() - 1).getId())
            : null;

        return new CursorPage<>(content, size, nextCursor, hasNext);
    }

    /**
     * Get all instances for owner.
     *
     * @param ownerId owner UUID
     * @return list of instance responses
     */
    @Transactional(readOnly = true)
    public List<InstanceResponse> getInstancesByOwner(UUID ownerId) {
        return instanceRepository.findByOwnerIdAndDeletedFalse(ownerId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update instance.
     *
     * @param id instance UUID
     * @param request update request
     * @return updated instance response
     */
    public InstanceResponse updateInstance(UUID id, UpdateInstanceRequest request) {
        log.info("Updating instance: {}", id);

        Instance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        if (instance.isDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted instance: " + id);
        }

        // Update organization name
        if (request.getOrganizationName() != null) {
            instance.setOrganizationName(request.getOrganizationName());
        }

        // Update tier
        if (request.getTier() != null) {
            instance.setTier(request.getTier());
        }

        // Update custom domain (validate tier)
        if (request.getCustomDomain() != null) {
            if (!instance.canUseCustomDomain()) {
                throw new IllegalArgumentException("Custom domain requires PREMIUM or ENTERPRISE tier");
            }
            instance.setCustomDomain(request.getCustomDomain());
        }

        // GAP-098: notification preferences
        if (request.getEmailNotifications() != null) {
            instance.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getTrialReminders() != null) {
            instance.setTrialReminders(request.getTrialReminders());
        }

        Instance updated = instanceRepository.save(instance);

        log.info("Updated instance: {}", id);

        return toResponse(updated);
    }

    /**
     * Delete instance (soft delete).
     *
     * @param id instance UUID
     */
    public void deleteInstance(UUID id) {
        log.info("Deleting instance: {}", id);

        Instance instance = instanceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Instance not found: " + id));

        if (instance.isDeleted()) {
            throw new IllegalArgumentException("Instance already deleted: " + id);
        }

        // Close connection pool for this instance
        try {
            dataSourceConfig.closeDataSource(id);
            log.info("Closed DataSource for instance: {}", id);
        } catch (Exception e) {
            log.warn("Failed to close DataSource for instance: {}", id, e);
        }

        // Soft delete instance
        instance.softDelete();
        instance.setStatus(InstanceStatus.DELETED);
        instanceRepository.save(instance);

        log.info("Deleted instance: {}", id);
    }

    /**
     * Suspend expired trials (scheduled job).
     */
    public void suspendExpiredTrials() {
        List<Instance> expiredTrials = instanceRepository.findExpiredTrials(LocalDateTime.now());

        log.info("Found {} expired trials to suspend", expiredTrials.size());

        for (Instance instance : expiredTrials) {
            instance.suspend();
            instanceRepository.save(instance);
            log.info("Suspended expired trial: {} (subdomain: {})", instance.getId(), instance.getSubdomain());
        }
    }

    /**
     * Suspend expired subscriptions (scheduled job).
     */
    public void suspendExpiredSubscriptions() {
        List<Instance> expiredSubscriptions = instanceRepository.findExpiredSubscriptions(LocalDateTime.now());

        log.info("Found {} expired subscriptions to suspend", expiredSubscriptions.size());

        for (Instance instance : expiredSubscriptions) {
            instance.suspend();
            instanceRepository.save(instance);
            log.info("Suspended expired subscription: {} (subdomain: {})", instance.getId(), instance.getSubdomain());
        }
    }

    /**
     * Convert entity to response DTO.
     *
     * @param instance instance entity
     * @return instance response
     */
    private InstanceResponse toResponse(Instance instance) {
        return InstanceResponse.builder()
            .id(instance.getId())
            .subdomain(instance.getSubdomain())
            .slug(instance.getSlug())
            .customDomain(instance.getCustomDomain())
            .organizationName(instance.getOrganizationName())
            .ownerId(instance.getOwnerId())
            .contactEmail(instance.getContactEmail())
            .tier(instance.getTier())
            .status(instance.getStatus())
            .trialStartedAt(instance.getTrialStartedAt())
            .trialExpiresAt(instance.getTrialExpiresAt())
            .trialDaysLeft(instance.getTrialDaysLeft())
            .subscriptionId(instance.getSubscriptionId())
            .subscriptionExpiresAt(instance.getSubscriptionExpiresAt())
            .isActive(instance.isActive())
            .isOnTrial(instance.isOnTrial())
            .createdAt(instance.getCreatedAt())
            .updatedAt(instance.getUpdatedAt())
            .emailNotifications(instance.isEmailNotifications())
            .trialReminders(instance.isTrialReminders())
            .build();
    }
}
