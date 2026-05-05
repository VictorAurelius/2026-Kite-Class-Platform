# Gaps Roadmap — Epic-Based Organization

**Mục tiêu:** Biến 103 gaps thành actionable roadmap với epics + dependencies + sprints.

> **Khi nào đọc file này thay vì README.md?**
> - README: flat index, tra cứu 1 gap
> - ROADMAP: execution planning, sprint planning, dependency check

---

## 🎯 Current Status Snapshot (2026-05-04)

### 🚀 Next Action (signpost for new session)

**Recommended next: Meta-P0 first per `meta-gap-priority.md` — close GAP-356 (rule extension `audit-to-gap-pipeline.md` v1.2.0 + detector + self-test + memory; filed 2026-05-05 as 5th-recurrence escalation), THEN Wave 19 K-12 LEGAL Phase 1C wave-pack (4 buckets: GAP-322c child-protection mandatory reporting + hash-chain audit, GAP-323c GradeFormulaService TT 22/2021 + gradebook, GAP-321c PDPL consent + write actions, GAP-321b-1-conduct facet wiring).** Wave 18b3 SHIPPED 2026-05-04 (K-12 LEGAL trio Phase 1B remainder wave-pack — 5 PRs #779 plan + #780 Bucket A offline queue + k6 + #782 Bucket B LLTP UI + concrete MinIO SDK + #781 Bucket C 3 facet wiring PARTIAL with 3 sub-gaps filed + closure PR this). 12 consecutive 0-clarification agents same-day (Wave 18a + 18b1 + 18b2 + 18b3). All 3 K-12 LEGAL trio gaps stay 🟡 PARTIAL with explicit Phase 1C scope pickable next.

| Gap | Phase 1A status | Phase 1B status | Phase 1C |
|---|---|---|---|
| GAP-321 Parent Portal | 🟡 PARTIAL — transcript route + scope guard PDPL | GAP-321b 🟡 PARTIAL — 4 read-only facets + audit log skeleton + V53 (Wave 18b2 Bucket C); Zalo OTP / multi-children polish / write actions / concrete data sources for fees+conduct+notifications follow-up | GAP-321c (PDPL granular consent + 4 write actions + i18n EN/zh-CN) |
| GAP-322 Child Protection | 🟡 PARTIAL — Incident + AES-256 + safeguarding role | GAP-322b 🟡 PARTIAL — Vetting service + state machine + AES-256 + MinIO storage stub + RBAC + V52 (Wave 18b2 Bucket B); LLTP upload UI + verify queue UI + concrete MinIO SDK + 111 webhook follow-up | GAP-322c (Đ.51 banner + hash-chain audit + 7y retention + pen test) |
| GAP-323 Period Attendance | 🟡 PARTIAL — AttendancePeriod + tenant.vertical_type | GAP-323b 🟡 PARTIAL — Phase 1B v1 backend (#769) + mobile UI v1 tap-grid + bulk actions (#771 Bucket A); offline queue / Playwright perf / matview / concurrent load test / parent-portal facet exposure follow-up | GAP-323c (GradeFormulaService TT 22/2021 + state machine + gradebook UI) |

**Wave 18b3 SHIPPED 2026-05-04** — all 3 Phase 1B remainder buckets merged: ~~GAP-347 (meta)~~ ✅ #775. ~~GAP-323b offline + k6~~ shipped #780 (status 🟡 PARTIAL — PWA background-sync, conflict UI, queue LRU follow-up). ~~GAP-322b LLTP + MinIO SDK~~ shipped #782 (status 🟡 PARTIAL — resumable multipart, virus scan, audit on upload to Phase 1C). ~~GAP-321b 3 facet wiring~~ shipped #781 PARTIAL (fees real-wired; conduct + notifications stay v1 stubs, 3 sub-gaps filed: GAP-321b.1-fees-instalment-payment-history P2 + GAP-321b.1-conduct-incident-visibility P1 + GAP-321b.1-notifications-engine-wiring P1 hard-blocked by GAP-063b). Phase 1C = ~2-3 weeks. Stage 1 K-12 GA estimate ~12-16 weeks remaining (was 14-18; Wave 18b3 burned down ~1-2 weeks).

5-stage K-12 program (Q3 2026 → Q3 2027 GA) in [P5 review §Stage 1-5](../../00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md).

**Track 2 (UI kits production port)** — Phase 1 ADR + workspace scaffolding DONE (PR #713 merged 2026-04-30). Phase 2-6 (15 OPEN gaps GAP-266..280) — multi-week roadmap detailed in [`documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md`](../../03-planning/waves/wave-track-2-ui-kits-port-umbrella.md). Trigger Phase 2 (5 priority components G2/G6/G5/G7/D1) khi MVP-essential blockers từ Wave 17 review findings cần real components.

**Dependabot pre-MVP lock** — closed 4 failing PRs (#715/#716/#717/#718), restricted weekly bumps to patch-only via PR #731 (merged 2026-04-30). Resume condition: post-MVP launch (~4-6 weeks) per GAP-283.

**Production deploy estimate** — MVP soft launch ~4-6 weeks; GA ~8-12 weeks; full Track 2 production-grade UI ~10-14 weeks. See most recent persona AC ROADMAP analyses below for breakdown.

---

**2026-05-05 (GAP-362 filed — TenantIsolationIT flake orphan):** P1 test-isolation correctness gap. `TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` (line 148) sporadically fails on `mvn verify` since Wave 14; flagged inline in GAP-347 closure (PR #775) + Wave 19 Bucket A closure (PR #793) as "out of scope, pre-existing" but no dedicated gap until now → orphaned debt. Filed per `audit-to-gap-pipeline.md` Step 1-3 + `gap-done-discipline.md` §2 anti-pattern. Numbered 362 to avoid collision with reserved 359/360/361 (Bucket A/B/C in flight). Wave-eligibility: post-Wave-19 P1.

---

**2026-05-05 (GAP-358 filed — dev workstation server migration P2):** Migrate WSL2 → Oracle Cloud Always Free ARM A1.Flex (1× 4 OCPU + 24 GB RAM) for stable remote dev workstation per `feedback_agent_kill_root_cause.md` Tailscale + mosh + tmux 3-layer stack. Triggered by 3-agent kill incident 2026-05-05 (PC restart + 50 uncommitted files orphaned in worktrees). Existing `infrastructure/terraform-oracle/` is production-targeted (2-VM split); new gap proposes separate `terraform-oracle-dev/` module + Phase 2 VSCode Remote-SSH + code-server browser fallback for mobile. Wave-eligibility: post-Wave-19 P2.

---

**2026-05-05 (GAP-357 filed — deprecated exception-ctor migration sweep):** P3 tech-debt umbrella covering 43 source files using deprecated `ValidationException(String)` / `EntityNotFoundException(String, Long)` ctors that have new error-code-aware replacements. State-check expanded scope vs IDE-flagged subset (LSP only diagnoses opened files). Filed instead of fixed during Wave 19 wait window — heavy overlap with active Bucket A childprotection module (23 call sites). Migration deferred to post-Wave-19 wave-pack (Phase 1 = ~17 module PRs, parallel-eligible).

---

**2026-05-05 (GAP-356 SHIPPED — Meta-P0 5th-recurrence escalation, audit-to-gap-pipeline.md v1.2.0):** GAP-356 filed (PR #787) + closed (PR 2 stacked) — `audit-to-gap-pipeline.md` extended to v1.2.0 with §2.6 Wave-Plan Pre-Flight State-Check Protocol + `_TEMPLATE.md` State-Check Evidence section + `session-docs-check` Rule 16 detector + 3-fixture self-test (good-symbol-with-evidence ✅, bad-symbol-no-evidence ✅ FAIL on Wave 18b3 incident symbols, forward-flagged-allowed ✅) + `feedback_wave_plan_state_check.md` memory + cross-link in `feedback_wave_plan_through_pr.md`. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect (5th recurrence Wave 18b3) → Classify (existing Step 2.5 covers gap-filing only) → Rule+Enforce (rule + detector + template same PR per §6.5) → Self-Test (Wave 18b3 plan symbols `Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification.audienceScope` flagged by detector) → Retro Log (this entry). Wave 19 K-12 LEGAL Phase 1C plan now uses new template + detector. If 6th recurrence detected → meta-rule audit (Rule 16 + §2.6 both failing).

---

**2026-05-04 (Wave 18b3 K-12 LEGAL Trio Phase 1B Remainder SHIPPED — 5 PRs merged same-day, 12-agent 0-clarification streak):** Continued K-12 LEGAL trio momentum into Phase 1B remainder via 4th consecutive same-day wave-pack. 3 disjoint buckets ran simultaneously: Bucket A (GAP-323b IndexedDB offline queue + k6 perf), Bucket B (GAP-322b LLTP upload UI + concrete AWS SDK v2 MinIOStorageImpl), Bucket C (GAP-321b 3 facet data wiring — fees real, conduct + notifications stay v1 stubs after state-check). 0-clarification all 3 agents (12 consecutive across Wave 18a + 18b1 + 18b2 + 18b3 same day).

**Merged sequence (5 PRs):**
1. **#779** — Wave 18b3 plan (foundation, docs-only)
2. **#780** — Bucket A: GAP-323b offline queue + k6 → 🟡 PARTIAL. IndexedDB queue (idb v8) + `useOfflineAttendanceQueue` hook + `OfflineSyncStatusBadge` wired into `(teacher)/attendance/period/.../page.tsx`; k6 script asserting `p(95)<2000`. 14 new tests (612/612 FE green, +14, 0 regressions); `pnpm build` strict-mode green. Coordinator inline fix: `// @ts-nocheck` on k6 script (Next.js typecheck included `tests/perf/`; k6 has its own runtime).
3. **#782** — Bucket B: GAP-322b LLTP UI + concrete MinIO SDK → 🟡 PARTIAL. Real `S3Client.putObject` AWS SDK v2 impl replaces 18b2 stub; `POST /api/v1/vettings/{vettingId}/documents` multipart endpoint (10MB cap, PDF + image/* MIME); FE form at `(dashboard)/admin/vetting/[vettingId]/upload/page.tsx`; LocalStack/MinIO testcontainer round-trip IT. 28 BE + 5 FE tests; jacoco on new code: `MinIOVettingDocumentStorageImpl` 93%, `VettingController` 79%, `VettingDocumentResponse` 79%. V54 NOT used (file metadata in response, separate table to Phase 1C).
4. **#781** — Bucket C: GAP-321b 3 facet wiring → 🟡 PARTIAL. Fees facet real-wired (date-range JPQL + `@EntityGraph` + `assertSelectCount ≤3` + N+1 IT). Conduct + notifications stay v1 stubs after agent state-check found `Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification` entity all 0 matches in codebase. **3 sub-gaps filed**: GAP-321b.1-fees-instalment-payment-history (P2 v2 enrichment), GAP-321b.1-conduct-incident-visibility (P1), GAP-321b.1-notifications-engine-wiring (P1 hard-blocked by GAP-063b). 12 test additions; 96/96 parent + invoice tests green.
5. **Closure PR (this)** — wave plan flip + ROADMAP §🚀 Next Action update + 3 gap files Wave 18b3 Log entries + wave-history.jsonl Rule 15 append.

**Wave 18b3 outcomes:**
- 3 K-12 LEGAL Phase 1B gaps stay 🟡 PARTIAL (Phase 1C scope remains for all 3)
- 3 sub-gaps filed (GAP-321b.1-* trio) — explicit Phase 1B remainder follow-up scope per gap
- 12-agent same-day 0-clarification streak (record holds)
- Estimated K-12 Stage 1 remaining: ~12-16 weeks (was 14-18; Wave 18b3 burned down ~1-2 weeks)
- **5th GAP-190/197 head-truncation recurrence detected** — wave plan §3 Bucket C referenced absent schema. Per `audit-to-gap-pipeline.md` Step 2.5 4th-recurrence escalation policy, 5th hit = file gap on the rule itself. Recommended scope: extend Step 2.5 protocol from pre-gap state-check to pre-plan state-check — wave plans must verify all referenced entities/rules/fields exist before agents read the plan as ground truth.
- Two coordinator-side incidents recovered cleanly: (i) PR #780 first CI run failed Next.js typecheck on k6 script — fixed inline; (ii) Bucket B agent's worktree absolute-path leak contaminated local main twice — recovered via `git reset --hard origin/main`; origin not affected (verified `git ls-remote`).

**Counts:** 155 → **157 OPEN** (-1 GAP-347 closed PR #778 same-day-earlier; +3 sub-gaps GAP-321b.1-* filed by Bucket C; net +2 from Wave 18b2 closure tally).

---

**2026-05-04 (UI kits roadmap sync — doc-only, GAP-348 + GAP-349 filed):** Session audit found 2 missing scope artifacts on UI kits + Track 2 axis: (1) Round 3 kits (`kiteclass-student` PR #700, `kitehub-admin` PR #703 merged 2026-04-29) shipped with **agent self-report** scores (116 ⭐⭐ / 107.2) but **no external review** through `quality/ui-review/SKILL.md` — per `feedback_audit_calibration.md` self-audit overstates 15-20 pts; trusting these scores while planning Track 2 Phase 4 production port (GAP-269 student + GAP-271 admin) ports unvetted designs into production code. (2) Track 2 umbrella plan (`wave-track-2-ui-kits-port-umbrella.md`) lists Phase 2 as "5 priority components × 3-5 days" but has no concrete wave-pack breakdown — risks serial-PR anti-pattern (GAP-229 incident: 90 min serial vs 30 min parallel). 2 gaps filed: **GAP-348** (Round 3 persona-driven review, P1, 2-3 days, parallelizable A+B) + **GAP-349** (Track 2 Phase 2 wave-pack plan, P1, 5-bucket wave-pack ~3 hr execution). README `ui_kits/README.md` Round 3 status synced (🟡 ACTIVE → ✅ DONE with self-report caveat + GAP-348 cross-link).

**Counts:** 157 → **159 OPEN** (+GAP-348, +GAP-349).

---

**2026-05-04 (UI kits Round 3 storytelling gap — GAP-350 filed):** Session audit found `kitehub-story-v2/` listed in `ui_kits/README.md` Status as 🔵 future but no gap tracked. Round 1 baseline (`kitehub-story` 546 LOC JSX) preserved in `07-archived/design-round-1-2026-04-29/`; Direction A scope decision documented in `dossier/08-direction-decisions.md` Decision 3 (marketing-only polish, LOWER priority). Without a tracked gap, Track 2 GAP-275 (KH public marketing port) had ambiguous source. **GAP-350** filed P2 (Marketing/Feature-tier per `meta-gap-priority.md` — not blocker, pickable when MVP-critical waves quiet); paired with GAP-274 (KC public marketing) as candidate 2-bucket marketing wave-pack. README Status row 🔵 future → 🔵 OPEN with cross-link.

**Counts:** 159 → **160 OPEN** (+GAP-350).

---

**2026-05-05 (Simulation gap finder — 3-axis matrix → 5 new gaps GAP-351..355):** Applied `quality/simulation-gap-finder.md` 3-axis matrix sampling (5 personas × 8 stages × 10 categories) to UI Kits + Track 2 production port scope. Diagonal sweep + state-check each candidate (`audit-to-gap-pipeline.md` Step 2 + 2.5) before file. 5 real gaps found, 0 duplicates, 4 borderline folded into existing.

**Filed:**
- **GAP-351** (P1, Meta) — `@kite/shared-ui` semver + breaking-change policy (Developer × Evolution × C10). 0 hits "semver" in `packages/shared-ui`. Gates Phase 2 component churn.
- **GAP-352** (P1, Compliance) — WCAG AA third-party audit (axe-core / lighthouse-ci / screen-reader) before Track 2 production port (Platform Admin × Provisioning × C6). 0 hits "axe-core"/"lighthouse-ci". GAP-348 covers visual /128 + persona, NOT formal WCAG.
- **GAP-353** (**P0**, LEGAL) — PDPL 2023 cookie/consent banner in KH+KC marketing kits (Platform Admin × Discovery × C6). 0 hits "PDPL"/"cookie banner" in GAP-274/275/350. PDPL effective 2026-07-01 ~8 weeks; MVP launch ~4-6 weeks precedes effective date.
- **GAP-354** (P2, Performance) — Per-kit bundle size budget for 7 kit ports (End User × Daily × C4). 0 hits "bundle.budget"/"kit.*gzip" in GAP-26X/27X/349. GAP-349 has per-component, not per-kit.
- **GAP-355** (P2, Operations) — Visual regression drift policy (prototype↔production sync over time) (Developer × Evolution × C10). GAP-273/349 capture *initial baseline* only; 0 drift-policy mentions. Paired with GAP-351 as governance wave-pack candidate.

**Folded into existing (not filed):** cross-kit empty-state consistency → GAP-277; component deprecation playbook → GAP-351; i18n EN/zh-CN marketing → GAP-321c (parent portal already tracking); Storybook formal infra → GAP-349 foundation bucket scope.

**Counts:** 160 → **165 OPEN** (+GAP-351..355).

---

**2026-05-04 (Incident → rule pipeline applied — wave-history.jsonl append rule):** User flagged 3 consecutive waves (18a, 18b1, 18b2) missing `wave-history.jsonl` appends despite `wave-pack-planner` SKILL.md §Rules requirement. Per `incident-to-rule-pipeline.md` 5-stage: Stage 1 Detect ✓. Stage 2 Classify: rule existed but no enforcement — pure gentleman's agreement. Stage 3+4 ship in this PR — `session-docs-check` Rule 15 detector + 3 self-test fixtures (good-flip-with-append PASS / bad-flip-no-append FAIL / bad-flip-bad-json FAIL) all green via `test/run-rules.sh`. Stage 5 retro logged here. Sister PR `meta/wave-history-backfill-18a-18b1-18b2` ships the actual missing entries. Detector now blocks future closures from skipping the append (WARN default, FAIL in `--strict`); override trailer `WAVE_HISTORY_OVERRIDE: <reason>` available for rare doc-only corrections.

**Counts:** unchanged (no new gaps; this is a meta-process fix).

---

**2026-05-04 (Wave 18b2 K-12 LEGAL Trio Phase 1B Foundation SHIPPED — 4 PRs merged same-day):** Continued K-12 LEGAL trio momentum into Phase 1B execution via parallel-agent wave-pack (Wave 18b1 precedent). 3 disjoint buckets ran simultaneously: Bucket A (FE mobile UI for GAP-323b), Bucket B (vetting service foundation for GAP-322b), Bucket C (4 parent portal read-only facets for GAP-321b). 0-clarification across all 3 agents (9 consecutive across Wave 18a + 18b1 + 18b2 same day).

**Merged sequence (5 PRs):**
1. **#770** — Wave 18b2 plan (foundation, docs-only)
2. **#771** — Bucket A: GAP-323b Phase 1B v1 mobile UI → stays 🟡 PARTIAL. Tap-grid (42×4 buttons) + bulk actions (mark-all-present + reset + save) + route shell `/teacher/attendance/period/[classId]/[periodNo]/[date]` + `attendancePeriodApi` client + TanStack hooks. 19 new FE tests; 598/598 frontend suite green; `pnpm build` green.
3. **#772** — Bucket B: GAP-322b foundation → 🟡 PARTIAL. Vetting entity + 6-state state-machine guard + AES-256 reuse + MinIO storage stub interface + RBAC gate + V52 + `BR-VETTING-001..005` with 5-attribute frontmatter. 48 vetting tests + 72 cumulative module tests green.
4. **#773** — Bucket C: GAP-321b foundation → 🟡 PARTIAL. 4 read-only facet controllers (attendance/fees/conduct/notifications) + per-read audit log skeleton (REQUIRES_NEW txn + best-effort error swallow) + V53 + 5 BR + 4 UC. 1230/1230 mvn green. **Sonar 78.2% gate fail** at first run — 24 follow-up unit tests pushed to reach gate; root cause traced to JaCoCo surefire-only artifact (failsafe `.exec` not merged); admin-merged with **GAP-347 meta-fix filed** for `pom.xml` jacoco surefire+failsafe merge.
5. **Closure PR (this)** — wave plan flip draft → complete + ROADMAP §🚀 Next Action update + GAP-347 filed. Memory `feedback_webmvctest_mock_reset.md` saved (Mockito mock-state leak across `@WebMvcTest` methods, surfaced by Bucket B's mixed `verify(...)` + `verify(never())` pattern).

**Wave 18b2 outcomes:**
- 3 K-12 LEGAL Phase 1B gaps ALL flipped 🔵 OPEN/🟡 PARTIAL → 🟡 PARTIAL (foundation shipped per gap)
- 1 meta-gap filed (GAP-347 — JaCoCo merge config for Sonar)
- Estimated K-12 Stage 1 remaining: ~14-18 weeks (was 18-24; Wave 18b1+18b2 burned down 4-6 weeks)
- 3 parallel agents 0-clarification (9 consecutive same-day streak)
- Notable findings preserved: student-name placeholder (Agent A) needs hydration when GAP-321b ships student-listing endpoint; 3/4 facets stub-empty (Agent C) tracked under GAP-321b.1; ParentReadAuditLogService uses REQUIRES_NEW + best-effort swallow design (Agent C — flag for review).

**Counts:** 154 → **155 OPEN** (+1 GAP-347 filed; 0 closed — all 3 wave gaps stay 🟡 PARTIAL with explicit follow-up).

---

**2026-05-04 (Wave 18b2 first PR — GAP-323b Phase 1B v1 backend foundation):** Continued K-12 LEGAL trio momentum into Phase 1B execution. Single-agent serial PR (Step 0 wave-eligibility checked: GAP-323b sub-tasks 1B.1..1B.6 are not disjoint enough for parallel agents — UI depends on API, offline depends on UI, etc.). Scope landed: idempotent batch upsert (`POST /api/v1/attendance/periods` with V50 unique-tuple lookup) + optimistic-lock PATCH (`@Version`) + on-demand daily roll-up endpoint (matview deferred per BR-PERIOD-ATT-010 §note) + V51 `period_no BETWEEN 1 AND 10` CHECK + new `OPTIMISTIC_LOCK_CONFLICT` error code on `GlobalExceptionHandler`. 4 new business rules (BR-PERIOD-ATT-008..011), 3 new use cases (UC-PERIOD-ATT-W-001/W-002/R-005). 19 tests green (9 unit + 10 IT TestContainers Postgres). Status flip: GAP-323b OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — mobile UI / offline queue / matview variant / 30-GVCN concurrent load test / parent-portal /attendance facet / fine-grained RBAC explicitly deferred to follow-up PRs (not silently dropped).

**Counts:** 154 OPEN (no change — GAP-323b stays open as PARTIAL per discipline rule).

---

**2026-05-04 (Wave 18b1 K-12 LEGAL Trio Phase 1A SHIPPED — same-day same-session as Wave 18a, 5 PRs merged):** Continued cross-persona keystones momentum into K-12 LEGAL trio. Phase 1A skeleton wave-pack pattern (Wave Legal-BRD precedent + Wave 18a) — each bucket ships structural foundation only; UI + workflow deferred to Phase 1B/1C via sister gaps.

**Merged sequence (5 PRs):**
1. **#763** — GAP-285 admin testGetRevenue time-bomb fix (relative dates) → 🟢 DONE. Unblocks every future PR's CI.
2. **#764** — Wave 18b1 plan (foundation, docs-only)
3. **#765** — Bucket F: GAP-323 Phase 1A → 🟡 PARTIAL. AttendancePeriod entity + V50 + V24 (instances.vertical_type CENTER/K12_SCHOOL discriminator) + 3-layer business docs. 4 unit + 5 IT green; TestContainers V1-V50 fresh DB verified.
4. **#766** — Bucket D: GAP-321 Phase 1A → stays 🟡 PARTIAL. ParentTranscriptController + Service with PDPL Art 16 scope-guard via ParentStudentLink + /parent/transcript/[childId] FE route. 30 BE + 6 FE tests green. **State-check addendum:** GAP-345 audit MISSED Wave 2 inline-fetch FE skeleton at `(dashboard)/parent/page.tsx`; agent replaced cleanly. **4th GAP-190/197 head-truncation recurrence** — closure PR extends `audit-to-gap-pipeline.md` Step 2.5 to ban head-truncation in state-check.
5. **#767** — Bucket E: GAP-322 Phase 1A → 🟡 PARTIAL. NEW `module/childprotection/` + Incident entity + AES-256-GCM AttributeConverter (33 tests, tamper detection via auth tag) + V49 + SAFEGUARDING_OFFICER system-template role. Encryption pattern matches existing kitehub-subscription EncryptionService.

**Wave 18b1 outcomes:**
- 3 K-12 LEGAL gaps ALL flipped to 🟡 PARTIAL with Phase 1A foundation
- **6 sister gaps filed** (this closure PR): GAP-321b/c, GAP-322b/c, GAP-323b/c — Phase 1B + 1C scope per gap explicit
- Estimated K-12 Stage 1 completion: ~18-24 weeks (3 gaps × 4-8 weeks each Phase 1B+1C)
- 0-clarification on all 3 agents (3 consecutive Wave 18b1; 6 consecutive across Wave 18a + 18b1 same day)
- 2 mid-flight CI fixes from Wave 18a precedent successfully avoided in 18b1

**Counts:** 148 → **154 OPEN** (-1 GAP-285 closed; +6 sister gaps filed; net +5).

---

**2026-05-04 (Wave 18a Cross-Persona Keystones Phase 1 SHIPPED — Phase-1 wave-pack methodology validated again, 6 PRs merged):** Per ROADMAP §🚀 Next Action recommendation, 3 disjoint buckets shipped via wave-pack pattern (Wave 13/14 Legal-BRD precedent ~5x speedup). Wall-clock ~3.5h total (foundation 30min + 3 parallel agents ~1.5h longest path + 2 mid-flight CI fixes + sequential merge + closure PR).

**Merged sequence (6 PRs):**
1. **#756** — Wave 18a plan (foundation, docs-only; ce47b7ef on main)
2. **#757** — GAP-345 K-12 LEGAL trio state-check audit + revise GAP-321/322/323 (3rd recurrence of GAP-190/197 anti-pattern caught proactively before Wave 18b plan; Phase 1 GAP-052a + GAP-054 + GAP-099 confirmed shipped earlier)
3. **#760** — Bucket A: GAP-290 Recurring class generator → 🟢 DONE. Pure java.time RRULE generator (chosen over ical4j to avoid transitive dep + CVE burden); Strategy pattern preserved. 1144/0/0 backend tests + 573/0 frontend tests. Acceptance: 8/8 ACs verified.
4. **#758** — Bucket C: GAP-057 P1 Payroll → 🟡 PARTIAL. HOURLY calc engine only (3 other types deferred to GAP-057b); 15 unit tests + HALF_EVEN banker's rounding scale=2 for VND codified.
5. **#759** — Bucket B: GAP-063 P1 Notification → 🟡 PARTIAL. NotificationChannel interface (Strategy) + SESEmailService implements + NotificationPreference entity + V23 + settings UI; 25/25 email + 366/366 subscription + 488/488 frontend tests. Mid-flight fix: GAP-240 lesson recurrence — coordinator added admin app @EnableJpaRepositories + @EntityScan for new notification packages (Bucket B agent missed this).
6. **#761** — GAP-346 test skip audit (filed during Wave 18a CI review): kiteclass-frontend 26.7% skip ratio (206/771 tests) vs kitehub-frontend 0% + Java 0% — proposes 5-phase remediation including CI warning mechanism (skip-budget script + mandatory `[SKIP: reason]` comment + PR diff comment).

**Wave 18a outcomes:**
- **GAP-290 → 🟢 DONE** (full ship, all 4 personas unblocked for recurring class scheduling)
- **GAP-063 → 🟡 PARTIAL** Phase 1 (notification abstraction + email migrated; Zalo/SMS/quiet-hours/fallback/cost → GAP-063b)
- **GAP-057 → 🟡 PARTIAL** Phase 1 (HOURLY entities + read-only UI; 3 types/tax/BHXH/PDF/bank/run-approve → GAP-057b)
- **3 sister/audit gaps filed:** GAP-345 (state-check audit), GAP-346 (skip audit), GAP-063b + GAP-057b (Phase 2 follow-ons)

**CI flakes encountered (none Wave 18a's fault):**
- PR #759 admin tests = GAP-285 pre-existing (`AdminControllerTest.testGetRevenue` — failing on every PR per ROADMAP entry)
- PR #758 SonarCloud = advisory `continue-on-error: true`, doesn't block
- PR #759 initial: pnpm/action-setup@v6 transient resolve failure (cleared on retry)

**Counts:** 145 OPEN → **148 OPEN** (-1 GAP-290 closed; 2 PARTIAL stay counted; +4 new gaps GAP-345/346 + GAP-063b/057b; net +3).

---

**2026-05-04 (Wave 17 Persona Review Round 1 SHIPPED — same-day end-to-end execution, 8 PRs merged):** Phase 1 (PR #739) plan + foundation. Phase 2 attempted parallel background agents → 3/4 killed silently mid-flight; **root cause identified: SSH SIGHUP cascade when mobile session disconnects** (NOT runtime/context limit). Memory `feedback_agent_kill_root_cause.md` saved. Phase 2 re-run with `commit-after-each-file` mandate → all 4 agents shipped clean. Wall-clock total ~5h (Phase 1 + recovery loop + Phase 2 re-run + parallel mobile-resilient stack + restructure).

**Merged sequence (8 PRs):**
1. **#745 P2 Small Center review** — score 36.8/100, 8 gaps (GAP-296..303). Top: notification + commission keystones.
2. **#747 P3 Medium Center review** — score **9.6/100** (0 PASS!), 15 gaps (GAP-306..320 — full reserved range). Top: commission/payroll, multi-class scheduling, RBAC audit.
3. **#748 P5 K-12 School review** — score **8.3/100** (largest scope — 134 ACs across 5 personas), 24 gaps (GAP-321..344). Top: LEGAL parent portal (Luật GD Đ.83) + child protection (Luật Trẻ Em Đ.51 criminal liability) + period attendance K-12 model.
4. **#749 P1 Solo Teacher review** — score 36.2/100, 10 gaps (GAP-286..295). Top: mobile OTP signup + Zalo notification + recurring class generator.
5. **#750 docs(05-guides): restructure 28 root files → 0** — 0 root .md files (only README), 8 new domain subfolders (local-dev, remote-access, deploy, monitoring, infrastructure, tenant-lifecycle, branding, contributing). 363 inbound refs updated repo-wide via sed. 27 git mv preserved history.
6. **#746 docs(ssh-guide): mosh layer + ntfy mobile push** — SSH guide §3.4 mobile-resilient stack (Tailscale + mosh + tmux); 3 runnable migration scripts (cleanup-windows.ps1, setup-wsl2.sh, android-checklist.md); ntfy.sh push as stop-hook channel #4 with last-assistant-message body parsing; Vietnamese translation.
7. **Closure PR (this)** — dedupe + ROADMAP sync + personas-catalog measured scores + GAP-152 → 🟢 DONE.

**Wave 17 outcomes:**
- **288 ACs scored** across 4 personas (Tier-1) + secondary docs
- **Coverage measured vs estimated:** ALL 4 LOWER than 2026-04-14 estimates → estimates were optimistic
- **57 NEW gaps filed** (vs ~25 expected) — deeper review surfaced more cases
- **Cross-persona keystones:** GAP-063 (Zalo/SMS, blocks all 4) + GAP-057 (commission, blocks 3) recommend bump P1 → P0
- **K-12 LEGAL surface:** parent portal + child protection criminal liability + MoET license verification — blocks K-12 GA until ~6-week Stage 1 lands

**Counts:** 88 OPEN → **145 OPEN** (-1 GAP-152 closed; +57 new gaps; +1 GAP-285 from earlier session). Tier-1 persona readiness measured: NONE ready for GA at current state.

---

**2026-05-04 (3 PRs merged — main CI red triage + SSH access guide + Wave 17 Phase 1 plan):** session focused on unblocking + setting up for next wave-pack execution.

1. **PR #737 — `fix(docker): pnpm workspace context for frontend images (GAP-284)`** — diagnosed main CI red post-merge of #735 (Track 2 umbrella + #713 ADR-024 Phase 1). Root cause: `@kite/shared-ui@workspace:*` workspace dep introduced by #713 but Dockerfile narrow context (`kiteclass/kiteclass-frontend`) couldn't resolve `pnpm-workspace.yaml` / `packages/shared-ui`. Fix: repo-root context for both frontend Dockerfiles + `outputFileTracingRoot` in `next.config.js` + workflow `matrix.include` (frontend only) + repo-root `.dockerignore`. Mirror fix applied to kitehub-frontend Dockerfile (incidental coverage). Verified: post-merge `push: main` Docker workflow run 25300563672 success. **GAP-284 → 🟢 DONE**.
2. **GAP-285 filed** — `AdminControllerTest.testGetRevenue` failing on every PR's CI; pre-existing, surfaced during #737 triage (not caused by Docker fix). Out of scope for #737 per `audit-to-gap-pipeline.md`. P2, dedicated PR for fix.
3. **PR #738 — `docs: GAP-284 closure + SSH terminal access guide (with Android setup)`** — flips GAP-284 → 🟢 DONE per `gap-done-discipline.md` §2 + ships [`documents/05-guides/remote-access/ssh-terminal-direct-access.md`](../../05-guides/remote-access/ssh-terminal-direct-access.md) (end-to-end tested 2026-05-04 on desktop + Android phone via Tailscale): WSL2 sshd hardening with **ssh.socket drop-in** (Ubuntu 24.04+ critical footgun), Windows portproxy + Task Scheduler persistence, Tailscale install via direct-download (winget --silent UAC failure), §4 full Android setup (Tailscale Always-on VPN, Termux key gen, Termius gotchas, battery optimization caveat), §10 7 lessons learned. Decision rule: SSH-direct for verification loops, Claude Code for state-aware decisions/artifacts.
4. **PR #739 — `docs(wave-17): Persona Review Round 1 plan + foundation (GAP-152 Phase 1)`** — Phase 1 of Wave 17 GAP-152. Wave plan with 4 buckets (P1/P2/P3/P5) + reserved GAP ranges + agent prompt template + 4-layer design coverage check + foundation `documents/00-brd/persona-reviews/README.md`. Phase 2 (4 background agents shipping reviews + closure PR) deferred to fresh `/clear` session per `/start-session` skill degradation rule.

**Memory entries saved this session:** `feedback_local_verification_discipline.md` — codifies 3 rule violations (project scripts vs `docker buildx` direct / `run_in_background:true` for long ops / Monitor over sleep+poll) caught by user during #737 work; prevents recurrence.

**Counts:** 88 OPEN → 88 OPEN (-1 GAP-284 closed; +1 GAP-285 filed; net 0). Tier-1 personas now READY for Wave 17 Phase 2 execution.

---

**2026-04-30 (Wave Secondary-Persona-AC SHIPPED — Cluster 16, 12th wave-pack, ~80 min wall-clock):** 5 PRs merged sequence #725 foundation (secondary/ subdir + README + parent README extension + ROADMAP, 349 LOC) → #726 Agent A student-in-P2 + student-in-P3 (31 ACs, 569 LOC) → #729 Agent B **student-in-P5 + parent-in-P5 USER CRITICAL** (52 ACs, 777 LOC, **84 legal citations** Luật Trẻ em + PDPL Art 16 + Luật Giáo dục Đ.83 + MOET, 14 LEGAL ACs + 3 LEGAL CRITICAL incl PH-as-perpetrator workflow + joint custody + parental consent granular) → #728 Agent C teacher-employee-in-P3 + teacher-employee-in-P5 (47 ACs, 635 LOC, **56 MOET citations** TT 22/2021 + TT 32/2020 + Bộ luật Lao động + Luật BHXH + Luật Viên chức, GVCN + Bộ môn dual-role split) → #727 Agent D admin-in-P3 + admin-in-P5 (37 ACs, 624 LOC, 11 legal citations, multi-role RBAC). **Total wave delta: 167 ACs across 8 NEW secondary persona AC docs + foundation, 2,605 LOC body**. Wall-clock ~80 min (vs Wave 15 30 min for 1-doc/agent — 2-doc-per-agent pattern scales linearly, agents avg ~9 min wall-clock for 2 docs). All 4 agents 0-clarification-round (**27th-30th consecutive**). **Pattern reuse milestone:** 3rd consecutive wave reusing `_TEMPLATE.md` + `docs-only-skeleton-agent.md` template variant — validates pattern at scale (Wave 14 BRD + Wave 15 tenant + Wave 16 secondary). **GAP-153 → 🟢 DONE** (all 9 ACs met). **Path B success:** GAP-152 P5 review now **UNBLOCKED** — Wave 17 ready với 12 AC docs (4 tenant + 8 secondary, 288 ACs total). Wave 13/14/15 lessons applied: prune worktrees BEFORE final merge (4/4 clean merges). ~25 candidate NEW gaps surfaced (joint custody UX, multi-tenant SSO, PH-as-perpetrator workflow, hardship payment, recording 1-to-1 calls) — filing deferred to GAP-152 review per `audit-to-gap-pipeline.md` Step 2.5. **GAP-281 + GAP-282 filed inline** as Phase 2/3 follow-ups (4 P1 cells + 8 P2 cells deferred). **Counts: 87 OPEN → 88 OPEN** (-1 GAP-153 closed; +2 GAP-281/282 filed; net +1).

**2026-04-30 (Wave Secondary-Persona-AC KICKED OFF — Cluster 16, 12th wave-pack):** Closes GAP-153 Phase 1 (Secondary Persona AC — Student/Parent/Teacher/Admin per tenant context). Per `meta-gap-priority.md` §3 Business-Logic-P0 tier — **unblocks GAP-152** (which is Blocked-by GAP-153 per gap §Dependencies). Path B chosen over Path A (faster but PARTIAL closure-loop) for governance compliance with `gap-done-discipline.md`. Foundation: `documents/00-brd/persona-criteria/secondary/` subdir + README + parent README extension + ROADMAP. 4 parallel agents ship 8 P0 secondary persona AC docs (4 agents × 2 docs each):
- Agent A: `student-in-P2.md` + `student-in-P3.md` (student journey at small + medium center scale)
- Agent B: `student-in-P5.md` + `parent-in-P5.md` (USER critical pair — K-12 student + parent legal mandate)
- Agent C: `teacher-employee-in-P3.md` + `teacher-employee-in-P5.md` (teacher commission tracking + GVCN workflow)
- Agent D: `admin-in-P3.md` + `admin-in-P5.md` (multi-role admin RBAC + văn phòng/giáo vụ workflow)

**Reuse `_TEMPLATE.md`** từ GAP-151 (Wave 15) — 6 categories template scales to secondary personas without modification (validated by `docs-only-skeleton-agent.md` template variant codified Wave 14). **Pattern reuse milestone:** 3rd consecutive wave using same template (Wave 14 BRD skeletons + Wave 15 tenant AC + Wave 16 secondary AC).

Wave plan: `documents/03-planning/waves/wave-2026-04-30-secondary-persona-ac.md`. Overlap analysis: 0 HARD, 1 SOFT (read-only `_TEMPLATE.md` + `personas-catalog.md` citations). Closure target: GAP-153 → 🟢 DONE + GAP-281/282 follow-ups filed (P1/P2 deferred cells). Counts: unchanged by kickoff.

**2026-04-30 (Wave Persona-AC-Template SHIPPED — Cluster 15, 11th wave-pack, ~30 min wall-clock):** 5 PRs merged sequence #719 foundation (template _TEMPLATE.md với 6 categories + persona-criteria/README.md + skill v1.1→v1.2 + 00-brd/README + ROADMAP, 573 LOC) → #720 Agent A GAP-151 P1 Solo Teacher AC (29 ACs, 356 LOC, mobile-first + Zalo PRIMARY) → #721 Agent B P2 Small Center (25 ACs, 337 LOC, 60% commission + Zalo OA) → #722 Agent C P3 Medium Center (31 ACs, 354 LOC, RBAC + BHXH/BHYT/TNCN + multi-class scheduling) → #723 Agent D P5 K-12 School USER PRIORITY (36 ACs, 456 LOC, **73 MOET citations**, 3 P0 LEGAL flagged ACs incl parent portal mandate). **Total wave delta: 1,503 LOC across 4 NEW + 2 modified files, 121 ACs combined** (target 60-120 hit upper bound). Wall-clock ~30 min — matches Wave 13/14 cadence despite session-resume hazard (foundation interrupted 2026-04-29 evening due to transient skill-Edit errors; resumed 2026-04-30 cleanly after 14-commit main rebase including Waves UI Kits Round 3 + UI Coverage Audit + ADR-024 — verified non-impacting on BRD scope). All 4 agents 0-clarification-round (**23rd-26th consecutive** since wave-pack methodology adoption). **First real test of `docs-only-skeleton-agent.md` template variant** (codified Wave 14 Agent D) — held without adjustment for AC-derivation work; proves template scales beyond pure structural skeleton. **GAP-151 → 🟢 DONE** (all 8 ACs met). Wave 13/14 lessons applied: prune worktrees BEFORE final merge (4/4 clean merges, 0 main-already-used glitch), coordinator cd verification held (0 contamination). 19 candidate NEW gaps surfaced across 4 personas — filing deferred to GAP-152 review per `audit-to-gap-pipeline.md` Step 2.5 state-check. **Counts: 88 OPEN → 87 OPEN** (-1 GAP-151 closed; +0 new since candidate gaps deferred). **Next wave-pack candidate: GAP-152 Round 1 persona review execution** (consumes this wave's 4 AC docs).

**2026-04-30 (Wave Persona-AC-Template KICKED OFF — Cluster 15, 11th wave-pack):** Closes GAP-151 (Persona-Specific Acceptance Criteria — Template + Per-Persona AC Docs) full-ship via wave-pack. Per `meta-gap-priority.md` §3 Business-Logic-P0 tier — sister cluster của Wave Business Correctness 2026-04-29 (closed GAP-049/050/150) + Wave Legal-BRD Phase 1+1.5 (closed 7/7 BRD legal skeletons). **Strategic value:** unblocks GAP-152 (Round 1 review execution) — next wave-pack candidate after this lands. Foundation: `_TEMPLATE.md` (6-category AC structure: onboarding/ops/fin/comm/edge/exit) + `persona-criteria/README.md` index + `persona-based-business-review.md` skill update v1.1→v1.2 (replaces ad-hoc "Key needs" walkthrough với load-from-AC-doc flow). 4 parallel agents ship 4 Tier-1 AC docs: P1 Solo Teacher / P2 Small Center / P3 Medium Center / P5 K-12 School (15-30 ACs each, total 60-120 ACs). **First real test of new `docs-only-skeleton-agent.md` template variant** (codified Wave 14 Agent D, this is 1st live use). Wave plan: `documents/03-planning/waves/wave-2026-04-30-persona-ac-template.md`. Overlap analysis: 0 HARD, 1 SOFT (read-only `personas-catalog.md` citation by all 4 agents). Closure target: GAP-151 → 🟢 DONE (all 8 ACs met — template + 4 docs + skill update + READMEs + ROADMAP). Counts: unchanged by kickoff. Session-resume note: foundation work paused 2026-04-29 evening (skill Edit transient errors), resumed 2026-04-30 cleanly after 14-commit main rebase (Waves UI Kits Round 3 + UI Coverage Audit + ADR-024 unrelated to BRD scope).

**2026-04-29 (Wave Legal-BRD Phase 1.5 SHIPPED — Cluster 14, 10th wave-pack, sister of Cluster 13, ~30 min wall-clock):** 5 PRs merged sequence #693 foundation → #694 Agent A GAP-183 Refund (11 sections + 4 tables incl 4×4 eligibility matrix + L1-L7 escalation ladder, 384 LOC) → #695 Agent B GAP-185 Billing/VAT (14 sections + 6 tables incl payment method matrix + late fee + tax calc examples + **14+ tax citations**, 457 LOC) → #696 Agent C GAP-186 Child Protection (11 sections + 5 matrices incl persona trigger + minor data handling + safeguarding incident + mandatory reporting + age verification ASCII flow + **44 legal citations** Luật Trẻ em 2016 + Decree 56/2017 + PDPL Art 16 + Penal Code 142-147, 475 LOC) → #697 Agent D META codify `docs-only-skeleton-agent.md` template variant (192 LOC NEW) + extend `retrospective-checklist.md` với 4+-agent local-state hazards section (3 hazards: worktree-held branches block --delete-branch / coordinator cd contamination / git reset NUKES dirty files, 109 LOC modify). Total wave delta: 1,316 LOC BRD + 301 LOC meta = **1,617 LOC across 4 NEW + 1 modified files**. Wall-clock: foundation ~12 min + 4 parallel agents ~5.9 min wall (longest C 5.9 / D 7.5 / B 5.1 / A 4.7) + sequential merge ~3 min + closure ~10 min = **~30 min total** (vs Wave 13 35 min). **Wave 13 lessons applied successfully:** prune worktrees BEFORE final merge prevented "main is already used by worktree" glitch (4/4 merges clean); 0 contamination incidents; 0 coordinator cd issues. All 4 agents 0-clarification-round (19th, 20th, 21st, 22nd consecutive). **MILESTONE: 7/7 BRD legal mandate skeletons DONE** (TOS/AUP/Privacy/Retention/Refund/Billing/Child-Protection) → Phase 1 of GAP-154 umbrella **COMPLETE**. **Meta deliverable:** `docs-only-skeleton-agent.md` template variant codified at 2nd recurrence threshold — avoids 3rd-time re-derivation cost; future skeleton waves use new template directly. **Counts: 88 OPEN → 88 OPEN** (-3 OPEN closed via flip; +3 PARTIAL stay counted; net 0). All 7 BRD legal gaps now 🟡 PARTIAL waiting on Phase 2 legal counsel content via GAP-154.

**2026-04-29 (Wave Legal-BRD Phase 1.5 KICKED OFF — Cluster 14, 10th wave-pack, sister of Cluster 13):** Same-day extension after Wave 13 Legal-BRD Phase 1 SHIPPED (4 docs ~35 min). Cluster 14 = 3 remaining OPEN P0 BL legal mandate gaps: GAP-183 (Refund/Dispute, VN Consumer Protection Law 2023), GAP-185 (Billing/VAT, TCT e-invoice Circular 78/2021/TT-BTC mandate), GAP-186 (Child Protection K-12, Law on Children 2016 + PDPL Art 16). **+ 1 meta-track agent** codifies recurring `docs-only-skeleton-agent.md` template variant (2nd recurrence threshold = early codify) + extends `retrospective-checklist.md` với 4+-agent local-state hazard pattern from Wave 13. Wave plan: `documents/03-planning/waves/wave-2026-04-29-legal-brd-phase1-5.md`. Overlap analysis: 0 HARD, 2 SOFT (read-only rule citations). Foundation PR ships `00-brd/README.md` updates centrally. **Milestone target:** 7/7 BRD legal mandate skeletons DONE (closes Phase 1 of GAP-154 umbrella; Phase 2 legal counsel content remains). Closure: 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 for 3 BRD gaps; meta deliverable shipped in closure ROADMAP entry. Counts: unchanged by kickoff.

**2026-04-29 (Wave Legal-BRD Phase 1 SHIPPED — Cluster 13, 9th wave-pack, ~30 min wall-clock):** 5 PRs merged sequence #687 foundation → #689 Agent A GAP-180 TOS (15 sections + Glossary + 8-field frontmatter, 401 LOC) → #688 Agent B GAP-181 AUP (11 sections + 5 tables incl prohibited-content/strike/appeal, 411 LOC) → #691 Agent C GAP-182 Privacy (22 sections + 4 tables + **61 PDPL article citations**, 319 LOC) → #690 Agent D GAP-184 Retention (9 sections + 9-row retention matrix + 14 informed-gut markers, 195 LOC). Total skeleton delta: 1,326 LOC across 4 NEW files in `documents/00-brd/`. Wall-clock: foundation ~15 min + 4 parallel agents ~5.7 min wall (Agent C longest 5.7 min, A 5.7 min, B 4.6 min, D 4.1 min) + sequential merge ~5 min + closure ~10 min = ~30-35 min total — **wave-pack methodology now ~5x speedup confirmed across 9 consecutive waves** (Obs 75 / DR-Backup 75 / Meta-Day-2 6 / Meta-Gov-2 50 / Meta Phase-2 30 / Business Correctness 75 / UI Kits R2 130 / Review Process Improvement 110 / Legal-BRD 30). All 4 agents 0-clarification-round (15th, 16th, 17th, 18th consecutive). **Worktree-contamination incident on Agent C** (Write tool initially landed file at main worktree path; caught immediately on first verification grep, recovered cleanly, no upstream contamination) — documented in GAP-182 Log per `feedback_worktree_absolute_path_contamination.md`. **Local main glitch** during PR #691 merge: `gh pr merge --squash --delete-branch` post-merge checkout failed with "fatal: 'main' is already used by worktree" because 4 agent worktrees still on detached HEADs of merged branches; coordinator recovered via `git fetch && git reset --hard origin/main`. Lesson for next wave: prune worktrees BEFORE final merge OR accept local stale state until cleanup. **All 4 gaps flipped 🔵 OPEN → 🟡 PARTIAL** per `gap-done-discipline.md` §3 PARTIAL exit-ramp (Phase 1 AC fully met; Phase 2 content + legal counsel sign-off blocked-on stakeholder engagement, tracked GAP-154 umbrella). Counts: 88 OPEN → **88 OPEN** (-4 OPEN closed via flip; +4 PARTIAL stay counted; net 0). GAP-183/185/186 deferred next wave per parallel-agent rule #9 (4-doc sweet-spot).

**2026-04-29 (Wave Legal-BRD Phase 1 KICKED OFF — Cluster 13, 9th wave-pack):** Per `meta-gap-priority.md` §3 Business-Logic-P0 ranks above Feature-P0 — sister cluster của Wave Business Correctness 2026-04-29 (closed GAP-049/050/150). Cluster 13 = 4 disjoint OPEN P0 BL legal mandate gaps: GAP-180 (TOS, 15 sections), GAP-181 (AUP, 8 sections), GAP-182 (Privacy Policy — **VN PDPL Decree 13/2023 mandate**, 16 sections), GAP-184 (Data Retention — **VN PDPL Art 6 mandate**, 8 sections + retention matrix). Phase 1 = skeleton-only (frame + sections + cross-refs + TODO markers); Phase 2 (legal counsel content) deferred qua GAP-154 umbrella. Wave plan: `documents/03-planning/waves/wave-2026-04-29-legal-brd-phase1.md`. Overlap analysis: 0 HARD, 1 SOFT (read-only `meta-gap-priority.md` citation). Foundation PR ships `00-brd/README.md` directory map updates centrally → 4 agents touch ONLY their respective skeleton file. GAP-183/185/186 deferred next wave (4-doc cluster size là sweet-spot per parallel rule #9; 7-doc full slice over-budget single wave). Closure: 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 (Phase 2 blocked-on legal counsel). Counts: unchanged by this kickoff entry.

**2026-04-29 (Wave Review Process Improvement + Option D Pages SHIPPED — same-day, 12th cluster, FIRST `incident-to-rule-pipeline.md` 5-stage applied to landing-page miss):** User flagged miss "đã có UI của trang kitehub đâu nhỉ, tôi vẫn thấy 3 repo" after Wave UI Kits Round 2 closure (PR #678). Recovery via 4-PR wave: foundation #680 (Tier 1 — landing parity script + output-review-mandate v1.2.0 → v1.3.0 + review template + memory + GAP-264/265 placeholders) → 2 parallel agents (#682 Tier 2 ui-review-prototype skill 11 files +1095 LOC + 3 callable scripts; #681 Tier 3 hook+CI+lefthook 5 files +191 LOC + 4 self-tests PASS) → 1 bonus parallel agent #683 Option D (Pages workflow + 7 hero screenshots 2.5MB + README showcase section). All 4 PRs CLEAN merged sequential post-CI. **GAP-263 → 🟢 DONE** (umbrella verified post-merge: Tier 1 script + Tier 2 3 scripts + Tier 3 hook/CI all PASS on current state). **GAP-264 → 🟢 DONE** (skill SHIPPED). **GAP-265 → 🟢 DONE** (enforcement SHIPPED). Memory `feedback_post_merge_doc_sync.md` extended with landing-page parity lesson + 3-tier pattern (rule + script + enforcement). Wall-clock ~110 min total (foundation 25 + 2-parallel 50 + 3rd-parallel 7 + sequential merge + closure 30 + cleanup). 12th, 13th, 14th consecutive 0-clarif waves. Counts: -GAP-263 -GAP-264 -GAP-265 net -3 OPEN. Bonus: GitHub Pages live demo `https://victoraurelius.github.io/2026-Kite-Class-Platform/` (1-time post-merge user setup: Settings → Pages → Source = "GitHub Actions", then `gh workflow run deploy-design-system.yml`).

**2026-04-29 (Wave UI Kits Round 2 — Wave 1.5/1.6/1.7 add-ons + starter-kit Phase 2b SHIPPED — same-day extension):** After Wave 1 closure (PR #672), user flagged scope gap "màn hình chính của kitehub đâu" — Wave 1 only shipped KC-side kits despite P2 Center Owner persona using BOTH KC + KH. Recovery via 3 single-agent add-on waves spawned PARALLEL with starter-kit Phase 2b cross-repo work (4 background agents max-cap per `feedback_parallel_agent_strategy.md` rule #9). All 4 agents 0-clarification-round (8th, 9th, 10th, 11th consecutive). 6 PRs merged sequence #677 (formal review report — closes `output-review-mandate.md` §1 "Review evidence preserved" mandate gap) → #673 Wave 1.5 kitehub-pro v2 (32 files, 5,301 LOC, avg 107.8/128, KH SaaS dashboard P2 owner) → #674 Wave 1.6 kiteclass-teacher (28 files, 3,630 LOC, avg 108.0/128, GVCN+subject teacher Tier 2 KC) → #675 Wave 1.7 ai-branding-wizard-v2 (32 files, 4,611 LOC, avg **115.6/128 highest**, Direction C 6-step wizard refactor with ENTERPRISE Advanced Mode + per-resource approve + quality gate /100 widget) → #676 starter-kit Log update → claude-starter-kit#10 upstream rules batch (9 rules, VERSION 2.2.0 → 2.3.0, MERGED on remote). Wave aggregate after add-ons: **7 kits × 76 screens × avg 110.5/128 (+51% vs Round 1 ~73/128)**. Starter-kit local mirror created at v2.3.0 per Q4=A decision; project pointer `.claude/.starter-kit-version` 2.1.0 → 2.3.0. New memories filed: `feedback_phase_0_governance_violation.md` + `feedback_wave_scope_completeness_check.md`. **GAP-262 Phase 2b PR 1 SHIPPED**; gap stays 🟡 PARTIAL — Phase 2 PR 2/3 (skills batches v2.4.0/v2.5.0) deferred. **GAP-263 stays 🔵 OPEN** — Phase 1 standard shipped foundation PR #668 + applied this wave; Phase 2 GAP-264 (`ui-review-prototype` skill) + Phase 3 GAP-265 (hook/CI enforcement) deferred. Counts: -GAP-262 PARTIAL still counted; net unchanged.

**2026-04-29 (Wave UI Kits Round 2 SHIPPED — Cluster 11, 8th wave-pack, FIRST non-gap-closing wave-pack):** 4 PRs merged sequence #668 foundation (29 files +4,871 LOC: wave plan + folder skeleton + GAP-263 Phase 1 HTML prototype review standard + Round 1 archive) → #669 Agent A `kiteclass-pro v2` (14 files +3,119 LOC, avg **108.4/128** Direction B HIGHEST priority — ⌘K palette + sparklines + drag-drop + dark-mode-morph + toast-confetti) → #670 Agent B `kiteclass-parent` (23 files +4,543 LOC, avg **114/128** Direction D pivot to web responsive PWA-grade NOT native, manifest+sw, Zalo OA card primary push) → #671 Agent C `5 components` (35 files +4,177 LOC, avg **106.7/128** for G2 Attendance Roster + G5 Payment Method Selector + G6 Invoice Detail + G7 Parent Invite + G12 Bulk Actions Bar). **Wave aggregate: avg 109.7/128 across 52 screens** (vs Round 1 baseline ~73/128 = +50% lift). Wall-clock ~130 min (foundation 45 + parallel 75 + merge/cleanup 10). Token cost ~1.05M total. **7th consecutive 0-clarification-round wave**, 0 worktree contamination, 0 file conflicts. **Phase 0 governance lesson captured** — pre-wave scaffolding + HTTP server attempt was rolled back per user "Option A" because skipped brainstorm + skipped task breakdown + violated `feedback_wave_plan_through_pr.md`. Recovery proved corrective: foundation PR with retroactive governance shipped clean, 3 agents shipped clean, ~50% Round 1 quality lift achieved. **Track 2 production port to Next.js code DEFERRED** until user accepts Round 2 — will file GAP-264..267 only after acceptance, per `gap-done-discipline.md` §3 PARTIAL exit-ramp. GAP-263 stays 🔵 OPEN (Phase 1 matrix-row + version bump landed; Phase 2 ui-review-prototype skill + Phase 3 hook/CI enforcement deferred). Counts: unchanged (no gaps closed; +GAP-263 filed; +Wave 11 row to Active wave queue marked SHIPPED).

**2026-04-29 (Wave Meta Phase-2 Cleanup SHIPPED — Cluster 7, ~30 min wall-clock, 6th wave-pack):** 3 PRs merged sequence #663 (Agent A, GAP-193 P2 → 🟢 DONE: session-lock guard + audit-gate.py telemetry + new `/end-session` skill, 4/4 smoke test pass) → #661 (Agent B, GAP-194 P2 → 🟢 DONE: lefthook pre-commit gate + local-dev guide) → #662 (Agent C, GAP-195 Phase 2a: triage report 110 candidates classified, 9 recommended for upstream PR; **GAP-262 filed** for Phase 2b cross-repo work). All 3 agents 0-clarification-round (**6th consecutive**). First mixed code/config/docs wave — feature-tdd-agent template held without adjustment. Wall-clock variance 30 vs 95 estimate — waves ship faster when Phase 1 shipped + scope tight + agents experienced. Counts: 89 OPEN → **88 OPEN** (-GAP-193 -GAP-194 closed; GAP-195 stays PARTIAL; +GAP-262 filed; net -1).

**2026-04-29 (Wave Meta Phase-2 Cleanup KICKED OFF — Cluster 7, 6th wave-pack):** Phase-2 follow-throughs of 3 Meta-P1+P2 gaps already shipped Phase 1 — close deferred work. Agent A: GAP-193 P2 (session-lock hook + telemetry, Java audit-gate.py extension). Agent B: GAP-194 P2 (lefthook pre-commit gate — uses lefthook since project has no `.husky/`, npm-only). Agent C: GAP-195 Phase 2a (starter-kit retro-sync triage report; cross-repo upstream PR deferred to Phase 2b → **GAP-262** filed by Agent C). First wave with non-docs majority work (validates `feature-tdd-agent` template). Wave plan: `documents/03-planning/waves/wave-2026-04-29-meta-phase2-cleanup.md`. Overlap: 0 HARD, 2 SOFT (both rule citations). Counts: unchanged by kickoff entry.

**2026-04-29 (Wave Meta-Gov 2 SHIPPED — Cluster 6, ~50 min wall-clock, 5th wave-pack):** 3 PRs merged sequence #656 (Agent B, GAP-225 → 🟢 DONE: scaffold-as-DONE matrix sync + 5 affected-gap cross-refs preserved) → #657 (Agent C, GAP-224 → 🟢 DONE + GAP-202/206/207 status syncs from stale 🟠 IN_PROGRESS to 🟢 DONE per `feedback_post_merge_doc_sync.md`) → #658 (Agent A, GAP-245 → 🟡 PARTIAL: Maven `strict-warnings` profile + 3 CI workflows; **GAP-261-werror-flipday filed** for Phase 2 burndown — plan said GAP-258 but numbering collision, agent used 261). 5th wave-pack execution validates pattern (~50 min vs ~70 estimate, faster due to foundation-bundling savings). All 3 agents 0-clarification-round. **Numbering collision lesson** captured: wave plans should not pre-allocate gap IDs for follow-up gaps — instruct agent to find next-free ID + report. Counts: 91 OPEN → **89 OPEN** (-GAP-224 -GAP-225 closed, GAP-245 PARTIAL stays counted; +GAP-261 filed; 3 syncs no count change; net -2).

**2026-04-29 (Wave Meta-Gov 2 KICKED OFF — Cluster 6 sliced from meta backlog):** Bundled with Wave Business Correctness closure in same foundation PR (saves 1 PR overhead). Cluster 6 = 3 Meta gaps disjoint: GAP-245 (P1 — Maven `-Xlint`/`-Werror` profile in CI), GAP-225 (P1 — scaffold-as-DONE governance docs truth-up), GAP-224 (P3 — `collect-state.sh` regex fix) + housekeeping status-sync for GAP-202/206/207 (merged PRs but Status still IN_PROGRESS). Wave plan: `documents/03-planning/waves/wave-2026-04-29-meta-gov-2.md`. Overlap analysis: 0 HARD, 1 SOFT. Per `meta-gap-priority.md` §3 Meta-P1 ranks above feature-P0 — top-of-queue after BL wave. 3 worktree-isolated agents spawn after this closure-foundation PR merges. Counts: unchanged by this kickoff entry.

**2026-04-29 (Wave Business Correctness SHIPPED — Cluster 5 Phase-1, ~75 min wall-clock):** 3 PRs merged sequence #651 (Agent A, GAP-150 → 🟢 DONE: 5 BRD skeletons + README) → #652 (Agent B, GAP-049 → 🟡 PARTIAL: rule + matrix flip; **GAP-156 filed** for Phase 2 audit) → #653 (Agent C, GAP-050 → 🟡 PARTIAL: 3 framework ACs; execution stays in GAP-152). Hotfix #654 detoured ~10 min (README freshness 2 files crossed 90d threshold same day; per `feedback_ci_gate_ship_incidental_coverage.md` fix-in-same-PR). All 3 agents 0-clarification-round (`docs-only-agent.md` template stable). Coordinator-only ROADMAP rule held (0 merge race). 4th wave-pack execution validates ~5x speedup vs serial. **GAP-155** filed (BRD content fill, Phase 2 of GAP-150). Counts: 91 OPEN → **91 OPEN** (-GAP-150 closed; +GAP-155 +GAP-156 filed; net +1).

**2026-04-29 (Wave Business Correctness KICKED OFF — Cluster 5 Phase-1 sliced):** Per `meta-gap-priority.md` §3, Cluster 5 (BL-P0+P1) ranks above feature-P0. Original cluster ~7-9h → oversized per `cluster-pattern.md`. Sliced each into Phase 1 (~25-30 min/agent, ~75 min wave wall-clock target): GAP-150 = ALL ACs (skeleton docs already scoped Phase-1-only); GAP-049 = rule file + matrix-row flip only, Phase-2 audit→GAP-156 to be filed by Agent B; GAP-050 = 3 remaining framework ACs (cadence + pre-flight + quality-audit category), execution stays in GAP-152. Wave plan: `documents/03-planning/waves/wave-2026-04-29-business-correctness.md`. Overlap analysis: 0 HARD, 1 SOFT (read-only citation of `meta-gap-priority.md`). 3 worktree-isolated agents spawn after foundation PR merges. Counts: unchanged by this kickoff entry.


**2026-04-28 (Wave 9 skill restructure SHIPPED — 2-agent parallel cluster, ~6 min wall-clock):** Long-deferred Wave 9 single-track item closed via 3-slice cluster. Slice A (Agent A worktree-isolated, ~5.5 min) split `.claude/skills/workflow/development-workflow.md` (1221 LOC monolith) → folder skill `development-workflow/SKILL.md` (52 lines) + 8 reference docs (1170 LOC across files). Slice B (Agent B worktree-isolated, ~4.4 min, parallel with A) split `.claude/skills/workflow/priority-pr-planning.md` (800 LOC monolith) → folder skill `priority-pr-planning/SKILL.md` (55 lines) + 8 reference docs (792 LOC across files). Slice C (coordinator, ~5 min) updated `_README-skills-index.md` paths + emptied `GRANDFATHERED_EXEMPTIONS` in `scripts/check-skill-conventions.sh`. Verification: PASS 47 / WARN 12 / FAIL 0 (was PASS 44 / WARN 14 / FAIL 0; both grandfathered files eliminated). Both new SKILL.md files comply with skill-conventions §2 (frontmatter + trigger-keyword description + Gotchas section + body <100 lines). Counts: unchanged (no gap files; this was Wave 9 single-track item). Validates wave-pack-planner methodology again — 2 disjoint file buckets, 2 parallel agents, ~6 min vs estimated ~1h serial = ~10x speedup.

**2026-04-28 (GAP-259 SHIPPED PARTIAL + GAP-260 follow-up filed — gateway tenant-keyed rate limit, ~45 min, PR #641 merged):** Sister of GAP-258 from same 2026-04-28 article state-check, closed PARTIAL same-session per `gap-done-discipline.md` §3 exit-ramp. Implementation: `KeyResolverConfig` extended with `tenantKeyResolver` (subdomain-keyed, stateless to run before `TenantResolverGatewayFilterFactory` in filter chain) + `apiKeyResolver` (X-API-Key header); `RateLimitConfig` extended with `tierMultiplier` map (FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×, **data-only** — actual `RedisRateLimiter` enforcement deferred); `RateLimitMetricsFilter` global filter emits `gateway.rate.limit.rejected{key_type, tenant}` Counter on 429; `application.yml` `platform-branding` route wired with `RequestRateLimiter` + `tenantKeyResolver` (replenishRate=30, burstCapacity=60, env-overridable). 17 unit tests + 27/27 gateway suite green. **ADR-023 ACCEPTED** documenting strategy + 3 alternatives rejected (JWT-only / TenantResolver-first / Envoy-Kong). **GAP-260 (P2) filed** for Stage 1+2+3: tier multiplier enforcement (custom `TierAwareRedisRateLimiter` extension) + remaining 6 authenticated routes wiring + alert rule extension. Counts: 91 OPEN → **91 OPEN** (-0 closed; GAP-259 stays PARTIAL; +GAP-260 filed; net 0 vs post-#640 baseline).

**2026-04-28 (GAP-258 SHIPPED — AI input prompt token cap, single-PR ~30 min, PR #640 merged):** Article-driven gap from earlier today (GAP-122 wave) closed same-day. Implementation: `PromptTokenEstimator` util (chars/4 heuristic) + `AIInputCapConfig` (tier-aware caps FREE 2000 / BASIC 4000 / PREMIUM 8000 / ENTERPRISE 16000 tokens, env-overridable) + `AIInputCapService` (guard with Micrometer counter `ai.input.token.rejection{tier}`) wired into all 4 `AIBrandingController` endpoints AFTER rate-limit, BEFORE `recordUsage` (so reject path doesn't burn quota). 13 unit tests + 3 IT (oversize reject / within-cap allow / FREE-vs-PREMIUM differential). Business rules `BR-INPUT-CAP-001..007` + metrics catalog row + 4 config keys in `ai-agent-workflow/rules.md`; `.claude/rules/ai-branding-guidelines.md` v1.1.0→1.2.0 with new §2.5 MANDATORY rule. Verification: 166/166 kitehub-branding tests green. UX-impact analysis: ~25-40× headroom over typical wizard usage; only edge case is data-URI logos (correct rejection). Counts: 92 OPEN → **91 OPEN** (-GAP-258).

**2026-04-28 (GAP-122 SHIPPED single-gap parallel wave + 2 sister gaps filed from article state-check):** Single-gap focus per Option B handoff, sliced internally into 3 disjoint slices for parallel agent execution (validates `feedback_wave_plan_before_serial_prs.md` rule that single-gap ≠ single-thread). Slice A (CI gate, `scripts/check-alert-runbook-url.py` + workflow job) + Slice B (12 alerts in kitehub docker + helm `kitehub-platform-alerts` group) + Slice C (12 runbook stubs + `alerting-standards.md` 192 LOC + runbooks/README index update). 3 worktree-isolated agents ~11 min cumulative parallel + ~5 min coordinator merge + verify. Incidental coverage: 6 pre-existing alerts (`DocumentBrandingCacheMissStorm` × 2, 5 SLO-tier alerts) gained `runbook_url` after CI gate surfaced them — 5 SLO point to existing `api-performance-slo.md` doc, 2 cache alerts point to new `branding-cache-miss-storm.md` runbook (78 LOC) shipped same PR. Verification: full repo scan 3 files / 54 alerts / 0 failures. **Sister gaps filed via 2026-04-28 article-driven state-check** (article: "Những lỗi 'chết người' khi build AI backend (Phần 2) — Không rate limit"): GAP-258 P1 — AI input prompt token validation (cost-attack defense; current `OpenAIClient` caps output only); GAP-259 P1 — gateway tenant-key rate limit (currently `ipKeyResolver` only, NAT-shared IP starves co-tenants). Article points 1+5+3 already DONE in project (CB, tier differentiation, request-count cap); points 2+6 filed (this wave + GAP-019/017 cover); point 4 retry assessed acceptable (CB suffices). Counts: 91 OPEN → **92 OPEN** (-GAP-122 closed; +GAP-258 +GAP-259 filed; net +1).

**2026-04-28 evening (Cluster 4 KH admin flagged OVERSIZED — sliced; Option B single-gap pick handoff for next session):** After Wave DR/Backup SHIPPED, attempted Cluster 4 (KH admin GAP-066/067/068) per skill Step 1-2. File-overlap check exit 1 + coordinator gap-file review surfaced **size mismatch**: each gap = multi-week feature (066 ~2-3w, 067 ~11w phased, 068 ~3w), not fit wave-pack 60-75 min target. Per `cluster-pattern.md` §"Anti-cluster patterns" oversized rule, cluster declined. **User chose Option B** (single-gap focus). Handoff plan written: `documents/03-planning/plans/pr-next-session-single-gap-handoff.md`. **Recommended next-session pick: GAP-122** (12 platform alerts, ~3-4h, no blocker — Prometheus + Alertmanager already shipped Wave Obs + GAP-121 runbook template ready). Fallback: GAP-067 Phase 1 stub (infra-blocked for full scope, only stub-only viable). Cluster 4 row in §"Active wave queue" → 🟡 SLICED. Counts unchanged (no gaps closed/filed by this entry — handoff annotation only). `data/wave-history.jsonl` appended with cluster-evaluation data point for analyze-overlap.sh v1.1 calibration.

**2026-04-28 (Wave DR/Backup SHIPPED — first real-world consumer of wave-pack-planner skill, ~75 min wall-clock, contamination incident captured as new memory rule):** 4 PRs merged sequence #631 foundation → #634 Agent B GAP-118 (clean cherry-pick) → #633 Agent C GAP-119 (post-rebase to drop contaminating commit) → #632 Agent A GAP-117. Counts: 100 OPEN → 98 OPEN (-GAP-118 -GAP-119 closed; GAP-117 stays as 🟡 PARTIAL with Phase 3 split into +GAP-257; net -2). 3 disjoint agents in single message wave-pack pattern validated end-to-end. **Critical lesson surfaced:** worktree absolute-path bug — all 3 agents reported same issue; Agent B's GAP-118 commit (`27f96c1e`) landed on Agent C's branch due to `cd` to coordinator absolute path bypassing worktree cwd → ~15 min coordinator recovery (rebase + force-push to drop duplicate). New memory `feedback_worktree_absolute_path_contamination.md` filed; SKILL.md §Gotchas + 3 agent templates (`docs-only`, `feature-tdd`, `wave-coordinator`) updated with worktree-cwd guard rule + RELATIVE path mandate. Wall-clock: foundation ~15 min + 3 parallel agents ~10 min + recovery ~15 min + sequential merges ~10 min + closure ~25 min = ~75 min wall-clock total — matches Wave Obs benchmark. Cluster pipeline next: Cluster 4 KiteHub admin (GAP-066/067/068).

**2026-04-28 (Wave Meta-Day-2 SHIPPED + Wave DR/Backup KICKED OFF — wave-pack-planner skill operational):** PR #630 (`bf24ce21`) closes "Day 2 framework deliverable" line item with `quality/wave-pack-planner/SKILL.md` + 6 reference docs + 5 agent prompt templates + `scripts/analyze-overlap.sh` + `data/wave-history.jsonl`. Skill self-validated by being built with own methodology — 3 parallel `general-purpose` agents (refs / templates / script+data) on disjoint buckets in single message → ~6 min wall-clock vs ~1h serial estimate (~10x meta speedup). User caught serial-vs-parallel anti-pattern mid-stream → switched mid-flight = real-world validation of `start-session` Step 4.5 wave-eligibility hint. Compliance: PASS 45 / WARN 14 / FAIL 0. Cleanup: 5 stale remote branches deleted (incl. 4 Wave Obs leftovers). **Wave DR/Backup KICKED OFF** as first real-world consumer of the new skill: Cluster Pack 2 = GAP-117 (P0 restore drill) + GAP-118 (P1 MinIO/S3 backup) + GAP-119 (P1 platform DR runbook). Wave plan: `documents/03-planning/waves/wave-2026-04-29-dr-backup.md`. Coordinator-reviewed overlap matrix: 0 HARD, 1 SOFT (audit-doc citation). Bucket: A=GAP-117 (`feature-tdd-agent`), B=GAP-118 (`feature-tdd-agent`), C=GAP-119 (`docs-only-agent`). Wall-clock target ~65-75 min. Counts unchanged by this entry (wave kickoff only). **Day 2 framework deliverable line below = SHIPPED.**

**2026-04-28 (Wave Observability SHIPPED — 3-agent parallel cluster pack, 4 PRs merged ~75 min wall-clock):** First agent-first wave-pack methodology demonstration COMPLETE per Option C hybrid strategy. PRs: foundation #624 → Agent A #626 (GAP-121 runbooks DONE) → Agent B #625 (GAP-143 Grafana DONE) → Agent C #627 (GAP-144 Alertmanager **PARTIAL** per `gap-done-discipline.md` §3 — 4/6 ACs done, 2 deferred to live-cluster mock-fire verification with `amtool` runbook recipe documented). Total wave delta: 25 files, +2251 LOC, −74 LOC. Cluster status: GAP-121 + GAP-143 → 🟢 DONE; GAP-144 → 🟡 PARTIAL (mock-fire ACs blocked on EKS+ESO+AWS SM secrets provisioning, tracked in §Current State table per gap-done-discipline). Counts: 102 OPEN → 100 OPEN (−GAP-121 −GAP-143; GAP-144 stays as 🟡 PARTIAL not removed from OPEN). **Cadence improvement:** 3 gaps closed in ~75 min vs traditional ~6h serial = ~5x speedup. Worktree-confusion artifact (Agent B + C cross-contamination of `adr/README.md`) recovered via stash dance — captured for Day 2 wave-pack-planner skill lessons-learned. Worktrees + 6 local branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6. **Day 2 framework deliverable** (next session): `quality/wave-pack-planner/SKILL.md` codifies cluster-then-spawn pattern + 5 specialized agent prompt templates (docs-only, test-only, p3-cleanup, feature-tdd, wave-coordinator). **Next wave candidates** documented below: GAP-122 (alerts) + Cluster 2 DR/Backup (GAP-117/118/119) + Cluster 3 KiteHub admin (GAP-066/067/068).

**2026-04-28 (Wave Observability KICKED OFF — first agent-first cluster pack):** Strategy shift discussed end-of-session — current cadence ~2-3 gap/day = 50-80 days to clear 125 OPEN+PARTIAL queue, too slow. Decision: Option C hybrid — execute first wave-pack today demonstrating cluster pattern, codify framework tomorrow. Wave plan: `documents/03-planning/waves/wave-2026-04-29-observability.md`. Cluster pack 1 = Observability (3 disjoint gaps): GAP-121 (per-alert runbooks, P1) + GAP-143 (Grafana dashboards in Helm, P1) + GAP-144 (Alertmanager production receivers, P0). 3 parallel worktree-isolated agents target ~60-80 min wave wall-clock. GAP-122 (12 new alerts) deferred to next wave to avoid `alert-rules.yml` race with Agent A's runbook_url backfill. GAP-114/115 (logging migration) parked separate track per `logs-format-standard.md` migration phases (multi-service rollout, not 1-PR scope). Lessons-learned from this wave feed into Day 2 framework PR (`quality/wave-pack-planner/SKILL.md`).

**2026-04-28 (Skill-conventions cleanup wave SHIPPED — 21 → 2 grandfathered, 5 PRs merged):** Skill-conventions cleanup wave (queue item #3 from earlier today) closed via 5 sequential + parallel PRs. **Phase 1 #616** (sync, 7 prefixed-heading renames: `## KiteClass Gotchas` / `## Vietnamese-specific gotchas` → `## Gotchas` for 4 core/* + 3 doc-gen/*). **Phase 2a #617** (sync, 4 quality skills full Gotchas + 5 workflow SKILL.md description trigger-keyword rewrites). **Phase 2b #618** (Agent A worktree-isolated, 5 workflow SKILL.md Gotchas appended). **Phase 2b #619** (Agent B worktree-isolated, 3 workflow loose .md Gotchas appended). **Phase 2c #620** (sync finalizer, removed 8 workflow entries from `GRANDFATHERED_EXEMPTIONS`). Net delta: PASS 44 → 44 (unchanged — files already passed silently after Gotchas added), WARN 38 → 14 (−24 cumulative across all phases), FAIL 0 throughout. Wave wall-clock ~50 min (incl. 2 parallel agents ~10 min). Worktrees cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6. **2 deferred to Wave 9** (`workflow/development-workflow.md` 1221 lines + `workflow/priority-pr-planning.md` 800 lines) — body exceeds 500-line Check 4 limit, frontmatter add would expose new FAIL until split or moved to `reference/`. Tracked under GAP-251 follow-up. Remaining 14 WARN: 12 audit/review eval-fixtures (separate GAP-253 best-practice concern), 2 grandfathered (the deferred large files). **Counts unchanged** — no gap files added/closed; this was an existing GAP-251 follow-up done as cleanup, not a new gap.

**2026-04-28 (AI Branding cluster ⏸ DEFERRED — pending local Ollama + Docker stack):** Pickup attempt on top-of-queue items (GAP-223 Sub-PR 223.2 + GAP-006 Gemma 4 9B migration) blocked at session-start pre-flight: `localhost:11434` not reachable + Docker stack down. Per `feedback_gap006_infra_blocker.md`, WSL2 CPU-only is too slow for 9B A/B test against MixSura (long-pole AC). GAP-244 dev-profile schema mismatch shipped today via V46 ✅ — one prior blocker cleared, but Ollama + stack still needed. GAP-006 + GAP-223 marked DEFERRED with Blocked-on header + Log entry; ROADMAP "Next recommended wave" queue rotated to skip the cluster. Stale GAP-229 entry removed from queue (was already DONE 2026-04-26 PR #561+#562). New top eligible: GAP-055 (BL-P0, VN report-card format, single-PR, no AI dep) and skill-conventions cleanup wave (Meta-P3, wave-eligible). Counts: unchanged (no gaps closed/filed by this entry — annotation only).

**2026-04-28 (GAP-255 SHIPPED + Wave Meta-Gov 1 follow-up complete — 7/8 wave gaps DONE):** PR #612 closes GAP-255 (README freshness CI). New `scripts/check-readme-freshness.sh` (~225 LOC, shellcheck-clean) + 5 self-test fixtures (3 dynamic-date generated runtime + 2 committed: `exempt.md` + `no-date.md`) + new CI job `readme-freshness` in `script-quality.yml`. Baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs. `output-review-mandate.md` v1.1.1 → v1.1.2 (PATCH) — added §3 matrix row "README freshness"; flipped "Skills (meta)" row PARTIAL → DONE post-#610 sync. **2 bugs caught during dev (validates self-test value):** (1) regex `^\*\*Last[ -]?Updated\*\*` failed on project's `**Last Updated:**` colon-inside-bold convention → relaxed to non-anchored `Last[ -]?Updated`; (2) YAML step name with colon-space (`5 fixtures: 3 dynamic`) parsed as mapping → workflow ran with 0 jobs registered → caught by `python3 -c "import yaml; yaml.safe_load(...)"` validation. Wave Meta-Gov 1 final: **7 DONE** (GAP-249/250/251/252/253/254/255), **1 GATED** (GAP-256 read-first rule — eligible to file after GAP-255 active ≥7d per `incident-to-rule-pipeline.md` premature-rule guard; timer started 2026-04-28). Counts: 92 OPEN → 91 OPEN (-GAP-255 closed; GAP-256 still OPEN as planned). 5 stale worktrees from prior waves (GAP-236 + gap-done-discipline) cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6.

**2026-04-28 (Wave Meta-Gov 1 SHIPPED — 4 PRs merged in sequence, 6/8 gaps DONE):** Foundation PR #607 (8 gap files + wave plan + ROADMAP + Phase 0 root README pixel-art redesign + kiteclass/kitehub README staleness fix) → Move 1 PR #608 (Agent A: 8-rule frontmatter backfill + `scripts/check-rule-frontmatter.sh` + CI job; bonus catch — detector found `output-review-mandate.md` missing `Applies-to` field, fixed inline with PATCH bump 1.1.0→1.1.1) → Move 2 PR #609 (Agent B: `scripts/check-skill-conventions.sh` 456 LOC + 21-skill grandfathered-exemption list + 3 self-test fixtures + 6 audit eval fixtures + skills index refresh + 5-tier severity rubric on `two-stage-code-review.md`) → Sub-PR C #610 (`skill-conventions` CI job wired, GAP-251 PARTIAL→DONE). All gap closures pass `gap-done-discipline.md` §2 (no banned phrases in DONE-flip Log entries). 6 DONE: GAP-249/250/251/252/253/254. 2 OPEN follow-ups: GAP-255 (README freshness CI), GAP-256 (read-first rule, conditional on GAP-255 active ≥7d). Wave wall-clock ~90 min. Counts: 98 OPEN → 92 OPEN (-6 closed; GAP-255/256 already counted as filed in 90→98 entry below).

**2026-04-28 (Wave Meta-Gov 1 FILED — 8 gaps from ecosystem audit + README sweep):** Per ecosystem audit + external research findings (top skill repos: anthropics/skills, obra/superpowers, trailofbits/skills, awesome-skills/code-review-skill, ComposioHQ/agent-orchestrator + tirth8205/code-review-graph for README style). 8 meta-governance gaps filed:
- **GAP-249** P1 Meta — Bulk frontmatter backfill on 8 non-compliant rules
- **GAP-250** P1 Meta — CI gate enforcing rule frontmatter (+ self-test)
- **GAP-251** P1 Meta — `scripts/check-skill-conventions.sh` lint for 27 SKILL.md
- **GAP-252** P2 Meta — Refresh `_README-skills-index.md` (12-day drift) + drift detector
- **GAP-253** P2 Meta — Eval fixtures pilot (business-logic-audit + security-audit; Anthropic 2026 mandate)
- **GAP-254** P2 Meta — Severity rubric (5-tier Blocker→Praise) on `two-stage-code-review`
- **GAP-255** P2 Meta — README freshness CI (`scripts/check-readme-freshness.sh` + workflow job, fixture-tested)
- **GAP-256** P2 Meta — Rule "read README before grep" (AI navigation) — conditional on GAP-255 active ≥7d

Plan: `documents/03-planning/waves/wave-meta-governance-1.md`. **Phase 0** (foundation PR inline): redesign root README with pixel-art KITE logo + Variant B frame + badges; light/moderate fix `kiteclass/README.md` + `kitehub/README.md` (Spring Boot 3.5.11→3.5.14, `Last Updated` refresh, service status table). **Move 1** (Agent A): GAP-249/250 — rule frontmatter discipline. **Move 2** (Agent B): GAP-251/252/253/254 — skills convention + index + eval fixtures + severity rubric. 2 parallel `isolation: worktree` agents per `feedback_parallel_agent_strategy.md`; wave plan PR-first per `feedback_wave_plan_through_pr.md`. Sub-PR C deferred for skill-conv CI wire-up.

Counts: **90 OPEN → 98 OPEN** (+GAP-249/250/251/252/253/254/255/256 filed).

**2026-04-28 (Triage — 4 follow-up gaps filed post-Wave GAP-236 + IDE warning incident):** Per `audit-to-gap-pipeline.md` Step 2.5 state-check:
- **GAP-245** P1 Meta — CI does not enforce IDE warnings (deprecation/unused/raw types). Process gap surfaced after PR #605 closed 8 shipped warnings; memory rule alone is insufficient enforcement layer per `feedback_incident_to_rule_pipeline.md` 5-stage pipeline.
- **GAP-246** P3 — delete unused `kiteclass-frontend/src/components/ui/calendar.tsx` (dead post-Wave 7-Perf attendance migration; Agent B finding). 1-line PR.
- **GAP-247** P2 — HCaptcha `next/dynamic` wrapper with forwardRef + useImperativeHandle for KH `/register` (~80 KB potential First Load JS win; Agent D revert documented).
- **GAP-248** P2 — KC `(auth)/layout.tsx` provider chunk hoist refactor (131 KB common chunk Agent A flagged); investigate-then-decide via `bundle-analyzer-baseline-kc.html` trace.

Counts: **86 OPEN → 90 OPEN** (+GAP-245/246/247/248 filed; all post-wave triage).

**2026-04-28 (Wave GAP-236 SHIPPED — 4 parallel agents, ~18 min wall-clock):** Per `feedback_parallel_agent_strategy.md` + `feedback_wave_plan_through_pr.md` (wave plan PR-first landed in #599). 4 worktree-isolated agents on disjoint FE buckets, 0 file conflicts (only additive Log conflicts on GAP-236 file resolved by parent rebase):

| Agent | Bucket | PR | Pages |
|:-----:|--------|:--:|:-----:|
| A | KC `(auth)` + `(public)` | #601 | 7 (top 3 auth routes −119 KB First Load JS) |
| B | KC `(dashboard)/{admin,attendance,billing}` | #600 | 5 (incl. `/attendance/reports` 417-LOC) |
| C | KC `(dashboard)/{classes,courses,students,teachers}` | #602 | 11 (largest bucket — form/attendance lazy) |
| D | KH all groups + Sub-PR C analyzer baselines | #603 | 10 (incl. `/admin/instances/[id]` 452-LOC) |

**Wave validation:**
- Total **33 pages converted** (≥30 AC threshold ✅)
- Per-app post-wave max First Load JS: KC 217 KB / KH 200 KB (well under 250 KB CI ceiling)
- All 90 routes (52 KC + 38 KH) within bundle budget
- 565 KC tests + 484 KH tests pass; 0 regression
- Sub-PR C: analyzer baseline HTMLs committed (KC 749 KB + KH 876 KB raw, both <5 MB so no compression)
- 3 follow-up findings surfaced for triage (unused `ui/calendar.tsx`, HCaptcha ref-forwarding gap, `(auth)/layout.tsx` chunk hoist)
- ~18 min wall-clock for 4 agents (vs estimated 1-2h serial)

GAP-127 PARTIAL → 🟢 effectively closed via GAP-236 closure. GAP-236 status: 🟡 PARTIAL → 🟢 DONE. Counts: **87 OPEN → 86 OPEN** (-GAP-236 closed).

**2026-04-28 (GAP-244 SHIPPED + dev profile cleanup):** Path A migration `V46__align_audit_columns_to_bigint.sql` ALTERs `created_by` / `updated_by` from VARCHAR to BIGINT across 19 V28..V44 tables, matching `BaseEntity` (Long). Idempotent DO block keyed on `information_schema.columns.data_type`; Wave02MigrationsTest extended with column-type assertion. PR #597 + PR #598 (revert `application-dev.yml` Flyway+create-drop workaround now Flyway+validate path is viable). 1123/1123 kiteclass-core tests green. Counts: **88 OPEN → 87 OPEN** (-GAP-244 closed).

**2026-04-27 (GAP-235 wave SHIPPED — 4 sub-PRs serial in single session, ~3h):** AI Branding mock-data wave fully closed. Sub-PR E1 #588 (OpenAPI export pipeline + `kiteclass/shared/` fixtures starter, fixed MockMvc-vs-springdoc + test-resources application.yml override bugs in test), Sub-PR F #589 (BE `BrandingDataSeeder` `@Profile("dev")` idempotent, 4 unit tests), Sub-PR E2 #590 (FE MSW v2 handlers — 11 endpoints, lifecycle state machine, ETag/304, 15 vitest tests), Sub-PR G #591 (`local-dev-mock-data.md` guide + `smoke-ai-branding-dev.sh` shellcheck-clean + `ai-branding-demo.spec.ts` Playwright spec gated by `AI_BRANDING_DEMO_RUN=1`). Live screenshot capture deferred — surfaced **GAP-244** (V29+ migrations declare `created_by VARCHAR(100)` while `BaseEntity.createdBy` is `Long`, sibling case to `feedback_jpa_jsonb_jdbctypecode.md`); **PR #592 ships dev-profile workaround** (application-dev.yml ddl-auto override + dev-start.sh dev-profile activation + `INTERNAL_API_SECRET` default) so Core boots in ~7s on fresh DB; root canonicalization tracked in GAP-244. Counts: **89 OPEN → 90 OPEN** (-GAP-235 closed; +GAP-244 filed). GAP-235 had 4 sub-PRs all merged in this session (E1/E2/F/G), GAP-244 is followup work.

**2026-04-27 (Wave P2-Cleanup SHIPPED — 3 parallel agents, ~12 min wall-clock):** Per `feedback_parallel_agent_strategy.md` + `feedback_wave_plan_through_pr.md` (wave plan via PR first — landed in #581). 3 worktree-isolated agents disjoint files, 0 conflicts:

| Agent | Gap | PR | Result |
|:-----:|-----|:--:|--------|
| A | GAP-234 architecture/diagram drift | #582 | 🟢 DONE — 8 files updated; 11 v2 entities added to ERD; 4 PUMLs synced (PNG/SVG regen deferred — needs plantuml binary) |
| B | GAP-236 FE bundle budget CI | #583 | 🟡 PARTIAL — CI guardrail shipped (250KB threshold + override mechanics); 13 unit tests; baseline KC <236KB / KH <194KB; 44+ page conversions still deferred |
| C | GAP-237 admin AMQP cache invalidation | #584 | 🟢 DONE — TopicExchange + 2 listener queues; 6 new tests; admin 29/29; subscription 355/355 (no regression); feature-flagged off until subscription-side dispatcher lands (informational ADR-021 follow-up) |

**Wave validation:**
- Zero merge conflicts (disjoint files honored ✅)
- Zero rule violations from agents
- ~93% wall-clock reduction (~12 min parallel vs estimated 4-6h serial)
- Wave plan PR-first per `feedback_wave_plan_through_pr.md` — no rule violation this time

Counts: **89 OPEN → 87 OPEN** (-GAP-234 -GAP-237 closed; GAP-236 stays PARTIAL but advanced).

**2026-04-27 (GAP-243 SHIPPED — flips GAP-241 + GAP-242 to DONE):** GAP-243 status 🔵 OPEN → 🟢 DONE same day. Option A (least invasive): extend AdminControllerTest's `@DynamicPropertySource` with S3 mock properties + `@MockBean RabbitTemplate` for Mockito proxy. Verification: AdminControllerTest **7/7 ✅**, admin full suite **23/23 ✅**, subscription **355/355 ✅** (no regression). `kitehub-ci.yml` admin job exclusion removed — full admin suite now runs in CI. Cascade closure: **GAP-241 PARTIAL → DONE** (CI exclusion gone), **GAP-242 PARTIAL → DONE** (downstream test path now green). Counts: **92 OPEN → 89 OPEN** (-GAP-241/242/243 closed). Wave 7 admin module cleanup chain fully resolved (GAP-238 → 240 → 241 → 242 → 243, 5 gaps closed in same session).

**2026-04-27 (GAP-242 PARTIAL — V11 Postgres SQL fixed):** GAP-242 status 🔵 OPEN → 🟡 PARTIAL. Root production bug resolved: V11 had `UNIQUE (..., (sent_at::date))` constraint with expression — Postgres rejects (SQL state 42601, only column names allowed in UNIQUE CONSTRAINT). Split into table CREATE + separate `CREATE UNIQUE INDEX` (which DOES support expressions). V11 had never run successfully against any Postgres → safe in-place edit. Subscription tests use Hibernate `ddl-auto=create-drop` (Flyway disabled) so 355/355 still pass. AdminControllerTest's deeper test-infra gaps (S3 mock, RabbitMQ mock for full @SpringBootTest) refiled as **GAP-243** (P2). Counts: **91 OPEN → 92 OPEN** (+GAP-243 filed; GAP-242 stays PARTIAL). GAP-241 also stays PARTIAL pending GAP-243.

**2026-04-27 (GAP-241 PARTIAL — admin/email/gateway CI jobs added):** GAP-241 status 🔵 OPEN → 🟡 PARTIAL. Added 3 jobs to `kitehub-ci.yml`: `test-admin` (excludes `AdminControllerTest` pending GAP-242 Flyway fix), `test-email` (20/20 pass), `test-gateway` (10/10 pass). `code-quality` job needs all 5 module tests. CI no longer blind to admin/email/gateway regressions. Re-enable `AdminControllerTest` once GAP-242 closes → flip GAP-241 to DONE. Counts: **91 OPEN → 91 OPEN** (-0; GAP-241 stays PARTIAL).

**2026-04-27 (GAP-240 SHIPPED + GAP-242 filed):** GAP-240 status 🔵 OPEN → 🟢 DONE same-day. Fix in same PR as GAP-238 hardening. (1) Admin's `@EnableJpaRepositories` + `@EntityScan` extended to include subscription's `outbox/idempotency/domain` packages. (2) GAP-238 fix hardened — `@ConditionalOnMissingBean` insufficient for user-code @Configuration ordering across modules; replaced with explicit `@Bean(name="adminCacheManager")` + `@Primary` and `@Bean(name="subscriptionCacheManager")`. Both beans coexist (distinct names); admin's @Primary wins for @Cacheable. Verification: `KiteHubAdminApplicationTest.contextLoads` ✅ passes (was failing); subscription full suite 355/355 still pass; admin unit tests 15/15. **Surfaced GAP-242**: 7 `AdminControllerTest` still fail with Flyway V11 SQL incompatibility in test DB (separate test-infra concern, P2). Counts: **91 OPEN → 91 OPEN** (-GAP-240 closed; +GAP-242 filed).

**2026-04-27 (GAP-238 SHIPPED + 2 follow-ups filed):** GAP-238 status 🔵 OPEN → 🟢 DONE same day filed. Fix: `@ConditionalOnMissingBean(CacheManager.class)` on subscription's bean + admin's manager declares transitive cache names + `@Configuration` rename for defensive uniqueness. Verification: admin unit tests 15/15 pass, subscription full suite 355/355 pass, BeanDefinitionOverrideException no longer in admin context startup. **Surfaced 2 deeper pre-existing issues** (not GAP-238 scope, filed as follow-ups): **GAP-240 P1** — admin JPA repository scan misses `SubscriptionOutboxRepository` (8 admin @SpringBootTest still fail context load); **GAP-241 P1** — `kitehub-ci.yml` doesn't test admin/email/gateway modules at all (CI blind spot — that's why GAP-238 + GAP-240 shipped to main invisibly). Counts: **90 OPEN → 91 OPEN** (-GAP-238 closed; +GAP-240 +GAP-241 filed).

**2026-04-27 (Wave 7-Perf SHIPPED — 4 parallel agents, ~16 min wall-clock vs 9-17h serial estimate):** 4 parallel `isolation: worktree` agents closed/advanced 4 perf gaps in disjoint scope. Per `feedback_parallel_agent_strategy.md` rule #5 (sequence merges) + rule #6 (manual worktree cleanup): 4 PRs merged, 4 worktrees force-removed, 4 local + 4 remote branches deleted.

| Agent | Gap | PR | Result |
|:-----:|-----|:--:|--------|
| A | GAP-126 admin dashboard cache | #569 | 🟢 DONE — @Cacheable + Pageable + in-process Spring event invalidation; 15/15 tests |
| B | GAP-127 FE code-splitting | #570 | 🟡 PARTIAL — bundle analyzer + 10 pages/app + optimizePackageImports; baseline <250KB; 1034/1034 tests |
| C | GAP-130 docker resource limits | #568 | 🟢 DONE — 4 compose files, 114 limit declarations; runbook in 05-guides |
| D | GAP-135 SLO instrumentation | #571 | 🟡 PARTIAL — 16/29 controllers @Timed; 5 Prom rules + 8 Grafana panels |

**4 follow-up gaps filed (Agent return findings):**
- **GAP-236** P2 — FE code-splitting completion (44+ pages) + CI bundle budget guardrail
- **GAP-237** P2 — Cross-service Outbox cache invalidation (kitehub-admin AMQP integration)
- **GAP-238** P1 — `cacheConfig` bean collision admin↔subscription (pre-existing, latent CI flake hazard)
- **GAP-239** P2 — API SLO coverage completion (13 + admin controllers) + PR template SLO declaration

**Wave validation:**
- Zero merge conflicts (disjoint files honored ✅)
- Zero rule violations from agents (worktree path discipline maintained ✅)
- Pre-existing CI bug surfaced (GAP-238) — would have remained latent without Wave 7-Perf
- Memory `feedback_wave_plan_through_pr.md` filed earlier same session for parent direct-push violation

Counts: **88 OPEN → 90 OPEN** (-GAP-126 -GAP-130 closed; +GAP-236/237/238/239 filed; GAP-127/135 stay PARTIAL but progressed). Wave 7 Meta+Feature P0 queue narrowed: GAP-005 + GAP-011 still infra/designer-blocked.

**2026-04-26 (GAP-014 planning portion v2-aligned — Wave 7 Meta-P0):** GAP-014 status PLANNED → 🟡 PARTIAL. Wave plan `wave-mock-data-local-dev.md` §7 rewritten end-to-end against shipped v2 controllers in `kiteclass-core` (NOT kitehub-branding per architecture doc drift). Replaced 12 aspirational endpoints with 10 real ones (InstanceController 8 + BrandingPackageController 1 + PublicBrandingController 1 + InternalWebhookController 1). Internal services (Analyzer/Planner/Executor/QualityReviewer/ContentModeration/Saga) called out as non-REST. Added §7.7 Out-of-scope với 6 deferred items (GAP-005/006/011/012/020/070). Implementation portion (MSW handlers + DataSeeder + demo) split to **GAP-235** (P1, wave-eligible 4 sub-PRs). Counts: **87 OPEN → 88 OPEN** (+GAP-235; GAP-014 stays PARTIAL). Wave 7 Meta-P0 queue narrowed: GAP-005 + GAP-011 remain (GAP-014 moved to PARTIAL).

**2026-04-26 (GAP-016 final closure — Wave 7 Meta-P0):** GAP-016 status 🟡 PARTIAL → 🟢 DONE. Final actions: (1) §2.9 business-gap-check audit ran with fixed grep scope (kiteclass-core + kitehub-branding) — 16/20 ✅, 2 ❌ tracked existing gaps (GAP-005 regenerate counter, GAP-011 ImageTemplate library), 1 ⚠️ Saga alternative pattern, 1 ⏭️ DB-dependent. (2) Skill `business-gap-check.md` §2.9 updated: grep scope `kitehub-branding` → `kiteclass-core` + class renames `BrandingAnalyzer→AnalyzerService`/`BrandingPlanner→PlannerService` + module-location note. (3) GAP-016 Findings table flipped — 7 items closed by GAP-229 (PRs #561/#562); 6 stale items split out as **GAP-234** (architecture doc + 4 PUML diagrams + database-design.md + docker-platform-architecture.md drift, P2 deferred). Per memory `feedback_audit_grep_scope.md`: skill grep scope correction is the kind of force-multiplier fix that prevents future false-positives like GAP-107. Counts: **87 OPEN → 87 OPEN** (-GAP-016 +GAP-234 net 0). Wave 7 Meta-P0 queue narrowed: GAP-005 + GAP-011 + GAP-014 remain.

**2026-04-26 (Wave session-followups — 3 parallel agents):** Closed loose ends từ session 5-PR. (1) **Skill bug fix:** `session-docs-check/scripts/check-docs.sh` Rule 8 logic — chỉ flag truly-new folders qua `git ls-tree -r --name-only $BASE_REF -- $dir` check, không flag pre-existing folders nhận file mới (3 audit dirs WARN false-positive). Retest cumulative session: 4 PASS / 0 WARN / 0 FAIL (was 5/3/0). (2) **GAP-229 closed:** Status 🟡 PARTIAL → 🟢 DONE. All 6 AC ticked. Phase 2/3 closure log entry references PRs #561/#562 + cite specific files (3 user guides + 3 instance-provisioning docs + 05-guides README index). Counts: **88 OPEN → 87 OPEN** (-GAP-229). (3) **3 audit gaps filed — GAP-231 (payment-invoice), GAP-232 (attendance), GAP-233 (student-enrollment):** API contract drift cluster from post-wave-7 audit. **Audit calibration finding** (per `feedback_audit_calibration.md`): audit Agent C over-stated severity — claimed "13 domains zero-doc" with "0 documented" cells; verification shows all 3 worst domains (payment-invoice, attendance, student-enrollment) **have existing api-contract.md files** with substantial content. Real drift is depth (auth blocks, error matrices, DTO schemas, UC linkage, side-effect cross-refs) NOT greenfield. GAP-231 also re-counted endpoints: audit said 23, real = 32 across 5 controllers. Gaps re-framed as "drift completion" — keeping P0 priority but scope reduced from "write from scratch" to "fill in gaps". Counts: **87 OPEN → 90 OPEN** (+GAP-231/232/233). **Wave validation:** 3 parallel agents returned in ~3 min wall-clock vs estimated ~30 min serial — pattern from `feedback_wave_plan_before_serial_prs.md` working as designed. Parent owned ROADMAP per `feedback_parallel_agent_strategy.md` rule #2 → zero merge conflicts despite 3 agents.

**2026-04-26 (GAP-229 Phase 1 SHIPPED — AI Branding business docs v2 sync):** 3 docs in `documents/01-business/kitehub/ai-branding/` synced from real `kiteclass-core` Waves 2-4 implementation. `rules.md` +24 v2 rules across 6 areas (BR-RES/LIFE/QUALITY/APRV/WIZARD/MOD/PKG) each with code reference + config key. `use-cases.md` +6 UCs (UC-AIB-07..12) sourced from real Controllers + Services. `api-contract.md` +12 v2 endpoints (8 lifecycle + 2 branding package + 1 internal webhook + 4 TBD approval) with schemas from real `InstanceController` + `BrandingPackageController` + `PublicBrandingController` + `InternalWebhookController`. Per memory `feedback_search_all_modules_before_missing_claim.md`: documented REAL impl not aspiration; gated features (tier counter, ENTERPRISE Advanced Mode) noted as scaffold/TBD where code lacks. Phase 2 (3 user guides) + Phase 3 (instance-provisioning verify) deferred to separate sessions. GAP-229 status 🔵 OPEN → 🟡 PARTIAL. No counts change (still PARTIAL).

**2026-04-26 (GAP-222c SHIPPED — Option B generalize migration_outbox → subscription_outbox):** Final outbox-cluster migration. V22 Flyway: rename `migration_outbox` → `subscription_outbox`, drop FK + drop NOT NULL on `instance_id`. Renamed `MigrationOutboxEvent`/`Repository`/`MigrationEventEmitter` → `Subscription*` (emitter now `@Component`); added `emit(UUID, ...)` overload for nullable instance_id (email pre-provisioning case). `InstancePurgeService` (line 188) + `EmailServiceClient.publishToQueue` (line 588) migrated to §3.5.1 Exception A: outbox.emit first + try/catch best-effort `rabbitTemplate.convertAndSend` with marker comment "outbox is the reliability net". `EmailServiceClient` class-level `@Transactional` to ensure outbox + EmailSentLog save share txn (private dispatchEmail couldn't be self-call proxied). `ObjectMapper` injected (Spring Boot's auto-configured one with JSR-310). `TrialToPaidService` constructor refactored to take emitter bean. 6 new tests (3 InstancePurgeService Exception A + 3 EmailServiceClient Exception A) — **355/355 kitehub-subscription tests green**. GAP-222c status 🔵 OPEN → 🟢 DONE. Counts: **89 OPEN → 88 OPEN** (-GAP-222c).

**2026-04-26 (GAP-222b SHIPPED — ParentInvitationServiceImpl outbox migration):** kiteclass-core internal migration applied as §3.5.1 Exception A (matches BrandingEventPublisher precedent in same module): outbox.enqueue first + existing fast-path try/catch with marker comment. Constructor expanded with OutboxEventWriter + ObjectMapper; test ObjectMapper uses findAndRegisterModules() for JavaTimeModule (matches Spring Boot default — initial omission caused Instant serialization failure in test, fixed). 13/13 ParentInvitationServiceTest + **1117/1117 full kiteclass-core suite green**. GAP-222b status 🔵 OPEN → 🟢 DONE. Counts: **90 OPEN → 89 OPEN** (-GAP-222b).

**2026-04-26 (GAP-230 SHIPPED — Exception D rule + AIQueueDispatcher marker):** Rule extension landed `design-patterns.md` v1.2.0 → v1.3.0: §3.5.1 Exception D (dedicated dispatcher infrastructure) with 4-criterion test (naming + caller-persists-first + no-business-logic + marker phrase) + AIQueueDispatcher example. Marker applied to `AIQueueDispatcher` class-level javadoc. Triage of 5 audit Cat 5 hits: 1 D (AIQueueDispatcher), 2 A (BrandingEventPublisher already documented + BrandingJobService closed by GAP-222a Phase 2), 2 still need Exception A migration (EmailServiceClient + InstancePurgeService) — re-scoped under existing **GAP-222c** which was UNBLOCKED + reduced from L (4 services) → M (2 services). GAP-230 status 🔵 OPEN → 🟢 DONE same day. Counts: **90 OPEN → 90 OPEN** (-GAP-230 net 0; GAP-222c stays open with revised scope).

**2026-04-26 (GAP-222a Phase 2 SHIPPED — kitehub-branding domain outbox):** Per ADR-021 (PROPOSED #556) per-module pattern executed: created `BrandingOutboxEvent` + `BrandingOutboxRepository` + `BrandingEventEmitter` in `kitehub-branding/outbox/`; Flyway `V21__create_branding_outbox.sql` in `kitehub-subscription`; `BrandingJobService.createJob()` migrated to outbox-first + best-effort fast-path (Exception A pattern). New `BrandingEventEmitterTest` (4 cases) + updated `BrandingJobServiceTest`. Full module suite **153/153 green**. `design-patterns.md` v1.1.0 → v1.2.0 (§3.5.1 default-rule paragraph cites both per-module precedents). AIQueueDispatcher case NOT migrated — class is dedicated dispatcher infrastructure, not domain-event source; needs §3.5.1 Exception D → filed **GAP-230** (Meta-P1, rule clarification). GAP-222a status 🟡 PARTIAL → 🟢 DONE. Counts: **90 OPEN → 90 OPEN** (-GAP-222a +GAP-230 = net 0).

**2026-04-26 (Wave 7 queue staleness fix — docs-only):** State-check trước khi pick Wave 7 next-action phát hiện priority queue line 4 stale — `PowerPoint format (Feature-P0)` đã DONE từ Wave 5 (GAP-047 closed Sub-PR 5.6b #532, 2026-04-25; PowerPoint deferred per Q6 scope-lock với Canva/Slides alternative justification). Removed stale entry; added GAP-229 (BL-P1 docs sync) per matrix-strict ordering; updated GAP-006 status BLOCKED → unblocked (Sub-PR 223.1 shipped 2026-04-26 #553/#554 means GAP-006 = Sub-PR 223.2 actionable). Pattern: lặp lại memory `feedback_gap_state_check_required.md` — ROADMAP cần state-check trước khi consume queue. No gap counts change (cleanup only).

**2026-04-26 (Sub-PR 223.1 CORRECTION — module path fix):** GAP-016 verification sweep phát hiện audit-gate.py rule patterns + skill SKILL.md + baseline audit references trong PR #553 đều dùng `kitehub-branding/` paths với class names từ architecture doc (BrandingPlanner/BrandingAnalyzer/BrandingExecutor) — KHÔNG match implementation thực tế. V2 code đã ship Waves 2-4 nhưng landed trong **`kiteclass/kiteclass-core/`** (NOT `kitehub-branding/`) với real names: `AnalyzerService`/`PlannerService`/`PlanExecutor`. Correction PR fixes: (1) audit-gate.py patterns + class names corrected, (2) skill SKILL.md updated, (3) baseline audit references updated (score 62/100 stays — calibration đúng), (4) GAP-225 cluster cells corrected, (5) GAP-016 status PLANNED → 🟡 PARTIAL với Findings table verified-real. Filed GAP-229 (P1 biz-logic) cho business docs v2 sync + 3 missing user guides — Living Documents rule violation từ Waves 2-4. Counts: **89 OPEN → 90 OPEN** (+GAP-229).

**2026-04-26 (Sub-PR 223.1 SHIPPED, Wave 7 governance scaffold landed):** GAP-223 Option C executed — single PR delivered: (1) skill `quality/ai-branding-quality-gate/` (manual checklist 5 sections × 20 = /100), (2) baseline audit `2026-04-26-baseline.md` 62/100 ⚠️ BASELINE, (3) `audit-gate.py` AUDIT_RULES + AUDIT_DIRS extended cho `kitehub-branding/` Java patterns, (4) `ai-branding-guidelines.md` v1.1.0 với §11.4 Migration test checklist + frontmatter backfill, (5) `output-review-mandate.md` v1.0.2 matrix line 75 re-sync, (6) 3 follow-up gaps GAP-226/227/228 cho real WCAG/vrg/ML (Wave 8+ scope). GAP-223 status 🔵 OPEN → 🟡 PARTIAL (Sub-PR 223.2 = GAP-006 Gemma 4 9B migration unblocked, queued separate session). Counts: **86 OPEN → 89 OPEN** (+GAP-226/227/228).

**2026-04-26 (afternoon, cross-gap audit triggered by GAP-223 Wave 7 kickoff):** Explore agent quét 220+ gap files + matrix + audit-gate.py + skill catalog → phát hiện **systemic scaffold-as-DONE governance debt**. 5 gaps (GAP-008/009/012/015/018) shipped Waves 2-4 marked DONE despite explicit deferred items + missing audit-gate rules + missing dedicated skills + matrix mismatches. **Filed GAP-225** (umbrella, 🟠 P1 meta, docs-only this PR) capturing pattern + 3 cluster fix plan (C1 AI agent, C2 Saga, C3 AI branding — last covered by GAP-223). `output-review-mandate.md` line 75 synced from "PLANNED" → "PARTIAL". 5 affected gap files cross-linked to GAP-225 in their Log sections (Status preserved DONE for audit trail). User decision: docs-only truth-up, không Wave 7 commitment. Phase 2-4 implementation deferred until scheduled. Counts: **84 OPEN → 86 OPEN** (+GAP-224 collector regex, +GAP-225 umbrella).

**2026-04-24 update:** ROADMAP coverage refresh — prior state had 141/186 gaps referenced (24% missing). This refresh brings coverage to 100% by adding Epic 15 (Vietnam K-12 Education, 14 gaps), appending 9 observability/ops gaps to Epic 6, 5 frontend P2 gaps to Epic 13, and 8 meta/CI gaps to Epic 14. Accurate counts now: **81/186 gaps DONE (44%)**, 84 OPEN, 14 PARTIAL/PLANNED, 7 IN_PROGRESS. Also: CI history policy tightened via PR #471 (soft cap 500→50, hard cap 1000→100, feature-branch failure age 7d→1d) and executed cleanup went 538→52 runs. Session skill fixes GAP-206 (wave+blockers accuracy, PR #468) + GAP-207 (Vietnamese output per CLAUDE.md, PR #470) CLOSED. GAP-205 CI retention automation CLOSED.

**2026-04-24 (later, Wave 5 kickoff):** PR #474 Sub-PR 5.0 opened; Core Service CI surfaced pre-existing flaky test `DefaultUrlAllowlistValidatorTest.allowsTenantListedHost` — `api.partner.com` resolving to `::1` on WSL2 + CI runners triggers validator's DNS-rebind guard. Confirmed on `main` with no Sub-PR 5.0 changes. **Filed GAP-212 (P1)** — test-only fix using RFC-2606 `.invalid` domain; blocks PR #474 merge and every future Core CI run. Counts: **82 OPEN → 83 OPEN** (+GAP-212).

**2026-04-24 (Wave 5 generator trio SHIPPED):** Sub-PRs 5.0 (#474 foundation + ADR-019), 5.1 (#476 PDF + invoice), 5.2 (#477 Excel + attendance), 5.3 (#478 Word + teacher contract) all merged to main same day. **GAP-047 status 🔵 OPEN → 🟡 PARTIAL.** PowerPoint deferred to Wave 6 per scope-lock (PR #473 Q6). Remaining before GAP-047 closes 🟢 DONE: Sub-PR 5.5 branding integration + HTTP endpoints, Sub-PR 5.6 wave completion. Counts: **84 OPEN → 84 OPEN, 14 → 15 PARTIAL** (GAP-047 reclassified). Recommend continuing Wave 5 (Sub-PR 5.5 next) before pivoting to GAP-046 or Wave 10.

**2026-04-24 (afternoon, Dependabot full-expansion):** PR #515 landed 1-PR-per-service Dependabot config (after PR #486 full-groups expansion produced 28 PRs, all closed). Fresh run created 4 all-deps group PRs; 2 failed with Spring Cloud BOM resolution error on Boot bumps (kiteclass-gateway #517, kitehub #518 which touches kitehub-gateway pom). **Filed GAP-213 (P1)** — pom BOM fix needed before Dependabot can ship Spring-touching PRs for these 2 services. Boot 3.5.13 → 3.5.14 for 7 kitehub poms + 1 gateway pom blocked until GAP-213 closed. Counts: **83 OPEN → 84 OPEN** (+GAP-213).

**2026-04-23 update:** Continuation of 2026-04-21 security session. Enabled Dependabot via `gh api PUT .../vulnerability-alerts` after GAP-202 skill exposed it was disabled. **Surfaced 89 npm alerts** (8 CRITICAL + 32 HIGH + 45 medium + 4 low). Initial triage incorrectly flagged 8 CRITICAL as false-positive (shallow jq query on only first vulnerable range); corrected analysis shows **all 8 CRITICAL are real** on `next@15.1.6` (GHSA-9qr9 fix 15.1.9, GHSA-f82v fix 15.2.3). Bump attempts (15.1.11, 15.3.9, 15.5.15) all broke `/pricing` + `/blog/[slug]` prerender via `Array.toJSON` regression in next 15.1.7+. Filed **GAP-204** P0 with Stage A (docs) + Stage B (RSC compat investigation) + Stage C (bump + close CRITICAL) + Stage D (triage remaining HIGH) + Stage E (re-enable auto-security-fixes). `/repo-status` reports **BLACK** — skill working correctly.

**2026-04-21 update:** During post-Wave-9.5 `/repo-status` session, user flagged skill missing GitHub Security checks. `gh api` probe surfaced **3 HIGH CVEs** + 4 medium on main (Dependabot silently disabled). Filed **GAP-202** (meta — skill blindspot, Meta-P1) + **GAP-203** (security — CVE fixes, BL-P0). Both re-open previously-closed Epic 5 (Security) + Epic 12 (Process). Priority: GAP-202 first per meta-gap rule, GAP-203 second (skill fix enables continuous detection; CVE fix closes current exposure). PRs #423/#424/#453/#454 shipped 2026-04-21. CVEs auto-closed by Trivy post-merge. Case study: `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`.

---

## 🎯 Previous Status Snapshot (2026-04-20)

**Progress:** 81/186 gaps CLOSED (44%) — recount 2026-04-24 after coverage sync; prior "73/178" was stale. Waves 1-4 + **Wave 8b SHIPPED** 2026-04-20 (6 parallel agents, PRs #401-#406) + **Wave 9 SHIPPED** 2026-04-21 (6 parallel agents, PRs #408-#413) + **Wave 9.5 SHIPPED** 2026-04-21 (4 parallel agents, PRs #415-#418: GAP-192 Phase 4b-i backend completeness with 45 new tests, GAP-132 fan-out → DONE, GAP-134 expand → DONE; GAP-043 fan-out attempted but 4/5 reverted due to Redis+Jackson typing regression — only BrandingPackage proxy retains sync=true). **Audit catch-up Part A — 5/5 COMPLETE** 2026-04-19. **Part B top-5 priorities — 5/5 SHIPPED** 2026-04-20 (PRs #371–#375) closing 9 gaps. **Re-audit validated 2026-04-20:** business-logic 65→**72** (+7), performance 58→**64** (+6). **Master plan merged PR #382** covers 92 open gaps across 12 waves (~2-3 months). **6 meta gaps tracked** (GAP-170–175) from output-review-mandate §4 VIOLATIONS → Wave 8b. **Part C Sprint 0 CLOSED** 2026-04-20 — GAP-149 (audit grep scope fix) closed, 5 audit skills hardened against multi-module false positives. **Business-logic tier added to priority matrix** 2026-04-20 (`meta-gap-priority.md` §3) — 3 new gaps GAP-150/151/152 track BRD completion + persona AC + persona review execution. **12 new gaps filed 2026-04-20 (GAP-190..201)** from action-1 + simulation; **GAP-196 dropped same-day** (user decision — 9router ADR not effective); **GAP-190 + GAP-197 scope-revised** to 🟡 PARTIAL after state-check found existing infrastructure (sitemap/robots/OG/JsonLd/blog MDX + enhanced-attendance-calendar PR 3.8.1). Net: 11 active new gaps — 1 BL-P0 (GAP-192), 3 BL-P1 (GAP-190/191/200), 4 Meta-P1 (GAP-193/194/199/201), 2 Meta-P2 (GAP-195/198), 1 Feature-P2 (GAP-197). Quality audit baseline 77/100 pending next refresh (due 2026-04-26).

**Priority order (updated 2026-04-20):** Meta-P0 → **Business-Logic-P0** → Feature-P0 → Meta-P1 → Business-Logic-P1 → Feature-P1 → ... Reference `.claude/rules/meta-gap-priority.md` §3 for tier definitions + tie-breakers.

> **Recently closed (do NOT count as blockers):** GAP-046 Wave 6 2026-04-26 (audit 82/100 + ADR-020); GAP-047 Wave 5 2026-04-25 (#532 doc-gen trio).

**GA Blockers remaining: 5 — ordered per `meta-gap-priority.md` (meta before feature within P0).**

| # | Gap | Title | Type | Status | Effort |
|:-:|-----|-------|:----:|:------:|:------:|
| 1 | **GAP-223** | AI Branding migration verification governance — Sub-PR 223.1 SHIPPED 2026-04-26 (skill + audit-gate rule + §11.4 + baseline 62/100); Sub-PR 223.2 = GAP-006 ⏸ DEFERRED 2026-04-28 (Ollama + Docker stack required) | 🔴 P0 Meta (governance) | 🟡 PARTIAL | Sub-PR 223.2 ⏸ DEFERRED |
| 2 | ~~GAP-222a~~ | ~~Extract Outbox infra to shared lib~~ — superseded by ADR-021 per-module pattern; closed via GAP-222a Phase 2 + GAP-222b + GAP-222c (all DONE 2026-04-26) | 🟠 Meta (infra) | ✅ DONE | — |
| 3 | **GAP-016** | Living docs impact scope (3-layer sweep) | 🔴 Meta (docs contract) | 🟡 PLANNED | S |
| 4 | GAP-011 | Template library curation (30 templates) | Feature | 🟡 PLANNED | L |
| 5 | GAP-014 | Wave mock plan include AI branding | Feature | 🟡 PLANNED | M |
| 6 | GAP-005 | AI queue fair scheduling (Phase 2) | Feature | 🟡 IN_PROGRESS | M |

> **Priority rule:** Meta-gaps (skills/rules/workflow) go first at each P-level — 1 broken skill/rule affects every future PR, so force multiplier first. Ref `.claude/rules/meta-gap-priority.md`.

**Epics fully closed:** Epic 5 (Security/Compliance), Epic 11 (SaaS Lifecycle Hardening), Epic 12 (Process/DevOps Maturity), Epic 13 (Frontend Quality — 4/5).

**Next recommended wave:** Wave Meta-Gov 1 **CLOSED 2026-04-28** (7/8 gaps DONE; GAP-256 GATED). Next priority queue (per `meta-gap-priority.md` Meta > Feature):

> ⏸ **AI Branding cluster DEFERRED 2026-04-28** — GAP-223 Sub-PR 223.2 + GAP-006 are blocked on local Ollama daemon + Docker stack (WSL2 CPU-only infeasible for Gemma 4 9B A/B test per `feedback_gap006_infra_blocker.md`). Will resume when: (1) Ollama running with `gemma4:9b` + `nqduc/mixsura:mixsura-q6_K`, (2) `./kitehub/scripts/up.sh` green, (3) sufficient compute for 9B inference. See GAP-006 + GAP-223 Log entries 2026-04-28.

**STRATEGY SHIFT 2026-04-28**: Linear queue replaced by **wave-pack clusters** (5-8 related gaps per wave, 3-5 parallel agents). Demo wave (Observability) shipped 3 gaps in ~75 min — projected 5x cadence improvement. Below: cluster pipeline.

### Active wave queue (clustered)

| # | Wave / Cluster | Gaps | Priority | Status |
|:-:|----------------|------|:--------:|:------:|
| 1 | ~~Observability — Wave 1~~ | GAP-121 (P1) + GAP-143 (P1) + GAP-144 (P0 PARTIAL) | mixed | ✅ SHIPPED 2026-04-28 |
| 2 | ~~Observability — Wave 2~~ | GAP-122 (DONE 2026-04-28 single-gap parallel wave) + GAP-144 mock-fire backfill (deferred, infra-blocked) | P1 | ✅ SHIPPED 2026-04-28 (GAP-122 only); GAP-144 mock-fire still infra-blocked |
| 3 | ~~DR/Backup cluster~~ | GAP-117 (🟡 PARTIAL, Phase 3 → GAP-257) + GAP-118 (🟢 DONE) + GAP-119 (🟢 DONE) | P0+P1 | ✅ SHIPPED 2026-04-28 |
| 4 | **KiteHub admin cluster** | GAP-066 + GAP-067 + GAP-068 (P1, all KH services) | P1 | 🟡 SLICED 2026-04-28 — oversized per `cluster-pattern.md`; if revived, decompose into Phase-1 sub-gaps (~3h each) → wave-pack the 3 sub-gaps. See `documents/03-planning/plans/pr-next-session-single-gap-handoff.md` §"Cluster 4 deferred work" |
| 5 | ~~Business correctness cluster — Wave Phase 1~~ | GAP-049 (P0 PARTIAL — Phase 2 → GAP-156) + GAP-050 (P0 PARTIAL — exec in GAP-152) + GAP-150 (P1 DONE — Phase 2 → GAP-155) | P0+P1 | ✅ SHIPPED 2026-04-29 — `documents/03-planning/waves/wave-2026-04-29-business-correctness.md` |
| 6 | ~~Meta-Gov 2 cluster~~ | GAP-245 (P1 PARTIAL — Phase 2 → GAP-261) + GAP-225 (P1 DONE) + GAP-224 (P3 DONE) + GAP-202/206/207 status sync DONE | P1+P3 | ✅ SHIPPED 2026-04-29 — `documents/03-planning/waves/wave-2026-04-29-meta-gov-2.md` |
| 7 | ~~Meta Phase-2 Cleanup cluster~~ | GAP-193 P2 (DONE) + GAP-194 P2 (DONE) + GAP-195 P2a (PARTIAL — Phase 2b → GAP-262) | P1+P2 | ✅ SHIPPED 2026-04-29 — `documents/03-planning/waves/wave-2026-04-29-meta-phase2-cleanup.md` (~30 min wall-clock, 6th wave-pack) |
| 8 | **Parent/import cluster** | GAP-052 (P0 PARTIAL) + GAP-063 (P1) + GAP-137 (P0) + GAP-139 (P1) | P0+P1 | 🔵 OPEN |
| 9 | **K-12 features wave** | GAP-055 (P1, Phase 1 Tasks 3-10) + GAP-056 (P1) + GAP-057 (P1) | P1 | 🟡 IN_PROGRESS (GAP-055 Tasks 0-2 DONE) |
| 10 | **Logging migration** (separate track) | GAP-114 (P0) + GAP-115 (P1) | P0+P1 | 🔵 OPEN — multi-PR scope per `logs-format-standard.md` migration phases |
| 11 | ~~UI Kits Round 2 wave~~ + Wave 1.5/1.6/1.7 add-ons | kiteclass-pro v2 + kiteclass-parent + 5 components + **kitehub-pro v2 + kiteclass-teacher + ai-branding-wizard-v2** + GAP-263 Phase 1 | P2 (HTML prototypes — Plan B route from Claude Design block) | ✅ SHIPPED 2026-04-29 — Wave 1: 5 PRs (#668-#672) + Wave 1.5/1.6/1.7 add-ons: 5 PRs (#673/#674/#675/#676/#677) — total 10 PRs, **76 screens, avg 110.5/128 (+51% vs R1 baseline 73/128)**. First wave-pack for non-gap-closing deliverable creation; first multi-add-on extension for scope-gap recovery (kitehub miss → Wave 1.5; teacher persona → Wave 1.6; Direction C deferred → Wave 1.7). |
| 12 | ~~Wave Review Process Improvement~~ + Option D Pages | GAP-263 (DONE) + GAP-264 (DONE) + GAP-265 (DONE) + Option D Pages deploy | P1 Meta (review process coverage gap) | ✅ SHIPPED 2026-04-29 — 4 PRs (#680 foundation + #682 Tier 2 skill + #681 Tier 3 hook/CI/lefthook + #683 Option D Pages+screenshots+README). All 3 GAP-263 phases verified post-merge. Triggered by user-flagged miss PR #678 closure → fix shipped via `incident-to-rule-pipeline.md` 5-stage. Bonus deliverable: GitHub Pages live demo (https://victoraurelius.github.io/2026-Kite-Class-Platform/) + 7 hero screenshots + README showcase section (visitor-friendly). |
| 13 | ~~UI Kits Round 3 wave~~ | kiteclass-student kit + kitehub-admin kit + 7 components (G1/G3/G4/G8/G9/G10/G11) | P2 (HTML prototypes — Track 1 extension) | ✅ SHIPPED 2026-04-29 — 5 PRs (#699 foundation + #700/#703/#702/#701 agents) — total 4 parallel buckets, **76 demo states/screens, avg 109.7/128** (target ≥105 ✓, 0.8 pt below R2 110.5 — within band). Wall-clock ~90 min vs 150 estimated (-40%). Agent A kiteclass-student **avg 116/128 ⭐⭐ (HIGHEST kit Round 3)**, beat R2's parent kit 114. **Persona × Direction dossier matrix officially complete** (only `kitehub-story` Direction A marketing remaining, deliberately deferred per Decision 3). 26th consecutive 0-clarification wave. Track 2 production port (GAP-266..273) now user-acceptance gated per `gap-done-discipline.md` §3. |
| 14 | **UI Kits Track 2 production port** (multi-wave) | **15 gaps total** — GAP-273 (BLOCKING, components) + GAP-266..272 (7 R2/R3 kit ports) + **GAP-274..280 (7 audit-driven follow-ups)** | P2 (UX growth) + P1 (GAP-277 error pages hardening) | 🔵 OPEN 2026-04-29 — User accepted Round 3 quality. Coverage audit (Cluster 15) shipped 2026-04-29 → 7 follow-up gaps filed: GAP-274 (KC public marketing), GAP-275 (KH public+blog), GAP-276 (auth flows), GAP-277 (error pages, **P1**), GAP-278 (KH platform admin — distinct from kitehub-admin K-12), GAP-279 (modals D1..D10 catalog), GAP-280 (onboarding wizard). **Total Track 2 estimate revised 10-15 → 15-20 weeks.** Recommended sequence: GAP-273 FIRST → GAP-279+277 (cross-cutting) → GAP-276 (auth) → GAP-269+272 (highest-quality kits 116/115) → 266+270 → 267+268 → 271+280 → 274+275+278. |
| 15 | ~~UI Coverage Audit wave~~ | (audit-only, spawned GAP-274..280) | Meta — evidence preservation per `output-review-mandate.md` §1 | ✅ SHIPPED 2026-04-29 — 4 PRs (#707 foundation + #709 KC enumeration + #708 KH enumeration + closure). 201 production UI artifacts catalogued (40 KC + 24 KH pages + 14 modal sites + 108 components + 15 error/layout files). Coverage finding: ✅ 19% explicit / ⚠️ 37% implicit / ❌ **32% missing**. Evidence: `documents/04-quality/audits/ui-review/2026-04-29-frontend-ui-coverage-audit.md` + dossier `03/12/14/15` updated. 7 follow-up gaps filed referencing audit. 28th consecutive 0-clarification wave. First wave to use `agent-background-spawn-default.md` v1.0.0 rule (PR #705) — 2 agents background, parent stayed responsive. |

### Single-track items (not clustered)

- **GAP-256** (Meta-P2 GATED until ~2026-05-05) — README read-first rule
- **Wave 9 — large-skill restructure** (Meta-P3) — split `workflow/development-workflow.md` + `workflow/priority-pr-planning.md`
- **GAP-223 Sub-PR 223.2** ⏸ DEFERRED — see AI Branding banner
- **GAP-006** ⏸ DEFERRED — see AI Branding banner

### Day 2 framework deliverable — ✅ SHIPPED 2026-04-28 (PR #630)

- ✅ `quality/wave-pack-planner/SKILL.md` (133 lines) — cluster-then-spawn pattern, 5-step process, gotchas from Wave Obs
- ✅ 6 reference docs (`cluster-pattern`, `file-overlap-algorithm`, `agent-spawning-template`, `retrospective-checklist`, `wave-plan-template`, `background-loop-fleet`)
- ✅ 5 agent prompt templates (`docs-only`, `test-only`, `p3-cleanup`, `feature-tdd`, `wave-coordinator`) under `assets/agents/`
- ✅ `scripts/analyze-overlap.sh` (367 LOC, shellcheck-clean) — file overlap matrix + HARD/SOFT/None classification + exit-code gate
- ✅ `data/wave-history.jsonl` (seeded with Wave Obs entry; Wave Meta-Day-2 + Wave DR/Backup entries appended in foundation PRs)
- ✅ Background `/loop` fleet documented (doc-only per user Q1=A; auto-config deferred to user decision)
- 🟡 First real-world validation in progress: Wave DR/Backup (Cluster 3 above) consumes the skill end-to-end

**Earlier reference (Wave 7 + Wave 6 priorities now subsumed by above):**
- GAP-222a/b/c + GAP-230 SHIPPED 2026-04-26 ✅ (Outbox migration cluster fully closed)
- Wave 7-Perf SHIPPED 2026-04-27 (4 parallel agents — GAP-126/127/130/135)
- Wave 6 design pattern audit CLOSED 2026-04-26

---

## 1. Epic Taxonomy

186 gaps được group thành **15 epics** (updated 2026-04-24):

| Epic | Theme | Gaps | Priority |
|------|-------|------|:--------:|
| [E1](#epic-1-foundation-infrastructure) | Foundation Infrastructure | 5 | 🔴 MUST FIRST |
| [E2](#epic-2-core-ai-branding-pipeline) | Core AI Branding Pipeline | 6 | 🔴 CORE |
| [E3](#epic-3-ai-infrastructure) | AI Infrastructure (model + queue) | 5 | 🟠 SCALE |
| [E4](#epic-4-integration--delivery) | Integration & Delivery | 5 | 🟠 DEPLOY |
| [E5](#epic-5-security--compliance) | Security & Compliance | 6 | 🔴 NON-NEG |
| [E6](#epic-6-operations--scale) | Operations & Scale | 17 | 🟠 PRODUCTION |
| [E7](#epic-7-ux--conversion) | UX & Conversion | 9 | 🟠 GROWTH |
| [E8](#epic-8-admin--support) | Admin & Support | 7 | 🟡 INTERNAL |
| [E9](#epic-9-developer-experience) | Developer Experience | 3 | 🟡 FUTURE |
| [E10](#epic-10-cross-cutting--architecture) | Cross-cutting & Architecture | 5 | 🟡 CLEANUP |
| [E11](#epic-11-saas-lifecycle-hardening) | SaaS Lifecycle Hardening | 7 | 🔴 BLOCK GA |
| [E12](#epic-12-process--devops-maturity) | Process & DevOps Maturity | 11 | 🟠 PRODUCTION |
| [E13](#epic-13-frontend-quality) | Frontend Quality | 10 | 🟠 GROWTH |
| [E14](#epic-14-quality-governance) | Quality Governance | 35 | 🟡 INTERNAL |
| [E15](#epic-15-vietnam-k-12-education-features) | Vietnam K-12 Education Features | 14 | 🟠 DOMAIN |

---

## 2. Epics Detailed

### Epic 1: Foundation Infrastructure
**Goal:** Setup prerequisites cho AI Branding implementation.
**Why first:** Các epic khác depend vào này.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-011 | Template library curation plan + review standards | 🔴 P0 | L |
| GAP-014 🟡 | Wave mock plan include AI branding — planning v2-aligned 2026-04-26; impl split to GAP-235 | 🟡 PARTIAL | M |
| GAP-015 ✅ | Tenant provisioning auto-trigger (event-driven) — DONE Wave 3 | 🟢 DONE | M |
| GAP-016 ✅ | Living docs impact scope — DONE Wave 7 (2026-04-26, §2.9 audit 16/20 + skill scope fix; GAP-234 split out for diagram drift) | 🟢 DONE | S |
| GAP-046 ✅ | Design patterns applied systematically — DONE Wave 6 (2026-04-26, audit 82/100 Grade B + ADR-020) | 🟢 DONE | M |

**Dependencies:** None — starts immediately.

**Blocks:** Epic 2, Epic 4.

---

### Epic 2: Core AI Branding Pipeline
**Goal:** Build the actual AI branding feature (MVP).

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-007 ✅ | Resource classification pipeline — DONE Wave 2+3 | 🟢 DONE | L |
| GAP-008 ✅ | AI Agent workflow (analyzer/planner/executor) — DONE Wave 3 | 🟢 DONE | XL |
| GAP-009 ✅ | Instance provisioning lifecycle (6 states) — DONE Wave 2 | 🟢 DONE | L |
| GAP-013 ✅ | Guided branding wizard UX — DONE Wave 3 | 🟢 DONE | L |
| GAP-031 ✅ | Expand wizard inputs beyond logo — DONE Wave 3 | 🟢 DONE | M |
| GAP-004 | Template-based image composition (Canva-like) | 🟡 P2 | L |

**Dependencies:** Epic 1 (GAP-011 templates must exist).
**Blocks:** Epic 3, Epic 4.

---

### Epic 3: AI Infrastructure
**Goal:** Scale, reliability, model management.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-005 🟡 | AI queue fair scheduling — Phase 1 DONE 2026-04-18, Phase 2 open | 🟡 IN_PROGRESS | L |
| GAP-002 ✅ | Async pipeline for heavy AI tasks — DONE Wave 3 (2026-04-18) | 🟢 DONE | M |
| GAP-006 | Upgrade AI models — primary **Gemma 4 9B** (revised 2026-04-26 after candidate research vs Qwen 3.6/MixSura) + VN A/B test | 🟠 P1 | S-M (added pre-migration A/B step) |
| GAP-003 | Multi-tier image generation | 🟡 P2 | M |
| GAP-028 | AI model versioning & migration | 🟡 P2 | M |

**Dependencies:** Epic 2 (core pipeline).
**Blocks:** Epic 6 (ops).

---

### Epic 4: Integration & Delivery
**Goal:** Branding reaches users via multiple channels.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-010 ✅ | Branding package API + KiteClass integration — DONE Wave 3 | 🟢 DONE | M |
| GAP-021 ✅ | Branding propagation to email + services — DONE Wave 4 | 🟢 DONE | M |
| GAP-037 ✅ | Branded auth flows (verify, reset pwd) — DONE Wave 4 | 🟢 DONE | S |
| GAP-032 ✅ | Branded error pages (404/500) — DONE Wave 4 | 🟢 DONE | S |
| GAP-039 | Webhook reliability (retry, idempotency) | 🟠 P1 | M |

**Dependencies:** Epic 2 (branding data), Epic 1 (infrastructure).

---

### Epic 5: Security & Compliance
**Goal:** Non-negotiable legal/security requirements.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-018 ✅ | Content safety & compliance — DONE Wave 4 (MVP) | 🟢 DONE | L |
| GAP-041 ✅ | Security hardening (SVG XSS, SSRF, CSRF) — DONE Wave 4 | 🟢 DONE | M |
| GAP-042 ✅ | Legal/IP protection (DMCA workflow) — DONE Wave 4 | 🟢 DONE | M |
| GAP-012 ✅ | Automated instance quality review — DONE Wave 4 | 🟢 DONE | M |
| **GAP-203** | Fix 7 open CVEs in transitive Maven deps (3 HIGH) + enable Dependabot | 🔴 P0 | M |
| **GAP-204** | 89 npm alerts — 8 CRITICAL (next.js) + 32 HIGH + 45 medium + 4 low (5 stages A-E) | 🟡 P2 | XL |

**Dependencies:** Can parallelize với Epic 2. GAP-203 pairs with GAP-202 (detection skill fix). GAP-204 depends on GAP-202 (detection exposed scope) + compatibility work on JsonLd RSC serialization.
**Status:** 🟡 PARTIAL 2026-04-24 — All 8 CRITICAL + 32 HIGH + 39/45 medium CLOSED (92% resolved) via PRs #457/#458/#459/#460. Only 6 medium remain (axios 4 + follow-redirects 2 transitive) handled by Stage E auto-flow. Epic 5 **back to GREEN** (no CRITICAL/HIGH live on main). GAP-203 shipped 2026-04-21 (PR #424), GAP-202 shipped 2026-04-21 (PR #423/#453). Security session 2026-04-21 → 2026-04-24: total 8 PRs, 82/89 alerts closed.

---

### Epic 6: Operations & Scale
**Goal:** Production readiness.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-019 | AI observability & cost monitoring | 🟠 P1 | M |
| GAP-043 | Performance protection (cache stampede) | 🟠 P1 | M |
| GAP-030 | Disaster recovery for AI branding | 🟡 P2 | M |
| GAP-044 | Synthetic monitoring + feature flags | 🟡 P2 | M |
| GAP-024 | Asset lifecycle & storage cleanup | 🟡 P2 | S |

**Dependencies:** Epic 3 (need real traffic to monitor).

---

### Epic 7: UX & Conversion
**Goal:** User experience + revenue optimization.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-020 | Wizard state persistence | 🟠 P1 | S |
| GAP-017 | AI usage → billing integration | 🟠 P1 | M |
| GAP-026 | Trial/freemium AI mechanics | 🟠 P1 | M |
| GAP-036 | Tier upgrade UX (reveal, teaser) | 🟠 P1 | M |
| GAP-033 | Branding version history & rollback (user) | 🟡 IN_PROGRESS (Wave 4 partial — manual rollback done; auto + A/B deferred) | M |
| GAP-034 | Branding export pack (ZIP + PDF) | 🟡 P2 | M |
| GAP-025 | Mobile-first wizard UX | 🟡 P2 | M |

**Dependencies:** Epic 2, Epic 4.

---

### Epic 8: Admin & Support
**Goal:** Internal tools for operations team.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-023 | Admin moderation tools | 🟠 P1 | L |
| GAP-040 | Support impersonation & diagnostics | 🟠 P1 | M |
| GAP-022 | Template analytics & A/B | 🟡 P2 | M |
| GAP-029 | Quality gate calibration | 🟡 P2 | S |

**Dependencies:** Epic 5 (audit logs), Epic 6 (monitoring infra).

---

### Epic 9: Developer Experience
**Goal:** Open ecosystem for integrations.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-038 | Developer API docs + SDK libraries | 🟠 P1 | L |
| GAP-045 | Template marketplace (community) | 🟡 P2 | XL |

**Dependencies:** Epic 4 (stable APIs).
**Note:** Can defer until post-GA.

---

### Epic 10: Cross-cutting & Architecture
**Goal:** Platform-wide concerns, cleanup.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-047 🟢 | Document generation — Wave 5 DONE 2026-04-25 (#474/#476/#477/#478/#529/#530 + 5.6b). PPT deferred Wave 6. | 🔴 P0 | DONE |
| GAP-001 | kiteclass-gateway decision | 🟡 P2 | S |
| GAP-027 | Multi-brand per tenant (franchise) | 🟡 P2 | XL |
| GAP-035 | Wizard team collaboration | 🟡 P2 | L |
| GAP-221 | GitNexus pilot — code-intelligence MCP for multi-module audits | 🟡 P2 Meta | M (1-day pilot) |
| GAP-222 | Outbox bypass policy + migrate 5 direct-publish services | 🟡 PARTIAL | Policy + detector ✅ Sub-PR 6.4; migration → 222a/b/c |
| GAP-222a | Extract Outbox infra to shared lib (kitehub-* unblocker) | 🟠 P1 | S-M (~2-3h) — blocks 222c |
| GAP-222b | Migrate ParentInvitationServiceImpl to OutboxEventWriter (kiteclass-core internal, NOT blocked) | 🟠 P1 | S-M (~1-2h) |
| GAP-222c | Migrate 4 kitehub direct-publish sites (BrandingJobService + AIQueueDispatcher + InstancePurgeService + EmailServiceClient) | 🟠 P1 | L (~4-6h) — BLOCKED on 222a |

**Dependencies:** Mixed — document gen crosses all, multi-brand ties to all. GAP-221 is opt-in pilot (mirror RTK PR #531 pattern) — if ADOPT, becomes audit-skill force-multiplier; if REJECT, contained rollback.

---

## 3. Dependency Graph

```
                ┌──────────────────┐
                │ Epic 1 Foundation │ ←── MUST START FIRST
                └─────────┬────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
    ┌─────────────────┐    ┌──────────────────┐
    │  Epic 2 Core    │    │ Epic 5 Security  │ ←── PARALLEL
    │  Pipeline       │    │ & Compliance     │
    └────────┬────────┘    └─────────┬────────┘
             │                       │
   ┌─────────┼─────────┐             │
   ▼         ▼         ▼             │
 ┌────┐   ┌────┐    ┌────┐          │
 │ E3 │   │ E4 │    │ E7 │          │
 │ AI │   │Int.│    │ UX │          │
 │Inf.│   │    │    │    │          │
 └─┬──┘   └──┬─┘    └──┬─┘          │
   │         │          │            │
   └────┬────┴──────────┴────────────┘
        ▼
   ┌──────────────┐
   │ Epic 6 Ops   │ ←── Needs Epic 3, 4
   │ & Scale      │
   └──────┬───────┘
          │
          ▼
   ┌──────────────┐     ┌──────────────┐
   │ Epic 8 Admin │     │ Epic 9 DX    │
   │ & Support    │     │ (defer)      │
   └──────────────┘     └──────────────┘

   ┌──────────────┐
   │ Epic 10 X-cut│ ←── Can parallelize with most
   └──────────────┘
```

---

## 4. Sprint Roadmap

### 🚀 Sprint 0: Foundation (2 weeks) — MUST DO FIRST

**Goal:** Unblock all future work.
**Gaps:** GAP-011, 014, 016, 046
**Deliverables:**
- 30 initial templates curated
- Wave mock plan finalized
- Business docs updated
- Design pattern rules enforced

### 🚀 Sprint 1: MVP Pipeline (3 weeks)

**Goal:** End-to-end branding generation works.
**Gaps:** GAP-007, 008 (partial), 013, 031, 015
**Deliverables:**
- Resource router working
- Wizard with rich inputs
- Tenant created → auto-provision triggered
- First template-first branding generated

### 🚀 Sprint 2: Core Delivery (2 weeks)

**Goal:** Branding reaches users.
**Gaps:** GAP-009, 010, 032, 037
**Deliverables:**
- Lifecycle state machine
- Package API with ETag caching
- Branded error pages, auth flows
- Integration tests pass

### 🚀 Sprint 3: Security + Quality Gate (2 weeks) — PARALLEL with S1/S2

**Goal:** Non-negotiable compliance.
**Gaps:** GAP-018, 041, 012
**Deliverables:**
- Content moderation integrated
- Security hardening (SVG sanitize, SSRF protection, CSRF)
- Automated quality review in pipeline

### 🚀 Sprint 4: AI Scale (3 weeks)

**Goal:** Handle 100+ concurrent users.
**Gaps:** GAP-005, 002, 006 (Gemma 4 upgrade), 008 (finish)
**Deliverables:**
- RabbitMQ fair queue per tier
- Async image generation
- Gemma 4 in production

### 🚀 Sprint 5: UX Polish (2 weeks)

**Goal:** Conversion optimization.
**Gaps:** GAP-020, 021, 017, 026, 036
**Deliverables:**
- Wizard autosave/resume
- Email branding propagation
- Billing integration
- Trial mechanics + upgrade UX

### 🚀 Sprint 6: Ops Readiness (2 weeks)

**Goal:** Production launch ready.
**Gaps:** GAP-019, 043, 023, 042
**Deliverables:**
- Grafana dashboards
- Cache stampede protection
- Admin moderation UI
- Legal/IP framework

### 🚀 Sprint 7: Extended Features (flexible)

**Goal:** Enhancements based on feedback.
**Gaps:** Remaining P2 items (GAP-024, 025, 030, etc.)

### 🚀 Sprint 8+: Future / Nice-to-have

**Gaps:** GAP-027 (multi-brand), GAP-035 (collab), GAP-045 (marketplace), GAP-038 (SDK)

**Document Generation (GAP-047) — cross-cutting:**
Inject into Sprint 4-5 (invoice for billing, certificate for completion).

---

## 5. Critical Path

```
GAP-011 (templates) →
  GAP-007 (classification) →
    GAP-008 (agent) →
      GAP-009 (lifecycle) →
        GAP-010 (package API) →
          GAP-012 (quality gate) →
            [GA LAUNCH]
```

**Bottleneck:** GAP-011 (external dependency — designer) và GAP-008 (XL effort).

---

## 6. Effort Summary

| Size | Days | Gaps |
|------|------|------|
| S (Small, 1-3 days) | 3 | 5 gaps |
| M (Medium, 4-7 days) | 6 | 24 gaps |
| L (Large, 8-14 days) | 12 | 13 gaps |
| XL (Extra Large, 15+ days) | 20 | 5 gaps |

**Total estimated effort:** ~300 person-days (~6 months with 1 dev, ~2 months với 3 devs parallel).

---

## 7. Consolidation Opportunities

Some gaps có overlap, có thể merge:

| Candidates | Rationale |
|-----------|-----------|
| GAP-012 + GAP-029 | Both about quality review. Keep separate but implement together. |
| GAP-019 + GAP-044 | Both observability. Parts of same dashboard project. |
| GAP-032 + GAP-037 | Both branded pages (404/auth). Implement in 1 sprint together. |
| GAP-003 + GAP-028 | Both model versioning concerns. Unify when tackling. |
| GAP-018 + GAP-042 | Content safety + legal IP. Shared admin UI (GAP-023). |

**Don't merge** — track separately for clarity but implement in combined sprints.

---

## 8. Priority Tier Simplification

> **Superseded by refreshed tier table lower in file ("Updated Priority Tiers (103 gaps, refreshed 2026-04-18)").**
> Original Sprint 0-6 planning preserved here for historical context.

Original mapping (Wave 1 planning, pre-execution):

| Tier | Count (original plan) |
|------|-----------------------|
| 🟥 Block GA | 17 gaps |
| 🟨 Block GROWTH | 18 gaps |
| 🟦 Block SCALE | 12 gaps |

See refreshed counts + remaining-open list in §"Updated Priority Tiers" below.

---

## 9. Recommended Execution Model

**Team size scenarios:**

### Solo (1 dev, 6 months to GA)
- Strict sequential: Sprint 0 → 1 → 2 → 3 → 4 → 5 → 6
- Can't parallelize Epic 5 security
- Launch with 17 GA-blocker gaps closed

### Small team (3 devs, 2-3 months to GA)
- Parallel streams:
  - **Stream A (backend):** E1 → E2 → E3 → E6
  - **Stream B (frontend):** E1 → E2 wizard → E4 integration → E7 UX
  - **Stream C (security/ops):** E5 → E6 operations
- Launch with 25 gaps closed (GA + early growth)

### Full team (5+ devs, 1-2 months)
- All streams parallel
- Dedicated security team for Epic 5
- Launch with 30+ gaps closed

---

## 10. What To Do Right Now (Action Items)

1. **Approve roadmap** — user review this doc
2. **Assign Sprint 0 tasks** — GAP-011 (hire designer), GAP-014/016 (docs), GAP-046 (architecture)
3. **Set launch target date** — based on team size scenario
4. **Create tracking** — Linear/Jira/GitHub project với epics as milestones
5. **Cadence** — weekly sprint review, biweekly retro
6. **Dependency watchers** — alert when blocker resolved

---

## 11. Related Files

- `README.md` — flat index of all 47 gaps
- `_TEMPLATE.md` — template for new gaps
- Per-gap details: `GAP-XXX-*.md`
- AI Branding master design: `documents/02-architecture/ai-branding-v2-redesign.md`
- Design patterns: `documents/02-architecture/ai-branding-design-patterns.md`
- MiniMax skills analysis: `documents/04-quality/skills-gap-analysis-vs-minimax.md`

---

## 12. Progress Log

### Wave 2 — Data Model Foundation — 🟢 COMPLETE (2026-04-14)

7 sub-PRs merged sequentially:

| Sub-PR | PR | Gap | Status |
|--------|----|-----|--------|
| 2.1 ADRs (5 architectural decisions) | #271 | — | 🟢 |
| 2.2 Academic Year + Semester + Holiday | #273 | GAP-053 | 🟢 |
| 2.3 K-12 Multi-Subject Model | #275 | GAP-054 | 🟢 |
| 2.4 Role Hierarchy + Permissions | #276 | GAP-058 | 🟢 |
| 2.5 Instance Provisioning Lifecycle | #277 | GAP-009 | 🟢 |
| 2.6 Resource Classification Pipeline | #278 | GAP-007 | 🟢 |
| 2.7 Integration + Wave Completion | (this PR) | — | 🟢 |

**Wave 2 Gaps closed:** GAP-053, GAP-054, GAP-058, GAP-009, GAP-007

Deferred items from Wave 2 all landed in Wave 3: REST controllers (3.4), outbox foundation (3.1), concrete resource handlers (3.3), MinIO layout (3.3), internal webhooks (3.4).

### Wave 3 — AI Branding Core Pipeline — 🟢 COMPLETE (2026-04-14)

8 sub-PRs merged sequentially:

| Sub-PR | PR | Gaps addressed |
|--------|----|----|
| 3.1 ADRs (006-009) + Transactional Outbox foundation | #284 | — |
| 3.2 AI Provider adapter + Resilience4j | #285 | — |
| 3.3 Resource Handlers + MinIO storage layout | #286 | GAP-007 (completed) |
| 3.4 REST + Package API + webhook | #287 | GAP-010 ✅ |
| 3.5 AI Agent workflow + GAP-070 rebrand approval | #288 | GAP-008 ✅ GAP-070 ✅ |
| 3.6 Tenant Provisioning Saga | #289 | GAP-015 ✅ |
| 3.7 Guided Wizard UX | #290 | GAP-013 ✅ GAP-031 ✅ GAP-069 ✅ |
| 3.8 Integration + Wave Completion | (this PR) | 🟢 all closed |

**Wave 3 Gaps closed:** GAP-007 (full), GAP-008, GAP-010, GAP-013, GAP-015, GAP-031, GAP-069, GAP-070

Patterns landed: Outbox, Adapter, Strategy, Decorator, Command, Composite, Saga, State Pattern (×2), Builder, Proxy, Optimistic Lock, XState-style FSM (FE reducer).

Deferred to follow-up PRs / later waves (see `03-planning/wave-03-ai-branding-core.md` §Deferred): RabbitMQ consumer wiring, async generate Steps, real Ollama HTTP, REST for rebrand-approvals, Playwright E2E, SSE live progress.

### Wave 4 — Security & Compliance — 🟢 COMPLETE (2026-04-14, parallel-agent)

**First wave at this repo using parallel-agent execution** (worktree-isolated). 6 sub-PRs:

| Sub-PR | PR | Mode | Gaps addressed |
|--------|----|------|----------------|
| 4.0 Foundation + ADRs 010-013 | #294 | serialized (lead) | — |
| 4.1 Content Moderation | #297 | parallel agent #1 | GAP-018 ✅ |
| 4.2 Security Hardening (SVG/SSRF/CSRF) | #296 | parallel agent #2 | GAP-041 ✅ |
| 4.3 Legal/IP (DMCA + trademark) | #295 | parallel agent #3 | GAP-042 ✅ |
| 4.4 GDPR Deletion + retention | #298 | parallel agent #4 | GAP-073 ✅ |
| 4.5 Quality Gate | #299 | serialized (depends on 4.1) | GAP-012 ✅ |
| 4.6 Integration + Wave Completion | (this PR) | serialized | 🟢 all closed |

**Wave 4 Gaps closed:** GAP-012, GAP-018, GAP-041, GAP-042, GAP-073

Wall-clock vs serial: 4 middle sub-PRs took ~20min agent work + ~90min human sequencing vs estimated ~5 days serial. 3 application.yml conflicts during sequencing (resolved each time). 1 CI failure (CSRF test-profile secret) — trivially fixed.

Patterns landed: AuditLog, State Pattern (×3 new — Moderation, DMCA, Deletion), Strategy (Quality checks ×5), Adapter (CSRF), Saga (DMCA workflow), Decorator/Sanitizer (SVG XSS), Validator (URL allowlist).

Deferred (see `03-planning/wave-04-security-compliance.md` §Deferred): real ML NSFW classifier, USPTO API, MinIO streaming export, scheduled expiry job, real contrast/screenshot/URL-ping checks, KiteHub admin UI hookups (slated for Wave 8).

**Next Wave:** Wave 5 K-12 Critical Features (unblocked from Wave 2) OR Wave 6 Ops Readiness OR quality-audit refresh.

---

## NEW EPICS (added 2026-04-16)

### Epic 11: SaaS Lifecycle Hardening
**Goal:** Business logic cho subscription/trial/retention THẬT SỰ hoạt động đúng.
**Why:** Deep audit phát hiện rules có nhưng code thiếu enforcement.

| Gap | Title | Priority | Effort | Dependency |
|-----|-------|:--------:|:------:|:----------:|
| GAP-092 | Re-trial prevention (TR-07 not in code) | 🔴 P0 | S | — |
| GAP-093 | Database backup only logs (not functional) | 🟢 DONE | L | — |
| GAP-091 | Email idempotency guard (2/13 types) | 🟢 DONE | S | — |
| GAP-094 | Hard delete not implemented | 🟢 DONE | M | GAP-093 |
| GAP-095 | Email failure retry mechanism | 🟢 DONE | M | GAP-097 |
| GAP-096 | Email admin controls + monitoring dashboard | 🟢 DONE | L | GAP-097 |
| GAP-097 | Email queue via RabbitMQ (replace direct HTTP) | 🟢 DONE | M | — |

**Dependencies:**
- GAP-093 → GAP-094 (backup trước, hard delete sau)
- GAP-097 → GAP-095, GAP-096 (queue infrastructure trước, retry + admin sau)
**Critical:** MUST complete before GA. Without GAP-093, data loss. Without GAP-097, emails unreliable.

---

### Epic 12: Process & DevOps Maturity
**Goal:** Process gaps cho production readiness — scripts, migrations, CI, deploy, incidents.

| Gap | Title | Priority | Effort | When |
|-----|-------|:--------:|:------:|:----:|
| GAP-081 ✅ | Script review checklist — DONE | 🟢 DONE | S | — |
| GAP-082 ✅ | Migration review checklist — DONE | 🟢 DONE | S | — |
| GAP-086 ✅ | Incident response runbook — DONE | 🟢 DONE | M | — |
| GAP-087 ✅ | Deploy go/no-go checklist — DONE | 🟢 DONE | M | — |
| GAP-088 ✅ | Rollback procedure per service — DONE | 🟢 DONE | L | — |
| GAP-083 ✅ | Gap triage process — DONE | 🟢 DONE | S | — |
| GAP-084 ✅ | CI failure triage — DONE | 🟢 DONE | M | — |
| GAP-085 ✅ | Cross-app consistency check — DONE | 🟢 DONE | M | — |
| GAP-089 ✅ | Post-deploy smoke test — DONE | 🟢 DONE | M | — |
| GAP-090 ✅ | API contract tests — DONE | 🟢 DONE | L | — |
| **GAP-202** | `/repo-status` skill blind to GitHub Security (Dependabot, code-scanning, secret-scanning) | 🟠 P1 Meta | S | Wave 10 Sprint 0 |

**Status:** 🟠 Re-opened 2026-04-21 — GAP-202 filed after `/repo-status` reported GREEN while 3 HIGH CVEs were live on main. Meta-P1 per `meta-gap-priority.md` §3 (skill blindspot = force multiplier). 10/11 gaps DONE; 1 OPEN.

---

### Epic 13: Frontend Quality
**Goal:** Fix UI issues từ UI audit.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-076 ✅ | KiteHub capture mock auth — DONE | 🟢 DONE | M |
| GAP-077 ✅ | KiteClass dev error overlay — DONE | 🟢 DONE | S |
| GAP-078 ✅ | KiteHub dark mode not switching — DONE | 🟢 DONE | M |
| GAP-079 ✅ | KiteClass i18n gaps — DONE | 🟢 DONE | M |
| GAP-080 | KiteHub dashboard loading/error UX | 🟡 P2 | M |

**Status:** 4/5 DONE. Only P2 GAP-080 open.

---

### Epic 14: Quality Governance
**Goal:** Meta-process — review standards cho outputs mà chưa có review process.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-048 ✅ | Output review standards coverage — DONE | 🟢 DONE | M |
| GAP-049 | Business logic correctness (stakeholder review) | 🟠 P1 | M |
| GAP-050 | Persona-based business review process | 🟡 PLANNED | S |
| GAP-101 ✅ | Docs folder README standardization (4 folders) — DONE PR #349 | 🟢 P3 | S |
| GAP-102 🟡 | 05-guides completion + ADR kickoff — PARTIAL (Part 2 DONE #350, Part 1 P2 DONE #352, Part 1 P1 open) | 🟡 P2 | M |
| GAP-103 ✅ | Deploy philosophy consolidation + AWS Agent Plugins ADR — DONE PR #351 | 🟢 P3 | M |
| GAP-149 ✅ | Audit skill grep scope multi-module (prevent GAP-107 false positive) — DONE 2026-04-20 Part C Sprint 0 | 🟢 DONE | S |
| GAP-150 | BRD docs completion (5 skeleton files: business-objectives, compliance-scope, pricing-model, nfr-catalog, go-to-market) | 🟠 P1 biz-logic | M |
| GAP-151 | Persona-specific acceptance criteria template + 4 Tier 1 AC docs (P1/P2/P3/P5) | 🔴 P0 biz-logic | M |
| GAP-152 | Execute persona review round 1 — role-play 4 Tier 1 personas + reports | 🔴 P0 biz-logic | L |
| GAP-153 | Secondary persona AC (Student/Parent/Teacher/Admin × tenant contexts — 8 P0 cells) | 🔴 P0 biz-logic | M |
| GAP-154 | **BRD scope expansion umbrella** — 22 missing BRD docs via simulation (7 P0, 7 P1, 5 P2, 3 P3); Phase 1 sub-gaps FILED 2026-04-20 | 🔴 P0 biz-logic | XL (phased) |
| GAP-180 | **Terms of Service** (customer legal contract) — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-181 | **Acceptable Use Policy** (AUP) — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-182 | **Privacy Policy** — VN PDPL mandatory — Wave 8 | 🔴 P0 biz-logic | L |
| GAP-183 | **Refund + Dispute Resolution** — VN Consumer Protection mandatory — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-184 | **Data Retention + Deletion Policy** — VN PDPL Art 6 mandatory — Wave 8 | 🔴 P0 biz-logic | M |
| GAP-185 | **Billing Terms + VAT/TCT compliance** — Circular 78/2021 mandatory — Wave 8 | 🔴 P0 biz-logic | L |
| GAP-186 | **Child Protection Policy** (K-12 P5 blocker) — Law on Children 2016 — Wave 8 | 🔴 P0 biz-logic | L |
| GAP-190 🟡 | KiteHub SEO — infra shipped (sitemap/robots/OG/JsonLd/blog); gap narrowed to pricing SSR, canonical schemas, GA4, content plan, Lighthouse CI — **Wave 9** | 🟠 P1 biz-logic | M |
| GAP-191 | Domain Registration + DNS Strategy (kitehub.vn + per-instance + custom CNAME) — **Wave 9** | 🟠 P1 biz-logic | M |
| GAP-192 | **Trial → Paid Zero-Downtime Migration** (state machine + outbox + rollback; layers under GAP-026) — **Wave 9 (Agent 9-A, first priority)** | 🔴 P0 biz-logic | L |
| GAP-193 | Session Orchestration + /start-session skill + multi-session lock — **Wave 8b (Agent 8b-E)** | 🟠 P1 meta | M |
| GAP-194 | Bash/Python Script Compliance (shellcheck + ruff in CI; no .husky exists yet) — **Wave 8b (Agent 8b-D)** | 🟠 P1 meta | S |
| GAP-195 | Starter-Kit Bulk Retro-Sync (export learnings to remote kit) — **Wave 8b (Agent 8b-F)** | 🟡 P2 meta | M |
| GAP-197 🟡 | Attendance Calendar — component shipped (PR 3.8.1); gap narrowed to parent/student variants + a11y + week view + UI review + E2E — **Wave 11** (parent variant blocked by GAP-052 Wave 10) | 🟡 P2 feature | S |
| GAP-198 | FE↔BE Decoupled Consumer-Side Contract (producer-side DONE via GAP-090/InstanceApiContractTest) — **Wave 8b (Agent 8b-F)** | 🟡 P2 meta | M |
| GAP-199 | Rework Audit for Context-Degraded PRs (Wave 6-8 era) — **Wave 8b (Agent 8b-E)** | 🟠 P1 meta | M |
| GAP-200 | School MIS/SMS Integration (VNEDU + SMAS + Base.vn) — **Wave 9 (Agent 9-C)** | 🟠 P1 biz-logic | XL |
| GAP-201 | Tenant Off-boarding Runbook (cancel UX + export bundle + purge; consumes GAP-073 deferred) — **Wave 8b (Agent 8b-F)** | 🟠 P1 meta | M |

**Dropped:** GAP-196 (9router ADR) — user decision 2026-04-20, not effective for project scope.

**Dependencies:** GAP-101 → GAP-102 (needs 05-guides README) → GAP-103 (needs ADR template + 02-architecture README). GAP-151 blocks GAP-152. GAP-153 blocks GAP-152 P5 review (Student/Parent AC critical). GAP-150 Phase 2 (content fill) blocked on stakeholder engagement. GAP-190/191 block GTM (GAP-150 Phase 2). GAP-192 depends on GAP-108 (trial config hardcoded); aligns with GAP-026 AI-budget layer. GAP-197 parent-variant blocked by GAP-052. GAP-199 consumes GAP-193 detection heuristic. GAP-201 consumes GAP-073 deferred items.
**Split:** GAP-101 standalone PR. GAP-102 split Part 1 (guides) + Part 2 (ADR kickoff). GAP-103 after 101+102.

**Part C Sprint 0 (meta-skills calibration):** GAP-149 closed. 5 audit skills (business-logic, performance, ops-readiness, security, api-contract) now document safe grep scope patterns. Retroactive check confirmed GAP-106/108/110 are valid (not false positives).

**BRD + persona governance wave (2026-04-20):** GAP-150/151/152 bundled with `meta-gap-priority.md` §3 update adding Business-Logic tier. GAP-049 + GAP-050 AC scope-split for clarity (process vs content vs framework vs execution).

**Coverage sync 2026-04-24:** Added 8 previously-missing meta gaps to this epic:

| Gap | Title | Status | Epic rationale |
|-----|-------|:------:|----------------|
| GAP-170 | Gap review template + skill | 🟢 DONE (Wave 8b-A) | governance |
| GAP-171 | Rules docs ADR-like review process | 🟢 DONE (Wave 8b-A) | governance |
| GAP-172 | Architecture ADR process | 🟢 DONE (Wave 8b-B) | governance |
| GAP-173 | Email template review checklist | 🟢 DONE (Wave 8b-C) | governance |
| GAP-174 | Marketing + legal docs review | 🟢 DONE (Wave 8b-C) | governance |
| GAP-175 | Logs format standard (spec only; impl Wave 7) | 🟢 DONE (Wave 8b-D) | governance spec |
| GAP-176 | UI/UX Pro Max skill integration | 🔵 OPEN | skill upgrade |
| GAP-205 | CI history retention policy + automation (50-run cap) | 🟢 DONE (2026-04-24 PR #471) | CI governance |
| GAP-206 | `/start-session` skill accuracy fix | 🟢 DONE (2026-04-24 PR #468) | skill fix |
| GAP-207 | `/start-session` VN language per CLAUDE.md | 🟢 DONE (2026-04-24 PR #470) | skill fix |
| GAP-212 | Fix `DefaultUrlAllowlistValidatorTest` flaky DNS of `api.partner.com` → loopback (blocks every Core CI run; pre-existing surfaced by PR #474) | 🔵 OPEN 🟠 P1 | test-only fix (RFC-2606 `.invalid`) |
| GAP-213 | Spring Cloud BOM resolution fails on Dependabot all-deps PRs that bump Boot parent (kiteclass-gateway + kitehub-gateway poms) — blocks weekly Spring-touching Dependabot PRs | 🔵 OPEN 🟠 P1 | pom BOM fix (likely explicit `spring-cloud.version` bump alongside Boot, or root-pom BOM import) |
| GAP-214 | Wave 5 post-wave audit suite refresh — API contract + security + performance + ops + quality stale during Wave 5 sprint; closed by Sub-PR 5.6 wave completion. Used as `AUDIT_OVERRIDE` link for Sub-PR 5.5 PR #529. | 🟢 DONE (5.6a 2026-04-25) — 5 audits committed: api 95/100, sec 85/100, perf 63/100, ops 52/100, quality 78/100 | governance / audit refresh |
| GAP-215 | `BrandingService.getBranding()` not `@Cacheable` — DB hit per document render (Wave 5 perf audit P0-1). | 🟢 DONE (Sub-PR 5.6b 2026-04-25) — `@Cacheable("branding-by-tenant", sync=true)` + `@CacheEvict` on mutators + `BrandingCacheIntegrationTest` (5 cases) | backend / cache wiring |
| GAP-216 | PDF/XLSX/DOCX p95 micro-benchmark + soft-cap regression assertion (Wave 5 perf audit P0-2). | 🟢 DONE (Sub-PR 5.6b 2026-04-25) — soft-cap timing assertions in 3 generator tests (PDF <4s, XLSX/DOCX <2s); full JMH suite is a Wave 7 follow-up | testing / perf canary |
| GAP-217 | Alert rules for `/api/v1/documents/*` (p95, error rate, cache miss storm) — Wave 5 ops audit P0. | 🟡 PARTIAL (Sub-PR 5.6b 2026-04-25 filed 3 rules in helm + docker prometheus configs); routing deferred — blocked-by GAP-120 Alertmanager | ops / alerting |
| GAP-218 | PDF font-missing runbook + image-build validation step (Wave 5 ops audit P0). | 🟢 DONE (Sub-PR 5.6b 2026-04-25) — Dockerfile font-presence assertion + `documents/05-guides/operations/runbooks/pdf-generation-font-not-found.md` | ops / runbook + CI |
| GAP-219 | Wave 5 audit follow-ups umbrella — 5 P1 + 8 P2/P3 sub-bullets across api/sec/perf/ops categories. Tracking-only; sub-bullets split into individual gaps when scheduled. | 🔵 OPEN 🟠 P1 | umbrella / maintenance |
| GAP-220 | `BrandingVersionService.snapshot` JSONB column type mismatch — `branding_versions.snapshot_json` column is jsonb but JDBC sends varchar. Wave 4 latent bug surfaced by Sub-PR 5.6b `BrandingCacheIntegrationTest`. Production tenants updating branding will 500. Workaround: `@MockBean` skips path in test; real fix requires `@JdbcTypeCode(SqlTypes.JSON)` on entity. | 🔵 OPEN 🟠 P1 | backend / persistence |
| GAP-224 | `collect-state.sh` blocker regex — sub-IDs (GAP-222a) collapse, prose cross-refs (BLOCKS GAP-006) pollute output, `sort -u` breaks priority order. Cosmetic accuracy fix; affects every `/start-session`. | 🔵 OPEN 🟡 P3 | skill fix (single-file) |
| GAP-225 | **Scaffolded-as-DONE Governance Closure Umbrella** — 5 gaps (008/009/012/015/018) shipped Wave 2-4 marked DONE despite explicit deferred items + missing audit-gate rules + missing skills + matrix mismatches. Captures systemic pattern + 3 cluster fix plan (C1 AI agent, C2 Saga, C3 AI branding — last covered by GAP-223 Sub-PR 223.1). Docs-only umbrella; Phase 2-3 (C1+C2) deferred until scheduled. | 🔵 OPEN 🟠 P1 meta | XL (phased — C3 done via GAP-223; C1+C2 future) |
| GAP-226 | Real WCAG contrast measurement (replace `ContrastCheck` scaffold pass) — implements WCAG 2.1 §1.4.3 luminance formula on theme JSON pairs; baseline §3 8/20 → ≥16/20 target | 🔵 OPEN 🟠 P1 feature | M (Wave 8+) |
| GAP-227 | Real visual regression diff (replace `VisualRegressionCheck` scaffold pass) — needs screenshot service + MinIO baseline store + pixel-diff engine; baseline §3 → ≥16/20 target | 🔵 OPEN 🟠 P1 feature | L (Wave 8+; depends on screenshot service) |
| GAP-228 | Real ML content classifier (replace `ContentModerationService` 3-stage scaffold) — toxicity/NSFW/brand-safety models + admin review queue; closes GAP-018 deferred scope | 🔵 OPEN 🟠 P1 feature | L (Wave 8+; depends on ML inference infra) |
| GAP-229 | AI Branding business docs v2 sync + 3 missing user guides — surfaced by GAP-016 verification sweep; v2 implementation in kiteclass-core but business `01-business/kitehub/ai-branding/{rules,use-cases,api-contract}.md` still v1; 3 user guides (branding-integration, wizard-flow, template-contribution) DO NOT EXIST | 🔵 OPEN 🟠 P1 biz-logic | L phased (~5-6h: Phase 1 docs ~2h + Phase 2 guides ~3h + Phase 3 verify ~30min) |

---

### Epic 15: Vietnam K-12 Education Features

**Goal:** Vietnamese K-12 school operational features — attendance models, reports, payroll, integrations specific to VN education context. Most gaps filed 2026-04-15..17 from deep K-12 domain analysis.

**Why domain-specific epic:** These touch Vietnamese education law (Thông tư 22, Luật Giáo dục), local vendors (VNEDU, VietQR, Zalo, Viettel SMS), and cultural patterns (Hạnh kiểm, GVCN, lên lớp/ở lại lớp). Distinct from generic K-12 or SaaS patterns.

| Gap | Title | Priority | Status | Effort |
|-----|-------|:--------:|:------:|:------:|
| GAP-051 | Bulk Import Users via xlsx/CSV | 🟠 P1 | 🟢 DONE Wave 1 MVP | M |
| GAP-055 | Official Report Card (Bảng điểm VN format, Thông tư 22) | 🔴 P0 biz-logic | 🔵 OPEN | L |
| GAP-056 | Homeroom Teacher (GVCN) concept | 🟠 P1 | 🔵 OPEN | M |
| GAP-057 | Teacher Payroll + Commission Calculation | 🟠 P1 | 🔵 OPEN | L |
| GAP-059 | Student Conduct / Hạnh kiểm tracking | 🟠 P1 | 🔵 OPEN | M |
| GAP-060 | Period-based Attendance (nhiều tiết/ngày) | 🟠 P1 | 🔵 OPEN | M |
| GAP-061 | Promotion / Retention Logic (Lên lớp / Ở lại lớp) | 🟠 P1 | 🔵 OPEN | M |
| GAP-062 | Payroll Bank Integration (Batch Transfer) | 🟡 P2 | 🔵 OPEN | L |
| GAP-063 | SMS + Zalo Notification Integration | 🟠 P1 | 🔵 OPEN | M |
| GAP-064 | SCORM / xAPI Compliance (Corporate Training variant) | 🟡 P2 | 🔵 OPEN | L |
| GAP-066 | KiteHub Unified Reports / Analytics Dashboard | 🟡 P2 | 🔵 OPEN | L |
| GAP-067 | KiteHub Instance Control Plane (AWS-/Vercel-style ops console) | 🟡 P2 | 🔵 OPEN | XL |
| GAP-068 | KiteHub Admin AI-Branding Console | 🟡 P2 | 🔵 OPEN | L |
| GAP-109 | Student bulk-import rules undocumented | 🟠 P1 | 🟢 DONE Wave 9-D | S |

**Dependencies:**
- GAP-055 depends on Wave 2 academic year/semester model (DONE via GAP-053)
- GAP-060 depends on period-based scheduling (partial via GAP-099)
- GAP-061 depends on GAP-055 (report card gates promotion)
- GAP-063 pairs with GAP-200 (school MIS integration, broader scope)
- GAP-066/067/068 depend on KiteHub subscription + instance ops stability (Wave 9 shipped)

**Status:** 2/14 DONE. Remaining 12 OPEN are split across 3 domains: reporting/grades (055, 061, 066), teacher ops (056, 057), attendance/conduct (059, 060), integrations (062, 063, 064), admin (067, 068).

**Suggested wave assignment:**
- Wave 10 candidate: GAP-055 (P0) + GAP-056/060/061 cluster (VN K-12 core)
- Wave 11 candidate: GAP-057/059 + GAP-063 (teacher + comms)
- Wave 12+: GAP-062/064/066/067/068 (P2 tier)

---

### Coverage additions to existing epics (2026-04-24 sync)

**Epic 6 (Operations & Scale) += 12 gaps** (observability + ops hardening, Part A audit follow-ups):

| Gap | Title | Priority | Notes |
|-----|-------|:--------:|-------|
| GAP-112 | Distributed tracing missing | 🟠 P1 | Wave 7 observability |
| GAP-113 | Frontend error tracking missing | 🟠 P1 | Sentry/Rollbar |
| GAP-114 | Structured JSON logging + MDC propagation | 🟠 P1 | Wave 7 (standard shipped via GAP-175) |
| GAP-115 | Log aggregation pipeline (ELK/Loki) | 🟠 P1 | Wave 7 |
| GAP-116 | PII scrubbing in logs | 🔴 P0 | VN PDPL Art 6 |
| GAP-118 | MinIO backup + replication strategy | 🔴 P0 | DR foundation |
| GAP-119 | Platform-wide DR runbook + RTO/RPO | 🔴 P0 | Ops readiness |
| GAP-121 | Per-alert runbooks library | 🟠 P1 | Consumes GAP-120 |
| GAP-122 | Missing platform-critical alerts | 🟠 P1 | Extends GAP-120 |
| GAP-123 | HPA for KiteHub services | 🟠 P1 | Scale readiness |
| GAP-124 | PodDisruptionBudget + NetworkPolicy hardening | 🟠 P1 | k8s hardening |
| GAP-130 | Docker compose zero resource limits (host OOM risk) | 🟡 P2 | Dev/staging only |

**Epic 13 (Frontend Quality) += 5 gaps** (2026-04-20 ui-review P2 findings):

| Gap | Title | Priority |
|-----|-------|:--------:|
| GAP-137 | Bulk import frontend UI missing (Wave 1 backend inaccessible) | 🟠 P1 |
| GAP-138 | KiteClass landing hero — duplicated "Chuyên nghiệp" copy | 🟡 P2 |
| GAP-139 | Parent dashboard MVP is placeholder-only | 🟠 P1 |
| GAP-140 | `form-select` default placeholder hardcoded English | 🟡 P2 |
| GAP-141 | Register-student date input locale-forced dd/mm/yyyy | 🟡 P2 |

**Epic 7 (UX & Conversion) += 3 gaps:**
- GAP-071 — Branding migration on tier upgrade/downgrade (🟡 P2, OPEN)
- GAP-072 — Scheduled rebrand + academic-year-tied branding refresh (🟡 P2, OPEN)
- GAP-074 — AI-generated alt-text for accessibility (a11y) (🟠 P1, OPEN)

**Epic 9 (Developer Experience) += 1 gap:**
- GAP-075 — Developer sandbox tenant environment (🟡 P2, OPEN)

**Epic 10 (Cross-cutting) += 1 gap:**
- GAP-065 — Migration chain not fresh-deploy safe (🟢 DONE, meta/ops fix)

---

## Updated Priority Tiers (186 gaps, refreshed 2026-04-24)

| Tier | Description | Count |
|------|-------------|-------|
| 🟥 **Block GA** (remaining open) | Core pipeline foundation + doc gen + K-12 core + observability P0 | ~12 gaps |
| 🟨 **Block GROWTH** (open) | UX, conversion, ops, webhooks, VN integrations | ~30 gaps |
| 🟦 **Block SCALE** (open) | Multi-brand, marketplace, advanced, admin consoles | ~18 gaps |
| ⬜ **Process/Internal** (open) | Advanced governance, persona review, skills | ~14 gaps |
| 🟡 **PARTIAL/PLANNED** | Scope-verified, waiting on wave assignment | 14 gaps |
| 🟠 **IN_PROGRESS** | Active wave or session work | 7 gaps |
| ✅ **CLOSED** | Completed Waves 1-9.5 + Part A/B/C audits + 2026-04-24 session | **81 gaps (44%)** |

### 🟥 Block GA — Only 6 remain open (refresh 2026-04-18)

| Gap | Title | Status | Effort |
|-----|-------|:------:|:------:|
| GAP-005 | AI queue fair scheduling | 🟡 Phase 2 open | M remaining |
| GAP-011 | Template library curation (30 templates) | 🟡 PLANNED Sprint 0 | L |
| GAP-014 🟡 | Wave mock plan include AI branding — planning v2-aligned 2026-04-26; impl GAP-235 | 🟡 PARTIAL | M |
| GAP-016 ✅ | Living docs impact scope — DONE Wave 7 (2026-04-26) | 🟢 DONE | — |
| GAP-046 | Design patterns applied systematically | 🟡 PLANNED Sprint 0 | M |
| GAP-047 | Document generation — Wave 5 DONE 2026-04-25; PPT deferred Wave 6 | 🟢 DONE | — |

**Previously listed GA blockers now CLOSED:** GAP-007, 008, 009, 010, 012, 013, 015, 018, 031, 041, 042, 081, 082, 086, 087, 088, 092, 093.

---

**Last Updated:** 2026-04-25 (**Wave 5 DONE** — Sub-PR 5.6b shipped wave closure + 4 P0 audit fixes from 5.6a. **GAP-047 → 🟢 DONE.** Wave 5 ledger: #474 5.0 + #476 5.1 PDF + #477 5.2 Excel + #478 5.3 Word + #529 5.5 branding + HTTP + #530 5.6a audit suite + 5.6b closure. Audit suite scores: api 95 / sec 85 / perf 63 / ops 52 / quality 78. P0 closures: GAP-215 cache, GAP-216 soft-cap canary, GAP-218 font runbook + Dockerfile assertion. GAP-217 PARTIAL (rules filed, routing deferred to GAP-120 Alertmanager). PPT deferred to Wave 6 per scope-lock. **Recommended next action:** **GAP-046 design-pattern audit** (next Meta-P0). Or Wave 10 GAP-055 report-card VN if business priority shifts. RTK pilot scaffolded (#531) — opt-in single-day measurement before any team-wide rollout.)

**Prior:** 2026-04-21 (**Wave 9.5 SHIPPED** via 4 parallel agents — PRs #415-#418. Pushed 2 PARTIALs → DONE (GAP-132 caching fan-out, GAP-134 @EntityGraph expand 3→9 repos). GAP-192 Phase 4b-i backend completeness shipped (45 new tests, 330 total in kitehub-subscription: webhook HMAC + scheduler + idempotency + retry + admin ops); stays 🟡 PARTIAL until FE integration Phase 4c. GAP-043 fan-out attempted 5 caches but 4/5 reverted after Redis+Jackson typing regression caught in integration tests; BrandingPackage proxy retained sync=true. Follow-up gap: harden CacheConfig serializer before re-attempt.)

### Session 3 refresh 2026-04-18 — ROADMAP status audit

Discrepancies fixed:
- GAP-081, 082, 083, 084, 085, 086, 087, 088, 089, 090 — were listed as P0 Block GA / P1 pending, actually all DONE → Epic 12 fully closed
- GAP-076, 077, 078, 079 — were listed P0/P1, actually DONE → Epic 13 reduced to 1 open (P2)
- GAP-048 — Epic 14 governance, actually DONE
- GAP-007, 008, 009, 010, 012, 013, 015, 018, 031, 041, 042 — core AI branding + security gaps DONE Wave 2-4, epic tables updated inline
- GAP-002 — async pipeline DONE Wave 3 (2026-04-18)
- GAP-015 — tenant provisioning auto-trigger DONE Wave 3 (was in Epic 1 as open)
- Priority Tier counts: 95 → 103 total, Block GA 24 → 6 actual open, CLOSED 15 → 48

Triggered by: status check found 6+ "Block GA" gaps already merged but ROADMAP not refreshed since 2026-04-14 wave log entries.

### New gaps 2026-04-18 (TODO audit post Wave 4)

- **GAP-098** (P2) — Notification settings API not implemented — `InstanceTab.tsx:57`
- **GAP-099** (P2) — Structured class schedule (replace free-form text) — `SubjectSection.java:24`
- **GAP-100** (P3) — Lunar calendar for VN holidays — `VnHolidayProvider.java`

### New gaps 2026-04-18 (docs folder governance audit)

- **GAP-101** (P3) — Docs folder README standardization (4 folders: 00-brd, 02-architecture, 05-guides, 07-archived)
- **GAP-102** (P2) — 05-guides completion (6 operational guides) + ADR kickoff (template + ADR-001 jobs+RabbitMQ)
- **GAP-103** (P3) — Deploy philosophy consolidation + ADR-002 AWS Agent Plugins evaluation

### Planning docs added 2026-04-18

- `documents/03-planning/plans/plan-ui-ux-design-system-integration.md` — 3-PR plan to adopt ui-ux-pro-max reasoning rules + upgrade ui-review skill to /148 scoring
- `documents/03-planning/waves/wave-05-document-generation.md` — Wave 5 plan for GAP-047. **Status: 🟢 APPROVED 2026-04-24 → IN PROGRESS (4/6 sub-PRs SHIPPED)** — Sub-PR 5.0 foundation + ADR-019 (#474), 5.1 PDF + invoice (#476), 5.2 Excel + attendance (#477), 5.3 Word + teacher contract (#478) all merged 2026-04-24. Remaining: Sub-PR 5.5 (branding integration) + 5.6 (wave completion). ADR-019 PROPOSED → ACCEPTED on Sub-PR 5.6 merge.

### Rules added 2026-04-18

- `.claude/rules/docs-folder-structure.md` — generic rule extending `planning-docs-structure.md` pattern to all `documents/` folders (GAP-101)

**Prior:** 2026-04-16 (added Epics 11-14, 48 new gaps from UI/process/SaaS audits)

### Audit Catch-up 2026-04-19 — 3 baselines shipped (Part A 3/5) — 🟢 COMPLETE

Parallel-agent execution (3 worktree-isolated agents, ~10-11 min wall-clock each, zero conflicts). Conflict-control applied per `feedback_parallel_agent_strategy.md`: pre-assigned GAP ranges, parent-owned shared files (ROADMAP + output-review-mandate + MEMORY consolidated in this PR), parent-sequenced merges (3 clean FF merges).

| Audit | PR | Score | Grade | Gaps (range) |
|-------|:--:|:-----:|:-----:|--------------|
| business-logic /100 (refresh, 27d stale) | #366 | 65/100 | D | GAP-104 → GAP-110 (7) |
| ops-readiness /100 (first-ever baseline) | #365 | 49/100 | F | GAP-111 → GAP-125 (15) |
| performance /100 (first-ever baseline) | #364 | 58/100 | F | GAP-126 → GAP-135 (10) |

**32 new gaps created (GAP-104 → GAP-135).**

Top P0 findings (meta-gaps listed first per `meta-gap-priority.md`):
- **GAP-104** (P0 meta) — Wave 3 fair-queue Phase 1 shipped 8+ config keys, 0 BR-QUEUE-* rules. Living Docs contract broken.
- **GAP-105** (P0 meta) — `parent-portal` domain missing 3-layer docs despite `ParentPortalProperties.java:16` referencing `BR-PARENT-003` (ghost rule ID).
- **GAP-111** (P0) — Monitoring stack (Prometheus/Grafana) only in dev docker-compose; production Helm/k8s deploys blind.
- **GAP-120** (P0) — Alertmanager has 7 alert rules but 0 receiver configured — alerts would fire silent.
- **GAP-117** (P0) — Backup restore never tested (GAP-093 shipped pg_dump but no restore drill/runbook).
- **GAP-126** (P0) — Admin dashboard calls `findAll() × 2` on Instance + Subscription tables no-cache, 6 stream aggregations per request.
- **GAP-127** (P0) — Frontend 0 code-splitting across 64 pages; framer-motion (~130KB) + recharts (~180KB) in initial bundle (~400-550KB First Load JS).
- **GAP-129** (P0) — `BrandingPackage` accepts `instanceId` param but ignores it, returns cross-tenant findAll — perf + multi-tenancy bug.

Status changes applied in this consolidation PR (`.claude/rules/output-review-mandate.md` §3):
- business-logic: stale (27d) → CURRENT (2026-04-19)
- ops-readiness: VIOLATION (never audited) → BASELINE_CAPTURED (2026-04-19, 49/100)
- performance: PLANNED → BASELINE_CAPTURED (2026-04-19, 58/100)

**Remaining Part A audits (per plan `documents/03-planning/plans/plan-audit-catchup-2026-04-19.md`):**
- Audit 4: ui-review /128 (8d stale)
- Audit 5: quality-audit /100 refresh (depends on Audits 1-4 findings)

### Audit Catch-up Part A — 5/5 COMPLETE (2026-04-19) — 🟢 COMPLETE

Continuation of 3/5 entry above. Audits 4+5 shipped in same session:

| Audit | PR | Score | Gaps |
|-------|:--:|:-----:|------|
| ui-review /128 (refresh, 8d stale) | #368 | KC 81/128, KH 59/128 (+1 each) | GAP-136 → GAP-142 (7) |
| quality-audit /100 (refresh, final) | #369 | **77/100 C+** (Δ −18 vs 95/100) | — (no new gaps per plan §3.5) |

**Total Part A gaps: 39** (GAP-104 → GAP-142). Running total 48/142 closed (34%).

**Calibration insight (Audit 5 report):** −18 delta is NOT a regression in 5 days. The 95/100 on 2026-04-14 was optimistic self-audit without specialist data (ops, perf were never audited). The 77/100 today is the FIRST HONEST BASELINE with ground-truth evidence from 4 specialist audits. Future deltas measure genuine improvement against 77, not inflated 95.

**Top 5 next-wave priorities (meta-boost per `meta-gap-priority.md`):**
1. **GAP-104** Wave 3 BR-QUEUE rules (Meta P0, 4-6h) — Living Docs contract broken
2. **GAP-105** parent-portal 3-layer docs (Meta P0, 4-6h) — ghost rule reference
3. **GAP-136** KiteHub custom error pages (Feature P0, 2-3h) — 5+ routes return English 404
4. **GAP-111 + GAP-120** monitoring + alertmanager prod Helm (Feature P0, 1-2d) — ops visibility
5. **GAP-128/129/133/131 batch** perf quick wins (Feature P0/P1, 1d)

Expected recovery per Audit 5: 77 → 85 (B+) end Week 2, → 90 (A) end Week 4.

**Governance turnaround COMPLETE:** hook (PR #362) enforces freshness; 5 audits now FRESH; baselines captured for 2 never-audited categories (ops, perf). Part B (fix waves) tracked via top-5 priorities above.

### Audit Catch-up Part B — 5/5 top priorities SHIPPED (2026-04-20) — 🟢 COMPLETE

Parallel-agent execution continued from Part A. 5 worktree-isolated agents fixed the Audit 5 top-5 priorities simultaneously. Wall-clock: Agent A 6 min, C 7 min, B 8 min, D 15 min, E 69 min (Maven + testcontainers). Zero merge conflicts — disjoint file sets.

| PR | Gap(s) closed | Agent | Highlights |
|:--:|---------------|:-----:|------------|
| #371 | GAP-104 (Meta P0) | A | 18 BR-QUEUE rules + 4 UC-AGENT-08..11 + metrics catalogue |
| #373 | GAP-105 (Meta P0) | B | parent-portal 3-layer: 30 BR-PARENT + 6 UC-PARENT + 5 endpoints; BR-PARENT-003 verified |
| #372 | GAP-136 (P0) | C | 3 error pages (not-found/error/global-error) + 13/13 tests green, dark-mode + Vietnamese |
| #374 | GAP-111 + GAP-120 (P0, foundation) | D | Prometheus + Alertmanager Helm deps + ServiceMonitors; 3 follow-up gaps (GAP-143/144/145) |
| #375 | GAP-128 + GAP-129 + GAP-131 + GAP-133 (P0/P1) | E | Installment scan fix, BrandingPackage tenant isolation, 6/9 HTTP timeouts, Hibernate batch=50; 5 new test files, ~1430 tests green |

**Gaps closed in Part B: 9** (GAP-104, 105, 111, 120, 128, 129, 131, 133, 136) → progress 48/142 → 57/147 (39%).

**New follow-up gaps created: 5**
- GAP-143 Grafana Dashboards Helm (P1, from D)
- GAP-144 Alertmanager Production Receivers (P0, from D)
- GAP-145 Loki Tracing Stack (P2, from D)
- GAP-146 HTTP timeouts remainder — payment/email/captcha (P2, from E)
- GAP-147 KiteHub Admin OpenAPI bean conflict — pre-existing (P2, discovered by E)

**Top-3 residual GA risks** (to review next wave):
- GAP-144 Alertmanager receivers (needed before prod deploy — alerts still silent)
- GAP-127 FE code-splitting (64 pages, ~400-550KB First Load JS) — not in Part B scope
- GAP-126 Admin dashboard findAll cache — not in Part B scope

**Superpowers adherence:** All 5 agents followed brainstorm + task-breakdown + (TDD where code) + implementation + self-review. Agent C and E delivered tests alongside code (TDD). Agents D and E self-caught writing to main worktree by mistake (hard rule 3 from `feedback_parallel_agent_strategy.md`) — no contamination landed on main.

**Conflict-control effectiveness:** 4/5 agents zero-collision auto-FF merge. Agent E merged with local leftover from worktree-root confusion (cosmetic, discarded before pull). No PR-level conflicts.

### Re-audit 2026-04-20 — Part B impact validation — 🟢 COMPLETE

Ran 2 parallel re-audit agents after Part B merge to measure delta. First attempt crashed silently (both agents stopped ~21 min post-spawn, coincident with `mcp__ide__*` disconnect — unrelated infra issue). Respawn succeeded cleanly.

| Category | Baseline 2026-04-19 | Refresh 2026-04-20 | Δ | PR |
|----------|:-------------------:|:------------------:|:-:|:--:|
| business-logic /100 | 65 D | **72 C** | +7 | #379 |
| performance /100 | 58 F | **64 D** | +6 | #378 |

**Business-logic findings (PR #379):**
- 2 CLOSED: GAP-104 (Wave 3 BR-QUEUE verified), GAP-105 (parent-portal 3-layer verified)
- 1 FALSE POSITIVE retracted: **GAP-107** — baseline grep scope missed `kiteclass/kiteclass-core/`; `ResilientAIClient` + `MockAIClient` + `OllamaAIClient` all exist with correct `@Profile("ai-live")` wiring
- 1 NEW: **GAP-148** (P2) — `BR-QUEUE-015..018` circuit breaker config exists in kitehub-branding but 0 `@CircuitBreaker` annotation (dead config)
- 7 unchanged (GAP-106/108/109/110 + 3 minor)

**Performance findings (PR #378):**
- 3 CLOSED: GAP-128 (installment PK lookup), GAP-129 (BrandingPackage tenant + V45 index + regression test), GAP-133 (Hibernate batch=50 × 5 services)
- 1 PARTIAL: GAP-131 (6/9 sites; remainder → GAP-146)
- 6 UNCHANGED: GAP-126, 127, 130, 132, 134, 135 (not in Part B scope)
- 0 new gaps, 0 regressions
- Category deltas: DB +3, API +2, Cache 0, FE 0, Resource +1

**Lessons learned added to skill roadmap (future work):**
- Business-logic-audit skill needs explicit broader grep scope (not just `kitehub/` + `kiteclass/` top-level) — risked false-positive like GAP-107
- Re-audit pattern works: shows calibrated delta + flags regressions; took ~5-8 min per agent

**Cumulative progress after re-audit:**
- Progress 57/147 → 58/148 (GAP-107 closed, GAP-148 added)
- Quality-audit 77/100 unchanged (not refreshed this round)
- Next recovery milestone: 77 → ~80 B- after next sprint closing GAP-148 + GAP-146 + GAP-132 (1-2 days)
