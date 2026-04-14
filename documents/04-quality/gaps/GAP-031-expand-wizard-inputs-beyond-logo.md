# GAP-031: Expand Wizard Inputs Beyond Logo (Rich Brand Context)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (AI output quality blocker)
**Domain:** UX / Product / AI
**Detected:** 2026-04-14 (user raised)

## Problem

Current wizard (GAP-013) chỉ collect 4 inputs:
- Logo (optional)
- Audience (preset)
- Tone (preset)
- Template choice

**→ Quá thin. AI không đủ context để generate quality output.**

### So sánh best practice

| Tool | Inputs |
|------|--------|
| Canva Brand Kit | Name, description, industry, colors, tone |
| Wix ADI | Business type, services, location, hours, about, target |
| Durable.co | Name, category, services, hours, location, tagline, about |
| Webflow | Business type, services, target, style, images |
| **Our v2 (hiện tại)** | Logo + audience + tone + template (inadequate) |

### Impact của thin input

- Banner AI-generated: "Welcome to [Center Name]" ← generic, không personalize
- About-us text: không tailor theo services
- Course thumbnails: không có visual cue về môn học
- Marketing copy: generic "education-focused"

## Proposed Fix: Rich Brand Context Collection

### Tier 1: Essential (mandatory)

| Field | Type | Why |
|-------|------|-----|
| Tên trung tâm | text | Headlines, about-us copy |
| Category | radio | STEM / Ngoại ngữ / NT / TT / Tổng hợp |
| Môn học chính | multi-select | Relevant copy, course thumbnails |
| Target audience | radio | Already have (keep) |
| Tone | radio | Already have (keep) |

### Tier 2: High-value (optional nhưng khuyến khích)

| Field | Type | Why |
|-------|------|-----|
| Tagline | text (≤80 chars) | Hero subtitle, marketing |
| About description | textarea (1-2 câu) | AI context cho copy gen |
| Keywords | chips (3-5) | Guide AI style: "chuyên nghiệp, sáng tạo" |
| Location | radio | Bắc/Trung/Nam style preference |
| Năm thành lập | number | "Since 2015" trust signal |
| Logo | file upload | AI extract colors (already have) |

### Tier 3: Enrichment (optional)

| Field | Type | Why |
|-------|------|-----|
| Quy mô hiện tại | radio | Social proof |
| Website hiện tại | url | AI học style nếu có |
| Reference centers | url list | "Similar vibe to..." |
| Anti-patterns | chips | "Avoid..." |
| Contact info | email + phone | Footer, contact page |

## Revised Wizard (10 steps với progressive disclosure)

```
1. Welcome + info
2. Thông tin cơ bản (3 fields: Tên *, Tagline, Năm)
3. Dịch vụ (3 fields: Category *, Môn học *, Audience *)
4. Thương hiệu (3 fields: Tone *, Keywords, About)
5. Logo (1 field, optional)
6. Location (1 field — Bắc/Trung/Nam)
7. Advanced (collapsed) — enrichment fields
8. Template selection (filtered by Tier 1)
9. Preview + approve per resource
10. Deploy + celebration
```

### Progressive Disclosure Pattern

```tsx
<WizardStep title="Thương hiệu">
  <FormField label="Tone" required>{/* radio */}</FormField>

  <details>
    <summary>Tùy chọn nâng cao (giúp AI tốt hơn)</summary>
    <FormField label="Keywords">...</FormField>
    <FormField label="About">...</FormField>
  </details>

  <ButtonNext>Tiếp theo</ButtonNext>
</WizardStep>
```

### Timing Estimates

- Rush mode (chỉ essentials): 3-4 min
- Standard mode: 6-8 min
- Power user mode (all fields): 10-15 min

## AI Prompt Enhancement

Với rich inputs, AI prompt có thể compose:

```
BEFORE (thin input):
"Generate banner for education center, professional tone"

AFTER (rich input):
"Generate banner for 'Trung tâm Anh ngữ ABC',
 established 2015, serving high school students in HCM.
 Services: IELTS prep, Communication English.
 Tagline: 'Chinh phục IELTS với giáo viên bản ngữ'.
 Tone: professional, trustworthy, approachable.
 Keywords: quality, experienced, results.
 Use warm blue palette matching logo colors.
 Include subtle SG cityscape accent."
```

→ AI output **personalized, contextual, high quality**.

## Fallback Strategy

Missing fields → hợp lý defaults:

| Missing | Fallback |
|---------|----------|
| Tên | Tenant slug capitalized |
| Môn học | "giáo dục tổng hợp" |
| Tagline | AI-generated từ audience + tone |
| About | Skip, không include trong output |
| Location | Default "Việt Nam" |
| Keywords | Derived từ audience + tone |

## Data Reuse

Thu thập 1 lần → dùng nhiều nơi:

- **Branding assets**: banner, hero, thumbnails
- **Landing page**: about section, services list
- **Email templates**: welcome email personalization
- **SEO**: meta description, title tags
- **Admin directory**: tenant profile listing
- **Marketing insights**: aggregate data for product decisions

## Acceptance Criteria

- [ ] Wizard expanded to 10 steps (progressive disclosure)
- [ ] 5 Tier 1 mandatory fields implemented
- [ ] 6 Tier 2 high-value fields with clear benefit indicators
- [ ] 5 Tier 3 enrichment fields behind "Advanced" toggle
- [ ] AI prompt templates updated to use all context
- [ ] Fallback logic for missing optional fields
- [ ] A/B test: thin vs rich input → quality score + satisfaction
- [ ] Data reused in 6+ surfaces (list above)
- [ ] Timing validated: rush mode 3-4min, full mode 10-15min

## UX Considerations

- ❌ Wizard quá dài → abandonment
- ✅ Mandatory fields minimal (5) — rush mode 3 min OK
- ✅ Clear "Skip" buttons cho optional
- ✅ Progress indicator hiển thị "2/5 bước bắt buộc"
- ✅ Preview updates real-time khi nhập fields

## Dependencies

- Supersedes GAP-013 (wizard UX) with expanded scope
- Requires GAP-020 (state persistence) — 10-step wizard cần save draft
- Informs GAP-008 (agent workflow) — analyzer uses new fields

## Log

- 2026-04-14 — User raised: "input chỉ có logo là không hợp lý"
