# GAP-374: Tag-based Release Automation CI Workflow

**Status:** 🟡 PARTIAL — workflow + changelog generator shipped Wave 38 Bucket A; notification channel + live tag end-to-end test deferred to follow-up GAP-NEW (post-Phase-1-BETA when first real `v0.9.0-beta` tag fires)
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA + 1.5 PAID)
**Domain:** DevOps / CI/CD
**Found:** 2026-05-06 (Release 1 deploy plan; per `versioning-policy.md` §14)
**Affects:** Release process consistency, deployment automation

## Problem

KHÔNG có CI workflow auto-trigger trên git tag push. Hiện tại deploy-production.yml chỉ manual `workflow_dispatch`. Khi tag `v1.0.0` push, không có automation:
- Build production Docker images
- Push to registry
- Generate changelog
- Create GitHub Release
- Notify deployment channel

## Proposed Fix

`.github/workflows/release-tag.yml`:

```yaml
name: Release on tag
on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+.[0-9]+*'  # vX.Y.Z + pre-release tags

permissions:
  contents: write  # for GitHub Release creation
  packages: write  # for registry push

jobs:
  validate-tag:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.extract.outputs.version }}
      is_prerelease: ${{ steps.extract.outputs.is_prerelease }}
    steps:
      - id: extract
        run: |
          VERSION=${GITHUB_REF#refs/tags/}
          IS_PRE=$([[ "$VERSION" =~ -[a-z]+ ]] && echo "true" || echo "false")
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "is_prerelease=$IS_PRE" >> $GITHUB_OUTPUT

  build-images:
    needs: validate-tag
    runs-on: ubuntu-latest
    steps:
      - checkout
      - setup-buildx
      - login-to-ecr
      - build-multi-arch (linux/amd64, linux/arm64)
        # tag images with version + 'latest' (only if !is_prerelease)
      - push-to-registry

  generate-changelog:
    needs: validate-tag
    runs-on: ubuntu-latest
    steps:
      - checkout
      - generate-changelog-from-commits-since-previous-tag
        # parse conventional commits + version-bump hints from versioning-policy §9
      - create-github-release
        # use changelog as body
        # mark as prerelease if is_prerelease

  notify:
    needs: [build-images, generate-changelog]
    runs-on: ubuntu-latest
    steps:
      - notify-deploy-channel (Slack/Discord/email)
```

## Acceptance Criteria

- [x] Workflow file created `.github/workflows/release-tag.yml`
- [x] Tag pattern matches: `v0.9.0-beta`, `v0.9.0-beta.1`, `v1.0.0-rc.1`, `v1.0.0`, `v1.0.1`, `v2.0.0`, etc.
- [x] Multi-arch Docker build (amd64 + arm64) — delegated to existing `docker-build-push.yml` which already triggers on `tags: 'v*.*.*'` (Wave 37 Bucket B). Scope split documented in `versioning-policy.md` §6.4.
- [x] Push to ECR (ap-southeast-1) với version + latest tags — delegated to `docker-build-push.yml` (no Docker Hub per ADR-025).
- [x] Changelog generation from conventional commits since previous tag — `scripts/generate-changelog.sh` (bash + awk classifier covering feat / fix / docs / refactor / perf / style / build / ci / sec / revert / breaking-change marker)
- [x] GitHub Release created với changelog body — idempotent (`gh release edit` if exists, else `gh release create`)
- [x] Pre-release tags marked correctly — regex `-(alpha|beta|rc)` → `--prerelease` flag
- [ ] Deploy notification (decide channel) — **DEFERRED** to follow-up gap. Workflow ships placeholder `notify` job logging release state; channel selection (Slack / Discord / email) still open per §Open decisions.
- [ ] Self-test: push test tag, verify workflow runs end-to-end — **DEFERRED**. Synthetic-fixture self-test of changelog parser shipped (`bash scripts/generate-changelog.sh --self-test` PASS, classifies 10 conventional commits into 7 sections). Live end-to-end requires actual tag push — first real `v0.9.0-beta` tag will exercise the pipeline; test coverage at that point.
- [x] `versioning-policy.md` §6 Release process updated với automated steps — added §6.4 Automated release pipeline (CI) cross-referencing both workflows.

## Open decisions

- Notification channel (Slack? Discord? Email?)
- Changelog format (Keep a Changelog vs custom)
- Auto-deploy to staging on `-rc` tag?
- Auto-deploy to production on `vX.Y.Z` (NO prefix) — recommend NO, keep manual confirm

## Effort estimate

~1-2 ngày YAML + testing.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` §7
- Versioning: `documents/03-planning/roadmap/versioning-policy.md` §6 + §9
- Existing workflows: `.github/workflows/deploy-{staging,production}.yml`
- Sister: GAP-375 (GitHub Release template + changelog folder)

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-07** (Wave 38 Bucket A): PARTIAL. Shipped `.github/workflows/release-tag.yml` + `scripts/generate-changelog.sh` (with `--self-test`) + `versioning-policy.md` §6.4 cross-link. Image push intentionally delegated to existing `docker-build-push.yml` (Wave 37 Bucket B already covers `tags: 'v*.*.*'` trigger với Trivy + SBOM + Cosign) — scope split documented. Deferred: (a) notification channel selection (Slack / Discord / email TBD), (b) live end-to-end test against real tag push (requires actual `v0.9.0-beta` tag — first BETA tag will exercise pipeline). Both deferrals tracked under follow-up gap to be filed alongside Wave 38 closure. Per `gap-done-discipline.md` §3 PARTIAL exit ramp — 2 of 10 AC items unchecked → status remains PARTIAL.
- **2026-05-06:** Filed by Release 1 deploy plan PR. Per versioning-policy §14 open item. STRONGLY recommend Phase 1 BETA — manual tag deploy works but automation prevents drift + ensures consistent release artifacts.
