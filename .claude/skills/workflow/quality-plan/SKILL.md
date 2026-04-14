---
name: quality-plan
description: Auto-generate PR plan + wave strategy sau mỗi quality audit
user-invocable: true
argument-hint: "[kitehub|kiteclass|all]"
---

# /quality-plan — Generate PR Plan từ Audit Gaps

## Khi nào dùng?

Sau mỗi `/quality-audit` hoặc `/business-gap-check`, chạy skill này để tự động tạo plan.

```
/quality-audit kitehub    → Score 96/100, 4 gaps
/quality-plan kitehub     → Tạo plan v(next) với PRs cho 4 gaps
```

---

## Step 1: Đọc audit mới nhất

```bash
# Tìm audit mới nhất
ls -t documents/04-quality/audits/quality/quality-audit-*-$TARGET.md | head -1
```

Đọc file → extract "Remaining Gaps" section.

---

## Step 2: Đọc business gap check mới nhất (nếu có)

```bash
ls -t documents/04-quality/audits/business/business-gap-check-*-$TARGET.md | head -1
```

Đọc → extract "❌ Failed Checks".

---

## Step 3: Gộp gaps → PRs

**Rules:**
- Mỗi gap → 1 PR (hoặc gộp nếu cùng service/file)
- Mỗi PR có: score impact, estimate, scope, tasks
- Priority: score impact cao + effort thấp → P0
- PRs cần Docker → group riêng

**Template cho mỗi PR:**
```markdown
### PR-V{version}-{target}-{number}: {title}

**Score:** {category} +{points} → {new}/10
**Estimate:** {time}
**Scope:**
- [ ] Task 1
- [ ] Task 2
```

---

## Step 4: Tạo Wave Strategy

Phân tích dependency + shared files → chia agents:

```markdown
## Wave {next}

| Agent | PRs | Files | Docker |
|-------|-----|-------|--------|
| 1 | ... | ... | ✅/❌ |
| 2 | ... | ... | ✅/❌ |
```

**Rules:**
- Docker PRs → agent riêng
- 0 shared files giữa agents
- Max 4 agents per wave

---

## Step 5: Tạo plan file

Lưu tại: `documents/03-planning/quality-plan-v{next}-{target}.md`

Nếu plan cho cả 2 → `quality-plan-v{next}-final-push.md`

---

## Step 6: Update /continue skill references

Đọc `.claude/skills/workflow/continue/SKILL.md` → thêm plan mới vào danh sách.

---

## Step 7: Output summary

```markdown
## Quality Plan Generated

| Target | Current | Target | PRs | Waves |
|--------|---------|--------|-----|-------|
| KiteHub | X/100 | 100 | N | 1 |
| KiteClass | X/100 | 100 | N | 1 |

Plan: documents/03-planning/quality-plan-v{X}.md
Next: `/continue` hoặc launch Wave {X}
```

---

## Auto-trigger

Skill này PHẢI được gợi ý chạy sau:
1. `/quality-audit` phát hiện score < 100
2. `/business-gap-check` phát hiện gaps > 0
3. `/wave-completion-check` phát hiện issues mới

**KHÔNG tự chạy** — luôn hỏi user: "Score {X}/100, {N} gaps. Tạo plan mới?"
