# GAP-820: Zalo Group link integration per-class (Phase 1.5 paid tier)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-01 (Zalo audit + thesis-as-future-state-mandate rule landing)
**Phase:** phase-1.5-paid
**Affects:** Class entity + Parent/Student class card UI + Teacher/Owner class edit form

## Problem

Thesis Ch1 §1.2.5 row "Tích hợp Zalo (OA + **nhóm**)" claim KiteHub có Zalo group integration. Reality: KHÔNG có design/code cho Zalo group anywhere. Thesis Ch1 §1.1.2 P2 cũng claim "phụ huynh nhận cập nhật thường xuyên qua **nhóm Zalo OA** đã được kết nối" — conflate Zalo OA (broadcast) với Zalo Group (chat).

Per `thesis-as-future-state-mandate.md` v1.0.0 §3.1 — Phase 1.5 paid tier MUST deliver "Group link" interpretation. Constraint: Zalo Group KHÔNG có public API → KiteHub không tự create group, chỉ store link + render CTA.

## Thesis source

- Ch1 §1.2.5 competitor table row "Tích hợp Zalo (OA + nhóm)" KiteHub: Có
- Ch1 §1.1.2 P2: "phụ huynh nhận cập nhật thường xuyên qua nhóm Zalo OA đã được kết nối"
- Ch1 §1.1.3 user needs: "kênh liên lạc trực tiếp với giáo viên qua Zalo (90% phụ huynh dùng Zalo)"

## Root cause

Zalo Group = personal/public group chat; Zalo TOS cấm bot automation cho personal groups. KhÔng có public API để KiteHub tự create/manage group. Realistic model: **tenant manually creates Zalo group cho mỗi class** + paste link vào KiteHub field + KiteHub renders deep-link CTA "Tham gia nhóm Zalo lớp".

## Proposed fix

Per design doc `documents/02-architecture/zalo-integration-design.md` §4:

1. DB migration: add `zalo_group_link` VARCHAR(500) + `zalo_group_qr_code_url` VARCHAR(500) + `zalo_group_updated_at` TIMESTAMP to `classes` table
2. BE endpoint: `PATCH /api/v1/classes/{id}/zalo-group` Owner/Teacher only + validation regex
3. FE Teacher/Owner class edit form: "Liên kết Zalo Group" field (input link + optional QR upload)
4. FE Parent/Student class card: "Tham gia nhóm Zalo lớp" CTA button → deep-link `https://zalo.me/g/...`
5. FE Notification card (Phase 1.5 trigger): inline Zalo group CTA khi grade/attendance published — "Xem cập nhật cho cả lớp tại nhóm Zalo"
6. Audit log: `audit_logs` entry on Zalo group link update (capture old + new value)
7. Cross-tenant isolation IT: tenant A không edit được class B; class B Zalo link không leak qua tenant A view

## Acceptance criteria

- [ ] DB migration adds 3 columns to `classes` table (link + QR URL + updated_at)
- [ ] BE endpoint `PATCH /api/v1/classes/{id}/zalo-group` Owner/Teacher only via @PreAuthorize
- [ ] Validation: link regex `^https://zalo\.me/g/[a-zA-Z0-9]+$`; reject malformed
- [ ] FE class edit form Vietnamese label "Liên kết nhóm Zalo lớp" + tooltip explain manual create
- [ ] FE class card Parent/Student view renders "Tham gia nhóm Zalo" CTA (nếu link exists)
- [ ] FE fallback: nếu Zalo không cài → copy link to clipboard + tooltip "Mở Zalo bằng tay"
- [ ] Audit log entry on each update với old + new value
- [ ] IT test cross-tenant isolation — tenant A không edit được class B + link không leak
- [ ] IT test validation — malformed link returns 400 với Vietnamese error message
- [ ] api-contract.md updated với new endpoint
- [ ] Business doc `documents/01-business/kiteclass/class/use-cases.md` updated với UC mới "Owner gắn Zalo group link cho class"

## Future scope (defer Phase 2+)

- Auto-sync grade/attendance từ KiteHub → Zalo Group (cần Zalo Group Bot API — không tồn tại public hiện tại)
- Auto-create Zalo group via API (không feasible per Zalo TOS)
- 2-way messaging Zalo Group ↔ KiteHub teacher chat (manual workflow only)

## Related

- Design doc: `documents/02-architecture/zalo-integration-design.md` §4 (paired same-PR)
- Sister rule: `.claude/rules/thesis-as-future-state-mandate.md` v1.0.0 (paired same-PR)
- Sister gap: GAP-819 Phase 1.5 Zalo OA active push (paired)
- Phase 1 minimum: passive Zalo CTA (Footer, SupportMenu) GAP-660 DONE
- Business doc: `documents/01-business/kiteclass/class/use-cases.md` (extend trong fix PR)
