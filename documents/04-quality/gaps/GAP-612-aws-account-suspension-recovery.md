# GAP-612 — AWS account suspended (verification pending); production stack stopped

**Status:** 🔵 OPEN
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

- **2026-05-17:** Gap filed during Wave 90 walkthrough. Recovery sequence documented for user resume. Session ended mid-walkthrough; 7 walkthrough gaps (GAP-605..611) parked until AWS restoration.
