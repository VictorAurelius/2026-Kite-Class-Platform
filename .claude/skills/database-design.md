# Skill: Database Design

Tham khảo thiết kế CSDL cho dự án KiteClass Platform V3.1.

## Mô tả

Tài liệu thiết kế database bao gồm:
- **KiteHub Database**: Single shared database cho SaaS platform
  - sales.* (customers, orders, subscriptions, payments, pricing_plans)
  - messages.* (chat_sessions, notifications)
  - maintaining.* (instances, health_checks)
  - ai_agents.* (ai_sessions, marketing_assets)

- **KiteClass Instance Database**: Database-per-tenant (complete isolation)
  - User Module: users, roles, permissions, user_roles
  - Class Module: courses, classes, class_schedules, class_sessions, enrollments
  - Learning Module: attendance, grades, assignments, submissions
  - Billing Module: invoices, invoice_items, payments
  - Gamification Module: point_rules, student_points, badges, rewards
  - Parent Module: parents, parent_children

## Trigger phrases

- "thiết kế database"
- "database schema"
- "cấu trúc bảng"
- "entity relationship"
- "index strategy"

## Files

| File | Path |
|------|------|
| Tài liệu chính | `documents/plans/database-design.md` |

## Quick Reference

### Naming Conventions
| Element | Convention | Example |
|---------|------------|---------|
| Tables | snake_case, plural | `students`, `class_schedules` |
| Columns | snake_case | `first_name`, `created_at` |
| Primary Keys | `id` (BIGSERIAL) | `id` |
| Foreign Keys | `{table}_id` | `student_id`, `class_id` |
| Indexes | `idx_{table}_{columns}` | `idx_students_email` |

### Common Columns (Audit)
```sql
created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
created_by BIGINT REFERENCES users(id),
updated_by BIGINT REFERENCES users(id),
deleted BOOLEAN DEFAULT FALSE NOT NULL,
deleted_at TIMESTAMP WITH TIME ZONE,
version INTEGER DEFAULT 0 NOT NULL
```

### Key Tables
- **users**: Core user entity với auth, profile, OAuth support
- **classes**: Quản lý lớp học với teacher, schedule, tuition
- **enrollments**: Quan hệ Student-class
- **invoices**: Billing với computed balance_due
- **student_points**: Gamification point tracking
- **parents**: Quan hệ Parent-child với tích hợp Zalo

## Actions

### Tra cứu schema
Đọc file `documents/plans/database-design.md` để xem chi tiết.

### Xem ERD
Xem phần "4. Entity Relationship Diagrams" trong tài liệu.

### Query optimization
Xem phần "5. Indexes & Performance" cho index strategy và optimized queries.
