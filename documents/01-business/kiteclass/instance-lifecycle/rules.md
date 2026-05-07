# Instance Lifecycle — Business Rules

**Domain:** instance-lifecycle
**Source:** GAP-009, ADR-004

## Rules

### FrontendInstance
| ID | Rule |
|----|------|
| BR-INST-001 | slug unique per tenant (when deleted=false) |
| BR-INST-002 | status transitions enforced by state machine; direct mutation forbidden |
| BR-INST-003 | retry count auto-increments on FAILED; retry blocked after MAX_RETRIES (default 3) |
| BR-INST-004 | brandingVersion increments on every successful DEPLOY transition |
| BR-INST-005 | deployed_at / failed_at / last_regenerate_at are immutable once set by the state machine |

## State Machine

```
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED ⇄ REGENERATING
                    ↓              ↓          ↑
                  FAILED ←───── FAILED ───────┘ (retry from FAILED → INITIALIZING)
```

### Allowed transitions

| From | Allowed targets |
|------|-----------------|
| NOT_STARTED | INITIALIZING |
| INITIALIZING | GENERATING, FAILED |
| GENERATING | DEPLOYED, FAILED |
| DEPLOYED | REGENERATING |
| REGENERATING | DEPLOYED, FAILED |
| FAILED | INITIALIZING |

Any other transition → `IllegalStateException`.

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `instance.lifecycle.max-retries` | 3 | Max retry attempts before abandoning |
| `instance.lifecycle.retry-backoff-seconds` | 60 | Delay between auto-retries (future) |

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — tenant lifecycle states; no PII surface (tenant metadata only). Cross-reference `kitehub/data-retention/rules.md` for tenant-data deletion compliance.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Lifecycle state added/removed, off-boarding regulation change.

## Log
- 2026-04-14 — Initial rules (GAP-009, ADR-004)
