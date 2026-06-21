# Business Logic Documents

**Last Updated:** 2026-06-21

> SOURCE OF TRUTH cho business rules. Mỗi domain = 1 folder × 3 layers (rules.md + use-cases.md + api-contract.md). Index đầy đủ ở §5 — hiện **75 domains** (27 KiteHub + 48 KiteClass), tất cả đủ 3 layers (verify: `bash scripts/check-3-layer-completeness.sh`).

## Quy tắc

### 1. Lưu ở đâu?

```
documents/01-business/          ← TẤT CẢ business logic ở đây
├── README.md                   ← File này (index + quy tắc)
├── kitehub/                    ← KiteHub platform (27 domains × 3 layers — tất cả complete)
│   ├── trial-lifecycle/        ← rules.md + use-cases.md + api-contract.md (mẫu 3-layer)
│   ├── subscription-billing/
│   ├── instance-provisioning/
│   ├── auth/ · auth-2fa/ · sso/ · signup-otp/   ← auth cluster
│   ├── beta-access/ · beta-status/ · onboarding/ · off-boarding/
│   ├── consent/ · preferences/ · feedback/ · support/
│   └── ...                     ← (+ 14 domains khác — danh sách đầy đủ ở §5 Index)
└── kiteclass/                  ← KiteClass core (48 domains × 3 layers — tất cả complete)
    ├── student-enrollment/     ← rules.md + use-cases.md + api-contract.md (mẫu 3-layer)
    ├── course-class/ · teacher/ · attendance/ · grade-assignment/
    ├── payment-invoice/ · payment-record/ · payroll/ · course-pricing/
    ├── parent-portal/ · student-portal/ · tenant-auth/   ← Wave auth-1/2 KC-native login
    ├── analytics-report/       ← dashboard doanh thu + tỷ lệ điểm danh (GAP-775)
    └── ...                     ← (+ 35 domains khác — danh sách đầy đủ ở §5 Index)
```

**KHÔNG lưu trong:**
- ❌ `kiteclass-core/docs/` — nằm sâu, không ai mở
- ❌ `documents/03-planning/` — đó là plan, không phải business rules
- ❌ `.claude/skills/` — đó là hướng dẫn cho Claude, không phải business truth

**Lý do:** 1 nơi duy nhất, dễ tìm, dễ review, dễ grep.

### 2. Cấu trúc 3-Layer per domain

**Mỗi domain = 1 folder với 3 files** (xem chi tiết: `.claude/skills/reference/business-docs-3-layer.md`)

```
{domain}/
├── rules.md          # Layer 1: Business Rules (constraints, config keys) — ~50-80 lines
├── use-cases.md      # Layer 2: Use Cases (actor, steps, errors, FE behavior) — ~80-120 lines
└── api-contract.md   # Layer 3: API Contract (endpoints, request/response) — ~60-100 lines
```

**Verification chain:** `BR-xxx → UC-xxx → endpoint → @Mapping → @Test`

### 3. Khi nào tạo/cập nhật?

| Thời điểm | Action |
|-----------|--------|
| Bắt đầu module mới | `/pre-flight-check domain` → tạo business doc TRƯỚC KHI code |
| PR thay đổi business rule | Cập nhật doc TRONG CÙNG PR (same commit) |
| `/business-gap-check` phát hiện gap | Tạo/fix doc + code cùng lúc |
| Code review | Reviewer check: doc khớp code không? |

**Rule: Doc và code PHẢI cùng PR.** Nếu đổi `plusDays(14)` → đổi `trial-lifecycle.md` trong cùng commit.

### 4. Làm sao để nhớ tham chiếu?

**Cơ chế tự động:**
- Pre-commit hook check: nếu sửa file trong `service/` → warn nếu không sửa file trong `01-business/`
- `/pre-flight-check pr` nhắc check business docs
- CLAUDE.md reference đến thư mục này

**Cơ chế thủ công:**
- Mỗi @ConfigurationProperties class có comment link đến business doc
- Mỗi business doc có "Last verified" date
- Monthly review: đọc tất cả docs, so sánh với code

### 5. Index — Documents hiện có

> Tất cả domain dưới đây có đủ 3 layers (rules.md + use-cases.md + api-contract.md). Verify: `bash scripts/check-3-layer-completeness.sh` → 75/75 complete. "Last Verified" = ngày verify gần nhất doc-khớp-code; `index sync` = ngày xác nhận file presence trong đợt sync index (GAP-666, không phải re-verify doc-vs-code).

#### KiteHub (27 domains × 3 layers — tất cả complete ✅)
| Domain | rules | use-cases | api-contract | Last Verified |
|--------|-------|-----------|-------------|---------------|
| admin | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| admin-audit | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| ai-branding | ✅ | ✅ | ✅ | 2026-03-24 |
| auth | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| auth-2fa | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| beta-access | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| beta-status | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| consent | ✅ | ✅ | ✅ | 2026-06-21 (GAP-1516 use-cases — was PARTIAL GAP-353b) |
| custom-domain | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| data-retention | ✅ | ✅ | ✅ | 2026-03-24 |
| domain-management | ✅ | ✅ | ✅ | 2026-03-24 |
| email | ✅ | ✅ | ✅ | 2026-06-21 (GAP-664 use-cases backfill) |
| email-lifecycle | ✅ | ✅ | ✅ | 2026-03-24 |
| feedback | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| instance-provisioning | ✅ | ✅ | ✅ | 2026-03-24 |
| marketing | ✅ | ✅ | ✅ | 2026-06-21 (GAP-1516 use-cases — was PARTIAL GAP-353b) |
| notification | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| off-boarding | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| onboarding | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| preferences | ✅ | ✅ | ✅ | 2026-06-21 (GAP-664 rules + use-cases backfill) |
| seed | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| signup-otp | ✅ | ✅ | ✅ | 2026-06-21 (GAP-286 Phase-1 backend OTP scaffold — mock delivery) |
| sso | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| subscription-billing | ✅ | ✅ | ✅ | 2026-03-24 |
| support | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| trial-lifecycle | ✅ | ✅ | ✅ | 2026-03-24 |
| trial-to-paid-migration | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |

#### KiteClass (48 domains × 3 layers — tất cả complete ✅)
| Domain | rules | use-cases | api-contract | Last Verified |
|--------|-------|-----------|-------------|---------------|
| academic-year | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| ai-agent-workflow | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| ai-provider | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| analytics-report | ✅ | ✅ | ✅ | 2026-06-02 (GAP-775 Mảng B11 — revenue + attendance dashboard) |
| attendance | ✅ | ✅ | ✅ | 2026-03-24 |
| branding-api | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| branding-wizard | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| bulk-import | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| child-protection | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| content-moderation | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| course-class | ✅ | ✅ | ✅ | 2026-03-24 |
| course-pricing | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| data-retention | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| document-generation | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| gamification-points | ✅ | ✅ | ✅ | 2026-03-24 |
| grade-assignment | ✅ | ✅ | ✅ | 2026-03-24 |
| instance-lifecycle | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| k12-model | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| legal-ip-protection | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| lms | ✅ | ✅ | ✅ | 2026-03-24 |
| marketing | ✅ | ✅ | ✅ | 2026-03-24 |
| mis-integration | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| multi-subject-gradebook | ✅ | ✅ | ✅ | 2026-05-05 (Wave 19 Bucket B GAP-323c Phase 1C v1) |
| multi-tenancy | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| notification-email | ✅ | ✅ | ✅ | 2026-03-24 |
| outbox-events | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| parent-portal | ✅ | ✅ | ✅ | 2026-04-19 |
| payment-invoice | ✅ | ✅ | ✅ | 2026-03-24 |
| payment-record | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| payroll | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| period-attendance | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| quality-gate | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| rebrand-approval | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| report-card | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| reschedule | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| resource-classification | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| resource-handlers | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| role-hierarchy | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| security-foundation | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| security-hardening | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| staff-invitation | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| storage | ✅ | ✅ | ✅ | 2026-03-24 |
| student-enrollment | ✅ | ✅ | ✅ | 2026-03-24 |
| student-portal | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| teacher | ✅ | ✅ | ✅ | 2026-03-24 |
| tenant-auth | ✅ | ✅ | ✅ | 2026-06-06 (Wave auth-2 GAP-1009 — KC-native login Option B) |
| tenant-provisioning | ✅ | ✅ | ✅ | 2026-06-21 (index sync) |
| tenant-settings | ✅ | ✅ | ✅ | 2026-03-24 |
