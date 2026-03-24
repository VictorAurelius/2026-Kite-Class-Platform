# Wave 8 — Business Logic, Skills Fix, Documentation Overhaul

**Date:** 2026-03-24
**Priority:** P0 CRITICAL
**Baseline:** KiteHub 1/7 business docs, KiteClass 6/9 docs, Skills không enforce, README outdated 1 tháng

---

## Problem Statement

1. **9 business docs thiếu** — code có rules nhưng không document
2. **Skills không enforce** — quality audit 96/100 nhưng business docs chỉ 45%
3. **450KB docs duplicate** — `kiteclass-core/docs/modules/` vs `01-business/`
4. **CLAUDE.md thiếu context** — session mới không biết project structure, wave strategy, naming convention
5. **README.md outdated nghiêm trọng** — KiteHub vẫn ghi "Coming soon", folder structure sai hoàn toàn
6. **Không có cơ chế nhắc update docs** — README, business docs, plans bị quên sau mỗi wave

---

## PR List

### PR-1: Fix Skills + Pre-commit (enforce business docs) [CRITICAL]

**Scope:**
- [ ] Update `/quality-audit` SKILL.md — thêm business doc scoring vào Category 8 (Documentation)
  - Business docs tồn tại cho mỗi domain đã implement (2 pts)
  - Business docs khớp code (config keys, rules) (2 pts)
  - Giảm max Documentation từ 10 → 10 nhưng redistribute
- [ ] Update `/pre-flight-check` — Layer 1 thêm check "code change → business doc required"
- [ ] Update `/wave-completion-check` — Level 5 verify business docs đi kèm code
- [ ] Update `pre-commit-check.sh` — warning khi sửa service code mà không sửa `01-business/`
- [ ] Tạo skill mới: `workflow/docs-freshness/SKILL.md` — nhắc update docs theo PR/wave:
  - Danh sách "living docs" cần update liên tục: README.md, 01-business/, plans, audit reports
  - Trigger: sau mỗi wave merge, sau mỗi PR thay đổi structure/business logic
  - Check: last modified date vs last code change date

### PR-2: KiteHub Business Docs (6 docs) [CRITICAL]

**Format:** 4 sections per doc (Rules, Flow, Emails, Config) — ~100-150 lines each
**Source:** Extract từ code analysis (SubscriptionService, EmailServiceClient, etc.)

- [ ] `kitehub/subscription-billing.md` — 9 rules, 4 email triggers, grace period 3d, prorated calculation
- [ ] `kitehub/email-lifecycle.md` — 13 templates, trigger mapping, idempotency (alreadySentToday), 4 schedulers
- [ ] `kitehub/instance-provisioning.md` — subdomain rules, 23 reserved names, trial limit 1x, DB lifecycle
- [ ] `kitehub/domain-management.md` — custom domain, DNS TXT verify, tier lock PREMIUM+, 48h timeout
- [ ] `kitehub/data-retention.md` — 5 tier configs (7/7/30/60/90d), warning 50%/80%, 3AM scheduler
- [ ] `kitehub/ai-branding.md` — rate limits per tier (3/10/50/unlimited), template gallery, daily reset

### PR-3: KiteClass Business Docs (3 docs) + Cleanup Duplicates [HIGH]

**New docs:**
- [ ] `kiteclass/notification-email.md` — email triggers, templates, admin contact
- [ ] `kiteclass/tenant-settings.md` — theme, logo, favicon, S3 storage rules
- [ ] `kiteclass/gamification-points.md` — points per action, leaderboard, reset rules

**Cleanup (450KB+ removed):**
- [ ] DELETE `kiteclass/kiteclass-core/docs/modules/` (11 files) — duplicate of `01-business/`
- [ ] DELETE `kiteclass/kiteclass-core/docs/module-business-logic.md` — deprecated
- [ ] DELETE `kiteclass/docs/SESSION-SUMMARY.md` — stale (2026-03-01)
- [ ] UPDATE `kiteclass/kiteclass-core/README.md` — add pointer: "Business logic → documents/01-business/kiteclass/"
- [ ] UPDATE `documents/01-business/README.md` — complete index for all 16 docs

### PR-4: CLAUDE.md + README.md Overhaul [HIGH]

**CLAUDE.md — thêm sections thiếu:**
- [ ] Project Overview: KiteHub (SaaS platform) + KiteClass (tenant service), shared infra
- [ ] Folder Structure: `infrastructure/`, `documents/01-08`, services
- [ ] Docker Naming: `kite-*` shared, `kitehub-*` service, `kiteclass-*` service
- [ ] Wave Branch Strategy: wave/X → PR → squash merge → main (KHÔNG merge trực tiếp)
- [ ] Living Docs: danh sách docs cần update liên tục
- [ ] Pre-commit Hook: 15+ checks tự động
- [ ] Current State: active plans, scores, wave status

**README.md — rewrite hoàn toàn:**
- [ ] KiteHub: KHÔNG "Coming soon" — mô tả 6 services, 220+ PRs, production-ready
- [ ] Folder structure: thêm `infrastructure/`, đúng `documents/01-08`
- [ ] Tech stack: cả KiteHub + KiteClass đầy đủ
- [ ] Setup guide: link đến `kitehub/QUICK_START.md` + `kiteclass/QUICK_START.md`
- [ ] Architecture: link đến `documents/02-architecture/`
- [ ] Business docs: link đến `documents/01-business/`
- [ ] Last Updated: 2026-03-24

---

## Execution

| Agent | PRs | Scope | Files touched |
|-------|-----|-------|---------------|
| 1 | PR-1 | Skills + pre-commit hook | `.claude/skills/`, `.claude/scripts/` |
| 2 | PR-2 | 6 KiteHub business docs | `documents/01-business/kitehub/` |
| 3 | PR-3 | 3 KC docs + cleanup 450KB | `documents/01-business/kiteclass/`, `kiteclass/kiteclass-core/docs/` |
| 4 | PR-4 | CLAUDE.md + README.md | Root files |

**0 shared files** — safe for parallel execution.

---

## Completion Criteria

- [ ] `documents/01-business/` has 16 docs (7 existing + 9 new)
- [ ] Each doc has 4 sections: Rules, Flow, Emails, Config
- [ ] Each doc rules extracted từ actual code (không viết từ imagination)
- [ ] `/quality-audit` includes business doc scoring
- [ ] `pre-commit-check.sh` warns on missing business docs
- [ ] New skill `docs-freshness` nhắc update living docs
- [ ] 0 duplicate docs in `kiteclass/kiteclass-core/docs/modules/`
- [ ] CLAUDE.md đủ context cho session mới (project overview, folder, wave strategy)
- [ ] README.md phản ánh đúng hiện trạng 2026-03-24
- [ ] Cross-references: service READMEs → business docs → architecture docs
