# API Terms of Use / Developer License — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + Tech Lead
**Reviewer:** Legal counsel + Business Lead
**Legal basis:** **Luật Sở hữu trí tuệ 2022 (sửa đổi)** (license + IP) · **Nghị định 13/2023/NĐ-CP (PDPL — L1)** khi API trả dữ liệu cá nhân · **Luật An ninh mạng 2018**
**Related:** [`terms-of-service.md`](terms-of-service.md) · [`acceptable-use-policy.md`](acceptable-use-policy.md) · [`data-classification-policy.md`](data-classification-policy.md) · [`security-posture-summary.md`](security-posture-summary.md)

---

## 1. Phạm vi & mục đích

Điều khoản sử dụng API công khai + license cho developer tích hợp (webhook, public API, third-party app). Phase 1 BETA chưa mở public API → doc này là **forward skeleton** cho khi mở API program (Phase 2+).

Skeleton Phase 1: khung license + rate-limit + data-use; **điều khoản cụ thể cần Legal khi launch API program** — KHÔNG cam kết API contract trước khi có.

> TBD: Phase 1 BETA chưa expose public API; tài liệu này định hình điều khoản trước khi mở developer program.

---

## 2. License grant

- Cấp quyền sử dụng API **non-exclusive, non-transferable, revocable** cho mục đích tích hợp hợp pháp với tenant của developer.
- KHÔNG cấp quyền sở hữu; mọi IP của KiteHub/KiteClass giữ nguyên (cross-ref [`brand-trademark-policy.md`](brand-trademark-policy.md)).

> TBD (Phase 2 — legal): phạm vi license per tier; điều kiện thu hồi.

## 3. Rate limiting & fair use

Rate-limit per tenant/key — đồng bộ gateway rate-limit hiện có (per GAP-514 auth rate-limit). Vượt giới hạn → 429.

> TBD (Phase 2): quota per tier, burst policy, pricing cho API call vượt quota.

## 4. Data use & privacy (L1)

- API trả dữ liệu cá nhân (học sinh/phụ huynh) phải tuân [`data-classification-policy.md`](data-classification-policy.md) + PDPL consent.
- Developer là **bên xử lý dữ liệu** → phải ký DPA (cross-ref `vendor-management-third-party-risk.md` ngược chiều).
- Cấm: scrape, resell, train AI model trên dữ liệu cá nhân không có consent.

## 5. Prohibited uses

Đồng bộ [`acceptable-use-policy.md`](acceptable-use-policy.md): không reverse-engineer, không DoS, không lách rate-limit, không truy cập cross-tenant.

## 6. Liability & termination

- Cung cấp API **"as-is"** trong giai đoạn beta/preview.
- Quyền thu hồi key khi vi phạm; thông báo deprecation per [`versioning-deprecation-policy.md`](versioning-deprecation-policy.md).

> TBD (Phase 2 — legal): giới hạn trách nhiệm, indemnification, governing law (tòa án VN per L3 cho consumer-dev).

## 7. Out of Scope (this skeleton)

- Public API surface design (Phase 2 — API program)
- Pricing for API tiers (Phase 2 — finance)
- Developer portal + key management UX (Phase 2)

## 8. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P2 batch — J. API Terms / Developer License). License + rate-limit + data-use + prohibited-use structure; forward skeleton (Phase 1 BETA no public API yet). Cites IP Law + PDPL L1 + Cybersecurity Law.
