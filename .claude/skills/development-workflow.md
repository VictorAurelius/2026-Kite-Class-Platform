# Skill: Development Workflow

**Version:** 2.0 (Merged from 5 workflow skills)
**Last Updated:** 2026-01-27
**Purpose:** Comprehensive development workflow for KiteClass Platform - từ planning đến deployment

---

## 📋 Overview

Skill này tổng hợp toàn bộ quy trình phát triển:
- **Git Workflow**: Branching, commits, pull requests
- **Feature Development**: Planning, coding, testing checklist
- **Documentation**: Update plan tracking và quick-start
- **Quality Assurance**: Code review, merge criteria

---

## 🎯 When to Use This Skill

**Use this workflow for ALL development work:**
- ✅ Implementing new features
- ✅ Fixing bugs
- ✅ Refactoring code
- ✅ Updating documentation
- ✅ Before merging PRs

---

## 📐 Development Workflow Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                    DEVELOPMENT WORKFLOW                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. PLAN        2. CODE         3. TEST        4. DOCUMENT       │
│     │               │               │               │             │
│     ▼               ▼               ▼               ▼             │
│  Requirements   Implementation  Unit Tests    Update Docs        │
│  Design Mapping  Coding         Integration   Plan Tracking      │
│  Task Breakdown  Standards      E2E Tests     Quick-Start        │
│     │               │               │               │             │
│     └───────────────┴───────────────┴───────────────┘             │
│                         │                                         │
│                         ▼                                         │
│              5. REVIEW & COMMIT                                   │
│                         │                                         │
│                         ▼                                         │
│              6. MERGE TO DEVELOP                                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

# PART 1: GIT WORKFLOW

## 🌳 Branching Strategy (Git Flow)

```
main ────●────────────────●────────────────●────────────────●─────► (Production)
         │                ▲                ▲                ▲
         │                │                │                │
develop ─┼──●──●──●──●────┼────●──●──●─────┼────●──●────────┼─────► (Integration)
         │  │             │    │           │    │           │
         │  │  feature/   │    │           │    │           │
         │  └──KC-123 ────┘    │           │    │           │
         │                     │           │    │           │
         │        feature/     │           │    │           │
         │        KC-456 ──────┘           │    │           │
         │                                 │    │           │
         │                    hotfix/      │    │           │
         │                    KC-789 ──────┴────┘           │
         │                                                  │
         │                                      release/    │
         │                                      v1.0 ───────┘
```

### Branch Types

| Branch | Pattern | From | Merge To | Purpose |
|--------|---------|------|----------|---------|
| `main` | `main` | - | - | Production code |
| `develop` | `develop` | `main` | `main` | Integration branch |
| `feature` | `feature/KC-{id}-{desc}` | `develop` | `develop` | New features |
| `bugfix` | `bugfix/KC-{id}-{desc}` | `develop` | `develop` | Bug fixes |
| `hotfix` | `hotfix/KC-{id}-{desc}` | `main` | `main`, `develop` | Urgent fixes |
| `release` | `release/v{version}` | `develop` | `main`, `develop` | Release prep |

### Branch Naming Rules

**Format:**
```
{type}/KC-{ticket-id}-{short-description}
```

**Examples:**
```bash
# Features
feature/KC-123-student-enrollment
feature/KC-456-qr-payment-integration

# Bug fixes
bugfix/KC-321-fix-attendance-calculation

# Hotfixes
hotfix/KC-999-security-vulnerability

# Releases
release/v1.0.0
```

**Rules:**
- Chỉ dùng lowercase
- Dùng dấu `-` thay space
- Giữ ngắn gọn (< 50 chars)
- Luôn có ticket ID

---

## 📝 Commit Messages (Conventional Commits)

### Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer]

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

### Commit Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(student): add enrollment API` |
| `fix` | Bug fix | `fix(attendance): correct calculation` |
| `docs` | Documentation | `docs(api): update swagger specs` |
| `style` | Formatting | `style: format with prettier` |
| `refactor` | Refactoring | `refactor(billing): extract payment service` |
| `test` | Tests | `test(student): add unit tests` |
| `chore` | Build, CI, tooling | `chore: update dependencies` |
| `perf` | Performance | `perf(query): optimize student list` |

### Commit Examples

```bash
# Feature with body
feat(gateway): implement PR 1.5 - Email Service

Implement complete email service with password reset functionality.

Features:
- Email service with reactive patterns
- Password reset flow
- HTML email templates

Tests:
- 5 unit tests (all passing)
- 8 integration tests

Files: 19 files

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>

# Bug fix
fix(attendance): correct percentage calculation

The attendance percentage was including cancelled sessions.
Now only counts active sessions.

Fixes KC-456

# Breaking change
feat(api)!: change pagination response format

BREAKING CHANGE: Pagination now uses `content` instead of `data`.
Migration guide in docs/migration/v2.md
```

### Commit Message Rules

- **Subject**: Imperative mood, lowercase, no period (< 72 chars)
- **Body**: Wrap at 72 chars, explain what & why
- **Footer**: Reference tickets, breaking changes
- **Co-Authored-By**: ALWAYS include for AI assistance

### Using HEREDOC for Complex Commits

```bash
git commit -m "$(cat <<'EOF'
feat(core): implement PR 2.3 - Student Module

Complete Student module implementation with full CRUD operations.

Features:
- Student entity with soft delete
- CRUD endpoints with pagination
- Business rules validation
- Redis caching with 1-hour TTL

Tests:
- 35/41 tests passing (85% coverage)
- 6 tests pending Docker/security setup

Files: 15 files changed

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## 🔀 Pull Request Process

### 1. Creating a Feature Branch

```bash
# Sync with develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/KC-123-new-feature

# Make changes and commit
git add .
git commit -m "feat(module): implement feature"

# Push and create PR
git push -u origin feature/KC-123-new-feature
```

### 2. PR Template

```markdown
## Description
Implement student bulk import feature allowing admins to upload Excel files.

## Type of Change
- [x] New feature
- [ ] Bug fix
- [ ] Breaking change
- [ ] Documentation update

## Related Tickets
- Closes KC-123
- Related to KC-456

## Changes Made
- Added Excel upload endpoint
- Implemented file parsing service
- Added validation for required fields
- Created error report generator

## Checklist
- [x] Code follows project style guidelines
- [x] Self-reviewed my code
- [x] Added unit tests
- [x] All tests passing locally
- [x] Updated documentation

## Testing Instructions
1. Go to Students → Import
2. Upload the sample file from `test/fixtures/students.xlsx`
3. Verify import results
```

### 3. Merge Strategy

| Branch | Merge Method | Reason |
|--------|--------------|--------|
| feature → develop | **Squash merge** | Clean history |
| release → main | **Merge commit** | Preserve release commits |
| hotfix → main | **Merge commit** | Audit trail |
| main → develop | **Merge commit** | Sync changes |

---

# PART 2: FEATURE DEVELOPMENT CHECKLIST

## ✅ Phase 1: Planning & Design (Before Coding)

### Database Design Mapping

- [ ] Xác định tables liên quan trong `database-design.md`
- [ ] Review schema và columns cần thiết
- [ ] Xác định indexes cần thiết (performance)
- [ ] Xác định relationships (FK, constraints)
- [ ] Migration script đã được tạo (Flyway)

### API Design Mapping

- [ ] Xác định endpoints cần implement
- [ ] Xác định request/response DTOs
- [ ] Xác định error codes và messages
- [ ] OpenAPI spec đã được update

### UI Design Mapping (Frontend)

- [ ] Xác định components cần tạo
- [ ] Review Figma/wireframe
- [ ] Xác định state management

### Task Breakdown

- [ ] Chia nhỏ feature thành tasks cụ thể
- [ ] Estimate complexity (T-shirt sizing: S/M/L/XL)
- [ ] Identify blockers và dependencies

---

## 💻 Phase 2: Implementation (Coding)

### Backend Coding Standards (Java/Spring Boot)

**Package Structure:**
```
com.kiteclass.{service}
├── common/          (shared code)
├── config/          (Spring configuration)
└── module/
    └── {module}/    (e.g., student)
        ├── controller/
        ├── service/
        ├── repository/
        ├── dto/
        ├── entity/
        └── mapper/
```

**Naming Conventions:**
- Classes: `PascalCase` (e.g., `StudentService`)
- Methods/Variables: `camelCase` (e.g., `getStudentById`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_STUDENTS`)
- DTOs: Use Java Records
- Validation: `@Valid`, `@NotNull`, `@Size`
- Exceptions: Custom exceptions, `@RestControllerAdvice`

### Frontend Coding Standards (TypeScript/React)

**Component Naming:**
- Components: `PascalCase` (e.g., `StudentList`)
- Hooks: `use` prefix (e.g., `useAuth`, `useStudents`)
- Types/Interfaces: `PascalCase` (e.g., `Student`, `ApiResponse`)
- Files: `kebab-case` (e.g., `student-list.tsx`)

**State Management:**
- React Query: Server state
- Zustand: Client state
- Context API: Shared UI state

### Comment Guidelines

**When to Comment:**
- ✅ Complex business logic
- ✅ Algorithm explanation
- ✅ Workaround with reason
- ✅ TODO with ticket number

**When NOT to Comment:**
- ❌ Self-explanatory code
- ❌ Redundant comments
- ❌ Commented-out code (delete it)

**Examples:**

```java
// ✅ GOOD - Complex business logic
// Calculate attendance percentage excluding cancelled sessions
// Formula: (attended / total_active_sessions) * 100
double percentage = calculateAttendancePercentage(student, sessions);

// ❌ BAD - Obvious comment
// Get student by ID
Student student = studentRepository.findById(id);

// ✅ GOOD - Workaround with reason
// WORKAROUND: MapStruct cannot map BaseEntity fields via Builder
// See: https://github.com/mapstruct/mapstruct/issues/XXXX
Student student = mapper.toEntity(request);
student.setId(1L);

// ✅ GOOD - TODO with ticket
// TODO(KC-789): Add email notification when status changes to GRADUATED
```

### Design Patterns to Use

**Backend Patterns:**
- Repository Pattern: Data access abstraction
- DTO Pattern: Separate entity and transfer objects
- Service Layer: Business logic isolation
- Builder Pattern: Complex object creation
- MapStruct: Type-safe bean mapping

**Frontend Patterns:**
- Container/Presentational: Logic vs UI separation
- Custom Hooks: Reusable logic
- Composition: Component composition over inheritance
- Provider Pattern: Context API for shared state

---

## 🧪 Phase 3: Testing

### Unit Tests (REQUIRED)

**Coverage Requirements:**
- Minimum: 80% coverage
- Service layer: 90%+ coverage
- Controllers: Happy path + error cases
- Repositories: Basic CRUD operations

**Testing Patterns:**

```java
// Service Unit Test (Mockito)
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository repository;

    @Mock
    private StudentMapper mapper;

    @InjectMocks
    private StudentServiceImpl service;

    @Test
    void createStudent_shouldCreateSuccessfully() {
        // Given
        CreateStudentRequest request = createDefaultRequest();
        Student entity = createDefaultStudent();
        when(repository.existsByEmailAndDeletedFalse(any())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        // When
        StudentResponse response = service.createStudent(request);

        // Then
        assertNotNull(response);
        verify(repository).save(entity);
    }
}
```

### Integration Tests

**What to Test:**
- API endpoint testing (MockMvc)
- Database interaction (Testcontainers)
- Authentication/Authorization flows
- External service integration

```java
// Integration Test with Testcontainers
@SpringBootTest
@Testcontainers
class StudentRepositoryTest extends IntegrationTestBase {
    @Autowired
    private StudentRepository repository;

    @Test
    void findByEmailAndDeletedFalse_shouldReturnStudent() {
        // Given
        Student student = createAndSaveStudent("test@example.com");

        // When
        Optional<Student> found = repository.findByEmailAndDeletedFalse("test@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }
}
```

### Test Checklist

- [ ] Unit tests cover happy path
- [ ] Unit tests cover edge cases
- [ ] Unit tests cover error scenarios
- [ ] Integration tests for API endpoints
- [ ] Integration tests for database operations
- [ ] All tests pass locally
- [ ] No test warnings or flaky tests

---

## 📚 Phase 4: Documentation Updates

### 4.1 Update Implementation Plan

**WHEN:** After completing any PR implementation

**FILE:** `/documents/scripts/kiteclass-implementation-plan.md`

**WHAT TO UPDATE:**

#### Progress Tracking Section

```markdown
## Gateway Service (feature/gateway branch)
- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
...
- ✅ PR 1.X: [Just completed PR] ← UPDATE THIS

**Gateway Status:** X/7 PRs completed (X.X%) ← UPDATE THIS
**Tests:** XX passing (XX unit + XX integration) ← UPDATE THIS
**Last Updated:** 2026-XX-XX (PR 1.X - Description) ← UPDATE THIS
**Current Work:** [Next planned work] ← UPDATE THIS
```

#### PR Status Icon

Change from `⏳` (pending) to `✅` (completed):

```markdown
## ✅ PR 1.5 - Email Service  ← Change icon
```

#### Overall Progress

```markdown
**Overall Progress:** X/27 PRs completed (X.X%) ← UPDATE THIS
**Last Updated:** 2026-XX-XX (PR X.X - Description) ← UPDATE THIS
```

---

### 4.2 Update QUICK-START.md

**WHEN:** After completing PR, before ending session

**FILE:** Service-specific `docs/QUICK-START.md`
- Gateway: `kiteclass-gateway/docs/QUICK-START.md`
- Core: `kiteclass-core/docs/QUICK-START.md`
- Frontend: `kiteclass-web/docs/QUICK-START.md`

**WHAT TO UPDATE:**

#### Current State Section

```markdown
## 🚀 Current State

**Latest Completed:** PR X.X - Feature Name
**Branch:** feature/{service}
**Tests:** XX passing (XX unit + XX integration)
**Docker:** ✅ Ready
**Status:** Ready for PR X.X
```

#### Add New PR Option

```markdown
## 🚀 Option X: PR X.X - Feature Name

**Copy this prompt when context is cleared:**

\```
I'm continuing development on KiteClass {Service} Service.

CURRENT STATE:
- Working directory: /path/to/kiteclass-{service}
- Branch: feature/{service}
- Completed: PR X.X (Feature Name)
- Status: XX tests, Docker setup complete

DOCUMENTATION TO READ:
1. /path/to/docs/README.md
2. /path/to/docs/PR-X.X-SUMMARY.md

MY REQUEST: Implement PR X.X - Next Feature

REQUIREMENTS:
- Requirement 1
- Requirement 2
- Add tests

CONSTRAINTS:
- Follow reactive patterns (Gateway)
- Follow module structure (Core)
- Use existing error handling

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Communicate in Vietnamese for easier control.**

Please read the docs first, then help me implement step by step.
\```
```

#### Update Completed Items

Mark completed PRs:
```markdown
### Completed PRs
- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
- ✅ PR 1.X: [Just completed] ← ADD THIS
```

---

### 4.3 Update Module Documentation

**WHEN:** After implementing Core Service modules

**FILES:** `kiteclass-core/docs/modules/{module}-module.md`

**WHAT TO UPDATE:**
- Implementation status
- Business rules (if changed)
- API endpoints (if added/modified)
- Error scenarios (if new errors added)
- Caching strategy (if changed)
- Future enhancements (if completed items)

**Template:** See `kiteclass-core/docs/module-business-logic.md`

---

## 📋 Phase 5: Documentation Update Checklist

**Before Committing PR:**

- [ ] Implementation plan status icon updated (⏳ → ✅)
- [ ] Progress tracking statistics updated
- [ ] Test count updated
- [ ] Last Updated date updated
- [ ] Current Work updated
- [ ] Overall progress percentage recalculated
- [ ] QUICK-START.md updated with new PR option
- [ ] QUICK-START.md current state updated
- [ ] Module docs updated (if applicable)

---

## 🔍 Phase 6: Code Review & Commit

### KiteClass-Specific Code Review Checklist

**⚠️ CRITICAL: These checks are MANDATORY for KiteClass Platform**

#### Multi-Tenant Security Checks

- [ ] **Tenant Context Injection**: All repository queries include `instance_id` filter
- [ ] **No Hardcoded Instance IDs**: No `UUID.fromString("...")` in code
- [ ] **TenantContext Usage**: Use `TenantContext.getCurrentInstanceId()` instead of hardcoded IDs
- [ ] **@TenantScoped Annotation**: All repositories have `@TenantScoped` or manual tenant filtering
- [ ] **Cross-Tenant Access Prevention**: Tested that users from Tenant A cannot access Tenant B data
- [ ] **JWT Token Validation**: JWT tokens are validated for correct instanceId
- [ ] **API Response Filtering**: Responses only include data for current tenant
- [ ] **Bulk Operations Safety**: Bulk operations (delete, update) respect tenant boundaries

**Example of CORRECT tenant filtering:**
```java
// ✅ GOOD - Uses tenant context
@Query("SELECT s FROM Student s WHERE s.instanceId = :instanceId AND s.deletedAt IS NULL")
Page<Student> findByInstanceId(@Param("instanceId") UUID instanceId, Pageable pageable);

// ❌ BAD - No tenant filter
@Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL")
Page<Student> findAll(Pageable pageable);
```

#### Feature Detection Checks

- [ ] **Feature Gate Usage**: Premium features wrapped in `@RequireFeature("FEATURE_NAME")`
- [ ] **Tier-Based Access Control**: API returns 403 if feature unavailable for tier
- [ ] **UI Feature Gating**: Frontend components use `<FeatureGate>` for tier-locked features
- [ ] **Upgrade Prompts**: Locked features show upgrade prompt with required tier
- [ ] **Feature Config Caching**: Instance feature config cached with 1-hour TTL
- [ ] **Graceful Degradation**: Missing features degrade gracefully (not crash)

**Example of CORRECT feature gating:**
```java
// ✅ GOOD - Feature gate with annotation
@PostMapping("/engagement/track")
@RequireFeature("ENGAGEMENT")
public ResponseEntity<EngagementResponse> trackEngagement(@Valid @RequestBody TrackEngagementRequest request) {
    return ResponseEntity.ok(engagementService.track(request));
}

// ❌ BAD - No feature gate
@PostMapping("/engagement/track")
public ResponseEntity<EngagementResponse> trackEngagement(@Valid @RequestBody TrackEngagementRequest request) {
    return ResponseEntity.ok(engagementService.track(request));
}
```

#### Payment Security Checks

- [ ] **Amount Validation**: Payment amounts match tier pricing (499k/999k VND)
- [ ] **Double Payment Prevention**: Order status checked before processing
- [ ] **Order Expiry Validation**: QR codes expire after 10 minutes
- [ ] **Transaction Idempotency**: VietQR callbacks are idempotent
- [ ] **Audit Logging**: All payment state changes logged
- [ ] **Error Handling**: Payment failures logged with reason
- [ ] **No Financial Data in Logs**: Never log full credit card/bank details

**Example of CORRECT payment validation:**
```java
// ✅ GOOD - Validates order status
public void verifyPayment(VerifyPaymentRequest request) {
    PaymentOrder order = orderRepo.findByOrderId(request.getOrderId())
        .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

    // Prevent double payment
    if (order.getStatus() == PaymentStatus.PAID) {
        throw new PaymentAlreadyPaidException(order.getOrderId());
    }

    // Verify amount matches
    if (!order.getAmount().equals(request.getAmount())) {
        throw new AmountMismatchException(order.getAmount(), request.getAmount());
    }

    order.setStatus(PaymentStatus.PAID);
    orderRepo.save(order);
}

// ❌ BAD - No validation
public void verifyPayment(VerifyPaymentRequest request) {
    PaymentOrder order = orderRepo.findByOrderId(request.getOrderId()).get();
    order.setStatus(PaymentStatus.PAID);
    orderRepo.save(order);
}
```

#### Trial System Checks

- [ ] **Trial Status Validation**: Trial days remaining calculated correctly
- [ ] **Grace Period Handling**: Grace period (3 days) enforced after trial expires
- [ ] **Suspension Logic**: Instance suspended after grace period expires
- [ ] **Data Retention**: Suspended instances retained for 90 days before deletion
- [ ] **Status Transitions**: Only valid status transitions allowed (TRIAL → GRACE → SUSPENDED → DELETED)
- [ ] **Banner Display**: Correct trial banner shown based on status

#### AI Service Checks (if applicable)

- [ ] **Circuit Breaker**: AI calls wrapped in circuit breaker (Resilience4j)
- [ ] **Timeout Handling**: AI calls timeout after 10 seconds
- [ ] **Fallback Strategy**: Graceful fallback if AI service unavailable
- [ ] **Cost Controls**: Request throttling to prevent runaway costs
- [ ] **Error Logging**: AI failures logged with error details
- [ ] **Retry Logic**: Exponential backoff retry (max 3 attempts)

#### General Security Checks

- [ ] **Input Validation**: All user inputs validated with `@Valid`, `@NotNull`, `@Size`
- [ ] **SQL Injection Prevention**: Using JPA/JPQL (not raw SQL)
- [ ] **XSS Prevention**: Frontend sanitizes user-generated content
- [ ] **CSRF Protection**: CSRF tokens enabled for state-changing operations
- [ ] **Authentication**: All non-public endpoints require JWT token
- [ ] **Authorization**: Role-based access control enforced (OWNER, TEACHER, STUDENT)
- [ ] **Sensitive Data**: No passwords, API keys, secrets in code/logs
- [ ] **Rate Limiting**: API endpoints rate-limited (100 req/min per instance)

---

### Code Review Checklist (Self-Review)

**Before Creating PR:**

- [ ] Code đúng requirement
- [ ] **ALL KiteClass-specific checks passed** (above section)
- [ ] Không có security issues
- [ ] Xử lý error cases đầy đủ
- [ ] Có unit tests (coverage >= 80%)
- [ ] Code clean, readable
- [ ] Không có duplicate code
- [ ] Performance acceptable
- [ ] No compilation warnings
- [ ] Documentation updated

### Warning Policy

| Warning Type | Action |
|--------------|--------|
| Compilation warning | MUST fix before merge |
| Deprecated API | Must have upgrade plan |
| Security warning | CANNOT merge |
| Performance warning | Review and document |

### Commit Workflow

**Step 1: Check Git Status**
```bash
git status
```

**Step 2: Stage Changes**
```bash
git add -A
```

**Step 3: Commit with Detailed Message**

Use HEREDOC for complex commits:

```bash
git commit -m "$(cat <<'EOF'
feat(core): implement PR 2.3 - Student Module

Complete Student module with CRUD operations and business rules.

Features:
- Student entity with soft delete
- CRUD endpoints with pagination
- Business rules validation (email/phone uniqueness)
- Redis caching with 1-hour TTL
- MapStruct for DTO mapping

Changes:
- Created Student entity, DTOs, mapper, service, controller
- Created Flyway migration V2__create_student_tables.sql
- Created 5 test classes (35/41 tests passing)
- Updated module business logic documentation

Tests:
- 10/10 service tests passing ✅
- 3/3 mapper tests passing ✅
- 0/5 controller tests (needs security config)
- 0/6 repository tests (needs Docker)

Files: 15 files changed

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

**Step 4: Verify Commit**
```bash
git log -1 --stat
```

### Commit Rules

1. **ALWAYS commit after implementing features**
2. **NEVER skip commit step**
3. **ALWAYS use Co-Authored-By for AI assistance**
4. **ALWAYS include test results**
5. **ALWAYS list major changes**

---

## ✅ Phase 7: Merge Criteria

### PR Merge Checklist

**MUST PASS before merge:**

- [ ] All checklist items completed
- [ ] Tests pass 100% (or documented failures with reason)
- [ ] No compilation warnings
- [ ] Code review approved
- [ ] Documentation updated (plan, quick-start, module docs)
- [ ] No security warnings
- [ ] CI/CD pipeline green

### Cannot Merge If:

- ❌ Any checklist item uncompleted
- ❌ Tests failing (without documented reason)
- ❌ Security warnings present
- ❌ No code review
- ❌ Breaking changes without migration guide

---

# PART 3: RELEASE & HOTFIX PROCESSES

## 🚀 Release Process

### 1. Create Release Branch

```bash
# From develop
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0
```

### 2. Version Bump

```bash
# Update version in:
# - pom.xml: <version>1.2.0</version>
# - package.json: "version": "1.2.0"

git add .
git commit -m "chore: bump version to 1.2.0"
```

### 3. Release Notes

```markdown
# Release v1.2.0

## New Features
- KC-123: Student bulk import
- KC-456: QR payment integration

## Bug Fixes
- KC-789: Fix attendance calculation

## Breaking Changes
- None

## Migration Steps
1. Run database migration: `flyway migrate`
2. Update environment variables (see `.env.example`)
```

### 4. Merge to Main & Tag

```bash
# Merge to main
git checkout main
git merge release/v1.2.0 --no-ff -m "Release v1.2.0"
git tag -a v1.2.0 -m "Version 1.2.0"
git push origin main --tags

# Merge back to develop
git checkout develop
git merge release/v1.2.0 --no-ff
git push origin develop

# Delete release branch
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

---

## 🔥 Hotfix Process

**ONLY for critical production bugs!**

```bash
# 1. Create from main
git checkout main
git pull origin main
git checkout -b hotfix/KC-999-critical-fix

# 2. Fix and commit
git add .
git commit -m "fix(auth): patch security vulnerability"

# 3. Merge to main
git checkout main
git merge hotfix/KC-999-critical-fix --no-ff
git tag -a v1.2.1 -m "Hotfix v1.2.1"
git push origin main --tags

# 4. Merge to develop
git checkout develop
git merge hotfix/KC-999-critical-fix --no-ff
git push origin develop

# 5. Cleanup
git branch -d hotfix/KC-999-critical-fix
```

---

# PART 4: TROUBLESHOOTING

## Common Issues & Solutions

### Issue: Merge Conflicts

**Solution:**
```bash
# Update from develop
git checkout feature/your-branch
git fetch origin
git merge origin/develop

# Resolve conflicts in IDE
# Then commit
git add .
git commit -m "chore: resolve merge conflicts"
```

### Issue: Accidental Commit to Wrong Branch

**Solution:**
```bash
# Undo last commit (keep changes)
git reset HEAD~1 --soft

# Switch to correct branch
git checkout correct-branch

# Commit again
git add .
git commit -m "your message"
```

### Issue: Need to Amend Last Commit

**Solution:**
```bash
# Make additional changes
git add .

# Amend last commit (without editing message)
git commit --amend --no-edit

# Or amend with new message
git commit --amend
```

---

## 📊 Quick Reference

### Git Aliases (Recommended)

Add to `~/.gitconfig`:

```bash
[alias]
    co = checkout
    br = branch
    ci = commit
    st = status

    # Feature workflow
    feature = "!f() { git checkout develop && git pull && git checkout -b feature/$1; }; f"

    # Pretty log
    lg = log --oneline --graph --decorate --all

    # Amend without edit
    amend = commit --amend --no-edit

    # Undo last commit (keep changes)
    undo = reset HEAD~1 --soft
```

### Common Commands

```bash
# Start new feature
git feature KC-123-new-feature

# View pretty history
git lg

# Check branch status
git status

# Quick commit all changes
git add -A && git commit -m "feat: quick description"

# Push and set upstream
git push -u origin feature/KC-123-new-feature
```

---

## 📋 Complete Development Checklist

Use this checklist for EVERY feature/PR:

### Planning ✅
- [ ] Database design mapped
- [ ] API design mapped
- [ ] UI design mapped (if applicable)
- [ ] Tasks broken down

### Coding ✅
- [ ] Code follows style guidelines
- [ ] Comments added for complex logic
- [ ] No commented-out code
- [ ] Design patterns applied

### Testing ✅
- [ ] Unit tests written (>= 80% coverage)
- [ ] Integration tests written
- [ ] All tests passing locally
- [ ] No test warnings

### Documentation ✅
- [ ] Implementation plan updated
- [ ] QUICK-START.md updated
- [ ] Module docs updated (if applicable)
- [ ] OpenAPI specs updated (if API changes)

### Review ✅
- [ ] Self-review completed
- [ ] No security issues
- [ ] No compilation warnings
- [ ] Performance acceptable

### Commit ✅
- [ ] Changes staged
- [ ] Conventional commit message
- [ ] Co-Authored-By included
- [ ] Test results included in commit message

### Merge ✅
- [ ] PR created with template
- [ ] CI/CD pipeline green
- [ ] Code review approved
- [ ] Merged to develop

---

## 🎯 Related Skills

- `code-style.md` - Coding standards and patterns
- `api-design.md` - REST API design guidelines
- `database-design.md` - Database schema design
- `testing-guide.md` - Testing strategies and patterns
- `error-logging.md` - Exception handling and logging
- `kiteclass-core/docs/module-business-logic.md` - Module documentation template

---

**Last Updated:** 2026-01-30
**Version:** 2.1 (Added KiteClass-Specific Code Review Checklist)
**Merged From:**
- git-workflow.md
- pr-commit-workflow.md
- feature-development-checklist.md
- update-plan-tracking.md
- update-quick-start.md

**Author:** KiteClass Team
**Status:** ✅ Active
