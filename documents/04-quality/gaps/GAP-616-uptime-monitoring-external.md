# GAP-616 — Uptime monitoring external (UptimeRobot / BetterStack integration)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-05-18 (Wave 92 Bucket E meta backlog filing — carry-forward từ Wave 90 ROADMAP §🚀 long-term P2/P3 follow-ups line 158)
**Affects:** Long-term observability posture; phát hiện outage độc lập với AWS CloudWatch in-account monitoring; không phải Phase 1 BETA blocker, Phase 1.5+ candidate

## Problem

Hiện tại observability stack hoàn toàn dựa vào AWS CloudWatch (alarms + dashboard + SNS) trong cùng AWS account 906286017800. Khi account bị suspended (GAP-612 sự cố 2026-05-17), toàn bộ in-account monitoring "đi cùng" với production:
- CloudWatch alarms không fire vì account suspended
- SNS topics không deliver
- Dashboard không truy cập được
- Không có cảnh báo độc lập từ bên ngoài để dev biết "production xuống"

Outside-in audit (ngoài-vào) — user-facing uptime monitoring từ external probe — hoàn toàn thiếu. Beta user / future tenant gặp 5xx hoặc 522 sẽ không có channel độc lập để verify "đây là KiteHub xuống chứ không phải mạng của tôi" trừ khi truy cập statuspage.io (cũng external) — nhưng statuspage không có active uptime probe.

Cụ thể context Wave 90 ROADMAP entry "Long-term follow-ups: P2 uptime monitoring + P2 DR plan" — gap này codify P2 uptime portion thành actionable gap.

## Root Cause

Solo-dev mode + Phase 1 BETA scope ưu tiên trong-AWS observability (CloudTrail + CloudWatch — Wave 84-85 GAP-437/414). External uptime probe được defer vì:
- Không phải Phase 1 BETA blocker (chưa có tenant onboarded)
- Chi phí cognitive load + integration thêm 1 vendor mới
- Free tier UptimeRobot / BetterStack đủ cho 5-10 probe — chi phí $0
- Trigger event (GAP-612 AWS suspension) chỉ vừa surface 2026-05-17, lesson-learned mới rút ra

## Proposed Fix

### Phase 1 — Pick vendor (~30 phút research)

**Option A: UptimeRobot Free Tier**
- 50 monitors, 5-phút check interval, email alert
- Public status page included
- Limitation: no SSL cert expiry monitoring, no incident timeline retention >2 tháng
- Sign-up: <https://uptimerobot.com/>
- Cost: $0/tháng

**Option B: BetterStack (formerly Better Uptime) Free Tier**
- 10 monitors, 3-phút check interval, email + Slack alert
- Public status page included
- 90-day incident timeline retention
- SSL cert expiry monitoring built-in
- Sign-up: <https://betterstack.com/>
- Cost: $0/tháng (free tier); $25/tháng (10 monitors + 30s interval + SMS) khi scale

**Recommend BetterStack** vì 90-day retention + SSL monitoring + 3-phút interval better cho Phase 1.5+ scale; vẫn free tier.

### Phase 2 — Configure monitors (~1h)

Monitor scope:
1. `https://kitehub.me/` — apex landing page (200 OK)
2. `https://kitehub.me/api/v1/auth/health` — backend health endpoint (200 OK + JWT-ready)
3. `https://kitehub.me/api/v1/admin/health` — admin path health
4. `https://kitehub.me/api/v1/beta-signup` — beta cohort entry endpoint
5. SSL cert expiry alert (30-day warning)

Alert channels:
- Email tới `vannkite@outlook.com`
- (Phase 1.5+) Slack webhook khi join team

### Phase 3 — Public statuspage (~30 phút)

- Create BetterStack public statuspage at `status.kitehub.me` (subdomain)
- Embed link trong tenant-facing docs (Phase 1.5+) + admin dashboard footer
- Reference URL từ `/beta-status` route (đã exist per `documents/05-guides/user-manual/anonymous/` Phase 1 sample)

### Phase 4 — Document runbook (~30 phút)

Create `documents/05-guides/operations/external-uptime-monitoring.md`:
- Vendor login + API key rotation
- Add/remove monitor procedure
- Incident triage flow (BetterStack alert → CloudWatch correlation → SSM verify)
- Cross-reference với `incident-response-runbook.md`

## Acceptance Criteria

- [ ] Vendor selected (BetterStack free tier recommended) + account signed up
- [ ] 4 monitors configured cho production endpoints (apex, auth health, admin health, beta-signup)
- [ ] SSL cert expiry monitoring active (30-day warning)
- [ ] Alert channel = email `vannkite@outlook.com` verified delivery
- [ ] Public statuspage `status.kitehub.me` accessible (subdomain DNS cấu hình via Cloudflare)
- [ ] Runbook `documents/05-guides/operations/external-uptime-monitoring.md` shipped
- [ ] ROADMAP entry "Long-term follow-ups: P2 uptime monitoring" flipped DONE
- [ ] Self-test: simulate downtime (stop EC2 staging instance 5 phút) → verify alert fires within 10 phút

## Related

- **Wave 92 plan:** [`documents/03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md`](../../03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md) §3 Bucket E
- **Sister gaps:** GAP-617 (DR plan) + GAP-618 (AWS Health daily check) — long-term observability cluster
- **Trigger event:** GAP-612 AWS account suspension 2026-05-17 — surfaced gap "không có external alert khi account-level fail"
- **Wave 90 ROADMAP §🚀:** line 158 "Long-term follow-ups: P2 uptime monitoring + P2 DR plan" — gap formalize P2 uptime portion
- **Rule:** `audit-to-gap-pipeline.md` §3 (gap template); `output-review-mandate.md` §3 (ops-readiness audit standard)
- **Cross-link runbook:** `documents/05-guides/operations/incident-response-runbook.md` — future §X external monitor section

## Log

- **2026-05-18:** Gap filed by Wave 92 Bucket E meta backlog filing per inside-out audit ROADMAP §"Long-term P2/P3 follow-ups" carry-forward từ Wave 90 line 158. Trigger event = GAP-612 AWS suspension surfaced gap "in-account monitoring đi cùng production khi account-level fail". Defer implementation Wave 93+ hoặc Phase 1.5 — không phải Phase 1 BETA blocker (chưa có tenant onboarded).
