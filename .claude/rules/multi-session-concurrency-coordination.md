---
paths:
  - ".claude/session-locks/**"
  - "documents/04-quality/gaps/**"
  - "kiteclass/kiteclass-core/src/main/resources/db/migration/**"
  - "kitehub/kitehub-subscription/src/main/resources/db/migration/**"
  - ".claude/rules/multi-session-concurrency-coordination.md"
---

# Multi-Session Concurrency Coordination — reserve shared monotonic resources, branch riêng

**Priority:** 🟠 MANDATORY — concurrent-session governance preventing shared-resource collision
**Version:** 1.0.0
**Created:** 2026-06-10
**Last-Reviewed:** 2026-06-10
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + memory auto-load + worked self-test trên 2026-06-10 incident — 2 session độc lập cùng branch → GAP-1111 collision + V96 collision + gap-status.csv 3-way conflict) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "≥2 independent coordinator sessions tranh shared monotonic resources". Sister to `feedback_parallel_agent_strategy` (parallel AGENTS trong 1 session, 1 coordinator sở hữu ID-space) tại boundary khác — multi-SESSION không share state in-memory; nâng session-lock từ HINT → authoritative resource-reservation. META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi session khi có ≥2 session-lock active (của session khác) — tại session-start lock-check, tại allocate gap-ID / migration-version, tại chọn branch. Out-of-scope: single-session (1 lock active = chỉ mình); parallel AGENTS spawned trong cùng 1 coordinator session (covered by `feedback_parallel_agent_strategy` — 1 coordinator sở hữu ID-space).

---

## 1. The Rule

> **Khi có lock của session khác đang active, mỗi session PHẢI: (a) làm trên branch RIÊNG off `main` qua worktree (KHÔNG commit lên branch session khác đã claim); (b) reserve block gap-ID + migration-version trong lock file của mình + check lock session khác TRƯỚC khi allocate; (c) coi `gap-status.csv` conflict là expected, resolve ADDITIVE (giữ mọi row).**

Hai+ session độc lập = hai+ coordinator KHÔNG chia sẻ state in-memory. Mỗi session tính `max(gap-id)+1` / `max(V-version)+1` độc lập trên cùng base → ra CÙNG số → collision khi merge. `gap-status.csv` 2+ nơi append EOF → conflict. Session-lock hiện tại chỉ là HINT (`start-session` SKILL ghi rõ "advisory hint", `session-locks/README.md` "advisory hint to detect concurrent-session conflicts") → KHÔNG govern resource allocation.

Rule này nâng session-lock từ hint → **authoritative resource-reservation**: lock file của session khác trở thành source-of-truth cho biết gap-ID/migration-version nào ĐÃ bị reserve, để session tiếp theo skip block đó.

Sister mechanisms cover boundary khác:
- `feedback_parallel_agent_strategy` — parallel AGENTS trong 1 session: 1 coordinator sở hữu ID-space, agents nhận pre-allocated ID (intra-session). Rule này = inter-SESSION (mỗi session 1 coordinator riêng).
- `start-session` SKILL §Step 3 — tạo lock (hint). Rule này = govern resource khi lock conflict.
- `gap-architecture-v2.md` — gap-status.csv canonical. Rule này = additive-resolve discipline khi multi-session append.

Force-multiplier: 1 chuẩn reserve-block → mọi multi-session subsequent allocate disjoint ID-space → eliminate collision class permanently.

---

## 2. Trigger pattern — khi nào rule fires

Rule fires khi ALL: scope = multi-session (≥2 lock active) AND đang chạm shared monotonic/append resource.

| Tình huống | Fire? |
|---|---|
| **Session-start lock-check** thấy ≥2 lock active (≥1 của session khác) | ✅ YES — đọc reserved-block của session khác trước khi làm gì |
| **Allocate gap-ID mới** (`max(gap-id)+1`) khi lock khác active | ✅ YES — skip block đã reserved, reserve block của mình |
| **Allocate migration-version** (`max(V)+1` Flyway) khi lock khác active | ✅ YES — skip reserved V-range, reserve range của mình |
| **Chọn branch để commit** khi lock khác claim branch đó | ✅ YES — branch riêng off `main` qua worktree |
| **Append `gap-status.csv`** khi lock khác active | ✅ YES — additive-resolve, giữ mọi row |
| **Single-session** (≤1 lock, chỉ mình) | ❌ NO — không có contention; allocate `max+1` bình thường |
| **Parallel agents spawned bởi mình** (1 coordinator) | ❌ NO — `feedback_parallel_agent_strategy` covers; coordinator pre-allocates |
| Đọc file thuần (không allocate/append shared resource) | ❌ NO |

Rule **KHÔNG** fires khi: chỉ 1 lock active (mình), HOẶC allocation thuộc ID-space mình đã sở hữu (intra-session agents), HOẶC thao tác không chạm gap-ID / migration-version / gap-status.csv / branch ownership.

---

## 3. Required artifacts khi rule fires

### 3.1 Lock-file schema extension (authoritative resource-reservation)

Lock file hiện là **YAML** (`.claude/session-locks/session-{YYYYMMDD-HHMMSS}-{hostname}.lock`, per `session-locks/README.md` + `start-session/reference/context-template.md` §2 — KHÔNG phải JSON; honor format thực tế). Extend với 3 field reservation:

```yaml
# --- existing fields ---
session_id: 20260610-013500-wsl-victor
started: 2026-06-10T01:35:00+07:00
branch: feature/wave-X-session-A
worktree: /home/user/projects/repo/.claude/worktrees/agent-aaaa
gaps: [GAP-1111, GAP-1112]
last_heartbeat: 2026-06-10T01:50:00+07:00
# --- NEW: resource reservation (rule này) ---
reserved_gap_ids: "GAP-1111..GAP-1120"          # block (10-wide) session này sở hữu
reserved_migration_versions: "V96..V100"        # block Flyway version session này sở hữu
owned_branches:                                  # branch session này claim (không session khác commit lên)
  - feature/wave-X-session-A
```

**Quy ước block:**
- `reserved_gap_ids` — block ≥10-wide (giảm tần suất re-reserve); format `GAP-NNNN..GAP-MMMM` (inclusive range).
- `reserved_migration_versions` — block ≥5-wide per service; format `V96..V100`. Nếu multi-service, dùng map `{kiteclass-core: "V96..V100", kitehub-subscription: "V40..V44"}`.
- `owned_branches` — list branch session claim; session khác KHÔNG commit lên.

### 3.2 Allocate procedure (đọc → skip → reserve → ghi)

```
1. ls .claude/session-locks/*.lock  → đọc MỌI lock active (purge stale >4h trước)
2. Gom union(reserved_gap_ids) + union(reserved_migration_versions) + union(owned_branches)
   của TẤT CẢ session khác
3. base_max = max(gap-id trong gap-status.csv, gap files)         # base trên main
   next_free_block = block đầu tiên SAU base_max KHÔNG overlap union(reserved) session khác
4. Reserve block của mình → ghi reserved_gap_ids / reserved_migration_versions
   vào lock file của MÌNH (Edit/Write lock file)
5. Allocate gap-ID / V-version TỪ block của mình (không phải max+1 toàn cục)
```

### 3.3 gap-status.csv additive-resolve

Khi merge/rebase 2 session cùng append `gap-status.csv`:
- Conflict tại EOF là **EXPECTED** (2+ append cùng vùng) — không phải lỗi.
- Resolve **ADDITIVE**: giữ MỌI row của cả 2 session (không drop bên nào), sort theo gap-ID nếu cần.
- Verify post-resolve: `bash scripts/check-gap-status-csv.sh` (nếu có) + count rows ≥ max(2 nhánh).

### 3.4 Cross-reference trong PR body

PR body mỗi session ghi section:

```markdown
## Multi-session coordination (per multi-session-concurrency-coordination.md §3)

- Active locks observed: <list session_id khác + reserved blocks của họ>
- My reserved block: gap-ID `GAP-NNNN..GAP-MMMM`, migration `V96..V100`
- My owned branch: <branch>
- gap-status.csv resolve: ADDITIVE (kept all rows từ N sessions)
```

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Tính `max(gap-id)+1` độc lập khi lock khác active | Đọc reserved-block session khác → allocate từ block disjoint của mình |
| Lấy `max(V)+1` Flyway mà không check reserved_migration_versions | Skip reserved V-range session khác → reserve range riêng |
| Commit lên branch session khác đã claim (`owned_branches`) | Branch RIÊNG off `main` qua worktree |
| Coi gap-status.csv conflict là lỗi → drop 1 bên | Additive-resolve: giữ mọi row cả 2 session |
| Tin session-lock là enforcement (nó là hint) | Nâng thành authoritative: ghi + đọc reserved-block |
| Reserve block 1-wide (GAP-1111 đơn lẻ) | Reserve block ≥10-wide để giảm re-reserve + race |
| Bỏ qua stale lock leftover (01:35 leftover) mà không purge | Purge lock >4h TRƯỚC khi đọc union(reserved) |
| Allocate xong mới ghi lock | Ghi reserved-block TRƯỚC khi allocate (claim-then-use) |

---

## 5. Override mechanism

Genuine exception (chỉ 1 session thật nhưng 2 lock do crash leftover; HOẶC user explicit chấp nhận đơn-luồng dù 2 lock):

```
git commit -m "...
MULTI_SESSION_OVERRIDE: <reason — e.g. 'lock 01:35 là crash leftover đã verify (no live process), purged; single live session'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely stale-lock purge automation cần fix, HOẶC multi-session pattern phổ biến hơn dự kiến → nâng enforcement).

---

## 6. Worked self-test — 2026-06-10 originating incident

**Scenario:** Lần đầu chạy 2 session độc lập cùng lúc trên cùng branch `feature/tier-ui-fix-g2-browser-2026-06-09`. Mỗi session 1 coordinator riêng, KHÔNG share state in-memory. Base trên main: `max(gap-id)` ≈ GAP-1089/1110-range, `max(V)` = V95 (kiteclass-core).

**Va chạm THẬT (without rule):**

| Resource | Session A | Session B | Collision |
|---|---|---|---|
| Gap-ID | `max+1` = GAP-1111 | `max+1` = GAP-1111 | ✅ CÙNG GAP-1111 |
| Migration | `max+1` = V96 | `max+1` = V96 | ✅ CÙNG V96 |
| gap-status.csv | append EOF | append EOF | ✅ merge conflict |
| Branch | commit lên branch chung | commit lên branch chung | ✅ ownership unclear |

Tự-tham-chiếu: author chính rule này spawn agent worktree thứ 3 → 3-way collision GAP-1111/1112/1113/1114 trong gap-status.csv. Rule này tự nó là ví dụ sống.

**Apply rule retroactively (counterfactual):**

1. Session-start lock-check (§2): Session B thấy lock Session A active → fire rule.
2. Session A reserve `GAP-1111..GAP-1120` + `V96..V100` + `owned_branches: [feature/...A]` vào lock A (§3.1).
3. Session B đọc lock A (§3.2 step 2) → union(reserved) = {GAP-1111..1120, V96..100} → skip → reserve `GAP-1121..GAP-1130` + `V101..V105` + branch riêng off `main`.
4. Agent thứ 3 đọc lock A + lock B → reserve `GAP-1131..GAP-1140` + `V106..V110`.
5. gap-status.csv: mỗi session append row trong block riêng → conflict additive-resolve (§3.3) giữ mọi row, 0 ID đụng.

| Metric | Without rule | With rule |
|---|---|---|
| Gap-ID collision | 3-way (1111×3) | 0 (disjoint blocks) |
| Migration collision | 3-way (V96×3) | 0 (disjoint V-ranges) |
| gap-status.csv resolve | manual conflict + risk drop row | additive, deterministic |
| Branch ownership | ambiguous | explicit `owned_branches` |
| Re-reserve overhead | n/a | ~1 lock Edit/session (block ≥10-wide) |

**Verdict:** Rule fires đúng trên chính incident sinh ra nó (kể cả 3rd-agent self-reference). Counterfactual: 0 collision. Self-test PASS ✅.

---

## 7. Path-scope justification (per `context-budget-mandate.md` §3.2)

Rule này dùng `paths:` frontmatter (path-scope), KHÔNG always-load. Lý do chọn path-scope thay vì always-load:

- **Multi-session là HIẾM** — incident 2026-06-10 là lần ĐẦU TIÊN. 99% session là single-session → always-load ~3k token × mọi session là waste cho case không fire. `context-budget-mandate.md` §3.2 ưu tiên path-scope khi fire-moment có natural file-scope.
- **Fire-moment CÓ natural file-scope** — rule chỉ cần fire khi chạm shared resource: lock-check (`.claude/session-locks/**`), gap-ID allocate (`documents/04-quality/gaps/**`), migration-version allocate (`**/db/migration/**` của 2 service). Đúng 3 path glob → path-scope khớp chính xác fire-moment.
- **Session-start nudge phủ bởi memory** — concern "rule cần present tại session-start để prompt lock-check" được giải quyết bởi `feedback_multi_session_concurrency` memory (always-load mỗi session, lightweight ~0.4k token nudge). Phân công sạch: **memory = nudge nhẹ luôn-bật tại session-start; rule = chi tiết đầy đủ load khi chạm shared-resource path**. Đây là pattern đã established (vd `discovery-to-gap-inline-filing` memory + rule pair).
- **Budget check pass** — `scripts/check-context-budget.sh` skip path-scoped rules khỏi per-rule gate (head -3 thấy `^paths:`); rule này KHÔNG cộng vào always-load total.

### 7.1 Atomic-unique-bar check (per `rule-change-process.md` §5.1)

- ✅ **Atomic:** single concept = reserve disjoint shared-resource blocks khi ≥2 session.
- ✅ **Unique:** session-lock = MECHANISM (hint) không phải rule governing allocation; `feedback_parallel_agent_strategy` = intra-session (1 coordinator); `gap-architecture-v2.md` = gap-ID format không cover multi-session contention. Rule này = inter-session resource-reservation, chưa rule nào cover.
- ✅ **Widely applicable:** mọi multi-session run subsequent (rare nhưng catastrophic khi xảy ra).
- ✅ **Body discipline §1:** The Rule có 2 "và"-equivalent ("+") trong (b) — ≤2 conjunction OK.

Re-evaluate nếu: (a) multi-session thành thường-xuyên (>10%/quarter) → cân nhắc always-load + detector, (b) Anthropic publish distributed-session coordination primitive, (c) rule >300 dòng.

---

## 8. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 8.1 Reviewer-checklist (active now)

Pre-merge review cho PR sinh ra từ session khi có ≥2 lock active:

- [ ] PR body có section `## Multi-session coordination` (per §3.4)?
- [ ] Gap-ID PR này thuộc reserved block của session (disjoint với session khác)?
- [ ] Migration-version (nếu có) thuộc reserved V-range disjoint?
- [ ] Branch off `main` riêng (không commit lên `owned_branches` session khác)?
- [ ] gap-status.csv resolve additive (row count ≥ max nhánh, không drop)?

### 8.2 Self-detection (in-turn)

Tại session-start VÀ trước khi allocate gap-ID / migration-version, Claude mentally check:
- `ls .claude/session-locks/` → có ≥2 lock (≥1 của session khác) không?
- Nếu CÓ → STOP `max+1` reflex → đọc reserved-block session khác → reserve block disjoint của mình TRƯỚC khi allocate.
- Nếu KHÔNG (single-session) → allocate bình thường.

### 8.3 Memory auto-load (paired same-PR)

Memory entry `feedback_multi_session_concurrency.md` loads mỗi session (always-on nudge), reminds 5-bullet checklist: (1) ls locks tại start; (2) ≥2 lock → reserve block; (3) gap-ID/V từ block riêng không max+1; (4) branch riêng off main; (5) gap-status.csv additive-resolve.

### 8.4 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** Auto-reserve wiring vào `collect-state.sh` (executable script) — khi `/start-session` thấy ≥2 lock, tự gom union(reserved) + suggest next-free block + ghi reserved-block vào lock mới. Đây là automation NON-TRIVIAL: sửa executable script đang chạy + parse YAML lock (`yq`) + compute next-free-block + write-back. Không phải grep/file-exist check <50 LOC.
- **Recurrence count:** 1 (2026-06-10, incident đầu tiên multi-session).
- **FP risk:** Trung bình — stale-lock leftover (vd 01:35 leftover) có thể bị tính nhầm là active session nếu purge logic chưa robust.
- **Decision:** Reviewer-checklist §8.1 + memory auto-load §8.3 + worked self-test §6 ĐỦ cho v1.0.0. Defer detector vì: (a) recurrence chỉ 1; (b) automation sửa `collect-state.sh` cần test cẩn thận (race trên write-back lock); (c) cost-of-next-miss (1 batch ID re-number) < cost-build-detector (robust YAML reserve + stale-purge hardening). Revisit khi recurrence ≥2 OR multi-session thành pattern thường-xuyên. Follow-up: nếu recurrence #2 → file gap wiring `collect-state.sh` auto-reserve + stale-purge hardening.

### 8.5 Override mechanism

Per §5 trailer `MULTI_SESSION_OVERRIDE:` — logged quarterly retro. Pattern frequency >10%/quarter → meta-review.

---

## 9. Relationship to other rules

- **`start-session` SKILL.md** §Step 3 + `session-locks/README.md` — session-lock = MECHANISM (hint); rule này nâng thành authoritative resource-reservation. Lock file YAML schema extension §3.1.
- **`feedback_parallel_agent_strategy`** (memory) — sister tại boundary intra-session (parallel AGENTS, 1 coordinator sở hữu ID-space). Rule này = inter-SESSION (mỗi session 1 coordinator). `GAP-829 ID collision` lesson (parallel agents) được multi-session khuếch đại.
- **`gap-architecture-v2.md`** — gap-ID format + gap-status.csv canonical; rule này thêm additive-resolve discipline + reserved-block allocation khi multi-session.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn reserve-block → mọi multi-session subsequent allocate disjoint).
- **`incident-to-rule-pipeline.md`** — rule này = direct output 2026-06-10 multi-session collision incident qua 5-stage.
- **`rule-change-process.md`** §6.5 Enforcement Parity — rule + reviewer-checklist §8.1 + memory paired §8.3 + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row all same PR.
- **`context-budget-mandate.md`** §3.2 — path-scope justified §7.
- **`discovery-to-gap-inline-filing.md`** — incident này surface during multi-session work; gap GAP-1114 filed inline per đó.
- **`feedback_multi_session_concurrency.md`** (memory, paired same-PR).

---

## 10. Log

- **2026-06-10 (v1.0.0):** Rule created in response to user-flagged miss 2026-06-10: lần đầu chạy 2 session độc lập cùng lúc trên cùng branch `feature/tier-ui-fix-g2-browser-2026-06-09` → va chạm THẬT ở shared monotonic/append resources (GAP-1111 collision do cả 2 tính `max(gap-id)+1` độc lập; V96 migration collision; gap-status.csv merge conflict do 2+ append EOF; branch ownership ambiguous do session-lock chỉ là hint). Tự-tham-chiếu: author chính rule này spawn agent worktree thứ 3 → 3-way collision GAP-1111/1112/1113/1114. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged multi-session collision) → Classify ✓ (session-lock = HINT mechanism không govern resource; `feedback_parallel_agent_strategy` = intra-session 1 coordinator KHÁC multi-session; chưa rule nào cover ≥2 independent coordinator tranh shared monotonic resources) → Rule+Enforce ✓ (this file + reviewer-checklist §8.1 + memory `feedback_multi_session_concurrency.md` paired + worked self-test §6 on originating incident + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 — rule fires đúng trên chính incident incl. 3rd-agent self-reference; counterfactual 0 collision via disjoint reserved-block) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered inter-session resource-reservation class; no constraint loosening; existing single-session work grandfathered; rule applies prospectively từ this PR forward 2026-06-10). Atomic-unique-bar §7.1 passed. Detector (§8.4 `collect-state.sh` auto-reserve wiring) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions (non-trivial executable-script automation + recurrence 1 + cost-of-next-miss < detector-build-cost); reviewer-checklist + memory + worked self-test sufficient cho v1.0.0; revisit khi recurrence ≥2.
