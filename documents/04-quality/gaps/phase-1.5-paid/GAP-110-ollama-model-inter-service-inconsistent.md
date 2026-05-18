# GAP-110: Ollama Default Text Model Inconsistent Between kitehub-branding and kiteclass-core

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Infrastructure / AI Provider / Cross-service Config
**Found:** 2026-04-19 (business-logic audit)
**Affects:** AI call consistency, inference quality reproducibility, operations debug

## Problem

Rule `documents/01-business/kiteclass/ai-provider/rules.md:57-58` document config key:
```
| `ai.ollama.default-model` | `gemma2` | Default model for Ollama calls |
```

Implementation trong 2 services:

**kiteclass-core** (`kiteclass/kiteclass-core/src/main/resources/application.yml:131-135`):
```yaml
ai:
  ollama:
    base-url: ${AI_OLLAMA_BASE_URL:http://localhost:11434}
    default-model: ${AI_OLLAMA_MODEL:gemma2}
```
✅ matches rule.

**kitehub-branding** (`kitehub/kitehub-branding/src/main/resources/application.yml:54-58`):
```yaml
ai:
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://kite-ollama:11434}
    text-model: ${OLLAMA_TEXT_MODEL:llama3.1:8b}
    vision-model: ${OLLAMA_VISION_MODEL:llava:13b}
    timeout-seconds: ${OLLAMA_TIMEOUT:120}
```
❌ Key name KHÁC (`text-model` vs `default-model`) + default value KHÁC (`llama3.1:8b` vs `gemma2`) + extra keys (`vision-model`, `timeout-seconds`) không documented in rules.md.

Consequence:
- AI analysis cho branding (kitehub) khác model với AI analysis cho k12/education logic (kiteclass) → output chất lượng không consistent
- Env var prefix khác: `AI_OLLAMA_*` (kiteclass) vs `OLLAMA_*` (kitehub) → ops config phải maintain 2 convention cho cùng 1 Ollama instance
- Rules.md BR-AI-004 chỉ mention "default-model" — không mention "text-model"/"vision-model" separation

## Root Cause

2 services phát triển parallel trong Wave 3 (kitehub-branding sub-PR 3.2 ship trước, kiteclass-core AI integration ship sau). Config property names không standardized giữa 2 services. Rules.md được viết reference một service (có vẻ kiteclass-core), không extend sang kitehub-branding.

## Proposed Fix

3 options, pick one:

### Option A (minimal — fix docs only)
Update rules.md accept reality: multi-service có thể có default khác nhau, document rationale:
```markdown
| `ai.ollama.text-model` (kitehub-branding) | `llama3.1:8b` | Text analysis cho branding (logo, tagline parsing) — prioritize instruction-following |
| `ai.ollama.default-model` (kiteclass-core) | `gemma2` | General education AI (quiz, summarization) — lightweight inference |
| `ai.ollama.vision-model` (kitehub-branding) | `llava:13b` | Logo vision analysis |
```

### Option B (full standardize — code refactor)
Unify config schema cả 2 services:
```yaml
ai:
  ollama:
    base-url: ...
    models:
      text: gemma2        # default
      vision: llava:13b   # kitehub-branding only, ignore in kiteclass
    timeout-seconds: 120
```
Env var: `KITE_AI_OLLAMA_MODELS_TEXT`, `KITE_AI_OLLAMA_MODELS_VISION` (prefix `KITE_` project-wide).

### Option C (ops choice)
Keep code as-is, document explicit warning trong rules.md + `documents/05-guides/ai-model-matrix.md` showing which service uses which model for what purpose. Ops team decides nếu muốn standardize.

**Recommended:** Option A hiện tại (low-risk, docs only), Option B khi Wave 5+ AI features expand đủ để justify refactor.

## Acceptance Criteria
- [ ] Rules.md `ai-provider/rules.md` Config Keys table list cả `text-model`, `vision-model`, `default-model` variants với service annotation
- [ ] Rationale cho default value khác (performance vs capability) documented
- [ ] Env var naming convention (ops-facing) documented
- [ ] Hoặc (Option B) 2 service config schemas unified với code changes

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Related gap: GAP-107 (AI provider rules reference non-existent classes — adjacent concern)
- Ops concern: future deploy guide cho Ollama instance sizing
