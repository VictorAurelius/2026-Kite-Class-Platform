# Business Gap Check Report: KiteHub

**Ngày:** 2026-03-23
**Commit:** `069365b`
**Skill:** `/business-gap-check kitehub`

---

## Summary

| Domain | Checks | Pass | Fail | Score |
|--------|--------|------|------|-------|
| Registration & Onboarding | 6 | 3 | 3 | 50% |
| Trial Lifecycle | 6 | 2 | 4 | 33% |
| Subscription & Payment | 6 | 4 | 2 | 67% |
| Data Retention & Cleanup | 5 | 0 | 5 | 0% |
| Email System | 5 | 2 | 3 | 40% |
| Domain Management | 5 | 3 | 2 | 60% |
| Security & Rate Limiting | 4 | 3 | 1 | 75% |
| Configuration | 3 | 1 | 2 | 33% |
| **Total** | **40** | **18** | **22** | **45%** |

### Business Readiness: 45% — Nhiều gaps critical cần fix trước production

---

## ❌ Failed Checks (22 Gaps)

### Registration & Onboarding (3 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 1 | Reserved subdomain list | Có list: admin, api, www, test... | ❌ Không có validation | 🔴 Security: user có thể đăng ký subdomain "admin" |
| 2 | Onboarding tips email | Gửi 24h sau register | ❌ Không có scheduler/template | 🟡 UX |
| 3 | Untyped request bodies | Typed DTOs cho tất cả endpoints | ❌ 3 endpoints dùng `Map<String,String>`: resendVerification, updateProfile, changePassword | 🟠 Validation bypass |

### Trial Lifecycle (4 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 4 | Trial duration configurable | Từ `@ConfigurationProperties` | ❌ **Hardcoded `plusDays(14)`** trong Instance.java:185 | 🔴 Không thể thay đổi mà không deploy |
| 5 | Trial limit 1x per owner | Check "ever had trial" kể cả DELETED | ❌ **Chỉ check `countByOwnerIdAndDeletedFalse`** — cho phép 2 trial, và tạo mới nếu xóa instance cũ | 🔴 Business logic sai |
| 6 | Trial midpoint email | Gửi email ngày 7 | ❌ Không có | 🟡 Engagement |
| 7 | Prevent re-trial | Block owner đã từng trial | ❌ Không có `existsByOwnerIdAndTrialStartedAtIsNotNull` | 🔴 Lỗ hổng: user xóa instance → tạo trial mới vô hạn |

### Subscription & Payment (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 8 | Grace period configurable | Từ config | ❌ **Hardcoded `GRACE_PERIOD_DAYS = 3`** | 🟠 |
| 9 | MAX_FREE configurable | Từ config | ❌ **Hardcoded `MAX_FREE_INSTANCES_PER_OWNER = 2`** (nên là 1) | 🔴 Sai giá trị (2 thay vì 1) |

### Data Retention & Cleanup (5 fails — TOÀN BỘ)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 10 | Retention period per tier | Config: trial=7, basic=30, premium=60, enterprise=90 | ❌ **Không tồn tại** | 🔴 Data giữ vĩnh viễn hoặc hardcode 30 ngày |
| 11 | Retention warning emails | 2 email cảnh báo trước xóa | ❌ **Không tồn tại** | 🔴 Xóa data mà không cảnh báo |
| 12 | Data backup trước xóa | pg_dump → S3 | ❌ **Placeholder FUTURE** (DatabaseBackupScheduler.java:72) | 🔴 Mất data vĩnh viễn |
| 13 | Data cleanup scheduler | Auto xóa sau retention | ❌ **Placeholder FUTURE** (DatabaseBackupScheduler.java:63) | 🔴 Data zombie |
| 14 | Cleanup notification | Email "data đã xóa" | ❌ **Không tồn tại** | 🟠 |

### Email System (3 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 15 | Template cho mỗi trigger | 1:1 match giữa code calls vs templates | ❌ **4 templates missing:** trial-expired, trial-expiration-warning, subscription-renewal-reminder, subscription-suspended | 🔴 Code gọi template không tồn tại → crash/silent fail |
| 16 | Email sent log (idempotency) | Table `email_sent_log` | ❌ **Không tồn tại** — scheduler có thể gửi duplicate | 🟠 Spam user |
| 17 | Unsubscribe link | GDPR compliance | ❌ **Không có** trong bất kỳ template nào | 🟠 Legal risk |

### Domain Management (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 18 | BASE_DOMAIN configurable | Từ config | ❌ **Hardcoded `".kiteclass.com"` trong TenantResolverGatewayFilterFactory.java:36** | 🔴 Không thể đổi domain |
| 19 | DNS verification service | CustomDomainService.verifyDomain() | ❌ **Không tồn tại** — custom domain set mà không verify DNS | 🟠 Custom domain có thể không hoạt động |

### Security (1 fail)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 20 | AI usage rate limit per tier | 3/day free, 10/day basic, etc. | ❌ **Không có** — unlimited AI calls | 🟠 Abuse/cost overrun |

### Configuration (2 fails)

| # | Check | Expected | Actual | Impact |
|---|-------|----------|--------|--------|
| 21 | Business constants externalized | `@ConfigurationProperties` cho trial, subscription, retention | ❌ **3 critical constants hardcoded:** trial=14 days, grace=3 days, max_free=2 | 🔴 Phải deploy lại để đổi |
| 22 | Public config API | `GET /api/platform/config/public` | ❌ **Không tồn tại** — frontend hardcode numbers | 🟠 Frontend/email inconsistent |

---

## ✅ Passed Checks (18)

### Registration & Onboarding
- ✅ Email verification flow (register → sendVerification → verifyEmail)
- ✅ Welcome email sau verify (welcome.html template)
- ✅ CAPTCHA protection (`CaptchaService`, configurable enabled/disabled)

### Trial Lifecycle
- ✅ Trial warning emails code exists (TrialExpirationChecker, 8 AM daily)
- ✅ Trial expired → suspend (suspendExpiredTrial logic)

### Subscription & Payment
- ✅ Trial → Paid transition (convertTrialToSubscription, zero downtime)
- ✅ Payment webhook verify (HMAC-SHA256 signature)
- ✅ Renewal reminder emails code exists (SubscriptionExpirationChecker)
- ✅ Auto-suspend after grace (processExpiredSubscriptions)

### Domain Management
- ✅ Subdomain validation (regex `^[a-z0-9-]+$`)
- ✅ Custom domain tier check (PricingTier.allowsCustomDomain → Premium+ only)
- ✅ TenantResolver filter (3-step resolution: header → subdomain → custom domain)

### Security
- ✅ Rate limit on register (RequestRateLimiter 3 req/s)
- ✅ CORS per environment (configurable via env var)
- ✅ Brute force protection (rate limit on auth endpoints)

### Configuration
- ✅ Infrastructure config externalized (DB, Redis, RabbitMQ, email provider, JWT)

---

## Critical Path — 🔴 P0 Gaps (phải fix trước production)

| # | Gap | Risk nếu không fix | Fix |
|---|-----|---------------------|-----|
| 5,7 | Trial không giới hạn 1 lần | User spam trial vô hạn | Check `trialStartedAt IS NOT NULL` |
| 9 | MAX_FREE = 2 (nên 1) | Sai business rule | Đổi config → 1 |
| 4 | Trial 14 ngày hardcoded | Không đổi được | ConfigurationProperties |
| 15 | 4 email templates missing | Code crash khi gửi email | Tạo 4 templates |
| 10-14 | Data retention toàn bộ missing | Data không bao giờ cleanup / mất data | Implement full lifecycle |
| 1 | Reserved subdomain | User chiếm "admin.kiteclass.com" | Add reserved list |
| 18 | BASE_DOMAIN hardcoded | Không thể dùng kitehub.vn | Externalize config |
| 21 | Constants hardcoded | Deploy lại mỗi lần đổi | ConfigurationProperties |

---

## Mapping Gaps → PRs (SaaS Implementation Plan)

| Gap # | PR | Status |
|-------|-----|--------|
| 4, 8, 9, 21 | PR-SAAS-1 (Configurable Constants) | ⬜ TODO |
| 15 | PR-SAAS-2 (Missing Email Templates) | ⬜ TODO |
| 10-14 | PR-SAAS-3 (Data Retention Service) | ⬜ TODO |
| 5, 7 | PR-SAAS-4 (Trial Limit 1x) | ⬜ TODO |
| 16 | PR-SAAS-5 (Email Sent Log) | ⬜ TODO |
| 2, 6 | PR-SAAS-7 (Email Lifecycle) | ⬜ TODO |
| 20 | PR-SAAS-9 (AI Rate Limiting) | ⬜ TODO |
| 22 | PR-SAAS-12 (Public Config API) | ⬜ TODO |
| 1 | PR-SAAS-14 (Reserved Subdomains) | ⬜ TODO |
| 18 | PR-SAAS-15 (Configurable BASE_DOMAIN) | ⬜ TODO |
| 19 | PR-SAAS-16 (Custom Domain Verify) | ⬜ TODO |
| 17 | Chưa có PR — cần thêm unsubscribe link | ⬜ NEW |
| 3 | PR-V3-3 (Typed DTOs) đã có trong quality plan | ⬜ TODO |

### Gap mới phát hiện (chưa có trong plan):
- **Unsubscribe link trong email templates** — GDPR compliance → thêm vào PR-SAAS-2
- **PaymentWebhookController dùng `Map<String, Object>`** — nên typed DTO → thêm vào PR-V3-3

---

## Next Steps

1. **Merge PR #193** (docs) để commit tất cả plans
2. **Bắt đầu Phase 1** (PR-SAAS-1 → PR-SAAS-4) — fix 🔴 P0 gaps
3. **Re-run `/business-gap-check kitehub`** sau Phase 1 để verify
