# RTK (Rust Token Killer) — 1-day pilot

**Status:** EXPERIMENTAL pilot — single developer, single day, opt-in only
**Created:** 2026-04-25
**Owner:** @VictorAurelius (initiator); review by lead before any team-wide rollout
**Upstream:** https://github.com/rtk-ai/rtk
**License:** Apache-2.0

---

## What is RTK

A Rust binary that installs a Claude Code `PreToolUse` Bash hook to filter, deduplicate, and compress noisy command output before it reaches the model context. Targets `git`, test runners, build tools, `docker`, JSON, AWS CLI. Vendor claim: 60–90% token savings on bash-heavy sessions.

**Caveat — relevant to OUR project:** RTK only wraps the `Bash` tool. It does **not** affect `Read`, `Grep`, `Glob`, `Agent`, or `Write` — which together account for the majority of our session-token spend (audit reports, gap files, Wave plan amendments, code reads, agent returns). Realistic pilot ceiling: ~10–15% session-level savings, not 60–90%.

Rationale + full feasibility analysis: see this PR's review thread.

---

## Pilot scope

**One developer, one workday.** Measure-and-decide. No team-wide rollout, no CI integration, no implication for any other developer's machine.

| Hard rules | |
|------------|----|
| Telemetry | **Disabled** — `RTK_TELEMETRY_DISABLED=1` exported globally; reject opt-in prompt during `rtk init` |
| Failure mode | RTK "tee mode" leaves full output in `~/.local/share/rtk/tee/` for any failing command — no information loss on test/build errors |
| Vietnamese diacritics in test data | Watch for any deduplication or truncation that mishandles VN strings; if observed, abort pilot and file a gap |
| Hook stack interaction | Existing PreToolUse hooks: `audit-gate.py`, gap-drift detection, pre-commit checks. RTK adds one more. Verify ordering doesn't break audit-gate decisions |
| Rollback | `scripts/rtk-pilot/uninstall.sh` restores the unhooked state in one command |

---

## Install + run + measure

```bash
# 1. Install (telemetry-decline + hook setup)
./scripts/rtk-pilot/install.sh

# 2. Verify install + telemetry disabled
./scripts/rtk-pilot/check.sh

# 3. Run a normal Claude Code session (wave work, audit, debugging).
#    Capture session token counts before + after via /context.

# 4. After session ends, fill in:
#    documents/05-guides/rtk-pilot/measurement-protocol.md

# 5. Uninstall
./scripts/rtk-pilot/uninstall.sh
```

---

## Success criteria (for adopting team-wide)

All four must hold for a follow-up PR proposing team-wide adoption:

1. **Token savings ≥ 20% on bash-heavy sessions** — measured against a comparable non-RTK session of similar shape (wave work / audit / debug).
2. **Zero output-loss incidents** — the AI never misses information that an unfiltered output would have surfaced. Test failures / build errors / `git diff` content all reach the model with sufficient detail to act.
3. **No interaction with existing hooks** — `audit-gate.py` continues to block / warn correctly; gap-drift detection runs as expected.
4. **Vietnamese / VN diacritic content survives compression** — no garbled or dropped chars when `mvn test` output contains VN test data (e.g., "Nguyễn Văn Đức", "Hóa đơn").

If any criterion fails, file a gap (or close the pilot with a "WONT_FIX" note explaining which criterion failed) and revert. Do not roll out partial-wins.

---

## Open questions to resolve during pilot

- **Hook ordering:** does Claude Code execute multiple PreToolUse hooks in a defined order? Confirm `audit-gate.py` runs before/after RTK as appropriate.
- **`gh pr ...` JSON output:** we already use `--json`; does RTK further compress it or pass through? If it tries to "smart-filter" structured JSON, that's a regression.
- **`mvn dependency:tree`:** does RTK keep the actual dependency tree or strip it? If the latter, transitive-version debugging becomes impossible.
- **Tee log retention:** `~/.local/share/rtk/tee/*` accumulates; is there an automatic prune? File a follow-up gap if not.

---

## Rollback / removal

```bash
./scripts/rtk-pilot/uninstall.sh
```

This:
1. Removes the RTK PreToolUse hook from the global Claude Code settings.
2. Optionally uninstalls the `rtk` binary itself (the script asks).
3. Leaves `documents/05-guides/rtk-pilot/` in place as historical record.

If the pilot is rejected, also: remove `experiment/rtk-pilot` branch, file a "rejected after pilot" log entry in `measurement-protocol.md`, and close the followup planning gap (filed during pilot if needed).

---

## Related

- Vendor README: https://github.com/rtk-ai/rtk
- Memory: (to be added if pilot succeeds — `feedback_rtk_pilot_outcome.md`)
- CLAUDE.md output-discipline rules — `scripts/check-ci.sh`, `scripts/test-local.sh`, `| tail -N` patterns RTK partially overlaps with
- `mcp-first-with-fallback.md` — alternative bash-savings path via GitHub MCP structured queries
