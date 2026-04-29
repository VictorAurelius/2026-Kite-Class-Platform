# Starter Kit Changelog

Quản lý theo Semantic Versioning: `MAJOR.MINOR.PATCH`
- **MAJOR**: Thay đổi breaking (đổi cấu trúc skill, bỏ script)
- **MINOR**: Thêm skill/script/rule mới
- **PATCH**: Fix bug, cải thiện nội dung existing

---

## [2.3.0] — 2026-04-29 — Q2 retro-sync, rules batch

### Added (9 rules from downstream meta-governance)

- `rules/rule-change-process.md` (v1.1.0) — semver governance for rule edits, paired with §6.5 Enforcement Parity Mandate
- `rules/output-review-mandate.md` (v1.2.0) — master mandate; every output type has documented review standard + process + evidence
- `rules/audit-to-gap-pipeline.md` (v1.0.0) — Issue → Gap Check → Gap File → Memory → Fix PR pipeline with state-check Step 2.5
- `rules/meta-gap-priority.md` (v1.0.0) — meta gaps ahead of feature gaps at same P-level (force-multiplier rule)
- `rules/gap-done-discipline.md` (v1.0.0) — banned-phrase list + PARTIAL exit ramp + override trailer for gap closure
- `rules/incident-to-rule-pipeline.md` (v1.0.0) — 5-stage pipeline turning user-flagged misses into permanent guards (Detect → Classify → Rule+Enforce → Self-Test → Retro Log)
- `rules/mcp-first-with-fallback.md` (v1.0.0) — tool selection: MCP-first, CLI fallback
- `rules/docs-folder-structure.md` (v1.0.0) — generic README-per-top-level rule for `documents/` tree

### Updated

- `rules/skill-conventions.md` (v1.0.0) — Anthropic-internal skill best-practices expanded with starter-kit version-management discipline + UI Audit Workflow section. Light-scrubbed to remove project-specific paths (replaced `kiteclass-frontend/` with generic `your-frontend/`). Frontmatter added (Priority + Version + Created + Last-Reviewed + Reviewer-Approver + Applies-to).

### Notes

- Source: downstream project Kite Platform (private), 2026-04-04 → 2026-04-29 meta-governance evolution. Triage report `retro-sync-triage-2026-04-29.md` identified 110 candidates; this batch ships 9 rules. Skills batches deferred to v2.4.0 (core+workflow) + v2.5.0 (quality+reference).
- All rules pass triage 4-question checklist (Generalize / Stable / No project paths / Battle-tested).
- Light scrubbing applied to 3 rules (`output-review-mandate`, `skill-conventions`, `meta-gap-priority`) — specific GAP IDs replaced with `<example: GAP-XXX>` placeholders.
- Rule 13 detector for `gap-done-discipline.md` (skill-side) deferred to v2.4.0 skills batch.

### Migration

For existing kit consumers: drop the new rules into `.claude/rules/`. No breaking changes (additive only). MINOR bump per `rules/skill-conventions.md §Starter-Kit Version Management`.

---

## [2.2.0] — 2026-04-04

### Added
- `docs/vibe-coding-guide/` — Bộ hướng dẫn Vibe Coding cho người trái ngành IT
  - 23 Jupyter notebooks (.ipynb) bằng tiếng Việt
  - 5 phần: Nền tảng → Starter Kit → Superpowers → Business Logic → Thực hành
  - Giải thích từ AI/LLM cơ bản đến quy trình phát triển sản phẩm chuyên nghiệp
  - Mỗi khái niệm kỹ thuật = ví von đời thường + bash cells chạy thử
  - Target: non-developer có thể vibe code ra sản phẩm chất lượng theo chuẩn kit

---

## [2.1.0] — 2026-04-02

### Added
- `skills/quality/ui-review/SKILL.md` — Portable UI audit skill template
  - Per-screen /128 scoring, strict rubric, before/after workflow
  - Fix verification protocol (step 0), scoring bands
  - Battle-tested through 17 runs + 40 PRs on Smart Quiz
- `scripts/capture-screenshots.ts` — Portable screenshot capture
  - Auto-detect dev server, start if needed
  - Labeled folders with per-page subfolders
  - Auto-update latest/ when using --label
  - Configurable PAGES, dark mode key, dev command
- `rules/skill-conventions.md` — Added "UI Audit Workflow" section
  - 8 key rules from real mistakes (auto-capture, strict scoring, etc.)

## [2.0.0] — 2026-03-30

### BREAKING — Root directory restructured
- **Scripts** moved: `*.sh` → `bin/` (init-project, upgrade-project, install-remote, contribute, publish, test-kit)
- **Docs** moved: `INSTALL.md`, `GETTING-STARTED.md`, `CONTRIBUTING.md`, `EXTRACTION-GUIDE.md` → `docs/`
- Root reduced from 14 files → 4 (README, VERSION, CHANGELOG, kit-manifest.yml)
- All cross-references updated in docs and README

### Migration
Replace `./init-project.sh` → `./bin/init-project.sh`, etc.
Replace `/tmp/kit/init-project.sh` → `/tmp/kit/bin/init-project.sh` in install commands.

## [1.5.0] — 2026-03-30

### Added
- Full sync with remote repo `VictorAurelius/claude-starter-kit`
- Pulled from remote: `INSTALL.md`, `GETTING-STARTED.md`, `CONTRIBUTING.md`, `EXTRACTION-GUIDE.md`, `install-remote.sh`, `publish.sh`, `kit-manifest.yml`, `.claude-plugin/` (plugin.json, marketplace.json), `skills/reference/ui-template-guide.md`
- Remote repo sync rules in `rules/skill-conventions.md`

### Changed
- VERSION/CHANGELOG/README fully synced (were diverged: remote=1.3.0, local=1.4.0)

## [1.4.0] — 2026-03-30

### Added
- `rules/skill-conventions.md` — How to write skills following Anthropic internal best practices
  - 9 principles: folder-based, progressive disclosure, trigger descriptions, gotchas > generic, etc.
  - Anthropic's 9 skill categories reference
  - Quick checklist for new skills
- `templates/skill-folder/` — Template for folder-based skills
  - `SKILL.md` template with trigger description, gotchas, skill contents
  - `reference/detail.md` template for on-demand loading

### Changed
- `skills/core/tdd-enforcement.md` — Slimmed from 120 lines generic methodology to gotchas template with `{project}` placeholders
- `skills/core/brainstorming-methodology.md` — Replaced with decision log template + when-mandatory rules
- `skills/core/systematic-debugging.md` — Replaced with common bugs template + debug workflow
- All core skills now follow "don't teach Claude what it knows" principle

## [1.3.1] — 2026-03-26

### Changed
- `skills/reference/ui-template-guide.md` — Add "Option B: Component Library as Design System"
  - When to choose component library over Figma (solo dev, rapid iteration, no designer)
  - Framework-agnostic library table (Svelte, React, Vue, CSS-only)
  - Business doc template for design tokens + component inventory
  - Migration rules (coexist → swap → delete old)
  - Additional anti-patterns: duplicate component systems, custom CSS over library
  - Pre-commit checks updated for multi-framework (*.svelte, *.tsx, *.vue)

## [1.3.0] — 2026-03-26

### Added
- `skills/reference/ui-template-guide.md` — Figma/template-first UI workflow, page checklist, anti-patterns
- `kit-manifest.yml` — File classification for safe upgrades (override-safe/new-only/merge-required)
- `VERSION` file — Tracks kit version for upgrade detection
- `install-remote.sh` — Install/upgrade from remote git repo
- `GETTING-STARTED.md`, `CONTRIBUTING.md` — Onboarding docs

### Changed
- `upgrade-project.sh` — Full plan-based upgrade flow (--plan → --apply → --force)

### Applied to Smart Quiz
- Added `ui-template-guide.md` skill
- Skipped `test-local.sh` (project-specific config)
- Skipped `development-workflow.md` (project-specific config)

## [1.2.0] — 2026-03-25

### Added
- `skills/continue.md` — Priority action skill for continuing work

## [1.1.1] — 2026-03-25

### Added
- `.claude-plugin/plugin.json` — Plugin metadata
- `install-remote.sh` — Remote install script
- `EXTRACTION-GUIDE.md`

### Changed
- `skills/reference/diagrams.md` — Graphviz requirement, render rules, verification

## [1.1.0] — 2026-03-25

### Added
- `templates/settings.local.json.template` — Claude Code permissions (bypass prompt)
- `templates/vscode-settings.json.template` — VS Code settings per language (Java, TS, Python, Go)
- `skills/reference/project-structure.md` — Folder structure best practice + anti-patterns + refactor checklist
- `skills/reference/ide-setup.md` — Claude permissions, VS Code config, test runners, MD warnings fix, common IDE warnings guide

- `skills/reference/diagrams.md` — PlantUML/Mermaid guide, tool comparison, minimum diagrams list
- `scripts/render-diagrams.sh` — Auto-render PlantUML + Mermaid, --check mode

### Changed
- `skills/_README-skills-index.md` — Added 3 new reference skills + 1 script

## [1.0.0] — 2026-03-24

### Initial Release
**Source:** Kite Class Platform (200+ PRs, 10+ waves)

#### Skills (9 files)
- core/brainstorming-methodology.md
- core/tdd-enforcement.md
- core/two-stage-code-review.md
- core/systematic-debugging.md
- core/task-breakdown-guide.md
- workflow/development-workflow.md
- quality/quality-audit.md
- reference/business-docs-3-layer.md
- reference/service-docs-standard.md

#### Scripts (3 files)
- check-ci.sh (with --status mode)
- test-local.sh (auto-detect, --quick mode)
- pre-commit-check.sh (extensible framework)

#### Seed Memories (4 files)
- Scripts not ad-hoc: dùng scripts cho mọi operation
- CI before scoring: đợi CI xong mới chấm điểm
- Self-test before push: test local trước push
- Business design first: docs trước code

#### Tools (3 scripts)
- init-project.sh: setup dự án mới
- upgrade-project.sh: import vào dự án đã có
- sync-to-kit.sh: đồng bộ từ dự án gốc

#### Templates (2 files)
- CLAUDE.md.template
- README.md.template
