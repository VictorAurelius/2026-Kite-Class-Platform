package com.kiteclass.core.module.marketing.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bean-validation contract for the public contact form (GAP-1221).
 * VN parents thường chỉ để lại SĐT — email và subject là optional;
 * email vẫn phải đúng format KHI có (BR-MKT-001 v2).
 */
class CreateContactMessageRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void nameAndMessageOnly_shouldHaveNoViolations() {
        CreateContactMessageRequest request = CreateContactMessageRequest.builder()
                .name("Chị Trần Thị Hồng")
                .phone("0934567890")
                .message("Cho tôi hỏi lịch học thử lớp 5.")
                .build();

        Set<ConstraintViolation<CreateContactMessageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void invalidEmailFormat_shouldStillFail() {
        CreateContactMessageRequest request = CreateContactMessageRequest.builder()
                .name("Chị Trần Thị Hồng")
                .email("not-an-email")
                .message("Cho tôi hỏi lịch học thử lớp 5.")
                .build();

        Set<ConstraintViolation<CreateContactMessageRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("email");
    }

    @Test
    void blankName_shouldFail() {
        CreateContactMessageRequest request = CreateContactMessageRequest.builder()
                .message("Cho tôi hỏi lịch học thử lớp 5.")
                .build();

        Set<ConstraintViolation<CreateContactMessageRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("name");
    }
}
