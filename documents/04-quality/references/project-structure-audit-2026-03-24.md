# Project Structure Audit Report

**Date:** 2026-03-24
**Scope:** Docker naming, folder structure, stale files, security

---

## 1. Docker Naming Convention

### Problem: Shared infrastructure mang prefix `kitehub-`

KiteHub và KiteClass **dùng chung** PostgreSQL, Redis, RabbitMQ, MinIO, Gateway.
Nhưng tất cả đều có prefix `kitehub-` → gây hiểu nhầm KiteClass "ké" infra KiteHub.

| Container hiện tại | Thuộc về | Đề xuất |
|-------------------|----------|---------|
| `kitehub-postgres` | **Platform shared** | `kite-postgres` |
| `kitehub-redis` | **Platform shared** | `kite-redis` |
| `kitehub-rabbitmq` | **Platform shared** | `kite-rabbitmq` |
| `kitehub-minio` | **Platform shared** | `kite-minio` |
| `kitehub-mailhog` | **Platform shared** | `kite-mailhog` |
| `kitehub-gateway` | **Platform gateway** (route cả KiteClass) | `kite-gateway` |
| `kitehub-prometheus` | **Platform monitoring** | `kite-prometheus` |
| `kitehub-grafana` | **Platform monitoring** | `kite-grafana` |
| `kitehub-subscription` | KiteHub service | giữ nguyên |
| `kitehub-branding` | KiteHub service | giữ nguyên |
| `kitehub-email` | KiteHub service | giữ nguyên |
| `kitehub-admin` | KiteHub service | giữ nguyên |
| `kitehub-frontend` | KiteHub frontend | giữ nguyên |
| `kiteclass-core` | KiteClass service | giữ nguyên |
| `kiteclass-frontend` | KiteClass frontend | giữ nguyên |

**Compose project name:** `kitehub` → `kite-platform`

### Fixed: Rename 8 shared containers + project name

---

## 2. Docker Compose Files — Duplicates & Outdated

### Problem: 9 docker-compose files, unclear which is canonical

| File | Status | Action |
|------|--------|--------|
| `./docker-compose.kitehub.yml` (root) | **OUTDATED** — chỉ có 2 services, dùng `version: 3.8` | **XOÁ** |
| `kitehub/docker-compose.kitehub.yml` | **CANONICAL** — full 14 services | Giữ, rename containers |
| `kitehub/docker-compose.kitehub-only.yml` | OK — backend only | Update container names |
| `kitehub/docker-compose.oracle-backend.yml` | OK — Oracle deploy | Kiểm tra |
| `kitehub/docker-compose.oracle-frontend.yml` | OK — Oracle deploy | Kiểm tra |
| `kiteclass/docker-compose.dev.yml` | OK — standalone dev | Giữ |
| `kiteclass/docker-compose.standalone.yml` | OK — minimal | Giữ |
| `kiteclass/kiteclass-gateway/docker-compose.dev.yml` | **Nested duplicate** | Kiểm tra |
| `kiteclass/kiteclass-gateway/docker-compose.yml` | **Nested duplicate** | Kiểm tra |

### Fixed: Delete root outdated file, document canonical sources

---

## 3. Duplicate Dockerfiles

### Problem: `./docker/kiteclass/` duplicates service-level Dockerfiles

| Duplicate | Source of truth |
|-----------|----------------|
| `docker/kiteclass/Dockerfile.core` | `kiteclass/kiteclass-core/Dockerfile` |
| `docker/kiteclass/Dockerfile.frontend` | `kiteclass/kiteclass-frontend/Dockerfile` |
| `docker/kiteclass/Dockerfile.gateway` | `kiteclass/kiteclass-gateway/Dockerfile` |

### Fixed: Delete `./docker/kiteclass/` directory

---

## 4. Nested Empty Directory

### Problem: `kitehub/kitehub/` — empty artifact

Path: `kitehub/kitehub/kitehub-admin/src/test/resources/` — completely empty, leftover from refactor.

### Fixed: Delete `kitehub/kitehub/`

---

## 5. Stale Files

| File | Issue | Action |
|------|-------|--------|
| `documents/action-1.md` | Duplicate of `07-archived/early-ideas/action-1.md` | XOÁ |
| `documents/image.png` | Stale image at documents root | Move to archived |
| `kiteclass/CURRENT-WORK.md` | Session artifact | XOÁ |
| `kiteclass/kiteclass-frontend/SESSION-STATUS.md` | Session artifact | XOÁ |

### Fixed: Remove stale files

---

## 6. Security — .env Files Committed

| File | Risk | Action |
|------|------|--------|
| `kitehub/.env` | Chứa secrets (passwords, keys) | Kiểm tra nội dung |
| `kiteclass/kiteclass-frontend/.env.local` | Local config | Kiểm tra nội dung |

### Action: Audit .env content, add to .gitignore if needed

---

## 7. Missing Documentation

### Problem: Không có document mô tả Docker architecture

Cần tạo: `documents/02-architecture/docker-platform-architecture.md`
- Mô tả shared infrastructure model
- Service topology
- Network layout
- Volume management
- Canonical docker-compose file references

### Fixed: Tạo architecture document

---

## Summary of Changes

| Category | Items | Impact |
|----------|-------|--------|
| Docker rename | 8 containers + project name | HIGH — scripts cần update |
| Delete outdated | 1 docker-compose, 3 Dockerfiles, 1 empty dir | LOW |
| Delete stale | 3 files | LOW |
| New documentation | 1 architecture doc | MEDIUM |
| Security audit | 2 .env files | HIGH |
