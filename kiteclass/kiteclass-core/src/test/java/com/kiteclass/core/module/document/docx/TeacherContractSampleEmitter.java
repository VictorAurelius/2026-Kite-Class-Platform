package com.kiteclass.core.module.document.docx;

import com.kiteclass.core.module.document.DocumentFormat;
import com.kiteclass.core.module.document.DocumentRequest;
import com.kiteclass.core.module.document.DocumentResponse;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Utility to (re)generate {@code document-samples/teacher-contract-sample.docx}. Remove the
 * {@code @Disabled} locally, run with {@code -Dtest=TeacherContractSampleEmitter#emit}, commit the
 * output, then restore the annotation.
 */
@Disabled("Utility; remove annotation locally when regenerating teacher-contract-sample.docx")
class TeacherContractSampleEmitter {

    @Test
    void emit() throws Exception {
        DocumentRequest req = DocumentRequest.builder()
                .format(DocumentFormat.DOCX)
                .templateId("teacher-contract")
                .tenantId("tenant-sample")
                .data(Map.of(
                        "teacherName", "Nguyễn Văn Đức",
                        "teacherIdNumber", "012345678",
                        "tenantName", "Trường THPT Lê Quý Đôn",
                        "tenantAddress", "12 Đường Lê Lợi, Quận 1, TP. Hồ Chí Minh",
                        "startDate", "2026-05-01",
                        "endDate", "2027-04-30",
                        "salaryVnd", new BigDecimal("15000000"),
                        "subjects", "Toán, Vật lý"))
                .build();
        DocumentResponse resp = new DocxGenerator().generate(req);
        Path out = Path.of("src/test/resources/document-samples/teacher-contract-sample.docx");
        Files.write(out, resp.bytes());
    }
}
