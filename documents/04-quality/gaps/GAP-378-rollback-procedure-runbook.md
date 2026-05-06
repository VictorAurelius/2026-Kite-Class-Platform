# GAP-378: Rollback Procedure Runbook (detailed)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA + 1.5 PAID)
**Domain:** DevOps / Operations
**Found:** 2026-05-06 (Release 1 deploy plan §5)
**Affects:** Recovery time during incidents, reduce MTTR

## Problem

Release 1 deploy plan §5 has high-level rollback procedure (8 steps). Cần detailed runbook với:
- Specific commands
- Pre-conditions verification
- Per-stack rollback (Helm, DB, DNS, Cloudflare)
- Validation checks at each step
- Communication scripts

## Proposed Fix

`documents/05-guides/operations/runbooks/rollback-runbook.md`:

```markdown
# Rollback Runbook

## When to rollback (triggers)
- Critical bug affecting >10% tenants in first 24h
- Database corruption / data loss
- Authentication completely broken
- Payment processor failures (>50% transactions failed)
- Performance degradation > 2× baseline P95

## Decision authority
- Solo dev mode: coordinator decides + executes
- Future: on-call engineer + tech lead approval

## Pre-rollback checklist
1. Confirm trigger met (per above)
2. Notify status page (incident created)
3. Email tenants: "Maintenance for X minutes"
4. Backup current state (snapshot trước rollback)
5. Identify previous working version (git tag)

## Rollback steps (detailed)

### Step 1: Application rollback
```bash
# Helm-managed (if K8s)
helm history kitehub --namespace kitehub | head -5
helm rollback kitehub <previous-revision> --namespace kitehub --wait

# OR Docker-compose (Oracle Cloud)
ssh ubuntu@<oracle-vm>
cd /opt/kite
docker-compose -f docker-compose.beta.yml down
git checkout v0.9.0-beta  # or previous stable tag
docker-compose pull
docker-compose up -d
```

### Step 2: Database rollback (if schema change)
```bash
# Identify migrations to rollback
docker exec kitehub-db psql -U postgres -d kitehub \
  -c "SELECT version, description FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5;"

# Apply rollback SQL (prepared pre-deploy)
docker exec kitehub-db psql -U postgres -d kitehub < rollback-vN.sql

# OR pg_restore from snapshot (if data corruption)
pg_restore --clean --if-exists -h <host> -U postgres -d kitehub <snapshot-file>
```

### Step 3: DNS rollback
```bash
# Cloudflare API revert
curl -X PATCH "https://api.cloudflare.com/.../dns_records/<id>" \
  -H "Authorization: Bearer $CF_API_TOKEN" \
  -d '{"content": "<previous-ip>"}'

# Wait propagation (TTL 5 min)
dig kitehub.vn +short
```

### Step 4: Cache invalidation
- Cloudflare: purge cache via dashboard or API
- Application caches: restart Redis or selective key delete

### Step 5: Smoke test rolled-back version
```bash
./scripts/smoke-test.sh https://kitehub.vn https://kiteclass.vn
# Expect: all 15+ assertions pass
```

### Step 6: Communicate to tenants
- Status page: update incident với rollback complete
- Email: "Service restored to vX.Y.Z; investigation ongoing"
- Beta tenants: dedicated message about beta period extension if needed

### Step 7: Post-incident review (within 48h)
- Per `output-review-mandate.md` §6
- Root cause analysis
- Action items + assigned owner
- Update runbook with lessons learned

## Per-component rollback specifics

### Frontend (Next.js)
- Static assets versioned via build hash (no rollback needed for old visitors)
- Service worker cache: bust via version bump

### Backend (Spring Boot)
- Multi-instance: rolling rollback (one pod at a time)
- DB connection pool: graceful drain

### AI Branding
- Cached generated assets: keep (don't invalidate)
- Pending generation jobs: cancel + notify users

### Email transactional
- In-flight emails: let complete
- New emails: route to fallback provider if main provider down

### Payment processor
- In-flight transactions: monitor; refund manually if stuck
- Webhook handler: idempotent retry

## Validation post-rollback
- [ ] All public pages load
- [ ] Tenant login works
- [ ] Existing data accessible
- [ ] Smoke test passes
- [ ] Status page shows green
- [ ] Tenant complaints subside

## Recovery (post-rollback)
- Investigate root cause (logs, traces, audit trail)
- Fix in branch off rolled-back version
- Test thoroughly trên staging
- Re-deploy với incremental validation
- Document learnings in retro

## Communication templates
[email templates for tenant comms in different scenarios]
```

## Acceptance Criteria

- [ ] `documents/05-guides/operations/runbooks/rollback-runbook.md` created
- [ ] 7-step procedure documented với specific commands
- [ ] Per-component rollback specifics
- [ ] Validation checklist
- [ ] Communication templates (email/status page)
- [ ] Recovery flow post-rollback
- [ ] Cross-link from `release-1-deploy-plan.md` §5
- [ ] Smoke test integration (auto-trigger rollback option)

## Effort estimate

~1 ngày docs + ~1 ngày scripts.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` §5
- Existing runbook: `documents/05-guides/operations/runbooks/deployment-procedures.md` (generic)
- Sister: GAP-377 (smoke test triggers rollback)

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. STRONGLY recommend cho recovery confidence + reduce MTTR.
