# GAP-424: Statuspage / Instatus runbook Vietnamese quick-start overlay

**Status:** 🟢 DONE 2026-05-15 (Wave 84 Bucket E — PR pending)
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — solo dev VN-first; blocks first-deploy execution if user can't follow mixed-language doc smoothly)
**Domain:** Operations / Documentation / VN-localization
**Found:** 2026-05-07 (post-WSL-migration session — Stream A user-action coverage audit)
**Affects:** Solo dev (VN-first per CLAUDE.md) executing Phase 1 BETA §1.5 Instatus signup + incident comms

---

## Problem

`documents/05-guides/operations/incident-comms-runbook.md` (205 dòng, Wave 38 Bucket C) là mixed-language: 31/205 dòng VI = ~15% diacritic. Một số sections Vietnamese (Bối cảnh + Severity Levels skeleton), nhưng:

1. 6-Step Incident Procedure các sub-sections English (Detect / Triage / Post Initial Incident / Update Cadence / Resolve / Post-mortem)
2. Commands + JSON examples + Statuspage-specific terminology English
3. KHÔNG có Instatus signup walkthrough (link vendor signup, custom domain setup `status.kitehub.vn`, severity config, test incident flow)

State-check (per `audit-to-gap-pipeline.md` §2.5) 2026-05-07:
```bash
$ grep -c "[àáảãạăâèéẹêìíịòóỏôơùúụừứửữựỳýỹỵđ]" \
    documents/05-guides/operations/incident-comms-runbook.md
31                         # 31/205 lines = 15.1% Vietnamese density (mixed)

$ grep -nE "^#{1,3} " documents/05-guides/operations/incident-comms-runbook.md
1:# Incident Communication Runbook         ← EN
10:## 1. Bối cảnh + Scope                  ← VI
21:## 2. Roles                              ← EN
36:## 3. Severity Levels                    ← EN
47:## 4. 6-Step Incident Procedure          ← EN (mixed sub-sections)
49:### Step 1 — Detect                      ← EN
60:### Step 2 — Triage                      ← EN
70:### Step 3 — Post Initial Incident       ← EN
                          # Headings 50/50 mixed
```

Plus: file references `release-1-deploy-runbook.md` Phase 1 §1.5 Instatus signup steps (5 sub-tasks) but doesn't have detailed signup walkthrough in this runbook itself — solo dev cần walkthrough KHI thực sự signup, không phải checklist 5 dòng.

## Root Cause

Wave 38 Bucket C (GAP-373) shipped runbook khi coordinator-applied sau Sonnet thrash 2x — VN translation pass-through bị truncate trong middle of write. Result = mixed VI/EN. Plus: scope of GAP-373 was incident-comms (post-incident communication), không bao gồm pre-deploy signup walkthrough — gap trong scope thiết kế.

## Proposed Fix

**Option A — Quick-start VI overlay + signup walkthrough** (~80 dòng, fastest):

Add 2 sections ở đầu file:

1. `## §0 Hướng Dẫn Nhanh (Vietnamese)` — overlay 6-step incident procedure VI (mỗi step 2-3 dòng, cross-link tới EN section bên dưới)

2. `## §0.1 Instatus Signup Walkthrough (Vietnamese)` — step-by-step:
   - Tạo Instatus account (Free tier)
   - Define 5 components (KH-API, KC-API, Marketing, Auth, Email)
   - Configure custom domain `status.kitehub.vn` → Cloudflare CNAME
   - Severity levels per `incident-comms-runbook.md` §3
   - Test: tạo incident sample → resolve → verify subscriber email
   - Common pitfalls VN (timezone GMT+7 vs Instatus UTC default, payment method)

**Option B — Section-by-section translation** (~120 dòng touch):
Translate 6-Step procedure VI in-place. More work but cleaner long-term.

**Recommended: Option A** for Phase 1 BETA speed (paralllel with GAP-423).

## Acceptance Criteria

- [x] `incident-comms-runbook.md` có `## 🇻🇳 Hướng dẫn nhanh — Tiếng Việt` overlay header section + existing `## §0 Hướng Dẫn Nhanh (Vietnamese)` section comprehensive
- [x] Overlay + §0 contain 6-step Vietnamese summary covering: Detect → Triage → Post Initial → Update Cadence → Resolve → Post-mortem
- [x] §0.1 Instatus Signup Walkthrough Vietnamese (7 sub-steps from account creation → custom domain → 5 components → severity config → test incident)
- [x] Overlay + §0 + §0.1 cross-link to corresponding EN sections §4 Step 1-6 + `release-1-deploy-runbook.md` Phase 1 §1.5
- [x] Total file VN diacritic density 32.6% (≥30% threshold) — measured via `grep -c` over 136/417 lines
- [x] No regression in EN section content (overlay added BEFORE existing §0; §0/§0.1/§1-§6 preserved)
- [x] PR title scope `docs(GAP-423/GAP-424):` per conventional commits

## Related

- `incident-comms-runbook.md` (the doc to overlay)
- `cloudflare-setup.md` (✅ Vietnamese — reference standard cho VN runbook)
- ADR-027 (Statuspage vendor decision — Instatus chosen)
- GAP-373 (parent — original incident-comms ship)
- GAP-394 (sibling — 4 missing runbooks)
- GAP-423 (sibling — SES VN overlay, same session)
- CLAUDE.md §"CRITICAL: Communication Language" Vietnamese-first rule
- Wave 39 candidate cluster (consider folding into Wave 39 or new Wave 40 + GAP-394 cluster)

## Log

- **2026-05-15:** Wave 84 Bucket E shipped — Vietnamese quick-start overlay added at top of `incident-comms-runbook.md` (Status/Wave/Last reviewed bumped to 2026-05-15). Overlay supplements existing `## §0 Hướng Dẫn Nhanh` + `## §0.1 Instatus Signup Walkthrough` sections (Wave 38 ship) with: standardized 3-5 paragraph quick-start template (what is Instatus / when to use / 6-step procedure summary / 5 common pitfalls / cross-links). VN diacritic density 30% → 32.6% (AC ≥30% met). All AC verified per `gap-done-discipline.md` §2. Sibling GAP-423 closed same PR.
- **2026-05-07:** Filed during post-WSL-migration session Stream A coverage audit. Found 5 doc gaps in user-action prereqs Phase 1 §1.1-1.5; Statuspage = 1 of 5. Sibling GAP-423 (SES VN overlay) filed same PR. Per `agent-action-bias.md` v1.0.0: agent files gap directly without offloading.
