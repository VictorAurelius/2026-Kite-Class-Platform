# Persona Acceptance Criteria — Tier-1 Per-Persona AC Docs

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Per-persona Acceptance Criteria (AC) framework — formal, measurable, reviewable. Source for `persona-based-business-review` skill execution (GAP-152 Round 1 review onwards).

**Audience:** Product Manager (drives review), Business Lead (validates persona scale assumptions), Domain expert (acts as real persona). Secondary: Engineering (consumes gap-linkage to plan fixes).

---

## Why This Folder Exists

User requirement (2026-04-20):
> "mỗi loại đối tượng cần khởi tạo 1 bộ tiêu chí của họ và họ sẽ review nghiệp vụ của hệ thống xem có đúng tiêu chí chưa"

`personas-catalog.md` defined 10 personas với "Key needs" bullet lists — but those are NOT formal ACs. Reviewer phải improvise checklist mỗi lần → non-reproducible, score drift.

This folder closes the gap: every Tier-1 persona has a formal `P<N>-<slug>.md` AC doc với 15-30 measurable ACs (Test + Fail signal + Linked gap), enabling quarterly reproducible reviews.

---

## Directory Map

| Path | Purpose | Tier | Status |
|------|---------|:----:|:------:|
| `README.md` | This index | — | — |
| [`_TEMPLATE.md`](_TEMPLATE.md) | Reusable AC template — 6 categories (onboarding, ops, fin/admin, comm, edge, exit) | — | 🟢 v1 |
| [`P1-solo-teacher.md`](P1-solo-teacher.md) | Solo Teacher (gia sư tự do) AC — 1 teacher, 5-50 students — **29 ACs** | 1 | 🟡 DRAFT v1 (PR #720) |
| [`P2-small-center.md`](P2-small-center.md) | Small Tutoring Center (trung tâm nhỏ/lớp học thêm) AC — 1-3 teachers, 20-100 students — **25 ACs** | 1 | 🟡 DRAFT v1 (PR #721) |
| [`P3-medium-center.md`](P3-medium-center.md) | Medium Education Center (trung tâm vừa) AC — 5-20 teachers, 100-500 students — **31 ACs** | 1 | 🟡 DRAFT v1 (PR #722) |
| [`P5-k12-school.md`](P5-k12-school.md) | Public/Private K-12 School (trường tiểu học/THCS/THPT) AC — 50+ teachers, 500-3000 students — **USER PRIORITY, 36 ACs, 73 MOET citations** | 1 | 🟡 DRAFT v1 (PR #723) |

**Total: 121 ACs across 4 Tier-1 personas** (Wave Persona-AC-Template SHIPPED 2026-04-30, GAP-151 → 🟢 DONE).

### Secondary persona AC (Phase 1, Wave 16, 2026-04-30)

Per-role × tenant-context AC for users **within** a tenant — extends GAP-151 template để cover Student/Parent/Teacher/Admin perspectives:

| Folder | Phase | Cells | Status |
|--------|:-----:|:-----:|:------:|
| [`secondary/`](secondary/) | Phase 1 (8 P0 cells) | Student×3 + Parent×1 + Teacher×2 + Admin×2 | 🟡 DRAFT v1 (Wave Secondary-Persona-AC, GAP-153) |

Phase 2 (P1 cells, 4 cells) → GAP-281 follow-up.
Phase 3 (P2 cells, 4 cells) → GAP-282 follow-up.

---

## Tier-2 / Tier-3 Personas (Deferred)

Per GAP-151 §Out-of-Scope, Tier-2/3 personas defer to follow-up gap (extends template once Tier-1 reviews stabilize):

- **P4** Large Education Chain / Franchise (Tier 2 — wave-pack post Tier-1 reviews)
- **P7** Corporate Training Department (Tier 2)
- **P8** Online Course Creator (Tier 2 — extends with content marketplace AC)
- **P9** International/Bilingual School (Tier 2 — extends K-12 với multi-language ACs)
- **P6** University/College (Tier 3 — possibly out-of-scope per personas-catalog)
- **P10** Special Education Center (Tier 3)
- **Secondary personas** (Admin/Director, Teacher, Student, Parent, Accountant within tenant) → tracked separately in **GAP-153**

---

## File Placement Rules

- ✅ **Belongs here:**
  - One markdown file per persona ID (P1, P2, ..., P10)
  - Template + README index (this file)

- ❌ **Does NOT belong here:**
  - Persona definitions / catalog → [`../personas-catalog.md`](../personas-catalog.md) (canonical source)
  - Review reports / scored ACs → [`../persona-reviews/`](../persona-reviews/) (separate folder, populated by GAP-152 onwards)
  - Cross-persona analysis → in review reports (not persona AC docs)

- **Naming:** `P<N>-<lowercase-kebab-slug>.md` (matches persona ID + short name)

---

## How to Use

### For AC author (creating new persona AC doc)

1. **Read** [`_TEMPLATE.md`](_TEMPLATE.md) §"How to Use This Template"
2. **Read** [`../personas-catalog.md`](../personas-catalog.md) — find persona's Key needs + Pain points
3. **Read** existing GAP-051..064 (gap files for cross-linkage)
4. **Copy** template → `P<N>-<slug>.md`
5. **Fill** §0 Context with realistic scale + organization archetype
6. **Derive** 15-30 ACs across 6 categories (onboarding, ops, fin, comm, edge, exit)
7. **Cross-link** existing gaps in §Gap Linkage Summary
8. **Status remains blank** — filled at review time (GAP-152 Round 1)

### For reviewer (executing review)

1. **Read** AC doc for persona
2. **Role-play** with real scale assumption — do NOT vague-walkthrough
3. **Mark each AC** PASS / PARTIAL / FAIL with evidence
4. **Calculate** Coverage % = (PASS + 0.5 × PARTIAL) / total × 100
5. **Output** scored report to [`../persona-reviews/YYYY-QN-persona-review.md`](../persona-reviews/)
6. **File NEW gaps** for FAIL ACs without existing gap, via `audit-to-gap-pipeline.md` Step 2.5 state-check first

---

## Quarterly Review Cadence

Per [`persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md) §Quarterly Review Cadence — calendar-anchored at end-of-quarter:

| Quarter | Review window | Output file |
|---------|---------------|-------------|
| Q1 | Mar 26-31 | `../persona-reviews/YYYY-Q1-persona-review.md` |
| Q2 | Jun 25-30 | `../persona-reviews/YYYY-Q2-persona-review.md` |
| Q3 | Sep 26-30 | `../persona-reviews/YYYY-Q3-persona-review.md` |
| Q4 | Dec 22-31 | `../persona-reviews/YYYY-Q4-persona-review.md` |

Off-cycle triggers: new VN regulation, new persona added, ≥3 user complaints clustering on same persona, pricing model change, major cross-persona feature launched.

---

## Last-Reviewed Tracking

| Persona | Last reviewed | Score | Reviewer | Next review |
|---------|:-------------:|:-----:|----------|:-----------:|
| P1 Solo Teacher | — | — | — | GAP-152 Round 1 (Q3 2026) |
| P2 Small Center | — | — | — | GAP-152 Round 1 (Q3 2026) |
| P3 Medium Center | — | — | — | GAP-152 Round 1 (Q3 2026) |
| P5 K-12 School | — | — | — | GAP-152 Round 1 (Q3 2026) |

(Updated by reviewer post each quarterly review cycle.)

---

## Related

- **Catalog:** [`../personas-catalog.md`](../personas-catalog.md) — canonical persona definitions (10 personas, Tier 1/2/3 classification)
- **Review skill:** [`../../../.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md) — methodology + quarterly cadence
- **Reports folder:** [`../persona-reviews/`](../persona-reviews/) — output reports (created when GAP-152 Round 1 executes)
- **Audit pipeline:** [`.claude/rules/audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md) — Step 2.5 state-check before filing new gaps
- **Parent gaps:** [GAP-050](../../04-quality/gaps/GAP-050-persona-based-business-review.md) (review process), [GAP-151](../../04-quality/gaps/GAP-151-persona-acceptance-criteria-template.md) (this AC framework), [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md) (Round 1 execution), [GAP-153](../../04-quality/gaps/GAP-153-secondary-persona-acceptance-criteria.md) (secondary persona AC)
