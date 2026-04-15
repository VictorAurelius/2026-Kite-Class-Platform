# Branding API — Use Cases

### UC-BAPI-01: Initiate Provisioning (REST)
- **Actor:** KiteHub onboarding orchestrator (internal system)
- **Call:** `POST /api/v1/instances` with `{tenantId, slug}`
- **Steps:**
  1. Validate request body (slug regex, tenant non-blank)
  2. Delegate to `InstanceLifecycleService.initiate`
  3. Return 201 with full instance DTO
- **Errors:** 400 invalid slug; 400 slug already in use (from service)

### UC-BAPI-02: Fetch Composite Branding Package
- **Actor:** KiteClass frontend on page load
- **Call:** `GET /api/v1/branding/{instanceId}/package` (optional `If-None-Match`)
- **Steps:**
  1. Proxy checks Redis cache `branding-package`
  2. Hit → build response from cached package
  3. Miss → assemble via `BrandingPackageServiceImpl.getByInstanceId`, cache it
  4. Build ETag from `brandingVersion` + payload hash
  5. If `If-None-Match` matches → 304 Not Modified
  6. Else → 200 with body + ETag
- **Postcondition:** FE caches by ETag for cheap revalidation

### UC-BAPI-03: List Instances By Status (Admin)
- **Actor:** Ops console (future GAP-067 UI)
- **Call:** `GET /api/v1/instances?status=FAILED`
- **Steps:**
  1. Delegate to repository filter
  2. Return list (each entry is `InstanceResponse`)

### UC-BAPI-04: Mark Branding Completed
- **Actor:** Branding pipeline worker (Sub-PR 3.5/3.6)
- **Call:** `POST /api/v1/instances/{id}/branding-completed` with optional `{frontendUrl}`
- **Steps:**
  1. `markBrandingCompleted(id, url)` transitions to DEPLOYED, bumps version
  2. Service emits `instance.deployed` via outbox
  3. Response: 200 with updated instance
- **Side effect:** Outbox poller dispatches event; downstream cache eviction + webhook happens via event subscribers (wired in Sub-PR 3.6)

### UC-BAPI-05: Rebrand Trigger
- **Actor:** Tenant admin (via KiteHub customer portal)
- **Call:** `POST /api/v1/instances/{id}/rebrand`
- **Steps:**
  1. Lifecycle transitions DEPLOYED → REGENERATING
  2. Outbox emits `instance.regenerating`
  3. Pipeline starts; current branding stays live until new DEPLOYED

### UC-BAPI-06: Mark Failed
- **Actor:** Pipeline worker on error
- **Call:** `POST /api/v1/instances/{id}/failed` with `{reason}`
- **Steps:**
  1. Reason required + ≤1000 chars
  2. Lifecycle sets FAILED, bumps retryCount
  3. Outbox emits `instance.failed`

### UC-BAPI-07: Retry
- **Actor:** Admin / auto-retry scheduler
- **Call:** `POST /api/v1/instances/{id}/retry`
- **Steps:**
  1. Service refuses when retryCount ≥ MAX_RETRIES (409)
  2. Else transitions FAILED → INITIALIZING, emits `instance.initializing`

### UC-BAPI-08: Internal Webhook — Evict Cache
- **Actor:** Outbox RabbitMQ dispatcher (future) / ops manual
- **Call:** `POST /internal/notify/instance-deployed?instanceId=X`
- **Steps:**
  1. Gateway validates internal source
  2. Controller calls `CachingBrandingPackageProxy.evict(id)`
  3. Response: 200 with "evicted" status

## Log
- 2026-04-14 — Initial UCs
