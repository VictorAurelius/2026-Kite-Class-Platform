# Tenant Provisioning — API Contract

> Internal Java SPI. REST / event-listener wiring lands alongside the outbox RabbitMQ dispatcher in a follow-up Sub-PR.

## TenantCreatedEvent

```java
@Value
@Builder
public class TenantCreatedEvent {
    String tenantId;
    String slug;
    String audience;
    String tone;
}
```

## TenantProvisioningSaga

### provision(TenantCreatedEvent) → Long
Returns the persisted `FrontendInstance` id on success.

**Happy-path sequence:**
1. `lifecycle.initiate(tenantId, slug)` — NOT_STARTED → INITIALIZING
2. `provisionInfrastructure` (stub)
3. `lifecycle.markInfrastructureReady(id)` — INITIALIZING → GENERATING
4. Analyzer → Planner → PlanExecutor
5. Last Step transitions DEPLOYED + evicts branding package cache

**Exceptions:**
| Thrown | When | Compensation? |
|--------|------|---------------|
| IllegalArgumentException (slug in use) | initiate throws | No (no row yet) |
| StepException | plan step fails unrecovered | Yes: markFailed(id, message) |
| RuntimeException | analyzer / planner / infra stub fails | Yes: markFailed(id, message) |

## Future integrations

| Trigger | Wiring |
|---------|--------|
| RabbitMQ queue `tenant.created` | MessageListener calls saga.provision |
| Spring @EventListener(TenantCreatedEvent) | In-proc alternative |
| REST fallback `POST /internal/provision` | Ops manual kick |
| `@Scheduled` retry | FAILED rows with retryCount<MAX |

## Log
- 2026-04-14 — Initial contract
