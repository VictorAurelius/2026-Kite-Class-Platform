# Chapter Mapping — Docs to Thesis Chapters

Maps existing project documents to thesis chapter structure for easy reference extraction.

## V1 Status (Wave 100.7 Phase 4 closure 2026-05-19)

✅ **Thesis V1 milestone SHIPPED** — Ch.1 (3 parts) + Ch.2 + Ch.3 + Ch.4 narrative content + 44 IEEE refs bibliography + 89% inline cite utilization + cross-ref-audit Round 3 production-ready. Ch.5-7 defer Wave 102+ (Phase 1 BETA scope). Defense prep (Phase 5) defer Wave 110+ per GAP-653.

| Phase | Status | PRs |
|---|---|---|
| Phase 1 (Wave 100 D+F+100.5) | ✅ DONE | #1580, #1581, #1583, #1584 |
| Phase 2 (Wave 100.7 narrative) | ✅ DONE | #1587, #1588, #1589 |
| Phase 3a (bibliography polish) | ✅ DONE | #1592 |
| Phase 3b (DOCX scoping) | 🟡 PARTIAL 20% — GAP-646 | #1593 |
| Phase 4 (V1 closure) | ✅ DONE | #1595, #1596, #1597, #1598, #1599 |
| Phase 5 (defense prep) | ⏳ DEFER Wave 110+ | GAP-653 |

---

## Mapping Table

| Chapter | Title | Primary Sources | Supplementary |
|---------|-------|-----------------|---------------|
| Ch1 | Introduction | `01-business/` (domain overview, problem statement) + `chapter-1-competitor-analysis.md` Part 1 ✅ Wave 100 + `chapter-1-ai-techniques.md` Part 2 ✅ Wave 100 + `chapter-1-vn-law-methodology.md` Part 3 ✅ Wave 100.7 Phase 2 Agent 2a | `07-archived/research/competitive/` (market analysis); cross-jurisdiction extension (GDPR vs PDPL vs PDPA SG) defer Wave 101 |
| Ch2 | System Architecture | `02-architecture/{multi-tenant-architecture, multi-tenant-isolation-patterns, service-catalog-and-auth-flow, c4-context-container, database-architecture-map}.md` + `chapter-2-system-architecture.md` ✅ Wave 100.7 Phase 2-2b | `04-quality/audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md` (VN edu SaaS positioning); `08-thesis/references/technology-stack.md` (stack rationale) |
| Ch3 | Implementation (Code Snippets) | `chapter-3-implementation.md` ✅ Wave 100.7 Phase 2 Agent 2c — 5 code snippets representative từ JWT filter + Tenant RLS + Outbox dispatcher + Beta controller + Next.js page | `01-business/` (business rules per domain); `06-diagrams/` (use case diagrams + ERD); merge với Requirements Analysis bằng cross-reference Section 2 |
| Ch4 | Deployment Results + KPI | `chapter-4-deployment-results.md` ✅ Wave 100.7 Phase 2 Agent 2c — AWS Singapore Free Tier + User onboarding flow + KPI structure + Beta scope (placeholders cho real numbers chờ data từ NFR đo lường + beta cohort feedback — Wave thesis-2 scope) | `02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md`; `02-architecture/deployment-strategy.md`; `06-diagrams/` |
| Ch5 | Implementation roadmap (defer Wave 102+) | `05-guides/` (operations guides) | `03-planning/kitehub-saas-implementation-plan.md` (wave strategy) |
| Ch6 | Testing & Evaluation | `04-quality/` (audit reports, completion checks) | `08-thesis/references/testing-results.md` (test metrics) |
| Ch7 | Conclusion | `04-quality/` (final quality audit reports) | `08-thesis/references/quality-metrics.md` (score timeline) |

## Chapter Details

### Chapter 1: Introduction
- **Problem statement**: Multi-tenant SaaS platform for education management
- **Scope**: KiteHub (SaaS platform) + KiteClass (tenant application)
- **Status**: Part 1 + Part 2 + Part 3 ✅ Wave 100 + Wave 100.7 Phase 2 — competitor analysis + AI techniques + VN law + audit-driven methodology; cross-jurisdiction extension (GDPR vs PDPL vs PDPA SG) ⏳ defer Wave 101+
- **Sources**:
  - `chapter-1-competitor-analysis.md` — §1.1 Giới thiệu chung đề tài + §1.2 Cơ sở chuyên ngành + §1.3 Khảo sát thị trường + 5 hệ thống tham khảo (BeeClass + MISA + Mona + Easy Edu + DotB) — restructured Wave 102.5 Bucket C per khung-chuẩn UTC numbering 1.X.Y.Z (G11 + G15)
  - `chapter-1-ai-techniques.md` — §1.4 Kỹ thuật AI tích hợp (AI Branding + Quality Gate + roadmap) — renumbered Wave 102.5 Bucket C
  - `chapter-1-vn-law-methodology.md` — §1.5 Khung pháp lý VN (PDPL 2023 + Cybersecurity 2018 + Thông tư 78) + §1.6 Phương pháp luận Quality-Driven Development 4 trụ cột + §1.7 Phạm vi đề tài (Item 10 phase intro hybrid) + Kết luận Chương 1 thống nhất — restructured Wave 102.5 Bucket C
  - `chapter-1-conclusion-backup-2026-05-20.md` — backup §1.7 conclusion (Item 5) per docs-archival-cadence.md Tier 2 timestamp
  - `01-business/README.md` — Business rules overview
  - `07-archived/research/competitive/` — Competitive landscape analysis
  - `references/bibliography.md` — 43 IEEE sources (Wave 100 added 8 new + Wave 100.7 Phase 2 added 5 new for Chapter 1)

### Chapter 2: System Architecture
- **Scope**: Functional Requirements + Non-Functional Requirements + Architecture (C4 L1+L2, multi-tenant single-bucket RLS, service decomposition, defense-in-depth 5 layers) + SaaS Model (lifecycle + billing + plan tier) + Blended Learning Context (VN edu market characteristics)
- **Status**: ✅ Wave 100.7 Phase 2-2b shipped (`chapter-2-system-architecture.md` ~12-15 pages Vietnamese narrative compressed from 5 source architecture docs)
- **Sources**:
  - `chapter-2-system-architecture.md` — Compressed narrative (this chapter)
  - `02-architecture/multi-tenant-architecture.md` — Defense-in-depth 5 layers + RLS implementation + cross-tenant leak prevention
  - `02-architecture/multi-tenant-isolation-patterns.md` — ADR-style 6 patterns comparative matrix (Pool model adopted Phase 1 BETA)
  - `02-architecture/service-catalog-and-auth-flow.md` — Backstage service catalog + dependency graph + auth sequence
  - `02-architecture/c4-context-container.md` — C4 Level 1 + Level 2 diagrams + narrative per cluster
  - `02-architecture/database-architecture-map.md` — 91 entity catalog + RLS coverage 56% + per-service mapping

### Chapter 3: Implementation (Code Snippets) ✅ Wave 100.7 Phase 2 Agent 2c
- **Scope**: 5 đoạn code snippet đại diện từ source code thực tế (cite file:line)
- **Status**: ✅ DONE Wave 100.7 Phase 2 Agent 2c
- **Sources**:
  - `chapter-3-implementation.md` — 5 snippets: JWT Auth (kitehub-gateway) + Tenant RLS Interceptor (kiteclass-core) + Outbox Dispatcher (kitehub-subscription) + Beta Access Controller (kitehub-subscription) + Next.js page (kitehub-frontend)
  - Snippets pull từ source code thực — không paraphrase
  - Mỗi snippet kèm phân tích design pattern + cite file path + line range

### Chapter 4: Deployment Results + KPI ✅ Wave 100.7 Phase 2 Agent 2c
- **Scope**: Triển khai AWS Singapore Free Tier + User onboarding + KPI structure + Beta scope
- **Status**: ✅ DONE Wave 100.7 Phase 2 Agent 2c với placeholders cho real numbers (defer Wave thesis-2 — chờ data từ NFR đo lường + beta cohort feedback)
- **Sources**:
  - `chapter-4-deployment-results.md` — 4 sections (4.1 Cloud AWS + 4.2 User Onboarding + 4.3 KPI + 4.4 Beta Scope)
  - `02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md` — Singapore Free Tier rationale
  - `02-architecture/deployment-strategy.md` — Architecture B 5 nguyên tắc deploy
  - Pending: real cost numbers (GAP-648), beta evidence ≥4 tenants (GAP-649), lessons learned post-tenant interview (Wave 102+)

### Chapter 5: Implementation Roadmap (defer Wave 102+)
- **Wave execution**: 5 waves of parallel implementation
- **Key features**: Subscription management, email lifecycle, custom domains, billing
- **Sources**:
  - `05-guides/` — Operations and setup guides
  - `03-planning/kitehub-saas-implementation-plan.md` — Full implementation plan with PR tracking
  - `03-planning/parallel-execution-strategy.md` — Wave parallelization strategy

### Chapter 6: Testing & Evaluation
- **Test coverage**: Unit, integration, E2E tests
- **Quality audits**: Systematic scoring methodology
- **Sources**:
  - `04-quality/` — All quality audit reports and wave completion checks
  - `08-thesis/references/testing-results.md` — Aggregated test metrics

### Chapter 7: Conclusion
- **Quality trajectory**: Score improvements over time
- **Lessons learned**: Methodology effectiveness
- **Sources**:
  - `04-quality/quality-audit-*` — Final audit reports
  - `08-thesis/references/quality-metrics.md` — Score timeline and trends
