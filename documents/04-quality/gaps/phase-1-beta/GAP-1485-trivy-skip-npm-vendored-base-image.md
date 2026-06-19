# GAP-1485: Trivy skip npm-vendored base-image CVEs (forward-prevention)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** DevOps
**Phase:** phase-1-beta
**Found:** 2026-06-19 (repo-status audit — 17 HIGH CodeQL/Trivy)
**Affects:** `.github/workflows/docker-build-push.yml` (Trivy scan step)

## Problem

Repo-status RED driver = 17 HIGH "CodeQL" alert. Audit 2026-06-19: tool thật = **Trivy** (image scan), 17/17 HIGH nằm trong `/usr/lib/node_modules/npm/node_modules/` (tar/glob/minimatch/cross-spawn) — **deps bundled của npm CLI** trong base image `node:22-trixie-slim`. npm là build-tool, KHÔNG chạy runtime (Next.js standalone dùng node). **0 runtime exposure, 0 code mình.** Mỗi image scan re-detect → tích alert → RED giả.

## Fix (DONE this PR)

1. **Dismiss 17 HIGH** hiện tại (`won't fix`, documented npm-vendored/not-runtime) — clear RED tức thì, durable.
2. **`skip-dirs: usr/lib/node_modules/npm`** trong Trivy step `docker-build-push.yml` — path-skip (root-fix, không per-CVE `.trivyignore` per `release-fix-retry-budget.md` §4) → scan tương lai không re-add npm-vendored noise.

## Acceptance Criteria

- [x] 17 HIGH dismissed won't-fix với reason documented
- [x] Trivy `skip-dirs` npm path added, YAML valid
- [ ] Verify ở next image scan (tag/dispatch): 0 npm-vendored alert mới (AWS-gated — validate khi redev)

## Related

- Audit verdict 2026-06-19 (npm-vendored noise, 0 our-code)
- Cluster: GAP-400 (Trivy scan PARTIAL) + GAP-442 (base image bump PARTIAL)
- Python base CVE (banner-renderer cpython, medium/low) = separate, không trong 17 HIGH
