---
paths:
  - "CLAUDE.md"
---

# CLAUDE.md Content Discipline — base context budget guard

**Priority:** 🟠 MANDATORY — base-context-budget governance
**Version:** 1.0.0
**Created:** 2026-05-15
**Last-Reviewed:** 2026-05-15
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule path-scoped per `context-budget-mandate.md` §3.1; complements `context-budget-mandate.md` cụ thể cho CLAUDE.md scope; built-in enforcement = §2 ceiling + §3 banned-content list + reviewer-checklist per §6.5 Enforcement Parity Mandate)
**Applies to:** Mọi PR touching `CLAUDE.md` (project root) — file này auto-load 100% mọi session, mọi turn → mỗi dòng thừa = tax mọi tương lai

---

## 1. The Rule

> **CLAUDE.md phải giữ <= 250 dòng (target ~200).** Mọi nội dung chi tiết (mechanism cụ thể, ví dụ, checklist) PHẢI đặt trong rule file riêng path-scoped và CLAUDE.md chỉ chứa 1-liner pointer.

CLAUDE.md auto-load 100% mọi session, mọi turn (per `context-budget-mandate.md` §2). 1 dòng thừa × N session × N turn = chi phí tích lũy nhanh. Wave 73 Meta Context Optimization codify ngân sách <120k tokens base — CLAUDE.md là 1 trong 3 nguồn lớn (CLAUDE.md + always-load rules + memory).

---

## 2. Content ceiling + structure

| Section | Mục đích | Max dòng |
|---|---|---|
| Communication language | 4 dòng tối đa | 4 |
| Current Phase | Active phase + decision context locked | 20 |
| Operational shortcuts (AWS start/stop, override pointer) | Each shortcut 1-2 dòng + link sang rule | 15 |
| Project Overview | 2 product line definitions | 10 |
| Superpowers methodology summary | 5 steps + link sang core skills | 15 |
| Docker / Git workflow / Wave strategy | Each major workflow ~5 dòng + link | 30 |
| Business Logic 3-Layer | Pattern + 3 file template + link | 15 |
| Living Documents | Table 5 rows + 1-line rule | 10 |
| Skills Reference | Index pointer chỉ + main category list | 30 |
| Folder Structure + Docker Naming | Reference tables | 20 |
| **Total target** | | **<200** |

Sections không trong table này = candidate xóa hoặc move sang rule.

---

## 3. Banned content (move sang rule file)

| ❌ Banned trong CLAUDE.md | ✅ Đặt ở đâu |
|---|---|
| Detailed step-by-step procedure (5+ bullet steps) | Rule path-scoped `.claude/rules/*.md` |
| Concrete code examples (>10 dòng) | Skill `.claude/skills/**/SKILL.md` |
| Self-test / worked examples | Rule `_examples/` folder |
| Detector regex / hook logic | `.claude/hooks/*.py` |
| Override mechanism table (when X allowed, when Y banned, etc.) | Dedicated rule file |
| Specific gap numbers / PR refs in narrative | Memory entries hoặc ROADMAP |
| Anti-pattern table >5 rows | Rule file |
| Multi-section deep-dive (>15 dòng cho 1 concept) | Split rule file + 1-liner pointer trong CLAUDE.md |

---

## 4. Required pattern for new CLAUDE.md additions

Khi add concept mới vào CLAUDE.md, mỗi addition PHẢI follow pattern:

```markdown
**<Tên concept>:** <1 câu mô tả tóm tắt>. Quy trình / chi tiết: `.claude/rules/<rule-name>.md` (path-scoped).
```

Ví dụ ĐÚNG (Wave 84 override pattern):
```markdown
**Solo-dev override:** khi dev nói "claude trigger" / "tôi cho phép" → claude được phép `gh workflow run terraform-apply.yml` (override `release-deploy-standard.md` §9 BANNED). Quy trình chi tiết: `.claude/rules/dev-authorized-terraform-trigger.md`.
```

Ví dụ SAI (bloat):
```markdown
**Solo-dev override:** khi dev nói các phrase sau...
1. Pre-flight bắt buộc...
2. Pre-mutation audit...
3. Default dry_run=true trước...
[20+ more lines]
```

---

## 5. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| "CLAUDE.md là master doc, càng đầy đủ càng tốt" | CLAUDE.md là INDEX; chi tiết ở rule file |
| Copy-paste rule content vào CLAUDE.md "để visible" | Link 1-liner; rule load on-demand qua `paths:` |
| Add new section vì "important" | Quantify: section >10 dòng → tách rule |
| Inline workflow diagram, ASCII art | External file + link |
| List 10 file paths inline | Link folder + main 3-5 paths |
| Forward-date "Wave 87 will need..." | CLAUDE.md = current-state truth, không placeholder |

---

## 6. Worked self-test — 2026-05-15 override addition

**Scenario:** User said "thêm vào claude md, khi dev cho phép claude trigger thì được với override rule".

**Attempt 1 (bloated — violates rule):**
- Added 30-line section directly vào CLAUDE.md với phrase list + 5 gate procedure + out-of-scope table
- User flagged: "claude md sửa quá dài, ngắn ngọn thôi, ảnh hưởng context start session"
- → Confirmation rule violation; net +30 dòng base context permanent

**Attempt 2 (correct per §4 pattern):**
- CLAUDE.md gets 1 paragraph: "Solo-dev override: ... Quy trình chi tiết: `.claude/rules/dev-authorized-terraform-trigger.md`"
- Detailed mechanism → `.claude/rules/dev-authorized-terraform-trigger.md` (path-scoped, NOT base load)
- Net CLAUDE.md cost: ~3 dòng vs ~30 dòng

→ Rule fires correctly on the originating incident. Self-test PASS ✅

Counterfactual cost if rule existed at attempt time:
- Attempt 1 caught immediately by §3 banned-content check (multi-bullet procedure → BANNED)
- §4 pattern auto-applied
- User round-trip saved
- Base context: 3 dòng (vs 30) — savings ~95% for this addition

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active now)

Mỗi PR touching `CLAUDE.md`:
- [ ] Tổng dòng CLAUDE.md sau PR ≤250?
- [ ] Mỗi section thuộc table §2? Nếu KHÔNG → tách rule
- [ ] Banned content §3 không xuất hiện?
- [ ] New addition follows §4 pattern (1-liner + link)?
- [ ] Rule file linked đã tồn tại (path-scoped)?

### 7.2 Memory auto-load (optional)

`feedback_claude_md_discipline.md` reminder — auto-load mỗi session để claude pre-check trước khi add section.

### 7.3 CI grep detector (deferred ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard)

Future: `scripts/check-claude-md-size.sh` count dòng + grep banned-content patterns. CI fail nếu >250 dòng hoặc match banned patterns. Defer cho v1.0.0; reviewer-checklist + worked self-test sufficient.

### 7.4 Override mechanism (rare)

Genuine exception (CLAUDE.md cần section chi tiết vì không có rule scope tương ứng):

```
git commit -m "...
CLAUDE_MD_BLOAT_OVERRIDE: <reason — vd 'no path-scope rule applicable, content cross-cuts'>"
```

Trailer logged. Pattern frequency >2/quarter → meta-review rule.

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Add detailed rule content vào CLAUDE.md "cho dev dễ thấy" | Rule file path-scoped — claude auto-load khi context match |
| Skip path-scope vì "rule này quan trọng" | Path-scope KHÔNG mất quan trọng — chỉ deferred load |
| Treat CLAUDE.md edit như rule edit (full version bump + log) | CLAUDE.md là index, not governed by `rule-change-process.md`; this rule (`claude-md-content-discipline.md`) là governance lớp |
| Restructure CLAUDE.md mỗi wave | Stability quan trọng — restructure ≤1×/quarter |

---

## 9. Relationship to other rules

- **`context-budget-mandate.md`** §2 — sister rule cho rules + memory; this rule cụ thể cho CLAUDE.md scope
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test all ship same PR
- **`docs-folder-structure.md`** — generic folder rule; this rule = specialized cho root CLAUDE.md
- **`readme-content-discipline.md`** — sister rule cho root README.md (volatile content denylist + stable allowlist); same pattern, different file
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-15 incident "user flagged CLAUDE.md bloat" via 5-stage pipeline
- **`output-review-mandate.md`** §3 — adds row "CLAUDE.md content" tracking review standard (Wave 84 closure includes this update)
- **`feedback_claude_md_discipline.md`** (memory, optional follow-up — defer ≥7 days)

---

## 10. Log

- **2026-05-15 (v1.0.0):** Rule created in same session as originating incident (Wave 84 closure session). Triggered by user explicit ask: "thêm rule review claude.md" + push-back "claude md sửa quá dài, ngắn ngọn thôi, ảnh hưởng context start session". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user flagged CLAUDE.md bloat trong real-time) → Classify ✓ (no existing rule codifies CLAUDE.md size/content discipline; `context-budget-mandate.md` covers rules + memory but không CLAUDE.md specifically; `readme-content-discipline.md` covers root README.md but không CLAUDE.md) → Rule+Enforce ✓ (this file + same-PR `dev-authorized-terraform-trigger.md` sister rule (correct pattern for detailed content) + CLAUDE.md condensed 1-liner + rules-index.csv 2 new rows per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on the originating attempt — 30-line bloat caught + corrected to 3-line + 95% savings) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint preventing CLAUDE.md bloat; no constraint loosening; existing CLAUDE.md grandfathered until next refresh per `output-review-mandate.md` §3 row to be added). Path-scoped per `context-budget-mandate.md` §3.1 — không bloat base context.
