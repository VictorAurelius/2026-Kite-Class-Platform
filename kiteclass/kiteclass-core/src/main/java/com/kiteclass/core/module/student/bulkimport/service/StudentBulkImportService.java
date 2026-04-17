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
import java.util.List;
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

    /** Hard upper bound on rows per upload. */
    public static final int MAX_ROWS = 10_000;

    /** Rows persisted per transaction. */
    public static final int CHUNK_SIZE = 500;

    private final XlsxParser xlsxParser;
    private final RowValidator rowValidator;
    private final BulkImportChunkExecutor chunkExecutor;
    private final ErrorReportGenerator errorReportGenerator;

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
        int successCount = 0;

        for (int from = 0; from < rows.size(); from += CHUNK_SIZE) {
            int to = Math.min(from + CHUNK_SIZE, rows.size());
            List<BulkImportRow> chunk = rows.subList(from, to);
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
        return errorReportGenerator.generate(rows, errors);
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
            throw new BusinessException(
                    "BULK_IMPORT_ROW_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST,
                    "Số dòng vượt quá giới hạn " + MAX_ROWS + " (thực tế: " + rowCount + ")");
        }
    }

    private List<BulkImportRow> parseSafely(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return xlsxParser.parse(in);
        } catch (IOException e) {
            throw new BulkImportParseException("Không đọc được file upload: " + e.getMessage(), e);
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
}
