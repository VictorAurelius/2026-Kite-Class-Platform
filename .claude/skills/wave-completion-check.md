# Skill: Wave Execution & Completion Check

**Version:** 2.0
**Last Updated:** 2026-03-23
**Purpose:** Full wave lifecycle: pre-check → launch → merge → verify → update

---

## Usage

```
/wave-completion-check pre      # TRƯỚC khi launch agents
/wave-completion-check [number] # SAU khi merge tất cả PRs
```

---

## Wave Branch Strategy

**Pattern: Integration Branch** — agents merge vào wave branch, không trực tiếp main.

```
main (protected, luôn sạch)
  └── wave/2 (integration branch)
        ├── feature/saas-5-email-log      ← Agent 1 PR → wave/2
        ├── feature/saas-6-seo            ← Agent 2 PR → wave/2
        ├── feature/kc-1-5-tests-todos    ← Agent 3 PR → wave/2
        └── feature/saas-3-retention      ← Agent 4 PR → wave/2

Sau khi tất cả agents merge + wave check pass:
  wave/2 → PR → main (1 PR duy nhất, đã verify)
```

### Quy trình chi tiết

```
1. PRE-WAVE CHECK
   git checkout main && git pull
   git checkout -b wave/X
   git push origin wave/X

2. LAUNCH AGENTS
   Mỗi agent tạo branch từ wave/X (không từ main)
   Agent PRs target: wave/X (không phải main)

3. MERGE VÀO WAVE BRANCH
   Merge từng agent PR vào wave/X
   Fix conflicts trên wave/X (an toàn)
   CI chạy trên wave/X

4. WAVE COMPLETION CHECK
   Chạy đầy đủ 6 levels trên wave/X
   Fix mọi issues trên wave/X

5. MERGE WAVE → MAIN
   Tạo PR: wave/X → main
   Squash merge → 1 commit sạch trên main
   Main luôn sạch, CI luôn green

6. CLEANUP
   Delete wave/X branch
   Delete agent feature branches
```

### Lợi ích

| Aspect | Trước (direct main) | Sau (wave branch) |
|--------|---------------------|-------------------|
| Main stability | Có thể broken giữa merge | Luôn green |
| Fix conflicts | Fix trên main (nguy hiểm) | Fix trên wave branch (an toàn) |
| Rollback | Revert từng PR phức tạp | Delete wave branch |
| Review | Review từng PR lẻ | Review cả wave 1 PR |
| CI history | Nhiều red/green trên main | Main chỉ green |

---

## Pre-wave Check (TRƯỚC khi launch agents)

```bash
# 1. Main clean?
git status
# Expected: clean working tree

# 2. CI green?
gh run list --branch main --limit 3 --json workflowName,conclusion \
  --jq '.[] | "\(.workflowName): \(.conclusion)"'
# Expected: all success

# 3. Create wave branch
git checkout main && git pull
git checkout -b wave/X
git push origin wave/X

# 4. No stale branches?
git branch -r | grep -v "main\|HEAD\|wave/" | wc -l
# Expected: 0
```

### Conflict Prediction Matrix

TRƯỚC launch, liệt kê shared files:

```markdown
| Agent | Files sẽ sửa | Shared with |
|-------|-------------|-------------|
| 1 | InstanceService, TrialConfig | Agent 2 (InstanceService) |
| 2 | InstanceService, TenantResolver | Agent 1 (InstanceService) |
| 3 | email templates only | None |
| 4 | documents/ only | None |

SHARED FILES: InstanceService.java → merge Agent 1 TRƯỚC Agent 2
```

**Decision rules:**
- 0 shared files → safe parallel
- 1-2 shared files → parallel OK, plan merge order
- 3+ shared files → **gộp agents** hoặc sequential

### Agent Prompt Checklist (thêm vào cuối MỌI agent prompt)

```
## Agent Rules
- Tạo branch từ wave/X (KHÔNG từ main): git checkout wave/X && git checkout -b feature/xxx
- PR target: wave/X (KHÔNG phải main)
- KHÔNG merge, chỉ push và tạo PR hướng về wave/X

## Agent Completion Checklist (PHẢI verify trước push)
- [ ] grep -rn "ModifiedClassName" src/test/ → update ALL referencing tests
- [ ] application.yml AND application-test.yml có config mới
- [ ] @SpringBootTest tests load config? (check application.yml in test/resources/)
- [ ] Mock setup dùng lenient() nếu không phải mọi test cần mock
- [ ] List shared files với agents khác: [...]
```

### Rollback Plan

```
NẾU wave-completion-check Level 1 FAIL (CI broken):
  1. Xác định PR gây lỗi: gh run view --log-failed
  2. git revert [merge-commit-hash]
  3. Push revert → CI green lại
  4. Fix trên branch → re-merge

NẾU Level 3 FAIL (business logic sai):
  1. Tạo hotfix: git checkout -b hotfix/wave-X-fix
  2. Fix logic
  3. Push + PR → merge
```

---

## Tại sao cần?

```
Vấn đề: 4 agents tạo 4 PRs song song
  PR-A: Sửa InstanceService (thêm reserved subdomain)
  PR-B: Sửa InstanceService (đổi trial limit logic)
  PR-C: Sửa email templates
  PR-D: Move documents

CI check TỪNG PR riêng → PASS
Merge tất cả vào main → CÓ THỂ:
  1. Conflict logic (A+B cùng sửa 1 method)
  2. Config mâu thuẫn (A set value X, B expect value Y)
  3. Template mismatch (C tạo template nhưng code từ B gọi sai tên)
  4. Cross-reference broken (D move files, A reference path cũ)
```

---

## Checklist (chạy trên main sau merge)

### Level 1: CI Green trên main

```bash
# PHẢI chạy đầu tiên
bash scripts/check-ci.sh
# Hoặc nếu script không available:
gh run list --branch main --limit 5 --json workflowName,conclusion \
  --jq '.[] | "\(.workflowName): \(.conclusion)"'
```

**Pass criteria:** TẤT CẢ workflows SUCCESS trên commit mới nhất của main.
**Fail action:** Fix CI trước khi tiếp tục Wave tiếp theo.

### Level 2: Integration Consistency

```bash
# 1. Email templates match code triggers
echo "=== Email Templates ==="
ls kitehub/kitehub-email/src/main/resources/templates/emails/
echo "=== Code triggers ==="
grep -rn "templateName.*=\|template.*\"" kitehub/kitehub-*/src/main --include="*.java" | grep -v test
# Manual verify: mỗi template trong code CÓ file tương ứng

# 2. Config keys consistent
echo "=== Config in application.yml ==="
grep -A2 "kitehub:" kitehub/kitehub-subscription/src/main/resources/application.yml
echo "=== Config usage in code ==="
grep -rn "@Value.*kitehub\.\|ConfigurationProperties.*kitehub" kitehub/kitehub-*/src/main --include="*.java"
# Manual verify: mọi @Value key CÓ trong application.yml

# 3. No hardcoded business constants
echo "=== Hardcoded constants ==="
grep -rn "static final.*= [0-9]" kitehub/kitehub-*/src/main --include="*.java" \
  | grep -v "serialVersionUID\|VERSION\|LOG\|QUEUE\|EXCHANGE\|ROUTING\|DLQ\|RESERVED"
# Expected: 0 results (trừ RESERVED_SUBDOMAINS)

# 4. No merge conflict markers
echo "=== Conflict markers ==="
grep -rn "<<<<<<\|======\|>>>>>>" kitehub/ kiteclass/ --include="*.java" --include="*.yml" --include="*.tsx" --include="*.ts"
# Expected: 0 results

# 5. No broken imports
echo "=== Broken imports check ==="
grep -rn "import.*RESERVED\|import.*TrialConfig\|import.*SubscriptionConfig" kitehub/kitehub-*/src/main --include="*.java" | head -10
# Manual verify: imports resolve correctly

# 6. Reserved subdomain check live
echo "=== Reserved check in code ==="
grep -rn "RESERVED_SUBDOMAINS\|validateSubdomainNotReserved" kitehub/kitehub-*/src/main --include="*.java"
# Verify: gọi trong TẤT CẢ create instance methods
```

### Level 3: Business Logic Verification

```bash
# 1. Trial config complete
echo "=== Trial Config ==="
grep -A10 "class TrialConfig" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/config/TrialConfig.java
# Verify: durationDays, maxPerOwner, warningDays, midpointDay

# 2. Trial limit logic
echo "=== Trial limit ==="
grep -B2 -A5 "existsByOwnerIdAndTrialStartedAt\|Moi tai khoan" kitehub/kitehub-*/src/main --include="*.java"
# Verify: check ở TẤT CẢ create methods (createTrialInstance, createPendingInstance, registerInstance)

# 3. Config injected (not hardcoded)
echo "=== Injection check ==="
grep -n "trialConfig\.\|subscriptionConfig\.\|dataRetentionConfig\." kitehub/kitehub-*/src/main --include="*.java"
# Verify: config được inject và sử dụng

# 4. BASE_DOMAIN configurable
echo "=== Base domain ==="
grep -n "baseDomain\|BASE_DOMAIN\|kiteclass\.com" kitehub/kitehub-gateway/src/main --include="*.java" -r
# Verify: không còn hardcode, chỉ có @Value injection

# 5. Secure defaults
echo "=== Insecure defaults ==="
grep -rn "changeme\|sk-mock.*key\|password.*=.*test" kiteclass/kiteclass-core/src/main --include="*.java" --include="*.yml"
# Expected: 0 results in production code
```

### Level 4: Regression Check

```bash
# 1. Business gap re-check (chỉ categories liên quan đến wave)
# Chạy manually: verify gaps đã fix thực sự

# 2. Count remaining gaps
grep -rn "TODO\|FIXME\|FUTURE\|HACK" kitehub/kitehub-*/src/main --include="*.java" | wc -l
echo "--- frontend ---"
grep -rn "TODO\|FIXME" kitehub/kitehub-frontend/src --include="*.ts" --include="*.tsx" | wc -l
# Compare với trước wave

# 3. Test count didn't decrease
echo "=== Test files ==="
find kitehub -name "*Test.java" -o -name "*IT.java" | wc -l
# Compare với trước wave (phải >= )
```

### Level 5: Documentation Sync

```bash
# 1. Plans updated
echo "=== SaaS Plan status ==="
grep "✅\|⬜\|🔄" documents/03-planning/kitehub-saas-implementation-plan.md | tail -20
# Verify: completed PRs marked ✅

# 2. Business docs reflect changes
echo "=== Business docs ==="
ls documents/01-business/kitehub/
# Verify: business docs match new config values
```

#### Business Doc Verification
- Mỗi PR trong wave có thay đổi business logic → check business doc đi kèm
- Nếu business doc thiếu → flag as BLOCKER (không merge wave vào main)
- Cross-reference: đếm domains có code vs domains có doc trong 01-business/

```bash
# 3. Business doc coverage check
echo "=== Services with business logic ==="
ls -d kitehub/kitehub-*/src/main/java/com/kitehub/*/service/ 2>/dev/null | sed 's|.*kitehub-||;s|/src.*||' | sort -u
echo "=== Business docs ==="
ls documents/01-business/kitehub/ 2>/dev/null || echo "NO BUSINESS DOCS FOUND"
# Verify: mỗi service có doc tương ứng

# 4. Config keys in business docs vs code
echo "=== Config keys in code ==="
grep -rn "@Value.*kitehub\.\|@ConfigurationProperties" kitehub/kitehub-*/src/main --include="*.java" | grep -v test
echo "=== Config in business docs ==="
grep -rn "kitehub\." documents/01-business/ 2>/dev/null || echo "NO CONFIG IN BUSINESS DOCS"
# Verify: mọi config key trong code có trong business doc
```

---

## Output Report

```markdown
# Wave [X] Completion Check

**Date:** [date]
**PRs merged:** #xxx, #xxx, #xxx, #xxx
**Main commit:** [hash]

## Results

| Level | Check | Status | Detail |
|-------|-------|--------|--------|
| 1 | CI Green | ✅/❌ | All workflows pass |
| 2.1 | Email template match | ✅/❌ | X templates, X triggers |
| 2.2 | Config consistency | ✅/❌ | All @Value keys in yml |
| 2.3 | No hardcoded constants | ✅/❌ | X found |
| 2.4 | No conflict markers | ✅/❌ | 0 found |
| 3.1 | Trial config complete | ✅/❌ | 4 fields |
| 3.2 | Trial limit in all methods | ✅/❌ | 3 methods |
| 3.3 | Config injected | ✅/❌ | X injections |
| 3.4 | BASE_DOMAIN configurable | ✅/❌ | No hardcode |
| 3.5 | Secure defaults | ✅/❌ | 0 insecure |
| 4.1 | TODO count | ℹ️ | Before: X, After: X |
| 4.2 | Test count | ℹ️ | Before: X, After: X |
| 5.1 | Plans updated | ✅/❌ | X PRs marked done |

## Business Gaps Fixed
[List gaps that this wave addressed]

## Issues Found
[Any new issues discovered during check]

## Wave Metrics

| Metric | Value |
|--------|-------|
| Agents launched | X |
| CI pass first try | X/X (XX%) |
| Conflicts resolved | X files |
| Fix iterations | X rounds |
| Agent time | Xm |
| Fix time | Xm |
| Review + doc time | Xm |
| **Total wave time** | **Xm** |

## Verdict
✅ Wave X complete — ready for Wave X+1
❌ Wave X has issues — fix before proceeding
```

---

## Level 6: Post-wave Documentation Sync (BẮT BUỘC)

Wave 1 cho thấy: **nếu không enforce, plans/reports sẽ outdated ngay sau merge.**

```bash
# 1. SaaS plan completion status
grep "⬜\|✅" documents/03-planning/kitehub-saas-implementation-plan.md | tail -20
# Verify: PRs vừa merge đã mark ✅ + PR number

# 2. Gap reports updated
grep "⬜\|✅" documents/04-quality/business-gap-check-*-kitehub.md | grep -c "✅"
grep "⬜\|✅" documents/04-quality/business-gap-check-*-kiteclass.md | grep -c "✅"
# Verify: gaps fixed trong wave đã mark ✅

# 3. Parallel strategy updated
grep "Wave.*COMPLETED\|Wave.*TODO" documents/03-planning/parallel-execution-strategy.md
# Verify: current wave marked COMPLETED

# 4. Refactor plan updated (nếu có refactor PR)
grep "✅\|⬜" documents/03-planning/docs-and-skills-refactor-plan.md
# Verify: completed items marked

# 5. Minor issues noted
# Verify: wave check report có "Issues Found" section với items tracked
```

**PHẢI commit doc updates TRƯỚC khi bắt đầu wave tiếp.**

---

## Rules

- PHẢI chạy SAU mỗi wave, TRƯỚC khi bắt đầu wave tiếp
- Level 1 (CI) fail → STOP, fix trước
- Level 2-3 có fail → fix trước khi Wave tiếp
- Level 4-5 là informational — track trends
- **Level 6 (doc sync) là BẮT BUỘC** — commit updates trước Wave tiếp
- Lưu report: `documents/04-quality/wave-[X]-completion-check.md`

---

## Lessons Learned

### Wave 1 (2026-03-23)
- **Agent miss test configs:** `@PostConstruct` trong PR #195 crash `@SpringBootTest` vì `application.yml` (không phải `application-test.yml`) không có secret. Fix: thêm config vào BASE test yml.
- **Agent miss UnnecessaryStubbingException:** PR #197 mock `trialConfig` trong `setUp()` nhưng không phải mọi test dùng → Mockito strict mode fail. Fix: `lenient()`.
- **Merge conflict predictable:** PR #195 + #197 cùng sửa `InstanceService.java` → conflict. Nên merge PR ít sửa shared files trước.
- **Doc update dễ quên:** Sau merge 4 PRs, plans/gap reports vẫn hiện ⬜ TODO → phải enforce Level 6 check.
- **CI pass ≠ quality OK:** PR CI pass riêng lẻ, nhưng integration issues chỉ phát hiện khi merge vào main.
- **Direct to main nguy hiểm:** Main broken giữa merge 4 PRs. **Giải pháp: Wave branch pattern** (v2.0) — agents merge vào `wave/X`, verify xong mới merge `wave/X → main`.

### Wave 4 (2026-03-24)
- **⚠️ VIOLATION: Tự merge wave → main.** User nói "merge" → agent merge cả wave/4 → main mà không hỏi. Đúng ra: "merge" = merge PRs vào wave, KHÔNG phải wave → main. **Rule: LUÔN hỏi rõ "merge wave/X → main?" trước khi merge vào main.**
- **FE test false positive:** Agent dùng `getByText` cho text xuất hiện nhiều lần (category label + card label) → TS2345 + TestingLibraryElementError. Fix: `getAllByText`.
- **check-ci.sh script counts old failures:** Script exit khi thấy 0 in_progress nhưng có old failures → false negative. Cần cải thiện script chỉ check latest run.
