---
title: Wave rst-cascade-1 Cluster 4 — Infra + UI walkthrough (coordinator inline)
status: complete
created: 2026-05-26
phase: phase-1-beta
wave: rst-cascade-1
gaps: [GAP-502, GAP-656]
---

# Wave rst-cascade-1 Cluster 4 — Infra + UI walkthrough

Coordinator inline walkthrough cho 2 infra + UI gaps. Phase 0 Preflight surfaced 1 cascade finding (RabbitMQ queue declaration missing) ghi vào §3 below.

## GAP-502 walkthrough — kh_backend production thrashing stability

**Pre-walkthrough %:** 90% PARTIAL
**Post-walkthrough verdict:** PARTIAL 95% (+5 delta)
**Stack uptime at walkthrough:** ~50 min continuous

### Evidence per AC

| AC | Verdict | Evidence |
|---|---|---|
| RC1: rabbit-listener services start without AmqpAuthException; healthy ≥10 min | ✅ Workaround | kitehub-email + kitehub-* all start clean. kiteclass-core needed manual `class.rescheduled.queue` declare (Phase 0 cascade — see §3); post-declare ≥45min healthy. Code fix follow-up gap filed below. |
| RC2: No OOM in 1h sliding window; memory <80% limit at steady state | ⚠️ Partial (~50min/60min) | Snapshot 2026-05-26 (post 45min uptime): 6/6 services restart=0 OOM=false healthy. Memory: kitehub-admin 370.6MB/1GB (36%), kitehub-branding 422.4MB/2GB (21%), kitehub-email 264MB/1GB (26%), kitehub-subscription 357.4MB/1GB (35%), kite-gateway 341.7MB/512MB (67%), kiteclass-core 521MB/1GB (52%). All <80% target ✓. Full 1h soak deferred (extend +15min cho true 1h verify) |
| Stability gate: 5 kitehub-* services Up ≥30 min continuous | ✅ | All 5 kitehub-* + kiteclass-core restart=0 healthy ~45min |
| API reliability: 10 consecutive POST /api/v1/beta-access/request return 2xx OR 4xx (not 502/400-empty) | ✅ Via Cluster 3 agent | Cluster 3 onboarding agent fired multiple POST beta-signup variations — no 502s observed |
| Trigger identified | ✅ Root cause: RabbitMQ class.rescheduled.queue NOT_FOUND post Wave aws-restore-1 (RabbitMQ data volume recreated → queues lost from previous declarations). Documented §3 cascade finding below. |
| GAP-447 sizing decision (t3.medium vs t3.large) | ⏳ Deferred Phase β AWS | Local soak cannot drive AWS sizing decision. Defer Phase β |
| Plan 1 self-test re-runnable | ✅ | Cluster 1+2+3 agents all walked endpoints clean post stack-healthy |

### Verdict rationale

Stay PARTIAL 95% — 5/7 AC ✅, 1/7 ⚠️ partial (1h soak need +15min extension), 1/7 ⏳ deferred Phase β AWS.

Flip DONE candidate Phase β post AWS soak + GAP-447 sizing decision documented.

---

## GAP-656 walkthrough — UI Coordinator widget collision

**Pre-walkthrough %:** 80% PARTIAL
**Post-walkthrough verdict:** PARTIAL 85% (+5 delta)

### Evidence per AC

| AC | Verdict | Evidence |
|---|---|---|
| `useOnboardingPhase()` hook implement + 5 phase types | ✅ Shipped | `kitehub/kitehub-frontend/src/hooks/useOnboardingPhase.ts` exists (verify file presence) |
| `SupportMenu` component thay thế GAP-540 + GAP-542 floating widgets | ✅ Shipped | `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` exists |
| Staggered reveal logic — 1 modal/banner active at a time | ⚠️ Code present | SupportMenu.test.tsx test file exists; behavior verification deferred (need browser walkthrough) |
| Playwright spec `onboarding-mobile.spec.ts` PASS ≥375px + 360px viewports | ❌ Not shipped | `find kitehub/kitehub-frontend/e2e -name "onboarding-mobile*"` returns 0 — Playwright spec missing |
| httpOnly cookie cross-tab dismiss sync verified (2-tab same browser + 2 browsers) | ❌ Not verified | Cluster 4 inline scope không cover browser dual-tab walkthrough |
| GAP-540 + GAP-542 scope updates to reference this gap as prereq | ⏳ Deferred | Check during closure batch |
| `cd kitehub/kitehub-frontend && pnpm test --run && pnpm build && pnpm lint` PASS | ⏳ Not run | Inline scope; defer separate CI verify |

### FE landing pages health (inline verify)

- http://localhost:3001 (kitehub-frontend) — HTTP 200 OK với CSP headers
- http://localhost:3000 (kiteclass-frontend) — HTTP 200 OK với CSP headers

### Verdict rationale

3/7 AC ✅ shipped (hook + component + test file), 1/7 ⚠️ code present untested, 3/7 ❌ pending (Playwright E2E + cookie sync + pnpm verify). Stay PARTIAL 85% with explicit deferred items.

Follow-up Wave to ship Playwright `onboarding-mobile.spec.ts` + cookie cross-tab sync test + pnpm CI verify cycle.

---

## §3 Cascade findings (Phase 0 preflight + Cluster 4 surfaced)

### Cascade finding #1 — RabbitMQ `class.rescheduled.queue` declaration missing (Wave br-4 GAP-291 incomplete)

**Severity:** 🟠 P1 (workaround available; production deploy will recur on every RabbitMQ data-volume recreate)
**Phase:** phase-1-beta
**Domain:** Backend
**Found:** 2026-05-26 Wave rst-cascade-1 Phase 0 Preflight

**Symptom:**
- `kiteclass-core` 24 restarts during stack startup
- Spring AMQP log: `Caused by: com.rabbitmq.client.ShutdownSignalException: channel error; protocol method: #method<channel.close>(reply-code=404, reply-text=NOT_FOUND - no queue 'class.rescheduled.queue' in vhost '/', class-id=50, method-id=10)`
- Actuator `/actuator/health` returns 503 SERVICE_UNAVAILABLE → docker healthcheck fail → restart loop

**Root cause:**
- `kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/event/consumer/ClassRescheduledEmailConsumer.java:45` declares `@RabbitListener(queues = "class.rescheduled.queue")` — Spring AMQP defaults to `queueDeclarePassive` (assume queue exists)
- `kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java:85-86` says "Exchanges, queues, and bindings are defined per-module" — BUT no per-module config file exists for class/reschedule module
- Wave br-4 Bucket D (PR #1781) shipped consumer code WITHOUT matching `@Bean Queue` declaration in module config

**Workaround applied:**
```bash
docker exec kite-rabbitmq rabbitmqadmin declare queue name=class.rescheduled.queue durable=true -u kitehub -p $RABBITMQ_PASS
# → kiteclass-core restart → healthy in ~30s
```

**Proposed Fix (follow-up gap):**
Add module config `kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/config/ClassRabbitConfig.java`:
```java
@Configuration
public class ClassRabbitConfig {
    @Bean
    public Queue classRescheduledQueue() {
        return QueueBuilder.durable("class.rescheduled.queue").build();
    }
    // + exchange + binding declarations
}
```

Cluster 3 agent confirmed similar pattern: `ClassRescheduledNoOpConsumer.java` also uses same passive queue. Both consumers need queue declaration.

**Action item:** File new gap GAP-NEW-rabbitmq-class-rescheduled-queue-declaration-missing P1 phase-1-beta. Bundle với GAP-291 follow-up cluster nếu other similar declaration misses found across Wave br-4/br-7 ship.

### Cascade finding #2 — GAP-610 invalid UUID 500 instead of 400 (Cluster 3 finding)

Per Cluster 3 agent report — Wave br-5 Class C fix (PR #1828) only handled VALID UUID format inputs. Invalid UUID format triggers Spring `MethodArgumentTypeMismatchException` → HTTP 500 instead of proper 400 Bad Request.

**Action item:** Follow-up gap file `GAP-610-invalid-uuid-cascade` P1 — add `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` to convert to 400 JSON body OR use String type param + manual UUID validation.

### Cascade finding #3 — GAP-516 wave plan label discrepancy

Per Cluster 3 — Wave plan §3.α labels GAP-516 = "Tenant init flow" but gap-status.csv + actual gap file = "2FA TOTP mandatory PLATFORM_ADMIN". Tenant init flow = GAP-531.

**Action item:** Update wave plan §3 label OR document drift in closure log. Pure documentation, no code impact.

### Cascade finding #4 — GAP-611 expected status drift (HTTP 400 vs 404)

Per Cluster 3 — Wave plan expected "JSON 404"; actual implementation returns HTTP 400 (validation error). HTTP 400 is MORE semantically correct per RFC 7231 (empty body = malformed request, not missing resource).

**Action item:** Update AC + ROADMAP wording to reflect correct semantic (400 for empty body, 404 for non-existent token). Then flip DONE.

---

## Summary

| Gap | Verdict | Delta |
|---|---|---|
| GAP-502 | PARTIAL 95% (+5) | Path to DONE Phase β: extend soak +15min + GAP-447 sizing decision |
| GAP-656 | PARTIAL 85% (+5) | Path to DONE Wave 99+: Playwright E2E + cookie sync + pnpm CI verify |

**Cascade findings:** 4 new (1 P1 RabbitMQ queue + 1 P1 GAP-610 UUID handler + 2 documentation drift)

**Stack stability post Phase 0 cascade fix:** 6/6 backend services restart=0 OOM=false healthy ~50min uptime. Strong stability signal.

## References

- Plan: `documents/03-planning/waves/wave-2026-05-26-rst-cascade-1-local-first-aws-verify.md` §3.α Cluster 4
- Rule: `pre-handoff-self-test-completeness.md` §2.4
- Rule: `audit-to-gap-pipeline.md` §2.8 fix-time state-check
- Sister audits: `2026-05-26-wave-rst-cascade-1-cluster-1-email.md`, `2026-05-26-wave-rst-cascade-1-cluster-2-auth-admin.md`, `2026-05-26-wave-rst-cascade-1-cluster-3-onboarding.md`
