package com.kiteclass.core.module.enrollment.bulkimport.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.module.enrollment.bulkimport.dto.EnrollmentBulkResult;
import com.kiteclass.core.module.enrollment.bulkimport.service.EnrollmentBulkImportService;
import com.kiteclass.core.module.enrollment.bulkimport.service.EnrollmentTemplateGenerator;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller exposing the bulk-enroll endpoints (GAP-1104).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/v1/enrollments/bulk-import/template} — download the xlsx
 *       template (attachment {@code mau-import-ghi-danh.xlsx}); no tenant header.</li>
 *   <li>{@code POST /api/v1/enrollments/bulk-import/preview} — parse + resolve +
 *       field-validate; no DB writes.</li>
 *   <li>{@code POST /api/v1/enrollments/bulk-import/commit} — enroll valid rows
 *       via the existing single-enroll flow; HTTP 201.</li>
 * </ul>
 *
 * <p>Mirrors {@code BulkImportController} (student bulk-import) response style.
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/enrollments/bulk-import")
@RequiredArgsConstructor
@Tag(name = "Enrollment Bulk Import", description = "Bulk-enroll students into classes via xlsx (GAP-1104)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-d", "controller", "enrollment-bulk-import"})
public class EnrollmentBulkImportController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Download filename — Vietnamese ("mẫu import ghi danh") ASCII-safe. */
    private static final String TEMPLATE_FILENAME = "mau-import-ghi-danh.xlsx";

    private final EnrollmentBulkImportService service;
    private final EnrollmentTemplateGenerator templateGenerator;

    /**
     * Download the bulk-enroll xlsx template. No tenant header required — the
     * template is identical for every tenant.
     *
     * @return xlsx bytes as a downloadable attachment
     */
    @GetMapping("/template")
    @Operation(
            summary = "Download bulk-enroll template xlsx",
            description = "Returns an xlsx with a data sheet (header + 2 example rows) and a Vietnamese guide sheet."
    )
    public ResponseEntity<Resource> downloadTemplate() {
        log.info("REST request bulk-enroll template download");
        byte[] bytes = templateGenerator.generateTemplate();
        Resource body = new ByteArrayResource(bytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(TEMPLATE_FILENAME).build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(body);
    }

    /**
     * Preview (dry-run) endpoint. Parses, resolves student/class, and validates
     * fields, returning counts + first 10 errors. No DB writes.
     *
     * @param file     multipart xlsx file (field name {@code file})
     * @param tenantId tenant instance ID from {@code X-Tenant-Id} header
     * @return resolve/validation summary
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'STAFF', 'TEACHER')")
    @Operation(
            summary = "Preview bulk-enroll xlsx",
            description = "Parse + resolve student/class + field-validate; returns counts and first 10 errors. No DB writes. "
                    + "Staff+teacher-tier authz (OWASP A01) — GAP-1527."
    )
    public ApiResponse<EnrollmentBulkResult> preview(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("REST request bulk-enroll preview: file={}, tenantId={}",
                file != null ? file.getOriginalFilename() : "<null>", tenantId);
        EnrollmentBulkResult result = service.preview(file, tenantId);
        return ApiResponse.success(result, "Xem trước xong");
    }

    /**
     * Commit endpoint. Enrolls valid rows (skip-and-report). Returns HTTP 201.
     *
     * @param file     multipart xlsx file
     * @param tenantId tenant instance ID
     * @return summary with success/error counts; inline errors truncated to 10
     */
    @PostMapping(value = "/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRINCIPAL', 'STAFF', 'TEACHER')")
    @Operation(
            summary = "Commit bulk-enroll xlsx",
            description = "Resolve + enroll valid rows via single-enroll flow. Invalid/failed rows skipped and reported. "
                    + "Staff+teacher-tier authz (OWASP A01) — GAP-1527."
    )
    public ResponseEntity<ApiResponse<EnrollmentBulkResult>> commit(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("REST request bulk-enroll commit: file={}, tenantId={}",
                file != null ? file.getOriginalFilename() : "<null>", tenantId);
        EnrollmentBulkResult result = service.commit(file, tenantId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Ghi danh hàng loạt hoàn tất"));
    }
}
