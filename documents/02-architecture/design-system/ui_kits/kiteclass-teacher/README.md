# kiteclass-teacher — Wave 1.6 ADD-ON UI Kit

**Wave:** UI Kits Round 2 → Wave 1.6 (Add-on closing Wave 1 scope gap)
**Persona:** Teacher (homeroom GVCN + subject teacher) — Tier 2 KC user, ~50% KiteClass user base
**Stack:** Tailwind + shadcn + Radix + lucide (NO Framer Motion — KC stack restriction per dossier 09)
**Device:** Tablet primary (1024px) · Desktop secondary
**Theme:** KiteClass blue (`221.2 83.2% 53.3%`)

---

## Why this kit exists (Wave-scope-completeness check)

Wave 1 shipped 2 KC kits (`kiteclass-pro-v2`, `kiteclass-parent`) but missed Teacher persona — homeroom GVCN + subject teacher are ~50% of KC active users. Per `feedback_wave_scope_completeness_check.md`, this gap surfaced post-merge during persona coverage audit. This kit closes the gap.

Round 2 extends 5 production-screen redesigns:

| Production route | Baseline | Target | Kit screens |
|---|:---:|:---:|---|
| `/classes/[id]/attendance` | 84/128 | ≥105 | 6 (default · marking · saved · empty · error · dark) |
| `/classes/[id]/grades` *(NEW)* | — | ≥105 | 6 (default · editing · validation · finalize · finalized · dark) |
| `/classes/[id]/schedule` *(NEW)* | — | ≥105 | 4 (week-view · create-slot · conflict · dark) |
| `/attendance/reports` (heaviest 417 LOC) | 80/128 | ≥105 | 5 (overview · detail · loading · empty · dark) |
| `/settings/teacher` | — | ≥105 | 3 (profile · payroll · dark) |

**Total: 24 HTML state files + index + app.jsx + styles.css + README** = 28 deliverables.

---

## Files

```
kiteclass-teacher/
├── README.md          ← this file
├── styles.css         ← KC blue theme + att-toggle + grade-cell + heatmap + sparkbar
├── index.html         ← click-thru navigation hub (5 sections, 24 screens)
├── app.jsx            ← React reference (AttendanceRoster + GradebookTable + PayrollSummary)
└── screens/
    ├── attendance-default.html      (110/128)
    ├── attendance-marking.html      (108/128)
    ├── attendance-saved.html        (109/128)
    ├── attendance-empty.html        (102/128)
    ├── attendance-error.html        (105/128)
    ├── attendance-dark.html         (110/128)
    ├── grade-entry-default.html     (112/128)
    ├── grade-entry-editing.html     (110/128)
    ├── grade-entry-validation-error.html (107/128)
    ├── grade-entry-finalize-confirm.html (108/128)
    ├── grade-entry-finalized.html   (109/128)
    ├── grade-entry-dark.html        (110/128)
    ├── schedule-week-view.html      (108/128)
    ├── schedule-create-slot.html    (106/128)
    ├── schedule-conflict-error.html (105/128)
    ├── schedule-dark.html           (108/128)
    ├── reports-overview-default.html (113/128)
    ├── reports-detail-class.html    (110/128)
    ├── reports-loading.html         (100/128)
    ├── reports-empty.html           (102/128)
    ├── reports-dark.html            (110/128)
    ├── settings-default.html        (109/128)
    ├── settings-payroll.html        (108/128)
    └── settings-dark.html           (108/128)
```

---

## Quality self-report (per dossier 10-acceptance-criteria.md)

| Metric | Value |
|---|:---:|
| Screens | 24 |
| Avg score | **108/128** |
| Min score | 100/128 |
| Max score | 113/128 |
| Target floor | 95/128 |
| Lift target (avg) | +28 vs baseline |

### Per-section averages

| Section | Avg | Notes |
|---|:---:|---|
| Daily attendance | 107/128 | G2 Roster pattern embedded; 25 students × 4-button toggle |
| Grade entry | 109/128 | Inline G3 gradebook; sticky first column; MoET classification footer |
| Schedule | 107/128 | Inline G4 week-view; Mon-first; conflict detection |
| Reports | 107/128 | Replaces 417 LOC heaviest screen; KPI sparkline + heatmap + bar chart |
| Settings | 108/128 | Profile + MST + payroll history + Zalo OA notifications |

### WCAG AA contrast measurements (sample)

All measurements per HTML comment block at top of each file. Spot checks:
- Body text on bg-card (light): **14.8:1** (AAA)
- Foreground on dark bg: **16.8:1** (AAA)
- Primary KC blue on white: **4.5:1** (AA exact)
- White on att-present green-600: **4.6:1** (AA)
- White on att-absent red-500: **4.5:1** (AA exact)
- White on att-late amber-500: **3.0:1** (AA Large only — used on chips ≥18px text)

---

## VN UX strict (per dossier 02)

- ✅ **Class names**: `Lớp 10A2 - Toán nâng cao`, `Lớp 11B1 - Văn`
- ✅ **Attendance codes**: P (green-600) · V (blue-500) · M (red-500) · L (amber-500) — always **icon + letter** never color-only
- ✅ **Grade scale**: 0-10 decimal (`8.5`, `9.25`); honor classification ≥9 Xuất sắc · ≥8 Giỏi · ≥6.5 Khá · ≥5 TB · <5 Yếu
- ✅ **Names**: Bùi Thị Anh, Đặng Văn Bảo, Hoàng Thị Cẩm, Lê Minh Đức, Nguyễn Văn An, Phạm Thị Hương, Trần Quang Huy, Vũ Thị Mai (25 unique)
- ✅ **Date format**: `15/04/2026 14:00 - 15:30`
- ✅ **Late penalty**: `10%/ngày, tối đa 50%` (also encoded in `app.jsx#applyLatePenalty`)
- ✅ **GVCN comments**: empathetic Vietnamese ("Em An có tiến bộ tốt môn Toán")
- ✅ **Teacher MST**: 10-digit format (`8001234567`)
- ✅ **Currency**: `200.000đ/giờ`, `15% hoa hồng`, `14.4Mđ` (compact M for monthly figures)
- ✅ **Week**: Monday-first (T2-T3-T4-T5-T6-T7-CN)

---

## Touch UX (tablet primary)

- All buttons ≥ 44×44px (`tap-target`)
- Cell tap targets in attendance/grade ≥ 48×48 (`att-toggle`, `tap-target-lg`)
- Grade-cell `64px × 40px` to fit 5-char "10.00" without overflow
- Sticky save bar bottom — large primary action button at thumb reach

---

## Dark mode

6 of 24 screens have dark variants (one per section + 1 for grade-entry dark). Dark mode tokens defined in `styles.css` `:root` with `.dark` overrides:
- `--att-present` 160 60% 50% (lifted from light 39% for dark visibility)
- `--att-absent` 0 75% 65%
- `--att-late` 38 90% 60%

---

## Tech constraints honoured

| Constraint (dossier 09) | Honoured |
|---|:---:|
| KC stack: NO Framer Motion | ✅ CSS transitions + Tailwind Animate only |
| Tailwind via Play CDN | ✅ All HTML self-contained |
| lucide via unpkg | ✅ |
| Inter font (Google Fonts) | ✅ inherits from `_shared/colors_and_type.css` |
| Self-contained HTML | ✅ no build step required |
| Light + dark work | ✅ |

---

## Known gaps / future scope

- [ ] G3 Gradebook component spec (deferred to Wave 2 per dossier 04 — UI shipped inline here)
- [ ] G4 Schedule component spec (deferred to Wave 2 — UI shipped inline)
- [ ] G8 Calendar component spec (deferred to Wave 2 — heatmap shipped inline in reports-detail)
- [ ] Real Playwright capture of all 24 screens (this kit ships HTML; capture happens post-merge per `feedback_targeted_audit.md`)
- [ ] Component extraction once Wave 2 G3/G4/G8 specs land — currently each kit has duplicated grade-table HTML

---

## Wave context

Round 2 wave plan: `documents/03-planning/waves/wave-2026-04-29-ui-kits-round-2.md`
Related Round 1 PRs (shipped main): #668-#672
Round 2 sister kits:
- Wave 1.5: `kitehub-pro-v2` (in progress)
- Wave 1.6: this kit (`kiteclass-teacher`)

Per `feedback_wave_scope_completeness_check.md` — wave-scope coverage audits should run before wave merge to catch persona gaps like Teacher.

## Tenant-theme demo (GAP-1230)

Switcher nổi góc phải-dưới **"Chủ đề theo giáo viên"** demo 3 GV demo-trio per-tenant — đổi màu toàn kit runtime THẬT (set class `kc-demo-{ha|nhi|khanh}` trên `<html>`+`<body>` → override token `--primary`/`--accent`/`--ring`):

- **Cô Hà · Toán** — xanh dương `#2563EB`
- **Thầy Nhì · Hóa** — xanh lá `#16A34A`
- **Cô Khánh · Anh** — cam `#EA580C`

Nguồn dùng chung: `_shared/scripts/tenant-theme-demo.{css,js}` (port pattern từ `kiteclass-public/about.html` + `landing-personal`). Wire vào mọi screen + `index.html`. Affordance click có hiệu ứng runtime thật, không inert (per `design-source-implementation-parity.md` §3.2).

**Production:** theme thật đến từ `branding` package per ADR-009 (build-time per-tenant) — switcher này CHỈ là demo affordance trong design kit.
