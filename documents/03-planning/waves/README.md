---
title: Waves — Wave Plan Index
status: active
created: 2026-06-08
updated: 2026-06-08
---

# 03-planning/waves — Wave Plans

**Rules:** [`docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) · [`planning-docs-structure.md`](../../../.claude/rules/planning-docs-structure.md) · [`wave-tag-numbering-convention.md`](../../../.claude/rules/wave-tag-numbering-convention.md) · [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) §2.6 · [`wave-closure-scope-completeness.md`](../../../.claude/rules/wave-closure-scope-completeness.md)
**Skill:** [`.claude/skills/quality/wave-pack-planner/SKILL.md`](../../../.claude/skills/quality/wave-pack-planner/SKILL.md) — cluster gap + spawn parallel agents.

Nơi lưu **mọi wave plan**. Mỗi file là kế hoạch của một đợt làm việc (wave). Template chuẩn: [`_TEMPLATE.md`](_TEMPLATE.md). Lịch sử wave (canonical, machine-readable): [`.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl`](../../../.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl).

---

## Khái niệm: Wave là gì trong dự án này

Wave = **một đợt làm việc gom ≥3 gap disjoint, chia thành buckets, spawn agent song song** (worktree-isolated) thay vì serial PR — codify "5x speedup" của wave-pack methodology. Một wave gồm:

- **Plan PR** (file này) merge TRƯỚC khi spawn agent (per `feedback_wave_plan_through_pr.md`).
- **Buckets** — đơn vị công việc disjoint, mỗi bucket 1 agent.
- **Closure** — sync 4 target (gap-status.csv + ROADMAP + wave-history.jsonl + MEMORY) + scope-completeness reconciliation per `wave-closure-scope-completeness.md`.

**Wave plan PHẢI có** (per `_TEMPLATE.md` + CI `check-wave-plan-completeness.sh`): §1 Brainstorm → §2 Task Breakdown → §3 Scope → §4 State-Check Evidence (bắt buộc per `audit-to-gap-pipeline.md` §2.6/§2.6.1) → §5 Verification Gates → §6 Agent Spawn → §7 Closure Protocol → §8 Log. Frontmatter: `title` + `status` + `created` + `waves`.

---

## Naming (per `wave-tag-numbering-convention.md`)

Từ Wave thesis-1 (2026-05-23) trở đi dùng tag-based: `wave-{YYYY-MM-DD}-{tag_primary}-{counter}-{descriptor}.md`.

| Component | Ví dụ |
|---|---|
| Filename | `wave-2026-06-07-p0-local-1-db-integrity-sweep.md` |
| Branch | `wave/p0-local-1` |
| Commit | `plan(wave-p0-local-1): ...` / `feat(wave-p0-local-1-bucket-a): ...` |

Wave 01-107 (sequential cũ) grandfathered — không backfill tag.

---

## Directory Map

| Path | Mục đích | Typical files |
|---|---|---|
| [`_TEMPLATE.md`](_TEMPLATE.md) | Template wave plan (copy khi tạo wave mới) | 1 |
| `wave-{date}-{tag}-{N}-*.md` | Wave plan (active + shipped) | ~110 |
| `wave-01-30/` | Archive sub-range wave plan cũ (per `docs-folder-volume-budget.md` split) | nhiều |

---

## File Placement Rules

- ✅ **Belongs here:** Wave plan (kế hoạch một đợt work với buckets + state-check evidence).
- ❌ **Does NOT belong here:**
  - Feature/strategy plan không phải wave → [`../plans/`](../plans/)
  - Gap files → [`documents/04-quality/gaps/`](../../04-quality/gaps/)
  - Audit reports → [`documents/04-quality/audits/`](../../04-quality/audits/)
- Naming: `wave-{YYYY-MM-DD}-{tag}-{counter}-{descriptor}.md` per `wave-tag-numbering-convention.md` §2.2.

---

## Archive Policy

Per [`docs-archival-cadence.md`](../../../.claude/rules/docs-archival-cadence.md) §2: wave plan `status: complete` archive khi **≥60 ngày POST closure** → `documents/07-archived/planning-{year}/waves/`. Per `docs-folder-volume-budget.md` §2 (cap 50 time-bound): khi folder vượt cap → sub-split theo wave-range subdir (vd `wave-01-30/`).

---

## Key Documents

- [`_TEMPLATE.md`](_TEMPLATE.md) — wave plan template
- [`wave-pack-planner/SKILL.md`](../../../.claude/skills/quality/wave-pack-planner/SKILL.md) — methodology + agent spawning
- [`wave-history.jsonl`](../../../.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl) — lịch sử wave canonical
