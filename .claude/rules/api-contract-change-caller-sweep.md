---
paths:
  - "**/*.java"
---

# API-Contract Change Caller Sweep — change a method contract → sweep all callers (prod + test) + run tests, not just compile

**Priority:** 🟠 MANDATORY — code-change completeness governance
**Version:** 1.0.0
**Created:** 2026-05-28
**Last-Reviewed:** 2026-05-28
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on GAP-799 двойн miss 2026-05-28) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "method-contract change → caller sweep + full-test verify"; sister của `cross-flow-bug-class-sweep.md` ở axis khác (caller sweep vs bug-class sweep); META P0 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi PR thay đổi **method contract** của 1 Java method: đổi signature (thêm/bớt param), swap call-site sang method khác, thêm `@Deprecated`, rename, đổi return type/exception. Scope = `**/*.java` (production + test).

---

## 1. The Rule

> **Khi thay đổi method contract (signature / call-site swap / `@Deprecated` / rename), PHẢI trong CÙNG PR:**
> 1. **Sweep TẤT CẢ callers** — production AND test (mock stubs `when(...)`, `verify(...)`, tests-of-the-method, deprecated-method usages).
> 2. **Migrate/clean** mỗi caller (cập nhật call, cập nhật mock, suppress/remove deprecated usage).
> 3. **Chạy affected test suite** (`./mvnw test`, KHÔNG chỉ `compile`) trước khi push — compile KHÔNG bắt được stale mock + test assertion fail.

`compile` chỉ verify type-correctness. Stale Mockito stubs + behavioral test assertions chỉ fail khi RUN tests. Deprecated-method usages còn sót = công việc dở dang (warnings + drift).

---

## 2. Trigger pattern — khi nào fire

| Thay đổi | Ví dụ | Caller class cần sweep |
|---|---|---|
| **Call-site swap** (service gọi method repo khác) | `existsByCodeAndDeletedFalse` → `existsByCodeAndInstanceIdAndDeletedFalse` | Mock stub `when(repo.oldMethod(...))` + `verify(repo.oldMethod(...))` trong *ServiceTest |
| **Signature change** (thêm param) | `createStudent(req)` → `createStudent(req, tenantId)` | Mọi caller + test gọi method |
| **`@Deprecated` mới** | mark `oldMethod` deprecated | Mọi caller còn dùng → migrate hoặc `@SuppressWarnings("deprecation")` nếu cố ý giữ |
| **Rename** | `getX` → `getXScoped` | Mọi reference (prod + test + javadoc `{@link}`) |
| **Đổi exception/return** | throw new exception type | Test `assertThatThrownBy` + catch sites |

Rule **KHÔNG** fire khi: thêm method MỚI hoàn toàn (no existing caller), đổi method body thuần (contract không đổi), comment-only.

---

## 3. Required actions (same PR)

### 3.1 Caller sweep (grep, no `| head` truncation per `audit-to-gap-pipeline.md` §2.5)

```bash
# Production + test callers of the changed method
grep -rn "oldMethodName" <module>/src/main/java <module>/src/test/java --include="*.java"
```

Mỗi hit: (a) production caller → migrate; (b) mock stub/verify → swap sang method mới với matchers đúng (`eq(...)`, `any()`); (c) test-of-deprecated-method → migrate sang non-deprecated HOẶC `@SuppressWarnings("deprecation")` nếu cố ý; (d) javadoc `{@link}` / `{@code}` → cập nhật (không phải caller, no warning, nhưng sync để khỏi rot).

### 3.2 Mock-specific (critical — compile-invisible)

Khi service đổi call-site, *ServiceTest mock stubs trở nên stale:
- `when(repo.oldMethod(...)).thenReturn(...)` → service gọi newMethod (unstubbed → Mockito default false) → duplicate-detection / branch test FAIL.
- `verify(repo.oldMethod(...))` → method không được gọi → verify FAIL.
- Nếu newMethod đọc context mới (vd `TenantContext.getCurrentTenant()` throw nếu unset) → test PHẢI set context (`@BeforeEach` + `@AfterEach` clear).

### 3.3 Run tests (not just compile) before push

```bash
cd <module> && ./mvnw test -Dtest='<AffectedServiceTest>,<AffectedRepositoryTest>'
# Expect: Tests run: N, Failures: 0, Errors: 0
```

Per `admin-merge-discipline.md` §3 — verify exact candidate. `compile` PASS ≠ tests PASS.

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Push sau khi chỉ `./mvnw compile` cho code change | `./mvnw test` affected classes — stale mock chỉ fail khi RUN |
| Đổi service call-site, quên mock stub trong *ServiceTest | Sweep + swap `when(...)` + `verify(...)` sang method mới |
| Thêm `@Deprecated` nhưng để callers còn dùng | Migrate mọi caller (prod + test) HOẶC `@SuppressWarnings` nếu cố ý |
| Để javadoc `{@link oldMethod}` rot sau rename | Sweep cả javadoc references |
| "Compile pass → chắc ok" | Compile = type-check; behavioral test = run-check; cần cả hai |
| Trust CI bắt stale mock thay vì local test | CI round-trip ~5-10 phút; local `mvnw test` ~30s — bắt sớm hơn |
| Migrate prod callers, bỏ qua test callers | Test callers (mocks/stubs/verifies) cũng là callers — sweep hết |

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active now)

PR thay đổi method contract:
- [ ] Grep callers (prod + test) đã chạy, full output (no `| head`)?
- [ ] Mọi production caller migrated?
- [ ] Mock stubs/verifies trong *ServiceTest swapped sang method mới (matchers đúng)?
- [ ] Test đọc context mới (TenantContext...) set + clear trong @BeforeEach/@AfterEach?
- [ ] Deprecated-method usages migrated / `@SuppressWarnings` / removed (zero deprecation warning)?
- [ ] `./mvnw test` affected classes chạy local PASS (không chỉ compile)?

### 5.2 Override mechanism

```
git commit -m "...
API_CONTRACT_SWEEP_DEFER: <reason — e.g. 'caller in separate module, follow-up gap GAP-NNN'>"
```

Trailer logged. Pattern frequency >5%/quarter → meta-review.

### 5.3 CI grep detector + memory auto-load (deferred per `incident-to-rule-pipeline.md` §3.1)

- Detector phức tạp: cần AST/diff parse để link method-signature change → caller set; recurrence 0 post-rule; reviewer-checklist + worked self-test §6 đủ cho v1.0.0. Revisit khi recurrence ≥2.

---

## 6. Worked self-test — GAP-799 двойн miss (2026-05-28)

Áp dụng rule retroactively vào GAP-799 fix session (PR #1954):

**Miss 1 — stale mocks:** Đổi `CourseServiceImpl` + `StudentServiceImpl` call-site từ `existsByCodeAndDeletedFalse` → `existsByCodeAndInstanceIdAndDeletedFalse`. Push sau khi chỉ `./mvnw compile` (PASS). CI FAIL: `CourseServiceTest` + `StudentServiceTest` mock stubs còn stub method cũ → service gọi method mới (unstubbed → false) → duplicate-detection test FAIL + `verify(oldMethod)` FAIL + `TenantContext.getCurrentTenant()` throw (test chưa set).

→ Rule §3.2 + §3.3 fire: nếu chạy `./mvnw test` local TRƯỚC push → bắt ngay (CourseServiceTest 18/18 + StudentServiceTest 10/10 sau khi swap mock + set TenantContext). Tiết kiệm 1 CI FAIL round-trip + watcher stop.

**Miss 2 — deprecation leftover:** Thêm `@Deprecated` cho `existsByCodeAndDeletedFalse`. Để 2 test cũ ở `CourseRepositoryTest:110,119` còn gọi → deprecation warnings (user flag 2 lần).

→ Rule §3.1(c) fire: sweep deprecated-method usages → migrate 2 test cũ sang instance-scoped → zero warning.

**Verdict:** Rule fires correctly trên CẢ HAI miss. Counterfactual: sweep callers (prod+test) + `mvnw test` (không chỉ compile) trước push → 0 CI FAIL + 0 deprecation warning leftover. Self-test PASS ✅

---

## 7. Relationship to other rules

- **`cross-flow-bug-class-sweep.md`** — sister rule cùng family "change once → sweep". Axis khác: nó sweep BUG-CLASS signature qua sister flows; rule này sweep CALLERS của 1 method-contract change (prod + test).
- **`admin-merge-discipline.md`** §3 — verify exact merge candidate local (`mvnw verify`); rule này sharpens: code change phải `mvnw test` (không chỉ compile) trước push.
- **`ci-queue-local-runner-threshold.md`** §2 — code PR >20 LOC dùng CI canonical; rule này thêm: vẫn nên `mvnw test` affected classes local cho fast feedback khi đổi method contract.
- **`design-patterns.md`** §3.12 Entity-Migration-Mapper triad drift — cùng tinh thần "change ripples to N artifacts atomically".
- **`audit-to-gap-pipeline.md`** §2.5 hardened state-check — no `| head` truncation khi sweep; rule này inherit.
- **`incident-to-rule-pipeline.md`** v1.1 — rule này = direct output GAP-799 двойн miss qua 5-stage pipeline.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 paired same PR.
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 chuẩn sweep+test → mọi method-contract change subsequent auto-comply).

---

## 8. Log

- **2026-05-28 (v1.0.0):** Rule created in response to user direction 2026-05-28 "cập nhật meta để tránh lỗi" sau GAP-799 fix двойн miss: (1) đổi service call-site nhưng quên swap mock stubs trong *ServiceTest → push sau chỉ `compile` → CI FAIL; (2) thêm `@Deprecated` nhưng để test callers còn dùng → deprecation warnings (user flag 2 lần). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged 2x) → Classify ✓ (no existing rule mandates caller-sweep + run-tests-not-compile cho method-contract change; `cross-flow-bug-class-sweep.md` covers bug-class signature sweep, không cover method-caller sweep; `admin-merge-discipline.md` §3 covers merge-candidate verify chung không specific cho contract-change ripple) → Rule+Enforce ✓ (this file + reviewer-checklist §5.1 + worked self-test §6 trên GAP-799 originating двойн miss + paired same-PR with deprecation cleanup + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example — rule fires correctly trên cả 2 miss + counterfactual 0 CI FAIL + 0 warning) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn → mọi method-contract change subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered class; no constraint loosening; existing changes grandfathered; rule applies prospectively từ this PR forward). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: method-contract change → caller sweep + run tests) + ✅ unique (sister cross-flow-bug-class-sweep covers bug-class axis) + ✅ widely applicable (mọi signature/deprecation/rename) + ✅ body discipline §1 ≤2 "and". Path-scoped `paths: ["**/*.java"]` per `context-budget-mandate.md` §3.1 — load chỉ khi context chạm Java files. CI detector + memory auto-load deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 0 post-rule; reviewer-checklist + self-test sufficient).
