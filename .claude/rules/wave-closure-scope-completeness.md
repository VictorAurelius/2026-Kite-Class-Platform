---
paths:
  - "documents/03-planning/waves/**"
---

# Wave Closure Scope Completeness — every plan §3 Scope item must reconcile at closure

**Priority:** 🟠 MANDATORY — wave-level governance preventing scope-pending orphan items
**Version:** 1.0.0
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-18
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + 3 follow-up gap files paired same-PR + worked self-test on Wave 87/88 DNS incident + Wave 92 6-item retroactive) per `rule-change-process.md` §6.5 Enforcement Parity Mandate; no constraint loosening — extends `gap-done-discipline.md` §3 PARTIAL exit ramp từ per-gap level lên wave level)
**Applies to:** Every wave closure PR (frontmatter `status: draft → complete` flip) hoặc wave-history.jsonl append entry

---

## 1. The Rule

> **Wave closure PR PHẢI include scope-completeness reconciliation table — mọi item trong wave plan §3 Scope được categorize: ✅ DONE / 🟡 PARTIAL (với gap link) / ❌ NOT-IMPLEMENTED (với follow-up gap link HOẶC explicit out-of-scope rationale).**

Wave-level scope discipline parallels per-gap `gap-done-discipline.md` §3 PARTIAL exit ramp. Per-gap rule prevents silent DONE flip với deferred AC; per-wave rule prevents silent wave-complete flip với scope-pending items.

Đây là sister rule cho `gap-done-discipline.md` — extends discipline từ gap level lên wave level. Cùng failure pattern: "main work done, side-effect items defer/silent" → orphan items mất tracking khi context flush.

---

## 2. Trigger pattern — khi nào rule fire

Rule fire khi wave closure approaching:

| Pattern | Ví dụ |
|---|---|
| Wave plan frontmatter flip `status: draft → complete` | `documents/03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md` |
| `wave-history.jsonl` append entry với `"status":"complete"` | Wave 92 closure |
| Closure PR title chứa "closure", "SHIPPED", "Wave N closure" | PR #1517 |
| ROADMAP §🎯 add "Wave N SHIPPED" section | ROADMAP §🚀 Wave 92 entry |

Rule KHÔNG fire khi:
- Wave plan PATCH (mid-flight scope adjustment, không flip complete)
- Individual bucket PR merge (per-bucket scope, không wave-level)
- Per-gap closure PR (covered by `gap-done-discipline.md`)

---

## 3. Required content — scope-completeness reconciliation table

Wave closure PR body OR wave plan §7 Closure Protocol section MUST contain table mapping mỗi plan §3 Scope item:

```markdown
## Scope-Completeness Reconciliation

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket A — Admin audit log enrichment V61 + 5 columns + 3 IT | ✅ DONE | — |
| 2 | Bucket A — Other admin controllers annotation | 🟡 PARTIAL | GAP-XXX (Wave N+1 candidate) |
| 3 | Bucket A — FE admin UI enrichment hiển thị | ❌ NOT-IMPLEMENTED | GAP-YYY filed này wave hoặc next |
| 4 | Wave post-audit suite ≤3 ngày | 🟡 PARTIAL | GAP-ZZZ cadence mandate |
| 5 | Live verify all 3 admin v1 controllers | ❌ NOT-IMPLEMENTED (gated GAP-612 AWS) | GAP-AAA — unblocks post-restore |
```

### Categorization decision tree

```
For each plan §3 Scope item:
  Did this item ship + AC verified?
    YES → ✅ DONE
    PARTIAL → 🟡 PARTIAL
      Must have: gap file link với Status:PARTIAL + remaining work documented
    NOT shipped → ❌ NOT-IMPLEMENTED
      Must have ONE of:
        (a) Follow-up gap link (Status:OPEN, scheduled to fix Wave N+1+)
        (b) Explicit out-of-scope rationale (e.g., "Deferred Phase 1.5 per cost-benefit")
        (c) Override trailer WAVE_SCOPE_DROP: <reason>
```

### Banned shortcuts

- ❌ Wave status: complete flip mà chưa write reconciliation table
- ❌ "Item X defer to Wave N+1" trong commit body mà KHÔNG file gap
- ❌ "Live verify defer post-AWS-restore" mà KHÔNG file gap reference GAP-612 unblock condition
- ❌ "Out-of-scope" claim mà KHÔNG có rationale + audit trail
- ❌ Wave-history.jsonl `followup` field replace gap filing (followup là note, KHÔNG enforce)

---

## 4. Why this rule exists — 2 recurrence pattern

### Recurrence #1 (2026-05-15/17, Wave 87/88) — CF DNS cutover orphan

Wave 87 plan §3 Bucket D propose CF apex DNS cutover Vercel → EC2 self-host via PR #1466 5-gate. PR #1466 merged 2026-05-16 (workflow code) NHƯNG cutover thực thi DEFER Wave 88 Bucket D dev-trigger. Wave 87 closure shipped status: complete; Wave 88 plan §3 Bucket D ghi "CF apex cutover defer Wave 88 Bucket D dev-trigger".

Result: workflow shipped nhưng cutover thực never executed at closure time — silent orphan trong scope của Wave 87 + Wave 88. User flagged 2026-05-18 thông qua câu "giống như vụ cấu hình DNS từ CF sang self-host".

### Recurrence #2 (2026-05-18, Wave 92) — 3 orphan items

Wave 92 plan §3 Scope 5 buckets. Closure PR #1517 shipped status: complete. 6 items pending khi audit hôm nay:

| # | Item | Tracked ở đâu | Verdict |
|---|---|---|---|
| 1 | GAP-521 other admin controllers annotation | gap file PARTIAL 85 notes | ✅ Tracked (PARTIAL) |
| 2 | GAP-521 FE admin UI hiển thị enrichment | gap file PARTIAL 85 notes | ✅ Tracked (PARTIAL) |
| 3 | GAP-599 live multi-tab UX verify | gap file PARTIAL 85 notes | ✅ Tracked (PARTIAL) |
| 4 | Wave 92 post-wave audit suite ≤3 ngày | Chỉ wave-history `followup` field + ROADMAP narrative | ❌ **ORPHAN** |
| 5 | Live verify 3 admin v1 controllers | Chỉ wave-history mention | ❌ **ORPHAN** |
| 6 | Live verify GAP-432 boundary + GAP-600 IT prod-equiv | Chỉ wave-history mention | ❌ **ORPHAN** |

3/6 orphan = silent loss risk khi context flush. Closure PR claimed status: complete misleading signal.

→ Class incident: "wave shipped với scope partial nhưng status: complete = misleading signal, không enforce orphan-prevention".

---

## 5. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 5.1 Reviewer-checklist (active now — paired same-PR)

Khi review wave closure PR (title chứa "closure" / "SHIPPED" / "Wave N closure"), reviewer hỏi:

- [ ] Closure PR body có Scope-Completeness Reconciliation table?
- [ ] Mỗi plan §3 Scope item liệt kê + categorized ✅/🟡/❌?
- [ ] PARTIAL items có gap link với Status:PARTIAL + completion_pct?
- [ ] NOT-IMPLEMENTED items có (a) follow-up gap link, (b) out-of-scope rationale, OR (c) `WAVE_SCOPE_DROP:` trailer?
- [ ] wave-history.jsonl `followup` field synced với gap files (KHÔNG replace gap)?

### 5.2 PR template extension (deferred per premature-rule guard ≥7 ngày)

Future enhancement: `.github/PULL_REQUEST_TEMPLATE.md` Output Review section add row:
> - [ ] **Wave closure scope completeness** — nếu PR flip wave plan status: complete, closure PR body chứa Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3

Defer until 2nd flake / next wave closure to verify reviewer-checklist sufficient first.

### 5.3 Detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

Future: `session-docs-check` Rule N — scan wave plan `status: complete` diff line; require matching closure PR body OR wave plan §7 contains "Scope-Completeness Reconciliation" heading + table rows ≥ plan §3 bucket count. Defer until 2nd recurrence after rule lands.

### 5.4 Override mechanism

Genuine wave-scope-drop exception (rare):

```
git commit -m "...
WAVE_SCOPE_DROP: <plan-scope-item-id> — <reason — e.g. 'business pivot mid-wave deprioritizes feature X to Phase 2'>
WAVE_SCOPE_DROP_FOLLOWUP: <gap link OR ADR link justifying scope cut>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review (likely scope-decision discipline issue, không phải rule mis-scoped).

### 5.5 Sister-rule cross-reference

Rule này extends `gap-done-discipline.md` §3 PARTIAL exit ramp từ per-gap → per-wave. Cùng pattern: enumerate completed/PARTIAL/dropped items, follow-up gap mandatory cho non-DONE.

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Flip wave plan status: complete vì "5/5 buckets merged" | Verify mỗi plan §3 item reconcile separately (multi-bucket items có thể partial) |
| Defer "live verify" mà KHÔNG file gap | File follow-up gap với block condition (vd "gated GAP-612 AWS restore") |
| Wave-history.jsonl `followup` field = sole tracking cho orphan items | `followup` là note, gap file là canonical track |
| ROADMAP §Pending list replace gap filing | ROADMAP narrative + gap file dual-track (rule yêu cầu gap file) |
| Closure PR body chỉ liệt kê DONE buckets, không mention pending | Table với mọi item — DONE + PARTIAL + NOT-IMPLEMENTED |
| Post-wave audit suite cadence-only catch (per `post-wave-audit-mandate.md`) | Catch nhưng KHÔNG track cụ thể wave nào missing audit → file gap explicit per wave |

---

## 7. Worked self-test

### 7.1 Recurrence #1 — Wave 87/88 DNS cutover (retroactive apply)

**State at Wave 87 closure (2026-05-17):**
- Plan §3 Bucket D ghi "CF apex cutover via PR #1466 5-gate"
- PR #1466 merged 2026-05-16 (workflow code shipped)
- Cutover thực thi DEFER Wave 88 Bucket D dev-trigger

**Apply §3 reconciliation table retroactively:**

| Plan §3 item | Verdict | Follow-up |
|---|---|---|
| Bucket D CF apex cutover workflow code | ✅ DONE | PR #1466 |
| Bucket D CF apex cutover EXECUTE | ❌ NOT-IMPLEMENTED | **Required gap link** (Wave 88 Bucket D) |

Wave 87 closure actual: status: complete flip + Wave 88 plan §3 mention defer. ❌ KHÔNG có gap file dedicated cho "execute cutover" — orphan trong Wave 87 scope.

Verdict nếu rule áp dụng lúc đó: closure PR sẽ require gap filing cho execute step → trackable across context flush → user wouldn't have to surface manually 1 ngày sau.

### 7.2 Recurrence #2 — Wave 92 6-item (retroactive apply)

**State at Wave 92 closure PR #1517 (2026-05-18):**

| Plan §3 item | Verdict | Tracked? |
|---|---|---|
| Bucket A — V61 + 5 columns + 3 IT (Phase 2 enrichment) | ✅ DONE | PR #1513 |
| Bucket A — Other admin controllers annotation | 🟡 PARTIAL | GAP-521 file Status:PARTIAL 85 |
| Bucket A — FE admin UI hiển thị enrichment | 🟡 PARTIAL (subsumed in GAP-521) | GAP-521 |
| Bucket B — BE findAll bounded + 5 boundary tests | ✅ DONE | PR #1515 |
| Bucket B — FE JWT sessionStorage migration | ✅ DONE (test JSDOM PASS) | PR #1515 |
| Bucket B — Live multi-tab browser UX verify | 🟡 PARTIAL gated AWS | GAP-599 file Status:PARTIAL 85 |
| Bucket C — beta_request abort scheduler + V53 + 11 tests | ✅ DONE | PR #1512 |
| Bucket D — NEW rule professional-manual + 3 admin v1 controllers | ✅ DONE | PR #1514 |
| Bucket E — 3 NEW gap files filed | ✅ DONE | PR #1511 |
| Post-wave audit suite ≤3 ngày | ❌ NOT-IMPLEMENTED | ❌ **ORPHAN** — wave-history followup only |
| Live verify 3 admin v1 controllers | ❌ NOT-IMPLEMENTED | ❌ **ORPHAN** |
| Live verify GAP-432 boundary + GAP-600 IT prod-equiv | ❌ NOT-IMPLEMENTED | ❌ **ORPHAN** |

3 ORPHAN items = silent loss risk. Closure PR claimed status: complete misleading.

Verdict nếu rule áp dụng lúc đó: closure PR sẽ require 3 follow-up gap files (GAP-619/620/621) trước flip complete → orphan-prevention enforced.

→ Rule fires correctly trên cả 2 recurrence. Self-test PASS ✅

**Counterfactual cost-save:** 1-2 user round-trip eliminated per wave closure (no need user surface orphan items manually). Class incident eliminated for future waves.

---

## 8. Auto-load justification (per `context-budget-mandate.md` §3)

Rule này dùng `paths:` frontmatter (`documents/03-planning/waves/**`) — path-scoped MANDATORY, KHÔNG always-load. Lý do:

- Rule trigger chỉ khi wave plan / closure context (file-scope rõ ràng)
- Token cost ~1.2k × mỗi wave closure window (3-5 lần per quarter)
- Reviewer-checklist sufficient cho v1.0.0; detector defer per premature-rule guard
- Re-evaluate priority bump CRITICAL nếu 3rd recurrence within 90 ngày

---

## 9. Relationship to other rules

- **`gap-done-discipline.md`** §3 PARTIAL exit ramp — sister rule per-gap level; rule này extends per-wave level
- **`audit-to-gap-pipeline.md`** Step 5 ROADMAP — gap filing pipeline; rule này thêm mandate "wave closure MUST file follow-up gap cho orphan items"
- **`post-merge-sync-completeness.md`** §2 — 4 sync targets (CSV/ROADMAP/wave-history/MEMORY); rule này extends với "scope-completeness reconciliation table" as 5th implicit target
- **`post-wave-audit-mandate.md`** §2.2 — post-wave audit cadence (catch-after-fact); rule này prevent-before-fact (require gap filing AT closure time)
- **`incident-to-rule-pipeline.md`** — master pipeline; rule này = direct output 2026-05-18 user-flagged 2nd recurrence
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + 3 follow-up gap files + worked self-test all ship same PR
- **`meta-csv-index-pattern.md`** §6 — rules-index.csv row added cho rule này (100% coverage)
- **`output-review-mandate.md`** §3 — adds row "Wave closure scope completeness" tracking review standard

---

## 10. Open Items / Follow-ups

- [ ] Detector wiring (`session-docs-check` Rule N) — defer ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard. File follow-up gap nếu 2nd recurrence post-rule-merge happens.
- [ ] PR template checkbox extension (§5.2) — defer same window
- [ ] Memory auto-load (`feedback_wave_closure_scope_completeness.md`) — defer per same guard

---

## 11. Log

- **2026-05-18 (v1.0.0):** Rule created in response to user-flagged 2nd recurrence (CF DNS cutover Wave 87/88 + Wave 92 3-orphan-items) — class pattern "wave shipped status:complete nhưng scope-pending items orphan". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user "vậy các tasks còn lại của wave 92 sẽ xử lý như thế nào, hay nó thành risk, nên update meta để cover vấn đề wave closure rồi mà vẫn còn vấn đề trong scope, giống như vụ cấu hình DNS từ CF sang self-host không?") → Classify ✓ (no existing rule covers wave-level scope completeness; `gap-done-discipline.md` §3 per-gap; `audit-to-gap-pipeline.md` §5 ROADMAP only; `post-merge-sync-completeness.md` §2 4-targets only; `post-wave-audit-mandate.md` §2.2 cadence catch-after-fact) → Rule+Enforce ✓ (this file + 3 follow-up gap files GAP-619/620/621 cho Wave 92 orphan items + rules-index.csv row + output-review-mandate.md §3 row + ROADMAP backfill + reviewer-checklist + worked self-test on 2 recurrences per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example on Wave 87/88 DNS + Wave 92 — rule fires correctly + counterfactual eliminate 1-2 user round-trip per wave) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint extending sister rule `gap-done-discipline.md`; no constraint loosening; existing wave closures grandfathered; rule applies prospectively từ Wave 93+). Path-scoped per `context-budget-mandate.md` §3.1 (`paths: ["documents/03-planning/waves/**"]`). Detector + PR template + memory auto-load deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày — v1.0.0 enforcement = reviewer-checklist + 3 follow-up gaps + worked self-test sufficient.
