# GAP-547: TwoFactor 2FA endpoint cluster — undocumented api-contract + unversioned URL

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (kitehub-subscription) / Docs (api-contract)
**Found:** 2026-05-14 (API Contract audit post-Wave-78)
**Affects:** Pre-release `v1.0.0-rc.*` gate (per `audit-skill-rubric-api-contract-audit.md` §6.2); future FE consumer; 3rd-party integration risk

## Problem

Wave 78 shipped `TwoFactorController` (`kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorController.java`, 250+ lines, 5 endpoints + 8 DTOs):

- `POST /api/auth/2fa/enroll-init`
- `POST /api/auth/2fa/enroll-confirm`
- `POST /api/auth/2fa/verify`
- `POST /api/auth/2fa/recovery-codes/regenerate`
- `POST /api/auth/2fa/disable`

Cả 5 endpoints:
1. **Không có `documents/01-business/kitehub/auth-2fa/api-contract.md`** (P0 — vi phạm Cat 1.1 rubric + `business-docs-3-layer.md` mandate)
2. **Không nằm dưới `/api/v[0-9]+/**`** URL versioning scheme (P1 — vi phạm `versioning-policy.md` §7.1 + Cat 4.1 rubric)

Pre-release `v1.0.0-rc.*` gate per `audit-skill-rubric-api-contract-audit.md` §6.2 yêu cầu ZERO P0 FAIL — gap này block.

## Root Cause

`/api/auth/**` namespace pre-existing từ Wave 35 auth scaffolding không versioned. Wave 78 thêm 2FA endpoints theo pattern cũ thay vì migrate sang `/api/v1/auth/**`. Đồng thời, contract-first-for-cross-layer rule applied cho 4 NEW domain endpoints khác (onboarding/feedback/beta-status/support) NHƯNG bỏ qua 2FA cluster.

## Proposed Fix

### Phase 1 — Contract documentation (P0, blocking)

1. Tạo `documents/01-business/kitehub/auth-2fa/` folder:
   - `rules.md` — BR-2FA-001..005 (TOTP secret encryption, recovery code generation, max attempts, enrollment grace period, disable confirmation)
   - `use-cases.md` — UC-2FA-001..005 (enrollment, confirm, verify on login, recovery, disable)
   - `api-contract.md` — 5 endpoints schema 1:1 với `TwoFactorController` + 8 DTOs (`EnrollInitResponse`, `EnrollConfirmRequest/Response`, `VerifyRequest/Response`, `RegenerateRequest/Response`, `DisableRequest/Response`)
2. Cross-link từ `documents/01-business/kitehub/auth-2fa/api-contract.md` tới các pre-launch security rules (`pre-launch-auth-hardening-checklist.md` §2.5/§2.6 nếu có row 2FA).
3. Cập nhật `documents/01-business/kitehub/README.md` add row auth-2fa domain.

### Phase 2 — Versioning migration (P1, follow-up cho cùng wave hoặc Wave 79)

1. Migrate `/api/auth/**` → `/api/v1/auth/**` (toàn bộ auth namespace).
2. Giữ alias `/api/auth/**` ≥6 tháng (per `versioning-policy.md` deprecation policy) + add `Deprecation` header.
3. Update gateway routes + FE consumers (kitehub-frontend auth hooks).
4. ADR ghi nhận decision + migration timeline.

## Acceptance Criteria

- [ ] Phase 1: 3 files trong `documents/01-business/kitehub/auth-2fa/` exist (rules.md + use-cases.md + api-contract.md)
- [ ] Phase 1: api-contract.md liệt kê 5 endpoints với method/path/request/response schema/error codes 1:1 với `TwoFactorController` + DTOs
- [ ] Phase 1: Cross-link section §Related references rules.md + use-cases.md + Wave 78 plan + `versioning-policy.md`
- [ ] Phase 1: `documents/01-business/kitehub/README.md` row auth-2fa added
- [ ] Phase 1: `gap-status.csv` row GAP-547 status flip → 🟡 PARTIAL (Phase 1 only) sau khi 3 files merge
- [ ] Phase 2 (separate gap or follow-up): ADR + migration plan + alias period documented
- [ ] Verification: re-run api-contract-audit → Cat 1.1 sub-check pass cho 5 endpoints 2FA

## Related

- Audit: `documents/04-quality/audits/api-contract/2026-05-14-post-wave-78.md` (bug list P0 #1-5)
- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md`
- Rule: `.claude/rules/audit-skill-rubric-api-contract-audit.md` v1.0.1 §2.1/§2.4
- Rule: `.claude/rules/versioning-policy.md` §7.1
- Rule: `.claude/rules/business-docs-3-layer.md` (3 files per domain)
- Rule: `.claude/rules/contract-first-for-cross-layer.md` v1.0.1 — không enforced Wave 78 cho 2FA cluster
- Code: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorController.java`
- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`

## Log

- **2026-05-14:** DONE — Wave 79 Bucket A closure. /v1/auth/2fa/* versioned routes shipped via gateway+kitehub-subscription dual-mapping (unversioned legacy alias kept for backwards compat); api-contract.md 6 endpoints documented; v1.0.0-rc promotion blocker cleared (PR #1365).

- **2026-05-14:** Filed from post-Wave-78 API contract audit. Verdict FAIL (P0). Block `v1.0.0-rc.*` pre-release gate. Phase 1 (docs) recommended next wave; Phase 2 (versioning) defer-able to Wave 79+.
