# Skill: Pre-flight Check (Multi-layer)

**Version:** 1.0
**Last Updated:** 2026-03-23
**Purpose:** Ngăn gaps tích tụ bằng cách check ở 3 tầng: PR, Domain, Project

---

## Usage

```
/pre-flight-check pr          # Trước khi tạo PR
/pre-flight-check domain      # Trước khi bắt đầu 1 business domain mới
/pre-flight-check project     # Milestone check (mỗi 10-25 PRs hoặc trước release)
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

## Tại sao cần 3 tầng?

```
Bài học: 188 PRs merged → phát hiện 22 business gaps → 39 PRs fix
Root cause: Chỉ check ở tầng PR (micro), bỏ qua domain (meso) và project (macro)

Layer 1 — PR:      "Code này chạy đúng không?"
Layer 2 — Domain:  "Module này khớp với business flow không?"
Layer 3 — Project: "Tất cả modules kết nối đúng không?"
```

---

## Layer 1: PR Pre-flight (trước MỖI PR)

**Khi nào:** Sau brainstorm, trước khi viết code.
**Thời gian:** 5 phút.
**Trigger:** Tự động khi `/continue` hoặc manual `/pre-flight-check pr`

### Checklist

```markdown
## PR Pre-flight: [PR name]

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
- [ ] Nếu có domain/URL → configurable? (KHÔNG hardcode .kiteclass.com)
- [ ] FUTURE/TODO/placeholder → KHÔNG được merge (implement hoặc tạo tracking issue)

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

## Quy tắc Escalation

```
Layer 1 fail → Fix trong PR hiện tại (developer)
Layer 2 fail → DỪNG development, hoàn thành documents (developer + lead)
Layer 3 fail → DỪNG release, tạo fix plan (team)
```

---

## Integration với Superpowers

```
BEFORE (cũ — chỉ check tầng PR):
  Brainstorm → Breakdown → TDD → Implement → Review

AFTER (mới — check 3 tầng):
  [Layer 2: Domain Pre-flight]     ← MỖI module mới
    ↓
  [Layer 3: Project Pre-flight]    ← MỖI milestone
    ↓
  Brainstorm → [Layer 1: PR Pre-flight] → Breakdown → TDD → Implement → Review
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
