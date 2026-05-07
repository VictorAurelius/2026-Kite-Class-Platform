# GAP-385: Beta-signup form thiếu PDPL 2023 consent flow

**Status:** 🔵 OPEN
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

- [ ] FE checkbox + privacy/terms links + submit gate
- [ ] BE field `consentGiven` + validation `@AssertTrue`
- [ ] DB migration adds 2 columns
- [ ] Audit log entry on consent given
- [ ] Integration test: missing consent → 400 BETA_CONSENT_REQUIRED
- [ ] Integration test: consent=true → 201, DB row has consent_at populated
- [ ] Update `documents/01-business/kitehub/beta-access/rules.md` (nếu tồn tại; else file thêm) với BR-CONSENT-001 5-attribute compliance block citing PDPL 2023 §3.1
- [ ] Update privacy policy doc (`documents/legal/`) nếu thiếu beta-flow specific section
- [ ] Existing PENDING rows decision: drop hoặc backfill consent (recommend: drop pre-launch)

## Related

- Source audit: `documents/04-quality/audits/security/2026-05-07-wave-33-beta-deploy.md` (Finding #2 — A06 OWASP)
- Parent gap: GAP-372 (beta tenant invite — Wave 33)
- Compliance rule: `.claude/rules/business-logic-review.md` v1.0.0 §2.4 PDPL 2023 mandate
- Phase context: PDPL hard deadline 2026-07-01 per CLAUDE.md
- Memory: `feedback_release_1_first_session_priority.md` Phase 1 trigger

## Log

- **2026-05-07** Filed from Security /100 audit Wave 33. State-check: 0 existing gaps cover beta-signup PDPL consent (grep `beta.*PDPL|consent.*beta` returned 0 matches in gaps dir). Wave 23 PDPL Phase 2 closed main signup compliance — beta is NEW surface.
