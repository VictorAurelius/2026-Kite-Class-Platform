# README Content Discipline

**Priority:** 🟠 MANDATORY — root README is the project's first impression
**Version:** 1.0.0
**Created:** 2026-04-29
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Root `README.md` of the repository (and any equivalent visitor-first index page)

---

## 1. The Rule

> **Root README is for STABLE information only.** Anything that changes more often than ~quarterly belongs in `ROADMAP.md`, gap files, audit reports, or wave plans — NOT in the README.

Visitors land on README first. Stale numbers + score badges + PR references erode trust faster than missing information. If a metric needs updating after every wave, every audit, every merge — it does NOT belong in README.

Triggered by 2026-04-29 incident: README accumulated wave-specific scores (77/100, 110.5/128, +51%, 90 open gaps, "Wave GAP-236 most recent merge") that drifted within hours of multi-wave session. User flagged "trình bày UI dạng bảng cực kỳ xấu" + "không dùng các thông tin cần thay đổi nhiều".

---

## 2. ✅ Belongs in root README (STABLE)

Information that changes ≤ once per quarter or stays stable across product lifecycle:

| Category | Examples |
|----------|----------|
| **Product identity** | Name, logo (SVG), tagline, two-line elevator pitch |
| **Architecture** | 2-product structure, multi-tenancy approach (DB-level), shared infra, communication language |
| **Tech stack categories** | Backend / Frontend / Data / AI / Infra — ONE line per layer, NO version numbers (versions belong in `package.json` / `pom.xml`) |
| **Repository structure** | Top-level folder map (00-brd / 01-business / etc. — these are governance categories, very stable) |
| **Quick Start commands** | Stable scripts (`./scripts/up.sh`) — referenced by name, not by current behavior |
| **Documentation index** | Where-to-find map (Business / Architecture / Quality / etc. — folders are stable) |
| **Governance principles** | Solo-dev mode, wave strategy, audit-to-gap pipeline, Vietnamese-first — these don't change |
| **Main screens per service** | Owner dashboard / Teacher / Parent / Student / Admin — feature categories, NOT current screen scores |
| **Cross-cutting components** | Attendance roster / payment selector / invoice detail — feature names, NOT G2/G5/G7 IDs (those are dossier-internal) |
| **Authoritative links** | CLAUDE.md, ROADMAP.md, design system live URL — fixed paths |

---

## 3. ❌ Does NOT belong in root README (VOLATILE)

Anything below should live in `ROADMAP.md`, gap files, audit reports, or wave plans — README links to them, doesn't restate them:

| Category | Why excluded | Where it goes |
|----------|--------------|---------------|
| **Audit scores** (Quality 77/100, Security 85/100, etc.) | Refresh quarterly + after every wave | `documents/04-quality/audits/` reports + ROADMAP §Status Snapshot |
| **UI scores per kit** (108.4/128, 114/128, etc.) | Change every UI redesign | Kit-level READMEs + review reports |
| **Score deltas** (+51% lift, +9 since baseline) | Compare against baseline that itself shifts | Audit reports — not README |
| **PR numbers** (#600-#603, #668-#684) | Generated continuously | Commit log + ROADMAP entries |
| **Gap counts** ("90 open gaps") | Change every wave | `ROADMAP.md` §counts |
| **Specific version pins** (Spring Boot 3.5.14, Next.js 15.5) | Bump weekly via dependabot | `pom.xml` / `package.json` — README says "Spring Boot" / "Next.js" generically |
| **"Most recent merge"** (Wave GAP-236 4 agents 18min) | Stale within hours of next wave | ROADMAP §Status Snapshot top entry |
| **"Current wave"** (Meta-Governance 1) | Changes per wave | ROADMAP §Active wave queue + wave plan files |
| **Component IDs** (G2/G5/G7/G12) | Internal dossier identifiers | `dossier/04-component-gaps.md` |
| **Screen counts** ("76 screens · 138 files · 620 hrefs") | Change per kit ship | Wave plan + review report |
| **Wall-clock metrics** ("~110 min", "~3.5M tokens") | Per-wave data | `wave-history.jsonl` |
| **"Last Updated: YYYY-MM-DD"** | Drifts; better tracked by git log + per-doc README freshness check | Use `**Last-Reviewed:** YYYY-MM-DD` only on rule files (per rule-change-process.md frontmatter) |
| **Star badges** (![CI passing], ![77/100 C+]) | Decay fast; misleading when stale | Link to live CI / audit reports instead |
| **Persona table with ⭐ markers** | UI design state, not project identity | Design system kit READMEs |

---

## 4. Decision rule for borderline content

When unsure if something belongs in root README, ask:

1. **Will this change in the next 90 days?** If yes → NOT in README. Link to where it lives.
2. **Could a new contributor in 12 months still trust this?** If no → NOT in README.
3. **Does updating this require touching README every PR/wave?** If yes → NOT in README. Move to ROADMAP / gap / audit.
4. **Is this a category vs an instance?** Categories (Backend, Architecture, Governance) → README. Instances (specific score, PR number, wave name) → linked artifact.

If answers are mixed, default to MOVE OUT of README.

---

## 5. Format conventions

- **Logo:** SVG only (`<img src="assets/...svg">`), NOT pixel art / ASCII art / monospace block. Pixel art was tried 2026-04-28 and looked dated immediately.
- **Tables:** for STRUCTURAL comparison only (e.g. the project vs the project roles). NEVER for score / state tables (those drift).
- **Lists over tables for screens:** "Owner dashboard / Teacher / Parent / ..." reads better than "| Kit | Score | Persona |" rows. Lists scale gracefully when adding new screens; tables force every new screen to need a new column or row of metadata.
- **Versions:** mention stack categories (Spring Boot, Next.js, PostgreSQL) NOT exact versions. Versions live in `pom.xml` / `package.json`.
- **Links to dynamic state:** EXPLICIT pointer at end of each section: "For dynamic state, see ROADMAP.md".
- **No emoji clutter:** functional emoji only (📖 docs link, 🇻🇳 language flag). No decorative ⭐ / 🚀 / 🎨 in section headers (those are review-doc patterns, not README).

---

## 6. Out of scope

This rule covers ROOT README only. Other READMEs have different content rules:

| README | Content rule |
|--------|--------------|
| `<backend-product>/README.md` / `<tenant-product>/README.md` | Service-level setup + scripts (versions OK; service-specific) |
| `documents/02-architecture/design-system/ui_kits/README.md` | Kit catalog with scores (volatile but kit-internal — readers expect freshness) |
| `documents/04-quality/gaps/ROADMAP.md` | Status snapshot + queue (volatile by design) |
| Per-kit `README.md` | Score self-report + AC checklist (volatile, kit-internal) |
| Skill SKILL.md / rule.md | Per `skill-conventions.md` / `rule-change-process.md` frontmatter |

Root README is ONLY for visitor first impression. Internal docs follow their own rules.

---

## 7. Enforcement

- **Pre-merge PR review:** any PR touching root `README.md` flagged for "stable content?" check. Reviewer rejects if introducing volatile metrics.
- **CI script (Phase 2 deferred):** `scripts/check-readme-stability.sh` — grep for known-volatile patterns (score numbers, PR refs `#\d+`, "most recent merge", explicit version pins matching `\d+\.\d+\.\d+`) — exit 1 if found in root README. Tracked in §8 Open Items below.
- **Quarterly README audit:** verify §3 Anti-list still accurate; check root README hasn't accumulated drift.
- **Hook (Phase 3 deferred):** `audit-gate.py` AUDIT_RULES `readme-stability-required` rule — block PR adding volatile patterns to root README without reviewer override trailer.

### Override mechanism

Genuine exception (e.g. major launch announcement requiring volatile claim temporarily):

```
git commit -m "...
README_VOLATILE_OK: <reason + planned removal date>"
```

Trailer logged in quarterly retro.

---

## 8. Open Items / Follow-ups

- [ ] **Phase 2 detection** — `scripts/check-readme-stability.sh` (regex grep + CI job). Track in follow-up gap when Phase 1 stabilizes (~7 days from rule landing per `incident-to-rule-pipeline.md` premature-rule guard).
- [ ] **Phase 3 hook enforcement** — `audit-gate.py` AUDIT_RULES rule. Same timing as Phase 2.

---

## 9. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| `[![Quality](https://img.shields.io/badge/Quality-77%2F100_C%2B-yellow.svg)](.)` | Link to `documents/04-quality/` audit reports — let reader find current score |
| Pixel art `▄█▄ ██ ██` ASCII logo block | `<img src="assets/kite-mark.svg" width="96">` |
| "**90 open gaps** across 12 waves" | "Gap-driven roadmap — see ROADMAP.md" |
| `[Wave GAP-236 — 4 parallel agents, ~18 min, 33 pages]` | "Wave-pack methodology — see ROADMAP §Active wave queue" |
| Persona × kit score matrix table | Bullet list of main screens per service |
| "Spring Boot 3.5.14 · Next.js 15.5.15" | "Spring Boot · Next.js" — versions in `pom.xml` / `package.json` |
| **Last Updated: 2026-04-28** at bottom | Omit; use `**Last-Reviewed:**` on rule files only |
| README as a status dashboard | README as a stable identity card; link to dashboard |

---

## 10. Relationship to other rules

- **`output-review-mandate.md`** §3 — root README is an output type; this rule is its standard. Adds row coverage.
- **`rule-change-process.md`** §6.5 Enforcement Parity — this rule ships with §7 enforcement plan + §8 follow-up gaps for full automation.
- **`incident-to-rule-pipeline.md`** — this rule is direct output of 2026-04-29 incident "README quá xấu — quá nhiều thông tin cụ thể". Detect → Classify → Rule (this) → Self-test (root README rewrite passes §3 anti-list) → Retro Log (this §10 + Log).
- **`docs-folder-structure.md`** — generic README rule for `documents/` subfolders. This rule SPECIALIZES for root README; subfolder READMEs follow docs-folder-structure rules.
- **`skill-conventions.md`** — README freshness CI script (`scripts/check-readme-freshness.sh`) covers `**/README.md` Last-Updated date check; Phase 2 of this rule extends with stability check (different concern).

---

## 11. Self-test

Run this on root README:

```bash
# Should NOT match anything in well-disciplined README
grep -E '\d+/\d+|#[0-9]+|GAP-[0-9]+|Wave [0-9]+|\d+\.\d+\.\d+' README.md
```

If matches found:
- Score numbers (`77/100`, `110.5/128`) → MOVE to audit reports + link
- PR refs (`#600-#603`) → MOVE to ROADMAP + link
- Gap refs in main body (`GAP-236`) → MOVE to ROADMAP + link (OK in §Governance link target)
- Wave numbers (`Wave 12`) → MOVE to ROADMAP + link
- Version pins (`3.5.14`, `15.5.15`) → MOVE to `pom.xml` / `package.json` + use generic name

Self-test on current root README (post-rewrite 2026-04-29) should yield ≤ 5 matches (only governance link targets `GAP-264..267` if mentioned, or rule-cited version numbers like Java 17 LTS which is itself stable).

---

## 12. Log

- **2026-04-29** (v1.0.0): Rule created as direct output of `incident-to-rule-pipeline.md` 5-stage applied to user feedback "readme vẫn quá xấu" with 4 specific complaints: (1) too much project-specific info, (2) ugly UI table, (3) pixel art logo, (4) volatile metrics. Same-PR enforcement: root README rewritten to pass §3 anti-list + use SVG logo + list-not-table for screens + remove all volatile metrics. Phase 2 (CI script) + Phase 3 (hook) deferred per §8 Open Items, premature-rule guard ≥7 days. Reviewer: @nguyenvankiet (solo-dev MINOR — new constraint, no loosening; new rule with built-in enforcement via same-PR README rewrite + reviewer manual until Phase 2 lands).
