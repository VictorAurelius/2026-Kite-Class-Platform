# GAP-726: KC `/branding/wizard` render trắng + SSR ECONNREFUSED localhost:8080

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend)
**Detected:** 2026-05-23 (RST Đợt 107 Mảng B-onboard B2)
**Resolved:** 2026-06-02 (autonomous gap campaign — branch `feature/GAP-726-wizard-ssr-blank`)
**Related PRs:** (Đợt 107 RST PR pending)
**Related Docs:**
- `documents/04-quality/audits/persona-review/2026-05-23-wave-107-rst-a-b-onboard.md`
- `documents/03-planning/waves/wave-2026-05-23-107-hybrid-rst-anonymous-onboard-plus-email-fix.md`

## Vấn đề

Đợt 107 RST Mảng B-onboard B2 walk Chủ trung tâm vào trợ lý cài đặt ban đầu `/branding/wizard` tại KC frontend `localhost:3000`. Sau khi đăng nhập với `owner.test@test.vn` (đã seeded Đợt 105), điều hướng đến `/branding/wizard` cho ra:

- **Body trống** — không có `h1`, không có nội dung text (Playwright `bodyText.trim().length < 50`)
- **Không có input/form** — wizard component không render UI
- **Console log SSR cảnh báo**: `Failed to fetch landing page data: Error: connect ECONNREFUSED 127.0.0.1:8080`

Hai phát hiện ghép thành một gap vì cùng nguyên nhân gốc rễ: scaffold KC frontend ở trạng thái pre-tenant + cấu hình SSR data fetch trỏ sai cổng dịch vụ.

### Bằng chứng

```bash
# RST Đợt 107 B2 (chạy 2026-05-23):
PLAYWRIGHT_BASE_URL=http://localhost:3000 \
  pnpm exec playwright test e2e/_rst-wave-107-owner-onboard.spec.ts \
  --project=chromium --grep "RST-B2"

# Output:
# B2_URL: http://localhost:3000/branding/wizard
# B2_H1: no-h1
# B2_BODY_SAMPLE: (empty)
# B2_INPUT_COUNT: -1 (page closed before count resolved)
# B2_VERDICT: BLANK_RENDER_BUG
#
# WebServer log:
# Failed to fetch landing page data: Error: connect ECONNREFUSED 127.0.0.1:8080
# axios → 127.0.0.1:8080 không có dịch vụ nào lắng nghe
```

Ảnh chụp: `/tmp/rst-screenshots/wave-107/b2-1-branding-wizard.png` (trang trắng).

### Đối chiếu với B4 thành công

B4 walk 9 routes dashboard (`/overview` `/branding` `/students` `/teachers` `/classes` `/courses` `/attendance` `/billing` `/settings`) đều render H1 tiếng Việt đúng (Tổng quan / AI Branding / Học viên / Giáo viên / Lớp học / Khóa học / Điểm danh / Hóa đơn / Cài đặt). Chỉ riêng `/branding/wizard` (không phải `/branding` đứng độc lập) bị lỗi render.

## Nguyên nhân gốc rễ (chưa xác minh đầy đủ — RST mới chỉ phát hiện)

`src/app/(dashboard)/branding/wizard/page.tsx`:

```typescript
'use client';

import { BrandingWizard } from '@/components/branding/wizard/BrandingWizard';

export default function BrandingWizardPage() {
  return <BrandingWizard tier="PRO" tenantId="current-tenant" slug="my-school" />;
}
```

Giả thuyết:
1. **Hardcoded `tenantId="current-tenant"` + `slug="my-school"` không khớp tenant thật** của `owner.test` → `BrandingWizard` cố gọi API với slug giả → fail → component render null (không có error boundary fallback)
2. **'use client' + AuthContext chưa hydrate** xong → component đầu vòng render trả null
3. **ECONNREFUSED 8080** ở SSR landing data có thể chặn page render hoàn toàn (Next.js SSR error → blank)

Cần investigation deeper. Nhiều khả năng là tổ hợp cả 3.

ECONNREFUSED 8080 riêng: thông thường gateway KC chạy tại 8080 (per `docker-compose.kitehub.yml`?). Stack hiện tại 13 dịch vụ healthy nhưng `docker ps` không liệt dịch vụ nào ở 8080 — có thể KC frontend dev cấu hình trỏ sang cổng đã thay đổi mà chưa cập nhật.

## Tiêu chí chấp nhận (AC)

- [x] `/branding/wizard` sau khi `owner.test` đăng nhập render UI wizard (≥1 input/button hiển thị) — verify qua unit test `wizard-page.test.tsx` "renders the wizard UI (welcome step) for an authenticated owner" (PASS: `buttons.length > 0`)
- [x] Tenant ID + slug đọc từ session (KHÔNG hardcode `current-tenant` + `my-school`) — `tenantId` đọc từ `useAuthStore` (JWT claim set lúc login), `slug` đọc từ `useTenantFromUrl()` (query param/subdomain) + localStorage fallback
- [x] Fallback graceful — nếu session chưa hydrate / chưa có tenant, hiển thị message tiếng Việt "Đang tải thông tin trung tâm…" thay vì trang trắng (verify test "shows graceful loading message (not blank) when session has no tenant")
- [x] SSR `axios` ECONNREFUSED 8080: state-check xác nhận lỗi này KHÔNG thuộc route wizard. `BrandingWizard` là `'use client'` pure (state-machine FSM), KHÔNG có SSR fetch. ECONNREFUSED 8080 phát từ `(public)/page.tsx:43` (landing page) — route khác, đã có try/catch fallback graceful (line 42-56). Wizard route không bị ảnh hưởng.
- [x] Suspense boundary: `useSearchParams` (qua `useTenantFromUrl`) bọc trong `<Suspense>` — production `next build` PASS (`✓ Compiled successfully`, `/branding/wizard` prerendered static, 59/59 pages, exit 0), không prerender bailout

> Playwright spec B2 positive (`B2_INPUT_COUNT > 0`) defer — repro yêu cầu live stack + seeded `owner.test` browser walk; unit test + build verify đã cover AC chính. Browser walk verify Phase 1 BETA live (gated GAP-612 AWS restore) — không block DONE flip vì code-level fix đã verify đầy đủ qua unit test (3/3 PASS) + production build PASS.

### Out of scope (per `gap-done-discipline.md` §3 Option B)

- Live verify hậu-AWS — gated GAP-612 AWS phục hồi (file follow-up `GAP-XXX-post-aws-live-verify-branding-wizard-wave-108`)

## Đề xuất sửa

**Đợt 108 Mảng B-onboard tiếp:**
1. Investigate KC frontend env config — `NEXT_PUBLIC_*_URL` cờ trỏ đâu so với `docker-compose.kitehub.yml` ports
2. Sửa `BrandingWizardPage` đọc `tier`/`tenantId`/`slug` từ AuthContext
3. Bọc `<BrandingWizard>` bằng error boundary có message tiếng Việt
4. Cập nhật SSR data fetch trỏ cổng đúng (likely 8081 OR loại bỏ SSR fetch khỏi route này nếu không cần)
5. Mở rộng Playwright B2 verify positive — ≥1 input + verify VN heading

## Ưu tiên

P1: chặn B2 luồng Trợ lý cài đặt ban đầu — Chủ trung tâm seeded không hoàn tất onboarding qua wizard. WORKAROUND = vào `/branding` standalone (render OK per B4) để cấu hình thương hiệu thủ công, nhưng mất experience wizard.

## Log

- **2026-05-23 (RST Đợt 107 B2):** Phát hiện qua Playwright walk; ảnh chụp lưu `/tmp/rst-screenshots/wave-107/b2-1-branding-wizard.png` + console log SSR `ECONNREFUSED 127.0.0.1:8080`. File gap defer Đợt 108 phân loại; Đợt 107 closure không sửa B2 vì scope là email cụm + RST khám phá.

- **2026-06-02 (fix — autonomous gap campaign, branch `feature/GAP-726-wizard-ssr-blank`):** Scope revised after fix-time state-check (per `audit-to-gap-pipeline.md` §2.8 — gap >7 ngày tuổi).

  **State-check (root cause):** 3 giả thuyết trong gap được verify empirically:
  1. **Hardcoded `tenantId="current-tenant" slug="my-school"`** → CONFIRMED present `wizard/page.tsx:14`. Root cause chính của render-blank — wizard cố submit với tenant giả không tồn tại.
  2. **`'use client' + AuthContext chưa hydrate`** → CONFIRMED contributing — page không có loading/fallback state, blank trước khi hydrate.
  3. **ECONNREFUSED 8080 chặn render** → REJECTED. Lỗi này phát từ `(public)/page.tsx:43` (landing page SSR fetch), KHÔNG phải route wizard. `BrandingWizard` là `'use client'` pure (FSM state-machine via `useBrandingWizard`), không có SSR data fetch. Landing page đã có try/catch fallback graceful (line 42-56). Hai phát hiện trong gap thực ra là 2 nguyên nhân gốc KHÁC NHAU ở 2 route khác nhau — chỉ wizard route cần fix.

  **Fix:** rewrite `wizard/page.tsx`:
  - `tenantId` đọc từ `useAuthStore` (JWT claim set lúc login per `useAuth.ts:42-46`)
  - `slug` đọc từ `useTenantFromUrl()` (query param/subdomain) + localStorage fallback (`tenantSubdomain` → `tenantId`)
  - `<Suspense>` boundary bọc inner component (vì `useTenantFromUrl` dùng `useSearchParams`) — satisfy Next.js prerender boundary per `fe-build-local-verify.md`
  - Graceful loading state khi `tenantId` chưa hydrate (message tiếng Việt) thay vì blank

  **Verify:**
  - Unit test mới `wizard-page.test.tsx` 3/3 PASS (render wizard UI cho authenticated owner / graceful loading khi no tenant / không dùng scaffold value)
  - Branding suite 6/6 PASS (no regression với gateway test)
  - `next lint` wizard page: ✔ No ESLint warnings or errors
  - `pnpm --filter kiteclass-frontend build`: ✓ Compiled successfully, `/branding/wizard` prerendered static (○), 59/59 pages, exit 0

  **Cross-flow sweep (per `cross-flow-bug-class-sweep.md`):** grep `tenantId="current-tenant"` / `slug="my-school"` toàn FE → 0 sister site (chỉ match doc comment trong fix). Single-site fix cover full bug class.

  **Browser walk B2 positive** defer — gated live stack + seeded `owner.test` (GAP-612 AWS restore). Code-level fix verify đầy đủ qua unit test + production build.
