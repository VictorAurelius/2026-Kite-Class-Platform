# GAP-390: Wave 34 API P1 cluster — tenantId hardcoded null + SSE assertions missing + path param type mismatch

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟠 P1 cluster (3 sub-issues — API hardening, ship after P0 GAP-272n already filed Wave 34)
**Domain:** Backend / API Contract
**Found:** 2026-05-07 (API Contract /100 audit Wave 34 — agent ad28b70c)
**Affects:** `kitehub-branding` BrandingJobResponse + DeployStreamControllerTest + api-contract.md

## Problem (3 sub-issues)

### 390-A: `tenantId` hardcoded null trong BrandingJobResponse
- `BrandingJobResponse.from()` line 54 sets `tenantId=null`
- Contract `api-contract.md` requires UUID
- Comment notes "blocked on Bucket C FrontendInstance integration"
- Bucket C đã ship (PR #908 InstanceLifecycleService) — dependency NOW available; chỉ cần wire

### 390-B: SSE event assertions missing trong DeployStreamControllerTest
- Test validates emitter lifecycle (open/close)
- KHÔNG validate streaming event payloads: `state-change`, `progress`, `complete`, `log`
- Risk: SSE shape drift sẽ pass tests + break FE silently
- Contract đặc tả 4 event types nhưng chỉ emitter lifecycle tested

### 390-C: api-contract.md numeric `instanceId` example mismatch UUID actual
- Contract shows `/api/v1/branding/instances/12345/...` (numeric example)
- Implementation uses UUID
- Confusion cho future reader; doc-only fix

## Proposed Fix

### 390-A
```java
// BrandingJobResponse.java (or BrandingJobResponseFactory)
public static BrandingJobResponse from(BrandingJob job, FrontendInstance instance) {
    return new BrandingJobResponse(
        job.getId(),
        instance.getTenantId(),  // wire from Bucket C
        // ...
    );
}
```
Update all callers (controller + tests) để pass FrontendInstance qua Spring Data lookup.

### 390-B
Extend `DeployStreamControllerTest`:
```java
@Test
void deployStreamEmitsExpectedEvents() {
    // Subscribe + collect events
    List<ServerSentEvent<?>> events = subscribe(jobId);
    assertThat(events).extracting(ServerSentEvent::event)
        .containsExactly("state-change", "progress", "progress", "complete");
    assertThat(events.get(0).data()).asString().contains("\"state\":\"INITIALIZING\"");
    // ... validate payload shapes per contract
}
```

### 390-C
Update `documents/01-business/kitehub/ai-branding/api-contract.md`:
- Replace numeric `12345` example với UUID `550e8400-e29b-41d4-a716-446655440000`
- Add note: "Path params are UUID v4 strings; numeric examples in legacy docs are for illustration only"

## Acceptance Criteria

- [x] **390-A**: BrandingJobResponse populates tenantId from request scope (MDC `tenantId` populated by gateway tenant filter); unit tests verify both populated + null-fallback paths. Note: `FrontendInstance` referenced in original gap text does not exist in this module — state-check (`audit-to-gap-pipeline.md` §2.5) found 0 occurrences across `kitehub/`. Wired from MDC instead, which is the request-scoped tenant identifier per `logback-spring.xml` MDC keys.
- [x] **390-B**: SSE test asserts 4 contract event types (`state-change` / `progress` / `complete` / `error`) + payload field shapes (`from`/`to`/`ts`, `finalStatus`/`jobId`, `errorCode`/`retryable`/`message`). 4 new payload-assertion tests added via `Mockito.mockConstruction(SseEmitter.class)` to capture `send()` calls.
- [x] **390-C**: api-contract.md numeric IDs → UUID examples (5 occurrences replaced + identifier note added at doc top).

## Out-of-scope (track separately)

| Item | Where |
|---|---|
| API Contract /100 audit re-run + delta verification (target ≥85/100 from 72 baseline) | Post-wave audit per `post-wave-audit-mandate.md` §2.1 — runs within 3 days of Wave 36 closure (separate PR) |
| Persistent `tenantId` column on `BrandingJob` / lifecycle entities (avoids relying on MDC) | Future schema work — not blocking; MDC-sourced value is correct for request-scoped responses |

## Related

- Source audit: `documents/04-quality/audits/api/2026-05-07-wave-34-ai-branding-api-contract.md` (Findings #2, #3, #4)
- Sister gap: GAP-272n (POST /regenerate response shape — P0 BLOCKING, ship first)
- Sister gap: GAP-272o (lifecycle orchestrator wire — P1)
- Parent: Wave 34 closure (PR #911)

## Log

- **2026-05-07** Filed from API Contract /100 audit Wave 34. State-check: GAP-272n covers regenerate shape only; tenantId null + SSE assertions + path param doc are NEW findings not covered by 272n/272o. Bundled per `audit-to-gap-pipeline.md` §3 P1 cluster pattern.
- **2026-05-07** Wave 36 Bucket B shipped (PR #PENDING). 390-A wired tenantId via `MDC.get("tenantId")` in `BrandingJobV1Controller`; `BrandingJobResponse.from(...)` gained a 3-arg overload accepting tenantId (legacy 2-arg overload retained, calls 3-arg with null). 390-B added 4 payload-assertion tests using `Mockito.mockConstruction` to intercept `SseEmitter.send()` and verify event names + JSON field shapes. 390-C replaced 5 numeric `12345` examples with UUID `550e8400-e29b-41d4-a716-446655440000` and added a clarifying identifier note at the top of `api-contract.md`. Verification: `./mvnw -pl kitehub-branding test -Dtest='*DeployStream*,*BrandingJobResponse*,*BrandingJobV1*'` clean. Re-audit /100 deferred to post-wave audit (separate PR per `post-wave-audit-mandate.md`) — see Out-of-scope.
