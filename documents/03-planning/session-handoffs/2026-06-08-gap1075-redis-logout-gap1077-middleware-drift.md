# Session Handoff 2026-06-08 — GAP-1075 Redis logout + GAP-1076 admin + GAP-1077 middleware drift

**Ngày:** 2026-06-08
**Branch:** `fix/v87-attendance-status-normalize-kc5` (22 commit ahead `origin/main`, **PR #2274 OPEN**, chưa merge)
**Tiếp nối:** `2026-06-08-kc1-g2-optionb-logout-design-first-meta.md`
**Context khi handoff:** 81% (818k/1M Opus)

---

## 1. Việc đã làm session này

### Gaps DONE (live-verified)
- **GAP-1075 DONE** — server-side logout + Redis refresh-token blacklist (kitehub-subscription). Full Redis blacklist (design-canonical, user chọn). `POST /api/auth/logout` + `RefreshTokenBlacklistService` (fail-open) + `refresh()` reject revoked + spring-data-redis wire + FE `auth.ts`. 14 unit test. **Live walk PASS Redis thật** (login→refresh 200→logout 200→blacklist key `refresh-blacklist:43a280f5...`→reuse 400 reject). Commit `fa1f5faa`/`41313d7a`, moved closed/.
- **GAP-1073 cross-flow sweep DONE** — kitehub-frontend `client.ts` + `use-branding.ts` cùng bug FormData Content-Type (mất boundary) → FIX. Build exit 0. Commit `57afef32`.
- **GAP-1076 DONE** — kitehub-admin unhealthy: SB3 thiếu `spring.data.redis` block (compose chỉ set SB2 `SPRING_REDIS_HOST`) → RedisHealthIndicator default localhost DOWN. Fix fallback chain → admin healthy + redis UP. Found via GAP-1075 sweep. Commit `b1a88edb`.

### Meta-rules
- **`g1-browser-walk-before-flip` v1.1.0** — §3.1 production-accurate domain simulation: host/subdomain flow PHẢI test bằng **subdomain Host thật (nip.io default no-sudo, /etc/hosts fallback)**; CẤM `?tenant=`/query-override làm bằng chứng (bypass `extractSlugFromHost`). Trigger: tôi chọn `?tenant=` trong recipe GAP-811 (bypass resolution). Commit `b059e6aa` + memory `feedback_production_accurate_local_domain_sim`.

### GAP-1077 filed (P1 OPEN) — design↔code drift LỚN
- **Phát hiện:** host→tenant middleware build SAI frontend. Design (`tenant-domain-landing-architecture.md`) = **kiteclass-frontend**; code build ở **kitehub-frontend** (`:3001`, tag GAP-811). kiteclass-frontend middleware = **0 file** → GAP-811 thực ra **CHƯA done** (session trước + recipe tưởng done ở :3001 = nhầm).
- **User chốt (AskUserQuestion):** middleware THUỘC **kiteclass-frontend (theo design)**.
- GAP-811 thêm drift banner (kitehub middleware ≠ deliverable; chưa flip DONE). Commit `4a3d3bfa`.

### Recipe G2 rescoped
- `2026-06-08-g2-recipe-kc1-remaining-browser-walks.md` — **BỎ Mảng C (GAP-811)** (sai scope: là KiteClass landing, không phải KC-1 dashboard; test sai FE). KC-1 recipe giờ = **Mảng A (cross-tab :3000) + Mảng B (logo :3000)** only. KC-1 dashboard lấy tenant từ JWT claim, không host middleware (Phase 2).

### Stack
- Rebuild all images (`build-all.sh`) + `up.sh --force-recreate` → **13/13 healthy** (image mới nhất cho WSL). Infra (postgres/redis/rabbitmq/minio/mailhog) + tất cả service UP.

---

## 2. Next session — theo user

### 2.1 Tối ưu agents (user yêu cầu)
- Review chiến lược spawn agents (per `agent-model-opus-default` Opus default + `agent-background-spawn-default` background + `feedback_parallel_agent_strategy` wave-pack ≤5 concurrent). Cụ thể hóa khi bắt đầu.

### 2.2 Fix GAP-1077 (implement middleware đúng FE)
1. Implement `kiteclass-frontend/src/middleware.ts` host→tenant per GAP-811 Approach A (đọc Host → BE resolve UUID `by-subdomain` → inject `x-tenant-id` cho SSR landing). BE endpoint `by-subdomain/{slug}` đã LIVE (verified 200 → Sky).
2. Quyết định số phận `kitehub-frontend/src/middleware.ts` (remove nếu apex thuần marketing / repurpose nếu KH có preview surface riêng — cần design làm rõ).
3. Align domain `kitehub.me` (ad-interim) vs `kiteclass.com` (design) — hoặc update design doc. Liên quan brand-pivot deferred (memory).
4. Walk production-accurate qua **kiteclass-frontend `:3000`** bằng **nip.io subdomain** (`sky-education.127.0.0.1.nip.io:3000`) per `g1-browser-walk-before-flip` §3.1 — KHÔNG `?tenant=`.
5. Flip GAP-811 + GAP-1077 DONE sau khi đúng FE + walk PASS.

### 2.3 KC-1 G2 (chờ human)
- Recipe sạch (A+B `:3000`). Credentials verified: A=`owner@skyedu.vn`/`SkyEdu@2026`, B=`owner.test@test.vn`/`Test@1234`. Human test → báo 4-outcome → flip GAP-1074/1072/1073.

### 2.4 PR #2274
- 22 commit, chưa merge main. Cân nhắc merge hoặc tiếp tục accumulate.

---

## 3. Credentials / facts verified (sau re-seed 2026-06-08)
- Tenant A Sky: `owner@skyedu.vn`/`SkyEdu@2026`, instance `0edaee10-2d13-44be-9151-12b78b7c5fd4`, subdomain `sky-education`.
- Tenant B: `owner.test@test.vn`/`Test@1234`, subdomain `skytest`/`sky-test`.
- ⚠️ Credential cũ `walk.owner+bucketb@skyedu.vn` KHÔNG còn (re-seed).
- nip.io verified: `sky-education.127.0.0.1.nip.io` → resolve 127.0.0.1, middleware parse `parts[0]`.

## 4. Port mapping (tránh nhầm lại)
- `:3000` = **kiteclass-frontend (KiteClass)** — KC-1 dashboard
- `:3001` = **kitehub-frontend (KiteHub)** — marketing apex (middleware GAP-811 ở đây = SAI chỗ, GAP-1077)
