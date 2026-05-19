---
audience: dev
last-updated: 2026-05-19
phase: phase-1-beta
wave: 100.7-phase-3b
gaps: [GAP-646, GAP-647, GAP-655]
status: complete
---

# Thesis DOCX Pipeline — Scoping Decision (Wave 100.7 Phase 3b)

> 📅 Cập nhật lần cuối: **2026-05-19** · Wave 100.7 Phase 3b · Đọc khoảng **12 phút**

## TL;DR

- Wave 100.7 Phase 3b chốt **scoping doc + scaffold placeholder** cho thesis DOCX pipeline (GAP-646); Step 1-3 implementation defer cho focused session sau.
- Khuyến nghị chọn **Apache POI XWPF (Java) — Edit-Fill pipeline** trên `.claude/skills/document-generation/word/` đã có. Lý do: on-stack (Java 17 + Maven đã ready), zero install cost, có ADR-019 Facade + Strategy đã chứng minh, VN typography (TNR 13pt + A4 + margin 3-2-2 cm) chính xác qua XWPFParagraph + CTPageSz/CTPageMar.
- Pandoc + LibreOffice cùng chưa cài (`which` trả về exit 127); cài thêm tooling = scope creep cho thesis-only task, lợi ích thấp hơn tận dụng skill foundation hiện có.
- Binary `.docx` template (`assets/thesis-template.docx`) authored offline trong LibreOffice/Word, commit cùng MD5/SHA hash trong README để track binary change qua diff-opacity.
- Phase 3b closure = decision doc + scaffold script + GAP-646 flip PARTIAL 20%. Step 1-3 = 3-4 sub-tasks ~6-8h focused session.

## 1. Background & problem statement

Thesis defense Q4 2026 cần deliverable cuối cùng = file DOCX báo cáo (~80-120 trang) format chuẩn VN CS theses (HUST/UIT/UET style). Wave 100.7 Phase 2 đã ship 7 chapter MD source (`chapter-1-*.md` 4 files + `chapter-2-system-architecture.md` + `chapter-3-implementation.md` + `chapter-4-deployment-results.md`); Phase 3a (parallel agent) đang consolidate bibliography IEEE format từ `references/bibliography.md`.

Phase 3b nhận scope **scoping + scaffold** cho phần còn lại — DOCX assembly pipeline. GAP-646 §Proposed Fix nêu 3 step:

1. **Step 1 — Thesis template DOCX**: `assets/thesis-template.docx` với cover trang + TOC + 7-chapter shell + bibliography section + appendix; TNR 13pt body / 14pt heading, A4, margins 3cm top + 2cm sides + 2cm bottom (per `khung-chuan/khung-bao-cao-do-an.png` reference).
2. **Step 2 — Chapter assembly script**: `scripts/assemble-thesis-docx.sh` đọc `chapter-mapping.md`, walk source paths, inject figures + citation rendering, output `documents/08-thesis/build/thesis-vN.docx`.
3. **Step 3 — Skill extension**: `.claude/skills/document-generation/word/SKILL.md` add §Thesis pipeline với `templateId = thesis-report` + `ThesisReportBuilder` extends teacher-contract Create pipeline.

**Phase 3b deferred lý do**: binary DOCX template authoring = diff-opaque (Git không track meaningful diff cho `.docx` binary); cần dedicated focused session để (a) author template offline trong LibreOffice/Word, (b) iterate visual review vs `khung-chuan/` reference image, (c) commit + validate render qua POI read-back. Parallel với Phase 3a bibliography assembly Phase 3b chỉ ship scoping + scaffold để (a) unblock decision quyết định approach + (b) lock down skill extension contract cho focused session.

## 2. Option matrix — 3-tool comparison

Tooling inventory empirical (verified `which` commands 2026-05-19 Phase 3b worktree):

| Tool | Available? | Java 17 | Maven | Notes |
|---|:---:|:---:|:---:|---|
| **Apache POI XWPF** | ✅ (via Maven) | ✅ `/usr/bin/java` openjdk 17.0.18 | ✅ `/usr/bin/mvn` | Already on-stack qua `.claude/skills/document-generation/word/` |
| **Pandoc** | ❌ `which` exit 127 | N/A | N/A | Would require `apt install pandoc` (~80MB transitive) |
| **LibreOffice headless** | ❌ `which soffice/libreoffice` exit 127 | N/A | N/A | Would require `apt install libreoffice` (~600MB) |
| **python-docx** | ❌ `ModuleNotFoundError` | N/A | N/A | Would require pip install (alternative Python path) |

### 2.1 Comparison matrix

| Tiêu chí | Apache POI XWPF (Java) | Pandoc + reference.docx | LibreOffice headless |
|---|---|---|---|
| **Maintainability** | ✅ Cao — POI Java code đã có teacher-contract example trong skill; team đã quen POI patterns | ⚠️ Trung bình — Pandoc filters (Lua/Haskell) khó debug; reference.docx binary cũng diff-opaque | ⚠️ Trung bình — `soffice --convert-to docx` là CLI black-box; debug = chạy local + visual diff |
| **Git diff friendliness** | ✅ Excellent — Java source code diff-able; template binary diff-opaque như mọi approach | ⚠️ Mixed — `.md` source diff-able nhưng reference.docx + Lua filter binary/scripted | ⚠️ Mixed — MD source diff-able; conversion config trong shell script |
| **VN typography fidelity** | ✅ **Highest** — direct CTPageSz/CTPageMar/CTFonts control; TNR 13pt + A4 + margins exact match (POI test) | ⚠️ Medium — Pandoc styles bám reference.docx; fine-tune phải sửa reference template; PageBreak/TOC sometimes drifts | ⚠️ Medium — LO auto-conversion may lose styles từ MD→DOCX; better cho ODT pipeline |
| **Setup cost** | ✅ **Zero** — Java 17 + Maven đã ready, skill foundation đã có ADR-019 Facade | ❌ ~5 min install + ~80MB disk + reference.docx authoring | ❌ ~10 min install + ~600MB disk |
| **Skill extension path** | ✅ Linear — add `ThesisReportBuilder extends Builder` pattern; Wave 5 GAP-208 đã track skill expansion roadmap | ⚠️ Tangential — Pandoc tooling chưa có skill foundation; thêm subagent loại mới | ⚠️ Tangential — LO chưa có skill foundation |
| **Recommended use case** | Complex programmatic DOCX generation với precise typography control + multi-template registry | Markdown-heavy academic pipeline với template-driven styling + light scripting | Quick batch convert MD→DOCX khi không cần fidelity control |
| **VN academic norm compliance** | ✅ POI test verifies CTPageSz=A4 + margins=3-2-2cm + CTFonts=TNR | ⚠️ Phải tune reference.docx manually + Pandoc YAML metadata | ⚠️ Phải override default LO styles |
| **Citation rendering** | ⚠️ Custom — `{{cite:GAP-XXX}}` → `[N]` via post-processing pass (need bibliography lookup map) | ✅ Native — pandoc-citeproc + CSL style file (IEEE.csl available) | ❌ No native citation engine |
| **Figure injection** | ✅ POI XWPFRun.addPicture với caption paragraph + numbering | ✅ MD `![caption](img.png)` → Pandoc auto-converts | ⚠️ MD pictures convert nhưng caption/numbering manual |
| **TOC auto-generation** | ✅ POI `XWPFAbstractNum` + `CTSdtBlock` field code | ✅ Pandoc `--toc` flag + reference.docx TOC placeholder | ⚠️ LO may insert TOC but format inconsistent |
| **Cross-reference (Figure 3.1, Table 4.2)** | ⚠️ Custom — manual numbering pass over text | ✅ Pandoc `@fig:label` syntax | ❌ No native cross-ref |
| **Author workflow** | Programmatic — Java code + binary template | Markdown editor + reference.docx authored once | Markdown editor + LO config |
| **Cost-of-next-miss (if pipeline breaks)** | Java exception stack trace → debug straightforward | Pandoc/Lua opaque error → debug 2-3x longer | LO CLI exit code only → debug hardest |

### 2.2 Counter-argument cho Pandoc

Pandoc có native citation engine (citeproc + CSL) + auto cross-reference — 2 feature mà POI yêu cầu custom implementation. Tuy nhiên:

- VN edu pipeline historically uses Word-direct workflow; supervisors comment qua Word Track Changes. Pandoc output cần extra render check vs supervisor's Word render → reduces native advantage.
- POI custom citation pass (lookup `{{cite:GAP-XXX}}` → `[N]` via `bibliography.md` parsed map) là deterministic ~50 LOC Java; cost-benefit acceptable.
- Pandoc reference.docx authoring task EQUAL hardness to POI XWPF template authoring — cả 2 đều binary-opaque.

### 2.3 Counter-argument cho LibreOffice

LibreOffice headless `soffice --convert-to docx input.md` chỉ thuần MD→DOCX conversion; KHÔNG xử lý:
- Multi-source assembly (chapter mapping → concatenated DOCX)
- Figure caption + numbering programmatically
- Bibliography section auto-format
- Cover page với placeholder injection

→ LO chỉ là 1 building block, không phải full pipeline. Loại bỏ khỏi consideration.

## 3. Decision

**Chọn Apache POI XWPF (Java) — Edit-Fill pipeline** mở rộng từ `.claude/skills/document-generation/word/`.

### 3.1 Rationale

1. **Zero setup cost** — Java 17 + Maven empirically verified ready trên Phase 3b worktree; không cần install bổ sung (Pandoc 80MB / LibreOffice 600MB đều scope creep cho thesis-only task).
2. **Skill foundation match** — Wave 5 đã ship `DocumentGenerationService` (ADR-019 Facade + Strategy pattern), `DocxGenerator` Builder pattern, A4 + margin control verified qua `TeacherContractBuilder`. Adding `ThesisReportBuilder` = linear extension; same patterns, same testing approach.
3. **VN typography fidelity highest** — POI CTPageSz/CTPageMar/CTFonts cho fine-grained control match `khung-chuan/khung-bao-cao-do-an.png` reference (TNR 13pt + A4 + margin 3-2-2 cm). Pandoc reference.docx cần fine-tune manual qua trial-and-error; LO conversion drift.
4. **Edit-Fill pipeline mới (vs Create only Wave 5)** — Wave 5 skill scope chỉ Create; Step 2 thesis assembly cần Edit-Fill (load existing `assets/thesis-template.docx` + substitute placeholders). Đây là natural expansion theo `docx-3-pipelines.md` taxonomy (`MiniMax minimax-docx` 3-pipeline taxonomy đã document Edit-Fill như "later wave").
5. **Citation pass deterministic** — `{{cite:GAP-XXX}}` placeholder → `[N]` IEEE format dùng `bibliography.md` parsed map (Phase 3a deliverable). ~50 LOC Java, predictable, testable.
6. **Skill expansion roadmap** — GAP-208 (Wave 7 scope) đã track skill expansion beyond teacher-contract; thesis pipeline aligns roadmap.

### 3.2 Trade-offs accepted

| Trade-off | Impact | Mitigation |
|---|---|---|
| Citation rendering custom (vs Pandoc native citeproc) | ~50 LOC extra Java | Deterministic + testable; covered bằng JUnit fixture sample bibliography |
| Cross-reference numbering manual (Figure 3.1, Table 4.2) | Need explicit numbering pass | Java `Map<String, Integer> figureCounters` pre-walk; 1 pass |
| Binary template diff-opaque | Hard to review template change qua PR | §5 Binary authoring workflow address |
| Phase 3b ships scoping only, not full implementation | Step 1-3 defer focused session | GAP-646 PARTIAL 20% with clear AC remaining |

### 3.3 Alternatives rejected (recorded)

| Option | Reason rejected |
|---|---|
| Pandoc + reference.docx | Setup cost (80MB + reference.docx authoring) without clear typography fidelity advantage; VN academic Word-direct workflow reduces native citeproc benefit |
| LibreOffice headless | Black-box conversion; no multi-source assembly capability; 600MB install scope creep |
| python-docx | Off-stack (project = Java); duplicates POI capability without skill foundation; would split DocumentGenerationService responsibilities |
| Direct OOXML manipulation (zip + XML) | Brittle vs POI library abstractions; POI exists để avoid this |
| LaTeX → DOCX pipeline | VN academic Word-direct workflow incompatible; supervisor cần native DOCX cho Track Changes |

## 4. Implementation roadmap — focused session sau

Khi user trigger focused session để execute Step 1-3, break thành 4 sub-tasks (effort estimates assume single-session focused work):

### 4.1 Sub-task A — Template DOCX authoring (offline, ~2-3h)

**Owner**: dev (human) + Claude pair với LibreOffice/Word session.

**Steps**:
1. Open `documents/08-thesis/khung-chuan/khung-bao-cao-do-an.png` reference image side-by-side
2. Author `assets/thesis-template.docx` offline trong LibreOffice/Word:
   - Cover trang: title VN, tên SV, MSSV, GVHD, năm, trường — placeholder pattern `{{var.title}}`, `{{var.student_name}}`, `{{var.student_id}}`, `{{var.supervisor}}`, `{{var.year}}`, `{{var.school}}`
   - TOC placeholder (Word TOC field code, auto-update on render)
   - 7 chapter shell: H1 heading + placeholder `{{chapter.N.body}}` cho mỗi chapter
   - Bibliography section: H1 + placeholder `{{bibliography.entries}}` (Phase 3a output injected here)
   - Appendix section: H1 + sub-section placeholders
3. Apply VN academic styling:
   - Body: Times New Roman 13pt, justified, line-height 1.5
   - H1 heading: TNR 14pt bold, page-break-before
   - H2-H3: TNR 13pt bold, indent
   - Page: A4 portrait (210×297mm = 11906×16838 twips)
   - Margins: 3cm top + 2cm right + 2cm bottom + 3cm left (HUST/UIT norm — note: 3cm left vs 2cm right để binding gutter)
4. Save as `assets/thesis-template.docx`
5. Compute SHA256 hash + commit hash trong `assets/README.md` table:

   ```markdown
   | File | SHA256 | Authored | Note |
   |---|---|---|---|
   | thesis-template.docx | `<hash>` | 2026-XX-XX | VN CS thesis HUST/UIT norm |
   ```

6. Visual review checklist:
   - [ ] Cover trang match `khung-chuan/` reference
   - [ ] TOC field code generates trên `Ctrl+A → F9` test
   - [ ] Page count consistent (12 trang skeleton)
   - [ ] Diacritics render đúng (Times New Roman handles Unicode natively, no font substitution)

**Effort**: 2-3h (authoring + visual iterate vs reference image + hash documentation).

### 4.2 Sub-task B — ThesisReportBuilder Java (~2h)

**Owner**: Claude với JUnit testing.

**Steps**:
1. Extend `.claude/skills/document-generation/word/SKILL.md` §3-pipeline table mark Edit-Fill = ✅ scope thesis-pipeline.
2. Create `ThesisReportBuilder.java` extending teacher-contract pattern:
   - Constructor reads `assets/thesis-template.docx` via `XWPFDocument(FileInputStream)`
   - `setVariable(String key, String value)` substitutes `{{var.key}}` placeholders bằng XWPFParagraph text replacement
   - `setChapterBody(int n, String markdown)` injects chapter content (MD → XWPF paragraphs conversion qua simple parser hoặc reuse Pandoc nếu available later)
   - `setBibliography(List<BibliographyEntry>)` injects entries vào `{{bibliography.entries}}` placeholder
   - `render() → byte[]` XWPFDocument.write to ByteArrayOutputStream
3. Wire qua `DocxGenerator` routing: `templateId = "thesis-report"` → `ThesisReportBuilder`.
4. JUnit fixtures:
   - `thesis_empty_render.docx` — render skeleton với empty placeholders → 12 trang
   - `thesis_sample_chapter_1.docx` — inject sample chapter MD → verify TNR/A4/margin retained
   - `thesis_bibliography_5_entries.docx` — verify IEEE format `[1] Author, "Title," Source, Year.` render đúng
5. Read-back validation: load output `.docx` via `XWPFDocument(new FileInputStream(out.docx))` + assert CTPageSz=A4 + CTPageMar={3cm,2cm,2cm,3cm} + CTFonts=Times New Roman.

**Effort**: 2h (Java code + JUnit + integration).

### 4.3 Sub-task C — assemble-thesis-docx.sh chapter walking (~1-2h)

**Owner**: Claude.

**Steps**:
1. Replace scaffold placeholder `scripts/assemble-thesis-docx.sh` với production implementation:
   - Parse `documents/08-thesis/chapter-mapping.md` để get chapter → source files mapping
   - For each chapter N:
     - Walk source paths, concatenate MD content
     - Process `![caption](img.png)` → inject figures với numbering (Figure N.M)
     - Process `{{cite:GAP-XXX}}` placeholders → lookup `bibliography.md` map → replace với `[N]`
     - Process `Table: caption` → numbering (Table N.M)
   - Call Java entry point: `java -jar kitehub-core.jar assemble-thesis --chapter-N=path1.md,path2.md --bibliography=refs.md --output=build/thesis-vN.docx`
2. Support `--dry-run` flag — validate inputs without writing output (CI smoke per AC #6).
3. Support `--chapters=1,3,5` flag — selective render cho iterative drafting.
4. Output: `documents/08-thesis/build/thesis-vN.docx` (gitignored per `test-artifact-format-standard.md` §4.2 PDF gitignored pattern; thesis-build/ similar).

**Effort**: 1-2h (script + smoke testing).

### 4.4 Sub-task D — Skill SKILL.md extension + reference doc (~1h)

**Owner**: Claude.

**Steps**:
1. Update `.claude/skills/document-generation/word/SKILL.md`:
   - §When to use: add "Generate VN CS thesis DOCX với 7-chapter shell + IEEE bibliography"
   - §3-pipeline routing: flip Edit-Fill row to ✅ shipped (Wave 100.7 follow-up)
   - §How it works: add §"Thesis pipeline" section describing `templateId = thesis-report`
   - §Gotchas: add thesis-specific (e.g., "3cm left margin for binding gutter on VN academic standard")
   - §Reference: link `reference/thesis-pipeline.md` (new — sub-task D)
2. Create `.claude/skills/document-generation/word/reference/thesis-pipeline.md`:
   - Architecture: ThesisReportBuilder class diagram
   - Placeholder taxonomy: `{{var.X}}` / `{{chapter.N.body}}` / `{{bibliography.entries}}` / `{{cite:GAP-XXX}}`
   - Citation rendering algorithm: MD parser → `{{cite:...}}` → bibliography map lookup → `[N]` IEEE
   - Figure numbering: pre-walk pass + counter Map
   - Cross-reference: future scope (note for next iteration)
3. Append `.claude/skills/_README-skills-index.md` row tracking thesis-pipeline extension.

**Effort**: 1h (docs + index sync).

### 4.5 Total effort estimate

| Sub-task | Effort |
|---|---|
| A — Template authoring | 2-3h |
| B — ThesisReportBuilder Java | 2h |
| C — Assembly script | 1-2h |
| D — Skill docs | 1h |
| **Total** | **6-8h focused session** |

Acceptable cho single focused dedicated session. Suggest schedule sau Phase 3a bibliography ship (cần `bibliography.md` parsed format as input).

## 5. Binary template authoring approach

`.docx` là OOXML zip; Git `diff` không reveal meaningful change. Đây là constraint inherent của binary office formats, không phải POI-specific. Address qua workflow:

### 5.1 Authoring cycle

```
1. Open assets/thesis-template.docx trong LibreOffice/Word
2. Edit visually (add section / change style / update placeholder)
3. File → Save (DOCX format, preserve)
4. Compute SHA256:
     sha256sum assets/thesis-template.docx
5. Update assets/README.md table:
     | thesis-template.docx | <new-hash> | <date> | <change-note> |
6. git add assets/thesis-template.docx assets/README.md
7. git commit -m "docs(thesis): update template — <change-note>; SHA256: <hash>"
```

### 5.2 Diff-opacity mitigation

Since `git diff assets/thesis-template.docx` shows `Binary files differ`, mitigate qua:

1. **SHA256 manifest** trong `assets/README.md` — reviewer can verify file integrity + track historical hashes
2. **Change note column** trong README table — author documents WHAT changed semantically ("added cover page logo placeholder", "fixed H2 indent 0.5cm")
3. **POI read-back validation** — `ThesisReportBuilder` JUnit fixture loads template + asserts structural invariants (12 page skeleton, 7 H1 headings, A4 + margins) → CI catches structural regression
4. **Visual snapshot fallback** — author exports PDF preview `assets/thesis-template-preview.pdf` (gitignored) + reviewer compares manually if structural change suspected
5. **Atomic edit commits** — KHÔNG bundle template edit với code change; isolate cho easier git blame + revert

### 5.3 PR review checklist for template change

When PR touches `assets/thesis-template.docx`:

- [ ] SHA256 hash updated trong `assets/README.md`?
- [ ] Change note column filled với semantic description?
- [ ] POI read-back JUnit still passes (CI green)?
- [ ] Visual snapshot exported nếu structural change (optional but recommended)?
- [ ] Atomic commit (no code change bundled)?

### 5.4 Backup + history preservation

Binary file size ~50-200KB; Git LFS overkill. Plain Git OK. Note: revert tới previous template version qua `git checkout <SHA> -- assets/thesis-template.docx` works straightforward.

## 6. Acceptance criteria — Phase 3b closure

Phase 3b counts as **complete** when ALL items below satisfied:

- [x] `documents/08-thesis/docx-pipeline-scoping.md` shipped (this file)
- [x] `scripts/assemble-thesis-docx.sh` scaffold placeholder shipped (chmod +x, exit 0, references decision doc)
- [x] GAP-646 status flipped 🔵 OPEN → 🟡 PARTIAL với completion_pct=20
- [x] GAP-646 `## Current State (verified 2026-05-19)` section added documenting Phase 3b shipped artifacts
- [x] GAP-646 Log entry appended với Phase 3b summary
- [x] `documents/04-quality/gaps/gap-status.csv` GAP-646 row updated: status=PARTIAL, completion_pct=20, last_verified=2026-05-19, notes appended
- [x] No file move (PARTIAL ≠ DONE per `gap-folder-organization.md` v2.0.0 §3.2 — file stays at `phase-1-beta/GAP-646-thesis-docx-pipeline.md`)
- [x] PR created với clear scope summary + recommended option + deferred items

**NOT in scope (defer focused session):**

- [ ] AC #1 `assets/thesis-template.docx` template authoring (Sub-task A)
- [ ] AC #2 `scripts/assemble-thesis-docx.sh` production implementation (Sub-task C)
- [ ] AC #3 Sample render với VN typography verification (Sub-task B JUnit)
- [ ] AC #4 Bibliography section auto-format (depends GAP-647 Phase 3a output)
- [ ] AC #5 `chapter-mapping.md` placeholder pattern update (Sub-task D)
- [ ] AC #6 CI smoke `--dry-run` exit 0 (Sub-task C deliverable — current scaffold satisfies trivially)
- [ ] AC #7 Cross-reference numbering rendered (Sub-task C deliverable)

Phase 3b PARTIAL 20% breakdown:
- Scoping decision doc: 60% of Phase 3b scope ≈ 12% project completion
- Scaffold placeholder: 30% of Phase 3b scope ≈ 6% project completion  
- GAP status sync: 10% of Phase 3b scope ≈ 2% project completion
- **Total: 20%**

Remaining 80% = focused session Step 1-3 implementation (6-8h estimated).

## 7. Cross-references

| Artifact | Type | Relevance |
|---|---|---|
| [GAP-646](../04-quality/gaps/phase-1-beta/GAP-646-thesis-docx-pipeline.md) | Parent gap | This scoping doc closes 20% của AC scope |
| [GAP-647 thesis-bibliography-ieee](../04-quality/gaps/) | Paired Phase 3a | Bibliography parsed format = input cho `setBibliography()` injection |
| [GAP-655 citation-extract](../04-quality/gaps/) | Future scope | Auto-extract `{{cite:GAP-XXX}}` placeholders từ chapter MD source |
| [ADR-019 Facade + Strategy pattern](../02-architecture/adr/ADR-019-*.md) | Architecture | DocumentGenerationService reuses pattern; ThesisReportBuilder follows |
| [Skill: document-generation/word](../../.claude/skills/document-generation/word/SKILL.md) | Skill foundation | Wave 5 teacher-contract Create pipeline; thesis = Edit-Fill extension |
| [Reference: docx-3-pipelines](../../.claude/skills/document-generation/word/reference/docx-3-pipelines.md) | Pipeline taxonomy | Edit-Fill semantic definition |
| [GAP-208 skill template expansion](../04-quality/gaps/) | Wave 7 roadmap | Tracks beyond-teacher-contract templates; thesis aligned |
| [khung-chuan/khung-bao-cao-do-an.png](khung-chuan/khung-bao-cao-do-an.png) | Reference image | VN CS thesis layout norm (HUST/UIT style) |
| [chapter-mapping.md](chapter-mapping.md) | Chapter source mapping | Input cho assemble script Sub-task C |
| [references/bibliography.md](references/bibliography.md) | Bibliography source | Phase 3a output → ThesisReportBuilder input |
| [references/CITATION-STYLE.md](references/CITATION-STYLE.md) | IEEE citation format | Defines `[N] Author, "Title," Source, Year.` rendering |

## 8. Risks + open questions

| Risk | Impact | Mitigation |
|---|---|---|
| Template authoring iterates nhiều cycles cycle (visual review vs reference image) | Sub-task A effort tăng 3h → 5h | Reserve focused session ≥6h block; iterate trong session, không cross-session |
| Phase 3a bibliography format khác expected (deferred decision Phase 3a) | ThesisReportBuilder injection contract mismatch | Phase 3a output validated qua Sub-task B JUnit fixture; revise contract nếu cần |
| POI XWPF version compatibility với template authored Word 365 / LO 7.x | Rare crash on load | Pin POI version 5.2.x trong `pom.xml`; test load template từ both Word 365 + LO 7.x in JUnit |
| Citation rendering edge case: GAP-XXX reference without bibliography entry | Render `[?]` placeholder hoặc throw | Throw RuntimeException với clear message; force author to add bibliography entry |
| Cross-reference (Figure N.M) numbering across chapters | Off-by-one trong multi-chapter assembly | Single-pass pre-walk counters per chapter; JUnit fixture verifies Figure 3.1 + 3.2 + 4.1 ordering |
| Binary template change review burden | Slow PR cycles cho template iteration | Sub-task A authoring done once; subsequent edits rare (likely <5/year) |

Open questions for focused session:

1. Pandoc availability post-focused-session install? If easy install OK, consider hybrid POI + Pandoc (Pandoc cho MD→DOCX paragraph conversion, POI cho template + placeholders). Decision deferred.
2. Visual diff tool cho template review? Consider `docx2txt` CLI hoặc Python `python-docx` script để extract text-only diff. Defer to focused session.
3. PDF preview generation post-assemble? Useful cho supervisor share. Defer — out of Phase 3b scope.

## 9. Reviewer checklist (Wave 100.7 Phase 3b acceptance)

- [ ] Scoping doc complete (sections 1-8 present)?
- [ ] Recommended option clearly stated với rationale (§3.1)?
- [ ] Alternatives recorded với reject reason (§3.3)?
- [ ] Implementation roadmap actionable (§4 sub-tasks A-D với effort estimates)?
- [ ] Binary template authoring workflow addressed diff-opacity (§5)?
- [ ] AC for Phase 3b closure listed (§6)?
- [ ] Cross-references complete (§7)?
- [ ] Risks identified (§8)?
- [ ] Vietnamese narrative + English technical identifiers per `dev-readable-doc-language.md` §2?
- [ ] `last-updated: 2026-05-19` per `session-currentdate-check.md`?
