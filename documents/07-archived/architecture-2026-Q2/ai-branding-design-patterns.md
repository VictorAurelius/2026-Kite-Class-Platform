# AI Branding — Design Patterns Catalog

**Trạng thái:** 🟡 DRAFT
**Ngày:** 2026-04-14
**Companion to:** `ai-branding-v2-redesign.md`

Mapping design patterns (GoF + architectural + DDD) vào từng component của AI Branding để tối ưu hóa maintainability, testability, scalability.

---

## 1. Tại sao cần design patterns?

Current v2 design **đã dùng patterns implicit** nhưng không systematic:
- AIClient interface → hint of Strategy Pattern
- FrontendInstanceStatus transitions → hint of State Pattern
- RabbitMQ events → hint of Observer/Pub-Sub
- BrandingAnalyzer/Planner/Executor → hint of Pipeline/Chain

**Chưa có:**
- Documented pattern choices
- Consistent application
- Testability benefits realized
- Future-proofing

Applying patterns systematically:
- ✅ Clear responsibility boundaries
- ✅ Easier testing (mock interfaces)
- ✅ Pluggable components (swap AI provider, template engine)
- ✅ Standard vocabulary (team communication)
- ✅ Scalability via proven solutions

---

## 2. Pattern Catalog — Per Component

### 2.1 AI Provider Selection → **Strategy Pattern**

**Problem:** Support multiple AI providers (Ollama local, OpenAI cloud, AWS Bedrock) with same interface.

```java
public interface AIClient {
  String analyzeLogo(String logoUrl);
  byte[] generateImage(ImagePrompt prompt);
  String generateText(TextPrompt prompt);
}

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public class OllamaClient implements AIClient { ... }

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAIClient implements AIClient { ... }

// Spring injects correct strategy based on config
```

**Benefit:** Switch provider via env var, no code change. Test with mock easily.

---

### 2.2 Resource Routing → **Chain of Responsibility**

**Problem:** Classify request → route to STATIC / TEMPLATE / FULL_AI with fallback.

```java
public abstract class ResourceHandler {
  protected ResourceHandler next;

  public ResourceHandler setNext(ResourceHandler next) { this.next = next; return next; }

  public BrandingResource handle(ResourceRequest req) {
    if (canHandle(req)) return process(req);
    if (next != null) return next.handle(req);
    throw new NoHandlerException();
  }

  abstract boolean canHandle(ResourceRequest req);
  abstract BrandingResource process(ResourceRequest req);
}

// Concrete: StaticHandler → TemplateHandler → AIHandler → FallbackHandler

var chain = new StaticHandler()
  .setNext(new TemplateHandler())
  .setNext(new AIHandler())
  .setNext(new FallbackHandler());

chain.handle(request);
```

**Benefit:** Add new resource type = add handler, no modification. Clear routing logic.

---

### 2.3 Instance Lifecycle → **State Pattern**

**Problem:** FrontendInstance has 6 states với strict transitions.

```java
public interface InstanceState {
  void initiate(FrontendInstance instance);
  void generate(FrontendInstance instance);
  void deploy(FrontendInstance instance);
  void fail(FrontendInstance instance, String reason);
}

public class NotStartedState implements InstanceState {
  public void initiate(FrontendInstance i) {
    i.setState(new InitializingState());
    // ...
  }
  // Others throw IllegalTransitionException
}

// State transitions encapsulated in state classes, not scattered if/switch
```

**Benefit:** Impossible transitions fail at compile/runtime. Each state = 1 class, easy to test.

---

### 2.4 Pipeline Steps → **Command Pattern + Composite**

**Problem:** Plan = sequence of Steps with retry/fallback.

```java
public interface Step extends Command {
  void execute(StepContext ctx);
  boolean canUndo();
  void undo(StepContext ctx);
}

// Composite: Plan is a Step containing sub-Steps
public class ExecutionPlan implements Step {
  private List<Step> steps;

  public void execute(StepContext ctx) {
    for (var step : steps) {
      step.execute(ctx);
    }
  }
}
```

**Benefit:** Steps composable, replaceable, queueable. Commands can be persisted (for retry).

---

### 2.5 Step Enhancements → **Decorator Pattern**

**Problem:** Cross-cutting: retry, logging, metrics around each step.

```java
public class RetryableStep implements Step {
  private final Step wrapped;
  private final int maxAttempts;

  public void execute(StepContext ctx) {
    for (int i = 0; i < maxAttempts; i++) {
      try { wrapped.execute(ctx); return; }
      catch (RetryableException e) { delay(i); }
    }
    throw new MaxRetriesException();
  }
}

public class LoggedStep implements Step { ... }
public class MetricsStep implements Step { ... }

// Compose:
Step step = new MetricsStep(new LoggedStep(new RetryableStep(new GenerateImageStep())));
```

**Benefit:** Add cross-cutting concerns without modifying Step implementations.

---

### 2.6 BrandingPackage Creation → **Builder Pattern**

**Problem:** BrandingPackage has many optional fields.

```java
public class BrandingPackage {
  // Private fields

  public static Builder builder(String instanceId) { return new Builder(instanceId); }

  public static class Builder {
    public Builder theme(ThemeConfig t) { ... }
    public Builder assets(Map<String, String> a) { ... }
    public Builder metadata(Metadata m) { ... }
    public BrandingPackage build() {
      // Validate required fields
      return new BrandingPackage(this);
    }
  }
}

// Usage:
var pkg = BrandingPackage.builder(instanceId)
  .theme(theme)
  .assets(assets)
  .metadata(meta)
  .build();
```

**Benefit:** Fluent construction, validation in build(). Immutable objects.

---

### 2.7 External AI APIs → **Adapter + Anti-Corruption Layer**

**Problem:** Gemma 4, OpenAI, AWS Bedrock have different request/response formats.

```java
// Common domain interface
public interface AIModel {
  ModelResult invoke(ModelRequest req);
}

// Adapters for each external API
public class GemmaAdapter implements AIModel {
  public ModelResult invoke(ModelRequest req) {
    // Convert domain ModelRequest → Gemma-specific format
    var gemmaReq = convertToGemmaFormat(req);
    var gemmaResp = gemmaApi.call(gemmaReq);
    // Convert response back to domain format
    return convertFromGemmaFormat(gemmaResp);
  }
}

public class OpenAIAdapter implements AIModel { ... }
```

**Benefit:** Domain model isolated from vendor changes. Replace vendor = swap adapter only.

---

### 2.8 Cached Package API → **Proxy Pattern**

**Problem:** Package API heavy query, cache frequently.

```java
public class CachedBrandingService implements BrandingService {
  private final BrandingService delegate;
  private final Cache<String, BrandingPackage> cache;

  public BrandingPackage getPackage(String tenantId) {
    return cache.get(tenantId, () -> delegate.getPackage(tenantId));
  }
}

// Spring injects CachedBrandingService as primary bean
```

**Benefit:** Transparent caching, no change to callers. Easily swap cache strategy.

---

### 2.9 Complex Subsystem → **Facade Pattern**

**Problem:** Many services (analyzer, planner, executor, quality, storage) — complex to coordinate.

```java
@Service
public class BrandingFacade {
  // Dependencies: 8+ services internally

  public BrandingResult createBranding(CreateBrandingRequest req) {
    // Orchestrate all services:
    // 1. Analyze → BrandingContext
    // 2. Plan → ExecutionPlan
    // 3. Execute → Resources
    // 4. Quality check → Score
    // 5. Deploy → Instance
    // Return unified result
  }
}

// Controller calls 1 facade method instead of 8 services
```

**Benefit:** Simplified API for clients, encapsulates orchestration.

---

### 2.10 Event Publishing → **Observer + Publish-Subscribe**

**Problem:** Multiple services need to react to branding events.

Already using RabbitMQ (pub-sub). Document explicitly:

```java
// Publisher
eventPublisher.publish("branding.exchange", "branding.updated",
  new BrandingUpdatedEvent(instanceId, version));

// Subscribers (decoupled)
@RabbitListener(queues = "kiteclass.branding-cache-invalidation")
@RabbitListener(queues = "kitehub-email.branding-refresh")
@RabbitListener(queues = "cdn-invalidation.branding-updated")
```

**Benefit:** Add consumer without modifying publisher. Loose coupling.

---

### 2.11 Atomic Event Publishing → **Outbox Pattern**

**Problem:** Need to publish event AFTER DB commit (not half-state where DB updated but event lost).

```java
@Entity
public class OutboxEvent {
  Long id;
  String eventType;
  String payload;
  Boolean published;
}

@Transactional
public void updateBranding(String tenantId, BrandingUpdate update) {
  brandingRepo.save(update);
  outboxRepo.save(new OutboxEvent("branding.updated", payload));
  // Both in same DB transaction
}

@Scheduled(fixedDelay = 1000)
public void publishOutbox() {
  var pending = outboxRepo.findUnpublished();
  for (var event : pending) {
    rabbitTemplate.publish(event);
    event.markPublished();
  }
}
```

**Benefit:** Exactly-once event delivery guarantee. No lost events on crash.

---

### 2.12 Saga for Distributed Txn → **Saga Pattern**

**Problem:** Provisioning spans multiple services (tenant → infra → branding → deploy). If one fails, need to rollback others.

```java
public class ProvisioningSaga {
  public void run(String tenantId) {
    var steps = List.of(
      new CreateInfrastructureStep(),    // compensate: deleteInfra
      new GenerateBrandingStep(),         // compensate: deleteBranding
      new DeployInstanceStep(),           // compensate: removeDNS
      new NotifyTenantStep()              // compensate: sendFailureEmail
    );

    var completed = new ArrayList<SagaStep>();
    for (var step : steps) {
      try {
        step.execute();
        completed.add(step);
      } catch (Exception e) {
        // Rollback completed steps in reverse order
        Collections.reverse(completed);
        completed.forEach(SagaStep::compensate);
        throw e;
      }
    }
  }
}
```

**Benefit:** Distributed transaction semantics without 2-phase commit.

---

### 2.13 Resilience → **Circuit Breaker + Bulkhead**

**Problem:** AI service flaky → cascade failure. Use Resilience4j.

```java
@Service
public class AIServiceCaller {

  @CircuitBreaker(name = "ai-service", fallbackMethod = "fallback")
  @Bulkhead(name = "ai-service", type = THREADPOOL)
  @Retry(name = "ai-service")
  public GeneratedImage generate(Request req) {
    return aiClient.generate(req);
  }

  public GeneratedImage fallback(Request req, Exception e) {
    return templateService.getDefault(req.resourceType);
  }
}
```

Config:
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
  bulkhead:
    configs:
      default:
        maxConcurrentCalls: 20  # isolate AI thread pool
```

**Benefit:** Fast failure when upstream down. Template fallback maintains UX.

---

### 2.14 Version History → **Event Sourcing** (optional)

**Problem:** Branding version history (GAP-033).

Current approach: snapshot each version.
Alternative: Event sourcing — rebuild state từ events.

```
Events:
- BrandingCreated(tenantId, initialConfig)
- ColorsChanged(tenantId, newColors)
- TemplateApplied(tenantId, templateId)
- LogoUploaded(tenantId, logoUrl)
- ...

Current state = replay all events for tenant
```

**Benefit:** Full audit trail, replay to any version, CQRS-friendly.
**Downside:** Complex — recommend snapshot for v2, event sourcing for v3.

---

### 2.15 Repository + Aggregate → **DDD Patterns**

**Problem:** Data access scattered.

```java
// Aggregate Root
public class FrontendInstance {  // Encapsulates BrandingVersion, QualityReport, etc.
  private List<BrandingVersion> versions;
  private InstanceStatus status;

  // Business logic methods
  public void rebrand(NewBranding input) { ... }
  public void recordQuality(QualityReport r) { ... }
}

// Repository
public interface FrontendInstanceRepository {
  FrontendInstance findById(String id);
  void save(FrontendInstance instance);
}
```

**Benefit:** Domain logic in domain model, not scattered in services.

---

### 2.16 Wizard Flow → **State Machine (XState)**

**Problem:** Wizard 10 steps với branching, validation, autosave.

Frontend uses XState:
```typescript
const wizardMachine = createMachine({
  id: 'branding-wizard',
  initial: 'welcome',
  states: {
    welcome: { on: { NEXT: 'basicInfo' } },
    basicInfo: { on: { NEXT: 'services', BACK: 'welcome' } },
    // ...
    preview: {
      on: {
        APPROVE: 'deploying',
        REGENERATE: { target: 'generating', actions: 'incrementCounter' }
      }
    },
    deploying: {
      invoke: { src: 'deployService', onDone: 'success', onError: 'failed' }
    },
    // ...
  }
});
```

**Benefit:** Testable, visualizable, impossible states impossible.

---

### 2.17 Migration Strategy → **Strangler Fig Pattern**

**Problem:** Migrate from v1 (direct AI) to v2 (agent workflow) without downtime.

```
Phase 1: Build v2 components, hidden behind feature flag
Phase 2: Route 1% traffic to v2 (shadow mode, log differences)
Phase 3: Gradual ramp: 10% → 50% → 100%
Phase 4: Remove v1 code
```

**Benefit:** Risk mitigation, rollback ready.

---

## 3. Anti-Patterns to Avoid

❌ **God Service** — 1 BrandingService doing everything (currently happening in AIBrandingService)
❌ **Primitive Obsession** — Passing Strings everywhere instead of value objects (ThemeColor, PromptInput)
❌ **Shotgun Surgery** — Change AI provider → modify 20 files
❌ **Feature Envy** — AIService reaches into Tenant object too much
❌ **Circular Dependencies** — Service A → B → C → A
❌ **Leaky Abstractions** — Domain leaking Ollama-specific types

---

## 4. Pattern-to-Gap Mapping

| Pattern | Primarily addresses | Related Gaps |
|---------|---------------------|--------------|
| Strategy | AI provider flexibility | GAP-006, GAP-028 |
| Chain of Responsibility | Resource routing | GAP-007 |
| State | Instance lifecycle | GAP-009 |
| Command + Composite | Pipeline steps | GAP-008 |
| Decorator | Step cross-cutting | GAP-008, GAP-019 |
| Builder | Complex object creation | GAP-010 |
| Adapter + ACL | External AI APIs | GAP-006, GAP-028 |
| Proxy (Caching) | Package API performance | GAP-043 |
| Facade | Simplified API | GAP-010, GAP-038 |
| Observer / Pub-Sub | Event distribution | GAP-002, GAP-009 |
| Outbox | Reliable events | GAP-039 |
| Saga | Distributed provisioning | GAP-015, GAP-030 |
| Circuit Breaker + Bulkhead | Resilience | GAP-002, GAP-030 |
| Event Sourcing | Version history (future) | GAP-033 |
| Repository + Aggregate | Data access | GAP-016 |
| State Machine (XState) | Wizard flow | GAP-013, GAP-020 |
| Strangler Fig | Migration strategy | GAP-008, GAP-028 |

---

## 5. Recommended Implementation Order

**Phase 1 — Foundation patterns (must have):**
1. Strategy Pattern (AIClient already partially done)
2. State Pattern (FrontendInstanceStatus)
3. Command Pattern (Steps)
4. Facade Pattern (BrandingFacade)

**Phase 2 — Resilience patterns:**
5. Circuit Breaker + Bulkhead
6. Outbox Pattern
7. Chain of Responsibility (resource routing)

**Phase 3 — Enterprise patterns:**
8. Saga Pattern (provisioning)
9. Event Sourcing (version history — optional)
10. CQRS (if scaling issues)

**Phase 4 — Frontend:**
11. XState wizard state machine
12. Compound Components

**Phase 5 — Migration:**
13. Strangler Fig (v1 → v2 rollout)

---

## 6. Testing Benefits

Patterns enable better testing:

| Pattern | Test benefit |
|---------|--------------|
| Strategy | Mock AIClient trivially |
| State | Test each state class isolated |
| Command | Test each Step isolated |
| Adapter | Test domain logic without external APIs |
| Outbox | Test event delivery guarantees |
| Saga | Test rollback behavior |

---

## 7. Anti-Pattern Detection in Review

Code review checklist:
- [ ] Không if/else cascade cho AI provider logic (should use Strategy)
- [ ] Không switch trên status everywhere (should use State)
- [ ] Không big service with 20 methods (should use Facade over smaller services)
- [ ] Không direct external API calls (should use Adapter)
- [ ] Không event publishing without outbox (reliability)

## 8. Log

- 2026-04-14 — Pattern catalog created to systematize AI Branding v2
