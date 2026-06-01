---
audience: mixed
status: draft
created: 2026-06-01
updated: 2026-06-01
title: Zalo Integration Design — OA push + Group link (Phase 1 BETA → Phase 1.5 paid)
---

# Zalo Integration Design — OA push + Group link

**Trigger:** Audit thesis 2026-06-01 phát hiện drift — thesis Ch1 + Ch2 claim "đã kết nối / đã tích hợp Zalo OA" trong khi thực tế chỉ có **passive CTA** (deep-link button trong email + Footer + SupportMenu) shipped Wave 98 GAP-660. Active push (ZNS adapter) + Zalo Group integration chưa thiết kế chi tiết. Doc này codify hiện trạng + thiết kế Phase 1.5 path.

**Scope:** 2 distinct Zalo integration classes:
1. **Zalo OA push** — broadcast active messages (grade notification / attendance alert / fee reminder) qua Zalo Notification Service (ZNS) template-based API
2. **Zalo Group link** — per-class Zalo group chat link field (tenant manually creates group, KiteHub stores link + renders deep-link CTA — không tự create group vì Zalo không có public API)

**Status:** v1 design proposal — Phase 1.5 paid tier trigger. Phase 1 BETA only ships passive CTA (existing, no new code).

---

## 1. Hiện trạng (Phase 1 BETA — DONE)

### 1.1 Zalo OA passive CTA (GAP-660 DONE Wave 98)

| Surface | Implementation | Reference |
|---|---|---|
| Beta-invite email | Zalo OA CTA button (deep-link `https://zalo.me/<oa-id>`) | `kitehub/kitehub-email/templates/beta-invite.html` |
| Staff invite email | Same CTA | `templates/invite-staff.html` |
| Footer all FE pages | "Zalo: <oa-id>" link | `kitehub-frontend/src/components/Footer.tsx` |
| SupportMenu | Zalo entry with deep-link | `kitehub-frontend/src/components/SupportMenu.tsx` |
| Env config | `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` (default `kitehub`) | `.env.production.template` |
| Setup runbook | `documents/05-guides/integration/zalo-oa-setup-runbook.md` | Wave 98 |

**Behavior:** User clicks Zalo CTA → deep-link mở Zalo app → user phải chủ động chat với OA. **Không có push từ KiteHub → user.**

### 1.2 NotificationChannelType.ZALO enum placeholder

Per `documents/01-business/kitehub/notification/rules.md` BR-NOTIF-002:
- Enum value `ZALO` tồn tại trong `NotificationChannelType` (forward-compat)
- Phase 1 channel `kitehub.notification.channels.enabled=EMAIL` only
- UI Settings toggle "SMS/ZALO/PUSH" disabled với tooltip "Sắp ra mắt — GAP-063b"
- BR-NOTIF-010: send-time dispatcher logs `channel.disabled.in.phase1` and skips

### 1.3 Architecture doc đã có (C4 L1)

- `c4-context-container.md`: Zalo OA = external system; edge `Kite -->|OA broadcast support fast-path| Zalo` (description mismatches reality — broadcast chưa impl)
- `design-system/dossier/02-vietnamese-ux-musts.md`: Zalo OA card layout spec (320×100)
- `design-system/dossier/05-business-flows.md`: attendance/grade flow ghi "parents notified via Zalo OA" — aspirational
- `design-system/dossier/08-direction-decisions.md`: "Zalo OA primary (~95% reach VN parents) + Web Push fallback"

---

## 2. Thesis = goal state — Phase 1.5 acceptance criteria mapping

Per `thesis-as-future-state-mandate.md` (NEW META rule paired same-PR): thesis content (`documents/08-thesis/`) describes **goal state** that Phase 1.5 (paid tier) MUST deliver. Thesis wording KHÔNG sửa — Phase 1.5 features deliver thesis claims as reality.

| Thesis section | Claim (goal state) | Phase 1 BETA reality | Phase 1.5 delivery commitment |
|---|---|---|---|
| Ch1 §1.1.2 | "đã kết nối kênh Zalo OA cho liên lạc với phụ huynh" | Passive CTA (GAP-660 DONE) | Active push (ZNS) — GAP-819 P0 phase-1.5-paid |
| Ch1 §1.1.2 (P2) | "phụ huynh nhận cập nhật thường xuyên qua nhóm Zalo OA đã được kết nối" | Passive CTA + manual share Zalo group | Active ZNS push + per-class Zalo Group link — GAP-819 + GAP-820 |
| Ch1 §1.2.5 table row "Tích hợp Zalo (OA + nhóm)" KiteHub: Có | KiteHub có OA + Group | Passive CTA OA only; Group link không design | GAP-819 (OA push) + GAP-820 (Group link) |
| Ch1 §1.4 (methodology) | "kênh thông báo Zalo OA đã kết nối" | Passive CTA | GAP-819 active ZNS push |
| Ch2 §System architecture | "đã tích hợp Zalo OA" | Passive CTA + c4 diagram node | GAP-819 wires active push |
| Ch2 §Compatibility table | "kênh giao tiếp Zalo cho phụ huynh" | Passive CTA Có | ✅ Met by Phase 1 BETA (passive cũng là kênh) |

**Acceptance criteria** Phase 1.5 ship khi thỏa:
- ✅ Active ZNS adapter shipped + 3 templates approved by Zalo + 1+ tenant verify Zalo OA ownership + end-to-end grade/attendance push verified
- ✅ Zalo Group link field per-class + FE CTA "Tham gia nhóm Zalo" + audit log

Khi cả 2 ship → thesis claims "đã kết nối/đã tích hợp Zalo OA" + "phụ huynh nhận cập nhật qua nhóm Zalo OA đã được kết nối" = TRUE.

---

## 3. Phase 1.5 design — Zalo OA active push (ZNS adapter)

### 3.1 Trigger conditions

Phase 1.5 paid tier unlock khi:
- Tenant chuyển từ FREE → STARTER/PRO subscription
- Tenant verify Zalo OA ownership (cung cấp `zalo_oa_id` + ZNS template approval từ Zalo)
- Notification preference user-level enable kênh `ZALO`

### 3.2 ZNS template-based push (VNG/Zalo API constraint)

Zalo ZNS yêu cầu **template approval** trước khi gửi:
- Mỗi notification type (grade_published / attendance_alert / fee_reminder / class_canceled) cần 1 template Vietnamese approved by Zalo
- Template format: ≤500 ký tự, có placeholder `{{name}}`, `{{class}}`, `{{date}}`, etc.
- Rate limit: ~500-1000 messages/day/OA cho tier free, paid tier theo gói VNG
- Cost: ~250-500đ per message (variable theo template type)

### 3.3 Adapter design

```
┌─────────────────────────────────┐
│  NotificationProducer           │
│  (Grade / Attendance / Fee)     │
└──────────┬──────────────────────┘
           │ NotificationChannel.send(recipient, msg, ctx)
           ▼
┌─────────────────────────────────┐
│  NotificationDispatcher         │
│  - read user preference         │
│  - resolve channel order        │
│  - dispatch to adapter          │
└──────────┬──────────────────────┘
           │ ZALO channel selected
           ▼
┌─────────────────────────────────┐
│  ZaloZnsAdapter (NEW)           │
│  - template_id lookup           │
│  - param mapping                │
│  - HTTP POST → openapi.zalo.me  │
│  - retry + DLQ + audit log      │
└─────────────────────────────────┘
```

**Implementation files (Phase 1.5):**
- `kitehub/kitehub-email/src/main/java/com/kitehub/email/adapter/ZaloZnsAdapter.java` (NEW — implements `NotificationChannel`)
- `kitehub/kitehub-email/src/main/java/com/kitehub/email/zalo/ZaloZnsClient.java` (NEW — HTTP client)
- `kitehub/kitehub-email/src/main/resources/zalo-templates.yml` (NEW — local template_id ↔ NotificationType mapping)
- DB migration: add `zalo_oa_id`, `zalo_zns_template_id` columns to `tenants` table
- Config: `kitehub.notification.channels.enabled=EMAIL,ZALO` (paid tenants only via guard)

### 3.4 Fallback chain

Per BR-NOTIF-010 future scope:
- User preference order: ZALO → EMAIL (fallback)
- If ZALO send fails (rate-limit / template not approved / recipient không follow OA) → fallback EMAIL
- Audit log emit `notification.zalo.fallback` with reason

### 3.5 Acceptance criteria

- [ ] `ZaloZnsAdapter` implements `NotificationChannel` interface
- [ ] Tenant onboarding wizard step "Zalo OA configuration" với fields `zalo_oa_id` + `zns_template_id_grade` + `zns_template_id_attendance` + `zns_template_id_fee`
- [ ] At least 3 ZNS templates Vietnamese drafted + Zalo approval requested (out-of-scope code; ops work)
- [ ] IT test verifies dispatcher routes ZALO channel → ZaloZnsAdapter
- [ ] Fallback chain ZALO → EMAIL works on rate-limit/error response
- [ ] Audit log captures dispatch outcome per BR-NOTIF-001
- [ ] Cost monitoring dashboard tracks Zalo ZNS spend per tenant (CloudWatch metric)

---

## 4. Phase 1.5 design — Zalo Group link integration

### 4.1 Constraint: No Zalo Group public API

- Zalo Group (chat) = personal/public group; **không có public API** để KiteHub tự create/manage
- Per Zalo TOS, bot automation cho personal/public group bị cấm
- Realistic model: **tenant tự tạo Zalo group cho mỗi class** + paste link vào KiteHub

### 4.2 Data model

DB migration thêm columns vào `classes` table:
- `zalo_group_link` VARCHAR(500) — link Zalo group (vd `https://zalo.me/g/abcxyz`)
- `zalo_group_qr_code_url` VARCHAR(500) (optional) — QR code image URL nếu tenant upload
- `zalo_group_updated_at` TIMESTAMP

### 4.3 FE surfaces

**Teacher/Owner UI (class detail page):**
- Edit class → "Liên kết Zalo Group" field (input link / upload QR)
- Validation: regex `^https://zalo\.me/g/[a-zA-Z0-9]+$`
- Save → `PATCH /api/v1/classes/{id}` body `{ "zaloGroupLink": "..." }`

**Parent/Student UI (class card):**
- Class card hiển thị "Tham gia nhóm Zalo lớp" CTA button
- Click → mở deep-link `zalo.me/g/abcxyz` trong Zalo app
- Fallback web nếu Zalo không cài: copy link to clipboard + tooltip "Mở Zalo bằng tay"

**Notification card (Phase 1.5 trigger):**
- Khi grade/attendance published → notification card có inline Zalo group CTA "Xem cập nhật cho cả lớp tại nhóm Zalo"
- Khuyến khích parent join group nếu chưa join (no enforcement)

### 4.4 Backend

- New endpoint: `PATCH /api/v1/classes/{id}/zalo-group` (Owner/Teacher only)
- Validation: BR-CLASS-XXX (Phase 1.5+) — link format + tenant-owned class
- Audit log: `audit_logs` table entry on update

### 4.5 Acceptance criteria

- [ ] DB migration adds `zalo_group_link` + `zalo_group_qr_code_url` + `zalo_group_updated_at` to `classes`
- [ ] BE endpoint `PATCH /api/v1/classes/{id}/zalo-group` Owner/Teacher only + validation
- [ ] FE edit class form has Zalo group link input + QR upload
- [ ] FE class card renders "Tham gia nhóm Zalo" CTA cho parent/student
- [ ] Audit log capture changes
- [ ] Vietnamese tooltip + error message per `dev-readable-doc-language.md`
- [ ] IT test: cross-tenant isolation — tenant A không edit được class của tenant B

---

## 5. Out-of-scope (defer Phase 2+)

- Auto-sync grade/attendance từ KiteHub → Zalo Group (cần Zalo Group Bot API — không tồn tại public)
- Auto-create Zalo group via API (không feasible)
- 2-way messaging Zalo Group ↔ KiteHub teacher chat (manual workflow Phase 2 nếu Zalo mở API)

---

## 6. Phase plan summary

| Component | Phase 1 BETA (DONE) | Phase 1.5 paid (NEW) | Phase 2+ |
|---|---|---|---|
| Zalo OA passive CTA | ✅ GAP-660 | maintain | — |
| Zalo OA active push (ZNS) | ❌ | 🆕 GAP-819 (new) | — |
| Zalo Group link field | ❌ | 🆕 GAP-820 (new) | — |
| Zalo Group bot auto-sync | ❌ | ❌ (constraint) | possibly if Zalo opens API |
| GAP-063b Notification Phase 2 full | — | partial overlap | full scope |

---

## 7. Cross-references

- **Architecture:** `c4-context-container.md` §Zalo OA (update needed — clarify passive vs active)
- **Business rules:** `documents/01-business/kitehub/notification/rules.md` BR-NOTIF-002 + BR-NOTIF-010
- **Existing gap:** GAP-063b (P1 phase-2 Notification Phase 2 full scope — overlaps với GAP-819 narrow Phase 1.5)
- **Setup runbook:** `documents/05-guides/integration/zalo-oa-setup-runbook.md` (passive CTA setup)
- **Thesis = goal state** (per `thesis-as-future-state-mandate.md`): thesis Ch1 §1.1.2 + Ch1 §1.2.5 + Ch1 §1.4 + Ch2 claims = Phase 1.5 acceptance criteria. KHÔNG sửa thesis wording; Phase 1.5 ship sẽ make claims TRUE.

---

## 8. Log

- **2026-06-01:** v1 design created. Triggered by audit thesis 2026-06-01 phát hiện drift "đã kết nối Zalo OA" claim vs reality (passive CTA only). Doc clarifies Phase 1 BETA = passive CTA DONE; Phase 1.5 = 2 new gaps (active push GAP-819 + Zalo Group link GAP-820). Thesis wording fix tracked separately. Per `outside-in-coverage-trigger.md` §4 row 5 SKIP — user direct scope direction. Per `audit-to-gap-pipeline.md` §2.6 state-check — symbols verified: NotificationChannelType.ZALO ✅ exists / GAP-063b ✅ exists / GAP-660 ✅ closed / c4-context-container.md Zalo node ✅ exists.
