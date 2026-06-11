---
name: ai-branding-quality-gate
description: "Dùng khi PR thay đổi AI Branding behavior — model swap (Ollama → Bedrock, Llama → Gemma), prompt template change, AIClient/Step interface, ContentModerationService, InstanceQualityReviewer logic, 5 *QualityCheck classes. Auto-trigger qua audit-gate khi files match `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{ai,branding,instance,quality,moderation,provisioning}/**/*.java`. Manual checklist mode (5 sections × scoring) + delta-vs-baseline rubric. Output: `documents/04-quality/audits/ai-branding/YYYY-MM-DD-<change>.md`."
user-invocable: true
---

# AI Branding Quality Gate — Migration & Behavior Change Verification

Stub skill (Sub-PR 223.1). Manual checklist mode active until automated checks (real WCAG/vrg/ML) land per follow-up gaps GAP-226/227/228. Triggers on AI Branding code changes per `audit-gate.py` AUDIT_RULES.

**Why this skill exists:** AI Branding behavior change (model upgrade, prompt rewrite, provider swap) can silently regress tenant quality without trigger. `ai-branding-guidelines.md` §5 mandates 5 quality checks before DEPLOYED transition; §11 requires unit + integration + E2E. This skill verifies the change touches all 3 layers + provides delta-vs-baseline scoring.

## Process

### 0. Identify the change scope

Categorize change per `ai-branding-guidelines.md` §11.4 Migration test checklist:

| Change type | Risk | Required checks |
|-------------|:----:|-----------------|
| **Model swap** (Llama → Gemma, llama3.1 → llama3.2) | 🔴 P0 | All 5 sections below |
| **Prompt template rewrite** | 🟠 P1 | §1 + §2 + §4 |
| **Provider swap** (Ollama → Bedrock) | 🔴 P0 | All 5 + Adapter integration test |
| **§5 Quality Reviewer logic** | 🟠 P1 | §3 + §4 |
| **ContentModerationService logic** | 🟠 P1 | §2 + §4 |
| **Step class added/removed** | 🟡 P2 | §1 unit test |

### 1. Read existing baseline

Check `documents/04-quality/audits/ai-branding/`:
- `*-baseline.md` — last full-checklist score
- Last `YYYY-MM-DD-*.md` — most recent delta report

If first-ever audit for this domain → baseline file is `2026-04-26-baseline.md` (created Sub-PR 223.1).

### 2. Run 5-section checklist

Each section scored /20 → total /100. Below 70 = block migration.

#### §1. Output behavior consistency (/20)

For model swap / prompt change / provider swap:

- [ ] **VN content quality** — generate 5 sample outputs (3 audience: K-12 school / English center / driving school × 2 tones: professional / friendly). Score each output 0-4 against:
  - Cultural fit (VN education tone, no awkward translation feel) — 0-1
  - Length appropriate for target widget (banner 1 line, hero 3 sentences) — 0-1
  - Brand-safe wording (no slang, no profanity) — 0-1
  - Schema match (returns expected JSON structure for downstream Steps) — 0-1
- [ ] **5/5 sample outputs ≥3/4** = pass. Below = regression.
- [ ] **A/B vs baseline** — pick 1 baseline output, 1 new-model output for same prompt. Manual blind compare. New ≥ baseline = pass.

#### §2. Tool-calling / Schema integration (/20)

For provider swap or model upgrade with tool-calling change:

- [ ] **PlannerService.generatePlan()** still produces valid `Plan` JSON (snapshotTest) — `kiteclass-core/module/ai/workflow/PlannerService.java`
- [ ] **AnalyzerService.analyze()** returns `AnalysisResult` with all required fields populated — `kiteclass-core/module/ai/workflow/AnalyzerService.java`
- [ ] **PlanExecutor** can execute returned plan without unhandled exceptions — `kiteclass-core/module/ai/workflow/PlanExecutor.java`
- [ ] **Step interface** contracts unchanged (or migration script provided) — `kiteclass-core/module/ai/workflow/Step.java`
- [ ] **Outbox events** fire correctly with new payload schema — `kiteclass-core/module/branding/events/BrandingEventPublisher.java`

#### §3. §5 Quality Gate compatibility (/20)

For §5 Quality Reviewer / ContentModerationService logic change:

- [ ] **InstanceQualityReviewer.review()** scaffold checks callable — `kiteclass-core/module/quality/service/InstanceQualityReviewer.java`
- [ ] **5 *QualityCheck Strategy classes** present + callable: `ContrastQualityCheck`, `CssVarsQualityCheck`, `AssetUrlsQualityCheck`, `VisualRegressionQualityCheck`, `LogoPlacementQualityCheck` (`kiteclass-core/module/quality/check/`)
- [ ] **Mock 5/5 PASS** scenario → score 100/100 → `markDeployed()` fires (via `InstanceLifecycleService`)
- [ ] **Mock <70/100** scenario → `markFailed()` fires + reason captured in `QualityReport` entity
- [ ] **ContentModerationService** 3-stage pipeline returns `ModerationStatus` correctly — `kiteclass-core/module/moderation/ContentModerationService.java`
- [ ] **Failure → AuditLog** entry created (verify via integration test)

#### §4. Resilience & fallback (/20)

For any AI behavior change:

- [ ] **CircuitBreaker fallback** — disable AI provider → `templateFallback` activates → STATIC/TEMPLATE path returns cached/default theme
- [ ] **Bulkhead isolation** — concurrent 4-worker test (per `kitehub-branding` Oracle 24GB constraint) → no thread starvation
- [ ] **Retry policy** — verify exponential backoff config unchanged (or migration test added)
- [ ] **Timeout** — request >2 min triggers timeout + queues for retry
- [ ] **Tier rate-limit** — FREE 3/session, BASIC 10, PREMIUM 30 still enforced

#### §5. Tier-specific governance (/20)

For changes that may affect tier behavior:

- [ ] **FREE tier** — template-first routing intact (≥80% requests STATIC/TEMPLATE)
- [ ] **BASIC tier** — regenerate counter visible + decremented per regenerate
- [ ] **PREMIUM tier** — additional template variants accessible
- [ ] **ENTERPRISE tier** — Advanced Mode toggle + free-prompt opt-in still gated by `ai.enterprise.advancedModeEnabled` flag
- [ ] **Free-form prompt BANNED for FREE/BASIC/PREMIUM** (per `ai-branding-guidelines.md` §2.1)

### 3. Score & decide

```
Total /100:
  ≥85 — ✅ PASS, ship migration
  70-84 — ⚠️ PASS WITH NOTES (file follow-up gaps for sub-issues <16/20)
  <70 — 🛑 BLOCK migration; fix issues, re-run gate
```

### 4. Write report

Save to `documents/04-quality/audits/ai-branding/YYYY-MM-DD-<change-name>.md`:

```markdown
# AI Branding Quality Gate — <Change Name> — YYYY-MM-DD

**Trigger:** <files changed / PR #>
**Change type:** <model swap / prompt / provider / §5 logic / etc>
**Verdict:** ✅ PASS / ⚠️ NOTES / 🛑 BLOCK
**Score:** XX/100

## Section scores
| §  | Title | Score | Notes |
|---|-------|:-----:|-------|
| 1 | Output consistency | XX/20 | ... |
| 2 | Tool-calling / schema | XX/20 | ... |
| 3 | §5 Quality Gate compat | XX/20 | ... |
| 4 | Resilience & fallback | XX/20 | ... |
| 5 | Tier governance | XX/20 | ... |

## Sample outputs (§1)
<paste 5 outputs, scored>

## Issues found
<bullet list of items <16/20; reference follow-up gaps if filed>

## Delta vs baseline
<paste table: baseline score vs current per section>
```

## Gotchas

- **Manual mode is the default until GAP-226/227/228 ship.** Real WCAG measurement, visual regression diff, ML classifier scoring require infra (theme JSON, screenshot service, HTTP client) — those land Wave 8+. Do NOT mark §1 §3 sections "automated PASS" without those infra.
- **Sample outputs must be FRESH per audit run.** Don't reuse outputs from baseline; new-model behavior may differ.
- **5 sample outputs minimum for §1.** Smaller sample = unreliable scoring.
- **For provider swap (Ollama → Bedrock), §2 Adapter integration test is MANDATORY.** Different provider = different response schema = `Adapter` class changes.
- **§5 free-form prompt rule (BANNED for FREE/BASIC/PREMIUM tier) cannot be loosened without ENTERPRISE-only opt-in flag.** Verify config flag intact.
- **Score 100/100 is suspicious.** External auditor typically scores 15-20pts lower than self-audit (per memory `feedback_audit_calibration.md`). If self-score = 100, request peer/AI second-opinion before merge.

## Skill Contents

- `SKILL.md` — this file (process + 5 sections + scoring rubric)
- `documents/04-quality/audits/ai-branding/2026-04-26-baseline.md` — first-run baseline scoring current state (referenced by §3 delta-vs-baseline)

## When NOT to use this skill

- **PR docs-only** (no Java changes in `kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}/`) — audit-gate auto-skips per `is_docs_only()` rule
- **PR touches `kiteclass-core/src/test/`** only — test changes don't trigger behavior regression risk
- **PR touches `kitehub-branding/`** (legacy v1 module — rate limit, jobs, templates) — use generic audits per existing AUDIT_RULES; v2 redesign landed in kiteclass-core, not here
- **PR touches `kiteclass-frontend/` AI Branding pages** (HTML/CSS/TSX) — use `ui-review` skill instead

## Module location note (verified 2026-04-26 GAP-016 sweep)

V2 AI Branding implementation landed in `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/`, NOT in `kitehub/kitehub-branding/` as originally architected in `documents/02-architecture/ai-branding-v2-redesign.md`. The kitehub-branding module retains v1 only (rate limit + jobs + template gallery + AIBrandingService). This skill targets actual implementation location; architecture doc may need follow-up sync.

## Related

- `ai-branding-guidelines.md` §5 (Quality Gate 5 checks), §11 (Testing Requirements + §11.4 Migration test checklist)
- `output-review-mandate.md` matrix line 75 ("AI-generated assets" row)
- `audit-gate.py` AUDIT_RULES rule entry for `ai-branding-quality-gate`
- GAP-223 (filed this skill via Option C Sub-PR 223.1)
- GAP-225 (umbrella; cluster C3 covered by this skill)
- GAP-226/227/228 (real WCAG/vrg/ML follow-ups — Wave 8+ scope)
- `ui-review` skill — template inspiration for this skill structure
