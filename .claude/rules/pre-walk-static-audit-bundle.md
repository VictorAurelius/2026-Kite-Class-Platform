---
paths:
  - "kitehub/kitehub-frontend/src/components/auth/**"
  - "kitehub/kitehub-frontend/src/app/(auth)/**"
  - "kitehub/*/src/main/java/**"
  - "kiteclass/*/src/main/java/**"
  - "kitehub/kitehub-gateway/src/main/resources/application.yml"
  - "scripts/check-stale-images.sh"
  - "scripts/check-fe-bare-catch.sh"
  - "scripts/check-be-rollback-side-effects.sh"
  - "scripts/check-gateway-shared-breaker.sh"
  - ".claude/rules/pre-walk-static-audit-bundle.md"
---

# Pre-Walk Static Audit Bundle — 4 detectors before user-facing flow walk

**Priority:** 🟠 MANDATORY — pre-walk static detection governance
**Version:** 1.0.0
**Created:** 2026-06-04
**Last-Reviewed:** 2026-06-04
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (4 scripts + CI wire + reviewer-checklist + worked self-test on Wave flow-kh1 G2 walk session 2026-06-04) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies pre-walk static detection bundle for bug classes surfaced by Wave flow-kh1 G2 walk)
**Applies to:** Coordinator workflow before triggering ANY user-facing flow walk (per `feature-ship-runtime-walk-mandate.md` §2) + each session start via `start-session collect-state.sh` for stale-image early signal

---

## 1. The Rule

> **Before triggering any user-facing flow walk (G2/G3 RST per Flow Verification Campaign), coordinator PHẢI run the 4-script pre-walk static audit bundle. Each script targets a bug class surfaced by Wave flow-kh1 G2 walk session 2026-06-04 — running pre-walk would have eliminated the same-day discovery cost.**

Force-multiplier: 1 chuẩn pre-walk bundle → mọi subsequent flow walk in the 22-flow Verification Campaign auto-screens these classes pre-walk → eliminate mid-walk rebuild + bug-catalog overhead per `feature-ship-runtime-walk-mandate.md` v1.1.0 §3.4 catalog-then-batch.

---

## 2. The bundle (4 scripts)

| # | Script | Class | Origin gap |
|---|---|---|---|
| 1 | `scripts/check-stale-images.sh` | Docker image older than latest source commit → rebuild needed | Wave flow-kh1 G2 (kc-core 2 days stale, missed GAP-866) |
| 2 | `scripts/check-fe-bare-catch.sh` | FE auth `} catch {` bare blocks masking error semantics | GAP-924, GAP-926 |
| 3 | `scripts/check-be-rollback-side-effects.sh` | BE rollback rotating secret/token without resurfacing to caller | GAP-927 |
| 4 | `scripts/check-gateway-shared-breaker.sh` | Gateway CircuitBreaker shared between read + write paths under same auth/instances family | GAP-928 (Phase 2 carve-out) |

All scripts ship with `--warn` (advisory exit 0) + default (strict exit 1) modes. Default in CI = `--warn` per v1.0.0 stabilization period.

---

## 3. When to run

| Trigger | Scripts |
|---|---|
| Before any G2/G3 RST walk per Flow Verification Campaign | All 4 |
| Before any `feature-ship-runtime-walk-mandate.md` §2 trigger | All 4 |
| Session start via `start-session collect-state.sh` | Stale-images count surfaced; full bundle invocation hint per `Stale images:` line |
| Pre-merge CI on PR touching covered paths | Bundle (WARN-only v1.0.0 via `continue-on-error`) |

Skip rule when: (a) docs-only PR (no walk impact), (b) pure infra change (no FE/BE/gateway diff), (c) override trailer per §5.

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Walk against running stack without stale-image check | Run `bash scripts/check-stale-images.sh` first — rebuild stale services BEFORE walk |
| Skip bundle "vì small flow" | Bundle takes <10s end-to-end; cost vs mid-walk discovery cost is ~50x lower |
| Treat WARN as ignore | WARN = file follow-up gap per `discovery-to-gap-inline-filing.md` §3 OR fix inline |
| Run bundle but skip stale-image check "image looks recent" | Empirical check — image ISO timestamp vs git log commit time; "looks recent" is feel-based |
| Add new bug class without ship-time script | Each new flow-walk bug class surfaced → add detector to bundle per `cross-flow-bug-class-sweep.md` §3 |

---

## 5. Override mechanism

```
git commit -m "...
PRE_WALK_BUNDLE_SKIP: <reason — e.g. docs-only walk, no FE/BE diff>
PRE_WALK_BUNDLE_FOLLOWUP: <gap link if any deferred>"
```

Pattern frequency >10% per quarter triggers meta-review.

---

## 6. Worked self-test — Wave flow-kh1 G2 walk session 2026-06-04 (originating incident)

**Apply bundle retroactively to the 6 bugs surfaced during Wave flow-kh1 G2 walk:**

| Bug | Class | Would bundle catch pre-walk? |
|---|---|---|
| Stale kc-core image (missing GAP-866) | Stale image | ✅ check-stale-images.sh would WARN |
| GAP-924 silent 401 (FE bare catch) | FE bare catch | ✅ check-fe-bare-catch.sh would flag site |
| GAP-926 (FE bare catch + sweep candidates) | FE bare catch | ✅ check-fe-bare-catch.sh would flag 3 sites incl deferred BetaRequestForm:124 |
| GAP-927 rollback rotation invisible | BE rollback side-effect | ✅ check-be-rollback-side-effects.sh would flag |
| GAP-928 shared breaker | Gateway shared breaker | ✅ check-gateway-shared-breaker.sh would flag pre-Phase-2 |
| GAP-930 EmailConsumer mismatch (@Async) | Out of scope this bundle | ❌ orthogonal class (separate detector if recurrence ≥2) |

**Counterfactual:** 5/6 bugs catchable pre-walk → ~2.5h walk session cost reduced by ~50% (no mid-walk rebuild cycle for stale image; no live debug for bare-catch + rollback side-effects). Bundle runtime ~10s. ROI ≈ ~1.25h saved per walk × 22 flows in Campaign = ~27h aggregate prospective saving.

**Verdict:** Bundle fires on 5/6 originating bug classes. Self-test PASS ✅.

### Verified on this PR (post-build):

```
check-stale-images.sh:            8 services, 1 STALE (kiteclass-frontend — informational)
check-fe-bare-catch.sh:           3 sites (BetaRequestForm:124 + RecoveryCodesDisplay:51 + 2fa-challenge:27)
check-be-rollback-side-effects.sh: 0 sites (GAP-927 fix applied)
check-gateway-shared-breaker.sh:  0 sites (GAP-928 Phase 2 carve applied)
```

All scripts return matching expected fix-state per origin gaps. Detector parity confirmed.

---

## 7. Enforcement

### 7.1 Reviewer-checklist (active now)

Pre-merge for PR closing a gap whose AC mentions a user-facing flow walk OR PR pushing a G2/G3 recipe:

- [ ] PR body cites `## Pre-walk static audit bundle` section with N/N scripts run + verdicts?
- [ ] Stale image surfaced → rebuild documented?
- [ ] WARN entries → either fixed inline OR follow-up gap filed per `discovery-to-gap-inline-filing.md`?

### 7.2 Session-start signal (active now)

`collect-state.sh` adds line `Stale images: <count>` with hint to run bundle when ≥1.

### 7.3 CI wire (active WARN-only)

`.github/workflows/quality-rules-skills.yml` job `pre-walk-static-audit` runs 3 of 4 detectors (skips stale-images — no docker daemon in CI) WARN-only via `continue-on-error: true`. Stabilization period; promote to BLOCKING after 30 days zero false positives.

### 7.4 Detector deferral (HONEST per `incident-to-rule-pipeline.md` §3.1)

Stale-image CI detector deferred — needs Docker daemon hosted runner (cost > value cho v1.0.0); revisit when self-hosted runner standard. FE/BE/gateway detectors active in WARN mode (CI hosted runner suffices).

---

## 8. Relationship to other rules

- **`feature-ship-runtime-walk-mandate.md`** v1.1.0 §3.4 — catalog-then-batch walk workflow; this rule reduces catalog entries pre-walk
- **`cross-flow-bug-class-sweep.md`** §3 — sweep evidence mandate; each bundle script implements one sweep class permanently
- **`incident-to-rule-pipeline.md`** v1.1 — this rule = direct output Wave flow-kh1 G2 walk 2026-06-04 (6 bugs) applied through 5-stage
- **`discovery-to-gap-inline-filing.md`** v1.0.0 — WARN surfaced by bundle → file inline per §3
- **`docs-only-pr-no-block-wait.md`** v1.1.0 §5.5 — bundle scripts <60s so foreground OK
- **`rule-change-process.md`** §6.5 — rule + 4 scripts + CI wire + worked self-test + collect-state hook all paired same PR
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 bundle → 22-flow Campaign auto-screens)
- **`g2-handoff-md-mandate.md`** v1.0.0 — G2 recipe must include `## Pre-walk audit summary` row referencing bundle run

---

## 9. Auto-load justification (per `context-budget-mandate.md` §3.2)

Rule này dùng `paths:` frontmatter — path-scoped, không always-load. Lý do:
- Fires only at pre-walk decision moment (coordinator triggering RST walk OR PR touching covered paths)
- Path-trigger natural: FE auth components / BE service code / gateway application.yml / scripts/check-*.sh
- Token cost ~3k × moments when relevant (low frequency, high value)

```yaml
---
paths:
  - "kitehub/kitehub-frontend/src/components/auth/**"
  - "kitehub/kitehub-frontend/src/app/(auth)/**"
  - "kitehub/*/src/main/java/**"
  - "kiteclass/*/src/main/java/**"
  - "kitehub/kitehub-gateway/src/main/resources/application.yml"
  - "scripts/check-stale-images.sh"
  - "scripts/check-fe-bare-catch.sh"
  - "scripts/check-be-rollback-side-effects.sh"
  - "scripts/check-gateway-shared-breaker.sh"
---
```

(Frontmatter applied at top of file in same commit.)

---

## 10. Log

- **2026-06-04 (v1.0.0):** Rule created in response to Wave flow-kh1 G2 walk session 2026-06-04 — 6 unexpected bugs surfaced in ~3h. Root causes: stale Docker image (kc-core missing GAP-866), FE bare-catch masking errors (GAP-924, GAP-926), BE rollback side-effects invisible to caller (GAP-927), gateway shared breaker between read+write (GAP-928). Per `incident-to-rule-pipeline.md` v1.1 5-stage: Detect ✓ (Wave flow-kh1 G2 walk concrete recurrence — 6 bugs/3h) → Classify ✓ (no existing static bundle covers these classes prospectively; sister rules `cross-flow-bug-class-sweep.md` covers post-fix sweep, `feature-ship-runtime-walk-mandate.md` covers walk discipline not pre-walk static) → Rule+Enforce ✓ (this file + 4 scripts + CI wire WARN-only + collect-state.sh stale-images hint + worked self-test §6 per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 — 5/6 originating bug classes catchable pre-walk; detector parity verified on this PR's worktree state) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 22-flow Verification Campaign auto-screens prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying pre-walk static bundle; no constraint loosening; existing walks grandfathered; rule applies prospectively từ Wave flow-kh2+ forward 2026-06-04). Stale-image CI detector HONEST DEFER per `incident-to-rule-pipeline.md` §3.1 (Docker daemon required on runner — cost > value v1.0.0).
