---
title: Phase 3 — Image push runbook (first OIDC-driven ECR push)
status: active
created: 2026-05-07
updated: 2026-05-07
related:
  - release-1-deploy-runbook.md
  - secrets-populate-phase-2-4.md
phase: "3"
---

# Phase 3 — Image push runbook (first OIDC-driven ECR push)

**Reference runbook:** [`release-1-deploy-runbook.md`](../../03-planning/roadmap/release-1-deploy-runbook.md) §3

**Pre-condition:** Phase 2.1 (state backend) + Phase 2.2 (OIDC roles) + Phase 2.3 (production apply incl. ECR repos) + Phase 2.4 (secrets populate) all ✅ DONE.

---

## Purpose

Phase 3 là **lần đầu tiên OIDC-driven role thực sự fire ở chế độ state-mutating** (push image lên ECR). Trước đó chỉ có `terraform-plan-role` chạy read-only ở PR CI (per Phase 2.2 verification PR #992).

Mục tiêu Phase 3:

1. Verify trust policy chain: GitHub Actions OIDC token → AWS STS → `ecr-push-role` → ECR `PutImage` thực thi sạch end-to-end.
2. Smoke-test 10 ECR repos đã được Phase 2.3 tạo (kite/kitehub-* + kite/kiteclass-*) chấp nhận multi-arch image push.
3. Verify Trivy scan + Cosign keyless sign + SBOM Syft hoạt động trên CI runner production.
4. Establish baseline cho mọi production release sau này — nếu Phase 3 sạch, CI tag-driven flow tin được.

Nếu Phase 3 fail, KHÔNG được tiến vào Phase 4 staging E2E gate.

---

## Pre-flight checklist

### GitHub Variables (set ở repo Settings → Secrets and variables → Actions → Variables tab)

Các Variables sau PHẢI đã set theo `release-1-deploy-runbook.md` §2.2:

- [ ] `AWS_CONFIGURED=true`
- [ ] `AWS_REGION=ap-southeast-1`
- [ ] `ECR_REGISTRY=906286017800.dkr.ecr.ap-southeast-1.amazonaws.com`
- [ ] `TERRAFORM_STATE_BUCKET=kitehub-terraform-state-906286017800` (đã set Phase 2.1, không trực tiếp dùng cho Phase 3 nhưng sanity check)

Verify:

```bash
gh variable list --repo VictorAurelius/2026-Kite-Class-Platform
```

Expected output: 4 Variables trên (giá trị ECR_REGISTRY phải match account ID 906286017800 từ `terraform output`).

### OIDC roles (Phase 2.2 + GAP-436)

Verify 3 roles tồn tại ở account 906286017800 (định nghĩa ở [`infrastructure/terraform-aws/iam.tf`](../../../infrastructure/terraform-aws/iam.tf), trust policy claim `repo:VictorAurelius/2026-Kite-Class-Platform:*`):

```bash
aws iam list-roles --query "Roles[?starts_with(RoleName, 'terraform-plan-role') || starts_with(RoleName, 'ecr-push-role') || starts_with(RoleName, 'deploy-staging-role')].RoleName" --output table
```

Expected: 3 rows — `terraform-plan-role`, `ecr-push-role`, `deploy-staging-role`.

Reference: PR #993 (GAP-436 Phase 1+2+3).

### ECR repos (Phase 2.3 — PR #994)

10 ECR repos đã được Phase 2.3 apply tạo (per `infrastructure/terraform-aws/ecr.tf`):

| # | Repo |
|---|------|
| 1 | `kite/kitehub-subscription` |
| 2 | `kite/kitehub-gateway` |
| 3 | `kite/kitehub-branding` |
| 4 | `kite/kitehub-admin` |
| 5 | `kite/kitehub-email` |
| 6 | `kite/kitehub-platform` |
| 7 | `kite/kitehub-frontend` |
| 8 | `kite/kiteclass-core` |
| 9 | `kite/kiteclass-gateway` |
| 10 | `kite/kiteclass-frontend` |

Verify:

```bash
aws ecr describe-repositories --region ap-southeast-1 \
  --query "repositories[?starts_with(repositoryName, 'kite/')].repositoryName" \
  --output table
```

Expected: 10 rows. Nếu thiếu → re-run Phase 2.3 targeted apply trên `aws_ecr_repository.*` resources.

> **Note:** `release-1-deploy-runbook.md` §2.3.5 ghi "9 repos" — số thực tế là 10 (bao gồm cả `kite/kiteclass-gateway`). Sub-PR follow-up sẽ sync con số trong runbook gốc.

### Workflow file

- [ ] `.github/workflows/docker-build-push.yml` exists (Wave 37 Bucket B)
- [ ] Workflow trigger có `tags: ['v*']` (kích hoạt khi push tag `v*`)
- [ ] Workflow assumes `ecr-push-role` qua `aws-actions/configure-aws-credentials@v4` với `role-to-assume: arn:aws:iam::906286017800:role/ecr-push-role`

```bash
grep -E "tags:|role-to-assume:" .github/workflows/docker-build-push.yml
```

---

## Trigger sequence

User trigger ad-hoc test tag (per `agent-aws-access.md` Tier 3 — user-only mutation):

```bash
git tag v0.9.0-beta-staging.1
git push origin v0.9.0-beta-staging.1
```

Tag pattern `v0.9.0-beta-staging.<N>` để phân biệt với production tags `v1.0.0+`. Bump `<N>` mỗi lần re-test trong Phase 3.

---

## Monitor

```bash
# Watch real-time
gh run watch

# OR query latest run
gh run list --workflow=docker-build-push.yml --limit 1
gh run view <run-id> --log
```

Expected: ~10–15 min total (10 services × 2 archs amd64 + arm64 = 20 builds matrix-parallel; build cache trên `ghcr.io` cache).

Per `mcp-first-with-fallback.md`, GitHub MCP `list_workflow_runs` là primary; `gh run` là fallback CLI.

---

## Verification

### 5.1 ECR images present

Cho mỗi repo trong 10 repos `kite/<service>`:

```bash
for repo in kite/kitehub-subscription kite/kitehub-gateway kite/kitehub-branding \
            kite/kitehub-admin kite/kitehub-email kite/kitehub-platform \
            kite/kitehub-frontend kite/kiteclass-core kite/kiteclass-gateway \
            kite/kiteclass-frontend; do
  echo "=== $repo ==="
  aws ecr describe-images --repository-name "$repo" --region ap-southeast-1 \
    --query "imageDetails[?imageTags!=null]|[].imageTags" --output table
done
```

Expected mỗi repo: 2 tags

- `v0.9.0-beta-staging.1`
- `latest`

Nếu repo nào thiếu image → check workflow logs cho service tương ứng (matrix job naming `build-<service>`).

### 5.2 Trivy scan results

Trivy chạy post-build trong workflow. Xem trong `gh run view <id> --log` filter theo step `Trivy scan`:

- HIGH/CRITICAL CVE → workflow fail (Wave 37 Bucket B policy)
- Initial run cho image base mới có thể có vài HIGH known-issue → document exception process bên dưới
- MEDIUM/LOW → log only, không fail

**Trivy exception process** (nếu fail HIGH/CRITICAL ở first run):

1. Xem CVE ID + affected package trong log
2. Check upstream fix availability (e.g., `apk upgrade` cho Alpine, `apt upgrade` cho Debian base)
3. Nếu fix có → update Dockerfile base image hoặc thêm package upgrade step → re-tag
4. Nếu fix CHƯA có → file follow-up gap, add `.trivyignore` với CVE ID + expiry date (max 30 ngày), commit, re-tag
5. KHÔNG được skip Trivy bằng `--exit-code 0` permanently

### 5.3 Cosign signature verify

Workflow `docker-build-push.yml` Wave 37 Bucket B đã wire Cosign keyless sign post-push. Verify command (có thể defer actual verify run cho Phase 4):

```bash
# Pull image digest first
DIGEST=$(aws ecr describe-images --repository-name kite/kitehub-gateway \
  --region ap-southeast-1 \
  --image-ids imageTag=v0.9.0-beta-staging.1 \
  --query 'imageDetails[0].imageDigest' --output text)

# Verify keyless signature
cosign verify \
  --certificate-identity-regexp "https://github.com/VictorAurelius/2026-Kite-Class-Platform/.github/workflows/docker-build-push.yml@.*" \
  --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
  906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/kitehub-gateway@${DIGEST}
```

Expected: `Verified OK` + transparency log entry. Defer cho Phase 4 nếu Phase 3 chỉ cần smoke test "image push được".

### 5.4 SBOM artifact

Workflow uploads SBOM (Syft CycloneDX format) làm GitHub Actions artifact. Verify:

```bash
gh run view <run-id> --log | grep -i "syft\|sbom"
gh run download <run-id> --name sbom-* --dir /tmp/sbom
ls /tmp/sbom/
```

Expected: 10 SBOM JSON files (1 per service).

---

## Failure modes + fixes

### F1. OIDC trust policy mismatch

**Symptom:** Workflow log shows `AssumeRoleWithWebIdentity ... AccessDenied: Not authorized to perform sts:AssumeRoleWithWebIdentity`.

**Root cause:** Trust policy `subject` claim không match. Pattern phải là `repo:VictorAurelius/2026-Kite-Class-Platform:*` (any branch/tag) hoặc tighter `repo:VictorAurelius/2026-Kite-Class-Platform:ref:refs/tags/v*` (tags only).

**Fix:**

1. Check current trust policy ở [`infrastructure/terraform-aws/iam.tf`](../../../infrastructure/terraform-aws/iam.tf) — search resource `aws_iam_role.ecr_push_role` → `assume_role_policy`
2. Verify `Condition.StringLike."token.actions.githubusercontent.com:sub"` pattern
3. Nếu sai → edit `iam.tf` → `terraform plan -target=aws_iam_role.ecr_push_role` → user-confirm → `terraform apply` (per `terraform-apply-retry-reconfirm.md`)
4. Re-tag (bump `.2`) → re-trigger workflow

### F2. ECR repo missing

**Symptom:** Workflow log shows `RepositoryNotFoundException: The repository with name 'kite/<service>' does not exist`.

**Root cause:** Phase 2.3 apply skipped 1+ repo (e.g., partial state from mid-apply failure).

**Fix:**

1. Verify count: `aws ecr describe-repositories --region ap-southeast-1 --query 'repositories[?starts_with(repositoryName, "kite/")] | length(@)'` → should be 10
2. Nếu < 10 → identify missing → `cd infrastructure/terraform-aws && terraform plan -target='aws_ecr_repository.repos["<missing>"]'` → user-confirm → `terraform apply` (mirror Phase 2.3 protocol)
3. Re-tag → re-trigger

### F3. Trivy CRITICAL CVE blocks merge

**Symptom:** `Trivy scan` step fails with HIGH/CRITICAL CVE list.

**Fix path A — base image upgrade:** Update `FROM ...` line trong Dockerfile của service affected → push commit → re-tag.

**Fix path B — package upgrade in Dockerfile:** Add `RUN apk upgrade --no-cache` (Alpine) hoặc `RUN apt-get update && apt-get upgrade -y` (Debian) ngay trước `USER` directive.

**Fix path C — `.trivyignore` (last resort):** Nếu CVE chưa có upstream fix, document exception:

```
# .trivyignore
# CVE-2026-XXXXX - <package> <version> - no upstream fix yet, expires 2026-06-07
# Tracked: GAP-XXX
CVE-2026-XXXXX
```

Plus follow-up gap referencing `agent-aws-access.md` Tier 3 review process.

### F5. Multi-arch build fails — base image manifest missing arch

**Symptom:** Workflow log: `ERROR: failed to solve: <base-image>: failed to resolve source metadata for docker.io/library/<image>: no match for platform in manifest: not found`. Build stage fails before Push to ECR. OIDC AssumeRole + ECR login already succeeded.

**Worked example (2026-05-07 first OIDC trigger, run #25527705091):** 7 Java services failed because `maven:3.9-eclipse-temurin-{17,21}-alpine` (Docker Hub) only publishes amd64 manifest, no arm64. Workflow declared `platforms: linux/amd64,linux/arm64` → buildx tried to pull arm64 variant → not found.

**Root cause check (2 commands):**

```bash
# 1. List declared platforms in workflow push step
grep -nE "^[[:space:]]+platforms:" .github/workflows/docker-build-push.yml

# 2. For each base image (FROM line), verify Docker Hub manifest supports declared archs
docker buildx imagetools inspect maven:3.9-eclipse-temurin-17-alpine 2>&1 | grep Platform
# If output lacks "linux/arm64" line → image incompatible with multi-arch
```

**Fix options:**

| Option | Pro | Con |
|---|---|---|
| **A. Drop arm64 (single-arch)** | 1-line workflow change; matches Phase 1 BETA EC2 t3.medium = amd64 per ADR-025 | Mất arm64 path khi cần Graviton migration Phase 2+ |
| **B. Switch base image** to non-alpine variant (e.g., `maven:3.9-eclipse-temurin-17` without `-alpine`) | Giữ multi-arch | 7 Dockerfile sửa; image size +50MB; runtime stage cũng phải đổi |
| **C. Use multi-arch alpine variant** (e.g., `eclipse-temurin:17-jdk-alpine` — bare JDK without maven wrapper) | Multi-arch + alpine size | Phải tự install Maven; ~1 line `RUN apk add maven` |

Recommend **A** cho Phase 1 BETA. Restore arm64 ở Phase 2+ Graviton timeline.

**Pre-flight prevention (manual gotcha — formal CI check deferred per `incident-to-rule-pipeline.md` premature-rule guard):**

Trước khi merge PR thay đổi `platforms:` trong `docker-build-push.yml` HOẶC thêm/đổi base image trong bất kỳ Dockerfile, chạy `docker buildx imagetools inspect <base>` cho mọi base image trong matrix. Verify từng image có manifest cho mọi platform được declare. Frequency: gần như 0 (1-time issue 2026-05-07; arm64 hiện disabled). Escalate sang CI script + rule formal nếu 2nd recurrence.

---

### F4. Cosign sign fails (OIDC token expiry)

**Symptom:** Workflow log shows `error signing image: getting signer: getting Fulcio signer: ...` near end of run.

**Root cause:** GitHub Actions OIDC token có TTL ~6 phút; nếu build mất quá lâu (e.g., cold cache + 10 services), Cosign sign step có thể chạy sau khi token expired.

**Fix:**

1. Check job duration — nếu > 15 min → consider splitting matrix (5 services / job × 2 jobs)
2. Verify `id-token: write` permission ở job level (workflow Wave 37 Bucket B should already have it)
3. Re-trigger nếu transient — KHÔNG được skip cosign sign

---

## Post-success actions

### 6.1 Trigger Bucket E checklist (GAP-436 Phase 4)

Sau khi Phase 3 xanh end-to-end, OIDC chain đã verified production-ready. Bucket E của Wave 42 sẽ:

- [ ] Remove static `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` từ GitHub Secrets (deprecated bởi OIDC)
- [ ] Verify 3 GitHub Variables (`AWS_CONFIGURED`, `AWS_REGION`, `ECR_REGISTRY`) là duy nhất AWS-related config
- [ ] Update `release-1-deploy-runbook.md` Phase 2.2 closure note với "OIDC verified Phase 3 passed"
- [ ] Memory `feedback_agent_aws_readonly_logging.md` (GAP-438 Phase 4) reinforces OIDC pattern

### 6.2 Re-verify AWS state via smoke script

Run smoke-aws Phase 2.3 verification script (Bucket B output of Wave 42):

```bash
bash scripts/smoke-aws-phase-2-3.sh
```

Expected: 0 unexpected resources, 10 ECR repos populated, no IAM access drift since Phase 2.3 apply.

Per `agent-aws-access.md` §5, output saves automatically to `documents/04-quality/audits/aws-verification/2026-05-07-phase-3-image-push.md`.

### 6.3 Update wave plan + ROADMAP

Per `feedback_post_merge_doc_sync.md`:

- [ ] `documents/03-planning/roadmap/release-1-deploy-runbook.md` Phase 3 checkboxes 3.1-3.4 → all ✅
- [ ] `documents/04-quality/gaps/ROADMAP.md` § Status Snapshot — add "Phase 3 image push verified $(date +%F) — 10 services × 2 archs"
- [ ] Wave 42 plan flip `status: complete` if Phase 3 closes the wave

---

## Cross-references

- [`release-1-deploy-runbook.md`](../../03-planning/roadmap/release-1-deploy-runbook.md) §3 — parent runbook (this file is the detailed expansion of §3.1-3.4)
- [`secrets-populate-phase-2-4.md`](secrets-populate-phase-2-4.md) — predecessor runbook (Phase 2.4 must close before Phase 3)
- [GAP-436](../../04-quality/gaps/GAP-436-oidc-deploy-roles.md) — OIDC role provisioning (Phase 1+2+3 PR #993; Phase 4 follows post-Phase-3 success)
- [`.claude/rules/agent-aws-access.md`](../../../.claude/rules/agent-aws-access.md) — Tier 3 verification framework (this runbook follows Tier 1 read-only verification + user-trigger Tier 3 mutation)
- [`.claude/rules/release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §3.1 PRE-RELEASE — `v0.9.0-beta-staging.*` tag falls under PRE-RELEASE subset
- [`infrastructure/terraform-aws/iam.tf`](../../../infrastructure/terraform-aws/iam.tf) — OIDC trust policy source of truth
- [`infrastructure/terraform-aws/ecr.tf`](../../../infrastructure/terraform-aws/ecr.tf) — 10 ECR repos defined here
- [`.github/workflows/docker-build-push.yml`](../../../.github/workflows/docker-build-push.yml) — workflow under test in Phase 3

---

## Out of scope

- Production tag deploy (`v1.0.0+`) — that's Phase 7 GA gate
- Helm chart push to OCI registry — Phase 4+ scope
- Cosign attestation verification by EC2 user-data pull — Phase 4 SSM deploy validates
- Multi-region image replication — Phase 1 BETA single-region `ap-southeast-1` only
