# GAP-214: Wave 5 post-wave audit suite refresh

**Status:** 🟢 DONE (Sub-PR 5.6a, 2026-04-25 — 5 audit reports committed)
**Priority:** 🟠 P1 — governance compliance gate for Wave 5 closure
**Domain:** Quality / Audit governance
**Found:** 2026-04-25 (during Sub-PR 5.5 self-review of audit freshness)
**Affects:** `kiteclass-core` document-generation module + `pom.xml` dependency surface; gates Wave 5 merge to main

## Problem

`post-wave-audit-mandate.md` §2.2 + §3 require audits to refresh within 7 days for code PRs that touch covered file patterns. By 2026-04-25 (Sub-PR 5.5 push), two required audits are 8 days stale:

| Audit | Last run | Triggered by Sub-PR 5.5 |
|-------|----------|-------------------------|
| API Contract /100 | `audits/api/api-contract-audit-2026-04-17-saas.md` | New `DocumentGenerationController`, `DocumentGenerationRequestDto`, `api-contract.md` rewrite |
| Security /100 | `audits/security/security-audit-2026-04-17-saas.md` | `pom.xml` ognl re-pin (3.4.x → 3.3.4) + new auth-protected endpoints + tenant resolution path |

Wave 5 has been merging sub-PRs daily since 2026-04-24 (5.0/5.1/5.2/5.3) — each was a code PR touching covered patterns. Cumulative drift on both categories is real, not just hours-over-threshold.

Two more audits are also stale ahead of wave completion (lower priority for *this* PR but in scope for Sub-PR 5.6 per §4 Day 0–3 cadence):
- Performance /100 baseline (`audits/performance/`) — last 2026-04-19, 6 days old, but Wave 5 added document generation (PDF rendering ~2-5s p95 expected per BR-DOC-PDF-007); needs refresh.
- Ops Readiness /100 (`audits/ops/`) — last 2026-04-19, 6 days; new public endpoints + branding cache path warrant a re-look.
- Quality /100 refresh — last 2026-04-19 (77/100 C+, honest baseline); should rerun post-wave per `post-wave-audit-mandate.md` §2.3.

## Root Cause

Per-sub-PR cadence within an active wave drifts past 7-day threshold. `audit-gate.py` hook would block in-Claude-session ops, but doesn't enforce on `git push` / `gh pr create` — so PRs land without surfacing the gap until self-review.

## Proposed Fix

Run the full audit suite during Sub-PR 5.6 (Wave 5 wave-completion), per `post-wave-audit-mandate.md` §4 Day 0–3 cadence:

1. **API Contract /100** — covers Sub-PRs 5.0–5.5 cumulative public-surface changes (Generator interface, DocumentRequest/Response, DocumentGenerationController endpoints, OpenAPI annotations).
2. **Security /100** — covers `pom.xml` cumulative bumps (jjwt, springdoc, tika, jsoup, opencsv, jacoco, AWS SDK, commons-compress, poi, ognl) + new auth-protected endpoints + tenant resolution path through `BrandingService.getBranding()`.
3. **Performance /100** — Wave 5 introduces synchronous PDF/XLSX/DOCX generation with branding lookup; needs measured baseline to confirm BR-DOC-PDF-007 budget (`<2s p95 for 1-page invoice`) and decide whether GAP-210 async queue is still optional.
4. **Ops Readiness /100** — new HTTP surface + branding cache reads; rate-limit posture, observability hooks, error-response format conformance.
5. **Quality /100 refresh** — full 10-category rerun per `post-wave-audit-mandate.md` §2.3 (mandatory after every wave merge).

Each audit follows `audit-to-gap-pipeline.md` Step 1–5 — issues found → gap files → fix queue.

## Acceptance Criteria

- [ ] `documents/04-quality/audits/api/api-contract-audit-2026-04-{XX}-saas.md` committed (where XX ≤ 28 to stay within Day 0–3 of Wave 5 merge)
- [ ] `documents/04-quality/audits/security/security-audit-2026-04-{XX}-saas.md` committed
- [ ] `documents/04-quality/audits/performance/performance-audit-2026-04-{XX}.md` committed
- [ ] `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-{XX}.md` committed
- [ ] `documents/04-quality/audits/quality/quality-audit-2026-04-{XX}.md` committed
- [ ] Any P0/P1 findings filed as new gaps per `audit-to-gap-pipeline.md`
- [ ] `output-review-mandate.md` §3 matrix rows updated with new audit dates
- [ ] Sub-PR 5.6 PR description references this gap as closed

## Used as AUDIT_OVERRIDE link

Sub-PR 5.5 (PR #529) merges with `AUDIT_OVERRIDE: <reason> documents/04-quality/gaps/closed/GAP-214-wave5-post-wave-audit-suite.md` per `post-wave-audit-mandate.md` §3 override clause. The override is honoured because (a) Sub-PR 5.5 is mid-wave (not the wave-merge itself), (b) Sub-PR 5.6 is the natural slot for the full suite per §4 Day 0–3 cadence, and (c) this gap commits to the schedule.

## Related

- `documents/03-planning/waves/wave-05-document-generation.md` §4 Sub-PR 5.6 scope (audit deliverable added there)
- `documents/04-quality/gaps/closed/GAP-047-document-generation-skills.md` (Wave 5 master gap — references this for audit closure)
- `.claude/rules/post-wave-audit-mandate.md` §2.2 freshness, §3 enforcement, §4 runbook, §5 baselines
- `.claude/rules/audit-to-gap-pipeline.md` (workflow each audit will follow)
- Past audit reports being refreshed:
  - `documents/04-quality/audits/api/api-contract-audit-2026-04-17-saas.md`
  - `documents/04-quality/audits/security/security-audit-2026-04-17-saas.md`
  - `documents/04-quality/audits/performance/performance-audit-2026-04-19.md` (assumed path)
  - `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` (assumed path)
  - `documents/04-quality/audits/quality/quality-audit-2026-04-19.md` (assumed path)

## Log

- **2026-04-25 (CLOSED via Sub-PR 5.6a):** All 5 audits ran in parallel via Explore agents + parent compilation. Reports committed under `documents/04-quality/audits/{api,security,performance,ops,quality}/`. Findings: 4 P0 (filed as GAP-215/216/217/218, all blocking Sub-PR 5.6b per wave plan §4), 5 P1 + 8 P2/P3 (filed as umbrella GAP-219). Scores: api 95, security 85, performance 63, ops 52, quality 78. Wave 5 stays code-complete + audited; ops debt explicit (alerting + structured logs + Prometheus prod deploy carry from baseline). Status 🔵 → 🟢 DONE.
- **2026-04-25:** Gap created during Sub-PR 5.5 self-review. API Contract + Security audits 8 days stale; Performance/Ops/Quality 6 days. Filed to give Sub-PR 5.5 PR #529 a valid AUDIT_OVERRIDE link per `post-wave-audit-mandate.md` §3, and to commit Sub-PR 5.6 to running the full suite.
