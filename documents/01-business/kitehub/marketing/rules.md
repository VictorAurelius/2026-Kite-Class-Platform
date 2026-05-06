# Marketing — Business Rules

**Domain:** KiteHub Marketing (public surfaces — landing, blog, pricing, catalog)
**Version:** 1.0
**Updated:** 2026-05-06
**Last verified:** 2026-05-06

Cross-product canonical: rules trong file này áp dụng cho cả KiteHub marketing surfaces lẫn KiteClass tenant marketing surfaces (`kiteclass/marketing/rules.md` cross-link tới đây cho `BR-PDPL-CONSENT-*` để tránh drift). Existing KC marketing rules (`BR-MKT-001..024` cho contact / lead / landing) vẫn ở `kiteclass/marketing/rules.md`.

---

## 1. Rules

### PDPL Consent Banner Rules

Per `business-logic-review.md` v1.0.0 §2 (5-attribute mandate). Implements PDPL 2023 + Decree 13/2023/NĐ-CP cho mọi public marketing surface.

#### BR-PDPL-CONSENT-001: Cookie consent banner mandatory on public marketing surfaces

- **Value:** Banner BẮT BUỘC hiển thị trên first visit của public marketing routes — KiteHub: `/`, `/blog`, `/pricing`, `/about`, `/contact`; KiteClass: `/`, `/catalog`, `/about`, `/contact` (config key: `kite.consent.banner.public-routes`)
- **Source:** **VN law / regulation** — Luật Bảo vệ Dữ liệu Cá nhân (PDPL 2023) **Article 11(1)** (data subject must be notified before personal data is processed) + **Decree 13/2023/NĐ-CP Article 17** (notice requirement preceding processing). Industry standard: GDPR-style consent banners are de-facto standard for VN-facing SaaS post-PDPL announcement (Hotmart VN, Teachable VN, Got It pre-2026 all show banners).
- **Rationale:** PDPL Art 11 prohibits processing personal data without notice + consent. Marketing surfaces collect at minimum analytics cookies (visitor identification across pages = personal data per Art 2.1 definition). Without banner, every public-route load = one PDPL violation. Banner trên first-visit + persisted decision = compliant minimum. Why "first visit" not "every visit": Art 11 requires notice once + remember-the-choice; per-visit re-prompting would violate Art 13 user-experience guidance (consent revocation must be at least as easy as giving consent — implies stable persisted choice).
- **Reviewer:** @nguyenvankiet (acting **Compliance scout + Product Owner**, solo-dev, 2026-05-06). Legal counsel formal review queued via **GAP-182 Phase 2** (Privacy Policy + DPO designation) + **GAP-156** (quarterly business-correctness audit). Consumer-Protection-Law reviewer not applicable (consent banner is privacy-domain, not consumer-rights-domain).
- **Compliance check:** **Compliant** — PDPL 2023 Art 11(1) (notice before processing) + Decree 13/2023/NĐ-CP Art 17 (consent collection mechanism). Cybersecurity Law (Luật An ninh mạng 2018) Considered — not triggered because banner does not relate to data localization. Consumer Protection Law 2023 Considered — not triggered because banner is not pricing display / refund policy.
- **Review cadence:** **Annual** + **event-driven**. **Next review:** 2027-05-06 OR within 30 days of any of: (a) PDPL amendment publication, (b) Decree 13/2023 implementing-decree publication, (c) MPS A05 (Cybersecurity Department) public guidance on consent UX, (d) competitor compliance pattern shift signal (≥2 of top-3 VN edu-SaaS competitors change banner UX).
- **Code reference:** `packages/shared-ui/src/components/ConsentBanner/index.tsx` (component) + `kitehub-frontend/src/app/(public)/layout.tsx` + `kiteclass-frontend/src/app/(public)/layout.tsx` (mount points).

---

#### BR-PDPL-CONSENT-002: Granular consent toggles + no dark patterns

- **Value:** **3 categories** — `essential` (always-on, info-only, locked toggle) / `analytics` (opt-in, default-off) / `marketing` (opt-in, default-off). **3 CTAs** với equal visual weight: `"Từ chối tất cả"` / `"Đồng ý tất cả"` / `"Tuỳ chỉnh"` (config key: `kite.consent.banner.categories` + `kite.consent.banner.cta-labels`). "Equal visual weight" định nghĩa: cùng button size (min-width, padding), cùng color saliency (no greyed-out Reject), cùng position priority (left-to-right không hide Reject ngoài viewport).
- **Source:** **VN law / regulation** — PDPL 2023 **Article 11(2)** (granular consent for each processing purpose) + Decree 13/2023/NĐ-CP **Article 13** (consent must be specific and informed for each purpose, không gộp một-cú-click cho tất cả purposes). **Industry standard** — GDPR Art 7(2) (consent must be freely given, no pre-ticked boxes; tham chiếu vì PDPL drafted under GDPR influence; CNIL France 2022 + ICO UK 2023 enforcement actions established "no dark patterns" precedent for consent UX). **Industry standard** — IAB Europe TCF 2.2 (3-cookie-category model: necessary / preferences-analytics / advertising-marketing).
- **Rationale:** PDPL Art 11(2) cấm gộp consent: 1 click = consent cho mọi purpose là vi phạm. Granular = mỗi purpose tách toggle riêng, user có quyền opt-in từng cái. Why exactly 3 categories (not 2, not 5): (a) `essential` không bao giờ opt-out (kỹ thuật cookie cho session, CSRF, language preference — không cần consent vì legal-basis là legitimate-interest per Art 11(3) exemption); (b) `analytics` riêng vì purpose khác (đo traffic, cải tiến UX) ≠ marketing; (c) `marketing` riêng vì purpose là re-targeting / behavioural ads, đối tượng risk PDPL cao nhất. Splitting analytics + marketing thành nhiều sub-toggles tạo cognitive overload → người dùng paradoxically click "Accept all" để skip → ngược lại purpose Art 11. Why equal visual weight: dark-pattern A/B tests (Nielsen Norman Group 2023) show "Reject" button buried/greyed → 5-15% reject rate vs. 50%+ when equal-weight. Reject-rate manipulation = de-facto "consent-forced" = violation Art 11(2) "freely given" requirement.
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev, 2026-05-06). Legal counsel + UX-research formal review queued via **GAP-182 Phase 2** + **GAP-156**. UX-research deferral noted because dark-pattern detection requires user testing infra not available solo-dev mode.
- **Compliance check:** **Compliant** — PDPL 2023 Art 11(2) (granular consent per purpose) + Decree 13/2023 Art 13 (specific + informed for each purpose). Aligned-with (not strictly-required-by-VN-law) — GDPR Art 7(2) (no dark patterns) precedent.
- **Review cadence:** Same as 001 — Annual + event-driven. **Next review:** 2027-05-06 OR within 30 days of: PDPL amendment, Decree 13/2023 implementing-decree, MPS A05 dark-pattern guidance publication, ≥1 VN regulator enforcement action against dark-pattern banner.
- **Code reference:** `packages/shared-ui/src/components/ConsentBanner/types.ts` (`ConsentCategory` enum) + `packages/shared-ui/src/components/ConsentBanner/ConsentBanner.tsx` (3 CTAs render).

---

#### BR-PDPL-CONSENT-003: Consent record retention 36 months

- **Value:** **36 months** sau ngày user record consent (banner click) HOẶC sau ngày user revoke consent (whichever later) (config key: `kite.consent.record.retention-months`). Áp dụng cho cả LocalStorage decision record (client-side) lẫn server-side consent audit log (when GAP-353b Phase 2 ships server-side API).
- **Source:** **VN law / regulation** — PDPL 2023 **Article 6** (retention only as long as necessary for processing purpose) + **Article 23** (minimum retention for service-related personal data). **VN law** — **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 Article 12** (consumer dispute window 24 months). **Sister-rule reference** — `business-logic-review.md` v1.0.0 §4.3 worked example "DR-03 Personal data retention period" (36 months pattern with same 24mo + buffer rationale). Cross-link target: `documents/01-business/kitehub/data-retention/rules.md` + `documents/01-business/kiteclass/data-retention/rules.md` — consent records flow through same retention pipeline as other PII (RET-04 PREMIUM 60d / RET-05 ENTERPRISE 90d cover live-tenant data; consent records get longer 36mo because they're proof-of-compliance not active-tenant data).
- **Rationale:** Why 36 months not 24, not 60: Consumer Protection Law dispute window 24mo → bằng chứng consent phải tồn tại tối thiểu 24mo (chứng minh tại thời điểm xảy ra dispute, công ty đã có consent valid). PDPL Art 6 yêu cầu KHÔNG retain quá purpose: với consent records, purpose = legal proof-of-compliance, hết khi dispute window hết. 36mo = 24mo dispute + 12mo buffer cho late-arriving complaints + slow regulator inquiry response. Storage cost negligible (1 record per user per consent-event ≤ 1KB JSON; 100k users × 5 events/year × 1KB × 3 years = ~1.5GB total — trivial). Why not 60mo (PDPL DR-03 pattern for active PII): consent records không phải active PII, không phải tax records (không cần 7-year retention per `Luật Quản lý Thuế`). 60mo = over-retention vi phạm Art 6 minimum-necessary principle. Why not 24mo flat: zero buffer = late-arriving dispute filed day 730 không có evidence → noncompliance risk.
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Legal scout + Compliance + Product Owner, solo-dev, 2026-05-06). Formal legal counsel review queued via **GAP-182 Phase 2** + **GAP-156**. Tax-advisor review NOT required (consent records không phải tax records).
- **Compliance check:** **Compliant** — PDPL 2023 Art 6 (proportionate retention) + Art 23 (minimum legal retention service-data) + Consumer Protection Law 2023 Art 12 (24mo dispute window covered). Tax law (Luật Quản lý Thuế 2019) Considered — not triggered (consent records ≠ financial records).
- **Review cadence:** **Annual** + event-driven. **Next review:** 2027-05-06 OR within 30 days of: PDPL amendment changing Art 6 retention principle, Consumer Protection Law amendment changing dispute window, Decree 13/2023 retention-specific implementing-decree publication.
- **Code reference:** `packages/shared-ui/src/components/ConsentBanner/storage.ts` (LocalStorage TTL helper) + server-side endpoint `POST /api/v1/consent/record` (Phase 2 shipped Wave 25 Bucket A — GAP-353b).
- **Implementation (Phase 2 — Wave 25 Bucket A):**
  - Backend: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/controller/ConsentController.java` (3 endpoints) + `consent/service/ConsentServiceImpl.java` (idempotent upsert) + `consent/cron/ConsentRetentionCron.java` (DR-03 36-month purge, daily 03:00) + Flyway `V25__create_consent_record.sql`.
  - Frontend: `packages/shared-ui/src/components/ConsentBanner/api.ts` (best-effort fetch wrappers) + `useConsent.ts` (LocalStorage primary + server-side audit trail).
  - API contract: [`./api-contract.md`](./api-contract.md).

---

#### BR-PDPL-CONSENT-004: Consent revocation flow

- **Value:** Revocation MUST be at least as easy as giving consent. **3 access paths**: (a) Settings / Privacy page với "Cookie preferences" section, (b) banner re-trigger từ footer link "Cài đặt cookies" available trên mọi public route, (c) browser-side LocalStorage clear ngay lập tức triggers re-prompt. Re-prompt **automatic** sau: (i) 12-month default expiration (config key: `kite.consent.banner.expiry-months`, default `12`), (ii) material policy change (defined as: privacy-policy.md content hash change OR cookie category list expand). On revocation: LocalStorage entry cleared, analytics scripts unloaded từ DOM (script tags removed), cookies category-marked-marketing/analytics deleted client-side.
- **Source:** **VN law / regulation** — PDPL 2023 **Article 13** (right to withdraw consent) + Decree 13/2023/NĐ-CP **Article 18** (revocation mechanism must not be more burdensome than initial consent). **Industry standard** — GDPR Art 7(3) precedent (consent withdrawal as easy as giving) — non-binding for VN but cited because PDPL drafted under GDPR influence. **Sister-rule** — `kitehub/data-retention/rules.md` BR-RET-001 (7-day grace + reversible cancelDeletion) demonstrates same "reversibility-as-easy-as-action" principle for adjacent privacy-domain rule.
- **Rationale:** PDPL Art 13 right-to-withdraw is binding mandate. Decree 13/2023 Art 18 cấm "revocation harder than consent" (vd. consent = 1 click banner; revocation buried 5-clicks-deep settings menu = violation). Why exactly 3 access paths: (a) Settings page = canonical "I want to manage privacy" entrypoint (matches mental model — privacy preferences = account settings); (b) Footer link = always-available re-trigger (avoid forcing user vào Settings nếu user just want to change toggle once); (c) LocalStorage clear = developer/power-user fallback + auto-trigger on browser private-mode reset. Why 12-month default expiration: (i) GDPR Art 7(2) "consent should not be permanent" — per WP29 Opinion 5/2020 implies periodic re-consent; PDPL drafted similar; 12mo = annual = matches user's mental "yearly review" cadence; (ii) shorter (e.g. 6mo) = annoying re-prompts = user blindly clicks "Accept all" = consent fatigue = de-facto invalid consent; (iii) longer (e.g. 24mo) = stale consent — user forgot what they agreed to, particularly risky if cookie categories expand mid-window. Why "material policy change" force re-prompt: PDPL Art 11 yêu cầu informed consent per current policy — nếu policy change, prior consent stops being "informed" → must re-prompt. Definition "policy content hash change" gives concrete trigger; "cookie category expand" handles common case (adding marketing category mid-quarter). Why "scripts unloaded từ DOM": consent revocation phải cũng halt active processing (Art 13(1) "stops processing"), không chỉ ngăn future load — nếu Google Analytics đã loaded, just setting flag không đủ, phải remove script tag + clear globals.
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Compliance scout + Product Owner + Tech Lead, solo-dev, 2026-05-06). Legal counsel + UX-research formal review queued via **GAP-182 Phase 2** + **GAP-156**. Tech Lead hat worn for "scripts unloaded from DOM" technical mechanism — verified achievable via React effect cleanup + explicit `script.remove()`.
- **Compliance check:** **Compliant** — PDPL 2023 Art 13(1) (right to withdraw) + Decree 13/2023 Art 18 (revocation mechanism parity). Aligned-with — GDPR Art 7(3) (withdrawal ease).
- **Review cadence:** **Annual** + event-driven. **Next review:** 2027-05-06 OR within 30 days of: PDPL amendment changing Art 13, Decree 13/2023 Art 18 implementing-decree, MPS A05 revocation-UX guidance, ≥1 VN regulator enforcement action against burdensome-revocation pattern.
- **Code reference:** `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` (revocation hook + DOM script cleanup) + future Settings page route TBD Phase 2 (GAP-353b).

---

### PDPL DSAR (Data Subject Access Request) Rules

Per `business-logic-review.md` v1.0.0 §2 (5-attribute mandate). Implements PDPL 2023 Article 14 (six data-subject rights) + Decree 13/2023/NĐ-CP Article 19 (response SLA) for the self-service intake form shipped Wave 26 Bucket A (GAP-353c).

#### BR-PDPL-DSAR-001: Six PDPL Art 14 rights enumerated as right_type

- **Value:** DSAR ticket `right_type` enum chứa **đúng 6 giá trị**: `ACCESS` (quyền truy cập), `RECTIFICATION` (quyền chỉnh sửa), `ERASURE` (quyền xoá), `PORTABILITY` (quyền chuyển dữ liệu), `RESTRICT` (quyền hạn chế xử lý), `OBJECT` (quyền phản đối xử lý). Implemented as Java enum `DsarRightType` + Postgres VARCHAR(50) constraint via JPA `@Enumerated(EnumType.STRING)`.
- **Source:** **VN law / regulation** — PDPL 2023 **Article 14(1)–(6)** liệt kê chính xác 6 quyền: Art 14(1) right to information / access; Art 14(2) right to rectify; Art 14(3) right to erasure; Art 14(4) right to data portability; Art 14(5) right to restrict processing; Art 14(6) right to object. Decree 13/2023/NĐ-CP **Articles 9–14** chi tiết hoá từng quyền. Industry standard: GDPR Articles 15–22 6-rights pattern (PDPL Art 14 drafted parallel).
- **Rationale:** Why exactly 6 (not 4, not 8): PDPL Art 14 statute fixes the count — adding rights beyond statute creates user expectations the law doesn't back; trimming to fewer rights = noncompliance. Why enum (not free-text): structured enum unblocks (a) DPO triage routing per right type, (b) statistics + reporting per Art 14 row, (c) future automation (e.g. ERASURE → trigger DR-03 retention sweep). Why English enum names + Vietnamese FE labels: code-side English matches PDPL official translation; FE labels Vietnamese-first per CLAUDE.md communication-language rule. Why sub-divide ACCESS vs PORTABILITY: PDPL Art 14(1) "view what is held" ≠ Art 14(4) "machine-readable export" — enum captures the legal distinction so DPO response template differs.
- **Reviewer:** @nguyenvankiet (acting **Compliance scout + Product Owner + Tech Lead**, solo-dev, 2026-05-06). Legal counsel formal review queued via **GAP-182 Phase 2** + **GAP-156**.
- **Compliance check:** **Compliant** — PDPL 2023 Art 14(1)–(6) (six rights enumeration) + Decree 13/2023/NĐ-CP Art 9–14 (per-right scope).
- **Review cadence:** **Annual** + event-driven. **Next review:** 2027-05-06 OR within 30 days of: PDPL amendment changing Art 14 right list, Decree 13/2023 Art 9–14 implementing-decree updates, MPS A05 DSAR-handling guidance.
- **Code reference:** `kitehub-subscription/src/main/java/com/kitehub/subscription/dsar/entity/DsarRightType.java` + V26 migration `right_type VARCHAR(50)` + FE form radio in `kitehub-frontend/src/app/(public)/legal/data-rights/DataRightsForm.tsx` + KC twin.

---

#### BR-PDPL-DSAR-002: 20-day SLA from submission to response

- **Value:** DPO response **trong vòng 20 ngày kể từ ngày submit DSAR** (config: `sla_deadline = created_at + INTERVAL '20 days'` — server-side computed at insert; not user-overridable). `SlaTimerCron` daily 04:00 logs ERROR cho tickets `PENDING|IN_REVIEW` quá deadline. Hard-stop = 30 ngày tối đa nếu request phức tạp (cần legal review hoặc cross-tenant queries) — DPO phải proactively notify requester ≤ ngày 15 nếu cần kéo dài.
- **Source:** **VN law / regulation** — Decree 13/2023/NĐ-CP **Article 19** (controller phản hồi DSAR trong 20 ngày, kéo dài thêm tối đa 10 ngày nếu phức tạp + thông báo lý do). PDPL 2023 **Article 14(7)** (cross-reference: response timeline per implementing decree). **Industry standard** — GDPR Art 12(3) (1 month default + 2 months extension) — non-binding cho VN nhưng cite vì PDPL drafted under GDPR influence; chosen 20d (PDPL stricter than GDPR's 30d) to stay compliant với cả hai chế độ nếu KiteClass expand outside VN.
- **Rationale:** Why exactly 20 days: Decree 13/2023 Art 19 binding — không phải convention. Why hard-coded vs config: SLA là legal mandate, không phải tier benefit; cho phép tenant tuỳ chỉnh = creating "fast SLA tier" ≠ compliant. Why DPO notify ≤ ngày 15 nếu cần extension: Art 19 yêu cầu thông báo + lý do trước khi extension activates; ngày 15 = buffer 5 ngày để DPO assemble extension request + có ngày 16-20 để send. Why 30-day hard-stop: Art 19 cho phép extend "tối đa 10 ngày" → 20 + 10 = 30 ngày là ceiling tuyệt đối. Why log ERROR (not auto-escalate): DPO triage stays human-in-the-loop; auto-escalate = process step không backed bởi formal counsel review (deferred GAP-182 Phase 2).
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev, 2026-05-06). Legal counsel formal review queued via **GAP-182 Phase 2** + **GAP-156**.
- **Compliance check:** **Compliant** — PDPL 2023 Art 14(7) + Decree 13/2023/NĐ-CP Art 19 (20-day default + 10-day extension cap with notification).
- **Review cadence:** **Annual** + event-driven. **Next review:** 2027-05-06 OR within 30 days of: Decree 13/2023 Art 19 amendment, MPS A05 SLA-extension procedure publication, ≥1 enforcement action against late-DSAR-response.
- **Code reference:** `kitehub-subscription/src/main/java/com/kitehub/subscription/dsar/entity/DsarTicket.java#onCreate` (SLA computed `now + 20 days`) + `dsar/cron/SlaTimerCron.java` (overdue log) + V26 migration `sla_deadline TIMESTAMP NOT NULL`.

---

#### BR-PDPL-DSAR-003: Identity verification via national_id_last4 + DPO callback

- **Value:** DSAR submission yêu cầu **4 chữ số cuối CCCD/CMND + email + họ tên** — KHÔNG yêu cầu full national ID. DPO callback verify danh tính out-of-band (phone hoặc email reply) trước khi xử lý ACCESS / ERASURE / PORTABILITY (rights mà việc giả mạo gây thiệt hại). RECTIFICATION / RESTRICT / OBJECT có thể proceed trên 3-field basic match nếu request không impact data integrity.
- **Source:** **VN law / regulation** — PDPL 2023 **Article 14** (controller phải xác thực danh tính trước khi disclose / mutate dữ liệu cá nhân) + Decree 13/2023/NĐ-CP **Article 24** (data minimization principle — chỉ thu thập tối thiểu cần thiết). **Industry standard** — Hotmart VN DSAR flow (4-digit last + email confirm), Teachable VN (email + last 4 SSN equivalent). **GDPR precedent** — Art 12(6) (controller may request additional info to confirm identity but không vượt mức cần thiết).
- **Rationale:** Why 4 digits cuối (not full ID): full CCCD/CMND = sensitive PII per Decree 13/2023 Art 28 → thu thập = trigger registration requirement nếu volume cao. 4 chữ số last = sufficient để cross-check với Tenants's existing PII (nếu DSAR submitter là user đã sign up, KiteHub lưu full CCCD ở user profile và verify match) mà không phát sinh thêm sensitive-PII collection. Why email + name + 4-digit (3-field): one-field (email only) = trivially impersonatable; 3-field = increases attacker work-factor đủ cao để legitimate self-service. Why DPO callback for ACCESS/ERASURE/PORTABILITY (not RECTIFICATION/RESTRICT/OBJECT): impersonation attack vectors khác nhau — disclose / delete / export tạo thiệt hại nếu attacker; rectify / restrict / object reversible. Cost-benefit: callback overhead 5-10 phút × ACCESS/ERASURE/PORTABILITY rate (low) acceptable; rectify-restrict-object higher volume + lower attack cost.
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Compliance scout + Product Owner + Tech Lead, solo-dev, 2026-05-06). Legal counsel formal review queued via **GAP-182 Phase 2** + **GAP-156**.
- **Compliance check:** **Compliant** — PDPL 2023 Art 14 (identity verification requirement) + Decree 13/2023/NĐ-CP Art 24 (data minimization). Aligned-with — GDPR Art 12(6).
- **Review cadence:** **Annual** + event-driven. **Next review:** 2027-05-06 OR within 30 days of: PDPL amendment, Decree 13/2023 Art 24 implementing-decree, ≥1 enforcement action against under-verification (false-disclose) hoặc over-verification (excess-collection).
- **Code reference:** `dsar/dto/DsarRequest.java` (`@Pattern(regexp = "^[0-9]{4}$")`) + V26 migration `national_id_last4 VARCHAR(4) NOT NULL` + DPO callback procedure documented in DPIA (GAP-353d Bucket B Wave 26).

---

#### BR-PDPL-DSAR-004: DSAR ticket retention 36 months from resolved_at

- **Value:** DSAR ticket retention = **36 months kể từ resolved_at** (hoặc kể từ created_at nếu ticket vẫn open quá 1 năm — overdue cron alert). Sister-rule với BR-PDPL-CONSENT-003 (consent records 36mo) + DR-03 (PII retention 36mo). Hard-delete cron deferred to follow-up gap; meanwhile manual purge.
- **Source:** **VN law / regulation** — PDPL 2023 **Article 6** (retention only as long as necessary) + **Article 23** (minimum retention service-related data) + Consumer Protection Law 2023 **Article 12** (24mo dispute window). **Sister-rule** — `business-logic-review.md` §4.3 worked example DR-03 + BR-PDPL-CONSENT-003 (36mo pattern, identical legal mechanics).
- **Rationale:** Identical legal mechanics to BR-PDPL-CONSENT-003: dispute window 24mo + 12mo buffer = 36mo. DSAR records are proof-of-compliance evidence (chứng minh KiteHub đã respond đúng SLA + đúng scope) — same purpose category as consent records. Why 36mo từ resolved_at (not created_at): un-resolved tickets mean DPO chưa close the loop; nếu ticket open quá 365d, system flags as anomaly (not silently purge un-handled requests). Storage cost: 1 ticket ≈ 2KB JSON; 100k users × 0.5 DSAR/year × 2KB × 3 years = ~300MB — trivial.
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Legal scout + Compliance + Product Owner, solo-dev, 2026-05-06). Legal counsel formal review queued via **GAP-182 Phase 2** + **GAP-156**.
- **Compliance check:** **Compliant** — PDPL 2023 Art 6 + Art 23 + Consumer Protection Law 2023 Art 12. Tax law (Luật Quản lý Thuế 2019) Considered — not triggered.
- **Review cadence:** **Annual** + event-driven. **Next review:** 2027-05-06 OR within 30 days of: PDPL amendment changing Art 6 retention principle, Consumer Protection Law amendment changing dispute window.
- **Code reference:** V26 migration `resolution TEXT NULL` + `resolved_at TIMESTAMP NULL` + future `DsarRetentionCron` (follow-up gap, Wave 27+).

---

#### BR-PDPL-DSAR-005: Honeypot anti-spam on public DSAR endpoint

- **Value:** DSAR submit form có **hidden honeypot field** `companyWebsite` — invisible to real users (CSS `hidden` + `position: absolute; left: -9999px` + `tabindex=-1` + `aria-hidden="true"`). Server reject HTTP 400 nếu `companyWebsite` non-empty. reCAPTCHA NOT required cho v1 (key not yet provisioned); future enhancement nếu spam volume > 10/day. Rate-limit at gateway (out of scope this rule).
- **Source:** **Industry standard** — OWASP Cheat Sheet "Forgot Password" (honeypot pattern for unauthenticated public forms). **Sister-rule** — KiteHub gateway rate-limit policy (existing). **Operational rationale** — public DSAR endpoint = unauthenticated → bot vector for either (a) DOS by submission flood, (b) spam DPO inbox via reflection.
- **Rationale:** Why honeypot (not reCAPTCHA): reCAPTCHA requires Google API key (cost: free tier limited; enterprise paid; key provisioning out of solo-dev scope) + degrades UX với extra friction. Honeypot zero-friction cho real users. Why field name `companyWebsite` (vs generic `honeypot`): named realistically tricks naive bots to fill; bots that recognize "honeypot" naming skip. Why HTTP 400 (vs 429 rate-limit hoặc silent log + accept): explicit reject deters retry; 429 implies "try later"; silent accept = inflates ticket queue with bot rows. Why CSS `hidden` + `position absolute` + `tabindex=-1` + `aria-hidden`: belt-and-suspenders against accessibility tools — screen readers shouldn't read field, keyboard users shouldn't tab to it, assistive tech shouldn't expose it.
- **Reviewer:** Same as 001 — @nguyenvankiet (acting Compliance scout + Product Owner + Tech Lead, solo-dev, 2026-05-06). UX-research formal review queued via **GAP-156**.
- **Compliance check:** **N/A** — anti-spam mechanism not directly regulated by PDPL/Decree 13. WCAG-compliance check `aria-hidden` + tab-order verified manually.
- **Review cadence:** **Annual** + event-driven on bot-volume threshold (≥10 honeypot rejections/day → re-evaluate adding reCAPTCHA).
- **Code reference:** `dsar/dto/DsarRequest.java` (`companyWebsite` field + service-layer reject) + FE `DataRightsForm.tsx` honeypot div (KH + KC twins).

---

## 2. Config Keys

| Key | Default | Description |
|-----|---------|-------------|
| `kite.consent.banner.public-routes` | `["/", "/blog", "/pricing", "/about", "/contact", "/catalog"]` | Routes nơi banner phải hiển thị first-visit |
| `kite.consent.banner.categories` | `["essential", "analytics", "marketing"]` | 3-category granular consent model |
| `kite.consent.banner.cta-labels` | VN: `"Từ chối tất cả" / "Đồng ý tất cả" / "Tuỳ chỉnh"` | Equal-weight CTA copy |
| `kite.consent.record.retention-months` | `36` | Consent record retention (BR-PDPL-CONSENT-003) |
| `kite.consent.banner.expiry-months` | `12` | Re-prompt cadence default (BR-PDPL-CONSENT-004) |
| `kite.consent.banner.storage-key` | `kite.consent.v1` | Versioned LocalStorage key (BR-PDPL-CONSENT-003 + 004) |

---

## 3. Cross-link to KiteClass marketing rules

`documents/01-business/kiteclass/marketing/rules.md` cross-links tới file này cho `BR-PDPL-CONSENT-*` (canonical here, KC inherits). Existing `BR-MKT-001..024` (contact / lead / landing) vẫn ở KC marketing rules.md vì những rule đó scope KC-tenant-specific (tenant landing customization, tenant lead pipeline) không apply cho KH platform marketing.

---

## 4. Out-of-scope (tracked separately)

- ~~**Server-side consent API** — Phase 2, follow-up gap GAP-353b~~ ✅ SHIPPED Wave 25 Bucket A (3 endpoints `/api/v1/consent/record|{visitorId}|{visitorId}/revoke`; DR-03 36-month purge cron). See [`api-contract.md`](./api-contract.md).
- ~~**DSAR self-service intake form** — PDPL Art 14 6 rights; manual email-based DSAR was acceptable MVP. Follow-up gap **GAP-353c** (~6h)~~ ✅ SHIPPED Wave 26 Bucket A (POST `/api/v1/dsar/request` + GET `/api/v1/dsar/{ticketId}` + 5 BR-PDPL-DSAR-* rules + 20-day SLA cron). See [`api-contract.md`](./api-contract.md) §8.
- **DPIA documentation** — Decree 13/2023/NĐ-CP Art 24-30 mandates DPIA cho orgs processing >100k PII subjects; MVP solo-dev <<100k → defer. Follow-up gap **GAP-353d** (~4h)
- **use-cases.md** for marketing domain — Phase 2 of trio (Wave 25 ships api-contract.md alongside Phase 2 server endpoints; use-cases.md still deferred until DSAR ships per GAP-353c).

---

## 5. Log

- **2026-05-06** — Wave 26 Bucket A (GAP-353c). Added 5 `BR-PDPL-DSAR-001..005` rules với full 5-attribute review per `business-logic-review.md` v1.0.0 covering: 6 PDPL Art 14 right enumeration, 20-day Decree 13/2023 Art 19 SLA, identity verification via national_id_last4 + DPO callback, 36-month retention (sister to BR-PDPL-CONSENT-003 + DR-03), honeypot anti-spam. Updated §4 Out-of-scope to flip GAP-353c from deferred → shipped. Reviewer: @nguyenvankiet (acting Compliance scout + Product Owner + Tech Lead, solo-dev). Legal counsel formal review queued GAP-182 Phase 2 + GAP-156.
- **2026-05-06** — Initial rules. Wave 23 Bucket A. Created file kèm 4 BR-PDPL-CONSENT-* rules với full 5-attribute review per `business-logic-review.md` v1.0.0. Source: PDPL 2023 Articles 11-13, Decree 13/2023/NĐ-CP Articles 13/17/18, Consumer Protection Law 2023 Art 12, GDPR precedent (non-binding). Reviewer: @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev). Legal counsel formal review queued GAP-182 Phase 2 + GAP-156. Closes Wave 23 Bucket A AC item "kitehub/marketing/rules.md created with BR-PDPL-CONSENT-001..004 5-attribute".
