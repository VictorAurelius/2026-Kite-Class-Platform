---
title: Flow Verification Campaign — thông toàn bộ flow Phase 1 BETA
status: active
created: 2026-06-04
updated: 2026-06-04
waves: []
gaps: [GAP-914]
---

# Flow Verification Campaign

**Mục tiêu:** Thông (verify chạy end-to-end) toàn bộ ~22 user-facing flow của KiteHub + KiteClass cho Phase 1 BETA. Sau khi 22 flow ✅ mới quay lại quy trình fix-gap-theo-wave cho backlog cosmetic.

**MODE hiện tại (override default session priority):** KHÔNG pick P0 gap từ triage để fix. Thay vào đó: chọn flow chưa thông kế tiếp (theo §3 dependency order) → loop qua wave plan của flow đó → chỉ fix **blocker do walk lòi ra**. Gap KHÔNG chặn flow (cosmetic P2/P3) defer sang wave-fix phase sau campaign.

---

## 1. Định nghĩa "1 flow THÔNG" — 3 gate (tất cả phải PASS)

| Gate | Ai | Tiêu chí |
|---|---|---|
| **G1 — Agent runtime walk** | Claude | Walk end-to-end trên stack thật (Postgres + services), happy + ≥1 sad path PASS; fix mọi blocker lòi ra; evidence (HTTP + DB row + side effect) |
| **G2 — Human real local test** | Dev (user) | Con người tự test flow trên local stack thành công (UI/API), xác nhận trải nghiệm thật đúng — KHÔNG chỉ tin agent walk |
| **G3 — Production-parity guarantee** | Claude + Dev | Local PASS phải **đảm bảo 100% chạy production**: walk trên production-equivalent (cùng Docker image tag, Postgres+Flyway+RLS thật KHÔNG H2, gateway JWT→header auth, prod-profile config, env-var đủ). Per `local-fix-production-parity-check.md` + bài học H2-giấu-bug (GAP-914). Nếu local≠prod ở điểm nào → note + đảm bảo trước khi flip thông |

Flow chỉ `✅ THÔNG` khi G1 + G2 + G3 đều PASS. G1 đạt → `🔄 walk-pass-pending-human` chờ G2.

---

## 2. Loop protocol mỗi flow (per `feature-ship-runtime-walk-mandate` §3.4)

```
1. Tạo wave plan cho flow (lazy — khi bắt đầu loop)
2. Stack up production-equivalent + persona credential
3. Walk → CATALOG mọi blocker đến hết walk (không rebuild giữa chừng)
4. Batch-fix P0/P1 blocker (DB drift / wiring / entity) → single rebuild
5. Re-walk full flow
6. Lặp 3-5 đến khi mọi path G1 PASS
7. Hand cho human (G2) + xác nhận parity (G3)
8. Flip flow ✅ + evidence vào wave plan + campaign table
```

Gap chặn flow lòi ra → fix tại chỗ + file gap inline (per `discovery-to-gap-inline-filing`). Gap không chặn → ghi defer, không fix trong campaign.

---

## 3. Dependency graph (state-checked 2026-06-04) — quyết định priority

Edges xác minh từ use-cases `Precondition` + entity FK (12→students, 7→classes, 4→courses):

```mermaid
flowchart TD
    KH2[KH-2 Auth login/2FA] --> KH1[KH-1 Beta funnel → provision instance]
    KH2 --> ALL[mọi flow authenticated]
    KH1 --> KH3[KH-3 Subscription create/trial→paid]
    KH1 --> KC1[KC-1 Tenant provisioning/settings]
    KC1 --> KC2[KC-2 Staff invite + RBAC]
    KC2 --> KC3[KC-3 Course/class/schedule]
    KC3 --> KC4[KC-4 Student enrollment]
    KC4 --> KC5[KC-5 Attendance]
    KC4 --> KC6[KC-6 Grade/report card]
    KC4 --> KC7[KC-7 Invoice/payment — ENROLLMENT_CREATED event]
    KC5 --> KC8[KC-8 Parent portal]
    KC6 --> KC8
    KC7 --> KC8
    KC4 --> KC9[KC-9 Student portal]
    KH4[KH-4 Subscription upgrade ✅ VERIFIED] -.-> KH1
```

**Thứ tự loop chuẩn hóa (topological):** KH-2 → KH-1 → KH-3 → KC-1 → KC-2 → KC-3 → KC-4 → {KC-5, KC-6, KC-7 song song} → {KC-8, KC-9}. Secondary (độc lập, sau core): KH-5/6/7/8/9/10, KC-10/11/12.

---

## 4. Flow inventory + status (22 flow)

| # | Flow | Priority | Status | Wave plan | Blocker đã biết |
|---|---|---|---|---|---|
| KH-2 | Auth + onboarding (register→verify→login→2FA→wizard) | 1 | ⬜ | — | — |
| KH-1 | Beta funnel: request→admin approve→email invite→provision | 2 | ⬜ | — | — |
| KH-3 | Subscription create + trial→paid migration | 3 | ⬜ | — | — |
| KH-4 | **Subscription upgrade manual VietQR + admin confirm** | — | ✅ THÔNG (G1) | — | (GAP-914 fixed) |
| KC-1 | Tenant provisioning + lifecycle + settings | 4 | ⬜ | — | — |
| KC-2 | Staff invitation → accept → RBAC role | 5 | ⬜ | — | GAP-886/893 (role) |
| KC-3 | Academic: year→course→class→schedule | 6 | ⬜ | — | GAP-909 (course drift) |
| KC-4 | Student enrollment + bulk import | 7 | ⬜ | — | — |
| KC-5 | Attendance: mark → period | 8 | ⬜ | — | GAP-874 (attendance drift) |
| KC-6 | Grade → report card → gradebook | 8 | ⬜ | — | GAP-875 (grading drift) |
| KC-7 | Invoice → payment record → reconcile | 8 | ⬜ | — | 🔴 GAP-882 (CHECK) + GAP-879 |
| KC-8 | Parent portal (child grade/attendance/payment) | 9 | ⬜ | — | — |
| KC-9 | Student portal (today/grades/notif) | 9 | ⬜ | — | — |
| KH-5 | Subscription downgrade/cancel/renew | sec | ⬜ | — | — |
| KH-6 | AI Branding wizard generate→apply→approval | sec | ⬜ | — | — |
| KH-7 | Custom domain / domain management | sec | ⬜ | — | — |
| KH-8 | Off-boarding + data retention (PDPL) + consent | sec | ⬜ | — | — |
| KH-9 | Admin console: instance/audit/beta-request mgmt | sec | ⬜ | — | (1 phần qua KH-4) |
| KH-10 | Notification/email/feedback/support | sec | ⬜ | — | — |
| KC-10 | Per-tenant branding wizard → approval | sec | ⬜ | — | — |
| KC-11 | Notification (Zalo OA+email) + document gen PDF | sec | ⬜ | — | GAP-721 (Zalo stub) |
| KC-12 | Reschedule / payroll / gamification / analytics | sec | ⬜ | — | — |

Status: ⬜ chưa walk · 🔄 G1 pass chờ human (G2) · ✅ THÔNG (G1+G2+G3).

---

## 5. Per-flow wave plan convention

Mỗi flow → 1 wave plan tag `flow` tạo **lazy** (khi bắt đầu loop), per `wave-tag-numbering-convention`:
`documents/03-planning/waves/wave-{YYYY-MM-DD}-flow-{kh2|kc4|...}-{slug}.md`. Plan chứa loop §2 + 3-gate §1 + parity §1-G3. Campaign table cột "Wave plan" link tới khi tạo.

---

## 6. Khi campaign xong (22 ✅)

Quay lại wave-based gap-fix cho backlog cosmetic còn lại (31 anomaly gap Wave 13 đa số P2/P3 + các gap non-flow-blocking). Per `meta-gap-priority` + audit-to-gap-pipeline.

## 7. Log

- **2026-06-04**: Campaign tạo. Dependency graph state-checked (auth=root, enrollment gate cho attendance/grade/invoice). 3-gate "thông" định nghĩa (agent walk + human local test + production-parity). KH-4 đã ✅ G1 (phiên billing verify, GAP-914 fixed). Order chuẩn hóa topo. Wave plan lazy per flow.
