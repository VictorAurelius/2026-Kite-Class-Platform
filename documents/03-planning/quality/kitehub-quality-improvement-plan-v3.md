# KiteHub Quality Improvement Plan v3

**Ngày tạo:** 2026-03-23
**Baseline:** 91/100 (Grade A) — audit ngày 2026-03-23
**Mục tiêu:** 97+/100 (Grade A+)
**Dựa trên:** Quality Audit 2026-03-23, Action Items

---

## Gap Analysis

| # | Category | Current | Target | Gap |
|---|----------|---------|--------|-----|
| 1 | E2E Functionality | 6 | 10 | -4 |
| 2 | Security | 9 | 10 | -1 |
| 3 | DevOps/Infrastructure | 8 | 10 | -2 |
| 4 | UI/UX | 9 | 10 | -1 |
| 5 | Code Quality | 9 | 10 | -1 |
| **Total** | | **91** | **100** | **-9** |

*Categories đã đạt 10/10: Backend Tests, Frontend Tests, CI/CD, Documentation, Project Management*

---

## PR Plan

### PR-V3-1: E2E Docker Verification (Quick Win)

**Score impact:** E2E +2 → 8/10
**Estimate:** 30 phút
**Yêu cầu:** Docker Desktop running
**Scope:**
- [ ] Start stack: `cd kitehub && ./scripts/up.sh`
- [ ] Wait healthy: `./scripts/wait-for-healthy.sh`
- [ ] Run E2E: `./scripts/test-api-e2e.sh` — phải pass 100%
- [ ] Fix bất kỳ test nào fail
- [ ] Screenshot evidence lưu vào `documents/04-quality/`

**Lý do:** E2E chỉ được 6/10 vì chưa verify realtime với Docker. Cần chạy thật 1 lần.

---

### PR-V3-2: AI Branding E2E Verification

**Score impact:** E2E +2 → 10/10
**Estimate:** 1 giờ
**Yêu cầu:** Docker Desktop + 8GB+ RAM cho Ollama
**Scope:**
- [ ] Start với AI profile: `./scripts/up.sh --profile ai-local`
- [ ] Pull model: `docker exec kitehub-ollama ollama pull llama3.1:8b`
- [ ] Test branding flow: POST `/api/platform/branding/ai/analyze-logo`
- [ ] Test theme generation: POST `/api/platform/branding/ai/generate-theme`
- [ ] Fix nếu có lỗi
- [ ] Add E2E test cases cho AI endpoints vào `test-api-e2e.sh`

**Lý do:** AI features implemented nhưng chưa verify end-to-end.

---

### PR-V3-3: Security — Typed DTOs for AuthController

**Score impact:** Security +1 → 10/10
**Estimate:** 30 phút
**Scope:**
- [ ] Replace `Map<String, String>` trong `AuthController.updateProfile()` với `UpdateProfileRequest` DTO
- [ ] Replace `Map<String, String>` trong `AuthController.changePassword()` với `ChangePasswordRequest` DTO
- [ ] Replace `Map<String, String>` trong `AuthController.resendVerification()` với `ResendVerificationRequest` DTO
- [ ] Add `@Valid` + validation annotations (`@NotBlank`, `@Email`, `@Size`)
- [ ] Add tests cho validation

**Files cần sửa:**
- `AuthController.java` — 3 endpoints
- Tạo 3 DTO files mới

---

### PR-V3-4: DevOps — Alerting Rules + Secrets Doc

**Score impact:** DevOps +2 → 10/10
**Estimate:** 1 giờ
**Scope:**
- [ ] Tạo `kitehub/docker/prometheus/alert-rules.yml` — basic alerts:
  - Service down (up == 0)
  - High error rate (>5% 5xx in 5m)
  - High response time (p99 > 2s)
  - Low disk space (<10%)
- [ ] Cấu hình Prometheus load alert rules
- [ ] Tạo `documents/02-architecture/secret-management.md`:
  - Liệt kê tất cả secrets (JWT_SECRET, ENCRYPTION_MASTER_KEY, etc.)
  - Cách generate: `./scripts/setup.sh`
  - Rotation policy
  - Production: env vars, không hardcode

**Files cần tạo/sửa:**
- `kitehub/docker/prometheus/alert-rules.yml` (mới)
- `kitehub/docker/prometheus/prometheus.yml` — thêm rule_files
- `documents/02-architecture/secret-management.md` (mới)

---

### PR-V3-5: UI/UX — Onboarding Checklist

**Score impact:** UI/UX +1 → 10/10
**Estimate:** 0.5 ngày
**Scope:**
- [ ] Tạo `OnboardingChecklist` component trong kitehub-frontend
- [ ] Hiển thị sau register/login lần đầu:
  - ✅ Đăng ký tài khoản
  - ⬜ Xác nhận email
  - ⬜ Tạo instance đầu tiên
  - ⬜ Upload logo
  - ⬜ Tùy chỉnh theme
- [ ] Persist checklist state trong localStorage
- [ ] Add component test

**Files cần tạo:**
- `kitehub-frontend/src/components/onboarding/OnboardingChecklist.tsx`
- `kitehub-frontend/src/components/onboarding/__tests__/OnboardingChecklist.test.tsx`

---

### PR-V3-6: Code Quality — IDE Warnings Cleanup

**Score impact:** Code Quality +1 → 10/10
**Estimate:** 30 phút
**Scope:**
- [ ] Scan tất cả IDE warnings còn lại (Java + TypeScript)
- [ ] Fix unused imports, missing types
- [ ] Verify: 0 warnings trong IDE Problems tab

---

## Execution Order

```
Phase 1 — Docker Verification (1 giờ, cần Docker):
  PR-V3-1 (E2E verify) ──→ PR-V3-2 (AI verify)

Phase 2 — Code Improvements (2 giờ, không cần Docker):
  PR-V3-3 (typed DTOs) ──→ PR-V3-4 (alerting + secrets)
                                    ↓
Phase 3 — Polish (1 ngày):
  PR-V3-5 (onboarding) ──→ PR-V3-6 (IDE cleanup)
```

**Note:** Phase 1 cần Docker Desktop running. Phase 2-3 có thể làm song song.

---

## Score Projection

| Sau PR | Score | Grade | Tăng |
|--------|-------|-------|------|
| Baseline | 91 | A | — |
| PR-V3-1 (E2E verify) | 93 | A | +2 |
| PR-V3-2 (AI verify) | 95 | A+ | +2 |
| PR-V3-3 (typed DTOs) | 96 | A+ | +1 |
| PR-V3-4 (alerting + secrets) | 98 | A+ | +2 |
| PR-V3-5 (onboarding) | 99 | A+ | +1 |
| PR-V3-6 (IDE cleanup) | **100** | **A+** | +1 |

---

## Estimate tổng

| Phase | PRs | Time |
|-------|-----|------|
| Phase 1 | V3-1, V3-2 | 1.5 giờ |
| Phase 2 | V3-3, V3-4 | 2 giờ |
| Phase 3 | V3-5, V3-6 | 1 ngày |
| **Total** | **6 PRs** | **~2 ngày** |

---

## Completion Status

| PR | Status | GitHub | Score |
|----|--------|--------|-------|
| PR-V3-1 E2E Docker Verify | ⬜ TODO | — | +2 |
| PR-V3-2 AI Branding E2E | ⬜ TODO | — | +2 |
| PR-V3-3 Typed DTOs | ⬜ TODO | — | +1 |
| PR-V3-4 Alerting + Secrets | ⬜ TODO | — | +2 |
| PR-V3-5 Onboarding Checklist | ⬜ TODO | — | +1 |
| PR-V3-6 IDE Cleanup | ⬜ TODO | — | +1 |
| **Total** | **0/6** | | **0/9** |
