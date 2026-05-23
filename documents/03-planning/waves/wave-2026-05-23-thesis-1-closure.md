---
title: Wave thesis-1 — Đóng cụm khóa luận pre-defense
status: draft
created: 2026-05-23
updated: 2026-05-23
wave: 1
tag_primary: thesis
tags_secondary: [doc, beta-prep, meta]
counter: 1
date_launch: 2026-05-23
waves: [thesis-1]
gaps: [GAP-647, GAP-651, GAP-652, GAP-653, GAP-655, GAP-687, GAP-689, GAP-623, GAP-648, GAP-649]
---

# Wave thesis-1 — Đóng cụm khóa luận pre-defense

**Goal:** Đóng tất cả gap thesis không bị chặn AWS, đưa `thesis-v1.docx` ≥85/100 + defense deck + beta cohort plan sẵn sàng cho bảo vệ.

**Trigger:** User direction 2026-05-23 "draft 1 wave fix all remaining thesis gaps" + chốt tag-based numbering scheme thay Wave 108 sequential.

**Estimated wall-clock:** ~5-6h agent work (Bucket D longest); ~24h serial → ~4-5x speedup.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phục vụ persona Author (chính tôi — sinh viên bảo vệ khóa luận) + GVHD + GVPB + Defense committee. Domain = academic deliverable (thesis-v1.docx + defense deck + beta cohort plan). Wave precede = Wave 102 closure (thesis-v1.docx baseline 82/100 B-) + Wave 100 outside-in audit (3-agent persona/benchmark/failure-mode 2026-05-19).

**Q2 (trade-offs):** 
- **Option rejected: file Wave 108 sequential** → user explicit chốt tag-based scheme; sequential mất signal cross-cutting wave grouping.
- **Option rejected: ship Bucket D Phase 2 `--execute` ở Wave thesis-1.1 riêng** → user chốt ship cùng Wave thesis-1; ETA Bucket D 5-6h chấp nhận được vì Opus 4.7 coordinator inline.
- **Option rejected: full beta cohort scope (invite email + Calendar template + audit checklist)** → user chốt plan doc only; execution defer Wave thesis-2 hậu GAP-612 AWS restore.
- **Option rejected: file 3 stub gap mới tracking Wave thesis-2 defer (648/649/687-P3)** → user chốt append Log của 3 gap hiện tại; không tăng gap count.

**Q3 (risks):**
- **Bucket D Phase 2 `--execute` mode fail** → `ThesisReportBuilder.java` production pipeline có bug → docx re-bake score thấp hơn dry-run baseline 82. **Recover:** coordinator git revert Bucket D commit + ship Phase 1 only như fallback; Phase 2 tách Wave thesis-1.1 follow-up.
- **6-agent rate-limit (Wave 102.7.4 lesson)** → spawn 6 song song có thể trigger Anthropic rate-limit. **Mitigation:** stagger 2-2-2 (Đợt 1 = A+B, Đợt 2 = C+F, Đợt 3 = D+E coordinator inline Opus 4.7).
- **Bucket A + B asset overlap** (skill `thesis-citation-extract` vs `thesis-figure-curation`) → cùng touch `documents/08-thesis/refs.md` hoặc `documents/06-diagrams/`. **Mitigation:** Bucket A read-only refs.md, không sửa; Bucket B INDEX.md riêng per chapter, không overlap.
- **Outside-in audit skip exception** → áp `outside-in-coverage-trigger.md` §4 row 4 (Wave 100 audit < 30 ngày). Risk: thesis surface có thể đã shift sau Wave 102.7.6. **Mitigation:** explicit document skip rationale §2 dưới; nếu Bucket C deck draft surface gap unforeseen → file follow-up gap Wave thesis-2.
- **AWS GAP-612 restore timeline unknown** → Wave thesis-2 (NFR + beta execution) có thể stall indefinite. **Mitigation:** Wave thesis-1 standalone valuable — defense readiness 8-8.5 mà không cần Wave thesis-2; 9-10đ requires Wave thesis-2 ship.

---

## 2. Outside-in audit decision

**SKIP** per `outside-in-coverage-trigger.md` §4 row 4 — Wave 100 (2026-05-19) 3-agent (persona simulation + VN edu SaaS benchmark + failure-mode matrix) đã cover thesis surface; audit < 30 ngày → exception áp dụng. Wave 102.7.6 audit + GAP-688 closure audit cũng check residuals.

Surfaced gap (10 thesis gap) = direct output từ Wave 100 3-agent synthesis. Wave thesis-1 = execution của outside-in findings, không phải new inside-out brainstorm cần audit lại.

---

## 3. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|---|---|---|---|---|
| §0 META | New rule `wave-tag-numbering-convention.md` + skill update + matrix row + CSV + 3 gap Log + plan file | coordinator inline | ~30-40 min | ✅ ship trước agent spawn |
| A | GAP-647 Step 3 + GAP-655 | bg-agent | ~2-3h | ✅ skill `thesis-citation-extract` + refs.md read-only |
| B | GAP-651 | bg-agent | ~3-4h | ✅ skill `thesis-figure-curation` + INDEX.md per Ch.1-4 |
| C | GAP-653 | bg-agent | ~4-5h | ✅ defense deck Reveal.js + Q&A sheet (standalone) |
| D | GAP-687 Phase 1+2 + GAP-689 Phase 3+4 | coordinator inline Opus 4.7 | ~5-6h | ⚠️ shared `thesis-v1.docx` artifact — coordinator độc quyền re-bake |
| E | GAP-623 | coordinator inline | ~2-3h | ✅ doc-only `release-1-beta-cohort-plan.md` |
| F | GAP-652 | bg-agent | ~3-4h | ✅ `seed-thesis-demo-tenants.sh` + demo script local Docker |

Disjoint check:
- Bucket A read `refs.md` only; Bucket B write INDEX.md per chapter — no overlap
- Bucket C draft `defense-deck.html` + `defense-qa-response-sheet.md` — independent file path
- Bucket D độc quyền `thesis-v1.docx` + `ThesisReportBuilder.java` — coordinator inline tránh agent conflict
- Bucket E write `release-1-beta-cohort-plan.md` — new file path
- Bucket F write `seed-thesis-demo-tenants.sh` + `thesis-multi-tenant-demo-script.md` — new file paths

---

## 4. Scope (compact schema)

**Stake tier:** MEDIUM-HIGH → model Opus 4.7 (1M) cho coordinator + Bucket D inline; bg-agent Bucket A/B/C/F dùng default model per skill spawn template.

**Cross-layer?** NO — thesis closure = docs + skill scope; không touch BE/FE production code (except Bucket D `ThesisReportBuilder.java` thuần thesis tooling, không production).

> Gap referencing: query state qua `bash scripts/query-gaps.sh <prefix>` trước khi spawn agent confirm status/priority match.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|---|---|:-:|---|:-:|
| 0 | **META prereq** | new rule + skill update + matrix + CSV + plan + 3 gap Log | 🟠 P0 | `.claude/rules/wave-tag-numbering-convention.md` + `.claude/skills/quality/wave-pack-planner/SKILL.md` + `.claude/rules/output-review-mandate.md` + `.claude/rules/rules-index.csv` + `documents/03-planning/waves/wave-2026-05-23-thesis-1-closure.md` + `documents/04-quality/gaps/phase-1-beta/GAP-{648,649,687}*.md` | SHIP FIRST (Phase 1) |
| 1 | **A** | GAP-647 Step 3 + GAP-655 | 🟠 P1 | `.claude/skills/quality/thesis-citation-extract/**` + `documents/08-thesis/refs.md` (read-only) | Đợt 1 |
| 2 | **B** | GAP-651 | 🟠 P1 | `.claude/skills/quality/thesis-figure-curation/**` + `documents/08-thesis/chapters/INDEX.md` (per Ch.1-4) | Đợt 1 |
| 3 | **C** | GAP-653 | 🟠 P1 | `documents/08-thesis/defense/defense-deck.html` + `defense-qa-response-sheet.md` + `defense-demo-script.md` + `practice-schedule.md` | Đợt 2 |
| 4 | **F** | GAP-652 | 🟠 P1 | `scripts/seed-thesis-demo-tenants.sh` + `documents/08-thesis/defense/multi-tenant-demo-script.md` | Đợt 2 |
| 5 | **D** | GAP-687 P1+P2 + GAP-689 P3+P4 | 🟠 P1 | Bucket D scope (coordinator inline, no agent) | Đợt 3 |
| 6 | **E** | GAP-623 | 🟠 P1 | `documents/03-planning/release/release-1-beta-cohort-plan.md` | Đợt 3 |

### Bucket D — Coordinator inline scope chi tiết

**Phase 1 (~1.5-2h):** GAP-687 Phase 1 strip-or-rename
- Sweep `documents/08-thesis/chapters/**` cho TODO markers + draft-only sections
- Rename hoặc strip per content discipline (cite `thesis-content-standard.md` rubric)

**Phase 2 (~2-3h):** GAP-687 Phase 2 `ThesisReportBuilder --execute` production mode
- Adapt `kitehub/kitehub-thesis-tools/src/main/java/.../ThesisReportBuilder.java` (hoặc Python `create_thesis_v1.py` path per Wave 102.6 pivot)
- Replace dry-run heuristic với production pipeline (full MD parse + figure inject + bibliography auto-format)
- 17 JUnit test verify PASS (per GAP-646 baseline) + new test cho `--execute` mode

**Phase 3+4 (~1-1.5h):** GAP-689 Phase 3+4 final polish + signoff
- Polish round 4 (residual misses post Wave 102.7.6)
- Signoff doc `documents/08-thesis/SIGNOFF.md` ghi acceptance criteria met

**Re-bake (~30 min):** `python create_thesis_v1.py --execute` → `thesis-v1.docx` v2
- Audit qua `thesis-content-standard.md` rubric → target ≥85/100 B+
- Nếu <85 → revert Phase 2, ship Phase 1 only Wave thesis-1; Phase 2 tách Wave thesis-1.1

### Bucket E — Coordinator inline scope chi tiết

**`release-1-beta-cohort-plan.md` (~2-3h):**
- §1 Bối cảnh + objective: 9 tuần thu ≥4 nhận xét ký tay
- §2 Persona target: 2 GV trial (anonymous prospect) + 2 GV VIP (warm intro)
- §3 Timeline gantt 9 tuần: T-9 invite send → T-7 onboard call → T-6..T-3 active use → T-2 review collect → T-1 buffer → T-0 defense
- §4 Invite flow narrative (template hoãn Wave thesis-2)
- §5 Feedback collection template (signed review template — defer execution Wave thesis-2)
- §6 Risk + mitigation (GAP-612 chặn AWS, dependency timeline)
- §7 Acceptance criteria

---

## 5. Defer Wave thesis-2 (hậu GAP-612 AWS restore)

3 gap append Log dòng (không file gap stub mới per user direction):

| Gap | Log entry append |
|---|---|
| GAP-648 NFR data capture | `2026-05-23: DEFER Wave thesis-2 — k6 production load test + CloudWatch p50/p95 ≥30 ngày + AWS Cost Explorer screenshots cần production cluster live. Trigger restart: GAP-612 DONE + cluster live ≥7 ngày.` |
| GAP-649 Beta cohort execution | `2026-05-23: DEFER Wave thesis-2 — ≥4 nhận xét ký tay cần beta tenant thật + 9 tuần timeline. Wave thesis-1 ship plan doc only (Bucket E). Trigger restart: GAP-612 DONE + invite gửi.` |
| GAP-687 Phase 3 (NFR + beta + Ch.5-7 evidence) | `2026-05-23: DEFER Wave thesis-2 Phase 3 — Ch.5-7 evidence phụ thuộc NFR (GAP-648) + beta execution (GAP-649) production data. Wave thesis-1 ship Phase 1+2 only. Trigger restart: GAP-648 + GAP-649 DONE.` |

---

## 6. Coordinator inline (4 nhiệm vụ)

### Phase 1 — META prereq + plan file (~30-40 min, THIS PR)

1. ✅ Checkout `wave/thesis-1-closure` từ `origin/main` (Task #1)
2. ✅ Ship `.claude/rules/wave-tag-numbering-convention.md` v1.0.0 (Task #2)
3. ✅ Update `wave-pack-planner/SKILL.md` Section "Wave numbering" + `output-review-mandate.md` §3 matrix row + `rules-index.csv` (Task #3)
4. 🔄 Tạo plan file (this file) + append Log 3 gap defer (Task #4 in-progress)
5. ⏳ Commit + push + PR `plan(wave-thesis-1): closure 6 bucket parallel + META convention prereq` (Task #5)
6. ⏳ User review + merge plan PR (out of session)

### Phase 2 — Spawn 6 bucket (sau plan PR merge)

Stagger 2-2-2:

```
Đợt 1 (parallel bg-agent):
  - Bucket A: agent prompt cite GAP-647 Step 3 + GAP-655 scope + skill template
  - Bucket B: agent prompt cite GAP-651 scope + figure selection criteria + INDEX template

Đợt 2 (parallel bg-agent, sau Đợt 1 land):
  - Bucket C: agent prompt cite GAP-653 deck + Q&A + practice
  - Bucket F: agent prompt cite GAP-652 demo script + seed scripts

Đợt 3 (coordinator inline Opus 4.7, sau Đợt 1+2 ship):
  - Bucket D: Phase 1+2 + GAP-689 P3+P4 + re-bake docx
  - Bucket E: release-1-beta-cohort-plan.md doc-only
```

### Phase 3 — Closure (~30 min)

1. Re-bake `thesis-v1.docx` lần cuối sau Bucket A+B output merge vào source MD
2. Audit qua `thesis-content-standard.md` rubric → verify ≥85/100
3. Sync `gap-status.csv`:
   - GAP-647 PARTIAL 80 → DONE 100
   - GAP-651 OPEN → DONE
   - GAP-653 OPEN → DONE
   - GAP-655 OPEN → DONE
   - GAP-689 PARTIAL 50 → DONE
   - GAP-623 OPEN → DONE (doc-only)
   - GAP-652 OPEN → DONE (script-only)
   - GAP-687 OPEN → PARTIAL 67 (Phase 1+2 ship; Phase 3 defer)
4. Append `wave-history.jsonl` entry mới format:
   ```json
   {"wave":"thesis-1","tag_primary":"thesis","tags_secondary":["doc","beta-prep","meta"],"counter":1,"date":"2026-05-23","theme":"Thesis closure 6 bucket parallel + META convention prereq","gaps":["GAP-647","GAP-651","GAP-653","GAP-655","GAP-687","GAP-689","GAP-623","GAP-652"],"...":"..."}
   ```
5. Update ROADMAP §🎯 Current Status Snapshot entry mới format
6. Session handoff doc `documents/03-planning/session-handoffs/2026-05-23-wave-thesis-1-closure.md`

### Phase 4 — Self-test §0 rule

1. Append Log entry trong `wave-tag-numbering-convention.md` §10 ghi Phase 1 ship + Phase 2/3 in-progress
2. Confirm `rules-index.csv` `path_trigger` cột match `paths:` rule frontmatter

---

## 7. Acceptance Criteria

### Phase 1 (THIS PR)

- [x] §0 META artifact: rule + skill update + matrix row + CSV row shipped
- [x] Plan file Wave thesis-1 (this file) tạo
- [ ] 3 gap defer Log dòng append (Task #4 sub-step)
- [ ] Plan PR `plan(wave-thesis-1): ...` created + CI green + user merge

### Phase 2 (sau plan merge — không Phase 1 scope)

- [ ] 6 bucket ship qua 6 PR squash merge → branch wave/thesis-1 collect all
- [ ] 7 gap chuyển status: 647 DONE / 651 DONE / 653 DONE / 655 DONE / 689 DONE / 623 DONE doc-only / 652 DONE script-only
- [ ] 1 gap PARTIAL Phase 1+2 ship: 687
- [ ] `thesis-v1.docx` re-bake ≥85/100 per `thesis-content-standard.md` rubric

### Phase 3 (closure)

- [ ] `wave-history.jsonl` entry mới format pass schema check
- [ ] ROADMAP §🎯 entry mới format
- [ ] Session handoff doc shipped

### Self-test wave-tag-numbering-convention rule

- [x] Wave identifier `wave-thesis-1-closure` ✅
- [x] Plan filename `wave-2026-05-23-thesis-1-closure.md` ✅
- [x] Branch `wave/thesis-1-closure` ✅
- [x] Plan PR commit `plan(wave-thesis-1): ...` ✅ (Phase 1 commit)
- [x] Frontmatter wave=1 + tag_primary=thesis + tags_secondary=[doc, beta-prep, meta] + counter=1 + date_launch=2026-05-23 ✅
- [ ] Bucket commit format `feat(wave-thesis-1-bucket-{A-F}): ...` (Phase 2)
- [ ] wave-history.jsonl entry mới format (Phase 3)
- [ ] ROADMAP entry "Wave thesis-1 ..." mới format (Phase 3)

---

## 8. Risk + mitigation matrix

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Bucket D Phase 2 `--execute` bug | Trung bình | docx score thấp hơn 82 baseline | Coordinator inline test trước re-bake; fallback ship Phase 1 only nếu fail |
| Rate-limit 6 agent | Trung bình | spawn fail / retry loop | Stagger 2-2-2 per Wave 102.7.4 lesson |
| Bucket A + B asset conflict | Thấp | merge conflict on refs.md / INDEX.md | Bucket A read-only refs.md; Bucket B INDEX.md per chapter (disjoint) |
| Outside-in audit miss | Thấp | thesis surface shifted post Wave 102.7.6 | §2 explicit skip + follow-up gap nếu Bucket C surface unforeseen |
| GAP-612 AWS chưa restore | Cao | Wave thesis-2 stall indefinite | Wave thesis-1 standalone valuable cho defense 8-8.5đ |
| Plan PR merge delay | Thấp | Phase 2 blocked | Coordinator có thể spawn agents trong branch wave/thesis-1-closure trước merge nếu user OK |

---

## 9. Sản phẩm dự kiến (deliverables summary)

**Tài liệu thesis:**
- `thesis-v1.docx` re-bake ≥85/100 (production pipeline `--execute` mode)
- 44+ figure numbered + caption VN + INDEX per Ch.1-4
- Bibliography IEEE 100% (44+ refs, citation-extract automated)
- TODO scrub + final polish round 4

**Tài liệu defense:**
- `defense-deck.html` Reveal.js 30-40 slide
- `defense-demo-script.md` 15 phút end-to-end
- `defense-qa-response-sheet.md` 20 câu Q&A
- `practice-schedule.md` 2 buổi T-3 + T-2

**Tài liệu beta + demo:**
- `release-1-beta-cohort-plan.md` (doc-only, execution defer)
- `seed-thesis-demo-tenants.sh`
- `multi-tenant-demo-script.md` 5 phút secondary demo

**Tooling skill mới:**
- `.claude/skills/quality/thesis-citation-extract/**`
- `.claude/skills/quality/thesis-figure-curation/**`

**META convention:**
- `.claude/rules/wave-tag-numbering-convention.md` v1.0.0
- `wave-history.jsonl` schema extension (legacy + new coexist)
- `wave-pack-planner/SKILL.md` Section "Wave numbering"
- `output-review-mandate.md` §3 matrix row "Wave naming convention"

---

## 10. Related

- Rule: `.claude/rules/wave-tag-numbering-convention.md` v1.0.0 (META prereq §0)
- Rule: `.claude/rules/docs-filename-prefix-convention.md` Tier 3 (sister rule, paired enforcement)
- Rule: `.claude/rules/thesis-content-standard.md` v1.0.0 (rubric cho docx score)
- Rule: `.claude/rules/outside-in-coverage-trigger.md` §4 row 4 (audit skip rationale §2)
- Rule: `.claude/rules/output-review-mandate.md` §3 (matrix row "Wave naming convention")
- Skill: `.claude/skills/quality/wave-pack-planner/SKILL.md` (Section "Wave numbering" updated)
- Wave 100 audit: `documents/04-quality/audits/persona-review/2026-05-18-thesis-{persona-demo,vn-saas-benchmark,defense-failure-mode-matrix}.md`
- Wave 102 closure: `documents/03-planning/waves/wave-2026-05-21-102.7.6-thesis-v1-final-polish.md`
- Memory: `feedback_wave_pack_cross_gap_clustering.md`
- Memory: `feedback_parallel_agent_strategy.md`
- Memory: `feedback_vietnamese_narrative_default.md` (paired session lesson 2026-05-23)
