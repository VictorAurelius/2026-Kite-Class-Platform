# GAP-670: Wave 99B B1 — Service Catalog + Dependency Graph + Auth Flow + Role-Guard Matrix

**Status:** 🟢 DONE 2026-05-19 — NEW file `documents/02-architecture/service-catalog-and-auth-flow.md` shipped với Backstage-pattern service catalog (18 rows) + Mermaid `flowchart TB` dependency graph (~37 edges across 18 nodes) + Mermaid `sequenceDiagram` auth flow (~24 sequence steps, 4 phases) + role-guard matrix (10 controllers) + closes incident-class GAP-518/GAP-604/GAP-637 anchor
**Priority:** 🔴 P0 (Persona 1+2+3 cite as Top 3 need per Wave 99B outside-in audit)
**Domain:** Meta / Documentation
**Phase:** phase-1-beta
**Wave:** 99b
**Created:** 2026-05-19
**Closed:** 2026-05-19
**Sister gaps:** GAP-668 (B6 foundation — archive sweep, DONE), GAP-669 (B0 Last-Reviewed backfill, DONE), GAP-671..674 (B2-B5 sister buckets Wave 99B, parallel)

---

## Problem

Wave 99B Bucket B1 plan (`documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md` §3 B1) mandate NEW file `documents/02-architecture/service-catalog-and-auth-flow.md` chứa 4 content blocks:

1. **Service Catalog table** (Backstage pattern) — inventory all backend services + frontends + shared infra với per-row: name / repo path / port / responsibility / owner / health endpoint / runbook link / on-call
2. **Dependency Graph (Mermaid flowchart TB)** — inter-service HTTP + RabbitMQ + DB + S3 flows
3. **Auth Flow (Mermaid sequenceDiagram)** — JWT → gateway → @PreAuthorize chain per GAP-604 + tenant context propagation
4. **Role-Guard Matrix** — per-role permitted endpoints, anchor to GAP-518 + GAP-637 recent incidents

Per Wave 99B outside-in audit (3 agents):
- **Persona 1** (new backend dev debug 403): cites Service Catalog + Auth Flow + Database Map as Top 3 need
- **Persona 2** (new frontend dev): cites Service Catalog + Golden-path Onboarding as Top 2 need
- **Persona 3** (SRE on-call): cites Service Dependency + SLO Registry as Top 2 need
- **External Benchmark agent:** Backstage Service Catalog convention + C4 model recommend service catalog as foundation

Coverage gap: pre-Wave-99B, service inventory scattered across `kitehub-architecture.md` §2 + `kiteclass-architecture.md` (partial) + `kitehub/docker-compose.kitehub.yml` (source-of-truth, but YAML format unfriendly for browse). Auth flow documented prose in `kitehub-architecture.md` §3 + `multi-tenant-architecture.md` §3 (split). Role-guard incidents (GAP-518/GAP-604/GAP-637) lack single canonical reference anchor.

## Root Cause

Pre-Wave-99B arch folder evolved organically from Wave 1-91. Service catalog never consolidated because:
- Wave 96 PR2 base arch reports (kitehub/kiteclass/multi-tenant) prioritized per-product narrative không phải cross-product inventory
- ADR-032 (Wave 96 kiteclass-gateway removal) shifted auth scope — old service catalog references would be stale
- Outside-in audit Wave 99B = first systematic surface-up Persona-driven inventory mandate

## Proposed Fix

**Single PR ships:**

1. NEW file `documents/02-architecture/service-catalog-and-auth-flow.md` với:
   - Frontmatter (`audience: dev` + `last-reviewed: 2026-05-19` + sister-docs + scope)
   - §1 Service Catalog — 8 backend rows + 2 frontend rows + 8 infra rows
   - §2 Dependency Graph — Mermaid `flowchart TB` với subgraph layers (Edge / FE / GW / KH / KC / Infra / External)
   - §3 Auth Flow — Mermaid `sequenceDiagram` 4 phases (login → request → role-guard → tenant-context+RLS)
   - §4 Role-Guard Matrix — 10 controllers × @PreAuthorize × permitted roles + reference incidents (GAP-518/GAP-604/GAP-637/GAP-638)
   - §5 References — source-of-truth + sister docs + ADRs + rules + business docs + recent gaps
2. File this GAP-670 với Status DONE + closure log
3. Add gap row to `gap-status.csv` (status:DONE, phase:phase-1-beta, wave:99b, completion_pct:100)

**Out of scope (avoid scope creep):**
- ADR/threat-model index update — separate scope (sister buckets B2/B4)
- README.md folder index update — separate scope (Wave 99B B5 GAP-674)
- C4 L1/L2 diagram split — separate scope (Wave 99B B4 GAP-673)
- DB architecture map consolidation — separate scope (Wave 99B B3 GAP-672)
- Compliance × Code map — separate scope (Wave 99B B2 GAP-671)

## Acceptance Criteria

- [x] NEW file `documents/02-architecture/service-catalog-and-auth-flow.md` created với frontmatter (`audience: dev`, `last-reviewed: 2026-05-19`, `sister-docs`, `scope`)
- [x] §1 Service Catalog table covers 8 backend services + 2 frontends + 8 infra components (=18 rows) với 8 mandatory columns (name/repo/port/responsibility/owner/health/runbook/on-call)
- [x] §2 Dependency Graph — Mermaid `flowchart TB` block với 18 nodes + ~37 edges + subgraph clusters (Edge/FE/GW/KH/KC/Infra/External) per `diagram-format-selection.md` §2.2
- [x] §3 Auth Flow — Mermaid `sequenceDiagram` block với 4 phases (login → request → role-guard → tenant-context+RLS) + ~24 sequence steps + alt branches (JWT invalid/role mismatch/RLS enforce)
- [x] §4 Role-Guard Matrix — 10 controllers × @PreAuthorize annotation × permitted roles + role taxonomy section + 4 recent incident anchors (GAP-518/GAP-604/GAP-637/GAP-638)
- [x] §5 References — source-of-truth files + sister docs + ADRs (ADR-011/023/031/032) + rules (audit-service-isolation/diagram-format-selection/pre-launch-auth-hardening) + business docs + recent gaps
- [x] Vietnamese narrative per `dev-readable-doc-language.md` §2 (technical tokens HTTP/JWT/CORS/RLS/MinIO/SES giữ English natural)
- [x] Mermaid syntax verified clean (zero PlantUML, zero ASCII >5 nodes per `diagram-format-selection.md` rule)
- [x] Cross-link verify — all referenced ADRs (011/023/031/032) + rules + business docs + gaps exist
- [x] `gap-status.csv` row added (GAP-670, status:DONE, phase:phase-1-beta, wave:99b, completion_pct:100)
- [x] Port collision note documented (kitehub-admin + kitehub-branding both container :8083 — verify pre-deploy)
- [x] PR auto-merge eligible per `docs-only-pr-auto-merge.md` v1.0.2 §2 (diff = 1 NEW doc + 1 NEW gap + 1 CSV row sync, all in `documents/**`)

## State-Check Evidence (per `audit-to-gap-pipeline.md` §2.5)

Code state verified 2026-05-19 worktree commit `worktree-agent-a08b04ebd863ab17a`:

| Reference | Verification | Verdict |
|---|---|---|
| `kitehub/docker-compose.kitehub.yml` service list | `grep -E "^\s+(kitehub-\|kiteclass-\|kite-)\w+:" kitehub/docker-compose.kitehub.yml` → 12 service definitions | ✅ verified |
| Backend service pom.xml inventory | `find kitehub kiteclass -maxdepth 2 -name "pom.xml"` → 7 backend services (6 kitehub + 1 kiteclass-core) | ✅ verified |
| Application port mappings | grep `port:` in 6 service application.yml → admin 8083, gateway 9000, subscription 8081, email 8084, branding 8083, core 8081 (compose maps host 8088) | ✅ verified |
| `kitehub-platform/` library check | pom.xml exists, KHÔNG có application.yml + KHÔNG có Dockerfile entry trong compose | ✅ verified — library JAR |
| `kiteclass-gateway/` post-ADR-032 | `find kiteclass -maxdepth 2 -name "pom.xml"` → only `kiteclass-core/pom.xml`, gateway folder removed | ✅ verified |
| `@FeignClient` / `RestTemplate` / `WebClient` usage | grep returned: EmailServiceClient + EmailSenderService + VietQRService + CaptchaService (RestTemplate); OllamaClient + OpenAIClient + BrandingClient (WebClient) | ✅ verified — 7 inter-service/external clients |
| RabbitMQ exchanges | grep "EXCHANGE" trong subscription EmailQueueConfig + PurgeQueueConfig → email.exchange + email.exchange.dlq + instance.purge.exchange (fanout) + branding.events (kiteclass→email) | ✅ verified — 4 exchanges |
| `@PreAuthorize` audit | grep 10 controllers: AdminInstances/Payments/Revenue + ImpersonationController + StaffInvitationController + SubscriptionController + PaymentController + BetaAccessController (public) + AuthController (public) | ✅ verified |
| GAP-604 / GAP-518 / GAP-637 references | `find documents/04-quality/gaps -name "GAP-{518,604,637}-*"` returns 3 files in phase-1-beta + closed/ | ✅ verified |
| `multi-tenant-architecture.md` existence | `ls documents/02-architecture/multi-tenant-architecture.md` exists | ✅ verified |
| `service-catalog-and-auth-flow.md` (this PR creates) | grep returns 0 hits pre-PR | 🆕 to-be-created by this gap |

**Verdict:** all referenced symbols verified ✅ or marked 🆕 to-be-created. No phantom references.

## Pre-Handoff Verify (per `pre-handoff-self-test-completeness.md`)

N/A — docs-only artifact. No user-facing UI flow / login / API endpoint added. Verification = doc render on GitHub (Mermaid native render verified syntactically) + cross-link integrity (all ADR/rule/gap references resolve).

## Closure Log

- **2026-05-19** (DONE): Bucket B1 shipped per Wave 99B plan §3 B1 + §5 verification gates. NEW file `documents/02-architecture/service-catalog-and-auth-flow.md` 18 catalog rows + ~37-edge Mermaid dependency graph + ~24-step Mermaid auth flow sequence + 10-row role-guard matrix + recent incident anchors (GAP-518/GAP-604/GAP-637/GAP-638). State-check evidence § documents 11 verified symbols + 1 to-be-created (this file). Self-test PASS per Wave 99B verification gate B1: Mermaid render preview on GitHub + cross-link verify + persona Top-3 need coverage. Per `audit-to-gap-pipeline.md` §2.5 hardened protocol — no `head` truncation used; all grep ran full output. Per `docs-folder-volume-budget.md` Rule 3 — `documents/02-architecture/` root-level now 11 files (10 from B6 baseline + this new file), under cap 50. Per `gap-folder-organization.md` v2.0.0 §3.3 — gap file placed in `phase-1-beta/closed/` directly (DONE one-way archive). Per `meta-csv-index-pattern.md` — `gap-status.csv` row added same PR.

## Follow-up scope

| Item | Priority | Target wave |
|---|---|---|
| Auto-gen Service Catalog from `@FeignClient` parser + Maven POM scanner (Backstage convention) | P2 | Wave 100+ (defer until inventory >25 services) |
| Auto-gen dependency graph from RabbitMQ exchange + REST client static analysis | P2 | Wave 100+ |
| Kitehub-admin + kitehub-branding port collision cleanup (both container :8083) | P2 | Wave 100+ deployment polish |
| Service Catalog row for kitehub-frontend/kiteclass-frontend health endpoint format verify | P3 | Wave 100+ |
| Role-Guard matrix automated drift detector (BE @PreAuthorize ↔ FE RoleGuard literal reconcile) | P1 META | Wave 100+ (per GAP-518 recurrence prevention) |

## Related

- Wave 99B plan: `documents/03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md`
- Sister gaps: GAP-668 (B6), GAP-669 (B0), GAP-671 (B2), GAP-672 (B3), GAP-673 (B4), GAP-674 (B5)
- Incident anchors: GAP-518 (BE/FE role-name mismatch Wave 71b), GAP-604 (gateway JWT→headers Wave 89), GAP-637 (admin v1 @PreAuthorize missing Wave 92), GAP-638 (6 admin endpoints undocumented Wave 92)
- Sister docs: `documents/02-architecture/kitehub-architecture.md`, `kiteclass-architecture.md`, `multi-tenant-architecture.md`
- ADRs: ADR-011 (tenant isolation), ADR-023 (gateway key resolver), ADR-031 (FE self-host EC2), ADR-032 (kiteclass-gateway removal)
- Rules: `diagram-format-selection.md` (Mermaid mandate), `audit-service-isolation.md` (zero-trust), `pre-launch-auth-hardening-checklist.md` (refresh rotation)
