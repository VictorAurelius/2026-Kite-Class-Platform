# GAP-474: GAP-ID Collision — rename Wave 60 GAP-470 K8s runAsNonRoot

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (housekeeping, không block cutover)
**Domain:** Meta / Governance
**Found:** 2026-05-11 (Wave 61 post-closure correction PR)
**Affects:** `documents/04-quality/gaps/GAP-470-k8s-deployments-runasnonroot-security-context.md` + cross-references trong audit reports, wave plans, ROADMAP, CSV

## Problem

Hai gap file dùng cùng numeric ID `GAP-470`:

| File | Topic | Status | Source |
|---|---|---|---|
| `closed/GAP-470-netty-epoll-cve-2026-42577-line-bump.md` | Netty CVE transitive bump | 🟢 DONE 2026-05-11 (sớm hơn cùng ngày) | Post-Wave 57 CodeQL follow-up |
| `GAP-470-k8s-deployments-runasnonroot-security-context.md` | K8s `runAsNonRoot` securityContext | 🟢 DONE 2026-05-11 (Wave 61 Bucket E) | Wave 60 Bucket A OWASP audit follow-up |

Wave 60 Bucket A audit agent filed GAP-470 mới mà không grep existing GAP-470 trong `closed/` folder per `audit-to-gap-pipeline.md` §2 Step 2 (Duplicate Check). Closure narrative Wave 61 + CSV row reference K8s GAP-470 → ambiguity khi reader tra cứu `GAP-470`.

Tương tự ADR-028 incident Wave 58 Bucket C (ADR-027 conflict caught + auto-renamed).

## Root Cause

`audit-to-gap-pipeline.md` §2 Step 2 search command `grep -rl "X" documents/04-quality/gaps/` mặc định KHÔNG include `closed/` subfolder nếu agent dùng `--include` filter hoặc not following hardened protocol §2.5 "Include `documents/04-quality/gaps/`" full output. Wave 60 Bucket A agent dùng audit-only Explore subagent có context budget hẹp, có thể đã head-truncate hoặc miss closed/.

## Proposed Fix

1. **Rename** `GAP-470-k8s-deployments-runasnonroot-security-context.md` → `GAP-474-k8s-deployments-runasnonroot-security-context.md`
2. **Move** sang `closed/` (status DONE đã satisfied per `gap-done-discipline.md` §2 — Wave 61 Bucket E PR #1177 cited)
3. **CSV update** row 309: `GAP-470` → `GAP-474` (ID + filename column)
4. **Cross-reference sweep** (per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync):
   - `documents/03-planning/waves/wave-2026-05-12-60-pre-cutover-p0-hardening.md` §8 Log
   - `documents/03-planning/waves/wave-2026-05-12-61-stop-when-idle-cutover.md` (gaps frontmatter + §3 Bucket E + §8 Log)
   - `documents/03-planning/waves/wave-history.jsonl` Wave 60+61 entries
   - `documents/04-quality/gaps/ROADMAP.md` Wave 60+61 sections
   - `documents/04-quality/audits/security/2026-05-11-pentest-light-owasp.md` finding row
   - Any commit messages / PR titles future (cannot rewrite past commits)

## Acceptance Criteria

- [ ] Renamed file moved to `closed/`
- [ ] CSV row updated (GAP-474)
- [ ] grep `GAP-470` trong `documents/03-planning/`, `documents/04-quality/`, `.claude/` returns ONLY closed netty-epoll references (post-sweep)
- [ ] `bash scripts/check-gap-status-csv.sh` PASS
- [ ] CSV validator + no new duplicate IDs

## Related

- `audit-to-gap-pipeline.md` §2 Step 2 (Duplicate Check) — agent missed
- `audit-to-gap-pipeline.md` §2.5 Hardened state-check protocol — banned `head` truncation
- ADR-028 precedent (Wave 58 Bucket C ADR ID collision self-corrected)
- Closure PR #1178 narrative drift documented this issue

## Log

- **2026-05-11:** Gap filed post Wave 61 Bucket E CSV correction. Identified ID collision khi update CSV row for K8s GAP-470 DONE flip; closed/ folder đã có netty-epoll GAP-470 từ earlier 2026-05-11 work session. Rename + sweep scope ~30 phút work for separate session.
