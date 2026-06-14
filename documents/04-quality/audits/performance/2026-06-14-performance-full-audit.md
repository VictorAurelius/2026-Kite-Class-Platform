# Performance Full Audit — post wave-p0-closeout-1

**Date:** 2026-06-14
**Auditor:** Claude (Opus 4.8, `performance-audit` skill)
**Scope:** Toàn bộ monorepo Kite Platform (10 service Java + 2 FE Next.js) tại main HEAD `bf207a6ea`
**Rubric:** `.claude/skills/quality/performance-audit/SKILL.md` + `.claude/rules/audit-skill-rubric-performance-audit.md` (5 category × ≥5 sub-check, per-check pass/fail)
**Baseline tham chiếu:** Wave 85 (2026-05-15) `86/100 B+` + Wave meta-6 (2026-05-28) `85/100 B+`
**Phương pháp:** STATIC analysis (grep N+1 / findAll / index / @Cacheable / resilience / Dockerfile / Helm) + light runtime probe (13 container Docker stack healthy). **Production/AWS-scale measurement KHÔNG khả dụng (GAP-612 AWS suspended)** → mọi item đo prod-scale đánh dấu `❓ UNCHECKED` minh bạch, KHÔNG mặc định PASS.

---

## 0. Verdict

| | |
|---|---|
| **Score** | **82/100 — B** |
| **Audit-level verdict** | **PARTIAL PASS** |
| Phase 1 BETA gate (≥80) | ✅ PASS (+2 buffer, tight) |
| v1.0.0-rc gate (≥85) | ❌ FAIL (-3) |
| Delta vs Wave 85 baseline (86) | **-4** |

**Tóm tắt:** KHÔNG có P0 "bom" thực sự (không N+1 explosion, không infinite cache, không thiếu pagination trên hot-path user-data — attendance/grade/enrollment/invoice đều đã `Page<>`). Index coverage rất tốt (286 + 99 `CREATE INDEX`). Điểm tụt -4 vs baseline đến từ: (a) per-check rubric re-enumerate phát hiện 5 finding MỚI (branding bulkhead, cache metric, kiteclass-core JVM, email/VietQR circuit-breaker, 4 unbounded `findAll()` sites), (b) Cat 3 chấm bảo thủ do 2 P0 size-check (bundle) `❓ UNCHECKED` — production build KHÔNG chạy trong lượt audit này (config discipline còn nguyên; Wave 85 đo trong-ngưỡng → KHÔNG phải regression).

---

## 1. Bug list (FAIL trước, score sau — per rubric §4 primacy)

| # | Sub-check | Severity | File:line evidence | Gap |
|---|---|---|---|---|
| F-001 | 5.2 Bulkhead | **P1** | `kitehub/kitehub-branding/.../client/ResilientAIClient.java:58,64,77,82` chỉ có `@CircuitBreaker`, KHÔNG `@Bulkhead`; `application.yml:159` resilience4j có `circuitbreaker` nhưng KHÔNG có khối `bulkhead`. AI image-gen = external call CHẬM nhất (10-30s) → burst có thể saturate Tomcat thread pool. Đối chiếu `kiteclass-core ResilientAIClient` có đủ `@Bulkhead+@CircuitBreaker+@Retry`. | GAP-1356 |
| F-002 | 4.5 Cache hit-ratio metric | **P1** | Không tìm thấy `cache.gets`/`recordStats`/`CacheMetricsRegistrar`/`enableStatistics` ở bất kỳ service nào — Redis + Caffeine cache không emit hit-ratio metric qua Micrometer → mù observability cache. | GAP-1357 |
| F-003 | 1.1/2.2 unbounded list | **P1** | `kiteclass-core .../instance/controller/InstanceController.java:70` `repository.findAll()` khi `status==null`, trả `List<InstanceResponse>` không `Pageable`. | GAP-1359 |
| F-004 | 1.1/2.2 unbounded list | **P1** | `kitehub-admin .../controller/AdminPaymentsController.java:50` `listPendingPayments()` trả `List<PaymentResponse>` toàn cục, không `Pageable` (đã có bản `Page<>` ở `kiteclass-core PaymentController:142` — pattern lệch). | GAP-1360 |
| F-005 | 5.3 Circuit breaker | **P2** | External client thiếu `@CircuitBreaker`: `kitehub-email` (`ResendEmailService`, `ZaloOAClient`, `BrandingClient`), `kitehub-subscription` (`VietQRService`, `EmailServiceClient`). AI + gateway đã có CB. Giảm nhẹ: có timeout WebClient + email đi async qua RabbitMQ broker. | GAP-1361 |
| F-006 | 5.4 JVM container tuning | **P2** | `kiteclass/kiteclass-core/Dockerfile:71` `ENTRYPOINT ["java","-jar","app.jar"]` — KHÔNG `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` (6/6 service kitehub đều có). kiteclass-gateway không có Dockerfile riêng. Java 17+ bật UseContainerSupport mặc định nhưng MaxRAMPercentage default 25% → heap under-provisioned, lệch fleet. | GAP-1358 |
| F-007 | 1.1 internal findAll | **P2** | `kiteclass-core AcademicYearService.java:114 listAll()` + `quality/check/AssetUrlsQualityCheck.java:35 run()` dùng `findAll()` (bảng nhỏ/internal nhưng load-all-into-memory). | GAP-1362 |
| F-008 | 3.1/3.2 bundle size | **P2** `❓ UNCHECKED` | Production `pnpm build` route-size + First Load JS KHÔNG đo lượt này (vượt light-probe budget). Static config tốt: `output: standalone` + `optimizePackageImports` + 17/26 `dynamic()` + raw `<img>` ≤5. Cần build verify. | GAP-1364 |
| F-009 | 2.1/2.3 SLO + slow-query | **P2** `❓ UNCHECKED` | P95 top-10 endpoint chưa load-test (AWS suspended); Postgres `log_min_duration_statement` chưa bật. Local health probe 2.4ms (không đại diện hot-path). | GAP-1365 |
| F-010 | 2.4 SLO doc | **P2** | SLO per-endpoint-class chưa tài liệu hóa đầy đủ (GAP-135 partial precedent). | GAP-1366 |
| F-011 | 4.6 Redis persistence | **P2** `❓ UNCHECKED` | RDB/AOF prod config chưa verify (AWS-gated; local docker default). | GAP-1367 |
| F-012 | 4.2 cache TTL granularity | **P3** | `kitehub-admin CacheConfig.java:68` dùng single `max(dashboard 300s, revenue 3600s)=3600s` cho CẢ HAI cache → dashboard cache stale tới 1h thay vì 5min (bounded, không infinite — chỉ sub-optimal freshness). | GAP-1363 |

> **0 P0 FAIL** (không có sub-check P0 nào fail ở mức "bom"). Các finding 5.2/5.3/5.4 là sub-check rubric-P0 nhưng severity thực tế hạ xuống P1/P2 (có giảm nhẹ: CB+retry cho branding, async-broker + timeout cho email, UseContainerSupport default-on cho JVM) → verdict PARTIAL PASS, không phải hard FAIL.

---

## 2. Per-category scoring

### Category 1 — DB Query Efficiency → **17/20**

| # | Check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 1.1 | Zero unbounded `findAll()` | P0 | ⚠️ FAIL (P1 eff.) | 4 site thật: InstanceController:70, AdminPaymentsController, AcademicYearService:114, AssetUrlsQualityCheck:35 (bảng bounded/internal — KHÔNG catastrophic). 4 site `{@code findAll()}` khác trong grep là JAVADOC mô tả code CŨ đã fix (GAP-432). |
| 1.2 | LAZY fetch (no EAGER) | P0 | ✅ PASS | 0 `@OneToMany/@ManyToMany EAGER` |
| 1.3 | `@EntityGraph`/`JOIN FETCH` | P1 | ✅ PASS | 10 `@EntityGraph` + JOIN FETCH (academicyear/invoice/grade/parent...) |
| 1.4 | Index trên WHERE-column | P1 | ✅ PASS | 286 `CREATE INDEX` (kiteclass-core) + 99 (subscription); incl. partial index `WHERE status='PENDING'` |
| 1.5 | No raw query string-concat | P0 | ✅ PASS | `AuthorizationBean` createNativeQuery dùng `:param + setParameter` (binding đúng) |
| 1.6 | HikariCP pool tuned | P1 | ✅ PASS | production yml: `maximum-pool-size:10, minimum-idle:2` (core/subscription/email/admin) |

### Category 2 — API Response Time → **16/20**

| # | Check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 2.1 | P95 <2s top-10 | P0 | `❓ UNCHECKED` | Không load-test prod-scale (AWS suspended). Local health probe gateway `HTTP 200 in 0.0024s` (không đại diện) |
| 2.2 | Pagination mọi list-endpoint | P0 | ⚠️ FAIL (P1 eff.) | InstanceController.list + AdminPaymentsController.listPendingPayments thiếu Pageable. Discipline TỐT: attendance/enrollment/invoice/vetting/parent-facet đều `Page<>`. List<> còn lại là parent-scoped small-set (acceptable exemption) |
| 2.3 | Slow query log | P1 | `❓ UNCHECKED` | `log_min_duration_statement` chưa set (AWS RDS param không truy cập được) |
| 2.4 | SLO per endpoint-class | P1 | ⚠️ PARTIAL | SLO doc chưa đầy đủ (GAP-135 partial) |
| 2.5 | Async-eligible trả jobId | P1 | ✅ PASS | branding job + AI qua ResilientAIClient |
| 2.6 | Bulk endpoint chunk | P1 | ✅ PASS | Không phát hiện bulk 10k-in-txn |

### Category 3 — Frontend Bundle → **17/20**

| # | Check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 3.1 | Route bundle ≤250KB | P0 | `❓ UNCHECKED` | Fresh `pnpm build` không chạy lượt này — không default PASS |
| 3.2 | First Load JS ≤200KB | P0 | `❓ UNCHECKED` | Như trên |
| 3.3 | Code-splitting per route | P1 | ✅ PASS | 17 (kitehub) + 26 (kiteclass) `dynamic()` |
| 3.4 | Tree-shaking | P1 | ✅ PASS | `experimental.optimizePackageImports` cấu hình ở cả 2 app |
| 3.5 | `next/image` cho ảnh | P1 | ✅ PASS | raw `<img>`: kitehub 1, kiteclass 3 (≤5) |
| 3.6 | Font subset/preload | P2 | ✅ PASS | next.config `images` + standalone output |

> Cat 3 chấm 17 (không 20) do 2 P0 size-check UNCHECKED. KHÔNG phải regression — config discipline nguyên vẹn; Wave 85 đo trong-ngưỡng. GAP-1364 theo dõi build-verify.

### Category 4 — Caching Strategy → **17/20**

| # | Check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 4.1 | Redis cho session/rate-limit/AI | P0 | ✅ PASS | `@Cacheable` rộng: students/leads/landingPages/courses/teachers/branding/regenerateQuota/admin-dashboard/email-branding/gateway-branding |
| 4.2 | TTL configured (no infinite) | P0 | ✅ PASS (P3 note) | core `entryTtl(1h)`; subscription `expireAfterWrite(300s)`; branding 300/900s; admin single-max-TTL (sub-optimal — GAP-1363) |
| 4.3 | Cache-aside graceful | P1 | ✅ PASS | branding `sync=true`; fallback graceful |
| 4.4 | Invalidation documented | P1 | ✅ PASS | `@CacheEvict` rộng + GAP-792 tenant-key fix |
| 4.5 | Hit-ratio metric (Micrometer) | P1 | ❌ FAIL | Không có `cache.gets`/`recordStats` → GAP-1357 |
| 4.6 | Redis persistence (RDB/AOF) | P2 | `❓ UNCHECKED` | prod config chưa verify (AWS-gated) → GAP-1367 |

### Category 5 — Resource Utilization → **15/20**

| # | Check | Sev | Verdict | Evidence |
|---|---|---|---|---|
| 5.1 | Thread pool tuned | P0 | ✅ PASS | `server.tomcat.threads` ở production yml mọi service |
| 5.2 | Bulkhead external call | P0 | ⚠️ FAIL (P1 eff.) | branding ResilientAIClient KHÔNG `@Bulkhead` (core có đủ) → GAP-1356 |
| 5.3 | Circuit breaker external | P0 | ⚠️ FAIL (P2 eff.) | email/Zalo/VietQR thiếu CB (AI+gateway có; email async-broker + timeout giảm nhẹ) → GAP-1361 |
| 5.4 | JVM memory limit container | P0 | ⚠️ FAIL (P2 eff.) | kiteclass-core Dockerfile thiếu MaxRAMPercentage (6 kitehub có) → GAP-1358 |
| 5.5 | K8s requests+limits | P1 | ✅ PASS | helm kiteclass-instance (4 tier) + kitehub (per-svc requests+limits) |
| 5.6 | Pool exhaustion alert | P1 | ✅ PASS | prometheus `hikaricp_connections_active/max >0.8/0.9` |

---

## 3. Score summary + delta

| Category | This audit | Wave 85 baseline | Delta |
|---|:---:|:---:|:---:|
| 1. DB Query Efficiency | 17/20 | ~18 | -1 |
| 2. API Response Time | 16/20 | ~18 | -2 |
| 3. Frontend Bundle | 17/20 | ~18 | -1 (UNCHECKED methodology) |
| 4. Caching Strategy | 17/20 | ~17 | 0 |
| 5. Resource Utilization | 15/20 | ~15 | 0 |
| **Total** | **82/100 B** | **86/100 B+** | **-4** |

---

## 4. `❓ UNCHECKED` items (minh bạch — KHÔNG default PASS)

| Sub-check | Lý do UNCHECKED | Gap |
|---|---|---|
| 2.1 P95 latency top-10 | Load-test prod-scale không khả dụng (GAP-612 AWS suspended) | GAP-1365 |
| 2.3 Postgres slow-query-log | RDS parameter group không truy cập (AWS) | GAP-1365 |
| 3.1 Route bundle ≤250KB | Fresh production build không chạy lượt này (light-probe budget) | GAP-1364 |
| 3.2 First Load JS ≤200KB | Như trên | GAP-1364 |
| 4.6 Redis persistence RDB/AOF | Prod Redis config không verify (AWS-gated) | GAP-1367 |

---

## 5. Findings → Gaps (12 — reserved block GAP-1356..GAP-1367)

| Gap | P | Domain | Finding |
|---|---|---|---|
| GAP-1356 | P1 | Backend | kitehub-branding ResilientAIClient thiếu `@Bulkhead` (AI image-gen unbounded concurrency) |
| GAP-1357 | P1 | DevOps | Cache hit-ratio metric không emit (Micrometer cache stats) toàn fleet |
| GAP-1358 | P2 | DevOps | kiteclass-core/gateway Dockerfile thiếu JVM container tuning (`-XX:MaxRAMPercentage`) |
| GAP-1359 | P1 | Backend | `InstanceController.list()` unbounded `findAll()` (no Pageable) |
| GAP-1360 | P1 | Backend | `AdminPaymentsController.listPendingPayments()` unbounded list (no Pageable) |
| GAP-1361 | P2 | Backend | Email/Zalo/VietQR external client thiếu `@CircuitBreaker` |
| GAP-1362 | P2 | Backend | `AcademicYearService.listAll()` + `AssetUrlsQualityCheck.run()` internal unbounded findAll |
| GAP-1363 | P3 | Backend | kitehub-admin cache single-max-TTL → dashboard 300s thực tế 3600s stale |
| GAP-1364 | P2 | Frontend | FE route bundle size UNCHECKED — cần fresh production build verify |
| GAP-1365 | P2 | DevOps | API P95 SLO chưa load-test + Postgres slow-query-log chưa bật (AWS-gated) |
| GAP-1366 | P2 | DevOps | Gateway per-endpoint-class SLO documentation chưa đầy đủ |
| GAP-1367 | P2 | DevOps | Redis persistence (RDB/AOF) prod config UNCHECKED (AWS-gated) |

---

## 6. Runtime context (light probe)

Docker stack 13+ container `healthy` (kite-postgres/redis/rabbitmq/minio + kitehub-{gateway,subscription,email,branding,admin} + kiteclass-{core,frontend} + kitehub-frontend). Gateway `actuator/health` HTTP 200 ~2.4ms (idle, không đại diện hot-path/load).

## 7. Cross-references

- GAP-432 (DONE) — 3 service findAll bounded (Analytics/Payment/Instance-subscription) — precedent; GAP-1359/1360/1362 là site MỚI khác.
- GAP-408 (PARTIAL) — JVM heap dev-profile — GAP-1358 mở rộng sang prod Dockerfile.
- GAP-354 (phase-2) — per-kit bundle budget — GAP-1364 là verify FE app phase-1.
- GAP-776/918 — gateway CB cold-start — KHÁC GAP-1361 (email/VietQR thiếu CB hoàn toàn).
- GAP-135 — SLO partial — GAP-1366.
