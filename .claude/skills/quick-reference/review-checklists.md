# Code Review Checklists — Stage 1 + Stage 2

## Stage 1: Specification Compliance (BLOCKING)

### 1. Requirements Match
```markdown
- [ ] Matches PR description exactly
- [ ] All acceptance criteria from implementation plan implemented
- [ ] No missing features
- [ ] No scope creep (extra features not requested)
```

### 2. Edge Cases Coverage
```markdown
- [ ] Handles null/empty inputs (validation)
- [ ] Handles invalid data
- [ ] Handles errors gracefully (EntityNotFoundException, etc.)
- [ ] Multi-tenant isolation (if applicable) — query filters by instance_id
```

### 3. File Paths Match Plan
```markdown
- [ ] Code files in correct service/package
- [ ] Test files in corresponding test directory
- [ ] No unexpected file changes (other services/packages)
```

### 4. API Contracts Match Design
```markdown
- [ ] Request/Response DTOs match api-design.md
- [ ] HTTP status codes correct (201 CREATED, 200 OK, 404 NOT_FOUND, 400 BAD_REQUEST)
- [ ] Endpoint paths follow conventions (/api/v1/students)
```

### 5. Tests Prove Requirements
```markdown
- [ ] Every acceptance criterion has corresponding test
- [ ] Tests actually verify requirement (not just compile)
- [ ] Tests pass (green CI)
```

**Outcome:** ✅ PASS → Stage 2 | ❌ FAIL → BLOCK, list issues, return to developer

---

## Stage 2: Code Quality (GRADED)

### 🔴 CRITICAL Issues (BLOCKING)

| Issue | Example |
|-------|---------|
| SQL Injection | `"SELECT ... WHERE name = '" + name + "'"` |
| Missing transaction | Two operations without `@Transactional` |
| Breaking API change | Removing field from Response DTO |
| Auth bypass | `@GetMapping` without `@PreAuthorize` |
| Multi-tenant data leak | Query without `instance_id` filter |

**Any CRITICAL → BLOCK PR.**

### 🟠 MAJOR Issues (Strong Recommendation)

| Issue | Example |
|-------|---------|
| N+1 query | `students.forEach(s -> s.getCourses().size())` in loop |
| Test coverage <80% | New service method with 0 tests |
| Missing error handling | `.get()` without `orElseThrow()` |
| Class too large | Service class >300 lines (extract subservice) |
| Missing `@Transactional` on multi-step operations | |

**MAJOR issues → APPROVE with strong recommendation.**

### 🟡 MINOR Issues (Optional)

| Issue | Example |
|-------|---------|
| Vague naming | `List<Student> list`, `Student s` |
| Code duplication | Same mapping logic in 3 methods |
| Missing JavaDoc | Public method no `/** */` |
| Style inconsistency | `method1(){...}` vs `method2() { ... }` |

**MINOR only → APPROVE (optional follow-up issue).**

---

## KiteClass Example — Stage 1 Pass

```markdown
## Stage 1: PR 2.15 Student CRUD

Requirements (from plan):
1. POST /api/students → ✅ StudentController.createStudent() exists
2. GET /api/students/{id} → ✅ getStudent() exists
3. PUT /api/students/{id} → ✅ updateStudent() exists
4. DELETE /api/students/{id} → ✅ deleteStudent() (soft delete) exists

Edge cases:
- Null inputs: ✅ @NotBlank, @Email validation
- Duplicate email: ✅ DuplicateResourceException test exists
- Multi-tenant: ✅ findByIdAndDeletedFalse() (Hibernate-filter safe)

API contracts:
- DTOs: ✅ CreateStudentRequest, StudentResponse match design
- Status: ✅ 201 CREATED, 404 EntityNotFoundException

Tests: ✅ 18 tests, all green

Stage 1 Outcome: ✅ PASS → Proceed to Stage 2
```
