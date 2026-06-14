# GAP-1338: Versioning inconsistency — kitehub `/api/platform/**` + `/api/auth/**` unversioned vs kiteclass `/api/v1/**`

**Status:** 🟡 PARTIAL
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** kitehub-subscription controllers (base path convention) + versioning policy doc

## Problem

Cat 4.1 (P0 per rubric — yêu cầu 0 endpoint ngoài `/api/v[0-9]+/`) FAIL:

- **kiteclass-core** theo `/api/v1/**` (versioned) ✅
- **kitehub-subscription** theo `/api/platform/**` + `/api/auth/**` (unversioned) — vd `/api/platform/subscriptions`, `/api/platform/payments`, `/api/auth/password-reset-request`.

Mixed convention không được bắc cầu bởi versioning-policy doc nào. Hệ quả: consumer (mobile/3rd-party/partner) không có quy ước version nhất quán giữa 2 product surface; breaking-change future trên `/api/platform/**` không có URL version để evolve an toàn.

Lưu ý: `/api/platform/` là intentional namespace của kitehub (không phải lỗi accidental). Nhưng nó vẫn là policy gap chưa documented; do đó P2 (không phải breakage hiện tại, consumer-stable nội bộ kitehub) thay vì P0-as-rubric.

Bằng chứng versioning-awareness có tồn tại: `TwoFactorController.java:105-155` expose dual-path `/api/v1/auth/2fa/*` + `/api/auth/2fa/*` → team biết versioning nhưng chưa nhất quán hóa.

## Proposed Fix

(1) Tạo/ cập nhật versioning-policy doc (`documents/02-architecture/` hoặc ADR) giải thích: kiteclass dùng `/api/v1/`, kitehub dùng `/api/platform/` namespace (rationale + version-evolution strategy cho platform namespace). (2) HOẶC migrate kitehub sang `/api/v1/platform/**` nếu quyết định versioned URL. (3) Document trong subscription-billing/api-contract.md preamble.

## Acceptance Criteria

- [ ] Versioning policy documented (ADR/rules) bắc cầu 2 convention + evolution strategy
- [ ] api-contract.md preamble nêu convention per-service
- [ ] Decision: giữ namespace hay migrate versioned — recorded

## Resolution

🟡 PARTIAL (2026-06-15, branch `fix/audit-fixC-apidocs-2026-06-14`). DOCUMENT convention + decision (URL re-versioning = breaking → defer migration):
- `kitehub/subscription-billing/api-contract.md` §"API versioning convention": bảng 3 surface (kiteclass `/api/v1/**` versioned; kitehub `/api/platform/**`+`/api/auth/**` namespace unversioned — chủ ý; cross-product auth `/api/v1/auth/**` versioned) + rationale + evolution strategy (additive field / header-version `Accept: vnd.kitehub.v2+json` / `/api/platform/v2/**` khi cần) + tiền lệ `TwoFactorController` dual-path (GAP-547).
- Decision: GIỮ namespace `/api/platform/**`, KHÔNG migrate URL-versioned Phase 1 (breaking cho mọi consumer; lợi ích < chi phí khi chưa có external consumer).

AC #1 ✅ (policy documented bắc cầu 2 convention + evolution strategy); AC #2 ✅ (preamble nêu convention per-service); AC #3 ✅ (decision recorded: giữ namespace).

PARTIAL (không DONE): cross-service URL inconsistency vẫn tồn tại ở code theo quyết định "giữ namespace Phase 1"; migrate toàn bộ URL-versioned (option khi có external API consumer) = wave riêng, deferred Phase 2.

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B7
- Rubric: `audit-skill-rubric-api-contract-audit.md` §2.4 check 4.1
- Related: GAP-547 (2FA unversioned URL — đã resolve dual-path)
