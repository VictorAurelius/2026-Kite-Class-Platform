---
title: Statuspage account-prep runbook (Phase 1 BETA — public incident comms)
status: active
created: 2026-05-26
updated: 2026-05-26
phase: phase-1-beta
audience: dev
wave: wave-beta-prep-1
gaps: [GAP-373]
---

# Statuspage Account Prep — `status.kitehub.me`

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) · [`.claude/rules/deployment-naming-convention.md`](../../../.claude/rules/deployment-naming-convention.md) §2 · [`.claude/rules/dev-readable-doc-language.md`](../../../.claude/rules/dev-readable-doc-language.md)
**Audience:** Solo dev / first-time deploy operator cần public-facing status page cho Phase 1 BETA beta cohort.
**Closes (partial):** GAP-373 (Status page minimal) — Phase 1 ship Statuspage.io free tier; auto-sync defer Phase 2.
**Cross-link:** Wave `beta-prep-1` Bucket C item 1 — paired với SNS alerts wiring + restore drill verify.

---

## 1. Mục đích

Phase 1 BETA cohort (5 invite tenants từ Wave `beta-prep-1`) cần kênh public incident comms tách khỏi hệ thống chính:

- Beta tenant kiểm `status.kitehub.me` khi gặp lỗi → biết có phải maintenance / known incident không
- Solo dev đăng update khi RDS storage cảnh báo (per `infrastructure/terraform-aws/production-alerts.tf` SNS) hoặc Phase 1 BETA P0 incident
- Khách hàng tiềm năng đọc trang xem uptime + release history → trust signal trước khi sign-up

**Decision (locked Wave beta-prep-1 2026-05-26):** ship **Statuspage.io free tier** (vendor-hosted) thay vì self-host single-page HTML. Lý do:

| Tiêu chí | Statuspage.io Free | Self-host single-page |
|---|---|---|
| Vendor uptime SLA | 99.9% Atlassian-managed | Phụ thuộc EC2 Phase 1 BETA Free Tier |
| Cost Phase 1 BETA | $0 (≤2 team members, ≤5 components) | $0 nhưng tốn time setup + maintain |
| Public URL | `kitehub.statuspage.io` (default) → custom domain `status.kitehub.me` via CNAME | EC2 nginx route cần auth-skip + caching layer |
| Subscriber email | Built-in (free tier có 100 subscribers) | Phải tự gửi qua Resend / SES |
| Component status auto-sync | API token + cron Phase 2 | Pure manual cập nhật |
| Reader UX | Polished UI + history timeline + RSS feed | Phải tự build |

**Phase 1 BETA acceptable scope:** manual cập nhật khi incident xảy ra. Phase 2 auto-sync optional khi đã có CloudWatch alarm pipeline ổn định.

---

## 2. Sequence (T-0 ngày thực hiện)

```
T-0 ngày  ┌─ §3 Bước 1 — Đăng ký account Statuspage.io     (10 phút)
          ├─ §3 Bước 2 — Tạo page + 7 components           (15 phút)
          ├─ §3 Bước 3 — Subscriber config + email branding (10 phút)
          ├─ §3 Bước 4 — Custom domain status.kitehub.me   (15 phút, blocks DNS CNAME)
          └─ §3 Bước 5 — Embed public link site footer     (5 phút, blocks FE)
```

Total: ~55 phút end-to-end. KHÔNG block deploy chain (independent từ AWS / CF).

---

## 3. Bước thực hiện

### Bước 1 — Đăng ký account Statuspage.io (~10 phút)

1. Mở [https://www.statuspage.io/](https://www.statuspage.io/) → click **Get started** (Free tier eligibility: ≤2 team members, ≤5 components, 100 subscribers — vừa đủ Phase 1 BETA).
2. Đăng ký bằng email `support@kitehub.me` (per `documents/02-architecture/env-reference.yaml` `support_email`).
3. Xác nhận email → set password (lưu vào password manager per `03-password-manager.md` runbook).
4. Skip team invite Phase 1 BETA (solo dev).

### Bước 2 — Tạo page + 7 components (~15 phút)

1. Page name: `KiteHub Status` · Slug: `kitehub` → URL mặc định `kitehub.statuspage.io`.
2. Timezone: `Asia/Ho_Chi_Minh` (UTC+7).
3. Public/private: **Public** (anyone can view).
4. Components — thêm 7 components theo nhóm:

   | # | Component name | Group | Description (≤120 ký tự) |
   |---|---|---|---|
   | 1 | KiteHub API | Backend services | API gateway `kitehub.me/api/*` — endpoint chính cho dashboard owner/admin |
   | 2 | KiteHub Frontend | Frontend | Trang chủ `kitehub.me` + dashboard owner — landing + onboarding |
   | 3 | KiteClass API | Backend services | KiteClass tenant API endpoint — student / parent / teacher dashboards |
   | 4 | KiteClass Frontend | Frontend | Tenant subdomain `{slug}.kitehub.me` — UI cho tenant users |
   | 5 | Database | Infrastructure | PostgreSQL RDS — central tenant data store |
   | 6 | Email | Infrastructure | Transactional email via AWS SES — invite / verify / invoice notifications |
   | 7 | Auth | Infrastructure | JWT + admin login + 2FA challenge flow |

5. Status mặc định: **Operational** (xanh) cho cả 7.
6. Save page.

### Bước 3 — Subscriber config + email branding (~10 phút)

1. Settings → **Subscribers** → enable **Email subscribers** (built-in, 100 free).
2. Sender email: `noreply@kitehub.me` (per `env-reference.yaml`).
3. Subject prefix: `[KiteHub Status]` cho dễ filter.
4. Branding:
   - Logo upload: `assets/kite-mark.svg` (per `documents/02-architecture/design-system/brand-assets/`)
   - Brand color: `#1f6feb` (KiteHub primary blue)
   - Footer: `KiteHub Platform · Phase 1 BETA · support@kitehub.me`
5. Test gửi 1 email tới `vannkite@outlook.com` để verify delivery.

### Bước 4 — Custom domain `status.kitehub.me` (~15 phút)

**⚠️ Pre-mutation check** per [`.claude/rules/pre-mutation-state-check.md`](../../../.claude/rules/pre-mutation-state-check.md) §3 cho mọi Cloudflare DNS PATCH:

1. Statuspage Settings → **Custom domain** → enter `status.kitehub.me`.
2. Statuspage hiển thị target CNAME (vd `statuspage.production.com` hoặc Atlassian-issued).
3. Cloudflare DNS dashboard → `kitehub.me` zone → **Add record**:
   - Type: `CNAME`
   - Name: `status`
   - Target: `<Statuspage-provided-CNAME>`
   - Proxy status: **DNS only** (KHÔNG bật orange cloud — Statuspage handles SSL)
   - TTL: Auto
4. Đợi 2-5 phút → Statuspage verify CNAME → tự cấp SSL certificate (free, Let's Encrypt).
5. Verify: `curl -sI https://status.kitehub.me` → HTTP 200.

### Bước 5 — Embed public link site footer (~5 phút)

1. Mở FE source `kitehub/kitehub-frontend/src/components/Footer.tsx` (hoặc tương đương).
2. Thêm link: `<a href="https://status.kitehub.me">Trạng thái hệ thống</a>`.
3. Cũng add link vào `documents/05-guides/user-manual/anonymous/index.md` support footer per `.claude/rules/user-manual-content-standard.md` §2 row 5.
4. Commit + ship FE deploy Wave subsequent.

---

## 4. Acceptance criteria

- [ ] Account Statuspage.io free tier đăng ký dùng `support@kitehub.me`
- [ ] Page `KiteHub Status` tạo với 7 components (API + FE × 2 sản phẩm + DB + Email + Auth)
- [ ] Sender email cấu hình `noreply@kitehub.me` + branding logo + brand color
- [ ] Custom domain `status.kitehub.me` resolve qua CNAME + SSL active
- [ ] `curl -sI https://status.kitehub.me` trả về HTTP 200
- [ ] Public link `https://status.kitehub.me` embed trong FE footer + user manual

---

## 5. Subscriber growth plan (Phase 1 BETA)

| Cohort | Auto-subscribe? | Cách add |
|---|---|---|
| 5 beta tenants Wave `beta-prep-1` | Optional opt-in | Tenant onboarding email kèm CTA "Theo dõi trạng thái" → click subscribe form |
| Solo dev | Auto | Tự subscribe sau setup |
| Stakeholders (advisor, KiteHub team) | Opt-in via runbook | Email handoff: `https://status.kitehub.me/subscribe` |
| Anonymous prospects | Public read access | Không subscribe required; có thể nếu muốn |

100 subscribers free tier dư cho Phase 1 BETA (5-50 expected).

---

## 6. Manual incident update workflow (Phase 1 BETA)

Khi nhận P0 alert qua SNS email (per `infrastructure/terraform-aws/production-alerts.tf`):

1. Mở Statuspage manage console (`manage.statuspage.io/pages/<slug>/incidents`).
2. Click **Create Incident**:
   - Name: vắn tắt mô tả (`RDS storage 80% — investigating`)
   - Status: `Investigating` → `Identified` → `Monitoring` → `Resolved`
   - Components affected: chọn từ 7 components đã setup
   - Message: tiếng Việt narrative per `dev-readable-doc-language.md` §2 row "End-user docs"
3. Subscribers tự nhận email cập nhật mỗi khi update.
4. Khi resolve → click **Resolve** → component status quay về Operational.

**Cadence update:** Investigating 30-60 phút sau detect; Identified khi tìm ra root cause; Monitoring khi đã patch; Resolved khi confirmed healthy ≥ 30 phút.

---

## 7. Phase 2 auto-sync roadmap (defer)

Khi Phase 1 BETA ổn định + CloudWatch alarm pipeline reliable:

| Step | Mô tả | Effort |
|---|---|---|
| 1 | Generate Statuspage API token (Settings → API) | 5 phút |
| 2 | Tạo Lambda function trigger từ SNS topic `kitehub-production-alerts` → POST tới Statuspage API | 1-2 giờ |
| 3 | Map CloudWatch alarm name → Statuspage component ID | Map bảng trong Lambda env vars |
| 4 | Test với 1 alarm controlled fire (vd RDS storage threshold tạm thời ≥ current) | 30 phút |
| 5 | Cron job daily uptime calculation → POST tới Statuspage uptime metrics | 1 giờ |

Total Phase 2 effort: ~4-5 giờ. Track follow-up gap sau Phase 1 BETA cohort live ≥ 2 tuần.

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Self-host single-page HTML "vì free tier có giới hạn" | Statuspage.io free tier vừa đủ Phase 1 BETA (≤2 team + ≤5 components dùng được 7 — Statuspage tính components có thể group; xác nhận lại với CSR nếu cần thiết); subscriber + uptime SLA + UX polish > setup time |
| Skip custom domain "vì `kitehub.statuspage.io` default OK" | Custom domain trust signal cao hơn + match brand |
| Bật Cloudflare proxy orange cloud cho `status.kitehub.me` CNAME | DNS only — Statuspage manage SSL độc lập; orange cloud break SSL handshake |
| Set incident status `Resolved` ngay khi patch | Monitoring ≥ 30 phút trước Resolve để verify |
| Hardcode `status.kitehub.me` trong code | Dùng env var per `env-reference.yaml` + `markdown-variable-reference.md` `{{status_page_url}}` (defer add vào yaml khi need) |
| Public manage console URL | Manage console phải auth-protected; chỉ public read URL `status.kitehub.me` |

---

## 9. Cross-links

- **Sister runbook:** `documents/05-guides/account-prep/06-resend-account-setup.md` (transactional email cho Statuspage subscriber emails)
- **Decision context:** Wave `beta-prep-1` plan `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md` §3 Bucket C item 1
- **Sister rule:** `.claude/rules/no-vercel-references.md` §4 exception "transition reality" — Statuspage external vendor, không thuộc Vercel decommission scope
- **Closes (partial):** GAP-373 (Status page minimal) — Phase 1 scope; Phase 2 auto-sync deferred gap

---

## 10. Log

- **2026-05-26 (initial):** Runbook created cho Wave `beta-prep-1` Bucket C item 1. Decision Statuspage.io free tier locked sau cost-benefit comparison (vendor SLA + subscriber + UX polish > self-host effort). Custom domain `status.kitehub.me` via CF CNAME DNS-only (no proxy). Manual incident update workflow Phase 1 BETA acceptable; Phase 2 auto-sync defer. Per `audit-to-gap-pipeline.md` §2.5 — gap state-check: GAP-373 OPEN P1 (`bash scripts/query-gaps.sh GAP-373`); runbook closes Phase 1 scope partial, full DONE flip waits Phase 2 auto-sync ship.
