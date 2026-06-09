---
paths:
  - "documents/03-planning/waves/wave-*-flow-*.md"
  - "documents/03-planning/roadmap/flow-verification-campaign.md"
  - "documents/04-quality/gaps/**"
  - "documents/05-guides/operations/*-g2-recipe-*.md"
  - ".claude/rules/small-gap-inline-fix.md"
---

# Small-Gap Inline-Fix — fix gap NHỎ ngay khi walk bắt, không file-rồi-defer

**Priority:** 🟠 MANDATORY — walk-found gap backlog hygiene governance
**Version:** 1.0.0
**Created:** 2026-06-08
**Last-Reviewed:** 2026-06-08
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + memory auto-load + worked self-test on Flow Verification Campaign KC-1 G2 session 2026-06-08 — GAP-1067/1072 fix-inline vs GAP-1071 root-fix-defer phân loại đúng) per §6.5 Enforcement Parity Mandate; no constraint loosening — refine `flow-verification-campaign.md` §MODE (defer cosmetic-LỚN) bằng cách codify "cosmetic-NHỎ-RẺ → fix inline"; META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi gap được 1 walk (G2 human / RST / G1 browser-walk / G3 parity walk) bắt được, tại thời điểm quyết định fix-inline-vs-defer. Out-of-scope: gap surfaced ngoài walk (audit run → `audit-to-gap-pipeline.md`; non-audit work discovery → `discovery-to-gap-inline-filing.md`).

---

## 1. The Rule

> **Khi 1 walk (G2 human / RST / G1 browser-walk / G3 parity) bắt được gap NHỎ (low-effort, bounded, low-risk), MUST fix inline trong cùng session (batch theo `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch) + flip DONE — KHÔNG chỉ file-and-defer "wave sau".** Chỉ file-and-defer khi gap KHÔNG nhỏ (large / architecture / cross-cutting cần sweep+design / cần context flow khác / security-sensitive cần review / blocked external). Vẫn file gap (per `discovery-to-gap-inline-filing.md`) cho mọi finding — khác biệt là fix + flip DONE cùng session thay vì để OPEN tồn đọng.

Định nghĩa "gap NHỎ" — fix inline khi TẤT CẢ 4 tiêu chí hold:

| # | Tiêu chí | Pass khi |
|---|---|---|
| **(a)** | Effort | Ước tính ≤ ~30 phút / single-concern bounded (1 site, 1 endpoint, 1 config, 1 page render) |
| **(b)** | Scope locality | In-scope HOẶC adjacent flow đang walk — KHÔNG cần context flow khác để fix đúng |
| **(c)** | Risk | Low-risk: KHÔNG phải architecture decision, KHÔNG đổi cross-service contract, KHÔNG security/authz-sensitive logic |
| **(d)** | Verifiability | Verify được NGAY trên stack hiện tại (rebuild + re-walk được trong session) |

Nếu BẤT KỲ tiêu chí (a)-(d) FAIL → gap KHÔNG nhỏ → file-and-defer (gap OPEN, fix sang wave-fix phase). Nếu TẤT CẢ pass → fix inline + flip DONE.

File-rồi-defer gap 10-dòng để "wave sau" tạo backlog tồn đọng + chi phí re-load context (re-stack-up + re-walk-to-repro + re-read flow) > chi phí fix ngay. Force-multiplier: 1 chuẩn fix-small-inline → mọi walk subsequent (22-flow campaign × N gap/walk) auto-comply → eliminate small-gap-backlog-decay class.

Đây là **refinement của campaign §MODE**, KHÔNG mâu thuẫn: MODE defer cosmetic-LỚN (large defer wave-fix phase); rule này nói cosmetic-NHỎ-RẺ thì fix luôn để tránh tồn đọng. Sister rule khác boundary:
- `discovery-to-gap-inline-filing.md` §1 — file gap inline khi discover trong non-audit work (filing direction, không quyết fix-vs-defer)
- `feature-ship-runtime-walk-mandate.md` §3.4 — catalog-then-batch protocol DURING walk (cách fix, không quyết fix-vs-defer)
- `cross-flow-bug-class-sweep.md` §1 — sau fix sweep sister flow (post-fix direction)
- **rule này** — quyết fix-inline-vs-defer cho gap walk bắt được, theo size

---

## 2. Trigger pattern — khi nào rule fires

Rule fires khi 1 walk surfaces gap AND walk loại trong {G2 human local test, RST exploratory walk, G1 browser-walk, G3 production-parity walk}:

| Đang walk | Bắt được gap | Fire rule? |
|---|---|---|
| **G2 human local test** | FE console error / icon missing / footer typo / layout shift cosmetic | ✅ YES — classify size → fix inline nếu nhỏ |
| **G1 browser-walk** | Stale docker-proxy / single missing route / 1 endpoint 404 | ✅ YES |
| **RST exploratory walk** | 1 validation thiếu / 1 enum doc sai / 1 error message generic | ✅ YES |
| **G3 parity walk** | 1 env-var local override missing / 1 prod-profile flag | ✅ YES |
| Audit run (post-wave audit suite) | Audit finding | ❌ NO — `audit-to-gap-pipeline.md` covers |
| Non-audit work (docs/refactor/cleanup) | Schema anomaly / dead code | ❌ NO — `discovery-to-gap-inline-filing.md` covers |

Rule **KHÔNG** fires khi:
- Gap surfaced ngoài walk (audit / non-audit discovery) — sister rules cover filing
- Gap đã có gap mở khớp scope — comment vào gap hiện tại, không tạo duplicate
- Walk đang ở fix-loop iterate (chưa flip G1) — fix loop tự nhiên gom blocker; rule này về quyết-định-tại-thời-điểm-classify

---

## 3. Decision matrix — nhỏ → fix inline vs lớn → defer

Sau khi walk bắt gap + file gap inline (per `discovery-to-gap-inline-filing.md`), classify size để quyết fix-vs-defer:

| Verdict | Khi nào (per §1 4 tiêu chí) | Action |
|---|---|---|
| **FIX INLINE** | TẤT CẢ (a) effort ≤30p + (b) scope-local + (c) low-risk + (d) verify-now | Batch fix per `feature-ship-runtime-walk-mandate.md` §3.4 → single rebuild → re-walk → flip DONE cùng session |
| **DEFER** | BẤT KỲ tiêu chí FAIL — multi-file refactor lớn / architecture decision / cross-cutting cần sweep+design / cần context flow khác / security-sensitive cần review / blocked external | File gap OPEN + ghi defer reason; fix sang wave-fix phase sau campaign |
| **SPLIT** | Gap có cả fix-site-đơn-lẻ NHỎ + root-fix LỚN | Fix-site đơn lẻ inline (unblock walk) + defer phần root-fix (file OPEN sub-gap với grep evidence) |

### 3.1 Ví dụ phân loại — KC-1 G2 session 2026-06-08

| Gap | Mô tả | 4 tiêu chí | Verdict | Lý do |
|---|---|---|---|---|
| **GAP-1067** | Stale docker-proxy port-forward `:3000` → ERR_EMPTY_RESPONSE | (a)✅ ~10p restart proxy + (b)✅ in-scope stack + (c)✅ infra restart no logic + (d)✅ re-open browser verify | **FIX INLINE** | Cross-cutting-infra nhưng fix global 1 lần ~10p, benefit mọi walk sau |
| **GAP-1072** | Logo render lỗi trên dashboard header (asset path) | (a)✅ ~15p sửa path + (b)✅ in-scope page + (c)✅ cosmetic no logic + (d)✅ re-render verify | **FIX INLINE** | Single-concern cosmetic rẻ |
| **GAP-1071 (root-fix)** | Move shell layout → Next.js `layout.tsx` (cần triage 10 page) | (a)❌ >30p multi-page + (b)❌ cross-cutting 10 page + (c)⚠️ shared component refactor + (d)⚠️ cần re-walk nhiều page | **SPLIT** | Fix-site đơn lẻ (1 page shell) inline để unblock walk; defer phần root-fix-move-shell (file OPEN sub-gap, triage 10 page sang wave-fix) |
| (giả định) GAP-XXX | Đổi gateway tenant-resolution wire-format toàn hệ | (a)❌ + (c)❌ cross-service contract + security | **DEFER** | Architecture decision, cần design + sweep — defer wave-fix |

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| File gap 10-dòng cosmetic rồi defer "wave sau" khi đủ 4 tiêu chí nhỏ | Fix inline cùng session + flip DONE |
| Defer gap nhỏ rồi quên → backlog decay (re-load context cost > fix cost) | Fix tại chỗ khi walk còn warm context |
| "Để batch hết campaign rồi fix 1 lượt" cho gap nhỏ rẻ | Campaign §MODE chỉ defer cosmetic-LỚN; nhỏ-rẻ fix ngay |
| Fix inline gap LỚN (architecture/cross-service) giữa walk vì "thấy luôn rồi" | Gap lớn → file OPEN + defer; tránh scope-creep giữa walk |
| Bỏ file gap "vì fix luôn rồi, khỏi cần gap" | Vẫn file gap per `discovery-to-gap-inline-filing.md` (CSV-canonical trail) — chỉ flip DONE cùng session |
| Flip DONE mà chưa re-walk verify fix | Re-walk per `pre-handoff-self-test-completeness.md` §3 trước flip DONE |
| Inline-rebuild mỗi gap nhỏ (thrash) | Catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4 → single rebuild |
| Classify "nhỏ" bằng cảm tính | Apply 4 tiêu chí §1 (a)+(b)+(c)+(d) — TẤT CẢ pass mới nhỏ |

---

## 5. Override mechanism

Genuine exception (gap nhỏ nhưng session context/time vượt ngắn-hạn, vd cluster ≥5 gap nhỏ cùng walk vượt session capacity):

```
git commit -m "...
SMALL_GAP_DEFER: <gap-id(s)> — <reason — e.g. '6 cosmetic gaps cùng KC-1 walk, session capacity hết, batch wave-fix-1'>
SMALL_GAP_FOLLOWUP: <gap link OR wave-fix plan link tracking batch fix within Ndays>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely size-classification mis-tuned OR fix-inline discipline drift).

---

## 6. Worked self-test — KC-1 G2 session (2026-06-08, originating incident)

**Scenario:** KC-1 G2 human walk bắt nhiều gap nhỏ (GAP-1067 docker-proxy, GAP-1071 layout shell, GAP-1072 logo render — mỗi cái ~10-30 phút). Theo campaign §MODE ban đầu (defer cosmetic P2/P3 sang wave-fix phase), tất cả bị file-and-defer → backlog tồn đọng. User chốt 2026-06-08: "đối với các gap nhỏ, nên fix luôn tránh tồn đọng khi bắt bug G2".

**Apply rule retroactively tại moment classify mỗi gap:**

| Gap | 4 tiêu chí §1 | Rule verdict | Hành động đúng |
|---|---|---|---|
| GAP-1067 docker-proxy | (a)(b)(c)(d) đều ✅ | FIX INLINE | restart proxy ~10p + re-verify browser → DONE cùng session |
| GAP-1072 logo render | (a)(b)(c)(d) đều ✅ | FIX INLINE | sửa asset path ~15p + re-render → DONE cùng session |
| GAP-1071 root-fix move-shell | (a)❌ >30p + (b)❌ 10 page | SPLIT | fix-site 1 page inline (unblock walk) + defer root-fix-move-shell (OPEN sub-gap) |

→ **Rule fires correctly:** GAP-1067 + GAP-1072 phân loại FIX INLINE đúng (cùng session, tránh tồn đọng); GAP-1071 root-fix phân loại SPLIT đúng (fix-site đơn lẻ inline + defer phần root-fix cross-cutting cần triage 10 page).

**Counterfactual với rule active từ đầu:**
- Không có rule (thực tế): 3 gap nhỏ file-and-defer → backlog 3 gap OPEN → wave-fix phase sau re-load context (re-stack-up + re-walk-repro + re-read) ~30-45p/gap overhead
- Có rule: GAP-1067/1072 fix ngay (~25p tổng, context warm) + GAP-1071 split (fix-site inline + root-fix defer 1 gap thay vì cả gap)
- **Net savings: ~60-90p re-load context eliminated cho 2 gap fix-inline + backlog giảm từ 3 → 1 (chỉ root-fix LỚN defer)**

**Verdict:** Self-test PASS ✅. Rule fires correctly trên originating KC-1 G2 incident; phân loại 3 gap đúng (2 FIX INLINE + 1 SPLIT). Prospective application tới Flow Verification Campaign §4 22-flow queue G2 walks eliminate small-gap-backlog-decay class.

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho wave plan closure PR / campaign update sau 1 walk surfaces gap:

- [ ] Mỗi gap walk bắt được đã file inline (per `discovery-to-gap-inline-filing.md`)?
- [ ] Mỗi gap classify size per §1 4 tiêu chí (a)+(b)+(c)+(d)?
- [ ] Gap đủ 4 tiêu chí nhỏ → fixed inline + flipped DONE cùng session (KHÔNG để OPEN tồn đọng)?
- [ ] Gap KHÔNG nhỏ (FAIL ≥1 tiêu chí) → file OPEN + defer reason ghi rõ?
- [ ] Gap SPLIT → fix-site đơn lẻ inline + root-fix defer (OPEN sub-gap)?
- [ ] Nếu override trailer `SMALL_GAP_DEFER:` present, reason + follow-up valid per §5?

### 7.2 Memory auto-load (paired same-PR)

Memory entry `feedback_small_gap_inline_fix.md` loads at session start, reminds 4-bullet checklist trước khi defer gap walk bắt được.

### 7.3 Override mechanism

Per §5 trailer `SMALL_GAP_DEFER:` — logged quarterly retro. Pattern frequency >10%/quarter → meta-review.

### 7.4 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** Scan campaign/wave PR cho gap-filed-during-walk + classify gap size (effort/scope/risk/verifiability) + verify OPEN-vs-DONE state matches size — requires gap-size NLP classification + walk-context parser, NOT trivial bash
- **Recurrence count:** 1 today (KC-1 G2 2026-06-08)
- **FP risk:** High — size classification inherently judgement; legit DEFER (large gap) vs miss (small gap deferred) hard to distinguish heuristically
- **Decision:** Reviewer-checklist §7.1 + memory auto-load §7.2 + worked self-test §6 sufficient cho v1.0.0; revisit detector khi recurrence-count ≥2 post-rule (22-flow campaign G2 walks subsequent)

---

## 8. Relationship to other rules

- **`discovery-to-gap-inline-filing.md`** v1.0.0 §1 — sister covers FILING direction (file gap inline khi discover); rule này covers FIX-vs-DEFER decision sau khi filed (size-based). Compose: walk bắt gap → file inline (discovery rule) → classify size + fix-or-defer (rule này).
- **`feature-ship-runtime-walk-mandate.md`** v1.1.0 §3.4 — catalog-then-batch protocol DURING walk; rule này dùng §3.4 batch protocol khi fix-inline (single rebuild cho N small fixes).
- **`pre-handoff-self-test-completeness.md`** v1.2.0 §3 — POST-FIX re-walk trước DONE flip; rule này mandate re-walk verify trước flip DONE cho fix-inline.
- **`cross-flow-bug-class-sweep.md`** v1.0.1 §1 — sau fix sweep sister flow; nếu small-gap fix là bug-class → sweep apply.
- **`flow-verification-campaign.md`** §MODE + §4.5 — rule này refine §MODE (defer cosmetic-LỚN → nhỏ-rẻ fix inline); §4.5 blast-radius matrix complement (single-flow small → fix inline).
- **`gap-done-discipline.md`** §2 — DONE flip requires AC verified; rule này mandate flip DONE cùng session cho small-gap fix-inline (sau re-walk).
- **`g1-browser-walk-before-flip.md`** v1.0.0 — G1 browser-walk là 1 trong walk classes §2 trigger; small-gap bắt qua browser-walk apply rule này.
- **`g2-handoff-md-mandate.md`** v1.0.0 — G2 human walk là walk class chính §2 trigger; small-gap bắt qua G2 apply rule này.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn fix-small-inline → mọi walk subsequent auto-comply prospectively → eliminate small-gap-backlog-decay class).
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + memory + worked self-test §6 + rules-index.csv row + output-review-mandate.md §3 row + campaign §MODE refine all paired same PR.
- **`incident-to-rule-pipeline.md`** v1.1 — rule này = direct output 2026-06-08 user-flagged "gap nhỏ nên fix luôn tránh tồn đọng" qua 5-stage pipeline.
- **`context-budget-mandate.md`** §3.2 — path-scoped (campaign + wave-flow + gap + g2-recipe paths) — không tăng always-load band.
- **`output-review-mandate.md`** §3 — paired same-PR row "Small-gap inline-fix during walk".
- **`feedback_small_gap_inline_fix.md`** (memory, paired same-PR per Enforcement Parity).

---

## 9. Auto-load justification (per `context-budget-mandate.md` §3.2)

Rule này dùng `paths:` frontmatter (path-scoped, KHÔNG always-load). Lý do:

- **Fire tại walk-classify decision-time, scope-bound vào flow campaign** — rule kích hoạt khi đang walk flow (G1/G2/G3/RST) bắt gap + quyết fix-vs-defer. Natural file-scope: wave-flow plans + campaign doc + gap files + G2 recipe — đúng surface session đang touch khi walk flow campaign.
- **Path-scope ĐỦ cover trigger** — walk flow campaign luôn touch `wave-*-flow-*.md` HOẶC `flow-verification-campaign.md` HOẶC gap files. Khác `discovery-to-gap-inline-filing.md` (always-load vì discover trong MỌI work scope không có natural glob), rule này bound vào flow-walk → path-scope chính xác.
- **Token cost tiết kiệm** — path-scoped → 0 base-context khi session không walk flow. README rule-count ceiling: 12 always-load (OK <18) + 88 path-scoped (OK <100) sau khi thêm rule này.
- **Priority 🟠 MANDATORY giữ nguyên** — không nâng CRITICAL; §5 exception cho phép defer cluster; path-scope per `context-budget-mandate.md` §3.1 default cho domain-specific rule.

Re-evaluate nếu: (a) walk loại mới ngoài flow campaign cần rule (vd standalone RST không qua flow file), (b) recurrence >2 chứng tỏ cần always-load awareness.

---

## 10. Log

- **2026-06-08 (v1.0.0):** Rule created in response to user-flagged 2026-06-08 Flow Verification Campaign KC-1 G2 session: G2 walk bắt nhiều gap nhỏ (GAP-1067 docker-proxy / GAP-1071 layout shell / GAP-1072 logo render, mỗi cái ~10-30 phút) → theo campaign §MODE ban đầu bị file-and-defer → backlog tồn đọng + re-load context cost > fix-ngay cost. User chốt "đối với các gap nhỏ, nên fix luôn tránh tồn đọng khi bắt bug G2". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no existing rule quyết fix-inline-vs-defer theo gap-size cho walk-found gaps; `discovery-to-gap-inline-filing.md` covers FILING direction không quyết fix-vs-defer; `feature-ship-runtime-walk-mandate.md` §3.4 covers HOW-to-fix batch protocol không quyết fix-vs-defer; `flow-verification-campaign.md` §MODE defer cosmetic blanket không phân biệt nhỏ-rẻ vs lớn) → Rule+Enforce ✓ (this file + reviewer-checklist §7.1 + memory `feedback_small_gap_inline_fix.md` paired same-PR + rules-index.csv row + output-review-mandate.md §3 row + campaign §MODE refine per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên KC-1 G2 originating incident — rule fires correctly, phân loại 3 gap đúng 2 FIX INLINE + 1 SPLIT + counterfactual ~60-90p re-load context saved) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn fix-small-inline → Flow Verification Campaign §4 22-flow queue G2 walks subsequent auto-comply prospectively → eliminate small-gap-backlog-decay class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint refining campaign §MODE (defer cosmetic-LỚN → nhỏ-rẻ fix inline); no constraint loosening — vẫn file gap per discovery rule, chỉ thêm fix+flip-DONE cùng session cho gap nhỏ; existing deferred small-gaps grandfathered (batch wave-fix); rule applies prospectively từ next walk forward 2026-06-08). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: fix small walk-found gap inline theo size) + ✅ unique (sister rules cover filing / batch-protocol / campaign-mode-blanket — không cover size-based fix-vs-defer decision) + ✅ widely applicable (mọi walk-found gap × 22-flow campaign) + ✅ body discipline §1 ≤2 conjunction. Path-scoped per `context-budget-mandate.md` §3.2 (flow campaign + wave-flow + gap + g2-recipe paths) — always-load band giữ 12 (OK <18); path-scoped band 88 (OK <100). Detector (§7.4) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (recurrence 1, FP risk high cho size-classification); reviewer-checklist + memory auto-load + worked self-test §6 sufficient cho v1.0.0.
