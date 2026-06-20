# Versioning + Deprecation Policy — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** Tech Lead + PM
**Reviewer:** Business Lead
**Legal basis:** **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (L3)** — thông báo trước khi thay đổi/ngừng tính năng material ảnh hưởng người dùng
**Related:** [`api-terms-developer-license.md`](api-terms-developer-license.md) · [`terms-of-service.md`](terms-of-service.md) · [`customer-sla-uptime.md`](customer-sla-uptime.md)

---

## 1. Phạm vi & mục đích

Chính sách versioning + deprecation cho API + tính năng + breaking change. Đảm bảo khách hàng + developer có thời gian thích ứng trước khi đổi/ngừng tính năng.

Skeleton Phase 1 (maturity P3): khung policy; **deprecation window cụ thể chốt khi có API program + customer base**.

---

## 2. Versioning scheme

- API: semantic version (path `/api/v1/...` hiện tại). Breaking change → `/api/v2/...`.
- Product: theo release plan (`release-1-plan-2026.md`).

> TBD (Phase 2): chính sách giữ song song bao nhiêu API major version; sunset cadence.

## 3. Deprecation window

| Loại thay đổi | Thông báo trước | Kênh |
|---|:---:|---|
| API breaking change | TBD ≥90 ngày | email + changelog + header `Sunset` |
| Tính năng material ngừng | TBD ≥30 ngày (L3) | email + in-app banner |
| Bug-fix / non-breaking | không bắt buộc | changelog |

> TBD (Phase 2 — legal/PM): chốt deprecation window per loại; L3 yêu cầu thông báo material change ≥30 ngày.

## 4. Communication

- Changelog công khai (TBD location).
- `Sunset` / `Deprecation` HTTP header cho API deprecated.
- Migration guide cho breaking change.

## 5. Tuân thủ pháp lý (L3)

- Thay đổi material ảnh hưởng quyền lợi người tiêu dùng phải thông báo trước + cho quyền hủy nếu không đồng ý (đồng bộ `terms-of-service.md` + `customer-sla-uptime.md` §6).

## 6. Out of Scope (this skeleton)

- Concrete deprecation windows (Phase 2 — needs customer base)
- Changelog tooling/location (Phase 2)
- API version parallel-support count (Phase 2 — API program)

## 7. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P3 batch — O. Versioning + Deprecation Policy). Versioning scheme + deprecation window + communication structure; windows TBD Phase 2. Cites Consumer Protection L3 material-change notice.
