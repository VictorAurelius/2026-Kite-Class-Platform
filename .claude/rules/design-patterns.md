---
paths:
  - "**/*.java"
  - "**/*.tsx"
  - "**/*.ts"
---

# Design Patterns — Project Rules

**Priority:** 🟠 MANDATORY
**Version:** 1.5.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-05-25
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.5.0 MINOR self-approve per `rule-change-process.md` §5; adds §3.12 "Entity-Migration-Mapper triad drift" anti-pattern, paired same-PR with new CI script `scripts/check-entity-mapper-consistency.sh` + workflow job `entity-mapper-consistency` in `quality-code.yml` per §6.5 Enforcement Parity Mandate; no constraint loosening for prior work; existing entities grandfathered, anti-pattern applies prospectively. Closes GAP-743 (Wave meta-1 Bucket D). v1.4.0 (kept): adds §3.11 audit/log service @Transactional. v1.3.1 (kept): thêm `paths:` frontmatter. v1.3.0 (kept): MINOR self-approve per §5)
**Applies to:** All `*.java` and `*.tsx`/`*.ts` source under `kiteclass/**`, `kitehub/**`, plus PR review / refactor decisions

Project-wide rules for applying design patterns. **Mandatory** khi develop new feature, refactor, review PR.

Reference catalog: `documents/02-architecture/ai-branding-design-patterns.md` (AI Branding-specific)
Skill helper: `.claude/skills/reference/design-pattern-advisor.md`
Audit skill: `.claude/skills/quality/design-pattern-audit/SKILL.md` (enforces §3 anti-pattern list)
Rule-change governance: `.claude/rules/rule-change-process.md`

---

## 1. Core Principles

### 1.1 Apply Patterns WHERE They Fit (Not Everywhere)

**YAGNI (You Aren't Gonna Need It):**
- Simple case → direct code
- 1 implementation unlikely to change → no Strategy
- Few states với clear transitions → enum with method OK
- Single consumer → no pub-sub

**Rule:** Pattern justified if:
- ≥2 implementations (actual or imminent)
- Clear variation/change axis
- Complexity worth flexibility

### 1.2 Standard Names Only

Use canonical GoF / enterprise pattern names. Don't invent "custom patterns".

✅ `AIClientStrategy`, `InstanceStateMachine`, `BrandingFacade`
❌ `AIProviderManager`, `StatusHandler`, `BrandingCoordinator` (unclear)

### 1.3 Document Pattern Choice

Every pattern application has javadoc:
```java
/**
 * Strategy Pattern — swap AI provider via config.
 *
 * Implementations: OllamaClient, OpenAIClient, BedrockClient
 * Selected by: ai.provider property (AIProviderConfig)
 */
public interface AIClient { ... }
```

---

## 2. Mandatory Patterns per Context

| Context | Required Pattern | Rationale |
|---------|-----------------|-----------|
| **Multiple implementations (AI providers, payment gateways, storage backends)** | Strategy | Swap via config |
| **Entity with finite lifecycle states** | State Machine | Enforce transitions |
| **Pipeline of steps with retry/undo** | Command + Composite | Composability |
| **Service orchestrating ≥3 dependencies** | Facade | Simplified API |
| **External API vendor-specific** | Adapter + ACL | Vendor isolation |
| **Cross-cutting (retry, logging, metrics)** | Decorator | Clean separation |
| **Event publishing với DB txn** | Outbox | Reliable delivery |
| **Distributed multi-service txn** | Saga | Compensation rollback |
| **External call flaky** | Circuit Breaker + Bulkhead + Retry | Resilience |
| **Request routing with fallback** | Chain of Responsibility | Extensible |
| **Complex FE state flow (wizards, forms)** | XState State Machine | Testable |
| **Legacy code replacement** | Strangler Fig | Incremental migration |

---

## 3. Anti-Patterns BANNED

PR sẽ bị reject nếu có:

### 3.1 God Service/Class
```
❌ BAD: AIBrandingService với 25 public methods, 800 lines
✅ GOOD: BrandingFacade (5 methods) + Analyzer + Planner + Executor + ...
```

**Threshold:** Service >15 methods hoặc >500 lines = refactor required.

### 3.2 Primitive Obsession
```
❌ BAD:
  public void setPrimaryColor(String color) { ... }  // "#2563eb" unvalidated

✅ GOOD:
  public void setPrimaryColor(ThemeColor color) { ... }

  @Value
  class ThemeColor {
    String hex;  // validated in constructor
    public ThemeColor(String hex) {
      if (!hex.matches("^#[0-9A-Fa-f]{6}$")) throw new IllegalArgumentException();
      this.hex = hex;
    }
  }
```

### 3.3 Status/Type Switch Statements
```
❌ BAD:
  if (status == SCHEDULED) startClass(...)
  else if (status == IN_PROGRESS) completeClass(...)
  else if (status == COMPLETED) ...

✅ GOOD (State Pattern):
  status.transition(event);  // polymorphic dispatch
```

### 3.4 Direct External API Coupling
```
❌ BAD:
  OllamaResponse resp = ollamaApi.call(...);
  String text = resp.getChoices().get(0).getMessage().getContent();  // Ollama-specific

✅ GOOD (Adapter):
  AIResult result = aiClient.invoke(request);  // domain type
  String text = result.getText();
```

### 3.5 Direct Event Publishing
```
❌ BAD:
  @Transactional
  public void update(...) {
    repo.save(...);
    rabbitTemplate.send(...);  // If broker down, event lost but DB updated
  }

✅ GOOD (Outbox):
  @Transactional
  public void update(...) {
    repo.save(...);
    outbox.save(new Event(...));  // Same txn
  }
  // Separate worker publishes from outbox
```

#### 3.5.1 Outbox Bypass Policy

> **Default: every cross-service event MUST flow through `OutboxEventWriter` (or a per-module domain outbox — see precedents `MigrationOutboxRepository` in kitehub-subscription, `BrandingEventEmitter` in kitehub-branding). Direct `rabbitTemplate.convertAndSend(...)` is BANNED unless one of the documented exceptions below applies. Silent bypass is the anti-pattern.**

Per ADR-021, kitehub-* modules use per-module domain outbox (entity + repository + emitter) instead of a cross-product shared library. Choose the precedent that matches your module: `kiteclass-core` consumers use the generic `OutboxEventWriter`; `kitehub-*` consumers copy the per-module pattern.

Three — and only three — documented exceptions are permitted. Any direct publish MUST cite which exception applies inline as a code comment so reviewers + audit skill can recognize it.

**Exception A — Fast-path with outbox backup**

When latency-sensitive consumers (cache eviction, UI push) need sub-second propagation AND an outbox row already covers the same event for reliability. Both writes must occur in the same `@Transactional` block; outbox-first, direct-publish-second; direct publish must be wrapped in try/catch so its failure never propagates.

```java
// EXAMPLE — see kiteclass-core .../branding/events/BrandingEventPublisher
outbox.enqueue(routingKey, aggregateType, aggregateId, payload); // reliability net

if (rabbitTemplate != null) {
    try {
        // Best-effort fast-path for cache eviction; outbox is the reliability net.
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    } catch (Exception ex) {
        log.warn("Direct publish failed — outbox will retry: {}", ex.getMessage());
    }
}
```

Required marker comment: must include the literal phrase `outbox is the reliability net` OR `fast-path` so the audit detector skips it.

**Exception B — Bean wiring / Config code**

Code in `*Config.java`, `*Configuration.java`, or javadoc/comment example-only references is exempt — these don't execute at request time.

**Exception C — Test fixtures**

Code in `src/test/**` paths is exempt; tests deliberately exercise broker-direct paths to validate consumers.

**Exception D — Dedicated dispatcher infrastructure**

A class whose single purpose is to publish events to RabbitMQ on behalf of callers (e.g. `AIQueueDispatcher` routes by tier, `EmailServiceClient` ships email events) is exempt when ALL four criteria hold:

1. **Naming:** class name is suffixed `Dispatcher`, `Publisher`, `Client`, or equivalent infrastructure naming
2. **Caller contract:** callers MUST have already persisted their domain change before invoking it (verified at code review, NOT enforced by runtime)
3. **No business logic:** the dispatcher contains routing, serialization, metric emission, send — nothing else. Any conditional that depends on domain state means it's a service, not a dispatcher → use Exception A instead
4. **Marker:** class-level javadoc includes the literal phrase `dedicated dispatcher infrastructure`

```java
// EXAMPLE — kitehub-branding/.../queue/AIQueueDispatcher.java
/**
 * Routes AI jobs to the correct tier queue based on priority.
 *
 * <p>This is dedicated dispatcher infrastructure (per design-patterns.md §3.5.1
 * Exception D) — its single purpose is the broker handoff. Callers persist
 * their {@code BrandingJob} row in their own transaction before invoking.</p>
 */
@Component
public class AIQueueDispatcher { ... }
```

Rationale: wrapping a dispatcher in outbox creates wrap-the-wrapper and adds latency to the operation the dispatcher exists to make fast. Reliability moves to callers, which write domain row + outbox row before invoking the dispatcher.

**Anti-pattern (BANNED):** any other direct-publish site without one of these markers. The `quality/design-pattern-audit` skill (Cat 5) flags every site; reviewers verify each flagged site is either eliminated, migrated to outbox, or documented under A/B/C/D.

### 3.6 Missing Resilience on External Calls
```
❌ BAD:
  String result = restTemplate.postForObject(aiApiUrl, req, String.class);
  // Exception propagates → cascade failure

✅ GOOD:
  @CircuitBreaker(name = "ai", fallbackMethod = "templateFallback")
  @Bulkhead(name = "ai")
  @Retry(name = "ai")
  public String callAI(Request req) { ... }
```

### 3.7 Feature Envy
```
❌ BAD:
  public void processInvoice(Invoice inv) {
    var total = inv.getItems().stream().mapToDouble(i -> i.getAmount()).sum();
    total -= inv.getDiscount();
    // Accessing Invoice data more than own class
  }

✅ GOOD:
  public void processInvoice(Invoice inv) {
    var total = inv.calculateTotal();  // Domain logic in Invoice
  }
```

### 3.8 Shotgun Surgery
Change requires modifying ≥5 files → pattern violation (likely missing abstraction).

### 3.9 Long Parameter List
```
❌ BAD:
  public void createBranding(String name, String logo, String color1,
    String color2, String font, String audience, String tone, ...) { ... }

✅ GOOD:
  public void createBranding(BrandingRequest req) { ... }
```

### 3.10 Leaky Abstraction
```
❌ BAD:
  public interface AIClient {
    OllamaResponse analyze(OllamaRequest req);  // Ollama types leak
  }

✅ GOOD:
  public interface AIClient {
    AnalysisResult analyze(AnalysisRequest req);  // Domain types
  }
```

### 3.11 Audit/Log Service Joining Parent `@Transactional`

```java
❌ BAD:
  @Service
  public class LoginAuditService {
    @Transactional                                    // joins parent txn (REQUIRED)
    public void recordLogin(User u, HttpServletRequest req) {
      try {
        repository.save(new LoginAuditLog(...));      // SQLException → Spring sets rollback-only
      } catch (Exception ex) {
        log.warn("audit failed: {}", ex.getMessage()); // swallow, but flag persists
      }
    }
  }
  // Caller's @Transactional commit phase throws UnexpectedRollbackException → HTTP 500.
  // Javadoc claim "never blocks caller" is FALSE as implemented.

✅ GOOD:
  @Service
  public class LoginAuditService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)   // own physical txn
    public void recordLogin(User u, HttpServletRequest req) {
      try {
        repository.save(new LoginAuditLog(...));
      } catch (Exception ex) {
        log.warn("audit failed: {}", ex.getMessage());        // truly isolated now
      }
    }
  }
```

**Why this is a separate anti-pattern from §3.5 Outbox bypass:** Outbox enforces same-txn writes for delivery guarantees. Audit/log/notification services have the **opposite** requirement — caller's success must NOT depend on the side-effect, so the side-effect must run in its own txn boundary.

Trigger this anti-pattern when:
- Service class name suffix is `*AuditService` / `*LogService` / `*NotificationService` / `*TrackingService`
- Method named `record*`, `log*`, `audit*`, `track*`, `notify*`
- Javadoc claims "never blocks caller" / "best-effort" / "audit failure swallowed"
- BUT `@Transactional` uses default propagation (joins parent)

Authoritative rule: `.claude/rules/audit-service-isolation.md` (mandates `Propagation.REQUIRES_NEW` for every method matching scope). Companion incident: 2026-05-16 production admin login 500 (audit RCA `documents/04-quality/audits/aws-verification/2026-05-16-admin-login-500-rca.md`).

### 3.12 Entity-Migration-Mapper Triad Drift

```java
❌ BAD:
  // Wave br-4 hotfix #1784 pattern — added entity field, forgot Flyway migration
  @Entity
  public class Course {
    private String name;
    private PricingModel pricingModel;                  // NEW field
  }
  // V70__alter_course_pricing_model_default.sql      ← DOES NOT EXIST
  // → Spring boots fine in H2; production Postgres throws column-not-found at first query

  // Wave br-4 hotfix #1787 pattern — added DB column + JPA mapping, forgot MapStruct @Mapping
  @Mapper
  public interface ClassMapper {
    // missing: @Mapping(target = "rescheduledByUserId", ignore = true)
    // missing: @Mapping(target = "rescheduledAt", ignore = true)
    // 6 reschedule audit columns silently ignored → strict-warnings build fails
  }

✅ GOOD:
  // Entity field, migration, mapper declaration land same PR
  @Entity public class Course { ... private PricingModel pricingModel; }
  // V70__alter_course_pricing_model_default.sql:
  //   ALTER TABLE courses ADD COLUMN pricing_model VARCHAR(20) NOT NULL DEFAULT 'PER_HOUR';
  @Mapper public interface CourseMapper {
    @Mapping(target = "pricingModel", source = "request.pricingModel")
    Course toEntity(CreateCourseRequest request);
  }
```

**Why this matters:** Wave br-4 (2026-05-22) saw 60% bucket hotfix rate; 2 of 3 hotfixes (#1784 + #1787) were entity ↔ migration ↔ mapper triad drift — caught post-merge by IDE/CI instead of pre-merge. The triad MUST move atomically.

**Detection:** `scripts/check-entity-mapper-consistency.sh` (CI job `entity-mapper-consistency` in `quality-code.yml`, WARN-mode v1) heuristically scans entity fields → migration columns (camelCase → snake_case mapping). Override: PR body trailer `ENTITY_MAPPER_DRIFT_OVERRIDE: <reason + follow-up gap>`.

**Limitations of detector v1:** grep-based, no AST. False positives expected on:
- Inherited `BaseEntity` fields (filtered via allowlist: id, created_at, updated_at, ...)
- Relations (`@OneToMany` / `List<X>` excluded)
- Custom `@Column(name = "...")` overrides (parsed)
- Migration columns added in later V file than entity (acceptable when same PR ships both)

Companion incident: 2026-05-22 Wave br-4 hotfixes #1784 + #1787 (audit `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` §OPS-BR4-002). Gap: GAP-743.

---

## 4. PR Review Checklist

Reviewer KIỂM TRA:

- [ ] Pattern choice documented trong javadoc
- [ ] YAGNI check (not over-engineered)
- [ ] Service <15 methods (else Facade refactor)
- [ ] Status transitions via State Pattern (not switch)
- [ ] External APIs wrapped by Adapter
- [ ] Events published via Outbox (if DB txn)
- [ ] External calls have Circuit Breaker + fallback
- [ ] No primitive obsession (value objects used)
- [ ] No Ollama/OpenAI types in domain layer
- [ ] Resource routing via Chain (if ≥3 types)
- [ ] No God Service (>500 lines)
- [ ] Tests leverage pattern boundaries
- [ ] Entity field changes paired with Flyway migration + MapStruct @Mapping update same PR (§3.12)

---

## 5. Refactoring Triggers

Khi detect anti-patterns trong existing code, ƯU TIÊN refactor:

| Trigger | Refactor Required |
|---------|-------------------|
| Service exceeds 500 lines | Extract với Facade + services |
| Status scattered if/switch | State Pattern |
| Event publish scattered | Centralize with Outbox |
| Same external API wrapped differently | Consolidate Adapter |
| 3+ similar classes | Extract template method or strategy |

---

## 6. Training & Adoption

### New team member
- Read: `ai-branding-design-patterns.md` catalog
- Read: this rules doc
- Shadow pattern-applying PRs

### Existing team
- Review anti-patterns quarterly
- Code kata sessions on pattern application
- Architecture review meetings include pattern check

### Tooling
- `/design-pattern-advisor` skill — interactive guidance
- ArchUnit tests — enforce pattern constraints in CI
- SonarQube rules — detect some anti-patterns

---

## 7. Pattern Maturity Levels

| Level | Description | Action |
|-------|-------------|--------|
| ⭐ Basic | GoF patterns applied correctly | Expected for all devs |
| ⭐⭐ Intermediate | Enterprise patterns (Saga, Outbox) | Senior devs |
| ⭐⭐⭐ Advanced | DDD Aggregates, CQRS, Event Sourcing | Architects |

Team goal: majority Level 2, architects Level 3.

---

## 8. Common Mistakes to Avoid

- ❌ Applying Singleton everywhere (makes testing hard)
- ❌ Creating abstractions prematurely (YAGNI violation)
- ❌ Over-using interfaces for single implementation
- ❌ Factory patterns for simple `new` cases
- ❌ Observer for 1 observer (just call method)
- ❌ Command pattern for simple actions (just use lambda)

---

## 9. Log

- **2026-05-25** (v1.5.0): MINOR — added §3.12 "Entity-Migration-Mapper Triad Drift" anti-pattern. Triggered by Wave br-4 (2026-05-22) 60% bucket hotfix rate; 2 hotfixes (#1784 Course.pricingModel missing migration; #1787 ClassMapper missing @Mapping ignore) traced to triad drift per ops-readiness audit OPS-BR4-002. Paired same-PR with new CI script `scripts/check-entity-mapper-consistency.sh` (heuristic v1, WARN-mode) + workflow job `entity-mapper-consistency` in `quality-code.yml` per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Closes GAP-743 (Wave meta-1 Bucket D). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — existing entity-migration-mapper drift grandfathered; detector WARN-mode initially exit 0; HARD STOP deferred until 2nd recurrence post-rule per `incident-to-rule-pipeline.md` §3.1). Self-test: 2 inline fixtures (PASS + FAIL) verified; real-repo scan 89 entities / 126 migrations / 16 mappers / 117 WARN findings — heuristic FP expected (grep-based not AST).
- **2026-05-16** (v1.4.0): MINOR — added §3.11 "Audit/Log Service Joining Parent @Transactional" anti-pattern. Triggered by 2026-05-16 production admin login 500 incident (`LoginAuditService.recordLogin` join parent txn → SQLException → `UnexpectedRollbackException` → 500). Paired same-PR with new sister-rule `.claude/rules/audit-service-isolation.md` v1.0.0 (positive form mandating `Propagation.REQUIRES_NEW`) + `postgres-specific-type-testcontainers.md` v1.0.0 (Bug 1 of same incident) + `release-deploy-standard.md` smoke admin-login extension per `rule-change-process.md` §6.5 Enforcement Parity Mandate. MINOR bump per §4 — adds anti-pattern that BLOCKS prior-passing PRs (any new audit service `@Transactional` w/o REQUIRES_NEW will now be flagged). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening; existing 15+ audit/log services grandfathered, anti-pattern applies prospectively to new code + edits).
- **2026-05-14** (v1.3.1): PATCH — thêm `paths: ["**/*.java", "**/*.tsx", "**/*.ts"]` frontmatter qua Wave 73 Bucket A5 (path-scope 6 design/wave/AI rules). Per Anthropic native `paths:` mechanism, rule giờ chỉ auto-load khi Claude đọc file Java/TypeScript/TSX. Không thay đổi rule content/scope; reduces base context auto-load per Wave 73 Meta Context Optimization plan. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — additive frontmatter, no constraint change).
- **2026-04-26** (v1.3.0): MINOR — added §3.5.1 Exception D (Dedicated dispatcher infrastructure) with 4-criterion test (naming + caller contract + no business logic + marker phrase) + AIQueueDispatcher example. Updated anti-pattern wording from A/B/C → A/B/C/D. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — paired with AIQueueDispatcher javadoc marker application + GAP-230 closure in same PR). Closes GAP-230. Motivation: GAP-222a Phase 2 found `AIQueueDispatcher` did not fit Exception A (no co-located domain transaction; class IS the dispatcher). Wrapping a dispatcher in outbox adds latency to the operation the dispatcher exists to make fast. Exception D legitimizes "dedicated dispatcher infrastructure" pattern under strict 4-criterion test that prevents abuse as escape hatch.
- **2026-04-26** (v1.2.0): MINOR — extended §3.5.1 default-rule paragraph to cite per-module domain outbox precedents (`MigrationOutboxRepository`, `BrandingEventEmitter`) alongside `OutboxEventWriter`, and added one-paragraph guidance pointing to ADR-021 for module-by-module choice. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — paired with ADR-021 acceptance + GAP-222a Phase 2 implementation in same wave). Closes part of GAP-222a Phase 3 (rule clarification AC). Motivation: original §3.5.1 mentioned MigrationOutboxRepository only as a parenthetical example; ADR-021 elevates per-module pattern to primary path for cross-product modules — rule must reflect.
- **2026-04-26** (v1.1.0): MINOR — added §3.5.1 Outbox Bypass Policy (Exceptions A/B/C + anti-pattern envelope). Backfilled mandatory frontmatter (Version, Created, Last-Reviewed, Reviewer-Approver, Applies-to) per `rule-change-process.md` §3 backfill-on-next-edit policy. Closes part of GAP-222 Phase 1 (Sub-PR 6.4). Reviewer: @nguyenvankiet (solo-dev self-approve per §5 matrix for MINOR — paired with detector calibration in same PR + post-wave audit in Sub-PR 6.5). Motivation: design-pattern audit baseline 2026-04-26 (Sub-PR 6.1) found 5 services bypassing Outbox without policy → couldn't tell which were intentional. §3.5.1 turns "silent bypass" into "documented exception or violation" so future audits + reviewers can decide objectively.
- 2026-04-14 (v1.0.0): Rules created based on AI Branding v2 design patterns catalog
