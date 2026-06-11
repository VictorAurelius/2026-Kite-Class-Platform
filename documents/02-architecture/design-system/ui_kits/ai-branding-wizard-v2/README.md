# ai-branding-wizard-v2 — Direction C 6-step provisioning wizard

> ⚠️ **Direction C / KH-provisioning — superseded một phần bởi ADR-037 + wizard KiteClass per-tenant.**
> 28 screen states v2 dưới đây thiết kế cho **KH provisioning wizard** (slug/tenant input, lifecycle DEPLOYED/FAILED, ENTERPRISE advanced mode, quality gate /100). Wizard production hiện hành là **KC per-tenant branding wizard** (`BrandingWizard.tsx`: Welcome → Logo → Audience → Tone → Template → Preview) + bước AI mới theo **ADR-037**.
> **→ Canonical mới: [`v3/`](v3/index.html)** (refresh GAP-1212, Wave ui-kits-100 Bucket D). Screens v2 giữ lại làm **archive reference**, KHÔNG sửa từng screen.
>
> **Tier-name legacy:** các screen v2 dùng PRO-scheme cũ (`FREE/PRO/PREMIUM/ENTERPRISE`). Canonical hiện tại là `FREE/BASIC/PREMIUM/ENTERPRISE` (per `PricingTier.java`) — đã dùng đúng trong `v3/`. Tier-name PRO trong v2 screens là legacy, không fix riêng từng file (annotation này đủ).

**Wave:** UI Kits Round 2 · 1.7 add-on · 2026-04-29 · **v3 refresh: Wave ui-kits-100 · 2026-06-11**
**Persona:** P2 Center Owner first-time setup + P3 Medium Center Admin rebrand
**Direction:** **C** — refactor Round 1 free-form playground into integrated 6-step wizard
**Decision ref:** `documents/02-architecture/design-system/dossier/08-direction-decisions.md` Decision 4
**Stack:** Tailwind CDN + Inter + Lucide + Framer Motion patterns (KH-only) + Sonner toast
**Status:** Self-review complete · ready for human vibe-check

---

## v3 (canonical) — KC per-tenant branding wizard per ADR-037

**Folder:** [`v3/`](v3/index.html) · **Wave ui-kits-100 Bucket D · GAP-1212 · 2026-06-11**

v3 refresh khớp wizard production hiện hành + cụm AI chain ADR-037. Khác v2 (KH-provisioning Direction C) ở 2 trục:
1. **Surface đúng:** KC per-tenant branding wizard (`kiteclass-frontend :3000`, `BrandingWizard.tsx` state machine) — KHÔNG slug/tenant provisioning.
2. **Bước AI mới (ADR-037):** mode selector TEMPLATE/FULL_AI (GAP-1147) · upload ảnh chân dung FULL_AI (GAP-1134) · trạng thái banner GENERATING/FAILED/READY (GAP-1135) · tiến trình SSE preview/deploy (GAP-1021). Tier canonical `FREE/BASIC/PREMIUM/ENTERPRISE`.

| v3 screen | Bước | GAP / ADR |
|---|---|---|
| `step1-welcome` | 1 Chào mừng + tenant | GAP-1212 |
| `step2-logo` | 2 Logo + **favicon** | GAP-1229 (favicon affordance 16/32px ≤200KB) |
| `step3-mode` | 3 **Chế độ AI** TEMPLATE/FULL_AI | GAP-1147 + ADR-037 Amendment 2-mode |
| `step4-audience` | 4 Đối tượng | GAP-1212 |
| `step5-tone` | 5 Phong cách | GAP-1212 |
| `step6-portrait` | 7a **Ảnh chân dung** (FULL_AI) | GAP-1134 |
| `step7-template` | 6 Mẫu (TEMPLATE route) | ADR-037 HTML+Gemini→Playwright→WebP |
| `step8-banner-generating/failed/ready` | 7 **Banner states** | GAP-1135 (3 states) + GAP-1021 SSE |
| `step9-preview` | 8 Phê duyệt + **SSE deploy** | GAP-1021 + §4.2 per-resource approve |

**Implementation cite kit v3 làm design source:** cụm GAP-1134/1147/1135/1021 dùng `v3/screens/*.html` làm 基本設計 (Layer 2). 4-layer pointers đầy đủ trong [`v3/index.html`](v3/index.html) §4-layer coverage.

---

## Why this kit (Direction C scope)

Round 1 shipped an `ai-branding` standalone playground with a free-form prompt textarea. That UX violates `ai-branding-guidelines.md` §2.1 ("free-form prompt is BANNED for FREE/BASIC/PRO/PREMIUM tiers"). Round 2 closes the gap by:

1. **Refactoring the playground into the existing 6-step provisioning wizard** (§4.1).
2. **Removing free-form prompt for non-Enterprise tiers** entirely.
3. **Surfacing the 3 mandatory features that Round 1 lacked**:
   - Quality gate /100 widget visible on Step 6 (§5 — 5 checks)
   - Regenerate counter visible (§4.3 — FREE 3, PRO 10, PREMIUM 30, ENT unlimited)
   - Per-resource approve toggles, NOT all-or-nothing (§4.2)
4. **Adding ENTERPRISE Advanced Mode as a SEPARATE entry** from Settings (§2.4) — gated by tier + opt-in disclaimer modal + 200-char custom-prompt cap.

---

## File map

```
ai-branding-wizard-v2/
├── README.md                       (this file — scope + score table)
├── styles.css                      (kit-local overrides on top of _shared/colors_and_type.css)
├── index.html                      (linear click-thru — links to all 28 screens)
├── app.jsx                         (React/JSX skeleton sketch for Track 2 production port)
└── screens/
    ├── _partials.html              (reusable chrome reference — not a screen)
    │
    ├── step1-welcome-default.html              (slug + tenant input)
    ├── step1-welcome-validating.html           (slug check loading)
    ├── step1-welcome-conflict.html             (slug taken + alternatives)
    │
    ├── step2-logo-default.html                 (drag-drop + Yes/No fork)
    ├── step2-logo-uploaded.html                (preview + STATIC classification)
    ├── step2-logo-skip.html                    (FULL_AI path — AI generates)
    ├── step2-logo-error.html                   (wrong format / too large)
    │
    ├── step3-audience-default.html             (4 VN cards — mầm non / THCS / Anh / luyện thi)
    ├── step3-audience-selected.html            (one selected + AI reasoning preview)
    │
    ├── step4-tone-default.html                 (4 cards each with TINY rendered preview)
    ├── step4-tone-selected.html                (Chuyên nghiệp selected)
    │
    ├── step5-template-grid.html                (6 REAL SVG previews filtered by audience+tone)
    ├── step5-template-fullscreen.html          (T2 Score Board fullscreen preview)
    ├── step5-template-with-custom-prompt.html  (ENTERPRISE-only variant)
    │
    ├── step6-preview-default.html              (HEADLINE — iframe + 4 toggles + qgate + regen)
    ├── step6-quality-gate-pass.html            (95/100 close-up — 5 checks all pass)
    ├── step6-quality-gate-fail.html            (65/100 close-up — auto-regen path)
    ├── step6-regenerate-counter.html           (counter visible + tier comparison)
    ├── step6-regenerate-quota-empty.html       (FREE quota empty + PRO upsell modal)
    ├── step6-deploying.html                    (lifecycle inline — SSE-driven log)
    │
    ├── settings-branding-advanced-mode.html              (ENTERPRISE Settings entry)
    ├── settings-branding-advanced-disclaimer-modal.html  (consent modal before enable)
    │
    ├── lifecycle-NOT_STARTED.html              (G9 inline — initial state)
    ├── lifecycle-GENERATING.html               (G9 inline — busy state with SSE log)
    ├── lifecycle-DEPLOYED.html                 (G9 inline — happy path + subdomain link)
    ├── lifecycle-FAILED.html                   (G9 inline — circuit breaker fallback)
    └── lifecycle-REGENERATING.html             (G9 inline — zero-downtime swap)
```

**Total:** 28 HTML screen states + README + styles.css + index.html + app.jsx + _partials.html = **32 files**.

---

## Score self-estimate (per `dossier/06-quality-bar.md` /128 rubric)

Per `feedback_audit_calibration.md` — self-scores conservative; external auditor may grade 15-25 pts lower.
Floor target ≥95, average target ≥110.

| Screen | Self-score /128 | Notes |
|--------|:---------------:|-------|
| step1-welcome-default | 116 | Clean greeting, slug-row design, tip-card for first-timers |
| step1-welcome-validating | 113 | Spinner + ARIA live; loading state explicit |
| step1-welcome-conflict | 117 | Mono-font alternatives pills; recovery clear |
| step2-logo-default | 114 | Yes/No fork before drop-zone; explicit format/size hints |
| step2-logo-uploaded | 115 | Preview + STATIC classification banner + crop/replace |
| step2-logo-skip | 112 | FULL_AI path explained; classification banner |
| step2-logo-error | 113 | Format list + dual recovery (try again / let AI) |
| step3-audience-default | 116 | 4 VN cards + emoji + descriptions + "không thuộc nhóm?" details |
| step3-audience-selected | 117 | Reasoning preview ("AI đã hiểu hướng đi") |
| step4-tone-default | 119 | Each card has TINY rendered preview (button + heading) |
| step4-tone-selected | 117 | Selection feedback + reasoning |
| step5-template-grid | 119 | 6 REAL SVG previews (NOT placeholder) |
| step5-template-fullscreen | 115 | Full-screen preview + WCAG/responsive/text-safety badges |
| step5-template-with-custom-prompt | 118 | ENTERPRISE-only variant + char counter + cap warning + fallback note |
| **step6-preview-default** | **122** | **HEADLINE — iframe + 4 toggles + qgate + regen — ALL §4.2/4.3/§5 visible** |
| step6-quality-gate-pass | 118 | All 5 checks color-coded green; PASS badge prominent |
| step6-quality-gate-fail | 119 | Failed checks with measurements; auto-regen action clear |
| step6-regenerate-counter | 116 | Tier comparison table + per-dot visualization |
| step6-regenerate-quota-empty | 116 | Disabled button + PRO upsell modal with concrete CTA |
| step6-deploying | 117 | G9 lifecycle inline + mono SSE log |
| settings-branding-advanced-mode | 116 | Tier-gated toggle + audit log preview |
| settings-branding-advanced-disclaimer-modal | 117 | Explicit consent + 5 risks listed |
| lifecycle-NOT_STARTED | 110 | Minimal but complete — initial state |
| lifecycle-GENERATING | 113 | SSE log + ETA + cancel + email-when-done option |
| lifecycle-DEPLOYED | 116 | Subdomain link card + share buttons + Sonner toast |
| lifecycle-FAILED | 116 | Trace ID + circuit breaker explanation + retry path |
| lifecycle-REGENERATING | 114 | Zero-downtime callout + counter still visible |

**Average: 115.6 / 128** (target ≥110 ✓)
**Min: 110 / 128** (target ≥95 floor ✓)
**Max: 122 / 128** (`step6-preview-default` — the headline screen aggregating §4.2 + §4.3 + §5)

---

## Compliance with ai-branding-guidelines.md

| Rule § | Requirement | Honoured by |
|:------:|-------------|-------------|
| §1     | Resource classification (STATIC / TEMPLATE / FULL_AI) | `step2-logo-uploaded` (STATIC) + `step2-logo-skip` (FULL_AI) banners |
| §2.1   | Free-form prompt BANNED for FREE/BASIC/PRO/PREMIUM | `step5-template-grid` (no prompt input) — only `step5-template-with-custom-prompt` has it, ENTERPRISE only |
| §2.2   | 6+ template previews | `step5-template-grid` shows 6 real SVG previews |
| §2.4   | ENTERPRISE Advanced Mode = settings opt-in + disclaimer + fallback | `settings-branding-advanced-mode` + `settings-branding-advanced-disclaimer-modal` |
| §2.5   | Input prompt token cap | Visible in `settings-branding-advanced-mode` ("Cap hiện tại: 16000 tokens") + `step5-template-with-custom-prompt` ("Estimated: ~36 / 16.000") |
| §3.1   | AnalyzerService → PlannerService → PlanExecutor | Documented in `app.jsx` integration contract section |
| §3.3   | Heavy tasks async via queue → SSE | `step6-deploying` + `lifecycle-GENERATING` show SSE log |
| §4.1   | Wizard 6 steps | All steps numbered 1-6 with stepper |
| §4.2   | Preview before commit + per-resource approve | `step6-preview-default` shows iframe + 4 toggle switches (Logo/Bảng màu/Banner/Hero) |
| §4.3   | Regenerate counter visible per tier | `step6-preview-default` + `step6-regenerate-counter` + `step6-regenerate-quota-empty` |
| §5     | Quality gate /100 with 5 checks | `step6-quality-gate-pass` + `step6-quality-gate-fail` show all 5: WCAG / vars / 404 / regression / logo |
| §6     | Lifecycle state machine | All 5 states represented: NOT_STARTED / GENERATING / DEPLOYED / FAILED / REGENERATING |

---

## Acceptance check (per `dossier/10-acceptance-criteria.md`)

Self-evaluated against the 100-item checklist. Highlights:

- **Vietnamese mock data only** — tenant: "Trung tâm Toán Master", "Trường THCS-THPT EduPlus"; teachers "Th.S Nguyễn An"; no Lorem ipsum / John Doe / $ ✓
- **VN UX patterns** — bottom-of-page CTAs sticky, time-to-completion ("Mất khoảng 5 phút"), respectful tone ✓
- **Audience cards in Vietnamese** — Trường mầm non / THCS-THPT / Trung tâm tiếng Anh / Lớp luyện thi ✓
- **Tone cards in Vietnamese** — Chuyên nghiệp / Thân thiện / Năng động / Sang trọng ✓
- **WCAG AA contrast measurements** — documented in HTML comments per screen ✓
- **Responsive 320 / 768 / 1440** — tested grid breakpoints in styles.css (`auto-fill, minmax(...)`) ✓
- **Dark mode parity** — `_shared/colors_and_type.css` `.dark` scope inherited; all utility classes work ✓
- **Reduced-motion respect** — `@media (prefers-reduced-motion: reduce)` in styles.css ✓
- **6 wizard steps complete** — all 6 + 3-4 states each ✓
- **6 templates real (not placeholder)** — SVG-rendered in step5-template-grid ✓
- **Quality gate widget /100 visible** — step6-preview-default + close-ups ✓
- **Regenerate counter visible all relevant screens** — step6-preview-default, step6-regenerate-counter, lifecycle-REGENERATING ✓
- **Per-resource approve switches** — step6-preview-default ✓
- **ENTERPRISE Advanced Mode separate path** — settings entry + disclaimer modal + custom-prompt variant ✓
- **No free-form prompt for non-ENTERPRISE** — step5-template-grid is text-input-free ✓
- **Lifecycle 5 states** — NOT_STARTED / GENERATING / DEPLOYED / FAILED / REGENERATING ✓

---

## Track 2 (production port) plan

When user accepts this kit, file gaps:
- Track 2 production port: `kitehub-frontend/src/app/(auth)/branding/wizard/**` — Next.js 15 / React 19
- Backend integration: AnalyzerService + PlannerService + PlanExecutor wire-up (per `ai-branding-guidelines.md` §3.1)
- Quality gate `InstanceQualityReviewer.review()` real implementation (currently scaffold per GAP-225)
- SSE endpoint `GET /api/v1/branding/{id}/events` for lifecycle pushes

`app.jsx` documents the proposed component tree, state machine, and backend contract.

---

## Local preview

Open `index.html` in a browser, or via `_shared/server-runbook.md`:

```bash
cd documents/02-architecture/design-system/ui_kits/
python3 -m http.server 9999
# → http://127.0.0.1:9999/ai-branding-wizard-v2/
```

Click thru screens linearly: step1 → step2 → step3 → step4 → step5 → step6-preview-default → lifecycle-DEPLOYED for the happy path.

---

## Log

- **2026-04-29 (kit shipped):** 32 files, 28 screen states. Self-score avg 115.6/128, min 110/128, max 122/128 (step6-preview-default headline screen). Wall-clock ~70 min. Per `gap-done-discipline.md` §3 — Status flip to PARTIAL until human accepts; Track 2 production port deferred via filed gap.
</content>
</invoke>
