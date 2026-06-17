---
audience: mixed
created: 2026-05-23
phase: thesis-1
gap: GAP-652
status: ready-for-thesis-2-execution
---

# Demo cô lập multi-tenant — hướng dẫn 5 phút phụ trợ

> Demo phụ trợ chạy SAU luồng KiteHub chính trong buổi bảo vệ tốt nghiệp. Mục tiêu chứng minh **cô lập dữ liệu multi-tenant thực sự** — không phải slide, mà chạy trực tiếp trên 2 tenant + hiển thị cross-tenant 403 + cưỡng chế RLS GUC tại tầng DB.
>
> Theo GAP-652 (Wave thesis-1 Bucket F) thuộc đề tài "Khóa luận tốt nghiệp KiteHub", đây là điểm khác biệt duy nhất so với các khóa luận VN edu SaaS khác — chứng minh multi-tenant trực tiếp.

## Mục tiêu

Chứng minh 3 lớp cô lập cho hội đồng bảo vệ:

1. **Tầng UI/JWT** — Owner đăng nhập 2 tenant khác nhau, mỗi người chỉ thấy dữ liệu của trung tâm mình
2. **Tầng API** — request cross-tenant bị HTTP 403 từ chối + bản ghi audit log
3. **Tầng DB** — GUC PostgreSQL Row-Level Security `app.current_tenant_id` cưỡng chế tại session-local — 3 query × 3 kết quả khác nhau

## Phân bổ thời gian (5 phút × 5 pha)

| Pha | Khoảng thời gian | Hoạt động chính |
|---|---|---|
| Chuẩn bị | T-0:00 → T-0:30 (30 giây) | Chạy seed script, xác minh 2 tenant + 4 class + 4 student |
| Bước 1 | T-0:30 → T-2:00 (1.5 phút) | Đăng nhập Owner tenant A (Sky Education) — hiển thị lớp/học sinh |
| Bước 2 | T-2:00 → T-3:30 (1.5 phút) | Đăng xuất → đăng nhập Owner tenant B (Quang Minh) — hiển thị dữ liệu khác |
| Bước 3 | T-3:30 → T-4:30 (1 phút) | Chứng minh cross-tenant 403 qua curl |
| Bước 4 | T-4:30 → T-5:00 (30 giây) | Chứng minh RLS tại tầng DB (3 query × 3 kết quả) |

**Tổng: chính xác 5 phút** (30 + 90 + 90 + 60 + 30 = 300 giây).

## Tiền điều kiện (chuẩn bị trước bảo vệ)

Trước buổi bảo vệ, máy chạy demo bảo vệ PHẢI có:

- [ ] Docker Desktop đang chạy + stack KiteHub đã up qua `bash kitehub/scripts/up.sh --profile full`
- [ ] 8/8 service healthy (`docker ps | grep kite-` hiển thị: postgres + redis + rabbitmq + minio + kitehub-platform + kitehub-subscription + kitehub-frontend + kiteclass-core)
- [ ] Browser sẵn ở `http://localhost:3000` (FE KiteClass), `http://localhost:3001` (admin KiteHub)
- [ ] Terminal mở sẵn cd về repo root cho seed script + truy vấn DB

## Chuẩn bị (T-0:00 → T-0:30)

```bash
# Seed 2 demo tenants — idempotent, ~3-5 giây
bash scripts/seed-thesis-demo-tenants.sh
```

**Kết quả mong đợi**:

```
[HH:MM:SS] Mode: seed
[HH:MM:SS] Seed mode — creating tenant_a (Trung tâm Anh ngữ Sky Education) + tenant_b (Trung tâm Toán Quang Minh)
[HH:MM:SS] ...
 tenant_a classes  | 2
 tenant_a students | 2
 tenant_b classes  | 2
 tenant_b students | 2
[HH:MM:SS] ✓ tenant 11111111-1111-1111-1111-111111111111 has 2 classes (expected 2)
[HH:MM:SS] ✓ tenant 22222222-2222-2222-2222-222222222222 has 2 classes (expected 2)
[HH:MM:SS] Seed complete
```

Nói với hội đồng:

> "Em vừa seed 2 trung tâm demo: Sky Education + Quang Minh, mỗi trung tâm 2 lớp + 2 học sinh. Bây giờ em sẽ chứng minh data của 2 trung tâm hoàn toàn cách ly."

## Bước 1: Owner tenant A đăng nhập + xem lớp (T-0:30 → T-2:00, 1.5 phút)

**Thao tác**:

1. Mở browser tab 1 — URL `http://localhost:3000/login`
2. Đăng nhập với thông tin đăng nhập:
   - Email: `hong.tran@sky-edu.demo`
   - Mật khẩu: `<đặt qua phần mở rộng seed Wave thesis-2 hoặc tài khoản Owner test đã có>`
3. Click "Đăng nhập"

**UI mong đợi**:

- Dashboard hiển thị "Trung tâm Anh ngữ Sky Education" ở header
- Sidebar menu: Lớp học, Học sinh, Giáo viên, Báo cáo
- Click "Lớp học" → bảng hiển thị 2 dòng:
  - `Lớp Anh ngữ 5A1` — Giáo viên: Trần Thị Hồng — 1.500.000đ/khóa
  - `Lớp Anh ngữ 7B` — Giáo viên: Trần Thị Hồng — 1.500.000đ/khóa
- Click "Học sinh" → bảng hiển thị 2 dòng:
  - `Trần Thị Hồng` — student.hong.a@sky-edu.demo
  - `Nguyễn Văn An` — student.an.a@sky-edu.demo
- **KHÔNG thấy** bất kỳ lớp Toán nào, KHÔNG thấy `Phạm Thị Mai` / `Lê Văn Quang`

**Điểm nhấn cho hội đồng**:

> "Owner Sky Education chỉ thấy dữ liệu của trung tâm mình — 2 lớp Anh ngữ + 2 học sinh. Đây không phải bộ lọc UI — em sẽ chứng minh ngay sau đây bằng cách kiểm tra DB."

**Xác minh JWT (tùy chọn — nếu hội đồng yêu cầu)**:

Mở DevTools → tab Application → Cookies → sao chép giá trị JWT → dán vào https://jwt.io → hiển thị payload có claim `instanceId: "11111111-1111-1111-1111-111111111111"`.

## Bước 2: Đăng xuất + đăng nhập tenant B (T-2:00 → T-3:30, 1.5 phút)

**Thao tác**:

1. Click avatar góc phải → "Đăng xuất"
2. Đăng nhập với thông tin đăng nhập khác:
   - Email: `minh.le@quang-minh.demo`
   - Mật khẩu: `<đặt qua phần mở rộng seed Wave thesis-2>`
3. Click "Đăng nhập"

**UI mong đợi**:

- Dashboard hiển thị "Trung tâm Toán Quang Minh" ở header (đổi từ Sky Education)
- Click "Lớp học" → bảng hiển thị 2 dòng:
  - `Lớp Toán 9B` — Giáo viên: Lê Quang Minh — 1.800.000đ/khóa
  - `Lớp Toán 10A` — Giáo viên: Lê Quang Minh — 1.800.000đ/khóa
- Click "Học sinh" → bảng hiển thị 2 dòng:
  - `Phạm Thị Mai`
  - `Lê Văn Quang`
- **KHÔNG thấy** bất kỳ lớp Anh ngữ nào, KHÔNG thấy `Trần Thị Hồng` / `Nguyễn Văn An`

**Điểm nhấn**:

> "Khi đăng nhập với tài khoản Quang Minh, dashboard hiển thị dữ liệu hoàn toàn khác — 2 lớp Toán, 2 học sinh khác. Không có một dòng dữ liệu nào từ Sky Education lọt qua."

## Bước 3: Chứng minh cross-tenant 403 (T-3:30 → T-4:30, 1 phút)

Đây là điểm then chốt — chứng minh **bảo mật** không phải chỉ bộ lọc UI bề ngoài.

**Kịch bản** (kể với hội đồng):

> "Giả sử admin Quang Minh tình cờ biết class ID của Sky Education (qua shoulder surfing hoặc URL guessing). Họ thử dùng JWT của mình để truy cập class đó. Hệ thống sẽ phản ứng thế nào?"

**Thao tác** (trong terminal mới — chia màn hình với browser):

```bash
# Lấy JWT tenant B từ DevTools (copy từ browser tab 2 — đang login Quang Minh)
TENANT_B_JWT="<paste JWT của Owner Quang Minh ở đây>"

# Lấy class ID của tenant A (Sky Education) — giả định attacker đã biết
# Trong demo thật, dùng class ID hiển thị URL Sky Education ở Step 1
TENANT_A_CLASS_ID="<ID của Lớp Anh ngữ 5A1 — copy từ URL hoặc network tab Step 1>"

# Attempt cross-tenant request — expect 403
curl -i -H "Authorization: Bearer ${TENANT_B_JWT}" \
     "http://localhost:8080/api/v1/classes/${TENANT_A_CLASS_ID}"
```

**Response mong đợi**:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "error": "FORBIDDEN",
  "message": "Tenant mismatch — request blocked",
  "tenant_id_requesting": "22222222-2222-2222-2222-222222222222",
  "tenant_id_resource":   "11111111-1111-1111-1111-111111111111"
}
```

(Hoặc nếu kiteclass-gateway/JWT filter trả 404 theo ngữ nghĩa default-deny của RLS: 404 cũng chấp nhận được — vì RLS silent-deny, gateway thấy "không tồn tại" thay vì lộ sự tồn tại của resource.)

**Xác minh audit log** (tùy chọn — chạy SQL sau curl):

```bash
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -c \
  "SET row_security = off;
   SELECT created_at, user_id, action, resource_type, status_code, error_message
   FROM audit_log
   WHERE created_at > NOW() - INTERVAL '1 minute'
   ORDER BY created_at DESC LIMIT 1;"
```

**Điểm nhấn**:

> "Gateway + RLS phòng thủ kép: gateway so sánh `tenantId` trong JWT với `instance_id` của resource → 403 nếu không khớp. Đồng thời audit_log ghi nhận hành vi truy cập chéo tenant — về sau có thể dùng cho phát hiện mối đe dọa."

## Bước 4: Chứng minh RLS tại tầng DB (T-4:30 → T-5:00, 30 giây)

Đây là điểm khác biệt cuối — chứng minh cô lập **tại tầng DB**, không phải chỉ code ứng dụng.

**Thao tác** (terminal):

```bash
docker exec kite-postgres psql -U kitehub -d kiteclass_shared <<'EOF'
-- Query 1: Sky Education context
SET LOCAL app.current_tenant_id = '11111111-1111-1111-1111-111111111111';
SELECT count(*) AS sky_count FROM classes WHERE deleted = false;
-- Expect: 2

-- Query 2: Quang Minh context
SET LOCAL app.current_tenant_id = '22222222-2222-2222-2222-222222222222';
SELECT count(*) AS quang_minh_count FROM classes WHERE deleted = false;
-- Expect: 2 (KHÁC dataset)

-- Query 3: No tenant context (default-deny)
RESET app.current_tenant_id;
SELECT count(*) AS no_ctx_count FROM classes WHERE deleted = false;
-- Expect: 0 (RLS silent-deny khi GUC unset — per V58 migration)
EOF
```

**Kết quả mong đợi**:

```
 sky_count
-----------
         2

 quang_minh_count
-------------------
                 2

 no_ctx_count
--------------
            0
```

**Điểm nhấn (cao trào của demo)**:

> "3 query y hệt nhau, chỉ khác 1 GUC `app.current_tenant_id` — trả 3 kết quả khác nhau: 2, 2, 0. Đây không phải bộ lọc code ứng dụng mà là cưỡng chế policy PostgreSQL Row-Level Security.
>
> Ngay cả khi kẻ tấn công vượt qua được code ứng dụng (vd SQL injection), RLS vẫn từ chối query nếu không có GUC đúng. Ngữ nghĩa default-deny — không có GUC = không thấy dữ liệu, không phải thấy toàn bộ dữ liệu.
>
> Đây là chuẩn SaaS multi-tenant doanh nghiệp — Stripe, Shopify, Notion đều dùng mẫu tương tự. KiteHub là VN edu SaaS đầu tiên cài đặt áp dụng tại tầng DB."

## Bằng chứng dự phòng (bắt buộc chụp trước bảo vệ)

Nếu demo trực tiếp gặp sự cố (Docker down, trục trặc mạng, JWT hết hạn), dùng bản quay sẵn dự phòng:

### Yêu cầu chụp trước buổi bảo vệ

Trong 1 session diễn tập trước (≥ 1 ngày trước bảo vệ), chụp các bằng chứng sau:

| # | Bằng chứng | Đường dẫn | Cách chụp |
|---|---|---|---|
| 1 | UI Bước 1 — dashboard Sky Education 2 lớp Anh ngữ | `documents/08-thesis/defense/screenshots/multi-tenant-step1-sky-education-classes.png` | Browser toàn màn hình 1920×1080, locale `vi-VN` |
| 2 | UI Bước 2 — dashboard Quang Minh 2 lớp Toán | `documents/08-thesis/defense/screenshots/multi-tenant-step2-quang-minh-classes.png` | Browser toàn màn hình, locale `vi-VN` |
| 3 | Bước 3 — response curl cross-tenant 403 | `documents/08-thesis/defense/screenshots/multi-tenant-step3-cross-tenant-403.png` | Ảnh chụp màn hình terminal 1280×720 |
| 4 | Bước 4 — bảng 3 query × 3 kết quả | `documents/08-thesis/defense/screenshots/multi-tenant-step4-rls-guc-proof.png` | Ảnh chụp màn hình terminal |
| 5 | (Tùy chọn) Video quay sẵn đầy đủ 5 phút | `documents/08-thesis/defense/screenshots/multi-tenant-demo-full.mp4` | OBS Studio / trình quay màn hình kazam |

**Chuẩn độ phân giải**: Desktop 1440×900 hoặc 1920×1080. Locale browser = `vi-VN` (nhãn UI tiếng Việt).

### Yêu cầu chú thích

Mỗi ảnh chụp màn hình PHẢI có (hậu xử lý bằng GIMP / Figma):

- Mũi tên đỏ (`#dc2626`) chỉ vào phần tử quan trọng (vd "Tên trung tâm header", "Số lớp = 2")
- Viền vàng (`#facc15`) khoanh vùng cần chú ý
- Số bước (1, 2, 3...) đặt trên ảnh chụp màn hình tương ứng phần trình bày trong báo cáo

(Theo `user-manual-content-standard.md` §2 row 6 yêu cầu chú thích — áp dụng từ nay về sau cho tài liệu demo khóa luận.)

## Xử lý rủi ro + tình huống lỗi

| Rủi ro | Giảm thiểu |
|---|---|
| Docker stack chưa up | Chạy `bash kitehub/scripts/up.sh --profile full` 5 phút trước demo + xác minh 8/8 healthy |
| JWT hết hạn giữa demo | Đăng nhập lại nhanh; có video quay dự phòng sẵn |
| Mạng timeout khi curl cross-tenant | Curl đã kiểm thử trước trong diễn tập; có ảnh chụp màn hình 403 dự phòng |
| Lỗi cú pháp GUC Postgres | Đã kiểm thử trước khối 3 query trong diễn tập; copy-paste từ kịch bản này |
| Hội đồng yêu cầu reset demo | `bash scripts/seed-thesis-demo-tenants.sh --cleanup` rồi `seed` lại (~5s tổng) |

## Ngoài phạm vi (hoãn Wave thesis-2)

Phần này nằm trong phạm vi GAP-652 nhưng bàn giao ở Wave thesis-2 (sau khi khôi phục tài khoản AWS theo GAP-612):

- **Thực thi trực tiếp trên production AWS RDS** — script đã sẵn sàng, nhưng seed DB runtime bắt buộc có sự cho phép của con người + kiểm toán trước khi mutate theo `pre-mutation-state-check.md` §3
- **Chụp ảnh màn hình thật** — đường dẫn placeholder trong thư mục `screenshots/`; việc chụp thực tế hoãn sang Wave thesis-2 sau khi UI hoàn thiện ổn định
- **Quay video dự phòng** — kịch bản sẵn sàng cho việc quay diễn tập; ship Wave thesis-2 hoặc session trước bảo vệ
- **Đặt mật khẩu Owner** trong seed script — hiện script seed instance + students + classes nhưng không tạo dòng Owner User với hash mật khẩu (tích hợp bảng FrontendInstance + User cần join platform.users). Hoãn phần mở rộng seed mật khẩu sang Wave thesis-2 hoặc dùng các tài khoản test sẵn có của Sky Education / Quang Minh đã có trong env.

## Tiêu chí nghiệm thu (đóng Wave thesis-1 Bucket F)

- [x] `scripts/seed-thesis-demo-tenants.sh` đã tạo + đặt bit thực thi
- [x] Script xử lý 3 chế độ: mặc định (seed), `--dry-run`, `--cleanup`
- [x] Script idempotent (chạy lại an toàn qua `ON CONFLICT DO NOTHING` + mẫu cleanup-before-seed)
- [x] Smoke dry-run Docker local PASS: `bash scripts/seed-thesis-demo-tenants.sh --dry-run` → thoát 0 + in ra các lệnh SQL/API dự kiến
- [x] `documents/08-thesis/defense/multi-tenant-demo-script.md` đã tạo — 5 pha × tổng 5 phút + chứng minh cross-tenant 403 + chứng minh lớp RLS + lệnh chụp bằng chứng dự phòng
- [x] Không mutate database thực tế tại thời điểm CI (chỉ bàn giao script)
- [x] ShellCheck PASS trên `seed-thesis-demo-tenants.sh`

## Tài liệu liên quan

- `scripts/seed-thesis-demo-tenants.sh` — cài đặt seed
- `documents/08-thesis/chapter-2-system-architecture.md` — phần kiến trúc multi-tenant
- `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql` — migration RLS (GAP-466 / Wave 56)
- `kiteclass/kiteclass-core/src/main/resources/db/migration/V59__rls_admin_bypass_and_null_force_fail.sql` — làm cứng RLS (Wave 85 Cat 3 +2 A01 NULL force-fail)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java` — `SET LOCAL app.current_tenant_id` mỗi `@Transactional`
- `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSEnforcementIT.java` — integration test RLS tự động (chứng minh song song)
- `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSHardeningIT.java` — test admin bypass + NULL force-fail
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-652-thesis-multi-tenant-isolation-demo.md` — đóng gap DONE 2026-05-23

## Log

- **2026-05-23**: Created — Wave thesis-1 Bucket F closes GAP-652 script-only mode. Runtime execution + screenshot capture defer Wave thesis-2 hậu GAP-612 AWS restore.
