# KiteHub Quality Improvement Plan v2

**Ngày tạo:** 2026-03-23
**Baseline:** 77/100 (Grade C) — audit ngày 2026-03-22
**Mục tiêu:** 90+/100 (Grade A)
**Dựa trên:** Quality Audit Framework (10 categories × 10 điểm)

---

## Gap Analysis

| # | Category | Current | Target | Gap |
|---|----------|---------|--------|-----|
| 1 | E2E Functionality | 3 | 7 | -4 |
| 2 | Security | 7 | 9 | -2 |
| 3 | Backend Tests | 8 | 10 | -2 |
| 4 | Frontend Tests | 10 | 10 | 0 |
| 5 | CI/CD | 9 | 10 | -1 |
| 6 | UI/UX | 9 | 10 | -1 |
| 7 | DevOps/Infrastructure | 5 | 8 | -3 |
| 8 | Documentation | 8 | 10 | -2 |
| 9 | Code Quality | 8 | 10 | -2 |
| 10 | Project Management | 10 | 10 | 0 |
| **Total** | | **77** | **91** | **-14** |

---

## PR Plan

### PR-R1: CI/CD Cleanup (Quick Win)

**Score impact:** CI/CD +1 → 10/10
**Estimate:** 15 phút
**Scope:**
- [ ] Xóa stale branch `feature/fix-kitehub-startup-issues` (đã merged từ lâu)
- [ ] Verify: `git branch -r` không còn stale branches

**Lý do:** Branch tồn đọng sau khi merged, không có value thêm.

---

### PR-R2: Swagger / OpenAPI Documentation

**Score impact:** Documentation +2 → 10/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Thêm `springdoc-openapi-starter-webmvc-ui` vào `kitehub-platform`, `kitehub-admin`, `kitehub-branding`
- [ ] Cấu hình `@OpenAPIDefinition` với title, version, description
- [ ] Add `@Tag` annotation cho mỗi controller
- [ ] Add `@Operation` + `@ApiResponse` cho critical endpoints (auth, subscription, branding)
- [ ] Cấu hình gateway route `/docs/**` → platform swagger
- [ ] Verify: `http://localhost:8080/swagger-ui.html` hiển thị đầy đủ

**Files cần sửa:**
- `kitehub-platform/pom.xml` — thêm dependency
- `kitehub-platform/src/main/resources/application.yml` — springdoc config
- Các `*Controller.java` — add annotations

---

### PR-R3: Security — Input Validation

**Score impact:** Security +2 → 9/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Audit tất cả `@PostMapping`/`@PutMapping` endpoints trong `kitehub-platform`, `kitehub-admin`
- [ ] Add `@Valid` annotation trước `@RequestBody` nơi còn thiếu
- [ ] Add constraint annotations trong DTOs (`@NotBlank`, `@Email`, `@Size`, `@NotNull`)
- [ ] Add `@ControllerAdvice` cho `MethodArgumentNotValidException` → 400 response đẹp
- [ ] Verify: POST với invalid data → 400 Bad Request kèm field errors

**Files cần sửa:**
- Tất cả `*Controller.java` có `@RequestBody`
- Tất cả `*Request.java` / `*Dto.java`
- `GlobalExceptionHandler.java` (nếu chưa có, tạo mới)

---

### PR-R4: DevOps — Monitoring Stack

**Score impact:** DevOps +3 → 8/10
**Estimate:** 1 ngày
**Scope:**
- [ ] Thêm `Prometheus` + `Grafana` vào `docker-compose.kitehub.yml`
- [ ] Add `spring-boot-starter-actuator` + `micrometer-registry-prometheus` vào kitehub services
- [ ] Cấu hình Prometheus scrape config cho tất cả services
- [ ] Tạo Grafana dashboard JSON cơ bản (JVM metrics, HTTP requests, DB pool)
- [ ] Update `scripts/status.sh` để show health of monitoring stack
- [ ] Document: backup strategy trong `documents/02-architecture/backup-strategy.md` (cron + pg_dump)
- [ ] Verify: `http://localhost:3000` Grafana hiển thị metrics

**Files cần sửa/tạo:**
- `kitehub/docker-compose.kitehub.yml` — thêm prometheus, grafana services
- `kitehub/docker/prometheus/prometheus.yml` — scrape config
- `kitehub/docker/grafana/dashboards/kitehub-overview.json`
- Các `pom.xml` — thêm actuator + micrometer
- Các `application.yml` — expose `/actuator/prometheus`

---

### PR-R5: E2E — Docker Startup Reliability

**Score impact:** E2E Functionality +4 → 7/10
**Estimate:** 1 ngày
**Scope:**
- [ ] Fix cold start issue: `test-api-e2e.sh` chờ tất cả services healthy trước khi chạy tests
- [ ] Add `wait-for-healthy.sh` script (poll Docker health checks với timeout 120s)
- [ ] Verify: `./scripts/up.sh && ./scripts/test-api-e2e.sh` pass ngay lần đầu
- [ ] Verify Playwright E2E specs: auth, dashboard, branding flows
- [ ] Document: `QUICK_START.md` — steps để chạy E2E locally

**Files cần sửa:**
- `kitehub/scripts/test-api-e2e.sh` — thêm health wait
- `kitehub/scripts/wait-for-healthy.sh` (tạo mới)
- `kitehub/QUICK_START.md` — update E2E section

---

### PR-R6: Backend Integration Tests

**Score impact:** Backend Tests +2 → 10/10
**Estimate:** 1.5 ngày
**Scope:**
- [ ] Tạo `InstanceProvisioningIT.java` — test full flow: create instance → DB provisioned → status active
- [ ] Tạo `SubscriptionBillingIT.java` — test: subscription create → payment → activate
- [ ] Tạo `BrandingFlowIT.java` — test: upload config → queue job → theme generated
- [ ] Dùng `@SpringBootTest` + Testcontainers (PostgreSQL) thay vì H2
- [ ] Add `testcontainers` dependency (đã có trong project hoặc cần thêm)
- [ ] Verify: `mvnw verify -pl kitehub-platform` chạy cả unit + integration tests

**Files cần tạo:**
- `kitehub-platform/src/test/java/.../integration/InstanceProvisioningIT.java`
- `kitehub-platform/src/test/java/.../integration/SubscriptionBillingIT.java`
- `kitehub-branding/src/test/java/.../integration/BrandingFlowIT.java`

---

### PR-R7: Code Quality — TypeScript + UI Polish (Optional)

**Score impact:** Code Quality +1, UI/UX +1 → 10/10 each
**Estimate:** 1 ngày
**Scope:**
- [ ] Fix TypeScript IDE warnings trong `kiteclass-frontend` (`.next/types` issues)
- [ ] Verify: `pnpm type-check` và VS Code không còn red squiggles
- [ ] Kiểm tra accessibility: thêm `aria-label` vào các interactive elements thiếu
- [ ] Verify: Chrome DevTools → Lighthouse accessibility score > 90

---

## Execution Order

```
Phase 1 — Quick Wins (1 ngày):
  PR-R1 (stale branch) ──→ PR-R2 (swagger) ──→ PR-R3 (validation)
                                                        ↓
Phase 2 — Infrastructure (2 ngày):
  PR-R4 (monitoring) ──→ PR-R5 (E2E reliability)
                                ↓
Phase 3 — Tests + Polish (2-3 ngày):
  PR-R6 (integration tests) ──→ PR-R7 (code quality, optional)
```

---

## Score Projection

| Sau PR | Score | Grade | Tăng |
|--------|-------|-------|------|
| Baseline | 77 | C | — |
| PR-R1 (stale branch) | 78 | C | +1 |
| PR-R2 (swagger) | 80 | B | +2 |
| PR-R3 (validation) | 82 | B | +2 |
| PR-R4 (monitoring) | 85 | B | +3 |
| PR-R5 (E2E) | 89 | B+ | +4 |
| PR-R6 (integration tests) | 91 | A | +2 |
| PR-R7 (code quality) | **93** | **A** | +2 |

---

## Estimate tổng

| Phase | PRs | Days |
|-------|-----|------|
| Phase 1 | R1, R2, R3 | 1 ngày |
| Phase 2 | R4, R5 | 2 ngày |
| Phase 3 | R6, R7 | 2 ngày |
| **Total** | **7 PRs** | **~5 ngày** |

---

## Completion Status

| PR | Status | GitHub | Score |
|----|--------|--------|-------|
| PR-R1 Stale branch cleanup | ✅ DONE | direct | +1 |
| PR-R2 Swagger/OpenAPI | ✅ DONE | #186 | +2 |
| PR-R3 Input Validation | ✅ DONE | #187 | +2 |
| PR-R4 Monitoring Stack | ✅ DONE | #188 | +3 |
| PR-R5 E2E Reliability | ✅ DONE | #189 | +4 |
| PR-R6 Integration Tests | ✅ DONE | #190 | +2 |
| PR-R7 Code Quality Polish | ✅ DONE | #191 | +2 |
| **Total** | **7/7** | | **16/16** |
