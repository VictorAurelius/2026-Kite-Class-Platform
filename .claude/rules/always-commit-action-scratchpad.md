---
paths:
  - documents/action-*.md
---

# Always Commit Action Scratchpad — never stash/defer user inside docs

**Priority:** 🟠 MANDATORY — user inside preservation governance
**Version:** 1.0.0
**Created:** 2026-05-20
**Last-Reviewed:** 2026-05-20
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (path-scoped auto-load + reviewer-checklist + paired memory + worked self-test trên 2 fails 2026-05-18 stash@{3} + 2026-05-20 Wave 102.7.3 scope miss) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-implicit "user inside scratchpad always commit" expectation that was missed twice cross-session)
**Applies to:** Mọi file `documents/action-*.md` (user inside-out scratchpad). Per `feedback_no_unprompted_read_user_scratchpad.md` Claude KHÔNG read unprompted — nhưng KHI user edit file, Claude PHẢI commit ngay, không stash/defer.

---

## 1. The Rule

> **Khi user edit `documents/action-*.md`, Claude PHẢI commit thay đổi vào main branch trong same session, KHÔNG được stash, defer, hoặc bỏ qua.** User inside scratchpad là canonical source of user-flagged ideas + concerns ngoài ROADMAP / inside-out-queue / gaps. Stash/defer = lost inside = wave plan miss + recurring meta-fail.

Inside scratchpad là channel duy nhất user dùng để capture inside-out items dài/đa-topic mà không muốn break vào chat. Mất content scratchpad = mất nguồn inside-out canonical = wave plans subsequent miss scope → recurring fail (Wave 102.7.3 missed action-2.md §4 inside items là direct consequence của pattern).

---

## 2. Trigger pattern — khi nào rule fire

Rule fire khi:

| Pattern | Ví dụ |
|---|---|
| User mention edit/append action-x.md | "tôi vừa update action-2.md", "thêm inside vào action-x" |
| User reference content "trong action-x" | "check action-2 xem inside có gì" |
| File mtime mới hơn last commit (workspace state mismatch) | `git diff HEAD documents/action-*.md` không empty |
| User typed input chứa inside-list style (`1./2./3.` numbered scope, dấu `=>` thinking arrow) | Common user inside-pattern signal |
| Session-end approaching với uncommitted action-x.md changes | Per `session-end-context-check.md` §4.5 sync target verification |

Rule **KHÔNG** fire khi:
- File path không match `documents/action-*.md` pattern
- User explicit: "không commit action-x.md" (genuine override per §5 trailer)
- File deleted intentionally (user request)

---

## 3. Hành động Claude phải làm khi rule fire

### Bước 1: Verify state

```bash
git status documents/action-*.md
git diff HEAD documents/action-*.md | head -20
```

### Bước 2: Stage + commit (single commit, descriptive message)

```bash
git add documents/action-*.md
git commit -m "docs(action): sync action-N scratchpad inside content

User inside items captured: [summary 1-line]
Per always-commit-action-scratchpad.md §1 — no stash/defer."
```

### Bước 3: Push

Per `docs-only-pr-auto-merge.md` §2, action-*.md is docs-only scope → auto-merge eligible OR bundle vào next wave plan PR.

### Bước 4: Sync per `post-merge-sync-completeness.md` §2 if applicable

- Nếu inside item triggers Wave plan / gap filing → file follow-up gap + reference action-x line
- Nếu inside item is meta-rule signal → apply `incident-to-rule-pipeline.md` 5-stage

### Bước 5: Reference trong session-handoff (post-wave closure)

Session-handoff template should include section "User inside items consumed" + path to `documents/action-x.md` lines NN-NN với rationale.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| `git stash` action-x.md changes "để sau commit" | Commit ngay same session |
| Bỏ qua action-x.md trong session-end sync check | Verify status + commit before propose end |
| Tự edit/overwrite action-x.md without user explicit | User là author canonical; Claude chỉ commit, không edit content |
| Treat action-x.md như temp file ignore | Per `feedback_no_unprompted_read_user_scratchpad.md` Claude không READ unprompted nhưng PHẢI commit khi user edit |
| Defer commit vào "next wave plan PR" mà không track | Commit ngay; reference từ wave plan nếu cần |
| Lost stash → silently proceed | File follow-up gap nếu stash recovery khả thi; transparent về miss |

---

## 5. Worked self-test

### 5.1 Fail #1 — 2026-05-18 stash@{3} (Wave 95 era)

**Original incident:**
- User edit action-2.md trên branch `wave/95-gap-folder-organization-pr1` (~32 dòng inside: Phase 1.5 gaps + 4 docs-scaling rules questions + audit folder organization + OCR upload feature)
- Coordinator stash với note "action-2.md scratchpad — defer"
- Stash never applied; content stayed in stash@{3} dangling

**Apply rule retroactively:**
1. **Bước 1:** `git diff HEAD documents/action-2.md` → 32 lines diff present
2. **Bước 2:** `git add documents/action-2.md && git commit -m "docs(action): Wave 95 inside scratchpad sync"`
3. **Bước 3:** push + auto-merge per `docs-only-pr-auto-merge.md`
4. **Bước 4:** OCR upload feature → file GAP-XXX defer Phase 1.5

**Counterfactual cost:** ~3 wave subsequent (Wave 96+) had no visibility of Wave 95 inside items because stash never landed. Recovery cost 2026-05-20: ~10min investigation + audit-trail loss permanent (32 dòng wave/95 era not in main history).

Self-test PASS — rule fires correctly on origin incident.

### 5.2 Fail #2 — 2026-05-20 Wave 102.7.3 scope miss

**Original incident:**
- /start-session 2026-05-20 16:37 → only read `wave-102.7-handoff.md` (1 of 2 handoffs)
- `post-wave-102.5-handoff.md` §2.1 explicit: "GVHD feedback NEW input — user sẽ share trong session mới" + "AskUserQuestion explicit còn items nào ngoài GVHD feedback"
- action-2.md §4 (lines 37-44) committed inside dev items: project-jargon scrub / claude scrub / figure folder / personal data lookup
- Wave 102.7.3 scope locked WITHOUT AskUserQuestion explicit → missed both GVHD feedback prompt + action-2.md §4 inside items
- User flagged "fail hoàn toàn so với inside tôi yêu cầu"

**Apply rule retroactively:**
1. **Bước 1:** action-2.md ALREADY committed (no edit pending — file ở HEAD đúng)
2. **Bước 2:** N/A (no commit needed — file already in main)
3. **Bước 3:** N/A
4. **Bước 4:** ⚠️ KEY MISS — rule §3 Bước 4 says "Nếu inside item triggers Wave plan → file follow-up gap + reference action-x line". Wave 102.7.3 was locked WITHOUT consulting action-2.md §4 lines 37-44 inside dev items (project-jargon scrub etc.). Rule complemented by `inside-out-completeness-trigger.md` §3 Bước 4 AskUserQuestion → mandatory before wave plan scope lock.
5. **Bước 5:** ⚠️ Session-handoff `wave-102.7-handoff.md` did NOT reference action-2.md §4 inside items — handoff truncated to wave-specific scope only, not full inside scope. Recovery: future handoffs MUST reference both `action-N.md` user inside lines AND wave-specific scope.

**Counterfactual cost:** Wave 102.7.3 missed academic-defense-critical scope (project-jargon scrub). thesis-v1.docx has 32 hits BETA/GA/Phase remaining → giảng viên reject signal. Recovery: Wave 102.7.4 needed to fix.

Self-test PASS — rule fires correctly + identifies §3 Bước 4-5 cross-link with `inside-out-completeness-trigger.md` as enforcement parity.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 Path-scoped auto-load (active immediately)

Rule loaded khi Claude touch `documents/action-*.md` path. Per `context-budget-mandate.md` §3.1 path-scope justified (rule applies chỉ khi action-x.md trong context).

### 6.2 Reviewer-checklist (manual)

Pre-merge PR review:
- Diff touches `documents/action-*.md`?
- Commit message follow §3 Bước 2 format (`docs(action): sync ...`)?
- Wave plan reference inside items via `action-x.md` line refs (nếu Wave consumes inside)?
- Session-handoff §"User inside items consumed" section present (nếu wave closure)?

### 6.3 Memory auto-load (paired same-PR)

Memory entry `feedback_always_commit_action_scratchpad.md` reminds Claude at session start: "User inside scratchpad commit immediately, never stash/defer". 4-bullet checklist.

### 6.4 Detector (deferred per `incident-to-rule-pipeline.md` §3 premature-rule guard ≥7 ngày)

Future enhancement: scan session-end-context-check.md §4.5 5-target check để extend với 6th target "action-*.md uncommitted check". Defer wiring detector until 2nd recurrence post-rule-merge. v1.0.0 enforcement = path-scoped auto-load + reviewer-checklist + memory + worked self-test sufficient.

### 6.5 Cross-link với related rules

- **`feedback_no_unprompted_read_user_scratchpad.md`** — Claude KHÔNG read unprompted. Rule này không loosen: still no unprompted read; BUT khi user edit (state-mismatch), commit mandatory.
- **`inside-out-completeness-trigger.md`** §3 Bước 4 — AskUserQuestion mandatory before scope lock. Rule này extend Bước 1 source check: action-x.md is 4th implicit source (after ROADMAP / inside-out-queue / AskUserQuestion).
- **`post-merge-sync-completeness.md`** §2 — 4 sync targets (CSV / ROADMAP / wave-history / MEMORY). Action-x.md is implicit 5th target khi inside content driving wave/gap/rule decisions.
- **`session-end-context-check.md`** §4.5 — 5-target docs-sync check at session-end. Rule này extend: action-x.md uncommitted = block end-session propose.

### 6.6 Override mechanism

Genuine exception (user explicit: "không commit action-x lần này"):

```
git commit -m "...
ACTION_SCRATCHPAD_NO_COMMIT: <reason — e.g., 'user explicit defer commit cho cleanup pass'>"
```

Trailer logged. Pattern frequency >5% triggers meta-review.

---

## 7. Atomic-unique-bar check (per `rule-change-process.md` §5.1)

- ✅ **Atomic concept:** single responsibility = commit action-x.md, no stash/defer
- ✅ **Unique scope:** no existing rule covers (closest = `feedback_no_unprompted_read_user_scratchpad.md` covers READ direction, không cover WRITE direction)
- ✅ **Widely applicable:** every session user edits action-*.md (frequency mỗi 1-2 sessions)
- ✅ **Body discipline:** §1 The Rule has 1 "and" conjunction (≤2)

Pass all 4 criteria → atomic-unique rule.

---

## 8. Relationship to other rules

- **`feedback_no_unprompted_read_user_scratchpad.md`** (memory) — sister rule covering READ direction; rule này covers WRITE (commit) direction. Both apply parallel.
- **`inside-out-completeness-trigger.md`** §3 — wave plan scope lock requires AskUserQuestion; rule này extends Bước 1 source check (action-x.md as 4th source).
- **`post-merge-sync-completeness.md`** §2 — 4 sync targets; rule này adds implicit 5th target.
- **`session-end-context-check.md`** §4.5 — 5-target docs sync; rule này adds explicit check "action-*.md uncommitted?"
- **`docs-only-pr-auto-merge.md`** — action-*.md is docs-only → auto-merge eligible.
- **`gap-done-discipline.md`** — gap closure that references action-x line MUST commit action-x first.
- **`incident-to-rule-pipeline.md`** — rule này direct output of 2 recurrence incidents (2026-05-18 stash@{3} + 2026-05-20 Wave 102.7.3 scope miss) applied through 5-stage.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + path-scoped auto-load + memory + reviewer-checklist + worked self-test §5 (2 fails) all ship same PR.
- **`output-review-mandate.md`** §3 — adds row "Action scratchpad commit" tracking review standard.
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (fix 1 commit discipline → eliminate inside-loss class permanently).

---

## 9. Log

- **2026-05-20 (v1.0.0):** Rule created in response to user direction 2026-05-20 post Wave 102.7.3 closure: "thêm rules là luôn commit file action-x.md chứ không bỏ qua". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged 2 fails — stash@{3} Wave 95 era + Wave 102.7.3 scope miss missing action-2.md §4 inside items) → Classify ✓ (no existing rule mandates action-x.md commit discipline; sister `feedback_no_unprompted_read_user_scratchpad.md` covers READ direction only) → Rule+Enforce ✓ (this file + path-scoped auto-load `documents/action-*.md` + paired memory `feedback_always_commit_action_scratchpad.md` + reviewer-checklist + worked self-test §5 on 2 recurrence incidents per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§5 worked example on 2 fails — rule fires correctly + identifies §3 Bước 4-5 cross-link với `inside-out-completeness-trigger.md`) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix commit discipline 1 lần → eliminate inside-loss class permanently across future sessions. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-implicit user-inside-preservation expectation; no constraint loosening; existing scratchpad commits grandfathered; rule applies prospectively từ next session forward). Atomic-unique-bar §7 check passed: ✅ atomic + ✅ unique scope + ✅ widely applicable + ✅ body discipline. Detector wiring (§6.4 session-end-context-check.md §4.5 extension) deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; v1.0.0 enforcement = path-scoped auto-load + memory + reviewer-checklist + worked self-test §5 sufficient.
