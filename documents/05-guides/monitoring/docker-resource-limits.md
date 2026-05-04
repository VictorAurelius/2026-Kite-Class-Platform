# Docker Resource Limits — Tuning Guide

**Status:** Active
**Created:** 2026-04-26
**Closes:** GAP-130 (P0 — host OOM / CPU starvation risk)
**Audience:** DevOps / SRE / on-call engineers
**Applies to:** All 4 compose files under `kitehub/`

---

## 1. Why limits matter

Without Docker resource limits:

- **Memory leaks cascade.** Một service rò bộ nhớ (ví dụ `kitehub-branding` giữ AI response blobs) → ngốn toàn bộ host RAM → kernel OOM killer pick container ngẫu nhiên (thường là `kite-postgres`) → toàn bộ stack down.
- **JVM hiểu sai heap size.** `JAVA_OPTS="-XX:MaxRAMPercentage=75.0"` đọc cgroup memory limit; không có Docker limit → JVM thấy host's total RAM → grows unbounded.
- **CPU starvation.** Một consumer chạy hot loop → starve gateway + postgres của CPU cycles → request timeouts cascade.
- **No FD ceiling.** Connection pool runaway → exhaust OS file descriptors.

Sau GAP-130 (Wave 7), mọi service trong 4 compose files đều có `deploy.resources.limits` + `reservations`.

---

## 2. Compose files covered

| File | Purpose | Service count |
|------|---------|---------------|
| `kitehub/docker-compose.kitehub.yml` | Canonical full stack (dev + reference for Helm) | 19 services |
| `kitehub/docker-compose.kitehub-only.yml` | KiteHub services only (no KiteClass core/FE) | 11 services |
| `kitehub/docker-compose.oracle-backend.yml` | Oracle Cloud — Backend VM (VM 1) | 8 services |
| `kitehub/docker-compose.oracle-frontend.yml` | Oracle Cloud — Frontend + AI VM (VM 2) | 4 services |

---

## 3. Chosen limits + rationale

Resource caps được đặt theo workload class. Override per-environment qua env vars (xem §5).

### 3.1 Infrastructure tier

| Service | Memory | CPU | Reservation | Rationale |
|---------|--------|-----|-------------|-----------|
| `kite-postgres` | 2g | 2.0 | 512m | Multi-tenant DB cần headroom cho shared_buffers + connection pool |
| `kite-redis` | 512m | 0.5 | 128m | Cache only, hot keyset nhỏ; light footprint |
| `kite-rabbitmq` | 1g | 1.0 | 256m | Queue + management UI Erlang VM tiêu khoảng 200-400 MB baseline |
| `kite-minio` | 512m | 0.5 | 128m | Object storage; main RAM use là index cache |
| `kite-mailhog` | 256m | 0.25 | 64m | Local SMTP dev; tiny footprint |

### 3.2 Setup / one-shot containers (exit nhanh)

| Service | Memory | CPU | Reservation | Rationale |
|---------|--------|-----|-------------|-----------|
| `kite-minio-setup` | 128m | 0.25 | 32m | `mc` CLI chạy 1 lần để tạo bucket |
| `kite-ollama-setup` | 256m | 0.5 | 64m | Pull model — IO-bound, không cần CPU |
| `kite-base` | 128m | 0.25 | 32m | Build-only profile — `echo` then exit |

### 3.3 Monitoring tier (profile `monitoring`, off by default)

| Service | Memory | CPU | Reservation | Rationale |
|---------|--------|-----|-------------|-----------|
| `kite-prometheus` | 512m | 0.5 | 128m | TSDB scrape; small instance for dev |
| `kite-grafana` | 512m | 0.5 | 128m | Dashboard rendering |

### 3.4 AI tier (profile `ai-local`, off by default)

| Service | Memory | CPU | Reservation | Rationale |
|---------|--------|-----|-------------|-----------|
| `kite-ollama` | 8g | 4.0 | 2g | Llama 3.1 8B Q4 takes ~6GB; Gemma 4 9B target similar (xem memory `feedback_gap006_infra_blocker.md`) |

### 3.5 Backend services (Spring Boot baseline)

| Service | Memory | CPU | Reservation | Rationale |
|---------|--------|-----|-------------|-----------|
| `kitehub-subscription` | 1g | 1.0 | 256m | Standard Spring Boot service; JVM heap 75% of 1g = 768MB |
| `kitehub-branding` | 2g | 2.0 | 512m | AI workload — holds Ollama responses, image bytes pre-S3-upload |
| `kitehub-email` | 1g | 1.0 | 256m | Standard service, idle-leaning |
| `kitehub-admin` | 1g | 1.0 | 256m | Standard CRUD service |
| `kite-gateway` | 512m | 1.0 | 128m | Routing only (Spring Cloud Gateway); CPU cao hơn vì proxy throughput |
| `kiteclass-core` | 1g | 1.0 | 256m | Standard backend |

### 3.6 Frontend services (Next.js production)

| Service | Memory | CPU | Reservation | Rationale |
|---------|--------|-----|-------------|-----------|
| `kitehub-frontend` | 512m | 0.5 | 128m | Next.js standalone server, low traffic admin UI |
| `kiteclass-frontend` | 512m | 0.5 | 128m | Multi-tenant student/teacher UI |
| `nginx` (oracle-frontend) | 256m | 0.5 | 64m | Reverse proxy + TLS termination |

---

## 4. Sum totals

### 4.1 Canonical (`docker-compose.kitehub.yml`)

| Profile combination | Mem sum | CPU sum |
|---------------------|---------|---------|
| **Default** (no `ai-local`, no `monitoring`, no `build-only`) | ~11.9g | 12.5 |
| **+ monitoring** | ~12.9g | 13.5 |
| **+ ai-local** | ~20.1g | 17.0 |
| **Full stack (all profiles)** | ~21.3g | 18.25 |

### 4.2 Oracle Cloud target (24GB ARM)

| Compose file | Mem sum | CPU sum | VM target |
|--------------|---------|---------|-----------|
| `oracle-backend.yml` | 9.0g | 9.5 | 12GB ARM (with headroom) |
| `oracle-frontend.yml` | 9.25g | 5.5 | 12GB ARM (Ollama dominant) |

**Constraint check:**
- ✅ Sum mem ≤ 24GB Oracle ARM target — `9.0 + 9.25 = 18.25g` < 24g (over 2 VMs)
- ⚠️ Sum CPU 12.5 (default canonical) > 8.0 typical dev box. Docker không enforce sum ≤ host cores (mỗi container scheduling độc lập), nhưng dev với host < 8 cores nên override (xem §5.3).

---

## 5. Override pattern (env var)

Mỗi service expose 3 env vars (mem limit / cpu limit / mem reservation). Format:

```
{SERVICE}_MEM_LIMIT     # default: per §3 table
{SERVICE}_CPU_LIMIT
{SERVICE}_MEM_RESERVE
```

Examples:

```bash
# Dev with 4-core / 8GB host — shrink everything
export POSTGRES_MEM_LIMIT=1g
export POSTGRES_CPU_LIMIT=1.0
export BRANDING_MEM_LIMIT=1g
export BRANDING_CPU_LIMIT=1.0
export OLLAMA_MEM_LIMIT=4g  # use smaller quantized model
export OLLAMA_CPU_LIMIT=2.0
./scripts/up.sh

# Production VM — generous (override default to 4G postgres)
export POSTGRES_MEM_LIMIT=4g
export POSTGRES_CPU_LIMIT=4.0
docker compose -f docker-compose.oracle-backend.yml up -d
```

### 5.1 Available env vars

Per service class:

- `POSTGRES_*`, `REDIS_*`, `RABBITMQ_*`, `MINIO_*` (infra)
- `SUBSCRIPTION_*`, `BRANDING_*`, `EMAIL_*`, `ADMIN_*`, `GATEWAY_*` (kitehub backends)
- `KITECLASS_CORE_*` (kiteclass core)
- `KITEHUB_FRONTEND_*`, `KITECLASS_FRONTEND_*`, `NGINX_*`
- `OLLAMA_*`

Một số short-lived containers (mailhog, minio-setup, ollama-setup, base, prometheus, grafana) không có env override — chúng chạy với fixed inline values vì không cần tune.

### 5.2 Recommended dev profile (8GB / 4-core WSL2)

```bash
# .env.dev-small
POSTGRES_MEM_LIMIT=1g
POSTGRES_CPU_LIMIT=1.0
RABBITMQ_MEM_LIMIT=512m
SUBSCRIPTION_MEM_LIMIT=768m
BRANDING_MEM_LIMIT=1g
BRANDING_CPU_LIMIT=1.0
EMAIL_MEM_LIMIT=512m
ADMIN_MEM_LIMIT=512m
KITECLASS_CORE_MEM_LIMIT=512m
GATEWAY_CPU_LIMIT=0.5
SUBSCRIPTION_CPU_LIMIT=0.5
BRANDING_CPU_LIMIT=1.0
EMAIL_CPU_LIMIT=0.5
ADMIN_CPU_LIMIT=0.5
KITECLASS_CORE_CPU_LIMIT=0.5
```

Source `.env.dev-small` trước khi `./scripts/up.sh` để fit 8GB host.

### 5.3 Recommended prod profile (Oracle ARM 24GB)

Defaults trong compose files đã calibrate cho Oracle 24GB. Không cần override trừ khi bumped to bigger model (Gemma 4 9B):

```bash
# When migrating to Gemma 4 9B (per GAP-006)
export OLLAMA_MEM_LIMIT=10g  # bump for Gemma headroom
export OLLAMA_CPU_LIMIT=4.0
```

---

## 6. Verification

### 6.1 Static (config check)

```bash
for f in kitehub/docker-compose.kitehub.yml \
         kitehub/docker-compose.kitehub-only.yml \
         kitehub/docker-compose.oracle-backend.yml \
         kitehub/docker-compose.oracle-frontend.yml; do
  echo "=== $f ==="
  docker compose -f "$f" config > /dev/null && echo "VALID"
done
```

### 6.2 Runtime (live stats)

```bash
# Run stack
./scripts/up.sh

# Verify caps respected — MEM% column should never exceed 100%
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.CPUPerc}}"

# Verify cgroup limit applied
docker inspect kitehub-branding --format '{{.HostConfig.Memory}}'  # bytes
docker inspect kitehub-branding --format '{{.HostConfig.NanoCpus}}'  # nano-cpus (1.0 cpu = 1e9)
```

### 6.3 JVM cgroup awareness check

```bash
docker exec kitehub-branding java -XshowSettings:system -version 2>&1 | grep -E "Memory|CPUs"
# Expected:
#   Operating System Metrics:
#       Provider: cgroupv2
#       Memory Limit: 2.00G
#       CPU Quota: 200000us
```

---

## 7. Tuning playbook

### Symptom: container hit memory cap (OOMKilled)

```bash
# 1. Confirm container OOMKilled
docker inspect <container> --format '{{.State.OOMKilled}}'  # true → OOM

# 2. Check JVM heap dump (if Spring Boot)
docker exec <container> jcmd 1 GC.heap_dump /tmp/heap.hprof
docker cp <container>:/tmp/heap.hprof ./

# 3. Bump limit via env var
export <SERVICE>_MEM_LIMIT=2g  # was 1g
./scripts/rebuild.sh <service>

# 4. If recurring → file gap to investigate leak (don't just bump forever)
```

### Symptom: container slow (CPU throttled)

```bash
# Check throttling
docker exec <container> cat /sys/fs/cgroup/cpu.stat | grep throttled
# nr_throttled > 0 → CPU cap too tight

# Bump
export <SERVICE>_CPU_LIMIT=2.0
./scripts/rebuild.sh <service>
```

### Symptom: Compose v3.8+ docker swarm mode warning

`deploy:` block là Compose v3 + Swarm syntax. Khi chạy `docker compose` (không phải swarm), hầu hết ignore `deploy.replicas` etc. nhưng `deploy.resources.limits` được Compose v2 honor as `mem_limit`/`cpus`. Nếu thấy warning "deploy.resources.limits not supported", upgrade Docker Compose v2.20+.

---

## 8. Helm / Kubernetes parity

Khi deploy K8s qua Helm (xem `infrastructure/helm/`), các limits trong compose phải mirror sang `values.yaml` của từng chart:

```yaml
# infrastructure/helm/kitehub-branding/values.yaml
resources:
  limits:
    memory: 2Gi
    cpu: "2"
  requests:
    memory: 512Mi
    cpu: "500m"
```

**Audit theo GAP-130 AC #5:** sau khi Helm charts shipped, cross-check compose vs values.yaml. Discrepancy = file follow-up gap. Currently tracked under ops-readiness audit follow-up.

---

## 9. Related

- Gap closed: GAP-130 (`documents/04-quality/gaps/GAP-130-docker-resource-limits-missing.md`)
- Audit source: `documents/04-quality/audits/performance/performance-audit-2026-04-19.md` §5
- Wave plan: `documents/03-planning/waves/wave-7-perf-cluster.md`
- Standards: `.claude/skills/devops/devops-standards.md`
- Memory reference: AI Branding Ollama footprint — `memory/feedback_gap006_infra_blocker.md`

---

## 10. Log

- **2026-04-26:** Document created closing GAP-130 (Wave 7 perf cluster, agent C). All 4 compose files updated with `deploy.resources.limits` + `reservations`; env-var override pattern established.
