---
paths: ["documents/03-planning/waves/**", "documents/03-planning/inside-out-queue.md"]
---

# Inside-out Completeness Trigger — Claude phải tự audit inside-out queue trước khi lock wave scope

**Priority:** 🔴 CRITICAL — force-multiplier governance preventing inside-out items bị miss
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; sister rule với `outside-in-coverage-trigger.md` v1.0.0; new rule với built-in enforcement (memory mirror + queue file + self-detection checklist + worked self-test on 2026-05-14 Wave 78 incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-uncovered audit step trước khi lock wave scope)
**Applies to:** Mỗi lần Claude draft wave plan PR (status: draft) hoặc add scope items vào wave plan đang in-flight. Apply parallel với `outside-in-coverage-trigger.md` — outside-in audit covers user-needs blind spot; this rule covers queued user-proposals blind spot.

---

## 1. The Rule

> **Trước khi lock scope wave plan, Claude PHẢI pull inside-out items từ 3 nguồn (không chỉ ROADMAP):**
> 1. **ROADMAP.md** `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action — canonical queue
> 2. **inside-out-queue.md** `documents/03-planning/inside-out-queue.md` — user-flagged items beyond ROADMAP
> 3. **AskUserQuestion explicit** — "còn inside-out items nào ngoài N items này mà bạn nhớ không?"
>
> Và RUN inside-out completeness audit agent (tương tự outside-in audit) khi wave scope candidates ≥3 items HOẶC khi gap age >7 ngày (canonical staleness).

Inside-out = items user/dev đã propose hoặc queue trong system (gap files / planning docs / chat). Outside-in = items chưa propose nhưng cần (persona simulation / benchmark / failure-matrix).

Wave 78 incident 2026-05-14: Claude pulled inside-out chỉ từ ROADMAP §🚀 Next Action → missed 5 user-proposed items (Premium plan / Feedback channel / Email content audit / user manual / + audit-found GAP-480/527/531/040 BLOCKING). User caught the miss. Rule này close audit hole.

---

## 2. Trigger pattern — khi nào rule fire

Rule fire khi Claude approaching scope lock cho wave plan:

| Pattern | Ví dụ |
|---|---|
| **About to write `## 3. Scope` section** của wave plan PR | Wave 78 plan draft |
| **About to AskUserQuestion "Scope wave có items A/B/C OK không?"** | Confirm scope decision |
| **About to commit wave plan file** với `status: draft` | Final pre-PR step |
| **Adding items vào wave đang in-flight** | Mid-wave scope expansion |
| **User confirm Sub-wave split** ("Sub-wave A first" / "Mega-wave" etc.) | Scope decision lock |

Rule **KHÔNG** fire khi:
- Wave plan đã shipped (closure scope không thay đổi)
- Bug fix PR cụ thể đã có root cause
- Wave 100% docs-only (vd rules consolidation wave)
- Hotfix prod incident (speed > completeness)

---

## 3. Hành động Claude phải làm khi rule fire

### Bước 1: 3-source inside-out pull

```bash
# Source 1: ROADMAP canonical
grep -A 20 "🚀 Next Action" documents/04-quality/gaps/ROADMAP.md | head -50

# Source 2: User-flagged queue
cat documents/03-planning/inside-out-queue.md
# Filter: status=queued, phase relevance matches current wave scope

# Source 3: Phase-relevant gaps from CSV (catch items missing from queue file)
bash scripts/query-gaps.sh "" OPEN <current-phase>
bash scripts/query-gaps.sh "" PARTIAL <current-phase>
```

### Bước 2: Cross-reference với wave scope draft

So sánh items pulled (Bước 1) với current Wave plan scope. List:
- ✅ Captured: items đã trong wave scope
- ⚠️ Missing: items chưa trong wave scope (Phase 1 BETA relevant + status non-DONE)
- ✅ Defer: items relevant nhưng phase mismatch hoặc explicit Wave N+1 (document why defer)

### Bước 3: Spawn inside-out completeness audit agent (when scope ≥3 items)

Background agent scan:
- ROADMAP historical (cả §🚀 superseded sections)
- Session handoffs (`documents/03-planning/session-handoffs/*.md`)
- PR logs last 20 (`documents/03-planning/pr-logs/PR-*.json`) cho "deferred" / "follow-up" / "Wave NN" mentions
- Archived wave plans (`documents/07-archived/planning-*/*.md`)
- Closed wave plans §7 Closure Protocol mentions next-wave queue
- gap-status.csv full filter `phase: <current-phase>` non-DONE

Output: items missed + suggested wave assignment + rationale.

### Bước 4: AskUserQuestion explicit

> "Inside-out scope hiện tại có N items. Audit agent surface thêm M items. Còn inside-out nào trong đầu bạn ngoài N+M items này không? (multi-select để add hoặc Other để custom)"

Multi-select options: items user-confirmed earlier sessions + recent chat-flagged items + "có item khác".

### Bước 5: Integrate findings vào wave plan §1 Brainstorm Q1

Highlight:
- **Inside-out from canonical** (ROADMAP items shipped)
- **Inside-out from queue file** (user-proposed items consumed)
- **Inside-out from audit** (items missed but surfaced)
- **Outside-in NEW** (per `outside-in-coverage-trigger.md` 3-agent convergence)

Wave plan §1 Brainstorm Q1 must show ALL 4 buckets separately để future readers thấy completeness.

---

## 4. Các trường hợp ngoại lệ (skip rule)

Rule KHÔNG mandate khi:

| Case | Lý do |
|---|---|
| Wave plan PATCH hotfix prod incident | Tốc độ ưu tiên; inside-out audit cho follow-up wave |
| Wave 100% docs governance (rules/skills consolidation) | Không có user-facing scope cần queue check |
| Wave plan scope <3 items | Cost của 3-source pull > marginal coverage benefit |
| Inside-out queue file đã được consult ≤24h (recent session) | Refresh không cần — nhưng vẫn AskUserQuestion explicit |
| User explicit "skip queue check, just execute current scope" | Tôn trọng quyết định user; ghi nhận lý do trong wave plan §8 Log |

Khi skip → vẫn note "Inside-out completeness audit skipped: <reason>" trong wave plan §1 Brainstorm Q1 để future reader hiểu.

---

## 5. Banned shortcuts

| ❌ Không được | ✅ Phải làm |
|---|---|
| Chỉ pull inside-out từ ROADMAP §🚀 (1 nguồn) | 3 nguồn: ROADMAP + queue file + AskUserQuestion |
| Skip queue file "vì user chưa cập nhật" | Read file regardless; consume tất cả items status=queued matching phase |
| AskUserQuestion sau khi đã lock scope | Hỏi BEFORE scope lock — findings vào §1 Brainstorm Q1 |
| Coi audit agent là optional khi scope ≥3 items | Spawn audit agent default; skip chỉ khi exempt §4 |
| Defer queue item silently không document | Nếu defer Wave N+1, document trong wave plan §1 Brainstorm Q1 + update queue file `status: queued` không thay đổi |
| Consume queue item nhưng KHÔNG update queue file `status: consumed` | Update file trong wave plan PR same diff (per `post-merge-sync-completeness.md`) |

---

## 6. Worked self-test — Wave 78 incident 2026-05-14

**Bối cảnh:** User asked "có giải pháp để lưu inside của tôi chứ?" sau khi Claude lock Wave 78 scope với 9 items (5 inside-out P0 + 4 outside-in N1/N2/N7/N8) → Claude pulled inside-out CHỈ từ ROADMAP §🚀.

**Apply rule retroactively at Wave 78 scope lock moment:**

Bước 1: 3-source inside-out pull (giả định rule existed):
- Source 1 ROADMAP: 5 items (GAP-508/514/515/518/428) ✓ captured
- Source 2 queue file: KHÔNG TỒN TẠI tại thời điểm đó → red flag (queue file thiếu = need create)
- Source 3 CSV query phase-1-beta non-DONE: 26 items → 5 BLOCKING (GAP-480/527/531/040 + PDPL) missing from scope

Bước 2 cross-reference: ⚠️ 5 audit-surfaced items + user-confirmed 3 items missing from current Wave 78 scope draft.

Bước 3 audit agent spawn: would have caught these without user nudge.

Bước 4 AskUserQuestion: would have caught Premium plan / Feedback channel / Email content audit user mention.

Bước 5 integrate: Wave 78 plan §1 Brainstorm Q1 would show:
- **Inside-out from ROADMAP (5):** GAP-508/514/515/518/428
- **Inside-out from queue file (3 user-confirmed):** Feedback channel, Email content audit (Wave 78); Premium plan (defer Wave 79)
- **Inside-out from audit (3 BLOCKING):** GAP-480/527/531 (defer GAP-040 + PDPL Wave 79)
- **Outside-in NEW (4):** N1/N2/N7/N8

**Kết quả nếu rule áp dụng từ đầu:**
- 0 user push-back (vs 2 push-backs actual)
- Wave 78 scope correct ngay từ lần đầu
- Save ~30min iteration round-trip

→ Rule fires đúng cho incident gốc. Self-test PASS ✅

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Path-scoped auto-load

`paths:` frontmatter — rule load khi Claude đọc `documents/03-planning/waves/**` OR `documents/03-planning/inside-out-queue.md`. Per `context-budget-mandate.md` §3.1 path-scope justified vì rule fire chỉ khi planning context.

### 7.2 Self-detection mỗi turn

Trước khi Claude write `## 3. Scope` section của wave plan file HOẶC commit wave plan với `status: draft`:
- Run §3 Bước 1-5 checklist mentally
- Nếu skip step nào → document why trong wave plan §1 Brainstorm Q1 (force transparency)

### 7.3 Memory auto-load

Memory entry `project_phase_1_beta_inside_out_queue.md` mirrors `documents/03-planning/inside-out-queue.md` semantically — Claude session start reads memory → reminded queue file exists + how to consult.

### 7.4 Reviewer-checklist (manual)

Khi review wave plan PR, reviewer hỏi:
- Wave plan §1 Brainstorm Q1 có 4 buckets (ROADMAP / queue / audit / outside-in) shown separately?
- Nếu chỉ có ROADMAP shown → flag + ask author "đã consult queue file + AskUserQuestion chưa?"

### 7.5 Override mechanism

User explicit từ chối inside-out audit:

```
INSIDE_OUT_AUDIT_SKIP: <lý do — vd "hotfix prod incident, defer audit Wave N+1">
INSIDE_OUT_AUDIT_FOLLOWUP: <link to scheduling next-wave audit>
```

Pattern frequency >30% trong 1 tháng → meta-review rule (có thể đang quá nặng).

### 7.6 Detector (deferred per premature-rule guard ≥7 ngày)

Future: scan wave plan §1 Brainstorm Q1 for required 4-bucket structure ("ROADMAP" + "queue" + "audit" + "outside-in" keywords) → WARN if any missing. Defer per `incident-to-rule-pipeline.md` premature-rule guard; enforcement memory + self-detection + reviewer-checklist đủ cho v1.0.0.

---

## 8. Relationship to other rules

- **`outside-in-coverage-trigger.md`** — SISTER rule. Outside-in covers "items user chưa nghĩ ra"; this rule covers "items user đã propose nhưng Claude miss". Both apply parallel cho user-facing wave scope.
- **`audit-to-gap-pipeline.md`** §2.6 wave-plan state-check — extends with completeness layer (state-check covers absent-symbol; this rule covers absent-scope-items)
- **`meta-gap-priority.md`** §3 — meta gaps ưu tiên cao nhất; this rule là META P0 force-multiplier (Wave 78 ngay)
- **`gap-architecture-v2.md`** §3 — gap-status.csv canonical; Source 3 trong §3 Bước 1 query CSV
- **`post-merge-sync-completeness.md`** §2 — Target 2 (ROADMAP) + new Target 5 (inside-out-queue.md) synced cùng PR khi consume item
- **`incident-to-rule-pipeline.md`** — rule này là direct output của 2026-05-14 user-flagged Wave 78 inside-out miss applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + queue file + memory + self-test all ship same PR
- **`feedback_wave_plan_through_pr.md`** — wave plan PR-first; this rule extends với "queue file consult happens BEFORE wave plan PR mở"
- **`output-review-mandate.md`** §3 — adds row "Wave plan scope completeness" tracking standard
- **`project_phase_1_beta_inside_out_queue.md`** (memory, paired same-PR) — mirror of queue file for cross-session awareness

---

## 9. Log

- **2026-05-14 (v1.0.0):** Rule created sau 2026-05-14 user-flagged miss "có giải pháp để lưu inside của tôi chứ? hay tôi vẫn phải tự log vào file md như action-2.md à?" + Wave 78 incident (Claude lock scope chỉ với ROADMAP + UI kits nudge → miss Premium plan / Feedback channel / Email content audit / user manual / 5 audit-surfaced BLOCKING items). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged 2x meta-questions in same session) → Classify ✓ (no existing rule mandates inside-out 3-source pull; `outside-in-coverage-trigger.md` sister covers user-needs blind spot but không cover queued-proposals blind spot) → Rule+Enforce ✓ (this file + paired same-PR `documents/03-planning/inside-out-queue.md` canonical file + memory mirror `project_phase_1_beta_inside_out_queue.md` + worked self-test §6 on Wave 78 incident per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on the originating Wave 78 session — rule fires correctly + counterfactual eliminates 2 push-back round-trips) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint covering previously-uncovered audit step; no constraint loosening for prior work; existing wave plans grandfathered; rule applies prospectively từ Wave 78 forward). Detector wiring (§7.6) deferred per premature-rule guard ≥7 ngày.
