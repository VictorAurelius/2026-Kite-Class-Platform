package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import com.kiteclass.core.module.student.bulkimport.dto.RowError;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates a single {@link BulkImportRow} and converts it to a
 * {@link CreateStudentRequest} ready to be passed to {@code StudentService}.
 *
 * <p>Validation rules mirror {@link CreateStudentRequest}:
 * <ul>
 *   <li>{@code name}: required, 2–100 chars</li>
 *   <li>{@code email}: required, RFC-ish email</li>
 *   <li>{@code phone}: optional, {@code ^0\d{9}$} when present</li>
 *   <li>{@code dateOfBirth}: optional, {@code dd/MM/yyyy}, past-or-present</li>
 *   <li>{@code gender}: optional, MALE/FEMALE/OTHER (case-insensitive)</li>
 * </ul>
 *
 * <p>All field errors for a given row are collected — we don't stop after the
 * first failure so the user gets a complete picture in the error report.
 *
 * @author KiteClass Team
 * @since 2.4.0
 */
@Slf4j
@Component
public class RowValidator {

    /** Lenient RFC-5322-ish check; matches Hibernate validator's @Email default. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    /** dd/MM/yyyy — user-friendly Vietnamese date format. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Result of validating one row.
     *
     * @param request validated payload, or {@code null} if validation failed
     * @param errors  zero or more field errors for the row
     */
    public record ValidationResult(CreateStudentRequest request, List<RowError> errors) {

        public boolean isValid() {
            return errors.isEmpty() && request != null;
        }
    }

    /**
     * Validates the given row and, if valid, produces the matching
     * {@link CreateStudentRequest}.
     *
     * @param row the raw parsed row
     * @return validation result (either a request or a list of errors)
     */
    public ValidationResult validate(BulkImportRow row) {
        List<RowError> errors = new ArrayList<>();
        int rowNum = row.rowNumber();

        // name
        String name = trimToNull(row.name());
        if (name == null) {
            errors.add(new RowError(rowNum, "name", "Tên là bắt buộc"));
        } else if (name.length() < 2 || name.length() > 100) {
            errors.add(new RowError(rowNum, "name", "Tên phải từ 2-100 ký tự"));
            name = null; // don't propagate invalid value
        }

        // email
        String email = trimToNull(row.email());
        if (email == null) {
            errors.add(new RowError(rowNum, "email", "Email là bắt buộc"));
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(new RowError(rowNum, "email", "Email không hợp lệ"));
            email = null;
        } else if (email.length() > 255) {
            errors.add(new RowError(rowNum, "email", "Email không vượt quá 255 ký tự"));
            email = null;
        }

        // phone (optional)
        String phone = trimToNull(row.phone());
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(new RowError(rowNum, "phone",
                    "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)"));
            phone = null;
        }

        // dateOfBirth (optional)
        LocalDate dob = null;
        String dobRaw = trimToNull(row.dateOfBirth());
        if (dobRaw != null) {
            try {
                dob = LocalDate.parse(dobRaw, DATE_FORMATTER);
                if (dob.isAfter(LocalDate.now())) {
                    errors.add(new RowError(rowNum, "date_of_birth",
                            "Ngày sinh không thể là ngày trong tương lai"));
                    dob = null;
                }
            } catch (DateTimeParseException e) {
                errors.add(new RowError(rowNum, "date_of_birth",
                        "Ngày sinh không đúng định dạng dd/MM/yyyy"));
            }
        }

        // gender (optional)
        Gender gender = null;
        String genderRaw = trimToNull(row.gender());
        if (genderRaw != null) {
            Optional<Gender> parsed = parseGender(genderRaw);
            if (parsed.isEmpty()) {
                errors.add(new RowError(rowNum, "gender",
                        "Giới tính chỉ nhận MALE hoặc FEMALE"));
            } else {
                gender = parsed.get();
            }
        }

        // address (optional, max 1000 to match CreateStudentRequest)
        String address = trimToNull(row.address());
        if (address != null && address.length() > 1000) {
            errors.add(new RowError(rowNum, "address",
                    "Địa chỉ không vượt quá 1000 ký tự"));
            address = null;
        }

        String note = trimToNull(row.note());

        if (!errors.isEmpty()) {
            return new ValidationResult(null, errors);
        }

        CreateStudentRequest request = new CreateStudentRequest(
                name, email, phone, dob, gender, address, note);
        return new ValidationResult(request, errors);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Accepts MALE/FEMALE/OTHER case-insensitively. {@code OTHER} is absent
     * from the current {@link Gender} enum (MALE/FEMALE only) so it is rejected
     * with the same error as any invalid value — callers may extend the enum
     * later without breaking the import contract.
     */
    private static Optional<Gender> parseGender(String raw) {
        String upper = raw.toUpperCase(Locale.ROOT);
        for (Gender g : Gender.values()) {
            if (g.name().equals(upper)) {
                return Optional.of(g);
            }
        }
        return Optional.empty();
    }
}
