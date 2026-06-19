# GAP-1455: Live-verify INTERNAL_API_URL trên fe-host prod (kitehub + kiteclass SSR) post-AWS-restore

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-16 (Phase-3 GAP-1444 fix — prod-parity follow-up)
**Affects:** `infrastructure/fe-host/pm2-ecosystem.config.js` (kitehub-frontend + kiteclass-frontend env `INTERNAL_API_URL`)

## Problem

GAP-1444 fix thêm `INTERNAL_API_URL` cho server-side SSR fetch. Local Docker dùng `http://kite-gateway:9000` (service DNS). Prod fe-host (EC2, PM2 chạy node trực tiếp — KHÔNG có Docker network) được set `INTERNAL_API_URL=https://api.kitehub.me` trong `pm2-ecosystem.config.js` (cho cả kitehub-frontend + kiteclass-frontend — latent class chung: KC đã dùng `INTERNAL_API_URL` trong code nhưng PM2 chưa từng set).

Giá trị `https://api.kitehub.me` là declared-default hợp lý (public gateway domain, fe-host nginx serve `api.kitehub.me`) NHƯNG chưa live-verify được vì **AWS stack đang stopped** (cost-control + restore-pending per `project_aws_cost_posture`). Cần confirm SSR fetch từ fe-host node process reach gateway thật (không hairpin lỗi / không TLS loop) sau khi AWS restore.

## Proposed Fix

Post-AWS-restore: deploy PM2 config mới → walk:
1. `curl` từ fe-host EC2: server-side fetch path (vd `https://api.kitehub.me/api/v1/beta-status`) → 200.
2. Browser `https://kitehub.me/beta-status` → render live status (không fallback "Không tải được").
3. Nếu hairpin `api.kitehub.me` từ fe-host lỗi (nginx loop / SG) → đổi sang backend private IP / internal DNS.

## Acceptance Criteria
- [ ] fe-host SSR fetch reach gateway (kitehub-frontend beta-status + kiteclass-frontend resolveTenant/landing) → live data, không fallback
- [ ] Giá trị `INTERNAL_API_URL` prod confirmed đúng (api.kitehub.me HOẶC private IP) cho cả 2 FE

## Related
- Parent fix: GAP-1444 (beta-status SSR wrong host)
- Env-coverage class: GAP-802 / GAP-803
- Blocked-by: AWS restore (per `project_aws_cost_posture`)
- Per `.claude/rules/local-fix-production-parity-check.md` §3.2 follow-up exit ramp
