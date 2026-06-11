---
title: Demo-trio Seed Coverage Audit — landing-100 (academic data + image assets)
audience: mixed
audit_id: AUDIT-2026-06-11-demo-trio-seed-coverage
category: local-stack
phase: phase-1-beta
wave: landing-100
date: 2026-06-11
method: design-first code inspection (DESIGN thesis §4 → GAPS → DOCUMENTS → CODE)
verdict: ⚠️ PARTIAL — seed mới ở mức branding+landing-hero; thiếu academic data + 4 lỗi asset ảnh
score: 38/100
---

# Demo-trio Seed Coverage Audit — landing-100

**Ngày:** 2026-06-11
**Phương pháp:** Điều tra design-first per `design-first-investigation-order.md` — đọc DESIGN (thesis chương 4 §4.1-4.4 + `tenant-domain-landing-architecture.md`) → GAPS (`query-gaps.sh`) → DOCUMENTS (wave `landing-100` plan) → CODE cuối cùng (`BrandingDataSeeder.java`, migrations, onboarding).
**Phạm vi:** Mức độ seed data cho landing-100 demo-trio (Khánh / Hà / Nhì), trọng tâm academic data (điểm danh / điểm / học phí / lớp / học viên) + asset ảnh.
**Verdict:** ⚠️ **PARTIAL — CHƯA đạt "đẹp 100% + seed như thesis"** (38/100).

---

## 1. Yêu cầu (DESIGN — thesis §4.3 / §4.4)

| Tenant | Gói | Môn | Branding | Data nghiệp vụ thesis yêu cầu |
|---|---|---|---|---|
| **Cô Nguyễn Thị Hà** | Miễn phí | Toán tiểu học | Xanh dương, template (no AI) | Danh sách học viên · lịch buổi học · **điểm danh** (giao diện cơ bản) · **hóa đơn học phí thủ công** · đối soát chuyển khoản · giới hạn lớp/HV |
| **Thầy Nguyễn Đình Nhì** | Trả phí | Hóa THCS | Xanh lá, AI Branding | Quy mô lớn hơn · **bảng giá nhiều mức** · báo cáo doanh thu · **tỷ lệ điểm danh nâng cao** · không giới hạn lớp/HV |
| Cô Đỗ Lan Khánh | (walkthrough §4.1) | — | — | tenant minh hoạ luồng |

Wave `landing-100` plan §3 Bucket G hứa: *"Branding + lớp/GV/HS/học phí/điểm/điểm danh khớp Hình 4.1/4.3/4.4"*.

---

## 2. Hiện trạng (CODE = reality)

### 2.1 Nguồn seed đã truy hết

| Nguồn | File:line | Tạo gì |
|---|---|---|
| Java seeder | `kiteclass-core .../dev/seeder/BrandingDataSeeder.java:269-330` (`seedDemoTrio` / `seedTrioTenant`) | **CHỈ 3 thứ/tenant:** `FrontendInstance` (DEPLOYED) + `Branding` (màu/logo/zalo/fb) + `LandingPage` hero (title/subtitle/image/màu). KHÔNG academic. |
| SQL seed | `kiteclass-core/.../db/migration/V16__seed_test_data.sql:1` | **No-op** ("references tables not in kiteclass_shared") |
| Onboarding sample import | `OnboardingServiceImpl.java:77-121` | `1 teacher + 1 course + 1 class + 3 students + 3 enrollments` — **KHÔNG attendance, KHÔNG grade, KHÔNG invoice/payment**; chỉ chạy khi owner bấm import, **không auto** cho demo-trio |

### 2.2 Ma trận phủ data thực tế

| Data (thesis yêu cầu) | Hà | Nhì | Khánh | Nguồn |
|---|:--:|:--:|:--:|---|
| Branding + theme màu | ✅ | ✅ | ✅ | `BrandingDataSeeder` |
| Landing hero (title/subtitle/ảnh) | ✅ | ✅ | ✅ | `seedTrioTenant:205-213` |
| Teachers list / Pricing / Stats / Testimonials (landing sections) | ❌ | ❌ | ❌ | empty-state (Bucket A anti-fab ẩn) |
| Lớp / Học viên | ❌¹ | ❌¹ | ❌¹ | chỉ khi owner manual import (skeleton 1 lớp/3 HS) |
| **Điểm danh (attendance)** | ❌ | ❌ | ❌ | **không seeder nào tạo** |
| **Điểm (grades)** | ❌ | ❌ | ❌ | không seeder nào tạo |
| **Học phí (invoice/payment)** | ❌ | ❌ | ❌ | không seeder nào tạo |

¹ `grep "new Attendance|new Grade|new Student..."` trong context seed = rỗng; grep demo-trio slug trong academic code = rỗng.

→ **Plan Bucket G đóng `complete` nhưng scope academic thực tế khuyết.** Deviation chưa track (sister `wave-closure-scope-completeness` class).

### 2.3 Asset ảnh — 4 lỗi xác minh bằng code

| # | Lỗi | Bằng chứng |
|---|---|---|
| F1 | **5.4MB binary PNG đã push remote, trái rule user** | `demo-banners/{co-ha-toan,thay-nhi-hoa,co-khanh-phapluat}.png` git-tracked, commit `7e9d2cd` (PR #1969). `.gitignore:160` ghi "never commit binaries to remote" nhưng chỉ chặn `demo/`, **sót `demo-banners/`** |
| F2 | **Khánh hero 404 trên remote/clone mới** | `BrandingDataSeeder.java:314` `seedSkyLanding` set hero = `/demo/sky/teacher-do-lan-khanh.webp` → `git check-ignore` xác nhận **gitignored** → không có trên remote |
| F3 | **Banner 2MB dùng làm logo + logo≡hero** | dòng 247 `setLogoUrl(KHANH_BANNER_URL)` (2MB PNG làm logo); dòng ~203+211 Hà/Nhì `setLogoUrl(bannerUrl())` **≡** `setHeroImageUrl(bannerUrl())` (logo và hero chung 1 file) |
| F4 | **Format ngược plan Bucket C** | demo-banners = PNG 1672×941, 1.5–2MB (chưa webp / chưa `next/image`); bộ webp đúng chuẩn 24–80KB lại bị gitignore |

### 2.4 Resolution layer (đã track riêng — không trùng audit này)

`by-subdomain/{co-ha-toan,thay-nhi-hoa}` → **404** vì thiếu kitehub `instances` rows + UUID mismatch 2 scheme (`a1100000` vs `ad0fa96e`). Đã track ở **[[GAP-1180]]** (session song song, PR #2312). Là **prerequisite** cho seed academic resolve được.

---

## 3. Chấm điểm (38/100)

| Hạng mục | Điểm | Ghi chú |
|---|:--:|---|
| Branding + theme seed | 18/20 | 3 tenant đủ màu/tagline/contact |
| Landing hero seed | 10/15 | hero có; Khánh 404 remote (-5) |
| Landing sections (teachers/pricing/stats) | 0/15 | toàn empty-state |
| Academic core (lớp/HV) | 2/15 | chỉ skeleton qua manual import |
| **Điểm danh + điểm** | 0/15 | không seed |
| Học phí (invoice/payment) | 0/10 | không seed |
| Asset ảnh (format/tracking/path) | 3/10 | 4 lỗi F1-F4 |
| Tenant resolution (by-subdomain) | 5/0 (bonus, track GAP-1180) | manual INSERT, không durable |
| **Tổng** | **38/100** | ⚠️ PARTIAL FAIL (gate Phase 1 BETA ≥80) |

---

## 4. Gap candidates (reserve block 1190-1199 per `multi-session-concurrency-coordination`)

| Gap | P | Scope |
|---|---|---|
| GAP-1190 | P1 | Seed academic core 2 tenant (Hà FREE limited / Nhì PAID unlimited): courses + classes + students + enrollments + schedule/sessions |
| GAP-1191 | P1 | Seed **điểm danh (attendance)** records cho buổi học 2 tenant (Nhì có tỷ lệ điểm danh nâng cao) |
| GAP-1192 | P2 | Seed **điểm (grades)** theo grading_scale (V88) cho 2 tenant |
| GAP-1193 | P1 | Seed **học phí**: Hà hóa đơn thủ công + payment chuyển khoản; Nhì bảng giá nhiều mức + doanh thu |
| GAP-1194 | P2 | Seed landing sections data (teachers[] / pricing / stats) lấp empty-state — bỏ phụ thuộc onboarding manual |
| GAP-1195 | P1 | Fix asset ảnh: convert webp + `next/image`, gitignore `demo-banners/`, tách logo khỏi banner, sửa Khánh hero path (durable, không 404 remote) |

→ Gộp thành **Wave `demo-seed`** (xem `wave-2026-06-11-demo-seed-1-2tenant-full.md`).

---

## 5. Recommendations

1. **Prerequisite:** GAP-1180 (kitehub instances + UUID reconcile) PHẢI land trước → by-subdomain resolve → academic seed mới walk được G2.
2. Seed academic **idempotent** qua `BrandingDataSeeder` (mở rộng) HOẶC seeder mới `DemoAcademicSeeder` (`@Profile("dev")`) — KHÔNG hardcode SQL workaround (per GAP-1180 bài học manual-INSERT không durable).
3. Tôn trọng giới hạn gói: Hà FREE (giới hạn lớp/HV), Nhì PAID (unlimited) — seed đúng quota để demo đúng phân khúc.
4. Asset: chuyển sang **webp committed** (24–80KB an toàn remote) thay PNG 2MB; gỡ `demo-banners/*.png` khỏi git history nếu cần (per rule binary-không-lên-remote).
5. Re-walk G2★ nip.io subdomain sau seed (per `g1-browser-walk-before-flip` §3.2 production access-mode parity).

---

## Related
- DESIGN: `documents/08-thesis/chapter-4-deployment-results.md` §4.1-4.4 (Hình 4.1/4.3/4.4)
- DESIGN: `documents/02-architecture/tenant-domain-landing-architecture.md`
- Wave: `documents/03-planning/waves/wave-2026-06-09-landing-100.md` Bucket G
- Prerequisite gap: [[GAP-1180]] (kitehub instances seeder + UUID reconcile)
- Sibling: [[GAP-810]] (image assets, DONE — note "binary gitignored local-only"), [[GAP-826]] (multi-banner), [[GAP-815]] (landing content editor)
- Code: `kiteclass-core .../dev/seeder/BrandingDataSeeder.java`, `OnboardingServiceImpl.java`, `V16__seed_test_data.sql`
- Rule: `design-first-investigation-order`, `discovery-to-gap-inline-filing`, `wave-closure-scope-completeness`
