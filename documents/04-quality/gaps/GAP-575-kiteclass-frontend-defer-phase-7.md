---
title: "GAP-575: kiteclass-frontend defer Phase 7 (Wave 82 Bucket C scope)"
status: OPEN
priority: P2
domain: Frontend
phase: phase-1-beta
wave: 82-bucket-c-deferred
created: 2026-05-15
---

# GAP-575: kiteclass-frontend deploy defer Phase 7 per ADR-031

**Status:** 🔵 OPEN (deferred scope per ADR-031)
**Priority:** 🟡 P2 (Phase 1 BETA scope không include tenant FE; defer khi tenant signup live)
**Domain:** Frontend / Deploy
**Found:** 2026-05-15 Wave 82 Bucket C deploy (kitehub-frontend deployed; kiteclass-frontend explicitly skipped)
**Affects:** Tenant-app FE serving (subdomain `*.kitehub.me`) — currently không có FE serve cho tenant routes

## Problem

Wave 82 Bucket C đã deploy chỉ `kitehub-frontend` (marketing/admin FE on apex `kitehub.me`). `kiteclass-frontend` (tenant FE on `*.kitehub.me` subdomain) chưa deploy.

PM2 ecosystem config có 2 apps:
- ✅ `kitehub-frontend` port 4701 — ONLINE
- ⏸ `kiteclass-frontend` port 4700 — NOT STARTED (explicit `--only kitehub-frontend` filter trong `pm2 start`)

nginx config `infrastructure/fe-host/nginx-fe.conf` đã reference `app.kitehub.me` subdomain (or per Agent 2 design `kiteclass.kitehub.me`) routing to port 4700, nhưng không có process listening → 502 nếu user truy cập subdomain.

## Root Cause

ADR-031 §Decision Outcome documents scope split:
- Phase 1 BETA: kitehub-frontend (marketing + admin)
- Phase 7+ (or post-MVP): kiteclass-frontend (multi-tenant)

Tenant signup flow chưa live (Wave 81 Bucket G spot check verified beta-access endpoint + admin approval flow, nhưng end-to-end tenant onboarding chưa shipped). Khi tenant signup live, kiteclass-frontend deploy mới có user-facing impact.

## Proposed Fix (Phase 7 scope)

Khi Phase 7 reach:

1. Build kiteclass-frontend qua `pnpm build:kc` (script trong root package.json line 9)
2. rsync `.next/standalone` → `/var/www/kiteclass-frontend/kiteclass/kiteclass-frontend/` (parallel pattern với kitehub)
3. Copy `.next/static` + `public/` adjacent
4. PM2 start `--only kiteclass-frontend` (sau khi GAP-574 fix `cwd` path)
5. Verify `curl https://app.kitehub.me/` (hoặc subdomain Agent 2 picked) returns 200
6. Update nginx config nếu cần thêm subdomain routes

OR: extract kiteclass-frontend FE deploy thành riêng Bucket khi cần.

## Acceptance Criteria (when Phase 7 unlocks)

- [ ] kiteclass-frontend Next.js standalone build PASS
- [ ] PM2 starts kiteclass-frontend on port 4700 (no OOM trên t3.small)
- [ ] nginx config serves subdomain (e.g., `app.kitehub.me` hoặc `*.kitehub.me` wildcard subdomain)
- [ ] Cert wildcard `*.kitehub.me` covers subdomain (đã có per current setup)
- [ ] curl `https://<tenant-subdomain>.kitehub.me/` returns 200 end-to-end
- [ ] Memory baseline post-2-app deploy không spike >85% (CloudWatch alarm armed per GAP-566)

## Memory consideration

t3.small 2GB RAM + 2GB swap. Currently 1 Next.js standalone = ~120MB. 2nd standalone sẽ thêm ~120MB → total ~250MB Node + ~50MB nginx + ~400MB system = ~700MB used. Plenty headroom.

Tuy nhiên when ISR regen happens trên cả 2 apps concurrent + traffic spike, peak có thể reach 1.5GB+. Memory alarm at 85% sẽ fire warning. Acceptable cho Phase 7 BETA monitoring.

## Related

- Parent ADR: `documents/02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md` §Phase progression scope
- Sister gap: GAP-574 (PM2 config bugs — affects kiteclass-frontend block khi enable)
- Wave 82 plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §Bucket C scope
- Phase 7 triggers: tenant signup flow live + 1st real tenant request kiteclass subdomain

## Log

- **2026-05-15:** Gap filed as PARTIAL exit-ramp per `gap-done-discipline.md` §3. Wave 82 Bucket C scope explicitly limited to kitehub-frontend per ADR-031; kiteclass-frontend deploy tracked here for Phase 7 visibility. Không blocking Bucket H (dev walk-through scope = marketing + admin tenant onboarding flow on kitehub.me apex).
