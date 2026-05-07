# GAP-401: Multi-arch Docker Build (linux/amd64 + linux/arm64)

**Status:** 🔵 OPEN
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

- [ ] All 11 production images build cho cả `amd64` + `arm64`
- [ ] Buildx GHA cache enabled (giảm ~50% build time subsequent runs)
- [ ] `docker manifest inspect` shows 2 arch entries per tag
- [ ] CI build time vẫn <15 min total cho 11 images × 2 arch (parallelized)

## Related

- GAP-398 (parent)
- AWS Graviton pricing (~20% discount Phase 2)
- Apple Silicon dev parity
