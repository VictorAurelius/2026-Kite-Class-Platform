# Gaps Roadmap — Epic-Based Organization

**Mục tiêu:** Biến 103 gaps thành actionable roadmap với epics + dependencies + sprints.

> **Khi nào đọc file này thay vì README.md?**
> - README: flat index, tra cứu 1 gap
> - ROADMAP: execution planning, sprint planning, dependency check

---

## 🎯 Current Status Snapshot (2026-06-01 — **Wave beta-readiness-9 SHIPPED — 4/4 parallel buckets**) — Track B Phase 1 BETA gate. 4 parallel Opus agents (PR #2017 plan + #2018/2019/2021/2022 + landing-kit #2020). **Notable: 3/4 buckets stale gaps — agent state-check (`audit-to-gap-pipeline.md` §2.8) prevented re-implement/regression.** A **GAP-814** PARTIAL 75% (header-strip pre-shipped #1991; delivered 27-route tenant-coverage audit + filed **GAP-825** JWT-sig+isolation). B **GAP-772+773** DONE-SUPERSEDED (staff-invite canonical kitehub-subscription Wave 80; kiteclass-core reversed GAP-786; agent correctly NOT rebuilt). C **GAP-127** PARTIAL 85% (real fix +@tanstack/react-table optimizePackageImports; both build pass). D **GAP-658** PARTIAL 90% (already VN Wave 98 B2; residual en-US = intentional BR-SEED-001 fixtures). Also shipped **landing review kit** (#2020 — design brief + /100 checklist for KiteClass landing redesign). Prior: Wave onboarding-polish-2 (GAP-536/538/610) + GAP-727/732 authz. **Next:** AWS-up session — live verify GAP-814/536/610/658 + GAP-825 + GAP-659 copywriter + GAP-823 META instances-triad (ADR); Wave thesis-2 NFR; Wave meta-9 candidate sau 30-day grace.

## 🎯 Previous: Wave meta-8 SHIPPED — catalog apply + 2 META detectors (2026-06-01 PR #2007 pending merge) — 5/5 buckets coordinator-inline ~1h (2.5x speedup). Bucket A 71 CSV updates + 18 status flips; Bucket B 14 SCOPE-REVISE Log markers; Bucket C META audit-cadence detector + GAP-821 + baseline 76 stale-cadence waves; Bucket D META CSV↔AC drift detector + GAP-822 + baseline 226 drift gaps; Bucket E GAP-444 → WONTFIX. Phase 2 (HARD STOP flips + direction-aware classification) queued Wave meta-9 sau 30-day grace.

## 🎯 Previous: Wave meta-7 META audit SHIPPED + CI runner optimize (2026-06-01) — 6 PRs merged (Foundation #1997 + Bucket A #1999 + B #2000 + C #2002 + D #2001 + path-to-thesis-goal #2003 + CI runner #2004). 172 P0+P1 gaps audited → **19 SHIPPED-DONE flip** (GAP-212/370/503/534/535/538/607/609/657/659/674/728/744/752/753/785/790/791/792) + 50 PARTIAL adjust catalog + 14 SCOPE-REVISE + 1 DROP candidate (GAP-444). **38% drift class confirmed** (65/172) validates GAP-791/792 hypothesis. Paired META rule `thesis-as-future-state-mandate.md` v1.0.0 + 2 Phase 1.5 Zalo gaps (GAP-819 OA push + GAP-820 Group link) + `path-to-thesis-goal.md` 15-wave roadmap. CI optimize: 17 docs jobs self-hosted → ubuntu-latest (queue contention -50%).

## 🗂️ Previous Snapshot (2026-06-01 — **Wave tenant-domain-1 cluster fix SHIPPED**) — 5/5 buckets merged (Foundation #1990 + Bucket A gateway IDOR #1991 + Bucket C FE middleware #1992 + Bucket B BE slug→UUID #1993 + Bucket D DNS+ACM scaffold #1994) ~2.5h coordinator-inline (~2.4x speedup vs 5-6h estimate). **4 PARTIAL gaps** (GAP-811 FE 70% / GAP-812 DNS 40% Phần B SSL + Phần C scheduler defer GAP-816 / GAP-813 BE slug→UUID 55% / GAP-814 gateway 60%) — live RST walk all 4 buckets gated GAP-612 AWS restore + Bucket D ACM apply. **0 user round-trip** trên 3 sequential CSV merge conflicts (keep-both pattern auto-resolved). **Post-wave audit suite required ≤3 days** per `post-wave-audit-mandate.md` (business-logic + api-contract + ops-readiness). Closure PR queued.

## 🗂️ Previous Snapshot (2026-05-28 — **Wave A full regression RST walk**) — 126-row acceptance catalog (`phase-1-beta-acceptance-self-test.csv`) walked: 5 luồng Wave A (user browser + Claude API verify) + ~100 rows via 3 Opus parallel agents (admin-public-smoke + owner-ops + teacher-parent-student). **GAP-794 ✅ DONE-ready** (PDPL consent verified end-to-end: POST 201 + DB row + GET 200, không 401). **GAP-790 ✅ API-layer PASS** (onboarding-progress 200). **3 NEW gaps filed per `audit-to-gap-pipeline.md`:** GAP-797 🔴 **P0 thật** (email template var-name contract drift — beta-invite render `------`/`beta-accept` → flow 1 die, user không signup; cross-flow sweep +welcome) + GAP-795 🟠 P1 **(RE-SCOPED — original P0 gateway-tenant was RST agent MISDIAGNOSIS; §2.8 fix-time investigation: gateway resolve+core filter+instance_id tag all WORK, shared-DB+filter architecture, kiteclass_877dff9d legacy, MDC tenant=- red herring; real bug = X-User-Id UUID vs Long.parseLong → UserContext null → created_by NULL, Wave meta-6 #13 recurrence; GAP-791/792 UNBLOCKED)** + GAP-796 🟠 P1 (kiteclass-core GlobalExceptionHandler mask 404/405 → 500, cả 2 agent confirm). Teacher-invite email = existing **GAP-787** confirmed open (publisher chưa publish; #1938 chỉ thêm consumer — EMAIL-RESET delivered ✅). Known gaps confirm open: GAP-515 lockout / GAP-521 audit-log endpoint / GAP-525 reject email. Findings: `documents/04-quality/audits/rst-html/2026-05-28-full-regression/INDEX.md` + `../2026-05-28-wave-a-5-flow-walk.md`. **Wave A NOT ready full DONE** — walk caught P0+P1 mà audit suite + Mockito miss (`feature-ship-runtime-walk-mandate.md` working as designed). **Fix-prep DONE (handoff for re-walk):** 4 fix branches pushed remote — GAP-797 `worktree-agent-aed0d9e4` (email var reconcile, 84 tests) + GAP-796 `worktree-agent-a62d4303` (404/405 handlers, 9/9) + GAP-795 `worktree-agent-a9489709` (UserContext/BaseEntity/V73 migration Long→UUID, 81 tests, ⚠️ authz PARTIAL) + GAP-787 already-wired-main (DONE candidate). GAP-792 OK. **GAP-795 investigation = MISDIAGNOSIS corrected** (tenant isolation WORKS, shared-DB+filter; re-scoped P0→P1 X-User-Id). **GAP-798 P1 NEW filed** = the "fix triệt để" remaining piece (authz V2 bridge: parents/teachers/students need `user_id` UUID FK + invite-accept populate + authz UUID compare — security-sensitive, investigate-first). **Handoff at 77% context** (re-walk migration + authz V2 = critical work, fresh context per session-orchestration): `documents/03-planning/session-handoffs/2026-05-28-wave-a-regression-walk-fix-branches-ready.md`. **Next session:** rebuild + RST re-walk 3 branches live → merge → GAP-798 V2 bridge → seed 1 English-teaching center (thesis Ch4 + manual GAP-537 25% + re-walk fixture triple-duty) → flip gaps DONE.

## 🎯 Previous: Wave meta-6 Bucket A walk SHUTDOWN at Bước 2.10 → 17 bugs surfaced → Plan D v2 LOCKED 4-8 weeks → Wave A planned (2026-05-28) — Session 2026-05-28 shipped 5 PRs merged + 1 pending: PR #1916 (7 walk-fixes + META rule `feature-ship-runtime-walk-mandate.md` v1.0.0 + 17-bug findings) → `b6539bab`; PR #1920 (mermaid CI tempfile race fix) → `a5039e24`; PR #1921 (audit retro Wave 80+ 46 features enumerated / 10 sampled / 50% NONE walk evidence) → `95cc53b6`; PR #1922 (3 gaps GAP-786/787/788 + Bug #17 code comment fix + META rule `docs-only-pr-no-block-wait.md` v1.0.0) → `fa6256c4`; PR #1923 (3 outside-in agents + Plan D v2 synthesis: persona simulation + failure-mode matrix + BETA-launch benchmark) → `4aa6d054`; PR #1924 (Wave A plan Phase 2 BETA Week 1-2 P0 fixes — Bug #14 email + Bug #17 user provision + GAP-704 JWT + Course/Class CRUD) PENDING. **Walk verdict:** Wave meta-6 Bucket A GAP-772 ship-DONE was premature per `gap-done-discipline.md` §2 — 2 P0 paths COMPLETELY MISSING (Bug #14 email never sent + Bug #17 accept doesn't create user). Code comment mis-referenced GAP-779 (unrelated /me endpoint); real fix tracked GAP-786. **Plan D v2 LOCKED** per 3-agent CONVERGENCE: 5-8 calendar weeks realistic; 4 weeks fastest credible; 1 friend close-loop first → expand 2+3 at Week 6 gate; P2 Owner-only cohort; 5 critical paths MVP slice; concierge install pattern (Stripe + Superhuman). **Phase 2 BETA scope locked** ~10 days time-box Wave A (@PreAuthorize sweep + Bug #14 + Bug #17 + GAP-704 + CRUD); Wave C/D/E defer Phase 3. Audit suite RETRO trust-pass anti-pattern recurrence ≥7 quantified. Main HEAD `4aa6d054`. **Next session pickup:** PR #1924 auto-merge then Decision Day 1-2 architecture lock Bucket A+B Option B (Outbox + RabbitMQ pattern) per `release-fix-retry-budget.md` §3.5 investigation-first → Week 1 execution Bucket A Bug #14 email path. Per session handoff `documents/03-planning/session-handoffs/2026-05-28-wave-meta-6-walk-shutdown-plan-d-v2-locked.md`. **Previous Wave 106 RST status (kept for reference):** Wave 106 RST Mảng A 🟢 PASS 3/3 luồng + GAP-764 P0 fixed + 2 META rules extended; PRs #1896 merged + #1897 CI green; Mảng B-D queued.

## 🎯 Previous: Wave 106 RST Mảng A (2026-05-27 — **Wave 106 RST Mảng A 🟢 PASS 3/3 luồng + GAP-764 P0 fixed + 2 META rules extended; PRs #1896 merged + #1897 CI green; Mảng B-D queued next session**) — Wave 106 RST đợt đầu shipped Mảng A (Anonymous A1+A2+A3) walked single coordinator-inline ~25 phút + 7 findings filed PR #1896 (6 non-blocking P2-P3 defer Đợt 107 per plan §7.4 + 1 P0 escalation GAP-764 separate PR #1897). PR #1897 ships P0 fix BetaAccessService.sanitizeFreeText UTF-8 mode + Flyway V57 backfill (rows 11+12 verified raw post-apply) + 2 META rule extensions paired same-PR per `rule-change-process.md` §6.5: `vn-localization-audit-checklist.md` v1.0.0→v1.1.0 §5 data roundtrip preservation through sanitization layers (META P0 force-multiplier prevent recurrence cho mọi future input sanitization touching tenant-facing field) + `pre-handoff-self-test-completeness.md` v1.1.1→v1.2.0 §3 post-fix re-walk mandate (META P0 — fix from RST/audit walk MUST re-walk source scope BEFORE DONE flip). Regression-guard `BetaAccessServiceSanitizeFreeTextTest` 16 tests PASS local. 2 follow-up gaps filed: GAP-769 (P2) Testcontainers IT full service roundtrip + GAP-770 (P2) META audit Wave beta-prep-1 + Wave 79 closure scope-completeness retroactive. Mảng status: A 🟢 PASS, B-onboard ⚪ NOT WALKED, B-CRUD ⚪, B-vận-hành ⚪, C ⚪, D 🟡 PARTIAL (Đợt 105 D1+D2 baseline). **Previous (kept for sister-session reference):** RST cleanup cluster shipped 2026-05-27 morning — 4 PRs merged (1884+1885+1886+1887) + GAP-758/759 DONE + GAP-760 PARTIAL 40% + GAP-761 NEW OPEN. Main HEAD `7967f01b` (pre-Wave-106). — Session 2026-05-27 close-out RST cleanup: PR #1884 GAP-758 (Option A layout fix Phase 1 BETA persona route-restrict — KC spec 5/5 PASS Owner JWT real-login walk + KH OWNER 4/5 PASS bao gồm /school-admin/bulk-import critical path), PR #1885 GAP-759 (KC class-lifecycle E2E pre-existing flake — root cause Wave 105 contract sync miss; setupAuthMocks helper synced; paired RST→E2E spec gap-759-flat-auth-shape-contract.spec.ts 2 tests), PR #1886 GAP-760 PARTIAL 40% (KH setupMockAuth Zustand hydration race — Option B addInitScript shipped, 13→15/20 PASS improvement; residual 5/20 cần Option C production code wait-gate), PR #1887 GAP-761 NEW OPEN (Zustand persist rehydrate route-guard sentinel — Option C scope ~4-5h, useAuthStore.persist.hasHydrated() across 5 route-guard layouts). Main HEAD `7967f01b`. **Wave 106 RST execution queued next session** per session-handoff `documents/03-planning/session-handoffs/2026-05-27-rst-cleanup-wave-106-queue.md` + wave plan `wave-2026-05-23-106-rst-phase-1-beta-full-walk.md` — 23 luồng × 4 vai trò (Anonymous + Owner + Staff + Platform_Admin), 6 mảng sequential (~3-5h agent-wall, KHÔNG parallel — same Docker port). Per Wave 106 Q2 chốt 2026-05-27: "RST đầy đủ 23 luồng + sửa tại chỗ với lỗi chặn luồng". PR #1888 wave-rst-html-1 plan: keep OPEN, defer refine sau Wave 106 (lúc đó có real RST findings inform HTML dashboard scope).

## 🎯 Previous: Wave beta-prep-1 SHIPPED (2026-05-26 — **7 PRs merged + 17 DONE + 8 PARTIAL + 2 NOT-IMPL + 4 follow-up gaps + 1 META rule pre-flight-aws-lifecycle-check.md v1.0.0; PRs #1872 H + #1873 C + #1871 D + #1875 E + #1877 B + #1874 A + #1876 F+G**) — Wave beta-prep-1 (9-bucket parallel + 1 meta) shipped trong single session ~6h coordinator-inline (vs ~3-4 tuần plan estimate — ~80x speedup via 6-agent parallel + 2 spawn rounds). 2 spawn rounds (1st: 4/6 Sonnet thrash + Anthropic plan quota exhaust at 22:30 BKK; 2nd post-quota-reset: 4 Opus 4.7 successful). 2 fix-agents for FE PRs (E2E + unit tests). 3 ADMIN_MERGE_OVERRIDE: GAP-746 trailers (kiteclass-core multi-tenant flake exception class per `admin-merge-discipline.md` v1.0.3 §11). Main HEAD `a64bcef2` post all 7 merges. **17 DONE items:** Bucket A (Privacy + ToS + Retention + Breach SOP) + Bucket B (auth race IT + bucket policy + RLS negative IT) + Bucket E (3 code fixes — DataIntegrityViolation handler + idempotent verify + race recovery) + Bucket F (5/7 items shipped F1 + F2 + F3 + F4 + F5) + Bucket G (4/4 widget + Zalo OA stub + HelpLink + escalation runbook) + Bucket H (ADR-036 + FAQ + GAP-754). **8 PARTIAL:** Bucket A.2 consent FE only (BE pending GAP-755), Bucket B.1 CVE (0 HIGH found vs plan stale "6 HIGH backlog"), Bucket B.3 upload cap (3 IT @Disabled), Bucket C.1/2/3 (Statuspage runbook + 8 SNS alarms tf code + restore drill framework — terraform apply DEFER GAP-756), Bucket D GAP-727 (6 IT, live verify gated GAP-612+GAP-756), Bucket F.7 (multi-branch filter FE only, BE mirror defer). **2 NOT-IMPLEMENTED:** F.6 bulk-invite CSV (V62 + admin UI scope deferred) + Bucket L landing+pricing audit (defer Wave beta-prep-2). **Phase β AWS smoke:** infrastructure UP ✅ (`api.kitehub.me/actuator/health` 200 + landing 200 + beta-status 200); Wave code NOT deployed (FE /privacy /terms /waitlist 404 — pre-wave Docker image on EC2). Full production deploy DEFER → GAP-756 P0 (blocked GAP-612 RST policy gate 2026-05-25 + ECR `kitehub-platform` repo provisioning gap). **Follow-up gaps filed:** GAP-754 multi-branch foundation Phase 2 (paired ADR-036) + GAP-755 PDPL consent BE persistence (paired Bucket A FE) + GAP-756 Wave production deploy + RST verify P0 + GAP-757 Post-wave audit suite refresh P1 (3-day window per `post-wave-audit-mandate.md` §2.2 deadline 2026-05-29). **1 META rule shipped:** `pre-flight-aws-lifecycle-check.md` v1.0.0 (force-multiplier prevent cred-rotate cycle recurrence ~12min/session save) — triggered by Phase β session 2026-05-26 17:50 `dev-admin` keys expired → 12min user-action cycle. **AWS stack STOPPED post-closure** (3 EC2 stopped + RDS stopping background) to save Free Tier hours. **Next critical path:** GAP-756 deploy pipeline (~1.5-2h next session — local RST → re-enable ECR push → tag v0.9.0-beta-staging.22 → docker build → SSM deploy → smoke admin-login + wave endpoints). Per `release-fix-retry-budget.md` v1.2.0 §5 tooling-fix-then-retry exception class.

## 🎯 Previous: Wave rst-cascade-1 SHIPPED (2026-05-26 — **5 DONE flips + 14 PARTIAL + 5 cascade findings + new rule ci-queue-local-runner-threshold.md v1.0.0; PRs #1861 + #1865 + #1869**) — Wave rst-cascade-1 walked 19 cascade PARTIAL gaps via 3 Opus 4.7 parallel bg-agents (Phase α) + coordinator inline + AWS production smoke (Phase β). **5 DONE flips**: GAP-684 admin-login + GAP-514 gateway rate-limit + GAP-508 production env + GAP-724 FE auth + GAP-611 (promoted Phase β — RFC 7231 reword HTTP 400 vs 404). **5 cascade findings**: 2 P1 promoted GAP-752 (RabbitMQ class.rescheduled.queue declaration missing — Wave br-4 GAP-291 incomplete) + GAP-753 (beta-signup invalid UUID 500 — cần @ExceptionHandler + E2E spec per RST→E2E promotion). **Wall-clock**: ~1h 45min (3-4x wave-pack parallel speedup). **AWS cost**: $0.5 actual vs $15-16 projection. **New rule shipped**: `ci-queue-local-runner-threshold.md` v1.0.0 META P1 force-multiplier (saved ~15 min wall-clock + 63 CI job slots on 4-PR session). **Next critical path**: Wave beta-prep-1 mega-wave (9 bucket parallel scope post 3-Opus outside-in audit consensus: A PDPL min 5 items + B security-beta-min + C ops monitoring + D class-teacher + E concurrency hardening + F beta invite + G tenant support Zalo OA Free + H multi-branch decision spike + L landing+pricing audit) — PDPL deadline 2026-07-01 (5-tuần countdown; ship ~2026-06-23 = 8-15d buffer). **Previous:** Wave aws-restore-1 SHIPPED — production stack FULLY RESTORED ~3.5h; ALB ELIMINATED PERMANENTLY ($20-25/mo permanent save); 13 cascade gaps unblock for Wave rst-cascade-1 — 5 PRs Wave aws-restore-1: #1852 Wave plan + RDS snapshot_identifier var + lifecycle ignore_changes + workflow input + #1853 fix TF_VAR_aws_account_id wiring (closes GAP-692 Phase 1) + #1854 Phase C ALB elimination (nginx multi-host + var.enable_alb default true→false + api CF DNS) + #1855 fix cloudwatch-dashboard remove 4 ALB widgets + #1856 terraform import jwt_challenge + resend_api_key secrets (closes GAP-717). Plus closure PR — 5-target sync: GAP-612 DONE 100% + GAP-717 DONE 100% + GAP-693 stays PARTIAL 70% (SOP runbook deferred Wave aws-rebuild-sop-1). **End-to-end smoke** `curl https://api.kitehub.me/actuator/health` HTTP 200 from outside (CF edge → kc_app_fe EIP → nginx vhost block 3.5 → upstream kh_backend_gateway 10.0.0.129:8080 → Spring DB UP + Redis UP + 17GB free disk). Apex preserved 200. Phase C2 retry #1 needed (DependencyViolation manual revoke 10 orphan SG ingress). Architecture pivot user AskUserQuestion Path B — kc_app_fe nginx Host-based vhost routes api.kitehub.me → kh_backend gateway private VPC reuse existing EIP + wildcard *.kitehub.me cert + PM2. Wave aws-restore-1 (tag_primary: aws-restore, counter: 1). **Previous session 2026-05-26 (earlier):** Wave audit-stale-sweep-1 SHIPPED — 38 active Phase 1 BETA P0 state-check; 0 stale-DONE; 2 file-vs-CSV drift fix; 38 last_verified bumped (PR #1851). Critical path identified: GAP-612 single-root unblock → cascade 13 PARTIAL→DONE. **Previous:** Wave beta-readiness-7 SHIPPED 5/5 buckets — Document performance cluster (cache + JMH canary + alerts + fonts + Outbox DLQ) — 6 PRs merged: #1842 Bucket D GAP-218 (Dockerfile font assertion comment fix; Part A+B already shipped Wave 5 Sub-PR 5.6b) + #1843 Bucket E GAP-742 (NEW outbox-dlq-alerts Prometheus group + investigation runbook VN) + #1844 Bucket C GAP-217 (alert rules state-check found existing lines 100-144 prometheusrule.yaml from Sub-PR 5.6b era; normalized 2 runbook_url + new promtool CI job + helm dep build fix) + #1845 Bucket B GAP-216 (soft-cap canary 3 generator tests + BR-DOC-PDF-007 rule clarification + GAP-750 follow-up filed) + this closure PR (5 GAP DONE + git mv to closed/ + wave-history append + ROADMAP + handoff). **5 GAPs DONE:** GAP-215/216/217/218 P0 + GAP-742 P1. **1 NEW gap filed:** GAP-750 P1 (JMH proper suite Wave 109+ ops-readiness scope). **Pattern lớn**: 4/5 buckets (A+B+C+D) state-check phát hiện code đã shipped Wave 5 era — chỉ Bucket E (GAP-742) thực sự greenfield. META insight: `gap-done-discipline.md` §2 stale-OPEN recurring → triggered user pivot **Wave audit-stale-sweep + 4 hard blocker waves (security-1 CVE / ops-1 Restore Drill / compliance-1 PDPL Cookie / perf-1 FE code-splitting) BEFORE Đợt 108 RST** (sequencing chốt 2026-05-26). GAP-746 ADMIN_MERGE_OVERRIDE used for B+D (kiteclass-core multi-tenant test flake unrelated to scope per `admin-merge-discipline.md` v1.0.3 §11). Wall-clock ~3-4h (4 Opus 1M bg-agents parallel + coordinator inline Bucket A + closure). **Previous:** Wave beta-readiness-6 SHIPPED 3/3 buckets — API contract drift trio — 5 PRs merged: #1836 plan patch state-check (3 path corrections + Opus mandate) + #1838 Bucket A payment-invoice (api-contract.md 189→830 lines, 31 endpoints, PaymentController+PaymentWebhook+Invoice+Refund+InstallmentPlan) + #1839 Bucket B attendance (113→599 lines, 18 endpoints across 4 ctrl vs gap-claim 9 in 1 — 3 prior waves shipped without back-sync) + #1840 Bucket C student-enrollment (91→803 lines, 25 endpoints across 5 ctrl with HTTP status corrections + header contracts + cross-domain side-effects) + this closure PR (3 GAP DONE + git mv to closed/ + wave-history append + ROADMAP + handoff). **3 P0 gaps DONE:** GAP-231/232/233. Wall-clock ~4-5h vs ~12-15h serial → 3-4x speedup. Pre-spawn state-check via #1836 caught 3 scope errors (kitehub-subscription → kiteclass-core path; documents/01-business/ 2-level nesting kiteclass/) eliminated ~30-60 min preventable round-trips. **Wave beta-readiness-7 plan patch shipped same session (PR #1837)** — scope-completeness reconciliation table + Bucket A scope reduced (GAP-215 already shipped in code per state-check; coordinator inline ~30 min) + Bucket C+E path correction (helm prometheusrule.yaml). **br-7 spawn HELD next session** per user direction (4 Opus agents B/C/D/E + coordinator inline A queued).

## 🎯 Previous: Wave meta-5 SHIPPED (2026-05-25 — cert-expiry alarm false-positive fix + meta-4 Vercel residue cleanup) — CloudWatch alarm `kitehub-kc-app-fe-cert-expiry` fire 8 ngày false-positive (EC2 stopped post-GAP-612 + Wave br-8 cleanup → metric không emit → `breaching` treatment). Fix Tier 3 mutation: `TreatMissingData: breaching` → `notBreaching` + force state OK. User pre-authorized qua `AGENT_AWS_TIER3_OK` trailer per `agent-aws-access.md` §6. Audit artifact `documents/04-quality/audits/aws-verification/2026-05-25-cert-expiry-alarm-fix.md` shipped. `alb-architecture.md` §8.1 updated. **Wave meta-4** (Vercel residue cleanup PR #1830) shipped — eliminate Vercel FAILURE check cycle khỏi PR pipeline. **GAP-748** filed P1 — kiteclass-frontend E2E `class-lifecycle.spec.ts` ECONNREFUSED pre-existing flake.

## 🎯 Previous: Wave beta-readiness-5 CLOSURE (2026-05-25 — 3/3 buckets PARTIAL + 4 GAP closures + 1 META rule + ALB doc) — Pre-spawn state-check (per `audit-to-gap-pipeline.md` §2.8 + `release-fix-retry-budget.md` §3.5) caught 3 scope errors → refined 4 buckets → 3 buckets. **GAP-606 🟢 DONE** (Wave 91 stale 8d) / **GAP-608 🟡 PARTIAL 90%** (terraform IaC ses:SendEmail; live verify GAP-747 gated GAP-612) / **GAP-610 🟡 PARTIAL 75%** (3 hypotheses REJECTED — defensive hardening Wave 91 đã shipped; Testcontainers IT unblocked) / **GAP-611 🟡 PARTIAL 70%** (Class D application-layer 404 → JSON errorCode response). 7 PRs merged session này (#1822 Wave meta-3 closure + #1823 scope refine + #1824/1827/1828 buckets + #1825 META E2E vs RST rule + #1826 ALB architecture doc + #1829 closure PR). NEW META rule `e2e-rst-test-layer-boundary` v1.0.0 codifies E2E vs RST boundary + RST→E2E promotion mandate. ALB doc shipped Vietnamese-revised per dev feedback. 6 frontmatter drift sync (102.8/102.9/103/105/beta-readiness-4/beta-readiness-8 status: draft → complete). **Follow-up:** GAP-747 + GAP-746 + GAP-748 + ops-readiness-audit post-Wave-br-5 within 3 days (per `post-wave-audit-mandate.md`). (Wave meta-3 entry preserved trong git history — `wave-history.jsonl` canonical.)

## 🎯 Previous: Wave meta-3 CLOSURE (2026-05-25 — GAP-735 + GAP-745 🟢 DONE, GAP-746 re-classified P1 functional bug) — Empirical investigation per `release-fix-retry-budget.md` §3.5 (newly-landed v1.2.0 Investigation phase mandate, PR #1821) revealed 2 residual multi-tenant test fails là **service-layer functional bugs**, KHÔNG phải test infrastructure: `EnrollmentRepository.findByIdAndDeletedFalse(id)` thiếu tenant filter → cross-tenant GET trả về 500 thay vì 404; `InvoiceServiceImpl` own-tenant filter trả empty. Wave meta-2 dynamic TRUNCATE fix đã hoàn thành test-isolation scope (4/6 baseline unblock + INV-2026-000001 collision class eliminated). **GAP-735 🟢 DONE (100%)** + **GAP-745 🟢 DONE (100%)** + **GAP-746 P1** (re-classified P2→P1, scope revised từ test-infra hypothesis sang service-layer multi-tenant isolation functional bug — Path A repository tenant filter + Path B exception mapper + Path C InvoiceFlow read; defer dedicated future wave). `admin-merge-discipline.md` v1.0.3 §11 Log documents `ADMIN_MERGE_OVERRIDE: GAP-735` trailer no longer needed prospectively. (Previous Wave meta-2 entry preserved trong git history — `wave-history.jsonl` canonical.)

### 🚀 Next Action 2026-05-26+ — Wave meta-3 candidate: GAP-746 targeted investigation (2 multi-tenant isolation tests có thể assume listener không fire during setUp; option A inspect each test method body, option B disable listener on 2 classes). Then: remove `ADMIN_MERGE_OVERRIDE: GAP-735` từ rule exception list khi GAP-746 close.

## 🎯 Previous: Wave meta-1 SHIPPED 4/4 buckets — 5 PRs merged (2026-05-25) — Plan PR #1791 (drafted Wave audit-1) + Bucket C #1810 admin-merge-discipline Log+PATCH **DONE** + Bucket D #1812 Entity-Mapper CI gate (**GAP-743 DONE** — scripts/check-entity-mapper-consistency.sh + workflow + design-patterns.md §3.12) + Bucket A #1813 @Rollback(true) 3 IT classes **PARTIAL** (retry #2 @DirtiesContext made worse 1→13 errors per `release-fix-retry-budget.md` §4 GROWING pivot signal → reverted to @Rollback-only) + Bucket B #1811 TestFixtureCleanup utility **PARTIAL** (Spring TestExecutionListener, won't fix GAP-735 alone) + closure docs #1814 (GAP-745 follow-up + this ROADMAP entry). **GAP-735 PARTIAL 50%** (EnrollmentIT residual flake `shouldIsolate_multiTenantData` — root cause `InvoiceTestDataBuilder` hardcoded `INV-2026-000001` across 7 test files → GAP-745 Wave meta-2 candidate). **GAP-743 DONE 100%** (Entity-Mapper triad CI WARN-mode active, HARD STOP target post-stabilization). Bucket A agent timed out mid-mvn-verify → coordinator-adopted work. 4 PRs used ADMIN_MERGE_OVERRIDE trailer (Vercel 24h rate-limit + decommissioned per `no-vercel-references.md` Wave 88).

### 🚀 Next Action 2026-05-26+ — Wave meta-1 follow-ups: (1) **GAP-745 InvoiceTestDataBuilder deep fix** (unique invoice numbers per test method — Wave meta-2 candidate); (2) Remove Vercel from GitHub required-checks branch protection (Wave meta-2 cleanup); (3) **Resume Wave beta-readiness-5** (4 P0 cluster — GAP-606/608/610/611 signup E2E) HOẶC br-6 (API contract drift trio) HOẶC br-7 (document performance cluster); (4) Post-wave audit suite for Wave br-8 ≤3 ngày (deadline 2026-05-28) per `post-wave-audit-mandate.md` §2.2 — Security + Business Logic + API Contract refresh.

## 🎯 Previous Snapshot (2026-05-25 — Wave beta-readiness-8 SHIPPED 7/7 buckets + AWS UNBLOCK + cleanup — 10 PRs merged) — Plan PR #1796 + Đợt 1 (#1797 Bucket E + #1799 Bucket A + #1801 Log sync + #1800 Bucket D+F bundle admin override) + Đợt 2 (#1804 Bucket B + #1805 Bucket C admin override + #1806 Log sync) + Mid-wave META (#1798 agent-model-opus-default + #1802 disable docker-build-push push:main + #1803 AWS post-restore cleanup audit). **6 P0 gaps closed:** GAP-737/738/739/740/741 + GAP-612 PARTIAL 5%→30%. **NEW META rule** `agent-model-opus-default.md` v1.0.0 — Opus 4.7 mandatory cho mọi non-trivial Agent spawn.

## 🎯 Previous Snapshot (2026-05-25 — Wave audit-1 SHIPPED 4/4 audit reports + 4 future wave plans drafted) — 4 audits drove Wave beta-readiness-8 scope. ALL 4 Sonnet agents thrashed first attempt (Wave br-4 N=4/4 lesson) → codified META rule `agent-model-opus-default.md` Wave 8 mid-wave.

### 📜 Previous: Wave beta-readiness-4 SHIPPED 5/5 buckets + 3 hotfixes + 1 new META rule — 11 PRs merged — Plan PR #1778 + Bucket A #1779 META env-coverage RESEND IaC PARTIAL 90% (live verify gated GAP-612 AWS suspended) + Bucket B #1782 PDPL consent immutable + hash chain PARTIAL counsel-deferred + Bucket C #1783 Pricing PER_HOUR + GAP-292b paired payment recording DONE + hotfix #1784 Course entity pricingModel/unitPrice fields + Bucket D #1781 Reschedule + email fallback DONE + Bucket E #1785 Email tone matrix Thymeleaf + VN sample audit DONE + new META rule #1786 fix-up-ci-selective-rerun v1.0.0 + hotfix #1787 ClassMapper @Mapping ignore + hotfix #1788 strict-warnings cleanup. 3 outside-in agents (persona + VN edu benchmark + failure-mode matrix) surface 7 P0 + 10 P1 cross-bucket cells. User 3-Q decisions: PDPL keep 2026-07-01 / pricing PER_HOUR primary per Apollo/ILA VN benchmark / GAP-292b paired. Sonnet 200k thrash recurrence (B/C/D first attempt) → Opus 1M retry success. ADR numbering chaos resolved: ADR-033 (D Cal.com reschedule) + ADR-034 (B cookie consent) + ADR-035 (C pricing). Migration version reservation: V56 (B consent) + V67 (C pricing) + V67b→V69 (C payment records) + V68 (D reschedule audit). 8 follow-up gaps tracked (file pending): resend-live-verify-post-restore P1 / env-coverage-hard-stop P2 / pricing-data-reclassification P1 / zalo-oa-notification P2 / vat-einvoice-misa P2 / counsel-pdpl-review P1 / resend-vs-ses-eval P2 / classmapper-meta-entity-vs-migration-consistency P2 META. **Previous: Wave beta-readiness-2 SHIPPED 4/4 buckets — 9 PR merged** — second tag-based execution wave: #1767 plan + #1768 B authz hasAccessToClass fix PARTIAL admin-merge AUDIT_OVERRIDE → GAP-735 + #1769 A idempotency 1/3 controllers admin-merge AUDIT_OVERRIDE → GAP-735 + #1770 C state-check phát hiện outbox dispatcher đã có sẵn Wave 91 PR #1487 + #1771 D contract drift GAP-662 Option B + GAP-663 PreferencesControllerIT 4/4 PASS + #1772 GAP-734 follow-up file + #1773 session handoff + #1774 post-merge sync GAP-662+663 Log + #1775 GAP-735 pre-existing flake file. 4 follow-up gaps: GAP-732 P1 (test re-enable Bucket B) + GAP-733 P2 (v1 namespace migration EmailController) + GAP-734 P1 (signup + beta-request idempotency kitehub-subscription) + GAP-735 P1 (pre-existing test flake `kiteclass-core`). Key finding: 6 CI test fail trên Bucket A + B = flake có sẵn trên main (verify cục bộ: CourseSecurityTest 15/15 PASS isolated nhưng 4 fail trong full suite — test pollution). Coordinator Opus 1M inline + Agent A Opus narrow scope survive; 3 Sonnet agent (B/C/D) fail autocompact thrash do path-scoped rule auto-load + multi-file read overflow 200k context.

### 🚀 Next Action 2026-05-24 — Post-wave audit suite Wave beta-readiness-4 ≤3 ngày (deadline 2026-05-27) per `post-wave-audit-mandate.md` §2.2 — Security + Business-Logic + API contract + Ops Readiness refresh impact 5 buckets. Then GAP-735 dedicated test isolation wave (Testcontainer DB reset between IT classes + @Transactional@Rollback) để remove AUDIT_OVERRIDE trailer dependency cho future code PRs. Plus GAP-612 AWS restoration tracker — unblock cluster: GAP-NEW-resend-live-verify + Bucket A live verify (GAP-508 Phase 2.5) + Bucket B counsel review path + Bucket E VN ISP smoke test. Wave beta-readiness-5 candidates: GAP-732 (B test re-enable) + GAP-734 (A signup+beta-request kitehub-subscription) + remaining Phase 1 BETA P0 backlog + GAP-NEW pricing data reclassification UI + persona Vy email migration backfill.

### 🎉 Wave beta-readiness-2 SHIPPED 2026-05-24 — Idempotency + Authz hasAccessToClass + Outbox + Contract drift (4/4 buckets)

9 PR merged ~6h actual session compute. Second tag-based execution wave per `wave-tag-numbering-convention.md` §2.

| PR | Bucket | Scope |
|---|---|---|
| #1767 | Plan | 4-bucket scope docs-only |
| #1768 | B Authz (GAP-727 PARTIAL) | Class entity teacher_id field + ClassServiceImpl set teacherId từ UserContext; production defect FIXED; 2 @Disabled test bodies defer → GAP-732 |
| #1769 | A Idempotency (GAP-730 PARTIAL 1/3) | V66 migration + IdempotencyService + IdempotencyScope enum + EnrollmentController wrapped + 2 IT PASS; signup + beta-request live trong kitehub-subscription → GAP-734 follow-up |
| #1770 | C Outbox state-check | GAP-605 flip DONE qua §2.8 fix-time state-check — SubscriptionOutboxDispatcher + fast-path đã ship Wave 91 PR #1487; tiết kiệm ~3h xây dựng duplicate |
| #1771 | D Contract drift (GAP-662 + GAP-663) | api-contract.md Option B doc sync `/api/email/send` → `/api/platform/emails/send`; PreferencesControllerIT 4/4 PASS (cookie httpOnly=false + SameSite=Lax + 401 without JWT + 400 validation); Option A v1 namespace migration → GAP-733 |
| #1772 | GAP-734 file | Bucket A scope reconciliation follow-up (Signup + BetaRequest trong kitehub-subscription) |
| #1773 | Session handoff | 2026-05-24 execution log |
| #1774 | Post-merge sync | GAP-662 + GAP-663 Log entries cite PR #1771 + PR-1771.json |
| #1775 | GAP-735 file | Pre-existing test flake `kiteclass-core` (2 deterministic + 4 CI-suite pollution) — P1 dedicated fix wave candidate |

### 🎉 Wave beta-readiness-1 SHIPPED 2026-05-24 — Security cluster + enrollment capacity greenfield (3/4 buckets executed)

5 PR merged ~5h actual session compute. First tag-based execution wave per `wave-tag-numbering-convention.md` §2.

| PR | Bucket | Scope |
|---|---|---|
| #1761 | A XSS sanitize | DOMPurify on TemplateFullscreen+TemplateGrid SVG ('use client') + JsonLd escapeScriptContent; 6 SSG server pages reverted (isomorphic-dompurify jsdom SSG incompat); 11 unit tests pass |
| #1762 | B Enrollment capacity | V65 migration max_students+current_enrolled+CHECK; PESSIMISTIC_WRITE lock; concurrent IT 10 PASS+10 CLASS_FULL |
| #1763 | D Authz audit | CrossTenantAuthzTest 7/7 + CrossUserAuthzTest 9/9; 3 critical findings surfaced |
| #1764 | 4 gap follow-ups | GAP-727/728/729/730 filed |
| #1765 | Cleanup | 2 unused imports |

**Bucket C deferred via GAP-730** (Idempotency POST narrow — agent blocked content filter; re-spawn Wave br-2).

**3 Phase 1 BETA findings surfaced from D audit:** GAP-727 P0 (hasAccessToClass broken — full teacher lock-out, not IDOR) + GAP-728 P1 (TestSecurityConfig missing @EnableMethodSecurity) + GAP-729 P1 (11/19 controllers no per-resource authz guard).

Phase 1 BETA gate CANNOT clear yet — needs Wave br-2 ship Idempotency + hasAccessToClass + per-resource authz cluster.



### 🎉 Wave 107 SHIPPED 2026-05-24 — Đợt hybrid RST + 3 agent fix cụm thư parallel

4 PR merged ~3h: #1745 FIX-659 per-tone variants + invite-staff.html + 12 unit tests / #1744 FIX-543 email content audit + VN tone pass 5 templates / #1747 FIX-657 List-Unsubscribe + Reply-To + render verify @SpringBootTest / #1746 RST walk 7 flows (Mảng A 3/3 + Mảng B-onboard 3/4) + GAP-726 file (B2 wizard blank). RST findings 86% PASS; B2 → GAP-726 P1 defer Wave 108 workaround `/branding` standalone. VND `500.000₫/tháng` format `/pricing` chuẩn + VN locale `<html lang="vi">` + H1 chuẩn 9/9 KC dashboard routes (Tổng quan / AI Branding / Học viên / Giáo viên / Lớp học / Khóa học / Điểm danh / Hóa đơn / Cài đặt). 3 gap PARTIAL 95% giữ trạng thái per `gap-done-discipline.md` §3 Option A (live verify deferred gated GAP-612): GAP-543 (Wave 98 80% + Wave 107 15%) / GAP-657 (Wave 98 80% + Wave 107 15%) / GAP-659 (Wave 98 80% + Wave 107 15%). 3 outside-in audit spawned 04:00 UTC.

### 🎉 Wave thesis-1 SHIPPED 2026-05-23 — Thesis closure 6 bucket parallel + META wave-tag-numbering-convention v1.0.0 prereq

**First tag-based wave** (Wave 01-107 sequential grandfathered per rule §5 migration). Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 row 4 (Wave 100 audit 3-agent ≤30 ngày). META domain `meta-governance` per `post-wave-audit-mandate.md` §2.4.1 → NO AUDIT SUITE REQUIRED.

| PR | Bucket | Commit | Gap(s) closed | Key change |
|---|---|---|---|---|
| #1748 | plan + META §0 | `53f30e27` | (prereq) | `wave-tag-numbering-convention.md` v1.0.0 + skill section + matrix row + CSV row + plan + 3 gap Log defer |
| #1749 | B figure-curation | `644a3575` | GAP-651 DONE | Skill `thesis-figure-curation` + 4 INDEX per Ch.1-4 (baseline 27 visual / 0% caption → actionable list) |
| #1750 | A citation-extract | `bd855905` | GAP-647 + GAP-655 DONE | Skill `thesis-citation-extract` + extract/verify/self-test (real smoke: 31 matched / 1 orphan-body / 7 orphan-bib) |
| #1751 | F demo script | `f6b71ecb` | GAP-652 DONE (script-only) | Seed script 3-mode + 5-phút multi-tenant demo (UI/JWT + API 403 + RLS DB) |
| #1752 | C defense deck | `7f09a4ca` | GAP-653 DONE | Reveal.js 40 slide tiếng Việt + 4 Mermaid + 20 Q&A × 4 archetype + 15-phút demo + practice T-3+T-2 |
| #1753 | E cohort plan | `1d870e76` | GAP-623 DONE (doc-only) | `release-1-beta-cohort-plan.md` 9-tuần timeline + 4 GV persona + signed review template |
| #1754 | D V1 docx polish | `cc03d708` | GAP-689 DONE + GAP-687 PARTIAL 67% | 4 backup → archive + `create_thesis_v1.py --execute` + rubric 76/100 C+ PASS + SIGNOFF.md |
| #1755 | closure | (this PR) | — | Wave plan status:complete + wave-history mới format + ROADMAP entry + reconciliation + handoff |

**Defer Wave thesis-2** (3 gap, trigger restart = GAP-612 AWS restore + cluster live ≥7 ngày):
- GAP-648 NFR data capture (k6 + CloudWatch p50/p95 ≥30 ngày + AWS Cost)
- GAP-649 Beta cohort execution (≥4 nhận xét ký tay; 9-tuần timeline)
- GAP-687 Phase 3 (NFR + beta + Ch.5-7 evidence)

**Defense readiness:** 8-8.5đ ship-state (deck + Q&A + demo + bibliography + figures + V1 docx 76/100); 9-10đ chờ Wave thesis-2.



### 🎉 Wave 105 SHIPPED 2026-05-23 — Persona Walk + P0 Security Cluster (Beta-Readiness) 5/5 buckets + hotfix (PR #1727 / #1723 / #1725 / #1728 / #1724 / #1726)

**Sequential merge train** (per `concurrent-production-mutation-ops.md` serialize main mutations). Coordinator-driven; 5 bucket PRs already drafted by parallel agents prior session. Merge order: clean-first → CI-reactive fixes → cross-bucket conflict surface → inline hotfix → rebase + merge last 2.

| PR | Bucket | Commit | CI status at merge | Key change |
|---|---|---|---|---|
| #1727 | E Security P0 | `5dcdc2a8` | ✅ clean | 5 real-code bugs: PaymentController userId=1L hardcoded (B1/D1), beta-request XSS (A4), idempotency missing (A1), enrollment race (B5), per-resource authz (C3/D3 OWASP A01) |
| #1723 | C Teacher | `7ab14ab1` | ✅ clean | Per-class authz @authz bean + teacher endpoints + cross-class spoof IT 6 PASS |
| #1725 | B Owner | `be6f53e6` | ✅ clean (after CI fix push) | Owner walk + onboarding IMPORT_DATA dual-mode reframe + 3 follow-up gaps GAP-720/721/722. Coordinator fixes: CSV row Bucket B audit + GAP-722 phase-1-beta → phase-1.5-paid move + OnboardingChecklist test regex (stale label) |
| #1728 | 🔥 HOTFIX | `1fb853fb` | ✅ clean (3 commits) | Cross-bucket conflicts post-merge: GradeController duplicate @PreAuthorize (2 methods) + @Component("authz") collision (AuthorizationHelper broken-on-Postgres deleted, AuthorizationBean retained) + AttendanceClassBatchControllerIT bean swap |
| #1724 | D Parent | `aa2a40b1` | ✅ all-pass after rebase | Parent walk + VietQR idempotency + Zalo OA stub + multi-child authz 5/5 IT PASS. Coordinator fix: CSV row append + rebase on hotfixed main |
| #1726 | A Anonymous | `fbc75c7b` | ✅ all-pass after V55 dedup | Anonymous walk + XSS regex strip + HtmlEscape defense-in-depth + idempotency. Coordinator fix: drop duplicate V55 migration (Bucket A had `V55__beta_request_pending_email_partial_unique.sql`; main's V55 from Bucket E0 = `V55__beta_request_email_unique_pending.sql`, identical semantics; tests synced to main constraint name `idx_beta_request_email_unique_pending`) |

**Cross-bucket conflicts surfaced (META lesson):** Bucket E + Bucket E0 (prior session PR #1721) both added `@PreAuthorize` to GradeController methods + both added `@Component("authz")` bean (different schemas — AuthorizationHelper used non-existent `teacher_classes` table). Bucket C also added own variants. Pairwise diff per `pre-mutation-state-check.md` §1.5 would have caught BEFORE merge. Current process catches POST-merge via CI Test Core Service failure on next PR's run = 1 inline hotfix PR cycle saved by rule improvement.

**Vercel kiteclass FAILURE on 3/6 PRs (transient infra):** Vercel free-tier preview build rate-limit hit ("Deployment rate limited — retry in 24 hours"). Per `release-fix-retry-budget.md` §5 `RELEASE_RETRY_TRANSIENT` trailer applied; backend merge non-blocking.

**Audit obligations (GAP-716 deadline 2026-05-25):** `AUDIT_OVERRIDE: api-contract-audit deferred to GAP-716 batch` trailer applied to all 6 PRs. Combined Wave 104.5 + Wave 105 post-wave audit suite (business-logic + api-contract + ops-readiness) due ≤ 2 days from now.

**Side-tasks completed mid-merge:** Restart kite local Docker stack 13/13 healthy (drop+recreate `kiteclass_shared` DB + workaround V25 alter-branding bug via pre-create empty `branding` table; tracked separately as pre-existing local-stack bug).

**Coordinator merges:** 6 PRs all via `gh pr merge --squash --delete-branch`, no `--admin` bypass (per `admin-merge-discipline.md`). Local verify `./mvnw -P strict-warnings compile/test-compile` exit 0 on hotfix branch + Bucket A V55 dedup branch.



Wave 103 SHIPPED 6-bucket local self-test full walk (PR #1709 commit `345b4c0b`). **4 gaps DONE 100% local** (GAP-637/620/518/519 admin RBAC walk + OWASP A01 fix) + **8 gaps PARTIAL revised** (GAP-516/531/538/543/657/659/693/695) + **6 follow-up gap files filed** (GAP-702..707 — 3 P0 + 2 P1 + 1 P2 — Bucket A/B/C/D real bugs surfaced). Wave 104 plan drafted on `wave/104-fix-followup-bugs` branch (5 buckets parallel A JWT + B email + C 2FA gateway + D audit log + E re-self-test) — agent spawn deferred to next session for context budget preservation. AWS account 906286017800 verification in progress 2026-05-22 (Ginnette re-sent secure link 04:00 UTC; user uploaded MasterCard ending 53 statement; awaiting AWS verification team 24-48h). Next session pickup options: (a) Execute Wave 104 (~2-2.5h critical path local-only — fix 6 follow-up bugs); (b) Wait AWS restoration → Wave 104 + Wave 105 prod live verify cluster; (c) Wave 103 post-merge audit suite api-contract + ops-readiness deadline 2026-05-25 (GAP-708 P1); (d) 01-business/auth docs sync (GAP-709 P1 Living Docs rule).

### 🎉 Wave 103 SHIPPED 2026-05-22 — Local Self-Test Full Walk + 6 Real Bug Findings (PR #1709 squash `345b4c0b`)

**6-bucket parallel** (E inline coordinator + 5 background agents A/B/C/D/F via worktree isolation): E stack-up smoke 13/13 containers healthy + branding REDIS_HOST env fix; A admin walk Opus medium 7/7 IT PASS + class-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` backfill + caught real OWASP A01 (OWNER had access /admin/dashboard pre-fix); B owner walk Opus full surfaced GAP-704 JWT tenantId claim missing post-beta-signup; C 2FA TOTP Opus full end-to-end PASS direct port 8081 surfaced GAP-705+706 gateway/subscription challenge-token bridge missing; D email Mailhog Sonnet (inline re-handled after sub-agent autocompact thrash) revealed GAP-702 approval-email-not-firing + GAP-703 List-Unsubscribe + multipart/alternative MISSING despite Wave 98 B1 claim shipped; F AWS rebuild SOP Sonnet 551-line VN playbook 13 step + 5 gate + 8 FM (inline re-committed after sub-agent thrash). Coordinator CI fixes: audits-index CSV 5 rows + wave-plan-completeness 4 missing sections + gitleaks TOTP secret redact + .gitleaksignore. **3 production-impacting bugs would have shipped to beta without local self-test** (OWASP A01 + JWT tenantId + approval email pipeline) — self-test pattern validated as force-multiplier. **Side-effect**: admin@kitehub.com role upgraded ADMIN→PLATFORM_ADMIN in shared local DB; kitehub-branding REDIS_HOST env var added to compose. **Coordinator post-merge**: 12 gap status flips (4 DONE + 8 PARTIAL revisions) + 2 follow-up gaps GAP-708/709 + 4 DONE gaps git mv → phase-1-beta/closed/ + wave-history append + ROADMAP sync + session-handoff. **Audit-gate.py hook compliance 1/5**: flagged missing api-contract + ops-readiness audits (deadline 2026-05-25 GAP-708) + business logic changed without 01-business/auth/* sync (GAP-709). PR: #1709 squash merge 11 commits → main `345b4c0b`.

### 🚀 Prior Action 2026-05-21 — GAP-612 AWS restoration check + Wave 102.9 post-wave audit suite (cadence 2026-05-24)

Wave 102.9 batch 2 SHIPPED 4/4 buckets as **docs-only state-check PARTIAL** — fix-time state-check per `audit-to-gap-pipeline.md` §2.8 revealed all code-AC already shipped in prior waves (72b/92/97/98 B1); remaining AC all live verify blocked GAP-612 AWS suspension. **8 PARTIAL gaps standby for batch DONE flip post-AWS-restoration** (GAP-516 2FA + GAP-531/538 tenant init/onboarding + GAP-543/657/659 email + GAP-620/637 admin RBAC). Next session pickup options: (a) Wait GAP-612 AWS restoration (D+4=today, Support case 177903869600100; D+7=2026-05-24 escalate Twitter @AWSSupport; D+8=2026-05-25 evaluate new account; deadline xoá account=2026-06-01) → ~30min batch live verify cluster → 8 gaps DONE; (b) Wave 102.9+102.8 consolidated post-wave audit suite per `post-wave-audit-mandate.md` §2.4 release-deploy-artifacts domain milestone deadline 2026-05-24 (3-day cadence — Security /100 + Ops Readiness /100); (c) Wave 102.10 planning (post AWS-restore: GAP-693 AWS rebuild SOP + GAP-269b/138/139/658 Tier 3).

### 🎉 Wave 102.9 batch 2 SHIPPED 2026-05-21 — Self-Test Completion docs-only state-check (4/4 buckets PARTIAL, all live verify GAP-612 blocked)

**5 PRs ship sequence** post agent-spawn pattern failure (3 sonnet autocompact + 2 opus rate-limited → coordinator foreground Opus 4.7 1M completed all 4 buckets). Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check: all 4 buckets' code-AC reality-mismatched vs original wave plan §3 Scope — code already shipped Wave 72b BE+FE 2FA / Wave 92 Bucket D admin v1 / Wave 97 Bucket A @PreAuthorize backfill / Wave 98 B1 email layer hardening. Bucket E (#1699 pre-session GAP-699 gateway JWT_SECRET compose passthrough — DONE). Bucket A (#1705 GAP-637 PARTIAL 60% + GAP-620 OPEN retained, admin RBAC code intact via 3 controllers @PreAuthorize + 6 @WithMockUser security tests). Bucket B (#1703 GAP-531+538 PARTIAL retained — agent state-check found TenantInitController never existed; canonical flow = POST /api/v1/admin/beta-requests/{id}/approve + signup token; FE checklist 244 LOC + V43 migration + Playwright e2e + VN seed worker all shipped). Bucket C (#1706 GAP-516 PARTIAL 80% retained — 12/16 AC shipped Wave 72b TwoFactor* services + V37 + 16 tests + 2fa-setup/challenge pages). Bucket D (#1707 GAP-543/657/659 PARTIAL 80% retained — Wave 98 B1 shipped 5/5 critical templates .txt + SESEmailService:258 Reply-To + ResendEmailService:119-122 List-Unsubscribe + List-Unsubscribe-Post + Tone.java FORMAL_SAFE_DEFAULT + EmailTemplateRenderer). Plus #1704 chore PR-1702.json log sync. Hotfix shipped commit `7511e23d` (ShellCheck job idempotent install — self-hosted runner sudo NOPASSWD limitation). 4 state-check audit artifacts shipped `audits/persona-review/2026-05-21-wave-102.9-bucket-{a,b,c,d}-*-state-check.md`. 0 `--admin` merges; all 5 PRs CI 22/22 SUCCESS. PR: #TBD closure (this PR with ROADMAP + wave-history + handoff sync).


### 🎉 Wave 102.8.1 SHIPPED 2026-05-21 — Admin Browser Walk Verify Post FE Rebuild Check (GAP-518 DONE + GAP-519 DONE + GAP-699 filed)

**Mini-PATCH single PR ship** post Wave 102.8 closure: agent walk §2.4 (a)→(g) qua curl headless verified FE image `:latest` thực ra FRESH (built 04:54 UTC today, KHÔNG phải stale `gap-284-test` như Wave 102.8 Bucket D giả định) → KHÔNG cần rebuild. Agent staged commits bị user kill mid-task (tới bước "Now let me update the CSV") do user thắc mắc "sao agent vẫn đang chạy"; coordinator inherit agent's work (3 artifact: audit + GAP-699 file + git mv 2 gaps) + sync CSV (GAP-518/519 → DONE, GAP-699 OPEN P1 row append) + commit + PR #1697. §2.4 result: 5 PASS (a/b/d/e/f) + 2 PARTIAL ((c) browser interactive code-cover Wave 101 A 27/27 unit tests; (g) gateway-blocked tracked GAP-699 separate scope per GAP-518 AC text). Audit `documents/04-quality/audits/local-stack/2026-05-21-wave-102-8-1-browser-walk-verify.md` 12 read-only commands + 3 findings (#1 gateway JWT_SECRET passthrough missing — Wave 89 Bucket A GAP-604 regression / #2 Postgres volume password drift workaround / #3 FE image fresh assumption correction). GAP-699 OPEN P1 DevOps — 2-line compose edit ~5-10min, promoted Wave 102.9 Bucket E SEQUENTIAL FIRST. PR: #1697 closure (commit `0238d5ef`).

### 🎉 Wave 102.8 SHIPPED 2026-05-21 — Self-Test Readiness Foundation (GAP-694 DONE + GAP-692 PARTIAL 33% + GAP-695 PARTIAL 50% + GAP-481 DONE + GAP-518 PARTIAL 99% + GAP-519 PARTIAL 90%)

**4-bucket parallel + 1 closure PR ship sequence** (post outside-in synthesis 2026-05-21 — 3 agents F-1 + E-1 + P-1 produced 4 gaps GAP-692/693/694/695): Bucket A (PR #1691) Docker preflight + .env populate — `kitehub/scripts/check-docker.sh` shipped + integrated into `up.sh`/`setup.sh`, Docker Desktop launched via `powershell.exe Start-Process`, `.env` populated 20/20 keys, `up.sh --profile infra-only` verified 4 services healthy (postgres+redis+rabbitmq+minio). Bucket B (PR #1692) env-reference tooling — `documents/02-architecture/env-reference.yaml` 10 rows × 3 envs + `scripts/render-env-vars.sh` + `scripts/check-unresolved-env-vars.sh` + `.claude/rules/markdown-variable-reference.md` v1.0.0 + TF `var.domain_name` STALE fix `kiteclass.com` → `kitehub.me` + `var.aws_account_id` + `var.secrets_prefix` + CI job `env-vars-render`. Bucket C (PR #1693) codify rule — `.claude/rules/local-self-test-before-aws-deploy.md` v1.0.0 với 11 sections + 3-scenario fixture + worked self-test Wave 71b retroactive. Bucket D (PR #1694) live admin Tier 1 — curl POST `/api/auth/login admin@kitehub.com/Admin@KiteHub123` via gateway port 9000 → HTTP 200 + JWT `role:"ADMIN"`; gateway routing `/api/v1/admin/*` returns 400 (NOT 404) closes GAP-481; Sidebar.tsx 4 admin testid'd links verified code-side. Bucket B trigger `terraform-plan` CI OIDC failure (pre-existing infra issue, 2 consecutive runs failed; AUDIT_OVERRIDE trailer added to PR #1692 + GAP-698 P1 META filed tracking ops-readiness audit deferred Wave 102.9 milestone). 4 bucket PRs + 1 plan PR (#1690) + 1 closure PR (this PR) = 6 total. Wall-clock A 8.6min + B 14.9min + C 8.1min + D 18.2min (Bucket D longest do FE image rebuild + stack-up retries) — all background parallel per `agent-background-spawn-default.md`. **GAP-693 (AWS rebuild SOP playbook)** defer Wave 102.9 milestone per user direction (hard-blocked GAP-612 AWS suspension + GAP-694 DONE + GAP-692 Phase 1 DONE prereqs satisfied by this wave; OIDC fix prereq tracked GAP-698 also Wave 102.9). All §3 Scope items + coordinator AUDIT_OVERRIDE reconciled ✅ per `wave-closure-scope-completeness.md` §3 table in closure PR body. PR: #TBD closure (this PR).

### 🎉 Wave 102.7.6 SHIPPED 2026-05-21 — Thesis V1 Final Polish (GAP-697 17+ residual misses, 2-bucket parallel A/B)

**2-bucket parallel ship sequence** (post Wave 102.7.5 closure docx grep audit phát hiện residual lược-bỏ-miss): Bucket A (PR #1687) Ch.1 MD scrub — `chapter-1-competitor-analysis.md` + `chapter-1-vn-law-methodology.md` jargon scrub 6 hits (`giai đoạn beta` → `thử nghiệm` / `paid beta` → `thanh toán thử nghiệm` / `production launch` → `vận hành chính thức`) + Ch.1 personas reframe 5 hits (lines 109/111/113/115/117 drop `Nhóm N — ` prefix, preserve italic + persona name + English label + cites). Bucket B (PR #1688) pipeline scrub `create_thesis_v1.py` — jargon 9 inline + MỞ ĐẦU §3 listing rewrite (3-layer narrative grouping consistent với Wave 102.7.5 Ch.2 §2.3.5 precedent) + Wave 103+ repo jargon neutral language + Anthropic Claude bibliography drop (canonical source `references/bibliography.md` NOT inline array; 39 → 38 entries; orphan [15] citation in `chapter-1-ai-techniques.md` line 66 dropped; 24 cross-refs renumbered across 5 chapter MDs). Coordinator post-merge hotfix wrapped-string line 1258-1259 (Bucket B agent missed contiguous match across Python concatenation) + re-bake `thesis-v1.docx` PASS (646 paragraphs, 4 sections, 38 bibliography; comprehensive grep verify ALL PASS: jargon = 0, Nhóm N — = 0, microservice listing = 0, Wave 103+ = 0, Anthropic Claude = 0). Wall-clock ~25 min (2 agents background; no rate-limit recurrence; worktree path footgun mitigation worked per Wave 102.7.5 lesson). GAP-697 DONE 100% + git mv to `phase-1-beta/closed/`. Out-of-scope deferred Wave 102.7.7+ candidate: `chapter-1-ai-techniques.md` 3 `giai đoạn GA` hits (NOT loaded by pipeline; low priority). All 7 §3 Scope items + coordinator re-bake reconciled ✅ per `wave-closure-scope-completeness.md` §3 table in closure PR body. PR: #1689 closure (this PR).

### 🎉 Wave 102.7.5 SHIPPED 2026-05-21 — Thesis V1 Deferred Cleanup (GAP-696 8 items, 3-bucket parallel A/B/C)

**3-bucket parallel ship sequence** (post Wave 102.7.4 rate-limit recovery): Bucket A (PR #1684) Ch.2 narrative reframe — 6 `Nhóm N — ` separator drops + service catalog listing rewrite per S3 v1.1.0 narrative grouping (3 layers KiteHub platform + KiteClass tenant + FE applications). Bucket B (PR #1683) pipeline cleanup `create_thesis_v1.py` — Phụ lục A REMOVE + Phụ lục B inline GitHub link trong KẾT LUẬN §1 + Phụ lục C REMOVE + ABC sort terms (10) + abbrevs (26) VN-aware + `add_appendix()` function deleted entirely + step counter synced "[8/8] Tài liệu tham khảo IEEE". Bucket C (PR #1682) figures/ folder + bìa personal data verify audit — `documents/08-thesis/figures/README.md` 67 lines per `docs-folder-structure.md` §3 template + audit `2026-05-21-bia-pipeline-personal-data-verify.md` verdict ALIGNED ✓ (8/8 fields match canonical `student-info.md`, no drift). Coordinator post-merge re-bake `thesis-v1.docx` PASS (647 paragraphs, 4 sections, -136 bytes vs pre-wave reflecting Phụ lục A+C removal). Pre-spawn meta cleanup PR #1681 narrowed 5 docs-scaling rules `paths:` frontmatter → wave-plan-read auto-load reduced 14→~9 rules. Wall-clock ~30 min (3 agents background parallel; rate-limit not recurred). GAP-696 DONE 100% + git mv to `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0. All 7 §3 Scope items + coordinator re-bake reconciled ✅ per `wave-closure-scope-completeness.md` §3 table in closure PR body. PR: #1685 closure (this PR).

### 🎉 Wave 102.7.4 SHIPPED 2026-05-21 — Thesis V1 Project-jargon Scrub (32 hits → 0 across 3 chapters)

**Consolidated single bucket (3-parallel rate-limited, salvage approach):** Coordinator finished project-jargon scrub Ch.2 (22→0) + Ch.3 (1→0) + Ch.4 (9→0) = **32/32 hits BETA/GA/Phase → 0** academic synonyms (giai đoạn thử nghiệm / giai đoạn vận hành chính thức / nhóm tenant thử nghiệm / Tenant Requests / APPROVE_TENANT_REQUEST enum + 14 context-specific patterns). thesis-v1.docx re-baked clean, **653 paragraphs preserved**. Closes action-2.md §4 line 38 inside missed ("giảng viên không nhận GAP ID/wave"). PR: #1677 consolidated (after rate-limited 3-bucket parallel from PR #1676 closed superseded). **Deferred Wave 102.7.5 (GAP-696 filed):** 8 items — Bucket A task A2 (Nhóm separator) + A3 (listing rewrite) + Bucket C Phụ lục A/B/C cleanup + ABC sort + figures folder + personal data lookup đề cương.

---

### 🎉 Wave 102.7.3 SHIPPED 2026-05-20 — Thesis V1 Academic Integrity P0 (citations + measurement methodology + RLS rubric)

**3 buckets parallel ~2h:** Bucket A Ch.1 §1.3 vendor stats citation 8 blocks (F-A1-01..04 + P1-03) — page-num + URL+access-date evidence ([2] Magenest / [3] 6Wresearch / [4] VECITA / [5] MISA / [6] Mona / [7] Easy Edu / [8] DotB / [26] BeeClass); web-only sources use `[N] (truy cập DD/MM/YYYY)` (no fabricated page-nums). Bucket B Ch.4 §4.3.4 measurement methodology block (P2-01/02/09 + F-B4-01) — 3 KPI reframed Sơ bộ → Ước tính (Uptime per SLA) / Probe-endpoint sample (P95 latency) / Single audit (Lighthouse) + explicit tool/N/date/env caveat. Bucket C Ch.2 §2.2.3 Bảng 2.6 RLS scoring rubric (P2-03 + F-B1-01) — 1-5 scale + AWS SaaS Lens [27 tr.21] + Pothon [28] source anchor + author-defined rubric ownership + Pattern 4 per-trục rationale 3+5+4+5+4+5=26 self-consistent. **Audit handoff numbering correction:** Wave 102.7.0 audit said "Bảng 2.4 RLS" — actual is **Bảng 2.6** post Wave 102.7.2 restructure. thesis-v1.docx re-rendered single coordinator pipeline bake → **653 paragraphs** (Wave 102.7.2 631 → +22 net). PRs: #1663 plan + #1666 A + #1664 B + #1665 C + #1667 closure. LibreOffice headless unavailable — manual Word F9 pre-defense per Wave 102.6 Bucket D fallback. **14 P0 academic-integrity findings closed.** Follow-up GAP-691 filed for post-wave audit suite cadence ≤2026-05-23. Coordinator lesson: 3 agents over-created `audits/thesis/` subfolder + README; recovery via per-branch rebase + git mv to existing `persona-review/` category per CSV enum.

---

### 🎉 Wave 102.7.2 SHIPPED 2026-05-20 — Thesis V1 Content P0 (Ch.2 rename + Ch.3 shared-flow + Ch.4 §4.2 reframe + cross-ref + math)

**4 buckets parallel ~1h:** Bucket A Ch.2 rename `KIẾN TRÚC` → `PHÂN TÍCH THIẾT KẾ HỆ THỐNG` + H2 restructure (2.1 Phân tích / 2.2 Thiết kế tổng thể / 2.3 Thiết kế chi tiết) + 13 internal cross-ref sync + math F-B2-02 17 (not 18) + F-B2-03 HikariCP 60 (not 70). Bucket B Ch.3 screenshot **9→4 composite figures** (Hình 3.1 Discovery+Onboarding 4 screens / 3.2 Daily ops 2 / 3.3 Admin ops 2 / 3.4 Test pyramid) + S8 source attribution 100% + ~4-5 trang saved. Bucket C Ch.4 §4.2 reframe (65 lines User Onboarding → placeholder "Kết quả end-user pre-defense") + §4.0 drop per S2 + F-B4-02 math $13 → $7.38 + cross-ref F-C5-04/05 (Ch.3 §3.4/§3.5 anchors broken → fix to Ch.2 §2.3.4 RLS + drop Outbox cite) + P2-08 §2.5 outside-in drop. Bucket D B1-08 bibliography rename `DANH MỤC TÀI LIỆU THAM KHẢO` → `TÀI LIỆU THAM KHẢO` (UTC convention). thesis-v1.docx re-rendered 4 sections / **631 paragraphs** (Wave 102.7.1 673 → -42 từ trim Ch.3 + Ch.4). PRs: #1656 D + #1657 B + #1658 C + #1659 A. Net ~5-7 trang saved.

---

### 🎉 Wave 102.7.1 SHIPPED 2026-05-20 — Thesis V1 Structural P0 (4 new pages + bìa + đại từ + H1 wraps)

**2-bucket file-disjoint design:** Bucket P (pipeline coordinator) — 7 fixes single edit pass `create_thesis_v1.py` +348 -32 (Bìa underscore via `add_horizontal_line()` XML pBdr helper + bỏ Chuyên ngành/Năm 9→7 fields + đại từ tôi→em pipeline narrative + B1-01 NHẬN XÉT GVHD page + B1-02 LỜI CAM ĐOAN page ~220 từ standard VN academic oath + B1-03 TÓM TẮT VI + ABSTRACT EN standalone 200-300 từ + B1-07 MỞ ĐẦU H1 wrap + B1-09 KẾT LUẬN H1 wrap). Bucket M (chapter MDs) — đại từ tôi→em sweep found only 2 hits Ch.2 (Wave 102.5 prior already consolidated). thesis-v1.docx 616 → **673 paragraphs (+57)** từ 4 structural pages. New frontmatter order: Bìa chính → Bìa phụ → NHẬN XÉT GVHD → LỜI CAM ĐOAN → TÓM TẮT → ABSTRACT → LỜI CẢM ƠN → MỤC LỤC → MỞ ĐẦU H1 → CHƯƠNG 1-4 → KẾT LUẬN H1 → TLTK. PRs: #1653 Bucket M + #1654 Bucket P.

---

### 🎉 Wave 102.7.0 SHIPPED 2026-05-20 — META thesis-content-standard.md v1.0.2 → v1.1.0 (8 new rules)

**Single PR ~30min:** 3 user rules (S1 single-child heading ban + S2 cấm chapter intro/summary + S3 ngôn ngữ tiếng Việt 100% narrative) + 5 outside-in META (S4 citation evidence + S5 measurement methodology + S6 cross-ref integrity + S7 acronym first-use + S8 figure source attribution). Triggered by 3-agent outside-in audit 2026-05-20 (Agent 1 Persona 41 + Agent 2 UTC Benchmark 13 + Agent 3 Failure-mode 28 = 82 NEW findings beyond 14 user items). Coverage: ~37 P0 + ~30 P1 cluster patterns. Audit artifact `AUDIT-2026-05-20-wave-102.7-outside-in-consolidated`. PR: #1652.

---

### 🎉 Wave 102.6 SHIPPED 2026-05-20 — Thesis V1 Phase 1+2 shortcut (GAP-689 Phase 1+2 DONE; Phase 3+4 PLANNED defer)

**4 parallel buckets ~75min agent + ~20min coordinator:** User direction 2026-05-20 lock GAP-689 scope — G5 WONTFIX (bibliography 39 entries đủ), G6 active via LibreOffice headless bake (không chỉ document), Phase 3+4 PLANNED defer indefinite. 4-bucket file-disjoint design: A=Ch.1 G9 figure cite / B=Ch.2 G4+G9 sweep / C=Ch.4 G4+G9 sweep / D=create_thesis_v1.py LibreOffice bake. **Surprising over-spec finding:** 3 of 4 buckets reported "0 chapter edits — already compliant from Wave 102.5 Bucket A PR #1628 first-pass". Only 1 real fix shipped (Bucket B Ch.2 Line 311 AWS SaaS Lens `[27, tr.21]` page-num add); Bucket A + C produced audit artifacts documenting compliance evidence. Bucket D PR #1650 ship `auto_populate_fields()` defensive function (`subprocess.run` with 3 graceful-fail paths) + README §4 prereq + §7 LibreOffice/Word fallback. PRs: #1646 GAP-689 scope revision + #1647 wave plan + #1648 Bucket C audit + #1649 Bucket B fix + #1650 Bucket D LibreOffice bake. GAP-689 flipped PARTIAL completion_pct=50.

---

### 🎉 Wave 102.5 SHIPPED 2026-05-20 — Thesis V1 fix bundle (11 user items + 19 audit gaps; 11 deferred GAP-689 Wave 102.6)

**5 parallel buckets + 1 coordinator + 1 plan patch:** Bucket B foundation sweep (terminology+pronoun+jargon+phase) → A/C/D/E parallel post-B → A coordinator-style replace stale #1626. Per `wave-closure-scope-completeness.md` §3 reconciliation: 9 P0 + 10 P1 audit gaps bundled (G1 KẾT LUẬN VÀ KIẾN NGHỊ + G2 DANH MỤC TLTK + G3 Vancouver renumber + G4 [N, tr.NNN] + G5 source mix + G6 TOC F9 + G7 UML/ERD/Use Case CO-EXIST with C4 + G8 caption hình DƯỚI + G9 Nguồn cite + G10 Mở đầu 2 trang + G11 §1.1+§1.2 intro + G13 testing detail + G14 thuật ngữ ≥15 + G15 numbering 1.X.Y.Z + G16 style verify + G17 page numbering 3-section + G18 DB table format + G19 bìa phụ 6-field + G22 screenshot source cite + G25 acknowledgement 3-part). 11 remaining → GAP-689 Wave 102.6. Decisions locked via AskUserQuestion 2026-05-20: pronoun=tôi / system modeling=C4+UML co-exist / DB design=full ERD+schema / scope=9P0+10P1 bundle. PRs: #1622 plan + #1623 patch + #1624 Bucket B + #1625 Bucket D + #1630 Bucket C + #1632 Bucket E + #1628 Bucket A. Audit `AUDIT-2026-05-20-wave-102-5-khung-chuan`. **Defense-ready maintained post-Word-F9**.

---

### 🎉 Wave 102.4 SHIPPED 2026-05-20 — Thesis V1 ≥95 A TARGET HIT (95/100 A raw / 97/100 A post-Word-F9 ship-ready)

**Single coordinator PR ~1h:** citation order renumber by first-appearance (97 ref renumbers + 9 orphan refs dropped → bibliography sequential 1-39) + 33 hyperlinks blue+underline + binding gutter 0.5cm + typo polish (Khoa + năm năm) + Word F9 pre-defense ship instruction in `08-thesis/README.md`. Audit `AUDIT-2026-05-20-thesis-v1-wave-102-4-polish-docx`. **Defense-ready post-F9.** Pre-defense ceiling 99/100 via C8 real KPI + beta feedback (Wave 110+).

---

### 🎉 Wave 102.3 SHIPPED 2026-05-20 — Thesis V1 polish path to ≥95 A (~91.5/100 A-, +5 vs Wave 102.2)

**4 PRs merged:** #1616 plan + Bucket D drop + #1617 Ch.4 (bullet 43%→0% + trim §4.4) + #1618 Ch.3 (trade-offs trim) + #1619 Ch.2 (§2.4/§2.5 trim + orphan `[49]`→`[34]` VECITA fix). Bucket D bibliography UTC giáo trình refs DROPPED per user direction "giáo trình UTC không cần thiết". DOCX re-rendered 1.3MB / ~72 trang (within 60-80 cap, down từ ~94-96). 10 page-num cites `[N, tr.X]` added across 3 chapters. Audit `AUDIT-2026-05-20-thesis-v1-wave-102-3-polish-docx`.

---

## 🎯 Previous Status Snapshot (2026-05-19 — Wave 102.2 SHIPPED thesis V1 ~90.5/100 A- + Wave 102.1 fix-pass + Wave 102 META + Wave 102 GAP-688 + Wave 101 + Wave 100.7)

### 🎉 Wave 102.2 SHIPPED 2026-05-19 — Thesis V1 polish (8 user fixes + Mermaid PNG + Công nghệ section)

Wave 102.2 closes 8 user-flagged issues post Wave 102.1 review qua **3 parallel agents** + outside-in research. Final score estimate ~90.5/100 A- (+8 vs Wave 102.1 82.5 B-).

| Issue | Fix | Status |
|---|---|---|
| 1. Mermaid as code text | PNG render pipeline kroki.io HTTP API + cache | ✅ 11 Mermaid PNGs embedded (12 inline shapes total with logo) |
| 2. Icons ✅/✗/❌/⚠️ + arrows | Strip all + rule v1.0.2 No-icon principle | ✅ 54 icon + 49 arrow hits → 0 |
| 3. Khảo sát reorg + cut tables | Ch.1.1 174→70 (-60%); 4 tables → 1 + add §3 Khảo sát nhu cầu end-user 5 personas | ✅ Agent 7 done |
| 4. Tách 2 danh mục riêng | Danh mục THUẬT NGỮ + Danh mục TỪ VIẾT TẮT → 2 H1 riêng biệt | ✅ Coordinator script edit |
| 5. No font swap inline code | `add_inline_runs()` TNR italic (NOT Courier New 11pt); rule v1.0.2 No-font-swap principle | ✅ 0 Courier New runs |
| 6. BRD Ch.2 (outside-in) | Verdict: NOT mandated by UTC; §2.1 FR + §2.2 NFR sufficient | ✅ No change needed |
| 7. Công nghệ section (outside-in) | MANDATORY per UTC blank template; ADD Ch.3 §3.1 6 sub-sections | ✅ Coordinator add ~60 lines |
| 8. Cut Phần A+B Ch.1.3 (outside-in) | COMPRESS (not cut); Ch.1.3 171→81 (-53%) preserve C8+C9 categories | ✅ Agent 8 done |

**PR:** #1614 (commit `a94ceaf6`). Rule v1.0.2 PATCH same-PR. CI 21/22 SUCCESS (1 Vercel cosmetic GAP-495).

### 🎉 Wave 102.1 SHIPPED 2026-05-19 — Thesis V1 fix-pass (42→82.5/100 B-, 6 parallel agents)

Wave 102.1 closes 7 user-flagged + 39+ persona-simulation findings via 6 parallel chapter agents (~50 min wall-clock vs 3-4h serial). Score 42→82.5 (+40.5). PR #1613 (commit `b896ad9b`). Rule v1.0.1 standalone-document principle same-PR.

### 🎉 Wave 102 META SHIPPED 2026-05-19 — thesis-content-standard.md v1.0.0 9-category rubric

Rule META closes coverage gap exposed bởi Wave 102 GAP-688 audit (rubric v1 82 inflated vs v2 42 baseline; -40 delta). PR #1612 (commit `1b93e325`). 9-category rubric grounded UTC spec + BAO_CAO sample + DE_CUONG sample + 43 persona findings + 7 user issues.

### 🎉 Wave 102 GAP-688 SHIPPED 2026-05-19 — Thesis V1 Python Pipeline (production V1 milestone)

Wave 102 Phase 1 = direct response to user mandate "ưu tiên tuyệt đối cho V1 chuẩn" post Wave 101 closure. Python pipeline `create_thesis_v1.py` (~900 LOC) ships production V1 `thesis-v1.docx` thay thế pandoc draft (60/100 D+), đạt **82/100 B-** trên 6-category rubric (+22 points, exceeds target ≥75 C+ by +7).

| Deliverable | Path | Notes |
|---|---|---|
| Python script | `documents/08-thesis/create_thesis_v1.py` | ~900 LOC; placed inline per sister-pattern compliance với `documents/07-archived/academic/word-reports/{bao-cao-thuc-tap,de-cuong-datn}/` |
| Output DOCX | `documents/08-thesis/thesis-v1.docx` | ~300 KB; 1710 paragraphs, 36 tables, 44 bibliography entries, A4 + TNR + UTC margins |
| Re-audit report | `documents/04-quality/audits/persona-review/2026-05-19-thesis-v1-python-pipeline-docx-audit.md` | 82/100 B- (delta +22 vs baseline pandoc draft) |
| Pandoc draft archive | `documents/07-archived/thesis-drafts/2026-05-19-thesis-v1-draft-pandoc-superseded.docx` | superseded; preserved for retro |
| GAP-688 → DONE + closed/ | — | All 12 AC checked; `git mv` to `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 §3.3 |

**Approach decisions (per user direction):**
- Custom Python markdown parser inline (NOT pandoc subprocess) — full control over UTC font/margin compliance
- venv `documents/08-thesis/.venv/` (gitignored) — `python-docx 1.2.0 + lxml 6.1.1`
- Script reuses ~600 lines styling helpers from `create_bao_cao_thuc_tap.py` (cover, headings, tables, IEEE refs); ~300 lines new code (markdown parser + thesis-specific cover + chapter loader + bibliography parser)

**Audit delta breakdown (6 categories):**
- C1 Format compliance 3→13/15 (+10) — A4 + TNR + margins + formal cover + TOC field
- C2 Content completeness 12→13/15 (+1) — intro + conclusion + abbreviations added
- C3 Bibliography 8→13/15 (+5) — IEEE hanging indent + italic markdown rendering
- C4 VN narrative 14→15/15 (+1) — cover Vietnamese formalized
- C5 Cross-ref 13→14/20 (+1) — 36 tables render properly (vs 2 pandoc); figure refs still pending GAP-651
- C6 Examiner readiness 10→14/20 (+4) — formal cover page replaces pandoc no-cover

**Follow-up gaps tracked (Wave 102+ scope):**
- GAP-687 Phase 1 — scrub 22 TODO + 5 placeholder trong chapter MDs
- GAP-651 — image curation + figure injection (extend `create_thesis_v1.py` to render `![alt](path)`)
- GAP-648 — NFR + KPI real data capture (replace Ch.4 placeholders)
- GAP-649 — beta cohort findings aggregation (9-week timeline started 2026-05-19)
- Table caption SEQ numbering follow-up (Wave 103+)
- Mermaid diagram rendering follow-up (Wave 103+)
- Meta-rule `.claude/rules/thesis-content-standard.md` future (Wave 105+ codify 6-category rubric)

**Defense window timeline:** 2026-08-15 → 3-month buffer cho follow-up gaps + Wave 110+ defense prep (GAP-653).

---

### 🎉 Wave 101 SHIPPED 2026-05-19 — Product Demo-Blockers cluster (4 buckets close-out)

Wave 101 = direct continuation of Wave 100.7 thesis V1 closure into Track D Product demo-blockers per `release-1.5-thesis-scope.md` §2. User direction "ship 4-bucket plan now, accept PARTIAL exits if AWS GAP-612 blocks live walk".

| Bucket | Gap | Verdict | PR |
|---|---|---|---|
| A | GAP-518 admin role BE/FE | 🟡 PARTIAL 95→97 (live walk gated GAP-684/GAP-612) | #1603 |
| B | GAP-562 + GAP-562b kitehub-branding @PreAuthorize | ✅ DONE 90/85→100 (spring-security + 4 controllers + 7 IT) | #1607 |
| C | GAP-287 wizard "Sử dụng mặc định" | ✅ DONE 0→100 (10 unit + 3 Playwright; FE-only clean) | #1605 |
| D | GAP-538 onboarding seed + E2E spec | 🟡 PARTIAL 90→95 (live walkthrough gated GAP-612) | #1604 |
| — | thesis-v1-draft.docx pandoc convert (side-quest) | ✅ shipped 113K (60/100 D+ self-audit) | #1606 |
| plan | Wave 101 plan PR | — | #1602 |

**Bucket B fix cycle:** SecurityConfig broke 5 pre-existing `@WebMvcTest` classes (11 test failures). 2 fix commits `a967249f` + `7c28c2f3` added `@AutoConfigureMockMvc(addFilters = false)` to 4 broken test classes. Final state 31/31 CI PASS.

**3 follow-up gaps filed (this closure PR):** GAP-685 (Wave 101 audit suite — deadline 2026-05-22 per `post-wave-audit-mandate.md` §2.2) / GAP-686 (3-layer business doc sync kitehub-branding RBAC) / GAP-687 (thesis V1 DOCX audit 3-phase follow-ups).

**Cleanup:** 4 worktree husks + 3 merged branches pruned via `scripts/prune-merged-worktrees.sh` per `post-wave-cleanup.md`. Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3 in wave plan §7.1.

---

### 🎉 Wave 100.7 Phase 3b V2 SHIPPED 2026-05-19 — Full DOCX pipeline (GAP-646 DONE)

Phase 3b implementation pivoted Edit-Fill → **Create pipeline** because LibreOffice/Word not on-stack. ThesisReportBuilder mirrors `TeacherContractBuilder` Wave 5 pattern — POI XWPF programmatically builds thesis skeleton without binary template.

| Deliverable | Path | Lines |
|---|---|---|
| ThesisReportBuilder Java (POI Create pipeline) | `kiteclass/kiteclass-core/.../docx/ThesisReportBuilder.java` | ~250 |
| ThesisReportBuilder JUnit (17 tests, all PASS) | `kiteclass/kiteclass-core/.../docx/ThesisReportBuilderTest.java` | ~270 |
| DocxGenerator route (`thesis-report` templateId) | `kiteclass/kiteclass-core/.../docx/DocxGenerator.java` | +11 |
| Assembly script V2 (dry-run validation) | `scripts/assemble-thesis-docx.sh` | rewrite ~90 |
| Skill docs (Thesis pipeline section) | `.claude/skills/document-generation/word/SKILL.md` | +30 |
| GAP-646 → DONE + git mv to `phase-1-beta/closed/` | — | — |

**VN academic norms applied (HUST/UIT/UET):**
- A4 portrait (11906 × 16838 twips)
- Margins 3-2-2-3 cm binding gutter (3cm left = 1701 twips, others 2cm = 1134 twips)
- Times New Roman 13pt body / 14pt heading / 18pt cover title

**CI result:** 27 SUCCESS + 1 NEUTRAL (Trivy no-op), zero FAILURE. 31/31 unit tests PASS local (17 new + 14 existing DocxGenerator).

**PR sequence Wave 100.7 entire run** (13 PRs `#1591`-`#1600`): PR-log sync + bibliography master consolidation + DOCX scoping + Phase 3 closeout + Phase 4 Foundation + Buckets A/B/C/D V1 closure + Phase 3b V2 implementation.

**Wave 100.7 final state:** 4.5/5 phases DONE (Phase 1 + 2 + 3a + 3b V2 + 4 ✅; Phase 5 defense prep DEFER Wave 110+ per GAP-653). Phase 3b V3 production `--execute` mode (Spring Boot CLI runner + MD parser + figure inject + bibliography auto-format) defer follow-up gap.

---

### 🎉 Wave 100.7 V1 SHIPPED 2026-05-19 — Thesis V1 milestone (4 of 5 phases DONE; Phase 5 defer Wave 110+)

Wave 100.7 status: `complete` flipped 2026-05-19 evening. Thesis V1 content draft milestone shipped: 4 chapters Vietnamese narrative (~2,500 lines) + 44 IEEE refs bibliography 89% inline cite utilization. Per `wave-closure-scope-completeness.md` §3 Scope-Completeness Reconciliation table trong wave plan §7.1.

| Phase | Status | Verdict |
|---|---|---|
| **1** Ch.1 Part 1 + DB arch v2 + isolation ADR + META | ✅ DONE | Wave 100 D + F + 100.5 (morning) |
| **2** Ch.1 Part 2 + Ch.2 + Ch.3 + Ch.4 narrative | ✅ DONE | Wave 100.7 Phase 2 (afternoon) — 3 parallel bg-agents |
| **3a** Bibliography master consolidation | ✅ DONE | PR #1592 (evening) — IEEE audit + cross-ref Round 1 + notes |
| **3b** DOCX pipeline scoping | 🟡 PARTIAL 20% | PR #1593 — POI Edit-Fill chosen; full Step 1-3 defer focused session ~6-8h |
| **4** Coordinator V1 ship + GAP closures | ✅ DONE | PRs #1595/1596/1597/1598 + V1 closure PR — Foundation + Buckets A/B/C/D |
| **5** Defense Q&A + slide deck | ⏳ DEFER Wave 110+ | GAP-653 — out of Phase 1 BETA scope |

**Phase 4 V1 closure deliverables this session:**
- Foundation (PR #1595, `b3a7361a`): bibliography `[40]`→`[21]` dedup + `[41-43]`→`[40-42]` renumber
- Bucket A (PR #1596, `34c863ee`): Ch.1 orphan retro-cite — 6 inline cites (`[31][32][33][34][35][40-post-renumber]`)
- Bucket B (PR #1598, `b113e646`): Ch.2 LOCAL `[1]-[8]` migrated to global (2 new refs `[43]` Pothon + `[44]` Brown C4) + Ch.2 `[5]` PDPL số fix
- Bucket C (PR #1597, `5279cbbd`): Ch.3+4 orphan retro-cite — 9 inline cites (`[18][19][26][27][29][37][38][41][42]`)
- Bucket D (this V1 closure PR): Ch.2 inline cite `[43]`+`[44]` + cross-ref-audit Round 3 final state + GAP-650 DONE + GAP-683 DONE + GAP-647 PARTIAL 80%

**Orphan reduction:** 24 → **5** (-79%, target ≤8 EXCEEDED). 5 remaining intentional defer: `[5][6]` UIT VN academic context / `[8]` Fowler Ch.2 V2 expansion / `[30]` Spring Security optional / `[36]` GPT-4 Wave 101+ if AI scope expands.

**Pending placeholders (TODO trong content, tracked):**
- Beta evidence ≥4 tenants — GAP-649 Wave 102+
- Real KPI deploy metrics — GAP-648 Wave 102+
- Defense prep (Q&A + slides) — GAP-653 Wave 110+
- AWS account restore — GAP-612 (Bucket E DEFER Wave 101+)

**Next session candidates:**
- Phase 3b DOCX implementation (~6-8h focused) — POI XWPF skill extension + `assets/thesis-template.docx` authoring + assembly script Step 1-3 per `docx-pipeline-scoping.md` roadmap
- Wave 101+ — Bucket E live verify (post AWS restore); Phase 2/2.5 K-12 expansion scope; Wave 100 Buckets A/B/C deferred work
- Wave 110+ — Phase 5 defense prep

---

### 🎉 Wave 100.7 Phase 3a + 3b PARTIAL SHIPPED 2026-05-19 — bibliography master consolidation + DOCX pipeline scoping

Phase 3 of 5-phase orchestration plan (Wave 100.7) — user split scope per `outside-in-coverage-trigger.md` §4 (Wave 100 audits cited; sub-wave exempt). Phase 3a fully shipped; Phase 3b scoping-only (Step 1-3 GAP-646 implementation defer focused session).

| # | Track | Scope | PR | Status |
|---|---|---|---|---|
| 1 | 3a Bibliography | IEEE format audit 43/43 PASS + cross-ref audit (17 cited / 24 orphan / 8 collision in Ch.2) + notes section update | #1592 | ✅ MERGED `c3fad990` |
| 2 | 3b DOCX scoping | docx-pipeline-scoping.md (Apache POI XWPF recommended over Pandoc/LibreOffice) + scaffold script placeholder + GAP-646 → PARTIAL 20% | #1593 | ✅ MERGED `01a0667b` |
| 3 | Closeout sync | GAP-683 filed (Ch.2 numbering collision + PDPL inconsistency + orphan retro-cite) + ROADMAP + wave-history sync | this PR | ⏳ in-flight |

**Phase 3a defense-risk findings (filed GAP-683 P0 follow-up):**
- Chapter 2 LOCAL `[1]-[8]` section conflicts global `[1]-[43]` bibliography
- `[40]` semantically duplicates `[21]` (PDPL 49/2023/QH15) — needs merge
- Chapter 2 `[5]` PDPL số sai (`91/2025/QH15` ≠ canonical `49/2023/QH15`)
- 24/43 orphan refs (56%) — Wave 100 + Wave 100.7 Phase 2 additions chưa retro-cite

**Phase 3b DOCX path locked:** Apache POI XWPF Edit-Fill pipeline. Rationale = zero setup (Java + Maven already on-stack) + existing skill foundation (Wave 5 teacher-contract Create pipeline + ADR-019 Facade+Strategy) + VN typography fidelity (fine-grained CTPageSz + CTPageMar + CTFonts control). Implementation roadmap ~6-8h focused session: Sub-task A template authoring 2-3h + B `ThesisReportBuilder` extends `Builder` 2h + C assembly script 1-2h + D skill docs 1h.

**Phase progression next sessions:** Phase 4 (coordinator review V1 + ship V1 PR + GAP-650 DONE — natural venue for GAP-683 chapter polish sweep) → Phase 5 defer Wave 110+ (defense Q&A + slide deck GAP-653).

---

### 🎉 Wave 100.7 Phase 2 SHIPPED 2026-05-19 — Thesis V1 content draft 4 chapters (3 parallel bg-agents)

Phase 2 of 5-phase orchestration plan (Wave 100.7) — content draft via 3 parallel worktree-isolated bg-agents. Per `outside-in-coverage-trigger.md` §4 exception (Wave 100 audits cited frontmatter — same-day audit consensus reused for sub-wave). All 4 PRs squash-merged sequential per `concurrent-production-mutation-ops.md` (shared `chapter-mapping.md`).

| # | Phase | Agent / Scope | PR | Status |
|---|---|---|---|---|
| 1 | Plan re-create | v2 cleanly từ main HEAD `69a415b7` (PR #1582 corrupted CLOSED) | #1586 | ✅ MERGED `e297853c` |
| 2 | 2-2a Ch.1 Part 2 | VN law (PDPL 2023 + Cybersecurity 2018 + Thông tư 78 + Decree 13/147) + 5 trụ cột audit-driven methodology + 5 IEEE refs | #1587 | ✅ MERGED `37994ffe` (~10 trang, ~25k chars) |
| 3 | 2-2b Ch.2 | System architecture narrative ~720 lines, 5 sub-sections (Functional/NFR/Architecture C4 L1+L2/SaaS/B-learning) + 5 Mermaid diagrams | #1588 | ✅ MERGED `ff8497f2` |
| 4 | 2-2c Ch.3 + Ch.4 | Ch.3 472 lines (5 code snippets: JWT filter / Tenant RLS / Outbox dispatcher / Beta controller / Next.js page) + Ch.4 644 lines (AWS Free Tier + User onboarding + KPI + Beta scope với TODO placeholders) | #1589 | ✅ MERGED `b2e4d0ef` |

**Total Phase 2 content:** ~2,500 lines Vietnamese narrative + 5 new IEEE refs (bibliography 38 → 43). Bibliography expansion: refs [39] Decree 147/2024 + [40] PDPL 2023 + [41] N.T. Phuong+L.H. Anh SaaS compliance VN + [42] Forsgren Accelerate + [43] Sato et al. Continuous Delivery ICSE 2020.

**Incident logged:** Agent worktree path pollution recurrence (class GAP-624) — Agent 2a + 2c initially wrote files to MAIN repo paths instead of their own worktree directories despite `isolation: worktree` flag. Both agents self-recovered (git branch -f + git reset, cherry-pick to fresh branch). Coordinator rebased 2b + 2c onto updated main with `chapter-mapping.md` 3-way conflict resolution. Follow-up META gap deferred next session — investigate harness behavior + harden agent prompts + meta-rule strengthening.

**Phase progression next sessions:** Phase 3 (bibliography master consolidation 15+ refs + DOCX pipeline GAP-646 pandoc reference.docx) → Phase 4 (coordinator review V1 + ship V1 PR + GAP-650 DONE) → Phase 5 defer Wave 110+ (defense Q&A + slide deck GAP-653).

### 🎉 Wave 100 PARTIAL ship 2026-05-19 — Bucket D + F + META cross-cut (3/6 buckets shipped Phase 1)

Wave 100 thesis push 3-audit consensus baked into scope (persona+benchmark+failure-mode). Phase 1 docs-heavy parallel ship:

| # | Bucket | Gap | PR | Status |
|---|---|---|---|---|
| 1 | D thesis Ch.1 Part 1 (competitor + AI techniques) | GAP-650 Part 1 | #1584 | ✅ DONE Part 1 (Part 2 defer Wave 101) |
| 2 | F database-architecture-map v2 rewrite | GAP-681 | #1583 | ✅ DONE 100% (16 sections, 11 Mermaid, Vietnamese narrative) |
| 3 | META VN-localization audit checklist | GAP-680 | #1584 (paired) | ✅ DONE v1.0.0 (4 sections cross-bucket) |
| 4 | C email-only signup phased | GAP-286 | TBD | ⏳ DEFER next session (code-heavy multi-PR) |
| 5 | A batch invoice UX | GAP-297 | TBD | ⏳ DEFER (x-large, sequential before B) |
| 6 | B income dashboard | GAP-293 | TBD | ⏳ DEFER (sequential sau A) |
| E (extra) | Bucket E close-out GAP-518/538 | — | TBD | ⏳ DEFER Wave 101+ (AWS GAP-612 blocked) |

**Sync PR shipped same session:** session-end-context-check.md v1.0.1 → v1.1.0 (new §4.5 docs-sync 5-target check at session-end decision moment) + GAP-680 CSV+file DONE sync + ROADMAP + wave-history backfill + session-handoff note.

### 🎉 Wave 100.5 SHIPPED 2026-05-19 — Multi-tenant Isolation Patterns ADR (PR #1581)

Single-bucket standalone ADR-style report `documents/02-architecture/multi-tenant-isolation-patterns.md` v1.0.0 (~15-20 pages, 12 sections, 6 patterns P1-P6 evaluated × 6 axes, 4 Mermaid flowchart, 7 IEEE references) — thesis Chapter 2 source material per user direction "tạo documents thôi chứ, còn trong thesis thì phải trình bày hợp lý với số trang 60". GAP-682 DONE 100%. Section 7 trong `multi-tenant-architecture.md` cross-link footer pointer added.

---

## 🎯 Previous Status Snapshot (2026-05-19 — Wave 99B 7/7 SHIPPED + closure: arch docs sweep complete + GAP-661 audit suite + Wave 99C META synthesis + output-review-mandate v1.12.3 streamline)

### 🎉 Wave 99C META synthesis SHIPPED 2026-05-19 — 2 detectors + 2 META-META gaps (PR #1566)

3-agent outside-in META audit (Persona + External Benchmark + Failure-Mode Matrix) consensus implementation:

| Action | Outcome |
|---|---|
| **Action 1** — `check-3-layer-completeness.sh` CI detector | ✅ shipped (real-repo 4 violations surface WARN: `kitehub/email`+`marketing`+`preferences`+`kiteclass/multi-tenancy`); closes Wave 92 GAP-640 + Wave 98 GAP-664 recurrence #2 |
| **Action 2** — `check-cross-layer-contract-drift.sh` CI detector | ✅ shipped (heuristic v1 WARN); closes Wave 98 GAP-662 incident + `contract-first-for-cross-layer.md` §6.2 deferred-detector debt (12 ngày) |
| **Action 3** — GAP-675 META-META filed (P1) | ✅ filed — audit `incident-to-rule-pipeline.md` §3 premature-rule guard usage; 6 recent rules deferred detectors never landed |
| **Action 4** — GAP-676 META filed (P2) | ✅ filed — Quality Gate vs Issue Tracking classification per SonarQube pattern (defer ship per Benchmark consensus avoid scope creep) |

Rule edits: `audit-to-gap-pipeline.md` v1.4.3 + `contract-first-for-cross-layer.md` v1.0.2 PATCH (close deferred-detector debt).

### 🎉 Wave 99B B6 SHIPPED 2026-05-19 — Architecture archive sweep (PR #1565)

Foundation bucket of Wave 99B — archive 6 stale/superseded files (`living-docs-audit-2026-04` + `ai-branding-v2-redesign` + `ai-branding-design-patterns` + `backup-strategy` + `docker-platform-architecture` + `email-lifecycle`) → `documents/07-archived/architecture-2026-Q2/`. Root-level count **16 → 10** (volume cap 50 compliant). README index synced (Wave 96 PR2 canonical pivot). **GAP-668 DONE 100%.** Unblocks B0-B5 buckets per Wave 99B plan §6.

### 🎉 GAP-661 SHIPPED 2026-05-19 — Wave 98 post-closure audit suite (4 audits + 6 follow-up gaps)

| Audit | Score | Δ vs baseline | Phase 1 BETA gate | New gaps |
|---|---|---|:---:|---|
| **UI /128** | 110.6 A | -1.4 vs Wave 83 baseline 112.0 A+ (within sample noise) | ✅ PASS | GAP-665 (P1 /legal/terms restructure) + GAP-667 (P2 UI hygiene) |
| **Quality /100** | 90/110 (83 tech) B+ | +5 raw vs Wave 53 baseline 85/110 | ✅ PASS +10 buffer | 0 new |
| **API Contract /100** | 76 C 🔴 FAIL | -3 vs Wave 92 baseline 79 C+ | 🔴 FAIL -4 | GAP-662 (P0 EmailController URL drift) + GAP-663 (P0 PreferencesController zero IT) |
| **Business Logic /100** | 73 C+ PARTIAL FAIL | +3 vs Wave 92 baseline 70 C | 🔴 FAIL -7 | GAP-664 (P1 3-layer doc — shared BL+API) + GAP-666 (P2 BR-ID javadoc + README index) |

**Status:** 🟢 SHIPPED T-2 from 2026-05-21 deadline. 4 parallel agents converged in ~6 min wall-clock. `audits-index.csv` +4 rows. `output-review-mandate.md` §3 matrix 4 REFRESHED markers + body streamline round 2 (v1.12.1→v1.12.2 PATCH, size 40.5k→38.6k under threshold). `gap-status.csv` +6 new rows + GAP-661 DONE flip + file moved `phase-1-beta/closed/`.

**Path to Phase 1 BETA gate ≥80:** API +4 via GAP-662+663+664 cluster (~3.25h) → 80 PASS; Business +7 via GAP-664+666 (shared GAP-664) → 80 PASS. Both unblock together — 1 wave can clear both gates.

### 🎉 Wave 98 SHIPPED 2026-05-18 — Cluster B beta-cohort polish (8/8 buckets)

| Bucket | Gap(s) | Outcome | PR |
|---|---|---|---|
| B0 PREREQ | GAP-656 | UI Coordinator (`useOnboardingPhase` + `SupportMenu` + `OnboardingCoordinator` + `PreferencesController` + mobile spec) — PARTIAL 80% (live verify gated GAP-612 AWS) | #1548 |
| B1 | GAP-657 + GAP-659 | Email layer hardening + Tone enum + 5 plain-text siblings + staff-invite template — PARTIAL 80% (live render verify gated SES prod approval) | #1553 |
| B2 | GAP-658 + GAP-538 | VN sample seed worker (6 VN data CSVs + `VietnamSampleDataGenerator` + 3-layer business doc) — PARTIAL 80% / GAP-538 PARTIAL 90% (consumer wiring gated service materializing) | #1550 |
| B3 | GAP-539 | Banner close — dashboard mount + version chip + PDPL consent + /beta-status freshness — **DONE 100%** | #1551 |
| B4 | GAP-541 | Vietnamese i18n close — 4 canonical VI catalogs + TOS + Privacy v1 disclaimer — **DONE 100%** | #1549 |
| B5 | GAP-540 + GAP-542 | SupportMenu wiring — FeedbackForm modal + legacy widget cleanup — **DONE 100% both** | #1555 |
| B6 | GAP-660 | Zalo OA fast-path — SupportMenu Zalo item + footer + email CTA + remove "coming Phase 1.5" copy — **DONE 100%** | #1557 |
| B7 | GAP-518 | P3 role-guard verify — BE role enum + RoleGuardMatrixIT + Playwright spec — PARTIAL 95% (live browser verify gated GAP-612 AWS) | #1556 |

**Wave 98 status:** 🟢 SHIPPED 8/8 buckets per plan §3 Scope. **5 gaps DONE 100% + 5 gaps PARTIAL ≥80%** (3 gated by GAP-612 AWS suspension live-verify portion per `pre-handoff-self-test-completeness.md` §5.4 PARTIAL exit ramp). Post-merge sync rounds 1+2 (PR #1552 + #1554) covered Wave 1 gap drift; this closure PR covers Wave 2 (B5/B6/B7) gap drift + closure reconciliation.

**Post-Wave-98 audit suite cadence per `post-wave-audit-mandate.md` §2.2:** UI /128 sample + Quality /100 refresh due within 3 days (≤2026-05-21). Filed **GAP-661** to schedule.

### 🎉 Wave 99B SHIPPED 2026-05-19 — Architecture docs sweep + expansion (7/7 buckets DONE)

Foundation + expansion sweep of `documents/02-architecture/` — closure PR ships all 7 buckets DONE:

| # | Bucket | Gap | PR | Outcome |
|---|---|---|---|---|
| 1 | B6 Archive sweep (FOUNDATION) | GAP-668 | #1565 | ✅ 6 stale files → `07-archived/architecture-2026-Q2/`; root 16 → 10 (volume cap 50 compliant) |
| 2 | B0 Last-Reviewed backfill + Mermaid migration | GAP-669 | #1568 | ✅ 24 non-ADR arch files frontmatter synced |
| 3 | B1 Service Catalog + Dependency Graph + Auth Flow | GAP-670 | #1569 | ✅ 18-service catalog + Mermaid flowchart + sequenceDiagram + 10-controller role-guard matrix |
| 4 | B2 Compliance Map + SLO Registry + NFR + Risk Register | GAP-671 | #1574 | ✅ 19 compliance rows + 11-service SLO + 35+ NFR + 5-row Risk per arc42 §10 |
| 5 | B3 Database Architecture Map (consolidated) | GAP-672 | #1573 | ✅ 91 entities + RLS 51/91 + FK erDiagram + Flyway 114 V-files; GAP-677 auto-gen follow-up Wave 100+ |
| 6 | B4 C4 Context + Container (L1+L2) | GAP-673 | #1571 | ✅ Mermaid L1 8 actors + 6 external + L2 2 FE + 1 gateway + 7 services + 4 infra subgraph |
| 7 | B5 Golden-path Onboarding Tour README orchestrator | GAP-674 | #1577 | ✅ REWRITE 103 → 204 lines + 7-step reading order tour + Trace-one-request 8-layer tutorial + per-persona index P1/P2/P3/P4 |

**Wave 99B closure status:** 🟢 SHIPPED 7/7 buckets DONE 100%. Cleanup: 4 merged worktree-agent branches deleted, 0 husks per `post-wave-cleanup.md` §2. Scope-Completeness Reconciliation per `wave-closure-scope-completeness.md` §3 in wave plan §7.1.

**Post-wave audit suite cadence (≤2026-05-22):** filed **GAP-678** P1 — Quality /110 + Business Logic /100 refresh; 2 bg-agents parallel; expected positive deltas Cat 1 Rule Coverage (Wave 99C detectors closing recurrence #2) + Cat 9 Architecture (4 NEW canonical artifacts) + Business Logic Cat 1 (B2 compliance row verdict).

**Out-of-wave shipped same session:**
- ✅ PR #1572 — Mermaid `<br/>` recurrence #6+#7 fix (sequence/state diagrams) + new detector `check-mermaid-sequence-state-br.sh`
- ✅ Wave 99C META synthesis PR #1566 — 2 detectors (3-layer + cross-layer drift) + 2 META-META gaps GAP-675/676
- ✅ PR #1576 — `output-review-mandate.md` v1.12.3 streamline round 3 (53k → 33k char Claude-metric, well under 40k threshold)

> **📍 Next session ĐỌC TRƯỚC (Wave 99B SHIPPED; remaining pending → thesis pivot):**
>
> **Pending work carry-forward to next session:**
> 1. **GAP-678 — Wave 99B post-wave audit suite** (cadence ≤2026-05-22) — Quality /110 + Business Logic /100 refresh; spawn 2 bg-agents parallel
> 2. **PR #1570 rebase** — GAP-675 META-META, conflicts với Wave 99B merges (script-quality.yml + rules-index.csv). Complex rebase ~30 min careful work
> 3. **4 real-repo 3-layer violations** (Wave 99C detector WARN mode, HARD STOP 2026-06-19):
>    - `kitehub/email` missing `use-cases.md` (GAP-664 partial covers)
>    - `kitehub/marketing` missing `use-cases.md` (file follow-up gap)
>    - `kitehub/preferences` missing `rules.md` + `use-cases.md` (GAP-664 covers)
>    - `kiteclass/multi-tenancy` missing `use-cases.md` + `api-contract.md` (file follow-up gap)
> 4. **GAP-662..667 cluster (Phase 1 BETA gate fixes)** — DEFER Wave 100+ per user pivot priority
> 5. **Wave 99 thesis META plan PR #1559** — pivot to thesis after audit suite + Phase 1 BETA gate cluster
>
> **After GAP-678 audit + PR #1570 rebase + 4 violations → /clear + thesis pivot per user 2026-05-19 plan.**
> 3. **5 PARTIAL Wave 98 gaps gated GAP-612 AWS** (656/657/658/518 live verify + 543 manual render verify) — close-out post-restore: GAP-518 95→100%, GAP-656 80→95%, GAP-657 80→95% (manual 2-client render), GAP-543 80→100%, GAP-658 80→95% (consumer wiring trigger).
> 4. **Post-wave-98 audit suite verdicts** — UI ✅ 110.6 A + Quality ✅ 90/110 B+ PASS (no follow-up); API 🔴 76 C FAIL + Business 🔴 73 C+ FAIL (item 1 cluster fixes both).
> 4. **bg-agent lesson learned** (carry-forward Wave 97) — context-thrashing rate-limited 4 agents this wave (B1/B3/B6/B7 first attempts). 2-concurrent retry pattern PROVEN safe. 3-concurrent threshold hit Anthropic throttle.
> 5. **Wave 96 sweep batch B-F follow-ups** (carry-forward unchanged):
>    - Action B: markdown frontmatter strip 34 gaps (defer Phase 3)
>    - Action C: UPDATE_SCOPE 4 gaps (GAP-203/220/052/155/438)
>    - Action F: human triage 12 UNCLEAR (incl GAP-040 anomaly)
> 6. **GAP-612 AWS suspension** Day 4 96h+ no response 2026-05-21 — escalation executed: alt contacts set (Billing/Operations/Security) + case 177903869600100 replied 9-bullet evidence ($0 balance + 0 notification + identity-verify willingness); 6-agent outside-in synthesis spawned filed **3 follow-up gaps**: GAP-694 P0 local self-test investigation (Phase 0 blocks Item 4 + rebuild Gates 2/3; renumbered from 691 due collision với Wave 102.7.3 audit), GAP-692 P1 META env-reference.yaml multi-env refactor (eliminates Class 4 config-drift permanently), GAP-693 P0 META AWS rebuild SOP playbook 13 steps + 5 gates + 8 failure-mode prevention (BLOCKED on 612+692+694). **Phase 0A investigation SHIPPED 2026-05-21:** Docker Desktop process not running on Windows host (P0 root cause, 5min fix); .env 9 missing keys (P1); preflight check-docker.sh (P2 META). Status flipped OPEN → PARTIAL 5%. Decision: Item 1 LEGIT chính danh account #2 sau khi #1 resolved (no duplicate detect + ToS-compliant); sequencing Phase 0 → Item 2 → rebuild.
> 7. **Wave 97 PARTIAL close-outs** (carry): GAP-637/638/647 await GAP-654/655 execution

### 🟡 Wave 97 PARTIAL ship 2026-05-18 — audit P0+P1 gate-closing (3.5/4 buckets delivered)

| Bucket | Gap(s) | Wave-plan delivery | Gap status | PR |
|---|---|---|---|---|
| A | GAP-637 | ✅ DONE (3 controllers + 6 tests + SecurityConfig per plan §3) | PARTIAL 60% (gap AC > wave scope; defers GAP-638+GAP-612) | #1540 |
| B | GAP-638 | 🟡 PARTIAL (B1 docs only; B2 DTOs + B3 deprecation DEFER) | PARTIAL 30% (defers **GAP-654** + GAP-612) | #1543 |
| C | GAP-639+640 | ✅ DONE | DONE both | #1542 |
| D | GAP-642+644 | ✅ DONE | DONE both | #1542 |

**Wave 97 status:** 🟡 PARTIAL ship (NOT status: complete) — Bucket B incomplete vs plan §3. Closure PR cần Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3.

**2 orphan-cleanup gaps filed 2026-05-18 (compliance with `gap-done-discipline.md` §3):**
- **GAP-654** Admin v1 typed DTOs + controller refactor + legacy @Deprecated (Wave 98 candidate — completes Bucket B2/B3 scope)
- **GAP-655** Thesis citation-extract skill (Wave 98+ thesis tooling — completes GAP-647 Step 3)

> **📍 Next session ĐỌC TRƯỚC (Wave 97 PARTIAL closed):**
> 1. **Wave 98 plan draft** — thesis infrastructure foundation per `release-1.5-thesis-scope.md` §3 Wave 98 row; 4 buckets candidates: GAP-646 thesis-docx pipeline / GAP-647 bibliography Phase 2 (via GAP-655 citation-extract skill) / GAP-650 chapter 1 literature Part 1 / GAP-648 NFR k6 baseline
> 2. **GAP-654 Wave 98 candidate** — Admin v1 typed DTOs + controller refactor + legacy @Deprecated (~1-1.5 bucket scope; completes Wave 97 Bucket B2+B3 deferred portion)
> 3. **GAP-655 Wave 98+ candidate** — Thesis citation-extract skill (~0.5-1 bucket; completes GAP-647 Step 3 deferred portion)
> 4. **bg-agent lesson learned** — context-thrashing 2x failure cho Java code scope > 6 file ops. Wave 98 spawn pattern: docs-heavy buckets parallel bg-agent OK; code-heavy buckets prefer foreground OR mini-agent surgical split (B1 + B2 pattern per Wave 97 Bucket B retry)
> 5. **Release 1.5 thesis-scope LOCKED 2026-05-18** — 8 META gaps GAP-646..653 + 7 product demo-blocker re-prioritized (GAP-287/297/293/562/518/286/538). Decision-point cuối Phase 1 ~2026-07-15 cho upgrade Release 2 option
> 6. **GAP-612 AWS suspension** restoration pending D+4 = 2026-05-21 trigger escalate (unchanged from Wave 96)
> 7. **Wave 97 PARTIAL gap close-out** post-AWS-restore: GAP-637 60% → 100% + GAP-638 30% → 90% (via GAP-654) + GAP-647 50% → 100% (via GAP-655). All 3 PARTIAL blocked by AWS live verify portion OR follow-up scope.
> 8. **Wave 96 sweep batch B-F follow-ups** (carry-forward unchanged):
>    - Action B: markdown frontmatter strip 34 gaps (defer Phase 3 per gap-architecture-v2)
>    - Action C: UPDATE_SCOPE 4 gaps (GAP-203/220/052/155/438)
>    - Action F: human triage 12 UNCLEAR + AMBIGUOUS gaps (incl GAP-040 anomaly)

### 🎉 Wave 97 Bucket A SHIPPED 2026-05-18 — GAP-637 admin @PreAuthorize + SecurityConfig + 6 security tests

**PR #1540 merged** (10 files: 3 controllers + SecurityConfig 114L + 3 SecurityTest classes + pom.xml + GAP file + CSV). Local `mvn verify -P strict-warnings` PASS 50 tests / 0 failures. CI 25 checks PASS. Status PARTIAL 60% — wave plan §3 Bucket A scope delivered fully; AC ≥18 endpoint tests + api-contract docs + AWS live verify defer companion gaps. NOTE: bg-agent first run failed context-thrashing 21min; salvaged working tree quality-verified.

### 🎉 PR #1539 MERGED 2026-05-18 — 8 thesis META gaps + Plan 1.5 thesis-scope + 3 outside-in audits

**Highlights:**
- Release 1.5 thesis scope LOCKED (Phase 1 BETA + Phase 1.5 paid features); defense window 2026-08-15 → 2026-10-15
- 3 outside-in audit agents (persona / VN benchmark / failure-mode) consolidated findings
- VN benchmark verdict: scope AMBITIOUS top 5-10% VN CS thesis 2026
- 8 META gaps filed: GAP-646 thesis-docx-pipeline (P0) / GAP-647 thesis-bibliography-ieee (P0) / GAP-648 thesis-nfr-data-capture (P0) / GAP-649 thesis-beta-cohort-execution (P0) / GAP-650 thesis-chapter-1-literature (P0) / GAP-651 thesis-image-curation (P1) / GAP-652 multi-tenant-isolation-demo (P1) / GAP-653 thesis-defense-prep-deck (P1)
- Plan 1.5 4 work tracks: META infra / NFR data / beta cohort / 7 product demo-blockers
- Decision-point cuối Phase 1 ~2026-07-15 cho upgrade Release 2 option

### 🎉 Wave 96 SHIPPED 2026-05-18 — gap re-triage batch A+D + kiteclass-gateway removal + diagram rule + 3 architecture reports

**PRs:** #1535 (Wave 96 PR1 — re-phase 9 gaps + GAP-145 WONTFIX + 3 audit reports), #1536 (Wave 96 PR2 — kiteclass-gateway removal + ADR-032 + diagram-format-selection rule v1.0.0 + kitehub/kiteclass/multi-tenant arch reports).

**Highlights:**
- 5-agent parallel re-triage 661 gaps (~30 min vs ~3-5h serial) — `documents/04-quality/audits/meta/2026-05-18-wave-96-gap-retriage-full-sweep.md`
- Batch A: 9 gaps re-phased (7 phase-2→phase-1-beta + 2 phase-2→phase-1.5-paid)
- Batch D: GAP-145 WONTFIX (consolidated by GAP-434+111+112)
- kiteclass-gateway REMOVED per ADR-032 / GAP-001 Option A (154 files + 9 ref sweeps + backup branch `archive/kiteclass-gateway-pre-removal-2026-05-18`)
- NEW rule `diagram-format-selection.md` v1.0.0 + self-test (`email-architecture.md` ASCII → Mermaid)
- 3 NEW arch reports (Mermaid): `kitehub-architecture.md` (517L/5 diagrams) + `kiteclass-architecture.md` UPDATE (451L/5) + `multi-tenant-architecture.md` (529L/5 dev runbook)
- `output-review-mandate.md` v1.10.0 → v1.12.0 (+2 matrix rows)

### 🎉 Wave 95 SHIPPED 2026-05-18 — gap folder organization (v1.0.0 → v2.0.0 + mass migration 466 files)

**PRs:** #1532 (v1.0.0 status-driven — superseded same session), #1533 (PR1.5 v2.0.0 forward-fix phase-only), #1534 (mass migration 466 files + CI strict).

**Highlights:**
- NEW rule `gap-folder-organization.md` v2.0.0 (phase-only design + per-phase `closed/` archive)
- 466 files migrated phase-X/[closed/] layout
- 3-agent outside-in audit caught v1.0.0 status-driven design wrong → v2.0.0 forward-fix
- Cost reduction: ~1,200 git mv over 6 months → ~120-150 (24x)
- Sister-tool fix `check-gap-status-csv.sh` recursive walk (Agent 3 Class 4 prediction validated)
- CI `gap-folder-location.sh` strict mode active

### 🎉 Wave 94c SHIPPED 2026-05-18 — GAP-619 Wave 92 post-wave audit suite (5 categories)

### 🎉 Wave 94c SHIPPED 2026-05-18 — GAP-619 Wave 92 post-wave audit suite (5 categories)

**PR #1531 merged** (21 files / 2984 insertions). 5 audit agents parallel + 1 gap-drafting agent. 3-day deadline 2026-05-21 met 3 ngày trước. Audit scores + 8 new gaps + 1 Wave 96 stub + GAP-619 DONE flip. Path Phase 1 BETA gate 80 via GAP-637 fix + GAP-612 AWS restore.

### 🎉 Wave 94b SHIPPED 2026-05-18 — waves/ subdir split per Rule 3 volume budget

**PR #1530 merged** (85 files: 81 git mv + 3 READMEs + 1 plan). Split `documents/03-planning/waves/` 108 files → 3 wave-range subdirs (wave-01-30 / wave-31-60 / wave-61-90) + 27 active at root. All subdirs under 50-file Rule 3 cap.

### 🎉 Wave 93 SHIPPED 2026-05-18 — Phase 1.5 PAID payment outside-in audit + 12 gaps + 4 re-scope + §7 follow-up

**PR #1528 merged** (~37 files atomic). 3-agent QR base audit + 3-agent OCR audit (Casso/SePay webhook pivot) + 26-gap re-triage. 11 new gaps GAP-625..636 + GAP-636 webhook + 4 re-scope GAP-108/183/185/594 + 5 cross-ref + Wave 93 §7 follow-up DONE (close GAP-581 DUPLICATE + phase moves GAP-123/124/415 → phase-2 + pair GAP-625↔GAP-578 Phase 1.5a sequential).

### Session 2026-05-18 totals

| Metric | Count |
|---|---|
| PRs shipped | 4 (#1528 + #1529 rule v1.1.0 + #1530 + #1531) |
| New gaps | 21 (GAP-625..645) |
| Re-scope existing | 4 (GAP-108/183/185/594) |
| Audit reports | 8 (3 Wave 93 + 5 Wave 94c) |
| Rules version bumps | 2 (outside-in v1.0.0→v1.1.0 / output-review-mandate v1.9.0→v1.9.1) |
| Phase 1.5 active gap count | 30 → 36 (+10 new -1 DUPLICATE -3 phase moved) |
| waves/ folder utilization | 216% → 54% (Rule 3 cap restored) |

### 🎉 Wave 92 SHIPPED 2026-05-18 — Pre-Tenant Cluster + Audit Follow-ups (preserved for historical context below)

### 🎉 Wave 92 SHIPPED 2026-05-18 — Pre-Tenant Cluster + Audit Follow-ups (5/5 buckets parallel offline-safe)

**7 PRs merged:** #1510 (collision-fix gap ID renumber 614/615/616 → 616/617/618) + #1511 (E meta backlog 3 new gaps) + #1512 (C beta_request abort cleanup) + #1513 (A admin audit enrichment Phase 2 V54) + #1514 (D professional-manual rule + 3 admin v1 controllers) + #1515 (B BE findAll bounded + FE JWT tab-scoped storage) + #1516 (thesis sync: action-2 + khung báo cáo move 07-archived → 08-thesis + PR-logs).

**Bucket outcomes:**
- ✅ A — GAP-521 PARTIAL 70→85% Phase 2 enrichment (V54 + 5 columns + AdminAuditAspect + 3 IT). Remaining: other admin controllers + FE admin UI defer Wave 93+
- ✅ B — GAP-432 DONE (symptom self-corrected Wave 41 PR #1000; Bucket B added 5 boundary tests hardening per audit-to-gap-pipeline §2.8 fix-time state-check) + GAP-599 PARTIAL 0→85% (sessionStorage facade `jwt-storage.ts` + 7 production sites migrated + 17 unit tests + 3 two-tab simulation PASS). Live multi-tab UX verify pending AWS restore GAP-612
- ✅ C — GAP-600 DONE (BetaRequestAbortCleanupScheduler + V53 composite index + ABORTED terminal status + 11 tests)
- ✅ D — NEW rule `professional-manual-content-standard.md` v1.0.0 sister cho user-manual-content-standard + 3 admin v1 controllers (Instances/Payments/Revenue fix Wave 90 walkthrough 404) + 14 unit tests + Manual split queue item consumed
- ✅ E — 3 NEW gaps filed: GAP-616 P2 uptime monitoring + GAP-617 P3 disaster recovery + GAP-618 P2 AWS health dashboard (all phase-1.5-paid backlog)

**Side artifacts:**
- 1 new rule `professional-manual-content-standard.md` v1.0.0 + paired same-PR enforcement
- 1 investigation audit `2026-05-18-fe-runtime-state-and-cve-gate-investigation.md` (6 CodeQL HIGH paper-tiger gate analysis — Vercel runtime serving, container chưa serve, gate Wave 88 cutover post-AWS-restore)
- 3 thesis outside-in audits (T1 persona / T2 benchmark / T3 failure-mode) in-flight cho Wave 93+ thesis plan
- 5 worktree husks pruned + 5 Wave 92 bucket branches deleted

**Hotfix landed mid-flight:**
- V53 collision Bucket A vs C — rebased Bucket A + renamed V53 → V54 (force-push 1dd67f50 → 5fc24a88; CI re-ran clean trên rebased HEAD per admin-merge-discipline §3)
- Wave 92 plan §3 Bucket E gap ID collision 614/615/616 → 616/617/618 (PR #1510 — pre-spawn fix; recurrence #6 of audit-to-gap-pipeline §2.6 symbol verify miss class)

**Pending:**
- GAP-612 AWS suspension restoration (D+4 = 2026-05-21 trigger escalate)
- GAP-521 + GAP-599 PARTIAL → Wave 93 candidates
- **GAP-619** Wave 92 post-wave audit suite ≤3 days deadline 2026-05-21 per `post-wave-audit-mandate.md` §2.2 + new rule `wave-closure-scope-completeness.md` §3
- **GAP-620** Wave 92 Bucket D live verify 3 admin v1 controllers post-AWS-restore (gated GAP-612)
- **GAP-621** Wave 92 Bucket B+C live verify prod-equivalent env post-AWS-restore (gated GAP-612)
- 6 CodeQL HIGH FE container CVEs — gate Wave 88 cutover (post AWS restore + base image refresh node:22-trixie-slim → bookworm-slim option)
- Thesis plan task — outside-in audits done; merge findings + scope lock + Wave 93 thesis-1 spawn (blocked-on release 2 plan lock per user 2026-05-18 decision)

### 🎉 Wave 84 SHIPPED 2026-05-15 — Ops Observability + Secrets Rotation + Account-Prep + Terraform Apply LIVE (7/7 buckets executed)

**7 PRs merged:** #1417 (F GAP-431 helm startupProbe verified) + #1418 (G GAP-414 EC2 right-sizing) + #1419 (DE GAP-423/424 VN overlays) + #1420 (A GAP-437 CloudTrail observability) + #1421 (B GAP-379 secrets rotation) + #1422 (C GAP-394 account-prep runbooks) + closure PR (Bucket H Ops Readiness audit).

**Live-applied infrastructure** (workflow run 25929212198, 2026-05-15 16:33 UTC, ~1m30s):
- Plan: 35 added / 2 changed / 0 destroyed → Apply complete clean
- ✅ CloudWatch dashboard `kitehub-phase-1-overview` extended với 4 security widgets + ALB/RDS rows
- ✅ 4 security alarms (failed_iam_auth OK / root_account_use INSUFFICIENT_DATA / sg_changes_burst INSUFFICIENT_DATA / secrets_access_burst OK)
- ✅ 3 EC2 low-CPU alarms (INSUFFICIENT_DATA — 7d baseline pending)
- ✅ Lambda `kitehub-production-rotate-secret-handler` Active, 3 secrets RotationEnabled (next 2026-08-13)
- ✅ Lambda `kitehub-ec2-cost-report` Active, monthly cron 1st-of-month 08:00 UTC
- ✅ 2 SNS topics: `kitehub-security-alerts` + `kitehub-cost-alerts`

**Governance side-effects:**
- New rule `.claude/rules/dev-authorized-terraform-trigger.md` v1.0.0 — codifies dev-authorized override of `release-deploy-standard.md` §9 BAN
- New rule `.claude/rules/claude-md-content-discipline.md` v1.0.0 — CLAUDE.md content ceiling + banned-content list
- CLAUDE.md trimmed (30-line override section → 3-line pointer)

**Pending user-action:**
- RDS db-password rotation bootstrap qua AWS console (Serverless Application Repository, ~5-10 min, per `secrets-rotation-runbook.md` §5.2.1)
- Pre-existing `kitehub-kc-app-fe-cert-expiry` ALARM investigation (unrelated to Wave 84)
- Follow-up gap: GitHub Actions sync-event delivery on force-push (per Bucket B saga ADMIN_MERGE_FOLLOWUP)

**Audit artifacts:**
- 3 pre-apply: `documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-{a,b,g}-*-plan.md`
- 1 post-apply: `2026-05-15-wave-84-buckets-abg-post-apply.md`
- 1 Ops Readiness refresh: `documents/04-quality/audits/ops-readiness/2026-05-15-wave-84-post-apply.md` (Bucket H)

### 🎉 Wave 83 SHIPPED 2026-05-15 — Hot-fix + PDPL Cookie Consent + Email schema fix (7/7 executed)

### 🎉 Wave 83 SHIPPED 2026-05-15 — Hot-fix + PDPL Cookie Consent + Email schema fix (7/7 executed)

**7 PRs merged:** #1407 (Bucket A GAP-571 — 6 Spring exception handlers RFC 7807) + #1408 (Bucket E GAP-558 cookie consent + Footer link) + #1409 (wave plan §5-8 sections fix) + #1410 (Bucket B GAP-570 NoHandlerFoundException → 404) + #1412 (Bucket G closure protocol) + #1413 (audit suite 4 reports: API 82 / BL 71 / Sec v2 90 PASS / UI 112/128) + #1414 (GAP-572 Phase 4 dual-schema fetch-secrets.sh). Tags: staging.17 + staging.18 deployed AWS.

**Live-verified post-deploy:**
- POST `/api/v1/auth/nonexistent` → **404** ✅ (NoHandlerFoundException + NoResourceFoundException both mapped)
- POST `/api/auth/verify-email` empty → **400** ✅ (MissingServletRequestParameterException RFC 7807)
- POST `/api/v1/auth/beta-signup/validate` wrong method → **405** ✅ (HttpRequestMethodNotSupportedException + Allow header)
- POST request-beta-access correct DTO → **201** ✅

**Bucket outcomes (5 DONE + 1 deferred + 1 in-flight):**
- ✅ A — GAP-571 validation 500 → 400/401/405 (PR #1407, live-verified)
- ✅ B — GAP-570 NoHandlerFoundException 404 (PR #1410 follow-up, live-verified)
- ✅ C — beta-status (self-healed 200, no fix needed)
- ✅ D — gateway `/kitehub-subscription/*` (false-positive finding, expected 404)
- ✅ E — GAP-558 cookie consent (PR #1408 ConsentGatedAnalytics + Footer; 80% scope subsumed Wave 23 GAP-353/368)
- 🚨 **F — GAP-370 email production E2E BLOCKED** → GAP-572 deferred (Resend schema JSON vs plain-string mismatch + agent-aws-access §2.2 violation Task #73 — user rotate pending)
- ⏳ G — Closure protocol + audit suite in-flight (background agent 4 reports — API Contract + Business Logic + Security v2 + UI /128)

**Wave 84 queued:** ops observability + runbooks (~10-14h) — CloudTrail GAP-437, secrets rotation GAP-379, 4 account-prep runbooks GAP-394, VN overlays GAP-423/424, startupProbe GAP-431, EC2 right-sizing GAP-414. Target: Ops Readiness /100 ≥80 (vs 60 baseline).



### 🎉 Wave 82 SHIPPED 2026-05-15 — FE Self-Host AWS EC2 + Wave 81 follow-ups (8 buckets + 10 PRs + 4 follow-up gaps)

**10 PRs merged:** #1396 (Bucket F+A — 8 gateway routing fixes + Spring config + script rename + ADR-031 + GAP-565..568) + #1397 (OTel CVE-2026-45292 BOM 1.49→1.62) + #1398 (Bucket B drafts — terraform + nginx + PM2 + certbot + runbook + CORS audit) + #1399 (GAP-570/571 + runbook SSM/Secrets Manager fix) + #1400 (AMI pin prevent surprise EC2 replacement) + #1401 (IAM TagInstanceProfile fix) + #1402 (post-apply audit) + #1403 (CF token Secrets Manager align) + #1404 (4 follow-up gaps) + closure PR.

**Production state:**
- ✅ BE kh-backend v0.9.0-beta-staging.16 (api.kitehub.me) — F4 gateway routing fix LIVE (beta-status 200 was 400) + OTel CVE patched
- ✅ FE kc-app-fe NEW EC2 `i-05cfda7c6c60b683f` t3.small ap-southeast-1 @ public IP `54.179.70.37`
- ✅ DNS cutover via Cloudflare API: `kitehub.me` CNAME Vercel → A record EC2 (proxied=false, ttl=300)
- ✅ Cert wildcard `*.kitehub.me` Let's Encrypt exp 2026-08-13
- ✅ nginx 1.28.3 + Node 20.20.2 + PM2 fork mode (kitehub-frontend port 4701, 122.9MB)
- ✅ `https://kitehub.me/` HTTP 200 in 360ms + `/api/health` 200 + cert_verify=0 OK
- ✅ Vercel Free Tier cap no longer a blocker (off DNS path)

**5 gap closures:**
- GAP-565 DONE (F6 SG ASCII descriptions verified) · GAP-568 DONE (BE CORS gateway pre-allowlist verified post-flip) · GAP-569 DONE (OTel 1.62.0 deployed)
- GAP-566 PARTIAL 60 (swap + alarm armed; PM2 hot-fix on EC2; repo bugs → GAP-574)
- GAP-567 PARTIAL 50 (cert acquired; auto-renewal timer + CW metric fail AL2023 → GAP-572/573)

**Wave 82 lessons learned:**
- Pre-mutation state-check per `pre-mutation-state-check.md` §3 caught AMI drift DESTRUCTIVE plan (3 EC2 replace) → pivot AMI pin PR #1400 saved kh-backend.
- STS session credential cache: IAM policy update + apply needs new workflow_dispatch session (cached perms ≠ refreshed mid-run).
- Agent design assumed SSM Parameter Store but user pre-populated Secrets Manager → PR #1403 align.
- Hot-fixes on EC2 surface 3 PM2 ecosystem.config.js bugs + AL2023 certbot no systemd units → repo source bugs tracked GAP-572..574.

### 🚀 Next Action — Self-test readiness 4-tier plan SHIPPED (2026-05-21) — Tier 0 unlock pending

**GAP-695 SHIPPED 2026-05-21** — comprehensive parent catalog cho self-test execution:
- 📋 [`GAP-695`](phase-1-beta/GAP-695-self-test-readiness-comprehensive-plan.md) — 4-tier gap catalog với dependency order + effort estimate
- 📋 [`documents/05-guides/local-dev/self-test-readiness-plan.md`](../../05-guides/local-dev/self-test-readiness-plan.md) — TL;DR + dependency graph + ordered fix sequence

**Critical path ~6-7h dev effort (1-2 dev days):**
- **Tier 0** Stack startup (~1-1.5h) gated [`GAP-694`](phase-1-beta/GAP-694-local-self-test-investigation-fix.md) — Docker Desktop launch (user action) + `.env` 9 keys populate + (optional META) preflight `check-docker.sh`
- **Tier 1** Endpoint + auth (~2h) — GAP-518 closure local + GAP-519 sidebar nav + GAP-481 routing verify + GAP-520 JWT rotation
- **Tier 2** Business flow (~3-4h) — GAP-538 onboarding E2E + GAP-637 `@PreAuthorize` backfill + GAP-531 tenant init walk + GAP-516 2FA flow
- **Tier 3** Data realism (~5-6h optional) — GAP-658 VN seed + GAP-543/659 email tone + GAP-657 email hardening + GAP-269b student endpoints

**User action required (next session):**
1. Launch Docker Desktop trên Windows host (`powershell.exe Start-Process` per `agent-action-bias.md` §1 Part B) — ~5min
2. Append 9 missing keys vào `kitehub/.env` per Phase 0A audit findings — ~10min
3. `bash kitehub/scripts/up.sh --profile infra-only` → exit gate Phase 1 Tier 0
4. Spawn Wave-pack agents parallel cho Tier 1 + Tier 2 fixes per plan §3

**Force-multiplier:** 1 catalog eliminate 30+ gap cross-reference overhead per session restart. Bypass AWS dependency (GAP-612) cho code-side closure local.

---

### 🚀 Next Action — Wave 91 code shipped (5/5 buckets + CVE) — Coordinator F BLOCKED by GAP-612 AWS suspension (2026-05-17/18)

**Wave 91 batch-1 SHIPPED (5/6 PRs merged offline despite AWS suspension):**
- ✅ #1486 Bucket C — admin-new-login-alert email template (GAP-606)
- ✅ #1488 Bucket E — FE claim code redemption page (GAP-609)
- ✅ #1490 Bucket D — beta signup BE defensive hardening (GAP-610+611; all 7 hypotheses NOT confirmed static, shipped @Query + filter test + RLS testcontainer IT; runtime bug investigation pending Coordinator F)
- ✅ #1487 Bucket A — outbox dispatcher + RMQ DLQ (GAP-605+607)
- ✅ #1489 sister fix — 3 HIGH CVEs FE base image (CodeQL alerts CVE-2026-29111 + CVE-2026-4878)
- ⏳ #1485 Bucket B — EC2 IAM ses:SendEmail — PARKED OPEN (terraform-plan CI fails do `sts:AssumeRoleWithWebIdentity` denied khi AWS suspended; code OK, fmt+validate pass; re-run CI khi AWS active → merge ngay)

**Coordinator F sequence (POST-AWS-RESTORE):**
1. User responds AWS case `177903869600100` + waits AWS approval
2. `aws sts get-caller-identity` returns identity (not suspended)
3. Re-run CI on PR #1485 (terraform-plan passes) → merge PR
4. `bash scripts/aws/start-stack.sh`
5. GAP-613 Phase 1 (CloudWatch reduce) — disable non-critical alarms + shorten log retention
6. Tag `v0.9.0-beta-staging.22` → docker-build-push wait
7. `terraform-apply.yml -f targets='aws_iam_role_policy.ec2_secrets_s3' -f confirm=APPLY -f dry_run=true` → reconcile → `dry_run=false` (Bucket B IAM apply)
8. `deploy-production.yml -f version=v0.9.0-beta-staging.22 -f confirm=DEPLOY` (Bucket A+C+D+E code deploy)
9. Backfill stuck outbox rows: SSM SQL `UPDATE subscription_outbox SET dispatched_at = NULL` (one-time)
10. Live verify all 7 code gaps + sub-finding admin endpoints
11. Flip GAP-604/602/603/605/606/607/608/609/610/611/612/613 DONE (or PARTIAL với follow-up)
12. Closure docs PR (final Wave 91)

**Stake risk:** Coordinator F sequence dài 8-10 steps, requires careful serialization per `concurrent-production-mutation-ops.md` §3.1 (Bucket B terraform apply + Bucket A+C+D+E deploy MUST serial — IAM first).

---

### 🚀 Next Action — AWS account SUSPENDED — production OUTAGE + Wave 91 BLOCKED until restore (2026-05-17) [historical]

🔴 **GAP-612 AWS account 906286017800 suspended** mid-Wave-90 walkthrough phase 2. Production stack force-stopped (CF 522). Beta cohort onboarding fully blocked.

**User action required (NOT Claude):**
1. Check email `vannkite@outlook.com` + spam folder cho AWS verification request
2. Reply to AWS với requested info
3. Wait approval (24-72h typical)
4. After restore: `aws sts get-caller-identity` → `bash scripts/aws/start-stack.sh` → resume Wave 91

**Wave 90 walkthrough phase 2 surfaced 8 bugs (filed, queued Wave 91):**
- GAP-605 P0 outbox dispatcher chưa implement (events stuck NULL)
- GAP-606 P0 email template `admin-new-login-alert.html` MISSING
- GAP-607 P1 RMQ DLQ chưa configured (poison messages infinite retry)
- GAP-608 P0 EC2 IAM thiếu `ses:SendEmail`
- GAP-609 P1 FE thiếu UI nhập claim code (chỉ accept ?token=UUID)
- GAP-610 P0 GET validate token TOKEN_NOT_FOUND (RLS suspect)
- GAP-611 P0 POST beta-signup 404 (gateway/security shadow OR Wave 89 JWT filter regression)
- GAP-612 P0 AWS account suspension (account-level blocker)

**Wave 90 phase 1 (live verify) DID succeed pre-suspension:**
- GAP-604 admin endpoint 401→200 ✅
- GAP-602 PM2 cwd ✅
- GAP-603 PM2 systemd auto-start ✅

**Walkthrough verified end-to-end PRE-suspension:**
- Submit beta request → DB row PENDING ✅
- Admin login → JWT ✅
- Admin approve → DB row APPROVED + claim_code generated ✅
- Email delivery → blocked by 4 infra bugs ❌
- FE signup deep-link → blocked by 2 BE bugs ❌

**Wave 91 sequence khi AWS restored:**
1. Verify production state (curl + SSM)
2. Plan Wave 91 cluster với 8 gaps (parallel buckets)
3. Cluster A email infra (605/606/607/608)
4. Cluster B beta signup (609/610/611)
5. Live verify all → flip DONE
6. Long-term follow-ups: P2 uptime monitoring + P2 DR plan

### Long-term P2/P3 backlog (filed 2026-05-18 via Wave 92 Bucket E — formalize Wave 90 line 158 carry-forward)

| Gap | Title | Priority | Phase | Defer rationale |
|---|---|---|---|---|
| [GAP-616](GAP-616-uptime-monitoring-external.md) | Uptime monitoring external (UptimeRobot / BetterStack integration) | 🟡 P2 | Phase 1.5+ | Outside-in observability — surfaced bởi GAP-612 (in-account monitoring đi cùng production khi account-level fail); BetterStack free tier recommend; defer khi chưa có tenant onboarded |
| [GAP-617](GAP-617-disaster-recovery-plan.md) | Disaster recovery plan (multi-region OR backup mechanism + RTO/RPO targets) | 🟢 P3 | Phase 1.5+ | Single-account single-region blast radius exposed bởi GAP-612; deprioritize P2→P3 vì chưa có tenant data đáng protect; nâng lên P2/P1 khi 5+ tenant live; cross-link GAP-257 restore drill |
| [GAP-618](GAP-618-aws-health-dashboard-daily-check.md) | AWS Service Health Dashboard daily check (automated scrape + alert) | 🟡 P2 | Phase 1.5+ | Lesson-learned từ GAP-612 (account suspension chỉ tới email manual); RSS feed ap-southeast-1 free tier + GitHub Actions scheduled workflow; solo-dev mode acceptable manual email check Phase 1 BETA |

---

### 🚀 Next Action — Wave 90 SHIPPED (live verify all DONE) + Wave 91 backlog (2026-05-17) [historical — pre-suspension]

**Wave 90 — Live verify Wave 89 (CLOSED 2026-05-17):**
- Deploy `v0.9.0-beta-staging.21` (Wave 89 gateway + PM2 code) via `deploy-production.yml` ✅
- Terraform apply targeted `aws_instance.kc_app_fe` (user_data hash from PR #1479) ✅
- EC2 reboot test: boot 16:26 UTC → pm2-ec2-user.service active 16:26:27 (17s) → both FE online no manual
- Admin endpoint `/api/v1/admin/beta-requests` baseline 401 → post-deploy **HTTP 200** với PLATFORM_ADMIN JWT ✅
- Audit: `documents/04-quality/audits/aws-verification/2026-05-17-wave-90-live-verify.md`
- 3 PARTIAL flipped DONE: **GAP-604 DONE 100%** (gateway JWT filter) + **GAP-602 DONE 100%** (PM2 cwd) + **GAP-603 DONE 100%** (PM2 systemd)
- One incidental DB realign: SQL UPDATE admin password hash (Wave 88 §8 same workaround) — recurring pattern flagged for hygiene gap

**Production state:** Stack RUNNING (kept up per user request — XLSX `phase-1-beta-acceptance-self-test.xlsx` rendered ready for walkthrough). User stops via `bash scripts/aws/stop-stack.sh --force` when done.

**Wave 91 backlog candidates:**
- (P1) Admin endpoints `/api/v1/admin/{instances,payments,revenue}` return 404 — route map missing OR controller not implemented (sub-finding from GAP-604 verify, not GAP-604 scope)
- (P3) `seed-admin-password` secret rotation hygiene — automate DB realign OR document SQL UPDATE in runbook (recurring class: Wave 88 + Wave 90)
- ~~(P2) GAP-601 ops-readiness audit (deadline 2026-05-20)~~ ✅ **DONE 2026-05-18** — ops audit shipped, score 75/100 C delta -3 vs Wave 84, GAP-614 filed
- Pre-tenant cluster: GAP-525 / GAP-514 / GAP-524 / GAP-515 / GAP-521 — unblocked (admin endpoints now work)
- Manual split queue: `documents/03-planning/inside-out-queue.md` 5th item

**Wave 92 queue (from Wave 91 ops audit + Wave 91 plan §7):**
- (P1) GAP-614 — Wave 91 Bucket D V60 RLS migration verify (filed 2026-05-18 by Wave 91 ops audit OPS-W91-010)
- (P1) GAP-613 — CloudWatch Free Tier reduce plan (Wave 91 plan Bucket F prerequisite)
- (P1) Monthly synthetic alert drill cadence — setup `alertmanager-mock-fire.yml` workflow (carry-forward Wave 84)

---

### 🚀 Next Action — Wave 89 SHIPPED + Wave 90 user-action live verifies queued (2026-05-17) [historical]

**Wave 89 — Gateway JWT + PM2 Ops Cluster (CLOSED 2026-05-17):**
- 2 buckets parallel shipped via plan PR #1478 → Bucket A PR #1480 (gateway) → Bucket B PR #1479 (PM2)
- **Bucket A:** `JwtAuthenticationGatewayFilter` (jjwt 0.13.0 pinned-match `kitehub-subscription`) + 7 unit test cases (56/56 mvn verify PASS); GAP-576 state-check verdict no-code-needed (routes already exist Wave 79+); GAP-604 PARTIAL 85% (live verify deferred)
- **Bucket B:** `pm2-ecosystem.config.js` cwd fix (monorepo nested) + `pm2 startup systemd` wired vào `ec2-kc-app.tf` user_data + defensive `scripts/deploy-fe.sh` + runbook `documents/05-guides/deploy/pm2-systemd-auto-start.md`; GAP-602 PARTIAL 80% + GAP-603 PARTIAL 70% (terraform apply + EC2 reboot test deferred)
- GAP-576 DONE 100% (no-code verdict)

**Wave 90 — user-action live verifies (BLOCKER beta cohort onboarding):**
- **GAP-604 live verify (P0):** `bash scripts/aws/start-stack.sh` → login admin → `curl -H "Authorization: Bearer $JWT" https://api.kitehub.me/api/v1/admin/beta-requests` → 200 expected (was 401)
- **GAP-602 + GAP-603 live verify (P1):** `gh workflow run terraform-apply.yml -f confirm=APPLY` (dev-trigger per `dev-authorized-terraform-trigger.md`) → triggers EC2 user_data update → `pm2 startup systemd` runs; verify via `aws ec2 reboot-instances` + curl `https://kitehub.me/` returns 200 sans manual `pm2 start`
- **GAP-601 ops-readiness audit (P2):** scheduled by 2026-05-20 per AUDIT_OVERRIDE Wave 88 trailer

**Pre-tenant gap cluster (defer Wave 91+ post GAP-604 live verify):** GAP-525 / GAP-514 / GAP-524 / GAP-515 / GAP-521

**Manual split queue:** `documents/03-planning/inside-out-queue.md` 5th item — Wave 91+

---

### 🚀 Next Action — Wave 88 SHIPPED + Wave 89 GAP-604 P0 gateway JWT propagation (2026-05-17) [historical]

**Wave 88 — Vercel decommission + cutover (CLOSED 2026-05-17):**
- All 3 workflows executed under user "claude trigger" authorization
- Gates A+B+C all green: deploy-production (gateway CORS) + terraform-apply (EIP + IAM + SNS) + cloudflare-apex-cutover (DNS flip)
- Production cutover complete: `kitehub.me` + `app.kitehub.me` → EIP `52.221.161.175` (NOT Vercel)
- FE rebuild với `NEXT_PUBLIC_API_URL=https://api.kitehub.me` (Wave 82 Bucket C miss fixed)
- Admin YAML duplicate `server:` key fixed (PR #1465 merge + tag staging.20)
- Claude Playwright walkthrough: Anonymous 11/11 PASS, Platform_Admin 10/24 PASS + 9 BLOCKED by GAP-604
- Audit: `documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md`
- Vercel auto-deploy disabled (PR #1474). User actions Wave 88 step 2-3 (dashboard + GitHub App) still pending — không block production

**Wave 89 — P0 BLOCKER (must fix before beta cohort invite):**
- **GAP-604 P0 Backend** — Gateway thiếu `JwtAuthenticationGatewayFilter`. Admin endpoints all 401 dù JWT valid. `kitehub-gateway` cần implement filter convert `Authorization: Bearer <JWT>` → `X-User-Id` + `X-User-Roles` headers cho `XUserRolesHeaderFilter` downstream. Production admin operations completely non-functional.

**Wave 89 P1 follow-ups:**
- GAP-602 P1 — `pm2-ecosystem.config.js` cwd path mismatch monorepo
- GAP-603 P1 — PM2 systemd auto-start on EC2 reboot
- GAP-601 P2 — Ops-readiness audit deferred (run by 2026-05-20)

**Pre-tenant gap cluster (defer Wave 90+ post GAP-604):** GAP-525 / GAP-514 / GAP-524 / GAP-515 / GAP-521

**Manual split queue:** `documents/03-planning/inside-out-queue.md` 5th item — Wave 90+

---

### 🚀 Next Action — Wave 87 SHIPPED + Wave 88 Vercel decommission queued (2026-05-17)

**Wave 87 — Dev Self-Test Enablement (CLOSED 2026-05-17):**
- 5 batch-1 PRs merged: A seed+creds #1470 / B preflight+reset #1469 / C CSV refine #1472 / D CORS+state-check sync #1471 / E new gaps #1468
- Walkthrough audit PR #1473 (11 Anonymous Vercel URLs PASS + API health UP + GAP-523 CORS apex `kitehub.me` ✅ nhưng subdomain `app.kitehub.me` + `kitehub.vercel.app` còn 403)
- Wave 88 PR #1474 ship: rule `no-vercel-references.md` v1.0.0 + `vercel.json` `deploymentEnabled: false` × 2 (auto-deploy disabled)
- Dev self-test toolchain ready: `bash scripts/dev/self-test-preflight.sh && bash scripts/dev/seed-personas.sh && bash scripts/render-acceptance-test-xlsx.sh phase-1-beta-acceptance-self-test`
- Bucket F (Playwright storageState) defer Wave 88+
- 2 new gaps filed: GAP-599 (P0 JWT tab collide) + GAP-600 (P1 beta_request abort cleanup)

**Wave 88 — Vercel decommission + cutover queue:**
- Step 1/3: ✅ DONE (vercel.json `deploymentEnabled: false` × 2 — auto-deploy off)
- Step 2/3: ⏳ USER ACTION — disconnect repo trong Vercel dashboard
- Step 3/3: ⏳ USER ACTION — uninstall Vercel GitHub App / scope khỏi repo
- Bucket B (sweep planning docs Vercel refs) — defer agent spawn
- Bucket C (sweep code Vercel refs) — defer agent spawn
- Bucket D (PR #1466 5-gate cutover execute): ⏳ DEV TRIGGER required (per `dev-authorized-terraform-trigger.md`)
- Bucket E (post-cutover Tier 1 verify) — gates after D

**Pre-tenant gap cluster (queue Wave 88+):** GAP-525 beta signup / GAP-514 rate-limit / GAP-524 email verify / GAP-515 lockout / GAP-521 audit log entity — defer post-walkthrough-1 để prioritize đúng

**Manual split queue (Wave 88+):** professional vs end-user manual track per `documents/03-planning/inside-out-queue.md` (5th item)

---

### 🚀 Next Action — Wave 86 expanded scope post Bucket A outside-in audits 2026-05-15 (4 P0 BLOCKERS + 21 AC additions + 17 NEW gaps GAP-582..598)

Per [`wave-2026-05-15-86-rc1-tag-preflight.md`](../../03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md) integration shipped this PR. 3 outside-in audit artifacts merged main qua PR #1432 (persona-outside-in 5×5 / benchmark-vn-saas-edu 10Qs / simulation-3axis 28 cells) converged **4 P0 BLOCKERS** + **21 AC additions distributed Bucket B-H** + **17 NEW gaps** filed.

**🚨 4 P0 BLOCKERS chặn tag `v1.0.0-rc.1`:**
1. **[GAP-584](GAP-584-magic-link-cloudflare-cache-bypass.md)** Magic-link Cloudflare cache bypass — cross-tenant invite redirect leak risk (sim cell 19); chặn Bucket E pass + Bucket G invite
2. **[GAP-585](GAP-585-cookie-consent-pdpl-decree-13-granular.md)** Cookie consent banner PDPL Decree 13 granular — compliance + first-impression damage (benchmark Q7); chặn Bucket E pass
3. **GAP-144 AlertManager receivers wired BEFORE invite** (Wave 84 P1 → Wave 86 P0 escalation cohort-invite context) — silent restart loop, MTTR <2h impossible; chặn Bucket H + Bucket G
4. **Spring Boot bump regression suite** (B-AC1+AC2+AC3 to GAP-440) — @Async/webhook/heap regression; cascades chặn Bucket G invite

**17 NEW gaps filed (Wave 86 OR defer):**
- Bucket-paired (Wave 86): GAP-582 OAuth idempotency P1, GAP-583 RDS storage alarm P1, GAP-584 CF magic-link **P0**, GAP-585 cookie consent **P0**, GAP-586 P1 invite email P1, GAP-587 P3 invite email P1, GAP-588 P2 onboarding wizard P1, GAP-589 admin bounce+impersonate P1, GAP-590 email expiry policy P1, GAP-591 cohort retention D7/D14/D30 P1, GAP-592 SLA published P2
- Defer Wave 87+: GAP-593 Most Popular badge P3, GAP-594 refund policy P2, GAP-595 landing CTA+demo P1, GAP-596 form inline validation P2, GAP-597 P2 invite management P2, GAP-598 P3 edit-window+P2 unlock P2
- Defer Wave 88+: **[GAP-599](GAP-599-jwt-tab-collide-storage-isolation.md) P0 JWT tab collide** (FE localStorage single-key → multi-actor walkthrough flaky; Wave 87 Bucket E docs mitigation shipped, sessionStorage fix Wave 88+), **[GAP-600](GAP-600-beta-request-abort-cleanup.md) P1 beta_requests abort cleanup** (no scheduled job cho stale PENDING rows; Bucket B reset script partial mitigation, @Scheduled fix Wave 88+) — Wave 87 outside-in audit #3 failure-mode matrix new finds

**Wave 86 scope expansion:** AC count B(3) + C(3) + D(1) + E(7) + F(2) + G(7) + H(14) = **37 ACs** (was 9 acceptance gates). `estimated_wall_clock` 14-20h → 24-30h.

**Carry-forwards still tracked:** GAP-576 P0 gateway auth 404, GAP-574 P1 PM2 bugs, GAP-257 P0 restore drill, GAP-144 P1 AlertManager (escalated P0 Wave 86 cohort-invite context).

### 🚀 Next Action — Phase 1 BETA acceptance walk-through 2026-05-15 (post-Wave-82 FE + post-Wave-84 Bucket H)

Full 126-row triage shipped: [`documents/05-guides/operations/acceptance-tests/phase-1-beta-walkthrough-2026-05-15.md`](../../05-guides/operations/acceptance-tests/phase-1-beta-walkthrough-2026-05-15.md). Summary: **14 PASS / 79 TESTABLE-USER / 6 BLOCKED-FE-PARTIAL / 27 BLOCKED-FOLLOWUP**. Beta-readiness **~58%** — đủ cho cohort 1-2 invite nếu chấp nhận apex Vercel `kitehub.me` làm primary UX + manual workaround admin flow (GAP-518/519/525).

**Critical blocker class:** [GAP-574](GAP-574-pm2-ecosystem-config-3-bugs.md) **P1** PM2 (`app.kitehub.me` 502) — KHÔNG chặn cứng (Vercel apex serve 200 OK trên mọi route). Phải đóng trước cohort 3+ scale hoặc trước khi shrink Vercel.

**New findings filed:** [GAP-576](GAP-576-gateway-auth-routes-404-login-verify-email-password-reset.md) **P0** (gateway auth routes 404: `/api/v1/auth/login` + `/verify-email` + `/password-reset` — chặn admin login + email verify + password reset flows). Sister candidate (`/api/v1/branding` 404) — file Wave 85 sau verify gateway route config (will claim next free GAP id ≥ 582 since GAP-577..581 used Wave 85 Bucket A integration 2026-05-15).

**Wave 85 expanded scope post Bucket A outside-in audits 2026-05-15** ([wave plan](../../03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md), [persona audit](../audits/persona-review/2026-05-15-pre-wave-85-persona-outside-in.md), [simulation audit](../audits/persona-review/2026-05-15-pre-wave-85-simulation-3axis.md)): integrated **4 P0 CRITICAL ACs** (B-AC1 RLS unit-test/B-AC6 HikariCP cross-tenant leak prevention/B-AC8 NULL force-fail/E-AC1 MaxRAMPercentage=60% override t3.small for GAP-502 RC2 recurrence prevention) + 14 additional ACs = **18 total enhancements** (8B + 1C + 1D + 3E + 1F + 4G + 1H); wall-clock 12-16h → 20-24h. **5 NEW gaps defer Wave 86:** [GAP-577](GAP-577-platform-admin-hardening-wave-86.md) P0 admin hardening (MFA + IP allowlist + 30min session + immutable admin audit) + [GAP-578](GAP-578-p2-owner-2fa-mandatory.md) P0 P2 owner 2FA + new-device alert + [GAP-579](GAP-579-soft-delete-restore-window.md) P1 soft-delete 30-day restore + [GAP-580](GAP-580-email-send-idempotency-key.md) P1 email idempotency + [GAP-581](GAP-581-per-tenant-rate-limit.md) P1 per-tenant rate limit gateway. Wave 85 cell-5 OOM scenario phải validate production 14-day post-deploy trước khi flip GAP-502 PARTIAL 90→95→DONE.

**Phase 1 BETA full launch gate (cohort 3+, ≥10 tenants):** (1) GAP-574 PM2 (2) GAP-525 invite E2E (3) GAP-518+519 admin role-guard + nav (4) GAP-524 email-verify pipeline (5) GAP-576+577 gateway routes (6) GAP-144 AlertManager (Wave 84 carry-forward P1). ETA: 2-3 waves (~2-3 tuần).

### 🚀 Next Action — Wave 82 Bucket B/C follow-ups (4 non-blocking — file 2026-05-15 post Wave 82 closure)

Wave 82 Bucket B + C deployed thành công on production 2026-05-15. 4 known follow-ups filed cho future closure:

- **[GAP-572](GAP-572-certbot-systemd-timer-al2023-not-shipped.md)** P2 — Certbot systemd timer setup fails on AL2023 (package không ship unit files). Manual cert renew works; cert valid 90d until 2026-08-13. Script fix needed (inline unit creation OR cron fallback).
- **[GAP-573](GAP-573-cloudwatch-cert-days-to-expire-publisher-not-installed.md)** P2 — CloudWatch `CertDaysToExpire` metric publisher chưa install (Step 5 abort do GAP-572 Step 4 fail). Alarm stuck `INSUFFICIENT_DATA`. Fix unblock when GAP-572 fixed.
- **[GAP-574](GAP-574-pm2-ecosystem-config-3-bugs.md)** **P1** — `pm2-ecosystem.config.js` 3 bugs (max_memory_restart `'1.2G'` invalid → use `'1200M'`; cwd path wrong cho monorepo standalone; `/var/log/pm2` perm). Hot-fix manual applied on EC2; repo source bugs persist — **future deploys fail without fix**.
- **[GAP-575](GAP-575-kiteclass-frontend-defer-phase-7.md)** P2 — `kiteclass-frontend` deploy defer Phase 7 per ADR-031 (tenant FE scope post-MVP).

**Order:** GAP-574 P1 ưu tiên cao nhất — affects mọi future FE deploy. GAP-572 + GAP-573 cùng class (auto-renewal); fix together. GAP-575 = future scope, defer until tenant signup live.

Pre-Wave-82 4 P0 prerequisites (GAP-565..568) now closed per Wave 82 Bucket B implementation. Pre-existing follow-ups still tracked: GAP-570 P2 F5 Spring 500→404 incomplete + GAP-571 P1 2 validation endpoints 500-instead-400.

### 🎉 Wave 81 SHIPPED 2026-05-15 — DEPLOY+SMOKE Backend production-ready (7 buckets + 4-attempt Bucket F fail-fast secret saga)

### 🎉 Wave 81 SHIPPED 2026-05-15 — DEPLOY+SMOKE Backend production-ready (7 buckets + 4-attempt Bucket F fail-fast secret saga)

**9 PRs merged:** #1387 (ECR matrix fix kitehub-frontend) + #1388 JWT_CHALLENGE_SECRET + #1389 TOTP+STAFF_INVITATION+KITE_VERSION + #1390 TOTP Spring relaxed binding admin yaml-less + #1391 heredoc env expansion hotfix + #1392 Bucket G spot check audit + #1393 audits-index backfill + #1394 closure cleanup + (this PR closure protocol sync).

**Outcome:**
- ✅ Backend production-ready: api.kitehub.me/actuator/health 200 UP (db/redis/disk/ssl); 7 services on tag `0.9.0-beta-staging.14`
- ✅ Admin seeded: `admin@kitehub.me` PLATFORM_ADMIN
- ✅ Infrastructure: CF DNS active + SES sandbox + Resend DKIM verified + 3 leaked creds rotated (GAP-525 closure)
- ✅ Bucket G 10/126 spot check: 8 PASS + 1 PARTIAL (beta-status 400 → Wave 82 P1) + 2 doc bugs surfaced
- ⚠️ FE Vercel STALE ~38h (Free Tier build cap hit ~2026-05-13) — Wave 78-81 contracts không reflect → full 126-row dev walk-through BLOCKED until Wave 82 Bucket B+C FE rebuild
- 4 new follow-ups → Wave 82 Bucket F: `/api/v1/beta-status` 400 + CSV row IDs mismatch + CSV `/api/v1/auth/login` path drift + `rotate-leaked-credentials.sh` wrapper name
- 4 new fail-fast guard env vars documented `.env.production.template`: JWT_CHALLENGE_SECRET + TOTP_ENCRYPTION_KEY + KITEHUB_AUTH_TOTP_ENCRYPTION_KEY + KITEHUB_STAFF_INVITATION_SIGNING_SECRET

**Wave 81 lessons learned (Bucket F 4-attempt saga):** root scope sweep at retry #2 per `release-fix-retry-budget.md` §4 — 3 fail-fast guards in `kitehub-subscription` (ChallengeTokenService + TotpSecretCipher + InvitationTokenService); KITE_VERSION stale default in fetch-secrets.sh caused /etc/kite/.env corruption; TOTP env name yaml-explicit vs Spring relaxed binding mismatch for admin yaml-less = dual-write fix; heredoc body `${ENV}` expansion = set -u trip + secret leak risk. Documented `documents/05-guides/operations/2026-05-15-wave-81-jwt-secret-fix-runbook.md`.

**Session housekeeping:** CI history 690 → 52; local branches 22 → 1. PRs 1388-1394 admin-merge bypass per `admin-merge-discipline.md` §3 GitHub Free Tier throttle context — Wave 82 Bucket E self-hosted GitHub runner on WSL eliminates class.

**Wave 82 queued:** [`documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md`](../../03-planning/waves/wave-2026-05-15-82-fe-self-host.md) 8 buckets — FE rebuild architecture (CF Pages/EC2/Vercel Pro) → deploy infra → FE build với Wave 78-81 contracts → DNS cutover → self-hosted runner → Wave 81 follow-ups → user manual P2/P3/Admin → full 126-row walk-through.

### 🚀 Next Action — Wave 82 Bucket B prerequisites (4 P0 BLOCKING — file 2026-05-15 via Bucket A outside-in failure-mode matrix audit)

User locked AWS EC2 t3.small self-host (vs Vercel Pro recommended by 2 outside-in agents). Failure-mode matrix audit surface 4 P0 mitigations PHẢI address TRƯỚC khi provision EC2 kc-app (Bucket B) hoặc DNS flip (Bucket D):

- **[GAP-565](GAP-565-wave-82-ec2-security-group-description-port-restriction.md)** P0 — EC2 security group description audit + port 4701 SG self-reference (KHÔNG public). Failure matrix F6.
- **[GAP-566](GAP-566-wave-82-t3-small-ram-tuning-pm2-swapfile-memory-alarm.md)** P0 — t3.small 2GB RAM tuning: PM2 max_memory_restart=1.2G + 2GB swapfile + CloudWatch memory alarm >85% 5min. Failure matrix F7 (ISR regen `/beta-status` OOM risk).
- **[GAP-567](GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md)** P0 — Certbot DNS-01 (Cloudflare API token) + systemd timer auto-renewal + CloudWatch CertDaysToExpire metric + alarm <30d. Failure matrix F10 (HTTP-01 race với nginx port 80, silent 90d expire → 100% outage).
- **[GAP-568](GAP-568-wave-82-be-cors-allowlist-sweep-pre-dns-flip.md)** P0 — BE `CORS_ALLOWED_ORIGINS` sweep 7 services (kitehub-{admin,branding,email,gateway,subscription} + kiteclass-{core,gateway}) + preflight OPTIONS verify từng endpoint trước khi flip DNS. Failure matrix F11 (silent CORS reject post-cutover).

**Order:** GAP-565/566/567 ship trong Bucket B EC2 provisioning (cùng terraform apply); GAP-568 ship sau Bucket B (new origin xác định) NHƯNG trước Bucket D DNS flip. Mỗi gap require AWS verification artifact per `pre-mutation-state-check.md` §3 trước khi user trigger terraform apply / DNS edit.



### 🎉 Wave 80 SHIPPED 2026-05-15 — v1.0.0-rc Blockers (4 buckets + 1 fix-cycle + 1 gitignore pre-add)

**6 PRs merged:** #1378 plan + #1379 Bucket A (META audit format v2 ALL 5 cats per SOC2/ISO27001/OWASP ASVS) + #1380 chore(gitignore) user-manual PDF pre-add + #1381 Bucket C (RBAC FE RoleGuard + BE @PreAuthorize PaymentController + SubscriptionController + audit handler) + #1382 Bucket D (F2 user manual 15 sources P2/P3/Admin + Puppeteer PDF script + Playwright capture script + 10/20 screenshots) + #1383 Bucket B (invite-staff email + InvitationController real impl + HMAC token TTL 7d + 3 FE routes + V49 audit log + Suspense boundary fix).

**Outcome (gap-status.csv canonical):**
- 3 gaps DONE 100%: GAP-561 (invite-staff full flow) · GAP-561b (Wave 80 closure) · GAP-564 (META audit v2 ALL 5 cats)
- 3 gaps PARTIAL: GAP-537→75 (F2 sources + scripts + 10/20 screenshots; P2/P3 placeholder → GAP-537c) · GAP-562→90 (kitehub-branding @PreAuthorize defer Wave 81) · GAP-562b→85 (FE RoleGuard + most BE; kitehub-branding defer Wave 81)
- 1 NEW gap filed: GAP-537c P1 (P2 Owner + P3 Manager screenshots + Tier 2 annotation — Wave 81)
- **v1.0.0-rc gate cleared:** 2FA versioning + RBAC enforcement + audit v2 format + invite flow all shipped → Wave 81 DEPLOY+SMOKE unblocked

**Infra changes:**
- V49__create_staff_invitation_audit_log.sql (audit trail per state transition CREATED/SENT/ACCEPTED/REVOKED/RESENT)
- `RbacAccessDeniedHandler` wired into SecurityConfig (writes admin_audit_log on every 403 — IP + UA + denied role)
- `InvitationTokenService` HMAC-SHA256 TTL 7d + `@PostConstruct` fail-fast production dev-default guard
- Frontend: `RoleGuard` component + `useRole` hook + 3 layout wraps (billing/branding/settings) + Sidebar customerNav role filter
- Frontend: `/admin/staff` list + `/admin/staff/invite` form + `/staff/accept-invite` public Suspense-wrapped landing + 3 help/{p2-owner,p3-manager,platform-admin} persona landings
- `scripts/render-user-manual-pdf.{sh,mjs}` Puppeteer A4 portrait header/footer
- `scripts/capture-user-manual-screenshots.{sh,mjs}` Playwright vi-VN 1440×900 desktop + 375×812 mobile fallback
- 5 Wave 78 audit reports annotated "v1 format" banner cross-link to GAP-564 v2 template

**Wave 80 closure protocol satisfied per `gap-done-discipline.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:**
- ✅ Wave plan `status: draft` → `status: complete` flipped + Bucket D retroactive note
- ✅ `wave-history.jsonl` Wave 80 entry appended (Rule 15)
- ✅ ROADMAP §🚀 Next Action updated (this section)
- ✅ Worktree prune scheduled (this closure PR)
- ✅ 4 gap markdown Log entries + gap-status.csv 7 row updates (6 affected + 1 new GAP-537c)
- ✅ GAP-537c follow-up filed Wave 81 target

**Post-wave audit suite due ≤3 ngày (per `post-wave-audit-mandate.md` §2.2) — MUST use v2 format per GAP-564:**
- UI /128 across admin staff routes (invite + list + accept) + customer billing/branding/settings RoleGuard + help/{p2,p3,admin} persona landings
- API Contract /100 across 5 staff invitation endpoints + 14 RBAC-protected billing+subscription endpoints
- Business Logic /100 (RBAC enforcement + invite flow + audit log)
- Security /100 **v2 format** (per-control evidence Command/Output/Verdict/EvidenceID per SOC2/ISO27001/OWASP ASVS) — verify RBAC + HMAC token + audit handler
- Ops Readiness /100 (V49 migration + Puppeteer/Playwright dev deps)
- Quality /100 weekly refresh


### 🎉 Wave 79 SHIPPED 2026-05-14 — Beta Invite Close-Out v1.0.0-rc gate (8 buckets + 4 cleanup PRs + 1 META gap filed)

**12 PRs merged:** #1364 Bucket 0 Foundation (3 new domains 3-layer auth-2fa+roles+cookie-consent + 15-key config registry + 2 MSW handlers) · #1365 Bucket A P0 v1.0.0-rc gate (2FA versioning + feedback gateway+tenantId + 22-key @Value wiring) · #1366 Bucket B P0 outside-in invite (V45/V46 + RBAC OWNER/STAFF + staff invitation skeleton — PARTIAL self-declared, GAP-558 cookie consent split out) · #1367 Bucket C P1 cluster security (password-reset BE + default-deny + TOTP/JWT fail-fast + tenant JWT cross-check) · #1368 Bucket D P1 UX retention (Radix Dialog focus-trap + onboarding nav + disclaimer specificity + data-reset-policy) · #1369 Bucket E P1 docs+tests (testcontainers + support scope note + BR-AUTH-009 + BR-refs audit) · #1370 + #1371 Bucket F1 P1 META user-manual-content-standard rule v1.0.0 + 5 anonymous-prospect MDX pages · #1372 Bucket F-bis P1 admin impersonation + V48 audit log + 30s TTL + cleanup unused imports · #1373 docker-compose hardcoded password placeholders (GitHub Secret Scanning fix) · #1374 cleanup unused fields + resource leak · #1375 GAP-564 META filed.

**Outcome (gap-status.csv canonical):**
- 13 gaps DONE 100%: GAP-040 (impersonation) · GAP-545 (Dialog focus-trap) · GAP-547 (2FA versioning) · GAP-548 (password-reset BE) · GAP-551 (feedback gateway+tenantId) · GAP-552 (default-deny) · GAP-553 (TOTP/JWT fail-fast) · GAP-554 (onboarding tenant cross-check) · GAP-555 (15-key config wiring) · GAP-556 (support scope clarification) · GAP-557 (BR-refs audit) · GAP-559 (onboarding nav) · GAP-560 (data-reset policy) · GAP-563 (META user-manual-content-standard rule)
- 4 gaps PARTIAL: GAP-537→25% (F1 anonymous sample only; F2 P2/P3/Admin defer Wave 80+) · GAP-544→80% (testcontainers IntegrationTest migrated; DatabaseBackupServiceTest mock-kept) · GAP-561→50% (V45 + 501 stubs; email+FE deferred GAP-561b) · GAP-562→50% (V46 + PlatformRole + @PreAuthorize staff; FE role-guard+billing/branding @PreAuthorize deferred GAP-562b)
- 1 NEW gap filed: GAP-564 META P0 (outside-in expanded) — security-audit skill ALL 5 categories must mandate per-control evidence (Command run + Output + Verdict + Evidence artifact ID) per SOC2/ISO27001/OWASP ASVS baseline; bumped P1 → P0 block v1.0.0-rc promotion
- 1 NEW gap deferred: GAP-558 cookie consent banner FE+BE (PDPL deadline 2026-07-01 still in window — Wave 80+ Bucket TBD)
- Phase 1 BETA Plan 1 invite-ready: v1.0.0-rc gate cleared (2FA versioning + 15-key config wired + RBAC P0 + 4 OWASP P0 closed) — Wave 80 DEPLOY next

**Infra changes:**
- V45__create_staff_invitations.sql + V46__create_rbac_roles.sql + V47__add_user_password_reset_columns.sql + V48__create_impersonation_audit_log.sql (4 NEW migrations)
- KitehubSubscriptionApplication @EntityScan extended +feedback.entity +onboarding.entity +staff.entity +impersonation (latent Wave 78 bug catch + Wave 79 new modules)
- KiteHubAdminApplication @EnableJpaRepositories + @EntityScan extended +staff.repository +staff.entity +impersonation
- kitehub-gateway: +kitehub-feedback-v1 route + 6 2FA routes (v1 + legacy alias) with circuit-breaker + rate-limit
- SecurityConfig: anyRequest().authenticated() default-deny + explicit allowlist + 2FA endpoints carved out
- TotpSecretCipher + ChallengeTokenService @PostConstruct fail-fast guard for production dev-default
- OnboardingProgressController X-Tenant-Id × JWT claim cross-check via TenantHeaderJwtMismatchException
- ImpersonationService + ImpersonationAuditEntry + 30s TTL hard-limit + audit log ip+user_agent
- StaffInvitation entity + PlatformRole enum (OWNER/STAFF) + @PreAuthorize staff endpoints
- Vercel git.deploymentEnabled main-only whitelist (GAP-495 closed — non-main PRs skip Vercel)
- kiteclass/docker-compose*.yml 11 hardcoded passwords → ${VAR:-CHANGE-ME-dev-only} placeholders (GitHub Secret Scanning fix)

**Rules shipped:**
- `user-manual-content-standard.md` v1.0.0 (META P1 force-multiplier) — 15-item checklist + persona discoverability matrix + reviewer-checklist + worked self-test on F1 5-page anonymous sample; paired same-PR `output-review-mandate.md` §3 matrix row + `rules-index.csv` row

**Wave 79 closure protocol satisfied per `gap-done-discipline.md` + `post-merge-sync-completeness.md` §2 + `post-wave-cleanup.md`:**
- ✅ Wave plan frontmatter `status: complete` flipped
- ✅ `wave-history.jsonl` appended (Rule 15)
- ✅ ROADMAP §🚀 Next Action updated (this section)
- ✅ Worktree prune scheduled (this closure PR)
- ✅ 14 gap markdown Log entries + gap-status.csv 16 rows synced (this closure PR)
- ✅ ADMIN_MERGE_OVERRIDE trailer cited on each bucket PR — Vercel rate-limit GAP-495 class + local test pass evidence per `admin-merge-discipline.md` §4
- ✅ AUDIT_OVERRIDE trailer cited — closure-audit deferred to ≤3-day window per `post-wave-audit-mandate.md` §2.2 (Wave 79 multi-domain ineligible for §2.4 milestone deferral)

**Post-wave audit suite due ≤3 ngày (per `post-wave-audit-mandate.md` §2.2):**
- UI /128 across kitehub-frontend admin (impersonation + onboarding nav) + auth (2FA flow) + public (Footer/Disclaimer)
- API Contract /100 across 2FA endpoints + staff endpoints + impersonation endpoints + feedback gateway
- Business Logic /100 across 3 NEW rules.md (auth-2fa + roles + cookie-consent) + BR-refs audit
- Security /100 (default-deny + fail-fast + tenant cross-check + RBAC + impersonation TTL) — **NEW format v2 per GAP-564 expanded** (per-control evidence template Command run + Output + Verdict + Evidence artifact ID)
- Ops Readiness /100 (V45/V46/V47/V48 + Docker non-root + SecurityConfig default-deny)
- Quality /100 weekly refresh

**Outside-in audit findings (paired same-PR with closure):**
- `documents/04-quality/audits/persona-review/2026-05-14-gap-564-outside-in-audit-skill-trust.md` — 3 personas (Legal Counsel K-12 / Insurance & Compliance Auditor / Beta Tenant Security Officer) verdicts REJECT 5/5 categories Wave 78 audit format; expand GAP-564 → all 5 categories; bump P1 → P0; format v2 template required forward


### 🎉 Wave 78 SHIPPED 2026-05-14 — Beta Invite Launch Retain UX/trust (7 buckets + 1 hotfix + GAP-544 filed)

**7 bucket PRs merged:** #1349 Bucket 0 Foundation (4 NEW api-contract.md + MSW handlers) · #1351 Bucket A FE Polish (Prospects UI kit + VN i18n audit, GAP-428/541) · #1352 Bucket D Admin/Security (FE role compat + beta invite runbook, GAP-518/480) · #1353 Bucket E Email + Smoke (kitehub-email actuator + 5-template audit + tenant init handoff runbook, GAP-527/543/531) · #1354 Bucket C Backend Close-out (auth rate limit + Retry-After UX + env config, GAP-508/514/515) · #1355 Bucket F Beta Business (feedback widget + email survey day-7/14 + footer support, GAP-542/540 — admin-merge w/ GAP-544 follow-up) · #1356 Bucket B UX Onboarding (checklist + sample data seed + beta disclaimer + /beta-status, GAP-538/539, V43 — admin-merge w/ GAP-544 follow-up). Plus hotfix #1350 (2FA TS strict-mode errors unblock all FE CI) + PR #1301 (Wave 72b Bucket A TOTP 2FA BE rebased 56 commits + merged, GAP-516 BE 80%).

**Outcome (gap-status.csv canonical):**
- 2 gaps DONE 100%: GAP-480 (beta invite runbook), GAP-515 (FE Retry-After UX + BE lockout total)
- 12 gaps PARTIAL advanced: GAP-428→70 / GAP-508→75 / GAP-514→90 / GAP-518→90 / GAP-527→60 / GAP-531→50 / GAP-538→85 / GAP-539→90 / GAP-540→80 / GAP-541→60 / GAP-542→80 / GAP-543→40
- 1 NEW gap: GAP-544 (kitehub-subscription integration tests Postgres :5433 testcontainers flakiness, P1, Wave 79)
- Phase 1 BETA Plan 1 invite-ready: onboarding + trust + feedback + support discoverability foundation shipped

**Infra changes:**
- V43__create_onboarding_progress_table.sql + V44__create_feedback_submissions_table.sql (2 NEW migrations)
- kitehub-gateway/application.yml: `/api/auth/password-reset-request` route + 7-route rate limit policy per `pre-launch-auth-hardening-checklist.md` §2.1
- kitehub-email/application.yml: actuator endpoints expose (health, info, metrics, prometheus)
- KiteHubAdminApplication: +2 entity packages + 2 repo packages (onboarding + feedback) per GAP-382 admin scan
- Floating FeedbackWidget (5-star + comment + email) + Footer with support@/Help/beta-status links
- BetaDisclaimerBanner (cookie-persistent dismiss) + /beta-status static MVP page
- 12 business docs (3-layer × 4 NEW domains: onboarding/feedback/beta-status/support) + 4 MSW handlers
- 580-line beta-invite-flow.md runbook + tenant-init-handoff runbook + 5 email-template audit notes
- scripts/smoke-email-actuator.sh + AuthRouteRateLimitConfigTest (8 structural assertions)

**Wave 78 closure protocol satisfied per `gap-done-discipline.md` + `post-merge-sync-completeness.md` §2:**
- ✅ Wave plan frontmatter `status: complete` flipped
- ✅ `wave-history.jsonl` appended (this PR — Rule 15)
- ✅ ROADMAP §🚀 Next Action updated (this section)
- ✅ Worktree prune ran (pre-closure)
- ✅ AUDIT_OVERRIDE trailer cited on each bucket PR — closure-audit deferred to ≤3-day window per `post-wave-audit-mandate.md` §2.2 (Wave 78 multi-domain ineligible for §2.4 milestone deferral)

**Post-wave audit suite due ≤3 ngày (per `post-wave-audit-mandate.md` §9 of Wave 78 plan):**
- UI /128 across kitehub-frontend public + dashboard + auth (Buckets A/B/D/F)
- API Contract /100 across 4 NEW endpoints + auth + onboarding + feedback controllers (Buckets B/C/F)
- Business Logic /100 across 4 NEW rules.md + 5 email-template audit findings (Bucket E)
- Security /100 (auth rate limit + lockout + Retry-After + admin role compat — Bucket C/D)
- Quality /100 weekly refresh

### ✅ Wave 77 SHIPPED 2026-05-14 EOD — Beta Invite Launch Foundation SEND (4 buckets + 1 hotfix)

**5 PRs merged:** #1339 plan + #1347 hotfix (AuthService DI + V42 ALTER) + #1343 Bucket A (Email SEND DNS+runbook+smoke) + #1344 Bucket B (kitehub-email actuator healthcheck) + #1345 Bucket C (cred rotation automation) + #1346 Bucket D (V39/V40/V41 + invite token single-use + slug normalize VN + idempotency).

**Outcome (gap-status.csv canonical):**
- GAP-370 60% → 95% / GAP-502 80% → 90% / GAP-525 50% → 85% / GAP-530 0% → 10%
- 4 new outside-in P0 gaps filed + advanced: GAP-533 80% / GAP-534 80% / GAP-535 70% / GAP-536 65%
- 12 active Phase 1 BETA P0 PARTIAL (no OPEN remaining)
- 51/51 Bucket D tests PASS; AuthService DI + login_audit_log schema bugs hotfixed

**Mid-session incident survived:** GitHub account `VictorAurelius` suspended ~10:15 UTC → restored ~12:55 UTC via appeal (~2h40m downtime). Zero data loss. Permanent insurance shipped:
- Multi-pushurl mirror (`origin` pushes BOTH GitHub + GitLab)
- GitLab project `gitlab.com/victoraurelius/kite-class-platform` (private, full 112 branches + 13 tags)
- Self-hosted runner `kite-dev-wsl2-shell` (unlimited free CI failover)
- Weekly tarball cron Sunday 02:00 UTC
- Incident log [`2026-05-14-github-account-suspension-and-gitlab-migration.md`](../audits/incidents/2026-05-14-github-account-suspension-and-gitlab-migration.md)

### 🟢 Wave 78 Bucket D SHIPPED 2026-05-14 — Admin/Security Close-out (GAP-518/480)

**Scope:**
- GAP-518 (admin role compat PLATFORM_ADMIN vs ADMIN) — regression-safe `auth-helpers.test.ts` (10 cases) added; CSV `completion_pct` 80 → 90; live browser walkthrough still gated per `pre-handoff-self-test-completeness.md` §2.4 (b)(c)
- GAP-480 (beta invitation flow undefined) — runbook `documents/05-guides/operations/beta-invite-flow.md` shipped covering 5 bước end-to-end (public request → admin review → email → invitee signup → owner first login) + smoke checklist + per-step failure modes; OPEN → DONE (100%)

**Files touched:**
- `kitehub/kitehub-frontend/src/lib/__tests__/auth-helpers.test.ts` (NEW)
- `documents/05-guides/operations/beta-invite-flow.md` (NEW)
- `documents/05-guides/operations/README.md` (link)
- `documents/04-quality/gaps/{GAP-480,GAP-518}*.md` + `gap-status.csv`

Local verify: 10/10 auth-helpers tests PASS · `pnpm build` GREEN · `check-gap-status-csv.sh` PASS (364 rows).

### ✅ Wave 73 SHIPPED 2026-05-14 — Meta Context Optimization (path-scope 30 rules + UserPromptSubmit dynamic inject + context-budget-mandate)

**9 PRs merged:** #1308 plan + #1309 Bucket 0 + #1310/#1311/#1312/#1313/#1314 Buckets A1-A5 (path-scope ~30 MANDATORY rules) + #1315 Bucket C (UserPromptSubmit hook inject-rule-digest.py + 20 tests pass) + #1316 Bucket D (context-budget-mandate.md v1.0.0 + output-review-mandate v1.6→v1.7 + rules-index 54→55).

**Outcome:**
- ~30 MANDATORY rules path-scoped via Anthropic native `paths:` frontmatter — auto-load chỉ khi Claude đọc file matching glob
- UserPromptSubmit hook live-verified injecting rule digests on keyword match (audit, deploy, merge, terraform, gap closure, etc.)
- `context-budget-mandate.md` codifies <120k base mandate
- Estimated context savings: ~150k tokens per session

**Deferred (filed):**
- 🔵 **GAP-528 OPEN P1 Meta** — Bucket B (8 deterministic enforcement hooks) agent worktree self-deadlocked + auto-cleaned; needs re-implement via stub-first pattern. Lessons-learned codified in GAP-528 AC.

### ✅ Wave 68 SHIPPED 2026-05-13 (verification pass + bonus drift fix)

- ✅ **Verification 3/3** — SES API check (DENIED CaseId 177857212400418, user accept sandbox), smoke E2E `api.kitehub.me/actuator/health` 200 + kh_backend TG healthy, audit 87/100 baseline maintained (Wave 53 refresh in 3-day freshness)
- 🆕 **GAP-501 filed + DONE** — kc_app ALB target group drift post-Vercel pivot (502 on `/`, `/auth/*`, `/dashboard/*` via api.kitehub.me)
- ✅ **PR #1250** — `infrastructure/terraform-aws/ec2.tf` remove 3 ALB resources (TG + attachment + listener rule); pre-mutation audit artifact filed
- ✅ **PR #1251** — sync: GAP-501 DONE + gap-status.csv per Rule 17
- ✅ Terraform apply user-triggered: dry-run 25783133968 + real apply 25783192647 SUCCESS (`0 add, 0 change, 3 destroy`)
- ✅ **Live smoke verified** — `api.kitehub.me/` 502→**404**; `/auth/login` 502→**404**; `/actuator/health` 200 unchanged; TG `kitehub-kc-app-tg` returns `TargetGroupNotFound`
- ⚠️ **GAP-370 SES re-submit needed** — API trả DENIED CaseId 177857212400418; user accept sandbox for now; needs manual investigation hoặc re-submit qua AWS Console

### 🔴 BLOCKER (2026-05-13 audit-of-trust pass)

**GAP-502 P0 BLOCKING filed** — kh_backend production thrashing:
- RC1: RabbitMQ `AuthenticationFailureException ACCESS_REFUSED PLAIN` — Spring context init fail → restart loop (11 die events/1h)
- RC2: Container OOM kills — JVM real footprint exceeds 320-480 MiB limits; GAP-447 sizing assumption (t3.medium 4GB, "1.5GB headroom") invalidated; actual headroom ~0.4 GB
- Plan 1 self-test BLOCKED — API endpoints unstable (POST `/api/v1/beta-access/request` returns 400 empty OR 502)

Full evidence: [`audits/aws-verification/2026-05-13-audit-of-trust-production-instability.md`](../audits/aws-verification/2026-05-13-audit-of-trust-production-instability.md)

**Fix sequence (user-triggered, mutation):**
1. **GAP-502** (P0 BLOCKING): SSH/SSM kh_backend → diagnose RabbitMQ creds vs `/etc/kite/.env` (option A) hoặc defer rabbit listener (option B); restart stack + verify 30 min stable
2. **GAP-503** (P1, follow-up — depends GAP-502): Tier 2 config optimization — JVM `MaxRAMPercentage=50.0` + Tomcat thread tune + HikariCP right-size + healthcheck grace period (~5-6h, no infra change)
3. Re-run audit-of-trust pass → unblock Plan 1
4. Phase 1.5 prep: terraform Architecture A (single t3.large) ready cho trigger gate ≥30 paying tenants

### ✅ Wave 72a SHIPPED 2026-05-14 — Self-test unblock + OWASP A07 P0/P1 hardening (6 buckets parallel)

**Plan PR #1282** + **6 bucket PRs** (#1283 A + #1284 D + #1285 C + #1286 E + #1287 B + #1288 F) — all merged to main. Side-track: **#1289** statusline sync + **#1290** starter-kit v2.4.0 retro-sync (17 new + 3 updated rules) + canonical remote **#11** also merged.

**Gaps outcome:**
- 🟢 **GAP-522 DONE** — security-audit Cat 1/2/3/5 per-check rubric extension + 4 sister rules (`pre-launch-{dependency,secrets,owasp-rest,infra}-hardening-checklist.md`)
- 🟡 **GAP-514 PARTIAL 66%** — gateway rate limit on 5 missing auth routes; live 429 smoke deferred to post-deploy
- 🟡 **GAP-515 PARTIAL 80%** — V35 lockout columns + AuthService 5-attempt/15min + exponential backoff; FE Retry-After UX deferred
- 🟡 **GAP-518 PARTIAL 80%** — FE role-guard widen `'OWNER'|'ADMIN'|'PLATFORM_ADMIN'` + login redirect fix; live admin walkthrough deferred to user
- 🟡 **GAP-519 PARTIAL 80%** — admin sidebar with 4 testid'd nav links; live click-through deferred
- 🟡 **GAP-520 PARTIAL 90%** — JWT dual-key verifier + rotation runbook; first real AWS Secret rotation = ops follow-up
- 🟡 **GAP-521 PARTIAL 70%** — V36 admin_audit_log + AOP aspect + @Auditable annotation on 2 endpoints; other controllers + FE review page deferred
- 🟡 **GAP-525 PARTIAL 50%** — credential-rotation-runbook + incident artifact (3 credentials by class/ID only); actual rotation = user-action

**New artifacts:** [`documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`](../../05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv) 126 rows pre-filled in Vietnamese with UTF-8 BOM (Wave 72b Bucket G relocated + translated; supersedes archived `plan-1-self-test-e2e-superseded.md`) — user opens in spreadsheet, ticks `status` column, surfaces blocker gaps per row. Render XLSX: `bash scripts/render-acceptance-test-xlsx.sh <csv>`.

### 🚀 Next Action (Wave 77 SHIPPED EOD 2026-05-14 — user-action deploy follow-ons + Wave 78 queued)

📍 **Next session ĐỌC TRƯỚC:** [`documents/03-planning/session-handoffs/2026-05-14-eod-session-handoff.md`](../../03-planning/session-handoffs/2026-05-14-eod-session-handoff.md) — full state + pickup order

**Wave 77 SHIPPED** — Beta Invite Launch Foundation SEND. 4/4 buckets MERGED + 1 hotfix:
- ✅ PR #1339 plan + #1347 hotfix (AuthService DI + V42 ALTER login_audit_log)
- ✅ PR #1343 Bucket A — Email SEND foundation (terraform-cloudflare DNS + deliverability runbook + 2 smoke scripts) — GAP-370 95% / GAP-533 80% / GAP-530 10%
- ✅ PR #1344 Bucket B — kitehub-email actuator healthcheck — GAP-502 90%
- ✅ PR #1345 Bucket C — Credential rotation automation wrapper — GAP-525 85%
- ✅ PR #1346 Bucket D — Tenant signup security (V39/V40/V41 + InviteTokenService + TenantSlugNormalizer + IdempotencyService + 51/51 tests) — GAP-534 80% / GAP-535 70% / GAP-536 65%

**Phase 1 BETA P0 state (gap-status.csv canonical):** 12 active PARTIAL items (no OPEN remaining) — see EOD handoff for table.

**User-action follow-ons for Wave 77 deploy (next session):**

1. **Bucket A deploy** (~30min user time):
   - Resend dashboard: add domain `kitehub.me` → capture 3 DKIM CNAME values
   - `cp infrastructure/terraform-cloudflare/terraform.tfvars.example terraform.tfvars` → fill values
   - Pre-apply audit artifact `documents/04-quality/audits/cloudflare-verification/YYYY-MM-DD-bucket-a-email-dns.md`
   - `cd infrastructure/terraform-cloudflare && terraform init && terraform plan -out=tfplan && terraform apply tfplan`
   - `dig` verify 5 DNS records propagated
   - Warm-up Day 1-7 per `documents/05-guides/deploy/email-deliverability-runbook.md`
   - Mail-tester ≥8/10 × 3 consecutive runs → flip GAP-533 toward DONE

2. **Bucket B deploy** (~5min):
   - `gh workflow run deploy-production.yml -f confirm=APPLY -f dry_run=false`
   - Post-deploy (~150s start_period): `aws ssm send-command` → verify `docker ps --filter name=kitehub-email --format "{{.Status}}"` shows `Up X minutes (healthy)`

3. **Bucket C rotation execution** (~25min, 3 creds):
   - `bash scripts/rotate-leaked-credentials.sh --cred=admin-password` (~10min)
   - `bash scripts/rotate-leaked-credentials.sh --cred=resend-api-key` (~10min)
   - `bash scripts/rotate-leaked-credentials.sh --cred=cloudflare-token` (~5min)
   - Fill 3 audit skeleton files (wrapper-generated)
   - Commit trailer to flip GAP-525 DONE: `GAP-525_USER_ROTATED: admin-pwd YYYY-MM-DD / cloudflare YYYY-MM-DD / resend YYYY-MM-DD`

4. **Bucket D deploy** (auto via deploy workflow):
   - Flyway V39 → V40 → V41 will apply on next `deploy-production.yml` run
   - Live verify per `pre-handoff-self-test-completeness.md` §2.4 → flip GAP-534/535/536 toward DONE

5. **GAP-530 5-email-type live verify** post-Bucket-A warm-up (final blocker for Plan 1 invite send)

**Pending PRs (open from this session):**
- 🟢 **PR #1340** Meta inside-out-completeness-trigger rule (ready for merge if CI green)
- 🟡 **PR #1341** Wave 78 plan (DRAFT) — Sub-wave B RETAIN scope (6 buckets / 14 P0); promote to ready after Wave 77 deploy
- 🟢 **PR #1342** Incident log + backup script + GitLab CI smoke (ready for merge if CI green)

**Sub-wave B (Wave 78) DRAFT PR #1341** — RETAIN UX/trust scope (6 buckets / 14 P0 items):
- Inside-out close-out: GAP-508 env config registry meta + GAP-514 rate limit live 429 smoke + GAP-515 lockout FE Retry-After UX + GAP-518 live admin walkthrough + GAP-428 Prospects UI kit (P0 effective)
- Outside-in NEW P0: GAP-538 onboarding+sample data + GAP-539 beta disclaimer+/beta-status page + GAP-540 support channel discoverability + GAP-541 customer-facing VN i18n audit + GAP-542 feedback channel widget + GAP-543 email content audit 5 types
- Cross-layer YES → Bucket 0 Foundation required (api-contract.md + MSW handlers for 4 NEW endpoints)
- Defer Wave 79: GAP-348 + GAP-364 UI kit polish + GAP-537 user manual + GAP-040 support impersonation + Premium plan disclaimer

**GitHub suspension incident permanent insurance shipped:**
- Multi-pushurl mirror (origin → GitHub + GitLab simultaneously)
- Self-hosted GitLab runner `kite-dev-wsl2-shell` (failover ready)
- Weekly tarball cron Sunday 02:00 UTC → `~/backups/`
- Incident log `documents/04-quality/audits/incidents/2026-05-14-github-account-suspension-and-gitlab-migration.md`
- 5 follow-up gaps tracked (off-device backup automation / workflow translation / AWS OIDC dual-issuer / burst push throttle / migration runbook)

---

### 🚀 Next Action (Wave 78 — Beta Invite Launch Retain UX/trust, 2026-05-14 draft — pipelined on Wave 77)

**Wave 78 plan drafted** — `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` — 6 buckets parallel + Bucket 0 Foundation (cross-layer YES — 4 NEW endpoints `/feedback` + `/api/v1/beta-status` + `/api/v1/onboarding-progress` + `/api/v1/support-tickets`). 14 P0/P1 items total. Estimated wall-clock ~6-8h longest bucket. Stake tier HIGH → Opus 4.7 full.

**Inside-out close-out (8 existing):**
- 🟡 GAP-428 (Prospects UI kit P1 → P0 effective RETAIN) — landing/pricing/signup pages first-touch
- 🟡 GAP-480 (beta invite flow doc) — runbook covering end-to-end pipeline
- 🟡 GAP-508 PARTIAL 60% (env config registry meta) — extend with 4 NEW endpoints
- 🟡 GAP-514 PARTIAL 66% (rate limit live 429 + `/api/auth/password-reset-request` route)
- 🟡 GAP-515 PARTIAL 80% (FE Retry-After UX)
- 🟡 GAP-518 PARTIAL 80% (role mismatch live walkthrough verify)
- 🔵 GAP-527 (kitehub-email actuator + E2E smoke)
- 🔵 GAP-531 (tenant init handoff post-approve walkthrough)

**6 NEW gaps filed Wave 78:**
- 🔵 GAP-538 P0 — Day-1 onboarding checklist + sample/demo data seed (outside-in N1)
- 🔵 GAP-539 P0 — Beta disclaimer banner + /beta-status page (outside-in N2)
- 🔵 GAP-540 P0 — Beta support channel discoverability (support@ + widget + footer) (outside-in N7)
- 🔵 GAP-541 P0 — Customer-facing VN i18n audit (TOS + email + banner) (outside-in N8)
- 🔵 GAP-542 P0 — Feedback channel (in-app widget + email survey day-7/14) (inside-out — user confirmed)
- 🔵 GAP-543 P0 — Email content audit 5 critical email types (inside-out — user confirmed)

**Buckets:** 0 Foundation api-contract.md + MSW · A FE Polish (GAP-428+541) · B UX onboarding (GAP-538+539) · C Backend close-out (GAP-508+514+515) · D Admin/security (GAP-518+480) · E Email + smoke (GAP-527+543+531) · F Beta business (GAP-542+540).

**Wave 79 queue (polish + defer):**
- Premium plan implementation (DEFERRED from Wave 78 per user confirm)
- GAP-040 (support impersonation — defer; not Phase 1 BETA RETAIN blocker)
- UI kit polish remaining gaps (post-Wave 78 Bucket A baseline)
- GAP-537 user manual Vietnamese — screenshots-based per persona (deferred to Wave 79)

**Draft PR pipelined on top of Wave 77 PR #1339** — do NOT auto-merge until Wave 77 lands + user reviews Wave 78 plan.

---

### 🚀 Next Action — historical (Wave 72b — 2FA + audit rubric review + admin verify, post-Plan 1 user walkthrough) [SUPERSEDED by Wave 77]

**Wave 72a P0 BLOCKERS unblocked** — admin UI now usable (GAP-518/519 PARTIAL 80%); auth surface protected (GAP-514 rate limit + GAP-515 lockout PARTIAL); credentials covered by runbook (GAP-525 PARTIAL 50%); meta rubric extended (GAP-522 DONE).

**Wave 72b plan stub** (filed in Wave 72a plan PR #1282): `wave-2026-05-14-72b-2fa-audit-rubric-review.md` — 5 buckets, runs after Wave 72a closure. Full §3-§5 elaboration deferred to dedicated Wave 72b plan PR per stub §4 spawn condition.

**Wave 72b candidate scope:**

1. **GAP-526 P1** — Verify admin UI subpages reach correct backends (3 subpages × DevTools click-through). **Depends on Wave 72a Bucket C live walkthrough.**
2. **GAP-516 P1** — 2FA TOTP mandatory for PLATFORM_ADMIN (cross-layer FE+BE, Bucket 0 Foundation required for new endpoints)
3. **GAP-517 P2** — PLATFORM_ADMIN login alert from new IP/UA + Resend transactional template
4. **GAP-523 P0 META** — Audit skill rubric review wave: apply primacy + per-check to 6 sister skills (quality / ops-readiness / performance / api-contract / business-logic / ui-review)
5. **GAP-524 P1 META** — pre-handoff rule additional flow classes (file-upload, payment, multi-tenant, SSE, async job, time-sensitive, i18n)

**Pre-Wave-72b user-action (Plan 1 walkthrough first):**

- Open `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` in spreadsheet (run `bash scripts/render-acceptance-test-xlsx.sh <csv>` for Excel UX)
- Tick each row's `status` column (pass/fail/blocked/-)
- File bugs surfaced per row's `blocker_gap` column → Wave 72b scope OR new gap
- 3 user-action items pending: GAP-525 credential rotation × 3 (admin password / Cloudflare token / Resend key)

**Wave 72a deferrals tracked inline in gap files** (per `gap-done-discipline.md` §3 PARTIAL exit ramp): GAP-514 live 429 smoke + `/api/auth/password-reset-request` route; GAP-515 FE Retry-After UX; GAP-518/519 live admin walkthrough; GAP-520 first real AWS Secret rotation; GAP-521 other admin controllers annotation + FE review page; GAP-525 user-action × 3.

**Wave 71b SHIPPED 2026-05-13** — Gateway routing scope extension (PR #1276 + tag `v0.9.0-beta-staging.13` deployed). GAP-512 → 🟢 DONE (3-path live verify: admin/beta-requests 401, consent/record 400, branding/slug-availability 200). GAP-513 Resend → 🟢 DONE (AWS Secret populated, SSM verify RESEND_API_KEY length=35 prefix=re_ho in kitehub-email container).

**Plan 1 BETA — infrastructure ready cho Bước 3-7:**

1. **Bước 3 verify-email** — gateway routes ✓, email send ✓
2. **Bước 4 admin approve/reject** — `/api/v1/admin/beta-requests/**` reaches subscription ✓
3. **Bước 5 email send** — RESEND_API_KEY present ✓ (END-TO-END SMOKE PENDING: register beta → check inbox)
4. **Bước 6 tenant onboarding** — branding routes ✓
5. **Bước 7 dashboard** — consent + notification routes ✓

**Next session signpost:**

- User runs Phase 1 BETA acceptance self-test ([`documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`](../../05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv) — 126 rows, Vietnamese narrative + UTF-8 BOM, CSV format with pre-filled `input_data`; Plan 1 archived to `documents/07-archived/planning-2026/plan-1-self-test-e2e-superseded.md` 2026-05-14 per Wave 72b Bucket G)
- File any bugs surfaced → Wave 72b+
- **GAP-506 Phase 1** (P1 — deploy-prod tech debt: chicken-and-egg fix + start_period bump) — defer to next maintenance wave
- **Post-release downsize evaluation** (≥4 weeks): if avg MemoryUtilization <60% + zero OOM → t3.large → t3.medium

---

### 🚀 Next Action (Wave 70 — GAP-502 P0 fix, blocks Plan 1) [HISTORICAL — Wave 70 SHIPPED]

**Wave 69 main scope** (re-rescoped — tooling delivery, user self-execute Plan 1 lần đầu):

GAP-372 (invite mechanism) marked DONE per checkbox nhưng **chưa từng test thật end-to-end trên production**. User chốt self-test 100% bằng tay lần đầu (có guide chi tiết) + ship Playwright scaffold cho lần re-test sau.

**Wave 69 deliverables (tooling, không phải execution):**
1. **Follow-along guide** — [`plan-1-self-test-e2e-superseded.md`](../../07-archived/planning-2026/plan-1-self-test-e2e-superseded.md) (archived 2026-05-14 per Wave 72b Bucket G; superseded by acceptance test CSV) — originally enhanced với evidence log template + helper commands + setup checklist
2. **Playwright scaffold** — `kitehub/kitehub-frontend/e2e/production-self-test/full-flow.spec.ts` (7 tests, `.skip`-ed by default; selectors cần calibrate sau khi user manual-run lần đầu); `playwright.config.ts` `testIgnore` để CI không pick up; README hướng dẫn run on-demand

**Post-Wave-69 (user manual execute):**
- User chạy Plan 1 theo guide → 7 bước có kết quả pass/fail
- Bugs found → file gap mới, fix trong wave tiếp
- Sau khi flow stable → calibrate selectors trong `full-flow.spec.ts`, opt-in CI (hoặc giữ on-demand)

**Path to invite — 2 plans + N waves remaining:**
- ✅ Wave 67: Production seed
- ✅ Wave 68: Verification + kc_app drift fix
- ⏳ **Wave 69:** Plan 1 self-test E2E (verify code path)
- ⏳ Wave 70+: Plan 2 (real cohort outreach + first invites) — chỉ execute sau khi Plan 1 SHIPPED
- ⏳ Wave 70+: Plan 3 (rollback drill quarterly execute) — parallel với Plan 2

**Deferred (out of Wave 69 scope):**
- Rollback drill quarterly execute → Plan 3 (separate wave)
- First beta invite real cohort → Plan 2 (gated on Plan 1 SHIPPED)
- GAP-370 SES re-submit → parallel passive action (không block Wave 69)

**Parallel passive checks:**
- GAP-412 AWS Activate — rejected ×2 per GAP-497, deprioritized
- GAP-447 kc_app sizing — DONE per CSV (right-sizing complete)

### ✅ 2026-05-13 SHIPPED (post-Wave-66 follow-through)

### ✅ 2026-05-13 SHIPPED (post-Wave-66 follow-through)

- ✅ **PR #1233** — Disable cost-saving EventBridge scheduler (`enable_cost_scheduling=false` default; resolves manual stop vs auto-restart conflict)
- ✅ **PR #1234** — IAM apply role: +`iam:ListInstanceProfilesForRole` +`iam:ListRoleTags` +`iam:ListOpenIDConnectProviderTags` (terraform AWS provider IAM Read coverage)
- ✅ **PR #1235** — ECR lifecycle preserve version tags + SSM poll early-exit on FAIL log markers + docker-build-push sha-conditional on tag events (3 sister bugs)
- ✅ **GAP-482 DONE** — Deploy E2E verified via run 25776387051 on `v0.9.0-beta-staging.11`: ALB 200, target healthy, 5 containers Started
- 🆕 **GAP-498 filed** — Deploy workflow poll redesign (ALB target health vs SSM Status field) — P2 non-blocking, deploy IS functional; pivot per `release-fix-retry-budget.md` §3
- ✅ **PR #1237** — GAP-498 Path B (ALB target health + smoke 200 replaces SSM Status poll); IAM `AlbTargetHealthForDeployPoll` Sid added
- ✅ **PR #1238** — Fix smoke retry #1: HTTPS api.kitehub.me + `-L` (ALB:80 redirect to HTTPS caused 301 vs 200 mismatch)
- ✅ **GAP-498 DONE** — E2E verified run 25777744962 ~3.5min wall-clock (vs old 8min false-timeout). All AC items checked.

### ✅ Wave 67 SHIPPED 2026-05-13 (via Path B direct SQL seed)

- ✅ **GAP-376 DONE** — Production data seed E2E (admin@kitehub.me PLATFORM_ADMIN in DB)
- ✅ **GAP-499 DONE** — Wave 67 prerequisites complete (CI conversion + secret provisioning)
- ✅ **GAP-500 DONE** — Path B direct SQL seed (bypassed Spring runner per retry budget pivot)
- ✅ Seed run 25782010115 wall-clock **52s** (vs prior 180s timeouts of Spring runner approach)
- ✅ 7 PRs shipped: #1240/1241/1242/1243/1244/1245 (prep+retry fixes) + #1247 (Path B)

### ✅ Wave 66 SHIPPED (5 PRs: plan + 3 buckets + 1 vendor follow-up) — historical

### ✅ Wave 66 SHIPPED (5 PRs: plan + 3 buckets + 1 vendor follow-up)

- ✅ **Plan** (#1224) — Wave 66 plan v2 with state-check evidence corrections (audit-to-gap-pipeline.md §2.6 hardened)
- ✅ **Bucket 0** (#1225) — GAP-494 Lighthouse CI cache fix (removed setup-node built-in cache:pnpm, added explicit actions/cache@v5; Lighthouse PASS verified)
- ✅ **Bucket A** (#1227) — GAP-493 Path B preflight job + iam.tf RdsDescribeForPreflight Sid (audit artifact 2026-05-12-gap-493-path-b-preflight.md)
- ✅ **Bucket Z** (#1226) — 5-gap docs sweep: GAP-369/398/399 DONE 100%; GAP-447 PARTIAL 75%; GAP-482 PARTIAL 95% (state-check evidence cited each Log)
- ⚠️ **GAP-495** (#1228) — Vercel rate-limit noise mitigation Phase 1 (silent:true + autoAlias:false). Phase 2/3 user-action.

**Post-Wave-66 user actions (gated on user-triggered workflows per `release-deploy-standard.md` §9):**
1. `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` → land IAM `rds:DescribeDBInstances` extension
2. `gh workflow run deploy-production.yml -f dry_run=false` → verify preflight job + flip GAP-482 DONE
3. Vercel Dashboard Settings → Notifications → disable "Failing deploys" GitHub status (per GAP-495 Phase 2)

### ✅ Wave 65 SHIPPED (8 PRs, 5 DONE + 1 PARTIAL + 1 incident-rules + 1 closure)

- ✅ **Bucket A1** (#1206) — GAP-487 MEMORY orphans state-corrected (0 orphans on disk-vs-index)
- ✅ **Bucket A2** (#1207) — GAP-488 wave-history.jsonl 64 orphan entries backfilled
- ✅ **Bucket B** (#1210) — GAP-486 `post-merge-sync-completeness.md` v1.0.0 + Rule 17 detector + 3 fixtures + PR template
- 🟡 **Bucket C** (#1211) — GAP-485 PARTIAL 55% (`meta-csv-index-pattern.md` v1.0.0 + ADRs CSV 28 rows + Rules CSV 36 rows + 2 query helpers + 2 validators + CI job); Tier 3 skills+audits → GAP-490
- ✅ **Bucket D** (#1209) — GAP-484 OTel autoconfig fix (7 application.yml services); production deploy verification deferred per `release-deploy-standard.md` §9
- ✅ **Bucket E** (#1208) — GAP-483 EC2 user_data terraform applied 2026-05-12 07:52 UTC (in-place update 2 EC2)
- ✅ **Incident rules** (#1212) — 2026-05-12 concurrent ops + visibility-gap incident → `concurrent-production-mutation-ops.md` v1.0.0 + `release-fix-retry-budget.md` v1.1.0 + GAP-491 follow-up + audit artifact extension

### 🚀 Next Action (signpost cho new session — Wave 66)

**✅ GAP-491 SHIPPED 2026-05-12** (PR #1216 + #1217 + #1218):
- CloudWatch log group `/aws/ssm/kite-deploy` live (retention 7d)
- `deploy-production.yml` adds `--cloud-watch-output-config` + interleave `filter-log-events` poll
- IAM `logs:FilterLogEvents` scoped to log group on `github_deploy` OIDC role
- **Verified live** on deploy run 25748003956: workflow log shows `│ <stdout>` interleaved with `Status=InProgress` polls

**✅ GAP-493 Path A SHIPPED 2026-05-12** — Phase 1 BETA infra alive:
- Root cause 1: RDS stopped (cost-saving scheduler) → started via `aws rds start-db-instance`
- Root cause 2: Flyway V34 checksum mismatch (DB vs code) → `DROP SCHEMA public CASCADE; CREATE SCHEMA public` (pre-launch, no real data)
- Restarted kitehub-admin/branding/email/subscription via docker compose restart
- **Verified:** ALB target healthy, `curl https://api.kitehub.me/actuator/health` = HTTP 200 ✅

**✅ GAP-493 Path B SHIPPED 2026-05-12** — Wave 66 Bucket A:
- `deploy-production.yml` `preflight` job added between validate + deploy — runs `aws rds describe-db-instances --db-instance-identifier kitehub-postgres`, fails fast (<30s) with `::error::` referencing `scripts/start-stack.sh` if status ≠ `available`
- `infrastructure/terraform-aws/iam.tf` `github_deploy_inline` extended with `RdsDescribeForPreflight` Sid (`rds:DescribeDBInstances`, Resource=`*` — least-privilege via action-only since RDS Describe doesn't support tag Condition)
- Pre-mutation audit: `documents/04-quality/audits/aws-verification/2026-05-12-gap-493-path-b-preflight.md`
- **Apply pending user-triggered** `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` post-merge per `release-deploy-standard.md` §9

**V34 file audit (follow-up):** revert if accidental edit, or renumber as V36 if intentional schema change. Tracked separately.

**Wave 66 SPAWNED 2026-05-12** (PR #1224 plan merged — 2 bg-agents + 1 coordinator):

- **Bucket 0** — GAP-494 Lighthouse CI cache fix (bg-agent, Opus medium)
- **Bucket A** — GAP-493 Path B deploy preflight + IAM rds:DescribeDBInstances (bg-agent, Opus 4.7)
- **Bucket Z** — Coordinator docs-flip sweep: GAP-369 ✅ DONE, GAP-398 ✅ DONE, GAP-399 ✅ DONE, GAP-447 🟡 75% (kc_app drift GAP-450 + CWAgent user-action), GAP-482 🟡 95% (E2E gated on Bucket A merge)

**Post-Wave-66 actions:**
- ✅ Bucket A merged → trigger `terraform-apply.yml workflow_dispatch confirm=APPLY` to land IAM `RdsDescribeForPreflight` extension
- Trigger `deploy-production.yml workflow_dispatch dry_run=false` → verifies preflight job + flips GAP-482 to DONE (Bucket Z marked 95%)
- Wave 67 picks up: GAP-376 prod data seed + GAP-412 AWS Activate D+14 cutover

**Parallel work (AWS-side wait — no user action needed):**
- **GAP-370** — SES production access form SUBMITTED 2026-05-12. Waiting AWS reply 24-48h. Next session check: AWS Console → SES → Account dashboard → sandbox status.
- **GAP-412** — AWS Activate Founders Pack application resubmit ($1k credit gated on `kitehub.me` accessibility — now satisfied; check status).

**Session work (Wave 66 buckets — no user action):**
- **GAP-376** — Production data seed (admin user + system config) → run `scripts/seed-production.sh` once Wave 66 picks it up.

**Path to invite first 5 beta tenants — estimate 4 waves (~4 tuần)**

| Wave | Scope | Trigger to next |
|---|---|---|
| Wave 66 | GAP-493 B + 482 close + 447 verify + 369 Phase 2 + 398/399 docs | Infra polish done |
| Wave 67 | GAP-376 seed + GAP-491 dashboard polish + monitoring tune | Production-ready |
| Wave 68 | GAP-370 SES (post 24-48h AWS reply) + smoke E2E + audit /100 ≥80 baseline | Invite-ready (~~GAP-372~~ DONE Wave 45) |
| Wave 69 | Rollback drill (`smoke-rollback-cycle.sh --execute`) + final audit /100 ≥80 + pre-launch acceptance | First invite |

Per CLAUDE.md §CURRENT PHASE Phase 1 → Phase 2 trigger: audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần.

**Wait-state (out of session control):**
- GAP-370 SES — User submits form, AWS replies 24-48h
- GAP-412 AWS Activate — User submits, AWS replies ~weeks

**Follow-up gaps to file (Wave 66 Bucket 0 — meta cleanup):**
- ~~**GAP-494**~~ — ✅ DONE 2026-05-12 (Wave 66 Bucket 0) — Lighthouse CI fixed via explicit actions/cache@v5 pattern matching frontend-ci.yml

### 📜 Wave 64 cutover (2026-05-12) — historical reference

**Wave 64 cutover (2026-05-12) — 7 cascading bugs surfaced; retry budget exhausted; pivoted to session-end checkpoint per `release-fix-retry-budget.md` §3.**

### 🚀 Next Action — historical (Wave 64 — now superseded by Wave 65)

✅ **Cutover Done (shipped this session):**
- ACM cert imported (`arn:aws:acm:ap-southeast-1:906286017800:certificate/e0adcd76-9d72-4567-a32e-a62d7987ccb1`) — Cloudflare Origin CA-issued, hostnames `*.kitehub.me` + `kitehub.me`, expires 2041
- ALB HTTPS:443 listener live (terraform apply via workflow_dispatch)
- HTTP:80 redirects to HTTPS
- CF SSL mode `full strict` + Always HTTPS `on` (CF API)
- `api.kitehub.me` proxied=true through CF
- 6 DNS records added (SES verification TXT + 3 DKIM CNAMEs + DMARC TXT + SPF merge)
- Docker images pushed v0.9.0-beta-staging.9 (10 services via tag trigger)
- deploy-prod.sh executes successfully (containers start, DB connect via Hikari)
- Bug 1-4 fixed (PR #1199 + #1200): IAM tag mismatch, hardcoded EC2 ID, ec2:DescribeInstances perm, secret prefix mismatch

❌ **Blocking next session:**

| GAP | Title | Priority |
|-----|-------|---------|
| **GAP-484** | Java services OTel OTLP tracing autoconfig crash — 5 kitehub-* services fail Spring startup with `Invalid endpoint, must start with http:// or https://` (env var disable doesn't work; need code-level fix) | 🔴 P0 BLOCKING |
| **GAP-483** | EC2 user_data missing git + repo clone bootstrap — every EC2 replacement breaks (worked around via manual SSM this session) | 🔴 P0 BLOCKING |
| GAP-482 | ✅ FIXED PR #1199 + #1200 (4 of 6 bugs from cascade) | — |

### Concrete next-session pickup order

1. **Fix GAP-484 (OTel) FIRST** — pick Option A from gap file: add to `kitehub-*/src/main/resources/application.yml`:
   ```yaml
   management:
     otlp:
       tracing:
         endpoint: ${MANAGEMENT_OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}
   ```
   Tag new version `v0.9.0-beta-staging.10` → docker-build-push → re-trigger deploy-production.
2. **Fix GAP-483 (user_data)** — extend `infrastructure/terraform-aws/ec2.tf` `ec2_user_data` add `dnf install -y git` + `git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git /opt/kite-prod`. terraform apply (will replace 2 EC2 again — accept, no data to lose pre-launch).
3. **Verify health** — `curl https://api.kitehub.me/actuator/health` should return 200; ALB target `kh-backend` should be `healthy`.
4. **Submit SES production access form** (Console, paste template `email-ses-setup-runbook.md` §4.1.1) — can do parallel with steps 1-3 to start 24-48h timer.
5. **Production seed** — `bash scripts/seed-production.sh` (gated on Java services healthy).
6. **Wave 65 candidates** post-cutover: define beta invite flow (GAP-480), gateway routing audit (GAP-481), GAP-478 ESO v1 bump.

### State at session end (Cloudflare + AWS facts for fix-time state-check)

- `kitehub.me` zone ID: `bb54ef8f69b0ef03085ce8903d90a5a4`
- ACM cert ARN: `arn:aws:acm:ap-southeast-1:906286017800:certificate/e0adcd76-9d72-4567-a32e-a62d7987ccb1`
- ALB listeners: HTTP:80 (redirect→HTTPS), HTTPS:443 (ACM cert)
- EC2 instances (live): kh-backend `i-00505094277deda29` + kc-app `i-007b72fffc6dcad22` (manual SSM-bootstrapped this session — will need re-bootstrap if replaced again before GAP-483 fix)
- Containers running on kh-backend: 5 kitehub-* + rabbitmq + redis (Java services in crash-loop)
- CF API token stored at `~/.cf-token.env` (gitignored, perm 600); zone ID at `/tmp/cf-zone-id.txt`
- GitHub Variable `ALB_ACM_CERTIFICATE_ARN` set

### Cost note (session discovery)

AWS Billing dashboard cost summary $8.31 MTD = **gross usage**. Bills tab "Estimated grand total: $0.00" = **net payable** after Free Tier credits (12-month new account). User KHÔNG phải trả gì cho May 2026 nếu giữ trong Free Tier ceilings.

---

**Wave 64 SHIPPED 2026-05-11 — Cleanup cluster (3 buckets, 4 PR + closure handles 26 Dependabot alerts):**

- **Bucket B GAP-204** → PR #1193 (Dependabot AUTO) → 🟢 **next 15.5.15→15.5.18 MERGED**. Original agent died with uncommitted local; Dependabot AUTO covered same scope = used efficiently.
- **Bucket C GAP-476 + GAP-475 Sub-5** → PR #1195 (re-spawn) → 🟢 **GAP-476 DONE**: Spring Actuator Flyway expose + custom `FlywayEndpointAuthFilter` (gateway-trust pattern) + 6 unit tests PASS + smoke-test wired. GAP-475 75%→90% PARTIAL.
- **Bucket D GAP-465** → PR #1194 (coordinator salvage) → 🟢 **GAP-465 DONE**: 179 LOC audit report (2 charts YELLOW verdict) + GAP-478 P1 (ESO v1 bump) + GAP-479 P2 (Phase 2 EKS batch) filed.
- **Closure** → 26 NEW Dependabot alerts post-Bucket-B surfaced root-cause: stale per-FE pnpm-lock dupes (next@15.5.15) vs root pnpm-lock (15.5.18). Workspace mode → root canonical → delete per-FE locks + gitignore. Both FE builds PASS.

**Stats Wave 64:**
- **3 gap DONE** (GAP-465, GAP-476, GAP-204 75% PARTIAL from 50%)
- **2 new gap filed** (GAP-478 P1, GAP-479 P2)
- **Wall-clock:** ~40 min coordinator (recovery overhead from session-interrupt)
- **Speedup:** ~4.5× vs 3h serial (recovery cost reduces normal ~30× speedup)
- **Streak:** 98 consecutive 0-clarification waves
- **Recovery discipline:** Session-interrupt protocol applied — triaged 3 agent states (dead/salvage/Dependabot-covered) without redundant re-runs

**User-action gates remaining (Phase 1 BETA pre-launch):**
1. Wave 61 cutover gates (ACM cert + ALB HTTPS:443 + SES domain verify + SES production form + tier-3 workflow + resume stack + final smoke)
2. Wave 63 rollback enablement (`terraform apply` IAM role + GitHub Environment `production` config + first `bash scripts/smoke-rollback-cycle.sh --execute` for TTR baseline)

**Wave 65 candidates** (post-Wave 64):
- (a) **User-action gates execution** → invite first beta tenant (Wave 61 + 63 user-actions)
- (b) GAP-478 ESO v1 bump (P1, ~30min mechanical)
- (c) GAP-479 Phase 2 EKS batch (deferred Phase 2)
- (d) GAP-475 final DONE (gated user-action TTR baseline)

---

**Wave 63 SHIPPED 2026-05-11 — Rollback workflow (GAP-477 P1, 3 buckets, 4 PR gồm plan):**

- **Bucket A GAP-477 IAM+workflow** → PR #1188 → 🟢 **shipped**: terraform IAM `kitehub-rollback-role` (least-priv ECR+SSM+EC2 describe+CloudWatch) + `.github/workflows/rollback.yml` 3-job (validate/rollback/notify). Stack EC2+SSM (state-check confirmed zero ECS). +383 LOC. terraform validate + actionlint clean.
- **Bucket B GAP-477 script wire-up** → PR #1189 → 🟢 **shipped**: `smoke-rollback-cycle.sh` `trigger_rollback()` calls real `gh workflow run rollback.yml`; JSON adds `workflow_run_id` + `restore_workflow_run_id`. +21 net LOC. Shellcheck clean.
- **Bucket C GAP-477 docs** → PR #1190 → 🟢 **shipped**: `incident-response-runbook.md` §8 rewrite + §8.1-8.4 (invocation/cadence/TTR/troubleshooting). `release-deploy-standard.md` v1.0.1 → v1.1.0 (new §4.4 rollback execution + §9 matrix row). +67 net LOC. Markdown clean.
- **GAP-477 status:** OPEN → PARTIAL 85% (all agent-deliverable AC checked; user-action remaining: terraform apply + GitHub Environment config + first `--execute` for TTR baseline).

**Stats Wave 63:**
- **0 gap DONE** (GAP-477 PARTIAL 85% — user-action gate clean)
- **Wall-clock:** ~4 min coordinator (3 Opus-full agents parallel)
- **Speedup:** ~30× vs 2h serial estimate
- **Streak:** 97 consecutive 0-clarification waves
- **release-deploy-standard.md** bumped v1.0.1 → v1.1.0 paired same-wave (rule-change-process §6.5)

**User-action gates remaining (Phase 1 BETA pre-launch):**
1. Wave 61 cutover gates (ACM cert import via `aws acm import-certificate` + ALB HTTPS:443 listener + SES domain verify + SES production access form + tier-3-cutover workflow_dispatch + resume stack + final smoke)
2. **Wave 63 rollback enablement** (NEW): `terraform apply` rollback IAM role + GitHub Environment `production` config + first `bash scripts/smoke-rollback-cycle.sh --execute` for TTR baseline

**Wave 64 candidates** (post-Wave 63):
- (a) User finalize Wave 61 + Wave 63 user-action gates → invite first beta tenant
- (b) GAP-476 Flyway migration HTTP endpoint (P2, Spring Actuator Flyway) → unblocks GAP-475 Sub-5 DONE
- (c) Helm + k8s staleness audit (GAP-465)
- (d) GAP-475 PARTIAL → DONE final flip (gated on user-action TTR baseline + GAP-476 endpoint)

---

**Wave 62 SHIPPED 2026-05-11 — Smoke test coverage extensions (GAP-475, 3 buckets, 4 PR gồm plan):**

- **Bucket A GAP-475 Sub-1+4+5** → PR #1183 → 🟡 PARTIAL: `smoke-test.sh` +294 LOC; auth happy-path env-gated, latency report 10 endpoints, Flyway head graceful SKIP. Sub-5 deferred → **GAP-476 P2** filed (no HTTP migration endpoint in codebase).
- **Bucket B GAP-475 Sub-2+3** → PR #1184 → 🟢 **2 sub DONE**: `smoke-ses.sh` +229 LOC + runbook §6.1 +97 LOC; Mailgun primary + IMAP fallback; MFA adapted to link-based `?token=<UUID>` (BE source-of-truth per `audit-to-gap-pipeline.md` §2.5).
- **Bucket C GAP-475 Sub-6** → PR #1185 → 🟡 PARTIAL: `smoke-rollback-cycle.sh` +291 LOC + `incident-response-runbook.md` §8; default `--dry-run`; `rollback.yml` absent → **GAP-477 P1** filed (workflow_dispatch rollback workflow).
- **GAP-475 status:** OPEN → PARTIAL 75% (4/6 sub functional; 2 deferred to GAP-476 + GAP-477).

**Stats Wave 62:**
- **0 gap DONE** (PARTIAL exit-ramp clean) · **2 sub DONE within GAP-475** (Sub-2 + Sub-3) · **2 new gap filed** (GAP-476 + GAP-477)
- **Wall-clock:** ~4 phút coordinator (3 agents parallel, max ~4 min Bucket C)
- **Speedup:** ~45× vs 3h serial estimate
- **Streak:** 96 consecutive 0-clarification waves

**Wave 63 candidates** (post-Wave 62):
- (a) **GAP-477 rollback workflow** (P1) — workflow_dispatch + IAM role + dry-run validate → unblocks Sub-6 final DONE + production rollback readiness
- (b) **GAP-476 migration endpoint** (P2) — Spring Boot Actuator Flyway expose + admin-auth → unblocks Sub-5 final DONE
- (c) User finalize cutover gates per Wave 61 §🚀 (ACM + ALB HTTPS:443 + SES production approval) → invite first beta tenant
- (d) Helm + k8s staleness audit (GAP-465)

---

**Wave 61 SHIPPED 2026-05-11 — Stop-when-idle cutover (5 buckets, 6 PR gồm plan + closure):**

- **Bucket A GAP-369 + GAP-449** → PR #1175 → 🟡 PARTIAL: State-check phát hiện DNS đã LIVE từ Tier 2 (PR #1085) — Bucket A pivot sang audit artifact `2026-05-11-wave-61-bucket-a-dns-state.md`. SSL Full strict + Always HTTPS blocked bởi ACM empty (Origin Cert chưa import) + ALB HTTPS:443 listener missing + stack STOPPED. GAP-369 50→70%, GAP-449 OPEN→PARTIAL 30%.
- **Bucket B GAP-370 SES production** → PR #1173 → 🟡 PARTIAL 75%: `scripts/smoke-ses.sh` 197 LOC Tier 1 verify; runbook +170/-12 với production form template + 3 AWS-rejection reply templates + post-approval verify. User-action: submit form + 24-48h approval.
- **Bucket C GAP-376 + GAP-449** → PR #1174 → 🟡 PARTIAL 80%: State-check phát hiện seed runner đã ship Wave 33 PR #895 → Bucket C focus operational. `production-seed-runbook.md` ~210 LOC; `smoke-test.sh` thêm `STOP_WHEN_IDLE_E2E=1` scenario. User-action: real production seed = first cutover step.
- **Bucket D GAP-473 (mới)** → PR #1176 → 🟡 PARTIAL 40%: `start-stack.sh` + `stop-stack.sh` (249/248 LOC dry-run exit 0); `stack-on-demand-runbook.md` 11 sections gồm EventBridge Lambda template deferred Phase 1.5. Per `agent-aws-access.md` §4: ship scripts, user executes.
- **Bucket E GAP-470 + GAP-471 + GAP-472** → PR #1177 → 🟢 **3 gap DONE**: K8s `runAsNonRoot` + Vercel headers CORS tight + Gateway `SecurityHeadersFilter` parity (kitehub-gateway tạo mới, kiteclass-gateway extend HSTS+CSP). All 9 Docker builds + BE/FE tests + Lighthouse PASS. Mozilla Observatory target ≥B post-cutover.

**Stats Wave 61:**
- **3 gap DONE** (GAP-470 + GAP-471 + GAP-472)
- **5 gap PARTIAL** với user-action gates documented (GAP-369/370/376/449/473)
- **1 new gap** (GAP-473 auto start/stop)
- **Wall-clock:** ~45 phút coordinator (5 agent parallel) · **Speedup:** ~160× vs 5-7 ngày
- **Streak:** 95 consecutive 0-clarification waves
- **Cost validated:** Stack stays STOPPED Wave 61 design; ~$3-5/mo storage only

**User-action gates còn lại để complete cutover** (CLI-first per `agent-action-bias.md` §1 Part B; Console chỉ khi không có CLI path):
1. Import Cloudflare Origin Cert vào AWS ACM ap-southeast-1 — `aws acm import-certificate --region ap-southeast-1 --certificate fileb://cert.pem --private-key fileb://key.pem --certificate-chain fileb://chain.pem`
2. Add ALB HTTPS:443 listener (qua workflow_dispatch terraform-apply.yml)
3. SES verify domain `kitehub.me` + DKIM — `aws ses verify-domain-identity --region ap-southeast-1 --domain kitehub.me` + `aws ses verify-domain-dkim --region ap-southeast-1 --domain kitehub.me`; thêm DKIM CNAME + SPF/DMARC TXT vào Cloudflare DNS qua `wrangler` (xem `email-ses-setup-runbook.md` §3.2 cho payload)
4. Submit SES production access form (paste template `email-ses-setup-runbook.md` §4.1.1) — **Console-only** (AWS không có API submit support case này) → `agent-action-bias.md` §3 exception row 1
5. Run Path Y `tier-3-cutover.yml` workflow_dispatch + confirm "APPLY" — `gh workflow run tier-3-cutover.yml -f confirm=APPLY`
6. Resume stack (`bash scripts/aws/start-stack.sh`) → run `seed-production.sh` → smoke
7. Wait 24-48h SES approval → final smoke (`bash scripts/smoke-ses.sh`)

**Phase 1 BETA critical-path tracker (post Wave 61):**

| Step | Status |
|------|:------:|
| 1. Phase 4 milestone audit | ✅ DONE (Wave 53+54) |
| 2. Production observability validation | 🟡 PARTIAL (Wave 55) |
| 2.5 Multi-tenant RLS + perf methodology | 🟡 PARTIAL (Wave 56+57) |
| 2.6 Pre-cutover P0 hardening | ✅ DONE (Wave 60) |
| **2.7** Stop-when-idle cutover artifacts | ✅ **DONE Wave 61** (DNS verified + SES prep + seed runbook + start/stop automation + security headers P0) |
| 3. AWS funding decision | 🟡 RESUBMITTED 2026-05-11; pending D+14 (2026-05-25) — DECOUPLED Wave 61 stop-when-idle |
| **4. Tier 3 cutover execution** | ⏳ user-action gates (ACM import + ALB HTTPS:443 + workflow_dispatch APPLY + smoke) |
| 5. Beta tenant onboarding (4-6 week beta period) | ⏳ gated step 4 user-action |

**Wave 62 candidates:**
- (a) **User finalize cutover gates** → invite first beta tenant (recommend post user-action 7 items above)
- (b) Helm + k8s artifacts staleness audit (GAP-465, ~1 ngày)
- (c) ADR README drift sweep + 2 residual HIGH CVE post-Wave 57 (deferred Wave 58)
- (d) Lambda scheduler GAP-473 Phase 2 (deferred Phase 1.5 unless beta volume justifies)

---

**Wave 60 SHIPPED 2026-05-11 — Pre-cutover P0 hardening (4 buckets, 4 PR):**

- **Bucket A GAP-406 OWASP Top 10 self-audit** → PR #1168 → 🟢 **DONE**: 76/100 C+, 0 P0 / 3 P1 / 2 P2 / 1 P3 findings; live header probe HSTS PASS + 4/5 missing → 3 follow-up gaps (GAP-470 K8s `runAsNonRoot` / GAP-471 Vercel FE headers + CORS / GAP-472 Gateway `SecurityHeadersFilter` parity); SQL/JWT/AES/SSRF clean. 3 P1 follow-up sẽ promote → P0 cho v1.0.0 PRODUCTION cutover gate.
- **Bucket B GAP-137 FE bulk-import** → PR #1169 → 🟢 **DONE**: `/admin/bulk-import` page + 7 component tests (full suite 728 pass/0 fail) + MSW handler + students entry-point. Format thực tế = XLSX (Apache POI BE) chứ không phải CSV như plan ghi — agent state-check catch. Admin-merge per `admin-merge-discipline.md` §2 (Vercel rate-limited environmental; Frontend Tests + E2E + Build CI green).
- **Bucket C GAP-430 + GAP-114** → PR #1167 → 🟢 **DONE** (cả 2): Fix-time state-check phát hiện metric đã đổi Wave 41 PR #983 (`kite_backup_last_success_timestamp_seconds` + `absent()` arm) → Option B scope-cut (drop AC + §Out-of-scope cite GAP-435 promtool fixture); GAP-114 Wave 25 logging infra verified, live 3-service smoke → GAP-115 deploy verification. CSV validator PASS 289 rows.
- **Bucket D-2 GAP-* PR# backfill (6 candidates)** → PR #1170 (docs-only) → 2 DONE / 4 PARTIAL: GAP-050 + GAP-116-pii flip DONE Option B; GAP-033 + GAP-049 + GAP-102 + GAP-112 stay PARTIAL với follow-up cited (Wave 6 deps / GAP-156 / GAP-111 Phase 2). 10 PR refs backfilled. (Bucket D Explore audit ban đầu refuse 0/6 flips vì cả 6 fail Criterion 4 PR# missing → Bucket D-2 spawn Option 1 chuyển sang backfill.)

**Stats Wave 60:**
- **Gap DONE: 6** (GAP-406 + GAP-430 + GAP-114 + GAP-137 + GAP-050 + GAP-116-pii)
- **Gap PARTIAL unchanged: 4** (GAP-033/049/102/112 — đúng kỷ luật + follow-up)
- **New gap filed: 3** (GAP-470/471/472 — security headers P1 → P0 v1.0.0)
- **Wall-clock:** ~30 phút coordinator (plan-spawn-merge same session)
- **Speedup:** ~144× vs 3-5 ngày plan estimate
- **Streak:** 94 consecutive 0-clarification waves
- **Merge overrides:** #1169 + #1170 admin-merge per Vercel rate-limit environmental (all real CI green)

**Phase 1 BETA critical-path tracker (post Wave 60):**

| Step | Status |
|------|:------:|
| 1. Phase 4 milestone audit | ✅ DONE (Wave 53+54) |
| 2. Production observability validation | 🟡 PARTIAL (Wave 55; live-cluster gated step 4) |
| **2.5** Multi-tenant RLS + perf methodology | 🟡 PARTIAL (Wave 56+57; staging exec gated step 4) |
| **2.6** Pre-cutover P0 hardening | ✅ **DONE Wave 60** (OWASP audit + backup metric verify + FE bulk-import + 6 FLIP-DONE sweep) |
| 3. AWS funding decision | 🟡 RESUBMITTED 2026-05-11; pending D+14 (2026-05-25 reminder set) |
| 4. Tier 3 cutover (api.kitehub.me HTTPS) | ⏳ runbook ready GAP-449; gated step 3 funding decision |
| 5. Beta tenant onboarding (4-6 week beta period) | ⏳ gated step 4 + RLS hardening |

**Wave 61 candidates:**
- (a) AWS Activate D+14 cutover execution (gated 2026-05-23 approval)
- (b) Helm + k8s artifacts validation (GAP-465 staleness audit pre-Phase-1.5)
- (c) ADR README index drift sweep + 2 residual HIGH CVE post-Wave 57 (deferred Wave 58)
- (d) Beta tenant invite mechanism + signup smoke (gated step 3)

**Cleanup:** All 4 Wave 60 PRs merged; worktree husks pruned (`prune-merged-worktrees.sh --yes` clean); local main synced với origin/main.

---

**Session 2026-05-11 earlier — Gap Architecture v2 hardened + Legal scope cut (5 PRs):**

📌 **PICK-UP SIGNAL CHO SESSION MỚI:**

**A. Hạ tầng quản lý gap (gap-architecture-v2.md v1.0.0 → v1.0.3):**
- **#1161** Phase 2 bulk migration — `gap-status.csv` 5→289 rows (100% coverage active gaps)
- **#1162** Phase 4 `collect-state.sh` query CSV + surface "Phase 1 BETA P0 count" mỗi session start
- **#1163** Phase 2.1 auto-fill (27 reclassifications) + audit-pipeline §2.8 step 0 CSV query + wave-plan template gap-ref convention
- **#1164** 3-agent staleness audit của 88 gap → 44 CSV updates (34 phase reclass + 3 WONTFIX + 7 completion refine)
- **#1165** Legal scope-cut — 29 gap moved to `pending/` folder (PDPL + Luật Trẻ em + Giáo dục + Tax + Lao động)

**B. CSV distribution sau session:**
| Status | Count |
|---|---|
| OPEN | 166 |
| PARTIAL | 86 |
| **PENDING** (legal defer) | **29** |
| IN_PROGRESS | 3 |
| WONTFIX | 3 |
| PLANNED | 2 |
| **Tổng** | **289** |

**C. Quyết định lớn — Legal defer (2026-05-11):**

Solo-dev chốt: KiteClass **không làm compliance pháp lý toàn diện** trong Phase 1 BETA + Phase 1.5 PAID. 29 gap legal → `documents/04-quality/gaps/pending/` (xem `pending/README.md`). Hệ quả:
- PDPL hạn 2026-07-01 **chấp nhận risk** — beta cohort được brief "v1 pending counsel review"
- Không onboard tenant K-12 trong Phase 1 BETA + 1.5 (gating)
- Re-eval triggers: legal counsel engaged / first commercial tenant / first K-12 request / regulator inquiry / Phase 2 ramp / Phase 3 K-12

**D. Phase 1 BETA critical path còn lại (post-legal-defer):**

| Cluster | Gap | Trạng thái | Wave đề xuất |
|---|---|---|---|
| **Cutover AWS Activate D+14** | GAP-369 DNS, GAP-370 SES, GAP-376 seed, GAP-398 docker, GAP-399 ECR, GAP-412 Activate, GAP-447 EC2 right-size | 7 P0 PARTIAL chờ AWS Activate ~2026-05-23 | **Wave 61** |
| **P0 hardening pre-cutover** | GAP-406 pen-test OWASP, GAP-430 backup metric, GAP-114 logging verify (90%) | 3 P0 ship được ngay | **Wave 60** |
| **FE UX P0** | GAP-137 bulk import frontend UI | 1 P0 — 1-2 ngày FE work | **Wave 60** |
| **AI MVP** | GAP-005 AI queue (Phase 1 MVP done, Phase 2 scale defer) | IN_PROGRESS 40% | Defer Phase 2 |
| **Beta tenant onboarding** | GAP-371 CDN, GAP-379 secrets, GAP-380 staging, GAP-432 findAll, GAP-466 RLS, GAP-374 release CI, GAP-440 Spring Boot bump | 14 P1 PARTIAL | **Wave 62-63** |

**E. Wave roadmap đến beta launch (4-6 tuần):**

1. **Wave 60 — Pre-cutover P0 hardening** (1 tuần)
   - GAP-430 backup alert metric fix
   - GAP-406 pen-test OWASP Top 10 (manual, self-audit)
   - GAP-114 logging 90% PARTIAL → DONE flip (per-gap §2 verify)
   - GAP-137 bulk import frontend UI
   - Cleanup: 6 FLIP-DONE candidates (GAP-033/049/050/102/112/116)

2. **Wave 61 — AWS Activate D+14 Production Cutover** (gated 2026-05-23, ~1 tuần)
   - Step 4 Tier 3 cutover: EC2/RDS resume + DNS bind + SES production approval
   - GAP-369 + GAP-376 + GAP-370 production verify
   - Post-deploy smoke + audit artifact

3. **Wave 62 — Beta tenant onboarding readiness** (2-3 tuần)
   - GAP-371 Cloudflare CDN
   - GAP-379 secrets management
   - GAP-380 staging parity
   - GAP-432 findAll pagination
   - GAP-466 RLS production hardening
   - Beta cohort recruitment + invite flow

4. **Wave 63 — Beta launch + stabilization** (2 tuần)
   - Onboard 5-10 beta tenants
   - 0 P0 incidents 2 tuần stabilization
   - Quality audit /100 ≥80 maintained (currently 87)
   - User feedback collection

**F. Streak + housekeeping:**
- Streak: 99 consecutive 0-clarification waves
- No agents in flight; 0 worktree husks; coordinator clean
- Local main synced với origin/main 2026-05-11 sau merge #1165

**G. Infra state (live snapshot — cost-save mode):**
- AWS account 906286017800 / ap-southeast-1
- EC2 `kitehub-kh-backend` + `kitehub-kc-app`: STOPPED
- RDS `kitehub-postgres`: STOPPED
- ALB `kitehub-alb`: ACTIVE
- CloudTrail `kitehub-main`: IsLogging:True
- 0 alarms ALARM
- Resume khi: beta onboard / smoke test pre-launch / Wave 61 cutover

**Session work summary (2026-05-11 — Gap Architecture v2 + Legal defer):**

**5 PRs shipped (5/5 squash-merge clean CI):**
- **#1161** `feat(gaps)(phase-2)`: bulk migrate 289 active gap files to gap-status.csv
- **#1162** `feat(start-session)`: query gap-status.csv for blockers + Phase 1 BETA P0 count
- **#1163** `feat(gaps)(phase-2.1+4)`: richer phase inference + audit-pipeline CSV note + wave template
- **#1164** `chore(gaps)(audit-2026-05-11)`: apply 44 CSV reclassifications from 3-agent staleness audit
- **#1165** `chore(gaps)(pending-legal)`: defer 29 legal/compliance gaps — solo-dev scope cut

**Wave 55 SHIPPED — Production Observability (6 PRs):**
- #1118 plan; #1119 A GAP-434 Loki/Promtail Phase 2 🟡 PARTIAL; #1125 B GAP-112 distributed tracing 7 modules 🟡 PARTIAL; #1120 C GAP-144 alertmanager mock-fire 🟡 PARTIAL; #1121 side-discoveries (GAP-467 + GAP-468 filed); #1129 closure. Wall-clock ~95min coordinator (26× speedup vs 5-7 day estimate). Streak 90.

**Wave 56 SHIPPED — Multi-tenant Postgres RLS Hardening (3 PRs):**
- #1130 plan; #1131 single-bucket atomic GAP-466 (1854/1854 tests PASS + 4/4 RLSEnforcementIT); #1132 closure. Wall-clock ~38min (200× speedup). Streak 91. Coordinator-applied GAP-ID rename fix (agent's GAP-467 perf baseline → GAP-469 to avoid collision with existing helm gap).

**Wave 57 plan drafted (PR #1133 OPEN):**
- 3 disjoint buckets (helm/maven/scripts+docs); est ~1 day with 3 parallel agents
- Bucket A GAP-467 P1 — extract Go-templates from values.yaml (~2h)
- Bucket B GAP-468 P1 — Spring Boot 3.5.14 → latest BOM (9 HIGH CVE fix; ~3-4h)
- Bucket C GAP-469 P2 — RLS perf baseline pgbench harness + methodology + runbook (full staging execution deferred; ~3-4h)

**New gaps filed this session (3):**
- **GAP-467** 🟠 P1 — Helm values.yaml Go-templates pre-existing PR #984 break (blocks `helm lint` CI)
- **GAP-468** 🟠 P1 — Spring Boot BOM bump fixes 9 HIGH CVE in built Docker jars
- **GAP-469** 🟡 P2 — RLS performance baseline measurement (deferred Phase 4 AC from GAP-466)

**Open Dependabot PRs (not session-blocking; review at next CVE triage):**
- #1122, #1123, #1124, #1126, #1127, #1128 — automated dep bumps; some likely subsumed by Wave 57 Bucket B BOM bump

**Stale local branches (defer destructive cleanup):**
- ~10 local branches from squash-merged Wave 47-53 (squash changes hash so `prune-merged-worktrees.sh --merged` doesn't catch). Run `git branch -D` after explicit confirm at next session.

---

**Previous session (post-Wave-54 cluster, 5 PRs):**
- **PR #1112** fix(it): unused imports removed AttendanceClassBatch + StudentPortal IT (`ADMIN_MERGE_OVERRIDE` for Vercel rate-limit environmental)
- **PR #1113** audit(aws): actual cost Apr-May 2026 — $0 MTD vs theoretical $144-216 estimate (Free Tier 12mo intact đến ~2027-05-07; account spend effectively $0 vì stack stopped post-Wave-50)
- **PR #1114** docs(aws-credit) v1.1: actual numbers — Phase 1.5 cost correction. Premise "Release 1 không phát sinh cost" CORRECTED: nếu Activate denied, realistic personal cash **$293 over 4.5 tháng** (Phase 1 BETA Architecture B ~$40/mo × 3mo + Phase 1.5 Architecture A ~$115/mo × 1.5mo; Free Tier KHÔNG cover t3.large)
- **PR #1115** docs(aws-activate): Founder $1k **RESUBMITTED 2026-05-11 01:19 ICT** sau GAP-459 root-cause fix shipped (PR #1086 SSR shell + canonical URL `.vn`→`.me`). Form values carried forward from 2026-05-09 submission; description text refreshed nhấn "Live at kitehub.me" (247 chars). GAP-459 🟡→🟢 DONE; GAP-412 🔴→🟡 PARTIAL pending approval D+7-10 BD (~2026-05-21). Calendar reminder D+14 (2026-05-25 10:00 ICT) set via Google Calendar MCP event `rk5m95s90qm1ulavk4qfhsui20`
- **PR #1116** docs(gaps): 4 infra gaps surfaced từ user 4-question deployment review:
  - **GAP-463** P2 — `infrastructure/README.md` sync với Phase 1 BETA reality (terraform-oracle archived, EKS hint misleading, helm/k8s dormant clarification)
  - **GAP-464** P2 — ECS Fargate vs EKS architecture decision (ADR-025 §5 follow-up commitment unfulfilled; trigger Phase 1 BETA closure)
  - **GAP-465** P2 — Helm + k8s artifacts validation pre-Phase-1.5-migration (GAP-415 sub-task; staleness audit + remediate + CI guard)
  - **GAP-466** 🟠 **P1** — Multi-tenant Postgres RLS defense-in-depth (PDPL 2023 Art 23 compliance; pre-Phase-1-BETA-launch hardening security-critical)

**Phase 1 BETA critical-path tracker (updated 2026-05-11):**

| Step | Status |
|------|:------:|
| 1. Phase 4 milestone audit | ✅ DONE (Wave 53+54: UI 111.7 + Quality 87 + Performance 81) |
| 2. Production observability validation | 🟡 PARTIAL (Wave 55 SHIPPED 2026-05-11; chart-level + foundation DONE per PRs #1119 Loki / #1125 tracing / #1120 alertmanager; live-cluster validation gated step 4 first deploy) |
| **2.5** Multi-tenant RLS hardening | 🟡 PARTIAL (Wave 56 RLS defense-in-depth PR #1131 + Wave 57 Bucket C perf methodology PR #1136; full staging perf execution deferred to step 4 cutover per `release-deploy-standard.md` §9) |
| 3. AWS funding decision | 🟡 RESUBMITTED 2026-05-11; pending D+14 (2026-05-25 reminder set) |
| 4. Tier 3 cutover (api.kitehub.me HTTPS) | ⏳ runbook ready GAP-449; gated step 3 funding decision |
| 5. Beta tenant onboarding (4-6 week beta period) | ⏳ gated step 4 + RLS hardening |

**Wave 56 SHIPPED 2026-05-11 — Multi-tenant Postgres RLS Hardening (PRs #1130 plan + #1131 single-bucket + closure):**
- **Bucket A GAP-466 RLS defense-in-depth** → PR #1131 → 🟡 PARTIAL: 4-phase atomic ship.
  - **Phase 1 Migrations:** `V58__enable_rls_tenant_scoped_tables.sql` (51 kc-core tables RLS+FORCE) + `V34__enable_rls_tenant_scoped_tables.sql` (12 kh-subscription tables RLS, no FORCE — kh-sub lacks per-request `TenantContext`). `TenantAwareDataSourceInterceptor` aspect issues `set_config('app.current_tenant_id', :tid, true)` at every Spring `@Transactional` boundary; idempotent across nested propagation; default-deny when context empty.
  - **Phase 2 IT tests:** `RLSEnforcementIT` 4/4 PASS on TestContainers Postgres 15. Tests provision `kite_rls_test_role` (NOSUPERUSER + NOBYPASSRLS) + `SET LOCAL ROLE` per tx (Testcontainers `test` superuser would bypass FORCE).
  - **Phase 3 Docs+monitor:** `kiteclass-architecture.md` §Multi-Tenant Isolation rewritten layered defense; `runbooks/rls-policy-violation.md` P0 incident response; `prometheusrule.yaml` `RLSPolicyViolation` alert rate>0 fires P0; `multi-tenancy/rules.md` BR-MULTITENANT-001 5-attribute schema PDPL 2023 Art 23 compliance evidence anchored.
  - **Phase 4 Backwards-compat:** **1398/1398 kc-core PASS** (52 skipped, 0 fail/err) + **452/452 kh-subscription PASS** + 4/4 `RLSEnforcementIT` PASS. **Zero test breakage** despite 51-table FORCE RLS.
- **Side-discovery rename:** agent-filed `GAP-467 RLS perf baseline` collided with existing `GAP-467 helm values.yaml Go-templates` (merged PR #1121). Coordinator renamed → `GAP-469-rls-performance-baseline.md` + 4 in-text refs in GAP-466 fixed inline.
- **Risks materialized:** A test breakage ✅ MITIGATED zero break; B perf ⏳ DEFERRED → GAP-469 (sustained-load harness pending); C admin cross-tenant ✅ DOCUMENTED runbook §4 break-glass; D pool reuse ✅ MITIGATED `set_config(.., true)` + IT verifies; E batch jobs ✅ DOCUMENTED `TenantContext.runAs(...)` convention.
- **Wall-clock:** ~33 min agent + ~5 min coordinator rename = ~38 min vs ~5-6 day plan estimate (**~218× speedup**). Atomic single-bucket ship + perf-baseline-deferred-to-follow-up-gap pattern enabled.
- **Phase 1 BETA critical-path step 2.5** ⏳ → 🟡 PARTIAL (defense-in-depth shipped; perf gate deferred GAP-469).
- **Streak: 91** consecutive 0-clarification-flip waves.

**Wave 55 SHIPPED 2026-05-11 — Production Observability Validation (PRs #1118 plan + #1119 A Loki + #1125 B tracing + #1120 C alertmanager + #1121 side-discoveries + closure):**
- **Bucket A GAP-434 Loki/Promtail Phase 2** → PR #1119 → 🟡 PARTIAL: Helm subchart `grafana/loki-stack` 2.10.2 added; `loki:` block in values.yaml (single-binary + S3 90d + Promtail DaemonSet + JSON pipeline for tenantId/traceId/spanId/level/service); new `templates/grafana-datasource-loki.yaml` (uid `loki`); `scripts/smoke-test.sh` extended with `LOGS_OVERVIEW_E2E` (gated `SMOKE_LOGS_E2E=1`); 6/12 AC. Live `helm test` + LogQL query deferred to first deploy (no local k8s; matches GAP-144 mock-fire precedent).
- **Bucket B GAP-112 distributed tracing** → PR #1125 → 🟡 PARTIAL: 7 deployable modules (gateway + 6 KH services + kc-core/gateway) instrumented with `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` (Spring Boot 3.5 BOM-managed). `application.yml` env-driven `OTEL_SAMPLING_PROBABILITY=0.1` + `OTEL_EXPORTER_OTLP_ENDPOINT`. **RabbitMQ auto-instrumented** (no manual interceptor needed — Spring AMQP picks up W3C `traceparent` OOTB). 1 TracingConfigTest per service. All 7 modules `mvn verify -P strict-warnings` ✅. 3/5 AC; live Tempo backend + Grafana dashboard deferred to GAP-111 Phase 2.
- **Bucket C GAP-144 mock-fire backfill** → PR #1120 → stays 🟡 PARTIAL: ExternalSecret 3→5 keys (added `smtp-host` + `smtp-username`); `monitoring.alertmanager.smtp.smarthost` regional default extended; new `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` (285 lines — offline `amtool check-config` + online `amtool alert add` per-receiver + 4-section troubleshooting). Live-cluster delivery verification still gated platform deploy.
- **Side-discoveries** → PR #1121 → 2 follow-up gaps filed:
  - **GAP-467** 🟠 P1 — PR #984 embedded Go templates `{{- if ... }}` directly in `values.yaml` → breaks `helm lint`/`helm template` on main HEAD. Both Bucket A + C agents independently verified pre-existing via `git stash` baseline. Single fix (extract templated block to `templates/alertmanager-config.yaml`) unblocks all helm work.
  - **GAP-468** 🟠 P1 — 9 HIGH CVE in built Docker jars (6× netty 4.1.132 + postgres JDBC + bcprov + spring-cloud-gateway). Single Spring Boot BOM bump fixes most. **Sequencing:** MUST land after Bucket B merged (now satisfied; gap unblocked).
- **Risk A (`values.yaml` overlap A+C) materialized as expected; mitigation worked** — sequential rebase A→C produced clean merge (different YAML sub-trees: A added top-level `loki:`, C extended `monitoring.alertmanager.smtp` block).
- **Wall-clock:** ~95 min coordinator (3 agents parallel + sequential merge B→A→C) vs ~5-7 calendar days serial estimate. Wave-pack methodology continues 5-7× speedup.
- **Phase 1 BETA critical-path step 2** ⏳ → 🟡 PARTIAL (chart-level + foundation shipped; live-cluster validation gated step 4 first deploy via GAP-449 → step 3 AWS funding decision unblock).
- **Streak: 90** consecutive 0-clarification-flip waves.

**Wave 54 SHIPPED 2026-05-11 — Performance /100 redux + Observability state-check (PRs #1109 plan + #1110 Performance):**
- **Performance /100:** ✅ Bucket A PR #1110 — **81/100 B** (Δ +6 vs Wave 40 75/100). Zero P1 remaining (3 Wave 40 P1 unbounded `findAll` closed Wave 41 GAP-432). Wave 51 new endpoints fully compliant pagination. Zero new sub-gaps.
- **Observability state-check:** ✅ Bucket B Explore — Phase 1 BETA §3.6 deliverable verdict 🟡 **PARTIAL-VERIFIABLE**. Audit file `documents/04-quality/audits/ops-readiness/2026-05-11-wave-54-observability-state-check.md`. 4 follow-up gaps already filed (no duplicates):
  - **GAP-434** Loki/Promtail Phase 2 (~12-16h) — enables real log search
  - **GAP-112** Distributed tracing Micrometer + Tempo (~16-20h) — incident MTTR ~50% cut
  - **GAP-144** Alertmanager production receivers + PagerDuty (~6-8h) — on-call routing live
  - **GAP-257** Quarterly DR exercise + measured RTO/RPO (~8-12h; gated S3 backups 4+ weeks)
- **GAP-462** 🟡 PARTIAL → 🟢 **DONE** (3/3 audits complete; Phase 4 milestone obligation fully satisfied).
- **DOMAIN_MILESTONE_AUDIT trailer extended** với 3rd report Performance.
- **Phase 1 BETA critical-path step 1** ✅ **DONE.**
- **Phase 1 BETA critical-path step 2** ▶️ Wave 55+ scope crystallized: 3 parallel buckets (Loki + tracing + alert receivers) → full observability stack operationally validated by Wave 55 closure (~3 weeks → ~2026-06-01). GAP-257 deferred to Wave 56-57 (S3 backup accumulation gate).
- **Streak: 89** consecutive 0-clarification-flip waves.

**Wave 53 SHIPPED 2026-05-11 — Phase 4 milestone audit (PRs #1105 plan + #1106 UI + #1107 Quality):** 2/3 audits shipped (Performance C deferred Wave 54+ — agent hit usage limit pre-execution; reset 2026-05-11 02:50 Asia/Bangkok).
- **UI /128:** **111.7/128 A+** (+0.4 vs Wave 40 baseline 111.3). 4 kits DONE-eligible (kc-parent 114.4 / kc-student 116.4 / kh-admin 117.1 / ai-branding-wizard-v2 115.9 — all screens ≥105). 3 kits stay PARTIAL (kc-owner-pro / kc-teacher / kh-pro — `<105` screens carry-forward GAP-429 umbrella transient-state UX).
- **Quality /110:** **85/110 (87/100 / 80 tech-only) B+** (+1 vs Wave 40 86). +7 buffer above Phase 1 BETA threshold 80. Cat 4 FE Tests +2 (Wave 51 specs 209 component + 28 E2E); Cat 8 Docs +1 (audit-to-gap-pipeline §2.7 + student-portal/ 3-layer). Cat 11 placeholder 5/10 (GAP-152 pending).
- **Performance /100:** ❌ deferred Wave 54+. Wave 40 baseline 75/100 stable; risk LOW.
- **GAP-462 status:** OPEN → 🟡 PARTIAL pending Performance re-spawn.
- **DOMAIN_MILESTONE_AUDIT trailer** applied (2/3 reports cited).
- **Phase 4 PARTIAL → DONE flips:** conservative — 4 kits get UI-dimension AC ✅ Log entry but stay PARTIAL pending remaining deferred sub-gaps (267a Lighthouse + E2E / 269c Lighthouse + E2E / 271 ⌘K + role gate + MoET legal / 272 5 sub-letters f/g/m/n/o). No premature cascade per `gap-done-discipline.md` §2.
- **Phase 1 BETA critical-path step 1:** ⚡ ~80% — UI + Quality satisfied; Performance pending Wave 54.
- **Streak:** 88 (Wave 53 = 0 clarification rounds even with limit-hit recovery).
- **Wave 54 candidate scope:** Performance /100 re-spawn + step 2 production observability validation (GAP-115/116/117 state-check).

**📌 BRAND PIVOT 2026-05-10 — GAP-460 PLANNED P2 (deferred to Phase 1.5+):** User flagged collision searching "kitehub" Google → kitehub.eu (Czech water sports SaaS) #1; adjacent collisions KU Kite® (US K-12 assessment registered TM) + kiteclasses.org + Kerala IT. **Decision recorded: Path B' KiteClass.me as customer-facing brand + KiteHub stays internal** (dual-brand: customer remembers KiteClass; KiteHub = code repos / Docker / AWS / architecture docs / dev admin URLs only). **Execution deferred** per user 2026-05-10 "hiện tại không có công sức để rebrand" — Phase 1 BETA invite-only minimal SEO exposure → not blocker. **Phase 1 BETA tiếp tục dùng `kitehub.me` + KiteHub customer branding ad-interim.** 5-attribute review per `business-logic-review.md` §2 complete (decision valid). **Re-review trigger:** Phase 1.5 PAID public launch ≥30 days out / customer confusion incident / KU Kite®/KiteHub.eu file VN TM via Madrid / AWS Activate resubmit timing pressure. **GAP-461 META** filed — meta-rule `.claude/rules/brand-clearance-pre-domain.md` (deferred Wave 53+). **User-action defer too:** `kiteclass.me` claim + NOIP filing không rush, defensive registrations can happen any time.

**Wave 50 SHIPPED 2026-05-10 — Track 2 Phase 4 KH Kits (PRs #1091 plan + #1099 A kh-admin + #1096 B ai-branding-wizard v2):** 2 background agents Opus full + worktree isolation parallel ~78min vs ~7-9h estimate (6.2× speedup). Sequential merge A→B all CI green. GAP-271 + GAP-272 → 🟡 PARTIAL per `gap-done-discipline.md` §3.
- **GAP-271 kh-admin (#1099):** NEW `(school-admin)` route group at `/school-admin/*` (path-segment needed); 11 pages + 1 redirect; `SchoolAdminShell`; G1 + G3 + G10 imported from `@kite/shared-ui`; existing `(admin)/admin/**` PLATFORM admin untouched; auth gate in layout (no middleware change); login REUSE `/login`; 8/8 component tests + build green. Deferrals: ⌘K full keyboard handler stub, per-screen ≥105/128 audit (Phase 4 milestone), MoET legal sign-off (Phase 3 K-12), SCHOOL_ADMIN role gate (BE doesn't ship role).
- **GAP-272 ai-branding-wizard v2 (#1096):** **DISCOVERY:** wizard v2 already implemented via Wave 32+34 (17 components + reducer-driven 6-step orchestrator + 73 test files / 649 tests pass). Bucket B became **closure+verification** instead of rewrite (state-check pivot saved estimated ~6-8h). Shipped: `src/config/ai-input-cap.ts` centralized token-cap labels + regenerate quotas + estimator helper; TemplateStep refactor; 11/11 parent AC verified with implementation site map. Feature-flag rollout decision: REPLACE (no gating). 5 sub-letters remain open: 272f visual-regression / 272g E2E / 272m Advanced Mode persistence / 272n response wrapper / 272o orchestrator deploy-stream.

**Wave 51 SHIPPED 2026-05-10 — Wave 49 KC Follow-ups (PRs #1098 plan + #1100 A E2E sweep + #1101 B BE read APIs):** 2 background agents Opus + worktree parallel ~42min vs ~5h estimate (7.1× speedup). 5 sub-gap progressions:
- **GAP-267a** OPEN → 🟡 PARTIAL (Playwright DONE; Lighthouse defer post-HTTPS)
- **GAP-268b** OPEN → 🟢 DONE (Playwright happy + 1 error branch all pass)
- **GAP-269c** OPEN → 🟡 PARTIAL (Playwright DONE; Lighthouse defer)
- **GAP-268a** OPEN → 🟡 PARTIAL (`POST /api/v1/attendance/class/{classId}/batch` shipped + IT pass; FE wiring + outbox refactor deferred)
- **GAP-269b** OPEN → 🟡 PARTIAL (5 student-portal me-scoped read endpoints shipped + IT pass + NEW `documents/01-business/kiteclass/student-portal/` 3-layer docs + 5 BR + 5 UC; FE swap to real data + service-layer joins deferred to Phase 2 FE consumer PR)
- Bucket A spec adapted to actual fixture state (3 mismatches found: Lớp 10A2 not 6A1; localStorage key `kc.student.offline-submits` not `student-assignment-queue`; useMyChildren endpoint `/api/v1/parent/me/children` not `/api/parents/me/children`) — fragile-spec prevention via state-check.
- Bucket B created NEW `student/portal/` package separate from existing admin-facing `StudentController` to prevent scope mixing; outbox-event uniformity decision documented (inherits Wave 18b2 ApplicationEventPublisher pattern; cross-cutting outbox refactor separate scope).

**Phase 4 Track 2 progress:**

| Item | Trước | Sau Wave 50+51 |
|------|-------|----------------|
| Phase 4 kit DONE | 0/7 | 0/7 |
| Phase 4 kit PARTIAL | 2/7 (266 + 270) | **5/7** (266 + 267 + 268 + 269 + 270 + 271 + 272 = 7/7 PARTIAL — net **0 OPEN**) |
| Phase 4 kit OPEN | 5/7 | 0/7 |
| Phase 1 BETA §3.6 row #1 "8 Track 2 ports" | 2/8 | **7/8** (5+2 PARTIAL counting `@kite/shared-ui` shared-lib as 8th DONE) |

**Domain milestone audit deferred:** Per `post-wave-audit-mandate.md` §2.4 + §2.4.2, `phase-4-kit-ports` milestone reached at Wave 50 closure → audit suite obligation (UI /128 per kit + Quality + Performance) tracked as **GAP-462** (file separately). Audit prep checklist already produced via Explore agent (~5-6h sequential / 1.5-2h subagent-parallel). Streak: 86+87 = 87 consecutive 0-clarification-flip waves.

**Cleanup:** N worktree husks + merged branches pruned via `scripts/prune-merged-worktrees.sh --yes`.

**Wave 49 SHIPPED 2026-05-10 — Track 2 Phase 4 KC Personas (PRs #1089 plan + #1090 Bucket 0 PWA + #1092 A kc-parent + #1094 B kc-teacher + #1093 C kc-student):** 3 background agents Opus full với worktree isolation chạy parallel ~24min wall-clock vs ~8-9h estimate (21.3× speedup vs serial). Sequential merge A→B→C tất cả CI green (Frontend Tests + Build pass; E2E Playwright pass on A+C). Tất cả 3 GAP-267/268/269 flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 honest exit-ramp:
- **GAP-267 kc-parent (#1092):** 14/17 AC; Wave 18b1 `useMyChildren`+transcript preserved; 8 routes consolidating 17 prototype screens; Zalo OA primary card + Web Push fallback per VN UX musts §5; deferred: Lighthouse PWA ≥90 (needs HTTPS), Playwright E2E parent-invite spec, G7 redeem variant (tracked GAP-273). Sub-gap GAP-267a follow-up.
- **GAP-268 kc-teacher (#1094):** 24 screens → 11 routes under canonical `(teacher)/teacher/*` (bare `(teacher)/*` conflicted với `(dashboard)/attendance` + `(public)/page` per Next.js route-group semantics); preserved `(teacher)/attendance/period/[classId]/[periodNo]/[date]` Wave 18b2 route; G2/G3/G4/G8 imported từ `@kite/shared-ui`; 3 follow-ups (per-screen /128 UI audit + `attendancePeriodApi` overview-by-class extension + E2E flow spec).
- **GAP-269 kc-student (#1093):** 13 screens NEW `(dashboard)/student/*`; offline assignment retry shipped như page-context module `src/lib/offline/student-assignment-queue.ts` (NOT inline sw.js mod — JWT injection requires page context; clean separation from Bucket A permission flow); deferred social-login Zalo OA + Google backend / real REST endpoints / Lighthouse / E2E / login (existing `/login` reuse).
- **Phase 4 Track 2 progress:** 0/7 DONE → **3/7 PARTIAL (267+268+269)** + 2/7 PARTIAL pre-existing (266+270) + 2/7 OPEN (271+272 → Wave 50). Phase 1 BETA §3.6 row #1 "8 Track 2 ports shipped" advances 2/8 PARTIAL → 5/8 (3 PARTIAL + 2 PARTIAL pre-existing).
- **Discipline wins:** zero file conflicts (state-check disjoint paths verified pre-spawn); Wave 18b1 logic preserved verbatim; admin-merge-discipline.md respected (711/705/707 tests + builds + lint pre-PR on each bucket); zero silent DONE flips; max-cap 5 respected (3 parallel under cap). Streak: 85 consecutive 0-clarification-flip waves.
- **Cleanup:** 1 worktree husk + 2 merged branches pruned via `scripts/prune-merged-worktrees.sh --yes`.
- **Next**: Wave 50 (kh-admin K-12 Principal GAP-271 + ai-branding-wizard v2 GAP-272) per plan #1091 — 2 background agents Opus full ~7-9h longest path.

**Session 2026-05-10 SHIPPED — Tier 2 + Tier 3 automation cluster (PRs #1084 + #1085):**
- **PR #1084 MERGED** — Tier 2 status sync (4 docs reflect Origin Cert generated 2026-05-10) + new Tier 3 cutover runbook (`release-1-tier-3-cutover.md` ~600 lines: 10 steps + rollback + 13-criterion AC + Path X CLI vs Path Y workflow_dispatch).
- **PR #1085 OPEN** — Tier 3 cutover AUTOMATION (workflow_dispatch + narrow OIDC IAM `kitehub-github-tier-3-cutover` + `cloudflare-dns.sh` extended +4 subcommands `get/set-ssl-mode` + `get/set-always-https` qua extended Cloudflare token; Path Y 1-click setup ~1h pre-deploy).
- **AWS Activate $1k credit DENIED 2026-05-10** — reason "Your website cannot be accessed or fails to load". Curl audit confirmed (1) SSR bailout (`LandingShell.tsx ssr:false` → bot không-JS thấy "Đang tải trang chủ…") + (2) canonical URL trỏ `kitehub.vn` (16 hardcoded refs trong FE — domain dùng `.me` per GAP-458). **GAP-459 filed** với Phase 1-4 fix plan (canonical `.vn`→`.me` + LandingShellSSR server component); GAP-412 status flipped PARTIAL → 🔴 DENIED. Resubmit gated trên GAP-459 ship — work paused per user pivot ưu tiên Tier 3 automation trước.
- **Cloudflare token in-place edit** verified: extended `kite-cli-dns-edit` với `Zone:SSL:Edit` + `Zone:Zone Settings:Edit` (token VALUE preserved → no env/secret update). 4 REST API endpoints verified working.

**Wave 48 SHIPPED 2026-05-09** — DSAR DPO Email Notification (PR #1074, ~9min wall-clock vs 60-90min estimate, 8.3× speedup). Pattern reuse via `EmailServiceClient.dispatchEmail` outbox-first precedent: +2 methods in client + 2 Thymeleaf templates + `DsarServiceImpl.notifyDpo` wiring + 3 unit tests + `BR-PDPL-DSAR-006` 5-attribute review. **Cascade closure:** GAP-353c-followup-dpo-email-notification 🔵 OPEN → 🟢 DONE; GAP-353c parent 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (all 11/11 AC verified). mvn verify SUCCESS 455 tests; CI Test KiteHub Subscription Service 1m0s PASS.

**Wave 47 CLOSED 2026-05-09 — Phase A DEFERRED to Phase B (GAP-453):** Pre-flight verify aborted before flag flip; agent disproved 4 plan recon points (scope mismatch full e2e/ folder vs critical subset / route-mock incomplete in 2/3 spec files / `fullyParallel:true` not false / KC workflow `|| true` + `continue-on-error:true` makes flip semantically advisory). **Scope-correction note:** Wave 47 plan frontmatter listed `[GAP-403, GAP-404, GAP-420]` but those cover TAG-TIME pre-release E2E gate (workflow `e2e-pre-release.yml`, DONE Wave 37 2026-05-07) — Wave 47 actual scope was PR-TIME E2E gate activation in `frontend-ci.yml`/`kitehub-frontend-ci.yml`. **GAP-453 P1** now scopes that PR-time activation work với 3 Phase B options (B.1 docker-compose-in-CI / B.2 narrow subset scripts / B.3 MSW migration) — recommend B.2 as smallest reversible step.

**Phase 1 BETA Beta Access closure:** ✅ CODE COMPLETE 2026-05-08 (Wave 45 — GAP-372 DONE + GAP-370 PARTIAL pending AWS user-action).

**AWS Phase 1 BETA stack state (2026-05-09 09:52, confirmed cost-save — intentional):** account `906286017800` / `ap-southeast-1` đang ở mode tiết kiệm chi phí khi chưa có tenant onboarded — 2 EC2 (`kitehub-kh-backend`, `kitehub-kc-app`) STOPPED, RDS `kitehub-postgres` STOPPED, ALB `kitehub-alb` ACTIVE (DNS placeholder), CloudTrail `kitehub-main` IsLogging:True (audit baseline maintained), 0 alarms ALARM. **Resume khi:** beta tenant onboard / smoke test pre-launch / Phase 1.5 prep. Snapshot live nhìn qua `bash .claude/skills/workflow/start-session/scripts/collect-state.sh --refresh-aws` (cache 30m mặc định). Note này đặt ở §🚀 Next Action để session sau không nhầm "drift" → "force restart".

**Pending user actions (separate from session work):**
1. ✅ **Delete exposed `solo-dev-admin` orphan key** (AKID `AKIA…MMUZ`) — DONE 2026-05-08 via `scripts/delete-iam-access-key.sh` (orphan: not bound to any local profile, dev-admin uses `AKIA…52MY` unaffected). Audit: `documents/04-quality/audits/aws-verification/2026-05-08-orphan-key-delete-solo-dev-admin.md`. Future rotation of `52MY` available via `rotate-iam-access-key.sh` when chosen (not exposed, no urgency).
2. ✅ **Rotate `kite-readonly-wsl` key** — DONE 2026-05-08 via `scripts/rotate-iam-access-key.sh`; new AKID `AKIA…SVMD`, old `AKIA…E7SO` deleted. Audit: `documents/04-quality/audits/aws-verification/2026-05-08-key-rotation-readonly-wsl.md`
3. **CWAgent install** trên kh_backend via SSM Run Command — `right-size-stress-test.md` §1; alarm sẽ transitions từ INSUFFICIENT_DATA → active monitoring
4. **GAP-450 drift fix** separate session — `terraform import random_password` + verify state align
5. **AWS SES sandbox→production approval** (GAP-370 closure) — follow `documents/05-guides/deploy/email-ses-setup-runbook.md`; DKIM/SPF/DMARC verification; out-of-sandbox approval ticket; verify domain reputation pre-launch
6. **Domain procurement Release 1** (GAP-458 decision 2026-05-09) — chọn **Free path `kitehub.me`** qua GitHub Student Pack Namecheap (1 năm free, $10-20 renewal) per `account-prep/02b-github-student-pack-free-domain.md`. KiteClass tenants access qua subdomain pattern `tenant1.kitehub.me` (wildcard `*.kitehub.me`). User-action: claim domain → set Cloudflare nameservers → bind Vercel custom domain. Phase 1.5 PAID có thể switch sang `.vn` paid nếu cần thị trường VN trust signal.
   - **Tier 1 Frontend cutover DONE 2026-05-09:** Vercel apex `kitehub.me` bound + Let's Encrypt cert auto-issued (R13, valid 2026-05-10 → 2026-08-08); env var `NEXT_PUBLIC_API_URL=https://api.kitehub.me`; redeploy verified.
   - **Tier 2 Cloudflare/CLI tooling DONE 2026-05-10:** Email Routing active (`admin/support@kitehub.me` → Gmail forward); Cloudflare API token `kite-cli-dns-edit` saved (env + GitHub Secret); `scripts/cloudflare-dns.sh` wrapper shipped; **Cloudflare Origin Cert generated** (15-year validity, hostnames `kitehub.me + *.kitehub.me`, saved `~/.gcal-mcp/cloudflare-origin-cert/`); Wrangler CLI authed; Vercel CLI authed; 27 vercel:* skills loaded session.
   - **Tier 3 Backend cutover ⏳ PENDING:** runbook ready `documents/05-guides/deploy/release-1-tier-3-cutover.md` (10 steps + rollback + 13 acceptance criteria). Block trên: AWS Activate approval D+14 (calendar reminder 2026-05-23 set qua MCP) HOẶC user accept paying ~$85-100/mo trước approval. Sequence: resume EC2+RDS → ACM import Origin Cert → ALB HTTPS listener → Cloudflare proxy api.kitehub.me → SSL Full(strict) → Always HTTPS → smoke test.

**Wave 46 SHIPPED 2026-05-08** — Java deps bump cluster (GAP-440 + GAP-442). 3 buckets parallel ~75min:
- **#1059** plan
- **#1060** Bucket A docs-only — Spring Boot 3.5.14 IS upstream latest 3.5.x; GAP-451 filed (await-upstream)
- **#1062** Bucket B — Spring Cloud 2025.0.0 → 2025.0.2 in kiteclass-gateway (clears CVE-2025-41253 EL injection)
- **#1061** Bucket C — 10 Dockerfiles bumped alpine → noble (Java) / trixie-slim (Node 22); coordinator-applied gate raise 220MB → 320MB (Debian +60MB vs alpine, acknowledged trade-off)

CVE delta: 21 HIGH alerts → ~10 expected post-Trivy-rescan (12 npm-in-base + gnutls cleared by Bucket C; 9 Java CVE blocked GAP-451 await upstream).

**Wave 49+ candidates (Phase 1 BETA progression per `release-1-plan-2026.md` §3):**
- ✅ **Wave 48 SHIPPED** — DSAR DPO email integration (PR #1074); GAP-353c + GAP-353c-followup both 🟢 DONE; 8.3× speedup vs estimate via precedent reuse
- ✅ **Wave 47 CLOSED** — Phase A trial deferred to Phase B (GAP-453 P1 filed); plan executed but pre-flight aborted; closure log details in `documents/03-planning/waves/wave-47-e2e-activation.md` §8
- **GAP-453 (Phase B E2E)** — 🟢 DONE 2026-05-09 (both slices). KH B.2 narrow-subset gate active (PR #1078, ~9s local / 1m22s CI). KC C.2 direct-navigation gate active (PR #1079, ~17s local / 1m37s CI). Both KC + KH `if: true`, KC swallows removed → gates blocking. GAP-403/404/420 unblocked on Phase B path. **GAP-455 🟢 DONE** (this PR): KH coverage extended 5 → 12 tests (~28% → ~67% / ~100% of FE-reachable scenarios) via Phase 1 mock shape audit (3 DTO drift fixes vs `BetaAccessController` BE source) + Phase 2 7 error-branch tests (409 duplicate / 429 rate-limit / 400 honeypot / reject flow / 403 non-admin / TOKEN_EXPIRED / ALREADY_USED). Local 12/12 pass 15.1s. 3 ❌ remaining unreachable from FE (invalid-email/persona client-side regex; BE-consent-reject FE-disable).
- ✅ **Deployment-naming cleanup PR** — rule shipped PR #1055; cleanup PR shipped 2026-05-08 (3 file moves + 14 link updates: `operations/email-ses-setup-runbook.md` → `deploy/`, `operations/dns-setup-runbook.md` → `deploy/`, `deploy/terraform-apply-bootstrap.md` → `terraform-apply-bootstrap-runbook.md`). `secrets-management-runbook.md` split deferred to GAP-452 (P2, substantive editorial). `kiteclass-docker-deployment.md` already absent.
- DSAR + RTBF Phase 2 (GAP-353c + GAP-073)
- Custom domain procurement Phase 2 (GAP-369b deferred from Wave 43)
- GAP-451 await Spring Boot 3.5.15+ upstream (weekly check; if delayed >4 weeks, fall back per-CVE `<dependencyManagement>` overrides). Cross-link **GAP-456 Group B** — 12 of the 33 CVEs surfaced 2026-05-09 are Spring Boot transitive deps (10 Netty + bcprov + postgresql + commons-lang3 + spring-cloud-gateway); rolled into this tracking.
- **GAP-456 🟢 DONE 2026-05-09** — CVE triage: 33 alerts → 3 root-cause groups → A FIX (apt-get upgrade in 7 Dockerfiles, dpkg/libcap2/sed → 24.04.1, local verify proven) / B AWAIT-UPSTREAM (cross-link GAP-451) / C IGNORE-DOCUMENTED (`.trivyignore` 4 npm-internals entries, expiry 2026-08-09). Audit `documents/04-quality/audits/security/2026-05-09-cve-triage.md`. Repo level projected RED → YELLOW post-merge.
- GAP-441 P2 centralized pom override hygiene (originally deferred Wave 47; revisit after GAP-451 resolution)

**Strategic gates Phase 1 → Phase 1.5:** Quality audit /100 ≥80 (current 86 ✅) + 5 beta tenants live + 0 P0 incidents 2 tuần

---

### ⭐ Post-Wave-45 addendum 2026-05-08 — meta-rule + 2 CI fix PRs

Sau Wave 45 closure, 4 PRs bổ sung shipped để handle parallel-spawned tasks + recurring CI fail:
- **#1053** Wave 47 plan (E2E CI activation Phase A trial flag flip — renamed from Wave 46 in collision recovery 2026-05-08)
- **#1055** New rule `.claude/rules/deployment-naming-convention.md` v1.0.0 — incident-driven từ Wave 45 Bucket C drift (`email-ses-setup-runbook.md` actual path `operations/` vs plan-referenced `deploy/`)
- **#1056** Fix Trivy gate — extend staging.* exit-code 0 exemption to `main` push during Phase 1 BETA per `release-fix-retry-budget.md` §4 row 5; production gate intact cho v[0-9]+.* tags
- **#1057** Fix deprecated `DefaultCredentialsProvider.create()` → `.builder().build()` trong `SesIntegrationSmokeTest.java`

**Recurring CI fail loop broken:** post-Wave-45 closure 8 ECR push fails (run 25550657363) = recurrence của Phase 3 staging.1-7 saga. PR #1056 pivot ("remove the gate") thay vì patch (would have been 5+ retries). Won't re-trigger trên future main pushes during Phase 1 BETA.

---

### ⭐ Wave 45 SHIPPED 2026-05-08 — Beta Access Closure (GAP-372 + GAP-370)

**Trigger:** Wave 45 candidate selection — close 2 P0 BLOCKING gaps remaining cho Phase 1 BETA invite-only model. Wave 33 đã ship 90% GAP-372/370; Wave 45 closure scope = 3 follow-up items.

**4 PRs merged sequential** (~75min wall-clock vs 30-45min plan estimate; +30min coordinator-applied Bucket B finalization):
- **#1049** plan
- **#1051** Bucket A — `BetaAccessController.completeBetaSignup` wired vào `AuthService.registerFromBetaInvite` với conflict-rollback (SIGNED_UP → APPROVED + fresh 24h token); 3 new tests; +229/-5 LOC
- **#1052** Bucket B — Public `/register` disabled cả 2 frontends (BetaInviteOnlyNotice card pattern); coordinator-applied finalization sau agent terminated mid-build "Builds still running"
- **#1050** Bucket C — `SesIntegrationSmokeTest.java` profile-gated `@EnabledIfSystemProperty("aws-ses-real")` + `email-ses-setup-runbook.md` Wave 45 verification table all 7 steps ✅

**Bucket 0 Foundation skipped** (api-contract.md no drift — Wave 33+35 endpoints unchanged).

**Merge override:** All 3 với `ADMIN_MERGE_OVERRIDE: Vercel rate-limit external 24h block` per `admin-merge-discipline.md` §2 (Vercel build-rate-limit = qualified external transient).

**Gap status changes:**
- **GAP-372 → 🟢 DONE** (10/10 ACs satisfied); AWS SES production approval = user-executed (out-of-band)
- **GAP-370 → 🟡 PARTIAL** (code complete, AWS SES sandbox→production approval pending user-executed action)

**Incidents flagged:**
- Premature `prune-merged-worktrees.sh --yes` ran mid-wave (Agent 1 deployment-naming-rule still in flight) — violated `post-wave-cleanup.md` §2 anti-pattern. No work lost (agent was sandbox-blocked from Write/Bash before prune); rule reaffirms "DO NOT prune until ALL bucket PRs merged + no in-flight non-bucket agents".
- Drift detected: `email-ses-setup-runbook.md` actual path `documents/05-guides/operations/` while plan referenced `deploy/`. Triggers deployment-naming-convention rule + cleanup PRs (sister tasks parallel to closure).

**81st consecutive 0-clarification streak.** Stake MEDIUM, Opus medium effort.

---

### ⭐ Wave 43+44 bootstrap apply COMPLETE 2026-05-08

**Bootstrap via local `terraform apply`** (admin profile, chicken-and-egg per `release-deploy-standard.md` §9 v1.0.1 carve-out):

| Item | Status |
|---|---|
| 8 EventBridge schedulers ENABLED | ✅ first stop 22:00 ICT tonight |
| `kh_backend` t3.medium running healthy | ✅ in-place modify ~30s restart |
| `kc_app` replaced (`i-04f65503ace7febe4` → `i-07f6de54544162124` stopped) | ⚠️ unintentional, drift GAP-450 |
| `github_terraform_apply` IAM role provisioned | ✅ workflow_dispatch ready |
| GitHub Variable `AWS_TERRAFORM_APPLY_ROLE_ARN` set | ✅ |
| SNS `kitehub-memory-alerts` + email confirmed | ✅ `vannkite@outlook.com` subscription ARN `99266533-...` |
| `kh_backend_memory_high` alarm | ⚠️ INSUFFICIENT_DATA (CWAgent install pending) |
| `kc_app_memory_high` alarm | ⏸️ deferred Phase 7 (kc_app drift) |
| `kite-readonly-wsl` IAM user + Tier 1 boundary verified | ✅ Tier 3 `stop-instances` → AccessDenied confirmed |

**Verification artifact:** `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md`

**Cost saving achieved:** $157/mo → ~$45-55/mo target → $200 credit longevity 1.3 → 3.5-4 tháng (đủ Phase 1 BETA 9-12 tuần).

**Closeout PRs:** #1046 SNS tag fix + #1047 verification artifact + GAP status flips + GAP-450 filed.

**80th consecutive 0-clarification streak.**

---

### ⭐ Wave 44 SHIPPED 2026-05-08 — terraform-apply workflow_dispatch infra

**Trigger:** Wave 43 closure user-flagged "tại sao cần rule terraform apply human-only?" — rule §9 over-restrictive blocking workflow_dispatch + confirm pattern (industry standard Atlantis/TF Cloud).

**4 PRs merged sequential** (~15min wall-clock, 2× faster than 30min estimate; 1 CI fmt fix mid-flight):
- **#1041** plan
- **#1044** Bucket A coordinator-applied — rule revision §9 v1.0.0→v1.0.1 distinguishing 4 cases (auto-apply BAN preserved + agent-apply BAN preserved + ✅ workflow_dispatch carve-out + ⚠️ chicken-and-egg bootstrap carve-out) + `agent-aws-access.md` §4.3 cross-link + settings `Edit/Write(.claude/rules/**)` explicit permission (sandbox previously blocked agent edit despite `"*"` allow)
- **#1042** Bucket B — `.github/workflows/terraform-apply.yml` (workflow_dispatch + confirm input "APPLY" + dry_run mode) + new IAM `github_terraform_apply` role (PowerUserAccess + IAM perms + state access) + outputs
- **#1043** Bucket C — bootstrap runbook 387 LOC (one-time admin local apply → workflow_dispatch takeover → Wave 43 verify → GAP-446/447 DONE flip → admin key rotate)

**Lessons:** sandbox enforces explicit per-path policy on `.claude/rules/**` regardless of `"*"` wildcard + `bypassPermissions`. Settings hardened cross-session.

**79th 0-clarification streak.** Stake MEDIUM, Opus medium effort.

**Post-merge user actions (Wave 44 → Wave 43 close):** see `documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md` — one-time bootstrap apply → set GitHub Variable → workflow dry_run test → real apply → 2× verification artifacts → flip GAP-446/447 → rotate admin key.

---

### ⭐ Wave 43 SHIPPED 2026-05-08 — Cost discipline cluster

**Trigger:** User-flagged "ALB/EC2/RDS chạy liên tục lãng phí, $200 credit cháy 1.3 tháng".

**4 PRs + plan PR merged sequential** (~6-8min agent work, 4× faster than 30min estimate):
- **#1035** plan + GAP-448 Vercel `ignoreCommand` inline (2× vercel.json — Vercel quota saved validated post-merge via #1036/1037/1038 SUCCESS=Skipped)
- **#1036** Bucket A GAP-446 EventBridge Scheduler stop/start EC2+RDS (8 schedules Asia/Ho_Chi_Minh, IAM scoped tag-based + ARN, `enable_cost_scheduling` toggle, 270 LOC) — 🟡 PARTIAL (terraform shipped, CI apply pending)
- **#1038** Bucket B GAP-447 right-size m7i-flex.large → t3.medium + CloudWatch memory alarm + stress-test runbook + GAP-411 post-Vercel matrix update — 🟡 PARTIAL
- **#1037** Bucket C admin sweep — **GAP-373 → 🟢 DONE** Better Stack evidence, GAP-369 Phase 2 rescope, GAP-377/378 verified DONE Wave 25/26 (Wave 42 different layer), GAP-413 Log update
- **#1039** bonus — Java unchecked varargs warning fix `BetaAccessServiceTest:467`

**Pre-spawn cost saving:** kc-app instance stopped 2026-05-08T08:11Z (-$60/mo started immediately, mâu thuẫn GAP-445 fixed).

**Combined burn rate impact:** $157/mo → ~$45-55/mo target (post-CI-apply), **$200 credit 1.3 tháng → 3.5-4 tháng** (đủ Phase 1 BETA 9-12 tuần).

**Post-merge user actions pending:** terraform apply both stacks → §3 verify commands per `aws-cost-scheduling.md` + stress test per `right-size-stress-test.md` → file 2× verification artifacts → flip GAP-446/447 to DONE.

**78th 0-clarification streak.** Stake MEDIUM, Opus medium effort. 0 CI fails. 0 scope creep. State-check `audit-to-gap-pipeline.md` §2.5/§2.6 caught 2 cost leaks pre-spawn (kc-app running mâu thuẫn GAP-445 + dual m7i-flex.large over-provision).

---

### 🟢 RELEASE LẦN 1 PHASE 1 BETA ACTIVE — đọc trước

> **Authoritative plan:** [`documents/03-planning/roadmap/release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) — Phase 1 BETA (v0.9.0-beta, 9-12 tuần) → Phase 1.5 PAID (**v1.0.0** PRODUCTION, +4-6 tuần) → Phase 2 P3 (v2.0.0) → Phase 3 K-12 (v3.0.0). Target: ~13-18 tuần đến public paid launch.
>
> **Deploy plan chi tiết:** [`release-1-deploy-plan.md`](../../03-planning/roadmap/release-1-deploy-plan.md) — go-live runbook + rollback + 12 BLOCKING/STRONGLY recommend gaps GAP-369..380.
>
> **Versioning policy:** [`versioning-policy.md`](../../03-planning/roadmap/versioning-policy.md) — semver convention.
>
> **Phương châm session mới (chốt 2026-05-06):** ƯU TIÊN Phase 1 BETA wave-pack candidates (Wave 25-30) trước khi pick non-Release-1 gaps. Reference: `feedback_release_1_first_session_priority.md` (auto-loaded memory).

### 🚀 Next Action (signpost for new session)

**Recommended next pick (priority order per `feedback_release_1_first_session_priority.md`):**

🎉 **Phase 3 SHIPPED 2026-05-08** — staging.8 run #25530089671 SUCCESS. 8/8 services pushed to ECR `906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/*` với tag `v0.9.0-beta-staging.8`. OIDC end-to-end verified (AssumeRole + ECR push 200 OK + Trivy scan clean per CRITICAL-only staging gate).

**Phase 3 retro:** 8 retry attempts (staging.1→staging.8). 5 distinct failure modes surfaced + fixed:
1. ❌ staging.1 multi-arch base image manifest amd64-only → PR #1004 amd64-only workflow
2. ❌ staging.2 IAM ARN `kitehub-*` vs `kite/*` → PR #1005 ARN pattern fix + `terraform apply -target=`
3. ❌ staging.3 Trivy 6 HIGH+CRITICAL Java → PR #1009 `.trivyignore` (later superseded)
4. ❌ staging.4-6 Trivy CRITICAL-only gate iteration → PR #1011/#1012/#1014 (rule violation per release-fix-retry-budget.md §4 — 3 wasted retries)
5. ❌ staging.7 SBOM 403 Release-not-exists → PR #1015 skip post-push verify for staging.* + new rule `release-fix-retry-budget.md` v1.0.0
6. ❌ staging.7 actionlint legacy bugs → PR #1016 actionlint warn-only + skill Cat #9 post-push tag-class verify

**Lessons codified:**
- New rule `.claude/rules/release-fix-retry-budget.md` v1.0.0 (retry #2 → §3 STOP-AND-PIVOT decision flow)
- Skill `deploy-preflight-simulator` Cat #9 (post-push verify steps need tag-class `if:` guard)
- New CI workflow `actionlint.yml` (warn-only initially, tighten via GAP-443)
- 4 follow-up gaps: GAP-440/441/442 prod hardening trio + GAP-443 actionlint cleanup + GAP-444 Phase 4 deferral

**Phase 4 staging deploy DEFERRED** to Phase 7 production prep per GAP-444 + `release-fix-retry-budget.md` §4 pivot. Phase 4 scaffold (`docker-compose.staging.yml` missing + `deploy-staging.yml` workflow has bugs) was over-spec'd for solo-dev + Phase 1 BETA invite-only context. Phase 7 T-7 prep window will build prod-equivalent artifacts once correctly.

**Provisioned this session:**
- ✅ `vars.AWS_CONFIGURED=true`
- ✅ `vars.BACKUP_DRILL_ENABLED=false`
- ✅ `secrets.RDS_ENDPOINT` = `kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com`
- ✅ `secrets.REDIS_ENDPOINT` = `redis://kite-redis:6379`
- ✅ IAM ECR ARN pattern `kite/*` applied (PR #1005 + targeted apply)

**Next session priorities:**

1. **Phase 7 T-7 prep window** (per GAP-444 sub-tasks 7.1.1-7.1.7): create `docker-compose.production.yml` + fix `deploy-production.yml` + EC2 bootstrap + first deploy SSM exec test + smoke test + first invite tenant signup
2. **GAP-440** Spring Boot 3.5.14 → latest dep bump (clears 4-5 of 6 Java CVE classes) — **BLOCKED 2026-05-08 by GAP-451**: Wave 46 Bucket A pre-flight confirmed Maven Central has no newer 3.5.x patch yet. Await upstream Spring Boot 3.5.15+ release (monitor weekly).
3. **GAP-442** alpine 3.23 → 3.24 base image bump (clears CVE-2026-33845 gnutls)
4. **GAP-441** per-service pom override hygiene (parent pom dependencyManagement)
5. **GAP-451** Spring Boot 3.5.x no newer patch — monitoring upstream; unblocks GAP-440 when 3.5.15+ ships

After GAP-440/441/442 trio + Phase 7 prep → tag `v0.9.0-beta` (production launch) with strict Trivy gate naturally passing.

---

✅ **Wave 41 SHIPPED 2026-05-08** — fix-cluster post Wave 40 audit. 6 buckets A-F merged sequential. PRs #983 (A) #981 (B) #985 (C) #986 (D) #982 (E) #984 (F). 1 DONE (D GAP-272o 6/6 AC) + 5 PARTIAL với follow-ups tracked. Wall-clock ~30min vs 3.5h estimate (7× faster). 77th 0-clarif streak. ⚠️ Bucket C BREAKING API (Page envelope) — admin FE adapter check pre-Phase-7 needed.

✅ **Phase 2.1+2.2+2.3 DONE 2026-05-07** — AWS Singapore production infrastructure live. PRs #989 (closed superseded) → #990 (partial backend) → #991 (OIDC plan role) → #992 (CloudTrail GAP-437 Phase 1) → #993 (3 OIDC roles GAP-436 Phase 1+2+3) → #994 (Phase 2.3 71 resources + dashboard GAP-437 Phase 2) → #995 (agent-aws-access rule + first audit artifact GAP-438 Phase 1+3) → #996 (Wave 42 plan).

**Account 906286017800** state: ~94 AWS resources, $30/mo Year 1 (Free Tier 12mo). Outputs:
- ALB DNS: `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` (502 — backend chưa deploy)
- EC2 KH: `i-0b65c3947d36cae61` (13.212.99.40)
- EC2 KC: `i-04f65503ace7febe4` (47.128.15.254)
- RDS: `kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com:5432`
- ECR: `906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/{10 repos, all empty}`
- 8 Secrets Manager (3 auto-populated rds/jwt/encryption; 5 placeholders)
- CloudTrail trail `kitehub-main` (multi-region, IsLogging=true)
- CloudWatch dashboard `kitehub-phase-1-overview`

**Stream A pivots:**
- Frontend hosting: Cloudflare Pages → **Vercel** (`kitehub.vercel.app` + `kiteclass.vercel.app` HTTP 200)
- Email: AWS SES → **Resend** (`RESEND_API_KEY` in GH Secret)
- Status page: Atlassian Statuspage → **Better Stack** (`kite-platform.betteruptime.com`)

🚨 **NEW SESSION START HERE — Wave 42 spawn READY + Phase 3 user-action**

**Top priority — Wave 42 spawn (plan đã merge #996, ready to spawn 5 background agents):**

Plan: `documents/03-planning/waves/wave-2026-05-08-42-aws-deploy-followups.md`

5 buckets parallel (~65min wall-clock estimate):
| Bucket | Scope | Files |
|---|---|---|
| B | GAP-438 Phase 2 — skill `aws-smoke-test` + `scripts/smoke-aws-phase-N.sh` | `.claude/skills/devops/aws-smoke-test/` + `scripts/smoke-aws-phase-N.sh` |
| C | Phase 2.4 helper — `scripts/populate-secrets.sh` | `scripts/populate-secrets.sh` |
| D | Phase 3 prep runbook | `documents/05-guides/deploy/phase-3-image-push.md` (new) |
| E | GAP-436 Phase 4 — static-key removal checklist | `documents/04-quality/gaps/GAP-436-...md` (update) |
| F | GAP-438 Phase 4 — memory entry | `feedback_agent_aws_readonly_logging.md` |

**⚠️ Pre-spawn check:** verify PR #997 (memory→rules migration) state first. Memory entries Wave 42 references may have migrated to `.claude/rules/`. If migration done: update Wave 42 plan §3 references; if migration in-flight: wait + re-plan.

**Stream A — User-action (BLOCKING Phase 1 BETA launch):**

✅ Phase 1.1-1.5 substantially complete via pivots (Vercel + Resend + Better Stack live). Domain registration (1.1) skipped — using free `*.vercel.app` subdomain Phase 1; defer custom domain Phase 2.

**Phase 3 image push (user-action, ~10min):**
1. `gh variable set AWS_CONFIGURED --body true` (gates `docker-build-push.yml` ECR push job)
2. `git tag v0.9.0-staging.1 && git push origin v0.9.0-staging.1`
3. CI workflow auto-runs build 9 services + push to ECR
4. SSM session vào EC2 → `docker compose pull && docker compose up -d`
5. Verify ALB → 200 OK

**Stream B — Wave 43 candidates (post Wave 42):**

| Gap | Priority | Note |
|-----|----------|------|
| GAP-117 restore drill | 🟠 P1 | Cần staging up — defer until Stream A complete |
| GAP-204 npm CVE sweep + Trivy exception | 🟡 P2 | Wave 41 G optional skipped |
| GAP-435 promtool alert-rule unit test | 🟡 P2 | Wave 41 Bucket A follow-up |
| GAP-434 Loki/Promtail backend stack Phase 2 | 🟠 P1 | Wave 41 Bucket F follow-up, ~6-8h |
| GAP-272e SSE E2E EventSource polyfill | 🟡 P2 | Wave 41 Bucket D follow-up |
| Bucket C breaking API Page envelope — admin FE adapter check | 🟠 P1 | Pre-Phase-7 verify kitehub-admin consumers |
| ALB ACM cert + HTTPS listener | 🟠 P1 | Currently HTTP-only; Phase 4 staging gate cần HTTPS |
| EC2 user-data deploy script | 🟠 P1 | After Phase 3 image push: SSM run-command auto pull + compose up |
| GAP-156 rules.md Phase 2 stakeholder sign-offs | 🟡 P2 | After GAP-433 Phase 1 backfill |

**Pre-conditions before Wave 43 plan:**
1. Wave 42 closure complete (5 buckets B-F merged + closure PR)
2. PR #997 memory→rules migration merged
3. Phase 3 first image push verified (validates OIDC end-to-end)
4. State-check Wave 42 follow-up gaps AC clean

**Saved memories session 2026-05-07** (auto-load each session):
- `feedback_aws_sg_description_ascii_only.md` — em-dash trap
- `feedback_terraform_partial_backend_public_repo.md` — partial config pattern (public repo)
- `feedback_aws_observability_first.md` — CloudTrail before infra
- `feedback_terraform_targeted_apply_phases.md` — phased rollout
- `feedback_terraform_apply_retry_reconfirm.md` — re-confirm each retry
- `feedback_session_currentdate_check.md` — dates from `currentDate` not filenames

→ Migration to `.claude/rules/` in flight via PR #997 (5/6 entries become git-tracked rules).

---

⭐ **0. Wave 40 SHIPPED 2026-05-08** — Audit milestone cụm `release-deploy-artifacts` (đóng AUDIT_DEFER tag của Wave 33+34+37+38+39 theo `post-wave-audit-mandate.md` §2.4). **7 audits song song**: A UI 111.3/128 A+ +14.3 (#978), B Quality 86 B+ +6 ✅**PASS**(#973), C Security 87 B +3 ✅**PASS**(#974), D Performance 75 C +4 (#972), E Ops 60 D +7 (#975), F API Contract 72 C+ +1 (#976), G Business Logic 68 C -14 recalibration (#977). **2 cổng critical Quality + Security PASS Phase 7 ≥80** — production deploy unblocked từ formal gate view. 5 gaps mới + 9 P0/P1 surfaced cho Wave 41 cluster: GAP-427 (F API drift exchange-claim-code), GAP-428 (A UI prospect public pages), GAP-429 (A UI transient state UX), 3 P1 Performance (Analytics/Payment/Instance findAll unbounded), 1 P0 Ops (BackupJobFailure metric mismatch), 1 P1 Ops (startupProbe), 3 P1 BL (rules.md 5-attr coverage). 76th 0-clarif streak. ~65min wall-clock parallel + ~5min closure. **DOMAIN_MILESTONE_AUDIT trailer applied**. **Recommended next:** Wave 41 fix-cluster (Performance P1s + Ops P0 + GAP-272o + GAP-115/135 monitoring; ~6-10h ~5-7 ngăn parallel) HOẶC Stream A user actions (đang chạy song song).

⭐ **0a. Wave 39 SHIPPED 2026-05-07** — Dev-stack readiness + KC critical-journeys E2E reconciliation. 5 PRs merged (#963 plan, #964 D dev-stack verify, #965 C course-to-class-flow, #966 Stream C 6 docs, #967 A dashboard-nav, #968 B class-lifecycle). 4/4 Stream B buckets DONE: GAP-417 ✅ (Wave-39-eve), GAP-418 ✅, GAP-419 ✅, GAP-420 ✅ (17/17 tests pass A 8/8 + B 6/6 + C 3/3). Stream C 6 docs DONE: 4 GAP-394 account-prep runbooks (`documents/05-guides/account-prep/`) + GAP-423 SES VN overlay 0.99% → 15.72% density + GAP-424 Statuspage VN overlay 15.1% → 30.02% density + Instatus signup walkthrough 7-step VI. **3-stream parallel strategy validated** (Stream A user real-world + Stream B 4 bg agents wave-pack 65min + Stream C foreground docs 45min — orthogonal scope, 0 collision). 1 follow-up gap filed: **GAP-425** (cold rebuild BE images stale — surfaced "visual lần 1" cold-rebuild test post-closure: stale 6-week kitehub-subscription:latest with pre-GAP-242 V11 SQL syntax error → crash loop. P2). 75th consecutive 0-clarif streak. **Recommended next pick:** **Wave 40 audit-suite refresh + Phase 1 BETA prep** (per `post-wave-audit-mandate.md` §2.4 — release-deploy-artifacts cluster milestone audit suite still pending; Wave 33+34+37+38+39 multi-domain). HOẶC continue Stream A user actions (Cloudflare 1.3 đã sẵn sàng VN). HOẶC **GAP-272o** orchestrator wiring (P1, parallel-track during user Phase 1).

⭐ **0a. Operational session 2026-05-07 (evening — WSL kite-dev migration)** — User chuyển dev env sang WSL distro `kite-dev` để có 28GB RAM cap; environment validated end-to-end. **4 PRs shipped:**
- **#958** `rule(agent-action-bias) v1.0.0` — new MANDATORY rule: agent does work itself, prefer command over UI. Triggered by Docker Desktop WSL Integration UI-loop incident in same session. Memory `feedback_agent_action_bias.md` paired auto-load.
- **#917** `gap(394) Phase 1 BETA account-prep checklist` — filed via merge-conflict-resolved rebase (was stale from morning).
- **#959** `gaps(421/422)` — filed + fixed same PR per `agent-action-bias.md`: GAP-421 `down.sh --profile` flag (symmetric với up.sh) + GAP-422 94 `.sh`/`.py` exec-bit restoration on main (mode 100644→100755). Both 🟢 DONE.
- **#960** `chore(pnpm)` — approve build scripts (msw + sharp + unrs-resolver) + generate `mockServiceWorker.js` cho cả 2 FE.

**Environment validated:**
- Docker Desktop WSL Integration: enabled qua `settings-store.json` + jq + taskkill.exe + cmd.exe restart (commit-path, không UI walkthrough)
- Stack `infra-only` profile up time: ~17s healthy (postgres 5433 · redis 6380 · rabbitmq 15673 · minio 9100/9191 · mailhog 8025) · stack RAM 335MB / 27GB host
- pnpm workspace install: 787 packages (4.3s); 3 build scripts approved + ran successfully
- MSW worker generated cho cả 2 FE (md5 ba3d7ace... identical, 9120 bytes); convention-committed
- GitHub MCP: ✓ Connected (re-added qua `claude mcp add github`)

**🚨 NEXT — 3-stream parallel strategy (recommended for new session):**

| Stream | Owner | Time | Status |
|---|---|---|---|
| **A** User-action prereqs (signup AWS/domain/CF/SES/Instatus) | User | 1-2 ngày real-world | Ready to start NOW with existing `release-1-deploy-runbook.md` Phase 1 §1.1-1.5 — đã có high-level checklist 5 phần; chi tiết 1.3-1.5 đã có runbook (Wave 38) |
| **B** Wave 39 (4 buckets dev-stack/E2E fix) | Background agents (parallel via wave-pack) | ~45-60 min wall-clock | Buckets: GAP-419 P0 gateway + GAP-418 P1 KH Dockerfile + GAP-420 P1 KC selectors + GAP-417 P2 setup.sh JWT_SECRET (đã giảm 6→4 do GAP-421/422 fix Wave-39-eve) |
| **C** Phase 1 VN docs cluster: GAP-394 (4 runbooks) + GAP-423 (SES VN overlay) + GAP-424 (Statuspage VN overlay) = **6 docs total** | Foreground agent (sequential docs work) | ~5h | GAP-394 fills 4 missing (AWS walkthrough · domain registrar VN comparison · password manager · superadmin first-login). GAP-423/424 fill VN coverage gap on existing EN runbooks (SES 0.99% VI / Statuspage 15% VI). |

**Stream A coverage status (per Stream A audit 2026-05-07 evening):**

| Phase 1.x | Doc | VI status | Verdict |
|---|---|---|---|
| 1.1 Domain | `domain-management.md` + ADR-018 | Mixed (architectural) | ⚠️ Cần signup walkthrough → GAP-394 |
| 1.2 AWS account | ❌ Không tồn tại | — | ❌ → GAP-394 |
| 1.3 Cloudflare | ✅ `cloudflare-setup.md` (496 dòng) | ✅ Vietnamese đầy đủ | ✅ **Sẵn sàng** |
| 1.4 SES email | `email-ses-setup-runbook.md` (304 dòng) | ❌ 3/304 VI = effectively EN | ⚠️ → GAP-423 |
| 1.5 Statuspage | `incident-comms-runbook.md` (205 dòng) | ⚠️ 31/205 VI mixed | ⚠️ → GAP-424 |

**A có thể start NGAY với 1.3 Cloudflare** (đủ VI). Các phần còn lại signup được nếu user OK đọc EN — VN overlay là to-have, không hard-blocking.

**Recommended new session flow:**
1. `/start-session` → đọc summary này
2. Confirm A status với user (đã signup AWS chưa? domain registered?)
3. Spawn Wave 39 background (4 parallel agents per `agent-background-spawn-default.md`)
4. Foreground viết GAP-394 4 runbook docs sequential trong khi B chạy
5. Closure: merge B PRs (each independent) + ship C as single PR

0a. **Wave 36 SHIPPED 2026-05-07** — 5 P1 clusters DONE + audit re-run validated Phase 1 BETA Quality trigger gate. PRs A→B→C→D→E sequential merged: #933 Bucket A (GAP-388 security cluster + GAP-387 honeypot dead-wire absorbed via @ExceptionHandler controller-scoped wire-up + 6-digit claim code 2FA + V33 + Caffeine rate-limit), #930 Bucket B (GAP-390 API polish — tenantId via MDC + 4 SSE event assertions + UUID examples), #929 Bucket C (GAP-389 ops — scripts/backup-production.sh NEW + smoke-test extend + 7 BR-LIFE/QUALITY 5-attribute compliance blocks), #932 Bucket D (GAP-393 perf — Caffeine quota cache + SSE backpressure cap + idempotency cache; coordinator rebase conflict resolved on `DeployStreamControllerTest.java` additive merge B+D), #931 Bucket E (GAP-391 UI — RegenerateCounter quota refresh regression test + i18n-strategy.md NEW). 6 GAPs flipped DONE per `gap-done-discipline.md` §2. **Audit aggregate post-Wave-35 (re-run 7 specialists):** Quality 80/100 B (+7) ✅ Phase 1 trigger gate, Security 84/100 B (+12), Performance 71/100 C (+13), Business Logic 82/100 B− (+4), API Contract 71/100 C (-1 inventory), Ops Readiness 53/100 F (+3), UI 99/128 A+ (+2). 1 NEW P0 (GAP-387 honeypot dead-wire) bundled into Bucket A. 72nd consecutive 0-clarif streak. ~90min wall-clock.

1. **Wave 35 SHIPPED 2026-05-07** — 5 P0 BLOCKERS Phase 1 launch DONE: GAP-384 admin auth (#922) + GAP-385 PDPL consent (#921) + GAP-386 quality threshold (#919) + GAP-387 beta metrics (#920) + GAP-392 N+1 + V31 indexes (#918). Bucket 0 Foundation #916 cross-layer api-contract per `contract-first-for-cross-layer.md` v1.0.0. Side PRs same session: **#923 refactor Oracle→AWS Singapore Free Tier** (ADR-025 ACCEPTED — Phase 1 BETA cloud platform locked AWS) + **#924 CI path-filter fix** (kitehub-ci.yml scoped to backend modules, no longer fires on docs/FE PRs). 71st consecutive 0-clarif streak. ~95min wall-clock. **Audit re-run pending** per Wave 35 plan §7 closure protocol — verify Quality ≥80, Security ≥80, Phase 1 trigger gate progression.

2. **Wave 37 SHIPPED 2026-05-07** — 5 buckets parallel disjoint, 22 GAP-NEW Phase 1 BETA deploy readiness. PRs A→B→C→D→E sequential merged: #938 Bucket A (GAP-395/396/397 Terraform AWS Singapore Architecture B + state backend + plan CI OIDC, 13 .tf files), #936 Bucket B (GAP-398..402 Docker release: 5 KH Dockerfile multi-stage + ECR ap-southeast-1 + Trivy + multi-arch + SBOM/Cosign — coordinator-applied 2 fixes: COPY all sibling pom stubs cho parent `<modules>` validation + pnpm@9 pin trên 2 frontend Dockerfile do pnpm@latest=11 require Node 22.5+ node:sqlite builtin nhưng images chạy Node 20), #937 Bucket C (GAP-403..406 E2E pre-release gate + 3 beta-funnel specs + visual regression + OWASP baseline doc), #940 Bucket D (GAP-407..410 Compose profiles + JVM heap cap + Ollama stop policy + WSL2 config — coordinator-applied trực tiếp sau 2 lần Sonnet agent autocompact-thrash), #939 Bucket E (GAP-411..416 AWS sizing matrix + ADR-026 Ollama defer + Activate pitch deck + Budgets monitoring + EKS migration plan + cost report template). **Status flips:** 8 gap → 🟢 DONE (395/396/397/403/404/407/409/410/411/416), 14 gap → 🟡 PARTIAL (production execution + staging baselines + human-action items per `gap-done-discipline.md` §3 PARTIAL exit ramp). 73rd consecutive 0-clarif streak. Wall-clock parallel ~12 min longest (coordinator-applied Bucket D heaviest) + ~10 min coordinator CI fix iterations + ~10 min closure. **Audit strategy:** `AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts` — milestone = Phase 1 BETA launch wave (post-deploy AWS apply + smoke + signoff) per `post-wave-audit-mandate.md` §2.4.

3. **Wave 38 SHIPPED 2026-05-07** — Phase 1 BETA P1 STRONGLY cluster (4 P1 gaps). PRs all merged: #943 Bucket A (GAP-374 tag-based release CI workflow + generate-changelog.sh + versioning-policy §6.4), #945 Bucket B (GAP-371 Cloudflare CDN runbook 13 sections + verify-cdn-headers.sh — coordinator-salvaged Sonnet thrash), #944 Bucket C (GAP-373 incident-comms-runbook 9 sections + post-mortem-template + ADR-027 Instatus vendor — coordinator-applied Sonnet thrash 2x), #946 Bucket D (GAP-380 staging activation Architecture B — staging.tf 342 LOC + deploy-staging.yml rewrite + seed-staging-fixtures.sh + activation runbook — Opus salvaged 529 + coordinator post-fix terraform heredoc-ternary syntax). **Status flips:** 0 DONE / 4 PARTIAL — all deferred user-action (Cloudflare account+nameserver, Statuspage signup+DNS, terraform apply+first deploy, notification channel+live tag E2E). 74th consecutive 0-clarif streak. Wall-clock parallel ~12 min longest agent + ~15 min coordinator iterations + ~10 min closure. **Side-PR #947 release-1-deploy-runbook DRAFT** — Phase 0-9 ordered sequence post user-flagged "Isn't this an E2E test?" re-trace request; identifies 5 missed-items (plan stale, GitHub vars, ad-hoc test tag, OWASP wire, visual regression). **Phase 1 BETA P1 STRONGLY row → 0 OPEN** (all 7 either DONE/PARTIAL pending user-action). **Audit strategy:** AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts (same domain as Wave 37) — milestone Phase 1 BETA launch wave.

4. **✅ Phase 0 plan refresh DONE 2026-05-07** — PR #947 runbook merged + plan refresh shipped (this PR): `release-1-deploy-plan.md` §1.1+§1.2+§1.3+§2.1+§2.2+§2.4+§9 rewritten Oracle→AWS Singapore Architecture B per ADR-025; ADR-025/026/027 cross-linked; Oracle deployment doc archived → `documents/07-archived/oracle-deploy-2026/`; `documents/02-architecture/deployment-strategy.md` + `03-planning/README.md` + `03-planning/infrastructure/kitehub-infrastructure.md` cross-refs updated. **🚨 NEXT — User Phase 1 actions** (per [`release-1-deploy-runbook.md`](../../03-planning/roadmap/release-1-deploy-runbook.md) §1): domain registration `kitehub.vn`+`kiteclass.vn` · AWS account + Activate Founders Pack pitch · Cloudflare account + nameserver migration · SES production access request · Instatus signup. Estimated 1-2 ngày user time; agent tracks progress.

4a. **🟡 Operational session 2026-05-07 (post-Wave-38)** — 6 PRs shipped after Wave 38 closure (#947 runbook DRAFT, #949 Phase 0 plan refresh, #950 KH beta-funnel E2E specs reconciled 5/5 pass, #951 dev-stack admin S3+RabbitMQ config fixes, #952 follow-up gaps GAP-417/418/419 filed, #953 KC E2E login helper VN heading + GAP-420 filed). Validated mocked E2E both frontends; surfaced 4 dev-stack gaps cold-setup blockers. **Phase 4.5 staging E2E gate scope confirmed:** KH `beta-funnel/` 5/5 ✅ + KC `critical-journeys/` 4/8 sample (post-helper-fix) → 13 selector failures pending GAP-420. Total gate target post-fix-cluster = 22/22.

4b. ~~**🚨 RECOMMENDED Wave 39 candidate cluster**~~ — ✅ **SHIPPED 2026-05-07** as Wave 39 (see ⭐0 above). 4/4 buckets DONE; +GAP-425 filed as cold-rebuild follow-up.

5. **GAP-272o** P1 — wizard orchestrator wires `useDeployStream` + `useRegenerateQuota` vào DeployingStep + RegenerateCounter. Filed Wave 34 closure 2026-05-07. (Parallel-track: agent có thể pick up trong khi user chạy Phase 1.)
4. ~~**Wave 34**~~ — ✅ **SHIPPED 2026-05-07** (5 buckets, PRs #905/#906/#907/#908/#910). 8 sub-letters: 5 DONE + 3 PARTIAL. 2 new follow-ups (GAP-272n shape, GAP-272o wiring). First contract-first wave validation per rule §7.2 — predicted ≤2 sub-gap follow-ups vs Wave 32 v1's 8; actual = 2. ✅ Rule effective.
5. ~~**GAP-382**~~ — ✅ **DONE 2026-05-07** (PR #909): admin scan drift build-time detector wired vào `kitehub-ci.yml`; recurrence #4 prevented.
6. ~~**GAP-383**~~ — ✅ **DONE 2026-05-07** (PR #903): prune-merged-worktrees.sh handles detached HEAD.

---

**Wave 34 SHIPPED 2026-05-07 (AI Branding wizard backend cluster — first contract-first wave validation).** **Closes 8 sub-letters của GAP-272: 5 DONE + 3 PARTIAL + 2 new follow-ups** per `gap-done-discipline.md` §3. **First wave thực sự áp dụng `contract-first-for-cross-layer.md` v1.0.0** — Bucket 0 Foundation (api-contract.md UPDATE 7 endpoints + KH-frontend MSW infra) merge FIRST, sau đó A∥B∥C parallel BE, cuối D FE refactor. Outcomes: PR #905 Bucket 0 Foundation (api-contract 7 endpoints + 1 extension + MSW skeleton + msw 2.14.2 dep + setup.ts gating). PR #907 Bucket A `BrandingWizardController` + `DeployStreamController` + `RegenerateQuotaService` + `SlugAvailabilityService` + `BrandingRegenerateUsage` entity + V29 migration (Option β: branding shares subscription DB) + 17 new tests (186/186 module). PR #906 Bucket B `BrandingJobV1Controller` + `BrandingJobResponse` wrapper DTO + `QualityScoreAggregator` (deterministic v0 from real inputs, NOT stubbed) + Thymeleaf preview HTML với `X-Frame-Options: SAMEORIGIN` + `BrandColours` value object + 15 tests (181/181). PR #908 Bucket C **`InstanceLifecycleService` created from scratch** (none existed pre-Wave-34) + state machine match `ai-branding-guidelines.md` §6 + V30 migration + audit option α+γ (in-module `BrandingLifecycleEvent` table + RabbitMQ publish, không depend kiteclass `AuditLog*`) + **§6 compliance hinge enforced** (4 callsites trong `BrandingJobService` refactored → zero direct `setStatus(...)` remain) + 11 new + 4 regression tests. PR #910 Bucket D 6 hooks (`useSlugAvailability`/`useRegenerateQuota`/`useDeployStream`/`useQualityScore`/`usePreview`/`useLifecycleEvents`) + MSW handlers populate (7 endpoints × happy + error) + 3 component refactors (WelcomeStep/Step6Preview/LifecycleInline) + 17 new tests (632/632). All 5 PRs squash-merged 0/A/B/C/D sequential ngoại trừ A∥B∥C parallel batch. Wall-clock: Bucket 0 ~6min, A 10.5min ∥ B 9.4min ∥ C 11min, D 19.5min, total ~52min với coordinator CI watch + merge. **Self-test §7.2 of `contract-first-for-cross-layer.md`:** predicted ≤2 sub-gap follow-ups (vs Wave 32 v1's 8 ad-hoc) — actual = **2 new** (GAP-272n shape mismatch P2, GAP-272o orchestrator wiring P1). ✅ Rule effectiveness confirmed. **Side-task same session:** GAP-382 admin scan drift build-time detector (PR #909) shipped với self-test positive + negative case → recurrence #4 prevented; GAP-383 worktree-prune detached-HEAD bug (PR #903) shipped với 3-variant fixture self-test. Single-domain (AI Branding) wave + Phase 1 BETA pressure → audit suite required ≤3 ngày per `post-wave-audit-mandate.md` §2.1 (no milestone deferral). Worktree prune executed at closure (this PR). Counts: 167 → **169 OPEN** (5 closures 272d/h/i/j/l, 3 PARTIAL flips 272c/e/k, 4 new files 272n/o + GAP-382 closed + GAP-383 closed).

---

**Wave 33 SHIPPED 2026-05-07 (Phase 1 BETA deploy cluster — 4 P0 BLOCKING + 1 P1 STRONGLY).** **First Phase 1 BETA infrastructure wave**; closes deploy-readiness blockers per `release-1-deploy-plan.md`. **Outcomes:** PR #895 Bucket A GAP-376 ProductionSeedRunner + V27 idempotent migration + scripts/seed-production.sh wrapper + 14 new tests (407/407 module pass). PR #896 Bucket B GAP-370 beta-invite/confirmation Thymeleaf templates + EmailType enum centralization + SES bounce/complaint/rate-limit config + email-ses-setup-runbook.md + 8 new tests. PR #898 Bucket C GAP-372 BetaAccessRequest BE entity + 24h UUID token + Outbox event publish (kitehub-email subscribes via beta.invite.sent topic) + 6 REST endpoints + V28 migration + 3 FE pages (request-beta-access / beta-signup / admin/beta-requests) + 5 components + 21 BE + 10 FE tests; coordinator-applied admin scan fix on agent branch + force-push (per `feedback_admin_scan_packages_after_module_add.md` recurrence — beta package added to `KitehubAdminApplication` `@EnableJpaRepositories` + `@EntityScan`). PR #897 Bucket D GAP-369 dns-setup-runbook + GAP-379 secrets-management-runbook + ssl-cert-setup.sh + check-dns-propagation.sh + .env.production.template + .gitignore tightening (`.env.production` + `.env.staging` excluded; template allow-rule). All 4 PRs squash-merged sequentially A → B → D → C (after C admin-scan fix + Docker registry 502 transient rerun). Standards grounded per `release-deploy-standard.md` v1.0.0 (AWS Well-Architected + Twelve-Factor + DORA + OWASP + NIST + CNCF + VN PDPL + Luật An ninh mạng 2018 + Decree 53/2022). **5 GAPs flipped 🔵 OPEN → 🟡 PARTIAL** (GAP-376/370/372/369/379 — production execution + SES domain verification + domain registration + AWS Secrets Manager provisioning all user-executed steps per `gap-done-discipline.md` §3 PARTIAL exit ramp). 68th consecutive 0-clarification streak (4 agents 0-clarif). Wall-clock parallel ~30 min (longest path Bucket C 30min) + ~10 min coordinator CI fix + merge + ~10 min closure. **Side-PR #899 same session:** Wave 34 (AI Branding wizard backend cluster — 8 sub-letters 272c-l + first contract-first wave per `contract-first-for-cross-layer.md` v1.0.0 §7.2 self-test forward-looking → real validation) PIPELINED-drafted **6th consecutive** `wave-pack-planner` §Step 5.5 application (waves 28→29→30→31→32→33→34). Multi-domain Wave 33 (BE + FE + Docs) → **NO domain-milestone deferral**; audit suite required ≤3 ngày per `post-wave-audit-mandate.md` §2.1. Worktree prune deferred to closure-PR-merge (this PR) per `post-wave-cleanup.md` §2. Counts: 167 → **167 OPEN** (5 PARTIAL flips, 0 closures, 0 new gaps).

**Previous Wave 32 REWORK SHIPPED 2026-05-07 (AI Branding Wizard v2 Direction C 6-step refactor — Opus rerun after Sonnet v1 audit-flagged scaffold-as-DONE pattern).** **Closes Wave 32 v1 audit findings** (PRs #883/884/885 wontfix-rework, replaced by #889 Bucket B + #888 Bucket C + #890 Bucket D + #892 collision recovery + GAP-272h tech debt + this Phase B closure PR — orchestrator wiring `(customer)/branding/wizard/page.tsx` replaces 4 BucketPlaceholders với real `dynamic()` imports for AudienceStep/ToneStep/TemplateStep/Step6Preview + adapter callbacks for Bucket B's `onNext(selected)` payload signature). **Outcomes vs Sonnet v1:** 4/4 buckets pass §3.4 verification gate (Sonnet v1: 0/3 + Bucket B missing entirely); 0 mock-as-implementation surfaces remaining (Sonnet v1: MOCK_TAKEN_SLUGS, MOCK_BRAND, STUB_JOB_ID hardcoded inline); 0 cross-bucket scope leak (Sonnet v1: AudienceStep+ToneStep stubs in Bucket A); tests pass local before push (Sonnet v1: 2 TS2307 import-depth fails on CI). 11 sub-letter follow-ups filed under GAP-272 umbrella for the remaining backend endpoints + MSW infrastructure deferred (per `gap-done-discipline.md` §3 PARTIAL exit ramp). **GAP-272 stays 🟡 PARTIAL** (orchestrator wired + 4 step components shipped; backend endpoints GAP-272c/d/e/h/i/j/k/l + MSW setup tracked separately for Wave 34 cluster). Wizard tests: **67/67 pass** + tsc clean + `pnpm build` clean. Side-PR #882 same session: Wave 33 Phase 1 BETA deploy cluster plan PIPELINED-drafted (5th consecutive `wave-pack-planner` §Step 5.5 application — confirms PIPELINED-by-default pattern across Wave 27→28→29→30→31→32). Worktree prune deferred to post-Phase-A merge per `post-wave-cleanup.md` §2 ("DO NOT prune until all bucket PRs merged" — Phase A meta-update agent currently in-flight in `agent-ac27b829` worktree). Counts: 167 → **167 OPEN** (no new GAPs filed in closure; sub-letters 272c-l filed Wave 32 mid-flight #891).

**Previous Wave 31 SHIPPED 2026-05-06 (Phase 4 kit port — KH pro v2 production port — 4 page-cluster buckets + plan + closure, 6 PRs + Wave 32 plan side-PR).** **Second Phase 4 kit shipped**; first `@kite/shared-ui` consumer ở KH side. **Outcomes:** PR #880 Bucket A KH foundation primitives + customer dashboard home (16 tests; Decision B duplicate Wave 30 KC primitives với storage key re-namespace `kitehub.dashboard.theme`; ⌘K palette mounted ở `(customer)/layout.tsx` cho mọi customer page; subscription health KPIs derived từ existing `useOwnerInstances()`, sparkline real endpoint TODO inline). PR #877 Bucket B billing (7 tests / 492 total; G6 InvoiceDetail dynamic import + G5 PaymentMethodSelector tier upgrade flow + formatVNCurrency cho 3 KPI tiles; mock invoices KH-shape vì backend `/api/v1/subscription/invoices` chưa có — follow-up). PR #876 Bucket C branding hub (3 tests / 59 total; G11 ThemePreview integration + 6-template gallery + quota counter widget; wizard CTA placeholder pre-existing route — Wave 32 wires internals). PR #879 Bucket D instances + settings (18 tests / 509 total; G9 InstanceLifecycleStatus list compact pill + detail full timeline; mock client-side vì `/api/instances/{id}/status` chưa có — swap-site comment ghi sẵn; settings notifications + locale toggle thêm vào AccountTab). All 4 PRs squash-merged clean (0 conflicts — B/C/D không touch `_shared/dashboard-foundation/types.ts`, học bài Wave 30). Final KH frontend: **509/509 tests pass** + build clean. **GAP-270 stays 🟡 PARTIAL** (foundation + 4 clusters shipped; remaining visual regression baseline GAP-270b + E2E test GAP-270c + backend `/api/instances/{id}/status` endpoint GAP-270d). Hotfix CI #877: 1 transient Maven Central 403 trên `surefire-junit-platform:pom:3.5.5` — rerun job xanh. 67th consecutive 0-clarification streak (4 agents 0-clarif). Wall-clock parallel ~13.5 min (longest path Bucket A 13min) + ~5 min coordinator merge + ~10 min closure. **Side-PR #878 same session:** Wave 32 plan PIPELINED-drafted trong khi Wave 31 4 agents in-flight (4th consecutive `wave-pack-planner` §Step 5.5 application — pattern stable across waves 28→29, 29→30, 30→31, 31→32). Counts: 167 → **167 OPEN** (no GAPs filed/closed; follow-ups 270b/c/d to file as needed).

**Previous Wave 30 SHIPPED 2026-05-06 (Phase 4 kit port start — KC pro v2 production port — 4 page-cluster buckets + plan + closure, 6 PRs + Wave 31 plan side-PR).** **First Phase 4 kit shipped**; first `@kite/shared-ui` production consumer at dashboard scope. **Outcomes:** PR #871 Bucket A KC foundation primitives (21 tests; ThemeProvider next-themes wrapper + ⌘K Radix Dialog palette + KPICard + Sparkline pure SVG + canvas confetti vanilla; NO new deps; scope adjustment `(dashboard)/page.tsx`→`/overview/page.tsx` per Next.js route-group conflict với `(public)/page.tsx`). PR #872 Bucket B classes+courses (13 new tests / 643 total; DragDropList HTML5 DnD primitive; G4 ClassScheduleManager integration smoke). PR #873 Bucket C students+teachers (8 tests; G12 BulkActionsBar wired với 4 actions + destructive Xóa via D1 ConfirmDialog identity; G1 BulkImportDropzone tab switcher trong `/students/new` — presentational stub, real CSV parse → follow-up). PR #874 Bucket D billing+settings+branding (10 new tests / 640 total; G6 InvoiceDetail + formatVNCurrency trong billing detail; G10 PaymentStatusTimeline; G11 ThemePreview tab trong settings; branding gateway CTA → `/branding/wizard` placeholder; small follow-up: `PaymentTimelineState` không export via shared-ui barrel — locally derived). All 4 PRs squash-merged after coordinator-resolved 2 `_shared/dashboard-foundation/types.ts` conflicts (B + D both stubbed local fallback when A hadn't pushed; A canonical accepted via `git checkout origin/main -- path` explicit override since rebase theirs/ours semantics inverted). Final KC frontend: **682 tests pass / 206 skipped / 0 failures** + build clean. **GAP-266 stays 🟡 PARTIAL** (foundation + 4 clusters shipped; remaining visual regression baseline GAP-266b + E2E test GAP-266c + bundle size verify GAP-266d + drag-drop persistence). 66th consecutive 0-clarification streak (4 agents 0-clarif). Wall-clock ~10-17 min/agent parallel (Bucket D heaviest at 17min — 4 shared-ui integrations + 4 page modifications) + ~15 min coordinator merge resolution + ~10 min closure. **Side-PR #870 same session:** Wave 31 plan PIPELINED-drafted during Wave 30 Bucket A in-flight (3rd consecutive `wave-pack-planner` §Step 5.5 application — pattern stable across waves 28→29, 29→30, 30→31; codify as DEFAULT not optional). Counts: 167 → **167 OPEN** (no GAPs filed/closed; follow-ups 266b/c/d to file as needed).

### Release Plan Progress (per `feedback_wave_closure_release_progress_report.md`)

**Current Phase:** 🟢 **Phase 1 BETA ACTIVE** (P1 prospects + P2 SaaS owners; v0.9.0-beta target).

**Phase 1 BETA milestone status (per `release-1-plan-2026.md` §3 + §9):**

| Cluster | Status | Notes |
|---|---|---|
| **PDPL Phase 2 compliance** | ✅ **DONE** (Waves 23-26) | Cookie consent + DSAR + DPIA + DPO + MPS A05 + audit chain — closes PDPL hard-deadline 2026-07-01 |
| **Track 2 shared component lib** | ✅ **DONE** (Waves 27-29) | 12/12 G* + D1 ConfirmDialog + ConsentBanner shipped to `@kite/shared-ui`. ~265+ tests baseline. Domain-milestone audit deferred (Storybook + visual regression pending). |
| **Track 2 Phase 4 kit ports** | 🟡 **3 of 7 SHIPPED** (Waves 30-32+34) | KC pro v2 ✅ DONE (Wave 30); KH pro v2 ✅ DONE (Wave 31, GAP-270 PARTIAL); ai-branding-wizard (GAP-272) FE refactor Wave 32 + backend cluster Wave 34 ✅ — 5 sub-letters DONE (272d/h/i/j/l) + 3 PARTIAL (272c/e/k) + 2 new follow-ups (272n/o); GAP-272 parent stays 🟡 PARTIAL until 272f/g test deliverables + 272m Advanced Mode persistence + 272n/o close. teacher (GAP-268) tier P3; K-12 trio (GAP-267/269/271) deferred Phase 3 |
| **Phase 1 BETA P0 BLOCKING deploy** | 🟡 **PARTIAL** (Wave 33 + 37 SHIPPED 2026-05-07) | GAP-369 DNS runbook + GAP-370 email templates + GAP-372 beta-invite flow + GAP-376 production seed runner (Wave 33) + GAP-395..397 Terraform AWS Singapore (Wave 37) + GAP-398..402 Docker release pipeline (Wave 37) — **code/runbook/scripts/Terraform/Dockerfile shipped**; user-executed steps remaining (domain registration, SES production approval, AWS Secrets Manager provisioning, first-deploy seed execution, terraform apply, ECR push) |
| **Phase 1 BETA P1 STRONGLY** | 🟡 **PARTIAL** (Wave 33 + 37 + 38 + 43 SHIPPED) | GAP-371 CDN + **GAP-373 status page ✅ DONE Wave 43** (Better Stack live `https://kite-platform.betteruptime.com/` 2026-05-07) + GAP-374 tag-CI + GAP-377 smoke (✅ Wave 26) + GAP-378 rollback (✅ Wave 25) + GAP-379 secrets (🟡 Wave 33) + GAP-380 staging + GAP-403..406 E2E (Wave 37); **GAP-369 DNS** rescope Phase 2 (Wave 43 — free `*.vercel.app` + `*.betteruptime.com` accepted Phase 1; custom domain deferred Phase 2 trigger). Remaining: Cloudflare account, terraform apply, notification channel |
| **Phase 1 BETA account-prep checklist** | 🔵 **OPEN** (GAP-394 filed 2026-05-07) | 4 missing runbooks blocking first-deploy execution: AWS account creation + domain registrar + password manager policy + KiteHub superadmin first-login. ~6h docs work, no agent spawn needed |
| **Wave 37 Release-Hardening 5-layer** | 🟢 **SHIPPED** (2026-05-07) | 22 GAP-NEW: 8 DONE / 14 PARTIAL. AUDIT_DEFER_DOMAIN_MILESTONE per §2.4 |
| **Wave 38 P1 STRONGLY cluster** | 🟢 **SHIPPED** (2026-05-07) | 4 GAP-PARTIAL (GAP-371/373/374/380 all user-action deferred). Side-PR #947 release-1-deploy-runbook DRAFT pending review → Phase 0 plan refresh blocker for user-actions |
| **🚨 Phase 0 plan refresh** | 🔴 **BLOCKING** | `release-1-deploy-plan.md` §1.1+§2.2 stale (Oracle Cloud) post-ADR-025. PR #947 runbook draft pending review → after merge spawn Phase 0 agent (~30 min docs PR) → user starts Phase 1 user-actions |
| **K-12 LEGAL Phase 1B/1C** | 🟡 PARTIAL (Waves 18-19, 23-24) | Phase 1A+1B+1C v1 shipped; counsel review queued (Phase 3 trigger gate) |
| **Cloud strategy: Oracle → AWS switch** | ✅ **DONE** (ADR-025 ACCEPTED 2026-05-07) | Oracle Cloud Always Free signup fail (reject rate ~50% VN); switched Phase 1 BETA to AWS Singapore Free Tier thuần. Oracle artifacts archived `documents/07-archived/oracle-deploy-2026/`. Compliance debt accepted Phase 1 invite-only; Phase 3 trigger gate = counsel review OR migrate VN cloud. Follow-up: AWS terraform completeness audit (EKS vs ECS Fargate; 2GB RAM partitioning) |

**PDPL deadline countdown:** **2026-07-01** (~7-8 tuần from 2026-05-07).

**Phase 1 BETA launch trigger gate** (per `release-1-plan-2026.md` §11.1):
- ✅ 12/12 G* shipped (Wave 27-29)
- 🟡 3 of 7 Track 2 kit ports shipped (Waves 30-31, ai-branding wizard Wave 32+34); ~4 kits remaining (with K-12 trio deferred Phase 3 — effective ~1 critical: teacher GAP-268 P3)
- 🔴 Production deploy infra — Wave 33 PARTIAL; **4 NEW P0 from audit (GAP-384/385/386/387) BLOCKING**
- ⏳ 10-20 invite-only beta tenants
- 🔴 Quality audit /100 ≥80 baseline — **2026-05-07 audit weighted avg ~74** (UI 76% + Business 78% + API 72% + Security 72% + Ops 50%)
- 🔴 0 P0 incidents 2 tuần — **4 NEW P0s phát hiện 2026-05-07 (admin auth, PDPL consent, quality threshold, beta metrics)**

**Estimated remaining đến Phase 1 BETA launch:** **~5-8 tuần** (4 kit-port waves Wave 31-34 + 1-2 deploy-cluster waves + 1 beta-tenant onboarding wave). Aligns với `release-1-plan-2026.md` Phase 1 BETA window 9-12 tuần (currently in week 1-2).

**Recommended next pick:**
- **Option α:** Wave 31 KH pro v2 port (plan PR #870 ready) — continues kit port momentum, validates cross-app shared-ui pattern (KC ✅ → KH 2nd consumer)
- **Option β:** Phase 1 BETA P0 deploy cluster (GAP-369 DNS + GAP-370 email + GAP-372 beta tenant + GAP-376 prod seed) — critical path for actual BETA launch; agents prepare runbooks/scripts/terraform, user executes credentials
- **Option γ:** Wave 32 ai-branding-wizard (GAP-272, 28 screens) standalone — highest-quality kit, complex 6-step + Enterprise Advanced + Quality Gate widget

Khuyến nghị α nếu muốn drumbeat momentum + validate cross-app pattern; β nếu sắp launch cần deploy artifacts trước.

---

**Wave 29 SHIPPED 2026-05-06 (Track 2 Phase 3 final — port last 4 G* G1/G9/G11/G12 — 4 buckets + plan + closure, 6 PRs).** **Closes G* portion of GAP-273 (12/12 G* shipped post-Wave-29).** All 7 Phase 4 kit ports (GAP-266..272) unblocked from G* dependency standpoint. **Outcomes:** PR #867 Bucket A G1 BulkImportDropzone (31 tests = 11 component + 20 utils; CSV parse with UTF-8 BOM + VN names + quoted fields; phone `0\d{9,10}` + dd/mm/yyyy validation; root-container drag handlers fix; 6th synthetic `'error'` state for upload failures). PR #864 Bucket B G9 InstanceLifecycleStatus (28 tests = 12 component + 16 utils; state machine matches `ai-branding-guidelines.md` §6 verbatim — 6-state union with frozen TRANSITION_GRAPH adjacency map + STATE_VISUAL lookup map per design-patterns.md §3.3 no-switch-cascades; FAILED→GENERATING retry path explicitly tested). PR #865 Bucket C G11 ThemePreview (23 tests = 13 component + 10 utils; W3C WCAG 2.1 luminance formula verbatim; suggestFix deterministic AA-compliant; **reflexive coverage red→green cycle asserted** — component renders ITS OWN failing input + warns + auto-fixes on demo surface). PR #866 Bucket D G12 BulkActionsBar (15 tests; sticky `top|bottom|none` configurable; cross-component re-use D1 ConfirmDialog with identity preserved via re-export `G12.ConfirmDialog === D1.ConfirmDialog`; closed enum `BulkAction = 'EXPORT_CSV'|'ARCHIVE'|'ASSIGN'|'DELETE'` for TS exhaustiveness). All 4 PRs squash-merged after coordinator-resolved 2 additive `index.ts` conflicts (C + D after A+B merged first). Final shared-ui state: **~265+ tests** (241 from Bucket A merge baseline + Wave 29 net) + type-check clean. **GAP-273 stays 🟡 PARTIAL** — G* portion 12/12 done but Storybook/demo route + production ≥105/128 verification + visual regression baseline + D2..D10 dialogs (only D1 shipped Wave 28) remain. 65th consecutive 0-clarification streak (4 agents 0-clarif). Wall-clock ~7-11 min/agent parallel + ~12 min coordinator merge resolution (cleaner than Wave 28's ~25min — no new deps + no worktree contamination) + ~10 min closure. **Side-PR #868 same session:** Wave 30 plan PIPELINED-drafted during Wave 29 Bucket A in-flight (second formal `wave-pack-planner` §Step 5.5 application — pipeline pattern stable across 2 consecutive waves). Counts: 167 → **167 OPEN** (no GAPs filed/closed). **Recommended next pick:** **Wave 30 Phase 4 kit port start** (GAP-266 KC pro v2 — 4 page-cluster buckets, plan PR #868 ready; first production consumer of `@kite/shared-ui` at dashboard scope, validates adoption pattern). HOẶC **Phase 1 BETA P0 deploy cluster** (GAP-369 DNS + GAP-370 email + GAP-373 status page + GAP-379 secrets — critical path for actual BETA launch).

---

**Wave 28 SHIPPED 2026-05-06 (Track 2 Phase 3 — port 4 G* + D1 ConfirmDialog — 5 buckets + plan + closure, 7 PRs).** Teacher kit GAP-268 + student kit GAP-269 fully unblocked post-Wave-28. **Outcomes:** PR #856 Bucket A G3 Gradebook Entry Grid (35 tests = 21 utils + 14 component; spec deviation flagged correctly — HTML proto authoritative `1 decimal max`, not briefing's `0.25 step`). PR #857 Bucket B G4 Class Schedule Manager (20 tests = 11 utils overlap + 9 component; `detectConflicts` half-open `[start, end)` so back-to-back NOT conflict per VN convention). PR #858 Bucket C G8 Attendance Calendar (27 tests = 15 utils + 12 component; FULL ship — no deferred core; 30-day rolling streak, week-start Monday, arrow-key nav, prefers-reduced-motion). PR #860 Bucket D G10 Payment Status Timeline (10 tests; cross-component `formatVNCurrency` re-use confirmed via identity test `G10.formatVNCurrency === G6.formatVNCurrency` — no copy-paste drift). PR #859 Bucket E D1 ConfirmDialog Radix port (10 tests + `@radix-ui/react-dialog ^1.1.15` workspace dep; API parity with existing `kiteclass-frontend/src/components/ui/confirm-dialog.tsx`; 3 callsites stay UNTOUCHED, migration → follow-up gap). All 5 PRs squash-merged after coordinator-resolved 4 additive `index.ts` conflicts (B/C/D/E all touched same alphabetical export region after A merged first). Final shared-ui state: **210/210 tests** (108 baseline + 102 new) + type-check clean. **GAP-273 stays 🟡 PARTIAL** (8/12 G* shipped post-Wave-28: Wave 27 = G2+G5+G6+G7, Wave 28 = G3+G4+G8+G10; plus 1 D* = D1. Remaining 4 G* = G1+G9+G11+G12 → Wave 29). 64th consecutive 0-clarification streak (5 agents 0-clarif). Wall-clock ~7-13 min/agent parallel + ~25 min coordinator merge resolution + ~10 min closure. 2 agents flagged worktree absolute-path contamination (Bucket D + E) — same `feedback_worktree_absolute_path_contamination.md` recurrence; main worktree had leakage, coordinator stashed clean before merges. Counts: 167 → **167 OPEN** (no GAPs filed/closed). **Side-PR #861 same session:** `post-wave-audit-mandate.md` v1.1.0 — Domain-Milestone Audit Cadence (codifies user-requested workflow: cluster member waves defer audit to milestone via `AUDIT_DEFER_DOMAIN_MILESTONE` trailer; milestone wave runs full audit + `DOMAIN_MILESTONE_AUDIT` trailer; net STRICTER than per-wave §2.1). Side-PR #855: Wave 29 plan PIPELINED-drafted during Wave 28 agents (first formal §Step 5.5 application). **Recommended next pick:** **Wave 29 Track 2 Phase 3 final** (port last 4 G* G1/G9/G11/G12 — closes G* portion of GAP-273 AND triggers Track 2 milestone audit per new v1.1.0 rule; PR #855 plan ready). HOẶC Phase 4 kit ports start (all 7 kits unblocked).

---

**Wave 27 SHIPPED 2026-05-06 (Track 2 Phase 2 — port 4 priority shared-ui components — 4 buckets + plan + closure, 6 PRs).** First Track 2 component port wave per umbrella plan §Phase 2; D1 Confirm Dialog deferred Wave 28 Phase 0 (no formal spec). **Outcomes:** PR #848 Bucket A G2 Attendance Roster (12 tests; 5 spec states + 2 lifecycle; status cycle P/V/M/L; sticky save bar). PR #851 Bucket B G6 Invoice Detail + VN currency utils (27 tests = 16 utils + 11 component; `formatVNCurrency` + `formatVNTax` exported; `Intl.NumberFormat('vi-VN')` + manual `đ` suffix + U+2212 minus; print-friendly). PR #849 Bucket C G5 Payment Method Selector (9 tests; 5 methods VNPAY/MOMO/ZALOPAY/BANK/CASH per spec — agent caught spec authoritative vs briefing's 6-method claim, `QR` is a state not a method, deferred richer payment-flow composite). PR #850 Bucket D G7 Parent Invite Flow (13 tests = 6 pure validateEmail + 7 component flow; default channel ZALO_OA per dossier 02 ~95% VN parents; full a11y `role="alert"`/`role="status"`/`role="radiogroup"`; clipboard fallback). All 4 PRs squash-merged after coordinator-resolved index.ts additive merge conflicts (B/C/D all touched same alphabetical export region). 4 conflicts predicted + resolved. Final shared-ui state: **108/108 tests** (47 ConsentBanner baseline + 61 new) + type-check clean. 4 G* of 12 ported; **GAP-273 stays 🟡 PARTIAL** (4/12 G* shipped; remaining G1/G3/G4/G8/G9/G10/G11/G12 + ~10 D* dialogs → Wave 28+). Wall-clock ~7-8min/agent parallel (vs ~50min plan estimate — much faster than expected for component port scope). 63rd consecutive 0-clarification streak (4 agents 0-clarif). Counts: 167 → **167 OPEN** (no gaps closed/filed at wave; GAP-273 stays PARTIAL). 4 follow-up items tracked in plan §7 closure protocol — to file as gaps in this closure: shared-ui dedicated CI workflow (meta-P1 candidate), cross-app smoke test dev demo route, D1 Confirm Dialog spec creation (Wave 28 Phase 0 prerequisite), visual regression baseline. **Recommended next pick:** **Wave 28 Track 2 Phase 3** (4 components × 1 wave-pack — pick from G1/G3/G4/G8/G9/G10/G11/G12 by persona priority; D1 spec creation as Phase 0). HOẶC **Release Lần 1 deploy artifacts cluster** (GAP-379 secrets + GAP-372 beta tenant + GAP-371 CDN, BE+infra). HOẶC **Phase 1 BETA P0 BLOCKING** (GAP-373 status page + GAP-369 DNS prod + GAP-370 email transactional).

---

**Wave 26 SHIPPED 2026-05-06 (PDPL Phase 2 close-out + smoke test wave-pack — 3 buckets + plan + closure, 5 PRs + 1 hotfix).** Closes PDPL 2023 Phase 2 entirely before 2026-07-01 hard-deadline (~7-8 weeks). **Outcomes:** PR #844 Bucket A GAP-353c 🟡 PARTIAL (DSAR self-service form: V26 `dsar_ticket` Flyway + 10 Java files in `com.kitehub.subscription.dsar` package — entity/enums/repo/service/controller/cron/dto + 2 FE pages `(public)/legal/data-rights/page.tsx` × KH+KC + shared `DataRightsForm.tsx` + honeypot anti-spam + admin scan packages updated + `BR-PDPL-DSAR-001..005` 5-attribute rules; 10/11 AC; 393 BE tests + 491 FE tests; +follow-up `GAP-353c-followup-dpo-email-notification` P2 for `kitehub-email` cross-module integration). PR #841 Bucket B **GAP-353d 🟢 DONE** (DPIA Phase 2 documentation skeleton: 3 new BRD docs `dpo-designation.md` 165 LOC + `dpia.md` 237 LOC + `mps-a05-registration-check.md` 226 LOC = 628 LOC total, with full 5-attribute frontmatter per `business-logic-review.md` §2; cross-link updates in `privacy-policy.md` §2 + §13; `00-brd/README.md` index entries added; 8/8 AC; skeleton-as-deliverable explicit per gap §"Why P2"). PR #842 Bucket C **GAP-377 🟢 DONE** (smoke test extension: `scripts/smoke-test.sh` 265→383 LOC; 18 assertions = 11 baseline + 7 new (3 legal pages + 2 auth substitutes /login + /register + 1 KH `/api/health` + 1 ConsentBanner body-contains) + build-info echo; dual-URL support `<KH-url> <KC-url>` 0/1/2-arg with backward-compat; CI `Post-deploy smoke test` step in `deploy-staging.yml` consuming `vars.STAGING_KH_URL`/`STAGING_KC_URL`; latent BODY-buffer bug fixed by refactoring to file-based; +follow-up `GAP-377-followup-auth-route-checks` P3 for route-substitution re-alignment when `/auth/signup` + `/auth/request-beta-access` routes ship). PR #843 hotfix shipped parallel — removed 2 unused imports in `TenantContextFilterTest.java` flagged by IDE post-Wave-25 merge (1 file +0/-2; 5/5 tests pass). 0 merge conflicts. 62nd consecutive 0-clarification streak (3 agents 0-clarif; first-spawn token-quota-hit doesn't count — agents barely ran before limit reset). Wall-clock ~50min parallel (longest Bucket A 17min, B 8min, C 8min) + closure 10min. Token cost ~970k for 3 wave agents. Counts: 167 → **167 OPEN** (-GAP-353d closed; -GAP-377 closed; +GAP-353c-followup-dpo-email-notification +GAP-377-followup-auth-route-checks; GAP-353c stays in PARTIAL pool). **Recommended next pick:** **Wave 27 Track 2 FE start** (GAP-273 12-component port to `@kite/shared-ui` ADR-024, 3 buckets G1-G4/G5-G8/G9-G12 ~3 weeks; gates 7 kit gaps GAP-266..272). HOẶC **Release Lần 1 deploy artifacts cluster** (GAP-379 secrets management + GAP-372 beta tenant invite + GAP-371 CDN setup, 3-bucket BE+infra wave-pack, ~25-35h). HOẶC continue Phase 1 BETA-readiness (GAP-373 status page + GAP-369 DNS production + GAP-370 email transactional, 3-bucket P0 BLOCKING wave-pack).

---

**Wave 25 SHIPPED 2026-05-06 (PDPL Phase 2 + Critical Infra wave-pack — 3 buckets + plan + closure, 5 PRs).** State-check on the 6 ROADMAP-recommended gaps eliminated GAP-117 (PARTIAL Phase 1+2 already shipped PR #632), GAP-204 (PARTIAL, only Dependabot auto-PRs left), GAP-115 (depends GAP-111 monitoring infra) → substituted GAP-378 rollback runbook as Bucket C. **Outcomes:** PR #836 Bucket C **GAP-378 🟢 DONE** (rollback runbook 11 sections — 7-step procedure both Helm + Docker-compose paths, per-component specifics FE/BE/AI Branding/Email/Payment/DB pg_restore, 6 communication templates VN+EN, 10-item validation checklist, 6-step recovery flow, smoke test integration cross-link to GAP-377 forward reference, cross-link added at top of `release-1-deploy-plan.md` §5). PR #838 Bucket A GAP-353b 🟡 PARTIAL (server consent API in `kitehub-subscription` + V25 `consent_record` Flyway + 3 REST endpoints + `useConsent` hook API sync extension + visitor_id LocalStorage + DR-03 retention cron 36mo; 8/11 AC; 384/384 BE tests + 47/47 FE tests; +follow-up `GAP-353b-followup-multi-device-and-audit-chain` for cross-browser Playwright + hash-chain audit-log table + TestContainers IT). PR #837 Bucket B GAP-114+116 🟡 PARTIAL (combined logging stack: parent-pom logstash-logback-encoder 8.0 + 6 Java classes in `kitehub-platform` shared `com.kitehub.shared.logging` package — `PIIScrubber` + `PIIScrubberConverter` + `Redact` + `RedactSerializer` + `TenantContextFilter` + `RabbitMQTenantInterceptor` + `LoggingAutoConfiguration` + 8 `logback-spring.xml` per service + `documents/05-guides/operations/logging-standard.md`; 24/24 unit tests; 5/6 + 5/6 AC; +follow-up `GAP-116-followup-existing-code-pii-audit` for grep-based scan of existing log callsites). **Coordinator-applied 3 fixes to Bucket B before merge:** (1) pinned `logstash-logback-encoder` version=8.0 inline in `kitehub-platform/pom.xml` because installed pom resolves out-of-context (parent dependencyManagement doesn't transit through consumer modules at consume time → "invalid POM, transitive deps unavailable"); (2) added `@ConditionalOnClass(SecurityContextHolder.class)` to `LoggingAutoConfiguration.ServletConfig` so services without spring-security-core (`kitehub-subscription` has only spring-security-crypto) skip filter wiring instead of throwing `NoClassDefFoundError`; (3) added direct logstash dep to `kiteclass-gateway/pom.xml` since standalone module lacks transitive path through platform. 1 rebase conflict (B vs main ROADMAP §🚀 Next Action additive — coordinator resolved). 61st consecutive 0-clarification streak (Bucket A/B/C agents all 0-clarif; coordinator-fix iterations don't count against agents). Counts: 167 → **167 OPEN** (-GAP-378 closed; +GAP-353b-followup-multi-device-and-audit-chain +GAP-116-followup-existing-code-pii-audit; GAP-353b/114/116 PARTIAL pool unchanged). **Recommended next pick:** **Wave 26 PDPL Phase 2 close-out** (GAP-353c DSAR self-service form + GAP-353d DPIA docs, 2-bucket wave-pack ~10-14h combined; closes PDPL Phase 2 entirely before 2026-07-01 hard-deadline ~7-8 weeks). HOẶC **Track 2 FE start** (GAP-273 12-component port to `@kite/shared-ui` ADR-024, 3 buckets G1-G4/G5-G8/G9-G12 ~3 weeks; gates 7 kit gaps GAP-266..272). HOẶC pick GAP-377 smoke test script (P1 STRONGLY recommend, ~1 day, sister gap to GAP-378 just shipped).

---

**Wave 24 SHIPPED 2026-05-06 (K-12 LEGAL Phase 1C v1.5 remainder — 3 buckets + plan + closure + parallel Release Lần 1 plan, 6 PRs).** GAP-359 + GAP-360 + GAP-361 stay 🟡 PARTIAL covering 8 of 18 sub-tasks across 3 K-12 LEGAL gaps. **Outcomes:** PR #824 Bucket A GAP-359 v1.5 (V57 retention 7y migration + RetentionLifecycleService daily cron 2am + AuditChainVerificationCron daily cron 2:30am + Micrometer counter `child_protection.audit.chain.break` + audit-chain-break-runbook + BR-CHILD-PROTECT-008/009; 41 tests). PR #825 Bucket B GAP-360 v1.5 (SubjectGradeService state machine DRAFT→REVIEWED→PUBLISHED EnumMap + IllegalGradeTransitionException 409 + bulk-publish endpoint max 500 + SubjectGradeAllPublishedListener Outbox routing `kiteclass.k12.grades.all-published` + api-contract.md filled + BR-GRADEBOOK-006/007/008; 19 tests; ArchUnit deferred — workspace dep missing). PR #826 Bucket C GAP-361 v1.5 (consent gate × 4 facets transcript/attendance/conduct/notifications + 5/5 facets check version stale → RECONSENT_REQUIRED + ConsentService.bulkBumpVersion + ParentConsentAdminController bulk-bump @PreAuthorize ADMIN/PRINCIPAL/OWNER + BR-PARENT-PORTAL-014/015/016; 107 tests). **PR #827 Release Lần 1 Plan 2026** shipped parallel (605 lines, 3-phase rollout: P1+P2 soft launch 9-12 weeks → P3 +4-6 weeks → K-12 post-counsel +8-12 weeks). **Decision context locked:** solo dev, no counsel engaged, risk tolerance Moderate ("v1 pending counsel review" disclaimer OK non-K-12), Track 2 Option α full 8 ports Phase 1 confirmed. **Total MVP timeline: ~21-30 calendar weeks ≈ 5-7 tháng.** 60th consecutive 0-clarification streak (58→60 across 3 buckets). 1 merge conflict (Bucket C messages.properties additive với Bucket A retention keys — coordinator resolved). Counts: 167 → **167 OPEN** (no GAPs filed/closed; sub-task ACs ticked). **Recommended next pick:** **Wave 25 PDPL Phase 2 + Critical infra start** per Release Lần 1 Plan §9 — GAP-353b server consent API + GAP-114/115/116 logging stack + GAP-117 restore drill + GAP-204 npm CVE sweep (3-bucket BE wave-pack, ~45-60min). HOẶC Wave 25 Track 2 shared lib + first FE wave-pack (GAP-273 + GAP-274/275/276/277, FE-heavy 4-bucket).

---

**Wave 23 SHIPPED 2026-05-06 (PDPL 2023 legal compliance wave-pack — 4 buckets + plan + closure, 6 PRs).** GAP-353 P0 LEGAL closure (PDPL effective 2026-07-01, ~7-8 weeks countdown). 4 disjoint buckets parallel after simulation-gap-finder cluster pass surfaced GAP-368 (production legal pages) as hard dependency. **Outcomes:** PR #821 Bucket F **GAP-368 🟢 DONE** (6 production legal pages `/legal/{privacy,terms,cookies}` × KH+KC + new `documents/00-brd/cookie-policy.md` BRD doc, both `pnpm build` ✅). PR #819 Bucket BC GAP-353 L2+3 (ConsentBanner React in `packages/shared-ui/src/components/ConsentBanner/` + production integration both `(public)/layout.tsx`; 27 component tests + 21 layout tests; both builds ✅; LocalStorage MVP + WCAG AA + state machine NOT_PROMPTED→PROMPTED→{CONSENT_GIVEN|REJECTED}→REVOKED→RE_PROMPTED). PR #816 Bucket A GAP-353 L1 (4 `BR-PDPL-CONSENT-001..004` full 5-attribute per `business-logic-review.md` in canonical `kitehub/marketing/rules.md` + KC cross-link, citing PDPL Art 11-13 + Decree 13/2023 Art 13/17/18). PR #818 Bucket E GAP-353 L5 (`kitehub-story-v2/screens/consent-banner.html` mockup 110/128 self-rescore + G14 dossier entries × KC+KH + GAP-274/275/350 AC updates). **GAP-353 🔵 OPEN → 🟡 PARTIAL** per `gap-done-discipline.md` §3 (Phase 1 shipped; Phase 2 → 3 follow-up gaps). **3 follow-up gaps filed at closure:** GAP-353b (P1 — server consent API + audit-log link, ~12-16h), GAP-353c (P2 — DSAR self-service intake form per PDPL Art 14, ~6-8h), GAP-353d (P2 — DPIA Decree 13/2023 Art 24-30 docs, ~4-6h, P2 until subject count nears 100k threshold). 57th consecutive 0-clarification streak. Counts: 165 → **167 OPEN** (-GAP-368 closed; -GAP-353 stays-PARTIAL not counted closed; +GAP-353b/c/d). **Recommended next pick:** **GAP-353b + GAP-353c pair-wave** (server consent API + DSAR form, ~18-24h combined; closes PDPL Phase 2) OR continue K-12 Phase 1C remainder (GAP-359/360/361, ~3-4 weeks). Release Lần 1 launch ~3-5 weeks; PDPL hard-deadline 2026-07-01.

---

**Wave 22 SHIPPED 2026-05-06 (UI kits polish wave-pack — 3 buckets, 4 PRs).** 3-bucket scope-optimized: split GAP-364 → ship PARTIAL (school-profile rebuild only ~14h) + defer cross-screen items to GAP-364b (~23h saved from critical path). Wall-clock 35min vs 45min estimated. **Outcomes:** PR #811 Bucket A GAP-363 🟡 PARTIAL (coordinator-corrected from agent-claimed DONE per `gap-done-discipline.md` §2 — kit avg ≥105 self-rescore 102.5 unmet; payments.html Option C parent-trigger AC-FIN-001 + 4 polish items shipped, kit floor restored). PR #812 Bucket B GAP-364 🟡 PARTIAL (school-profile.html 91→107 +16). PR #813 Bucket C GAP-365 🟢 DONE (S-student.md Tier-1 431 lines, 21 ACs, 8 journey areas, verbatim AC-FIN-001 verified). **4 follow-up gaps filed at closure:** GAP-363b (P2 — kiteclass-student external re-audit + delta-to-≥105, ~10-15h), GAP-364b (P2 — kitehub-admin cross-screen polish skeletons/empty-states/dark-mode/staff-vetting/Zalo OA, ~23h), **GAP-366** (🟠 P1 Meta — `frontend-standards.md` extend kit-as-source-of-truth + dossier cross-link, surfaced by user Wave 22 Q2), **GAP-367** (🟠 P1 Meta — `quality/kit-production-parity` skill 4-layer parity check, surfaced by user Wave 22 Q3). 56th consecutive 0-clarification streak. **Track 2 ports:** GAP-269 (student) UNBLOCKED on P0 child-protection (kit avg <105 means port may need parallel polish via GAP-363b); GAP-271 (admin) STILL BLOCKED on cross-screen polish via GAP-364b. Counts: 161 → **165 OPEN** (-GAP-365; +GAP-363b/364b/366/367; GAP-363/364 stay PARTIAL pool). **Recommended next pick:** **GAP-353 P0 LEGAL PDPL cookie banner** (effective 2026-07-01 ~8 weeks, blocks GA marketing surfaces) — pair-eligible 2-bucket meta wave with GAP-366+GAP-367 (kit-as-source-of-truth + parity skill, ~25-32h combined, gates Track 2 Phase 2 component churn). OR continue K-12 Phase 1C remainder (GAP-359/360/361, ~3-4 weeks). Live demo: https://victoraurelius.github.io/2026-Kite-Class-Platform/.

---

**Wave 20 + 21 SHIPPED 2026-05-05 (UI kits review + storytelling) — 8 PRs same session, parallel wave-pack pattern.** Wave 20 (Round 3 review): kiteclass-student external avg **100.4/128** (delta -15.6 vs self-report 116, calibration band ✓; payments.html persona AC-FIN-001 child-protection violation P0) + kitehub-admin external avg **101.1/128** (delta -6.1 — kit had explicit WCAG ratios + MoET citations; calibration smaller-than-typical band). GAP-348 → 🟡 PARTIAL; 3 follow-up gaps filed: **GAP-363** (P1 BLOCKING — kiteclass-student polish), **GAP-364** (P2 — kitehub-admin polish), **GAP-365** (P2 BL — file Tier-1 `S-student.md` AC doc). Wave 21 (single-bucket): `kitehub-story-v2` Direction A marketing storytelling kit shipped (avg 109.8/128 self-report, GAP-350 🟢 DONE; unblocks GAP-275 source-of-truth). PR #801 mcp-first v1.1.0 (3-tier hierarchy MCP→Glob/Grep/Read→Bash) + PR #808 design-system Pages auto-deploy enabled (push: main on `ui_kits/**`). 53rd consecutive 0-clarification streak. Counts: 159 → **161 OPEN** (-GAP-350 closed; +GAP-363/364/365). **Recommended next session pick:** **Wave 22 polish wave-pack candidate** (GAP-363 P1 BLOCKING + GAP-364 P2 + GAP-365 P2 BL — 3 disjoint buckets ~50-65h total, unblocks Track 2 ports GAP-269/271) OR continue K-12 Phase 1C remainder (GAP-359/360/361, ~3-4 weeks). Track 2 Phase 4 ports BLOCKED on Wave 22 polish. Live demo: https://victoraurelius.github.io/2026-Kite-Class-Platform/ (auto-deploys now).

---

**Wave 19 SHIPPED 2026-05-05** — K-12 LEGAL Trio Phase 1C v1 + Parent Portal Phase 1B Remainder wave-pack (4 buckets, 9 PRs). Bucket A GAP-322c 🟡 PARTIAL (mandatory reporting banner + audit-log foundation + V54 + IncidentVisibilityScope) → GAP-359 follow-up. Bucket B GAP-323c 🟡 PARTIAL (SubjectGrade extension + GradeFormulaService TT 22/2021 + V55 + multi-subject-gradebook 3-layer docs) → GAP-360 follow-up. Bucket C GAP-321c 🟡 PARTIAL (PDPL Decree 13/2023 granular consent + V56 + ConsentService + complaint write) → GAP-361 follow-up. Bucket D GAP-321b-1-conduct 🟢 DONE (ParentConductFacet real-wired with visibility-scope filter). Hotfix PR #798 dropped deprecated `JSX.Element` annotation (React 19 compat). Workflow rename PR #799 (`Frontend CI` → `KiteClass Frontend CI` symmetry). 50-streak 0-clarification milestone hit (47-50 across 4 buckets). PC-restart agent kill incident — salvage closure agents recovered 50 uncommitted files cleanly. **Recommended next:** Phase 1C remainder follow-ups (GAP-359 child protection retention + pen test + 4-level escalation + full UI + cron + 111 webhook; GAP-360 multi-subject-gradebook state machine + UI + per-component CRUD; GAP-361 parent-portal 3 write actions + 4 facet gates + i18n EN/zh-CN + settings UI + re-consent admin tooling) — UI + state machine + Tổ trưởng workflow scope, ~3-4 weeks combined. Stage 1 K-12 GA estimate ~10-14 weeks remaining (was 12-16; Wave 19 burned down ~2 weeks).

| Gap | Phase 1A status | Phase 1B status | Phase 1C |
|---|---|---|---|
| GAP-321 Parent Portal | 🟡 PARTIAL — transcript route + scope guard PDPL | GAP-321b 🟡 PARTIAL — 4 read-only facets + audit log skeleton + V53 (Wave 18b2 Bucket C); GAP-321b-1-conduct ✅ DONE Wave 19 Bucket D 2026-05-05 (ParentConductFacet real-wired with visibility-scope filter); GAP-321b-1-fees + GAP-321b-1-notifications still follow-up | GAP-321c 🟡 PARTIAL — v1 SHIPPED Wave 19 Bucket C 2026-05-05 (PDPL Decree 13/2023 granular consent + V56 + ConsentService + ParentComplaint write + fees facet gated); 3 remaining write actions + 4 remaining facet gates + i18n EN/zh-CN + settings UI + re-consent admin tooling → GAP-361 |
| GAP-322 Child Protection | 🟡 PARTIAL — Incident + AES-256 + safeguarding role | GAP-322b 🟡 PARTIAL — Vetting service + state machine + AES-256 + MinIO storage stub + RBAC + V52 (Wave 18b2 Bucket B); LLTP upload UI + verify queue UI + concrete MinIO SDK + 111 webhook follow-up | GAP-322c 🟡 PARTIAL — v1 SHIPPED Wave 19 Bucket A 2026-05-05 (banner + audit-log + listener + V54 + visibility_scope + 5-attribute rules); retention enforcement + pen test + 4-level escalation + full UI + cron + 111 webhook → GAP-359 |
| GAP-323 Period Attendance | 🟡 PARTIAL — AttendancePeriod + tenant.vertical_type | GAP-323b 🟡 PARTIAL — Phase 1B v1 backend (#769) + mobile UI v1 tap-grid + bulk actions (#771 Bucket A); offline queue / Playwright perf / matview / concurrent load test / parent-portal facet exposure follow-up | GAP-323c 🟡 PARTIAL — v1 SHIPPED Wave 19 Bucket B 2026-05-05 (SubjectGrade extension + GradeFormulaService TT 22/2021 HALF_EVEN scale=1 + V55 + 3-layer multi-subject-gradebook docs + 12 unit + 3 IT env-gated); state machine + gradebook UI + per-component CRUD + Tổ trưởng workflow → GAP-360 |

**Wave 18b3 SHIPPED 2026-05-04** — all 3 Phase 1B remainder buckets merged: ~~GAP-347 (meta)~~ ✅ #775. ~~GAP-323b offline + k6~~ shipped #780 (status 🟡 PARTIAL — PWA background-sync, conflict UI, queue LRU follow-up). ~~GAP-322b LLTP + MinIO SDK~~ shipped #782 (status 🟡 PARTIAL — resumable multipart, virus scan, audit on upload to Phase 1C). ~~GAP-321b 3 facet wiring~~ shipped #781 PARTIAL (fees real-wired; conduct + notifications stay v1 stubs, 3 sub-gaps filed: GAP-321b.1-fees-instalment-payment-history P2 + GAP-321b.1-conduct-incident-visibility P1 + GAP-321b.1-notifications-engine-wiring P1 hard-blocked by GAP-063b). Phase 1C = ~2-3 weeks. Stage 1 K-12 GA estimate ~12-16 weeks remaining (was 14-18; Wave 18b3 burned down ~1-2 weeks).

5-stage K-12 program (Q3 2026 → Q3 2027 GA) in [P5 review §Stage 1-5](../../00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md).

**Track 2 (UI kits production port)** — Phase 1 ADR + workspace scaffolding DONE (PR #713 merged 2026-04-30). Phase 2-6 (15 OPEN gaps GAP-266..280) — multi-week roadmap detailed in [`documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md`](../../03-planning/waves/wave-track-2-ui-kits-port-umbrella.md). Trigger Phase 2 (5 priority components G2/G6/G5/G7/D1) khi MVP-essential blockers từ Wave 17 review findings cần real components.

**Dependabot pre-MVP lock** — closed 4 failing PRs (#715/#716/#717/#718), restricted weekly bumps to patch-only via PR #731 (merged 2026-04-30). Resume condition: post-Release Lần 1 launch (~4-6 weeks) per GAP-283.

**Production deploy estimate** — MVP soft launch ~4-6 weeks; GA ~8-12 weeks; full Track 2 production-grade UI ~10-14 weeks. See most recent persona AC ROADMAP analyses below for breakdown.

---

**2026-05-05 (Wave 19 Bucket D — GAP-321b-1-conduct DONE — stacked on Bucket A):** Wave 19 K-12 LEGAL Phase 1C v1 Bucket D shipped 🟢 DONE — `ParentConductFacetServiceImpl` real-wired against `IncidentRepository.findVisibleForParentList` consuming Bucket A's `IncidentVisibilityScope` enum + V54 column (PARENT_VISIBLE + PUBLIC scopes only; STAFF_ONLY excluded by JPQL). Hạnh kiểm rating projected coarsely from `Incident.severity` (LOW→TỐT / MEDIUM→KHÁ / HIGH→TRUNG_BÌNH / CRITICAL→YẾU) until digital rating store ships. Encrypted `description` never projected — `title` surfaces as `remark`. Existing v1-stub regression `staffOnlyIncidentEquivalent_notExposedToParent` flipped to ArgumentCaptor verifying service requested only PARENT_VISIBLE + PUBLIC scopes; new IT `ParentConductFacetEntityGraphIT` asserts assertSelectCount ≤3 + STAFF_ONLY exclusion against TestContainers Postgres. BR-PARENT-FACET-CONDUCT-002 in `documents/01-business/kiteclass/parent-portal/rules.md` flipped from "stub stays" → "real wiring with visibility-scope filter" citing BR-CHILD-PROTECT-005. **GAP-321b-1-conduct flipped 🔵 OPEN → 🟢 DONE** per `gap-done-discipline.md` §2 (all 8 AC checked, no banned phrases, no deferral). Stacked PR — base = Bucket A's branch, will rebase to main after #793 merges. **Counts:** 161 → **160 OPEN** (GAP-321b-1-conduct closed).

---

**2026-05-05 (Wave 19 Bucket A — GAP-322c Phase 1C v1 SHIPPED PARTIAL via salvage):** Wave 19 K-12 LEGAL Phase 1C v1 Bucket A shipped 🟡 PARTIAL via salvage-agent recovery after PC-restart killed the original Bucket A worker. Salvaged uncommitted worktree (5 modified + 13 new files) and verified via `mvn clean verify` on `kiteclass-core`. Ships: `IncidentVisibilityScope` enum (4 values STAFF_ONLY default), `ChildProtectionAuditLog` entity + Repository + Service hash-chain (SHA-256 chain per `(instance_id, entity_type)`), `IncidentReportingController` with `POST /api/v1/incidents/{id}/mandatory-report-ack`, `IncidentTransitionListener` (after-commit on CRITICAL+abuse-category), `V54__add_incident_visibility_scope_and_audit_log.sql` (visibility_scope DEFAULT STAFF_ONLY backward compat + child_protection_audit_log table + REVOKE DELETE), `IncidentBanner.tsx` FE (warning + acked states), 28 unit + 1 IT tests. BR-CHILD-PROTECT-005..007 in `documents/01-business/kiteclass/child-protection/rules.md` with full 5-attribute frontmatter (Source/Rationale/Reviewer/Compliance/Cadence per `business-logic-review.md`). UC-INCIDENT-CRITICAL-REPORT in `use-cases.md`. **GAP-322c flipped 🔵 OPEN → 🟡 PARTIAL** per `gap-done-discipline.md` §3 PARTIAL exit ramp. **GAP-359** filed for Phase 1C remainder (retention enforcement column + soft-delete block, pen test + remediation, AC-COMM-006 4-level escalation depending GAP-339, full UC-INCIDENT-CRITICAL-REPORT page UI, daily hash-chain integrity cron, MOLISA Tổng đài 111 webhook Stage 2 Q4 2026). K12_ENTERPRISE tier flag REMAINS DISABLED until GAP-322b + GAP-322c FULL + legal counsel sign-off via GAP-156. **Counts:** 160 → **161 OPEN** (+GAP-359; GAP-322c stays in PARTIAL pool counted with 'b' siblings).

---

**2026-05-05 (Wave 19 Bucket C — GAP-321c v1 SHIPPED 🟡 PARTIAL):** Salvaged from pre-WSL-restart agent work; coordinator closure agent verified + finalized. Ships PDPL Decree 13/2023 Art 16 granular consent infrastructure: V56 additive migration (`parental_consent` JSONB + `parent_complaint_queue` table) + ParentalConsent typed record + ConsentService (fail-safe deny on missing key) + ParentConsentController (GET + PUT `/api/v1/parent/consent`) + ParentComplaintController v1 (`POST /api/v1/parent/complaints` with scope guard) + ParentFeesFacetServiceImpl gated end-to-end (returns 403 `PARENT_CONSENT_REQUIRED`) + BR-PARENT-PORTAL-011..013 in `documents/01-business/kiteclass/parent-portal/rules.md` with 5-attribute frontmatter (Source PDPL Decree 13/2023 Art 16 + Luật Giáo dục 2019 Đ.83 K2; Compliance Compliant; Annual + event-driven cadence) + UC-PARENT-CONSENT-MANAGE + UC-PARENT-COMPLAINT-FILE. Verification: `mvn test` 6 targeted tests `BUILD SUCCESS`. Status flip 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — fees facet wired end-to-end + 1 of 4 write actions live; remaining work tracked in **GAP-361-parent-portal-phase-1c-remainder** (3 write actions + 4 remaining facet gates + i18n EN/zh-CN + settings UI + re-consent flow admin tooling).

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

**2026-05-05 (Wave 21 SHIPPED — GAP-350 Marketing Storytelling kit, 2 PRs):** Single-bucket agent built `documents/02-architecture/design-system/ui_kits/kitehub-story-v2/` from Round 1 baseline (archived JSX 546 LOC → HTML kit per Round 2/3 standard). 6 scroll-driven sections (hero 113 / parallax-features 110 / before-after 108 / một-ngày-chu-trung-tam 112 / mock-dashboard 109 / pricing-cta 107) avg **109.8/128** self-report (target ≥105 ✓; sits between Round 2 baselines 107.8 and 110.5 — honest scoring per `feedback_audit_calibration.md`). Static HTML + vanilla JS (NO React/Babel), token-themed via `_shared/colors_and_type.css`, KH sky+orange brand. ARIA slider + aria-live + `prefers-reduced-motion` + 3 viewports + realistic VN mock data (Trung tâm Toán Master, MoMo/VNPay/ZaloPay, NĐ 123/2020). Trust signal honest framing "Mục tiêu Q4 2026" per `business-logic-review.md` §2.1. Round 1 baseline preserved in `_v1-baseline/`. **GAP-350 🔵 OPEN → 🟢 DONE** per `gap-done-discipline.md` §2 (all 11 ACs verified). **GAP-275** (Track 2 KH public marketing port) source-of-truth unblocked. Counts: 162 → **161 OPEN** (-GAP-350). Wave 20 + Wave 21 both shipped same session in parallel (different scopes — `audits/` + `gaps/` for Wave 20 vs `ui_kits/kitehub-story-v2/` for Wave 21). Wall-clock Wave 21 ~12min agent + ~15min closure = ~27min.

**2026-05-05 (Wave 20 SHIPPED — GAP-348 Round 3 UI Kits Persona-Driven Review, 4 PRs):** Plan PR #802 + Bucket A PR #803 (kiteclass-student external avg 100.4/128, delta -15.6 vs self-report 116, calibration band ✓; APPROVE WITH POLISH; payments.html persona AC-FIN-001 child-protection violation P0) + Bucket B PR #805 (kitehub-admin external avg 101.1/128, delta -6.1 vs self-report 107.2 — unusually small per kit's explicit WCAG ratios + MoET citations + realistic VN K-12 mock data; APPROVE WITH POLISH; school-profile.html 91 below floor) + Closure PR (this). **GAP-348 → 🟡 PARTIAL** per `gap-done-discipline.md` §3 (polish work deferred to follow-ups, not yet executed). **3 follow-up gaps filed:** GAP-363 (P1 BLOCKING — kiteclass-student polish, payments persona violation + 4 partials), GAP-364 (P2 — kitehub-admin polish, school-profile rebuild + 5 medium-priority items), GAP-365 (P2 BL — file Tier-1 `S-student.md` AC doc currently absent; persona AC infrastructure gap surfaced by Bucket A). **Track 2 Phase 4 ports BLOCKED:** GAP-269 (student) on GAP-363 + GAP-365; GAP-271 (admin) on GAP-364. Counts: 159 → **162 OPEN** (+GAP-363/364/365; GAP-348 PARTIAL not counted closed; Wave 22 row added to queue). Wall-clock ~3-4h end-to-end. 29th consecutive 0-clarification wave-pack. **Wave 21 (GAP-350 marketing storytelling kit) ALSO ACTIVE** — single-bucket agent in flight in parallel; Wave 21 closure pending agent ship.

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
- **GAP-353** (**P0**, LEGAL) — PDPL 2023 cookie/consent banner in KH+KC marketing kits (Platform Admin × Discovery × C6). 0 hits "PDPL"/"cookie banner" in GAP-274/275/350. PDPL effective 2026-07-01 ~8 weeks; Release Lần 1 launch ~4-6 weeks precedes effective date.
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

**GA Blockers remaining: 3 — ordered per `meta-gap-priority.md` (meta before feature within P0).**

(Synced 2026-05-09: GAP-016 + GAP-014 closures from 2026-04-26/27 finally reflected in this table; row count 5 → 3 active.)

| # | Gap | Title | Type | Status | Effort |
|:-:|-----|-------|:----:|:------:|:------:|
| 1 | **GAP-223** | AI Branding migration verification governance — Sub-PR 223.1 SHIPPED 2026-04-26 (skill + audit-gate rule + §11.4 + baseline 62/100); Sub-PR 223.2 = GAP-006 ⏸ DEFERRED 2026-04-28 (Ollama + Docker stack required) | 🔴 P0 Meta (governance) | 🟡 PARTIAL | Sub-PR 223.2 ⏸ DEFERRED |
| 2 | ~~GAP-222a~~ | ~~Extract Outbox infra to shared lib~~ — superseded by ADR-021 per-module pattern; closed via GAP-222a Phase 2 + GAP-222b + GAP-222c (all DONE 2026-04-26) | 🟠 Meta (infra) | ✅ DONE | — |
| 3 | ~~GAP-016~~ | ~~Living docs impact scope (3-layer sweep)~~ — DONE 2026-04-26 (§2.9 business-gap-check audit 16/20 ✅; remaining diagram drift split to GAP-234) | 🔴 Meta (docs contract) | ✅ DONE | — |
| 4 | GAP-011 | Template library curation (30 templates) | Feature | 🟡 PLANNED | L |
| 5 | ~~GAP-014~~ | ~~Wave mock plan include AI branding~~ — DONE: planning portion 2026-04-26 (wave plan §7 v2-aligned); implementation portion via GAP-235 DONE 2026-04-27 (4 sub-PRs E1/F/E2/G shipped #588-#590 + closure) | Feature | ✅ DONE | — |
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
| 16 | ~~Wave 20 — GAP-348 Round 3 UI Kits Persona-Driven Review~~ | GAP-348 (P1) → GAP-363 (P1 BLOCKING) + GAP-364 (P2) + GAP-365 (P2 BL) | P1 (pre-port quality gate) | ✅ SHIPPED 2026-05-05 — 4 PRs (#802 plan + #803 Bucket A + #805 Bucket B + closure this PR). **Bucket A** `kiteclass-student` external avg **100.4/128** (delta -15.6 vs self-report 116, calibration band ✓; APPROVE WITH POLISH; payments.html 92 child-protection persona AC-FIN-001 violation P0). **Bucket B** `kitehub-admin` external avg **101.1/128** (delta -6.1 vs self-report 107.2 — unusually small per kit's explicit WCAG ratios + MoET citations + realistic VN K-12 mock data; APPROVE WITH POLISH; school-profile.html 91 below floor). **Bucket C closure** (this PR): GAP-348 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 (polish work deferred to follow-ups, not yet executed); 3 gaps filed (GAP-363/364/365); ui_kits/README Round 3 row synced; Track 2 Phase 4 ports GAP-269/271 BLOCKED until polish closes. ~3-4h wall-clock end-to-end. 29th consecutive 0-clarification wave-pack. |
| 17 | ~~Wave 21 — Marketing Storytelling kit~~ | GAP-350 (P2 — promoted from P2 LOWER 2026-05-05 per user request) | P2 | ✅ SHIPPED 2026-05-05 — 2 PRs (#807 single-bucket agent kit build + closure this PR). Single-bucket: built `ui_kits/kitehub-story-v2/` from Round 1 baseline (archived JSX 546 LOC → HTML kit per Round 2/3 standard). 6 scroll-driven sections (hero 113 / parallax-features 110 / before-after 108 / một-ngày-chu-trung-tam 112 / mock-dashboard 109 / pricing-cta 107) avg **109.8/128** self-report (target ≥105 ✓). Direction A marketing storytelling for P2 Center Owner trial-signup conversion. **GAP-350 → 🟢 DONE** per `gap-done-discipline.md` §2 (all 11 ACs verified). Static HTML + vanilla JS, KH sky+orange brand, ARIA + reduced-motion + 3 viewports + realistic VN mock data. Round 1 baseline preserved in `_v1-baseline/`. **GAP-275 source-of-truth unblocked** — Track 2 KH public marketing port now references this kit, not raw archive. Closure batched landing card + README Status row + GAP-350 status flip + GAP-275 cross-link sequentially after Wave 20 Part C (no file overlap). Counts: 162 → 161 OPEN. |
| 18 | **Wave 22 — Round 3 polish (post-Wave-20 follow-up)** | GAP-363 (P1 BLOCKING) + GAP-364 (P2) + GAP-365 (P2 BL) | P1+P2 | 🔵 OPEN 2026-05-05 — Wave-pack candidate (3 buckets disjoint: A=kiteclass-student polish ~12-14h, B=kitehub-admin polish ~32-42h, C=Tier-1 S-student.md AC doc ~6-8h). Filed by Wave 20 Bucket C from external review findings. **Track 2 ports GAP-269/271 BLOCKED until this wave closes** — recommended kick-off after Wave 21 ships to maintain marketing+polish flow. |

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
| **GAP-470** | netty-epoll 4.1.133 → 4.2.13 line bump for revised CVE-2026-42577 (Wave 57 GAP-468 follow-up) | 🟠 P1 | S |

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
