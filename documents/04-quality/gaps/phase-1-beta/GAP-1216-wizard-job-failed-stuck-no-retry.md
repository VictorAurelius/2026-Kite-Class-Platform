# GAP-1216: Job FAILED → user kẹt DeployingStep — không Retry/Back

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-11 (branding-100 failure-mode audit #3 + persona F7)
**Affects:** KH wizard DeployingStep

## Problem

Job FAILED chỉ append 1 dòng log đỏ — không nút Retry, không Back, không hướng dẫn. User kẹt màn deploy vô hạn (dead-end per pre-handoff §2.9 — retry/DLQ UX absent).

## Proposed Fix

FAILED state UI: lý do ngắn + Retry (re-enqueue) + Back-to-edit + liên hệ hỗ trợ. Bucket E wave branding-100.

## Acceptance Criteria

- [ ] FAILED → retry thành công được không mất input
- [ ] Back về bước trước giữ state

## Related

- Failure-mode #3; GAP-1021 (SSE), persona F7
