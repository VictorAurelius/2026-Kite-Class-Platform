# GAP-1217: Quality gate ≥70 không chạy trước DEPLOYED — mock đi thẳng COMPLETED

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-11 (branding-100 failure-mode audit #4)
**Affects:** kitehub-branding job pipeline (quality gate per ai-branding-guidelines §5 + GAP-1021 AC3)

## Problem

Thiết kế (kit v2 step 6 + ai-branding-quality-gate skill) yêu cầu quality gate /100 ≥70 trước deploy; pipeline mock đi PROCESSING→COMPLETED không chấm — asset kém vẫn DEPLOYED. GAP-1021 AC3 unmet, chưa có gap riêng track enforce point.

## Proposed Fix

Gate chạy trong pipeline trước flip DEPLOYED; <70 → FAILED với lý do + per-resource approve (ADR-037). Bucket C/E wave branding-100.

## Acceptance Criteria

- [x] Asset score <70 không bao giờ DEPLOYED tự động — **code-level DONE**: `BrandingJobV1Controller.approve` chấm `QualityScoreAggregator.aggregate(job)` TRƯỚC `provisionAsync`; `!passed` → `markJobFailed` + HTTP 422 `QUALITY_GATE_FAILED` (không deploy). Test PASS (gate-fail + gate-pass).
- [x] Score hiển thị ở preview/approve step — **code-level DONE**: approve response trả `qualityScore` (pass) hoặc `score`/`threshold`/`issues` (fail). (Score endpoint GET `/jobs/{id}/quality-score` đã có sẵn GAP-272c cho preview step.)

## Related

- Failure-mode #4; GAP-1021 AC3; `ai-branding-quality-gate` skill

## Log

- **2026-06-12** (Wave branding-100 Bucket C — code-level DONE, runtime-walk pending): Quality gate ≥70 wired vào pipeline trước DEPLOYED, DÙNG LẠI aggregator `QualityScoreAggregator` (GAP-272c #906, không rebuild). `BrandingJobV1Controller.approve` chấm score trước khi gọi `provisionAsync`: `!quality.passed()` → `brandingJobService.markJobFailed` + return 422 `QUALITY_GATE_FAILED` (kèm score/threshold/issues); pass → 202 + `qualityScore`. Threshold externalize `quality-gate.pass-threshold:70` (đã có). Per-resource approve (ADR-037) còn defer Bucket E FE. Tests PASS: `BrandingJobV1ControllerTest` 2 case mới (gate-fail 422 + markJobFailed + no-provision / gate-pass 202 + qualityScore + provisionAsync). Status PARTIAL: runtime-walk xác minh deploy job score<70 thật bị chặn (G2) + per-resource approve FE chưa.
