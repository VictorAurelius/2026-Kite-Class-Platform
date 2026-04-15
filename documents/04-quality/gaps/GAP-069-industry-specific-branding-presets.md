# GAP-069: Industry-Specific Branding Presets (VN School Types)

**Status:** 🟢 DONE (Wave 3 Sub-PR 3.7, merged 2026-04-14; SegmentPicker in Welcome step with 5 VN segments. BrandingPreset entity + backend template tagging deferred to follow-up when template library GAP-011 lands.)
**Priority:** 🟠 P1 (persona blocker for K-12 + center + university segments)
**Domain:** AI Branding / Product / UX
**Detected:** 2026-04-14 (simulation-gap-finder run on Wave 3 scope)
**Matrix cell:** Owner × Configuration × C9 Commercial + C2 UX

## Problem

Guided wizard (GAP-013) asks cho audience + tone generic — không phản ánh khác biệt nghiệp vụ giữa 3 segment thị trường giáo dục Việt Nam:

| Segment | Brand expectations |
|---------|---------------------|
| K-12 (trường phổ thông) | Thân thiện, formal-ish, color palette ấm (đỏ / xanh dương đậm), typography serif hoặc rounded, assets hình sinh viên phù hợp tuổi |
| Trung tâm (center) | Trẻ trung, energetic, bright colors, sans-serif, CTA assertive ("Đăng ký ngay"), hero banner marketing-driven |
| Đại học / cao đẳng | Academic formal, muted palette, serif headings, photography-grade imagery, giữ dignity |

Hiện wizard chỉ có text prompt "audience" → user tự viết → output AI không consistent với conventions của segment.

## Evidence

- `kitehub-frontend/src/app/(customer)/branding/wizard/page.tsx` — wizard hiện tại không có segment picker
- AI branding rule §4.1 yêu cầu 6 template previews nhưng không group theo segment
- Template library (GAP-011) scope cần "30 initial templates" nhưng không phân loại K-12 / center / uni

## Proposed Fix

### Step 1: Segment picker trong Step 1 wizard

```tsx
<SegmentCards>
  {[
    { id: 'k12',     label: 'Trường K-12 (tiểu học / THCS / THPT)', icon: '🏫' },
    { id: 'center',  label: 'Trung tâm giáo dục', icon: '🎓' },
    { id: 'univ',    label: 'Đại học / Cao đẳng', icon: '🎓' },
    { id: 'corp',    label: 'Training nội bộ doanh nghiệp', icon: '🏢' },
    { id: 'other',   label: 'Khác' },
  ]}
</SegmentCards>
```

### Step 2: Preset profile per segment

Backend lưu `BrandingPreset` entity:

```java
@Entity
class BrandingPreset {
  String segment;            // K12 / CENTER / UNIV / CORP
  PaletteProfile palette;    // muted vs energetic vs formal
  TypographyProfile fonts;   // serif vs sans vs rounded
  List<String> preferredTemplateIds;  // subset of library
  List<String> bannedKeywords;        // inappropriate-for-K-12 filters
  String toneGuidance;                // AI prompt snippet
}
```

### Step 3: Routing

`ResourceRoutingService.classify()` (Wave 2 GAP-007) honors `preset.preferredTemplateIds` → bias TEMPLATE category; `PlannerService` (Wave 3 GAP-008) tiêm preset vào AI prompt.

### Step 4: Template library tagging

GAP-011 curation plan bổ sung field `segments: [k12, center, univ, corp]` cho mỗi template → filter theo preset.

## Acceptance Criteria

- [ ] 5 segment presets seeded (K-12, CENTER, UNIV, CORP, OTHER)
- [ ] Wizard Step 1 shows segment picker với previews
- [ ] `BrandingPreset` entity + CRUD + migration
- [ ] PlannerService injects preset into AI prompt construction
- [ ] Template library items tagged with segments
- [ ] 3-layer docs: `01-business/kiteclass/branding-presets/`
- [ ] E2E: K-12 signup → wizard → generates appropriate K-12 style
- [ ] Unit test: preset routing biases correctly

## Dependencies

- GAP-011 (template library) — must tag templates by segment
- GAP-013 (wizard UX) — add segment step
- GAP-008 (AI agent workflow) — Planner consumes preset
- GAP-031 (expand inputs) — preset is a "rich input"

## Target Wave

**Wave 3 Sub-PR 3.7** (wizard UX) — slot trong Step 1. Effort +1 day.

## Log

- 2026-04-14 — Detected via simulation-gap-finder on Wave 3 scope; industry-specific gap for VN market
