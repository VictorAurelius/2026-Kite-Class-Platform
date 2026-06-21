# User Preferences — Use Cases

**Domain:** User preference state — dismissible banners + onboarding phase tracking (Wave 98 GAP-656 UI Coordinator)
**Service:** `kitehub-subscription`
**Sister layers:** [`rules.md`](rules.md) (Layer 1) · [`api-contract.md`](api-contract.md) (Layer 3)
**Last verified:** 2026-06-21 (GAP-664 backfill — grounded in `PreferencesController` + api-contract `POST /api/v1/preferences/dismiss-banner-state`)

---

## Phạm vi

Một endpoint duy nhất ở Phase 1: ghi nhận user đã đóng (dismiss) một banner UI để không hiển thị lại trong 30 ngày. Actors: **khách ẩn danh** (anonymous) hoặc **user đã đăng nhập** (BR-PREF-006 — cả hai đều dùng được; Phase 1 chưa phân biệt theo userId).

---

## UC-PREF-001 — Đóng banner (dismiss)

**Actor:** Khách ẩn danh hoặc user đã đăng nhập (bất kỳ role).
**Tiền điều kiện:** Banner đang hiển thị (vd disclaimer beta `beta-disclaimer-2026-q2`, modal onboarding `day-1-onboarding`).
**Trigger:** User bấm nút "X" / "Không hiển thị lại" trên banner.

**Steps:**
1. FE gọi `POST /api/v1/preferences/dismiss-banner-state` với body `{ "bannerKey": "beta-disclaimer-2026-q2", "dismissed": true }`.
2. Server validate `bannerKey` (kebab-case, 3–100 ký tự — BR-PREF-003) + `dismissed` non-null (BR-PREF-004).
3. Server sanitize `bannerKey` (lowercase + strip ngoài `[a-z0-9-]`, ≤100 ký tự) rồi ghi vào in-memory map (BR-PREF-007).
4. Server set header `Set-Cookie: kite-banner-dismissed-{bannerKey}=1; Path=/; Max-Age=2592000; SameSite=Lax; Secure` (BR-PREF-001/002/005).
5. Trả `204 No Content` (không body).

**Kết quả:** Cookie marker tồn tại trong browser 30 ngày; lần load trang sau, FE đọc cookie → ẩn banner.

**Errors:**
- `400 PREF_INVALID_BANNER_KEY` — `bannerKey` blank / sai format kebab-case / vượt 100 ký tự (BR-PREF-003).
- `400` (Bean Validation) — `dismissed` thiếu (BR-PREF-004). *Lưu ý: api-contract.md gọi đây là `PREF_MISSING_DISMISSED`; code thực tế trả validation 400 chung — xem rules.md §drift note.*
- `429 RATE_LIMITED` — vượt rate-limit per-IP gateway (60 req/phút/IP).

**FE behavior:** `OnboardingCoordinator` ẩn banner ngay (optimistic); hook `useOnboardingPhase` đọc `document.cookie` để giữ trạng thái khi chuyển tab/route. Cookie FE-readable có chủ đích (BR-PREF-005).

---

## UC-PREF-002 — Đồng bộ trạng thái dismiss qua tab / lần tải lại

**Actor:** User (ẩn danh hoặc đã đăng nhập) đã dismiss banner ở UC-PREF-001.
**Tiền điều kiện:** Cookie `kite-banner-dismissed-{bannerKey}=1` còn hiệu lực (trong 30 ngày).
**Trigger:** User mở tab mới / reload trang / điều hướng sang route khác cùng domain.

**Steps:**
1. Browser tự gửi cookie `kite-banner-dismissed-{bannerKey}` theo mọi request cùng domain (Path `/`).
2. FE hook `useOnboardingPhase` đọc `document.cookie` (đọc được vì BR-PREF-005 không httpOnly).
3. FE thấy marker `=1` → KHÔNG render banner đã đóng.

**Kết quả:** Banner không "nhấp nháy" hiện lại ở tab/route khác — trải nghiệm nhất quán cross-tab.

**Errors:** Không có (read-only client-side). Nếu cookie hết hạn (>30 ngày) → banner hiển thị lại (đúng thiết kế, BR-PREF-002).

**FE behavior:** Không gọi API ở bước này (Phase 1 chưa có GET endpoint đọc dismissal phía server — thuần cookie-driven). Phase 2 (Wave 99+) có thể thêm GET đọc từ `user_preferences`.

---

## UC-PREF-003 — Reset dismissal (bỏ đóng banner)

**Actor:** Admin / QA (kịch bản hiếm — BR-PREF-009).
**Tiền điều kiện:** Banner đã được dismiss trước đó.
**Trigger:** Cần buộc banner hiển thị lại (vd: bản beta disclaimer cập nhật nội dung, QA test lại flow onboarding).

**Steps:**
1. Gọi `POST /api/v1/preferences/dismiss-banner-state` với `{ "bannerKey": "...", "dismissed": false }`.
2. Server flip cookie value về `"0"` cùng TTL 30 ngày (BR-PREF-009).
3. Trả `204 No Content`.

**Kết quả:** Lần load sau, FE đọc marker `=0` → render lại banner.

**Errors:** Như UC-PREF-001 (cùng validation `bannerKey`).

**FE behavior:** Không phải flow user thường; thường thực thi qua dev tool / curl. Trong sản phẩm, "re-show banner sau version bump" sẽ được xử lý bằng `bannerKey` mới (vd `beta-disclaimer-2026-q3`) thay vì reset key cũ.

---

## Out-of-scope (Phase 2 → Wave 99+)

- Persist dismissal vào bảng `user_preferences` (`user_id + banner_key + dismissed_at`) khi user đã đăng nhập (BR-PREF-007 TODO).
- Phân vùng state theo `userId` từ `SecurityContextHolder` (BR-PREF-006 TODO — Phase 1 hardcode `"anonymous:"`).
- GET endpoint đọc dismissal phía server (Phase 1 thuần cookie-driven).
- Opt-out vĩnh viễn "Không bao giờ hiển thị lại" (DB row + permanent flag).
- Tách TTL thành config key `kitehub.preferences.banner-dismiss-ttl-days` (BR-PREF-002 TODO).

---

## Related

- [`rules.md`](rules.md) — BR-PREF-001..009
- [`api-contract.md`](api-contract.md) — `POST /api/v1/preferences/dismiss-banner-state`
- GAP-656 — domain origin · GAP-664 — 3-layer backfill (this file)
- `PreferencesControllerIT` — IT coverage

## Log

- **2026-06-21** — use-cases.md created (GAP-664 — 3-layer completeness backfill). 3 UC-PREF (dismiss / cross-tab-sync / reset) grounded trong endpoint `POST /api/v1/preferences/dismiss-banner-state` + `PreferencesController` cookie behavior. Đồng bộ UC-ID với api-contract.md (đã cite UC-PREF-001).
