---
audience: dev
---

# Runbook — Seed demo-trio (Hà + Nhì) lên Production y hệt dev

**Mục đích:** tái tạo CHÍNH XÁC 2 tenant demo canonical trên production giống hệt môi
trường dev — phục vụ demo thesis / khách hàng. Scheme canonical = **Scheme B** (chốt
2026-06-18): `co-ha-toan` + `thay-nhi-hoa` (KHÔNG dùng scheme A `ha-toantieuhoc`/`nhi-hoathcs`).

## Nguồn seed canonical (3 Java seeder idempotent, upsert mỗi boot)

| Thứ tự | Seeder | Service | Seed gì |
|---|---|---|---|
| 1 | `DemoTrioInstanceSeeder` | kitehub-subscription | `instances` rows (co-ha-toan = `a1100000-…0001`, thay-nhi-hoa = `b1100000-…0002`) — UUID cố định |
| 2 | `BrandingDataSeeder` | kiteclass-core | Landing content: theme màu + hero `.webp` + programs/teachers/pricing/stats (academic-accurate) |
| 3 | `DemoAcademicSeeder` | kiteclass-core | Academic chain: teacher→course→class→session→student→enroll→attendance→grade→invoice→payment. **Hà 12HV / Nhì 35HV** |

Cả 3 trước đây `@Profile("dev")` → chỉ chạy local. Đã đổi thành **`@Profile({"dev", "demo-seed"})`**:
- `dev` (local) → chạy mỗi boot như cũ (không đổi hành vi local).
- `demo-seed` (opt-in) → kích hoạt trên BẤT KỲ env (kể cả production) để seed.

## Quy trình seed lên production

> ⚠️ Production-mutation — chỉ dev/owner trigger. Seeders idempotent (upsert) nên re-run an toàn.

1. **Backup DB production trước** (RDS snapshot) — per `pre-mutation-state-check.md`.
2. **Deploy `kitehub-subscription` với profile `demo-seed`** (instances PHẢI có trước):
   ```
   SPRING_PROFILES_ACTIVE=prod,demo-seed
   ```
   Boot → `DemoTrioInstanceSeeder` tạo 2 instances (UUID cố định a1100000/b1100000).
3. **Deploy `kiteclass-core` với profile `demo-seed`** (sau khi instances tồn tại):
   ```
   SPRING_PROFILES_ACTIVE=prod,demo-seed
   ```
   Boot → `BrandingDataSeeder` (landing) + `DemoAcademicSeeder` (academic) chạy `ApplicationReadyEvent`.
4. **Verify** (production-accurate, qua gateway):
   - Landing: `GET https://co-ha-toan.kitehub.me/` + `https://thay-nhi-hoa.kitehub.me/` → render theme + hero + programs.
   - Academic count: co-ha-toan = 12 students, thay-nhi-hoa = 35 students (khớp dev).
   - Catalog: `/catalog` mỗi tenant → khóa học PUBLISHED + filter "Cấp lớp" động đúng cấp.
5. **TẮT profile `demo-seed` sau khi seed xong** (re-deploy chỉ `prod`) — tránh chạy lại mỗi boot trên prod. Seed đã persist trong DB.

## Lưu ý

- **Idempotent:** seeders upsert theo key cố định → re-run không nhân đôi. Nếu bước 3 boot trước khi instances kịp tạo (race), re-deploy kiteclass-core lần nữa.
- **UUID cố định** đảm bảo "y hệt" cross-env (dev a1100000/b1100000 = prod a1100000/b1100000).
- **FE fix global** (theme/banner/catalog/reco — session 2026-06-18) áp cho cả 2 tenant tự động qua kiteclass-frontend đã deploy.
- **Scheme A** (`ha-toantieuhoc`/`nhi-hoathcs`/`khanh-phapluat`, bash `seed-demo-independent-teachers.sh`) = bộ cũ trùng lặp — KHÔNG seed lên prod. Cleanup dev riêng (tùy chọn).
- **Khánh** (tenant chính thesis Chương 3): hiện `DemoAcademicSeeder` CHƯA seed academic cho Khánh (chỉ banner trong BrandingDataSeeder). Nếu cần Khánh đầy đủ → mở rộng `DemoAcademicSeeder` thêm `khanhSpec()` (việc riêng, ngoài scope "2 tenant").

## Tham chiếu
- Quyết định canonical: GAP-1196 (deprecate `seed-landing-content.sql`).
- `pre-mutation-state-check.md` (backup trước mutation prod).
- `release-deploy-standard.md` (deploy discipline).
