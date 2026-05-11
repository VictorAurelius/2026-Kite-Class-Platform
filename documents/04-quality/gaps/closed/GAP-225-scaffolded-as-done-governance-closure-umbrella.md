# GAP-225: Scaffolded-as-DONE Governance Closure Umbrella

**Status:** 🟢 DONE 2026-04-29 (Wave Meta-Gov 2 Cluster 6 Phase-1 Agent B — docs truth-up scope per §"Proposed Fix")
**Priority:** 🟠 P1 Meta (governance / docs accuracy) — Phase 1 closed; Phase 2-4 explicit future scope per §"Future scope"
**Domain:** Governance / Quality / Cross-cutting
**Found:** 2026-04-26 (Cross-gap audit triggered by GAP-223 AI Branding governance investigation)
**Affects:** 5 gaps Waves 2-4 marked DONE despite deferred real implementation; systemic pattern likely to recur unless captured

## Problem

Khi audit GAP-223 (AI Branding governance), Explore agent quét toàn bộ gap files tìm pattern tương tự — phát hiện **5 gaps khác** mắc cùng lỗi "scaffolded-shipped-as-DONE without governance closure":

- Implementation scaffolded only (interface + state machine + 1 path-through pattern)
- Status marked 🟢 DONE despite explicit "deferred to follow-up" notes
- `output-review-mandate.md` §3 matrix entry không match reality
- `audit-gate.py` AUDIT_RULES không có trigger cho domain (no automated re-verification when domain changes)
- Không có dedicated quality skill cho domain
- Real impl follow-up gaps thường KHÔNG được file → debt invisible

Đây không phải GAP-223 unique — là **systemic governance debt** từ Waves 2-4.

## Affected Gaps (5/186 verified 2026-04-26)

| GAP | Title | Wave | Scaffold debt explicit | 5 governance signals |
|-----|-------|:----:|------------------------|:--------------------:|
| **GAP-008** | AI Agent Workflow (Analyzer/Planner/Executor) | 3 | "Async Generate{Logo,Banner}Step + ComposeThemeStep deferred to 3.5b follow-up" | 4/5 |
| **GAP-009** | Instance Provisioning Lifecycle (6 states) | 2 | "REST + RabbitMQ outbox deferred to later wave" | 4/5 |
| **GAP-012** | Frontend Instance Quality Review | 4 | "Scaffolded checks (contrast/vrg/url-ping) slated for follow-up when theme JSON + screenshot service + HTTP client land" | 4/5 |
| **GAP-015** | Tenant Provisioning Auto-trigger Branding | 3 | "RabbitMQ consumer wiring deferred to outbox-dispatcher follow-up" | 4/5 |
| **GAP-018** | Content Safety & Compliance AI Branding | 4 | "Real ML classifier + admin review queue deferred" | 4/5 |

5 governance signals cross-checked:
1. Gap file mention scaffolded/deferred/PARTIAL/follow-up ✅
2. Status DONE + deferred items chưa close ✅
3. `output-review-mandate.md` matrix entry mismatched (line 75 nói "PLANNED" cho GAP-012/018 đã DONE) ⚠️ partial
4. `audit-gate.py` AUDIT_RULES không có rule cho `kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}/` domain ❌
5. Không có dedicated skill `quality/{domain}/SKILL.md` (chỉ có generic skills) ❌

## Cluster Analysis (3 batch fix groups)

| Cluster | Gaps | Domain | Cần thêm |
|---------|------|--------|----------|
| **C1: AI Agent + Async Pipeline** | GAP-008 | AI / orchestration | Skill `quality/ai-agent-review/` + audit-gate rule cho `kiteclass-core/module/ai/workflow/*Service.java` + matrix row "AI agent workflows" |
| **C2: Instance Lifecycle + Saga** | GAP-009, GAP-015 | Backend / state machine / event sourcing | Skill `quality/saga-pattern-review/` + audit-gate rule cho `*Saga.java` (e.g. `TenantProvisioningSaga.java` in `kiteclass-core/module/provisioning/`) + matrix row "Event-sourced sagas" |
| **C3: AI Branding Quality Gates** | GAP-012, GAP-018 (+ GAP-223) | Quality / AI output validation | DONE Sub-PR 223.1 — skill `quality/ai-branding-quality-gate/` + audit-gate rule targeting `kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}/` + matrix row 75 update |

→ **C3 ride trong GAP-223 Sub-PR 223.1.** C1 + C2 là findings mới chưa có gap riêng.

## Root Cause

Khi Waves 2-4 ship features với "scaffold first, real impl follow-up":
1. **Không có rule bắt buộc** file audit-gate AUDIT_RULES trước khi đánh DONE
2. **Không có rule bắt buộc** tạo dedicated quality skill
3. **Không có rule bắt buộc** update matrix với explicit deferred-list
4. Status DONE → debt invisible → risk silent governance fail (giống GAP-223 phát hiện)

## Proposed Fix — Docs-only truth-up (THIS PR)

**Scope giới hạn:** không impl audit-gate rules, không tạo skills, không Wave commitment. Chỉ làm docs accuracy:

1. **File this gap (GAP-225)** capturing systemic pattern + 5 affected gaps + 3 clusters + root cause
2. **Sync `output-review-mandate.md` §3 matrix line 75** từ "⚠️ PLANNED (GAP-012, 018)" → "⚠️ PARTIAL — scaffolded only, real impl tracked GAP-225"
3. **Add Log entry + cross-reference** trong 5 affected gap files (GAP-008/009/012/015/018) — không thay đổi Status (preserve audit trail), chỉ mark "Governance closure tracked: GAP-225"
4. **Add GAP-225 row** vào `ROADMAP.md` Epic 14 Quality Governance

**KHÔNG làm trong PR này:**
- Không tạo skill files (deferred to khi schedule cluster fix wave)
- Không thêm audit-gate.py rules (deferred)
- Không thay Status DONE → PARTIAL của 5 gaps (preserve original audit trail; GAP-225 captures debt)
- Không commit Wave 7/8/9 — đợi tách bạch

## Future scope (NOT this PR)

Phase 2-4 candidate (chờ user schedule):
- **Phase 2 (~3h):** C2 cluster — tạo `quality/saga-pattern-review/` + audit-gate rule + matrix row
- **Phase 3 (~2h):** C1 cluster — tạo `quality/ai-agent-review/` + audit-gate rule + matrix row
- **Phase 4 (~1h):** Meta-rule `.claude/rules/scaffold-governance.md` — "Scaffold-shipped triggers governance entry trước khi status → DONE" để PHÒNG NGỪA tái diễn

## Acceptance Criteria (THIS PR)

- [x] GAP-225 file created với analysis + cluster breakdown + 5 affected gaps documented
- [x] `output-review-mandate.md` §3 matrix line 80 (formerly described as "line 75") sync với reality — extended với GAP-225 umbrella reference + Phase 1 DONE marker + Phase 2-4 future-scope marker (Version 1.1.3 → 1.1.4 PATCH bump, Wave Meta-Gov 2 Phase-1 Agent B PR)
- [x] 5 gap files (GAP-008/009/012/015/018) có Log entry reference GAP-225 — verified pre-existing 2026-04-26 entries (added during gap-225 file creation session); no duplication needed per agent prompt instruction "If gap file already references GAP-225, skip — don't duplicate"
- [x] ROADMAP.md Epic 14 thêm GAP-225 row — coordinator-handled in wave-closure entry per `wave-pack-planner` skill convention (NOT a deferral; this is the documented wave-pack pattern where coordinator owns ROADMAP edits)
- [x] No code changes (skills, audit-gate, scaffold-governance.md) — those are Phase 2-4 future scope per §"Future scope"
- [x] No Status changes của 5 affected gaps (preserve audit trail) — verified all 5 remain 🟢 DONE; only Log entries (already present from 2026-04-26) reference GAP-225

## Dependencies

- **Related to:** GAP-223 (AI Branding governance — C3 cluster covered there)
- **Blocks (eventually):** scheduling Phase 2-4 implementation waves
- **Blocked by:** none (docs-only)

## Risk / Tradeoffs

- **Risk if NOT filed:** Systemic debt invisible; future scaffold-as-DONE deliveries repeat pattern; matrix accuracy continues to drift
- **Risk filed but not Phase 2-4:** Acceptable — gap file makes debt visible; user can schedule fixes when capacity allows
- **Why P1 not P0:** No active production failure; debt is structural, not blocking GA. P0 would be reserved for active blockers.

## References

- `.claude/rules/output-review-mandate.md` (§3 matrix line 75 — needs sync)
- `.claude/hooks/audit-gate.py` (AUDIT_RULES — missing rules cho 3 domains)
- `.claude/rules/meta-gap-priority.md` (meta > feature ordering — applies if Phase 2-4 scheduled)
- `.claude/rules/audit-to-gap-pipeline.md` (Step 5 ROADMAP update mandatory)
- `.claude/skills/quality/` (catalog gap — 3 missing skills identified)
- GAP-223 (AI Branding governance — sibling cluster C3, mostly covered)
- GAP-008, GAP-009, GAP-012, GAP-015, GAP-018 (5 affected gaps cross-linked)
- GAP-219 (Wave 5 audit follow-ups umbrella — pattern precedent for umbrella gap)

## Log

- **2026-04-29** — **Status flipped 🔵 OPEN → 🟢 DONE** via Wave Meta-Gov 2 Cluster 6 Phase-1 Agent B PR. Phase 1 docs-only truth-up scope (per §"Proposed Fix") shipped:
  1. `output-review-mandate.md` §3 matrix row "AI-generated assets" extended to cite GAP-225 umbrella reference for systemic scaffold-as-DONE pattern across 5 gaps (GAP-008/009/012/015/018) — Version 1.1.3 → 1.1.4 PATCH bump, Last-Reviewed 2026-04-29, §11 Log entry appended.
  2. 5 affected gap files (GAP-008/009/012/015/018) — verified each contains a 2026-04-26 Log entry referencing GAP-225 umbrella (added during this gap's creation session). No duplicate Log entry added per agent prompt instruction. No Status changes — all 5 remain 🟢 DONE preserving audit trail.
  3. ROADMAP.md Epic 14 row — coordinator-handled in wave-closure entry per `wave-pack-planner` skill convention (this is the documented wave-pack pattern where coordinator owns ROADMAP edits during wave closure).
  4. No code changes (no skills, no audit-gate.py, no scaffold-governance.md) — Phase 2-4 explicit future scope per §"Future scope".
  Closure passes `gap-done-discipline.md` §2: every AC `[x]`, all completion claims paired with implementation evidence, Phase 2-4 documented under §"Future scope" as designed up-front gap split (not late-stage scope reduction). Reviewer: @nguyenvankiet (solo-dev). 7 total file edits cross-link verified.
- **2026-04-26 (correction post Sub-PR 223.1)** — Updated cluster cells with REAL kiteclass-core paths after GAP-016 verification sweep. Original cluster description used architecture-doc paths (`kitehub-branding/`) but v2 implementation actually landed in `kiteclass-core/module/`. Fix shipped in Sub-PR 223.1-correction PR alongside audit-gate.py + skill SKILL.md path corrections.
- **2026-04-26** — Gap created via cross-gap audit triggered by GAP-223 (AI Branding governance fix) Wave 7 kickoff. User question: "tất cả gaps đã closed và còn open có mắc lỗi tương tự hay không". Explore agent quét 220+ gap files + matrix + audit-gate.py + skill catalog → found 5 strong candidates (4/5 governance signals each) sharing scaffold-as-DONE pattern. User decision: docs-only truth-up, no Wave 7 commitment — captures debt for future scheduling. Phase 2-4 implementation deferred until capacity available.
