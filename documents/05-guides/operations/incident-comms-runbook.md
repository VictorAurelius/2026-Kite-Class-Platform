# Incident Communication Runbook

**Wave:** 38 Bucket C (GAP-373) + Wave 84 Bucket E (GAP-424 — Vietnamese quick-start overlay refresh)
**Status:** Active 2026-05-15
**Last reviewed:** 2026-05-15
**Owner:** @nguyenvankiet (solo-dev, acting incident commander)
**Related:** [`post-mortem-template.md`](post-mortem-template.md), [`ADR-027 statuspage vendor`](../../../02-architecture/adr/ADR-027-statuspage-vendor.md), GAP-370 email infra

---

## 🇻🇳 Hướng dẫn nhanh — Tiếng Việt

> Section này là tóm tắt tiếng Việt cho dev VN chưa quen Instatus / status page. Phần technical chi tiết bên dưới (§0 quy trình incident + §0.1 signup walkthrough + §1-§6 EN procedure) giữ English/mixed để cross-locale stable cho terminology vendor.

**Instatus / Status page là gì:** Status page = trang public hiển thị health của các component dịch vụ (KH-API, KC-API, Marketing site, Auth, Email). Khi có sự cố, tenant invite-only (10-20 trường học Phase 1 BETA) check `status.kitehub.vn` để biết "đang fix" thay vì gửi support email loạn. Vendor đã chốt theo ADR-027 = **Instatus** (Free tier $0/tháng — đủ cho Phase 1 BETA, upgrade Starter $20/tháng khi cần custom CSS hoặc private page Phase 2).

**Khi nào dùng runbook này:**

- Lần đầu signup Instatus + custom domain `status.kitehub.vn` setup (1 lần / vendor account). Chi tiết: §0.1 Instatus Signup Walkthrough.
- Khi xảy ra incident production (P0/P1/P2/P3 per §3) → đăng status update theo 6-step quy trình (Detect → Triage → Post Initial → Update Cadence → Resolve → Post-mortem). Chi tiết: §0 bảng 6 bước + §4 EN chi tiết.
- Cấu hình severity level + subscriber email template khi onboard cohort tenant mới.
- Tham khảo template Vietnamese cho incident message body (status page hỗ trợ UTF-8 đầy đủ, subject + body Việt OK).
- Post-mortem RCA trong 5 ngày làm việc sau incident — dùng `post-mortem-template.md`.

**Quy trình tóm tắt (6 bước incident response, mỗi bước cross-link xuống §4):**

1. **Detect** (<5 phút) — phát hiện sự cố qua alert Cloudflare / CloudWatch / user report. Trực ca / on-call nhận page. Chi tiết: §4 Step 1.
2. **Triage** (5-10 phút) — đánh giá severity P0/P1/P2/P3 + phạm vi tenant ảnh hưởng. Chi tiết: §3 severity matrix + §4 Step 2.
3. **Post Initial** (<10 phút cho P0/P1) — đăng incident lên Instatus với severity + impact + ETA điều tra. Chi tiết: §4 Step 3 + sample message body Vietnamese.
4. **Update Cadence** (P0 = 30 phút / P1 = 60 phút / P2 = 4h) — cập nhật progress đều đặn để giảm tenant anxiety. Chi tiết: §4 Step 4.
5. **Resolve** (<15 phút sau verify fix) — đăng "Resolved" với root-cause summary 1-2 câu. Chi tiết: §4 Step 5.
6. **Post-mortem** (1-3h, trong 5 ngày làm việc) — viết RCA theo `post-mortem-template.md`, share team + tenant nếu P0. Chi tiết: §4 Step 6.

**Bẫy thường gặp:**

- ❌ Set timezone Instatus mặc định UTC → tenant VN thấy "incident at 02:00" hôm sau nhầm hôm trước. Phải set **(UTC+07:00) Asia/Ho_Chi_Minh** ở Bước 2 signup. Verify: dashboard → Settings → Timezone.
- ❌ Custom domain `status.kitehub.vn` proxy qua Cloudflare orange-cloud → SSL cert Instatus không issue được. Phải để **DNS only (gray cloud)** ở Cloudflare CNAME record. Verify: `dig CNAME status.kitehub.vn` trả về `<slug>.instatus.com` (không phải Cloudflare IP).
- ❌ Quên enable 2FA Instatus account → security posture không đủ khi cần grant team access Phase 2. Enable TOTP qua Bitwarden ngay sau signup.
- ❌ Post incident bằng tiếng Anh thuần khi tenant VN-only → tenant không hiểu impact. Dùng template Vietnamese (subject + body), Instatus subscriber email auto-render Vietnamese OK.
- ❌ Severity nhầm — P2 nhưng đăng "Major outage" tạo panic không cần thiết. Severity matrix §3 rõ: P0 = down hoàn toàn, P1 = phần lớn tenant, P2 = 1 component / 1 tenant, P3 = cosmetic.

**Khi gặp lỗi:** xem §0.1 Bước 7 "Test incident sample → resolve → verify subscriber email" để test setup, hoặc §6 "Troubleshooting" bên dưới. Nếu Instatus dashboard tự nó down: fallback Twitter/X account `@KiteHubStatus` (Phase 2 prep) hoặc tenant email blast qua AWS SES (cross-link `email-ses-setup-runbook.md`).

**Cross-link tiếng Việt mở rộng:**

- §0 quy trình 6-step Vietnamese đầy đủ (bảng + severity nhanh) chi tiết bên dưới.
- §0.1 Instatus Signup Walkthrough Vietnamese (Bước 1-7) — từ tạo account → custom domain → 5 components → severity config → test incident sample.
- Cloudflare CNAME setup Vietnamese: `documents/05-guides/vietnamese/cloudflare-setup.md` §DNS records.
- Post-mortem template: `post-mortem-template.md` (cùng folder).
- ADR vendor decision: `documents/02-architecture/adr/ADR-027-statuspage-vendor.md`.

---

## §0 Hướng Dẫn Nhanh (Vietnamese)

**Bối cảnh:** Phase 1 BETA cần status page public (`status.kitehub.vn`) để thông báo downtime cho 10-20 tenant invite-only. Vendor đã chốt theo ADR-027 = **Instatus** (Free tier $0/tháng cho 1 page + 5 component, public unlimited).

**6 bước quy trình incident** (cross-link xuống section EN bên dưới):

| # | Bước | Thời gian | Ai làm | Chi tiết EN |
|---|------|----------|--------|-------------|
| 1 | **Detect** — phát hiện sự cố qua alert (Cloudflare alert / CloudWatch / user report) | <5 phút | Trực ca / on-call | §4 Step 1 |
| 2 | **Triage** — đánh giá severity (P0/P1/P2/P3 per §3) + tác động phạm vi tenant | 5-10 phút | Trực ca | §4 Step 2 |
| 3 | **Post Initial** — đăng incident lên Instatus với severity + impact + ETA điều tra | <10 phút (P0/P1) | Trực ca | §4 Step 3 |
| 4 | **Update Cadence** — cập nhật mỗi 30 phút (P0) / 60 phút (P1) / 4 giờ (P2) | Tuỳ severity | Trực ca | §4 Step 4 |
| 5 | **Resolve** — sau khi fix verified, đăng "Resolved" với root-cause summary | <15 phút | Trực ca | §4 Step 5 |
| 6 | **Post-mortem** — viết RCA trong 5 ngày làm việc theo `post-mortem-template.md` | 1-3h | Incident commander | §4 Step 6 |

### Severity nhanh

- **P0** = sản xuất hoàn toàn down hoặc data loss → page on-call ngay + Instatus "Major outage" + tenant email blast
- **P1** = phần lớn tenant ảnh hưởng → on-call notify + Instatus "Partial outage" + tenant in-app banner
- **P2** = ảnh hưởng nhỏ hoặc 1 component / 1 tenant → ticket trong working hours + Instatus "Degraded performance"
- **P3** = cosmetic / minor / không user-facing → ticket bình thường, không cần Instatus

---

## §0.1 Instatus Signup Walkthrough (Vietnamese)

Đây là phần thực sự thiếu trong runbook gốc — Wave 38 chỉ checklist 5 dòng. Walkthrough đầy đủ:

### Bước 1 — Tạo Instatus account (~5 phút)

1. Mở [instatus.com](https://instatus.com) → Sign up.
2. Email: dùng `ops-admin@kitehub.vn` hoặc tài khoản admin chung. KHÔNG dùng email cá nhân (sau khi team mở rộng cần shared access).
3. Password: ≥16 chars, lưu vào Bitwarden vault `Kite-Production / 3rd-party / Instatus`.
4. Verify email → login dashboard.
5. Free tier: 1 page + 5 component + unlimited public visitor. Đủ cho Phase 1 BETA. Phase 2 cân nhắc Starter $20/tháng (custom CSS + private page).
6. Enable 2FA TOTP qua Bitwarden.

### Bước 2 — Tạo status page mới (~10 phút)

1. Dashboard → "Create new status page" → "From scratch".
2. Page name: **KiteHub & KiteClass Status**.
3. URL slug: `kitehub-kiteclass` (sẽ thành `kitehub-kiteclass.instatus.com` ban đầu, custom domain ở §Bước 3).
4. Logo: upload SVG `kite-mark.svg` (từ `assets/` repo).
5. Brand colors: primary `#2563eb` (KiteHub blue), background `#ffffff`.
6. Timezone: **(UTC+07:00) Asia/Ho_Chi_Minh** — quan trọng cho timestamp incident hiển thị giờ VN.
7. Language: **English** (Instatus chưa hỗ trợ Vietnamese UI; subscriber email có thể custom Vietnamese template trong Bước 5).
8. Save.

### Bước 3 — Custom domain `status.kitehub.vn` (~15 phút)

1. Instatus dashboard → Settings → Custom domain → "Add domain".
2. Nhập `status.kitehub.vn`.
3. Instatus cấp CNAME target (vd: `kitehub-kiteclass.instatus.com`).
4. Mở Cloudflare DNS dashboard → `kitehub.vn` zone → Add record:
   - Type: `CNAME`
   - Name: `status`
   - Target: `<từ Instatus>` (vd `kitehub-kiteclass.instatus.com`)
   - Proxy status: **DNS only** (gray cloud — Instatus tự handle SSL, KHÔNG proxy qua Cloudflare orange-cloud)
   - TTL: Auto
5. Save record. Đợi propagation 5-10 phút.
6. Quay về Instatus → Verify domain → click "Check DNS".
7. SSL cert auto-provisioned trong 5-15 phút (Let's Encrypt qua Instatus).
8. Verify: mở browser `https://status.kitehub.vn` → load OK với SSL valid.

### Bước 4 — Define 5 components (~5 phút)

Components = các phần dịch vụ user thấy. Map theo persona:

1. Dashboard → Components → "Add component" × 5:

| # | Component name | Description (EN) |
|---|---------------|-------------------|
| 1 | KiteHub API (Marketing + Admin) | Public marketing site + admin dashboard backend |
| 2 | KiteClass API (Tenant App) | Multi-tenant education platform backend |
| 3 | Email Delivery | Transactional emails (welcome, MFA recovery, billing) |
| 4 | Authentication | Login, MFA, session management |
| 5 | AI Branding | AI-generated branding assets pipeline |

2. Order: drag-drop theo priority hiển thị (KiteClass first vì user-facing nhiều nhất).
3. "Show on status page": ✅ ON cho cả 5.
4. "Showcase": ✅ ON (component hiển thị trên homepage status page).

### Bước 5 — Configure subscriber notifications (~10 phút)

1. Dashboard → Notifications → Email subscribers → "Allow email subscriptions": ✅ ON.
2. Custom email template (nếu Instatus paid; Free tier dùng default):
   - Subject Vietnamese: "KiteHub Status: {{incident.name}}"
   - Body intro Vietnamese: "Xin chào, chúng tôi đang xử lý sự cố {{severity}} ảnh hưởng đến {{components}}."
3. RSS feed: ✅ ON (auto-enable; không cần config).
4. Webhook (optional Phase 2): để integrate với Slack / Discord khi team mở rộng.
5. Test: subscribe email cá nhân → tạo test incident (Bước 6) → verify nhận email trong <2 phút.

### Bước 6 — Test incident flow (~10 phút)

1. Dashboard → Incidents → "Create new incident".
2. Title: **[TEST] Database connection slow**.
3. Severity: **Investigating** (lowest level — không trigger major alert).
4. Affected components: KiteClass API.
5. Message: "We are investigating slow DB queries impacting class list page. ETA 30 min."
6. Click "Create".
7. Verify:
   - Status page `https://status.kitehub.vn` show banner orange "Investigating"
   - Email subscriber nhận trong <2 phút
   - RSS feed updated
8. Update incident: click "Update" → status "Identified" → message "Root cause: connection pool exhausted. Scaling up."
9. Resolve: click "Resolve" → message "Connection pool scaled. All queries normal latency."
10. Status page banner clear → component status "Operational" green.
11. Delete test incident (Settings → "Delete incident") để không pollute history.

### Bước 7 — Pitfalls VN

- **Timezone:** Instatus default UTC. Phải đổi sang `Asia/Ho_Chi_Minh` trong Bước 2 §6 — nếu quên, timestamps incident hiển thị giờ UTC = user VN thấy "incident posted at 03:00" nhầm tưởng đêm khuya.
- **Payment method:** Instatus accept Visa/MasterCard quốc tế. VN debit Vietcombank đôi khi reject — fallback Visa credit Techcombank/VPBank.
- **Custom domain SSL:** nếu Cloudflare orange-cloud (proxy ON) → SSL fail vì Instatus tự issue cert qua Let's Encrypt. PHẢI để DNS only (gray cloud).
- **Email subscriber rate limit:** Instatus free tier giới hạn 100 subscriber. Phase 1 BETA 10-20 tenant đủ; Phase 2 cần upgrade Starter $20/tháng cho 1k subscriber.
- **Status badge embed:** copy badge HTML từ Instatus → embed vào KiteHub marketing site footer + KiteClass tenant dashboard footer (Phase 2).
- **History retention:** Instatus free giữ history 90 ngày. Phase 2 paid → 1 năm. Export incident JSON định kỳ nếu cần audit dài hạn.
- **Compliance VN PDPL:** subscriber email là dữ liệu cá nhân → khai báo trong privacy policy (đã có Wave 23 PDPL). Cho phép unsubscribe trong mỗi email (Instatus auto-include footer).

### Cross-link tiếng Việt

- Phần thực hiện chi tiết EN: §1 → §6 bên dưới
- Cloudflare DNS records add: `documents/05-guides/vietnamese/cloudflare-setup.md`
- Domain prerequisite: `documents/05-guides/account-prep/02-domain-registrar.md`
- Password manager: `documents/05-guides/account-prep/03-password-manager.md`
- Vendor decision: `documents/02-architecture/adr/ADR-027-statuspage-vendor.md`
- Phase 1 deploy runbook: `documents/03-planning/roadmap/release-1-deploy-runbook.md` Phase 1 §1.5

### Hỏi đáp thường gặp (Vietnamese FAQ)

**H1: Tại sao chọn Instatus mà không chọn StatusPage.io (Atlassian)?**
StatusPage.io tối thiểu $29/tháng cho 1 page. Instatus Free $0/tháng đủ Phase 1 BETA. Phase 2 nếu cần SLA enforcement / SAML SSO / advanced analytics → cân nhắc StatusPage.io paid hoặc Instatus Pro $50/tháng. Quyết định chốt theo ADR-027.

**H2: Self-host Cachet hoặc Statping được không?**
Có, nhưng tốn ~5h setup + 2h/tháng maintenance + thêm 1 server. Không tiết kiệm so với Instatus Free $0/tháng. Self-host chỉ có ý nghĩa Phase 3 khi compliance yêu cầu data localization VN.

**H3: Status page có cần 24/7 monitoring riêng không?**
Có. Cloudflare Health Check (free) ping endpoint chính mỗi phút → trigger alert nếu fail 3 lần liên tiếp. Alert đến Slack/email → người trực ca update Instatus thủ công. Phase 2 cân nhắc auto-update qua Instatus webhook API.

**H4: Khi nào nên đăng incident vs để yên?**
Quy tắc: nếu ≥1 user gửi support ticket / Slack ping / email với cùng vấn đề → POST incident ngay (dù chưa biết root-cause). Im lặng = giảm trust nhanh hơn admit vấn đề. Phase 1 BETA invite-only → admit thoải mái, đỡ áp lực.

**H5: Severity sai sau khi đăng có chỉnh được không?**
Có, edit incident → đổi severity. Nhưng làm subscriber receive duplicate email. Cân nhắc kỹ trước khi đăng. Quy tắc: nghi ngờ → severity cao hơn (P1 thay vì P2), downgrade dễ hơn upgrade.

**H6: Post-mortem template ở đâu?**
File `documents/05-guides/operations/post-mortem-template.md` (Wave 38 ship). Sao chép template cho mỗi P0/P1 trong vòng 5 ngày làm việc sau resolve. P2/P3 tuỳ chọn (không bắt buộc).

### Thuật ngữ tiếng Việt

| Thuật ngữ EN | Tương đương Việt | Ghi chú |
|--------------|-----------------|---------|
| Incident | Sự cố | Mọi event ảnh hưởng dịch vụ public-facing |
| Severity | Mức độ nghiêm trọng | P0/P1/P2/P3 theo §3 |
| Triage | Phân loại đầu | Đánh giá severity + scope ảnh hưởng đầu tiên |
| Detect | Phát hiện | Bước 1 — alert hoặc user report |
| Root cause | Nguyên nhân gốc | Lý do thật sự gây sự cố, viết trong post-mortem |
| Post-mortem | Báo cáo sau sự cố | Tài liệu RCA viết 5 ngày sau resolve |
| Subscriber | Người đăng ký nhận thông báo | Email subscriber qua status page |
| Component | Thành phần dịch vụ | 5 phần dịch vụ user thấy (KH-API, KC-API, Email, Auth, AI Branding) |
| Update cadence | Tần suất cập nhật | 30/60/240 phút theo P0/P1/P2 |
| Operational | Hoạt động bình thường | Trạng thái xanh trên status page |
| Degraded | Suy giảm hiệu năng | Vẫn chạy nhưng chậm/lỗi rải rác |
| Major outage | Mất dịch vụ nghiêm trọng | P0 — full down |
| Resolved | Đã khắc phục | Sau khi fix verified và monitor 30 phút stable |

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
