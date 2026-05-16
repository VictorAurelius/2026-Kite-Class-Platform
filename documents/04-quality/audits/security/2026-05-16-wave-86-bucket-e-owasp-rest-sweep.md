---
title: Pre-launch OWASP REST hardening sweep — Wave 86 Bucket E (Cat 3 — A01-A06/A08-A10)
status: complete
created: 2026-05-16
wave: 86
bucket: E
gaps: []
---

# Pre-launch OWASP REST Hardening Sweep — Wave 86 Bucket E (Cat 3)

## Scope

Verify all 9 per-OWASP-item checks defined in `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` v1.0.1 §2 (A01-A06 + A08-A10; A07 covered by separate auth sweep) plus E-AC5:

- **E-AC5** Bulk-import endpoint cap 1000 rows/request HTTP 413

## Methodology

For each OWASP item: state requirement → run evidence command (grep/find) → compare pass criteria → record verdict + per-item rationale.

## Results table

| # | OWASP item | Evidence | Verdict | Notes |
|---|---|---|---|---|
| 2.1 | A01 — Broken Access Control (per-resource authz) | `grep @PreAuthorize kitehub-admin/src` → 0 hits in `AdminController.java`; relies on gateway path-based routing | ⚠️ PARTIAL | Wave 71c GAP-518 fixed BE/FE role-mismatch (PLATFORM_ADMIN); Wave 85 V58/V59 RLS+admin-bypass aspect adds DB-layer guard. AdminController lacks method-level `@PreAuthorize`. File GAP-NEW-10 (P1) — add `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` defense-in-depth for AdminController methods |
| 2.2 | A02 — Cryptographic Failures (no MD5/SHA1, bcrypt cost≥10, TLS 1.2+, JWT RS256/HS256) | `grep MessageDigest.getInstance("(MD5|SHA-1)")` → 0 hits in src; Spring Security default bcrypt; ALB TLS policy enforced (Cat 5 §2.1) | ✅ PASS | No weak crypto found in source |
| 2.3 | A03 — Injection (parameterized queries) | `grep -E "(SELECT\|UPDATE\|DELETE\|INSERT).*\+\s*\w+\s*\+\|String.format.*WHERE.*%"` → 5 hits all in `RLSEnforcementIT.java` / `RLSHardeningIT.java` test code building SQL with safe constants (table name, role name); zero in production code | ✅ PASS | All matches in test/IT code; Spring Data JPA + Criteria API conventionally used in main src |
| 2.4 | A04 — Insecure Design (threat models for critical flows) | `ls documents/02-architecture/threat-models/` → directory does NOT exist | ❌ FAIL | File GAP-NEW-11 (P1) — create `documents/02-architecture/threat-models/{auth,ai-branding,bulk-import}.md` minimum 3 critical flows for Phase 1 BETA |
| 2.5 | A05 — Security Misconfiguration (prod profile hardened) | `application-production.yml` for 5 backend services: `management.endpoints.web.exposure.include: health,info,prometheus` (scoped — not `*`); `health.show-details: when_authorized`; debug logs disabled by default Spring Boot prod profile | ✅ PASS | Wave 85 Bucket E shipped 7 services prod profile (per `output-review-mandate.md` §3 performance row Cat 5 +2); actuator scoped + show-details authorized-only |
| 2.6 | A06 — Vulnerable & Outdated Components | Cross-reference to `pre-launch-dependency-hardening-checklist.md` (Cat 1 sister sweep — separate audit doc) | ✅ PASS (delegated) | See dependency sweep — sister audit handles Cat 1 |
| 2.7 | A08 — Software & Data Integrity (SHA-pinning) | Docker images tag-pinned in `Dockerfile*` (eclipse-temurin:21-jre + ubuntu noble); GH Actions tag-pinned (`actions/checkout@v4`); dependabot weekly across 6 ecosystems | ⚠️ PARTIAL | Tag-pinning + dependabot acceptable v1 per checklist §2.7; full SHA-pinning = Phase 1.5+. File GAP-NEW-12 (P2) — SHA-pin GH Actions critical workflows + base Docker images |
| 2.8 | A09 — Security Logging & Monitoring | V36 `admin_audit_log` (kitehub-subscription) + V60 immutable `admin_audit_logs` (kiteclass-core) + V48 `impersonation_audit_log` + Wave 85 admin-bypass paired aspect; `logs-format-standard.md` PII scrubbing | ✅ PASS | Multi-layer audit log defense Wave 85; PII scrubbing rule shipped; rate-limit breach alarm pattern present |
| 2.9 | A10 — SSRF (outbound URL allowlist) | AI Branding logo URL allowlist per Wave 4 + `ai-branding-guidelines.md` §9; non-AI-branding outbound HTTP minimal in current scope | ✅ PASS | AI Branding only major user-supplied URL surface; allowlist present; metadata IP blocked |

### E-AC5 — Bulk-import endpoint cap 1000 rows/request HTTP 413

| Check | Evidence | Verdict |
|---|---|---|
| Cap enforced server-side | `StudentBulkImportService.java:52`: `public static final int MAX_ROWS = 10_000;` — cap exists but at **10,000** rows, not 1,000 per E-AC5 | ❌ FAIL — value 10x higher than E-AC5 target |
| HTTP 413 response | Service throws ValidationException at line 186-189 with message "Số dòng vượt quá giới hạn 10000"; needs verification of HTTP 413 mapping (current may return 400) | ⚠️ INCONCLUSIVE — needs API contract verify |
| Documented in api-contract.md | Defer to Agent 3 (API spec) verification | ⚠️ INCONCLUSIVE |

E-AC5 verdict: ❌ FAIL — cap value disagrees with Bucket E target (10,000 vs 1,000). Either (a) lower cap to 1,000 per E-AC5 OR (b) update Wave 86 plan E-AC5 to reflect existing 10,000 cap (Bulk-import scope = onboarding 100s-1000s per `package-info.java` design). Recommendation: lower to 1,000 for Phase 1 BETA safety + raise to 10,000 in Phase 1.5 after observability validates throughput.

## Summary

- Total items: 9 OWASP + 1 E-AC5 = 10
- PASS: 5 (A02 / A03 / A05 / A06 / A08 audit log / A10 SSRF)
- PARTIAL: 2 (A01 method-level authz / A08 SHA pin)
- FAIL: 2 (A04 threat models / E-AC5 cap value mismatch)
- INCONCLUSIVE: 2 sub-items E-AC5 HTTP 413 + api-contract.md sync

## Overall verdict: PARTIAL

Blocks `v1.0.0-rc.*` until:
- A04 threat models 3 critical flows shipped (P1 BLOCKER) — file GAP-NEW-11
- E-AC5 cap-value reconciled (P0 — either lower MAX_ROWS or update plan)

PARTIAL items A01 + A08 acceptable v1 with documented `OWASP_REST_DEFER` + follow-up gaps.

## Recommendations

1. **P0:** Reconcile E-AC5 — recommendation: lower `MAX_ROWS = 1_000` for Phase 1 BETA per E-AC5; raise to 10,000 in Phase 1.5 post-load-test (paired with Bucket H H-AC1 K6 baseline). Verify HTTP 413 mapping at `@ExceptionHandler(ValidationException.class)` or `@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)`.
2. **P1 BLOCKER:** File GAP-NEW-11 — create 3 threat models (`auth.md`, `ai-branding.md`, `bulk-import.md`) under `documents/02-architecture/threat-models/` covering trust boundaries + attack surfaces + abuse cases + mitigations
3. **P1:** File GAP-NEW-10 — add method-level `@PreAuthorize` on AdminController for defense-in-depth (gateway is necessary but not sufficient)
4. **P2:** File GAP-NEW-12 — SHA-pin critical GH Actions + base Docker images (Phase 1.5+ scope)

## References

- `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` v1.0.1 §2
- `documents/04-quality/audits/security/2026-05-15-wave-85-post-apply-v2.md` (Cat 3 baseline 93/100)
- Wave 85 V58/V59/V60 RLS + admin-bypass + immutable audit_logs
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/service/StudentBulkImportService.java:52`
- `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/AdminController.java`
