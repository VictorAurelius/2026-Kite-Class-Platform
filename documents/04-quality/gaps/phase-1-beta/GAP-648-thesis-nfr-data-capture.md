# GAP-648: Thesis NFR data capture — load test + CloudWatch dashboards + AWS cost

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed (Backend + DevOps)
**Phase:** phase-1-beta
**Found:** 2026-05-18
**Related Audits:** [thesis-defense-failure-mode-matrix](../../audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md), [thesis-vn-saas-benchmark](../../audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md)

## Current State (verified 2026-05-18)

| Piece | Status |
|---|---|
| Load test scenario (k6/JMeter) | ❌ missing — không có file `tests/load/*.js` |
| Concurrent users baseline | ❌ chưa đo |
| API p50/p95/p99 latency capture | ⚠️ CloudWatch metrics exist (post-GAP-437 Phase 1) nhưng chưa screenshot capture cho thesis |
| AWS Cost Explorer export | ❌ chưa export |
| Service Registry -95% overhead benchmark | ❌ claim không có evidence (Failure-mode A2) |
| RDS query latency profiling | ⚠️ existing performance audit 86/100 có data nhưng không thesis-ready format |

## Problem

VN thesis benchmark Q2 + Failure-mode B1/A2/A4 cite "load test = không thể thiếu top schools" + "Service Registry -95% claim không có benchmark artifact" + "AWS Singapore latency benchmark không có". Hội đồng UIT/HUST/UET 2026 expect NFR section với số liệu cụ thể, không chỉ "hệ thống có thể scale" qualitative.

3 critical NFR datasets missing:
1. Load test results (k6 50-100 concurrent, P95 < 2s baseline)
2. CloudWatch p50/p95 dashboard screenshots ≥30-day window
3. AWS Cost Explorer monthly breakdown by service (cost justification cho ADR-025)

## Proposed Fix

### Step 1: k6 load test scenario

Create `tests/load/`:
- `tests/load/k6-baseline.js` — 50 concurrent users, 5-min ramp, key endpoints (login / list classes / create attendance / get invoice)
- `tests/load/k6-stress.js` — ramp 0→200 concurrent, identify breaking point
- `tests/load/README.md` — how to run + expected baseline (P95 < 2s, error rate < 1%)
- Output: `tests/load/results/2026-05-XX-baseline.json` committed cho thesis cite

Per `agent-action-bias.md` Part B — k6 (CLI) over UI tooling. Install: `apt-get install k6` hoặc Docker `grafana/k6:latest`.

### Step 2: CloudWatch dashboard screenshot capture

Script `scripts/capture-cloudwatch-thesis-screenshots.sh`:
- AWS CLI `aws cloudwatch get-metric-widget-image` cho metrics:
  - API Gateway p50/p95/p99 latency (per service)
  - RDS connection pool usage
  - EC2 CPU/memory utilization
  - SES email send rate + bounce rate
- Output PNG `documents/04-quality/audits/aws-verification/2026-MM-DD-cloudwatch-thesis-screenshots/`
- Cite Tier 1 read-only per `agent-aws-access.md` §2.1

Run weekly cho ≥30-day window trước defense.

### Step 3: AWS Cost Explorer export

`scripts/export-aws-cost-thesis.sh`:
- `aws ce get-cost-and-usage --granularity MONTHLY --metrics UnblendedCost AmortizedCost --group-by Type=DIMENSION,Key=SERVICE`
- Time period: ≥3 months retrospective (per defense window)
- Output CSV + chart `documents/08-thesis/references/aws-cost-monthly.csv` + `.png`
- Trim free tier credit application impact (ADR cite)

### Step 4: Service Registry benchmark

Per Failure-mode A2 — claim "-95% overhead" cần benchmark artifact:
- Micro-benchmark JMH OR Gatling cho path A (with Service Registry) vs path B (without)
- Output: `documents/04-quality/audits/performance/2026-MM-DD-service-registry-benchmark.md` + raw CSV
- Cite trong Chapter 4 §Đánh giá

### Step 5: NFR section template trong thesis-docx

Pair với GAP-646 thesis-docx-pipeline:
- Chapter 4 §Đánh giá phi chức năng — table response time target vs measured, availability SLA, security score timeline, cost breakdown
- Auto-inject từ datasets captured trong Steps 1-4

## Acceptance Criteria

- [ ] `tests/load/` k6 baseline + stress scenarios committed
- [ ] `tests/load/results/` ≥1 baseline run result committed (50 concurrent P95 < 2s confirmed)
- [ ] `scripts/capture-cloudwatch-thesis-screenshots.sh` shipped + 1 weekly run output committed
- [ ] `scripts/export-aws-cost-thesis.sh` shipped + ≥3-month cost CSV committed
- [ ] Service Registry benchmark artifact committed
- [ ] NFR table data points ready cho Chapter 4 injection
- [ ] CI smoke (load test --dry-run mode) exit 0

## Related

- GAP-646 thesis-docx-pipeline (Chapter 4 NFR section injection)
- GAP-650 thesis-chapter-1-literature (cites NFR data)
- Performance audit 86/100 baseline (existing — extend data capture format)
- `documents/02-architecture/adr/ADR-025-aws-singapore-free-tier-phase-1-beta.md` (cost justification)
- `agent-aws-access.md` §2.1 Tier 1 read-only commands

## Log

- **2026-05-18 (created):** Filed per outside-in audit. VN benchmark Q2 + Failure-mode B1/A2/A4 convergence on NFR data missing as P0 thesis-blocker.
- **2026-05-23:** DEFER Wave thesis-2 — k6 production load test + CloudWatch p50/p95 ≥30 ngày + AWS Cost Explorer screenshots cần production cluster live. Wave thesis-1 (`documents/03-planning/waves/wave-2026-05-23-thesis-1-closure.md`) scope-out per `outside-in-coverage-trigger.md` §4 row 4 (Wave 100 audit ≤30 ngày). Trigger restart: GAP-612 DONE + cluster live ≥7 ngày.
