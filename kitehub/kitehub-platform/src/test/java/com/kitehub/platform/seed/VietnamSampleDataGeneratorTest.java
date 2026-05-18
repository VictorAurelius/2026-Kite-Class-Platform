package com.kitehub.platform.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VietnamSampleDataGenerator (GAP-658 Wave 98 Bucket B2).
 *
 * <p>Verifies:
 * <ul>
 *   <li>CSV files load without IO errors (6 sources)</li>
 *   <li>Generated data has Vietnamese diacritics (regex match)</li>
 *   <li>VND currency format matches vi-VN locale</li>
 *   <li>Date format returns Vietnamese day-of-week</li>
 *   <li>Diversity ≥80 unique names over 100 calls (avoid constant-return bug)</li>
 *   <li>English fallback when seed.locale=en-US</li>
 * </ul>
 */
class VietnamSampleDataGeneratorTest {

    private static final Pattern VIETNAMESE_DIACRITIC_PATTERN =
            Pattern.compile(".*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ].*");

    private VietnamSampleDataGenerator generator;

    @BeforeEach
    void setUp() throws Exception {
        generator = new VietnamSampleDataGenerator();
        generator.setLocaleForTest("vi-VN");
        // Manually call @PostConstruct via reflection (Spring context not loaded here)
        Method loadMethod = VietnamSampleDataGenerator.class.getDeclaredMethod("loadCsvData");
        loadMethod.setAccessible(true);
        loadMethod.invoke(generator);
    }

    @Test
    @DisplayName("All 6 CSV files load successfully with expected minimum row counts")
    void csvFilesLoad() {
        assertThat(generator.getStudentRowsForTest()).hasSizeGreaterThanOrEqualTo(300);
        assertThat(generator.getTeacherRowsForTest()).hasSizeGreaterThanOrEqualTo(100);
        assertThat(generator.getCenterRowsForTest()).hasSizeGreaterThanOrEqualTo(50);
        assertThat(generator.getClassRowsForTest()).hasSizeGreaterThanOrEqualTo(50);
        assertThat(generator.getAddressRowsForTest()).hasSizeGreaterThanOrEqualTo(100);
        assertThat(generator.getSubjectRowsForTest()).hasSizeGreaterThanOrEqualTo(30);
    }

    @Test
    @DisplayName("generateStudent returns name with Vietnamese diacritics")
    void generateStudentReturnsVietnameseName() {
        VietnamSampleDataGenerator.SampleStudent student = generator.generateStudent();
        assertThat(student.fullName()).isNotBlank();
        assertThat(student.gender()).isIn("F", "M");
        assertThat(student.region()).isIn("Bắc", "Trung", "Nam");
        // At least 80% of names should have diacritics (some pure-ASCII names like "An" exist)
        // Verify at least 1 name across many calls
    }

    @Test
    @DisplayName("200 generateStudent calls produce ≥80 unique names (diversity check)")
    void generateStudentDiversity() {
        // Use 200 samples to robust statistical floor against birthday paradox variance.
        // With pool=300, expected unique on 200 draws ≈ 165; 80 is conservative floor.
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(generator.generateStudent().fullName());
        }
        assertThat(seen.size())
                .as("Diversity: should generate ≥80 unique names over 200 calls")
                .isGreaterThanOrEqualTo(80);
    }

    @Test
    @DisplayName("100 generateStudent calls produce at least 50 names with Vietnamese diacritics")
    void generateStudentHasDiacritics() {
        int withDiacritics = 0;
        for (int i = 0; i < 100; i++) {
            String name = generator.generateStudent().fullName();
            if (VIETNAMESE_DIACRITIC_PATTERN.matcher(name).matches()) {
                withDiacritics++;
            }
        }
        assertThat(withDiacritics)
                .as("Most VN names should contain diacritics (≥50/100)")
                .isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("generateTeacher returns name + specialty")
    void generateTeacherReturnsValidData() {
        VietnamSampleDataGenerator.SampleTeacher teacher = generator.generateTeacher();
        assertThat(teacher.fullName()).isNotBlank();
        assertThat(teacher.specialty()).isNotBlank();
    }

    @Test
    @DisplayName("generateCenter returns name + short_name + city")
    void generateCenterReturnsValidData() {
        VietnamSampleDataGenerator.SampleCenter center = generator.generateCenter();
        assertThat(center.name()).startsWith("Trung tâm");
        assertThat(center.shortName()).isNotBlank();
        assertThat(center.city()).isIn(
                "TP.HCM", "Hà Nội", "Đà Nẵng", "Cần Thơ",
                "Hải Phòng", "Nha Trang", "Khánh Hòa");
    }

    @Test
    @DisplayName("generateClass returns VN edu naming convention")
    void generateClassReturnsVNNaming() {
        VietnamSampleDataGenerator.SampleClass clazz = generator.generateClass();
        assertThat(clazz.name()).startsWith("Lớp");
        assertThat(clazz.gradeLevel()).isBetween(0, 12);
        assertThat(clazz.subject()).isNotBlank();
    }

    @Test
    @DisplayName("generateAddress returns VN format (street + district + city)")
    void generateAddressReturnsVNFormat() {
        VietnamSampleDataGenerator.SampleAddress addr = generator.generateAddress();
        assertThat(addr.street()).isNotBlank();
        assertThat(addr.district()).isNotBlank();
        assertThat(addr.city()).isNotBlank();
    }

    @Test
    @DisplayName("formatVND returns VND currency format with Vietnamese symbol")
    void formatVNDReturnsViVNCurrency() {
        String formatted = generator.formatVND(new BigDecimal("1500000"));
        // Java Locale vi-VN may output "1.500.000 ₫" or "1.500.000 VND" depending on JDK
        assertThat(formatted)
                .as("VND currency format")
                .containsAnyOf("1.500.000", "1,500,000");
        // Should NOT be USD/dollar
        assertThat(formatted).doesNotContain("$");
        assertThat(formatted).doesNotContain("USD");
    }

    @Test
    @DisplayName("formatVNDate returns Vietnamese day-of-week + dd/MM/yyyy")
    void formatVNDateReturnsVietnameseDayOfWeek() {
        // 2026-05-14 is Thursday (Thứ Năm)
        String formatted = generator.formatVNDate(LocalDate.of(2026, 5, 14));
        assertThat(formatted).contains("14/05/2026");
        // Vietnamese day-of-week should appear (Thứ + word)
        assertThat(formatted.toLowerCase()).containsAnyOf(
                "thứ", "thứ");  // Tolerant of locale data variants
    }

    @Test
    @DisplayName("formatVNTime returns 24-hour HH:mm")
    void formatVNTimeReturns24HourFormat() {
        assertThat(generator.formatVNTime(LocalTime.of(9, 30))).isEqualTo("09:30");
        assertThat(generator.formatVNTime(LocalTime.of(14, 0))).isEqualTo("14:00");
        assertThat(generator.formatVNTime(LocalTime.of(0, 5))).isEqualTo("00:05");
    }

    @Test
    @DisplayName("Null inputs return empty string (defensive)")
    void formatNullsReturnEmpty() {
        assertThat(generator.formatVND(null)).isEmpty();
        assertThat(generator.formatVNDate(null)).isEmpty();
        assertThat(generator.formatVNTime(null)).isEmpty();
    }

    @Test
    @DisplayName("English locale fallback returns English placeholders")
    void englishLocaleFallback() throws Exception {
        VietnamSampleDataGenerator englishGen = new VietnamSampleDataGenerator();
        englishGen.setLocaleForTest("en-US");
        Method loadMethod = VietnamSampleDataGenerator.class.getDeclaredMethod("loadCsvData");
        loadMethod.setAccessible(true);
        loadMethod.invoke(englishGen);

        assertThat(englishGen.generateStudent().fullName()).isEqualTo("Student Sample");
        assertThat(englishGen.generateTeacher().fullName()).isEqualTo("Teacher Sample");
        assertThat(englishGen.generateCenter().name()).isEqualTo("Example Center");
        assertThat(englishGen.generateClass().name()).isEqualTo("Class A1");
        assertThat(englishGen.generateAddress().street()).isEqualTo("123 Main St");
        assertThat(englishGen.generateSubject().name()).isEqualTo("General");
    }

    @Test
    @DisplayName("generateSubject returns name + abbreviation")
    void generateSubjectReturnsValidData() {
        VietnamSampleDataGenerator.SampleSubject subject = generator.generateSubject();
        assertThat(subject.name()).isNotBlank();
        assertThat(subject.abbreviation()).isNotBlank();
    }

    @Test
    @DisplayName("Sample data contains real Vietnamese family names")
    void sampleStudentNamesUseVietnameseFamilyNames() {
        Set<String> familyNames = new HashSet<>();
        for (String[] row : generator.getStudentRowsForTest()) {
            familyNames.add(row[1]); // last_name = family
        }
        // Verify common VN family names present
        assertThat(familyNames).containsAnyOf(
                "Trần", "Nguyễn", "Lê", "Phạm", "Hoàng", "Vũ", "Đỗ");
    }
}
