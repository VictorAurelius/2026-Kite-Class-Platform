# ADR-034: Cookie Consent Vendor — Self-build Banner + iubenda Privacy Policy Generator

**Status:** ACCEPTED
**Date:** 2026-05-24
**Deciders:** @nguyenvankiet (solo dev)
**Related Gap(s):** GAP-353b
**Wave:** beta-readiness-4 Bucket B

## Context

PDPL Decree 13/2023/NĐ-CP (effective 2026-07-01) yêu cầu KiteHub/KiteClass thu thập
informed consent từ end-user của tenant TRƯỚC khi xử lý PII. Compliance bao gồm:

1. **Banner UI** với granular toggles (essential/analytics/marketing) + equal CTA
   weight + no dark patterns + Vietnamese language
2. **Privacy Policy text** chính xác về PDPL Art 11-14 — cần legal-domain-specific
   wording (data subject rights, data controller identity, retention windows, cross-
   border transfer disclosure, regulator contact)
3. **Server-side audit trail** với tamper-evidence — sister scope của ADR này
   shipped Wave br-4 Bucket B (immutable + hash chain) NOT vendor-dependent

Solo dev mode (per CLAUDE.md Phase 1 BETA) KHÔNG có in-house counsel. Risk tolerance
Moderate cho non-K-12; K-12 (Phase 3) MUST counsel review.

3 options khảo sát:

| Option | Banner UI | Privacy Policy text | Cost | Counsel involvement |
|--------|-----------|---------------------|------|---------------------|
| A: Self-build banner + self-write privacy text | ✅ free, full control | ❌ dev no legal expertise — risk wrong wording | $0 | Counsel review BẮT BUỘC trước launch |
| B: Vendor SaaS (OneTrust/Cookiebot) | ✅ vendor-managed | ✅ vendor-maintained | $50-300/month | Vendor compliance team |
| C: **Self-build banner + iubenda Privacy Policy generator (PRO)** | ✅ free, full control + KiteHub branding | ✅ iubenda maintains text + monitors regulator updates | ~$27/year Pro | Optional — review v1 text only |

## Decision

**ADOPT Option C** — self-build ConsentBanner UI (already shipped Wave 23 Bucket BC
trong `packages/shared-ui/src/components/ConsentBanner/`) + **iubenda Privacy Policy
Generator (Pro tier ~$27/year)** cho privacy policy + cookie policy + terms of service
text generation cho Vietnamese language.

Server-side immutable consent + hash chain (Wave br-4 Bucket B `consent.immutable.*`
package) stays **fully in-house** — vendor không touch audit trail, không phụ thuộc
vendor uptime cho PDPL Art 11 compliance proof.

## Consequences

### Positive

- **Cost-effective:** ~$27/year vs $50-300/month vendor SaaS = save ~$580-3,600/year
  cho Phase 1 BETA solo dev budget
- **iubenda compliance updates:** vendor monitors PDPL + GDPR + CCPA changes; text
  auto-refreshes khi regulator publishes updates — solo dev không cần track manually
- **UI full control:** ConsentBanner UI tự shape theo KiteHub brand (Wave 23 already
  shipped `kitehub-story-v2` kit) — không bị vendor's generic CSS lock-in
- **Server audit trail vendor-independent:** PDPL Art 11 compliance evidence (hash
  chain + RLS immutability) hoàn toàn in-house — không lose audit trail nếu vendor
  outage / contract end
- **Easy to extend:** banner component sharable across KiteHub + KiteClass via
  `packages/shared-ui` workspace
- **Phase 3 K-12 path open:** counsel review cho v1 text easy (iubenda generates
  baseline; counsel chỉ red-line specific clauses)

### Negative

- **iubenda Vietnamese language quality unknown** — chưa benchmark hiệu quả của text
  generator cho VN-specific PDPL terminology. Mitigation: dev review + optional
  native VN legal reviewer in Phase 2.
- **iubenda lock-in cho privacy text:** if cancel subscription, text becomes stale.
  Mitigation: snapshot text vào `documents/05-guides/legal/` mỗi quarter cho fallback.
- **No vendor support cho server audit trail bugs** — hash chain implementation 100%
  in-house, solo dev fixes alone. Acceptable risk for Phase 1 BETA scope.
- **iubenda Pro tier still requires manual config** cho VN edu specific use cases
  (P5 K-12 → Phase 3 only)

### Neutral

- **Migration path open:** if vendor SaaS becomes cost-effective at Phase 2 (>50
  tenants), can switch vendor for privacy text only — banner UI + server audit trail
  stay unchanged
- **Counsel review queued Phase 2** per CLAUDE.md Phase 1 BETA risk tolerance —
  acceptable cho non-K-12; K-12 trigger Phase 3 counsel review mandatory

## Compliance status

| Article | Implementation | Vendor dependency |
|---------|---------------|-------------------|
| Art 11 (informed consent) | Self-build banner | None |
| Art 12 (right to access) | In-house hash chain API | None |
| Art 13 (right to withdraw) | In-house POST `/v2/withdraw` + analytics SDK sync handler | None |
| Art 14 (rút lại ≤5s) | `applyAnalyticsConsent` synchronous gtag denied | None |
| Privacy Policy text | iubenda Pro generated | **iubenda** |
| Cookie Policy text | iubenda Pro generated | **iubenda** |
| Terms of Service text | iubenda Pro generated | **iubenda** |

## Alternatives considered + rejected

### Option A — Self-write privacy text

Rejected — solo dev không có legal expertise. Wrong wording → regulator finding =
penalty 200M-500M VND per PDPL Art 19. Risk too high cho savings ~$27/year.

### Option B — Vendor SaaS (OneTrust/Cookiebot)

Rejected — cost too high cho Phase 1 BETA budget ($50-300/month). Vendor lock-in
cao hơn (banner UI + privacy text + cookie scanner all coupled). KhẢ năng customize
UI theo KiteHub brand limited.

### Option D — Hybrid: vendor banner + in-house policy

Rejected — defeats purpose of vendor (privacy text is hard part; banner UI is easy).

## Migration plan

| Phase | Action |
|-------|--------|
| Wave br-4 Bucket B (now) | Self-build banner shipped Wave 23 (already production); enable iubenda Pro account; generate VN privacy + cookie + terms; embed vào `/legal/privacy`, `/legal/cookies`, `/legal/terms` static pages |
| Phase 2 (post 5 beta tenants) | Counsel review v1 text generated by iubenda; red-line clauses; snapshot to `documents/05-guides/legal/` |
| Phase 3 (K-12 P5) | Mandatory counsel review per CLAUDE.md Track 2 mandate |

## Related

- GAP-353b — server immutable consent + hash chain (sister implementation)
- Wave beta-readiness-4 plan §3.2 Bucket B
- `documents/04-quality/compliance/pdpl-pre-launch-checklist.md`
- `documents/01-business/kitehub/consent/api-contract.md`
- `documents/02-architecture/design-system/dossier/14-common-components-inventory-kh.md` G14
- CLAUDE.md §"CURRENT PHASE" Phase 1 BETA risk tolerance Moderate
