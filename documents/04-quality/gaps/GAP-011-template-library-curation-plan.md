# GAP-011: Template Library Curation Plan + Review Standards

**Status:** 🟡 PLANNED (Wave 1 Sprint 0)
**Branch:** wave/01-foundation
**Priority:** 🔴 P0 (foundation — template-first architecture không work nếu không có templates)
**Domain:** Design / Product / Backend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md`
- `GAP-004` (template-based composition)
- `GAP-007` (resource classification)

## Problem

Template-first architecture cần **curated template library**. Hiện tại:
- ❌ Không có template nào được tạo
- ❌ Không có plan tạo templates (who, when, how)
- ❌ Không có review standards cho templates
- ❌ Không có CMS/admin UI để manage templates

Không có templates → template-first fail → mọi request fall back AI → overload (quay về vấn đề ban đầu).

## Curation Plan

### Sprint 0 (foundation): 30 initial templates

| Resource Type | Count | Breakdown |
|---------------|-------|-----------|
| Banners | 10 | 3 audience (THPT/ĐH/Kids) × 3 tone (pro/friendly/energetic) + 1 generic |
| Course thumbnails | 10 | 5 category (Toán/Văn/Ngoại ngữ/KHTN/Năng khiếu) × 2 style |
| Hero images | 5 | Generic hero (landing page), mix audiences |
| Email headers | 5 | Welcome / Payment / Reminder / Newsletter / Marketing |

**Budget & timeline:**
- 1 brand designer (freelance hoặc in-house), ~2 weeks
- Cost estimate: $1500-3000 (Vietnam freelance market)
- Deliverable: SVG templates + metadata JSON (placeholder locations, brand-color slots)

### Quarterly expansion

- +10 templates/quarter based on usage analytics
- Track: which templates used most, which never used → iterate
- A/B test: template A vs B for same use case → measure engagement

### CMS / Admin UI (phase 2)

Admin dashboard để:
- Upload new templates (SVG + preview PNG + metadata)
- Review queue cho community submissions (future)
- Deprecate old templates
- View analytics per template

## Review Standards (5 criteria)

Mỗi template phải pass tất cả 5 tiêu chuẩn trước khi approve.

### 1. Brand-Agnostic
- [ ] Template sử dụng CSS variables/placeholders, KHÔNG hardcode colors
- [ ] Test với 3 brand palettes khác nhau: warm (red/orange), cool (blue/teal), neutral (grey/black)
- [ ] Logo placeholder work với logo có 3 aspect ratios: 1:1 (square), 3:1 (wide), 2:3 (tall)

### 2. Accessibility (WCAG AA)
- [ ] Text contrast ratio ≥ 4.5:1 (normal text)
- [ ] Text contrast ratio ≥ 3:1 (large text, 18pt+)
- [ ] Font size ≥ 14px cho body text
- [ ] Color không phải yếu tố duy nhất truyền tải info

### 3. Responsive
- [ ] Render tốt ở 320px (mobile sm)
- [ ] Render tốt ở 768px (tablet)
- [ ] Render tốt ở 1920px (desktop full HD)
- [ ] Render tốt ở 3840px (4K) — for print-ready
- [ ] Text không overflow, image không stretch méo

### 4. Text Safety Zone
- [ ] Headline placeholder rộng đủ cho 50 chars tiếng Việt (có dấu)
- [ ] Không overlap với logo critical area
- [ ] Không overlap với CTA button
- [ ] Có padding tối thiểu 16px cho mobile, 32px cho desktop

### 5. Brand Family Consistency
- [ ] Nếu là set "friendly tone" — tất cả templates trong set dùng cùng visual language
- [ ] Cùng font family, icon style, corner radius
- [ ] User chọn 1 template → switch sang template khác trong cùng set → không "jarring"

## Template Metadata Schema

```json
{
  "id": "banner-modern-pro-01",
  "name": "Modern Professional Banner 01",
  "category": "banner",
  "audience": ["student-university", "student-highschool"],
  "tone": "professional",
  "style": "minimal",
  "svgUrl": "s3://templates/banner-modern-pro-01.svg",
  "previewUrl": "s3://templates/previews/banner-modern-pro-01.png",
  "placeholders": {
    "logo": { "x": 40, "y": 40, "maxWidth": 120 },
    "headline": { "x": 40, "y": 200, "maxWidth": 600, "maxChars": 50 },
    "subheadline": { "x": 40, "y": 260, "maxWidth": 500, "maxChars": 100 },
    "ctaText": { "x": 40, "y": 340, "maxChars": 20 }
  },
  "colorSlots": {
    "primary": "#background-accent",
    "secondary": "#headline-color",
    "accent": "#cta-bg"
  },
  "reviewPassed": true,
  "reviewedBy": "designer-uuid",
  "reviewedAt": "2026-04-20T00:00:00Z"
}
```

## Acceptance Criteria

- [ ] Template curation plan published & approved by stakeholder
- [ ] Budget allocated for Sprint 0 (30 templates)
- [ ] Designer hired / assigned
- [ ] 5 review criteria documented (this gap)
- [ ] 30 initial templates created + reviewed + approved
- [ ] Admin UI for template management (or fallback: manual JSON + S3 upload)
- [ ] Quarterly expansion budget planned
- [ ] Analytics tracking template usage

## Dependencies

- Blocks: GAP-004, GAP-007 (classification router cần templates tồn tại)
- Requires: designer resource (business decision)

## Log

- 2026-04-14 — Phát hiện template-first architecture cần plan cụ thể cho template creation
