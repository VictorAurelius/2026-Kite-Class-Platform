# GAP-004: Template-based image composition (Canva-like)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** AI / Frontend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/01-business/kitehub/ai-branding/rules.md` (rules AIB-10 đến AIB-13)
- `documents/03-planning/implementation/ai-local-implementation-plan.md`

## Problem

`TemplateGalleryService` đã có cho **theme/branding** (pre-built templates, không cần AI). Nhưng **images** (banner, hero, course thumbnail) hiện tại chỉ có AI generation — không có template fallback.

80% use cases có thể dùng template compose với branding colors thay vì AI → tiết kiệm resource, instant UX, consistent quality.

## Context

- Canva/Figma pattern: user chọn template → customize text + colors → export image
- Không cần AI cho những use cases phổ biến (banner generic, thumbnail chuẩn)
- AI chỉ dùng khi user muốn custom/unique
- Giảm load Ollama/SD → GPU/CPU rảnh cho tasks thực sự cần AI

## Evidence

- Rule AIB-10: "Template gallery — Pre-built templates, không cần AI" — CÓ cho theme, **CHƯA có cho images**
- `TemplateGalleryService` cover: education, business, general categories → chỉ themeConfig JSON
- Không có `ImageTemplateService` hoặc tương đương

## Proposed Fix

**PR-AI-7: Image Template Gallery**

**1. Tạo Image Templates dưới dạng SVG/HTML:**
```
templates/images/
├── banners/
│   ├── banner-modern-01.svg    (placeholders: {brand_name}, {primary_color}, {tagline})
│   ├── banner-classic-01.svg
│   └── ...
├── thumbnails/
│   ├── course-thumb-minimal.svg
│   └── course-thumb-bold.svg
└── heroes/
    └── ...
```

**2. Backend service:**
```java
@Service
public class ImageTemplateService {
  // List available templates
  List<ImageTemplate> listTemplates(String category);

  // Compose: template + brand params → image
  byte[] composeImage(Long templateId, ImageParams params);
  // Steps: SVG template → substitute placeholders → convert to PNG via headless browser or Batik
}
```

**3. Frontend flow:**
```
User clicks "Add Banner"
  ↓
Show 2 options:
  [Template Gallery]  ← instant, always works
  [AI Generate]       ← fallback for custom needs
  ↓
Template selected → customize (text, colors, font) → export PNG
```

**4. Lựa chọn tech:**
- SVG + CSS variables → substitute → html-to-image library (client-side, instant)
- OR server-side: Apache Batik (Java SVG → PNG)
- OR Resvg/librsvg (Rust, fast)

## Acceptance Criteria

- [ ] 10+ image templates pre-built (banners, thumbnails, heroes)
- [ ] FE có gallery UI chọn template
- [ ] Customize (text, colors) realtime preview
- [ ] Export PNG in <2s
- [ ] AI generation thành fallback option, không phải default
- [ ] Metrics: % template vs % AI usage tracked

## Benefits

- **Latency**: Instant (<2s) vs AI (5s-5min)
- **Cost**: $0 compute vs AI compute cost
- **Consistency**: Quality đồng nhất
- **Reliability**: Không phụ thuộc model availability
- **UX**: Canva-like editing = familiar pattern

## Dependencies

- Không blocked bởi gap nào
- Có thể làm parallel với PR-AI-2 (base image generation)

## Log

- 2026-04-14 — Phát hiện khi review AI design; opportunity cho quick win UX
