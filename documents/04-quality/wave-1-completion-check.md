# Wave 1 Completion Check

**Date:** 2026-03-23
**PRs merged:** #194, #196, #197, #195
**Main commit:** `a4fdd8c4`

## Results

| Level | Check | Status | Detail |
|-------|-------|--------|--------|
| **1** | CI Green on main | ✅ | KiteHub Platform CI: success (×3) |
| **2.1** | Email templates match code | ✅ | 8 templates, 4 code triggers — all match |
| **2.2** | Config keys consistent | ✅ | 3 new @ConfigurationProperties, all in application.yml |
| **2.3** | No hardcoded business constants | ⚠️ | Pool sizes (FREE=5, BASIC=10...) still hardcoded — not business-critical |
| **2.4** | No conflict markers | ✅ | 0 found |
| **3.1** | Trial config complete | ✅ | 4 fields: durationDays, maxPerOwner, warningDays, midpointDay |
| **3.2** | Trial limit in all create methods | ✅ | 3 methods: createTrialInstance, createPendingInstance, registerInstance |
| **3.3** | Config injected | ✅ | InstanceService(3), TrialService(1), SubscriptionRenewalService(1), schedulers(2) |
| **3.4** | BASE_DOMAIN configurable | ✅ | @Value injection, no hardcode |
| **3.5** | Insecure defaults | ⚠️ | `changeme-in-production` still in application.yml default — but @PostConstruct blocks it |
| **4.1** | TODO count | ℹ️ | Java: 5 FUTURE (DatabaseBackup, ContentPersistence), FE: 1 |
| **4.2** | Test count | ℹ️ | KiteHub: 39 (was 36, +3 new), KiteClass: 93 |
| **5.1** | Plans updated | ⬜ | Need to update SaaS plan completion status |

## Business Gaps Fixed by Wave 1

| Gap | Before | After |
|-----|--------|-------|
| Trial hardcode 14 days | ❌ hardcode | ✅ `kitehub.trial.duration-days` |
| MAX_FREE = 2 (should be 1) | ❌ wrong value | ✅ `existsByOwnerIdAndTrialStartedAtIsNotNull` |
| Grace period hardcode 3 days | ❌ hardcode | ✅ `kitehub.subscription.grace-period-days` |
| 4 email templates missing | ❌ missing | ✅ 4 templates created |
| No reserved subdomains | ❌ missing | ✅ 28 names blocked |
| BASE_DOMAIN hardcode | ❌ `.kiteclass.com` | ✅ configurable via env |
| Internal secret insecure | ❌ `changeme` default | ✅ @PostConstruct fail-fast |
| Docs duplicate numbering | ❌ 01×2, 02×2, etc | ✅ Clean 01-07 |

## Issues Found

| # | Issue | Severity | Action |
|---|-------|----------|--------|
| 1 | Pool sizes hardcoded (MultiTenantDataSourceConfig) | 🟡 Low | Track for future config PR |
| 2 | `changeme-in-production` still in application.yml | 🟡 Low | @PostConstruct blocks — cosmetic |
| 3 | 5 FUTURE placeholders remain | 🟠 Medium | SAAS-3 (data retention) will fix |
| 4 | SaaS plan completion status not updated | 🟡 Low | Update below |

## Verdict

✅ **Wave 1 complete — ready for Wave 2**

Core business logic gaps addressed. No critical integration issues. CI green.
