package com.kiteclass.core.module.student.bulkimport.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportResult;
import com.kiteclass.core.module.student.bulkimport.service.StudentBulkImportService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller exposing the bulk-import endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/students/bulk-import/preview} — parse + validate
 *       only, no DB writes.</li>
 *   <li>{@code POST /api/v1/students/bulk-import/commit} — parse + validate +
 *       create; returns the generated job ID in both the response body and
 *       the {@code Location} header.</li>
 *   <li>{@code GET /api/v1/students/bulk-import/jobs/{id}/errors} — stream
 *       the xlsx error report. The MVP is stateless: the caller re-uploads
 *       the original file, so this endpoint is implemented via a POST-shaped
 *       helper ({@link #downloadErrors}) that accepts the file.</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/students/bulk-import")
@RequiredArgsConstructor
@Tag(name = "Student Bulk Import", description = "Bulk import students via xlsx (GAP-051)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-d", "controller", "bulk-import"})
public class BulkImportController {

    private final StudentBulkImportService service;

    /**
     * Preview (dry-run) endpoint. Parses and validates the uploaded xlsx and
     * returns a summary of what <em>would</em> happen on commit. No DB writes.
     *
     * @param file     multipart xlsx file (field name {@code file})
     * @param tenantId tenant instance ID from {@code X-Tenant-Id} header
     * @return parse/validation summary
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Preview bulk-import xlsx",
            description = "Parse + validate only; returns counts and first 10 errors. No DB writes."
    )
    public ApiResponse<BulkImportResult> preview(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("REST request bulk-import preview: file={}, tenantId={}",
                file != null ? file.getOriginalFilename() : "<null>", tenantId);
        BulkImportResult result = service.preview(file, tenantId);
        return ApiResponse.success(result, "Preview xong");
    }

    /**
     * Commit endpoint. Creates valid students and persists a
     * {@code BulkImportJob} row. Returns HTTP 201 with {@code Location} header
     * pointing to the job resource.
     *
     * <p>Optional {@code initialPassword} form field (Wave flow-kc3, GAP-1277):
     * when supplied, every successfully-created student with an email gets a
     * KC-native login credential auto-provisioned so the imported batch is
     * login-ready. Invalid batch password → HTTP 400 {@code BULK_IMPORT_INVALID_PASSWORD}.
     *
     * @param file            multipart xlsx file
     * @param tenantId        tenant instance ID
     * @param initialPassword optional batch login password (opt-in auto-provisioning)
     * @return summary with {@code jobId} + {@code credentialsProvisioned}; inline errors truncated to 10
     */
    @PostMapping(value = "/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Commit bulk-import xlsx",
            description = "Parse + validate + create. Valid rows persisted; invalid rows skipped and reported. "
                    + "Optional initialPassword form field auto-provisions KC-native login for each created student."
    )
    public ResponseEntity<ApiResponse<BulkImportResult>> commit(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(value = "initialPassword", required = false) String initialPassword) {
        log.info("REST request bulk-import commit: file={}, tenantId={}, provisionCredentials={}",
                file != null ? file.getOriginalFilename() : "<null>", tenantId,
                initialPassword != null && !initialPassword.isBlank());
        BulkImportResult result = service.commit(file, tenantId, initialPassword);
        URI location = URI.create("/api/v1/students/bulk-import/jobs/" + result.jobId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .body(ApiResponse.success(result, "Import hoàn tất"));
    }

    /**
     * Download the blank import template xlsx (GAP-1102). Tenant-agnostic /
     * static — no {@code X-Tenant-Id} header required; same bytes for every
     * caller. Users grab this BEFORE uploading so they fill in the exact
     * canonical columns ({@code name}, {@code email}, ...) in the right format.
     *
     * @return xlsx bytes as attachment {@code mau-import-hoc-vien.xlsx}
     */
    @GetMapping(
            value = "/template",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    @Operation(
            summary = "Download blank import template xlsx",
            description = "Static blank template with canonical headers + 2 example rows + a HuongDan sheet. No auth tenant header needed."
    )
    public ResponseEntity<Resource> downloadTemplate() {
        log.info("REST request bulk-import template download");
        byte[] bytes = service.generateTemplate();
        Resource body = new ByteArrayResource(bytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename("mau-import-hoc-vien.xlsx")
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /**
     * Download the error-report xlsx for a given job. Stateless MVP: the
     * client re-uploads the original xlsx and we regenerate the report from
     * a fresh validation pass.
     *
     * @param jobId    job ID (kept in the path for future stateful variants)
     * @param file     the original uploaded xlsx
     * @param tenantId tenant instance ID (reserved for future per-job lookups)
     * @return xlsx bytes as attachment
     */
    @PostMapping(
            value = "/jobs/{id}/errors",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    @Operation(
            summary = "Download error report xlsx",
            description = "Re-validates the original file and returns an xlsx listing each rejected row."
    )
    public ResponseEntity<Resource> downloadErrors(
            @PathVariable("id") Long jobId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("REST request bulk-import error-report: jobId={}, tenantId={}", jobId, tenantId);
        byte[] bytes = service.generateErrorReport(file);
        Resource body = new ByteArrayResource(bytes);

        String filename = "bulk-import-errors-" + jobId + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename(filename)
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /**
     * Placeholder GET variant to register the route in OpenAPI. Always
     * returns 405: clients must POST the xlsx as multipart/form-data.
     */
    @GetMapping("/jobs/{id}/errors")
    @Operation(hidden = true)
    public ResponseEntity<Void> downloadErrorsGetNotSupported(@PathVariable("id") Long jobId) {
        log.debug("GET /jobs/{}/errors is not supported; use POST with multipart file", jobId);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
