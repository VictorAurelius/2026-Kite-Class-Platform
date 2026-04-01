# Skill Conventions — Based on Anthropic Internal Best Practices

How to write Agent Skills that maximize quality and minimize token waste.

## Core Principle

> "Context engineering > prompt engineering"
> — Every token in a skill competes with conversation history and reasoning space.

## 1. Skills Are Folders, Not Files

A skill is a **knowledge package**, not a markdown file.

```
my-skill/
├── SKILL.md              ← Entry point (<100 lines)
├── reference/            ← Loaded only when needed (zero tokens until read)
│   ├── scoring-guide.md
│   └── api-reference.md
├── scripts/              ← Executable helpers
│   └── check.sh
├── data/                 ← Persistent state (logs, previous results)
│   └── last-run.json
└── assets/               ← Templates, examples
    └── report-template.md
```

**Exception**: Very simple skills (commit, single-step commands) can be single .md files with frontmatter.

## 2. Progressive Disclosure (3 Layers)

| Layer | When Loaded | Token Cost | Content |
|-------|------------|------------|---------|
| **Metadata** | Always (on startup) | ~100 tokens/skill | `name` + `description` in frontmatter |
| **SKILL.md body** | When skill activated | 200-500 tokens | Process steps, gotchas, pointers to references |
| **Reference files** | When Claude decides to read | Zero until needed | Detailed tables, scoring criteria, API docs |

**Rule**: SKILL.md body should be <100 lines. Move tables, examples, detailed criteria to `reference/`.

## 3. Description = Trigger Condition (For Model, Not Human)

The `description` field is what Claude scans to decide "does this skill match?" — write it as trigger conditions.

```yaml
# BAD — human summary, no trigger context
description: "Run quality audit with 11 categories"

# GOOD — trigger conditions for the model
description: "Use when user says 'audit', 'quality check', 'ready to merge?',
  'kiểm tra chất lượng', or before merging to main. Runs 11-category checks
  with 100-point scoring."
```

**Tips**:
- Include exact phrases users say (in all languages they use)
- Include the situation/context when skill applies
- Adding examples improves activation rate from 72% to 90%

## 4. Don't Teach What Claude Already Knows

Claude knows TDD, debugging methodology, brainstorming techniques, Git workflow, etc. **Don't waste tokens repeating this.**

```
# BAD — 100+ lines explaining RED-GREEN-REFACTOR
## RED: Write Failing Test First (5-10 min)
1. Write test BEFORE any production code
2. Test must FAIL when first run...
[generic examples in Java, TypeScript...]

# GOOD — 30 lines of project-specific gotchas
## Smart Quiz Test Gotchas
1. Always mock speechSynthesis.cancel — tests crash without it
2. Guard window access — SSR has no window
3. Quiz direction tests — test all 3: ja-vi, vi-ja, vi-romaji
```

**What TO include**:
- Project-specific gotchas (failure points Claude can't know from training)
- Decision log (non-obvious choices already made)
- Internal APIs/libraries Claude doesn't know about

## 5. Build a Gotchas Section

The highest-value section in any skill. Update continuously.

```markdown
## Gotchas
- Dev server MUST run on port 5174 before capture
- Screenshots use page.reload() — increase timeout if theme wrong
- Vocabulary page renders blank without lesson data — not a real bug
```

## 6. Use File System for Context Engineering

Tell Claude what files exist in the skill folder — it will read them at the right time.

```markdown
## Skill Contents
- `reference/scoring-guide.md` — Detailed scoring tables (read when scoring)
- `scripts/capture.sh` — One-command screenshot capture
- `data/last-run.json` — Previous scores for comparison
```

## 7. Don't Railroad Claude

Skills get reused across many situations. Give information, not rigid step-by-step scripts.

```
# BAD — too rigid, breaks on edge cases
Step 1: Run exactly this command
Step 2: Read exactly this file
Step 3: Output exactly this format

# GOOD — goal + context, Claude adapts
Capture screenshots, then score 4 dimensions.
Consult reference/scoring-guide.md for criteria.
Save report to documents/04-quality/ui-review-latest.md.
```

## 8. Store Data in Skills

Skills can persist state between runs:

- **Append-only log**: `data/runs.log` — one line per run with date + score
- **Previous results**: `data/last-report.md` — compare with current
- **Config**: `data/config.json` — user preferences for this skill

## 9. Anthropic's 9 Skill Categories

| # | Category | Example |
|---|----------|---------|
| 1 | **Library & API Reference** | Internal SDK docs, design system reference |
| 2 | **Product Verification** | Playwright tests, screenshot capture, assertions |
| 3 | **Data Fetching & Analysis** | Dashboard queries, monitoring checks |
| 4 | **Business Process** | Standup posts, weekly recaps, status updates |
| 5 | **Code Scaffolding** | Generate boilerplate for specific patterns |
| 6 | **Code Quality & Review** | Linting, adversarial review, style enforcement |
| 7 | **CI/CD & Deployment** | PR babysitting, deploy automation |
| 8 | **Runbooks** | Incident investigation, alert triage |
| 9 | **Infrastructure Ops** | Orphan cleanup, maintenance procedures |

## Quick Checklist for New Skills

- [ ] Is this a folder (if complex) or single file (if simple)?
- [ ] Description written as trigger condition with example phrases?
- [ ] SKILL.md body <100 lines?
- [ ] Detailed content in `reference/` (not in body)?
- [ ] Gotchas section with project-specific failure points?
- [ ] NOT teaching Claude generic knowledge it already has?
- [ ] Tells Claude what files are in the skill folder?

## Starter-Kit Version Management

When modifying the starter-kit, follow this checklist **every time**:

### Before making changes
- [ ] Read `VERSION` file — know current version
- [ ] Read top of `CHANGELOG.md` — verify VERSION matches latest changelog entry
- [ ] If mismatch, fix sync FIRST before making new changes

### After making changes
- [ ] Bump `VERSION` file (MAJOR.MINOR.PATCH per semver rules below)
- [ ] Add entry to top of `CHANGELOG.md` with date and changes
- [ ] Update version in `README.md` header
- [ ] **All 3 files must show the same version number**

### Semver rules for starter-kit
| Change type | Bump | Example |
|-------------|------|---------|
| Remove/restructure existing skill or script | MAJOR | 2.0.0 |
| Add new skill, script, rule, or template | MINOR | 1.4.0 |
| Fix content, improve wording, update existing | PATCH | 1.4.1 |

### Gotchas
- `VERSION` file contains ONLY the version number (no other text)
- `README.md` version is in the header line: `**Version:** X.Y.Z`
- `CHANGELOG.md` entries are newest-first (top = latest)
- Never skip versions — increment from current, not from memory

## Remote Repo Sync

The starter-kit has a canonical remote repo: `github.com/VictorAurelius/claude-starter-kit` (branch: `main`).

Projects that use the kit (like Smart Quiz) have a **local copy** at `.claude/starter-kit/`.

### Before modifying kit in any project

```
1. Check remote VERSION:
   gh api repos/VictorAurelius/claude-starter-kit/contents/VERSION --jq '.content' | base64 -d

2. Check local VERSION:
   cat .claude/starter-kit/VERSION

3. If remote > local → update local first (pull remote changes)
4. If local > remote → push local changes to remote after this PR
5. If versions match → proceed with changes
```

### After modifying kit in a project

```
1. Make changes + bump version in project repo
2. Create PR on remote repo with same changes:
   - Clone remote: git clone git@github.com:VictorAurelius/claude-starter-kit.git /tmp/kit
   - Apply changes, bump VERSION/CHANGELOG/README
   - Push + create PR on remote repo
3. Both repos must end at same version
```

### Gotchas
- Remote repo has extra files (`docs/INSTALL.md`, `docs/GETTING-STARTED.md`, `.claude-plugin/`, `bin/install-remote.sh`) not in project copies — these are distribution files, don't delete them when syncing
- Project copies may have project-specific customizations in `core/` skills — don't overwrite remote generic templates with project-specific content
- Always compare file-by-file, not bulk copy
