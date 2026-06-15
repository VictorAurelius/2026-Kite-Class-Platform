---
title: Session Handoff — RBAC G2 walk + hardcode/mock state-check + fix-wave
date: 2026-06-15
scope: Flow Verification (RBAC G2) + hardcode/mock state-check audit + partial fix-wave
context_at_close: 85% (1M Opus)
---

# Session Handoff 2026-06-15

## Scope shipped this session

1. **Deploy-parity (MERGED)** — PR #2437 → main `1ec8a3640`: GAP-1407 banner-renderer deploy-ready (CI build-push + ecr.tf + kc.yml co-locate kc-app), GAP-1408 ADR-032 kiteclass-gateway cleanup (~19 files, grep 0 active ref), GAP-1409 dev-stack residual.
2. **RBAC G2 walk — PASS** (human-verified 3-role login OWNER→/dashboard, TEACHER→/teacher, STUDENT→/student). Walk-found fixes committed (PR #2439).
3. **Hardcode + mock-in-production state-check** (PR #2438): audit `documents/04-quality/audits/2026-06-15-hardcode-mock-state-check.md` + GAP-1410 umbrella + GAP-1411..1415. **MOCK vs HARDCODE** as primary axis (per dev directive).
4. **Fix-wave (partial):** P0 GAP-1413 + GAP-1416 + GAP-1411 done; 3 rate-limited (burst-6 → throttle; spawn 1-2/lượt next).

## Open PRs (DO NOT merge with failing/pending CI)

| PR | Branch | Content | CI |
|---|---|---|---|
| **#2438** | `feature/hardcode-mock-state-check-2026-06-15` | docs: audit + GAP-1410 umbrella + 1411-1415 | audits-index fix pushed → confirm green → merge (docs-only) |
| **#2439** | `feature/rbac-g2-walk-fixes-2026-06-15` | code: RBAC G2 (loginClient host-preserve GAP-1416 + role-display + teacher-logout) + P0 GAP-1413 nil-UUID resolver (9/9 test). **Email GAP-1414 REVERTED** (see Known issues) | gap-folder + BE→FE fixed; Java/FE CI re-running → confirm green → merge |

## Pickup (first task — in order)

1. **#2439** → confirm Java/FE CI green → merge (squash). RBAC G2 + P0 multi-tenant fix land on main.
2. **#2438** → merge when green (docs-only auto-merge).
3. **Email GAP-1414 re-PR:** reverted from #2439 because it surfaced **13 pre-existing email→404 routes** (`/unsubscribe`, `/support`, `/help/onboarding`, `/admin/dsar`, `/legal/data-rights/status` — EmailServiceClient links to non-existent FE routes). Email domain-config work preserved in commit `d61596c3d`. **File NEW gap** "email links → 13 missing FE routes" + re-apply email fix + add/fix routes in one PR.
4. **Parent fix GAP-1411 consolidate:** committed `227bd69fa` in worktree `.claude/worktrees/agent-a3116513879b3ce0f` (real KC-8 facet wiring: attendance/billing WIRED, dashboard/grades/payment PARTIAL+TODO no-fabricated-data, parent-shell logout, `pnpm build` pass). Rebase onto main → PR. (Worktree NOT pushed.)
5. **Re-run CONTROLLED (1-2 agents/lượt, NOT burst):** GAP-1415+965 (BE grade/invoice constants→config + enum i18n), GAP-1412 (student portal mock→real facet + student-shell logout), GAP-268 (teacher portal mock→real identity/API). All rate-limited this session.
6. **RBAC G2 close:** browser-confirm teacher-logout live (rebuilt) → flip campaign RBAC row.

## Known issues / decisions

- **Email GAP-1414 reverted** from #2439 (BE→FE detector hard-fail on 13 pre-existing email→404 routes surfaced by domain-config refactor). GAP-1414 stays OPEN. Re-PR with routes fixed.
- **Burst-spawn lesson:** spawned 6 Opus agents at once → Anthropic rate-limit (3 failed: GAP-1415/1412/268). Per `agent-concurrency-budget-inline-hybrid` — spawn 1-2/lượt. (Note: B-email + FE-1-parent recovered + finished despite initial throttle.)
- **AWS posture:** EC2 stopped, RDS stopped (cost). nip.io subdomain walk used for G2 (`sky-education-074901.127.0.0.1.nip.io:3000`).
- Lingering in-repo worktrees `.claude/worktrees/agent-a3116*` (parent fix — KEEP until consolidated) + possibly empty locked ones (harmless).

## Background / survives /clear

- Docker stack UP + healthy (15 containers, rebuilt latest main this session). kiteclass-frontend rebuilt with RBAC fixes (loginClient/role-display/teacher-logout live).
- Seeded demo tenant `sky-education-074901` (owner `owner+074901@skyedu.vn`/`SkyEdu@2026`, teacher `an.nguyen+074901@skyedu.vn`/`Teacher@2026`, student `mai.pham+074901@gmail.com`/`Student@2026`). Admin password reset to `Admin@KiteHub123` (was unrecoverable from secrets).

## Anchors
GAP-1410 umbrella (hardcode/mock) + audit artifact (on main when #2438 merges). `gap-status.csv` has 1407-1416 across merged main + open PRs.
