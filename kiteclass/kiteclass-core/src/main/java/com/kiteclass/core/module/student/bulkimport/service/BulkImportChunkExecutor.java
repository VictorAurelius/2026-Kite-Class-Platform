package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import com.kiteclass.core.module.student.bulkimport.dto.RowError;
import com.kiteclass.core.module.student.bulkimport.entity.BulkImportJob;
import com.kiteclass.core.module.student.bulkimport.entity.BulkImportStatus;
import com.kiteclass.core.module.student.bulkimport.repository.BulkImportJobRepository;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Separate Spring-managed bean for the write-path operations of the bulk
 * importer. This lives in its own class so its {@link Transactional} methods
 * can be invoked through the Spring proxy from {@link StudentBulkImportService}
 * (self-invocation from the same class would bypass transactional advice).
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportChunkExecutor {

    private final RowValidator rowValidator;
    private final StudentService studentService;
    private final BulkImportJobRepository jobRepository;

    /**
     * Persists a fresh {@link BulkImportJob} row in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BulkImportJob createJob(String filename, UUID tenantId, int totalRows) {
        BulkImportJob job = BulkImportJob.builder()
                .filename(filename != null ? filename : "unknown.xlsx")
                .status(BulkImportStatus.IN_PROGRESS)
                .totalRows(totalRows)
                .successCount(0)
                .errorCount(0)
                .build();
        job.setInstanceId(tenantId);
        return jobRepository.save(job);
    }

    /**
     * Updates the job with the final counts and transitions it to
     * {@link BulkImportStatus#COMPLETED}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeJob(Long jobId, UUID tenantId, int successCount, int errorCount) {
        jobRepository.findByIdAndInstanceIdAndDeletedFalse(jobId, tenantId).ifPresent(fresh -> {
            fresh.setSuccessCount(successCount);
            fresh.setErrorCount(errorCount);
            fresh.setStatus(BulkImportStatus.COMPLETED);
            fresh.setCompletedAt(Instant.now());
            jobRepository.save(fresh);
        });
    }

    /**
     * Processes one chunk of rows in its own transaction. Invalid rows and
     * duplicate-collision errors are collected as {@link RowError}s rather than
     * aborting the chunk.
     *
     * @param chunk    rows to process
     * @param tenantId tenant instance ID
     * @return tuple of success count and per-row errors
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChunkResult processChunk(List<BulkImportRow> chunk, UUID tenantId) {
        int success = 0;
        List<RowError> errors = new ArrayList<>();
        for (BulkImportRow row : chunk) {
            RowValidator.ValidationResult result = rowValidator.validate(row);
            if (!result.isValid()) {
                errors.addAll(result.errors());
                continue;
            }
            try {
                CreateStudentRequest request = result.request();
                studentService.createStudent(request, tenantId);
                success++;
            } catch (DuplicateResourceException dup) {
                errors.add(toRowError(row.rowNumber(), dup));
            } catch (BusinessException be) {
                errors.add(new RowError(row.rowNumber(), "row",
                        "Không tạo được học viên: " + be.getCode()));
            } catch (RuntimeException e) {
                log.warn("Unexpected error importing row {}: {}", row.rowNumber(), e.toString());
                errors.add(new RowError(row.rowNumber(), "row",
                        "Lỗi không xác định: " + e.getClass().getSimpleName()));
            }
        }
        return new ChunkResult(success, errors);
    }

    private static RowError toRowError(int rowNumber, DuplicateResourceException dup) {
        String code = dup.getCode();
        if ("STUDENT_EMAIL_EXISTS".equals(code)) {
            return new RowError(rowNumber, "email", "Email đã tồn tại trong hệ thống");
        }
        if ("STUDENT_PHONE_EXISTS".equals(code)) {
            return new RowError(rowNumber, "phone", "Số điện thoại đã tồn tại trong hệ thống");
        }
        return new RowError(rowNumber, "row", "Trùng lặp dữ liệu: " + code);
    }

    /**
     * Per-chunk aggregation tuple.
     *
     * @param successCount rows created successfully in this chunk
     * @param errors       row-level errors (validation + duplicate) for this chunk
     */
    public record ChunkResult(int successCount, List<RowError> errors) {
    }
}
