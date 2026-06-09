---
title: Wave RBAC-Shell 1 — KiteClass role-based login routing + per-role dashboard shell
status: draft
created: 2026-06-10
updated: 2026-06-10
tag_primary: rbac-shell
gaps: [GAP-1119]
---

# Wave RBAC-Shell 1 — KC role-based login routing + per-role shell

## TL;DR
Tầng **nền FE role-shell** cho KiteClass — foundation phải xong TRƯỚC/CÙNG Wave LMS-FE 1. Đóng **GAP-1119**. 4 role-shell buildable ngay (owner/staff/teacher/parent); student-shell scaffold + gated **KC-9** (student-auth pending).

## Brainstorm
- **Outside-in:** ĐÃ audit (persona lens trong `2026-06-10-pre-wave-lms-fe-outside-in.md` flagged owner/parent/admin scope). Decisions chốt GAP-1119 (2026-06-10): RBAC fixed-curated + cross-product SSO KH→KC + route quản-quyền ở KC.
- **Deps:** auth split KH/KC đã có (TEACHER/PARENT/STUDENT KC-native Option B; OWNER/STAFF KH `/api/v1/auth/**`). RBAC BE dynamic-capable (`role/{Role,Permission}` + role-hierarchy). Student-shell chặn bởi KC-9.
- **Risk chính:** cross-product SSO KH `:3001` → KC `:3000` (token handoff qua shared gateway) — phần khó nhất, cần design token-share/SSO-redirect.

## Buckets (worktree-parallel, Opus)
| Bucket | Scope | Dep |
|---|---|---|
| A — Login→role-redirect + role-guard | KC login đọc JWT `role` claim → redirect role-home; `RoleGuard` HOC/middleware chặn route group theo role (teacher không vào owner route...) | none |
| B — Per-role nav + dashboard home | 4 shell owner/staff/teacher/parent (nav + home theo bảng GAP-1119); student-shell scaffold gated | A |
| C — Cross-product SSO KH→KC | owner/staff login KH `:3001` → handoff token sang KC `:3000` school-mgmt (gateway shared JWT / SSO redirect) | A |
| D — RBAC management UI (fixed-curated) | KC owner-shell: assign user→role (5 template seeded `RoleSeederService`); KHÔNG dựng edit-permission-per-role UI | A |
| E — Doc invite split + business sync | doc hoá STAFF(KH StaffInvitation)/TEACHER(KC admin-provision) split; sync role-hierarchy + tenant-auth + architecture | none |

## Sequencing
A (foundation) → B/C/D song song → E song song. Student-shell = scaffold only (Increment B chờ KC-9).

## Acceptance
Theo GAP-1119 AC: login role-redirect + role-guard mọi route + 4 shell + cross-product SSO + RBAC-assign UI (no permission-edit) + invite split doc + (LMS surfaces Wave LMS-FE cắm đúng shell).
