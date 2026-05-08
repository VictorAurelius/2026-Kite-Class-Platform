# Known Failure Modes — Catalog of past Phase 1-3 incidents this skill prevents

Append-only history. Each entry = 1 incident with date, root cause, category that would have caught it, and link to fix.

---

## 2026-05-07 — Phase 3 first OIDC trigger (run #25527705091, tag v0.9.0-beta-staging.1)

**Symptom:** 7/9 Push to ECR jobs failed mid-build with `ERROR: failed to solve: maven:3.9-eclipse-temurin-17-alpine: failed to resolve source metadata for docker.io/library/maven:3.9-eclipse-temurin-17-alpine: no match for platform in manifest: not found`.

**Root cause:** Workflow `docker-build-push.yml:284` declared `platforms: linux/amd64,linux/arm64` but Java services use `maven:3.9-eclipse-temurin-{17,21}-alpine` base images which Docker Hub publishes amd64 manifest only.

**Catch category:** #1 Multi-arch base image manifest

**Detection signature:**
```bash
docker buildx imagetools inspect maven:3.9-eclipse-temurin-17-alpine | grep "Platform:" | grep arm64
# Empty output = arm64 missing
```

**Fix:** PR #1004 — drop arm64 from push step, keep amd64-only (matches Phase 1 BETA EC2 t3.medium per ADR-025).

---

## 2026-05-07 — Phase 3 retry (run #25528087813, tag v0.9.0-beta-staging.2)

**Symptom:** 8/8 Push to ECR jobs failed with `ERROR: failed to push 906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/kiteclass-gateway:0.9.0-beta-staging.2: unexpected status from HEAD request to https://906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/v2/kite/kiteclass-gateway/blobs/sha256:...: 403 Forbidden`. OIDC AssumeRole succeeded (`Authenticated as assumedRoleId AROA5GAW3FUEJPF24JPDT:GitHubActions`); ECR login succeeded; HEAD blob check denied.

**Root cause:** `infrastructure/terraform-aws/iam.tf:312` IAM policy Resource pattern was `arn:aws:ecr:.../repository/${var.project_name}-*` → with `project_name="kitehub"` expanded to `repository/kitehub-*`. Actual ECR repos use `kite/` namespace prefix per `ecr.tf:8-21` (e.g., `kite/kitehub-subscription`, `kite/kiteclass-gateway`). ARN `repository/kite/kitehub-subscription` does NOT match wildcard `kitehub-*`.

**Catch category:** #2 IAM resource ARN naming drift

**Detection signature:**
```bash
# Extract Resource patterns from IAM policy + actual ECR repo names from locals
TF_RESOURCE=$(grep -A1 'BatchCheckLayerAvailability' infrastructure/terraform-aws/iam.tf | grep Resource)
ECR_REPOS=$(grep -E '^\s+"kite/' infrastructure/terraform-aws/ecr.tf)
# Cross-check: TF_RESOURCE wildcard expansion must match every ECR_REPO entry
```

**Fix:** PR #1005 — change Resource pattern to `repository/kite/*` (least-privilege, matches all 10 ECR repos).

---

## 2026-05-07 — GitHub Variable AWS_CONFIGURED missing pre-Phase-3

**Symptom:** Initial Phase 3 trigger plan blocked because workflow `docker-build-push.yml:187` `if: vars.AWS_CONFIGURED == 'true'` evaluated false (var not set) → Push to ECR job skipped silently. User unaware until checking `gh run list` showed only Build Test jobs ran.

**Root cause:** GitHub Variable `AWS_CONFIGURED` not yet created on repo. Workflow conditional silently skips when condition false.

**Catch category:** #6 Workflow `if:` condition coverage + #7 GitHub Variables pre-flight

**Detection signature:**
```bash
# Enumerate vars referenced in workflow if-conditions
grep -E "if:.*vars\." .github/workflows/docker-build-push.yml | grep -oE 'vars\.[A-Z_]+' | sort -u
# vs
gh api repos/<owner>/<repo>/actions/variables --jq '.variables[].name' | sort -u
# diff = missing variables
```

**Fix:** Coordinator set `gh api -X POST .../actions/variables -f name=AWS_CONFIGURED -f value=true` before tag push.

---

## 2026-05-07 — populate-secrets.sh secret naming mismatch (Wave 42 Bucket C)

**Symptom:** Bucket C agent self-flagged in PR #1001 body: "Spec secret names use slash-segmented paths (`kite/prod/db/password`); the existing runbook references hyphen-segmented names from terraform (`kite/prod/rds-password`). Script follows the SPEC names verbatim. If the actual terraform-created secret names differ, follow-up PR will need either renaming or a name-mapping table."

**Root cause:** Naming convention drift between Bucket C spec, runbook reference, and terraform reality. Script uses one format; terraform-created secrets may use another → script `put-secret-value` against non-existent secret ID would fail with `ResourceNotFoundException`.

**Catch category:** #4 Secret naming drift

**Detection signature:**
```bash
# Cross-grep all 3 sources
TERRAFORM_NAMES=$(grep -E '"kite/' infrastructure/terraform-aws/secrets.tf | grep -oE '"[^"]+"')
SCRIPT_NAMES=$(grep -E 'kite/prod' scripts/populate-secrets.sh | grep -oE 'kite/[^"]+')
RUNBOOK_NAMES=$(grep -E '\`kite/' documents/05-guides/deploy/secrets-populate-phase-2-4.md | grep -oE '`kite/[^`]+`')
diff <(sort -u <<< "$TERRAFORM_NAMES") <(sort -u <<< "$SCRIPT_NAMES")  # diff = drift
```

**Fix:** Pending Phase 2.4 user-action verification; if mismatch confirmed, either rename terraform OR add mapping in script.

---

## 2026-05-07 — Phase 2.3 mid-apply em-dash failure

**Symptom:** Terraform apply #1 failed at `aws_security_group.ai_outbound`: `InvalidParameterValue: Character sets beyond ASCII are not supported`. SG description copy-pasted from architecture markdown contained em-dash `—` (U+2014).

**Root cause:** `aws_security_group.description` field uniquely restricted to ASCII; other AWS fields accept Unicode; Terraform passes through verbatim; AWS API rejects.

**Catch category:** Out-of-scope for this skill (covered by `.claude/rules/aws-sg-description-ascii.md` reviewer-checklist + memory auto-load). Listed here for historical completeness.

**Fix:** PR replaced em-dash with hyphen-minus; retry apply succeeded.

---

## Update protocol

When new deploy-time incident occurs:
1. Add entry above with date + symptom + root cause + catch category + detection signature + fix link
2. If incident root cause is NEW class (not in catalog of 8 categories) → discuss adding 9th category to `check-catalog.md`
3. Tag entry with `[in-skill]` (skill catches it) or `[out-of-scope]` (handled by other rule/skill)

This file = source of truth for "what classes of mistakes have we hit and which are now caught preflight."
