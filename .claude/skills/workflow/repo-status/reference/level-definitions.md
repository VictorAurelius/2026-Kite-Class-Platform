# Repo Status Levels — Detailed Definitions

## Overview

5 levels đánh giá sức khỏe remote repo dựa trên **4 nhân tố**:
- **F1:** CI status trên main
- **F2:** Open PRs + stale remote branches
- **F3:** Audit gaps chưa có PR fix tương ứng
- **F4:** GitHub Security — Dependabot + code-scanning + secret-scanning alerts

---

## Level Definitions

### GREEN — Healthy

**Điều kiện (TẤT CẢ phải đúng):**
- F1: CI xanh trên main (tất cả workflows pass)
- F2: 0 open PRs + 0 stale branches
- F3: 0 unfixed P0/P1 gaps từ latest audit
- F4: 0 critical/HIGH CVE, 0 secret alerts, 0 code-scanning warnings, Dependabot **enabled**

**Ý nghĩa:** Repo sẵn sàng cho development bình thường. Không có debt tích lũy.

**Action:** Tiếp tục development theo plan.

---

### YELLOW — Minor Issues

**Điều kiện (BẤT KỲ):**
- F1: CI xanh + minor audit gaps chỉ P2/P3
- F2: 1-2 stale branches HOẶC 1-2 open PRs đang active
- F3: Chỉ có P2/P3 gaps, không có P0/P1
- F4: 1-2 code-scanning warnings (MEDIUM severity) HOẶC 1+ medium Dependabot alerts, Dependabot enabled

**Ý nghĩa:** Repo ổn nhưng có minor housekeeping cần làm.

**Action:** Fix khi convenient, không cần ưu tiên cao.

---

### ORANGE — Needs Attention

**Điều kiện (BẤT KỲ):**
- F1: CI xanh nhưng có P1 audit gaps chưa fix
- F2: >2 stale branches HOẶC >2 open PRs
- F3: Có P1 gaps chưa có PR fix tương ứng
- F4: **Dependabot disabled** (silent-drift risk) HOẶC ≥3 code-scanning warnings

**Ý nghĩa:** Repo hoạt động nhưng đang tích debt. Cần xử lý trước khi thêm features mới.

**Action:** Fix TRƯỚC khi tạo PRs cho features mới. Ưu tiên cleanup. Nếu Dependabot disabled — bật ngay ở repo Settings → Code security and analysis.

---

### RED — Degraded

**Điều kiện (BẤT KỲ):**
- F1: CI đỏ trên main (≤7 ngày)
- F3: Có P0 gaps chưa fix
- F4: ≥1 HIGH CVE (Dependabot `high` severity HOẶC code-scanning `error` severity)

**Ý nghĩa:** Repo trong tình trạng xấu. CI đỏ hoặc HIGH CVE live trên main = exposure.

**Action:** Fix ngay. Security + CI fix = ưu tiên cao nhất. Không tạo PRs mới cho đến khi fix.

---

### BLACK — Broken

**Điều kiện (BẤT KỲ):**
- F1: CI đỏ trên main >7 ngày
- F4: ≥1 **CRITICAL** CVE HOẶC ≥1 **secret scanning alert** (credential actively leaked)
- Main branch không build được

**Ý nghĩa:** Repo hỏng nghiêm trọng hoặc đang có data-loss / credential-leak exposure. Development bị block.

**Action:** Stop tất cả work khác. Secret leak = rotate credentials NGAY + audit log truy cập. CRITICAL CVE = hotfix PR trong 24h. Escalate nếu cần.

---

## Decision Matrix

```
CI xanh? ─── No ──→ Bao lâu? ─── >7 days ──→ BLACK
   │                    │
   │                    └── ≤7 days ──→ RED
   │
   Yes
   │
   ├── Secret alert? ─── Yes ──→ BLACK
   │
   ├── CRITICAL CVE? ─── Yes ──→ BLACK
   │
   ├── HIGH CVE or code-scan error? ─── Yes ──→ RED
   │
   ├── P0 gaps? ─── Yes ──→ RED
   │
   ├── Dependabot disabled? ─── Yes ──→ ORANGE
   │
   ├── ≥3 code-scan warnings? ─── Yes ──→ ORANGE
   │
   ├── P1 gaps? ─── Yes ──→ ORANGE
   │
   ├── >2 stale items? ─── Yes ──→ ORANGE
   │
   ├── 1-2 stale / P2 gaps / any warnings? ─── Yes ──→ YELLOW
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
- Squash-merge workflow: branches "merged" theo PR không show `--merged` — dùng `gh pr list --state merged` cross-check
- Draft PRs tính như open PRs
- `origin/HEAD` không tính

### F3: Audit Gaps

**Checked by:** Parse latest `documents/04-quality/audits/quality/quality-audit-*.md`

**Metrics:**
- P0/P1/P2 items trong "Remaining Gaps" / "Action Items" / "Improvement Roadmap"
- Audit date (freshness — audit >30 ngày = nên re-audit)
- Cross-reference: gap đã có merged PR fix chưa?

**Gotchas:**
- Audit report là snapshot tại thời điểm — gaps có thể đã fix nhưng chưa re-audit
- Script parse bằng grep P0/P1/P2 markers — format thay đổi = cần update script
- UI audit (`ui-audit-issues-*.md`) cũng có gaps — tính chung
- Nếu gaps từ audit cũ (>30 ngày) đã có PRs fix → nên re-audit thay vì tin report cũ

### F4: GitHub Security (new 2026-04-21)

**Checked by:** `gh api` calls to 3 endpoints:
- `repos/{owner}/{repo}/dependabot/alerts?state=open`
- `repos/{owner}/{repo}/code-scanning/alerts?state=open`
- `repos/{owner}/{repo}/secret-scanning/alerts?state=open`

**Metrics:**
- **Dependabot:** severity breakdown (critical/high/medium/low) — `disabled` if HTTP 403
- **Code scanning (CodeQL):** severity breakdown (error/warning/note) — errors = HIGH
- **Secret scanning:** count of open alerts — any alert = BLACK level

**Disabled detection:** Script uses jq type-check (`type=="array"` means enabled, object with `message` field means disabled). Do NOT grep response body — CVE descriptions may contain word "disabled" and cause false positives.

**Gotchas:**
- Dependabot must be enabled at repo Settings → Code security and analysis. Disabled = silent drift — no auto-tracking of new CVEs.
- Secret scanning requires repo to be **public** or GitHub Enterprise tier for private repos.
- Code-scanning alerts only populate if CodeQL / Trivy / similar SARIF upload workflow runs. `gh api` returns `[]` if no scanner configured.
- Severity mapping: Dependabot `high`/`critical` = HIGH; code-scanning `error` = HIGH, `warning` = MEDIUM, `note` = LOW
- Auth: `gh auth token` needs at least `repo` + `security_events` scopes for private repos

---

## Upgrade Path (cách đưa repo về GREEN)

| From | To | Actions cần |
|------|----|-------------|
| BLACK → RED | Rotate leaked credentials + fix CRITICAL CVE (PR bump) hoặc fix CI build trên main |
| RED → ORANGE | Fix CI + fix P0 gaps + bump HIGH-severity deps |
| ORANGE → YELLOW | Fix P1 gaps + cleanup branches >2 + **enable Dependabot** (nếu disabled) |
| YELLOW → GREEN | Fix P2 gaps + cleanup tất cả branches + resolve code-scanning warnings |
