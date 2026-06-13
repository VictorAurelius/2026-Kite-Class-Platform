# GAP-1293: CI thiếu failsafe gate cho `*IT` slices — `*IT` regressions vô hình ở PR time

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-14 (trong khi đóng GAP-1278 — AC #2 decision recorded)
**Affects:** `.github/workflows/core-ci.yml`, `kiteclass/kiteclass-core/src/test/**/*IT.java`

## Problem

`core-ci.yml` chạy `./mvnw test -P strict-warnings`, mà surefire chỉ pick `*Test`.
Các class `*IT` (failsafe — `mvn verify` / `integration-test`) KHÔNG chạy ở PR CI.
Hệ quả: regression trong `*IT` slices vô hình tại PR time và chỉ lộ khi local
`mvn verify`. GAP-1278 (`AttendanceClassBatchControllerIT` context-load fail) là
ví dụ sống — bug nằm latent trên `main` đúng vì CI không chạy failsafe.

## Proposed Fix

Thêm 1 bước CI failsafe **có mục tiêu** cho các `@WebMvcTest`-style `*IT` slices KHÔNG
dùng Testcontainers (không cần Docker trên runner) — ví dụ chạy
`./mvnw failsafe:integration-test failsafe:verify -Dit.test=...` cho subset slice
không-Docker, HOẶC tách naming convention (`*SliceIT` vs `*Testcontainers IT`).
KHÔNG chạy full failsafe (Testcontainers ITs) trên mọi PR — Docker-on-runner cost +
queue time quá lớn (per `ci-queue-local-runner-threshold.md`).

## Acceptance Criteria

- [ ] CI có gate phát hiện được context-load fail của `@WebMvcTest`-style `*IT` slice tại PR time
- [ ] Full Testcontainers ITs vẫn KHÔNG bắt buộc chạy mỗi PR (cost rationale documented)

## Related

- Discovered in: GAP-1278 closure (AC #2 decision)
- `ci-queue-local-runner-threshold.md` — local-vs-CI runner threshold
