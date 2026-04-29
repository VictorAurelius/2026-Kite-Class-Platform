---
title: Wave Legal-BRD Phase 1 — 4 skeleton policy docs (TOS / AUP / Privacy / Retention)
status: complete
created: 2026-04-29
updated: 2026-04-29
shipped: 2026-04-29
gaps: [GAP-180, GAP-181, GAP-182, GAP-184]
deferred_to_next_wave: [GAP-183, GAP-185, GAP-186]
deferred_separate_track: []
umbrella: GAP-154
prs: [687, 689, 688, 691, 690]
total_loc: 1326
wall_clock_min: 35
---

# Wave Legal-BRD Phase 1 — Cluster Pack 13 (5th business-correctness slice)

**Wave date:** 2026-04-29 (kicked off this session)
**Cluster theme:** Phase 1 skeleton policy docs cho 4 P0 Business-Logic legal mandates. Phase 2 (legal counsel content + sign-off) defer qua GAP-154 umbrella.
**Strategy reference:** `meta-gap-priority.md` §3 — Business-Logic-P0 ranks above Feature-P0; sister cluster của Wave Business Correctness 2026-04-29 (GAP-049/050/150 closed). PDPL Decree 13/2023/NĐ-CP có hiệu lực 2026-07-01 → GAP-182 critical path.

## Why this wave

- 4 disjoint OPEN P0 BL gaps cùng theme `00-brd/` policy skeleton — perfect wave-pack candidate
- Phase 1 = pure docs work (skeleton + section structure + cross-refs + TODO markers) → mỗi agent ~30 min
- Phase 2 (legal counsel content) blocked-on stakeholder engagement → Phase 1 ship trước để unblock TOS/Privacy URLs cho VNPay onboarding + payment processor due diligence
- GAP-182 Privacy mandate `Decree 13/2023/NĐ-CP` Article 11 — luật bắt buộc, không phải nice-to-have
- 5-attribute review per `business-logic-review.md` v1.0.0 §2 không apply cho skeleton placeholder values; nhưng GAP-184 retention matrix có values (30/180 days, 5/10 years) — agents PHẢI mark TODO + `informed gut Q3 2026 review` cho mọi placeholder

## Scope

| # | Gap | Title | Priority | Agent | Disjoint files |
|:-:|-----|-------|:--------:|:-----:|----------------|
| 1 | **GAP-180** | Terms of Service (15 sections) | 🔴 P0 BL | A | `documents/00-brd/terms-of-service.md` (NEW) |
| 2 | **GAP-181** | Acceptable Use Policy (8 sections) | 🔴 P0 BL | B | `documents/00-brd/acceptable-use-policy.md` (NEW) |
| 3 | **GAP-182** | Privacy Policy — VN PDPL + GDPR (16 sections) | 🔴 P0 BL legal mandate | C | `documents/00-brd/privacy-policy.md` (NEW) |
| 4 | **GAP-184** | Data Retention + Deletion Policy (8 sections + matrix) | 🔴 P0 BL PDPL Art 6 | D | `documents/00-brd/data-retention-deletion-policy.md` (NEW) |

## Deferred (next wave — extend Wave 8 Business Governance)

- **GAP-183** — Refund/Dispute Resolution Policy. Defer chỉ vì 4-agent là sweet-spot per `feedback_parallel_agent_strategy.md` rule #9 (max-cap 4 background agents); 7-doc full slice over-budget single wave.
- **GAP-185** — Billing Terms + VAT/TCT Compliance. Same reason; cũng cần Tax advisor review trong Phase 2.
- **GAP-186** — Child Protection Policy. Same reason; cross-cuts với GAP-182 (minor data) + GAP-181 (CSAM zero-tolerance) — better to file sau khi 2 gap đó skeleton xong để tránh forward-reference broken links.

## Deferred (separate track)

- Phase 2 content fill (legal counsel engagement) → GAP-154 umbrella tracks. Mỗi gap Phase 2 sẽ file riêng khi legal counsel engaged (per `gap-done-discipline.md` §3 PARTIAL exit-ramp).

## File overlap analysis

Run via `bash .claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-180 GAP-181 GAP-182 GAP-184`.

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/00-brd/terms-of-service.md` (NEW) | A only | None |
| `documents/00-brd/acceptable-use-policy.md` (NEW) | B only | None |
| `documents/00-brd/privacy-policy.md` (NEW) | C only | None |
| `documents/00-brd/data-retention-deletion-policy.md` (NEW) | D only | None |
| `documents/00-brd/README.md` | foundation only | None — coordinator owns |
| `.claude/rules/meta-gap-priority.md` | A,B,C,D (read-only citation) | **SOFT** — read-only, no edit |
| `documents/04-quality/audits/business/brd-simulation-gap-finder-2026-04-20.md` | A (read-only) | None |
| `05-guides/` | (false-positive — Out-of-Scope section parsed) | **N/A** — Phase 2 only |

Net: **0 HARD, 1 SOFT (read-only citation)**. False-positive HARD `05-guides/` từ script reading "Out of Scope" sections; agents skip per "do NOT touch anything else" constraint.

**Mitigation:** foundation PR ships `00-brd/README.md` directory map updates centrally → agents KHÔNG touch README.

## Agent workflow

Per `feedback_parallel_agent_strategy.md` + `feedback_worktree_absolute_path_contamination.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off main (after this foundation PR merges)
3. Agent verify cwd: `pwd | grep -q "\.claude/worktrees/" || abort`
4. Commits + creates own PR — branch naming: `feat/wave-legal-brd-gap-{180|181|182|184}-skeleton`
5. Reports back PR number + cross-link verification + frontmatter check
6. Coordinator merges sequentially: A → B → C → D
7. Conflict resolution: none expected (4 separate NEW files)
8. Wave closure ROADMAP entry after all 4 merge
9. Status flip 🔵 OPEN → 🟡 PARTIAL on closure PR (NOT DONE — Phase 2 content blocked-on legal counsel per `gap-done-discipline.md` §3)

## Acceptance criteria (wave-level)

- [ ] 4 PRs merged (one per gap) with green CI
- [ ] All 4 gap files transitioned 🔵 OPEN → 🟡 PARTIAL với Log entry citing Phase 1 done + Phase 2 blocked-on legal counsel + GAP-154 umbrella
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry (counts -4 PARTIAL added)
- [ ] `documents/00-brd/README.md` directory map shows 4 new rows với status `skeleton`
- [ ] No conflicts left unresolved on main
- [ ] Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `data/wave-history.jsonl` entry appended với wall-clock + lessons
- [ ] MEMORY entry chỉ khi pattern mới (skeleton-only Phase-1 cluster pattern là đủ recurring để codify)

## Wall-clock target

- Foundation PR (this doc + ROADMAP entry + README directory map): ~15-20 min
- 4 parallel agents: ~30-40 min wall (each ~25-35 min agent-time, parallel)
- Sequential merge + verify (no conflicts expected): ~15-20 min
- Closure (ROADMAP + cleanup + retrospective): ~15-20 min
- **Total wave: ~75-100 min**

## Per-agent skeleton requirements

### Agent A — GAP-180 Terms of Service

Sections required (per gap §Scope):
1. Parties + Definitions (KiteClass/KiteHub provider vs Customer-tenant vs End Users teacher/student/parent)
2. Service Description (scope, tiers, exclusions)
3. Customer Obligations (content, security, lawful use, data accuracy)
4. Provider Obligations (uptime → GAP-189 SLA, support, security)
5. Acceptable Use → link GAP-181
6. Intellectual Property (customer data ownership, provider IP, feedback license)
7. Payment Terms → link GAP-185
8. Confidentiality + Data Protection → link GAP-182
9. Term + Termination (cancellation, suspension, data handling → GAP-184)
10. Warranties + Disclaimers
11. Limitation of Liability
12. Indemnification
13. Dispute Resolution → link GAP-183, jurisdiction VN, ADR
14. Modifications (notice period, acceptance mechanism)
15. Entire Agreement + Severability + Governing Law (VN law, TAND jurisdiction)

Frontmatter: `status: skeleton`, `owner: Legal`, `reviewer: PM + CEO`, `last_updated: 2026-04-29`, `tracking: GAP-180 (Phase 1) → GAP-154 umbrella (Phase 2 content)`.

### Agent B — GAP-181 Acceptable Use Policy

Sections required (8):
1. Scope + Acceptance (applies to tenant + end user)
2. Prohibited Content (illegal/CSAM/hate/adult/copyright/misinfo per VN Cybersecurity Law + Criminal Code)
3. Prohibited Conduct (account sharing, bot, scraping, reverse engineering, spam)
4. Education-Specific Prohibitions (academic fraud, leaked exams, teacher impersonation, predatory behavior toward minors → GAP-186)
5. Enforcement (warning system, suspension tiers, content removal, appeal)
6. Reporting (user violation reporting, SLA per severity)
7. Platform Response Time (SLA per severity)
8. Cooperation with Authorities (when/how disclose to MOET/police/courts)

**Required tables:**
- Prohibited content matrix (content type × platform response)
- Strike/suspension tier table
- Appeal flow skeleton

Frontmatter: `status: skeleton`, `owner: Legal + Trust & Safety`, `tracking: GAP-181`.

### Agent C — GAP-182 Privacy Policy (PDPL mandate)

Sections required (16, mandatory per VN PDPL Decree 13/2023):
1. Data Controller Identity
2. Data Protection Officer (DPO) — placeholder
3. Data Subject Categories (admin/teacher/student/parent/accountant)
4. Data Categories Processed (identification/contact/educational/financial/technical/sensitive)
5. Processing Purposes
6. Legal Basis (contract/consent/legal-obligation/legitimate-interest)
7. Data Sharing (VNPay/MoMo/Zalo/Google Workspace/hosting; no selling)
8. Cross-Border Transfer (Ollama local vs OpenAI international)
9. Retention Period → GAP-184
10. Data Subject Rights (PDPL Art 11: know/access/rectify/erasure/restrict/object/portability/complaint to MPS A05)
11. Exercising Rights (contact, response SLA 20-30 days per VN PDPL)
12. Minor Data (<16 tuổi VN; parental consent → GAP-186)
13. Security Measures (encryption, access control, audit)
14. Breach Notification (72 hours per VN PDPL → GAP-190 Incident Response)
15. Cookie Policy (session, analytics, consent)
16. Changes (notice period, re-consent)

**Required tables:**
- Data category matrix (category × purpose × legal basis × retention)
- Data subject rights table (right × how to exercise × response SLA)

Frontmatter: `status: skeleton`, `owner: Legal + DPO`, `legal_basis: VN PDPL Decree 13/2023, GDPR Art 13-14`, `tracking: GAP-182`.

### Agent D — GAP-184 Data Retention + Deletion Policy

Sections required (8 + retention matrix):
1. Retention Categories + Periods (matrix table)
2. Deletion Triggers (subject request PDPL Art 11, retention expiry, tenant termination, legal hold release)
3. Deletion Process (soft → hard timeline, anonymization, backup purge, search index, cache invalidation)
4. Legal Hold (disputes/investigations/regulatory; override retention clock; approval chain)
5. Exceptions (aggregated/anonymized analytics, legal archives MOET/Tax)
6. Tenant Offboarding Runbook (skeleton — full SOP in 05-guides/ Phase 2)
7. Subject Erasure Request Runbook (skeleton)
8. Audit Trail of Deletions (what/when/why/by whom)

**Required matrix (Phase 1 placeholder values với TODO markers):**

| Data Category | Active retention | Post-termination | Legal basis | Config key | Phase 2 review |
|---|:-:|:-:|---|---|:-:|
| User accounts | While active | TODO 30d | Contract | `retention.user-account.days` | informed gut Q3 2026 |
| Educational records | While active | TODO 5y | MOET Education Law | `retention.edu-records.years` | informed gut Q3 2026 |
| Financial records | While active | TODO 10y | VN Tax Law | `retention.financial.years` | informed gut Q3 2026 |
| Audit logs | TODO 1y | TODO 1y | Cybersecurity Law | `retention.audit-log.days` | informed gut Q3 2026 |
| Marketing consent | While active | TODO 3y | PDPL | `retention.marketing-consent.years` | informed gut Q3 2026 |
| AI generation outputs | While active | TODO 30d | Service contract | `retention.ai-output.days` | informed gut Q3 2026 |
| Support tickets | TODO 2y | TODO 2y | Consumer Law | `retention.support.years` | informed gut Q3 2026 |
| Parent comm logs | TODO 2y | TODO 1y | PDPL | `retention.comm-logs.years` | informed gut Q3 2026 |
| Student sensitive | While enrolled | TODO ≤6mo | PDPL minor | `retention.sensitive-minor.months` | informed gut Q3 2026 |

Frontmatter: `status: skeleton`, `owner: Legal + Engineering Lead`, `legal_basis: VN PDPL Art 6, GDPR Art 5(1)(e)`, `tracking: GAP-184 → GAP-108 (storage hardcoded)`.

## Per-agent constraints (enforced)

All 4 agents MUST:
- Path constraint: chỉ `documents/00-brd/<their-file>.md` — KHÔNG touch README, KHÔNG touch sibling skeleton files
- Frontmatter standard: copy from existing `documents/00-brd/personas-catalog.md` style (markdown-header, không YAML)
- Cross-link verify: every `[GAP-XXX](...)` link resolve được — test với `ls documents/04-quality/gaps/GAP-XXX*.md`
- Cross-link to docs/laws ngoài repo: cite section + decree number, không hyperlink (ngoài tầm)
- Frontmatter required fields: status, owner, legal_basis (nếu áp dụng), tracking, last_updated
- Vietnamese prose default; English cho legal terms (per CLAUDE.md communication rule)
- KHÔNG flip GAP-XXX Status to DONE — coordinator handle status flip per `gap-done-discipline.md`
- Worktree verify: `pwd | grep -q "\.claude/worktrees/" && git branch --show-current | grep -q "^feat/wave-legal-brd-"` trước Write/Edit
- RELATIVE paths only trong commands (per `feedback_worktree_absolute_path_contamination.md`)

## Lessons-learned (Wave Legal-BRD Phase 1, completed 2026-04-29)

### Worktree isolation
- [x] `isolation: "worktree"` held — 4 separate checkouts at `.claude/worktrees/agent-{aab9bbc0|acc3af30|afa795ee|ad409d25}/`
- [x] No cross-agent file collisions (4 disjoint NEW files)
- [⚠️] **Worktree-contamination on Agent C** (caught pre-commit) — Write tool initially landed `privacy-policy.md` at main worktree path (`/home/.../2026-Kite-Class-Platform/documents/...`) instead of agent worktree (`.claude/worktrees/agent-afa795ee/...`). Agent verified on first read grep, copied to correct path, removed stray, committed cleanly. Per `feedback_worktree_absolute_path_contamination.md` documented hazard — pattern surfaces despite prompt mandate. Mitigation candidate: hook to detect Write to absolute path outside `pwd` at the tool level.

### File overlap accuracy
- [x] Predicted: 0 HARD, 1 SOFT (read-only `meta-gap-priority.md` citation). Actual: 0 HARD, 0 SOFT (citations did not edit). False-positive HARD on `05-guides/` from script reading "Out of Scope" sections — confirmed false-positive (no agent touched `05-guides/`).

### Wall-clock variance
- **Estimated:** 75-100 min total (foundation 20 + parallel 30-40 + merge 15 + closure 15)
- **Actual:** ~35 min total (foundation 15 + parallel 5.7 wall + sequential merge 5 + closure 10)
- **Variance source:** agents finished much faster than 25-35 min estimate (avg 5.0 min). Skeleton-only docs work scales with agent prompt clarity not section count — deterministic, not multi-step decision tree. Re-calibration: docs-only-skeleton ~5 min/agent + sequential merge ~3 min/PR + closure ~10 min.

### Pattern reuse
- [x] **Skeleton-only Phase-1 cluster pattern is recurring** — 2nd instance after Wave Business Correctness 2026-04-29 (5 BRD skeleton via 3-agent pattern). 3rd recurrence triggers codification as agent template variant `docs-only-skeleton-agent.md`. Track: GAP-186 + GAP-189 + future BRD skeletons.
- [x] **4-doc cluster size optimal** — `feedback_parallel_agent_strategy.md` rule #9 max-cap 4 background agents. 5+ would over-budget single-message Agent calls. 3-doc would under-utilize parallelism. 4 = sweet-spot validated.

### Agent template effectiveness
- [x] `docs-only-agent.md` template held without adjustment — all 4 agents 0-clarification-round (15th-18th consecutive). Frontmatter style choice (markdown-header mimicking `personas-catalog.md`) consistent across 4 files.

### CI behavior
- [x] CI did NOT trigger for pure `documents/00-brd/*.md` agent PRs (workflow path filters don't match). Foundation PR #687 had 6 checks (touched ROADMAP + wave plan path) but agent PRs had 0 checks rendered. `mergeStateStatus=CLEAN` sufficient — do not block waiting for non-existent CI.

### Local-state hazards (4-agent waves)
- [⚠️] **Local main glitch during PR #691 merge:** `gh pr merge --squash --delete-branch` post-merge checkout failed with "fatal: 'main' is already used by worktree" because 4 agent worktrees still on detached HEADs of merged branches. Recovery via `git fetch && git reset --hard origin/main`. **New rule for 4+-agent waves:** prune worktrees BEFORE final merge of last PR, OR accept local stale state until cleanup task. Document in `retrospective-checklist.md` as recurring pattern.

### Phase 2 TODO marker pattern
- [x] `<!-- Phase 2: ... — informed gut Q3 2026, GAP-154 -->` inline marker works as both placeholder + retrospect anchor. Aligns with `business-logic-review.md` §2.1 "informed gut" Source category + quarterly re-review obligation. Reusable for future Phase-1 skeleton waves.

### Token cost
- Foundation: ~33K tokens (wave plan + README + ROADMAP)
- Agents combined: ~793K tokens (4 agents avg ~198K each)
- Closure: ~25K tokens
- **Total: ~850K** (vs Wave UI Kits R2 1.05M, Wave Review Process Improvement 850K) — comparable scale despite 4x agents.
