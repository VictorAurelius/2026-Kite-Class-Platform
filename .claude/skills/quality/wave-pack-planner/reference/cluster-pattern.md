# Cluster Pattern — When and How to Group Gaps

Detail cho Step 1 + Step 3 trong [SKILL.md](../SKILL.md). Đọc khi cần quyết định "có nên cluster cái này không?".

## Definition

**Cluster** = 3-7 OPEN gaps cùng theme/domain mà files chạm vào DISJOINT (hoặc chỉ overlap SOFT). Cluster là đơn vị plan của 1 wave-pack.

- <3 gaps: overhead wave plan > save time → ship 1-2 single PRs
- 3-5 gaps: sweet spot, 1 wave-pack với 3-5 parallel agents
- 6-7 gaps: max range, đặt agent cap 5 (per [feedback_parallel_agent_strategy.md](../../../../projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/feedback_parallel_agent_strategy.md) rule #9), buckets 2 gaps/agent OK
- >7 gaps: split thành 2 waves, không stuff cluster

## Decision tree

```
Có ≥3 OPEN gaps đang trên ROADMAP queue?
├─ NO  → /wave-pack-planner không phù hợp; pick 1 gap + /gap-to-pr-converter
└─ YES → tiếp tục
   │
   Các gaps có cùng theme (Observability / DR / Admin / Business / ...)?
   ├─ NO  → defer; chờ backlog tích đủ theme
   └─ YES → tiếp tục
      │
      File-overlap matrix: ≥1 HARD conflict (migration version, single service file)?
      ├─ YES → re-bucket (defer 1-2 gaps OR ship foundation defuse PR trước)
      └─ NO  → CLUSTER ELIGIBLE → tiếp tục Step 2
```

## Proven cluster themes

Per ROADMAP `§Active wave queue (clustered)` snapshot 2026-04-28:

| Theme | Gaps | Status |
|-------|------|--------|
| Observability — Wave 1 | GAP-121 + GAP-143 + GAP-144 | ✅ SHIPPED 2026-04-28 (this is the 1 data point) |
| Observability — Wave 2 | GAP-122 + GAP-144 mock-fire backfill | 🔵 NEXT |
| DR/Backup | GAP-117 + GAP-118 + GAP-119 | 🔵 OPEN |
| KiteHub admin | GAP-066 + GAP-067 + GAP-068 | 🔵 OPEN |
| Business correctness | GAP-049 + GAP-050 + GAP-150 | 🔵 OPEN |
| Parent/import | GAP-052 + GAP-063 + GAP-137 + GAP-139 | 🔵 OPEN |
| K-12 features | GAP-055 + GAP-056 + GAP-057 | 🟡 IN_PROGRESS |

**Note:** ROADMAP queue là source of truth. Khi đọc skill này, refresh từ `documents/04-quality/gaps/ROADMAP.md` — các cluster có thể đã shift.

## Anti-cluster patterns

| Anti-pattern | Symptom | Mitigation |
|--------------|---------|-----------|
| **Forced cluster** | Gaps đẩy chung chỉ vì "P0 cùng tuần" | Defer; chờ thực sự cùng theme |
| **Oversized** (>7 gaps) | Buckets >2 gaps/agent, agent prompts dài >300 LOC | Split 2 waves; cap 5 agents/wave |
| **Shared-state cluster** | 2+ gaps muốn migration version V_n cùng lúc | Pre-assign V_n / V_n+1 / V_n+2 trong wave plan, OR defer |
| **`application.yml` cluster** | 3 gaps cùng đụng spring config keys | Defer 2; lead-owns shared file per `feedback_parallel_agent_strategy.md` rule #2 |
| **Hidden chain** | GAP-A blocks GAP-B blocks GAP-C | Không cluster; serial OK với chain explicit |
| **Cross-service migration** | E.g. GAP-114 logging touches all services | Separate track; multi-PR per service, không 1 wave |
| **Audit-driven cleanup batch** | 5+ "remove dead code" gaps | OK to cluster nếu files truly disjoint; dùng `p3-cleanup-agent.md` template |

## Cluster eligibility checklist

Trước khi commit cluster, verify TẤT CẢ:

- [ ] ≥3 gaps cùng theme, ROADMAP queue confirms
- [ ] File-overlap matrix run, ≤1 SOFT conflict, 0 HARD
- [ ] Foundation PR (interfaces/shared lib) đã merge — hoặc không cần
- [ ] Agent buckets defined: mỗi agent có disjoint scope (≤2 gaps/agent)
- [ ] Migration version slots pre-assigned nếu ≥2 gaps add migrations
- [ ] Wave plan ready để PR (per [feedback_wave_plan_through_pr.md](../../../../projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/feedback_wave_plan_through_pr.md))
- [ ] Wall-clock target documented (60-120 min cho 3-5 agents)

Nếu BẤT KỲ unchecked → pause cluster, fix gap, re-evaluate.

## Sample size disclaimer (HONEST)

Methodology hiện tại có **1 data point** — Wave Observability 2026-04-28 (75 min wall-clock, 3 gaps, ~5x speedup vs serial estimate).

**Treat as "demonstrated, not proven":**
- 5x speedup là measure-once, chưa qua repeat trials
- Cluster theme heuristic dựa trên 1 successful cluster (Observability) — chưa biết "shared-file SOFT conflict auto-merges" đúng đến đâu cross theme
- Agent prompt drift, worktree contamination, wall-clock noise — đều là known unknowns

**Framework should evolve based on `data/wave-history.jsonl`:**
- Mỗi wave append entry với `{wave, date, gaps, agents, wall_clock_min, lessons[]}`
- Sau 5 wave entries → recalibrate decision tree (e.g. "themes A/B work well, theme C needs different bucketing")
- Sau 10 wave entries → consider promoting tuned heuristics vào rule (`.claude/rules/`)

Cross-link: [retrospective-checklist.md](retrospective-checklist.md) capture lessons; [agent-spawning-template.md](agent-spawning-template.md) calibrate prompt quality; [file-overlap-algorithm.md](file-overlap-algorithm.md) tune classification.

## Related

- [SKILL.md](../SKILL.md) — entry point
- [file-overlap-algorithm.md](file-overlap-algorithm.md) — Step 2 mechanics
- [wave-plan-template.md](wave-plan-template.md) — Step 4 template
- Memory `feedback_wave_pack_cross_gap_clustering.md` — motivation + 5x evidence
- Memory `feedback_parallel_agent_strategy.md` — within-cluster mechanics (9 rules)
