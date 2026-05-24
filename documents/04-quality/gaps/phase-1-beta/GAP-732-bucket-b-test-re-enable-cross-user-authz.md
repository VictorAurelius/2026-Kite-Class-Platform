# GAP-732: Wave beta-readiness-2 Bucket B — re-enable 2 @Disabled tests `CrossUserAuthzTest` A01-U01 + A01-U03

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (test coverage)
**Detected:** 2026-05-24 (Wave beta-readiness-2 Bucket B inline implementation — production defect FIXED but test bodies deferred)
**Affects:** OWASP A01 IDOR test coverage cho teacher-class authz guard `AuthorizationBean.hasAccessToClass()`

## Problem

Wave beta-readiness-2 Bucket B (GAP-727) fix production defect `hasAccessToClass` guard broken:
- Class entity now maps `teacher_id` ✅
- `ClassServiceImpl.createClass()` set `teacherId` từ `UserContext.getCurrentUser()` ✅
- Schema column ddl-auto generates trong test profile ✅ (entity field present)

2 @Disabled tests trong `CrossUserAuthzTest.java` được update messages reflect production fix DONE, NHƯNG test bodies still empty — re-enable cần dedicated fixture work:

- **A01-U01 (negative):** Teacher-2 GET `/api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance` cho class owned by Teacher-1 → 403
- **A01-U03 (positive):** Class owner Teacher-1 GET cùng endpoint → service-reached (NOT Spring Security 403)

## Root Cause

Bucket B inline implementation context-pressured (3 background agents fail autocompact thrash — Sonnet 200k context overflow); coordinator (Opus 1M) ship core production fix + defer test bodies cho follow-up.

Test fixture cần:
1. Create teacher1 + teacher2 via `testDataBuilder.createTestTeacher(...)`
2. Create course owned by teacher1 via `testDataBuilder.createTestCourse(...)`
3. POST `/api/v1/courses/{courseId}/classes` với `X-User-Id=teacher1Id` header → class với `teacher_id=teacher1Id`
4. Create at least 1 session để GET endpoint không hit 404 trên session lookup
5. A01-U01: mockMvc.perform GET với `X-User-Id=teacher2Id` → assertStatus 403
6. A01-U03: mockMvc.perform GET với `X-User-Id=teacher1Id` → assert NOT Spring Security 403 (service-reached pattern per A01-U04 precedent line 365-395)

## Proposed Fix (Wave beta-readiness-3+ candidate)

```java
@Test
@DisplayName("A01-U01: Teacher-2 cannot GET attendance roster for class owned by Teacher-1")
void teacher2_cannotGetAttendance_forClassOwnedByTeacher1() throws Exception {
    UUID tenantId = UUID.randomUUID();
    Long teacher1Id = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
    Long teacher2Id = testDataBuilder.createTestTeacher(mockMvc, objectMapper, tenantId);
    Long courseId = testDataBuilder.createTestCourse(mockMvc, objectMapper, tenantId, teacher1Id);

    // POST class với teacher1 X-User-Id → teacher_id=teacher1Id (per GAP-727 fix)
    String classJson = "{\"name\":\"Test Class 5A1\",\"maxStudents\":30}";
    MvcResult result = mockMvc.perform(post("/api/v1/courses/" + courseId + "/classes")
        .header("X-Tenant-Id", tenantId.toString())
        .header("X-User-Id", teacher1Id.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(classJson))
        .andExpect(status().isCreated())
        .andReturn();
    Long classId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    Long sessionId = /* create session via existing fixture */;

    // NEGATIVE: teacher2 attempts access → 403
    mockMvc.perform(get("/api/v1/attendance/classes/" + classId + "/sessions/" + sessionId + "/attendance")
        .header("X-Tenant-Id", tenantId.toString())
        .header("X-User-Id", teacher2Id.toString()))
        .andExpect(status().isForbidden());
}

@Test
@DisplayName("A01-U03: Teacher-1 CAN GET attendance roster cho own class")
void teacher1_canGetAttendance_forOwnClass() throws Exception {
    // Setup same as A01-U01 ...
    // POSITIVE: teacher1 (owner) → service reached pattern per A01-U04
    mockMvc.perform(get("/api/v1/attendance/classes/" + classId + "/sessions/" + sessionId + "/attendance")
        .header("X-Tenant-Id", tenantId.toString())
        .header("X-User-Id", teacher1Id.toString()))
        .andExpect(result -> {
            int status = result.getResponse().getStatus();
            String body = result.getResponse().getContentAsString();
            boolean isSpringSecurityDeny = (status == 403) && !body.contains("ATTENDANCE_");
            if (isSpringSecurityDeny) {
                throw new AssertionError("Expected @PreAuthorize PASS but got Spring Security 403");
            }
        });
}
```

## Acceptance Criteria

- [ ] A01-U01 test body shipped + PASS
- [ ] A01-U03 test body shipped + PASS
- [ ] Remove @Disabled annotation from both
- [ ] `./mvnw verify -P strict-warnings` PASS
- [ ] Optional: extract `createTestClass()` helper to `TestDataBuilder` nếu nhiều IT tests cần class fixture

## Out-of-scope

- Session fixture creation refactor — separate gap nếu pattern need broader use
- Move `CrossUserAuthzTest` to per-domain IT split — separate refactor

## Priority Rationale (P1)

Production defect GAP-727 FIXED in Wave beta-readiness-2 Bucket B (entity + service); test coverage extends defense-in-depth but không block Phase 1 BETA gate. P1 = ship trong Wave beta-readiness-3 hoặc Wave 109+.

## Related

- GAP-727 — parent gap (production defect, FIXED Wave beta-readiness-2 Bucket B)
- Wave beta-readiness-2 plan §3 Bucket B
- A01-U02 + A01-U04 existing tests — reference patterns
- `pre-handoff-self-test-completeness.md` §2.1 auth-gated flow

## Log

- **2026-05-24 (filed):** Filed during Wave beta-readiness-2 Bucket B inline implementation. Production defect FIXED (entity + service); test bodies deferred cho follow-up wave per `gap-done-discipline.md` §3 PARTIAL exit ramp. Context: 3 background Sonnet agents (B/C/D) failed autocompact thrash; coordinator (Opus 1M) ship core fix only.
