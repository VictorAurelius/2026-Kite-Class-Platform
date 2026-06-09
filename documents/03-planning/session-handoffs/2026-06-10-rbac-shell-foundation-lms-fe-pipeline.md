---
audience: dev
created: 2026-06-10
session: RBAC-Shell foundation + LMS-FE pipeline kickoff
---

# Session Handoff — 2026-06-10 — RBAC-Shell foundation shipped + remaining pipeline

**Scope:** Execute 2 plans `wave-rbac-shell-1.md` (phân quyền, FOUNDATION) + `wave-lms-fe-1.md` (LMS FE). Worktrees off `origin/main`.

## 1. Đã ship session này — RBAC-Shell Phase 1 foundation (3 bucket → 2 PR)

| Bucket | Branch | Gap | Trạng thái |
|---|---|---|---|
| **0 Pre-walk** | folded into E | — | ✅ persona sim artifact (5-6 failure mode/role) |
| **A Foundation** | `wave/rbac-shell-1-a-roleguard` (`c0ec58d7`) | **GAP-1122** | ✅ login→role-redirect + `RoleGuard` + `normalizeRole` parity adapter; IDOR-by-nav fix (`(dashboard)/admin/*`); **20 FE test + `next build` PASS**; **PARTIAL** (chờ G1+runtime walk) |
| **E Invite docs** | `wave/rbac-shell-1-e-invite-docs` (`d9c9c313`) | GAP-1123/1124/1125 | ✅ invite-split docs + role-hierarchy 5-role beta + Bucket F + invite decisions |

**PRs (base main, CHƯA merge):**
- **#2290** — Bucket A code. CI chạy, **cần review** (foundation+security).
- **#2291** — Bucket 0+E docs + plan Bucket F + invite Q-A..Q-D decisions. Docs-only, auto-merge khi CI green.

## 2. GAP-ID allocation (collision resolved)

origin/main đã chiếm **GAP-1121** (RLS-parity) → reserve-block của session (chạy trên branch cũ hơn) bị lệch.
- GAP-1122 = Bucket A foundation
- GAP-1123 = invite-split doc reconcile (PARTIAL) · GAP-1124 = TEACHER email-invite (OPEN) · GAP-1125 = bulk-invite (OPEN)
- **Block free còn lại: 1126-1132** (1126 = Bucket F BE @PreAuthorize khi spawn)

## 3. Decisions chốt (durable trong repo)

- **Bucket F** (BE `@PreAuthorize` role-literal alignment) = tích hợp vào `wave-rbac-shell-1.md` §2/§6 (Phase 2 ∥ B/C/D, dep A), KHÔNG file gap standalone.
- **Invite-redesign scope LOCKED** (per user Q-A..Q-D, ghi `invite-flow-redesign-discussion-2026-06-09.md` §9 + GAP-1124/1125):
  - Q-C: **Option 1** giữ split KH/KC + KC teacher-invite + bulk
  - Q-A: MANAGER **defer Phase 2** (BR-ROLE-005); Phase 1 chỉ STAFF(KH)+TEACHER(KC)
  - Q-B: bulk **cả** textarea(≤10) + CSV/XLSX (reuse `BulkImportController`)
  - Q-D: **(a)** annotate KC staff-invitation doc = planned + build TEACHER subset

## 4. Pipeline CÒN LẠI (next sessions — context-fresh recommended)

```
[NOW] PR #2290 review+merge  +  PR #2291 auto-merge (docs)
   ↓
PHASE 2 — RBAC-Shell (sau A merge):
   Bucket B (4 role-shell owner/staff/teacher/parent; student scaffold gated KC-9)
   ∥ Bucket C (cross-product SSO KH:3001 → KC:3000 — Risk #1 HIGH, design token-handoff trước impl)
   ∥ Bucket D (RBAC-assign UI fixed-curated, KC owner-shell)
   ∥ Bucket F (BE @PreAuthorize role-literal alignment, gap 1126)
   ↓
PHASE 3 — LMS-FE (wave-lms-fe-1.md; dep RBAC teacher-shell + F1 PR #2284 ✅ merged):
   Phase0-BE gap-fill (course list/publish/reorder/upload/roster + api-contract)
   → Increment A: Bucket A teacher-authoring ∥ B guest-paywall ∥ D assignment
   → Increment B: Bucket C student-player  ← GATED KC-9 student-auth
   ↓
INVITE-REDESIGN wave (GAP-1124 teacher-invite + GAP-1125 bulk) — scope locked §3; chạy sau RBAC SSO Bucket C
```

## 5. Blockers / cảnh báo cho session sau

- **GAP-1122 chưa DONE** — cần dựng Docker stack → G1 browser-walk + feature-ship runtime walk (`pre-walk-rbac-shell-login.md` đã có failure-mode để walk theo).
- **Bucket C SSO** = phần khó nhất (token handoff KH→KC qua shared gateway); nếu > 1 bucket → tách wave riêng, owner/staff dùng KC-native fallback tạm.
- **KC-9 student-auth** chưa ship → student-shell (RBAC B) + LMS student-player (LMS C) chỉ scaffold.
- **Parked:** main tree branch `wave/branding-fix-2026-06-10` có dirty FULL_AI work (AI tier-limit GAP-1119-collision, `ai-tier-limits.reserved`) — KHÔNG đụng, để session branding tự xử.
- **Parallel session** `session-20260610-031148` on `feature/tier-ui-fix-g2-browser` (PR #2279 branding G2) — no overlap với LMS/RBAC.
- **Lesson:** Bucket A bg-agent tưởng chết (155-byte output + stale mtime) nhưng vẫn sống (988s) — per `feedback_parallel_agent_not_dead_detection`, check transcript mtime + agent lifecycle trước khi kết luận/redo.

## 6. Pickup recipe (session sau)

1. `/start-session` → đọc lock `session-20260610-034209` + handoff này.
2. Review/merge PR #2290 (+#2291 nếu chưa auto-merge).
3. Dựng stack (`bash kitehub/scripts/up.sh ...`) → G1+runtime walk GAP-1122 → flip DONE.
4. Spawn Phase 2 (B/C/D/F) Opus agents, worktree off `origin/main` rebased trên A merge. Reserve gap-block trước (1126+).
