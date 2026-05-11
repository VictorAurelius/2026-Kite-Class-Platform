# GAP-322: Child Protection Workflow — CRIMINAL LIABILITY (Luật Trẻ em 2016 Đ.51 + Đ.25 + Decree 56/2017)

**Status:** 🟡 PARTIAL — Phase 1A foundation shipped 2026-05-04 (Wave 18b1 Bucket E). Phase 1B (vetting workflow + MinIO encrypted bucket + RBAC gate) → GAP-322b. Phase 1C (mandatory-reporting banner + hash-chained audit log + 7y retention) → GAP-322c.
**Priority:** 🔴 P0 LEGAL (criminal liability if breached)
**Domain:** Backend + Compliance + Frontend
**Detected:** 2026-05-04 (Wave 17 Bucket D — P5 K-12 persona review)
**Related Docs:**
- `documents/00-brd/persona-reviews/P5-k12-school-round-1-2026-05-04.md` Finding 2
- `documents/00-brd/persona-criteria/P5-k12-school.md` AC-EDGE-005, AC-ONBOARD-005, AC-COMM-006
- `documents/00-brd/child-protection-policy.md` (policy skeleton)
- Existing GAP-186 (policy-only, no workflow)

## Current State (verified 2026-05-04 per GAP-345)

### ⚠️ Existing-but-unrelated (clarification — DO NOT confuse)

| Piece | Status | Note |
|-------|--------|------|
| `kiteclass-core/module/legal/` | ⚠️ EXISTS | Contains DMCA + Trademark IP-protection only (`DmcaTakedownRequest.java`, `DmcaService.java`, `TrademarkCheckService.java`, `TrademarkCheckResult.java`). **NOT** child-protection workflow. Module name is potentially confusing — readers MUST verify before assuming overlap. |
| `kiteclass-core/module/moderation/` | ⚠️ EXISTS | Likely AI/content moderation (separate concern from safeguarding workflow). Verify scope before reuse. |
| `module/storage/StorageServiceImpl.java` | ⚠️ has `child.protection` text-grep hit | Verified false-positive — generic comment, not workflow. |

### ❌ Greenfield (this gap's scope)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Child protection policy doc | `documents/00-brd/child-protection-policy.md` | ✅ exists (skeleton — GAP-186) |
| Safeguarding officer role | `kiteclass-core/.../user/Role.java` | ❌ missing |
| Encrypted incident ticket entity | `kiteclass-core/.../incident` | ❌ missing |
| Mandatory reporting to Tổng đài 111 + công an | — | ❌ missing |
| Staff vetting (LLTP upload + verify workflow) | — | ❌ missing |
| MinIO encrypted storage for vetting evidence | — | ❌ missing |
| Non-repudiation audit log | — | ❌ missing |
| GAP-186 status | `documents/04-quality/gaps/GAP-186-child-protection-policy.md` | 🔵 OPEN — policy gap, this gap is workflow implementation |

**Grep + verification commands run 2026-05-04:**
```bash
grep -rl "incident\|safeguard\|abuse\|child.protection\|vetting" kiteclass/ --include="*.java"
# → 2 false-positive hits (storage + retention modules — generic comments, not workflow)
ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/legal/entity/
# → DmcaStatus.java + DmcaTakedownRequest.java only — NOT child-protection
ls kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/legal/service/
# → DmcaService.java + TrademarkCheckService.java only — NOT safeguarding
```
**Verdict:** Child-protection workflow itself is fully greenfield, but `module/legal/` and `module/moderation/` exist with potentially confusable names. This gap MUST create a new module (proposed: `module/childprotection/` or `module/safeguarding/`) — DO NOT add to `module/legal/` (DMCA-specific).

## Problem

K-12 schools handle minors. Vietnamese law imposes criminal liability on:

1. **Schools failing to report suspected abuse to MOLISA + công an within 24h** (Luật Trẻ em 2016 Điều 51).
2. **Allowing unvetted staff to access children** (Luật Trẻ em 2016 Điều 25 + Decree 56/2017).
3. **Mishandling children's personal data** (PDPL Decree 13/2023 Điều 16 — special protection).

Without this workflow:
- AC-EDGE-005 (child safety incident) FAIL — no encrypted ticket, no auto-suggest reporting, no evidence preservation
- AC-ONBOARD-005 (staff vetting 50 GV ≤7d) FAIL — no LLTP upload, no verify workflow, no MinIO encryption
- AC-COMM-006 (complaint escalation 4-level) FAIL — no safeguarding officer routing
- Platform liable as data processor under PDPL Decree 13 Art 16 special-protection clause

## Context

P5 K-12 persona review (2026-05-04) identified this as Finding 2 — second-highest P0 LEGAL after parent portal. GAP-186 was created earlier as policy skeleton; this gap (GAP-322) is the **workflow implementation** that policy mandates.

Persona simulation showed real risk: PH HS D 7A reports suspected online bullying with sensitive image → today system has no encrypted-ticket path → image goes through normal ticket queue → leak risk → school + platform criminally liable.

## Evidence

- Luật Trẻ em 2016 Điều 25: vetting nguời làm việc với trẻ em (background check + LLTP)
- Luật Trẻ em 2016 Điều 51: mandatory reporting nghi ngờ xâm hại trẻ em ≤24h to Tổng đài 111 + cơ quan công an
- Decree 56/2017/NĐ-CP: chi tiết Điều 25 — quy định cụ thể vetting procedure
- Decree 13/2023/NĐ-CP Điều 16: special protection of personal data of children — encryption + minimization + parental consent
- P5 review report Finding 2: 0% child-protection workflow coverage

## Proposed Fix

### Phase 1 — Staff vetting (Stage 1, Q3 2026)

1. **Vetting workflow:** HR/admin upload xlsx + zip per GV (CCCD scan + bằng tốt nghiệp + LLTP số 2 ≤6 tháng + ảnh 3×4)
2. **MinIO encrypted bucket:** `staff-vetting-evidence/{tenantId}/{userId}/`, AES-256 at rest
3. **Verify queue:** Admin-Kite reviews each → status `pending → verified | rejected`
4. **RBAC gate:** GV role cannot access student data until `verified=true` enforced at security filter
5. **Re-vetting cadence:** Annual reminder; LLTP refresh ≤2 years

### Phase 2 — Safeguarding officer + encrypted incident tickets (Stage 1, Q3 2026)

1. **New role:** `safeguarding_officer` distinguished from `admin`
2. **Encrypted incident entity:** `Incident` with field-level encryption on `description`, `evidence_paths`; only safeguarding officer + Hiệu trưởng + designated counselor can decrypt
3. **Auto-suggest mandatory reporting:** When incident `severity=CRITICAL` + `category=ABUSE|GROOMING|CSAM`, system shows banner "Luật Trẻ em 2016 Đ.51 — báo cáo Tổng đài 111 + công an địa phương ≤24h"
4. **Non-repudiation audit log:** Hash-chained log entries (immutable); admin cannot delete
5. **Evidence preservation:** When incident closes, evidence retained 7 years (financial-record class retention per ND-13/2023)

### Phase 3 — External integration (Stage 2, Q4 2026)

- Tổng đài 111 webhook (if MOLISA exposes API) or PDF export for offline submission
- Công an địa phương coordinator workflow

## Acceptance Criteria

### Phase 1A — DONE 2026-05-04 (Wave 18b1 Bucket E)

- [x] `Incident` entity with field-level encryption on sensitive fields (V49__add_child_protection_incidents.sql)
- [x] `SAFEGUARDING_OFFICER` system-template role + 3 permissions seeded at NIL UUID (cloned per-tenant by RoleSeederService)
- [x] AES-256-GCM `AesGcmAttributeConverter` with per-field random IV + 128-bit auth tag (16 unit tests; tampered IV/cipher/tag detection verified)
- [x] `IncidentService` Phase 1A CRUD skeleton: create / findById / findAll / updateStatus / assignOfficer / softDelete (17 unit tests)
- [x] Documentation 3-layer per `documents/01-business/kiteclass/child-protection/` (rules.md / use-cases.md / api-contract.md)
- [x] business-logic-review.md 5-attribute frontmatter (Source: Luật Trẻ em + Decree 13/2023 + Decree 56/2017 + BLHS Đ.147; Compliance: Compliant for Phase 1A skeleton; Reviewer: solo-dev acting Legal scout — formal counsel review queued via GAP-156; Cadence: Annual + event-driven on Luật Trẻ em / Decree amendment)
- [x] mvn test green (kiteclass-core full suite: 1192 tests, 0 failures, 0 errors)

### Phase 1B — DEFERRED to GAP-322b

- [ ] Staff vetting workflow: upload (LLTP số 2 + bằng tốt nghiệp + CCCD scan + ảnh 3×4) + verify queue + RBAC-gate teacher access until `verified=true`
- [ ] MinIO encrypted bucket `staff-vetting-evidence/{tenantId}/{userId}/` (AES-256 at rest)
- [ ] RBAC gate on Incident decryption — only `SAFEGUARDING_OFFICER` + `PRINCIPAL` + `COUNSELOR` may invoke decrypt-aware reads
- [ ] State-machine enforcement on `IncidentStatus` transitions (Phase 1A allows arbitrary)
- [ ] HTTP REST endpoints (POST / GET list / GET id / PUT status / PUT officer / DELETE)
- [ ] PH/HS/GV submission channels (parent → GAP-321 portal; GV → kiteclass UI)
- [ ] Annual re-vetting reminder + 2-year LLTP refresh scheduled job

### Phase 1C — DEFERRED to GAP-322c

- [ ] Mandatory-reporting auto-suggest banner (Đ.51) when `severity=CRITICAL` + `category ∈ {ABUSE, GROOMING, CSAM}`
- [ ] Hash-chained non-repudiation audit log (admin cannot delete entries — DB trigger + RBAC)
- [ ] 7-year evidence retention enforcement (`status=CLOSED` rows delete-protected while age < 7y)
- [ ] CSAM no-delete rule (BR-CHILD-PROT-016)
- [ ] Penetration test: encrypted fields cannot be read via DB direct query without decrypt key
- [ ] Test scenario: PH submits critical incident with image → only safeguarding officer + HT see decrypted; system shows Đ.51 reporting reminder; audit log immutable

### Stage 2 — DEFERRED multi-quarter (Q4 2026)

- [ ] Tổng đài 111 webhook (if MOLISA exposes API) or PDF export for offline submission
- [ ] Công an địa phương coordinator workflow

## Related

- **Implements:** GAP-186 child-protection-policy (policy → workflow)
- **Blocks K-12 deployment:** without this, platform cannot legally onboard K-12 tenants
- **Depends on:** GAP-321 (parent portal — incident submission channel)
- **Cross-cuts:** GAP-339 (complaint workflow — separate but adjacent), GAP-184 (retention 7y for evidence)
- **Wave plan:** Bucket D
- **Reviewer:** Phase 2 multi-stakeholder includes Legal counsel (child protection) per P5-k12-school.md §Reviewer Hat

## Log

- **2026-05-04 (Phase 1A SHIPPED — Wave 18b1 Bucket E)** — Foundation delivered: NEW `kiteclass-core/module/childprotection/` module with `AesGcmAttributeConverter` (AES-256-GCM, per-field 96-bit random IV, 128-bit auth tag, BYTEA storage), `Incident` entity with `@Convert` on `description` + `evidence_paths` columns, 3 enums (Severity/Category/Status), `IncidentRepository`, `IncidentService` Phase 1A CRUD skeleton, V49 migration with 7 indexes + CHECK constraints + role + 3 permission seeds. Tests: 33 new unit tests (16 converter + 17 service) covering encrypt/decrypt roundtrip, per-field random IV, tampered IV+cipher+tag detection (GCM auth tag), Vietnamese diacritics + emoji UTF-8 roundtrip, prod-profile fail-fast, dev-profile ephemeral-key fallback, validation error paths. Full kiteclass-core suite 1192 tests / 0 failures / 0 errors. 3-layer business docs created with `business-logic-review.md` 5-attribute frontmatter (cited Luật Trẻ em 2016 Đ.6/25/51/54 + Decree 56/2017 + Decree 13/2023 Art 16 + BLHS Đ.147). Phase 1B (GAP-322b) + Phase 1C (GAP-322c) sister gaps to be filed by closure coordinator. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp (deferred items have follow-up gap references). K12_ENTERPRISE tier remains BLOCKED until Phase 1B + Phase 1C ship + legal-counsel sign-off via GAP-156.
- **2026-05-04 (revision per GAP-345)** — Updated Current State to clarify `module/legal/` exists but is DMCA + Trademark IP-protection (NOT confusable with child-protection workflow); `module/moderation/` likely AI/content moderation (separate concern). Verdict unchanged — workflow itself fully greenfield, but explicit guidance added: this gap MUST create new module `module/childprotection/` or `module/safeguarding/`, do NOT extend `module/legal/`. Status remains 🔵 OPEN.
- **2026-05-04** — Filed during Wave 17 Bucket D P5 review. State-check: zero pre-existing implementation; GAP-186 is policy skeleton only. This gap is workflow implementation. Carries criminal-liability flag — recommend NOT enable K12_ENTERPRISE tier until landed.
