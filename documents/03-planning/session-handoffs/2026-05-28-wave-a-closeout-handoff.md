---
date: 2026-05-28
session-theme: Wave A (phase2-beta) 3-bucket parallel + post-merge sync + AWS pre-deploy walk scoping
audience: dev
status: complete
next-session-focus: Fix Final canonical state main HEAD gaps (no mass-plan) + AWS pre-deploy human walks
---

# Session handoff — Wave A closeout 2026-05-28

## Shipped main HEAD this session (5 PRs merged)

| PR | Scope | Merge SHA |
|---|---|---|
| #1929 | Wave A Bucket B — re-host staff invitations canonical → kitehub-subscription (GAP-786) | `8d8028d8` |
| #1930 | start-session enhancement — collect-state.sh PR CI status + GAP-NNN correlation | (merged) |
| #1931 | post-merge sync — GAP-786 Log + GAP-789 META filing + pr-logs | `5f725967` |
| #1932 | Wave A Bucket C — verify GAP-704+783 DONE + file GAP-790 (gateway TenantResolver) | `f7c040b8` |
| #1933 | Wave A Bucket D — Course/Class CRUD Owner IT + GAP-791/792 P0 tenant leaks | `58bb142e` |

## Final canonical state main HEAD (post-Wave-A) — NEXT SESSION fix these

User direction 2026-05-28: **fix hết các gaps này; KHÔNG mass-plan retro-walk waves nữa.**

| Gap | Status | Priority | Fix scope |
|---|---|---|---|
| **GAP-787** | OPEN | P0 | Bug #14 staff invite email never sent. Bucket A static analysis: infra 5-hop wired correct; needs RST walk on running stack to surface real root cause (may already be fixed by Wave 103 beta-invite email fix). Likely email-cluster sibling of GAP-702/605/606/713. |
| **GAP-788** | OPEN | P0 META | Wave 80+ retro-walk batch (apply feature-ship-runtime-walk-mandate retroactively). NO mass-plan per user — keep as tracking META. |
| **GAP-789** | OPEN | P1 META | Wave A Bucket B post-merge audit suite (business-logic + api-contract) + 01-business/kitehub/staff-invitations/ 3-layer docs. **Deadline 2026-05-31** per post-wave-audit-mandate §2.2. |
| **GAP-790** | OPEN | P1 | Gateway `/api/v1/staff-invitations/**` route missing `- TenantResolver` filter. 1-line YAML fix in `kitehub-gateway/src/main/resources/application.yml` (compare instance-apis route line 603-608). TenantResolverGatewayFilterFactory JWT tenantId fallback (GAP-711) auto-activates once filter added. **This is the actual root cause of Wave meta-6 walk Bug #8** (403 TENANT_CONTEXT_MISSING, not @PreAuthorize denial). |
| **GAP-791** | OPEN | P0 OWASP A01 | `CourseRepository.findBySearchCriteria(nativeQuery=true)` bypasses Hibernate `@Filter` tenant predicate → list endpoint leaks tenant B courses to tenant A. Empirically reproduced (CourseClassCrudOwnerIT @Disabled test). Cross-flow sweep: batch-audit ALL `nativeQuery=true` sites. |
| **GAP-792** | OPEN | P0 OWASP A01 | `CourseServiceImpl.@Cacheable(key="#id")` cache key missing tenantId → Redis cross-tenant pollution (500 serialization OR silent data leak). Cross-flow sweep: batch-audit ALL `@Cacheable` sites. |

DONE this session (no action needed): GAP-786 (Bug #17 user provision), GAP-704 (JWT tenantId — verified Wave 104 ship), GAP-783 (Owner 403 — not reproduce; root cause = GAP-790).

## AWS pre-deploy human-walk scoping (answer to user question)

**Staff invite flow ✅ walked (Wave A Bucket B).** Before AWS deploy, **5 more critical beta-tenant flows need human RST walk** (in beta-tenant journey order):

| # | Flow | Gaps involved | Blocked by |
|---|---|---|---|
| 1 | **Anonymous signup → login** (beta request → admin approve → email → set password → login → dashboard) | GAP-372 + GAP-702 + GAP-576 + GAP-704 | GAP-702 email firing + GAP-787 email path |
| 2 | **Owner onboarding wizard** (first-run post-login) | GAP-588 + GAP-712 + GAP-714 | — (likely ready) |
| 3 | **Course/Class CRUD** (core daily use) | GAP-791 + GAP-792 | **MUST fix GAP-791+792 P0 tenant leaks FIRST**, then walk |
| 4 | **Email delivery end-to-end** (invite + approval + welcome + password reset → MailHog/SES) | GAP-787 + GAP-702 + GAP-703 + GAP-605/606/713 | email cluster — Bug #14 class |
| 5 | **PDPL consent banner** (every anonymous visitor) | GAP-585 + GAP-737 | compliance gate |

**Payment flow = defer Phase 1.5** (not beta scope per release-1-plan-2026.md §4).

**Dependency note:** flows 1 + 4 share the email pipeline (Bug #14 class). Flow 3 blocked by 2 P0 tenant leaks. So pre-deploy sequence:
1. Fix GAP-790 (gateway 1-line) + GAP-791 + GAP-792 (tenant isolation) + GAP-787 email cluster
2. Walk flows 1-5 on running stack (production-equivalent)
3. Then AWS deploy (also requires GAP-612 AWS account restoration — suspended ~11 days)

## Blockers carried (not Wave A scope)

- **GAP-612** — AWS account suspended ~11 days → blocks production deploy chain entirely. Resolve via AWS support OR account migration BEFORE any deploy.
- **GAP-746** — kiteclass-core preexisting test flake (`AttendanceClassBatchControllerIT`) — multi-tenant functional bug, separable.
- **GAP-257 + GAP-144** — ops carry (restore drill + AlertManager receivers).

## META rules shipped this session

- `cross-flow-bug-class-sweep.md` v1.0.0
- `feature-ship-runtime-walk-mandate.md` v1.1.0 §3.4 (catalog-then-batch)
- `docs-only-pr-no-block-wait.md` v1.0.0
- collect-state.sh PR+gap correlation (PR #1930)

## Session notes

- 3 Opus agents spawned 2x (1st round quota-exhausted ~1k tokens each; 2nd round post-reset succeeded except Bucket A which hit context budget on rule auto-loads + did static-analysis-only).
- GAP-789 number collided 3-way (Bucket B audit META + Bucket C gateway + Bucket D nativeQuery); reconciled: META kept 789, Bucket C → 790, Bucket D → 791+792.
- gap-status.csv 609 rows validated.
