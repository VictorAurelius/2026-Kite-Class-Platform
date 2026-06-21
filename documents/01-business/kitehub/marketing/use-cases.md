# Use Cases — Marketing (PDPL Consent Banner + DSAR)

**Wave:** loop round 3 (GAP-1516 — 3-layer completeness)
**Service:** `kitehub-subscription`
**Status:** ⚠️ PARTIAL — mô tả runtime use-cases hiện có; counsel formal review queued Phase 2 (GAP-156 AC-D)
**Source rules:** [`rules.md`](rules.md) — `BR-PDPL-CONSENT-001..004` + `BR-PDPL-DSAR-001..006`
**API:** [`api-contract.md`](api-contract.md) — `/api/v1/consent/*` (banner v1) + `/api/v1/dsar/*`

---

## Phạm vi

Public marketing surface (pre-login, pseudonymous): cookie consent banner + Data Subject Access Request (DSAR). Actor chính = **Anonymous visitor / Data subject** (chưa đăng nhập, định danh bằng `visitorId` UUID client-side trong LocalStorage `kite_visitor_id`). Post-login authenticated consent dùng path v2 → xem [`../consent/use-cases.md`](../consent/use-cases.md).

---

## UC-MKT-CONSENT-01 — Hiển thị + chấp nhận cookie banner

**Actor:** Anonymous visitor.
**Tiền điều kiện:** Truy cập public marketing surface; chưa có consent record hợp lệ (chưa có / hết hạn 12 tháng per BR-PDPL-CONSENT-002 re-prompt cadence) trong LocalStorage.
**Trigger:** Page load trang marketing.

**Steps:**
1. FE hiển thị banner với toggle granular (essential locked-on + analytics + marketing) — BR-PDPL-CONSENT-001 (banner mandatory) + BR-PDPL-CONSENT-002 (no dark patterns: "Từ chối" ngang tầm "Chấp nhận").
2. Visitor chọn categories → FE gửi `POST /api/v1/consent/record` với `{visitorId, essentialConsented, analyticsConsented, marketingConsented, consentVersion}`.
3. Server coerce `essentialConsented=true` (BR-PDPL-CONSENT-001); idempotent upsert keyed `visitorId` (active record → update in-place; sau revoke → INSERT row mới giữ audit trail).
4. Trả `201/200` + record. FE lưu `kite.consent.v1` LocalStorage (BR-PDPL-CONSENT-003 versioned key) + ẩn banner.

**Kết quả:** consent ghi nhận server-side (retention 36 tháng — BR-PDPL-CONSENT-003); analytics/marketing scripts chỉ load nếu được granted.

**Errors:** `400 VALIDATION_ERROR` (thiếu `visitorId` / `analyticsConsented`). FE fallback: nếu API fail, KHÔNG load non-essential scripts (fail-closed về privacy).

**FE behavior:** banner không che nội dung chính (accessibility); essential luôn ON disabled; "Tùy chỉnh" mở granular toggles.

---

## UC-MKT-CONSENT-02 — Đọc trạng thái consent hiện tại

**Actor:** Anonymous visitor (cùng `visitorId`).
**Trigger:** Page load — FE kiểm tra consent đã lưu / hết hạn chưa.

**Steps:**
1. FE gửi `GET /api/v1/consent/{visitorId}`.
2. Server trả record mới nhất (granted categories + `consentVersion` + thời điểm).
3. FE quyết định re-prompt nếu hết hạn 12 tháng (BR-PDPL-CONSENT-002) hoặc `consentVersion` cũ.

**Errors:** `404` (chưa có record) → FE hiển thị banner (UC-01).

---

## UC-MKT-CONSENT-03 — Rút lại consent (revoke)

**Actor:** Anonymous visitor.
**Trigger:** Visitor mở "Tùy chọn cookie" và bấm "Rút lại đồng ý" (BR-PDPL-CONSENT-004 — dễ như cho đồng ý, PDPL Art 13(1) + NĐ13 Art 18).

**Steps:**
1. FE gửi `POST /api/v1/consent/{visitorId}/revoke`.
2. Server đánh dấu revoke (re-post sau đó tạo row mới — audit trail honest).
3. FE xóa/khóa non-essential scripts + cập nhật LocalStorage.

**Errors:** `404` (không có record để revoke).

**FE behavior:** nút revoke không bị chôn sâu (no dark pattern); xác nhận "Đã rút lại đồng ý cookie".

---

## UC-MKT-DSAR-01 — Gửi yêu cầu quyền dữ liệu (DSAR request)

**Actor:** Data subject (visitor / user yêu cầu thực thi quyền PDPL Art 14).
**Tiền điều kiện:** Có thông tin định danh tối thiểu (email + `national_id_last4` cho verify — BR-PDPL-DSAR-003).
**Trigger:** Visitor điền form DSAR trên trang quyền riêng tư.

**Steps:**
1. FE gửi `POST /api/v1/dsar/request` với `{right_type, email, national_id_last4, ...}` (`right_type` ∈ 6 quyền PDPL Art 14 — BR-PDPL-DSAR-001) + honeypot field (BR-PDPL-DSAR-005 anti-spam).
2. Server tạo DSAR ticket; `DsarServiceImpl#notifyDpo` gửi push email tới `dpo@kitehub.vn` (BR-PDPL-DSAR-006); SLA 20 ngày bắt đầu (BR-PDPL-DSAR-002).
3. Trả ticket id + xác nhận. Ticket retention 36 tháng từ `resolved_at` (BR-PDPL-DSAR-004).

**Kết quả:** DSAR ticket được tạo + DPO được thông báo; SLA 20 ngày tracking.

**Errors:** `400 VALIDATION_ERROR` (thiếu `right_type`/`email`); honeypot trip → silent drop (anti-spam).

**FE behavior:** xác nhận "Đã gửi yêu cầu — chúng tôi phản hồi trong 20 ngày làm việc"; hiển thị ticket id.

---

## UC-MKT-DSAR-02 — Tra cứu trạng thái DSAR ticket

**Actor:** Data subject (với ticket id).
**Trigger:** Visitor tra cứu tiến độ yêu cầu.

**Steps:**
1. FE gửi `GET /api/v1/dsar/{ticketId}`.
2. Server trả trạng thái ticket (open / in-progress / resolved + `resolved_at`).

**Errors:** `404` (ticket id không tồn tại).

---

## Ghi chú endpoint phụ

`GET /api/v1/public/tenants/by-subdomain/{slug}` (public tenant lookup) tồn tại trong api-contract §9 phục vụ tenant resolution cho landing — không thuộc consent/DSAR scope; xem tenant-domain landing docs.

## Related

- [`rules.md`](rules.md) — BR-PDPL-CONSENT-001..004 + BR-PDPL-DSAR-001..006 + §2 Config Keys
- [`api-contract.md`](api-contract.md) — endpoints + schemas
- [`../consent/use-cases.md`](../consent/use-cases.md) — post-login immutable v2 consent

## Log

- 2026-06-21 — use-cases.md created (GAP-1516 — 3-layer completeness; marketing đã có rules.md + api-contract.md, thiếu use-cases.md). 5 UC: cookie banner (display/read/revoke) + DSAR (request/status) grounded trong api-contract + BR-PDPL-CONSENT-001..004 / BR-PDPL-DSAR-001..006. Counsel formal review Phase 2 (GAP-156 AC-D).
