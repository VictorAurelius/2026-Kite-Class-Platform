# GAP-1217: Quality gate ≥70 không chạy trước DEPLOYED — mock đi thẳng COMPLETED

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-11 (branding-100 failure-mode audit #4)
**Affects:** kitehub-branding job pipeline (quality gate per ai-branding-guidelines §5 + GAP-1021 AC3)

## Problem

Thiết kế (kit v2 step 6 + ai-branding-quality-gate skill) yêu cầu quality gate /100 ≥70 trước deploy; pipeline mock đi PROCESSING→COMPLETED không chấm — asset kém vẫn DEPLOYED. GAP-1021 AC3 unmet, chưa có gap riêng track enforce point.

## Proposed Fix

Gate chạy trong pipeline trước flip DEPLOYED; <70 → FAILED với lý do + per-resource approve (ADR-037). Bucket C/E wave branding-100.

## Acceptance Criteria

- [ ] Asset score <70 không bao giờ DEPLOYED tự động
- [ ] Score hiển thị ở preview/approve step

## Related

- Failure-mode #4; GAP-1021 AC3; `ai-branding-quality-gate` skill
