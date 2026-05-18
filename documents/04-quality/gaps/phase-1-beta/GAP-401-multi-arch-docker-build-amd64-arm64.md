# GAP-401: Multi-arch Docker Build (linux/amd64 + linux/arm64)

**Status:** 🟡 PARTIAL — Wave 37 Bucket B
**Priority:** 🟡 P2
**Domain:** Docker / Build optimization
**Found:** 2026-05-07 (Wave 37 — Layer 2)
**Affects:** Future cloud diversification + Apple Silicon dev parity

## Problem

Hiện build chỉ `linux/amd64`. Phase 2+ có thể cần ARM:
- AWS Graviton instances (ARM, ~20% giảm cost vs equivalent x86)
- Apple Silicon dev (M1/M2/M3) chạy local image qua Rosetta 2 chậm

## Proposed Fix

Buildx multi-platform build:

```yaml
- uses: docker/setup-buildx-action@v3
- uses: docker/build-push-action@v5
  with:
    platforms: linux/amd64,linux/arm64
    push: true
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

QEMU emulation for ARM build trên amd64 runner (~3-5x slow vs native, OK cho Phase 1).

## Acceptance Criteria

- [x] All production images build cho cả `amd64` + `arm64` on push-to-main / tag (`platforms: linux/amd64,linux/arm64` in push-to-ecr job)
- [x] Buildx GHA cache enabled (`cache-from/to: type=gha,scope=${{ matrix.service }}` per-service scoping)
- [x] QEMU setup added (`docker/setup-qemu-action@v3`) for cross-arch emulation
- [ ] `docker manifest inspect` 2-arch verification — deferred to first live ECR push
- [ ] CI build time <15 min total — verify on first multi-arch push run

## Log

- **2026-05-07** (Wave 37 Bucket B): QEMU + Buildx setup + `platforms: linux/amd64,linux/arm64` in push job. PR build job uses amd64-only to keep PR CI fast. Cache scoped per-service to avoid cross-pollination. Live verification deferred to first push to main / tag.

## Related

- GAP-398 (parent)
- AWS Graviton pricing (~20% discount Phase 2)
- Apple Silicon dev parity
