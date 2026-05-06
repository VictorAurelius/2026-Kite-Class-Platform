# 03-planning — Active Planning Area

**Rules:** [`.claude/rules/planning-docs-structure.md`](../../.claude/rules/planning-docs-structure.md)

Active planning artifacts. Work that is upcoming, in-flight, or recently shipped. Completed plans >90 days old move to [`documents/07-archived/`](../07-archived/).

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `MASTER-GAPS-FIX-PLAN.md` | Top-level gap strategy | 1 |
| [`roadmap/`](roadmap/) | Cross-cutting multi-wave roadmaps | `wave-roadmap-p0.md`, `kitehub-saas-implementation-plan.md`, `parallel-execution-strategy.md` |
| [`waves/`](waves/) | Per-wave plan docs (active + pending) | `wave-XX-*.md` |
| [`plans/`](plans/) | Feature/domain-specific plans (non-wave) | `plan-ui-ux-design-system-integration.md`, `pr-kc-e2e-fix.md` |
| [`analyses/`](analyses/) | One-off analyses, best-practice studies | `kitehub-saas-best-practices-analysis.md` |
| [`pr-logs/`](pr-logs/) | Auto-generated PR lifecycle JSON | `PR-{N}.json` |
| [`prs/`](prs/) | Master PR index + per-service lists | `00-master-pr-index.md` |
| [`api/`](api/) | API contract planning | `api-contracts-overview.md` |
| [`database/`](database/) | DB design + migration plans | `database-design.md`, `database-migration-plan.md` |
| [`infrastructure/`](infrastructure/) | Infra provisioning plans | `kitehub-oracle-cloud-deployment.md` |
| [`implementation/`](implementation/) | Detailed per-service implementation plans | `core-service-implementation.md`, `frontend-plan.md` |
| [`quality/`](quality/) | Quality improvement plans (pre-audit era) | `code-review-pr-plan-COMPLETED.md` |
| [`testing/`](testing/) | Test strategy docs | `integration-testing-strategy.md`, `local-e2e-roadmap.md` |
| [`project-management/`](project-management/) | Schedules, checklists, surveys | `project-schedule.md` |

---

## Key Documents (start here)

- 🟢 **MVP Launch Plan 2026** (CURRENT — chốt 2026-05-06): [`roadmap/mvp-launch-plan-2026.md`](roadmap/mvp-launch-plan-2026.md) — 3-phase rollout P1+P2 → P3 → K-12; 9-12 tuần Phase 1 active; **đây là plan ưu tiên cho mỗi session mới** per `feedback_mvp_first_session_priority.md`
- **Legacy roadmap:** [`roadmap/wave-roadmap-p0.md`](roadmap/wave-roadmap-p0.md) — 11-wave P0 plan (superseded bởi MVP plan above)
- **Latest wave plan:** [`waves/`](waves/) — most recent file
- **PR index:** [`prs/00-master-pr-index.md`](prs/00-master-pr-index.md)
- **Planning rules:** [`.claude/rules/planning-docs-structure.md`](../../.claude/rules/planning-docs-structure.md)

---

## How to Add a New Plan

1. **Decide the category:**
   - Wave work → `waves/` (filename: `wave-{N|name}-{topic}.md`)
   - Feature/investigation → `plans/` (filename: `plan-{topic}.md`)
   - Cross-cutting roadmap → `roadmap/`
   - Analysis → `analyses/`

2. **Add frontmatter** (required for `waves/`, `plans/`, `roadmap/`):
   ```yaml
   ---
   title: Short title
   status: active | complete | superseded | draft
   created: YYYY-MM-DD
   updated: YYYY-MM-DD
   waves: [N]                  # for waves/ files
   gaps: [GAP-XXX]             # for plans touching gaps
   ---
   ```

3. **Commit via PR** — never push to main. See [`.claude/rules/planning-docs-structure.md`](../../.claude/rules/planning-docs-structure.md) §3-6 for details.

---

## Archive Policy

Move to [`documents/07-archived/planning-YYYY/`](../07-archived/) when:
- Wave merged + all gaps in wave marked DONE
- Plan superseded (add `superseded_by:` to new plan frontmatter)
- Doc >90 days old + no recent reference

Bulk archival: end-of-quarter.

---

## Log

- **2026-04-18:** Restructured — 15 root-level `.md` consolidated into `waves/`, `plans/`, `roadmap/`, `analyses/` per new rule `planning-docs-structure.md`.
