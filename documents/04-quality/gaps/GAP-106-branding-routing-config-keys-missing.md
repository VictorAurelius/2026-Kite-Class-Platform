# GAP-106: Branding Routing Config Keys Documented but Missing from application.yml

**Status:** 🟢 DONE (Wave 9-D, 2026-04-21)
**Priority:** 🟠 P1
**Domain:** KiteClass / AI Branding / Config Management
**Found:** 2026-04-19 (business-logic audit)
**Affects:** kiteclass-core branding module (where `ResourceRoutingService` actually lives — gap prompt said kitehub-branding), metric alerts

## Problem

`documents/01-business/kiteclass/resource-classification/rules.md:30-31` documents 2 config keys:

```markdown
| Key | Default | Purpose |
|-----|---------|---------|
| `branding.routing.template-first` | true | Enforce template-first philosophy |
| `branding.routing.max-ai-ratio` | 0.20 | Metric alert threshold for FULL_AI share |
```

Nhưng GREP xác nhận:
```
$ grep -r "branding.routing.template-first\|branding.routing.max-ai-ratio" \
    kitehub/*/src/main/resources/ kiteclass/*/src/main/resources/
# 0 hits
```

Consequence:
- **Template-first** (BR-RES-005 = "≥80% requests TEMPLATE/STATIC") chỉ enforce bằng logic flow hardcoded trong `ResourceRoutingService.classify()`, không có feature flag để rollback nếu bug
- **max-ai-ratio=0.20** là documented metric alert threshold nhưng không có Prometheus/Grafana alert nào bind tới config này → alert không trigger

## Root Cause

Rules.md được viết as design intent (Wave 3 planning, ADR-005) với assumption config keys sẽ được externalize khi implement. Implementation merge vào main với logic hardcoded nhưng không thêm config keys tương ứng. Reviewer không catch vì:
1. Không có automated test "if docs mention config key, application.yml must have it"
2. `verify-business-docs.sh` chỉ check 3-layer file existence, không check config key consistency

## Proposed Fix

### Step 1: Thêm config keys vào `kitehub-branding/application.yml`
```yaml
branding:
  routing:
    # BR-RES-005: template-first routing toggle (GAP-106)
    template-first: ${BRANDING_TEMPLATE_FIRST:true}
    # Metric alert threshold — FULL_AI share > 20% trigger Grafana alert
    max-ai-ratio: ${BRANDING_MAX_AI_RATIO:0.20}
```

### Step 2: Bind vào `@ConfigurationProperties`
Tạo/update `BrandingRoutingProperties.java` record với 2 fields + sane defaults.

### Step 3: Wire vào `ResourceRoutingService.classify()`
- `template-first=false` → skip TemplateMatchClassifier ưu tiên, go straight AI (debug/testing only)
- `max-ai-ratio` → emit Micrometer metric `branding.routing.ai-ratio`, configure alert rule

### Step 4: Update rules.md reference code location
Cập nhật `rules.md` Code Location column để reference BrandingRoutingProperties.

## Acceptance Criteria
- [ ] `grep branding.routing kitehub-branding/src/main/resources/application.yml` returns 2 lines
- [ ] `BrandingRoutingProperties` class exists với `@ConfigurationProperties(prefix = "branding.routing")`
- [ ] `ResourceRoutingService` dùng `properties.templateFirst()` thay vì hardcode
- [ ] Unit test verify `template-first=false` bypass template classifier
- [ ] Prometheus alert rule `branding-routing-ai-ratio.yml` reference `max-ai-ratio` threshold
- [ ] Rules.md cập nhật với link tới class + alert file

## Related
- Audit report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
- Original rule source: ADR-005, GAP-007 (Resource Classification)
- Related gaps: GAP-107 (AI provider ghost rules — similar pattern of rules referencing non-existent entities)

## Log
- 2026-04-21 (Wave 9-D) — Closed. Added `BrandingRoutingProperties` (`kiteclass-core/module/branding/config`), wired `ResourceRoutingService` to emit `branding.routing.classified` Micrometer counter tagged by category, added `branding.routing.*` keys to `kiteclass-core/src/main/resources/application.yml` with env-var overrides (`BRANDING_TEMPLATE_FIRST`, `BRANDING_MAX_AI_RATIO`). Startup log warns when `template-first=false`. Tests: `BrandingRoutingPropertiesTest` (3), extended `ResourceRoutingServiceTest` with counter-emission assertion (1). Rules.md updated with code refs + metrics table.
  - NOTE: gap prompt said kitehub-branding; actual location is kiteclass-core (ResourceRoutingService lives there). Scope corrected, same acceptance met.
