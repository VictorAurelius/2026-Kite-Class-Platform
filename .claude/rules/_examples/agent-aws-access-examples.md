---
parent_rule: agent-aws-access.md
purpose: deferred-load §Self-test + §Worked example for context budget compliance
---

# agent-aws-access — Examples / Self-test

Companion to `.claude/rules/agent-aws-access.md`. Body moved here per Wave 76 Bucket E streamline (context-budget-mandate.md §3.1 path-scope already in place via rule frontmatter; this file deferred-loaded only when reviewer reads it).

## Self-test (worked example — 2026-05-08 session)

Apply rule to user-flagged commands from Phase 2.3 retro session:

| Command | Tier | Verdict |
|---|---|---|
| `curl -sI https://kitehub.vercel.app/` | Tier 1 (network probe) | ✅ allowed |
| `aws ec2 describe-instances --query ...` | Tier 1 (`describe-`) | ✅ allowed |
| `aws rds describe-db-instances --query ...` | Tier 1 (`describe-`) | ✅ allowed |
| `aws sts get-caller-identity --profile default` | Tier 1 (allowed `get-*`) | ✅ allowed |
| `aws cloudtrail describe-trails` | Tier 1 (`describe-`) | ✅ allowed |
| `aws cloudtrail get-trail-status` | Tier 2 (always-confirm first time) | ⚠️ confirm |

Verdict: 5/6 commands ✅ allowed at Tier 1; 1 was Tier 2 borderline (no harm, status check). **No banned commands run.** ✓ rule fires correctly on the original session.

Logging gap: report was conversation-only, NOT saved to repo. ❌ rule §5 violated. Remediation: same-PR artifact `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md` saves the verification.

→ Self-test PASS for command tiering, FAIL→FIX for logging requirement (artifact ships same PR).
