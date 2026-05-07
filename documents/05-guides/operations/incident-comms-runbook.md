# Incident Communication Runbook

**Wave:** 38 Bucket C (GAP-373)
**Status:** Active 2026-05-07
**Owner:** @nguyenvankiet (solo-dev, acting incident commander)
**Related:** [`post-mortem-template.md`](post-mortem-template.md), [`ADR-027 statuspage vendor`](../../../02-architecture/adr/ADR-027-statuspage-vendor.md), GAP-370 email infra

---

## 1. Bối cảnh + Scope

Phase 1 BETA invite-only (10-20 tenants) cần official incident communication channel để:
- Tenants biết có downtime — không phải email-blast 1-on-1
- Public history of incidents → SLA tracking
- Professional appearance vs ad-hoc Slack/email

**Scope:** Mọi production incident touch user-facing surfaces (KiteHub API, KiteClass API, marketing site, auth, email delivery, AI Branding generation). Internal infrastructure incidents (CI failures, dev environment) KHÔNG cần status-page disclose.

---

## 2. Roles

Phase 1 BETA solo-dev mode:

| Role | Person | Responsibility |
|---|---|---|
| **Incident Commander (IC)** | @nguyenvankiet | Triage + decision authority + final RCA |
| **Communicator** | @nguyenvankiet | Status page updates + tenant emails |
| **Technical lead** | @nguyenvankiet | Diagnose + fix |
| **Subscriber notifications** | Instatus native (per ADR-027) | Email delivery to opt-in tenants |

Post-team-growth: split IC + Communicator + Technical roles.

---

## 3. Severity Levels

| Severity | Definition | Examples | Status update cadence | Target time-to-resolve |
|---|---|---|---|---|
| **🔴 Critical (Sev1)** | Full or large-scale outage; tenant cannot use core features | KiteHub API 5xx >50% requests; database down; auth 100% fail | Every 15 min | <2h |
| **🟠 Major (Sev2)** | Partial outage or feature-specific degradation | Single tenant slug broken; AI Branding generation fail; payment processor timeout | Every 30 min | <8h |
| **🟡 Minor (Sev3)** | Degraded experience; workaround exists | Slow load >5s; non-critical email delay; UI glitch | Every 2h | <24h |
| **🔵 Maintenance** | Planned downtime announced ≥48h ahead | Scheduled DB migration; SSL cert renewal | Initial post + completion | Per schedule |

---

## 4. 6-Step Incident Procedure

### Step 1 — Detect

Trigger sources (Phase 1 BETA):
- Grafana alert fires (post-Wave 37 GAP-115/135 monitoring)
- Sentry exception spike (per GAP-XXX Sentry integration)
- Tenant report via email/Slack
- Cloudflare DDoS spike (per GAP-371 CDN)
- Smoke test failure post-deploy (per GAP-377)

IC acknowledges within **5 min** (Phase 1 BETA SLA — solo dev awake hours; off-hours best-effort).

### Step 2 — Triage

IC assesses within **5-10 min**:
1. Severity per §3 table
2. Affected components (link với Instatus components)
3. Affected tenants (all? subset? specific tenant?)
4. Initial root cause hypothesis (1 sentence)

Decision: post incident to status page? (Yes for Sev1/Sev2 always; Sev3 if user-visible)

### Step 3 — Post Initial Incident

Within **5 min** of triage decision:

1. Open Instatus admin → "New incident"
2. Select severity + affected components
3. Status: Investigating
4. Message template (Vietnamese):

```markdown
🔍 **Đang điều tra:** [Tên component/feature] hoạt động bất thường

Chúng tôi đã ghi nhận sự cố ảnh hưởng [scope — tất cả/một số tenant]
liên quan đến [feature]. Đội ngũ kỹ thuật đang điều tra nguyên nhân.

Triệu chứng đã quan sát:
- [observation 1]
- [observation 2]

Cập nhật tiếp theo: trong vòng [15/30/120] phút.

Thời điểm phát hiện: [HH:MM ICT]
```

5. Subscribers nhận email tự động (Instatus native delivery — không qua GAP-370 email infra).

### Step 4 — Update Cadence

Per §3 table cadence (Sev1: 15 min, Sev2: 30 min, Sev3: 2h):

```markdown
🔧 **Đang xử lý:** Đã xác định nguyên nhân — [1 line]

Tiến độ:
- [HH:MM] Phát hiện
- [HH:MM] Xác định nguyên nhân: [root cause]
- [HH:MM] Đang triển khai fix: [action]

ETA hoàn thành: [HH:MM ICT]
```

**Quy tắc:** thà ghi "vẫn đang điều tra" còn hơn để tenant tự đoán. Im lặng = mất niềm tin.

### Step 5 — Resolve

Khi service hồi phục:

1. Verify smoke test pass (per `scripts/smoke-test.sh`)
2. Update Instatus → status Resolved
3. Final message:

```markdown
✅ **Đã giải quyết:** [Component/feature] hoạt động bình thường trở lại

Tóm tắt:
- Thời gian sự cố: [HH:MM] - [HH:MM] ICT (~[X] phút)
- Nguyên nhân: [1 sentence summary]
- Tác động: [scope]
- Khắc phục: [action taken]

Báo cáo phân tích chi tiết (post-mortem) sẽ được gửi tới các tenant
đăng ký trong vòng 48 giờ.

Cảm ơn quý khách đã thông cảm.
```

### Step 6 — Post-mortem

Within **48h** of resolution:

1. Use [`post-mortem-template.md`](post-mortem-template.md)
2. Fill RCA + timeline + impact + action items
3. Save to `documents/04-quality/incidents/INC-YYYY-MM-DD-{shortname}.md`
4. Email summary to subscribers (manual Phase 1; automate Phase 2 via GAP-370 email infra)

---

## 5. Message Templates Library

Stored inline §4 above. Adapt theo severity + component.

For maintenance announcements (Sev Maintenance), post **≥48h ahead**:

```markdown
🔵 **Bảo trì theo lịch:** [Component] sẽ tạm dừng [HH:MM]-[HH:MM] ICT ngày [DD/MM/YYYY]

Lý do: [reason]
Tác động dự kiến: [scope]
Kế hoạch dự phòng: [if any]

Mong quý khách lưu ý sắp xếp công việc.
```

---

## 6. Subscriber Notification Flow

**Phase 1 BETA — Instatus native (per ADR-027):**
- Tenants opt-in via Instatus public page subscribe form
- Email delivery handled by Instatus (no integration với GAP-370)
- Free tier: 100 subscribers cap (Phase 1 BETA invite-only ~10-20 — comfortable)

**Phase 2 considerations (if migrate self-hosted):**
- Migrate subscriber list (Instatus JSON export)
- Wire post-mortem auto-email through GAP-370 kitehub-email + Outbox pattern
- Add SMS for Sev1 (optional enhancement)

---

## 7. SLA Targets

| Phase | Uptime target | Notes |
|---|---|---|
| **Phase 1 BETA** | **99.5%** | Invite-only beta; ~3.6h downtime/month allowance; solo dev off-hours best-effort |
| **Phase 1.5 PAID** | 99.7% | Public paid launch; on-call rotation considered |
| **Phase 2** | 99.9% | Production maturity; team scale; multi-AZ Architecture B2 |

SLA breach = >0.5% over rolling 30-day window. Track on Instatus uptime metrics.

---

## 8. Cross-references

- [`post-mortem-template.md`](post-mortem-template.md) — RCA template
- [`ADR-027 statuspage vendor`](../../../02-architecture/adr/ADR-027-statuspage-vendor.md) — Instatus vendor decision
- GAP-370 email infra — Phase 2 subscriber notifications via kitehub-email Outbox
- GAP-371 CDN — Cloudflare attack analytics (incident detection signal)
- GAP-377 smoke test — resolution verification gate
- `documents/05-guides/operations/audit-chain-break-runbook.md` — sister runbook (audit-chain incident-specific)
- `documents/05-guides/operations/disaster-recovery-plan.md` — full DR scope (Sev1 super-set)

---

## 9. Log

- **2026-05-07:** Runbook created Wave 38 Bucket C (coordinator-applied sau Sonnet agent autocompact-thrash). Severity levels + 6-step procedure + subscriber notification flow + SLA targets per Phase 1 BETA scope. Cross-link với ADR-027 vendor + post-mortem template.
