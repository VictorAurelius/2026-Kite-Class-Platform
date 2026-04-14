# GAP-013: Guided Branding Wizard UX (closed-loop with flexibility)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / UX / Product
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md` §3

## Problem

**Triết lý:** KiteClass là SaaS giáo dục, KHÔNG phải AI creative platform. User target = "best possible branded instance" — KHÔNG phải "creative freedom".

Hiện tại chưa có thiết kế UX wizard. Nếu để free-form prompt → user không biết viết gì, output không predictable → bad UX. Nếu quá rập khuôn → user cảm thấy bó buộc.

Cần thiết kế wizard UX **guided nhưng flexible** — giống IKEA Kitchen Planner, Canva templates, Shopify theme customization.

## Design Principles

### 1. KHÔNG free-form prompts cho thường (99% users)
- User chọn từ options (presets), system compose prompt cố định
- Enterprise tier có thể opt-in "Advanced mode" với disclaimer

### 2. Preview trước commit
- FE render mock instance với generated assets
- User xem landing page, dashboard, login page preview
- Approve hoặc regenerate từng resource

### 3. Approve per resource, không all-or-nothing
- Logo: ✓
- Primary color: ✓ (user chỉnh manually nếu muốn)
- Banner: ✗ regenerate with different template
- Hero: ✓

### 4. Regenerate có limits (khuyến khích suy nghĩ trước khi regenerate)
- FREE: 3 regenerate/wizard session
- PRO: 10
- PREMIUM: 30
- ENTERPRISE: unlimited

## Wizard Flow (6 steps)

```
┌─────────────────────────────────────────────────────────┐
│ Step 1: Welcome + Info                                   │
│   "Chúng tôi sẽ tạo trang web cho trung tâm của bạn     │
│    trong 2 phút, theo phong cách giáo dục chuyên nghiệp"│
│   [Bắt đầu]                                              │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ Step 2: Upload Logo (optional)                           │
│   [Drag & drop] or [Skip — use text logo]                │
│   AI preview: extract primary color from logo            │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ Step 3: Chọn Đối Tượng Học Viên                          │
│   ○ Học sinh Tiểu học                                    │
│   ○ Học sinh THCS                                        │
│   ○ Học sinh THPT                                        │
│   ○ Sinh viên / Người đi làm                            │
│   ○ Trẻ em (3-6 tuổi)                                   │
│                                                          │
│ Step 4: Chọn Phong Cách                                  │
│   ○ Chuyên nghiệp   ○ Thân thiện                        │
│   ○ Năng động       ○ Sang trọng                        │
│                                                          │
│ Step 5: Chọn Template (6 previews)                       │
│   [Preview 1] [Preview 2] [Preview 3]                    │
│   [Preview 4] [Preview 5] [Preview 6]                    │
│   → System filter templates theo audience + tone chọn    │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ Step 6: Preview & Approve                                │
│                                                          │
│ [Live preview of instance — landing + dashboard]         │
│                                                          │
│ Generated Resources:                                     │
│ ┌─────────────────────────────────────────────────┐     │
│ │ 🎨 Colors                               [✓ OK]  │     │
│ │ [color swatches] [Customize]                    │     │
│ ├─────────────────────────────────────────────────┤     │
│ │ 🖼️ Logo                                 [✓ OK]  │     │
│ │ [logo preview] [Upload new]                     │     │
│ ├─────────────────────────────────────────────────┤     │
│ │ 🎭 Banner                          [🔄 Regen] │     │
│ │ [banner preview] [Regenerate] [Choose different]│     │
│ ├─────────────────────────────────────────────────┤     │
│ │ 📰 Hero                                 [✓ OK]  │     │
│ │ [hero preview]                                  │     │
│ └─────────────────────────────────────────────────┘     │
│                                                          │
│ Regenerate remaining: 2/3 (FREE tier)                    │
│                                                          │
│ [Deploy Instance] [Save draft]                           │
└─────────────────────────────────────────────────────────┘
```

## Advanced Mode (Enterprise tier only)

Optional "Advanced" tab trong wizard:
- Free-form prompt field (200 chars max)
- Disclaimer: "AI generation is unpredictable, may require multiple regenerates"
- Recommended prompts provided
- Fallback: nếu AI output fail quality gate (GAP-012), auto-fall back to template

## Post-Deploy Customization

Sau khi DEPLOYED, trong tenant dashboard:
- **Quick edits** (always available): primary color, logo replacement, tagline text
- **Regenerate banner** (uses AI quota)
- **Change template set** (full rebrand via wizard)

## UX Anti-Patterns to Avoid

| ❌ Don't | ✅ Do |
|---------|------|
| "Describe your banner in 50 words" | Choose from 6 template previews |
| Single "Generate" button → black box | Step-by-step with reasoning shown |
| All-or-nothing approve | Per-resource approve |
| Unlimited regenerates | Tier-based limits with counter visible |
| Hide preview until deploy | Show preview after each step |
| Mystery AI results | Explain why this template matches |

## Mobile Responsive

Wizard phải work tốt trên mobile (tenant chủ thường setup qua phone):
- Step-by-step với 1 decision per screen
- Swipe navigation
- Large touch targets (48px min)
- Preview có pinch-to-zoom

## Implementation

**Frontend:** Next.js wizard trong kitehub-frontend
- 6 step components
- Zustand store cho wizard state (persist to localStorage)
- Integration với kitehub-branding API
- Real-time preview iframe

**Backend:** No major changes — existing APIs sufficient với GAP-007, GAP-008

## Acceptance Criteria

- [ ] Wizard UI 6 steps designed (Figma)
- [ ] Implementation trong kitehub-frontend
- [ ] Preview iframe với mock instance
- [ ] Approve/regenerate per resource
- [ ] Regenerate counter per tier
- [ ] Advanced mode gated behind Enterprise tier
- [ ] Mobile responsive (tested on iPhone SE 375px)
- [ ] A/B test: wizard completion rate > 80%
- [ ] User testing: 5 tenants trials, feedback collected

## Dependencies

- **Requires GAP-011** (templates available)
- **Requires GAP-007** (resource classification)
- **Requires GAP-008** (agent workflow — provides regenerate capability)
- **Integrates with GAP-012** (show quality score in preview)

## References

- [Shopify Theme Editor](https://www.shopify.com/editions/summer2023/experts-marketplace) — per-section approve pattern
- [Canva Design Wizard](https://www.canva.com) — template-first approach
- [IKEA Kitchen Planner](https://kitchenplanner.ikea.com) — guided flexibility
- [Framer Templates](https://www.framer.com/templates/) — curated library model

## Log

- 2026-04-14 — Phát hiện cần wizard UX design sau user clarified "SaaS education, not AI platform"
