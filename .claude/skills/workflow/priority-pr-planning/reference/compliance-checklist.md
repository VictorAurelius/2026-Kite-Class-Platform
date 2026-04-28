# Mandatory Compliance Checklist + Compliance Matrix

> Pointer: read this when building the plan — these items MUST be present in every priority PR. Parent skill: `../SKILL.md`.

## ✅ Mandatory Compliance Checklist

### 1. Quality Standards (From Master Plan)

**Every priority PR MUST comply with:**

#### Backend Quality Standards
- [ ] **Code Coverage**: Minimum 80% for service layer (JaCoCo)
- [ ] **Test Types**: Unit tests (Mockito) + Integration tests (Testcontainers)
- [ ] **No Warnings**: Zero compiler warnings, zero deprecation warnings
- [ ] **JavaDoc**: All public methods with `@param`, `@return`, `@throws`
- [ ] **Error Handling**: Error codes from `messages.properties`
- [ ] **Validation**: Jakarta Bean Validation on DTOs
- [ ] **Multi-Tenant**: All entities with `instance_id` + Hibernate filters
- [ ] **Soft Delete**: `deleted` flag + `...AndDeletedFalse` repository methods
- [ ] **Audit Fields**: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- [ ] **Git Hooks**: Pre-commit checks pass

**Reference:** `.claude/skills/code-style.md`, `testing-guide.md`, `spring-boot-testing-quality.md` <!-- TODO: verify against current state -->

#### Frontend Quality Standards
- [ ] **TypeScript Strict**: No `any` type, all props typed
- [ ] **Component Structure**: Proper UI/container/hooks separation
- [ ] **Testing**: React Testing Library for all components
- [ ] **Accessibility**: ARIA labels, semantic HTML, keyboard nav
- [ ] **Error Handling**: User-friendly messages, loading states
- [ ] **API Integration**: React Query with proper cache
- [ ] **State Management**: Zustand/Context properly
- [ ] **Form Validation**: Zod schemas with clear errors
- [ ] **Feature Gates**: Use `<FeatureGate>` for tier features
- [ ] **Responsive**: Mobile-first, all screen sizes

**Reference:** `.claude/skills/frontend-development.md`, `frontend-code-quality.md` <!-- TODO: verify against current state -->

#### Security Standards
- [ ] **Input Validation**: Validate at API and UI layers
- [ ] **SQL Injection**: Parameterized queries (automatic with Spring Data JPA)
- [ ] **XSS Prevention**: Escape output, store raw in DB
- [ ] **Authentication**: JWT with refresh mechanism
- [ ] **Authorization**: RBAC enforcement
- [ ] **Multi-Tenant Isolation**: Hibernate filters
- [ ] **Internal APIs**: HMAC-SHA256 signatures
- [ ] **Sensitive Data**: Never commit secrets

**Reference:** `.claude/skills/architecture-overview.md`, `cross-service-data-strategy.md` <!-- TODO: verify against current state -->

#### Testing Standards
- [ ] **Unit Tests**: Fast, isolated, mocked dependencies
- [ ] **Integration Tests**: Real DB with Testcontainers
- [ ] **API Tests**: Full HTTP request/response cycle
- [ ] **Edge Cases**: Validation errors, boundaries, nulls
- [ ] **Multi-Tenant Tests**: Tenant isolation verified
- [ ] **Error Scenarios**: 4xx and 5xx responses tested
- [ ] **CI Pipeline**: All tests pass in GitHub Actions

**Reference:** `.claude/skills/testing-guide.md` <!-- TODO: verify against current state -->

---

### 2. Workflow Compliance (From Skills)

**Every priority PR MUST follow:**

#### Git Workflow
- [ ] **Branch Naming**: `{type}/KC-{id}-{short-desc}` (lowercase, `-`, ticket ID)
  - Examples: `fix/KC-001-multi-tenant-email`, `feature/KC-002-gateway-integration`
- [ ] **Branch From**: Always branch from `main` (after `git pull origin main`)
- [ ] **Commit Format**: Conventional Commits (type(scope): subject)
- [ ] **Commit Message**: HEREDOC for complex commits with body
- [ ] **Co-Authored-By**: ALWAYS include for AI assistance <!-- TODO: verify against current state — current CLAUDE.md says do NOT add Co-Authored-By trailer -->

**Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

#### Pull Request Process
- [ ] **PR Title**: `{type}({service}): {description} (KC-{id})`
- [ ] **PR Body**: Summary, Problem, Solution, Changes, Testing, Checklist, References
- [ ] **PR Creation**: Use `gh pr create` with detailed body
- [ ] **CI Monitoring**: `scripts/check-ci.sh` to monitor tests
- [ ] **Merge Strategy**: `gh pr merge --squash --delete-branch`

**Reference:** `.claude/skills/development-workflow.md` - Section "Pull Request Process"

---

### 3. Documentation Compliance

**Every priority PR MUST update:**

- [ ] **STATUS-UPDATE-YYYY-MM-DD.md**: Mark PR as complete, update progress %
- [ ] **Master Implementation Plan**: Update PR status (✅ done)
- [ ] **MEMORY.md**: Add lessons learned if applicable
- [ ] **README**: Update if adding new features/endpoints

---

## 📊 Priority Plan Compliance Matrix

| Requirement | Master Plan Source | Priority Plan Location | Status |
|-------------|-------------------|------------------------|--------|
| **Backend Quality** | Lines 85-96 | Section 7: Acceptance Criteria | ✅ Required |
| **Frontend Quality** | Lines 99-111 | Section 7: Acceptance Criteria | ✅ Required |
| **Security Standards** | Lines 113-122 | Section 7: Acceptance Criteria | ✅ Required |
| **Testing Standards** | Lines 125-134 | Section 4: Testing Checklist | ✅ Required |
| **Git Workflow** | `development-workflow.md` | Section 2: Workflow | ✅ Required |
| **Commit Format** | `development-workflow.md` | Section 5: Commit Strategy | ✅ Required |
| **PR Process** | `development-workflow.md` | Section 6: CI Validation | ✅ Required |
| **Multi-Tenant** | Architecture skill | Implementation + Tests | ✅ Required |
| **Soft Delete** | Architecture skill | Implementation + Tests | ✅ Required |
| **Error Codes** | `error-logging.md` | Implementation + Tests | ✅ Required |
