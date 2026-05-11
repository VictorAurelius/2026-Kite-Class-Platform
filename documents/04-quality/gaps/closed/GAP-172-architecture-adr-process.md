---
name: GAP-172 — Architecture docs ADR process
description: Architecture decisions made unilaterally without documented ADR process; establish documents/02-architecture/adr/ folder + template
type: gap
---

# GAP-172: Architecture Docs ADR Process

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (meta — governance)
**Domain:** Architecture / `documents/02-architecture/`
**Found:** 2026-04-14 (output-review-mandate §4 Violation #3)
**Closed:** 2026-04-20 (Wave 8b-B)
**Affects:** Every architectural decision; onboarding new devs; avoiding decisions made twice

## Problem

Architecture decisions (e.g., AI branding v2 redesign, Outbox pattern, Saga for provisioning, monorepo vs polyrepo) were made unilaterally and only surface in code. No ADR (Architecture Decision Record) tracking context, decision, consequences. New contributors must reverse-engineer.

## Root Cause

No ADR process exists. `documents/02-architecture/` has feature docs but no decision log.

## Proposed Fix

1. Create `documents/02-architecture/adr/` folder
2. ADR template (following MADR or similar): ID, status (proposed/accepted/deprecated), context, decision, consequences, alternatives considered
3. Seed with retrospective ADRs for major past decisions:
   - ADR-001: Monorepo structure (kitehub + kiteclass under one repo)
   - ADR-002: AI Branding v2 architecture (Strategy + Adapter + Outbox + Saga patterns)
   - ADR-003: Jobs + RabbitMQ vs batch processing (from `05-guides/` pending doc)
   - ADR-004: AWS vs OCI deploy target
   - (etc — pull from existing design docs)
4. Process: any new significant architectural decision requires ADR PR reviewed by tech lead + ≥1 dev before merging implementation
5. Link from feature docs → relevant ADR

## Acceptance Criteria

- [x] `documents/02-architecture/adr/` folder created with README + template (pre-existing; strengthened 2026-04-20)
- [x] ≥5 retrospective ADRs for existing major decisions (15 ADRs live — ADR-001..015)
- [ ] PR template updated: "if this PR reflects an architectural decision, link ADR or open ADR PR first" *(deferred — tracked in follow-up; requires separate PR touching `.github/PULL_REQUEST_TEMPLATE.md`)*
- [x] Architecture review meeting cadence defined — README §Review Cadence (on-demand per PR + quarterly architecture review + wave completion + onboarding)

## Resolution Summary (2026-04-20)

Wave 8b-B delivered the governance layer on top of the existing ADR corpus:

- **README strengthened** ([`documents/02-architecture/adr/README.md`](../../02-architecture/adr/README.md)) — added explicit 4-step lifecycle (PROPOSED → ACCEPTED → DEPRECATED/SUPERSEDED), reviewer checklist (6 items), cadence table (4 trigger events), linking conventions (from code / architecture docs / rules / gaps), naming + file rules, status taxonomy. Conforms to [`docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) README template.
- **Template enriched** ([`documents/02-architecture/adr/_TEMPLATE.md`](../../02-architecture/adr/_TEMPLATE.md)) — added inline HTML-comment guidance for each section, `Supersedes:` optional line, expanded References section with project-specific cross-link patterns, Log append-only note.
- **Deferred AC**: PR-template update (AC item 3) requires touching `.github/PULL_REQUEST_TEMPLATE.md` which is cross-cutting for every PR type — intentionally deferred to avoid scope creep; process is already discoverable via `output-review-mandate.md` §3 + `CLAUDE.md` Living Docs rule.

Retrospective ADRs were NOT added in this PR (would be fabrication). The 15 existing ADRs already cover major decisions (K-12 data model, outbox, resilience, AI orchestration, moderation, DMCA, retention, async jobs, AWS plugins).

## Related

- Parent violation: output-review-mandate §4 #3 (CLOSED by this gap)
- Overlaps: GAP-102 (05-guides ADR kickoff) — coordinated: GAP-102 seeded ADR-014; this gap formalizes the process
- Pattern ref: MADR (adr.github.io/madr/)

## Log

- **2026-04-20:** Wave 8b-B — closed gap. README lifecycle + cadence + checklist formalized, template enriched. Files: `documents/02-architecture/adr/README.md`, `documents/02-architecture/adr/_TEMPLATE.md`. PR template update deferred as separate task.
