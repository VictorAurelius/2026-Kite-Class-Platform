# API Contract Audit — Scoring Guide

## Grading Scale

| Score | Grade | Meaning |
|-------|-------|---------|
| 90-100 | A | API docs fully synchronized with code |
| 80-89 | B | Minor doc gaps, no breaking changes |
| 70-79 | C | Some undocumented endpoints |
| 60-69 | D | Significant drift — consumers at risk |
| <60 | F | Docs unreliable — cannot trust |

---

## Category 1: Endpoint Coverage (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | 100% endpoints documented, 100% docs have matching code |
| 16 | ≥90% coverage both directions |
| 12 | ≥75% code→doc, some orphan doc entries |
| 8 | ≥50% coverage, many undocumented endpoints |
| 4 | Docs exist but severely outdated |
| 0 | No api-contract.md or completely wrong |

**Bidirectional check:**
- Code → Doc: every `@XxxMapping` has doc entry
- Doc → Code: every documented endpoint has controller method

---

## Category 2: Request/Response Match (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | All DTO fields match docs, types correct, examples accurate |
| 16 | Fields match, 1-2 type mismatches (non-breaking) |
| 12 | Most fields match, missing optional fields in docs |
| 8 | Significant field drift |
| 4 | Response shapes completely different from docs |
| 0 | No schema documentation |

**Check method:**
- Read DTO class fields → compare with api-contract.md request/response tables
- Check `@JsonProperty` annotations for renamed fields
- Verify nullable/required matches doc

---

## Category 3: Error Code Consistency (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | All error codes documented with HTTP status + message format |
| 16 | Common errors documented, 1-2 missing edge cases |
| 12 | Happy path documented, error paths partial |
| 8 | Only generic 400/500 documented |
| 4 | Error handling exists but undocumented |
| 0 | No error documentation |

**Check:**
- `@ExceptionHandler` methods → documented error codes?
- Business exceptions → API error response format matches docs?
- HTTP status codes consistent (e.g., 404 for not found, 409 for conflict)

---

## Category 4: Versioning & Deprecation (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Version strategy documented, no breaking changes, deprecation notices |
| 16 | Version exists (`/api/v1/`), changes tracked in docs |
| 12 | Version prefix used but changes not tracked |
| 8 | No versioning strategy, breaking changes possible |
| 4 | Known breaking changes without notice |
| 0 | Constant breaking changes |

**Check:**
- `git log --diff-filter=M` on DTO files — any field removals/renames?
- Controller method signature changes since last audit
- New required fields added to request DTOs (= breaking for consumers)

---

## Category 5: Integration Test Coverage (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Every documented endpoint has IT with happy + error paths |
| 16 | ≥90% endpoints have IT, happy paths covered |
| 12 | ≥75% endpoints have IT |
| 8 | Key endpoints tested, many gaps |
| 4 | Minimal IT coverage |
| 0 | No integration tests for API endpoints |

**Check:**
- Count `*IT.java` or `*IntegrationTest.java` files
- Match test methods to documented endpoints
- Verify both 200 (success) and 4xx (error) scenarios tested
