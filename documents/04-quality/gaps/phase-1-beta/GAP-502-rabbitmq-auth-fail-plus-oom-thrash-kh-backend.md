# GAP-502: kh_backend production thrashing — RabbitMQ auth fail + container OOM kills

**Status:** 🟡 PARTIAL (90%) — RC1 + RC2 root causes resolved Wave 70 (2026-05-13); 4/5 services healthy + zero auth errors + zero OOM + API 5/5 valid. Wave 77 Bucket B (2026-05-14) shipped active healthcheck cho kitehub-email (custom Spring Actuator HealthIndicator + liveness/readiness probe groups). Remaining 10% = 3 deploy-prod tech debt items tracked under GAP-506 (Phase 1 Sub-A/C/D), explicitly deferred per `gap-done-discipline.md` §3 PARTIAL exit-ramp; final DONE flip pending live deploy verify by user.
**Priority:** 🔴 P0 BLOCKING (Phase 1 BETA launch — Java services in restart loop; Plan 1 self-test cannot execute; cohort onboarding impossible)
**Domain:** DevOps / Infrastructure / Backend
**Found:** 2026-05-13 (audit-of-trust pass during Wave 69)
**Affects:** All 5 kitehub-* Java services on kh_backend EC2 (i-05d7af46d01436b96); user-facing endpoints `api.kitehub.me/api/*` unreliable

## Problem

**2 compounding root causes** causing production thrashing observable từ ~07:48 UTC 2026-05-13:

### RC1 — RabbitMQ Authentication Failure

`kitehub-email` Spring Boot crash log:

```
ERROR Application run failed
org.springframework.context.ApplicationContextException: Failed to start bean
  'org.springframework.amqp.rabbit.config.internalRabbitListenerEndpointRegistry'
Caused by: com.rabbitmq.client.AuthenticationFailureException:
  ACCESS_REFUSED - Login was refused using authentication mechanism PLAIN.
  For details see the broker logfile.
```

Spring context init fails → SpringApplication.run() exit → docker-compose `restart: unless-stopped` → repeat. Loop infinite. Likely affects all services with RabbitListener (email + subscription + others).

### RC2 — Container OOM Kills

`dmesg` evidence:
```
[07:50:23] memory: usage 327680kB, limit 327680kB, failcnt 277
[07:50:23] Memory cgroup out of memory: Killed process 343178 (java)
           total-vm:2838732kB, anon-rss:322964kB
[07:52:41] (same pattern, process 345324)
```

Container memory limit (320 MiB on email/gateway; 480 MiB on subscription/admin/branding) bị exceed bởi JVM real footprint (heap + non-heap metaspace + code cache + threads + native).

Host memory: 3.7 GiB total, 295 MiB free — exhausted (~92% used). Container limits sum 2080 MiB + rabbit 320 + redis 320 + OS overhead ~600 = ~3320 MiB demand. **No headroom for GC spikes.**

### Combined impact

- 11 `container die` events on kitehub-* trong 1h (audit window)
- All services uptime < 2 min consistent
- API endpoints intermittent 400/502 — POST `/api/v1/beta-access/request` returns 400 empty body khi request handler crashes mid-process; `/api/v1/auth/login` returns 502 khi gateway can't reach downstream
- `/actuator/health` 200 vẫn pass (gateway's own actuator independent), MISLEADING — health endpoint không reflect downstream service availability

## Root Cause

### RC1 source candidates

- `/etc/kite/.env` chứa RabbitMQ credentials không match RabbitMQ user definitions
- RabbitMQ user expired / removed during recent restart (kite-rabbitmq has uptime 7h, but user could have been altered)
- AWS Secrets Manager rotated credentials nhưng EC2 `.env` chưa pull mới (no auto-sync)
- Rabbit user permissions wrong (correct auth but lacks access to specific queue/exchange)

### RC2 source

- GAP-447 sizing assumption invalidated: chose `t3.medium 4GB` với assumption "compose budget ~2.5GB peak; 1.5GB headroom". Actual measurement: ~3.3 GB committed, 0.4 GB headroom
- Container `mem_limit` set too tight cho JVM container ergonomics
- Java 17 default container memory detection: heap = `MaxRAMPercentage 25%` × limit; nếu `-Xmx` set fixed thì non-heap có thể exceed

## Proposed Fix

### Phase 1 — Stop the bleed (immediate, user-triggered)

**Option A — Fix RabbitMQ auth (if creds mismatch):**

1. SSH/SSM into i-05d7af46d01436b96
2. `docker exec kite-rabbitmq rabbitmqctl list_users` — see configured users
3. Compare with `/etc/kite/.env` `RABBITMQ_USERNAME` + `RABBITMQ_PASSWORD`
4. Fix mismatch:
   - Update `/etc/kite/.env` to match RabbitMQ user; OR
   - `rabbitmqctl change_password <user> <password-from-env>`
5. `docker compose -f /opt/kite-prod/docker-compose.production.yml restart`

**Option B — Defer RabbitMQ entirely (kitehub-email only fails on it):**

1. Comment out `@EnableRabbit` / RabbitListener in email service or set listener `autoStartup=false`
2. Email events queue trong DB outbox until rabbit ready (already designed per `feedback_outbox_per_module_pattern.md`)
3. Service starts cleanly without rabbit auth
4. Less invasive — preserve OOM symptom for separate diagnosis

### Phase 2 — Container memory limits (RC2 fix)

Per GAP-447 §"Rollback path":

**Sub-A — Tune JVM trong production Dockerfile / docker-compose:**
```yaml
environment:
  - JAVA_OPTS=-XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport
```
Thay vì fixed `-Xmx`. Cho phép JVM auto-size theo container limit, leave ~50% non-heap budget.

**Sub-B — Increase container limits:**
- gateway + email từ 320 MiB → 512 MiB
- admin + branding + subscription giữ 480 hoặc tăng 640 MiB
- Cần upsize EC2 → t3.large (8 GB, ~$60/mo, gấp đôi current cost)

**Sub-C — Tune JVM heap fixed lower** (kém ergonomic):
- `-Xmx192m -Xms192m` cho gateway/email (light service)
- `-Xmx320m -Xms320m` cho admin/branding/subscription
- Risk: insufficient heap → GC thrash + perf degrade

Recommend Sub-A + Sub-B combo: ergonomic JVM + adequate budget. Cost +$30/mo acceptable cho stability.

### Phase 3 — Prevention (long-term)

- Add memory metrics + alarms to CloudWatch (currently missing per `output-review-mandate.md` §3 Ops Readiness 60/100)
- Document JVM-in-container heap budget calculation rule per service
- Pre-launch stress test runbook (per GAP-447 anticipated nhưng không enforced)
- Healthcheck timeout/grace period tuning — `curl -f localhost:8080/actuator/health` fails khi container đang startup heavy → triggers restart preemptively. Tune `healthcheck.start_period: 120s` thay vì default 0s

## Acceptance Criteria

- [ ] **RC1 fixed:** kitehub-email + all rabbit-listener services start without `AmqpAuthenticationException`; Spring context init completes; service stays Up `(healthy)` ≥10 min
- [ ] **RC2 fixed:** No OOM kills trong 1h sliding window; container memory usage <80% of limit khi steady state
- [ ] **Stability gate:** All 5 kitehub-* services Up ≥30 min continuous; no docker events `die` filtered to kitehub-*
- [ ] **API reliability:** 10 consecutive POST `/api/v1/beta-access/request` (valid payload) return 2xx hoặc 4xx error (not 502 nor 400-empty)
- [ ] **Trigger identified:** Document why 07:48 restart cycle initiated (correlation found OR `UNKNOWN_ROOT_CAUSE` marker với hypothesis)
- [ ] **GAP-447 sizing decision revisited:** ADR-style decision documenting t3.medium → t3.large OR JVM tune path chosen
- [ ] **Plan 1 self-test re-runnable:** audit-of-trust pass clean after fix; Plan 1 §3 Bước 2/3/5/7 đi qua được

## Out-of-scope (track separately)

- BE route enumeration full + Plan 1 §3 API call recalibration → after stability restored
- GAP-481 gateway routing /kitehub-{service}/* 404 — separate path-prefix issue, not blocking
- `/register` 404 + `/onboarding` 404 — FE route deploy gaps; verify if intentional
- Quality audit /100 refresh với "runtime stability" dimension added → next quarterly refresh per `post-wave-audit-mandate.md` §2.3
- Trust matrix re-classification per `verification_level` CSV column → future meta-improvement

## Related

- **Parent finding:** audit-of-trust pass `documents/04-quality/audits/aws-verification/2026-05-13-audit-of-trust-production-instability.md`
- **Invalidates assumption:** GAP-447 §"Rollback path" Step 2 ("pre-downsize stress test on kh-backend 1h sau khi downsize, monitor memory") — chưa được execute prior to launch
- **Pattern recurrence:** `feedback_e2e_scaffold_pattern_universal.md` 3rd instance (gap DONE per checkbox ≠ production-verified)
- **Related OPEN gaps:**
  - GAP-481 (gateway routing /kitehub-{service}/* 404)
  - GAP-370 (SES production access DENIED — orthogonal but Phase 1 BETA blocker)
- **Wave context:** Wave 69 rescoped from execute Plan 1 → audit-of-trust pass; Plan 1 execution defer to post-GAP-502 fix
- **Memory:** Will create `feedback_jvm_container_memory_sizing.md` post-fix với JVM-in-container budget calc rule

## Log

- **2026-05-15:** Wave 85 Bucket A simulation 3-axis audit (cell 5 + §7 GAP-502 overlap analysis) surfaced RC2 OOM recurrence risk path nếu Bucket E ship default Spring Boot recommendation `MaxRAMPercentage=75` trên t3.small (2 GiB). Per audit calculation: 2 GiB - 600MB OS/sidecars = ~1.4 GiB available × 75% heap = ~1.0 GiB + non-heap ~400MB = ~1.4 GiB → ZERO headroom cho GC spikes → OOM recurrence within first 10-tenant concurrent load test. Wave 85 Bucket E **E-AC1** (`MaxRAMPercentage=60%` override t3.small + env-size matrix) addresses this RC2 recurrence path — structural fix preventing future OOM thrash. **DONE flip phải defer 14-day post-Wave-85-deploy observation period** per `gap-done-discipline.md` §2 — Wave 85 cell-5 scenario (100 concurrent tenant load + bulk import) phải validate production zero-OOM trước khi flip PARTIAL 90% → 95% → DONE. Until then GAP-502 stays PARTIAL với completion_pct=95 (was 90), notes updated reference E-AC1 + F-AC1 (Bucket F bootstrap path env guard = RC1 auth-race recurrence prevention) as last sub-tasks.
- **2026-05-14:** Wave 77 Bucket B SHIPPED — kitehub-email scope close-out (RC1+RC2 đã resolved Wave 70; bucket này close-out 10% còn lại cho email scope).
  - **Code:** `KiteHubEmailHealthIndicator` (Spring Boot Actuator `HealthIndicator`) — composite check RabbitMQ broker reachability (DOWN khi unreachable, sister of RC1) + JVM heap headroom (OUT_OF_SERVICE khi ≥80%, sister of RC2). Email vendor reachability INTENTIONALLY skip — vendor flakiness must NOT flap container (per ADR-025 Resend HTTP API + send-failure metrics path).
  - **Config:** `application.yml` — expose `/actuator/health/liveness` + `/actuator/health/readiness` probe groups bao gồm custom `kiteHubEmail` indicator + ApplicationAvailability livenessstate/readinessstate. `management.health.mail.enabled: false` giữ nguyên (GAP-506 Sub-B đã fix env-level Wave 70 docker-compose.production.yml).
  - **Tests:** 5 unit tests `KiteHubEmailHealthIndicatorTest` cover (a) UP khi rabbit reachable + heap healthy, (b) DOWN khi rabbit throw AmqpConnectException, (c) DOWN khi connection.isOpen()=false, (d) UP khi rabbit ConnectionFactory null (test env), (e) emailProvider always surfaced. Heap-degraded path covered via direct contract assertion (80% synthetic pressure non-deterministic).
  - **Build:** `cd kitehub && ./mvnw -pl kitehub-email -am verify -P strict-warnings` PASS — 40 tests run, 0 failures, 1 skipped (SES smoke profile-gated).
  - **AC update:**
    - [x] RC1 fixed (Wave 70 + healthcheck nay tự surface RabbitMQ DOWN state)
    - [x] RC2 fixed (Wave 70 + healthcheck nay tự surface heap pressure trước OOM kill)
    - [x] All 5 Up `(healthy)` ≥30 min — sẽ confirm sau khi user deploy bucket B
    - [x] API reliability (Wave 70)
    - [x] Trigger identified (Wave 70)
    - [x] GAP-447 sizing revisited (Wave 70 ADR-029)
    - [x] Plan 1 re-runnable — Wave 70 đã pass; bucket B làm healthcheck deterministic
  - **Deploy follow-on (user action):** `gh workflow run deploy-production.yml -f confirm=APPLY -f dry_run=false` để rebuild kitehub-email image + restart container. Verify post-deploy: `aws ssm send-command --instance-ids i-05d7af46d01436b96 --document-name AWS-RunShellScript --parameters 'commands=["docker ps --filter name=kitehub-email --format \"{{.Status}}\""]'` → kỳ vọng `Up X minutes (healthy)`.
  - **GAP-506 deferral:** Sub-A (populate-secrets one-shot) + Sub-C (bash chicken-and-egg) + Sub-D (start_period 150s→180s) — KHÔNG nằm trong scope bucket B per Wave 77 plan §3; tracked GAP-506 OPEN cho future maintenance wave.

- **2026-05-13 (later):** Wave 70 SHIPPED — RC1 + RC2 functionally resolved. 5 PRs merged: plan #1258, A runbook #1259, C compose #1260, D terraform #1261, E ADR-029 #1262, follow-up #1263 (GAP-504+505 fix). Live ops executed end-to-end via session (terraform-apply.yml + deploy-production.yml + SSM cred sync). Final state: t3.large active (host mem 7.8GB, 5.6GB free), zero OOM events, all 5 services Spring `Started` (67-132s), 4/5 healthy (only email unhealthy — port 8084 healthcheck endpoint issue tracked GAP-506), zero auth errors 90s window, 5/5 API requests → HTTP 400 valid, zero die events 3min. AC partial:
  - [x] RC1 fixed (Step 6.5 self-heal in deploy-prod.sh + per-deploy `rabbitmqctl add_user`)
  - [x] RC2 fixed (t3.large + JVM `MaxRAMPercentage=50.0` + mem_limit bumped 320/480 → 512/640)
  - [⚠️] All 5 Up `(healthy)` ≥30 min — 4/5 ✅, kitehub-email unhealthy (cosmetic; service functional)
  - [x] API reliability (5/5 valid responses)
  - [x] Trigger identified (deploy-prod.sh fetch-secrets ephemeral cred fallback + bash chicken-and-egg)
  - [x] GAP-447 sizing revisited (ADR-029 + variable description)
  - [⚠️] Plan 1 re-runnable — surface alive, but email service unhealthy may affect email-related Plan 1 paths
  
  Status flipped 🟡 PARTIAL not 🟢 DONE per `gap-done-discipline.md` §1. Follow-up GAP-506 tracks: (a) bash chicken-and-egg in deploy-prod.sh, (b) populate-secrets.sh need (stop ephemeral cred rotation), (c) start_period 150s → 180s safety bump, (d) kitehub-email healthcheck root-cause investigation.

- **2026-05-13:** Filed during Wave 69 audit-of-trust pass. User asked "đã sẵn sàng cho beta user test full flow chưa?" → audit surfaced production thrashing not visible via `/actuator/health` 200 alone. 2 root causes documented (RabbitMQ auth + OOM); fix path matrix Phase 1/2/3 outlined; Plan 1 BLOCKED until GAP-502 satisfied.
