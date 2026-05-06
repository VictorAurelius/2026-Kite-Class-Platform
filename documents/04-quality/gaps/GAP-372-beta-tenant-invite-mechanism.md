# GAP-372: Beta Tenant Invite Mechanism — Request Beta Access Form + Manual Approval Flow

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Phase 1 BETA invite-only model requires this)
**Domain:** Frontend / Backend / DevOps
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Phase 1 BETA tenant onboarding flow

## Problem

Phase 1 BETA = invite-only model — public signup form must be replaced với "Request Beta Access" form + coordinator manual approval flow + invite email với signup token.

Current state: public signup form trên `/auth/signup` accept anyone. Cần convert thành 3-stage flow:
1. **Request:** user submits beta access request (email + name + organization + persona)
2. **Approve:** coordinator manually reviews + approves request
3. **Invite:** approved user receives signup token email; completes signup with token validation

## Proposed Fix

### Frontend changes

- **Disable public signup form** trên marketing pages (`/auth/signup`)
- **NEW page:** `/auth/request-beta-access` với form fields:
  - Email (validated)
  - Full name
  - Organization name
  - Persona dropdown: "Solo teacher (P1)" / "Small center owner (P2)"
  - Referral source (optional)
  - Honeypot anti-spam field
- **NEW page:** `/auth/beta-signup?token=XXX` — completes signup với token validation; if valid, signup form pre-fills email + persona

### Backend changes

- **NEW entity:** `beta_access_request` (id, email, name, org, persona, source, status, created_at, approved_at, approver_id, invite_token, invite_sent_at)
- **NEW endpoints:**
  - `POST /api/v1/auth/request-beta-access` — public, rate-limited (1/IP/day)
  - `GET /api/v1/admin/beta-requests?status=PENDING` — coordinator
  - `POST /api/v1/admin/beta-requests/{id}/approve` — coordinator + sends invite email
  - `POST /api/v1/admin/beta-requests/{id}/reject` — coordinator
  - `POST /api/v1/auth/beta-signup` (requires `?token=XXX`) — validates token + creates tenant với beta-flag
- **Email template:** beta invite (per GAP-370)

### Coordinator review flow

- Email notification trên new request
- Admin dashboard trang beta requests
- Manual review criteria: trusted referral, P1+P2 fit, reasonable contact info
- One-click approve → triggers invite email với token (24h validity)

## Acceptance Criteria

- [ ] Public signup form disabled trên marketing pages
- [ ] `/auth/request-beta-access` form live + functional
- [ ] `beta_access_request` entity + Flyway migration
- [ ] 4 new endpoints implemented + Bean Validation
- [ ] Admin coordinator dashboard cho review queue
- [ ] Beta invite email template (depends GAP-370)
- [ ] Token generation + validation (24h TTL, single-use)
- [ ] Beta tenant flag set trên signup (dashboard banner + footer build info)
- [ ] Anti-spam: rate limit + honeypot + reCAPTCHA (optional)
- [ ] Tests: unit + IT (signup flow end-to-end với token)

## Open decisions

- Token TTL (24h vs 72h)?
- Auto-approve criteria for known referrers? (Phase 2 maybe)
- Beta capacity limit (10? 20? 50? — hardcoded vs dynamic)

## Effort estimate

~2-3 ngày BE + ~1-2 ngày FE + email template = total ~4-5 ngày.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` §2.3 Beta invite mechanism flow
- Sister: GAP-370 (email transactional — invite email delivery)

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA — invite-only model cannot work without this mechanism.
