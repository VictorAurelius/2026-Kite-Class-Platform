package com.kiteclass.core.module.document.xlsx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Utility to (re)generate {@code document-samples/attendance-sample.xlsx}. Remove the
 * {@code @Disabled} locally, run with {@code -Dtest=AttendanceSampleEmitter#emit}, commit the
 * output, then restore the annotation.
 */
@Disabled("Utility; remove annotation locally when regenerating attendance-sample.xlsx")
class AttendanceSampleEmitter {

    @Test
    void emit() throws Exception {
        DocumentRequest req = DocumentRequest.builder()
                .format(DocumentFormat.XLSX)
                .templateId("attendance")
                .tenantId("tenant-sample")
                .data(Map.of(
                        "weekStart", "2026-04-20",
                        "className", "10A1",
                        "students", List.of(
                                Map.of("id", "S001", "name", "Nguyễn Văn Đức"),
                                Map.of("id", "S002", "name", "Trần Thị Ánh"),
                                Map.of("id", "S003", "name", "Lê Minh Hằng")),
                        "attendance", Map.of(
                                "S001", Map.of("Thứ 2", "P", "Thứ 3", "P", "Thứ 4", "A",
                                        "Thứ 5", "P", "Thứ 6", "P", "Thứ 7", "L"),
                                "S002", Map.of("Thứ 2", "P", "Thứ 3", "P", "Thứ 4", "P",
                                        "Thứ 5", "P", "Thứ 6", "P", "Thứ 7", "P"),
                                "S003", Map.of("Thứ 2", "A", "Thứ 3", "A", "Thứ 4", "P",
                                        "Thứ 5", "P", "Thứ 6", "P", "Thứ 7", "P"))))
                .build();
        DocumentResponse resp = new XlsxGenerator().generate(req);
        Path out = Path.of("src/test/resources/document-samples/attendance-sample.xlsx");
        Files.write(out, resp.bytes());
    }
}
