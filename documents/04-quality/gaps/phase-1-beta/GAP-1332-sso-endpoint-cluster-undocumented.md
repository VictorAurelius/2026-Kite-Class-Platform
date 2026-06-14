# GAP-1332: SSO endpoint cluster undocumented — không có api-contract.md cho `/api/v1/auth/sso/**`

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** `SsoController` (kitehub-subscription) + `documents/01-business/kitehub/` (thiếu domain `sso/`)

## Problem

SSO cross-product login (KiteHub → KiteClass) là recent surface (GAP-1305 SSO owner-seed deterministic walk) nhưng cluster endpoint hoàn toàn KHÔNG có api-contract.md:

- `POST /api/v1/auth/sso/issue-code` (`SsoController.java:81`)
- `POST /api/v1/auth/sso/exchange` (`SsoController.java:129`)

`grep auth/sso documents/01-business/**/api-contract.md` = 0 hit; domain folder `documents/01-business/kitehub/sso/` KHÔNG tồn tại. Consumer (KiteClass FE exchange flow, mobile, partner) không có contract reference cho request/response/error-code của SSO code-issue + code-exchange — vi phạm Cat 1.1 (P0).

Lưu ý: test có tồn tại (`SsoControllerTest.java` + `SsoCodeServiceTest.java`) → code mature, chỉ thiếu doc.

## Proposed Fix

Tạo 3-layer doc `documents/01-business/kitehub/sso/` (rules.md + use-cases.md + api-contract.md). api-contract.md document 2 endpoint: request body (issue-code: target tenant/instance; exchange: short-lived code), response (issue-code → opaque code + TTL; exchange → JWT/session), error-codes (invalid/expired code, unauthorized issuer), TTL + one-time-use semantics.

## Acceptance Criteria

- [ ] `documents/01-business/kitehub/sso/api-contract.md` document cả 2 endpoint (path/method/request/response/error-codes)
- [ ] rules.md + use-cases.md đi kèm (3-layer per CLAUDE.md business-docs mandate)
- [ ] Verification chain: BR → UC → endpoint → `SsoController` mapping → `SsoControllerTest`

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B1
- Related: GAP-1305 (SSO owner-seed walk), GAP-709 (01-business/auth docs sync)
