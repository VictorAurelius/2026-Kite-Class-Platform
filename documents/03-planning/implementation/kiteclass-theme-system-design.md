# KiteClass Theme System & Content Architecture

**Ngày tạo**: 2026-03-18
**Status**: Design document - chưa implement
**Liên quan**: ui-refactor-plan.md, ai-local-implementation-plan.md, kitehub-implementation-plan.md

---

## 1. Tổng quan

### Vấn đề với design cũ
- Tất cả content phải qua AI Branding → bottleneck
- Chỉ 1 UI cho tất cả instances → trùng lặp
- Không phân biệt branding content vs operational content
- Instance phải chờ AI xong mới dùng được

### Design mới: 3 pillars

```
┌──────────────────────────────────────────────────┐
│              KITECLASS INSTANCE UI                │
├────────────────┬────────────────┬────────────────┤
│   TEMPLATES    │    THEMES      │    CONTENT     │
│   (Layout)     │   (Visual)     │   (Data)       │
├────────────────┼────────────────┼────────────────┤
│ 2 base layouts │ AI-generated   │ 3 content types│
│ 12+ sections   │ per logo       │ AI / CMS / CRUD│
│ Bật/tắt/sắp   │ CSS Variables  │ Slot-based     │
│ xếp sections   │ Unlimited      │ placement      │
│                │ combinations   │                │
│ Chọn 1 lần    │ Auto-generate  │ Ongoing        │
│ khi setup      │ khi branding   │ management     │
└────────────────┴────────────────┴────────────────┘
```

---

## 2. Templates (Layout Layer)

### 2 Base Templates

#### Template "Personal" - Giáo viên độc lập, gia sư, mentor

```
┌────────────────────────────┐
│ Header: Logo + Name + CTA  │
├────────────────────────────┤
│ Hero: Ảnh cá nhân + Bio    │
│ "Xin chào, tôi là..."     │
├────────────────────────────┤
│ Chuyên môn & Kinh nghiệm  │
├────────────────────────────┤
│ Khóa học (cards nhỏ)       │
├────────────────────────────┤
│ Học viên nói gì            │
├────────────────────────────┤
│ Lịch dạy & Đặt lịch       │
├────────────────────────────┤
│ Liên hệ                   │
└────────────────────────────┘
```

**Đặc điểm**: Intimate, personal brand, ít sections, warm tone

#### Template "Organization" - Trung tâm, trường học, doanh nghiệp

```
┌────────────────────────────┐
│ Header: Logo + Nav + CTA   │
├────────────────────────────┤
│ Hero: Banner + Headline    │
│ + Stats + CTA              │
├────────────────────────────┤
│ Giới thiệu tổ chức        │
├────────────────────────────┤
│ Khóa học (grid/carousel)   │
├────────────────────────────┤
│ Đội ngũ giáo viên          │
├────────────────────────────┤
│ Thành tích & Chứng chỉ    │
├────────────────────────────┤
│ Gallery ảnh                │
├────────────────────────────┤
│ Tin tức & Sự kiện          │
├────────────────────────────┤
│ Tuyển sinh                 │
├────────────────────────────┤
│ Bảng giá                   │
├────────────────────────────┤
│ Phụ huynh nói gì           │
├────────────────────────────┤
│ FAQ                        │
├────────────────────────────┤
│ Liên hệ & Bản đồ          │
├────────────────────────────┤
│ CTA cuối                   │
└────────────────────────────┘
```

**Đặc điểm**: Professional, nhiều sections, scalable

### Section System (12+ sections, bật/tắt)

| Section | Personal | TT Ngoại ngữ | TT Kỹ năng | Trường học | DN Đào tạo |
|---------|----------|--------------|------------|-----------|------------|
| Hero | ✅ | ✅ | ✅ | ✅ | ✅ |
| Giới thiệu | ✅ | ✅ | ✅ | ✅ | ✅ |
| Khóa học | ✅ | ✅ | ✅ | ✅ | ✅ |
| Giáo viên | ❌ | ✅ | ✅ | ✅ | ✅ |
| Chứng chỉ | ✅ | ✅ | ⚠️ | ✅ | ⚠️ |
| Gallery | ⚠️ | ✅ | ✅ | ✅ | ⚠️ |
| Tin tức | ❌ | ⚠️ | ⚠️ | ✅ | ⚠️ |
| Tuyển sinh | ❌ | ✅ | ✅ | ✅ | ✅ |
| Bảng giá | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Testimonials | ✅ | ✅ | ✅ | ✅ | ✅ |
| FAQ | ⚠️ | ✅ | ✅ | ✅ | ✅ |
| Phụ huynh portal | ❌ | ⚠️ | ❌ | ✅ | ❌ |
| Liên hệ | ✅ | ✅ | ✅ | ✅ | ✅ |

✅ = mặc định BẬT, ⚠️ = mặc định TẮT (user bật), ❌ = không có trong template

### AI tự suggest preset khi đăng ký

```
Register step: "Loại hình của bạn?"
  → Giáo viên độc lập    → Template Personal + preset A
  → Trung tâm ngoại ngữ  → Template Org + preset B (Chứng chỉ ON)
  → Trung tâm kỹ năng    → Template Org + preset C (Gallery ON)
  → Trường học            → Template Org + preset D (Tin tức ON, PH ON)
  → Doanh nghiệp         → Template Org + preset E (Corporate tone)
```

---

## 3. Themes (Visual Layer)

### Theme = CSS Variables (không thêm code)

```css
:root {
  /* AI generate từ logo */
  --primary: #1E40AF;
  --secondary: #3B82F6;
  --accent: #F59E0B;
  --background: #FFFBF5;

  /* AI suggest */
  --font-heading: 'Inter';
  --font-body: 'Inter';
  --border-radius: 12px;
  --shadow: 0 4px 6px rgba(0,0,0,0.07);
}
```

### AI tạo 3 theme variants từ 1 logo

```json
{
  "variants": [
    {
      "name": "Chuyên nghiệp",
      "colors": { "primary": "#1E40AF", "secondary": "#3B82F6", "accent": "#F59E0B" },
      "mood": "professional, trustworthy"
    },
    {
      "name": "Ấm áp",
      "colors": { "primary": "#EA580C", "secondary": "#FB923C", "accent": "#059669" },
      "mood": "warm, friendly, approachable"
    },
    {
      "name": "Hiện đại",
      "colors": { "primary": "#7C3AED", "secondary": "#A78BFA", "accent": "#10B981" },
      "mood": "modern, innovative, tech-forward"
    }
  ]
}
```

### Chống trùng lặp: 3 layers

```
Layer 1: Theme colors (AI generate từ logo → unique per instance)
Layer 2: Content thật (ảnh lớp học, giáo viên → mỗi TT khác nhau)
Layer 3: Section config (thứ tự, bật/tắt → mỗi TT customize khác)
= Mỗi instance UNIQUE
```

---

## 4. Content (Data Layer)

### 3 loại content - KHÔNG ĐI CHUNG 1 ĐƯỜNG

```
┌─────────────────────────────────────────────────────┐
│                   CONTENT TYPES                      │
├──────────────┬──────────────────┬───────────────────┤
│ AI-managed   │  User-managed    │  Operational      │
│ (Branding)   │  (CMS)           │  (CRUD)           │
├──────────────┼──────────────────┼───────────────────┤
│ Qua AI       │ Upload trực tiếp │ Nhập hàng ngày    │
│ Branding     │ Không qua AI     │ Qua KiteClass     │
│ Service      │                  │ Core              │
├──────────────┼──────────────────┼───────────────────┤
│ • Logo       │ • Ảnh lớp học    │ • Học viên        │
│ • Hero image │ • Ảnh giáo viên  │ • Lớp học         │
│ • Color theme│ • Chứng chỉ      │ • Điểm danh       │
│ • Marketing  │ • Tin tức        │ • Học phí          │
│   copy       │ • Sự kiện       │ • Lịch học         │
│ • Social     │ • Gallery        │ • Bài tập         │
│   banners    │ • Testimonials   │ • Điểm số         │
│ • OG image   │ • FAQ content    │ • Hóa đơn         │
│              │ • Contact info   │                   │
├──────────────┼──────────────────┼───────────────────┤
│ Khi nào:     │ Khi nào:         │ Khi nào:          │
│ Setup 1 lần  │ Bất kỳ lúc nào  │ Hàng ngày         │
│ + rebrand    │                  │                   │
├──────────────┼──────────────────┼───────────────────┤
│ Ai trigger:  │ Ai trigger:      │ Ai trigger:       │
│ User request │ User upload/edit │ Staff operations  │
│ → AI process │ → Lên UI ngay    │ → DB + API        │
│ → Preview    │                  │                   │
│ → Apply      │                  │                   │
└──────────────┴──────────────────┴───────────────────┘
```

---

## 5. Slot-based Asset Placement

### Mỗi section có predefined slots

```
Section "Hero":
  [AI slot] hero-image      1792x1024  JPG    AI generate
  [AI slot] headline        Text              AI generate
  [AI slot] tagline         Text              AI generate
  [CMS slot] cta-text       Text              User edit

Section "Giáo viên":
  [CMS slot] teacher-photo  400x400    JPG    User upload
  [CMS slot] teacher-name   Text              User nhập
  [CMS slot] teacher-bio    Text              User nhập
  [CMS slot] teacher-cert   Image[]           User upload

Section "Chứng chỉ":
  [CMS slot] cert-image     Image[]           User upload
  [CMS slot] cert-title     Text              User nhập
  [CMS slot] cert-student   Text              User nhập
```

### Quy tắc slot

| Quy tắc | Chi tiết |
|---------|----------|
| **AI auto-place** | AI output tự động vào đúng AI slots |
| **User manages CMS slots** | User upload/edit trực tiếp, không qua AI |
| **No free drag** | Không kéo thả tự do (giữ design consistency) |
| **Override allowed** | User có thể thay ảnh trong AI slot (upload ảnh riêng) |
| **Auto-crop** | Upload ảnh → tự crop đúng tỷ lệ slot |
| **Asset library** | AI tạo nhiều variants → lưu library → user swap |
| **Section reorder** | User có thể đổi thứ tự sections (kéo lên/xuống) |

---

## 6. Instance Initialization Flow (Redesigned)

### Cũ: Bắt buộc AI trước

```
Register → Upload logo → AI process (3-5 phút) → Instance ready
```

### Mới: Dùng ngay, AI optional

```
Register
  → Chọn loại hình (GV độc lập / TT / Trường / DN)
  → AI suggest template + section preset
  → Instance READY NGAY (default theme)
  ↓
  Song song 3 paths:

  Path A: AI Branding (optional, background)
    Upload logo → AI process → Thông báo khi xong → Preview → Apply
    User có thể dùng instance TRƯỚC khi AI xong

  Path B: CMS Content (anytime)
    Upload ảnh, viết tin, thêm certificates → Lên landing page ngay

  Path C: Operations (daily)
    Thêm học viên, tạo lớp, điểm danh → KiteClass Core
```

### Thời gian đến sử dụng

| | Cũ | Mới |
|---|---|---|
| Instance sử dụng được | 3-5 phút (chờ AI) | **Ngay lập tức** |
| Landing page có content | Sau AI xong | **Ngay** (default + CMS) |
| Branding hoàn chỉnh | 3-5 phút | 3-5 phút (nhưng không block) |
| Thêm khóa học | Sau AI | **Ngay** |

---

## 7. Preview & Customization Flow

### Theme Configurator (trong KiteHub Branding Wizard)

```
┌────────────────────────────────────────────────┐
│  Branding Wizard                               │
│                                                │
│  Step 1: Upload logo                           │
│  Step 2: AI analyze (background)               │
│  Step 3: Chọn theme                            │
│                                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │Chuyên    │ │  Ấm áp   │ │ Hiện đại │      │
│  │nghiệp   │ │          │ │          │      │
│  │[active]  │ │          │ │          │      │
│  └──────────┘ └──────────┘ └──────────┘      │
│                                                │
│  ┌──────────────────────────────────────┐     │
│  │  LIVE PREVIEW (iframe)              │     │
│  │                                      │     │
│  │  KiteClass FE + selected theme      │     │
│  │  CSS Variables injected via         │     │
│  │  postMessage                        │     │
│  │                                      │     │
│  └──────────────────────────────────────┘     │
│                                                │
│  Step 4: Customize sections                    │
│    ☑ Hero    ☑ Khóa học   ☑ Giáo viên        │
│    ☐ Gallery  ☐ Tin tức   ☑ Bảng giá          │
│    [↕ Kéo đổi thứ tự]                         │
│                                                │
│  Step 5: [Áp dụng theme này]                   │
└────────────────────────────────────────────────┘
```

### Kỹ thuật: iframe + postMessage

```typescript
// KiteHub (parent) - theme configurator
const applyTheme = (theme: ThemeConfig) => {
  iframeRef.current.contentWindow.postMessage({
    type: 'APPLY_THEME',
    theme: theme.colors,
    content: theme.copy,
  }, '*');
};

// KiteClass FE (iframe) - theme receiver
window.addEventListener('message', (e) => {
  if (e.data.type === 'APPLY_THEME') {
    Object.entries(e.data.theme).forEach(([key, value]) => {
      document.documentElement.style.setProperty(`--${key}`, value);
    });
  }
});
```

---

## 8. Data Model

### Theme Config (stored per instance)

```json
{
  "instanceId": "abc-123",
  "templateId": "organization",
  "theme": {
    "name": "Chuyên nghiệp",
    "colors": {
      "primary": "#1E40AF",
      "secondary": "#3B82F6",
      "accent": "#F59E0B",
      "background": "#FFFBF5"
    },
    "fonts": {
      "heading": "Inter",
      "body": "Inter"
    },
    "borderRadius": "12px"
  },
  "sections": {
    "hero": { "enabled": true, "order": 0 },
    "about": { "enabled": true, "order": 1 },
    "courses": { "enabled": true, "order": 2 },
    "teachers": { "enabled": true, "order": 3 },
    "certificates": { "enabled": true, "order": 4 },
    "gallery": { "enabled": false, "order": 5 },
    "news": { "enabled": false, "order": 6 },
    "pricing": { "enabled": true, "order": 7 },
    "testimonials": { "enabled": true, "order": 8 },
    "faq": { "enabled": false, "order": 9 },
    "contact": { "enabled": true, "order": 10 }
  },
  "assets": {
    "hero-image": "https://cdn.../hero.jpg",
    "logo": "https://cdn.../logo.svg",
    "logo-light": "https://cdn.../logo-light.svg",
    "og-image": "https://cdn.../og.jpg",
    "favicon": "https://cdn.../favicon.png"
  }
}
```

---

## 9. Implementation PRs

### PR-THEME-1: Theme System Foundation
**Scope**:
- [ ] CSS Variables theme system trong KiteClass FE
- [ ] ThemeProvider component (đọc config, apply CSS vars)
- [ ] postMessage API cho iframe preview
- [ ] Default theme (khi chưa branding)
**Estimate**: 2 ngày

### PR-THEME-2: Template System (2 templates)
**Scope**:
- [ ] Template "Personal" (7 sections)
- [ ] Template "Organization" (13 sections)
- [ ] Section toggle (bật/tắt/reorder)
- [ ] Dynamic import (code splitting per template)
**Estimate**: 3 ngày

### PR-THEME-3: CMS Slots (User Content)
**Scope**:
- [ ] Slot definition per section
- [ ] CMS editor UI (upload, edit text, reorder)
- [ ] Image auto-crop per slot spec
- [ ] Asset library (AI + user uploads)
**Estimate**: 3 ngày

### PR-THEME-4: AI Branding → Theme Config
**Scope**:
- [ ] Update AI output format → theme config JSON
- [ ] Auto-place AI assets vào AI slots
- [ ] 3 variant generation
- [ ] Theme preview trong KiteHub wizard (iframe)
**Estimate**: 2 ngày

### PR-THEME-5: Instance Init Redesign
**Scope**:
- [ ] Register flow: chọn loại hình → AI suggest preset
- [ ] Instant instance (default theme, no AI wait)
- [ ] Background AI processing
- [ ] Notification khi branding ready
**Estimate**: 2 ngày

**Total estimate: 12 ngày**

---

## 10. Tài liệu liên quan (cần update)

| Document | Cần update gì |
|----------|--------------|
| `kitehub-implementation-plan.md` | Phase 4 AI Branding: thêm theme config output, slot-based placement |
| `04-kitehub-prs.md` | PR 4.8: update AI Branding output format |
| `05-kitehub-frontend-prs.md` | PR 5.5: update Branding Wizard với theme preview |
| `ai-local-implementation-plan.md` | Update AI output → theme config mapping |
| `ui-refactor-plan.md` | Add KiteClass FE section, reference theme system |
| `frontend-plan.md` | Add KiteClass template/theme development |
| `03-frontend-prs.md` | Add PRs cho landing page templates |
