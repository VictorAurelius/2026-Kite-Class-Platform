# Cookie Policy — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — companion to privacy-policy.md §15 + GAP-353 ConsentBanner; Phase 2 legal counsel review qua GAP-182 Phase 2)
**Owner:** Legal + Data Protection Officer (TODO designation)
**Reviewer:** Legal counsel với VN PDPL expertise + DPO
**Last-Updated:** 2026-05-06
**Effective-Date:** 2026-05-06 (placeholder — finalize on counsel sign-off)
**Tracking:** GAP-368 (Phase 1, Wave 23 PDPL Legal Compliance) → GAP-182 Phase 2 (legal counsel + EN translation)
**Legal basis:** **Nghị định 13/2023/NĐ-CP** (PDPL — hiệu lực 2026-07-01) Điều 11, Điều 13(3); Luật An ninh mạng 2018; thông lệ quốc tế GDPR ePrivacy
**Languages:** Vietnamese (canonical, this file). English translation Phase 2.
**Cross-cuts:** [`./privacy-policy.md`](./privacy-policy.md) §15 Cookie section, [`./terms-of-service.md`](./terms-of-service.md), GAP-353 ConsentBanner spec, `BR-PDPL-CONSENT-002` (re-prompt cadence)

---

## Tóm tắt (Phase 1 skeleton)

Tài liệu này là **companion document** cho `privacy-policy.md` §15, mở rộng chi tiết cách KiteHub/KiteClass sử dụng cookies + LocalStorage trên các trang công khai (marketing surfaces) và các trang đã đăng nhập (authenticated surfaces). Phase 1 ship cấu trúc 8 sections + cookie category matrix + revocation flow. Phase 2 (qua GAP-182) — legal counsel review, DPO designation, EN translation, accurate retention finalization theo audit cookies thực tế.

Tham chiếu cookie banner UI: GAP-353 (Wave 23 Bucket BC) — ConsentBanner component link tới `/legal/privacy` + `/legal/cookies` + nút "Customize" (Privacy Center).

---

## 1. Purpose + Scope (Mục đích + Phạm vi)

**Mục đích:** Tuân thủ PDPL Decree 13/2023 Art 11 (right-to-information) + Art 13(3) (purpose-specific consent) đối với cookies + LocalStorage. Đảm bảo người dùng:
- Biết những cookie nào đang chạy + mục đích từng cookie
- Có quyền opt-in/opt-out (trừ cookies cần thiết)
- Có quyền rút lại đồng ý bất kỳ lúc nào với cùng mức độ dễ dàng như khi đồng ý

**Phạm vi áp dụng:**
- Public marketing surfaces: trang chủ, blog, pricing, catalog, contact, about, legal pages
- Authenticated dashboards: admin / teacher / parent / student / accountant
- Subdomain tenant (KiteClass instances)

**KHÔNG thuộc phạm vi:**
- Cookies do bên thứ ba độc lập đặt khi user click outbound link (không thuộc kiểm soát của KiteHub)
- Backend service-to-service cookies (không liên quan đến chủ thể dữ liệu)

## 2. Cookie Categories (Phân loại Cookie)

Mọi cookie + LocalStorage entry trên KiteHub/KiteClass được phân loại theo PDPL Art 13(3) — purpose-specific consent.

### 2.1 Essential cookies (Cookies cần thiết — KHÔNG cần consent)

**Căn cứ pháp lý:** Lợi ích chính đáng (PDPL Art 17.1.đ) — cần thiết để cung cấp dịch vụ user yêu cầu.

| Cookie / Storage key | Mục đích | Retention | Source |
|---------------------|----------|-----------|--------|
| `session_id` | Duy trì phiên đăng nhập | Session (xoá khi đóng browser) | First-party |
| `csrf_token` | Chống tấn công CSRF | Session | First-party |
| `lang` | Lưu ngôn ngữ ưa thích (vi/en) | 12 tháng | First-party |
| `kite-consent` (LocalStorage) | Lưu lựa chọn consent của user (granted/denied per-category + version + timestamp) | 12 tháng (re-prompt sau) | First-party |
| `kite-tenant-id` | Định danh tenant cho subdomain routing | Session | First-party |

User KHÔNG thể tắt essential cookies — nếu chặn qua browser, dịch vụ sẽ không hoạt động đúng.

### 2.2 Analytics cookies (Cookies phân tích — yêu cầu consent opt-in)

**Căn cứ pháp lý:** Sự đồng ý rõ ràng (PDPL Art 11 + Art 13(3)).

| Cookie / Storage key | Mục đích | Retention | Source | Trạng thái Phase 1 |
|---------------------|----------|-----------|--------|---------------------|
| (TODO Phase 2) | Đếm số lượt truy cập + path navigation aggregated | TODO (kiến nghị ≤12 tháng) | First-party hoặc self-hosted | **TẮT mặc định** — chờ Phase 2 vendor selection |

Phase 1: KHÔNG có analytics cookies thực tế. Phase 2 sẽ chọn vendor (self-hosted Plausible / Umami / Matomo ưu tiên data-residency-VN) + công bố chi tiết.

### 2.3 Marketing cookies (Cookies marketing — yêu cầu consent opt-in)

**Căn cứ pháp lý:** Sự đồng ý rõ ràng (PDPL Art 11 + Art 13(3)).

| Cookie / Storage key | Mục đích | Retention | Source | Trạng thái Phase 1 |
|---------------------|----------|-----------|--------|---------------------|
| (KHÔNG có) | Theo dõi hiệu quả chiến dịch quảng cáo | — | — | **KHÔNG sử dụng** Phase 1 |

KiteHub Phase 1 KHÔNG gắn pixel quảng cáo (Facebook Pixel, Google Ads, TikTok Pixel). Nếu Phase 2 thêm marketing cookies, sẽ:
- Update tài liệu này
- Re-prompt consent
- Material change notification 30 ngày trước

## 3. LocalStorage Usage (Sử dụng LocalStorage)

LocalStorage không phải HTTP cookie nhưng cùng đặt trên thiết bị user — PDPL coi tương tự cho mục đích consent.

| Storage key | Mục đích | Retention | Category |
|-------------|----------|-----------|----------|
| `kite-consent` | Lưu lựa chọn consent của user (cấu trúc: `{essential: true, analytics: bool, marketing: bool, version: "v1", timestamp: "ISO8601"}`) | 12 tháng | Essential |
| `kite-theme` | Lưu lựa chọn theme (light/dark/system) | Cho đến khi user clear browser data | Essential |
| `kite-sidebar-collapsed` | Lưu trạng thái sidebar admin (collapsed/expanded) | Cho đến khi user clear browser data | Essential |

LocalStorage entries dùng cho session preferences được coi là essential (cần thiết để cung cấp UX nhất quán).

## 4. Third-Party Cookies (Cookies bên thứ ba)

**Trạng thái Phase 1:** KiteHub/KiteClass **KHÔNG** sử dụng third-party cookies trên các surface công khai.

**Lưu ý future-ready:** nếu Phase 2 hoặc giai đoạn sau tích hợp:
- **Cổng thanh toán (VNPay/MoMo):** chỉ load khi user thực sự click "Thanh toán" — đặt cookies riêng theo policy của họ. KiteHub disclose vendor + policy URL khi tích hợp.
- **Map embeds (Google Maps trên trang Liên hệ):** chỉ load khi user opt-in qua placeholder click-to-load.
- **Video embeds (YouTube/Vimeo trên blog):** dùng `youtube-nocookie.com` / privacy-enhanced mode.
- **OAuth providers (Google/Microsoft login):** chỉ đặt cookies khi user chủ động chọn social login.

Mọi tích hợp third-party trong tương lai sẽ:
- Update tài liệu này với vendor + cookie names + retention
- Trigger re-prompt consent (material change)
- Đăng ký với DPO + log audit trail

## 5. Re-prompt Cadence (Tần suất Hỏi lại Đồng ý)

**Theo `BR-PDPL-CONSENT-002`** trong `documents/01-business/kiteclass/marketing/rules.md` (sẽ ship cùng wave 23):

- **Mặc định:** Re-prompt consent mỗi **12 tháng** (rolling window từ thời điểm consent gần nhất)
- **Material change trigger:** Re-prompt ngay khi:
  - Thêm cookie category mới (vd. thêm marketing cookies)
  - Thay đổi vendor (vd. swap analytics provider)
  - Thay đổi mục đích sử dụng cookies
  - Thay đổi retention period material (>20%)
- **Version-bump trigger:** Khi `kite-consent.version` field tăng (vd. v1 → v2) → consent stale → re-prompt
- **User-initiated re-review:** User có thể chủ động re-review qua Privacy Center → trigger lại banner

LocalStorage `kite-consent` lưu `timestamp` của consent gần nhất; banner check stale mỗi load page.

## 6. Revocation Flow (Quy trình Rút lại Đồng ý)

PDPL Art 12 (right to erasure) + Art 11.3 (right to withdraw consent) — user có thể rút lại với cùng mức độ dễ dàng như khi đồng ý.

### 6.1 Cách rút lại đồng ý

User có 3 cách:

1. **Privacy Center (chính thức)** — `/account/privacy-settings` (TODO Phase 2 endpoint):
   - Bật/tắt từng category cookie riêng lẻ
   - Thay đổi có hiệu lực ngay (cookies đã đặt sẽ bị xoá nếu user opt-out)
   - Tạo audit log entry với timestamp

2. **Cookie banner (re-trigger)** — Click link "Quản lý cookie" ở footer:
   - Mở lại banner ConsentBanner với lựa chọn hiện tại
   - User có thể đổi quyết định bất kỳ lúc nào

3. **Browser-level** — User có thể xoá cookies + LocalStorage qua browser settings:
   - Khi `kite-consent` bị xoá, lần truy cập kế sẽ trigger banner
   - Cookies essential vẫn cần thiết để dịch vụ hoạt động

### 6.2 Hậu quả khi opt-out

| Category opt-out | Hệ quả |
|-----------------|--------|
| Analytics | Không track aggregated stats; không ảnh hưởng đến dịch vụ |
| Marketing | Không track campaign attribution; không nhận quảng cáo retarget |
| Essential | KHÔNG thể opt-out — dịch vụ sẽ không hoạt động đúng |

### 6.3 Audit trail

Mọi consent change (grant + revoke) được log với:
- `userId` (nếu đã đăng nhập) hoặc `anonymous-id` (chưa đăng nhập)
- `timestamp` ISO-8601
- `categories` (which categories changed)
- `source` (banner / privacy-center / browser-clear)
- `version` (consent policy version at time of action)

Audit log retention: 24 tháng (per `logs-format-standard.md` security tier).

## 7. Cross-Reference (Tham chiếu chéo)

- **Privacy Policy §15:** [`./privacy-policy.md#15-cookie-policy-chính-sách-cookie`](./privacy-policy.md) — high-level cookie disclosure trong Privacy Policy
- **Terms of Service:** [`./terms-of-service.md`](./terms-of-service.md) §8 Confidentiality + Data Protection — DPA framework
- **Consent business rules:** `documents/01-business/kiteclass/marketing/rules.md` §`BR-PDPL-CONSENT-002` (re-prompt cadence)
- **ConsentBanner spec:** GAP-353 (Wave 23 Bucket BC) — `_shared/components/ConsentBanner/`
- **DSAR intake form:** GAP-353c (Wave 23 closure) — public form cho PDPL Art 14 rights
- **Logs format:** [`/.claude/rules/logs-format-standard.md`](../../.claude/rules/logs-format-standard.md) — audit log retention
- **Personas:** [`./personas-catalog.md`](./personas-catalog.md) — public visitors + authenticated personas

## 8. Changes to Cookie Policy (Thay đổi Chính sách Cookie)

**Mục đích:** Quy trình thay đổi + thông báo + re-consent (PDPL Art 11.2).

- **Phiên bản hiện tại:** Phase 1 SKELETON — `version: "v1"`, last-updated 2026-05-06
- **Material changes (yêu cầu re-consent):**
  - Thêm cookie category mới
  - Thay đổi vendor (analytics / marketing provider)
  - Thay đổi retention period >20%
  - Thay đổi mục đích xử lý
  - Thêm cross-border transfer
  - **Notification:** email + in-app banner + cookie banner re-prompt 30 ngày trước hiệu lực
- **Non-material changes (publish trực tiếp):**
  - Sửa typo, format
  - Bổ sung clarification không thay đổi nghĩa
  - Update vendor URL khi vendor đổi domain
  - **Notification:** changelog ở cuối tài liệu + last-updated date
- **Lịch sử phiên bản:** TODO Phase 2 — duy trì archive `/legal/cookies/v1`, `/legal/cookies/v2` để chủ thể tham chiếu
- **Ngôn ngữ:** Tiếng Việt là bản gốc (canonical). Bản tiếng Anh sẽ publish ở Phase 2; trong trường hợp khác biệt diễn giải, **bản tiếng Việt prevail**.

---

## Phase 2 TODO (consolidated)

Mục cần hoàn thành ở Phase 2 (qua GAP-182 Phase 2) trước khi GA:

- [ ] Designate analytics vendor (self-hosted Plausible/Umami/Matomo) + công bố cookie names + retention
- [ ] Confirm marketing vendors (nếu có) + DPA signed
- [ ] Privacy Center endpoint live (`/account/privacy-settings`) (Section 6.1)
- [ ] Material-change notification mechanism (email + in-app banner) (Section 8)
- [ ] EN translation parity
- [ ] Legal counsel sign-off (VN PDPL expert)
- [ ] Audit cookie inventory thực tế qua tool (vd. Cookiebot scan / manual cookie audit)
- [ ] Version archive URL pattern (`/legal/cookies/v1`, `/legal/cookies/v2`)
- [ ] Annual cookie audit cadence (kiểm tra thực tế cookies trên production khớp với policy)

---

## Log

- **2026-05-06 (Phase 1 SKELETON):** Created during Wave 23 Bucket F per GAP-368. 8 sections theo mandate Decree 13/2023/NĐ-CP Art 11 + Art 13(3). Companion document cho `privacy-policy.md` §15. Phase 2 (legal counsel + cookie inventory audit + EN translation + Privacy Center) tracked under GAP-182 Phase 2. Cross-link tới ConsentBanner GAP-353 (Wave 23 Bucket BC) + DSAR form GAP-353c.
