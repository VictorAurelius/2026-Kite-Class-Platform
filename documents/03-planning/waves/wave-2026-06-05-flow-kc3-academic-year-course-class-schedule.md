---
title: Wave flow-kc3 — Academic: year → course → class → schedule
status: draft
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc3]
tag_primary: flow
tags_secondary: [kc3, academic, course, class, schedule, kiteclass, campaign]
counter: 3
gaps: [GAP-909]
campaign: flow-verification-campaign
---

# Wave flow-kc3 — Academic: year → course → class → schedule

**Goal:** Walk end-to-end flow KC-3 (Owner/STAFF setup cấu trúc học thuật: tạo niên khóa → tạo khóa học → tạo lớp → xếp lịch tuần) trên stack production-equivalent, đạt **G1 PASS**. Xác minh chuỗi tạo cấu trúc nghiệp vụ trường hoạt động — nền tảng cho enrollment (KC-4) + attendance (KC-5) + grade (KC-6).

**Trigger:** KC-3 đứng sau KC-1 (tenant configured) + KC-2 (staff/teacher tồn tại để gán lớp). Mọi flow nghiệp vụ sau (enrollment/attendance/grade) giả định course+class+schedule đã tạo. Nếu KC-3 chưa thông → KC-4..9 thiếu context.

## 1. Brainstorm

**Bối cảnh state-check (2026-06-05):** KC-3 là kiteclass-core flow (không platform-side). Phát hiện quan trọng:
- ✅ **academicyear** module tồn tại (`module/academicyear`)
- ✅ **course** module + `CourseController` @ `/api/v1/courses`
- ⚠️ **class + schedule controller KHÔNG tìm thấy** qua quick grep (chỉ `/api/v1/courses`) — phần class→schedule có thể **chưa implement** hoặc tên module khác. **Cần verify-at-session-start** (state-check hardened per `audit-to-gap-pipeline.md` §2.5, no `| head`).
- Docs: `course-class` + `academic-year` + `reschedule` + `student-enrollment` 3-layer tồn tại.

**Pre-walk persona sim per `pre-walk-persona-simulation-mandate.md` BẮT BUỘC** (multi-step setup flow): persona "Owner/STAFF setup học thuật lần đầu" — tạo year → course → class → assign teacher → schedule. Likely failure modes: class/schedule endpoint thiếu (partial impl) / academic-year required cho course / teacher assign cross-tenant / schedule slot conflict.

**Blocker:** GAP-909 🟡 PARTIAL P2 — KC courses entity vs migration drift (`cover_image_url` + `suggested_tuition`). Low priority, không block walk.

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class | Effort |
|---|---|---|---|---|
| 0 (Pre-walk) | State-check class/schedule impl (verify controller tồn tại không) + spawn Opus pre-walk persona sim per `pre-walk-persona-simulation-mandate.md` §1 | Coordinator | n/a | ~10 phút |
| A | Loop walk: tạo academic-year → course → class → schedule slot (curl + DB verify mỗi bước) | claude (session-pick) | user-facing ✅ pre-walk required | 30-60 phút |
| B | Batch-fix blocker (class/schedule endpoint thiếu → có thể là feature-build, không chỉ walk; academic-year linkage; teacher assign) | claude | n/a | varies (có thể lớn nếu class/schedule chưa impl) |
| C | Re-walk + G1 verdict + G2 handoff MD per `g2-handoff-md-mandate.md` | claude | n/a | 15-30 phút |

## 3. Scope

Full §3 scope + bucket expansion happens tại session start khi pick wave này (per stub convention). Skeleton:
- **BE (kiteclass-core):** `academicyear` module + `course` module (`CourseController` @ `/api/v1/courses`). Class + schedule: **verify impl at session start** (controller/endpoint có tồn tại không).
- **FE (kiteclass-frontend):** academic/course/class pages — verify at session start ((teacher) area?).
- **Walk target:** tenant `sky-education` (reuse KC-1/KC-2), Owner `owner@skyedu.vn`.
- **Dependency:** KC-1 (tenant) + KC-2 (staff/teacher) — STAFF tenant resolution đã fix (GAP-981).

## 4. State-Check Evidence

Verified 2026-06-05 (coordinator quick state-check — full verify at session start):

| Symbol | Type | Verify command | Verdict |
|---|---|---|---|
| `module/academicyear` | BE module | `find kiteclass-core/.../module/academicyear` | ✅ exists |
| `CourseController` @ `/api/v1/courses` | BE controller | `grep -rn "CourseController" kiteclass-core/.../module/course` | ✅ exists |
| class controller @ `/api/v1/classes` | BE controller | `grep -rnE "RequestMapping.*classes" kiteclass-core` | ⚠️ **NOT FOUND quick grep — verify at session start (may be partial-impl OR different name)** |
| schedule controller @ `/api/v1/schedul*` | BE controller | `grep -rnE "RequestMapping.*schedul" kiteclass-core` | ⚠️ **NOT FOUND quick grep — verify at session start** |
| `documents/01-business/kiteclass/course-class/` | 3-layer docs | `ls documents/01-business/kiteclass/course-class/` | ✅ exists |
| `documents/01-business/kiteclass/academic-year/` | 3-layer docs | `ls` | ✅ exists |

**⚠️ Pre-walk risk:** class/schedule có thể chưa có controller → KC-3 walk có thể là **partial feature-build**, không chỉ walk. Session-start state-check (hardened, no `| head`) sẽ xác định scope thật.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — agent runtime walk | Claude | (a) tạo academic-year `POST` trả 201 + DB row; (b) tạo course `POST /api/v1/courses` trả 201; (c) tạo class (gán course + teacher) trả 201; (d) tạo schedule slot (class + day_of_week + time) trả 201 + không conflict; (e) GET chain verify cấu trúc đầy đủ | ⬜ |
| G2 — human local test | User | Login Owner → UI tạo year/course/class/schedule → verify hiển thị | ⬜ |
| G3 — production parity | Claude + User | Production: academic schema migrate sạch RDS + multi-tenant isolation + schedule timezone đúng | ⬜ |

G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 — ship same PR as G1 PASS flip.

## 6. Agent Spawn Pattern

- KHÔNG parallel-spawn cho walk (state-continuous).
- Pre-walk Opus agent BACKGROUND per `agent-background-spawn-default.md` + `agent-model-opus-default.md`.
- Nếu class/schedule chưa impl → Bucket B = feature-build (spawn agent ship controller+entity+migration TRƯỚC re-walk) — quyết định tại session start sau state-check.

## 7. Closure Protocol

1. Flip `gap-status.csv` rows DONE cho gaps closed wave này + git mv → `phase-1-beta/closed/`.
2. ROADMAP §🎯 Current Status Snapshot — thêm Wave flow-kc3 closure entry.
3. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`.
4. Wave plan frontmatter `status: draft → complete` + Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3.
5. Campaign §4 row KC-3 flip → `🔄 walk-pass-pending-human` (G1 ✅) + ship G2 handoff MD recipe.
6. wave-history.jsonl append tại full closure.

## 8. Log

- **2026-06-05 (plan stub ship):** Filed sau KC-2 G1 PASS (PR #2172). KC-3 = next-in-chain per campaign §3 (KC-2 → KC-3 → KC-4..9). State-check: academicyear + course module ✅; **class/schedule controller NOT found quick grep → verify-at-session-start (partial-impl risk)**. GAP-909 P2 blocker (courses entity drift, không block walk). Stub thỏa `check-wave-plan-completeness.sh` (8 sections + 4 frontmatter). Full §3 scope + pre-walk persona sim + class/schedule impl verification happens tại session start. KC-3 có thể là partial feature-build nếu class/schedule chưa impl.
