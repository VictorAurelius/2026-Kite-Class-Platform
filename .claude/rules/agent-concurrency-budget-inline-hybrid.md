---
paths:
  - ".claude/skills/quality/wave-pack-planner/**"
  - "documents/03-planning/waves/**"
---

# Agent Concurrency Budget + Inline-Hybrid — ít agent, bù bằng inline, giữ wall-clock

**Priority:** 🟠 MANDATORY — agent orchestration efficiency governance
**Version:** 1.0.0
**Created:** 2026-06-11
**Last-Reviewed:** 2026-06-11
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (self-detection + reviewer-checklist + memory auto-load + worked self-test on 2026-06-11 demo-seed wave — coordinator idle khi spawn ≤1 agent) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "khi giảm concurrency tránh limit, bù wall-clock bằng inline bucket". Sister cho `agent-model-opus-default` (model axis) + `agent-background-spawn-default` (sync axis) + `feedback_parallel_agent_strategy` memory (max-5 cap) tại axis mới: concurrency-budget + inline-compensation)
**Applies to:** Mọi wave/task execution có ≥3 bucket disjoint khi cần giảm số agent concurrent để tránh server/rate limit. Out-of-scope: single-bucket task, task không có limit-concern (cứ theo max-5 per `feedback_parallel_agent_strategy`).

---

## 1. The Rule

> **Khi giảm số agent spawn để tránh server/rate limit, coordinator PHẢI lấp idle bằng cách tự execute bucket disjoint INLINE song song với agent đang chạy.** "Ít agent" đi kèm "coordinator làm inline" — KHÔNG phải "coordinator ngồi chờ agent". Mục tiêu: wall-clock thấp như fan-out, nhưng concurrency budget an toàn limit.

Spawn ít agent (vd ≤1-2 Opus) đúng để tránh rate-limit (mỗi Opus = throughput token cao per `agent-model-opus-default`). NHƯNG nếu coordinator chờ không → mất hết lợi ích wall-clock của parallelism. Bù lại: coordinator tự làm bucket disjoint (asset/config/docs/single-file edit) TRONG khi agent chạy bucket nặng/cohesive.

Force-multiplier: 1 chuẩn hybrid → mọi wave limit-bound subsequent giữ wall-clock thấp mà không chạm limit.

---

## 2. Decision model

### 2.1 Concurrency budget

| Tình huống limit | Budget agent concurrent | Phần bucket dư |
|---|---|---|
| Limit nghiêm trọng (đã/đang chạm rate-limit) | **≤1 Opus** | coordinator inline tất cả phần còn lại disjoint |
| Limit-concern vừa (muốn phòng) | **≤2 Opus** | inline phần dư disjoint |
| Không limit-concern | max-5 per `feedback_parallel_agent_strategy` | (rule này không fire) |

### 2.2 Bucket → agent vs inline (phân loại)

| Bucket đặc điểm | Giao cho |
|---|---|
| Nặng + cohesive (1 file lớn cần iterate compile/test, vd seeder/service) | **Agent** (Opus, background) — agent tự loop compile-fix |
| Nhỏ + disjoint (asset fix, gitignore, config, single-file edit, docs, CSV row) | **Inline** (coordinator tự làm song song) |
| Cross-cutting cần nhiều file đọc/sửa rời | Agent nếu budget còn; inline nếu coordinator rảnh |

### 2.3 Nguyên tắc lấp idle

Sau khi spawn agent(s) trong budget → **TRƯỚC khi "chờ", coordinator phải hỏi: còn bucket disjoint nào tôi làm inline ngay được không?** Nếu CÓ → làm inline (không chờ). Chỉ "chờ notification" khi đã hết bucket inline-able HOẶC phần còn lại phụ thuộc output agent đang chạy.

---

## 3. Required behavior khi rule fires

```
1. Đếm bucket disjoint + đánh giá limit-concern → chọn budget §2.1
2. Phân loại bucket §2.2: heavy-cohesive → agent; small-disjoint → inline
3. Spawn agent(s) trong budget (background, Opus)
4. NGAY sau spawn: coordinator execute bucket inline-able song song (KHÔNG chờ)
5. Chỉ chờ notification khi hết bucket inline-able OR phần dư depends-on agent output
6. Agent xong → integrate (review/compile) → tiếp bucket phụ thuộc
```

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Spawn ≤1 agent rồi coordinator ngồi chờ trong khi còn bucket disjoint | Làm bucket disjoint inline song song với agent |
| Spawn 4-5 agent "cho nhanh" khi đang lo limit | ≤1-2 trong budget + bù inline |
| Giao bucket nhỏ disjoint (asset/config) cho agent rồi chờ | Coordinator tự làm inline ngay |
| "Để E/F sau khi agent #1 xong" khi E/F disjoint khỏi #1 | Làm E/F inline NGAY trong khi #1 chạy |
| Inline bucket cohesive đang được agent làm (đụng cùng file) | Chỉ inline bucket disjoint — tránh conflict |

---

## 5. Override mechanism

Genuine exception (mọi bucket còn lại đều depends-on agent output, không có gì inline-able):

```
inline note: "AGENT_CONCURRENCY_INLINE_NA: <reason — e.g. tất cả bucket dư phụ thuộc output seeder agent, không có disjoint inline-able>"
```

Pattern frequency >20%/quarter → meta-review (có thể bucket decomposition chưa tối ưu disjoint).

---

## 6. Worked self-test — demo-seed wave 2026-06-11 (originating incident)

**Scenario:** Wave demo-seed-1 có 6 bucket (A-D academic seeder cohesive 1 file / E landing sections / F asset fix). User chỉ thị "spawn ít agent tránh limit". Tôi spawn **1 Opus agent** (academic A-D, đúng budget ≤1) — tốt cho limit. NHƯNG rồi báo user "E/F làm SAU khi #1 xong" → **coordinator idle trong khi #1 chạy** (lãng phí wall-clock = full thời lượng agent #1).

**Apply rule retroactively:**
- §2.2: Bucket F (asset: gitignore + git rm PNG + webp + tách logo) = **small-disjoint** → inline-able, KHÔNG đụng file seeder agent #1.
- §2.3: ngay sau spawn #1 → coordinator làm **F inline song song** (+ viết rule meta này inline — chính là pattern đang áp dụng).

| Metric | Without rule (idle) | With rule (inline-hybrid) |
|---|---|---|
| Agent concurrent | 1 (an toàn limit) | 1 (an toàn limit) |
| Coordinator khi #1 chạy | ❌ chờ | ✅ làm F + meta inline |
| Wall-clock | #1 + (F sau) tuần tự | max(#1, F inline) — F "miễn phí" |
| Limit risk | thấp | thấp (như nhau) |

**Save:** ~thời lượng Bucket F (+ meta) overlap vào thời gian agent #1 chạy → wall-clock giảm mà limit-risk không tăng. Self-test PASS ✅ — rule fires đúng trên chính session sinh ra nó (viết rule này = inline work song song agent #1).

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Self-detection (in-turn, active now)
Ngay sau khi spawn agent(s) trong budget, TRƯỚC khi nói "chờ" / kết thúc turn:
- Còn bucket disjoint inline-able nào không? Nếu CÓ → làm inline ngay, không chờ.
- Nếu chỉ còn bucket depends-on agent → chờ hợp lệ.

### 7.2 Reviewer-checklist (active now)
Khi review wave execution / coordinator session:
- [ ] Số agent concurrent ≤ budget §2.1 khi limit-concern?
- [ ] Coordinator có làm bucket disjoint inline song song (không idle) khi agent chạy?
- [ ] Bucket inline disjoint khỏi file agent đang sửa (không conflict)?

### 7.3 Memory auto-load (paired same-PR)
`feedback_agent_concurrency_budget_inline.md` nhắc checklist §7.1 tại session start (luôn-on, bù cho path-scope rule).

### 7.4 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)
- **Complexity:** phát hiện "coordinator idle khi còn bucket inline-able" cần phân tích reasoning + bucket dependency graph — NLP, không trivial.
- **Recurrence:** 1 (2026-06-11).
- **Decision:** self-detection §7.1 + reviewer-checklist + memory + worked self-test đủ cho v1.0.0; revisit khi recurrence ≥2.

### 7.5 Override — per §5.

---

## 8. Atomic-unique-bar check (per `rule-change-process.md` §5.1)
- ✅ **Atomic:** single concept = concurrency budget + inline compensation
- ✅ **Unique:** `agent-model-opus-default` = model axis; `agent-background-spawn-default` = sync axis; `feedback_parallel_agent_strategy` = max-5 cap (upper bound); rule này = LOWER concurrency intentionally + inline-fill (khác axis)
- ✅ **Widely applicable:** mọi wave limit-bound
- ✅ **Body discipline:** §1 ≤2 conjunction

---

## 9. Relationship to other rules
- **`agent-model-opus-default.md`** — Opus mỗi agent (lý do throughput cao → cần budget); compose.
- **`agent-background-spawn-default.md`** — background spawn cho phép coordinator làm inline song song; compose trực tiếp.
- **`feedback_parallel_agent_strategy.md`** (memory) — max-5 cap; rule này chọn THẤP HƠN có chủ đích khi limit + bù inline.
- **`agent-action-bias.md`** §1 Part A — "do it yourself"; rule này = "do disjoint buckets yourself khi giảm agent".
- **`docs-only-pr-no-block-wait.md`** — không block-wait; rule này = không idle-wait khi còn inline work.
- **`incident-to-rule-pipeline.md`** — rule này = direct output 2026-06-11 user direction qua 5-stage.
- **`rule-change-process.md`** §6.5 — rule + self-detection + reviewer-checklist + memory + worked self-test + rules-index row + output-review-mandate §3 row same PR.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier.
- **`feedback_agent_concurrency_budget_inline.md`** (memory, paired same-PR).

---

## 10. Log
- **2026-06-11 (v1.0.0):** Rule created per user direction 2026-06-11 "sửa meta, để tránh server limit sẽ spawn ít agent và làm inline bucket để bù vào, mục tiêu là tiết kiệm thời gian". Triggered by same-session demo-seed wave: spawned 1 Opus agent (academic seeder, đúng budget ≤1 tránh limit) nhưng định để Bucket E/F "sau khi #1 xong" → coordinator idle. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged) → Classify ✓ (no existing rule covers concurrency-budget-LOW + inline-compensation; sister rules cover model/sync/max-cap axes) → Rule+Enforce ✓ (this file + self-detection §7.1 + reviewer-checklist §7.2 + memory paired + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row per §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 — rule fires đúng trên chính session; viết rule này inline song song agent #1 = pattern) → Retro Log ✓. META P1 force-multiplier per `meta-gap-priority.md` §3. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint; no loosening; prior waves grandfathered; prospective). Detector (§7.4) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 1, NLP complexity). Path-scoped per `context-budget-mandate.md` §3.1 (wave-pack-planner + waves) + memory always-on bù.
