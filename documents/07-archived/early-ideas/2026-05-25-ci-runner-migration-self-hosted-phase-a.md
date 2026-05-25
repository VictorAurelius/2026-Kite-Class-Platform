---
title: CI runner migration plan — Phase A migrate 8 CI workflow sang self-hosted
date: 2026-05-25
status: deferred-for-next-session
audience: dev
tags: [ops, ci-cd, self-hosted-runner, github-actions, queue-reduction]
---

# CI runner migration plan — Phase A (8 CI workflow → self-hosted)

## Bối cảnh

User flagged 2026-05-25: "GitHub liên tục đưa CI vào queue, mất thời gian chờ". GitHub-hosted free tier private repo có giới hạn 20 concurrent jobs → khi nhiều PR cùng lúc (vd Wave thesis-1 8 PR × 80 job = 640 job concurrent) → queue dài.

Phương án A đã chốt: **migrate 8 CI workflow "an toàn" sang self-hosted**, giữ deploy/external workflows trên `ubuntu-latest` (cần GitHub OIDC + Vercel/Cloudflare secrets).

## Trạng thái hiện tại (2026-05-25)

- 2 self-hosted runner online (v2.334.0):
  - `kite-dev-wsl-runner` (PID 283) — original
  - `NguyenVanKiet-runner-2` (PID 29293) — systemd service enabled
- 22 workflow job `runs-on: [self-hosted, Linux, X64]` đang chạy local
- 58 workflow job `runs-on: ubuntu-latest` đang chạy GitHub cloud
- RAM total 9.7GB, available 3.5GB, 2 runner idle ~250MB

## Scope Phase A — 8 workflow file migrate

| Workflow file | Lý do safe to migrate | Java/pnpm heavy? |
|---|---|---|
| `.github/workflows/core-ci.yml` | Java/Maven test thuần — không OIDC | ⚠️ Yes (~2GB peak) |
| `.github/workflows/frontend-ci.yml` | pnpm test/build — không OIDC | ⚠️ Yes (~1GB peak) |
| `.github/workflows/kitehub-frontend-ci.yml` | pnpm test/build | ⚠️ Yes (~1GB peak) |
| `.github/workflows/kitehub-ci.yml` | Java/Maven test | ⚠️ Yes (~2GB peak) |
| `.github/workflows/actionlint.yml` | YAML lint thuần | ✅ No |
| `.github/workflows/gitleaks-scan.yml` | Secret scan thuần | ✅ No |
| `.github/workflows/ci-cleanup.yml` | Admin cleanup gh CLI calls | ✅ No |
| `.github/workflows/script-quality.yml` (partial — đa số jobs đã self-hosted; sweep còn lại) | ShellCheck + Ruff + Python script tests | ✅ No |

## Workflow KHÔNG migrate (giữ ubuntu-latest)

| Workflow | Reason |
|---|---|
| `deploy-production.yml` | GitHub OIDC role assume `arn:aws:iam::*:role/kitehub-deploy-role` |
| `deploy-staging.yml` | GitHub OIDC role assume staging |
| `deploy-design-system.yml` | Vercel/storybook deploy — Vercel-specific env |
| `terraform-plan.yml` | OIDC role assume `kitehub-readonly-plan-role` |
| `cloudflare-apex-cutover.yml` | Cloudflare API token + DNS verification |
| `ec2-bootstrap.yml` | One-time bootstrap, sensitive |
| `release-tag.yml` | Release pipeline với git tag signing |
| `restore-drill.yml` | Disaster recovery — needs production AWS access |
| `rollback.yml` | Production rollback workflow |
| `e2e-pre-release.yml` | Playwright cần Chrome browser install (Phase B candidate) |
| `lighthouse.yml` | Lighthouse audit Chrome (Phase B) |
| `zap-baseline.yml` | OWASP ZAP scan (Phase B) |
| `smoke-tests.yml` | Production smoke (Phase B sau khi deploy ổn) |

## RAM constraint analysis

Worst case concurrent merge train (vd Wave thesis-1 8 PR):
- 2 runner local × 1 Java build mỗi = ~4GB → **OK** (available 3.5GB tight)
- 2 runner local × 1 Java build + 2 lint job concurrent = ~5GB → **TIGHT** (risk OOM)
- 2 runner local × 2 Java build concurrent = ~8GB → **OOM RISK HIGH**

**Mitigation:** Sau migrate Phase A, monitor RAM 1-2 wave. Nếu OOM:
- Option 1: thêm `concurrency:` group trên Java-heavy workflows (limit 1 concurrent per repo)
- Option 2: thêm runner #3 chỉ cho non-Java jobs (lint/script-quality) → 3 runner total, OOM-safe
- Option 3: revert 4 Java-heavy workflow (core-ci/kitehub-ci/frontend-ci/kitehub-frontend-ci) → giữ 4 lint workflow self-hosted only

## Migration steps (next session)

1. **Backup current workflow** state:
   ```bash
   git checkout -b chore/ci-migrate-phase-a-self-hosted
   ```

2. **Edit 8 workflow files** — replace `runs-on: ubuntu-latest` → `runs-on: [self-hosted, Linux, X64]`:
   - Một số workflow có nhiều job, mỗi job có riêng `runs-on:` → edit từng job
   - Một số job đã `runs-on: [self-hosted, ...]` → bỏ qua

3. **Verify local capability** trước commit:
   - Java tests: `cd kitehub && ./mvnw verify -P strict-warnings` local PASS
   - Frontend tests: `pnpm test --run` local PASS
   - ShellCheck + Ruff: chạy script tests local
   - Đảm bảo các tool cần thiết (Java 21, Maven 3.9, Node 22, pnpm 9, shellcheck, ruff) đã install trên runner machine

4. **Concurrency guard** — add vào Java-heavy workflows:
   ```yaml
   concurrency:
     group: ${{ github.workflow }}-${{ github.ref }}
     cancel-in-progress: true
   ```

5. **Commit + PR** với scope rõ ràng:
   - Title: `chore(ci-phase-a): migrate 8 CI workflow sang self-hosted runner`
   - Body: cite phương án A + 8 file list + RAM constraint mitigation

6. **Post-merge monitor** 2-3 wave merge train:
   - Track RAM trên dev machine khi merge train chạy
   - Track CI run duration trước/sau migrate
   - Track GitHub Actions free tier minutes saved

7. **Phase B candidate (sau Phase A ổn ≥7 ngày)**:
   - Install Chrome/Chromium trên runner machine
   - Install Playwright browsers
   - Migrate 4 file: e2e-pre-release + lighthouse + zap-baseline + smoke-tests

## Expected outcomes

- **GitHub free tier minutes saved:** ~70% (58 ubuntu-latest jobs → 22 retained, 36 migrated)
- **Queue reduction:** đa số CI job chạy ngay không đợi GitHub cloud slot
- **Trade-off:** Local machine load tăng; OOM risk khi merge train heavy
- **Security boundary preserved:** Deploy/OIDC workflows giữ ubuntu-latest → AWS production access không qua local creds

## Open questions cho next session

1. RAM monitoring tool — dùng `htop` realtime hay log periodically?
2. Concurrency limit per workflow — `cancel-in-progress: true` (lose progress) hay `cancel-in-progress: false` (queue)?
3. Phase A scope clarification — script-quality.yml có nhiều job (~10 job) cần check từng job đã self-hosted chưa hay vẫn ubuntu-latest mix
4. Runner #3 add-on contingency — preemptive thêm runner #3 cho non-Java jobs, hay chờ OOM thực tế?

## References

- Runner setup runbook: `/home/nguyenvankiet/actions-runner/` (PID 283) + `/home/nguyenvankiet/actions-runner-2/` (systemd service)
- GitHub free tier docs: https://docs.github.com/en/billing/managing-billing-for-github-actions/about-billing-for-github-actions
- Concurrency control: https://docs.github.com/en/actions/using-jobs/using-concurrency
- Sister consideration: `.claude/rules/release-deploy-standard.md` §9 — deploy execution human-triggered workflow_dispatch + OIDC pattern (KHÔNG migrate)
- Concurrent mutation rule: `.claude/rules/concurrent-production-mutation-ops.md` — production mutation ops always serial (workflow concurrency-aware)
