# Integration Smoke Test — Browser Walk-through Procedure

Carved from `documents/04-quality/audits/ui-review/_REVIEW-TEMPLATE.md` §5.2. This is the human-performed step that catches the class of miss the 3 scripts can't catch on their own (visual rendering errors, broken JS, layout collapse, dark-variant drift).

This procedure is **MANDATORY** for every wave that ships HTML/JSX prototype kits — not an optional extra. The 2026-04-29 landing-page parity miss happened because reviewers spot-checked one file per PR but never opened the actual landing page in a browser. Scripts catch parity, browser catches everything else.

---

## 1. Start static HTTP server

The kits live as static HTML — open them via a localhost HTTP server (NOT `file://`, which breaks relative imports + Tailwind CDN cors).

```bash
# From repo root, on any free port. Default 9999 per `_shared/server-runbook.md`.
cd documents/02-architecture/design-system/ui_kits/
python3 -m http.server 9999 > /tmp/ui-kits-server.log 2>&1 &
SERVER_PID=$!

# Sanity-check it's up
curl -sI http://127.0.0.1:9999/ | head -1   # expect: HTTP/1.0 200 OK

# When done:
# kill $SERVER_PID
```

If port 9999 is busy, pick another (8000, 8080) — server-runbook calls out that 9999 is conventional but not magic.

---

## 2. Open landing page

```
http://127.0.0.1:9999/
```

Expected:
- Header: "Kite Design System / Round 2 — UI Kits Preview"
- Status pill (top right): green "✓ X kits SHIPPED"
- Hero with target / quality info
- Grid of N kit cards (one per kit folder, EXCLUDING `_shared` and `_v1-baseline`)

Failure modes to watch:
- Blank page → static server not serving from correct dir
- Cards present but link to 404 → folder removed but card not
- Cards missing → folder added but card not (← the 2026-04-29 incident)
- Tailwind classes not applied → CDN blocked / cors / network

Run **Tier 1 landing-parity script** here in case visual check missed something:

```bash
bash documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh
```

---

## 3. Click each kit card → verify per-kit landing

For EACH kit card, click → land on `<kit>/index.html`. Verify:

| Check | What to confirm |
|-------|-----------------|
| Page loads | No 404, HTML parsed |
| Kit title displayed | Matches landing card text |
| Persona declared | "Persona: PX <persona-name>" visible somewhere |
| Score self-report | Avg/Min /128 shown in kit landing or README link works |
| Screen list | Links to `screens/*.html` present |
| Direction-specific theme | Colors match landing card gradient (visual sanity) |

Failure → review template §3.{N} "Anomalies noted" gets a row.

---

## 4. Sample 3 screens per kit → verify each loads

Per kit, click 3 screens (default + one state + one variant). For each:

| Check | What to confirm |
|-------|-----------------|
| Screen renders | No JS error in DevTools console |
| Persona aligns | Density / vocabulary fit declared persona |
| Vietnamese UX | All copy VN, currency `đ`, dates `dd/MM/yyyy`, phone `0901 234 567` |
| Mock data | VN names, no Lorem, no `$` |
| Dark variant | If `*-dark.html` exists, visit + verify same fidelity |
| Empty/error states | If present, copy is empathetic (not robotic) |

Track results in review template §5.2:

```markdown
| Kit | Landing card | First screen loads | Sample 3 screens load | Dark variant | Notes |
|-----|:---:|:---:|:---:|:---:|---|
| kiteclass-pro-v2  | ✓ | ✓ | 3/3 | ✓ | — |
| kiteclass-parent  | ✓ | ✓ | 3/3 | ✓ | — |
| ...               |   |   |     |   |   |
```

---

## 5. Run Tier 2 prototype-review skill scripts

Independent of the browser walk-through, these run in shell:

```bash
bash .claude/skills/quality/ui-review-prototype/scripts/link-checker.sh
bash .claude/skills/quality/ui-review-prototype/scripts/landing-parity.sh
bash .claude/skills/quality/ui-review-prototype/scripts/state-coverage.sh
```

Wire results into review template §5.3:

| Script | Exit code | Findings |
|--------|:---:|----------|
| link-checker | 0 / 1 | none / list |
| landing-parity | 0 / 1 | none / list |
| state-coverage | 0 / 1 | none / list |

If any exit 1, the wave is **REQUEST_CHANGES** until either fixed or filed as a follow-up gap with documented PARTIAL exit ramp per `gap-done-discipline.md` §3.

---

## 6. Tier 3 CI/hook (when GAP-265 lands)

After GAP-265 ships, the same script invocations trigger automatically on PR open via `.github/workflows/ui-kits-integration.yml` and pre-commit `lefthook.yml`. Reviewer just verifies the CI badge is green; no manual run needed.

Until GAP-265 ships, the manual run in step 5 IS the enforcement.

---

## 7. Stop server

```bash
pkill -f "http.server 9999"
# or kill $SERVER_PID from step 1
```

---

## 8. Report

Save the filled `_REVIEW-TEMPLATE.md` instance to:

```
documents/04-quality/audits/ui-review/{YYYY-MM-DD}-{wave-name}-review.md
```

Per `output-review-mandate.md` v1.3.0 §3 — review evidence is preserved here for future reviewers + audit-gate hook to consume.
