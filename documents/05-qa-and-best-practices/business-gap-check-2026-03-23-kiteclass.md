# Business Gap Check Report: KiteClass

**Ngày:** 2026-03-23
**Commit:** `069365b`
**Skill:** `/business-gap-check kiteclass`

---

## Summary

| Domain | Checks | Pass | Fail | Score |
|--------|--------|------|------|-------|
| Multi-tenant Isolation | 5 | 4 | 1 | 80% |
| Module Completeness | 6 | 4 | 2 | 67% |
| Input Validation | 4 | 3 | 1 | 75% |
| API Documentation | 3 | 3 | 0 | 100% |
| Testing | 5 | 3 | 2 | 60% |
| Security | 5 | 3 | 2 | 60% |
| Frontend Quality | 5 | 2 | 3 | 40% |
| Configuration | 4 | 2 | 2 | 50% |
| Scheduled Jobs | 3 | 2 | 1 | 67% |
| **Total** | **40** | **26** | **14** | **65%** |

### Business Readiness: 65% — Tốt hơn KiteHub (45%) nhưng vẫn cần cải thiện

---

## ❌ Failed Checks (14 Gaps)

### Multi-tenant Isolation (1 fail)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 1 | Tenant filter enabled trên mọi query | Hibernate filter auto-apply | ✅ `@FilterDef` + `@Filter` trên BaseEntity — **nhưng** chỉ 6 references đến tenantId trong Repository → cần verify filter enable tự động | 🟠 Potential data leak nếu filter không enable |

### Module Completeness (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 2 | LMS module implemented | Endpoints hoạt động | ❌ Module `lms/` tồn tại nhưng có Controller — **cần verify completeness** | 🟡 Feature incomplete |
| 3 | Gamification module implemented | Endpoints hoạt động | ❌ Module `gamification/` tồn tại — **cần verify completeness** | 🟡 Feature incomplete |

### Input Validation (1 fail)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 4 | Payment webhooks typed DTOs | Typed request classes | ❌ `PaymentWebhookController` dùng `Map<String, String>` cho MoMo + ZaloPay callbacks (2 endpoints) | 🟠 No validation on webhook payloads |

### Testing (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 5 | 0 @Disabled tests | Tất cả tests enabled | ❌ 2 `@Disabled` tests trong `InternalApiSecurityTest.java` (1 redundant, 1 flaky) | 🟡 Test debt |
| 6 | Integration tests (*IT.java) | Ít nhất 3 flow tests | ❌ 0 integration test files | 🟠 No end-to-end flow verification |

### Security (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 7 | Internal API secret secure | Strong secret, not default | ❌ Default value: `changeme-in-production` trong InternalRequestFilter | 🔴 Security risk nếu quên đổi |
| 8 | Payment notify URL configurable | Từ env var | ⚠️ `payment.notify-url` default hardcode `https://api.kiteclass.vn` — domain chưa tồn tại | 🟠 Payment callbacks sẽ fail |

### Frontend Quality (3 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 9 | 0 TODO/FIXME | Clean production code | ❌ **6 TODOs** trong frontend: useAuth hardcoded UUID, attendance fetch, enrollment fetch | 🟠 Incomplete features |
| 10 | useAuth tenantId from JWT | Decode từ JWT claims | ❌ **BLOCKED** — hardcoded placeholder UUID, comment: "Add tenantId to JWT in Gateway" | 🔴 Tenant context sai trên frontend |
| 11 | robots.txt + sitemap | SEO basics | ❌ Không có robots.ts/sitemap.ts (nhưng có OpenGraph metadata — partial) | 🟡 SEO |

### Configuration (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 12 | Late fee rate configurable | Từ config | ❌ **Hardcoded** `LATE_FEE_RATE = 0.001` trong InvoiceServiceImpl | 🟠 Business rule cứng |
| 13 | Storage cleanup grace period configurable | Từ config | ❌ **Hardcoded** `SOFT_DELETE_GRACE_PERIOD_DAYS = 30` | 🟡 |

### Scheduled Jobs (1 fail)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 14 | RabbitMQ event-driven | Async events cho cross-module | ❌ **FUTURE placeholder** — RabbitConfig chỉ có comment, chưa define queues | 🟡 Synchronous coupling |

---

## ✅ Passed Checks (26)

### Multi-tenant Isolation (4 pass)
- ✅ `@FilterDef("tenantFilter")` + `@Filter` trên BaseEntity — Hibernate auto-filter
- ✅ `TenantContext` ThreadLocal — set by `TenantFilterInterceptor`
- ✅ `UserContext` ThreadLocal — tracks current user
- ✅ `MultiTenantKeyGenerator` — cache isolation per tenant

### Module Completeness (4 pass)
- ✅ 14 modules có Controller: student, teacher, course, class, attendance, enrollment, grade, invoice, payment, assignment, marketing, settings, storage, lms
- ✅ Core CRUD operations cho Student, Teacher, Course, Class
- ✅ Attendance module (recording + reports)
- ✅ Payment module (VNPay, MoMo, ZaloPay gateways)

### Input Validation (3 pass)
- ✅ 42/44 `@RequestBody` có `@Valid` (95% coverage)
- ✅ 51 files dùng `@Valid`
- ✅ `GlobalExceptionHandler` xử lý `MethodArgumentNotValidException`

### API Documentation (3 pass)
- ✅ springdoc-openapi v2.8.4 integrated
- ✅ 88 `@Tag`/`@Operation` annotations
- ✅ Swagger UI accessible

### Testing (3 pass)
- ✅ 93 test files
- ✅ Core CI + Gateway CI green (3/3 mỗi cái)
- ✅ Frontend CI green

### Security (3 pass)
- ✅ Internal API filter (HMAC signature + timestamp validation)
- ✅ Tenant isolation via Hibernate filter
- ✅ Payment gateway credentials externalized (@Value)

### Frontend Quality (2 pass)
- ✅ OpenGraph metadata trên public pages
- ✅ `generateMetadata()` cho dynamic pages (catalog/[id])

### Configuration (2 pass)
- ✅ `StorageProperties` — `@ConfigurationProperties(prefix = "storage.s3")`
- ✅ Payment gateway URLs configurable

### Scheduled Jobs (2 pass)
- ✅ Payment expiry check (mỗi 10 phút)
- ✅ Storage cleanup (soft delete 30 ngày, orphan cleanup 2 AM)

---

## So sánh KiteHub vs KiteClass Gap Check

| Metric | KiteHub | KiteClass |
|--------|---------|-----------|
| Total checks | 40 | 40 |
| Pass | 18 | 26 |
| Fail | 22 | 14 |
| Score | **45%** | **65%** |
| Critical (🔴) | 8 | 2 |
| Medium (🟠) | 8 | 6 |
| Low (🟡) | 6 | 6 |

**KiteClass tốt hơn vì:**
- Hibernate tenant filter tự động (KiteHub dùng manual query)
- Validation coverage 95% (KiteHub có 3 untyped endpoints)
- 14 modules đều có Controller (feature-rich)
- OpenGraph metadata đã có

**KiteClass tệ hơn vì:**
- 6 frontend TODOs (đặc biệt useAuth BLOCKED)
- 0 integration tests
- RabbitMQ chưa implement (FUTURE)
- Internal API secret default insecure

---

## Mapping → KiteClass Quality Plan

| Gap # | Existing PR | Scope |
|-------|-------------|-------|
| 5 | PR-KC-1 (Fix @Disabled) | ⬜ TODO |
| 6 | PR-KC-2 (Integration tests) | ⬜ TODO |
| 9,10 | PR-KC-5 (Fix FE TODOs) | ⬜ TODO |
| 11 | Chưa có PR — SEO basics | ⬜ NEW |
| 4 | Chưa có PR — typed webhook DTOs | ⬜ NEW |
| 7 | Chưa có PR — secure internal secret default | ⬜ NEW |
| 8 | Chưa có PR — payment notify URL | ⬜ NEW |
| 12,13 | Chưa có PR — externalize constants | ⬜ NEW |
| 1 | Chưa có PR — verify tenant filter auto-enable | ⬜ NEW |

### New PRs cần thêm vào KiteClass Quality Plan:

| PR | Scope | Priority | Effort |
|----|-------|----------|--------|
| PR-KC-11: Typed webhook DTOs | MoMo + ZaloPay typed request classes | 🟠 P1 | 2 hrs |
| PR-KC-12: Secure defaults | Internal API secret fail-fast nếu default | 🔴 P0 | 1 hr |
| PR-KC-13: Frontend SEO basics | robots.ts, sitemap.ts | 🟡 P2 | 2 hrs |
| PR-KC-14: Externalize business constants | Late fee rate, storage grace period | 🟡 P2 | 2 hrs |
| PR-KC-15: Verify tenant filter | Integration test prove isolation works | 🟠 P1 | 0.5 day |
| PR-KC-16: Fix payment notify URL | Configurable, fail-safe default | 🟠 P1 | 1 hr |
