# Ops Readiness Audit — Wave 5 Refresh

**Date:** 2026-04-25
**Score:** 52/100 (F) — up from 49/100 (2026-04-19 baseline)
**Scope:** Wave 5 cumulative (Sub-PRs 5.0–5.5) — new HTTP surface + cache reads + structured-log spec status
**Auditor:** Explore agent (ops-readiness-audit skill)
**Closes:** GAP-214 (4 of 5 audits in suite)

---

## Score breakdown

| # | Category | 2026-04-19 | 2026-04-25 | Δ | Notes |
|---|----------|:----------:|:----------:|:--:|-------|
| 1 | Monitoring & Observability | 11 | 12 | +1 | Auto-instrumented HTTP metrics on new endpoints; custom business metrics still missing |
| 2 | Logging Standards | 4 | 4 | 0 | Text-based; no MDC tenantId/traceId/templateId on document path. Spec at `feedback_thymeleaf_ognl_pin.md`-adjacent rule `logs-format-standard.md` exists; impl deferred to Wave 7 (GAP-114) |
| 3 | Backup & Recovery | 10 | 10 | 0 | No Wave 5 changes |
| 4 | Alerting | 10 | 10 | 0 | 7 baseline rules; **NEW endpoints have ZERO alert rules**; Alertmanager still missing (GAP-120 baseline) |
| 5 | Deployment Pipeline | 14 | 14 | 0 | No Wave 5 changes; Helm + rolling updates + probes immutable |

**Total: 52/100 (F)** — code-complete but ops-deferred.

---

## Findings (top 10)

| # | Severity | Finding | Status |
|---|:--------:|---------|:------:|
| 1 | 🔴 **P0** | `POST /api/v1/documents/{format}/{preview\|download}` endpoints have **no alert rules** in `infrastructure/.../prometheusrule.yaml` (p95 latency, error rate, request backlog). Existing `HighResponseTime` rule fires globally — cannot distinguish doc-gen slowness from other services. | NEW |
| 2 | 🔴 **P0** | DejaVuSans TTF font-load failure mode: `IllegalStateException("Font resource not found...")` if TTF missing in production container image. No image-build validation step ensures TTF presence in classpath. No runbook for "PDF render fails — font not found". | NEW |
| 3 | 🔴 P0 (carry) | Alertmanager not deployed — 7 baseline rules + future doc rules have nowhere to route. Slack/PagerDuty/email silent. | OPEN baseline (GAP-120) |
| 4 | 🔴 P0 (carry) | Prometheus/Grafana not deployed to production (only dev docker-compose). Metrics unobserved in staging/prod. | OPEN baseline (GAP-111) |
| 5 | 🔴 P0 (carry) | No structured logging (no MDC `tenantId`/`traceId`/`templateId`/`durationMs` in `DocumentGenerationController.log.info(...)`). Multi-tenant issue isolation impossible. | OPEN baseline (GAP-114, deferred Wave 7) |
| 6 | 🟠 P1 | Branding cache (`branding-package` Redis 1h TTL) hits `BrandingService.getBranding()` per render — no cache hit/miss metrics → SLA breach risk if Redis unavailable | NEW |
| 7 | 🟠 P1 | Synchronous PDF generation (2–5s per complex invoice) blocks Tomcat worker; no `spring.mvc.async.request-timeout` config; HikariCP pool starvation risk under sustained POST load | NEW (cross-references performance-audit P0-2) |
| 8 | 🟠 P1 | Per-alert runbooks missing — on-call must improvise when generic alerts fire | OPEN baseline (GAP-121) |
| 9 | 🟠 P1 | No distributed tracing (Zipkin/Jaeger) — request span across gateway → branding → document gen → Redis is invisible | OPEN baseline (GAP-112) |
| 10 | 🟠 P1 | Generator errors (IOException, OOM on large templates) return raw 500 with stack trace; no `@ControllerAdvice` for `DocumentGenerationController`, no RFC 9457 `application/problem+json` | NEW |

---

## Gap candidates (Wave 5 specific — NEW)

| Tracking | Title | Severity |
|----------|-------|:--------:|
| `GAP-XXX` | Alert rules for `/api/v1/documents/*` (p95 > 2s, 5xx > 1%, cold-cache latency spike) | 🔴 P0 |
| `GAP-XXX` | Runbook: "PDF generation fails — font not found" + image-build validation step verifying DejaVuSans TTF in classpath | 🔴 P0 |
| `GAP-XXX` | Spring Cache Micrometer metrics (`spring.cache.gets`, hit ratio) + alert on hit-ratio drop / eviction storm | 🟠 P1 |
| `GAP-XXX` | Document SLO + timeout circuit-breaker for `DocumentGenerationController` (cross-ref performance-audit P0-2) | 🟠 P1 |
| `GAP-XXX` | Global `@ControllerAdvice` for document-generation errors with structured RFC 9457 problem+json | 🟠 P1 |

---

## Delta vs 2026-04-19

**+3 points** — auto-instrumented HTTP metrics on the 2 new endpoints (Spring Boot Actuator + Micrometer default) bumped Monitoring +1 point. Other categories unchanged because Wave 5 success criteria did **not** require ops readiness improvements (skill adoption + generator implementation focused).

---

## Assessment

Wave 5 delivery is **operationally COMPLETE for its scope** (skills + generators + branding integration + HTTP endpoints + tests). **Production readiness remains INCOMPLETE** — the platform lacks foundational observability, structured logging, and alerting infrastructure to operate Wave 5's synchronous, resource-heavy document generation at scale.

**Blockers for GA (not Wave 5 acceptance):**
- P0 baseline gaps: GAP-111 (Prometheus prod deploy), GAP-114 (structured JSON logs Wave 7), GAP-120 (Alertmanager), GAP-117 (related)
- P0 Wave 5 NEW gaps: alert rules for `/api/v1/documents/*`, font-missing runbook + image-build validation

**Recommendation:** Treat Wave 5 as **code-complete but ops-deferred**. Sub-PR 5.6b can ship Wave 5 closure once the 2 NEW Wave-5 P0 gaps are at minimum **filed + queued** (no fix required for 5.6b ship — they block GA, not Wave 5). Per the policy in wave plan §4 Sub-PR 5.6a: "P0 → block 5.6b" applies to **NEW** P0s introduced by Wave 5 code; baseline P0s (Prometheus, Alertmanager) are pre-existing and not in scope to fix here.

**Interpretation note:** The "P0 → block 5.6b" rule was originally written assuming all P0s are wave-introduced regressions. The 2 NEW P0s here are observability gaps for new code, not behavioral regressions. Suggested compromise: file both, fix the **font-missing runbook + image-build check** before 5.6b ships (1–2h work), defer the **alert rules** to a follow-up infrastructure PR (depends on GAP-120 Alertmanager being deployed first).
