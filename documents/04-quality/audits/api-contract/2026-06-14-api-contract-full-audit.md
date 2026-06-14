---
audience: mixed
---

# API-Contract Full Audit — post wave-p0-closeout-1 (2026-06-14)

**Audit ID:** AUDIT-2026-06-14-api-contract-full
**Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md` (/100, 5 categories, per-check pass/fail per `audit-skill-rubric-api-contract-audit.md`)
**Scope:** Toàn monorepo — 57 controller kiteclass-core + ~50 controller kitehub (6 service) ↔ 76 `documents/01-business/**/api-contract.md`. Focus surface gần đây: SSO (`/api/v1/auth/sso/**`), subscription lifecycle, LMS, reports, StorageController.
**Method:** Trích `@*Mapping` từ controller ↔ đối chiếu api-contract.md (path/method/request/response/error-codes); chạy 2 detector tĩnh `check-fe-be-api-contract.sh` + `check-be-fe-url-contract.sh`; kiểm RFC 7807 error envelope; kiểm versioning URL.
**Run date:** 2026-06-14 · **Auditor:** Claude (Opus 4.8) · **Branch:** `chore/audit-api-2026-06-14` (off main `cd44e035f`)

---

## SCORE: 80/100 — VERDICT: 🔴 FAIL (2 P0 sub-check FAIL)

> Per `audit-skill-rubric-api-contract-audit.md` §3 primacy: bug list là deliverable; điểm chỉ descriptive. Audit-level verdict = FAIL vì ≥1 P0 sub-check FAIL (Cat 1.1 endpoint undocumented + Cat 4.1 unversioned URL).

| # | Category (20pts) | Score | P0 FAIL? |
|---|------------------|:-----:|:--------:|
| 1 | Endpoint Coverage | 14/20 | ✅ FAIL (1.1) |
| 2 | Request/Response Schema Match | 18/20 | — |
| 3 | Error Code Consistency | 17/20 | — |
| 4 | Versioning & Deprecation | 14/20 | ✅ FAIL (4.1) |
| 5 | Integration Test Coverage | 17/20 | — |
| | **TOTAL** | **80/100** | **FAIL** |

So với Wave 40 baseline `72/100 C+ (36 undocumented)` → +8 (đa số domain Phase 1 BETA giờ đã có api-contract.md đầy đủ; drift còn lại tập trung ở recent surface SSO + cross-service inconsistency).

---

## 1. BUG LIST (deliverable — file:line evidence + severity)

### 🔴 P0 — block v1.0.0-rc

| # | Finding | Evidence | Gap |
|---|---------|----------|-----|
| B1 | **SSO endpoint cluster UNDOCUMENTED** — `POST /api/v1/auth/sso/issue-code` + `POST /api/v1/auth/sso/exchange` không có api-contract.md (không tồn tại domain folder `sso/`). Đây là recent surface (GAP-1305 SSO owner-seed walk). | `SsoController.java:57,81,129`; `documents/01-business/kitehub/sso/` KHÔNG tồn tại; `grep auth/sso documents/01-business` = 0 hit api-contract | GAP-1332 |

### 🟠 P1

| # | Finding | Evidence | Gap |
|---|---------|----------|-----|
| B2 | **ImpersonationController 3 endpoint chưa có api-contract.md** — `POST /api/v1/admin/impersonate/{tenantSlug}`, `POST /end`, `GET /audit-log`. admin-audit/api-contract.md chỉ nhắc `IMPERSONATE` như audit action-type, không document endpoint (path/method/request/response). | `ImpersonationController.java:49,64,83,92`; admin-audit/api-contract.md:173 chỉ là audit row | GAP-1333 |
| B3 | **FE→BE drift: `GET /api/v1/instance/config` không có BE mapping** — FeatureDetection hook query endpoint này nhưng kiteclass InstanceController base = `/api/v1/instances` (số nhiều, không có `/config`) → 404 → feature-gating fallback im lặng. | FE `useFeatureDetection.ts:17`; BE `InstanceController.java:40` (`/api/v1/instances`) — không có `/instance/config` | GAP-1334 |
| B4 | **FE→BE drift: kiteclass forgot/reset-password sai path** — FE gọi `POST /api/auth/forgot-password` + `/api/auth/reset-password` nhưng BE PasswordResetController expose `/api/auth/password-reset-request` + `/api/auth/password-reset-confirm` → 404. | FE `kiteclass-frontend/src/lib/api/auth.ts:117,124`; BE `PasswordResetController.java:44,52,62` | GAP-1335 |
| B5 | **Cross-service error envelope inconsistency** — kitehub-subscription dùng RFC 7807 `ProblemDetail` (application/problem+json); kiteclass-core dùng custom `ErrorResponse` DTO. 2 shape khác nhau giữa 2 service chính; api-contract.md không mô tả thống nhất envelope nào áp dụng đâu (Cat 3.3). | kitehub `GlobalExceptionHandler.java:9,44` (`ProblemDetail`); kiteclass `GlobalExceptionHandler.java:3,48` (`ErrorResponse`) | GAP-1337 |

### 🟡 P2

| # | Finding | Evidence | Gap |
|---|---------|----------|-----|
| B6 | **FE→BE method drift: generate-theme GET vs POST** — FE `use-theme-generation.ts` gọi `GET /api/platform/branding/ai/generate-theme`; BE chỉ expose `@PostMapping` → 405. | FE `use-theme-generation.ts:26` (GET); BE `AIBrandingController.java:46,185` (`@PostMapping`) | GAP-1336 |
| B7 | **Versioning inconsistency kitehub vs kiteclass** — kiteclass theo `/api/v1/**`; kitehub theo `/api/platform/**` + `/api/auth/**` (unversioned). Rubric 4.1 yêu cầu 0 endpoint ngoài `/api/v[0-9]+/`. Không có versioning-policy doc bắc cầu 2 convention; hệ quả: consumer (mobile/3rd-party) không có quy ước version nhất quán. (Intentional namespace nhưng undocumented as policy.) | `grep '@RequestMapping("/api/platform' kitehub/` = nhiều; `grep '/api/auth' PasswordResetController.java:44`; kiteclass dùng `/api/v1/` | GAP-1338 |

**Tổng: 7 finding net-new (1 P0 + 4 P1 + 2 P2).** Tất cả dedup-checked vs gap hiện có — không trùng.

### False-positive đã loại (không phải finding)
- `POST /api/auth/2fa/verify` (detector flag) → BE TwoFactorController expose CẢ `/api/v1/auth/2fa/verify` LẪN `/api/auth/2fa/verify` qua array-form `@PostMapping` (`TwoFactorController.java:128`). Detector không resolve array path → FP. SSO `check-be-fe-url-contract.sh` PASS 4/4.

---

## 2. Per-category per-check verdict

### Cat 1 — Endpoint Coverage = 14/20 (1.1 P0 FAIL → cap)
| Check | Verdict | Note |
|---|---|---|
| 1.1 Mọi `@*Mapping` documented | ❌ FAIL P0 | SSO (B1) + Impersonation (B2) undocumented |
| 1.2 No docs-orphan | ✅ PASS (sampled) | Focus area (storage/lms/reports/subscription) không orphan; full-set ❓ UNCHECKED |
| 1.3 Public endpoint tách section | ✅ PASS | PublicBranding/PublicConfig/PublicTenant có doc riêng |
| 1.4 Gateway-proxied routes mapped | ⚠️ PARTIAL | đa số doc nêu base path; proxy chain note rải rác |
| 1.5 Non-REST (SSE) documented | ✅ PASS | DeployStreamController SSE trong branding-wizard doc |
| 1.6 Webhook receiver documented | ✅ PASS | PaymentWebhook + MigrationWebhook trong subscription-billing/trial-migration |

### Cat 2 — Request/Response Schema Match = 18/20 (sampled, no FAIL)
| Check | Verdict | Note |
|---|---|---|
| 2.1 Request DTO fields | ✅ PASS | sampled storage/lms/reports/subscription field-match |
| 2.2 Response DTO fields | ✅ PASS | sampled |
| 2.3 Field types | ✅ PASS | sampled |
| 2.4 Required vs optional | ❓ UNCHECKED | không default PASS — chưa sample @NotNull vs docs |
| 2.5 Nested objects typed | ✅ PASS | sampled |
| 2.6 Enums khớp Java const | ✅ PASS | PaymentMethod canonical (GAP-739 đã resolve) |

### Cat 3 — Error Code Consistency = 17/20 (3.3 P1 FAIL)
| Check | Verdict | Note |
|---|---|---|
| 3.1 HTTP status match | ✅ PASS | sampled |
| 3.2 App error codes per-endpoint | ⚠️ PARTIAL | subscription/reports/storage có; nhiều domain thiếu Error Codes section |
| 3.3 Error body schema documented | ❌ FAIL P1 | B5 — 2 envelope khác nhau (ProblemDetail vs ErrorResponse), docs không thống nhất |
| 3.4 Validation 400 field-level | ✅ PASS | cả 2 handler có field errors |
| 3.5 Rate-limit 429 documented | ❓ UNCHECKED | chưa cross-ref rate-limit table |

### Cat 4 — Versioning & Deprecation = 14/20 (4.1 P0 FAIL → cap)
| Check | Verdict | Note |
|---|---|---|
| 4.1 Mọi endpoint dưới `/api/v[0-9]+/` | ❌ FAIL P0 | B7 — kitehub `/api/platform/**` + `/api/auth/**` unversioned (intentional namespace nhưng vi phạm rubric literal) |
| 4.2 No breaking change MINOR | ❓ UNCHECKED | không có oasdiff baseline |
| 4.3 Deprecated marked | ✅ PASS | không thấy `@Deprecated` drift |
| 4.4 Deprecation policy ≥6mo | ❓ UNCHECKED | chưa thấy preamble/ADR |
| 4.5 MAJOR migration guide | N/A | chưa có MAJOR bump |

### Cat 5 — Integration Test Coverage = 17/20 (5.3 P1 FAIL)
| Check | Verdict | Note |
|---|---|---|
| 5.1 Mọi endpoint có happy-path IT | ⚠️ PARTIAL | SSO có SsoControllerTest + SsoCodeServiceTest; phần lớn domain có IT; chưa exhaustive |
| 5.2 Error path 401/403/404/422/429 | ❓ UNCHECKED | chưa sample 5 endpoint error-path IT |
| 5.3 Consumer-driven contract test (Pact) | ❌ FAIL P1 | không có pact-jvm; detector tĩnh (check-fe-be/be-fe) là proxy duy nhất |
| 5.4 Backwards-compat oasdiff | ⚠️ PARTIAL | 2 grep detector thay oasdiff; chưa có schema diff CI |
| 5.5 Schema validation runtime | N/A P2 | optional |

---

## 3. Detector output (cross-cutting drift)

**`check-fe-be-api-contract.sh`** — Checked 33, Drift 5:
1. `GET /api/platform/branding/ai/generate-theme` → B6 (method drift, real)
2. `GET /api/v1/instance/config` → B3 (real 404)
3. `POST /api/auth/2fa/verify` → FALSE POSITIVE (BE dual-path array)
4. `POST /api/auth/forgot-password` → B4 (real 404)
5. `POST /api/auth/reset-password` → B4 (real 404)

**`check-be-fe-url-contract.sh`** — Checked 4, Missing 0 → PASS (mọi BE-built FE path resolve: reset-password, beta-signup/code, verify-email, staff/accept-invite).

---

## 4. Findings → Gaps

| Gap | P | Domain | Finding |
|-----|---|--------|---------|
| GAP-1332 | P0 | Backend | SSO endpoint cluster undocumented (B1) |
| GAP-1333 | P1 | Backend | Impersonation endpoints undocumented (B2) |
| GAP-1334 | P1 | Mixed | FE→BE drift `/api/v1/instance/config` 404 (B3) |
| GAP-1335 | P1 | Mixed | FE→BE drift forgot/reset-password path (B4) |
| GAP-1336 | P2 | Mixed | FE→BE method drift generate-theme GET vs POST (B6) |
| GAP-1337 | P1 | Backend | Cross-service error envelope inconsistency (B5) |
| GAP-1338 | P2 | Backend | Versioning inconsistency kitehub vs kiteclass (B7) |

---

## 5. References
- Rubric: `.claude/rules/audit-skill-rubric-api-contract-audit.md` §2 (5 category × per-check)
- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md`
- Detectors: `scripts/check-fe-be-api-contract.sh`, `scripts/check-be-fe-url-contract.sh`
- Prior baseline: Wave 40 `72/100`; Wave 103/104.5 audit obligations GAP-708/716
- Pipeline: `.claude/rules/audit-to-gap-pipeline.md` (findings → gaps)
