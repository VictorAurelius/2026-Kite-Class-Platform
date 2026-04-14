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

## Log
- 2026-04-14 — Initial rules (GAP-009, ADR-004)
