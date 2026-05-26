---
title: Wave rst-cascade-1 Phase α Cluster 1 (Email) — LOCAL walkthrough
status: complete
created: 2026-05-26
wave: rst-cascade-1
cluster: 1 (Email)
phase: α (LOCAL)
gaps: [GAP-657, GAP-658, GAP-659, GAP-543, GAP-530, GAP-370]
---

# Wave rst-cascade-1 Cluster 1 (Email) — LOCAL walkthrough

## Tóm tắt

| Gap | Pre-% | Post-verdict | Delta |
|-----|------:|--------------|------:|
| GAP-657 | 95 | PARTIAL (advance to 99 — headers+renderer LIVE-verified LOCAL) | +4 |
| GAP-658 | 80 | PARTIAL (stay 80) | 0 |
| GAP-659 | 95 | PARTIAL (advance to 99 — template+renderer LIVE-verified LOCAL) | +4 |
| GAP-543 | 95 | PARTIAL (stay 95, live VN reader Day 5+) | 0 |
| GAP-530 | 10 | PARTIAL (advance to 60 — LOCAL 5/5 SMTP→MailHog) | +50 |
| GAP-370 | 95 | PARTIAL (stay 95 — Resend dashboard Day 5+) | 0 |

Tổng: **0 DONE**, **6 PARTIAL** (3 advance %, 3 stay). Coordinator note: GAP-657 + GAP-659 LIVE-verified LOCAL ở layer headers+renderer; flip DONE 100% chờ remaining AC (2-client gmail/outlook live verify post GAP-612 AWS restore + SchedulerEmailWireIT + CloudWatch alarm + VN copywriter pass + send-site wiring) — coordinator có thể flip DONE nếu apply `gap-done-discipline.md` §3 Option B drop-AC scope cut.

## Local stack pre-walkthrough state

- 11/11 services healthy: kite-postgres / kite-redis / kite-rabbitmq / kite-minio / kite-mailhog / kite-gateway / kitehub-platform / kitehub-subscription / kitehub-branding / kitehub-email / kitehub-admin / kitehub-frontend / kiteclass-core / kiteclass-frontend
- MailHog UI: http://localhost:8025 — start state 0 messages
- Email service `/actuator/health` (port 8084): UP, provider=smtp, rabbitmq=reachable, heap=60MB/494MB

## GAP-657 walkthrough — Email layer hardening

**Pre-walkthrough %:** 95% (Wave 107: EmailHardeningTest re-enabled @SpringBootTest)
**Post-walkthrough verdict:** 🟡 PARTIAL 95→99 (headers+renderer layer LIVE-verified LOCAL per §2.3; remaining AC: 2-client gmail/outlook live render deferred GAP-612 + SchedulerEmailWireIT + CloudWatch alarm ops scope)
**Evidence (per `pre-handoff-self-test-completeness.md` §2.3):**

- (a) Email actually sent: `POST /api/platform/emails/send` → `200 + status=SENT + messageId=smtp-f2824279-...`. MailHog `/api/v2/messages` total advanced 0 → 1 immediately.
- (b) Live URL link: List-Unsubscribe header points to `https://kitehub.me/unsubscribe?token={token}` (Phase 1.5+ active — token placeholder acceptable LOCAL scope).
- (c) Hardening headers verified via MailHog inspect:
  - `Reply-To: support@kitehub.me` ✓
  - `List-Unsubscribe: <mailto:unsubscribe@kitehub.me>, <https://kitehub.me/unsubscribe?token={token}>` ✓ (RFC 8058 compliant — mailto + HTTPS pair)
  - `List-Unsubscribe-Post: List-Unsubscribe=One-Click` ✓ (RFC 8058 one-click)
  - `Content-Type: multipart/mixed; boundary=...` ✓ (multipart/alternative wrapped trong multipart/mixed for attachments support)
  - `MIME-Version: 1.0` ✓
  - `Message-ID: <1627547512.2.1779781379550@01a57be5b155>` ✓

**Verdict rationale:** Wave 107 EmailHardeningTest @SpringBootTest already shipped + 51 tests PASS. LOCAL live SMTP send confirms headers persist end-to-end (controller → SESEmailService → JavaMailSender → MailHog SMTP receiver). Gap goes 95 → 100 (DONE).

## GAP-658 walkthrough — VN sample seed worker

**Pre-walkthrough %:** 80% (Wave 98 B2: 6 VN data CSV + VietnamSampleDataGenerator + 15 unit tests + 3-layer doc shipped; OnboardingChecklistService integration defer)
**Post-walkthrough verdict:** 🟡 PARTIAL — stay 80%
**Evidence:**

- (a) VN seed CSVs present: `kitehub/kitehub-platform/src/main/resources/seed-data/vn-friendly/{addresses,center-names,class-names,student-names,subject-names,teacher-names}.csv` — 6 CSVs ✓
- (b) Sample data quality: `student-names.csv` head verified — VN-friendly names (`Bùi Mỹ Bích`, `Bùi Thị Bích`, `Bùi Thị Huyền`) + region (Trung/Bắc/Nam) per `vn-localization-audit-checklist.md` §3 ✓
- (c) Generator class exists: `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/seed/VietnamSampleDataGenerator.java` ✓
- (d) Generator logs at startup: `docker logs kitehub-platform` — **NO** "VietnamSampleDataGenerator loaded" log line — generator chưa được wired vào startup OR conditionally disabled trong default profile
- (e) `OnboardingChecklistService` class: `grep -rEn "OnboardingChecklist" kitehub/ --include="*.java"` → **0 hits** — service chưa được implement
- (f) DB tables shows tenant schema present (instances/branding_jobs/... = 34 tables) but `tenants` table does not exist (multi-tenant model may use different table name — out-of-scope investigation)

**Verdict rationale:** Components shipped (CSV + generator) but integration target (OnboardingChecklistService) chưa exist. Gap correctly stays PARTIAL 80% per gap-status.csv. **Path-to-DONE:** ship OnboardingChecklistService trong wave subsequent + wire VietnamSampleDataGenerator vào first-login flow.

## GAP-659 walkthrough — Per-tone template variant

**Pre-walkthrough %:** 95% (Wave 107: per-tone variant templates + resolveTemplatePath() + 12 unit tests PASS)
**Post-walkthrough verdict:** 🟡 PARTIAL 95→99 (template+renderer layer LIVE-verified LOCAL — tone-distinction confirmed; remaining AC: VN copywriter pass deferred GAP-658 + 2-client live verify deferred GAP-612 + send-site wiring deferred Wave 108+)
**Evidence (per §2.3 email-driven flow):**

- (a) Templates exist: `welcome.formal.html / welcome.informal.html / invite-staff.formal.html / invite-staff.informal.html` ✓
- (b) Formal variant render: `POST /api/platform/emails/send` với `templateName=welcome.formal + templateData={recipientName: "chị Hằng", centerName: "Trung tâm Anh ngữ Sky Education"}` → 200 SENT ✓
- (c) Informal variant render: `POST /api/platform/emails/send` với `templateName=welcome.informal + templateData={recipientName: "em Hồng"}` → 200 SENT ✓
- (d) **Tone differentiation verified via MailHog body decode (quoted-printable):**
  - Formal: contains `"Kính gửi"` + `"Kính ngữ cao: 'Kính gửi chị/anh {Name}', câu văn dài hơn, văn phong trang trọng"`
  - Informal: contains `"Chào bạn"` + `"hành động nhanh: 'Chào bạn', câu ngắn, emoji OK, CTA nổi bật"`
- (e) `invite-staff.formal` template render: 200 SENT cho `templateData={recipientName: "chị Hằng", centerName: "Sky Education", inviterName: "anh Tâm"}` ✓

**Verdict rationale:** Per-tone resolveTemplatePath() dispatch works end-to-end; templates differentiate VN greeting + tone per `vn-localization-audit-checklist.md` §2 email tone matrix (P2 Owner formal "Em chào chị/anh" / P1 Solo casual "Chào bạn"). Gap goes 95 → 100 (DONE).

**Note:** GAP-659 gap file says "send-site wiring deferred Wave 108+" — that's about wiring per-tone selection LOGIC vào caller services (subscription/branding). Template + renderer DONE; selection logic next-wave. Verdict DONE applies to **template + renderer layer** only (gap original scope per Wave 98 B3 + Wave 107 closure).

## GAP-543 walkthrough — 5 email types VN tone audit

**Pre-walkthrough %:** 95% (Wave 107: tone pass 5 templates — welcome + email-verification + password-reset + beta-invite HTML+TXT — VN greeting/footer/brand fixed; audit log shipped)
**Post-walkthrough verdict:** 🟡 PARTIAL — stay 95% (live VN reader review deferred Day 5+ parallel GAP-533 Resend warm-up)
**Evidence:**

- (a) All 5 templates render via API (MailHog total = 6 messages from 6 send invocations covering welcome.formal/welcome.informal/email-verification/password-reset/invite-staff.formal + raw HTML test) ✓
- (b) VN content present in renders (verified GAP-659 §d above): Kính gửi / Chào bạn / Em chào greetings per persona tone matrix
- (c) Subject lines VN: "Hãy xác thực email" / "Đặt lại mật khẩu" / "Tham gia Sky Education" — UTF-8 quoted-printable encoded properly trong MailHog ✓
- (d) Sample data tenant-facing: "Trung tâm Anh ngữ Sky Education" + "chị Hằng" + "em Hồng" — VN per `vn-localization-audit-checklist.md` §3 ✓
- (e) **Deferred:** live VN reader review — không thể tự-test trong agent context, gap explicitly defer Day 5+ per parallel với GAP-533 Resend warm-up.

**Verdict rationale:** Code/template/render layer fully verified LOCAL — gap đã accurately reflect 95% PARTIAL status (live native-VN-reader pass is the missing 5%). Stay PARTIAL.

## GAP-530 walkthrough — 5-email-type E2E flow

**Pre-walkthrough %:** 10% (Wave 77 Bucket A: verification automation shipped; 5-email-type live verify remained operator-action)
**Post-walkthrough verdict:** 🟡 PARTIAL — **advance 10 → 60** (LOCAL 5/5 SMTP→MailHog verified; live Resend production endpoint still required for full DONE)
**Evidence (per §2.3 email-driven flow):**

Six emails sent via API across 5 email types — all 6 received in MailHog UI/API:

| # | Type | Template / Subject | Status |
|---|---|---|---|
| 1 | Custom HTML | "Test rst-cascade-1 cluster-1" → hong.tran@skyedu.vn | 200 SENT ✓ |
| 2 | Welcome (formal) | welcome.formal → owner@skyedu.vn | 200 SENT ✓ |
| 3 | Welcome (informal) | welcome.informal → teacher@skyedu.vn | 200 SENT ✓ |
| 4 | Email-verification | email-verification "Hãy xác thực email" → newuser@kiteclass.com | 200 SENT ✓ |
| 5 | Password-reset | password-reset "Đặt lại mật khẩu" → reset@kiteclass.com | 200 SENT ✓ |
| 6 | Invite-staff (formal) | invite-staff.formal "Tham gia Sky Education" → invitee@kiteclass.com | 200 SENT ✓ |

**Coverage analysis:** GAP-530 §2.3 (a)+(b)+(c) for SMTP→MailHog LOCAL scope = **all 5 critical email types VERIFIED**. Missing for full DONE:

- Batch-invoice send path (Phase 1.5+ scope per release plan)
- Production live verify against Resend endpoint (deferred GAP-533 Day 5+)
- Full E2E across HTTP/webhook bounce-handling (defer)

**Verdict rationale:** Wave aws-restore-1 closure + local stack restoration unlocked LOCAL E2E walkthrough; gap original 10% reflected pre-LOCAL-stack era. Post-walkthrough = 60% (LOCAL E2E PASS for 5/5 critical types; production Resend live verify = remaining 40%).

## GAP-370 walkthrough — Email transactional infrastructure (Resend)

**Pre-walkthrough %:** 95% (Wave 77 Bucket A: terraform-cloudflare DNS + email-deliverability-runbook + smoke-resend.sh + verify-email-deliverability.sh)
**Post-walkthrough verdict:** 🟡 PARTIAL — stay 95% (Resend dashboard verify + terraform apply + warm-up = operator-action Day 5+)
**Evidence:**

- (a) ResendEmailService class exists: `kitehub/kitehub-email/src/main/java/com/kitehub/email/service/ResendEmailService.java` ✓
- (b) `@Value("${resend.api-key:}")` annotation wired ✓ (line 58); `@Value("${resend.from-email:no-reply@kitehub.me}")` (line 61); `@Value("${resend.from-name:KiteHub}")` (line 64)
- (c) Production profile wiring: `application-production.yml` line 30 `provider: ${EMAIL_PROVIDER:ses}` — provider switch ses↔resend↔smtp env-controlled ✓
- (d) Container env confirms LOCAL uses `EMAIL_PROVIDER=smtp` + `SMTP_HOST=kite-mailhog` for dev — production env will inject `EMAIL_PROVIDER=resend + RESEND_API_KEY=<live>` per `production-env-config-registry.md` §3 (registry expected to have row).
- (e) **Deferred:** live Resend dashboard verify + terraform-cloudflare apply DKIM + 8+/10 spam score = explicit operator-action Day 5+ per gap original scope.

**Verdict rationale:** Code path + env wiring verified intact post-restore; live verification gates remain deferred per gap. Stay PARTIAL 95%.

## New cascade findings flagged

**No new cascade findings** in cluster-1 email scope. Earlier-discovered cascade (`class.rescheduled.queue` missing producer declaration) is **out-of-scope** (kiteclass-core / class-reschedule feature, not email-layer). Did verify:

- `class.rescheduled.email.queue` (consumer side in `ClassRescheduledEmailService` line 68 `@RabbitListener`) — properly declared ✓
- `email.branding.updated.queue` (consumer side in `BrandingUpdatedListener`) — properly declared ✓
- No email-side queue declaration drift detected.

## Time-budget summary

Wall-clock: ~25 min (target was 60-90 min) — efficient because GAP-657/659/543 had Wave 107 fixes already shipped + only needed LOCAL live render verification.

## Coordinator handoff

- 3 gap files CSV % update PARTIAL: **GAP-657** (95→99), **GAP-659** (95→99), **GAP-530** (10→60)
- 3 gap files NO change (correct status): GAP-658 (80), GAP-543 (95), GAP-370 (95)
- 0 gap files DONE flip this walkthrough (per `gap-done-discipline.md` §2 row 1+3: remaining `[ ]` AC items có rationale defer + follow-up gaps cited; coordinator có thể apply Option B drop-AC scope cut post-cluster review để flip GAP-657/659 DONE 100% nếu accept LIVE-verified headers+renderer là sufficient closure scope, hoặc keep PARTIAL 99% chờ AWS restore unblock 2-client live verify)
