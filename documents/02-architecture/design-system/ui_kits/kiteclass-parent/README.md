# kiteclass-parent — Mobile-first PWA-grade UI kit

**Persona:** Pa. Parent (mobile-first 320–414px, evening usage 7pm–10pm, low-medium tech literacy)
**Direction:** D — pivoted to **web responsive + PWA-grade**, NOT native app (per [`dossier/08-direction-decisions.md §2`](../../dossier/08-direction-decisions.md))
**Wave:** Round 2 UI Kits, Agent B
**Status:** Prototype — for human vibe-check review (Track 2 production port deferred to GAP-264..267)"
".## Tenant-theme demo (GAP-1230)

Switcher nổi góc phải-dưới **"Chủ đề theo giáo viên"** demo 3 GV demo-trio per-tenant — đổi màu toàn kit runtime THẬT (set class `kc-demo-{ha|nhi|khanh}` trên `<html>`+`<body>` → override token `--primary`/`--accent`/`--ring`):

- **Cô Hà · Toán** — xanh dương `#2563EB`
- **Thầy Nhì · Hóa** — xanh lá `#16A34A`
- **Cô Khánh · Anh** — cam `#EA580C`

Nguồn dùng chung: `_shared/scripts/tenant-theme-demo.{css,js}` (port pattern từ `kiteclass-public/about.html` + `landing-personal`). Wire vào mọi screen + `index.html`. Affordance click có hiệu ứng runtime thật, không inert (per `design-source-implementation-parity.md` §3.2).

**Production:** theme thật đến từ `branding` package per ADR-009 (build-time per-tenant) — switcher này CHỈ là demo affordance trong design kit.."

**Last Updated:**" 2026-04-29

---

## What's in this kit

5 routes × 3-5 states + 3 PWA / notification specs = **17 HTML screens + manifest.json + sw.js + app.jsx**.

| Route | Default | Loading | Empty | Error | Dark | Other |
|-------|:-------:|:-------:|:-----:|:-----:|:----:|-------|
| **Trang chủ** (`home`) | ✅ | ✅ | ✅ (no child linked) | ✅ (offline) | ✅ | — |
| **Học bạ con** (`grades`) | ✅ overview | — | ✅ | — | — | ✅ subject detail |
| **Điểm danh** (`attendance`) | ✅ calendar | — | — | — | — | ✅ day detail |
| **Học phí** (`billing`) | ✅ list | — | — | — | — | ✅ pay + ✅ success |
| **Cài đặt** (`settings`) | ✅ | — | — | — | — | — |
| **Spec — Zalo OA card** | — | — | — | — | — | ✅ 4 variants |
| **Spec — PWA install** | — | — | — | — | — | ✅ |
| **Spec — Web Push permission** | — | — | — | — | — | ✅ |

---

## Self-scoring (`/128`)

Per [`dossier/06-quality-bar.md`](../../dossier/06-quality-bar.md) `/128` rubric. Target ≥105/128 per kit avg, no screen <95.

| # | Screen | Tech | Heuristic | Aesthetic | UX | Total `/128` |
|:-:|--------|:----:|:---------:|:---------:|:--:|:------------:|
| 1 | `home-default.html` | 30 | 28 | 30 | 28 | **116** |
| 2 | `home-loading.html` | 30 | 26 | 28 | 26 | **110** |
| 3 | `home-empty.html` | 28 | 28 | 28 | 28 | **112** |
| 4 | `home-error.html` | 28 | 28 | 28 | 26 | **110** |
| 5 | `home-dark.html` | 30 | 28 | 30 | 26 | **114** |
| 6 | `grades-overview.html` | 30 | 30 | 30 | 28 | **118** |
| 7 | `grades-subject-detail.html` | 28 | 28 | 30 | 27 | **113** |
| 8 | `grades-empty.html` | 26 | 28 | 28 | 26 | **108** |
| 9 | `attendance-calendar.html` | 30 | 28 | 30 | 28 | **116** |
| 10 | `attendance-day.html` | 28 | 28 | 28 | 26 | **110** |
| 11 | `billing-list.html` | 30 | 30 | 30 | 27 | **117** |
| 12 | `billing-pay.html` | 30 | 30 | 30 | 29 | **119** |
| 13 | `billing-success.html` | 28 | 28 | 30 | 27 | **113** |
| 14 | `settings.html` | 30 | 30 | 28 | 29 | **117** |
| 15 | `push-notification-card.html` | 30 | 30 | 31 | 30 | **121** |
| 16 | `pwa-install-prompt.html` | 30 | 28 | 30 | 27 | **115** |
| 17 | `web-push-permission.html` | 30 | 30 | 28 | 28 | **116** |
| | **Average** | **29.2** | **28.6** | **29.2** | **27.4** | **114.4 / 128** |

**Average: 114/128** · Target ≥105 ✅ · Min screen 108 ≥95 ✅

---

## Direction D pivot — what's IN scope

Per [`dossier/08-direction-decisions.md §2`](../../dossier/08-direction-decisions.md):

- ✅ **Mobile-first 320–414px** primary breakpoint
- ✅ **Graceful upscale** to 768px (centered max-w 480) and 1440px (shadow-card centered)
- ✅ **Bottom tab nav** 4 tabs (Trang chủ / Học bạ / Học phí / Cài đặt) — touch ≥44×44
- ✅ **Touch targets ≥ 44×44px** every interactive element (CSS rule in `styles.css`)
- ✅ **PWA manifest** (`manifest.json` — name, short_name, icons 192/512, theme_color, display:standalone, shortcuts, screenshots)
- ✅ **Service Worker** (`sw.js` — install/activate/fetch/push/notificationclick handlers)
- ✅ **Pull-to-refresh hint** (CSS class `.ptr-hint` + data-attribute trigger; full impl in Track 2)
- ✅ **Zalo OA primary push** — 4 card variants in `push-notification-card.html`, settings prioritizes Zalo OA toggle
- ✅ **Web Push fallback** — `web-push-permission.html` shows soft-ask pattern + Zalo OA fallback button
- ✅ **VN-first copy** ("anh/chị", "Con [tên]", "199.000đ")
- ✅ **`prefers-reduced-motion`** respected (CSS guard)
- ✅ **`viewport-fit=cover`** + `env(safe-area-inset-*)` for iOS Dynamic Island

## Direction D pivot — what's OUT of scope

Per same §2:
- ❌ React Native / Flutter (defer post-PMF)
- ❌ Native iOS/Android shells
- ❌ App Store / Play Store presence
- ❌ Native MoMo/VNPay SDK (web flow + redirect is enough for VN)

---

## VN UX musts hit

Per [`dossier/02-vietnamese-ux-musts.md`](../../dossier/02-vietnamese-ux-musts.md):

| Section | Hit |
|---------|:---:|
| Currency `199.000đ` lowercase đ | ✅ billing-list, billing-pay, billing-success |
| Date `dd/MM/yyyy` | ✅ all dates |
| Time `HH:mm` 24-hour | ✅ attendance, settings |
| Phone `0901 234 567` 4-3-3 grouping | ✅ settings, web-push-permission |
| Name order surname-first | ✅ "Lê Minh Tuấn", "Trần Thị Hương" |
| Grade scale 0–10 (not A-F) | ✅ grades-overview, grades-subject-detail |
| Honor classification (Xuất sắc / Giỏi / Khá / TB / Yếu) | ✅ grades-overview, grade-pill CSS |
| Class naming `Lớp 10A2` | ✅ all references |
| Academic year `2025-2026` | ✅ home, billing |
| Semester `Học kỳ I/II` Roman | ✅ grades-overview tabs |
| Attendance codes P/V/M/L | ✅ attendance-calendar legend |
| GVCN concept | ✅ grades-overview comment, attendance-day |
| Payment 5 VN methods (MoMo/VNPay/ZaloPay/Bank/Cash) | ✅ billing-pay |
| Zalo OA primary push channel | ✅ push-notification-card, settings, web-push-permission |
| Trust markers (PDPL 13/2023, "Bảo mật bởi VNPay") | ✅ billing-pay, settings |
| Address `bạn` (informal) | ✅ all body copy |

---

## Quality bar — WCAG AA + dark mode + mock data

| Bar | Status |
|-----|:------:|
| WCAG AA contrast ≥ 4.5:1 (body) ≥ 3:1 (large) | ✅ measured per screen, documented in HTML comment |
| Touch targets ≥44×44 | ✅ CSS rule `button, a { min-height: 44px; min-width: 44px }` |
| Keyboard navigation reachable | ✅ all interactive elements use semantic tags |
| Focus indicator visible | ✅ inherits browser default (ring-2 in production port) |
| `prefers-reduced-motion` respected | ✅ CSS guard at end of `styles.css` |
| Color-not-only (status by icon + text) | ✅ attendance-calendar legend, payment methods |
| Dark mode parity | ✅ `home-dark.html` + `.dark` class CSS overrides |
| 320 / 768 / 1440 viewports | ✅ `.app-shell` max-w 480 + media queries |
| Mock data 100% Vietnamese | ✅ no Lorem ipsum, no John Doe, no $ |
| Image dimensions specified | ✅ all SVGs have width/height (CLS protection) |

---

## File structure

```
kiteclass-parent/
├── README.md                # this file
├── styles.css               # imports _shared/colors_and_type.css; kit-specific
├── index.html               # click-thru viewer with iPhone 14 frame
├── app.jsx                  # React-flavoured component prototype (for Track 2 port)
├── manifest.json            # PWA manifest
├── sw.js                    # Service Worker spec (cache + push + sync stubs)
└── screens/                 # 17 per-screen HTML files
    ├── home-default.html
    ├── home-loading.html
    ├── home-empty.html
    ├── home-error.html
    ├── home-dark.html
    ├── grades-overview.html
    ├── grades-subject-detail.html
    ├── grades-empty.html
    ├── attendance-calendar.html
    ├── attendance-day.html
    ├── billing-list.html
    ├── billing-pay.html
    ├── billing-success.html
    ├── settings.html
    ├── push-notification-card.html  # Zalo OA card spec (320×100, 4 variants)
    ├── pwa-install-prompt.html      # Add to Home Screen overlay
    └── web-push-permission.html     # Browser permission soft-ask + Zalo OA fallback
```

---

## How to preview

Foundation PR ships HTTP server on `127.0.0.1:9999`. With it running:

- **Click-thru viewer:** http://127.0.0.1:9999/documents/02-architecture/design-system/ui_kits/kiteclass-parent/
- **Single screen:** http://127.0.0.1:9999/documents/02-architecture/design-system/ui_kits/kiteclass-parent/screens/home-default.html
- **Mobile preview:** open Chrome DevTools → Device toolbar → set 390×844 (iPhone 14)
- **Dark mode:** open `screens/home-dark.html` directly OR add `<html class="dark">` in DevTools

Without HTTP server: `open documents/02-architecture/design-system/ui_kits/kiteclass-parent/index.html`

---

## Production port plan (Track 2 — deferred)

| GAP | Scope | Estimated effort |
|-----|-------|------------------|
| GAP-264 | Port routes to `kiteclass-frontend/src/app/parent/` (5 routes) | ~1 wave |
| GAP-265 | Wire to real `/api/v1/parent/*` endpoints with React Query + Zod | ~0.5 wave |
| GAP-266 | Real Service Worker with Workbox + VAPID + Zalo OA backend integration | ~1 wave |
| GAP-267 | E2E tests (Playwright mobile viewport) + WCAG AA automation | ~0.5 wave |

File only AFTER user accepts Round 2 quality.

---

## Acceptance criteria (per dossier 10 100-item checklist)

| Section | Pass | Total | % |
|---------|:----:|:----:|:-:|
| Locale primitives (currency, date, phone, name) | 10 | 10 | 100% |
| Identity docs (CCCD/CMND mask) | N/A (parent kit doesn't collect ID) | — | — |
| Education-specific (grade scale, honor, class name) | 8 | 8 | 100% |
| Payment gateways (5 VN methods) | 5 | 5 | 100% |
| Communication (Zalo OA primary, Web Push fallback) | 6 | 6 | 100% |
| Trust markers (PDPL, free trial reassurance) | 3 | 3 | 100% |
| WCAG AA (contrast, touch, keyboard, dark) | 8 | 10 | 80% — focus indicator + reduced-motion documented but not stress-tested |
| Mock data quality (no Lorem, no John, no $) | 10 | 10 | 100% |
| Density discipline (sparse for parent persona) | 4 | 4 | 100% |
| Persona alignment (1 screen = 1 task) | 4 | 4 | 100% |
| **Total** | **58** | **60** | **97%** |

---

## Known gaps / nice-to-haves (Track 2 backlog)

- Pull-to-refresh JS hookup (CSS hint only in this kit)
- Real PNG icons for `manifest.json` (referenced from `_shared/assets/` placeholder paths)
- Real Zalo OA backend integration (status-toggle is FE-only state)
- Real Web Push subscription with VAPID keys
- Real `prefers-color-scheme: dark` auto-toggle (currently explicit `<html class="dark">`)
- Per-environment manifest (dev/staging/prod) `start_url`
- Service Worker offline write queue (background sync placeholder only)

---

## Sign-off

Per `output-review-mandate.md` §3 row "HTML/JSX prototypes" (v1.2.0 added 2026-04-29):
- [x] Per-screen `/128` rubric documented in HTML comment
- [x] WCAG AA contrast ratios measured per screen
- [x] Mock data 100% Vietnamese
- [x] Dark mode parity (`home-dark.html` + `.dark` overrides)
- [x] 3 viewports (320/768/1440) confirmed in `styles.css` media queries
- [x] PWA artifacts (manifest.json + sw.js) included
- [x] Acceptance per dossier 10 documented (97%)
- [ ] User vibe-check (post-merge) — **pending**
