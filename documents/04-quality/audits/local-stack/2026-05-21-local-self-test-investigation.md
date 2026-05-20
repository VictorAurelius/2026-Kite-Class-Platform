---
title: Local Stack Investigation — Solo-dev local self-test feasibility (Phase 0A)
status: complete
created: 2026-05-21
phase: phase-1-beta
wave: investigation
gaps: [GAP-691]
related-rules:
  - .claude/rules/pre-handoff-self-test-completeness.md (§2.4)
  - .claude/rules/agent-action-bias.md (§1 Part B command-over-UI)
  - .claude/rules/agent-aws-access.md (§5.1 audit format pattern)
  - .claude/rules/docs-subfolder-maturity.md (subdir creation gate)
audience: dev
---

# Local Stack Investigation — Solo-dev Self-Test Feasibility

## Scope

Empirical investigation tại sao solo dev không self-test được full stack local trên WSL distro `kite-dev` (Ubuntu 24.04, Windows 11 host). Task per upstream GAP-691 Phase 0A (investigation phase referenced trong task prompt) — read-only investigation; KHÔNG code edit, KHÔNG `.env` write, KHÔNG rule change. Mục tiêu: identify root cause classes + top 3 leverage fixes để Wave subsequent có thể unblock local self-test loop và satisfy `pre-handoff-self-test-completeness.md` §2.4 mandate (UI flow + role-guard verify).

Lưu ý discrepancy: GAP-691 ID trên main branch hiện tại trỏ tới "Wave 102.7.3 post-wave audit suite" (P1 META, audit cadence enforcement) — KHÔNG phải "local self-test investigation fix" mà task prompt mô tả. Audit artifact này tuân instruction trong task prompt + GAP context (Phase 0A scope) — file gap mới (e.g., GAP-693 hoặc tiếp theo trong sequence) có thể cần để track follow-up trong wave kế tiếp.

## Commands run (read-only, Tier 1-equivalent per `agent-aws-access.md` §2.1 pattern adapted)

| # | Command | Mục đích | Verdict |
|---|---|---|---|
| 1 | `docker version` / `docker info` | Verify Docker CLI reachable + daemon connected | ❌ `command not found` |
| 2 | `ls -la kitehub/docker-compose*.yml` | List canonical compose files | ✅ 2 files (`docker-compose.kitehub.yml` + `docker-compose.kitehub-only.yml`) |
| 3 | `ls -la kitehub/scripts/*.sh` | List Docker orchestration scripts | ✅ 16 scripts (up/down/build-all/setup/seed/etc.) |
| 4 | `ls scripts/local/` | Check repo-root local scripts dir | ❌ Không tồn tại — local scripts chỉ ở `kitehub/scripts/` |
| 5 | `ss -tlnp` filter common ports | Port conflict check (5432/5433/5672/6379/9000/8080/3000/3001/etc.) | ✅ Zero conflicts (stack chưa khởi động → ports clean) |
| 6 | `free -h` + `df -h` + `nproc` + cpuinfo | WSL2 resource baseline | ✅ 27 GiB RAM (4.1 used) + 942 GiB free + 8 cores i5-14400 |
| 7 | `which docker docker-compose` + `ls /usr/bin/docker*` | Locate Docker binary alternatives | ⚠️ Symlink tồn tại nhưng broken (`/usr/bin/docker -> /mnt/wsl/docker-desktop/cli-tools/usr/bin/docker`) |
| 8 | `cat /etc/wsl.conf` + `cat /etc/os-release` | Confirm WSL distro identity | ✅ `kite-dev` Ubuntu 24.04.4 LTS (Noble) — đúng target distro |
| 9 | `grep IntegratedWslDistros settings-store.json` | Inspect Docker Desktop WSL integration toggle config | ✅ `IntegratedWslDistros: ["kite-dev"]` — config OK (legacy v1.0 từ `feedback_agent_action_bias.md` 2026-05-07) |
| 10 | `wsl.exe -l -v` | List WSL distros + state | ❌ `docker-desktop` distro state = **Stopped** + `kite-dev` Running |
| 11 | `tasklist /FI "IMAGENAME eq Docker Desktop.exe"` | Verify Docker Desktop process running trên Windows host | ❌ `No tasks running which match` — Docker Desktop **không chạy** |
| 12 | `ls /mnt/wsl/docker-desktop/` | Verify mount point provisioned khi Docker Desktop active | ❌ `No such file or directory` — mount point absent (confirms #11) |
| 13 | `wc -l` + `grep -oE "^[A-Z_]+" .env` vs `.env.example` | Env template completeness diff | ⚠️ `.env` thiếu 9 keys so với template (HCAPTCHA × 3 + OLLAMA × 3 + AI_PROVIDER + CAPTCHA_ENABLED + NEXT_PUBLIC_KITECLASS_URL_PATTERN) |
| 14 | `grep "^  [a-z]" docker-compose.kitehub.yml` | List compose services | ✅ 25 services + named volumes (kite-base/gateway/postgres/redis/rabbitmq/minio/mailhog/ollama/prometheus/grafana + kitehub-{admin,branding,email,frontend,subscription} + kiteclass-{core,frontend}) |
| 15 | `head -80 kitehub/scripts/up.sh` + `down.sh` + `setup.sh` | Inspect orchestration logic (profiles, flags, secrets generation) | ✅ Scripts robust — `setup.sh` auto-generate `.env` với GAP-417/426 base64 hardening; `up.sh` Wave-37 GAP-407 profile system (infra-only/branding-only/beta-funnel/full/etc.) + Wave 42 GAP-425 cold-rebuild orchestration |

## Findings

### Real (verified empirically)

**Finding 1 — Docker daemon unreachable trong `kite-dev` distro:**

- `docker: command not found` mặc dù `/usr/bin/docker` symlink tồn tại
- Symlink target `/mnt/wsl/docker-desktop/cli-tools/usr/bin/docker` không tồn tại (mount point absent)
- Root cause: Docker Desktop process **Stopped** trên Windows host → docker-desktop WSL distro Stopped → mount point absent → CLI symlinks broken
- Config phía Docker Desktop ĐÚNG (`IntegratedWslDistros: ["kite-dev"]` per settings-store.json) — đây là legacy config từ session 2026-05-07 đã sửa (per `feedback_agent_action_bias.md` Docker WSL UI-loop incident); KHÔNG cần re-config Settings, chỉ cần restart Docker Desktop process

**Finding 2 — `.env` thiếu 9 keys so với `.env.example`:**

- Keys missing: `AI_PROVIDER`, `CAPTCHA_ENABLED`, `HCAPTCHA_SECRET_KEY`, `HCAPTCHA_SITE_KEY`, `NEXT_PUBLIC_HCAPTCHA_SITE_KEY`, `NEXT_PUBLIC_KITECLASS_URL_PATTERN`, `OLLAMA_BASE_URL`, `OLLAMA_TEXT_MODEL`, `OLLAMA_VISION_MODEL`
- Impact: nếu start với profile `branding-only` hoặc `full` → kitehub-branding sẽ fail (thiếu OLLAMA endpoint) + signup form fail (thiếu hCaptcha) + tenant subdomain resolution fail (thiếu `NEXT_PUBLIC_KITECLASS_URL_PATTERN`)
- `setup.sh` chỉ generate 11 core keys (Postgres/RabbitMQ/MinIO credentials + Encryption/JWT/Internal secrets + Mock OpenAI) — không touch hCaptcha/Ollama (cần manual hoặc external service)
- Severity dependent profile: `infra-only` profile sẽ unblock dù thiếu keys; `branding-only` / `beta-funnel` / `full` profile sẽ fail từng phần khi launch

**Finding 3 — Cấu trúc orchestration mature, sẵn sàng:**

- 16 scripts coverage tốt — `setup.sh` one-command bootstrap, `up.sh` với 10 profile + `--rebuild` + `--pull-from-ecr`, `status.sh` health + resource + recent errors, `wait-for-healthy.sh`, `seed-data.sh`, `test-api-e2e.sh`, `test-e2e-frontend.sh`
- 25 compose services + named volumes well-structured
- WSL2 resources thừa: 27 GiB RAM + 942 GiB disk + 8 cores → đủ chạy full profile (~18 GB stack per `up.sh` profile guide)
- Network ports clean — zero conflict trên 5433 (Postgres dev), 6380 (Redis dev), 5673 (RabbitMQ), 9000 (Gateway), 8081-8085 (KH services), 8088 (KC core), 3001 (KH frontend), 15673 (RMQ UI), 9191 (MinIO console), 8025 (MailHog) — tất cả unbound

### Phantom (KHÔNG phải vấn đề — loại trừ confounders)

- **Network port conflicts:** ❌ KHÔNG — `ss -tlnp` báo clean trên mọi port. Không cần kill conflicting process.
- **WSL2 performance / OOM:** ❌ KHÔNG — 27 GiB RAM free, 942 GiB disk, 8 cores Intel i5-14400. Hardware dư thừa cho stack ~18 GB peak.
- **Flyway baseline mismatch:** ❓ Không test được — Postgres chưa start. Defer kiểm tra tới sau khi Docker unblock + cold-start thành công.
- **Compose missing service:** ❌ KHÔNG — 25 services match expectation; KH 4 + KC 2 + 8 infra (Postgres/Redis/RabbitMQ/MinIO + Mailhog/Ollama/Prometheus/Grafana) + Gateway + auxiliary (kite-base/minio-setup/ollama-setup/minio-backup) + named volumes.
- **JVM heap / memory limits:** ❓ Không test được — services chưa start. Defer.
- **Docker Desktop config sai (WSL integration toggle off):** ❌ KHÔNG — `IntegratedWslDistros: ["kite-dev"]` trong settings-store.json. Config đã đúng từ session 2026-05-07.

## Top 3 root causes (ranked by leverage)

### #1 — Docker Desktop process not running trên Windows host (P0 BLOCKING — STOP-AND-FIX FIRST)

**Severity:** P0 — block 100% local self-test. Tất cả container ops không khả thi.

**Evidence:**
- `wsl.exe -l -v`: `docker-desktop` state = `Stopped`
- `tasklist`: zero process matching `Docker Desktop.exe`
- `/mnt/wsl/docker-desktop/`: directory không tồn tại
- `/usr/bin/docker` symlink dangling (target không reachable)

**Recommended fix (per `agent-action-bias.md` §1 Part B command-over-UI):**

```bash
# Trong WSL kite-dev, dùng /mnt/c/Windows path để launch Docker Desktop:
cmd.exe /c start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Hoặc PowerShell từ kite-dev (preferred per agent-action-bias.md):
powershell.exe -NoProfile -Command "Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'"

# Đợi 30-60s cho Docker Desktop boot + WSL integration sync, sau đó verify:
docker version 2>&1 | head -5
docker info 2>&1 | head -10

# Kỳ vọng:
# - docker version: Client version + Server version (engine)
# - docker info: Server Version 28.x, OS/Arch Linux/amd64, WSL2 backend, ~6 containers planned
```

**Verify post-launch:**
```bash
wsl.exe -l -v   # docker-desktop state = Running
ls /mnt/wsl/docker-desktop/cli-tools/usr/bin/docker   # symlink reachable
```

**Estimated effort:** ~5 phút (Docker Desktop boot time + verify).

**Persistence pattern:** Docker Desktop nên enable auto-start on Windows login để tránh recurrence sau reboot. Settings → General → "Start Docker Desktop when you sign in to your computer" toggle ON. Hoặc Windows shortcut trong `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup`.

### #2 — `.env` thiếu 9 keys cho non-infra-only profiles (P1 — block branding/full profile)

**Severity:** P1 — block 60% local self-test scope (branding-only + beta-funnel + full profiles). `infra-only` profile vẫn launch được.

**Evidence:**
- `diff <(grep -oE "^[A-Z_]+" .env.example | sort -u) <(grep -oE "^[A-Z_]+" .env | sort -u)` → 9 lines missing
- Categories missing: hCaptcha (3 keys — signup spam protection), Ollama (3 keys — AI branding generation), AI_PROVIDER (1 — provider selector), CAPTCHA_ENABLED (1 — toggle), NEXT_PUBLIC_KITECLASS_URL_PATTERN (1 — tenant subdomain resolver)

**Recommended fix:** sau khi Docker unblock + cold-start `infra-only` profile success, populate `.env`:

```bash
# Append missing keys với dev-safe defaults (KHÔNG commit real captcha secrets):
cat >> kitehub/.env <<'EOF'

# --- AI Provider (Wave 4 / GAP-018) ---
AI_PROVIDER=mock          # mock | openai | ollama
OLLAMA_BASE_URL=http://kite-ollama:11434
OLLAMA_TEXT_MODEL=llama3.2:3b
OLLAMA_VISION_MODEL=llava:7b

# --- hCaptcha (Wave 35 signup hardening) ---
CAPTCHA_ENABLED=false                          # Tắt cho local self-test
HCAPTCHA_SITE_KEY=10000000-ffff-ffff-ffff-000000000001    # hCaptcha test key (always pass)
HCAPTCHA_SECRET_KEY=0x0000000000000000000000000000000000000000  # test secret
NEXT_PUBLIC_HCAPTCHA_SITE_KEY=10000000-ffff-ffff-ffff-000000000001

# --- Tenant URL pattern (KiteClass subdomain resolution) ---
NEXT_PUBLIC_KITECLASS_URL_PATTERN=http://{tenant}.localhost:8088
EOF

# Verify all 20 keys present:
diff <(grep -oE "^[A-Z_]+" kitehub/.env.example | sort -u) <(grep -oE "^[A-Z_]+" kitehub/.env | sort -u)
# Kỳ vọng: empty diff
```

**Verify post-edit:** không commit `.env` (gitignored per convention). Restart affected services: `kitehub/scripts/restart.sh kitehub-branding kitehub-subscription kitehub-frontend`.

**Estimated effort:** ~10 phút (append + restart + verify branding endpoint reachable).

**Alternative:** mở rộng `setup.sh` để generate 9 keys missing với dev defaults — track separate gap (`setup.sh` enhancement). Lower priority — không block immediate fix #1+#2.

### #3 — One-command unblock script cho recurrence (P2 META force-multiplier)

**Severity:** P2 META per `meta-gap-priority.md` §3 — force-multiplier cho mỗi session subsequent (eliminate "lại không chạy được" recurrence cost).

**Evidence:**
- Pattern recurrence: 2026-05-07 Docker WSL UI-loop incident (per `feedback_agent_action_bias.md`) đã sửa config + `IntegratedWslDistros: ["kite-dev"]` — nhưng KHÔNG cover case Docker Desktop process not running
- Mỗi session start sau Windows reboot → Docker Desktop có thể stopped → session block lại từ đầu
- Existing `setup.sh` assume Docker daemon đã reachable — fail-fast nếu không

**Recommended fix:** tạo `kitehub/scripts/check-docker.sh` (preflight check) + integrate vào `up.sh` / `setup.sh`:

```bash
#!/bin/bash
# kitehub/scripts/check-docker.sh — verify Docker daemon reachable trước khi up/setup
# Per .claude/rules/agent-action-bias.md §1 Part B (command-over-UI)

set -e

# Quick check
if docker version >/dev/null 2>&1; then
  echo "[check-docker] ✅ Docker daemon reachable"
  exit 0
fi

echo "[check-docker] ❌ Docker daemon unreachable"
echo "[check-docker] Diagnostics:"

# Detect WSL2 environment
if grep -qi microsoft /proc/version 2>/dev/null; then
  WSL_STATE=$(/mnt/c/Windows/System32/wsl.exe -l -v 2>&1 | tr -d '\0' | grep -iE 'docker-desktop' || echo "not-found")
  echo "  - WSL distro 'docker-desktop' state: $WSL_STATE"

  if /mnt/c/Windows/System32/tasklist.exe /FI 'IMAGENAME eq Docker Desktop.exe' 2>&1 | grep -q 'Docker Desktop.exe'; then
    echo "  - Docker Desktop.exe: running"
  else
    echo "  - Docker Desktop.exe: NOT running"
    echo ""
    echo "[check-docker] Auto-launch attempt..."
    powershell.exe -NoProfile -Command "Start-Process 'C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe'" 2>/dev/null
    echo "[check-docker] Docker Desktop launching — wait 30-60s then re-run this script"
  fi
fi

exit 1
```

Integrate vào `up.sh` line 1 sau `set -e`:
```bash
bash "$(dirname "$0")/check-docker.sh" || { echo "[up.sh] Docker preflight failed. See output above."; exit 1; }
```

**Estimated effort:** ~30-45 phút (write script + 3 fixtures self-test + integrate vào up.sh + setup.sh + document trong `documents/05-guides/local-dev/wsl2-fresh-setup.md`).

**Force-multiplier value:** mỗi session subsequent sau Windows reboot tiết kiệm 1 user round-trip ("docker không chạy được" → diagnose → manually launch). Per Wave Obs ~5x speedup pattern.

## Pending follow-ups

| Action | Owner | When | Notes |
|---|---|---|---|
| Apply Fix #1 (launch Docker Desktop) | User trong session sau | Immediately when need local self-test | Per `agent-action-bias.md` §1 Part B — claude có thể run `powershell.exe -NoProfile -Command "Start-Process ..."` nếu user explicit cho phép |
| Apply Fix #2 (`.env` populate 9 keys) | User / Claude | Sau Fix #1 verified | Dev-safe defaults; KHÔNG commit `.env` (gitignored) |
| Apply Fix #3 (preflight `check-docker.sh`) | Wave subsequent (planning) | Theo wave 92+ scope | Force-multiplier META gap — file new gap để track |
| Wave 92+ scope: file GAP-693 (hoặc next sequence) cho persistent local self-test enablement | Wave-pack planner | Wave 92 brainstorm phase | Cover Fix #1+#2+#3 + persistence (Docker Desktop auto-start Windows login) + `wsl2-fresh-setup.md` update |
| Re-cold-start attempt sau Fix #1+#2 | Claude / User session sau | Phase 0B (per task prompt phasing) | Run `bash kitehub/scripts/up.sh --profile infra-only` đầu tiên (lightweight 1.5 GB), verify Postgres/Redis/RabbitMQ/MinIO healthy; sau đó scale lên `branding-only` hoặc `full` |
| Flyway baseline + schema verify (deferred) | Phase 0B | Sau infra-only stack up | `docker exec kite-postgres psql -U kitehub -d kitehub -c "select count(*) from flyway_schema_history"` |
| Service health audit + JVM heap baseline (deferred) | Phase 0B | Sau backend services up | `docker stats --no-stream` + `status.sh` |
| Update `wsl2-fresh-setup.md` Phase 0 prerequisites | Wave 92+ | Cùng wave với Fix #3 | Add "Docker Desktop auto-start on Windows login" guidance + cite this audit |
| **Pre-handoff smoke check** per `pre-handoff-self-test-completeness.md` §2.4 | Wave deploy admin login post-Phase 0B unlock | Sau full stack up | Currently BLOCKED — không thể satisfy §2.4 admin-flow checklist (login + role-guard + nav + render + action) trên local cho tới khi Fix #1 unblock |

## References

- **Upstream task:** GAP-691 Phase 0A (per task prompt — investigation phase; gap file actual on main = "Wave 102.7.3 post-wave audit suite" P1 META, discrepancy noted §Scope)
- **Cross-referenced rules:**
  - [`.claude/rules/pre-handoff-self-test-completeness.md`](../../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.4 admin-flow checklist — Local stack unblock prerequisite trước satisfy mandate
  - [`.claude/rules/agent-action-bias.md`](../../../../.claude/rules/agent-action-bias.md) §1 Part B — Command-over-UI cho fix recommendations
  - [`.claude/rules/agent-aws-access.md`](../../../../.claude/rules/agent-aws-access.md) §5.1 — Audit artifact format pattern adapted cho local scope
  - [`.claude/rules/dev-readable-doc-language.md`](../../../../.claude/rules/dev-readable-doc-language.md) §2-§4 — Vietnamese narrative + English identifier
  - [`.claude/rules/docs-subfolder-maturity.md`](../../../../.claude/rules/docs-subfolder-maturity.md) §2 — Subdir creation gate satisfied (Volume planned ≥5 files + Sister-pattern `aws-verification/`)
- **Existing setup guides:**
  - [`documents/05-guides/local-dev/wsl2-fresh-setup.md`](../../../05-guides/local-dev/wsl2-fresh-setup.md) — 60-90 min full setup procedure
  - [`documents/05-guides/local-dev/wsl-migration-playbook.md`](../../../05-guides/local-dev/wsl-migration-playbook.md) — Existing Windows install migration path
  - [`documents/05-guides/local-dev/local-dev-setup-non-wsl.md`](../../../05-guides/local-dev/local-dev-setup-non-wsl.md) — Mac/Linux native alternative
- **Prior incident:** 2026-05-07 Docker WSL UI-loop (per `feedback_agent_action_bias.md` memory) — `IntegratedWslDistros` config fix; this audit surfaces orthogonal failure mode (Docker Desktop process not running)
- **Scripts referenced:** `kitehub/scripts/{up,down,setup,status,seed-data,test-api-e2e,wait-for-healthy}.sh`
- **Compose canonical:** `kitehub/docker-compose.kitehub.yml` (25 services + named volumes + 10 profiles per GAP-407 Wave 37)
