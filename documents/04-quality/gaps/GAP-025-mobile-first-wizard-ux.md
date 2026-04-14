# GAP-025: Mobile-First Wizard UX

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / UX
**Detected:** 2026-04-14 (simulation)

## Problem

Current wizard design (GAP-013) mentions "mobile responsive" briefly, nhưng **không mobile-first**:

- ❌ Wizard layout ưu tiên desktop (multi-column)
- ❌ Logo upload: không support camera capture (mobile user chụp logo trực tiếp)
- ❌ Template picker: grid 6 previews khó trên mobile narrow
- ❌ Preview iframe scaled kém trên mobile
- ❌ Gestures (swipe, pinch-zoom) không tối ưu

Thực tế: tenant owner thường **setup qua mobile** lần đầu (on the go, quick setup).

## Proposed Fix

### 1. Mobile-First Responsive Design

```tsx
// Mobile: 1 column, big cards
// Tablet: 2 columns
// Desktop: 3 columns
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  {templates.map(...)}
</div>
```

- Minimum touch target: 48×48px
- Large buttons, readable fonts (≥16px body)
- Step progress bar visible (1/6, 2/6...)

### 2. Camera Capture for Logo

```tsx
<input
  type="file"
  accept="image/*"
  capture="environment"  // Back camera on mobile
  onChange={handleLogoUpload}
/>
```

Fallback desktop: file picker.

### 3. Touch-optimized Template Picker

- Horizontal swipe carousel (not grid)
- Tap to select, tap again to preview full-screen
- Large selection checkmark

```tsx
<SwipeableTemplates
  templates={filtered}
  onSelect={selectTemplate}
  currentIndex={currentTemplate}
/>
```

### 4. Mobile Preview

- Bottom sheet preview (slide up)
- Device toggle (preview as mobile/tablet/desktop)
- Pinch-to-zoom support

### 5. Gesture Shortcuts

- Swipe left/right: next/previous step
- Swipe down on preview: close
- Pull-to-refresh: regenerate current resource

### 6. Offline Resilience

- Service Worker cache templates + preview assets
- Queue actions khi offline, sync khi online
- Show "offline mode" banner

## Wireframes (Mobile)

```
┌─────────────────┐
│ ← Step 3/6      │  Header with back + progress
├─────────────────┤
│                 │
│ Chọn đối tượng  │  Big heading
│                 │
│ ┌─────────────┐ │
│ │ ◉ Tiểu học  │ │  Large radio cards
│ └─────────────┘ │
│ ┌─────────────┐ │
│ │ ○ THCS      │ │
│ └─────────────┘ │
│ ...             │
│                 │
│ ┌─────────────┐ │
│ │  Tiếp theo  │ │  Fixed bottom button
│ └─────────────┘ │
└─────────────────┘
```

## Acceptance Criteria

- [ ] Wizard tested trên iPhone SE (375px) + Android budget phone (360px)
- [ ] Camera capture cho logo upload
- [ ] Swipe carousel cho template picker
- [ ] Bottom sheet preview với device toggle
- [ ] All touch targets ≥ 48px
- [ ] Service Worker cache templates
- [ ] A/B test: desktop vs mobile completion rate
- [ ] User testing: 5 mobile users complete wizard successfully

## Dependencies

- GAP-013 (wizard UX) — mobile-first refinement

## Log

- 2026-04-14 — Mobile setup scenario identified
