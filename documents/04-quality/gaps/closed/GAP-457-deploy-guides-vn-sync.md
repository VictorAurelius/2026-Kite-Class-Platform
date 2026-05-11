# GAP-457: Deploy guides — Vercel runbook + tiếng Việt sync 3 Mixed guides + README workflow index

**Status:** 🟢 DONE 2026-05-09 — `vercel-production-setup.md` mới + 3 Mixed guides VN-synced + README workflow index 8+5 actions
**Priority:** 🟠 P1 — Phase 1 BETA pre-launch user-action coverage; CLAUDE.md tiếng Việt mandate
**Domain:** Documentation / DevOps / Deploy
**Found:** 2026-05-09 (user audit "có guide tiếng Việt cho tất cả 8 actions chưa?")
**Affects:** `documents/05-guides/deploy/README.md`, `documents/05-guides/deploy/vercel-production-setup.md` (mới), `documents/05-guides/deploy/dns-setup-runbook.md`, `documents/05-guides/deploy/aws-cost-scheduling.md`, `documents/05-guides/deploy/right-size-stress-test.md`

## Problem

User-flagged audit 2026-05-09: 6/8 user-executable Phase 1 BETA pre-launch actions có guide tiếng Việt; 2 actions thiếu guide hoàn toàn (Vercel-related); 3 guides thuần Mixed VN/EN ở header/audience.

Audit kết quả:

| § | Action | Guide | Trạng thái |
|---|---|---|---|
| 1 | Mua domain `.vn` | `account-prep/02-domain-registrar.md` | ✅ VN |
| 2 | Cloudflare account + nameservers | `deploy/cloudflare-setup.md` | ✅ VN |
| 3 | Cloudflare DNS A record → ALB | `deploy/cloudflare-setup.md` + `deploy/dns-setup-runbook.md` §2.3 | ✅/⚠️ |
| 4 | SSL Full(strict) + ACM | `deploy/dns-setup-runbook.md` §2.4 | ⚠️ Mixed audience EN |
| **5** | **Vercel `NEXT_PUBLIC_API_URL` env vars** | ❌ **THIẾU** | — |
| **6** | **Vercel custom domain bindings** | ❌ **THIẾU** | — |
| 7 | Resume EC2 + RDS | `deploy/aws-cost-scheduling.md` §4 | ⚠️ Mixed |
| 8 | ALB health + smoke test | `deploy/right-size-stress-test.md` + `scripts/smoke-test.sh` | ⚠️ Mixed |

CLAUDE.md §CRITICAL Communication Language quy định "ALWAYS communicate in Vietnamese (tiếng Việt)" — applies to user-facing docs targeting solo dev workflow.

## Proposed Fix

### Phase 1 — `vercel-production-setup.md` mới (~1h)

Ship combined guide cho §5 + §6 vì cả 2 cài qua cùng UI Vercel (Project Settings):
- §5 Environment Variables (Production env): `NEXT_PUBLIC_API_URL` + GA tracking + Sentry DSN
- §6 Domains tab: bind `kitehub.vn` / `kiteclass.vn` (hoặc subdomain pattern)
- Verification steps: smoke test FE → check CSP + headers + API call success

### Phase 2 — VN sync 3 Mixed guides (~1h)

3 guides có body tiếng Việt nhưng heading/audience English. Convert:
- `dns-setup-runbook.md` — Audience line + section heads
- `aws-cost-scheduling.md` — TOC headings
- `right-size-stress-test.md` — Title + audience

### Phase 3 — `deploy/README.md` workflow index (~1h)

README hiện chỉ có 1 paragraph generic mô tả deploy folder. Update thành workflow index theo Phase 1 BETA pre-launch order:
- 8 user-executable actions với link guide + cost matrix
- 5 extras (account creation, Activate, password manager, superadmin login, SES)
- Critical-path ordering (mua domain → Cloudflare → SSL → Vercel env → Vercel domain → resume compute → smoke)
- Link tới `release-1-deploy-plan.md` §2.1 pre-deploy checklist

## Acceptance Criteria

- [x] Phase 1: `documents/05-guides/deploy/vercel-production-setup.md` viết bằng tiếng Việt covering §5 + §6 với screenshots/text examples + verification steps
- [x] Phase 2: 3 Mixed guides có VN audience/title/main section headings
- [x] Phase 3: `deploy/README.md` updated với workflow index 8+5 actions + cost matrix link
- [ ] CI run on fix-PR shows green (no broken links from cross-references)

## Compliance

- ✅ CLAUDE.md §CRITICAL Communication Language — tiếng Việt mandate cho all user-facing docs
- ✅ `release-1-deploy-plan.md` §2.1 pre-deploy checklist — closes user-action coverage gap (§5 + §6)
- ✅ `output-review-mandate.md` §3 — guides là output type cần review standard
- ✅ `docs-folder-structure.md` — README required cho `documents/` top-level folder; this PR enhances existing

## Related

- Parent: Phase 1 BETA pre-launch user actions (per session audit 2026-05-09)
- Sibling: `account-prep/` 4 files (already VN, audience pattern reference)
- Link cluster: GAP-369 DNS, GAP-370 SES, GAP-371 Cloudflare CDN

## Log

- **2026-05-09** Filed in response to user audit: "có guide tiếng Việt cho tất cả 8 action chưa?" Coverage 6/8 + 3 Mixed; 2 actions (§5 §6 Vercel) hoàn toàn thiếu. User chose Path 3 (full sync — Vercel guide + Mixed sync + README index). Status flipped 🔵 → 🟢 DONE same day after PR ship.
