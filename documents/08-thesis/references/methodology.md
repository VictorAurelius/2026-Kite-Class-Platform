# Development Methodology Reference

Overview of the development methodology used in the Kite Class Platform project.

## Superpowers Methodology

A structured development workflow enforced for every pull request:

### 1. Quick Brainstorm (5-10 minutes)
- Analyze scope, risks, and edge cases
- Identify dependencies and blockers
- Document findings before writing any code

### 2. Task Breakdown (5-10 minutes)
- Decompose the PR into specific, estimable tasks
- Estimate effort for each task (S/M/L)
- Identify parallelizable vs sequential tasks

### 3. TDD: Test-Driven Development
- **Red**: Write failing tests first
- **Green**: Write minimum code to pass tests
- **Refactor**: Improve code quality while keeping tests green
- Tests and code must be in the same commit

### 4. Implementation
- Follow the task breakdown order
- Commit frequently (small, focused commits)
- Each commit should be independently reviewable

### 5. Code Review (Self-Review)
- Two-stage review: correctness first, then quality
- Check against coding standards
- Verify test coverage meets thresholds

## Wave Execution Strategy

The project uses a parallel wave execution strategy to maximize throughput:

| Wave | Focus | Duration |
|------|-------|----------|
| Wave 1 | Core SaaS features (subscription, trial, billing) | ~2 days |
| Wave 2 | Email lifecycle, data retention, quality improvements | ~2 days |
| Wave 3 | Custom domains, advanced billing, CI/CD | ~2 days |
| Wave 4 | Template gallery, config API, E2E testing | ~2 days |
| Wave 5 | AI integration, blog/SEO, Docker optimization | ~2 days |

### Wave Principles
- Each wave delivers a complete, testable increment
- PRs within a wave can be executed in parallel
- Wave completion is verified with a completion check (`workflow/wave-completion-check.md`)
- No wave starts until the previous wave passes its completion check

## Agile Practices

### Sprint Structure
- **Sprint length**: 1 wave (~2 days)
- **Planning**: Task breakdown at wave start
- **Review**: Quality audit at wave end
- **Retrospective**: Methodology adjustments between waves

### Documentation-Driven Development
- Business documents are the source of truth (`01-business/`)
- Document changes MUST accompany code changes in the same PR
- Business docs are created BEFORE implementation begins

## 3-Layer Pre-Flight Check

Before starting any PR, a three-layer verification is performed:

### Layer 1: PR-Level Check
- Are requirements clear and complete?
- Are dependencies available?
- Is the scope appropriate for a single PR?

### Layer 2: Domain-Level Check
- Does a business document exist for this domain?
- Are business rules fully specified?
- Are edge cases documented?

### Layer 3: Project-Level Check
- Does this PR align with the current wave plan?
- Are there conflicts with other in-progress PRs?
- Does this change affect shared infrastructure?

## Quality Assurance

### Quality Audit System
- 100-point scoring system across multiple dimensions
- Automated checks: linting, type checking, test coverage
- Manual review: architecture, naming conventions, documentation

### Test Coverage Requirements
- **Java (backend)**: Lines 80%, Functions 80%, Branches 75%
- **TypeScript (frontend)**: Lines 80%, Functions 80%, Branches 75%
- **E2E**: Critical user flows covered

### CI/CD Pipeline
- Automated on every push: lint, type-check, test, build
- Branch protection: PRs require passing CI before merge
- Quality gates: Coverage thresholds enforced
