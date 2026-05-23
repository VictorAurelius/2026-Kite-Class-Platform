---
audience: mixed
created: 2026-05-23
phase: thesis-1
gap: GAP-652
status: ready-for-thesis-2-execution
---

# Multi-tenant Isolation Demo — 5 phút walkthrough phụ trợ

> Demo phụ trợ chạy SAU primary KiteHub flow trong buổi bảo vệ tốt nghiệp. Mục tiêu chứng minh **multi-tenant data isolation thực sự** — không phải slide, mà chạy live trên 2 tenant + hiển thị cross-tenant 403 + RLS GUC enforcement tại DB layer.
>
> Theo GAP-652 (Wave thesis-1 Bucket F) thuộc đề tài "Khóa luận tốt nghiệp KiteHub", đây là điểm khác biệt duy nhất so với các thesis VN edu SaaS khác — multi-tenant proof live.

## Mục tiêu

Chứng minh 3 lớp isolation cho hội đồng bảo vệ:

1. **UI/JWT layer** — Owner đăng nhập 2 tenant khác nhau, mỗi người chỉ thấy data của trung tâm mình
2. **API layer** — cross-tenant request bị HTTP 403 reject + audit log entry
3. **DB layer** — PostgreSQL Row-Level Security GUC `app.current_tenant_id` enforces tại session-local — 3 query × 3 kết quả khác nhau

## Timing breakdown (5 phút × 5 phase)

| Phase | Khoảng thời gian | Hoạt động chính |
|---|---|---|
| Setup | T-0:00 → T-0:30 (30 giây) | Chạy seed script, verify 2 tenant + 4 class + 4 student |
| Step 1 | T-0:30 → T-2:00 (1.5 phút) | Login Owner tenant A (Sky Education) — show classes/students |
| Step 2 | T-2:00 → T-3:30 (1.5 phút) | Logout → login Owner tenant B (Quang Minh) — show data khác |
| Step 3 | T-3:30 → T-4:30 (1 phút) | Cross-tenant 403 proof via curl |
| Step 4 | T-4:30 → T-5:00 (30 giây) | RLS proof tại DB layer (3 query × 3 result) |

**Total: 5 phút chính xác** (30 + 90 + 90 + 60 + 30 = 300 giây).

## Tiền điều kiện (pre-defense setup)

Trước buổi bảo vệ, host máy bảo vệ PHẢI có:

- [ ] Docker Desktop running + KiteHub stack up qua `bash kitehub/scripts/up.sh --profile full`
- [ ] 8/8 services healthy (`docker ps | grep kite-` shows: postgres + redis + rabbitmq + minio + kitehub-platform + kitehub-subscription + kitehub-frontend + kiteclass-core)
- [ ] Browser sẵn ở `http://localhost:3000` (KiteClass FE), `http://localhost:3001` (KiteHub admin)
- [ ] Terminal mở sẵn cd về repo root cho seed script + DB queries

## Setup (T-0:00 → T-0:30)

```bash
# Seed 2 demo tenants — idempotent, ~3-5 giây
bash scripts/seed-thesis-demo-tenants.sh
```

**Expected output**:

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

## Step 1: Owner tenant A login + xem lớp (T-0:30 → T-2:00, 1.5 phút)

**Action**:

1. Mở browser tab 1 — URL `http://localhost:3000/login`
2. Login với credential:
   - Email: `hong.tran@sky-edu.demo`
   - Password: `<set qua seed extension Wave thesis-2 hoặc Owner test account đã có>`
3. Click "Đăng nhập"

**Expected UI**:

- Dashboard hiển thị "Trung tâm Anh ngữ Sky Education" ở header
- Sidebar menu: Lớp học, Học sinh, Giáo viên, Báo cáo
- Click "Lớp học" → bảng hiển thị 2 dòng:
  - `Lớp Anh ngữ 5A1` — Giáo viên: Trần Thị Hồng — 1.500.000đ/khóa
  - `Lớp Anh ngữ 7B` — Giáo viên: Trần Thị Hồng — 1.500.000đ/khóa
- Click "Học sinh" → bảng hiển thị 2 dòng:
  - `Trần Thị Hồng` — student.hong.a@sky-edu.demo
  - `Nguyễn Văn An` — student.an.a@sky-edu.demo
- **KHÔNG thấy** bất kỳ lớp Toán nào, KHÔNG thấy `Phạm Thị Mai` / `Lê Văn Quang`

**Highlight cho hội đồng**:

> "Owner Sky Education chỉ thấy data của trung tâm mình — 2 lớp Anh ngữ + 2 học sinh. Đây không phải UI filter — em sẽ chứng minh ngay sau đây bằng cách check DB."

**Verify JWT (tùy chọn — nếu hội đồng yêu cầu)**:

Mở DevTools → Application tab → Cookies → copy giá trị JWT → paste vào https://jwt.io → show payload có claim `instanceId: "11111111-1111-1111-1111-111111111111"`.

## Step 2: Logout + login tenant B (T-2:00 → T-3:30, 1.5 phút)

**Action**:

1. Click avatar góc phải → "Đăng xuất"
2. Login với credential khác:
   - Email: `minh.le@quang-minh.demo`
   - Password: `<set qua seed extension Wave thesis-2>`
3. Click "Đăng nhập"

**Expected UI**:

- Dashboard hiển thị "Trung tâm Toán Quang Minh" ở header (đổi từ Sky Education)
- Click "Lớp học" → bảng hiển thị 2 dòng:
  - `Lớp Toán 9B` — Giáo viên: Lê Quang Minh — 1.800.000đ/khóa
  - `Lớp Toán 10A` — Giáo viên: Lê Quang Minh — 1.800.000đ/khóa
- Click "Học sinh" → bảng hiển thị 2 dòng:
  - `Phạm Thị Mai`
  - `Lê Văn Quang`
- **KHÔNG thấy** bất kỳ lớp Anh ngữ nào, KHÔNG thấy `Trần Thị Hồng` / `Nguyễn Văn An`

**Highlight**:

> "Khi đăng nhập với tài khoản Quang Minh, dashboard hiển thị data hoàn toàn khác — 2 lớp Toán, 2 học sinh khác. Không có một dòng data nào từ Sky Education leak qua."

## Step 3: Cross-tenant 403 proof (T-3:30 → T-4:30, 1 phút)

Đây là điểm critical — chứng minh **security** không phải chỉ UI cosmetic filter.

**Scenario** (kể với hội đồng):

> "Giả sử admin Quang Minh tình cờ biết class ID của Sky Education (qua shoulder surfing hoặc URL guessing). Họ thử dùng JWT của mình để truy cập class đó. Hệ thống sẽ phản ứng thế nào?"

**Action** (trong terminal mới — split screen với browser):

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

**Expected response**:

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

(Hoặc nếu kiteclass-gateway/JWT filter trả 404 default-deny semantics theo RLS: 404 cũng acceptable — vì RLS silent-deny, gateway thấy "không tồn tại" thay vì leak existence của resource.)

**Verify audit log** (tùy chọn — chạy SQL sau curl):

```bash
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -c \
  "SET row_security = off;
   SELECT created_at, user_id, action, resource_type, status_code, error_message
   FROM audit_log
   WHERE created_at > NOW() - INTERVAL '1 minute'
   ORDER BY created_at DESC LIMIT 1;"
```

**Highlight**:

> "Gateway + RLS double-defense: gateway compare `tenantId` trong JWT với `instance_id` của resource → 403 nếu mismatch. Đồng thời audit_log ghi nhận attempted cross-tenant access — về sau có thể dùng cho threat detection."

## Step 4: RLS proof tại DB layer (T-4:30 → T-5:00, 30 giây)

Đây là điểm khác biệt cuối — chứng minh isolation **tại DB layer**, không phải chỉ app code.

**Action** (terminal):

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

**Expected output**:

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

**Highlight (climax của demo)**:

> "3 query y hệt nhau, chỉ khác 1 GUC `app.current_tenant_id` — trả 3 kết quả khác nhau: 2, 2, 0. Đây không phải app code filter mà là PostgreSQL Row-Level Security policy enforcement.
>
> Ngay cả khi attacker bypass được app code (vd SQL injection), RLS vẫn từ chối query nếu không có GUC đúng. Default-deny semantic — không có GUC = không thấy data, không phải seeing all data.
>
> Đây là chuẩn enterprise multi-tenant SaaS — Stripe, Shopify, Notion đều dùng pattern tương tự. KiteHub là implementation VN edu SaaS đầu tiên áp dụng tại DB layer."

## Backup evidence (pre-defense capture mandate)

Nếu live demo gặp sự cố (Docker down, network glitch, JWT expired), dùng pre-recorded fallback:

### Yêu cầu capture trước buổi bảo vệ

Trong 1 session pre-rehearsal (≥ 1 ngày trước defense), capture các evidence sau:

| # | Evidence | Đường dẫn | Cách chụp |
|---|---|---|---|
| 1 | Step 1 UI — Sky Education dashboard 2 lớp Anh ngữ | `documents/08-thesis/defense/screenshots/multi-tenant-step1-sky-education-classes.png` | Browser fullscreen 1920×1080, locale `vi-VN` |
| 2 | Step 2 UI — Quang Minh dashboard 2 lớp Toán | `documents/08-thesis/defense/screenshots/multi-tenant-step2-quang-minh-classes.png` | Browser fullscreen, locale `vi-VN` |
| 3 | Step 3 — cross-tenant curl response 403 | `documents/08-thesis/defense/screenshots/multi-tenant-step3-cross-tenant-403.png` | Terminal screenshot 1280×720 |
| 4 | Step 4 — 3-query × 3-result table | `documents/08-thesis/defense/screenshots/multi-tenant-step4-rls-guc-proof.png` | Terminal screenshot |
| 5 | (Optional) Pre-recorded full 5-phút video | `documents/08-thesis/defense/screenshots/multi-tenant-demo-full.mp4` | OBS Studio / kazam screen recorder |

**Resolution standard**: Desktop 1440×900 hoặc 1920×1080. Locale browser = `vi-VN` (Vietnamese UI labels).

### Annotation requirements

Mỗi screenshot PHẢI có (post-process bằng GIMP / Figma):

- Mũi tên đỏ (`#dc2626`) chỉ vào element quan trọng (vd "Tên trung tâm header", "Số lớp = 2")
- Viền vàng (`#facc15`) khoanh vùng cần chú ý
- Số bước (1, 2, 3...) đặt trên screenshot tương ứng narrative trong báo cáo

(Per `user-manual-content-standard.md` §2 row 6 annotation requirements — apply prospectively cho thesis demo materials.)

## Risk + failure-mode handling

| Risk | Mitigation |
|---|---|
| Docker stack chưa up | Run `bash kitehub/scripts/up.sh --profile full` 5 phút trước demo + verify 8/8 healthy |
| JWT expired giữa demo | Re-login nhanh; có backup recorded video sẵn |
| Network timeout cross-tenant curl | Curl pre-tested trong rehearsal; có screenshot 403 fallback |
| Postgres GUC syntax error | Pre-tested 3-query block trong rehearsal; copy-paste từ script này |
| Hội đồng yêu cầu reset demo | `bash scripts/seed-thesis-demo-tenants.sh --cleanup` rồi `seed` lại (~5s tổng) |

## Out-of-scope (defer Wave thesis-2)

Phần này nằm trong scope GAP-652 nhưng deliver Wave thesis-2 (post AWS account restore per GAP-612):

- **Live execution trên production AWS RDS** — script ready, nhưng runtime DB seed mandate human authorization + pre-mutation audit per `pre-mutation-state-check.md` §3
- **Real screenshot capture** — placeholder paths trong `screenshots/` folder; actual capture defer Wave thesis-2 sau khi UI polish stable
- **Backup video recording** — script sẵn-sàng cho rehearsal recording; ship Wave thesis-2 hoặc pre-defense session
- **Owner password set** trong seed script — hiện script seed instance + students + classes nhưng không tạo Owner User row với password hash (FrontendInstance + User table integration require platform.users join). Defer Wave thesis-2 password seed extension hoặc dùng existing test accounts của Sky Education / Quang Minh đã có trong env.

## Acceptance criteria (Wave thesis-1 Bucket F closure)

- [x] `scripts/seed-thesis-demo-tenants.sh` created + executable bit set
- [x] Script handles 3 modes: default (seed), `--dry-run`, `--cleanup`
- [x] Script idempotent (re-run safe via `ON CONFLICT DO NOTHING` + cleanup-before-seed pattern)
- [x] Local Docker dry-run smoke PASS: `bash scripts/seed-thesis-demo-tenants.sh --dry-run` → exits 0 + prints intended SQL/API calls
- [x] `documents/08-thesis/defense/multi-tenant-demo-script.md` created — 5 phase × tổng 5 phút + cross-tenant 403 proof + RLS layer proof + backup evidence capture commands
- [x] No actual database mutation tại CI time (script-only delivery)
- [x] ShellCheck PASS on `seed-thesis-demo-tenants.sh`

## Related artifacts

- `scripts/seed-thesis-demo-tenants.sh` — seed implementation
- `documents/08-thesis/chapter-2-system-architecture.md` — multi-tenant architecture section
- `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql` — RLS migration (GAP-466 / Wave 56)
- `kiteclass/kiteclass-core/src/main/resources/db/migration/V59__rls_admin_bypass_and_null_force_fail.sql` — RLS hardening (Wave 85 Cat 3 +2 A01 NULL force-fail)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/datasource/TenantAwareDataSourceInterceptor.java` — `SET LOCAL app.current_tenant_id` per `@Transactional`
- `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSEnforcementIT.java` — automated RLS integration test (parallel proof)
- `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/common/datasource/RLSHardeningIT.java` — admin bypass + NULL force-fail tests
- `documents/04-quality/gaps/phase-1-beta/closed/GAP-652-thesis-multi-tenant-isolation-demo.md` — gap closure DONE 2026-05-23

## Log

- **2026-05-23**: Created — Wave thesis-1 Bucket F closes GAP-652 script-only mode. Runtime execution + screenshot capture defer Wave thesis-2 hậu GAP-612 AWS restore.
