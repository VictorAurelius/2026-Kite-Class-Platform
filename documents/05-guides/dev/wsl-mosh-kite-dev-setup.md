# WSL → Mosh `kite-dev` — Mobile Remote Access Setup

**Audience:** Solo dev needing to continue work from mobile (Termius / Blink Shell / Termux + mosh client) when away from laptop.
**Status:** Active runbook
**Last reviewed:** 2026-05-08
**Cross-refs:**
- `scripts/setup-mosh-kite-dev.sh` — one-shot installer
- `documents/05-guides/dev/wsl2-config.md` — WSL2 base config
- `.claude/rules/agent-action-bias.md` §3 row 2-3 — interactive auth + sudo exceptions

---

## TL;DR

This WSL Ubuntu instance (Ubuntu 24.04 Noble) is now designated as `kite-dev` — a mosh-accessible dev box reachable from mobile via Tailscale tailnet (works on any network, no port-forward needed).

**Stack:**

| Layer | Tool | Purpose |
|---|---|---|
| Bootstrap | OpenSSH server | mosh handshake, fallback access |
| Session | Mosh | survive WiFi/4G switching, sleep/wake, latency |
| Reachability | Tailscale | anywhere VPN (free 100 devices), zero port-forward |
| Auth | SSH ed25519 key | `~/.ssh/id_ed25519.pub` mirrored to mobile |

---

## One-time setup

```bash
# Run as user with sudo:
sudo bash scripts/setup-mosh-kite-dev.sh
```

Idempotent. Installs: `mosh`, `openssh-server`, `tailscale`, `ufw`, `jq`. Sets runtime hostname to `kite-dev`, persists in `/etc/wsl.conf [network]` so WSL restart doesn't reset to Windows machine name. Opens UFW for SSH + mosh UDP. Renames Tailscale device to `kite-dev` (post-up only).

After script completes, run interactively (browser auth):

```bash
sudo tailscale up --hostname=kite-dev
# → Click URL printed, sign in to Tailscale, approve device "kite-dev"
```

Then apply WSL persistent hostname (Windows-side restart, one-time):

```powershell
# In Windows PowerShell:
wsl --shutdown
# → reopen WSL terminal — `hostname` returns kite-dev across reboots forever
```

Then get the tailnet IP:

```bash
tailscale ip -4
# Example: 100.91.182.85
```

**If Tailscale was activated BEFORE this script ran** (legacy state where device name = `vankiet-2` or similar): re-run script after `tailscale up` to rename device to `kite-dev` via `tailscale set --hostname=kite-dev` (no re-auth needed, tailscale 1.50+).

---

## Mobile client setup

### Recommended: Blink Shell (iOS) — native mosh + Termius (Android)

| Step | iOS Blink Shell | Android Termius |
|---|---|---|
| 1. Install | App Store | Play Store |
| 2. Add SSH key | Settings → Keys → Generate or Import | Settings → Keychain → Add private key |
| 3. Mirror key to WSL | `cat id_ed25519.pub` from mobile → append to WSL `~/.ssh/authorized_keys` | same |
| 4. Add host | `host kite-dev hostname=100.64.1.23 user=kitedev` | New Host: address=100.64.1.23, user=kitedev, key=above |
| 5. Connect | `mosh kite-dev` from Blink prompt | "Connect" → choose `Mosh` (not SSH) |

### Alternative: Termux on Android (terminal-only purists)

```bash
# In Termux:
pkg install mosh openssh
ssh-keygen -t ed25519
cat ~/.ssh/id_ed25519.pub  # paste to WSL ~/.ssh/authorized_keys
mosh kitedev@100.64.1.23
```

---

## Connection check from mobile

After full setup:

```
mosh kitedev@<tailnet-ip>
```

Expected: terminal prompt within ~2 seconds. SSH key auth (no password). After connect, mosh works even if you switch from WiFi to 4G mid-session — the connection survives.

Test commands once connected:

```bash
hostname  # → kite-dev
cd ~/projects/2026-Kite-Class-Platform
git status
claude  # if Claude Code CLI installed (separate scope; not part of this runbook)
```

---

## Keep-alive (laptop sleep handling)

Mosh tolerates network changes but NOT host shutdown. When laptop sleeps:

| Approach | Tradeoff |
|---|---|
| Windows Power Settings → "Allow wake timers" + "Sleep: Never on AC" | Heaviest battery use; works |
| Don't sleep; close laptop lid action = "Do nothing" (Windows Power Plan) | Hot lid; works |
| Wake-on-LAN from mobile | Requires router config; complex |
| Cloud dev box (Codespaces / EC2) | Different scope; not WSL |

For Phase 1 BETA solo-dev mode, recommended: keep laptop on AC + lid-close = "Do nothing" when on charger. Mobile reconnects mosh session automatically when laptop wakes.

---

## Tailscale tradeoffs

| Pro | Con |
|---|---|
| Free 100 devices | Free tier privacy: Tailscale Inc has metadata access |
| Zero router config / port-forward | Requires Tailscale account login on mobile + WSL |
| Works on any network (cafe, 4G, hotel WiFi) | Adds 5-15ms latency vs same-LAN direct |
| End-to-end WireGuard encryption | Free tier: max 3 users (solo dev OK) |
| Magic DNS: `ssh kitedev@kite-dev.tail-name.ts.net` | Need account for both endpoints |

If Tailscale not desired, alternatives:
- **LAN only** — Windows port-forward via netsh (requires admin PowerShell): `netsh interface portproxy add v4tov4 listenport=22 connectport=22 connectaddress=$(wsl hostname -I)` + Windows firewall rule. Mobile must be on same WiFi.
- **Cloudflare Tunnel** — free, similar to Tailscale but Cloudflare-routed.
- **SSH port-forward via router** — NOT recommended (laptop on/off + ISP IP changes + internet scans = security risk).

---

## Security posture

| Vector | Mitigation |
|---|---|
| Lost mobile | SSH key has no passphrase by default — set passphrase OR use FaceID/biometric unlock on Blink/Termius |
| Tailnet compromise | Tailscale ACLs limit blast radius; revoke device via admin panel |
| WSL local exploit | UFW limits external surface to 22/tcp + 60000-61000/udp |
| AWS credential exposure | `kite-readonly` profile already set up; ReadOnly blast-radius minimal (rotated 2026-05-08 PR #1064) |

For Claude Code sessions on mobile via mosh: same WSL identity = same AWS profiles available. Tier 2/3 AWS commands still need user explicit approve per `.claude/rules/agent-aws-access.md`.

---

## Closure

| Item | Status |
|---|---|
| Mosh server installed | ✅ Step 1 of script |
| SSH server active | ✅ Was already listening; script idempotent |
| Hostname → `kite-dev` runtime | ✅ Step 3 of script |
| Hostname persistent (`/etc/wsl.conf`) | ✅ Step 3 of script (requires `wsl --shutdown` once on Windows) |
| UFW firewall configured | ✅ Step 4 of script |
| Tailscale installed | ✅ Step 5 of script |
| Tailscale device renamed to `kite-dev` | ✅ Step 6 of script (post-up only — re-run script if needed) |
| Tailscale activated | ⏳ User-interactive (`sudo tailscale up --hostname=kite-dev`, browser) |
| Mobile client config | ⏳ User step (Blink / Termius / Termux) |

After user runs `sudo tailscale up` + activates mobile client, this WSL is reachable from mobile globally as `kite-dev`. Mosh session survives WiFi/4G/sleep — same shell context across day.

## Related

- `scripts/setup-mosh-kite-dev.sh` — installer
- `documents/05-guides/dev/wsl2-config.md` — base WSL config
- `.claude/rules/agent-action-bias.md` — sudo + interactive-auth exceptions
- `.claude/rules/agent-aws-access.md` — same AWS posture applies on mobile session

## Log

- **2026-05-08:** Runbook created + setup script shipped. WSL `VANKIET` → `kite-dev`. Designated as solo-dev's mosh node. Tailscale chosen over port-forward/Cloudflare for "anywhere" use case + zero-router-config. User runs `sudo bash scripts/setup-mosh-kite-dev.sh` once + `sudo tailscale up` for browser auth.
- **2026-05-08 (revision 2):** Persistence + Tailscale-rename added. Original v1 had: (a) hostname reset to `VANKIET` on WSL restart (script set runtime only, not `/etc/wsl.conf`), (b) Tailscale device joined as Windows hostname `vankiet-2` instead of `kite-dev` → Magic DNS lookup fails. Script v2 writes `[network] hostname=kite-dev generateHosts=false` to `/etc/wsl.conf` for persistence + uses `tailscale set --hostname=kite-dev` for clean device rename without re-auth (tailscale 1.50+). Step count 6→7. Runbook updated with `wsl --shutdown` follow-up requirement.
