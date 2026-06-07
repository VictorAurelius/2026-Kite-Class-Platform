# GAP-1047: Saga consumer thiếu TenantContext → frontend_instances.instance_id NULL → initiate chết

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-07 (KC-1 saga live walk#2 — sau khi GAP-1045 conversion fix unblock saga)
**Affects:** Tenant-provisioning saga (kiteclass-core); chặn GAP-945/946/947/952/953/954 happy-path

## Problem

Sau khi fix GAP-1045 (conversion), `tenant.created` được consume nhưng saga `initiate` INSERT `frontend_instances` với **`instance_id = NULL`** → `SQLState 23502 not-null violation` → initiate chết → frontend_instance không tạo → saga keystone vẫn dead (1 layer sâu hơn).

**Root cause:** `FrontendInstance` (BaseEntity) có `instance_id` (UUID, NOT NULL — RLS tenant id) được **auto-populate bởi `EntityPersistenceListener.@PrePersist`** từ `TenantContext.getCurrentTenant()`. Saga chạy trong **RabbitMQ consumer thread KHÔNG có request-scoped TenantContext** → `TenantContext.isSet()=false` → listener log "FAILED: NOT setting instanceId" → instance_id null → violation. Bug provisioning-1 pre-existing, bị GAP-1045 conversion bug che (saga chưa từng chạy).

## Root Cause

Saga là cross-service flow (consumer thread), không có TenantContext mà RLS auto-population dựa vào. Mọi entity saga persist (FrontendInstance + branding entities trong runBrandingPlan) đều RLS-scoped → cần context.

## Proposed Fix (SHIPPED)

`TenantCreatedEventConsumer.handlePayload`: parse `event.getTenantId()` (= subscription Instance UUID = RLS tenant) → `TenantContext.setCurrentTenant(uuid)` bao quanh `saga.provision` + notify, `clear()` trong finally. Auto-set instance_id cho FrontendInstance + branding entities + đúng RLS GUC/filter scope cho mọi @Transactional boundary trong saga. Thêm guard: non-UUID tenantId → drop + ACK (không scope được RLS). `TenantContext` không switch datasource (single shared DB row-scoping per `TenantAwareDataSourceInterceptor`) → an toàn.

## Acceptance Criteria

- [x] Consumer set TenantContext từ event tenantId + clear finally
- [x] Non-UUID tenantId guard (drop + ACK) + unit test
- [x] Live walk: saga initiate→GENERATING→DEPLOYED, `frontend_instances.instance_id` populated (verified KC-1 walk#3: id=5 instance_id=b40eb7b0 status=DEPLOYED)

## Related

- Parent blocker: GAP-1045 (conversion — fixed first, unblocked this)
- Discovered in: KC-1 saga walk#2 2026-06-07 (catalog-then-batch: conversion fix → next layer bug)
- Note: `EntityPersistenceListener` có `System.err.println` debug spam (line 39-54) — separate cleanup (GAP-1048 candidate)
