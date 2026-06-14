# GAP-1379: KC overview dashboard sparse (4/6 KPI placeholder + recent-activity placeholder) — first impression

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kiteclass/kiteclass-frontend/src/app/(dashboard)/overview/page.tsx`

## Problem

Dashboard home (overview) — màn hình đầu tiên Owner thấy sau login — hiện:
- 4/6 KPI card là placeholder `—` (Giáo viên, Điểm danh hôm nay, Doanh thu tuần, Tỷ lệ giữ chân) vì chưa có `/dashboard/stats` endpoint; chỉ "Học viên" + "Khóa học" có số thật (derive từ list totalElements).
- Section "Hoạt động gần đây" CHỈ là 1 đoạn text giải thích tại sao có `—`, KHÔNG phải activity feed thật.

First impression yếu/trống cho tenant mới — trông như app chưa hoàn thiện. **Lưu ý:** đây là honest design (GAP-805 anti-fake-data — thà `—` còn hơn số giả) + đã track chờ stats endpoint, nên KHÔNG phải bug nghiêm trọng; finding là về UX completeness của màn hình quan trọng nhất.

## Root Cause

Chưa có `GET /api/v1/dashboard/stats` aggregate endpoint → 4 metric không có nguồn data; recent-activity feed chưa build.

## Proposed Fix

Khi backend ship `/dashboard/stats` (gap riêng nếu chưa có): swap 4 placeholder + subtitle sang real data. Ngắn hạn cân nhắc: (a) thay "—" placeholder bằng KPI card có CTA/onboarding hint (vd "Thêm giáo viên đầu tiên →"), (b) recent-activity placeholder → empty-state có icon + CTA thay vì đoạn text giải thích thuần.

## Acceptance Criteria

- [ ] Quyết định: build stats endpoint (link gap BE) HOẶC ngắn hạn cải thiện placeholder UX
- [ ] Màn overview không còn cảm giác "trống chưa hoàn thiện" — placeholder thành onboarding-actionable hoặc real data
- [ ] Recent-activity: empty-state UI thay text giải thích thuần

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P3)
- Source: `kiteclass/kiteclass-frontend/src/app/(dashboard)/overview/page.tsx:68-143`
- Prior: GAP-805 (anti-fake-data placeholder strategy)
