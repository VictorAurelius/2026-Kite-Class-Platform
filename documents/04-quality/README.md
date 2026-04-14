# 04-Quality — Navigation Index

Tài liệu quality, audits, gaps tổ chức theo category.

Refactored 2026-04-14 từ 37 files flat → 4 subfolders + README.

---

## 📁 Structure

```
documents/04-quality/
├── README.md                    ← You are here
├── gaps/                        ← Active gap queue (64 gaps + ROADMAP)
│   ├── README.md               ← Flat index, status legend
│   ├── ROADMAP.md              ← Epic grouping + sprint plan
│   ├── _TEMPLATE.md            ← Template for new gaps
│   └── GAP-001..064.md         ← Individual gap files
│
├── audits/                      ← Historical audit reports
│   ├── quality/                ← Quality audit reports (14 files)
│   ├── architecture/           ← Architecture gap reports (3 files)
│   ├── business/               ← Business gap checks (2 files)
│   ├── ui/                     ← UI audit reports (2 files)
│   └── waves/                  ← Wave completion reports (10 files)
│
├── analyses/                    ← Cross-cutting analyses
│   ├── skills-gap-analysis-vs-minimax.md
│   └── todo-comments-analysis-report.md
│
└── references/                  ← Reference documentation
    ├── security-design.md
    ├── preview-website-best-practices.md
    ├── parent-service-clarification-2026-02-27.md
    └── project-structure-audit-2026-03-24.md
```

---

## 🎯 Quick Navigation

### "Tôi muốn..."

| Need | Go to |
|------|-------|
| Xem gaps OPEN cần fix | [`gaps/README.md`](gaps/README.md) |
| Plan implementation | [`gaps/ROADMAP.md`](gaps/ROADMAP.md) |
| Plan fix all gaps | [`../03-planning/MASTER-GAPS-FIX-PLAN.md`](../03-planning/MASTER-GAPS-FIX-PLAN.md) |
| Xem latest quality score | [`audits/quality/quality-audit-2026-04-12.md`](audits/quality/) (latest) |
| Latest UI review | [`audits/ui/ui-review-latest.md`](audits/ui/ui-review-latest.md) |
| Business gap history | [`audits/business/`](audits/business/) |
| Architecture decisions history | [`audits/architecture/`](audits/architecture/) |
| Wave completion history | [`audits/waves/`](audits/waves/) |
| Skills comparison với MiniMax | [`analyses/skills-gap-analysis-vs-minimax.md`](analyses/skills-gap-analysis-vs-minimax.md) |
| Security design | [`references/security-design.md`](references/security-design.md) |

---

## 📊 By Folder

### gaps/ (Active Work Queue)

**Purpose:** Active gap tracking. Mỗi gap = 1 file với status + priority + dependencies.

**Total:** 64 gaps (20 P0, 24 P1, 20 P2)

**Files:**
- `README.md` — flat index với status/priority legend
- `ROADMAP.md` — organized vào 10 epics + 8-sprint plan
- `_TEMPLATE.md` — template cho gaps mới
- `GAP-001..064.md` — individual gap files

**Master plan:** `documents/03-planning/MASTER-GAPS-FIX-PLAN.md`

### audits/quality/ (Quality Audits)

**Purpose:** Historical quality audit reports. Date-stamped per `/quality-audit` skill run.

**Files (14 dated):**
- quality-audit-2026-03-22-kitehub
- quality-audit-2026-03-23-kiteclass / kitehub
- quality-audit-2026-03-24-* (6 files)
- quality-audit-2026-03-25-wave13
- quality-audit-2026-03-26 / 27
- quality-audit-2026-04-04
- **quality-audit-2026-04-12** (latest)

**How to find latest:** `ls -t audits/quality/ | head -1`

### audits/architecture/ (Architecture Analysis)

**Files:**
- `architecture-gaps-analysis-2026-02-27.md` — historical gap analysis
- `architecture-gaps-report.md` — cumulative gap report
- `architecture-qa.md` — architecture Q&A knowledge base

### audits/business/ (Business Logic Checks)

**Files:**
- `business-gap-check-2026-03-23-kiteclass.md`
- `business-gap-check-2026-03-23-kitehub.md`

**Skill:** `.claude/skills/business-gap-check.md` (v1.3 — adds AI Branding + Design Patterns)

### audits/ui/ (UI Reviews)

**Files:**
- `ui-audit-issues-2026-04-11.md` — issue tracking
- `ui-review-latest.md` — latest per-screen scoring

**Skill:** `.claude/skills/quality/ui-review/SKILL.md`

### audits/waves/ (Wave Completion Reports)

**Files (10):**
- Wave 1-5 completion checks
- Wave 11 completion + 12 phase A + 13 review
- KiteClass Wave 10 progress + KiteHub Wave 11 progress

**Skill:** `.claude/skills/wave-completion-check.md`

### analyses/ (Cross-Cutting Analyses)

**Files:**
- `skills-gap-analysis-vs-minimax.md` — MiniMax-AI/skills review + adoption plan (informed GAP-047)
- `todo-comments-analysis-report.md` — TODO/FIXME/HACK inventory

### references/ (Reference Documentation)

**Files:**
- `security-design.md` — security architecture reference
- `preview-website-best-practices.md` — landing/preview UX guidelines
- `parent-service-clarification-2026-02-27.md` — parent service architecture note
- `project-structure-audit-2026-03-24.md` — project structure review

---

## 🔄 Workflow Integration

### When to create new files

| Event | Location | Skill |
|-------|----------|-------|
| Run `/quality-audit` | `audits/quality/quality-audit-YYYY-MM-DD[-target].md` | quality-audit |
| Run `/business-gap-check` | `audits/business/business-gap-check-YYYY-MM-DD-[target].md` | business-gap-check |
| Run `/ui-review` | Overwrite `audits/ui/ui-review-latest.md` | ui-review |
| Run `/wave-completion-check N` | `audits/waves/wave-N-completion-check.md` | wave-completion-check |
| Discover gap | `gaps/GAP-XXX-short-title.md` | Manual + gap-to-pr-converter |
| Cross-cutting analysis | `analyses/{topic}.md` | Manual |
| Reference doc | `references/{topic}.md` | Manual |

### Retention policy

| Folder | Retention |
|--------|-----------|
| `gaps/` | Active until DONE; archive to `_archived/` after 6 months |
| `audits/quality/` | Keep all (history valuable); review quarterly |
| `audits/business/` | Keep all |
| `audits/ui/` | Keep ~5 latest; archive older |
| `audits/waves/` | Keep all (wave history immutable) |
| `audits/architecture/` | Keep all |
| `analyses/` | Keep all |
| `references/` | Keep all; update in-place when deprecated |

---

## 📈 Metrics Summary (as of 2026-04-14)

- **Gaps:** 64 total (20 P0, 24 P1, 20 P2)
- **Quality audits run:** 14 historical
- **Business gap checks:** 2 historical
- **Wave completions:** 10 documented
- **Architecture decisions:** 3 documented
- **Latest quality score:** 93/100 A (2026-04-12)
- **Latest UI score:** auth avg 91/128, dashboard avg 75/128

---

## 🔗 Related

- Gap master plan: `../03-planning/MASTER-GAPS-FIX-PLAN.md`
- Business docs: `../01-business/`
- Architecture: `../02-architecture/`
- Skills: `../../.claude/skills/`
- Rules: `../../.claude/rules/`

---

**Last Refactored:** 2026-04-14 — flat 37 files → 4 organized subfolders + README index
