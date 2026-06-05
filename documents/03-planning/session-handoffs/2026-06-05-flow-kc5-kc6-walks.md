---
title: Session Handoff — KC-5 attendance + KC-6 grade G1 walks
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign KC-5 + KC-6
---

# Session Handoff 2026-06-05 — KC-5 + KC-6 G1 walks

## Shipped (PR #2180 — wave/flow-kc5-attendance → main)

3 commits: KC-5 (058e50d9) + KC-6 (804d540d) + docs-governance fix (3cfaefea). Rebased onto current main after KC-4 landed separately via #2179.

**KC-5 attendance — G1 PASS:** GAP-991 (single-mark authz OWASP A01) + GAP-992 (session-status guard) + GAP-993 (EXCUSED-requires-note) + GAP-994 (rate=(PRESENT+LATE)/total) + GAP-995 (enum docs) + **GAP-996 P0** (V87 — attendance schema drift student_id NOT NULL + lowercase status CHECK + stale unique, blocked ALL writes). GAP-997 P3 OPEN (no-tenant defense-in-depth). All DONE → closed/.

**KC-6 grade — G1 PASS:** **GAP-998 P0** (V88 — grading_scales EMPTY + legacy NOT-NULL drift, supersedes GAP-875 scaffold-close; seed per-tenant) + GAP-999 (grade authz OWASP A01, 11 endpoints + 2 helpers). OPEN follow-ups: GAP-1000 P1 (finalize teacherId spoof + ADMIN-blocked), GAP-1001 P2 (transcript semester/credit/studentName), GAP-1002 P1 (NULL-default unreachable + new-tenant provisioning).

Migrations: **V87** (attendance) + **V88** (grading_scales per-tenant seed). Both applied live (Flyway success).

## ⚠️ OPEN OBLIGATION — merge PR #2180 when CI green

User directed "merge when green". At session end: **0 failures, but 11 checks still QUEUED** (self-hosted runner backlog ~1hr). Test Core Service PASSED. Trivy NEUTRAL.

**NEXT SESSION FIRST ACTION:**
```
gh pr view 2180 --json statusCheckRollup --jq '[.statusCheckRollup[]|select((.status//"COMPLETED")!="COMPLETED")]|length'  # 0 = all done
gh pr view 2180 --json statusCheckRollup --jq '.statusCheckRollup[]|select((.conclusion//"")!="SUCCESS" and (.conclusion//"")!="SKIPPED")|"\(.name)\t\(.conclusion//.status)"'  # non-green
```
If all green → `gh pr merge 2180 --squash --delete-branch` (NO --admin per admin-merge-discipline). If any FAIL → investigate (expected pass: V87/V88 nullable-ALTER + seed-empty-on-fresh-CI; entity-drift IT unaffected).

## Key cross-cutting lesson (in MEMORY.md)

kiteclass-core IT use `ddl-auto=create-drop` (Flyway off) → schema from entities → **blind to schema↔entity drift**. Caught the SAME P0 class in BOTH KC-5 (attendance) and KC-6 (grade) only via production-equivalent walk. **KC-7 invoice likely same** — walk real Flyway schema, don't trust green IT.

## Next steps

1. Merge PR #2180 (when CI green).
2. **KC-7 Invoice → payment → reconcile** (⬜ next flow) — noted 🔴 GAP-882 (CHECK) + GAP-879 + schema-drift risk. Same pattern: pre-walk Opus persona-sim (schema-drift check) → walk.
3. G2 human tests for KC-5/KC-6 (recipes shipped: `2026-06-05-g2-recipe-kc5-attendance.md` + `-kc6-grade.md`).
4. Campaign §4: KC-1..KC-6 all 🔄 walk-pass-pending-human; KC-7/8/9 ⬜.

## Walk fixtures left in dev DB (sky tenant 0edaee10)

enrollment 32 ACTIVE; teacher_classes (3,14,MAIN_TEACHER); attendance session 5 COMPLETED; grade id=25 finalized B+; grading_scales seeded per-tenant. (G2 re-walks via UI.)

_Note: handoff untracked at session end to avoid re-triggering PR #2180 in-flight CI; commit on a later branch or next session if desired._
