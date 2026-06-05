---
title: Wave flow-kc4 — Student enrollment + bulk import
status: active
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc4]
tag_primary: flow
tags_secondary: [kc4, enrollment, bulk-import, student, file-upload, kiteclass, campaign]
counter: 4
campaign: flow-verification-campaign
gaps: []
---

# Wave flow-kc4 — Student enrollment + bulk import

**Goal:** Walk end-to-end flow KC-4 (Owner/STAFF ghi danh học sinh vào lớp + import hàng loạt học sinh từ CSV) trên stack production-equivalent, đạt **G1 PASS**. Nền tảng cho attendance (KC-5) + grade (KC-6) + invoice (KC-7) — mọi flow sau giả định học sinh đã enroll vào lớp.

**Trigger:** KC-4 đứng sau KC-3 (course+class+schedule đã tạo — G1 PASS 2026-06-05) + KC-2 (student tồn tại trong tenant). Enrollment gắn student ↔ class; bulk-import tạo nhiều student cùng lúc.

## 1. Brainstorm

**State-check (2026-06-05):** KC-4 = kiteclass-core flow. Endpoints CONFIRMED tồn tại (không partial-impl risk):
- ✅ `EnrollmentController` @ `/api/v1/enrollments` — POST enroll, GET `/{id}` + `/student/{studentId}` + `/class/{classId}`.
- ✅ `BulkImportController` @ `/api/v1/students/bulk-import` — `POST /preview` + `POST /commit` (multipart CSV), likely `/template`.
- ✅ `Enrollment` + `BulkImportJob` entity extends `BaseEntity` (tenant-scoped via `@Filter`).

**Pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` (BẮT BUỘC — enrollment persona + bulk-import file-upload flow):** Opus agent spawned 2026-06-05 → artifact `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc4-enrollment-bulk-import.md`. Likely failure modes: lớp đầy (maxStudents) / duplicate enrollment / cross-tenant student↔class (GAP-983 just fixed — verify enrollment lookup respects tenant) / CSV MIME spoof / VN-name UTF-8 BOM / partial-commit rollback / preview-vs-commit drift / currentEnrolled count side-effect.

**Isolation context:** GAP-983 (cross-tenant by-id leak) vừa fix Wave security-1. KC-4 lookup student + class by id → PHẢI verify enrollment KHÔNG cho enroll student tenant A vào class tenant B (cross-tenant enroll = breach). This is a NEW isolation surface (enroll = 2 by-id lookups).

**Blocker:** none known. File-upload (bulk-import) cần verify MinIO storage + MIME validation per `pre-handoff-self-test-completeness.md` §2.5.

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class |
|---|---|---|---|
| 0 (Pre-walk) | Opus persona sim agent → ≥5 failure modes per `pre-walk-persona-simulation-mandate.md` §3 | Coordinator | n/a (DONE — agent spawned) |
| A (Walk) | Coordinator G1 walk: enroll student→class (happy + capacity + duplicate + cross-tenant) | Coordinator | user-facing ✅ pre-walk required |
| B (Walk) | Bulk-import walk: CSV preview → commit (happy + bad-format + partial-fail + VN-name) per `pre-handoff-self-test-completeness.md` §2.5 file-upload checklist | Coordinator | user-facing ✅ |
| C (Fix) | Batch-fix high-confidence pre-walk findings + walk-surfaced bugs (catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4) | agent/coordinator | — |
| D (G2 handoff) | G2 recipe MD per `g2-handoff-md-mandate.md` khi G1 PASS | Coordinator | — |

## 3. Scope

Full §3 expansion happens at walk-time (after pre-walk agent returns). Skeleton:
- **BE (kiteclass-core):** `module/enrollment/**` (controller + service + Enrollment entity + EnrollmentRepository — note GAP-746 residual: verify `findByIdAndDeletedFalse` tenant-scoped, now covered by Wave security-1 centralized filter) + `module/student/bulkimport/**` (controller + service + BulkImportJob + CSV parser).
- **Verify target:** enroll 1 student vào class 14 (sky) → currentEnrolled tăng; duplicate enroll → 409; lớp đầy → capacity error; cross-tenant enroll → 404/403; bulk CSV preview → commit → N students created tenant-scoped.
- **Isolation (post GAP-983):** cross-tenant student↔class enroll attempt → reject.
- **Dependency:** KC-3 data (class 14, course 10 sky tenant). Student fixtures (KC-2).

## 4. State-Check Evidence

Verified 2026-06-05 (grep, no `| head` per `audit-to-gap-pipeline.md` §2.5):

| Symbol | Verify command | Verdict |
|---|---|---|
| `EnrollmentController` @ /api/v1/enrollments | `grep -rn "RequestMapping" enrollment/controller/` | ✅ `/api/v1/enrollments` + GET /{id} + /student/{id} + /class/{id} |
| `BulkImportController` @ /students/bulk-import | `grep -rn "RequestMapping\|PostMapping" student/bulkimport/controller/` | ✅ `/api/v1/students/bulk-import` + POST /preview + /commit (multipart) |
| `Enrollment` + `BulkImportJob` extends BaseEntity | `grep -rln "extends BaseEntity" enrollment/ student/bulkimport/` | ✅ both tenant-scoped (@Filter inherited per GAP-983 fix) |

Detailed request DTO + service logic = read at walk-time (Bucket A/B) to avoid duplicating pre-walk agent's investigation.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — coordinator walk | Claude | Enroll happy + capacity + duplicate + cross-tenant reject; bulk CSV preview→commit + bad-format + partial-fail; currentEnrolled side-effect correct; production-equivalent stack | ⬜ |
| G2 — human walk | User | Per G2 recipe MD (Bucket D) — enroll via UI + bulk-import CSV via UI | ⬜ |
| G3 — production parity | User | Post AWS restore — multi-tenant enroll isolation + file storage (MinIO/S3) | ⬜ |

## 6. Closure Protocol

1. Catalog walk findings → file gaps inline per `discovery-to-gap-inline-filing.md` §3.
2. Batch-fix high-confidence (pre-walk + walk) per `feature-ship-runtime-walk-mandate.md` §3.4.
3. Re-walk affected scope per `pre-handoff-self-test-completeness.md` §3 (if fixes shipped).
4. G2 recipe MD per `g2-handoff-md-mandate.md` (Bucket D).
5. Flip campaign §4 KC-4 row → 🔄 walk-pass-pending-human.
6. wave-history.jsonl append; frontmatter draft → active.
7. CSV + ROADMAP sync per `post-merge-sync-completeness.md`.

## 8. Pre-walk findings (2026-06-05) — `audits/persona-review/2026-06-05-pre-walk-kc4-enrollment-bulk-import.md`

12 failure modes (3 HIGH / 5 MEDIUM / 4 LOW). **Contract surprises (quan trọng cho walk):**
- ⚠️ **Bulk-import là XLSX-only, KHÔNG phải CSV** (Apache POI `XSSFWorkbook`). Walk PHẢI upload `.xlsx` thật. Schema: required `name`,`email`; optional `phone`,`date_of_birth` (dd/MM/yyyy),`gender` (MALE/FEMALE only),`address`,`note`. Multipart field `file` + header `X-Tenant-Id`.
- ⚠️ **Enroll body cần `tuitionAmount`** (`@NotNull`) + `studentId` + `classId` (Positive). Optional `Idempotency-Key` header.
- **Tenant-safety asymmetry:** bulk-import truyền `tenantId` explicit (`existsBy...AndInstanceId`, safe); enroll path dựa HOÀN TOÀN vào Hibernate `@Filter`+RLS (GAP-983 surface) → cross-tenant enroll = trust boundary cần re-walk.

**HIGH bugs batch-fix TRƯỚC walk (fix agent spawned):**
- **GAP-988** P1 — non-XLSX/malformed upload → 500 thay vì 400/413/415 (`parseSafely` chỉ catch IOException; no MIME pre-check).
- **GAP-989** P1 — `enrollStudent` thiếu class-status guard → enroll vào COMPLETED/CANCELLED → 201.

MEDIUM (spot-check at walk): VN-name UTF-8 round-trip / oversized multipart 500 / soft-deleted enroll 404 / cross-tenant @Filter dependency. LOW (defer): duplicate guards OK / re-enroll-after-withdraw / preview-vs-commit drift by-design / partial-fail skip-and-report by-design.

## 7. Log

- **2026-06-05 (plan ship):** Filed sau KC-3 G1 PASS (GAP-983 unblock). State-check confirmed enrollment + bulk-import endpoints exist (no partial-impl risk). Pre-walk persona sim agent (Opus) returned 12 failure modes — **2 surprises: bulk-import XLSX-only (not CSV) + enroll requires tuitionAmount.** 3 HIGH bugs → GAP-988 (bulk 500) + GAP-989 (class-status guard) filed + fix agent spawned (batch-fix before walk per `pre-walk-persona-simulation-mandate.md`). NEW isolation surface post GAP-983: enroll = 2 by-id lookups → cross-tenant enroll re-walk. Walk (Bucket A/B) after fixes land + rebuild.

## 9. G1 Outcome (2026-06-05)

**G1 ✅ PASS** (production-equivalent walk):
- Enroll happy (student 4 → class 14 + tuitionAmount) → 201, `currentEnrolled` 0→1.
- Duplicate enroll → 409. Cross-tenant enroll (khanh → sky) → 404 (isolation post-GAP-983).
- **GAP-989** enroll vào lớp COMPLETED → 400 `CLASS_NOT_ENROLLABLE` (was 201). **GAP-988** csv/fake.xlsx → 415/400 (was 500). IT 21/21.

GAP-988 + GAP-989 → DONE. GAP-990 (K12 homeroom guard) defer Phase 3. G2 handoff: [`2026-06-05-g2-recipe-kc4-enrollment-bulk-import.md`](../../05-guides/operations/2026-06-05-g2-recipe-kc4-enrollment-bulk-import.md). Campaign §4: KC-4 → 🔄 walk-pass-pending-human.

**Note:** bulk-import XLSX happy-path (preview→commit real .xlsx) deferred to G2 human UI test; G1 verified error-handling (4xx) + IT covers XLSX parse.
