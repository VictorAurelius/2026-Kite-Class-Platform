# Resend Paid Upgrade Runbook — Free → Pro $20/month

**Audience:** Solo dev quản lý Resend account khi Phase 1 BETA chuyển sang Phase 1.5 invite ≥20 tenants → Free tier 100/day = 3000/month không đủ.
**Standards:** AWS Well-Architected (Cost Optimization + Reliability) · ADR-025 Stream A · `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2.
**Cross-link upstream:** `06-resend-account-setup.md` (initial Free setup) + `documents/05-guides/operations/secrets-rotation-runbook.md` (API key rotation).
**Cross-link downstream:** `documents/05-guides/operations/email-deliverability-runbook.md` (SPF/DKIM/DMARC tuning hậu upgrade).
**Estimated time:** ~15 min (signup card + upgrade + verify) + 5 min monitoring config.
**Last-Updated:** 2026-05-16

---

## TL;DR

> Khi Phase 1.5 invite ≥20 tenants → Resend Free 3000 emails/month không đủ → upgrade Resend Pro $20/month 50k emails/month → D-14 trước Phase 1.5 invite plan để có buffer monitoring.

Quick path 5 bước:

1. **Check trigger condition** (§1): ≥20 tenants planned hoặc current usage >70% Free tier
2. **Login Resend dashboard** → Settings → Billing → Upgrade to Pro
3. **Add payment method** (credit card; recommend dùng virtual card limit $50 cho budget guardrail)
4. **Confirm upgrade** → verify new limit `50,000 emails/month` hiển thị
5. **Update internal docs** (this runbook §5 monitoring) + monitor 7d post-upgrade

---

## 1. Trigger conditions (when to upgrade)

Upgrade Resend Free → Pro KHI và CHỈ KHI một trong các điều kiện sau:

| Trigger | Threshold | Source |
|---|---|---|
| **Phase 1.5 invite planned ≥20 tenants** | 20 invite × ~30 emails/tenant first 30d = 600 → cumulative khi growing có thể vượt 3000/tháng | Wave 86 plan §3 Bucket E pre-Phase 1.5 readiness |
| **Current usage >70% Free tier** | >70 emails/day average (×30 = 2100/month) | Resend dashboard "Usage" tab |
| **Daily spike >80 emails** | Single day approach 100/day Free limit; risk hit limit + downtime | Resend dashboard alerts |
| **Custom features needed** | Multiple reply-to domains, custom domain Send-As, higher rate limit | Pro plan unlocks |

⚠️ **DON'T upgrade prematurely**: Phase 1 BETA ≤5 tenants × ≤20 emails/day = 100/day = Free tier đủ. Premature upgrade = $240/year burn rate before need.

---

## 2. Pre-upgrade checklist

| Item | Verify | Why |
|------|--------|-----|
| Current Resend account active | Login [resend.com](https://resend.com) → Dashboard accessible | Cần admin account để upgrade |
| `06-resend-account-setup.md` complete | Domain `kitehub.me` `Verified` + API key trong Secrets Manager | Tránh re-setup post-upgrade |
| Payment method ready | Credit card OR virtual card (recommend Privacy.com / Wise debit virtual) | Pro = recurring $20/month USD |
| Budget approval | Solo-dev: self-approve $240/year; pair-/team: budget owner approve | Avoid surprise expense |
| Current usage snapshot | Screenshot Resend dashboard "Usage" tab last 30d | Baseline for post-upgrade comparison |
| Beta tenant count forecast | Phase 1.5 plan ≥20 tenants → confirm timeline | Justify upgrade D-14 trước invite blast |

---

## 3. Step-by-step upgrade

### 3.1 Login + navigate (~3 min)

1. Mở [resend.com/settings/billing](https://resend.com/settings/billing) (cần login)
2. Current plan hiển thị: `Free — 100 emails/day, 3,000/month`
3. Click "Upgrade to Pro" button

### 3.2 Plan selection (~2 min)

| Plan | Limit | Price | Use case |
|---|---|---|---|
| Free | 100/day, 3000/month, 1 domain | $0 | Phase 1 BETA ≤5 tenants |
| **Pro** | **50,000/month, 10 domains, 24h support** | **$20/month** | **Phase 1.5 invite ≥20 tenants ← chọn này** |
| Business | 100k/month, dedicated IP option | $80/month | Phase 2 multi-cohort production |

Click "Choose Pro".

### 3.3 Payment method (~5 min)

1. Form payment hiển thị: Card number, expiry, CVC, billing address
2. Recommend virtual card: tạo qua Privacy.com (US) hoặc Wise (international) với limit $50/month — guardrail nếu Resend over-charge
3. Billing address: dùng địa chỉ cá nhân hoặc trung tâm (TP.HCM/Hà Nội)
4. Submit → Stripe charges $20 immediately (pro-rated nếu mid-month)

### 3.4 Verify upgrade (~3 min)

1. Sau khi Stripe confirm → Resend dashboard reload
2. Plan label hiển thị: `Pro — 50,000 emails/month`
3. Send test email qua API:
   ```bash
   curl -X POST https://api.resend.com/emails \
     -H "Authorization: Bearer $RESEND_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "from": "noreply@kitehub.me",
       "to": "admin@kitehub.me",
       "subject": "Resend Pro upgrade verify",
       "html": "Pro plan active từ '"$(date +%Y-%m-%d)"'"
     }'
   ```
   → Expect HTTP 200 + `id` trong response.
4. Check inbox `admin@kitehub.me` → email tới trong <30s.

### 3.5 Update internal records (~2 min)

1. Update `documents/05-guides/account-prep/06-resend-account-setup.md` §"Pro upgrade trigger" — change status từ "anticipated" → "DONE YYYY-MM-DD"
2. Update `release-1-plan-2026.md` if Phase 1.5 invite timeline references Resend capacity
3. Append entry to memory `feedback_resend_paid_upgrade.md` if applicable
4. CloudWatch dashboard / Grafana panel: update Resend usage metric scale (50k upper bound thay vì 3k)

---

## 4. Post-upgrade monitoring (7d)

| Day | Check | Action |
|---|---|---|
| D+1 | Resend "Usage" tab shows daily send count tracking accurately | Verify no double-charge |
| D+3 | No 429 rate-limit errors trong application logs | Confirm Pro limit applied |
| D+7 | Stripe statement shows single $20 charge (no surprise) | Validate billing |
| D+30 | Monthly aggregate < 50,000 emails | Validate cap appropriate for current scale |

---

## 5. Rollback (downgrade) procedure

Nếu trigger không materialize (vd. Phase 1.5 launch trì hoãn, tenants <10 sau 60d):

1. Resend dashboard → Settings → Billing → "Downgrade to Free"
2. ⚠️ Warning: emails sent over Free limit current month có thể bị throttled
3. Confirm → effective at next billing cycle
4. Stripe stops charging next month
5. Verify dashboard plan label = `Free` post-cycle

Recommend: KHÔNG downgrade trừ khi confirmed tenants <5 active over 30d (Phase 1 BETA scope) — Pro cushion cheap insurance ($20/month) vs risk hit limit during outreach blast.

---

## 6. Cost projection (Phase 1 → Phase 2)

| Phase | Tenants | Emails/month avg | Plan | Monthly cost |
|---|---|---|---|---|
| Phase 1 BETA (Tuần 1-12) | 1-5 | <1000 | Free | $0 |
| Phase 1.5 PAID invite (Tuần 13-18) | 5-20 | 1500-5000 | **Pro** | **$20** |
| Phase 2 medium-center (Tuần 19-30) | 20-50 | 5000-20000 | Pro | $20 |
| Phase 3 K-12 (Tuần 31+) | 50-100 | 20000-50000 | Pro near cap | $20 → eval Business $80 |
| Phase 4 scale | 100+ | >50000 | Business | $80+ |

Annual: Phase 1.5+ = $240/year Resend Pro baseline; reasonable cost for email-deliverability infrastructure.

---

## 7. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Stripe charge declined | Card limit / wrong billing address | Update payment method; retry; check virtual card balance |
| Resend dashboard still shows "Free" post-payment | Caching / propagation 1-2 min | Logout + login; if still "Free" sau 5 min → contact support@resend.com |
| Email still rate-limited 429 sau upgrade | Stale API key cached / wrong key | Verify `kitehub/production/resend-api-key` Secrets Manager value matches dashboard "API Keys" tab |
| Got billed twice in same month | Stripe pro-rate first month + next regular month — normal | Check Stripe dashboard charges; if true double-charge → support@resend.com refund |
| Want to test before commit | Resend Free trial 14d Pro features (sometimes promo) | Check dashboard banner cho promo; otherwise commit $20 first month — refundable nếu cancel <30d |

---

## 8. Related documents

- `06-resend-account-setup.md` — initial Free setup (prerequisite)
- `documents/05-guides/operations/secrets-rotation-runbook.md` — quarterly API key rotation
- `documents/05-guides/operations/email-deliverability-runbook.md` — SPF/DKIM/DMARC tuning (GAP-533)
- `documents/03-planning/roadmap/release-1-plan-2026.md` — Phase 1.5 invite plan timeline
- `release-deploy-standard.md` §3.4 — MAJOR release artifact (transactional email)
- Wave 86 plan §3 Bucket H H-AC6
