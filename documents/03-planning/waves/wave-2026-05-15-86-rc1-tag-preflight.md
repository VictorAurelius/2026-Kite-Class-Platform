---
title: Wave 86 — v1.0.0-rc.1 Tag Preflight + Final Audit Suite + Beta Cohort Invite
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 86
waves: [86]
risk_profile: HIGH (release candidate tag → production traffic; downstream cohort invite irreversible)
trigger: Wave 85 multi-tenant + performance baseline CLOSED; pre-launch hardening checklists ALL satisfied
estimated_wall_clock: 24-30h (was 14-20h pre Bucket A integration; +10h cho 21 AC additions + 4 P0 BLOCKERS + 17 NEW gaps file)
bucket_a_audits: [2026-05-15-pre-wave-86-persona-outside-in.md, 2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md, 2026-05-15-pre-wave-86-simulation-3axis.md]
---

# Wave 86 — v1.0.0-rc.1 Tag Preflight + Beta Cohort Invite

## 1. Brainstorm

**Q1 (goal — 4-source completeness per `inside-out-completeness-trigger.md` §3):**

Final pre-launch checklist execution + tag `v1.0.0-rc.1` + invite first 5 beta tenants. v1.0.0-rc gate = pre-launch hardening checklists all satisfy (`pre-launch-{auth,secrets,owasp-rest,infra,dependency}-hardening-checklist.md`) + post-wave audit suite ≥80 across all categories + dev self-test full 126 rows complete.

**Inside-out scope từ 4 sources (per `inside-out-completeness-trigger.md` §3 Bước 1-5):**

| Source | Items |
|---|---|
| **1. ROADMAP §🚀 canonical** | GAP-440 Spring Boot bump, GAP-537c P2/P3 user manual, GAP-412 AWS Activate resubmit, GAP-372 beta cohort invite mechanism, GAP-373 status page, GAP-377 smoke test script, GAP-378 rollback procedure, GAP-379 secrets management 90d rotation |
| **2. inside-out-queue.md** | Wave 86 consolidation pre-rc1; carry-forward GAP-576 P0 gateway auth 404, GAP-574 P1 PM2 bugs, GAP-257 P0 restore drill, GAP-144 P1 AlertManager receivers |
| **3. Inside-out audit (3 background agents Bucket A)** | 3 audit artifacts shipped PR #1432: persona-outside-in (5×5 matrix, 10 NEW gap proposals) + benchmark-vn-saas-edu (10 industry Qs, 7 NEW gap proposals) + simulation-3axis (28 cells sampled, 3 NEW gap proposals + 21 AC additions) — converged 4 P0 BLOCKERS |
| **4. Outside-in NEW (4-source convergence)** | 17 NEW gaps GAP-582..598 surface previously-uncovered scope (Cloudflare cache magic-link, cookie consent PDPL, invite email content audits, onboarding wizard step-count, admin bounce visibility, OAuth idempotency, RDS storage alarm, retention framework) |

**4 P0 BLOCKERS converged (must address before tag rc1):**
1. **GAP-584 Magic-link Cloudflare cache bypass** (sim cell 19 + Bucket E.4 AC) — cross-tenant invite redirect leak risk; chặn Bucket G invite
2. **GAP-585 Cookie consent PDPL Decree 13 granular** (benchmark Q7 + Bucket E.6 AC) — compliance + first-impression damage; chặn Bucket E pass
3. **GAP-144 AlertManager receivers wired BEFORE invite** (sim cell 16 H-AC4 + Wave 84 P1 escalation → P0 Wave 86) — chặn Bucket H monitoring premise; without alerts MTTR target <2h impossible
4. **Spring Boot bump regression suite Bucket B (AC1+AC2+AC3 to GAP-440)** — Spring Boot 3.5.x bump regression risk (@Async / webhook idempotency / heap baseline) cascades chặn Bucket G invite

**Q2 (decision context):** Wave 81-85 đã ship backend production + FE rebuild + RLS + performance + ops baseline. Wave 86 = consolidation wave — không feature mới, chỉ audit + tag + invite. Spring Boot dep bump GAP-440 ship cùng (last opportunity before tag). User manual P2/P3/Admin GAP-537c ship cùng (beta tenants cần). AWS Activate $1k credit resubmit GAP-412 (denied 2026-05-10 due to kitehub.vn stale ref Wave 81 fixed).

**Q3 (risks):**
- Tag prematurely → production traffic on un-hardened backend
- Beta cohort invite < ready → first-impression damage
- Dep bump introduce regression → revert + re-tag overhead
- AWS Activate denial #2 → financial cushion gone
- Outside-in audit per rule §3 — wave touch beta user (P2/P3 manager) UX path → SHOULD trigger outside-in coverage

## 2. Task Breakdown

| Bucket | Item | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A** | Outside-in audit (persona-based-business-review + benchmark + simulation) | parallel agents | 1h | First |
| **B** | GAP-440 Spring Boot 3.5.14 → latest dep bump + smoke test | coordinator | 2-3h | After A |
| **C** | GAP-537c P2 Owner + P3 Manager screenshots capture + Tier 2 annotation (defer from Wave 79/80) | coordinator + Playwright | 2-3h | Parallel B |
| **D** | GAP-412 AWS Activate $1k credit resubmit (kitehub.me URL post-Wave 81 fix) | user-action AWS Console | 30min | Parallel |
| **E** | Final pre-launch hardening 5 checklist verification — each script + manual sweep | coordinator | 3-4h | After A-D |
| **F** | Tag `v1.0.0-rc.1` + automated release CI workflow trigger | user-action + coordinator | 1h | After E |
| **G** | First 5 beta cohort invite (manual = Solo teacher P1 ×2 + Center Owner P2 ×3 from invite waiting list) | user-action + coordinator verify | 1-2h | After F (DNS warm) |
| **H** | Post-cohort-invite monitoring + first-incident response plan ready | coordinator | 2h | After G |

## 3. Scope — Bucket detail

### Bucket A — Outside-in audit

- `persona-based-business-review`: 5 personas × 5 questions focusing on first-30-min experience
  - Anonymous prospect → "What stops me converting?"
  - P1 Solo Teacher invited → "First class setup confusion?"
  - P2 Center Owner invited → "Onboarding wizard feels overwhelming?"
  - P3 Manager invited by Owner → "Permissions feel right?"
  - Platform Admin → "Audit log coverage feels safe?"
- WebSearch benchmark: VN SaaS edu industry signup conversion + churn studies (Zalo OA, Misa, Edupia)
- `simulation-gap-finder`: failure modes during first-100-user load (DB connection pool exhaust, email queue backlog, etc.)

### Bucket B — GAP-440 Spring Boot bump (P0 BLOCKER — Bucket G gated on B pass)

- Check latest Spring Boot 3.5.x patch release (security fixes only, no 3.6.x jump pre-launch)
- Pin `pom.xml` parent + dependencies; run mvn dependency:tree để verify transitive consistency
- Full smoke test post-bump per Wave 81 Bucket F pattern
- Dependabot vulnerability sweep: target 0 HIGH CVE per `pre-launch-dependency-hardening-checklist.md`

**AC additions từ Bucket A simulation audit (3 new):**
- [ ] **B-AC1** Verify `@Async` annotation processing post-bump — smoke test bulk-import endpoint returns 202 Accepted (not 200 sync) post-bump per sim cell 7
- [ ] **B-AC2** Webhook idempotency replay test — POST same `idempotency_key` 2× to payment webhook; 2nd request returns 200 with original payment record (per sim cell 11)
- [ ] **B-AC3** Heap baseline post-bump comparison — `jcmd <pid> VM.native_memory baseline` BEFORE bump + AFTER bump; alert if delta > 10% non-heap (per sim cell 17 GAP-502 OOM regression prevention)

### Bucket C — GAP-537c P2/P3 screenshots + onboarding audit (scope expand per Bucket A)

- Playwright capture 8 screens cho P2 Owner (signup → onboarding wizard → first class create)
- Playwright capture 6 screens cho P3 Manager (login → dashboard → invite accept)
- Tier 2 annotation overlay (Wave 79 standard) — arrows + numbered steps
- VN narrative captions per `dev-readable-doc-language.md`
- PDF render via `scripts/render-user-manual-pdf.sh p2-owner` + `p3-manager`

**AC additions từ Bucket A persona audit (3 new — scope expand từ "capture only" → "audit + capture"):**
- [ ] **C-AC1** P2 onboarding wizard step-count ≤7 verified + skip-and-resume UX (paired GAP-588; persona cell 3.3 + benchmark Q4 + Q9 sim — industry optimal 4-7 steps; >7 = 3× churn rate)
- [ ] **C-AC2** P3 first-login screen show permission matrix explicit ("Bạn có quyền: nhập điểm, điểm danh; KHÔNG có quyền: xóa lớp, sửa giá") per persona cell 4.2 trust gate
- [ ] **C-AC3** P1 first-class onboarding flow ≤5 phút verified (cell 2.3 cognitive load)

### Bucket D — GAP-412 AWS Activate resubmit

- USER ACTION: submit Activate Founders Pack application với new kitehub.me URL
- Application body cite: "Wave 81 deploy CLOSED; production live; review again"
- Expected: $1k credit + technical support tier

**AC addition từ Bucket A simulation audit (1 new):**
- [ ] **D-AC1** Application body cite "Wave 81 CLOSED + kitehub.me live + Wave 86 first 5 invited" + public kitehub.me URL trong application body cho AWS reviewer verification (sim cell 26)

### Bucket E — Pre-launch hardening verification (2 P0 BLOCKERS — E-AC4 + E-AC6)

- `pre-launch-auth-hardening-checklist.md` Cat 4 — all 9 items PASS
- `pre-launch-secrets-hardening-checklist.md` Cat 2 — all 8 items PASS (KMS CMK PARTIAL acceptable v1)
- `pre-launch-owasp-rest-hardening-checklist.md` Cat 3 — all REST best practices PASS
- `pre-launch-infra-hardening-checklist.md` Cat 5 — all 9 items PASS (CSP report-only acceptable v1)
- `pre-launch-dependency-hardening-checklist.md` Cat 1 — 0 HIGH CVE PASS
- Each checklist: script run + grep evidence + manual sweep result trong audit report

**AC additions từ Bucket A audits (6 new — 2 P0 BLOCKER):**
- [ ] **E-AC1** Magic link TTL = 24h documented trong auth checklist Cat 4 row 7; FE displays "Link expires in X hours" countdown; resend endpoint rate-limited 5/hour/email (paired GAP-590 P1 — sim cell 5 + benchmark Q5)
- [ ] **E-AC2** Redis `maxmemory-policy=allkeys-lru` row added to infra checklist Cat 5 — sweep verify (sim cell 10 — prevent cache stampede + RDS spike)
- [ ] **E-AC3** Tenant-switch flow row added to auth checklist Cat 4 — JWT swap + cache invalidate verified (sim cell 18 + `pre-handoff-self-test-completeness.md` §2.7)
- [ ] **🚨 E-AC4 P0 BLOCKER (paired GAP-584)** Magic-link endpoints (`/auth/magic`, `/auth/invite/*`) explicitly bypass Cloudflare cache via Page Rule OR `Cache-Control: no-store, no-cache, max-age=0` header — chặn Bucket G invite cho đến khi verify (sim cell 19 cross-tenant invite redirect leak risk)
- [ ] **E-AC5** Bulk-import endpoint cap = 1000 rows/request (HTTP 413 if exceeded); FE chunk client-side if > 1000 (sim cell 23)
- [ ] **🚨 E-AC6 P0 BLOCKER (paired GAP-585)** Cookie consent banner PDPL Decree 13 compliance — granular consent per purpose (analytics/marketing/functional split) + no dark pattern (no pre-checked boxes, equal-weight Accept/Reject) + withdraw mechanism trong footer settings + consent log retained ≥3 năm; self-test `curl -sI https://kitehub.me/ | grep -i 'set-cookie'` không có analytics cookie SET trước user explicit accept (benchmark Q7 compliance + first-impression damage)
- [ ] **E-AC7** Landing P95 mobile-3G <3s (Lighthouse mobile profile evidence) — performance baseline gate per persona cell 1.1 + perf 81/100 audit gap mobile-3G target explicit

### Bucket F — Tag v1.0.0-rc.1

- `git tag -s v1.0.0-rc.1 -m "Release candidate 1 — Phase 1 BETA — first 5 cohort invite"`
- Per `versioning-policy.md` §3
- Per `release-deploy-standard.md` §3.4 first PRODUCTION = MAJOR scope; 18-item checklist verified
- Automated release CI workflow `release.yml` triggered → ECR images tagged `v1.0.0-rc.1` + Helm chart bumped
- GitHub Release với changelog + DOMAIN_MILESTONE_AUDIT trailer per `post-wave-audit-mandate.md` §2.4.2

**AC additions từ Bucket A simulation audit (2 new):**
- [ ] **F-AC1** Release CI workflow `release.yml` includes step "Cloudflare cache purge after Vercel deploy" — verify CF API token trong production secrets + curl `purge_cache` for kitehub.me + kiteclass.me apex (sim cell 12)
- [ ] **F-AC2** GAP-574 PM2 status confirmed (resolved/PARTIAL với cohort-3+ deploy notation) BEFORE tag rc1; verify grep `scripts/deploy-prod.sh` for PM2 invocations; document workaround trong Bucket H runbook nếu unresolved (sim cell 20)

### Bucket G — Beta cohort invite (5 tenants) — gated on E-AC4 + B + H-AC4

- 5 hand-picked tenants từ invite waitlist (VN edu trainers):
  - 2 Solo Teacher P1 (English center freelance)
  - 3 Center Owner P2 (small center 50-100 students)
- Send invite via Resend production (verified Wave 83 Bucket F)
- Welcome page: TOS + Privacy + Beta disclaimer + "feedback channel" link
- Coordinator monitor: first-hour signup completion rate + first-day churn rate

**AC additions từ Bucket A persona + benchmark + simulation (7 new):**
- [ ] **G-AC1** Resend send wrapper retry 3× exponential backoff; fallback AWS SES backup nếu fail; emit metric `Email/InviteFail` → SNS topic (sim cell 3 — RabbitMQ DLQ chưa wired = invite dropped silently)
- [ ] **G-AC2** OAuth callback idempotency — `oauth_attempts.state_token` UNIQUE; backend rejects duplicate state với 409 (paired GAP-582 — sim cell 4 cross-tenant orphan record prevention)
- [ ] **G-AC3** Welcome email sets support SLA expectation explicit: "Phản hồi qua support@kitehub.me, SLA <24h business hours" (sim cell 13 + benchmark Q9)
- [ ] **G-AC4** Waitlist signup auto-ACK email <2min + SLA "2-3 ngày" trong email body (persona cell 1.4)
- [ ] **G-AC5** Invite email content audit per personas (paired GAP-586 P1 + GAP-587 P1):
  - Sender `support@kitehub.me` không phải `noreply@`
  - Tone Vietnamese natural per `dev-readable-doc-language.md`
  - Có tên thật người duyệt + feedback CTA visible
  - P3 invite email body: P2 owner name + center context + role explicit
  - Link tới `/status` (Statuspage URL Wave 84 GAP-424)
  - Link welcome guide URL `/help/p1-solo-teacher` or `/help/p2-owner` per persona
- [ ] **G-AC6** P2 first-login dashboard greeting personalized "Xin chào chị Hằng" + admin contact visible header + "Bạn là 1 trong 5 trung tâm đầu tiên" social proof banner (persona cell 3.1 + 3.2)
- [ ] **G-AC7** P2 onboarding-complete trigger summary email + 30-min benchmark validate (persona cell 3.4)

### Bucket H — Monitoring + incident response (1 P0 BLOCKER — H-AC4)

- Grafana dashboard `KiteHub-Production` (from Wave 84) → on-call rotation set
- Incident response runbook `documents/05-guides/operations/incident-response-runbook.md` updated với Wave 86 deploy specific paths
- First-incident SLA: <30 min MTTD (mean time to detect) + <2h MTTR (mean time to recover)

**AC additions từ Bucket A simulation + persona + benchmark (8 new — 1 P0 BLOCKER):**
- [ ] **H-AC1** K6 load profile "cohort-50-concurrent-signup" baseline before Phase 1.5 invite; P95 signup latency target <3s (sim cell 6 — Phase 1.5 future scaling)
- [ ] **H-AC2** RDS storage alarm `RDSFreeStorageSpace < 5GB` → SNS topic `production-alerts`; runbook `documents/05-guides/operations/rds-storage-runbook.md` resize procedure (paired GAP-583 P1 — sim cell 8 silent fill prevention)
- [ ] **H-AC3** EC2-Docker-equivalent health check documented: docker-compose `healthcheck:` + `restart: unless-stopped`; verify trong Bucket E `pre-launch-infra-hardening-checklist.md` Cat 5 (sim cell 14)
- [ ] **🚨 H-AC4 P0 BLOCKER** GAP-144 AlertManager receivers wired BEFORE invite (Bucket H prerequisite, not afterwards); SNS topic → email support@kitehub.me at minimum; verify alarm reaches inbox trong self-test (sim cell 16 + 21 — silent restart loop, MTTR <2h impossible without alerts)
- [ ] **H-AC5** Audit log retention policy = 90 days hot in RDS + archive to S3 lifecycle; nightly cron DELETE WHERE created_at < NOW() - INTERVAL '90 days'; runbook `documents/05-guides/operations/audit-log-retention-runbook.md` (sim cell 24)
- [ ] **H-AC6** Phase 1.5 invite plan (≥20 tenants) includes Resend paid plan upgrade ($20/month 50k emails); runbook `documents/05-guides/account-prep/resend-paid-upgrade-runbook.md` (sim cell 25 Free Tier ceiling)
- [ ] **H-AC7** Incident response runbook dry-run trước tag rc1: simulate "email không gửi" scenario, time-to-detect + time-to-recover, document baseline để measure real T+1 (sim cell 27)
- [ ] **H-AC8** Cohort retention D7/D14/D30 tracking framework — D7 activation % của 5 tenants đã tạo ≥1 lớp / mời ≥1 student; D14 % vẫn active login ≥1× tuần; D30 % vẫn active OR có churn reason captured; dashboard hoặc spreadsheet manual cho 5 cohort; activation milestone trigger Zalo OA outreach nếu D7 <50% (paired GAP-591 P1 — benchmark Q2 70% churn xảy ra 90d đầu)
- [ ] **H-AC9** Invite email delivery P95 <5min (Resend dashboard metric) per persona cell 2.1
- [ ] **H-AC10** P1 cold-start first-class + first-attendance <30min benchmark (validate via 1 cohort tenant) — persona cell 2.4
- [ ] **H-AC11** P3 first-day daily-ops <15 phút benchmark — persona cell 4.4
- [ ] **H-AC12** Admin cohort queue load <2s + runbook section "5-tenant first-cohort 30-min workflow" — persona cell 5.1 + 5.4
- [ ] **H-AC13** Admin Resend bounce visibility + impersonate-read-only debugging path (paired GAP-589 P1 — persona cell 5.5 + benchmark incident response prereq)
- [ ] **H-AC14** First-response SLA published doc `documents/05-guides/operations/support-sla-phase-1-beta.md` + invite email cite SLA + tracking spreadsheet (paired GAP-592 P2 — benchmark Q9)

## 4. State-Check Evidence

| Symbol | Verification | Verdict |
|---|---|---|
| 5 pre-launch checklist files | `ls .claude/rules/pre-launch-*-checklist.md` | ✅ exists (5 files) |
| `release.yml` workflow | `ls .github/workflows/release.yml` | 🆕 to-be-created (or verify exists) |
| AWS Activate ticket history | `documents/05-guides/account-prep/03-aws-activate-history.md` | ✅ exists (Wave 81 denial) |
| P2/P3 screenshot scripts | `ls scripts/capture-user-manual-screenshots.sh` | ✅ exists Wave 80 |
| Beta cohort invite list | `documents/01-business/kitehub/beta-cohort/p1-cohort-list.md` | 🆕 to-be-created |

## 5. Acceptance Gate

| Criterion | Met when |
|---|---|
| **🚨 P0 BLOCKER #1** Bucket A outside-in integration | 3 audit artifacts merged ✅ (PR #1432) + 21 AC distributed B-H ✅ (this PR) + 17 NEW gaps GAP-582..598 filed ✅ (this PR) |
| **🚨 P0 BLOCKER #2** Magic-link Cloudflare cache bypass (E-AC4 + GAP-584) | Page Rule OR `Cache-Control: no-store` header verified; curl test confirms zero CF cache hit on magic endpoints; chặn Bucket G invite |
| **🚨 P0 BLOCKER #3** Cookie consent PDPL Decree 13 (E-AC6 + GAP-585) | Granular consent banner shipped + no dark pattern + withdraw mechanism + consent log ≥3 năm retention; curl self-test PASS; chặn Bucket E pass |
| **🚨 P0 BLOCKER #4** GAP-144 AlertManager receivers wired (H-AC4) | SNS topic → support@kitehub.me alarm route verified via self-test; MTTR <2h achievable; chặn Bucket H monitoring premise; chặn Bucket G invite |
| **🚨 P0 BLOCKER #5** Spring Boot bump regression suite (B-AC1+AC2+AC3 to GAP-440) | @Async preserved + webhook idempotency replay + heap baseline delta <10%; chặn Bucket G invite cascade |
| GAP-440 Spring Boot bump | dep tree clean + 0 HIGH CVE + smoke test pass + B-AC1/AC2/AC3 PASS |
| GAP-537c screenshots + audit | 14 screens captured + annotated + PDF rendered + C-AC1 wizard ≤7 steps + C-AC2 permission matrix + C-AC3 P1 ≤5 phút |
| GAP-412 AWS Activate | application submitted (outcome separate) + D-AC1 cite kitehub.me public URL |
| 5 pre-launch checklists + 7 AC additions | all categories PASS or PARTIAL với follow-up gap + E-AC1..AC7 PASS |
| `v1.0.0-rc.1` tag | tag signed + release CI workflow green + ECR images tagged + F-AC1 CF cache purge step + F-AC2 GAP-574 status verified |
| 5 beta cohort invited + 7 AC additions | invite email delivered + Resend dashboard shows opens + G-AC1..AC7 PASS |
| Incident response ready + 14 AC additions | runbook updated + dashboard live + SLA targets defined + H-AC1..AC14 PASS (H-AC4 P0 BLOCKER) |
| Post-wave audit suite | all categories ≥80 v2 format |
| 17 NEW gaps filed | GAP-582..598 all OPEN với valid frontmatter + AC + Related links per `audit-to-gap-pipeline.md` §3 |

## 6. Cross-link

- Wave 85 closure: `wave-2026-05-15-85-multi-tenant-security-perf.md`
- `release-deploy-standard.md` §3.4 (MAJOR + first PROD)
- `versioning-policy.md` §3
- `outside-in-coverage-trigger.md` §3
- 5 pre-launch hardening checklists (Cat 1-5)
- `release-1-deploy-plan.md`
- Incident response runbook

## 5. Verification Gates

See §5 Acceptance Gate table above — bucket-level criteria. Post-wave audit per `post-wave-audit-mandate.md` §2.1 (Backend/FE/Security/Performance categories) per bucket scope.

## 6. Agent Spawn Pattern

Sequential coordinator execution where buckets share files (deploy state, gateway config). Parallel background agents for isolated FE work (cookie consent banner, screenshots capture) per `agent-background-spawn-default.md` §1. Outside-in audit agents (per `outside-in-coverage-trigger.md` §3) spawn parallel background when wave triggers (Wave 85/86 mark §1 Q4).

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-wave-cleanup.md` + `post-merge-sync-completeness.md`:
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append (Rule 15)
- ROADMAP §🎯 Snapshot prepend
- gap-status.csv sync per bucket DONE flips
- `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- Session handoff `2026-05-XX-post-wave-NN-handoff.md` NEW

## 8. Log

- **2026-05-15** (Bucket A integration): integrate 3 outside-in audit artifacts (persona-outside-in + benchmark-vn-saas-edu + simulation-3axis) merged main qua PR #1432 vào Wave 86 plan. Scope expansion: 8 buckets B-H giữ nguyên tên, AC count +21 (B: 3, C: 3, D: 1, E: 7, F: 2, G: 7, H: 14 — tổng 37 ACs). **4 P0 BLOCKERS converged across 3 audits:** (1) GAP-584 magic-link Cloudflare cache bypass — sim cell 19 cross-tenant leak; (2) GAP-585 cookie consent PDPL Decree 13 granular — benchmark Q7 compliance; (3) GAP-144 AlertManager receivers wired (Wave 84 P1 escalation → P0 cohort-invite context) — sim cell 16 silent restart loop; (4) Spring Boot bump regression suite — sim cell 7+11+17 @Async/webhook/heap. 17 NEW gaps GAP-582..598 filed cùng PR (deduped audit proposals; existing GAP-575/576/577/579 từ Wave 85 PR #1426 preserved with different scope). `estimated_wall_clock` 14-20h → 24-30h (+10h scope). Per `inside-out-completeness-trigger.md` §3 — 4-source completeness shown in §1 Brainstorm Q1. Reviewer: @nguyenvankiet (solo-dev).
- **2026-05-15** (draft): Plan drafted in batch PR #1406 covering 49 Phase 1 BETA remaining gaps → v1.0.0-rc.1 roadmap. Outside-in audit per `outside-in-coverage-trigger.md` §3 — Wave 83/84 SKIP per §4 exception (bug-fix + internal ops); Wave 85/86 FIRE (user-facing security + first cohort). Sections §5-7 + §8 appended PR #1409 post wave-plan-completeness CI fail.
