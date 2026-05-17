---
title: Wave 89 — Gateway JWT + PM2 Ops Cluster
status: draft
created: 2026-05-17
updated: 2026-05-17
waves: [89]
gaps: [GAP-604, GAP-576, GAP-602, GAP-603]
---

# Wave 89 — Gateway JWT + PM2 Ops Cluster

**Goal:** Unblock beta cohort onboarding bằng fix `kitehub-gateway` thiếu JWT-to-headers filter (GAP-604 P0) + close-out residual gateway route 404 gap (GAP-576) + harden PM2 production deploy (GAP-602/603 P1).

**Trigger:** Wave 88 closure (PR #1477) Claude Playwright walkthrough phát hiện Platform_Admin 24 flow rows: 10 PASS + 9 BLOCKED bởi GAP-604 (admin endpoints 401 dù JWT valid). Beta cohort invite không thể proceed cho tới khi gateway JWT propagation fix.

**Estimated wall-clock:** ~3-4h agent work × 2 buckets parallel ≈ longest bucket ~120 min.

---

## 1. Brainstorm

### Q1: Inside-out + outside-in completeness

**Inside-out from ROADMAP §🚀 Next Action (Wave 89 canonical):**
- GAP-604 P0 Gateway JWT filter — **BLOCKER beta cohort**
- GAP-602 P1 PM2 `ecosystem.config.js` cwd path
- GAP-603 P1 PM2 systemd auto-start
- GAP-601 P2 ops audit deferred (defer Wave 90 — separate audit cadence per `post-wave-audit-mandate.md` §2.4)

**Inside-out from `documents/03-planning/inside-out-queue.md` (5 items):**
- Premium plan → defer Phase 1.5 (Wave 79+); n/a Wave 89
- Feedback channel — consumed Wave 78
- Email content audit — consumed Wave 78
- User manual VN — consumed Wave 79
- Manual split professional vs end-user — Wave 90+ candidate (doc work, không match Wave 89 backend scope)

**Inside-out audit overlap (CSV query phase-1-beta non-DONE):**
- **GAP-576 OPEN P0** "Gateway auth routes 404" (Wave 85 filed) — same `kitehub-gateway` scope → merge vào Bucket A
- GAP-574 = phantom reference (mentioned in GAP-566 row text, no actual file/CSV row) → drop, scope already covered by GAP-602/603
- GAP-257 P0 restore drill, GAP-144 P1 AlertManager — không liên quan gateway/PM2, defer
- Pre-tenant cluster GAP-525/514/524/515/521 — defer Wave 90+ post GAP-604 unblock

**Outside-in NEW (per `outside-in-coverage-trigger.md` §4 exception):**
- SKIPPED — scope = backend infra fixes (gateway filter + PM2 config), không user-facing flow mới. Existing Wave 88 walkthrough audit (`documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md` §7) đã surface user-impact context.

### Q2: Trade-offs

- **Fix GAP-604 only (1 PR fast unblock)** — rejected: GAP-576/602/603 cùng infra scope, parallel ship hiệu quả hơn serial
- **Add GAP-601 ops audit Wave 89** — rejected: audit cadence separate (per `post-wave-audit-mandate.md` §2.4); audit defer 2026-05-20 standalone
- **Merge Bucket A + Bucket B (single PR)** — rejected: gateway Java change vs PM2 JS/systemd config disjoint, parallel agents hiệu quả hơn
- **State-check GAP-576 before vs after fix GAP-604** — chosen state-check trước: Wave 88 walkthrough audit ghi "Admin login E2E works (JWT issued)" suggesting `/auth/login` đã hết 404 (route map possibly fixed Wave 86 staging.19+staging.20); cần verify-and-close before duplicate work

### Q3: Risks + recovery

| Risk | Bucket | Recovery |
|---|---|---|
| JWT filter order conflict với existing `RateLimitMetricsFilter` / `SecurityHeadersFilter` | A | Set `@Order(-100)` per GAP-604 proposed fix; integration test verify filter chain order |
| JWT secret mismatch giữa gateway + downstream services → silent 401 | A | Use `JWT_SECRET` env from Secrets Manager (consistent với `kitehub-platform` config); unit test verify same secret |
| PM2 systemd unit conflict với existing nginx unit | B | Use namespaced service name `kitehub-frontend.service` + `kiteclass-frontend.service`; document trong `documents/05-guides/deploy/` |
| Bucket A breaks public auth endpoints (login/signup) | A | Filter `isPublicPath()` excludes `/api/auth/*` + `/api/v1/auth/*` + `/actuator/health` per GAP-604 spec |
| Cluster GAP-576 cần additional route config beyond GAP-604 | A | State-check first; nếu `/verify-email` + `/password-reset` còn 404 → add gateway routes same PR |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-604 + GAP-576 | bg-agent | ~120min | ✅ `kitehub-gateway/**` only |
| B | GAP-602 + GAP-603 | bg-agent | ~90min | ✅ `infrastructure/terraform-aws/ec2.tf` + `ecosystem.config.js` + systemd unit + `scripts/deploy-prod.sh` |

Disjoint: A touch Java + YAML trong `kitehub-gateway`; B touch terraform + JS config + bash + systemd unit. Zero path overlap.

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM-HIGH — Bucket A touches production auth path (high blast radius nếu fail). Model: Opus 4.7 cho Bucket A, Sonnet đủ cho Bucket B.
**Cross-layer? (per `contract-first-for-cross-layer.md` §2):** NO — gateway-only (Bucket A) + ops-only (Bucket B), không FE consumer trong wave này. Skip Bucket 0 Foundation.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** Gateway auth cluster | GAP-604, GAP-576 | 🔴 P0 | `kitehub/kitehub-gateway/src/main/java/.../filter/JwtAuthenticationGatewayFilter.java` (NEW) + `kitehub/kitehub-gateway/src/main/resources/application*.yml` (route map) + `kitehub/kitehub-gateway/src/test/java/.../filter/JwtAuthenticationGatewayFilterTest.java` (NEW) | parallel batch 1 |
| 2 | **B** PM2 ops cluster | GAP-602, GAP-603 | 🟠 P1 | `ecosystem.config.js` (root or `infrastructure/`) + `infrastructure/systemd/kitehub-frontend.service` (NEW) + `infrastructure/systemd/kiteclass-frontend.service` (NEW) + `infrastructure/terraform-aws/ec2.tf` (user_data) + `scripts/deploy-prod.sh` (update PM2 invocation) | parallel batch 1 |

### Bucket A — Gateway auth cluster (GAP-604 + GAP-576)

**Files:**
- NEW: `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java`
- EDIT: `kitehub/kitehub-gateway/src/main/resources/application.yml` + `application-prod.yml` route map (add `/api/v1/auth/login`, `/verify-email`, `/password-reset` routes nếu state-check confirm còn 404)
- NEW: `kitehub/kitehub-gateway/src/test/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilterTest.java`

**Implementation per GAP-604 §"Proposed Fix":**
- `@Component` `JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered`
- Read `Authorization: Bearer <JWT>` → parse claims (HMAC-SHA, secret từ env `JWT_SECRET`)
- Mutate request: add `X-User-Id` (sub), `X-User-Roles` (role claim, single OR comma-separated), `X-User-Email`
- Public path bypass: `/api/auth/*`, `/api/v1/auth/*`, `/actuator/health`, `/docs/*`
- Invalid JWT → 401 short-circuit
- `@Order(-100)` — before circuit breaker, after CORS

**Pre-implementation state-check (mandatory, in agent prompt):**
```bash
# Verify GAP-576 residual scope
curl -X POST -H "Content-Type: application/json" -d '{}' https://api.kitehub.me/api/v1/auth/login
# → if 400 = route exists (GAP-576 resolved Wave 86+), skip route map add
# → if 404 = route missing, add gateway route config same PR

curl -X POST -H "Content-Type: application/json" -d '{}' https://api.kitehub.me/api/v1/auth/verify-email
curl -X POST -H "Content-Type: application/json" -d '{}' https://api.kitehub.me/api/v1/auth/password-reset

# Verify existing filter chain
grep -rn "GlobalFilter\|@Order" kitehub/kitehub-gateway/src/main/java/
```

⚠️ Stack STOPPED — agent phải request user `bash scripts/aws/start-stack.sh` trước khi run state-check curls. Alternative: state-check qua reading `application*.yml` source code directly nếu stack không thể start.

**Tests:**
- Unit: JWT valid → 3 headers set; JWT expired → 401; JWT missing → pass-through (downstream rejects); public path → bypass without header set; malformed JWT → 401
- Integration (Spring Boot Test với WebTestClient): mock downstream, verify request mutation propagation

**Acceptance:**
- [ ] `JwtAuthenticationGatewayFilter` implemented + 5 unit test cases pass
- [ ] State-check GAP-576: verify 3 auth endpoints status; nếu 404 → add gateway route config same PR; document verdict trong PR body
- [ ] `cd kitehub && ./mvnw -pl kitehub-gateway verify -P strict-warnings` pass
- [ ] PR body includes self-test per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (CURL evidence post-deploy)
- [ ] GAP-604 + GAP-576 flipped DONE per `gap-done-discipline.md` §2 (live verify on stack-started production)
- [ ] Deploy verify deferred to user (post-merge `gh workflow run deploy-production.yml`) — PR body cite GAP-491 follow-up cho deploy observability nếu cần

### Bucket B — PM2 ops cluster (GAP-602 + GAP-603)

**Files:**
- EDIT: `ecosystem.config.js` (location TBD via state-check — likely `infrastructure/` or repo root) — fix `cwd` path để work in monorepo từ any directory
- NEW: `infrastructure/systemd/kitehub-frontend.service` (systemd unit file)
- NEW: `infrastructure/systemd/kiteclass-frontend.service` (systemd unit file)
- EDIT: `infrastructure/terraform-aws/ec2.tf` user_data — append `systemctl enable kitehub-frontend kiteclass-frontend` + `systemctl start ...` post bootstrap; OR add `pm2 startup systemd` + `pm2 save` cycle
- EDIT: `scripts/deploy-prod.sh` — invoke PM2 với explicit `--cwd` flag (resilience cho path mismatch)
- NEW: `documents/05-guides/deploy/pm2-systemd-auto-start.md` runbook (per `docs-folder-structure.md` §3)

**Pre-implementation state-check (mandatory):**
```bash
# Locate ecosystem.config.js
find . -name "ecosystem.config.js" -not -path '*/node_modules/*' -not -path '*/.git/*'

# Read GAP-602 + GAP-603 full proposed fix
cat documents/04-quality/gaps/GAP-602-pm2-ecosystem-cwd-path-mismatch.md
cat documents/04-quality/gaps/GAP-603-pm2-systemd-auto-start.md

# Verify current systemd units on EC2 (via Tier 1 read-only describe)
# SSM SendCommand 'systemctl list-unit-files | grep -E "pm2|kitehub|kiteclass"' — DEFER live check until stack start
```

**Tests:**
- shellcheck pass cho `deploy-prod.sh`
- `node -c ecosystem.config.js` syntax pass
- systemd unit validate: `systemd-analyze verify infrastructure/systemd/kitehub-frontend.service`

**Acceptance:**
- [ ] `ecosystem.config.js` `cwd` field resolved correctly từ any working directory (relative-path fix per GAP-602)
- [ ] 2 systemd unit files validated + documented trong runbook
- [ ] `ec2.tf` user_data extended với `pm2 startup systemd` OR systemd `enable` directly (chose-one based on PM2 best practice)
- [ ] Runbook docs `documents/05-guides/deploy/pm2-systemd-auto-start.md` 4 sections per template
- [ ] Live verify deferred to user post-merge: `bash scripts/aws/start-stack.sh && sleep 60 && curl https://kitehub.me/ → 200` (PM2 auto-started on boot)
- [ ] GAP-602 + GAP-603 flipped DONE per `gap-done-discipline.md` §2 (OR PARTIAL nếu live verify deferred — file follow-up gap cho live verify per §3 PARTIAL exit ramp)

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kitehub-gateway` module | Maven module | `ls kitehub/kitehub-gateway/pom.xml` | per Wave 87 Bucket D fix CORS | ✅ exists |
| `JwtAuthenticationGatewayFilter` class | Java class | `grep -rn "JwtAuthenticationGatewayFilter" kitehub/kitehub-gateway/src` | not present | 🆕 to-be-created (Bucket A) |
| Existing `GlobalFilter` impls | Java class | `grep -rn "implements GlobalFilter" kitehub/kitehub-gateway/src/main/java` | RateLimitMetricsFilter, SecurityHeadersFilter, TenantResolverGatewayFilterFactory | ✅ verify-at-spawn (Bucket A check order conflict) |
| Gateway route map | YAML | `grep -A 30 "routes:" kitehub/kitehub-gateway/src/main/resources/application*.yml` | not yet checked | ⚠️ verify-at-spawn (Bucket A — confirm `/api/v1/auth/{login,verify-email,password-reset}` route status for GAP-576) |
| `XUserRolesHeaderFilter` (downstream) | Java class | `grep -rn "XUserRolesHeaderFilter\|X-User-Roles" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/SecurityConfig.java` | per GAP-604 §"Root Cause" line 144 | ✅ exists (verify line number at spawn) |
| `JWT_SECRET` env var | env | `grep -rn "JWT_SECRET" kitehub/kitehub-gateway/src/main/resources` + `infrastructure/terraform-aws/secrets.tf` | not yet checked | ⚠️ verify-at-spawn (Bucket A — same secret across gateway + downstream) |
| `ecosystem.config.js` | JS config | `find . -name "ecosystem.config.js" -not -path '*/node_modules/*'` | not yet checked | ⚠️ verify-at-spawn (Bucket B — locate canonical path) |
| `infrastructure/systemd/` | Folder | `ls -d infrastructure/systemd/ 2>/dev/null` | not yet checked | 🆕 likely to-be-created (Bucket B) |
| `infrastructure/terraform-aws/ec2.tf` | Terraform | `ls infrastructure/terraform-aws/ec2.tf` | per Wave 88 user_data update | ✅ exists |
| `scripts/deploy-prod.sh` | Bash | `ls scripts/deploy-prod.sh` | per Wave 88 SSM SendCommand FE deploy | ✅ exists |
| `documents/05-guides/deploy/` | Folder | `ls -d documents/05-guides/deploy/` | per Wave 88 EIP cutover doc | ✅ exists |
| GAP-604 file | Gap file | `ls documents/04-quality/gaps/GAP-604-*.md` | per Wave 88 closure PR #1477 | ✅ exists OPEN P0 |
| GAP-576 file | Gap file | `ls documents/04-quality/gaps/GAP-576-*.md` | per Wave 85 filing | ✅ exists OPEN P0 |
| GAP-602 file | Gap file | `ls documents/04-quality/gaps/GAP-602-*.md` | per Wave 88 closure | ✅ exists OPEN P1 |
| GAP-603 file | Gap file | `ls documents/04-quality/gaps/GAP-603-*.md` | per Wave 88 closure | ✅ exists OPEN P1 |
| GAP-574 (phantom check) | Gap file | `find documents/04-quality/gaps -name "GAP-574*"` | not exists; referenced only in GAP-566 row text | ❌ phantom — drop from scope |
| AWS stack state | EC2/RDS | `cat .claude/session-aws-cache/snapshot.txt` (per collect-state.sh) | 3 EC2 + 1 RDS STOPPED | ⚠️ requires user `bash scripts/aws/start-stack.sh` for live verify |

**Banned shortcuts:**
- `| head` truncation on grep/find
- Skipping pre-deploy curl verification cho GAP-576 status
- Aspirational references without 🆕 flag

**verify-at-spawn:** bucket agents PHẢI run grep + ls commands listed trên trước khi propose changes; nếu absent → file sub-gap thay vì cascade fix.

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-gateway verify -P strict-warnings` | gateway-ci |
| B | `shellcheck scripts/deploy-prod.sh && node -c ecosystem.config.js && systemd-analyze verify infrastructure/systemd/*.service` (latter requires systemd in agent env — defer to terraform-cloud-deploy review nếu unavailable) | script-quality |

**Post-merge live verify (user-triggered):**
- A: `bash scripts/aws/start-stack.sh && JWT=$(curl ... login) && curl -H "Authorization: Bearer $JWT" https://api.kitehub.me/api/v1/admin/beta-requests` → 200 (NOT 401)
- B: `aws ec2 reboot-instances --instance-ids i-05cfda7c6c60b683f && sleep 90 && curl https://kitehub.me/` → 200 (PM2 auto-started)

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

**Batch 1 (parallel, simultaneous):** A + B — 2 agents `run_in_background: true`, `isolation: worktree`

Coordinator (this session OR next session) handles:
- Verify CI green per bucket
- Sequential merge to main (cả 2 buckets squash-direct vì disjoint paths)
- Final closure PR includes: ROADMAP update + wave plan `status: complete` + wave-history.jsonl append + `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`

---

## 7. Closure Protocol

Per `post-wave-cleanup.md` + `gap-done-discipline.md` + `post-merge-sync-completeness.md`:

- [ ] Bucket A + B both merged
- [ ] GAP-604 + GAP-576 + GAP-602 + GAP-603 flipped DONE (OR PARTIAL với follow-up filed per `gap-done-discipline.md` §3)
- [ ] Wave plan `status: complete` + `updated:` bumped
- [ ] `documents/03-planning/wave-history.jsonl` append entry
- [ ] ROADMAP §🚀 Next Action updated (queue Wave 90 = GAP-601 ops audit + pre-tenant cluster GAP-525/514/524/515/521)
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` clean
- [ ] Inside-out-queue.md unchanged (Wave 89 không consume queued items; Manual split → Wave 90+)
- [ ] Handoff message: "Wave 89 ✅ ship. Beta cohort unblocked. Next: `bash scripts/aws/start-stack.sh && bash scripts/dev/self-test-preflight.sh` → run admin walkthrough → invite cohort 1."

---

## 8. Log

- **2026-05-17:** Wave 89 plan drafted. Scope locked via AskUserQuestion explicit (option 4: 2 cluster buckets merging overlap candidates). Inside-out audit 3-source (ROADMAP + inside-out-queue.md + CSV phase-1-beta non-DONE) confirmed: GAP-574 phantom (drop), GAP-576 overlap với GAP-604 same gateway scope (merge Bucket A), pre-tenant cluster defer Wave 90+, Manual split queue item defer Wave 90+. Outside-in audit SKIPPED per `outside-in-coverage-trigger.md` §4 exception (backend infra fixes, no user-facing flow new). Cross-layer check: NOT cross-layer (gateway + PM2 disjoint, no FE consumer). Stack STOPPED — live verify deferred to user post-merge.
