# Content Moderation — Business Rules

**Domain:** content-moderation
**Source:** GAP-018, ADR-010, Wave 4 Sub-PR 4.1

## Rules

### Moderation lifecycle (State Machine)
| ID | Rule |
|----|------|
| BR-MOD-001 | Status transitions enforced by `ModerationStatus` machine (PENDING → APPROVED / REJECTED / NEEDS_HUMAN_REVIEW; NEEDS_HUMAN_REVIEW → APPROVED / REJECTED) |
| BR-MOD-002 | APPROVED and REJECTED are terminal — no further mutation |
| BR-MOD-003 | At most ONE non-terminal row per `(targetType, targetId)` at any time (caller checks before insert) |
| BR-MOD-004 | Every transition MUST write an `AuditLog` row in the same transaction (BR-AUDIT-001) |
| BR-MOD-005 | `flaggedKeywords` serialized as JSON array in `moderation_queue.flagged_keywords` (jsonb) |

### 3-stage pipeline (ADR-010)
| ID | Rule |
|----|------|
| BR-MOD-010 | **Stage 1** — `ContentModerationService.check()` runs the NSFW/keyword pre-check synchronously; ≥95% traffic must land here without escalation |
| BR-MOD-011 | **Stage 2** — on Stage-1 REJECTED, the CALLER decides whether to retry with TEMPLATE category only; the service only signals REJECTED + reason |
| BR-MOD-012 | **Stage 3** — NEEDS_HUMAN_REVIEW rows persist to `moderation_queue` and wait for Stage X admin UI (follow-up wave) |
| BR-MOD-013 | Score threshold: `score ≥ nsfw-threshold` → REJECTED; `nsfw-threshold - 0.2 ≤ score < nsfw-threshold` → NEEDS_HUMAN_REVIEW; else APPROVED |

### Classifier stub (Sub-PR 4.1 scope)
| ID | Rule |
|----|------|
| BR-MOD-020 | Stage 1 implementation in 4.1 is a deterministic keyword-score stub; no real ML dependency |
| BR-MOD-021 | Banned-keyword default list: `nsfw, nude, porn, sex, violence, gore, hate, slur, drug, weapon, terror` |
| BR-MOD-022 | Score formula: `min(1.0, hits / max(3, token-count))`, boosted to ≥0.85 on ≥2 hits, ≥0.55 on 1 hit |
| BR-MOD-023 | Real classifier (NSFWJS / AWS Rekognition / Azure) swapped in later by replacing `runStage1()` only — no API changes |

### Integration with AI Branding pipeline
| ID | Rule |
|----|------|
| BR-MOD-030 | `PublishPackageStep` (Wave 3 Sub-PR 3.5) MUST call `ContentModerationService.check()` before flipping instance to DEPLOYED |
| BR-MOD-031 | If result is REJECTED and `moderation.stage2.auto-fallback-to-template=true`, caller retries pipeline with TEMPLATE category; else marks instance FAILED |
| BR-MOD-032 | If result is NEEDS_HUMAN_REVIEW, instance stays in GENERATING (or equivalent pre-deploy state) until admin decision |
| BR-MOD-033 | Wiring into `PublishPackageStep` lands in Sub-PR 4.5 Quality Gate (ADR-010 Implementation Notes) |

### Audit actionType catalogue
| ID | actionType | Emitted when |
|----|-----------|--------------|
| BR-MOD-040 | `moderation.approved` | Stage 1 passes |
| BR-MOD-041 | `moderation.rejected` | Stage 1 auto-rejects |
| BR-MOD-042 | `moderation.needs_human_review` | Stage 1 borderline score |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `moderation.stage1.enabled` | `true` | Master switch. When `false`, every call returns APPROVED (dev / CI). |
| `moderation.stage1.nsfw-threshold` | `0.7` | Score at or above → REJECTED. Review band = `threshold − 0.2`. |
| `moderation.stage2.auto-fallback-to-template` | `true` | Hint surfaced in logs and forwarded via reason; caller owns the retry. |

## Migration reservation

| Version | Purpose |
|---------|---------|
| V36 | `moderation_queue` table (this Sub-PR) |

## Log
- 2026-04-15 — Initial rules (Wave 4 Sub-PR 4.1, GAP-018, ADR-010)
