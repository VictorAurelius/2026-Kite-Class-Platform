---
title: Wave RBAC-Shell 1 — KiteClass role-based login routing + per-role dashboard shell
status: draft
created: 2026-06-10
updated: 2026-06-09
waves: [rbac-shell-1]
tag_primary: rbac-shell
tags_secondary: [kiteclass, auth, role-shell, beta-prep]
counter: 1
gaps: [GAP-1119]
audience: dev
---

# Wave RBAC-Shell 1 — KC role-based login routing + per-role shell

> **Trạng thái:** DRAFT thảo luận — chờ user review/chốt scope. KHÔNG auto-merge.
> **Tiền đề:** chạy TRƯỚC/CÙNG `wave-lms-fe-1` (LMS surfaces cắm lên shell này).

## TL;DR
Tầng **nền FE role-shell** cho KiteClass — foundation cho mọi KC user-facing surface (LMS, course/class, grade, attendance, billing học phí, parent/student portal). Đóng **GAP-1119**. 4 role-shell buildable ngay (owner/staff/teacher/parent); student-shell scaffold + gated **KC-9** (student-auth pending).

## Quyết định đã chốt (2026-06-10, user — GAP-1119)
1. **RBAC depth = fixed-curated cho beta** — ship 5 role template seeded (OWNER/STAFF/TEACHER/PARENT/STUDENT); owner CHỈ gán user→role; KHÔNG dựng UI owner-sửa-permission-per-role; BE giữ dynamic-capable; defer permission-edit UI Phase 3.
2. **Owner/Staff auth = cross-product SSO KH→KC** — giữ split: OWNER/STAFF login KH `:3001`; token KH-minted handoff sang KC `:3000` qua shared gateway cho school-mgmt. TEACHER/PARENT/STUDENT login thẳng KC.
3. **Route quản-quyền (assign user→role) ở KC owner-shell** — role-hierarchy là KC domain (per-tenant school roles); KHÔNG ở KH.
4. **Invite split STAFF(KH)/TEACHER(KC) giữ nguyên + document rõ** → xem `documents/03-planning/plans/invite-flow-redesign-discussion-2026-06-09.md` (deliverable thảo luận riêng — user quyết định scope multi-role + bulk).

## 1. Brainstorm

### Outside-in
ĐÃ audit (persona lens trong `2026-06-10-pre-wave-lms-fe-outside-in.md` flagged owner/parent/admin scope). Decisions chốt GAP-1119: RBAC fixed-curated + cross-product SSO KH→KC + route quản-quyền KC.

### Pre-walk persona simulation (BẮT BUỘC per `pre-walk-persona-simulation-mandate.md`)
Login→role-redirect = auth flow → §2 trigger fires. Spawn Opus pre-walk agent cho mỗi role login walk (owner/staff/teacher/parent) return ≥5 failure mode (wrong-role redirect / guard bypass / SSO token loss / cross-tenant leak / stale session). Bucket 0 (pre-walk) thêm §2.

### Deps + Risk chính
- Auth split KH/KC đã có (TEACHER/PARENT/STUDENT KC-native; OWNER/STAFF KH).
- **Risk #1 (HIGH) — cross-product SSO KH `:3001` → KC `:3000`:** token handoff qua shared gateway, phần khó nhất. Cần design token-share/SSO-redirect (gateway JWT shared secret OR redirect-with-token). Bucket C risk-isolated.
- **Risk #2 — KC-9 student-auth blocker:** student-shell chỉ scaffold được, KHÔNG functional cho tới khi KC-9 ship.
- **Risk #3 — role-name parity BE seed vs FE guard** (per `pre-handoff-self-test-completeness.md` §2.4): grep BE `RoleSeederService` literal vs FE RoleGuard literal, reconcile (đã từng có `PLATFORM_ADMIN` vs `ADMIN` drift Wave 78 GAP-518).

## 2. Task Breakdown

Buckets (disjoint, worktree-parallel, Opus):

| Bucket | Scope | Dep | Walk class |
|---|---|---|---|
| **0** (Pre-walk) | Spawn Opus agent simulate mỗi role login walk → ≥5 failure modes per `pre-walk-persona-simulation-mandate.md` §3; save artifact persona-review/ | none | n/a |
| **A** — Login→role-redirect + role-guard | KC login đọc JWT `role` claim → redirect role-home; `RoleGuard` HOC/middleware chặn route group theo role (teacher KHÔNG vào owner route...) | Bucket 0 | auth flow ✅ pre-walk |
| **B** — Per-role nav + dashboard home | 4 shell owner/staff/teacher/parent (nav + home theo bảng GAP-1119); student-shell **scaffold gated KC-9** | A | user-facing ✅ pre-walk |
| **C** — Cross-product SSO KH→KC | owner/staff login KH `:3001` → handoff token sang KC `:3000` school-mgmt (gateway shared JWT / SSO redirect) | A | auth flow ✅ pre-walk (Risk #1) |
| **D** — RBAC management UI (fixed-curated) | KC owner-shell: assign user→role (5 template seeded `RoleSeederService`); KHÔNG dựng edit-permission-per-role UI | A | user-facing ✅ pre-walk |
| **E** — Doc invite split + business sync | doc hoá STAFF(KH StaffInvitation)/TEACHER(KC provision) split per `documents/03-planning/plans/invite-flow-redesign-discussion-2026-06-09.md`; sync role-hierarchy + tenant-auth + architecture business docs | none | n/a (docs) |

### Per-role dashboard (grounded 5 role + KC domain — từ GAP-1119)
| Role | Login → thấy | Login ở |
|---|---|---|
| OWNER | toàn quyền: mọi course/class + gán GV + students + billing học phí + payroll + branding + settings + analytics + role-assign | KH `:3001` → SSO KC `:3000` |
| STAFF | subset owner theo permission bundle (enrollment + attendance + invoice + staff) | KH `:3001` → SSO KC `:3000` |
| TEACHER | my courses/classes + LMS authoring + attendance + grade entry + completion roster | KC `:3000` native |
| STUDENT | chỉ học tập: my classes + lesson player + assignments + grades + progress + attendance + payments (own) — **chờ KC-9** | KC `:3000` (gated) |
| PARENT | child read-only: progress/grades/attendance/fees + notify (Zalo) | KC `:3000` native |

## 3. Scope

### Scope-completeness reconciliation (per `wave-closure-scope-completeness.md` — fill at closure)
| # | Plan §2 item | Verdict | Follow-up |
|---|---|---|---|
| _(điền tại closure)_ | | | |

## 4. State-Check Evidence

Bối cảnh điều tra (đã làm session 2026-06-10, design-first):

### BE — RBAC + auth-by-role đã thiết kế tách KH/KC (đã verify code)
- **RBAC dynamic-capable:** `kiteclass-core/module/role` có `Role` + `Permission` + `RoleService` + role-hierarchy (BR-ROLE Level 1-10, role bundle permissions, custom role per-tenant, seeded `RoleSeederService`). Design ADR-003-role-hierarchy.
- **Auth split KH/KC** (per `tenant-auth/rules.md` BR-AUTH-002 + `kitehub-kiteclass-boundary.md` §2):
  - **OWNER/STAFF login KiteHub** (`kitehub-subscription` `/api/v1/auth/**`, FE `:3001`, KHÔNG nằm trong `auth_credentials` KC).
  - **TEACHER/PARENT/STUDENT login KiteClass** (tenant-auth Option B, `/api/v1/tenant-auth/login`, FE `:3000`, `entity_type CHECK ∈ {PARENT,TEACHER,STUDENT}` V89:22). Teacher chỉ KC.
- Parent + Teacher auth ĐÃ PULLED FORWARD Phase 1 (Option B KC-native, working end-to-end PR #2186 per memory `project_parent_student_portal_phase2_gated`). Student + KC-9 vẫn pending.

### FE — role-shell YẾU/thiếu (đã verify code)
- Có route group `(dashboard)`/`(teacher)`/`(public)`/`(auth)` nhưng **KHÔNG có** login→role-based-redirect, **KHÔNG có** role-guard component (grep `RoleGuard`/`useRole`/`hasRole` → 0 hit), **KHÔNG có** cross-product handoff KH `:3001` → KC `:3000` cho owner/staff.
- → mọi role login gần như thấy cùng 1 shell; route KHÔNG bị chặn theo role (rủi ro: bất kỳ user login nào với tới route bất kỳ — IDOR-by-navigation risk).

## 5. Verification Gates

Theo GAP-1119 AC:
- [ ] KC login mint token → redirect đúng role-home; role-guard chặn route ngoài quyền
- [ ] 4 role-shell (owner/staff/teacher/parent) có nav + dashboard home riêng; student-shell scaffold + gated KC-9
- [ ] Cross-product SSO: owner/staff login KH `:3001` → vào KC `:3000` school-mgmt không re-login
- [ ] KC owner-shell có màn assign user→role (5 template seeded); KHÔNG expose permission-edit UI
- [ ] Doc invite split STAFF(KH)/TEACHER(KC) trong business doc + architecture
- [ ] LMS surfaces (Wave LMS-FE 1) cắm đúng shell: authoring→teacher, player→student

Mỗi shell + SSO: pre-walk persona simulation → G1 browser-walk per `g1-browser-walk-before-flip.md` → feature-ship runtime walk trước DONE.

## 6. Agent Spawn Pattern

Worktree-parallel + Opus per `agent-background-spawn-default.md` + `agent-model-opus-default.md`. Spawn order:

```
Bucket 0 (pre-walk simulation)
   ↓
Bucket A (login role-redirect + role-guard, foundation)
   ↓
Bucket B (4 shell) ∥ Bucket C (cross-product SSO, risk-isolated) ∥ Bucket D (RBAC-assign UI) ∥ Bucket E (invite split doc)
```
Student-shell = scaffold only (Increment B chờ KC-9). Bucket E độc lập (docs), ship parallel bất kỳ lúc nào.

## 7. Closure Protocol

Draft — fill tại closure. Per `wave-closure-scope-completeness.md` (reconciliation table §3) + `post-wave-cleanup.md` (prune worktrees + merged branches) + `post-merge-sync-completeness.md` (CSV / ROADMAP / wave-history / memory sync).

### Risk
- **Cross-product SSO (Risk #1 HIGH):** Bucket C cần design token handoff trước impl. Nếu SSO complexity > 1 bucket → split sang dedicated wave; Bucket B owner/staff shell tạm dùng KC-native fallback login (nếu khả thi) cho beta.
- **role-name parity drift:** verify BE seed vs FE guard literal (Risk #3) — reconcile trong Bucket A.
- **IDOR-by-navigation:** hiện route không chặn role → Bucket A role-guard = security fix, không chỉ UX (P1).

## 8. Log
- **2026-06-09:** Draft enrich từ session 2026-06-10 investigation (GAP-1119 + auth-split verify) — thêm BE/FE state verify, 3 Risk (cross-product SSO / KC-9 / role-name parity), Bucket 0 pre-walk, per-role login-location table, pointer tới invite redesign discussion doc, scope-completeness placeholder. EXTEND draft gốc (PR #2283 branch `feature/gap-1119-kc-role-shell`). Restructure sang 8 canonical section (per `_TEMPLATE.md`) khi ship qua PR #2287.
