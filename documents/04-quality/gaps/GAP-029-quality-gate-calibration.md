# GAP-029: Quality Gate Calibration & Feedback Loop

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Quality / AI
**Detected:** 2026-04-14 (simulation)

## Problem

Quality gate threshold 70/100 (GAP-012) là **arbitrary**. Chưa có:

- ❌ Calibration based on real user feedback
- ❌ False positive rate tracked (block tốt nhưng gate reject)
- ❌ False negative rate tracked (quality xấu nhưng gate approve)
- ❌ User override "accept anyway" option
- ❌ Threshold có thể khác per tier (enterprise strict hơn)

**Risk:** Gate quá strict → tenant frustrated, queue regens vô ích. Gate quá lenient → bad UX deploy.

## Proposed Fix

### 1. Per-Tier Thresholds

```yaml
quality-gate:
  threshold:
    FREE: 60          # More lenient, limited AI quota
    PRO: 70           # Standard
    PREMIUM: 80       # Higher bar for paid
    ENTERPRISE: 85    # Strictest
```

### 2. User Feedback on Quality

Sau khi DEPLOYED, post-deploy survey:
```
How satisfied với branding?
  ⭐⭐⭐⭐⭐
  [Comment]
```

Track correlation giữa:
- Quality gate score
- User rating
- Regeneration rate trong first week

### 3. Override Mechanism

Nếu score 60-69 (marginal):
```
System: "Quality score 68/100. Accept?"
  [View issues] — show flagged items
  [Regenerate] — try again
  [Accept anyway] — deploy with warnings shown to tenant
```

### 4. False Positive/Negative Tracking

```java
@Entity
public class QualityGateOutcome {
  Long id;
  String instanceId;
  Integer gateScore;
  Boolean gateBlocked;
  Boolean userOverride;
  Integer userSatisfactionRating; // collected later
  Boolean regenerated;
  Timestamp createdAt;
}
```

Monthly review:
- Score <70 blocked, user override → if satisfaction ≥4 star = false positive
- Score ≥70 passed, user rating ≤2 star = false negative
- Adjust threshold based on data

### 5. Automated Calibration

```java
@Scheduled(cron = "0 0 1 1 * *") // 1st of month
public void calibrateQualityGate() {
  // Analyze last month data
  var fpRate = calcFalsePositiveRate();
  var fnRate = calcFalseNegativeRate();

  if (fpRate > 0.2) suggestDecreaseThreshold();
  if (fnRate > 0.1) suggestIncreaseThreshold();

  // Don't auto-apply — require admin approval
  alertAdmin(suggestion);
}
```

### 6. Weight per Check Type

Some issues matter more:
```yaml
check-weights:
  wcag-contrast: 3.0       # Critical (accessibility)
  broken-assets: 5.0       # Critical (broken UX)
  css-vars-missing: 2.0    # High
  visual-regression: 1.0   # Medium
  logo-placement: 1.5      # Medium-high
```

Total score = sum(check_pass × weight) / sum(weights) × 100

## Acceptance Criteria

- [ ] Per-tier thresholds configurable
- [ ] Post-deploy satisfaction survey
- [ ] Override mechanism for marginal scores
- [ ] `QualityGateOutcome` tracked
- [ ] Monthly calibration report
- [ ] Weight system for different check types
- [ ] Admin dashboard: quality gate metrics

## Dependencies

- GAP-012 (quality review infrastructure)
- GAP-022 (user feedback collection)

## Log

- 2026-04-14 — Arbitrary threshold identified, needs data-driven calibration
