# GAP-612 — AWS account suspended (verification pending); production stack stopped

**Status:** 🟢 DONE 100% (Wave aws-restore-1 SHIPPED 2026-05-26 — production stack fully restored ~3.5h coordinator-inline). Recovery: Phase A `bash scripts/aws/start-stack.sh` 3 EC2 restart + Phase B RDS restore from snapshot `final-kitehub-postgresa9068e7e-...` (~8min, postgres 15.17 available) + Phase C2 ALB elimination terraform apply (`enable_alb=false` flip + ec2_app SG migrate kc_app_fe ingress 80/443/8080 + alb SG destroy — retry #1 after DependencyViolation manual revoke 10 orphan rules; $20-25/mo permanent save) + Phase C1 SSM nginx reload (git pull `/opt/kite-fe` HEAD `f989f10b`, copy nginx-fe.conf, reload zero-downtime, api.kitehub.me vhost active) + Phase C3 terraform-cloudflare apply (`cloudflare_record.api` CNAME → kitehub.me proxied=true, deleted orphan ALB DNS first per Path 1) + Phase D live smoke `curl https://api.kitehub.me/actuator/health` → HTTP 200 (CF edge → kc_app_fe EIP → nginx → kh_backend gateway 10.0.0.129:8080 → Spring DB UP + Redis UP + 17GB free disk + apex preserved 200). Sister gaps GAP-717 DONE (terraform import jwt_challenge + resend_api_key), GAP-693 stays PARTIAL 70% (SOP runbook deferred Wave aws-rebuild-sop-1).
**Priority:** 🔴 P0 (account-level blocker)
**Domain:** DevOps + Business
**Found:** 2026-05-17 (Wave 90 walkthrough — mid-session AWS access token InvalidClientTokenId + CF 522 origin timeout)
**Affects:** TOÀN BỘ production stack — EC2 instances likely force-stopped, RDS unreachable, SSM not callable, terraform/deploy workflows blocked

## Problem

Mid-Wave-90 walkthrough, sequence cảm nhận được:
1. AWS CLI bắt đầu trả `InvalidClientTokenId` cho mọi command
2. Cloudflare apex → origin returns **522 (origin timeout)** thay vì previous 200/401/404
3. User báo AWS console message: *"Your AWS account was suspended because your account details couldn't be verified. Please check email inbox/spam for AWS messages..."*

AWS account `906286017800` được flagged for verification (common cho new accounts trong 30 ngày đầu OR khi spending patterns unusual). Tài khoản đang `suspended` until user replies với additional information.

## Production impact

🔴 **COMPLETE production outage:**
- `kitehub.me` + `app.kitehub.me` — 522 (CF healthy, origin EC2 stopped)
- `api.kitehub.me` — 522
- Admin login non-functional
- Beta cohort onboarding non-functional
- Email infra non-functional (sister bugs GAP-605/606/608)
- SSM access blocked (cannot debug)
- terraform-apply.yml + deploy-production.yml workflows would fail (OIDC AssumeRoleWithWebIdentity → suspended)

## User action required (NOT agent)

1. Check email `vannkite@outlook.com` + spam folder cho message từ `no-reply@amazon.com` / `aws-support@amazon.com`
2. Reply với requested info (thường là: payment method verification, business address proof, government ID nếu corporate)
3. Wait AWS Support review (24-72h typical)
4. Verify account active again: `aws sts get-caller-identity` returns valid identity
5. Run `bash scripts/aws/start-stack.sh` để restart stopped instances
6. Resume Wave 91 cluster fix per `documents/04-quality/gaps/ROADMAP.md`

## Lessons + follow-ups

### Lessons
- New AWS accounts can be flagged at any moment in first 30 days — production launch from new account = high risk
- No fallback infrastructure (single AWS account, single region) = total outage exposure
- No production-status monitoring detected the 522 — surfaced only via user walkthrough

### Follow-up gaps (file when AWS restored)
- (P2) **Multi-region or cross-cloud DR plan** — Phase 2+ scope per `documents/02-architecture/deployment-strategy.md`
- (P2) **External uptime monitoring** (vd UptimeRobot / BetterStack free tier) ping kitehub.me every 1 min, alert via email — independent of AWS
- (P1) **Production status page** (GAP-373 already P1) — Statuspage public auto-incident when 522 detected
- (P3) **AWS account-health dashboard** check daily — `aws health describe-events` API monitoring

## Proposed Fix (recovery path)

### Phase 1 (user action, blocking)
User responds to AWS verification request. Wait approval.

### Phase 2 (post-restore, ~30 min)
1. `aws sts get-caller-identity` → confirm active
2. `bash scripts/aws/start-stack.sh` → restart EC2 + RDS
3. Verify endpoints: `curl -sI https://kitehub.me/` → 200, `curl -sI https://api.kitehub.me/actuator/health` → 200
4. Resume Wave 91 cluster fix (7 gaps queued)

### Phase 3 (long-term)
- Pre-pay AWS invoices to avoid future flag triggers
- Setup AWS Business Support plan ($100/mo) cho priority recovery if recurs
- File P2 follow-ups per "Lessons" above

## Acceptance Criteria

- [ ] AWS account active (sts get-caller-identity succeeds)
- [ ] EC2 instances `running` state (kh_backend + kc_app + kc_app_fe)
- [ ] RDS available
- [ ] kitehub.me HTTP 200
- [ ] api.kitehub.me /actuator/health HTTP 200
- [ ] Documented root cause of suspension (verification trigger) in this gap Log
- [ ] Long-term follow-ups filed (P2 uptime monitoring + P2 DR plan + P3 health dashboard)

## Related

- GAP-605/606/607/608/609/610/611 — Wave 91 cluster blocked until AWS restored
- `release-deploy-standard.md` §4.4 rollback — not applicable here (not deploy issue)
- `agent-aws-access.md` — operational constraints during suspension
- Wave 88 EC2 stop-stack pattern — but this is forced-stop by AWS, not voluntary

## Log

- **2026-05-27 (RST local EXTENDED verify PASS — GAP-756 Phase 1):** Per GAP-756 Phase 1 scope (user direction "Phase 1 only" + follow-up "RST full local chưa?" → extended scope). Local stack `bash kitehub/scripts/up.sh --profile full` → 13/13 services healthy ~3 phút. kitehub-frontend image rebuild qua `bash kitehub/scripts/rebuild.sh frontend` để pick up Wave Bucket F+G commit `a64bcef2` (image trước đó build 07:11 UTC trước Bucket F+G commit 17:53 UTC = stale). Smoke evidence:
  - **Infrastructure:** Gateway `/actuator/health` 200; FE roots hub :3001 + class :3000 → 200; RabbitMQ 12 queues 0 backlog; Flyway 56/56 success; mailhog 3 emails delivered (admin-login-alert ×2 + Vietnamese beta access code "Mã truy cập Beta KiteHub của bạn" sent at 03:04:38 ngay sau approve action — end-to-end approve→outbox→queue→SMTP verified).
  - **FE wave routes:** Bucket A `/legal/privacy` + `/legal/terms` 200; Bucket F+G `/waitlist` 200 (sau rebuild).
  - **Admin login:** `POST /api/auth/login` (admin@kitehub.com / Admin@KiteHub123 V9 seed) → HTTP 200 + JWT role=PLATFORM_ADMIN.
  - **Public beta-request:** `POST /api/v1/auth/request-beta-access` HTTP 201 row id=11 PENDING, consent payload + VN sample (Trần Thị Smoke / Trung tâm Smoke Test), Bucket A Wave 105 stored-XSS HTML entity encoding verified at input.
  - **Admin approve flow:** Login JWT → `GET /api/v1/admin/beta-requests?status=PENDING` 200 row 11 visible → `POST /api/v1/admin/beta-requests/11/approve` với approverId → HTTP 200 status APPROVED + approvedAt timestamp + beta access code email sent.
  - **Bucket E concurrency safety:** Direct PG 10 parallel INSERT same subdomain → 1 succeeded + 9 duplicate_key errors per `instances_subdomain_key` UNIQUE constraint. App TOCTOU race window covered by DB constraint per `GlobalExceptionHandler` 409 mapping.
  - **Bucket D schema audit:** 9 tables với `tenant_id` column (users, consent_record, consent_record_immutable, feedback_submissions, impersonation_audit_log, oauth_attempts, onboarding_progress, staff_invitations + audit_log).
  - **Caveat 1 (image rebuild required):** Initial image 2026-05-26 07:11 UTC missed Bucket F+G commit 17:53 UTC → `/waitlist` 404 pre-rebuild. Deploy pipeline (Phase 2+3) cần ensure ECR push picks up post-Bucket-F+G code (PR #1876 main HEAD `a64bcef2`).
  - **Caveat 2 (browser UI walk deferred):** §2.4 admin-flow (c)(e)(f)(g) UI render/click verify ngoài CLI scope. Defer browser test (Bucket B 2FA enrollment flow + Bucket D runtime cross-tenant fetch + UI approve click) → next session OR Playwright integration.
  - **Caveat 3 (EmailEvent deserialization legacy):** Earlier session 2026-05-26 07:45 UTC poisoned messages drained sạch (queue depth 0 confirmed at 03:06 UTC). Issue self-resolved.
  AC item "Local RST PASS" + "docker-build-push re-enable" flipped to checked. AWS stack stays stopped post-verify (Free Tier hours). Per `release-deploy-standard.md` §9 + `agent-aws-access.md` Tier 3 mandate, Phase 2+3 deploy execution defer next session user-trigger.
- **2026-05-22 (Day 5 — AWS support engagement progress, 3-message thread review via Gmail MCP):** Reviewed case `177903869600100` correspondence thread (3 most recent messages 01:15 / 04:03 / 12:06 UTC 22/05) via Gmail MCP. Cumulative status:
  - **Ginnette S. (01:15 UTC)** — initial reinstatement guidance: account on hold pending document verification; user must find secure one-time link sent to `mvann1207@gmail.com` on suspension day; 2-hour timer starts on first upload; SES production access case 177857212400418 advised to resume DIRECTLY in that case after reinstatement (Ginnette praised use case as "thorough — transactional email + bounce/complaint + PDPL compliance").
  - **Naman D. (04:03 UTC)** — re-issued secure email link at **04:00 UTC 22/05** (check spam folder + mark "not spam"); enumerated required documents: **(1) bank statement** (proof of payment instrument ownership) + **(2) utility bill** (water/phone/electricity); file criteria PNG/JPG/PDF only, PDF max 25 pages, max 4MB; document specimen at https://upload.aws.amazon.com/
  - **Karina R. (12:06 UTC, LATEST)** — answered user question "CCCD Việt Nam có thay được utility bill không?": "Please go ahead and upload all the documents you currently have available. Once uploaded, our internal verification service team will review them and determine if they are sufficient and advise on the next steps." → **AWS không xác nhận CCCD accepted, đề nghị upload thử + chờ verdict**.
  - **Open action items (user-side, NOT agent):**
    1. Tìm secure link AWS email gửi 04:00 UTC 22/05 (inbox + spam + mark "not spam")
    2. Chuẩn bị bank statement (MB MasterCard end 53 đã prep `.log/aws-kyc-ready/` per session memory)
    3. Chuẩn bị CCCD ảnh PNG/JPG (Karina khuyến nghị upload thay utility bill)
    4. Upload qua secure link (timer 2h sau first upload)
    5. Đợi verification team review (~24h thường)
    6. Sau reinstate: resubmit SES production access case 177857212400418 với use case detail (Ginnette đã praise content trong message 01:15)
  - **SES Production Access case 177857212400418 status (cross-ref Gmail):** **DENIED 2026-05-19 11:04 UTC** ("We are unable to grant your request at this time") — SES stuck in sandbox mode; resubmit blocked until account reinstated.
  - **Cross-reference với Wave 104.5/105 session work:**
    - GAP-717 (JWT_CHALLENGE_SECRET prod parity) verify still blocked by này; code/IaC can ship Wave 105 Bucket E0, live verify defer post-reinstate
    - SES production cutover plan needs fallback (Resend đang chuẩn bị per ADR-025 Stream A)
    - Wave 105 Bucket E0 scope (terraform secret + IAM grant + deploy script env injection) executable now WITHOUT AWS access (IaC only); verification post-reinstate
  - **Day 5 escalation trajectory:** Day 1 → Day 4 = 96h+ no response (per 2026-05-21 log entry below); Day 5 reply burst 22/05 (Ginnette + Naman + Karina, 3 messages within 11h) = AWS support actively engaged; estimate reinstate ~24-72h post upload IF documents accepted. Decision per Day 4 stays: KHÔNG tạo AWS account #2 (duplicate detection risk), persist current path.
  - Status stays **OPEN** per `gap-done-discipline.md` §2 — AC #1 (AWS account active) chưa thỏa; gap closes only sau khi `aws sts get-caller-identity --profile dev-admin` returns valid Account 906286017800.
- **2026-05-25 (Day 8 UNBLOCK — AWS hold removed):** Email `no-reply@amazonaws.com` lúc 2026-05-25T03:39:13Z subject "Your Amazon Web Services (AWS) Account" thông báo: "We reviewed your account and removed the temporary hold." Hold tổng cộng ~8 ngày (2026-05-17 16:50 → 2026-05-25 03:39 UTC). Cảnh báo từ AWS: "your EBS volumes may have been snapshotted + Fast Snapshot Restore have been disabled" — cần verify EBS state khi resume infra. Status flip 🟡 PARTIAL 5% → 30% — AWS account hết suspend nhưng các AC vẫn pending:
  - [x] AWS removed temporary hold (email confirm 2026-05-25T03:39 UTC)
  - [ ] `aws sts get-caller-identity --profile dev-admin` PASS verify
  - [ ] EBS volume state verify post-restore (`aws ec2 describe-volumes`)
  - [ ] RDS instance state verify post-restore (`aws rds describe-db-instances`)
  - [ ] CloudTrail `IsLogging=true` verify per `aws-observability-first.md` (`aws cloudtrail get-trail-status`)
  - [ ] ECR repos + image state verify (`aws ecr describe-repositories`)
  - [x] Local RST (full-stack `kitehub/scripts/up.sh --profile full` + smoke) PASS — gate trước resume AWS push (verified 2026-05-27 02:55 UTC per GAP-756 Phase 1 — chi tiết Log entry 2026-05-27)
  - [ ] terraform import `jwt_challenge_secret` per GAP-717 unblock
  - [ ] Wave 91 Bucket F live verify post-restore
  - [ ] 3 admin v1 controllers Wave 92 live verify
  - [x] `docker-build-push.yml` `push:main` + `tags:v*.*.*` triggers RE-ENABLE (re-enabled 2026-05-27 cùng PR với GAP-756 Phase 1 RST PASS verify)

  **Per user direction 2026-05-25:** "cancel tạm thời các CI có push lên AWS cho đến khi RST local thành công" → `docker-build-push.yml` `push:main` + `tags` triggers commented out trong cùng PR; `pull_request` (push:false build verification only) + `workflow_dispatch` (manual) preserved. Re-enable mechanism: uncomment block sau khi local RST PASS — gap follow-up step trong session khi đến đó.

  **Other AWS-push CI workflows** (`deploy-production.yml` + `terraform-apply.yml` + `ec2-bootstrap.yml` + `rollback.yml`) đã chỉ `workflow_dispatch` trigger (user-confirm `APPLY`/`BOOTSTRAP`) — đã gated. Không cần disable thêm.

  **Follow-up post-RST unblock cluster** (separate gaps đã filed pre-suspension): GAP-717 terraform import jwt_challenge_secret + GAP-257 restore drill + GAP-144 AlertManager + AWS SNS + Wave 91 Bucket F live verify + 3 admin v1 controllers Wave 92 live verify.

  Status stays **PARTIAL 30%** until 11 AC trên all PASS per `gap-done-discipline.md` §2.

- **2026-05-21 (Item 1-4 brainstorm + 6-agent outside-in synthesis):** User raised 4 decisions via `documents/action-2.md` lines 68-71: (1) tạo AWS account #2 với identity khác (RISKY — recommend LEGIT chính danh sau khi #1 resolved), (2) hardcoded ID → variable refactor, (3) rebuild SOP playbook, (4) self-test full local trước AWS. Spawned 6 parallel agents (3 outside-in items 3+4: failure-mode matrix F-1 + external benchmark E-1 + persona simulation P-1; 3 audit item 2: docs-A + infra-B + design-C). Synthesis output: **(a) 8 failure-mode classes** Wave 60-91 (top 3: Class 4 config-drift / Class 8 audit baseline / Class 2 Postgres binding); **(b) 13-step rebuild SOP ~2h cold-start** with 5 mandatory gates; **(c) hardcoded audit findings** ~95 files / 5,500+ occurrences across `kitehub.me`/`ap-southeast-1`/`906286017800`/secret-prefix/subdomains/emails/EC2 IDs (existing partial registry `production-env-config-registry.md` v1.1.1 covers runtime YAML scope only); **(d) env-reference.yaml design** ({{var}} mkdocs-macros + render-env-vars.sh + check-unresolved-env-vars.sh + 3-phase migration); **(e) persona drop-offs top 5 dev + top 5 beta tenant** (chị Hằng email Spam + chị Hằng dashboard empty + anh Tâm role-guard mismatch Wave 71b recurrence). **Decisions locked:** Item 1 = ✅ LEGIT chính danh sau khi #1 resolved (avoid ToS violation + duplicate detection + state loss); Items 3+4 sequencing = **Phase 0 local self-test fix → Item 2 refactor → rebuild** (eliminate Class 4 config-drift permanently before account swap). **Filed 3 follow-up gaps same session:** GAP-691 (Phase 0 local self-test investigation P0), GAP-692 (env-reference.yaml multi-env refactor META P1), GAP-693 (AWS rebuild SOP playbook P0 BLOCKED on GAP-612+691+692). Per `gap-done-discipline.md` §2 — GAP-612 stays OPEN until AWS account #1 fate determined.
- **2026-05-21 (escalation Day 4 — 96h+ no response):** Email check (Gmail MCP) + AWS Billing console (https://console.aws.amazon.com/billing/home#/account) verify session: (a) **0 suspension notification emails received** tại root `mvann1207@gmail.com` (inbox + spam + trash + all mail, sender filters: `amazon.com` / `aws.amazon.com` / `amazonaws.com` / `aws-receivables-support` / `trust-safety` since 2026-05-15) — chỉ có Free Tier alert + SES verification request + Support case correspondence stubs; (b) Billing console confirms **outstanding balance = $0.00 USD, payments due = 0** → console suspension screen text "non-payment of outstanding balance" là boilerplate sai (NOT root cause); (c) All 3 alternate contacts (Billing/Operations/Security) trước đó "None" → root email là contact duy nhất → AWS không có route nào khác để gửi notification → loại trừ giả thuyết "email gửi tới billing contact khác". Defensive actions taken: (1) Set 3 alternate contacts (Billing/Operations/Security) = `mvann1207@gmail.com` với phone `+84968727926`, title `Owner`; (2) Reply case 177903869600100 trong Support Center UI với 9 bullet evidence (billing $0 + 0 notification + alternate contacts confirmed + business impact + 4 explicit requests including identity-verification doc list willingness). Pending Twitter @AWSSupport escalation. Hypothesis updated: AWS Trust & Safety preemptive suspend pattern "new account 10 ngày tuổi + SES Production Access request (2026-05-12) + Free Tier 85% spike (2026-05-17 04:09 UTC) + SES email verification 16:44 UTC → suspend ~16:50 UTC" — looks-like-abuse heuristic, NOT actual non-payment. **Decision: KHÔNG tạo AWS account mới** (duplicate detection blast radius cao + mất terraform state/CloudTrail history/ECR images/RDS data/ACM cert account cũ + permanent ban risk nếu AWS treat làm evade-suspension); persist escalation thêm 24-48h trước khi cân nhắc Business Support upgrade ($29-100/mo) làm last resort. Status stays OPEN (per `gap-done-discipline.md` §2 — AC #1 AWS account active chưa thỏa). Per `agent-aws-access.md` §5 logging mandate.
- **2026-05-17 (case opened):** AWS Support case **177903869600100** opened by user via Account Activation flow with full reinstate request body (use case details + cross-ref to existing case 177857212400418). AWS responded with first correspondence within 5 minutes (likely auto-acknowledge OR agent first-pass). User must login Support Center to read response content (suspension blocks resources but NOT support console access). Existing SES case 177857212400418 also received new correspondence same window — likely AWS consolidating both cases. Awaiting user-pasted reply content for next-step analysis.
- **2026-05-17:** Gap filed during Wave 90 walkthrough. Recovery sequence documented for user resume. Session ended mid-walkthrough; 7 walkthrough gaps (GAP-605..611) parked until AWS restoration.
