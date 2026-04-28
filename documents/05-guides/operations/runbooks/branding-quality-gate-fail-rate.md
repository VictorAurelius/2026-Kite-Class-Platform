# Runbook: Branding Quality Gate Fail Rate

**Alert:** `BrandingQualityGateFailRate`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

`InstanceQualityReviewer.review()` (per `ai-branding-guidelines.md` §5) is returning **score <70 on >20% of newly-generated branding instances over 15 minutes**. Each <70 result blocks deploy and either auto-regenerates or marks the instance FAILED, depending on tier policy. A spike in fail rate means **AI drift**: the model started producing assets that fail one of the 5 gate checks (WCAG contrast ≥ 4.5:1, CSS vars applied, no broken asset URLs, visual regression vs baseline ≤20%, logo placement). Customers may experience longer provisioning, used-up regenerate quotas, or fallback to template-only.

## Note

> Metric `kite_branding_quality_score` (histogram) and `kite_branding_quality_gate_failed_total` require `InstanceQualityReviewer` to publish to Micrometer. Real WCAG/visual-regression/ML-classifier scoring is currently scaffold-only per GAP-225 / GAP-226 / GAP-227 / GAP-228 — until those land, the gate is checklist-driven and metric is **metric-pending**. Reference baseline 62/100 captured 2026-04-26 (`documents/04-quality/audits/ai-branding/`).

## Immediate checks (0-5 min)

1. **What's failing — which check?**
   ```bash
   kubectl logs -n kitehub deploy/kitehub-branding --tail=300 \
     | grep -E 'QualityReview|score|wcag|contrast|regression|placement|FAILED' -A 5
   ```
2. **Is the AI provider also failing?** — correlate with [`ai-provider-high-failure-rate.md`](./ai-provider-high-failure-rate.md). Often the same root cause.
3. **Check failed-instance distribution by tier** to see if pattern is generalized or tier-specific:
   ```bash
   docker exec kite-postgres psql -U postgres -d kitehub -c \
     "SELECT tier, count(*) FROM branding_instance \
      WHERE status='FAILED' AND updated_at > now() - interval '30 minutes' \
      GROUP BY tier;"
   ```
4. **Check most recent prompt template version** — recent change?
   ```bash
   git log --oneline -- kitehub/kitehub-branding/src/main/resources/prompts/ | head -5
   ```

## Likely causes

- **Model drift after upgrade** → recent migration to a new model (e.g. Llama → Gemma, llama3.1 → llama3.2) without running migration test checklist (`ai-branding-guidelines.md` §11.4). Model returns assets that systematically fail one check (e.g. low-contrast text). **Fix:** roll back model config; per §11.4, AI behavior changes need 5-sample A/B vs baseline before rollout. File migration audit `documents/04-quality/audits/ai-branding/`.
- **Prompt template regression** → a recent prompt edit removed the `--ensure-contrast` directive; assets now fail WCAG. **Fix:** roll back prompt; require migration test on prompt rewrites.
- **Logo upload pipeline broken** → user logos arrive but Sharp/ImageMagick processing fails, falls through to placeholder, placement check fails. **Fix:** see logs for `Sharp` or `ImageMagick` errors; restart pipeline if memory leak suspected.
- **Brand token CSS missing** → recent kiteclass-frontend deploy dropped a CSS variable; "CSS vars applied" check returns false because computed style differs. **Fix:** verify FE design-tokens build output (`kiteclass-frontend/src/styles/tokens.css`).
- **Template fallback's own assets stale** → fallback templates were not updated to match latest design system; baseline visual regression diff exceeds 20%. **Fix:** refresh template baselines.
- **Threshold too tight after a code change** → someone bumped contrast threshold to 7:1 (AAA) without informing AI lead; pipeline can't sustain it. **Fix:** revert threshold OR negotiate with design.

## Mitigation

```bash
# 1. Force template-only fallback for new provisionings (skips AI generation entirely)
curl -X POST http://kitehub-branding:8083/actuator/env \
  -H 'Content-Type: application/json' \
  -d '{"name":"branding.quality_gate.bypass_to_template","value":"true"}'
curl -X POST http://kitehub-branding:8083/actuator/refresh
# This causes new instances to skip §5 gate and route via template path — preview before commit
# requirement (§4.2) still enforced; tenant approves manually.

# 2. Re-evaluate currently-FAILED instances (some may have been false-fails):
curl -X POST http://kitehub-branding:8083/api/v1/internal/quality/re-review-failed \
  -H "Authorization: Bearer $INTERNAL_API_SECRET" \
  -d '{"since":"2026-04-28T00:00:00Z"}'

# 3. If model migration suspected, roll back ai.provider.model to last good version
kubectl set env deployment/kitehub-branding -n kitehub \
  AI_PROVIDER_MODEL=llama3.1:8b  # previous version
kubectl rollout status deployment/kitehub-branding -n kitehub

# 4. Run /ai-branding-quality-gate skill manually on a sample to validate
# (creates report at documents/04-quality/audits/ai-branding/YYYY-MM-DD-<change>.md)
```

After mitigation, monitor fail rate for 30 min. Goal: ≤5% sustained. If still elevated, treat as model-fundamental issue and freeze new tenant provisioning to template-only until root-caused.

## When to escalate

- Fail rate >50% sustained → block AI generation entirely (set `branding.mode=template_only` globally), file gap, engage AI lead
- Migration test (§11.4) was skipped for a recent change → page AI lead and product owner; per `audit-to-gap-pipeline.md`, file gap retroactively
- ENTERPRISE customers complaining of low-quality AI output → bypass auto-fallback, surface to admin tooling for manual regenerate

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Rule: `.claude/rules/ai-branding-guidelines.md` §5 (Quality Gate), §11.4 (Migration test checklist)
- Skill: `.claude/skills/quality/ai-branding-quality-gate/SKILL.md`
- Memory: `feedback_ai_branding_governance_gap.md`, `feedback_search_all_modules_before_missing_claim.md`
- Related runbooks: [`ai-provider-high-failure-rate.md`](./ai-provider-high-failure-rate.md), [`tenant-provisioning-failure.md`](./tenant-provisioning-failure.md)
