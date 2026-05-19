# Session-End Context Check — verify % budget before proposing end

**Priority:** 🟠 MANDATORY — session lifecycle discipline
**Version:** 1.1.0
**Created:** 2026-05-19
**Last-Reviewed:** 2026-05-19
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.0.1 PATCH self-approve per `rule-change-process.md` §5; same-session clarification — script đọc stdin JSON với `transcript_path` field, KHÔNG run standalone; §4 invocation recipe expanded với proper stdin construct + model auto-detect (200k vs 1M); threshold % normalize valid cho mọi total_tokens. v1.0.0 (kept): MINOR self-approve per §5; new rule với built-in enforcement (memory auto-load + self-detection checklist + worked self-test) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-implicit "don't propose end prematurely" guard; existing sessions grandfathered, rule applies prospectively)
**Applies to:** Mọi turn mà Claude có ý định propose end-session / suggest `/clear` / hint "hết session" / "session sau" / "wrap up" / "context degraded" — proactive proposal OR reactive proposal sau user nudge. Out-of-scope: user explicit ask `/clear` (do ngay, không check).

---

## 1. The Rule

> **Trước khi Claude propose end-session HOẶC suggest `/clear` HOẶC hint "session sau" / "hết context", PHẢI run `bash .claude/statusline-kite.sh` để check actual context %, sau đó decide theo §3 threshold table.**

Đề xuất end-session khi context vẫn rộng rãi (vd <50%) = waste user time + force cache miss + lose conversation continuity. Đề xuất end khi context thật sự high (>70%) = honor `context-budget-mandate.md` cache TTL + prep user cho fresh session.

Force-multiplier: KHÔNG check = guess; check = decision evidence-based. Cost ~50 tokens (1 Bash invocation) save vs cost N×1000 tokens cache miss khi user nghe lời clear sớm.

---

## 2. When this rule fires

Rule fires khi Claude turn sắp output text chứa phrasing pattern:

| Pattern | Example |
|---|---|
| Direct end propose | "End session", "Hết session", "Wrap up", "Session done" |
| Clear suggestion | "Recommend `/clear`", "Nên `/clear`", "Suggest clearing context" |
| Future-session deferral | "Session sau", "Next session", "Khi quay lại", "Pick up next time" |
| Context-degraded hint | "Context degraded", "Context full", "Token budget low", "Context heavy" |
| Compact suggestion | "Recommend `/compact`", "Nên compact" |
| Cache-miss warning | "Context approaching limit", "Approaching context max" |

Rule **KHÔNG** fire khi:
- User explicit ask: "Clear", "Hết session", "Wrap up đi" → execute directly (user authorization override)
- Mid-task pause unrelated to context: "Pause để bạn review", "Tôi chờ confirm" — không phải end-session
- Reporting CI/agent status: "Agent done", "Build done" — không phải session lifecycle

---

## 3. Threshold decision table

Sau khi run `bash .claude/statusline-kite.sh`, output format: `[icon] [progress-bar] X% N/200.0k $cost`. Extract `X%` value.

| Context % | Action |
|---|---|
| **< 50%** (< 100k/200k) | ❌ **KHÔNG propose end** — context rộng rãi, tiếp tục làm việc. Phản hồi user-asked context-related question với current % + "vẫn ổn, tiếp tục được". |
| **50-69%** (100-138k/200k) | 🟡 **Soft mention OK** — có thể note "context ~X%, vẫn còn room" nhưng KHÔNG propose end. Reserve cho near-threshold heads-up only. |
| **70-84%** (140-168k/200k) | 🟠 **Heads-up + ask** — flag user "context ~X% nearing limit, có muốn `/clear` sau task hiện tại không?" — đợi user decide, KHÔNG tự-execute. |
| **≥ 85%** (≥ 170k/200k) | 🔴 **Strong recommend** — đề xuất `/clear` sau task hiện tại finish + handoff note. User confirm trước khi clear. |
| **≥ 95%** (≥ 190k/200k) | ⚠️ **Force handoff** — task hiện tại MUST close ngay (no new work), write handoff note, recommend `/clear` immediate. |

Threshold dựa trên Anthropic prompt cache 5-min TTL + `context-budget-mandate.md` <120k baseline để bảo toàn cache window. 70% = 140k = cache-warm threshold breach signal.

---

## 4. Required action sequence

Khi rule fires (Claude detect mình sắp output text trong §2 pattern):

0. **Docs-sync verification (BẮT BUỘC v1.1.0)** — Verify 5 sync targets per §4.5 trước khi propose end. Nếu BẤT KỲ target stale → fix BEFORE propose end (bundle vào sync PR docs-only auto-merge). Skip step này = next session pickup miss state (gap drift, ROADMAP wrong status, wave-history lost, memory entries orphan, handoff missing).
1. **STOP text output composition.**
2. **Run statusline với proper stdin** (script đọc JSON stdin từ harness, KHÔNG standalone):

   ```bash
   TRANSCRIPT=$(ls -t ~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/*.jsonl 2>/dev/null | head -1)
   echo "{\"model\":{\"display_name\":\"$MODEL_NAME\",\"id\":\"$MODEL_ID\"},\"transcript_path\":\"$TRANSCRIPT\",\"cost\":{\"total_cost_usd\":0}}" \
     | bash .claude/statusline-kite.sh
   ```

   Model ID auto-detect: script parses `[1m]` / `1M` / `1m` substring → total=1,000,000; else total=200,000. Threshold % (§3) normalize valid cho cả 200k và 1M (% same meaning).
3. **Read X%** từ output format `[model] [bar] X% used/total $cost` (extract `\d+%` between bar và used/total).
4. **Apply §3 threshold table** → decide action class.
5. **Output text theo action class** — INCLUDE current % value cho transparency. Vd "Context hiện 44% — vẫn còn room, KHÔNG cần end session" HOẶC "Context 78% — nearing limit, đề xuất `/clear` sau task này".

Nếu script fail (exit non-0 / no output / transcript path missing): fall back to user manual ask "Bạn check status line % giúp tôi (top status bar trong Claude Code UI)? Tôi sẽ decide theo".

## 4.5 Docs-sync verification 5-target checklist (added v1.1.0)

Extends `post-merge-sync-completeness.md` §2 4-target framework với 5th target (session-handoff) — applied at session-end decision moment cụ thể, không chỉ per-PR moment.

| # | Target | Cách verify |
|---|---|---|
| 1 | `documents/04-quality/gaps/gap-status.csv` | Mọi gap status flip session này reflected — `git log --since="<session-start>" --diff-filter=M -- documents/04-quality/gaps/gap-status.csv` matches actual gap file states |
| 2 | `documents/04-quality/gaps/ROADMAP.md §🎯 Current Status Snapshot` | Wave / PR / gap shipped session này có entry trong ROADMAP — `grep "Wave NN\|GAP-XXX\|PR #NNNN"` ROADMAP returns recent shipping |
| 3 | `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` | Wave completions / wave plan ships có append entry — `tail -3 wave-history.jsonl` shows current wave |
| 4 | `~/.claude/projects/.../memory/MEMORY.md` index | New memory entries created session này có pointer trong MEMORY.md — `tail -5 MEMORY.md` shows recent additions |
| 5 | `documents/03-planning/session-handoffs/YYYY-MM-DD-*.md` (added v1.1.0) | Session handoff note exists cho session date với scope shipped + pickup state cho next session — `ls -t documents/03-planning/session-handoffs/ \| head -1` = today's date |

**Decision flow:**

```
1. Run 5-target check (BẮT BUỘC v1.1.0)
2. If ANY stale → fix BEFORE propose end:
   - Bundle sync into docs-only PR (per docs-only-pr-auto-merge.md auto-merge eligible)
   - Apply 1 PR for all 5 targets (atomic sync)
3. After sync clean → run §4 Step 1-5 context check sequence
4. Then propose end với both: clean docs sync + verified context %
```

**Banned shortcuts:**
- ❌ Propose `/clear` khi any sync target stale "I'll fix next session" — context flush = lose track
- ❌ Sync partial (3/5 targets) — atomic 5/5 required for clean handoff
- ❌ Skip session-handoff note "vì conversation context đủ" — Claude session context không persist; handoff doc là canonical pickup source

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Propose end-session bằng feel ("chắc context cao") | Run statusline script, get evidence % |
| Suggest `/clear` chỉ vì conversation dài turn count | Turn count ≠ context %. Check actual % via script |
| Auto-execute `/clear` không hỏi user | User confirm trước, ngay cả khi 95%+ |
| Skip check "vì task nhỏ" | Rule applies to EVERY end-session proposal regardless of task size |
| Include % from previous turn ("earlier was X%") | Re-check fresh — context grows mỗi turn |
| Round 95% xuống 85% "để safe" | Report exact %; transparent với user |
| Propose end khi user vừa start task mới | Task hiện tại finish trước, sau đó re-evaluate |

---

## 6. Worked self-test — retroactive 2026-05-19 session

**Scenario:** Session hôm nay (2026-05-19) — multiple agents spawn + gap triage + 4-Q decisions + rule additions. Suppose tại turn cuối tôi định propose "kết thúc session này".

**Apply §4 sequence:**

1. STOP text output composition ✅
2. Run `bash .claude/statusline-kite.sh` — output: `[icon] [bar] X% N/200.0k $cost`
3. Read X% — verified at rule-creation time, context still under threshold per §3
4. Apply table:
   - Nếu X < 50% → KHÔNG propose end; phản hồi "context still ok, tiếp tục được"
   - Nếu X 70-84% → heads-up + ask user
   - Nếu X ≥ 85% → strong recommend `/clear` post-task
5. Output text bao gồm exact % value

**Counterfactual without rule:** Claude propose end based on subjective "feel" (turn count, work volume) — có thể wrong direction (propose end khi context vẫn 30% = waste; HOẶC không propose khi context 90% = next turn context overflow + cache miss).

**Verdict:** rule fires correctly on session-end proposal moments. Evidence-based vs subjective decision. ✅

---

## 7. Auto-load justification (per `context-budget-mandate.md` §3.2)

Rule này KHÔNG dùng `paths:` frontmatter — luôn auto-load mỗi session. Lý do:

- **Cross-cuts mọi end-session proposal moment** — fire tại text-output composition time, không tại file-read time. Không có natural file-scope trigger.
- **Path-scope sẽ miss case quan trọng** — nếu scope `.claude/**` only, rule sẽ vắng mặt khi session work với code files thuần `kitehub/**` hoặc `documents/**` (đa số session) — đúng case rule cần fire nhất.
- **Hook-cover không khả thi v1** — phát hiện "Claude sắp output text propose end-session" cần NLP trên response candidate, vượt khả năng deterministic hook (PreToolUse/PostToolUse fire ở tool-call boundary, không ở text composition).
- **Token cost chấp nhận được** — ~1k tokens × mọi session; force-multiplier mỗi session tiết kiệm 1 false-end-propose round-trip + cache preservation.
- **Priority 🟠 MANDATORY giữ nguyên** — không nâng CRITICAL vì §3 exception "user explicit ask" relaxes; auto-load áp dụng theo `context-budget-mandate.md` §3.2 row 2.

Re-evaluate nếu: (a) Anthropic publishes pre-text-output NLP hook, (b) >5 false-positive trên session/quarter, (c) rule grows >300 lines (cost tăng).

---

## 8. Enforcement (per `rule-change-process.md` §6.5)

### 8.1 Memory auto-load (per-session)

Memory entry `feedback_session_end_context_check.md` (paired same-PR) loads at session start, reminds 5-bullet checklist trước mọi end-session text composition.

### 8.2 Self-detection (in-turn)

Trước khi send response chứa pattern §2 (end / clear / next session / context-degraded / compact), Claude mentally run §4 sequence. Nếu §4 không chạy → high probability sai threshold.

### 8.3 Reviewer / user manual

User có thể flag bằng cách hỏi "Sao đề xuất end mà chưa check %?" sau khi rule landed. Pattern repeated → file follow-up gap referencing this rule.

### 8.4 Override mechanism

Genuine exception (vd user explicit say "wrap up" — Claude execute không cần check; HOẶC end-of-context-window forced):

```
git commit -m "...
SESSION_END_CHECK_OVERRIDE: <reason — e.g., 'user explicit asked clear', 'context-window hard-limit forced end'>"
```

Trailer logged. Pattern frequency >5% triggers meta-review.

### 8.5 Detector (deferred per `incident-to-rule-pipeline.md` §3 premature-rule guard)

Future enhancement: scan recent session transcripts for end-session text WITHOUT corresponding `bash .claude/statusline-kite.sh` invocation. Defer per advisory-rule guard ≥7 ngày; memory auto-load + self-detection + worked self-test đủ cho v1.0.0.

---

## 9. Relationship to other rules

- **`context-budget-mandate.md`** §1 — auto-load <120k baseline; this rule operationalizes "when to act on budget" at end-session decision point
- **`agent-action-bias.md`** §1 Part A — do-it-yourself; this rule extends: do-state-check-yourself (run script vs guess)
- **`mcp-first-with-fallback.md`** — tool selection hierarchy; statusline script = Tier 3 (project script), correct usage
- **`feedback_meta_context_optimization.md`** (memory) — Wave 73 context savings; this rule sustains by preventing premature clear (which would lose cache + savings)
- **`incident-to-rule-pipeline.md`** — this rule = direct output 2026-05-19 user-flagged miss "Claude propose end without checking actual %" applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory + self-test all paired same PR
- **`output-review-mandate.md`** §3 — adds row "Session-end context check" tracking review standard
- **`feedback_session_end_context_check.md`** (memory, paired same-PR)

---

## 10. Log

- **2026-05-19 (v1.1.0):** MINOR — added §4 Step 0 docs-sync verification (BẮT BUỘC) + new §4.5 Docs-sync verification 5-target checklist extending `post-merge-sync-completeness.md` §2 4-target framework với 5th target (session-handoff note). Triggered by user-flagged miss 2026-05-19 same-session after Wave 100 Bucket D+F merge — I propose `/clear` cho fresh Phase 2 context BUT 4/5 sync targets stale (GAP-680 CSV OPEN vs file shipped DONE / ROADMAP §🎯 missing Wave 100/100.5/D/F entries / wave-history.jsonl missing Wave 100+ entries / session-handoff note absent cho 2026-05-19). User flagged "check updates đủ docs để start new session chưa, thêm rule là mỗi lần đề xuất new sessions thì phải thực hiện check update đủ docs chưa, thêm luôn vào rules check context ấy". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (existing rule v1.0.1 §4 covers context % only, không cover docs-sync targets at session-end decision moment; `post-merge-sync-completeness.md` covers per-PR-merge moment, không cover session-end-propose moment) → Rule+Enforce ✓ (this v1.1.0 + §4 Step 0 prepend + §4.5 5-target table + §5 banned shortcuts added + paired same-PR sync 4/5 stale targets fixed + worked self-test applied retroactively to current session) → Self-Test ✓ (this very session — rule fires correctly, drift detected on 4/5 targets, sync PR shipped same session per Step 0 mandate) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — extends previously-uncovered session-end docs-sync gate; no constraint loosening; existing session-end proposals grandfathered; rule applies prospectively từ this PR forward).
- **2026-05-19 (v1.0.1):** PATCH — same-session clarification sau initial self-test 2026-05-19. Script `bash .claude/statusline-kite.sh` standalone return `0%` (no stdin) → rule §4 instruction "Run: bash .claude/statusline-kite.sh" gây confusion. Discovered script reads JSON stdin với `transcript_path` field (per script line 7-11). §4 expanded với proper invocation recipe (TRANSCRIPT auto-find + JSON stdin construct + model auto-detect 200k vs 1M). Real self-test với proper stdin: model Opus 4.7 (1M), used 441k / 1M = 44% — verified §3 threshold "<50% → KHÔNG propose end" fires correctly tại rule-creation moment. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — clarification của existing invocation step, no constraint loosening; v1.0.0 enforcement intent unchanged).
- **2026-05-19 (v1.0.0):** Rule created at user request "thêm rule là check % context thực tế bằng .claude/statusline-kite.sh trước khi đề xuất end session". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no existing rule mandates context-% check at end-session decision point; `context-budget-mandate.md` covers auto-load size only, doesn't cover end-session gate; `agent-action-bias.md` covers do-it-yourself but không specific cho session lifecycle) → Rule+Enforce ✓ (this file + memory `feedback_session_end_context_check.md` paired same-PR + §6 worked self-test + §7 auto-load justification per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on 2026-05-19 session — rule fires correctly + evidence-based vs subjective decision) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint codifying previously-implicit "don't propose end prematurely" guard; no constraint loosening for prior sessions; existing sessions grandfathered, rule applies prospectively từ next session). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: check % before propose end) + ✅ unique (no overlap với existing context/session rules) + ✅ widely applicable (every session has end-decision moment) + ✅ body discipline §1 has 0 "and" conjunctions. Detector wiring (§8.5 transcript scan) deferred per premature-rule guard ≥7 ngày; v1.0.0 enforcement = memory auto-load + self-detection + worked self-test đủ.
