# GAP-458: Domain decision — `kitehub.me` qua GitHub Student Pack (free 1 năm)

**Status:** 🟢 DONE 2026-05-09 — runbook claim Student Pack `.me` shipped + 4 docs updated cho path Free
**Priority:** 🟠 P1 — domain choice block Phase 1 BETA pre-launch §2.1
**Domain:** Documentation / DevOps / Pre-launch
**Found:** 2026-05-09 (user audit "có tên miền nào không phải vn mà free không")
**Affects:** `documents/05-guides/account-prep/02b-github-student-pack-free-domain.md` (mới), `account-prep/02-domain-registrar.md`, `deploy/vercel-production-setup.md`, `deploy/dns-setup-runbook.md`, `deploy/README.md`, `release-1-plan-2026.md`

## Problem

User-flagged audit 2026-05-09: "có tên miền nào không phải vn mà free không, tôi cần 1 tên miền cho release 1 thôi". Existing docs (`account-prep/02-domain-registrar.md`, `dns-setup-runbook.md`, `vercel-production-setup.md`) đều giả định mua `.vn` paid (~$60/year) — không cover free path.

Sau analysis 5 options, user chọn **Path C — GitHub Student Developer Pack free `.me` domain qua Namecheap** (1 năm free, $10-20/year renewal):
- ✅ User confirmed Student Pack đã verified
- ✅ Domain choice: `kitehub.me` (KiteHub là front door SaaS — tenants signup + manage subscription)
- KiteClass tenants access qua subdomain pattern (vd `tenant1.kitehub.me/class`) hoặc path-based routing

## Background — Why kitehub.me

Per CLAUDE.md §Project Overview:
- **KiteHub** = SaaS platform quản lý education instances (signup, billing, invite, branding)
- **KiteClass** = Multi-tenant education platform per-tenant (student, course, class, attendance)

KiteHub là "front door" — beta tenants land vào marketing page → sign up via beta-access form → get invite email → claim code → tenant created. KiteClass chỉ accessible sau khi tenant login (subdomain hoặc path).

Phase 1 BETA invite-only ~10-20 tenants → 1 domain đủ; Phase 1.5 PAID public sẽ cần thêm hoặc upgrade. Student Pack 1 free `.me` cover được full Release 1 timeline (~6 tháng combined Phase 1 BETA + Phase 1.5 PAID).

## Cost analysis

| Item | Cost |
|---|---|
| `.me` domain qua GitHub Student Pack (Namecheap) | **$0 first year** |
| Renewal sau 1 năm | ~$10-20/year (Namecheap regular) |
| Cloudflare Free tier | $0 |
| Vercel Hobby Free (Phase 1 BETA invite-only non-commercial) | $0 |
| Vercel Pro upgrade (Phase 1.5 PAID commercial — yêu cầu Vercel TOS) | $20/month |
| ACM cert (AWS) | $0 |
| **Total Release 1 (~6 tháng):** | **$0** Phase 1 BETA + **~$20/mo** Phase 1.5 PAID portion |

Đây là path tiết kiệm tối đa cho Release 1 với branding pro (custom `.me` domain).

## Tradeoffs vs `.vn`

| Aspect | `.vn` paid | `.me` free (Student Pack) |
|---|---|---|
| Cost first year | ~$60 (2 domains) | **$0** (1 domain) |
| Renewal | ~$60/year | ~$10-20/year |
| Vietnamese market trust | ✅ ccTLD signal địa phương | ⚠️ generic gTLD |
| SSL Let's Encrypt | ✅ | ✅ |
| Cloudflare proxy | ✅ | ✅ |
| Vercel custom domain bind | ✅ | ✅ |
| 2 products separate (kitehub + kiteclass) | ✅ | ⚠️ subdomain pattern (vd `class.kitehub.me`) hoặc path routing |
| Phase 3 K-12 expansion | ⚠️ K-12 schools mong domain VN-rooted | ⚠️ Global appeal nhưng kém trust địa phương |
| Renewal commitment | Có mua = pay-or-lose | Có thể chuyển sang `.vn` Phase 1.5 PAID nếu cần |

**Recommendation:** dùng `.me` cho Release 1 (~6 tháng). Phase 2 P3 hoặc Phase 3 K-12 nếu thấy cần `.vn` thì mua thêm khi đó.

## Proposed Fix

### Phase 1 — New runbook (~30 min)

`documents/05-guides/account-prep/02b-github-student-pack-free-domain.md` covering:
- §1 Verify Student Pack eligibility
- §2 Claim `.me` domain qua Namecheap (Student Pack offer)
- §3 Cấu hình Cloudflare nameservers
- §4 Renewal strategy + auto-renew off

### Phase 2 — Update existing guides (~30 min)

| Guide | Edit |
|---|---|
| `account-prep/02-domain-registrar.md` | Add §"Free alternatives" link tới 02b runbook; clarify VN paid vs Student Pack free choice |
| `deploy/vercel-production-setup.md` §3.1 | Domain plans table thêm "kitehub.me" examples; default plan B subdomain pattern thay đổi |
| `deploy/dns-setup-runbook.md` §1 | Phase 1 BETA strategy table support cả `.vn` lẫn `.me` |
| `deploy/cloudflare-setup.md` | Domain examples include `.me` |
| `deploy/README.md` Tier 1 §1 | Add note "Free path C qua Student Pack — xem 02b" |
| `release-1-plan-2026.md` §1 Decision context | Cross-link domain choice |

## Acceptance Criteria

- [x] Phase 1: Runbook 02b shipped với step-by-step claim từ Student Pack
- [x] Phase 2: 4 existing guides updated với cross-links + free alternative path
- [x] kitehub.me decision recorded trong gap (this file) + ROADMAP §🚀 Next Action
- [ ] CI run on fix-PR shows green (no broken cross-links)

## Compliance

- ✅ CLAUDE.md tiếng Việt mandate — runbook + updates đều tiếng Việt
- ✅ `release-1-deploy-plan.md` §2.1 — domain procurement step has Free alternative
- ✅ `agent-action-bias.md` — runbook là user-action, agent KHÔNG tự claim domain (cần GitHub login + verify)

## Related

- Parent: `release-1-deploy-plan.md` §2.1 pre-deploy checklist (domain procurement step)
- Sibling: GAP-457 (deploy guides VN sync — landed PR #1083)
- Original .vn path: `account-prep/02-domain-registrar.md`
- Free TLD analysis: chat session 2026-05-09 (5 options ranked → user chose Path C)

## Log

- **2026-05-09** Filed in response to user audit "có tên miền nào không phải vn mà free không". 5 free options analyzed (vercel.app, .eu.org, GitHub Student Pack, promo first-year, Cloudflare Pages); user chose Path C `.me` qua Student Pack (verified eligibility). Domain choice = kitehub.me (KiteHub front door SaaS). Runbook 02b shipped + 4 docs updated. Status flipped 🔵 → 🟢 DONE same day.
