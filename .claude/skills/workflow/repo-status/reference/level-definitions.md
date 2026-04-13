# Repo Status Levels — Detailed Definitions

## Overview

5 levels đánh giá sức khỏe remote repo dựa trên 3 nhân tố:
- **F1:** CI status trên main
- **F2:** Open PRs + stale remote branches
- **F3:** Audit gaps chưa có PR fix tương ứng

---

## Level Definitions

### GREEN — Healthy

**Điều kiện (TẤT CẢ phải đúng):**
- F1: CI xanh trên main (tất cả workflows pass)
- F2: 0 open PRs + 0 stale branches
- F3: 0 unfixed P0/P1 gaps từ latest audit

**Ý nghĩa:** Repo sẵn sàng cho development bình thường. Không có debt tích lũy.

**Action:** Tiếp tục development theo plan.

---

### YELLOW — Minor Issues

**Điều kiện (BẤT KỲ):**
- F1: CI xanh + minor audit gaps chỉ P2/P3
- F2: 1-2 stale branches HOẶC 1-2 open PRs đang active
- F3: Chỉ có P2/P3 gaps, không có P0/P1

**Ý nghĩa:** Repo ổn nhưng có minor housekeeping cần làm.

**Action:** Fix khi convenient, không cần ưu tiên cao.

---

### ORANGE — Needs Attention

**Điều kiện (BẤT KỲ):**
- F1: CI xanh nhưng có P1 audit gaps chưa fix
- F2: >2 stale branches HOẶC >2 open PRs
- F3: Có P1 gaps chưa có PR fix tương ứng

**Ý nghĩa:** Repo hoạt động nhưng đang tích debt. Cần xử lý trước khi thêm features mới.

**Action:** Fix TRƯỚC khi tạo PRs cho features mới. Ưu tiên cleanup.

---

### RED — Degraded

**Điều kiện (BẤT KỲ):**
- F1: CI đỏ trên main (≤7 ngày)
- F3: Có P0 gaps chưa fix

**Ý nghĩa:** Repo trong tình trạng xấu. CI đỏ = code merge vào main có thể gây thêm vấn đề.

**Action:** Fix ngay. CI fix = ưu tiên cao nhất. Không tạo PRs mới cho đến khi CI xanh.

---

### BLACK — Broken

**Điều kiện (BẤT KỲ):**
- F1: CI đỏ trên main >7 ngày
- Main branch không build được

**Ý nghĩa:** Repo hỏng nghiêm trọng. Development bị block.

**Action:** Stop tất cả work khác. Toàn bộ effort vào fix CI/build. Escalate nếu cần.

---

## Decision Matrix

```
CI xanh? ─── No ──→ Bao lâu? ─── >7 days ──→ BLACK
   │                    │
   │                    └── ≤7 days ──→ RED
   │
   Yes
   │
   ├── P0 gaps? ─── Yes ──→ RED
   │
   ├── P1 gaps? ─── Yes ──→ ORANGE
   │
   ├── >2 stale items? ─── Yes ──→ ORANGE
   │
   ├── 1-2 stale items OR P2 gaps? ─── Yes ──→ YELLOW
   │
   └── All clean ──→ GREEN
```

---

## Factor Details

### F1: CI Status

**Checked by:** `scripts/check-ci.sh --status` hoặc `gh run list --branch main`

**Metrics:**
- Workflows passing/failing (deduplicated by name, latest run only)
- Days since last green (tính từ ngày CI pass cuối)
- Root cause of failure (từ `gh run view --log-failed`)

**Gotchas:**
- Chỉ check workflows trên branch `main`, không phải feature branches
- `in_progress` runs không tính là fail — đợi hoàn thành
- Docker Build pass nhưng Frontend CI fail = vẫn tính CI failing

### F2: PRs & Branches

**Checked by:** `gh pr list --state open` + `git branch -r --no-merged origin/main`

**Metrics:**
- Số open PRs (active, draft, stale)
- Số remote branches chưa merge vào main
- Age của mỗi item (commit date)

**Gotchas:**
- Branch có thể là WIP hợp lệ — check commit date trước khi recommend delete
- Draft PRs tính như open PRs
- `origin/HEAD` không tính

### F3: Audit Gaps

**Checked by:** Parse latest `documents/04-quality/quality-audit-*.md`

**Metrics:**
- P0/P1/P2 items trong "Remaining Gaps" / "Action Items" / "Improvement Roadmap"
- Audit date (freshness — audit >30 ngày = nên re-audit)
- Cross-reference: gap đã có merged PR fix chưa?

**Gotchas:**
- Audit report là snapshot tại thời điểm — gaps có thể đã fix nhưng chưa re-audit
- Script parse bằng grep P0/P1/P2 markers — format thay đổi = cần update script
- UI audit (`ui-audit-issues-*.md`) cũng có gaps — tính chung
- Nếu gaps từ audit cũ (>30 ngày) đã có PRs fix → nên re-audit thay vì tin report cũ

---

## Upgrade Path (cách đưa repo về GREEN)

| From | To | Actions cần |
|------|----|------------|
| BLACK → RED | Fix CI build trên main | 
| RED → ORANGE | Fix CI + fix P0 gaps |
| ORANGE → YELLOW | Fix P1 gaps + cleanup branches >2 |
| YELLOW → GREEN | Fix P2 gaps + cleanup tất cả branches |
