# GAP-358: Migrate WSL2 dev workstation to Oracle Cloud Always Free server

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (DX improvement — local WSL2 throttle/SSH stop incidents)
**Domain:** DevOps / Developer Experience / Infrastructure
**Found:** 2026-05-05 (after WSL2 stop incident + PATH-leak debugging session)
**Affects:** Solo-dev workflow — daily development, agent runs, mvn/pnpm builds, Docker stack
**Wave eligibility:** ❌ NOT during Wave 19; ✅ after Wave 19 closure

## Problem

Current setup: Windows + WSL2 Ubuntu 7.6 GB RAM + ~7.5 GB persistent storage. Recurring pain points surfaced 2026-05-04..05:

- **WSL2 sporadic stops** — observed 2026-05-05 PC restart killed 3 background agents mid-execution (50 uncommitted files orphaned in worktrees per `feedback_agent_kill_root_cause.md` SIGHUP cascade pattern)
- **PATH leak** from Windows → WSL via `appendWindowsPath` default true, breaks unquoted shell evals (fixed inline 2026-05-05 via `/etc/wsl.conf` interop=false + `.bashrc` filter; root cause was actually a broken `eval \"$(fnm env)\"` line)
- **Stack RAM ceiling** — full Kite stack (6 KiteHub services + KiteClass + 2 Next.js + Postgres/Redis/RabbitMQ/MinIO) saturates 7.6 GB → swapping when running `mvn verify` + dev servers + Claude agents simultaneously
- **No remote access** — solo-dev tied to single workstation; mobile/iPad access via SSH to local WSL requires public IP exposure or Tailscale relay back through dev machine
- **Agent reliability** — long-running background agents die with workstation reboot; no checkpoint/resume

## Current State (verified 2026-05-05)

| Artifact | State | Evidence |
|---|---|---|
| `infrastructure/terraform-oracle/` | ✅ exists, **production-targeted** | `compute.tf` 2-VM split (backend 12GB + frontend+AI 12GB = full Always Free) |
| Terraform applied? | ❓ unknown without OCI console | `.terraform.lock.hcl` exists → `terraform init` ran; no local state file (S3 backend per `main.tf`) |
| Oracle deployment doc | ✅ `documents/03-planning/infrastructure/kitehub-oracle-cloud-deployment.md` (313 lines, 2026-03-19) | Production dual-cloud strategy (Oracle primary, AWS backup) |
| Tailscale + mosh setup memory | ✅ `feedback_agent_kill_root_cause.md` references "Tailscale + mosh + tmux 3-layer stack" | for SSH stability against SIGHUP |
| WSL footprint | ~7.5 GB persistent + 8-12 GB peak RAM | `du -sh ~/projects/2026-Kite-Class-Platform ~/.m2 ~/.local/share/pnpm` |

## Root Cause

Existing `terraform-oracle` is **production-only** — both VMs allocated to running KiteHub stack at full capacity. No headroom for dev tooling (VSCode Server + ARM Java + Maven + pnpm + Docker for builds + AI agents). User's question "migrate WSL up to server" needs a **separate dev workstation deployment**, not extension of production setup.

Three viable architectures (decision required at design phase):

### Option A — Single dev VM (separate from production)

- 1× A1.Flex 4 OCPU + 24 GB RAM (full Always Free quota)
- 200 GB block volume
- Mutually exclusive with production deployment (uses same Always Free quota)
- **Best for:** dev-only workflow; production deployed later or via paid tier
- Cost: $0/month

### Option B — Hybrid: production 2-VM + small x86 dev jump box

- Existing 2× ARM A1.Flex (production)
- Plus 2× AMD x86 1/8 OCPU + 1 GB RAM (Always Free quota for x86) as Tailscale relay / SSH bastion / VSCode tunnel
- Dev work still happens locally on WSL or laptop; jump box just provides remote-access path
- **Best for:** keeping prod + having mobile-access without full dev migration
- Cost: $0/month

### Option C — Dev tools co-resident on production backend VM

- Existing terraform-oracle backend VM (12 GB RAM) runs production stack
- Add VSCode Server + Tailscale + agent runtime ON SAME VM
- Tight resource pressure: production stack already needs 12 GB → adding dev tools requires reducing service heap sizes or accepting swap
- **Best for:** team-of-one with infrequent simultaneous dev + prod stress
- Cost: $0/month

**Recommendation:** Option A initially (dev-only migration; production deferred), evaluate Option B/C once production deployment timeline clarifies.

## Proposed Fix

### Phase 1 — Dev VM provisioning (Option A baseline)

- New Terraform module `infrastructure/terraform-oracle-dev/` separate from `terraform-oracle/` (production)
- 1× A1.Flex 4 OCPU + 24 GB RAM, 200 GB boot volume, ap-singapore-1
- Oracle Linux 9 ARM, ssh-key from variables
- User-data: install Docker + Compose ARM + Tailscale + Node 20 + Java 17 LTS + Maven + pnpm
- VCN with Tailscale-only ingress (no public ports beyond what Tailscale needs); SSH via Tailscale ZeroTier-like mesh (no public IP exposure)

### Phase 2 — Remote development stack

- **Tailscale** mesh — server + Windows laptop on same tailnet
- **VSCode Remote-SSH** — primary dev UX; `code --remote ssh-remote+oracle-dev /home/ubuntu/projects/2026-Kite-Class-Platform`
- **code-server** (browser VSCode) — secondary, for mobile/iPad access via Tailscale Funnel or Cloudflare Tunnel + auth
- **mosh** — SSH that survives network changes (per `feedback_agent_kill_root_cause.md`)
- **tmux** — persistent session for long-running agents (.bashrc auto-attaches)
- **Repo bootstrap** — clone via SSH + run `./scripts/up.sh` to bring full stack up

### Phase 3 — Agent runtime hardening

- Background agents run in tmux sessions on server; survive client disconnect
- Worktree state on server (no `wsl --shutdown` to lose work)
- Agent completion notifications via ntfy.sh push (already configured per `feedback_agent_kill_root_cause.md`)
- DR/backup: Oracle Object Storage snapshots (per `infrastructure/terraform-oracle/main.tf` references GAP-118/119)

### Phase 4 — Decommission WSL2 (optional)

- After 2-week parallel run, decommission local WSL Ubuntu (or keep as fallback)
- Local laptop becomes thin client (VSCode + browser + Tailscale only)

## Acceptance Criteria

- [ ] `infrastructure/terraform-oracle-dev/` module created (separate state from production)
- [ ] 1× ARM A1.Flex 4 OCPU + 24 GB RAM provisioned via terraform apply
- [ ] Tailscale installed + tailnet configured; server reachable via Tailscale hostname (no public SSH)
- [ ] Repo cloned + `./scripts/up.sh` brings full stack up successfully on ARM
- [ ] VSCode Remote-SSH connects + ARM extensions auto-install (Java, TypeScript, Spring Boot Tools, Docker, Copilot, Anthropic Claude if used)
- [ ] code-server accessible via Tailscale Funnel OR Cloudflare Tunnel from mobile browser
- [ ] tmux session "claude" auto-attaches on SSH login (mirror current `.bashrc` line 124-129)
- [ ] mosh-server installed; mosh connection works against Tailscale hostname
- [ ] Background agent in tmux survives client disconnect
- [ ] Documentation: `documents/05-guides/dev-workstation-oracle-cloud-runbook.md` covers provision → onboarding → daily-use → DR
- [ ] Cost verified $0/month after 30-day usage (Always Free quota intact)
- [ ] Idle reclaim mitigation: cron heartbeat job or low-traffic Tailscale keepalive

## Out-of-scope

- Production deployment migration (separate, tracked by existing `terraform-oracle/` initiative)
- AWS backup-tier decision (separate strategic question)
- KiteClass tenant infrastructure (AWS-targeted per existing strategy)
- High-availability / multi-region (Always Free is single-AD; HA = paid tier discussion)
- Custom domain / DNS / SSL for code-server (use Tailscale Funnel hostname or `*.ts.net` initially)

## Caveats / Risks

| # | Risk | Mitigation |
|---|------|-----------|
| R1 | ARM aarch64 — niche Docker images may lack ARM variant | Most common images multi-arch (Postgres/Redis/RabbitMQ/MinIO/Ollama all ✅); fallback x86 emulation via QEMU if needed (slow) |
| R2 | Always Free reclaim if instance "idle" > 7 days | Cron heartbeat (`curl https://kite.example/health` every 1h); Tailscale keepalive |
| R3 | "Out of host capacity" when creating ARM instance | Retry-loop script (`tofu plan + apply` cron until succeed); or Tokyo/Mumbai region as fallback |
| R4 | VN ↔ Singapore latency 60-100ms typical | mosh adapts; saves are async; intermittent typing OK |
| R5 | Account verification requires credit card + may take days | Plan ahead; manual Oracle Cloud signup before Phase 1 |
| R6 | Single VM = single point of failure | Acceptable for dev; production should be split per existing dual-cloud plan |
| R7 | Existing `terraform-oracle/` may already be applied (production) sharing same Always Free quota | **Verify before provisioning** — check OCI console; if quota consumed, choose Option B/C or paid tier |

## Estimated Effort

~2-3 days end-to-end:
- Phase 1: 0.5 day (terraform apply + verify provisioning)
- Phase 2: 0.5 day (Tailscale + VSCode Remote-SSH + code-server setup)
- Phase 3: 0.5 day (tmux + mosh + agent runtime test)
- Phase 4: optional, parallel-run period
- Doc + runbook: 0.5 day

## Related

- Sister initiative (production): `infrastructure/terraform-oracle/` + `documents/03-planning/infrastructure/kitehub-oracle-cloud-deployment.md`
- `feedback_agent_kill_root_cause.md` — Tailscale + mosh + tmux 3-layer stack specifically for SIGHUP survivability
- `feedback_local_verification_discipline.md` — long-running verifications, partial of why server stability matters
- `feedback_ide_warnings_check.md` — dev workstation hygiene
- GAP-244 (dev-stack root fix) — current `dev-start.sh` profile workaround would simplify on cleanly-provisioned server
- Memory `reference_postgres_mcp_setup.md` — Postgres MCP would be straightforward to enable on a stable server

## Log


- 2026-06-14: phase re-triage — n/a→phase-2 (migrate WSL2 dev workstation to Oracle Cloud; DX/infra post-launch).
- **2026-05-05** Filed after WSL stop incident (3 background agents killed mid-Wave-19) + PATH-leak debugging session. Root cause analysis revealed `eval \"$(fnm env)\"` broken-quote was the actual error, not Windows PATH directly — but pattern of incidents (stop + restart + state loss) motivates moving dev workstation to a stable server. Existing `terraform-oracle/` is production-targeted; this gap proposes separate dev VM. Wave-eligibility: NOT during Wave 19. Per `meta-gap-priority.md` Feature P2 (DX) — runs after Wave 19 LEGAL P0 + post-Wave-19 audit suite + GAP-357 deprecation sweep.
