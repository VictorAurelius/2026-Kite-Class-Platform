# UI Kit — KiteClass Marketing Site (Beta-signup landing)

The **product marketing landing for the KiteClass platform itself** — distinct from the per-tenant landing templates (`landing-personal`, `landing-organization`). This is the page that invites **center owners (P2)** to join the Phase 1 **Beta**, and is the design target for production `kiteclass/kiteclass-frontend/src/app/(public)/page.tsx`.

Open `index.html` for the full interactive landing (cookie consent, persona segmented control, beta form success state).

**Đa-banner carousel prototype:** mở `carousel-demo.html` — hero carousel nhiều slide theo giáo viên (auto-rotate + dots + prev/next), wire `.hero-slide` CSS. Design artifact cho GAP-826; production per-tenant = Phase 1.5.

## Built to a brief + measured against a checklist (TDD-for-design loop)
Designed from two reference docs in the KiteClass repo and self-scored before delivery:
- `.claude/skills/quality/ui-review/reference/landing-page-design-brief.md` — generative direction (persona, value-prop, IA, tone).
- `.claude/skills/quality/ui-review/reference/landing-page-review-checklist.md` — 10-dimension /100 evaluative rubric.
- `.claude/rules/vn-localization-audit-checklist.md` — VN format / label / sample-data / culture rules.

### 4 hard gates (fail → block) — all satisfied
1. **Beta disclaimer present** — top strip, nav pill, dedicated beta note in Trust section, footer bar.
2. **WCAG AA contrast** — vibrant brand orange `#F97316` / blue `#3B82F6` kept for *decorative fills only*; text-bearing CTAs use deeper contrast-safe shades (orange-700 `#C2410C`, blue-700 `#1D4ED8`); hero gradient deepened (blue-800→violet-800) so white body text clears 4.5:1. Verified per text/CTA pair.
3. **Cookie consent (Nghị định 13/2023/NĐ-CP)** — dismissible banner with "Chỉ cookie cần thiết" / "Đồng ý tất cả", persisted to `localStorage`.
4. **No overclaim** — only Phase 1 features (điểm danh, học phí, điểm số, lớp/khóa, cổng phụ huynh, quản lý giáo viên); no fabricated testimonials, competitors, or numbers; beta status stated honestly.

## Information architecture (per brief §5)
Beta strip → Nav → **Hero** (headline + subhead + "Đăng ký Beta" + product mockup, beta badge) → **Problem→Solution** (3 pains: điểm danh thủ công / học phí sai / phụ huynh hỏi Zalo) → **Features** (6 benefit-framed cards) → **Cách hoạt động** (3 steps) → **Cổng phụ huynh** (trust highlight) → **Trust & an toàn dữ liệu** (NĐ 13 + beta note) → **Final CTA + beta form** → **Footer** (liên hệ, pháp lý, đơn vị phát hành, beta disclaimer) → **Cookie consent**.

## VN localization (per audit checklist)
- 100% tiếng Việt tự nhiên, xưng hô "anh/chị" (formal-respectful cho chủ trung tâm).
- VND format `1.500.000đ`; dates `01/06/2026` (dd/MM/yyyy); revenue `128.500.000đ`.
- VN sample data: `Trần Thị Hồng`, `Nguyễn Văn An`, `Lớp Anh ngữ 5A1`, `Trung tâm Anh ngữ Sky Education`, `0901 234 567`, `hong.tran@skyedu.vn` — no `John Doe`/`Class A1`.
- VN culture: Zalo notification example, `GVCN` terminology, mother-as-contact framing, evening class time (18:00).

## SEO (D9)
`<title>` + meta description tiếng Việt, Open Graph + Twitter card, JSON-LD `SoftwareApplication` + publisher Organization, `lang="vi"`, canonical, single `<h1>`, skip-link, semantic `<header>/<main>/<footer>`.

## Files (self-contained — no external `_shared/` dependency)
- `index.html` — SEO head + JSON-LD + composition.
- `marketing.jsx` — all components (nav, hero + `AppMock` dashboard, problem/solution, features, how-it-works, parent band, trust, beta form, footer, cookie consent).
- `product.css` — marketing-specific styles + the contrast-safe palette overrides.
- `colors_and_type.css`, `landing.css`, `primitives.jsx` — bundled copies of the shared design-system foundations (tokens, base components, `Icon` set), so this folder is portable and can be dropped into the repo / ported on its own without touching the existing `_shared/`.

## Self-score (indicative)
D1 value-prop 9 · D2 conversion 9 · D3 trust 8 · D4 feature 9 · D5 visual 9 · D6 responsive 8 · D7 a11y 9 · D9 SEO 9 · D10 VN/content 10 — **≈88/100**, all 4 hard gates ✓. *(D8 Core Web Vitals applies to the production Next build, not this HTML mock; this artifact is the visual/IA/content reference for that build.)*

> **Note:** this is a high-fidelity design artifact (React via CDN + Babel). For production, port the sections to Next.js with `next/image` + `next/font` and re-run Lighthouse/axe per the checklist before merge.
