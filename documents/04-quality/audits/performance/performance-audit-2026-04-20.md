# Performance Audit — Refresh 2026-04-20

**Date:** 2026-04-20
**Auditor:** Claude (autonomous, isolated worktree)
**Skill:** `.claude/skills/quality/performance-audit/SKILL.md`
**Scope:** Delta re-audit sau Part B perf batch (PR #375 + follow-up #377). Static analysis only.
**Type:** **Refresh** — baseline 2026-04-19 = 58/100 F (PR #364)

**Final score: 64/100 — Grade D (Performance risks — address before scale)**

**Delta vs 58/100 baseline: +6 điểm (F → D)** — đúng hướng nhưng chưa đạt target +8/+15 mà prompt kỳ vọng, do 4 gaps closed tập trung vào 2 sections (DB + API) trong khi 2 sections khác (Frontend Bundle, Caching) không có thay đổi.

---

## Executive Summary

Part B perf batch (PR #375) đóng 4 gaps P0/P1 tập trung vào DB query + HTTP resilience:

- **GAP-128 (P0)** — `InstallmentPlan.findAll().stream().filter(...)` full-table scan + nested N+1 đã được thay bằng PK lookup trên `InstallmentRepository` (V12 `idx_installments_plan` index). Regression test (`InstallmentPlanServiceTest`) assert `installmentPlanRepository.findAll()` NEVER called. **CLOSED.**
- **GAP-129 (P0, multi-tenancy + perf)** — `BrandingPackage` service ignore tham số `instanceId` và gọi `findAll()` across ALL tenants. Fix: method mới `findByInstanceIdAndDeletedFalse(UUID)` + composite index V45 `(instance_id, deleted)` + `@Index` trên entity. Test `BrandingPackageServiceImplTest` dùng `verifyNoMoreInteractions` bảo đảm chỉ tenant A's UUID được query. **CLOSED.**
- **GAP-131 (P1, partial 6/9)** — 6 trong 9 HTTP client sites có timeout rõ ràng:
  - `kitehub-subscription/RestTemplateConfig` (connect 5s / read 30s) qua `RestTemplateBuilder.connectTimeout(...)`/`readTimeout(...)` (non-deprecated API sau PR #377). Phủ `EmailServiceClient`, `CaptchaService`, `VietQRService`, `EmailConsumer`, và `EmailSenderService` (trước kia bypass bằng `new RestTemplate()` field → giờ inject bean).
  - `kiteclass-gateway/CoreServiceClient`, `kitehub-gateway/BrandingClient`, `kitehub-email/BrandingClient`, `kitehub-branding/OllamaClient` — Netty `HttpClient` với `CONNECT_TIMEOUT_MILLIS=5000` + `responseTimeout(...)`.
  - 3 sites còn lại (payment, email SMTP thêm, captcha) → GAP-146 (P2, deferred cho next sprint, cần Resilience4j wrapper + WireMock integration test + ArchUnit lint). **PARTIAL CLOSED 6/9.**
- **GAP-133 (P1)** — `spring.jpa.properties.hibernate.jdbc.batch_size: 50` + `batch_versioned_data: true` + `order_inserts: true` + `order_updates: true` thêm vào 5 application.yml (kiteclass-core + kitehub-subscription/admin/branding/gateway). `HibernateBatchConfigTest` load production yml trực tiếp → regression phát hiện tại unit-test time. **CLOSED.**

**Gap NOT addressed trong batch này (vẫn UNCHANGED):**
- GAP-126 Admin dashboard `findAll() × 2` (P0)
- GAP-127 Frontend code-splitting (P0)
- GAP-130 Docker resource limits (P0)
- GAP-132 `@EnableCaching` missing 3 services (P1)
- GAP-134 `@EntityGraph`/JOIN FETCH chỉ 1 occurrence (P1)
- GAP-135 No p95 SLO documented (P2)

**PR #374 (Helm Prometheus/Alertmanager foundation)** — marginal perf impact (observability infrastructure, không phải optimization); sẽ phản ánh nhiều hơn trong ops-readiness audit.

---

## Category Scores

| # | Category | Trước (04-19) | Nay (04-20) | Δ | Grade |
|---|----------|:-------------:|:-----------:|:-:|:-----:|
| 1 | DB Query Efficiency | 11/20 | **14/20** | **+3** | C |
| 2 | API Response Time | 13/20 | **15/20** | **+2** | C |
| 3 | Frontend Bundle | 8/20 | 8/20 | 0 | F |
| 4 | Caching Strategy | 13/20 | 13/20 | 0 | C- |
| 5 | Resource Utilization | 13/20 | **14/20** | **+1** | C |
| **TOTAL** | | **58/100 F** | **64/100 D** | **+6** | **D** |

---

## 1. Database Query Efficiency — 14/20 (+3)

### Cải thiện sau PR #375

**Unbounded reads đã đóng (2/8 sites trong baseline):**

| File | Trước | Sau |
|------|-------|-----|
| `InstallmentPlanServiceImpl.java:147` | `findAll().stream().filter(plan.installments.stream.anyMatch(id))` — O(plans × installments) | `installmentRepository.findById(id).getPlan()` — PK index lookup qua `idx_installments_plan` |
| `BrandingPackageServiceImpl.java:36` | `resourceRepository.findAll()` — cross-tenant scan (còn là multi-tenancy leak) | `findByInstanceIdAndDeletedFalse(UUID)` qua composite index V45 |

**Index footprint tăng:**
- 167 explicit Flyway indexes → **168** (V45 thêm `idx_branding_resources_instance_deleted` composite).
- 164 `@Index` JPA annotations → vẫn **164** (composite đã declare trong entity `BrandingResource` thay vì single `@Index(name="idx_branding_resource_type", columnList="instance_id, type")` — PR khéo léo giữ nguyên cũ + thêm mới).

**Hibernate tuning đã xử lý:**
- `spring.jpa.properties.hibernate.jdbc.batch_size: 50` + `batch_versioned_data: true` + `order_inserts: true` + `order_updates: true` trên 5 services.
- `HibernateBatchConfigTest` trên kiteclass-core parse application.yml production → assert 4 giá trị → regression fails at build time. Đây là high-quality test (không mock config, đọc file thật).

### Residual risks (UNCHANGED)

| Rủi ro | Severity | Gap |
|--------|:--------:|-----|
| `AnalyticsService.java:46,47,116` + `AdminController.java:75,174` — admin dashboard `findAll() × 2` Instance + Subscription every hit | 🔴 P0 | GAP-126 |
| `InstanceController.java:70` — `repository.findAll()` khi no status filter | 🟠 P1 | (trong GAP-126 scope) |
| `PaymentService.java:121` — `getAllPayments(null)` no pagination | 🟠 P1 | (Wave 2 pagination work) |
| `InstanceService.java:337` — `listAllInstances()` no pageable | 🟠 P1 | (Wave 2 pagination work) |
| `AssetUrlsQualityCheck.java:35` — quality-check path load all branding resources | 🟡 P2 | (non-user-facing, acceptable) |
| Chỉ 1 `@EntityGraph`/`JOIN FETCH` trong toàn bộ main code (`ParentStudentLinkRepository`) | 🟠 P1 | GAP-134 |

### Scoring rationale

Từ 11/20 (rubric: "Multiple N+1 patterns, some unbounded queries") lên **14/20** (rubric between "1-2 N+1 patterns trong non-critical paths, pagination exists" = 12 và "No N+1, most queries paginated" = 16). Chưa đạt 16 vì:
- Admin dashboard path vẫn full scan (cost-sensitive).
- JOIN FETCH coverage chưa tăng — mọi `OneToMany` vẫn lazy theo mặc định.
- 4 `findAll()` sites trong production paths còn lại.

Lên được 14 vì 2 P0 critical paths đóng + Hibernate batch config nghiêm túc + composite index mới + regression tests mới.

---

## 2. API Response Time — 15/20 (+2)

### Cải thiện

**HTTP timeout coverage:**
- Trước: 5/14 sites có timeout. Sau: **11/14** (≈79%). 3 sites còn lại tracked rõ ràng trong GAP-146.
- `CoreServiceClient` (kiteclass-gateway → core) là hot cross-service hop; có timeout = latency budget có biên.
- `OllamaClient` đã có timeout trước, nhưng PR #375 standardize thêm `connect 5s + responseTimeout(timeoutSeconds+5)` pattern.
- Regression tests:
  - `RestTemplateConfigTest` dùng reflection trên `JdkClientHttpRequestFactory` → xác nhận `connectTimeout`/`readTimeout` bound.
  - `CoreServiceClientTimeoutTest` — `ReactorClientHttpConnector` timeout verification.

**PR #377 cleanup:**
- `setConnectTimeout(Duration)` / `setReadTimeout(Duration)` deprecated trong Spring Boot 3.4+ → thay bằng `connectTimeout(Duration)` / `readTimeout(Duration)`. Cùng hành vi, non-deprecated API. Discovered qua IDE warnings check (feedback rule từ memory).

### Residual risks (UNCHANGED)

- **Admin dashboard handler** vẫn đồng bộ gọi `findAll() × 2` + groupBy stream trong HTTP request — P1 latency bomb khi >10k instances (GAP-126).
- **No server-side timeout** (`spring.mvc.async.request-timeout` không set) — slow DB query vẫn holds Tomcat thread vô hạn.
- **No p95 API latency budget** documented (GAP-135) — regressions invisible.
- **3 HTTP sites remainder** (payment gateway, email SMTP sender thêm, captcha) → GAP-146. Payment đặc biệt cần Resilience4j + idempotency key pattern, không chỉ timeout.

### Scoring rationale

Từ 13/20 lên **15/20** (between "Most endpoints fast, 1-2 slow identified" = 12 và "All endpoints <500ms p95, basic load test done" = 16). Chưa đạt 16 vì:
- Không có load test để confirm endpoint p95.
- Admin dashboard query vẫn unbounded sync.
- p95 budget chưa có.

Lên được 15 vì HTTP timeout coverage đã 79% + hot cross-service path (CoreServiceClient) có timeout + tests regression tự động.

---

## 3. Frontend Bundle — 8/20 (0)

**UNCHANGED.** Không có FE changes liên quan bundle optimization trong Part B batch.

- Grep `next/dynamic` → 0 hits cả hai FE projects (xác nhận lại).
- `next.config.js` vẫn minimal: `output: 'standalone'` + `images.remotePatterns`. Không `bundle-analyzer`, không `modularizeImports`, không `optimizePackageImports`.
- PR #372 (GAP-136 — KiteHub not-found/error pages) thêm 3 pages + 3 tests → bundle tăng nhẹ (negligible, stub pages).

**GAP-127** (Frontend code-splitting + bundle analyzer, P0) vẫn OPEN và là blocker lớn nhất cho điểm category này.

---

## 4. Caching Strategy — 13/20 (0)

**UNCHANGED.** Không có thay đổi caching trong Part B batch.

- `@EnableCaching` declarations: 3 (kiteclass-core CacheConfig, kitehub-email BrandingCacheConfig, kitehub-gateway GatewayBrandingCacheConfig). **kitehub-subscription / kitehub-admin / kitehub-platform vẫn thiếu** — GAP-132 UNCHANGED.
- GAP-043 (cache stampede) vẫn OPEN.
- Không có TTL differentiation per-cache (vẫn 1h default).
- Không có cross-service cache invalidation event consumer.

---

## 5. Resource Utilization — 14/20 (+1)

### Cải thiện

- **Hibernate `jdbc.batch_size=50`** áp dụng cho 5 services — bulk import (Wave 1 GAP-051) giờ batch 50 INSERTs/flush thay vì 1-per-row. Impact đặc biệt rõ trong `studentService.createStudent` loop của bulk-import feature.
- **`order_inserts` + `order_updates`** — Hibernate sort statements by entity type trước khi batch, tối ưu statement cache reuse.

### Residual risks (UNCHANGED)

- **Zero Docker `deploy.resources.limits`** trong `kitehub/docker-compose.kitehub.yml` (GAP-130 P0).
- **Không có RabbitMQ `prefetch-count`** setting (grep empty).
- **Không có JVM `-Xmx` hard ceiling** — vẫn dựa vào `MaxRAMPercentage=75.0` với container không có memory limit.
- **Connection pool chưa differentiated** giữa services (kitehub-subscription vẫn default pool=10).

### PR #374 side-effect

PR #374 thêm Helm Prometheus/Alertmanager foundation với ServiceMonitor/PrometheusRule stubs. Không phải direct resource tuning, nhưng wire up observability cho latency/error alerts (7 alerts mirror từ `kitehub/docker/prometheus/alert-rules.yml`). Marginal contribution đến "resource visibility" — không scoring boost vì chưa deploy.

### Scoring rationale

Từ 13/20 lên **14/20** (between "Basic pool config, memory limits in Docker/k8s" = 12 và "Connection pools sized, thread pools configured, memory limits set, health probes correct" = 16). Không đạt 16 vì Docker limits vẫn zero.

Lên được 14 vì Hibernate batch tuning có regression test cứng (`HibernateBatchConfigTest` parse yml thật) + order_inserts/updates.

---

## Baseline Top-10 Tracker

| # | Risk baseline 04-19 | Gap | Trạng thái 04-20 |
|---|---------------------|-----|:----------------:|
| 1 | Admin dashboard `findAll() × 2` | GAP-126 | **UNCHANGED** |
| 2 | FE 0 code-splitting, bundles >300KB | GAP-127 | **UNCHANGED** |
| 3 | InstallmentPlan full-table scan | GAP-128 | **CLOSED** |
| 4 | BrandingPackage cross-tenant findAll() | GAP-129 | **CLOSED** |
| 5 | Docker resource limits zero | GAP-130 | **UNCHANGED** |
| 6 | 9 HTTP timeouts missing | GAP-131 | **PARTIAL 6/9** (remainder GAP-146) |
| 7 | `@EnableCaching` missing 3 services | GAP-132 | **UNCHANGED** |
| 8 | Hibernate `jdbc.batch_size` unset | GAP-133 | **CLOSED** |
| 9 | JOIN FETCH/@EntityGraph absent | GAP-134 | **UNCHANGED** |
| 10 | No p95 API SLO documented | GAP-135 | **UNCHANGED** |

**Closed: 3/10. Partial: 1/10. Unchanged: 6/10.**

**New findings (2026-04-20):** KHÔNG có new perf gap nào phát sinh từ Part B batch (clean PR — không regression).

---

## Residual Risks Ordered (post-04-20)

1. 🔴 **GAP-126 P0** — Admin dashboard `findAll() × 2` synchronous. Cost-sensitive khi tăng scale. Next sprint phải materialize-view hoặc scheduled aggregation.
2. 🔴 **GAP-127 P0** — Frontend 0 code-splitting. 64 pages, bundles projected >300KB. Blocker nghiêm trọng cho điểm cat 3.
3. 🔴 **GAP-130 P0** — Docker resource limits zero. Một leak = host OOM.
4. 🟠 **GAP-043 P1** — Cache stampede (already open). Khi branding cache flush under load → 1000 concurrent DB hits.
5. 🟠 **GAP-132 P1** — 3 services không `@EnableCaching` → silent no-op risk.
6. 🟠 **GAP-134 P1** — Chỉ 1 JOIN FETCH trong toàn repo. Mọi list endpoint chạm collection = N+1.
7. 🟠 **GAP-146 P2** — 3 HTTP sites remainder cần Resilience4j + idempotency.
8. 🟡 **GAP-135 P2** — Không có p95 budget → regressions invisible.

**Top 3 residual risks (ranked bởi blast radius × business impact):**

1. **GAP-127 (FE code-splitting)** — mỗi page load tốn bandwidth + TTI; ảnh hưởng 100% user-facing traffic.
2. **GAP-126 (Admin dashboard)** — scale blocker; khi multi-tenant lên 1000+ instances sẽ OOM Spring handler.
3. **GAP-134 (JOIN FETCH)** — silent N+1 trên mọi list endpoint touching lazy collection; hidden latency.

---

## Next Perf Wave Recommendation

### Sprint perf-02 (1-2 tuần, mechanical)

1. **GAP-132** — thêm `@EnableCaching` + `spring.cache.type: redis` vào kitehub-subscription / admin / platform. Mechanical, ít risk.
2. **GAP-146** — payment/email/captcha timeouts + Resilience4j cho payment.
3. **GAP-134** — pick top-5 `@Transactional` sites with collection access → `@EntityGraph(attributePaths=...)`. Mechanical một file mỗi site.
4. **GAP-135** — define p95 budgets (list <500ms, detail <200ms, write <800ms) + Prometheus alert. PR #374 Helm foundation đã có PrometheusRule stub → wire alert vào.

**Expected delta:** +5 → +8 (target 69-72, grade C-).

### Sprint perf-03 (2-3 tuần, design-heavy)

1. **GAP-126** — admin dashboard: cache aggregate hoặc materialized view scheduled nightly. Cần design (live data vs cached trade-off).
2. **GAP-043** — branding cache stampede: request coalescing với Redis `SET NX` lock hoặc Caffeine single-flight.
3. **GAP-127** — FE: `@next/bundle-analyzer` + `modularizeImports` cho lucide-react/date-fns + `optimizePackageImports` cho Radix + convert top 5-10 pages to `dynamic()` + `images.formats: ['avif','webp']`. Đo actual bundle trước/sau.

**Expected delta:** +8 → +12 (target 77-84, grade B-).

### Sprint perf-04 (ops-heavy)

1. **GAP-130** — Docker limits per-service trong `kitehub/docker-compose.kitehub.yml` + Helm values.yaml.
2. JVM `-Xmx` hard ceiling trong Dockerfile.
3. RabbitMQ `prefetch-count` per consumer.
4. `spring.mvc.async.request-timeout` global.

**Expected delta:** +5 → +8 (target 82-92, grade B+/A-).

---

## Out of scope (not performed)

- Live load testing (prompt constraint)
- Profiling real production data
- Running `next build` để đo actual bundle sizes
- Fixing any issue trong audit này (audit-to-gap-pipeline.md §6 — audit chỉ tạo gap, không fix)
- Modifying `ROADMAP.md`, `.claude/rules/*`, MEMORY.md (prompt constraint)

---

## New Gaps Created

**KHÔNG new gap** — Part B batch clean, không regression. 3 HTTP sites remainder đã được tracked trong GAP-146 (tạo bởi PR #376). Pre-existing kitehub-admin bean conflict tracked trong GAP-147.

Không sử dụng GAP-152 → GAP-155 range vì không có new issue nào cần file.

---

## Delta Summary

- **Score:** 58/100 F → **64/100 D** (+6)
- **Gaps closed fully:** 3 (GAP-128, GAP-129, GAP-133)
- **Gaps partial closed:** 1 (GAP-131: 6/9 sites; remainder GAP-146)
- **Gaps unchanged:** 6 (GAP-126, -127, -130, -132, -134, -135)
- **New gaps:** 0 (zero regression)
- **Next milestone target:** 69-72 (C-) after Sprint perf-02 (mechanical wins on GAP-132, -134, -135, -146)
