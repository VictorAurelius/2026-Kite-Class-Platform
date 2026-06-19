# GAP-1482: `/opt/kite-prod` không phải git repo trên EC2 → deploy scripts không pull được fix

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-18 (Bước 2 deploy — SSM `git rev-parse HEAD` → no-git trên cả 2 EC2)
**Affects:** `deploy-production.yml` workflow + `scripts/deploy-prod.sh` + `scripts/deploy-kc.sh`

## Problem

`/opt/kite-prod` trên cả kh-backend (i-05d7af46d01436b96) lẫn kc-app (i-01ad56b0067d0213b) **KHÔNG có `.git`** — files được đặt thủ công (Jun 17 dates, hot-patch overlays). Trong khi deploy scripts giả định git-managed:

- `deploy-prod.sh:81` check `[[ ! -d "$DEPLOY_DIR/.git" ]]` → **exit 3** ("bootstrap has not run") → `deploy-production.yml` workflow sẽ FAIL.
- `deploy-kc.sh:33` no-git → WARN-and-continue dùng files CŨ → không pull fix mới.

2026-06-18 phải workaround bằng `curl` 3 file fix trực tiếp từ `raw.githubusercontent.com/.../main` để deploy PR #2490. Workaround ổn cho lần này nhưng deploy chuẩn (workflow) vẫn vỡ tới khi git được khôi phục.

## Proposed Fix

Khôi phục git-managed `/opt/kite-prod` trên cả 2 EC2: `git init` + add remote (public repo) + `fetch` + `reset --hard origin/main`, hoặc re-clone sạch. Verify `deploy-prod.sh` qua hết check exit-3. Cân nhắc đưa vào EC2 user_data (per `deploy-production.yml` comment line 134-137 nói user_data clone repo — nhưng thực tế no-git → user_data clone có thể đã fail OR `.git` bị xoá).

## Acceptance Criteria

- [ ] `git -C /opt/kite-prod rev-parse HEAD` trả SHA hợp lệ (cả 2 EC2)
- [ ] `deploy-prod.sh` qua được Step 1 git check (không exit 3)
- [ ] `deploy-production.yml` workflow_dispatch chạy thành công end-to-end

## Related

- Discovered in: PR #2490 deploy session 2026-06-18
- Audit: `documents/04-quality/audits/aws-verification/2026-06-18-kc-subscription-config-deploy-pre-mutation.md`
