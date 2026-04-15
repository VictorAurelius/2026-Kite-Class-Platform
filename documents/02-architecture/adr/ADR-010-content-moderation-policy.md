# ADR-010: Content Moderation Policy (Staged Review)

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Legal + Architect
**Related Gap:** GAP-018 (Wave 4 Sub-PR 4.1)

## Context

AI branding produces images + text that may contain NSFW / hate / copyright-violating content. We must:
- Block publication of unsafe content to a live tenant site
- Keep automated pipeline fast (>95% of traffic should skip human review)
- Maintain audit trail for regulators / DMCA responses
- Avoid over-blocking legitimate brand art (false positive cost is high for tenants)

## Decision

**3-stage content moderation pipeline:**

```
AI output
   │
   ▼
Stage 1: Pre-check (automated, synchronous)
   ├── NSFW classifier (hash match + vision model score)
   ├── Keyword bans (e.g. competitor trademark list)
   └── Content-safety heuristics (banned colors on flag-adjacent imagery)
   │
   ├── PASS ──► Stage 3 directly
   └── FAIL ──► Stage 2
   │
Stage 2: Template-only fallback (automated)
   ├── AI output discarded
   ├── Re-run pipeline with TEMPLATE category only
   └── PASS ──► Stage 3; FAIL ──► human review queue
   │
Stage 3: Deploy (no more checks)

Stage X: Human review queue (admin UI, follow-up wave)
```

**State machine** on every moderation run (`ModerationStatus` enum):
- PENDING → APPROVED / REJECTED / NEEDS_HUMAN_REVIEW (terminal: APPROVED, REJECTED)

Every transition writes to `audit_log` (shared foundation from Sub-PR 4.0).

## Consequences

### Positive
- ✅ ≥95% traffic never waits for humans (Stage 1 pre-check)
- ✅ Legitimate content protected by template-only fallback
- ✅ Auditable (everything recorded)
- ✅ Extensible — swap classifier without touching state machine

### Negative
- ❌ Stage 1 classifier false-positive rate caps tenant experience
- ❌ Human review queue requires admin UI (Wave 8 Admin Console)
- ❌ Keyword ban list becomes operational burden (requires curation)

## Alternatives

- **A. Human review on every publication** — rejected: unscalable, latency kills UX
- **B. No moderation, rely on tenant responsibility** — rejected: legal exposure
- **C. Third-party moderation API (AWS Rekognition, Azure Content Moderator)** — deferred: vendor lock-in; scaffolding in 4.1 decouples the check, can swap later

## Implementation Notes

`ContentModerationService.check(BrandingResource) → ModerationResult`.
Wired into `PublishPackageStep` — blocks `DEPLOY` if result is REJECTED or NEEDS_HUMAN_REVIEW.

Config keys:
- `moderation.stage1.enabled` (default true)
- `moderation.stage1.nsfw-threshold` (0.0–1.0, default 0.7)
- `moderation.stage2.auto-fallback-to-template` (default true)

## References
- GAP-018
- ai-branding-guidelines.md §5 (Quality Gate)

## Log
- 2026-04-14 — Accepted
