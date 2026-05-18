# GAP-001: Quyết định giữ/xóa kiteclass-gateway service

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Architecture
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/docker-platform-architecture.md`
- `kitehub/docker-compose.kitehub.yml`
- `kiteclass/kiteclass-gateway/` (code)

## Problem

KiteClass folder có 3 services: `kiteclass-core`, `kiteclass-frontend`, `kiteclass-gateway`. Nhưng `kitehub/docker-compose.kitehub.yml` chỉ start 2 services đầu, bỏ `kiteclass-gateway`. Routing được xử lý bởi `kite-gateway` (shared). Không có ADR giải thích rõ.

## Context

- Docs `docker-platform-architecture.md` nói: "Gateway — Single entry point routing `/api/v1/subscriptions/*` to KiteHub services and `/api/v1/courses/*` to KiteClass."
- `kite-gateway` (shared) đã cover KiteClass routing → `kiteclass-gateway` trở thành redundant
- Code `kiteclass/kiteclass-gateway/` vẫn tồn tại, gây confusion khi onboarding

## Evidence

- Grep `kiteclass-gateway` trong `kitehub/docker-compose.kitehub.yml` → không có service này
- Folder `kiteclass/kiteclass-gateway/` vẫn có Dockerfile, src/, pom.xml
- `kiteclass/docker-compose.dev.yml` + `docker-compose.standalone.yml` — cần check xem có dùng kiteclass-gateway không

## Proposed Fix

### Option A: Xóa kiteclass-gateway
- Remove folder `kiteclass/kiteclass-gateway/`
- Update `kiteclass/docker-compose.*.yml` nếu có reference
- Clean up `03-planning/prs/01-gateway-prs.md` nếu có PR liên quan

### Option B: Giữ cho standalone mode
- Document use case: KiteClass chạy độc lập không có KiteHub
- Tạo ADR `documents/02-architecture/adr-kiteclass-gateway-usage.md`
- Update `docker-platform-architecture.md` giải thích 2 mode (integrated vs standalone)

## Acceptance Criteria

- [ ] User quyết định Option A hoặc B
- [ ] Nếu A: code removed, docs updated
- [ ] Nếu B: ADR viết, docs giải thích rõ, standalone mode có docker-compose riêng
- [ ] Onboarding không còn confusion

## Log

- 2026-04-14 — Phát hiện trong review về local test setup
