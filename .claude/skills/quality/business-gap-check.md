---
name: business-gap-check
description: "Dùng khi user nói 'gap check', 'kiểm tra gaps', 'business logic gaps', 'missing features'. Phát hiện gaps giữa code thực tế và yêu cầu SaaS chuẩn."
user-invocable: true
---

# Skill: Business Gap Check

**Version:** 1.3
**Last Updated:** 2026-04-14 (thêm §2.9 AI Branding + §KC-2.10 Design Patterns)
**Purpose:** Phát hiện gaps trong business logic giữa code thực tế và yêu cầu SaaS chuẩn

---

## Usage

```
/business-gap-check [kitehub|kiteclass|all]
```

## Relationship với /quality-audit

| | `/quality-audit` | `/business-gap-check` |
|--|-------------------|----------------------|
| **Focus** | Kỹ thuật (tests, CI, docs, code style) | Nghiệp vụ (business logic đúng/sai) |
| **Output** | Score /100, Grade A-D | Pass/Fail %, gap list |
| **Ví dụ** | "CI green ✅" | "Trial limit sai (2 thay vì 1) ❌" |
| **Khi dùng** | Đánh giá chất lượng tổng quan | Verify nghiệp vụ trước production |

**Bổ sung nhau:** quality-audit có thể cho 91/100 nhưng business-gap chỉ 45% — code chạy tốt nhưng logic sai.

---

## Instructions

### Bước 0 — Canonical-status lookup TRƯỚC khi emit candidates (BẮT BUỘC)

Skill này emit gap list (Failed Checks table) sau khi scan business domains → MUST chạy state-check qua canonical CSV + ROADMAP §Dropped TRƯỚC khi output, KHÔNG phải sau (Bước 2C hiện tại chặn ở filing time, quá muộn — candidate đã liệt kê trong report).

**Mandatory commands (read full output, KHÔNG `| head` truncate per `audit-to-gap-pipeline.md` §2.5 hardened protocol):**

```bash
# 1. List items đã shipped (skip nếu candidate match)
bash scripts/query-gaps.sh "" DONE ""

# 2. List items active scope (cross-ref candidate)
bash scripts/query-gaps.sh "" PARTIAL ""
bash scripts/query-gaps.sh "" OPEN ""
bash scripts/query-gaps.sh "" IN_PROGRESS ""

# 3. List items user-rejected
grep -E "Dropped:.*GAP-" documents/04-quality/gaps/ROADMAP.md
```

**Filter rule:** Emit failed check (trong report) CHỈ khi:
- Domain check fail thực sự (verified via grep code)
- KHÔNG match existing CSV row tracking issue tương tự (status nào cũng count)
- ID KHÔNG appear trong ROADMAP §Dropped section
- Attach evidence inline: "Verified against gap-status.csv YYYY-MM-DD: GAP-XXX absent → genuinely new business gap"

**Why mandatory:** 2026-04-20 audit `simulation-gap-finder` (sister skill) emit 12 candidates với 66% noise (7 shipped same week + 1 user-rejected). Pattern recurrence: bất cứ khi nào audit skill scan codebase mà skip canonical CSV lookup. Memory `feedback_audit_candidate_pre_filing_state_check.md` documents incident. Reference: `audit-to-gap-pipeline.md` §2.5 + `gap-architecture-v2.md` §1 + `pre-mutation-state-check.md` §1.

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

### Bước 2B: KiteClass-specific Checks (khi target = kiteclass hoặc all)

Thay thế Bước 2 bằng các checks dưới đây khi target là `kiteclass`:

```bash
# KiteClass data collection
grep -rn "final.*=.*[0-9]" kiteclass/kiteclass-core/src/main --include="*.java" | grep -v "serialVersionUID\|logger\|LOG\|VERSION"
grep -rn "TODO\|FIXME\|HACK\|FUTURE" kiteclass/kiteclass-core/src/main --include="*.java"
grep -rn "TODO\|FIXME" kiteclass/kiteclass-frontend/src --include="*.ts" --include="*.tsx"
grep -rn "Map<String.*>.*@RequestBody\|@RequestBody.*Map" kiteclass/kiteclass-core/src/main --include="*.java"
grep -rn "@Valid" kiteclass/kiteclass-core/src/main --include="*.java" -l | wc -l
grep -rn "@RequestBody" kiteclass/kiteclass-core/src/main --include="*.java" | wc -l
grep -rn "@Scheduled" kiteclass/kiteclass-core/src/main --include="*.java"
grep -rn "ConfigurationProperties\|@Value" kiteclass/kiteclass-core/src/main --include="*.java"
grep -rn "@FilterDef\|@Filter\|tenantFilter" kiteclass/kiteclass-core/src/main --include="*.java"
grep -rn "changeme\|default.*secret\|placeholder" kiteclass/kiteclass-core/src/main --include="*.java"
find kiteclass/kiteclass-core/src/main -name "*Controller.java" | wc -l
find kiteclass/kiteclass-core/src/test -name "*IT.java" | wc -l
grep -rn "@Disabled" kiteclass/kiteclass-core/src/test --include="*.java"
```

#### KC-2.1 Multi-tenant Isolation

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Hibernate tenant filter on BaseEntity | Search `@FilterDef("tenantFilter")` | Có trên BaseEntity |
| TenantContext ThreadLocal | Search `TenantContext` class | Set by interceptor |
| Filter auto-enabled on all queries | Search `enableFilter("tenantFilter")` trong JPA config | Auto-enable |
| Integration test prove isolation | Search `*IT.java` test tenant isolation | Test 2 tenants không thấy data nhau |
| No raw SQL bypass filter | Search native queries, verify có WHERE tenant | Không bypass |

#### KC-2.2 Module Completeness

| Check | Cách verify | Expected |
|-------|-------------|----------|
| All 15 modules have Controller | Count Controllers vs module dirs | 1:1 match |
| Student CRUD complete | Endpoints: list, get, create, update, delete | Đủ 5 operations |
| Teacher CRUD complete | Same as student | Đủ 5 operations |
| Course → Class → Enrollment flow | Code path verify | End-to-end |
| Attendance recording + report | AttendanceController endpoints | Record + report |
| Payment gateway integration | VNPay/MoMo/ZaloPay gateway clients | Ít nhất 1 hoạt động |

#### KC-2.3 Input Validation

| Check | Cách verify | Expected |
|-------|-------------|----------|
| @Valid coverage | Count @Valid vs @RequestBody | >90% coverage |
| GlobalExceptionHandler | Search ControllerAdvice | Xử lý validation errors |
| No untyped Map request bodies | Search `Map.*@RequestBody` | 0 hoặc chỉ webhooks |
| DTO constraint annotations | Search @NotBlank, @NotNull, @Size | Trên tất cả DTOs |

#### KC-2.4 Security

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Internal API secret secure | Search default value | Không phải "changeme" |
| Internal API filter exists | Search InternalRequestFilter | HMAC + timestamp |
| Payment credentials externalized | Search @Value payment.* | Từ env vars |
| CORS configured | Search CORS trong gateway | Configurable |
| No sensitive data in logs | Search log.info/debug cho password, secret | Không log sensitive |

#### KC-2.5 Frontend Quality

| Check | Cách verify | Expected |
|-------|-------------|----------|
| 0 TODO/FIXME | grep TODO trong src/ | 0 |
| useAuth tenant context | Search useAuth hook | Decode tenantId từ JWT, không hardcode |
| SEO basics (robots, sitemap) | Search robots.ts, sitemap.ts | Có |
| OpenGraph metadata | Search openGraph trong layout/page | Có trên public pages |
| No console.log in production | Search console.log | 0 (trừ dev-only) |

#### KC-2.6 Configuration

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Business constants externalized | Search hardcoded final = [number] | Từ @ConfigurationProperties |
| Storage config externalized | Search StorageProperties | Có |
| Payment URLs configurable | Search payment.* @Value | Configurable |
| Late fee / grace period configurable | Search hardcoded rates | Từ config |

#### KC-2.7 Scheduled Jobs & Async

| Check | Cách verify | Expected |
|-------|-------------|----------|
| Payment expiry check | Search @Scheduled trong payment | Có |
| Storage cleanup | Search StorageCleanupScheduler | Có |
| RabbitMQ event-driven | Search RabbitConfig exchanges/queues | Defined (không chỉ placeholder) |

#### 2.9 AI Branding (v2 redesign) — kiteclass-core

Reference: `documents/02-architecture/ai-branding-v2-redesign.md`, `.claude/rules/ai-branding-guidelines.md`, `documents/04-quality/gaps/closed/GAP-016-ai-branding-v2-living-docs-impact.md`

> **Module location note (verified 2026-04-26):** v2 implementation shipped to `kiteclass/kiteclass-core/` (NOT `kitehub/kitehub-branding/` as architecture doc specified). `kitehub-branding/` retains v1 only. Class renames: `BrandingAnalyzer → AnalyzerService`, `BrandingPlanner → PlannerService`, `BrandingExecutor → PlanExecutor`. Architecture doc drift tracked separately (see GAP-016 Findings + follow-up).

| Check | Cách verify | Expected |
|-------|-------------|----------|
| ResourceCategory enum | `grep -rln "enum ResourceCategory" kiteclass/kiteclass-core/src/main kitehub/kitehub-branding/src/main` | STATIC, TEMPLATE, FULL_AI |
| ResourceRoutingService | Search class across both modules | Has `classify()` + `route()` methods |
| AnalyzerService (v2 name) | Search class | Extract BrandingContext from request |
| PlannerService (v2 name) | Search class | Returns ExecutionPlan (list of Steps) |
| PlanExecutor | Search class | Executes with fallback support |
| Step interface | `grep -rln "interface Step" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/workflow` | Has `hasFallback()`, `fallback()` |
| FrontendInstance entity | Search `@Entity class FrontendInstance` | Has status, retryCount, brandingVersion |
| FrontendInstanceStatus enum | Search enum | NOT_STARTED/INITIALIZING/GENERATING/DEPLOYED/REGENERATING/FAILED |
| InstanceLifecycleService | Search class | Transition methods only, validates state machine |
| InstanceQualityReviewer | Search class | Returns QualityReport with score /100 |
| Tenant provisioning saga | `grep -rln "class TenantProvisioningSaga" kiteclass/kiteclass-core/src/main` | Saga exists (alternative to direct @RabbitListener pattern) |
| BrandingResource entity | Search `@Entity class BrandingResource` | Has category, templateId, aiJobId, metadata |
| Image template / library | Search entity OR migration `image_templates`/`branding_templates` | Sprint 0 deliverable per GAP-011 — currently no entity (templates lazy via `ImageTemplate*`/migration); track as scaffold |
| Package API | `grep -rln "BrandingPackageController" kiteclass/kiteclass-core/src/main` | Endpoint returns composite response |
| Wizard FE component | `find kiteclass/kiteclass-frontend/src kitehub/kitehub-frontend/src -iname "*Wizard*"` | `BrandingWizard.tsx` + `wizard-machine.ts` exist |
| Regenerate limits config | `grep -rn "regenerate" kiteclass/kiteclass-core/src/main/resources/application.yml` | Per-tier limits (3/10/30/-1) — pending per `ai-branding-guidelines.md` §4.3; track GAP-005 |
| No free-form prompt | `grep -rln "textarea.*prompt\|placeholder.*Describe.*banner" kiteclass/kiteclass-frontend/src kitehub/kitehub-frontend/src` | 0 results (except Enterprise opt-in gated) |
| Template count | Query DB `SELECT count(*) FROM branding_templates WHERE active=true` (or equivalent migration) | ≥30 (Sprint 0 baseline per GAP-011) |
| Quality gate integration | Search `score < PASS_THRESHOLD\|score.*70` in `InstanceLifecycleService` + `InstanceQualityReviewer` | Gate confirmed at score < 70 |
| Webhook on branding.updated | `grep -rln "BrandingUpdatedEvent\|BrandingEventPublisher" kiteclass/kiteclass-core/src/main` | Event published via outbox |

#### KC-2.10 Design Patterns (NEW — mandatory from 2026-04-14)

Reference: `.claude/rules/design-patterns.md`, `documents/02-architecture/ai-branding-design-patterns.md`

| Check | Cách verify | Expected |
|-------|-------------|----------|
| No God Service | `find kitehub-*/src/main -name "*Service.java" -size +20k` | 0 files |
| Strategy for AI providers | Search `AIClient` interface + implementations | Interface + ≥2 impls |
| State Pattern for status | Search status transitions | No switch scattered; use State classes |
| Command Pattern for pipeline | Search `Step` interface | Exists, composable |
| Facade for orchestration | Search `*Facade.java` | BrandingFacade (or similar) exists |
| Adapter for external APIs | Search `*Adapter.java` | OllamaAdapter, OpenAIAdapter exist |
| Outbox for events | Search `outbox_events` table + publisher | Exists |
| Circuit Breaker | Search `@CircuitBreaker` annotations | On external HTTP calls |
| Bulkhead | Search `@Bulkhead` annotations | On AI service calls |
| Saga for distributed txn | Search `*Saga.java` or similar | Provisioning uses saga |
| No primitive obsession | Search `String color`, `String status` fields | Value objects preferred |
| Pattern documented | Search javadoc for pattern names | "Strategy Pattern", "State Pattern" etc. |

#### KC-2.8 Testing

| Check | Cách verify | Expected |
|-------|-------------|----------|
| 0 @Disabled tests | grep @Disabled | 0 |
| Integration tests exist | find *IT.java | >0 |
| Tenant isolation test | Search test prove 2 tenants isolated | Có |
| CI green | gh run list | All pass |

### Bước 2C: State-Check trước khi file gap (BẮT BUỘC)

Trước khi output report + tạo gap file cho mỗi issue found:

1. Grep actual code paths gap sẽ touch (FE src, BE src, infra, docs)
2. Nếu implementation đã tồn tại (toàn phần) → KHÔNG tạo gap, note vào report "already shipped — no gap filed"
3. Nếu partial → tạo gap với status 🟡 PARTIAL + section `## Current State (verified YYYY-MM-DD)` liệt kê file + LOC
4. Nếu nothing → tạo gap 🔵 OPEN bình thường

Reference: `.claude/rules/audit-to-gap-pipeline.md` Step 2.5. Vi phạm = gap phải rewrite sau (xem incident GAP-190/197 2026-04-20).

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

- Save to `documents/04-quality/audits/business/business-gap-check-[date]-[target].md`
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

---

## Gotchas

- **State-check before filing a gap** — per `audit-to-gap-pipeline.md` Step 2.5, grep actual paths the gap would touch; partial implementations exist (GAP-190/197 incident — `feedback_gap_state_check_required.md`)
- **`grep` scope must include `-core` submodules** — `kiteclass/kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}/` is where v2 lives, not `kitehub-branding/` (architecture doc drift — `feedback_search_all_modules_before_missing_claim.md`)
- **Hardcoded numbers are not always business gaps** — many constants are `serialVersionUID`, port numbers, byte-size limits; filter via `grep -v "serialVersionUID\|logger\|LOG"` before flagging
- **Email-template-without-call ≠ unused** — some templates fire from cron jobs (`@Scheduled`) or message consumers; verify both code paths before claiming dead template
- **Status transition gaps mask state machine errors** — `setStatus()` calls outside the canonical state machine class are usually the bug, not "missing transitions"; cross-reference with `design-patterns.md` §3.3
