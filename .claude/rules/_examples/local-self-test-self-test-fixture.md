# local-self-test-before-aws-deploy — Self-Test Fixture

Companion to `.claude/rules/local-self-test-before-aws-deploy.md` §8 Self-test fixture mandate (per `rule-change-process.md` §6.5 Enforcement Parity Mandate Stage 4 self-test).

3 synthetic PR body scenarios + expected rule verdict. Demonstrates rule §3 evidence requirement fires correctly across PASS / FAIL / WARN axis. Reviewer reference khi applying §7.1 checklist.

---

## Fixture 1 — PASS (PR body includes local self-test evidence)

### PR body excerpt

```markdown
## Summary

feat(wave-XXX-Z): production hotfix admin login 500 (GAP-NNN)

## Local self-test evidence

- Profile run: `bash kitehub/scripts/up.sh --profile full`
- Services healthy: 8/8 (kite-postgres + kite-redis + kite-rabbitmq + kite-minio + kitehub-platform + kitehub-subscription + kitehub-frontend + kiteclass-core Up 60s healthy)
- Admin login flow per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g):
  - (a) Credential available: `admin@kitehub.me / <password>` cited trong PR body
  - (b) curl POST /api/auth/login → HTTP 200 + JWT in response body ✓
  - (c) Login UI → redirect /admin ✓ (browser DevTools confirmed)
  - (d) Admin role-guard accepts seeded `PLATFORM_ADMIN` role ✓
  - (e) Sidebar contains link `/admin/beta-requests` ✓
  - (f) `/admin/beta-requests` page renders với 3 pending requests ✓
  - (g) Approve action POST /api/v1/admin/beta-requests/1/approve → HTTP 200 + UI updates ✓
- Smoke script: manual walk evidence cited per §2.4 (scripts/local/smoke-e2e.sh chưa tồn tại Wave 102.8 — manual walk acceptable substitute per rule §3 Step 2 note v1.0.0)

## Triggered AWS workflow

- `gh workflow run deploy-production.yml -f environment=production`
- Run ID: 12345678
```

### Rule verdict

**§7.1 reviewer-checklist:**
- [x] §3 Step 3 `## Local self-test evidence` section present
- [x] Stack-up evidence cited (profile + services healthy 8/8)
- [x] Admin walk evidence cited per §2.4 (a)→(g) all 7 rows
- [x] Smoke script OR manual walk evidence (manual walk cited acceptable v1.0.0)

**Outcome:** ✅ PASS — rule §3 evidence requirement satisfied; reviewer approves merge với AWS deploy workflow trigger documented.

---

## Fixture 2 — FAIL (PR body absent local evidence + no override trailer)

### PR body excerpt

```markdown
## Summary

feat(wave-XXX-Z): production hotfix admin login 500 (GAP-NNN)

Fixed admin login 500 via JWT secret rotation + role-guard literal sync.

## Test plan

- [ ] Verify post-deploy admin login works on production
- [ ] Verify role-guard accepts seeded role
- [ ] Verify session storage migration complete

## Triggered AWS workflow

- `gh workflow run deploy-production.yml -f environment=production`
- Run ID: 12345679
```

### Commit body excerpt

```
feat(wave-XXX-Z): production hotfix admin login 500

Fixed JWT secret rotation + role-guard literal sync.
```

### Rule verdict

**§7.1 reviewer-checklist:**
- [ ] §3 Step 3 `## Local self-test evidence` section MISSING
- [ ] Stack-up evidence NOT cited
- [ ] Admin walk evidence NOT cited
- [ ] No `LOCAL_SMOKE_SKIP:` trailer trong commit body

**Outcome:** ❌ FAIL — rule §3 evidence requirement violated; reviewer BLOCKS merge.

**Reviewer remediation message:**

> ❌ Local self-test evidence missing per `local-self-test-before-aws-deploy.md` §3.
>
> Required actions:
> 1. Run `bash kitehub/scripts/up.sh --profile full` + verify services healthy
> 2. Walk admin login flow per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) — cite evidence each row
> 3. Add `## Local self-test evidence` section to PR body per §3 Step 3
>
> OR add `LOCAL_SMOKE_SKIP: <step> — <reason>` trailer + `LOCAL_SMOKE_FOLLOWUP: <gap-link>` per §5 override mechanism.

---

## Fixture 3 — WARN (PR body absent local evidence + `LOCAL_SMOKE_SKIP:` trailer với reason + follow-up gap)

### PR body excerpt

```markdown
## Summary

feat(wave-XXX-Z): P0 production incident — admin login 500 hotfix (GAP-NNN)

Emergency hotfix cho production admin login 500. Local Docker broken (WSL2 distro stopped sau host reboot, `kitehub/scripts/check-docker.sh` reports Docker daemon unreachable; auto-launch via `powershell.exe` failed 3 retries — escalated user manual fix queued GAP-MMM).

## Triggered AWS workflow

- `gh workflow run deploy-production.yml -f environment=production`
- Run ID: 12345680

## Post-deploy verification plan

- (a) Real-time CloudWatch logs cho admin login attempts
- (b) Post-deploy smoke `scripts/smoke-test.sh` exit 0
- (c) Backfill local self-test trong follow-up GAP-MMM session within 24h
```

### Commit body excerpt

```
feat(wave-XXX-Z): P0 prod admin login 500 hotfix

P0 production incident — admin login 500 affecting all admin users. Hotfix
deployed bypass local self-test per release-fix-retry-budget.md §5 tooling-fix
exception (local Docker broken, fix queued GAP-MMM).

LOCAL_SMOKE_SKIP: stack-up — P0 prod incident, local Docker broken (WSL2 distro stopped post host reboot, auto-launch failed 3 retries), fix queued GAP-MMM
LOCAL_SMOKE_FOLLOWUP: GAP-MMM — Docker WSL2 distro auto-recovery + local self-test backfill within 24h post-incident-resolution
```

### Rule verdict

**§7.1 reviewer-checklist:**
- [ ] §3 Step 3 `## Local self-test evidence` section ABSENT
- [ ] BUT: `LOCAL_SMOKE_SKIP:` trailer present với reason
- [ ] AND: `LOCAL_SMOKE_FOLLOWUP:` trailer references follow-up gap GAP-MMM với explicit completion window
- [ ] §5 override conditions met: P0 production incident + tooling-state blocker + follow-up tracked

**Outcome:** ⚠️ WARN (allowable per §5 override mechanism) — rule §3 evidence deferred via formal trailer; reviewer approves merge với commitment to backfill local self-test within follow-up GAP-MMM 24h window.

**Quarterly retro logging:** trailer counted toward §5 "pattern frequency >5%/quarter triggers meta-review" tally. Nếu 3+ override events trong quarter → file meta-gap reviewing rule scope (likely local stack health regression class needs dedicated fix).

---

## Verdict — fixture coverage

| Axis | Fixture | Verdict | Reviewer action |
|---|---|---|---|
| Compliant evidence | Fixture 1 | ✅ PASS | Approve + merge |
| Absent evidence + no override | Fixture 2 | ❌ FAIL | Block + remediation message |
| Absent evidence + valid override | Fixture 3 | ⚠️ WARN | Approve + log toward §5 retro tally |

**3 scenarios cover full §3 evidence requirement axis + §5 override mechanism + §7.1 reviewer-checklist application.** Rule fires correctly trên all 3 axis.

**Counterfactual without rule:** PR Fixture 2 (absent evidence) ships → production hotfix deploy → 500 không catch local → user round-trip + retro gap filings (GAP-518/519 pattern recurrence). Rule catches at reviewer-checklist time → 0 round-trip cost.

---

## Cross-link

- Rule: `.claude/rules/local-self-test-before-aws-deploy.md` §3 evidence + §5 override + §7.1 reviewer-checklist
- Sister rule: `.claude/rules/pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (a)→(g)
- Sister rule: `.claude/rules/release-fix-retry-budget.md` §5 row "Tooling-fix-then-retry" — override mechanism precedent
- Master pipeline: `.claude/rules/incident-to-rule-pipeline.md` Stage 4 self-test mandate
- Enforcement parity: `.claude/rules/rule-change-process.md` §6.5
