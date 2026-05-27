---
title: Session handoff 2026-05-27 — RST scope discussion + Phase 1 BETA flow gate
status: complete
created: 2026-05-27
audience: dev
session_date: 2026-05-27
session_focus: GAP-756 Phase 1 RST verify → RST scope/coverage discussion → flow gate strategy
key_decisions:
  - 126-row CSV phase-1-beta-acceptance-self-test KHÔNG đảm bảo flow thông cho toàn bộ KH+KC
  - Project canonical decision (Wave 106 + GAP-725) DEFER Parent/Teacher/Student personas Phase 2
  - UI exposure audit PARTIAL (interrupted) — KH /school-admin + KC (dashboard)/(teacher)/(student) routes exist nhưng layout role-guard incomplete
related_gaps: [GAP-756, GAP-612, GAP-725, GAP-724]
related_waves: [102.8, 102.9, 103, 105, 106, 107, beta-prep-1, rst-cascade-1]
---

# Session 2026-05-27 — RST Scope Discussion + Phase 1 BETA Flow Gate

## Bối cảnh session

User mở session với 2 goal:
1. Fix unused-var diagnostic trong `BucketEConcurrencyIT.java` line 119
2. Start GAP-756 (Wave beta-prep-1 production deploy + RST verify)

Goal 2 mở rộng thành discussion sâu về RST scope — kết quả: KHÔNG ship deploy được trong session này vì coverage gap analysis surface ra nhiều vấn đề chưa resolve.

## Đã ship

### PR #1879 — Fix Java unused var (DONE, merged squash `29220b3b`)
- 1-line delete: `final UUID ownerId = UUID.randomUUID()` line 119 (declared never used)
- CI 24/24 PASS
- Branch `fix/bucket-e-concurrency-it-unused-var` đã xoá

### PR #1880 — GAP-756 Phase 1 RST PASS + re-enable docker-build-push (OPEN, CI 32/32 PASS)
- Branch `chore/gap-756-phase-1-rst-pass`
- CI CLEAN MERGEABLE — chưa merge vì user pivot vào RST scope discussion
- Files: `.github/workflows/docker-build-push.yml` (re-enable push) + GAP-612 Log + GAP-756 PARTIAL 35% + gap-status.csv sync
- Smoke evidence: 13/13 services + admin login + 3 wave FE routes + admin approve + email delivery via mailhog + 56 Flyway migrations + Bucket E PG concurrent race + Bucket D 9-table tenant_id audit

## RST 2-định nghĩa quan trọng (cần disambiguate)

### Định nghĩa #1 — Release Self-Test (canonical per `e2e-rst-test-layer-boundary.md`)

```yaml
Trigger: Thủ công per-release (pre-Phase-gate)
Cadence: 1 cycle per major release (Phase 1 → 1.5 → 2 → 3)
Coverage: Persona-driven full journey (4-6 vai trò × 20-25 luồng)
Cost: ~3-5h human per cycle
Authority: Product-level acceptance (UX + cultural awareness)
Catches: Discovery — gap chưa spec
```

Canonical artifact: `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` (126 rows).

### Định nghĩa #2 — Restore + Smoke Test (DevOps context GAP-612/GAP-756)

```yaml
Trigger: Post-AWS-suspension recovery / pre-resume deploy
Cadence: One-time gate sau infrastructure incident
Coverage: Local stack restore + endpoint smoke
Cost: ~30 min
Authority: DevOps gate (re-enable AWS push)
Catches: Stack broken / image stale / regression in restore path
```

Canonical procedure: 6-step `up.sh --profile full` + admin login + endpoint walk + Log GAP-612 + re-enable workflow.

## Coverage analysis session này

### Phase 1 BETA acceptance test CSV (126 rows)

**Persona distribution:**
| Persona | Rows | Share |
|---|---|---|
| P2_Center_Owner | 56 | 44% |
| Platform_Admin | 24 | 19% |
| Anonymous | 17 | 13% |
| Teacher | 8 | 6% |
| Pre-tenant | 8 | 6% |
| Pa_Parent | 7 | 6% |
| All | 5 | 4% |
| Student | 1 | 1% |
| **Total** | **126** | 100% |

**Phase distribution:**
| Phase | Rows |
|---|---|
| Auth | 33 |
| Admin_Ops | 19 |
| Class_Mgmt | 18 |
| Setup | 13 |
| Settings | 11 |
| Provisioning | 8 |
| Payment | 7 |
| Branding | 6 |
| Attendance | 4 |
| Grade | 3 |
| Off-boarding | 2 |
| Data_Export | 2 |

### Business domain coverage

| Side | Business domains | CSV row coverage |
|---|---|---|
| **KH** (`documents/01-business/kitehub/`) | **24 domains** | ~80% (signup/onboard/admin/payment/branding/instance flows touched) |
| **KC** (`documents/01-business/kiteclass/`) | **45 domains** | **~30%** — chỉ classroom MVP (class + student + enroll + basic attendance + basic grade) |

### KC 25/45 domain ZERO walk trong CSV

ai-agent-workflow · ai-provider · child-protection · content-moderation · course-pricing (basic only) · data-retention · document-generation · gamification-points · k12-model · legal-ip-protection · lms · mis-integration · payment-record · payroll · quality-gate · rebrand-approval · report-card · reschedule · resource-classification · resource-handlers · storage · security-foundation · security-hardening · outbox-events · multi-tenancy explicit · multi-subject-gradebook (partial) · bulk-import (2 rows only) · student-portal (1 row only)

## Prior docs đã địa chỉ vấn đề scope extension

### 1. GAP-725 — KC Parent/Teacher auth path architectural — `🟦 DEFERRED Phase 2`

```yaml
title: KC Parent/Teacher persona auth path — architectural gap
status: 🟦 DEFERRED Phase 2
deferred_to: phase-2
discovered_via: Wave 105 RST UI walk 2026-05-23
```

**Root cause architectural blocker:**
- KH `PlatformRole` enum: OWNER / STAFF / PLATFORM_ADMIN only (không Parent / Teacher)
- KC FE `UserType`: declares ADMIN/STAFF/TEACHER/PARENT/STUDENT
- POST /api/auth/login lands KH subscription (chỉ issue OWNER/STAFF JWT)
- Net effect: KHÔNG có production login path issuing JWT `role: PARENT` or `role: TEACHER`

**Impact:** KC `/parent/*` và `/teacher/*` routes redirect `/login` infinite loop production.

### 2. Wave 106 plan — `status: draft` (filed 2026-05-23)

**File:** `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`

**Q1 scope explicit:**
> "4 vai trò Pha 1 BETA — Khách ẩn danh / Chủ trung tâm / Nhân viên / Quản trị nền tảng. **Phụ huynh + Giáo viên đã đẩy Pha 2 (GAP-725); Học sinh không nằm trong Pha 1.**"

**Q2 scope decisions:**
- ❌ "Sửa hết gap đang mở trước rồi mới RST" — rejected
- ❌ "Cắt phạm vi còn 12 hoặc 8 luồng cốt lõi" — rejected
- ❌ "Chỉ ghi gap, không sửa tại chỗ" — rejected
- ✅ **"RST đầy đủ 23 luồng + sửa tại chỗ với lỗi chặn luồng"** — chosen

23 luồng × 4 vai trò = effective ~92 row equivalents (subset of 126 CSV rows).

### 3. Wave 107 — `status: complete` (filed 2026-05-23)

**File:** `documents/03-planning/waves/wave-2026-05-23-107-hybrid-rst-anonymous-onboard-plus-email-fix.md`

Hybrid RST shipped: Mảng A (Anonymous) + B-onboard (Owner đăng nhập) + email fix. Subset của Wave 106 scope.

### 4. Wave aws-restore-1 + Wave rst-cascade-1

`wave-2026-05-26-rst-cascade-1-local-first-aws-verify.md` — local-first RST cycle shipped trước wave-beta-prep-1.

### 5. Outside-in audit V2 (2026-05-24)

**File:** `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-persona-walkthrough-v2-state-checked.md`

Personas: **P1 Solo Teacher "Vy" / P2 Center Owner "Hằng" / P3 Center Manager "Tâm"** — outside-in pattern đã apply gần đây. Methodology cited `audit-to-gap-pipeline.md` §2.8 fix-time state-check.

### 6. Wave 102.8 + 102.9 + 103

Self-test readiness + tier-2-3 + local-self-test-full-walk plans — iteration history.

## UI exposure audit (partial — interrupted)

### KH frontend exposed routes (potentially flow-broken Phase 1)

- `kitehub-frontend/src/app/(school-admin)/school-admin/bulk-import` — Bulk import UI route
- `kitehub-frontend/src/app/(school-admin)/school-admin/report-cards` — Report cards UI route
- `kitehub-frontend/src/app/(school-admin)/school-admin/teacher-management` — Teacher management UI
- `kitehub-frontend/src/app/(school-admin)/school-admin/parent-comms` — Parent communications UI
- Plus multi-class-roster, conduct, school-profile, empty-states pages

**Layout role-guard state:** `kitehub-frontend/src/app/(school-admin)/layout.tsx` documents:
> "the existing app does not yet differentiate `SCHOOL_ADMIN` vs `ADMIN` roles."
> "Accept any authenticated session (no `SCHOOL_ADMIN` role gate yet) so the port can ship without a backend role rollout."

→ ANY authenticated user CAN access these routes — bao gồm Owner persona Phase 1 BETA. Flow gãy nếu code chưa hoàn chỉnh.

### KC frontend exposed routes (potentially flow-broken Phase 1)

- `(teacher)/teacher` + `(teacher)/attendance` — Teacher routes exist
- `(dashboard)/parent` — Parent routes exist
- `(dashboard)/student` + `(dashboard)/students` — Student routes exist
- `(dashboard)/courses`, `branding`, `attendance`, `teachers`, `overview`, `billing`, `admin`, `settings`
- `(auth)/parent-invite` — parent invite flow exists

**Layout role-guard state:** `(teacher)/layout.tsx` mentions "minimal auth gate for the per-tiết attendance route" — chưa enforce Teacher role.

**KHÔNG có Next.js middleware files** `middleware.ts` cho FE — route guards client-side trong layout only.

### Audit chưa hoàn thiện

Còn cần verify:
1. Mỗi route layout có thực sự enforce role không (client-side useAuthStore role check)?
2. Owner persona thực tế có thể click vào /school-admin/bulk-import / report-cards không?
3. KC /parent /teacher /student routes thực sự throw redirect /login loop hay accessible với Owner JWT?
4. Sidebar nav Owner có expose link tới advanced KC domain pages không?

## User distinction critical (lần đầu surface session này)

User clarified 2 loại bug khác nhau:

| Type | Mô tả | Beta acceptance |
|---|---|---|
| **Flow bug** | Flow gãy giữa chừng, không thể completion (redirect loop, 5xx mid-flow, broken persona) | ❌ **KHÔNG accept** trong beta |
| **Edge case bug** | UI polish, copy issues, edge value validation, cosmetic | ✅ Accept trong beta |

→ Standard "beta accept bug" KHÔNG apply cho flow gãy. **Mọi business flow phải end-to-end thông cho beta launch.**

Với standard này, 126-row CSV KHÔNG đảm bảo:
- Parent/Teacher/Student personas flow thông (GAP-725 architectural defer)
- 25 KC domain flow thông (zero walk = unknown breakage)
- KH advanced flows (payment Phase 1.5+, off-boarding rare path)

## Discussion progression — 4 lựa chọn user đã reject/accept

### Question 1: "RST full local chưa?"
**User position:** challenge claim "RST PASS" — vì mới smoke API-level subset
**My ack:** Đã smoke ~5-7 row equivalent ≈ ~5% canonical RST coverage
**Decision:** Extend RST scope NOW (~45-60 min)
**Result:** ✅ Phase 1 EXTENDED smoke shipped (admin approve + email + Bucket E race + Bucket D schema + 56 Flyway + RabbitMQ drain)

### Question 2: "RST là gì? đã RST đúng theo định nghĩa của dự án chưa?"
**User position:** Sharper challenge — RST canonical = full persona walkthrough
**My disambiguate:** 2 definitions (Restore+Smoke vs Release Self-Test)
**Decision:** Run full 126-row RST cycle bây giờ
**Result:** ⚠️ Chưa execute — pivot vào scope discussion sau

### Question 3: "Run full 126-row RST cycle => đủ cho cả KH và toàn bộ nghiệp vụ KC chưa?"
**User position:** Coverage gap analysis demand
**My ack:** KHÔNG — KH 80% / KC 30% / 25 KC domain zero walk
**Decision:** "Extend CSV +200 rows cho full KC coverage trước khi run"
**Result:** ⚠️ Stop — user kế tiếp ask "meta nào review extend này?" (governance question)

### Question 4: "meta nào sẽ review extend này"
**User position:** Process check trước commit work
**My response:** List ~8-10 rule applicable + outside-in-coverage-trigger NOT fired (rule violation)
**Decision:** "Spawn 3 outside-in audit agents song song TRƯỚC khi author rows"
**Result:** ⚠️ Stop — user kế tiếp ask "check lại xem có tài liệu RST cũ nói về vấn đề extend này không?"

### Question 5: "check lại xem có tài liệu RST cũ nói về vấn đề extend này không?"
**User position:** State-check mandate
**My finding:** Prior docs đã consistently decide Phase 1 BETA scope restrained:
- GAP-725: Parent/Teacher defer Phase 2
- Wave 106 Q1: explicit Phase 2 scope deferral
- Wave 106 Q2: rejected scope expansion
- Wave 107: hybrid subset shipped
- Wave 102.8/102.9/103: prior iteration history
**Verdict:** Đề xuất "+200 rows extend" CONFLICT với decisions đã filed multiple times
**Result:** ⚠️ Stop — user pivot vào "Nhưng 126 row đấy có đảm bảo full nghiệp vụ KH KC cho beta chạy không bug production không?"

### Question 6: "Nhưng 126 row đấy có đảm bảo full nghiệp vụ KH KC cho beta chạy không bug production không?"
**User position:** Critical business risk assessment
**My ack:** KHÔNG — production beta WILL have bugs given coverage gaps + GAP-725 + 25 KC domain zero walk
**Decision:** User clarify "beta chấp nhận bug nhưng KHÔNG được flow gãy"
**Result:** ⚠️ Strict standard set — flow must always be working

### Question 7: "Beta accept bug nhưng flow phải luôn thông cho mọi nghiệp vụ KH KC? 126 row đảm bảo được không?"
**User position:** Even stricter — flow gate strict standard
**My ack:** KHÔNG — Parent/Teacher 100% guaranteed flow gãy (GAP-725); 25 KC domain unknown
**Decision:** "Audit UI exposure trước — verify Phase 1 UI chỉ expose Owner+Anonymous+Admin flows"
**Result:** ⚠️ Audit interrupted at finding stage (partial findings logged §UI exposure audit above)

### Question 8 (final): "Logs hết discuss về RST lần này lại để lần sau dễ tham chiếu"
**Result:** ✅ This document.

## Decision frame cho session sau

### Lựa chọn A: Strict scope — UI hide undeveloped flows (engineer effort)
- **Audit UI exposure complete** (continue from §UI exposure audit partial)
- File GAP-NEW-feature-flag-phase-1-ui-restrict P0 cho mọi route flow gãy
- Hide UI routes: KC `/parent` `/teacher` `/student` + KH `/school-admin/bulk-import` `/report-cards` etc.
- Test với hidden UI → Owner persona ONLY accessible → 126-row CSV cover ~70% Owner flow
- Effort: ~3-5h (UI audit + feature flag work + smoke verify)

### Lựa chọn B: Full RST coverage trước beta (testing effort)
- Fix GAP-725 architectural blocker first (~5-8h Phase 2 work)
- Extend CSV +200 rows full coverage (~3-4h authoring)
- Run 326-row cycle (~6-8h walkthrough)
- Plus UI feature flag for unfinished domains (~2-3h)
- Total: ~16-23h work + GAP-725 fix gate Phase 2 timeline

### Lựa chọn C: Accept project's existing risk model + execute Wave 106 scope
- Run 126-row CSV theo Wave 106 plan canonical (23 luồng × 4 vai trò Phase 1)
- Mark Parent/Teacher/Student rows blocked-GAP-725
- 5 beta tenants live + 0 P0 incidents 2 tuần = gate per CLAUDE.md Phase 2 trigger
- Risk tolerance Moderate per project decision
- Effort: ~3-5h cycle execute

### Lựa chọn D: Quality audit /100 first + simulation-gap-finder
- Run `quality-audit` skill /100 trên main HEAD post-Wave-beta-prep-1
- Run `simulation-gap-finder` 3-axis matrix
- Evidence-based decision dựa output audit
- Effort: ~45-60 min audit
- Output: list flows actually broken vs feature-flagged

## Open questions next session

1. **UI exposure final verdict:** Mọi route layout có enforce role-guard không? Owner persona thực tế có thể click vào KC Parent/Teacher/Student routes không?
2. **GAP-725 fix scope:** Architectural fix Phase 2 — bao nhiêu effort? Options A/B/C (per gap §Proposed Fix) chọn cái nào?
3. **Feature flag mechanism:** Project có pattern feature flag cho UI hide không? Hay enforcement runtime role redirect?
4. **Beta cohort exact scope:** 5 beta tenants = Owner persona only? Hay multi-role tenant?
5. **PR #1880 fate:** Merge or defer? CI green CLEAN; chỉ docs change + workflow yml re-enable. Decision pending RST scope answer.
6. **Outside-in audit invocation:** Run 3-agent audit (persona/benchmark/failure-mode) trước khi commit RST extension? User reject inside scope discussion này.

## Files touched session này

- ✅ `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/concurrency/BucketEConcurrencyIT.java` (PR #1879, merged `29220b3b`)
- ✅ `.github/workflows/docker-build-push.yml` (PR #1880, re-enable push triggers)
- ✅ `documents/04-quality/gaps/phase-1-beta/closed/GAP-612-aws-account-suspension-recovery.md` (PR #1880, 2 AC checked + Log entries)
- ✅ `documents/04-quality/gaps/phase-1-beta/GAP-756-wave-beta-prep-1-production-deploy-rst-verify.md` (PR #1880, Status PARTIAL 25→35%)
- ✅ `documents/04-quality/gaps/gap-status.csv` (PR #1880, GAP-756 row sync)
- ✅ This file (untracked, session-handoff)

## Rules cited session này (governance reference)

- `e2e-rst-test-layer-boundary.md` v1.0.0 — RST canonical definition #1
- `test-artifact-format-standard.md` v1.0.1 — CSV format requirements
- `dev-readable-doc-language.md` v1.0.2 — narrative Vietnamese
- `vn-localization-audit-checklist.md` v1.0.0 — sample data + culture
- `outside-in-coverage-trigger.md` v1.1.0 — inside-out trigger (rule fired)
- `audit-to-gap-pipeline.md` v1.4.3 — state-check mandate
- `gap-done-discipline.md` v1.0.1 — DONE flip criteria
- `pre-handoff-self-test-completeness.md` v1.1.1 — §2.4 admin-flow checklist
- `release-deploy-standard.md` v1.2.0 — §9 deploy execution mandate
- `agent-aws-access.md` — Tier 3 deploy human-only
- `agent-model-opus-default.md` v1.0.0 — agent spawn discipline
- `post-merge-sync-completeness.md` v1.0.1 — 4-target sync
- `docs-only-pr-auto-merge.md` v1.0.2 — §3 mixed-scope manual confirm
- `admin-merge-discipline.md` v1.0.3 — `--admin` flag banned

## Pickup state next session

1. **PR #1880 CI green CLEAN** — decide merge vs hold (recommend merge since gate work independent)
2. **UI exposure audit continue** — verify role-guard runtime behavior + sidebar nav exposure
3. **Choose strategy A/B/C/D** based on UI audit verdict + business risk tolerance
4. **AWS stack stopped** — restart `bash scripts/aws/start-stack.sh` if Phase 2+3 deploy progresses
5. **Wave 106 plan stays `draft`** — activate as new wave plan OR fold into different wave scope per chosen strategy

## Cross-link to related artifacts

- Acceptance test CSV: `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` (126 rows)
- Acceptance test README: `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.md`
- Wave 106 plan (draft): `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`
- Wave 107 plan (complete): `documents/03-planning/waves/wave-2026-05-23-107-hybrid-rst-anonymous-onboard-plus-email-fix.md`
- GAP-725 (Parent/Teacher architectural): `documents/04-quality/gaps/phase-2/GAP-725-kc-parent-teacher-auth-path-architectural.md`
- Outside-in V2 audit: `documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-persona-walkthrough-v2-state-checked.md`
- Wave beta-prep-1 plan: `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md`
- Phase 1 BETA persona walkthrough: `documents/04-quality/audits/persona-review/2026-05-14-phase-1-beta-persona-walkthrough.md`
