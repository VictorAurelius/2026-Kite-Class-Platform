# GAP-1027: Consent v1 — read/revoke consent by visitorId không auth (IDOR, mitigated UUID)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-06 (KH-8 consent G1 walk)
**Affects:** `ConsentController` v1 (`/api/v1/consent`, kitehub-subscription)

## Problem

KH-8 G1 walk: ConsentController v1 (`/api/v1/consent`) KHÔNG có @PreAuthorize (public by design — anonymous visitor record cookie consent trước khi login). Walk evidence: `POST /record` không auth → 201; `GET /{visitorId}` → 200; `POST /{visitorId}/revoke` → 200.

Hệ quả IDOR nhẹ: bất kỳ ai biết/đoán `visitorId` của người khác có thể đọc consent status + revoke consent của họ. Mitigated bởi `visitorId` = UUIDv4 (unguessable) → chỉ khai thác được khi leak visitorId (vd qua log/URL/analytics). Defense-in-depth, không phải P0.

Note: consent **v2** (`/api/v1/consent/v2`, ImmutableConsentController) ĐÃ SECURE — `@PreAuthorize("@consentAuthz.canAccessUser(#userId)")` bind X-User-Id + gateway strip forged header + RLS no-update/no-delete (PDPL Art 11 tamper-proof). v2 là path cho authenticated user; v1 cho anonymous visitor.

## Root Cause

v1 public-by-design cho cookie consent, nhưng read/revoke chỉ dựa visitorId không thêm secret/binding.

## Proposed Fix

1. Bind revoke/read v1 với một consent-secret (cookie httpOnly chứa token gắn visitorId) thay vì chỉ visitorId trần; HOẶC
2. Rate-limit + audit revoke-by-visitorId; HOẶC
3. Document accept-risk (UUID unguessable đủ cho Phase 1 BETA) nếu business OK.

## Acceptance Criteria

- [ ] Read/revoke v1 cần proof-of-ownership (cookie token) HOẶC documented risk-accept
- [ ] (nếu fix) IT cover revoke với/không token

## Related

- Discovered in: KH-8 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh8-offboarding-pdpl-consent.md` (FM-4)
- Note: consent v2 secure (no gap); pre-walk FM-1 (gateway block consent) + FM-3 (DSAR double-block) REFUTED — consent v1 + DSAR reachable via gateway (201/200) in walk
