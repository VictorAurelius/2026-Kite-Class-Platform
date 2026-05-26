---
title: Wave thesis-2 — Re-ground khung chuẩn DATN + clear inside backlog
status: in-progress
created: 2026-05-26
updated: 2026-05-26
tag_primary: thesis
tags_secondary: [meta, content-quality]
waves: [thesis-2]
gaps: [GAP-688]
audience: dev
---

# Wave thesis-2 — Re-ground khung chuẩn DATN + clear inside backlog

**Goal:** Bốn buckets ship iterative — Bucket A re-ground rule `thesis-content-standard.md` v2.0.0 MAJOR theo khung chuẩn UTC nguyên bản (`documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` + `Quy dinh trinh bay do an tot nghiep.pdf`), bỏ phần thừa khỏi pipeline (LỜI CAM ĐOAN + TÓM TẮT + ABSTRACT + NHẬN XÉT GVHD), restructure chapter 1 từ 3-files thành 1 chương "Tổng quan" khớp khung. Bucket B-F clear 5 DEFERRED + 5 MISSED inside items từ Wave 102.7 audit map. Wave OPEN qua nhiều PRs, đóng khi user xác nhận hài lòng.

**Trigger:** User direction 2026-05-26 — session sửa thesis. Audit map `2026-05-20-wave-102.7-14-item-inside-mapping.md` cho thấy inside coverage 42% (8/19 items); user-flagged "Lời cam đoan không có trong khung chuẩn" surface coverage gap trong rule `thesis-content-standard.md` v1.1.0 (rule mâu thuẫn nội tại: line 33+72 nói optional, line 251+417 nói required). User cung cấp ảnh khung chuẩn nguyên bản → "ngoài lời cam đoan thì cái gì không theo khung chuẩn cũng bỏ".

**Estimated wall-clock:** Wave OPEN iterative; mỗi bucket ~1-3h coordinator-inline. Bucket A ưu tiên ship trước (META re-ground), B-F sau theo thứ tự user feedback.

---

## 1. Brainstorm

**Q1 (alignment):** Personas served = GVHD (advisor) + GVPB (reviewer) + Defense committee (khoa CNTT trường UTC). Domain = thesis V1+ ship cho academic submission khóa luận tốt nghiệp cử nhân CNTT. Khung chuẩn = `khung-bao-cao-do-an.png` nguyên bản từ trường UTC + `Quy dinh trinh bay do an tot nghiep.pdf` mandatory spec. Wave này force-multiplier per `meta-gap-priority.md` §3 — 1 rule re-ground → mọi thesis V1+ V2+ subsequent auto-comply với khung đúng.

**Q2 (trade-offs):**

| Alternative | Decision | Lý do |
|---|---|---|
| Outside-in audit re-run trước lock plan | ❌ Skip (per user 2026-05-26 chốt) | Findings cũ Wave 102 đủ (43 persona + 82 outside-in); thesis-content-standard.md đã có 9-category rubric outside-in |
| Bucket A META trước hay content backlog trước | ✅ Bucket A trước (per user) | Tránh ship content theo khung cũ rồi rework khi rule re-ground |
| 1-day blitz vs OPEN iterative | ✅ OPEN iterative (per user) | Scope lớn (META rule MAJOR + 5 content clusters); user review từng bucket |
| Bỏ Lời cam đoan vs giữ vs make optional | ✅ Bỏ hoàn toàn | User direction explicit: không có trong khung chuẩn → bỏ |
| Restructure Ch.1 3-files → 1 chương | ✅ Restructure | Khung nguyên bản chỉ liệt kê 4 chương; current 3-file split là project-internal pattern, không khớp |

**Q3 (risks):**

| Risk | Mitigation | Recovery |
|---|---|---|
| Rule v2.0.0 MAJOR bump ngược conflict với Wave 102.7.x đã ship | Document supersede chain rõ ràng trong §11 Log; existing thesis-v1.docx grandfathered nhưng sẽ re-bake trong Bucket A.4 | Bucket A.1 ship trước, A.2-A.4 sau khi user confirm v2.0.0 OK |
| Re-bake thesis-v1.docx fail (pipeline error sau khi rip sections) | Backup thesis-v1.docx trước modify pipeline; test pipeline trên small chapter trước full bake | Revert pipeline + re-plan A.2 split nhỏ hơn |
| Restructure Ch.1 3-files → 1 chương phá vỡ cross-references | Pre-flight grep `chapter-1-{ai-techniques,competitor-analysis,vn-law-methodology}` cross-refs trong code + docs; reconcile trong A.3 | Defer Ch.1 restructure sang follow-up wave if cross-refs phức tạp; bucket A ship META rule + section-removal only |
| Content backlog B-F surface conflict với khung mới | Bucket A v2.0.0 ship trước → B-F apply rule prospectively | Defer conflicting items sang follow-up gap |
| Wave OPEN quá lâu → drift | Wave header `status: in-progress` + update mỗi bucket ship; auto-archive cadence 60d post-closure per `docs-archival-cadence.md` | User-driven close: wave closure PR khi 6 bucket ship + user hài lòng |

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|---|---|---|---|---|
| **A** META khung chuẩn re-ground | 4 sub-tasks (A.1 rule v2.0.0 + A.2 pipeline rip sections + A.3 Ch.1 restructure + A.4 re-bake verify) | Coordinator-inline | ~3h | ✅ owns `thesis-content-standard.md` + `create_thesis_v1.py` + `chapter-1-*.md` |
| **B** Phụ lục cleanup (item 8+9) | A bổ sung Ch.4 hoặc bỏ + B link github + C audit bỏ | Coordinator hoặc bg-agent | ~1h | ✅ owns `create_thesis_v1.py` line 1908-1925 |
| **C** ABC sort + F9 workflow (item 10+11) | Danh mục thuật ngữ+viết tắt sort ABC + manual F9 workflow doc pre-defense | Coordinator | ~30min | ✅ owns pipeline danh mục functions + new workflow doc |
| **D** Văn nói→văn viết (item 12+14) | Pattern "Nhóm 1 — Chủ trung tâm:" rewrite + Ch.2 listing pattern lines 24/53/263/319 | Coordinator hoặc bg-agent | ~1.5h | ✅ owns `chapter-2-system-architecture.md` |
| **E** Project-jargon scrub (item 15) | BETA/GA/Phase/Wave/GAP scrub Ch.2 22 + Ch.4 9 + Ch.3 1 hit | Coordinator hoặc bg-agent | ~2h | ✅ owns Ch.2 + Ch.3 + Ch.4 MDs |
| **F** Misc action-2.md (item 17+18+19) | Figure ID folder + auto-gen diagrams (defer subset) + lookup đề cương verify bìa + focus khung release-2 | Coordinator | ~1h | ✅ owns pipeline bìa + docs convention |

Disjoint check: ✅ B owns pipeline-section-only; C owns pipeline-danh-mục-only; D owns Ch.2 narrative; E owns Ch.2-3-4 narrative scrub (sequential with D); F owns pipeline-bìa + new doc. Bucket A ships FIRST (META precedes content per user direction).

---

## 3. Scope (compact schema)

**Stake tier:** HIGH (academic deliverable + MAJOR rule bump) → model: Opus 4.7 full per `agent-model-opus-default.md`
**Cross-layer?** NO (single artifact class = thesis docs + pipeline + rule); skip Bucket 0 Foundation.

| # | Bucket | Inside item(s) | Priority | Files (glob) | Spawn order |
|:-:|---|---|:---:|---|:---:|
| 1 | **A** META re-ground | Khung chuẩn re-ground (rule v2.0.0) + remove non-khung sections (LỜI CAM ĐOAN/TÓM TẮT/ABSTRACT/NHẬN XÉT GVHD) + Ch.1 restructure | 🔴 P0 | `.claude/rules/thesis-content-standard.md` + `documents/08-thesis/create_thesis_v1.py` + `documents/08-thesis/chapter-1-*.md` | FIRST — coordinator-inline |
| 2 | **B** Phụ lục cleanup | Item 8 + 9 | 🟠 P1 | `documents/08-thesis/create_thesis_v1.py` lines 1908-1925 | After A merged |
| 3 | **C** ABC sort + F9 | Item 10 + 11 | 🟠 P1 | `create_thesis_v1.py` danh mục functions + new `documents/05-guides/operations/thesis-pre-defense-checklist.md` | After A merged |
| 4 | **D** Văn nói→văn viết | Item 12 + 14 | 🟡 P2 | `documents/08-thesis/chapter-2-system-architecture.md` lines 24/53/263/319 + Nhóm pattern | After A merged |
| 5 | **E** Project-jargon scrub | Item 15 | 🔴 P0 (academic integrity) | `documents/08-thesis/chapter-{2,3,4}-*.md` | After A merged |
| 6 | **F** Misc action-2.md | Item 17 + 18 + 19 | 🟡 P2 | `create_thesis_v1.py` bìa + new figures folder convention doc | After A merged |

### Bucket A — META khung chuẩn re-ground (DETAIL)

**A.1 Rule update** `thesis-content-standard.md` v1.1.0 → **v2.0.0 MAJOR**:
- Re-ground §1 grounding sources: khung-bao-cao-do-an.png là PRIMARY (image scan transcribed), Quy dinh trinh bay PDF SECONDARY, BAO_CAO_THUC_TAP.docx + DE_CUONG_DATN.docx TERTIARY (samples reference only)
- Drop §2 C1 sub-criteria mentioning LỜI CAM ĐOAN required (reconcile contradiction line 33+72 vs line 251+417)
- Drop §2 C2 sub-criteria mentioning Abstract VN/EN scoring (not in khung)
- Update §2 C2 chapter structure rubric khớp khung 4-chương:
  - Ch.1 "Tổng quan về bài toán và các công nghệ, công cụ" (Hiện trạng / Bài toán / Công nghệ)
  - Ch.2 "Phân tích và thiết kế hệ thống" (FR/NFR / Sơ đồ tổng thể / Use case / Quy trình / Mô hình hóa)
  - Ch.3 "Phân tích, thiết kế và triển khai hệ thống" (chương chính + BRD/ERD/Class/DB)
  - Ch.4 "Đánh giá kết quả và Kết luận" (4.1 Kết quả triển khai / 4.2 Kết quả / 4.3 So sánh đánh giá / 4.4 Kết luận+Kiến nghị+Phương hướng)
- §3 Banned patterns: thêm "LỜI CAM ĐOAN section" + "TÓM TẮT/Abstract page riêng" + "NHẬN XÉT GVHD page" vào banned (per user direction)
- §11 Log: v2.0.0 entry document supersede chain v1.0.x + v1.1.0; existing thesis-v1.docx grandfathered until Bucket A.4 re-bake

**A.2 Pipeline update** `create_thesis_v1.py`:
- Remove function `add_loi_cam_doan` (line 1165-1225)
- Remove function `add_tom_tat` (line 1230+)
- Remove function `add_abstract` (line 1255+ estimate)
- Remove function `add_nhan_xet_gvhd` (line 1190+ estimate)
- Update ordering line 1968-1972: from "NHẬN XÉT GVHD → LỜI CAM ĐOAN → TÓM TẮT → ABSTRACT → LỜI CẢM ƠN" → "LỜI CẢM ƠN" (direct, only)

**A.3 Chapter 1 restructure**:
- Verify current 3-file split: `chapter-1-ai-techniques.md` + `chapter-1-competitor-analysis.md` + `chapter-1-vn-law-methodology.md`
- Option X (preferred): merge 3 files → single `chapter-1-tong-quan.md` với 3 sub-sections (1.1 Hiện trạng + 1.2 Bài toán + 1.3 Công nghệ, công cụ)
- Option Y (fallback if scope big): keep 3 files nhưng update pipeline ordering + chapter title heading to render as 1 chương trong docx
- Cross-ref scrub: grep `chapter-1-(ai-techniques|competitor-analysis|vn-law-methodology)` trong toàn repo + reconcile

**A.4 Re-bake + verify**:
- Backup `thesis-v1.docx` → `thesis-v1.docx.bak-pre-thesis-2`
- Run `python create_thesis_v1.py` → new `thesis-v1.docx`
- Verify structure khớp khung: open via `unzip -p thesis-v1.docx word/document.xml | grep -E "LỜI CAM ĐOAN|TÓM TẮT|ABSTRACT|NHẬN XÉT GVHD" returns 0 matches`
- Verify page count target 60-80 (cử nhân) — flag if vẫn >90
- User review thesis-v1.docx → confirm khớp khung trước Bucket B-F spawn

### Bucket B-F — placeholder (detail expanded khi spawn)

Bucket B-F detail sẽ expand vào wave plan PR update khi Bucket A merge xong và user confirm sẵn sàng. Hiện tại Bucket B-F scope captured trong `2026-05-20-wave-102.7-14-item-inside-mapping.md` (5 DEFERRED + 5 MISSED inside items).

---

## 4. State-Check Evidence

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` | Reference image | `ls documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` | exists per user direction | ✅ exists |
| `documents/07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.pdf` | UTC spec | `ls ...Quy dinh...` | exists 330KB | ✅ exists |
| `.claude/rules/thesis-content-standard.md` | Rule file | Read + frontmatter | v1.1.0 shipped Wave 102.7.0 2026-05-20 | ✅ exists |
| `documents/08-thesis/create_thesis_v1.py` | Pipeline | `grep LỜI CAM ĐOAN create_thesis_v1.py` | 5 matches line 1165/1167/1175/1968/1972 | ✅ exists (need removal) |
| `documents/08-thesis/chapter-1-ai-techniques.md` | Chapter | `ls` | exists | ✅ exists |
| `documents/08-thesis/chapter-1-competitor-analysis.md` | Chapter | `ls` | exists | ✅ exists |
| `documents/08-thesis/chapter-1-vn-law-methodology.md` | Chapter | `ls` | exists | ✅ exists |
| `documents/08-thesis/chapter-1-tong-quan.md` | Chapter (target Option X) | `ls` | 0 hits | 🆕 to-be-created (Bucket A.3) |
| `documents/04-quality/audits/persona-review/2026-05-20-wave-102.7-14-item-inside-mapping.md` | Audit map | Read full | shipped 2026-05-20 | ✅ exists |
| `documents/05-guides/operations/thesis-pre-defense-checklist.md` | Workflow doc (Bucket C) | `ls` | 0 hits | 🆕 to-be-created (Bucket C) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate | Manual user-review gate |
|---|---|---|---|
| A | `python3 documents/08-thesis/create_thesis_v1.py && unzip -p documents/08-thesis/thesis-v1.docx word/document.xml \| grep -cE "LỜI CAM ĐOAN\|TÓM TẮT\|ABSTRACT\|NHẬN XÉT GVHD"` returns `0` | Rule frontmatter (rule v2.0.0) + wave-plan-completeness (this file) | User open thesis-v1.docx + confirm khớp khung |
| B | `grep -nE "Phụ lục A\|Phụ lục B\|Phụ lục C" documents/08-thesis/create_thesis_v1.py` returns expected state (B inline KẾT LUẬN; A+C removed per Wave 102.7.5) | none beyond existing | User confirm Phụ lục state |
| C | `python3 create_thesis_v1.py` re-bake + verify danh mục thuật ngữ sorted ABC | none | User confirm sort + F9 workflow doc readable |
| D | `grep -nE "^- Nhóm [0-9] — \|^Kiến trúc hệ thống bao gồm.*kitehub-" chapter-2-system-architecture.md` returns 0 (post-rewrite) | none | User confirm narrative uyển chuyển |
| E | `grep -rnE "BETA\|GA\|Phase [0-9]\|Wave [0-9]+\|GAP-[0-9]+" documents/08-thesis/chapter-*.md` returns near-0 hits | rule v2.0.0 §3 banned patterns check | User confirm Ch.2/3/4 academic clean |
| F | Per-item: figure ID convention doc readable + bìa pipeline lookup verified + release-2 scope reference clean | none | User confirm |

**Wave-level AC (close condition):**
- [ ] Bucket A merged + user confirm thesis-v1.docx re-bake khớp khung chuẩn
- [ ] Bucket B-F merged + user confirm content fixes acceptable
- [ ] Wave header `status: in-progress` → `status: complete` chỉ khi user explicit "wave done"
- [ ] Wave closure PR ship — flip status + append `wave-history.jsonl` entry + `ROADMAP.md` §🎯 Current Status entry

**Bucket A AC:**
- [ ] `thesis-content-standard.md` v2.0.0 MAJOR shipped với supersede chain documented
- [ ] `create_thesis_v1.py` không còn function/call `add_loi_cam_doan` + `add_tom_tat` + `add_abstract` + `add_nhan_xet_gvhd`
- [ ] Pipeline rebake `thesis-v1.docx` không chứa "LỜI CAM ĐOAN" / "TÓM TẮT" / "ABSTRACT" / "NHẬN XÉT GVHD" headings
- [ ] Ch.1 structure khớp khung (Option X merged OR Option Y pipeline-rendered as 1 chương)
- [ ] Page count target 60-80 trang cử nhân (soft deduct 81-90; auto-FAIL >90 per rule §4)

---

## 6. Agent Spawn Pattern

**Wave-thesis-2 spawn mode: COORDINATOR-INLINE (NOT parallel bg-agents)** per user direction "OPEN iterative":
- Bucket A-F serial, ship 1 bucket per PR, user review từng PR trước bucket tiếp theo
- KHÔNG spawn parallel `Agent` tool — wave thesis-2 ưu tiên user feedback loop từng iter thay vì 4-5 agent parallel
- Mỗi bucket = 1 branch riêng từ main (sau wave plan PR merge) → PR riêng → user review → merge → next bucket
- Exception: nếu bucket size lớn (vd Bucket E project-jargon scrub Ch.2 22 + Ch.4 9 + Ch.3 1 hit), có thể spawn 1 Opus bg-agent (per `agent-model-opus-default.md` + `agent-background-spawn-default.md`) để execute scrub, coordinator review output trước commit

**Per `feedback_wave_plan_through_pr.md`:** Wave plan PR merge FIRST (this PR) → spawn Bucket A execution sau đó.

**Per `agent-model-opus-default.md` v1.0.0:** Nếu bg-agent invoked, `model: "opus"` mandatory (Opus 4.7 1M).

---

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md` v1.0.0 + `gap-done-discipline.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

**Per-bucket PR closure:**
- Update bucket-affected GAP file Log + status (vd GAP-688 PARTIAL tracking)
- Bucket PR docs-only → auto-merge per `docs-only-pr-auto-merge.md` khi CI green
- Bucket PR touching code (pipeline / chapter MDs) → user review trước merge

**Final wave closure PR (sau Bucket A-F + user confirm hài lòng):**
- ✅ Scope-Completeness Reconciliation table (per `wave-closure-scope-completeness.md` §3) — mỗi §3 Scope item categorize ✅ DONE / 🟡 PARTIAL (gap link) / ❌ NOT-IMPLEMENTED (follow-up gap OR rationale)
- ✅ Flip wave plan frontmatter `status: in-progress` → `status: complete`
- ✅ Append `wave-history.jsonl` entry với new format (`tag_primary: thesis`, `counter: 2`, `tags_secondary: [meta, content-quality]`) per `wave-tag-numbering-convention.md` §2.5
- ✅ Update `ROADMAP.md` §🎯 Current Status Snapshot — add "Wave thesis-2 (YYYY-MM-DD): ..." entry
- ✅ GAP-688 Status update (PARTIAL → DONE nếu user confirm hài lòng entire scope)
- ✅ Run `bash scripts/prune-merged-worktrees.sh --yes` (per `post-wave-cleanup.md`) — không expected husks vì coordinator-inline mode
- ✅ `## Release Plan Progress` section trong closure PR body — thesis V2+ ship status + Phase 1 BETA impact

**Outside-in audit decision (per `outside-in-coverage-trigger.md` v1.1.0):**

User direction 2026-05-26: **SKIP outside-in re-run** per rule §4 exception "User đã trải qua outside-in (audit gần đây ≤30 ngày)" — Wave 102.7 outside-in audit 2026-05-19 (7 ngày trước) qualifies. Documented per rule mandate.

**Inside-out completeness (per `inside-out-completeness-trigger.md` v1.0.0):**

Scope source = Wave 102.7 audit map `2026-05-20-wave-102.7-14-item-inside-mapping.md` (14 items × 19 with action-2.md §4) + user inspection 2026-05-26 (khung chuẩn re-ground). 3-source pull skipped per rule §4 exception "User explicit scope locked, just execute current scope" — Wave 102.7 audit consolidation đã exhaustive cho thesis inside scope.

**Related refs:**
- Audit source: `documents/04-quality/audits/persona-review/2026-05-20-wave-102.7-14-item-inside-mapping.md` (14-item inside × Wave 102.7.x output mapping)
- Khung chuẩn primary: `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` (user-provided 2026-05-26)
- UTC spec secondary: `documents/07-archived/academic/word-reports/templates/Quy dinh trinh bay do an tot nghiep.pdf`
- Predecessor wave: `wave-2026-05-23-thesis-1-closure.md` (Wave thesis-1 closed 2026-05-23)
- Rule being bumped: `.claude/rules/thesis-content-standard.md` v1.1.0 → v2.0.0 MAJOR
- Gap: `GAP-688` (Wave 102 closure — opened parent gap cho thesis content quality)
- Inside source: `documents/action-2.md` §4 (committed PR #1585 2026-05-19)
- Apply rules: `outside-in-coverage-trigger.md` §4 exception (audit recent), `wave-tag-numbering-convention.md` (counter=2, tag_primary=thesis), `feedback_wave_plan_through_pr.md` (this PR draft)

---

## 8. Log

- **2026-05-26 (draft):** Wave plan created in response to user direction "session sửa thesis + tạo wave có tag thesis mới + chưa close wave này khi user hài lòng". 3-question AskUserQuestion chốt scope: skip outside-in re-run + Bucket A META first + OPEN iterative ship. Khung chuẩn re-ground primary source = `khung-bao-cao-do-an.png` user-provided 2026-05-26; secondary = UTC `Quy dinh trinh bay do an tot nghiep.pdf` (verified via unzip XML extraction). User-flagged "lời cam đoan không có trong khung chuẩn" → triggered rule re-ground v2.0.0 MAJOR scope. Wave OPEN iterative — Bucket A first, B-F sau, close PR final khi user confirm hài lòng.
