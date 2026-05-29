---
title: Wave rst-html-1 — RST HTML dashboard MVP (capture + landing + screenshot reuse cho manual)
status: draft
created: 2026-05-27
updated: 2026-05-27
audience: dev
tag_primary: rst-html
tags_secondary: [phase-1-beta, rst-cycle, user-manual, outside-in-audited]
counter: 1
date_launch: 2026-05-27
waves: [rst-html-1]
gaps: []
---

# Wave rst-html-1 — RST HTML dashboard MVP

**Goal:** Ship 1 HTML dashboard hiển thị RST flow test results + screenshots, đồng thời pipeline annotation reusable cho user manual artifacts.

**Trigger:** User direction 2026-05-27 — "kết quả RST được tạo ra dưới dạng HTML giống UI kits để dev kiểm soát trực quan, phần RST cực kỳ quan trọng, ảnh hưởng đến toàn bộ dự án" → re-scoped sau outside-in audit 3 agents 2026-05-27: chỉ cần 1 dashboard + screenshots tái sử dụng cho manual. RST cycle Đợt 105 bắt 5 bugs E2E miss → high-leverage tool.

**Estimated wall-clock:** ~2-3 ngày total (1 bucket sequential OR split 3 layers parallel ~1-1.5 ngày).

---

## 1. Brainstorm (5-10 min)

### Q1 — Inside-out scope (dev proposed initial)

User initial scope 2026-05-27: "RST result HTML giống UI kits, screenshots reuse cho manual" — phrase pattern matches `outside-in-coverage-trigger.md` v1.1.0 §2 row "Liệt kê features có sẵn + hỏi 'đã đủ chưa?'". Fired outside-in audit BEFORE scope lock.

### Q1 — Outside-in findings (3 Opus agents parallel, 2026-05-27)

3 agents triangulated independently per `outside-in-coverage-trigger.md` v1.1.0 §3 Bước 4:

**Agent A — Persona simulation (5 personas):** Inside-out spec "giống UI kits" = **40% fit, 60% mismatch**. UI kits = showcase/aesthetic; RST = decision-grade handoff. Cross-persona must-haves: top-of-fold SHIP/HOLD verdict + per-test drilldown (drilldown đã có sẵn trong Playwright HTML reporter native).

**Agent B — External benchmark (8 tools):** Allure Report = full ADOPT base would be ~1.5 wave (vs ~3-5 wave pure self-build). **NHƯNG** user re-scope giảm thành "1 dashboard + screenshots" → Allure overkill. **Playwright HTML reporter native + custom landing index** = best fit cho minimal scope ~0.5-1 ngày foundation.

**Agent C — Failure-mode matrix (22 cells):** 5 CRITICAL gaps trên Allure-scale plan. Re-scope giảm scope → critical cells còn relevant: VN PDF font subset (Zalo share), XSS escape (multi-tenant PDPL), screenshot retention path resolution. Other 17 cells defer.

### Q2 — Re-scope decision (user 2026-05-27)

User clarified: "tôi chỉ cần 1 dashboard cho flow test, kết quả test của RST thôi, nó sẽ hiển thị các ảnh trong quá trình RST, những ảnh này có thể tái sử dụng để làm manual".

**Drop 7 features (defer Phase 1.5+):** Allure adoption / CSV→Allure adapter / mikepenz JUnit annotation / cycle history trend / diff view cycle N vs N-1 / bug class taxonomy / executive summary business-translated.

**Keep 5 essentials:** Vietnamese narrative + VN sample data / reuse `ui_kits/_shared/colors_and_type.css` tokens / top-of-fold SHIP/HOLD verdict / per-test drilldown via Playwright HTML report / screenshot reuse cho user manual.

### Q3 — Risks + recovery

| Risk | Mitigation |
|---|---|
| Playwright `screenshot: 'on'` mặc định fire trên fail only → per-step capture sẽ cần inline `page.screenshot()` calls | Document trong spec template; reusable pattern cho future specs |
| Screenshot annotation script (Layer 3) chưa stable | Defer Layer 3 to follow-up bucket nếu Layer 1+2 đủ MVP. Manual annotation fallback (GIMP/Figma) acceptable Wave 1 |
| Folder volume budget — `documents/03-planning/waves/` đã 106 files (>50 cap) | DOCS_VOLUME_OVERRIDE trailer cited; sub-split waves backlog tracked separate |
| VN font subset Puppeteer PDF | Defer (Layer 2 ship HTML only; PDF print defer Phase 2 — current scope HTML preview đủ) |
| Manual screenshot reuse path không match `user-manual-content-standard.md` §2 row 6 mandate format | Annotation script (Layer 3) output to `documents/05-guides/user-manual/screenshots/{persona}/{topic}-step-{N}.png` exactly per rule |

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|---|---|---|---|---|
| A | Layer 1 — Capture screenshots ở RST spec steps | bg-agent | ~0.5 ngày | ✅ KC + KH spec edits |
| B | Layer 2 — Custom landing index 1 page HTML | bg-agent | ~1 ngày | ✅ NEW folder `documents/02-architecture/design-system/rst_report/` |
| C | Layer 3 — Screenshot annotation script reusable cho manual | bg-agent | ~1 ngày | ✅ NEW `scripts/render-rst-screenshots.sh` + Sharp/ImageMagick |

Bucket A unblocks Bucket B (Layer 2 needs screenshots existed to render thumbnails). Bucket C parallel với Bucket B (independent script).

---

## 3. Scope

### Bucket A — Layer 1 capture screenshots (~0.5 ngày)

**Goal:** Mọi RST E2E spec capture screenshot tại mỗi step quan trọng (login form, post-login dashboard, mỗi bounce/redirect, action confirmation).

**Changes:**
- `kiteclass/kiteclass-frontend/playwright.config.ts`: project-level override `screenshot: 'on'` cho RST spec subset (vd via project name `rst`); HOẶC inline `page.screenshot()` calls trong từng RST spec body
- `kitehub/kitehub-frontend/playwright.config.ts`: tương tự
- 2 new RST spec template files: `kiteclass-frontend/e2e/rst/{flow-name}.spec.ts` + `kitehub-frontend/e2e/rst/{flow-name}.spec.ts` — pattern cho future RST cycle specs
- Resolution: `1440×900` desktop (default cho user-manual-content-standard.md §2 row 6 mandate)
- Output path: `playwright-report/data/{spec-id}-step-{N}.png` (Playwright default)

**State-Check Evidence:**
- `kiteclass/kiteclass-frontend/playwright.config.ts` exists ✅ (verified earlier session)
- `kitehub/kitehub-frontend/playwright.config.ts` exists ✅
- Playwright `screenshot: 'on'` is valid config option ✅ (per Playwright docs)
- `page.screenshot()` API available ✅

### Bucket B — Layer 2 landing index 1 page (~1 ngày)

**Goal:** 1 HTML page show test flow results + screenshots aggregated.

**Path:** `documents/02-architecture/design-system/rst_report/index.html`

**Content:**
- **Top-of-fold** — SHIP/HOLD/INVESTIGATE banner (server-rendered from latest cycle JSON) + tổng PASS/FAIL count + cycle date
- **Flow grid table** — 1 row per RST flow:
  - Flow name (Vietnamese): vd `Đăng nhập Owner KC`, `Bounce Owner /school-admin`, `Đăng ký anonymous prospect`
  - PASS/FAIL/SKIP badge
  - Thumbnail screenshot (1st step)
  - Drill-down link → Playwright HTML report per-spec page
- **Vietnamese narrative + VN sample data** (per `vn-localization-audit-checklist.md` §2-§3)
- **CSS:** reuse `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` tokens

**Generation pattern:**
- `scripts/build-rst-landing.sh`: read Playwright JSON report output (`playwright-report/results.json`) + screenshot paths → render `index.html` via simple template
- Static HTML — không cần build pipeline

**State-Check Evidence:**
- `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` exists ✅
- Playwright `--reporter=json` flag valid ✅
- New folder `documents/02-architecture/design-system/rst_report/` — 🆕 to-be-created bởi Bucket B

### Bucket C — Layer 3 annotation script reusable cho manual (~1 ngày)

**Goal:** Script overlay annotation (mũi tên đỏ + viền vàng + số bước) lên raw screenshots → output match `user-manual-content-standard.md` §2 row 6 mandate path.

**Path:** `scripts/render-rst-screenshots.sh`

**Behavior:**
- Input: `playwright-report/data/{spec-id}-step-{N}.png`
- Process: Sharp (Node) OR ImageMagick (CLI) — overlay shapes per metadata embed trong spec test header comments:
  ```typescript
  // @rst-annotate: arrow at (450, 220), box (380-520, 200-240), step 2
  await page.screenshot({ path: 'step-2.png', fullPage: true });
  ```
- Output: `documents/05-guides/user-manual/screenshots/{persona}/{topic}-step-{N}.png`
- Filename ASCII slug (no diacritic — per `user-manual-content-standard.md` §2 + Zalo share friendly)

**Reuse path:**
- User manual MDX pages reference these annotated screenshots
- Same RST cycle artifacts feed both dashboard (Layer 2) + manual (Layer 3) without duplicate capture work

**State-Check Evidence:**
- Sharp OR ImageMagick available ✅ (Sharp đã ship trong `kitehub-frontend` Node deps; ImageMagick available via `apt-get install imagemagick` fallback)
- `documents/05-guides/user-manual/screenshots/` folder — 🆕 to-be-created or existed (per `user-manual-content-standard.md` §2 row 6)
- `scripts/render-*.sh` pattern precedent: `scripts/render-acceptance-test-xlsx.sh`, `scripts/render-env-vars.sh`, `scripts/render-user-manual-pdf.sh` (mandated) ✅

---

## 4. State-Check Evidence

| Symbol | Pattern | Grep evidence | Verdict |
|---|---|---|---|
| `playwright.config.ts` (KC) | file exists | `ls kiteclass/kiteclass-frontend/playwright.config.ts` → exists | ✅ verified |
| `playwright.config.ts` (KH) | file exists | `ls kitehub/kitehub-frontend/playwright.config.ts` → exists | ✅ verified |
| `screenshot: 'on'` Playwright config | API valid | per Playwright docs (https://playwright.dev/docs/test-use-options#screenshot) | ✅ external doc |
| `page.screenshot()` API | API exists | used trong existing specs e.g., `gap-758-persona-route-restrict.spec.ts` | ✅ verified |
| `_shared/colors_and_type.css` | file exists | `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` | ✅ verified |
| `documents/02-architecture/design-system/rst_report/` | folder | NEW — to-be-created Bucket B | 🆕 to-be-created |
| `documents/05-guides/user-manual/screenshots/` | folder | per `user-manual-content-standard.md` §2 row 6 | 🆕 to-be-created or existed |
| `vn-localization-audit-checklist.md` rule | rule exists | `.claude/rules/vn-localization-audit-checklist.md` v1.0.0 | ✅ verified |
| `user-manual-content-standard.md` rule | rule exists | `.claude/rules/user-manual-content-standard.md` v1.0.0 | ✅ verified |
| Sharp Node package | dep | `kitehub/kitehub-frontend/package.json` | ✅ verified (used by existing tooling) |

No absent-symbol references — all symbols verified ✅ or marked 🆕 to-be-created với explicit creation owner per `audit-to-gap-pipeline.md` §2.6.

---

## 5. Verification Gates

Per bucket completion:

**Bucket A done when:**
- 2 RST spec template files created (`kiteclass-frontend/e2e/rst/sample.spec.ts` + `kitehub-frontend/e2e/rst/sample.spec.ts`) demonstrating screenshot capture pattern
- Playwright config screenshot mode override documented (project-level OR spec-level inline)
- 1 sample run produces ≥3 screenshots in `playwright-report/data/`

**Bucket B done when:**
- `documents/02-architecture/design-system/rst_report/index.html` renders ≥1 sample flow row với screenshot thumbnail + PASS/FAIL badge
- `scripts/build-rst-landing.sh` runs without error → outputs valid HTML
- Visual check: open `index.html` trong browser → top-of-fold verdict + flow grid visible
- Vietnamese narrative + VN sample data verified (vd flow name `Đăng nhập Owner KC`, persona `Trần Thị Hồng`)

**Bucket C done when:**
- `scripts/render-rst-screenshots.sh` runs on sample raw screenshot → outputs annotated PNG to `documents/05-guides/user-manual/screenshots/{persona}/{topic}-step-{N}.png`
- Visual check: annotated screenshot có mũi tên đỏ + số bước theo metadata
- Pattern reusable: spec comment `// @rst-annotate: ...` parsed correctly

**Wave done when:**
- All 3 buckets verified
- 1 end-to-end demo: run RST sample spec → capture screenshots → annotate → render landing → open browser → see flow status + click drill-down to Playwright report per-spec page

---

## 6. Agent Spawn Pattern

Per `agent-model-opus-default.md` v1.0.0 — mọi agent `model: "opus"`.
Per `feedback_parallel_agent_strategy.md` — max 5 concurrent.

**Sequential A → B + C parallel:**
1. Spawn Bucket A first (background, Opus) — capture screenshots foundation
2. Wait Bucket A done → spawn B + C concurrent (2 agents parallel, both Opus)
3. Coordinator merge order: A → B → C

**Worktree isolation:** each bucket trong separate agent worktree per `feedback_worktree_absolute_path_contamination.md`.

---

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md`:
- Scope-Completeness Reconciliation table mỗi bucket (planned vs shipped)
- Post-merge sync 5 targets per `session-end-context-check.md` §4.5 (gap-status.csv + ROADMAP + wave-history.jsonl + MEMORY.md + session-handoff)
- File `wave-history.jsonl` append entry với `wave: rst-html-1` + outcome summary
- ROADMAP.md §🎯 entry referencing wave completion
- Session-handoff note `documents/03-planning/session-handoffs/2026-05-27-rst-html-1-mvp-shipped.md`

**Defer to follow-up waves:**
- `wave-rst-html-2` — Cycle history JSONL + diff view N vs N-1 + trend chart
- `wave-rst-html-3` — Bug class taxonomy + recurrence counter + executive summary business-translated
- `wave-rst-html-4` (or later) — Allure adoption nếu scope grow beyond minimal

---

## 8. Log

- **2026-05-27 (draft):** Wave plan filed in response to user direction "draft wave plan PR ngay" sau outside-in audit 3 Opus agents 2026-05-27 đồng thuận re-scope từ 6-bucket Allure-base ~1.5 wave xuống 3-layer minimal ~2-3 ngày. Per `outside-in-coverage-trigger.md` v1.1.0 Bước 5 — findings (40% fit inside-out, 60% mismatch + 3-5× cost overkill cho minimal user spec) integrated into §1 Brainstorm Q1+Q2. Per `wave-tag-numbering-convention.md` v1.0.0 — wave naming `wave-rst-html-1` (tag_primary=`rst-html`, counter=1). Per `audit-to-gap-pipeline.md` §2.6 — §4 State-Check Evidence all symbols verified or marked 🆕 to-be-created với explicit creation owner. Per `meta-gap-priority.md` §3 — META P1 (tooling force-multiplier cho mọi RST cycle subsequent + reusable cho user manual mandate per `user-manual-content-standard.md` §2 row 6). Per `docs-folder-volume-budget.md` — waves folder over-cap (106>50); DOCS_VOLUME_OVERRIDE trailer in commit body documents context.
