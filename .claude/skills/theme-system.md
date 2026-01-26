# Skill: Theme System

Hệ thống quản lý Theme cho KiteClass Platform.

## Mô tả

Tài liệu về Theme System:
- Theme architecture (Hybrid model)
- Theme templates và customization
- Branding settings
- Technical implementation
- Business model (Free/Pro/Enterprise)

## Trigger phrases

- "theme system"
- "giao diện"
- "branding"
- "customization"
- "màu sắc"

---

## Theme Architecture

### Hybrid Model

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           THEME SYSTEM ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                              KITEHUB                                     │   │
│  │                                                                          │   │
│  │   ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐   │   │
│  │   │ Theme Marketplace │    │ Instance Manager │    │ Theme Preview  │   │   │
│  │   │                   │    │                  │    │                │   │   │
│  │   │ • Free Themes     │    │ • Assign Theme   │    │ • Live Preview │   │   │
│  │   │ • Pro Themes      │    │ • Custom CSS     │    │ • Before/After │   │   │
│  │   │ • Custom Themes   │    │ • Bulk Update    │    │                │   │   │
│  │   └──────────────────┘    └──────────────────┘    └────────────────┘   │   │
│  │                                     │                                    │   │
│  └─────────────────────────────────────┼────────────────────────────────────┘   │
│                                        │                                        │
│                                        │ Theme Config Sync                      │
│                                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                         KITECLASS INSTANCE                               │   │
│  │                                                                          │   │
│  │   ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐   │   │
│  │   │ Theme Engine     │    │ Branding Settings│    │ User Prefs     │   │   │
│  │   │                  │    │                  │    │                │   │   │
│  │   │ • Apply Template │    │ • Logo           │    │ • Dark Mode    │   │   │
│  │   │ • CSS Variables  │    │ • Primary Color  │    │ • Font Size    │   │   │
│  │   │ • Component Theme│    │ • Display Name   │    │ • Compact View │   │   │
│  │   └──────────────────┘    └──────────────────┘    └────────────────┘   │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Phân quyền quản lý

| Cấu hình | Quản lý tại | Ai có quyền | Tần suất thay đổi |
|----------|-------------|-------------|-------------------|
| Theme Template | KiteHub | Customer (Owner) | Hiếm khi |
| Custom CSS | KiteHub | Customer (Enterprise) | Hiếm khi |
| Logo, Favicon | Instance | Center Admin | Thỉnh thoảng |
| Primary Color | Instance | Center Admin | Thỉnh thoảng |
| Display Name | Instance | Center Admin | Hiếm khi |
| Dark/Light Mode | Instance | End User | Thường xuyên |

---

## Theme Templates

### Available Templates

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           THEME TEMPLATE CATALOG                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  FREE THEMES                                                                    │
│  ════════════════════════════════════════════════════════════════════════════  │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │    CLASSIC      │  │    MODERN       │  │   FRIENDLY      │                │
│  │   ┌─────────┐   │  │   ┌─────────┐   │  │   ┌─────────┐   │                │
│  │   │ ══════  │   │  │   │ ══════  │   │  │   │ 🎨═════ │   │                │
│  │   │ ┌─┐ ┌─┐ │   │  │   │         │   │  │   │ ┌─┐ ┌─┐ │   │                │
│  │   │ │ │ │ │ │   │  │   │ ┌─────┐ │   │  │   │ │●│ │●│ │   │                │
│  │   │ └─┘ └─┘ │   │  │   │ │     │ │   │  │   │ └─┘ └─┘ │   │                │
│  │   └─────────┘   │  │   └─────────┘   │  │   └─────────┘   │                │
│  │                 │  │                 │  │                 │                │
│  │ Truyền thống    │  │ Tối giản       │  │ Nhiều màu sắc   │                │
│  │ Chuyên nghiệp   │  │ Hiện đại       │  │ Thân thiện      │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│                                                                                 │
│  PRO THEMES (Gói Pro+)                                                         │
│  ════════════════════════════════════════════════════════════════════════════  │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │    PLAYFUL      │  │    PREMIUM      │  │   DARK PRO      │                │
│  │   ┌─────────┐   │  │   ┌─────────┐   │  │   ┌─────────┐   │                │
│  │   │ 🌈✨🎮  │   │  │   │ ▓▓▓▓▓▓▓ │   │  │   │ ▒▒▒▒▒▒▒ │   │                │
│  │   │ ┌─┐ ┌─┐ │   │  │   │ ┌─────┐ │   │  │   │ ┌─────┐ │   │                │
│  │   │ │☺│ │☺│ │   │  │   │ │ ◆◆◆ │ │   │  │   │ │ ░░░ │ │   │                │
│  │   │ └─┘ └─┘ │   │  │   │ └─────┘ │   │  │   │ └─────┘ │   │                │
│  │   └─────────┘   │  │   └─────────┘   │  │   └─────────┘   │                │
│  │                 │  │                 │  │                 │                │
│  │ Animation       │  │ Sang trọng      │  │ Dark by default │                │
│  │ Gamification    │  │ IELTS/Business  │  │ Easy on eyes    │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│                                                                                 │
│  ENTERPRISE ONLY                                                                │
│  ════════════════════════════════════════════════════════════════════════════  │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐                                      │
│  │  CUSTOM THEME   │  │  WHITE LABEL    │                                      │
│  │   ┌─────────┐   │  │   ┌─────────┐   │                                      │
│  │   │ YOUR    │   │  │   │ YOUR    │   │                                      │
│  │   │ BRAND   │   │  │   │ BRAND   │   │                                      │
│  │   │ HERE    │   │  │   │ 100%    │   │                                      │
│  │   └─────────┘   │  │   └─────────┘   │                                      │
│  │                 │  │                 │                                      │
│  │ Full CSS access │  │ No KiteClass   │                                      │
│  │ Custom design   │  │ branding       │                                      │
│  └─────────────────┘  └─────────────────┘                                      │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Template Specifications

| Template | Primary Color | Style | Target Audience |
|----------|---------------|-------|-----------------|
| **Classic** | `#1e40af` (Blue) | Professional, Traditional | Trung tâm luyện thi, ôn tập |
| **Modern** | `#0ea5e9` (Sky) | Minimal, Clean | Trung tâm ngoại ngữ |
| **Friendly** | `#22c55e` (Green) | Colorful, Warm | Trung tâm đa lĩnh vực |
| **Playful** | `#f59e0b` (Amber) | Fun, Animated | Trung tâm trẻ em, mầm non |
| **Premium** | `#7c3aed` (Violet) | Luxurious, Elegant | IELTS, Business English |
| **Dark Pro** | `#6366f1` (Indigo) | Dark, Modern | Tech-savvy centers |

---

## Data Models

### Database Schema

```sql
-- KiteHub Database
-- ================

-- Theme templates (managed by KiteHub Admin)
CREATE TABLE themes.templates (
    id VARCHAR(50) PRIMARY KEY,           -- 'classic', 'modern', etc.
    name VARCHAR(100) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    preview_url VARCHAR(500),

    -- Pricing
    tier VARCHAR(20) NOT NULL,            -- 'free', 'pro', 'enterprise'

    -- Theme configuration
    config JSONB NOT NULL,                -- Full theme config

    -- Metadata
    version VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Instance theme assignments
CREATE TABLE themes.instance_themes (
    instance_id BIGINT PRIMARY KEY REFERENCES maintaining.instances(id),
    template_id VARCHAR(50) REFERENCES themes.templates(id),
    custom_css TEXT,                      -- Enterprise only
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by BIGINT REFERENCES sales.customers(id)
);

-- KiteClass Instance Database
-- ============================

-- Branding settings (managed by Center Admin)
CREATE TABLE settings.branding (
    id BIGSERIAL PRIMARY KEY,

    -- Visual branding
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    display_name VARCHAR(200),
    tagline VARCHAR(500),

    -- Colors (override template defaults)
    primary_color VARCHAR(7),             -- Hex: #0ea5e9
    secondary_color VARCHAR(7),

    -- Contact info shown in footer
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    address TEXT,

    -- Social links
    facebook_url VARCHAR(500),
    zalo_url VARCHAR(500),

    -- Metadata
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by BIGINT REFERENCES users(id)
);

-- User preferences (per user)
CREATE TABLE settings.user_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id),

    -- Theme preferences
    color_mode VARCHAR(10) DEFAULT 'system',  -- 'light', 'dark', 'system'
    font_size VARCHAR(10) DEFAULT 'medium',   -- 'small', 'medium', 'large'
    compact_mode BOOLEAN DEFAULT FALSE,

    -- Notification preferences
    email_notifications BOOLEAN DEFAULT TRUE,
    push_notifications BOOLEAN DEFAULT TRUE,

    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### TypeScript Types

```typescript
// Theme Template (from KiteHub)
interface ThemeTemplate {
  id: string;
  name: string;
  description: string;
  thumbnailUrl: string;
  previewUrl: string;
  tier: 'free' | 'pro' | 'enterprise';
  version: string;
  config: ThemeConfig;
}

// Theme Configuration
interface ThemeConfig {
  // Colors
  colors: {
    primary: ColorScale;
    secondary: ColorScale;
    accent: ColorScale;
    neutral: ColorScale;
    success: string;
    warning: string;
    error: string;
    info: string;
  };

  // Typography
  typography: {
    fontFamily: {
      sans: string;
      mono: string;
    };
    fontSize: Record<string, string>;
    fontWeight: Record<string, number>;
    lineHeight: Record<string, number>;
  };

  // Spacing & Layout
  spacing: Record<string, string>;
  borderRadius: Record<string, string>;

  // Shadows
  shadows: Record<string, string>;

  // Component-specific
  components: {
    button: ComponentTheme;
    input: ComponentTheme;
    card: ComponentTheme;
    sidebar: ComponentTheme;
    // ...
  };
}

interface ColorScale {
  50: string;
  100: string;
  200: string;
  300: string;
  400: string;
  500: string;  // Default
  600: string;
  700: string;
  800: string;
  900: string;
}

// Branding Settings (Instance level)
interface BrandingSettings {
  logoUrl: string | null;
  faviconUrl: string | null;
  displayName: string;
  tagline: string | null;
  primaryColor: string | null;  // Override template
  secondaryColor: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  address: string | null;
  socialLinks: {
    facebook?: string;
    zalo?: string;
  };
}

// User Preferences
interface UserPreferences {
  colorMode: 'light' | 'dark' | 'system';
  fontSize: 'small' | 'medium' | 'large';
  compactMode: boolean;
  emailNotifications: boolean;
  pushNotifications: boolean;
}

// Resolved Theme (combined)
interface ResolvedTheme {
  template: ThemeTemplate;
  branding: BrandingSettings;
  userPrefs: UserPreferences;

  // Computed
  effectiveColorMode: 'light' | 'dark';
  effectivePrimaryColor: string;
}
```

---

## API Endpoints

### KiteHub APIs

```
# Theme Templates
GET    /api/v1/themes/templates              # List available templates
GET    /api/v1/themes/templates/{id}         # Get template details
GET    /api/v1/themes/templates/{id}/preview # Get preview URL

# Instance Theme Management
GET    /api/v1/instances/{id}/theme          # Get instance theme
PUT    /api/v1/instances/{id}/theme          # Update instance theme
POST   /api/v1/instances/{id}/theme/preview  # Generate preview
```

### KiteClass Instance APIs

```
# Branding (Center Admin)
GET    /api/v1/settings/branding             # Get branding settings
PUT    /api/v1/settings/branding             # Update branding
POST   /api/v1/settings/branding/logo        # Upload logo
POST   /api/v1/settings/branding/favicon     # Upload favicon

# User Preferences (Any user)
GET    /api/v1/users/me/preferences          # Get my preferences
PUT    /api/v1/users/me/preferences          # Update my preferences

# Theme (Public)
GET    /api/v1/theme                         # Get resolved theme for current instance
```

---

## Frontend Implementation

### Theme Provider

```typescript
// providers/theme-provider.tsx
'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

interface ThemeContextType {
  theme: ResolvedTheme | null;
  colorMode: 'light' | 'dark';
  setColorMode: (mode: 'light' | 'dark' | 'system') => void;
  isLoading: boolean;
}

const ThemeContext = createContext<ThemeContextType | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [colorMode, setColorModeState] = useState<'light' | 'dark'>('light');

  // Fetch theme from API
  const { data: theme, isLoading } = useQuery({
    queryKey: ['theme'],
    queryFn: () => api.get<ResolvedTheme>('/theme'),
    staleTime: 1000 * 60 * 60, // 1 hour
  });

  // Apply CSS variables when theme changes
  useEffect(() => {
    if (theme) {
      applyThemeVariables(theme);
    }
  }, [theme]);

  // Handle color mode
  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove('light', 'dark');
    root.classList.add(colorMode);
  }, [colorMode]);

  const setColorMode = (mode: 'light' | 'dark' | 'system') => {
    if (mode === 'system') {
      const systemMode = window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light';
      setColorModeState(systemMode);
    } else {
      setColorModeState(mode);
    }

    // Save to user preferences
    api.put('/users/me/preferences', { colorMode: mode });
  };

  return (
    <ThemeContext.Provider value={{ theme, colorMode, setColorMode, isLoading }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
};
```

### Apply CSS Variables

```typescript
// lib/theme-utils.ts
export function applyThemeVariables(theme: ResolvedTheme) {
  const root = document.documentElement;
  const { template, branding } = theme;

  // Apply colors
  const primaryColor = branding.primaryColor || template.config.colors.primary[500];
  root.style.setProperty('--color-primary', primaryColor);

  // Generate color scale from primary
  const primaryScale = generateColorScale(primaryColor);
  Object.entries(primaryScale).forEach(([key, value]) => {
    root.style.setProperty(`--color-primary-${key}`, value);
  });

  // Apply other template colors
  const { colors } = template.config;
  Object.entries(colors.neutral).forEach(([key, value]) => {
    root.style.setProperty(`--color-neutral-${key}`, value);
  });

  // Apply typography
  const { typography } = template.config;
  root.style.setProperty('--font-sans', typography.fontFamily.sans);
  root.style.setProperty('--font-mono', typography.fontFamily.mono);

  // Apply border radius
  Object.entries(template.config.borderRadius).forEach(([key, value]) => {
    root.style.setProperty(`--radius-${key}`, value);
  });
}

// Generate color scale from a single color
function generateColorScale(baseColor: string): Record<string, string> {
  // Use chroma.js or similar library
  return {
    50: lighten(baseColor, 0.95),
    100: lighten(baseColor, 0.9),
    200: lighten(baseColor, 0.75),
    300: lighten(baseColor, 0.5),
    400: lighten(baseColor, 0.25),
    500: baseColor,
    600: darken(baseColor, 0.1),
    700: darken(baseColor, 0.25),
    800: darken(baseColor, 0.4),
    900: darken(baseColor, 0.5),
  };
}
```

### Tailwind Configuration

```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Use CSS variables for theming
        primary: {
          50: 'var(--color-primary-50)',
          100: 'var(--color-primary-100)',
          200: 'var(--color-primary-200)',
          300: 'var(--color-primary-300)',
          400: 'var(--color-primary-400)',
          500: 'var(--color-primary-500)',
          600: 'var(--color-primary-600)',
          700: 'var(--color-primary-700)',
          800: 'var(--color-primary-800)',
          900: 'var(--color-primary-900)',
          DEFAULT: 'var(--color-primary)',
        },
        // ... other colors
      },
      fontFamily: {
        sans: ['var(--font-sans)', 'system-ui', 'sans-serif'],
        mono: ['var(--font-mono)', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
      },
    },
  },
};
```

---

## Branding Settings UI

### Instance Admin Settings Page

```typescript
// app/(dashboard)/settings/branding/page.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useBranding, useUpdateBranding } from '@/hooks/use-branding';

const brandingSchema = z.object({
  displayName: z.string().min(2).max(200),
  tagline: z.string().max(500).optional(),
  primaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/).optional(),
  contactEmail: z.string().email().optional(),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
});

export default function BrandingSettingsPage() {
  const { data: branding, isLoading } = useBranding();
  const { mutate: updateBranding, isPending } = useUpdateBranding();

  const form = useForm({
    resolver: zodResolver(brandingSchema),
    defaultValues: branding,
  });

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Cài đặt thương hiệu"
        description="Tùy chỉnh giao diện và thông tin hiển thị của trung tâm"
      />

      <Form {...form}>
        <form onSubmit={form.handleSubmit(updateBranding)} className="space-y-8">

          {/* Logo Upload */}
          <Card>
            <CardHeader>
              <CardTitle>Logo & Favicon</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-8">
                <LogoUpload
                  currentUrl={branding?.logoUrl}
                  onUpload={(url) => form.setValue('logoUrl', url)}
                />
                <FaviconUpload
                  currentUrl={branding?.faviconUrl}
                  onUpload={(url) => form.setValue('faviconUrl', url)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Basic Info */}
          <Card>
            <CardHeader>
              <CardTitle>Thông tin cơ bản</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FormField
                control={form.control}
                name="displayName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tên hiển thị</FormLabel>
                    <FormControl>
                      <Input placeholder="Trung tâm Anh ngữ ABC" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="tagline"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Slogan</FormLabel>
                    <FormControl>
                      <Input placeholder="Nơi ươm mầm tài năng" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Colors */}
          <Card>
            <CardHeader>
              <CardTitle>Màu sắc</CardTitle>
              <CardDescription>
                Tùy chỉnh màu chủ đạo của giao diện
              </CardDescription>
            </CardHeader>
            <CardContent>
              <FormField
                control={form.control}
                name="primaryColor"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Màu chủ đạo</FormLabel>
                    <FormControl>
                      <div className="flex items-center gap-3">
                        <ColorPicker
                          value={field.value}
                          onChange={field.onChange}
                        />
                        <Input
                          {...field}
                          placeholder="#0ea5e9"
                          className="w-32 font-mono"
                        />
                      </div>
                    </FormControl>
                    <FormDescription>
                      Màu này sẽ được áp dụng cho buttons, links và các thành phần chính
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Color Preview */}
              <div className="mt-4 p-4 border rounded-lg">
                <p className="text-sm text-muted-foreground mb-2">Xem trước:</p>
                <div className="flex gap-2">
                  <Button style={{ backgroundColor: form.watch('primaryColor') }}>
                    Button Primary
                  </Button>
                  <Button variant="outline" style={{
                    borderColor: form.watch('primaryColor'),
                    color: form.watch('primaryColor')
                  }}>
                    Button Outline
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Contact Info */}
          <Card>
            <CardHeader>
              <CardTitle>Thông tin liên hệ</CardTitle>
              <CardDescription>
                Hiển thị ở footer và trang liên hệ
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Email, Phone, Address fields */}
            </CardContent>
          </Card>

          {/* Theme Info (Read-only) */}
          <Card>
            <CardHeader>
              <CardTitle>Theme Template</CardTitle>
              <CardDescription>
                Được quản lý trên KiteHub
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4">
                <img
                  src={branding?.template?.thumbnailUrl}
                  alt={branding?.template?.name}
                  className="w-24 h-16 object-cover rounded"
                />
                <div>
                  <p className="font-medium">{branding?.template?.name}</p>
                  <p className="text-sm text-muted-foreground">
                    {branding?.template?.description}
                  </p>
                </div>
              </div>
              <p className="mt-4 text-sm text-muted-foreground">
                <InfoIcon className="inline h-4 w-4 mr-1" />
                Để thay đổi theme, vui lòng truy cập{' '}
                <a href="https://kitehub.vn/settings" className="text-primary underline">
                  KiteHub
                </a>
              </p>
            </CardContent>
          </Card>

          {/* Submit */}
          <div className="flex justify-end">
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
```

---

## Business Rules

### Theme Access by Plan

| Feature | Basic | Pro | Enterprise |
|---------|:-----:|:---:|:----------:|
| Free Themes | ✅ | ✅ | ✅ |
| Pro Themes | ❌ | ✅ | ✅ |
| Custom Primary Color | ✅ | ✅ | ✅ |
| Custom Logo/Favicon | ✅ | ✅ | ✅ |
| Custom CSS | ❌ | ❌ | ✅ |
| White Label | ❌ | ❌ | ✅ |
| Custom Theme Design | ❌ | ❌ | ✅ (Add-on) |

### Theme Change Rules

1. **Downgrade**: Nếu downgrade từ Pro → Basic, theme Pro sẽ chuyển về Classic (Free)
2. **Upgrade**: Khi upgrade, có thể chọn theme mới ngay lập tức
3. **Preview**: Luôn cho phép preview trước khi apply
4. **Rollback**: Lưu theme trước đó để rollback nếu cần

---

## Actions

### Chọn theme khi tạo instance
Trên KiteHub → Create Instance → Step 3: Choose Theme

### Đổi logo/màu
Instance → Settings → Branding → Upload/Edit

### Đổi theme template
KiteHub → Instances → [instance] → Settings → Theme

### Custom CSS (Enterprise)
KiteHub → Instances → [instance] → Settings → Advanced → Custom CSS
