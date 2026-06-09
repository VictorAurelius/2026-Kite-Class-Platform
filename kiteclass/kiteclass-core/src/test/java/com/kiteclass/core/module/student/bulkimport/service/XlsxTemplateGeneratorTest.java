package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.module.student.bulkimport.dto.BulkImportRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link XlsxTemplateGenerator} (GAP-1102).
 *
 * <p>The key contract is a round-trip: the generated template MUST parse cleanly
 * via {@link XlsxParser} AND its example rows MUST be VALID per {@link RowValidator}
 * so users can copy the pattern without producing errors on re-upload.
 */
class XlsxTemplateGeneratorTest {

    private final XlsxTemplateGenerator generator = new XlsxTemplateGenerator();
    private final XlsxParser parser = new XlsxParser();
    private final RowValidator rowValidator = new RowValidator();

    @Test
    @DisplayName("generateTemplate() produces non-empty xlsx bytes")
    void generatesNonEmptyBytes() {
        byte[] bytes = generator.generateTemplate();
        assertThat(bytes).isNotEmpty();
    }

    @Test
    @DisplayName("Generated template parses cleanly via XlsxParser — no exception, ≥2 example rows")
    void templateRoundTripsThroughParser() {
        byte[] bytes = generator.generateTemplate();

        assertThatCode(() -> parser.parse(new ByteArrayInputStream(bytes)))
                .doesNotThrowAnyException();

        List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(bytes));

        // Header row (name + email required) resolved → example rows surfaced.
        assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
        // name + email present on every example row (required columns resolved).
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.name()).isNotBlank();
            assertThat(row.email()).isNotBlank();
        });
    }

    @Test
    @DisplayName("Every example row passes RowValidator with ZERO errors (copy-safe pattern)")
    void exampleRowsAreValid() {
        byte[] bytes = generator.generateTemplate();
        List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(bytes));

        assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
        for (BulkImportRow row : rows) {
            RowValidator.ValidationResult result = rowValidator.validate(row);
            assertThat(result.errors())
                    .as("example row %d (%s) should be valid", row.rowNumber(), row.name())
                    .isEmpty();
            assertThat(result.isValid())
                    .as("example row %d (%s) should be valid", row.rowNumber(), row.name())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("First example row carries all 7 columns; second leaves optional address/note blank")
    void exampleRowsCoverRequiredAndOptionalFields() {
        byte[] bytes = generator.generateTemplate();
        List<BulkImportRow> rows = parser.parse(new ByteArrayInputStream(bytes));

        BulkImportRow first = rows.get(0);
        assertThat(first.name()).isEqualTo("Nguyễn Văn An");
        assertThat(first.email()).isEqualTo("an.nguyen@example.com");
        assertThat(first.phone()).isEqualTo("0901234567"); // leading zero preserved
        assertThat(first.dateOfBirth()).isEqualTo("15/03/2010"); // dd/MM/yyyy preserved
        assertThat(first.gender()).isEqualTo("MALE");
        assertThat(first.address()).isNotBlank();
        assertThat(first.note()).isNotBlank();

        BulkImportRow second = rows.get(1);
        assertThat(second.name()).isEqualTo("Trần Thị Bình");
        assertThat(second.gender()).isEqualTo("FEMALE");
        // optional address + note left blank → parser returns null (still valid)
        assertThat(second.address()).isNull();
        assertThat(second.note()).isNull();
    }
}
