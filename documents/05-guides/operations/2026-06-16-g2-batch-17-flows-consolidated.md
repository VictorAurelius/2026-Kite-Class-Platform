---
title: G2★ Walk Batch — 17 flow pending human verification (đợt tập trung)
audience: dev
created: 2026-06-16
scope: Consolidated handoff cho 1 đợt human G2★ browser walk 17 flow còn 🔄 walk-pass-pending-human
references:
  - .claude/rules/g1-browser-walk-before-flip.md
  - .claude/rules/g2-handoff-md-mandate.md
  - documents/03-planning/roadmap/flow-verification-campaign.md
---

# G2★ Walk Batch — 17 flow (đợt tập trung)

> **Bối cảnh:** 22/22 flow đã có agent-G1-browser walk. 17 flow còn `🔄 walk-pass-pending-human` — cần **bạn (human)** tự walk qua browser thật để flip `🟢 THÔNG (local)`. Agent-headless ≠ human G2★ (per `g1-browser-walk-before-flip.md`). G3-infra (TLS/LB/wildcard) gated GAP-612 (AWS).
>
> **Cách dùng:** mỗi flow có recipe chi tiết riêng (cột Recipe). File này = **index + setup chung** để bạn walk liên tục, giảm context-switch.

---

## 0. Setup chung (làm 1 lần trước cả đợt)

```bash
# 1. Stack up (nếu chưa chạy)
cd kitehub && bash scripts/status.sh        # kiểm tra full stack healthy
# nếu kiteclass-core KHÔNG healthy → đọc KC-5 recipe §FM-1 (V87 crash-loop)

# 2. Seed parent cho KC-8 (chỉ KC-8 cần)
docker exec -i kite-postgres psql -U kiteclass -d kiteclass_shared \
  < kiteclass/kiteclass-core/scripts/dev-seed-parent-kc8.sql

# 3. Sau mỗi lần rebuild FE → restart để nip.io resolve đúng (gotcha GAP-1067)
docker restart kiteclass-frontend kitehub-frontend
```

**Access mode (per `kitehub-kiteclass-boundary`):**
- **KiteHub flow (KH-*)** → `http://localhost:3001` (KH portal resolve tenant qua JWT claim — localhost OK, production-accurate).
- **KiteClass flow (KC-*)** → `http://<subdomain>.127.0.0.1.nip.io:3000` (host-based, **CẤM** `localhost:3000?tenant=`). Subdomain = cột `instances.subdomain`.

**Credentials chính:**
| Persona | Login | Dùng cho |
|---|---|---|
| Owner (KH/KC) | `owner@skyedu.vn` / `SkyEdu@2026` | KC-1/2, KH-5..10 |
| Owner (tenant 074901) | `owner+074901@skyedu.vn` / `SkyEdu@2026` | KC-3..7 (sky-education-074901) |
| Parent | `hong.tran+074901@gmail.com` / `Parent@123` | KC-8 (sau seed) |
| Admin nền tảng | `admin@kitehub.com` (login trực tiếp, không 2FA) | KH-9 admin console |

**Tenant test:** `sky-education-074901` (TRIAL, 2 courses + 7 classes + students), `sky-education-171900`, `co-ha-toan` (ACTIVE), `thay-nhi-hoa` (ACTIVE).

---

## 1. Đợt A — KiteHub flows (`:3001`, login owner@skyedu.vn) — 6 flow

| Flow | Verify-focus (1 dòng) | Recipe | Residual đã biết |
|---|---|---|---|
| **KH-5** Subscription downgrade/cancel/renew | downgrade tier + cancel (DangerZone) + renew; sad-path | `2026-06-06-g2-recipe-kh5-subscription-lifecycle.md` | GAP-1015/1016/1017 (Phase-3 fixed, re-walk confirm) |
| **KH-6** AI Branding wizard | generate theme → job async → assets → apply | `2026-06-06-g2-recipe-kh6-ai-branding-wizard.md` | GAP-1019/1020/1021 |
| **KH-7** Custom domain | add domain → PENDING_VERIFY → delete; sad-path | `2026-06-06-g2-recipe-kh7-domain-management.md` | **agent walk ✅ FULL PASS**; GAP-1462 error-msg (cosmetic) |
| **KH-8** Off-boarding + PDPL consent + DSAR | consent v1/v2 + DSAR submit + off-boarding | `2026-06-06-g2-recipe-kh8-offboarding-pdpl-consent.md` | GAP-1025 (Phase-3 fixed) |
| **KH-9** Admin console | dashboard + instance suspend/activate + beta-requests (login **admin@kitehub.com**) | `2026-06-06-g2-recipe-kh9-admin-console.md` | GAP-1028/1029 audit-log |
| **KH-10** Notification/email/feedback/support | feedback submit + notif-prefs + admin-email console | `2026-06-06-g2-recipe-kh10-notification-email-feedback-support.md` | GAP-1031 (Phase-3 fixed) |

---

## 2. Đợt B — KiteClass flows (`:3000` nip.io, login owner+074901@skyedu.vn) — 11 flow

> Access: `http://sky-education-074901.127.0.0.1.nip.io:3000` (verify subdomain resolve trước).

| Flow | Verify-focus (1 dòng) | Recipe | Residual đã biết |
|---|---|---|---|
| **KC-1** Tenant settings (branding + preferences) | settings → branding PUT persist → version-history | `2026-06-05-g2-recipe-kc1-tenant-settings.md` | **agent ✅ PASS 0 bug**; GAP-1461 cosmetic |
| **KC-2** Staff invite → accept → RBAC | mời nhân viên → email accept link → STAFF login → role scope (flow này `:3001`) | `2026-06-05-g2-recipe-kc2-staff-invitation.md` | **FM-1 fixed**; GAP-1459 STAFF dashboard |
| **KC-3** Academic: course→class→schedule | tạo course → class → schedule recurrence | `2026-06-05-g2-recipe-kc3-academic-course-class-schedule.md` | **agent ✅ PASS**; niên-khóa GAP-982 (no UI) |
| **KC-4** Student enrollment + bulk import | enroll student + bulk CSV import | `2026-06-05-g2-recipe-kc4-enrollment-bulk-import.md` | GAP-1424/1425 fixed |
| **KC-5** Attendance: mark → period | điểm danh single + bulk + stats | `2026-06-05-g2-recipe-kc5-attendance.md` | GAP-991..996 fixed |
| **KC-6** Grade → report card → gradebook | nhập điểm → calculate → finalize → transcript | `2026-06-05-g2-recipe-kc6-grade.md` | GAP-998/999 fixed |
| **KC-7** Invoice → payment record → reconcile | record-payment → reconcile SENT→PARTIAL→PAID | `2026-06-05-g2-recipe-kc7-invoice-payment.md` | GAP-1003 fixed |
| **KC-8** Parent portal | **login PARENT** (hong.tran+074901) → child facets → consent gate | `2026-06-05-g2-recipe-kc8-parent-portal.md` | **agent ✅ PASS** sau seed; FE facet mock Phase 1.5 (GAP-1458) |
| **KC-10** Per-tenant branding wizard | branding wizard → approval | `2026-06-06-g2-recipe-kc10-per-tenant-branding-wizard.md` | GAP-1034/1035 (Phase-3 fixed) |
| **KC-11** Notification (Zalo+email) + document gen PDF | doc gen pdf/xlsx/docx + reports | `2026-06-06-g2-recipe-kc11-notification-document-gen.md` | GAP-1039/1040 fixed |
| **KC-12** Reschedule / payroll | reschedule + payroll backend | `2026-06-06-g2-recipe-kc12-reschedule-payroll.md` | GAP-1041 (Phase-3 fixed) |

---

## 3. Báo kết quả mỗi flow (per `g2-handoff-md-mandate` §3)

Sau khi walk mỗi flow, báo 1 trong 4 outcome:
1. **✅ PASS** — mọi affordance OK → tôi flip `🟢 THÔNG (local)` + evidence vào campaign §4.
2. **⚠️ PASS-with-bug** — chạy được nhưng có bug nhỏ → tôi file gap + fix inline nếu ≤30p.
3. **🔴 BLOCKED** — không qua được 1 bước → tôi điều tra + fix → bạn re-walk.
4. **❓ unclear** — không chắc expected → tôi clarify từ rules.md/use-cases.md.

**Khi báo bug:** ghi (a) flow + bước, (b) làm gì, (c) thấy gì vs mong đợi, (d) screenshot/console nếu có.

---

## 4. Sau khi 17 flow PASS

- Tôi flip campaign §4 17 row → `🟢 THÔNG (local)` + evidence.
- G3-infra (TLS/LB/wildcard-cert/real-DNS) cho cả 22 → chờ **GAP-612** (AWS restore) → đợt G3 riêng.
- Campaign §6: 22 ✅ THÔNG (local) → quay lại fix-gap-theo-wave cho backlog cosmetic.
