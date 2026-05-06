# GAP-375: GitHub Release Template + Changelog Folder

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 nice-to-have (Phase 1 BETA — improvement)
**Domain:** DevOps / Documentation
**Found:** 2026-05-06 (Release 1 deploy plan; per `versioning-policy.md` §14)
**Affects:** Release notes consistency, changelog discoverability

## Problem

`versioning-policy.md` §8 defines Changelog convention nhưng KHÔNG có:
- `documents/03-planning/changelogs/` folder created
- First changelog file (CHANGELOG-v0.9.0-beta.md template)
- GitHub Release template defined
- Release notes auto-population workflow

## Proposed Fix

### 1. Create folder
- `documents/03-planning/changelogs/` với README.md explaining structure
- Template: `documents/03-planning/changelogs/_TEMPLATE.md`

### 2. Template (per versioning-policy §8)
```markdown
# Release vX.Y.Z — <name> (YYYY-MM-DD)

## 🎯 Highlights
- top 3 user-facing wins

## ✨ Added (new features)
## 🔄 Changed
## 🐛 Fixed
## 🔒 Security
## 📚 Docs
## ⚠️ Breaking changes (MAJOR only)
## 🙏 Acknowledgements
## Migration guide (MAJOR only)
```

### 3. GitHub Release template
`.github/RELEASE_TEMPLATE.md`:
```markdown
## What's Changed
<auto-populated from CHANGELOG-vX.Y.Z.md>

## Installation / Upgrade
- Production: <link to deploy-runbook>
- Staging: <link to staging deploy>

## Acknowledgements
- Contributors: <list>

**Full Changelog:** <comparison link>
```

### 4. First changelog
- Backfill `CHANGELOG-v0.x.x.md` cho earlier releases (optional)
- Create stub `CHANGELOG-v0.9.0-beta.md` to be filled at Phase 1 BETA launch

## Acceptance Criteria

- [ ] `documents/03-planning/changelogs/` folder created với README
- [ ] `_TEMPLATE.md` file
- [ ] `.github/RELEASE_TEMPLATE.md` (or equivalent)
- [ ] Stub changelog cho v0.9.0-beta
- [ ] Cross-link from `versioning-policy.md` §8
- [ ] Workflow integration: GAP-374 release-tag.yml uses template

## Effort estimate

~half day docs.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Versioning: `documents/03-planning/roadmap/versioning-policy.md` §8
- Sister: GAP-374 (CI release automation consumes changelog)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. Per versioning-policy §14 open item. P2 — không block launch nhưng professionalism + audit trail.
