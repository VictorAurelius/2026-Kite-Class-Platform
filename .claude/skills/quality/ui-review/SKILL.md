---
name: ui-review
description: "Dùng khi user nói 'review UI', 'check design', 'audit screenshots', 'UI trông thế nào', hoặc sau mỗi PR có thay đổi frontend. Auto-chạy sau mỗi frontend PR. Capture before/after screenshots, score per-screen trên thang /128."
user-invocable: true
---

# UI Review — KiteClass Frontend

Per-screen scoring + before/after screenshots. Dùng Playwright (đã có sẵn trong kiteclass-frontend).

## Process

### 0. Fix Verification (BẮT BUỘC nếu có report trước)

Check từng issue đã report trong `documents/04-quality/ui-review-latest.md`:

```
| Issue | Status | Notes |
|-------|--------|-------|
| [previous issue] | FIXED / STILL OPEN / PARTIAL | ... |
```

### 1. Capture Screenshots

**BEFORE fix** (BẮT BUỘC):
```bash
cd kiteclass/kiteclass-frontend
npx tsx scripts/capture-screenshots.ts --label before-pr-XXX
```

**AFTER fix merged** (auto-chạy, không chờ user):
```bash
npx tsx scripts/capture-screenshots.ts --label after-pr-XXX
```

Output: `documents/screenshots/{label}/{page}/{theme}-{viewport}.png`

Script auto-detect dev server, start nếu chưa chạy. Auto-update `latest/` khi dùng `--label`.

### 2. Score per screen (/128)

5 dimensions, mỗi screen chấm độc lập:

- **Technical (/20)** — accessibility, responsive, theming, anti-patterns (hardcoded colors, window.confirm)
- **Design Heuristics (/40)** — Nielsen's 10 heuristics (0–4 mỗi cái)
- **Visual Aesthetics (/28)** — color, typography, sizing, spacing, alignment, hierarchy, polish
- **User Friendliness (/20)** — first impression, navigation, action clarity, learning curve
- **WCAG Accessibility (/20)** — contrast, touch targets, labels, screen reader, keyboard

Report LOWEST screen riêng — đây là quality bar thực sự.

### 3. Before/After comparison

```
| Screen | Before | After | What changed |
|--------|--------|-------|--------------|
| login  | before-pr-XXX/login/dark-mobile.png | after-pr-XXX/login/dark-mobile.png | ... |
```

### 4. Output

Lưu report vào `documents/04-quality/ui-review-latest.md`.

---

## Scoring Rubric

- **0/4** = Thiếu hoàn toàn
- **1/4** = Có nhưng bị broken
- **2/4** = Có nhưng có vấn đề rõ ràng **(DEFAULT cho hầu hết features)**
- **3/4** = Hoạt động tốt, nhất quán TRÊN TẤT CẢ screens
- **4/4** = Genuinely excellent

**"Có feature" = 2/4, KHÔNG phải 3/4.** Trước khi cho 3: "External auditor có đồng ý đây là tốt không?"

---

## Gotchas — KiteClass-specific

- **WSL2 + NTFS mount = node_modules broken** — pnpm: `ERR_PNPM_EACCES rename _tmp→final` (632/634). npm: truncates large packages (`require-hook` missing). **Không thể install node_modules trên `/mnt/f/` (NTFS).** Fix: chạy dev server từ Windows PowerShell, HOẶC copy source vào WSL2 native fs (`~/`) để install
- **autoprefixer.js missing** — khi node_modules corrupt. Triệu chứng: Build Error trên tất cả pages. Fix: reinstall dependencies
- **Next.js 15.1.6 outdated warning** — hiển thị banner đỏ góc phải trong screenshots. Không ảnh hưởng functionality nhưng ảnh hưởng visual score
- **Auth pages** — `/login`, `/register`, `/forgot-password` là `'use client'` — chụp được mà không cần backend, nhưng cần node_modules đầy đủ
- **Dashboard pages** — cần đăng nhập trước; xem playwright.config.ts để tham khảo auth setup
- **Tenant theme** — KiteClass dùng `kiteclass_theme` localStorage key, KHÁC với `theme` (next-themes)
- **Landing page fallback** — API fail → hiển thị default text/colors. Screenshot không đại diện cho tenant thực tế
- **Port** — Next.js chạy trên 3000, không phải 5173
- **Score what you SEE** — không được self-score theo code; external auditor thường thấp hơn 20–35 pts
- Screenshots gitignored — local only, không commit PNG

---

## KiteClass Pages to Audit

### Public / Auth (không cần backend)

| Page | Route | Priority |
|------|-------|----------|
| Landing | `/` | 🔴 High |
| Login | `/login` | 🔴 High |
| Register | `/register` | 🟡 Medium |
| Register Student | `/register/student` | 🟡 Medium |
| Forgot Password | `/forgot-password` | 🟢 Low |
| Reset Password | `/reset-password` | 🟢 Low |
| About | `/about` | 🟡 Medium |
| Catalog | `/catalog` | 🟡 Medium |
| Contact | `/contact` | 🟢 Low |

### Dashboard (cần auth — setup storageState)

`/classes`, `/courses`, `/students`, `/attendance`, `/billing`, `/settings`, `/teacher/dashboard`

---

## Skill Contents

- `scripts/capture-screenshots.ts` (trong `kiteclass/kiteclass-frontend/scripts/`) — auto-capture
- `documents/04-quality/ui-review-latest.md` — latest report
- `documents/screenshots/latest/` — latest screenshot set
