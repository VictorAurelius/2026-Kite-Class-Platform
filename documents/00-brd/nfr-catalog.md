# Non-Functional Requirements Catalog

**Status:** skeleton
**Created:** 2026-04-29
**Updated:** 2026-04-29
**Owner:** Architect + PM
**Reviewer:** Tech Lead + SRE
**Related Gap:** [GAP-150](../04-quality/gaps/GAP-150-brd-docs-completion.md) (content creation tracking)

---

## 1. Scope / Context

TODO: Mô tả 1 đoạn — NFR catalog định nghĩa quality attributes (uptime, performance, scalability, security, accessibility) per tier. Liên kết với `pricing-model.md` (tier mapping) + ops audit baselines (`output-review-mandate.md` §3 Ops Readiness 52/100, Performance 63/100). Mục tiêu Wave 5+: cải thiện baselines theo NFR targets dưới đây.

**Status:** baselines captured in `documents/04-quality/audits/`; targets dưới đây là 6-month + 12-month goals, không phải current state.

---

## 2. Availability / Uptime SLA

| Tier | Target Uptime | Allowed Downtime/Month | Credits |
|------|:-------------:|:----------------------:|:-------:|
| FREE | None (best-effort) | — | None |
| BASIC | 99.5% | TODO ~3.6h | TODO 5% credit |
| PREMIUM | 99.9% | TODO ~43m | TODO 10% credit |
| ENTERPRISE | 99.95% (negotiable) | TODO ~22m | Custom in MSA |

**Measurement:**
- TODO — define synthetic monitor (e.g. Pingdom, Datadog)
- TODO — exclude scheduled maintenance windows (max X hours/quarter, ≥7d notice)
- TODO — measurement endpoint + grace period

---

## 3. Disaster Recovery (RTO/RPO)

| Scenario | RTO Target | RPO Target | Notes |
|----------|:----------:|:----------:|-------|
| Single service crash | TODO <5m | TODO 0 | auto-restart, no data loss |
| Single AZ failure | TODO <30m | TODO <5m | multi-AZ for ≥PREMIUM |
| Region failure | TODO <4h | TODO <1h | DR region (cold standby) |
| DB corruption | TODO <2h | TODO <15m | Point-in-time recovery |
| Catastrophic (region + backups gone) | TODO <24h | TODO <24h | Off-region cold backups |

**Backup policy:**
- TODO daily full + N hourly incremental
- Retention: 30d hot + 1y cold (PII compliance per `compliance-scope.md` §2.6)
- Off-region copy ≥PREMIUM tier
- Quarterly DR drill mandatory (link `documents/05-guides/incident-response/`)

---

## 4. Performance Budgets

### 4.1 API latency

| Endpoint class | P50 | P95 | P99 | Notes |
|---------------|:---:|:---:|:---:|-------|
| Read (GET, cached) | TODO 50ms | TODO 200ms | TODO 500ms | Hit Redis hot path |
| Read (GET, DB) | TODO 100ms | TODO 400ms | TODO 1000ms | Indexed query |
| Write (POST/PUT) | TODO 200ms | TODO 800ms | TODO 2000ms | Includes outbox |
| AI inference (sync) | TODO N/A — banned | — | — | Always async (`ai-branding §3.3`) |
| AI inference (async job) | TODO 30s | TODO 90s | TODO 180s | Per `ai-branding §3.3` 2-5min ceiling |

### 4.2 Frontend bundle budgets

| Asset | Initial | Per-route | Notes |
|-------|:-------:|:---------:|-------|
| JS (gzipped) | TODO <200KB | TODO <100KB | Code split per route |
| CSS (gzipped) | TODO <50KB | TODO <20KB | Tailwind purge |
| Total page weight | TODO <500KB | TODO <300KB | Excluding lazy assets |
| Image (per page) | TODO ≤3 | — | Lazy + responsive |

### 4.3 Web Vitals (per Lighthouse / RUM)

| Metric | P75 Target |
|--------|:----------:|
| LCP (Largest Contentful Paint) | TODO <2.5s |
| INP (Interaction to Next Paint) | TODO <200ms |
| CLS (Cumulative Layout Shift) | TODO <0.1 |
| TTFB | TODO <800ms |

### 4.4 Database

| Metric | Target |
|--------|:------:|
| Connection pool utilization (avg) | TODO <70% |
| Slow query (>500ms) rate | TODO <0.1% |
| N+1 detection | TODO 0 in CI |

---

## 5. Scalability Targets

### 5.1 Capacity per tenant
| Dimension | BASIC | PREMIUM | ENTERPRISE |
|-----------|:---:|:-------:|:----------:|
| Concurrent users | TODO 50 | TODO 500 | TODO 5000+ |
| Request rate (req/s) | TODO 5 | TODO 50 | TODO 500+ |
| DB rows (per major table) | TODO 100K | TODO 1M | TODO 10M+ |

### 5.2 Platform total
TODO:
- Tenants: 10K target year 1
- Active users (daily): 100K target year 1
- Storage: link `pricing-model.md` cumulative

### 5.3 Horizontal scaling
TODO:
- Stateless services (kiteclass-core, kitehub-*) — autoscale based on CPU/memory + queue depth
- Stateful (Postgres) — vertical first, sharding plan post 1M tenants
- AI workers — bulkhead per `ai-branding §11.4.4` Oracle 24GB constraint

---

## 6. Security NFRs

(Detailed in `compliance-scope.md` §2 PDPL + §4 Cybersecurity. NFR view = measurable targets.)

| Requirement | Target |
|-------------|:------:|
| Pen-test cadence | Annual |
| Critical CVE patch SLA | TODO <72h |
| High CVE patch SLA | TODO <7d |
| Failed login lockout | TODO 5 attempts → 15min lock |
| Session timeout (inactive) | TODO 30min |
| Encryption at rest | AES-256 (Postgres TDE / EBS) |
| Encryption in transit | TLS 1.3 minimum |
| Secrets rotation | Quarterly auto |

---

## 7. Accessibility (a11y)

**Target:** WCAG 2.1 Level AA across all UIs (per `.claude/rules/ai-branding-guidelines.md` §5 + §8)

| Requirement | Target |
|-------------|:------:|
| Color contrast | ≥4.5:1 (normal) / ≥3:1 (large) |
| Keyboard navigation | All interactive elements |
| Screen reader | NVDA + VoiceOver tested |
| Focus indicators | Visible, ≥2px |
| Alt text | All meaningful images |
| Form errors | Programmatically associated |
| Language declaration | `<html lang="vi">` (default) + `lang` attrs cho mixed content |

---

## 8. Maintainability NFRs

| Metric | Target |
|--------|:------:|
| Test coverage (line) | TODO ≥80% |
| Test coverage (branch) | TODO ≥70% |
| CI pipeline duration | TODO <15m |
| Mean PR cycle time | TODO <2d |
| Build reproducibility | Hash-pinned deps |
| Documentation freshness | Living Docs rule (CLAUDE.md) |

---

## 9. Observability NFRs

(Implementation tracked GAP-114/115/116; spec in `.claude/rules/logs-format-standard.md`)

| Requirement | Target |
|-------------|:------:|
| Structured JSON logs | All services |
| Log retention hot/warm/cold | 7d/30d/180d (per `logs-format-standard.md` §4) |
| Distributed tracing | All requests have traceId |
| Metrics scraping | Prometheus 15s interval |
| Alert MTTR (P1) | TODO <30m |
| Alert MTTR (P2) | TODO <4h |
| Dashboard coverage | Per service: rate/error/latency |

---

## 10. Tier-NFR Matrix

| NFR Class | FREE | BASIC | PREMIUM | ENTERPRISE |
|-----------|:----:|:---:|:-------:|:----------:|
| Uptime SLA | None | 99.5% | 99.9% | 99.95% |
| RTO | Best-effort | 4h | 1h | <30m |
| RPO | None | 1h | 15m | 5m |
| Multi-AZ | ❌ | ❌ | ✅ | ✅ |
| Off-region backup | ❌ | ❌ | ✅ | ✅ |
| Custom NFR (negotiated) | ❌ | ❌ | ❌ | ✅ |

---

## 11. Dependencies / References

- BRD: [`pricing-model.md`](pricing-model.md) §2 — tier feature gates
- BRD: [`business-objectives.md`](business-objectives.md) §4.5 — platform health KPIs
- BRD: [`compliance-scope.md`](compliance-scope.md) — security baseline
- Audits: `documents/04-quality/audits/ops-readiness/` (baseline 49/100 → 52/100), `performance/` (baseline 58/100 → 63/100)
- Rule: [`.claude/rules/post-wave-audit-mandate.md`](../../.claude/rules/post-wave-audit-mandate.md) — cadence
- Rule: [`.claude/rules/logs-format-standard.md`](../../.claude/rules/logs-format-standard.md) — observability spec
- Rule: [`.claude/rules/ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md) §5 WCAG AA, §11.4.4 bulkhead
- Skill: `.claude/skills/quality/performance-audit/`, `.claude/skills/quality/ops-readiness-audit/`

---

## 12. Out of Scope (this skeleton)

- Final NFR numbers signed off (Phase 2 — needs Architect + SRE workshop)
- SLA contract templates per tier (Legal, separate gap)
- Capacity model spreadsheet (Architect, separate work)
- Specific monitoring vendor selection (Architecture decision, ADR)

---

## 13. Log

- 2026-04-29 — Skeleton created (GAP-150 Phase 1). NFR table structure complete with placeholder targets; final values require Architect + SRE workshop in Phase 2 GAP-155. Cross-references to existing baselines (Ops 52/100, Performance 63/100) preserve continuity với audit history.
