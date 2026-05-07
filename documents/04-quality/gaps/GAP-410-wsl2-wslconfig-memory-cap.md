# GAP-410: WSL2 .wslconfig Template (Memory Cap)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps / WSL2 / Local dev
**Found:** 2026-05-07 (Wave 37 — Layer 4)
**Affects:** Windows host stability khi WSL2 grab quá nhiều RAM

## Problem

WSL2 default cấp 50% host RAM cho Linux VM. Trên 32GB Windows host → WSL2 lấy ~16GB. Nếu Docker Desktop + IDE + browser chạy đồng thời → Windows base bị ép memory → swap to disk → freeze.

## Proposed Fix

`.wslconfig` template document `documents/05-guides/dev/wsl2-config.md` + `.wslconfig.example`:

```ini
[wsl2]
memory=24GB                  # cap WSL2 → Windows giữ ≥3GB safety
processors=8                 # max CPU cores cho WSL2
swap=8GB                     # explicit swap file size
swapFile=C:\\temp\\wsl2-swap.vhdx
localhostForwarding=true
nestedVirtualization=false   # save resources nếu không cần
```

User copy → `%USERPROFILE%\.wslconfig` → `wsl --shutdown` → restart WSL2.

## Acceptance Criteria

- [ ] `documents/05-guides/dev/wsl2-config.md` documents recommended values
- [ ] `.wslconfig.example` template file ở repo root (gitignored production OK)
- [ ] Trade-off table: 16GB cap (baseline) / 24GB cap (recommended) / 28GB cap (heavy dev) / no cap (default)
- [ ] Document Windows 11 OOM Killer behavior khi WSL2 OOM

## Related

- GAP-407 (compose profiles)
- GAP-408 (JVM heap cap)
- Microsoft WSL2 best practices doc
