# GAP-757 — Wave beta-prep-1 post-wave audit suite refresh

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta (audit governance)
**Detected:** 2026-05-26
**Deadline:** 2026-05-29 (3-day window per `post-wave-audit-mandate.md` §2.2)
**Related Wave:** Wave beta-prep-1 closure 2026-05-26 (main HEAD `a64bcef2`)

## Problem

Per `.claude/rules/post-wave-audit-mandate.md` §2.2 — after wave merge, required audit suite MUST run within 3 days. Wave beta-prep-1 merged 7 PRs cluster cuối ngày 2026-05-26. Deadline 2026-05-29.

Wave beta-prep-1 touches MULTIPLE domains per §2.1 file-pattern matrix → cannot defer via §2.4 Domain-Milestone Audit Cadence (which requires SINGLE domain).

## Proposed Fix

Run audit suite per §2.1 file-pattern matrix on wave changes:

| Domain | Audit | Bucket coverage |
|---|---|---|
| **Security** /100 | `quality/security-audit/SKILL.md` v2 format | Bucket B (CVE + auth race + upload cap + bucket policy + RLS negative) + Bucket A (consent BE persistence gap = security finding) |
| **Ops Readiness** /100 | `quality/ops-readiness-audit/SKILL.md` | Bucket C (Statuspage + 8 SNS alarms terraform + restore drill framework) |
| **UI** /128 per-screen sample | `quality/ui-review/SKILL.md` | Bucket F+G (BetaRequestForm + landing footer + HelpLink + waitlist page + 4 new runbooks audience verify) |
| **Performance** /100 | `quality/performance-audit/SKILL.md` | Bucket E concurrency 5 paths (DataIntegrityViolation + idempotent verify + race recovery) — verify no perf regression vs baseline 86/100 B+ Wave 85 |
| **Business Logic** /100 | `quality/business-logic-audit/SKILL.md` | Bucket A PDPL legal docs match `documents/01-business/legal/` + Bucket H ADR-036 + Bucket E rule compliance |
| **Quality** /110 | `quality-audit/SKILL.md` | 11-category refresh post-wave baseline (vs 90/110 B+ Wave 98) |

## Acceptance Criteria

- [ ] 6 audit reports filed `documents/04-quality/audits/{security,ops-readiness,ui,performance,business,quality}/2026-05-XX-wave-beta-prep-1-*.md`
- [ ] `audits-index.csv` 6 new rows added (per `meta-csv-index-pattern.md` 100% coverage parity)
- [ ] New gap files filed cho findings per `audit-to-gap-pipeline.md` Step 3
- [ ] `output-review-mandate.md` §3 matrix rows updated với new scores + delta annotations (per audit refresh cadence)
- [ ] ROADMAP §🎯 Current Status: post-wave-audit refresh entry
- [ ] Deadline met 2026-05-29 (3-day window per `post-wave-audit-mandate.md` §2.2)

## Dependencies + Blockers

- **GAP-756 Wave production deploy** — some audits (UI manual walk, Performance live latency) benefit from deployed code. Domain audits SECURITY/Ops/Business can run on main code without deploy.
- **Time budget** — 6 audits × ~30-45 min each = ~3-4h coordinator-inline. Can parallelize via 4-5 Opus agents per `feedback_parallel_agent_strategy.md`.

## Effort estimate

**Total: ~3-4h** parallel (or ~6-8h serial). 4-5 Opus agents spawn pattern per `wave-pack-planner` skill.

## Log

- **2026-05-26 (Filed P1 OPEN):** GAP-757 created as Wave beta-prep-1 closure follow-up per `post-wave-audit-mandate.md` §2.2 3-day window. Deadline 2026-05-29. Spawn 4-5 Opus parallel audit agents recommended per wave-pack-planner pattern.
