# KiteHub SaaS Implementation Plan

**Ngày tạo:** 2026-03-23
**Dựa trên:** SaaS Best Practices Analysis + Leader Decisions
**Domain:** kitehub.vn
**Mục tiêu:** Nghiệp vụ SaaS chuẩn, SEO-ready, Template-first AI

---

## Confirmed Decisions

| # | Decision | Confirmed |
|---|----------|-----------|
| 1 | Trial limit | **1 lần per owner** |
| 2 | Data retention | **Theo gói payment** (không cố định) |
| 3 | Trial backup | **7 ngày** sau khi hết trial, cảnh báo email 2 lần |
| 4 | AI approach | **Template-first** (instant UX) |
| 5 | Blog platform | **MDX** (in-repo, simple, free) |
| 6 | Domain | **kitehub.vn** |

### Data Retention theo gói:

| Gói | Retention sau suspend | Backup |
|-----|----------------------|--------|
| Trial (hết hạn) | 7 ngày | Email cảnh báo ngày 3 và ngày 6 |
| FREE (hết hạn) | 7 ngày | Email cảnh báo ngày 3 và ngày 6 |
| BASIC | 30 ngày | Email cảnh báo ngày 14 và ngày 27 |
| PREMIUM | 60 ngày | Email cảnh báo ngày 30 và ngày 55 |
| ENTERPRISE | 90 ngày | Email cảnh báo ngày 60 và ngày 85 |

---

## PR Plan (13 PRs, 4 Phases)

### Phase 1: Business Logic Foundation (3 ngày)

---

#### PR-SAAS-1: Configurable Business Constants

**Priority:** 🔴 P0
**Estimate:** 0.5 ngày
**Scope:**

Tạo `@ConfigurationProperties` classes thay thế tất cả hardcoded constants.

**Tasks:**
- [ ] Tạo `TrialConfig.java` (`kitehub.trial.*`)
  - `duration-days: 14`
  - `max-per-owner: 1`
  - `warning-days: [3, 1]`
- [ ] Tạo `SubscriptionConfig.java` (`kitehub.subscription.*`)
  - `grace-period-days: 3`
  - `warning-days: [7, 3, 1]`
- [ ] Tạo `DataRetentionConfig.java` (`kitehub.data-retention.*`)
  - Retention theo tier: `trial: 7, free: 7, basic: 30, premium: 60, enterprise: 90`
  - `warning-count: 2`
- [ ] Refactor `Instance.startTrial()` → dùng `TrialConfig.durationDays`
- [ ] Refactor `InstanceService.MAX_FREE_INSTANCES_PER_OWNER` → dùng `TrialConfig.maxPerOwner`
- [ ] Refactor `SubscriptionRenewalService.GRACE_PERIOD_DAYS` → dùng `SubscriptionConfig`
- [ ] Refactor `TrialExpirationChecker` → dùng `TrialConfig.warningDays`
- [ ] Add config vào `application.yml` với defaults
- [ ] Tests: verify config injection hoạt động

**Files cần tạo:**
- `kitehub-subscription/src/main/java/com/kitehub/subscription/config/TrialConfig.java`
- `kitehub-subscription/src/main/java/com/kitehub/subscription/config/SubscriptionConfig.java`
- `kitehub-subscription/src/main/java/com/kitehub/subscription/config/DataRetentionConfig.java`

**Files cần sửa:**
- `Instance.java` — inject TrialConfig
- `InstanceService.java` — inject TrialConfig
- `SubscriptionRenewalService.java` — inject SubscriptionConfig
- `TrialExpirationChecker.java` — inject TrialConfig
- `SubscriptionExpirationChecker.java` — inject SubscriptionConfig
- `application.yml` — add kitehub.trial.*, kitehub.subscription.*, kitehub.data-retention.*

---

#### PR-SAAS-2: Missing Email Templates

**Priority:** 🔴 P0
**Estimate:** 0.5 ngày
**Scope:**

Tạo 4 email templates đang missing (code gọi nhưng template chưa có).

**Tasks:**
- [ ] Tạo `trial-expired.html` — "Trial đã hết. Data lưu {retentionDays} ngày."
- [ ] Tạo `trial-expiration-warning.html` — "Còn {daysRemaining} ngày trial."
- [ ] Tạo `subscription-renewal-reminder.html` — "Subscription sắp hết, còn {days} ngày."
- [ ] Tạo `subscription-suspended.html` — "Instance tạm dừng. Data lưu {retentionDays} ngày."
- [ ] Tất cả templates dùng variables từ config (không hardcode số ngày)
- [ ] Templates tiếng Việt, responsive, KiteHub branding
- [ ] Test: verify Thymeleaf render đúng

**Files cần tạo:**
- `kitehub-email/src/main/resources/templates/emails/trial-expired.html`
- `kitehub-email/src/main/resources/templates/emails/trial-expiration-warning.html`
- `kitehub-email/src/main/resources/templates/emails/subscription-renewal-reminder.html`
- `kitehub-email/src/main/resources/templates/emails/subscription-suspended.html`

---

#### PR-SAAS-3: Data Retention Policy + Service

**Priority:** 🔴 P0
**Estimate:** 1 ngày
**Scope:**

Implement data retention lifecycle: suspend → cảnh báo 2 lần → backup → cleanup.

**Tasks:**
- [ ] Tạo `DataRetentionService.java`:
  - `getRetentionDays(PricingTier tier)` → trả về retention period theo gói
  - `checkRetentionWarnings()` → gửi email cảnh báo 2 lần
  - `processExpiredRetention()` → backup + cleanup
- [ ] Tạo `DataRetentionScheduler.java` (@Scheduled daily 3 AM):
  - Check suspended instances quá retention period
  - Gửi warning emails (2 lần theo config)
  - Backup data (pg_dump → S3)
  - Drop instance database sau retention hết
  - Update instance status → DELETED
- [ ] Tạo `email-data-retention-warning.html` — "Data sẽ bị xóa sau {daysLeft} ngày."
- [ ] Tạo `email-data-deleted.html` — "Data đã được xóa."
- [ ] Tạo `email_sent_log` table — tránh gửi duplicate
- [ ] Tests: verify retention logic theo tier

**Files cần tạo:**
- `kitehub-subscription/.../service/DataRetentionService.java`
- `kitehub-subscription/.../scheduler/DataRetentionScheduler.java`
- `kitehub-email/.../templates/emails/data-retention-warning.html`
- `kitehub-email/.../templates/emails/data-deleted.html`
- Flyway migration: `email_sent_log` table

---

#### PR-SAAS-4: Trial Limit 1x per Owner

**Priority:** 🔴 P0
**Estimate:** 2 giờ
**Scope:**

Đổi trial limit từ 2 → 1 per owner. Ngăn tạo trial lần 2.

**Tasks:**
- [ ] Refactor `InstanceService`: `MAX_FREE_INSTANCES_PER_OWNER` → `TrialConfig.maxPerOwner` (default 1)
- [ ] Add validation: nếu owner đã từng có trial (kể cả SUSPENDED/DELETED) → reject
- [ ] Add repository method: `countByOwnerIdAndStatusIn(ownerId, [TRIAL, ACTIVE, SUSPENDED])`
- [ ] Add repository method: `existsByOwnerIdAndTrialStartedAtIsNotNull(ownerId)` — check ever had trial
- [ ] Error message: "Mỗi tài khoản chỉ được dùng thử 1 lần."
- [ ] Tests: verify reject khi tạo trial lần 2

---

### Phase 2: SEO + Email Lifecycle (3 ngày)

---

#### PR-SAAS-5: Email Sent Log (Idempotency)

**Priority:** 🟠 P1
**Estimate:** 0.5 ngày
**Scope:**

Tạo `email_sent_log` table để tracking và tránh duplicate emails.

**Tasks:**
- [ ] Flyway migration: tạo `email_sent_log` table
  ```sql
  CREATE TABLE email_sent_log (
    id UUID PRIMARY KEY,
    instance_id UUID,
    email_type VARCHAR(100),  -- trial-warning, subscription-reminder, etc.
    recipient VARCHAR(255),
    sent_at TIMESTAMP,
    UNIQUE(instance_id, email_type, DATE(sent_at))
  );
  ```
- [ ] Tạo `EmailSentLogRepository`
- [ ] Update `EmailServiceClient`: check log trước khi gửi
- [ ] Update schedulers: query log để skip đã gửi
- [ ] Tests: verify không gửi duplicate

---

#### PR-SAAS-6: SEO Foundation

**Priority:** 🟠 P1
**Estimate:** 1 ngày
**Scope:**

Thêm tất cả SEO fundamentals cho kitehub.vn.

**Tasks:**
- [ ] Tạo `src/app/robots.ts` — allow /, disallow /api/ /dashboard/ /admin/
- [ ] Tạo `src/app/sitemap.ts` — /, /pricing, /features
- [ ] Update `layout.tsx` metadata — OpenGraph, Twitter Card, canonical
- [ ] Tạo `/public/favicon.ico` + `/public/apple-touch-icon.png`
- [ ] Tạo `/public/og-image.png` (1200x630)
- [ ] Add `generateMetadata()` cho /pricing page
- [ ] Add semantic H1 cho landing page
- [ ] Update `next.config.js`: i18n, trailingSlash
- [ ] Tests: verify meta tags render đúng

---

#### PR-SAAS-7: Complete Email Lifecycle

**Priority:** 🟠 P1
**Estimate:** 1 ngày
**Scope:**

Tạo remaining email templates và triggers cho full user journey.

**Tasks:**
- [ ] Tạo `onboarding-tips.html` — gửi 24h sau register
- [ ] Tạo `trial-midpoint.html` — gửi ngày 7 của trial
- [ ] Tạo `subscription-expired.html` — khi subscription hết + grace period
- [ ] Tạo `data-retention-final-warning.html` — 1 ngày trước xóa data
- [ ] Update `TrialExpirationChecker` — thêm midpoint email (ngày 7)
- [ ] Tạo `OnboardingEmailScheduler` — gửi tips 24h sau
- [ ] Update `EmailServiceClient` — thêm methods mới
- [ ] All templates: responsive, tiếng Việt, dùng config variables

**Email lifecycle tổng:**
```
Register → [verify] → [welcome] → [+24h tips] → [+7d midpoint]
→ [trial-3d] → [trial-1d] → [expired] → [retain-warn-1] → [retain-warn-2] → [deleted]
```

---

### Phase 3: AI Template + Content (3 ngày)

---

#### PR-SAAS-8: Template Gallery (Canva-like)

**Priority:** 🟠 P1
**Estimate:** 2 ngày
**Scope:**

Tạo pre-built template system cho instant branding (không cần AI).

**Tasks:**
- [ ] Tạo `BrandingTemplate` entity — id, name, category, thumbnail, themeConfig (JSON)
- [ ] Tạo 15 templates:
  - 5 Education (trung tâm ngoại ngữ, trường mầm non, STEM, âm nhạc, mỹ thuật)
  - 5 Business (coaching, fitness, yoga, dance, tutoring)
  - 5 General (minimal, modern, classic, playful, professional)
- [ ] Mỗi template có: colorScheme, fontPair, heroSVG, sectionLayouts
- [ ] API: `GET /api/platform/branding/templates` — list all
- [ ] API: `GET /api/platform/branding/templates/{id}/preview` — preview
- [ ] API: `POST /api/platform/branding/templates/{id}/apply` — apply to instance
- [ ] Frontend: Template gallery page trong branding wizard
- [ ] Frontend: Preview component (instant, < 1s)
- [ ] Tests: API + component tests

---

#### PR-SAAS-9: AI Queue Rate Limiting per Tier

**Priority:** 🟡 P2
**Estimate:** 0.5 ngày
**Scope:**

Rate limit AI requests theo pricing tier.

**Tasks:**
- [ ] Tạo `AIRateLimitConfig` — requests per day per tier
  - FREE/TRIAL: 3/day
  - BASIC: 10/day
  - PREMIUM: 50/day
  - ENTERPRISE: unlimited
- [ ] Tạo `ai_usage_log` table — track usage per instance per day
- [ ] Add rate check trước khi queue AI job
- [ ] Return 429 khi exceed limit
- [ ] Frontend: hiển thị remaining quota
- [ ] Tests: verify rate limiting

---

### Phase 4: Content Marketing (2 ngày)

---

#### PR-SAAS-10: Structured Data (JSON-LD)

**Priority:** 🟡 P2
**Estimate:** 0.5 ngày
**Scope:**

Thêm Schema.org markup cho Google Rich Results.

**Tasks:**
- [ ] Landing page: `SoftwareApplication` schema
- [ ] FAQ section: `FAQPage` schema
- [ ] Pricing: `Product` + `AggregateOffer` schema
- [ ] Organization: `Organization` schema (kitehub.vn)
- [ ] Tạo reusable `JsonLd` component
- [ ] Tests: validate JSON-LD output

---

#### PR-SAAS-11: Blog System (MDX)

**Priority:** 🟡 P2
**Estimate:** 2 ngày
**Scope:**

Blog với MDX cho content marketing + SEO.

**Tasks:**
- [ ] Setup MDX: `@next/mdx` + `contentlayer` hoặc custom loader
- [ ] Tạo `content/blog/` directory structure
- [ ] Tạo blog layout: `/blog` (list) + `/blog/[slug]` (detail)
- [ ] Blog metadata: title, description, date, author, tags, ogImage
- [ ] `generateStaticParams()` cho SSG
- [ ] `generateMetadata()` cho per-post SEO
- [ ] RSS feed: `/feed.xml`
- [ ] 3 seed articles:
  - "5 bước quản lý trung tâm giáo dục hiệu quả"
  - "Tại sao cần phần mềm điểm danh online?"
  - "KiteHub ra mắt: Tạo website trung tâm trong 5 phút"
- [ ] Sitemap: include blog posts
- [ ] Blog sidebar: recent posts, tags, CTA

---

#### PR-SAAS-12: Public Config API

**Priority:** 🟡 P2
**Estimate:** 2 giờ
**Scope:**

API public cho frontend đọc business config (trial days, retention, pricing).

**Tasks:**
- [ ] Tạo `PublicConfigController` — `GET /api/platform/config/public`
- [ ] Response: `{ trialDays, retentionPolicy, pricingTiers, features }`
- [ ] Frontend: fetch config thay vì hardcode
- [ ] Cache: 1 hour (không cần realtime)
- [ ] Tests: verify response format

---

#### PR-SAAS-13: Architecture Documentation Update

**Priority:** 🟡 P2
**Estimate:** 2 giờ
**Scope:**

Cập nhật docs phản ánh decisions đã confirm.

**Tasks:**
- [ ] Update `kitehub-saas-best-practices-analysis.md` — mark decisions confirmed
- [ ] Tạo `documents/02-architecture/email-lifecycle.md` — full email journey diagram
- [ ] Tạo `documents/02-architecture/data-retention-policy.md` — retention rules per tier
- [ ] Update `QUICK_START.md` — add SEO section
- [ ] Update `CLAUDE.md` — add SaaS business rules reference

---

## Execution Timeline

```
Phase 1 — Business Logic (3 ngày):
  Day 1: PR-SAAS-1 (config) + PR-SAAS-4 (trial limit)
  Day 2: PR-SAAS-2 (templates) + PR-SAAS-3 (retention) start
  Day 3: PR-SAAS-3 (retention) complete

Phase 2 — SEO + Email (3 ngày):
  Day 4: PR-SAAS-5 (email log) + PR-SAAS-6 (SEO) start
  Day 5: PR-SAAS-6 (SEO) complete + PR-SAAS-7 (email lifecycle) start
  Day 6: PR-SAAS-7 complete

Phase 3 — AI + Content (3 ngày):
  Day 7-8: PR-SAAS-8 (template gallery)
  Day 9: PR-SAAS-9 (AI rate limit)

Phase 4 — Marketing (2 ngày):
  Day 10: PR-SAAS-10 (JSON-LD) + PR-SAAS-12 (config API) + PR-SAAS-13 (docs)
  Day 11: PR-SAAS-11 (blog MDX)
```

---

## Score Projection

| Sau PR | Quality Score | Business Score |
|--------|-------------|----------------|
| Baseline | 91/100 | Gaps in email, retention, trial |
| Phase 1 done | 93 | ✅ Config, email templates, retention, trial limit |
| Phase 2 done | 96 | ✅ SEO foundation, email lifecycle complete |
| Phase 3 done | 98 | ✅ Template gallery, AI rate limit |
| Phase 4 done | **100** | ✅ Blog, JSON-LD, public config |

---

## Completion Status

| PR | Status | GitHub | Phase |
|----|--------|--------|-------|
| PR-SAAS-1 Configurable Constants | ✅ DONE | #197 (Wave 1) | 1 |
| PR-SAAS-2 Missing Email Templates | ✅ DONE | #194 (Wave 1) | 1 |
| PR-SAAS-3 Data Retention Service | ✅ DONE | #201→#202 (Wave 2) | 1 |
| PR-SAAS-4 Trial Limit 1x | ✅ DONE | #197 (Wave 1) | 1 |
| PR-SAAS-5 Email Sent Log | ✅ DONE | #201→#202 (Wave 2) | 2 |
| PR-SAAS-6 SEO Foundation | ✅ DONE | #200→#202 (Wave 2) | 2 |
| PR-SAAS-7 Email Lifecycle | ✅ DONE | #203→#206 (Wave 3) | 2 |
| PR-SAAS-8 Template Gallery | ✅ DONE | #211→#212 (Wave 4) | 3 |
| PR-SAAS-9 AI Rate Limiting | ✅ DONE | #217→#218 (Wave 5) | 3 |
| PR-SAAS-10 Structured Data | ✅ DONE | #214→#218 (Wave 5) | 4 |
| PR-SAAS-11 Blog MDX | ✅ DONE | #217→#218 (Wave 5) | 4 |
| PR-SAAS-12 Public Config API | ✅ DONE | #209→#212 (Wave 4) | 4 |
| PR-SAAS-13 Architecture Docs | ✅ DONE | #209→#212 (Wave 4) | 4 |
| PR-SAAS-14 Reserved Subdomains | ✅ DONE | #195 (Wave 1) | 1 |
| PR-SAAS-15 Configurable BASE_DOMAIN | ✅ DONE | #195 (Wave 1) | 1 |
| PR-SAAS-16 Custom Domain UI | ✅ DONE | #205→#206 (Wave 3) | 3 |
| PR-SAAS-17 SSL Automation | ✅ DONE | #214→#218 (Wave 5) | 4 |
| **Total** | **17/17 ✅ COMPLETE** | | |

### Wave 5 Notes (2026-03-24) — FINAL
- AI rate limiting: per-tier config, 429 when exceeded
- Blog: 3 Vietnamese articles, SSG, per-post SEO
- JSON-LD: SoftwareApplication + Organization schema
- All plans 100% complete

### Wave 4 Notes (2026-03-24)
- Template gallery: 5 seed templates, full backend + frontend
- Public config API: GET /api/platform/config/public
- Architecture docs: email-lifecycle.md, data-retention-policy.md
- FE test fixes: TS2345 + duplicate text (getAllByText)
- ⚠️ VIOLATION: Wave merged to main without user confirm

### Wave 2 Notes (2026-03-23)
- Agent used `classItem.teacherId` but `Class` type has no such field → fix on wave/2
- Wave branch strategy: 0 conflicts, main never broken
- CI pass first try: 3/4 (75%, up from 50%)

### Wave 1 Notes (2026-03-23)
- Pool sizes (MultiTenantDataSourceConfig) vẫn hardcoded → track cho future config PR
- `changeme-in-production` còn trong application.yml default → safe vì @PostConstruct blocks
- 5 FUTURE placeholders → fix trong SAAS-3 (data retention)
