# Business Logic Documents

## Quy tắc

### 1. Lưu ở đâu?

```
documents/01-business/          ← TẤT CẢ business logic ở đây
├── README.md                   ← File này (index + quy tắc)
├── kitehub/                    ← KiteHub platform (8 domains × 3 layers expected; some 🟡 PARTIAL)
│   ├── trial-lifecycle/        ← rules.md + use-cases.md + api-contract.md
│   ├── subscription-billing/
│   ├── email-lifecycle/
│   ├── instance-provisioning/
│   ├── domain-management/
│   ├── data-retention/
│   ├── ai-branding/
│   └── marketing/              ← Wave 23 (rules.md only — use-cases/api-contract → GAP-353b/c)
└── kiteclass/                  ← KiteClass core (13 domains × 3 layers = 39 files)
    ├── student-enrollment/     ← rules.md + use-cases.md + api-contract.md
    ├── course-class/
    ├── teacher/
    ├── attendance/
    ├── analytics-report/      ← báo cáo dashboard doanh thu + tỷ lệ điểm danh (GAP-775)
    ├── grade-assignment/
    ├── payment-invoice/
    ├── gamification-points/
    ├── notification-email/
    ├── tenant-settings/
    ├── lms/
    ├── marketing/
    ├── storage/
    ├── parent-portal/          ← Wave 2 MVP (GAP-052a + GAP-105)
    └── tenant-auth/            ← Wave auth-1 KC-native login (PARENT/TEACHER/STUDENT, GAP-1009)
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

#### KiteHub (8 domains; 7 complete + 1 🟡 PARTIAL)
| Domain | rules | use-cases | api-contract | Last Verified |
|--------|-------|-----------|-------------|---------------|
| trial-lifecycle | ✅ | ✅ | ✅ | 2026-03-24 |
| subscription-billing | ✅ | ✅ | ✅ | 2026-03-24 |
| email-lifecycle | ✅ | ✅ | ✅ | 2026-03-24 |
| instance-provisioning | ✅ | ✅ | ✅ | 2026-03-24 |
| domain-management | ✅ | ✅ | ✅ | 2026-03-24 |
| data-retention | ✅ | ✅ | ✅ | 2026-03-24 |
| ai-branding | ✅ | ✅ | ✅ | 2026-03-24 |
| marketing | ✅ | ⏳ → GAP-353b | ⏳ → GAP-353b | 2026-05-06 (Wave 23 Bucket A — PDPL consent canonical) |

#### KiteClass (13 domains × 3 layers = 39 files ✅)
| Domain | rules | use-cases | api-contract | Last Verified |
|--------|-------|-----------|-------------|---------------|
| student-enrollment | ✅ | ✅ | ✅ | 2026-03-24 |
| course-class | ✅ | ✅ | ✅ | 2026-03-24 |
| teacher | ✅ | ✅ | ✅ | 2026-03-24 |
| attendance | ✅ | ✅ | ✅ | 2026-03-24 |
| analytics-report | ✅ | ✅ | ✅ | 2026-06-02 (GAP-775 Mảng B11 — revenue + attendance dashboard) |
| grade-assignment | ✅ | ✅ | ✅ | 2026-03-24 |
| payment-invoice | ✅ | ✅ | ✅ | 2026-03-24 |
| gamification-points | ✅ | ✅ | ✅ | 2026-03-24 |
| notification-email | ✅ | ✅ | ✅ | 2026-03-24 |
| tenant-settings | ✅ | ✅ | ✅ | 2026-03-24 |
| lms | ✅ | ✅ | ✅ | 2026-03-24 |
| marketing | ✅ | ✅ | ✅ | 2026-03-24 |
| storage | ✅ | ✅ | ✅ | 2026-03-24 |
| parent-portal | ✅ | ✅ | ✅ | 2026-04-19 |
| tenant-auth | ✅ | ✅ | ✅ | 2026-06-06 (Wave auth-2 GAP-1009 — KC-native login Option B) |
| multi-subject-gradebook | ✅ | ✅ | ✅ | 2026-05-05 (Wave 19 Bucket B GAP-323c Phase 1C v1) |
