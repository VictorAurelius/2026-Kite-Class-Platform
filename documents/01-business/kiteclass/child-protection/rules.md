# Child Protection — Business Rules

**Domain:** KiteClass Core / Compliance / Safeguarding
**Version:** 0.2 (Phase 1A + Phase 1B foundation)
**Created:** 2026-05-04
**Last-Reviewed:** 2026-05-04
**Reviewer-Approver:** @nguyenvankiet (acting Legal scout, solo-dev, 2026-05-04). Formal Vietnamese child-protection counsel review queued via GAP-156.

---

## Frontmatter (per `.claude/rules/business-logic-review.md` 5-attribute)

| Attribute | Value |
|-----------|-------|
| **Source** | (1) Luật Trẻ em 2016 Đ.6 (quyền được bảo vệ), Đ.25 (vetting nhân sự), Đ.51 (mandatory reporting ≤24h), Đ.54 (môi trường mạng); (2) Decree 56/2017/NĐ-CP (chi tiết Đ.25 vetting procedure); (3) PDPL Decree 13/2023/NĐ-CP Điều 16 (special protection of children's PII); (4) Bộ luật Hình sự Đ.147 (CSAM criminal liability); (5) P5 K-12 persona review 2026-05-04 Finding 2 (AC-EDGE-005 / AC-ONBOARD-005 / AC-COMM-006) |
| **Rationale** | Schools handle minors → Vietnamese law imposes criminal liability on (a) failure to report suspected abuse to MOLISA + công an ≤24h (Đ.51), (b) allowing unvetted staff access to children (Đ.25 + Decree 56/2017), (c) mishandling children's personal data (Decree 13/2023 Art 16). Phase 1A ships entity + encryption-at-rest skeleton — the FOUNDATION for Phase 1B vetting workflow + Phase 1C mandatory-reporting banner + audit log. AES-256-GCM with per-field random IV chosen because (i) industry-standard AEAD construction, (ii) defends pattern-analysis on encrypted columns, (iii) 128-bit auth tag detects tampering. 7-year retention chosen because (a) Decree 13/2023 Art 16 minimum 24mo for service-related minor PII, (b) Bộ luật Tố tụng Hình sự statute-of-limitations on child-abuse offences extends past 24mo, (c) parallels existing financial-record retention class — single audit policy across two compliance regimes simplifies data lifecycle. |
| **Reviewer** | @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-04). Formal child-protection counsel + DPO review queued — see GAP-156 acceptance criteria item. Phase 1B/1C closure REQUIRES legal counsel sign-off before K-12 tenant onboarding flag is enabled. |
| **Compliance check** | **Compliant (Phase 1A skeleton)** — PDPL Decree 13/2023 Art 16 satisfied at-rest via AES-256-GCM (BR-CHILD-PROT-002); Luật Trẻ em 2016 Đ.6 quyền được bảo vệ supported via encrypted ticket pathway. **PARTIAL** for Đ.51 (mandatory reporting banner deferred to GAP-322c Phase 1C) and Đ.25 (vetting workflow deferred to GAP-322b Phase 1B). Tenant onboarding for K-12 vertical is BLOCKED until Phase 1B + 1C ship. |
| **Review cadence** | **Annual + event-driven**. Triggers: (1) any amendment to Luật Trẻ em 2016 (next major review window 2027), (2) PDPL Decree 13/2023 implementing-decree publication, (3) Decree 56/2017/NĐ-CP amendment, (4) BLHS amendment to Đ.147 (CSAM scope), (5) major child-protection incident in any KiteClass tenant. **Next review:** 2027-05-04 OR within 30 days of any cited statute amendment. |

---

## 1. Scope of Phase 1A

> Phase 1A ships **Incident entity + AES-256 encryption skeleton + SAFEGUARDING_OFFICER role seed only**. Vetting workflow (LLTP upload + verify queue + RBAC gate teacher access until verified), MinIO encrypted bucket for vetting evidence, Đ.51 mandatory-reporting auto-suggest banner, hash-chained non-repudiation audit log, and 7-year retention enforcement are deferred to **GAP-322b (Phase 1B)** and **GAP-322c (Phase 1C)**.

Phase 1A is the FOUNDATION layer: encryption converter, table schema, role + permission templates, and CRUD service. Phase 1B/1C build vetting + reporting + retention enforcement on top.

K-12 tenant onboarding flag (`tenant.vertical_type=K12_ENTERPRISE`) MUST remain disabled until Phase 1B + Phase 1C ship and pass legal-counsel sign-off (GAP-156).

---

## 2. Rules

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-CHILD-PROT-001 | Multi-tenant isolation | `instance_id` populated by BaseEntity tenant filter; queries restricted by Hibernate `tenantFilter`. Cross-tenant Incident reads rejected. | 1A |
| BR-CHILD-PROT-002 | Sensitive fields encrypted at rest | `description` + `evidence_paths` columns are PostgreSQL `BYTEA` and stored encrypted via `AesGcmAttributeConverter` (AES-256-GCM, 96-bit random IV per field, 128-bit auth tag). Plaintext NEVER persisted. Per PDPL Decree 13/2023 Art 16. | 1A |
| BR-CHILD-PROT-003 | Tampered ciphertext rejected | GCM auth tag verification on decrypt — modified ciphertext, modified IV, or wrong key throws `RuntimeException("Failed to decrypt sensitive field")`. Integrity guaranteed. | 1A |
| BR-CHILD-PROT-004 | Soft-delete only | `deleted` flag inherited from BaseEntity. Phase 1A allows soft-delete. Phase 1C (GAP-322c) prohibits delete on `status=CLOSED` while age < 7 years (Decree 13/2023 minor-PII retention). | 1A (Phase 1C tightens) |
| BR-CHILD-PROT-005 | Title plaintext, non-sensitive | `title` is searchable VARCHAR(200). Sensitive narrative MUST go in `description` (encrypted). Reporters must be coached not to include names in title. Phase 1B UI enforces with linter prompt. | 1A |
| BR-CHILD-PROT-006 | Status defaults to REPORTED | New incidents created via `IncidentService.create(...)` always start `status=REPORTED`. Phase 1A allows arbitrary transitions (skeleton); Phase 1B (GAP-322b) locks transitions per state machine in `use-cases.md` UC-CHILD-PROT-005. | 1A (Phase 1B locks) |
| BR-CHILD-PROT-007 | SAFEGUARDING_OFFICER role | System role seeded at NIL UUID; `RoleSeederService` clones into each tenant at provisioning time. Level 3 in role hierarchy (peer of VICE_PRINCIPAL). | 1A |
| BR-CHILD-PROT-008 | Reporter user id required | Every Incident MUST have non-null `reporter_user_id` for audit trail + reporter notifications. Anonymous reporting (Phase 1B/1C) implemented via system-account proxy, not null. | 1A |
| BR-CHILD-PROT-009 | RBAC-gated decryption (Phase 1B) | Only `SAFEGUARDING_OFFICER` + `PRINCIPAL` (Hiệu trưởng) + `COUNSELOR` (designated) may invoke `IncidentService.findById/findAll` with decrypt enabled. Phase 1A allows any tenant user (skeleton). | 1B (GAP-322b) |
| BR-CHILD-PROT-010 | Đ.51 mandatory-reporting banner | When `severity=CRITICAL` + `category ∈ {ABUSE, GROOMING, CSAM}`, UI shows banner: "Luật Trẻ em 2016 Đ.51 — báo cáo Tổng đài 111 + công an địa phương ≤24h". Phase 1C ships UI + service-level emit. | 1C (GAP-322c) |
| BR-CHILD-PROT-011 | Hash-chained non-repudiation audit log | Every CRUD on Incident emits an immutable audit log entry chained by SHA-256(prev_entry_hash || entry_payload). Admin CANNOT delete entries. Phase 1C ships. | 1C (GAP-322c) |
| BR-CHILD-PROT-012 | 7-year evidence retention | `status=CLOSED` incidents retained 7 years (financial-record class per ND-13/2023 Art 16 + statute-of-limitations on child-abuse offences). Delete-protection enforced at service layer + DB trigger. Phase 1C ships. | 1C (GAP-322c) |
| BR-CHILD-PROT-013 | Vetting workflow gate | Teachers (`role=GV`) cannot access student PII until their vetting record `status=verified` (LLTP số 2 ≤6 tháng + bằng tốt nghiệp + CCCD scan + ảnh 3×4). Phase 1B ships workflow + RBAC filter. | 1B (GAP-322b) |
| BR-CHILD-PROT-014 | MinIO encrypted bucket for vetting | Vetting evidence (LLTP scans, CCCD images) stored at `s3://staff-vetting-evidence/{tenantId}/{userId}/` with bucket-level AES-256 encryption-at-rest. Object-level encryption deferred to Phase 1C. | 1B (GAP-322b) |
| BR-CHILD-PROT-015 | Re-vetting cadence | Annual reminder for staff vetting refresh; LLTP must be re-uploaded ≤2 years (Decree 56/2017 procedural standard). Phase 1B ships scheduled job. | 1B (GAP-322b) |
| BR-CHILD-PROT-016 | Critical incident may not be soft-deleted | `severity=CRITICAL` + `category=CSAM` may NEVER be soft-deleted. Phase 1C enforces; Phase 1A allows for skeleton-test convenience. | 1C (GAP-322c) |
| BR-CHILD-PROT-017 | Encryption key rotation | Master key rotation requires re-encryption of all rows. Phase 1A: out-of-scope (single-key). Phase 2+ ships rotation runbook tied to KMS adoption. | Future |

## 3. Type definitions

### IncidentSeverity (enum)

| Value | Meaning | Phase 1C banner |
|-------|---------|:---------------:|
| `LOW` | Minor concern, school-internal handling sufficient | — |
| `MEDIUM` | Pattern of behavior; requires homeroom + parent involvement | — |
| `HIGH` | Significant harm or risk; safeguarding officer escalation required | — |
| `CRITICAL` | Suspected abuse / grooming / CSAM; restricted decryption | ✅ Đ.51 banner (with abuse category) |

### IncidentCategory (enum)

| Value | Maps to | Notes |
|-------|---------|-------|
| `BULLYING` | Bắt nạt giữa học sinh | Đ.6 quyền được bảo vệ |
| `ABUSE` | Xâm hại trẻ em (physical, emotional, sexual) | Đ.4 §6 + Đ.51 mandatory report |
| `GROOMING` | Online grooming / luring of minors | Đ.54 môi trường mạng |
| `CSAM` | Child sexual abuse material | Đ.51 + BLHS Đ.147 — strictest handling |
| `OTHER` | Doesn't fit above | Triage via safeguarding officer |

### IncidentStatus (enum)

| Value | Meaning | Phase 1B state machine |
|-------|---------|-----------------------|
| `REPORTED` | Initial submission | → INVESTIGATING |
| `INVESTIGATING` | Officer has acknowledged + opened case | → ESCALATED, RESOLVED |
| `ESCALATED` | Tổng đài 111 / công an / MOLISA notified | → RESOLVED |
| `RESOLVED` | Investigation concluded with findings | → CLOSED |
| `CLOSED` | Case finalized; 7-year retention starts | (terminal) |

## 4. Phase boundary

| Capability | 1A (this) | 1B (GAP-322b) | 1C (GAP-322c) |
|-----------|:--------:|:-------------:|:-------------:|
| Incident entity + 3 enums | ✅ | — | — |
| AES-256-GCM converter | ✅ | — | — |
| V49 schema + role seed | ✅ | — | — |
| CRUD service skeleton | ✅ | — | — |
| Vetting workflow (LLTP upload) | — | ✅ | — |
| MinIO encrypted bucket | — | ✅ | — |
| RBAC gate on decryption | — | ✅ | — |
| Đ.51 mandatory-reporting banner | — | — | ✅ |
| Hash-chained audit log | — | — | ✅ |
| 7-year retention enforcement | — | — | ✅ |
| Tổng đài 111 webhook (Stage 2) | — | — | — (Q4 2026) |

## 5. Open compliance items — formally queued for GAP-156 review

- [ ] DPO + child-protection counsel review of complete 5-attribute rule set
- [ ] Penetration test: encrypted fields cannot be read via DB direct query without decrypt key
- [ ] Re-vetting cadence calibration — Annual reminder vs 2-year LLTP refresh sufficient?
- [ ] Anonymous reporting mode design (proxy-account vs system-user pattern) for PH/HS confidentiality
- [ ] Decision: store children's PII (subject_student_id) plaintext (current) vs encrypted? Phase 1A keeps plaintext FK for query efficiency; counsel review required.

## 6. Vetting workflow rules (Phase 1B foundation — GAP-322b, Wave 18b2 Bucket B)

Each rule below carries the 5-attribute review frontmatter per `.claude/rules/business-logic-review.md`:
- **Source** common to BR-VETTING-001..005: Decree 56/2017/NĐ-CP §Đ.25 + Luật Trẻ em 2016 Đ.25 (vetting nhân sự); PDPL Decree 13/2023/NĐ-CP Art 16 (special protection of child-related personal data).
- **Reviewer** common: @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-04). Formal counsel review queued — see GAP-156.
- **Review cadence** common: Annual + event-driven on amendment of Decree 56/2017, Luật Trẻ em 2016 Đ.25, or PDPL Decree 13/2023. **Next review:** 2027-05-04.

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-VETTING-001 | Vetting state machine | Service-layer enforced transitions: `PENDING → SUBMITTED → INTERVIEW_DONE → APPROVED \| REJECTED`; `APPROVED → EXPIRED`. REJECTED + EXPIRED are terminal. Illegal transitions throw `VETTING_INVALID_TRANSITION` (HTTP 400). **Rationale:** mirrors Decree 56/2017 procedural sequence (collect docs → interview → approve/reject); explicit enum prevents skipping interview. **Compliance check:** Compliant — Decree 56/2017 §Đ.25 procedural standard. | 1B foundation |
| BR-VETTING-002 | AES-256 on sensitive vetting fields | `vettings.lltp_number` + `vettings.police_check_details` columns are PostgreSQL `BYTEA`, encrypted at rest via the same `AesGcmAttributeConverter` shipped Wave 18b1 (Phase 1A). Plaintext NEVER persisted. **Rationale:** LLTP số 2 references criminal-record check on a person who works with minors — same special-protection class as Incident sensitive fields. **Compliance check:** Compliant — PDPL Decree 13/2023/NĐ-CP Art 16. | 1B foundation |
| BR-VETTING-003 | RBAC — SAFEGUARDING_OFFICER only | `/api/v1/vettings/*` reads + writes restricted to callers carrying `SAFEGUARDING_OFFICER` role on the `X-User-Roles` header forwarded by the Gateway. Anyone else receives 403 `VETTING_RBAC_DENIED`. Staff teachers without an APPROVED record are blocked from student-PII endpoints by a separate filter (Phase 1B follow-up, deferred). **Rationale:** Decree 56/2017 §Đ.25 mandates background-check on adults with student-PII access; controller-layer gate stops unverified teacher access at the perimeter. **Compliance check:** Compliant — Decree 56/2017 + Đ.25. | 1B foundation |
| BR-VETTING-004 | Storage contract pinned, MinIO impl deferred | `VettingDocumentStorage` interface with `storeDocument` / `getDownloadUrl` / `deleteDocument`. Phase 1B foundation ships `MinIOVettingDocumentStorageImpl` as a stub (deterministic URLs, INFO log, no-op delete) so callers compile + smoke-test against the contract. Concrete MinIO SDK wiring (server-side AES-256, signed URLs, 7-year retention bucket policy) deferred to Phase 1B follow-up. **Rationale:** pin contract early to unblock UI work without coupling to backend timing. **Compliance check:** Considered — concrete encryption + retention pending; tracked in GAP-322b follow-up. | 1B foundation |
| BR-VETTING-005 | Soft-delete + audit | Vetting records inherit BaseEntity soft-delete + audit columns (created_at/by, updated_at/by, version, deleted). Soft-delete preserves the row for audit per Decree 56/2017 §Đ.25 procedural-record requirement. Phase 1C will tighten anti-delete on REJECTED + 7-year retention enforcement (parallels BR-CHILD-PROT-012 for incidents). **Rationale:** vetting decisions must be reproducible 7 years on for audit / criminal liability. **Compliance check:** Compliant (foundation) — full retention enforcement deferred to GAP-322c. | 1B foundation (tightens 1C) |

## Log

- **2026-05-04** (v0.2): Phase 1B foundation — vetting workflow rules BR-VETTING-001..005 added; sister of Phase 1A BR-CHILD-PROT-001..017. Wave 18b2 Bucket B (GAP-322b). Vetting service-level state machine + AES-256 on `lltp_number` + `police_check_details` + RBAC gate on `/api/v1/vettings/*` (SAFEGUARDING_OFFICER only) + `VettingDocumentStorage` contract with stub impl shipped. LLTP file upload UI + verify queue UI + concrete MinIO SDK wiring + 7-year retention enforcement deferred to Phase 1B follow-up + Phase 1C (GAP-322c).
- **2026-05-04** (v0.1): Phase 1A foundation — entity + encryption + role seed shipped (Wave 18b1 Bucket E). Phase 1B (GAP-322b) + Phase 1C (GAP-322c) sister gaps to be filed by closure coordinator.
