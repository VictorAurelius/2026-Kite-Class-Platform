---
audience: dev
domain: seed
layer: 2-use-cases
last-updated: 2026-05-18
version: 1.0
---

# Seed Worker — Layer 2: Use Cases

**Related rules:** [`rules.md`](./rules.md) BR-SEED-001..BR-SEED-010
**Wave/Gap:** GAP-658 Wave 98 Bucket B2

---

## UC-SEED-001 — Default tenant first-touch seed

**Actor:** Seed worker service (background process during tenant provisioning)
**Trigger:** New KiteHub instance provisioned; OnboardingChecklistService request "seed sample data" cho default tenant
**Pre-conditions:**
- Tenant created (P2 Center Owner, vd chị Hằng)
- `seed.locale=vi-VN` (default per BR-SEED-001)
- `VietnamSampleDataGenerator` bean injected

**Main flow:**
1. Seed worker request `generateCenter()` → tạo Trung tâm sample (BR-SEED-005)
2. Loop 3-5 lần: request `generateClass()` → tạo classes mẫu (BR-SEED-004)
3. Loop 15-30 lần per class: request `generateStudent()` → tạo students mẫu (BR-SEED-002)
4. Loop 5-10 lần: request `generateTeacher()` → tạo teachers mẫu (BR-SEED-003)
5. Pre-fill addresses từ `generateAddress()` (BR-SEED-006)
6. Pre-fill subjects từ `generateSubject()` (BR-SEED-007)
7. Persist entities với fields formatted theo BR-SEED-008/009/010 (VND currency, VN date, 24h time)

**Post-condition:** Tenant dashboard hiển thị sample data Vietnamese-friendly. Chị Hằng login → thấy "Lớp Anh ngữ 5A1", "Trần Thị Hồng", "Trung tâm Sky Education", "1.500.000đ", "Thứ Hai, 14/05/2026" → trust signal đúng.

**Errors:**
- CSV resource missing → `IllegalStateException` at startup (fail-fast per BR-SEED-001)
- `seed.locale` invalid → fall back default `vi-VN` (logged WARN)

---

## UC-SEED-002 — Test fixture với English placeholders

**Actor:** Integration test khởi chạy với English locale
**Trigger:** Test fixture set `seed.locale=en-US`

**Main flow:**
1. Test class set property `seed.locale=en-US`
2. `VietnamSampleDataGenerator.generateStudent()` returns `SampleStudent("Student Sample", "U", "N/A")`
3. Test asserts English placeholder presence (cross-locale coverage)

**Post-condition:** Test isolation từ VN sample pool (deterministic placeholder values).

---

## UC-SEED-003 — Onboarding checklist pre-fill sample (FUTURE — defer)

**Actor:** OnboardingChecklistService (chưa tồn tại trong kitehub-platform; future scope)
**Trigger:** P2 Center Owner click "Tạo lớp đầu tiên" hoặc "Thêm học sinh đầu tiên" trong onboarding checklist

**Main flow:**
1. Service call `VietnamSampleDataGenerator.generateClass()` → pre-fill class name + grade + subject
2. User edit (minimal edits required → trust signal)
3. Persist sau khi user confirm

**Status:** Defer post-GAP-658 ship. Tracked separately khi OnboardingChecklistService lands trong kitehub-platform OR kitehub-subscription module.

---

## FE behavior notes

- Sample data hiển thị TRỰC TIẾP trong dashboard (KHÔNG masking placeholder UI)
- Date format `formatVNDate()` apply tại render time (server-side render hoặc FE i18n)
- Currency format `formatVND()` apply tại render time

---

## Log

- **2026-05-18 (v1.0):** Use cases tạo Wave 98 Bucket B2 per GAP-658. UC-SEED-003 deferred — OnboardingChecklistService chưa tồn tại trong kitehub-platform module.
