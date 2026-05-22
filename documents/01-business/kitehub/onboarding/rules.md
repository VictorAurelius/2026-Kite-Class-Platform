# Onboarding Progress — Business Rules

**Domain:** Tenant onboarding checklist tracking (Wave 78 — GAP-538; Wave 105 Bucket B — Owner persona reorder)
**Last verified:** 2026-05-22 (Wave 105 Bucket B Owner walk)
**Config prefix:** `kitehub.onboarding`

File này document business values cho onboarding checklist flow. Mỗi rule có 5 attributes theo `.claude/rules/business-logic-review.md` §2.

> **Bucket 0 stub status:** rules ở dưới là stub form (≥1 đầy đủ 5 attributes + ≥1 placeholder). Bucket B (GAP-538) sẽ enrich thêm theo final implementation.

---

## BR-ONBOARD-001 — Onboarding checklist là per-tenant, persist BE-side

- **Value:** Mỗi tenant có MỘT row `onboarding_progress` (1:1 với `tenant_id`). State persist trong DB Postgres (table `onboarding_progress`), KHÔNG dùng localStorage. Cross-device sync mặc định.
- **Source:** Wave 78 outside-in audit (per `outside-in-coverage-trigger.md` Wave 73) + decision Q3 trong wave plan §1 Brainstorm — "localStorage vs BE table → chọn BE table cho cross-device persistence + admin tracking".
- **Rationale:** Beta tenant (P1 solo teacher, P2 center owner) thường dùng nhiều thiết bị (laptop tại trường + điện thoại di chuyển). LocalStorage không sync → user confused tại sao checklist "reset" khi đổi browser. BE persistence cũng cho phép platform admin track completion % across cohort (analytics future scope Wave 79+).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14). Formal stakeholder review queued post-Phase-1 BETA.
- **Compliance check:** **Considered** — onboarding state KHÔNG phải PII per PDPL 2023 Art 2(3) (chỉ chứa step completion flags, không có name/email/ID). Tuy nhiên `tenant_id` link tới tenant data → retention rule kế thừa retention chung của tenant (per BR-TENANT-DATA-RETENTION).
- **Review cadence:** Quarterly. **Next review:** 2026-08-14. Event triggers: cross-device sync complaints, ≥10% drop-off tại bước IMPORT_DATA.
- **Code reference:** (planned) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/onboarding/entity/OnboardingProgress.java` (Bucket B — Wave 78 GAP-538).

## BR-ONBOARD-002 — IMPORT_DATA dual-mode (real bulk-import + sample seed)

- **Value:** Step `IMPORT_DATA` hỗ trợ HAI path để mark complete (Wave 105 Bucket B persona walk reframe):
  - **Path A — Owner real bulk-import (chị Hằng persona):** user upload xlsx qua `POST /api/v1/students/bulk-import/commit` (KiteClass core — per `BulkImportController` GAP-051). Step auto-marks complete khi job status = `COMPLETED` với ≥1 row imported. Cap 200/batch, async job.
  - **Path B — Sample/demo seed (Solo persona):** user explicit opt-in (Radix Dialog confirmation) qua FE checkbox + button "Bật dữ liệu mẫu". Server-side validate `tenant.metadata.is_beta_demo_data=true` trước khi emit `onboarding.demo-data.requested` event.
- **Source:** Wave 78 plan §3 Risks (sample seed leak) + Wave 105 Bucket B outside-in persona walk (per `documents/04-quality/audits/persona-review/2026-05-22-wave-105-bucket-b-owner-walk.md`) — Hằng 160 học viên sẵn có sẽ KHÔNG dùng demo seed; cần real bulk-import path.
- **Rationale:** Original Wave 78 design assumed Solo/curious persona dominant — `IMPORT_DATA` chỉ là sample-seed opt-in. Wave 105 outside-in walk Bucket B FAIL surfaced rằng P2 Center Owner (Hằng) sẽ skip step "tuỳ chọn" + đi thẳng `kiteclass/students/bulk-import` (chậm hơn 5-10 phút find UI). Dual-mode keep both personas covered + bulk-import-first ordering satisfies Owner business reality. Demo seed gating (Path B `is_beta_demo_data` flag) giữ data hygiene cho Solo.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Data hygiene scout, solo-dev, 2026-05-22 Wave 105 Bucket B reframe).
- **Compliance check:** N/A — Path A xlsx upload tenant's own student data namespace (đã có RLS isolation per `multi-tenant-architecture.md`); Path B demo seed cùng tenant namespace; không touch PII của bên thứ ba.
- **Review cadence:** Quarterly. **Next review:** 2026-08-22. Event triggers: ≥10% Owner-persona skip rate observed, KC bulk-import endpoint schema change, demo seed cohort policy change.
- **Code reference:**
  - Path A: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/controller/BulkImportController.java` (GAP-051 — DONE Wave 71)
  - Path B: (planned) Bucket B Sample-data seeder + `is_beta_demo_data` flag check — Wave 78 unfinished Bucket B sub-scope.
  - FE: `kitehub/kitehub-frontend/src/components/onboarding-checklist/OnboardingChecklist.tsx` (Wave 105 — 2 CTA buttons cần update)

## BR-ONBOARD-003 — Completion 100% triggers retention pipeline

- **Value:** Khi tenant đạt `completionPercent=100%` lần đầu, BE emit `onboarding.completed` event qua outbox. Retention pipeline subscribe event → schedule email day-7 survey + day-14 retention check-in (Bucket E + Bucket F scope).
- **Source:** Wave 78 outside-in audit Tier 1 — beta tenant retention insight cần survey trigger predictable.
- **Rationale:** *(placeholder — Bucket B sẽ enrich với data từ analytics khi available)*
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-14). Formal review post-Wave-78 close.
- **Compliance check:** N/A — event metadata KHÔNG chứa PII.
- **Review cadence:** Quarterly. **Next review:** 2026-08-14.
- **Code reference:** (planned) Bucket B outbox emit + Bucket F event subscriber.

---

## Config

Tracking `@Value` wiring status per GAP-555 (Wave 78 Business Logic audit P0).

| Key | Default | Purpose | Wired |
|-----|---------|---------|:-----:|
| `kitehub.onboarding.step-ids` | `PROFILE_SETUP,INVITE_TEAM,IMPORT_DATA,CREATE_FIRST_CLASS,EXPLORE_FEATURES` | Hardcoded enum whitelist; FE render dynamic | 🆕 Wave 79 Bucket A target |
| `kitehub.onboarding.put-rate-limit-per-min` | `60` | Per-tenant PUT rate limit tại gateway | 🆕 Wave 79 Bucket A target |

**Wave 79 Bucket A scope (GAP-555):** Add `@Value` injection cho 2 keys above ở module `kitehub-subscription/onboarding/service`.

Config keys nằm trong `application.yml` BE module (Bucket B).
