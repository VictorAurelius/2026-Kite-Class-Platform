---
title: Pre-Wave 86 Simulation Gap Audit — 3-axis matrix (cohort onboard × first-day data volume × edge case)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 86
audit_type: simulation-gap-finder
related: [GAP-440, GAP-537, GAP-412, GAP-502, GAP-574, GAP-372, GAP-373, GAP-377, GAP-378, GAP-379]
sister_audit: 2026-05-15-pre-wave-85-simulation-3axis.md
---

# Pre-Wave 86 Simulation Gap Audit — 3-axis matrix (rc1 tag + first 5 beta cohort invite + first-100-user load)

## 1. Scope

Audit ngoài-vào (outside-in) thứ ba của Wave 86 Bucket A — applying `simulation-gap-finder` 3-axis matrix lên scope Wave 86 (Buckets B-H) để stress-test failure modes trong realistic Phase 1 BETA first-30-day cohort load. Wave 86 = **consolidation wave + first production traffic** — không feature mới, chủ yếu audit + dep bump + tag rc1 + invite 5 beta tenants. Mục tiêu: surface AC additions, NEW gap proposals, verify GAP-502 OOM recurrence risk + GAP-574 PM2 bug blocker under first-100-user load.

**Wave 86 baseline scope:**
- A: Outside-in audit (this audit = simulation portion of 3-agent convergence)
- B: GAP-440 Spring Boot 3.5.x dep bump + 0 HIGH CVE sweep + smoke
- C: GAP-537c P2 Owner (8 screens) + P3 Manager (6 screens) screenshots + Tier 2 annotation + PDF
- D: GAP-412 AWS Activate $1k credit resubmit (post-kitehub.me fix)
- E: 5 pre-launch hardening checklists verification (auth/secrets/owasp-rest/infra/dependency)
- F: Tag `v1.0.0-rc.1` + release CI workflow + ECR images bump + Helm chart bump
- G: First 5 beta cohort invite (2× Solo P1 + 3× Center Owner P2) via Resend
- H: Post-cohort monitoring + Grafana dashboard + incident response SLA (<30min MTTD, <2h MTTR)

**Production baseline (post Wave 85):**
- t3.small EC2 (2 GiB RAM, 2 vCPU) × 2 (kh_backend + kc_app)
- RDS db.t3.micro (1 GiB RAM, max_connections ≈ 87)
- Wave 85 deliverables ASSUMED LIVE: RLS V50-V52 enabled với B-AC1..AC8 fixes, Tier 2 config với E-AC1 MaxRAMPercentage=60, GAP-432 findAll bounded, smoke tests G-AC1..AC4
- GAP-502 OOM thrash 95% (post Wave 85 — pending 14-day observation)
- GAP-574 PM2 bug (blocker future FE deploy cohort 3+) — status TBD
- Resend production verified (Wave 83 Bucket F)
- 5 beta cohort = first production traffic; before this, zero real-tenant users

## 2. Methodology

3-axis matrix tổng 350 cells (5 × 5 × 14). Sampled STRATEGICALLY **28 cells** focus realistic Phase 1 BETA first 30 days:
- Cohort load: 5-20 tenants
- Per-tenant data: empty đến 500 students (typical small VN edu center)
- Edge cases: happy path + critical externals (email queue, OAuth, network, DB pool, S3 quota, payment webhook, monitoring cascade)

**Axis 1 — Cohort onboard concurrency:** {single, 5 sequential, 5 concurrent, 50 mass, 100 mass}
**Axis 2 — First-day data volume per tenant:** {empty, 10 student, 100 student, 500 student, 1k student}
**Axis 3 — Edge case (14):** happy / email queue backlog / OAuth retry / network timeout invite link / DB conn-pool exhaust / RDS storage near-full / S3 quota / Redis evict / payment webhook delay / Cloudflare cache stale / support overflow / K8s pod restart / log volume spike / monitoring cascade

**Sampling strategy:**
- 60% happy-path concurrency cells (5 concurrent + 50 mass) × low/mid volume — most likely first-30-day scenarios
- 25% critical-edge × realistic load (email queue, network timeout, DB pool, payment webhook)
- 15% extreme-edge sanity check (RDS storage, monitoring cascade, K8s restart)

**Baselines referenced:**
- Wave 85 simulation audit (`2026-05-15-pre-wave-85-simulation-3axis.md`) — cell 13 HikariCP cross-tenant leak, cell 5 OOM regression
- Wave 84 Ops Readiness 78/100 (CloudTrail + startupProbe + secrets rotation + cost monitoring + 4 account-prep runbooks)
- Wave 83 Security 90/100 v2 format
- GAP-502 RC1+RC2 root causes (RabbitMQ auth + OOM)
- GAP-574 PM2 historical bug pattern

## 3. Sampled cells (28)

| # | Cohort | Volume | Edge case | Scenario | Failure mode | Bucket match | AC addition |
|---|---|---|---|---|---|---|---|
| 1 | single | empty | happy | First tenant accepts invite link → completes signup → lands on `/dashboard` | Baseline path; nếu Resend invite template rendering broken (Wave 83 verified but post-bump regression risk) → user thấy raw `{{magic_link}}` placeholder | ✅ G | (none — covered, but see #3) |
| 2 | 5 sequential | empty | happy | 5 tenants invited 1-by-1 over 2 hours; each completes signup before next invited | Sequential = serialized; Resend rate limit (100/day free tier) OK; DB connection burst negligible | ✅ G | (none — covered) |
| 3 | 5 concurrent | empty | email queue backlog | 5 invite emails queued same minute via Resend production; kitehub-email service consumes queue | Nếu RabbitMQ DLQ chưa wired (GAP-144 carry-forward Wave 84) → 1 transient SES throttle = email dropped, no retry, no alert → tenant Vy không nhận invite | 🟡 G+H | **G-AC1:** Resend send wrapper kiểm tra response status; retry 3× exponential backoff; nếu fail → fall back to AWS SES backup (Wave 83 Bucket F) + emit alert metric `Email/InviteFail` → SNS topic |
| 4 | 5 concurrent | 100 student | OAuth retry | 5 P2 owners accept invite → sign up via Google OAuth; 1 hits transient 503 từ Google identity service | OAuth retry chưa codified — nếu FE flow retries automatically + BE creates duplicate user account → cross-tenant orphan record | ❌ G | **G-AC2 (NEW gap):** OAuth callback idempotency — `oauth_attempts.state_token` UNIQUE; backend rejects duplicate state với 409 → file as GAP-575 P1 |
| 5 | 5 concurrent | 100 student | network timeout invite link | Tenant Hằng's network drops mid invite-link click; magic link token expires 10 phút | Token TTL không documented trong `pre-launch-auth-hardening-checklist.md` Cat 4; nếu TTL quá ngắn (< 5 phút) → tenant nhập email lại, resend tedious | 🟡 E+G | **E-AC1:** Document magic link TTL = 24h trong checklist row 7; FE displays "Link expires in X hours" countdown; resend endpoint rate-limited 5/hour/email |
| 6 | 50 mass | empty | happy | Future scaling: 50 tenants invited Day 30 (Phase 1.5 candidate) | 50 simultaneous signup = 50 DB INSERT + 50 RLS context inits + 50 RabbitMQ welcome msg + 50 audit log rows + 50 Resend send → ~5s burst | 🟡 H | **H-AC1:** Load-test K6 profile "cohort-50-concurrent-signup" baseline before Phase 1.5 invite; P95 signup latency target <3s |
| 7 | 5 concurrent | 500 student | DB conn-pool exhaust | 3 owners bulk-import 500 students each (CSV upload); HikariCP per-service 10 connections | 3 concurrent bulk import × long-running INSERT transactions = 3 connections held 30-60s; remaining 7 servicing normal traffic; Wave 85 E-AC3 mandates async @Async — verify enforce post-bump | 🟡 B+H | **B-AC1:** Verify Spring Boot 3.5.x bump KHÔNG regress `@Async` annotation processing — smoke test bulk-import endpoint còn returns 202 Accepted (not 200 sync) post-bump |
| 8 | 5 concurrent | 100 student | RDS storage near-full | RDS db.t3.micro 20 GiB; with 5 tenants × 100 students × audit logs + email_send_audit (Wave 85 G-AC4) → ~12 GiB after 30 days → 60% threshold | Storage autoscaling DISABLED on t3.micro (cost saving); reaching 80% → manual intervention; no alarm wired Wave 84 | ❌ H | **H-AC2 (NEW):** CloudWatch alarm `RDSFreeStorageSpace < 5GB` → SNS topic `production-alerts`; runbook `documents/05-guides/operations/rds-storage-runbook.md` (resize procedure) → file GAP-576 P1 |
| 9 | 5 concurrent | 100 student | S3 quota | Tenants upload logo + first-class banners; AWS Free Tier S3 5 GB/month | First cohort cumulative ≈ 50-200 MB — well under 5 GB; nhưng nếu user re-uploads (no compression) → drift | ✅ — | (none — sufficient headroom; revisit Wave 100+) |
| 10 | 5 concurrent | 100 student | Redis evict storm | Beta cohort load triggers Redis eviction (cache-only, no critical state); maxmemory policy chưa set | Nếu `allkeys-lru` chưa configured → Redis hits maxmemory → OOM → restart → cache stampede toward RDS → P95 spike | 🟡 E | **E-AC2:** `pre-launch-infra-hardening-checklist.md` Cat 5 add row "Redis maxmemory + maxmemory-policy=allkeys-lru configured" — verify Bucket E sweep |
| 11 | 5 concurrent | 500 student | payment webhook delay | Hằng đăng ký gói Premium (Stripe webhook); webhook delayed 30s do third-party | Webhook handler idempotency (per `pre-handoff-self-test-completeness.md` §2.6 row d) — verify post Spring Boot bump | 🟡 B | **B-AC2:** Smoke test Bucket B include webhook idempotency replay (POST same `idempotency_key` 2×, second returns 200 with original payment record); regression cover |
| 12 | 5 concurrent | empty | Cloudflare cache stale | After Vercel deploy rc1, Cloudflare cache returns stale FE bundle → tenants see Wave 85 UI not Wave 86 | Cache purge step thiếu trong release CI? Wave 84 added Cloudflare runbook (GAP-394) — verify purge command in `release.yml` | 🟡 F | **F-AC1:** Release CI workflow `release.yml` includes step "Cloudflare cache purge after Vercel deploy" — verify CF API token in production secrets + curl `purge_cache` for kitehub.me + kiteclass.me apex |
| 13 | 5 concurrent | 100 student | support overflow | Beta cohort encounters first-impression bugs; all 5 file support tickets in 24h | Support channel = email-only currently (Resend support@kitehub.me); no ticketing tool; manual triage; if user expects Intercom chat → trust damage | ⚠️ G | **G-AC3:** Set explicit beta-period expectation in welcome email: "Phản hồi qua support@kitehub.me, SLA <24h" — document in `documents/05-guides/user-manual/anonymous/beta-access.md` (Wave 79) |
| 14 | 5 concurrent | 100 student | K8s pod restart mid-onboard | EC2-based deploy (not k8s in Phase 1 BETA); equivalent = systemd service restart during deploy | Phase 1 BETA uses Docker on EC2 (per ADR-025 AWS Singapore); Wave 84 startupProbe wired Helm 7/7 services — but EC2 Docker doesn't use Helm | 🟡 H | **H-AC3:** Document EC2-Docker-equivalent health check: docker-compose healthcheck + restart policy = `unless-stopped`; verify in Bucket E `pre-launch-infra-hardening-checklist.md` Cat 5 |
| 15 | 5 concurrent | 100 student | log volume spike | 5 tenants concurrent → CloudWatch Logs ingestion ~50 MB/day; Free Tier 5 GB/month | Cumulative 5 × 30 ≈ 1.5 GB/month — under Free Tier; nhưng CloudWatch retention default 14 days; longer retention costs $ | ✅ H | (none — within budget; documentented retention 14d acceptable) |
| 16 | 5 concurrent | 100 student | monitoring alarm cascade | CloudWatch alarm thresholds tuned for empty-tenant baseline; 5 tenants concurrent triggers false positives (CPU >50%, mem >70%) | Wave 84 ops audit identified GAP-144 AlertManager receivers blocking — alarms fire but no route to on-call (Slack/email/SMS not wired) | 🔴 H | **H-AC4 (P0 BLOCKER):** GAP-144 receivers wired BEFORE invite (Bucket H prerequisite, not afterwards); SNS topic → email support@kitehub.me at minimum; verify alarm reaches inbox in self-test |
| 17 | 5 concurrent | 500 student | OOM mid-tx + GAP-502 recurrence | 5 P2 owners concurrent + 3 of them bulk-import 500 students → Tier 2 config E-AC1 (MaxRAMPercentage=60) hit | Wave 85 E-AC1 should prevent OOM; nếu Spring Boot 3.5.x bump (Bucket B) introduces memory regression → GAP-502 recurrence | 🟡 B+H | **B-AC3:** Post-bump heap dump comparison — `jcmd <pid> VM.native_memory baseline` BEFORE bump + AFTER bump; alert if delta > 10% non-heap |
| 18 | 5 concurrent | 100 student | OAuth retry + tenant-switch | 1 owner Hằng has 2 tenants (her own + sister center test account); switches mid-onboarding | Per Wave 85 cell 7 + `pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch — JWT swap correct? Cache invalidated? | 🟡 E | **E-AC3:** Bucket E verify `pre-launch-auth-hardening-checklist.md` Cat 4 row "tenant-switch flow" — picker triggered if user has ≥2 tenants; JWT scoped to chosen tenant; localStorage purged on switch |
| 19 | 5 concurrent | 100 student | Cloudflare cache stale + invite link | Invite link includes magic token in query string; Cloudflare aggressive cache strips/caches → 2nd tenant clicking gets 1st tenant's redirect | Magic token URLs MUST set `Cache-Control: no-store` + Cloudflare bypass; verify in `pre-launch-infra-hardening-checklist.md` Cat 5 | ❌ E | **E-AC4 (NEW gap):** Magic-link endpoints (`/auth/magic`, `/auth/invite/*`) explicitly bypass Cloudflare cache via Page Rule OR `Cache-Control: no-store, no-cache, max-age=0` header — file GAP-577 P0 BLOCKER (chặn invite) |
| 20 | single | empty | GAP-574 PM2 blocker | Tenant Vy accepts invite, FE app served via Vercel (not PM2 — PM2 is BE/Node service); GAP-574 = PM2 process manager bug affecting kc_app or kh_backend EC2 deploy | GAP-574 description TBD per task brief — if it blocks PM2-managed services on EC2, future FE deploys cohort 3+ will fail to restart cleanly; rc1 deploy still OK but cohort N+ deploys risk | 🟡 F+H | **F-AC2:** GAP-574 status MUST flip resolved/PARTIAL với explicit "blocks cohort 3+ deploy" notation BEFORE Bucket F tag rc1; if unresolved → tag still OK but Bucket H runbook documents rollback procedure để avoid PM2 path |
| 21 | 5 concurrent | 100 student | log volume spike + monitoring cascade | Pod restart + log spike + alarm cascade combo: kitehub-email Docker container OOM-killed (residual GAP-502 risk) → restarts → emits restart event → CloudWatch alarm `ContainerRestart` fires | If alarm not routed (cell 16 H-AC4 unresolved) → silent restart loop; first detection only when tenant reports "didn't receive email" | 🔴 H | (covered by H-AC4) |
| 22 | 5 sequential | 100 student | payment webhook delay | Hằng đăng ký Premium Day 3 of beta; Stripe webhook delivered after 60s | Phase 1 BETA tenants on FREE plan typically; Premium subscription edge case; verify Wave 85 webhook handler still works post Spring Boot bump | 🟡 B | (covered by B-AC2) |
| 23 | single | 500 student | DB conn-pool exhaust + RDS storage | One mass-import tenant (1000 students upload at once) tests data-volume edge | Wave 85 D-AC1 pagination uses slice (no count) — verify Bucket E sweep `pre-launch-owasp-rest-hardening-checklist.md` Cat 3 includes pagination defaults | 🟡 E | **E-AC5:** Bulk-import endpoint cap = 1000 rows/request (HTTP 413 if exceeded); FE UX shows progress bar; chunk client-side if > 1000 |
| 24 | 5 concurrent | 100 student | RDS storage near-full + RLS audit logs | 5 tenants × RLS audit log writes (Wave 85 B-AC7 admin BYPASS audit) accelerate storage growth | Audit log retention policy unset → grows unbounded → reaches 80% storage in ~6 months | 🟡 H | **H-AC5:** Audit log retention policy = 90 days hot in RDS + archive to S3 lifecycle; cron job nightly DELETE WHERE created_at < NOW() - INTERVAL '90 days'; document in `documents/05-guides/operations/audit-log-retention-runbook.md` |
| 25 | 50 mass | 100 student | email queue backlog | Day 30 Phase 1.5 candidate: 50 tenants invited same hour → 50 invite emails queued | Resend Free Tier 100/day = 50 invites OK; nhưng cumulative welcome + first-class-create notifications = ~200 emails — exceeds Free Tier; need paid plan trigger | 🟡 H | **H-AC6:** Phase 1.5 invite plan (≥20 tenants) includes Resend paid plan upgrade ($20/month 50k emails); document in `documents/05-guides/account-prep/resend-paid-upgrade-runbook.md` |
| 26 | 5 concurrent | empty | happy + AWS Activate denial | GAP-412 resubmit happens simultaneously với invite cohort (Bucket D parallel Bucket G) | If Activate denied #2 (Wave 81 #1 already denied) → financial cushion gone → potential infra cost overrun → emergency cost cap activation per Wave 84 GAP-414 | 🟡 D+H | **D-AC1:** Bucket D resubmit cite "Wave 81 deploy CLOSED + kitehub.me live + Wave 86 first 5 tenants invited" trong application body; attach link to public kitehub.me URL for AWS reviewer verification |
| 27 | 5 concurrent | 100 student | support overflow + monitoring alarm | First incident T+24h: tenant Tâm reports "không nhận được email" — support@kitehub.me inbox + CloudWatch alarm both fire | MTTR target <2h — but support manual triage + incident response runbook bám theo Wave 86 specific paths NEEDS dry-run | 🟡 H | **H-AC7:** Incident response runbook dry-run trước tag rc1: simulate "email không gửi" scenario, time-to-detect + time-to-recover, document baseline để measure real T+1 |
| 28 | 5 concurrent | 1000 student | extreme volume + OOM | Edge: 1 tenant uploads 1000 students AND 4 others bulk-importing concurrently | Wave 85 E-AC1 + B-AC1 should hold; nếu B-AC3 heap-dump baseline shows drift → defer Bucket G invite until tuning | 🟡 B+G | (covered B-AC3) |

## 4. Top 10 failure modes (priority-ranked by blast-radius × likelihood)

| # | Failure mode | P-level | Bucket | Rationale |
|---|---|---|---|---|
| 1 | AlertManager receivers chưa wired (GAP-144) → silent restart loop, no MTTD (cell 16, 21) | 🔴 P0 | H | Blocks Wave 86 entire monitoring premise; without alerts, MTTR target <2h impossible — invite cohort would experience outage with zero detection |
| 2 | Magic-link endpoints cached by Cloudflare → cross-tenant invite redirect leak (cell 19) | 🔴 P0 | E | Catastrophic security breach in onboarding flow; blocks Bucket G invite — file NEW GAP-577 P0 BLOCKER |
| 3 | RDS storage no alarm wired → silent fill → manual emergency (cell 8) | 🟠 P1 | H | Phase 1 BETA timeline 30 days → 60% threshold reached; without alarm, first detection = service down — file NEW GAP-576 P1 |
| 4 | OAuth retry creates duplicate user/orphan tenant (cell 4) | 🟠 P1 | NEW gap | Cross-tenant orphan record = data integrity nightmare; file NEW GAP-575 P1 |
| 5 | Spring Boot 3.5.x bump regresses @Async / webhook idempotency / memory baseline (cells 7, 11, 17) | 🟠 P1 | B | Multi-dimensional regression risk post-bump; B-AC1/B-AC2/B-AC3 smoke tests prevent |
| 6 | Email queue backlog + no DLQ → invite email silently dropped (cell 3) | 🟠 P1 | G | First-impression damage; tenant doesn't even see Wave 86 product — G-AC1 retry+fallback |
| 7 | Audit log retention unbounded → storage growth (cell 24) | 🟡 P2 | H | Slow-burn; 6-month timeline gives buffer; H-AC5 retention policy |
| 8 | Resend Free Tier ceiling hit at Phase 1.5 (cell 25) | 🟡 P2 | H | Phase 1.5 future scope; H-AC6 paid upgrade documented |
| 9 | GAP-574 PM2 blocker for cohort 3+ FE deploys (cell 20) | 🟡 P2 | F | Doesn't block rc1; blocks future scaling — F-AC2 status check before tag |
| 10 | Cloudflare cache stale post Vercel deploy → tenants see old UI (cell 12) | 🟡 P2 | F | UX confusion but recoverable in minutes; F-AC1 release CI workflow add purge step |

## 5. AC additions per bucket

**Bucket B (Spring Boot bump) — 3 new ACs:**
- B-AC1 Verify `@Async` annotation processing post-bump (smoke bulk-import returns 202)
- B-AC2 Webhook idempotency replay test (POST same key 2× → 2nd returns 200 with original record)
- B-AC3 Heap baseline + post-bump comparison (`jcmd VM.native_memory`); alert delta > 10% non-heap

**Bucket D (AWS Activate resubmit) — 1 new AC:**
- D-AC1 Application body cite "Wave 81 CLOSED + kitehub.me live + Wave 86 first 5 invited" + public URL for reviewer

**Bucket E (Pre-launch hardening) — 5 new ACs:**
- E-AC1 Magic link TTL = 24h documented in auth checklist; FE countdown; resend rate-limit
- E-AC2 Redis `maxmemory-policy=allkeys-lru` row added to infra checklist Cat 5
- E-AC3 Tenant-switch flow row in auth checklist Cat 4 (JWT swap + cache invalidate)
- **E-AC4 (P0 BLOCKER):** Magic-link endpoints bypass Cloudflare cache (Page Rule OR `Cache-Control: no-store`) — paired NEW GAP-577
- E-AC5 Bulk-import row cap 1000/request (HTTP 413); FE chunked client-side

**Bucket F (Tag rc1) — 2 new ACs:**
- F-AC1 Release CI workflow includes "Cloudflare cache purge" step post Vercel deploy
- F-AC2 GAP-574 status confirmed (resolved/PARTIAL với cohort-3+ deploy notation) BEFORE tag

**Bucket G (Beta cohort invite) — 3 new ACs:**
- G-AC1 Resend wrapper retry 3× + fallback to AWS SES + metric `Email/InviteFail` → SNS
- **G-AC2 (NEW gap):** OAuth callback idempotency (state_token UNIQUE) — paired NEW GAP-575
- G-AC3 Welcome email sets support SLA expectation ("<24h via support@kitehub.me")

**Bucket H (Monitoring + incident response) — 7 new ACs:**
- **H-AC1:** K6 load profile "cohort-50-concurrent-signup" baseline before Phase 1.5
- **H-AC2 (NEW gap):** RDS storage alarm + runbook — paired NEW GAP-576
- H-AC3 EC2-Docker healthcheck documented (compose `healthcheck:` + `restart: unless-stopped`)
- **H-AC4 (P0 BLOCKER):** GAP-144 AlertManager receivers wired BEFORE invite (Bucket H prerequisite)
- H-AC5 Audit log retention 90d hot + S3 archive + nightly cleanup cron + runbook
- H-AC6 Phase 1.5 Resend paid upgrade runbook documented
- H-AC7 Incident response runbook dry-run BEFORE tag rc1 (simulate "email không gửi"); measure baseline MTTD/MTTR

## 6. NEW gap proposals

| Proposed ID | Title | P | Bucket affinity | Notes |
|---|---|---|---|---|
| GAP-575 | OAuth callback idempotency (state_token UNIQUE constraint + 409 on duplicate) | 🟠 P1 | G (G-AC2) | Cell 4; prevents cross-tenant orphan record từ OAuth retry |
| GAP-576 | RDS storage alarm wiring + resize runbook | 🟠 P1 | H (H-AC2) | Cell 8; CloudWatch alarm + SNS + manual runbook (autoscale disabled cost-saving) |
| GAP-577 | Magic-link endpoints bypass Cloudflare cache (BLOCKER chặn Bucket G invite) | 🔴 P0 | E (E-AC4) | Cell 19; cross-tenant invite redirect leak risk = catastrophic; Cloudflare Page Rule + `Cache-Control: no-store` |

## 7. GAP-502 OOM recurrence — still RC2 risk under 100-user load?

**Wave 85 status:** GAP-502 PARTIAL 95% (per sister audit). RC2 fix = E-AC1 (MaxRAMPercentage=60 on t3.small) shipped in Wave 85 Bucket E.

**Wave 86 first-100-user load analysis:**
- Realistic Phase 1 BETA cohort = 5 tenants × max 500 students each = 2.5k rows total in active query workload
- JVM heap @ 60% × 1.4 GiB available ≈ 850 MB; non-heap ≈ 400 MB; total ≈ 1.25 GiB → headroom 150 MB cho t3.small 2 GiB container limit
- Cell 17 (5 concurrent × 500 student bulk import) stress-tests this baseline; Wave 85 E-AC3 async bulk-import + B-AC3 heap-baseline-comparison mitigate

**Verdict:** **Under realistic first-100-user load (5 tenants × 500 students Phase 1 BETA), GAP-502 RC2 OOM recurrence risk = LOW** provided:
1. Wave 85 E-AC1 MaxRAMPercentage=60 deployed ✅ (assumed Wave 85 closure)
2. Wave 86 B-AC3 heap baseline comparison post Spring Boot bump prevents regression ✅ (proposed this audit)
3. Wave 86 B-AC1 verify @Async still operational post-bump ✅ (proposed this audit)

**Wave 86 ship guidance:**
- GAP-502 stays PARTIAL 95% UNTIL 14-day observation window post Wave 86 invite (per Wave 85 sister audit recommendation)
- DONE flip Day +14 after invite IF zero OOM events in CloudWatch + heap stays <80% peak utilization
- Risk if Spring Boot 3.5.x bump regresses memory profile: B-AC3 detector catches before invite; Bucket G invite gated on Bucket B pass

**Conclusion:** GAP-502 RC2 risk = LOW under Wave 86 realistic load; B-AC1/B-AC2/B-AC3 add defense-in-depth against Spring Boot bump regression.

## 8. GAP-574 PM2 bug — chặn future FE deploy cohort 3+?

**Status check requirement:** task brief mentions GAP-574 "PM2 bugs" without specific blocking scope. Per cell 20 analysis:

**Phase 1 BETA deploy architecture (assumed):**
- FE = Next.js apps deployed via Vercel (kitehub.me + kiteclass.me)
- BE = Spring Boot Docker containers on EC2 (t3.small × 2)
- PM2 process manager TYPICALLY used for Node.js services; Phase 1 BETA may not use PM2 for FE (Vercel serverless) or BE (Docker)

**Possible scenarios:**
1. **PM2 manages BE Docker on EC2 (kc_app + kh_backend)** → cell 20 applies → GAP-574 blocks future deploy; F-AC2 status check before tag rc1
2. **PM2 not in active deploy path Phase 1 BETA** → GAP-574 lower priority; can defer Wave 87+ Phase 1.5
3. **PM2 used for auxiliary scripts only (cron jobs, log shippers)** → GAP-574 affects ops not first 5 cohort UX

**Wave 86 action:**
- F-AC2 mandates GAP-574 status verified BEFORE tag rc1
- If scenario 1 + GAP-574 unresolved → rc1 tag still OK (current process works), but document workaround in Bucket H incident response runbook + defer cohort scaling >5 tenants until GAP-574 closed
- If scenario 2/3 → continue Wave 86 as planned; file GAP-574 follow-up Wave 87 candidate

**Cross-ref recommendation:** Bucket F coordinator MUST grep current EC2 deploy scripts (`scripts/deploy-prod.sh` per Wave 85 Bucket F GAP-506) for PM2 invocations — verify deploy path không depend on PM2 cho first 5 cohort.

## 9. Verdict — Wave 86 scope completeness %

**Wave 86 scope coverage of simulated failure modes:**

| Category | Cells covered | Cells gap | % |
|---|---|---|---|
| Cohort invite happy path (cells 1, 2) | 2 fully | 100% | ✅ |
| Email/queue/Resend (cells 3, 25) | 1 partial + 1 partial Phase 1.5 | 50% | ⚠️ |
| OAuth/auth flow (cells 4, 5, 18) | 1 gap + 2 partial | 33% | 🔴 |
| DB pool + bulk import (cells 7, 11, 23, 28) | 2 partial + 2 covered post Wave 85 | 50% | ⚠️ |
| Storage + retention (cells 8, 24) | 0 covered + 2 gap | 0% | 🔴 |
| Cache + CDN (cells 12, 19) | 1 partial + 1 gap (P0!) | 25% | 🔴 |
| Monitoring + alarm cascade (cells 16, 21, 27) | 1 P0 blocker + 2 cascade | 0% | 🔴 |
| GAP-502 OOM regression (cells 17, 28) | covered by Wave 85 + B-AC3 | 80% | ✅ |
| GAP-574 PM2 blocker (cell 20) | partial (F-AC2 status check) | 50% | ⚠️ |
| Phase 1.5 forward-looking (cells 6, 8 partial, 25) | 1 covered + 2 partial | 50% | ⚠️ |

**Overall Wave 86 scope completeness vs simulated production reality: ~48%**

**Critical AC additions required BEFORE Wave 86 ship (P0 BLOCKERS):**
1. **E-AC4 (GAP-577)** Magic-link Cloudflare cache bypass — chặn Bucket G invite; cross-tenant security
2. **H-AC4 (GAP-144 wiring)** AlertManager receivers — chặn Bucket H monitoring premise
3. **F-AC2** GAP-574 PM2 status verified — chặn Bucket F tag confidence
4. **B-AC1/AC2/AC3** Spring Boot bump regression suite — chặn Bucket B → cascades chặn Bucket G

**P1 ACs (defer ≤ 7 days post-ship if necessary):**
- G-AC1 Resend retry+fallback+metric (cohort scale-up risk)
- G-AC2 (GAP-575) OAuth idempotency (cross-tenant orphan)
- H-AC2 (GAP-576) RDS storage alarm (6-month timeline buffer)

**Recommendation:** Wave 86 scope **MUST add 21 ACs** (3B + 1D + 5E + 2F + 3G + 7H) before lock. Without **E-AC4 + H-AC4 + F-AC2 + B-AC1/AC2/AC3** P0 BLOCKERS, Wave 86 ships first cohort invite with monitoring blind spot + onboarding security gap + dep-bump regression risk. File **GAP-575/576/577** post-Wave-86 ship (defer scope) OR before-Wave-86 if user wants strict P0 closure.

**Confidence:**
- **HIGH** for P0 BLOCKERS (Wave 84 ops audit explicit GAP-144 P1 carry-forward → realistically P0 in cohort-invite context; cell 19 Cloudflare cache bypass standard industry practice; Wave 85 sister audit Spring Boot bump regression precedent)
- **MEDIUM** for P1 ACs (Phase 1.5 scaling concerns; 5-tenant immediate scope tolerable but technical debt accelerating)
- **LOW** for forward-looking cells 25, 26 (Phase 1.5+ scope; out of immediate Wave 86 acceptance criteria)

**Outside-in vs inside-out delta:** Wave 86 inside-out scope (8 buckets in plan §3) addresses dep bump + screenshots + tag + invite + monitoring well. Outside-in simulation surfaces 3 critical gaps (Cloudflare cache, AlertManager receivers, OAuth idempotency) that inside-out missed — typical 30-40% delta for outside-in audits per `outside-in-coverage-trigger.md` baseline data.

## 10. References

- Sister simulation audit Wave 85: `2026-05-15-pre-wave-85-simulation-3axis.md`
- Wave 86 plan: `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md`
- Wave 84 ops-readiness 78/100: `documents/04-quality/audits/ops-readiness/2026-05-15-wave-84-post-apply.md` (referenced via `output-review-mandate.md` §3 row)
- Wave 83 security 90/100 v2 format
- GAP-502 historical RC1+RC2: `documents/04-quality/gaps/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md`
- GAP-144 AlertManager receivers (P1 carry-forward Wave 84 → P0 Wave 86 cohort-invite context)
- GAP-440 Spring Boot 3.5.x bump
- GAP-412 AWS Activate $1k credit resubmit
- GAP-574 PM2 bug status TBD
- Skill: `.claude/skills/quality/simulation-gap-finder/SKILL.md`
- Rule: `outside-in-coverage-trigger.md` §3 (Wave 86 first-cohort scope = strong trigger)
- Rule: `pre-handoff-self-test-completeness.md` §2.6 payment, §2.7 multi-tenant, §2.8 SSE — cells 11, 18 reference
