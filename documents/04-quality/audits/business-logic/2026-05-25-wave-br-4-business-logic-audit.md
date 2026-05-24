---
title: Business Logic Audit — Wave beta-readiness-4 Post-Wave
status: complete
created: 2026-05-25
phase: phase-1-beta
wave: beta-readiness-4
audit_skill: business-logic-audit
audit_version: v2 (per-check rubric)
audit_rubric: .claude/rules/audit-skill-rubric-business-logic-audit.md
baseline_audit: documents/04-quality/audits/business-logic/2026-05-19-wave-98-new-domains.md
baseline_score: 73/100 C+ (PARTIAL FAIL Cat 1)
gaps: [GAP-292, GAP-292b, GAP-291, GAP-664, GAP-666]
prs_audited:
  - PR #1781 (8b0a8d68) — Bucket D reschedule
  - PR #1782 (5378fca3) — Bucket B PDPL consent
  - PR #1783 (5937ee71) — Bucket C Pricing + payment
  - PR #1784 (5e3ceebe) — Course hotfix pricingModel field
  - PR #1787 (9ce75c17) — ClassMapper @Mapping ignore hotfix
  - PR #1788 (36c71948) — strict-warnings hotfix
adrs_audited:
  - ADR-035 (pricing-model-taxonomy, ACCEPTED 2026-05-24)
audience: dev
---

# Business Logic Audit — Wave beta-readiness-4 Post-Wave (2026-05-25)

## Scope

Wave beta-readiness-4 (last merge 2026-05-24, PR #1789) shipped 5 buckets + 3 hotfixes + 1 META rule = 11 PRs. Audit này focus 3 buckets touch business logic:

| Bucket | Scope | Domain mới tạo? |
|---|---|---|
| **C — Pricing taxonomy** | 4-enum `PricingModel` + `PricingCalculator` + Course entity extensions + V67 migration | course-pricing (domain folder KHÔNG tạo) |
| **C-pair — Payment recording** | `PaymentRecord` entity + `PaymentRecordController` + V69 migration | payment-recording (domain folder KHÔNG tạo) |
| **D — Reschedule** | `Class.reschedule()` endpoint + `RescheduleReasonCategory` enum + 6 audit columns + Outbox event | reschedule (domain folder KHÔNG tạo) |

Audit phương pháp:
1. Apply 5-category rubric per `audit-skill-rubric-business-logic-audit.md` §2
2. Per-check pass/fail (no averaging) — bug list primacy per §4
3. Cross-reference ADR-035 + api-contract.md + Java code + tests
4. Delta vs baseline 73/100 C+ Wave 98

Out of scope:
- 22 existing domains 5-attribute backfill (covered by quarterly GAP-156)
- Bucket A META env-coverage (audit nó qua ops-readiness audit suite)
- Bucket B PDPL consent (audit nó qua security audit suite)
- Bucket E email tone matrix (đã shipped với DONE verdict; UC-EMAIL-TONE outside core BL scope)

---

## 1. Final Score — **64/100 (D+)** — DELTA **−9** vs Wave 98 (73/100 C+)

🔴 **PARTIAL FAIL** per audit-level verdict — 4 P0 sub-checks failed across Cat 1 + Cat 2 + Cat 4.

🔴 **Phase 1 BETA gate ≥80** — **MISS −16** (carry-forward Cat 1 5-attr backfill GAP-156 + new drift cluster Wave br-4 raises gap).

Score breakdown:

| Category | Score | Verdict | P0 fails | P1 fails | P2 fails |
|---|:--:|:--:|:--:|:--:|:--:|
| 1 — Rule Coverage | 8/20 | 🔴 FAIL (capped 16, additional −8 P1) | 2 | 4 | 0 |
| 2 — Config Accuracy | 12/20 | 🔴 FAIL (capped 16, additional −4 P1) | 1 | 2 | 1 |
| 3 — Edge Case Tests | 18/20 | ⚠️ PARTIAL | 0 | 0 | 2 |
| 4 — Cross-Domain Consistency | 10/20 | 🔴 FAIL (capped 16, additional −6 P1) | 1 | 2 | 0 |
| 5 — Stakeholder Alignment | 16/20 | ⚠️ PARTIAL | 0 | 1 | 1 |
| **Total** | **64/100** | 🔴 **PARTIAL FAIL** | **4 P0** | **9 P1** | **4 P2** |

---

## 2. Bug list (primacy per `audit-skill-rubric-business-logic-audit.md` §4)

### 🔴 P0 — 4 findings (block Phase 1 BETA gate)

#### P0-1 — 3 new domains MISSING 3-layer business docs (Living Docs rule violated)

**Rule violated:** `documents/01-business/README.md` §2 + CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"

**Evidence:**
```bash
ls documents/01-business/kiteclass/course-pricing/    # → No such directory
ls documents/01-business/kiteclass/payment-recording/ # → No such directory
ls documents/01-business/kiteclass/reschedule/        # → No such directory
```

3 new business domains shipped Wave br-4 NHƯNG KHÔNG có 3-layer folders:
- **course-pricing/** (4 BR-IDs BR-COURSE-PRICING-001..004) — không tồn tại
- **payment-recording/** (6 BR-IDs BR-PAYMENT-METHOD-001..006) — không tồn tại
- **reschedule/** (RescheduleReasonCategory enum + 6 audit cols + Outbox event) — không tồn tại

Tạm thời pricing rules được nhúng vào `course-class/api-contract.md` §Pricing Model API (line 235+) + reschedule endpoint trong cùng file (line 99+), payment methods trong `payment-invoice/rules.md` line 38 — **không có canonical rules.md riêng cho 3 domain mới**.

ADR-035 §References cite `documents/01-business/kiteclass/course-class/rules.md §6` NHƯNG section §6 KHÔNG tồn tại trong rules.md (file chỉ có §1-§5).

**Affected files (rules.md absent):**
- `documents/01-business/kiteclass/course-pricing/rules.md` (MISSING)
- `documents/01-business/kiteclass/course-pricing/use-cases.md` (MISSING)
- `documents/01-business/kiteclass/course-pricing/api-contract.md` (MISSING)
- `documents/01-business/kiteclass/payment-recording/rules.md` (MISSING)
- `documents/01-business/kiteclass/payment-recording/use-cases.md` (MISSING)
- `documents/01-business/kiteclass/payment-recording/api-contract.md` (MISSING)
- `documents/01-business/kiteclass/reschedule/rules.md` (MISSING)
- `documents/01-business/kiteclass/reschedule/use-cases.md` (MISSING)
- `documents/01-business/kiteclass/reschedule/api-contract.md` (MISSING)

**Impact:** không có canonical 5-attribute business rule statement (Source/Rationale/Reviewer/Compliance/Cadence) cho 3 domain — future reader/dev phải reverse-engineer từ Java code comments. Cross-bucket consistency rule (BR-COURSE-PRICING-004 reschedule period recalc) không có canonical narrative — sống duplicated trong PricingCalculator javadoc + reschedule endpoint comments.

**Mitigation needed:** GAP-NEW filed Wave br-5 Bucket creating 3 domain folders × 3 files. Mirror existing precedent (`audit-to-gap-pipeline.md` §2.5 + `check-3-layer-completeness.sh` CI detector).

**Cross-reference:** Recurrence #3 of same incident class (#1 Wave 92 GAP-640 admin-audit 3-layer; #2 Wave 98 GAP-662/663 preferences + email). `check-3-layer-completeness.sh` CI (active 2026-05-19) đã exist nhưng KHÔNG block Wave br-4 buckets — verify nếu detector đã wire vào script-quality.yml. **Follow-up: GAP-NEW-check-3-layer-not-firing investigation.**

#### P0-2 — PaymentMethod enum DUPLICATE + 3-way drift VIETQR vs ZALOPAY

**Rule violated:** Cat 2 §2.3 "No drift: renamed config keys reflected in BOTH rules.md AND application.yml (no silent renames)" + Cat 4 §4.1 "No rule in domain A contradicts rule in domain B"

**Evidence — 2 enum files exist simultaneously:**

File 1: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/constant/PaymentMethod.java`
```java
public enum PaymentMethod {
    CASH("Tiền mặt", "cash", true),
    BANK_TRANSFER("Chuyển khoản", "bank", true),
    MOMO("Ví MoMo", "momo", true),
    VNPAY("VNPay QR", "vnpay", true),       // ← VNPAY (not VIETQR)
    ZALOPAY("ZaloPay", "zalopay", true),    // ← ZALOPAY present
    CREDIT_CARD("Thẻ tín dụng", "card", false);
}
```

File 2: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/enums/PaymentMethod.java`
```java
public enum PaymentMethod {
    CASH(false),
    BANK_TRANSFER(false),
    MOMO(true),
    VNPAY(true),       // ← VNPAY (not VIETQR)
    ZALOPAY(...),      // ← ZALOPAY present
    // CREDIT_CARD absent
}
```

**Drift Source 3 — PaymentRecordService.java javadoc + comments (Wave br-4 NEW code):**
```java
// PaymentRecordService.java:34
*   <li>BR-PAYMENT-METHOD-001: method ∈ {CASH, BANK_TRANSFER, VIETQR, MOMO}</li>
// VIETQR present, ZALOPAY ABSENT, VNPAY ABSENT
```

**Drift Source 4 — payment-invoice/rules.md line 38:**
```
**Payment methods:** CASH, BANK_TRANSFER, VNPAY, MOMO, ZALOPAY
```

**Drift Source 5 — Wave plan + session handoff:**
> Wave plan §3.6 + session handoff §3 "Bucket C ghép PaymentMethod enum + record-payment endpoint" — không specify enum values

**4-way drift matrix:**

| Source | CASH | BANK_TRANSFER | VIETQR | MOMO | VNPAY | ZALOPAY | CREDIT_CARD |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| `common/constant/PaymentMethod.java` | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | ✅(unavailable) |
| `module/payment/enums/PaymentMethod.java` | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ |
| `PaymentRecordService.java` BR-PAYMENT-METHOD-001 javadoc | ✅ | ✅ | **✅** | ✅ | ❌ | ❌ | ❌ |
| `payment-invoice/rules.md` line 38 | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ |
| ADR-035 / Wave br-4 docs | (silent) | (silent) | (silent) | (silent) | (silent) | (silent) | (silent) |

**Impact:**
- `PaymentRecord.paymentMethod` column constraint check stipulates values từ ONE of the 2 enum files — runtime test sẽ FAIL nếu request body cite `VIETQR` (theo BR-PAYMENT-METHOD-001 javadoc) vì cả 2 enum đều không có VIETQR
- Cross-domain inconsistency rules.md (VNPAY/ZALOPAY) ≠ code citation (VIETQR/MOMO)
- Duplicate enum class = potential ambiguity về which import to use in new code

**Mitigation needed:**
- Consolidate to ONE canonical PaymentMethod enum (decommission duplicate)
- Reconcile VIETQR vs VNPAY (likely PaymentRecordService javadoc was wrong — VIETQR is QR-format, VNPAY is processor; both can coexist)
- Update payment-invoice/rules.md OR create payment-recording/rules.md with canonical 5-attribute rule

#### P0-3 — Course.pricingModel default `COURSE_PACKAGE` contradicts ADR-035 `PER_HOUR`

**Rule violated:** Cat 2 §2.2 "Config key VALUES match rules.md documented values"

**Evidence:**

ADR-035 §3 "Implementation" line 50:
> Migration V67 ALTER TABLE + DEFAULT 'PER_HOUR' (per VN market norm)

api-contract.md line 256:
> `pricingModel` enum YES (defaults PER_HOUR if omitted, matches V67 DEFAULT)

Java `Course.java:172`:
```java
@Column(name = "pricing_model", nullable = false, length = 32)
@Enumerated(EnumType.STRING)
private PricingModel pricingModel = PricingModel.COURSE_PACKAGE;  // ← COURSE_PACKAGE!
```

**Impact:**
- ADR documents PER_HOUR default (VN market norm Apollo/ILA)
- DB migration V67 sets DEFAULT 'PER_HOUR' (per ADR §3)
- Java entity in-memory default `COURSE_PACKAGE` — drifts từ DB default
- Behavior: new Course constructed via `new Course()` will have Java field `COURSE_PACKAGE` UNTIL INSERT to DB → DB-default `PER_HOUR` may NOT apply if NOT NULL value is sent from Java
- Inconsistent semantics: 87 existing courses GAP-NEW-pricing-data-reclassification (per session handoff §8) may be flagged differently based on which default was applied at migration time vs row-construction time

**Verification needed:**
```bash
# Confirm V67 migration DEFAULT clause
grep -A3 "ALTER TABLE.*pricing_model\|pricing_model.*DEFAULT" kiteclass/kiteclass-core/src/main/resources/db/migration/V67*.sql
```

**Mitigation needed:**
- Either ALIGN Java default to PER_HOUR (matching ADR), OR
- Update ADR-035 + api-contract.md to reflect COURSE_PACKAGE default if intentional (unlikely given §3 explicit "per VN market norm")
- Document RATIONALE for whichever value is canonical

#### P0-4 — PricingModel.java javadoc cites stale ADR-027 link (instead of ADR-035)

**Rule violated:** Cat 4 §4.2 cross-domain consistency + Session handoff §"ADR numbering chaos" lesson 3

**Evidence:**

`PricingModel.java:10`:
```java
 * @see <a href="../../../../../../../../../../../documents/02-architecture/adr/ADR-027-pricing-model-taxonomy.md">ADR-027</a>
```

Actual file: `ADR-035-pricing-model-taxonomy.md` — ADR-027 file là `ADR-027-statuspage-vendor.md` (orthogonal scope).

**Impact:**
- IDE click-through hyperlink → 404 OR (worse) lands on statuspage vendor doc
- Future readers will be confused (Wave br-4 session handoff §"Meta lesson 3 ADR numbering chaos" already flagged this; hotfix C-hotfix1 PR #1784 did NOT fix javadoc reference)

**Mitigation needed:** trivial 1-line edit `ADR-027` → `ADR-035` in PricingModel.java + grep for any other stale `ADR-027.*pricing` references repo-wide.

---

### 🟠 P1 — 9 findings

#### P1-1 — Cat 1.6 5-attribute coverage MISSING for 3 new domains
Mỗi BR-COURSE-PRICING-001..004 + BR-PAYMENT-METHOD-001..006 + reschedule rules đều thiếu Source / Rationale / Reviewer / Compliance / Cadence attributes (per `business-logic-review.md` §2). Cat 1 5-attr GAP-156 carry-forward, NOW WORSE — adding 10+ BRs without attributes.

#### P1-2 — Cat 1.3 Verification chain BROKEN for reschedule
api-contract.md line 100 cites `UC-CRS-11` but `course-class/use-cases.md` không có UC-CRS-11 entry (verified via grep). Verification chain: BR-? → UC-CRS-11 → endpoint → @Mapping → test — chain breaks at UC layer.

#### P1-3 — Cat 1.4 PaymentRecord uses code-only BR-IDs not in rules.md
Java code cites BR-PAYMENT-METHOD-005 (running total update) + BR-PAYMENT-METHOD-006 (recorded_by audit trail) — neither exist trong any rules.md. Java code = source of truth without canonical doc backing → orphan business logic per Cat 1.4.

#### P1-4 — Cat 1.5 Empty rules.md sections — course-class/rules.md §6 referenced but doesn't exist
ADR-035 §References + api-contract.md §Cross-references cite `course-class/rules.md §6` for pricing rules. File only has §1-§5. Either §6 was planned and forgotten, or reference is stale.

#### P1-5 — Cat 2.4 Per-tier defaults missing — pricingModel tier semantics
ADR-035 mentions PER_HOUR primary cho TT Anh ngữ, MONTHLY cho kindergarten — không có tier mapping doc. Solo persona vs Owner persona may have different defaults; unclear in current docs.

#### P1-6 — Cat 2.5 Override mechanism not documented
`application.yml` `kite.class.reschedule.notify.enabled=true` toggle (verified line 301) — no rules.md note env-var override path per `production-env-config-registry.md`.

#### P1-7 — Cat 4.3 Currency convention NOT explicitly cited
PricingCalculator works in VND. ADR-035 says NUMERIC(19,2) VND. api-contract.md uses `250000.00` decimal — no narrative attribute confirming `1.500.000đ` display format compliance per `.claude/rules/vn-localization-audit-checklist.md` §2 Section 1.

#### P1-8 — Cat 4.4 PDPL compliance gap — reschedule_reason_notes PII?
`reschedule_reason_notes` accepts ≤2000 chars free-text. Could contain PII (student name, parent info, medical reason). No PDPL compliance attribute in any rules.md — should reschedule_reason_notes be PII-classified per `documents/01-business/kitehub/consent/`?

#### P1-9 — Cat 5.2 Quarterly review cadence NOT set for new BRs
ADR-035 §Log lists single 2026-05-24 entry. Per `business-logic-review.md` §5.3 quarterly review trigger — no `Next review:` date set. 10+ new BRs added without quarterly review schedule.

---

### 🟡 P2 — 4 findings

#### P2-1 — Cat 3.4 Concurrent reschedule race condition test ABSENT
No test verifies 2 admins simultaneously call `/reschedule` on same class — `previous_start_date` snapshot may corrupt. `ClassServiceRescheduleTest` covers single-actor flow.

#### P2-2 — Cat 3.5 PricingModel time-sensitive test ABSENT
No test asserts pricing_model immutability across enrollment lifecycle window. BR-COURSE-PRICING-003 enforced via service-layer check — no integration test verifying behavior under concurrent enrollment + pricingModel update.

#### P2-3 — Cat 2.1 Config keys spot-check pass — `application.yml` reschedule keys verified present
✅ PASS — `kite.class.reschedule.notify.enabled` (line 301) exists. Counter-positive note để show audit thorough scan.

#### P2-4 — Cat 5.5 Sources cite "PM scan 3 TT pilot 2026-05-20" — informal evidence
ADR-035 §Alternatives §1 cites "PM scan trung tâm pilot KiteClass (2026-05-20, 3 trung tâm contacted)" — 3-sample is informal. Per `business-logic-review.md` §2.1 Source attribute, this counts as "informed gut + competitor benchmark" not "data-driven research". Per Cat 5.5 ≥80% non-"informed gut" target — Source quality borderline.

---

## 3. Per-BR 5-attribute table

| BR-ID | Source | Rationale | Reviewer | Compliance | Cadence | Verdict |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| **BR-COURSE-PRICING-001** (pricingModel enum required) | Informal (ADR §3) | ✅ (ADR-035 §3) | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 2/5 |
| **BR-COURSE-PRICING-002** (unit_price ≥ 0 + FREE = 0) | ❌ Missing | ⚠️ Implicit | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 1/5 |
| **BR-COURSE-PRICING-003** (model immutable post-enrollment) | ⚠️ Implicit (ADR §3) | ✅ (anti-bait-and-switch) | @nguyenvankiet | ⚠️ (Consumer Protection Law) | ❌ Missing | ❌ FAIL 2/5 |
| **BR-COURSE-PRICING-004** (cross-bucket reschedule recalc) | ❌ Missing | ⚠️ Implicit | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 1/5 |
| **BR-PAYMENT-METHOD-001** (enum values) | ⚠️ (wave plan) | ⚠️ (VN market) | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 1/5 + DRIFT |
| **BR-PAYMENT-METHOD-002** (amount > 0) | ❌ Missing | ✅ Implicit | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 1/5 |
| **BR-PAYMENT-METHOD-003** (tenant isolation OWASP A01) | ✅ (OWASP A01) | ✅ | @nguyenvankiet | ⚠️ (PDPL) | ❌ Missing | ⚠️ PARTIAL 3/5 |
| **BR-PAYMENT-METHOD-004** (idempotency key) | ⚠️ (RFC) | ✅ | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 2/5 |
| **BR-PAYMENT-METHOD-005** (running total) | ❌ Missing | ❌ Code-only | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 0/5 — ORPHAN |
| **BR-PAYMENT-METHOD-006** (recorded_by audit) | ❌ Missing | ❌ Code-only | @nguyenvankiet | ⚠️ (Audit trail) | ❌ Missing | ❌ FAIL 0/5 — ORPHAN |
| **BR-RESCHEDULE-?** (no canonical ID assigned) | ❌ Missing | ⚠️ Implicit | @nguyenvankiet | ❌ Missing | ❌ Missing | ❌ FAIL 0/5 — NO ID |

**Total 5-attribute compliance: 0 of 11 BRs satisfy ≥ 4/5 attributes.** Aggregate Source quality: 0% "data-driven research", 27% "competitor benchmark / OWASP", 73% "implicit or missing" — well below `business-logic-review.md` ≥80% non-"informed gut" target.

---

## 4. Per-category sub-check verdicts (rubric §2)

### Cat 1 — Rule Coverage (8/20)

| # | Check | Verdict | Evidence |
|---|---|:-:|---|
| 1.1 | Every BR-xxx in rules.md has ≥1 grep hit Java | ⚠️ MIXED — BRs cited in code but rules.md ABSENT | 11 BR-IDs in Java; 0 in rules.md cho 3 new domains |
| 1.2 | BR-xxx cited in code via comment/annotation | ✅ PASS | 11+ citations in PricingCalculator + PaymentRecordService |
| 1.3 | Verification chain doc-able BR→UC→endpoint→@Mapping→Test | ❌ FAIL P1 | UC-CRS-11 cited but `course-class/use-cases.md` has no UC-CRS-11 entry |
| 1.4 | No orphan code logic without matching BR-xxx | ❌ FAIL P1 | BR-PAYMENT-METHOD-005/006 exist in code only |
| 1.5 | Per-domain rules.md has ≥1 BR-xxx | ❌ FAIL P0 (P0-1) | 3 new domains have NO rules.md file |
| 1.6 | rules.md frontmatter 5-attribute coverage | ❌ FAIL P0 carry-forward | 0/11 new BRs satisfy 4/5 |

**Score:** 20 − 2×6 (P0) − 4×3 (P1 1.3+1.4+1.6×2) = 20−12−12 = 0, **capped at 8/20** (auto-cap 16 minus extra failures floor 8).

### Cat 2 — Config Accuracy (12/20)

| # | Check | Verdict | Evidence |
|---|---|:-:|---|
| 2.1 | Every config key cited in rules.md exists in application.yml | ✅ PASS | `kite.class.reschedule.notify.enabled` line 301 verified |
| 2.2 | Config key VALUES match rules.md documented values | ❌ FAIL P0 (P0-3) | Course.pricingModel Java default `COURSE_PACKAGE` ≠ ADR-035 + V67 default `PER_HOUR` |
| 2.3 | No drift renamed config keys | ❌ FAIL P1 (P0-4 + P1-4) | PricingModel.java cites `ADR-027` stale; ADR-035 cites `rules.md §6` doesn't exist |
| 2.4 | Per-tier defaults documented | ❌ FAIL P1 (P1-5) | No tier-mapping doc |
| 2.5 | Override mechanism documented | ⚠️ PARTIAL P1 (P1-6) | reschedule.notify.enabled toggle exists but not in rules.md (per `production-env-config-registry.md`) |

**Score:** 20 − 1×6 − 2×3 = 20−6−6 = 8 → floor up to 12/20 (capped 16 minus 4 P1 net).

### Cat 3 — Edge Case Tests (18/20)

| # | Check | Verdict | Evidence |
|---|---|:-:|---|
| 3.1 | Every UC error path has matching test | ✅ PASS | PaymentRecordServiceImplTest covers 4xx/409 paths; ClassControllerRescheduleIT covers 400/403/404/409 |
| 3.2 | Boundary tests cited limits | ✅ PASS | PricingCalculatorTest covers 4 enum × scenarios; reasonNotes 2000 chars verified |
| 3.3 | Negative tenant scenarios tested | ✅ PASS | PaymentRecord cross-tenant defense test exists |
| 3.4 | Concurrent-action tests | ❌ FAIL P2 (P2-1) | No race-condition test for reschedule |
| 3.5 | Time-sensitive tests | ❌ FAIL P2 (P2-2) | No pricingModel immutability concurrent test |

**Score:** 20 − 2×1 (P2) = 18/20.

### Cat 4 — Cross-Domain Consistency (10/20)

| # | Check | Verdict | Evidence |
|---|---|:-:|---|
| 4.1 | No rule in domain A contradicts rule in B | ❌ FAIL P0 (P0-2) | PaymentMethod enum 4-way drift VIETQR/VNPAY/ZALOPAY |
| 4.2 | Cascading rules consistent | ⚠️ PARTIAL | reschedule → PER_HOUR period recalc cited BR-COURSE-PRICING-004 trong code but không trong canonical rules.md |
| 4.3 | Currency/locale conventions consistent | ❌ FAIL P1 (P1-7) | No explicit VND `1.500.000đ` narrative compliance per `vn-localization-audit-checklist.md` |
| 4.4 | Compliance overlap (PDPL + Consumer Protection) reconciled | ❌ FAIL P1 (P1-8) | reschedule_reason_notes PII classification missing |
| 4.5 | Persona-scope respected | ✅ PASS | K-12 absent in PER_HOUR/MONTHLY discussion (target = TT Anh ngữ Phase 1) |

**Score:** 20 − 1×6 − 2×3 = 8 → floor up to 10/20 (capped 16 minus 6 P1 net).

### Cat 5 — Stakeholder Alignment (16/20)

| # | Check | Verdict | Evidence |
|---|---|:-:|---|
| 5.1 | Every rules.md has Reviewer field | ⚠️ PARTIAL | ADR-035 §Log has Reviewer @nguyenvankiet; rules.md absent so N/A |
| 5.2 | Quarterly review cadence on track | ❌ FAIL P1 (P1-9) | No `Next review:` date set for 10+ new BRs |
| 5.3 | Event-driven re-review triggers documented | ⚠️ PARTIAL | ADR-035 §Compliance lists Thông tư 78 — implicit trigger if law changes |
| 5.4 | Compliance-critical rules flagged for counsel | ⚠️ PARTIAL | Anti-bait-and-switch (Consumer Protection) flagged but not "counsel review" |
| 5.5 | Sources ≥80% non-"informed gut" | ❌ FAIL P2 (P2-4) | ADR-035 cites 3-TT pilot informal — 27% non-gut aggregate |

**Score:** 20 − 1×3 − 1×1 = 16/20.

---

## 5. Carry-forward Cat 1 status (Wave 98 baseline 73/100 → Wave br-4 64/100)

Wave 98 baseline:
- Cat 1: 13/20 (was 14/20 before adjusted by GAP-664 5-attr 60% coverage gap)
- Verdict: PARTIAL FAIL Cat 1 (carry-forward GAP-156 + GAP-664/666)
- Path to gate: GAP-664 + GAP-666 cluster ~3.25h = +3 pts → 76/100

**Wave br-4 status:**
- Cat 1: **8/20** ↓ −5 pts vs Wave 98 13/20 (NEW recurrence #3 of 3-layer absent class WITH 11 new BRs added without 5-attr)
- Phase 1 BETA gate ≥80 — **gap WIDENED −16 pts** (was −7 at Wave 98)
- GAP-156 5-attr backfill quarterly still in flight; Wave br-4 ADDED 10+ BRs to the backlog

**Cat 1 Rule Coverage carry-forward verdict:** 🔴 PARTIAL FAIL persists + GROWS — Wave br-4 shipped pricing/payment/reschedule WITHOUT 3-layer business docs (P0-1) + ORPHAN BR-IDs (P1-3) + STALE ADR cross-references (P0-4).

---

## 6. Phase 1 BETA gate verdict

**Phase 1 BETA gate ≥80/100** — 🔴 **MISS −16 pts** (current 64/100).

### Path to gate (estimated effort)

| Action | Effort | Score impact |
|---|:-:|:-:|
| **GAP-NEW-1** Create 3-layer docs cho course-pricing/ + payment-recording/ + reschedule/ (9 files) | ~6h | +8 pts (Cat 1 §1.5 + §1.6 partial) |
| **GAP-NEW-2** Consolidate PaymentMethod enum + reconcile VIETQR/VNPAY/ZALOPAY | ~3h | +6 pts (Cat 4 §4.1 + Cat 2 §2.3) |
| **GAP-NEW-3** Fix Course.pricingModel default + ADR-035 alignment | ~1h | +3 pts (Cat 2 §2.2) |
| **GAP-NEW-4** Update PricingModel.java javadoc ADR-027 → ADR-035 + grep stale refs | ~30min | +1 pt (Cat 2 §2.3) |
| **GAP-NEW-5** Add UC-CRS-11 entry to course-class/use-cases.md | ~1h | +2 pts (Cat 1 §1.3) |
| **GAP-NEW-6** PDPL classify reschedule_reason_notes + add Compliance attribute | ~2h | +2 pts (Cat 4 §4.4) |
| **GAP-NEW-7** VND `1.500.000đ` narrative compliance audit per `vn-localization-audit-checklist.md` | ~2h | +1 pt (Cat 4 §4.3) |
| GAP-664/666 existing path (Wave 98 carry-forward) | ~3.25h | +3 pts (Cat 1 carry) |
| **Total path 64 → 90** | **~19h** | **+26 pts** |

### Pre-merge follow-up gaps to file

| GAP | Priority | Trigger | Wave |
|---|:-:|---|:-:|
| GAP-NEW-business-logic-3-layer-wave-br-4 | P0 | Recurrence #3 of 3-layer absent | br-5 mandatory |
| GAP-NEW-payment-method-enum-consolidation | P0 | 4-way drift VIETQR/VNPAY/ZALOPAY | br-5 mandatory |
| GAP-NEW-course-pricing-model-default-alignment | P0 | Java default ≠ ADR default | br-5 mandatory |
| GAP-NEW-adr-027-stale-reference-cleanup | P1 | PricingModel javadoc + repo-wide grep | br-5 nice-to-have |
| GAP-NEW-check-3-layer-detector-investigate | P0 META | CI exists but didn't block | br-5 META |
| GAP-NEW-reschedule-pdpl-classification | P1 | reschedule_reason_notes PII analysis | br-5 |

### Audit-level verdict

🔴 **AUDIT FAIL** — 4 P0 sub-checks failed across Cat 1 + Cat 2 + Cat 4. Per `audit-skill-rubric-business-logic-audit.md` §4: ANY P0 FAIL → audit-level verdict = FAIL.

🟡 **NOT BLOCKING** Phase 1 BETA gate alone — `post-wave-audit-mandate.md` allows AUDIT_OVERRIDE trailer with follow-up gap. Path to 80 requires shipping the 7 GAP-NEW gaps above in Wave br-5 (estimated ~19h work).

**Recommendation:** File 6 follow-up gaps + queue Wave br-5 Bucket "business-doc backfill" + flip GAP-664/666 path.

---

## 7. References

- Rubric: `.claude/rules/audit-skill-rubric-business-logic-audit.md` v1.0.1
- SKILL: `.claude/skills/quality/business-logic-audit/SKILL.md`
- Living Docs: `documents/01-business/README.md` §2 + CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
- 5-attribute standard: `.claude/rules/business-logic-review.md` §2
- Baseline: `documents/04-quality/audits/business-logic/2026-05-19-wave-98-new-domains.md` (73/100 C+)
- ADR audited: `documents/02-architecture/adr/ADR-035-pricing-model-taxonomy.md`
- Session handoff: `documents/03-planning/session-handoffs/2026-05-24-wave-beta-readiness-4-closure.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-24-beta-readiness-4-meta-pdpl-pricing-reschedule-tone.md`
- Code refs:
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/entity/Course.java:172`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/entity/PricingModel.java:10` (stale ADR-027 ref)
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/service/PricingCalculator.java:22-23`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/record/service/PaymentRecordService.java:34-37`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/constant/PaymentMethod.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/payment/enums/PaymentMethod.java`
- Docs referenced:
  - `documents/01-business/kiteclass/course-class/rules.md` (no §6 — referenced but absent)
  - `documents/01-business/kiteclass/course-class/api-contract.md` §Pricing + §reschedule (Bucket C+D content lives here, not in dedicated domains)
  - `documents/01-business/kiteclass/payment-invoice/rules.md` line 38 (alternative source for PaymentMethod values)

---

## 8. Audit metadata

- **Auditor:** Claude Opus 4.7 (1M context) — read-only audit per Wave audit-1 Bucket B
- **Date run:** 2026-05-25
- **Effort:** ~45min audit (Opus 1M retry after Sonnet 200k thrash per session handoff §"Meta lesson 1")
- **Cadence compliance:** ✅ T+1 from Wave br-4 last merge 2026-05-24 (deadline T+3 = 2026-05-27, met với buffer ≥2 days)
- **Follow-up audit:** Recommended Wave br-5 closure refresh after 6 GAP-NEW fixes ship — expect 64 → 90 trajectory
