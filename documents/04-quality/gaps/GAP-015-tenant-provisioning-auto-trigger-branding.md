# GAP-015: Tenant Provisioning thiếu auto-trigger AI Branding

**Status:** 🟢 DONE (Wave 3 Sub-PR 3.6, merged 2026-04-14; TenantProvisioningSaga orchestrates initiate → infra stub → ready → Analyzer → Planner → PlanExecutor → DEPLOYED with compensation. RabbitMQ consumer wiring deferred to outbox-dispatcher follow-up.)
**Priority:** 🔴 P0 (blocker UX — new tenant không có branding automated)
**Domain:** Backend / Frontend / Integration
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/ai-branding-v2-redesign.md`
- `kitehub-frontend/src/components/onboarding/OnboardingWizard.tsx`
- `kitehub-platform/src/main/java/com/kitehub/platform/domain/enums/InstanceStatus.java`

## Problem

**Quá trình khởi tạo tenant mới KHÔNG auto-trigger AI branding.** Flow hiện tại:

```
Tenant đăng ký → Email verify → TRIAL status →
  OnboardingWizard hiện link "/branding" →
  Tenant phải click manual sang branding page →
  Tenant phải manual trigger từng bước
```

**Vấn đề:**
- Không có event-driven automation
- Tenant có thể skip branding → instance deploy với default ugly theme
- Không có lifecycle state machine (ngoài subscription status)
- `kitehub-branding` service không listen `tenant.created` event

## Evidence

### 1. OnboardingWizard chỉ có link
`kitehub-frontend/src/components/onboarding/OnboardingWizard.tsx`:
```tsx
{ label: 'Tạo thương hiệu AI', desc: 'Upload logo → AI tạo website', href: '/branding' },
```
→ Manual link, không auto-redirect hay trigger backend.

### 2. InstanceStatus confuse nghĩa
`kitehub-platform/.../InstanceStatus.java`:
```java
enum InstanceStatus { PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED }
```
→ Subscription lifecycle, **KHÔNG** phải provisioning lifecycle.

GAP-009 đề xuất state machine provisioning mới. **Conflict tên enum cần resolve.**

### 3. Không có RabbitMQ listener
```bash
grep -l "onTenantCreated\|TenantCreatedEvent\|tenant.created" kitehub-branding/src/
# → No results
```
→ Branding service **không biết** khi tenant mới được tạo.

## Proposed Fix

### Architecture: Event-Driven Auto-Provisioning

```
Tenant đăng ký
  ↓
[kitehub-subscription] publish event "tenant.created"
  ↓
┌──────────────────────────────────────────────────┐
│ [kitehub-branding] RabbitMQ listener              │
│   • Create FrontendInstance (status=NOT_STARTED)  │
│   • Wait for user to complete wizard              │
│     OR auto-start với default template (skip)     │
└──────────────────────────────────────────────────┘
  ↓
[Tenant login] → FE shows "Complete branding" banner
  ↓
Tenant complete wizard → FE send request
  ↓
POST /api/v1/branding/start-provisioning
  ↓
FrontendInstance: NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED
  ↓
Event "instance.deployed" → notify tenant via email + FE SSE
```

### Rename & Split Enums

**Naming clarity:**
```java
// kitehub-platform: SUBSCRIPTION lifecycle (existing, keep name)
enum SubscriptionStatus {
  PENDING, TRIAL, ACTIVE, SUSPENDED, DELETED
}
// (Rename InstanceStatus → SubscriptionStatus để match semantic)

// kitehub-branding: PROVISIONING lifecycle (NEW, từ GAP-009)
enum FrontendInstanceStatus {
  NOT_STARTED, INITIALIZING, GENERATING, DEPLOYED, REGENERATING, FAILED
}
```

2 enums riêng cho 2 concerns riêng.

### Implementation Steps

**1. Event publishing:**
```java
// kitehub-subscription
@Service
public class TenantService {
  public Tenant createTenant(CreateTenantRequest req) {
    var tenant = tenantRepository.save(...);
    eventPublisher.publish(new TenantCreatedEvent(
      tenant.getId(), tenant.getSlug(), tenant.getOwnerEmail()));
    return tenant;
  }
}
```

**2. Event listener in branding:**
```java
// kitehub-branding
@Component
public class TenantProvisioningListener {

  @RabbitListener(queues = "kite.tenant.created")
  public void onTenantCreated(TenantCreatedEvent e) {
    // Create FrontendInstance with NOT_STARTED status
    instanceService.initialize(e.tenantId, e.slug);
    // Send welcome email with wizard link
    emailService.sendBrandingWelcome(e.ownerEmail, e.tenantId);
  }
}
```

**3. FE show banner for incomplete branding:**
```tsx
// kitehub-frontend
function TenantDashboard() {
  const { instance } = useInstanceStatus();
  if (instance.status === 'NOT_STARTED') {
    return <CompleteBrandingBanner onStart={() => router.push('/branding/wizard')} />;
  }
  if (instance.status === 'GENERATING') {
    return <ProvisioningProgress jobId={instance.currentJobId} />;
  }
  // ... normal dashboard
}
```

**4. Wizard completion triggers deploy:**
```tsx
// After user approves all resources in wizard
async function handleDeploy() {
  await fetch('/api/v1/branding/start-provisioning', {
    method: 'POST',
    body: JSON.stringify({ selectedTemplates, colors, etc. })
  });
  router.push('/branding/progress'); // shows INITIALIZING → ... → DEPLOYED
}
```

**5. Fallback: auto-provision default nếu skip wizard sau N days**
```java
@Scheduled(cron = "0 0 2 * * *") // Daily 2am
public void autoProvisionSkippedTenants() {
  var stale = instanceRepo.findNotStartedOlderThan(3_DAYS);
  for (var instance : stale) {
    brandingJobService.enqueueWithDefaultTemplate(instance.getTenantId());
    // User có thể rebrand sau
  }
}
```

## Integration với Existing Code

**Không break existing:**
- `InstanceStatus` rename → `SubscriptionStatus` (simple refactor, find/replace)
- OnboardingWizard link "/branding" vẫn work, nhưng BE backend logic giờ drive lifecycle
- Tenant dashboard check `FrontendInstanceStatus` thay vì chỉ subscription

## Acceptance Criteria

- [ ] Rename `InstanceStatus` → `SubscriptionStatus` trong kitehub-platform
- [ ] Tạo `FrontendInstanceStatus` enum + entity trong kitehub-branding
- [ ] Event `tenant.created` published từ kitehub-subscription
- [ ] Event listener `TenantProvisioningListener` tạo FrontendInstance
- [ ] API `POST /api/v1/branding/start-provisioning` trigger lifecycle
- [ ] FE dashboard check provisioning status, show banner/progress
- [ ] Welcome email có branding wizard link
- [ ] Scheduled job auto-provision default sau 3 days skip
- [ ] Integration test: tenant register → auto-init FrontendInstance → wizard → DEPLOYED

## Dependencies

- **Depends on GAP-009** (FrontendInstanceStatus enum design)
- **Depends on GAP-013** (wizard UX)
- **Integrates with GAP-010** (package API triggered when DEPLOYED)

## Log

- 2026-04-14 — Phát hiện tenant provisioning KHÔNG auto-trigger branding; OnboardingWizard chỉ có link manual; không có event listener
