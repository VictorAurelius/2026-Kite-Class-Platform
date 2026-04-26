# Design Pattern Audit — Scoring Guide

Detailed rubric for each of 5 anti-pattern categories. Each category 20 pts; total /100.

Reference: `.claude/rules/design-patterns.md` §3 (BANNED anti-patterns) + §1.1 YAGNI principle.

---

## Category 1 — God Service / Class (20 pts)

Threshold: any `*.java` file in `src/main/java/**` exceeding 500 LOC = refactor required (`design-patterns.md` §3.1).

| Module state | Pts |
|--------------|:---:|
| All services <400 LOC | 20 |
| All services <500 LOC, but ≥1 service in 400-500 range | 16 |
| 1 service in 500-600 range | 10 |
| 1 service >600 LOC OR 2+ services 500-600 | 5 |
| Multiple services >600 OR 1 service >800 | 0 |

**Excluded from count:**
- Test files (`*Test.java`)
- Generated code (Lombok, MapStruct outputs)
- DTOs / Records (no logic)
- Configuration classes (`@Configuration` / `@ConfigurationProperties`)

**Recovery suggestion:** Facade Pattern split — extract sub-services by capability axis. Original Facade keeps public API; sub-services become private collaborators.

---

## Category 2 — Status Switch / If Cascade (20 pts)

Detect: `if (.*Status ==` or `switch (.*Status)` patterns indicating polymorphism opportunity for State Pattern.

| Status switch density | Pts |
|----------------------|:---:|
| 0 cascades in non-test code | 20 |
| 1-3 cascades, all small (<5 cases each) | 16 |
| 4-10 cascades OR ≥1 cascade with ≥5 cases | 10 |
| 10-30 cascades OR repeated cascade in 3+ files | 5 |
| >30 cascades OR cascade replicated across services | 0 |

**Excluded:**
- `switch` on String enum without state semantics (e.g. parsing route paths)
- Single-case if-checks (`if (status == ACTIVE) return ...`) — not cascades
- Test setup (`given().status(...)` builders)

**Recovery suggestion:** State Pattern per `design-patterns.md` Mandatory Patterns matrix — `entity.transition(event)` with state classes implementing `next(event)`. Reserve only for entities with ≥3 lifecycle states + meaningful invariants per state.

---

## Category 3 — Primitive Obsession (20 pts)

Detect: public method signatures or entity fields using primitives (String, int) for domain concepts that have validation rules.

| Primitive obsession sites | Pts |
|--------------------------|:---:|
| 0 sites in domain layer | 20 |
| 1-3 sites, all in DTOs at boundary | 16 |
| 4-10 sites mixed across domain + DTOs | 10 |
| Pervasive use of String for color/email/phone in domain | 5 |
| No value objects at all in codebase | 0 |

**Watch list (require value objects per ai-branding-guidelines.md):**
- Color hex → `ThemeColor` (validated `#[0-9A-Fa-f]{6}`)
- Email → `Email` value object (RFC 5322 + scrubber-safe)
- Vietnamese phone → `Phone` (10-11 digit `0\d{9,10}`)
- Money amounts → `Money(amount, currency)` not `BigDecimal` alone
- TenantId / InstanceId → typed wrappers, not raw `UUID`

**Excluded:**
- Boundary-layer DTOs translating from JSON (REST controllers) — accepted
- Records used as parameter clusters (not domain primitives)

**Recovery suggestion:** Introduce `@Value` records or POJOs with validation in constructor. See `design-patterns.md` §3.2 example.

---

## Category 4 — Leaky Abstraction (20 pts)

Detect: external API types (Ollama / OpenAI / vendor responses) appearing OUTSIDE adapter packages.

| Leaky type sites | Pts |
|-----------------|:---:|
| 0 sites in domain layer | 20 |
| 1-2 sites in clearly-named adapter classes (acceptable) | 16 |
| 3-5 sites scattered, but adapter pattern exists for ≥1 vendor | 10 |
| Vendor types in service / domain code | 5 |
| No adapter layer; vendor types everywhere | 0 |

**Watch list:**
- `OllamaResponse`, `OllamaRequest`, `OllamaContext` — must stay inside `*ollama*` adapter package
- `OpenAIResponse`, `ChatCompletion` — same, OpenAI adapter only
- Vendor-specific exception classes — wrap to domain `AIException` at boundary
- HTTP framework types (`ResponseEntity`, `WebClient.Response`) outside controllers

**Excluded:**
- Adapter / ACL packages (`*adapter*`, `*acl*`, `*client/external*`)
- Spring Boot framework types (`ResponseEntity`, `Pageable`) in controllers — expected

**Recovery suggestion:** Adapter Pattern + Anti-Corruption Layer. Domain code consumes `AIClient` (interface returning domain types); adapter implementation translates vendor responses.

---

## Category 5 — Direct Event Publish (No Outbox) (20 pts)

Detect: `rabbitTemplate.convertAndSend` / `streamBridge.send` outside the `*outbox*` package.

| Direct publish sites | Pts |
|----------------------|:---:|
| 0 — all events through `OutboxEventWriter` | 20 |
| 1-2 sites (legacy, not yet migrated) | 16 |
| 3-5 sites mixed with outbox usage | 10 |
| Half of services bypass outbox | 5 |
| No outbox infra; all events direct-publish | 0 |

**Watch list:**
- `rabbitTemplate.convertAndSend(...)` — must go through outbox
- `streamBridge.send(...)` — same
- Spring `ApplicationEventPublisher.publishEvent(...)` — acceptable for in-process events, NOT cross-service
- Direct Kafka `KafkaTemplate.send()` — same as RabbitMQ rule

**Excluded:**
- `OutboxEventPublisher` itself (it's the publisher; flagging it would be circular)
- `LoggingEventDispatcher` (test/dev path)
- In-process `@EventListener` patterns (Spring lifecycle events)

**Recovery suggestion:** Outbox Pattern per `kiteclass-core/common/outbox/`. Replace direct publish with `outboxEventWriter.write(event)` inside the `@Transactional` method; the `OutboxEventPublisher` worker drains async.

---

## Final Score Interpretation

| Range | Grade | Interpretation |
|-------|:-----:|----------------|
| 90-100 | A | Pattern-disciplined codebase; Wave 7+ refactor optional |
| 75-89 | B | Most patterns applied; 1-2 hotspots warrant gaps |
| 60-74 | C | Partial application; 3-5 hotspot gaps + targeted refactors |
| 45-59 | D | Significant anti-pattern presence; wave-scale refactor justified |
| <45 | F | Foundation rework needed; consider quarter-scale architecture initiative |

First-run baselines for never-audited categories typically land in C-D range per memory `feedback_audit_calibration.md` — first low score is honest baseline, not regression.

---

## Calibration Adjustments

Apply ONLY if first-run baseline shows systematic skew (e.g., all modules cluster in 400-500 LOC range due to legitimate-large legacy services).

| Symptom | Adjustment |
|---------|-----------|
| All modules 450-550 LOC (legacy noise floor) | Raise God-Service threshold to 600 for that audit cycle; document in audit report |
| Status switches concentrated in 1 lifecycle entity (already known State refactor candidate) | Score the entity at 5pts but don't penalize other categories |
| Vendor types appear in test fixtures only | Re-grep with `--include="*.java"` excluding `**/test/**` |

Document any calibration in audit report header so subsequent audits compare like-for-like.
