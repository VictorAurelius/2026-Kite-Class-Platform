# Skill: Business Gap Check

**Version:** 1.0
**Last Updated:** 2026-03-23
**Purpose:** Phát hiện gaps trong business logic giữa code thực tế và yêu cầu SaaS chuẩn

---

## Usage

```
/business-gap-check [kitehub|kiteclass|all]
```

---

## Instructions

### Bước 1: Thu thập business rules từ code

Chạy song song:

```bash
# 1. Tìm tất cả hardcoded business constants
grep -rn "final.*=.*[0-9]" kitehub/kitehub-*/src/main --include="*.java" | grep -v "serialVersionUID\|logger\|LOG"

# 2. Tìm email templates vs email calls
ls kitehub/kitehub-email/src/main/resources/templates/emails/ 2>/dev/null
grep -rn "sendEmail\|template.*=.*\"" kitehub/kitehub-*/src/main --include="*.java"

# 3. Tìm @Scheduled jobs
grep -rn "@Scheduled" kitehub/kitehub-*/src/main --include="*.java"

# 4. Tìm status transitions
grep -rn "setStatus\|InstanceStatus\.\|SubscriptionStatus\." kitehub/kitehub-*/src/main --include="*.java"

# 5. Tìm validation gaps
grep -rn "Map<String.*String>.*@RequestBody\|@RequestBody.*Map" kitehub/kitehub-*/src/main --include="*.java"

# 6. Tìm TODO/FIXME/HACK trong production code
grep -rn "TODO\|FIXME\|HACK\|XXX\|FUTURE" kitehub/kitehub-*/src/main --include="*.java"

# 7. Tìm exception handling gaps
grep -rn "catch.*Exception.*e\)" kitehub/kitehub-*/src/main --include="*.java" | grep -v "log\."

# 8. Tìm missing @Transactional
grep -rn "save\|delete\|update" kitehub/kitehub-*/src/main --include="*Service.java" | grep -v "@Transactional" | head -20

# 9. Check config externalization
grep -rn "ConfigurationProperties\|@Value" kitehub/kitehub-*/src/main --include="*.java"

# 10. Check reserved words/security
grep -rn "admin\|root\|test\|demo" kitehub/kitehub-*/src/main --include="*.java" | grep -i "subdomain\|reserved"
```

### Bước 2: Check từng Business Domain

#### 2.1 Registration & Onboarding

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Email verification flow | Code path: register → sendVerification → verifyEmail | Có đầy đủ |
| Reserved subdomain check | Search "reserved" trong InstanceService | Có list reserved names |
| Duplicate email check | Search "existsByEmail" | Ngăn đăng ký trùng email |
| Welcome email sau verify | Search "welcome" template trigger | Gửi sau activate |
| Onboarding tips email | Search "onboarding" email trigger | Gửi 24h sau register |
| CAPTCHA protection | Search "captcha" trong AuthController | Có cho /register |

#### 2.2 Trial Lifecycle

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Trial duration configurable | Search `plusDays` hoặc `trialDays` | Từ config, không hardcode |
| Trial limit per owner | Search `MAX_FREE` hoặc `maxPerOwner` | 1 lần duy nhất |
| Trial warning emails | Search `warning` trong scheduler | Gửi theo config (7, 3, 1 ngày) |
| Trial expired → suspend | Search `suspendExpiredTrial` | Auto suspend khi hết |
| Trial midpoint email | Search "midpoint" | Gửi giữa trial |
| Prevent re-trial | Search logic check "ever had trial" | Block nếu đã trial |

#### 2.3 Subscription & Payment

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Trial → Paid transition | Search `convertTrialToSubscription` | Zero downtime |
| Payment webhook verify | Search "signature" verify | HMAC-SHA256 |
| Grace period configurable | Search `GRACE_PERIOD` | Từ config |
| Renewal reminder emails | Search "renewal-reminder" | 7, 3, 1 ngày trước |
| Auto-suspend after grace | Search `suspendExpiredSubscription` | Auto suspend |
| Subscription created email | Search "subscription-created" template | Gửi sau payment |

#### 2.4 Data Retention & Cleanup

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Retention period per tier | Search `retention` config | Khác nhau theo tier |
| Retention warning emails | Search "retention-warning" | 2 lần trước xóa |
| Data backup trước xóa | Search `pg_dump` hoặc `backup` | Backup → S3 |
| Data cleanup scheduler | Search "cleanup" scheduler | Auto sau retention |
| Cleanup notification | Search "data-deleted" template | Email sau xóa |

#### 2.5 Email System

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Template exists cho mỗi trigger | ls templates/ vs grep sendEmail | 1:1 match |
| Email sent log (idempotency) | Search `email_sent_log` table | Có table tracking |
| Unsubscribe link | Search "unsubscribe" trong templates | GDPR compliance |
| Email variables từ config | Search hardcode "14 ngày" trong templates | Dùng Thymeleaf variables |

#### 2.6 Domain Management

| Check | Cách verify | Expected |
|-------|-------------|----------|
| BASE_DOMAIN configurable | Search `.kiteclass.com` hardcode | Từ config |
| Reserved subdomain list | Search "reserved" validation | Có list blocked names |
| Custom domain verify | Search `verifyDomain` | DNS verification flow |
| Custom domain SSL | Search SSL/cert logic | Auto hoặc documented |
| Custom domain tier check | Search `allowsCustomDomain` | Premium+ only |

#### 2.7 Security & Rate Limiting

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Rate limit per tier | Search `rate-limit` config | Khác nhau theo tier |
| AI usage rate limit | Search AI quota/limit | Per tier per day |
| Brute force protection | Search login rate limit | Có limit |
| CORS per environment | Search `CORS_ALLOWED_ORIGINS` | Configurable |

#### 2.8 Configuration

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Business constants externalized | Search `@ConfigurationProperties` | Tất cả constants từ config |
| Frontend reads config | Search `/api/platform/config` | Public config API |
| Admin can change config | Search admin config endpoint | Hoặc YAML cũng được |

### Bước 3: Output Gap Report

```markdown
# Business Gap Check Report: [KiteHub/KiteClass]

**Ngày:** [date]
**Commit:** [hash]

## Summary

| Domain | Checks | Pass | Fail | Score |
|--------|--------|------|------|-------|
| Registration | X | X | X | X% |
| Trial | X | X | X | X% |
| Subscription | X | X | X | X% |
| Data Retention | X | X | X | X% |
| Email | X | X | X | X% |
| Domain | X | X | X | X% |
| Security | X | X | X | X% |
| Configuration | X | X | X | X% |
| **Total** | **X** | **X** | **X** | **X%** |

## ❌ Failed Checks (Critical Gaps)

| # | Domain | Check | Expected | Actual | Impact |
|---|--------|-------|----------|--------|--------|
| 1 | ... | ... | ... | ... | 🔴/🟠/🟡 |

## ✅ Passed Checks

[List all passing checks]

## Action Items

| Priority | Gap | Fix | Effort |
|----------|-----|-----|--------|
| 🔴 P0 | ... | ... | ... |
```

### Bước 4: Lưu report

- Save to `documents/05-qa-and-best-practices/business-gap-check-[date]-[target].md`
- Cross-reference với existing plans

---

## Rules

- LUÔN đọc code thật, KHÔNG đoán
- LUÔN giao tiếp tiếng Việt
- Chấm Pass/Fail dựa trên evidence (code, config, templates)
- Nếu code gọi template nhưng template không tồn tại → FAIL
- Nếu constant hardcoded thay vì config → FAIL
- Nếu logic chưa implement (placeholder/FUTURE) → FAIL
- Cross-check: email template tồn tại ↔ code trigger ↔ scheduler call
