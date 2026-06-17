---
title: Session Handoff — KC-1/KC-3 G2 flow-walk fixes + KH-3 real VietQR setup
date: 2026-06-17
scope: Flow Verification Campaign G2 — KC-1 color-picker, KC-3 session-mgmt UI, KH-3 QR real
audience: dev
---

# Session Handoff 2026-06-17 — KC-1 + KC-3 G2 walk fixes + KH-3 real QR

## Scope shipped (3 PR merged, 1 open)

| PR | Nội dung | Status |
|---|---|---|
| **#2461** | GAP-1467 KC-1 branding color-picker dual-register → controlled inputs (picked color now saved) | ✅ merged · GAP-1467 **DONE** · KC-1 🟢 THÔNG (local) |
| **#2462** | GAP-1468 KC-3 post-creation session-management UI (class detail → "Tạo buổi học theo lịch" → RecurrenceForm → generate-from-recurrence) | ✅ merged · GAP-1468 **PARTIAL** (chờ human G2★) |
| **#2463** | GAP-1470 data-table row-click → detail (UX enhancement) | ✅ merged · **DEFER** ("section sau làm") |
| **#2464** | GAP-1469 KH-3 QR fix (`unoptimized` + allowlist) | 🟡 **OPEN** · CI running · GAP-1469 PARTIAL chờ re-walk |

## Verified live (G2 / agent browser-walk, nip.io)
- **KC-1**: human G2★ FULL PASS — color picker controlled, server color persist `#3bf79f/#5ff7d8/#b71053` vs old `#3B82F6` + PUT 200.
- **KC-3 session creation**: agent browser-walk (section + nút + dialog render) + BE round-trip `generate-from-recurrence` **0 → 20 sessions** (class 26). UI LIVE trên kiteclass-frontend.

## Pickup — việc đầu tiên session sau

1. **QR re-walk (KH-3, GAP-1469 → DONE):** real VietQR đã set trong `kitehub/.env` (`PAYMENT_MOCK_MODE=false` + MB `0988269432`, persist). kitehub-frontend đã có `unoptimized` fix deployed. → Human: KiteHub `:3001` **nâng gói LẠI từ đầu** (payment MỚI — payment PENDING cũ `e294eb33` đã bake mock qrCodeUrl) → QR MB thật scannable. Nếu PASS → merge **#2464** + flip GAP-1469 DONE. (Memory: `reference_kh3_qr_vietqr_real_setup.md`.)
2. **#2464 merge** sau re-walk PASS (CI FE-build hiện đang chạy; chưa merge per end-session gotcha).
3. **KC-3 GAP-1468 human G2★** click-through thật (agent-walk chưa thay human) → flip DONE.
4. (Optional) **GAP-1470** implement row-clickable table (DEFER — stopPropagation actions + keep Link tên + consider all data tables).
5. **SePay full auto-confirm** (nếu muốn KH-3 end-to-end): tunnel + `SEPAY_API_KEY` per `sepay-webhook-local-verify-recipe.md`.

## Background services / state (survive /clear)
- Docker stack UP + healthy: gateway/kiteclass-core/2 FE/infra + kitehub-subscription (rebuilt mock-off real-VietQR). `kitehub-admin` cycling unhealthy (KH-9 only, non-blocker).
- Tenant `g2walk` (seed `bash kitehub/scripts/seed-walk-tenant.sh` idempotent). Class 26 = 20 sessions (từ verify).
- `kitehub/.env` đã thêm PAYMENT_MOCK_MODE=false + VIETQR MB (gitignored, persist).

## Known issues / cleanup
- **#2464 (QR)** CI FE-build chạy — verify trước merge.
- **4 stale agent-worktree husks** (a10e0/a307d/a5f60/a7d61) dirty từ phiên TRƯỚC — chưa triage (carry-forward từ start handoff 2026-06-16). + ~44 husk tổng → `bash scripts/prune-merged-worktrees.sh --dry-run`.
- 2 worktree phiên này còn (kite-wt-kh3qr QR-branch pushed #2464 · kite-wt-handoff này) → remove sau merge.
- `documents/03-planning/pr-logs/PR-2398.json` untracked (pre-existing từ phiên trước).
- 🔒 **hygiene**: số TK MB `0988269432` nằm trong handoff committed `2026-06-09-...-kh3-g2-pass.md:31` + giờ handoff này — leak nhẹ, mask sau (follow-up, không gấp).
- Meta: GAP-1468 E2E `class-lifecycle.spec` đã fix `.first()` (section "Buổi học" mới collide strict-mode — sweep per cross-flow-bug-class-sweep).
