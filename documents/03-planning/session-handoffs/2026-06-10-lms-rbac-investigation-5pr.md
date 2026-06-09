---
title: Session Handoff — LMS investigation → 5 PR (RLS đính chính + F1 paywall + role-shell + 2 wave draft + META)
audience: dev
created: 2026-06-10
scope: discuss-only session (song song 1 working session khác) — investigation LMS KiteClass → fix + plan
push_status: 5 PR open trên remote (KHÔNG merged)
---

# Session Handoff 2026-06-10 — LMS / RBAC investigation

## TL;DR
Discuss-only session (có 1 working session khác chạy song song cùng branch). Investigate LMS KiteClass: từ giả định "BE đủ chỉ cần dựng FE" → **sai theo 2 chiều**. Outside-in audit 3-lens phát hiện **1 P0 paywall thật** + **role-shell foundation thiếu**. Ship 5 PR + 2 wave draft.

## Đính chính quan trọng (tôi đã sai)
- **RLS 4 bảng LMS KHÔNG thiếu** (tôi nói thiếu — sai). V79 dòng 577-613 `DO $$` đã áp ENABLE+FORCE+policy `tenant_isolation` hardened V59 cho cả 4 bảng. Agent verify-before-fix bắt được. → GAP-1112 reframe = test-guard (không phải security). Doc 09-lms đã sửa.
- **"BE đủ" quá lạc quan** — BE LMS có **F1 CRITICAL paywall bypass** (`getCourseStructureForStudent` rò content+videoUrl bài paid) + F2/F3/F10 + thiếu endpoint (course-list/publish/upload/owner-parent-scope).

## 5 PR (open, chưa merge)
| PR | Gap | Nội dung |
|---|---|---|
| #2280 | 1111/1113 | docs cluster `09-lms` (RLS đính chính) + GAP-1113 FE-defer |
| #2281 | 1112 | RLS test-guard `LmsRlsIsolationIT` (P2) |
| #2282 | 1114 | META rule `multi-session-concurrency-coordination` (UTC-edge CI note) |
| #2283 | 1119 | role-shell foundation gap + 2 wave draft + handoff này |
| #2284 | 1115-1118 | **F1 BE-fix** (P0 paywall + enrollment + 500 + tenant-leak) — 46/46 test PASS, **PARTIAL** chờ walk |

Audit report 3-lens: `documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (trong #2284).

## Quyết định chốt (2026-06-10)
- Auth split KH/KC: OWNER/STAFF → KH `:3001`; TEACHER/PARENT/STUDENT → KC `:3000`. Teacher chỉ KC.
- RBAC **fixed-curated** beta (5 template, owner gán user→role; BE giữ dynamic; defer edit-permission UI Phase 3).
- Owner/Staff vào KC qua **cross-product SSO KH→KC**. Route quản-quyền ở **KC owner-shell**.
- Invite **STAFF(KH)/TEACHER(KC)** giữ split.
- **2 wave tuần tự:** `wave-rbac-shell-1` (GAP-1119, foundation) → `wave-lms-fe-1` (GAP-1113, lean MVP).

## Pickup (next session / working session)
1. ⚠️ **Runtime-walk F1** (GAP-1115 P0: non-enrolled student → structure content=null; enrolled → full) trên stack production-equivalent TRƯỚC khi flip DONE. 4 gap PARTIAL.
2. **Merge 5 PR:** đều đụng `gap-status.csv` + branch session kia → resolve **additive** (per rule GAP-1114). Session kia cũng đã filed GAP-1111/1112 riêng → ID collision thật (live test của META rule).
3. **KC-9 student-auth** = blocker student-shell + LMS Increment B.
4. Execute 2 wave theo dependency: RBAC-Shell → LMS-FE.

## Lưu ý multi-session
2 session độc lập cùng branch → collision gap-ID/migration/CSV (đã catch → rule GAP-1114 + memory `feedback_multi_session_concurrency`). Block ID dùng session này: 1111-1119 (my) ; session kia: 1111-1112 (theirs, khác content).
