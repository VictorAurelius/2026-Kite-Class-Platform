# Wave Roadmap — P0 Features + Related P1 Clusters

**Created:** 2026-04-17
**Scope:** 5 remaining P0 gaps + 3 P1 clusters (AI pipeline, branding propagation, doc generation consumers)
**Target:** Clear all in **~16 weeks** (3.5–4 months) with 2-dev parallel execution

---

## Context

Session 2026-04-17 closed 16 gaps via 20 PRs. Remaining P0 are all large features requiring proper planning:

| Gap | Title | Scope |
|-----|-------|:-----:|
| GAP-005 | AI queue fair scheduling + horizontal scaling | L |
| GAP-047 | Document generation skills + infrastructure | XL |
| GAP-049 | Business logic correctness review | L |
| GAP-051 | Bulk import users xlsx/CSV | M-L |
| GAP-052 | Parent portal + accounts | XL |

Plus P1 clusters that batch naturally:
- **AI Pipeline:** GAP-002, 017, 019, 023 (anchor GAP-005)
- **Branding Propagation:** GAP-021, 032, 033, 037 (anchor GAP-010 done)
- **Doc Gen Consumers:** GAP-055, 057 (anchor GAP-047)

---

## Operating Constraints

- 1–2 devs + AI agents; sustained pace ≈ 1.5 dev-weeks/calendar week
- Wave length: **2–4 weeks** (split if >4 weeks)
- K-12 blockers (GAP-051, 052) pulled forward — gate pilot revenue
- Stakeholder inputs collected **Wave 0** in parallel
- 20% slack per wave (16-gap/day session was outlier, not baseline)

---

## Wave 0 — Stakeholder Sync (Parallel, 3–4 days)

**Deliverable:** `documents/03-planning/wave-p0-stakeholder-inputs.md` — checklist owned by stakeholders.

| Input Needed | Blocks Wave | Owner |
|--------------|:-----------:|-------|
| AI SLA per tier (Free/Pro/Enterprise) | 3 | Product |
| Pricing model for AI metering | 6 | Finance |
| Legal firm LOI (VN education law) | 9 | Legal/CEO |
| VN MoET circulars for K-12 retention | 2, 10 | Legal/Compliance |
| Doc template visuals (report card, payroll) | 8, 10, 11 | Design/Product |
| **GAP-002 async status audit** | 3 | Tech Lead |

---

## Wave Sequence

| # | Name | Weeks | Gaps | Track | Why |
|:-:|------|:-----:|------|:-----:|-----|
| 1 | Bulk Import MVP | 2 | GAP-051 | A | K-12 blocker, quick win |
| 2 | Parent Portal Identity | 3 | GAP-052a | B | K-12 blocker, plumbing |
| 3 | AI Async + Fair Queue P1 | 3 | GAP-002 verify + GAP-005a | A | Gates billing |
| 4 | Branding Propagation | 3 | GAP-021, 032, 033p, 037 | B | Parallel-friendly |
| 5 | Parent Portal Dashboard | 2 | GAP-052b | B | Wave 2 value ship |
| 6 | AI Billing + Observability | 3 | GAP-017, 019 | A | Revenue capture |
| 7 | Moderation Admin | 2 | GAP-023, 018 | B | Safety completion |
| 8 | Doc Gen Infra | 3 | GAP-047 P1 | A | Unblock all docs |
| 9 | Compliance MVP | 2 | GAP-049 | B | After surface area |
| 10 | Report Card Template | 3 | GAP-047 P2, GAP-055 | A | K-12 value |
| 11 | Payroll Slip | 2 | GAP-057 | B | Cluster wrap |

**Total:** 28w serial → **~16w with 2-dev tracks**.

### Track Assignment

- **Dev A (Infra):** Waves 1 → 3 → 6 → 8 → 10
- **Dev B (Product):** Waves 2 → 4 → 5 → 7 → 9 → 11

---

## Per-Wave Detail

Each wave section details: MVP scope, deferred scope, dependencies, stakeholder inputs, technical approach, files to create/touch, success criteria.

### Wave 1 — Bulk Import MVP (GAP-051, 2w)

**MVP:** `POST /api/v1/students/bulk-import` (xlsx), dry-run preview, batch 500-row chunks, error report xlsx, audit log. **Deferred:** teacher import, async progress, upsert.

**Stakeholder:** xlsx column schema locked Day 1, duplicate policy.

**Tech:** Apache POI 5.2 + OpenCSV 5.9. New package `kiteclass-core.module.student.bulkimport`. Reuse `StudentService.createStudent`.

**Success:** 500+ students in <20s P95, 1000-row integration test.

### Wave 2 — Parent Portal Identity (GAP-052a, 3w)

**MVP:** `Parent` entity + `ParentStudentLink`, email invite (24h token), `PARENT` role, JWT `linked_student_ids`, skeleton dashboard.

**Stakeholder:** VN PDPL parent PII guidance, invitation copy, dual-parent-same-email policy.

**Tech:** Replace `Student.java:121-122` commented FK with link-table. Feature flag `kiteclass.parent.portal.enabled=false` until PDPL ready.

**Success:** invite → signup → login → children count. 403 on other family (IT test).

### Wave 3 — AI Async + Fair Queue P1 (GAP-002 + GAP-005a, 3w)

**MVP:** Audit GAP-002 first. Priority queues `ai.request.{free,pro,enterprise}` weighted RR (3:2:1). Redis semaphore concurrency caps. Stateless consumer. Backpressure degrade. Metrics.

**Stakeholder:** SLA targets, concurrency caps — **Day 1 blocker**.

**Tech:** New `AIQueueConfig` (mirrors `EmailQueueConfig.java`), `AIQueueDispatcher` sole chokepoint. Redis-backed `AIRateLimitService`.

**Success:** 500 concurrent, no starvation, Enterprise P95 <30s. 2-consumer scale test.

### Wave 4 — Branding Propagation (GAP-021, 032, 033p, 037, 3w)

**MVP:** Thymeleaf branded emails, gateway branded 404/500, branded auth flows, `BrandingVersion` entity with manual rollback.

**Tech:** 5-min TTL per-tenant cache. `BrandingContext` React provider.

### Wave 5 — Parent Portal Dashboard (GAP-052b, 2w)

**MVP:** `GET /api/v1/parent/dashboard` aggregator (attendance 30d, grades, invoice, upcoming classes). Mobile-responsive.

**Tech:** `CompletableFuture` parallel service calls. `@ParentAccess` aspect. Batch-by-student-ids queries in 4 repos.

**Success:** <500ms P95, Mobile Lighthouse >85.

### Wave 6 — AI Billing + Observability (GAP-017, 019, 3w)

**MVP:** `AIUsageEvent` per request, `ai_usage_ledger`, monthly aggregation → invoice line items. Grafana dashboards. Alerts (queue depth, cost anomaly >3x 7d avg). Admin cost explorer.

**Stakeholder:** pricing model — **Day 1 blocker**.

### Wave 7 — Moderation Admin (GAP-023, 018, 2w)

**MVP:** Admin UI for `ModerationQueue`. `ModerationDecision` entity. Auto-escalation rules.

**Success:** 50 items/hour/moderator, audit trail.

### Wave 8 — Doc Gen Infra (GAP-047 P1, 3w)

**MVP:** New `kiteclass-core/module/document`. Apache POI + **OpenPDF** (AGPL-free). Strategy pattern. Async `document.generate` queue → S3. Reference: enrollment certificate.

**Risk:** iText AGPL → OpenPDF fallback. VN Unicode font test early.

**Success:** Branded certificate PDF <10s, S3 retrievable, 3 templates tested.

### Wave 9 — Compliance MVP (GAP-049, 2w)

**MVP:** 3 critical VN rules w/ legal sign-off (grade weights, attendance threshold, parent PII consent). `@ComplianceRule` annotation + AOP aspect. Registry dashboard. Quarterly review template.

**Deps:** Legal engagement (Wave 0) — **hard blocker**.

### Wave 10 — Report Card (GAP-047 P2, GAP-055, 3w)

**Scope:** MoET-compliant PDF per student or batch. Grades, GPA, comments, signature line, tenant-branded header.

### Wave 11 — Payroll Slip (GAP-057, 2w)

Payroll template + batch + e-sign hooks. Cluster 3 wrap.

---

## Cross-Wave Risks

| Risk | Wave | Mitigation |
|------|:----:|-----------|
| Stakeholder input lag | 0 | Kick off today, parallel to W1 |
| GAP-002 async incomplete | 3 | W0 audit catches; W3 +1-2w if needed |
| Legal lead time 2-4w | 0, 9 | Start contract today |
| iText AGPL licensing | 8 | OpenPDF fallback pre-committed |
| Dev bandwidth shocks | All | 20% slack; outlier days not baseline |

---

## Verification After Each Wave

1. CI green on main
2. `./scripts/pr-compliance-check.sh <PR>` passing
3. Wave completion check skill (7 levels)
4. Required audits per audit-gate mapping
5. Gap files marked DONE with PR links
6. ROADMAP.md updated
7. Actuals vs estimate noted for calibration

---

## Critical Files Referenced

Backend pattern anchors:
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/EmailQueueConfig.java` — all new queue configs mirror this
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/queue/BrandingJobConsumer.java` — async pattern
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/BackupStorageService.java` — S3 pattern
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/service/StudentService.java` — CRUD pattern

Skills to keep updated:
- `.claude/skills/workflow/wave-completion-check.md`
- `.claude/skills/workflow/gap-to-pr-converter.md`
- `.claude/skills/quality/business-logic-audit/SKILL.md`

---

## Log

- **2026-04-17** — Roadmap created from planning session after closing 16 gaps. Wave 0 stakeholder inputs collection kicked off.
