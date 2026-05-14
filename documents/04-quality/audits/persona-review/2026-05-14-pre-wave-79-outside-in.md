---
title: Persona Outside-In Audit — Pre-Wave-79 Phase 1 BETA invite-launch
status: complete
created: 2026-05-14
audit_type: outside-in
wave: 79
personas: [P2_center_owner, P3_center_manager, anonymous_prospect, platform_admin]
related_inside_out_gaps: [GAP-547, GAP-551, GAP-555, GAP-545, GAP-548, GAP-552, GAP-553, GAP-554, GAP-544, GAP-537, GAP-040, GAP-556, GAP-557]
related_audits: [2026-05-14-phase-1-beta-persona-walkthrough.md]
new_gaps_filed: [GAP-558, GAP-559, GAP-560, GAP-561, GAP-562]
---

# Persona Outside-In Audit — Pre-Wave-79 Phase 1 BETA Invite-Launch

## Scope

Walkthrough post-Wave-78 production-ready surface với 4 personas đặc trưng cho Phase 1 BETA invite cohort. Mục đích: tìm gap OUTSIDE inside-out queue Wave 79 (P0 + P1 đã định + meta), để add vào Wave 79 plan §1 Brainstorm Q1 TRƯỚC khi plan PR mở.

Surface walk-qua:
- Public anonymous: landing (`/`) → pricing (`/pricing`) → request-beta-access (`/auth/request-beta-access`) → beta-status (`/beta-status`)
- Auth surface: login (`/login`) với rate-limit + Retry-After countdown (GAP-515) + admin role compat (GAP-518); 2FA setup (`/2fa-setup`); 2FA challenge (`/2fa-challenge`)
- Admin internal: AdminLayout sidebar (Beta Requests / Instances / Payments / Revenue per GAP-519) → /admin/beta-requests approve flow → tenant provisioning runbook (GAP-480)
- Customer dashboard: `/dashboard` → `/onboarding` checklist 5 steps (GAP-538) → beta disclaimer banner (GAP-539) → feedback widget (GAP-542) → support footer (GAP-540)
- Email templates: welcome / beta-request-confirmation / beta-invite (claim code) / subscription-* / dsar-* / data-retention-* / onboarding-tips

## Phương pháp

Đóng vai từng persona 5-7 phút, đặt 5 câu "tại sao?" mỗi điểm kẹt. Khác với audit `2026-05-14-phase-1-beta-persona-walkthrough.md` (focus P1 Solo + P2 Center + P4 Creator), audit này focus 4 persona MỚI cho launch readiness: **P2 Owner Beta-Approved, P3 Manager invited, Anonymous Prospect pre-conversion, Platform Admin internal ops**. Mục tiêu tìm gap NGOÀI inside-out queue 13 items đã biết.

---

## Persona 1 — P2 Center Owner (Chị Hằng, 38, chủ trung tâm Anh ngữ Hải Phòng, 120 hs)

**Profile:**
- Đã submit beta request 3 ngày trước; vừa nhận email "beta-invite" với claim code 6 chữ số
- Tech-savvy: trung bình. Dùng Zalo, FB Ads, Excel + Misa; chưa setup SaaS bao giờ
- Beta motivation: bạn gửi link giới thiệu; muốn giảm thời gian quản lý 4h/tuần đang dành cho lịch học manual
- Lo lắng chính: data trung tâm bị reset trong beta; nhân viên không quen UI mới; phải training lại

### Walkthrough

**Bước 1 — Mở email "beta-invite" → click claim:**
- Email render OK trên Gmail mobile (header gradient brand, claim code monospace 22px, expiry visible)
- Click "Mở trang đăng ký" → redirect tới `/auth/register?invite=...` (claim code workflow per GAP-388)
- Bước "nhập claim code 6 chữ số" → OK nhưng KHÔNG có copy-from-email helper button. Trên mobile, user phải switch app email ↔ browser 2 lần để gõ tay 6 chữ số (high friction)
- Cookie consent / GA tracking trên signup page: KHÔNG thấy banner cookie consent dù form đã có PDPL consent checkbox (GAP-385). Inconsistent — Phase 1 EU/PDPL tier cần cookie banner separate từ form consent.

**Bước 2 — Set password (12+ chars complex):**
- Per `pre-launch-auth-hardening-checklist.md` §2.3 — password policy 12+ chars + mix + reject top-10000 leaked passwords. Chị Hằng quen password 8 chars Misa cũ
- KHÔNG có password-strength meter visual ngay khi gõ. User chỉ thấy error sau submit fail → 2-3 vòng retry
- "Show password" toggle: thiếu → user typing-blind, dễ typo passphrase 14 chars → fail → cảm giác app "khó tính"

**Bước 3 — First login → dashboard:**
- 2FA enrollment KHÔNG bắt buộc cho P2 (chỉ PLATFORM_ADMIN per GAP-516 Wave 72b Bucket B). OK theo policy, nhưng dashboard không có "**Tăng cường bảo mật: bật 2FA**" CTA voluntary → user trung tâm không tự nhận awareness
- Beta disclaimer banner xuất hiện (GAP-539): "KiteHub đang trong giai đoạn Beta. Dữ liệu có thể bị reset..." → tốt, nhưng "dữ liệu có thể bị reset" làm chị Hằng hoảng. **Banner thiếu cụ thể**: bao giờ reset? Reset toàn bộ hay chỉ 1 phần? Có backup không?
- Onboarding checklist 5 steps (GAP-538) hiển thị ở `/onboarding`, NHƯNG: dashboard `/dashboard` KHÔNG có "Bạn còn 5/5 bước onboarding chưa làm — đi tới checklist" CTA prominent. User phải tự tìm `/onboarding` URL hoặc click qua nav (chưa thấy nav entry trong customerNav Sidebar — Sidebar.tsx chỉ có 4 entries: Tổng quan / Thanh toán / AI Branding / Cài đặt, KHÔNG có Onboarding)

**Bước 4 — Vào /onboarding khám phá:**
- Checklist 5 step rendering OK; tiến độ 0/5 với "0% tiến độ"
- Step "Bật dữ liệu mẫu" (IMPORT_DATA) → click → confirm dialog "tạo lớp học, học viên, lịch học mẫu" → OK nhưng **không nói rõ số lượng cụ thể** (10 hs giả? 5 lớp?). User lo "có tạo 1000 records mock không?"
- 4 step còn lại (BRANDING / INVITE_STAFF / FIRST_CLASS / ...) → label visible nhưng KHÔNG có "Tiến tới bước này" hành động link đến route relevant. User click vào tickbox để đánh dấu DONE manual → not honest checklist (user chưa thực sự làm)
- Toast/success message khi seed data complete: thiếu "Xem dữ liệu mẫu ở đâu?" → user không biết navigate đến class list

**Bước 5 — Feedback widget gặp:**
- Floating "💬 Góp ý" button bottom-right (GAP-542) → click → modal mở
- Modal là **fullscreen overlay trên mobile** che hết content phía sau → user mất context "tôi đang ở trang nào, đang phản hồi về cái gì?" — page URL hidden (form auto-attach `pageUrl` nhưng user không thấy)
- Form ask "Email (không bắt buộc)" → P2 Owner đã login → form NÊN prefill email từ user session để user 1-click submit không phải gõ lại

**Bước 6 — Daily ops sau 3 ngày:**
- Mở 4G mobile vào `/dashboard` → load chậm 3-4s (chưa có lazy chunk cho phần customer area)
- Notification gửi cho phụ huynh khi điểm danh: chưa wire (per GAP-063 Phase 2). User mong "tự động SMS/Zalo" → thất vọng, defer
- Settings → "Xuất dữ liệu" (PDPL self-service export): KHÔNG thấy ở Cài đặt. Customer setting chỉ có account profile + theme; chưa có "Quản lý dữ liệu" tab cho download/delete

### Gap surface (Persona 1)

- **N1-P0** [NEW] Cookie consent banner thiếu trên public site (signup + landing + pricing). PDPL 2023 mandate cookie consent banner **independent** với in-form consent (GAP-385 chỉ cover form). Sai compliance: tracking cookies (GA, Vercel analytics) chạy trước khi user opt-in → vi phạm PDPL Art 11 + Decree 13/2023 Art 4. → **GAP-558**
- **N1-P1** [NEW] Dashboard thiếu CTA "Bạn còn 5/5 bước onboarding chưa làm" + thiếu nav entry trong customerNav Sidebar. User phải tự tìm `/onboarding` URL. → **GAP-559**
- **N1-P1** [NEW] Beta disclaimer banner thiếu specificity: "dữ liệu có thể bị reset" — bao giờ? toàn bộ hay 1 phần? có backup không? → tăng anxiety thay vì calm. Cần link "Chính sách reset dữ liệu beta" → small doc page giải thích. → **GAP-560**
- **N1-P2** Password complexity UX: thiếu strength meter visual + "show password" toggle → 2-3 vòng retry trên signup. (Inside-out GAP-548 covers password-reset BE controller, NHƯNG không cover register UX). → Add to Wave 79 candidate, hoặc consolidate vào GAP-548 scope.
- **N1-P2** Voluntary 2FA awareness CTA cho non-admin tenants. User mong muốn nhưng không thấy → wait Wave 80+.

---

## Persona 2 — P3 Center Manager (Anh Tâm, 32, manager được invite bởi P2 Owner, daily ops)

**Profile:**
- Manager được P2 Owner (chị Hằng) invite từ Settings → Team (giả định feature đã có — nhưng thực tế **chưa có** per persona walkthrough 2026-05-14 PR-THUY-7)
- Tech-savvy: cao — dùng Notion, Asana cho công việc cũ
- Role: vận hành lịch học + chấm công nhân viên + báo cáo cho Owner hàng tuần
- Beta motivation: Owner bảo "anh test giúp em xem dùng được không"

### Walkthrough

**Bước 1 — Nhận email "invite-staff" → set password:**
- ❌ **Email template `invite-staff.html` KHÔNG tồn tại** trong `kitehub-email/src/main/resources/templates/emails/`. List 17 templates: welcome / beta-invite / subscription-* / dsar-* / data-retention-* / onboarding-tips / email-verification — KHÔNG có invite-staff
- → P3 Manager flow KHÔNG ship được trong Phase 1 BETA. P2 Owner invite staff = blocker. Overlap với 2026-05-14 audit PR-THUY-7 (P3 Center Manager invite blocker).

**Bước 2 — Giả định invite-staff đã có, login successful:**
- AdminLayout chỉ có 2 variant: `customer` (P2 Owner) + `admin` (PLATFORM_ADMIN). KHÔNG có variant `staff` cho P3 Manager → P3 Manager login sẽ thấy đầy đủ Sidebar customer nav (Tổng quan / Thanh toán / AI Branding / Cài đặt)
- Permission boundary: P3 Manager có thể click `/billing` (Thanh toán) và thấy thẻ tín dụng của Owner? Có thể click `/branding` (AI Branding) và regenerate banner gây cost? → **RBAC gap nghiêm trọng**. Inside-out queue chưa có gap riêng cho RBAC (PR-THUY-14 trong audit cũ là P0 nhưng chưa filed)

**Bước 3 — Daily report cho Owner:**
- P3 cần "in báo cáo tuần" hoặc "share dashboard link cho Owner" → chưa có public/shareable view; chưa có PDF export báo cáo
- P3 muốn nhận notification "có lớp mới được tạo / có học viên đăng ký" → email notification engine chưa ship Phase 1

### Gap surface (Persona 2)

- **N2-P0** [NEW] `invite-staff.html` email template thiếu + tenant invite-staff API endpoint khả năng chưa tồn tại (verify với BE controller). Blocker cho P3 Manager flow ngay từ ngày đầu beta. → **GAP-561**
- **N2-P0** [NEW] RBAC role separation Customer vs Staff: AdminLayout variant `staff` chưa có; permission boundary chưa enforce. P3 có thể access billing / AI Branding của Owner. Inside-out GAP-554 chỉ cover X-Tenant-Id cross-check cho onboarding, KHÔNG cover RBAC role-based access. → **GAP-562**
- **N2-P1** Báo cáo tuần PDF export cho Manager — defer Wave 80+ (P3 specific, không phải P2 P0 blocker)

---

## Persona 3 — Anonymous Prospect (Em Vy, 24, intern marketing, đang research SaaS để pitch sếp)

**Profile:**
- Lần đầu vào KiteHub qua Google Search "phần mềm quản lý trung tâm tiếng anh"
- Tech-savvy: trung bình, biết review SaaS qua G2, Capterra, demo video
- Mục tiêu: collect info → pitch sếp → quyết định signup

### Walkthrough

**Bước 1 — Landing page (`/`):**
- Hero render OK (server-rendered SSR shell + dynamic LandingShell client chunk). First Contentful Paint < 2s trên desktop
- Tagline phù hợp tiếng Việt: "Nền tảng quản lý trung tâm giáo dục thông minh" — OK
- KHÔNG có **social proof**: số tenant đang dùng, logo customer, testimonial — Em Vy không có gì để pitch sếp ngoài tagline + 4 pricing tier
- KHÔNG có **comparison vs Misa/KidsPay/Smile** — Em Vy đã list 5 options trên Excel, KiteHub không stand out
- "Yêu cầu Beta" CTA → form `/auth/request-beta-access` — phù hợp cho Phase 1 nhưng Em Vy chưa quyết signup, chỉ research → bounce

**Bước 2 — Pricing (`/pricing`):**
- 4 tier cards rendered đẹp với toggle hàng tháng/năm
- FREE tier "10 học viên / 1 giảng viên" → Em Vy nghĩ "trường mình 100 hs, không đủ" → skip FREE
- BASIC 500k/tháng "50 học viên" → trường 100 hs cũng không đủ → suggest PREMIUM 1.5tr/tháng "200 học viên" — đắt → cần justify
- **Pricing claim "Dùng thử 14 ngày, không cần thẻ tín dụng" trên meta description** nhưng **KHÔNG có thực sự trial flow visible** trên signup/request-beta — Phase 1 BETA pivot sang invite-only, không phải free trial → discrepancy meta vs reality
- Pricing FAQ section render từ `PRICING_FAQS` array — không thấy "Có miễn phí onboarding setup không?" hoặc "Migrate từ Misa được không?" — common questions của Em Vy không được trả lời

**Bước 3 — Beta status (`/beta-status`):**
- Page render OK; "Hoạt động bình thường" badge xanh
- "Cập nhật lần cuối: ngày/giờ vi-VN" — OK
- KHÔNG có **subscribe email update** khi status changes → Em Vy muốn "đăng ký nhận thông báo" để khi out-of-beta thì biết

### Gap surface (Persona 3)

- **N3-P1** [NEW] Landing thiếu social proof + competitor comparison → conversion rate thấp cho anonymous prospect. Trong inside-out queue chưa có (PR-LAN-1 audit cũ chỉ flag P1 Solo Teacher, không cover anonymous prospect researcher). Cần Wave 79 candidate hoặc Wave 80. Skip filing gap riêng (overlap với marketing scope GAP-541 vi-i18n audit).
- **N3-P0** [NEW] **Meta description claim "Dùng thử 14 ngày, không cần thẻ tín dụng" mismatch với Phase 1 BETA invite-only reality** → false advertising risk + PDPL Art 9 (truthful communication). Sửa pricing page meta + FAQ. Lý do P0: legal/compliance risk. → consolidate vào GAP-541 vi-i18n audit scope hoặc file riêng. **Khuyến nghị file GAP riêng vì spec scope khác.** → skip filing for now (GAP-541 scope covers content audit).
- **N3-P2** Beta status page thiếu "Subscribe email khi out-of-beta" — defer Wave 80+

---

## Persona 4 — Platform Admin (Bạn Mai, 27, ops/dev của KiteHub, approve beta requests + handle support)

**Profile:**
- Internal team, dùng AdminLayout daily
- Role PLATFORM_ADMIN (sau GAP-518 unified với legacy ADMIN)
- Daily ops: check beta-requests queue, approve/reject, monitor instances, respond support email

### Walkthrough

**Bước 1 — Login với 2FA setup:**
- Email + password → backend trả `requires2fa_enrollment: true` (Wave 72b Bucket B) → redirect `/2fa-setup` (GAP-516)
- QR code render + recovery codes display + acknowledge checkbox + first TOTP submit — flow OK
- ⚠️ `/2fa-setup` POST `/api/auth/2fa/enroll-init` → endpoint UNDOCUMENTED + UNVERSIONED per inside-out GAP-547 → vẫn chưa fix
- Sau enroll xong → `router.push('/admin')` → vào AdminLayout

**Bước 2 — /admin → AdminLayout:**
- Sidebar variant `admin` hiển thị 4 nav: Beta Requests / Instances / Payments / Revenue (per GAP-519). OK
- Header: "Quản trị hệ thống" + admin email + Đăng xuất → OK
- ❌ **/admin HOME page chỉ có placeholder?** Per `admin/page.tsx` — cần verify nội dung. Click `/admin` → expect dashboard tổng quan stats (số beta-requests pending, số instances active, MRR, ...) — KHÔNG có summary card nào trên admin landing

**Bước 3 — /admin/beta-requests → approve flow:**
- List beta requests pending hiển thị
- Click "Duyệt" → submit POST /api/v1/admin/beta-requests/{id}/approve
- → trigger backend: send invite email (beta-invite.html) với claim code 6 chữ số + create pending tenant
- ⚠️ **No batch approve UI** — nếu 30 yêu cầu cuối tuần, Mai phải click từng yêu cầu một
- ⚠️ **No "reject reason" templated** — Mai phải tự gõ lý do, không có 5-6 lý do common preset (off-target persona, K-12 not yet supported, etc.)
- ⚠️ **Search/filter beta-requests** by persona / source / org name → có không? Cần verify (likely chưa)

**Bước 4 — /admin/instances → suspend/configure:**
- Per GAP-480 runbook tenant provisioning DONE → admin can suspend tenant
- ⚠️ **No "send announcement to all tenants" feature** — nếu Mai cần báo "maintenance window tối nay", phải gửi email thủ công ngoài app

**Bước 5 — Handle support email:**
- Mai check inbox `support@kitehub.me` (per Footer)
- ⚠️ **No ticketing dashboard inside admin** — Mai dùng Gmail manual; mất context, không link được Gmail thread ↔ tenant ID
- Inside-out GAP-040 (support impersonation) marks P1 cho Wave 79 — đúng scope. Nhưng support email triage UI cũng cần.

### Gap surface (Persona 4)

- **N4-P1** [NEW] /admin home page thiếu summary dashboard (beta-requests pending count, instances active, MRR, recent feedback count, recent support email). Mai phải click qua 4 nav để check status → ineffective daily ops. → **GAP filed:** prioritize Wave 80 (admin home enhancement); skip new gap for Wave 79 scope unless P2 Owner blocked.
- **N4-P0** Batch approve UI + reject-reason templates trên /admin/beta-requests. Nếu Phase 1 BETA target 10 tenants/week, manual approve OK. Nếu 30+ /week → bottleneck. **DEFER khi data thực sự surface** — skip filing gap, monitor Wave 79.
- **N4-P1** Support email triage UI: integrate Gmail thread ↔ tenant ID. Overlap với GAP-040 (impersonation). Skip filing.

**Tổng Persona 4 outside-in:** 0 new P0/P1 gap file riêng. Findings được monitor, ưu tiên defer Wave 80 hoặc consolidate vào GAP-040.

---

## Inside-out vs Outside-in Overlap Map

| Inside-out gap (đã có) | Persona walkthrough cover? | Outside-in surfaced cùng issue? |
|---|---|---|
| GAP-547 (2FA endpoints undocumented + unversioned) | ✅ Persona 4 step 1 | ✅ — confirmed |
| GAP-551 (feedback routing missing tenant context + rate limit) | ✅ Persona 1 step 5 (widget) | ✅ — confirmed; thêm prefill email |
| GAP-555 (Wave 78 config keys not wired) | indirect — config drift | ✅ |
| GAP-545 (dialog focus-trap escape key) | ✅ Persona 1 (onboarding demo dialog + feedback modal) | ✅ — confirmed (a11y) |
| GAP-548 (password-reset BE controller missing) | ✅ Persona 1 step 2 (signup password UX) | ✅ — extend scope với strength meter |
| GAP-552 (SecurityConfig default-allow fallback) | n/a — not user-facing | n/a |
| GAP-553 (TOTP cipher + JWT challenge secret dev default) | n/a — security implementation | n/a |
| GAP-554 (Onboarding tenant header JWT cross-check) | indirect — tied vào RBAC | partial overlap với N2-P0 RBAC |
| GAP-544 (Subscription integration tests testcontainers) | n/a — backend tests | n/a |
| GAP-537 (User manual VN screenshots per persona) | ✅ Persona 3 (anonymous research) | partial — chỉ giải nỗi sợ "không biết dùng" |
| GAP-040 (Support impersonation) | ✅ Persona 4 step 5 | ✅ |
| GAP-556 (Support domain rules.md misleading) | n/a — internal doc | n/a |
| GAP-557 (Wave 78 use-cases missing BR refs) | n/a — internal doc | n/a |

**Coverage analysis:**
- 13 inside-out gaps = focus auth + feedback + support + onboarding internal correctness
- Outside-in surface 5 NEW gaps NGOÀI inside-out queue:
  - **GAP-558** Cookie consent banner (PDPL P0)
  - **GAP-559** Dashboard CTA + Sidebar nav cho /onboarding (P1)
  - **GAP-560** Beta disclaimer banner specificity (P1)
  - **GAP-561** invite-staff.html email template + BE endpoint (P0 cho Phase 1 P3 Manager flow)
  - **GAP-562** RBAC role separation Customer vs Staff (P0 cho multi-user trung tâm)

**Pattern:** dev brainstorm 13 gaps tập trung **internal correctness (auth/feedback/support hardening)**. Outside-in audit surface 5 gaps tập trung **multi-user + compliance + onboarding UX** — class hoàn toàn khác. Coverage hai-góc-nhìn bù trừ chứ không thay thế.

---

## Recommendations cho Wave 79 plan §1 Brainstorm Q1

### Must-add P0 (block invite launch):
1. **GAP-558** Cookie consent banner (PDPL compliance — legal risk)
2. **GAP-561** invite-staff.html email template + BE endpoint (P3 Manager flow blocker)
3. **GAP-562** RBAC role separation Customer vs Staff (multi-user trung tâm core; sensitive data leak risk)

### Should-add P1 (degrades retention nhưng không block launch):
4. **GAP-559** Dashboard CTA + Sidebar nav cho /onboarding (onboarding completion rate)
5. **GAP-560** Beta disclaimer banner specificity (user anxiety reduction)

### Wave 79 scope expansion suggestion

Wave 79 hiện đang queue 13 inside-out items. Adding 5 outside-in gaps → 18 items tổng. Recommend:

- **Wave 79 core:** 13 inside-out + 3 outside-in P0 (GAP-558/561/562) = 16 items
- **Wave 80 carry-forward:** 2 outside-in P1 (GAP-559/560) — không blocker invite
- **OR** ship 18 items qua 4-5 buckets parallel với 4-5 agents per `wave-pack-planner` pattern

### Outside-in risk surface NẾU NOT addressed:

- **GAP-558 skipped:** PDPL audit (Phase 1.5 dao động deadline) flag cookie tracking trước consent → fine + redo
- **GAP-561 skipped:** P2 Owner invite manager → "tính năng chưa hoạt động" → support ticket flood + retention loss
- **GAP-562 skipped:** P3 Manager click `/billing` → thấy thẻ tín dụng Owner → trust crisis, churn Owner toàn bộ; có thể trigger PDPL data-protection ticket nếu phát hiện ai đó thấy data mà không nên

---

## Meta observation — dev brainstorm bias

Pattern lặp lại với audit cũ (2026-05-14 PR-Phase-1-BETA-walkthrough): dev brainstorm tốt cho **internal hardening (auth/feedback/support)** nhưng miss **multi-user + compliance + UX onboarding discoverability**.

Lý do: dev tự test daily với single PLATFORM_ADMIN account + clean dev DB. Phase 1 BETA tenant THỰC TẾ = multi-user trung tâm (Owner + Manager + Accountant) + real data + VN compliance scrutiny. Persona-based outside-in caught 5 gaps mà dev không nhìn thấy.

Áp dụng `.claude/rules/outside-in-coverage-trigger.md` — Wave 79 plan §1 Brainstorm Q1 PHẢI bao gồm cả 13 inside-out + 5 outside-in finding ở trên TRƯỚC khi plan PR mở.

---

## References

- Inside-out queue: GAP-547/551/555 (P0), GAP-545/548/552/553/554/544/537/040/556/557 (P1)
- Sister audit: `documents/04-quality/audits/persona-review/2026-05-14-phase-1-beta-persona-walkthrough.md` (focus P1 Solo + P2 Center + P4 Creator pre-Wave-73)
- Rule: `.claude/rules/outside-in-coverage-trigger.md` (mandates this audit before plan PR)
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2.5/§2.6 state-check before gap filing
- Personas catalog: `documents/00-brd/personas-catalog.md`
- Wave 78 scope: closed PR #1357 (Bucket B/C/D/E/F shipped); ROADMAP §🚀 Wave 79 candidate
- Phase 1 BETA plan: `documents/03-planning/roadmap/release-1-plan-2026.md` §3 Phase 1
