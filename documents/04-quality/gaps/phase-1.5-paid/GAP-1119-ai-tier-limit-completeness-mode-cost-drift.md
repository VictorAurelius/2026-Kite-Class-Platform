# GAP-1119: AI tier-limit completeness — AI-mode column + GPT-5.5 cost-differentiation + PREMIUM regen drift + route new AI calls qua guards

**Status:** 🔵 OPEN
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

## Acceptance Criteria

- [ ] SUB-22 matrix có cột AI-mode (TEMPLATE all / FULL_AI ENTERPRISE-only) + enforce gate
- [ ] FULL_AI (GPT-5.5) cap riêng + cost metric; CircuitBreaker fallback TEMPLATE
- [ ] PREMIUM regen drift reconciled (30 vs 50 chốt 1 giá trị, code+matrix sync); session/day label thống nhất
- [ ] Mọi AI call mới ([[GAP-1117]]) qua AIInputCapService + AIRateLimitService; IT verify reject khi hết quota / sai tier

## Related

- Discovered in: discuss AI tier limits 2026-06-10 (user "tránh miss" khi wire AI thật)
- Canonical: `subscription-billing/rules.md` SUB-22 + `ai-branding-guidelines.md` §2.4/§2.5/§4.3 + ADR-037 (cost) + GAP-260 (multiplier) + GAP-005 (fair-scheduling)
- Depends/feeds: [[GAP-1117]] (real AI wiring — enforce guards here)
- Guards: `AIInputCapService` / `AIRateLimitService` / `DistributedRateLimiter`

## Log

- **2026-06-10:** Filed khi rà SUB-22 matrix lúc wire AI thật ([[GAP-1117]]). 3 miss: AI-mode column thiếu, GPT-5.5 cost không phân biệt, PREMIUM regen drift 30(canonical)/50(code) + risk new-call bypass guards. Per `discovery-to-gap-inline-filing.md`. GAP-ID block reserve 1119-1120.
