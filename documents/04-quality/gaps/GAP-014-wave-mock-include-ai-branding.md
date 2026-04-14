# GAP-014: Wave Mock Plan Missing AI Branding Workflow

**Status:** 🟡 PLANNED (Wave 1 Sprint 0)
**Branch:** wave/01-foundation
**Priority:** 🔴 P0 (scope gap — wave plan incomplete)
**Domain:** Backend / Frontend / Mock Data
**Detected:** 2026-04-14
**Related Docs:**
- `documents/03-planning/wave-mock-data-local-dev.md`
- `documents/02-architecture/ai-branding-v2-redesign.md`

## Problem

Wave plan hiện tại (`wave-mock-data-local-dev.md`) scope CHỈ cho **KiteClass FE + BE**. **KHÔNG bao gồm**:

- ❌ KiteHub `kitehub-branding` service APIs (AI generation endpoints)
- ❌ AI workflow mới (Analyzer → Planner → Executor)
- ❌ Resource classification router (GAP-007)
- ❌ Guided wizard UX (GAP-013)
- ❌ Branding package API (GAP-010)
- ❌ Instance provisioning lifecycle (GAP-009)

User raised: "các plan về mock có đang bỏ qua mock AI branding và chạy workflow mới chốt cho kiteclass frontend không?" — **CÓ, đang bỏ qua**.

## Evidence

Trong wave plan §1.1 Table:
```
| Branding | 3 | 1 (GET) | 2 |
```
→ 3 endpoints là `settings/branding` đơn giản của KiteClass (GET/PUT branding config + POST logo), **không phải** AI branding workflow.

Thiếu kitehub-branding endpoints như:
- `POST /api/platform/branding/ai/generate-theme`
- `GET /api/platform/branding/assets/{instanceId}`
- `POST /api/v1/branding/analyze` (Analyzer)
- `POST /api/v1/branding/plan` (Planner)
- `POST /api/v1/branding/execute` (Executor)
- `GET /api/v1/branding/{id}/package` (GAP-010 new)
- `GET /api/v1/branding/jobs/{jobId}` (async job status)
- `GET /api/v1/templates` (template gallery)
- `GET /api/v1/instances/{id}/status` (lifecycle status)

## Proposed Fix

### 1. Update wave plan

Mở rộng `wave-mock-data-local-dev.md` với section riêng:

**§1.1 — thêm vào FE API Coverage:**
```
| KiteHub Branding (AI) | 12+ | 0 | 12+ |
```

**§1.3 — thêm FK chain:**
```
... Classes → BrandingJob (instanceId FK) →
    ResourceRequest → BrandingResource (categorized) →
    InstanceStatus (lifecycle) → ...
```

**§4 — thêm sub-PR:**
```
PR E: FE MSW mock cho AI Branding workflow
  - Mock kitehub-branding endpoints (analyze, plan, execute, package)
  - Mock async job polling (returns COMPLETED sau 2s simulated)
  - Mock template gallery (return 6 sample templates per category)
  - Mock quality review (return score 85 mặc định)
  - Mock lifecycle transitions (NOT_STARTED → INITIALIZING → ... → DEPLOYED)

PR F: BE DataSeeder cho kitehub-branding
  - Seed 30 templates với metadata
  - Seed 1 sample tenant với đầy đủ branding resources (status=DEPLOYED)
  - Seed 1 sample branding job (COMPLETED)
  - Seed sample instance với quality score
```

### 2. Mock workflow demo

Local dev nên demo được **full flow**:
```
1. Login tenant admin
2. Onboarding wizard → trigger branding wizard
3. Choose audience/tone/style (mocked template picker)
4. Preview instance (mock rendered)
5. Approve resources
6. Watch lifecycle: INITIALIZING → GENERATING → DEPLOYED
7. View branding package API response
8. Trigger regenerate → new job
9. View quality score report
```

Tất cả chạy với mock data, không cần real AI model calls.

### 3. Integration với KiteClass frontend

Mock endpoint `GET /api/v1/branding/{id}/package` trong kiteclass-frontend mock:
- Trả về mock theme + assets URLs (placeholder images)
- KiteClass FE `BrandingProvider` load → apply CSS vars
- Demo theme switching khi regenerate mock event fire

## Acceptance Criteria

- [ ] Wave plan updated với scope kitehub-branding
- [ ] 12+ new endpoints mocked (AI workflow)
- [ ] Lifecycle state machine mocked (with simulated delays)
- [ ] Template gallery mocked (6 samples per category)
- [ ] Quality review mocked
- [ ] Demo script: full flow runnable locally không cần AI model
- [ ] Screenshots: wizard → preview → deployed (captured)

## Dependencies

- Blocks: demonstrating AI branding design end-to-end before real implementation
- Related: GAP-007, GAP-008, GAP-009, GAP-010, GAP-011, GAP-012, GAP-013

## Log

- 2026-04-14 — Phát hiện wave plan scope thiếu AI branding (user raised)
