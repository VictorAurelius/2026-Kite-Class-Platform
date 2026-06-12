# Tenant Onboarding Checklist

> Last updated: 2026-04-18 | Owner: Customer Success / DevOps

End-to-end checklist để provision 1 tenant (trường học) mới từ initial contact đến first-classroom-live. Target time: **3 business days** từ signed LOI đến tenant can log in.

---

## 1. Tenant Tiers Overview

| Tier | AI Regenerates | Concurrent AI | Max Students | Price |
|------|:--------------:|:-------------:|:------------:|:-----:|
| Free | 3/session | 1 | 50 | $0 |
| Pro | 10/session | 3 | 500 | TBD |
| Enterprise | Unlimited | 10 | Unlimited | Custom |

Pricing TBD — xem [GAP-103](../04-quality/gaps/GAP-103-deploy-philosophy-aws-plugins-adr.md) + BRD pricing model.

---

## 2. Pre-onboarding (Day -3 to -1)

### 2.1 Business qualification
- [ ] Confirmed school type (K-12 / trung tâm / university)
- [ ] Target student count (determines tier)
- [ ] Compliance requirements collected (PDPL parent consent, MoET data retention)
- [ ] LOI / contract signed
- [ ] Billing info captured (if paying tier)

### 2.2 Technical prerequisites
- [ ] Desired subdomain confirmed (e.g., `school-name.kitehub.me`)
- [ ] Branding assets collected (logo, color preferences — optional)
- [ ] Primary admin email confirmed (cannot change easily post-provisioning)
- [ ] Locale confirmed (vi-VN default, en-US optional)
- [ ] Academic year structure confirmed (VN standard 08-15 to 05-31, or custom)

### 2.3 Communication
- [ ] Welcome email scheduled
- [ ] Training session booked (60 min walkthrough)
- [ ] Support channel created (Slack/Zalo shared with admin)

---

## 3. Day 1 — Provisioning

### 3.1 Create tenant instance

```bash
# Option A: Admin portal (preferred khi UI ready)
# Go to https://admin.kitehub.vn/tenants/new
# Fill: name, subdomain, tier, admin email, locale

# Option B: CLI (current)
cd kitehub
./scripts/exec.sh kitehub-admin \
  curl -X POST http://localhost:8085/api/v1/tenants \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{
      "name": "Trường ABC",
      "subdomain": "abc",
      "tier": "PRO",
      "adminEmail": "admin@abc.edu.vn",
      "locale": "vi-VN"
    }'
```

Verify:
- [ ] Tenant record created in `kitehub.tenants` table
- [ ] DNS subdomain `abc.kitehub.me` resolves (Cloudflare propagation 1-5 min)
- [ ] SSL cert issued by cert-manager (check via `curl -I https://abc.kitehub.me`)
- [ ] Initial instance status = `NOT_STARTED`

### 3.2 Run branding wizard (if tier supports)

Follow the 6-step wizard:
1. Welcome screen acknowledged
2. Logo uploaded (or skipped for default)
3. Audience selected (K-12 / Adult / Mixed)
4. Tone selected (Professional / Friendly / Energetic / Luxurious)
5. Template chosen (6 previews shown)
6. Preview approved per-resource

State transitions observed:
```
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED
```

Follow [`../.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §4 UX requirements.

### 3.3 Send admin invitation

- [ ] Invitation email sent to admin
- [ ] Admin token valid 24h (reset if admin misses window)
- [ ] Admin can complete signup via subdomain URL
- [ ] Admin 2FA enabled (MANDATORY for paying tiers)

---

## 4. Day 2 — Initial Data Setup

### 4.1 Academic year
- [ ] Academic year created (e.g., 2025-2026)
- [ ] Semesters configured (Học kỳ 1, Học kỳ 2)
- [ ] VN holidays auto-populated (placeholder dates — awaiting GAP-100 lunar calendar)
- [ ] Custom school holidays added

### 4.2 User roles
- [ ] Staff imported (admin portal or CSV)
- [ ] Role assignments verified (PRINCIPAL, TEACHER, STAFF)
- [ ] Multi-role users configured correctly

### 4.3 Classes + Subjects
- [ ] Subject catalog created (môn học)
- [ ] Class rooms defined (lớp học)
- [ ] Homeroom teacher assigned per class
- [ ] Teacher-subject-class linkage set

### 4.4 Student bulk import
- [ ] Student xlsx template downloaded
- [ ] File filled by school staff (500-row chunks)
- [ ] Dry-run executed (preview errors)
- [ ] Final import triggered
- [ ] Error report reviewed + fixes applied

Follow [`../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md`](../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) if issues.

---

## 5. Day 3 — Training + Go-Live

### 5.1 Admin training (60 min session)
- [ ] Dashboard walkthrough
- [ ] Attendance demo (mark a class)
- [ ] Grade entry demo (enter grades for 1 subject)
- [ ] Report card preview (generates PDF — Wave 10 dependency)
- [ ] Parent portal preview (Wave 2/5 — if enabled)

### 5.2 Teacher training (30 min session x N groups)
- [ ] Login flow
- [ ] Take attendance (mobile + desktop)
- [ ] Enter grades
- [ ] View schedule
- [ ] Communicate with parents (when feature ships)

### 5.3 Go-live verification
- [ ] 1 real class attendance recorded successfully
- [ ] 1 real grade entry successful
- [ ] Email notifications delivered (to admin test inbox)
- [ ] Monitoring dashboard shows tenant traffic
- [ ] Support channel responsive

---

## 6. Post-onboarding (Week 1)

### 6.1 Check-in Day 3, 7, 14
- [ ] Admin satisfaction feedback collected
- [ ] Support tickets triaged (target: 0 P0, < 5 P1-P2)
- [ ] Usage metrics captured (DAU, feature adoption)
- [ ] Any data import follow-up issues resolved

### 6.2 Tenant-specific customization (if tier allows)
- [ ] Custom email templates (Pro+ only) — GAP-021 scope
- [ ] Custom branding refresh (Enterprise only)
- [ ] API access configured (Enterprise only — GAP-038)

---

## 7. Common Issues + Fixes

### 7.1 Subdomain not resolving
Cloudflare DNS cache. Wait 5 min, or force refresh: `dig +trace abc.kitehub.me`.

### 7.2 SSL cert not issued
cert-manager webhook error. Check:
```bash
kubectl describe certificate abc-kiteclass-com -n kite-ingress
```
Common cause: rate limit from Let's Encrypt (50 certs/week/domain). Use staging issuer for testing.

### 7.3 Admin can't log in after invitation
Token expired or invitation email went to spam.
```bash
# Resend invitation
./scripts/exec.sh kitehub-admin \
  curl -X POST http://localhost:8085/api/v1/tenants/$TENANT_ID/admin/resend-invite
```

### 7.4 Bulk import fails on encoding
VN characters (dấu) không đúng encoding. Ensure xlsx saved as UTF-8 (không phải Windows-1258).

### 7.5 Branding wizard stuck at GENERATING
AI queue backlog. Check:
```bash
# Queue depth
curl -u guest:guest http://localhost:15672/api/queues/kitehub/ai.request.enterprise | jq .messages
```
If > 100, scale consumers hoặc escalate.

---

## 8. Offboarding (Tenant Churn)

Separate procedure — see future `tenant-offboarding-procedure.md` (planned). Key points:
- 90-day data retention (GDPR + VN PDPL)
- Data export before deletion (Enterprise tier right)
- Graceful deprovisioning (notify users, archive data, release subdomain)

---

## 9. SLA Commitments per Tier

| Metric | Free | Pro | Enterprise |
|--------|:----:|:---:|:----------:|
| Uptime | 99% | 99.5% | 99.9% |
| Support response | Best effort | 24h | 4h |
| Data recovery (RTO) | 72h | 24h | 4h |
| Onboarding time | Self-service | 3 days | Custom |

---

## 10. Related

- [`../.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) — branding wizard rules
- [`../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md`](../04-quality/gaps/GAP-051-bulk-import-users-xlsx.md) — bulk import
- [`../04-quality/gaps/GAP-052-parent-portal.md`](../04-quality/gaps/GAP-052-parent-portal.md) — parent portal (Wave 2/5)
- [`incident-response-runbook.md`](incident-response-runbook.md) — if provisioning fails
- [`../02-architecture/domain-management.md`](../02-architecture/domain-management.md) — DNS architecture
- BRD [`../00-brd/personas-catalog.md`](../00-brd/personas-catalog.md) — tenant persona profiles

---

## 11. Log

- **2026-04-18:** Created (GAP-102 Part 1 P2 batch).
