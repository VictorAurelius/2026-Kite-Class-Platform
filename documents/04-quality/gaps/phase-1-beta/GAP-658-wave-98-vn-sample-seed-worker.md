# GAP-658: VN sample seed worker — replace English placeholder data with Vietnamese-friendly content

**Status:** 🟡 PARTIAL (90%)
**Priority:** 🔴 P0
**Domain:** Backend (seed-worker + sample-data service)
**Detected:** 2026-05-18 (Wave 98 prep — outside-in audit persona walkthrough P2 Hằng + failure-mode M-NEW-15)
**Parent audit:** `documents/04-quality/audits/persona-review/2026-05-18-wave-98-cluster-b-beta-cohort-outside-in.md` F-NEW-3 + failure-mode matrix M-NEW-15 (native VN copywriter pass)

## Problem

GAP-538 PARTIAL 85% (Day-1 onboarding checklist + sample/demo data seed) explicit defer Vietnamese-friendly seed worker. Persona walkthrough P2 Center Owner (chị Hằng, 45, Q.1 TP.HCM English center) revealed:

| First-touch element | Current state | Trust impact |
|---|---|---|
| Sample student names | `John Doe`, `Jane Smith` | ❌ Hằng nghĩ "đây là phần mềm Mỹ, không phù hợp VN" |
| Sample class names | `Class A1`, `Class B2` | ❌ Không khớp VN edu convention (`Lớp 5A1`, `Lớp Anh ngữ 9B`) |
| Sample center name | `Example Center`, `Demo School` | ❌ Generic placeholder |
| Sample currency | `$60.00`, `60 USD` | ❌ VN phải VND `1.500.000đ` |
| Sample dates | `Mon May 14, 2026` | ❌ VN convention `Thứ Hai, 14/05/2026` |
| Sample address | `123 Main St, Anytown` | ❌ Không VN format |

Per `user-manual-content-standard.md` §2 row 7 (VN sample data mandate) + `dev-readable-doc-language.md` §2 (VN narrative). Sample data là first-touch trust signal — wrong tone từ giây đầu = bounce.

## Root Cause

Seed-worker service ship MVP với English Lorem-Ipsum-style placeholders. Không có:
- VN-friendly name pool (300+ Vietnamese names regions Bắc/Trung/Nam)
- VN edu class naming convention generator (lớp + grade-level + section)
- VND currency formatter inline
- VN date/time/address formatters

## Proposed Fix

### Step 1: VN data pool

`kitehub/kitehub-platform/src/main/resources/seed-data/vn-friendly/`:
- `student-names.csv` — 300 rows (full name + diacritics) regional balance
- `teacher-names.csv` — 100 rows
- `center-names.csv` — 50 rows (educational center patterns: "Trung tâm Anh ngữ Sky Education", "Trung tâm Toán Quang Minh")
- `class-names.csv` — 50 rows (`Lớp Anh ngữ 5A1`, `Lớp Toán 9B`)
- `addresses.csv` — 100 rows (HCM + Hà Nội + Đà Nẵng districts)
- `subject-names.csv` — 30 rows (Anh ngữ, Toán, Lý, Hóa, Văn, Sử, Địa, Tin học...)

Source: native VN copywriter pass (P0 deliverable Wave 98 paired GAP-659 staff-invite/persona-tone email).

### Step 2: VN data generator service

`kitehub/kitehub-platform/.../VietnamSampleDataGenerator.java`:
- `generateStudent()` → random row từ student-names + random class enrollment
- `generateClass()` → random class-name + 15-30 students + teacher
- `generateCenter()` → random center-name + 1-3 classes + 1 owner
- `formatVND(BigDecimal)` → `1.500.000đ`
- `formatVNDate(LocalDate)` → `Thứ Hai, 14/05/2026` (using `Locale("vi", "VN")`)

### Step 3: Replace English placeholders trong seed-worker

`kitehub-platform/.../SeedWorkerService.java`:
- Remove all `John Doe` / `Class A1` / `Example Center` / `$60.00` constants
- Inject `VietnamSampleDataGenerator` for default tenant seeding
- Add `seed.locale` config (default `vi-VN`; allow `en-US` cho test fixtures)

### Step 4: Onboarding checklist sample-data integration

`kitehub-platform/.../OnboardingChecklistService.java`:
- Day-1 checklist item "Tạo lớp đầu tiên" → pre-fills sample class với VN naming
- "Thêm học sinh đầu tiên" → pre-fills sample student với VN name
- User edit (acceptance: minimal edits required → trust signal)

### Step 5: GAP-538 sync

After this gap DONE → GAP-538 §AC update:
- AC7 VN sample seed ✅ (Steps 1-4)
- GAP-538 PARTIAL 85 → 95%

## Acceptance Criteria

- [x] 6 VN data CSV files trong `seed-data/vn-friendly/` (≥300 students, 100 teachers, 50 centers, 50 classes, 100 addresses, 30 subjects) — Wave 98 B2
- [x] `VietnamSampleDataGenerator` service implement + unit tests — Wave 98 B2 (15 tests PASS)
- [x] Replace English placeholders trong seed data — verified Wave beta-readiness-9 Bucket D: ZERO `John Doe`/`Jane Smith`/`Class A1`/`Class B2`/`Example Center`/`Demo School` literals trong default-locale (`vi-VN`) production path across kiteclass-core (`BrandingDataSeeder` = VN: "Trung tâm Anh ngữ Sky Education", "cô Khánh") + kitehub-platform (VN CSVs). `SeedWorkerService` không tồn tại trong kiteclass-core/kitehub-platform — domain-only modules; integration tracked future scope khi service materializes (per Wave 98 B2 DEFERRED note)
- [ ] `OnboardingChecklistService` pre-fill VN sample data — service không tồn tại; defer paired-Bucket B4 i18n
- [ ] Native VN copywriter review pass (P0 — pair với GAP-659) — paired Wave 98 Bucket B4 i18n
- [x] BE test PASS — `BrandingDataSeederTest` 4/4 (kiteclass-core, strict-warnings) + `VietnamSampleDataGeneratorTest` 15/15 (kitehub-platform), BUILD SUCCESS
- [x] GAP-538 PARTIAL 85 → 90% updated — Wave 98 B2

## Effort estimate

~1 wave bucket + 0.5 day native VN copywriter review (paired GAP-659). Parallel-safe.

## Related

- **Parent audits:** outside-in persona F-NEW-3 + failure-mode M-NEW-15
- **Sister gap:** GAP-538 PARTIAL 90% (onboarding checklist) — Wave 98 B2 ship closes AC7 seed-data portion (85→90%)
- **Pair:** GAP-659 (staff-invite email + persona-tone) — shared native VN copywriter pass
- **Standards referenced:** `user-manual-content-standard.md` §2 row 7 VN sample data; `dev-readable-doc-language.md` §2
- **Wave 98 bucket:** B2 (extends GAP-538)

---

## Log

- **2026-05-18 (PARTIAL 80%) — Wave 98 Bucket B2:** Foundation shipped via wave/98-b2-vn-sample-seed branch:
  - ✅ 6 CSV files trong `kitehub/kitehub-platform/src/main/resources/seed-data/vn-friendly/` với UTF-8 BOM: student-names.csv (300 rows, 295 unique full names), teacher-names.csv (100 rows + specialty), center-names.csv (50 rows), class-names.csv (50 rows), addresses.csv (104 rows across 8 VN cities), subject-names.csv (30 rows VN name + EN abbreviation)
  - ✅ `VietnamSampleDataGenerator` Spring `@Component` (`com.kitehub.platform.seed`) — 7 generator methods + formatVND/formatVNDate/formatVNTime + 6 DTO records (SampleStudent/Teacher/Center/Class/Address/Subject) + locale fallback `seed.locale=en-US`
  - ✅ 15 unit tests PASS (`VietnamSampleDataGeneratorTest`) — CSV load, diversity (≥80 unique trên 200 calls), Vietnamese diacritics presence, VND format, VN date day-of-week, 24h time format, English fallback
  - ✅ `documents/01-business/kitehub/seed/{rules,use-cases,api-contract}.md` 3-layer business doc — 10 BR rules + 3 use cases + Java API contract
  - ✅ `cd kitehub && ./mvnw -pl kitehub-platform verify -P strict-warnings` PASS (39 tests total, 0 failures)
  - ✅ Generator Python scripts `scripts/seed-data/generate-vn-{student,teacher}-names.py` (deterministic seed=20260518 cho reproducibility)
  - **DEFERRED:**
    - SeedWorkerService refactor — service does NOT exist trong kitehub-platform (domain-only module); zero English placeholder constants currently grep-able. Integration tracked future scope khi SeedWorker service materializes in kitehub-platform OR migrates từ kitehub-subscription
    - OnboardingChecklistService pre-fill integration — service does NOT exist trong kitehub-platform; defer paired-Bucket follow-up
    - Native VN copywriter review pass — paired Wave 98 Bucket B4 i18n (parallel execution; pre-merge integration)
    - `seed.locale` config key trong `application.yml` — kitehub-platform là shared module, no application.yml; consumer modules add key per BR-SEED-001
  - **AC7 GAP-538 closed via shipped foundation:** GAP-538 progress 85% → 90%

- **2026-05-18 (PR #1550 merged)** — Post-merge sync per `post-merge-sync-completeness.md` §4. Foundation shipped + 3-layer business doc `documents/01-business/kitehub/seed/{rules,use-cases,api-contract}.md` paired bonus. SeedWorkerService + OnboardingChecklistService wiring deferred to consumer module when service materializes; native VN copywriter pass paired Wave 98 Bucket B4 (PR #1549).

- **2026-06-01 (PARTIAL 80% → 90%) — Wave beta-readiness-9 Bucket D:** State-check (per `audit-to-gap-pipeline.md` §2.8) anchored kiteclass-core `BrandingDataSeeder.java`. Scope = REMAINING English placeholders only (foundation already shipped Wave 98 B2). Findings:
  - **kiteclass-core (anchor):** `BrandingDataSeeder.java` = only seeder; ALREADY fully Vietnamese (Sky Education tenant = "Trung tâm Anh ngữ Sky Education", tagline "Chắp cánh tương lai Anh ngữ", VN teacher "cô Khánh", VN hero slogans). V16 seed migration = no-op (references tables not in `kiteclass_shared`); V19/V20 landing migrations clean. Grep-zero `John Doe`/`Jane Smith`/`Class A1`/`Class B2`/`Example Center`/`Demo School` across `kiteclass-core/src/main` (java + resources).
  - **kitehub-platform:** `VietnamSampleDataGenerator` default `seed.locale=vi-VN` → pulls from 6 VN CSVs. The 4 English literals (`Example Center` L144 / `Class A1` L157 / `123 Main St` L171 / `Teacher Sample` L131) are the **intentional `en-US` test-fixture fallback** per BR-SEED-001 — asserted by `VietnamSampleDataGeneratorTest:185-188`. NOT remaining-placeholders-to-fix; replacing them would break the documented EN fallback contract. NO fabricated work done (per state-check discipline + task instruction "don't fabricate work").
  - **Seed scripts:** `scripts/seed-*.sh` clean; single "John Doe" hit = comment explicitly forbidding English placeholders.
  - **BE test:** `cd kiteclass/kiteclass-core && ./mvnw test -Dtest='*Seed*,*SampleData*,*BrandingDataSeeder*' -P strict-warnings` → `BrandingDataSeederTest` 4/4, BUILD SUCCESS. Cross-verify `VietnamSampleDataGeneratorTest` 15/15 (kitehub-platform), BUILD SUCCESS.
  - **Verdict:** code AC "replace English placeholder sample data" = DONE (zero remaining in default-locale production path). Stays PARTIAL (90%) per `gap-done-discipline.md` §1 — 2 AC genuinely open: (a) `OnboardingChecklistService` pre-fill (service không tồn tại, defer Bucket B4 i18n); (b) native VN copywriter review pass (paired GAP-659/Bucket B4). Live walkthrough AWS-gated → `FEATURE_SHIP_WALK_DEFER` (GAP-612 AWS suspension). No code change shipped this bucket — verification-only confirming work already covered.
