# Skill: Design Pattern Advisor

**Version:** 1.0
**Created:** 2026-04-14
**Purpose:** Help developers choose + apply right design patterns. Detect anti-patterns trong review.

---

## When to Use

- Designing new feature hoặc service
- Refactoring existing code
- Code review (self + peer)
- Discussing architecture trade-offs
- User says: "cần design pattern nào?", "refactor thế nào?", "code smell không?"

---

## Methodology

### Step 1: Classify Problem

Ask 3 questions:
1. **What varies?** (algorithm, behavior, state, creation)
2. **What stays fixed?** (interface, sequence, structure)
3. **What's the coupling pain?** (tight, scattered, leaky)

### Step 2: Match Pattern

Use decision tree:

```
Problem: Multiple implementations swap-able?
  → Strategy Pattern

Problem: Object has finite states + strict transitions?
  → State Pattern

Problem: Need to undo/queue/log operations?
  → Command Pattern

Problem: Complex subsystem exposed as simple API?
  → Facade Pattern

Problem: External API vendor-specific?
  → Adapter Pattern + Anti-Corruption Layer

Problem: Request routing with fallback chain?
  → Chain of Responsibility

Problem: Many optional fields?
  → Builder Pattern

Problem: Expensive operation cached transparently?
  → Proxy Pattern

Problem: Cross-cutting concerns (retry, logging)?
  → Decorator Pattern

Problem: Many observers of same event?
  → Observer / Pub-Sub

Problem: Reliable event publishing with DB txn?
  → Outbox Pattern

Problem: Distributed transaction across services?
  → Saga Pattern

Problem: Upstream flaky, need fast failure?
  → Circuit Breaker + Bulkhead

Problem: Versioning state with replay?
  → Event Sourcing

Problem: Complex stateful UI flow?
  → State Machine (XState)

Problem: Gradual replacement of legacy?
  → Strangler Fig
```

### Step 3: Apply (Don't Over-Engineer)

**YAGNI check before applying:**
- Is the variation point real or hypothetical?
- Will there be ≥2 implementations in foreseeable future?
- Is complexity worth the flexibility?

Simple case → direct code. Complex case → pattern.

### Step 4: Verify Anti-Patterns NOT introduced

Check common smells:
- God Class (>15 methods, >500 lines)
- Primitive Obsession (Strings for structured data)
- Shotgun Surgery (change requires touching N files)
- Feature Envy (method accesses other class data more than own)
- Leaky Abstraction (interface reveals implementation)
- Circular Dependencies
- Long Parameter List (>4 params)

---

## Project-Specific Pattern Catalog

Canonical reference: **`documents/02-architecture/ai-branding-design-patterns.md`**

Quick map:

| Domain | Required Pattern |
|--------|-----------------|
| AI Provider Selection | Strategy |
| Instance Lifecycle | State Machine |
| Pipeline Steps | Command + Composite |
| Service Orchestration | Facade |
| External APIs (Ollama, OpenAI, Stripe) | Adapter + ACL |
| Resource Routing (static/template/AI) | Chain of Responsibility |
| Event Publishing | Observer + Outbox |
| Distributed Transactions | Saga |
| Resilience | Circuit Breaker + Bulkhead |
| Complex Frontend Flow | XState State Machine |
| Legacy Migration | Strangler Fig |

---

## Anti-Pattern Detection

### 1. God Service
```
Symptoms: 1 service with 20+ public methods, 500+ lines
Fix: Break into smaller services + Facade on top
Example: AIBrandingService → BrandingFacade + Analyzer + Planner + Executor
```

### 2. Primitive Obsession
```
Symptoms: String colors, int status codes, String prompts
Fix: Value objects
  - String "#2563eb" → ThemeColor value object
  - int 1 → OrderStatus enum
  - String prompt → PromptInput with validation
```

### 3. Scattered Status Logic
```
Symptoms: if (status == X) ... else if (status == Y) everywhere
Fix: State Pattern
  - Each status = 1 State class
  - Transitions encapsulated
  - Invalid transitions throw at compile/runtime
```

### 4. Direct External API Calls
```
Symptoms: Ollama-specific types used in domain layer
Fix: Adapter Pattern + ACL
  - Domain has AIModel interface
  - OllamaAdapter converts domain <-> Ollama
  - Swap vendor = swap adapter
```

### 5. Unreliable Event Publishing
```
Symptoms: rabbitTemplate.send() directly in service
Fix: Outbox Pattern
  - Save event to DB in same txn as data change
  - Background job publishes from outbox
  - Guaranteed exactly-once delivery
```

### 6. Missing Resilience
```
Symptoms: @RestTemplate call → propagates failure on cascade
Fix: Circuit Breaker + Bulkhead + Timeout
  - Resilience4j annotations
  - Fallback methods
  - Isolated thread pools
```

---

## Output Format

Khi user hỏi "pattern nào cho X?":

```markdown
## Problem: [summary]

**Varies:** ...
**Fixed:** ...
**Pain:** ...

## Recommended: [Pattern Name]

**Why:** [explanation]

**Code skeleton:**
```java
// Minimal example
```

**Benefits:**
- ...

**Alternatives considered:**
- [Other pattern] — why not chosen

**YAGNI check:** ✅ Justified / ❌ Over-engineering, use simple code

**Related in codebase:**
- [existing usage]
```

---

## Example Use Cases

### Case 1: "Thêm AWS Bedrock vào AI providers"

**Advisor recommends:** Strategy Pattern (extend existing)
- Add `BedrockClient implements AIClient`
- Config `ai.provider=bedrock` switches
- Zero changes to consumers

### Case 2: "Status transitions phức tạp, hard to track"

**Advisor recommends:** State Pattern
- Convert enum + if/switch → State classes
- Transitions encapsulated
- Unit test each state isolated

### Case 3: "Branding service quá lớn"

**Advisor recommends:** Refactor to Facade + smaller services
- Extract BrandingAnalyzer, BrandingPlanner, BrandingExecutor
- BrandingFacade orchestrates
- Original service becomes thin wrapper

### Case 4: "Event sometimes lost on crash"

**Advisor recommends:** Outbox Pattern
- Add `outbox_events` table
- Publish from outbox async
- Guarantees at-least-once (with idempotent consumers)

---

## Integration với Review Process

### Pre-implementation (brainstorm stage)
```
/design-pattern-advisor "need to support multiple payment gateways"
→ Suggests: Strategy + Adapter
→ Code skeleton provided
→ Implementation guided
```

### Code review stage
```
/design-pattern-advisor review <file>
→ Scans for anti-patterns
→ Suggests refactors
→ Points to pattern catalog
```

### Architecture review
```
/design-pattern-advisor architecture [feature]
→ Reviews design doc
→ Recommends patterns per component
→ Identifies missing patterns
```

---

## Skill Contents

- This SKILL.md — advisor methodology
- Reference catalog: `documents/02-architecture/ai-branding-design-patterns.md`
- Rules: `.claude/rules/design-patterns.md`
- Complements: `two-stage-code-review.md` (pattern review stage)

## Rules

- ✅ Match pattern to problem, not force problem to pattern
- ✅ YAGNI check before applying
- ✅ Standard pattern names trong code/docs
- ✅ Document pattern choice trong javadoc
- ❌ Không apply patterns cho trivial cases
- ❌ Không mix patterns without rationale
- ❌ Không invent custom "patterns" — use standard ones
