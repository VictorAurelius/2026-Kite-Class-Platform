# Skill: Database Design

Tham khảo thiết kế CSDL cho dự án KiteClass Platform V3.1.

## Mô tả

Tài liệu thiết kế database bao gồm:
- **KiteHub Database**: Single shared database cho SaaS platform
  - sales.* (customers, orders, subscriptions, payments, pricing_plans)
  - messages.* (chat_sessions, notifications)
  - maintaining.* (instances, health_checks)
  - ai_agents.* (ai_sessions, marketing_assets)
  - themes.* (templates, template_versions, instance_themes)

- **KiteClass Instance Database**: Database-per-tenant (complete isolation)
  - User Module: users, roles, permissions, user_roles
  - Class Module: courses, classes, class_schedules, class_sessions, enrollments
  - Learning Module: attendance, grades, assignments, submissions
  - Billing Module: invoices, invoice_items, payments
  - Gamification Module: point_rules, student_points, badges, rewards
  - Parent Module: parents, parent_children
  - Settings Module: branding, user_preferences

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

---

## Theme Tables Schema

### KiteHub Database - themes.*

```sql
-- Theme templates quản lý centralized
CREATE TABLE themes.templates (
    id VARCHAR(50) PRIMARY KEY,           -- e.g., 'modern-light', 'playful-kids'
    name VARCHAR(100) NOT NULL,
    description TEXT,
    tier VARCHAR(20) NOT NULL DEFAULT 'free',  -- 'free', 'pro', 'enterprise'
    config JSONB NOT NULL,                 -- Full theme config (colors, typography, etc.)
    preview_url VARCHAR(500),
    screenshots JSONB DEFAULT '[]',        -- Array of screenshot URLs
    popularity INTEGER DEFAULT 0,          -- Số lượng instance sử dụng
    is_active BOOLEAN DEFAULT TRUE,
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Version history cho templates
CREATE TABLE themes.template_versions (
    id BIGSERIAL PRIMARY KEY,
    template_id VARCHAR(50) NOT NULL REFERENCES themes.templates(id),
    version VARCHAR(20) NOT NULL,
    config JSONB NOT NULL,
    changelog TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(template_id, version)
);

-- Mapping instance -> theme
CREATE TABLE themes.instance_themes (
    instance_id BIGINT PRIMARY KEY REFERENCES maintaining.instances(id),
    template_id VARCHAR(50) NOT NULL REFERENCES themes.templates(id),
    custom_overrides JSONB,               -- Custom CSS/config overrides (Enterprise)
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    applied_by BIGINT                     -- User who applied the theme
);

-- Indexes
CREATE INDEX idx_templates_tier ON themes.templates(tier);
CREATE INDEX idx_templates_popularity ON themes.templates(popularity DESC);
CREATE INDEX idx_instance_themes_template ON themes.instance_themes(template_id);
```

### KiteClass Instance Database - settings.*

```sql
-- Branding settings cho instance
CREATE TABLE settings.branding (
    id SERIAL PRIMARY KEY,                -- Singleton record (id = 1)
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    primary_color VARCHAR(7),             -- Hex color, e.g., '#0ea5e9'
    secondary_color VARCHAR(7),
    display_name VARCHAR(200) NOT NULL,
    tagline VARCHAR(500),
    footer_text TEXT,
    social_links JSONB DEFAULT '{}',      -- {"facebook": "...", "zalo": "..."}
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

-- User preferences (color mode, language, etc.)
CREATE TABLE settings.user_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    color_mode VARCHAR(10) DEFAULT 'system',  -- 'light', 'dark', 'system'
    language VARCHAR(5) DEFAULT 'vi',         -- 'vi', 'en'
    timezone VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh',
    notifications JSONB DEFAULT '{"email": true, "push": true, "sms": false}',
    ui_settings JSONB DEFAULT '{}',           -- Compact mode, sidebar collapsed, etc.
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Audit log cho settings changes
CREATE TABLE settings.change_log (
    id BIGSERIAL PRIMARY KEY,
    setting_type VARCHAR(50) NOT NULL,     -- 'branding', 'theme', 'preferences'
    changed_by BIGINT REFERENCES users(id),
    old_value JSONB,
    new_value JSONB,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_user_preferences_updated ON settings.user_preferences(updated_at);
CREATE INDEX idx_change_log_type ON settings.change_log(setting_type, changed_at);
```

### Theme Config JSON Structure

```json
{
  "colors": {
    "primary": {
      "50": "#f0f9ff",
      "100": "#e0f2fe",
      "500": "#0ea5e9",
      "900": "#0c4a6e"
    },
    "secondary": { "..." },
    "success": "#22c55e",
    "warning": "#f59e0b",
    "error": "#ef4444",
    "background": "#ffffff",
    "foreground": "#0f172a",
    "muted": "#64748b"
  },
  "typography": {
    "fontFamily": {
      "sans": "Inter, system-ui, sans-serif",
      "mono": "JetBrains Mono, monospace"
    },
    "fontSize": {
      "xs": "0.75rem",
      "sm": "0.875rem",
      "base": "1rem",
      "lg": "1.125rem"
    }
  },
  "borderRadius": {
    "sm": "0.25rem",
    "md": "0.375rem",
    "lg": "0.5rem",
    "full": "9999px"
  },
  "shadows": {
    "sm": "0 1px 2px rgba(0,0,0,0.05)",
    "md": "0 4px 6px rgba(0,0,0,0.1)"
  },
  "components": {
    "button": {
      "borderRadius": "md",
      "fontWeight": "500"
    },
    "card": {
      "borderRadius": "lg",
      "shadow": "sm"
    }
  }
}
```

## Actions

### Tra cứu schema
Đọc file `documents/plans/database-design.md` để xem chi tiết.

### Xem ERD
Xem phần "4. Entity Relationship Diagrams" trong tài liệu.

### Query optimization
Xem phần "5. Indexes & Performance" cho index strategy và optimized queries.
