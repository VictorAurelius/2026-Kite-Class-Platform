---
title: Wave meta-6 — Fix P0 staff invite + Meta closure-completeness + Wave rst-html-1 Path B
status: planning
created: 2026-05-27
waves: [meta-6]
wave: 6
tag_primary: meta
tags_secondary: [hotfix, tooling, beta-prep, rst-html]
counter: 6
date_launch: 2026-05-27
audience: dev
---

# Wave meta-6 — Fix P0 staff invite + Meta update closure-completeness + Wave rst-html-1 Path B

## TL;DR

3 buckets parallel-eligible (~3-5 ngày coordinator-inline OR ~1-1.5 ngày 3-agent parallel):

1. **Bucket A — Fix P0 staff invite** (GAP-772 + GAP-773 paired) — KC StaffInvitation entity + Controller + V64 migration + FE `/staff/accept-invite` route. Mirror `parent-invitations` pattern (template tồn tại).
2. **Bucket B — Meta update closure-completeness** — bump `wave-closure-scope-completeness.md` v1.0.0 → v1.0.1 PATCH với recurrence #2 (GAP-774 D4 audit log Wave 92 escape) + close GAP-770 + retroactive audit Wave 92 + Wave 79 closure reconciliation tables + decide detector wiring per recurrence ≥2 condition.
3. **Bucket C — Wave rst-html-1 Path B execute** — merge PR #1888 plan + execute 3 sub-buckets narrow scope Mảng A only (capture screenshots + custom landing HTML 1 page + annotation script).

## 1. Brainstorm (5 phút)

### Q1 — Inside-out completeness (per `inside-out-completeness-trigger.md` §3)

| Source | Items | In scope |
|---|---|---|
| User explicit (this session 2026-05-27) | "fix P0", "update meta", "wave rst html path B" | ✅ tất cả 3 |
| ROADMAP §🚀 | Wave 107 hybrid (email fix) | ⏳ defer Wave 107 sau |
| Inside-out queue file | (não consult; user explicit 3 items) | (skip) |
| Recent gap filings | GAP-772/773 P0 from Wave 106 Mảng B-D probe | ✅ Bucket A |

### Q2 — Outside-in coverage (per `outside-in-coverage-trigger.md` §4 row 4)

Skip outside-in audit cho wave này vì:
- Recent outside-in coverage ≤30 ngày: Wave 100 persona × 4 vai trò 2026-05-19 + PR #1888 outside-in 3 Opus agents 2026-05-27 → cumulative coverage đủ
- 3 buckets đều technical/governance fix — no user-facing scope decisions
- Bucket C rst-html-1 đã có outside-in audit của riêng nó (re-scope từ Allure 6-bucket → MVP 3-bucket per PR #1888)

Documented skip per `outside-in-coverage-trigger.md` §4 ending mandate.

### Q3 — Bậc rủi ro

TRUNG BÌNH (Opus medium model cho 3 buckets):
- Bucket A: schema change (V64) + new entity + role-guard — touches kiteclass-core BE layer
- Bucket B: rule version bump + retroactive audit — docs governance scope
- Bucket C: tooling (HTML + scripts) — no production code

Không đụng kiến trúc, không cross-layer drift (Bucket A FE↔BE đã có pattern parent-invitations làm template per `contract-first-for-cross-layer.md` §2 — contract via parent precedent existing, KHÔNG cần Bucket 0 Foundation).

### Q4 — Dependencies

- Bucket A ⊥ B ⊥ C (disjoint files, parallel-safe)
- Bucket A depends on V63 last migration committed (verified main HEAD)
- Bucket C depends on PR #1888 merge (sequential)

---

## 2. Task Breakdown

### Bucket A — Fix P0 staff invite (GAP-772 + GAP-773)

**Effort:** ~1-2 ngày
**Owner:** Backend + Frontend

Sub-tasks:
1. KC Entity `StaffInvitation` (kiteclass/kiteclass-core/.../module/staff/):
   - Fields: id, tenant_id, email, role, token (UUID), expires_at, status (PENDING/ACCEPTED/EXPIRED/CANCELLED), created_by_user_id, created_at, accepted_at
   - JPA mapping với standard pattern (no Postgres-specific types per `postgres-specific-type-testcontainers.md` §3)
2. V64 Flyway migration `kiteclass-core/src/main/resources/db/migration/V64__create_staff_invitations.sql`
3. `StaffInvitationRepository` extends JpaRepository
4. `StaffInvitationController` (`/api/v1/staff-invitations`):
   - `POST /` — Owner creates invite (role-guard OWNER); generate token + 7-day expiry; trigger email
   - `GET /` — list pending cho current tenant
   - `DELETE /{id}` — Owner cancel
   - `POST /{token}/accept` — Staff claim (creates User row với role STAFF + links tenant_id)
5. Audit logging via `Propagation.REQUIRES_NEW` per `audit-service-isolation.md` v1.0.0 (NEW entity creation = sensitive action)
6. Email template Vietnamese narrative per `vn-localization-audit-checklist.md` §2 (subject: `Mời gia nhập Trung tâm {tên} trên KiteHub`)
7. FE `kiteclass-frontend/src/app/(auth)/staff-invite/[token]/page.tsx` — mirror `parent-invite/[token]/page.tsx`
8. FE Owner-side `kiteclass-frontend/src/app/(dashboard)/staff/invitations/page.tsx` — list + create + cancel
9. Integration tests:
   - `StaffInvitationPostgresIT` Testcontainers per `postgres-specific-type-testcontainers.md` §1 (mandatory for new entity)
   - Cross-tenant isolation IT (sister-pattern per Wave meta-3 GAP-746 fix)
   - VN diacritic roundtrip per `vn-localization-audit-checklist.md` v1.1.0 §5
10. RST→E2E paired specs per `e2e-rst-test-layer-boundary.md` §3:
    - `kiteclass-frontend/e2e/staff-invite-claim.spec.ts` — Owner create + Staff claim end-to-end
    - `kiteclass-frontend/e2e/staff-invite-role-guard.spec.ts` — Staff role-guard 403 confirm

### Bucket B — Meta update closure-completeness v1.0.1

**Effort:** ~0.5 ngày
**Owner:** Meta governance

Sub-tasks:
1. Bump `.claude/rules/wave-closure-scope-completeness.md` v1.0.0 → v1.0.1 PATCH:
   - §11 Log entry: recurrence #2 = GAP-774 D4 audit log (V62/V63 schema ship Wave 92 nhưng không có Controller + FE page; closure reconciliation table escape)
   - Check `incident-to-rule-pipeline.md` §3.1 detector trigger conditions: recurrence ≥2 confirmed → consider ship detector now (§5.3 `session-docs-check` Rule N proposal)
2. Retroactive audit Wave 92 closure PR #1517 (per `gap-folder-organization.md` v2.0.0 — closed gap location):
   - Did PR have Scope-Completeness Reconciliation table?
   - Did 6 plan §3 items get categorized ✅/🟡/❌ + follow-up gap links?
   - Document findings inline
3. Retroactive audit Wave 79 closure (per GAP-770 §Problem):
   - GAP-767 `/faq` route 404 from Mảng A — orphan from Wave 79?
4. Close `GAP-770` (META audit Wave 92 + Wave 79 retroactive) — flip 🟢 DONE with reconciliation evidence
5. Detector ship (if §3.1 conditions met): `scripts/check-wave-closure-completeness.sh` heuristic scan wave plan `status: complete` diff line → require matching closure PR body OR §7 contains "Scope-Completeness Reconciliation" heading + table rows ≥ §3 bucket count. WARN-mode initially.
6. Optional: file follow-up gap nếu detector defer per §3.1 (recurrence < 2 was prior condition; now ≥2 confirmed → ship)
7. Update `documents/04-quality/audits/audits-index.csv` row cho audit artifact (per `meta-csv-index-pattern.md` §3)
8. Sync `gap-status.csv` cho GAP-770 status flip

### Bucket C — Wave rst-html-1 Path B execute

**Effort:** ~2-3 ngày
**Owner:** Tooling
**Reference:** PR #1888 plan `wave-2026-05-23-rst-html-1-...md` (merge first)

Sub-tasks (per PR #1888 §3):
1. **C1 Capture screenshots ở RST spec steps (Path B narrow = Mảng A only):**
   - Modify existing Playwright RST specs for Mảng A1/A2/A3 to call `page.screenshot()` at step boundaries
   - Output to `/tmp/rst-screenshots/wave-106-mang-a/<flow>-<step>.png` per Wave 106 plan §7.6
   - Defer Mảng B-D capture to follow-up wave (post-human-walk completion)
2. **C2 Custom landing HTML 1 page:**
   - NEW folder `documents/04-quality/audits/rst-html/wave-106-mang-a/`
   - `index.html` reuse `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` tokens
   - Top-of-fold SHIP/HOLD verdict per outside-in audit findings
   - Per-test drilldown link to Playwright HTML reporter native output
   - Vietnamese narrative + VN sample data per `vn-localization-audit-checklist.md` §2
3. **C3 Annotation script:**
   - NEW `scripts/render-rst-screenshots.sh` — annotate raw screenshots with red arrows + yellow highlights + step numbers per `user-manual-content-standard.md` §2 row 6
   - Reusable cho user manual scope (Wave 79 Bucket F1 anonymous + future P2/P3 manual)
   - Engine: ImageMagick `convert` (no JS deps)
4. Per-rule compliance (per PR #1888 §"Per-rule compliance"):
   - `wave-tag-numbering-convention.md` ✅ (wave-rst-html-1 OR sub-bucket của wave-meta-6 ✅)
   - `audit-to-gap-pipeline.md` §2.6 State-Check Evidence ✅
   - `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 5 findings in §1 ✅
   - `meta-gap-priority.md` §3 META P1 force-multiplier ✅
   - `docs-folder-volume-budget.md` §2.3 `DOCS_VOLUME_OVERRIDE` trailer if new folder pushes count over cap
5. **Decision needed: keep wave-rst-html-1 as standalone PR #1888 OR fold into wave-meta-6 Bucket C?**
   - Recommendation: keep PR #1888 as standalone — wave-meta-6 Bucket C ship trigger (merge + execute) Path B narrow scope only
   - PR #1888 plan file stays as canonical wave-rst-html-1 plan; wave-meta-6 §3 Bucket C references it

---

## 3. Scope (lược đồ rút gọn)

| # | Bucket | Files modified | Disjoint? |
|:-:|---|---|:---:|
| A | Fix P0 staff invite | `kiteclass-core/.../module/staff/**` (NEW) + `kiteclass-frontend/src/app/(auth)/staff-invite/**` (NEW) + `(dashboard)/staff/invitations/**` (NEW) + V64 migration | ✅ entirely new scope |
| B | Meta update | `.claude/rules/wave-closure-scope-completeness.md` + `documents/04-quality/audits/meta/2026-05-27-wave-92-79-closure-retroactive.md` (NEW) + `scripts/check-wave-closure-completeness.sh` (NEW, optional) + 2 CSV syncs | ✅ governance files only |
| C | Wave rst-html-1 Path B | `documents/04-quality/audits/rst-html/wave-106-mang-a/**` (NEW) + `scripts/render-rst-screenshots.sh` (NEW) + Playwright spec edits Mảng A | ✅ NEW folder + tooling |

### State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol/Path | Verification | Verdict |
|---|---|---|
| `kiteclass/kiteclass-core/.../module/parent/` (template Bucket A mirror) | `ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/` | ✅ exists |
| `ParentInvitationController.java` (template) | `find kiteclass/kiteclass-core -name "ParentInvitationController.java"` | ✅ exists |
| `kiteclass-frontend/src/app/(auth)/parent-invite/[token]/page.tsx` (FE template) | `ls kiteclass/kiteclass-frontend/src/app/\(auth\)/parent-invite/\[token\]/page.tsx` | ✅ exists |
| V63 last migration (Bucket A V64 next) | `ls kiteclass/kiteclass-core/src/main/resources/db/migration/V6[0-9]*.sql` | ✅ V63 last shipped Wave 92 |
| `.claude/rules/wave-closure-scope-completeness.md` v1.0.0 | `grep "Version:" .claude/rules/wave-closure-scope-completeness.md` | ✅ v1.0.0 (Bucket B bump v1.0.1) |
| GAP-770 file (Bucket B closes) | `bash scripts/query-gaps.sh GAP-770` | ✅ exists OPEN |
| PR #1888 plan file (Bucket C merge) | `gh pr view 1888 --json state` | ✅ OPEN draft |
| `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` (Bucket C reuse) | `ls documents/02-architecture/design-system/ui_kits/_shared/` | ✅ exists |
| `staff/accept-invite` FE route (Bucket A creates) | `find kiteclass/kiteclass-frontend/src/app -path "*staff*"` | 🆕 to-be-created (Bucket A) |
| `StaffInvitationController.java` (Bucket A creates) | `find kiteclass/kiteclass-core -name "StaffInvitation*"` | 🆕 to-be-created (Bucket A) |
| V64 migration file (Bucket A creates) | (none expected pre-Bucket-A) | 🆕 to-be-created (Bucket A) |
| `scripts/check-wave-closure-completeness.sh` (Bucket B optional) | (none expected) | 🆕 to-be-created (Bucket B) |
| `scripts/render-rst-screenshots.sh` (Bucket C creates) | (none expected) | 🆕 to-be-created (Bucket C) |

---

## 4. State-Check Evidence

Xem §3 "State-Check Evidence (per audit-to-gap-pipeline.md §2.6)" — 8/13 symbols verified exist ✅ + 5/13 explicit 🆕 to-be-created with bucket owner.

## 5. Verification Gates

| Bucket | Lệnh kiểm thử cục bộ | CI gate |
|---|---|---|
| A | `cd kiteclass/kiteclass-core && ./mvnw verify -pl kiteclass-core -P strict-warnings` + `pnpm -F kiteclass-frontend test --run && pnpm -F kiteclass-frontend build` + Testcontainers IT round-trip | core-ci + frontend-ci + `entity-mapper-consistency` |
| B | `bash scripts/check-rule-frontmatter.sh` + `bash scripts/check-rule-staleness.sh` + `bash scripts/check-gap-status-csv.sh` + (optional) `bash scripts/check-wave-closure-completeness.sh --dry-run` | script-quality + rule-staleness |
| C | `bash scripts/check-docs-folder-volume.sh` (verify new folder under cap) + screenshot capture local run smoke | script-quality |

## 6. Agent Spawn Pattern

**Recommendation: 3-agent parallel với Opus 4.7 per `agent-model-opus-default.md` v1.0.0 + `agent-background-spawn-default.md` v1.0.1.**

Bucket A ⊥ B ⊥ C disjoint files → safe parallel spawn:

```python
Agent(model="opus", run_in_background=true, subagent_type="general-purpose",
      isolation="worktree", description="Bucket A staff invite", prompt="...")
Agent(model="opus", run_in_background=true, subagent_type="general-purpose",
      isolation="worktree", description="Bucket B meta closure-completeness", prompt="...")
Agent(model="opus", run_in_background=true, subagent_type="general-purpose",
      isolation="worktree", description="Bucket C rst-html-1 Path B", prompt="...")
```

Coordinator merge sequentially A → B → C sau cả 3 ship completion notification. Per `release-fix-retry-budget.md` §3.5 — nếu Bucket A retry #2 trigger, investigation phase mandate fires.

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md` v1.0.0 (paired bump v1.0.1 trong Bucket B này):

1. **File này** flip `status: planning → in-progress` khi agent spawn, → `complete` khi 3 buckets DONE
2. **Scope-Completeness Reconciliation table** bắt buộc trong closure PR body:
   - Bucket A: ✅DONE / 🟡PARTIAL / ❌NOT-IMPL với follow-up
   - Bucket B: ✅DONE / 🟡 với follow-up
   - Bucket C: ✅DONE / 🟡 với follow-up
3. **GAPs closed:** GAP-772, GAP-773 (Bucket A), GAP-770 (Bucket B)
4. **RST→E2E promotion mandate** (per `e2e-rst-test-layer-boundary.md` §3): Bucket A fix pairs new E2E specs same PR — required, not exempt
5. **wave-history.jsonl append** entry với new format (per `wave-tag-numbering-convention.md` §2.5):
   ```json
   {
     "wave": "meta-6",
     "tag_primary": "meta",
     "tags_secondary": ["hotfix", "tooling", "beta-prep", "rst-html"],
     "counter": 6,
     "date": "2026-05-27",
     "theme": "Fix P0 staff invite + Meta closure-completeness v1.0.1 + Wave rst-html-1 Path B"
   }
   ```
6. **ROADMAP §🎯 Current Status Snapshot** entry append
7. **Post-wave audit suite** ≤3 ngày per `post-wave-audit-mandate.md` §2.4 domain milestone trigger (security + ops-readiness + api-contract suite required cho Bucket A scope; Bucket B+C governance scope exempt per §2.4.1 `meta-governance` row)
8. **Local CI parity** (per `ci-queue-local-runner-threshold.md` §3) — Bucket A code change requires CI canonical; Bucket B+C docs-only auto-merge eligible

### Tiêu chí kết đợt

- [ ] 3/3 buckets DONE OR có scope-completeness reconciliation explicit cho PARTIAL items
- [ ] GAP-772 + GAP-773 + GAP-770 flipped 🟢 DONE
- [ ] V64 migration applied + tested
- [ ] FE `/staff/accept-invite` route 200 + role-guard verified
- [ ] `wave-closure-scope-completeness.md` v1.0.1 shipped với recurrence #2 Log entry
- [ ] PR #1888 merged (Bucket C dependency)
- [ ] Screenshot capture working cho Mảng A
- [ ] HTML dashboard render Mảng A artifacts
- [ ] Annotation script smoke test PASS
- [ ] Post-wave audit suite triggered ≤3 ngày deadline 2026-05-30 (cho Bucket A code change scope)

---

## 8. Log

- **2026-05-27 (status: planning):** Wave plan created in response to user direction post Wave 106 Mảng B-D probe layer ship: "tạo wave mới: fix P0, update meta, wave rst html path B". 3 buckets disjoint parallel-eligible. Bucket A fix GAP-772+GAP-773 P0 staff invite (mirror parent-invitations); Bucket B meta update closure-completeness v1.0.1 với recurrence #2 (GAP-774 D4 → consider detector ship per `incident-to-rule-pipeline.md` §3.1); Bucket C wave-rst-html-1 Path B narrow scope (Mảng A only, PR #1888 plan merge + execute). Outside-in audit skipped per `outside-in-coverage-trigger.md` §4 row 4 (recent coverage ≤30 ngày — Wave 100 persona + PR #1888 audit). Wave numbering per `wave-tag-numbering-convention.md` v1.0.0 — tag_primary=meta, counter=6 (next monotonic per meta-1..meta-5 history). Inside-out 3-source per `inside-out-completeness-trigger.md` §3 — user-explicit 3 items + Wave 106 leftover P0 from probe layer + PR #1888 existing draft. Reviewer: @nguyenvankiet.

---

## 9. Risks + Mitigations

| Risk | Mitigation |
|---|---|
| Bucket A schema change V64 conflicts với in-flight V64 in another branch | State-check pre-spawn: `git fetch && find kiteclass-core -name V64*.sql` confirm clear |
| Bucket B detector ship gây CI noise (heuristic FP) | WARN-mode initial 30 ngày grace period per `incident-to-rule-pipeline.md` §3.1 stabilization pattern |
| Bucket C Mảng B-D screenshots defer → HTML dashboard narrow Mảng A only | Document Path B narrow scope explicit; follow-up wave extends khi human walk B-D complete |
| Parallel agent Sonnet thrash recurrence | Mandatory Opus per `agent-model-opus-default.md` §1 (wave-history shows recurrence ≥2 → Opus only) |
| Cross-bucket contract drift (Bucket A FE↔BE) | Mirror parent-invitations precedent (existing template) — không phải net-new contract design |

## 10. Out-of-scope

- Mảng B-D browser human walk (different layer — defer follow-up wave per Wave 106 handoff §"Next session")
- Wave 107 hybrid email fix (separate wave, GAP-762/763/765/766/767/768 deferred)
- GAP-774 D4 audit log Controller (P1, defer Wave 107+ OR Mảng D ship cùng human-walk)
- Other P1/P2 from Wave 106 (GAP-775/776/777/778/779/780/781) — Wave 107+
