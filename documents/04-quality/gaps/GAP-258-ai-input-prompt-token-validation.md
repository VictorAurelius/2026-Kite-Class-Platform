# GAP-258: AI Input Prompt Token Validation

**Status:** 🟢 DONE 2026-04-28
**Priority:** 🟠 P1
**Domain:** Security / AI / Cost
**Found:** 2026-04-28 (article-driven check vs `kitehub-branding` AI client state)
**Affects:** kitehub-branding AI cost surface (Ollama + OpenAI providers); cost-attack vulnerability

## Problem

`kitehub-branding`'s AI clients (`OpenAIClient`, `OllamaClient`, `ResilientAIClient`) cap **output** tokens (`max_tokens=1000` for vision analysis, `max_tokens=500` for text gen) but do not validate **input** prompt length before forwarding to provider. An attacker (or runaway client) can send a prompt of arbitrary length, costing money on the inbound side.

**Evidence:**
- `kitehub-branding/src/main/java/com/kitehub/branding/client/OpenAIClient.java` lines 78, 148 — only `max_tokens` (output) parameter set
- No prompt-length validation in `AIBrandingService` callsites
- `AIRateLimitService` counts requests, not tokens
- Per-tier daily cap of 3 / 10 / 50 / -1 requests can be saturated with 100k-token prompts → far worse cost than the request-count cap suggests

## Root Cause

Tier rate limit was scoped to "request count" only. Token-level budget was assumed to come later (GAP-019 / GAP-017 cover cost tracking, but neither *enforces* an input cap — only measures usage post-fact).

Article cited 2026-04-28: "Cost Attack" — DDoS the cost surface with valid-looking but expensive requests. Bài viết chỉ ra: "không giới hạn token đầu vào" là một trong 5 sai lầm phổ biến.

## Proposed Fix

1. Add config key `ai.input.max-tokens` (default 4000, override per provider/feature in `application.yml`).
2. Add util `PromptTokenEstimator` (cl100k_base or simple `length / 4` heuristic for v1).
3. In `AIBrandingService` (and `ContentGenerationService`) callsites, reject before invoking client when estimate > cap → return 400 `INPUT_TOO_LONG` with size + cap in error body.
4. Tier-aware caps: FREE 2000 / BASIC 4000 / PREMIUM 8000 / ENTERPRISE configurable. Wire into `AIRateLimitConfig`.
5. Emit Micrometer counter `ai_input_token_rejection_total{tier}` for observability (Prometheus alert pattern available).
6. Smoke test: integration test `BrandingControllerInputCapIT` POSTs oversize prompt → assert 400.

## Acceptance Criteria

- [x] `ai.input.max-tokens` config key with tier-aware overrides — `AIInputCapConfig` (`@ConfigurationProperties(prefix = "ai.input")`) with `free|basic|premium|enterprise-max-tokens`; env-overridable via `AI_INPUT_*_MAX_TOKENS`
- [x] Rejection path returns 400 with structured error before any provider call — `AIInputCapService#checkInputSize` invoked in all 4 endpoints AFTER rate-limit check, BEFORE `recordUsage`
- [x] Counter `ai.input.token.rejection{tier}` emitted (visible at `/actuator/prometheus` via Micrometer; Prom adopts `_total` suffix per convention)
- [x] Integration test asserts FREE-tier oversize prompt → 400 — `BrandingControllerInputCapIT` 3 tests (oversize reject / within-cap allow / FREE-vs-PREMIUM differential)
- [x] `documents/01-business/kiteclass/ai-agent-workflow/rules.md` updated with BR-INPUT-CAP-001..007 entries + counter row in metrics table + 4 config keys
- [x] `.claude/rules/ai-branding-guidelines.md` §2.5 added — MANDATORY rule with rationale + tier table + counter contract

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Real BPE tokenizer (cl100k / o200k) | Stage 2 — start with char/4 heuristic, swap when tiktoken-java lands |
| Per-feature caps (vision vs text) | Stage 2 — first ship single global tier cap |
| Token usage telemetry → billing | Tracked GAP-019 (observability) + GAP-017 (billing) |

## Related

- Source: 2026-04-28 article "Những lỗi 'chết người' khi build AI backend (Phần 2) — Không rate limit → mất tiền" (LinkedIn-style writeup, anonymous author cited by user)
- Code: `kitehub-branding/src/main/java/com/kitehub/branding/client/OpenAIClient.java` lines 78, 148
- Config: `kitehub-branding/src/main/java/com/kitehub/branding/config/AIRateLimitConfig.java`
- Existing: `AIRateLimitService` (request-count cap), `AIQueueDispatcher` (per-tier queue routing), `ResilientAIClient` (CB)
- Related gaps: GAP-017 (billing), GAP-019 (observability), GAP-122 (alerts — `AIProviderHighFailureRate` may co-fire on rejection storms)
- Rules: `.claude/rules/ai-branding-guidelines.md` §2 (User Prompt Constraints)

## Log

- 2026-04-28 — SHIPPED. Single-PR implementation: `PromptTokenEstimator` util (chars/4 heuristic) + `AIInputCapConfig` (tier-aware caps) + `AIInputCapService` (guard with Micrometer counter) + 4 endpoint wiring in `AIBrandingController` + 13 unit tests + 3 integration tests + business rules `BR-INPUT-CAP-001..007` + metrics catalog row + ai-branding-guidelines.md §2.5 v1.1.0→1.2.0. Verification: 166/166 kitehub-branding tests green. UX impact analysis: ~25-40× headroom over typical wizard usage (50-100 tokens per request); only edge case is data URI logos (correct rejection — should pre-upload to S3). Real BPE tokenizer (tiktoken-java) deferred per §Out-of-scope.
- 2026-04-28 — Discovered via article state-check (CLAUDE-Cmd `/start-session` flow). State-check confirmed `OpenAIClient` line 78/148 only caps output tokens. No existing gap covers input-cap (GAP-017/019 cover post-fact cost tracking, not enforcement).
