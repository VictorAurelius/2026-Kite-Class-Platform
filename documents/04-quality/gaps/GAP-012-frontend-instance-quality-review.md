# GAP-012: Automated Frontend Instance Quality Review (post-branding)

**Status:** 🟢 DONE (Wave 4 Sub-PR 4.5, merged 2026-04-14; InstanceQualityReviewer + 5 Strategy-pattern checks + QualityReviewStep in PlannerService. Scaffolded checks (contrast/vrg/url-ping) slated for follow-up when theme JSON + screenshot service + HTTP client land.)
**Priority:** 🟠 P1
**Domain:** Quality / AI / Frontend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md` §4
- `.claude/skills/quality/ui-review/SKILL.md` (existing UI audit skill — reuse pattern)

## Problem

Sau khi AI branding apply lên frontend instance, **không có mechanism tự động** verify chất lượng. Có thể tenant được deploy với:
- Contrast quá thấp (unreadable)
- Logo render sai size/cắt
- Theme CSS vars không apply đúng (default colors còn sót)
- Broken asset URLs (404)
- Responsive broken (mobile không đọc được)

Không có quality gate → tenant user thấy UI xấu → churn.

## Evidence

- `BrandingJob` completion chỉ check technical success (AI call OK) — không check visual output
- Không có Playwright test chạy sau deploy
- `JobStatus = COMPLETED` không đồng nghĩa với "instance looks good"
- Existing `ui-review` skill chỉ dùng manual, chưa integrate vào pipeline

## Proposed Fix: Automated Quality Review Pipeline

### Pipeline flow

```
BrandingJob status: GENERATING
      ↓
Resources generated + stored
      ↓
Deploy to instance.{slug}.kiteclass.com
      ↓
┌──────────────────────────────────────────────┐
│     AUTOMATED QUALITY REVIEW (NEW)           │
│                                              │
│  1. Playwright launch                        │
│  2. Capture screenshots 4 key pages × 2 BPs  │
│     - landing, login, dashboard, course      │
│     - desktop (1440px), mobile (375px)       │
│  3. Run quality checks:                      │
│     - WCAG contrast analyzer                 │
│     - CSS variables applied check            │
│     - Broken asset URLs scan                 │
│     - Visual regression vs baseline          │
│     - Logo placement check                   │
│  4. Score /100                               │
└──────────────┬───────────────────────────────┘
               ↓
        ┌──────┴──────┐
        │             │
     ≥70 ✓       <70 ✗
        │             │
        ▼             ▼
   DEPLOYED     FAILED
                + alert ops
                + regenerate
```

### Quality Check Rules

```java
public class InstanceQualityReviewer {

  public QualityReport review(String instanceId) {
    var score = 100;
    var issues = new ArrayList<Issue>();

    // Check 1: WCAG contrast (30 points)
    for (var page : KEY_PAGES) {
      var contrastIssues = checkContrast(page);
      score -= contrastIssues.size() * 5;
      issues.addAll(contrastIssues);
    }

    // Check 2: Theme CSS vars (20 points)
    var cssCheck = checkCssVariables(instanceId);
    if (!cssCheck.primaryApplied) { score -= 10; issues.add(...); }
    if (!cssCheck.secondaryApplied) { score -= 5; issues.add(...); }
    if (!cssCheck.fontsLoaded) { score -= 5; issues.add(...); }

    // Check 3: Asset URLs (15 points)
    for (var url : getBrandingAssetUrls(instanceId)) {
      if (fetch(url).status != 200) {
        score -= 5;
        issues.add(new BrokenAssetIssue(url));
      }
    }

    // Check 4: Visual regression (20 points)
    var diffScore = compareWithBaseline(instanceId);
    if (diffScore < 80) { score -= 20; issues.add(new VisualRegressionIssue()); }

    // Check 5: Logo quality (15 points)
    var logoCheck = analyzeLogoPlacement(instanceId);
    if (logoCheck.cropped) score -= 10;
    if (logoCheck.tooSmall) score -= 5;

    return new QualityReport(score, issues);
  }
}
```

### Thresholds

| Score | Action |
|-------|--------|
| 90-100 | ✅ Auto-deploy, mark DEPLOYED |
| 70-89 | ⚠️ Deploy with warnings, alert tenant + ops |
| 50-69 | 🟠 Block deploy, auto-regenerate (1 retry) |
| <50 | 🔴 Block deploy, mark FAILED, require manual review |

### Implementation

- **Backend service:** `InstanceQualityReviewer` trong kitehub-branding
- **Playwright runner:** Docker container `kite-quality-checker` chạy khi có event `branding.completed`
- **Visual baseline:** Store reference screenshots trong S3 per template set
- **Report storage:** `instance_quality_reports` table với score, issues, screenshots

### Integration với Lifecycle (GAP-009)

```
GENERATING → DEPLOYED chỉ qua quality gate:

BrandingJob COMPLETED
  ↓
QualityReviewer.review(instanceId)
  ↓
  score ≥ 70 → InstanceStatus.DEPLOYED
  score < 70 → InstanceStatus.FAILED (trigger retry/alert)
```

### Tenant visibility

Dashboard tenant show quality score + issues:
```
Your instance quality: 87/100 ✓
Issues found:
  ⚠ Banner contrast slightly low (4.2:1, recommend 4.5:1)
  ⚠ Logo padding tight on mobile
[Regenerate] [Accept anyway]
```

## Skills/Rules cần tạo

Bổ sung vào `.claude/skills/quality/`:
- `instance-quality-review.md` — skill chạy quality check + generate report
- `.claude/rules/instance-quality-standards.md` — thresholds, check criteria

Tái sử dụng pattern từ `ui-review/SKILL.md` (đã tồn tại cho manual UI audit).

## Acceptance Criteria

- [ ] `InstanceQualityReviewer` service implemented
- [ ] Playwright runner containerized (`kite-quality-checker`)
- [ ] 5 quality checks (contrast, CSS vars, assets, visual regression, logo)
- [ ] Integration with `BrandingJob` completion (auto-trigger)
- [ ] Integration with `InstanceStatus` lifecycle (gate DEPLOYED)
- [ ] Quality report stored + viewable trong admin dashboard
- [ ] Tenant dashboard shows quality score + actionable issues
- [ ] Skill doc `.claude/skills/quality/instance-quality-review.md`
- [ ] Thresholds calibrated based on 10+ test instances

## Dependencies

- **Depends on GAP-009** (instance lifecycle) — quality review là 1 state transition
- **Depends on GAP-010** (FE integration) — cần URLs để Playwright test
- **Depends on GAP-011** (template library) — baseline screenshots per template

## Log

- **2026-04-26** — **Governance closure tracked: [GAP-225](GAP-225-scaffolded-as-done-governance-closure-umbrella.md)** (Scaffolded-as-DONE Governance Closure Umbrella) + [GAP-223](GAP-223-ai-branding-migration-verification-governance.md) Sub-PR 223.1 scope. Scaffold debt: "Scaffolded checks (contrast/vrg/url-ping) slated for follow-up when theme JSON + screenshot service + HTTP client land". Status preserved 🟢 DONE for audit trail; this gap is part of Cluster C3 (AI Branding Quality Gates, paired with GAP-018) — covered by GAP-223 Option C plan (skill `quality/ai-branding-quality-gate/` + audit-gate rule + matrix line 75 sync). `output-review-mandate.md` line 75 synced this PR from "PLANNED" → "PARTIAL" reflecting actual scaffold state. No code change this PR — docs truth-up only.
- 2026-04-14 — Phát hiện cần automated review trong AI branding pipeline (user raised)
