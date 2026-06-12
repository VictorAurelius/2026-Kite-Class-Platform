---
description: "Dùng trước khi bắt đầu PR hoặc domain mới, user nói 'pre-flight', 'check trước khi code', 'chuẩn bị PR', 'domain mới', 'persona impact'. 4-layer check: PR scope / Domain business docs / Project health / Persona impact."
---

# Skill: Pre-flight Check (Multi-layer)

**Version:** 1.1
**Last Updated:** 2026-04-29
**Purpose:** Ngăn gaps tích tụ bằng cách check ở 4 tầng: PR, Domain, Project, Persona

---

## Usage

```
/pre-flight-check pr          # Trước khi tạo PR
/pre-flight-check domain      # Trước khi bắt đầu 1 business domain mới
/pre-flight-check project     # Milestone check (mỗi 10-25 PRs hoặc trước release)
/pre-flight-check persona     # Khi PR thay đổi user-facing business logic
```

---

## Business Logic Documents

**Location:** `documents/01-business/` (xem `documents/01-business/README.md` cho quy tắc đầy đủ)

**Format mỗi doc:** 4 sections, ~100-150 dòng
1. **Rules** — bảng business rules + config key + code location
2. **Flow** — text diagram, max 15 bước
3. **Emails** — trigger → template → timing → variables
4. **Config** — YAML keys chính xác

**Rule:** Doc và code PHẢI cùng PR. Đổi logic → đổi doc trong cùng commit.

---

## Tại sao cần 4 tầng?

```
Bài học: 188 PRs merged → phát hiện 22 business gaps → 39 PRs fix
Root cause: Chỉ check ở tầng PR (micro), bỏ qua domain (meso) và project (macro)
2026-04-29 update: Thêm Layer 4 (persona) sau khi GAP-050 surfacing rằng
business correctness gaps thường disguise as code-level gaps; persona perspective
catches them earlier.

Layer 1 — PR:       "Code này chạy đúng không?"
Layer 2 — Domain:   "Module này khớp với business flow không?"
Layer 3 — Project:  "Tất cả modules kết nối đúng không?"
Layer 4 — Persona:  "Persona nào dùng feature này? Có degrade ai không?"
```

---

## Layer 1: PR Pre-flight (trước MỖI PR)

**Khi nào:** Sau brainstorm, trước khi viết code.
**Thời gian:** 5 phút.
**Trigger:** Tự động khi `/continue` hoặc manual `/pre-flight-check pr`

### Checklist

```markdown
## PR Pre-flight: [PR name]

### State-Check (BẮT BUỘC — reference `.claude/rules/audit-to-gap-pipeline.md` Step 2.5)
- [ ] Grep'd the paths this PR will touch (FE routes/components, BE services/controllers, migrations, infra, docs)
- [ ] Verified the PR is NOT duplicating existing implementation
- [ ] If partial existing implementation found → PR scope narrowed to delta
- [ ] If gap file has `## Current State (verified YYYY-MM-DD)` → re-verified it still matches codebase

### Business Logic
- [ ] Business rules lấy từ config? (KHÔNG hardcode số ngày, số lượng, tỷ lệ)
- [ ] Nếu tạo email trigger → template file ĐÃ TỒN TẠI hoặc tạo trong cùng PR?
- [ ] Nếu tạo @Scheduled → có idempotency check? (không gửi duplicate)
- [ ] Nếu thay đổi status transition → document trong code/commit message?
- [ ] Business doc check: nếu PR thay đổi business logic (service, config, scheduler) → verify `documents/01-business/` có doc tương ứng và đã update
- [ ] Config key check: mọi @ConfigurationProperties/config key trong code → phải có trong business doc Config section

### Code Quality
- [ ] Nếu @RequestBody → có @Valid + typed DTO? (KHÔNG dùng Map<String, Object>)
- [ ] Nếu tạo constant → @ConfigurationProperties hoặc @Value? (KHÔNG static final hardcode)
- [ ] Nếu có domain/URL → configurable? (KHÔNG hardcode .kitehub.me)
- [ ] FUTURE/TODO/placeholder → KHÔNG được merge (implement hoặc tạo tracking issue)

### Design Patterns (reference `.claude/rules/design-patterns.md`)
- [ ] Nếu có ≥2 implementations → Strategy Pattern (interface + impls)
- [ ] Nếu có entity với finite states → State Pattern (KHÔNG switch scattered)
- [ ] Nếu pipeline of steps → Command + Composite
- [ ] Nếu service >15 methods → refactor với Facade Pattern
- [ ] Nếu call external API → Adapter Pattern (domain types, not vendor types)
- [ ] Nếu publish event + DB change → Outbox Pattern (same txn)
- [ ] Nếu external HTTP call → Circuit Breaker + fallback required
- [ ] Pattern choice documented trong javadoc (e.g., `// Strategy Pattern`)
- [ ] No primitive obsession (value objects for colors, status, etc.)
- [ ] No God Service (<500 lines per service class)

### Testing
- [ ] Test cover business rule? (không chỉ happy path)
- [ ] Nếu multi-tenant → test tenant isolation?
- [ ] Nếu scheduler → test idempotency?
```

### Auto-check commands

```bash
# Run trước khi commit
grep -rn "FUTURE\|TODO\|FIXME\|HACK" $(git diff --cached --name-only -- '*.java') 2>/dev/null
grep -rn "Map<String.*>.*@RequestBody" $(git diff --cached --name-only -- '*.java') 2>/dev/null
grep -rn "static final.*= [0-9]" $(git diff --cached --name-only -- '*.java') 2>/dev/null | grep -v "serialVersionUID\|VERSION\|LOG"
```

### Kết quả
- **TẤT CẢ pass** → Tiến hành code
- **Bất kỳ fail** → Fix trước khi code, hoặc document lý do exception

---

## Layer 2: Domain Pre-flight (trước khi bắt đầu MODULE MỚI)

**Khi nào:** Trước khi code bất kỳ module/feature domain nào (email, trial, payment, branding...).
**Thời gian:** 30-60 phút.
**Trigger:** Khi plan có module mới, hoặc manual `/pre-flight-check domain`

### Checklist

```markdown
## Domain Pre-flight: [Domain name]

### 1. User Journey Document
- [ ] Viết TOÀN BỘ flow end-to-end (đầu → cuối)
      Ví dụ Trial: register → verify → start trial → warning emails → expire → suspend → data cleanup
- [ ] Mỗi bước có: user thấy gì, system làm gì, email gì gửi, data gì thay đổi
- [ ] Review với leader/stakeholder TRƯỚC KHI code
- [ ] Lưu: documents/01-business/[domain]-user-journey.md

### 2. Business Rules Inventory
- [ ] List TẤT CẢ business rules cho domain này
- [ ] Mỗi rule: giá trị, configurable (Y/N), ai quyết định
- [ ] Ví dụ:
      | Rule | Value | Configurable | Decided by |
      |------|-------|-------------|------------|
      | Trial duration | 14 days | Yes | Product |
      | Max trial per owner | 1 | Yes | Product |
      | Grace period | 3 days | Yes | Product |
- [ ] Lưu: documents/01-business/[domain]-business-rules.md

### 3. Email Map (nếu domain liên quan email)
- [ ] List MỌI email trigger trong domain
- [ ] Mỗi email: trigger event, template name, timing, variables
- [ ] Template files PHẢI tạo TRƯỚC hoặc CÙNG LÚC với code trigger
- [ ] Lưu: documents/01-business/[domain]-email-map.md

### 4. Status Transitions (nếu domain có entity có status)
- [ ] Vẽ state machine: STATUS_A → EVENT → STATUS_B
- [ ] Mỗi transition: ai trigger, validation gì, side effects gì
- [ ] Edge cases: invalid transitions, concurrent transitions
- [ ] Lưu: documents/01-business/[domain]-state-machine.md

### 5. Data Lifecycle (nếu domain quản lý user data)
- [ ] Data tạo khi nào? Ai tạo?
- [ ] Data backup khi nào? Ở đâu?
- [ ] Data xóa khi nào? Cảnh báo trước bao lâu?
- [ ] Retention period: configurable theo tier?
- [ ] Lưu: documents/01-business/[domain]-data-lifecycle.md

### 6. Config Inventory
- [ ] List TẤT CẢ config keys cho domain
- [ ] Mỗi key: prefix, type, default, description
- [ ] Tạo @ConfigurationProperties class TRƯỚC KHI code service
- [ ] Lưu trong: application.yml với comment

### 7. API Contract
- [ ] List endpoints: method, path, request DTO, response DTO
- [ ] Mỗi DTO: typed class (KHÔNG Map), validation annotations
- [ ] Error responses: status codes + message format
```

### Kết quả
- **Tất cả documents tạo xong** → Tiến hành tạo PRs
- **Thiếu document** → DỪNG, hoàn thành document trước
- **Cần confirm từ leader** → Ghi rõ câu hỏi, chờ confirm, KHÔNG giả định

---

## Layer 3: Project Pre-flight (MILESTONE CHECK)

**Khi nào:**
- Mỗi 10-25 PRs merged
- Trước release/deploy
- Khi chuyển phase (Phase 1 → Phase 2)
- Manual `/pre-flight-check project`

**Thời gian:** 1-2 giờ.

### Checklist

```markdown
## Project Pre-flight: Milestone [X]

### 1. Cross-domain Consistency
- [ ] Tất cả email templates tồn tại cho mọi trigger trong code?
      ```bash
      # Templates vs triggers
      ls kitehub/kitehub-email/src/main/resources/templates/emails/
      grep -rn "template.*=" kitehub/kitehub-*/src/main --include="*.java" | grep -v test
      ```
- [ ] Tất cả business constants từ config (KHÔNG hardcode)?
      ```bash
      grep -rn "static final.*= [0-9]" kitehub/kitehub-*/src/main --include="*.java"
      ```
- [ ] Tất cả status transitions documented?
- [ ] Frontend và backend dùng CÙNG config values? (không mỗi nơi hardcode khác nhau)

### 2. Business Logic Integrity
- [ ] Chạy `/business-gap-check [target]` → score bao nhiêu %?
- [ ] Tất cả 🔴 P0 gaps đã fix?
- [ ] User journey từ register → churn không có "hố" (missing step)?

### 3. Technical Quality
- [ ] Chạy `/quality-audit [target]` → score bao nhiêu?
- [ ] CI green cho TẤT CẢ workflows?
- [ ] 0 stale branches?
- [ ] 0 TODO/FUTURE/placeholder trong production code?
      ```bash
      grep -rn "TODO\|FUTURE\|FIXME\|HACK\|placeholder" */*/src/main --include="*.java" | wc -l
      ```

### 4. Documentation Sync
- [ ] Plan documents có completion tracking (✅/⬜)?
- [ ] Architecture docs phản ánh code hiện tại?
- [ ] QUICK_START.md chạy được từ đầu đến cuối?

### 5. Security
- [ ] Không có default secrets ("changeme", "test-secret")?
      ```bash
      grep -rn "changeme\|test-secret\|sk-mock\|password.*=.*['\"]" */*/src/main --include="*.java" --include="*.yml"
      ```
- [ ] Tất cả endpoints có auth hoặc documented là public?
- [ ] Input validation trên mọi @RequestBody?
- [ ] Rate limiting trên mọi public endpoint?

### 6. Deployment Readiness (trước release)
- [ ] Docker compose up → tất cả services healthy?
- [ ] E2E tests pass?
- [ ] Monitoring/alerting configured?
- [ ] Backup strategy documented + tested?
- [ ] Rollback plan documented?
```

### Kết quả
- **quality-audit ≥ 90 AND business-gap ≥ 80%** → Ready for next phase
- **Dưới threshold** → Tạo fix plan TRƯỚC KHI tiếp tục features
- **Cần confirm** → List câu hỏi cho leader

---

## Layer 4: Persona Impact Check (khi PR thay đổi user-facing business logic)

**Khi nào fires:** PR adds/changes/removes user-facing business logic — endpoint behavior, validation rule, status transition, pricing, notification trigger, UI flow, data visibility, permissions. KHÔNG fire cho refactor / test / infra-only PRs.

**Thời gian:** 10-15 phút.

**Trigger:** Manual `/pre-flight-check persona`, OR auto-trigger if PR diff touches:
- `*Controller.java`, `*Service.java` (business logic)
- `rules.md`, `use-cases.md`, `application.yml` (business contract)
- `kiteclass-frontend/src/app/**`, `kitehub-frontend/src/app/**` (user-facing routes)
- Status enum changes, permission/role changes

**Reference:** `.claude/skills/quality/persona-based-business-review.md` (full methodology) + `documents/00-brd/personas-catalog.md` (canonical persona list).

### Steps

```markdown
## Persona Pre-flight: [PR name]

### Step 1: Identify affected personas
- [ ] Read `documents/00-brd/personas-catalog.md` — note Tier 1 personas list
      (Solo Teacher, Tutoring Center, Medium Center, K-12 School are current Tier 1)
- [ ] For each Tier 1 persona, ask: "Does this PR's change affect this persona's workflow?"
      — Answer Y/N + 1-line justification per persona
- [ ] List affected personas as `affects: [P1, P5]` in PR description

### Step 2: Coverage non-degradation check
- [ ] Locate latest persona review report under `documents/00-brd/persona-reviews/`
      (sorted by date — read most recent for each affected persona)
- [ ] If NO review report exists yet: note "first review pending GAP-152" in PR body
      and skip to Step 3 (cannot measure delta with no baseline)
- [ ] If review report EXISTS: locate persona's Coverage Analysis table; for each
      use case the PR touches, verify post-PR state ≥ pre-PR state. Flag any regression.
- [ ] If degradation found → BLOCKER. Either narrow PR scope OR file follow-up gap
      with persona link before merging.

### Step 3: New gap surfacing
- [ ] Does the PR introduce a NEW persona impact not in any review report?
      (e.g., adding parent-portal endpoint when no review covered parent persona)
- [ ] If yes → file gap via `audit-to-gap-pipeline.md` Step 3 referencing
      affected personas; trigger off-cycle review per cadence rules in
      `persona-based-business-review.md` §Quarterly Review Cadence

### Step 4: PR description marker
- [ ] Add to PR body: `Persona impact: [P1 yes / P5 partial / P3 no]` + 1-line summary
- [ ] If degradation flagged + accepted (with follow-up gap), include `PERSONA_OVERRIDE: GAP-XXX`
      trailer in commit message (analogous to AUDIT_OVERRIDE pattern)
```

### Auto-detect commands

```bash
# 1. Detect user-facing changes in PR diff
git diff main...HEAD --name-only | grep -E "Controller\.java$|rules\.md$|use-cases\.md$|/app/" | head -20

# 2. List Tier 1 personas
grep -A1 "^## P[12345]" documents/00-brd/personas-catalog.md | head -30

# 3. Find latest review reports per persona
ls -lt documents/00-brd/persona-reviews/*.md 2>/dev/null | head -5

# 4. Check if any persona review references this PR's domain
grep -l "$(echo '<domain-keyword>')" documents/00-brd/persona-reviews/*.md 2>/dev/null
```

### Kết quả

- **All Tier 1 personas non-degraded (or N/A baseline pending)** → Tiến hành merge
- **Any degradation flagged** → STOP merge. Either narrow scope or `PERSONA_OVERRIDE` trailer + follow-up gap
- **No review report exists yet for affected persona** → Note "first review pending GAP-152", proceed but flag for next quarterly cycle

### Why this layer

Business correctness gaps (per `meta-gap-priority.md` business-logic tier) often look like ordinary feature gaps from a code-only review angle. A bulk-import endpoint that works perfectly for 50 students passes Layer 1-3 but fails Layer 4 when K-12 School persona scale (500 students) surfaces the bottleneck. Layer 4 catches this earlier than Layer 3's milestone audit.

GAP-050 motivated this layer; GAP-152 ships first review reports. Until GAP-152 lands, Layer 4 runs in best-effort mode (no baseline to compare against), but the persona identification step still exercises the perspective.

---

## Quy tắc Escalation

```
Layer 1 fail → Fix trong PR hiện tại (developer)
Layer 2 fail → DỪNG development, hoàn thành documents (developer + lead)
Layer 3 fail → DỪNG release, tạo fix plan (team)
Layer 4 fail → STOP merge: narrow scope OR file follow-up gap + PERSONA_OVERRIDE trailer
```

---

## Integration với Superpowers

```
BEFORE (cũ — chỉ check tầng PR):
  Brainstorm → Breakdown → TDD → Implement → Review

AFTER (mới — check 4 tầng):
  [Layer 2: Domain Pre-flight]     ← MỖI module mới
    ↓
  [Layer 3: Project Pre-flight]    ← MỖI milestone
    ↓
  Brainstorm → [Layer 1: PR Pre-flight] → Breakdown → TDD → Implement
    ↓
  [Layer 4: Persona Pre-flight]    ← khi PR thay đổi user-facing logic
    ↓
  Review → merge
    ↓
  [Layer 3 lại nếu đủ PRs]
```

---

## Anti-patterns (KHÔNG được lặp lại)

| Anti-pattern | Hậu quả | Ví dụ thực tế |
|-------------|---------|--------------|
| Code trước, design sau | 39 PRs tồn đọng | Trial logic code trước khi chốt business rules |
| Hardcode business rules | Deploy lại mỗi lần đổi | `plusDays(14)`, `MAX_FREE = 2` |
| Tạo email trigger nhưng không tạo template | Runtime crash/silent fail | 4 templates missing |
| TODO/FUTURE trong production | Technical debt vĩnh viễn | `DatabaseBackupScheduler: FUTURE implement` |
| Quality audit quá muộn | Phát hiện gaps khi đã build xong | Audit ở PR #188 thay vì PR #50 |
| CI green = done | Bỏ sót business logic sai | Score 91/100 nhưng gap 45% |
| Không tách Planning PR vs Implementation PR | Requirements thay đổi giữa chừng | Mix docs + code + config trong 1 PR |
| Agent PR chỉ sửa files được chỉ định | Miss test configs, mock setup | @PostConstruct crash @SpringBootTest vì thiếu config |
| Merge song song không check conflict trước | InstanceService conflict | 2 PRs cùng sửa 1 file → resolve thủ công |
| Merge xong không update plans | Plans outdated, gaps report sai | 5 PRs done nhưng plans vẫn hiện ⬜ TODO |
| CI pass = quality OK | Integration issues ẩn | Individual PR pass nhưng main có thể fail |
| Skip Layer 4 vì "code change nhỏ" | Persona-blocking regression escapes review | Bulk-import endpoint pass Layer 1-3 nhưng K-12 persona không scale |

---

## Gotchas

- **Layer 4 best-effort mode until GAP-152 ships** — first persona review reports land via GAP-152; until then, Layer 4 cannot measure delta. Run Step 1 (identify affected personas) anyway; Step 2 returns "baseline pending" not a failure
- **`affects: [P1, P5]` annotation in PR body is the audit trail** — without it, post-hoc audits cannot tell which PRs touched which persona. Reviewers should reject Layer-4-applicable PRs missing this line
- **Layer 4 ≠ user testing** — skill is desk-checking against the catalog + reports, not running real user trials. Real user testing remains a separate cycle (off-cycle review trigger)
- **Catalog drift breaks Layer 4** — if `personas-catalog.md` Tier 1 list goes stale (persona retired, new one added), Layer 4 produces wrong gates. Quarterly cadence keeps catalog fresh; Layer 4 trusts current catalog state

---

## Log

- **2026-04-29** (v1.1): Added Layer 4 (Persona impact check) — fires when PR changes user-facing business logic. Reads `documents/00-brd/personas-catalog.md` Tier 1 list, locates latest `documents/00-brd/persona-reviews/*.md` per affected persona, blocks merge if degradation detected. Best-effort mode until GAP-152 ships first reports. Closes GAP-050 framework AC #5 ("pre-flight-check project layer integrates persona review step"). Reviewer: @nguyenvankiet (solo-dev — paired with `persona-based-business-review.md` §Quarterly Review Cadence + `quality-audit/SKILL.md` Cat 11 in same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate).
- **2026-03-23** (v1.0): Skill created — 3 layers (PR / Domain / Project).
