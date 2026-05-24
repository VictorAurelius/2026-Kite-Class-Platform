---
title: Wave beta-readiness-1 — Security cluster + enrollment capacity greenfield
wave: 1
tag_primary: beta-readiness
tags_secondary: [security, greenfield-enrollment, p0-cluster, phase-1-closure]
counter: 1
date_launch: 2026-05-24
status: draft
audience: dev
gaps:
  - GAP-NEW-XSS-DOMPurify
  - GAP-NEW-ENROLLMENT-CAPACITY
  - GAP-NEW-IDEMPOTENCY-KITECLASS
  - GAP-NEW-AUTHZ-A01-AUDIT
audits:
  - 2026-05-24-outside-in-phase-1-closure-failure-mode-matrix-v2-state-checked.md (A2 + A4 + A3 + A5)
  - 2026-05-24-outside-in-phase-1-closure-persona-walkthrough-v2-state-checked.md (cross-cuts)
---

# Wave beta-readiness-1 — Security cluster + enrollment capacity greenfield

**Mục tiêu:** Ship 4 verified P0 blockers chặn Phase 1 BETA invite. Khắc phục các lỗ hổng security + data-integrity mà V2 audit state-checked verify (per `audit-to-gap-pipeline.md` §2.8).

**Khởi sự:** V2 audit 3 reports shipped 2026-05-24 (PR #1759); roadmap V2 amendment confirmed 8 P0 + 1 PDPL deadline; Wave beta-readiness-1 = first sub-wave execution.

**Thời gian ước tính:** ~10h (1-2 phiên) — 4 bucket scope, mostly cross-module parallel.

**Tag scheme:** per `.claude/rules/wave-tag-numbering-convention.md` §2 — `beta-readiness-1` (first tag-based Phase 1 closure wave). Counter 1, descriptor `security-greenfield`.

---

## 1. Brainstorm (5-10 phút)

**Q1 (đối tượng phục vụ):**
- Phase 1 BETA gate prerequisite — beta tenant invite KHÔNG triển khai mà chưa fix các security P0 + enrollment race
- 3 personas affected: P2 Owner (enrollment + authz), P3 Manager (enrollment ops + authz), Anonymous (XSS attack surface)

**Q2 (giải pháp đã xét và loại):**
- ❌ Defer all 4 bucket sang post-AWS-restore → security risk too high pre-beta-invite
- ❌ Ship as 4 separate single-bucket waves → overhead high, cluster naturally cohesive (all P0 security/data-integrity)
- ❌ Skip Bucket D authz audit → A01 OWASP scope partial = beta user could exploit cross-tenant
- ✅ **4-bucket parallel wave với cross-module disjoint scope** — agents song song không xung đột

**Q3 (rủi ro):**
- Bucket A (XSS) đụng FE shared layout components — verify scope rời rạc với SVG template scope cụ thể
- Bucket B (enrollment capacity) = greenfield schema migration — pre-existing Class table cần check trước
- Bucket C (idempotency narrow) sharable pattern với existing PaymentIdempotencyService — agent đọc Wave 105 Bucket D code làm baseline
- Bucket D (authz audit) read-only audit + add tests; risk thấp nhưng có thể surface bugs khác (drift Wave 108-N expand)

---

## 2. Task Breakdown

| Bucket | Loại | Agent | Phụ thuộc | Thời gian |
|---|---|---|---|---|
| **A** XSS sanitize 9 dangerouslySetInnerHTML + DOMPurify | FE fix | Agent A worktree | Không | ~2h |
| **B** Enrollment capacity model greenfield (schema + maxStudents + check + tests) | BE schema | Agent B worktree | Không | ~3h |
| **C** Idempotency POST narrow (signup + enrollment + beta-request controllers — pattern từ PaymentIdempotencyService) | BE pattern | Agent C worktree | Không (read Wave 105 Bucket D code) | ~3h |
| **D** Per-resource authz A01 OWASP audit + cross-tenant IT tests | BE audit | Agent D worktree | Không (read-only audit + add tests) | ~2h |
| Tổng hợp + ship | Main session | — | All 4 done | ~30 phút |

**Kiểm tra rời rạc:**
- A đụng `kitehub/kitehub-frontend/src/components/branding/wizard/Template*.tsx` + `src/components/seo/JsonLd.tsx` + `src/app/help/**/page.tsx` + `src/app/(public)/blog/[slug]/page.tsx` + `src/app/(public)/beta-status/page.tsx`
- B đụng `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/enrollment/**` + `kiteclass/kiteclass-core/src/main/resources/db/migration/V*__enrollment_capacity.sql`
- C đụng `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/auth/controller/` (signup) + `enrollment/controller/` (overlap với B?) + `beta-request/controller/` + new `*IdempotencyService.java` pattern
- D đụng `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/*/controller/` (audit pass — read-only) + add IT tests trong `src/test/java/`

**Conflict risk B vs C trên enrollment module:** C touches EnrollmentController.java cho idempotency header; B touches Enrollment entity + migration. Conflict tại CONTROLLER level (B adds capacity-check method + C adds idempotency middleware). Mitigation: spawn B trước, merge B, then C reads merged code.

**Revised spawn pattern:** A + D parallel ngay (disjoint); B sequential trước C; C spawn sau B merge.

---

## 3. Scope (per V2 amendment)

**Bậc rủi ro:** HIGH (security + greenfield schema migration; PHẢI ship trước beta invite)
**Mô hình:** Opus 4.7 — Sonnet cho 4 agents (cost-efficient cho execution scope rõ ràng)
**Có đụng xuyên tầng?** PARTIAL — Bucket A FE-only; Bucket B/C/D BE-only. KHÔNG có cross-layer endpoint contract change (Bucket C adds Idempotency-Key header support — existing endpoints, không thay shape). Skip Bucket 0 Foundation per `contract-first-for-cross-layer.md` §2.

| # | Bucket | Phạm vi | V2 audit evidence |
|---|---|---|---|
| 1 | A XSS sanitize | Wrap 9 `dangerouslySetInnerHTML` sites bằng DOMPurify (or `sanitize-html`) — SVG template highest risk; help pages MDX trust source nhưng wrap defensive | FM V2 A2 elevated — 9 sites verified `grep -rln "dangerouslySetInnerHTML" kitehub/kitehub-frontend/src/` |
| 2 | B Enrollment capacity model | Schema migration `V*__enrollment_capacity.sql` adds `class.max_students` + `class.enrolled_count` (or denormalize từ COUNT); `Enrollment` entity link Class; capacity check trong `EnrollmentService.create()` throws `BusinessException.CLASS_FULL`; concurrent IT test 10 simultaneous enrollment requests = 1 success + 9 CLASS_FULL | FM V2 A4 elevated — `grep -rn "@Version\|@Lock\|max_capacity\|maxStudents\|enrolledCount" kiteclass/kiteclass-core/` = 0 hits |
| 3 | C Idempotency POST narrow | Pattern từ `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/payment/PaymentIdempotencyService.java` (Wave 105 Bucket D) — port to 3 controllers: signup (auth module), enrollment (post-Bucket-B), beta-request. Each adds new `*IdempotencyService.java` + DB table migration `V*__idempotency_keys.sql` (per-controller) + `@PostMapping` middleware checks Idempotency-Key header | FM V2 A3 — payment exists, narrow 3 missing |
| 4 | D Per-resource authz A01 audit | Per controller (kiteclass-core all `/api/**` POST/PUT/DELETE) verify `@PreAuthorize` resource-scope (e.g. parent edit own child only, owner edit own tenant only); add cross-tenant cross-user IT tests (`@SpringBootTest` + 2-tenant Postgres + 2-user JWT) | FM V2 A5 — 5 @PreAuthorize hits parent module = partial |

### Bằng chứng kiểm tra trạng thái (per `audit-to-gap-pipeline.md` §2.6)

| Tham chiếu | Lệnh xác minh | Phán quyết |
|---|---|---|
| 9 dangerouslySetInnerHTML sites | `grep -rln "dangerouslySetInnerHTML" kitehub/kitehub-frontend/src/` | ✅ 9 files verified |
| Enrollment module exists | `find kiteclass/.../enrollment -type f` | ✅ 10+ files (entity, repo, controller, dto, mapper, event) |
| Enrollment capacity model absent | `grep -rn "@Version\|@Lock\|max_capacity\|maxStudents\|enrolledCount" kiteclass/.../enrollment/` | ✅ 0 hits (greenfield confirmed) |
| Class entity (for maxStudents extension) | `find kiteclass/.../class -name "Class.java"` | ⚠️ verify-at-spawn-time Agent B |
| PaymentIdempotencyService pattern | `cat .../parent/payment/PaymentIdempotencyService.java` | ✅ Wave 105 Bucket D code reusable |
| Signup controller | `find kiteclass/.../auth -name "*SignupController*" -o -name "*RegisterController*"` | ⚠️ verify-at-spawn-time Agent C |
| Beta-request controller | `find kiteclass/.../beta -name "*Controller*"` | ⚠️ verify-at-spawn-time Agent C |
| DOMPurify dependency | `grep -r "dompurify\|isomorphic-dompurify" kitehub/kitehub-frontend/package.json` | ⚠️ verify-at-spawn-time Agent A (install if absent) |

---

## 4. State-Check Evidence

Xem §3 — table đã verify 9 XSS sites + enrollment module + absent capacity model + PaymentIdempotencyService pattern. ⚠️ verify-at-spawn-time cho Class entity + signup/beta-request controllers + DOMPurify dep.

---

## 5. Verification Gates

| Bucket | Lệnh kiểm thử cục bộ | CI gate |
|---|---|---|
| A XSS | `cd kitehub/kitehub-frontend && pnpm test --run components/branding/wizard components/seo` + manual browser test SVG render escape | frontend-ci |
| B Enrollment capacity | `cd kiteclass/kiteclass-core && ./mvnw test -P strict-warnings -Dtest=EnrollmentServiceTest` + concurrent IT test 10 requests | core-ci |
| C Idempotency | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest=*IdempotencyTest -P strict-warnings` | core-ci |
| D Authz audit | `cd kiteclass/kiteclass-core && ./mvnw test -Dtest=CrossTenantAuthzTest -P strict-warnings` | core-ci |

---

## 6. Agent Spawn Pattern

### 6.1 Cấu hình 4 agent

| Agent | Loại | Worktree | Phạm vi đụng | Mô hình | Spawn order |
|---|---|---|---|---|---|
| A | `general-purpose` | `/tmp/wt-br1-xss-dompurify` | 9 FE files + `package.json` (DOMPurify add) + unit tests | Sonnet | Parallel với D |
| B | `general-purpose` | `/tmp/wt-br1-enrollment-capacity` | `kiteclass-core/.../enrollment/**` + `Class` entity + migration + tests | Sonnet | Sequential FIRST (blocks C) |
| C | `general-purpose` | `/tmp/wt-br1-idempotency-narrow` | 3 controllers + new `*IdempotencyService.java` × 3 + migrations + tests | Sonnet | After B merge |
| D | `general-purpose` | `/tmp/wt-br1-authz-a01-audit` | Read-only audit + add cross-tenant IT tests | Sonnet | Parallel với A |

**Spawn pattern:**
```bash
# Phase 1: A + B + D parallel (3 worktree song song)
git worktree add /tmp/wt-br1-xss-dompurify -b wave/beta-readiness-1-bucket-a-xss origin/main
git worktree add /tmp/wt-br1-enrollment-capacity -b wave/beta-readiness-1-bucket-b-enrollment-capacity origin/main
git worktree add /tmp/wt-br1-authz-a01-audit -b wave/beta-readiness-1-bucket-d-authz-audit origin/main

# Spawn 3 agents A + B + D parallel (single message Agent[] với run_in_background=true)
# Wait for B merge before spawning C

# Phase 2: C sau B merge (1 worktree)
git worktree add /tmp/wt-br1-idempotency-narrow -b wave/beta-readiness-1-bucket-c-idempotency origin/main  # rebase post-B-merge
```

### 6.2 Mỗi agent prompt skeleton

```
Bạn là agent fix Wave beta-readiness-1 Bucket {X} trong worktree /tmp/wt-br1-{slug}.
KHÔNG cd ra ngoài worktree này.

Scope: <scope detail per §3>
V2 audit evidence: <reference file path + line excerpt>
State-check: run grep/find verify per §2.8 BEFORE implement.

Quy trình:
1. cd /tmp/wt-br1-{slug}
2. Read V2 audit `documents/04-quality/audits/persona-review/2026-05-24-...v2-state-checked.md` finding {X}
3. State-check current code (grep + find) — confirm V2 verdict
4. Implement code change + tests (per §3 bucket scope)
5. Run `cd <module> && ./mvnw verify` OR `pnpm test --run` PASS
6. Commit: `feat(wave-beta-readiness-1-bucket-{X}): <summary>` — NO Co-Authored-By
7. Push branch
8. Tạo PR: `gh pr create --base main --title "..." --body "..."` với Test plan + Output Review Checklist + paired evidence
9. Báo cáo (<500 từ): PR URL + scope + test status + state-check evidence cited
```

### 6.3 Banned (per existing rules)

- ❌ `cd` ngoài worktree riêng (per `feedback_worktree_absolute_path_contamination.md`)
- ❌ `--admin` merge mà chưa local verify per `admin-merge-discipline.md`
- ❌ Absolute path `/tmp/wt-br1-*` trong commit message
- ❌ `Co-Authored-By` (per CLAUDE.md)
- ❌ Skip state-check per §2.8 (lessons từ V1 audit false-positive)

---

## 7. Closure Protocol

### 7.1 4 PR Wave beta-readiness-1 + 1 closure PR

| PR | Bucket | Scope |
|---|---|---|
| PR-A | A XSS | 9 dangerouslySetInnerHTML sanitize + DOMPurify wrap + unit test |
| PR-B | B Enrollment capacity | Schema migration + entity + service capacity check + concurrent IT test |
| PR-C | C Idempotency narrow | 3 IdempotencyService + migrations + middleware + IT test |
| PR-D | D Authz audit | Cross-tenant IT test + identified findings filed as follow-up gaps (no fix, only audit) |
| Closure PR | Coordinator | wave plan §8 Log + scope reconciliation table + wave-history.jsonl + ROADMAP |

### 7.2 NEW gap candidates spawned per Bucket execution

Each bucket SHOULD file NEW gap để track:
- Bucket A → GAP-NEW-XSS-DOMPurify (close in same PR via Option B)
- Bucket B → GAP-NEW-ENROLLMENT-CAPACITY (close in same PR via Option B)
- Bucket C → GAP-NEW-IDEMPOTENCY-KITECLASS (close in same PR via Option B)
- Bucket D → GAP-NEW-AUTHZ-A01-AUDIT (open follow-up gaps for findings)

Per `gap-done-discipline.md` §3 Option B — drop "live verify production" AC + Out-of-scope section + flip DONE post-merge.

### 7.3 Tiêu chí kết Wave beta-readiness-1

- [ ] 4 PR merged main (sequential A+D → wait B → C)
- [ ] 4 NEW gap files filed + CSV rows + flipped DONE (Option B reframe)
- [ ] Wave plan §8 Log entry + status: complete + scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3
- [ ] wave-history.jsonl appended với new tag format per `wave-tag-numbering-convention.md` §2.5
- [ ] ROADMAP §🚀 + §🎉 SHIPPED Wave beta-readiness-1 entry
- [ ] Audit suite per `post-wave-audit-mandate.md` §2.1 — Security audit (matrix row trigger from Java code changes) + Frontend security (XSS)

---

## 8. Log

- **2026-05-24 (draft):** Wave beta-readiness-1 plan PR draft. V2 audit verified 4 P0 bucket scope (drop V1 PaymentController bucket — already fixed Wave 105 Bucket E0). State-check per §2.6 confirmed 9 XSS sites + enrollment module no capacity + idempotency pattern reusable từ Wave 105 Bucket D. Spawn sequence revised: A+D parallel ngay, B sequential first (blocks C), C sau B merge. Estimated 1-2 phiên ~10h. Per `wave-tag-numbering-convention.md` §2 first tag-based execution wave.
