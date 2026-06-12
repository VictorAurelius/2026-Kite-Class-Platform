# GAP-1241: Sweep stale tenant-platform domain `kiteclass.vn`/`.com` → canonical `kitehub.me` + CI detector

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-12 (user-flagged "kitehub.me mà? => fix toàn bộ các chỗ đang khiến claude nhận thức sai")
**Affects:** code runtime (BE email/branding/gateway/subscription + FE wizard/middleware/seo) + infra (helm / k8s / terraform / nginx) + active docs (architecture / business 3-layer / planning / guides / diagrams) + skills

## Problem

Domain mua THẬT duy nhất của nền tảng = **`kitehub.me`** (GAP-458/459 Path C GitHub Student Pack; `documents/02-architecture/env-reference.yaml` `domain_root = kitehub.me` ghi rõ "supersedes kiteclass.com STALE"; terraform `var.domain_name` default đã flip kiteclass.com→kitehub.me trước đó). Tenant landing production = `{slug}.kitehub.me` (CSP/CORS production đã có `*.kitehub.me`; `SlugAvailabilityService` có RESERVED denylist).

Tuy nhiên ~270 reference STALE tới `kiteclass.vn` / `kiteclass.com` còn rải rác trong code + docs ACTIVE — gồm:
- BE: `EmailServiceClient`, `MockProvisioningService`, `BrandingJobV1Controller`, `DomainService` backup URL, gateway `BASE_DOMAIN` default, email Thymeleaf templates (`{slug}.kiteclass.vn`, `support@kiteclass.com`).
- FE: wizard `WelcomeStep`/`Step6Preview`/`DoneStep`, `tenant-url.ts`, middleware/test fixtures, SEO/footer/sitemap, `themeReceiver` ALLOWED_ORIGINS.
- Infra: `nginx.conf` server_name, k8s `kiteclass-template` ingress/configmap, helm `values.yaml` hosts/origins/emails, terraform `dns` module + tfvars example.
- Docs: architecture (`tenant-domain-landing-architecture.md` mermaid), business 3-layer, planning runbooks, ui_kits HTML mockups, skills.

Mỗi reference khiến Claude (và reader) nhận thức SAI rằng `kiteclass.com` là product surface — đúng class "nhận thức sai" user flag. Statically-detectable per `cross-flow-bug-class-sweep.md` §4.1 → cần persistent CI detector, không chỉ grep 1 lần.

## Fix (shipped same PR)

1. **Sweep** mọi tenant-platform reference `kiteclass.vn`/`.com` → `kitehub.me` (tenant URL → `{slug}.kitehub.me`; apex/main-site/CDN/email tương ứng) qua code + infra + active docs + skills.
2. **GIỮ (marker-exempted `# stale-domain-ok`):**
   - `DomainService.RESERVED_DOMAINS` denylist (protective claim-denylist, NOT tenant URL).
   - `DomainServiceTest` custom-domain example `app.kiteclass.com` (denylist test fixture).
   - `env-reference.yaml` + terraform `variables.tf` supersession notes (documents the flip).
3. **CI detector** `scripts/check-stale-domain-references.sh` (WARN-mode job `stale-domain-references` trong `quality-code.yml`) + self-test fixtures — guards re-introduction across MỌI flow trên MỌI PR.
4. **Meta** `kitehub-kiteclass-boundary.md` §2 "Domain (prod)" KC cell làm rõ `{slug}.kitehub.me` (v1.0.1).

## Acceptance Criteria

- [x] 0 stale `kiteclass.vn`/`.com` trong scan scope (chỉ marker-exempted còn lại) — `bash scripts/check-stale-domain-references.sh` exit 0.
- [x] Special-case transforms đúng (`kitehub.kiteclass.com`→`kitehub.me`, `hub.kiteclass.com`→`kitehub.me`, dedup themeReceiver origins).
- [x] Flyway migrations + Docker LABEL namespaces (`com.kiteclass.commit-hash`) KHÔNG bị sửa (false-positive excluded).
- [x] Affected BE tests PASS (EmailServiceClient / DomainService / MockProvisioning / Branding) + FE tsc/vitest PASS.
- [x] CI detector self-test PASS (FAIL-fixture flagged / keep-marker exempt / canonical clean / archived excluded).

## Related

- Discovered in: wave `documents/03-planning/waves/wave-2026-06-11-branding-100.md` (domain canonical sweep)
- Sister: GAP-1088 (kitehub.vn/.com email domain drift — different domain, out of scope here)
- Canonical: GAP-458/459 (kitehub.me Path C), `env-reference.yaml` domain_root
- Meta: `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable → persistent detector), `kitehub-kiteclass-boundary.md` v1.0.1

## Log — 2026-06-12: merged PR #2374

Sweep merged (#2374, 281 files): code runtime + infra + docs ACTIVE + boundary rule §2 +
CI detector `check-stale-domain-references.sh` (WARN-mode job `stale-domain-references`,
strict scan 0 finding). Post-merge: UI kits landing parity smoke ✅; terraform fmt hotfix
c0bd957ba cùng branch trước merge. Residual cosmetic (ngoài scope): terraform identifier
`kiteclass_com_*` trong modules/dns (commented-out block, detector không match identifier).
