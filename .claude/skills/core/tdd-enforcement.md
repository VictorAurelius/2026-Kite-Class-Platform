---
description: "Dùng trước khi viết code, user nói 'implement', 'viết feature', 'add endpoint', 'TDD', 'test trước'. Bắt buộc: new features, bug fixes, API endpoints, business logic. Skip: refactoring (tests exist), typos, config files, Lombok getters/setters."
---

# TDD Enforcement

## Khi nào dùng (Mandatory)

- New features (all new code)
- Bug fixes — write failing test reproducing bug FIRST
- API endpoints (test contract before implementation)
- Business logic (services, repositories)

## Khi nào skip

- Refactoring existing code (tests already exist)
- Typos, documentation updates, config files (application.yml, pom.xml)
- Simple getters/setters (Lombok generates)

**Default:** If in doubt, write test first. Cost is low, benefit is high.

## RED → GREEN → REFACTOR

1. **🔴 RED** — Write failing test (defines expected behavior). Test PHẢI fail khi first run.
2. **🟢 GREEN** — Write MINIMAL code to pass test. No premature optimization, no extra features.
3. **♻️ REFACTOR** — Clean up (remove duplication, improve naming). All tests PHẢI still pass.

## Gotchas

- **MockMvc async** — Reactive endpoints (`Mono<ResponseEntity>`) cần pattern đặc biệt:
  ```java
  MvcResult async = mockMvc.perform(post("/api/branding/ai/analyze-logo")...)
      .andExpect(request().asyncStarted()).andReturn();
  mockMvc.perform(asyncDispatch(async)).andExpect(status().isOk());
  ```
- **Testcontainers fields** — `@Container` manages lifecycle; dùng `@SuppressWarnings("resource")` trên field, KHÔNG manually `.close()`
- **Git hook timestamp** — File mới (chưa có git history) luôn bypass TDD check (timestamp=0). Commit test trước, code sau để enforce
- **`scripts/test-local.sh` trước push** — TDD development: OK dùng `./mvnw test -Dtest=Specific`. Nhưng PHẢI dùng `scripts/test-local.sh` trước push (full suite)
- **Pre-commit hook hiện tại** — Warning mode (Week 1-4), blocking mode Week 5+; chỉ check Java files (backend)

## Enforcement

Git pre-commit hook tại `.claude/scripts/pre-commit-check.sh` — auto-check timestamp test vs code file.

## Skill Contents

- `quick-reference/tdd-workflow-diagram.md` — Visual RED-GREEN-REFACTOR flow
- `quick-reference/tdd-phases.md` — Full phase details với Java + TypeScript examples
- `quick-reference/tdd-git-hook.md` — Git hook implementation + known limitations

## Trigger Phrases

"implement", "viết feature", "add endpoint", "TDD", "test trước", "create service", "add method"

## Quick Checklist

- [ ] 🔴 RED: Test written FIRST? (before production code)
- [ ] 🔴 RED: Test FAILS initially? (proves test works)
- [ ] 🟢 GREEN: Minimal code only? (no over-engineering)
- [ ] 🟢 GREEN: Test PASSES now?
- [ ] ♻️ REFACTOR: Code cleaned up? (DRY, clear naming)
- [ ] ♻️ REFACTOR: ALL tests still pass?
- [ ] Pre-push: `scripts/test-local.sh` pass?
