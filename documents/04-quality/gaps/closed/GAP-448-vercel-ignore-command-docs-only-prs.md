# GAP-448: Vercel `ignoreCommand` — Skip build cho docs-only PRs

**Status:** 🟢 DONE 2026-05-08 (shipped trong PR #1035 cùng Wave 43 plan)
**Priority:** 🟠 P1
**Domain:** Infrastructure / CI / Cost
**Found:** 2026-05-08 (PR #1035 review — Vercel build trigger trên docs-only PR)
**Affects:** Vercel free tier build minutes (6,000 min/month) + merge gate UNSTABLE state

## Problem

Vercel Git Integration default behavior = build mọi commit trên branch link với project. Path filter trong GitHub Actions workflows (frontend-ci, kitehub-frontend-ci) skip docs-only PRs đúng, nhưng Vercel KHÔNG đọc workflow YAML — webhook trigger build trên mọi push.

Kết quả:
- Mỗi docs-only PR → 2 Vercel builds (kiteclass + kitehub) × ~1-2 min = ~3-4 min wasted
- Solo-dev ~5 docs PR/ngày = ~10% Vercel free tier monthly quota cháy vô ích
- PR #1034 đã hit `Vercel – kitehub: build-rate-limit upgrade prompt` → confirm chạm rate limit
- PR #1035 (docs-only Wave 43 plan) trigger 2 Vercel builds dù không touch FE → wasted

Đi ngược spirit "solo-dev tiết kiệm resources" đã chốt 2026-04-24 cho 6 test workflows GitHub Actions.

## Root Cause

Vercel monorepo project setup không có `vercel.json` với `ignoreCommand`. Default = build always.

## Proposed Fix

Tạo `vercel.json` ở mỗi FE project root với `ignoreCommand` git-diff filter:

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "ignoreCommand": "git diff HEAD^ HEAD --quiet -- :/kiteclass/kiteclass-frontend :/packages/shared-ui :/pnpm-lock.yaml :/package.json && exit 0 || exit 1"
}
```

**Logic:**
- `git diff HEAD^ HEAD --quiet -- <paths>` → exit 0 nếu KHÔNG có diff trong những path liệt kê
- `&& exit 0` → exit 0 = Vercel skip build
- `|| exit 1` → exit 1 = Vercel build

**Path filters per project:**

| Project | Trigger build khi diff trong |
|---|---|
| `kiteclass` | `kiteclass/kiteclass-frontend/` + `packages/shared-ui/` + `pnpm-lock.yaml` + `package.json` |
| `kitehub` | `kitehub/kitehub-frontend/` + `packages/shared-ui/` + `pnpm-lock.yaml` + `package.json` |

**Pathspec `:/`** = absolute từ repo root, hoạt động từ bất kỳ subdir nào (Vercel project Root Directory typically là FE subdir).

## Acceptance Criteria

- [x] `kiteclass/kiteclass-frontend/vercel.json` created với `ignoreCommand` filter cho kiteclass scope
- [x] `kitehub/kitehub-frontend/vercel.json` created với `ignoreCommand` filter cho kitehub scope
- [x] JSON schema reference (`$schema`) cho IDE validation
- [x] Path filters bao gồm: FE folder + shared-ui + lockfiles
- [ ] Verify post-merge: PR docs-only tiếp theo phải skip Vercel build (state = `succeeded` với "Build Skipped" message hoặc check không xuất hiện)
- [x] Documentation: GAP-448 (this file) + commit message rõ rationale

## Verification (post-merge)

Sau khi PR #1035 merge, push commit docs-only test:
```bash
echo "test" >> documents/04-quality/gaps/test.md && git add . && git commit -m "test: verify Vercel skip"
```
Vercel dashboard expected: deployment shows "Skipped" status, no build minutes consumed.

Rollback nếu vercel.json gây lỗi: `git revert <commit>` — vercel.json absence = back to default behavior (build always).

## Related

- **Spirit alignment:** CLAUDE.md "CI Trigger Policy — Solo-dev Mode (2026-04-24)" — same rationale (skip redundant CI work)
- **Sister rules:** `release-fix-retry-budget.md` (cost discipline), `output-review-mandate.md` §3 (every output reviewed including infra config)
- **Wave 43:** filed cùng PR plan #1035 (docs-only context surfaced this gap)
- **Vercel docs:** https://vercel.com/docs/projects/project-configuration/git-configuration#ignored-build-step

## Log

- **2026-05-08** — Filed + shipped same-PR (PR #1035 Wave 43 plan). User-flagged sau khi check CI trên #1035 thấy Vercel pending dù PR docs-only. Spirit-violation với 2026-04-24 CI Trigger Policy. Fix: 2× `vercel.json` với git-diff `ignoreCommand`. Status DONE trên ship — verification post-merge tracked.
