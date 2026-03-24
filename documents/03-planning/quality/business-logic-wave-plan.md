# Wave 8 — Business Logic & Documentation Fix

**Date:** 2026-03-24
**Priority:** P0 CRITICAL
**Baseline:** KiteHub 1/7 docs, KiteClass 6/9 docs, Skills broken

---

## Problem Statement

Business logic đã code trong Wave 1-5 nhưng:
1. **9 business docs thiếu** — code có rules nhưng không document
2. **Skills không enforce** — quality audit 96/100 nhưng business docs 45%
3. **450KB docs duplicate** — `kiteclass-core/docs/modules/` vs `01-business/`
4. **README outdated** — không reflect `infrastructure/` folder

---

## PR List

### PR-1: Fix Skills (enforce business docs) [CRITICAL]

**Scope:**
- [ ] Update `/quality-audit` SKILL.md — thêm business doc scoring vào Category 8
- [ ] Update `/pre-flight-check` — Layer 1 thêm check "code change → business doc required"
- [ ] Update `/wave-completion-check` — Level 5 verify business docs đi kèm code
- [ ] Update `pre-commit-check.sh` — warning khi sửa service code mà không sửa `01-business/`

### PR-2: KiteHub Business Docs (6 docs) [CRITICAL]

**Format:** 4 sections per doc (Rules, Flow, Emails, Config) — ~100-150 lines each

- [ ] `kitehub/subscription-billing.md` — 9 rules, 4 email triggers, grace period, prorated
- [ ] `kitehub/email-lifecycle.md` — 13 templates, triggers, idempotency, schedulers
- [ ] `kitehub/instance-provisioning.md` — subdomain rules, reserved 23 names, trial limit, DB lifecycle
- [ ] `kitehub/domain-management.md` — custom domain, DNS verification, tier lock, 48h timeout
- [ ] `kitehub/data-retention.md` — 5 tier configs, warning 50%/80%, 3AM scheduler
- [ ] `kitehub/ai-branding.md` — rate limits per tier, template gallery, daily reset

### PR-3: KiteClass Business Docs (3 docs) [HIGH]

- [ ] `kiteclass/notification-email.md` — email triggers, templates, admin contact
- [ ] `kiteclass/tenant-settings.md` — theme, logo, favicon, S3 storage
- [ ] `kiteclass/gamification-points.md` — points per action, leaderboard, rules

### PR-4: Cleanup Duplicates & Scattered Docs [HIGH]

- [ ] DELETE `kiteclass/kiteclass-core/docs/modules/` (11 files, 450KB) — duplicate
- [ ] DELETE `kiteclass/kiteclass-core/docs/module-business-logic.md` — deprecated
- [ ] DELETE `kiteclass/docs/SESSION-SUMMARY.md` — stale
- [ ] UPDATE `kiteclass/kiteclass-core/README.md` — add pointer to `documents/01-business/kiteclass/`
- [ ] UPDATE `documents/01-business/README.md` — update index with all new docs

### PR-5: README & Cross-references [MEDIUM]

- [ ] UPDATE root `README.md` — add `infrastructure/` to structure, fix date
- [ ] UPDATE `documents/01-business/README.md` — complete index for all 16 docs
- [ ] Add cross-links between service READMEs and business docs

---

## Execution

| Agent | PRs | Scope | Conflict risk |
|-------|-----|-------|---------------|
| 1 | PR-1 | Skills fixes (.claude/skills/) | None |
| 2 | PR-2 | 6 KiteHub business docs (documents/01-business/kitehub/) | None |
| 3 | PR-3 + PR-4 | 3 KC docs + cleanup duplicates | None |
| 4 | PR-5 | README updates | None |

**0 shared files** — safe for parallel execution.

---

## Completion Criteria

- [ ] `documents/01-business/` has 16 docs (7 existing + 9 new)
- [ ] Each doc has 4 sections: Rules, Flow, Emails, Config
- [ ] `/quality-audit` includes business doc scoring
- [ ] `pre-commit-check.sh` warns on missing business docs
- [ ] 0 duplicate docs in `kiteclass/kiteclass-core/docs/modules/`
- [ ] README reflects current project structure
