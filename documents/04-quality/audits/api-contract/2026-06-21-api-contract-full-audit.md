---
audience: mixed
---

# API-Contract Full Audit — phase-1-closeout-loop refresh (2026-06-21)

**Audit ID:** AUDIT-2026-06-21-api-contract-full
**Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md` (/100, 5 categories, per-check pass/fail per `audit-skill-rubric-api-contract-audit.md`)
**Scope:** Toàn monorepo — controller kiteclass-core + kitehub (6 service) ↔ 76+ `documents/01-business/**/api-contract.md`. Focus surface mới kể từ 2026-06-14: mobile-OTP signup (GAP-286 — `OtpController` + 3-layer docs `documents/01-business/kitehub/signup-otp/`). Re-verify trạng thái 7 gap baseline GAP-1332..1338 + GAP-1251 branding.
**Method:** Trích `@*Mapping` từ controller ↔ đối chiếu api-contract.md (path/method/request/response/error-codes); chạy 2 detector tĩnh `check-fe-be-api-contract.sh` + `check-cross-layer-contract-drift.sh`; kiểm RFC 7807 error envelope; kiểm versioning URL; query `gap-status.csv` cho trạng thái baseline gaps.
**Run date:** 2026-06-21 · **Auditor:** Claude (Opus 4.8) · **Branch:** read từ main HEAD `3d5179551`

---

## SCORE: 81/100 — VERDICT: 🔴 FAIL (2 P0 sub-check FAIL) — delta **+1** vs baseline 80/100

> Per `audit-skill-rubric-api-contract-audit.md` §3 primacy: bug list là deliverable; điểm chỉ descriptive. Audit-level verdict = FAIL vì ≥1 P0 sub-check FAIL (Cat 1.1 endpoint undocumented = GAP-1251 branding cluster + Cat 4.1 unversioned URL).

| # | Category (20pts) | Score | Δ | P0 FAIL? |
|---|------------------|:-----:|:--:|:--------:|
| 1 | Endpoint Coverage | 16/20 | +2 | ✅ FAIL (1.1 — GAP-1251 branding) |
| 2 | Request/Response Schema Match | 18/20 | 0 | — |
| 3 | Error Code Consistency | 16/20 | −1 | — |
| 4 | Versioning & Deprecation | 14/20 | 0 | ✅ FAIL (4.1) |
| 5 | Integration Test Coverage | 17/20 | 0 | — |
| | **TOTAL** | **81/100** | **+1** | **FAIL** |

**Net change từ baseline 2026-06-14 (80/100):** 4 baseline findings RESOLVED (SSO B1 + Impersonation B2 + forgot/reset-password B4 + generate-theme B6 — đều DONE qua PR #2415) + OTP signup surface mới được document đầy đủ (Cat 1 +2). Bị cấn lại bởi: 1 baseline finding mới surface (OTP error-code drift, Cat 3 −1) + 2 P0 cap chưa giải quyết (GAP-1251 branding undocumented Phase 1 BETA + GAP-1338 versioning deferred Phase 2). Net +1 → vẫn FAIL.

---

## 1. BUG LIST (deliverable — file:line evidence + severity)

### 🔴 P0 — block v1.0.0-rc (đều đã có gap tracking)

| # | Finding | Evidence | Gap (existing) |
|---|---------|----------|-----|
| B1 | **Branding wizard + legacy endpoints UNDOCUMENTED** — `/api/v1/branding`, `/api/v1/branding/instances`, `/api/v1/branding/jobs`, `/api/v1/branding/public`, `/api/v1/settings/branding` (~13 endpoint) không có api-contract.md khớp. Đây là P0-class undocumented cluster còn lại sau khi SSO+Impersonation đã fix. | detector `check-cross-layer-contract-drift.sh` flag 5 branding base path; `documents/01-business/kitehub/ai-branding/api-contract.md` + `kiteclass/branding-wizard` + `kiteclass/branding-api` có doc nhưng KHÔNG cover wizard-job/instances surface | **GAP-1251** (PARTIAL 50%) — STILL CAPS |
| B2 | **Versioning inconsistency kitehub `/api/platform/**` + `/api/auth/**` unversioned** — rubric 4.1 yêu cầu 0 endpoint ngoài `/api/v[0-9]+/`. kitehub vẫn dùng namespace không version trong khi kiteclass dùng `/api/v1`. Intentional namespace nhưng undocumented as policy. | `grep '@RequestMapping("/api/platform'` nhiều hit; kiteclass `/api/v1/` | **GAP-1338** (PARTIAL 80%, deferred phase-2) |

### 🟡 P2 — NEW finding (chưa có gap)

| # | Finding | Evidence | Gap |
|---|---------|----------|-----|
| B3 | **OTP error-code value drift docs ↔ code** — `signup-otp/api-contract.md:32` ghi `{ "error": "INVALID_PHONE" }` và `use-cases.md:25` ghi `400 INVALID_PHONE`, nhưng `OtpController.java:81,92` set `error="OTP_INVALID_PHONE"`; thêm code `OTP_INVALID_PAYLOAD` (line 92) chưa được document. Consumer mobile sẽ branch sai error code. | docs vs `OtpController.java:81,92` | **GAP-1508** (NEW) |
| B4 | **`check-cross-layer-contract-drift.sh` heuristic v1 FP rate quá cao** — báo 84 drift candidate nhưng verify mẫu: `/api/v1/classes` (8 doc), `/api/v1/courses` (5), `/api/v1/students` (12), `/api/v1/grades` (4) ĐỀU đã document → ~đa số là false-positive. Detector không thể HARD-STOP (per `contract-first-for-cross-layer.md` §6.2) với FP rate này → mất giá trị làm CI guard cho Cat 1.1/1.2. | detector output 84 candidate vs grep doc coverage | **GAP-1509** (NEW) |

### ✅ Baseline findings RESOLVED kể từ 2026-06-14 (PR #2415)

| Baseline | Finding | Status mới |
|---|---|---|
| B1 (P0) | SSO endpoint cluster undocumented | ✅ GAP-1332 DONE — `documents/01-business/kitehub/sso/{rules,use-cases,api-contract}.md` tồn tại |
| B2 (P1) | ImpersonationController 3 endpoint undocumented | ✅ GAP-1333 DONE — documented trong admin-audit |
| B4 (P1) | FE→BE drift forgot/reset-password path | ✅ GAP-1335 DONE |
| B6 (P2) | FE→BE method drift generate-theme GET vs POST | ✅ GAP-1336 DONE |

### ⏳ Baseline findings còn OPEN (tracked, không re-file)

| Baseline | Finding | Status |
|---|---|---|
| B3 (P1) | FE→BE drift `GET /api/v1/instance/config` 404 | GAP-1334 PARTIAL 30% (detector vẫn flag) |
| B5 (P1) | Cross-service error envelope inconsistency | GAP-1337 PARTIAL 50%, deferred phase-2 |
| B7 (P2) | Versioning inconsistency | GAP-1338 PARTIAL 80%, deferred phase-2 → B2 above |

### False-positive đã loại
- `POST /api/auth/2fa/verify` (detector `check-fe-be-api-contract.sh`) → BE `TwoFactorController` expose dual-path array `/api/v1/auth/2fa/verify` + `/api/auth/2fa/verify`; detector không resolve array path → FP (giữ nguyên từ baseline).
- 84 cross-layer-drift candidate (classes/courses/students/grades…) → kiteclass-core domains ĐÃ có api-contract.md (48 files); detector heuristic v1 không khớp prose-format → FP (→ B4 GAP-1509).

---

## 2. Per-category per-check verdict

### Cat 1 — Endpoint Coverage = 16/20 (1.1 P0 FAIL → cap; +2 vs baseline)
| Check | Verdict | Note |
|---|---|---|
| 1.1 Mọi `@*Mapping` documented | ❌ FAIL P0 | SSO+Impersonation+OTP đã fix; branding cluster (GAP-1251 ~13 endpoint) còn undocumented → cap. **OTP signup ✅ documented** (paths khớp controller exact). |
| 1.2 No docs-orphan | ❓ UNCHECKED | detector FP cao (B4) → không verify được full-set |
| 1.3 Public endpoint tách section | ✅ PASS | PublicBranding/PublicConfig + OTP `permitAll /api/v1/auth/**` documented |
| 1.4 Gateway-proxied routes mapped | ⚠️ PARTIAL | đa số doc nêu base path |
| 1.5 Non-REST (SSE) documented | ✅ PASS | DeployStreamController SSE trong branding-wizard doc |
| 1.6 Webhook receiver documented | ✅ PASS | PaymentWebhook + MigrationWebhook documented |

### Cat 2 — Request/Response Schema Match = 18/20 (sampled, no FAIL; Δ0)
| Check | Verdict | Note |
|---|---|---|
| 2.1 Request DTO fields | ✅ PASS | OTP `RequestOtpRequest{phone,channel}` + `VerifyOtpRequest{phone,code}` khớp api-contract.md |
| 2.2 Response DTO fields | ✅ PASS | OTP `RequestOtpResponse{requestId,channel,expiresInSeconds,mock}` + `VerifyOtpResponse{verified,signupToken,reason}` khớp doc |
| 2.3 Field types | ✅ PASS | sampled |
| 2.4 Required vs optional | ❓ UNCHECKED | OTP `@NotBlank @Pattern` khớp doc "required"; full-set chưa sample |
| 2.5 Nested objects typed | ✅ PASS | sampled |
| 2.6 Enums khớp Java const | ✅ PASS | OTP `channel` ZALO/SMS khớp doc |

### Cat 3 — Error Code Consistency = 16/20 (3.3 P1 + 3.2 P2 OTP drift; −1 vs baseline)
| Check | Verdict | Note |
|---|---|---|
| 3.1 HTTP status match | ✅ PASS | OTP 200/400/429 khớp doc |
| 3.2 App error codes per-endpoint | ❌ FAIL P2 | **NEW B3** — OTP docs ghi `INVALID_PHONE`, code set `OTP_INVALID_PHONE` + `OTP_INVALID_PAYLOAD` undocumented (GAP-1508) |
| 3.3 Error body schema documented | ❌ FAIL P1 | B5 baseline — ProblemDetail (kitehub) vs ErrorResponse (kiteclass) chưa thống nhất (GAP-1337) |
| 3.4 Validation 400 field-level | ✅ PASS | cả 2 handler có field errors; OTP `MethodArgumentNotValidException` handler có |
| 3.5 Rate-limit 429 documented | ✅ PASS (OTP) | OTP request-otp 429 `RATE_LIMITED` + `retryAfterSeconds` documented khớp code |

### Cat 4 — Versioning & Deprecation = 14/20 (4.1 P0 FAIL → cap; Δ0)
| Check | Verdict | Note |
|---|---|---|
| 4.1 Mọi endpoint dưới `/api/v[0-9]+/` | ❌ FAIL P0 | B2 — kitehub `/api/platform/**` + `/api/auth/**` unversioned (GAP-1338 deferred phase-2). OTP dùng `/api/v1/auth/signup` ✅ versioned đúng. |
| 4.2 No breaking change MINOR | ❓ UNCHECKED | không có oasdiff baseline |
| 4.3 Deprecated marked | ✅ PASS | không thấy `@Deprecated` drift |
| 4.4 Deprecation policy ≥6mo | ❓ UNCHECKED | chưa thấy preamble/ADR |
| 4.5 MAJOR migration guide | N/A | chưa có MAJOR bump |

### Cat 5 — Integration Test Coverage = 17/20 (5.3 P1 FAIL; Δ0)
| Check | Verdict | Note |
|---|---|---|
| 5.1 Mọi endpoint có happy-path IT | ⚠️ PARTIAL | OTP có `OtpServiceTest` + `OtpControllerTest` (cited trong api-contract.md verification chain); phần lớn domain có IT; chưa exhaustive |
| 5.2 Error path 401/403/404/422/429 | ❓ UNCHECKED | chưa sample 5 endpoint error-path IT |
| 5.3 Consumer-driven contract test (Pact) | ❌ FAIL P1 | không có pact-jvm; 2 detector tĩnh là proxy duy nhất |
| 5.4 Backwards-compat oasdiff | ⚠️ PARTIAL | grep detector thay oasdiff; chưa có schema diff CI |
| 5.5 Schema validation runtime | N/A P2 | optional |

---

## 3. Detector output (cross-cutting drift)

**`check-fe-be-api-contract.sh`** — Checked 32, Drift 2:
1. `GET /api/v1/instance/config` → GAP-1334 (real 404, PARTIAL 30%)
2. `POST /api/auth/2fa/verify` → FALSE POSITIVE (BE dual-path array)

→ Cải thiện rõ vs baseline (5 drift → 2; forgot/reset-password + generate-theme đã fix).

**`check-cross-layer-contract-drift.sh`** — 84 drift candidate, WARN mode. Verify mẫu cho thấy ~đa số FP (classes/courses/students/grades đều có doc). Genuine undocumented = branding cluster (GAP-1251). → B4 GAP-1509 (detector cần cải thiện để dùng được làm guard).

---

## 4. Findings → Gaps

| Gap | P | Domain | Finding | Status |
|-----|---|--------|---------|--------|
| GAP-1251 | P1 | Backend | Branding wizard + legacy endpoints undocumented (~13) (B1) | EXISTING PARTIAL 50% — caps verdict |
| GAP-1338 | P2 | Backend | Versioning unversioned kitehub (B2) | EXISTING PARTIAL 80% phase-2 |
| GAP-1334 | P1 | Mixed | FE→BE `/api/v1/instance/config` 404 | EXISTING PARTIAL 30% |
| GAP-1337 | P1 | Backend | Error envelope inconsistency | EXISTING PARTIAL 50% phase-2 |
| **GAP-1508** | **P2** | **Backend** | **OTP error-code value drift docs↔code (B3)** | **NEW 2026-06-21** |
| **GAP-1509** | **P2** | **DevOps** | **cross-layer-drift detector heuristic FP rate (B4)** | **NEW 2026-06-21** |

---

## 5. GAP-1251 + OTP verdict summary

- **GAP-1251 (branding undocumented ~13 endpoints):** STILL PARTIAL 50% → **STILL CAPS verdict FAIL** (Cat 1.1 P0). Không re-file (per `audit-to-gap-pipeline.md` §2.5 dedup).
- **OTP endpoints (GAP-286):** **NOW DOCUMENTED** — 3-layer docs `documents/01-business/kitehub/signup-otp/` tồn tại; `api-contract.md` paths khớp `OtpController` exact (`POST /api/v1/auth/signup/request-otp` + `/verify-otp`); request/response schema + 429 rate-limit + RFC 7807 envelope đầy đủ. **Cat 1.1 OTP coverage = PASS.** Tồn 1 drift nhỏ: error-code value (`INVALID_PHONE` doc vs `OTP_INVALID_PHONE` code) → GAP-1508 P2.

---

## 6. References
- Rubric: `.claude/rules/audit-skill-rubric-api-contract-audit.md` §2 (5 category × per-check)
- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md`
- Detectors: `scripts/check-fe-be-api-contract.sh`, `scripts/check-cross-layer-contract-drift.sh`
- Prior baseline: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` (80/100)
- Pipeline: `.claude/rules/audit-to-gap-pipeline.md` (findings → gaps)
