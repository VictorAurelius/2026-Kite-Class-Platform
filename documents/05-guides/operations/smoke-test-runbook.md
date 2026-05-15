# Smoke Test Runbook — Bucket G (Wave 85)

**Status:** ACTIVE
**Last Updated:** 2026-05-15
**Related:** GAP-475 (Wave 85 Bucket G), `release-deploy-standard.md` §3.1, `pre-handoff-self-test-completeness.md`

---

## 1. Tổng quan

Bucket G ship 6 smoke script bao phủ 6 kịch bản pre-launch + 4 AC tăng cường (Bucket A simulation):

| Script | Phạm vi | AC covered |
|---|---|---|
| `smoke-login-happy-path.sh` | E2E BE+FE login + JWT + redirect | `pre-handoff-self-test §2.1` |
| `smoke-email-verify-loop.sh` | Signup → email → verify link → state flip | `pre-handoff-self-test §2.3` |
| `smoke-2fa-totp.sh` | TOTP enroll + verify + window rotation | `pre-handoff-self-test §2.10` |
| `smoke-p95-latency-k6.sh` | k6 50 VUs × 5m, P95 < 500ms + OOM check | G-AC2 (OOM 10-tenant load) |
| `smoke-migration-rollback.sh` | V49 → V48 → V49 cycle (manual undo SQL pattern) | DR readiness |
| `smoke-tenant-isolation-rls.sh` | RLS multi-tenant + NULL session + audit log immutable | G-AC1 + G-AC3 + G-AC4 |
| `smoke-rollback-cycle.sh` (đã có Wave 63) | ECS rollback + smoke + restore cycle | `release-deploy-standard.md §4.4` |

---

## 2. Cách dùng

### 2.1 Dry-run (default)

Mọi script chạy `dry-run` mặc định — KHÔNG hit network/DB:

```bash
bash scripts/smoke-login-happy-path.sh
bash scripts/smoke-email-verify-loop.sh
bash scripts/smoke-2fa-totp.sh
bash scripts/smoke-p95-latency-k6.sh
bash scripts/smoke-migration-rollback.sh
bash scripts/smoke-tenant-isolation-rls.sh
```

Dry-run validate:
- Script syntax + shellcheck
- Dependency availability (k6, psql, oathtool/pyotp)
- Endpoint danh sách + flow phases

### 2.2 Execute (staging only)

Mỗi script REFUSE production hosts. `--execute` yêu cầu host = `*staging*` hoặc `localhost`.

```bash
# Login smoke
SMOKE_BASE_URL=https://staging.kitehub.vn \
SMOKE_USER=qa@kitehub.me SMOKE_PASS=*** \
  bash scripts/smoke-login-happy-path.sh --execute

# Email verify (signup tạo tài khoản mới)
SMOKE_BASE_URL=https://staging.kitehub.vn \
SMOKE_EMAIL=qa+$(date +%s)@kitehub.me \
  bash scripts/smoke-email-verify-loop.sh --execute

# TOTP (cần admin JWT đã enroll 2FA)
SMOKE_ADMIN_JWT=eyJ... \
  bash scripts/smoke-2fa-totp.sh --execute

# k6 load (cần k6 installed)
SMOKE_BASE_URL=https://staging.kitehub.vn \
  bash scripts/smoke-p95-latency-k6.sh --execute

# Migration rollback (staging DB)
PG_HOST=staging-kitehub-rds.amazonaws.com PG_USER=qa \
PGPASSWORD=*** bash scripts/smoke-migration-rollback.sh --execute

# RLS tenant isolation
PG_HOST=staging-kitehub-rds.amazonaws.com PG_USER=qa \
PGPASSWORD=*** bash scripts/smoke-tenant-isolation-rls.sh --execute
```

### 2.3 CI workflow

Trigger qua GitHub Actions:

```bash
# Dry-run mọi script (default)
gh workflow run smoke-tests.yml

# Execute scenario cụ thể trên staging (cần secret pre-configured)
gh workflow run smoke-tests.yml -f mode=execute -f scenario=login
```

Workflow auto-run dry-run trên PR khi diff touches `scripts/smoke-*.sh` hoặc workflow YAML.

---

## 3. Cadence

| Script | Tần suất | Trigger |
|---|---|---|
| Tất cả 6 dry-run | Mỗi PR touching smoke-* | CI auto |
| Login + email-verify + TOTP | Pre-release tag (per `release-deploy-standard.md §3.1`) | Manual workflow_dispatch |
| k6 P95 + OOM | Weekly (staging) | Cron defer GAP-475 follow-up |
| Migration rollback | Monthly maintenance window | Manual |
| RLS isolation | Post-Bucket-B merge + quarterly | Manual |

---

## 4. AC coverage matrix

| AC | Script | Phase | Status |
|---|---|---|---|
| G-AC1 cross-tenant data leak | `smoke-tenant-isolation-rls.sh` | Phase 1 (10 tenants × isolation probe) | ✅ shipped |
| G-AC2 OOM 10-tenant concurrent | `smoke-p95-latency-k6.sh` | k6 + post-run dmesg/docker logs | ✅ shipped (manual OOM verify) |
| G-AC3 RLS NULL session var | `smoke-tenant-isolation-rls.sh` | Phase 2 (RESET app.current_tenant_id) | ✅ shipped |
| G-AC4 admin_audit_logs UPDATE/DELETE | `smoke-tenant-isolation-rls.sh` | Phase 3 (mutation rejection) | ✅ shipped |

---

## 5. Troubleshooting

| Triệu chứng | Cause | Fix |
|---|---|---|
| `[ABORT] --execute refuses non-staging host` | Host không match `*staging*` / localhost | Đúng — set `SMOKE_BASE_URL` về staging |
| `psql not installed` | Local missing | `sudo apt install postgresql-client` |
| `k6 not installed` | Local missing | `https://k6.io/docs/getting-started/installation/` |
| `TOTP generator failed` | Thiếu oathtool/pyotp | `sudo apt install oathtool` OR `pip install pyotp` |
| `Could not fetch tenant IDs` | RLS chặn superuser query | Set `app.current_tenant_id` trước hoặc grant bypass |
| `UPDATE on admin_audit_logs SUCCEEDED` | Immutability trigger missing | File P0 — implement BEFORE/INSTEAD OF trigger |

---

## 6. Liên kết

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md` §3 Bucket G
- GAP-475: `documents/04-quality/gaps/open/GAP-475-*.md`
- Rule: `.claude/rules/release-deploy-standard.md` §3.1 Smoke test script mandate
- Rule: `.claude/rules/pre-handoff-self-test-completeness.md` §2 flow classes
- Sister script: `scripts/smoke-rollback-cycle.sh` (Wave 63 GAP-477)
