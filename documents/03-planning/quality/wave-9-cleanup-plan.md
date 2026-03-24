# Wave 9 — CI Fix, Security, Audit, Cleanup

**Date:** 2026-03-24
**Priority:** P0 (CI broken) + P1 (security + audit)

---

## PR List

### PR-1: Fix CI Docker Build [P0 — CI BROKEN]

Docker workflow references deleted `docker/kiteclass/Dockerfile.*`.
- [ ] Update `.github/workflows/docker-build-push.yml` — change Dockerfile path from `docker/kiteclass/Dockerfile.${{ matrix.service }}` to `kiteclass/kiteclass-${{ matrix.service }}/Dockerfile`
- [ ] Verify all 3 services (core, gateway, frontend) have correct Dockerfile paths

### PR-2: Security — Remove .env from git [P0]

`kitehub/.env` tracked with ENCRYPTION_MASTER_KEY + JWT_SECRET.
- [ ] `git rm --cached kitehub/.env`
- [ ] Add `kitehub/.env` to `.gitignore` (verify pattern)
- [ ] Verify `.env.example` still exists as template

### PR-3: Quality Audit Rerun [P1]

Skills fixed, 16 business docs created — need accurate baseline.
- [ ] Run `/quality-audit kitehub` → new report
- [ ] Run `/quality-audit kiteclass` → new report
- [ ] Run `/business-gap-check kitehub` → verify gaps closed
- [ ] Run `/business-gap-check kiteclass` → verify gaps closed
- [ ] Save reports to `documents/04-quality/`

### PR-4: Update Plans + Thesis Refs [P1]

- [ ] Update `wave-9-cleanup-plan.md` completion status
- [ ] Update `quality-plan-v4-final-push.md` — mark Wave 8 done
- [ ] Update `08-thesis/references/testing-results.md` — 16 business docs, 9 services
- [ ] Update `08-thesis/references/quality-metrics.md` — new scores

### PR-5: Cleanup Stale Branches [P2]

- [ ] Delete remote: wave/3, wave/6, wave/8, fix/*, chore/*
- [ ] Verify no open PRs on those branches

### PR-6: Alert Rules Complete [P2]

Design doc has 7+ rules, actual has 3-4.
- [ ] Add missing: high memory, DB pool exhaustion, payment failure rate
- [ ] Match `documents/03-planning/infrastructure/monitoring-observability.md` design

---

## Execution

| Agent | PR | Conflict risk |
|-------|-----|---------------|
| 1 | PR-1 + PR-2 | None (CI + .env) |
| 2 | PR-3 | None (documents/04-quality/) |
| 3 | PR-4 + PR-5 + PR-6 | None (documents + infra) |

---

## Completion Criteria

- [ ] CI all green (5/5 workflows pass)
- [ ] kitehub/.env removed from git
- [ ] Quality audit reflects true state
- [ ] 0 stale remote branches
- [ ] Alert rules match design doc
