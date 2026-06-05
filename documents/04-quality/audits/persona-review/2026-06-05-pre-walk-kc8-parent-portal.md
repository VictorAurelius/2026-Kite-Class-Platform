---
title: Pre-walk persona simulation — KC-8 Parent portal
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign KC-8 pre-walk per pre-walk-persona-simulation-mandate.md §3
references:
  - documents/03-planning/waves/wave-2026-06-05-flow-kc8-parent-portal.md
  - .claude/rules/pre-walk-persona-simulation-mandate.md
---

# Pre-walk persona simulation — KC-8 Parent portal

**Persona:** Phụ huynh (PARENT) đăng nhập xem điểm/điểm danh/học phí/hạnh kiểm của con.
**Agent:** Opus background, 2026-06-05. 10 failure mode (3 HIGH / 5 MEDIUM / 2 LOW).

## Failure modes

| # | Title | Where | Symptom | Verdict |
|---|---|---|---|---|
| 1 | Missing `from`/`to` → HTTP 500 (not 400) | `GlobalExceptionHandler` thiếu `@ExceptionHandler(MissingServletRequestParameterException)` → fallthrough `Exception.class` 500 | Parent mở tab Điểm danh/Học phí cold (FE không gửi date) → 500 generic, audit không ghi | **HIGH — fix before walk** |
| 2 | Notifications facet require from/to (contract mismatch vs FE "no params") | `ParentNotificationsFacetController` `@RequestParam from/to` no default | FE notif drawer wired "no params" → 400/500 | MEDIUM — verify |
| 3 | Notifications + Payment thiếu `@PreAuthorize` (defense-in-depth) | `ParentNotificationsFacetController` + `ParentPaymentController` (vs 4 sibling có `@PreAuthorize hasAccessToChild`) | IDOR cross-parent VẪN bị service-layer chặn (`existsBy...` → 403); chỉ thiếu first-line layer, OWASP A01 consistency | **HIGH consistency, LOW risk — fix batch #1** |
| 4 | Payment KHÔNG có consent gate (5 read facet có) | `ParentPaymentController` không gọi `consentService.checkConsent` | Parent thiếu consent vẫn POST payment được dù không đọc fees được (asymmetric pay-but-can't-see) | MEDIUM — confirm vs rules.md, không fix blind |
| 5 | Consent-blocked read KHÔNG ghi audit "success" | audit `logRead` chạy SAU consent gate | Blocked read → exception trước audit write → no false-success row (✅ đúng PDPL) | LOW — positive verify |
| 6 | First-login parent default consent → 403 EVERYWHERE | `ConsentServiceImpl.checkConsent` — default V56 no fields → `PARENT_CONSENT_REQUIRED` mọi facet | Parent login → /me/children OK → click facet → 403 khắp nơi; FE phải branch 3 code (CONSENT_REQUIRED/RECONSENT/FACET_FORBIDDEN) | **HIGH — verify walk + FE gap note** |
| 7 | `reference_id IS NULL` PARENT users → blanket 401 | gateway forward no header → `requireParentId` 401 | PARENT provisioned thiếu reference_id backfill → 401 toàn portal dù JWT hợp lệ | MEDIUM — psql check |
| 8 | `hasAccessToChild` không verify role PARENT (ref-id collision) | `AuthorizationBean.hasAccessToChild` trust `getCurrentReferenceId()`, no role check; defense = gateway route gate | TEACHER/STUDENT ref-id == parents.id (BIGINT collision) + craft request → access nếu gateway không role-gate `/api/v1/parent/**` | MEDIUM — **verify gateway route gate (promotes HIGH nếu absent)** |
| 9 | Cross-tenant child read = Hibernate @Filter trên link query | `existsByParentIdAndStudentIdAndDeletedFalse` derived query inherits tenant filter | Parent A đọc childId tenant B → link không tồn tại trong filtered view → 403 | LOW — verify 1 curl |
| 10 | Inverted range 400 (✅) nhưng no upper bound + size cap | facet check `from.isAfter(to)` → 400; no max page-size guard | `?size=100000` → large response | LOW — minor hardening |

## Recommended pre-walk batch fix

- **HIGH (fix before walk):** #1 (MissingServletRequestParameter → 400) + #3 (2 thiếu @PreAuthorize, one-line symmetric).
- **VERIFY before walk:** #6 (consent grant path để walk happy), #8 (gateway PARENT route gate — escalate HIGH nếu absent), #7 (psql null reference_id).
- **DEFER to walk catch:** #4 (payment consent — confirm rules.md), #5/#9 (positive verify), #2 (FE contract), #10 (size cap).

## Positive findings (walk-relevant)

- `@EnableMethodSecurity(prePostEnabled=true)` present (`SecurityConfig:28`) → `@PreAuthorize` live không inert.
- consent-gate-then-audit ordering đúng mọi facet (no false-success audit on block).
- type-mismatch `childId=abc` → 400 PARAM_TYPE_MISMATCH; AccessDenied → 403.
- 2 thiếu @PreAuthorize KHÔNG phải live IDOR (service `existsBy...` guards cả 2).
