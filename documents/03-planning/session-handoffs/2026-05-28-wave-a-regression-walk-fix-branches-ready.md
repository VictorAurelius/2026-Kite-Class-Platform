---
audience: dev
date: 2026-05-28
session-theme: Wave A full regression RST walk + fix prep — 4 fix branches ready, GAP-798 V2 bridge = remaining "fix triệt để"
status: complete
next-session-focus: Re-walk + merge 3 fix branches → GAP-798 authz V2 bridge (the triệt-để piece) → seed 1 center → flip gaps DONE
context-at-handoff: 77% (handoff để re-walk migration + authz V2 chạy trong context sạch — critical work)
---

# Session handoff — Wave A regression walk + fix branches ready (2026-05-28)

## Đã ship session này (trên main)

| PR | Nội dung |
|---|---|
| #1941 | Full 126-row RST walk findings (INDEX + 3 cluster docs) + GAP-795/796/797 filed + audits-index rows |
| #1942 | §2.8 misdiagnosis correction banner (owner-ops + teacher-parent-student) |
| #1943 | Dev-readability polish — body align §2.8 correction (~11 spots marked, audit trail preserved) + English-prose → Vietnamese + `→ Fix: GAP-NNN` pointers |

**Docs wave-a-regression = DONE + dev-suitable trên main.** Entry point: `documents/04-quality/audits/rst-html/2026-05-28-full-regression/INDEX.md`.

## Walk verdict (gateway tenant isolation = MISDIAGNOSIS, đã sửa)
RST agent ban đầu báo "P0 tenant isolation vỡ" — SAI. §2.8 fix-time investigation verify empirical: gateway resolve JWT tenant + core set TenantContext + `instance_id` tagged + Hibernate filter active → **isolation WORKS** (shared-DB+filter; `kiteclass_877dff9d` legacy; MDC `tenant=-` red herring). GAP-795 re-scoped P0→P1 (X-User-Id UUID). **Bài học:** §2.8 fix-time state-check cứu việc fix non-bug.

## 4 fix branch READY (pushed remote, CHƯA merge — cần RST re-walk trước)

| Gap | Branch (remote) | Commit | Scope | Trạng thái |
|---|---|---|---|---|
| GAP-797 | `worktree-agent-aed0d9e4ae7f39ae0` | `05a0c978` | email template var-name reconcile (claimCode/inviteUrl/expiresAt) + welcome + regression test; 84 tests PASS | ✅ ready |
| GAP-796 | `worktree-agent-a62d4303b3533080f` | `fe0b524` | kiteclass-core 404/405 handlers (NoHandlerFound→404, MethodNotSupported→405) + config; +2 handlers (10→12, no deletion verified); 9/9 tests | ✅ ready |
| GAP-795 | `worktree-agent-a9489709d3f7b09ae` | `1fdcb439` | UserContext+BaseEntity+JpaConfig+TenantFilterInterceptor Long→UUID + V73 migration (~30 bảng, USING NULL::uuid safe) + sweep 34 files; 81 tests PASS | ⚠️ core done, authz PARTIAL → GAP-798 |
| GAP-787 | (no branch — already wired main) | — | staff-invite email đã wired (StaffInvitationController→sendInviteStaffEmail, 65 tests). Stale gap | ✅ DONE candidate |

GAP-792 = ✅ already OK (CourseService/TeacherService đã tenant-scoped key — no fix).

## "Fix triệt để" còn lại

### GAP-798 (P1) — authz V2 bridge = THE remaining triệt-để piece
GAP-795 fix audit chain xong, NHƯNG authz ownership PARTIAL fail-closed: parents/teachers/students có numeric PK, KHÔNG có `user_id` UUID link → `hasAccessToChild` + UserPreferences + 4 controller (Storage/Assignment/LessonProgress/Lms) không match actor-UUID. **Fix V2:** add `user_id UUID` FK + populate at invite-accept + authz UUID compare. **Security-sensitive → investigate invite-accept link point TRƯỚC khi implement** (per `release-fix-retry-budget.md` §3.5). Đừng spawn agent blind. Chi tiết: `documents/04-quality/gaps/phase-1-beta/GAP-798-*.md`.

### Teacher-invite-email (kiteclass-core /teachers) — finding chưa file gap
Walk OWNER-TEACHER-004: tạo teacher qua `/api/v1/teachers` → không có invite email MailHog. KHÁC GAP-787 (staff-invitation, kitehub-subscription, đã wired). **Scope-check: teacher-create có nên gửi invite email by design không?** Nếu có → file gap mới. Nếu Phase 1 BETA không cần → note.

## NEXT SESSION — thứ tự execute (context sạch)

1. **Re-walk + merge 3 fix branch** (per `feature-ship-runtime-walk-mandate.md` §3 + `admin-merge-discipline.md`):
   - Rebuild: `bash kitehub/scripts/rebuild.sh email` + `subscription` (GAP-797) + kiteclass-core (GAP-795+796). Stack local 13 service (check `bash kitehub/scripts/status.sh`).
   - **RST re-walk live** (NOT just merge):
     - GAP-797: trigger beta-approve → MailHog email có **mã 6 số THẬT** + link `/signup/beta?code=` (không `------`/`/beta/accept`)
     - GAP-796: `curl` route không tồn tại → 404 (không 500); sai method → 405
     - GAP-795: tạo teacher/course qua gateway → DB `created_by` = Owner UUID (không NULL); kiteclass-core log không còn `Invalid X-User-Id`
     - GAP-787: trigger staff-invite → MailHog có invite-staff email
   - Merge từng branch (local verify per `admin-merge-discipline.md`, NO --admin blind). Cred: Owner `owner.test@test.vn`/`Test@1234` tenant `877dff9d`; Admin `admin@kitehub.com`/`Admin@KiteHub123`. Login `/api/auth/login` (KHÔNG /api/v1/). Gateway :9000, MailHog :8025.
2. **GAP-798 V2 bridge** — investigate invite-accept → design user_id UUID bridge → implement → authz IT → re-walk parent flow (fail-closed → fail-correct).
3. **Seed script 1 trung tâm** (user direction Q2/Q3 2026-05-28): English-teaching center ("Sky English Center" IELTS/TOEIC), VN labels (per `vn-localization-audit-checklist`: Trần Thị Hồng, VND, niên khóa), stock images CC0 (Unsplash/Pexels, ghi nguồn) via storage presigned API hoặc URL asset. **Reproducible seed SCRIPT** (không manual DB insert). **Triple duty:** thesis Ch4 minh chứng + manual GAP-537 25% screenshots + RST re-walk fixture (unblock 18 NEEDS-DATA T/P/S). Làm SAU khi GAP-795 merged (re-walk + screenshot trên UI đã fix).
4. **Flip gaps DONE** (chỉ khi re-walk pass): GAP-795/796/797/787 + sync 4-target (gap-status.csv + ROADMAP + wave-history + handoff).
5. **Manual GAP-537** (PARTIAL 75%, HTML đã tích hợp /help routes, last-updated 2026-05-14): 25% còn lại = screenshots — feed từ seed script step 3.

## Worktree cleanup
Husks accumulating (`.claude/worktrees/agent-*`). Sau khi 3 fix branch merged → `bash scripts/prune-merged-worktrees.sh --dry-run` rồi prune. Branches đã push remote nên prune worktree an toàn.

## Context discipline note
Handoff tại 77% vì re-walk migration 34-file + authz V2 = critical work (`session-orchestration`: không chạy degraded). Fresh session = clean context cho migration verify + authz security.
