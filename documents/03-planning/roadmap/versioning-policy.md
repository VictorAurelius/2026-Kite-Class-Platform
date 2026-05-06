---
title: Versioning Policy — Semver convention + release process
status: active
created: 2026-05-06
updated: 2026-05-06
applies_to: [Kite Platform — KiteHub + KiteClass]
---

# Versioning Policy — Kite Platform

**Trạng thái:** ACTIVE — chốt 2026-05-06.
**Phương pháp:** Semantic Versioning 2.0 (semver) — `MAJOR.MINOR.PATCH[-PRERELEASE]`.

---

## 1. Why semver

Kite Platform là multi-tenant SaaS với:
- API consumers (KiteHub ↔ KiteClass internal + future external partners)
- Frontend bundle (kiteclass-frontend + kitehub-frontend)
- Database schema (Flyway V57+)
- Multi-persona expansion (P1+P2 → P3 → K-12 → enterprise)

Semver trade-offs khác versioning models:
- **CalVer** (2026.05.0) — date-based; tốt cho continuous deploy, không express breaking changes
- **Persona-tier** (Release 1/2/3) — milestone naming; không track granular fixes
- **Hybrid** — semver cho product + CalVer cho build IDs (recommended)

→ **Adopt semver for product version**, augmented với git tag + build ID.

---

## 2. Version format

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
  0.9.0-beta.1       — Phase 1 BETA first iteration
  0.9.0-beta.5       — Phase 1 BETA fifth iteration (bug fixes)
  1.0.0-rc.1         — Release Lần 1 production candidate (pre-launch)
  1.0.0              — Release Lần 1 PRODUCTION (public paid launch)
  1.0.1              — Patch (bug fix on prod)
  1.1.0              — Minor (new feature within P1+P2)
  2.0.0              — Release 2 (P3 medium-center add)
  3.0.0              — Release 3 (K-12 P5 add — major compliance class change)
```

---

## 3. Bump rules

### MAJOR (1→2→3)

Bump when SHIPPING:
- **New persona tier expansion** — P3 add, K-12 add, enterprise add
- **New compliance class** — counsel sign-off + MPS A05 registration (K-12)
- **Breaking API change** consumed by external partners
- **Backwards-incompatible DB schema** requiring data migration > simple Flyway
- **Major architecture shift** — service split, cross-region deployment, etc.

### MINOR (1.0→1.1→1.2)

Bump when SHIPPING:
- **New feature within current persona scope** — vd: AI Branding marketplace, public API/SDK, mobile PWA
- **New optional UI surface** — settings panel mới, dashboard widget mới
- **Backwards-compatible API endpoints** — new endpoints, response field additions
- **Significant performance/UX improvement** — measured metric improvement

### PATCH (1.0.0→1.0.1)

Bump when SHIPPING:
- **Bug fix** — hotfix, regression fix
- **Security patch** — CVE fix, vulnerability remediation
- **Doc-only fix** — typos, broken links, clarifications
- **Internal refactor** — no user-facing impact (rename methods, dep upgrades)

### PRE-RELEASE tags

| Tag | Purpose | Example |
|---|---|---|
| `-alpha` | Internal dev testing only | 1.0.0-alpha.1 |
| `-beta` | External invite-only beta | 0.9.0-beta.3 |
| `-rc` | Release candidate, pre-public-launch | 1.0.0-rc.1 |

---

## 4. Release mapping (current + planned)

| Version | Release name | Phase | Persona scope | Trigger condition | Estimate |
|---|---|---|---|---|---|
| **0.x.x** | Pre-launch dev | Wave 1-30 | None (internal) | — | Current state 2026-05-06 |
| **0.9.0-beta** | Phase 1 BETA launch | Phase 1 BETA | P1+P2 invite-only, free | 8 Track 2 ports + observability + 10-20 invite tenants | Week 9-12 |
| **0.9.x-beta** | Beta iterations | Phase 1 BETA | P1+P2 invite-only | Bug fixes + UX from beta feedback | Week 9-12 |
| **1.0.0-rc** | Release candidate | Phase 1.5 PAID | P1+P2 candidate | All 5 BLOCKING + 4 STRONGLY recommend gaps closed; QA pass | Week 13-17 |
| **🎯 1.0.0** | **Release Lần 1 PRODUCTION** | Phase 1.5 PAID | P1+P2 public paid | Public signup + payment processor live + Quality audit /100 ≥85 | **Week 13-18** |
| **1.0.x** | Patches | post-launch | P1+P2 | Bug fixes, security patches, hotfixes | Continuous |
| **1.1.0+** | Minor adds | post-launch | P1+P2 | AI Branding evolution (template/multi-tier/multi-brand), Dev API, Marketplace | Week 19+ rolling |
| **2.0.0** | Release 2 | Phase 2 | + P3 medium-center | P3 commission + scheduling + RBAC + financial reports complete | Week 17-24 |
| **2.x.x** | P3 patches/minors | Phase 2 maintenance | P3 ecosystem | Feature additions within P3 scope | Continuous |
| **3.0.0** | Release 3 | Phase 3 | + K-12 P5 | Counsel sign-off + DPO + MPS A05 + DPIA + K-12 LEGAL trio prod-grade | Week 25-34 |
| **3.x.x** | K-12 Stage progression | Phase 3 maintenance | K-12 P5 | 5-stage K-12 program per P5 review (Stage 1 → 2 → 3 → 4 → 5) | Q3 2026 → Q3 2027 |
| **4.0.0+** | Future expansion | TBD | Enterprise / international / mobile / advanced | Vision-only, not concrete plan | Post-2027 |

---

## 5. Sub-version threads

Multiple feature threads progress trong cùng major version:

### 5.1 Track 2 (production rebuild theo UI kit)
- v0.9.x: 8 ports Phase 1 BETA (Track 2 Phase 2 partial)
- v1.0.x: refinements
- v1.1.x: completions (modals/dialogs catalog full)
- v2.0.x: P3 admin port (kitehub-admin)
- v3.0.x: K-12 ports (kiteclass-parent / kiteclass-student / kitehub platform admin)

### 5.2 AI Branding evolution
- v1.0: minimum (logo + color theme picker)
- v1.1: template-based image composition (GAP-004)
- v1.2: multi-tier image generation (GAP-003)
- v1.3: multi-brand per tenant (GAP-027)
- v2.x: marketplace (GAP-045)
- v3.x: scheduled rebrand academic year refresh (GAP-072)

### 5.3 K-12 Stage program (per P5 review existing 5-stage program)
- v3.0: Stage 1 GA (Q3 2026)
- v3.1: Stage 2 (Q4 2026)
- v3.2: Stage 3 (Q1 2027)
- v3.3: Stage 4 (Q2 2027)
- v3.4: Stage 5 = Full K-12 GA (Q3 2027)

### 5.4 PDPL maturity
- v1.0: PDPL Phase 1+2 ("v1 pending counsel" + LocalStorage consent + DSAR manual + DPIA docs)
- v2.0: counsel-reviewed full TOS/Privacy/Cookie/AUP/Refund (post-counsel engagement)
- v3.0: K-12 LEGAL trio production-grade + DPO + MPS A05

---

## 6. Release process

Per release tag:

### 6.1 Pre-release checklist (alpha/beta/rc)

- [ ] Wave-pack closure PR shipped
- [ ] All Phase milestones met (per `release-X-plan.md` deliverables)
- [ ] Quality audit /100 score ≥ threshold (BETA: ≥80; RC: ≥85)
- [ ] No P0 incidents trong 1 tuần (BETA), 4 tuần (RC)
- [ ] Changelog drafted: `documents/03-planning/changelogs/CHANGELOG-vX.Y.Z.md`
- [ ] User-facing docs updated (release notes, FAQ updates)

### 6.2 Production release checklist

- [ ] All pre-release checklist done
- [ ] Counsel-reviewed legal docs (post-Phase-2)
- [ ] Pen-test report committed
- [ ] Production deploy runbook executed
- [ ] Monitoring dashboards active + alerts wired
- [ ] Rollback plan documented
- [ ] Tag release: `git tag -s vX.Y.Z -m "Release vX.Y.Z — <summary>"`
- [ ] Push tag: `git push origin vX.Y.Z`
- [ ] GitHub Release created với changelog + binary attachments (if any)
- [ ] Public announcement (if applicable)

### 6.3 Patch release checklist (1.0.x)

- [ ] Hotfix branch from `main` (or release tag if maintaining old branch)
- [ ] Test coverage on regression
- [ ] Quick smoke test
- [ ] Tag patch: `vX.Y.Z+1`
- [ ] Skip changelog if doc-only; update if user-facing

---

## 7. Sub-component versioning

Major sub-components versioned independently khi cần:

### 7.1 API versioning (URL-based)
- `/api/v1/...` — current REST API; locked once consumed externally
- `/api/v2/...` — future breaking API changes; v1 maintained N releases parallel

### 7.2 Database migration (Flyway)
- `V[1-9][0-9]+__description.sql` — sequential, never renamed
- Current state: V57+ (Wave 24)
- Versioning thread separate từ product version

### 7.3 Frontend bundle (Next.js + React)
- `package.json` version sync với product version
- `pnpm-lock.yaml` updated per dep bump
- Bundle hash auto-generated per build

### 7.4 Shared lib (`@kite/shared-ui`)
- Internal workspace; sync với product version per `package.json`
- Future: independent semver if open-sourced (per GAP-351)

### 7.5 Docker images
- Tag pattern: `kiteclass-core:vX.Y.Z` + `kiteclass-core:latest`
- Built from git tag in CI/CD

---

## 8. Changelog convention

`documents/03-planning/changelogs/CHANGELOG-vX.Y.Z.md`:

```markdown
# Release vX.Y.Z — <name> (YYYY-MM-DD)

## 🎯 Highlights
- <user-facing top 3 wins>

## ✨ Added (new features)
- New endpoint X for Y purpose
- ...

## 🔄 Changed
- ...

## 🐛 Fixed
- Bug Z fixed (closes GAP-XXX)
- ...

## 🔒 Security
- CVE-XXXX patched

## 📚 Docs
- ...

## ⚠️ Breaking changes (MAJOR only)
- API X removed; use Y instead
- ...

## 🙏 Acknowledgements
- Beta tester contributions

## Migration guide (MAJOR only)
- Steps to upgrade from vX-1
```

---

## 9. Commit message convention (extend conventional commits)

```
type(scope): description [version-bump]

type:
  feat     — new feature (MINOR bump candidate)
  fix      — bug fix (PATCH bump)
  docs     — doc-only (PATCH bump if user-facing)
  refactor — internal (no bump)
  test     — test-only (no bump)
  chore    — meta (no bump)
  perf     — perf improvement (MINOR if measured)
  security — security patch (PATCH bump priority)
  breaking — breaking change (MAJOR bump)

scope examples:
  wave-25, gap-353b, kc-frontend, ki-core, k12, parent-portal, ai-branding,
  release-1, release-2, ...

[version-bump] tag (optional, hint to release manager):
  [patch] [minor] [major] [no-bump]
```

Examples:
```
feat(wave-25-A): GAP-353b — server consent API + audit-log link [minor]
fix(kc-frontend): GAP-XXX — payment redirect timeout [patch]
breaking(api-v2): remove /api/v1/branding endpoints [major]
chore(rules): bump versioning-policy v1.0 → v1.1 [no-bump]
```

---

## 10. Hotfix procedure

Khi P0 incident on production:

1. Branch from current production tag: `git checkout -b hotfix/critical-X v1.0.0`
2. Apply minimal fix
3. Verify mvn + pnpm test pass
4. Tag patch: `git tag v1.0.1`
5. Deploy hotfix to prod
6. Backport fix to `main`
7. Post-incident review trong 48h (per `output-review-mandate.md`)

---

## 11. Pre-release distribution

Beta tenants nhận trải nghiệm:
- Beta dashboard banner: "Bạn đang dùng Kite v0.9.0-beta — Beta period free, feedback welcome"
- Footer build info: "v0.9.0-beta+build.20260601.143" (with timestamp + commit hash)
- Beta-only feature flags (vd: experimental AI Branding wizard) controllable per tenant
- Beta tenant ID prefix `BETA-` cho easy support routing

Production tenants:
- Footer: "v1.0.0" (no -beta suffix)
- No experimental flags by default

---

## 12. Versioning enforcement

### CI checks
- PR title MUST match conventional commits format (per `.github/workflows/`)
- Breaking change PR requires `[major]` tag in title + changelog draft attached
- Release PR creates git tag + GitHub Release automatically

### Reviewer checklist
- Per release: confirm version bump correct per §3 rules
- Confirm changelog complete + user-readable
- Confirm migration guide included for MAJOR

### Audit trail
- All releases tracked in `documents/03-planning/changelogs/CHANGELOG-vX.Y.Z.md`
- Wave-history.jsonl entries cross-link release tag

---

## 13. Subsequent release plan files

Per Option β chốt 2026-05-06: detailed plans cho Release 2/3/4+ DEFERRED đến gần Phase 1.5 PAID launch khi có data từ beta. Hiện tại chỉ have:

- ✅ `release-1-plan-2026.md` — full detail (Phase 1 BETA + Phase 1.5 PAID)
- ⏳ `release-2-plan-2026.md` — TBD (Phase 2 P3 add, viết khi Phase 1 stable)
- ⏳ `release-3-plan-2026.md` — TBD (Phase 3 K-12 + Stage 1-5 program, viết post-counsel engagement)
- ⏳ `release-4plus-vision.md` — TBD (post-K-12 expansion vision)

Lý do defer detail:
- Solo dev cần version policy NGAY để consistent commit messages + changelog từ Wave 25 trở đi
- Release 2/3 detail bây giờ chưa có data từ beta → over-planning
- Release 4+ vision quá xa, premature

Lock long-term direction trong this versioning policy + Release 1 plan §13/14 (Limitations + Positioning) — đủ signal direction mà không premature commit.

---

## 14. Open items

- [ ] Set up CI workflow for tag-based release automation
- [ ] Create `documents/03-planning/changelogs/` folder + first changelog file
- [ ] Add release-tag protection rule in GitHub branch settings
- [ ] Document GitHub Release template
- [ ] Set up dependabot label cho semver hint (`automerge:patch`, `manual:major`)

---

## 15. Log

- **2026-05-06:** Versioning policy created. User chốt Option β (versioning policy now, Release 2/3/4+ plans later). Reference Release Lần 1 Plan adopt semver mapping. Recommend `v0.9.0-beta` cho Phase 1 BETA → `v1.0.0` cho Phase 1.5 PAID public launch.
