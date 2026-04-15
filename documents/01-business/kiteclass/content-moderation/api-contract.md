# Content Moderation — API Contract

> Sub-PR 4.1 exposes the service-level contract only. REST endpoints for the admin moderation
> queue are planned for a later wave (admin console).

## ContentModerationService

### check(targetType, targetId, text, imageUrl) → ModerationResult
- Runs the 3-stage pipeline (ADR-010) synchronously.
- Always returns a non-null `ModerationResult` with a terminal-or-escalated status (`APPROVED`, `REJECTED`, `NEEDS_HUMAN_REVIEW` — never `PENDING`).
- Always writes one `AuditLog` row (`moderation.{approved|rejected|needs_human_review}`) in the same transaction.
- Persists a `ModerationQueue` row ONLY for non-APPROVED outcomes (admin dashboard / audit trail).
- Never throws on malformed input — null / blank `text` and `imageUrl` are treated as "no signal, approve".

**Parameters:**
| Name | Type | Required | Notes |
|------|------|:--------:|-------|
| `targetType` | String (≤100) | yes | Logical category, e.g. `branding.logo`, `branding.banner` |
| `targetId` | String (≤100) | yes | Caller-owned identifier |
| `text` | String | no | Prompt or output text to scan |
| `imageUrl` | String | no | Currently metadata-only in stub; real classifier plugs in here |

**Return:** `ModerationResult` value object — `status`, `score (0..1)`, `flaggedKeywords`, `reason`.

## Value objects

### ModerationResult (immutable)
```java
ModerationResult {
  ModerationStatus status;     // APPROVED | REJECTED | NEEDS_HUMAN_REVIEW
  double score;                // [0.0, 1.0]
  List<String> flaggedKeywords;
  String reason;               // "stage1.pass" | "banned-keyword: ..." | "borderline-score" | "stage1.disabled"
}
```

## Entity

### ModerationQueue
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| instance_id | UUID | multi-tenant |
| target_type | VARCHAR(100) | see rules.md |
| target_id | VARCHAR(100) | caller-owned |
| status | VARCHAR(32) | CHECK constraint: PENDING/APPROVED/REJECTED/NEEDS_HUMAN_REVIEW |
| score | DOUBLE PRECISION | |
| flagged_keywords | JSONB | array of strings |
| reason | VARCHAR(500) | |
| assigned_reviewer_id | BIGINT | set when admin takes ownership |
| decided_at | TIMESTAMP | set on transition to terminal state |
| + BaseEntity audit cols | | created_at / updated_at / created_by / updated_by / version / deleted |

## Repository

```java
List<ModerationQueue> findByStatusAndDeletedFalse(ModerationStatus status);
Optional<ModerationQueue> findFirstByTargetTypeAndTargetIdAndDeletedFalse(String targetType, String targetId);
```

## Error contract (service layer)

| Condition | Exception |
|-----------|-----------|
| Invalid state transition (e.g. re-approving a terminal row) | `IllegalStateException` (from `ModerationQueue.transitionTo`) |
| Null `targetType` or `targetId` | NPE at repository layer (validation is a caller concern — controller will add `@NotBlank` in future REST sub-PR) |

## Future REST surface (follow-up wave)

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/v1/admin/moderation/queue?status=NEEDS_HUMAN_REVIEW | List pending reviews |
| POST | /api/v1/admin/moderation/queue/{id}/approve | Transition NEEDS_HUMAN_REVIEW → APPROVED |
| POST | /api/v1/admin/moderation/queue/{id}/reject | Transition NEEDS_HUMAN_REVIEW → REJECTED (reason required) |
| POST | /api/v1/admin/moderation/queue/{id}/assign | Claim a row (`assigned_reviewer_id`) |

Error codes (future):
- 400: missing reason on reject
- 404: queue id not found
- 409: concurrent decision (optimistic lock)
- 422: attempt to mutate terminal row

## Log
- 2026-04-15 — Initial contract (Wave 4 Sub-PR 4.1)
