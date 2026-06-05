---
title: Wave security-1 Bucket 0 spike — filter-enablement root cause + design space
audience: dev
created: 2026-06-05
scope: GAP-983 cross-tenant by-id leak — spike investigation findings trước khi implement
---

# Wave security-1 Bucket 0 — Spike findings (GAP-983)

## Root cause confirmed (empirical reading + walk evidence)

1. **`@Filter` declared trên `BaseEntity` MappedSuperclass** (`BaseEntity.java:43-44` — `@FilterDef` + `@Filter(name="tenantFilter", condition="instance_id = :tenantId")`). 3 marketing entity (Lead/LandingPage/ContactMessage) re-declare `@Filter` riêng.
2. **OSIV off** (`application.yml:70` `open-in-view: false`) — test profile inherit (không override trong application-test.yml).
3. **`TenantFilterInterceptor.preHandle:85-89`** enable filter trên `entityManagerProvider.getIfAvailable().unwrap(Session.class)`. Interceptor chạy TRƯỚC khi `@Transactional` service method mở transaction → enable filter trên session KHÔNG phải session mà `@Transactional` method dùng.
4. **`ClassServiceImpl.getClass` (line 200-202) `@Transactional(readOnly=true)`** → mở transaction-bound session riêng → filter chưa enable → `findByIdAndDeletedFalse` leak cross-tenant. Method KHÔNG `@Transactional` (course/teacher/student getById) khác behavior.

## Test-harness masking (quan trọng cho Bucket D)

- `TenantIsolationIT` annotated `@Transactional` Ở CLASS LEVEL (line ~44) → mọi MockMvc request chạy trong CÙNG 1 test transaction → filter enable bởi `TestTenantContextFilter` áp dụng cho session đó → `@Transactional` service methods JOIN transaction đó (propagation REQUIRED) → **leak bị mask trong test**.
- Existing test CHỈ test by-id isolation cho **student** (`shouldIsolateBothTenantsData` — student getById KHÔNG @Transactional → filtered OK). `shouldIsolateCourseDataBetweenTenants` CHỈ test LIST.
- → Bucket D PHẢI: (a) bỏ class-level `@Transactional` để mỗi request có session riêng (reproduce prod model), (b) thêm by-id case cho class (@Transactional path) + course/teacher/session.

## Design space (3 candidates per plan §1)

- (a) `TransactionSynchronization` register tại txn begin → enable filter trên session.
- (b) Hibernate `Integrator`/`SessionFactoryBuilder` filter-on-open.
- (c) AOP `@Around`/`@Before` trên `@Transactional` (hoặc service layer) → `entityManager.unwrap(Session.class).enableFilter(...)`. **Ordering caveat:** aspect phải chạy INSIDE transaction (sau txn begin). Spring `@EnableTransactionManagement` default order = LOWEST_PRECEDENCE → cần verify ordering empirically.

## Open question for spike

- `@Filter` trên MappedSuperclass CÓ inherit cho 58 subclass entity trong Hibernate 6.6 (Spring Boot 3.5.14) không? Walk fix-v1 (thêm @Filter 4 entity → teacher blocked) gợi ý KHÔNG inherit hoàn toàn. Spike PHẢI verify empirically: nếu inherit OK → Bucket B chỉ cần fix enablement; nếu không → Bucket B cần explicit per-entity `@Filter`.

## Env

- Docker UP local → chạy Testcontainers IT trực tiếp (`postgres:15-alpine` per `TestContainersConfiguration`).
- Self-hosted runner OFFLINE (user đang bật) — full IT suite validate qua runner OR local.

## Bucket B prep — entity inventory + exclusion analysis (coordinator, parallel với Bucket A)

**61 entity extends `BaseEntity`** (plan ước ~58). 3 marketing (Lead/LandingPage/ContactMessage) đã có explicit `@Filter`.

**Exclusion analysis (entity cross-tenant by-design phải EXCLUDE khỏi filter):**

| Candidate | Verdict | Lý do |
|---|---|---|
| `role/Role`, `role/Permission`, `role/UserRole` | ✅ KEEP filter (per-tenant) | Unique index `instance_id,name` → roles/permissions scoped per instance (`is_system` flag cho system-defined nhưng vẫn per-instance). Filter đúng. |
| `common/outbox/OutboxEvent` | ✅ KEEP (safe) | Dispatcher `OutboxEventPublisher.findDispatchable` chạy scheduled background KHÔNG có tenant context → filter tự không enable → dispatcher thấy hết. Write xảy ra trong tenant request (instance_id auto-set). An toàn. |
| `common/audit/AuditLog`, `parent/audit/ParentReadAuditLog` | ✅ KEEP (per-tenant) | Audit log per-tenant; mỗi tenant chỉ đọc audit của mình — filter đúng. |
| `moderation/ModerationQueue`, `retention/DeletionRequest`, `retention/Retention` | ⚠️ Bucket B verify | Có thể platform-admin cross-tenant read. KHÔNG exposed qua REST getById (không trong attack surface). Nếu có platform path đọc cross-tenant → path đó phải clear TenantContext (không phải exclude entity). |

**Kết luận chính:** Safety đến từ mechanism "chỉ enable filter khi `TenantContext.getCurrentTenant() != null`" — `@Filter` INERT khi chưa enable. Background dispatcher + platform-admin paths (không set tenant) tự nhiên thấy hết. → **Exclusion list per-entity ≈ RỖNG cho kiteclass-core** (strictly multi-tenant per-school, không có legitimate cross-instance read trong tenant request).

**Quyết định Bucket B phụ thuộc finding @Filter-inheritance của Bucket A:**
- Nếu MappedSuperclass `@Filter` inherit OK sau khi fix enablement → Bucket B gần như rỗng (chỉ verify).
- Nếu cần explicit per-entity `@Filter` → Bucket B apply cho 58 tenant-scoped entity (exclusion moot vì @Filter inert without enablement); chỉ cần đảm bảo `condition="instance_id = :tenantId"` đúng + platform-admin paths clear TenantContext.

**Lưu ý cho Bucket A mechanism:** PHẢI guard `tenant != null` trước khi enable filter (nếu không, background dispatcher + pre-tenant auth path sẽ bị filter với null param → vỡ).
