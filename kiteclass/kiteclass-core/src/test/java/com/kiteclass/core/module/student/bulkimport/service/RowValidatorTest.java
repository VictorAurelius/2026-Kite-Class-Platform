package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.common.constant.Gender;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import com.kiteclass.core.module.student.bulkimport.dto.RowError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RowValidator}.
 */
class RowValidatorTest {

    private final RowValidator validator = new RowValidator();

    @Test
    @DisplayName("Valid row with all fields filled in")
    void validatesValidRow() {
        BulkImportRow row = new BulkImportRow(2, "Nguyen Van A", "a@test.com",
                "0901234567", "15/05/2010", "MALE", "HCM", "note");

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.request().name()).isEqualTo("Nguyen Van A");
        assertThat(result.request().email()).isEqualTo("a@test.com");
        assertThat(result.request().phone()).isEqualTo("0901234567");
        assertThat(result.request().dateOfBirth()).isEqualTo(LocalDate.of(2010, 5, 15));
        assertThat(result.request().gender()).isEqualTo(Gender.MALE);
    }

    @Test
    @DisplayName("Valid row with only required fields (email + name)")
    void validatesMinimalRow() {
        BulkImportRow row = new BulkImportRow(3, "Alice", "alice@test.com",
                null, null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isTrue();
        assertThat(result.request().phone()).isNull();
        assertThat(result.request().dateOfBirth()).isNull();
        assertThat(result.request().gender()).isNull();
    }

    @Test
    @DisplayName("Missing name → error")
    void rejectsMissingName() {
        BulkImportRow row = new BulkImportRow(2, null, "a@test.com",
                null, null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("name");
    }

    @Test
    @DisplayName("Missing email → error")
    void rejectsMissingEmail() {
        BulkImportRow row = new BulkImportRow(2, "Alice", null,
                null, null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("email");
    }

    @Test
    @DisplayName("Invalid email format → error")
    void rejectsInvalidEmail() {
        BulkImportRow row = new BulkImportRow(2, "Alice", "not-an-email",
                null, null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("email");
    }

    @Test
    @DisplayName("Invalid phone (too short) → error")
    void rejectsInvalidPhone() {
        BulkImportRow row = new BulkImportRow(2, "Alice", "a@test.com",
                "123", null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("phone");
    }

    @Test
    @DisplayName("Phone not starting with 0 → error")
    void rejectsPhoneNotStartingWithZero() {
        BulkImportRow row = new BulkImportRow(2, "Alice", "a@test.com",
                "1901234567", null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("phone");
    }

    @Test
    @DisplayName("Bad date format → error")
    void rejectsBadDateFormat() {
        BulkImportRow row = new BulkImportRow(2, "Alice", "a@test.com",
                null, "2010-05-15", null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("date_of_birth");
    }

    @Test
    @DisplayName("Future date of birth → error")
    void rejectsFutureDateOfBirth() {
        String future = LocalDate.now().plusYears(1).format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        BulkImportRow row = new BulkImportRow(2, "Alice", "a@test.com",
                null, future, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("date_of_birth");
    }

    @Test
    @DisplayName("Gender is case-insensitive (male → MALE)")
    void parsesGenderCaseInsensitive() {
        BulkImportRow row = new BulkImportRow(2, "Alice", "a@test.com",
                null, null, "female", null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isTrue();
        assertThat(result.request().gender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    @DisplayName("Invalid gender → error")
    void rejectsInvalidGender() {
        BulkImportRow row = new BulkImportRow(2, "Alice", "a@test.com",
                null, null, "UNKNOWN", null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("gender");
    }

    @Test
    @DisplayName("Multiple errors aggregated in one row")
    void aggregatesMultipleErrors() {
        BulkImportRow row = new BulkImportRow(2, "A", "bad-email",
                "bad-phone", "bad-date", "bad-gender", null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        List<RowError> errs = result.errors();
        assertThat(errs).extracting(RowError::field)
                .contains("name", "email", "phone", "date_of_birth", "gender");
    }

    @Test
    @DisplayName("Name too short → error (1 char)")
    void rejectsNameTooShort() {
        BulkImportRow row = new BulkImportRow(2, "A", "a@test.com",
                null, null, null, null, null);

        RowValidator.ValidationResult result = validator.validate(row);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(RowError::field).contains("name");
    }
}
