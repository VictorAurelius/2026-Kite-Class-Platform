# GAP-1114: Thiếu meta-rule điều phối ≥2 session độc lập tranh shared monotonic resources

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Meta
**Found:** 2026-06-10 (multi-session collision incident — lần đầu chạy 2 coordinator session song song)
**Affects:** `.claude/session-locks/` (lock = HINT) + `documents/04-quality/gaps/gap-status.csv` (EOF append) + gap-ID allocation (`max+1`) + Flyway migration-version allocation (`max(V)+1`) + branch ownership

## Problem

2026-06-10: lần đầu tiên chạy 2 session độc lập cùng lúc trên cùng branch `feature/tier-ui-fix-g2-browser-2026-06-09`. Mỗi session là 1 coordinator riêng, KHÔNG chia sẻ state in-memory. Va chạm THẬT xảy ra ở mọi shared monotonic/append resource:

1. **Gap ID collision:** cả 2 session đều tính `max(gap-id)+1` độc lập → cùng ra GAP-1111. Memory đã có lesson `GAP-829 ID collision` cho parallel agents — multi-session khuếch đại class này (2 coordinator không share ID-space, không chỉ 2 agent trong 1 coordinator).
2. **Migration version collision:** cả 2 đều lấy `V96` kế tiếp (kiteclass-core đang ở V95).
3. **`gap-status.csv` merge conflict:** 2+ nơi append EOF cùng vùng → conflict khi merge/rebase.
4. **Branch ownership mơ hồ:** 2 session cùng 1 branch; session-lock chỉ là HINT (`start-session` SKILL §Step 3 + `session-locks/README.md` ghi rõ "advisory hint to detect concurrent-session conflicts"), lock leftover 01:35 không được dọn, không ngăn được gì.

Bằng chứng tự-tham-chiếu: việc author CHÍNH rule điều phối này spawn agent worktree thứ 3 → 3-way collision GAP-1111/1112/1113/1114 trong gap-status.csv. Incident tự nó là ví dụ sống của coverage gap.

## Root Cause

Cơ chế đã có vs thiếu (Stage 2 Classify per `incident-to-rule-pipeline.md`):

- **Có:** `start-session` skill + `.claude/session-locks/` (lock = HINT, không govern resource allocation). `feedback_parallel_agent_strategy` (parallel AGENTS trong 1 session — 1 coordinator sở hữu ID-space, KHÁC multi-session).
- **Thiếu:** rule govern 2 SESSION độc lập tranh shared monotonic resources. Session-lock chưa bao giờ được nâng từ hint → authoritative resource-reservation.

## Proposed Fix

Tạo meta-rule `.claude/rules/multi-session-concurrency-coordination.md` v1.0.0 mandate: khi có lock session khác active → mỗi session (a) branch riêng off `main` qua worktree, (b) reserve block gap-ID + migration-version trong lock file của mình + check lock session khác TRƯỚC khi allocate, (c) coi `gap-status.csv` conflict là expected + resolve ADDITIVE. Nâng session-lock từ hint → authoritative resource-reservation (extend lock YAML schema với `reserved_gap_ids` + `reserved_migration_versions` + `owned_branches`).

## Acceptance Criteria

- [x] Rule `.claude/rules/multi-session-concurrency-coordination.md` v1.0.0 created (Priority MANDATORY, path-scoped)
- [x] §1 The Rule atomic (branch riêng + reserve block + additive-resolve)
- [x] §3.1 lock-file YAML schema extension (`reserved_gap_ids`/`reserved_migration_versions`/`owned_branches`) + §3.2 allocate procedure (đọc → skip → reserve → ghi)
- [x] §6 worked self-test áp rule retroactively vào incident 2026-06-10 (2 session → GAP-1111 + V96 + CSV 3-way collision) — counterfactual 0 collision, PASS
- [x] Enforcement parity: rules-index.csv row + output-review-mandate.md §3 matrix row + reviewer-checklist §8.1 + memory `feedback_multi_session_concurrency.md`
- [x] Detector HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (collect-state.sh auto-reserve wiring = non-trivial executable-script automation + recurrence 1)

## Related

- Discovered in: multi-session collision 2026-06-10 (branch `feature/tier-ui-fix-g2-browser-2026-06-09`)
- Rule: `.claude/rules/multi-session-concurrency-coordination.md` v1.0.0 (closes this gap)
- Sister memory: `feedback_parallel_agent_strategy` (intra-session boundary), `GAP-829 ID collision` lesson (parallel agents — multi-session khuếch đại)
- Pipeline: `incident-to-rule-pipeline.md` 5-stage; META P1 force-multiplier per `meta-gap-priority.md` §3
