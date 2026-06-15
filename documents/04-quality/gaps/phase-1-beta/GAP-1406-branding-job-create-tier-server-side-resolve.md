# GAP-1406: Branding job-create controllers resolve subscription tier server-side (staleness hardening)

**Status:** 🔵 OPEN
**Priority:** 🟢 P2
**Domain:** Backend
**Found:** 2026-06-15 (GAP-1020 cross-flow sweep)
**Affects:** kitehub-branding job-create flow (FULL_AI banner cost gating)

## Problem

Cross-flow sweep của GAP-1020 (per `cross-flow-bug-class-sweep.md`) tìm thấy 2 site CÙNG bug-class "trust gateway-injected tier header cho entitlement" mà GAP-1020 KHÔNG cover (gap chỉ scope 6 site AIBrandingController + BrandingWizardController):

- `BrandingJobController.java:95` — `@RequestHeader("X-Subscription-Tier", defaultValue="FREE")` → tier truyền vào job-create / FULL_AI gating (GAP-1135/1137).
- `BrandingJobV1Controller.java:186` — tương tự, wizard v1 job-create.

KHÔNG phải lỗ hổng spoof live: gateway đã strip client `X-Subscription-Tier` + re-inject từ JWT `tier` claim (GAP-1020 gateway side, committed `e7444b455`). 2 site này nhận tier gateway-trusted (JWT claim), KHÔNG phải raw client. Vấn đề còn lại = **staleness**: JWT `tier` là snapshot tại login; user downgrade PREMIUM→FREE giữa session vẫn trigger được FULL_AI banner (có phí) trong cửa sổ token còn hiệu lực, cho tới khi refresh token. GAP-1020 đã wire `SubscriptionTierResolver` (đọc `instances.tier` authoritative) cho 6 site chính; 2 site job-create này nên dùng cùng resolver để đóng cửa sổ staleness.

## Proposed Fix

Inject `SubscriptionTierResolver` vào `BrandingJobController` + `BrandingJobV1Controller`, resolve tier từ gateway-trusted instance id (X-Instance-Id / X-Tenant-Id) thay vì dùng thẳng header. Mirror pattern AIBrandingController (GAP-1020).

## Acceptance Criteria

- [ ] BrandingJobController + BrandingJobV1Controller resolve tier qua `SubscriptionTierResolver`
- [ ] FULL_AI gating (GenerationMode/FullAiQuotaService) nhận tier server-resolved, không phải header thuần
- [ ] Test: downgrade-mid-session (instances.tier=FREE) → job-create không nâng lên FULL_AI

## Related

- Discovered in: GAP-1020 cross-flow sweep (this PR)
- Depends: GAP-1020 (`SubscriptionTierResolver`); GAP-1135/1137 (FULL_AI tier gating)
