# GAP-612 — AWS account suspended (verification pending); production stack stopped

**Status:** 🟡 PARTIAL (5% — alt contacts set + case 177903869600100 replied with evidence; awaiting AWS response)
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

- **2026-05-21 (Item 1-4 brainstorm + 6-agent outside-in synthesis):** User raised 4 decisions via `documents/action-2.md` lines 68-71: (1) tạo AWS account #2 với identity khác (RISKY — recommend LEGIT chính danh sau khi #1 resolved), (2) hardcoded ID → variable refactor, (3) rebuild SOP playbook, (4) self-test full local trước AWS. Spawned 6 parallel agents (3 outside-in items 3+4: failure-mode matrix F-1 + external benchmark E-1 + persona simulation P-1; 3 audit item 2: docs-A + infra-B + design-C). Synthesis output: **(a) 8 failure-mode classes** Wave 60-91 (top 3: Class 4 config-drift / Class 8 audit baseline / Class 2 Postgres binding); **(b) 13-step rebuild SOP ~2h cold-start** with 5 mandatory gates; **(c) hardcoded audit findings** ~95 files / 5,500+ occurrences across `kitehub.me`/`ap-southeast-1`/`906286017800`/secret-prefix/subdomains/emails/EC2 IDs (existing partial registry `production-env-config-registry.md` v1.1.1 covers runtime YAML scope only); **(d) env-reference.yaml design** ({{var}} mkdocs-macros + render-env-vars.sh + check-unresolved-env-vars.sh + 3-phase migration); **(e) persona drop-offs top 5 dev + top 5 beta tenant** (chị Hằng email Spam + chị Hằng dashboard empty + anh Tâm role-guard mismatch Wave 71b recurrence). **Decisions locked:** Item 1 = ✅ LEGIT chính danh sau khi #1 resolved (avoid ToS violation + duplicate detection + state loss); Items 3+4 sequencing = **Phase 0 local self-test fix → Item 2 refactor → rebuild** (eliminate Class 4 config-drift permanently before account swap). **Filed 3 follow-up gaps same session:** GAP-691 (Phase 0 local self-test investigation P0), GAP-692 (env-reference.yaml multi-env refactor META P1), GAP-693 (AWS rebuild SOP playbook P0 BLOCKED on GAP-612+691+692). Per `gap-done-discipline.md` §2 — GAP-612 stays OPEN until AWS account #1 fate determined.
- **2026-05-21 (escalation Day 4 — 96h+ no response):** Email check (Gmail MCP) + AWS Billing console (https://console.aws.amazon.com/billing/home#/account) verify session: (a) **0 suspension notification emails received** tại root `mvann1207@gmail.com` (inbox + spam + trash + all mail, sender filters: `amazon.com` / `aws.amazon.com` / `amazonaws.com` / `aws-receivables-support` / `trust-safety` since 2026-05-15) — chỉ có Free Tier alert + SES verification request + Support case correspondence stubs; (b) Billing console confirms **outstanding balance = $0.00 USD, payments due = 0** → console suspension screen text "non-payment of outstanding balance" là boilerplate sai (NOT root cause); (c) All 3 alternate contacts (Billing/Operations/Security) trước đó "None" → root email là contact duy nhất → AWS không có route nào khác để gửi notification → loại trừ giả thuyết "email gửi tới billing contact khác". Defensive actions taken: (1) Set 3 alternate contacts (Billing/Operations/Security) = `mvann1207@gmail.com` với phone `+84968727926`, title `Owner`; (2) Reply case 177903869600100 trong Support Center UI với 9 bullet evidence (billing $0 + 0 notification + alternate contacts confirmed + business impact + 4 explicit requests including identity-verification doc list willingness). Pending Twitter @AWSSupport escalation. Hypothesis updated: AWS Trust & Safety preemptive suspend pattern "new account 10 ngày tuổi + SES Production Access request (2026-05-12) + Free Tier 85% spike (2026-05-17 04:09 UTC) + SES email verification 16:44 UTC → suspend ~16:50 UTC" — looks-like-abuse heuristic, NOT actual non-payment. **Decision: KHÔNG tạo AWS account mới** (duplicate detection blast radius cao + mất terraform state/CloudTrail history/ECR images/RDS data/ACM cert account cũ + permanent ban risk nếu AWS treat làm evade-suspension); persist escalation thêm 24-48h trước khi cân nhắc Business Support upgrade ($29-100/mo) làm last resort. Status stays OPEN (per `gap-done-discipline.md` §2 — AC #1 AWS account active chưa thỏa). Per `agent-aws-access.md` §5 logging mandate.
- **2026-05-17 (case opened):** AWS Support case **177903869600100** opened by user via Account Activation flow with full reinstate request body (use case details + cross-ref to existing case 177857212400418). AWS responded with first correspondence within 5 minutes (likely auto-acknowledge OR agent first-pass). User must login Support Center to read response content (suspension blocks resources but NOT support console access). Existing SES case 177857212400418 also received new correspondence same window — likely AWS consolidating both cases. Awaiting user-pasted reply content for next-step analysis.
- **2026-05-17:** Gap filed during Wave 90 walkthrough. Recovery sequence documented for user resume. Session ended mid-walkthrough; 7 walkthrough gaps (GAP-605..611) parked until AWS restoration.
