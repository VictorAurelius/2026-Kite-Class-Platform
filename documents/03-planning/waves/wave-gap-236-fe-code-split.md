---
title: Wave GAP-236 — FE code-splitting completion (4 parallel agents)
status: planned
created: 2026-04-28
updated: 2026-04-28
gaps: [GAP-236]
parent_audit: documents/04-quality/audits/performance/performance-audit-2026-04-19.md
parent_gap: GAP-127
---

# Wave GAP-236 — FE code-splitting completion

**Status:** 🔵 PLANNED
**Trigger:** GAP-236 Sub-PR B — 44+ pages still using static imports for heavy children. Sub-PR A (CI bundle budget) shipped Wave P2-Cleanup; Sub-PR C (analyzer HTML baseline) deferred.
**Strategy:** 4 parallel `isolation: worktree` agents; lead consolidates ROADMAP + GAP-236 status post-merge.

---

## Wave-eligibility verification (Step 0)

| Q | Answer |
|---|--------|
| ≥3 sub-tasks? | ✅ YES — 4 page-bucket partitions |
| Disjoint files? | ✅ YES — each agent owns exclusive `(group)/` directories; no overlap |
| Self-contained build? | ✅ YES — each agent runs `pnpm build` + bundle-budget check on own app |

→ Wave-eligible. Spawn 4 agents.

---

## Agent assignments

### Agent A — KiteClass auth + public pages
**Branch:** `feature/wave-gap-236-A-kc-auth-public`
**Files (exclusive):**
- `kiteclass/kiteclass-frontend/src/app/(auth)/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/app/(public)/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/components/auth/**` (only if creating new lazy wrappers)
- `kiteclass/kiteclass-frontend/src/components/public/**` (only if creating new lazy wrappers)
**Out-of-bounds:** `(dashboard)/**`, `dashboard/**`, any KiteHub file, `next.config.js`, `package.json`
**Page count:** 11 (login, register, register/student, forgot-password, reset-password, parent-invite/[token], home, about, catalog, catalog/[id], contact)
**Acceptance:**
- ≥6 pages converted to use `next/dynamic` for heavy children (forms, content sections)
- `pnpm build` green; `scripts/check-bundle-budget.mjs` green (no route >250KB First Load JS)
- No regression on existing tests (`pnpm test`)
- Heavy candidates: `/about` (434 lines), `/contact` (196 lines react-hook-form), `/catalog/*`, `/(auth)/login` + `/(auth)/register*` (react-hook-form)

### Agent B — KiteClass admin + attendance + billing
**Branch:** `feature/wave-gap-236-B-kc-admin-attendance-billing`
**Files (exclusive):**
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/admin/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/attendance/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/billing/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/components/attendance/**` (only if creating new lazy wrappers)
- `kiteclass/kiteclass-frontend/src/components/billing/**` (only if creating new lazy wrappers)
**Out-of-bounds:** `(auth)/**`, `(public)/**`, `(dashboard)/{classes,courses,students,teachers,branding,parent,settings}/**`, any KiteHub file
**Page count:** 8 (admin/attendance/stats, attendance, attendance/reports, billing, billing/[id], billing/[id]/pay, plus root admin pages)
**Acceptance:**
- ≥5 pages converted; `/attendance/reports` (417 lines, react-day-picker) MUST be one of them
- `pnpm build` + bundle-budget green; `pnpm test` green
- Heavy candidates: `/attendance/reports`, `/billing/*` (date-pickers), `/admin/*`

### Agent C — KiteClass classes + courses + students + teachers
**Branch:** `feature/wave-gap-236-C-kc-classes-courses-students-teachers`
**Files (exclusive):**
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/classes/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/courses/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/students/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/teachers/**/*.tsx`
- `kiteclass/kiteclass-frontend/src/components/{class,course,student,teacher}/**` (only if creating lazy wrappers)
**Out-of-bounds:** `(auth)/**`, `(public)/**`, `(dashboard)/{admin,attendance,billing,branding,parent,settings}/**`, any KiteHub file
**Page count:** 17 (classes/{,[id],[id]/attendance,[id]/edit}, courses/{,[id],[id]/classes/new,[id]/edit,new}, students/{,[id],[id]/attendance,[id]/edit,new}, teachers/{,[id],[id]/edit,new})
**Acceptance:**
- ≥10 pages converted (this is the largest bucket; aim for breadth)
- Form-heavy pages (`/*/new`, `/*/[id]/edit`) get react-hook-form deep trees lazy-loaded
- Detail pages (`/*/[id]`) get heavy detail panels lazy-loaded
- `pnpm build` + bundle-budget green; `pnpm test` green

### Agent D — KiteHub all routes + Sub-PR C analyzer baseline
**Branch:** `feature/wave-gap-236-D-kh-all-and-analyzer-baseline`
**Files (exclusive):**
- `kitehub/kitehub-frontend/src/app/**/*.tsx` (all groups: admin/auth/customer/public)
- `kitehub/kitehub-frontend/src/components/**` (only if creating new lazy wrappers)
- `documents/04-quality/audits/performance/bundle-analyzer-baseline-{kc,kh}.html` (NEW — Sub-PR C)
**Out-of-bounds:** any kiteclass-frontend file, any backend file, `next.config.js`, `package.json`, `bundle-budget.json`
**Page count:** 24 (5 admin + 3 auth + 11 customer + 5 public)
**Acceptance:**
- ≥9 KH pages converted (already has 1 from Wave 7-Perf landing)
- `/(admin)/admin/instances/[id]` (452 lines) MUST be converted — branding wizard integration
- `/(customer)/dashboard` + `/(customer)/branding/*` get heavy children lazy-loaded
- Sub-PR C: `ANALYZE=true pnpm build` for both apps (KC + KH); commit resulting HTML reports to `documents/04-quality/audits/performance/`
- `pnpm build` + bundle-budget green; `pnpm test` green

---

## Hard rules (per `feedback_parallel_agent_strategy.md`)

1. **No agent touches** `ROADMAP.md`, `MEMORY.md`, `output-review-mandate.md`, `next.config.js`, `package.json`, `bundle-budget.json`, or any `application.yml` — parent consolidates post-merge.
2. **GAP file updates** — each agent updates ONLY their own AC checkbox in `GAP-236-fe-code-splitting-completion.md` Sub-PR B section + adds Log entry. No other gap file touched.
3. **Worktree path** — agents work in `/tmp/claude-worktree-<agent>` or `~/.claude/worktrees/`; do NOT write to main repo working copy.
4. **No new gaps filed by agents** — if agent finds out-of-scope issues (e.g. missing analyzer HTML, dependency upgrade), return them in agent summary; parent files follow-up gaps.
5. **Per-route bundle budget** — if a converted page now exceeds 250KB, agent must add per-route override in `bundle-budget.json` ONLY via parent (rule 1) — instead, agent should pick a different lazy boundary.
6. **Test-profile escape hatch** — N/A (FE only; no Spring profile concerns).

---

## Consolidation (parent post-merge)

After all 4 PRs merged:
1. Update `ROADMAP.md` Current Status Snapshot with wave-GAP-236 entry
2. Mark GAP-236 as 🟢 DONE if ≥30 pages total converted across all 4 agents (current AC threshold)
3. Update GAP-127 status reference if appropriate (parent gap)
4. Memory entry: `feedback_wave_gap_236_retro.md` if any new lessons surface (e.g. lazy-boundary pitfalls)
5. Re-baseline bundle budget after wave to capture wins

---

## Risk mitigation

| Risk | Mitigation |
|------|------------|
| Lazy-loading breaks SSR for SEO-critical pages (`/about`, `/contact`, catalog) | Use `dynamic(..., { ssr: true })` for content-heavy SSR pages; only `ssr: false` for client-interactive sub-trees |
| Form state lost across lazy boundary | Lift form provider above the lazy boundary; only lazy-load presentational sub-trees |
| Bundle budget regression on a converted page | Agent re-runs `pnpm build` + `scripts/check-bundle-budget.mjs` before commit; if regression, pick different boundary |
| Test snapshot drift from new wrapper components | Agents update snapshot tests in own bucket only |
| Analyzer HTML reports too large to commit (>5MB) | Agent D gzips reports if >2MB raw; or commits reduced summary JSON instead |
| 4 agents same `bundle-budget.json` race | Hard rule 5 forbids agent edits — only parent updates after consolidation |

---

## Out of scope (this wave)

- React Server Components migration (separate gap if pursued)
- Backend perf gaps (separate audit follow-ups)
- Page conversions beyond 4-bucket partition (e.g. `/(dashboard)/{branding,parent,settings}/**` — refile if remaining count significant after wave closes)
- `next.config.js` tuning beyond what's already shipped (Wave 7-Perf covered `optimizePackageImports`, `modularizeImports`, image formats)
