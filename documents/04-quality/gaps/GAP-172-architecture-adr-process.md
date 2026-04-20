---
name: GAP-172 — Architecture docs ADR process
description: Architecture decisions made unilaterally without documented ADR process; establish documents/02-architecture/adr/ folder + template
type: gap
---

# GAP-172: Architecture Docs ADR Process

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (meta — governance)
**Domain:** Architecture / `documents/02-architecture/`
**Found:** 2026-04-14 (output-review-mandate §4 Violation #3)
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

- [ ] `documents/02-architecture/adr/` folder created with README + template
- [ ] ≥5 retrospective ADRs for existing major decisions
- [ ] PR template updated: "if this PR reflects an architectural decision, link ADR or open ADR PR first"
- [ ] Architecture review meeting cadence defined (weekly? biweekly?)

## Related

- Parent violation: output-review-mandate §4 #3
- Overlaps: GAP-102 (05-guides ADR kickoff) — may merge or coordinate
- Pattern ref: MADR (markdown-adr.github.io)
