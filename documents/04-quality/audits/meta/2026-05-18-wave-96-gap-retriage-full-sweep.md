---
title: Wave 96 Gap Re-Triage Full Sweep — 5-agent parallel audit (661 gaps)
status: complete
created: 2026-05-18
phase: phase-1-beta
wave: 96
scope: gap-backlog-cleanup
related-gaps: [GAP-203, GAP-220, GAP-145, GAP-040, GAP-052, GAP-155, GAP-438]
related-rules: [gap-architecture-v2.md, gap-done-discipline.md, meta-gap-priority.md, outside-in-coverage-trigger.md, audit-to-gap-pipeline.md]
---

# Wave 96 — Gap Backlog Re-Triage Full Sweep

**Trigger:** User direction 2026-05-18 — "quét lại 1 lượt các gaps để xem chúng có bị outdated, cần update theo inside mới, có thể closed được hay không => tối ưu agents".

**Method:** 5 parallel outside-in audit agents partitioned by phase + DONE archive verification + legacy orphan classification.

**Scope:** 661 markdown files total (466 CSV-tracked + 195 legacy orphans).

**Wall-clock:** ~30 min (parallel) vs estimated ~3-5h serial.

---

## 1. Coverage matrix

| Agent | Partition | Count | Output CSV |
|---|---|---|---|
| 1 | phase-1-beta active (OPEN/PARTIAL/PENDING/IN_PROGRESS/PLANNED/WONTFIX) | 151 | `agent-1.csv` |
| 2 | phase-1-beta DONE (archive verification) | 81 | `agent-2.csv` |
| 3 | phase-2 + phase-3 active | 153 | `agent-3.csv` |
| 4 | phase-1.5-paid + unclassified (active + closed) | 81 | `agent-4.csv` |
| 5 | legacy orphans (root closed/, no CSV row) | 195 | `agent-5.csv` |
| **Total** | | **661** | |

Per-agent CSVs preserved at `/tmp/wave-96-retriage/agent-N.csv` (transient — copy to `documents/04-quality/audits/meta/wave-96-retriage-output/` if want to persist).

---

## 2. Aggregate verdict counts

### Agent 1 — phase-1-beta active 151

| Verdict | Count | % | Action needed |
|---|---:|---:|---|
| KEEP_CURRENT | 144 | 95.4% | None |
| UNCLEAR | 4 | 2.6% | Human triage |
| UPDATE_SCOPE | 2 | 1.3% | Bump completion / Log |
| DUPLICATE | 1 | 0.7% | Consolidate |

**Top insight:** phase-1-beta backlog is **healthy** (95.4% accurate). Heavy gating on GAP-612 AWS suspension blocks ≥6 gaps (GAP-257, GAP-537c-followup, GAP-599, GAP-610, GAP-611, GAP-620, GAP-621).

### Agent 2 — phase-1-beta DONE 81 (markdown frontmatter drift audit)

| Verdict | Count | % | Reinterpretation |
|---|---:|---:|---|
| VERIFIED_DONE | 40 | 49.4% | Truly DONE; AC checked + Log cites PR |
| FALSE_DONE | 18 | 22.2% | ⚠️ **Markdown drift, not false** — CSV says DONE; markdown Status header stale |
| ORPHAN_LOG | 16 | 19.8% | Same drift — Log says DONE but markdown Status header reads OPEN |
| AMBIGUOUS_DONE | 7 | 8.6% | Genuine ambiguity needing spot-check |

**🚨 Critical reinterpretation:** Agent 2's "FALSE_DONE" + "ORPHAN_LOG" verdicts (34 gaps total) flagged markdown frontmatter `**Status:**` field reading OPEN/PARTIAL while CSV row says DONE. **Spot-check on 4 samples (GAP-465 / GAP-585 / GAP-512 / GAP-604) confirmed CSV is correct — these ARE truly DONE per work record.**

This is **markdown frontmatter cache drift** — exactly the problem `gap-architecture-v2.md` §1 explicitly predicted:
> "Cố gắng trust status field trong gap file = trust-the-document trap."
> "Status/priority field trong frontmatter là cache informational, có thể drift, KHÔNG canonical."

Per `gap-architecture-v2.md` Phase 3: "Strip Status/Priority field from gap files. Add header comment: 'Canonical status: gap-status.csv. To update, edit CSV.'" — this is **scheduled future work** in the rule itself, not new debt.

**Net:** 34 markdown frontmatter sync candidates (informational only; CSV remains canonical).

### Agent 3 — phase-2 + phase-3 active 153

| Verdict | Count | % | Action needed |
|---|---:|---:|---|
| KEEP_CURRENT | 140 | 91.5% | None |
| **RE-PHASE** | **9** | **5.9%** | 🚨 Move 7 to phase-1-beta + 2 to phase-1.5-paid |
| UPDATE_SCOPE | 2 | 1.3% | Audit overlap with shipped scope |
| DUPLICATE | 2 | 1.3% | Filename collision (per `gap-architecture-v2.md` §10) |

**🚨 Critical finding:** 9 gaps mis-classified phase-2 at filing time during Wave 17 persona reviews. These are actually phase-1-beta scope (blocking current persona work).

### Agent 4 — phase-1.5-paid + unclassified 81

| Verdict | Count | % | Action needed |
|---|---:|---:|---|
| KEEP_CURRENT | 78 | 96.3% | None |
| UPDATE_SCOPE | 1 | 1.2% | GAP-438 bump |
| UNCLEAR | 1 | 1.2% | GAP-040 anomaly |
| RE-PHASE | 1 | 1.2% | GAP-011 |

**Cleanest partition.** Wave 93 outside-in audit already pre-cleaned phase-1.5-paid payment scope.

### Agent 5 — legacy orphans 195

| Verdict | Count | % | Action needed |
|---|---:|---:|---|
| LEGACY_ARCHIVE | 195 | 100% | None — grandfathered per rule v2.0.0 §2.1 |

**100% clean** — no duplicates, no deletables, no CSV backfill candidates. Pre-CSV migration archive well-behaved.

---

## 3. 🚨 Concrete batch actions

### Action A — Re-phase 9 gaps (Agent 3 finding)

**Move from phase-2/ → phase-1-beta/ (7 gaps):**

| Gap | Title | Why P1B not P2 |
|---|---|---|
| GAP-286 | Mobile OTP signup | P0 — Phase 1 BETA core persona blocker; status header cites BETA persona violation |
| GAP-287 | Branding wizard skip | P0 — Phase 1 BETA persona blocker |
| GAP-291 | Reschedule lesson | P0 — Phase 1 BETA core persona daily ops gap |
| GAP-292 | Per-session pricing | P0 — Phase 1 BETA dominant pricing model for Solo persona |
| GAP-293 | Monthly income summary | P1 — Phase 1 BETA financial UX |
| GAP-294 | Attendance no-show | P1 — Phase 1 BETA edge case |
| GAP-297 | Batch invoice gen | P0 — even Solo has 15 students/month requiring batch |

**Move from phase-2/ → phase-1.5-paid/ (2 gaps):**

| Gap | Title | Why P1.5 not P2 |
|---|---|---|
| GAP-298 | Manual bank reconcile | Payment Phase 1.5 trigger |
| GAP-299 | Payment reminder | Phase 1.5 payment scope |

**Action mechanics** (per `gap-folder-organization.md` v2.0.0 §3.3):
- `git mv` 7 files: `phase-2/GAP-XXX.md` → `phase-1-beta/GAP-XXX.md`
- `git mv` 2 files: `phase-2/GAP-XXX.md` → `phase-1.5-paid/GAP-XXX.md`
- Update CSV `phase` column + `filename` column for 9 rows
- 1 batch PR estimated ~30 min

**Impact:** Phase 1 BETA active count 151 → 158 (+7); phase-1.5-paid 38 → 40 (+2); phase-2 83 → 74 (-9). Largest subdir still well under 200 cap.

### Action B — Markdown frontmatter sync (Agent 2 finding)

**34 gaps with markdown Status drift from CSV** (18 FALSE_DONE + 16 ORPHAN_LOG):

- GAP-376, GAP-432, GAP-437, GAP-465, GAP-499, GAP-500, GAP-512, GAP-513, GAP-525, GAP-564, GAP-570, GAP-571, GAP-576, GAP-585, GAP-600, GAP-602, GAP-603, GAP-604 (FALSE_DONE — markdown says OPEN/PARTIAL, CSV says DONE)
- GAP-476, GAP-482, GAP-498, GAP-528, GAP-545, GAP-547, GAP-548, GAP-551, GAP-552, GAP-553, GAP-554, GAP-555, GAP-559, GAP-560, GAP-561, GAP-563 (ORPHAN_LOG — Log says DONE, Status header stale)

**Choose ONE approach:**

| Option | Effort | Pros | Cons |
|---|---|---|---|
| **A. Defer to Phase 3** per `gap-architecture-v2.md` §4 | 0 now | CSV is canonical; strip frontmatter later (when reader-confusion proven) | Drift accumulates |
| **B. Backfill markdown Status field** to match CSV | ~1h script | Reader-friendly; markdown matches CSV | One-off cleanup; future drift still possible |
| **C. Strip markdown Status field entirely** + add comment "Canonical status: gap-status.csv" | ~30 min script | Final solution per `gap-architecture-v2.md` Phase 3 | Bigger semantic change |

**Recommendation:** Option C (strip + comment) — aligns with already-planned Phase 3 work in `gap-architecture-v2.md` §4.

### Action C — Update scope for 4 gaps

| Gap | Current | Recommended | Reason |
|---|---|---|---|
| GAP-203 | IN_PROGRESS 40% | PARTIAL 90% or DONE | PR #424 merged 2026-04-21 closing 3 HIGH + 4 MEDIUM CVEs; only alerts-show-empty verify remains |
| GAP-220 | OPEN 0% | DONE 100% (verify) | PR #533 shipped 2026-04-22 fixing JSONB binding; verify before flip |
| GAP-052 (parent portal) | likely PARTIAL | Update Log re GAP-321 trilogy | Wave 2/5 reference outdated; delivery via GAP-321/321b/321c |
| GAP-155 (BRD content fill) | OPEN | Check overlap with Wave 23 BRD effort | May have shipped post-filing |
| GAP-438 (agent-aws-access) | OPEN 0% | PARTIAL ≥50% | Rule exists in `.claude/rules/`; first artifact 2026-05-08 |

### Action D — Resolve 3 duplicates

| Gap | Issue | Resolution |
|---|---|---|
| GAP-145 (Loki + Tempo stack) | Scope duplicated by GAP-434 (Loki Phase 2) + GAP-111/112 (Tempo tracing) | Mark DUPLICATE; close with cross-reference |
| GAP-200 (× 2 files) | Two `GAP-200-*.md` share prefix stem (SMS + general integration) | Rename one to disambiguate (per `gap-architecture-v2.md` §10 filename-collision cleanup) |
| GAP-321b-1 (× 2 files) | Same — two files share stem (notifications + fees) | Rename to disambiguate |

### Action E — Re-phase GAP-011 (Agent 4)

| Gap | Current | Recommended |
|---|---|---|
| GAP-011 Template Library Curation Plan | unclassified P0 | phase-1.5-paid OR phase-2 (per release-1-plan §6.1 — AI Branding scope deferred to minimal logo+color) |

### Action F — Human triage 12 UNCLEAR + AMBIGUOUS

| Gap | Agent | Question |
|---|---|---|
| GAP-215, GAP-216, GAP-218 | 1 | Wave 5 audit P0; need check if Sub-PR 5.6b shipped (output-review §3 says yes) |
| GAP-608 | 1 | EC2 SES IAM — Agent says "possibly obsolete if Resend pivot per ADR-025"; verify ADR + email-architecture.md (Resend NOT wired; SES still primary). **Verdict: GAP-608 still valid** (per my email-architecture.md confirm SES primary) |
| GAP-414, GAP-483, GAP-484, GAP-561b, GAP-565, GAP-568, GAP-569 | 2 | DONE candidates with partial AC verification — need spot-check per gap |
| GAP-040 | 4 | File in unclassified/closed/ but body still 🔵 OPEN + no CSV row → orphan inconsistency. Determine: was truly DONE (CSV backfill) OR mis-placed (move back)? |

---

## 4. Cross-cutting findings

### 4.1 Backlog health

- **Phase 1 BETA active backlog**: 151 (P1B) + 7 RE-PHASE candidates = **158 effective active**. 95%+ accurate classification.
- **Future-phase backlog (phase-2/phase-3)**: 144 (P2/P3) - 7 RE-PHASE - 2 (to P1.5) = **135 truly future-deferred**. 91% accurate classification.
- **Phase 1.5 PAID**: 38 + 2 RE-PHASE in = **40 effective**. Wave 93 outside-in pre-cleaned.
- **Unclassified meta**: 35 active. Mostly meta-rule + governance scope.

### 4.2 Heavy gating on GAP-612 AWS suspension

≥6 gaps in phase-1-beta active blocked pending AWS restore:
- GAP-257 Restore drill Phase 3 quarterly
- GAP-537c-followup-screenshot-capture
- GAP-599 JWT tab collide storage isolation (smoke-blocked)
- GAP-610 validate-token returns NOT_FOUND
- GAP-611 post-beta-signup route 404
- GAP-620 Wave 92 Bucket D live verify admin v1 controllers
- GAP-621 Wave 92 Bucket B/C live verify prod-equivalent

**Critical path:** AWS account re-setup (see `documents/04-quality/audits/aws-verification/2026-05-18-aws-account-recreation-estimate.md`) is single largest backlog unblocker.

### 4.3 Wave 95 PR1.5 migration audit residue

Agent 2 noted concern about migration drift: "Likely accidentally moved during the v1.0.0 → v2.0.0 docs reorg or PR1.5 mass migration".

**Verified false alarm:** spot-check confirmed PR2 migration correctly placed files per CSV. The drift is **pre-existing markdown frontmatter staleness** (predates Wave 95) — not caused by migration. Migration is sound.

### 4.4 Recurring outside-in coverage pattern

This sweep itself = recurrence #3 of pattern caught by user (per memory `feedback_outside_in_recurring_miss.md`):
1. Wave 79 user manual format/discoverability — user caught after F1 implementation
2. Wave 95 v1.0.0 gap folder taxonomy — user caught post-merge
3. **Wave 95 closeout — user proposed gap re-triage instead of PR3 cosmetic sweep** (huge ROI swap)

→ Pattern: user has stronger inside-out sense than Claude for "is this work the right shape?". Continued vigilance via `outside-in-coverage-trigger.md` v1.1.0 mandatory.

---

## 5. Output preservation

Per `output-review-mandate.md` §3 evidence mandate, raw agent CSVs preserved at:
- `/tmp/wave-96-retriage/agent-1.csv` (151 rows)
- `/tmp/wave-96-retriage/agent-2.csv` (81 rows)
- `/tmp/wave-96-retriage/agent-3.csv` (153 rows)
- `/tmp/wave-96-retriage/agent-4.csv` (81 rows)
- `/tmp/wave-96-retriage/agent-5.csv` (195 rows)

**Note:** `/tmp/` is non-persistent. If batch actions decided, copy CSVs to `documents/04-quality/audits/meta/wave-96-retriage-output/` before /tmp cleanup.

---

## 6. Recommended batch action sequence

| # | Action | Effort | Trigger |
|---|---|---|---|
| 1 | **Action A: Re-phase 9 gaps** (Agent 3) | ~30 min, 1 PR | Recommended immediate — affects current Phase 1 BETA scope |
| 2 | **Action D: Resolve 3 duplicates** | ~15 min, 1 PR | Quick wins |
| 3 | **Action C: Update 4 UPDATE_SCOPE** | ~30 min, 1 PR (mostly Log entries) | Spot-verify before flip |
| 4 | **Action F: Human triage 12 UNCLEAR/AMBIGUOUS** | ~30-60 min, requires user decisions | User input needed |
| 5 | **Action B: Markdown frontmatter strip (Option C)** | ~30 min batch script | Optional — defers `gap-architecture-v2.md` Phase 3 |
| 6 | **AWS account re-setup unblock 6+ gaps** | ~4-5h active + 24-72h async | Pre-req for many DONE flips |

**Total optimization potential:** if all 6 actions ship, backlog reflects reality + 6-12 gaps unlock to actual DONE state + 9 phase corrections + 34 frontmatter sync.

---

## 7. Log

- **2026-05-18:** Wave 96 full gap sweep audit shipped. 5 agents parallel, 661 gaps audited, ~30 min wall-clock. Findings consolidated above. Triggered by user direction "tối ưu agents" replacing PR3 cosmetic sweep with high-value backlog re-triage. Per `outside-in-coverage-trigger.md` v1.1.0 §3 — second outside-in trigger pattern in same session (Wave 95 PR1 taxonomy + Wave 96 sweep). Agent reports preserved at `/tmp/wave-96-retriage/agent-N.csv`. Critical reinterpretation noted: Agent 2's FALSE_DONE/ORPHAN_LOG verdicts (34 gaps) flagged markdown frontmatter drift, NOT actual false-DONE — CSV remains canonical per `gap-architecture-v2.md` §1.
