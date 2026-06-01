---
audience: dev
---

# GAP-773 — KC `/staff/accept-invite` FE route 404 (Mảng C1 blocker)

**Status:** 🟢 DONE — SUPERSEDED (FE route relocated to kitehub-frontend per GAP-786 Wave A Bucket B 2026-05-28)
**Priority:** 🔴 P0
**Domain:** Frontend
**Found:** 2026-05-27 (Wave 106 RST Mảng C catalog probe)
**Affects:** C1 Nhân viên nhận thư mời → đăng ký
**Phase:** phase-1-beta

## Problem

`curl -sI http://localhost:3000/staff/accept-invite` → HTTP 404.

FE route catalog:
```
kiteclass-frontend/src/app/(auth)/parent-invite          ← PARENT only
kiteclass-frontend/src/app/(auth)/parent-invite/[token]
# KHÔNG có (auth)/staff/* hoặc staff/accept-invite
```

Wave 106 plan §3 C1 expects staff click invite link → land claim page. Page không tồn tại — invite email sẽ link tới dead URL.

## Root Cause

Pair với GAP-772 (BE controller missing) — FE chưa được build vì BE chưa có endpoint.

## Proposed Fix

Pair fix với GAP-772 cùng PR khi quyết định Option A vs B:
- Option A: thêm `(auth)/staff/accept-invite/[token]/page.tsx` (mirror parent-invite pattern); + role-guard render Vietnamese form per `vn-localization-audit-checklist.md` §2
- Option B: defer — không tạo route, plan §3 C đánh dấu out-of-scope Phase 1 BETA

## Resolution — SUPERSEDED by GAP-786 (Wave A Bucket B, 2026-05-28)

Paired với GAP-772. State-check 2026-06-01 (Wave beta-readiness-9 Bucket B): scope staff-invite (BE+FE) đã bị đảo ngược + relocate khỏi kiteclass-core/kiteclass-frontend sang **kitehub-subscription + kitehub-frontend** bởi GAP-786 (Wave A Bucket B, 2026-05-28) — lý do cross-DB (kiteclass-core không share `UserRepository` với kitehub-subscription). Xem GAP-772 §Resolution cho diễn biến đầy đủ.

→ FE accept-invite route **không cần tạo trong kiteclass-frontend**. Canonical FE route đã tồn tại + vận hành trong kitehub-frontend.

### Canonical FE đang vận hành (verified 2026-06-01)

| Route | Location | Evidence |
|---|---|---|
| Public accept-invite | `kitehub-frontend/src/app/(public)/staff/accept-invite/page.tsx` | gọi `GET /api/v1/staff-invitations/by-token/{token}` (preview tenant + role) → password-set form (A07: ≥12 ký tự + mixed-case + digit) → `POST /api/v1/staff-invitations/{token}/accept` |
| Owner invite UI | `kitehub-frontend/src/app/(admin)/admin/staff/invite/page.tsx` | Owner gửi invite (email + role) |

Email invite link trỏ tới `{inviteBaseUrl}/staff/accept-invite?token={rawToken}` (per `StaffInvitationController` dòng 273) — match kitehub-frontend public route, KHÔNG dead URL.

## Acceptance Criteria

- [x] Decision sync với GAP-772 — cả hai SUPERSEDED, scope relocate sang kitehub-subscription + kitehub-frontend
- [x] Route exists + render form: `kitehub-frontend/.../(public)/staff/accept-invite/page.tsx` (canonical, NOT kiteclass-frontend)
- [x] kiteclass-frontend route không cần tạo — superseded reason logged

## Related

- **Supersedes this gap:** GAP-786 (Wave A Bucket B) — `closed/GAP-786-staff-invite-accept-user-provision-missing.md`
- Sister BE: GAP-772 (closed cùng PR này, same superseded reason)
- GAP-790 (gateway staff-invitations route — public token paths included) — DONE
- Wave 106 plan §3 C1 — `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`

## Log

- **2026-06-01** (Wave beta-readiness-9 Bucket B) — flip 🟢 DONE SUPERSEDED, paired với GAP-772. State-check empirical (per `audit-to-gap-pipeline.md` §2.8): canonical FE accept-invite route đã tồn tại trong kitehub-frontend `(public)/staff/accept-invite/page.tsx` + Owner invite UI `(admin)/admin/staff/invite/page.tsx`; email link trỏ đúng route (không dead URL). Scope kiteclass-frontend bị reversal bởi GAP-786 (cross-DB). NO build PR — flip DONE với findings per §2.8 decision matrix. git mv → phase-1-beta/closed/.
