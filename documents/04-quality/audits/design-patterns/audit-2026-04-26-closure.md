# Design Pattern Audit — 2026-04-26 (Wave 6 Closure)

**Run:** Sub-PR 6.5 (post-Wave 6 re-audit per `post-wave-audit-mandate.md` §2.3)
**Auditor:** Self-audit (Claude opus-4-7)
**Skill version:** `cfb88a1a` baseline + Sub-PR 6.4 calibrations + Sub-PR 6.5 Cat 4 Config-filter
**Scope:** All `*.java` under `kiteclass/*/src/main/java/` + `kitehub/*/src/main/java/` (same as baseline)
**Methodology:** `quality/design-pattern-audit/SKILL.md` + calibrated detector
**Predecessor:** `audit-2026-04-26.md` (baseline 70/100 Grade C)

> **Calibration note (per memory `feedback_audit_calibration.md`):** This re-run uses calibrated detectors from Sub-PR 6.4 (Cat 2/3/4/5) + a final Cat 4 Config-file filter added in this PR. Compare delta against baseline, not absolute number — score gain reflects both real refactor (6.2 TrialToPaidService) AND detector precision improvements.

---

## Score Summary

| # | Category | Baseline | **Closure** | Δ | Notes |
|---|----------|:--------:|:-----------:|:--:|-------|
| 1 | God Service / Class | 10 | **16** | +6 | TrialToPaidService 546 → 385 LOC (Sub-PR 6.2 Facade extract); InstanceService 496 LOC remains in 400-500 borderline (NOT a §3.1 violation — preventive monitor only) |
| 2 | Status switch / If cascade | 20 | **20** | 0 | Calibration eliminated FeignConfig HTTP-status false-positive; 0 raw hits |
| 3 | Primitive Obsession | 14 | **14** | 0 | No code change Wave 6; entity-side hits unchanged: Student.email/phone/address, Branding.address, ContactMessage.email |
| 4 | Leaky Abstraction (Vendor leak) | 16 | **20** | +4 | Calibration accepts `/client/` as adapter convention (ADR-020); Config-file filter (Sub-PR 6.5) skips bean-wiring imports → 0 false-positives, 0 real |
| 5 | Direct Event Publish | 10 | **12** | +2 | Calibration recognizes §3.5.1 Exception A markers — BrandingEventPublisher correctly skipped; 5 real bypasses remain (GAP-222a/b/c roadmap) |
| **Total** | | **70** | **82** | **+12** | **Grade C → B** |

Per scoring-guide §"Final Score Interpretation": `75-89 B — Patterns mostly applied; targeted refactor + bypass migration remaining`.

🎯 **Hits Wave 6 closure target** (≥78 per audit baseline §Comparison).

---

## Detailed Findings

### Category 1 — God Service (16/20, +6)

```
496  InstanceService.java                      ← in 400-500 range (preventive monitor)
385  TrialToPaidService.java                   ← post-refactor (was 546 — Sub-PR 6.2)
153  MigrationRetryRunner.java                 ← extracted from TrialToPaid
80   MigrationStateMachine.java                ← extracted from TrialToPaid
48   MigrationEventEmitter.java                ← extracted from TrialToPaid
```

**Verdict:** Zero violations of §3.1 (>500 LOC). InstanceService at 496 stays on monitor list — Sub-PR 6.3 was SKIPPED 2026-04-26 after state-check confirmed the service is below threshold; preventive refactor on a non-violating service rejected per YAGNI + Wave 6 plan §3 risk #4. Re-evaluate in next quarterly audit.

### Category 2 — Status Switch (20/20, 0)

Detector calibration (Sub-PR 6.4): xargs filter strips files where every `switch (...status)` is actually `switch (response.status())` HTTP status — eliminates the FeignConfig false-positive that scored 20/20 in baseline (correctly identified by inspection, but now eliminated upstream by detector itself).

**Verdict:** Clean. No domain-status switch cascades.

### Category 3 — Primitive Obsession (14/20, 0)

Calibrated detector now uses multiline `@Column` + entity-field broadening. Real entity-level violations (acceptable scoring at 14/20):

```
Student.email     (String, @Column(name = "email", length = 255))
Student.phone     (String, @Column(name = "phone", length = 20))
Student.address   (String, @Column(name = "address", columnDefinition = "TEXT"))
Branding.address  (String, columnDefinition = "TEXT")
ContactMessage.email (String, @Email)
```

**Acceptable (per calibrated scoring-guide):**
- Payment gateway DTOs (BigDecimal amount in `processRefund`) — boundary
- Utility classes (HexColorUtil, ColorUtils) — utility, not domain entity
- JWT/User services (String email parameters) — boundary signatures

**Verdict:** 3 entities affected (Student is the heaviest). NOT in current Wave 6 scope. Future P3 cleanup gap candidate when value-object library exists.

### Category 4 — Leaky Abstraction (20/20, +4)

Sub-PR 6.4 calibration: accept `/client/` packages as adapter convention. Sub-PR 6.5 Config-filter: skip `*Config.java` bean-wiring imports.

```
$ grep ... | calibrated filter
(0 results)
```

**Verdict:** Clean. `OllamaClient` package decision recorded in **ADR-020** (Wave 6.5 — accept `client/` as adapter convention; rename rejected as code-churn without architectural benefit).

### Category 5 — Direct Event Publish (12/20, +2)

Calibrated detector recognizes `design-patterns.md` §3.5.1 Exception A markers (`outbox is the reliability net` / `fast-path` comments). `BrandingEventPublisher` now correctly skipped.

Real bypasses remaining (5 sites in 5 services):

```
ParentInvitationServiceImpl.java:284  (kiteclass-core — GAP-222b, NOT blocked)
BrandingJobService.java:69            (kitehub-branding — GAP-222c, BLOCKED on 222a)
AIQueueDispatcher.java:65,70          (kitehub-branding — GAP-222c)
EmailServiceClient.java:588           (kitehub-subscription — GAP-222c)
InstancePurgeService.java:188         (kitehub-subscription — GAP-222c)
```

**Verdict:** Score +2 reflects calibration precision gain (zero false-positives now); real-bypass count unchanged because Phase 2 migration was deliberately deferred to GAP-222a/b/c per Sub-PR 6.4 scope decision (state-check showed kitehub modules need shared outbox lib first).

---

## Hotspot → Gap Pipeline (no new gaps this run)

Per `audit-to-gap-pipeline.md` §6 (Step 2 + 2.5):

| Finding | Existing gap? | Action |
|---------|--------------|--------|
| TrialToPaidService 546 → 385 LOC | ✅ GAP-046 (refactor row updated 6.2) | No new gap; Wave 6 closure marks done |
| InstanceService 496 LOC (still in 400-500) | ⚠️ GAP-046 (deferred — under threshold) | Monitor; no gap until LOC exceeds 500 |
| Student/Branding/ContactMessage Primitive Obsession | ⚠️ Defer (P3) | Future cleanup gap when value-object lib exists; not blocking GA |
| OllamaClient `/client/` naming | ✅ ADR-020 (this PR) | Decision recorded; not a gap |
| 5 services bypass Outbox | ✅ GAP-222a/b/c (Sub-PR 6.4) | No new gap; sub-gap chain captures migration |

---

## Wave 6 Score Trajectory

| Date | Sub-PR | Score | Grade | Note |
|------|--------|------:|:-----:|------|
| 2026-04-26 | 6.1 baseline | 70 | C | First-ever design-pattern audit; 5 real Cat 5 bypasses + 2 false-positives |
| 2026-04-26 | 6.2 (TrialToPaid Facade) | (in-flight, code only) | — | LOC dropped 546 → 385; tests stable |
| 2026-04-26 | 6.4 (policy + calibration) | (in-flight, calibration) | — | Detector cleaner; §3.5.1 policy live |
| **2026-04-26** | **6.5 closure (this run)** | **82** | **B** | Wave 6 target hit; 222a/b/c chain covers remaining migration |

---

## Skill Calibration Findings (this run)

For next audit cycle, no new calibrations needed. Sub-PR 6.5 closed the last gap (Cat 4 Config-filter). All 4 baseline calibration items now baked in.

**Future improvements to consider:**
- Cat 1: distinguish "monitor zone" (400-500 LOC) from "violation" (>500 LOC) in scoring-guide so InstanceService doesn't silently drag the score
- Cat 3: standalone "Primitive Obsession follow-up gap" for Student/Branding/ContactMessage if value-object library lands

---

## Next Audit Due

Per `post-wave-audit-mandate.md` §2.3 + §2.1:
- **Quarterly baseline:** 2026-07-26 (3 months out)
- **After GAP-222a + 222b ship** (estimated Wave 7 if prioritized) — expect Cat 5 to jump from 12 to ~16+ (bypasses migrated)
- **After Wave 7+ if Student value-object refactor** — expect Cat 3 to jump from 14 to ~18+

---

## Log

- **2026-04-26** — Wave 6 closure audit. Score 70 → 82 (+12, C → B). Cat 1 +6 (TrialToPaid refactor), Cat 4 +4 (Config-filter calibration), Cat 5 +2 (§3.5.1 marker recognition). Cat 4 false-positive count: 0 (was 1). Cat 5 false-positive count: 0 (was 2). Wave 6 closes 🟢 DONE for GAP-046.
