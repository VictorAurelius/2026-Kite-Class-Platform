package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportResult;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import com.kiteclass.core.module.student.bulkimport.dto.RowError;
import com.kiteclass.core.module.student.bulkimport.entity.BulkImportJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrator (Facade) for the bulk-import feature.
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #preview(MultipartFile, UUID)} — parse + validate only, no DB
 *       writes; lets the user see problems before committing.</li>
 *   <li>{@link #commit(MultipartFile, UUID)} — parse + validate + create valid
 *       rows, persisting a {@link BulkImportJob} row for audit.</li>
 * </ul>
 *
 * <p>Invalid rows are <em>skipped and reported</em> — valid rows are still
 * created. The caller can download the xlsx error report to fix and re-upload
 * just the failed rows.
 *
 * <p>Per-chunk transactions: rows are processed in {@link #CHUNK_SIZE} batches
 * via {@link BulkImportChunkExecutor}. A failure in one chunk does not roll
 * back already-saved chunks — aligns with the skip-and-report policy and
 * avoids holding one giant transaction open.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentBulkImportService {

    /**
     * Hard upper bound on rows per upload.
     *
     * <p>Wave 86 E-AC5: lowered from 10_000 → 1_000 per spec
     * "Bulk-import endpoint cap = 1000 rows/request (HTTP 413 if exceeded);
     * FE chunk client-side if > 1000". Aligns with Phase 1 BETA performance
     * envelope (t3.micro) and prevents single-request memory blow-up.</p>
     */
    public static final int MAX_ROWS = 1_000;

    /** Rows persisted per transaction. */
    public static final int CHUNK_SIZE = 500;

    private final XlsxParser xlsxParser;
    private final RowValidator rowValidator;
    private final BulkImportChunkExecutor chunkExecutor;
    private final ErrorReportGenerator errorReportGenerator;
    private final XlsxTemplateGenerator xlsxTemplateGenerator;

    /**
     * Dry-run: parse + validate only. Does not write to the database.
     *
     * @param file     the uploaded xlsx
     * @param tenantId tenant instance ID (kept in signature to mirror {@link #commit})
     * @return summary with per-row errors; {@code jobId} is {@code null}
     */
    public BulkImportResult preview(MultipartFile file, UUID tenantId) {
        assertFilePresent(file);
        List<BulkImportRow> rows = parseSafely(file);
        assertRowLimit(rows.size());

        List<RowError> errorList = new ArrayList<>();
        for (BulkImportRow row : rows) {
            RowValidator.ValidationResult result = rowValidator.validate(row);
            if (!result.isValid()) {
                errorList.addAll(result.errors());
            }
        }
        errorList.addAll(detectInFileDuplicates(rows));

        int failedRows = countFailedRows(errorList);
        int success = rows.size() - failedRows;
        log.info("Bulk-import preview: tenantId={}, total={}, success={}, failedRows={}",
                tenantId, rows.size(), success, failedRows);
        return new BulkImportResult(
                null,
                rows.size(),
                success,
                failedRows,
                truncate(errorList)
        );
    }

    /**
     * Commit phase: parse + validate + create. Persists a {@link BulkImportJob}
     * row regardless of outcome so administrators can audit past imports.
     *
     * @param file     the uploaded xlsx
     * @param tenantId tenant instance ID
     * @return summary with per-row errors and the generated {@code jobId}
     */
    public BulkImportResult commit(MultipartFile file, UUID tenantId) {
        assertFilePresent(file);
        List<BulkImportRow> rows = parseSafely(file);
        assertRowLimit(rows.size());

        BulkImportJob job = chunkExecutor.createJob(file.getOriginalFilename(), tenantId, rows.size());
        List<RowError> allErrors = new ArrayList<>();

        // In-file duplicate detection runs BEFORE DB commit so we can flag
        // duplicate rows as errors instead of hitting a UNIQUE constraint at
        // insert time (which would produce a 500 response).
        List<RowError> duplicateErrors = detectInFileDuplicates(rows);
        allErrors.addAll(duplicateErrors);

        java.util.Set<Integer> skipRowNumbers = new java.util.HashSet<>();
        for (RowError e : duplicateErrors) {
            skipRowNumbers.add(e.rowNumber());
        }

        int successCount = 0;
        for (int from = 0; from < rows.size(); from += CHUNK_SIZE) {
            int to = Math.min(from + CHUNK_SIZE, rows.size());
            List<BulkImportRow> chunk = new ArrayList<>(rows.subList(from, to));
            chunk.removeIf(r -> skipRowNumbers.contains(r.rowNumber()));
            if (chunk.isEmpty()) {
                continue;
            }
            BulkImportChunkExecutor.ChunkResult r = chunkExecutor.processChunk(chunk, tenantId);
            successCount += r.successCount();
            allErrors.addAll(r.errors());
        }

        int failedRows = countFailedRows(allErrors);
        chunkExecutor.finalizeJob(job.getId(), tenantId, successCount, failedRows);

        log.info("Bulk-import commit done: jobId={}, tenantId={}, total={}, success={}, failedRows={}",
                job.getId(), tenantId, rows.size(), successCount, failedRows);

        return new BulkImportResult(
                job.getId(),
                rows.size(),
                successCount,
                failedRows,
                truncate(allErrors)
        );
    }

    /**
     * Re-parses the given file and runs validation only, then generates an
     * xlsx error report. The MVP is stateless: clients re-upload the original
     * file to retrieve the error list for a given job.
     *
     * @param file the original uploaded xlsx
     * @return xlsx bytes containing the error report (headers + rows)
     */
    public byte[] generateErrorReport(MultipartFile file) {
        assertFilePresent(file);
        List<BulkImportRow> rows = parseSafely(file);
        assertRowLimit(rows.size());

        List<RowError> errors = new ArrayList<>();
        for (BulkImportRow row : rows) {
            RowValidator.ValidationResult result = rowValidator.validate(row);
            if (!result.isValid()) {
                errors.addAll(result.errors());
            }
        }
        errors.addAll(detectInFileDuplicates(rows));
        return errorReportGenerator.generate(rows, errors);
    }

    /**
     * Generates the blank import template xlsx (GAP-1102). Tenant-agnostic /
     * static — same bytes for every caller, so no {@code tenantId} parameter.
     *
     * @return xlsx bytes for the downloadable template (canonical headers + 2
     *         example rows + a HuongDan instructions sheet)
     */
    public byte[] generateTemplate() {
        return xlsxTemplateGenerator.generateTemplate();
    }

    // ------------------------------------------------------------------ helpers

    private static void assertFilePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "BULK_IMPORT_EMPTY_FILE", HttpStatus.BAD_REQUEST,
                    "File upload rỗng hoặc không được cung cấp");
        }
    }

    private static void assertRowLimit(int rowCount) {
        if (rowCount > MAX_ROWS) {
            // Wave 86 E-AC5: PAYLOAD_TOO_LARGE (HTTP 413) per spec
            // "HTTP 413 if exceeded; FE chunk client-side if > 1000".
            throw new BusinessException(
                    "BULK_IMPORT_ROW_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE,
                    "Số dòng vượt quá giới hạn " + MAX_ROWS + " (thực tế: " + rowCount + ")");
        }
    }

    /**
     * Canonical content-type for .xlsx (OOXML spreadsheet).
     */
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Rejects uploads that are not .xlsx BEFORE handing the stream to Apache
     * POI. GAP-988: non-xlsx content (CSV, renamed image, corrupt bytes) makes
     * {@link XSSFWorkbook} throw a {@link RuntimeException} that previously
     * escaped to the catch-all handler as HTTP 500. The pre-check turns the
     * common cases into a clean HTTP 415; the POI wrap below catches the rest.
     *
     * <p>Accepts a file when EITHER the extension is {@code .xlsx} OR the
     * content-type is the OOXML spreadsheet type — browsers occasionally send
     * {@code application/octet-stream}, so a correct extension alone is enough.
     */
    private static void assertXlsxType(MultipartFile file) {
        String name = file.getOriginalFilename();
        boolean hasXlsxExtension = name != null
                && name.toLowerCase(Locale.ROOT).endsWith(".xlsx");
        String contentType = file.getContentType();
        boolean hasXlsxContentType = XLSX_CONTENT_TYPE.equalsIgnoreCase(contentType);

        if (!hasXlsxExtension && !hasXlsxContentType) {
            throw new BusinessException(
                    "BULK_IMPORT_INVALID_FILE_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    name == null ? "<unknown>" : name);
        }
    }

    private List<BulkImportRow> parseSafely(MultipartFile file) {
        assertXlsxType(file);
        try (InputStream in = file.getInputStream()) {
            return xlsxParser.parse(in);
        } catch (IOException e) {
            throw new BulkImportParseException("Không đọc được file upload: " + e.getMessage(), e);
        } catch (BulkImportParseException e) {
            // Already a domain 400 (missing headers, empty workbook) — propagate as-is.
            throw e;
        } catch (RuntimeException e) {
            // GAP-988: Apache POI throws RuntimeExceptions (NotOLE2FileException,
            // POIXMLException, generic) when the bytes are not a valid xlsx — e.g.
            // a CSV or image renamed to .xlsx. Map to HTTP 400 instead of 500.
            throw new BulkImportParseException(
                    "File không phải xlsx hợp lệ hoặc đã hỏng: " + e.getMessage(), e);
        }
    }

    private static int countFailedRows(List<RowError> errors) {
        return (int) errors.stream().map(RowError::rowNumber).distinct().count();
    }

    private static List<RowError> truncate(List<RowError> errors) {
        if (errors.size() <= BulkImportResult.MAX_RETURNED_ERRORS) {
            return List.copyOf(errors);
        }
        return List.copyOf(errors.subList(0, BulkImportResult.MAX_RETURNED_ERRORS));
    }

    /**
     * Detects emails or phones that appear more than once in the same uploaded
     * file. The FIRST occurrence is considered valid; subsequent rows with the
     * same email/phone are flagged as errors. Runs entirely in-memory — avoids
     * hitting the DB UNIQUE constraint at insert time (which would bubble up
     * as a 500 instead of a graceful row-level error).
     *
     * <p>Case-insensitive for email; exact match for phone.
     */
    private static List<RowError> detectInFileDuplicates(List<BulkImportRow> rows) {
        List<RowError> errors = new ArrayList<>();
        Map<String, Integer> firstEmailRow = new HashMap<>();
        Map<String, Integer> firstPhoneRow = new HashMap<>();
        for (BulkImportRow row : rows) {
            String email = row.email() == null ? null : row.email().trim().toLowerCase(Locale.ROOT);
            if (email != null && !email.isEmpty()) {
                Integer prev = firstEmailRow.putIfAbsent(email, row.rowNumber());
                if (prev != null) {
                    errors.add(new RowError(row.rowNumber(), "email",
                            "Email trùng với dòng " + prev + " trong cùng file"));
                }
            }
            String phone = row.phone() == null ? null : row.phone().trim();
            if (phone != null && !phone.isEmpty()) {
                Integer prev = firstPhoneRow.putIfAbsent(phone, row.rowNumber());
                if (prev != null) {
                    errors.add(new RowError(row.rowNumber(), "phone",
                            "Số điện thoại trùng với dòng " + prev + " trong cùng file"));
                }
            }
        }
        return errors;
    }
}
