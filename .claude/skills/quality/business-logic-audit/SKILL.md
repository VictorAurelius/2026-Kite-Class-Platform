---
name: business-logic-audit
description: "Dùng khi user nói 'business audit', 'logic check', 'kiểm tra business logic', 'code đúng rules chưa', hoặc trước GA release. Verify code implement đúng rules.md + use-cases.md. 5 categories /100."
user-invocable: true
---

# /business-logic-audit — Verify Code ↔ Business Rules

Score /100. Walk through every domain in `documents/01-business/`, verify code implements rules correctly.

## Process

### 1. Collect Domains

```bash
ls documents/01-business/kiteclass/ documents/01-business/kitehub/ | grep -v README
```

### 2. Per-Domain: 5 Checks

For EACH domain folder, read `rules.md`, `use-cases.md`, `api-contract.md`, then verify in code:

| # | Category (20pts) | How |
|---|-----------------|-----|
| 1 | **Rule Coverage** | Each BR-xxx → grep codebase for implementation. Missing = -4/rule |
| 2 | **Config Accuracy** | Config keys in rules.md → match `application.yml` values |
| 3 | **Edge Case Tests** | Each UC error path → has *Test.java. Missing = -4/path |
| 4 | **Cross-Domain Consistency** | No contradicting rules between domains |
| 5 | **Stakeholder Alignment** | Rules reflect VN education market + law. Flag for human review |

Scoring details: `reference/scoring-guide.md`

### 3. Output

Save to `documents/04-quality/audits/business/business-logic-audit-[date].md`

### 4. Scripts

```bash
# Existing — checks 3-layer structure exists
scripts/verify-business-docs.sh

# Manual — verify each BR-xxx has code path
# Grep for config keys in application.yml
```

## Context Management

Audit này có thể tốn 30-50K tokens nếu không kiểm soát. Tuân thủ:

1. **Output limiting** — LUÔN pipe grep results qua `| head -N`:
   - BR-xxx grep: `| head -30` (chỉ cần biết có/không, không cần xem hết)
   - Config key grep: `| head -20`
   - Test file count: dùng `wc -l` thay vì list full
2. **Per-domain staging** — Nếu >5 domains, score 2 domains đầy đủ rồi apply pattern cho còn lại. Chỉ individually score domains có cấu trúc ĐẶC BIỆT.
3. **Subagent delegation** — Nếu >8 domains hoặc codebase >500 Java files:
   - Agent 1: KiteClass domains
   - Agent 2: KiteHub domains
   - Parent: aggregate scores
4. **Skip known-good** — Domains không thay đổi từ audit trước → carry forward score, chỉ verify version match

## Gotchas

- Config keys are in `application.yml` AND `application-test.yml` — check both
- Some BR-xxx implemented in gateway (rate-limit rules) not core — search all modules
- Wave 4 added 6 new domains — don't miss `security-foundation/`, `content-moderation/`, etc.
- Category 5 (Stakeholder) always requires human review — Claude flags, human decides
- Grep output cho large codebase có thể 1000+ lines — LUÔN giới hạn

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category with examples
