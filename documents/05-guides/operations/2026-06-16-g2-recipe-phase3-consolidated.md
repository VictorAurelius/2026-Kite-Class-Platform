---
title: G2 walk recipe — Phase-3 consolidated (14 fix, 7 flow KH/KC)
date: 2026-06-16
type: g2-handoff-recipe
tier: 2-time-bound
wave: wave-flow-fix-1-phase3
pr: 2451
flows: [KH-5, KH-6, KH-8, KH-9, KH-10, KC-10, KC-12]
gaps: [1435, 1436, 1437, 1438, 1439, 1440, 1441, 1442, 1443, 1444, 1445, 1446, 1447, 1448]
status: g2-pending-human
---

# G2 walk recipe — Phase-3 (Flow Verification Campaign)

Mục tiêu: **human browser-walk 11 FE fix** (3 BE/SSR đã coordinator-verified) trên local Docker stack đã rebuild từ branch `wave-flow-fix-1-phase3`. Pass → flip 14 gaps PARTIAL→DONE + 7 flow re-walk ✅.

## 0. Đã verified sẵn (coordinator, KHÔNG cần walk lại)
| Gap | Verified |
|---|---|
| GAP-1439 (DSAR permit) | ✅ `POST :9000/api/v1/dsar/request` → 400 (qua auth, không 401) |
| GAP-1444 (beta-status SSR) | ✅ `:3001/beta-status` render live, no "Không tải được"; env `INTERNAL_API_URL` applied |
| GAP-1437 (theme 400) | ✅ `LogoAnalysisValidationTest` 3 PASS (null body → 400) — browser optional |

## 1. Setup (BẮT BUỘC trước walk)
Stack đã rebuild 4 service (kitehub-frontend/subscription/branding + kiteclass-frontend) từ merged code, project `kite-platform`. Verify:
```bash
docker ps --format '{{.Names}}\t{{.Status}}' | grep -E 'kitehub-frontend|kitehub-subscription|kitehub-branding|kiteclass-frontend|kite-gateway'
# Cả 5 phải (healthy), 4 service đầu uptime < vài phút (đã recreate)
```
- **KH FE = `http://localhost:3001`** (platform console — single-domain, KHÔNG subdomain-tenant) · **KC FE = `http://<slug>.127.0.0.1.nip.io:3000`** (multi-tenant — resolve tenant qua Host subdomain; **CẤM `localhost:3000` thuần / `?tenant=`** per `g1-browser-walk-before-flip.md` §3.1/§3.2) · gateway `:9000` (per `kitehub-kiteclass-boundary.md`)
- **KC tenant slug walk:** `sky-education` (verify resolve: `curl :9000/api/v1/public/tenants/by-subdomain/sky-education` → có `"subdomain"`) → base `http://sky-education.127.0.0.1.nip.io:3000` ; fallback hosts `127.0.0.1 sky-education.kiteclass.local` (per `g1-browser-walk-before-flip.md` §3.3)
- **⚠️ POST-REBUILD (GAP-1067):** stack vừa rebuild → port-forward `:3000` Windows↔container có thể stale → truy cập timeout/ERR_EMPTY_RESPONSE. Fix TRƯỚC walk: `docker restart kiteclass-frontend kitehub-frontend`, chờ ~12s.
- Credentials: **KH** `owner.test@test.vn` / `Test@1234` (OWNER) · `admin.test@test.vn` / `Test@1234` (PLATFORM_ADMIN, 2FA) · **KC** `owner@skyedu.vn` / `SkyEdu@2026` (owner tenant sky-education)
- **Seed cần (từ pre-walk artifact `2026-06-16-pre-walk-phase3.md`):**
  - KH-9 revenue (1441): ≥1 payment row trong tháng hiện tại (nếu trống → empty-state, vẫn PASS)
  - KC-10 branding (1446): ≥1 branding version row (nếu trống → list rỗng, vẫn PASS)
  - KH-5 (1435/1436): cần owner BASIC (có subscription) + owner TRIAL (no subscription) — nếu chỉ có 1 persona, walk phần áp dụng được + note
- **DevTools Console + Network tab MỞ suốt walk** (bắt NaN / 404 / 500 / CSP warning).

---

## 2. Walk steps

### Flow KH-8 — DSAR public form (GAP-1438) · persona: anonymous (không login)
1. **Action:** mở `http://localhost:3001/legal/data-rights`
   **Expected:** form render (Họ tên, email, CCCD, loại quyền, nút "Gửi yêu cầu DSAR").
2. **Action:** điền Họ tên `Trần Thị Hồng`, email `hong.tran@skyedu.vn`, CCCD `1234`, quyền = "Truy cập (ACCESS)" → click "Gửi yêu cầu DSAR".
   **Expected (PASS):** Network tab `POST :9000/api/v1/dsar/request` (KHÔNG `:3001/api/...` 404). Trả 201 → panel success + ticket UUID + SLA 20 ngày.
   **Verify:** `docker exec kite-postgres psql -U kitehub -d kitehub -c "SELECT id,status FROM dsar_ticket ORDER BY created_at DESC LIMIT 1;"` → row PENDING.
3. **Sad path:** submit form rỗng (hoặc CCCD sai) → **message tiếng Việt cụ thể** (vd "Dữ liệu không hợp lệ"), KHÔNG dump khối HTML thô vào alert.

### Flow KH-9 — Admin console (GAP-1440/1441/1442) · persona: PLATFORM_ADMIN
1. **Action:** login `admin.test@test.vn` qua `/admin` (2FA) → vào `:3001/admin`.
   **Expected (1440 PASS):** 4 KPI card hiển thị **số thật** (active/trial/suspended instances, tổng/doanh thu tháng) — **KHÔNG NaN/undefined/blank**. "Thanh toán chờ" có số. "Instance mới trong tháng" có số.
   **Sad (1440):** nếu KPI ra `NaN` hoặc trống dù BE trả data → FAIL.
2. **Action:** vào `:3001/admin/revenue`.
   **Expected (1441 PASS):** totalRevenue + MRR ra **VND thật** (không hardcode "0đ" cứng); chart ngày render bars HOẶC empty-state graceful; breakdown theo tier.
3. **Action:** xem **DevTools Console** trên bất kỳ page `:3001`.
   **Expected (1442 PASS):** KHÔNG còn warning `upgrade-insecure-requests is ignored when delivered in a report-only policy`.

### Flow KH-10 — Support/feedback + onboarding (GAP-1443/1445) · persona: OWNER
1. **Action:** login `owner.test@test.vn` → vào `:3001` dashboard.
   **Expected (1443 PASS):** thấy **nút support/feedback floating** (`?` góc dưới-phải). Click → menu: "Gửi phản hồi", "Liên hệ email" (`mailto:support@kitehub.me`), "Zalo OA", "Hướng dẫn nhanh", "Trạng thái beta".
2. **Action:** click "Gửi phản hồi" → điền + submit.
   **Expected:** Network `POST /api/v1/feedback` → 201 + toast success.
3. **Action (1445):** dashboard load với **owner tenantless** (nếu có persona; nếu không skip + note).
   **Expected (1445 PASS):** KHÔNG có error đỏ "Không tải được tiến độ"; Console KHÔNG có request `GET /api/v1/onboarding-progress` (FE guard skip khi không tenant). Owner CÓ tenant → checklist render bình thường.

### Flow KH-5 — Subscription downgrade/cancel (GAP-1435/1436) · persona: OWNER
1. **Action (1435):** owner đang **BASIC (có subscription)** → `:3001/billing/upgrade` → bước chọn tier.
   **Expected (PASS):** card "Miễn phí (FREE)" **KHÔNG** có nhãn "Hạ gói"; thay vào đó guidance "Để chuyển về Miễn phí, vui lòng hủy đăng ký" + nút "Hủy đăng ký". Click card FREE → **KHÔNG toast lỗi 400**. Chọn PREMIUM (upgrade) vẫn chạy.
   **Note:** owner TRIAL/FREE (chưa sub) → card FREE = "Hiện tại" (disabled cũ).
2. **Action (1436):** owner **TRIAL/không subscription** → `:3001/settings` → Danger Zone → card "Hủy đăng ký".
   **Expected (PASS):** nút disabled + dòng "Bạn chưa có gói đăng ký để hủy"; KHÔNG mở dialog, KHÔNG redirect `/billing?success=cancelled` (success giả). Owner có sub active → dialog mở bình thường.

### Flow KC-10 — Branding (GAP-1446/1447) · persona: OWNER KC (subdomain nip.io)
1. **Action (1446):** login owner KC tại `http://sky-education.127.0.0.1.nip.io:3000/login` → Settings → Branding (`http://sky-education.127.0.0.1.nip.io:3000/branding`) → cuộn tới "Lịch sử phiên bản".
   **Expected (PASS):** list versions; version active có badge "Đang dùng" + nút "Khôi phục" của nó disabled. Click "Khôi phục" version cũ → ConfirmDialog → confirm → toast success + list refetch.
   **Note:** nếu chưa có version nào → list rỗng graceful (vẫn PASS).
2. **Action (1447):** mở `http://sky-education.127.0.0.1.nip.io:3000/branding/wizard`.
   **Expected (PASS):** thấy **hand-off card** + cảnh báo amber "chưa có đăng nhập dùng chung KiteClass↔KiteHub"; link mở KH wizard ở **tab mới** (`target="_blank"`) — KHÔNG auto-bounce/dead-end về `:3001/login`. Tab KC giữ session.

### Flow KC-12 — Payroll (GAP-1448) · persona: OWNER KC (subdomain nip.io)
1. **Action:** `http://sky-education.127.0.0.1.nip.io:3000/admin/payroll` với filter ra empty-state.
   **Expected (PASS):** copy empty-state "Kỳ lương sẽ xuất hiện sau khi chạy bảng lương — chức năng 'Chạy bảng lương' sẽ có ở Phase 2". **KHÔNG** còn `PayrollService.calculate(...)`.

---

## 3. Sad-path checklist (tối thiểu)
- KH-8: form rỗng → VN message, no raw HTML
- KH-5: owner no-sub cancel → no fake success
- KH-10: tenantless owner → no onboarding error
- KC-10: wizard → no dead-end bounce

## 4. Báo kết quả (chọn 1)
- ✅ **PASS toàn bộ** → báo "G2 PASS 11/11" → coordinator flip 14 gaps DONE + 7 flow ✅ + merge PR #2451.
- ⚠️ **PASS một phần** → liệt kê gap FAIL + symptom + screenshot → coordinator fix inline (per `small-gap-inline-fix.md`) → re-walk phần đó.
- 🔴 **FAIL nhiều** → dừng, báo pattern → coordinator điều tra (có thể stack/seed, không phải code — xem pre-walk #1).
- ⏸️ **Blocked** (seed/persona thiếu) → báo cái thiếu → coordinator seed.

## 5. Troubleshooting
- Fix "không hiệu lực" → check service đã recreate chưa (uptime < vài phút). Nếu cũ: `cd kite-wt-phase3-inline/kitehub && docker-compose -f docker-compose.kitehub.yml up -d --force-recreate <service>`.
- beta-status còn fallback ở request đầu → ISR stale, reload lần 2 (revalidate=300).
- KPI vẫn NaN → hard-reload (Ctrl+Shift+R) clear bundle cache cũ.

## 6. G3 preview (sau G2)
- GAP-1455 (prod `INTERNAL_API_URL` fe-host PM2) — verify post-AWS-restore (AWS stopped, gated GAP-612).
- GAP-1447 full SSO KC↔KH — defer (design + AWS).
