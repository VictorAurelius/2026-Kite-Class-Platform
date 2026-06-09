---
paths:
  - "infrastructure/terraform-aws/ecr.tf"
  - ".github/workflows/docker-build-push.yml"
  - "scripts/check-ecr-lifecycle-coverage.sh"
---

# Retention-Policy Completeness — every CI-produced artifact category must be capped

**Priority:** 🟠 MANDATORY — storage cost + unbounded-growth governance
**Version:** 1.0.0
**Created:** 2026-06-09
**Last-Reviewed:** 2026-06-09
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (static CI detector `scripts/check-ecr-lifecycle-coverage.sh` + reviewer-checklist + worked self-test on 2026-06-10 ECR cosign-accumulation incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "CI introduces new artifact/tag category but retention policy doesn't cover it → unbounded growth + silent cost creep"; statically-detectable → persistent detector per `cross-flow-bug-class-sweep.md` §4.1)
**Applies to:** Mọi PR thay đổi (a) lifecycle/retention policy của một storage system (ECR `aws_ecr_lifecycle_policy`, S3 lifecycle, CloudWatch log retention, RDS snapshot retention), HOẶC (b) CI workflow sinh ra một tag/artifact/object category mới (cosign sig/att, SBOM tag, new image tag scheme, new log stream). Out-of-scope: PR không đụng storage policy lẫn artifact-producing CI.

---

## 1. The Rule

> **Khi CI/automation sinh ra một artifact category mới vào storage system (ECR image tag, S3 object prefix, log stream), retention/lifecycle policy của storage đó PHẢI cap nó — hoặc có quyết định "keep-forever" được ghi rõ.** Mọi tag-family unbounded-growth không được cap = leak.

Code cho biết CI sinh gì; lifecycle policy cho biết cái gì được dọn. Khi hai cái lệch — category mới không khớp rule expire nào — nó tích vô hạn, gây cost creep âm thầm mà không ai nhận ra cho tới khi đọc bill.

Force-multiplier: 1 detector tĩnh so sánh "CI sinh gì" ↔ "policy cap gì" → mọi PR subsequent auto-catch category chưa cap.

---

## 2. Trigger pattern — khi nào rule fires

| Tình huống | Fire? |
|---|---|
| Thêm cosign/SBOM/attestation step vào `docker-build-push.yml` (sinh tag `sha256-*.sig/.att`) | ✅ YES — phải có lifecycle rule cap `sha256-` |
| Đổi tag scheme image (thêm prefix mới) | ✅ YES — cap hoặc keep-forever-có-chủ-đích |
| Sửa `ecr.tf` lifecycle policy | ✅ YES — verify vẫn cover mọi family CI sinh |
| Thêm S3 upload prefix mới vào code | ✅ YES — S3 lifecycle phải cover (hoặc ghi keep-forever) |
| Thêm log stream / metric mới | ✅ YES — retention phải set |
| PR thuần docs / không đụng storage policy lẫn artifact CI | ❌ NO |

Rule **KHÔNG** fires khi: tag family mới được CHỦ ĐÍCH keep-forever (vd version tag `v*`/`0.x` cho redeploy cũ) — nhưng quyết định đó PHẢI ghi comment trong policy.

---

## 3. Required action / artifacts

### 3.1 Khi thêm artifact-producing CI step

PR phải đảm bảo lifecycle policy cap tag-family mới HOẶC comment ghi rõ keep-forever:

```hcl
# Ví dụ: cosign sinh sha256-*.sig/.att → cap chúng
{
  rulePriority = 4
  description  = "Keep last 40 cosign signature/attestation tags (sha256-*.sig/.att)"
  selection = { tagStatus = "tagged", tagPrefixList = ["sha256-"], countType = "imageCountMoreThan", countNumber = 40 }
  action = { type = "expire" }
}
```

### 3.2 Detector phải PASS

`bash scripts/check-ecr-lifecycle-coverage.sh` exit 0. Detector cross-reference `docker-build-push.yml` (có cosign/attest?) ↔ `ecr.tf` (có rule `sha256-`?).

### 3.3 Keep-forever phải có comment

Family chủ đích không cap (vd version tag) → comment trong `ecr.tf` ghi rõ + ước tính cost. KHÔNG để family rớt qua mọi rule một cách im lặng.

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Thêm cosign signing mà không cap `sha256-*` | Pair lifecycle rule `sha256-` cùng PR |
| Giả định "untagged 7-day expire dọn hết" | Cosign tag là *tagged* `sha256-` — untagged rule không bắt |
| Để tag-family rớt qua mọi rule im lặng | Cap hoặc comment keep-forever-có-chủ-đích |
| Sửa lifecycle policy mà không chạy detector | `check-ecr-lifecycle-coverage.sh` trước push |
| Đổ lỗi CI push "sinh nhiều image" | CI push/sign mỗi merge là đúng; gap là ở policy coverage |
| Dọn one-time rồi quên detector | One-time prune + persistent detector (statically-detectable class) |

---

## 5. Override mechanism

Genuine exception (family chủ đích keep-forever ngoài comment, hoặc storage không có lifecycle API):

```
git commit -m "...
RETENTION_POLICY_OVERRIDE: <storage + family — reason — e.g. 'version tags v* kept-forever for old-version redeploy, ~$5/mo accepted'>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 6. Worked self-test — 2026-06-10 ECR cosign-accumulation incident

**Scenario:** GAP-402 (Wave ~89) thêm cosign keyless signing + SBOM attestation vào `docker-build-push.yml` → mỗi merge sinh `sha256-<digest>.sig` + `.att` cho 10 service. `ecr.tf` lifecycle policy (redesign 2026-05-13) cap `sha-`/`main`/`latest`/`pr-` + expire untagged, NHƯNG prefix `sha256-` (≠ `sha-`) rớt qua MỌI rule → ~2,780 sig/att tích vô hạn (4,210 total images / ~$2/mo + clutter), phát hiện khi user thấy AWS vượt credit.

**Apply rule retroactively (counterfactual):** Tại PR thêm cosign signing (GAP-402), detector `check-ecr-lifecycle-coverage.sh` chạy:
- Grep `docker-build-push.yml` → tìm thấy `cosign` + `sbom: true` + `provenance: true` → CI sinh `sha256-*`.
- Grep `ecr.tf` tagPrefixList → `{sha-, main, test, latest, pr-}` — KHÔNG có `sha256-`.
- → **FAIL**: "produces cosign sha256-*.sig/.att but ecr.tf has NO sha256- expire rule".

→ PR bị block tại CI → tác giả thêm rule 4 cùng PR → 0 accumulation. **Verified:** chạy detector trên ecr.tf pre-fix (không rule 4) → FAIL với đúng message; trên ecr.tf post-fix → PASS. Self-test PASS ✅.

**Counterfactual cost:** detector tại GAP-402 PR → 0 image rác, 0 cost creep, 0 one-time prune session. Without: ~3,715 image phải dọn + ~$1-2/mo waste + 1 user-flagged incident.

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Static CI detector (active now)

`scripts/check-ecr-lifecycle-coverage.sh` — wired job `ecr-lifecycle-coverage` trong `.github/workflows/quality-infra.yml` (paths: `ecr.tf` + `docker-build-push.yml` + detector). Blocking on FAIL. Self-test step asserts detector fires on pre-fix state. Statically-detectable class → persistent detector mandatory per `cross-flow-bug-class-sweep.md` §4.1.

### 7.2 Reviewer-checklist (active now)

PR đụng lifecycle policy HOẶC artifact-producing CI:
- [ ] CI sinh tag/artifact family mới? Family đó được cap trong lifecycle policy?
- [ ] Sửa lifecycle policy? Vẫn cover mọi family CI sinh (detector PASS)?
- [ ] Family keep-forever có comment ghi rõ + cost estimate?

### 7.3 Detector scope-extension (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)

- **Complexity:** Mở rộng detector sang S3 lifecycle + CloudWatch log retention + RDS snapshot = parse thêm nhiều terraform resource + cross-ref upload-site code — non-trivial, mỗi storage type 1 parser riêng.
- **Recurrence:** 1 (ECR cosign, 2026-06-10). Chưa có S3/log/snapshot accumulation incident.
- **Decision:** ECR detector §7.1 + reviewer-checklist §7.2 (cover các storage khác qua manual review) đủ cho v1.0.0; revisit extend detector khi recurrence ≥1 trên storage type khác (S3/log/snapshot).

### 7.4 Override — per §5.

---

## 8. Relationship to other rules

- **`cross-flow-bug-class-sweep.md`** §4.1 — statically-detectable class → persistent detector mandate; rule này = instance (ECR tag-coverage detect được tĩnh).
- **`local-fix-production-parity-check.md`** — code-fix → prod-env-surface sweep; sister direction. Rule này = CI-artifact → retention-policy coverage.
- **`audit-to-gap-pipeline.md`** §2.7 Decision-Doc Code-Sync — decision-doc → code sweep; rule này = artifact-producer → policy sweep.
- **`agent-aws-access.md`** §4.1 — ECR `batch-delete-image`/`delete-repository` Tier 3; rule này bổ sung phòng-ngừa-tích-luỹ (không cần delete nếu policy cap đúng).
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier.
- **`incident-to-rule-pipeline.md`** — rule này = direct output 2026-06-10 ECR cosign-accumulation incident qua 5-stage.
- **`rule-change-process.md`** §6.5 Enforcement Parity — rule + detector + reviewer-checklist + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row all same PR.
- **`context-budget-mandate.md`** §3.2 — path-scoped (`paths:` frontmatter) — load chỉ khi đụng `ecr.tf` / `docker-build-push.yml` / detector.

---

## 9. Log

- **2026-06-09 (v1.0.0):** Rule created in response to user question (session local 2026-06-10 GMT+7 = 2026-06-09 UTC per CI frontmatter gate) "cần update meta để tránh lỗi không?" sau ECR cost-cleanup (4,210 → ~895 images, −79% prune). Triggered by 2026-06-10 incident: GAP-402 cosign keyless signing sinh `sha256-*.sig/.att` nhưng `ecr.tf` lifecycle policy không cap prefix `sha256-` (≠ `sha-`) → ~2,780 sig/att tích vô hạn, phát hiện khi AWS vượt credit. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged cost overrun + asked "update meta?") → Classify ✓ (no existing rule covers "CI artifact category vs retention policy coverage"; `cross-flow-bug-class-sweep.md` §4.1 mandates persistent detector for statically-detectable class but không có ECR-specific rule; `local-fix-production-parity-check.md` covers code→infra, không cover artifact→policy) → Rule+Enforce ✓ (this file + `scripts/check-ecr-lifecycle-coverage.sh` static detector + `quality-infra.yml` job `ecr-lifecycle-coverage` + reviewer-checklist + worked self-test §6 on originating incident + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 — detector FAILs on pre-fix ecr.tf, PASSes on post-fix; fires correctly on originating incident) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 detector → mọi PR đụng lifecycle policy / artifact CI auto-catch uncapped category. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered retention-coverage class; no constraint loosening; existing storage policies grandfathered (ECR fixed same PR-cluster #2288); rule applies prospectively từ this PR forward 2026-06-10). Atomic-unique-bar §5.1: ✅ atomic (1 concept: retention policy must cover every CI artifact category) ✅ unique (sister rules cover code→infra / decision→code, không artifact→policy) ✅ widely applicable (mọi storage-policy / artifact-CI change) ✅ body discipline §1 ≤2 conjunction. Detector scope-extension (S3/log/snapshot) HONEST-deferred §7.3 (recurrence 1, per-storage-type parser non-trivial); ECR detector + reviewer-checklist sufficient cho v1.0.0.
