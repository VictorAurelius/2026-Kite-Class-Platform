# GAP-1119: AI tier-limit completeness — AI-mode column + GPT-5.5 cost-differentiation + PREMIUM regen drift + route new AI calls qua guards

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (user-flagged khi wire AI thật: "check lại limit loại AI + số lần dùng theo tier, tránh miss")
**Affects:** `subscription-billing/rules.md` SUB-22 matrix + `kitehub-branding` AIRateLimitService/AIInputCapService/DistributedRateLimiter + `application.yml` branding.rate-limit + GAP-1117 generation wiring

## Problem

Khi wire AI thật (Gemini free template + GPT-5.5 full-AI per ADR-037 amendment [[GAP-1117]]), rà ma trận limit theo tier (SUB-22) lộ **3 miss** + 1 risk:

### #1 — Matrix thiếu cột "AI mode" (TEMPLATE vs FULL_AI access per tier)
SUB-22 matrix (`subscription-billing/rules.md`) có cột regen/input-cap/multiplier nhưng **KHÔNG có cột AI-mode**. `ai-branding-guidelines.md` §2.4 nói full-prompt (giờ = GPT-5.5 FULL_AI) chỉ ENTERPRISE — nhưng quy tắc này sống ở prose §2.4, chưa vào canonical matrix → dễ drift. Cần thêm cột: **TEMPLATE (Gemini) = mọi tier; FULL_AI (GPT-5.5) = ENTERPRISE only**.

### #2 — Không phân biệt cost theo LOẠI AI (Gemini free $0 vs GPT-5.5 paid)
Regen/rate-limit/input-cap hiện **generic per tier**, không phân biệt AI type. `AIRateLimitService` grep = 0 per-type gate. Nhưng GPT-5.5 tốn tiền thật/call (Gemini $0). ADR-037 Consequences: *"GPT 5.5 banner cost: monitor cost/banner; cap regen quota per tenant (GAP-005 fair-scheduling)"*. → FULL_AI (GPT-5.5) cần **cap riêng/tighter** (separate quota OR weight nặng) + cost metric. Hiện miss.

### #3 — PREMIUM regen DRIFT: canonical 30 vs code 50
- SUB-22 matrix + §4.3: PREMIUM branding regen = **30**.
- Code `application.yml` `branding.rate-limit.premium-per-day` = **50**.
→ 2 source lệch (30 vs 50). Cần chốt giá trị canonical (mặc định SUB-22=30) + sync code. Thêm: §4.3 nhãn "per session" vs SUB-22 nhãn "per ngày" — concept drift (session vs day) cần thống nhất.

### #4 (risk) — New AI calls có thể bypass guards
[[GAP-1117]] WIZARD-BE wire Gemini/GPT-5.5 mới. PHẢI verify mọi call đi qua `AIInputCapService` (§2.5) + `AIRateLimitService`/`DistributedRateLimiter` (regen §4.3 + per-day) — không bypass.

## Proposed Fix

1. **#1** Thêm cột "AI mode (TEMPLATE/FULL_AI)" vào SUB-22 matrix: TEMPLATE = FREE/BASIC/PREMIUM/ENTERPRISE; FULL_AI(GPT-5.5) = ENTERPRISE only (cite §2.4). Enforce gate ở generation routing.
2. **#2** AI-type-differentiated cost cap: FULL_AI (GPT-5.5) quota riêng (vd per-tenant per-month) + Micrometer cost metric `ai.fullai.call{tier}`; CircuitBreaker fallback TEMPLATE. Per GAP-005 fair-scheduling.
3. **#3** Chốt PREMIUM regen = 30 (canonical SUB-22) → sync `application.yml` `premium-per-day` 50→30 (HOẶC user chốt 50 → update matrix); thống nhất nhãn session-vs-day.
4. **#4** Verify GAP-1117 generation route mọi AI call qua input-cap + rate-limit guards; add IT chứng minh FREE hết 3 regen → reject; FULL_AI non-ENTERPRISE → reject.

## Decision (user 2026-06-10)

- **PREMIUM regen = 30** (chốt canonical SUB-22) → sync code `application.yml branding.rate-limit.premium-per-day` 50→30.
- **FULL_AI (GPT-5.5) = PREMIUM + ENTERPRISE** (nới §2.4 từ ENTERPRISE-only): PREMIUM được FULL_AI nhưng **quota riêng nhỏ** (vd 3-5 banner GPT-5.5/tháng, cap cost chặt); ENTERPRISE unlimited. FREE/BASIC = TEMPLATE (Gemini) only.
- → Cập nhật prospectively: `ai-branding-guidelines.md` §2.4 + ADR-037 amendment + SUB-22 matrix AI-mode column khi implement.

## Acceptance Criteria

- [x] SUB-22 matrix có cột AI-mode (`subscription-billing/rules.md`): TEMPLATE = mọi tier; FULL_AI = **PREMIUM (5/tháng) + ENTERPRISE (∞)** + enforce gate `GenerationMode.forTier`; §2.4 (`ai-branding-guidelines.md` v1.3.0) + ADR-037 amendment synced
- [x] FULL_AI cost cap: `FullAiQuotaService` PREMIUM quota riêng (`ai.rate-limit.fullai-premium-per-month` mặc định 5) + ENTERPRISE unlimited + cost metric `ai.fullai.call{tier,outcome}` (Micrometer); CircuitBreaker fallback TEMPLATE (ResilientAIClient + no-key/quota-exceeded → TEMPLATE)
- [x] PREMIUM regen = 30 (code `AIRateLimitConfig.premiumPerDay` + `application.yml branding.rate-limit.premium-per-day` 50→30 sync matrix; `AIRateLimitConfigTest.defaultValues_areCorrect` verify 30)
- [x] Mọi AI call qua AIInputCapService (WIZARD-BE wired §2.5) + tier-gate verify: `GenerationModeTest` (PREMIUM/ENT→FULL_AI, FREE/BASIC→TEMPLATE) + `FullAiQuotaServiceTest` (FREE/BASIC reject quota=0; PREMIUM cap; ENT unlimited) + `AIBrandingProcessorTest` (PREMIUM quota-exhausted→TEMPLATE, BASIC ineligible→TEMPLATE). FREE 3-regen reject = `AIRateLimitConfig.freePerDay=3` existing

## Related

- Discovered in: discuss AI tier limits 2026-06-10 (user "tránh miss" khi wire AI thật)
- Canonical: `subscription-billing/rules.md` SUB-22 + `ai-branding-guidelines.md` §2.4/§2.5/§4.3 + ADR-037 (cost) + GAP-260 (multiplier) + GAP-005 (fair-scheduling)
- Depends/feeds: [[GAP-1117]] (real AI wiring — enforce guards here)
- Guards: `AIInputCapService` / `AIRateLimitService` / `DistributedRateLimiter`

## Log

- **2026-06-10 (DONE):** Tier-limit enforcement shipped trên wave/branding-fix-2026-06-10 (post WIZARD-BE integrate). Code: `GenerationMode.forTier` ENTERPRISE-only → PREMIUM+ENTERPRISE gate; new `FullAiQuotaService` (PREMIUM monthly cap qua Redis `DistributedRateLimiter.incrementMonthlyFullAiUsage` + graceful fail-open; ENTERPRISE unlimited; FREE/BASIC quota=0); `AIBrandingProcessor.generateBanner` quota-gate + `recordFullAiCall` cost metric `ai.fullai.call{tier,outcome}` (MeterRegistry); `AIRateLimitConfig` premium-per-day 50→30 + `fullai-{premium,enterprise}-per-month`. Doc: SUB-22 matrix AI-mode column + `ai-branding-guidelines.md` §2.4 v1.3.0 + ADR-037 amendment. Tests: GenerationModeTest 12 + FullAiQuotaServiceTest 6 + AIBrandingProcessorTest 10 (+3 GAP-1119) + AIRateLimitConfigTest 9 (+1 fullai). **Verify: surefire 316 green + `mvn clean verify -P strict-warnings` BUILD SUCCESS exit 0.** Live FULL_AI cost behavior end-to-end gated by [[GAP-1117]] real provider wiring (rasterise stub + dev keys) — enforcement code + unit tests complete. Per `gap-done-discipline.md` §2 (AC verified) + `gap-folder-organization.md` v2.0.0 §3.3 (git mv → phase-1.5-paid/closed/).
- **2026-06-10:** Filed khi rà SUB-22 matrix lúc wire AI thật ([[GAP-1117]]). 3 miss: AI-mode column thiếu, GPT-5.5 cost không phân biệt, PREMIUM regen drift 30(canonical)/50(code) + risk new-call bypass guards. Per `discovery-to-gap-inline-filing.md`. GAP-ID block reserve 1119-1120.
