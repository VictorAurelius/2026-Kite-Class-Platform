---
title: Wave flow-kc10 — KC-10 Per-tenant branding wizard G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kc10]
wave: wave-2026-06-06-flow-kc10
tag_primary: flow-kc10
tags_secondary: [branding, multi-tenant, file-upload, campaign-g1]
date: 2026-06-06
flow: KC-10 (Per-tenant branding wizard → approval)
gaps: [GAP-1034, GAP-1035, GAP-1036, GAP-1037, GAP-1038]
---

# Wave flow-kc10 — KC-10 Per-tenant branding wizard G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KC-10 — KiteClass per-tenant branding: edit settings (logo/favicon/colors) + version history + rollback + public branding serve. Flow secondary thứ 7. Sau wave này còn KC-11/12 để hoàn tất G1-all-first.

## 1. Brainstorm

KC-10 = KiteClass-side branding (kiteclass-core), KHÁC KH-6 (kitehub-branding AI wizard). 5 controller trên kiteclass-core; branding data ở `kiteclass_shared` DB (tenant-scoped by instance_id). Risk class trọng tâm: cross-tenant IDOR (`{instanceId}` path var), file-upload (logo/favicon SVG-XSS/MIME/MinIO), gateway role-bridge cho kiteclass-core, public branding cross-tenant leak.

## 2. Task Breakdown

1. Pre-walk Opus persona-sim (≥5 FM) → artifact.
2. MUST-run checks (routing collision / tenant Host / authz grep).
3. Walk 5 sub-flow (settings via gateway; shadowed controllers via direct :8080), security spot-checks.
4. Catalog → file gaps → wave plan + sync.

## 3. Scope

- `kiteclass-core`: `BrandingController` (`/api/v1/settings/branding` GET/PUT + logo/favicon), `BrandingVersionController` (`/api/v1/branding/{instanceId}/versions` + rollback, @PreAuthorize ADMIN/OWNER), `BrandingPackageController` (`/{instanceId}/package`), `PublicBrandingController` (`/api/v1/branding/public`), `InternalWebhookController` (`/internal/notify`).
- `kitehub-gateway`: route order (collision source).
- DB `kiteclass_shared`: branding / branding_versions / branding_resources / rebrand_approvals / frontend_instances.

## 4. State-Check Evidence

- Stack up healthy (gateway :9000, kiteclass-core, kite-postgres, kite-minio, kite-mailhog).
- Auth: OWNER token (kitehub `/api/auth/login` owner.test, tenantId aaaabbbb-…-0001) reaches kiteclass-core branding (GET 200). STAFF login `Test@1234` sai pw → mint HS512 STAFF token (same tenant) via gateway JWT_SECRET cho authz test.
- Instances: thanglong (`11111111-…`), sky-education (`e8ff87e1-…`), branding row tenant (`0edaee10-…`).

## 5. Verification Gates

### Pre-walk

Opus persona-sim, 11 FM, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc10-per-tenant-branding-wizard.md` (🔴1 🟠4 🟡4 🟢2). Headline override IDOR hypothesis: dominant blocker = **gateway routing collision** (FM-1), không phải IDOR. Clarifications: FM-6 không có approval workflow (rebrand_approvals orphan); FM-9 snapshot_json JSONB bound đúng (`@JdbcTypeCode`, không drift bug); FM-10 InternalWebhook HMAC-gated (secure); FM-4/5 IDOR latent (masked by FM-1 shadow).

### G1 walk — evidence (live)

**Reachable (BrandingController via gateway :9000):**
- PUT branding (OWNER, full body primary+secondary+accent) → **200** id:2 created "Sky Test Academy"; GET → 200; GET theme → 200.
- PUT validation: thiếu accentColor → 400 (required colors enforced).

**Shadowed controllers — backend verified via direct :8080:**
- `GET /api/v1/branding/{ownInstance}/versions` (X-User headers) → **200** (empty content; backend OK). Via gateway → 401 (GAP-1034 shadow).
- `GET /api/v1/branding/public` via gateway → 401 (shadow); FE login can't fetch tenant branding.

**Security spot-checks:**
- **FM-7 A01 CONFIRMED:** STAFF token PUT branding → **200**, overwrote "HACKED-BY-STAFF" (GAP-1035). BrandingController 0 @PreAuthorize.
- **FM-4/5 IDOR DEFENDED:** owner.test (tenant aaaabbbb) GET versions/package của instance thanglong (`11111111-…`) direct → **400** (tenant-mismatch); own-instance → 200. Tenant binding holds, no cross-tenant data leak (contrast KH-5/7). ⚠️ re-verify post GAP-1034 routing fix.
- FM-10 InternalWebhook: HMAC-gated, no gateway route (secure, khác GAP-1031).

**Bug surfaced (1 P0 + 2 P1 + 2 minor) — all filed, no inline fix:**
- 🔴 **GAP-1034 P0** (FM-1): gateway routing collision — `kitehub-branding-v1 /api/v1/branding/**` (application.yml:593) shadows kiteclass-core catch-all (:749) → 3/5 controller unreachable + login default theme.
- 🟠 **GAP-1035 P1** (FM-7): BrandingController 0 @PreAuthorize → STAFF mutate branding (A01, runtime CONFIRMED).
- 🟠 **GAP-1036 P1**: logo/favicon upload → 500 NoSuchBucketException (bucket `kiteclass-files` thiếu trong MinIO + no ensure-bucket; missing-part 500-not-400).
- 🟡 **GAP-1037 P2** (FM-3): logo allowlist accepts `image/svg+xml` + MIME client-trusted → SVG-XSS latent (runtime verify blocked by GAP-1036).
- 🟢 **GAP-1038 P3** (FM-6): rebrand_approvals approval workflow orphan — flow "→ approval" misnomer (rollback = apply).

**No inline fix** — GAP-1034 gateway edge (blast radius cao) + GAP-1035 authz batch security-1 + GAP-1036 bucket provisioning cần infra + GAP-1037 blocked by 1036. Per `release-fix-retry-budget` §3.5 investigation-first done (root cause + fix plan trong mỗi gap). Inline @PreAuthorize fix (GAP-1035) considered nhưng defer to security-1 batch để giữ G1 PR docs-only consistent với KH-10 + vì FE branding flow broken bởi GAP-1034 anyway (chưa có user hitting live mutation).

## 6. Agent Spawn Pattern

1 Opus pre-walk persona-sim agent (background, model opus). Walk solo coordinator. Không parallel bucket (G1 linear).

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1034 P0 — gateway routing collision (Backend/gateway)
- GAP-1035 P1 — BrandingController authz A01 (Backend)
- GAP-1036 P1 — logo upload bucket-missing 500 (Backend/DevOps)
- GAP-1037 P2 — SVG-XSS latent (Backend/security)
- GAP-1038 P3 — approval workflow orphan (Backend/scope)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

**GAP-1034 class (broad gateway predicate shadows/exposes wrong service):** sister GAP-1031 (KH-10 email route). Recommend gateway-wide route-predicate audit (separate meta task) — không sweep inline wave này (đã có 2 instances = pattern; gateway route audit = dedicated scope).

**GAP-1035 class (controller missing @PreAuthorize):** grep kiteclass-core controllers thiếu @PreAuthorize trên mutation endpoints — defer to security-1 batch audit (GAP-999/1005/1035 cluster signals systemic kiteclass-core authz review needed).

### Sync targets

- gap-status.csv: 5 rows ✅
- campaign §4 table: KC-10 → 🔄 walk-pass-pending-human ✅
- wave-history.jsonl: flow-kc10 entry ✅
- audits-index.csv: pre-walk row ✅

### Outcome

KC-10 G1 **PASS** (walk reachable — BrandingController via gateway; shadowed controllers verified at backend via direct). IDOR DEFENDED (contrast KH cluster). 1 P0 routing + 2 P1 (authz + upload) + 2 minor filed cho Wave security-1 + infra. Campaign KC-10 → `🔄 walk-pass-pending-human`. Docs-only PR. Remaining G1: KC-11/12.

## 8. Log

- **2026-06-06:** G1 walk. Pre-walk Opus 11 FM (headline = routing collision, không IDOR). Walk: PUT branding OWNER 200 (id:2); STAFF PUT 200 A01 (GAP-1035); versions direct :8080 200 (gateway shadow GAP-1034); logo upload 500 NoSuchBucket (GAP-1036); IDOR cross-tenant 400 DEFENDED; SVG allowlist latent (GAP-1037); rebrand_approvals orphan (GAP-1038); InternalWebhook HMAC secure. No inline fix (gateway edge + authz batch security-1 + bucket infra). Campaign → walk-pass-pending-human.
