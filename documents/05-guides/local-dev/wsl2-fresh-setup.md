# WSL2 Fresh-Machine Setup — Clean-Room Reproducer

**Last Updated:** 2026-04-28
**Purpose:** Stand up a working dev environment for `2026-Kite-Class-Platform` from scratch on a brand-new machine running Windows 10/11 + WSL2.
**Audience:** Solo dev (primary) + new contributor onboarding.
**Time:** 60–90 min on a fast machine with good network.

> **Migrating from an existing Windows install (D:\\... → ~/projects/...)?** Use [`wsl-migration-playbook.md`](wsl-migration-playbook.md) instead — it covers preserving Claude Code memory + conversation history + credential migration.
>
> **Mac / native Linux?** Use [`local-dev-setup-non-wsl.md`](local-dev-setup-non-wsl.md). Most steps below still apply — skip Phase 1 WSL bits.

---

## What you get at the end

- Project cloned at `~/projects/2026-Kite-Class-Platform/` (Linux ext4 — never `/mnt/c/...`)
- Full Docker stack runnable via `cd kitehub && ./scripts/up.sh` — ~20 containers (Postgres, Redis, RabbitMQ, MinIO + setup + backup, Mailhog, Prometheus, Grafana, optional Ollama, kite-base, kitehub-{subscription,branding,email,admin}, kite-gateway, kiteclass-{core,gateway,frontend}, kitehub-frontend)
- Both Maven test suites green (`mvn -pl kitehub-branding test` etc.)
- Both frontends building (KiteHub via pnpm, KiteClass via npm)
- Claude Code wired with GitHub MCP server connected
- RTK proxy active (token-saving for dev commands)

---

## Phase 0 — Prerequisites (Windows host, ~5 min)

On the Windows machine, in PowerShell as **Administrator**:

```powershell
# 1. Verify WSL2 is available
wsl --status

# 2. Set WSL2 as default
wsl --set-default-version 2
```

### Option A — Quick install (default distro, default location)

Dùng khi chỉ cần 1 instance duy nhất, lưu mặc định ở `%LocalAppData%\Packages\...`.

```powershell
# Install Ubuntu 24.04 LTS
wsl --install -d Ubuntu

# → Tự động tạo user + password khi lần đầu launch
```

### Option B — Custom instance (recommended cho dự án)

Dùng khi muốn: đặt tên riêng, chọn ổ đĩa lưu, chạy nhiều instance song song, hoặc dễ backup/migrate.

#### B1. Tải rootfs image

```powershell
# Cách 1: Tải rootfs từ cloud images (Ubuntu 24.04 — recommended)
# Vào: https://cloud-images.ubuntu.com/wsl/noble/current/
# Tải file: ubuntu-noble-wsl-amd64-wsl.rootfs.tar.gz
# Hoặc dùng curl:
curl -L -o "$env:USERPROFILE\Downloads\ubuntu-24.04-rootfs.tar.gz" `
  "https://cloud-images.ubuntu.com/wsl/noble/current/ubuntu-noble-wsl-amd64-wsl.rootfs.tar.gz"

# Cách 2: Export từ distro có sẵn (nếu đã có Ubuntu cài sẵn)
wsl --export Ubuntu "$env:USERPROFILE\Downloads\ubuntu-backup.tar"
```

#### B2. Tạo custom instance

```powershell
# Chọn tên + thư mục lưu (ví dụ: ổ F:\, tên "kite-dev")
$NAME = "kite-dev"
$INSTALL_DIR = "F:\WSL\$NAME"
$ROOTFS = "$env:USERPROFILE\Downloads\ubuntu-24.04-rootfs.tar.gz"

# Tạo thư mục đích
New-Item -ItemType Directory -Force -Path $INSTALL_DIR

# Import — tạo instance mới từ rootfs
wsl --import $NAME $INSTALL_DIR $ROOTFS --version 2

# Verify
wsl -l -v
#   NAME        STATE    VERSION
#   kite-dev    Stopped  2
```

#### B3. Tạo user (không dùng root)

`wsl --import` mặc định login root. Phải tạo user thường:

```powershell
# Vào instance với quyền root
wsl -d kite-dev

# === Bên trong WSL (đang là root) ===
```

```bash
# Tạo user mới
NEW_USER="kitedev"
adduser $NEW_USER
# → Nhập password khi được hỏi

# Thêm vào sudo group
usermod -aG sudo $NEW_USER

# Đặt user mặc định khi launch (thay vì root)
cat > /etc/wsl.conf <<EOF
[user]
default=$NEW_USER

[boot]
systemd=true

[interop]
appendWindowsPath=false
EOF

exit
```

```powershell
# Restart instance để áp dụng wsl.conf
wsl --terminate kite-dev
wsl -d kite-dev whoami
# → kitedev (không phải root)
```

#### B4. Đặt làm default (optional)

```powershell
# Nếu muốn `wsl` (không -d) tự vào instance này
wsl --set-default kite-dev
```

#### B5. Quản lý nhiều instances

```powershell
# Liệt kê tất cả instances
wsl -l -v

# Vào instance cụ thể
wsl -d kite-dev

# Vào với user cụ thể (override default)
wsl -d kite-dev -u root

# Stop 1 instance
wsl --terminate kite-dev

# Stop tất cả WSL
wsl --shutdown

# Xóa instance (CẢNH BÁO: xóa toàn bộ filesystem!)
wsl --unregister kite-dev

# Backup instance (export → tar)
wsl --export kite-dev "F:\Backup\kite-dev-backup.tar"

# Restore / clone instance từ backup
wsl --import kite-dev-v2 "F:\WSL\kite-dev-v2" "F:\Backup\kite-dev-backup.tar" --version 2
```

#### B6. Di chuyển instance sang ổ khác

```powershell
# Export → Unregister → Import lại ở vị trí mới
$NAME = "kite-dev"
$BACKUP = "$env:USERPROFILE\Downloads\$NAME-migrate.tar"
$NEW_DIR = "D:\WSL\$NAME"

wsl --export $NAME $BACKUP
wsl --unregister $NAME
New-Item -ItemType Directory -Force -Path $NEW_DIR
wsl --import $NAME $NEW_DIR $BACKUP --version 2

# Verify — user default vẫn giữ nguyên (lưu trong /etc/wsl.conf)
wsl -d $NAME whoami
```

### WSL memory + performance tuning

```powershell
# (One-time) Increase WSL memory cap if your machine has ≥16 GB RAM.
# Create %UserProfile%\.wslconfig with:

notepad "$env:USERPROFILE\.wslconfig"
```

Nội dung `.wslconfig`:

```ini
[wsl2]
memory=10GB
processors=6
swap=4GB
localhostForwarding=true
# nestedVirtualization=true    # Bật nếu cần Docker-in-Docker hoặc KVM

[experimental]
autoMemoryReclaim=gradual      # Tự thu hồi RAM không dùng (Win 11 22H2+)
sparseVhd=true                 # Tự shrink disk khi xóa file (Win 11 22H2+)
```

```powershell
# Áp dụng — restart tất cả WSL instances
wsl --shutdown
```

### Docker Desktop setup

Install **Docker Desktop** for Windows (https://www.docker.com/products/docker-desktop/). After install:

1. Settings → General → enable **Use the WSL 2 based engine**
2. Settings → Resources → **WSL Integration** → enable for your distro (Ubuntu hoặc `kite-dev`)
3. Apply & Restart

Verify in WSL terminal (`wsl` or `wsl -d kite-dev`):
```bash
docker --version          # Docker version 27.x+
docker compose version    # v2.x
docker ps                 # empty list (or running containers if you've used it before)
```

---

## Phase 1 — System packages in WSL Ubuntu (~10 min)

```bash
# Update base system
sudo apt update && sudo apt upgrade -y

# Toolchain + build tools
sudo apt install -y \
  build-essential \
  curl \
  wget \
  git \
  jq \
  unzip \
  ca-certificates \
  gnupg \
  openssh-client

# Java 17 (project requires Java 17 — see kitehub/pom.xml `<java.version>17</java.version>`)
sudo apt install -y openjdk-17-jdk
java --version    # openjdk version "17.0.x"

# Maven
sudo apt install -y maven
mvn --version     # Apache Maven 3.6.x or newer

# Python 3 (Claude Code hooks + scripts under scripts/*.py)
sudo apt install -y python3 python3-pip
python3 --version

# GitHub CLI — official repo
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
  | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update && sudo apt install -y gh
gh --version
```

### Node.js via fnm

`fnm` is preferred over apt because it lets the project pin Node versions cleanly.

```bash
curl -fsSL https://fnm.vercel.app/install | bash
# Reload shell
exec bash

# Install Node 22 (project default) and set as default
fnm install 22
fnm default 22
node --version    # v22.x
npm --version     # 10.x
```

Add `fnm` to `.bashrc` so node/npm are on PATH at every shell:
```bash
grep -q 'fnm env' ~/.bashrc || cat >> ~/.bashrc <<'EOF'

# fnm — Node version manager
eval "$(fnm env --use-on-cd)"
EOF
exec bash
```

### pnpm (KiteHub frontend)

```bash
npm install -g pnpm
pnpm --version    # 9.x or newer
```

### RTK — Rust Token Killer (token-saving CLI proxy)

The project's hooks rewrite shell commands through RTK for ~60-90% token savings on dev operations. Without it, hooks still work — they just no-op.

```bash
# Install Rust if missing
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
source "$HOME/.cargo/env"

# Install RTK (latest from crates.io)
cargo install rtk

# Verify — IMPORTANT: there's a name collision with reachingforthejack/rtk (Rust Type Kit).
# Project requires Stigliano/rtk (Rust Token Killer).
rtk --version       # should show rtk 0.37.x
rtk gain --help     # should NOT say "command not found"
which rtk           # should be ~/.cargo/bin/rtk
```

If `rtk gain` errors with "command not found" after `cargo install rtk` → you got the wrong package. Uninstall (`cargo uninstall rtk`) and follow project-specific install instructions in `~/.claude/RTK.md`.

---

## Phase 2 — Git + GitHub auth (~5 min)

```bash
# Identity
git config --global user.name "<your-name>"
git config --global user.email "<your-email>"
git config --global init.defaultBranch main
git config --global pull.ff only       # forbid merge commits on main (project rule)
git config --global core.autocrlf input # safety net — store LF on disk, accept CRLF on read

# SSH key (recommended over PAT)
ssh-keygen -t ed25519 -C "<your-email>" -f ~/.ssh/id_ed25519 -N ""
cat ~/.ssh/id_ed25519.pub
# → paste into github.com/settings/keys

# Verify SSH
ssh -T git@github.com      # "Hi <your-handle>! You've successfully authenticated..."

# Authenticate gh CLI (needed for PR workflow + GitHub MCP token)
gh auth login              # interactive — choose SSH protocol when prompted
gh auth status             # confirm scopes: repo, workflow, read:org
```

---

## Phase 3 — Clone + bootstrap project (~10 min)

```bash
mkdir -p ~/projects && cd ~/projects
git clone git@github.com:<owner>/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform

# Make all shell scripts executable (Git may not preserve +x across filesystems)
chmod +x scripts/*.sh kitehub/scripts/*.sh kiteclass/scripts/*.sh .claude/scripts/*.sh 2>/dev/null
```

### 3.1 Generate `.env` files (CRITICAL — `up.sh` will fail without them)

Five `.env.example` files exist; copy each to `.env` (the kitehub one is auto-generated by `setup.sh`):

```bash
# KiteHub stack — auto-generates secure random passwords
cd kitehub
./scripts/setup.sh
# → creates kitehub/.env with random POSTGRES_PASSWORD / RABBITMQ_PASSWORD /
#   MINIO_ROOT_PASSWORD / ENCRYPTION_MASTER_KEY / JWT_SECRET / INTERNAL_API_SECRET
cd ..

# KiteClass — manual copy (no setup.sh)
cp kiteclass/.env.example kiteclass/.env
cp kiteclass/kiteclass-gateway/.env.example kiteclass/kiteclass-gateway/.env

# Frontends
cp kitehub/kitehub-frontend/.env.example kitehub/kitehub-frontend/.env.local
cp kiteclass/kiteclass-frontend/.env.example kiteclass/kiteclass-frontend/.env.local
```

Edit each `.env` to replace any remaining `CHANGE_ME` values. For local dev:
- `OPENAI_API_KEY=sk-mock-key` is fine (mock mode — no real API calls)
- `HCAPTCHA_*` test keys (`10000000-ffff-...`) work without real hCaptcha account
- `CAPTCHA_ENABLED=false` for local

### 3.2 Pull Maven + npm dependencies

```bash
# Backend — kitehub modules
( cd kitehub && ./mvnw -q -pl kite-base,kitehub-platform -am install -DskipTests )
( cd kitehub && ./mvnw -q -pl kitehub-branding,kitehub-subscription,kitehub-email,kitehub-admin,kitehub-gateway -am test-compile )

# Backend — kiteclass modules
( cd kiteclass && ./mvnw -q -pl kiteclass-core -am test-compile )

# Frontend — KiteHub uses pnpm
( cd kitehub/kitehub-frontend && pnpm install --frozen-lockfile )

# Frontend — KiteClass uses npm
( cd kiteclass/kiteclass-frontend && npm ci )
```

> **Pinned versions to leave alone:** `kiteclass-core/pom.xml` pins `ognl 3.3.4` (Thymeleaf 3.1.x compat — see in-file comment). Dependabot has an `ignore` entry. Don't bump manually; if you see PRs trying to, close them.

### 3.3 Playwright browsers (for FE smoke tests)

```bash
( cd kiteclass/kiteclass-frontend && npx playwright install --with-deps chromium )
( cd kitehub/kitehub-frontend && npx playwright install chromium )
```

The `--with-deps` flag installs the system libraries Chromium needs (libnss3, libgbm1, etc.) on a clean Ubuntu.

---

## Phase 4 — Bring up the Docker stack (~5–10 min first time)

```bash
cd kitehub
./scripts/up.sh
# Follow the wait — Docker pulls images on first run, can take a few minutes
./scripts/wait-for-healthy.sh
./scripts/status.sh
# All containers should be running / healthy
```

Service ports cheat-sheet (host-side):

| Port | Service | Notes |
|------|---------|-------|
| 9000 | kite-gateway | Main API entry |
| 8080–8085 | kitehub-{subscription,branding,email,admin} + kiteclass-core | Per-service Spring Boot |
| 3000 | kiteclass-frontend | Tenant-facing |
| 3001 | kitehub-frontend | Platform admin |
| 5433 | kite-postgres | Both products share one Postgres |
| 6379 | kite-redis (kitehub) | |
| 6380 | kite-redis (kiteclass alt) | If running both stacks |
| 5673 | kite-rabbitmq | AMQP |
| 15672 | kite-rabbitmq | Management UI |
| 9001 | kite-minio | Console |
| 9002 | kite-minio | API |
| 8025 | kite-mailhog | Web UI for captured emails |
| 9090 | kite-prometheus | Metrics scrape UI |
| 3030 | kite-grafana | Dashboards (admin/admin default) |
| 11434 | kite-ollama | Only when started with `--profile ai-local` |

### 4.1 Optional: local Ollama for AI Branding

To run AI Branding fully offline (skip OpenAI mock):

```bash
AI_PROVIDER=ollama ./scripts/up.sh --profile ai-local
# First run pulls models — can take 10+ minutes (llama3.1:8b ~5 GB, llava:13b ~8 GB)
# Verify: docker logs kite-ollama-setup | tail
```

Set `AI_PROVIDER=ollama` in `kitehub/.env` to make services use it.

### 4.2 KiteClass core dev profile

If you start `kiteclass-core` from your IDE (not Docker), use the `dev` profile to bypass V46+ Flyway schema validation that expects an aligned DB:

```bash
cd kiteclass/kiteclass-core
../mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# OR via the helper script
( cd kiteclass && ./scripts/dev-start.sh )
```

The dev profile sets `spring.jpa.hibernate.ddl-auto=update` + `spring.flyway.enabled=false` — workaround tracked in `feedback_dev_profile_schema_workaround.md`. Production uses `validate` + Flyway.

---

## Phase 5 — Claude Code + MCP (~10 min)

```bash
# Install Claude Code globally
npm install -g @anthropic-ai/claude-code
claude --version

# Launch in project (this auto-creates ~/.claude/projects/<slug>/ on first run)
cd ~/projects/2026-Kite-Class-Platform
claude --help > /dev/null   # registers project
```

### 5.1 GitHub MCP server (recommended)

Per `.claude/rules/mcp-first-with-fallback.md` §4.1 — official Docker-based server, reuses your `gh` CLI token:

```bash
docker pull ghcr.io/github/github-mcp-server

GITHUB_MCP_TOKEN=$(gh auth token)
claude mcp add github -s user \
  --env "GITHUB_PERSONAL_ACCESS_TOKEN=$GITHUB_MCP_TOKEN" \
  -- docker run -i --rm -e GITHUB_PERSONAL_ACCESS_TOKEN ghcr.io/github/github-mcp-server

claude mcp list   # github should show "✓ Connected"
```

### 5.2 Postgres MCP server (when you need DB introspection)

Skip this until the dev stack is up + you're doing audit work that benefits from schema queries. Recipe is pre-approved (memory `reference_postgres_mcp_setup.md`):

```bash
set -a; source kitehub/.env; set +a
claude mcp add postgres -s project \
  -- npx -y @modelcontextprotocol/server-postgres \
  "postgresql://${POSTGRES_USER:-kitehub}:${POSTGRES_PASSWORD}@localhost:5433/${POSTGRES_DB:-kitehub}"

claude mcp list
```

**Never point this at production.** Local dev only.

### 5.3 Hooks sanity check

`.claude/settings.local.json` ships with a few hooks (audit-gate, RTK rewrites). After clone they reference the project via `${CLAUDE_PROJECT_DIR}` env var, so no path edit is needed. Verify:

```bash
echo '{"tool_input":{"command":"echo hello"},"tool_output":""}' \
  | python3 .claude/hooks/audit-gate.py
# Expected: JSON output (decision/systemMessage). Exit 0.
```

If hooks fail with `python: command not found`, your distro doesn't symlink `python3` → `python`. The project hooks invoke `python3` explicitly, so this should not happen on Ubuntu 24.04.

---

## Phase 6 — Smoke test before you start work (~5 min)

Each step must pass:

```bash
# 1. Repo health
./scripts/repo-status.sh                  # GREEN expected

# 2. Skill convention check (CI gate)
bash scripts/check-skill-conventions.sh   # 0 FAIL expected

# 3. Build one backend module
( cd kitehub && ./mvnw -q -pl kitehub-branding test )
# → 166 tests pass

# 4. Build one frontend
( cd kiteclass/kiteclass-frontend && npm run build )

# 5. Stack health (if Docker up)
docker exec kite-postgres pg_isready -U "${POSTGRES_USER:-kitehub}"
docker exec kite-redis redis-cli ping     # PONG
curl -sSf http://localhost:9000/actuator/health | jq .status  # "UP"

# 6. Hit Mailhog UI
xdg-open http://localhost:8025 || echo "Open http://localhost:8025 in browser"
```

---

## Phase 7 — Optional but useful (~15 min)

### 7.1 VS Code with Remote-WSL

```bash
# In WSL terminal, from project root:
code .
# First launch downloads the WSL Server. Recommended extensions auto-prompt:
#   - Java Extension Pack
#   - ESLint
#   - Prettier
#   - Tailwind CSS IntelliSense
#   - GitLens
```

### 7.2 IntelliJ IDEA (alternative)

JetBrains Gateway is the cleanest path — it runs the IDE backend in WSL, frontend in Windows. Open the project via `~/projects/2026-Kite-Class-Platform`. Avoid `\\wsl$\...` UNC paths — they work but are slow.

### 7.3 Grafana dashboards

`http://localhost:3030` → admin / admin → import dashboards from `infrastructure/helm/kitehub/dashboards/` if you want pre-wired SLO views.

### 7.4 CI history + cleanup

The project keeps CI history at ≤50 runs (per `CLAUDE.md` "CI History Hygiene"). Run periodically:

```bash
gh workflow run ci-cleanup.yml --field dry_run=true   # preview
```

---

## Known gotchas

| Symptom | Cause | Fix |
|---------|-------|-----|
| `up.sh` fails: "POSTGRES_PASSWORD: variable not set" | Missing `kitehub/.env` | Run `cd kitehub && ./scripts/setup.sh` |
| `kiteclass-core` Flyway error on startup | V46+ schema mismatch in fresh DB | Use `dev` profile (Phase 4.2) or run with Flyway disabled |
| `mvn ... NoSuchMethodError: OgnlContext.<init>(Map)` | Dependabot or manual bump moved `ognl` past 3.3.4 | Pin back to 3.3.4 in `kiteclass-core/pom.xml` — see in-file comment |
| Test fails with `UnknownHostException: api.partner.com` on WSL2 | Placeholder hostname resolves to loopback under WSL2 NAT | Use RFC-2606 reserved names: `.invalid`, `.test`, or IP literals. Memory: `feedback_test_hostnames_rfc2606.md` |
| Playwright crashes on `chromium` launch | Missing system libs | `npx playwright install --with-deps chromium` (the `--with-deps` is the fix) |
| Hooks not firing in Claude Code | Old absolute path from a previous Windows install in `.claude/settings.local.json` | The repo-shipped settings use `${CLAUDE_PROJECT_DIR}` — don't override |
| Slow Docker volume mount, hot reload broken | Code is on `/mnt/c/...` (NTFS) | Move to `~/projects/...` (ext4). Never put project code in `/mnt/c/`. |
| `git status` shows every file as modified after clone | `core.autocrlf` mis-configured | `git config --global core.autocrlf input` then `git rm --cached -r . && git reset --hard` |
| `gh pr merge` succeeds despite failing CI | Solo-dev mode — branch protection not enforcing required checks | Use `scripts/check-ci.sh <branch>` to gate manually before merge |
| Worktree branches stuck after parallel agents | Harness locks worktrees | Wait — they auto-clean when the parent agent exits. Manual: `git worktree remove .claude/worktrees/<name> -f -f` |
| RTK `gain` says "command not found" | Wrong `rtk` package — name collision with Rust Type Kit | Reinstall via the Rust Token Killer source, verify `which rtk` shows project's binary |
| Custom WSL instance login as root | `wsl --import` defaults to root user | Add `/etc/wsl.conf` with `[user] default=<username>` then `wsl --terminate <name>` |
| Custom WSL disk grows but never shrinks | VHDX doesn't auto-compact by default | Add `sparseVhd=true` under `[experimental]` in `.wslconfig` (Win 11 22H2+) |
| `appendWindowsPath=false` breaks `code .` | VS Code CLI needs Windows PATH | Run `export PATH="$PATH:/mnt/c/Users/<you>/AppData/Local/Programs/Microsoft VS Code/bin"` hoặc bỏ `appendWindowsPath` setting |

---

## Reference files (read these once)

- `CLAUDE.md` — project rules + commit / wave / docs conventions
- `.claude/rules/skill-conventions.md` — how skills work + when CI gate fires
- `.claude/rules/mcp-first-with-fallback.md` — when to prefer MCP vs CLI
- `documents/05-guides/local-dev/local-dev-mock-data.md` — seeded data for offline dev
- `documents/05-guides/infrastructure/dependabot-guide.md` — pin/ignore rules so weekly bumps don't break you
- `documents/05-guides/infrastructure/SECRET-MANAGEMENT.md` — how prod secrets work (you don't need this for local)
- `kitehub/scripts/help.sh` — list of all kitehub helper commands

---

## Log

- **2026-04-28 (update):** Added Phase 0 Option B — custom WSL instance creation (`wsl --import`): rootfs download, custom name + install directory, user creation + `/etc/wsl.conf`, multi-instance management (list/stop/backup/clone/move), disk migration. Added `.wslconfig` experimental settings (`autoMemoryReclaim`, `sparseVhd`). 3 new gotchas (root default, VHDX growth, `appendWindowsPath`).
- **2026-04-28:** Doc created. Pulled together as a clean-room reproducer separate from `wsl-migration-playbook.md` (which assumes existing Windows D:\\... installation). Adds: setup.sh step before up.sh, RTK install, Java 17 explicit, all 5 .env templates, Playwright `--with-deps`, Postgres MCP recipe, dev-profile note for kiteclass-core, ognl 3.3.4 pin, RFC-2606 test-hostname gotcha, current container count (~20), CI-without-branch-protection gotcha, worktree cleanup gotcha. Cross-link added to `wsl-migration-playbook.md`.
