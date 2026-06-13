# Architecture Decision Records (ADRs)

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) · [`.claude/rules/output-review-mandate.md`](../../../.claude/rules/output-review-mandate.md) §4
**Parent:** [`documents/02-architecture/`](../)

Lightweight records of significant architecture decisions — the "why" layer for technical choices. Each ADR captures the **context**, **decision**, and **consequences** so future contributors don't re-litigate settled questions or reverse-engineer intent from code.

**Audience:** Architects, Tech Leads, Senior Engineers. Secondary: new contributors onboarding, auditors reviewing rationale.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index + process | 1 |
| [`_TEMPLATE.md`](_TEMPLATE.md) | MADR-inspired template for new ADRs | 1 |
| `ADR-NNN-*.md` | Individual decision records (zero-padded 3-digit ID) | 15+ |

---

## When to Write an ADR

Write an ADR when the decision satisfies **≥1** criterion:

- ✅ Choosing between ≥2 viable alternatives (framework, pattern, infra, vendor)
- ✅ Design affects >1 service / module / bounded context
- ✅ Hard-to-reverse change (schema, API contract, integration surface)
- ✅ Cross-cutting concerns (security, performance, deployment, observability)
- ✅ Policy/compliance choice (data retention, moderation, DMCA)

### When NOT to write one

- ❌ Trivial implementation details (helper methods, local naming)
- ❌ Easy-to-reverse choices (internal refactors)
- ❌ Already covered by an accepted ADR or project rule (link instead)

If in doubt → write it. Cheap to add, expensive to miss.

---

## ADR Process (Lifecycle)

Every new ADR follows the 4-step governance loop. Each ADR exists in exactly one **Status** at any time.

### Step 1 — PROPOSED (author drafts)

1. Copy [`_TEMPLATE.md`](_TEMPLATE.md) → `ADR-NNN-kebab-title.md`
2. Pick the next free NNN from the index below
3. Fill Context, Decision, Consequences, Alternatives
4. Set `Status: PROPOSED` and open a PR labeled `adr`

### Step 2 — REVIEW (PR required, min 2 approvals)

- **Required:** Tech Lead + ≥1 senior engineer (architect ideal)
- **Optional consult:** domain owner (security/DBA/SRE/product) depending on scope
- Review checklist (reviewer):
  - [ ] Context clear enough that a newcomer understands the forces
  - [ ] Decision stated plainly ("We will do X") — no hedging
  - [ ] Positive + negative consequences honest (not just upsides)
  - [ ] ≥2 alternatives considered with reasons for rejection
  - [ ] Links to related ADRs, gaps, rules, patterns
  - [ ] No conflict with an existing accepted ADR (if yes → must explicitly supersede)

### Step 3 — ACCEPTED (merge)

- Update `Status: ACCEPTED`
- Append to Index table in this README
- Add Log entry at the bottom of this README (date + summary + gap link if any)
- Merge PR — ADR is now canonical.

**Never edit content of an ACCEPTED ADR.** Corrections to the decision itself require a new ADR that supersedes it (Step 4). Typo-level fixes to an accepted ADR are OK.

### Step 4 — DEPRECATED / SUPERSEDED (decision changes)

When a new decision replaces an old one:

1. Write the new ADR following Steps 1-3
2. In the new ADR's "References" section: `Supersedes ADR-NNN`
3. In the old ADR: update `Status: SUPERSEDED by ADR-MMM` and add a Log line
4. Keep the old ADR in place (history matters) — do NOT delete or archive

---

## Review Cadence

| Event | Cadence | Action |
|-------|:-------:|--------|
| New ADR PR | On demand | 2-approver review per Step 2 |
| Wave completion | Per wave | Verify ADRs authored for significant wave decisions |
| Quarterly architecture review | Quarterly | Scan index for DEPRECATED candidates + missing ADRs |
| Onboarding | Per new senior hire | New hire reads all ACCEPTED ADRs |

Out-of-cadence reviews triggered by: production incident referencing a decision, major version bump of a framework cited in an ADR, or explicit request in `output-review-mandate.md` §4 audit.

---

## Index

| ADR | Title | Status | Date |
|-----|-------|:------:|------|
| [001](ADR-001-k12-data-model.md) | K-12 Multi-Subject Data Model | ACCEPTED | 2026-04-14 |
| [002](ADR-002-academic-year-structure.md) | Academic Year + Semester Structure | ACCEPTED | 2026-04-14 |
| [003](ADR-003-role-hierarchy.md) | Hierarchical Role-Based Access Control | ACCEPTED | 2026-04-14 |
| [004](ADR-004-instance-lifecycle.md) | Frontend Instance Provisioning Lifecycle | ACCEPTED | 2026-04-14 |
| [005](ADR-005-resource-classification.md) | Resource Classification Pipeline | ACCEPTED | 2026-04-14 |
| [006](ADR-006-ai-agent-orchestration.md) | AI Agent Orchestration (Analyzer → Planner → Executor) | ACCEPTED | 2026-04-14 |
| [007](ADR-007-outbox-pattern-for-events.md) | Outbox Pattern for Reliable Event Publishing | ACCEPTED | 2026-04-14 |
| [008](ADR-008-resilience-for-ai-calls.md) | Resilience for External AI Calls (Circuit Breaker + Bulkhead + Retry) | ACCEPTED | 2026-04-14 |
| [009](ADR-009-branding-package-api.md) | Branding Package Composite API | ACCEPTED | 2026-04-14 |
| [010](ADR-010-content-moderation-policy.md) | Content Moderation Policy (Staged Review) | ACCEPTED | 2026-04-14 |
| [011](ADR-011-defense-in-depth-security.md) | Defense-in-Depth Security (Validators + Output-Encoders + CSP) | ACCEPTED | 2026-04-14 |
| [012](ADR-012-dmca-trademark-workflow.md) | DMCA / Trademark Workflow | ACCEPTED | 2026-04-14 |
| [013](ADR-013-data-retention-classification.md) | Data Retention Classification (GDPR + VN Compliance) | ACCEPTED | 2026-04-14 |
| [014](ADR-014-async-jobs-queue-over-batch.md) | Async Jobs Queue (RabbitMQ) over Batch Framework | ACCEPTED | 2026-04-18 |
| [015](ADR-015-aws-agent-plugins-evaluation.md) | AWS Agent Plugins Evaluation — Defer Adoption | ACCEPTED | 2026-04-18 |
| [016](ADR-016-fe-be-contract-strategy.md) | Frontend ↔ Backend Contract Strategy | ACCEPTED | 2026-04-19 |
| [017](ADR-017-mis-sync-strategy.md) | School MIS Sync Strategy (One-Shot vs Live) | PROPOSED | 2026-04-21 |
| [018](ADR-018-domain-registrar-dns.md) | Domain Registrar, DNS Provider, and TLD Policy | ACCEPTED (draft) | 2026-04-21 |
| [019](ADR-019-document-generation-architecture.md) | Document Generation Architecture (Wave 5 / GAP-047) | ACCEPTED | 2026-04-25 |
| [020](ADR-020-vendor-client-package-naming.md) | Vendor Client Package Naming Convention | ACCEPTED | 2026-04-26 |
| [021](ADR-021-per-module-outbox-vs-shared-lib.md) | Per-Module Domain Outbox over Cross-Product Shared Library | PROPOSED | 2026-04-26 |
| [022](ADR-022-alertmanager-secret-strategy.md) | Alertmanager Secret Strategy — External Secrets Operator + AWS Secrets Manager | ACCEPTED | 2026-04-28 |
| [023](ADR-023-gateway-key-resolver-strategy.md) | Gateway Rate-Limit Key Resolver Strategy — IP / Tenant / API Key | ACCEPTED | 2026-04-28 |
| [024](ADR-024-shared-ui-lib-strategy.md) | Shared UI Library Strategy — pnpm Workspace Package (`@kite/shared-ui`) | ACCEPTED | 2026-04-30 |
| [025](ADR-025-aws-only-deploy-phase-1-free-tier.md) | AWS-only Deploy for Phase 1 BETA (Free Tier, Singapore region) — supersede Oracle Cloud primary | ACCEPTED | 2026-05-07 |
| [026](ADR-026-ollama-defer-phase-2.md) | Defer Ollama / FULL_AI Inference to Phase 2 (Phase 1 BETA = Template-Only) | ACCEPTED | 2026-05-07 |
| [027](ADR-027-statuspage-vendor.md) | Status Page Vendor — Instatus Free Tier (Phase 1 BETA) | ACCEPTED | 2026-05-07 |
| [028](ADR-028-ecs-fargate-vs-eks-phase-1-beta.md) | ECS Fargate vs EKS for Phase 1 BETA Container Orchestration | ACCEPTED | 2026-05-11 |

Next free ID: **ADR-046**.

> **Canonical index:** This per-ADR table is human-readable but stops at ADR-028 (pre-existing drift). The authoritative, always-current index is [`adrs-index.csv`](adrs-index.csv) (per [ADR-030](ADR-030-csv-canonical-meta-indexes.md) + `meta-csv-index-pattern.md`) — query via `bash scripts/query-adrs.sh`. ADR-029 → ADR-045 live in the CSV. Wave kitehub-biz-100 added ADR-041..045 (tier-sync centralization, trial→paid atomicity, manual-VietQR dunning/churn lifecycle, owner notification channels, custom-domain state machine).

---

## Naming & File Rules

- Filename: `ADR-NNN-short-kebab-title.md` (3-digit zero-padded)
- Template: `_TEMPLATE.md` (leading underscore keeps it sorting first)
- Title inside file: `# ADR-NNN: Title Case With Spaces`
- One decision per ADR — split large topics into multiple ADRs cross-linked

---

## Status Taxonomy

```
PROPOSED  → Under review in open PR (not yet canonical)
ACCEPTED  → Merged + canonical; implementations align with this
DEPRECATED → No longer followed but not yet replaced (action pending)
SUPERSEDED by ADR-MMM → Replaced by a newer decision
```

Transitions: `PROPOSED → ACCEPTED → (DEPRECATED | SUPERSEDED)`. Never re-open an ACCEPTED ADR — write a new one that supersedes it.

---

## Linking Conventions

When referencing ADRs from other docs:

- **From code:** JavaDoc `@see documents/02-architecture/adr/ADR-NNN-title.md`
- **From architecture docs:** markdown link `[ADR-NNN](adr/ADR-NNN-title.md)`
- **From rules:** reference by ID only (`per ADR-NNN`) — rules are cross-linked via rule index
- **From gaps:** add `Related ADR: ADR-NNN` under Related section

---

## Archive Policy

**ADRs are NEVER archived or deleted.** They form an append-only historical record. Instead:

- Decision no longer followed → mark DEPRECATED + append Log line
- Decision replaced → mark SUPERSEDED by ADR-MMM + append Log line
- Folder relocation → update links; preserve ADR IDs across reorganizations

Non-ADR files in this folder (hypothetical drafts, notes) follow the parent folder archive policy ([`../README.md`](../README.md) §Archive Policy).

---

## Related

- [`../README.md`](../README.md) — parent 02-architecture index
- [`.claude/rules/design-patterns.md`](../../../.claude/rules/design-patterns.md) — applied patterns referenced by many ADRs
- [`.claude/rules/output-review-mandate.md`](../../../.claude/rules/output-review-mandate.md) §4 — mandates this ADR process
- [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) — README format rule (this README conforms)
- [MADR format](https://adr.github.io/madr/) — inspiration for `_TEMPLATE.md`
- [Michael Nygard original post](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions) — Context/Decision/Consequences format

---

## Log

- **2026-04-28:** Added ADR-023 ACCEPTED (Gateway Rate-Limit Key Resolver Strategy — IP / Tenant / API Key). Closes GAP-259 PARTIAL (tenant + apiKey resolvers + branding route wiring + metrics filter + tier-multiplier config keys data-only). GAP-260 follow-up tracks tier-multiplier enforcement + remaining route coverage. Alternatives JWT-only / TenantResolver-first / Envoy-Kong rejected.
- **2026-04-28:** Added ADR-022 ACCEPTED (Alertmanager Secret Strategy — ESO + AWS Secrets Manager). Closes Wave Observability GAP-144 secret-strategy AC. Formalizes pre-existing `terraform-aws/secrets.tf` ESO intent (line 48-56). Alternatives sealed-secrets / raw values / Vault rejected.
- **2026-04-24:** Added ADR-019 PROPOSED (Document Generation Architecture — Wave 5 Sub-PR 5.0 / GAP-047). Records pure-backend, inline-in-core, OpenHTMLtoPDF+PDFBox+POI decision; alternatives iText/hybrid/microservice/headless-browser rejected.
- **2026-04-21:** Added ADR-017 PROPOSED (School MIS sync strategy — GAP-200 Phase 1). Backfilled ADR-016 index entry (pre-existing drift).
- **2026-04-20:** Governance upgrade (GAP-172) — README expanded with explicit 4-step lifecycle (PROPOSED → ACCEPTED → DEPRECATED/SUPERSEDED), reviewer checklist, cadence table, linking conventions. Template enriched. Closes `output-review-mandate.md` §4 #3 Architecture Docs violation.
- **2026-04-18:** Added ADR-015 (AWS Agent Plugins defer, GAP-103).
- **2026-04-18:** Index backfilled (was showing only 5/13). Added ADR-014 capturing RabbitMQ-over-Batch decision retroactively (GAP-102 ADR kickoff).
- **2026-04-14:** ADRs 001-013 created (initial architecture documentation sweep).
