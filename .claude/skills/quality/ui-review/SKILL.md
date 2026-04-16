---
name: ui-review
description: "Dùng khi user nói 'review UI', 'check design', 'audit screenshots', 'UI trông thế nào', hoặc sau mỗi PR có thay đổi frontend. Auto-chạy sau mỗi frontend PR. Capture before/after screenshots, score per-screen trên thang /128. Covers KiteClass (port 4700) + KiteHub (port 4701) — tất cả public, auth, và dashboard pages."
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
- `documents/screenshots/kiteclass-{label}/` — KiteClass (30 pages × 4 = ~120 PNGs)
- `documents/screenshots/kitehub-{label}/` — KiteHub (19 pages × 4 = ~76 PNGs)
- `manifest.md` trong mỗi folder — auto-generated, **committed to git**

**Dashboard auth:** Script tự inject mock Zustand state vào localStorage — không cần backend.
Dashboard pages render đầy đủ layout; API calls fail gracefully (shows loading/error UI).

### 2. Manifest Triage (TRƯỚC KHI xem ảnh)

Đọc `manifest.md` để phân loại pages TRƯỚC, tránh xem ảnh không cần thiết:

```
Size < 20KB  → SKIP (loading spinner / empty shell) → auto-score 24/128
Size 20-50KB → SPOT-CHECK (xem 1 ảnh light-desktop xác nhận)
Size > 50KB  → FULL REVIEW (xem + score đầy đủ)
```

Ghi danh sách pages cần xem TRƯỚC khi mở bất kỳ ảnh nào.

### 3. Context-Efficient Scoring

**CRITICAL: UI audit tiêu tốn rất nhiều tokens. Tuân thủ 4 rules sau để tránh compaction:**

#### Rule 1: Smart Sampling — chỉ xem 1 variant per page

Chỉ xem `light-desktop` cho mỗi page. Xem thêm variants CHỈ KHI:
- File size bất thường (light vs dark chênh >50%)
- Page có dark-mode-specific logic cần verify
- Previous audit flagged responsive issue → xem mobile

#### Rule 2: Group Scoring — score đại diện, apply cho nhóm

Pages cùng layout pattern → score 1 đại diện, copy ±5 cho pages khác:

| Group | Representative | Apply to |
|-------|---------------|----------|
| Public marketing | landing | pricing, about |
| Auth forms | login | register, forgot-password, reset-password |
| Dashboard tables | classes | students, teachers, courses, attendance |
| Dashboard forms | student-new | teacher-new, course-new, class-edit |
| Dashboard detail | class-detail | student-detail, teacher-detail, course-detail |
| Admin tables | admin-instances | admin-payments |

Chỉ individually score pages có layout ĐẶC BIỆT (dashboard home, branding-wizard, catalog).

#### Rule 3: Subagent Delegation cho full audit

Full audit (>20 pages) PHẢI dùng subagents để tránh context overflow:

```
Parent: đọc manifests → phân nhóm → spawn agents → aggregate scores

Agent 1: KiteHub public + auth (7 pages, ~10 ảnh)
Agent 2: KiteHub customer + admin (17 pages, ~8 ảnh review + 9 auto-score)
Agent 3: KiteClass public + auth (10 pages, ~10 ảnh)
Agent 4: KiteClass dashboard (21 pages, ~5 ảnh review + 16 auto-score)
```

Mỗi agent: xem ảnh → score → trả JSON summary (KHÔNG trả chi tiết per-dimension).

Agent return format:
```json
{
  "scores": [{"page": "landing", "total": 92, "notes": "gradient hero, good responsive"}],
  "issues": [{"id": "H-1", "severity": "P1", "page": "dashboard", "issue": "sidebar overlap"}],
  "autoScored": [{"page": "settings", "total": 24, "reason": "loading spinner only"}]
}
```

#### Rule 4: Targeted Audit — chỉ audit changed pages

Sau baseline, KHÔNG full re-audit. Chỉ audit screens bị ảnh hưởng bởi PR:

```bash
# Xác định changed files
git diff main --name-only | grep -E '\.(tsx|css|scss)$'

# Map file → screen (từ page registry)
# Chỉ capture + score affected screens
```

### 4. Score Dimensions (/128)

5 dimensions, mỗi screen độc lập:

- **Technical /20** — responsive, dark mode, theming, anti-patterns
- **Design Heuristics /40** — Nielsen's 10 heuristics (0–4 mỗi cái)
- **Visual Aesthetics /28** — color, typography, spacing, hierarchy, polish
- **User Friendliness /20** — first impression, navigation, action clarity
- **WCAG /20** — contrast, touch targets, labels, screen reader, keyboard

Report LOWEST screen — đây là quality bar thực sự.

### 5. Before/After comparison

```
| Screen | Before | After | Delta | Notes |
|--------|--------|-------|-------|-------|
| login  | 82/128 | 95/128 | +13 | i18n fixed |
```

### 6. Output

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
- **Two localhost ports** — KiteClass: 4700, KiteHub: 4701. Đừng nhầm
- **Dashboard auth injection** — mock token không valid → API 401 → pages show loading/error. Đây là intentional: captures error UI
- **`kiteclass_theme`** — KiteClass dùng key riêng (KHÁC `theme` của next-themes). Script inject cả hai
- **Dark mode** — nếu light/dark screenshots trông giống nhau: dark mode không hoạt động (cần investigate)
- **Catalog loading spinner** — không có backend → spinner vô hạn. Expected behavior to capture
- **Parameterized routes** (`[id]`) — dùng `audit-001` placeholder → có thể 404. Captures error handling UI
- **`manifest.md` committed, `*.png` gitignored** — commit manifest sau mỗi audit run
- **Score what you SEE** — external auditor thường thấp hơn 20–35 pts so với self-score
- **Compaction kills scoring** — nếu context bị compact giữa audit, scores drift vì mất visual memory. Dùng subagents để tránh
- **KiteClass `waitUntil`** — capture script dùng `domcontentloaded` → CSS có thể chưa load. Nếu ảnh trông unstyled, đây là capture bug, không phải code bug

---

## Full Page Registry

### KiteClass (port 4700) — 30 pages

| Group | Pages |
|-------|-------|
| Public (5) | landing, about, catalog, catalog-detail, contact |
| Auth (5) | login, register, register-student, forgot-password, reset-password |
| Dashboard (21) | classes, class-detail, class-edit, class-attendance, courses, course-new, course-detail, course-edit, course-class-new, students, student-new, student-detail, student-edit, student-attendance, teachers, teacher-new, teacher-detail, teacher-edit, attendance, attendance-reports, attendance-stats, billing, billing-detail, billing-pay, settings, teacher-dashboard |

### KiteHub (port 4701) — 19 pages

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
