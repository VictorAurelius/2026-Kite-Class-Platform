# ADR-012: DMCA / Trademark Workflow

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Legal + Architect
**Related Gap:** GAP-042 (Wave 4 Sub-PR 4.3)

## Context

AI-generated branding can accidentally produce:
- Logos resembling registered trademarks (legal exposure)
- Content derived from copyrighted imagery (DMCA takedown required)
- Brand elements infringing on third-party IP

Without a workflow we have no response procedure when takedown notices arrive, and our Terms of Service can't point at a process.

## Decision

**Two-track workflow with shared audit:**

### Track 1: Proactive trademark check (generation-time)
- `TrademarkCheckService` — runs during `GenerateLogoStep` (Wave 3 async step)
- Compares logo hash + textual trademark keywords against seed trademark list (internal + optionally USPTO API in future)
- On hit: pipeline routes to TEMPLATE category (fallback) + flags for human review
- Scaffold in Sub-PR 4.3; real OSS trademark API integration deferred

### Track 2: Reactive DMCA takedown
- Public intake: `POST /public/dmca` (rate-limited, captcha-gated; form on `/legal/dmca` page)
- Creates `DmcaTakedownRequest` entity (PENDING → REVIEWING → VALID/INVALID → EXECUTED/CONTESTED)
- State machine; audit log entry per transition
- VALID → flag affected branding asset → revert to TEMPLATE category; notify tenant with counter-notice window (10 days per DMCA §512(g))
- CONTESTED → restore asset after window if no court order received

Both tracks write to the shared `audit_log` (foundation from Sub-PR 4.0).

## Consequences

### Positive
- ✅ Legal defensibility: we have a documented procedure
- ✅ Separates proactive (prevents issues) from reactive (responds to notices)
- ✅ Audit trail satisfies §512 "safe harbor" records requirement
- ✅ State machine prevents double-takedowns / race conditions

### Negative
- ❌ Seed trademark list is a curation burden
- ❌ DMCA counter-notice window introduces live operational latency
- ❌ False takedowns can harm tenants — human review mandatory for VALID/INVALID decision

## Alternatives

- **A. Skip proactive check, reactive only** — rejected: regulators increasingly expect "reasonable measures"
- **B. Outsource to legal-as-a-service (e.g. Counter-Notice API)** — deferred: vendor evaluation outside wave scope
- **C. Auto-approve DMCA valid → execute without human review** — rejected: false-positive cost too high

## Implementation Notes

Sub-PR 4.3 delivers:
- Entity + state machine + service
- V37 migration
- `/public/dmca` intake endpoint (rate-limited via existing `RateLimitingFilter`)
- KiteHub FE `/legal/dmca` page with form
- Seed `banned_trademarks.yml` (scaffold)

Deferred: USPTO API integration, counter-notice email automation.

## References
- GAP-042
- DMCA §512 (safe harbor + counter-notice)
- Our `ai-branding-guidelines.md` §4.4 (anti-patterns: AI must never use external brand assets)

## Log
- 2026-04-14 — Accepted
