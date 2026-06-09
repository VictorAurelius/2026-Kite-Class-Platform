package com.kiteclass.core.module.student.bulkimport.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.student.bulkimport.dto.BulkImportResult;
import com.kiteclass.core.module.student.bulkimport.entity.BulkImportJob;
import com.kiteclass.core.module.student.bulkimport.entity.BulkImportStatus;
import com.kiteclass.core.module.student.bulkimport.repository.BulkImportJobRepository;
import com.kiteclass.core.module.student.dto.CreateStudentRequest;
import com.kiteclass.core.module.student.service.StudentService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StudentBulkImportService}. Heavy collaborators
 * ({@link BulkImportChunkExecutor}, {@link StudentService}, repository) are
 * mocked — we verify orchestration, error aggregation, and pre-conditions.
 */
@ExtendWith(MockitoExtension.class)
class StudentBulkImportServiceTest {

    @Mock
    private StudentService studentService;

    @Mock
    private BulkImportJobRepository jobRepository;

    @InjectMocks
    private BulkImportChunkExecutor chunkExecutor;

    private StudentBulkImportService service;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        XlsxParser parser = new XlsxParser();
        RowValidator validator = new RowValidator();
        ErrorReportGenerator generator = new ErrorReportGenerator();
        XlsxTemplateGenerator templateGenerator = new XlsxTemplateGenerator();
        // Manually build service — @InjectMocks is already used for the chunk executor.
        chunkExecutor = new BulkImportChunkExecutor(validator, studentService, jobRepository);
        service = new StudentBulkImportService(parser, validator, chunkExecutor, generator, templateGenerator);
    }

    @Test
    @DisplayName("preview() does not call StudentService")
    void previewDoesNotWriteToDatabase() throws IOException {
        MockMultipartFile file = file(new String[][]{
                {"name", "email", "phone"},
                {"Alice", "alice@test.com", "0901111111"}
        });

        BulkImportResult result = service.preview(file, tenantId);

        assertThat(result.jobId()).isNull();
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isZero();
        verify(studentService, times(0)).createStudent(any(), any());
        verify(jobRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("commit() batches valid rows to StudentService and persists a job row")
    void commitCreatesValidRows() throws IOException {
        MockMultipartFile file = file(new String[][]{
                {"name", "email", "phone"},
                {"Alice", "alice@test.com", "0901111111"},
                {"Bob", "bob@test.com", "0902222222"},
                {"Cara", "cara@test.com", "0903333333"}
        });

        when(jobRepository.save(any())).thenAnswer(inv -> {
            BulkImportJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(42L);
            }
            return job;
        });
        when(jobRepository.findByIdAndInstanceIdAndDeletedFalse(eq(42L), any()))
                .thenAnswer(inv -> java.util.Optional.of(
                        BulkImportJob.builder()
                                .filename("test.xlsx")
                                .status(BulkImportStatus.IN_PROGRESS)
                                .totalRows(3)
                                .successCount(0)
                                .errorCount(0)
                                .build()));
        when(studentService.createStudent(any(CreateStudentRequest.class), eq(tenantId)))
                .thenReturn(stubResponse());

        BulkImportResult result = service.commit(file, tenantId);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.errorCount()).isZero();
        assertThat(result.jobId()).isEqualTo(42L);
        verify(studentService, times(3)).createStudent(any(), eq(tenantId));
    }

    @Test
    @DisplayName("commit() reports in-file duplicate-email as row error (no DB call for dup row)")
    void commitRecordsDuplicateEmailErrors() throws IOException {
        MockMultipartFile file = file(new String[][]{
                {"name", "email", "phone"},
                {"Alice", "alice@test.com", "0901111111"},
                {"Duplicate", "alice@test.com", "0902222222"}
        });

        when(jobRepository.save(any())).thenAnswer(inv -> {
            BulkImportJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(7L);
            }
            return job;
        });
        when(jobRepository.findByIdAndInstanceIdAndDeletedFalse(eq(7L), any()))
                .thenAnswer(inv -> java.util.Optional.of(BulkImportJob.builder().build()));

        // Only called once — the duplicate row is filtered BEFORE reaching studentService
        when(studentService.createStudent(any(CreateStudentRequest.class), eq(tenantId)))
                .thenReturn(stubResponse());

        BulkImportResult result = service.commit(file, tenantId);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).field()).isEqualTo("email");
        assertThat(result.errors().get(0).message()).contains("trùng");
        verify(studentService, times(1)).createStudent(any(), eq(tenantId));
    }

    @Test
    @DisplayName("commit() aggregates validation errors (invalid rows skipped, not aborting)")
    void commitAggregatesValidationErrors() throws IOException {
        MockMultipartFile file = file(new String[][]{
                {"name", "email", "phone"},
                {"Alice", "alice@test.com", "0901111111"},
                {"", "bad-email", "phone-bad"}, // 3 invalid fields
                {"Cara", "cara@test.com", "0903333333"}
        });

        when(jobRepository.save(any())).thenAnswer(inv -> {
            BulkImportJob job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(99L);
            }
            return job;
        });
        when(jobRepository.findByIdAndInstanceIdAndDeletedFalse(eq(99L), any()))
                .thenAnswer(inv -> java.util.Optional.of(BulkImportJob.builder().build()));
        when(studentService.createStudent(any(CreateStudentRequest.class), eq(tenantId)))
                .thenReturn(stubResponse());

        BulkImportResult result = service.commit(file, tenantId);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(2);
        // The middle row produced 3 field errors, so errors list has 3 items but failedRows = 1
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.errors()).extracting(err -> err.field())
                .contains("name", "email", "phone");
        verify(studentService, times(2)).createStudent(any(), any());
    }

    @Test
    @DisplayName("commit() rejects empty file")
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/xlsx", new byte[0]);

        assertThatThrownBy(() -> service.commit(file, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BULK_IMPORT_EMPTY_FILE");
    }

    @Test
    @DisplayName("commit() rejects non-xlsx extension with HTTP 415 (GAP-988)")
    void rejectsNonXlsxExtension() {
        // CSV content + .csv extension + text/csv content-type → 415, not 500.
        MockMultipartFile file = new MockMultipartFile("file", "students.csv",
                "text/csv", "name,email\nAlice,alice@test.com\n".getBytes());

        assertThatThrownBy(() -> service.commit(file, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BULK_IMPORT_INVALID_FILE_TYPE")
                .extracting("status")
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(jobRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("commit() rejects image renamed to .xlsx with HTTP 400, not 500 (GAP-988)")
    void rejectsRenamedImageAsXlsx() {
        // PNG magic bytes but a .xlsx extension → passes the type pre-check (extension OK)
        // then Apache POI throws a RuntimeException, which must map to 400 (not 500).
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("file", "logo.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", pngBytes);

        assertThatThrownBy(() -> service.commit(file, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BULK_IMPORT_PARSE_ERROR")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(jobRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("preview() rejects corrupt-bytes .xlsx with HTTP 400, not 500 (GAP-988)")
    void rejectsCorruptXlsxOnPreview() {
        // Random non-zip bytes with a valid extension → POI RuntimeException → 400.
        MockMultipartFile file = new MockMultipartFile("file", "garbage.xlsx",
                "application/octet-stream", "this is not a real spreadsheet".getBytes());

        assertThatThrownBy(() -> service.preview(file, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BULK_IMPORT_PARSE_ERROR")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("commit() rejects files exceeding MAX_ROWS with HTTP 413 PAYLOAD_TOO_LARGE")
    void rejectsOverMaxRows() throws IOException {
        // Wave 86 E-AC5: MAX_ROWS=1_000 cap with PAYLOAD_TOO_LARGE (413) status
        // (spec: "HTTP 413 if exceeded; FE chunk client-side if > 1000")
        int dataRows = StudentBulkImportService.MAX_ROWS + 1;
        assertThat(StudentBulkImportService.MAX_ROWS)
                .as("Wave 86 E-AC5 cap")
                .isEqualTo(1_000);

        String[][] data = new String[dataRows + 1][];
        data[0] = new String[]{"name", "email"};
        for (int i = 1; i <= dataRows; i++) {
            data[i] = new String[]{"Student " + i, "s" + i + "@test.com"};
        }
        MockMultipartFile file = file(data);

        assertThatThrownBy(() -> service.commit(file, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BULK_IMPORT_ROW_LIMIT_EXCEEDED")
                .extracting("status")
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verify(jobRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("generateErrorReport() produces a non-empty xlsx byte array")
    void generateErrorReportProducesBytes() throws IOException {
        MockMultipartFile file = file(new String[][]{
                {"name", "email"},
                {"", "bad-email"}
        });

        byte[] xlsx = service.generateErrorReport(file);

        assertThat(xlsx).isNotEmpty();
        // xlsx files start with PK (ZIP magic bytes)
        assertThat(xlsx[0]).isEqualTo((byte) 'P');
        assertThat(xlsx[1]).isEqualTo((byte) 'K');
    }

    // ------------------------------------------------------------ helpers

    private static MockMultipartFile file(String[][] rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                String[] cols = rows[r];
                for (int c = 0; c < cols.length; c++) {
                    row.createCell(c).setCellValue(cols[c]);
                }
            }
            wb.write(out);
            return new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private static com.kiteclass.core.module.student.dto.StudentResponse stubResponse() {
        return new com.kiteclass.core.module.student.dto.StudentResponse(
                1L, "x", "x@x.com", null, null, null, null, null, null, null);
    }
}
