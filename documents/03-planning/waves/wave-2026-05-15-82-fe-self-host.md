---
title: Wave 82 — FE self-host kc-app + Wave 81 follow-ups
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 82
waves: [82]
estimated_wall_clock: 12-18h
risk_profile: MEDIUM (FE migration touches user-facing, DNS cutover)
trigger: Wave 81 backend deploy CLOSED; FE Vercel stale ~38h + Free Tier daily limit hit
---

# Wave 82 — FE Self-Host + Wave 81 Follow-ups

## 1. Brainstorm (5-10 min)

**Q1 (goal + alignment):** Migrate FE serving từ Vercel (Free Tier rate-limited) sang self-hosted on AWS EC2 (architecture B kh-backend hoặc new kc-app instance) để bypass build/deploy quota. Đồng thời resolve các Wave 81 follow-ups còn lại. Wave 81 backend deploy CLOSED 2026-05-15. FE Vercel stale ~38h (build cap hit ~2026-05-13). Backend đã có Wave 78+79+80+81 contract changes (Beta Status, Onboarding wizard, Staff Invitation, 2FA) nhưng FE chưa rebuild → dev test full 126 rows hit FE-BE drift, không phải code bugs.

**Q2 (trade-offs / decision context):**

| Constraint | State |
|---|---|
| Vercel Free Tier | 100 builds/day — hit cap ~38h ago |
| Vercel Pro upgrade | $20/mo per user — defer cost decision |
| Self-host on existing EC2 (kh-backend t3.medium 4GB) | RAM tight (currently ~3.2GB used by 7 BE services) |
| New EC2 (kc-app t3.medium) | $30/mo (Phase 1 BETA tier) — within budget |
| Cloudflare Pages migration | Free tier; requires DNS change + Next.js compat verify |

**Bucket A decision (2026-05-15):** Outside-in audit ran 2 agent parallel (external benchmark + failure-mode matrix); both converged on Vercel Pro recommendation. User locked **AWS EC2 self-host** (cost-priority, lock vendor on AWS). 4 P0 mitigation gaps surface từ failure matrix cần preempt trong Bucket B: F6 (SG description audit) / F7 (PM2 + swapfile + memory alarm, t3.small NEW instance — không co-host kh-backend) / F10 (certbot DNS-01 cert renewal) / F11 (BE CORS sweep pre-flip).

**Q3 (risks):**

- DNS cutover breaks public site mid-deploy → Mitigation: blue-green deploy + DNS gradual TTL drop (60s) 24h pre-cutover
- Self-hosted runner offline on WSL reboot → Mitigation: systemd service auto-restart + monitoring
- FE-BE contract drift post-rebuild còn surface → Mitigation: Bucket G full 126-row dev walk-through phát hiện remaining
- New EC2 cost exceeds Phase 1 BETA budget → Mitigation: t3.small (~$15/mo) primary; t3.medium fallback only nếu RAM proves tight
- t3.small 2GB RAM tight cho 2 Next standalone + nginx + PM2 → Mitigation: swapfile + OOM alarm (F7 P0)
- Cert renewal failure silent → 100% outage → Mitigation: certbot DNS-01 + monitoring (F10 P0)
- Cross-host CORS reject post-DNS flip → Mitigation: BE allowed-origins sweep + verify pre-flip (F11 P0)

## 2. Task Breakdown

| Bucket | Item | Owner | Effort | Sequential? |
|---|---|---|---|---|
| **A** | FE rebuild architecture decision (✅ DONE — AWS self-host locked) | coordinator + user | ~30min | First |
| **B** | FE deploy infrastructure setup (new EC2 t3.small + nginx + PM2 + certbot DNS-01 + SG with description + BE CORS sweep) | user actions + coordinator | 3-4h | After A |
| **C** | FE build + deploy với Wave 78+79+80+81 changes | CI + verify | 2-3h | After B |
| **D** | DNS cutover `kitehub.me` → new FE host (gradual TTL drop) | user (CF DNS edit) + coordinator verify | 1h | After C |
| **E** | Self-hosted GitHub runner setup trên WSL (Task #64) | user actions trên WSL | 1h | Parallel B-D |
| **F** | Wave 81 follow-up bug fixes (✅ DONE — see §6) | coordinator | 2h | Parallel |
| **G** | User manual P2/P3/Admin pages (defer từ Wave 79 F1) | optional defer Wave 83 | 2-4h | Parallel D-E |
| **H** | Post-FE-rebuild full 126-row dev walk-through | user (USER ACTION) | 2-3h dev side | After D |

**Sequential check:** A→B→C→D→H. E parallel after A. F (already done) parallel. G optional defer.

## 3. Scope — Bucket detail

### Bucket A — Architecture decision (✅ DONE 2026-05-15)

- 2 outside-in agents (external benchmark + failure-mode matrix) ran parallel
- Both recommended Vercel Pro $20/mo; user locked **AWS EC2 self-host** for vendor consistency
- ADR draft `ADR-XXX-fe-self-host.md` defer to Bucket B start
- 4 P0 mitigations identified (F6/F7/F10/F11) tracked as Bucket B pre-flight requirements

### Bucket B — FE deploy infrastructure (new EC2 t3.small)

- Files: `infrastructure/terraform-aws/ec2-kc-app.tf` (NEW), nginx config, PM2 ecosystem file, certbot DNS-01 setup
- Pre-flight P0 mitigations: SG description audit, PM2 + swapfile + memory alarm, certbot DNS-01 (avoid HTTP-01 race), BE allowed-origins sweep
- ADR-XXX-fe-self-host.md drafted same bucket

### Bucket C — FE build + deploy

- Build kitehub-frontend + kiteclass-frontend với Wave 78-81 contracts
- Deploy to new EC2 via CI workflow OR manual SSM
- Smoke test pre-DNS-flip via direct IP

### Bucket D — DNS cutover

- TTL drop 60s on `kitehub.me` 24h pre-flip
- Flip CF DNS record → new EC2 IP (proxied)
- Verify `dig kitehub.me` returns new host within 5min

### Bucket E — Self-hosted GitHub runner (see §7 for setup steps)

- Install on WSL + register + systemd service
- Sweep `runs-on: ubuntu-latest` → `runs-on: self-hosted` cho 19 workflows

### Bucket F — Wave 81 follow-up bug fixes (see §6 — ✅ ALL DONE)

### Bucket G — User manual pages (optional Wave 83 defer)

### Bucket H — Post-rebuild dev walk-through

- User runs full 126-row CSV walk-through against deployed FE+BE
- Output: bug list → defines Wave 83 scope

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|---|---|---|---|
| `kitehub-frontend/` source | FE codebase | `ls kitehub/kitehub-frontend/` | ✅ exists |
| `kiteclass-frontend/` source | FE codebase | `ls kiteclass/kiteclass-frontend/` | ✅ exists |
| Vercel project config | Vercel deploy state | `cat .vercel/project.json` | ✅ exists (stale 38h) |
| `actions/runner` self-hosted setup | Runner host | `ls ~/actions-runner` | 🆕 to-be-created (Bucket E) |
| `infrastructure/terraform-aws/ec2-kc-app.tf` | TF kc-app EC2 | `ls infrastructure/terraform-aws/ec2-kc-app*` | 🆕 to-be-created (Bucket B) |
| `documents/05-guides/deploy/fe-self-host-runbook.md` | Runbook | `ls documents/05-guides/deploy/fe-self-host-runbook.md` | 🆕 to-be-created (Bucket B) |
| `documents/02-architecture/adr/ADR-XXX-fe-self-host.md` | ADR | `find documents/02-architecture/adr -iname '*fe-self-host*'` | 🆕 to-be-created (Bucket B) |

## 5. Verification Gates (Wave 82 close acceptance)

| Criterion | Met when |
|---|---|
| FE rebuilt với Wave 78+79+80+81 contracts | `kitehub.me` returns Wave 81-version HTML + new components |
| FE-BE contract integration | Browse onboarding wizard works; staff invite renders; 2FA challenge UI shows |
| DNS cutover successful (no downtime >5 min) | `dig kitehub.me` returns new host; no Cloudflare cache miss for >1h |
| Self-hosted runner active | Future PR CI runs on `self-hosted` không hit Free Tier minutes |
| Wave 81 follow-ups closed | 5 Bucket F items DONE (see §6); 2 audit-surfaced findings filed Wave 83 |
| Full 126-row dev walk-through completes | User reports walk-through outcome — defines Wave 83 scope |
| Bucket A 4 P0 mitigations addressed | SG description audit + PM2/swapfile/memory alarm + certbot DNS-01 + BE CORS sweep all verified |

## 6. Agent Spawn Pattern

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH (production cutover; user-facing DNS flip) → model: Opus 4.7 full coordinator.

**Cross-layer (per `wave-pack-planner/SKILL.md` §Step 4.5):** YES (FE rebuild + BE contract sync). Bucket A foundation = ADR + decision lock (DONE 2026-05-15). Bucket B-D sequential serial (deploy ops cannot parallelize per `concurrent-production-mutation-ops.md`).

**Bucket A:** 2 parallel background agents (external benchmark via WebSearch + failure-mode matrix via simulation-gap-finder) — convergence pattern. ✅ DONE 2026-05-15.

**Bucket B-D:** User-executed (terraform apply / DNS edit / deploy workflow_dispatch). Coordinator verify-only via Tier 1 read-only AWS commands.

**Bucket E:** User-action trên WSL (runner install). Coordinator runs sed workflow migration after runner online.

**Bucket F (✅ DONE):** Coordinator single-thread (5 bug fixes serial within scope). Outside-in mode for Bucket A2 (failure-mode matrix surface 7 additional same-class gateway routing bugs).

**Bucket G:** Defer Wave 83 (optional scope).

**Bucket H:** User self-test (full 126-row walk-through). Coordinator runs ~60-row API contract sweep post-deploy (per Task #10).

## 7. Closure Protocol

Per `post-wave-cleanup.md` mandate:
- [ ] All Bucket A-H verified PASS or explicit defer documented
- [ ] Bucket A ADR draft `ADR-XXX-fe-self-host.md` shipped
- [ ] DNS cutover verified `dig kitehub.me` post-flip
- [ ] FE-BE integration smoke verified via dev walk-through Bucket H
- [ ] 4 P0 mitigations (F6/F7/F10/F11 from failure-matrix) verified addressed
- [ ] Wave 83 follow-up gaps filed (3 missing application-production.yml, audit-service-ports.sh bug, FEEDBACK CSV rows)
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` clean
- [ ] ROADMAP §🎯 Current Status Snapshot updated → Wave 82 SHIPPED
- [ ] `wave-history.jsonl` Wave 82 entry appended
- [ ] Wave 83 plan draft started OR explicit "no Wave 83 needed" closure note

## 8. Log

- **2026-05-15** (draft): Wave 82 plan created post Wave 81 closure. Bucket A spawned 2 outside-in agents (external benchmark + failure-mode matrix); both converged on Vercel Pro recommendation. User locked AWS EC2 self-host (cost-priority, AWS vendor lock). Bucket F shipped same session via PR #1396 — 5 follow-up items DONE + 7 audit-surfaced gateway routing bugs fixed (15-bug class total: beta-status + staff-invitations × 4 + admin-impersonate × 3). 2 audit-surfaced findings defer Wave 83 (3 services missing application-production.yml, audit-service-ports.sh script bug).

## 9. Wave 81 follow-up gaps consolidated (Bucket F — ✅ ALL DONE)

1. **`/api/v1/beta-status` 400 empty body** — ✅ DONE Wave 82 Bucket F4: root cause = gateway routing bug. `/api/v1/beta-status` fell through `/api/v1/**` catch-all → kiteclass-core (wrong service) → TenantResolver rejected as 400 (no tenant header on public path). Fix: added explicit route `kitehub-beta-status` → kitehub-subscription:8080 trong `kitehub-gateway/src/main/resources/application.yml`. `audit-gateway-routes.sh` surface 7 same-class drift: 4 staff-invitations + 3 admin-impersonate → all 8 fixed same PR (audit clean: 45 routes, 91 endpoints, zero wrong-service).

2. **CSV row IDs mismatch Wave plan §G** — ✅ DONE Wave 82 Bucket F3: Wave 81 plan §G updated to use existing CSV IDs (OWNER-PROVISION-001 / OWNER-TEACHER-001 / TEACH-LOGIN-001 / ADM-BETA-APPROVE-001 / PARENT-LOGIN-001 substitutes). FEEDBACK widget rows defer Wave 83 (no CSV equivalent yet).

3. **CSV references `/api/v1/auth/login` nhưng deployed path = `/api/auth/login`** — ✅ DONE Wave 82 Bucket F2: state-check verified zero `/api/v1/auth/login` refs trong CSV (only `/api/v1/auth/request-beta-access` x2 matching deployed `BetaAccessController.java:79`). Symptom not present per `audit-to-gap-pipeline.md` §2.8 decision matrix.

4. **`scripts/rotate-leaked-credentials.sh` wrapper name bug** — Task #59 ✅ DONE Wave 82 Bucket F1: renamed `scripts/rotate-credentials.sh` (general-purpose); backward-compat symlink `rotate-leaked-credentials.sh` kept; header tone updated.

5. **Spring Boot returns 500 thay vì 404 cho POST static-not-found** — ✅ DONE Wave 82 Bucket F5: added `spring.web.resources.add-mappings: false` vào 4 WebMVC services (kitehub-admin / kitehub-branding / kitehub-subscription / kiteclass-core). Skipped gateway services (WebFlux) + email (WebFlux) + platform (shared lib).

### New audit findings surfaced Bucket F4 (defer Wave 83 follow-ups)

- **3 services missing `application-production.yml`** (kitehub-admin, kitehub-branding, kitehub-email) — `audit-spring-profiles.sh` flagged: `SPRING_PROFILES_ACTIVE=production` env set but no matching file → Spring silently ignores profile, production overrides never apply. Per `production-env-config-registry.md` §11.3.
- **`scripts/audit-service-ports.sh` script bug** — line 129 unbound variable error trips script before completing. Per `audit-to-gap-pipeline.md` Step 3, file as P2 follow-up gap.

## 7. Self-hosted GitHub runner setup (Bucket E reference)

Per CLAUDE.md migration scope clarification user yêu cầu: chỉ cần (a) install runner + register, (b) sed thay `runs-on: ubuntu-latest` → `runs-on: self-hosted` trong 19 workflow files. Secrets/variables giữ nguyên trong GitHub Secrets.

### Setup steps (USER ACTION trên WSL)

```bash
# 1. Download GitHub runner
mkdir -p ~/actions-runner && cd ~/actions-runner
curl -o actions-runner-linux-x64-2.319.1.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.319.1/actions-runner-linux-x64-2.319.1.tar.gz
tar xzf ./actions-runner-linux-x64-2.319.1.tar.gz

# 2. Get registration token (one-time per runner, từ repo Settings → Actions → Runners → New self-hosted runner)
# Hoặc qua API:
gh api -X POST "repos/VictorAurelius/2026-Kite-Class-Platform/actions/runners/registration-token" --jq .token

# 3. Configure runner
./config.sh --url https://github.com/VictorAurelius/2026-Kite-Class-Platform \
  --token <TOKEN_FROM_STEP_2> \
  --name kite-dev-wsl-runner \
  --labels self-hosted,Linux,X64,wsl \
  --work _work

# 4. Install as systemd service (persistent)
sudo ./svc.sh install
sudo ./svc.sh start

# 5. Verify
sudo ./svc.sh status
gh api "repos/VictorAurelius/2026-Kite-Class-Platform/actions/runners" --jq '.runners[] | {name,status,labels:[.labels[].name]}'
```

### Workflow migration

```bash
# Coordinator runs this after runner online:
find .github/workflows -name "*.yml" -exec sed -i 's/runs-on: ubuntu-latest/runs-on: [self-hosted, Linux, X64]/' {} \;
git diff .github/workflows/
```

**Caveat:** Self-hosted runner runs untrusted PR code (PR từ forks). Solo-dev mode acceptable; team mode cần GitHub branch protection + workflow approval.

## 8. Risk + mitigation (consolidated)

| Risk | Mitigation |
|---|---|
| DNS cutover breaks public site mid-deploy | EC2 path = blue-green deploy + DNS gradual TTL drop (60s) 24h pre-flip |
| Self-hosted runner offline on WSL reboot | systemd service auto-restart + monitoring dashboard alert |
| FE-BE contract drift post-rebuild còn surface | Bucket H full 126-row test phát hiện remaining; tail follow-ups vào Wave 83 |
| t3.small 2GB RAM OOM under ISR regen | swapfile + memory alarm; fallback t3.medium nếu sustained pressure |
| SSL cert renewal silent failure (90d expire) | certbot DNS-01 (avoid HTTP-01 race) + cert expiry monitor 30d ahead |
| EC2 SG misconfig → port 4701 internet-exposed | SG with description audit per `aws-sg-description-ascii.md` enforcement |
| Cross-host CORS reject post-DNS flip | BE allowed-origins config pre-allowlist NEW EC2 IP/domain + verify via curl pre-flip |

## 9. Cross-link

- Wave 81 closure: `documents/03-planning/waves/wave-2026-05-14-81-deploy-smoke.md`
- Wave 81 Bucket G audit: `documents/04-quality/audits/pre-self-test/2026-05-15-wave-81-spot-check.md`
- Bucket F PR: #1396 (gateway routing + spring 404 + script rename)
- FE migration ADR (to-be-created Bucket B): `documents/02-architecture/adr/ADR-XXX-fe-self-host.md`
- Task #59 (rotate-leaked-credentials.sh wrapper bug — ✅ DONE Bucket F1)
- Task #61 (Vercel redeploy BLOCKED → AWS self-host instead)
- Task #63 (Wave 82 plan draft — THIS DOC)
- Task #64 (Self-hosted GitHub runner setup — Bucket E)
- `.claude/rules/outside-in-coverage-trigger.md` — applied 2 agent audit Bucket A
- `.claude/rules/production-env-config-registry.md` §11.3 — 3 services missing application-production.yml (Wave 83 defer)
