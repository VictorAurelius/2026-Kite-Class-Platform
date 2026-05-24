# GAP-726: KC `/branding/wizard` render trắng + SSR ECONNREFUSED localhost:8080

**Status:** 🟡 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend)
**Detected:** 2026-05-23 (RST Đợt 107 Mảng B-onboard B2)
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

- [ ] `/branding/wizard` sau khi `owner.test` đăng nhập render UI wizard (≥1 input/button hiển thị)
- [ ] Tenant ID + slug đọc từ AuthContext (KHÔNG hardcode `current-tenant` + `my-school`)
- [ ] Error boundary bao quanh `<BrandingWizard>` — nếu component throw, hiển thị message tiếng Việt thay vì trang trắng
- [ ] SSR `axios` call trỏ đến cổng đúng (kiểm `next.config.*` + env `NEXT_PUBLIC_KC_GATEWAY_URL` hoặc tương đương — cập nhật sang cổng dịch vụ thật, ví dụ 8081 nếu gateway dời cổng)
- [ ] Playwright spec `e2e/_rst-wave-107-owner-onboard.spec.ts` B2 PASS với `B2_INPUT_COUNT > 0`

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
