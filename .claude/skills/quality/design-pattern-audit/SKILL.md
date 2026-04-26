---
name: design-pattern-audit
description: "Dùng khi user nói 'design pattern audit', 'pattern check', 'kiểm tra design patterns', 'God service đâu', 'anti-pattern hotspot', hoặc trước Wave 7+ refactor planning. Score code against `.claude/rules/design-patterns.md` §3 BANNED list. Output: hotspot list (file path + LOC + violated pattern) + score /100."
user-invocable: true
---

# /design-pattern-audit — Verify Code ↔ Pattern Rules

Score /100. Walk every Java service module, flag `design-patterns.md` §3 anti-patterns. Closes GAP-046 cycle.

## Process

### 1. Collect Modules

```bash
find kiteclass kitehub -maxdepth 3 -type d -name src | grep main/java
```

### 2. Per-Module: 5 Anti-Pattern Categories (20 pts each)

For EACH service module, run detectors and apply rubric in `reference/scoring-guide.md`:

| # | Category (20pts) | Detector | Pass criteria |
|---|------------------|----------|---------------|
| 1 | **God Service / Class** | `find */src/main/java -name "*Service.java" -exec wc -l {} +` | No service > 500 LOC (per `design-patterns.md` §3.1) |
| 2 | **Status switch/if cascade** | grep `if.*Status ==` / `switch.*[Ss]tatus` | State Pattern used for entities with ≥3 lifecycle states |
| 3 | **Primitive Obsession** | grep public method signatures with `String color\|String email\|String hex` | Value objects (ThemeColor, Email) preferred for validated primitives |
| 4 | **Leaky Abstraction** | grep `OllamaResponse\|OpenAIResponse` outside `*adapter*` packages | External API types isolated to Adapter layer |
| 5 | **Direct event publish (no Outbox)** | grep `rabbitTemplate.send\|streamBridge.send` outside `outbox/` packages | Events go through `OutboxEventWriter` |

Detector recipes: `reference/anti-pattern-detectors.md`

### 3. Output

Save report to `documents/04-quality/audits/design-patterns/audit-YYYY-MM-DD.md` with structure:
- Score /100 (sum of 5 categories × 20)
- Per-module table (rows = modules, columns = 5 categories)
- Hotspot list — each violation = 1 row (file path + LOC + category + suggested fix)
- Comparison vs previous audit if exists

### 4. Hotspot → Gap Pipeline

For each P0/P1 hotspot, follow `audit-to-gap-pipeline.md` Step 2 (duplicate check) + Step 2.5 (state-check) + Step 3 (gap file). Do NOT fix in audit session.

## Grep Scope — CRITICAL

Multi-module repo (KiteHub 6 services + KiteClass core/gateway/frontend). Narrow scope = silent false-positive (memory `feedback_audit_grep_scope.md`).

```bash
# Preferred — broad with extension filter
grep -rnE "pattern" --include="*.java"

# Or — explicit submodules
grep -rn "pattern" kiteclass/*/src/ kitehub/*/src/ --include="*.java"
```

## Calibration Notes

First-run baseline: expect ~50-65/100. Per memory `feedback_audit_calibration.md`, self-audits overstate 15-20pts vs specialist. Trust the delta, not absolute number. Don't treat first low score as regression.

Threshold rationale (`design-patterns.md` §3.1): ">500 lines = refactor required". Adjust ONLY if all modules cluster between 450-550 (legacy noise floor).

## Context Management

5-25K tokens depending on module count. Tactics:

1. **Pipe greps** — `| head -30` for cascade detectors (only need count, not full match list)
2. **Per-module staging** — score 2 modules deeply, apply pattern to rest
3. **Subagent split** — KiteClass agent + KiteHub agent if >10K tokens

## Gotchas

- **Status switches in test files** — exclude `--include="*Test.java"` patterns; tests legitimately exercise state transitions via switch
- **Lombok `@Builder` does NOT trigger Primitive Obsession** — generated code, not signature design choice
- **Outbox detector** — `OutboxEventPublisher.publish()` IS the right path; only flag `rabbitTemplate.send` OUTSIDE the outbox package
- **God Service threshold** — measure `wc -l` against the .java file, not LOC of method bodies
- **Calibration first run** — DO NOT file gaps until baseline reviewed; threshold may need module-specific calibration

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category with point deductions
- `reference/anti-pattern-detectors.md` — Grep one-liners for each category, false-positive avoidance
