# WSL Migration Playbook

**Purpose:** Move this project from Windows `D:\person\2026-Kite-Class-Platform\` to WSL `~/projects/2026-Kite-Class-Platform/` safely.

**Target audience:** Solo dev (primary) + team members with similar Windows setup.

**Timing:** Execute **between Wave 2 and Wave 3** (Parent Portal shipped, AI queue not started). Do NOT migrate mid-wave.

**Total time:** 2–4 hours including verification.

---

## Why migrate

See `documents/03-planning/wave-roadmap-p0.md` rationale. Short version:
- Docker I/O 2-3x faster (NTFS→ext4 mount overhead eliminated)
- Native inotify file watcher (Spring DevTools, Next.js Fast Refresh)
- Bash scripts run native (no Git Bash / MSYS_NO_PATHCONV workarounds)
- CI parity (ubuntu-latest)
- MCP servers work better (see Phase 5)

---

## Pre-Migration Checklist

**⚠️ STOP if any item is not checked.**

### Repo state
- [ ] Current branch = `main`
- [ ] `git status --short` shows clean working tree
- [ ] `git log --oneline origin/main..HEAD` shows nothing (nothing unpushed)
- [ ] No in-flight PRs that you have uncommitted changes for
- [ ] `./scripts/repo-status.sh` shows GREEN
- [ ] No active worktrees: `git worktree list` shows only main

### Tool versions to document (before migrate)
Save a snapshot so you can match versions in WSL:

```bash
# On Windows, save to C:\tmp\windows-tool-versions.txt
node --version         # e.g., v22.8.0
npm --version          # e.g., 10.8.2
java --version         # e.g., 17.0.x
mvn --version          # e.g., 3.9.x
docker --version       # e.g., 27.x
docker compose version # e.g., v2.x
python --version       # e.g., 3.11.x
gh --version           # e.g., 2.x
```

### Credentials to migrate
- [ ] GitHub SSH key (~/.ssh/id_ed25519) or personal access token
- [ ] `gh auth status` — note which account + scopes
- [ ] Docker Hub login (if used)
- [ ] Any `.env` files with secrets (NOT in git)
- [ ] Claude Code memory files location

---

## Phase 1: WSL Environment Setup (30 min)

### 1.1 Verify WSL2 + distro

```bash
# In PowerShell (as admin)
wsl --status
wsl --list --verbose
```

Expected: Ubuntu (or Debian) on version 2. If not:
```bash
wsl --install -d Ubuntu
```

### 1.2 Enable Docker Desktop WSL Integration

**CRITICAL:** This step determines migration speed benefits.

1. Open Docker Desktop
2. Settings → Resources → **WSL Integration**
3. Enable "Enable integration with my default WSL distro"
4. Toggle ON for your Ubuntu distro
5. Click **Apply & Restart**

Verify in WSL terminal:
```bash
docker --version          # should work
docker compose version    # should work
docker ps                 # should show empty list or running containers
```

### 1.3 Install runtimes in WSL Ubuntu

```bash
# Update
sudo apt update && sudo apt upgrade -y

# Node.js via fnm (matches Windows version better than apt)
curl -fsSL https://fnm.vercel.app/install | bash
exec bash
fnm install 22  # or match your Windows version
fnm default 22

# Java 17 (for Maven builds)
sudo apt install -y openjdk-17-jdk
java --version

# Maven
sudo apt install -y maven
mvn --version

# Python 3 (for hooks)
sudo apt install -y python3 python3-pip
python3 --version

# Git + gh CLI
sudo apt install -y git
# gh CLI per official instructions
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update
sudo apt install -y gh

# pnpm (used by KiteHub frontend)
npm install -g pnpm

# Misc tools
sudo apt install -y curl jq unzip build-essential
```

### 1.4 Configure Git identity

```bash
git config --global user.name "VictorAurelius"
git config --global user.email "vannkite@outlook.com"
git config --global init.defaultBranch main
git config --global pull.ff only

# Optional: sign commits with SSH key
git config --global commit.gpgsign false   # unless you use GPG
```

### 1.5 Authenticate GitHub

```bash
# Option A: SSH key (recommended)
ssh-keygen -t ed25519 -C "vannkite@outlook.com"
cat ~/.ssh/id_ed25519.pub  # copy this to github.com/settings/keys

# Option B: gh CLI (also installs git credential helper)
gh auth login
# Follow prompts, choose SSH or HTTPS
```

Verify:
```bash
ssh -T git@github.com   # should say "Hi VictorAurelius!"
gh auth status          # should show logged in
```

---

## Phase 2: Project Migration (15 min)

### 2.1 Fresh clone (NOT copy)

**Why fresh clone:** Avoids Windows line ending artifacts, ensures `.gitattributes` from #333 takes effect cleanly.

```bash
mkdir -p ~/projects
cd ~/projects
git clone git@github.com:VictorAurelius/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform
```

### 2.2 Verify line endings are correct

```bash
# Check .sh files are LF (not CRLF)
file scripts/up.sh scripts/smoke-test.sh scripts/pr-compliance-check.sh
# Expected: "ASCII text executable" — NOT "with CRLF line terminators"

# Spot check
cat -A scripts/up.sh | head -5
# Expected: no "^M" characters at end of lines
```

If you see `^M` or `CRLF`: `.gitattributes` from PR #333 should have prevented this. Report as bug.

### 2.3 Make scripts executable

```bash
# Git may not preserve executable bit across filesystems
chmod +x scripts/*.sh
chmod +x kitehub/scripts/*.sh 2>/dev/null
chmod +x kiteclass/scripts/*.sh 2>/dev/null
chmod +x .claude/scripts/*.sh 2>/dev/null

# Verify
ls -l scripts/*.sh | head -5
# Expected: -rwxr-xr-x permissions
```

### 2.4 Install dependencies

```bash
# Backend (Maven) — test compile each service
cd kitehub/kitehub-subscription && mvn test-compile -q && cd -
cd kiteclass/kiteclass-core && mvn test-compile -q && cd -

# Frontend (KiteHub - pnpm)
cd kitehub/kitehub-frontend && pnpm install && cd -

# Frontend (KiteClass - npm)
cd kiteclass/kiteclass-frontend && npm install && cd -
```

### 2.5 Install Playwright browsers

```bash
cd kiteclass/kiteclass-frontend && npx playwright install chromium && cd -
cd kitehub/kitehub-frontend && npx playwright install chromium && cd -
```

---

## Phase 3: Claude Code Setup (30 min)

### 3.1 Install Claude Code in WSL

```bash
npm install -g @anthropic-ai/claude-code
claude --version
```

### 3.2 Migrate Claude Code memory + conversation history

**Location mapping:**
- Old (Windows): `C:\Users\NguyenVanKiet\.claude\projects\D--person-2026-Kite-Class-Platform\`
- New (WSL): `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/`

**What to copy:**
- `memory/` directory — learned feedback, preferences, project context
- `*.jsonl` files — full conversation history (each session = 1 UUID.jsonl)
- `*/` session state dirs (same UUID as .jsonl) — tool call state, plans
- `plans/` from user home — plan files referenced from conversations

```bash
# Find new project slug (WSL auto-creates on first `claude` in the dir)
cd ~/projects/2026-Kite-Class-Platform
claude --help > /dev/null  # trigger project registration
ls ~/.claude/projects/ | grep 2026-Kite-Class
# Note the directory name, e.g., "-home-nguyenvankiet-projects-2026-Kite-Class-Platform"

NEW_SLUG="-home-nguyenvankiet-projects-2026-Kite-Class-Platform"
OLD_DIR="/mnt/c/Users/NguyenVanKiet/.claude/projects/D--person-2026-Kite-Class-Platform"
NEW_DIR="$HOME/.claude/projects/$NEW_SLUG"

# A. Memory files (learned preferences — no path issues)
cp -r "$OLD_DIR/memory" "$NEW_DIR/"

# B. Conversation JSONL files (history)
cp "$OLD_DIR"/*.jsonl "$NEW_DIR/" 2>/dev/null

# C. Session state directories (match each .jsonl by UUID)
for session_dir in "$OLD_DIR"/*/; do
  [[ "$(basename "$session_dir")" == "memory" ]] && continue
  cp -r "$session_dir" "$NEW_DIR/"
done

# D. Plan files (live in user home, outside project dir)
mkdir -p ~/.claude/plans
cp /mnt/c/Users/NguyenVanKiet/.claude/plans/*.md ~/.claude/plans/ 2>/dev/null

# Verify
ls "$NEW_DIR/memory/" | head -5    # MEMORY.md + feedback_*.md
ls "$NEW_DIR"/*.jsonl | wc -l       # session count (should be ≥1)
ls ~/.claude/plans/ | head -5       # plan .md files
```

**⚠️ Caveats for conversation history:**

| What works | What doesn't |
|-----------|--------------|
| `claude --continue` shows old sessions | Re-running a tool call from old history that references `D:\...` — old absolute path doesn't exist in WSL |
| `claude --resume <session-id>` loads full context | Tools attempting to `Read` a path logged in the old session → fail gracefully, new tool call uses WSL path |
| Memory preserved | Session file mtimes reset on copy (cosmetic only) |
| Plan files referenced in history still accessible | Plans with hardcoded `C:\Users\...` paths need re-pointing |

**Session ID collision:** Claude Code uses UUID per session. Copying preserves UUIDs — safe. No collision with new WSL sessions (those get new UUIDs on creation).

**File size note:** Long-running sessions can be 50-100+ MB per `.jsonl`. Copy is fast but first load after copy may take 5-10s while Claude Code indexes.

### 3.3 Fix `.claude/settings.local.json`

**CRITICAL:** Hook command has hardcoded Windows path.

```bash
# Open file
code .claude/settings.local.json
# Or: nano .claude/settings.local.json
```

Find line (~28):
```json
"command": "python D:/person/2026-Kite-Class-Platform/.claude/hooks/audit-gate.py"
```

Change to:
```json
"command": "python3 /home/nguyenvankiet/projects/2026-Kite-Class-Platform/.claude/hooks/audit-gate.py"
```

**Or better** — use relative path via env var:
```json
"command": "python3 ${CLAUDE_PROJECT_DIR}/.claude/hooks/audit-gate.py"
```

(Check Claude Code docs for exact env var name.)

### 3.4 Verify hooks fire

```bash
# Test audit-gate hook manually
echo '{"tool_input":{"command":"gh pr merge 999 --squash"},"tool_output":""}' | python3 .claude/hooks/audit-gate.py
# Expected: JSON output with decision or systemMessage
```

### 3.5 Launch Claude Code in project

```bash
cd ~/projects/2026-Kite-Class-Platform
claude
# Inside Claude Code, test:
# - Ask "ls" → verify Bash tool works
# - Ask to read CLAUDE.md → verify Read tool works
# - Check memory is loaded (mention something saved in memory)
```

---

## Phase 4: Verification (30 min)

Run through this end-to-end smoke test. Each step must pass before proceeding.

### 4.1 Git operations

```bash
git pull --ff-only               # should work
git log --oneline -5             # should show recent commits
git status                       # clean
git branch -a                    # shows remote branches
```

### 4.2 Build both services

```bash
# KiteHub subscription
cd kitehub/kitehub-subscription
mvn clean test -q                # should pass
cd -

# KiteClass core (may skip if Docker not up)
cd kiteclass/kiteclass-core
mvn test-compile -q              # compile at least
cd -
```

### 4.3 Docker stack

```bash
cd kitehub
./scripts/up.sh
# Wait 1-2 min for all 14 containers

./scripts/status.sh
# All should be "healthy" or "running"

./scripts/down.sh  # cleanup
```

### 4.4 Frontend build

```bash
cd kitehub/kitehub-frontend
pnpm build                       # should complete
cd -

cd kiteclass/kiteclass-frontend
npm run build                    # should complete
cd -
```

### 4.5 Scripts

```bash
./scripts/pr-compliance-check.sh 332    # should show PR #332 compliance
./scripts/repo-status.sh                # should show GREEN
./scripts/smoke-test.sh http://localhost:9000   # if stack up
```

### 4.6 Test PR workflow end-to-end

**Dummy PR test** to verify full workflow before actual wave work:

```bash
git checkout -b test/wsl-migration-verification
echo "# WSL migration verified $(date)" >> documents/05-guides/wsl-migration-playbook.md
git add documents/05-guides/wsl-migration-playbook.md
git commit -m "test(wsl): verify migration workflow end-to-end"
git push -u origin test/wsl-migration-verification
gh pr create --title "test: WSL migration verification" --body "Verification PR — merge and delete if all checks pass."
# Wait for CI
until gh run list --branch test/wsl-migration-verification --limit 1 --json conclusion --jq '.[0].conclusion' | grep -qE 'success|failure'; do sleep 30; done
gh run list --branch test/wsl-migration-verification --limit 3

# If green:
gh pr close $(gh pr view --json number --jq .number) --delete-branch --comment "WSL migration verified"
git checkout main
git branch -D test/wsl-migration-verification
```

**Expected:** CI green. Audit-gate.py hook fires on merge (check PR-*.json auto-staged).

---

## Phase 5: MCP Server Installation (30 min, optional)

**Do this AFTER Phase 4 passes.**

### 5.1 GitHub MCP (user scope) — use OFFICIAL Docker image

> **⚠️ Heads-up (2026-04-18):** The npm package
> `@modelcontextprotocol/server-github` is **DEPRECATED**. GitHub now
> publishes the official server at `ghcr.io/github/github-mcp-server`.

```bash
# Pull official image
docker pull ghcr.io/github/github-mcp-server

# Uses your gh CLI auth token — no separate PAT needed
GITHUB_MCP_TOKEN=$(gh auth token)

claude mcp add github -s user \
  --env "GITHUB_PERSONAL_ACCESS_TOKEN=$GITHUB_MCP_TOKEN" \
  -- docker run -i --rm -e GITHUB_PERSONAL_ACCESS_TOKEN ghcr.io/github/github-mcp-server

# Verify
claude mcp list
# Should show: github (user scope) — ✓ Connected
```

**Token scopes needed:** `repo`, `workflow`, `read:org` (default `gh auth login` covers these).

### 5.2 PostgreSQL MCP (project scope, dev DB only)

**Defer until Docker stack is up + you're starting a wave that needs DB
introspection (Wave 3 AI queue, Wave 8 doc gen).**

```bash
# Prerequisites: kitehub stack running
cd ~/projects/2026-Kite-Class-Platform/kitehub && ./scripts/up.sh

# POSTGRES_PASSWORD comes from your local .env (set via ./scripts/setup.sh)
source kitehub/.env 2>/dev/null || export POSTGRES_PASSWORD=<your-dev-password>

claude mcp add postgres -s project \
  -- npx -y @modelcontextprotocol/server-postgres \
  "postgresql://kitehub:$POSTGRES_PASSWORD@localhost:5433/kitehub"

# Verify
claude mcp list
```

**⚠️ Never point this at production.** Dev DB only, readonly ideal.

### 5.3 (Optional) Playwright MCP

Defer until Wave 4 (branding propagation) when visual regression matters.

### 5.4 Test MCP server

```bash
cd ~/projects/2026-Kite-Class-Platform
claude
# Inside: ask "list my open PRs" — should use GitHub MCP
# Inside: ask "show schema of backup_records table" — should use PostgreSQL MCP
```

---

## Phase 6: IDE Setup (15 min)

### 6.1 VS Code with WSL extension

```bash
# In WSL terminal, from project root
cd ~/projects/2026-Kite-Class-Platform
code .
# First time: VS Code will install WSL server in distro
```

Extensions to install (in WSL context, not local):
- Java Extension Pack
- Extension Pack for Java
- ESLint
- Prettier
- Tailwind CSS IntelliSense
- GitLens

### 6.2 IntelliJ IDEA (if used)

IntelliJ has WSL support but works best with code on Windows filesystem. If you want IntelliJ:
- Option A: Use "Remote Development" feature (IntelliJ Gateway) — runs JetBrains server in WSL
- Option B: Keep code in WSL, open via `\\wsl$\Ubuntu\home\...` (slower but works)

VS Code is simpler for WSL.

---

## Rollback Plan

If anything breaks badly and you need to go back to Windows:

1. Windows copy still intact at `D:\person\2026-Kite-Class-Platform\`
2. Just `cd` there and continue working
3. Any commits made in WSL are already on GitHub — pull them:
   ```
   cd /d/person/2026-Kite-Class-Platform
   git fetch origin
   git reset --hard origin/main
   ```
4. Don't delete the Windows copy until WSL has been used for ≥1 full wave without issues

---

## Archive Windows Copy

**After 2 weeks of stable WSL usage**, archive the Windows copy:

```powershell
# In PowerShell
# Set readonly so accidental edits fail
attrib +R "D:\person\2026-Kite-Class-Platform\*.*" /S
# Or move to archive location
Move-Item "D:\person\2026-Kite-Class-Platform" "D:\archive\2026-Kite-Class-Platform-windows-pre-wsl"
```

---

## Known Gotchas

### Filesystem performance (critical)

**Good:** `~/projects/` (Linux ext4) — fast
**Bad:** `/mnt/c/...` or `/mnt/d/...` (Windows NTFS mounted) — slow

Never put code in `/mnt/c/` with intent to use WSL performance. Always use native Linux filesystem.

### Memory file format

Claude Code memory stores in text files. Copy preserves content. But:
- File modification times reset on copy → older memories may appear "new"
- No migration tool — manual `cp -r` is the way

### Docker volume mount paths

If any `docker-compose.yml` has absolute paths like `D:\...`, update to relative or `~/...`.

Grep first:
```bash
grep -rn "D:/" docker-compose*.yml 2>/dev/null
grep -rn "D:\\\\" docker-compose*.yml 2>/dev/null
```

### Port conflicts

Windows apps can bind ports on localhost even when WSL tries to use them:
- Check: `netstat -ano | findstr :4700` (in Windows PowerShell)
- Common conflict: some Windows service on port 3000 (KiteClass FE default)

### Hook env vars

If `audit-gate.py` used env vars set via Windows PowerShell, re-export in WSL `~/.bashrc`:
```bash
export GITHUB_TOKEN=ghp_xxx
export AWS_ACCESS_KEY_ID=...
# etc.
```

### Line endings (should be fixed by .gitattributes)

If you see CRLF in `.sh` files after clone:
```bash
# Force reset line endings
git rm --cached -r .
git reset --hard
```

---

## Post-Migration Checklist

**Within first week of WSL usage:**

- [ ] CI green on ≥3 PRs from WSL
- [ ] Docker stack starts successfully via `./scripts/up.sh`
- [ ] All Maven tests pass locally
- [ ] Frontend builds complete locally
- [ ] Hooks fire correctly (check PR-*.json auto-staged)
- [ ] Agent workflow (spawn subagents, cherry-pick) works
- [ ] MCP servers responding (if installed)
- [ ] No CRLF warnings on any commit

**After 2 weeks:**

- [ ] Archive Windows copy
- [ ] Document any additional gotchas found
- [ ] Update this playbook with lessons learned

---

## Comparison: Windows vs WSL Setup

| Metric | Windows (current) | WSL (target) |
|--------|:------:|:---:|
| Project location | `D:\person\...` (NTFS) | `~/projects/...` (ext4) |
| Shell | Git Bash | Bash (native) |
| Python | `python` (Windows exe) | `python3` (Linux) |
| Docker I/O | Slow (NTFS mount) | Fast (native) |
| inotify | Buggy | Works |
| Line endings | CRLF risk (mitigated via #333) | LF native |
| Path separators | Mixed (`\` vs `/`) | `/` only |
| `.sh` script | Needs Git Bash | Native |
| Hook Python cmd | `python D:/...` (abs) | `python3 ~/...` (abs) |
| VS Code | Direct open | Via WSL extension |

---

## Log

- **2026-04-17:** Playbook created pre-migration. Scheduled for execution between Wave 2 and Wave 3.
- **YYYY-MM-DD:** (update after actual migration)
