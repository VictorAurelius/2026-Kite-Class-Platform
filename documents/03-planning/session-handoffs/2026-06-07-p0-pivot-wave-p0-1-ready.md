# Session handoff — 2026-06-07 (P0-over-G2 pivot; wave-p0-1 plan merged, ready to execute)

**Scope:** Sau khi P3 G3 production-parity COMPLETE (handoff trước `2026-06-07-p3-g3-complete-g2-next.md`), user đổi hướng: **"P0 gap statistics should be prioritized over G2"** ([[project-p0-priority-over-g2]]). Session này pivot sang drive-down P0 count. NEXT = execute wave-p0-1 (re-spawn agents serial).

## Shipped this session (second half)

| PR | Scope | Status |
|----|-------|--------|
| #2237 | Close GAP-1050 (InstanceController residual IDOR) + GAP-814 (X-Tenant-Id anti-spoof) — both G3 runtime-verified via :9000 | ✅ merged |
| #2238 | wave-p0-1 plan — Phase 1 BETA P0 local cluster (3 disjoint Opus buckets) | ✅ merged |

**Phase 1 BETA P0: 31 → 29** (closed 1050 + 814). Canonical count: `awk -F',' 'NR>1 && $5=="P0" && $7=="phase-1-beta" && $4!="DONE" && $4!="WONTFIX"' documents/04-quality/gaps/gap-status.csv | wc -l`.

## ⚠️ NEXT SESSION — execute wave-p0-1 (plan merged, NOT yet executed)

Plan: `documents/03-planning/waves/wave-p0-1-beta-local-cluster.md`. 3 disjoint buckets, all Phase 1 BETA P0:
- **Bucket A** GAP-882 — invoice `status` enum↔CHECK drift (kiteclass-core, sole Flyway writer)
- **Bucket B** GAP-946 — saga fail-loud on DB provisioning error (kitehub-subscription `DatabaseProvisioningService` + `InstanceService`)
- **Bucket C** GAP-948 — wire existing `EmailServiceClient.sendTenantReadyEmail` (line ~534) to outbox after saga DEPLOYED (kitehub-subscription)

**Execution (per plan §6):** spawn 3 Opus worktree agents. **First 3-parallel attempt 2026-06-07 hit server-side rate-limit** (all 3 died ~60s, 0 commits, worktrees auto-cleaned). → **Re-spawn SERIAL (1-at-a-time) OR small-batch** to avoid rate-limit. Then: octopus-merge → rebuild kiteclass-core + kitehub-subscription → CI (`Test Core Service`) → G3 walk (invoice SENT/REFUNDED persist via :9000 + saga fail→FAILED + tenant-ready email in MailHog) → flip GAP-882/946/948 DONE + git mv closed/ + CSV sync. Expected P0: 29 → 26.

**G3 walk recipe ready:** `.claude/g3-walk-scratch/mint.py` (HS512 JWT mint) + [[project-g3-walk-recipe]] (gateway :9000 + fixtures + drift pre-check).

## P0 backlog 3-bucket map (per [[project-p0-priority-over-g2]])
- 🟢 **Local-verifiable now**: GAP-882/946/948 (wave-p0-1) · GAP-975/976/610 (near-done verify-walk) · GAP-885 (RLS → wave-p0-2)
- 🔴 **AWS-gated** (needs `bash scripts/aws/start-stack.sh`): GAP-793/502/608/533/567/566/572/117/756/648 + GAP-952 CloudWatch live-apply
- 🟡 **OPEN net-new**: GAP-950/951/286/297/877/942

## Stack state
Local prod-parity stack UP + 4 backend services fresh from main (gateway/subscription/kiteclass-core/branding rebuilt earlier this session). AWS STOPPED. Course id=1 (tenant aaaabbbb) fixture status fixed `active`→`PUBLISHED` mid-session.

## Language reminder
User flagged mid-session: respond in **tiếng Việt** (narrative) + English identifiers per CLAUDE.md §Communication Language + `dev-readable-doc-language.md`. Commit messages English (git convention).
