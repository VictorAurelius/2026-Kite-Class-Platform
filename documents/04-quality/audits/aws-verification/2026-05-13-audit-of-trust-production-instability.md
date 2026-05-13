---
title: AWS Verification — Audit-of-Trust Pass uncovers production thrashing (Plan 1 self-test BLOCKED)
status: complete
created: 2026-05-13
phase: phase-1-beta
wave: 69
gaps: [GAP-502, GAP-447, GAP-370, GAP-481]
---

# Audit-of-Trust Pass — Production instability uncovered

## Scope

User-flagged 2026-05-13 sau khi probe `kitehub.me/auth/request-beta-access` → 404. Path fix (PR #1255) chỉ là phần nổi. User chốt **audit-of-trust pass** trước khi execute Plan 1 self-test. Pass này expose ra thrashing production system — Plan 1 KHÔNG thể chạy reliably trong state hiện tại.

Scope audit gồm:
- FE routes — kitehub.me/* paths
- BE API endpoints — api.kitehub.me/api/*
- Spring Cloud Gateway route configuration
- Container/process state trên EC2 kh_backend
- Memory pressure + OOM history
- Trigger correlation cho restart cycle

---

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# FE route probes
for path in / /request-beta-access /login /beta-signup /verify-email /admin /admin/beta-requests /dashboard /onboarding /pricing /register; do
  curl -sS -o /dev/null -w "%{http_code}  ${path}\n" -L "https://kitehub.me${path}"
done

# BE endpoint probes
for ep in /actuator/health /api/v1/beta-access/request /api/v1/auth/login /api/v1/admin/beta-requests; do
  curl -sS -o /dev/null -w "%{http_code}  ${ep}\n" "https://api.kitehub.me${ep}"
done

# Gateway routes introspection
curl -sS https://api.kitehub.me/actuator/gateway/routes

# Container state via SSM
aws ssm send-command --instance-ids i-05d7af46d01436b96 --document-name AWS-RunShellScript \
  --parameters 'commands=["docker ps", "docker stats --no-stream", "docker events --since 5m"]'

# OOM kill history
dmesg -T --since "15 min ago" | grep -iE "oom|killed|memory"

# Spring Boot service logs
docker logs --tail 30 kitehub-admin
docker logs --tail 30 kitehub-email
docker logs --tail 30 kitehub-branding

# Workflow trigger correlation
gh run list --workflow=docker-build-push.yml --limit 5
gh run list --workflow=deploy-production.yml --limit 5

# Scheduler state (verify cost-saving disabled per PR #1233)
aws scheduler list-schedule-groups
aws scheduler list-schedules --group-name default
```

---

## Findings

### Real changes / state observed

#### F1 — FE routes (PASS — sau khi fix path drift)

| Path | Status | Note |
|---|---|---|
| `/` | 200 ✅ | Landing |
| `/request-beta-access` | 200 ✅ | Bước 2 target — path đúng là root-level (Next.js `(auth)` route group) |
| `/login` | 200 ✅ | Bước 3 admin + Bước 7 user |
| `/beta-signup` | 200 ✅ | Bước 5 signup với token |
| `/admin/beta-requests` | 200 ✅ | Bước 3 admin dashboard |
| `/verify-email` | 200 ✅ | |
| `/pricing` | 200 ✅ | |
| `/dashboard` | 200 ✅ | (may need auth — probe unauth returned 200 = SPA shell) |
| `/onboarding` | **404** ⚠️ | Mentioned in Plan 1 §5 Bước 5 ("auto-login hoặc redirect tới onboarding") — route không tồn tại |
| `/register` | **404** ⚠️ | Source `src/app/(auth)/register/page.tsx` tồn tại nhưng deploy thiếu — likely intentional (Phase 1 BETA chỉ accept signup qua invite token via `/beta-signup`) |

PR #1255 fixed Plan 1 + Playwright spec paths từ `/auth/*` → root-level.

#### F2 — BE endpoint state (FAIL — thrashing)

| Endpoint | HTTP | Diagnosis |
|---|---|---|
| `/actuator/health` | 200 | Gateway's own actuator alive |
| `/actuator/gateway/routes` | intermittent 200 / 502 | Returns route list when gateway up; 502 khi gateway service restart |
| `/api/v1/beta-access/request` (POST + JSON) | **400 empty body** | Route exists (returns 400 not 404) nhưng request handler crash/timeout mid-process |
| `/api/v1/auth/login` (POST + JSON) | **502** | Gateway can't reach downstream service |
| `/api/v1/admin/beta-requests` | **400 → 502** | Race với container restart |
| `/kitehub-{service}/actuator/health` | **404** | GAP-481 — wrong path prefix; Spring Cloud Gateway routes by path predicate, not service-name prefix |

Spring Cloud Gateway DOES have routes configured (verified via `/actuator/gateway/routes` snapshot trong moments gateway up):
- `Paths: [/api/auth/register]` Methods POST → `http://kitehub-subscription:8080`
- (Other routes truncated do response 502 trong subsequent calls)

So real API routes use prefix `/api/auth/*`, `/api/...` (path-based) — NOT `/api/v1/*` Plan 1 assumed. **Plan 1 §3 Bước 2/3/5/7 API call examples need re-calibration** to match actual gateway route table.

#### F3 — Container restart loop (ROOT FINDING — CRITICAL)

`docker ps` snapshot lúc audit (2 lần cách nhau ~2 min):

**Snapshot 1 (07:49 UTC):**
```
kitehub-subscription   Up About a minute (health: starting)
kitehub-gateway        Up 28 seconds (health: starting)
kitehub-admin          Up 26 seconds (health: starting)
kitehub-branding       Up 22 seconds (health: starting)
kitehub-email          Up 28 seconds (health: starting)
```

**Snapshot 2 (07:52 UTC):**
```
kitehub-subscription   Up About a minute (health: starting)
kitehub-gateway        Up 5 seconds (health: starting)
kitehub-admin          Up About a minute (health: starting)
kitehub-branding       Up About a minute (unhealthy)
kitehub-email          Up 19 seconds (health: starting)
```

Container uptime < 2 min consistently → **all 5 Java services trong restart loop**.

`docker events --since 1h --filter event=die` đếm được **11 die events** trên kitehub-* containers trong giờ vừa qua.

#### F4 — Root cause #1: RabbitMQ Authentication Failure

`kitehub-email` log tail:

```
2026-05-13T07:53:40.197 ERROR  Application run failed
org.springframework.context.ApplicationContextException: Failed to start bean
  'org.springframework.amqp.rabbit.config.internalRabbitListenerEndpointRegistry'
Caused by: com.rabbitmq.client.AuthenticationFailureException:
  ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN.
  For details see the broker logfile.
```

→ kitehub-email **không thể khởi động được Spring context** vì auth RabbitMQ fail → app exit → docker-compose restart → repeat. Loop infinite.

Likely root cause:
- RabbitMQ user/password trong `/etc/kite/.env` không match Rabbit's configured users
- HOẶC RabbitMQ user expired / definitions.json removed user during recent restart
- HOẶC secret rotation đã update Secrets Manager nhưng EC2 `.env` chưa pull mới

Other Java services có thể cùng pattern (chưa verify từng cái — kitehub-admin log không show rabbit dependency, kitehub-branding log show clean qua phase pre-rabbit).

#### F5 — Root cause #2: OOM Kills (container memory cgroup)

`dmesg --since "15 min ago" | grep -i oom`:

```
[Wed May 13 07:50:23 2026] C2 CompilerThre invoked oom-killer
[Wed May 13 07:50:23 2026] memory: usage 327680kB, limit 327680kB, failcnt 277
[Wed May 13 07:50:23 2026] Memory cgroup out of memory:
  Killed process 343178 (java) total-vm:2838732kB, anon-rss:322964kB,
  file-rss:21808kB, UID:1001 pgtables:952kB oom_score_adj:0

[Wed May 13 07:52:41 2026] runc:[2:INIT] invoked oom-killer
[Wed May 13 07:52:41 2026] memory: usage 327680kB, limit 327680kB, failcnt 307
[Wed May 13 07:52:41 2026] Killed process 345324 (java)
  total-vm:2837468kB, anon-rss:321756kB, file-rss:21908kB
```

2 OOM kills observed in 15 min trên container 320 MiB limit (= `kitehub-email` hoặc `kitehub-gateway` — cả 2 set 320MB limit).

JVM real footprint = heap (`-Xmx`) + non-heap (metaspace + code cache + threads + native) ≈ heap × 1.4-1.8. Container limit 320 MiB không cover JVM startup peak.

#### F6 — Memory pressure (host-level)

```
Mem total:    3.7 GiB
Mem used:     1.7 GiB
Mem free:     295 MiB
Swap:         0
```

```
Container memory limits sum: 480 + 320 + 480 + 480 + 320 = 2080 MiB
Other tenants: rabbitmq 320, redis 320, OS+docker daemon ~600 MiB = ~1240 MiB
Total demand: ~3320 MiB on 3700 MiB t3.medium = 90% baseline commit
```

→ 295 MiB free is below safety threshold. Any GC sweep + spike → OOM cascade.

GAP-447 (kc_app sizing right-size DONE 2026-05-08) chose `t3.medium 4GB` cho kh_backend với assumption: "compose budget ~2.5GB peak; 1.5GB headroom". **Assumption sai** — peak commit ~3.3 GB, headroom ~0.4 GB.

#### F7 — Workflow trigger correlation (timing mystery)

Recent workflows that COULD trigger container restart:

| Time (UTC) | Workflow | Conclusion |
|---|---|---|
| 07:41:54 | docker-build-push.yml on `main` | success |
| 07:02:51 | sync GAP-501 closure | merged |
| 06:47:45 | terraform-apply (dry_run=false GAP-501) | success |
| 04:10:44 | deploy-production.yml | success |

Restart loop visible from ~07:48 onwards (uptime <1 min mỗi container). Closest trigger = 07:41 docker-build-push success on `main`.

**Hypothesis:** docker-build-push at 07:41 pushed SHA-tagged image cho main commits. Nhưng EC2 containers run `staging.11` (version tag), không phải SHA. Vậy docker-build-push KHÔNG nên trigger redeploy. Trigger thực chưa rõ — có thể:
- Manual `docker compose up -d` từ session khác
- Cron auto-pull (chưa thấy timer khớp)
- Health check cascade (1 service unhealthy → docker-compose tries restart all)
- OOM kill → restart-policy `unless-stopped` → cascade

#### F8 — Scheduler state (verified disabled per PR #1233)

```
aws scheduler list-schedule-groups → only "default"
aws scheduler list-schedules --group-name default → (empty)
```

EventBridge cost-saving scheduler **không active**. Không phải trigger.

#### F9 — Spring Cloud Gateway routes (partial discovery)

Gateway snapshot khi up trong vài giây trả `/actuator/gateway/routes`:

```json
[
  {
    "predicate": "Paths: [/api/auth/register] match-trailing-slash: true && Methods: [POST]",
    "route_id": "auth-register",
    "filters": [
      "DedupeResponseHeader Access-Control-Allow-Credentials Access-Control-Allow-Origin",
      "RequestRateLimiter (order=1)",
      "AddResponseHeader X-Gateway-Version=1.0",
      "SpringCloudCircuitBreakerResilience4J name=authCircuitBreaker fallback=/fallback/auth"
    ],
    "uri": "http://kitehub-subscription:8080",
    "order": 0
  },
  ...
]
```

Confirms gateway has routes configured + uses path-based routing (not service-name prefix per GAP-481). Full route enumeration blocked vì gateway 502 trong subsequent calls.

### Phantom updates (none)

Não có drift state metadata loại. Findings F1-F9 đều là real state.

### Verdict

**Production trong state thrashing. Plan 1 self-test KHÔNG thể execute reliably hôm nay.** Trust level audit-result:

| Trust dimension | Pre-audit | Post-audit |
|---|---|---|
| Code merged + CI pass | Cao | Cao (CI vẫn pass — không depend production runtime) |
| Quality audit 87/100 baseline | Trung bình thấp | **Thấp** — score dựa test count, không reflect runtime stability |
| Gap `🟢 DONE` cho FE flow features | Thấp | **Rất thấp** — endpoints unstable; gap closure không validate runtime |
| Plan/docs route paths | Thấp | Vừa phải (PR #1255 fixed `/auth/*` → root-level) |
| BE API routes calibration | n/a | **Thấp** — Plan 1 §3 cite `/api/v1/*` nhưng gateway routes use `/api/auth/*` (path-based); cần re-calibrate post-stabilization |

---

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| GAP-501 ALB drift fix | 2026-05-13 06:47 | terraform apply 25783192647 SUCCESS; 502→404 verified Wave 68 |
| GAP-447 EC2 right-size t3.medium | 2026-05-08 | CSV DONE; sizing assumption now **invalidated** by F6 |
| GAP-484 OTel OTLP autoconfig fix | 2026-05-12 Wave 65 | Spring context init OK per F4 log evidence (services boot past OTel phase) |
| GAP-481 Gateway path routing | 2026-05-12 (OPEN) | F9 confirms gateway routes path-based, GAP-481 OPEN P1 still relevant |
| Wave 67 Production seed (admin user) | 2026-05-13 | admin@kitehub.me PLATFORM_ADMIN exists per Wave 67; cannot verify auth flow now do thrash |

---

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| File GAP-502 P0 BLOCKING — RabbitMQ auth + OOM thrash | Agent (this PR) | Captures F3+F4+F5+F6 findings |
| Investigate `/etc/kite/.env` RabbitMQ creds vs Rabbit user definitions | User (manual SSM exec) | Out-of-scope for read-only audit; mutation |
| Decide path forward: tune JVM heap / increase container limits / upsize t3.large | User | Per GAP-447 §Rollback path matrix; mutation |
| Re-stabilize Java services | User | manual `docker compose down + up` AFTER root cause fixed |
| Re-run audit-of-trust pass sau stabilization | Agent | Plan 1 execute chỉ khi audit pass clean |
| BE route enumeration full | Agent (next session, stable) | Calibrate Plan 1 §3 Bước 2/3 API call examples |

---

## Recommendations

1. **STOP Plan 1 execution** — defer cho đến khi RabbitMQ auth + OOM resolved
2. **File GAP-502 P0 BLOCKING** — primary fix item, blocks Phase 1 BETA launch
3. **Re-evaluate GAP-447 sizing decision** — t3.medium assumption broken; consider t3.large 8GB (~$60/mo) hoặc tune JVM `-Xmx` smaller (Java 17 docker container ergonomics: `-XX:MaxRAMPercentage=50.0` thay vì fixed -Xmx)
4. **Fix GAP-481 Gateway routing OPEN** in next wave — full route map needed for Plan 1 §3 calibration
5. **Update Plan 1 §3** with actual gateway routes once stable (likely `/api/auth/register`, `/api/auth/login`, `/api/v1/beta-access/*`, etc.)
6. **Investigate trigger** of 07:48 restart cycle — chưa explained; could be coincidence (Java services crash + restart policy cascade) hoặc external (cron, manual op trong session khác)

---

## Honest trust statement

User asked 2026-05-13: "gaps này khiến tôi mất niềm tin vào kết quả test của dự án?"

Audit-of-trust pass kết quả: **đúng, niềm tin nên thấp hơn**. Pattern thứ 3 của `feedback_e2e_scaffold_pattern_universal.md` — checkbox-DONE artifacts (gap closure, test specs, plan docs) generated từ source-scan KHÔNG bằng "production-verified".

Phải đến khi user manually probe production thì thấy:
- Route paths sai (PR #1255 fix)
- API endpoints unstable
- Container restart loop (F3)
- RabbitMQ auth fail (F4)
- OOM kills (F5)
- Memory budget exhausted (F6)

Tất cả đều là issues mà CI pass + audit /100 87 + gap DONE checkbox không catch. Quality audit /100 baseline phải refresh với "runtime stability" dimension added (~~existing dimensions: code, FE tests, docs, persona coverage; missing: runtime/production-smoke~~).

---

## References

- PR: <to-be-added>
- Branch: `feat/wave-69-audit-of-trust-gap-502`
- Sister gap: GAP-502 (P0 BLOCKING — primary fix item)
- Related: GAP-447 (sizing — invalidated), GAP-481 (gateway routing OPEN), GAP-484 (OTel DONE)
- Memory: `feedback_e2e_scaffold_pattern_universal.md` (3rd recurrence)
- Wave 68 closure: `wave-2026-05-13-68-verification-pass-and-kc-app-tg-drift.md`
- Plan 1: `documents/03-planning/end-user/plan-1-self-test-e2e.md` (cannot execute until GAP-502 fixed)
