# GAP-204: npm security backlog — 89 Dependabot alerts (8 CRITICAL blocked by next.js RSC compat)

**Status:** 🟡 PARTIAL — all CRITICAL + HIGH closed; 6 medium (axios + follow-redirects) pending auto-PR from Dependabot Stage E
**Priority:** 🟡 P2 (Security — no CRITICAL/HIGH exposure remains; medium alerts non-urgent, automated flow handles)
**Domain:** Frontend / Security
**Detected:** 2026-04-23 (after GAP-202 skill exposed Dependabot alerts that were disabled)
**Related PRs:** #455 (closed — 15.5.15 bump broke /pricing)
**Related Docs:**
- `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`
- `kitehub/kitehub-frontend/src/components/seo/JsonLd.tsx` (compat suspect)
- `kitehub/kitehub-frontend/src/app/(public)/pricing/page.tsx` (first break site)
- `kitehub/kitehub-frontend/src/app/(public)/blog/[slug]/page.tsx` (second break site)

## Current State (verified 2026-04-23)

| Piece | Path / Value | Status |
|-------|--------------|--------|
| Dependabot alerts enabled | repo Settings → Code security | ✅ Enabled via `gh api PUT .../vulnerability-alerts` |
| Automated security fixes | repo Settings | 🟡 DISABLED (prevent 89-PR flood during triage) |
| `next` installed (both frontend workspaces) | `15.1.6` (pnpm-lock.yaml resolved) | ❌ Affected by 8 CRITICAL + 16+ HIGH CVEs |
| 8 CRITICAL CVE ranges covering 15.1.6 | `GHSA-9qr9-h5gf-34mp`: `>= 15.1.0, < 15.1.9` — fix **15.1.9**<br>`GHSA-f82v-jwr5-mffw`: `>= 15.0.0, < 15.2.3` — fix **15.2.3** | ❌ REAL (not false-positive as initially triaged) |
| First clean `next` version (no security deprecation) | `15.3.9` (tested 2026-04-23) | 🟡 Available but build break |
| `/pricing` + `/blog/[slug]` RSC prerender | Tested 15.1.11, 15.3.9, 15.5.15 — all fail with `TypeError: a.map is not a function` at `Array.toJSON` in `next-server/app-page.runtime.prod.js` | ❌ Compat regression between next 15.1.6 → 15.1.7 |
| JsonLd component usage | Renders `<script dangerouslySetInnerHTML={__html: JSON.stringify(data)} />` in server components via `faqPageSchema(...)` / `blogPostingSchema(...)` | 🟡 RSC serialization invokes custom `Array.prototype.toJSON` added in next 15.1.7+ |

**Grep commands run:**
```bash
REPO="VictorAurelius/2026-Kite-Class-Platform"
# Count all alerts
gh api "repos/$REPO/dependabot/alerts?state=open&per_page=100" --jq 'length'   # → 89
# Get ALL vulnerable ranges per alert (fixes earlier shallow-query bug)
gh api "repos/$REPO/dependabot/alerts?state=open&severity=critical&per_page=100" \
  --jq '.[] | {ghsa, ranges: [.security_advisory.vulnerabilities[] | select(.package.name=="next") | .vulnerable_version_range], fix: .security_vulnerability.first_patched_version.identifier}'
# Confirm lockfile version
grep -A 2 "^  next:" kitehub/kitehub-frontend/pnpm-lock.yaml   # → 15.1.6
# Confirm compat break
cd kitehub/kitehub-frontend && pnpm build   # → prerender error on /pricing + /blog/[slug]
```

## Problem

After GAP-202 landed the Security factor in `/repo-status`, enabling Dependabot surfaced **89 npm alerts**. Initial triage (same session, 2026-04-23) **incorrectly** concluded the 8 CRITICAL alerts were false-positives by querying only `.vulnerabilities[0]` per advisory. GHSA advisories often have MULTIPLE `vulnerable_version_range` entries (one per affected major/minor line). Correct query using `.security_vulnerability.vulnerable_version_range` (which Dependabot computes as applicable to the resolved version) shows:

- **All 8 CRITICAL alerts are real** on next@15.1.6:
  - 4× `GHSA-9qr9-h5gf-34mp` (RCE via React flight protocol) — fix ≥ 15.1.9
  - 4× `GHSA-f82v-jwr5-mffw` (Authorization Bypass in Middleware) — fix ≥ 15.2.3
- Minimum version closing both families: **15.2.3**
- Minimum CLEAN version (no other security deprecation): **15.3.9**

Attempted bumps (15.1.11, 15.3.9, 15.5.15) all break `/pricing` + `/blog/[slug]` prerender with:
```
TypeError: a.map is not a function
  at Array.toJSON (next-server/app-page.runtime.prod.js)
  at JSON.stringify (<anonymous>)
```

The break is a behavioral change introduced between next 15.1.6 and 15.1.7 in how RSC serializes arrays during static generation. Next 15.1.7+ monkey-patches `Array.prototype.toJSON` in the app-page runtime; when the patched toJSON encounters something it expects to be an Array (calls `.map` on it) but isn't, it throws. Our `JsonLd` component pattern (`<script dangerouslySetInnerHTML={__html: JSON.stringify(faqPageSchema(PRICING_FAQS))} />`) is a plausible trigger site because the schema object contains `mainEntity: faqs.map(...)`.

Until this compat is fixed, **CRITICAL CVEs remain live on main** — any deploy of 15.1.6 carries:
- RCE via React flight protocol (public attack surface)
- Middleware authorization bypass (potential auth evasion)

## Context

- **Session flow:** GAP-202 skill (detection) merged → GAP-203 CVEs (Java) merged → enabled Dependabot (case study) → surfaced npm backlog → initial triage error → correct triage.
- **Skill validation:** `/repo-status` correctly reports BLACK when 8 CRITICAL detected — skill works as designed (GAP-202 AC satisfied).
- **Automation disabled:** `gh api DELETE repos/$REPO/automated-security-fixes` during triage to prevent second 89-PR flood.
- **Initial misanalysis:** First triage queried `.vulnerabilities[0].vulnerable_version_range` which only picks the first range. GHSA advisories with multiple affected lines (like GHSA-9qr9 affecting 13.x, 14.x canary, 15.0.x, 15.1.x, 15.2.x, 15.3.x, 15.4.x, 15.5.x, 16.x separately) need iteration through all ranges. Query fixed in this gap's `## Grep commands run` section.

## Evidence

**All 8 CRITICAL ranges (post-fix query):**

| Alert # | GHSA | Ranges covering 15.1.6 | Fix |
|---------|------|-------------------------|-----|
| 71, 57, 21, 1 | GHSA-f82v-jwr5-mffw | `>= 15.0.0, < 15.2.3` | 15.2.3 |
| 77, 63, 27, 7 | GHSA-9qr9-h5gf-34mp | `>= 15.1.0-canary.0, < 15.1.9` | 15.1.9 |

**next.js clean-version map (tested 2026-04-23):**
```
15.1.6  — installed (vulnerable)
15.1.7  — deprecated (security)
15.1.8  — deprecated
15.1.9  — deprecated (closes GHSA-9qr9 only)
15.1.10 — deprecated
15.1.11 — deprecated
15.2.0  — deprecated
15.2.3  — deprecated (minimum for both CRITICAL)
15.2.6  — deprecated
15.3.0  — deprecated
15.3.5  — deprecated
15.3.6  — deprecated
15.3.8  — deprecated
15.3.9  — CLEAN ✅ (first viable)
15.4.11 — CLEAN
15.5.15 — CLEAN
```

**Build break reproduction (all produce same error):**
```
TypeError: a.map is not a function
  at Array.toJSON (...next-server/app-page.runtime.prod.js:17:14944)
  at stringify (<anonymous>)
  at eB (...:17:26169)  # RSC payload encoder
Export encountered an error on /(public)/pricing/page: /pricing, exiting the build.
```

Break sites (by bump version):
- 15.1.11: `/pricing` only
- 15.3.9, 15.5.15: `/pricing` + `/blog/[slug]`

Common denominator: both pages use `<JsonLd data={...schema(...)} />` from server component.

## Proposed Fix (staged)

### Stage A — Document + stop-the-bleed (this PR)
1. Land THIS gap file with accurate analysis
2. Update ROADMAP — re-open Epic 5 with GAP-204 P0
3. Re-run `/repo-status` → expect BLACK (confirms skill's correct read)
4. **Do NOT dismiss CRITICAL** — they're real
5. Do NOT attempt bump in this PR — blocked by Stage B

### Stage B — Fix RSC compat (separate PR, high priority)
Investigation paths (pick one that works):

**Option B1: Refactor JsonLd to use string payload**
```tsx
// Before:
export function JsonLd({ data }: { data: Record<string, unknown> }) {
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }} />;
}
// Call: <JsonLd data={faqPageSchema(PRICING_FAQS)} />

// After: pre-stringify at call site (bypasses RSC array serialization on prop)
<script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(faqPageSchema(PRICING_FAQS)) }} />
```

**Option B2: Mark JsonLd as client component**
```tsx
'use client';
// JsonLd.tsx — RSC payload for client components doesn't invoke the Array.toJSON path
```

**Option B3: Check next.js release notes for the 15.1.7 RSC serialization change** — see if there's an opt-out flag.

**Option B4: Move JSON-LD generation to build-time via `generateStaticParams` + plain data object**.

Test each with `pnpm build` locally. Pick cleanest option.

### Stage C — Bump next + close CRITICAL (bundled with Stage B)
After Stage B fix:
1. Bump `next` and `@next/third-parties` to `15.3.9` (first clean version) in both workspaces
2. Regenerate pnpm-lock.yaml both frontends
3. `pnpm build` both frontends → verify `/pricing` + `/blog/[slug]` prerender
4. Push PR, merge after CI green
5. Verify 8 CRITICAL auto-close after Dependabot re-scans lockfile

### Stage D — Triage remaining 24 HIGH + 45 medium + 4 low (separate PRs)
Per-package with `security_vulnerability.first_patched_version` as canonical fix version:
- Re-check vite/picomatch/axios/follow-redirects/lodash/rollup/flatted/minimatch with same query pattern
- Bump direct deps first (vite, axios), transitives auto-resolve

### Stage E — Re-enable automated security fixes
`gh api PUT repos/$REPO/automated-security-fixes` after Stages A+B+C+D complete.

## Acceptance Criteria

### Stage A (this PR — immediate)
- [ ] GAP-204 file committed with accurate triage
- [ ] ROADMAP updated: Epic 5 + Current Status Snapshot
- [ ] Memory updated: triage pattern (query all ranges, not just first)

### Stage B (blocker for C)
- [ ] Root cause of `Array.toJSON` break identified (release notes or bisect)
- [ ] Workaround chosen + implemented
- [ ] Local `pnpm build` succeeds on next@15.3.9 for both workspaces
- [ ] Snapshot review of rendered `/pricing` + `/blog/[slug]` JSON-LD confirms same schema output as before

### Stage C
- [ ] `next@15.3.9` + `@next/third-parties@15.3.9` in both workspaces
- [ ] Post-merge: `gh api .../dependabot/alerts?state=open&severity=critical` returns 0
- [ ] `/repo-status` flips BLACK → RED (or lower if HIGH also drops)

### Stage D
- [ ] All remaining HIGH alerts fixed or dismissed with evidence

### Stage E
- [ ] `gh api repos/$REPO/automated-security-fixes` returns `{"enabled": true}`
- [ ] `/repo-status` → GREEN

## Related

- **Blocks:** Production deploy (CRITICAL RCE + auth bypass live)
- **Blocked by:** next.js RSC serialization investigation (Stage B)
- **Depends on:** GAP-202 (detection), GAP-203 (Java CVE fix pattern)
- **Case study:** `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`
- **Rule:** `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 — state-check COMPLETED
- **Rule:** `.claude/rules/meta-gap-priority.md` — Business-Logic-P0 (security correctness)
- **Lesson:** always query `security_vulnerability.vulnerable_version_range` (Dependabot-computed applicability) rather than iterating `.vulnerabilities[].vulnerable_version_range` only [0] — the latter misses alerts for packages affected by multiple version-line ranges.

## Log

- **2026-04-23** — Initial write-up. 89 alerts surfaced after enabling Dependabot via `gh api`. Initial triage mistakenly identified 8 CRITICAL as false-positive by querying only first range per advisory.
- **2026-04-23 (same session)** — Triage corrected. All 8 CRITICAL are REAL (GHSA-9qr9 fix 15.1.9, GHSA-f82v fix 15.2.3). Tested bumps 15.1.11, 15.3.9, 15.5.15 — all break `/pricing` + `/blog/[slug]` prerender with `Array.toJSON` error introduced in next 15.1.7. Stage B investigation required before fix can land. Automated security fixes DISABLED (prevent flood during triage). Alert 77 temporarily dismissed during test, then re-opened; no other dismissals executed.
- **2026-04-23** — Decision: do NOT dismiss CRITICAL (they're real exposures). Status stays 🔴 OPEN P0 until Stage B + C complete.
- **2026-04-24 Stage B+C** (PR #457 + #458): Root cause of `/pricing` + `/blog/[slug]` prerender break identified — **PRICING_FAQS was exported from `'use client'` module, array crossed RSC boundary when imported by server page**. Fix (two-part): (a) extract data to non-client module `pricing/faqs.ts`, (b) harden JsonLd to accept pre-stringified `json: string` prop. Bumped next 15.1.6 → 15.3.9 (first clean version). Removed stale `kiteclass-frontend/package-lock.json`. Result: 8 CRITICAL + 14 HIGH closed (89 → 55 alerts, BLACK → RED).
- **2026-04-24 Stage D** (PR #459 + #460): Bumped next 15.3.9 → 15.5.15 (build works thanks to Stage B fix), vite 8.0.0 → 8.0.10 (added as direct devDep after pnpm.overrides alone failed due to workspace peer resolution), `@vitejs/plugin-react` 5→6, vitest 4.0.18 → 4.1.5, picomatch override 4.0.3 → 4.0.4. Also added pnpm.overrides for flatted/minimatch/rollup. Dismissed 2 stale lodash advisories (HIGH #92 + medium #91) as `inaccurate` — advisory fix version 4.18.0 does not exist on npm (4.17.23 is latest stable). Result: 55 → 6 alerts (RED → YELLOW). All HIGH + CRITICAL closed.
- **2026-04-24 Stage E**: `gh api PUT repos/$REPO/automated-security-fixes` re-enabled. Remaining 6 medium (axios 4 + follow-redirects 2, transitive via axios) will be handled by Dependabot auto-PRs. axios bump 1.7.9 → 1.15.0+ will close both packages. Stage E active.
- **2026-04-24 Gotchas captured**:
  - Dependabot alert query: **always** use `security_vulnerability.vulnerable_version_range` (Dependabot-computed applicability), NOT `.vulnerabilities[0]` (first range only misses multi-line advisories). Caused initial false-positive triage this session.
  - Next.js 15.1.7+ regression: patches `Array.prototype.toJSON` in RSC runtime. Server-component import of array from `'use client'` module → `TypeError: a.map is not a function` at prerender. Fix pattern: keep data in non-client modules.
  - pnpm workspace override gotcha: `pnpm.overrides` can be silently ignored when parent's peer range permits older version. Solution: add override target as explicit direct dep (`pnpm add -D vite@latest`).
  - Lodash advisory 4.18.0 is stale/inaccurate: no such version exists (latest 4.17.23). Safe to dismiss as `inaccurate`.
