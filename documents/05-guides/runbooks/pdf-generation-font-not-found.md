# Runbook: PDF generation fails — "Font resource not found"

**Closes:** GAP-218 (runbook half)
**Severity when fired:** P0 — every `POST /api/v1/documents/pdf/{preview|download}` request returns 500 to the tenant
**Audit reference:** `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-25-wave5.md` finding #2

---

## Symptom

HTTP 500 from `POST /api/v1/documents/pdf/{preview|download}`. Application logs contain:

```
java.lang.IllegalStateException: Font resource not found on classpath: /fonts/DejaVuSans.ttf.
    Expected bundled under kiteclass-core/src/main/resources/fonts/.
    at com.kiteclass.core.module.document.pdf.InvoiceRenderer.openClasspathFont(...)
```

(Or the `-Bold` variant.) Vietnamese diacritics in invoice headers are the only path that loads these fonts; non-PDF formats (xlsx, docx) are unaffected.

## Immediate triage (≤5 min)

1. **Confirm scope** — which pods / instances affected?
   ```bash
   kubectl logs -l app=kiteclass-core --tail=200 | grep -i "Font resource not found"
   ```
2. **Verify image content** — is the font actually in the deployed jar?
   ```bash
   kubectl exec -it <pod> -- jar tf /app/app.jar | grep -i DejaVuSans
   ```
   - Expected output: 2 entries — `fonts/DejaVuSans.ttf` and `fonts/DejaVuSans-Bold.ttf`
   - If 0 or 1 entries → image is broken → **roll back to last known-good image** while diagnosing
3. **Check image build provenance** — what commit / branch / tag built this image?
   ```bash
   kubectl describe pod <pod> | grep -E "image:|COMMIT_HASH|BUILD_TAG"
   ```

## Roll back

```bash
# Helm — set image tag back to last green
helm upgrade kiteclass-core ./infrastructure/helm/kiteclass-core \
  --set image.tag=<previous-known-good-tag> --reuse-values

# Verify
kubectl rollout status deployment/kiteclass-core
kubectl logs -l app=kiteclass-core --tail=50 | grep -i "Font resource"  # should be empty
```

If the rollback target also lacks the font (regression has been masked for multiple deploys), escalate to **font-bundling-broken-in-CI** root-cause investigation below.

## Root-cause investigation

After rolling back, find why fonts went missing in the bad build:

| Check | Command | What to look for |
|-------|---------|-----------------|
| Were TTFs deleted from source? | `git log --all -- kiteclass/kiteclass-core/src/main/resources/fonts/` | Recent `D` (delete) entries |
| Did Dockerfile change? | `git log -p -- kiteclass/kiteclass-core/Dockerfile` | `COPY` step or `mvn package` flags affecting resources |
| Did Maven plugin order change? | `git log -p -- kiteclass/kiteclass-core/pom.xml` | `maven-jar-plugin`, `maven-resources-plugin`, `maven-shade-plugin` exclusions |
| Did the image-build CI job catch it? | Check the workflow log for the bad build | Should see "FAIL: DejaVuSans TTF (regular + bold) missing from app.jar" if our GAP-218 check fired |

The Dockerfile contains a defense-in-depth assertion (added in Sub-PR 5.6b under GAP-218) that fails the image build if either TTF is missing from `app.jar`. If a broken image still reached production, that check has a hole — file a follow-up gap.

## Permanent fix paths

| Cause | Fix |
|-------|-----|
| Font deleted from source | Restore from git; add code-owner protection on `kiteclass-core/src/main/resources/fonts/` |
| Dockerfile resource-bundling regression | Revert the bad Dockerfile change; re-test image-build job |
| Maven plugin exclusion drift | Add explicit `<include>fonts/*.ttf</include>` to maven-resources-plugin config |
| CI image-build check disabled | Restore the `RUN test "$(jar tf ...)"` step to Dockerfile (Sub-PR 5.6b lineage) |

After fix: re-build image, re-deploy, verify `kubectl exec ... jar tf /app/app.jar | grep DejaVuSans` shows 2 entries.

## Prevention (already shipped + suggested)

- ✅ Image-build assertion in `kiteclass/kiteclass-core/Dockerfile` (GAP-218 — Sub-PR 5.6b) — fails the build if either TTF is missing.
- ⏳ Optional follow-up: `ApplicationListener<ApplicationReadyEvent>` smoke check that `getResourceAsStream("/fonts/DejaVuSans.ttf")` returns non-null at startup, failing fast on misconfigured deploy. (Not in 5.6b scope — file a follow-up gap if desired.)
- ⏳ Optional follow-up: custom `HealthIndicator` exposed at `/actuator/health/font-resources` so liveness/readiness probes catch the issue before traffic routes to a bad pod.

## Related

- Code: `kiteclass-core/src/main/java/com/kiteclass/core/module/document/pdf/InvoiceRenderer.java:128–136`
- Build: `kiteclass-core/Dockerfile` (font-presence assertion)
- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-25-wave5.md`
- Gap: GAP-218
- Wave plan: `documents/03-planning/waves/wave-05-document-generation.md` §4 Sub-PR 5.6b
