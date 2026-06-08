# GAP-1060: `rebuild.sh all` / `build-all.sh` không rebuild KiteClass + lỗi "no service selected"

**Status:** 🟢 DONE
**Priority:** 🟠 P2 (Meta — build tooling; silent-failure class)
**Domain:** DevOps
**Found:** 2026-06-08 (WSL `kite` env setup — Task "rebuild image local theo code mới nhất")
**Affects:** `kitehub/scripts/build-all.sh`, `kitehub/scripts/rebuild.sh`

## Problem

Phát hiện khi rebuild local images trên WSL `kite` mới. Hai bug liên quan trong build tooling:

1. **`build-all.sh` không build KiteClass.** Script tự nhận là "build all" nhưng chỉ build subset KiteHub (base + 5 backend + frontend). `kiteclass-core` + `kiteclass-frontend` (đều có build context trong `docker-compose.kitehub.yml`) bị bỏ sót → chạy trên image cũ. Step numbering còn ghi `[N/6]` dù chỉ có 3 step.

2. **`rebuild.sh all` báo "no service selected".** Bước restart cuối chạy `docker-compose -f docker-compose.kitehub.yml up -d` (trần). Mọi service trong compose đều có `profiles:` tag → bare `up -d` chọn **0 service** (verified: `config --services` trần trả 0, `--profile full` trả 14). Lỗi này + `set -e` làm `rebuild.sh all` exit non-zero, khiến chuỗi `rebuild.sh all && rebuild.sh kiteclass-core && ...` dừng giữa chừng → KiteClass im lặng không được rebuild dù exit code tổng (qua `tee`) là 0.

Hệ quả: dev tin "rebuild all" đã rebuild toàn bộ theo code mới nhất, nhưng KiteClass vẫn chạy image cũ (3-9 ngày) — silent staleness.

## Fix (shipped)

- `build-all.sh`: thêm Step 4 (`kiteclass-core`) + Step 5 (`kiteclass-frontend`) qua `docker-compose build`; sửa numbering `/6` → `/5`.
- `rebuild.sh all`: `docker-compose ... up -d` → `docker-compose ... --profile full up -d` + comment giải thích profile.

## Acceptance Criteria

- [x] `build-all.sh` build cả kiteclass-core + kiteclass-frontend
- [x] `rebuild.sh all` không còn "no service selected" — `--profile full` chọn đủ 14 service
- [x] `bash -n` + `shellcheck -S error` clean cả 2 script
- [x] Live verify: rebuild thực tế trên WSL `kite` → 14 container chạy image `:latest` mới, healthy

## Related

- Discovered in: WSL `kite` env-setup session 2026-06-08; fix PR branch `fix/rebuild-script-profile-kiteclass`
- Rule: `discovery-to-gap-inline-filing.md` (filed inline same session)
- Compose profiles: `kitehub/docker-compose.kitehub.yml` (per GAP-407 Wave 37)
