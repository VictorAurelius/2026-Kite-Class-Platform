---
audience: dev
flow: KC-1 (tenant settings) — GAP-1074 session persistence + tenant isolation
gate: G2 (human browser walk)
created: 2026-06-08
stack: local Docker (kite-* + kiteclass-* full profile)
---

# G2 Recipe — KC-1 Session Isolation (GAP-1074 Option B)

## 1. Mục tiêu

Verify **Option B** (tenant-scoped localStorage) trên browser thật:
1. **Cross-tab persist** — mở URL tab mới KHÔNG bắt login lại (fix chính GAP-1074, thay sessionStorage per-tab của GAP-830).
2. **Tenant isolation (OWASP A01)** — 2 tenant đăng nhập song song KHÔNG clobber/leak token lẫn nhau.

Unit test 24/24 PASS + production build PASS — bước này là **runtime browser verify** bắt buộc trước khi flip G2 (per `g1-browser-walk-before-flip.md`: curl ≠ browser).

## 2. Setup (đã sẵn sàng)

| Thành phần | Trạng thái |
|---|---|
| Stack local | ✅ Up healthy (kite-gateway/postgres/redis + kiteclass-core + kiteclass-frontend) |
| kiteclass-frontend | ✅ Rebuilt 2026-06-08 11:57 (Option B + 7 shell page GAP-1071) |
| FE URL | `http://localhost:3000` |

### Credentials

| Tenant | Email | Password | Instance id (tenantId trong JWT) | Org name hiển thị |
|---|---|---|---|---|
| **A** | `owner@skyedu.vn` | `SkyEdu@2026` | `e8ff87e1-69fc-4842-a263-7385c68b4ffb` | Trung tâm Anh ngữ Sky Education |
| **B** | `walk.owner+bucketb@skyedu.vn` | `SkyEdu@2026` | `ba8bfdce-2669-44be-b288-cedf73559c8a` | Walk Bucket B Test |

> Password tenant B đã reset = `SkyEdu@2026` (cùng tenant A) cho tiện test. Org name khác nhau rõ ràng để phân biệt trên dashboard.

## 3. Các bước (chạy tuần tự)

> Tip: mở DevTools (F12) → tab **Application** → **Local Storage** → `http://localhost:3000` để quan sát key trực tiếp.

### Bước 1 — Login tenant A (tab 1)
- **Action:** Mở `http://localhost:3000` → login `owner@skyedu.vn` / `SkyEdu@2026`
- **Expected:** Vào dashboard "Trung tâm Anh ngữ Sky Education", có data.
- **Verify (DevTools):** Local Storage có `kc:e8ff87e1-69fc-4842-a263-7385c68b4ffb:accessToken` + `kc:activeTenant` = `e8ff87e1-...`
- **Sad path:** Nếu bị redirect về /login ngay → báo (auth bootstrap lỗi).

### Bước 2 — Cross-tab persist (fix chính GAP-1074) ⭐
- **Action:** Copy URL dashboard ở tab 1 → mở **tab 2 mới** → paste URL → Enter.
- **Expected:** Tab 2 vào thẳng dashboard tenant A, **KHÔNG bắt login lại**.
- **Verify:** Cùng data tenant A như tab 1.
- **Sad path:** Nếu tab 2 bắt login lại → **FAIL chính** (Option B không cross-tab) → báo ngay, dừng.

### Bước 3 — Login tenant B song song (tab 3)
- **Action:** Mở **tab 3** → `http://localhost:3000` → login `walk.owner+bucketb@skyedu.vn` / `SkyEdu@2026`.
- **Expected:** Tab 3 vào dashboard "Walk Bucket B Test" (org name khác tenant A).
- **Verify (DevTools):** Local Storage giờ có **CẢ HAI** key riêng biệt:
  - `kc:e8ff87e1-...:accessToken` (tenant A)
  - `kc:ba8bfdce-2669-44be-b288-cedf73559c8a:accessToken` (tenant B)
  - `kc:activeTenant` = `ba8bfdce-...` (login gần nhất)
- **Sad path:** Nếu chỉ thấy 1 key, hoặc key A bị ghi đè bởi B → **FAIL isolation** → báo.

### Bước 4 — No-clobber (quay lại tab 1)
- **Action:** Quay tab 1 (tenant A) → refresh (F5).
- **Expected:** Tab 1 **vẫn tenant A** "Sky Education" — KHÔNG bị nhảy sang tenant B dù B login sau.
- **Verify:** Data vẫn của tenant A.
- **Sad path:** Tab 1 hiển thị data tenant B → **FAIL clobber** → báo.

### Bước 5 — Logout-isolation
- **Action:** Ở tab 1 → Logout tenant A.
- **Expected:** Tab 1 về /login. Quay **tab 3** (tenant B) refresh → **vẫn đăng nhập** tenant B.
- **Verify (DevTools):** `kc:e8ff87e1-...:accessToken` đã bị xóa; `kc:ba8bfdce-...:accessToken` **còn nguyên**.
- **Sad path:** Tab 3 cũng bị logout → **FAIL** (logout A xóa token B) → báo.

## 4. Báo kết quả (chọn 1)

| Outcome | Ý nghĩa | Hành động tiếp |
|---|---|---|
| ✅ **PASS 5/5** | Cross-tab + isolation OK | Tôi flip GAP-1074 DONE + KC-1 G2 PASS + commit + mở PR |
| ⚠️ **PASS chính (B1-2), FAIL phụ (B3-5)** | Cross-tab OK, isolation lỗi | Tôi điều tra isolation; GAP-1074 giữ PARTIAL |
| ❌ **FAIL B2** | Cross-tab không hoạt động | Tôi điều tra Option B core; KHÔNG flip |
| 🔄 **Lỗi lạ** | Console error / blank / 500 | Gửi tôi screenshot + console log |

Báo dạng: `B1 ✅ / B2 ✅ / B3 ✅ / B4 ✅ / B5 ✅` (hoặc đánh dấu bước FAIL).

## 5. Troubleshooting

| Triệu chứng | Nguyên nhân có thể | Cách xử |
|---|---|---|
| Tab mới trắng / ERR_EMPTY_RESPONSE | docker-proxy stale (GAP-1067) | `docker restart kiteclass-frontend` rồi thử lại |
| Login B vào nhưng data giống A | Gateway tenant-resolution chưa nhận X-Tenant-Id mới | Báo tôi (có thể GAP-1068 residual) |
| LocalStorage không có key `kc:*` | Option B chưa load (cache cũ) | Hard refresh Ctrl+Shift+R |
| 401 sau vài giây | Token refresh flow | Báo tôi kèm Network tab |
| `POST /api/auth/logout 404` + không logout | BE chưa có logout endpoint (GAP-1075) | ✅ ĐÃ FIX 2026-06-08: logout giờ client-side (rebuild FE). Re-test bước 5. |
| Console: `CSP upgrade-insecure-requests ignored ... report-only` | CSP report-only mode (dev) | **Benign noise** — không phải lỗi, bỏ qua |

## 6. Preview G3 (sau khi G2 PASS)

G3 = production-parity walk qua gateway `:9000` với JWT thật — verify X-Tenant-Id propagation + cross-tenant 403 defense ở tầng gateway. Tôi chạy sau khi bạn confirm G2.
