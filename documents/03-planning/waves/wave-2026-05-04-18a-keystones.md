---
title: Wave 18a — Cross-Persona Keystones Phase 1 (GAP-290 + GAP-063 P1 + GAP-057 P1)
status: complete
created: 2026-05-04
updated: 2026-05-04
waves: [18a]
gaps: [GAP-290, GAP-063, GAP-057]
phase: 1
expected_outputs: 3 PRs (1 full ship + 2 Phase 1) + 2 sister gaps filed (GAP-063b, GAP-057b) + closure PR
actual_outputs: 6 PRs merged (#756 plan + #757 audit + #758 Bucket C + #759 Bucket B + #760 Bucket A + #761 skip audit) + closure PR (this) filing GAP-063b + GAP-057b. GAP-290 → 🟢 DONE; GAP-063 + GAP-057 → 🟡 PARTIAL. Wall-clock ~3.5h total. 0-clarification on all 3 agents (3 consecutive). 2 mid-flight CI fixes: admin app scan packages + TS strict-mode array-access.
strategy: Phase-1 wave-pack (Legal-BRD precedent) — 3 disjoint buckets, parallel agents
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 18a — Cross-Persona Keystones Phase 1

**Wave kickoff readiness:** 🟢 ALL preconditions met (Wave 17 closed 2026-05-04 SHIPPED; 4 persona reviews available; 0 blocker gaps for Phase 1 scope).

**Wall-clock estimate:** Foundation PR ~30 min + 3 parallel agents ~1.5-2h longest path + sequential merge ~20 min + closure PR ~20 min = **~2.5-3h total**.

**Methodology:** Phase-1 wave-pack pattern (precedent: Wave 13/14 Legal-BRD Phase 1+1.5, ~30 min wall-clock at 5x speedup). Each bucket Phase 1 only; defer remaining work to sister gaps GAP-063b + GAP-057b (Bucket A GAP-290 ships full, no Phase 2 needed).

---

## §1 Brainstorm (Superpowers methodology)

### Q1 — Persona alignment

| Bucket | Gap | Personas affected | Wave 17 evidence |
|--------|-----|-------------------|------------------|
| A | GAP-290 (Recurring class) | P1 + P2 + P3 + P5 | AC-OPS-002 FAIL trong P1 review |
| B | GAP-063 Phase 1 | All 4 Tier-1 | 8/21 P2 FAILs depend on notification; P5 child protection requires Zalo (deferred Phase 2) |
| C | GAP-057 Phase 1 | P2 + P3 + P5 | P3 Medium Center 9.6/100 — commission/payroll = top blocker |

All 3 gaps are cross-persona keystones — fixing them lifts measured persona scores from Wave 17 baseline.

### Q2 — Trade-offs (詳細設計 layer)

| Decision | Option chosen | Reason |
|----------|---------------|--------|
| Bucket A library | `ical4j` (Apache-2.0) | RFC 5545 standard, mature, no GPL risk |
| Bucket B interface location | `kitehub-email/api/NotificationChannel` | Avoid premature shared lib; extract khi 2nd adapter ship (GAP-063b) |
| Bucket B preference entity location | `kitehub-subscription` | Belongs với User aggregate |
| Bucket C entity location | `kiteclass-core/module/payroll` NEW | Greenfield clean isolation |
| Phase 1 UI scope | A: full form (per AC); B: settings page; C: read-only list | Match AC strictly; minimize FE work |
| Migration V-numbers | A: V47 (kiteclass-core), C: V48 (kiteclass-core), B: V23 (kitehub-subscription) | Reserved here to avoid agent collisions; state-check 2026-05-04: kiteclass-core latest=V46, kitehub-subscription latest=V22 |

**Phase 1 scope cuts (deferred sister gaps):**
- **GAP-063b:** Zalo ZNS adapter + SMS adapter (Twilio/VNStack) + quiet hours + fallback chain + cost tracking
- **GAP-057b:** COMMISSION/SALARY/HYBRID payroll types + VN tax (TNCN) + BHXH/BHYT + PDF payslip + bank export format + admin run/approve UI

### Q3 — Risks + mitigations

| # | Risk | Mitigation |
|---|------|-----------|
| 1 | Timezone (Bucket A): RRULE store UTC vs render Asia/Ho_Chi_Minh | Test cases include timezone; ical4j default UTC verified |
| 2 | Edit recurrence past sessions (Bucket A) | State machine: `attendance_taken=true` sessions immutable |
| 3 | Existing SES email callers break post-refactor (Bucket B) | Backward-compat signatures; IT covers existing callers |
| 4 | JSONB schema (Bucket A) | MUST pair `@JdbcTypeCode(SqlTypes.JSON)` per memory `feedback_jpa_jsonb_jdbctypecode.md` |
| 5 | Long-running agents (~1-2h) → SSH SIGHUP risk | Use mosh + tmux + ntfy stack (Wave 17 lessons); commit-after-each-file mandate per `feedback_agent_kill_root_cause.md` |
| 6 | Outbox pattern (Bucket B) | Notification send qua Outbox per `design-patterns.md` §3.5.1 |
| 7 | Test ObjectMapper JSR-310 (Bucket B+C) | `findAndRegisterModules()` per memory `feedback_objectmapper_test_jsr310.md` |
| 8 | Bucket A + C both touch `kiteclass-core` Flyway directory | V-numbers reserved; no merge conflict |

### Q4 — File-overlap analysis (HARD vs SOFT)

| File/area | A | B | C | Conflict |
|---|:-:|:-:|:-:|:-:|
| `kiteclass-core/module/clazz/*` | WRITE | — | READ | SOFT |
| `kiteclass-core/module/payroll/*` | — | — | WRITE (NEW) | NONE |
| `kiteclass-core/db/migration/V47-V48` | V47 | — | V48 | NONE (reserved) |
| `kitehub-email/*` | — | WRITE | — | NONE |
| `kitehub-subscription/module/notification/*` | — | WRITE (NEW) | — | NONE |
| `kitehub-subscription/db/migration/V23` | — | V23 | — | NONE (reserved) |
| FE `kiteclass-frontend/.../classes/new` | WRITE | — | — | NONE |
| FE `kiteclass-frontend/.../admin/payroll` | — | — | WRITE (NEW) | NONE |
| FE `kitehub-frontend/.../settings/notifications` | — | WRITE (NEW) | — | NONE |
| Business docs (3-layer per domain) | clazz UPDATE | notification NEW | payroll NEW | NONE |

**Verdict:** 0 HARD conflicts, 1 SOFT (Bucket C reads Bucket A's `ClassSession.hours` schema — read-only). Disjoint per `feedback_parallel_agent_strategy.md` rule #5 (different files = parallel-safe).

---

## §2 Task Breakdown

| # | Task | Phase | Wall-clock | Owner |
|---|------|:-:|:-:|---|
| 1 | This wave plan + foundation skeleton | 1 | 30 min | Claude (parent) |
| 2 | Foundation PR review + merge | 1 | 5 min | User approve |
| 3 | Spawn 3 background agents (worktree-isolated, run_in_background:true) | 2 | <5 min | Claude (parent, fresh session) |
| 4a | Agent A — GAP-290 full ship | 2 | ~1.5h | Background agent |
| 4b | Agent B — GAP-063 Phase 1 | 2 | ~1.5h | Background agent |
| 4c | Agent C — GAP-057 Phase 1 | 2 | ~2h | Background agent |
| 5 | 3 individual PRs CI green + sequential merge | 2 | ~20 min | Claude (parent) |
| 6 | Closure PR — ROADMAP sync + memory entry + GAP status flips + sister gaps GAP-063b/057b filed | 2 | ~20 min | Claude (parent) |

**Phase 1 total:** ~35 min. **Phase 2 total:** ~2-2.5h wall-clock.

---

## §3 Scope per Bucket (基本設計 layer)

### Bucket A — GAP-290 Recurring Class Generator (full ship)

| Item | Value |
|------|-------|
| Branch (in worktree) | `wave/18a-bucket-a-recurring-class` |
| Domain | `kiteclass-core/module/clazz` |
| Files (write) | `entity/Class.java` (extend), `service/RecurrenceService.java` NEW, `controller/ClassController.java` (endpoint add), `dto/RecurrenceRuleDto.java` NEW, V47 migration |
| **State-check 2026-05-04** | `kiteclass-core/module/k12/entity/ClassScheduleSlot.java` (GAP-099 Phase 1) đã có structured slot pattern cho K-12, javadoc note "Phase 2 future: iCal feed + attendance session generator". **Agent decision needed:** (a) extend ClassScheduleSlot tới non-K12 personas, hoặc (b) build RecurrenceService riêng cho clazz module với cùng RRULE pattern. Recommend (b) for module isolation; reference ClassScheduleSlot làm pattern. |
| FE files | `kiteclass-frontend/src/app/(authenticated)/classes/new/page.tsx` + `components/RecurrenceForm.tsx` NEW |
| Library | `ical4j` 4.0.x (Apache-2.0) |
| Tests | `RecurrenceServiceTest` (unit, edge cases: 1 session, 100 sessions, leap year, exclude dates), `ClassControllerIT` (full create flow) |
| Business docs | `documents/01-business/kiteclass/clazz/{rules.md, use-cases.md, api-contract.md}` UPDATE — add UC-CLASS-RECURRING + BR-CLASS-009 (recurrence_rule schema) + endpoint contract |
| 4-layer | 要件: AC-OPS-002 (P1 review) + cross-persona blast / 基本: form mockup + flow / 詳細: state machine for edit / コンポ: RecurrenceForm widget |
| Acceptance | All 8 ACs in GAP-290 met; CI green; business docs updated same PR |

### Bucket B — GAP-063 Phase 1 Notification Abstraction

| Item | Value |
|------|-------|
| Branch | `wave/18a-bucket-b-notification-abstraction` |
| Domain | `kitehub-email` (refactor) + `kitehub-subscription/module/notification` (new) |
| Files (write) | `kitehub-email/api/NotificationChannel.java` NEW interface, `kitehub-email/service/SESEmailService.java` REFACTOR (implements interface), `kitehub-subscription/module/notification/{entity/NotificationPreference.java, repository/, service/, controller/}` NEW, V23 migration |
| **State-check 2026-05-04** | `kitehub-subscription/V18__add_notification_preferences.sql` (GAP-098) đã thêm 2 boolean columns `email_notifications` + `trial_reminders` trên `instances` table — instance-level coarse preferences. GAP-063 design = User-level + per-NotificationType + Set<Channel> = much richer. **No conflict** — new entity table; V18 columns stay for legacy instance-level fallback. Agent must mention V18 trong rules.md "Existing state" section. |
| FE files | `kitehub-frontend/src/app/settings/notifications/page.tsx` NEW (preference CRUD UI) |
| Tests | `NotificationChannelTest` (interface contract), `NotificationPreferenceServiceTest` (CRUD), `EmailServiceClientIT` (existing callers still work) |
| Business docs | `documents/01-business/kitehub/notification/{rules.md, use-cases.md, api-contract.md}` NEW (full 3-layer) |
| Phase 1 ACs from GAP-063 | ✅ Notification abstraction interface, ✅ Email adapter (existing migrate), ⏸ SMS (defer GAP-063b), ⏸ Zalo (defer), ✅ User preference UI (Phase 1 scope), ⏸ Quiet hours (defer), ⏸ Cost tracking (defer), ⏸ Fallback chain (defer) |
| 4-layer | 要件: P2/P3 review evidence (8 P2 FAILs) / 基本: settings page mock / 詳細: interface contract + Outbox publish per Exception D / コンポ: NotificationPreferenceForm |
| Sister gap filed | **GAP-063b** — Phase 2: Zalo + SMS + quiet hours + fallback + cost tracking |

### Bucket C — GAP-057 Phase 1 Payroll (HOURLY only)

| Item | Value |
|------|-------|
| Branch | `wave/18a-bucket-c-payroll-hourly` |
| Domain | `kiteclass-core/module/payroll` (greenfield) |
| Files (write) | `entity/{PayrollConfig.java, PayrollPeriod.java}` NEW, `enums/{PayrollType.java, PayrollStatus.java}` NEW, `service/PayrollService.java` NEW (HOURLY calc only), `controller/PayrollController.java` NEW (read-only endpoints), `repository/` NEW, V48 migration |
| **State-check 2026-05-04** | Grep `Payroll\|Commission\|TeacherSalary` trong toàn repo = 0 hits. Greenfield xác nhận. |
| FE files | `kiteclass-frontend/src/app/(authenticated)/admin/payroll/page.tsx` NEW (read-only list) |
| Tests | `PayrollServiceTest` (HOURLY calc + edge cases), `PayrollControllerIT` (admin views list) |
| Business docs | `documents/01-business/kiteclass/payroll/{rules.md, use-cases.md, api-contract.md}` NEW (full 3-layer) |
| Phase 1 ACs from GAP-057 | ✅ PayrollConfig + PayrollPeriod entities, ⚠️ Calc engine (HOURLY only — defer 3 other types), ⏸ Monthly run UI (defer), ⏸ Payslip PDF (defer), ⏸ VN tax/BHXH (defer), ⏸ Bank export (defer), ✅ Audit log (Phase 1 scope — basic) |
| 4-layer | 要件: P3 9.6/100 commission FAIL evidence / 基本: admin list page mock / 詳細: calc engine pseudo-code (HOURLY) / コンポ: PayrollListTable |
| Sister gap filed | **GAP-057b** — Phase 2: 3 remaining payroll types + VN tax + BHXH + PDF + bank export + run/approve UI |

---

## §4 Agent Prompt Template

Use `feature-tdd-agent` template per `feedback_parallel_agent_strategy.md`. Each agent receives this prompt structure (placeholders filled per bucket):

```
You are an isolated worktree agent for Wave 18a Bucket {A|B|C}.

CONTEXT
- Working directory: {worktree path — RELATIVE only, NEVER absolute}
- Branch: wave/18a-bucket-{a|b|c}-{slug}
- Wave plan: documents/03-planning/waves/wave-2026-05-04-18a-keystones.md
- Your gap: {GAP-290 | GAP-063 Phase 1 | GAP-057 Phase 1}
- Disjoint files: {list per §3 above}

MANDATES
1. RELATIVE paths only (per memory feedback_worktree_absolute_path_contamination.md)
2. TDD: write failing test FIRST, then production code (per CLAUDE.md Superpowers)
3. Commit after each file (per memory feedback_agent_kill_root_cause.md SIGHUP risk)
4. Business docs updated in SAME PR as code (per CLAUDE.md Living Docs)
5. JSONB pairs with @JdbcTypeCode(SqlTypes.JSON) per memory
6. Outbox pattern for notification send (Bucket B only) per design-patterns.md §3.5.1
7. ObjectMapper findAndRegisterModules() in tests if serialize LocalDateTime

DELIVERABLES
- Code + tests + business docs (3-layer)
- 4-layer V-model matrix in PR description
- PR title: "feat(18a-{bucket}): {gap-id} Phase 1 — {summary}"
- PR body: AC checklist with checked items + deferred items linked to sister gap
- Status: GAP-{XXX} → 🟡 PARTIAL (Bucket B+C) or 🟢 DONE (Bucket A)

SELF-VERIFICATION before opening PR
- mvn test (full module) green
- pnpm test (FE module) green
- Business docs grep references match code
- No TODO comments without follow-up gap link
```

---

## §5 4-Layer V-Model Coverage (per `design-layer-coverage.md` §2.3)

| Layer | Bucket A (GAP-290) | Bucket B (GAP-063) | Bucket C (GAP-057) |
|-------|-------------------|-------------------|-------------------|
| **要件定義** | AC-OPS-002 + cross-persona evidence | P2/P3 review FAILs + child protection PDPL | P3 9.6/100 commission keystone |
| **基本設計** | Form mockup + state diagram | Settings page mock + flow | Admin list page mock |
| **詳細設計** | RecurrenceService + edit state machine | NotificationChannel interface + Outbox flow | PayrollService HOURLY calc pseudo-code |
| **コンポ設計** | RecurrenceForm component | NotificationPreferenceForm | PayrollListTable |

All 3 buckets have all 4 layers covered explicitly in their bucket scope above.

---

## §6 Foundation PR Scope (this PR)

This PR ships:
1. ✅ This wave plan document (`documents/03-planning/waves/wave-2026-05-04-18a-keystones.md`)
2. ✅ ROADMAP §🚀 Next Action update — pin Wave 18a as active wave
3. ✅ V-number reservation comment in Flyway directories (Bucket A=V60 kiteclass, B=V61 kitehub, C=V62 kiteclass)
4. ✅ Skeleton folders (empty placeholder for `kitehub-subscription/module/notification/`, `kiteclass-core/module/payroll/`) — agents fill them
5. ✅ Business docs domain README placeholders (`01-business/kitehub/notification/README.md`, `01-business/kiteclass/payroll/README.md`)

Out of scope for foundation PR (agents do):
- ❌ Any code changes inside skeletons (TDD mandate — test first)
- ❌ Migration SQL content (agents own)
- ❌ Business doc content (agents own)

---

## §7 Closure PR Scope

After 3 agent PRs merged, closure PR ships:
1. GAP-290 → 🟢 DONE (full ship)
2. GAP-063 → 🟡 PARTIAL (Phase 1 only; Log entry references GAP-063b)
3. GAP-057 → 🟡 PARTIAL (Phase 1 only; Log entry references GAP-057b)
4. **GAP-063b NEW** filed (Phase 2 scope deferred)
5. **GAP-057b NEW** filed (Phase 2 scope deferred)
6. ROADMAP §🎯 Current Status Snapshot updated (counts: ~145 OPEN → ~144 OPEN; -1 GAP-290 closed, +2 sister gaps filed, 2 PARTIAL stay counted)
7. `documents/00-brd/personas-catalog.md` — note Wave 18a impact on personas (estimate score lift; actual measurement Wave 19 review round 2)
8. `documents/03-planning/waves/wave-2026-05-04-18a-keystones.md` — `status: complete` + `actual_outputs` filled
9. Memory entry if surprises (e.g., long-agent stack issues, ical4j gotchas) — only if non-obvious

---

## §8 Acceptance Criteria for this Wave

- [ ] Foundation PR merged (this PR)
- [ ] 3 agent PRs CI green and merged
- [ ] GAP-290 → 🟢 DONE (per `gap-done-discipline.md` §2 — all 8 ACs checked)
- [ ] GAP-063 → 🟡 PARTIAL with sister gap GAP-063b filed
- [ ] GAP-057 → 🟡 PARTIAL with sister gap GAP-057b filed
- [ ] Business docs (rules + use-cases + api-contract) shipped same PR per CLAUDE.md Living Docs
- [ ] 4-layer V-model coverage matrix in each PR description (per `design-layer-coverage.md`)
- [ ] Closure PR merged with ROADMAP + personas-catalog + memory updates
- [ ] Wall-clock total ≤ 4h (target ~3h; cap ~4h before flagging stuck)

---

## §9 Wave 18b Preview (NOT this wave)

After 18a SHIPPED, next wave candidate is **Wave 18b — K-12 Stage 1 LEGAL trio**:
- GAP-321 (Parent portal — Luật GD Đ.83)
- GAP-322 (Child protection — Luật Trẻ Em Đ.51, criminal liability)
- GAP-323 (Period attendance K-12 model)

Multi-week scope (3-5 days/gap). Plan separately when 18a closes. K-12 Stage 1-5 program details: `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` §Stage 1-5.

---

## §10 Out-of-scope for Wave 18a

- GAP-223 (AI Branding governance) — continues deferred (not Wave 17 keystone)
- GAP-322/323 (K-12 LEGAL) — Wave 18b
- GAP-063 Phase 2 (Zalo/SMS/quiet hours/fallback) — GAP-063b after 18a
- GAP-057 Phase 2 (4 types/tax/BHXH/PDF/bank) — GAP-057b after 18a
- Track 2 UI kits production port — gated on MVP-essential blockers (this wave provides 1 keystone unblock)

---

## §11 Log

- **2026-05-04 (v0.2)** — State-check fixes applied: (a) migration V-numbers corrected V60-62 → V47/V48 (kiteclass-core latest=V46) + V61→V23 (kitehub-subscription latest=V22); (b) Bucket A note added ClassScheduleSlot (GAP-099 k12-only) precedent — agent decides reuse vs fork; (c) Bucket B note added V18 instance-level prefs (GAP-098) — no conflict, agent must reference in rules.md. Per `audit-to-gap-pipeline.md` Step 2.5 — state-check before plan ship.
- **2026-05-04 (v0.1)** — Plan created. Per `feedback_wave_plan_through_pr.md` MUST go through PR (not direct push). Per `incident-to-rule-pipeline.md` self-test: this plan applies `cluster-pattern.md` oversized rule (declined Option C multi-day; accepted Option A Phase-1 wave-pack). User chose Option A 2026-05-04 after state-check showed all 3 gaps oversized for single 75-min wave.
