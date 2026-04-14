# GAP-044: Synthetic Monitoring + Feature Flags

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / Release Management
**Detected:** 2026-04-14 (simulation: cross-cutting × C7 + C10)

## Problem

2 gaps operational riêng biệt nhưng related:

**A. Synthetic Monitoring:**
- ❌ Chỉ reactive monitoring (alert khi user report)
- ❌ Không proactive test (continuous check user flows)
- ❌ Regression detection slow

**B. Feature Flags:**
- ❌ Không có feature flag system
- ❌ Deploy = all-or-nothing (risky for large changes)
- ❌ Không thể A/B test dễ dàng
- ❌ Không có gradual rollout

## Proposed Fix

### Part A: Synthetic Monitoring

**Continuous Playwright tests** chạy every 5-10 min:

```typescript
// synthetic-tests/branding-flow.ts
test('tenant can complete branding wizard', async ({ page }) => {
  await page.goto(`${TEST_TENANT_URL}/auth/login`);
  await login(page, TEST_CREDENTIALS);

  await page.goto('/branding/wizard');

  // Step 1-6
  await page.click('text=Bắt đầu');
  await page.setInputFiles('[type=file]', TEST_LOGO);
  await page.click('text=Tiếp theo');
  // ...

  // Verify deploy success
  await expect(page.getByText('DEPLOYED')).toBeVisible({ timeout: 60000 });
});

test('branding package API returns valid response', async ({ request }) => {
  const res = await request.get(`/api/v1/branding/${TEST_TENANT}/package`);
  expect(res.ok()).toBeTruthy();
  const pkg = await res.json();
  expect(pkg.theme.primaryColor).toMatch(/^#[0-9a-f]{6}$/i);
});
```

Schedule via Kubernetes CronJob or external service (Checkly, Grafana Synthetic).

**Alerts:**
- Synthetic test fail → PagerDuty alert
- Latency regression → warning
- Success rate < 99% → ticket

### Part B: Feature Flag System

**Library:** Unleash, LaunchDarkly, or self-hosted GrowthBook

```java
@Service
public class FeatureFlagService {
  public boolean isEnabled(String flag, Context ctx) {
    return flagClient.isEnabled(flag, ctx);
  }
}

// Usage:
if (featureFlags.isEnabled("ai-branding-v2-wizard", tenantContext)) {
  return newWizard();
} else {
  return oldWizard();
}
```

**Flag types:**

| Flag Type | Example | Use case |
|-----------|---------|----------|
| Boolean | `enable-gemma4` | On/off for all |
| Percentage | `wizard-v2: 10%` | Gradual rollout |
| Targeting | `feature-X: tier=PREMIUM` | Tier-specific |
| A/B | `template-picker-layout` | Variant test |

**Use cases:**
- Deploy new features dark → ramp up traffic
- Kill switch for emergencies (disable feature without deploy)
- A/B test 2 UX variants
- Tier-gated features
- Beta testing program (invite-only)

**Management UI:**
- `/admin/feature-flags` — list, toggle, target
- Audit log: who changed what
- Rollback: revert to previous value

### Integration

```
Synthetic test runs for prod → detects regression
  ↓
Admin flips feature flag OFF (no deploy needed)
  ↓
Issue isolated, diagnosis begins
  ↓
Fix deployed → flag gradually re-enabled (10% → 50% → 100%)
```

## Acceptance Criteria

### Monitoring
- [ ] 5+ synthetic tests covering critical flows
- [ ] Schedule every 10 min across regions
- [ ] Alerting integrated (PagerDuty/Slack)
- [ ] Dashboard: success rate per test
- [ ] Runbook: respond to synthetic test failures

### Feature Flags
- [ ] Feature flag library integrated (Unleash or similar)
- [ ] 10+ flags defined for rollout control
- [ ] Admin UI to toggle flags
- [ ] Audit log of flag changes
- [ ] Kill switch documented cho emergency
- [ ] Rollout playbook: new feature → gradual ramp

## Dependencies

- GAP-019 (observability) — dashboards integration
- CI/CD pipeline — feature flag config in deploy

## Log

- 2026-04-14 — Operational maturity gap
