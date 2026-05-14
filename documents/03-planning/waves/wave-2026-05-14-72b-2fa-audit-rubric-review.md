---
title: Wave 72b — 2FA + Audit Skill Rubric Review + Admin Subpages Verify
status: draft
created: 2026-05-14
updated: 2026-05-14
waves: [72b]
gaps: [GAP-516, GAP-517, GAP-523, GAP-524, GAP-526]
---

# Wave 72b — 2FA + Audit Skill Rubric Review + Admin Subpages Verify

**Goal:** Close the remaining P1/P2 self-test + OWASP A07 surface AFTER Wave 72a lands. Bring 2FA + login alert online for admin, generalize per-check rubric pattern to 6 other audit skills, verify admin subpages actually work, and extend pre-handoff rule with 7 additional flow classes.

**Trigger:** Wave 72a completion (5 P0 closed). Wave 72b cannot start before Wave 72a Bucket C (GAP-518) merges — GAP-526 depends on admin UI being usable to verify subpages.

**Dependency on Wave 72a:**
- Bucket A (GAP-516 2FA) — independent
- Bucket B (GAP-517 login alert) — independent
- Bucket C (GAP-526 subpages verify) — **blocked on Wave 72a Bucket C merge**
- Bucket D (GAP-523 audit rubric review) — independent
- Bucket E (GAP-524 pre-handoff extension) — independent

**Estimated wall-clock:** ~60 min longest bucket (Bucket A — TOTP enrollment FE + BE + recovery codes), ~30 min coordinator wrap-up.

---

## 1. Brainstorm

**Q1 (alignment):** P2 admin persona; secondary security-audit credibility (Bucket D). Serves Phase 1 BETA "trustworthy admin surface" milestone + Phase 1.5 PAID readiness.

**Q2 (trade-offs):**
- 2FA enrollment UX vs admin-account-recovery — bucket A must ship 10 single-use recovery codes alongside enrollment to avoid lockout. Considered SMS fallback, rejected — VN Twilio not provisioned + adds compliance scope.
- Bucket D scope = "review 6 audit skills" — could be a full wave on its own. Accepted as single bucket because pattern is mechanical (apply primacy + per-check rubric extension established by Wave 71c PR #1278). One agent ships sister-rule scaffolds for each of 6 categories with per-check tables; deeper bug-finding work batched as follow-up gaps.

**Q3 (risks):**
- 2FA enrollment flow could lock out PLATFORM_ADMIN if recovery codes lost. Mitigation: codes shown ONCE at enrollment + AWS Secrets Manager backup runbook.
- Bucket D could ship 6 rules with poor-quality rubrics. Mitigation: each rule includes worked self-test against current main state; if rubric doesn't surface ≥1 finding, rubric isn't fine-grained enough.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-516 (2FA TOTP) | bg-agent | ~60min | ✅ kitehub-subscription auth + new kitehub-frontend pages |
| B | GAP-517 (login alert) | bg-agent | ~30min | ✅ kitehub-subscription LoginAuditService + Resend template |
| C | GAP-526 (subpages verify) | bg-agent | ~30min | ✅ verify-only + small FE fixes if found |
| D | GAP-523 (audit rubric review) | bg-agent | ~50min | ✅ `.claude/skills/quality/{quality,ops,perf,api,business,ui}-*/` + 6 NEW sister rules |
| E | GAP-524 (pre-handoff ext) | bg-agent | ~30min | ✅ `.claude/rules/pre-handoff-self-test-completeness.md` extend with 7 flow classes |

---

## 3. Scope (stub — full §3 deferred to dedicated Wave 72b plan PR)

**Stake tier:** MEDIUM (P1+P2 follow-up, no production blocker).
**Cross-layer?** Bucket A is cross-layer (FE+BE, NEW endpoints). **Bucket 0 Foundation REQUIRED** for Bucket A per `contract-first-for-cross-layer.md` §2.

Full §3 table + per-bucket file globs deferred to Wave 72b plan PR (filed after 72a merge).

## 4. State-Check Evidence (stub — full §4 deferred)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| (deferred) | (deferred) | Full State-Check Evidence to be collected when Wave 72b plan PR is drafted post-72a merge | per Rule 16 stub allowance | 🆕 deferred — full collection in Wave 72b plan PR |

This stub exists so Wave 72a closure PR can reference "next wave" + ROADMAP §🚀 has visibility. Per `gap-done-discipline.md` §3 PARTIAL exit-ramp: deferred work is tracked, not hidden.

---

## 4. Spawn condition

Wave 72b plan PR can open after:
- Wave 72a closure PR merged (`status: complete` flip + wave-history entry)
- ROADMAP §🚀 updated to reference Wave 72b
- gap-status.csv shows GAP-518 = DONE (Bucket C of 72a verified)

---

## 5. Log

- **2026-05-14** (stub draft): Stub plan file created to give Wave 72a closure + ROADMAP visibility into next wave scope. Full plan elaboration deferred to dedicated Wave 72b PR post-72a-merge.
