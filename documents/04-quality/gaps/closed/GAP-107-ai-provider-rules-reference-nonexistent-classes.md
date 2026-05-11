# GAP-107: AI-Provider Rules.md References Non-Existent Classes (ResilientAIClient / MockAIClient / ai-live profile)

**Status:** 🟢 DONE (FALSE POSITIVE — retract 2026-04-20)
**Priority:** 🟠 P1
**Domain:** KiteClass / AI Branding / Business Docs
**Found:** 2026-04-19 (business-logic audit)
**Retracted:** 2026-04-20 (business-logic re-audit PR #379 — classes verified present)
**Affects:** kitehub-branding, reviewer trust trong rules.md, onboarding new devs

## Resolution (2026-04-20)

Baseline audit (2026-04-19) grep scope was `kitehub/` + `kiteclass/` as a single search, but the actual classes live in `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/`. Re-audit verified 3 classes exist:

- `ResilientAIClient.java` — `kiteclass/kiteclass-core/.../module/ai/client/`
- `MockAIClient.java` — same folder
- `OllamaAIClient.java` — same folder
- Tests: `ResilientAIClientTest.java` + `MockAIClientTest.java` + `Wave03IntegrationTest.java`

`@Profile("ai-live")` wiring on `OllamaAIClient` + default `MockAIClient` confirmed match rules.md description. **The gap's premise was wrong** — no drift actually exists between rules and code.

**Action:** retract GAP-107 as false positive. Rules.md in kiteclass/ai-provider/ accurate.

**Lesson learned for audit skill:** grep scope should include `kiteclass/kiteclass-core/` explicitly, not just `kitehub/` + `kiteclass/` top-level (which excludes the `-core` submodule's sources).

## Problem

`documents/01-business/kiteclass/ai-provider/rules.md:12-15` document 6 rules (BR-AI-001 → BR-AI-006). Trong đó:

- **BR-AI-002**: "All calls routed through `ResilientAIClient` (primary bean) — Circuit Breaker + Bulkhead + Retry + fallback"
- **BR-AI-005**: "Exactly one `baseAIClient` bean active per environment: `MockAIClient` (default) or `OllamaAIClient` (profile `ai-live`)"
- **Supported providers table**: `MockAIClient | default (no profile) | ✅ ready`, `OllamaAIClient | ai-live profile`

Nhưng reality:
```
$ grep -r "ResilientAIClient\|MockAIClient" kitehub/ kiteclass/ --include="*.java"
# 0 hits

$ ls kitehub/kitehub-branding/src/main/java/com/kitehub/branding/client/
AIClient.java
OllamaClient.java       # không phải OllamaAIClient
OpenAIClient.java       # không phải rule mention
```

`AIProviderConfig.java:46-64` dùng `ai.provider` property (string "ollama" / "openai") để chọn bean, KHÔNG dùng Spring profile `ai-live`. Resilience4j annotations nằm rải rác trong controller/service chứ không aggregate vào wrapper class như "ResilientAIClient".

## Root Cause

Rules.md được viết as design spec cho Sub-PR 3.2 (Wave 3 AI provider scaffolding), ADR-008. Implementation ship với tên class khác và wiring pattern khác nhưng rules.md không update lại. Code tiến hóa, docs tĩnh. Không có automated check class-name-references.

Meta impact: developer mới đọc rules.md tìm `ResilientAIClient` → không tìm thấy → mất tin tưởng vào rules.md → skip doc review cho PR sau.

## Proposed Fix

Option A (recommended — accept reality): update `rules.md` để match code thực tế.

```markdown
### AIClient interface
| ID | Rule |
|----|------|
| BR-AI-001 | Domain code MUST reference `AIClient` interface, never concrete provider types (Adapter pattern) |
| BR-AI-002 | Resilience applied per-method via @CircuitBreaker/@Bulkhead/@Retry annotations từ resilience4j (no single "ResilientAIClient" wrapper) |
| BR-AI-005 | Exactly one AI client active per deployment: `OllamaClient` hoặc `OpenAIClient`, selected by `ai.provider` property (not Spring profile) |

### Supported providers (current)
| Provider | Config | Status |
|----------|--------|--------|
| `OpenAIClient` | `ai.provider=openai` (default) | ✅ production |
| `OllamaClient` | `ai.provider=ollama` | ✅ local/dev |
```

Option B (refactor code to match rules): tạo wrapper class `ResilientAIClient` + `MockAIClient`. Work intensive, YAGNI — Option A preferred.

## Acceptance Criteria
- [ ] Rules.md BR-AI-002, BR-AI-005, provider table match grep output
- [ ] Class names trong rules.md exist trong codebase (verify với `grep -c "ClassName" kitehub/kitehub-branding/src/main/java/`)
- [ ] `ai.provider` property documented là selection mechanism (không phải Spring profile)
- [ ] Resilience pattern (annotations vs wrapper) documented đúng actual
- [ ] Rules.md "Log" section ghi 2026-04-19 — audit catch-up

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Rule source: ADR-008 (AI provider abstraction), Wave 3 Sub-PR 3.2
- Related gaps: GAP-104 (fair-queue undocumented), GAP-106 (branding.routing config missing) — similar drift pattern
