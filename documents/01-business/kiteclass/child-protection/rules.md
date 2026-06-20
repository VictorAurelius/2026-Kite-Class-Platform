# Child Protection — Business Rules

**Domain:** KiteClass Core / Compliance / Safeguarding
**Version:** 0.5 (Phase 1A + Phase 1B foundation + Phase 1B remainder + Phase 1C v1 + Phase 1C v1.5 retention)
**Created:** 2026-05-04
**Last-Reviewed:** 2026-05-06
**Reviewer-Approver:** @nguyenvankiet (acting Legal scout, solo-dev, 2026-05-06). Formal Vietnamese child-protection counsel review queued via GAP-156.

---

## Frontmatter (per `.claude/rules/business-logic-review.md` 5-attribute)

| Attribute | Value |
|-----------|-------|
| **Source** | (1) Luật Trẻ em 2016 Đ.6 (quyền được bảo vệ), Đ.25 (vetting nhân sự), Đ.51 (mandatory reporting ≤24h), Đ.54 (môi trường mạng); (2) Decree 56/2017/NĐ-CP (chi tiết Đ.25 vetting procedure); (3) PDPL Decree 13/2023/NĐ-CP Điều 16 (special protection of children's PII); (4) Bộ luật Hình sự Đ.147 (CSAM criminal liability); (5) P5 K-12 persona review 2026-05-04 Finding 2 (AC-EDGE-005 / AC-ONBOARD-005 / AC-COMM-006) |
| **Rationale** | Schools handle minors → Vietnamese law imposes criminal liability on (a) failure to report suspected abuse to MOLISA + công an ≤24h (Đ.51), (b) allowing unvetted staff access to children (Đ.25 + Decree 56/2017), (c) mishandling children's personal data (Decree 13/2023 Art 16). Phase 1A ships entity + encryption-at-rest skeleton — the FOUNDATION for Phase 1B vetting workflow + Phase 1C mandatory-reporting banner + audit log. AES-256-GCM with per-field random IV chosen because (i) industry-standard AEAD construction, (ii) defends pattern-analysis on encrypted columns, (iii) 128-bit auth tag detects tampering. 7-year retention chosen because (a) Decree 13/2023 Art 16 minimum 24mo for service-related minor PII, (b) Bộ luật Tố tụng Hình sự statute-of-limitations on child-abuse offences extends past 24mo, (c) parallels existing financial-record retention class — single audit policy across two compliance regimes simplifies data lifecycle. |
| **Reviewer** | @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-04). Formal child-protection counsel + DPO review queued — see GAP-156 acceptance criteria item. Phase 1B/1C closure REQUIRES legal counsel sign-off before K-12 tenant onboarding flag is enabled. |
| **Compliance check** | **Considered (self-assessed, counsel pending GAP-156 AC-D)** — per `documents/00-brd/compliance-checklist.md` L1/L5: **Luật Trẻ em 2016** (Đ.6 quyền được bảo vệ, Đ.25 vetting nhân sự, Đ.51 mandatory reporting ≤24h); **Nghị định 13/2023/NĐ-CP (PDPL) Điều 20** (bảo vệ dữ liệu cá nhân của trẻ em — canonical child-PII provision; AES-256-GCM at-rest control BR-CHILD-PROT-002); **Luật An ninh mạng 2018** (Đ.54 môi trường mạng / data security). ⚠️ Self-assessed only — Phase 1A = encryption skeleton; Đ.51 banner (Phase 1C) + Đ.25 vetting (Phase 1B) still PARTIAL. **K-12 tenant onboarding BLOCKED until Phase 1B+1C ship AND child-protection counsel signs off (GAP-156 AC-D).** **Citation-reconcile flag:** per-rule blocks §2/§6/§7/§8 cite "Decree 13/2023 Art 16" — compliance-checklist canonical is **Điều 20** (children's data); reconcile Art 16 vs Điều 20 with counsel before K-12 launch. |
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
| BR-VETTING-004 | Storage — concrete MinIO SDK | `VettingDocumentStorage` interface with `storeDocument` / `getDownloadUrl` / `deleteDocument`. **Wave 18b3 (Phase 1B remainder)** ships `MinIOVettingDocumentStorageImpl` as concrete AWS SDK v2 wiring against the existing `S3Client` + `S3Presigner` beans (`S3Config`); reuses `storage.s3.*` credentials but routes vetting evidence to a dedicated bucket `childprotection.minio.bucket` (default `kiteclass-vetting`). Object key layout: `vetting/{vettingId}/{sanitized-filename}`. Presigned download URLs capped at 15 minutes regardless of caller TTL. Server-side encryption assumed at bucket policy level (SSE-S3) — bucket admin owns the lifecycle policy. 7-year retention bucket lifecycle policy + delete-prevention on REJECTED records deferred to Phase 1C (GAP-322c). **Rationale:** dedicated bucket scope keeps RBAC + retention narrow to special-protection class; presigned-URL TTL cap protects against credential-leak blast radius. **Compliance check:** Compliant for at-rest + access-cap; retention enforcement deferred to Phase 1C. | 1B remainder |
| BR-VETTING-006 | LLTP upload endpoint + 10MB cap | `POST /api/v1/vettings/{vettingId}/documents` accepts a single multipart file (`file` field). Server-side caps: ≤10MB (returns 400 `VETTING_DOC_TOO_LARGE`); empty file rejected (`VETTING_DOC_EMPTY`); blank filename rejected (`VETTING_DOC_FILENAME_REQUIRED`); RBAC SAFEGUARDING_OFFICER enforced via existing `requireSafeguardingOfficer` (`X-User-Roles` header). Vetting record existence verified before persist (404 surfaces). Response 201 returns `{vettingId, storageKey, sizeBytes, contentType}`. **Rationale:** 10MB cap covers PDF LLTP scans + CCCD photos comfortably; resumable / chunked upload deferred to follow-up sister gap. **Compliance check:** Compliant — Decree 56/2017 §Đ.25 requires document persistence; PDPL Decree 13/2023 Art 16 satisfied via dedicated bucket + at-rest encryption (BR-VETTING-002 + BR-VETTING-004). | 1B remainder |
| BR-VETTING-005 | Soft-delete + audit | Vetting records inherit BaseEntity soft-delete + audit columns (created_at/by, updated_at/by, version, deleted). Soft-delete preserves the row for audit per Decree 56/2017 §Đ.25 procedural-record requirement. Phase 1C will tighten anti-delete on REJECTED + 7-year retention enforcement (parallels BR-CHILD-PROT-012 for incidents). **Rationale:** vetting decisions must be reproducible 7 years on for audit / criminal liability. **Compliance check:** Compliant (foundation) — full retention enforcement deferred to GAP-322c. | 1B foundation (tightens 1C) |

## 7. Phase 1C v1 rules (GAP-322c, Wave 19 Bucket A — mandatory reporting + audit log)

These rules use the prefix **`BR-CHILD-PROTECT-*`** (note: distinct from the Phase 1A prefix `BR-CHILD-PROT-*`) to keep Phase 1C's mandatory-reporting + audit-log subdomain easy to grep without colliding with the existing 17 Phase 1A rules. Bucket A authors them; Bucket D consumes BR-CHILD-PROTECT-005 (visibility scope) for the parent-portal conduct facet.

Each rule below carries the full 5-attribute frontmatter per `.claude/rules/business-logic-review.md`:
- **Source** common to BR-CHILD-PROTECT-005..007: (1) Luật Trẻ em 2016 Đ.51 (mandatory reporting ≤24h to Tổng đài 111 + công an địa phương); (2) PDPL Decree 13/2023/NĐ-CP Art 16 (children's PII special protection); (3) Bộ luật Hình sự Đ.147 (CSAM criminal liability); (4) Decree 56/2017/NĐ-CP §Đ.25 (child-abuse reporting procedure); (5) ND-13/2023/NĐ-CP financial-record retention precedent (parallels 7-year retention class).
- **Reviewer** common: @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-05). Formal child-protection counsel + DPO review queued — see GAP-156. Phase 1C closure REQUIRES legal counsel sign-off before K-12 tenant onboarding flag enables.
- **Review cadence** common: Annual + event-driven on amendment of Luật Trẻ em 2016 Đ.51, PDPL Decree 13/2023, Decree 56/2017, or BLHS Đ.147. **Next review:** 2027-05-05.

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-CHILD-PROTECT-005 | Visibility scope | `incidents.visibility_scope` ∈ {`PARENT_VISIBLE`, `PUBLIC`, `STAFF_ONLY`, `RESTRICTED`}. Defaults to `STAFF_ONLY` (legacy + new rows). Parent-portal conduct facet (Bucket D) JPQL filter only surfaces `PARENT_VISIBLE` + `PUBLIC`. **Rationale:** without this column ABUSE / GROOMING records risk leaking to parents through the conduct facet; the column is the gate. **Compliance check:** Compliant — PDPL Decree 13/2023 Art 16 (child PII restricted to safeguarding roles by default). | 1C v1 |
| BR-CHILD-PROTECT-006 | Đ.51 mandatory reporting trigger | When an Incident reaches `severity=CRITICAL` AND `category ∈ {ABUSE, GROOMING, CSAM}` the system fires `IncidentCriticalEvent` (after-commit) → audit log entry + FE banner. Banner CTA wires to `POST /api/v1/incidents/{id}/mandatory-report-ack` which the safeguarding officer calls after they have actually filed the external report. **Rationale:** Đ.51 imposes school-side criminal liability for failure to report ≤24h; the banner removes ambiguity ("did anyone report?") and the audit log proves the chain to công an / MOLISA. **Compliance check:** Compliant — Luật Trẻ em 2016 Đ.51 + Decree 56/2017 §Đ.25 (procedural standard); BLHS Đ.147 for CSAM. | 1C v1 (more channels Phase 1C remainder) |
| BR-CHILD-PROTECT-007 | Hash-chain audit log | Table `child_protection_audit_log` is append-only. Each row stores `prev_hash` + `content_hash = SHA-256(prev_hash || canonical_payload_json)` per `(instance_id, entity_type)` chain (genesis `prev_hash` = 64-char zeros). DELETE/TRUNCATE revoked from typical app role at V54 migration; daily integrity verification cron deferred to Phase 1C remainder. **Rationale:** non-repudiation — audit must outlive admin-tier compromise. Hash chain detects post-hoc edits even if a hostile DBA bypasses the GRANT. **Compliance check:** Compliant — Luật Trẻ em 2016 Đ.51 (proof-of-report when audited); ND-13/2023 (7-year financial-record class precedent for retention). 7-year retention column + soft-delete block deferred to Phase 1C remainder. | 1C v1 (retention enforcement remainder) |

## 8. Phase 1C v1.5 rules (GAP-359 sub-tasks 359.1 + 359.5, Wave 24 Bucket A — retention enforcement + hash-chain cron)

These rules extend §7 BR-CHILD-PROTECT-* with the deferred remainder shipped Wave 24 Bucket A. They share the §7 common 5-attribute frontmatter (Source / Reviewer / Review cadence) and are reproduced inline only where the per-rule rationale + compliance check differ. Sub-tasks 359.2 (pen test), 359.3 (4-level escalation), 359.4 (full report page UI), 359.6 (Tổng đài 111 webhook Stage 2) remain deferred.

| ID | Rule | Detail | Phase |
|----|------|--------|-------|
| BR-CHILD-PROTECT-008 | 7-year retention enforcement on CLOSED incidents | When `IncidentService.updateStatus(...)` transitions an Incident to `CLOSED`, the service stamps `incidents.retention_until = closed_at + 7 years` (current implementation: `Instant.now() + 7 × 365 days`). Soft-delete attempts via `IncidentService.softDelete(...)` while `retention_until > now` throw `RetentionWindowActiveException` (HTTP 409 + `INCIDENT_RETENTION_WINDOW_ACTIVE`). After expiry the daily `RetentionLifecycleService` cron (02:00) secure-deletes the row (mark deleted + null-out `description` + null-out `evidence_paths`) and appends `INCIDENT_RETENTION_EXPIRED_DELETE` to the hash-chain audit log. Retention deadline is sticky — once set on first-close it does not extend on re-close. **Source:** PDPL Decree 13/2023/NĐ-CP Art 16 (children's PII special protection) + Luật Trẻ em 2016 Đ.51 (mandatory-reporting follow-through requires audit trail to outlive operational lifecycle) + BLHS Đ.147 (CSAM statute of limitations) + ND-13/2023 financial-record retention precedent. **Rationale:** 7 years parallels Vietnamese financial-record retention class — single audit policy across two compliance regimes simplifies data lifecycle. Sticky deadline (set on first-close, not last) prevents adversarial re-opening to extend retention indefinitely. Service-layer block + DB cron + audit-log entry is belt-and-suspenders against admin bypass. **Reviewer:** @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-06). Formal child-protection counsel sign-off queued via GAP-156. **Compliance check:** Compliant — PDPL Decree 13/2023 Art 16 minor-PII retention class; ND-13/2023 financial-record class precedent. **Review cadence:** Annual + event-driven on amendment of PDPL Decree 13/2023 / Luật Trẻ em 2016 / BLHS Đ.147. **Next review:** 2027-05-06. | 1C v1.5 |
| BR-CHILD-PROTECT-009 | Daily hash-chain integrity verification cron | The `AuditChainVerificationCron` runs daily at 02:30 (after 02:00 retention sweep so retention-driven appends land before verification). Iterates every distinct `(instance_id, entity_type)` chain from `child_protection_audit_log` and re-computes `SHA-256(prev_hash || payload_json)` for each entry via `ChildProtectionAuditService.verifyChain(instanceId, entityType)`. On break: WARN log + `child_protection.audit.chain.break{instance, entityType}` Micrometer counter increment (alert pattern parallels existing `RateLimitBreachSpike`). Per-chain checked: `child_protection.audit.chain.verified{instance, entityType, result=pass\|fail}`. Operational runbook: `documents/05-guides/operations/audit-chain-break-runbook.md`. **Source:** BR-CHILD-PROTECT-007 non-repudiation invariant + Luật Trẻ em 2016 Đ.51 (proof-of-report when audited by công an / MOLISA) + standard tamper-evident audit-log pattern. **Rationale:** V54 `REVOKE DELETE` is line-of-defence #1 against application-tier tamper; daily verification is line-of-defence #2 against rogue DBA / direct-SQL bypass that the GRANT cannot stop. Counter alert pattern reuses existing observability stack — no new infrastructure. **Reviewer:** @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-06). Formal sign-off queued via GAP-156. **Compliance check:** Compliant — supports Luật Trẻ em 2016 Đ.51 chain-of-custody requirement; reinforces BR-CHILD-PROTECT-007. **Review cadence:** Annual + event-driven on amendment of Luật Trẻ em 2016 Đ.51 / Decree 56/2017. **Next review:** 2027-05-06. | 1C v1.5 |

### Phase 1C v1.5 boundary

- ✅ `incidents.retention_until` column (V57 migration) + 7-year stamp on CLOSED transition
- ✅ Soft-delete blocked while retention window active (`RetentionWindowActiveException` HTTP 409)
- ✅ Daily `RetentionLifecycleService` cron — secure-delete + audit-log append
- ✅ Daily `AuditChainVerificationCron` cron — hash-chain integrity verification + Micrometer counters
- ✅ Audit-chain-break runbook for SRE response
- ❌ Pen test execution + remediation (GAP-359 sub-task 359.2)
- ❌ AC-COMM-006 4-level complaint escalation (GAP-359 sub-task 359.3, depends GAP-339)
- ❌ Full UC-INCIDENT-CRITICAL-REPORT page UI (GAP-359 sub-task 359.4)
- ❌ Tổng đài 111 webhook (GAP-359 sub-task 359.6, Stage 2 — Q4 2026)

### Phase 1C v1 boundary

- ✅ Visibility scope column + default `STAFF_ONLY`
- ✅ `IncidentCriticalEvent` + `IncidentTransitionListener` after-commit audit log on CRITICAL+abuse-category
- ✅ Hash-chain table with append-only invariant + REVOKE DELETE
- ✅ Mandatory-report banner FE (warning + ack states)
- ✅ `POST /api/v1/incidents/{id}/mandatory-report-ack` endpoint with SAFEGUARDING_OFFICER RBAC + audit append
- ❌ 7-year retention column + soft-delete block (Phase 1C remainder follow-up)
- ❌ Daily hash-chain integrity verification cron (Phase 1C remainder)
- ❌ Pen test execution + remediation (Phase 1C remainder)
- ❌ AC-COMM-006 4-level complaint escalation (depends GAP-339)
- ❌ Full UC-INCIDENT-CRITICAL-REPORT page UI (Phase 1C remainder; banner + endpoint suffice for v1)
- ❌ Tổng đài 111 webhook (Stage 2 — Q4 2026)

## Log

- **2026-05-06** (v0.5): Phase 1C v1.5 retention — Wave 24 Bucket A (GAP-359 sub-tasks 359.1 + 359.5). Added §8 with BR-CHILD-PROTECT-008 (7-year retention on CLOSED + soft-delete block via `RetentionWindowActiveException` HTTP 409 + daily `RetentionLifecycleService` cron at 02:00 → secure-delete + audit append) and BR-CHILD-PROTECT-009 (daily `AuditChainVerificationCron` at 02:30 → re-compute `SHA-256(prev_hash || payload_json)` per chain + Micrometer counters `child_protection.audit.chain.break{instance,entityType}` + `child_protection.audit.chain.verified{instance,entityType,result}`). V57 migration adds `incidents.retention_until` column + backfill `COALESCE(updated_at, created_at) + 7y` for safety + partial index for cron scan. Operational runbook shipped at `documents/05-guides/operations/audit-chain-break-runbook.md`. Sub-tasks 359.2 (pen test), 359.3 (4-level escalation), 359.4 (full report page UI), 359.6 (Tổng đài 111 webhook Stage 2) remain deferred — GAP-359 stays 🔵 OPEN until coordinator closes Phase 1C complete.
- **2026-05-05** (v0.4): Phase 1C v1 — Wave 19 Bucket A (GAP-322c v1). Added §7 with BR-CHILD-PROTECT-005 (visibility scope), BR-CHILD-PROTECT-006 (Đ.51 mandatory reporting trigger), BR-CHILD-PROTECT-007 (hash-chain audit log) — full 5-attribute frontmatter. V54 migration adds `incidents.visibility_scope` (DEFAULT `STAFF_ONLY`) + `child_protection_audit_log` (hash-chain, append-only via REVOKE DELETE). `IncidentCriticalEvent` fires on CRITICAL+abuse-category create (after-commit listener appends audit row). FE `IncidentBanner.tsx` cites Đ.51 + 24h obligation. `POST /api/v1/incidents/{id}/mandatory-report-ack` endpoint persists audit entry. 7-year retention enforcement, daily integrity cron, pen test, 4-level complaint escalation, full UC-INCIDENT-CRITICAL-REPORT page UI deferred to Phase 1C remainder follow-up gap.
- **2026-05-04** (v0.3): Phase 1B remainder — Wave 18b3 Bucket B (GAP-322b remainder). BR-VETTING-004 reframed from "stub" to "concrete MinIO SDK" (AWS SDK v2, dedicated `kiteclass-vetting` bucket, 15-min presigned URL TTL cap). BR-VETTING-006 added covering the new `POST /api/v1/vettings/{vettingId}/documents` multipart upload endpoint (10MB cap; reuse RBAC + existence check). LLTP upload form FE shipped at `/admin/vetting/[vettingId]/upload`. 7-year retention bucket policy + virus-scan webhook + resumable upload + audit-log entries on upload remain deferred to Phase 1C (GAP-322c) + follow-up sister gaps.
- **2026-05-04** (v0.2): Phase 1B foundation — vetting workflow rules BR-VETTING-001..005 added; sister of Phase 1A BR-CHILD-PROT-001..017. Wave 18b2 Bucket B (GAP-322b). Vetting service-level state machine + AES-256 on `lltp_number` + `police_check_details` + RBAC gate on `/api/v1/vettings/*` (SAFEGUARDING_OFFICER only) + `VettingDocumentStorage` contract with stub impl shipped. LLTP file upload UI + verify queue UI + concrete MinIO SDK wiring + 7-year retention enforcement deferred to Phase 1B follow-up + Phase 1C (GAP-322c).
- **2026-05-04** (v0.1): Phase 1A foundation — entity + encryption + role seed shipped (Wave 18b1 Bucket E). Phase 1B (GAP-322b) + Phase 1C (GAP-322c) sister gaps to be filed by closure coordinator.
