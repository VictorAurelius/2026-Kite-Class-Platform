# Wave 11 — KiteHub 93→100 + 3-Layer Business Docs

**Date:** 2026-03-24
**Baseline:** KiteHub 93/100 (A), Business Gap 95% (57/60)
**Target:** KiteHub 100/100 (A+), Business Gap 100%, 3-Layer docs complete

---

## Business Docs Restructure

### 7 domains KiteHub cần chuyển + bổ sung:

| Domain | rules.md | use-cases.md | api-contract.md |
|--------|----------|-------------|-----------------|
| trial-lifecycle | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| subscription-billing | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| email-lifecycle | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| instance-provisioning | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| domain-management | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| data-retention | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |
| ai-branding | ✅ có (migrate) | ❌ tạo mới | ❌ tạo mới |

**Tổng:** 7 rules.md (migrate) + 7 use-cases.md (mới) + 7 api-contract.md (mới) = 21 files

---

## PR List

### PR-1: Restructure business docs (7 domains → folders) [CRITICAL]

- [ ] Migrate 7 single-file docs → folder structure
- [ ] Update `documents/01-business/README.md` index
- [ ] Update cross-references

### PR-2: Layer 2 — Use Cases cho 7 domains [CRITICAL]

**Extract từ actual code:**
- [ ] `trial-lifecycle/use-cases.md` — create trial instance, trial expiration, convert to paid
- [ ] `subscription-billing/use-cases.md` — create subscription, upgrade, downgrade, cancel, renew, prorate
- [ ] `email-lifecycle/use-cases.md` — trigger flow cho 13 templates, idempotency
- [ ] `instance-provisioning/use-cases.md` — create instance, provision DB, reserve subdomain, delete
- [ ] `domain-management/use-cases.md` — setup custom domain, verify DNS, remove domain
- [ ] `data-retention/use-cases.md` — retention warnings, cleanup, soft delete
- [ ] `ai-branding/use-cases.md` — generate branding, rate limit check, template gallery

### PR-3: Layer 3 — API Contracts cho 7 domains [CRITICAL]

- [ ] 7 api-contract.md files — endpoints từ actual Controllers
- [ ] Cross-reference UC-IDs
- [ ] Request/response JSON từ DTOs

### PR-4: Project Management Finalize [+3]

- [ ] Finalize wave-3-completion-check
- [ ] Update parallel-execution-strategy
- [ ] Project status summary

### PR-5: JWT Security + Backend Tests [+2]

- [ ] JWT fail-fast (remove #{null})
- [ ] SubscriptionExpirationCheckerTest

### PR-6: Frontend Tests + API Documentation [+2]

- [ ] InstanceTab tests + missing settings tests
- [ ] KiteHub API reference doc

### PR-7: Close Business Gaps [+3 gaps]

- [ ] Fix ai-branding.md service boundary
- [ ] KiteHub-branding unit tests (AIRateLimitServiceTest, TemplateGalleryServiceTest)
- [ ] Document mock API keys

---

## Execution

| Agent | PRs | Scope |
|-------|-----|-------|
| 1 | PR-1 + PR-2 | Restructure + Use Cases |
| 2 | PR-3 | API Contracts |
| 3 | PR-4 + PR-5 | Project Mgmt + Security + Tests |
| 4 | PR-6 + PR-7 | Frontend Tests + Docs + Gaps |

---

## Score Projection

| After | Quality | Business Gap |
|-------|---------|-------------|
| Baseline | 93/100 | 95% |
| +PR-1,2,3 (3-layer docs) | 94 | 98% |
| +PR-4 (Project Mgmt) | 97 | 98% |
| +PR-5 (Security + Tests) | 99 | 98% |
| +PR-6 (FE Tests + API doc) | 100 | 98% |
| +PR-7 (Business gaps) | 100 | 100% |
