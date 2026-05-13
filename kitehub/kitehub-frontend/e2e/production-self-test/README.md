# Production Self-Test E2E Specs

**Status:** Scaffold (Wave 69) — selectors + assertions cần calibrate sau khi user manual-run lần đầu per [`documents/03-planning/end-user/plan-1-self-test-e2e.md`](../../../../documents/03-planning/end-user/plan-1-self-test-e2e.md).
**Target:** Real production stack (`https://kitehub.me` + `https://api.kitehub.me`) — **không MSW mocks**, **không CI auto-trigger**.

---

## Khác gì `e2e/beta-funnel/`?

| Aspect | `beta-funnel/` (existing) | `production-self-test/` (new — this folder) |
|---|---|---|
| Target | MSW-mocked endpoints | Real production `api.kitehub.me` |
| Trigger | CI auto on PR (`e2e-pre-release.yml`) | Manual on-demand only |
| Side effects | None (mocked) | **Creates real DB rows** (beta_access_request, tenant, user, class) |
| Run frequency | Every PR touching FE | Pre-cohort verification + post-deploy smoke |
| Cleanup | n/a | Manual SQL cleanup OR dedicated test-tenant flag |

---

## How to run (manual)

### Prerequisites

1. `admin@kitehub.me` đăng nhập được — credentials trong AWS Secrets Manager `kitehub/production/admin-credentials`
2. Recipient email verify trên SES (test recipient `*@*` đã `create-email-identity` + click verify link)
3. Production stack alive — `curl https://api.kitehub.me/actuator/health` = 200

### Run all production self-test specs

```bash
cd kitehub/kitehub-frontend

# Set env vars
export E2E_PROD_ADMIN_EMAIL=admin@kitehub.me
export E2E_PROD_ADMIN_PASSWORD=<from-secrets-manager>
export E2E_PROD_TEST_RECIPIENT=<verified-recipient-email>
export E2E_PROD_BASE_URL=https://kitehub.me
export E2E_PROD_API_BASE=https://api.kitehub.me

# Run với headed mode để observe (lần đầu)
pnpm exec playwright test e2e/production-self-test/ --headed

# Run với trace (debug nếu fail)
pnpm exec playwright test e2e/production-self-test/ --trace on
```

### Run specific step

```bash
# Bước 2 only (form submission)
pnpm exec playwright test e2e/production-self-test/full-flow.spec.ts -g "Bước 2"
```

---

## Cleanup after run

Mỗi run tạo real DB rows. Cleanup options:

**Option A — SQL delete (recommended cho dev):**
```bash
aws ssm start-session --target i-05d7af46d01436b96 --document-name AWS-StartInteractiveCommand --parameters command="docker exec kite-postgres psql -U kite -d kitehub -c 'DELETE FROM beta_access_request WHERE email LIKE \\'%test-self-test%\\'; DELETE FROM users WHERE email LIKE \\'%test-self-test%\\'; DELETE FROM tenants WHERE name LIKE \\'test-tenant-self-test-%\\';'"
```

**Option B — test-tenant flag (production-grade, future):**
- Add `is_test_tenant` boolean to tenants table
- Filter from analytics + admin dashboards
- Auto-purge cron job daily

Currently Option A acceptable (low frequency self-test, ≤10 rows/run).

---

## Why skipped-by-default in CI?

- Real DB writes — không phải concern lúc Phase 1 BETA pre-launch (no real users), nhưng risk khi cohort live
- Network flake against real prod = false negatives
- SES sandbox không send tới CI runner email → email-receipt assertions sẽ fail
- Better: run manual on-demand trước mỗi cohort batch hoặc khi nghi ngờ regression

When opt-in cho CI? Sau khi:
1. Test-tenant flag exists (Option B above)
2. Email-receipt step extracted to separate suite (run with mailtrap/MailHog)
3. CI runner whitelisted để send → CI cohort

---

## Selectors calibration workflow

Lần đầu user manual-run per Plan 1 follow-along guide → ghi nhận thực tế selectors + flow timing. Sau đó:

1. Update `full-flow.spec.ts` `data-testid` hoặc role-based selectors khớp với FE thực
2. Bỏ `test.skip` annotation từng bước khi selectors verified
3. Add screenshots assertions cho visual regression

Until calibrated, specs ở trạng thái **scaffold (skipped)** — không run mà cũng không block CI.

---

## Related

- [Plan 1 self-test guide](../../../../documents/03-planning/end-user/plan-1-self-test-e2e.md)
- [e2e/beta-funnel/](../beta-funnel/) — sister MSW-mocked specs (GAP-404 + GAP-455)
- [playwright.config.ts](../../playwright.config.ts) — shared config
