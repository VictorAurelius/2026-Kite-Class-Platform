# Quality Polish — Remaining 5 points (95→100)

**Date:** 2026-03-25
**Current:** 95/100 (A+)
**Target:** 100/100

---

## Items

### PR-able (code changes)

| # | Item | Category | Impact | Effort | Dependency |
|---|------|----------|--------|--------|-----------|
| 1 | Add hCaptcha to KC registration | Security | +1 | 2-3 days | None |
| 2 | Add KC Playwright E2E specs (3-5 critical paths) | Frontend | +1 | 1-2 days | Docker running |

### Infrastructure (environment/config)

| # | Item | Category | Impact | Effort | Dependency |
|---|------|----------|--------|--------|-----------|
| 3 | Enable Docker Desktop WSL integration | E2E | +2 | Setup only | Docker Desktop license |
| 4 | Enable Docker-in-CI for integration tests | Backend | +1 | 1 day | CI config access |

---

## Notes

- Items 3+4 là environment setup, không phải code quality
- Item 1 (captcha) có thể làm independent PR
- Item 2 (Playwright) cần Docker running để test
- KHÔNG tạo wave cho 2-4 PRs nhỏ — dùng feature branch + PR lẻ
