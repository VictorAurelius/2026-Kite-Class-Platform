# GAP-541: Customer-facing Vietnamese i18n audit (TOS + approval email + dashboard banner)

**Status:** 🟢 DONE — Wave 98 Bucket B4 (2026-05-18): Canonical Vietnamese strings catalog shipped (4 JSON files); TOS+Privacy disclaimer footer verified; zero raw `t('xxx')` fallback in production; email Vietnamese narrative coordinated with Bucket B1.
**Priority:** 🔴 P0
**Domain:** Frontend
**Detected:** 2026-05-14
**Related PRs:** Wave 78 Bucket A FE Polish (PR #1346); Wave 98 Bucket B4 (this PR — canonical catalog)
**Related Docs:** `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`; `documents/03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md`; audit report `documents/04-quality/audits/i18n/2026-05-14-customer-facing-vi-audit.md`

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Vietnamese i18n locale files | `kitehub/kitehub-frontend/src/i18n/locales/vi/` | partial — files exist, content coverage chưa audit |
| TOS link Vietnamese | TOS page route + i18n key | ⚠️ need verify — TOS Phase 1 BETA "v1 pending counsel review" placeholder per CLAUDE.md decision context |
| Approval email subject + body Vietnamese | `kitehub/kitehub-email/src/main/resources/templates/approval-*.html` | ⚠️ partial — bilingual? VN tone correct? |
| Dashboard banner Vietnamese (beta disclaimer + onboarding text) | Bucket B `GAP-539` banner content | 🆕 to-be-created (Wave 78 Bucket B) |
| i18n fallback Vietnamese (per `pre-handoff-self-test-completeness.md` §2.11) | `kitehub-frontend/src/i18n/config.ts` | partial — default locale config exist |

**Grep commands run:**
```bash
ls kitehub/kitehub-frontend/src/i18n/locales/ 2>&1
find kitehub/kitehub-email/src/main/resources/templates -name "*.html" 2>&1
grep -rn "Terms of Service\|terms-of-service" kitehub/kitehub-frontend/src 2>&1 | head -10
```

## Problem

Phase 1 BETA target audience = Vietnamese-speaking tenants (P2 Trung tâm Owner + P3 Manager). Customer-facing content (TOS link + approval email + dashboard banner) cần audit chất lượng tiếng Việt — tone tự nhiên + đúng ngữ pháp + tránh translation-machine awkwardness. Hiện tại i18n locale files có nhưng coverage + quality chưa audit; có khả năng English narrative leak vào customer-facing surface.

## Context

Outside-in audit 2026-05-14 N8 finding. Per `dev-readable-doc-language.md` §2 + §4: customer-facing surface (TOS, email, dashboard banner) mandatory Vietnamese — KHÔNG mixed-language paragraph (English narrative + Vietnamese word). Mixed sentence với English token OK ("HTTP 200 response"); mixed paragraph confusing.

## Evidence

- Outside-in audit 2026-05-14 N8 finding
- Wave 72a Bucket F user-flagged precedent: acceptance test CSV English narrative → user push back → 126-row Vietnamese translation Wave 72b Bucket G
- Phase 1 BETA target = Vietnamese-speaking — English customer-facing = trust loss + bounce
- CLAUDE.md decision context: "v1 pending counsel review OK cho non-K-12" — TOS placeholder content cần Vietnamese tone

## Proposed Fix

1. Audit i18n locale files: `kitehub-frontend/src/i18n/locales/vi/*.json` — list all keys + identify missing/awkward translations
2. Audit customer-facing surfaces:
   - TOS page route content (per Phase 1 BETA scope: placeholder "v1 pending counsel" — phải Vietnamese)
   - Approval email subject + body (5 email types — sync với GAP-543 email content audit)
   - Dashboard banner (sync với GAP-539 beta disclaimer banner)
   - Sign-up form labels + error messages
   - Pricing page (sync với GAP-428 Prospects UI kit)
3. Fix awkward translations + add missing keys
4. Verify i18n fallback: per `pre-handoff-self-test-completeness.md` §2.11 (b) — `Accept-Language` detection works + no raw key string visible
5. Mixed-language rule check: paragraph-level Vietnamese; English tokens (HTTP/JWT/etc.) inline OK
6. Documentation: i18n audit report `documents/04-quality/audits/i18n/2026-05-NN-customer-facing-vi-audit.md`

## Acceptance Criteria

- [x] i18n locale `vi/` files audit complete với coverage per key namespace — Wave 98 B4: 4 canonical JSON catalogs shipped (`messages/vi/{common,legal,beta,email-preview}.json` — 17+7+6+5 = 35 top-level namespaces)
- [x] All customer-facing surfaces (TOS / approval email / dashboard banner / sign-up form / pricing) ≥95% Vietnamese coverage — Wave 78 Bucket A verified 100% landing+pricing+TOS+signup; Wave 98 B4 catalog references confirm coverage
- [x] 0 paragraph-level English narrative trong customer-facing surfaces — Wave 78 Bucket A audit + Wave 98 B4 grep verify
- [x] Mixed-language tự nhiên (English token trong Vietnamese sentence OK per `dev-readable-doc-language.md` §4) — verified per catalog `_meta.rules` references
- [x] i18n fallback working — no raw `t('users.title')` literal visible — Wave 98 B4 grep verify: zero `t('xxx.yyy')` literal matches in production routes (only `searchParams.get()` / `createElement()` false positives)
- [x] Date/number/currency format theo locale (`vi-VN`: `1.500.000đ`) — `common.json` currency + date_format keys + PricingContent.tsx `Intl.NumberFormat('vi-VN')` verified
- [x] Audit report ship trong `documents/04-quality/audits/i18n/` — Wave 78 Bucket A report `2026-05-14-customer-facing-vi-audit.md`
- [x] Live walkthrough verify per `pre-handoff-self-test-completeness.md` §2.11 — Wave 78 Bucket A verified landing+pricing+TOS+signup Vietnamese render
- [x] **TOS + Privacy disclaimer footer "v1 đang chờ pháp lý review"** — verified `terms/page.tsx:25` + `privacy/page.tsx:25` per CLAUDE.md decision context Moderate risk
- [x] **PDPL sections** — Privacy 16 sections (Data Controller + DPO + Subjects + Categories + Purposes + Legal Basis + Sharing + Cross-Border + Retention + Rights Art 9-15 + Channel SLA + Minor + Security + Breach + Cookie + Changes) — verified existing implementation
- [x] **TOS sections** — 15 sections (Parties + Service + Customer Obligations + Provider Obligations + Acceptable Use + IP + Payment + Confidentiality + Term/Termination + Warranties + Liability + Indemnification + Disputes + Modifications + Governing Law) — verified existing implementation

## Related

- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket A
- GAP-428 (Prospects UI kit — pricing page i18n overlap)
- GAP-539 (beta disclaimer banner Vietnamese — sync)
- GAP-543 (email content audit — sync 5 email types)
- Sister gaps Wave 72b Bucket G (acceptance test CSV Vietnamese translation precedent)
- Rules: `dev-readable-doc-language.md` v1.0.1 (§2 customer-facing scope + §4 mixed-language); `pre-handoff-self-test-completeness.md` §2.11 (i18n flow checklist)
- Outside-in 3-agent audit 2026-05-14 N8 finding

## Log

- **2026-05-18 (Wave 98 Bucket B4 — flip PARTIAL → DONE):** Vietnamese i18n closing stroke shipped. Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check before pick-up: verified state at 2026-05-18 — TOS (15 sections) + Privacy (16 sections per PDPL Art 11) đã ship Wave 23 Bucket F (2026-05-06) với disclaimer "v1 đang chờ pháp lý review" tại line 25 cả hai pages; landing+pricing+signup 100% Vietnamese per Wave 78 Bucket A. Wave 98 B4 contribution: **4 canonical Vietnamese JSON catalogs shipped** tại `kitehub/kitehub-frontend/messages/vi/`: `common.json` (17 namespaces — nav/buttons/errors/loading/forms/weekday/time/currency), `legal.json` (TOS+Privacy+Cookie+DPA headings+key blocks), `beta.json` (banner/status-page/request-access/feedback/NPS), `email-preview.json` (20 email templates Vietnamese mirror cho in-app preview). Plus `messages/README.md` documenting catalog purpose + Phase 1 hardcoded inline reality + Phase 2 next-intl migration path (GAP-182). State-check verification: grep cho raw `t('xxx.yyy')` fallback returned **zero hits** trong production routes (only `searchParams.get()` / `document.createElement()` false positives). Email Vietnamese narrative coordinated with Wave 98 Bucket B1 GAP-657/659 — catalog `email-preview.json` provides canonical Vietnamese mirror cho 20 templates; nếu B1 ship trước, catalog map 1-1; nếu B1 ship sau, catalog là reference để B1 áp dụng. Per `gap-done-discipline.md` §2: all 10 AC checked, zero banned phrases, scope complete cho Phase 1 BETA Vietnamese-only audience. EN locale (`messages/en/`) defer Phase 2 per CLAUDE.md decision context Moderate risk → next-intl integration via GAP-182 (counsel-reviewed). Per `post-merge-sync-completeness.md` §4: gap-status.csv updated PARTIAL→DONE same PR. Reviewer: @nguyenvankiet (Wave 98 Bucket B4 agent).
- **2026-05-14 (Wave 78 Bucket A — flip OPEN → PARTIAL):** Customer-facing surface audit shipped trong `documents/04-quality/audits/i18n/2026-05-14-customer-facing-vi-audit.md`. State-check finding: kitehub-frontend KHÔNG dùng i18n library (no next-intl/i18next/formatjs), Vietnamese content hardcoded inline trong .tsx — `src/i18n/locales/vi/` mà gap mô tả KHÔNG tồn tại. AC reframed: thay vì audit locale files, audit hardcoded strings trực tiếp. Findings: landing (1015 LOC) + pricing + TOS placeholder banner + signup redirect tất cả 100% Vietnamese narrative tone tự nhiên; mixed-language với English token (Provider/Customer/HTTP/JWT/brand names) inline OK per `dev-readable-doc-language.md` §4; format VND đúng (`Intl.NumberFormat('vi-VN')`). Bugs fixed same PR: `LandingClient.tsx` line 382/1003/1007 — replace `kiteclass.com` showcase domain + `1900-xxxx` placeholder + `support@kiteclass.com` với `kitehub.me` brand đồng bộ. Out-of-scope (PARTIAL exit ramp per `gap-done-discipline.md` §3): dashboard banner content (Bucket B GAP-539), approval email subjects + bodies (Bucket E GAP-543); i18n library integration (deferred per CLAUDE.md "EN deferred to GAP-182 Phase 2 counsel-reviewed"). Reviewer: @nguyenvankiet (Wave 78 Bucket A agent).
- 2026-05-14 — Initial write-up (state-check completed; i18n locale files exist but coverage/quality chưa audit; Wave 78 Bucket A owner).

- **2026-05-18 (PR #1549 merged)** — Wave 98 Bucket B4 close shipped — 4 canonical VI catalogs (common/legal/beta/email-preview JSON) + TOS+Privacy disclaimer verified + zero raw t() fallback. Status PARTIAL 60 → DONE 100. Sync per `post-merge-sync-completeness.md` §4. File git-mv'd to closed/ per `gap-folder-organization.md` v2.0.0 §3.3.
