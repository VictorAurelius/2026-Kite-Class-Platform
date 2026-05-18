# GAP-659: Staff-invite email + persona-tone split (formal owner vs informal teacher)

**Status:** 🟡 PARTIAL (80% — Wave 98 B1)
**Priority:** 🔴 P0
**Domain:** Backend (kitehub-email templates + tone logic)
**Detected:** 2026-05-18 (Wave 98 prep — outside-in audit persona F-NEW-6 + external benchmark B-NEW-3)
**Parent audit:** `documents/04-quality/audits/persona-review/2026-05-18-wave-98-cluster-b-beta-cohort-outside-in.md` F-NEW-6 + `2026-05-18-wave-98-cluster-b-external-benchmark.md` B-NEW-3 (VN business email tone register)

## Problem

GAP-543 PARTIAL 40% scope = 5 critical email types (welcome / verify-email / password-reset / beta-invite / day-7-feedback). External benchmark audit + persona walkthrough surfaced **2 critical gaps**:

### Issue 1: Staff-invite email type missing

Beta cohort target = P2 Center Owner (chị Hằng) → invites P3 Manager (anh Tâm) trong onboarding flow. GAP-543 scope KHÔNG có staff-invite template → P2 → P3 flow blocked.

Verified empirically: `grep -rn "staff-invite\|staff_invite\|invite_staff" kitehub/kitehub-email/src/main/resources/templates/` → 1 file (`invite-staff.txt` plain-text only, no `.html` sibling). HTML template missing.

### Issue 2: Persona-tone register wrong

External benchmark (Talkpal VN formal email + Travel With Languages VN email guide + Misa meInvoice templates) revealed VN business email convention:

| Persona | Tone | Salutation | Vocabulary |
|---|---|---|---|
| P2 Center Owner (Hằng, 45, authority figure) | **Formal** | `Kính gửi chị Hằng,` | Senior register, full honorifics, longer sentences |
| P3 Center Manager (Tâm, 32, peer) | **Semi-formal** | `Chào anh Tâm,` | Mid-register, professional but not stiff |
| P1 Solo Teacher (Linh, 28, freelance) | **Informal** | `Chào bạn,` | Friendly, action-oriented, shorter |
| Anonymous Prospect (Vy, varies) | **Formal-safe** | `Kính gửi Quý khách,` | Default formal (safer for authority figure) |

Current state: GAP-543 5 templates ship **single tone** "Chào bạn" informal cho mọi persona → **trust-burning** khi sent to authority figure (P2 Owner) trong VN business culture.

## Root Cause

GAP-543 scope drafted inside-out (per-template content only) without external benchmark on VN business email convention. No persona-tone matrix existed at GAP-543 filing time.

## Proposed Fix

### Step 1: Staff-invite email template (HTML + plain-text)

`kitehub-email/src/main/resources/templates/emails/staff-invite.html` + `.txt`:
- HTML: header logo + body + CTA "Chấp nhận lời mời" + footer support@
- Plain-text sibling per GAP-657 §Step 1
- Variables: `{centerName}`, `{inviterName}`, `{inviteeName}`, `{role}`, `{acceptUrl}`, `{expiryHours}`

### Step 2: Persona-tone template variant generator

`kitehub-email/.../EmailTemplateRenderer.java`:
- Add `Tone` enum: `FORMAL_AUTHORITY` / `SEMI_FORMAL_PEER` / `INFORMAL_FRIEND` / `FORMAL_SAFE_DEFAULT`
- Resolve tone từ recipient role:
  - `PLATFORM_ADMIN` / `CENTER_OWNER` → FORMAL_AUTHORITY
  - `CENTER_MANAGER` → SEMI_FORMAL_PEER
  - `TEACHER` (solo) → INFORMAL_FRIEND
  - Anonymous / unknown → FORMAL_SAFE_DEFAULT
- Template select variant: `welcome.html` → `welcome.formal.html` / `welcome.informal.html` / etc.

### Step 3: Native VN copywriter pass cho 5 critical + staff-invite (6 total)

Pair với GAP-658 (VN sample seed copywriter pass). Same writer reviews:
- Salutation register
- Body tone consistency
- Vietnamese business email conventions (formal closing "Trân trọng," vs informal "Cảm ơn,")
- Cultural fit (avoid direct imperatives với authority figure)

### Step 4: Wave 98 simplification — default FORMAL_SAFE_DEFAULT

Wave 98 ship MUST: ALL emails default to **FORMAL_SAFE_DEFAULT** tone (safer than informal-to-authority).

Wave 99+ defer: persona-aware tone split logic (Step 2 full implementation). Wave 98 = single safe template, but with VN copywriter-reviewed register.

### Step 5: GAP-543 sync

After this gap DONE → GAP-543 §AC update:
- AC1 5 critical templates content ✅ (this gap covers content review)
- AC2 staff-invite 6th template ✅ (Step 1)
- GAP-543 PARTIAL 40 → 80% (deliverability portion in GAP-657; persona-aware tone Wave 99)

## Acceptance Criteria

- [ ] `staff-invite.html` + `staff-invite.txt` templates implement
- [ ] 6 templates (5 critical + staff-invite) reviewed by native VN copywriter
- [ ] Wave 98 default = FORMAL_SAFE_DEFAULT tone applied to all 6 templates
- [ ] `Tone` enum + `EmailTemplateRenderer` Tone-resolution wired (logic ready for Wave 99 variants)
- [ ] Persona-tone matrix documented trong `documents/01-business/kitehub/email/rules.md`
- [ ] `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` PASS
- [ ] GAP-543 PARTIAL 40 → 80% updated

## Effort estimate

~1 wave bucket + 0.5 day native VN copywriter (shared GAP-658 budget). Parallel-safe với B0/B1.

## Related

- **Parent audits:** outside-in F-NEW-6 + external benchmark B-NEW-3
- **Sister gap:** GAP-543 PARTIAL — this gap closes content/tone portion; GAP-657 closes deliverability portion
- **Pair:** GAP-658 (VN sample seed) — shared native VN copywriter pass
- **Standards referenced:** `user-manual-content-standard.md` §2 row 4 Vietnamese narrative; external benchmark Misa eInvoice / Haravan VN business email patterns
- **Wave 98 bucket:** B1 (extends GAP-543; pair with GAP-657)

## Log

- **2026-05-18 (Wave 98 B1 PARTIAL 80%):** Shipped content + tone foundation:
  - `staff-invite` HTML + `.txt` templates already existed (verified `kitehub/kitehub-email/src/main/resources/templates/emails/invite-staff.html` + `invite-staff.txt`). Existing variables (`recipientName`/`ownerName`/`tenantName`/`role`/`inviteUrl`/`expiresAt`) match GAP-659 spec close enough — `centerName` semantically equivalent to `tenantName`; `inviteeName` covered by `recipientName`; `expiryHours` covered by `expiresAt` narrative.
  - `Tone` enum (`com.kitehub.email.api.Tone`) — 4 values (FORMAL_AUTHORITY / SEMI_FORMAL_PEER / INFORMAL_FRIEND / FORMAL_SAFE_DEFAULT) + `fromRole(String)` static method with case-insensitive role mapping.
  - `EmailTemplateRenderer` (`com.kitehub.email.service.EmailTemplateRenderer`) — central renderer with Tone parameter. Wave 98 simplification: all tones resolve to base template path (`emails/{name}`). Wave 99 TODO marker in `resolveTemplatePath()` for per-tone variant templates.
  - All 5 critical templates default to FORMAL_SAFE_DEFAULT salutation ("Kính gửi anh/chị ...") + closing ("Trân trọng, Đội ngũ KiteHub") — verified in `.txt` siblings (the canonical plain-text body which paired GAP-657).
  - Tone resolution rules documented in `documents/01-business/kitehub/email/rules.md` §BR-EMAIL-004 (role → tone matrix) + §BR-EMAIL-005 (sender identity).
  - Tone enum tests (`EmailTemplateRendererTest.toneFromRole_resolvesCorrectly`) cover PLATFORM_ADMIN / CENTER_OWNER / center_manager (case insensitive) / TEACHER / null / unknown / empty. ALL PASS.
- **2026-05-18 — Deferred items (carry-over to Wave 99):**
  - **Per-tone variant template files** (§Step 2 full implementation) — `welcome.formal.html` / `welcome.informal.html` / `welcome.semi-formal.html` etc. not created. Single FORMAL_SAFE_DEFAULT template per type ships Wave 98 per §Step 4 simplification.
  - **Native VN copywriter pass** (§Step 3) — paired GAP-658 deferred to Wave 99 budget (shared writer).
  - **Persona-tone routing logic at send-site** — `EmailController` + `EmailRequest` DTO not yet extended to accept `recipientRole` → resolve tone. Current path: caller passes `tone` variable in `variables` map OR renderer reads `variables.recipientRole`. Concrete callsite wiring (e.g., `kitehub-subscription` invite endpoint passing P3 Manager role → resolve to SEMI_FORMAL_PEER) deferred follow-up gap.
- **2026-05-18 — Verification commands run:**
  - `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` → BUILD SUCCESS.
  - Tone resolution test: 6 assertions PASS (PLATFORM_ADMIN, CENTER_OWNER, center_manager, TEACHER, null, UNKNOWN_ROLE).
