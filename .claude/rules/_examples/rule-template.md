# Rule Scaffold Template (companion to `rule-change-process.md`)

> Đây là **template tham khảo** để viết rule mới — KHÔNG phải rule thật. Đặt trong `_examples/` để CI rule (frontmatter + index-csv coverage, scan top-level `.claude/rules/*.md`) bỏ qua. Copy phần dưới `=== SCAFFOLD ===` ra file `.claude/rules/<kebab-name>.md`, điền giá trị, rồi:
> 1. Thêm row vào `.claude/rules/rules-index.csv` (per `meta-csv-index-pattern.md` — 100% coverage, CI block nếu thiếu).
> 2. Thêm row "matrix" vào `output-review-mandate.md` §3 (per `rule-change-process.md` §6.5 Enforcement Parity Mandate).
> 3. Ship enforcement (reviewer-checklist + worked self-test) CÙNG PR.

Tham chiếu format chính thức: `rule-change-process.md` §3 (frontmatter) + §5.1 (atomic-unique-bar) + §6.5 (Enforcement Parity Mandate). Mọi rule mới thường là output của `incident-to-rule-pipeline.md` 5-stage (Detect → Classify → Rule+Enforce → Self-Test → Retro Log).

---

```
=== SCAFFOLD (copy từ đây) ===

# <Rule Title> — <one-line essence>

**Priority:** 🟠 MANDATORY        <!-- 🔴 CRITICAL | 🟠 MANDATORY | 🟡 ADVISORY -->
**Version:** 1.0.0                 <!-- MAJOR.MINOR.PATCH -->
**Created:** YYYY-MM-DD
**Last-Reviewed:** YYYY-MM-DD       <!-- ≤ today; CI check-rule-frontmatter.sh enforces -->
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on <originating incident>) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "<class>")
**Applies to:** <scope chính xác — file paths / decision moments>
<!-- Nếu rule chỉ relevant khi chạm file cụ thể, thêm `paths:` frontmatter per context-budget-mandate.md §3.2;
     nếu always-load (cross-cut mọi turn) thì thêm §"Auto-load justification". -->

---

## 1. The Rule

> **<Một câu mệnh lệnh, atomic, ≤2 "and" — đây là điều rule bắt buộc.>**

<1-2 đoạn: vì sao rule tồn tại + incident gốc. Sister rule (nếu có) ở boundary khác.>

---

## 2. Trigger pattern — khi nào rule fire

| Pattern | Ví dụ |
|---|---|
| <khi nào> | <ví dụ cụ thể> |

Rule **KHÔNG** fire khi: <exception scope>.

---

## 3. Required action / artifacts khi rule fires

<Bước cụ thể HOẶC artifact bắt buộc. Nếu cần evidence trong PR body, mô tả section format.>

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| <sai> | <đúng> |

---

## 5. Override mechanism

Genuine exception:

\```
git commit -m "...
<RULE>_OVERRIDE: <lý do + follow-up gap link>"
\```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 6. Worked self-test — <originating incident> (YYYY-MM-DD)

<Apply rule retroactively vào incident gốc → chứng minh rule fires correctly + counterfactual (cost saved). Self-test PASS.>

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)
- [ ] <check 1>
### 7.2 Detector (deferred per `incident-to-rule-pipeline.md` §3.1)
<Honest defer: complexity + recurrence-count + FP-risk + revisit trigger. KHÔNG copy-paste boilerplate "defer ≥7 days".>
### 7.3 Override mechanism — per §5.

---

## 8. Relationship to other rules

- **`rule-change-process.md`** §6.5 — rule + checklist + self-test paired same PR
- **`incident-to-rule-pipeline.md`** — rule này = direct output incident gốc qua 5-stage
- **`output-review-mandate.md`** §3 — paired same-PR matrix row
- **`meta-gap-priority.md`** §3 — META force-multiplier (nếu meta rule)

---

## 9. Log

- **YYYY-MM-DD (v1.0.0):** Rule created. Triggered by <incident>. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ → Classify ✓ → Rule+Enforce ✓ → Self-Test ✓ → Retro Log ✓. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint, no loosening; existing work grandfathered; prospective).

=== SCAFFOLD (đến đây) ===
```
