# GAP-438: Agent AWS access workflow + verification log artifacts

**Status:** 🔵 OPEN — Phase 1+3 shipping this PR; Phase 2+4 follow-up
**Priority:** 🟠 P1 (governance gap; no incident yet but pattern drift risk)
**Domain:** DevOps / Governance
**Found:** 2026-05-08 (user-flagged in Phase 2.3 retro session)
**Affects:** Every Claude session that interacts with AWS — currently no rule constrains command scope, no required logging.

## Problem

User-flagged 2026-05-08 sau Phase 2.3 apply: *"lệnh check có vẻ là lệnh tự do, cần bổ sung workflow cho agent aws theo chuẩn đã đề cập chưa? báo cáo chi tiết như này có lưu logs tại repo không?"*

Hai gaps:

### Gap 1 — No rule constrains agent AWS command scope

Phase 2.3 verification session, agent ran ad-hoc:
- `curl https://kitehub.vercel.app/`
- `aws ec2 describe-instances ...`
- `aws rds describe-db-instances ...`

These là READ-ONLY, an toàn — nhưng KHÔNG có rule nào:
- List allowed read-only commands
- Ban mutation commands (potential for accident)
- Require pre-flight check trước mỗi AWS API call

Existing rules cover related but không specific:
- `release-deploy-standard.md` §9 — agent role per phase (HIGH-level, không command-list)
- `agent-action-bias.md` — agent does work itself (does NOT cover safety boundary)
- `mcp-first-with-fallback.md` — tool-flavor selection (does NOT cover read-only AWS)

### Gap 2 — Verification reports not saved to repo

Detailed AWS resource inventory + endpoint check tôi vừa report user — only in conversation, KHÔNG saved as artifact:
- Future Claude session không thấy được unless re-derive
- User audit/review không có evidence trail
- `documents/04-quality/audits/` có 6 subfolders cho UI/security/perf/ops/api/business-logic NHƯNG không có `aws-verification/` hoặc `deploy-smoke-tests/`

## Root Cause

Combination of:
1. Project mới gặp Phase 2.3 first-time (Stream A pivot tới AWS Singapore actual apply 2026-05-08)
2. Existing rules viết khi agent chỉ touch local code + GitHub, chưa tính đến AWS API
3. Audit folder structure pre-dates AWS deployment

## Proposed Fix — 4 phases

### Phase 1 (this PR) — File rule `agent-aws-access.md`

`.claude/rules/agent-aws-access.md`:
- §2 Allowed read-only commands (whitelist by prefix: `describe-`, `list-`, `get-` excluding `get-secret-value`)
- §3 Banned mutation commands (any `create-`, `delete-`, `put-`, `update-`, `terminate-`, `modify-`, `apply`)
- §4 Always-confirm commands (anything writing to S3/RDS/EC2)
- §5 Logging requirement (output → `documents/04-quality/audits/aws-verification/YYYY-MM-DD-<topic>.md`)
- §6 Override mechanism + trailer
- §7 Self-test + worked example
- §8 Relationship to other rules (release-deploy-standard.md §9 + agent-action-bias.md)

### Phase 3 (this PR) — Save current session as first audit artifact

`documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md`:
- Inventory ~94 resources
- Endpoint accessibility check (Vercel ✅ ALB 502 EC2 timeout)
- Commands run (curl + aws CLI)
- Findings (backend not deployed, ECR empty, etc.)

→ Templates future verification artifacts.

### Phase 2 (follow-up PR, ~30min)

`.claude/skills/devops/aws-smoke-test/SKILL.md` + `scripts/smoke-aws-phase-N.sh`:
- Reusable bash script collecting resource state
- Output formatted markdown report → save to audit folder
- Trigger: `bash scripts/smoke-aws-phase-N.sh --phase=2.3 --save`

### Phase 4 (follow-up, ~5min)

Memory entry `feedback_agent_aws_readonly_logging.md`:
- Auto-load each session
- Reminder: read-only AWS allowed; log artifact required
- Cross-link rule + skill

## Acceptance Criteria

### Phase 1 (this PR)
- [ ] `.claude/rules/agent-aws-access.md` v1.0.0 với 8 sections + Log
- [ ] Frontmatter per `rule-change-process.md` §3
- [ ] Cross-link from `release-deploy-standard.md` §9 + `agent-action-bias.md`

### Phase 3 (this PR)
- [ ] `documents/04-quality/audits/aws-verification/` folder created
- [ ] First audit artifact saved (current session inventory + check report)
- [ ] Audit folder README explaining format

### Phase 2 (follow-up)
- [ ] Skill scaffold
- [ ] Working `smoke-aws-phase-N.sh` for Phase 2.3 / 2.4 / 4

### Phase 4 (follow-up)
- [ ] Memory entry saved + indexed

## Related

- `release-deploy-standard.md` §9 (agent role matrix)
- `agent-action-bias.md` (agent does work)
- `mcp-first-with-fallback.md` (tool flavor)
- `output-review-mandate.md` §3 (will add row "AWS verification reports")
- This session: 5 memory entries already saved covering Phase 2.3 lessons; this gap is META-coverage governance for future AWS sessions

## Log

- **2026-05-08:** GAP filed in response to user-flagged retro after Phase 2.3 apply. Phase 1+3 shipping this PR; Phase 2+4 deferred follow-up.
