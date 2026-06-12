---
title: API Contract Audit — post-wave cadence (ui-kits-100 + landing-100)
status: complete
created: 2026-06-12
phase: phase-1-beta
wave: post-ui-kits-100+landing-100 (cadence catch-up per post-wave-audit-mandate, deadline 2026-06-14)
audit_skill: api-contract-audit
audit_version: v2 (per-check rubric audit-skill-rubric-api-contract-audit.md v1.0.1)
baseline_audit: documents/04-quality/audits/api-contract/2026-05-19-wave-98-new-contracts.md
baseline_score: 76/100 C FAIL (2 P0)
base_sha: 1f6baea26
gaps_filed: [GAP-1251, GAP-1252]
---

# API Contract /100 — Post-wave Cadence Audit (2026-06-12)

**Base SHA:** `1f6baea26` (đã gồm branding-100 Bucket A/F #2356/2357; PR #2358/#2359 BE branding đang merge mid-audit — audit chạy trên snapshot worktree, reviewer re-verify final main).
**Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md`
**Rubric:** `.claude/rules/audit-skill-rubric-api-contract-audit.md` v1.0.1 (5 cat × ≥5 sub-check; 1 P0 FAIL → cat cap ≤16 + audit-level verdict FAIL).
**Gate:** Phase 1 BETA ≥80.
**Constraint:** code-level/artifact-based audit (live curl blocked GAP-612 AWS suspension); reliance trên controller signatures + DTO grep + doc cross-ref + IT presence.

## Aggregate verdict

**80/100 C+ — raw score đạt gate ≥80 NHƯNG audit-level verdict FAIL** (1 P0 §2.1.1: branding endpoint coverage). **Delta vs Wave 98 baseline (76): +4.**

Driver: 2 P0 của Wave 98 đã RESOLVED (GAP-662 EmailController URL drift reconciled + GAP-663 PreferencesController IT shipped), nhưng branding-100 introduce **NEW P0** endpoint-coverage gap (~13 branding endpoint undocumented) → raw score lên 80 mà verdict vẫn FAIL theo rubric P0-mechanic.

## Scope

Cadence catch-up: re-verify 2 Wave-98 P0 fix + delta-sweep branding-100 BE (8 branding controllers, ~29 endpoint) cho coverage/versioning/deprecation. ui-kits-100 + landing-100 = FE/kit (không chạm controller). Scale toàn repo: 102 controller, 380 @Mapping.

## State-check chain (per `audit-to-gap-pipeline.md` §2.5)

| Artifact | Verify | Result |
|---|---|---|
| GAP-662 EmailController URL drift | `grep RequestMapping EmailController.java` vs doc | ✅ RESOLVED — Option B sync: doc sửa `/api/email/send`→`/api/platform/emails/send` khớp code; gateway = internal service-to-service (không FE-proxy → no 404 risk). GAP-662 DONE. |
| GAP-663 PreferencesController IT | `find -name PreferencesController*` test | ✅ RESOLVED — `PreferencesControllerIT.java` tồn tại. GAP-663 DONE. |
| GAP-637/638 (Wave 92 carry) | `query-gaps.sh 637/638` | GAP-637 DONE (admin v1 @PreAuthorize); GAP-638 PARTIAL P1 (typed DTO defer) — không P0. |
| Branding endpoint coverage | code-set − doc-set diff | ❌ ~13 undocumented (xem bug list) |
| Legacy BrandingJobController @Deprecated | `grep @Deprecated` | ❌ no annotation (dual-mount) |
| Versioning `/api/v1/` | namespace scan | ⚠️ legacy `/api/platform/{emails,branding/*}` unversioned (pre-existing, GAP-733 email defer) |

## Bug list (precedes score per primacy §4)

### P0 — branding endpoint coverage (audit-blocking)

1. **P0 (Cat 1 §2.1.1) — ~13 branding endpoint undocumented trong api-contract.md.**
   - Lõi wizard branding-100: `POST /api/v1/branding/jobs` (submit, `BrandingJobV1Controller:111`), `POST /jobs/preview-banner` (:169), `POST /jobs/{jobId}/approve` (:251) → **0 doc hits** mỗi cái.
   - Legacy namespaces undocumented: `/api/platform/branding/content/**` (ContentGenerationController), `/api/platform/branding/jobs/**` (BrandingJobController 5 endpoint), `/api/platform/branding/assets/**` (AssetStorageController 3 endpoint).
   - Evidence: `grep -rln "<path>" documents/01-business/` → 0 files. Submit/preview/approve là luồng user-facing chính của wave.
   - File gap: **GAP-1251 P1** (severity gap = P1 vì endpoint internal/admin-scoped + branding-100 BE mid-merge có thể ship docs; nhưng rubric §2.1.1 cố định P0 → cat cap + verdict FAIL).

### P1 — Wave scope

2. **P1 (Cat 4 §2.4.3) — legacy BrandingJobController dual-mount không @Deprecated.**
   - `BrandingJobController` `/api/platform/branding/jobs` (line 43) chạy song song `BrandingJobV1Controller` `/api/v1/branding/jobs` (line 58). V1 javadoc nhắc legacy nhưng legacy KHÔNG có `@Deprecated` + no removal-date.
   - File gap: **GAP-1252 P2**.

3. **P1 (Cat 1 §2.1.2 doc-orphan) — `kiteclass/branding-wizard/api-contract.md` document `/api/v1/instances` nhưng wizard thật ở `/api/v1/branding/jobs` (KiteHub).** Doc-location/ownership confusion. Bundle GAP-1251.

### P2 — observation

4. **P2 (Cat 4 §2.4.1) — legacy `/api/platform/{emails,branding/ai,branding/content,branding/templates,branding/jobs,branding/assets}` unversioned.** Pre-existing (không Wave-introduced); email tracked GAP-733 (defer rename Wave 109+); branding legacy cùng class. Không file mới.

## Category scores (per rubric §2)

| # | Category (20) | Sub-check verdict | Score | Notes |
|---|---|---|:---:|---|
| 1 | **Endpoint Coverage** | §2.1.1 FAIL (13 branding undocumented) P0 → cap ≤16; §2.1.2 1 doc-orphan (branding-wizard); §2.1.3/1.4 public+gateway ok | **16/20** | P0 cap. Preferences/email giờ PASS (GAP-662/663). |
| 2 | **Request/Response Match** | EmailController khớp; preferences cookie/DTO reconciled (GAP-663); branding undocumented → no schema để match (coverage issue not match); 1 P1 branding-wizard mismatch | **18/20** | 1 P1 |
| 3 | **Error Code Consistency** | RFC7807 surface 11 handler (Wave 83 carry); branding undocumented endpoint thiếu error-code doc | **16/20** | 1 P1 |
| 4 | **Versioning & Deprecation** | §2.4.1 legacy unversioned (P1, pre-existing); §2.4.3 dual-mount no @Deprecated (P1, GAP-1252) | **14/20** | 2 P1 |
| 5 | **Integration Test Coverage** | PreferencesControllerIT shipped ✅ (GAP-663); branding wizard IT partial; §5.3 CDC tests vẫn missing (Wave 40 carry P1) | **16/20** | 1 P1 |

## Overall

```
Total = 16 + 18 + 16 + 14 + 16 = 80/100  (Grade C+)
Raw score = 80 → đạt numeric gate Phase 1 BETA ≥80
Audit-level verdict = FAIL (1 P0 §2.1.1 branding endpoint coverage)
```

## Delta vs Wave 98 baseline

| Metric | Wave 98 (2026-05-19) | This audit (2026-06-12) | Delta |
|---|:---:|:---:|:---:|
| Overall | 76/100 C FAIL (2 P0) | **80/100 C+** (1 P0) | **+4** |
| P0 count | 2 (EmailController URL + Preferences zero-IT) | 1 (branding coverage) | −1 net |
| Cat 1 Endpoint Coverage | capped (preferences undoc'd) | 16 (branding undoc'd) | swap finding |
| Cat 5 IT | capped (Preferences zero-IT) | 16 (PreferencesIT shipped) | +resolved |

**Delta drivers (+4):** GAP-662 doc reconciled (+) + GAP-663 PreferencesControllerIT (+); offset bởi NEW branding-100 coverage P0 giữ verdict FAIL.

## Path to clean PASS (verdict)

1. **GAP-1251** (P1): document 13 branding endpoint → §2.1.1 diff=0 → Cat 1 → 18-20, clear P0 → **verdict PASS @ ~82-84**.
2. **GAP-1252** (P2): @Deprecated legacy controller → Cat 4 +2.
3. (carry) CDC tests §5.3 + RFC7807 full surface → Cat 3/5 polish.

Sau GAP-1251: **~82/100 PASS** (raw + verdict).

## Gaps filed

- **GAP-1251** (P1, Backend) — branding-100 wizard + legacy endpoints undocumented (~13).
- **GAP-1252** (P2, Backend) — legacy BrandingJobController dual-mount no @Deprecated.
