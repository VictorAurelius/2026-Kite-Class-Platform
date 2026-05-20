# GAP-694: Local self-test investigation + fix (Docker Desktop / WSL2 stack)

**Status:** 🟡 PARTIAL (15% — Phase 0A investigation SHIPPED 2026-05-21; Phase 0B fix + Phase 0C rule pending)
**Priority:** 🔴 P0 (META — blocks Item 4 mandate + rebuild SOP Gate 2/3)
**Domain:** DevOps
**Detected:** 2026-05-21
**Related PRs:** TBD
**Related Docs:** GAP-612 escalation context; `pre-handoff-self-test-completeness.md`; outside-in audit synthesis 2026-05-21

## Current State (verified 2026-05-21)

> Per `audit-to-gap-pipeline.md` §2.5 state-check. Outside-in persona-simulation agent (2026-05-21 synthesis) flagged dev drop-off #2 + #3 — local stack drift + no automated smoke. Root cause UNKNOWN — needs investigation.

| Piece | File / Path | Status |
|-------|-------------|--------|
| Docker scripts | `kitehub/scripts/up.sh` / `down.sh` / `logs.sh` / `status.sh` | ✅ shipped per `CLAUDE.md` §Docker Scripts |
| Canonical compose | `kitehub/docker-compose.kitehub.yml` | ✅ shipped |
| Smoke script (post-deploy AWS) | `scripts/smoke-test.sh` | ✅ shipped (GAP-377 closed) |
| **Local Playwright smoke** | `scripts/local/smoke-e2e.sh` | ❌ missing |
| **Local stack preflight** | `scripts/local/preflight-stack.sh` | ❌ missing |
| **Local self-test SOP doc** | `documents/05-guides/local-dev/local-self-test-sop.md` | ❌ missing |
| Production env config registry | `.claude/rules/production-env-config-registry.md` | ✅ shipped (covers application*.yml suspect defaults) |
| Existing dev-stack class issues | `GAP-244` dev-stack schema mismatch (status unknown — pre-flight before fix) | ⚠️ partial — needs re-verify |

**Grep commands run:**
```bash
grep -rl "smoke-e2e\|preflight-stack" scripts/ 2>/dev/null  # 0 results — script chưa tồn tại
find documents/05-guides/local-dev -iname "*self-test*" 2>/dev/null  # 0 results — SOP doc missing
ls -la kitehub/docker-compose*.yml  # canonical compose exists
```

## Problem

Solo dev hiện KHÔNG self-test được full stack local (Docker Desktop / WSL2). Root cause chưa rõ — candidate hypotheses:

1. **Port conflict** — port 5433/5432 (postgres) / 5672 (rabbit) / 6379 (redis) / 9000 (gateway) chiếm bởi orphan container session trước
2. **Env var missing** — `.env` files thiếu secrets (JWT_SECRET, AWS keys) khi compose up
3. **Flyway baseline mismatch** — V60 RLS migration (GAP-614 false-positive — não exists) hoặc V-N migration drift giữa local vs prod schema
4. **WSL2 performance** — disk I/O chậm khiến Spring Boot startup timeout / probe fail
5. **docker-compose missing service** — services CI-built đầy đủ nhưng local compose chỉ ship 5/6 services
6. **JVM heap / memory limits** — WSL2 default memory cap (8GB) không đủ cho 6 services + frontends concurrent

Per outside-in persona simulation 2026-05-21: dev (anh Kiệt) reach Step 2 trong rebuild flow → `docker-compose up` fail (port 5433 in use, Flyway baseline error, NoSuchBeanDefinitionException) → not actionable error → fatigue → skip self-test → deploy AWS với bugs latent.

## Context

Per Item 4 outside-in synthesis 2026-05-21 (user-chosen sequencing "Phase 0 local self-test fix → Item 2 refactor → rebuild"), Phase 0 fix là PREREQUISITE để codify "local self-test mandatory" rule. Nếu skip → rule = aspirational fiction (per `incident-to-rule-pipeline.md` advisory-rule guard).

Impact:
- 🔴 **Beta tenant velocity** — mỗi rebuild/deploy cycle thiếu local gate → role-guard mismatch (Wave 71b pattern) + CORS misconfig (Wave 82) + env wire bug (Wave 81 JWT) escape to prod
- 🔴 **Solo-dev fatigue cost** — manual click smoke 5+ phút/cycle × 5 cycles/day = 25 min/day wasted vs automated 30s
- 🔴 **Item 4 mandate blocker** — không có working local smoke → rule "local self-test trước AWS deploy" không enforceable

## Evidence

- Outside-in synthesis 2026-05-21 (this gap origin) — agent persona simulation Step 2 drop-off
- `feedback_audit_of_trust_pass.md` (memory) — recurrence #4 "AC [x] ≠ production-verified" pattern
- `pre-handoff-self-test-completeness.md` §2.4 — admin-flow checklist requires browser walkthrough; current state = manual only
- CLAUDE.md §Docker Scripts — scripts exist nhưng smoke automation missing

## Proposed Fix

### Phase 0A — Investigate (1-2 hours, agent-spawned)

Spawn investigation agent với scope:
1. Run `./kitehub/scripts/up.sh` từ clean state → capture full log + identify first failure
2. Enumerate ports in use: `ss -tlnp | grep -E ':(5432|5433|5672|6379|9000|3000|3010|8080)'`
3. Verify `.env` template completeness: diff `.env.production.template` vs required env vars in `application*.yml` (cross-ref `production-env-config-registry.md` suspect defaults)
4. Verify Flyway baseline: `docker exec kite-postgres psql -U kite -d kitehub -c "SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10"` (if container up) HOẶC manual schema dump diff vs target migrations
5. Check WSL2 resources: `cat /proc/meminfo` + `df -h /mnt/wsl/docker-desktop`
6. Enumerate compose services vs CI build matrix: `grep -E "^  [a-z-]+:" kitehub/docker-compose.kitehub.yml | wc -l` vs `.github/workflows/docker-build-push.yml` service count

Output: `documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md` với root-cause analysis + fix recommendations ranked by leverage.

### Phase 0B — Fix top 3 root causes (~1 day)

Based on 0A findings, ship same-PR:
- Fix top 3 highest-leverage root causes (likely: port cleanup script + env template completeness + Flyway baseline doc)
- Ship `scripts/local/preflight-stack.sh` — check ports/env/Flyway/memory before `up.sh`; print actionable errors
- Ship `scripts/local/smoke-e2e.sh` skeleton — Playwright headless walk admin login → P2 onboarding → P3 manager (≤5 min target runtime)
- Ship `documents/05-guides/local-dev/local-self-test-sop.md` — SOP for dev re-onboarding + new-machine setup

### Phase 0C — Codify rule (~30 min)

After 0A+0B working: ship `.claude/rules/local-self-test-before-aws-deploy.md` v1.0.0 với:
- Mandate `bash scripts/local/smoke-e2e.sh` PASS before any AWS deploy workflow trigger
- Override trailer `LOCAL_SMOKE_SKIP: <reason + follow-up gap>` per `incident-to-rule-pipeline.md` pattern
- Pattern frequency >2/week skip triggers meta-review

## Acceptance Criteria

- [ ] Investigation audit artifact landed (`documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md`)
- [ ] `./kitehub/scripts/up.sh` succeeds clean from cold state (6 services + 2 frontends healthy `actuator/health` 200)
- [ ] `scripts/local/preflight-stack.sh` shipped + tested (port/env/Flyway/memory checks; actionable errors)
- [ ] `scripts/local/smoke-e2e.sh` shipped + Playwright admin+P2+P3 flow PASS in ≤5 min local
- [ ] `documents/05-guides/local-dev/local-self-test-sop.md` written + dev re-onboarding tested
- [ ] `.claude/rules/local-self-test-before-aws-deploy.md` v1.0.0 codified với override mechanism
- [ ] Self-test artifact: rule applied retroactively to Wave 71b admin-login incident → would have caught role-guard mismatch locally

## Related

- **GAP-612** AWS account suspension (parent gap; this is Phase 0 prerequisite)
- **GAP-693** AWS rebuild SOP playbook (downstream gap; depends on this DONE)
- **GAP-692** hardcoded refactor (parallel work; can ship before/after)
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (what local smoke verifies)
- `.claude/rules/production-env-config-registry.md` (env coverage audit — informs preflight-stack.sh checks)
- `.claude/rules/agent-action-bias.md` §1 Part A (do it yourself, automation > manual)
- `meta-gap-priority.md` §3 — Meta-P0 force-multiplier (every future rebuild/deploy benefits)
- Outside-in audit synthesis 2026-05-21 (this gap origin)
- **[`GAP-695`](GAP-695-self-test-readiness-comprehensive-plan.md)** — parent catalog cho self-test readiness (this gap = Tier 0 sub-scope: Docker + .env + preflight); Tier 1-3 execution paths enumerate trong GAP-695 §Current State catalog

## Log

- **2026-05-21 (renumbered + Phase 0A SHIPPED):** Renumbered from GAP-691 → GAP-694 (collision với main's GAP-691 = Wave 102.7.3 post-wave audit suite filed same day). Phase 0A investigation agent SHIPPED audit artifact `documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md`. **Top 3 root causes identified:** (1) P0 BLOCKING — Docker Desktop process KHÔNG chạy trên Windows host (WSL `docker-desktop` distro Stopped; fix `powershell.exe Start-Process 'Docker Desktop.exe'` ~5 phút), (2) P1 — `.env` thiếu 9 keys so với `.env.example` (hCaptcha × 3 + Ollama × 3 + AI_PROVIDER + CAPTCHA_ENABLED + tenant pattern; fix dev-safe defaults ~10 phút), (3) P2 META force-multiplier — preflight `kitehub/scripts/check-docker.sh` wrap `up.sh`/`setup.sh` với daemon-reachable check + auto-launch (~30-45 phút). **Phantoms ruled out:** ports clean (zero conflicts), WSL2 resources dư thừa (27 GiB RAM + 942 GiB free + 8 cores), Docker Desktop config đã đúng từ session 2026-05-07 (IntegratedWslDistros: kite-dev), compose service complete (25 services match expectation), scripts maturity high (16 scripts hardened by GAP-407/417/425/426). Status OPEN → PARTIAL 15%. Phase 0B (apply 3 fixes) + Phase 0C (codify rule) pending follow-up.
- **2026-05-21** — Gap filed from outside-in audit synthesis (3 parallel agents: failure-mode + external benchmark + persona simulation). User-chosen sequencing "Phase 0 local self-test fix → Item 2 refactor → rebuild" makes this gap the foundation. Investigation deferred to follow-up agent spawn (Phase 0A) — root cause unknown until empirical run.
