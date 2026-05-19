# Chapter Mapping — Docs to Thesis Chapters

Maps existing project documents to thesis chapter structure for easy reference extraction.

## Mapping Table

| Chapter | Title | Primary Sources | Supplementary |
|---------|-------|-----------------|---------------|
| Ch1 | Introduction | `01-business/` (domain overview, problem statement) + `chapter-1-competitor-analysis.md` Part 1 ✅ Wave 100 + `chapter-1-ai-techniques.md` Part 1 ✅ Wave 100 | `07-archived/research/competitive/` (market analysis); Part 2 (threat-to-validity + IEEE 15+ + cross-jurisdiction) defer Wave 101 |
| Ch2 | Theoretical Background | `07-archived/research/technology/` (tech evaluation) | `08-thesis/references/technology-stack.md` (stack rationale) |
| Ch3 | Requirements Analysis | `01-business/` (business rules per domain) | `06-diagrams/` (use case diagrams, ERD) |
| Ch4 | System Design | `02-architecture/` (architecture decisions) | `06-diagrams/` (architecture diagrams, class diagrams) |
| Ch5 | Implementation | `05-guides/` (operations guides) | `03-planning/kitehub-saas-implementation-plan.md` (wave strategy) |
| Ch6 | Testing & Evaluation | `04-quality/` (audit reports, completion checks) | `08-thesis/references/testing-results.md` (test metrics) |
| Ch7 | Conclusion | `04-quality/` (final quality audit reports) | `08-thesis/references/quality-metrics.md` (score timeline) |

## Chapter Details

### Chapter 1: Introduction
- **Problem statement**: Multi-tenant SaaS platform for education management
- **Scope**: KiteHub (SaaS platform) + KiteClass (tenant application)
- **Status**: Part 1 ✅ Wave 100 (Bucket D) — competitor analysis + AI techniques + bibliography seed; Part 2 ⏳ defer Wave 101 (threat-to-validity + 15+ IEEE sources + cross-jurisdiction + PDPL 2025 timeline)
- **Sources**:
  - `chapter-1-competitor-analysis.md` — Phần 1: phân tích 4 đối thủ VN edu SaaS (MISA/Mona/Easy Edu/DotB)
  - `chapter-1-ai-techniques.md` — Phần 2: kỹ thuật AI tích hợp trong KiteHub (AI Branding + Quality Gate + roadmap)
  - `01-business/README.md` — Business rules overview
  - `07-archived/research/competitive/` — Competitive landscape analysis
  - `references/bibliography.md` — 38 IEEE sources (Wave 100 added 8 new for Chapter 1)

### Chapter 2: Theoretical Background
- **Technology foundations**: Microservices, Spring Boot, Next.js, Docker
- **Methodology**: Superpowers, Agile, TDD
- **Sources**:
  - `07-archived/research/technology/` — Technology evaluation and comparison
  - `08-thesis/references/technology-stack.md` — Final stack with rationale
  - `08-thesis/references/methodology.md` — Development methodology

### Chapter 3: Requirements Analysis
- **Functional requirements**: Business rules per domain (subscription, billing, etc.)
- **Non-functional requirements**: Performance, security, scalability
- **Sources**:
  - `01-business/` — All domain business rule files
  - `06-diagrams/plantuml/` — Use case diagrams, ERD

### Chapter 4: System Design
- **Architecture**: Microservices with API Gateway
- **Database design**: Multi-tenant PostgreSQL with schema isolation
- **Sources**:
  - `02-architecture/` — Architecture decisions, data retention, SSL, domain management
  - `06-diagrams/plantuml/` — Architecture diagrams, class diagrams, sequence diagrams

### Chapter 5: Implementation
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
