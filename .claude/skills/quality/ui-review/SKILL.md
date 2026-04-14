---
name: ui-review
description: "Dùng khi user nói 'review UI', 'check design', 'audit screenshots', 'UI trông thế nào', hoặc sau mỗi PR có thay đổi frontend. Auto-chạy sau mỗi frontend PR. Capture before/after screenshots, score per-screen trên thang /128. Covers KiteClass (port 3000) + KiteHub (port 3001) — tất cả public, auth, và dashboard pages."
user-invocable: true
---

# UI Review — KiteClass + KiteHub Frontend

Per-screen scoring + before/after screenshots. Covers 100% screens: public, auth, dashboard (mock auth injected).

## Process

### 0. Fix Verification (BẮT BUỘC nếu có report trước)

Đọc `documents/04-quality/audits/ui/ui-review-latest.md`, check từng issue:

```
| Issue | Status | Notes |
|-------|--------|-------|
| [previous issue] | FIXED / STILL OPEN / PARTIAL | ... |
```

### 1. Capture Screenshots

**Một lệnh — cả hai apps:**
```bash
# Từ project root
./scripts/capture-ui-all.sh --label pr-XXX

# Hoặc từng app riêng:
cd kiteclass/kiteclass-frontend && npx tsx scripts/capture-screenshots.ts --label pr-XXX
cd kitehub/kitehub-frontend    && npx tsx scripts/capture-screenshots.ts --label pr-XXX
```

Output per app:
- `documents/screenshots/{label}/` — KiteClass (30 pages × 4 = ~120 PNGs)
- `documents/screenshots/kitehub-{label}/` — KiteHub (19 pages × 4 = ~76 PNGs)
- `manifest.md` trong mỗi folder — auto-generated, **committed to git**

**Dashboard auth:** Script tự inject mock Zustand state vào localStorage — không cần backend.
Dashboard pages render đầy đủ layout; API calls fail gracefully (shows loading/error UI).

### 2. Đọc manifest.md trước khi xem ảnh

```bash
# manifest.md là index — đọc trước để biết có gì, page nào fail
documents/screenshots/{label}/manifest.md
documents/screenshots/kitehub-{label}/manifest.md
```

manifest.md có: page list, HTTP status, file sizes, error pages — không cần đọc lại ảnh cho thông tin cơ bản.

### 3. Score per screen (/128)

Đọc screenshots để chấm. 5 dimensions, mỗi screen độc lập:

- **Technical /20** — responsive, dark mode, theming, anti-patterns
- **Design Heuristics /40** — Nielsen's 10 heuristics (0–4 mỗi cái)
- **Visual Aesthetics /28** — color, typography, spacing, hierarchy, polish
- **User Friendliness /20** — first impression, navigation, action clarity
- **WCAG /20** — contrast, touch targets, labels, screen reader, keyboard

Report LOWEST screen — đây là quality bar thực sự.

### 4. Before/After comparison

```
| Screen | Before | After | Delta | Notes |
|--------|--------|-------|-------|-------|
| login  | 82/128 | 95/128 | +13 | i18n fixed |
```

### 5. Output

Lưu vào `documents/04-quality/audits/ui/ui-review-latest.md`. Commit `manifest.md` (không commit PNG).

---

## Scoring Rubric

- **0/4** = Thiếu hoàn toàn
- **1/4** = Có nhưng broken
- **2/4** = Có nhưng vấn đề rõ ràng **(DEFAULT cho hầu hết features)**
- **3/4** = Tốt, nhất quán TRÊN TẤT CẢ screens
- **4/4** = Genuinely excellent

**"Có feature" = 2/4, KHÔNG phải 3/4.** Hỏi: "External auditor có đồng ý không?"

---

## Gotchas

- **WSL2 + NTFS = node_modules broken** — pnpm: `ERR_PNPM_EACCES rename _tmp→final`. npm: write truncation. Fix: start dev servers từ Windows PowerShell trước
- **Two localhost ports** — KiteClass: 3000, KiteHub: 3001. Đừng nhầm
- **Dashboard auth injection** — mock token không valid → API 401 → pages show loading/error. Đây là intentional: captures error UI
- **`kiteclass_theme`** — KiteClass dùng key riêng (KHÁC `theme` của next-themes). Script inject cả hai
- **Dark mode** — nếu light/dark screenshots trông giống nhau: dark mode không hoạt động (cần investigate)
- **Catalog loading spinner** — không có backend → spinner vô hạn. Expected behavior to capture
- **Parameterized routes** (`[id]`) — dùng `audit-001` placeholder → có thể 404. Captures error handling UI
- **`manifest.md` committed, `*.png` gitignored** — commit manifest sau mỗi audit run
- **Score what you SEE** — external auditor thường thấp hơn 20–35 pts so với self-score

---

## Full Page Registry

### KiteClass (port 3000) — 30 pages

| Group | Pages |
|-------|-------|
| Public (5) | landing, about, catalog, catalog-detail, contact |
| Auth (5) | login, register, register-student, forgot-password, reset-password |
| Dashboard (21) | classes, class-detail, class-edit, class-attendance, courses, course-new, course-detail, course-edit, course-class-new, students, student-new, student-detail, student-edit, student-attendance, teachers, teacher-new, teacher-detail, teacher-edit, attendance, attendance-reports, attendance-stats, billing, billing-detail, billing-pay, settings, teacher-dashboard |

### KiteHub (port 3001) — 19 pages

| Group | Pages |
|-------|-------|
| Public (4) | landing, pricing, blog, blog-detail |
| Auth (3) | login, register, verify-email |
| Customer (11) | dashboard, instance-detail, settings, billing, billing-history, billing-upgrade, billing-payment, branding, branding-assets, branding-templates, branding-wizard |
| Admin (5) | admin, admin-instances, admin-instance-detail, admin-payments, admin-revenue |

---

## Skill Contents

- `scripts/capture-ui-all.sh` — master script, cả hai apps một lệnh
- `kiteclass/kiteclass-frontend/scripts/capture-screenshots.ts` — KiteClass capture (v2)
- `kitehub/kitehub-frontend/scripts/capture-screenshots.ts` — KiteHub capture (v1)
- `documents/screenshots/README.md` — giải thích cấu trúc + git policy
- `documents/04-quality/audits/ui/ui-review-latest.md` — latest audit report
