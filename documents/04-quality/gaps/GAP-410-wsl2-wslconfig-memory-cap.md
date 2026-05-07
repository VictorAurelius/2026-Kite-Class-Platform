# GAP-410: WSL2 .wslconfig Template (Memory Cap)

**Status:** 🟢 DONE 2026-05-07 (Wave 37 Bucket D PR pending)
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

- [x] `documents/05-guides/dev/wsl2-config.md` shipped — full field reference + apply steps + verify commands + stack-up calculation
- [x] `.wslconfig.example` template ở repo root — sane default 24GB cap + commented optional fields
- [x] Trade-off matrix 5 rows: 8GB (16GB host) / 24GB (32GB recommended) / 28GB (32GB heavy) / 32-48GB (64+GB) / unset (default)
- [x] Windows 11 OOM Killer behavior documented — symptoms + 3-step fix path

## Log

- **2026-05-07 (Wave 37 Bucket D):** Shipped. Doc + example template ở repo root. Cross-link với GAP-407 profiles + GAP-408 JVM cap để show stack-up calculation (host 32GB → WSL 24GB cap → JVM cap 5×512MB → fits comfortable).

## Related

- GAP-407 (compose profiles)
- GAP-408 (JVM heap cap)
- Microsoft WSL2 best practices doc
