# GAP-1205: F-section defaults hiển thị copy marketing PLATFORM trên landing TENANT — sai audience + vi phạm hide-when-empty

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (landing-100 UI review full-page — cả 3 tenant demo-trio)
**Affects:** `kiteclass/kiteclass-frontend/src/components/sections/{ProblemSolutionSection,HowItWorksSection,FeaturesSection?,EnrollmentSection}` DEFAULT slot data + render-when-empty logic

## Problem

Full-page review 3 tenant (co-ha-toan / thay-nhi-hoa / sky-education) qua nip.io: các section port từ design-kit (Bucket F) render **copy mặc định của marketing-site PLATFORM** khi tenant không có slot data:

- ProblemSolution: "Vận hành trung tâm không còn vật vã đến vậy... đang xoay xở giữa Excel, Zalo và sổ giấy — đây là những việc nền tảng gỡ hộ" — copy bán KiteClass CHO chủ trung tâm, hiển thị trên landing của **gia sư cá nhân** mà audience là **phụ huynh/học viên**.
- Tính năng nổi bật (sky): "Hệ thống LMS / Quản lý Học viên / Thanh toán & Báo cáo" — feature platform, không phải nội dung trường.
- Tuyển sinh steps: "Đăng ký tài khoản → ... → Bắt đầu học" — onboarding platform.
- HowItWorks ("Bắt đầu trong ba bước"): render header + subtitle nhưng **không thấy 3 step cards** (header-only, section cụt).

Nguồn gốc: Bucket F port design kit `marketing-site` (vốn là trang bán platform) → DEFAULT slot copy đi theo nguyên văn. Vi phạm nguyên tắc Bucket A anti-fabrication (section không có data tenant → ẨN, không render copy không thuộc tenant) + làm landing đọc vô nghĩa với khách của trường → chặn mục tiêu rubric ≥90/100.

## Proposed Fix

1. F-sections (ProblemSolution / HowItWorks / TrustStrip / Features / Enrollment-marketing) **hide-when-empty** giống 6 sections Bucket A — chỉ render khi BE trả slot data per-tenant (GAP-1083 đã ship BE fields).
2. Nếu giữ default cho demo: viết default copy theo audience học viên/phụ huynh (generic giáo dục), không phải platform pitch.
3. Điều tra HowItWorks header-only render (DEFAULT_STEPS có nhưng cards không hiện).
4. Seed demo-trio F-section slot data thật (khớp thesis Hình 4.x) để demo đủ section.

## Acceptance Criteria

- [ ] Landing tenant không có F-data → không còn câu "Vận hành trung tâm..." / "Hệ thống LMS" / "Đăng ký tài khoản"
- [ ] Không section nào render header-only (cụt body)
- [ ] Demo-trio render F-sections với data seeded per-tenant
- [ ] Screenshot before/after + re-score rubric landing

## Related

- Discovered in: landing-100 G2★ UI review (PR #2326 session); screenshots /tmp/ui-*-desktop.png
- Sister: GAP-958 (anti-fabrication 6 sections — Bucket A), GAP-1083 (BE F-section fields — shipped), GAP-828/595/596 (Bucket F port)
- Blocker cho: wave landing-100 closure re-score ≥90/100
