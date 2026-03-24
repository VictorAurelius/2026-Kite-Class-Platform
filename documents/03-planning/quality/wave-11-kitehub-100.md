# Wave 11 — KiteHub 93→100

**Date:** 2026-03-24
**Baseline:** KiteHub 93/100 (A), Business Gap 95% (57/60)
**Target:** KiteHub 100/100 (A+), Business Gap 100%

---

## Gap Analysis (7 points to recover)

| Category | Current | Target | Gap | PRs needed |
|----------|---------|--------|-----|-----------|
| Project Mgmt | 7/10 | 10/10 | -3 | PR-1 |
| Security | 9/10 | 10/10 | -1 | PR-2 |
| Backend Tests | 9/10 | 10/10 | -1 | PR-3 |
| Frontend Tests | 9/10 | 10/10 | -1 | PR-4 |
| Documentation | 9/10 | 10/10 | -1 | PR-5 |
| Business Gap | 95% | 100% | -3 gaps | PR-6 |

---

## PR Definitions

### PR-1: Project Management Finalize [Project Mgmt +3]

**Yêu cầu chất lượng:**
- Tất cả wave completion checks finalized
- Parallel execution strategy up to date
- Active plans reflect current reality

**Scope:**
- [ ] Finalize `documents/04-quality/wave-3-completion-check.md` — mark complete, resolve pending items
- [ ] Update `documents/03-planning/parallel-execution-strategy.md` — reflect Waves 6-9, current project state
- [ ] Update `documents/03-planning/kitehub-saas-implementation-plan.md` — verify 17/17 status accurate
- [ ] Clean `documents/action-1.md` — hoặc archive nếu stale
- [ ] Tạo `documents/03-planning/project-status-2026-03-24.md` — tổng hợp trạng thái: waves completed, scores, remaining work

**Verification:** Không còn "in-progress" hoặc "modified" status trong docs đã hoàn thành

### PR-2: JWT Security Fix [Security +1]

**Yêu cầu chất lượng:**
- JWT secret fail-fast (không có nullable fallback)
- Production-safe default behavior

**Scope:**
- [ ] Fix JWT secret `#{null}` fallback trong `kitehub-subscription/src/main/resources/application.yml`
  - Đổi `jwt.secret: ${JWT_SECRET:#{null}}` → `jwt.secret: ${JWT_SECRET:?JWT_SECRET is required}`
- [ ] Verify tất cả @Value annotations cho secrets dùng fail-fast pattern
- [ ] Grep check: `grep -r "#{null}" kitehub/*/src/main/resources/` → phải return 0
- [ ] Update test configs nếu cần (test application.yml có thể set mock value)
- [ ] Tests: verify app fails to start without JWT_SECRET

**Verification:** `grep -r "#{null}" kitehub/` returns 0

### PR-3: Backend Test Coverage [Backend Tests +1]

**Yêu cầu chất lượng:**
- Test cho mọi scheduler
- Test cho mọi service class

**Scope:**
- [ ] Tạo `SubscriptionExpirationCheckerTest.java` — test:
  - sendRenewalReminders(): tìm subscriptions sắp hết hạn, gửi email đúng timing
  - processExpiredSubscriptions(): mark expired, suspend after grace period
  - Edge cases: no subscriptions, already expired, auto-renew enabled
- [ ] Verify test coverage: mỗi scheduler có test file tương ứng
- [ ] Run `mvn test -pl kitehub-subscription` — all pass

**Verification:** `find kitehub/kitehub-subscription/src/test/ -name "*Test.java" | wc -l` >= 25

### PR-4: Frontend Test Coverage [Frontend Tests +1]

**Yêu cầu chất lượng:**
- Tests cho tất cả settings components
- Tests cho InstanceTab

**Scope:**
- [ ] Tạo test cho `InstanceTab` component (hoặc verify nếu đã có)
- [ ] Tạo tests cho missing settings components
- [ ] Verify test count: `find kitehub/kitehub-frontend/src/ -name "*.test.*" | wc -l` >= 38

**Verification:** `npm test` (hoặc `vitest run`) all pass

### PR-5: API Documentation [Documentation +1]

**Yêu cầu chất lượng:**
- API documentation accessible
- OpenAPI annotations hoặc manual API doc

**Scope:**
- [ ] Tạo `documents/02-architecture/kitehub-api-reference.md` — list tất cả endpoints:
  - Gateway routes
  - Subscription API (instances, subscriptions, domains, payments)
  - Branding API (jobs, templates, assets)
  - Email API (internal)
  - Admin API (dashboard, instances, revenue)
- [ ] Mỗi endpoint: method, path, request body, response, auth required
- [ ] Cross-reference từ service READMEs

**Verification:** Doc exists, covers all controllers

### PR-6: Close Business Gaps [Business Gap +3]

**Yêu cầu chất lượng:**
- Mỗi gap có code/doc fix
- TDD cho code changes

**Scope:**
- [ ] Fix ai-branding.md — clarify service boundary: config nằm trong kitehub-branding (không phải subscription), thêm note rõ
- [ ] Tạo `kitehub/kitehub-branding/src/test/java/.../service/` — unit tests cho:
  - AIRateLimitServiceTest — check limit, increment usage, tier-based limits
  - TemplateGalleryServiceTest — list templates, get by id, filter by category
- [ ] Document mock API keys — thêm note trong `kitehub/.env.example` và `ai-branding.md`: "OPENAI_API_KEY: sk-mock-key for dev, replace in production"
- [ ] Run tests: `mvn test -pl kitehub-branding` all pass

**Verification:** Business gap check 60/60 (100%)

---

## Execution

| Agent | PR | Files | Conflict risk |
|-------|-----|-------|---------------|
| 1 | PR-1 (Project Mgmt) + PR-5 (API doc) | documents/ only | None |
| 2 | PR-2 (JWT fix) + PR-3 (Backend tests) | kitehub-subscription/ | Low (different files) |
| 3 | PR-4 (Frontend tests) | kitehub-frontend/ | None |
| 4 | PR-6 (Business gaps) | kitehub-branding/ + docs | None |

---

## Score Projection

| After | Score | Grade |
|-------|-------|-------|
| Baseline | 93 | A |
| +PR-1 (Project Mgmt) | 96 | A+ |
| +PR-2 (Security) | 97 | A+ |
| +PR-3 (Backend tests) | 98 | A+ |
| +PR-4 (Frontend tests) | 99 | A+ |
| +PR-5 (Documentation) | 100 | A+ |
| +PR-6 (Business gaps) | 100 + gaps closed | A+ |

---

## Dependencies

- Wave 10 (KiteClass) và Wave 11 (KiteHub) **không phụ thuộc nhau**
- Có thể chạy song song nếu đủ context window
- Hoặc tuần tự: Wave 10 trước → verify → Wave 11
