# UI Review — KiteClass Frontend

**Ngày:** 2026-04-02
**Phiên bản:** main @ `2e8fbcd8` (sau PR #253)
**Phương pháp:** Code inspection + screenshot capture (thấy build error)
**Skill:** `.claude/skills/quality/ui-review/SKILL.md`
**Next review:** Sau khi fix build error → chạy lại `npx tsx scripts/capture-screenshots.ts --label after-build-fix`

---

## 🔴 Blocker: Build Error (phải fix trước)

```
Error: Cannot find module 'autoprefixer/lib/autoprefixer.js'
```

**Root cause:** pnpm trên WSL2 + NTFS filesystem không thể tạo symlinks → `node_modules` bị corrupt sau khi run.
**Impact:** Dev server không start → không thể chụp screenshot thực tế.

**Root cause chính xác:**
```
ERR_PNPM_EACCES: EACCES permission denied, rename '..._tmp_4054' → '@mswjs/interceptors'
ENOENT: no such file or directory, open '...msw/package.json'
```
pnpm dùng atomic rename (tmp → final) để đảm bảo atomicity. NTFS trên WSL2 không support rename cross-device/permission → fail ở package thứ 632/634.

**Root cause chính xác (đã điều tra đầy đủ):**

| Package manager | Error | Root cause |
|----------------|-------|-----------|
| pnpm | `ERR_PNPM_EACCES rename _tmp → final` (ở package 632/634) | NTFS không support atomic cross-permission rename |
| npm | `Cannot find module '../server/require-hook'` | NTFS write truncation — nhiều file nhỏ bị mất khi ghi |

**Kết luận:** NTFS filesystem qua WSL2 mount (`/mnt/f/`) không đủ ổn định cho node_modules lớn (634+ packages). Cả pnpm lẫn npm đều fail theo cách khác nhau.

**Fix đúng (cần thực hiện từ Windows hoặc WSL2 native):**
```bash
# Option 1 — Chạy từ Windows PowerShell/CMD (RECOMMENDED)
cd F:\nam4\doan\2026-Kite-Class-Platform\kiteclass\kiteclass-frontend
pnpm install    # hoặc npm install
npm run dev

# Option 2 — Clone vào WSL2 native filesystem
cp -r /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend ~/kiteclass-frontend
cd ~/kiteclass-frontend
pnpm install
node_modules/.bin/next dev --port 3000

# Sau đó screenshot script có thể chạy từ /mnt/f/ (chỉ connect tới localhost:3000)
cd /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend
npx tsx scripts/capture-screenshots.ts --label after-build-fix
```

---

## Fix Verification (từ audit trước — wave 10)

| Issue | Status | Notes |
|-------|--------|-------|
| UI/UX score 9/10 — sitemap dynamic chưa test | OPEN | Chưa có sitemap test |
| Onboarding wizard basic | OPEN | Chưa được nâng cấp |
| Login text tiếng Anh ("Welcome back") | FOUND NEW | i18n chưa áp dụng cho auth pages |

---

## Score (Code Inspection — không có screenshots)

> ⚠️ Score dựa trên code review, không phải screenshots thực tế.
> Cần chạy lại sau khi fix build error để có score chính xác.

### Public Pages

#### Landing `/`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 16/20 | Fallback data khi API fail ✅, SSR ✅, SEO metadata ✅, Next.js 15.1.6 outdated ⚠️ |
| Design Heuristics | 28/40 | Template system linh hoạt, nhưng không test được rendering |
| Visual Aesthetics | 18/28 | Theme system với CSS variables ✅, tenant branding ✅ |
| User Friendliness | 14/20 | Dynamic template (Personal/Organization) ✅ |
| WCAG | 12/20 | OrganizationJsonLd ✅, screen reader chưa verify |
| **Total** | **88/128** | Cannot verify without actual render |

#### Login `/login`

| Dimension | Score | Notes |
|-----------|-------|-------|
| Technical | 14/20 | `'use client'` ✅, Zod validation ✅, form disabled khi loading ✅ |
| Design Heuristics | 26/40 | AuthLayout ✅, error messages hiển thị ✅, remember me ✅ |
| Visual Aesthetics | 18/28 | Shadcn components ✅, space-y-6 layout |
| User Friendliness | 14/20 | Clear CTA, forgot password link ✅ |
| WCAG | 10/20 | Labels qua FormInput ✅, nhưng chưa verify contrast |
| **Total** | **82/128** | |

**Issues phát hiện qua code:**
- ❌ Text English hardcode: `"Welcome back"`, `"Sign in to your account to continue"` — chưa i18n
- ❌ Password validation chỉ `min(6)` — không enforce complexity (đã note trong audit trước)
- ⚠️ Không có ARIA live region cho error messages

---

### Auth Pages Summary (code-based)

| Page | Phát hiện | Priority |
|------|-----------|---------|
| `/login` | Text English hardcode (i18n missing) | 🔴 High |
| `/register` | Cần check tương tự login | 🟡 Medium |
| `/register/student` | Cần check | 🟡 Medium |
| `/forgot-password` | Chưa review code | 🟢 Low |
| `/reset-password` | Chưa review code | 🟢 Low |

---

## Environment Issues Discovered

| Issue | Impact | Fix |
|-------|--------|-----|
| `autoprefixer.js` missing | Dev server không start | `npm install` hoặc chạy từ Windows |
| Next.js 15.1.6 outdated | Warning in browser | `pnpm add next@latest` |
| pnpm + WSL2 + NTFS symlink | pnpm install bị stuck | Dùng npm hoặc Windows native |

---

## Gotchas cần thêm vào SKILL.md

1. **pnpm + WSL2 + NTFS = symlink issue** — `node_modules` không được tạo đúng. Dùng `npm install` thay thế khi chạy trên WSL2 mount NTFS.
2. **Next.js 15.1.6 outdated warning** — hiển thị trong góc phải màn hình, ảnh hưởng screenshot chất lượng.
3. **Landing page cần fallback data** — API fail → hiển thị default text. Screenshot có thể không đại diện cho thực tế tenant.

---

## Action Items

| Priority | Action | Owner |
|----------|--------|-------|
| 🔴 P0 | Fix build error (`npm install` trên WSL2 hoặc Windows) | Dev |
| 🔴 P1 | i18n cho auth pages (login, register text) | PR |
| 🟡 P2 | Chạy lại screenshot capture sau fix | Claude |
| 🟡 P2 | Verify WCAG contrast trên auth pages | PR |
| 🟢 P3 | Upgrade Next.js 15.1.6 → latest | PR |

---

## Lần chạy tiếp theo

**Workflow đúng cho environment WSL2 + NTFS:**

```bash
# Bước 1: Start dev server từ Windows PowerShell
cd F:\nam4\doan\2026-Kite-Class-Platform\kiteclass\kiteclass-frontend
pnpm install   # hoặc npm install
npm run dev    # chạy trên port 3000

# Bước 2: Từ WSL2 (khi server đã up) — chạy capture
cd /mnt/f/nam4/doan/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend
npx tsx scripts/capture-screenshots.ts --label after-build-fix
# Script dùng tsx được cài global, không cần node_modules local
```

Sau đó review screenshots và update score section trên.
