package com.kiteclass.core.module.enrollment.bulkimport.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.enrollment.bulkimport.dto.EnrollmentBulkResult;
import com.kiteclass.core.module.enrollment.bulkimport.dto.EnrollmentBulkRow;
import com.kiteclass.core.module.enrollment.dto.CreateEnrollmentRequest;
import com.kiteclass.core.module.enrollment.service.EnrollmentService;
import com.kiteclass.core.module.student.bulkimport.dto.RowError;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator (Facade) for the enrollment bulk-import feature (GAP-1104).
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #preview(MultipartFile, UUID)} — parse + resolve + field-validate,
 *       no DB writes; surfaces resolution/validation problems before commit.</li>
 *   <li>{@link #commit(MultipartFile, UUID)} — parse + resolve + enroll each
 *       valid row by delegating to {@code EnrollmentService.enrollStudent}.</li>
 * </ul>
 *
 * <p>Mirrors {@code StudentBulkImportService} but delegates each row to the
 * existing single-enroll transaction rather than a chunk executor — every
 * {@code enrollStudent} call runs in its own {@code @Transactional} boundary
 * (Spring proxy), so an invalid row's rollback never affects already-enrolled
 * rows (skip-and-report policy). Business rules (capacity BR-ENROLL-001, duplicate
 * BR-ENROLL-002, discount BR-ENROLL-004, class status) are enforced inside
 * {@code enrollStudent}; preview surfaces only resolution + field validation +
 * in-file duplicates (DB business-rule errors are reported at commit).
 *
 * @author KiteClass Team
 * @since 2.7.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentBulkImportService {

    /**
     * Hard upper bound on rows per upload. Mirrors {@code StudentBulkImportService.MAX_ROWS}
     * (BR-BI-003) — keeps the request within the Phase 1 BETA performance envelope.
     */
    public static final int MAX_ROWS = 1_000;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final EnrollmentXlsxParser xlsxParser;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final EnrollmentService enrollmentService;

    /**
     * Dry-run: parse + resolve + field-validate only. No DB writes.
     *
     * @param file     the uploaded xlsx
     * @param tenantId tenant instance ID (resolves student/class within tenant)
     * @return summary with per-row errors (resolution + field validation + in-file dup)
     */
    public EnrollmentBulkResult preview(MultipartFile file, UUID tenantId) {
        assertFilePresent(file);
        List<EnrollmentBulkRow> rows = parseSafely(file);
        assertRowLimit(rows.size());

        List<RowError> errorList = new ArrayList<>();
        Map<String, Integer> firstSeenPair = new HashMap<>();
        int valid = 0;

        for (EnrollmentBulkRow row : rows) {
            ResolveResult result = resolveAndValidate(row, tenantId);
            if (!result.isValid()) {
                errorList.addAll(result.errors());
                continue;
            }
            String pairKey = result.request().getStudentId() + ":" + result.request().getClassId();
            Integer prev = firstSeenPair.putIfAbsent(pairKey, row.rowNumber());
            if (prev != null) {
                errorList.add(new RowError(row.rowNumber(), "row",
                        "Trùng ghi danh với dòng " + prev + " trong cùng file (cùng học sinh + lớp)"));
                continue;
            }
            valid++;
        }

        int failedRows = countFailedRows(errorList);
        log.info("Bulk-enroll preview: tenantId={}, total={}, valid={}, failedRows={}",
                tenantId, rows.size(), valid, failedRows);
        return new EnrollmentBulkResult(rows.size(), valid, failedRows, truncate(errorList));
    }

    /**
     * Commit phase: parse + resolve + enroll. Valid rows are enrolled via
     * {@code EnrollmentService.enrollStudent}; invalid/failed rows are skipped
     * and reported.
     *
     * @param file     the uploaded xlsx
     * @param tenantId tenant instance ID
     * @return summary with per-row errors and success count
     */
    public EnrollmentBulkResult commit(MultipartFile file, UUID tenantId) {
        assertFilePresent(file);
        List<EnrollmentBulkRow> rows = parseSafely(file);
        assertRowLimit(rows.size());

        List<RowError> allErrors = new ArrayList<>();
        Map<String, Integer> firstSeenPair = new HashMap<>();
        int successCount = 0;

        for (EnrollmentBulkRow row : rows) {
            ResolveResult result = resolveAndValidate(row, tenantId);
            if (!result.isValid()) {
                allErrors.addAll(result.errors());
                continue;
            }

            CreateEnrollmentRequest request = result.request();
            String pairKey = request.getStudentId() + ":" + request.getClassId();
            Integer prev = firstSeenPair.putIfAbsent(pairKey, row.rowNumber());
            if (prev != null) {
                allErrors.add(new RowError(row.rowNumber(), "row",
                        "Trùng ghi danh với dòng " + prev + " trong cùng file (cùng học sinh + lớp)"));
                continue;
            }

            try {
                enrollmentService.enrollStudent(request);
                successCount++;
            } catch (BusinessException ex) {
                allErrors.add(new RowError(row.rowNumber(), "row", mapBusinessError(ex)));
            } catch (RuntimeException ex) {
                log.warn("Bulk-enroll row {} failed unexpectedly: {}", row.rowNumber(), ex.getMessage());
                allErrors.add(new RowError(row.rowNumber(), "row",
                        "Lỗi không xác định khi ghi danh: " + ex.getMessage()));
            }
        }

        int failedRows = countFailedRows(allErrors);
        log.info("Bulk-enroll commit done: tenantId={}, total={}, success={}, failedRows={}",
                tenantId, rows.size(), successCount, failedRows);
        return new EnrollmentBulkResult(rows.size(), successCount, failedRows, truncate(allErrors));
    }

    // ------------------------------------------------------------- resolution

    /**
     * Result of resolving + validating one row.
     *
     * @param request resolved enrollment request, or {@code null} if invalid
     * @param errors  zero or more errors for the row
     */
    private record ResolveResult(CreateEnrollmentRequest request, List<RowError> errors) {
        boolean isValid() {
            return errors.isEmpty() && request != null;
        }
    }

    /**
     * Resolves a row's human keys (student email/phone, class code) to entity IDs
     * within the tenant and validates the amount fields. Collects ALL errors so
     * the user sees a complete picture per row.
     */
    private ResolveResult resolveAndValidate(EnrollmentBulkRow row, UUID tenantId) {
        List<RowError> errors = new ArrayList<>();
        int rowNum = row.rowNumber();

        // Resolve class (required) by code, tenant-scoped.
        Long classId = null;
        String classCode = trimToNull(row.classCode());
        if (classCode == null) {
            errors.add(new RowError(rowNum, "class_code", "Mã lớp (class_code) là bắt buộc"));
        } else {
            Optional<Class> clazz = classRepository
                    .findByClassCodeAndInstanceIdAndDeletedFalse(classCode, tenantId);
            if (clazz.isEmpty()) {
                errors.add(new RowError(rowNum, "class_code",
                        "Không tìm thấy lớp với mã '" + classCode + "'"));
            } else {
                classId = clazz.get().getId();
            }
        }

        // Resolve student by email (preferred) then phone, tenant-scoped.
        Long studentId = null;
        String email = trimToNull(row.studentEmail());
        String phone = trimToNull(row.studentPhone());
        if (email == null && phone == null) {
            errors.add(new RowError(rowNum, "student_email",
                    "Cần email (student_email) hoặc số điện thoại (student_phone) để xác định học sinh"));
        } else {
            Optional<Student> student = Optional.empty();
            if (email != null) {
                student = studentRepository.findByEmailAndInstanceIdAndDeletedFalse(email, tenantId);
            }
            if (student.isEmpty() && phone != null) {
                student = studentRepository.findByPhoneAndInstanceIdAndDeletedFalse(phone, tenantId);
            }
            if (student.isEmpty()) {
                String key = email != null ? "email '" + email + "'" : "số điện thoại '" + phone + "'";
                errors.add(new RowError(rowNum, "student_email",
                        "Không tìm thấy học sinh với " + key));
            } else {
                studentId = student.get().getId();
            }
        }

        // tuition_amount (required, ≥0, digits(8,2))
        BigDecimal tuition = parseAmount(trimToNull(row.tuitionAmount()), rowNum,
                "tuition_amount", "Học phí (tuition_amount)", 8, errors, true);
        if (tuition != null && tuition.signum() < 0) {
            errors.add(new RowError(rowNum, "tuition_amount", "Học phí không được âm"));
            tuition = null;
        }

        // discount_percent (optional, default 0, 0-100, digits(3,2))
        BigDecimal discount = BigDecimal.ZERO;
        String discountRaw = trimToNull(row.discountPercent());
        if (discountRaw != null) {
            discount = parseAmount(discountRaw, rowNum,
                    "discount_percent", "Phần trăm giảm giá (discount_percent)", 3, errors, false);
            if (discount != null && (discount.signum() < 0 || discount.compareTo(HUNDRED) > 0)) {
                errors.add(new RowError(rowNum, "discount_percent",
                        "Phần trăm giảm giá phải từ 0 đến 100"));
                discount = null;
            }
        }

        String note = trimToNull(row.note());
        if (note != null && note.length() > 2000) {
            errors.add(new RowError(rowNum, "note", "Ghi chú không vượt quá 2000 ký tự"));
            note = null;
        }

        if (!errors.isEmpty() || studentId == null || classId == null
                || tuition == null || discount == null) {
            return new ResolveResult(null, errors);
        }

        CreateEnrollmentRequest request = CreateEnrollmentRequest.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(tuition)
                .discountPercent(discount)
                .notes(note)
                .build();
        return new ResolveResult(request, errors);
    }

    /**
     * Parses a numeric string into a {@link BigDecimal}, enforcing the integer +
     * fraction digit envelope. Adds an error (and returns {@code null}) on
     * malformed input or missing required value.
     */
    private BigDecimal parseAmount(String raw, int rowNum, String field, String label,
                                   int maxIntegerDigits, List<RowError> errors, boolean required) {
        if (raw == null) {
            if (required) {
                errors.add(new RowError(rowNum, field, label + " là bắt buộc"));
            }
            return required ? null : BigDecimal.ZERO;
        }
        try {
            BigDecimal value = new BigDecimal(raw);
            BigDecimal stripped = value.stripTrailingZeros();
            int intDigits = stripped.precision() - stripped.scale();
            if (intDigits > maxIntegerDigits || Math.max(stripped.scale(), 0) > 2) {
                errors.add(new RowError(rowNum, field,
                        label + " sai định dạng (tối đa " + maxIntegerDigits + " chữ số phần nguyên, 2 chữ số thập phân)"));
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            errors.add(new RowError(rowNum, field, label + " phải là số hợp lệ"));
            return null;
        }
    }

    /** Maps an enrollStudent {@link BusinessException} code to a VN row message. */
    private static String mapBusinessError(BusinessException ex) {
        String code = ex.getCode();
        return switch (code) {
            case "ENROLLMENT_DUPLICATE" -> "Học sinh đã được ghi danh trong lớp này";
            case "CLASS_FULL" -> "Lớp đã đầy (đạt sĩ số tối đa)";
            case "CLASS_NOT_ENROLLABLE" -> "Lớp không thể ghi danh (đã hoàn thành hoặc đã hủy)";
            case "STUDENT_NOT_FOUND" -> "Không tìm thấy học sinh";
            case "CLASS_NOT_FOUND" -> "Không tìm thấy lớp";
            default -> "Ghi danh thất bại: " + code;
        };
    }

    // ------------------------------------------------------------------ helpers

    private void assertFilePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "ENROLLMENT_BULK_IMPORT_EMPTY_FILE", HttpStatus.BAD_REQUEST,
                    "File upload rỗng hoặc không được cung cấp");
        }
    }

    private void assertRowLimit(int rowCount) {
        if (rowCount > MAX_ROWS) {
            throw new BusinessException(
                    "ENROLLMENT_BULK_IMPORT_ROW_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE,
                    "Số dòng vượt quá giới hạn " + MAX_ROWS + " (thực tế: " + rowCount + ")");
        }
    }

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Rejects non-xlsx uploads before handing the stream to Apache POI — mirrors
     * the student bulk-import guard so corrupt/renamed files map to HTTP 415
     * instead of an unexpected 500.
     */
    private void assertXlsxType(MultipartFile file) {
        String name = file.getOriginalFilename();
        boolean hasXlsxExtension = name != null
                && name.toLowerCase(Locale.ROOT).endsWith(".xlsx");
        boolean hasXlsxContentType = XLSX_CONTENT_TYPE.equalsIgnoreCase(file.getContentType());
        if (!hasXlsxExtension && !hasXlsxContentType) {
            throw new BusinessException(
                    "ENROLLMENT_BULK_IMPORT_INVALID_FILE_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    name == null ? "<unknown>" : name);
        }
    }

    private List<EnrollmentBulkRow> parseSafely(MultipartFile file) {
        assertXlsxType(file);
        try (InputStream in = file.getInputStream()) {
            return xlsxParser.parse(in);
        } catch (IOException e) {
            throw new EnrollmentBulkImportParseException("Không đọc được file upload: " + e.getMessage(), e);
        } catch (EnrollmentBulkImportParseException e) {
            throw e;
        } catch (RuntimeException e) {
            // Apache POI throws RuntimeExceptions when the bytes are not a valid
            // xlsx (CSV/image renamed to .xlsx). Map to HTTP 400 instead of 500.
            throw new EnrollmentBulkImportParseException(
                    "File không phải xlsx hợp lệ hoặc đã hỏng: " + e.getMessage(), e);
        }
    }

    private static int countFailedRows(List<RowError> errors) {
        return (int) errors.stream().map(RowError::rowNumber).distinct().count();
    }

    private static List<RowError> truncate(List<RowError> errors) {
        if (errors.size() <= EnrollmentBulkResult.MAX_RETURNED_ERRORS) {
            return List.copyOf(errors);
        }
        return List.copyOf(errors.subList(0, EnrollmentBulkResult.MAX_RETURNED_ERRORS));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
