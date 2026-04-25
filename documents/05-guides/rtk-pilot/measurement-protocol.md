# RTK Pilot — Measurement Protocol

Fill in this file at the END of the pilot session and commit alongside the
final pilot decision.

---

## Session metadata

- **Date:** ____
- **Operator:** ____
- **Session type:** wave-work / audit / debug / mixed
- **RTK version:** `rtk --version` → ____
- **Session duration:** ____ hours
- **Tools used:** Claude Code on (macOS / Linux / WSL2)

## Token measurements

Compare a comparable non-RTK session (similar shape + duration) with the
RTK-enabled session. Capture token counts via Claude Code `/context` at
session end.

| Metric | Pre-RTK baseline | RTK-enabled session | Δ |
|--------|----------------:|--------------------:|---:|
| Total session tokens | | | |
| Tokens via `Bash` tool returns | | | |
| Tokens via `Read` / `Grep` / `Glob` | | | |
| Tokens via `Agent` returns | | | |
| Tokens via `Write` (drafts + reports) | | | |

**Bash-only savings:** ____ %  
**Total session savings:** ____ %

(Vendor claim: 60–90% **on bash output**. Project ceiling estimate: ~10–15%
**total session**. Compare against estimate.)

## Output-loss incidents

List every time the AI was missing information that would have been present
without RTK. Include command, what was lost, what the AI ended up doing.

| # | Command | What RTK dropped | Impact |
|---|---------|------------------|--------|

(Empty table = zero incidents = ✅ for criterion 2.)

## Hook interaction observations

- `audit-gate.py` invocations: did they run as expected? Y / N + notes
- Gap-drift detection: did it run as expected? Y / N + notes
- Pre-commit hooks: did they run as expected? Y / N + notes
- Any unexpected hook ordering / silent skips? ____

## Vietnamese / VN diacritic test

Run `mvn test -Dtest=PdfGeneratorTest` (which produces output containing
VN names "Nguyễn Văn Đức", "Hóa đơn", etc.) and inspect the RTK-filtered
output that reached the model.

- Diacritics preserved? Y / N
- Test names + assertion content readable? Y / N
- Any garbled bytes? ____

## Specific command behaviors

| Command | Pre-RTK output size | RTK-filtered size | Useful or lossy? |
|---------|---------------------:|-------------------:|------------------|
| `git status` | | | |
| `git diff main...HEAD` (large) | | | |
| `gh pr list --json ...` | | | |
| `mvn test -Dtest=PdfGeneratorTest` | | | |
| `mvn dependency:tree` | | | |
| `docker compose ps` (if used) | | | |

## Tee mode usage

- Number of failed commands during session: ____
- Tee logs reviewed (`~/.local/share/rtk/tee/`)? Y / N
- Tee mode rescued any AI confusion? Y / N + which incident

## Final decision

**Verdict:** ☐ adopt team-wide / ☐ keep as opt-in only / ☐ reject

**Rationale:** ____

**If adopt:** open a follow-up PR adding a documented install path to
project onboarding, and a memory entry capturing measured savings.

**If reject:** add a memory entry (`feedback_rtk_pilot_rejected.md`)
recording the rejection reason; uninstall via
`./scripts/rtk-pilot/uninstall.sh`; the `experiment/rtk-pilot` branch
remains as historical record.
