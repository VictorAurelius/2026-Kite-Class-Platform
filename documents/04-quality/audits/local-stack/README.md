# Local Stack Audits

**Rule:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md)

Per-session audit artifact cho local Docker stack — investigation môi trường local self-test (Docker daemon, compose stack, env templates, port conflicts, WSL2 resources), cold-start attempts, service health verification. Subdir scope mirror `aws-verification/` sister pattern theo `docs-subfolder-maturity.md` §2 Volume + Sister-pattern criteria (planned ≥5 files trong 30 ngày: investigation + fix verify + re-cold-start + service health + e2e).

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `YYYY-MM-DD-<topic>.md` | Per-session investigation/verification report | One per non-trivial local-stack session |

---

## File Placement Rules

- ✅ **Belongs here:** Local Docker stack investigation, cold-start attempts, compose service health audits, WSL2 resource baselines, port conflict reports, env template completeness checks, Flyway baseline reports trên local Postgres
- ❌ **Does NOT belong here:**
  - AWS production verification (`aws-verification/`)
  - Application-level smoke tests trên deployed environment (`ops-readiness/`)
  - Cloudflare DNS verification (`cloudflare-verification/`)
  - Quality / security / performance audit reports (separate subdirs)
- Naming: `YYYY-MM-DD-<topic-kebab-case>.md` — date prefix ISO + topic mô tả (`local-self-test-investigation`, `docker-wsl-integration-fix-verify`, `cold-start-attempt-N`)

---

## Required artifact sections

Mỗi audit artifact PHẢI có (per `pre-mutation-state-check.md` §3 + `agent-aws-access.md` §5.1 pattern adapted cho local scope):

- **Frontmatter:** `title` + `status: complete|partial|in-progress` + `created: YYYY-MM-DD` + `phase` + related gaps
- **Scope:** mục tiêu + bối cảnh phiên investigation
- **Commands run:** liệt kê tất cả lệnh đã chạy, mỗi lệnh kèm mục đích
- **Findings:** kết quả empirical (log lines, file paths, error messages, port numbers — KHÔNG speculate)
- **Root causes (top N):** xếp hạng theo evidence + impact + effort to fix
- **Recommendations:** specific commands / file edits / next-session actions
- **Pending follow-ups:** items defer hoặc gap mới cần file
- **References:** GAP IDs, related rules, prior audit artifacts

---

## Archive Policy

Move sang `documents/07-archived/local-stack-{year}/` khi:
- Audit > 90 ngày tuổi VÀ no recent reference (per `docs-archival-cadence.md` Table §2)
- Local stack environment đã được re-baselined sau migration lớn (WSL distro replacement, Docker Desktop major upgrade)

---

## Key Documents

- [2026-05-21 Local self-test investigation](2026-05-21-local-self-test-investigation.md) — Initial investigation per GAP-691 Phase 0A; identifies Docker Desktop WSL integration as P0 blocker
