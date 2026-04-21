# School MIS / SMS Integration Catalog

**Domain:** KiteClass Core / Integration / School MIS
**Version:** 1.0 (Phase 1 — GAP-200)
**Updated:** 2026-04-21
**Status:** DRAFT — catalog for onboarding strategy decision (see ADR-017)
**Owner:** Architecture + Business Lead

---

## 1. Purpose

KiteClass onboards K-12 schools in Vietnam that already run a School MIS / SMS
(School Management System) — typically **VNEDU**, **SMAS**, or **Base.vn**.
Asking them to re-type 300-2,000 students + 50-150 teachers into KiteClass is a
**hard blocker** for persona "K-12 Principal / IT Manager" (BRD Tier-1).

This catalog compares candidate MIS providers on:

1. API availability (public, partner-only, none)
2. Authentication mode (API key, OAuth2, SAML, IP allowlist)
3. Data model coverage (students, parents, teachers, classes, grades, attendance)
4. Sync cadence (one-shot import, pull on demand, push webhooks, live bi-directional)
5. Licensing cost + partnership process
6. Data-residency + PDPL Việt Nam implications

Catalog drives the **Phase 1 pilot decision** (which MIS to integrate first) and
defines the neutral `MisRosterSource` interface that all adapters implement
(see `.claude/rules/design-patterns.md` §2 — Adapter pattern mandatory).

---

## 2. Vietnamese MIS Providers (Primary targets)

### 2.1 VNEDU (vnEdu.vn)

| Attribute | Value |
|-----------|-------|
| Vendor | Viettel Group (state-linked telco) |
| Market share (K-12 VN) | ~55-65% of public schools (2024 Ministry of Education data) |
| Website | https://vnedu.vn |
| Public API | ❌ None published |
| Partner API | ⚠️ Available via MoET partnership agreement (requires govt-side approval) |
| Auth mode | API key + IP allowlist (partner tier) |
| Data model | Students, parents, teachers, classes, grades, attendance, subjects |
| Sync cadence | Pull-only batch (no webhooks). Partner tier polling every 5-15 min. |
| Licensing cost | Free for public schools; KiteClass integration needs **partnership fee negotiation** (estimated 50-100M VND/year per MoU) |
| Data residency | VN-only (Viettel data centers) |
| PDPL compliance | ✅ Default (govt-operated) |
| **Phase 1 verdict** | 🟢 **PILOT TARGET** — widest K-12 reach, partnership path exists |

**Fallback when no partnership:** CSV export from VNEDU admin console → KiteClass
bulk-import UI (already shipped via `StudentBulkImportIT`).

### 2.2 SMAS (smas.edu.vn)

| Attribute | Value |
|-----------|-------|
| Vendor | Viettel Business Solutions |
| Market share | ~20-25% of public schools; some private K-12 |
| Website | https://smas.edu.vn |
| Public API | ❌ None |
| Partner API | ⚠️ Same partnership channel as VNEDU (Viettel family) — limited docs |
| Auth mode | Assumed API key + HTTPS (unverified) |
| Data model | Students, teachers, classes, grades (attendance limited) |
| Sync cadence | Pull batch (assumed same as VNEDU) |
| Licensing cost | Bundled with Viettel telco contracts |
| Data residency | VN |
| PDPL compliance | ✅ Default |
| **Phase 1 verdict** | 🟡 **PHASE 2** — overlap with VNEDU adapter; share partnership agreement |

### 2.3 Base.vn (base.vn — HRM/SMS product)

| Attribute | Value |
|-----------|-------|
| Vendor | Base Enterprise (HCM) |
| Market share | ~5-8% of private K-12 (mostly urban, tech-forward) |
| Website | https://base.vn |
| Public API | ⚠️ REST API exists for some products (not specifically K-12 SMS) |
| Partner API | ✅ SaaS-style, partner developer program |
| Auth mode | OAuth2 client credentials |
| Data model | Students, staff, departments (no grades/attendance in v1) |
| Sync cadence | Pull + some webhooks (limited) |
| Licensing cost | Per-seat pricing; API add-on subscription |
| Data residency | VN (AWS Singapore for some tenants) |
| PDPL compliance | ⚠️ Requires DPA (data-processing agreement) per tenant |
| **Phase 1 verdict** | 🟡 **PHASE 2** — smaller TAM, but cleaner API + partner program |

---

## 3. International MIS (Reference / future-proofing)

### 3.1 Microsoft School Data Sync (SDS)

| Attribute | Value |
|-----------|-------|
| Vendor | Microsoft |
| API | ✅ Graph API / SDS v2.1 (public, OpenAPI spec) |
| Auth | OAuth2 (Azure AD tenant) |
| Data model | Full K-12 schema (OneRoster 1.2 standard) |
| Sync cadence | Pull + change-feed (delta queries) |
| Licensing | Free with Microsoft 365 Education |
| Data residency | Configurable (global regions) |
| **Verdict** | 🟢 **Reference adapter** — international private schools in VN use M365 Education. **Adapter reuses OneRoster logic.** |

### 3.2 Google Classroom + Admin SDK

| Attribute | Value |
|-----------|-------|
| Vendor | Google |
| API | ✅ Classroom API v1 + Directory API |
| Auth | OAuth2 (Google Workspace for Education) |
| Data model | Courses, rosters, assignments (no formal grade book export) |
| Sync cadence | Pull + push notifications (Pub/Sub) |
| Licensing | Free with Workspace for Education Fundamentals |
| Data residency | Configurable |
| **Verdict** | 🟢 **Reference adapter** — complement to MS SDS for schools on Google stack. |

### 3.3 OneRoster 1.2 (IMS Global standard)

Not a vendor — **open standard** for K-12 rostering (CSV + REST profiles).
Both MS SDS and modern US SIS products (PowerSchool, Infinite Campus) speak
OneRoster. If we implement OneRoster CSV import, we get ~70% of international
use cases "for free".

**Recommendation:** `OneRosterCsvAdapter` as Phase 2 sibling to `VneduAdapter`.

---

## 4. Comparison Matrix

| Feature | VNEDU | SMAS | Base.vn | MS SDS | Google Classroom |
|---------|:-----:|:----:|:-------:|:------:|:----------------:|
| Public API | ❌ | ❌ | ⚠️ | ✅ | ✅ |
| Partner API | ✅ | ⚠️ | ✅ | N/A | N/A |
| OAuth2 | ❌ | ❌ | ✅ | ✅ | ✅ |
| Students | ✅ | ✅ | ✅ | ✅ | ✅ |
| Parents | ✅ | ⚠️ | ❌ | ✅ | ❌ |
| Teachers | ✅ | ✅ | ✅ | ✅ | ✅ |
| Classes | ✅ | ✅ | ⚠️ | ✅ | ✅ |
| Grades | ✅ | ✅ | ❌ | ✅ | ⚠️ |
| Attendance | ✅ | ⚠️ | ❌ | ⚠️ | ❌ |
| Webhooks | ❌ | ❌ | ⚠️ | ✅ | ✅ |
| Delta sync | ❌ | ❌ | ⚠️ | ✅ | ✅ |
| PDPL-ready | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ |
| Free tier | ✅ | ✅ | ❌ | ✅ | ✅ |
| K-12 VN reach | ~60% | ~22% | ~6% | <1% | <1% |

Legend: ✅ full support · ⚠️ partial / requires workaround · ❌ not available.

---

## 5. Data Model — Neutral Roster Import

All adapters produce a **neutral DTO** (`RosterImport`) independent of source
MIS. Keeps KiteClass core free of vendor types (prevents leaky abstraction;
`.claude/rules/design-patterns.md` §3.10).

```
RosterImport
├── source: MisProvider (VNEDU | SMAS | BASE_VN | MS_SDS | GOOGLE | ONEROSTER_CSV)
├── fetchedAt: Instant
├── academicYear: String (e.g. "2025-2026")
├── students: List<StudentRecord>
├── parents: List<ParentRecord>
├── teachers: List<TeacherRecord>
├── classes: List<ClassRecord>
└── enrollments: List<EnrollmentRecord> (student ↔ class mapping)
```

Field-level mapping per source MIS lives in adapter-specific test fixtures
(`resources/mis/{provider}/sample-*.json`) — Phase 2 deliverable.

---

## 6. Licensing + Legal Checklist (per integration)

Before activating any MIS integration in production, Legal team signs off on:

- [ ] Partnership MoU or API agreement signed with MIS vendor
- [ ] PDPL Data Processing Agreement (DPA) with tenant school
- [ ] Data residency clause (where roster data is stored/processed)
- [ ] Retention limits (KiteClass does NOT retain MIS data beyond sync TTL)
- [ ] Purpose limitation (roster only — no re-sale, no analytics export)
- [ ] Breach notification SLA (72h per PDPL)
- [ ] Audit log retention (12 months minimum)
- [ ] Parent consent capture UI (for minor students whose data flows through)

---

## 7. Phase 1 Decision (Confirmed in ADR-017)

- **Pilot MIS:** VNEDU (skeleton adapter shipped this PR)
- **Implementation style:** one-shot roster import at onboarding (NOT live sync)
- **Conflict resolution:** MIS-wins by default; configurable per tenant via
  `kiteclass.mis.conflict-strategy` (MIS_WINS | KITECLASS_WINS | MANUAL_REVIEW)
- **Fallback when no partnership:** OneRoster CSV import (Phase 2)

---

## 8. References

- ADR: `documents/02-architecture/adr/ADR-017-mis-sync-strategy.md`
- Business rules: `documents/01-business/kiteclass/mis-integration/rules.md`
- Use cases: `documents/01-business/kiteclass/mis-integration/use-cases.md`
- API contract: `documents/01-business/kiteclass/mis-integration/api-contract.md`
- Gap: `documents/04-quality/gaps/GAP-200-school-mis-integration.md`
- Design patterns rule: `.claude/rules/design-patterns.md` §2 (Adapter, Strategy)
- Bulk import precedent: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/`
- OneRoster 1.2 spec: https://www.imsglobal.org/oneroster-v12-final-specification
- Microsoft SDS v2.1: https://learn.microsoft.com/en-us/schooldatasync/

---

## 9. Log

- 2026-04-21 — Catalog created (GAP-200 Phase 1). VNEDU selected as pilot.
