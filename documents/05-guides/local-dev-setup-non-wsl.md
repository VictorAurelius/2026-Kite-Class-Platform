# Local Dev Setup — Non-WSL (Mac / Linux Native)

> Last updated: 2026-04-18 | Owner: DevEx | Related: [`wsl-migration-playbook.md`](wsl-migration-playbook.md)

Setup guide cho developers trên **macOS** hoặc **Linux native** (Ubuntu/Debian/Fedora). WSL users dùng [`wsl-migration-playbook.md`](wsl-migration-playbook.md) thay vì guide này.

---

## 1. Prerequisites

### 1.1 System requirements
- **OS:** macOS 12+ hoặc Linux kernel 5.15+
- **RAM:** 16 GB tối thiểu (Docker + IDE + services = ~10 GB idle)
- **Disk:** 50 GB free (Docker images + node_modules + Maven .m2 = ~20 GB)
- **Network:** stable for Maven/npm downloads (first build ~30 min)

### 1.2 Required tools

| Tool | Version | Purpose |
|------|---------|---------|
| Docker Desktop / Docker Engine | 24.0+ | Container runtime |
| Docker Compose | v2.20+ | Multi-service orchestration |
| Java | 17 (LTS) | Spring Boot backends |
| Maven | 3.9+ | Java build (hoặc dùng `./mvnw`) |
| Node.js | 20 LTS | Frontend build |
| pnpm | 9+ | Frontend package manager (không dùng npm/yarn) |
| Git | 2.40+ | Version control |
| gh CLI | 2.40+ | GitHub operations |

### 1.3 Optional
- **fnm** / **nvm** — Node version manager (nếu cần switch Node versions)
- **SDKMAN** — Java version manager (khuyến nghị cho macOS)
- **httpie** hoặc **curl** — API testing
- **jq** — JSON parsing

---

## 2. macOS Setup (Homebrew)

```bash
# 1. Install Homebrew (nếu chưa có)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Install tools
brew install --cask docker
brew install git gh jq httpie
brew install openjdk@17 maven
brew install node@20 pnpm

# 3. Link Java 17 (Apple Silicon + Intel)
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc

# 4. Start Docker Desktop (GUI) — ensure 8 GB RAM allocated trong Preferences → Resources

# 5. Verify
java -version     # openjdk 17
mvn -version      # 3.9+
node -v           # v20
pnpm -v           # 9+
docker -v         # 24+
docker compose version  # v2+
```

---

## 3. Linux Setup (Ubuntu/Debian)

```bash
# 1. System update
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl git build-essential jq

# 2. Docker Engine
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # logout/login để apply
sudo systemctl enable --now docker

# 3. Java 17 (Temurin)
sudo apt install -y openjdk-17-jdk maven
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc

# 4. Node 20 + pnpm via fnm
curl -fsSL https://fnm.vercel.app/install | bash
source ~/.bashrc
fnm install 20 && fnm use 20
npm install -g pnpm@9

# 5. GitHub CLI
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | \
  sudo gpg --dearmor -o /usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] \
  https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list
sudo apt update && sudo apt install -y gh

# 6. Verify
docker run --rm hello-world
java -version
mvn -version
node -v && pnpm -v
```

Fedora/RHEL: replace `apt` với `dnf`, tools identical.

---

## 4. Clone & First Build

```bash
# Clone
git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git ~/projects/kite-platform
cd ~/projects/kite-platform

# GitHub CLI authentication
gh auth login

# First-time Docker stack bring-up (downloads ~4 GB images)
cd kitehub
./scripts/build-all.sh        # ~25 min lần đầu
./scripts/up.sh               # Start all services

# Verify stack health
./scripts/status.sh
```

Expected state:
- `kite-postgres` running on port `5433`
- `kite-redis` on `6379`
- `kite-rabbitmq` on `5672` (management UI: `15672`)
- `kite-minio` on `9000`
- `kitehub-gateway` on `9000`
- `kitehub-subscription`, `kitehub-branding`, `kitehub-email`, `kitehub-admin` on 8081-8085

---

## 5. Frontend Dev Servers

```bash
# KiteHub frontend (port 3000 theo config cũ, có thể thay đổi)
cd kitehub/kitehub-frontend
pnpm install
pnpm dev

# KiteClass frontend (port 5174)
cd kiteclass/kiteclass-frontend
pnpm install
pnpm dev
```

⚠️ **Gotcha (macOS):** nếu port conflict, kill process: `lsof -ti:3000 | xargs kill -9`.

---

## 6. Common Issues

### 6.1 Docker daemon not running (macOS)
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```
→ Mở Docker Desktop app, đợi whale icon trên menu bar.

### 6.2 Permission denied writing to /var/run/docker.sock (Linux)
```
ERROR: Got permission denied while trying to connect to the Docker daemon
```
→ `sudo usermod -aG docker $USER`, logout/login.

### 6.3 Port 5433 already in use
Other PostgreSQL instance (Homebrew postgres, system apt package) chiếm port.
```bash
# macOS
brew services stop postgresql@14
# Linux
sudo systemctl stop postgresql
```

### 6.4 Java version mismatch
```
Unsupported class file major version 61
```
→ Building với Java 17 nhưng JDK path trỏ Java 21. Check `JAVA_HOME` pointing đúng Java 17.

### 6.5 pnpm fetch timeout
Vietnam network đôi khi slow pulling npm packages. Dùng mirror:
```bash
pnpm config set registry https://registry.npmmirror.com
```

### 6.6 Frontend won't connect to backend (CORS)
Check gateway config — mặc định allow origin `localhost:3000`, `localhost:5174`. Nếu FE chạy port khác, update `kitehub-gateway/src/main/resources/application.yml`.

---

## 7. Daily Workflow

```bash
# Morning
cd kitehub && ./scripts/up.sh

# Hack
# ... code ...

# Test backend changes
cd kiteclass/kiteclass-core && ./mvnw test -pl :core-service -Dtest=MyTest

# Test frontend
cd kiteclass/kiteclass-frontend && pnpm test

# Evening
cd kitehub && ./scripts/down.sh    # Free up RAM overnight
```

---

## 8. Related Guides

- [`wsl-migration-playbook.md`](wsl-migration-playbook.md) — Windows developers using WSL2
- [`SECRET-MANAGEMENT.md`](SECRET-MANAGEMENT.md) — Secrets setup
- [`incident-response-runbook.md`](incident-response-runbook.md) — If prod-like bug hits local
- [`../02-architecture/docker-platform-architecture.md`](../02-architecture/docker-platform-architecture.md) — Service topology

---

## 9. Log

- **2026-04-18:** Created (GAP-102 Part 1 P2 batch).
