# GAP-1307: StorageController download-url bỏ qua LMS enrollment paywall (chỉ check visibility, không check enrollment)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (wave-p0-closeout-1 Bucket A — cross-flow sweep DEFER, PR #2403)
**Affects:** `kiteclass-core` storage + LMS (cross-module)

## Problem

`StorageController.generateDownloadUrl` (`GET /api/v1/storage/{fileId}/download-url`, `StorageController.java:140`) enforce access control bằng **visibility model** (`PUBLIC` / `PRIVATE` / `TENANT`) — KHÔNG check LMS enrollment.

Hệ quả paywall bypass: file tài liệu (material) của **bài học trả phí** nếu được lưu ở scope `TENANT` → **bất kỳ user cùng tenant nào** (kể cả student CHƯA enroll khóa trả phí) đều có thể lấy presigned download URL + tải nội dung. Đây cùng class với GAP-1115/1116 (LMS paywall) nhưng qua **đường storage download**, mà `LessonAccessGuard` (shipped #2403, service-layer LMS) KHÔNG cover — guard chỉ gate read-path (`getCourseStructureForStudent` / `getLessonForStudent`) + write-path (`completeLesson`), không gate file-download path.

Phát hiện qua cross-flow sweep của Bucket A (`cross-flow-bug-class-sweep.md` §3): classify FIX (3 LMS service site) + EXEMPT (guest/teacher) + **DEFER (this) = storage download-url** — cross-module concern ngoài scope service-layer của Bucket A.

## Proposed Fix

Khi file thuộc tài liệu bài học trả phí, storage download-url PHẢI thêm enrollment check (delegate `LessonAccessGuard` hoặc tương đương) — không chỉ dựa visibility. Cần liên kết file ↔ lesson ↔ pricing (cross-module storage↔LMS). Free/preview lesson + non-lesson files không đổi hành vi.

## Acceptance Criteria

- [ ] Student cùng tenant CHƯA enroll khóa trả phí → `download-url` cho file của bài trả phí bị từ chối (403).
- [ ] Student đã enroll → tải bình thường (200).
- [ ] File của bài free/preview + file không thuộc lesson → không bị siết (hành vi cũ giữ nguyên).
- [ ] Regression test (CI-bound `*Test`): non-enrolled → reject, enrolled → allow.

## Related

- Discovered in: PR #2403 (wave-p0-closeout-1 Bucket A cross-flow sweep DEFER) — coordinator allocated gap-ID per `multi-session-concurrency-coordination.md` (agent cố ý không tự mint để tránh collision).
- GAP-1115 / GAP-1116 (LMS paywall read+write path — service-layer, đã hardened qua `LessonAccessGuard` #2403).
- `cross-flow-bug-class-sweep.md` §3 — sweep evidence nguồn.
