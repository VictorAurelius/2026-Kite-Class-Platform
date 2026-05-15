# 07 — Vercel Account Setup Runbook

**Audience:** Solo dev tạo Vercel account lần đầu cho Phase 1 BETA — FE production hosting + preview deploys per PR + custom domain `app.kitehub.me`.
**Standards:** AWS Well-Architected (Reliability + Performance Efficiency) · ADR-025 (Phase 1 BETA hosting strategy) · `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2.
**Cross-link upstream:** Yêu cầu `01-aws-account-creation.md` (Secrets Manager để lưu env vars) + `05-cloudflare-account-setup.md` (DNS records cho `app.kitehub.me` CNAME → Vercel) + repo GitHub `VictorAurelius/2026-Kite-Class-Platform` đã active.
**Cross-link downstream:** Blocks `documents/05-guides/deploy/vercel-production-setup.md` (production env var seeding + custom domain binding) + `documents/05-guides/deploy/fe-self-host-runbook.md` (fallback path nếu Vercel free-tier exhausted) + `documents/05-guides/deploy/cloudflare-setup.md` (proxy rules cho `app.kitehub.me`).
**Estimated time:** ~45 min (chưa kể đợi domain DNS propagation 5-30 min).
**Last-Updated:** 2026-05-15

---

## TL;DR

> Tạo tài khoản Vercel Hobby (Free) qua GitHub OAuth → import repo `VictorAurelius/2026-Kite-Class-Platform` → cấu hình build (root directory `kitehub/kitehub-frontend`, framework Next.js) → seed env vars từ AWS Secrets Manager → link custom domain `app.kitehub.me` (CNAME → Vercel) → đánh giá Pro upgrade nếu hit build rate limit.

Quick path 7 bước cho Phase 1 BETA:

1. Signup tại [vercel.com/signup](https://vercel.com/signup) via GitHub OAuth
2. Click "Add New..." → "Project" → Import `VictorAurelius/2026-Kite-Class-Platform`
3. Configure: root `kitehub/kitehub-frontend`, build `pnpm build`, output `.next`, framework Next.js
4. Add env vars: `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_SUPPORT_EMAIL`, etc. (lấy từ Secrets Manager)
5. Deploy → verify preview URL `<project>.vercel.app`
6. Domains → Add `app.kitehub.me` → copy CNAME target → paste vào Cloudflare DNS
7. Monitor Usage → bật alerts nếu approaching Hobby tier limits → upgrade Pro $20/month khi cần

---

## 1. Trước khi bắt đầu — chuẩn bị

| Item | Yêu cầu |
|------|---------|
| GitHub account | Active với repo `VictorAurelius/2026-Kite-Class-Platform` accessible (own hoặc collaborator). |
| Email | Cùng email với GitHub (Vercel sẽ inherit). |
| Domain `kitehub.me` | Đã active tại Cloudflare (`05-cloudflare-account-setup.md` xong) — cần add CNAME `app`. |
| AWS Secrets Manager | Setup từ `01-aws-account-creation.md` — env vars seed từ đây qua `documents/05-guides/deploy/vercel-production-setup.md`. |
| Repo build success local | `cd kitehub/kitehub-frontend && pnpm install && pnpm build` chạy thành công trên máy dev. |
| `vercel.json` (optional) | Repo root hoặc `kitehub/kitehub-frontend/vercel.json` nếu cần custom config (rewrites, headers, redirects). |
| Password manager | Vault active (`03-password-manager.md`) để lưu Vercel credentials + integration tokens. |

⚠️ **Critical:** Vercel sync env vars CLEAR text sang build environment. NEVER commit `.env.local` / `.env.production` vào git. Source of truth = AWS Secrets Manager.

---

## 2. Step-by-step

### 2.1 Signup Vercel Hobby (~5 min)

1. Mở [vercel.com/signup](https://vercel.com/signup).
2. Click "Continue with GitHub" → authorize Vercel access GitHub account.
3. Confirm email (Vercel inherit từ GitHub).
4. Plan: **Hobby (Free)** — 100GB bandwidth/month, 6000 build min/month, unlimited static deploys.
5. Username: chọn unique slug (vd `kitehub-dev`) — sẽ thành URL pattern `<username>.vercel.app`.

Hobby tier coverage Phase 1 BETA:
- ≤5 tenants × moderate traffic = đủ bandwidth
- Build minutes: Next.js cold build ~3-5 min, ~6000/month = 1200-2000 builds = đủ cho dev velocity
- 100 deployments/day limit = đủ

Pro upgrade trigger ($20/user/month):
- Build rate limit hit (KEY ISSUE — observed Wave 83 closure CI): Hobby tier cấp 1-2 concurrent build, queue rest. Heavy CI = serial wait.
- Bandwidth > 100GB/month (≥10 tenants production traffic)
- Cần Speed Insights / Web Analytics paid tier
- Cần team collaboration (multiple GitHub users access Vercel project)

### 2.2 Import GitHub repo (~5 min)

1. Vercel dashboard → "Add New..." → "Project".
2. Section "Import Git Repository" → search `2026-Kite-Class-Platform`.
3. Nếu repo không hiển thị: click "Adjust GitHub App Permissions" → Vercel GitHub App → enable repo `VictorAurelius/2026-Kite-Class-Platform`.
4. Click "Import" cho repo.

### 2.3 Configure build settings (~10 min)

Vercel sẽ scan repo + đề xuất framework. Override per Phase 1 BETA structure:

| Setting | Value | Lý do |
|---------|-------|-------|
| **Framework Preset** | Next.js | Auto-detected |
| **Root Directory** | `kitehub/kitehub-frontend` | Frontend riêng trong monorepo, KHÔNG phải repo root |
| **Build Command** | `pnpm build` | Default Next.js build |
| **Output Directory** | `.next` | Next.js convention |
| **Install Command** | `pnpm install --frozen-lockfile` | Reproducible builds qua pnpm-lock.yaml |
| **Development Command** | `pnpm dev` | Cho preview deploys |
| **Node.js Version** | 20.x | Per `package.json` engines field |

⚠️ **`vercel.json` precedence:** Nếu repo có `kitehub/kitehub-frontend/vercel.json` thì config trong đó override dashboard. Kiểm tra:

```bash
cat kitehub/kitehub-frontend/vercel.json 2>/dev/null || echo "No vercel.json — use dashboard config"
```

Nếu chưa có `vercel.json` và muốn lock config trong git (recommended): tạo file với:

```json
{
  "buildCommand": "pnpm build",
  "outputDirectory": ".next",
  "installCommand": "pnpm install --frozen-lockfile",
  "framework": "nextjs"
}
```

Commit file này vào repo → Vercel sẽ honor.

### 2.4 Add environment variables (~10 min)

Vercel dashboard → Project → Settings → Environment Variables.

Phase 1 BETA env vars cần thiết (lấy từ AWS Secrets Manager hoặc Cloudflare):

| Variable | Value source | Environments |
|----------|--------------|--------------|
| `NEXT_PUBLIC_API_URL` | `https://api.kitehub.me` | Production + Preview + Development |
| `NEXT_PUBLIC_SUPPORT_EMAIL` | `support@kitehub.me` | All |
| `NEXT_PUBLIC_BETA_STATUS_URL` | `https://kitehub.me/beta-status` | All |
| `NEXT_PUBLIC_SENTRY_DSN` (defer) | Sentry project DSN (Phase 1.5+ optional) | All |
| `NEXT_PUBLIC_VERCEL_ENV` | auto-injected `production` / `preview` / `development` | All (system) |

⚠️ **`NEXT_PUBLIC_*` prefix:** = bundled into client JS = visible to anyone. NEVER prefix `NEXT_PUBLIC_` cho secret keys (API keys, DB credentials).

Server-only env vars (KHÔNG prefix `NEXT_PUBLIC_`):
- `KITEHUB_API_INTERNAL_TOKEN` (nếu FE call BE qua API Routes proxy) — defer Phase 1.5+
- `DATABASE_URL` (chỉ khi FE có serverless function call DB direct — KHÔNG dùng Phase 1 BETA pattern)

Seed từ Secrets Manager (one-shot manual):
```bash
# Local export
export NEXT_PUBLIC_API_URL="https://api.kitehub.me"
export NEXT_PUBLIC_SUPPORT_EMAIL="support@kitehub.me"

# Add qua Vercel CLI (alternative to dashboard)
npx vercel env add NEXT_PUBLIC_API_URL production
# → paste value when prompted
```

Hoặc full automation: `documents/05-guides/deploy/vercel-production-setup.md` §5 (env var sync script).

### 2.5 Deploy initial build (~5 min build + verify)

1. Click "Deploy" (botton sau khi configure env).
2. Vercel kick off build:
   - Clone repo → install dependencies → run `pnpm build` → upload `.next` artifacts
   - Wall-clock: ~3-5 min cho Next.js cold build
3. Verify preview URL: `https://<project-name>-<hash>.vercel.app`.
4. Browser test:
   - Landing page renders
   - API call hit `https://api.kitehub.me` (DevTools Network tab)
   - Console không có errors

⚠️ **Common build failures Phase 1 BETA:**
- `pnpm-lock.yaml` out of date → run `pnpm install` local + commit lock
- TypeScript errors → run `pnpm typecheck` local first
- Missing env vars → check Settings → Environment Variables completeness

### 2.6 Custom domain `app.kitehub.me` (~10 min + DNS propagation)

1. Vercel dashboard → Project → Settings → Domains → "Add Domain".
2. Nhập `app.kitehub.me` → "Add".
3. Vercel hiển thị CNAME record cần add:

```
Type     Name    Value
CNAME    app     cname.vercel-dns.com
```

4. Cloudflare dashboard → `kitehub.me` zone → DNS → Records → "Add record":
   - Type: **CNAME**
   - Name: `app`
   - Target: `cname.vercel-dns.com`
   - Proxy status: **DNS only** (⚪ grey cloud) — initial setup, Vercel sẽ issue Let's Encrypt cert cần direct access. Sau khi cert active → switch sang Proxied 🟠 nếu muốn CDN double-layer.
   - TTL: Auto
   - Save.

5. Quay lại Vercel → click "Refresh" → wait DNS propagation (5-30 min).
6. Status sẽ thành `Valid Configuration` ✅ + Let's Encrypt cert auto-issued.

7. Verify:
```bash
dig CNAME app.kitehub.me +short
# Expected: cname.vercel-dns.com

curl -I https://app.kitehub.me/
# Expected: HTTP/2 200, Let's Encrypt cert
```

⚠️ **Phase 1 BETA placeholder:** Domain `app.kitehub.me` được Vercel serve directly Phase 1. Production traffic Phase 2+ có thể migrate sang AWS CloudFront hoặc Cloudflare Pages — defer decision.

### 2.7 GitHub integration cho preview deploys (~5 min — auto)

Vercel auto-integrate qua GitHub App permission §2.2:

- Mỗi PR mở → Vercel build preview deploy
- Comment bot post preview URL vào PR
- Merge to main → Vercel build production deploy
- Disable cho specific branches qua Settings → Git → Production Branch (default `main`)

⚠️ **Build rate limit observed (Wave 83 closure 2026-05-15):**
- Vercel Hobby tier cấp 1-2 concurrent builds
- Heavy PR velocity → builds queue → wait time tăng
- Evidence: Wave 83 closure CI build wait ~3-5 min queue + 3-5 min build = ~8-10 min total cycle
- Pro upgrade gives 3+ concurrent builds → reduces wait

### 2.8 Monitor Usage + Pro upgrade decision (~5 min/tháng)

1. Vercel dashboard → Usage tab.
2. Track monthly:
   - **Build Execution Time:** target < 5000 min/month (Hobby cap 6000)
   - **Bandwidth:** target < 80GB/month (Hobby cap 100GB)
   - **Deployments:** target < 80/day (Hobby cap 100)
3. Alert thresholds: bật email alert khi 80% threshold.

Pro upgrade decision matrix:

| Condition | Action |
|-----------|--------|
| Build minutes > 80% Hobby cap 3 tháng liên tiếp | Upgrade Pro |
| Daily build queue wait > 10 min consistently | Upgrade Pro |
| Bandwidth > 90GB/month | Upgrade Pro |
| Team grows > 1 dev needing Vercel access | Upgrade Pro ($20/user/month) |
| Cần Speed Insights / Web Analytics production tier | Upgrade Pro |
| Phase 1 BETA solo-dev, traffic thấp | Stay Hobby |

---

## 3. Verify-via

| Check | Command | Expected |
|-------|---------|----------|
| GitHub OAuth active | Vercel dashboard → Settings → Login Connections | GitHub linked |
| Project imported | Vercel dashboard → project list | `2026-kite-class-platform` hoặc custom name |
| Build successful | Latest deployment status | `Ready` (green) |
| Preview URL works | `curl -I https://<project>.vercel.app/` | HTTP/2 200, valid SSL |
| Env vars active | Vercel dashboard → Settings → Environment Variables | All §2.4 vars present |
| Custom domain CNAME | `dig CNAME app.kitehub.me +short` | `cname.vercel-dns.com` |
| Custom domain SSL | `curl -I https://app.kitehub.me/` | HTTP/2 200, Let's Encrypt cert |
| Preview deploys auto | Open PR mới → check Vercel bot comment | URL `https://<project>-<branch>-<hash>.vercel.app` |
| Production deploys auto | Merge to main → Deployments tab | New `Production` deploy triggered |
| Build minutes < cap | Usage tab | < 5000 min current month |

---

## 4. Troubleshooting

### 4.1 Build fails với "Cannot find module"

**Symptom:** Build log shows `Error: Cannot find module '<name>'`.

**Debug:**
1. Check `kitehub/kitehub-frontend/package.json` có dependency declared.
2. Check `pnpm-lock.yaml` updated với latest installs.
3. Verify Root Directory = `kitehub/kitehub-frontend` (Vercel scan đúng package.json).

**Fix:**
```bash
cd kitehub/kitehub-frontend
pnpm install
git add pnpm-lock.yaml package.json
git commit -m "fix(fe): sync pnpm-lock.yaml"
git push
```

Vercel auto-rebuild.

### 4.2 Custom domain status `Invalid Configuration`

**Symptom:** Vercel domain page hiển thị "Invalid Configuration" cho `app.kitehub.me`.

**Debug:**
1. `dig CNAME app.kitehub.me +short` → expect `cname.vercel-dns.com`.
2. Nếu empty: Cloudflare CNAME chưa save → re-check §2.6.
3. Nếu trả về Cloudflare proxy IP (104.x.x.x / 172.x.x.x): Proxy status = Proxied (orange) thay vì DNS only (grey) → Vercel cert handshake fail.

**Fix:**
1. Cloudflare DNS → CNAME `app` → toggle proxy status sang **DNS only** (grey cloud).
2. Wait 1-2 min DNS propagation.
3. Vercel dashboard → Refresh → status → `Valid Configuration` ✅.
4. Sau khi cert active có thể chuyển sang Proxied nếu cần CDN.

### 4.3 Env var không reflect trong runtime

**Symptom:** Code đọc `process.env.NEXT_PUBLIC_API_URL` ra `undefined`.

**Debug:**
1. Var có prefix `NEXT_PUBLIC_`? (mandatory cho client-side bundle)
2. Var được add cho ENVIRONMENT phù hợp (Production / Preview / Development)?
3. Đã redeploy SAU khi add var? Vercel KHÔNG hot-reload env vars — phải trigger rebuild.

**Fix:**
1. Vercel dashboard → Deployments → latest → "Redeploy" → uncheck "Use existing Build Cache" → Redeploy.
2. Hoặc push commit mới → auto-rebuild.

### 4.4 Build rate limit "Too Many Builds"

**Symptom:** `Error: Too Many Builds — please wait before triggering another deployment`.

**Cause:** Hobby tier 1-2 concurrent builds limit. Heavy PR cadence → queue overflow.

**Fix:**
1. Short-term: wait queue clear (5-10 min).
2. Reduce build triggers: disable preview deploys cho draft PRs qua Settings → Git → "Ignore Build Step" script:
   ```bash
   # vercel.json
   {
     "ignoreCommand": "git log -1 --pretty=%B | grep -q '\\[skip-vercel\\]' && exit 0 || exit 1"
   }
   ```
3. Long-term: upgrade Pro ($20/user/month) → 3+ concurrent builds + priority queue.

### 4.5 SSL cert pending > 1h

**Symptom:** Vercel domain status `Pending SSL Certificate` >1h.

**Debug:**
1. Domain CNAME validates (§4.2 check).
2. Cloudflare proxy status = DNS only (Let's Encrypt cần direct access port 80).
3. CAA records không block: `dig CAA kitehub.me +short` → empty hoặc include `0 issue "letsencrypt.org"`.

**Fix:**
1. Add CAA record nếu cần:
   ```
   Type: CAA, Name: @, Tag: issue, Value: "letsencrypt.org"
   ```
2. Vercel dashboard → Refresh.

---

## 5. Audit + cost guard

### 5.1 Cost monitoring

Vercel dashboard → Usage → tab monthly. Snapshot trong quality audit cadence (`output-review-mandate.md` §3 row "Cost Optimization").

Hobby (Free) tier:
- 100GB bandwidth/month
- 6000 build minutes/month
- 100 deployments/day
- 1 user / project

Pro tier ($20/user/month):
- 1TB bandwidth/month
- 12000+ build minutes/month
- Unlimited deployments
- Team collaboration
- Speed Insights production tier
- 3+ concurrent builds
- Priority support

### 5.2 Audit trail

Vercel keeps deployment logs 30 days Hobby, 90 days Pro. Cho long-term audit:
- Production deploys → reference qua git commit SHA + Vercel deployment URL trong ROADMAP entries
- PR preview deploys → link trong PR description

### 5.3 Build optimization

Reduce build minutes:
1. Use `pnpm install --frozen-lockfile` (skip resolution time).
2. Enable Vercel build cache: Settings → Git → "Enable Build Cache" (default ON).
3. Reduce bundle size: tree-shake, code-split, dynamic imports.
4. Skip preview builds cho docs-only PRs (§4.4 ignoreCommand).

---

## 6. References

- [Vercel Docs — Getting Started](https://vercel.com/docs)
- [Vercel Docs — Environment Variables](https://vercel.com/docs/projects/environment-variables)
- [Vercel Docs — Custom Domains](https://vercel.com/docs/projects/domains)
- [Vercel Docs — Pricing](https://vercel.com/pricing)
- [`documents/05-guides/deploy/vercel-production-setup.md`](../deploy/vercel-production-setup.md) — production env var seeding + custom domain binding
- [`documents/05-guides/deploy/fe-self-host-runbook.md`](../deploy/fe-self-host-runbook.md) — fallback self-host nếu Vercel exhausted
- [`documents/05-guides/deploy/cloudflare-setup.md`](../deploy/cloudflare-setup.md) — Cloudflare proxy rules cho `app.kitehub.me`
- [ADR-025](../../02-architecture/adr/ADR-025-aws-singapore-free-tier.md) — Phase 1 BETA hosting strategy

---

## 7. Log

- **2026-05-15:** Runbook created (Wave 84 Bucket C, GAP-394). Closes 3/3 missing account-prep runbooks cho Phase 1 BETA onboarding. Cross-link production setup tại `deploy/vercel-production-setup.md`. Notes Pro upgrade trigger evidence từ Wave 83 closure CI build rate limit observation 2026-05-15. Reviewer: @nguyenvankiet (solo-dev).
