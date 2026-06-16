package com.kitehub.branding.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-1437 — {@code LogoAnalysis} required-field validation. Null/blank required fields must raise
 * constraint violations so {@code @Valid @RequestBody} on {@code /generate-theme} yields HTTP 400
 * instead of dereferencing nulls in {@code ThemeGenerationService} → NPE → HTTP 500.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@DisplayName("LogoAnalysis — required-field validation (GAP-1437)")
class LogoAnalysisValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("Empty body (all null required fields) → constraint violations present")
    void nullRequiredFieldsRaiseViolations() {
        LogoAnalysis empty = new LogoAnalysis();

        Set<?> violations = validator.validate(empty);

        // primaryColor, secondaryColor, accentColor, theme are @NotBlank → 4 violations minimum.
        assertThat(violations).hasSize(4);
    }

    @Test
    @DisplayName("Blank required field → violation on that field")
    void blankPrimaryColorRaisesViolation() {
        LogoAnalysis blank = LogoAnalysis.builder()
                .primaryColor("   ")
                .secondaryColor("#1E40AF").accentColor("#F59E0B").theme("MODERN")
                .build();

        var violations = validator.validate(blank);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("primaryColor"));
    }

    @Test
    @DisplayName("Valid body → no violations (no regression)")
    void validBodyHasNoViolations() {
        LogoAnalysis valid = LogoAnalysis.builder()
                .primaryColor("#2563EB").secondaryColor("#1E40AF").accentColor("#F59E0B")
                .theme("MODERN").typography("Sans").targetAudience("students")
                .build();

        Set<?> violations = validator.validate(valid);

        assertThat(violations).isEmpty();
    }
}
