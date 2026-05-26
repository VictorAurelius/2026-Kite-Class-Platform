# GAP-659: Staff-invite email + persona-tone split (formal owner vs informal teacher)

**Status:** 🟡 PARTIAL (99% — Wave rst-cascade-1 Cluster-1, template+renderer LIVE-verified LOCAL; remaining: VN copywriter pass deferred GAP-658 + 2-client live verify deferred GAP-612 + send-site wiring deferred Wave 108+)
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

- [x] `staff-invite.html` + `staff-invite.txt` templates exist (Wave 98 B1 ✅)
- [ ] 6 templates (5 critical + staff-invite) reviewed by native VN copywriter — **defer follow-up gap** (shared GAP-658 budget; post-AWS-restore)
- [x] Wave 98 default = FORMAL_SAFE_DEFAULT tone applied to all 6 templates (Wave 98 ✅)
- [x] `Tone` enum + `EmailTemplateRenderer` Tone-resolution wired (Wave 98 ✅)
- [x] Per-tone variant template files shipped: `welcome.formal.html` / `welcome.informal.html` / `invite-staff.formal.html` / `invite-staff.informal.html` (Wave 107 ✅)
- [x] `resolveTemplatePath()` tone-suffix dispatch + `renderHtmlWithFallback()` fallback (Wave 107 ✅)
- [x] 12 new unit tests cover tone dispatch, fallback behavior, VN-localization checks (Wave 107 ✅)
- [x] Persona-tone matrix documented trong `documents/01-business/kitehub/email/rules.md` (Wave 98 ✅)
- [x] `cd kitehub && ./mvnw -pl kitehub-email test -P strict-warnings` PASS (Wave 107 ✅)
- [x] GAP-543 PARTIAL 40 → 80% updated (Wave 98 ✅)
- [ ] Live verify post-deploy persona render — **OUT OF SCOPE** (AWS account suspended per GAP-612; reframe per `gap-done-discipline.md` §3 Option B — unit tests are verification method; live verify deferred to post-GAP-612 restore)
- [ ] Send-site wiring (`kitehub-subscription` invite endpoint → `recipientRole` → tone resolution) — deferred follow-up gap Wave 108+

## Effort estimate

~1 wave bucket + 0.5 day native VN copywriter (shared GAP-658 budget). Parallel-safe với B0/B1.

## Related

- **Parent audits:** outside-in F-NEW-6 + external benchmark B-NEW-3
- **Sister gap:** GAP-543 PARTIAL — this gap closes content/tone portion; GAP-657 closes deliverability portion
- **Pair:** GAP-658 (VN sample seed) — shared native VN copywriter pass
- **Standards referenced:** `user-manual-content-standard.md` §2 row 4 Vietnamese narrative; external benchmark Misa eInvoice / Haravan VN business email patterns
- **Wave 98 bucket:** B1 (extends GAP-543; pair with GAP-657)

## Log

- **2026-05-26 (Wave rst-cascade-1 Cluster-1 — PARTIAL 95→99, template+renderer LIVE-verified LOCAL):** LOCAL stack 11/11 healthy post Wave aws-restore-1 closure. Live SMTP→MailHog test rendered all 4 per-tone templates end-to-end: `welcome.formal` (200 SENT, body decoded contains "Kính gửi" + "Kính ngữ cao: 'Kính gửi chị/anh {Name}'") + `welcome.informal` (200 SENT, body contains "Chào bạn" + "hành động nhanh: 'Chào bạn', câu ngắn, emoji OK") + `invite-staff.formal` (200 SENT). resolveTemplatePath() dispatch + renderHtmlWithFallback() verified live. Tone-distinction confirmed per `vn-localization-audit-checklist.md` §2 email tone matrix (P2 Owner formal / P1 Solo casual). Walkthrough doc: `documents/04-quality/audits/quality/2026-05-26-wave-rst-cascade-1-cluster-1-email.md` §GAP-659. Completion 95→99 — gap stays PARTIAL pending: (a) VN copywriter pass deferred GAP-658, (b) 2-client live verify deferred GAP-612, (c) send-site wiring deferred Wave 108+. Coordinator decides DONE flip post-cluster review.
- **2026-05-23 (Wave 107 — PARTIAL 95%):** Shipped final 20% per-tone variant implementation:
  - Created 4 per-tone variant template files: `welcome.formal.html` (FORMAL_AUTHORITY / P2 Center Owner — kính ngữ cao, "Kính gửi chị/anh", "Trân trọng kính chào"), `welcome.informal.html` (INFORMAL_FRIEND / P1 Solo Teacher — thân mật, "Chào bạn! 👋", emoji OK, CTA hành động nhanh), `invite-staff.formal.html` (FORMAL_AUTHORITY staff invite), `invite-staff.informal.html` (INFORMAL_FRIEND staff invite). All 4 templates satisfy `vn-localization-audit-checklist.md` §2 4-section checklist (VND format / Vietnamese label / VN sample data `Trần Thị Hồng` / `Nguyễn Thị Hằng` / `Trung tâm Anh ngữ Sky Education` / VN cultural awareness Zalo contact).
  - `EmailTemplateRenderer.java`: added `TONE_SUFFIX` static `EnumMap<Tone, String>` (FORMAL_AUTHORITY→`.formal`, SEMI_FORMAL_PEER→`.semi-formal`, INFORMAL_FRIEND→`.informal`; FORMAL_SAFE_DEFAULT absent → base template); added `renderHtmlWithFallback()` (tries per-tone variant path, catches `TemplateInputException`, falls back to base template); updated `resolveTemplatePath()` (was TODO stub, now full tone-suffix dispatch); changed visibility `private` → package-private for testability.
  - `EmailTemplateRendererTest.java`: added 12 new test methods covering `resolveTemplatePath_*` (4 tests: FORMAL_AUTHORITY/INFORMAL_FRIEND/SEMI_FORMAL_PEER suffix dispatch + FORMAL_SAFE_DEFAULT base) and `render_*` (8 tests: formal/informal variant dispatch, semi-formal fallback, null tone, missing base template error, VN sample data baseline).
  - `cd kitehub && ./mvnw -pl kitehub-email test -P strict-warnings` → BUILD SUCCESS.
  - AC reframe: "live verify post-deploy persona render" moved OUT OF SCOPE (AWS GAP-612 suspended); native VN copywriter pass deferred follow-up; send-site wiring deferred Wave 108+.
  - Status: PARTIAL 95%. Coordinator to flip DONE after send-site follow-up gap filed.
- **2026-05-21 (Wave 102.9 Bucket D fix-time state-check):** Per `audit-to-gap-pipeline.md` §2.8 verified Wave 98 B1 work intact — `invite-staff.html` + `invite-staff.txt` templates exist; `Tone.java` enum exists tại `kitehub-email/src/main/java/com/kitehub/email/api/Tone.java` với FORMAL_SAFE_DEFAULT; `EmailTemplateRenderer.java` exists. Remaining AC (native VN copywriter review + persona-tone matrix doc + mvn verify) require external/live-stack work, defer. Status PARTIAL 80% retained — no progress this wave. State-check artifact: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.9-bucket-d-email-content-headers-state-check.md`. Sister to A+B+C state-check pattern.
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
- **2026-05-18 (PR #1553 merged)** — Post-merge sync per `post-merge-sync-completeness.md` §4. Paired with GAP-657 (B1). business-logic-audit + api-contract-audit DEFER to Wave 98 closure audit suite. Persona-tone send-site wiring + per-tone variant templates defer Wave 99. GAP-543 PARTIAL 40 → 80% (sister sync).
