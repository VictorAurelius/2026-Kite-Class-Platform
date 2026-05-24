---
title: PDPL Pre-Launch Checklist (Decree 13/2023/NĐ-CP)
status: partial-v1
wave: beta-readiness-4
gap: GAP-353b
created: 2026-05-24
audience: mixed
---

# PDPL Pre-Launch Checklist — Decree 13/2023/NĐ-CP

**Status:** ⚠️ PARTIAL — **v1 dev-implementation, pending counsel formal review**
**Wave:** beta-readiness-4 Bucket B — GAP-353b
**Effective regulator deadline:** 2026-07-01

> ⚠️ **DISCLAIMER (per CLAUDE.md Phase 1 BETA risk tolerance):** Nội dung dưới đây
> là dev-self-implementation theo hiểu biết tốt nhất của solo dev. KHÔNG phải
> opinion pháp lý chính thức. Counsel formal review queued Phase 2 (post 5 beta
> tenants live + 2 tuần stable). For K-12 tenant (P5 persona — Phase 3): counsel
> review BẮT BUỘC TRƯỚC launch per CLAUDE.md Track 2 mandate.

---

## 1. Scope

PDPL Decree 13/2023/NĐ-CP (Nghị định bảo vệ dữ liệu cá nhân) áp dụng cho:
- Mọi tenant Việt Nam của KiteHub/KiteClass (PII subject = end-user của tenant)
- Tenant nước ngoài có data subject là người Việt
- Phase 1 BETA scope: P2 Center Owner + P3 Center Manager (no K-12)

K-12 (P5 Principal) defers Phase 3 per CLAUDE.md mandate — counsel review mandatory.

---

## 2. Article-by-article checklist

### Art 11 — Informed consent

| Item | Implementation | Status |
|------|---------------|--------|
| Consent surface visible TRƯỚC khi collect PII | `packages/shared-ui/src/components/ConsentBanner/` (Wave 23 Bucket BC) mounted in `(public)/layout.tsx` | ✅ DONE |
| Granular per-category (essential/analytics/marketing) | `ConsentBanner` Tier 3 settings dialog với 3 toggles | ✅ DONE |
| Equal visual weight CTA, no dark patterns | "Từ chối tất cả" + "Tuỳ chỉnh" + "Đồng ý tất cả" 3 buttons same color + same size | ✅ DONE |
| Vietnamese language default | `lang` prop default `'vi'` | ✅ DONE |
| Privacy + Cookie + Terms linked | `privacyHref` + `cookieHref` + `termsHref` props mandatory | ✅ DONE |

### Art 12 — Right to access

| Item | Implementation | Status |
|------|---------------|--------|
| User can query their consent history | GET `/api/v1/consent/v2/{userId}` returns full hash-chain history | ✅ DONE (Wave br-4 Bucket B) |
| Hash chain integrity validation | `ConsentService.verifyChainIntegrity` SHA-256 chain check | ✅ DONE |
| Tamper-evident audit trail | RLS NO UPDATE NO DELETE policies on `consent_record_immutable` table | ✅ DONE |

### Art 13 — Right to rectification + Right to withdraw consent

| Item | Implementation | Status |
|------|---------------|--------|
| Withdraw flow accessible as grant flow | POST `/api/v1/consent/v2/withdraw` mirrors POST `/record` shape — single call API | ✅ DONE |
| Withdraw effective ≤5s (PDPL Art 14 "rút lại sự đồng ý dễ dàng như cho đồng ý") | Frontend `useConsent.revoke()` fires `gtag('consent','update', {analytics_storage:'denied',ad_storage:'denied'})` SYNCHRONOUSLY BEFORE server POST — `applyAnalyticsConsent` in `packages/shared-ui/src/components/ConsentBanner/analytics.ts` | ✅ DONE (Wave br-4 Bucket B) |
| Frontend test proves gtag fires BEFORE fetch POST | `__tests__/analytics.test.ts` "revoke fires gtag denied BEFORE fetch POST" | ✅ DONE |
| 5s budget assertion | `__tests__/analytics.test.ts` "revoke completes synchronously well under 5s budget" | ✅ DONE |
| Server-side withdraw = INSERT new row (NOT flip) | `ConsentService.withdrawConsent` calls `recordConsent` với `analytics+marketing=false` | ✅ DONE |
| Audit trail of withdraw event | Hash chain extends naturally — withdraw row's `prev_hash` links to previous grant | ✅ DONE |

### Art 14 — Right to erasure (after consent withdraw + retention expiry)

| Item | Implementation | Status |
|------|---------------|--------|
| Retention policy documented | `documents/02-architecture/data-retention-policy.md` DR-03 36 months for consent records | ✅ DONE (Wave 23) |
| Cron purge after retention expiry | `ConsentRetentionCron` daily 3am (Wave 25 Bucket A) — applies to `consent_record` v1 table | ⚠️ PARTIAL — v2 immutable table needs separate purge runbook (RLS blocks DELETE, requires superuser bypass + audit row) |
| Erasure request handling SOP | Manual SOP — solo dev replies email + executes purge | ⚠️ PARTIAL — automated request portal Phase 1.5+ |

### Art 16 — Cross-border data transfer

| Item | Implementation | Status |
|------|---------------|--------|
| Data residency Vietnam OR notified | AWS `ap-southeast-1` (Singapore) — non-VN region. Phase 1 BETA acceptable per Moderate risk tolerance; counsel review Phase 2 cho VN-region pivot OR notification filing | ⚠️ PARTIAL — Phase 2 decision |

---

## 3. Implementation evidence (Wave br-4 Bucket B specific)

### 3.1 Backend service (`kitehub-subscription`)

| File | Purpose |
|------|---------|
| `consent/immutable/ConsentRecordImmutable.java` | JPA entity, no setters (immutable getter-only), maps `consent_record_immutable` |
| `consent/immutable/ConsentRecordImmutableRepository.java` | Spring Data JPA — no delete/update methods exposed |
| `consent/immutable/ConsentInserter.java` | `@Component` REQUIRES_NEW + SERIALIZABLE INSERT primitive cho hash chain integrity |
| `consent/immutable/ConsentService.java` | Public service — recordConsent + withdrawConsent + findHistory + verifyChainIntegrity với retry loop |
| `consent/immutable/ImmutableConsentController.java` | 3 endpoints: POST `/record` + GET `/{userId}` + POST `/withdraw` mounted ở `/api/v1/consent/v2` |
| `db/migration/V56__create_consent_record_immutable.sql` | V56 migration — `consent_record_immutable` table + 2 indexes + RLS NO UPDATE NO DELETE policies |

### 3.2 Frontend (`packages/shared-ui`)

| File | Purpose |
|------|---------|
| `ConsentBanner/useConsent.ts` | Extended — calls `applyAnalyticsConsent` SYNCHRONOUSLY in `give`/`reject`/`revoke` before server sync |
| `ConsentBanner/analytics.ts` | NEW — `applyAnalyticsConsent(categories)` fires `gtag('consent','update',{analytics_storage,ad_storage,ad_user_data,ad_personalization,personalization_storage,functionality_storage,security_storage})` mapping |
| `ConsentBanner/api.ts` | Unchanged Wave 25 |
| `ConsentBanner/index.tsx` | Re-exports `applyAnalyticsConsent` + `ConsentMap` |

### 3.3 Tests

| File | Test scope |
|------|------------|
| `ConsentRecordImmutablePostgresIT` (backend) | INET round-trip IPv4/IPv6 + JSONB granted + hash chain validation + RLS UPDATE/DELETE blocked + chain detects tampering |
| `ConcurrentConsentWritesIT` (backend) | 2 threads × 4 inserts cùng userId → 8 rows + linear chain preserved (SERIALIZABLE + retry) |
| `__tests__/analytics.test.ts` (frontend) | gtag mapping correct + dataLayer fallback + no-throw on broken gtag + revoke fires gtag BEFORE fetch + revoke <5s budget |

### 3.4 Documentation

| Doc | Status |
|-----|--------|
| `documents/01-business/kitehub/consent/api-contract.md` | NEW Wave br-4 Bucket B |
| `documents/01-business/kitehub/consent/rules.md` | NEW pointer to `marketing/rules.md` canonical |
| `documents/02-architecture/design-system/dossier/14-common-components-inventory-kh.md` | G14 ConsentBanner row extended với Wave br-4 Bucket B note |
| `documents/02-architecture/adr/ADR-034-cookie-consent-vendor.md` | NEW vendor decision |

---

## 4. Gaps remaining (counsel review queued Phase 2)

| Gap | Description | Phase 2 trigger |
|-----|-------------|-----------------|
| Counsel formal review of consent text + privacy policy + terms of service | v1 text dev-drafted; counsel review mandatory cho v1.0.0 GA | 5 beta tenants live + 2 tuần stable |
| K-12 tenant counsel review (P5 Principal) | Phase 3 hard requirement per CLAUDE.md Track 2 mandate | Phase 3 kickoff |
| VN data residency decision | Phase 2 — AWS HCM region OR notification filing | Phase 2 |
| Erasure request automated portal | Phase 1.5+ — manual SOP Phase 1 BETA acceptable | Tenant request volume threshold |
| v2 immutable table purge runbook (RLS blocks DELETE) | Phase 1.5 — needs superuser bypass procedure + audit row | Retention deadline approach (36 months from Wave br-4 ship) |
| DPIA (Data Protection Impact Assessment) for AI Branding features | PDPL Art 25 may require; counsel review needed | Phase 2 |

---

## 5. Related

- `documents/01-business/kitehub/consent/api-contract.md` — server consent v2 endpoints
- `documents/01-business/kitehub/marketing/rules.md` — BR-PDPL-CONSENT-001..004
- `documents/02-architecture/data-retention-policy.md` — DR-03 36 months
- `documents/02-architecture/adr/ADR-034-cookie-consent-vendor.md` — vendor decision
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/immutable/**` — implementation
- CLAUDE.md §"CURRENT PHASE" — risk tolerance Moderate, "v1 pending counsel review" disclaimer OK for non-K-12
