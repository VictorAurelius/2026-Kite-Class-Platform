# GAP-911: CI workflows thiếu `concurrency: cancel-in-progress` → stale superseded runs

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Phase:** phase-1-beta
**Found:** 2026-06-03 (Wave 14 #2134 — push 3 commits liên tiếp → 3 Test Core Maven build chạy song song; phải `gh run cancel` thủ công)
**Affects:** `.github/workflows/{core-ci,quality-db,quality-code,quality-docs,quality-rules-skills,frontend-ci,kitehub-ci}.yml` (+ check gateway-ci nếu có)

## Problem

7 test/quality workflows thiếu `concurrency:` block → GitHub KHÔNG auto-cancel superseded runs khi push commit mới lên cùng branch. Hệ quả: mỗi push fix-up = thêm 1 run mới TRONG KHI run cũ vẫn chạy. Wave 14 #2134 push 3 commits (518af5d fixes → b2f0b20 gitleaks → 07086560 descope) → 3 Test Core Service Maven build (heavy, ~5-10 phút mỗi cái) chạy song song, burn CI minutes + self-hosted runner contention. Coordinator phải `gh run cancel` thủ công 2 stale runs.

Meta coverage hiện tại: `fix-up-ci-selective-rerun.md` cover **manual path-based cancel** (unrelated workflow trên fix-up) NHƯNG KHÔNG cover **auto superseded-commit cancel** (cùng workflow, SHA cũ). Đây là 2 axis khác nhau.

## Root Cause

Workflows thiếu:
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```
Không có concurrency group → GitHub treat mỗi push độc lập → old-SHA runs tiếp tục. (6 workflows KHÁC đã có concurrency: deploy-design-system / e2e-pre-release / docker-build-push / lighthouse / release-tag / zap-baseline — nhưng đó là deploy/heavy workflows, không phải PR test gates.)

## Proposed Fix

1. Add `concurrency: { group: ${{ github.workflow }}-${{ github.ref }}, cancel-in-progress: true }` vào 7 PR-triggered test/quality workflows.
2. Extend `.claude/rules/fix-up-ci-selective-rerun.md` — note concurrency-groups là PRIMARY mechanism (auto superseded cancel); manual path-based cancel = fallback hẹp cho unrelated-workflow case.
3. Per `.claude/rules/README.md` skill-vs-rule split: deterministic CI behavior = config/infra (concurrency YAML), KHÔNG phải model-interpreted rule.

## Acceptance Criteria

- [ ] 7 workflows có `concurrency: cancel-in-progress: true` keyed on workflow+ref
- [ ] Verify: push 2 commits liên tiếp lên 1 PR branch → run của commit đầu auto-cancelled
- [ ] `fix-up-ci-selective-rerun.md` extended (concurrency = primary, manual cancel = fallback) + version bump + Log entry

## Related

- Discovered in: Wave 14 #2134 (PR wave-14-bcde-entity-audit-replay) 2026-06-03
- `.claude/rules/fix-up-ci-selective-rerun.md` — sister mechanism (manual path-based cancel)
- `.claude/rules/README.md` skill-vs-rule split — deterministic CI = config not rule
- Workflow split context: quality-{code,docs,rules-skills,infra}.yml (2026-05-22)

## Log

- **2026-06-03:** Filed per `discovery-to-gap-inline-filing.md` — user flagged "meta ko cover vấn đề này à" sau khi coordinator phải manual cancel 2 stale CI runs Wave 14 #2134. User direction: "session sau fix luôn đấy nhé". Defer fix to next session (gpt-5.5) — context ~80% wrap-up. P2 (efficiency/CI-waste, không block functionality).
