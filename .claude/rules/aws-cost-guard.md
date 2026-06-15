---
paths:
  - ".github/workflows/**"
  - "infrastructure/terraform-aws/**"
  - ".claude/rules/aws-cost-guard.md"
---

# AWS Cost Guard — no routine billable-artifact production; push only at deploy

**Priority:** 🟠 MANDATORY — AWS cost governance
**Version:** 1.0.0
**Created:** 2026-06-15
**Last-Reviewed:** 2026-06-15
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on 2026-06-15 bill-spike incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "CI produces billable AWS artifacts on routine events → unbounded cost". Sister to `retention-policy-completeness.md` (cap-after-produce) tại boundary khác: don't-produce-on-routine)
**Applies to:** Mọi CI/CD workflow trigger config (`.github/workflows/**`) + terraform resource sinh recurring AWS cost (`infrastructure/terraform-aws/**`). Out-of-scope: deploy/release workflows triggered by tag/dispatch (đó chính là lúc được phép push).

---

## 1. The Rule

> **CI/automation KHÔNG được push billable AWS artifact (ECR image, S3 object, EBS snapshot, custom CloudWatch metric/dashboard) trên routine event (`push: main`, `pull_request`, scheduled lint). Push/produce CHỈ khi deploy event — release tag `v*.*.*` HOẶC `workflow_dispatch` lúc deploy.** PR validation builds phải `push: false` (build-only).

Routine events fire hàng chục lần/ngày (mỗi merge, mỗi PR). Mỗi lần push 10 multi-arch image → ECR tích vô hạn → cost creep âm thầm. Deploy events hiếm (chỉ khi thực sự ship) → push lúc đó = đúng nhu cầu, 0 lãng phí.

Sister rule ở boundary nghịch:
- `retention-policy-completeness.md` — NẾU produce artifact → lifecycle policy phải cap nó
- **This rule** — đừng produce artifact trên routine event ngay từ đầu (deploy-only)

Force-multiplier: 1 chuẩn deploy-only-push → mọi workflow subsequent không tích artifact rác.

---

## 2. Trigger pattern — khi nào rule fires

| Tình huống | Fire? |
|---|---|
| Workflow có `push: branches: [main]` + step push ECR/S3 | ✅ YES — đổi sang tag/dispatch-only |
| Workflow `pull_request` build image với `push: true` | ✅ YES — phải `push: false` (build-only validation) |
| Thêm step push S3/ECR vào job chạy trên routine event | ✅ YES |
| Scheduled cron sinh snapshot/metric mỗi giờ không cap | ✅ YES — verify cadence + retention |
| Terraform thêm resource recurring-cost (NAT gateway, idle EIP, oversized RDS, dashboard) | ✅ YES — justify cost trong PR |
| Deploy workflow (`deploy-*.yml` / tag `v*` / `workflow_dispatch`) push ECR | ❌ NO — đó là deploy-time, đúng |
| PR thuần docs / code không đụng workflow trigger lẫn terraform cost-resource | ❌ NO |

Rule **KHÔNG** fires khi push/produce gắn với deploy event (tag/dispatch/deploy-workflow) — đó là mục đích hợp lệ.

---

## 3. Required action / artifacts

### 3.1 Khi workflow push billable artifact

Trigger config PHẢI giới hạn push step vào deploy event:

```yaml
on:
  push:
    tags: ['v*.*.*']        # release = deploy
  pull_request: { ... }     # build-only (push:false)
  workflow_dispatch: { ... } # manual deploy
# job push-to-ecr:
#   if: ((github.event_name=='push' && startsWith(github.ref,'refs/tags/v')) || github.event_name=='workflow_dispatch')
```

PR job dùng `docker/build-push-action` với `push: false` cho validation. KHÔNG `push: branches: [main]` cho artifact-producing job.

### 3.2 Khi terraform thêm recurring-cost resource

PR body phải có 1 dòng cost-justify: resource + est. $/tháng + lý do cần (vd "NAT gateway $32/mo — required cho private subnet egress"). Idle resource (unattached EIP, stopped-but-provisioned, oversized) → flag để giảm.

### 3.3 Cost-driver sanity (session-start awareness)

`collect-state.sh` đã snapshot ECR image count + EC2/RDS state. Khi count tăng bất thường (vd ECR >100 image) → điều tra trước khi tiếp tục (early-warning, không chờ bill).

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| `push: branches: [main]` cho job push ECR | `push: tags: ['v*.*.*']` + `workflow_dispatch` only |
| PR build image với `push: true` "để test ECR" | `push: false` — build-only validation trên PR |
| "Push mỗi merge để image luôn sẵn sàng" | Push lúc deploy; deploy hiếm → image sinh đúng nhu cầu |
| Để CloudWatch dashboard/alarm chạy khi stack stopped | Tắt observability khi stack idle (re-provision qua terraform lúc restart) |
| Thêm NAT gateway/EIP không justify cost | Cost-justify dòng trong PR body; idle resource → release |
| Đổ lỗi "CI sinh nhiều image" khi bill tăng | Gap là trigger config (routine vs deploy) — sửa trigger |

---

## 5. Override mechanism

Genuine exception (vd cần image-per-merge cho staging auto-deploy có lifecycle cap chặt):

```
git commit -m "...
AWS_COST_GUARD_OVERRIDE: <workflow/resource — reason + cost cap — e.g. 'staging auto-deploy push main, capped 5-img lifecycle, ~$2/mo'>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 6. Worked self-test — 2026-06-15 bill-spike incident

**Scenario:** User báo "bill AWS tăng đột biến". Điều tra: ECR $43 (15 ngày) / 206GB / 2,746 image — `docker-build-push.yml` có `push: branches: [main]` → mỗi merge (~25 PR/session) push 10 multi-arch image → tích vô hạn. Lifecycle policy không kịp prune (countNumber cao).

**Apply rule retroactively (counterfactual):** Tại PR re-enable `docker-build-push.yml` `push: main` (2026-05-27 GAP-756), rule §2 fires → reviewer flag "push main = routine event, billable artifact" → đổi sang tag/dispatch-only → mỗi session 25 merge sinh 0 image (chỉ build-only PR validation); image chỉ sinh khi thực deploy (tag/dispatch, ~1-2 lần/tuần).

| Metric | Without rule (push:main) | With rule (deploy-only) |
|---|---|---|
| Image pushed/session (~25 merge) | ~250 (25×10) | 0 (build-only) |
| ECR steady-state | 206GB / 2,746 img unbounded | ~30 img (3/repo × deploy) |
| ECR cost/tháng | $43–86 | ~$2 |

**Save:** ~$40–80/tháng + 0 prune session. Self-test PASS ✅ — rule fires đúng trên incident gốc.

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)

PR đụng `.github/workflows/**` artifact-producing job HOẶC `infrastructure/terraform-aws/**`:
- [ ] Artifact-push job gated vào deploy event (tag `v*` / `workflow_dispatch`), KHÔNG `push: main`?
- [ ] PR build dùng `push: false` (build-only validation)?
- [ ] Terraform recurring-cost resource có cost-justify dòng trong PR body?
- [ ] Idle resource (unattached EIP / stopped-provisioned / oversized) flagged để giảm?

### 7.2 Self-detection (in-turn)

Trước khi thêm/sửa workflow trigger có artifact push: routine event (`push:main`/`pull_request` với push:true)? → STOP, gate vào deploy event.

### 7.3 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)

- **Complexity:** Parse workflow YAML → tìm job có ECR/S3 push step + check trigger gate (event_name condition) — cần YAML parser + cross-ref job `if:` với `on:` triggers, non-trivial.
- **Recurrence:** 1 (2026-06-15 ECR push:main). Sister `check-ecr-lifecycle-coverage.sh` (retention-policy-completeness) đã cover cap-side.
- **Decision:** Reviewer-checklist §7.1 + self-detection §7.2 + sister ECR detector + worked self-test §6 đủ cho v1.0.0; revisit khi recurrence ≥1 trên workflow khác (S3 push / snapshot cron).

### 7.4 Override — per §5.

---

## 8. Relationship to other rules

- **`retention-policy-completeness.md`** — sister boundary: rule kia cap artifact NẾU produce; rule này = đừng produce trên routine event. Compose: deploy-only push + lifecycle cap = double guard.
- **`agent-aws-access.md`** §4 — Tier 3 mutation governance (xoá/sửa AWS); rule này = phòng-ngừa-tích-luỹ qua CI trigger config.
- **`cross-flow-bug-class-sweep.md`** §4.1 — statically-detectable class → persistent detector (deferred §7.3).
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier.
- **`incident-to-rule-pipeline.md`** — rule này = direct output 2026-06-15 bill-spike incident qua 5-stage.
- **`rule-change-process.md`** §6.5 Enforcement Parity — rule + reviewer-checklist + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row same PR.
- **`context-budget-mandate.md`** §3.2 — path-scoped (`paths:` frontmatter) — load chỉ khi đụng workflow/terraform.

---

## 9. Log

- **2026-06-15 (v1.0.0):** Rule created in response to user direction 2026-06-15 "bill AWS tăng đột biến... sửa CI tự động push ECR đi, chỉ khi nào deploy mới push... tạo rule mới để tránh tăng chi phí aws". Triggered by bill-spike investigation: `docker-build-push.yml` `push: branches: [main]` → mỗi merge push 10 multi-arch image → ECR tích 206GB/2,746 img → $43+/15 ngày. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged bill spike) → Classify ✓ (no existing rule covers "don't produce billable artifact on routine CI event"; `retention-policy-completeness.md` covers cap-after-produce, không cover don't-produce-on-routine) → Rule+Enforce ✓ (this file + paired same-PR `docker-build-push.yml` trigger fix (push:main→tag/dispatch) + reviewer-checklist + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 — rule fires đúng, counterfactual ~$40–80/mo + 0 prune session) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn deploy-only-push → mọi workflow subsequent không tích artifact rác. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered routine-artifact-production class; no constraint loosening; existing workflows grandfathered (docker-build-push fixed same PR); rule applies prospectively từ this PR forward 2026-06-15). Atomic-unique-bar §5.1: ✅ atomic (1 concept: deploy-only billable-artifact push) ✅ unique (sister covers cap-after-produce) ✅ widely applicable (mọi artifact-producing CI) ✅ body §1 ≤2 conjunction. Detector HONEST-deferred §7.3 (recurrence 1, YAML-parse non-trivial); reviewer-checklist + sister ECR detector + worked self-test sufficient cho v1.0.0.
