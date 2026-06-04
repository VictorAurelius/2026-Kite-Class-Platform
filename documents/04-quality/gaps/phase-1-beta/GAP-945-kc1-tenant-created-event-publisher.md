# GAP-945: KC saga not wired — kitehub-subscription thiếu `tenant.created` publisher

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) — KH-2b → KC-1 chain critical path
**Defer-to:** After Wave flow-kh3 finish (per user direction 2026-06-04)

## Problem

`AuthService.registerFromBetaInvite:218` (kitehub-subscription) gọi `instanceService.createTrialInstance(...)` synchronously và KHÔNG enqueue `tenant.created` event (no outbox enqueue / no `rabbitTemplate.convertAndSend`). `TenantProvisioningSaga` trong kiteclass-core tồn tại như orphan code — không có `@RabbitListener(queues = "tenant.created.queue")` consumer. Hệ quả: KC tenant DB không được tạo, `Instance.status` stuck `INITIALIZING` mãi mãi, Owner login vào `kc-<slug>.kitehub.me/admin` → 404 hoặc spinner forever. Surfaced cross-audit: persona simulation Finding 1.2 + failure-mode matrix A1×E5×EC2 + A1×E8×EC2.

## Proposed Fix

Thêm outbox enqueue `tenant.created` trong `AuthService.registerFromBetaInvite` sau commit tx; wire `@RabbitListener` consumer trong kiteclass-core invoking `TenantProvisioningSaga.handle(event)`.

## Acceptance Criteria

- [ ] `grep -rn "tenant.created\|TenantCreatedEvent" kitehub/kitehub-subscription/src/main/java` ≥1 publisher hit
- [ ] `grep -rn "@RabbitListener.*tenant" kiteclass/kiteclass-core/src/main/java` ≥1 consumer hit
- [ ] Post-beta-signup walk: `Instance.status` transitions INITIALIZING → DEPLOYED trong <30s

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (persona / matrix consensus)
- Audit artifact: `documents/04-quality/audits/persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md`
- Sister gaps: GAP-925 (consumer wire-format Wave flow-kh1) — sibling outbox dispatcher work
- Flow Verification Campaign §4 row KC-1
