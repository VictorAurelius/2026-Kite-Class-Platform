---
paths:
  - documents/02-architecture/design-system/**
  - kiteclass/kiteclass-frontend/src/**
  - kitehub/kitehub-frontend/src/**
---

# Design-Source → Implementation Parity — đừng drop affordance khi port

**Priority:** 🟠 MANDATORY — implementation-fidelity governance preventing dropped-affordance class
**Version:** 1.1.0
**Created:** 2026-06-01
**Last-Reviewed:** 2026-06-01
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; adds §3 row 7 "Runtime effect" + §3.2 runtime click-verify mandate — tighten constraint (static wired-check no longer sufficient cho interactive affordance), triggered by own round-1 INERT miss same session (PR #2028 wired but visually inert; browser test bắt; PR #2030 fixed); paired §6 self-test update per §6.5 Enforcement Parity Mandate; no constraint loosening — closes "wired ≠ working" sub-class. v1.0.0 (kept): new rule với built-in enforcement (reviewer-checklist + worked self-test trên marketing-site ThemeSwitcher dropped-wiring 2026-06-01) per §6.5; META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi PR implement/port một design source (Claude Design artifact / Figma / mockup HTML / approved screen design) vào target code (ui_kits HTML kit, production page.tsx, React component). Out-of-scope: greenfield code không có design source upstream; refactor thuần (no design reference); backend-only changes.

---

## 1. The Rule

> **Khi implement/port một design source vào code, MUST verify parity từng affordance: mọi control / selector / toggle / state / section có trong source PHẢI có trong target — HOẶC document inline "dropped + lý do".** Copy component definition mà không wire vào render = dead code = parity miss.

Design source = artifact upstream định nghĩa UI/UX (Claude Design export, Figma frame, HTML mockup, approved screen). Implementation = code render thật. Parity = target hiển thị + hoạt động đúng những gì source thể hiện. Drop silently (copy code nhưng không wire / bỏ section / bỏ state) = regression mà reviewer thường không bắt vì "code có rồi".

---

## 2. Trigger pattern — khi nào rule fire

| Pattern | Ví dụ |
|---|---|
| **Port Claude Design / Figma → ui_kits kit** | image-1.png "Landing kit Personal" 4-option theme switcher → `marketing-site/` |
| **Port ui_kits mockup → production page.tsx** | `ui_kits/kiteclass-pro-v2/` → `(public)/page.tsx` |
| **Implement approved screen design → component** | Figma screen → React component |
| **Copy primitives/component lib từ source** | `primitives.jsx` ThemeSwitcher copied — nhưng wire vào render chưa? |

Rule **KHÔNG** fire khi: greenfield code (no upstream design), refactor không đổi behavior, backend-only.

---

## 3. Required parity checklist (per implementation)

Trước khi flip "implement xong" / merge PR port design source, verify TỪNG dòng:

| # | Parity dimension | Cách verify |
|---|---|---|
| 1 | **Interactive controls** | Mọi button / toggle / selector / segmented-control / switch trong source → render + wire onClick/onChange trong target |
| 2 | **States / variants** | Mọi state source thể hiện (hover / active / success / error / loading / empty) → có trong target |
| 3 | **Sections / blocks** | Mọi section của source (hero / features / trust / footer / CTA...) → present, không thiếu |
| 4 | **Demo affordances** | Theme switcher / persona toggle / locale picker (design-preview controls) → wire vào kit render (kit context); production strip + document |
| 5 | **Copied-but-unwired check** | Mọi component **định nghĩa** trong primitives/lib → grep **usage** trong render; định nghĩa mà không gọi = dead code = FAIL |
| 6 | **Content fidelity** | Hero copy / labels / sample data khớp source (không placeholder leftover) |
| 7 | **Runtime effect** (interactive affordance) | Wired ≠ working. Affordance có effect (toggle/selector/switcher) PHẢI runtime click-verify: chạy thật (browser/headless) → click → **observe** target thay đổi. Static "đã wire" KHÔNG đủ. |

### 3.1 Bằng chứng inline (PR body)

```markdown
## Design-source parity (per design-source-implementation-parity.md §3)

**Source:** <link/path artifact — Claude Design / Figma frame / ui_kits kit>
**Target:** <file path implementation>

| Affordance trong source | Trong target? | Verdict |
|---|---|---|
| <control/state/section> | wired / dropped | ✅ / ⚠️ dropped: <reason> |
```

Mỗi `⚠️ dropped` PHẢI có lý do (vd "production strip demo affordance — không hiển thị cho end-user") + nếu dropped là regression → file follow-up gap.

### 3.2 Runtime click-verify mandate (interactive affordance) — added v1.1.0

> **Affordance có visible effect (theme switcher / tab / toggle / accordion / filter) — KHÔNG được claim "parity ✅" bằng static check (grep usage / "đã wire"). MUST chạy thật + click + observe target đổi.**

Lý do (incident 2026-06-01, §6 self-test): ThemeSwitcher đã wire đúng (class + var đổi) nhưng target visuals đọc var KHÁC (`--hero-from` thay vì `--theme-*`) → click **vô hình**. Static check "wired ✅" PASS sai; chỉ runtime click→observe mới bắt.

Required evidence trong PR body:

```markdown
**Runtime verify:** <browser/headless tool> → click <affordance> → observed <target> changed:
- <variant 1>: <before> → <after> ✅
```

Cross-ref `feature-ship-runtime-walk-mandate.md` §3 — runtime walk; rule này áp riêng cho interactive affordance khi port design source.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Copy component vào primitives.jsx + coi như "đã implement" | Grep usage: component phải được GỌI trong render, không chỉ định nghĩa |
| Drop control "vì demo only" mà không document | "demo only" trong KIT context = vẫn render (preview affordance); document nếu strip |
| Port hero/section nhưng thiếu 1 selector source có | Liệt kê affordance source, đối chiếu 1-1 |
| "Code có sẵn rồi" làm bằng chứng implement | Dead code ≠ implemented; verify render thật |
| Skip parity vì "design source là demo" | Source là spec; drop = cần lý do inline |
| Trust visual giống nhau ở hero → bỏ qua phần dưới | Walk toàn bộ source top→bottom đối chiếu |

---

## 5. Override mechanism

Genuine drop hợp lệ (vd production strip demo control, source affordance không phù hợp target context):

```
git commit -m "...
DESIGN_PARITY_DROP: <affordance> — <reason — e.g. 'theme switcher = kit-only demo, production end-user không chọn theme'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review.

---

## 6. Worked self-test — marketing-site ThemeSwitcher (2026-06-01)

**Scenario:** Claude Design source (image-1.png "Landing kit — Personal") có panel "Chủ đề theo giáo viên" — **4 option** (KiteClass mặc định / Cô Hà / Cô Khánh / Thầy Nhì). Implement vào `ui_kits/marketing-site/`.

**Apply §3 checklist retroactively tới bản committed (pre-fix):**

| # | Dimension | Trạng thái pre-fix | Verdict |
|---|---|---|---|
| 1 | ThemeSwitcher control | Định nghĩa `primitives.jsx:115` NHƯNG không gọi trong `ProductLanding` | ❌ FAIL |
| 5 | Copied-but-unwired | `ThemeSwitcher` + `THEMES` + `.theme-*` CSS đều copy; usage grep = rỗng | ❌ FAIL (dead code) |
| 2 | Theme states | `.theme-ha/.theme-khanh/.theme-nhi` định nghĩa nhưng root `.ms-main` không apply `themeClass` | ❌ FAIL |

→ Rule fires correctly: §3 row 1+2+5 đều FAIL trên bản committed. Affordance 4-option của source bị drop silently lúc port — reviewer không bắt vì "code có trong primitives".

**Fix round 1 (PR #2028):** wire `<ThemeSwitcher>` + `themeClass(theme)` lên `.ms-main`. Static check PASS (row 1+5 ✅). **NHƯNG claim "deployed ✅" bằng grep HTML — KHÔNG runtime click-verify.**

**Round-1 INERT — §3 row 7 lý do tồn tại (added v1.1.0):** browser self-test sau đó lộ ra: click đổi `.theme-*` class + `--theme-*` vars NHƯNG marketing hero đọc `--hero-from/to` (hardcode) → click **vô hình**. Row 1+5 static PASS sai; chỉ **runtime click→observe** (row 7) mới bắt.

**Fix round 2 (PR #2030):** `.theme-*` override `--hero-from/to` (contrast-safe). Browser-verified (Playwright local + live): 3 theme đổi `.ms-hero` gradient + default restore.

**Counterfactual với v1.1.0 row 7 từ đầu:** round-1 INERT bị bắt ngay (1 runtime click test) → không có "deployed ✅" sai → 0 user round-trip. Self-test PASS — v1.1.0 row 7 fires đúng trên chính incident sinh ra nó. ✅

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active now)

PR port design source vào code:
- [ ] PR body có section `## Design-source parity` với bảng affordance 1-1?
- [ ] Mọi control/state/section source → wired trong target (hoặc `⚠️ dropped` + lý do)?
- [ ] Copied-but-unwired check: component định nghĩa trong primitives/lib có grep usage trong render?
- [ ] Dropped affordance là regression → follow-up gap filed?

### 7.2 CI grep detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)

- **Complexity:** "component defined but unused" detection trong JSX/TSX cần AST/import-graph parse, không trivial grep (false-positive với re-export, window-attached helpers, lazy-loaded).
- **Recurrence:** 1 (marketing-site ThemeSwitcher, hôm nay).
- **Decision:** reviewer-checklist §7.1 + worked self-test §6 đủ cho v1.0.0; revisit detector khi recurrence ≥2 OR có ESLint `no-unused` tích hợp design-parity awareness.

### 7.3 Memory auto-load (deferred per premature-rule guard ≥7 ngày)

Memory `feedback_design_source_implementation_parity.md` có thể remind lúc port design. Defer; reviewer-checklist + worked self-test đủ v1.0.0.

### 7.4 Override mechanism

Per §5 trailer `DESIGN_PARITY_DROP:`. Quarterly retro review >10% frequency.

---

## 8. Atomic-unique-bar check (per `rule-change-process.md` §5.1)

- ✅ **Atomic:** single concept = parity affordance source ↔ implementation
- ✅ **Unique:** `design-layer-coverage.md` covers 4-layer DOC tồn tại (要件/基本/詳細/コンポーネント); rule này covers AFFORDANCE parity giữa source design ↔ code render — khác axis. `output-review-mandate.md` covers review existence, không covers source-target affordance 1-1
- ✅ **Widely applicable:** mọi port design → code (ui_kits, production page, component)
- ✅ **Body discipline:** §1 The Rule ≤2 "and" conjunction

---

## 9. Relationship to other rules

- **`design-layer-coverage.md`** — sister design rule; that = 4-layer DOC completeness, this = source↔code affordance parity (different axis, compose)
- **`output-review-mandate.md`** §3 — paired same-PR row "Design-source implementation parity" tracking standard
- **`feature-ship-runtime-walk-mandate.md`** §3 — RST walk at feature ship; this rule = parity check at port time (upstream of walk)
- **`cross-flow-bug-class-sweep.md`** — sister "fix once → sweep sisters"; this = "port → verify nothing dropped" (different direction)
- **`incident-to-rule-pipeline.md`** — rule này direct output 2026-06-01 user-flagged miss "tại sao implement từ Claude Design vào ui_kits lại bỏ qua option" applied through 5-stage
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row all same PR
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (fix 1 chuẩn parity → mọi port design subsequent auto-comply)
- **`feedback_design_source_implementation_parity.md`** (memory, deferred §7.3)

---

## 10. Log

- **2026-06-01 (v1.1.0):** MINOR — added §3 row 7 "Runtime effect (interactive affordance)" + §3.2 runtime click-verify mandate. Triggered by own round-1 INERT miss same session: PR #2028 wired `<ThemeSwitcher>` (static row 1+5 PASS) + claimed "deployed ✅" via grep HTML — but browser self-test revealed click was visually inert (marketing hero reads `--hero-from/to`, not the toggled `--theme-*`). PR #2030 fixed (`.theme-*` override `--hero-from/to`), browser-verified local + live. Static "wired ✅" gave false PASS; only runtime click→observe catches "wired ≠ working" sub-class. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user "click ko có thay đổi, self-test chưa?") → Classify ✓ (v1.0.0 §3 covered static wired-check + copied-but-unwired but NOT runtime effect) → Rule+Enforce ✓ (§3 row 7 + §3.2 + §6 self-test update + cross-ref `feature-ship-runtime-walk-mandate.md` §3 paired same PR) → Self-Test ✓ (§6 — row 7 fires on own round-1 INERT incident) → Retro Log ✓ (this entry). MINOR per §4 (tighten constraint — could BLOCK a PR that previously passed static-only). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening; existing ports grandfathered; applies prospectively).
- **2026-06-01 (v1.0.0):** Rule created in response to user direction 2026-06-01 "làm meta-rule checklist parity" sau khi phát hiện `marketing-site` kit drop affordance "Chủ đề theo giáo viên" (4-option ThemeSwitcher) lúc port từ Claude Design source — component + CSS đều copy nhưng `<ThemeSwitcher>` không wire vào `ProductLanding` render → dead code, không hiển thị khi mở kit. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "tại sao implement từ source claude design vào ui kits lại bỏ qua") → Classify ✓ (no existing rule covers source↔implementation affordance parity; `design-layer-coverage.md` covers doc-layer existence khác axis; `output-review-mandate.md` covers review existence không covers affordance 1-1) → Rule+Enforce ✓ (this file + reviewer-checklist §7.1 + worked self-test §6 trên marketing-site ThemeSwitcher originating incident + rules-index.csv row + output-review-mandate.md §3 row paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example — §3 row 1+2+5 FAIL trên bản committed pre-fix, rule fires correctly + counterfactual 1 user round-trip saved) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn parity → mọi port design source subsequent auto-comply prospectively → eliminate dropped-affordance class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered design-implementation parity class; no constraint loosening; existing ports grandfathered; rule applies prospectively từ this PR forward 2026-06-01). Atomic-unique-bar §8 check passed. CI detector (§7.2) + memory auto-load (§7.3) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + worked self-test §6 sufficient cho v1.0.0. Path-scoped (`documents/02-architecture/design-system/**` + frontend src) per `context-budget-mandate.md` §3.2 — deferred-load khi touch design/frontend scope.
