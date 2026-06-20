# Vendor Management / Third-Party Risk Policy — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + Security Lead
**Reviewer:** Legal counsel + Tech Lead
**Legal basis:** **Nghị định 13/2023/NĐ-CP (PDPL — L1)** trách nhiệm Bên Kiểm soát với Bên Xử lý dữ liệu (data processor agreement)
**Related:** [`data-classification-policy.md`](data-classification-policy.md) · [`compliance-scope.md`](compliance-scope.md) · [`security-posture-summary.md`](security-posture-summary.md) · [`dpia.md`](dpia.md)

---

## 1. Phạm vi & mục đích

Quy trình đánh giá + quản lý rủi ro nhà cung cấp bên thứ ba (sub-processor) xử lý dữ liệu hoặc cung cấp hạ tầng trọng yếu. PDPL L1 quy định Bên Kiểm soát (KiteHub) chịu trách nhiệm với hành vi của Bên Xử lý → cần DPA + đánh giá.

Skeleton Phase 1: khung đánh giá + danh mục sub-processor; **DPA template + đánh giá chi tiết cần Legal Phase 2**.

---

## 2. Sub-processor inventory (danh mục)

| Vendor | Vai trò | Dữ liệu chạm | DPA |
|---|---|---|:---:|
| AWS (ap-southeast-1) | hạ tầng compute/storage/DB | tất cả (encrypted) | TBD |
| Resend / SES | email transactional | email + tên | TBD |
| SePay | payment QR (KH-3) | giao dịch | TBD |
| Zalo OA (Phase 2) | notification | SĐT + tên | TBD vendor-blocked |
| Cloudflare | DNS/CDN/tunnel | traffic metadata | TBD |

> TBD (Phase 2 — legal): ký DPA với từng sub-processor; xác nhận localization/transfer compliance (đặc biệt AWS region + data residency VN).

## 3. Đánh giá rủi ro (assessment)

Mỗi vendor mới chạm dữ liệu cá nhân phải qua: (a) phân loại dữ liệu chạm per [`data-classification-policy.md`](data-classification-policy.md); (b) security questionnaire; (c) DPA ký kết; (d) DPIA nếu high-risk (cross-ref [`dpia.md`](dpia.md)).

> TBD (Phase 2): security questionnaire template; ngưỡng "high-risk" trigger DPIA.

## 4. Ongoing monitoring

- Review định kỳ (TBD cadence — gợi ý hàng năm).
- Theo dõi breach của vendor → kích hoạt incident-response nếu ảnh hưởng dữ liệu Kite.

## 5. Tuân thủ pháp lý (L1)

- **PDPL:** Bên Kiểm soát chịu trách nhiệm liên đới; phải có hợp đồng xử lý dữ liệu (DPA) với mọi Bên Xử lý.
- Thông báo data-subject khi thay đổi sub-processor trọng yếu (TBD per privacy-policy).

## 6. Out of Scope (this skeleton)

- DPA legal template (Phase 2 — Legal counsel)
- Vendor security questionnaire (Phase 2)
- Localization legal opinion (Phase 2 — data residency VN)

## 7. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P2 batch — AA. Vendor Management / 3rd-Party Risk). Sub-processor inventory + assessment + monitoring structure; DPA + detailed assessment TBD Phase 2 legal. Cites PDPL L1 controller-processor responsibility.
