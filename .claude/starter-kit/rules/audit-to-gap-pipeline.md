# Audit → Gap → Fix Pipeline

**Priority:** 🟠 MANDATORY — audit findings governance
**Version:** 1.4.1
**Created:** 2026-04-16
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every audit run (UI /128, Quality /100, Security /100, Performance /100, API Contract /100, Ops Readiness /100, Business Logic /100), every wave plan drafting, every decision-doc PR (gap closure with config-shaped value, ADR, runbook with new domain/email/brand/env-var/region), and the gap files / fix PRs they produce

---

## 1. The Rule

> **Mọi issue từ audit PHẢI đi qua pipeline: Issue → Gap Check → Gap File → Memory → Fix PR**
> Không fix trực tiếp từ audit report. Không tạo gap duplicate. Không fix không có thứ tự.

---

## 2. Pipeline Steps

### Step 1: Issue Discovery (trong audit)

Audit output issue list. Mỗi issue có:
- ID (ví dụ: H-1, K-3)
- Severity (P0/P1/P2/P3)
- Screen/location
- Description

### Step 2: Duplicate Check (BẮT BUỘC trước tạo gap)

```bash
# Search existing gaps cho keyword liên quan
grep -rl "dark.mode\|404\|i18n\|mock" documents/04-quality/gaps/ | head -10
```

3 outcomes:
- **Exact duplicate** → link issue tới gap hiện tại, KHÔNG tạo mới
- **Related but different scope** → tạo gap mới, ghi "Related: GAP-XXX" 
- **Completely new** → tạo gap mới

### Step 2.5: State-Check Against Current Codebase (BẮT BUỘC trước tạo gap)

Step 2 only guards against **duplicate GAP files**. It does NOT detect when a gap proposes work already shipped as code. A gap filed against already-existing implementation wastes reviewer time and gets rewritten later (see 2026-04-20 GAP-190 / GAP-197 incident).

**Run code-state check before Step 3** — grep the actual paths the gap would touch:

```bash
# Frontend gap → check app routes + components
find {service}/src/app -type d -name "{topic}*"
grep -rl "{keyword}" {service}/src --include="*.tsx" --include="*.ts"

# Backend gap → check controllers + services + migrations
grep -rl "{keyword}" {service}/src/main/java --include="*.java"
ls {service}/src/main/resources/db/migration/ | grep -i "{topic}"

# Infra/CI gap → check workflows + scripts + hooks
ls -la .husky/ .github/workflows/
grep -l "{tool}" .github/workflows/*.yml

# Docs/runbook gap → check existing docs
find documents/05-guides documents/01-business -iname "*{topic}*"
```

Expected outcomes + how to proceed:

| Code state | Gap status to file | AC framing |
|-----------|-------------------|-----------|
| Nothing exists | 🔵 OPEN | Build-from-scratch |
| Partial implementation | 🟡 PARTIAL | Must include `## Current State (verified YYYY-MM-DD)` table listing what exists + what's missing; AC narrows to the delta |
| Fully implemented | SKIP filing — the gap is already DONE | Close the underlying concern by updating docs / existing gap, not by filing a new one |

**If filing 🟡 PARTIAL**, the gap file MUST contain:
- A `## Current State (verified YYYY-MM-DD)` section with file paths + line counts (or symbol names) as evidence
- A Log entry: "Scope revised after state-check. Found: ..."

**Anti-pattern detected 2026-04-20:** GAP-190 (SEO) and GAP-197 (attendance calendar) were filed without state-check; both had substantial implementations already (sitemap/robots/OG/JsonLd/blog MDX; enhanced-attendance-calendar 315 LOC PR 3.8.1). Both required follow-up rewrite PRs. Rule added to prevent recurrence.

**Recurrence 2026-05-04 (4th time):** GAP-345 K-12 LEGAL trio state-check audit + Wave 18b1 Bucket D found Wave 2 inline-fetch FE skeleton at `(dashboard)/parent/page.tsx` (159 LOC) that GAP-345 missed. Root cause: `head` truncation on grep + insufficient `find` depth. Hardened rule below.

### Hardened state-check protocol (post-2026-05-04 incident)

State-check **MUST NOT use `| head`** on `grep -rl` / `find` commands. Truncation hides existing implementations.

| ❌ Banned pattern | ✅ Required pattern |
|------------------|---------------------|
| `grep -rl "X" path/ \| head` | `grep -rl "X" path/` (read full output) |
| `find path/ -name "X*" \| head -10` | `find path/ -name "X*"` then count + sample |
| Single grep on entry name | Multiple greps: file name + class name + JSX selector + i18n key |
| Skipping `documents/` searches | Include `documents/04-quality/gaps/` to find prior gap files |

**Mandatory cross-checks:**
- For frontend gaps: `find {service}/src/app -type d` (full tree) + `grep -rl "{keyword}" {service}/src --include="*.tsx" --include="*.ts"` (no head)
- For backend gaps: `ls {service}/src/main/java/com/.../module/` (full module tree) + `grep -rl "Class.*{Topic}\|interface {Topic}" path/`
- For data layer: `ls {service}/src/main/resources/db/migration/` (FULL list, look for related V-prefixes) + `grep -l "{table_name}\|create table {topic}" db/migration/`
- For docs: `find documents -iname "*{topic}*"` (no head)

**Self-test if uncertain:** if the gap claims "fully greenfield" or "missing entirely," the agent MUST list the exact grep + find commands run AND the OUTPUT counts (e.g., "0 files found" or "3 files found, sampled, none match scope"). Inline these in `## Current State` section.

**Tracked recurrences:**
- 2026-04-20: GAP-190 (SEO), GAP-197 (attendance calendar) → PR #396 rewrite
- 2026-05-04 (3rd): GAP-345 audit revising GAP-321/322/323 → PR #757
- 2026-05-04 (4th): GAP-345 itself missed Wave 2 FE parent skeleton → Wave 18b1 Bucket D agent caught + flagged in PR #766. Hardened protocol added this section.
- 2026-05-04 (5th): Wave 18b3 plan §3 Bucket C referenced 3 absent symbols (`Incident.visibilityScope`, `BR-CHILD-PROTECT-005`, `Notification` entity) — agent caught at execution time + filed 3 sub-gaps. **5th recurrence escalation triggered → GAP-356 filed → this v1.2.0 rule extension below adds wave-plan pre-flight state-check (§2.6).**

If 6th recurrence detected, escalate to meta-rule audit (this rule's enforcement is failing despite both gap-filing protocol §2.5 AND wave-plan protocol §2.6 below).

### Step 2.6: Wave-Plan Pre-Flight State-Check (BẮT BUỘC trước merge wave plan PR)

**Why this exists:** Wave plans are higher-leverage than individual gaps (3-15× gap leverage — 1 plan governs 3-5 buckets × multiple days each). When a plan references absent entities/rules/migrations, the cost cascades: agents read plan as ground truth, execute against absent schema, recover at execution time (best case Wave 18b3 — agents caught it; worst case stalls until reviewer notices). Per `feedback_wave_plan_through_pr.md` wave plans merge BEFORE agent spawn — so the plan PR is the LAST checkpoint to catch absent-symbol references.

**Trigger:** any new file added under `documents/03-planning/waves/wave-*.md`.

**Required content in the wave plan:** a `## State-Check Evidence` section demonstrating that every code-symbol-shaped reference in §3 Scope has been verified present in the codebase. Use the table format from `documents/03-planning/waves/_TEMPLATE.md`.

**Symbols requiring verification:**

| Symbol type | Pattern (in backticks) | Required grep |
|-------------|------------------------|--------------|
| Java class / entity field | `` `ClassName.fieldName` `` or `` `ClassName.METHOD` `` | `grep -rn "ClassName" {service}/src/main/java` (no `\| head`) |
| Business rule ID | `` `BR-DOMAIN-NNN` `` | `grep -rn "BR-DOMAIN-NNN" documents/01-business/` |
| Flyway migration | `` `V[0-9]+__name.sql` `` | `ls {service}/src/main/resources/db/migration/V[0-9]+__name.sql` |
| Frontend component | `` `<ComponentName>` `` or `` `useHookName` `` | `grep -rn "ComponentName\|useHookName" {service}/src --include="*.tsx" --include="*.ts"` |
| Config key | `` `kite.foo.bar` `` | `grep -rn "kite.foo.bar" {service}/src/main/resources` |

**Forward-looking references (allowed exception):** symbols intentionally absent because the wave WILL CREATE them are allowed IF the State-Check Evidence row marks Verdict as `🆕 to-be-created` AND the Bucket explicitly owns the creation. Symbols referenced as if existing but absent → FAIL.

**Banned shortcuts (mirroring §2.5):**
- `| head` truncation on `grep -rl` / `find` commands
- Skipping verification "because the agent will check at execution time" — the whole point is to catch absent symbols BEFORE agent spawn
- Aspirational references ("we'll filter by `Incident.visibilityScope`") without a 🆕 to-be-created flag

**If pre-flight fails:** revise plan §3 Scope to either (a) drop the absent symbol, (b) reframe the bucket scope, or (c) flag the symbol as 🆕 to-be-created with explicit creation owner. Plan PR does not merge until State-Check Evidence section shows ✅/🆕 verdict for every symbol.

**Reference template:** `documents/03-planning/waves/_TEMPLATE.md` §State-Check Evidence.

**Detector:** `session-docs-check` Rule 16 (`scripts/check-docs.sh`) — fires on new wave plan files in diff; FAIL when symbol-shaped references in `## Scope` / `### Bucket` sections lack a corresponding `## State-Check Evidence` row OR the row's grep evidence is absent.

### Step 2.7: Decision-Doc Code-Sync (BẮT BUỘC trong cùng PR khi decision-doc thay đổi config-shaped value)

**Why this exists:** §2.5 covers state-check at gap-FILING (catch gaps proposing already-shipped work). §2.6 covers state-check at WAVE-PLANNING (catch plans referencing absent symbols). NEITHER covers the inverse direction: when a **decision document lands changing a config-shaped value**, are there stale code references that would silently drift?

Decision docs (gap files flipped DONE with a config decision, ADRs, runbooks, brand/policy guides) effectively introduce a new "ground truth" value. Code that references the OLD value becomes stale immediately on merge — but no rule forced a sync until something downstream broke. Per `incident-to-rule-pipeline.md` 5-stage applied to 2026-05-10 GAP-458→GAP-459 cascade (worked example below), this is rule-worthy: same class will recur on every brand/email/domain/env-var/region/payment-processor change.

**Trigger:** any PR that touches a "decision artifact" landing a NEW config-shaped value. Concretely:

| Artifact type | Examples |
|--------------|----------|
| Gap file flipped to 🟢 DONE that introduces a config decision | GAP-458 (`your-product-a.me` domain decision Path C) |
| ADR setting a new platform-wide value | ADR picking new payment processor / region / DB engine |
| Runbook referencing new domain/email/brand/environment | DNS runbook with new apex; email setup with new sender |
| Brand/policy guide changing user-facing identifier | Rename, rebrand, support-channel change |

**Required content in the decision-doc PR:** a `## Code-Sync Evidence` section (or equivalent prose in PR description) demonstrating that every code reference to the OLD value has been swept OR a follow-up sync gap is filed and linked.

**Config-shaped values requiring sweep:**

| Value class | Pattern | Required grep |
|-------------|---------|---------------|
| Domain (apex/subdomain) | `your-product-a.vn`, `your-product-b.vn`, `*.your-product-a.io` | `grep -rn "<old-domain>" your-product-a/ your-product-b/ infrastructure/ scripts/ documents/ --include="*.ts" --include="*.tsx" --include="*.java" --include="*.yml" --include="*.yaml" --include="*.tf" --include="*.sh"` (no `\| head`) |
| Email address | `support@<old>`, `dpo@<old>`, `noreply@<old>` | `grep -rn "@<old-domain>" <same scopes>` |
| Brand name (if rebranded) | `<your-product-a>` → `NewName` | `grep -rni "<old-brand>" <same scopes>` |
| Env var name | `OLD_VAR_NAME` | `grep -rn "OLD_VAR_NAME" <code+infra+helm+terraform>` |
| Cloud region | `ap-southeast-1` → other | `grep -rn "<old-region>" infrastructure/ .github/ helm/` |
| Payment processor / 3rd-party | `stripe` → `momo` | `grep -rn "<old-vendor>" <code scope>` |
| Cloud account ID | account renumber | `grep -rn "<old-account>" infrastructure/ documents/05-guides/` |

**Two valid outcomes per §3 acceptance criterion:**

1. **Sweep in same PR** — decision-doc PR includes the code edits replacing OLD → NEW (preferred when sweep is small ≤20 files OR the decision-doc author owns the affected code)
2. **Follow-up sync gap filed in same PR** — decision-doc PR includes a new gap file (or appends to existing gap) that explicitly tracks the code sync work as P0/P1, with grep evidence of the affected files

**Banned shortcuts (mirroring §2.5/§2.6):**

- `| head` truncation on grep — must read FULL output to surface ALL stale refs
- "We'll catch it next PR" without a follow-up gap — that's the failure mode this rule prevents
- Skipping sweep "because the decision is docs-only" — the decision IS the trigger; whether the doc itself touches code is irrelevant
- Sweeping ONLY the obvious files (FE source) — must include `infrastructure/`, `scripts/`, `documents/05-guides/runbooks`, helm values, terraform vars, .env.example, CI workflow files

**Forward-looking exception:** values intentionally absent from code at decision time (e.g., domain decision lands BEFORE FE code exists) are allowed IF the decision-doc PR explicitly cites the future code-creation as a tracked dependency (gap or wave plan with the code path scheduled).

**Detector:** deferred per `incident-to-rule-pipeline.md` §3 advisory-rule guard until 2nd recurrence (premature-rule guard ≥7 days). For v1.0.0, enforcement = §6.5-paired PR-template Output Review row + reviewer manual + worked self-test below.

**Reviewer-checklist line** (added to `.github/PULL_REQUEST_TEMPLATE.md` Output Review section same PR):
> - [ ] **Decision-doc code-sync** — if PR introduces or changes a config-shaped value (domain/brand/email/env-var/region/vendor/account-ID) in a gap-file/ADR/runbook, grep evidence shows zero stale refs in code+infra+scripts+helm+terraform OR a follow-up sync gap is filed and linked per `audit-to-gap-pipeline.md` §2.7

**Worked self-test — apply §2.7 retroactively to 2026-05-09 GAP-458 PR (#1084 predecessor):**

State at decision time:
- GAP-458 introduced `your-product-a.me` (Path C Free GitHub Student Pack) as Release 1 domain
- Decision-doc PR scope: 4 docs reflecting domain decision + ROADMAP update
- Code-sync evidence in PR: **none** — no `grep -rn "your-product-a.vn"` evidence; no follow-up sync gap filed
- Stale refs that existed at decision-time: 21 `your-product-a.vn` refs in `your-frontend-a/src/` (10 files) — verified post-hoc 2026-05-10 by GAP-459

Cost of the miss:
- AWS Activate Founder application denied 2026-05-10 (compute cover Phase 1 BETA ~10 tháng = real $1k)
- GAP-459 ~3h fix work + 2-week resubmit delay
- Drift-time = 1 day (2026-05-09 GAP-458 merge → 2026-05-10 denial caught)

Counterfactual with §2.7 applied at GAP-458 PR review:
- Reviewer asks "decision-doc code-sync evidence?" per checkbox
- Author runs `grep -rn "your-product-a.vn" your-product-a/ your-product-b/ infrastructure/ scripts/ documents/ --include="*.ts" --include="*.tsx" --include="*.java" --include="*.yml" --include="*.tf" --include="*.sh"` → 21 hits surfaced
- Two paths: (a) sweep in same PR (small ≤20 files, owner = author) OR (b) file follow-up sync gap with 21 hits as evidence
- AWS Activate denial eliminated; GAP-459 work eliminated

→ **Rule fires correctly on the originating incident. Self-test PASS.** ✅

### Step 2.8: Fix-Time State-Check (BẮT BUỘC trước khi propose solution cho gap >7 ngày tuổi hoặc drift-class)

**Why this exists:** §2.5/§2.6/§2.7 cover state-check tại **filing-time, planning-time, decision-doc-time**. NONE cover **fix-time** — khi agent/dev pick up existing gap để fix. Gaps mô tả symptom (drift, missing file, broken state) có thể **self-correct theo thời gian** giữa filed date và fix date — đặc biệt:

- Drift-class gaps (terraform state, repo state, dependency state) — runtime mechanisms (refresh, auto-merge, scheduled jobs) có thể đã sync state
- Tooling-state gaps (CI workflow, hook config) — có thể đã được fix bởi PR khác
- External-state gaps (API endpoints, DNS records, vendor approvals) — vendor có thể đã change

Fix proposed BÁM theo gap description mà KHÔNG state-check sẽ:
- Over-engineer solution cho non-existent problem
- Tốn effort + token vào PR không cần
- Discover only at execution-time mà symptom không còn — gây session pivot

**Trigger:** picking up gap để fix khi:
- Gap age ≥ 7 ngày từ Found date
- Gap class = drift / state-sync / tooling-state / external-state
- Gap mentioned in ROADMAP §🚀 Next Action (suggesting it's been queued)
- Gap blocked dependencies (waiting for credentials, infrastructure, vendor approval)

**Required content before drafting fix PR:**

0. **Canonical-status lookup first** — before any heavier check, query the CSV per `.claude/rules/gap-architecture-v2.md`: `bash scripts/query-gaps.sh <prefix>` returns the canonical (status, priority, phase, completion_pct, last_verified) row. If `last_verified` is recent and `status` already reflects current reality, the §2.8 fix-time state-check is satisfied — skip to step 3 Decision matrix. Querying CSV is ~50× cheaper than reading the gap file or grepping code.
1. **Empirical state-check** — when CSV is stale or symptom is system-state-dependent, run the verification commands the gap describes as symptom:
   - Drift gap → `terraform plan` (verify drift still present) + read state to confirm symptom
   - Repo-state gap → grep / find against current code
   - Tooling gap → run the tool that should be broken
   - External-state gap → API call / curl / DNS query
2. **Document findings** trong PR description hoặc comment trên gap before proposing solution:

```markdown
## Fix-time state-check (per audit-to-gap-pipeline.md §2.8)

Gap age: <N> days since Found YYYY-MM-DD.
Symptom claim: <copy from gap §Problem>
Verification commands run: <list>
Result: <empirical findings — drift still present / drift self-corrected / etc.>
Decision: <proceed with fix / flip DONE without fix / scope-revise / etc.>
```

3. **Decision matrix:**

| State-check result | Action |
|---|---|
| Symptom verified present | Proceed with proposed fix |
| Symptom no longer present (self-corrected) | Flip gap to 🟢 DONE với findings; NO fix PR needed |
| Symptom partially present (sub-set drifted) | Scope-revise fix to actual drift; document delta |
| Symptom diagnostic was wrong from filing | Edit gap §Problem để correct; flag as gap-quality issue |

**Banned shortcuts:**
- "Trust the gap description because it was filed by an experienced dev"
- "State-check at execution time is good enough" (no — execution-time pivot costs already-shipped PR effort)
- "Gap is small, state-check overhead > fix effort" (small gaps still benefit; 5-min `terraform plan` saves 1.5h PR + retro)

**Forward-looking exception:** gap với explicit deferred-execution semantics (e.g., "execute when AWS Activate D+14 unlocks") doesn't need fix-time state-check UNTIL the dependency unlocks. Reference dependency in gap; deferred state stays valid until trigger.

**Detector:** deferred per `incident-to-rule-pipeline.md` §3 advisory-rule guard (premature-rule guard ≥7 ngày). For v1.3.0 enforcement = §6.5-paired memory `feedback_gap_state_check_required.md` extension + worked self-test + reviewer-checklist line. After 2nd recurrence, file follow-up gap to wire detector (e.g., scan gap pick-up via session-transcript hook).

**Reviewer-checklist line:** when reviewing fix PR for gap >7 ngày tuổi OR drift-class:

> Did the fix PR description include "Fix-time state-check" section per §2.8 với empirical verification commands + findings? If no → ask author to verify symptom still present before proceeding.

**Worked self-test — apply §2.8 retroactively to 2026-05-11 GAP-450 session:**

State at fix pick-up (2026-05-11):
- GAP-450 Found 2026-05-08 — 3 ngày tuổi (boundary, but drift-class)
- Symptom claim: "Terraform state shows id='none' + recurring 'will be updated in-place'"
- §2.8 trigger: gap age + drift-class + mentioned in ROADMAP §🚀 Wave 60 candidates (f)

Required state-check BEFORE proposing fix (Path A/B/C):

```bash
# Verify drift still present
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 terraform plan -out=tfplan
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 terraform show -json tfplan \
  | jq '.resource_changes[] | select(.address | startswith("random_password.")) | {address, actions: .change.actions, before_id: .change.before.id, after_id: .change.after.id}'
```

Expected output (per actual 2026-05-11 investigation findings):
- `actions: ["update"]` but `before == after` for all 3 random_password → phantom plan, no real drift
- `aws_secretsmanager_secret_version.*` similar — id + version_stages + length match

**Decision per §2.8 matrix**: "Symptom no longer present (self-corrected)" → Flip gap to 🟢 DONE với findings; NO fix PR needed.

**Cost saved if §2.8 had been applied at fix-time:**
- PR #1154 (Option B lifecycle ignore_changes + 253-line runbook + audit artifact) — ~1.5h effort
- ~10-15k tokens conversation + tool calls
- Pre-flight infra stop (EC2 + RDS, ~13 min wait)

**Actual cost without §2.8:**
- PR #1154 shipped (Option B retains future-proofing value; runbook value reduced)
- PR #1155 (DONE flip post-investigation) shipped same day

→ **Rule fires correctly on the originating incident.** Self-test PASS — rule would have caught the miss at step 2 instead of step 5. ✅

### Step 3: Gap File Creation

Format chuẩn cho gap từ audit:

```markdown
# GAP-XXX: [Title]

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 / 🟠 P1 / 🟡 P2 / 🟢 P3
**Domain:** [Frontend / Backend / DevOps / ...]
**Found:** [date] ([audit type] audit)
**Affects:** [scope — pages, services, users]

## Problem
[Mô tả issue từ audit, kèm evidence: scores, screenshots, file sizes]

## Root Cause
[Phân tích nguyên nhân, hoặc "Cần investigate"]

## Proposed Fix
[Steps cụ thể]

## Acceptance Criteria
- [ ] [Measurable criteria]

## Related
- [Link tới audit report]
- [Link tới gaps liên quan]
- [Link tới existing fix attempts]
```

### Step 4: Memory Update

Sau khi tạo gaps, save memories cho:

| Memory type | Khi nào | Ví dụ |
|-------------|---------|-------|
| **feedback** | Pattern lặp lại cần tránh | "Port 3000 bị chiếm → luôn verify trước capture" |
| **project** | Quyết định ảnh hưởng roadmap | "Dark mode <your-product-a> chưa implement, cần thêm vào wave" |

**KHÔNG save memory cho:**
- Issue details (đã có trong gap file)
- Fix steps (sẽ có trong PR)
- Scores (đã có trong audit report)

### Step 5: Update ROADMAP (BẮT BUỘC)

Sau khi tạo gap files, PHẢI update `documents/04-quality/gaps/ROADMAP.md`:

1. **Assign epic** — gap thuộc epic nào? Tạo epic mới nếu cần.
2. **Assign sprint** — gap nên fix trong sprint nào? Dựa trên priority + dependencies.
3. **Update counts** — tổng số gaps trong epic heading.
4. **Update dependency graph** — nếu gap mới block hoặc bị block bởi gap khác.

**KHÔNG được tạo gap mà không update ROADMAP.** Gap không có trong ROADMAP = gap bị quên.

### Step 6: Fix Priority & Ordering

**Meta-boost first:** trước khi áp dụng thứ tự dưới, apply `meta-gap-priority.md` — gaps về skills/rules/workflow luôn đi trước feature gaps cùng P-level. Xem `.claude/rules/meta-gap-priority.md` §3 cho priority matrix đầy đủ.

Sau khi meta-boost áp dụng, fix gaps theo thứ tự:

```
1. P0 blockers (chặn audit/deploy/CI) — meta gaps trước feature gaps
2. P0 → P1 có dependency chain (fix A trước mới fix B được)
3. P1 independent (fix song song) — meta gaps trước feature gaps
4. P2 batch (gom nhiều P2 vào 1 PR)
5. P3 opportunistic (fix khi đụng file liên quan)
```

**Dependency rules:**
- Capture-tool bugs fix TRƯỚC content bugs (vì cần re-capture sau fix)
- Mock data gaps fix TRƯỚC UI scoring gaps (vì scores phụ thuộc content)
- Infrastructure fix TRƯỚC feature fix

---

## 3. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Fix issue trực tiếp trong audit session | Tạo gap file → fix trong PR riêng |
| Tạo gap mà không check duplicate | `grep` existing gaps trước |
| Fix P2 trước P0 | Respect priority + dependency order |
| Tạo 1 gap cho 5 issues khác nhau | 1 gap = 1 issue rõ ràng |
| Gom tất cả fixes vào 1 PR khổng lồ | Group by domain/priority, max 3-5 gaps per PR |
| Save mọi issue detail vào memory | Memory = patterns + decisions, gaps = details |

---

## 4. Mapping Audit Types → Gap Naming

| Audit | Gap prefix suggestion | Example |
|-------|----------------------|---------|
| UI Review /128 | GAP-XXX-{app}-{screen}-{issue} | GAP-076-your-product-a-capture-mock-auth |
| Quality Audit /100 | GAP-XXX-{category}-{issue} | GAP-049-business-correctness |
| Security Audit /100 | GAP-XXX-{owasp/category}-{issue} | GAP-041-svg-xss-protection |
| Performance Audit /100 | GAP-XXX-{area}-{issue} | GAP-XXX-n-plus-one-queries |
| API Contract /100 | GAP-XXX-{service}-{issue} | GAP-XXX-endpoint-undocumented |
| Ops Readiness /100 | GAP-XXX-{area}-{issue} | GAP-XXX-missing-health-probes |

---

## 5. Integration

- **CLAUDE.md** references this rule
- **Audit skills** output issue list → trigger this pipeline
- **gap-to-pr-converter** skill consumes gap files → generates PR
- **wave-completion-check** verifies all gaps in wave are DONE

---

## 6. Log

- **2026-05-11** (v1.4.1): PATCH — added §2.8 step 0 "Canonical-status lookup first" recommending `bash scripts/query-gaps.sh <prefix>` against `gap-status.csv` before heavier state-check. Reduces token cost ~50× per session per `gap-architecture-v2.md` token analysis. Closes the last remaining Phase 4 follow-up from `gap-architecture-v2.md` §10 for this rule. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — additive efficiency note, no constraint change).

- **2026-05-11** (v1.4.0): MINOR — added §2.8 Fix-Time State-Check extending state-check family từ filing-time (§2.5) + planning-time (§2.6) + decision-doc-time (§2.7) to **fix pick-up time**. Triggered by 2026-05-11 user-flagged retro after GAP-450 fix session: GAP-450 filed 2026-05-08 mô tả terraform state drift; fix session 2026-05-11 (3 ngày sau) proposed Option A surgery via PR #1154 (Path B+C combined: lifecycle ignore_changes + runbook + audit artifact, ~1.5h effort) **TRƯỚC KHI** state-check confirm symptom còn tồn tại. Phase 1 investigation post-PR-#1154 với dev-admin credentials revealed: state đã self-correct via terraform refresh runs over 3 ngày → Option A surgery is no-op. Cost: ~1.5h PR + ~10-15k tokens + EC2/RDS stop wait — preventable nếu state-check tại fix pick-up. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "lần fix gaps này chưa state-check trước khi tốn token để đưa ra solution đúng không?") → Classify ✓ (no existing rule covers fix-time direction; §2.5 covers filing-time, §2.6 wave-planning, §2.7 decision-doc — all input-time checks; fix pick-up was uncovered) → Rule+Enforce ✓ (this §2.8 + paired same-PR memory `feedback_gap_state_check_required.md` extension + worked self-test on GAP-450 incident + reviewer-checklist line per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§2.8 worked example on 2026-05-11 GAP-450 — rule fires correctly + would have caught miss at step 2 instead of step 5) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds new constraint covering previously-uncovered direction of state-check; no constraint loosening for prior work; existing fix PRs grandfathered, rule applies prospectively cho gaps ≥ 7 ngày tuổi OR drift-class từ session sau). Detector wiring deferred to 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; v1.4.0 enforcement = reviewer-checklist + memory auto-load + worked self-test sufficient.
- **2026-05-10** (v1.3.0): MINOR — added §2.7 Decision-Doc Code-Sync state-check extending state-check family from gap-filing (§2.5) + wave-planning (§2.6) to **decision-doc landing time**. Triggered by 2026-05-10 user-flagged retro after GAP-459 fix session: GAP-458 (`your-product-a.me` domain decision Path C, merged 2026-05-09) shipped without sweeping 21 stale `your-product-a.vn` refs in `your-frontend-a/src/` → AWS Activate Founder application denied next day with reason "Your website cannot be accessed or fails to load" → GAP-459 filed → ~3h fix + 2-week resubmit delay + real $1k credit at risk. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "is a meta update necessary after this gap fix experience?") → Classify ✓ (no existing rule covers decision-doc → code-sync direction; §2.5 covers filing-time, §2.6 covers planning-time, but inverse case "decision lands → grep stale code refs" was uncovered) → Rule+Enforce ✓ (this §2.7 + paired same-PR PR-template Output Review checkbox + memory `feedback_decision_doc_code_sync.md` + memory `feedback_nextjs_dynamic_loading_ssr.md` (Class C insight) per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§2.7 worked example on 2026-05-09 GAP-458 PR — rule fires correctly + 21 stale refs surfaced + counterfactual shows AWS Activate denial eliminated) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds new constraint covering previously-uncovered direction of state-check; no constraint loosening for prior work; existing decision docs grandfathered, rule applies prospectively; class includes domain/brand/email/env-var/region/vendor/account-ID rename — all would hit same pattern). Detector wiring deferred to 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days; v1.0.0 enforcement = PR-template checkbox + reviewer manual + memory auto-load + worked self-test sufficient.
- **2026-05-05** (v1.2.0): MINOR — added §2.6 Wave-Plan Pre-Flight State-Check Protocol extending state-check from gap-filing to wave-plan drafting. Triggered by 5th GAP-190/197 head-truncation recurrence (Wave 18b3 plan §3 Bucket C referenced 3 absent symbols `Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification` entity; agent caught at execution time + filed 3 sub-gaps GAP-321b.1-trio). Per self-mandated 5th-recurrence escalation clause: file gap on rule itself (GAP-356 filed 2026-05-05) → ship rule extension. Paired same-PR per `rule-change-process.md` §6.5 with: `session-docs-check` Rule 16 detector + 3-fixture self-test (good-flip / bad-absent-symbol / forward-flag-allowed) + `documents/03-planning/waves/_TEMPLATE.md` State-Check Evidence section + `feedback_wave_plan_state_check.md` memory + cross-link updates. Recurrence list updated inline (5th entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds new constraint covering previously-uncovered higher-leverage artifact, no constraint loosening for prior work).
- **2026-05-04** (v1.1.0): MINOR — added "Hardened state-check protocol" subsection to Step 2.5 banning `| head` truncation on grep/find during state-check. Triggered by 4th recurrence: GAP-345 K-12 LEGAL audit itself missed Wave 2 inline-fetch FE skeleton (159 LOC) at `(dashboard)/parent/page.tsx`; Wave 18b1 Bucket D agent caught at execution time + flagged in PR #766. Per `rule-change-process.md` §5 MINOR self-approve solo-dev — adds enforcement detail to existing rule, no constraint loosening. Recurrence list now tracked inline; 5th recurrence escalates to gap on this rule.
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill per GAP-249 — added Last-Reviewed + Reviewer-Approver + Applies-to fields; reformatted existing Version `1.0` → `1.0.0` (semver three-part canonical). No content change. Solo-dev PATCH self-approve per `rule-change-process.md` §5.
- 2026-04-20 — Added **Step 2.5 State-Check Against Current Codebase** after GAP-190 (SEO) + GAP-197 (attendance calendar) were filed without code-state verification; both required follow-up rewrite (PR #396). User feedback: "gaps phải dựa trên tình trạng của hệ thống hiện tại". Step 2.5 is BẮT BUỘC alongside Step 2 — dedupe alone is insufficient.
- 2026-04-16 — Rule created after UI audit session produced 5 gaps; user requested formalization
