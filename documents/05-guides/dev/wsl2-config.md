# WSL2 `.wslconfig` Template — Memory Cap (GAP-410)

**Wave:** 37 (Layer 4 Local dev resource)
**Status:** Active 2026-05-07
**Related:** GAP-407 (Compose profiles), GAP-408 (JVM heap cap)

## Vấn đề

WSL2 mặc định cấp **50 % RAM của host** cho Linux VM. Trên Windows 11 host 32 GB → WSL2 lấy ~16 GB. Khi chạy đồng thời Docker Desktop + IDE + browser + Slack/Teams → Windows base bị ép xuống dưới 8 GB → swap-to-disk → freeze hoặc OOM Killer.

## Giải pháp

Tạo file `.wslconfig` ở `%USERPROFILE%` (ví dụ `C:\Users\<you>\.wslconfig`), apply qua `wsl --shutdown` rồi mở lại WSL.

Template mẫu được commit ở repo root: [`.wslconfig.example`](../../../.wslconfig.example) — copy ra `%USERPROFILE%\.wslconfig`.

## Trade-off matrix theo dung lượng RAM host

| Host RAM | WSL2 cap | Workload phù hợp | Trade-off |
|---:|---:|---|---|
| 16 GB | **8 GB** | infra-only / single-service dev | Windows giữ 8 GB; full stack KHÔNG chạy nổi |
| 32 GB | **24 GB** *(recommended)* | full stack + IDE + browser | Windows giữ ≥8 GB an toàn; đủ cho `--profile full` |
| 32 GB | 28 GB | heavy dev (full + AI local) | Windows chỉ còn ~4 GB — risk nếu mở Teams/Slack |
| 64+ GB | 32-48 GB | nhiều worktree parallel | Windows dư dả; workload lớn không thiếu |
| any | (unset) | default Microsoft (50 %) | dễ swap-storm khi load đồng thời cao |

## File `.wslconfig.example`

```ini
[wsl2]
memory=24GB
processors=8
swap=8GB
swapFile=C:\\temp\\wsl2-swap.vhdx
localhostForwarding=true
nestedVirtualization=false
# kernelCommandLine=...   # advanced; uncomment nếu cần custom kernel args
# pageReporting=true       # Win11 22H2+: cho Windows reclaim memory khi WSL idle
```

### Field reference

- **memory** — Hard cap RAM cho VM. WSL2 không dùng vượt mức này.
- **processors** — Số vCPU. Mặc định = tất cả host CPU; cap để Windows giữ resources.
- **swap** — Swap file size cho VM. Đặt 8 GB cho safety net khi spike memory.
- **swapFile** — Path swap file. Để trên ổ SSD/NVMe để giảm latency. Không trỏ ra mạng.
- **localhostForwarding** — `true` để truy cập service WSL qua `localhost` từ Windows. Bắt buộc để IDE debug + browser test backend trên `localhost:9000`.
- **nestedVirtualization** — Tắt nếu không cần Docker-in-Docker hoặc VM lồng. Save RAM + CPU.
- **pageReporting** — Win11 22H2 trở lên; cho Windows reclaim WSL idle memory tự động (giảm áp lực cap cứng).

## Áp dụng

```powershell
# 1. Copy template ra user profile
Copy-Item .wslconfig.example "$env:USERPROFILE\.wslconfig"

# 2. Tắt tất cả WSL distro
wsl --shutdown

# 3. Mở lại WSL — config mới active
wsl
```

Verify trong WSL:

```bash
free -h     # total memory should match cap
nproc       # processor count
```

## Windows 11 OOM Killer hành vi

Khi WSL2 đụng cap, Linux kernel inside VM trigger OOM Killer kill process **lớn nhất theo RSS**. Trên dev workflow:

- Spring Boot service (~1-2 GB RSS) thường bị kill trước Postgres
- Postgres cluster ~512 MB-1 GB tùy load
- Docker engine itself ~200 MB

**Symptoms:** service Spring Boot crash random, log có `Killed` (no stack trace). Fix:

1. Tăng WSL2 cap thêm 2-4 GB nếu host còn dư
2. Giảm số service chạy đồng thời → dùng `--profile branding-only` hoặc `beta-funnel` thay `full`
3. Áp JVM heap cap (GAP-408 đã ship — `JAVA_TOOL_OPTIONS` cap mỗi service ~512-768 MB)

## Combine với Compose profiles + JVM cap

Stack hiện tại với cả 3 layer optimize (37GB → 27GB → fits Windows 32GB host):

```
Host 32 GB
├── Windows base + apps  ~6-8 GB
├── WSL2 cap (.wslconfig)  24 GB
│   ├── Docker engine + overhead  ~1 GB
│   ├── Compose --profile full  ~18 GB
│   │   ├── 5 services × 512 MB JVM cap = 2.5 GB (was 20+ GB without cap)
│   │   ├── 2 frontend Node × 512 MB = 1 GB
│   │   ├── PG/Redis/RMQ/MinIO/MailHog ~3 GB
│   │   └── headroom ~11 GB cho tests + AI cloud calls
│   └── IDE/shell overhead ~1 GB
└── Buffer swap ~8 GB
```

Profile `full` không kéo Ollama. Nếu cần test AI local:

```bash
# Combine profiles
docker compose -f kitehub/docker-compose.kitehub.yml --profile full --profile ai-local up -d
```

Tốn thêm ~6-8 GB cho Ollama; chỉ làm khi host có ≥40 GB.

## Theo dõi

Theo dõi RAM consumption thực tế:

```bash
# Trong WSL
watch -n 5 'free -h && echo --- && docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"'

# Trên Windows (PowerShell)
Get-Counter '\Memory\Available MBytes' -Continuous
```

Nếu `Available MBytes` Windows xuống dưới 2000 → giảm WSL2 cap hoặc đóng app khác.
