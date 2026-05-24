---
title: RST Đợt 107 — Mảng A (Khách ẩn danh) + Mảng B-onboard (Chủ trung tâm) walk
date: 2026-05-23
phase: phase-1-beta
wave: 107
audience: dev
audits: [persona-review]
---

# RST Đợt 107 — Mảng A + Mảng B-onboard

**Mục tiêu:** Walk 3 luồng anonymous KH frontend (localhost:3001) + 4 luồng owner-onboard KC frontend (localhost:3000) trong khi 3 agent song song fix cụm thư (FIX-543 / FIX-657 / FIX-659).

**Tiền điều kiện:** Docker stack 13 dịch vụ healthy + owner.test@test.vn / Test@1234 đã seed Đợt 105 + MailHog tại localhost:8025.

**Playwright specs:**
- `kitehub/kitehub-frontend/e2e/_rst-wave-107-anonymous.spec.ts` (3 tests Mảng A)
- `kiteclass/kiteclass-frontend/e2e/_rst-wave-107-owner-onboard.spec.ts` (4 tests Mảng B-onboard)

**Ảnh chụp:** `/tmp/rst-screenshots/wave-107/` (16 ảnh: 3 A + 13 B).

---

## Bảng thông/vỡ

| Luồng | URL | Trạng thái | Phán quyết |
|---|---|:---:|---|
| **A1** Land trang chủ KH | `localhost:3001/` | 🟢 PASS | Title VN `KiteHub - Nền tảng quản lý trung tâm giáo dục`; H1 `Quản lý trung tâm giáo dục thông minh hơn`; `<html lang="vi">`; nav 3 link VN `Bảng giá / Đăng nhập / Dùng thử miễn phí`; 4 CTA conversion. |
| **A2** Trang bảng giá KH | `localhost:3001/pricing` | 🟢 PASS | Định dạng VND `500.000₫/tháng` đúng per `vn-localization-audit-checklist.md` §2; KHÔNG có ký tự USD; 3+ gói tiers (FREE / BASIC / ...); narrative VN chuẩn (`Chọn gói phù hợp với quy mô trung tâm`). **Lưu ý:** `waitUntil: 'networkidle'` timeout 30s — page có long-tail network activity (analytics/Vercel beacons); dùng `domcontentloaded` thay thế. |
| **A3** Trạng thái Beta KH | `localhost:3001/beta-status` | 🟢 PASS | Title + H1 VN `Trạng thái Beta KiteHub`; date format VN `Thứ Hai, 18/05/2026`; 2 request CTA cho beta funnel; **trạng thái BE không tải được** (`Không tải được nội dung trạng thái BE`) — expected do AWS phục hồi GAP-612 còn chặn `kitehub.me` API. FE graceful fallback hiển thị changelog tĩnh. |
| **B1** Đăng nhập Chủ trung tâm KC | `localhost:3000/login` | 🟢 PASS | `POST /auth/login` 200; redirect `/dashboard` REDIRECTED_OK. PR #1737 Đợt 105 đã sửa 5 bug đăng nhập, smoke pass lại confirm regression-free. |
| **B2** Trợ lý cài đặt ban đầu | `localhost:3000/branding/wizard` | 🔴 **FAIL** | **Body trống** (no h1, no input). Cũng có SSR log `Failed to fetch landing page data: ECONNREFUSED 127.0.0.1:8080`. Đã file **GAP-726** P1 — defer Đợt 108. |
| **B3** Chọn trung tâm (1 tenant) | `localhost:3000/dashboard` | 🟢 PASS | Sau đăng nhập, KHÔNG hiển thị picker (`B3_PICKER_VISIBLE: 0`) — đúng hành vi 1-tenant default skip. owner.test có 1 trung tâm seeded. |
| **B4** Bảng điều khiển nav probe | 9 routes | 🟢 PASS | 9/9 routes render với H1 VN đúng: Tổng quan / AI Branding / Học viên / Giáo viên / Lớp học / Khóa học / Điểm danh / Hóa đơn / Cài đặt. 0 lỗi 404, 0 trang trắng. |

**Tóm tắt:** 6/7 luồng thông; 1 luồng vỡ (B2 wizard render trắng — đã file GAP-726). Tỷ lệ 86%.

---

## Lỗi mới phát hiện (file GAP để Đợt 108 phân loại)

| Gap | Mức | Vai trò chặn | File |
|---|---|---|---|
| **GAP-726** | 🟠 P1 | Chủ trung tâm (Mảng B-onboard B2) | `documents/04-quality/gaps/phase-1-beta/GAP-726-kc-branding-wizard-blank-render-econnrefused-8080.md` |

Workaround sẵn có: vào `/branding` standalone (đã verify B4 render OK) để cấu hình thương hiệu thủ công thay vì wizard. Chặn UX nhưng không chặn năng lực — vì vậy file GAP defer thay vì sửa tại chỗ.

---

## Bug chặn luồng — sửa tại chỗ trong Đợt 107?

**Không.** B2 là bug duy nhất; nó KHÔNG chặn luồng B1/B3/B4 (3 luồng còn lại) và có workaround `/branding` standalone. Theo plan §1 Brainstorm Q3 → file GAP mới + defer Đợt 108. Tránh drift scope email-fix wave thành full-stack fix wave.

---

## Quan sát ngoài bug

1. **VND format chuẩn:** `500.000₫/tháng` trên `/pricing` đúng `vn-localization-audit-checklist.md` §2. Không có dấu hiệu USD/$ leak.
2. **VN locale chuẩn:** `<html lang="vi">` khắp KH + KC frontend. Title + H1 + nav 100% VN.
3. **`waitUntil: 'networkidle'` không phù hợp** cho `/pricing` (long-tail analytics beacons). Đã chuyển sang `domcontentloaded` cho Đợt 107; ghi nhận để spec sau ưu tiên `domcontentloaded` mặc định.
4. **AWS GAP-612 fingerprint:** A3 `/beta-status` hiển thị "Không tải được nội dung trạng thái BE" → confirm BE `kitehub.me` API endpoint vẫn down. FE graceful fallback ổn.
5. **KC frontend `(public)/staff/accept-invite`** route exist nhưng chưa walk Đợt 107 (cần claim code thực). Defer khi có invite flow integration test.
6. **KC playwright config** chạy multi-browser (chromium/firefox/webkit/Mobile) — phần lớn browser binaries chưa cài → 14 fail "non-issue" trên non-chromium. Dùng `--project=chromium` để chỉ chạy chromium cho RST local.

---

## Khớp với plan §7.1 closure protocol

- ✅ Playwright spec 2 files (Mảng A + Mảng B-onboard)
- ✅ Ảnh chụp `/tmp/rst-screenshots/wave-107/` 16 ảnh
- ✅ Báo cáo persona-review (file này)
- ✅ 1 lỗi không chặn → file GAP-726 ưu tiên Đợt 108
- ❌ KHÔNG có lỗi chặn luồng → KHÔNG sửa tại chỗ

---

## Đầu vào cho Đợt 108

| Mảng | Trạng thái sau Đợt 107 |
|---|---|
| A (Khách ẩn danh A1-A3) | ✅ 3/3 PASS Đợt 107 |
| B-onboard (B1-B4) | ⚠️ 3/4 PASS — B2 defer GAP-726 |
| B-CRUD (B5-B8) | ❌ Chưa walk — cần seed dữ liệu nền |
| B-vận-hành (B9-B13) | ❌ Chưa walk |
| C (Nhân viên) | ❌ Chưa walk — cần B13 chạy trước |
| D (Quản trị) | ⚠️ D1+D2 đã thông Đợt 105; D3+D4 chưa walk |

Đề nghị Đợt 108 ưu tiên: fix GAP-726 (B2 wizard) + walk B-CRUD nếu có thời gian seed dữ liệu nền.

---

## Cross-link

- Plan: `documents/03-planning/waves/wave-2026-05-23-107-hybrid-rst-anonymous-onboard-plus-email-fix.md`
- Plan Đợt 106 đầy đủ (defer chờ AWS): `documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`
- Đợt 105 baseline RST (đăng nhập 5 bug fix): PR #1737
- GAP-612 AWS phục hồi (chặn live verify): `documents/04-quality/gaps/`
- Cụm thư song song (3 agent): PR #1744 (FIX-543) + PR #1745 (FIX-659) + PR #1746 (FIX-657, pending)
