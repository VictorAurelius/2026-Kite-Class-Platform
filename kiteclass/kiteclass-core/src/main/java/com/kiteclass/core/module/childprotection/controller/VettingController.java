package com.kiteclass.core.module.childprotection.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.childprotection.dto.VettingCreateRequest;
import com.kiteclass.core.module.childprotection.dto.VettingDocumentResponse;
import com.kiteclass.core.module.childprotection.dto.VettingResponse;
import com.kiteclass.core.module.childprotection.dto.VettingTransitionRequest;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.service.VettingService;
import com.kiteclass.core.module.childprotection.storage.VettingDocumentStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Vetting workflow REST endpoints (GAP-322b Phase 1B foundation).
 *
 * <p>RBAC (BR-VETTING-003): only callers carrying the
 * {@code SAFEGUARDING_OFFICER} role on the {@code X-User-Roles} header
 * forwarded by the Gateway may read or write Vetting records. Anyone else
 * receives 403 {@code VETTING_RBAC_DENIED}. Teachers without an APPROVED
 * vetting record are blocked from student-PII endpoints by a separate
 * filter (Phase 1B follow-up — out of scope for this foundation PR).
 *
 * <p>Phase 1B foundation scope: CRUD + state transition + soft delete. No
 * file upload UI yet (the {@code VettingDocumentStorage} contract is
 * satisfied by a stub — actual upload endpoint ships Phase 1B follow-up).
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vettings")
@RequiredArgsConstructor
@Tag(name = "Vetting", description = "Staff vetting workflow (GAP-322b Phase 1B foundation)")
public class VettingController {

    /** Role name authorised to read/write Vetting records. */
    static final String SAFEGUARDING_OFFICER = "SAFEGUARDING_OFFICER";

    /**
     * Per-file upload size cap (10 MB). LLTP scans + CCCD photos comfortably
     * fit; multi-part / chunked upload deferred to Phase 1B follow-up gap.
     */
    static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    private final VettingService vettingService;
    private final VettingDocumentStorage documentStorage;

    @GetMapping
    @Operation(summary = "List vetting records (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<Page<VettingResponse>>> list(
            @RequestParam(value = "status", required = false) VettingStatus status,
            Pageable pageable,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        Page<VettingResponse> page = vettingService.findAll(status, pageable)
                .map(VettingResponse::from);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vetting record by id (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<VettingResponse>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        Vetting v = vettingService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(VettingResponse.from(v)));
    }

    @PostMapping
    @Operation(summary = "Create a new vetting record in PENDING (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<VettingResponse>> create(
            @Valid @RequestBody VettingCreateRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        Vetting created = vettingService.create(
                req.teacherId(),
                req.lltpNumber(),
                req.policeCheckDetails(),
                req.expiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(VettingResponse.from(created)));
    }

    @PatchMapping("/{id}/transition")
    @Operation(summary = "Transition vetting status per BR-VETTING-001 state machine "
            + "(RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<VettingResponse>> transition(
            @PathVariable Long id,
            @Valid @RequestBody VettingTransitionRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long actorUserId
    ) {
        requireSafeguardingOfficer(roles);
        Vetting updated = vettingService.transition(id, req.targetStatus(), actorUserId);
        return ResponseEntity.ok(ApiResponse.success(VettingResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a vetting record (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        vettingService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload an LLTP / CCCD / police-check evidence document for a vetting
     * record (Wave 18b3 — GAP-322b Phase 1B remainder).
     *
     * <p>Accepts a single multipart {@code file} (10 MB cap), persists to
     * MinIO via {@link VettingDocumentStorage}, returns the storage key.
     *
     * <p><b>Phase boundary:</b> a dedicated {@code vetting_document} child
     * entity + audit-log entry on upload are deferred to Phase 1C
     * (GAP-322c). Resumable upload, virus scan, document deletion / replace
     * deferred to follow-up sister gaps.
     *
     * @param vettingId vetting record id (path param)
     * @param file      multipart upload (field name {@code file})
     * @param roles     {@code X-User-Roles} header forwarded by Gateway
     * @return 201 with {@link VettingDocumentResponse}
     */
    @PostMapping(
            value = "/{vettingId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Upload evidence document (LLTP / CCCD / police-check)",
            description = "Single-file multipart upload, ≤10MB. RBAC: SAFEGUARDING_OFFICER only."
    )
    public ResponseEntity<ApiResponse<VettingDocumentResponse>> uploadDocument(
            @PathVariable Long vettingId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);

        if (file == null || file.isEmpty()) {
            throw new ValidationException("VETTING_DOC_EMPTY", new Object[0]);
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ValidationException("VETTING_DOC_TOO_LARGE", new Object[0]);
        }

        // Verify vetting record exists (404 surfaces if not).
        Vetting vetting = vettingService.findById(vettingId);

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new ValidationException("VETTING_DOC_FILENAME_REQUIRED", new Object[0]);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            log.warn("Vetting document upload IOException vettingId={} filename={}",
                    vettingId, filename, ex);
            throw new BusinessException("VETTING_DOC_UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // GAP-1527 (OWASP A05/A08): server-side magic-byte content sniff. The
        // client-reported {@code file.getContentType()} is a hint only and was
        // previously stored verbatim — a script payload spoofed with an image/pdf
        // header would pass. Reject anything whose actual bytes are not PDF / JPG / PNG.
        if (!isAllowedDocumentContent(bytes)) {
            log.warn("Vetting document rejected — content not PDF/JPG/PNG vettingId={} filename={} clientMime={}",
                    vettingId, filename, file.getContentType());
            throw new ValidationException("VETTING_DOC_CONTENT_NOT_ALLOWED", new Object[0]);
        }

        String storageKey = documentStorage.storeDocument(vetting.getId(), filename, bytes);

        VettingDocumentResponse resp = new VettingDocumentResponse(
                vetting.getId(),
                storageKey,
                bytes.length,
                file.getContentType()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(resp));
    }

    /**
     * Reject the request unless the {@code X-User-Roles} header contains the
     * SAFEGUARDING_OFFICER role. Header is comma- or space-separated; matching
     * is case-insensitive.
     *
     * @param rolesHeader value of the {@code X-User-Roles} header (may be null)
     * @throws BusinessException 403 {@code VETTING_RBAC_DENIED} if missing role
     */
    static void requireSafeguardingOfficer(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            throw new BusinessException("VETTING_RBAC_DENIED", HttpStatus.FORBIDDEN);
        }
        List<String> roles = Arrays.stream(rolesHeader.split("[,\\s]+"))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (!roles.contains(SAFEGUARDING_OFFICER)) {
            throw new BusinessException("VETTING_RBAC_DENIED", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Server-side content allowlist by magic bytes (GAP-1527, OWASP A05/A08).
     * Accepts only PDF, JPEG and PNG — the legitimate evidence formats (LLTP /
     * CCCD photo / police-check scan). The client-declared MIME is NOT trusted.
     *
     * @param bytes the actual uploaded file content
     * @return {@code true} iff the leading bytes match PDF / JPEG / PNG
     */
    static boolean isAllowedDocumentContent(byte[] bytes) {
        return matchesPdf(bytes) || matchesJpeg(bytes) || matchesPng(bytes);
    }

    /** PDF: {@code %PDF} (0x25 0x50 0x44 0x46). */
    private static boolean matchesPdf(byte[] b) {
        return b != null && b.length >= 4
                && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46;
    }

    /** JPEG: {@code FF D8 FF}. */
    private static boolean matchesJpeg(byte[] b) {
        return b != null && b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    /** PNG: {@code 89 50 4E 47 0D 0A 1A 0A}. */
    private static boolean matchesPng(byte[] b) {
        return b != null && b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
    }
}
