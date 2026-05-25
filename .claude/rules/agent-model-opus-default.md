# Agent Model Opus Default — spawn Opus 4.7 cho mọi non-trivial Agent

**Priority:** 🟠 MANDATORY — agent reliability governance
**Version:** 1.0.0
**Created:** 2026-05-25
**Last-Reviewed:** 2026-05-25
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (memory auto-load + reviewer-checklist + worked self-test on Wave beta-readiness-8 Đợt 1 2026-05-25 + Wave br-4 recurrence) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies Wave br-4 lesson "Sonnet thrash → Opus retry success" pattern observed recurring ≥2 waves)
**Applies to:** Mọi `Agent` tool invocation (subagent spawn) trong project — `general-purpose`, `Explore`, `Plan`, hoặc bất kỳ subagent type nào. Out-of-scope: `statusline-setup`, `init` (single-shot config skills, không qua thrash threshold).

---

## 1. The Rule

> **Mọi Agent tool invocation PHẢI set `model: "opus"`** (Opus 4.7 1M context) unless §3 exception applies. Sonnet 4.6 4 default model — KHÔNG được dùng cho agent spawn vì có failure mode "autocompact thrash" trên non-trivial prompts.

Rule sharpens `agent-background-spawn-default.md` (cùng family — agent invocation discipline) với axis khác: model selection (Opus mandatory) thay vì sync mode (background mandatory). Cả hai apply mỗi spawn.

---

## 2. Why — recurrence pattern ≥2 waves

| Wave | Date | Failure mode | Recovery |
|---|---|---|---|
| **Wave br-4 audit suite** | 2026-05-22 | ALL 4 Sonnet 4.6 audit agents thrashed first attempt (autocompact 3x trong 3 turns) | 4 Opus 1M retry SUCCESS — shipped 4/4 audit reports |
| **Wave beta-readiness-8 Đợt 1** | 2026-05-25 | 2/3 Sonnet bg-agents thrashed (Bucket A + Bucket E — autocompact 3x); 1 Bucket D additionally leaked work to main via absolute path violation | 3 Opus 4.7 retry in progress (this rule's enforcement parity ship same session) |

**Recurrence count ≥ 2 distinct waves** = systemic Sonnet failure mode, không phải noise.

### Failure mode mechanism

Sonnet 4.6 thrashes khi:
- Agent prompt context + rule auto-load (path-scoped) + tool output > Sonnet effective working window
- Autocompact compresses recent turns → context refill immediate next turn (rule auto-loads still apply) → compresses again → 3x in 3 turns = "thrashing" signal
- Agent stops returning useful work after 3rd compact (per Anthropic harness rules)

Opus 4.7 (1M context model ID `claude-opus-4-7[1m]`) handles same prompt + auto-load + tool output comfortably — no compact triggered cho realistic agent prompts.

### Cost reasoning

Opus token cost ~5x Sonnet. But:
- 1 Sonnet thrash = full agent spawn cost (0 output) + retry cycle wall-clock cost
- 2 Sonnet retry > 1 Opus first-try success
- Wave coordinator round-trip cost (user round-trip + diagnose + decide retry) >> token delta

Cost-benefit: Opus default ROI positive khi recurrence count ≥ 2.

---

## 3. Allowed exceptions (rare)

`model: "sonnet"` (or omit, since harness default may be Sonnet) ACCEPTABLE ONLY khi:

| Case | Why exempt | Example |
|---|---|---|
| **Single keyword/file location lookup** (Explore < 2 min wall) | Single grep, không cần reasoning depth | "Find which file imports X" → 1 grep return |
| **Statusline / init / one-config-edit agents** | Out-of-scope per `Applies to`; single tool call | `statusline-setup`, `init` skills |
| **Cost-bound experiment, user explicit override** | User testing Sonnet on simple scope | "Try Sonnet to compare speed" — user-directed |
| **Haiku 4.5 task fitness** (future, not enabled now) | Some scopes truly Haiku-fitting | TBD when Haiku tested |

Khi invoke exception, state inline in agent prompt OR commit body: "Per `agent-model-opus-default.md` §3 row <X>".

---

## 4. Override mechanism

Genuine Sonnet/Haiku case ngoài §3 list:

```
AGENT_MODEL_SONNET_OVERRIDE: <reason — e.g. 'cost experiment on docs-only scope', 'parallel-of-N + Opus budget constraint'>
```

Trailer logged trong quarterly retro. Pattern frequency > 5% trong quarter → meta-review của §3 exception list.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Spawn 4-5 bg-agents Sonnet "vì rẻ hơn" | Opus 4.7 default — 1 success > 2 retry |
| Trust Sonnet sau 1 success "vì hôm trước OK" | Recurrence ≥ 2 waves đã chứng minh thrash; default Opus |
| Skip rule "vì agent prompt nhỏ" | Even small prompts + path-scoped rule auto-load + tool output → thrash threshold |
| Omit `model:` field "để dùng harness default" | Explicit `model: "opus"` mọi spawn — không trust default |
| Sonnet cho audit agents | Audit agents = reasoning-heavy = Opus mandatory (per Wave br-4 lesson) |
| Sonnet cho code-write bucket agents | Bucket agents (Wave pattern) = full implementation = Opus mandatory |
| Foreground Sonnet "vì foreground tránh thrash" | Background + Opus = both rules apply, không conflict |

---

## 6. Self-test — Wave beta-readiness-8 Đợt 1 incident 2026-05-25

**Scenario:** Coordinator spawned 3 bg-agents Sonnet 4.6 với `run_in_background: true` cho Đợt 1:
- Bucket A (a729e): ImmutableConsentController IDOR fix
- Bucket D (a8368): Course.pricingModel default
- Bucket E (af55f): PricingModel javadoc

**Actual outcome (Sonnet, this rule's counterfactual):**
- A: **thrashed** (autocompact 3x trong 3 turns) — 0 output
- D: leaked work via absolute path violation; agent worktree empty
- E: **thrashed** (autocompact 3x) — 0 output
- Failure rate: **2/3 = 67%** + 1 partial leak

**Counterfactual với rule (Opus 4.7 from start):**
- 3 Opus agents from start (đang retry now post-rule-creation)
- Wave br-4 lesson: 4/4 Opus retry SUCCESS → expected same pattern
- Failure rate projected: **~0/3** = matches Wave br-4 retry success rate

**Cost-save quantification:**
- 2 wasted bg-agent spawns (A + E thrash) ≈ ~30 min wall-clock + token waste
- 1 manual salvage operation (Bucket D leak recovery) ≈ ~15 min coordinator cost
- 1 user round-trip (Q "spawn 3 Opus retry?" decision) ≈ ~5 min
- Total preventable cost: ~50 min wall-clock + cognitive overhead per wave

**Verdict:** Rule fires correctly trên originating incident — Wave beta-readiness-8 Đợt 1 retry pattern (3 Opus parallel) là exactly what rule mandates. Self-test PASS ✅

---

## 7. Auto-load justification (per `context-budget-mandate.md` §3.2)

Rule này KHÔNG dùng `paths:` frontmatter — luôn auto-load mỗi session. Lý do:

- **Cross-cuts mọi agent spawn moment** — decision happen tại Agent tool invocation runtime, không tại file-read time. Không có natural file-scope trigger.
- **Path-scope sẽ miss critical case** — nếu scope `.claude/skills/**`, rule vắng mặt khi coordinator spawn agents ngoài skill context (mọi wave-pack execution). Đúng case rule cần fire nhất.
- **Hook-cover không khả thi v1** — pre-tool-call inspection Agent tool args để verify model=opus có thể làm nhưng cost-benefit chưa qua per `incident-to-rule-pipeline.md` premature-rule guard.
- **Token cost chấp nhận được** — ~1.1k tokens × mỗi session; force-multiplier mỗi spawn save 1 thrash retry cycle khi rule fires đúng.
- **Priority 🟠 MANDATORY giữ nguyên** — không nâng CRITICAL vì §3 exception list cho phép defer; auto-load áp dụng theo `context-budget-mandate.md` §3.2 row 2.

Re-evaluate nếu: (a) Sonnet 4.7+ release fixes thrash mode, (b) Haiku 4.5 proves fit for bucket-scale agents, (c) Anthropic publishes pre-tool-call NLP hook, (d) > 5 false-positive override trong quarter.

---

## 8. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 8.1 Memory auto-load (per-session)

Memory entry `feedback_agent_model_opus_default.md` (paired same-PR) loads at session start. 4-bullet checklist:
1. Mọi Agent tool spawn → `model: "opus"`
2. Sonnet OK ONLY cho §3 exception (single-lookup Explore / statusline / user-explicit)
3. Wave br-4 + Wave beta-readiness-8 Đợt 1 = recurrence ≥2 → systemic
4. Cost ROI positive khi recurrence ≥2 wave

### 8.2 Self-detection mỗi turn

Trước khi gọi Agent tool, coordinator mentally run check:
- Set `model: "opus"`?
- Nếu KHÔNG → match §3 exception nào? Justify inline?
- Nếu KHÔNG match → upgrade to opus trước khi invoke

### 8.3 Reviewer-checklist (manual)

Khi review skill file / agent prompt template / wave-pack-planner skill update touching agent spawn examples:
- [ ] Agent invocation examples set `model: "opus"`?
- [ ] §3 exception cited inline nếu Sonnet?

### 8.4 Wave-pack-planner skill cross-reference

`.claude/skills/quality/wave-pack-planner/reference/agent-spawning-template.md` (existing) — update để mention Opus default per this rule. Cross-link added trong §9 Relationship.

### 8.5 Detector (deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions)

- **Detector complexity:** Pre-tool-call hook inspect Agent tool args để verify `model` field — moderate complexity (PreToolUse hook + JSON parse args)
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-25)
- **FP risk:** Low — exception list narrow, clear binary check
- **Decision:** Reviewer-checklist §8.3 + memory auto-load §8.1 + worked self-test §6 sufficient cho v1.0.0; revisit detector when recurrence-count ≥1 post-rule (Sonnet spawn slip-through)

Future hook (when implemented):

```python
# .claude/hooks/agent-model-check.py
# Pre-tool-call hook: inspect Agent tool args
if tool_name == "Agent":
    model = args.get("model")
    if model != "opus" and not args.get("subagent_type") in ["statusline-setup", "init"]:
        warn("Agent model != opus — verify §3 exception per agent-model-opus-default.md")
```

### 8.6 Override mechanism

Per §4 trailer `AGENT_MODEL_SONNET_OVERRIDE:` — logged quarterly retro. Pattern frequency > 5% → meta-review.

---

## 9. Relationship to other rules

- **`agent-background-spawn-default.md`** v1.0.1 — sister rule (same family agent invocation discipline). That rule covers `run_in_background: true` axis; this rule covers `model: "opus"` axis. Both apply mỗi spawn — compose cleanly.
- **`agent-action-bias.md`** v1.0.1 — "do it yourself" governance. Orthogonal axis: WHEN to spawn agent. Once decision = spawn, this rule + agent-background-spawn-default both apply.
- **`feedback_parallel_agent_strategy.md`** (memory) — wave-pack methodology max 5 concurrent agents. This rule sharpens: 5 max × Opus default = max parallelism + reliability.
- **`feedback_worktree_absolute_path_contamination.md`** (memory) — orthogonal concern (path discipline). Bucket D leak 2026-05-25 violated both rules: Sonnet thrash recovery + absolute path.
- **`incident-to-rule-pipeline.md`** — this rule = direct output 2026-05-25 Wave beta-readiness-8 Đợt 1 incident (2/3 Sonnet thrash) applied through 5-stage pipeline.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory auto-load + worked self-test §6 + rules-index.csv row + output-review-mandate.md §3 row all ship same PR.
- **`context-budget-mandate.md`** §3.2 — this rule always-load justified §7 (cross-cutting agent-spawn decision moment).
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (fix 1 chuẩn → mọi agent spawn subsequent auto-comply prospectively).
- **`output-review-mandate.md`** §3 — paired same-PR với new matrix row "Agent model selection" tracking standard.
- **`feedback_agent_model_opus_default.md`** (memory, paired same-PR per Enforcement Parity).

---

## 10. Log

- **2026-05-25 (v1.0.0):** Rule created in response to user direction 2026-05-25 "thêm rule vào dự án, luôn spawn Opus 4.7 cho agents để tránh lỗi trên" — sau khi 2/3 Sonnet bg-agents thrashed trong Wave beta-readiness-8 Đợt 1 (Bucket A + Bucket E autocompact 3x; Bucket D additional absolute-path leak). Recurrence ≥2 waves confirmed pattern (Wave br-4 2026-05-22 ALL 4 Sonnet audit agents thrashed → 4 Opus retry success). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged + 2/3 today + recurrence Wave br-4) → Classify ✓ (no existing rule mandates Opus default; `agent-background-spawn-default.md` covers sync mode only, `agent-action-bias.md` covers when-to-spawn only) → Rule+Enforce ✓ (this file + memory `feedback_agent_model_opus_default.md` paired same-PR + reviewer-checklist + rules-index.csv row + output-review-mandate.md §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên Wave beta-readiness-8 Đợt 1 originating incident — rule fires correctly + counterfactual ~50 min wall-clock saved + Wave br-4 lesson confirms 4/4 Opus retry success pattern) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi agent spawn subsequent auto-comply. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered agent model selection class; no constraint loosening; existing Sonnet spawns grandfathered until next session; rule applies prospectively từ this PR forward 2026-05-25). Atomic-unique-bar §5.1 check: ✅ atomic (single concept: Opus default for agents) ✅ unique (sister rule agent-background-spawn-default different axis sync-mode) ✅ widely applicable (every Agent tool invocation) ✅ body discipline §1 ≤2 "and" conjunctions. Detector (§8.5 PreToolUse hook) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + memory auto-load + worked self-test §6 sufficient cho v1.0.0.
