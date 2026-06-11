# GAP-1208: Template PERSONAL render giọng/heading trung tâm — không khác biệt với ORGANIZATION

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (user G2★ walk — "template hình như là của trung tâm chứ không phải của giáo viên độc lập")
**Affects:** `kiteclass-frontend/src/lib/template/configs.ts` + section components heading hardcode + `TemplateRenderer.tsx`

## Problem

Tenant personal (cô Hà/thầy Nhì, `templateType=personal` — payload đúng) nhưng landing đọc như trang trung tâm: heading "Đội ngũ giáo viên" cho giáo viên solo, "Bảng giá"/"Đánh giá" giọng tổ chức, bộ section PERSONAL ≈ ORGANIZATION (sau GAP-1194 enable teachers). Root: section components hardcode heading center-voice; config `label` không flow vào render.

## Fix (PR #2326, agent GAP-1208)

TemplateRenderer truyền heading từ config section entry → components consume (fallback cũ); PERSONAL_TEMPLATE labels đổi giọng giáo viên độc lập ("Về giáo viên", "Học phí", "Phụ huynh & học viên nói gì"...); cân nhắc layout profile đơn cho 1 teacher.

## Acceptance Criteria

- [x] Landing personal: headings giọng cá nhân, khác biệt rõ với organization
- [x] Organization không regression
- [x] Build + tests PASS + re-walk screenshot

## Log

- **2026-06-11 (DONE):** Agent fix PR #2326: SectionConfig.heading/subheading + TemplateRenderer thread vào 9 section components (fallback giữ); PERSONAL_TEMPLATE voice GV độc lập ("Về giáo viên"/"Giáo viên đồng hành"/"Học phí"/"Phụ huynh & học viên nói gì"); TeachersSection profile đơn khi 1 GV. vitest 895 pass + build PASS. Re-walk verify screenshot cô Hà/thầy Nhì — voice cá nhân rõ, organization không đổi.

## Related

- Sister: GAP-1194 (teachers cho personal), GAP-1205 (F-section audience), wave landing-100 Bucket F
- Discovered in: user G2★ walk 2026-06-11
