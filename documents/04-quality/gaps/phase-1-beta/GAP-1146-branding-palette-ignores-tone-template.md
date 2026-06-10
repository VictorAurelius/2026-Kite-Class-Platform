# GAP-1146: Branding palette KHÔNG phản ánh tone/style/template — deriver hash org-name

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (deriver) + Frontend (preview)
**Found:** 2026-06-10 (Wizard Step 7 G2 browser-walk — PR #2289)
**Affects:** `kitehub-branding` `BrandColoursDeriver.derive` · wizard preview palette

## Problem

User chọn **phong cách (tone)** "Sang trọng / Năng động..." + **template** + **đối tượng** ở wizard, nhưng **bảng màu preview không đổi — chỉ ra 1 màu cố định** (G2 feedback #5: "bảng màu, phong cách, không được thể hiện ở preview, chỉ mặc định").

Root cause (state-check):
- `BrandColoursDeriver.derive(job)` = `PALETTE[Math.floorMod(hashCode(organizationName), PALETTE.length)]` — palette **chỉ là hash của TÊN trung tâm**, KHÔNG dùng `audience` / `tone` / `templateId`.
- `TEMPLATES` (TemplateGrid) không mang palette riêng (chỉ id/code/name + SVG preview).
- Hệ quả: đổi tone/template/audience → palette không bao giờ đổi. Cả **preview** lẫn **deploy** đều dùng palette hash-theo-tên, lệch hoàn toàn với phong cách user chọn — sai triết lý "branding phản ánh lựa chọn" (`ai-branding-guidelines.md` §1 TEMPLATE-first).

## Proposed Fix

1. Định nghĩa **palette theo tone** (professional / friendly / energetic / luxury) [+ biến thể theo template]; spec trong `ai-branding-guidelines.md` hoặc rules.md.
2. `BrandColoursDeriver` nhận `tone`/`templateId` → chọn palette theo phong cách (vẫn deterministic; org-name làm seed phụ cho biến thể).
3. Preview (`usePreviewBrandColors`) tự động phản ánh (vì đọc job-derived palette) — không cần đổi FE nếu BE sửa.

## Acceptance Criteria

- [ ] Đổi tone → palette preview đổi rõ rệt (4 tone = 4 hướng màu khác nhau).
- [ ] Palette deterministic + WCAG AA contrast (per `ai-branding-guidelines.md` §5).
- [ ] Preview ↔ deploy dùng cùng palette (không lệch).

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- `ai-branding-guidelines.md` §1 Resource Classification / §8 Template criteria
