# Skill: Wave Completion Check

**Version:** 1.0
**Last Updated:** 2026-03-23
**Purpose:** Verify chất lượng sau mỗi wave merge — phát hiện vấn đề integration giữa các PRs

---

## Usage

```
/wave-completion-check [wave-number]
```

**Khi nào:** SAU KHI merge tất cả PRs trong 1 wave vào main.

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
