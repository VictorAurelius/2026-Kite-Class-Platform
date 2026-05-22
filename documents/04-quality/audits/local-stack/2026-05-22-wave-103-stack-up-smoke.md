---
title: Wave 103 Bucket E — Local stack-up smoke verify (GAP-695 Tier 0)
status: complete
created: 2026-05-22
phase: phase-1-beta
wave: 103
bucket: E
gaps: [GAP-695]
---

# Wave 103 Bucket E — Local stack-up smoke

**Scope:** Verify `bash kitehub/scripts/up.sh --profile full` brings the full KiteHub stack (13 containers) to healthy state locally, gating Buckets A/B/C/D/F. Per Wave 103 plan §3 Bucket E.

**Result:** ✅ PASS — 13/13 containers healthy; 9/9 endpoints reachable; 1 config bug discovered + fixed in same bucket (kitehub-branding REDIS_HOST mismatch).

---

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1 — local Docker scope)

```bash
docker ps                                              # initial state snapshot
bash kitehub/scripts/down.sh                           # clean state
bash kitehub/scripts/up.sh --profile full              # start full profile (~3min)
docker inspect kitehub-branding --format ...           # diagnose unhealthy
docker logs kitehub-branding | tail                    # find Redis connection refused
grep -nE "REDIS|redis" docker-compose.kitehub.yml      # identify env var pattern
grep -iE "redis" kitehub-branding/.../application.yml  # find expected env name
# (1-line compose fix — see Findings)
docker compose ... up -d --no-deps --force-recreate kitehub-branding
curl -sI <each endpoint>                               # endpoint reachability
```

---

## Findings

### Service health (final state)

| # | Container | Status | Notes |
|:-:|---|---|---|
| 1 | `kite-postgres` | ✅ Up 5min healthy | port 5433 → 5432 |
| 2 | `kite-redis` | ✅ Up 5min healthy | port 6380 → 6379 |
| 3 | `kite-rabbitmq` | ✅ Up 5min healthy | port 5673 → 5672 + 15673 → 15672 mgmt |
| 4 | `kite-minio` | ✅ Up 5min healthy | port 9100 → 9000 + 9191 → 9091 |
| 5 | `kite-mailhog` | ✅ Up 5min (no healthcheck) | port 1025 (SMTP) + 8025 (UI/API) |
| 6 | `kite-gateway` | ✅ Up 2min healthy | port 9000 |
| 7 | `kitehub-subscription` | ✅ Up 5min healthy | port 8081 |
| 8 | `kitehub-branding` | ✅ Up 31s healthy | port 8083 (recreated after fix) |
| 9 | `kitehub-email` | ✅ Up 5min healthy | port 8084 |
| 10 | `kitehub-admin` | ✅ Up 5min healthy | port 8085 |
| 11 | `kiteclass-core` | ✅ Up 5min healthy | port 8088 |
| 12 | `kitehub-frontend` | ✅ Up 2min healthy | port 3001 |
| 13 | `kiteclass-frontend` | ✅ Up 2min healthy | port 3000 |

### Endpoint reachability (Tier 1 verify)

| Endpoint | Status | Verdict |
|---|:---:|:---:|
| `http://localhost:9000/actuator/health` (gateway) | 200 | ✅ |
| `http://localhost:8081/actuator/health` (platform/subscription) | 200 | ✅ |
| `http://localhost:8083/actuator/health` (branding) | 200 | ✅ (post-fix) |
| `http://localhost:8084/actuator/health` (email) | 200 | ✅ |
| `http://localhost:8085/actuator/health` (admin) | 200 | ✅ |
| `http://localhost:8088/actuator/health` (kc-core) | 200 | ✅ |
| `http://localhost:3000/` (kc-frontend SSR) | 200 | ✅ |
| `http://localhost:3001/` (kh-frontend SSR) | 200 | ✅ |
| `http://localhost:8025/api/v2/messages` (mailhog) | 404* | ⚠️ |

*Mailhog quirk: 404 status header but valid JSON body returned `{"total":0,"count":0,"start":0,"items":[]}`. Confirmed reachable + functional. Document for Bucket D agent.

### Discovered bug — kitehub-branding REDIS_HOST env var mismatch

**Root cause:** `kitehub-branding/src/main/resources/application.yml` reads `${REDIS_HOST:localhost}` (Spring Boot 3 + custom env name). `docker-compose.kitehub.yml` line 405-406 was passing `SPRING_REDIS_HOST` (Spring Boot 2 style). App fell back to `localhost:6379` → `Connection refused`.

**Evidence:**

```
docker logs kitehub-branding:
Caused by: io.netty.channel.AbstractChannel$AnnotatedConnectException:
  finishConnect(..) failed with error(-111): Connection refused: localhost/127.0.0.1:6379
```

**Cross-service env var inconsistency observed:**

| Service | Env var used in compose | Pattern |
|---|---|---|
| `kitehub-subscription` | `SPRING_REDIS_HOST` | Spring Boot 2 style |
| `kitehub-branding` | `SPRING_REDIS_HOST` (BUG — app expects `REDIS_HOST`) | Inconsistent |
| `kitehub-admin` | `SPRING_REDIS_HOST` | Spring Boot 2 style |
| `kite-gateway` | `REDIS_HOST` | Custom env name |
| `kiteclass-core` | `SPRING_DATA_REDIS_HOST` | Spring Boot 3 native |

**Fix applied (this bucket):** added `REDIS_HOST: kite-redis` + `REDIS_PORT: 6379` to kitehub-branding env block (KEPT `SPRING_REDIS_HOST` alongside for safety). 1 file changed, 4 lines added.

**Follow-up suggested (file as new gap post-Wave-103):** Standardize all 5 Redis-consuming services to Spring Boot 3 native env naming (`SPRING_DATA_REDIS_HOST`) — eliminates the 3-way inconsistency (REDIS_HOST / SPRING_REDIS_HOST / SPRING_DATA_REDIS_HOST). Lower P2 — not blocking Wave 103.

### Total time elapsed

- `down.sh` → `up.sh` → first health check: **3min 4s**
- Branding fix + recreate + healthy: **+45s**
- Endpoint verify: **+5s**
- **Total: ~4min wall-clock** (under 10min budget per plan §3 Bucket E)

---

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|---|---|---|
| Wave 102.8.1 FE image rebuild | 2026-05-21 | `docker inspect kitehub-frontend` Created field — fresh image |
| Wave 102.8 GAP-694 Docker WSL preflight | 2026-05-21 | `bash scripts/check-docker.sh` PASS (called by up.sh implicitly) |
| Wave 102.8 GAP-481 gateway routing | 2026-05-21 | gateway port 9000 responds 200 |

---

## Pending (this bucket → next bucket)

| Action | Owner | Notes |
|---|---|---|
| Spawn 5 parallel agents (A/B/C/D/F) | Coordinator | Stack ready — gate cleared |
| Each agent verify stack-health at start | Buckets A/B/C/D | `docker ps` snapshot in audit |
| Bucket D Mailhog API quirk acknowledge | Bucket D agent | 404 status + valid JSON body is normal |
| Follow-up gap: Redis env var standardization | Coordinator (post-wave) | P2 cross-service tech debt |

---

## Recommendations

1. **PROCEED** to spawn Buckets A/B/C/D/F in parallel (stack-up gate cleared)
2. **Note discovered bug** in coordinator handoff — Redis env var inconsistency exists in 3 services; only branding crashed because only branding's app.yml reads `${REDIS_HOST}` exclusively. Others fall through OK.
3. **File P2 follow-up gap** post-Wave-103 for env var standardization (out of scope this wave)

---

## References

- Wave 103 plan: `documents/03-planning/waves/wave-2026-05-22-103-local-self-test-full-walk.md` §3 Bucket E
- Parent catalog: `documents/04-quality/gaps/phase-1-beta/GAP-695-self-test-readiness-comprehensive-plan.md` Tier 0
- Prior smoke (Wave 102.8 / Tier 0 unlock): `documents/04-quality/audits/local-stack/2026-05-21-local-self-test-investigation.md`
- Compose file: `kitehub/docker-compose.kitehub.yml` (REDIS_HOST + REDIS_PORT added line 406-407)
- Related rules: `pre-mutation-state-check.md` §3 (audit artifact format), `agent-action-bias.md` §1 Part A (fix-it-yourself for sub-10min config fix)
