# Portable Skill — adapt {project} placeholders to your project

# Skill: Test-Driven Development Enforcement

**Purpose:** Enforce RED-GREEN-REFACTOR cycle for higher quality, fewer bugs.

---

## When to Use

**Mandatory:** New features, bug fixes, API endpoints, business logic (services, repositories).

**Skip:** Refactoring with existing tests, typos, doc updates, config files, generated code (Lombok getters/setters).

**Default:** If in doubt, write test first. Cost is low, benefit is high.

---

## RED-GREEN-REFACTOR Cycle

### RED: Write Failing Test First (5-10 min)

1. **Write test BEFORE any production code**
2. **Test must FAIL when first run** (if it passes, test is wrong)
3. **Test should be specific and clear** (Arrange-Act-Assert)

```java
// Name: method_Scenario_ExpectedResult
@Test
void createUser_WithValidData_ShouldReturnUserWithGeneratedId() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest("John", "john@example.com");
    // Act
    UserResponse response = userService.createUser(request);
    // Assert
    assertThat(response.getId()).isNotNull();
    assertThat(response.getName()).isEqualTo("John");
}
```

```typescript
test('UserList should display loading state initially', () => {
  render(<UserList />);
  expect(screen.getByText('Loading...')).toBeInTheDocument();
});
```

Run test — expected: FAIL (not implemented yet).

### GREEN: Minimal Code to Pass (10-20 min)

1. **Write just enough code to pass the test** — no over-engineering
2. **No premature optimization** — no caching, batching, async unless tested
3. **No extra features** — only what the test requires

```java
public UserResponse createUser(CreateUserRequest request) {
    User saved = userRepository.save(userMapper.toEntity(request));
    return userMapper.toResponse(saved);
}
```

Run test — expected: PASS.

### REFACTOR: Clean Up (5-15 min)

1. Remove duplication (extract mappers, shared logic)
2. Improve naming (clear, descriptive variables)
3. Extract methods if complex

Run ALL tests — expected: STILL PASS.

---

## Pre-Commit Hook (Optional Enforcement)

**Location:** `.claude/scripts/pre-commit.sh`

Core logic: for each modified source file, check if corresponding test file exists and was also modified.

```bash
#!/bin/bash
# TDD Enforcement Check
MODIFIED_SRC=$(git diff --cached --name-only --diff-filter=ACM | grep "src/main/.*\.java$")

for src_file in $MODIFIED_SRC; do
  test_file=$(echo "$src_file" | sed 's/src\/main/src\/test/' | sed 's/\.java$/Test.java/')
  if [ ! -f "$test_file" ]; then
    echo "WARNING: Missing test file: $test_file"
  fi
done
```

Start with WARNING mode, switch to BLOCKING mode (exit 1) once team is comfortable.

---

## Quick Reference Checklist

- [ ] **RED:** Did I write test FIRST?
- [ ] **RED:** Did test FAIL initially?
- [ ] **GREEN:** Did I write minimal code? (no over-engineering)
- [ ] **GREEN:** Does test PASS now?
- [ ] **REFACTOR:** Did I clean up code? (DRY, clear names)
- [ ] **REFACTOR:** Do all tests still PASS?
- [ ] **PRE-PUSH:** `scripts/test-local.sh` pass? (full suite before push)

---

## Task Ordering for TDD

- **Bottom-up:** Entity -> Repository -> Service -> Controller -> Tests
- **Test-first:** Test -> Entity -> Test -> Repository -> Test -> Service

---

## Success Metrics

- TDD compliance rate per PR (target: 85%+)
- Test coverage on new code (target: 85%+)
- Bug escape rate trend (target: -40%)
