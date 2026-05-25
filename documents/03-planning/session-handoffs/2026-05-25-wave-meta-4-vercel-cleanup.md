---
date: 2026-05-25
wave: meta-4
tag_primary: meta
tags_secondary: [vercel-decommission, csp-cleanup, workflow-comment, docs-only, background-agent]
status: complete
audience: dev
---

# Session Handoff — Wave meta-4 closure (Vercel residue cleanup)

## Scope shipped

**Wave meta-4** dọn Vercel residue khỏi repo để eliminate Vercel FAILURE check cycle khỏi mọi PR. Per `no-vercel-references.md` v1.0.0 (Wave 88 decommission 2026-05-17), Vercel đã bị gỡ làm production FE hosting, nhưng còn 3 lớp residue cần xử lý trong wave này:

| Lớp | Scope | Action |
|---|---|---|
| 2 | `kitehub/kitehub-frontend/vercel.json` + `kiteclass/kiteclass-frontend/vercel.json` | `git rm` cả 2 files |
| 4 | CSP `script-src` + `connect-src` trong 2 `next.config.js` | Remove `vercel.live`, `*.vercel-scripts.com`, `va.vercel-scripts.com`, `vitals.vercel-insights.com` |
| 5 | Comment legacy `.github/workflows/deploy-production.yml` line 6 | `KC stack via Vercel until then` → `self-hosted per ADR-025, Wave 82 pivot` |

Lớp 1 (production hosting Vercel→AWS EC2) đã ship Wave 82/88. **Lớp 3 (Vercel GitHub App uninstall) cần user click GUI parallel** — out-of-scope agent.

## Investigation evidence (per `release-fix-retry-budget.md` §3.5)

- `vercel.json` cả 2: `deploymentEnabled.main: false` + `github.silent: true` → an toàn gỡ (Vercel CI ngưng run sau gỡ)
- `next.config.js` CSP: FE source `kitehub/kitehub-frontend/src/` + `kiteclass/kiteclass-frontend/src/` KHÔNG có `@vercel/*` SDK imports; `package.json` không depend `@vercel/*` → CSP allowlist là dead config
- Workflow `deploy-production.yml` line 6 chỉ là comment legacy (kc-app EC2 đã có instance qua Wave 2.3+); workflow logic không dùng Vercel

## Files changed

```
.github/workflows/deploy-production.yml     |  2 +-
kiteclass/kiteclass-frontend/next.config.js |  4 ++--
kiteclass/kiteclass-frontend/vercel.json    | 41 --------------------------
kitehub/kitehub-frontend/next.config.js     |  4 ++--
kitehub/kitehub-frontend/vercel.json        | 41 --------------------------
```

Total: 5 files, -82 / +3 lines.

## Verification

- `node -e "require('./kitehub/kitehub-frontend/next.config.js')"` → OK
- `node -e "require('./kiteclass/kiteclass-frontend/next.config.js')"` → OK
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy-production.yml'))"` → OK
- `grep -niE 'vercel' ` 3 modified files → 0 matches (clean)
- Per `local-self-test-before-aws-deploy.md` §2.2 — PR này KHÔNG trigger AWS deploy workflow, pure config sweep → out-of-scope mandate full local stack up

## Pickup state cho session sau

1. **User action required (Lớp 3):** uninstall Vercel GitHub App qua `https://github.com/settings/installations`. Sau khi uninstall:
   - Verify PR mới không còn Vercel check trong `gh pr checks`
   - Remove `ADMIN_MERGE_OVERRIDE: Vercel` exception list khỏi `admin-merge-discipline.md` `## Vercel decommissioned exception` (nếu có) — final use of trailer cho Vercel class
2. Wave meta-5 candidates carry-forward từ Wave beta-readiness-5 closure:
   - GAP-746 P1 multi-tenant repository tenant filter — dedicated future wave
   - GAP-747 SES IAM live verify post GAP-612 AWS restore
   - ops-readiness-audit post-Wave-br-5 within 3 days (per `post-wave-audit-mandate.md` deadline 2026-05-28)
3. No follow-up gap filed Wave meta-4 (scope complete trong 1 PR)

## 4-target sync per `post-merge-sync-completeness.md`

| Target | Status |
|---|---|
| `gap-status.csv` | N/A — không gap mới hay flip |
| `ROADMAP.md` §🎯 Current Status Snapshot | ✅ updated (Wave meta-4 → snapshot; Wave beta-readiness-5 → Previous) |
| `wave-history.jsonl` | ✅ appended entry meta-4 |
| `MEMORY.md` index | N/A — không memory entry mới |
| Session handoff note (this file) | ✅ created |

## Override trailer used

```
ADMIN_MERGE_OVERRIDE: Vercel – kiteclass FAILURE = vendor-decommissioned false-positive per no-vercel-references.md v1.0.0; same exception class until Lớp 3 App uninstall complete (user GUI parallel). Final use of override trailer cho Vercel — sau Lớp 3 ship, không cần.
ADMIN_MERGE_FOLLOWUP: User uninstall Vercel GitHub App via https://github.com/settings/installations parallel session này — verify trên PR mới sau merge không còn Vercel check.
```
