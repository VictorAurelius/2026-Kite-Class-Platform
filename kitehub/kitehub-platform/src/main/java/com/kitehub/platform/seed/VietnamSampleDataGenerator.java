package com.kitehub.platform.seed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Vietnam Sample Data Generator — provides VN-friendly sample data for seed worker.
 *
 * <p>Replaces English Lorem-Ipsum placeholders ({@code John Doe}, {@code Class A1},
 * {@code Example Center}, {@code $60.00}) với Vietnamese-friendly sample data sourced
 * từ CSV files trong {@code seed-data/vn-friendly/} classpath resource folder.
 *
 * <p>Per GAP-658 (Wave 98 Bucket B2) — closes GAP-538 AC7 (VN sample seed worker).
 * Persona walkthrough P2 Center Owner (chị Hằng) first-touch trust signal: sample
 * data must match VN edu convention (`Lớp Anh ngữ 5A1`, `Trung tâm Sky Education`,
 * VND currency `1.500.000đ`, date `Thứ Hai, 14/05/2026`).
 *
 * <p>Locale-aware: {@code seed.locale=vi-VN} (default) returns Vietnamese data.
 * {@code seed.locale=en-US} falls back to English placeholders for test fixture
 * compatibility (documented in {@code documents/01-business/kitehub/seed/rules.md}).
 *
 * <p>Thread-safe: CSV data loaded once at startup into immutable lists; selection
 * uses {@code ThreadLocalRandom} to avoid contention.
 *
 * @see <a href="file:../../../../../../../../../documents/01-business/kitehub/seed/rules.md">seed/rules.md</a>
 */
@Component
public class VietnamSampleDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(VietnamSampleDataGenerator.class);

    private static final Locale VI_VN = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_FORMATTER_VN =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", VI_VN);
    private static final DateTimeFormatter TIME_FORMATTER_24H =
            DateTimeFormatter.ofPattern("HH:mm");

    @Value("${seed.locale:vi-VN}")
    private String locale;

    private List<String[]> studentRows;
    private List<String[]> teacherRows;
    private List<String[]> centerRows;
    private List<String[]> classRows;
    private List<String[]> addressRows;
    private List<String[]> subjectRows;

    @PostConstruct
    void loadCsvData() {
        studentRows = loadCsv("seed-data/vn-friendly/student-names.csv");
        teacherRows = loadCsv("seed-data/vn-friendly/teacher-names.csv");
        centerRows = loadCsv("seed-data/vn-friendly/center-names.csv");
        classRows = loadCsv("seed-data/vn-friendly/class-names.csv");
        addressRows = loadCsv("seed-data/vn-friendly/addresses.csv");
        subjectRows = loadCsv("seed-data/vn-friendly/subject-names.csv");
        log.info(
                "VietnamSampleDataGenerator loaded: {} students, {} teachers, {} centers, "
                        + "{} classes, {} addresses, {} subjects (locale={})",
                studentRows.size(), teacherRows.size(), centerRows.size(),
                classRows.size(), addressRows.size(), subjectRows.size(), locale);
    }

    private List<String[]> loadCsv(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = resource.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    // Strip UTF-8 BOM if present
                    if (!line.isEmpty() && line.charAt(0) == '﻿') {
                        line = line.substring(1);
                    }
                    firstLine = false;
                    continue; // Skip header
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                rows.add(line.split(","));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load CSV resource: " + resourcePath, e);
        }
        return Collections.unmodifiableList(rows);
    }

    /**
     * Generates a sample student with Vietnamese name + random gender + region.
     *
     * @return SampleStudent (full_name, gender, region)
     */
    public SampleStudent generateStudent() {
        if (isEnglishLocale()) {
            return new SampleStudent("Student Sample", "U", "N/A");
        }
        String[] row = pickRandom(studentRows);
        return new SampleStudent(row[2], row[3], row[4]);
    }

    /**
     * Generates a sample teacher with Vietnamese name + specialty.
     *
     * @return SampleTeacher (full_name, specialty)
     */
    public SampleTeacher generateTeacher() {
        if (isEnglishLocale()) {
            return new SampleTeacher("Teacher Sample", "General");
        }
        String[] row = pickRandom(teacherRows);
        return new SampleTeacher(row[2], row[5]);
    }

    /**
     * Generates a sample education center.
     *
     * @return SampleCenter (name, short_name, city)
     */
    public SampleCenter generateCenter() {
        if (isEnglishLocale()) {
            return new SampleCenter("Example Center", "Demo", "Example City");
        }
        String[] row = pickRandom(centerRows);
        return new SampleCenter(row[0], row[1], row[2]);
    }

    /**
     * Generates a sample class with VN edu naming convention (Lớp + subject + grade-section).
     *
     * @return SampleClass (name, grade_level, subject)
     */
    public SampleClass generateClass() {
        if (isEnglishLocale()) {
            return new SampleClass("Class A1", 5, "General");
        }
        String[] row = pickRandom(classRows);
        int grade = Integer.parseInt(row[1]);
        return new SampleClass(row[0], grade, row[2]);
    }

    /**
     * Generates a sample VN-format address.
     *
     * @return SampleAddress (street, district, city)
     */
    public SampleAddress generateAddress() {
        if (isEnglishLocale()) {
            return new SampleAddress("123 Main St", "Anywhere", "Example City");
        }
        String[] row = pickRandom(addressRows);
        return new SampleAddress(row[0], row[1], row[2]);
    }

    /**
     * Generates a sample subject (name + abbreviation).
     *
     * @return SampleSubject (name, abbreviation)
     */
    public SampleSubject generateSubject() {
        if (isEnglishLocale()) {
            return new SampleSubject("General", "GEN");
        }
        String[] row = pickRandom(subjectRows);
        return new SampleSubject(row[0], row[1]);
    }

    /**
     * Formats currency in VND convention (e.g., 1500000 → "1.500.000 ₫").
     *
     * @param amount BigDecimal currency value
     * @return VND-formatted string
     */
    public String formatVND(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        NumberFormat fmt = NumberFormat.getCurrencyInstance(VI_VN);
        return fmt.format(amount);
    }

    /**
     * Formats date in VN convention (e.g., 2026-05-14 → "Thứ Năm, 14/05/2026").
     *
     * @param date LocalDate
     * @return VN-formatted date string
     */
    public String formatVNDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER_VN);
    }

    /**
     * Formats time in 24-hour convention (e.g., 09:30, 14:00).
     *
     * @param time LocalTime
     * @return HH:mm string
     */
    public String formatVNTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.format(TIME_FORMATTER_24H);
    }

    private boolean isEnglishLocale() {
        return locale != null && locale.toLowerCase(Locale.ROOT).startsWith("en");
    }

    private String[] pickRandom(List<String[]> rows) {
        if (rows.isEmpty()) {
            throw new IllegalStateException("Sample data not loaded — CSV resource missing");
        }
        int index = ThreadLocalRandom.current().nextInt(rows.size());
        return rows.get(index);
    }

    // Test-only setter for locale (package-private for unit tests)
    void setLocaleForTest(String locale) {
        this.locale = locale;
    }

    // Test-only deterministic random injection
    String[] pickFirstForTest(List<String[]> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<String[]> getStudentRowsForTest() {
        return studentRows;
    }

    List<String[]> getTeacherRowsForTest() {
        return teacherRows;
    }

    List<String[]> getCenterRowsForTest() {
        return centerRows;
    }

    List<String[]> getClassRowsForTest() {
        return classRows;
    }

    List<String[]> getAddressRowsForTest() {
        return addressRows;
    }

    List<String[]> getSubjectRowsForTest() {
        return subjectRows;
    }

    // ─── DTOs ──────────────────────────────────────────────────────────────

    public record SampleStudent(String fullName, String gender, String region) {}

    public record SampleTeacher(String fullName, String specialty) {}

    public record SampleCenter(String name, String shortName, String city) {}

    public record SampleClass(String name, int gradeLevel, String subject) {}

    public record SampleAddress(String street, String district, String city) {}

    public record SampleSubject(String name, String abbreviation) {}
}
