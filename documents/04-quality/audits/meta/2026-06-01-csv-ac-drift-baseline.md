# Gap CSV ↔ AC Drift Baseline (Wave meta-8 Bucket D, 2026-06-01)

**Source:** `scripts/sync-gap-csv-from-ac.sh --warn` real-data run on 641 CSV rows.

## Interpretation guide

Per `gap-architecture-v2.md` v2.0.0 §3, **CSV `completion_pct` is canonical**;
gap-file AC checkbox bitmaps are universally stale (this is by design — coordinator
updates CSV at wave closure, not per-AC). This report surfaces bi-directional drift:

| Drift direction | Likely meaning | Action |
|---|---|---|
| `AC pct >> CSV pct` (Δ positive) | Work shipped, CSV pct not synced — true under-reporting | Update CSV pct via Wave meta-N catalog apply |
| `AC pct << CSV pct` (Δ negative) | Coordinator marked work DONE/PARTIAL high but AC checkboxes never ticked — stale checkbox (expected) | Ignore (AC checkbox bitmap deferred per CSV canonical rule); optionally tick AC at gap closure |

Phase 2 enhancement (Wave meta-9 candidate): split detector output into
"under-reported" vs "stale-checkbox" categories; HARD STOP only on
under-reporting direction.

---

# Gap CSV ↔ AC drift report (2026-06-01T10:05:08Z)

**Threshold:** 10pp

| Gap | File | AC | CSV pct | AC-derived pct | Δ |
|---|---|---|---|---|---|
| GAP-783 | phase-1-beta/closed/GAP-783-owner-jwt-spring-authority-mapping-403.md | 0/6 | 100% | 0% | -100pp |
| GAP-192 | phase-1.5-paid/GAP-192-trial-to-paid-zero-downtime-migration.md | 0/9 | 50% | 0% | -50pp |
| GAP-782 | phase-1-beta/GAP-782-wave-meta-6-post-merge-followups.md | 0/6 | 50% | 0% | -50pp |
| GAP-353 | phase-1-beta/GAP-353-pdpl-cookie-consent-banner-marketing-kits.md | 4/9 | 73% | 44% | -29pp |
| GAP-787 | phase-1-beta/closed/GAP-787-staff-invite-email-send-never-implemented.md | 0/10 | 100% | 0% | -100pp |
| GAP-786 | phase-1-beta/closed/GAP-786-staff-invite-accept-user-provision-missing.md | 0/7 | 100% | 0% | -100pp |
| GAP-197 | phase-1-beta/GAP-197-attendance-calendar-mode-ui.md | 0/8 | 50% | 0% | -50pp |
| GAP-785 | phase-1-beta/closed/GAP-785-rabbitmq-queue-auto-declare-missing.md | 0/5 | 100% | 0% | -100pp |
| GAP-357 | unclassified/GAP-357-deprecated-exception-ctors-warning-sweep.md | 2/7 | 50% | 28% | -22pp |
| GAP-195 | unclassified/GAP-195-starter-kit-bulk-retro-sync.md | 7/10 | 50% | 70% | 20pp |
| GAP-359 | phase-3/GAP-359-child-protection-phase-1c-remainder.md | 5/10 | 0% | 50% | 50pp |
| GAP-789 | phase-1-beta/GAP-789-wave-a-bucket-b-post-merge-audit-suite.md | 0/5 | 20% | 0% | -20pp |
| GAP-198 | unclassified/GAP-198-fe-be-mock-contract-tests.md | 4/12 | 50% | 33% | -17pp |
| GAP-460 | phase-1-beta/GAP-460-brand-pivot-kiteclass-me-customer-facing.md | 10/23 | 10% | 43% | 33pp |
| GAP-562b | phase-1-beta/closed/GAP-562b-fe-role-guard-and-preauthorize-extension.md | 0/6 | 100% | 0% | -100pp |
| GAP-465 | phase-1-beta/closed/GAP-465-helm-k8s-artifacts-validation-pre-phase-1-5-migration.md | 0/7 | 100% | 0% | -100pp |
| GAP-269c | phase-2/GAP-269c-student-e2e-lighthouse-verification.md | 1/3 | 50% | 33% | -17pp |
| GAP-369 | phase-1-beta/closed/GAP-369-production-dns-domain-setup.md | 6/8 | 100% | 75% | -25pp |
| GAP-222 | phase-1-beta/GAP-222-outbox-bypass-policy.md | 0/5 | 50% | 0% | -50pp |
| GAP-223 | phase-1-beta/GAP-223-ai-branding-migration-verification-governance.md | 0/8 | 50% | 0% | -50pp |
| GAP-675 | phase-1-beta/GAP-675-meta-meta-premature-rule-guard-audit.md | 0/5 | 70% | 0% | -70pp |
| GAP-674 | phase-1-beta/closed/GAP-674-wave-99b-b5-onboarding-tour-readme.md | 0/8 | 100% | 0% | -100pp |
| GAP-361 | phase-3/GAP-361-parent-portal-phase-1c-remainder.md | 3/8 | 0% | 37% | 37pp |
| GAP-360 | phase-3/GAP-360-multi-subject-gradebook-phase-1c-remainder.md | 6/11 | 0% | 54% | 54pp |
| GAP-363 | phase-2/GAP-363-kiteclass-student-polish-payments-persona-violation.md | 7/8 | 50% | 87% | 37pp |
| GAP-364 | phase-2/GAP-364-kitehub-admin-polish-school-profile-rebuild.md | 2/9 | 50% | 22% | -28pp |
| GAP-005 | phase-1-beta/GAP-005-ai-queue-fair-scheduling.md | 0/8 | 40% | 0% | -40pp |
| GAP-001 | unclassified/closed/GAP-001-kiteclass-gateway-decision.md | 0/4 | 100% | 0% | -100pp |
| GAP-804 | phase-1-beta/closed/GAP-804-branding-logo-upload-contract-drift.md | 0/3 | 100% | 0% | -100pp |
| GAP-805 | phase-1-beta/closed/GAP-805-sky-education-demo-tenant-polish.md | 0/7 | 100% | 0% | -100pp |
| GAP-807 | phase-1-beta/closed/GAP-807-dashboard-persisted-theme-not-applied.md | 3/4 | 100% | 75% | -25pp |
| GAP-602 | phase-1-beta/closed/GAP-602-pm2-ecosystem-cwd-path-mismatch.md | 0/5 | 100% | 0% | -100pp |
| GAP-603 | phase-1-beta/closed/GAP-603-pm2-systemd-auto-start.md | 0/6 | 100% | 0% | -100pp |
| GAP-600 | phase-1-beta/closed/GAP-600-beta-request-abort-cleanup.md | 0/7 | 100% | 0% | -100pp |
| GAP-708 | phase-1-beta/GAP-708-wave-103-post-merge-audit-suite-deadline.md | 0/6 | 20% | 0% | -20pp |
| GAP-606 | phase-1-beta/closed/GAP-606-email-template-admin-new-login-alert-missing.md | 0/4 | 100% | 0% | -100pp |
| GAP-353b-server-consent-api-audit-log | phase-1-beta/GAP-353b-server-consent-api-audit-log.md | 8/11 | 85% | 72% | -13pp |
| GAP-607 | phase-1-beta/closed/GAP-607-rabbitmq-dlq-not-configured.md | 0/5 | 100% | 0% | -100pp |
| GAP-604 | phase-1-beta/closed/GAP-604-gateway-jwt-to-headers-propagation.md | 0/7 | 100% | 0% | -100pp |
| GAP-605 | phase-1-beta/closed/GAP-605-outbox-dispatcher-not-implemented.md | 0/5 | 100% | 0% | -100pp |
| GAP-703 | phase-1-beta/closed/GAP-703-gap-657-ac-fail-retroactive-list-unsubscribe-multipart.md | 0/6 | 100% | 0% | -100pp |
| GAP-702 | phase-1-beta/closed/GAP-702-approval-email-not-firing-on-beta-approve.md | 0/5 | 100% | 0% | -100pp |
| GAP-608 | phase-1-beta/GAP-608-ec2-iam-ses-sendemail-missing.md | 0/4 | 90% | 0% | -90pp |
| GAP-609 | phase-1-beta/closed/GAP-609-fe-claim-code-redemption-page-missing.md | 0/5 | 100% | 0% | -100pp |
| GAP-707 | phase-1-beta/closed/GAP-707-login-audit-service-duplicate-row-warn.md | 0/4 | 100% | 0% | -100pp |
| GAP-706 | phase-1-beta/closed/GAP-706-subscription-security-challenge-token-bridge-missing.md | 0/6 | 100% | 0% | -100pp |
| GAP-705 | phase-1-beta/closed/GAP-705-gateway-jwt-filter-rejects-2fa-challenge-tokens.md | 0/6 | 100% | 0% | -100pp |
| GAP-704 | phase-1-beta/closed/GAP-704-jwt-missing-tenant-id-claim-post-beta-signup.md | 0/6 | 100% | 0% | -100pp |
| GAP-508 | phase-1-beta/closed/GAP-508-production-env-config-registry-meta-gap.md | 4/7 | 100% | 57% | -43pp |
| GAP-500 | phase-1-beta/closed/GAP-500-seed-runner-activation-debug.md | 0/4 | 100% | 0% | -100pp |
| GAP-503 | phase-1-beta/closed/GAP-503-jvm-tomcat-hikari-tier-2-optimization.md | 0/7 | 100% | 0% | -100pp |
| GAP-502 | phase-1-beta/GAP-502-rabbitmq-auth-fail-plus-oom-thrash-kh-backend.md | 0/7 | 90% | 0% | -90pp |
| GAP-273 | phase-2/GAP-273-track-2-port-12-components-shared-lib.md | 0/13 | 50% | 0% | -50pp |
| GAP-272 | phase-2/GAP-272-track-2-port-ai-branding-wizard-v2.md | 11/11 | 50% | 100% | 50pp |
| GAP-271 | phase-3/GAP-271-track-2-port-kitehub-admin.md | 0/10 | 50% | 0% | -50pp |
| GAP-506 | phase-1-beta/GAP-506-deploy-prod-script-tech-debt-cluster.md | 0/5 | 60% | 0% | -60pp |
| GAP-270 | phase-2/GAP-270-track-2-port-kitehub-pro-v2.md | 8/13 | 50% | 61% | 11pp |
| GAP-537c | phase-1-beta/GAP-537c-user-manual-p2-p3-screenshots-tier2-annotation.md | 0/5 | 50% | 0% | -50pp |
| GAP-583 | phase-1-beta/GAP-583-rds-storage-alarm-wiring-resize-runbook.md | 0/4 | 50% | 0% | -50pp |
| GAP-499 | phase-1-beta/closed/GAP-499-wave-67-seed-prerequisites.md | 4/7 | 100% | 57% | -43pp |
| GAP-689 | phase-1-beta/closed/GAP-689-wave-102.6-thesis-v1-deferred-items.md | 6/15 | 100% | 40% | -60pp |
| GAP-498 | phase-1-beta/closed/GAP-498-deploy-workflow-poll-redesign-alb-health.md | 0/5 | 100% | 0% | -100pp |
| GAP-585 | phase-1-beta/closed/GAP-585-cookie-consent-pdpl-decree-13-granular.md | 0/8 | 100% | 0% | -100pp |
| GAP-680 | phase-1-beta/closed/GAP-680-vn-localization-audit-checklist.md | 4/5 | 100% | 80% | -20pp |
| GAP-497 | unclassified/GAP-497-aws-credits-strategy-post-activate-rejection.md | 3/6 | 0% | 50% | 50pp |
| GAP-684 | phase-1-beta/closed/GAP-684-admin-role-live-walk-aws-restore-blocked.md | 0/7 | 100% | 0% | -100pp |
| GAP-135 | phase-1-beta/GAP-135-api-p95-latency-slos-undocumented.md | 4/6 | 50% | 66% | 16pp |
| GAP-139 | phase-1-beta/GAP-139-parent-dashboard-mvp-placeholder.md | 0/6 | 40% | 0% | -40pp |
| GAP-219 | unclassified/GAP-219-wave5-audit-followups-p1-p2.md | 0/3 | 55% | 0% | -55pp |
| GAP-218 | phase-1-beta/closed/GAP-218-pdf-font-missing-runbook.md | 0/5 | 100% | 0% | -100pp |
| GAP-525 | phase-1-beta/closed/GAP-525-rotate-credentials-leaked-session-2026-05-13.md | 5/12 | 100% | 41% | -59pp |
| GAP-213 | phase-1-beta/GAP-213-spring-cloud-bom-resolution.md | 0/4 | 60% | 0% | -60pp |
| GAP-528 | phase-1-beta/closed/GAP-528-wave-73-bucket-b-hooks-deterministic-enforcement.md | 0/8 | 100% | 0% | -100pp |
| GAP-212 | phase-1-beta/closed/GAP-212-url-allowlist-test-flaky-dns.md | 0/5 | 100% | 0% | -100pp |
| GAP-620 | phase-1-beta/closed/GAP-620-wave-92-bucket-d-live-verify-admin-v1-controllers.md | 0/6 | 100% | 0% | -100pp |
| GAP-215 | phase-1-beta/closed/GAP-215-branding-service-cacheable.md | 0/6 | 100% | 0% | -100pp |
| GAP-217 | phase-1-beta/closed/GAP-217-document-endpoints-alert-rules.md | 0/5 | 100% | 0% | -100pp |
| GAP-216 | phase-1-beta/closed/GAP-216-pdf-p95-micro-benchmark.md | 0/4 | 100% | 0% | -100pp |
| GAP-755 | phase-1-beta/GAP-755-pdpl-consent-be-persistence-integration.md | 0/6 | 30% | 0% | -30pp |
| GAP-756 | phase-1-beta/GAP-756-wave-beta-prep-1-production-deploy-rst-verify.md | 3/14 | 35% | 21% | -14pp |
| GAP-757 | phase-1-beta/GAP-757-wave-beta-prep-1-post-wave-audit-suite.md | 0/6 | 30% | 0% | -30pp |
| GAP-751 | phase-1-beta/closed/GAP-751-stale-gap-csv-prevention-hook.md | 0/7 | 100% | 0% | -100pp |
| GAP-752 | phase-1-beta/closed/GAP-752-rabbitmq-class-rescheduled-queue.md | 0/4 | 100% | 0% | -100pp |
| GAP-753 | phase-1-beta/closed/GAP-753-beta-signup-uuid-handler.md | 0/4 | 100% | 0% | -100pp |
| GAP-759 | phase-1-beta/closed/GAP-759-class-lifecycle-e2e-pre-existing-flake.md | 4/5 | 100% | 80% | -20pp |
| GAP-552 | phase-1-beta/closed/GAP-552-securityconfig-default-allow-fallback-defense-in-depth.md | 0/5 | 100% | 0% | -100pp |
| GAP-553 | phase-1-beta/closed/GAP-553-totp-cipher-and-jwt-challenge-secret-dev-default-fail-fast.md | 0/5 | 100% | 0% | -100pp |
| GAP-551 | phase-1-beta/closed/GAP-551-feedback-endpoint-missing-gateway-rate-limit-and-tenant-context.md | 0/6 | 100% | 0% | -100pp |
| GAP-449 | unclassified/GAP-449-terraform-apply-workflow-dispatch-rule-revise.md | 0/9 | 60% | 0% | -60pp |
| GAP-554 | phase-1-beta/closed/GAP-554-onboarding-tenant-header-jwt-cross-check.md | 0/5 | 100% | 0% | -100pp |
| GAP-555 | phase-1-beta/closed/GAP-555-wave-78-config-keys-documented-but-not-wired.md | 0/5 | 100% | 0% | -100pp |
| GAP-444 | phase-1-beta/GAP-444-phase-4-staging-defer-to-phase-7-prod-deploy.md | 0/7 | 30% | 0% | -30pp |
| GAP-559 | phase-1-beta/closed/GAP-559-onboarding-dashboard-cta-sidebar-nav.md | 0/7 | 100% | 0% | -100pp |
| GAP-447 | phase-1-beta/GAP-447-right-size-ec2-post-vercel-pivot.md | 0/7 | 75% | 0% | -75pp |
| GAP-440 | phase-1-beta/GAP-440-spring-boot-dep-bump-before-prod.md | 0/4 | 30% | 0% | -30pp |
| GAP-181 | phase-1.5-paid/GAP-181-acceptable-use-policy.md | 0/11 | 50% | 0% | -50pp |
| GAP-180 | phase-1.5-paid/GAP-180-terms-of-service.md | 0/12 | 50% | 0% | -50pp |
| GAP-183 | phase-1.5-paid/GAP-183-refund-dispute-resolution-policy.md | 0/11 | 50% | 0% | -50pp |
| GAP-099 | phase-3/GAP-099-structured-class-schedule.md | 0/5 | 50% | 0% | -50pp |
| GAP-348 | phase-2/GAP-348-round-3-ui-kits-persona-driven-review.md | 0/7 | 50% | 0% | -50pp |
| GAP-272c | phase-2/GAP-272c-quality-gate-score-aggregator-endpoint.md | 4/5 | 50% | 80% | 30pp |
| GAP-272k | phase-2/GAP-272k-live-brand-colors-from-generate-endpoint.md | 3/4 | 50% | 75% | 25pp |
| GAP-267a | phase-2/GAP-267a-parent-e2e-lighthouse-verification.md | 1/3 | 50% | 33% | -17pp |
| GAP-812 | phase-1-beta/GAP-812-custom-domain-dns-ssl-completion.md | 0/9 | 40% | 0% | -40pp |
| GAP-811 | phase-1-beta/GAP-811-fe-middleware-host-tenant-resolution.md | 0/8 | 60% | 0% | -60pp |
| GAP-810 | phase-1-beta/GAP-810-demo-landing-image-assets.md | 0/4 | 70% | 0% | -70pp |
| GAP-611 | phase-1-beta/closed/GAP-611-post-beta-signup-route-404.md | 0/6 | 100% | 0% | -100pp |
| GAP-610 | phase-1-beta/GAP-610-validate-token-returns-not-found-for-valid-token.md | 0/6 | 75% | 0% | -75pp |
| GAP-738 | phase-1-beta/closed/GAP-738-3-layer-business-docs-3-new-domains.md | 5/6 | 100% | 83% | -17pp |
| GAP-612 | phase-1-beta/closed/GAP-612-aws-account-suspension-recovery.md | 0/7 | 100% | 0% | -100pp |
| GAP-732 | phase-1-beta/GAP-732-bucket-b-test-re-enable-cross-user-authz.md | 0/5 | 60% | 0% | -60pp |
| GAP-428 | phase-1-beta/GAP-428-prospect-public-pages-missing-kit.md | 0/5 | 70% | 0% | -70pp |
| GAP-579 | phase-1-beta/GAP-579-soft-delete-restore-window.md | 0/8 | 40% | 0% | -40pp |
| GAP-576 | phase-1-beta/closed/GAP-576-gateway-auth-routes-404-login-verify-email-password-reset.md | 0/7 | 100% | 0% | -100pp |
| GAP-570 | phase-1-beta/closed/GAP-570-spring-500-instead-of-404-post-static-not-found-incomplete.md | 0/5 | 100% | 0% | -100pp |
| GAP-571 | phase-1-beta/closed/GAP-571-validation-endpoints-500-instead-400-pre-existing.md | 0/5 | 100% | 0% | -100pp |
| GAP-572 | phase-1-beta/GAP-572-resend-secret-schema-mismatch-plus-leak-rotate.md | 0/8 | 40% | 0% | -40pp |
| GAP-291 | phase-1-beta/closed/GAP-291-reschedule-lesson-session.md | 0/7 | 100% | 0% | -100pp |
| GAP-476 | phase-1-beta/closed/GAP-476-flyway-migration-http-endpoint.md | 0/5 | 100% | 0% | -100pp |
| GAP-292 | phase-1-beta/closed/GAP-292-per-session-pricing-model.md | 0/7 | 100% | 0% | -100pp |
| GAP-472 | phase-1.5-paid/GAP-472-gateway-security-headers-filter-parity.md | 3/5 | 75% | 60% | -15pp |
| GAP-764 | phase-1-beta/closed/GAP-764-beta-request-json-response-html-entity-escape.md | 5/6 | 100% | 83% | -17pp |
| GAP-790 | phase-1-beta/closed/GAP-790-gateway-staff-invitations-route-missing-tenant-resolver.md | 0/4 | 100% | 0% | -100pp |
| GAP-791 | phase-1-beta/closed/GAP-791-course-list-native-query-bypasses-tenant-filter.md | 0/5 | 100% | 0% | -100pp |
| GAP-792 | phase-1-beta/closed/GAP-792-courses-cache-key-not-tenant-scoped.md | 0/5 | 100% | 0% | -100pp |
| GAP-793 | phase-1-beta/GAP-793-production-email-provider-routing-resend-never-reached.md | 7/9 | 95% | 77% | -18pp |
| GAP-795 | phase-1-beta/closed/GAP-795-gateway-x-user-id-uuid-vs-long-usercontext-null.md | 0/5 | 100% | 0% | -100pp |
| GAP-796 | phase-1-beta/closed/GAP-796-kiteclass-core-404-405-masked-as-500.md | 0/4 | 100% | 0% | -100pp |
| GAP-797 | phase-1-beta/closed/GAP-797-email-template-variable-name-contract-drift.md | 0/6 | 100% | 0% | -100pp |
| GAP-798 | phase-1-beta/GAP-798-domain-entity-user-id-uuid-bridge-authz-v2.md | 0/7 | 50% | 0% | -50pp |
| GAP-200-school-mis-integration | phase-3/GAP-200-school-mis-integration.md | 9/14 | 50% | 64% | 14pp |
| GAP-040 | unclassified/closed/GAP-040-support-impersonation-tools.md | 0/8 | 100% | 0% | -100pp |
| GAP-646 | phase-1-beta/closed/GAP-646-thesis-docx-pipeline.md | 11/14 | 100% | 78% | -22pp |
| GAP-233 | phase-1-beta/closed/GAP-233-api-contract-student-enrollment-zero-doc.md | 0/8 | 100% | 0% | -100pp |
| GAP-232 | phase-1-beta/closed/GAP-232-api-contract-attendance-zero-doc.md | 0/8 | 100% | 0% | -100pp |
| GAP-644 | phase-1-beta/closed/GAP-644-scheduler-cloudwatch-drift-metric.md | 0/5 | 100% | 0% | -100pp |
| GAP-231 | phase-1-beta/closed/GAP-231-api-contract-payment-invoice-zero-doc.md | 0/8 | 100% | 0% | -100pp |
| GAP-645 | unclassified/GAP-645-wave-96-gap-folder-reorg-phase-subdirs.md | 0/9 | 30% | 0% | -30pp |
| GAP-642 | phase-1-beta/closed/GAP-642-v54-jsonb-testcontainers-it-missing.md | 0/6 | 100% | 0% | -100pp |
| GAP-640 | phase-1-beta/closed/GAP-640-admin-audit-domain-3-layer-docs-missing.md | 0/6 | 100% | 0% | -100pp |
| GAP-699 | phase-1-beta/closed/GAP-699-gateway-jwt-secret-compose-env-passthrough.md | 4/5 | 100% | 80% | -20pp |
| GAP-695 | phase-1-beta/GAP-695-self-test-readiness-comprehensive-plan.md | 5/10 | 85% | 50% | -35pp |
| GAP-693 | phase-1-beta/GAP-693-aws-rebuild-sop-playbook.md | 0/10 | 70% | 0% | -70pp |
| GAP-321b | phase-3/GAP-321b-parent-portal-phase-1b-facets-zalo-audit.md | 0/9 | 50% | 0% | -50pp |
| GAP-321c | phase-3/GAP-321c-parent-portal-phase-1c-pdpl-consent-write-actions.md | 5/8 | 0% | 62% | 62pp |
| GAP-517 | phase-1-beta/GAP-517-admin-login-new-ip-alert.md | 2/3 | 85% | 66% | -19pp |
| GAP-514 | phase-1-beta/closed/GAP-514-auth-endpoints-gateway-rate-limit.md | 5/7 | 100% | 71% | -29pp |
| GAP-512 | phase-1-beta/closed/GAP-512-gateway-routing-scope-extension-wave-71b.md | 0/5 | 100% | 0% | -100pp |
| GAP-268 | phase-2/GAP-268-track-2-port-kiteclass-teacher.md | 6/9 | 50% | 66% | 16pp |
| GAP-513 | phase-1-beta/closed/GAP-513-resend-manual-provisioning-user-action.md | 0/4 | 100% | 0% | -100pp |
| GAP-269 | phase-2/GAP-269-track-2-port-kiteclass-student.md | 0/10 | 50% | 0% | -50pp |
| GAP-639 | phase-1-beta/closed/GAP-639-beta-access-aborted-enum-orphan-rules-md.md | 0/5 | 100% | 0% | -100pp |
| GAP-638 | phase-1-beta/GAP-638-admin-v1-api-contract-docs-typed-dtos.md | 0/6 | 20% | 0% | -20pp |
| GAP-637 | phase-1-beta/closed/GAP-637-admin-v1-controllers-preauthorize-missing.md | 3/5 | 100% | 60% | -40pp |
| GAP-266 | phase-2/GAP-266-track-2-port-kiteclass-pro-v2.md | 0/10 | 50% | 0% | -50pp |
| GAP-267 | phase-2/GAP-267-track-2-port-kiteclass-parent.md | 8/11 | 50% | 72% | 22pp |
| GAP-262 | unclassified/GAP-262-starter-kit-upstream-retro-sync-pr.md | 0/12 | 50% | 0% | -50pp |
| GAP-747 | phase-1-beta/GAP-747-ses-iam-live-verify-post-restore.md | 0/7 | 17% | 0% | -17pp |
| GAP-746 | phase-1-beta/GAP-746-multi-tenant-isolation-tests-residual.md | 0/6 | 40% | 0% | -40pp |
| GAP-744 | phase-1-beta/closed/GAP-744-wave-br-4-pre-existing-test-fails-and-br-5-plan-completeness.md | 0/7 | 100% | 0% | -100pp |
| GAP-743 | phase-1-beta/closed/GAP-743-entity-migration-mapper-ci-gate.md | 0/6 | 100% | 0% | -100pp |
| GAP-742 | phase-1-beta/closed/GAP-742-outbox-dlq-alert-missing.md | 0/5 | 100% | 0% | -100pp |
| GAP-545 | phase-1-beta/closed/GAP-545-dialog-focus-trap-escape-key.md | 0/8 | 100% | 0% | -100pp |
| GAP-547 | phase-1-beta/closed/GAP-547-twofactor-endpoints-undocumented-and-unversioned.md | 0/7 | 100% | 0% | -100pp |
| GAP-540 | phase-1-beta/closed/GAP-540-beta-support-channel-discoverability.md | 0/9 | 100% | 0% | -100pp |
| GAP-543 | phase-1-beta/GAP-543-email-content-audit-5-types.md | 4/10 | 95% | 40% | -55pp |
| GAP-542 | phase-1-beta/closed/GAP-542-feedback-channel-widget-survey.md | 0/10 | 100% | 0% | -100pp |
| GAP-548 | phase-1-beta/closed/GAP-548-password-reset-backend-controller-missing.md | 0/8 | 100% | 0% | -100pp |
| GAP-451 | unclassified/GAP-451-spring-boot-3-5-x-no-newer-patch-await-upstream.md | 0/5 | 50% | 0% | -50pp |
| GAP-662 | phase-1-beta/closed/GAP-662-wave-98-email-controller-url-drift.md | 0/5 | 100% | 0% | -100pp |
| GAP-663 | phase-1-beta/closed/GAP-663-wave-98-preferences-controller-zero-it.md | 0/5 | 100% | 0% | -100pp |
| GAP-664 | phase-1-beta/GAP-664-wave-98-3-layer-doc-completeness-drift.md | 0/6 | 40% | 0% | -40pp |
| GAP-370 | phase-1-beta/closed/GAP-370-email-transactional-infrastructure.md | 7/9 | 100% | 77% | -23pp |
| GAP-127 | phase-1-beta/GAP-127-frontend-code-splitting-bundle-analyzer.md | 0/6 | 60% | 0% | -60pp |
| GAP-402 | phase-1-beta/GAP-402-sbom-image-signing-cosign.md | 4/5 | 50% | 80% | 30pp |
| GAP-321 | phase-3/GAP-321-parent-portal-v1-legal-mandate.md | 8/16 | 0% | 50% | 50pp |
| GAP-405 | phase-1-beta/GAP-405-visual-regression-baseline-playwright.md | 4/5 | 50% | 80% | 30pp |
| GAP-322 | phase-3/GAP-322-child-protection-workflow.md | 7/22 | 0% | 31% | 31pp |
| GAP-408 | phase-1-beta/GAP-408-jvm-heap-cap-dev-profile.md | 3/4 | 50% | 75% | 25pp |
| GAP-711 | phase-1-beta/closed/GAP-711-gateway-tenant-resolver-jwt-fallback.md | 0/5 | 100% | 0% | -100pp |
| GAP-712 | phase-1-beta/closed/GAP-712-onboarding-controller-tenant-jwt-fallback.md | 0/6 | 100% | 0% | -100pp |
| GAP-713 | phase-1-beta/closed/GAP-713-email-service-url-config-key-drift.md | 0/8 | 100% | 0% | -100pp |
| GAP-714 | phase-1-beta/closed/GAP-714-gateway-route-onboarding-progress-wrong-service.md | 0/5 | 100% | 0% | -100pp |
| GAP-715 | phase-1-beta/closed/GAP-715-admin-audit-log-binding-error.md | 0/6 | 100% | 0% | -100pp |
| GAP-716 | phase-1-beta/GAP-716-wave-104.5-post-merge-audit-suite-deadline.md | 0/6 | 50% | 0% | -50pp |
| GAP-717 | phase-1-beta/closed/GAP-717-jwt-challenge-secret-production-parity.md | 7/8 | 100% | 87% | -13pp |
| GAP-116-pii-scrubbing-logs | unclassified/closed/GAP-116-pii-scrubbing-logs.md | 5/6 | 100% | 83% | -17pp |
| GAP-438 | unclassified/GAP-438-agent-aws-access-workflow.md | 0/9 | 85% | 0% | -85pp |
| GAP-431 | phase-1-beta/closed/GAP-431-helm-startupprobe-missing.md | 3/5 | 100% | 60% | -40pp |
| GAP-432 | phase-1-beta/closed/GAP-432-unbounded-findall-services.md | 3/5 | 100% | 60% | -40pp |
| GAP-259 | phase-1.5-paid/GAP-259-gateway-rate-limit-tenant-key.md | 6/7 | 50% | 85% | 35pp |
| GAP-437 | phase-1-beta/closed/GAP-437-aws-observability-baseline.md | 0/8 | 100% | 0% | -100pp |
| GAP-436 | phase-1-beta/GAP-436-oidc-deploy-ecr-restore-roles.md | 0/7 | 30% | 0% | -30pp |
| GAP-569 | phase-1-beta/closed/GAP-569-otel-cve-2026-45292-baggage-unbounded-memory.md | 0/4 | 100% | 0% | -100pp |
| GAP-568 | phase-1-beta/closed/GAP-568-wave-82-be-cors-allowlist-sweep-pre-dns-flip.md | 0/8 | 100% | 0% | -100pp |
| GAP-567 | phase-1-beta/GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md | 0/11 | 55% | 0% | -55pp |
| GAP-566 | phase-1-beta/GAP-566-wave-82-t3-small-ram-tuning-pm2-swapfile-memory-alarm.md | 0/8 | 60% | 0% | -60pp |
| GAP-565 | phase-1-beta/closed/GAP-565-wave-82-ec2-security-group-description-port-restriction.md | 0/7 | 100% | 0% | -100pp |
| GAP-564 | phase-1-beta/closed/GAP-564-security-audit-cat2-grep-evidence-enforcement.md | 0/5 | 100% | 0% | -100pp |
| GAP-563 | phase-1-beta/closed/GAP-563-user-manual-content-review-standard-meta.md | 0/20 | 100% | 0% | -100pp |
| GAP-562 | phase-1-beta/closed/GAP-562-rbac-role-separation-customer-vs-staff.md | 0/9 | 100% | 0% | -100pp |
| GAP-560 | phase-1-beta/closed/GAP-560-beta-disclaimer-banner-specificity-data-reset-policy.md | 0/7 | 100% | 0% | -100pp |
| GAP-481 | phase-1-beta/closed/GAP-481-gateway-path-routing-404.md | 0/6 | 100% | 0% | -100pp |
| GAP-482 | phase-1-beta/closed/GAP-482-deploy-workflow-iam-tag-and-hardcoded-instance.md | 0/7 | 100% | 0% | -100pp |
| GAP-483 | phase-1-beta/closed/GAP-483-ec2-user-data-bootstrap-missing-git-repo.md | 2/4 | 100% | 50% | -50pp |
| GAP-484 | phase-1-beta/closed/GAP-484-java-services-otel-autoconfig-crash.md | 2/6 | 100% | 33% | -67pp |
| GAP-485 | unclassified/GAP-485-csv-canonical-for-meta-enumerations.md | 0/7 | 55% | 0% | -55pp |
| GAP-599 | phase-1-beta/GAP-599-jwt-tab-collide-storage-isolation.md | 0/6 | 92% | 0% | -92pp |
| GAP-052 | phase-3/GAP-052-parent-portal.md | 0/10 | 40% | 0% | -40pp |
| GAP-659 | phase-1-beta/closed/GAP-659-wave-98-staff-invite-email-persona-tone-split.md | 9/12 | 100% | 75% | -25pp |
| GAP-658 | phase-1-beta/GAP-658-wave-98-vn-sample-seed-worker.md | 0/7 | 80% | 0% | -80pp |
| GAP-204 | phase-1-beta/GAP-204-npm-security-backlog.md | 0/13 | 75% | 0% | -75pp |
| GAP-533 | phase-1-beta/GAP-533-resend-deliverability-warmup-dkim-dmarc-spf.md | 5/10 | 80% | 50% | -30pp |
| GAP-534 | phase-1-beta/closed/GAP-534-invite-token-single-use-enforcement.md | 7/8 | 100% | 87% | -13pp |
| GAP-535 | phase-1-beta/closed/GAP-535-tenant-slug-normalize-vn-diacritics.md | 6/8 | 100% | 75% | -25pp |
| GAP-537 | phase-1-beta/GAP-537-user-manual-vietnamese-screenshots-based.md | 0/5 | 25% | 0% | -25pp |
| GAP-538 | phase-1-beta/closed/GAP-538-onboarding-checklist-sample-data-seed.md | 6/8 | 100% | 75% | -25pp |
| GAP-653 | phase-1-beta/closed/GAP-653-thesis-defense-prep-deck.md | 4/6 | 100% | 66% | -34pp |
| GAP-657 | phase-1-beta/closed/GAP-657-wave-98-email-layer-hardening.md | 5/8 | 100% | 62% | -38pp |
| GAP-656 | phase-1-beta/GAP-656-wave-98-ui-coordinator-widget-collision.md | 0/7 | 80% | 0% | -80pp |
| GAP-721 | phase-1-beta/GAP-721-zalo-oa-owner-notify-stub.md | 0/6 | 60% | 0% | -60pp |
| GAP-724 | phase-1-beta/closed/GAP-724-kc-frontend-auth-path-mismatch.md | 0/5 | 100% | 0% | -100pp |
| GAP-727 | phase-1-beta/GAP-727-authz-a01-class-teacher-guard-broken.md | 0/5 | 80% | 0% | -80pp |
| GAP-322c | phase-3/GAP-322c-child-protection-phase-1c-mandatory-reporting-hash-audit.md | 7/13 | 0% | 53% | 53pp |
| GAP-322b | phase-3/GAP-322b-child-protection-phase-1b-vetting-minio-rbac.md | 7/10 | 0% | 70% | 70pp |
| GAP-728 | phase-1-beta/closed/GAP-728-test-security-config-enablemethodsecurity-missing.md | 0/4 | 100% | 0% | -100pp |

**Drift count:** 226 of 641 gaps audited (threshold 10pp)
