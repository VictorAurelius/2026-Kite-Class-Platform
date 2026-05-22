---
gap_id: GAP-709
title: 01-business/auth/* docs sync from Wave 103 auth findings (RBAC + JWT + 2FA)
status: OPEN
priority: P1
domain: Documentation
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: audit-gate.py post-merge hook flagged Wave 103 PR #1709 — "Business logic changed but no 01-business/ docs updated"
---

# GAP-709 — 01-business/auth/* docs sync from Wave 103 findings

## Problem

Wave 103 PR #1709 merged 2026-05-22 với 3 business-logic-impacting findings on auth flow nhưng `documents/01-business/auth/{rules,use-cases,api-contract}.md` không update trong cùng PR. Vi phạm CLAUDE.md §"Living Documents" rule "đổi logic = đổi doc trong cùng commit".

**Changes Wave 103 shipped that affected auth business logic:**

1. **Bucket A — AdminController class-level `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`** (commit `df300540`)
   - Pre-fix: `AdminController` accepted OWNER role → real OWASP A01 broken access control
   - Post-fix: only PLATFORM_ADMIN can access `/api/platform/admin/*` endpoints
   - Business rule clarification needed in `documents/01-business/auth/rules.md`

2. **Bucket B — JWT tenantId claim missing post-beta-signup** (filed as GAP-704)
   - Owner JWT lacks `tenantId` claim → `/api/v1/onboarding-progress` 400
   - Affects ALL tenant-scoped API consumers
   - Business rule decision needed: 1 user → 1 tenant (Phase 1 BETA) vs N tenant (Phase 2)

3. **Bucket C — 2FA via-gateway bridge missing** (filed as GAP-705 + GAP-706)
   - Challenge token vs access token secret separation OK by security design
   - But production gateway path broken — only direct port 8081 + spoofed headers works
   - Business rule clarification: 2FA setup flow MUST work via production gateway

## Context

Per `audit-gate.py` rules + CLAUDE.md §"CRITICAL: Business Logic Documents":
- Doc + code PHẢI cùng PR — đổi logic = đổi doc trong cùng commit
- Wave 103 shipped với only audit docs + plan docs, no `01-business/auth/*.md` update
- Hook flagged "Business logic changed but no 01-business/ docs updated" trong compliance score 1/5

## Proposed Fix

Update `documents/01-business/auth/` (or create if missing per 3-layer structure):

### rules.md updates
- BR-AUTH-XXX: PLATFORM_ADMIN required for `/api/platform/admin/*` (codify Wave 103 Bucket A fix)
- BR-AUTH-YYY: JWT tenantId claim mandatory for OWNER role post-signup (will be enforced by GAP-704 fix)
- BR-AUTH-ZZZ: 2FA TOTP challenge token verification on `/api/v1/auth/2fa/*` (clarify gateway flow GAP-705+706)
- BR-AUTH-WWW: Phase 1 BETA 1 user → 1 tenant binding model (decision for GAP-704)

### use-cases.md updates
- UC-AUTH-NNN: Admin login walk (codify Bucket A walk pattern as canonical)
- UC-AUTH-MMM: Owner signup → tenant init handoff (highlight GAP-704 fix scope)
- UC-AUTH-PPP: 2FA TOTP setup + challenge via gateway (Wave 104 GAP-705+706 fix scope)

### api-contract.md updates
- `/api/auth/login` — note 2FA challenge response shape (202 + challengeId)
- `/api/v1/auth/2fa/enroll-init` — added; per Wave 103 Bucket C discovery
- `/api/v1/auth/2fa/enroll-confirm` — added
- `/api/v1/auth/2fa/verify` — added
- `/api/v1/auth/2fa/disable` — added (with 403 CANNOT_DISABLE_2FA_FOR_ADMIN business rule)
- `/api/v1/admin/*` — add `@PreAuthorize` constraint note

## Acceptance Criteria

- [ ] `documents/01-business/auth/rules.md` updated with 4 BR entries
- [ ] `documents/01-business/auth/use-cases.md` updated with 3 UC entries
- [ ] `documents/01-business/auth/api-contract.md` synced with 5 endpoint additions/clarifications
- [ ] All 3 files reference Wave 103 PR #1709 commit `345b4c0b` for traceability
- [ ] Verification chain valid: BR-AUTH-XXX → UC-AUTH-NNN → endpoint → @Mapping → @Test
- [ ] Hook compliance check on next PR shows 5/5 score (post-docs ship)

## Related

- Wave 103 PR #1709 commit `345b4c0b` (auth-impacting changes shipped)
- GAP-704 JWT tenantId claim (BR-AUTH-YYY/UC-AUTH-MMM scope)
- GAP-705 + GAP-706 2FA gateway bridge (BR-AUTH-ZZZ/UC-AUTH-PPP scope)
- GAP-637/620 Admin RBAC (BR-AUTH-XXX scope)
- Sister: GAP-708 (Wave 103 audit suite deadline)
- Rule: CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
