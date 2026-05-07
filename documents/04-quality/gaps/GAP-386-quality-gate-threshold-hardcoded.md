# GAP-386: Quality gate `pass-threshold=70` hardcoded — vi phạm 12-factor config externalization

**Status:** 🔵 OPEN
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

- [ ] `application.yml` chứa `quality-gate.pass-threshold: 70`
- [ ] `QualityScoreAggregator` field annotated `@Value("${quality-gate.pass-threshold:70}")`
- [ ] Unit test: threshold=80 inject → score 75 returns FAIL; score 81 returns PASS
- [ ] Unit test: missing config → fallback 70
- [ ] `rules.md` §Quality Gate gets 5-attribute compliance block
- [ ] Helm `values.yaml` exposes override key
- [ ] `documents/01-business/kitehub/ai-branding/rules.md` BR-QUALITY-001 line numbers updated

## Related

- Source audit: `documents/04-quality/audits/business/2026-05-07-wave-34-ai-branding-business-logic.md` (Finding #1)
- Parent gap: GAP-272c (quality gate score aggregator endpoint — Wave 34)
- Rule: `.claude/rules/business-logic-review.md` v1.0.0 §2 (Source/Rationale/Reviewer/Compliance/Cadence)
- 12-factor: §III Config in environment

## Log

- **2026-05-07** Filed from Business Logic /100 audit Wave 34. State-check: GAP-272c covers aggregator endpoint scope but does NOT mention threshold externalization (grep on file confirmed only "deterministic v0 sub-scores" mentioned). Hardcoded threshold verified at `QualityScoreAggregator.java:33`.
