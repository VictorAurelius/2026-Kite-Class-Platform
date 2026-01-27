# Skill: Update Quick-Start

**IMPORTANT:** Update QUICK-START.md after EVERY significant work session to preserve context.

## Trigger Phrases
- "update quick start"
- "save context"
- After completing PR implementation
- Before ending session
- After major changes

## Purpose

Keep QUICK-START.md updated so new Claude sessions can resume work quickly without losing context.

## When to Update

1. After completing a PR
2. After major feature implementation
3. Before clearing context
4. When switching between tasks
5. At end of development session

## What to Update

### Current State
- Latest completed PR
- Branch name
- Number of tests passing
- Docker status

### Documentation Links
- Add new PR summary links
- Update "Latest" marker

### Requirements Section
- Update for next PR
- List dependencies
- List blocked items

### Completed Items
- Mark completed PRs with ✅
- Update roadmap
- Update test counts

## Template for Updates

```markdown
## 🚀 Option X: PR X.X - Feature Name

**Copy this prompt when context is cleared:**

\`\`\`
I'm continuing development on KiteClass Gateway Service.

CURRENT STATE:
- Working directory: /mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway
- Branch: feature/gateway
- Completed: PR X.X (Feature Name)
- Status: XX tests, Docker setup complete, ready for next PR

DOCUMENTATION TO READ:
1. /path/to/README.md
2. /path/to/PR-X.X-SUMMARY.md

MY REQUEST: Implement PR X.X - Next Feature

REQUIREMENTS:
- Requirement 1
- Requirement 2
- Add tests

CONSTRAINTS:
- Follow reactive patterns
- Use existing error handling
- Don't break existing tests

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Communicate in Vietnamese for easier control.**

Please read the docs first, then help me implement step by step.
\`\`\`
```

## Actions

### 1. Read Current Quick-Start
```bash
cat /path/to/QUICK-START.md
```

### 2. Update Sections
- Update "Current State"
- Add new PR option
- Update completed PRs list
- Update test counts
- Update roadmap

### 3. Add Vietnamese Note
Always include: `**NOTE: Communicate in Vietnamese for easier control.**`

### 4. Commit Changes
```bash
git add docs/QUICK-START.md
git commit -m "docs: update quick-start after PR X.X"
```

## File Location

`/mnt/e/person/2026-Kite-Class-Platform/kiteclass/kiteclass-gateway/docs/QUICK-START.md`

## Rules

1. **ALWAYS update after completing PR**
2. **ALWAYS add Vietnamese communication note**
3. **ALWAYS update test counts**
4. **ALWAYS mark completed items**
5. **ALWAYS commit the update**
