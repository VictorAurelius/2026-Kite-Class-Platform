---
paths:
  - ".github/workflows/terraform-apply.yml"
  - "infrastructure/terraform-aws/**"
  - "infrastructure/terraform-oracle/**"
  - "documents/04-quality/audits/aws-verification/**"
---

# Dev-Authorized Terraform Trigger — solo-dev override of release-deploy-standard.md §9

**Priority:** 🟠 MANDATORY — production deploy automation governance
**Version:** 1.0.0
**Created:** 2026-05-15
**Last-Reviewed:** 2026-05-15
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule path-scoped per `context-budget-mandate.md` §3.1; complements `release-deploy-standard.md` §9 với explicit user-authorized override pattern; built-in enforcement = 5-step procedural gate + audit artifact + reviewer-checklist per §6.5 Enforcement Parity Mandate)
**Applies to:** Mọi lần claude trigger `gh workflow run terraform-apply.yml` (hoặc `rollback.yml`) thay cho user khi user explicit cho phép trong session

---

## 1. The Rule

> **Default `release-deploy-standard.md` §9 BANS agent-initiated `terraform apply`.** Khi dev (user) trong session explicit cho phép bằng phrase rõ ràng — VD: "claude trigger đi", "tôi cho phép claude trigger", "claude run terraform apply" — claude được phép trigger workflow `terraform-apply.yml` với 5 gate procedural §2 bên dưới. KHÔNG mở rộng sang Tier 3 mutation khác.

---

## 2. 5 gate bắt buộc trước/trong/sau trigger

### 2.1 Pre-flight concurrent-op check (per `concurrent-production-mutation-ops.md` §4)

```bash
gh run list --status in_progress --json name,workflowName --jq '.[] | "\(.workflowName)"'
```

Output PHẢI empty trước khi trigger. Có overlap → STOP, đợi op cũ xong + verify state healthy.

### 2.2 Pre-mutation audit artifact tồn tại (per `pre-mutation-state-check.md` §3)

PHẢI có file `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>-plan.md` với scope + commands + findings + verdict. Wave-spawn agent thường tạo sẵn. Nếu thiếu → STOP, tạo trước.

### 2.3 Default `dry_run=true` trước

```bash
gh workflow run terraform-apply.yml --ref main -f confirm=APPLY -f dry_run=true -f version=main
gh run watch <run-id>
```

Đọc plan summary. Chỉ tiến sang `dry_run=false` khi:
- Plan match audit artifact §2.2 (số resource add/change/destroy đúng kỳ vọng)
- Không có surprise destroy/replace ngoài scope audit

Nếu dev explicit "skip dry_run" → có thể bypass §2.3, document override trong audit artifact.

### 2.4 Real apply + monitor

```bash
gh workflow run terraform-apply.yml --ref main -f confirm=APPLY -f dry_run=false -f version=main
gh run watch <run-id>
```

GitHub Environment `production` reviewer approval gate vẫn ép buộc human approve job — đây là 2nd cognitive checkpoint. Override rule không bypass GitHub Environment gate.

### 2.5 Post-apply Tier 1 verify + audit artifact

Per `agent-aws-access.md` §2.1 Tier 1 read-only. Verify resources tạo đúng (vd `aws cloudwatch list-dashboards`, `aws lambda get-function`, `aws secretsmanager describe-secret`). Save kết quả vào `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>-post-apply.md`:

```markdown
## Scope
Trigger thay user: <reason — citing dev authorization phrase>
Workflow run: <URL>
Plan dry_run run: <URL>

## Commands run (Tier 1)
<list>

## Findings
<resources verified present + state>

## Verdict
<safe / drift / fail>
```

---

## 3. Out of scope (vẫn BANNED dù dev explicit cho phép trigger)

| Op | Lý do giữ BANNED |
|---|---|
| `kubectl apply` production cluster | K8s chưa Phase 1; chưa có precedent |
| `aws ec2 terminate-instances` | Destructive, irreversible nhanh |
| `aws rds delete-db-instance` | Data loss class |
| `aws iam delete-role` / `delete-policy` | Auth blast radius |
| Cloudflare DNS DELETE / PATCH apex | DNS propagation cost |
| `gh secret set production` rotate sensitive | Live credential transition |
| Tier 3 mutation per `agent-aws-access.md` §4.3 | Existing ban giữ nguyên |

Khi gặp ops trên → ASK user, không auto-trigger.

---

## 4. Triggers (phrase nhận dạng)

Phrase user explicit cho phép trigger:
- "claude trigger" / "claude trigger đi" / "claude trigger giúp"
- "tôi cho phép claude trigger"
- "claude run terraform apply"
- "trigger apply giúp" (kèm context terraform)
- "apply đi" (chỉ khi prior turn đã discuss terraform apply)

Phrase KHÔNG đủ:
- "Sẵn sàng apply?" → đây là câu hỏi, không phải cho phép
- "Apply chưa?" → câu hỏi
- "Plan ready" → context check, không trigger
- Silent (no explicit phrase) → default BANNED giữ nguyên

Khi nghi ngờ → ask user `AskUserQuestion` cho explicit yes/no.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Trigger `dry_run=false` ngay khi dev cho phép | Default `dry_run=true` trước per §2.3 |
| Skip pre-flight concurrent check | `gh run list --status in_progress` luôn trước |
| Trigger song song 2 terraform apply | Single apply covers all pending; per §2.1 zero overlap |
| Mở rộng override sang Tier 3 mutation | Chỉ `terraform-apply.yml` + `rollback.yml`; rest BANNED |
| Trigger không có audit artifact | §2.2 mandatory; nếu thiếu STOP |
| Bỏ post-apply verify | §2.5 mandatory; verify trước khi report DONE |
| Treat user phrase "apply" trong context khác (vd "apply CSS") là trigger authorization | §4 trigger detection strict — phải trong terraform context |

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 Procedural — claude tự enforce 5 gates §2

Claude session chạy 5 gate trước/trong/sau mỗi trigger. Sai gate = vi phạm rule = file follow-up gap per `incident-to-rule-pipeline.md`.

### 6.2 GitHub Environment `production` reviewer gate

External enforcement layer — terraform-apply.yml workflow yêu cầu reviewer approve job ở environment `production`. Override rule này KHÔNG bypass GitHub Environment gate; gate vẫn là 2nd cognitive checkpoint.

### 6.3 Audit artifact trail

Mỗi trigger session ship `aws-verification/YYYY-MM-DD-<topic>-post-apply.md`. Quarterly retro review pattern frequency — nếu >5/quarter triggers/user → meta-review rule.

### 6.4 Override-of-override mechanism

Dev có thể tightening tạm thời (vd "hôm nay đừng trigger, tôi tự làm"):

```
# Trong session:
"Skip override hôm nay, tôi tự trigger" → claude tôn trọng + tự ban
```

Không cần trailer; chỉ trong session đó.

### 6.5 Detector (deferred)

Future: `audit-gate.py` scan session transcript cho `gh workflow run terraform-apply` invocation không có matching `aws-verification/*-post-apply.md` artifact landed same PR. Defer per `incident-to-rule-pipeline.md` §3 advisory-rule guard ≥7 ngày.

---

## 7. Relationship to other rules

- **`release-deploy-standard.md`** §9 — default BANS agent-trigger; this rule = explicit override pattern with 5 gates
- **`concurrent-production-mutation-ops.md`** §4 — §2.1 pre-flight check mandate cross-reference
- **`pre-mutation-state-check.md`** §3 — §2.2 audit artifact mandate cross-reference
- **`agent-aws-access.md`** §2.1 Tier 1 — §2.5 post-apply verify scope
- **`agent-aws-access.md`** §4.3 Tier 3 — §3 out-of-scope ops still BANNED
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + procedural gates + audit trail + CLAUDE.md note all ship same PR
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-15 user-flagged "tôi cho phép claude trigger" + "đỡ công dev chạy thủ công" via 5-stage pipeline
- **`context-budget-mandate.md`** §3.1 — path-scoped frontmatter (`paths:`) chosen vì rule chỉ relevant khi terraform context

---

## 8. Self-test (worked example — Wave 84 buckets A+B+G post-merge 2026-05-15)

**Scenario:** 6 Wave 84 buckets merged to main; 3 (A, B, G) shipped plan-only .tf files. Dev says "tôi cho phép claude trigger và monitor".

**Apply §2 gates:**

| Gate | Verify | Result |
|---|---|---|
| §2.1 Concurrent op check | `gh run list --status in_progress` | ✅ empty |
| §2.2 Pre-mutation audit | `documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-{a,b,g}-*-plan.md` | ✅ 3 artifacts exist |
| §2.3 dry_run=true first | Triggered run 25928976288 | ✅ in_progress |
| §2.4 Real apply | Pending dry-run review | ⏳ awaiting |
| §2.5 Post-apply verify | Pending apply | ⏳ awaiting |

§4 phrase detection: "tôi cho phép claude trigger" matches phrase list → authorization OK.

→ Rule fires correctly on the originating session. ✅

---

## 9. Log

- **2026-05-15 (v1.0.0):** Rule created in same session as the override-trigger event (Wave 84 buckets A+B+G post-merge). Triggered by user explicit phrase "tôi cho phép claude trigger và monitor" + follow-up "thêm vào claude md, khi dev cho phép claude trigger thì được với override rule (đỡ công dev chạy thủ công)". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user explicit ask to codify standing override) → Classify ✓ (no existing rule codifies dev-authorized override pattern; `release-deploy-standard.md` §9 only ALLOWS human-triggered; override needs explicit framework) → Rule+Enforce ✓ (this file + same-PR `claude-md-content-discipline.md` sister rule + CLAUDE.md condensed 1-liner pointing here + rules-index.csv 2 new rows per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§8 worked example on Wave 84 session) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint codifying solo-dev override; no constraint loosening for non-override sessions; rule applies prospectively when dev says trigger phrase §4). Path-scoped per `context-budget-mandate.md` §3.1 — không bloat base context.
