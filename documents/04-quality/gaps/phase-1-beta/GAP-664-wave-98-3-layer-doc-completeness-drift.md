# GAP-664: Wave 98 3-layer doc completeness drift — preferences + email domains missing layers

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta (Living Docs 3-layer compliance)
**Found:** 2026-05-19 (Wave 98 post-closure audit suite — GAP-661 Business Logic + API Contract audits both flagged)
**Affects:** `documents/01-business/kitehub/preferences/` + `documents/01-business/kitehub/email/` 3-layer doc structure

## Problem

CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure" mandate: mỗi domain PHẢI có 3 files (`rules.md` + `use-cases.md` + `api-contract.md`). Wave 98 ship 2 new domains nhưng KHÔNG đầy đủ 3 layers:

| Domain | Files shipped | Missing |
|---|---|---|
| `preferences/` (B0) | `api-contract.md` only | `rules.md` + `use-cases.md` |
| `email/` (B1) | `rules.md` + `api-contract.md` | `use-cases.md` |
| `seed/` (B2) | `rules.md` + `use-cases.md` + `api-contract.md` (all 3) | ✅ model citizen |

Both audit agents (Business Logic + API Contract) independently flagged → 2-of-3 audit convergence = strong signal.

Pattern is **recurring** — Wave 92 GAP-640 admin-audit domain shipped với same drift (3-layer missing → P1 META filed). Wave 98 = 2nd occurrence của same anti-pattern → meta-process fix candidate.

Impact: incomplete 3-layer = business rules (`rules.md`) hoặc user-facing scenarios (`use-cases.md`) lost trong code; future readers cannot reconstruct intent từ docs alone. Living Docs rule trong CLAUDE.md violated.

## Root Cause

Bucket B0 + B1 agents prioritized "ship working code + minimal doc to declare endpoint" — over-indexed trên api-contract.md (the file API consumers need) + dropped rules.md/use-cases.md (the files business reviewers need). No CI gate enforces 3-layer completeness when new domain folder created.

## Proposed Fix

### Step 1: Backfill preferences/ rules + use-cases

`documents/01-business/kitehub/preferences/rules.md`:
- BR-PREFERENCES-001: Banner dismiss state TTL = 7 days (config key `kitehub.preferences.banner-dismiss-ttl-days`)
- BR-PREFERENCES-002: Banner re-shows after version bump (compare `kitehub.preferences.banner-version` with cookie value)
- BR-PREFERENCES-003: User can opt-out permanently via `/preferences` settings page (Wave 99 scope)

`documents/01-business/kitehub/preferences/use-cases.md`:
- UC-PREFERENCES-001: P2 Center Owner dismisses beta banner sau onboarding tour
- UC-PREFERENCES-002: After v0.9.0-beta → v0.9.1-beta version bump, banner re-shows once
- UC-PREFERENCES-003: User clicks "Don't show again" → permanent opt-out cookie + DB row (defer Wave 99)

### Step 2: Backfill email/ use-cases

`documents/01-business/kitehub/email/use-cases.md`:
- UC-EMAIL-001: P2 Center Owner receives welcome email với formal tone (Kính gửi anh/chị)
- UC-EMAIL-002: P1 Solo Teacher receives invite email với semi-formal tone
- UC-EMAIL-003: Password reset email — security-critical, HttpOnly token, 15-min TTL
- UC-EMAIL-004: List-Unsubscribe header allows one-click opt-out per GAP-657
- UC-EMAIL-005: Beta cohort receives broadcast email với Zalo OA CTA per GAP-660
- UC-EMAIL-006: Staff invite email với tenant context per GAP-659

### Step 3: Update `documents/01-business/README.md` index

Add 3 new domain entries (preferences / email / seed) per `docs-folder-structure.md` README §Directory Map.

### Step 4 (META — defer follow-up): Pre-flight check skill

Per Wave 92 GAP-640 + Wave 98 = 2 recurrences → file follow-up META gap:
- Pre-commit hook hoặc skill `pre-flight-check domain` auto-invoke khi new domain folder created
- Verify 3 files present BEFORE allow merge
- Sister rule trong `audit-to-gap-pipeline.md` §2.5 state-check at filing time

(Step 4 = future scope, NOT this gap; track separately as Wave 99+ META candidate.)

## Acceptance Criteria

- [ ] `preferences/rules.md` created với 3 BR-PREFERENCES-* IDs
- [ ] `preferences/use-cases.md` created với 3 UC-PREFERENCES-* IDs
- [ ] `email/use-cases.md` created với 6 UC-EMAIL-* IDs covering shipped templates
- [ ] `documents/01-business/README.md` index updated (3 new entries)
- [ ] BR-IDs / UC-IDs consistent across rules ↔ use-cases ↔ api-contract per Living Docs verification chain
- [ ] Business Logic audit refresh next wave reflects fix → Cat 1 score +4-6 pts

## Related

- **Parent audits:** `documents/04-quality/audits/business-logic/2026-05-19-wave-98-new-domains.md` + `documents/04-quality/audits/api-contract/2026-05-19-wave-98-new-contracts.md`
- **Carry-forward pattern:** GAP-640 (Wave 92 admin-audit 3-layer missing — same drift)
- **Rule:** CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
- **Rule:** `business-logic-review.md` §2 — 5-attribute coverage (constraint / config_key / source_of_truth / verification / migration)
- **Future scope:** META pre-flight skill (post-3rd-recurrence trigger per `incident-to-rule-pipeline.md` premature-rule guard)
