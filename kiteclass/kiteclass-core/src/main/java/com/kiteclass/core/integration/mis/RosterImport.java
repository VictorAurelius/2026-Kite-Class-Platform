package com.kiteclass.core.integration.mis;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Neutral roster payload returned by every {@link MisRosterSource} adapter.
 *
 * <p>Vendor types never leak past this boundary — each adapter translates
 * provider-specific JSON / XML / CSV into these records before returning.
 * Enforces {@code .claude/rules/design-patterns.md} §3.10 (no leaky abstraction).
 *
 * <p>Records are nested to keep the public import surface cohesive without
 * spawning 5 top-level files each for StudentRecord / TeacherRecord / etc.
 *
 * @param source       which MIS produced this payload
 * @param fetchedAt    timestamp when the adapter finished building the DTO
 * @param academicYear e.g. {@code "2025-2026"}
 * @param students     student roster (may be empty — never null)
 * @param parents      parent roster (may be empty — never null)
 * @param teachers     teacher roster (may be empty — never null)
 * @param classes      class / homeroom roster
 * @param enrollments  student ↔ class associations
 * @since 2.20.0
 */
public record RosterImport(
        MisProvider source,
        Instant fetchedAt,
        String academicYear,
        List<StudentRecord> students,
        List<ParentRecord> parents,
        List<TeacherRecord> teachers,
        List<ClassRecord> classes,
        List<EnrollmentRecord> enrollments
) {

    /**
     * Compact constructor — replaces null collections with empty lists so
     * adapter call sites can stream/forEach safely without null guards.
     */
    public RosterImport {
        students = students == null ? List.of() : List.copyOf(students);
        parents = parents == null ? List.of() : List.copyOf(parents);
        teachers = teachers == null ? List.of() : List.copyOf(teachers);
        classes = classes == null ? List.of() : List.copyOf(classes);
        enrollments = enrollments == null ? List.of() : List.copyOf(enrollments);
    }

    /** Student record — neutral to MIS provider. */
    public record StudentRecord(
            String providerStudentId,
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String email,
            String phone,
            String gradeLevel,
            String homeroomClassId
    ) {}

    /** Parent record — many-to-many with students. */
    public record ParentRecord(
            String providerParentId,
            String fullName,
            String email,
            String phone,
            String relationship,
            List<String> linkedProviderStudentIds
    ) {
        public ParentRecord {
            linkedProviderStudentIds = linkedProviderStudentIds == null
                    ? List.of()
                    : List.copyOf(linkedProviderStudentIds);
        }
    }

    /** Teacher record — subject assignment handled Phase 2. */
    public record TeacherRecord(
            String providerTeacherId,
            String fullName,
            String email,
            String phone,
            String primarySubject
    ) {}

    /** Class (homeroom) record. */
    public record ClassRecord(
            String providerClassId,
            String name,
            String gradeLevel,
            String homeroomTeacherProviderId
    ) {}

    /** Enrollment record — student attached to class for an academic year. */
    public record EnrollmentRecord(
            String providerStudentId,
            String providerClassId,
            String academicYear,
            String status
    ) {}
}
