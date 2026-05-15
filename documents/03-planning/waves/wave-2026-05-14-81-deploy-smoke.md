---
title: Wave 81 — Deploy + Smoke (Phase 1 BETA production rollout for self-test)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [81]
gaps: [GAP-370, GAP-372, GAP-502, GAP-514, GAP-525, GAP-527, GAP-530, GAP-533]
---

# Wave 81 — Deploy + Smoke

**Goal:** Đưa code Wave 77+78+79 lên production environment + smoke test endpoints + pre-self-test spot check 10/126 rows → hand off cho dev tự test 126 rows. Sau Wave 80, dev có thể chạy `phase-1-beta-acceptance-self-test.csv` từ đầu tới cuối với real production URLs.
**Trigger:** Wave 79 close v1.0.0-rc gate (3 P0 audit + 3 P0 outside-in). Code đã ready trên main nhưng AWS stack stopped, kitehub-email chưa deploy, Resend DKIM chưa verify, V39-V46 migrations chưa apply. Self-test CSV (Wave 72b Bucket G ship) yêu cầu live production endpoints.
**Estimated wall-clock:** ~6-10h chia: ~3h user-action (AWS up + AWS Console steps + workflow_dispatch triggers) + ~2h coordinator/agent (state-check, smoke verify, pre-self-test walk) + ~2-4h CI wait time + buffer.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Wave 80 100% serve dev self-test enablement (1 internal "persona"). Domain coverage là **ops/deploy only** — không user-facing scope mới (per `outside-in-coverage-trigger.md` §4 exception row "Wave 100% internal scope (ops, refactor, tech debt)" → skip outside-in audit). Wave 80 là gate giữa "code merged main" ↔ "real dev self-test với real production endpoints".

**Q2 (trade-offs):**
- **Self-test trên localhost docker-compose vs prod environment:** chọn **prod environment** (Wave 80 ship). Localhost không cover: real Resend SMTP send, real DNS DKIM verify, real CloudFront/Cloudflare CDN, real PostgreSQL RDS với production schema. Self-test CSV explicitly cite `https://kitehub.me/` — localhost test = false-pass risk.
- **AWS stack start before vs after deploy:** chọn **before** (stage A) — RDS warm-up takes ~3-5 min cold start; deploy workflow needs RDS connection for Flyway. Serialize: AWS up → RDS available → deploy.
- **Deploy bundle: all services tại 1 workflow_dispatch vs per-service:** chọn **bundle deploy-production.yml** (existing workflow). Per `concurrent-production-mutation-ops.md` workflow handles intra-deploy serialization (V-migrations run sequential by version).
- **Seed via SQL script vs admin UI vs Flyway data migration:** chọn **dedicated seed script `scripts/seed-phase-1-beta.sh`** triggered after services healthy. Idempotent (UPSERT on email key + role name). Flyway data migration mixed schema+data = anti-pattern.
- **Pre-self-test spot check scope: 10 rows vs full 126:** chọn **10 rows critical-path** (3 anonymous landing + 4 owner signup→invite-staff + 3 admin approve). Coordinator catches deploy-class bugs (404, 500, slow); dev catches product-class bugs (UX, business logic).
- **Hand off self-test trên Beta tenant vs Demo tenant:** chọn **Demo tenant** (new tenant signup via Wave 77 beta-access flow). Real beta tenants không tồn tại yet; dev tự đăng ký = closest to real beta UX.

**Q3 (risks):**
- **RDS cold start failure:** RDS đã stopped 2 ngày → AWS auto-stop policy có thể delete instance state. Mitigation: verify `aws rds describe-db-instances` returns `available` post-start; nếu `failed-state` cần restore từ snapshot per restore drill (GAP-117).
- **Resend warm-up Day 0 deliverability:** Day 0 emails dễ rớt spam (no domain reputation yet). Mitigation: warm-up runbook (~5 emails) trước khi gửi self-test invite-staff; monitor Resend dashboard bounce rate.
- **V39-V46 migration cluster apply concurrent:** 8 migrations apply in sequence on first deploy. Risk: 1 V-fail → all rollback. Mitigation: per-migration log verify; dry-run trên staging RDS snapshot trước prod nếu migration count >5 (current 8 → recommended dry-run).
- **SES DENIED status (GAP-370 95% PARTIAL):** AWS SES sandbox DENIED case 177857212400418 unresolved. Resend là primary; SES backup chỉ cho fallback. Wave 80 không gate trên SES — Resend đủ Phase 1 BETA.
- **Cred rotation impacts running services:** rotating DB password / OIDC role secret invalidates current sessions. Mitigation: rotate BEFORE deploy (Bucket C → D) — new deploy pulls new creds; service restart fresh state.
- **Smoke test false-pass:** smoke test passes BUT real product bugs exist (UX, business logic). Mitigation: pre-self-test 10-row spot check covers product-class (Bucket G); smoke test scope only covers ops-class (Bucket F).
- **AWS stack restart cost:** AWS Free Tier 750h/mo EC2 — restart costs Free Tier hours. Mitigation: Wave 80 ship within 24h then stop-stack post-handoff cho dev tự start khi self-test (per `scripts/aws/stop-stack.sh`).

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A AWS up + state-check** | (Tier 1 verify) | user-action + agent verify | ~15 min | First |
| **B Email infra** | GAP-370 (SES re-submit) + GAP-533 (Resend DKIM) + GAP-502/527 (kitehub-email deploy) | user-action + agent prep | ~45 min | Parallel với C |
| **C Credential rotation** | GAP-525 (3 leaked creds) | user-action | ~30 min | Parallel với B |
| **D Deploy services** | GAP-514 (rate limit live), V39-V46 migrations apply | user workflow_dispatch + agent verify | ~30 min + ~15 min CI | After B+C |
| **E Initial seed** | GAP-372 (invite mechanism first real test) + admin/RBAC seed | agent script + user verify | ~45 min | After D |
| **F Smoke test** | (Tier 1 verify all critical endpoints) | agent | ~30 min | After E |
| **G Pre-self-test 10-row** | GAP-530 (email flow live verify) — first 10/126 rows critical path | coordinator (Claude) | ~1 h | After F |

**Sequential check:** A→B+C→D→E→F→G. Per `concurrent-production-mutation-ops.md`: deploy ops serialize. B+C parallel OK vì disjoint (B = email vendor + service deploy; C = AWS Secrets Manager cred rotation).

---

## 3. Scope (compact schema — Strategy B+C proven Wave 33)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH → model: Opus 4.7 full (production mutation discipline; pre-self-test gate)
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO → skip Bucket 0 Foundation (no new contracts; existing endpoints deploy-only)

> Wave 80 buckets are mostly USER-EXECUTED + AGENT-VERIFIED ops, not parallel agent worktree work. Spawn pattern differs from typical wave (xem §6 Agent Spawn Pattern).

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|---|---|:-:|---|:-:|
| 1 | **A** AWS up | (state-check only) | 🟠 ops | `scripts/aws/start-stack.sh` + verify outputs into `documents/04-quality/audits/aws-verification/2026-05-14-wave-80-pre-deploy-state.md` | First |
| 2 | **B** Email infra | GAP-370 + GAP-533 + GAP-502/527 | 🔴 P0 | AWS Console SES + Resend dashboard + `gh workflow run deploy-production.yml` (kitehub-email subset) + `documents/05-guides/deploy/email-deliverability-runbook.md` walkthrough | After A |
| 3 | **C** Cred rotation | GAP-525 | 🔴 P0 | `scripts/rotate-leaked-credentials.sh --cred=...` × 3 | Parallel với B |
| 4 | **D** Deploy services | GAP-514 (last 10% live verify), V39-V46 apply | 🔴 P0 | `gh workflow run deploy-production.yml -f confirm=APPLY -f dry_run=false` + Flyway auto-apply V39-V46 + ALB health verify | After B+C |
| 5 | **E** Initial seed | GAP-372 (invite mechanism first real prod test) | 🔴 P0 | `scripts/seed-phase-1-beta.sh` (NEW — create PLATFORM_ADMIN user + invite-staff Resend template ID + 2-role RBAC rows + sample-data flag default) | After D |
| 6 | **F** Smoke test | (Tier 1 verify) | 🟠 ops | `bash scripts/smoke-phase-1-beta.sh` (NEW — wraps 4 health + 7 rate-limit + 2FA enrollment dry-run + 1 email send test) | After E |
| 7 | **G** Pre-self-test | GAP-530 | 🟠 ops | coordinator walk 10/126 CSV rows + file bugs surfaced trong `documents/04-quality/audits/pre-self-test/2026-05-14-wave-80-spot-check.md` | After F |

### Bucket A — AWS stack up + pre-deploy state-check

- Files: `scripts/aws/start-stack.sh` (existing) + `documents/04-quality/audits/aws-verification/2026-05-14-wave-80-pre-deploy-state.md` (NEW audit artifact per `agent-aws-access.md` §5)
- Steps:
  1. User: `bash scripts/aws/start-stack.sh` → EC2 kh-backend + kc-app start; RDS kitehub-postgres start
  2. Agent: poll `aws ec2 describe-instances` until both `running` + `state.reason="ok"`
  3. Agent: poll `aws rds describe-db-instances --db-instance-identifier kitehub-postgres` until `DBInstanceStatus=available`
  4. Agent: verify ALB target group healthy via `aws elbv2 describe-target-health`
  5. Agent: verify CloudTrail still logging via `aws cloudtrail get-trail-status --name kitehub-main`
  6. Agent: write state-check artifact với Tier 1 commands run + outputs (per `pre-mutation-state-check.md` §3 mandate)
- Acceptance: All 4 resources healthy + audit artifact filed

### Bucket B — Email infrastructure (parallel với C)

- Files: AWS Console SES re-submit (manual) + Resend dashboard DKIM verify (manual) + `gh workflow run deploy-production.yml` (kitehub-email subset) + `documents/05-guides/deploy/email-deliverability-runbook.md` walkthrough
- Steps:
  1. User: AWS Console → SES → re-submit production access request (per GAP-370 retry; case 177857212400418 DENIED)
  2. User: Resend dashboard → kitehub.me domain → verify DKIM CNAME records (3 records) → wait DNS propagation (~5-15 min)
  3. Agent verify: `dig +short kitehub.me TXT; dig +short resend._domainkey.kitehub.me CNAME` returns expected values
  4. User: `gh workflow run deploy-production.yml -f service=kitehub-email -f confirm=APPLY -f dry_run=false`
  5. Agent verify: `aws ssm send-command` → `docker ps --filter name=kitehub-email --format "{{.Status}}"` shows `Up X minutes (healthy)`
  6. User: Resend dashboard send 5 warm-up emails to support@kitehub.me + 1 personal address → verify inbox delivery (NOT spam folder)
- Acceptance: GAP-533 status=DONE (DKIM verified); GAP-502/527 status=DONE (kitehub-email healthy); GAP-370 status=PARTIAL still (SES async approval; not gating)

### Bucket C — Credential rotation (parallel với B)

- Files: `scripts/rotate-leaked-credentials.sh` (existing) — invoke ×3 cho 3 leaked creds per GAP-525
- Steps:
  1. User: `bash scripts/rotate-leaked-credentials.sh --cred=<credential-id-1>` (cred-1 from 2026-05-13 session log)
  2. User: repeat for cred-2, cred-3
  3. Agent verify: AWS Secrets Manager rotation status via `aws secretsmanager describe-secret --secret-id <id>` → `LastRotatedDate` = today
- Acceptance: GAP-525 status=DONE (3 creds rotated)

### Bucket D — Deploy services (after B+C)

- Files: `gh workflow run deploy-production.yml -f confirm=APPLY -f dry_run=false` (deploys kitehub-subscription + kitehub-gateway + kitehub-admin + kitehub-frontend); Flyway auto-apply V39-V46 (8 migrations: V39-V41 Wave 77 + V42-V44 Wave 78 + V45-V46 Wave 79)
- Steps:
  1. User: workflow_dispatch trigger with confirm=APPLY
  2. Agent verify: workflow status via `gh run watch`
  3. Agent verify: `aws ssm send-command` → check each service container healthy + Flyway `flyway_schema_history` table has V39..V46 rows with `success=true`
  4. Agent verify: 7 rate-limit endpoint live 429 via `for endpoint in login refresh verify-email resend password-reset-request beta-access register; do curl -X POST ...; done` → expect 429 after threshold (GAP-514 last 10%)
  5. Agent verify: ALB target group all-healthy
- Acceptance: All services healthy + 8 migrations success + 7 rate-limit live 429 verified

### Bucket E — Initial seed (after D)

- Files: `scripts/seed-phase-1-beta.sh` (NEW) — create:
  - PLATFORM_ADMIN user (`admin@kitehub.me` per GAP-518) với TOTP 2FA pre-enrolled (recovery codes saved to AWS Secrets Manager)
  - invite-staff Resend template registration (template ID stored in AWS Parameter Store)
  - 2-role RBAC rows (OWNER + STAFF, from Wave 79 GAP-562)
  - Sample-data flag (Wave 78 sample-data-seed worker; opt-in step in onboarding)
- Steps:
  1. Agent: `bash scripts/seed-phase-1-beta.sh --env=production` (idempotent UPSERT)
  2. Agent verify: `curl -sS -H "Authorization: Bearer <admin-jwt>" https://api.kitehub.me/admin/me` returns admin user with role=PLATFORM_ADMIN
  3. Agent verify: SQL `SELECT * FROM rbac_roles` returns 2 rows
  4. User: AWS Secrets Manager check → `KITEHUB_ADMIN_TOTP_RECOVERY_CODES` populated
- Acceptance: GAP-372 status=DONE (invite mechanism end-to-end first prod test gate passed)

### Bucket F — Smoke test (after E)

- Files: `scripts/smoke-phase-1-beta.sh` (NEW) — wraps:
  - 4 health endpoints (kitehub-subscription, kitehub-gateway, kitehub-admin, kitehub-email)
  - 7 rate-limit endpoint 429 verification
  - 2FA enrollment dry-run (`POST /api/v1/auth/2fa/enroll-init` returns QR + recovery codes; do NOT confirm-enroll)
  - 1 email send test (beta-access form → email arrived in inbox within 30s)
  - Vercel frontend 200 OK với security headers (CSP, X-Frame-Options, HSTS)
- Steps:
  1. Agent: `bash scripts/smoke-phase-1-beta.sh --env=production --skip-mutation=true` (Tier 1 read-only)
  2. Agent: parse output; any FAIL → file gap as P0 BLOCKING for Bucket G
- Acceptance: Smoke test 100% PASS hoặc file P0 gap(s) cho any FAIL before Bucket G

### Bucket G — Pre-self-test 10-row spot check (after F)

- Files: `documents/04-quality/audits/pre-self-test/2026-05-14-wave-80-spot-check.md` (NEW)
- Steps:
  1. Coordinator (Claude) walk 10 rows from `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` critical path:
     - PUB-LAND-001/002/003 (3 anonymous landing rows)
     - OWNER-SIGNUP-001/002 (2 owner signup rows)
     - OWNER-ONBOARD-001 (onboarding entry)
     - OWNER-INVITE-001 (P3 Manager invite send)
     - MANAGER-LOGIN-001 (P3 Manager first login)
     - ADMIN-APPROVE-001 (admin approve beta-request)
     - FEEDBACK-001 (feedback widget submit)
  2. Coordinator: mark each row status PASS/FAIL/BLOCKED in spot-check audit
  3. Coordinator: file P0 gaps cho any FAIL/BLOCKED → resolve before hand-off
- Acceptance: 10/10 rows PASS OR any FAIL has P0 gap filed + hot-fix Wave 81 candidate

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|---|---|---|---|---|
| `scripts/aws/start-stack.sh` | Bash script | `ls scripts/aws/start-stack.sh` | 1 file | ✅ exists |
| `scripts/aws/stop-stack.sh` | Bash script | `ls scripts/aws/stop-stack.sh` | 1 file | ✅ exists |
| `scripts/rotate-leaked-credentials.sh` | Bash script | `ls scripts/rotate-leaked-credentials.sh` | 1 file | ✅ exists |
| `.github/workflows/deploy-production.yml` | CI workflow | `ls .github/workflows/deploy-production.yml` | 1 file | ✅ exists |
| `kitehub-email/Dockerfile` | Service container | `find kitehub -name 'Dockerfile' -path '*kitehub-email*'` | 1 file | ✅ exists |
| `documents/05-guides/deploy/email-deliverability-runbook.md` | Runbook | `ls documents/05-guides/deploy/email-deliverability-runbook.md` | 1 file | ✅ exists |
| `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` | Test matrix | `ls documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` + `wc -l` | 1 file, 127 lines (126 rows + header) | ✅ exists |
| Flyway V39 → V46 migrations | Migration files | `ls kitehub/kitehub-subscription/src/main/resources/db/migration/V{39,40,41,42,43,44}*.sql` | 6 files V39-V44 | ✅ exists (V39-V44); V45/V46 🆕 to-be-created Wave 79 Bucket B |
| AWS Secrets Manager `kite/prod/*` secrets | Secret store | `aws secretsmanager list-secrets --filters Key=name,Values=kite/prod/` | (verify at Bucket A runtime) | ⏳ to-be-verified |
| Resend domain `kitehub.me` DKIM records | DNS records | `dig +short resend._domainkey.kitehub.me CNAME` | (verify at Bucket B runtime) | ⏳ to-be-verified |
| `scripts/seed-phase-1-beta.sh` | Bash script (target Bucket E) | `ls scripts/seed-phase-1-beta.sh` | 0 matches | 🆕 to-be-created (Bucket E sub-task) |
| `scripts/smoke-phase-1-beta.sh` | Bash script (target Bucket F) | `ls scripts/smoke-phase-1-beta.sh` | 0 matches | 🆕 to-be-created (Bucket F sub-task) |
| `documents/04-quality/audits/aws-verification/` | Audit folder | `ls documents/04-quality/audits/aws-verification/ \| wc -l` | 12 files | ✅ exists |
| `documents/04-quality/audits/pre-self-test/` | Audit folder (target Bucket G) | `ls documents/04-quality/audits/pre-self-test/ 2>&1` | "No such directory" | 🆕 to-be-created (Bucket G) |
| `kitehub-admin` PLATFORM_ADMIN role | Java role enum | `grep -rn 'PLATFORM_ADMIN' kitehub/kitehub-subscription/src/main/java/.../auth` | matches (Wave 78 GAP-518) | ✅ exists |
| `rbac_roles` table | DB table (target Wave 79 V46) | `grep -l 'rbac_roles' kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql` | 0 matches | 🆕 to-be-created Wave 79 Bucket B V46 (prerequisite cho Wave 80 Bucket E seed) |

Banned shortcuts: no `| head` truncation, no skip verification "agents will check", no aspirational symbols without 🆕 flag.

---

## 5. Verification Gates (per bucket)

| Bucket | Local/agent verify command | CI gate / artifact |
|---|---|---|
| A | `aws ec2 describe-instances --query 'Reservations[].Instances[].[InstanceId,State.Name]'` + `aws rds describe-db-instances --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceStatus]'` | Audit artifact `2026-05-14-wave-80-pre-deploy-state.md` |
| B | `dig resend._domainkey.kitehub.me CNAME` + `gh run watch <deploy-run-id>` + `aws ssm send-command` Tier 1 `docker ps` verify | kitehub-ci (auto-trigger by workflow_dispatch) |
| C | `aws secretsmanager describe-secret --secret-id <id> --query LastRotatedDate` × 3 | (manual verify) |
| D | `gh run watch <run-id>` + Flyway `SELECT * FROM flyway_schema_history WHERE version >= 'V39'` returns 6+ rows + ALB `aws elbv2 describe-target-health` | kitehub-ci + deploy-production workflow |
| E | `curl -sS -H "Authorization: Bearer <admin-jwt>" https://api.kitehub.me/admin/me` returns role=PLATFORM_ADMIN + `SELECT count(*) FROM rbac_roles` = 2 | (manual verify) |
| F | `bash scripts/smoke-phase-1-beta.sh --env=production --skip-mutation=true` exit 0 | smoke-phase-1-beta artifact |
| G | Coordinator manual walk 10 rows → status column ticked in spot-check audit | `2026-05-14-wave-80-spot-check.md` |

---

## 6. Agent Spawn Pattern

Wave 80 KHÔNG dùng parallel worktree pattern (unlike Wave 77/78/79). Reasoning:
- Buckets là sequential ops (A→B+C→D→E→F→G) per `concurrent-production-mutation-ops.md`
- Mỗi bucket là MIX user-action + agent verify; không spawn-and-merge model
- Coordinator (Claude main session) chạy verify + script-prep; user chạy workflow_dispatch + AWS Console steps

Pattern per bucket:
1. Coordinator prep (Tier 1 read-only + script staging)
2. User-action (workflow_dispatch / AWS Console / script invoke)
3. Coordinator verify (Tier 1 read-only — `agent-aws-access.md` §2.1 allowlist)
4. Audit artifact append per `pre-mutation-state-check.md` §3

Bucket E + F + G có script-creation sub-tasks. Spawn 1 background agent cho 3 scripts cùng lúc:
- `scripts/seed-phase-1-beta.sh` (Bucket E target)
- `scripts/smoke-phase-1-beta.sh` (Bucket F target)
- Spot-check Markdown template (Bucket G prep)

Background agent ship 3 files trong 1 PR; user run scripts trong Bucket E/F/G timing.

Per `agent-aws-access.md`:
- Tier 1 (read-only `describe-*` / `list-*` / safe `get-*`): coordinator runs freely + logs to audit artifact
- Tier 2 (always-confirm): coordinator asks user before each call
- Tier 3 (mutations): USER ONLY — coordinator never invokes

Per `release-deploy-standard.md` §9: deploy execution = human-triggered workflow_dispatch only; agent ban auto-apply.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket completion update affected GAP file Log + status
- ROADMAP §🚀 Next Action updated trong closure PR — flip "Wave 80 in-progress" → "Wave 80 SHIPPED → dev self-test"
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Rule 15 enforcement)
- Sub-gaps file cho any deferral (e.g. GAP-370 SES vẫn PARTIAL post-Wave-80 if AWS chưa approve)
- `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- **`## Release Plan Progress` section trong closure PR body** — per `feedback_wave_closure_release_progress_report.md` rules #1-6: Wave 80 SHIPPED = "dev self-test gate" milestone; Waves Remaining table updated (3 rows: strict-min v0.9.0-beta = post-self-test fix wave 81 if needed; practical v0.9.0-beta = N/A unchanged; v1.0.0 PROD = sau Phase 2)
- **Post-Wave-80 audit suite per `post-wave-audit-mandate.md` §2.1:** Wave 80 100% ops scope — Ops Readiness /100 audit MANDATORY trong 3 ngày; per §2.4 eligible cho domain-milestone deferral `release-deploy-artifacts` (Wave 80 itself IS the milestone của deploy cluster). Quality /110 refresh post-wave checkpoint mandatory.
- **Hand-off message to user:** "Wave 80 SHIPPED. Production environment live. Self-test ready. Walk `phase-1-beta-acceptance-self-test.csv` 126 rows tại your pace. File bugs as P0/P1 gaps. Hot-fix wave 81 standby."

---

## 8. Log

- **2026-05-15** (closure): All 7 buckets DONE + 1 hotfix-cycle (Bucket F 4-attempt fail-fast secret fix saga). Backend production-ready (api.kitehub.me/actuator/health 200 UP — db/redis/disk/ssl). 8 PRs shipped: #1387 (ECR matrix fix kitehub-frontend) + #1388 JWT_CHALLENGE_SECRET + #1389 TOTP+STAFF_INVITATION+KITE_VERSION + #1390 TOTP Spring relaxed binding admin yaml-less + #1391 heredoc env expansion hotfix + #1392 Bucket G spot check audit + #1393 audits-index backfill + #1394 closure cleanup. Bucket G 10/126 row spot check: 8 PASS + 1 PARTIAL (beta-status 400 → Wave 82 P1) + 2 doc bugs (CSV row IDs vs wave plan + /api/v1/auth/login path drift). FE Vercel STALE ~38h (Free Tier build cap hit ~2026-05-13) — backend ready but full 126-row dev walk-through BLOCKED until Wave 82 Bucket B+C FE rebuild. Session housekeeping: CI history 690 → 52 runs; local branches 22 → 1. PRs 1388-1394 admin-merge bypass per `admin-merge-discipline.md` §3 (GitHub Free Tier throttle context) — follow-up Wave 82 Bucket E self-hosted GitHub runner eliminates class. Wave 82 plan drafted same session: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md`.
- **2026-05-14** (draft): Plan created. Wave 80 100% internal ops scope → outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 exception. Buckets sequential per `concurrent-production-mutation-ops.md`. Spawn pattern atypical (mix user-action + coordinator verify, no parallel worktree). Trigger: gate giữa Wave 79 code-complete ↔ dev self-test 126-row CSV walkthrough.
