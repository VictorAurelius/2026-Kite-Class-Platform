# GAP-218: PDF font-missing runbook + container image-build validation

**Status:** 🟢 DONE 100%
**Priority:** 🔴 P0 — blocks Sub-PR 5.6b per Wave 5 plan §4 "5.6a P0 → block 5.6b" policy
**Domain:** Ops / Deploy
**Found:** 2026-04-25 (Wave 5 audit suite — ops-readiness audit finding #2)
**Affects:** All PDF generation in production (any tenant, any template)

## Problem

`InvoiceRenderer` preloads `DejaVuSans` (regular + bold) from `kiteclass-core/src/main/resources/fonts/`. If the font file is missing from a built container image (Maven `target/classes/fonts/` not packaged correctly, Dockerfile `COPY` step misses the path, or future `.jar` repackaging strips resources), the FIRST PDF render call throws:

```java
// InvoiceRenderer.java:131
throw new IllegalStateException(
    "Font resource not found on classpath: " + path
        + ". Expected bundled under kiteclass-core/src/main/resources/fonts/.");
```

This becomes an HTTP 500 to the caller. There is currently:
1. **No image-build validation step** that asserts `fonts/DejaVuSans.ttf` and `fonts/DejaVuSans-Bold.ttf` are present in the produced `kiteclass-core` image.
2. **No runbook** in `documents/05-guides/runbooks/` instructing on-call how to diagnose + remediate.

Production failure mode: tenant submits invoice generation request → 500 → on-call paged via generic `HighErrorRate` alert (GAP-217) → log shows `IllegalStateException: Font resource not found` → on-call has to reverse-engineer what font, where it lives, why it's missing, and how to redeploy.

## Root Cause

Wave 5 Sub-PR 5.1 added the font dependency without adding a deploy-time guarantee. Maven `package` includes `src/main/resources/**` by default, but Dockerfile or CI pipeline could be misconfigured. Test suites don't catch image-level packaging bugs.

## Proposed Fix

### Part A — image-build validation (CI step)

Add a verification step to the GitHub Actions workflow that builds the kiteclass-core image:

```yaml
- name: Verify bundled fonts in built image
  run: |
    docker run --rm --entrypoint sh kiteclass-core:test -c \
      'unzip -l app.jar | grep -E "fonts/DejaVuSans(-Bold)?\.ttf" | wc -l' \
    | grep -q '^2$' || { echo "FAIL: DejaVuSans TTF (regular + bold) missing from app.jar"; exit 1; }
```

(Path / jar name to be adjusted to match actual build output.)

Optionally add an `ApplicationListener<ApplicationReadyEvent>` smoke check in `kiteclass-core` that `getResourceAsStream("/fonts/DejaVuSans.ttf")` returns non-null at startup, failing fast on misconfigured deploy.

### Part B — runbook

Create `documents/05-guides/operations/runbooks/pdf-generation-font-not-found.md`:

```markdown
# Runbook: PDF generation fails with "Font resource not found"

## Symptom
HTTP 500 from POST /api/v1/documents/pdf/{preview|download}.
Stack trace contains: IllegalStateException: Font resource not found on classpath: /fonts/DejaVuSans.ttf

## Immediate triage (≤5 min)
1. Confirm scope: which pods/instances? `kubectl logs -l app=kiteclass-core | grep "Font resource not found"`
2. Verify image content: `kubectl exec -it <pod> -- unzip -l /app/app.jar | grep DejaVuSans`
   - Expected: 2 entries (regular + bold)
   - If missing: image is bad → roll back to last known-good image

## Root cause investigation
- `git log -- kiteclass/kiteclass-core/src/main/resources/fonts/` — was the font deleted?
- Dockerfile changes affecting resource bundling?
- Maven plugin order changes that exclude resources?

## Permanent fix
- Re-add font OR fix bundling
- Add image-build validation (GAP-218 Part A) if not already in CI
- Re-deploy

## Prevention
- CI step must verify font presence post-build
- Smoke endpoint at `/actuator/health/font-resources` (custom HealthIndicator) — future enhancement
```

## Acceptance Criteria

- [ ] CI job for kiteclass-core image build adds font-presence verification step (fails the build if either TTF is missing from the JAR)
- [ ] Runbook committed at `documents/05-guides/operations/runbooks/pdf-generation-font-not-found.md`
- [ ] Runbook is referenced from GAP-217 alert rule annotations (`runbook:` field)
- [ ] Optional: startup smoke check via `ApplicationReadyEvent` listener that asserts font resources are loadable
- [ ] Optional: custom `HealthIndicator` exposed at `/actuator/health/font-resources`

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-25-wave5.md`
- Code: `kiteclass-core/src/main/java/com/kiteclass/core/module/document/pdf/InvoiceRenderer.java:128–136`
- GAP-217: alert rules reference this runbook
- GAP-214: parent audit suite gap

## Log

- **2026-04-25:** Filed from Wave 5 audit suite (ops audit finding #2). P0 because the failure mode is silent until first prod call → user-visible 500 → on-call has no playbook. CI validation + runbook = ~1h work, hard requirement before Sub-PR 5.6b ships per "P0 → block 5.6b" wave policy.

- **2026-05-26 (Wave br-7 Bucket D PR #1842 — Dockerfile font assertion + runbook already shipped Wave 5 Sub-PR 5.6b; 1-line Dockerfile comment path fix closure):** Flipped DONE 100% — . CSV row updated + file moved to phase-1-beta/closed/ per `gap-done-discipline.md` §2.
