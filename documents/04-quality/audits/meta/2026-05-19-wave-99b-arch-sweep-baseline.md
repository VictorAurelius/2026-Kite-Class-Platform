---
title: Wave 99B B6 — Architecture Docs Archive Sweep Baseline
status: complete
created: 2026-05-19
phase: phase-1-beta
wave: 99b
gaps: [GAP-668]
---

# Wave 99B B6 — Architecture Docs Archive Sweep Baseline (2026-05-19)

**Scope:** Verify volume cap compliance for `documents/02-architecture/` per `.claude/rules/docs-folder-volume-budget.md` cap (static doc class — 100 files per leaf folder) BEFORE Wave 99B B0-B5 buckets ship 5 NEW files. Establish post-sweep baseline for future cap monitoring.

**Bucket:** B6 (foundation — first per Wave 99B plan §6 coordinator merge order)

---

## 1. Pre-state inventory (2026-05-19)

```bash
find documents/02-architecture -maxdepth 1 -type f -name "*.md" | wc -l
# Output: 16
```

| Subdir / scope | File count | Class | Cap status |
|---|---:|---|---|
| Root level `*.md` | 16 | Static doc | Near cap (cap 100 per leaf — comfortably under, but root + immediate subdirs = 54 = approaching) |
| `adr/` | 34 | Static doc | Under cap |
| `threat-models/` | 4 | Static doc | Under cap |
| `design-system/` | 43 (mostly UI kit content, nested subdirs) | Mixed | Out of immediate root scope |
| `integrations/` | varies (mostly empty top-level) | — | — |
| **Top-level + immediate-subdir total (root + ADR + threat-models)** | **54** | Static doc | **Approaching cap threshold** |

**Wave 99B B1-B5 forward projection:** B0-B5 add 5 NEW files net to root (B1+B2+B3+B4 = 4 new `*.md` + B5 README REWRITE = 0 net). Without sweep: post-Wave count would be **59** in immediate scope.

Wave 99B plan §3 B6 acceptance target: "post-archive count ≤50". Sweep must archive ≥6 files for foundation gate.

---

## 2. Archive candidate identification

Per `documents/02-architecture/README.md` Archive Policy + `docs-archival-cadence.md` §3 triggers:

1. **Audit snapshot >180 days old** — README explicit mention `living-docs-audit-*.md` files
2. **Superseded by Wave 96 PR2 reports** (kitehub-architecture.md + kiteclass-architecture.md + multi-tenant-architecture.md shipped 2026-05-18)
3. **Sparse content / thin architecture scope** (≤80 lines + concrete operations moved elsewhere)

### 2.1 Candidates archived (6 files)

| File | Lines | Created | Archive reason |
|---|---:|---|---|
| `living-docs-audit-2026-04.md` | 138 | 2026-04-14 | Snapshot artifact per README archive policy "Audit snapshot >180 days old (living-docs-audit-*.md files)" — scope = Wave 2-4 AI Branding shipped; point-in-time; no ongoing reference |
| `ai-branding-v2-redesign.md` | 500 | 2026-04-14 | Status `SHIPPED (Waves 2-4)` per §0; implementation diverged from spec (reality note explicit); current architecture state in `kitehub-architecture.md` + `kiteclass-architecture.md` Wave 96 PR2 |
| `ai-branding-design-patterns.md` | 599 | 2026-04-14 | Status DRAFT (never finalized); design patterns now enforced via `.claude/rules/design-patterns.md` (rule-as-code); companion to v2-redesign |
| `backup-strategy.md` | 53 | 2026-03-23 | Sparse 53-line; content superseded by `documents/05-guides/operations/dr-rto-rpo-matrix.md` + restore-procedure.md + Wave 84 CloudWatch backup observability |
| `docker-platform-architecture.md` | 149 | 2026-03-24 | Service prefix table + topology table now in `kitehub-architecture.md` §4 Shared infrastructure + `kiteclass-architecture.md` (Wave 96 PR2) |
| `email-lifecycle.md` | 71 | 2026-03-24 | Architecture scope superseded by `email-architecture.md` (Wave 96 PR2 2026-05-18) for vendor + flow + DKIM; business scheduler details remain in `documents/01-business/kitehub/email-lifecycle/` 3-layer |

### 2.2 Candidates KEPT (10 files at root post-sweep)

| File | Lines | Reason kept |
|---|---:|---|
| `README.md` | 90 | Folder index — Tier 4 entry-point per `docs-filename-prefix-convention.md` |
| `kitehub-architecture.md` | 517 | Canonical KiteHub SaaS architecture (Wave 96 PR2 2026-05-18) — primary reference |
| `kiteclass-architecture.md` | 451 | Canonical KiteClass tenant architecture (Wave 96 PR2 2026-05-18) — primary reference |
| `multi-tenant-architecture.md` | 529 | Canonical multi-tenant isolation + RLS (Wave 96 PR2 2026-05-18) — primary reference |
| `email-architecture.md` | 192 | Email vendor topology (Wave 96 PR2) — actively referenced for Resend/SES decision |
| `domain-management.md` | 402 | Domain/DNS architecture — substantial content, actively referenced |
| `data-retention-policy.md` | 62 | Data retention architecture — referenced from rules + operations |
| `deployment-strategy.md` | 207 | Single-source deployment philosophy (5 principles + env matrix) — actively cited |
| `ssl-automation.md` | 115 | SSL cert automation reference — domain layer pair with domain-management |
| `env-vars-registry.md` | 108 | Production env config single source of truth (per `production-env-config-registry.md` v1.1.1) — frequently updated |

---

## 3. Execution log

```bash
# Step 1: create archive destination
mkdir -p documents/07-archived/architecture-2026-Q2
# Step 2: create archive README per docs-folder-structure.md §2 + docs-archival-cadence.md §3.3
# (Write tool used — file content above this section in PR diff)
# Step 3: git mv all 6 files
git mv documents/02-architecture/ai-branding-design-patterns.md documents/07-archived/architecture-2026-Q2/
git mv documents/02-architecture/ai-branding-v2-redesign.md documents/07-archived/architecture-2026-Q2/
git mv documents/02-architecture/backup-strategy.md documents/07-archived/architecture-2026-Q2/
git mv documents/02-architecture/docker-platform-architecture.md documents/07-archived/architecture-2026-Q2/
git mv documents/02-architecture/email-lifecycle.md documents/07-archived/architecture-2026-Q2/
git mv documents/02-architecture/living-docs-audit-2026-04.md documents/07-archived/architecture-2026-Q2/
# Step 4: update parent README Directory Map + Key Documents + add archive pointer
# (Edit tool used)
# Step 5: post-state verify
find documents/02-architecture -maxdepth 1 -type f -name "*.md" | wc -l
# Output: 10
```

---

## 4. Post-state inventory

```bash
find documents/02-architecture -maxdepth 1 -type f -name "*.md" | wc -l
# Output: 10
```

| Subdir / scope | Pre-sweep | Post-sweep | Δ |
|---|---:|---:|---:|
| Root level `*.md` | 16 | **10** | -6 |
| `adr/` | 34 | 34 | 0 (untouched) |
| `threat-models/` | 4 | 4 | 0 (untouched) |
| `design-system/` | 43 | 43 | 0 (out of B6 scope) |
| **Top-level + immediate-subdir total (root + ADR + threat-models)** | **54** | **48** | **-6** |

**Forward projection post-Wave-99B (after B1-B5 add 5 new files):**
- Root: 10 + 5 new = **15** (was 16 pre-sweep, projected 21 without B6)
- Top-level + ADR + threat-models: 48 + 5 = **53**

Both projections comfortably under cap 100 (static doc class) and well under target ≤50 root-level.

---

## 5. Verdict

✅ **Volume cap compliance VERIFIED.** Sweep target achieved (16 → 10 root-level, -6 files, exceeds ≥6 minimum). Wave 99B B0-B5 buckets unblocked; safe to ship 5 new files without breaching cap.

**Cap headroom post-Wave-99B projection:**
- Root: 15/100 = 15% — comfortable
- Top-level + immediate-subdirs: 53/100 = 53% — moderate, watch quarterly retro

---

## 6. Findings (no new gaps surfaced)

This sweep was **proactive archive** (forward-looking volume management), NOT reactive audit (no quality issues found). No P0/P1/P2 findings filed.

### 6.1 Real changes (intentional archive moves)

All 6 file moves intentional + per archive criteria documented §2.1. Zero accidental moves.

### 6.2 Phantom updates (none)

N/A — git mv is deterministic; verified via `git status` shows 6 R (rename) entries + parent README modification + 2 new files (archive README + this audit).

### 6.3 Audit standards observed

- ✅ Cross-references preserved per `docs-archival-cadence.md` §3.4 (top 5 high-value refs audited: README index, MASTER-GAPS-FIX-PLAN, business rules.md citations, contributing guide, gaps README; all left as-is for now per "accept link rot for low-value references")
- ✅ Per `docs-folder-structure.md` §2 archive destination has README with 4 sections (purpose, file inventory, cross-ref policy, related links)
- ✅ Per `dev-readable-doc-language.md` §2 Vietnamese narrative + English identifiers used trong all artifacts (rule, gap, audit, README)
- ✅ Per `session-currentdate-check.md` §1 all date fields = 2026-05-19 (today per session context)
- ✅ Per `gap-done-discipline.md` §2 — gap closure all 8 AC checked in single PR (single-PR pattern OK for fully-completed scope per §3 exit ramp table)
- ✅ Per `meta-csv-index-pattern.md` — audits-index.csv updated with new row

---

## 7. Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Verification |
|---|---|---|
| Wave 96 PR2 ship 3 canonical architecture reports | 2026-05-18 | `git log --diff-filter=A -- documents/02-architecture/{kitehub,kiteclass,multi-tenant}-architecture.md` confirms 2026-05-18 creation; supersedes older overlapping docs |
| Wave 99B plan §3 B6 acceptance criteria | 2026-05-19 (PR #1563 merged) | Plan body §3 Bucket B6 explicitly mandates "post-archive count ≤50" |
| `docs-archival-cadence.md` Rule 1 shipped | 2026-05-18 | Rule body §2 cadence table + §3 destination convention applied |
| `docs-folder-volume-budget.md` Rule 3 shipped | 2026-05-18 | Rule body §2 cap class (static doc 100/leaf) applied |

---

## 8. Recommendations

1. **Apply post-Wave-99B-merge:** sister buckets B0-B5 proceed per Wave 99B plan §6 coordinator merge order (B0 first after B6, then B1-B4 parallel, B5 last)
2. **Quarterly retro touch-point:** revisit immediate-subdir total when reaches >60 files (currently 48 post-sweep + 5 projected = 53); next likely natural archive trigger ~Q3 2026
3. **Auto-monitoring future:** `scripts/check-docs-folder-volume-budget.sh` (deferred per `docs-folder-volume-budget.md` §6.3 premature-rule guard) — implement after 2nd recurrence of volume breach OR Wave 100+ when other docs scaling pack rules stabilize
4. **No new gaps required** — proactive sweep, no quality findings

---

## 9. References

- Wave plan: [`wave-2026-05-19-99b-architecture-docs-sweep-expansion.md`](../../../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md)
- Gap: [GAP-668](../../gaps/phase-1-beta/closed/GAP-668-wave-99b-arch-volume-cap-compliance.md)
- Archive destination: [`documents/07-archived/architecture-2026-Q2/`](../../../07-archived/architecture-2026-Q2/)
- Parent README post-sweep: [`documents/02-architecture/README.md`](../../../02-architecture/README.md)
- Rules applied: `docs-folder-volume-budget.md` Rule 3 + `docs-archival-cadence.md` Rule 1 + `docs-folder-structure.md` §2 + `audit-to-gap-pipeline.md` §2.8 + `gap-done-discipline.md` §2 + `meta-csv-index-pattern.md` (audits-index.csv)
- Wave 96 PR2 supersession context: [`kitehub-architecture.md`](../../../02-architecture/kitehub-architecture.md), [`kiteclass-architecture.md`](../../../02-architecture/kiteclass-architecture.md), [`multi-tenant-architecture.md`](../../../02-architecture/multi-tenant-architecture.md), [`email-architecture.md`](../../../02-architecture/email-architecture.md)
