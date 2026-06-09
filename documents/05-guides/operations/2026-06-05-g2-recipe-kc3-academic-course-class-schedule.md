---
title: G2 Human Test Recipe — KC-3 Academic (course → class → schedule)
audience: dev
product: KiteClass (KC) — FE kiteclass-frontend :3000, backend kiteclass-core qua gateway :9000 (per kitehub-kiteclass-boundary.md §2)
created: 2026-06-05
flow: KC-3
scope: Flow Verification Campaign G2 handoff cho luồng KC-3 (Owner setup học thuật — tạo khóa học → lớp → xếp lịch tuần → verify sessions auto-gen + cross-tenant isolation)
---

# G2 Recipe — KC-3 Academic: course → class → schedule

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Owner tự tay setup cấu trúc học thuật trên stack local production-equivalent: tạo khóa học → tạo lớp dưới khóa → gán giáo viên → xếp lịch tuần (auto-gen buổi học) → kiểm tra cách ly đa tenant đã fix (GAP-983).

**Prereq:**
- Stack local UP (kiteclass-core healthy port 8088, gateway 9000). Check: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/actuator/health` → `200`.
- KC-1 đã thông (tenant `sky-education` provisioned) + KC-2 (đã có giáo viên trong tenant).
- Có sẵn data walk: tenant `sky-education` (`0edaee10-2d13-44be-9151-12b78b7c5fd4`) đã có course id=10, class id=14, teacher id=10, 27 buổi (MON+WED). Tenant thứ 2 `khanh-phapluat` (`126eaa8c-1f63-4c30-81b5-a5921b384b3b`) dùng để test isolation.

**Thời lượng:** ~10-15 phút.

**⚠️ Giới hạn đã biết:** Module `academic-year` (niên khóa) hiện **orphan — service có logic nhưng KHÔNG có controller/REST endpoint** (GAP-982 P1). Vì vậy bước "tạo niên khóa" KHÔNG walk được qua API/UI; course tạo độc lập không bắt buộc year. KC-3 G2 walk phần **course → class → schedule** (phần đã implement).

## 2. Setup

Lấy `instance_id` 2 tenant (nếu cần data mới):
```bash
docker exec kite-postgres psql -U kitehub -d kiteclass_shared -t -c "SELECT DISTINCT instance_id FROM classes;"
```
Mỗi request gửi header `X-Tenant-Id: <instance_id>`. G2 ưu tiên walk qua UI (frontend); mỗi bước có **curl fallback** (gọi thẳng core port 8088) nếu UI chưa có trang quản lý học thuật.

## 3. Các bước

### Bước 1 — Owner login + mở khu vực quản lý học thuật
- **Hành động:** Đăng nhập Owner của tenant `sky-education` → vào mục "Khóa học / Học thuật".
- **Kỳ vọng:** Thấy danh sách khóa học của chính tenant (course id=10 "..." hiển thị). KHÔNG thấy khóa của tenant khác.
- **Sad path:** Nếu trang trắng/spinner mãi → ghi nhận FE chưa render (báo BLOCKED).
- **Verify (curl):** `curl -s "http://localhost:8088/api/v1/courses" -H "X-Tenant-Id: 0edaee10-2d13-44be-9151-12b78b7c5fd4"` → HTTP 200, content chỉ chứa course của sky.

### Bước 2 — Tạo khóa học (course)
- **Hành động:** Click "Tạo khóa học" → nhập tên + chọn giáo viên (teacher id=10) → lưu.
- **Kỳ vọng:** HTTP 201, khóa mới xuất hiện trong danh sách với id mới.
- **Sad path:** Tên trống / thiếu teacher → 400 validation (thông báo tiếng Việt).
- **Verify (curl):** `POST /api/v1/courses` (base path) — payload gồm name + teacher numeric id; trả 201 + `data.id`.

### Bước 3 — Tạo lớp dưới khóa (class)
- **Hành động:** Trong khóa vừa tạo → "Tạo lớp" → nhập name (5-200 ký tự), mô tả, locationType (IN_PERSON), startDate/endDate, maxStudents (1-500) → lưu.
- **Kỳ vọng:** HTTP 201, lớp mới gắn vào khóa.
- **Sad path:** maxStudents > 500 hoặc endDate ≤ startDate → 400.
- **Verify (curl):** `POST /api/v1/courses/{courseId}/classes` với `CreateClassRequest{name, description, schedule, locationType, locationDetail, startDate, endDate, maxStudents}` → 201 + `data.id`.

### Bước 4 — Xếp lịch tuần (schedule → auto-gen buổi học)
- **Hành động:** Trong lớp → "Xếp lịch" → chọn các ngày trong tuần (vd Thứ 2 + Thứ 4) + giờ bắt đầu/kết thúc → lưu.
- **Kỳ vọng:** HTTP 200/201 + hệ thống **tự sinh các buổi học** trải từ startDate đến endDate đúng các ngày đã chọn (vd 27 buổi MON+WED như data sky hiện có).
- **Sad path:** Không chọn ngày nào → 400 "Phải chọn ít nhất 1 ngày học trong tuần". Giờ kết thúc ≤ giờ bắt đầu → 400.
- **Verify (curl):** `POST /api/v1/classes/{classId}/schedule` với `{daysOfWeek:["MONDAY","WEDNESDAY"], startTime:"18:00", endTime:"20:00"}` → trả List buổi học.

### Bước 5 — Kiểm tra buổi học đã sinh
- **Hành động:** Mở lớp → tab "Buổi học / Lịch".
- **Kỳ vọng:** Danh sách buổi đúng số lượng + đúng ngày trong tuần.
- **Verify (curl):** `GET /api/v1/classes/{classId}/sessions` -H tenant sky → 200 + danh sách buổi.

### Bước 6 — Verify cách ly đa tenant (GAP-983 fix) 🔒
- **Hành động:** Đăng nhập/đổi sang tenant `khanh-phapluat`, thử mở trực tiếp lớp/khóa/giáo viên của sky bằng id (14/10/10).
- **Kỳ vọng:** **404 Not Found** (KHÔNG còn rò 200). Đây là phần fix Wave security-1.
- **Verify (curl):**
  ```bash
  KHANH=126eaa8c-1f63-4c30-81b5-a5921b384b3b
  for p in classes/14 classes/14/sessions courses/10 teachers/10; do
    echo "$p -> $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/api/v1/$p -H "X-Tenant-Id: $KHANH")"
  done   # tất cả phải 404
  ```

## 4. Sad path quick checks
- Cross-tenant by-id (Bước 6) → 404 (đã fix GAP-983; trước đây rò 200).
- Schedule không chọn ngày → 400 tiếng Việt.
- Tạo lớp với ngày kết thúc ≤ ngày bắt đầu → 400.
- Niên khóa (academic-year): KHÔNG có endpoint (GAP-982) — bỏ qua, không tính FAIL.

## 5. Báo kết quả (4 outcome)
- ✅ **PASS** — Bước 1-6 đúng kỳ vọng (course/class/schedule tạo được, sessions auto-gen, cross-tenant 404). → flip KC-3 campaign row sang `✅ THÔNG (G1+G2)`.
- ⚠️ **PASS-with-note** — flow chính OK nhưng academic-year thiếu UI/API (GAP-982 đã track). → KC-3 THÔNG phần implemented; GAP-982 backlog.
- ❌ **FAIL-functional** — 1 bước create/schedule lỗi → ghi bước + HTTP code + screenshot → file gap.
- 🔴 **FAIL-isolation** — Bước 6 vẫn trả 200 (rò) → P0 regression GAP-983, báo ngay.
- ⛔ **BLOCKED** — FE chưa có trang quản lý học thuật → walk qua curl fallback + note FE gap.

## 6. Troubleshooting + G3 preview
- **404 cho cả own-access (sky GET course 10 → 404/500):** kiểm tra Redis cache poisoning (GAP-986) — flush: `docker exec kite-redis sh -c "redis-cli --scan --pattern 'courses*' | xargs -r redis-cli del; redis-cli --scan --pattern 'teacher*' | xargs -r redis-cli del"`.
- **Gateway 503 cold-start:** thử lại sau 5-10s (lazy bean init), hoặc gọi thẳng core 8088.
- **G3 preview (production parity):** sau khi G2 PASS local, G3 verify trên AWS (post GAP-612 restore) — multi-tenant isolation + RLS layer (GAP-985) + Flyway-migrated DB. RLS defense-in-depth (lớp 2 độc lập @Filter) còn hở — track GAP-985 trước khi tin tưởng production isolation hoàn toàn.
