# GAP-385: Beta-signup form thiếu PDPL 2023 consent flow

**Status:** 🟢 DONE 2026-05-07 (Wave 35 Bucket B)
**Priority:** 🔴 P0 BLOCKING — PDPL deadline 2026-07-01 (~7 tuần countdown), Phase 1 launch chặn
**Domain:** Backend + Frontend / Compliance (PDPL 2023 §3.1)
**Found:** 2026-05-07 (Security /100 audit Wave 33 — agent a24fe574)
**Affects:** `BetaRequestForm` (FE) + `BetaRequestDto` (BE) + Wave 33 GAP-372 invite mechanism

## Problem

Beta signup endpoint `POST /api/v1/auth/request-beta-access` thu thập PII:
- Email (PII)
- Họ tên (PII)
- Tên tổ chức (PII pháp nhân)
- Persona (P1_SOLO_TEACHER / P2_CENTER_OWNER) — semi-PII

**Vi phạm PDPL 2023 §3.1 (explicit consent for PII collection):**
- KHÔNG có consent checkbox trên form
- KHÔNG có privacy policy link
- KHÔNG có ToS reference
- KHÔNG store consent timestamp trong DB
- KHÔNG có audit log cho consent given event

## Root Cause

Wave 33 Bucket A/C (form + service) shipped với chỉ `@Email` + `@NotBlank` validation — không có compliance gate. PDPL compliance đã thực hiện cho main signup flow (Wave 23) nhưng beta-signup là entry surface MỚI không kế thừa.

## Proposed Fix

**Frontend (`kitehub-frontend/src/components/beta/BetaRequestForm.tsx` hoặc tương tự):**
- Add required checkbox: "Tôi đồng ý cho Kite xử lý dữ liệu cá nhân của tôi theo [Chính sách Quyền riêng tư](/privacy) và [Điều khoản Sử dụng](/terms)"
- Disable submit button cho đến khi checkbox checked
- Link `/privacy` + `/terms` (verify routes tồn tại; nếu chưa → file follow-up gap cho doc creation)

**Backend (`BetaRequestDto` + `BetaAccessRequest` + V*_beta migration):**
- Add field `consent_given: boolean NOT NULL` trong DTO + entity
- Add field `consent_at: TIMESTAMP NOT NULL` trong entity
- Validation `@AssertTrue` trên `consentGiven`
- Reject với HTTP 400 + error `BETA_CONSENT_REQUIRED` nếu false
- Audit log entry `beta.consent.given` qua outbox

**Migration:**
- New Flyway: `V{N}__beta_request_add_consent.sql` — ADD COLUMN consent_given + consent_at + backfill defaults cho existing PENDING rows (NULL → mark TBD)

## Acceptance Criteria

- [x] FE checkbox + privacy/terms links + submit gate (`BetaRequestForm.tsx` + `data-testid="beta-consent-checkbox"` + button `disabled={!consentGiven}` + `/legal/privacy` + `/legal/terms` links — both routes exist per `pnpm build` output)
- [x] BE field `consentGiven` + validation `@AssertTrue` (`BetaRequestDto.java` `@NotNull` + `@AssertTrue isConsentAccepted()` returning `BETA_CONSENT_REQUIRED`)
- [x] DB migration adds 2 columns (`V32__beta_request_add_consent.sql` — `consent_given BOOLEAN NOT NULL DEFAULT FALSE` + `consent_at TIMESTAMP WITH TIME ZONE NOT NULL`; backfill `consent_at = created_at`)
- [x] Audit log entry on consent given (`BetaAccessService.submitRequest` emits `beta.consent.given` via `SubscriptionEventEmitter` outbox; topic `audit.beta.consent`; verified by `BetaAccessServiceTest.submitRequestCreatesPending`)
- [x] Integration test: missing consent → 400 BETA_CONSENT_REQUIRED (`BetaAccessControllerTest.submitRequestRejectsMissingConsent`)
- [x] Integration test: consent=true → 201, DB row has consent fields populated (`BetaAccessControllerTest.submitRequestAcceptsValid` + `BetaAccessServiceTest.submitRequestCreatesPending` asserts `isConsentGiven()` + `getConsentAt()`)
- [x] `documents/01-business/kitehub/beta-access/rules.md` BR-BETA-001 5-attribute PDPL 2023 Art 11 block (shipped Wave 35 Bucket 0 Foundation PR #916; verified present 2026-05-07)
- [x] Privacy policy + ToS routes exist (`/legal/privacy` + `/legal/terms` rendered as static routes per `pnpm build`)
- [x] Existing PENDING rows decision: V32 migration backfills `consent_given=FALSE` + `consent_at=created_at`; coordinator policy gates approval (documented in V32 SQL header)

## Related

- Source audit: `documents/04-quality/audits/security/2026-05-07-wave-33-beta-deploy.md` (Finding #2 — A06 OWASP)
- Parent gap: GAP-372 (beta tenant invite — Wave 33)
- Compliance rule: `.claude/rules/business-logic-review.md` v1.0.0 §2.4 PDPL 2023 mandate
- Phase context: PDPL hard deadline 2026-07-01 per CLAUDE.md
- Memory: `feedback_release_1_first_session_priority.md` Phase 1 trigger

## Log

- **2026-05-07** Filed from Security /100 audit Wave 33. State-check: 0 existing gaps cover beta-signup PDPL consent (grep `beta.*PDPL|consent.*beta` returned 0 matches in gaps dir). Wave 23 PDPL Phase 2 closed main signup compliance — beta is NEW surface.
- **2026-05-07** Wave 35 Bucket B shipped — DTO `consentGiven` `@NotNull` + `@AssertTrue` (with `@JsonIgnore` on the assertion accessor to avoid Jackson serializing it back into request body); entity `BetaAccessRequest.consentGiven`/`consentAt` columns; V32 Flyway migration; `BetaAccessService.submitRequest` sets fields + emits `beta.consent.given` outbox event (audit topic `audit.beta.consent`, payload includes `requestId`/`email`/`persona`/`consentAt`); FE form gates submit on consent + sends `consentGiven=true` in POST. BE verify: 430/430 tests pass on `kitehub-subscription`. FE: 6/6 vitest tests + `next build` strict pass. Coordination with Bucket A (GAP-384 admin auth): no shared file conflict; controller tests retained their structure with consent fields appended.
