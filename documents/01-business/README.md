# Business Logic Documents

## Quy tắc

### 1. Lưu ở đâu?

```
documents/01-business/          ← TẤT CẢ business logic ở đây
├── README.md                   ← File này (index + quy tắc)
├── kitehub/                    ← KiteHub platform
│   ├── trial-lifecycle.md
│   ├── subscription-billing.md
│   ├── email-lifecycle.md
│   ├── domain-management.md
│   └── data-retention.md
└── kiteclass/                  ← KiteClass core
    ├── student-enrollment.md
    ├── attendance-flow.md
    ├── payment-flow.md
    └── ...
```

**KHÔNG lưu trong:**
- ❌ `kiteclass-core/docs/` — nằm sâu, không ai mở
- ❌ `documents/03-planning/` — đó là plan, không phải business rules
- ❌ `.claude/skills/` — đó là hướng dẫn cho Claude, không phải business truth

**Lý do:** 1 nơi duy nhất, dễ tìm, dễ review, dễ grep.

### 2. Chi tiết đến đâu?

**Nguyên tắc: 1 trang A4 per domain** (~100-150 dòng markdown)

MỖI document gồm 4 sections bắt buộc:

```markdown
# [Domain] Business Logic

## 1. Rules (PHẢI có)
Bảng business rules — cái này là SOURCE OF TRUTH.

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| TR-01 | Trial duration | 14 days | kitehub.trial.duration-days |
| TR-02 | Max trial per owner | 1 | kitehub.trial.max-per-owner |

## 2. Flow (PHẢI có)
Mermaid diagram hoặc text flow — 1 diagram, không quá 15 bước.

Register → Verify Email → Start Trial → [Day 7: midpoint email]
→ [Day 11: warning email] → [Day 13: final warning]
→ [Day 14: expire → suspend] → [Day 21: delete data]

## 3. Emails (nếu có trigger)
Bảng emails — template name PHẢI khớp file thực tế.

| Trigger | Template | Timing |
|---------|----------|--------|
| Trial start | welcome.html | Ngay lập tức |
| Trial day 11 | trial-warning.html | 8 AM daily check |

## 4. Config (PHẢI có)
Copy CHÍNH XÁC config keys — frontend/backend đọc từ đây.

```yaml
kitehub:
  trial:
    duration-days: 14
    max-per-owner: 1
```
```

**KHÔNG viết:**
- ❌ Scenario dài 50 dòng (ví dụ language center, art center...)
- ❌ Scope/Out-of-scope lists
- ❌ Architecture diagrams (đó thuộc `02-architecture/`)
- ❌ API endpoint lists (đó thuộc Swagger)
- ❌ Database schema (đó thuộc Flyway migrations)

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

#### KiteHub
| Document | Status | Last Verified |
|----------|--------|---------------|
| (chưa tạo — migrate từ analysis) | | |

#### KiteClass
| Document | Status | Last Verified |
|----------|--------|---------------|
| (chưa tạo — migrate từ kiteclass-core/docs/) | | |
