# GAP-386: Quality gate `pass-threshold=70` hardcoded — vi phạm 12-factor config externalization

**Status:** 🟢 DONE 2026-05-08 — Wave 35 Bucket C (PR pending). Threshold externalized via `@Value`, application.yml + Helm wiring + 4 new unit tests + BR-QUALITY-001 5-attribute compliance block in rules.md.
**Priority:** 🔴 P0 — chặn config flexibility + post-deploy A/B testing trên gate threshold
**Domain:** Backend / 12-factor compliance / Business Logic correctness
**Found:** 2026-05-07 (Business Logic /100 audit Wave 34 — agent ad3b6e89)
**Affects:** `kitehub-branding` AI Branding deploy gate — every instance deploy decision

## Problem

`kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/quality/QualityScoreAggregator.java:33`:

```java
private static final int THRESHOLD = 70;
```

**Vấn đề:**
1. Pass threshold 70/100 hardcoded → thay đổi yêu cầu recompile + redeploy (vi phạm 12-factor §III "Store config in environment")
2. KHÔNG inject từ `application.yml` → blocks A/B testing threshold post-deployment
3. `documents/01-business/kitehub/ai-branding/rules.md` BR-QUALITY-001 declare config key `quality-gate.pass-threshold` nhưng key này KHÔNG tồn tại trong `application.yml` → silent drift giữa rules.md và code

**Impact:** 
- Production tweak threshold → require Maven rebuild + Helm upgrade + restart
- Cannot canary new threshold per tenant tier (FREE 65 vs ENT 80, etc.)
- Audit gap: rules.md claim externalized, code không thực hiện

## Root Cause

Wave 34 Bucket B (PR #906 quality-score) ship aggregator scaffold với deterministic v0 sub-scores (per GAP-272c). Threshold extracted as constant nhưng chưa wire `@Value` injection. `audit-to-gap-pipeline.md` Step 2.5 state-check tại Wave 34 plan time có thể đã miss config externalization mandate.

## Proposed Fix

1. **Add config key** to `kitehub/kitehub-branding/src/main/resources/application.yml`:
   ```yaml
   quality-gate:
     pass-threshold: 70
   ```

2. **Inject via `@Value`** in QualityScoreAggregator:
   ```java
   @Value("${quality-gate.pass-threshold:70}")
   private int threshold;
   ```
   Default 70 fallback giữ backward compat khi env var thiếu.

3. **Update rules.md** §Quality Gate (line 90) với 5-attribute block:
   - Source: ai-branding-guidelines.md §5
   - Rationale: empirical observation; tunable per tenant tier post-launch
   - Reviewer: @nguyenvankiet (acting Product Owner solo-dev) — formal counsel review N/A
   - Compliance: N/A (pure threshold, no PDPL/Consumer Protection trigger)
   - Review cadence: Quarterly OR on threshold change PR

4. **Helm values.yaml** — expose `qualityGate.passThreshold` cho prod override

## Acceptance Criteria

- [x] `application.yml` chứa `quality-gate.pass-threshold: ${QUALITY_GATE_PASS_THRESHOLD:70}` (cao hơn spec — env-var indirection cho 12-factor compliance)
- [x] `QualityScoreAggregator` field annotated `@Value("${quality-gate.pass-threshold:70}")`
- [x] Unit test: custom threshold (composite+5) inject → score returns FAIL; threshold (composite-5) → PASS (`QualityScoreAggregatorThresholdTest#customThresholdAffectsPassDecision`)
- [x] Unit test: missing config → fallback 70 (`QualityScoreAggregatorThresholdTest#defaultThresholdIs70WhenNotInjected`)
- [x] `rules.md` §Quality Gate gets 5-attribute compliance block (Source/Rationale/Reviewer/Compliance/Cadence per `business-logic-review.md` v1.0.0 §2)
- [x] Helm `values.yaml` exposes override key (`branding.qualityGate.passThreshold`) + `deployment.yaml` env wiring `QUALITY_GATE_PASS_THRESHOLD`
- [x] `documents/01-business/kitehub/ai-branding/rules.md` BR-QUALITY-001 entry extended (config snippet + 5-attribute block appended to file)

## Related

- Source audit: `documents/04-quality/audits/business/2026-05-07-wave-34-ai-branding-business-logic.md` (Finding #1)
- Parent gap: GAP-272c (quality gate score aggregator endpoint — Wave 34)
- Rule: `.claude/rules/business-logic-review.md` v1.0.0 §2 (Source/Rationale/Reviewer/Compliance/Cadence)
- 12-factor: §III Config in environment

## Log

- **2026-05-08** Wave 35 Bucket C ship — Status flipped 🔵 OPEN → 🟢 DONE. Externalized via `@Value("${quality-gate.pass-threshold:70}")` + application.yml `quality-gate.pass-threshold: ${QUALITY_GATE_PASS_THRESHOLD:70}` + Helm chart `branding.qualityGate.passThreshold: 70` + deployment.yaml env-var wiring + 4 new unit tests in `QualityScoreAggregatorThresholdTest` (8 total in module test class — 4 new + 4 existing controller). Verification: `./mvnw -pl kitehub-branding -am verify -DskipITs` → BUILD SUCCESS, 220 tests pass, 0 failures. BR-QUALITY-001 5-attribute compliance block (Source/Rationale/Reviewer/Compliance/Cadence) appended to `documents/01-business/kitehub/ai-branding/rules.md` per `business-logic-review.md` v1.0.0 §2.
- **2026-05-07** Filed from Business Logic /100 audit Wave 34. State-check: GAP-272c covers aggregator endpoint scope but does NOT mention threshold externalization (grep on file confirmed only "deterministic v0 sub-scores" mentioned). Hardcoded threshold verified at `QualityScoreAggregator.java:33`.
