# Docker Scripts Required

## CRITICAL: Bắt buộc dùng scripts cho Docker operations

**KHÔNG BAO GIỜ** chạy lệnh Docker trực tiếp. **LUÔN LUÔN** dùng scripts trong `kitehub/scripts/`.

## ❌ WRONG - Không được dùng

```bash
# ❌ NEVER use these directly
docker-compose -f docker-compose.kitehub.yml up -d
docker-compose -f docker-compose.kitehub.yml down
docker-compose -f docker-compose.kitehub.yml logs
docker-compose -f docker-compose.kitehub.yml build
docker-compose -f docker-compose.kitehub.yml restart
docker exec -it kitehub-postgres psql
docker images | grep kitehub
docker ps
```

## ✅ CORRECT - Phải dùng scripts

```bash
# ✅ ALWAYS use scripts
./scripts/up.sh                    # Start stack
./scripts/down.sh                  # Stop stack
./scripts/down.sh --volumes        # Stop + remove data
./scripts/logs.sh gateway -f       # View logs
./scripts/build-all.sh             # Build all images
./scripts/rebuild.sh gateway       # Rebuild single service
./scripts/restart.sh gateway       # Restart service
./scripts/exec.sh postgres         # Execute in container
./scripts/status.sh --health       # Check status
./scripts/clean.sh --all           # Full cleanup
./scripts/help.sh                  # Show all commands
```

## Scripts Reference

| Script | Thay thế cho | Mô tả |
|--------|-------------|-------|
| `up.sh` | `docker-compose up -d` | Start stack |
| `down.sh` | `docker-compose down` | Stop stack |
| `logs.sh` | `docker-compose logs` | View logs |
| `build-all.sh` | `docker build` + `docker-compose build` | Build images |
| `rebuild.sh` | `docker-compose build` + `up` | Rebuild + restart |
| `restart.sh` | `docker-compose restart` | Restart service |
| `exec.sh` | `docker exec` | Run command in container |
| `status.sh` | `docker ps` + `docker-compose ps` | Check status |
| `clean.sh` | `docker rmi` + `docker system prune` | Cleanup |

## Lý do

1. **Consistency**: Scripts đảm bảo đúng project name, đúng compose file
2. **Safety**: Scripts có confirmation cho destructive operations
3. **Convenience**: Scripts có defaults hợp lý, giảm typing
4. **Documentation**: `help.sh` luôn hiện các commands available
5. **Best practices**: Scripts enforce đúng thứ tự build (base → children)

## Áp dụng cho

- **KiteHub** (`kitehub/`): Dùng `kitehub/scripts/*`
- **KiteClass** (`kiteclass/`): Dùng `kiteclass/scripts/*` (nếu có)

## Exception

Chỉ được dùng Docker commands trực tiếp khi:
- Debug script issues
- One-time operations không có script tương ứng
- Explicitly requested by user
