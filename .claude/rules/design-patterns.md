# Design Patterns — Project Rules

Project-wide rules for applying design patterns. **Mandatory** khi develop new feature, refactor, review PR.

Reference catalog: `documents/02-architecture/ai-branding-design-patterns.md` (AI Branding-specific)
Skill helper: `.claude/skills/reference/design-pattern-advisor.md`

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

- 2026-04-14 — Rules created based on AI Branding v2 design patterns catalog
