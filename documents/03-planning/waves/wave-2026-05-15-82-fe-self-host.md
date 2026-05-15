---
title: Wave 82 — FE self-host kc-app + Wave 81 follow-ups
status: draft
created: 2026-05-15
phase: phase-1-beta
wave: 82
estimated_wall_clock: 12-18h
risk_profile: MEDIUM (FE migration touches user-facing, DNS cutover)
trigger: Wave 81 backend deploy CLOSED; FE Vercel stale ~38h + Free Tier daily limit hit
---

# Wave 82 — FE Self-Host + Wave 81 Follow-ups

## 1. Goal

Migrate FE serving từ Vercel (Free Tier rate-limited) sang self-hosted on AWS EC2 (architecture B kh-backend or new kc-app instance) để bypass build/deploy quota. Đồng thời resolve các Wave 81 follow-ups còn lại.

**Trigger:** Wave 81 backend deploy CLOSED 2026-05-15. FE Vercel stale ~38h (build cap hit ~2026-05-13). Backend đã có Wave 78+79+80+81 contract changes (Beta Status, Onboarding wizard, Staff Invitation, 2FA) nhưng FE chưa rebuild → dev test full 126 rows hit FE-BE drift, không phải code bugs.

## 2. Decision context

| Constraint | State |
|---|---|
| Vercel Free Tier | 100 builds/day — hit cap ~38h ago |
| Vercel Pro upgrade | $20/mo per user — defer cost decision |
| Self-host on existing EC2 (kh-backend t3.medium 4GB) | RAM tight (currently ~3.2GB used by 7 BE services) |
| New EC2 (kc-app t3.medium) | $30/mo (Phase 1 BETA tier) — within budget |
| Cloudflare Pages migration | Free tier; requires DNS change + Next.js compat verify |

**Phase 1 BETA recommendation:** Cloudflare Pages free tier OR new t3.small EC2 (~$15/mo) — defer commit to §3 brainstorm.

## 3. Scope — Bucket list

| # | Bucket | Item | Owner | Risk | Wall-clock |
|---|---|---|---|---|---|
| 1 | **A** | FE rebuild architecture decision (CF Pages vs EC2 self-host vs Vercel Pro) | coordinator + user | Low | 1h |
| 2 | **B** | FE deploy infrastructure setup (CF Pages config OR EC2 + nginx OR Vercel Pro) | user actions + coordinator | Med | 3-4h |
| 3 | **C** | FE build + deploy với Wave 78+79+80+81 changes | CI + verify | Med | 2-3h |
| 4 | **D** | DNS cutover `kitehub.me` → new FE host | user (CF DNS edit) + coordinator verify | High (DNS prop ~5min TTL) | 1h |
| 5 | **E** | Self-hosted GitHub runner setup trên WSL (Task #64) — bypass CI Free Tier minutes throttle | user actions trên WSL | Low | 1h |
| 6 | **F** | Wave 81 follow-up bug fixes: `/api/v1/beta-status` 400, CSV doc sync (row IDs + login path), rotate-leaked-credentials.sh wrapper name | coordinator | Low | 2h |
| 7 | **G** | User manual P2/P3/Admin pages (defer từ Wave 79 F1) | optional defer Wave 83 | Med | 2-4h |
| 8 | **H** | Post-FE-rebuild full 126-row dev walk-through | user (USER ACTION) | Med | 2-3h (dev side) |

## 4. Outside-in audit (per `outside-in-coverage-trigger.md` — applied to Wave 82 scope)

Inside-out brainstorm trong §3 đề xuất bucket list từ goc nhin dev. **Outside-in audit cần spawn trước khi lock Wave 82 plan:**

| Method | Skill | Phù hợp |
|---|---|---|
| Persona simulation | `.claude/skills/quality/persona-based-business-review/SKILL.md` | Anonymous + P2 owner + P3 manager khi tenant đầu tiên truy cập post-Wave-82 |
| External benchmark | WebSearch | CF Pages vs Vercel vs self-host EC2 — VN SaaS market |
| Failure-mode matrix | `.claude/skills/quality/simulation-gap-finder/SKILL.md` | FE-BE drift modes; DNS cutover failure modes |

→ AskUserQuestion before locking scope: spawn outside-in audit không (per rule)?

## 5. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|---|---|---|---|
| `kitehub-frontend/` source | FE codebase | `ls kitehub/kitehub-frontend/` | ✅ exists |
| `kiteclass-frontend/` source | FE codebase | `ls kiteclass/kiteclass-frontend/` | ✅ exists |
| Vercel project config | Vercel deploy state | `cat .vercel/project.json` | ✅ exists (stale 38h) |
| `actions/runner` self-hosted setup | Runner host | `ls ~/actions-runner` | 🆕 to-be-created (Bucket E) |
| `infrastructure/terraform-aws/ec2-kc-app.tf` | TF kc-app EC2 | `ls infrastructure/terraform-aws/ec2-kc-app*` | 🆕 to-be-created (Bucket B if EC2 path) |
| `documents/05-guides/deploy/fe-self-host-runbook.md` | Runbook | `ls documents/05-guides/deploy/fe-self-host-runbook.md` | 🆕 to-be-created (Bucket B) |

## 6. Wave 81 follow-up gaps consolidated (Bucket F)

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

## 8. Acceptance gate (Wave 82 close)

| Criterion | Met when |
|---|---|
| FE rebuilt với Wave 78+79+80+81 contracts | `kitehub.me` returns Wave 81-version HTML + new components |
| FE-BE contract integration | Browse onboarding wizard works; staff invite renders; 2FA challenge UI shows |
| DNS cutover successful (no downtime >5 min) | `dig kitehub.me` returns new host; no Cloudflare cache miss for >1h |
| Self-hosted runner active | Future PR CI runs on `self-hosted` không hit Free Tier minutes |
| 4 Wave 81 follow-ups fixed OR deferred với tracking gap | Each item DONE / GAP filed |
| Full 126-row dev walk-through completes | User reports walk-through outcome — defines next Wave 83 |

## 9. Risk + mitigation

| Risk | Mitigation |
|---|---|
| DNS cutover breaks public site mid-deploy | CF Pages có rollback button; EC2 path = blue-green deploy + DNS gradual TTL drop |
| Self-hosted runner offline on WSL reboot | systemd service auto-restart + monitoring dashboard alert |
| FE-BE contract drift post-rebuild còn surface | Bucket G full 126-row test phát hiện remaining; tail follow-ups vào Wave 83 |
| New EC2 cost exceeds Phase 1 BETA budget | CF Pages free tier as primary; EC2 fallback only nếu CF compat issues |

## 10. Cross-link

- Wave 81 closure: `documents/03-planning/waves/wave-2026-05-14-81-deploy-smoke.md`
- Wave 81 Bucket G audit: `documents/04-quality/audits/pre-self-test/2026-05-15-wave-81-spot-check.md`
- FE migration ADR (to-be-created Bucket A): `documents/02-architecture/adr/ADR-XXX-fe-self-host.md`
- Task #59 (rotate-leaked-credentials.sh wrapper bug)
- Task #61 (Vercel redeploy BLOCKED)
- Task #63 (Wave 82 plan draft — THIS DOC)
- Task #64 (Self-hosted GitHub runner setup)
- `.claude/rules/outside-in-coverage-trigger.md` — apply §4 before locking scope
