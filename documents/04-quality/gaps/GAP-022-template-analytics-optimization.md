# GAP-022: Template Analytics & Optimization

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Analytics / Product
**Detected:** 2026-04-14 (simulation)

## Problem

Không có data-driven template optimization:

- ❌ Không metric "template nào được chọn nhiều nhất"
- ❌ Không regeneration rate per template (high = template quality kém)
- ❌ Không A/B testing framework cho variants
- ❌ Không user satisfaction survey post-branding
- ❌ Không feedback loop để improve templates

## Proposed Metrics

```sql
-- Per template usage
SELECT template_id, COUNT(*) as selections, AVG(regenerations) as avg_regen_rate
FROM branding_resources
GROUP BY template_id
ORDER BY selections DESC;

-- Abandonment per template
SELECT template_id,
  COUNT(*) FILTER (WHERE status = 'DEPLOYED') as deployed,
  COUNT(*) FILTER (WHERE status = 'ABANDONED') as abandoned,
  ROUND(100.0 * abandoned / NULLIF(deployed + abandoned, 0), 2) as abandon_rate
FROM branding_sessions
GROUP BY template_id;

-- User satisfaction
SELECT template_id, AVG(rating) as avg_rating, COUNT(*) as votes
FROM branding_feedback
GROUP BY template_id;
```

## Analytics Dashboard

### Template Performance
- Top 10 templates by usage
- Bottom 10 (candidates for removal)
- Regeneration rate heatmap (template × resource type)
- Abandonment funnel per template

### User Feedback
- Post-deploy satisfaction survey (1-5 stars)
- Free text feedback
- Common complaints categorized

### A/B Testing

```java
@Service
public class TemplateABTestService {
  public ImageTemplate selectVariant(String userId, String templateGroupId) {
    // Bucket user based on hash
    var bucket = hash(userId, templateGroupId) % 100;
    if (bucket < 50) return variantA;
    else return variantB;
  }

  public void recordOutcome(String userId, String templateId, Outcome outcome) {
    // Track: selected, regenerated, deployed, satisfaction
  }
}
```

## Feedback Loop

Quarterly review:
1. Identify underperforming templates (high regen rate, low satisfaction)
2. Create A/B variants với improvements
3. Test 2 weeks
4. Retire losers, roll out winners

## Acceptance Criteria

- [ ] Usage metrics tracked per template
- [ ] Regeneration rate per template + resource type
- [ ] Post-deploy satisfaction survey (optional, 1-click)
- [ ] Analytics dashboard for product team
- [ ] A/B test framework implemented
- [ ] Quarterly review process documented

## Dependencies

- GAP-011 (templates must exist first)
- GAP-019 (metrics infrastructure)

## Log

- 2026-04-14 — Gap in data-driven product improvement
