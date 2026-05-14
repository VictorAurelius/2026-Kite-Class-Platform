# Docs-Only PR Auto-Merge — skip "check CI? merge?" prompts

**Priority:** 🟠 MANDATORY — workflow friction reduction
**Version:** 1.0.0
**Created:** 2026-05-11
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every PR Claude creates whose diff is contained within the §2 docs-only scope, AND CI passes all required checks

---

## 1. The Rule

> **Sau khi tạo PR docs-only và CI báo all green, Claude PHẢI auto-merge với squash + branch cleanup trong một bước, KHÔNG hỏi user "check CI?" hoặc "merge?".** User vẫn có thể hold bằng `[do-not-auto-merge]` label hoặc trailer `DOCS_AUTO_MERGE_HOLD:` hoặc bằng cách nói "wait/don't merge" trong cùng turn.

Rule này sharpens `agent-action-bias.md` §1 Part A ("do it yourself") cho trường hợp docs-only PRs: blast radius cực thấp + CI là gate đầy đủ, do đó hỏi xác nhận hai lần (1× CI check, 1× merge) là friction không cần thiết. User đã ngầm approve qua việc accept PR creation; CI green là exit gate.

---

## 2. Docs-only scope (in)

PR diff PHẢI ONLY chạm các path dưới đây:

| Path pattern | Examples |
|---|---|
| `documents/**` | Tất cả docs (business, architecture, planning, quality, guides, diagrams, archived, thesis) |
| `*.md` repo root | README.md, CHANGELOG.md, ROADMAP.md |
| `.claude/rules/**/*.md` | Rule docs (governance) |
| `.claude/skills/**/*.md` | Skill SKILL.md + reference docs |
| `.claude/skills/**/data/**` | Skill data files (logs, configs) |
| `.claude/skills/**/assets/**` | Skill assets (templates) |
| `.claude/starter-kit/**/*.md` | Starter-kit docs |
| `.env.*.template` | Env templates với placeholder values only (no secrets) |

---

## 3. Out of scope (auto-merge BANNED — fall back to "check CI? merge?" pattern)

ANY of these in PR diff → rule N/A → manual confirmation per default workflow:

| Path / change type | Why excluded |
|---|---|
| `.github/workflows/*.yml` | CI logic change — needs scrutiny |
| `.github/PULL_REQUEST_TEMPLATE.md` | Template change affects future PRs |
| `.husky/**` | Pre-commit hooks — execution path |
| `scripts/**/*.sh` / `scripts/**/*.py` | Executable scripts |
| `.claude/skills/**/scripts/**` | Skill scripts (executable) |
| `.claude/hooks/**` | Hook code (executable) |
| `pom.xml`, `package.json`, `pnpm-lock.yaml`, `requirements*.txt` | Deps changes |
| `Dockerfile*`, `docker-compose*.yml` | Container build |
| `infrastructure/**` | Helm/k8s/terraform (any) |
| `*.java`, `*.ts`, `*.tsx`, `*.py`, `*.go` | Source code |
| `application*.yml`, `application*.properties` | Runtime config |
| `*.sql` | Migrations |
| `.gitignore`, `.gitattributes` | Repo behavior change |
| Any file ≥1MB (binary additions) | Out of doc shape |

If diff is **mixed** (e.g., `documents/foo.md` + `pom.xml`) → out of scope.

---

## 4. CI gate — what counts as "green"

Auto-merge ONLY proceed khi:

- All required status checks `state:SUCCESS` (per `gh pr checks --json state`)
- No checks in `IN_PROGRESS` / `PENDING` (wait until terminal)
- No checks in `FAILURE` / `CANCELLED` (rule N/A — handle per normal failure flow)
- For docs-only PRs, path filters thường skip backend test workflows → 3-10 checks all SUCCESS là norm, KHÔNG phải vấn đề
- `--admin` bypass BANNED — per `admin-merge-discipline.md`

---

## 5. Required behavior

Khi rule applies (§2 scope + §4 CI green):

```
1. Verify CI green via gh pr checks --json state OR MCP equivalent
2. mcp__github__merge_pull_request với merge_method:"squash"
3. git checkout main + git pull --ff-only + git branch -D <feature-branch>
4. Surface result trong 1 message:
   "✅ Merged #N (squash → <sha>). <1-line gap/scope status update>. Next?"
```

Tổng cộng 1 user message thay vì 3 (current pattern: "check CI" → "merge?" → final summary).

---

## 6. User-side override mechanisms

User có thể prevent auto-merge:

| Mechanism | Example |
|---|---|
| Inline instruction trong cùng turn | "Tạo PR nhưng đừng merge ngay" / "wait before merging" |
| GitHub label `[do-not-auto-merge]` | Add via `gh pr edit N --add-label do-not-auto-merge` |
| Commit trailer | `DOCS_AUTO_MERGE_HOLD: <reason>` trong squash commit message |
| Explicit "stop" phrasing | "stop / pause / không merge" trong turn ngay sau PR creation |

Nếu ANY override → revert to default "check CI? merge?" pattern.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Auto-merge PR có touched `pom.xml` "vì commit message bảo docs-only" | Inspect diff via `git diff --cached --name-only` — out-of-scope = manual flow |
| Auto-merge PR với `--admin` "vì CI sẽ green sau" | `--admin` BANNED per `admin-merge-discipline.md` |
| Skip CI gate "vì docs nhỏ" | CI là gate; pass = exit |
| Ask "check CI?" rồi sau đó "merge?" cho docs-only PR | One-shot: check CI → merge → cleanup → 1 status message |
| Auto-merge PR có `.github/workflows/` change | Workflow changes scrutinized — manual flow |
| Treat `.claude/rules/*.md` PR như "code" | Rules là docs governance — auto-merge OK nếu CI green |
| Hold auto-merge "vì rule mới chưa stable" | Rule landing với enforcement parity = ready; user override cụ thể nếu cần |

---

## 8. Self-test (worked example — 2026-05-11 session PRs #1151 + #1152)

### 8.1 PR #1151 — GAP-452 secrets runbook split

**Diff scope:** 9 files: 5 `documents/**/*.md` + `.claude/rules/deployment-naming-convention.md` + `.env.production.template` + `documents/05-guides/deploy/secrets-seeding-runbook.md` (new) + `documents/05-guides/operations/secrets-rotation-runbook.md` (rename).

**§2 check:** ALL paths trong scope. ✅
**§3 check:** Zero out-of-scope. ✅
**CI:** 10/10 SUCCESS. ✅
**§5 behavior:** Should have auto-merged + cleanup + 1 summary message.
**Actual behavior:** Asked "check CI?" → "merge?" → 3 turns.

→ Rule fires correctly on this PR. Self-test PASS ✅

### 8.2 PR #1152 — GAP-468 + GAP-470 DONE flip + ROADMAP sync

**Diff scope:** 3 files: `documents/04-quality/gaps/GAP-468*.md` + `GAP-470*.md` + `ROADMAP.md`.

**§2 check:** ALL paths trong `documents/**`. ✅
**§3 check:** Zero out-of-scope. ✅
**CI:** 3/3 SUCCESS (Vercel only — path filters skipped Java tests, expected). ✅
**§5 behavior:** Should have auto-merged.
**Actual behavior:** Asked "check CI?" → "merge?" → 3 turns.

→ Rule fires correctly. Self-test PASS ✅

### 8.3 PR #1150 — postgresql + commons-lang3 fix (counter-example)

**Diff scope:** 3 files: `your-product-a/pom.xml` + `your-product-b/your-core/pom.xml` + `your-product-b/your-gateway-b/pom.xml`.

**§3 check:** ALL files match `pom.xml` (out-of-scope BANNED). ❌
**Verdict:** Rule N/A. Default "check CI? merge?" pattern correct cho PR này (dep change = blast radius higher).

→ Rule correctly excludes code/deps PRs. Self-test PASS ✅

---

## 9. Enforcement (per `rule-change-process.md` §6.5)

### 9.1 Memory auto-load (per-session)

Memory entry `feedback_docs_only_pr_auto_merge.md` (paired same-PR) loads at session start. Includes:
- Quick checklist trước khi merge: diff scope ✅? CI green ✅? Override absent ✅?
- 3 worked examples từ §8

### 9.2 Self-detection (in-turn)

Sau khi tạo bất kỳ PR nào, before sending "check CI?" message, Claude mentally runs §2/§3 scope check. Nếu docs-only → run §4 CI wait → §5 auto-merge sequence trực tiếp.

### 9.3 Reviewer manual / user verification

User có thể flag bằng cách hỏi "tại sao vẫn check CI rồi mới merge?" sau khi rule landed. Pattern repeated trong cùng session → file follow-up gap referencing this rule.

### 9.4 Detector (deferred)

Future enhancement: scan recent session transcripts for "check CI?" + "merge?" two-prompt pattern on docs-only PRs. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; memory auto-load + worked self-test sufficient cho v1.0.0.

---

## 10. Relationship to other rules

- **`agent-action-bias.md`** §1 Part A — "do it yourself"; this rule is concrete instance cho docs-only merge scope
- **`admin-merge-discipline.md`** — `--admin` BANNED; this rule respects + cites
- **`gap-done-discipline.md`** — DONE flip PRs thường docs-only → strong candidate for auto-merge khi AC verified
- **`rule-change-process.md`** §6.5 — Enforcement Parity Mandate; this rule + memory + worked self-test same PR
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-11 user-flagged friction ("nếu PR docs-only thì không hỏi check CI mà merge luôn") applied through 5-stage pipeline
- **`feedback_docs_only_pr_auto_merge.md`** (memory, paired same-PR)
- **`output-review-mandate.md`** §3 — review standard preserved; CI green = review evidence cho docs-only

---

## 11. Log

- **2026-05-11 (v1.0.0):** Rule created at user request "thêm rules, nếu PR docs-only thì không hỏi check CI mà merge luôn" — direct response to 2026-05-11 session friction (3 docs-only PRs #1151 + #1152 + earlier each prompted "check CI?" + "merge?" two-step despite zero-risk scope). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged friction) → Classify ✓ (no existing rule covers; `agent-action-bias.md` §1 Part A covers general "do it yourself" but không specific cho merge gate timing) → Rule+Enforce ✓ (this file + memory `feedback_docs_only_pr_auto_merge.md` paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§8 worked examples PR #1151 + #1152 + counter-example #1150) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint sharpening existing "do it yourself" rule, no constraint loosening; existing 3-prompt pattern grandfathered for non-docs PRs; rule applies prospectively cho docs-only scope từ next session forward). Detector wiring (§9.4) deferred per premature-rule guard ≥7 ngày.
