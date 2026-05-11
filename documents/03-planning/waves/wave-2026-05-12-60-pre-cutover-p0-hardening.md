---
title: Wave 60 — Pre-cutover P0 hardening
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [60]
gaps: [GAP-430, GAP-406, GAP-114, GAP-137, GAP-033, GAP-049, GAP-050, GAP-102, GAP-112, GAP-116-pii-scrubbing-logs]
---

# Wave 60 — Pre-cutover P0 hardening

**Goal:** Đóng các P0 hardening trước khi AWS Activate D+14 (~2026-05-23) mở cutover. Clear pre-cutover backlog để Wave 61 chỉ tập trung cutover execution.
**Trigger:** Post-pending-legal scope cut (PR #1165 merge 2026-05-11); pre-cutover cleanup queue đã empty trừ các P0 hardening items.
**Estimated wall-clock:** ~3-5 ngày agent work (4 buckets song song, longest ~1-2 ngày).

---

## 1. Brainstorm (5-10 min)

**Q1 (persona alignment):** P1 Solo Teacher + P2 Small Center cần hardening cơ bản trước khi onboard. Beta cohort được brief "v1 pending counsel review" cho legal items; tech hardening (security headers, error rate, logging) phải pass cơ bản.

**Q2 (trade-offs):** Pen-test self-audit thay vì outsource (chấp nhận coverage thấp hơn để tiết kiệm budget); FLIP-DONE bulk có rủi ro nếu skip per-gap verify nên dành 1 bucket cho việc đó.

---

## 2. Task Breakdown

| Bucket | Effort | Risk |
|---|---|---|
| A. Pen-test OWASP Top 10 self-audit | 1-2 ngày | LOW (manual checklist) |
| B. GAP-137 bulk import frontend UI | 1-2 ngày | MEDIUM (FE new screen) |
| C. GAP-430 backup metric + GAP-114 logging DONE flip | 30 phút + per-gap verify | LOW |
| D. FLIP-DONE candidates verify (6 gaps) | 1 ngày | LOW (read-only audit + flip) |

---

## 3. Scope

**Stake tier:** MEDIUM → model: Opus medium per bucket
**Cross-layer?:** NO (Bucket B FE-only consumes existing BE; không cross-layer wave)

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** Pen-test OWASP | GAP-406 | 🔴 P0 | `documents/04-quality/audits/security/2026-XX-pentest-light.md` + remediation across `kitehub/`, `kiteclass/` | parallel |
| 2 | **B** Bulk import FE | GAP-137 | 🔴 P0 | `kiteclass/kiteclass-frontend/src/app/(dashboard)/admin/bulk-import/` | parallel |
| 3 | **C** Backup metric + logging | GAP-430, GAP-114 | 🔴 P0 | `kitehub/docker/prometheus/alert-rules.yml` + verify of `kitehub/kitehub-shared/logback-spring.xml` | parallel |
| 4 | **D** FLIP-DONE verify | GAP-033, 049, 050, 102, 112, 116-pii | 🟠 P1 meta | `documents/04-quality/gaps/closed/` (move files) + CSV update | parallel |

### Bucket A — Pen-test OWASP Top 10 self-audit

- Files: `documents/04-quality/audits/security/2026-XX-pentest-light.md` (new audit report)
- Scope: OWASP Top 10 (2021) categories — Broken Access Control, Cryptographic Failures, Injection, Insecure Design, Security Misconfiguration, Vulnerable Components, Auth Failures, Software Integrity, Logging/Monitoring, SSRF
- Remediation per finding → file follow-up gaps if non-trivial; ship trivial fixes inline
- Acceptance: audit report committed; security headers (HSTS, CSP, X-Frame-Options) verified on production-equivalent staging
- (Lưu ý: pen-test này = self-audit checklist, KHÔNG phải third-party engagement)

### Bucket B — GAP-137 Bulk import frontend UI

- Files: `kiteclass/kiteclass-frontend/src/app/(dashboard)/admin/bulk-import/page.tsx` + MSW handler + test
- Tests: component test + integration test với MSW
- Acceptance: upload CSV → preview → confirm → submit flow functional với BE endpoint (Wave 1 BE đã có)
- Cross-layer note: BE đã shipped Wave 1 GAP-051, FE consume existing endpoint — KHÔNG cần Bucket 0 Foundation

### Bucket C — GAP-430 backup metric + GAP-114 logging DONE flip

- Files: `kitehub/docker/prometheus/alert-rules.yml` (fix metric name từ `backup_job_failures_total` → `kitehub_backup_failures_total` hoặc đúng tên emit-end)
- Verify `BackupJobFailure` alert fires bằng synthetic Prometheus test
- GAP-114 DONE flip: verify 3-service live trace (kitehub-subscription + kitehub-branding + kiteclass-core) emit MDC fields → flip status → 🟢 DONE per `gap-done-discipline.md` §2
- Acceptance: alert mock-fire green; GAP-114 closed with verification artifact pointer

### Bucket D — FLIP-DONE verify 6 candidates

Per `gap-done-discipline.md` §2 audit each gap:
- GAP-033 manual rollback (CSV completion 95%)
- GAP-049 business-logic-review rule (95%)
- GAP-050 persona-based business review (95%)
- GAP-102 guides completion ADR (95%)
- GAP-112 distributed tracing infrastructure (90%)
- GAP-116-pii-scrubbing-logs (90%)

Per-gap: verify mọi AC item checked OR follow-up gap reference; if pass → move file to `closed/` + CSV status flip → DONE.

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `bulk-import/page.tsx` | FE component | `grep -r "bulk-import" kiteclass/kiteclass-frontend/src/app/` | Wave 1 BE shipped; FE chưa có | 🆕 to-be-created (Bucket B) |
| `BackupJobFailure` | Prometheus alert | `grep "BackupJobFailure" kitehub/docker/prometheus/` | Alert defined nhưng metric name mismatch | ✅ exists, needs fix |
| `logback-spring.xml` | Java logback config | `find . -name "logback-spring.xml"` | 8 service configs đã shipped Wave 25 | ✅ exists |
| `_pentest_light_2026-XX.md` | Audit artifact | (new) | KHÔNG có pre-existing | 🆕 to-be-created (Bucket A) |

---

## 5. Verification Gates (per bucket)

- **Bucket A:** Audit report ≥ 50/100 OWASP coverage; security headers verified với `curl -sI` (HSTS + CSP + X-Frame-Options); top 3 findings có gap follow-up
- **Bucket B:** `pnpm test --run` + `pnpm build` clean; FE đến BE smoke test (curl POST với mock CSV)
- **Bucket C:** `bash kitehub/scripts/prometheus-alert-test.sh BackupJobFailure` → ALERT FIRING within 30s; GAP-114 closing PR Log có "verification artifact: <pointer>"
- **Bucket D:** Mỗi gap pass `gap-done-discipline.md` §2 checklist (all AC checked OR §3 PARTIAL exit-ramp + follow-up gap)

---

## 6. Agent Spawn Pattern

4 parallel agents (per `agent-background-spawn-default.md` v1.0.0):
```
Agent A: subagent_type=general-purpose, isolation=worktree, run_in_background=true
Agent B: subagent_type=general-purpose, isolation=worktree, run_in_background=true
Agent C: subagent_type=general-purpose, isolation=worktree, run_in_background=true
Agent D: subagent_type=Explore (read-only audit + CSV update), run_in_background=true
```

---

## 7. Closure Protocol

- 4 bucket PRs → squash merge separate
- Closure PR: ROADMAP §🚀 Next Action update + Phase 1 BETA progress report + memory entry
- Append `documents/03-planning/wave-history.jsonl` entry per `feedback_wave_history_append_required.md`
- Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`

---

## 8. Log

- **2026-05-11 (SHIPPED):** Wave 60 closed. 4 buckets ship 4 PR (#1167 C, #1168 A, #1169 B, #1170 D-2) tất cả MERGED.
  - **5 gap DONE:** GAP-406 (OWASP 76/100), GAP-430 (metric verify Wave 41 already shipped), GAP-114 (logging Wave 25 verify), GAP-137 (FE bulk-import), GAP-050 (persona framework), GAP-116-pii.
  - **4 gap PARTIAL** (đúng kỷ luật + follow-up cited): GAP-033, GAP-049, GAP-102, GAP-112.
  - **3 follow-up filed:** GAP-470 K8s `runAsNonRoot`, GAP-471 Vercel FE headers, GAP-472 Gateway `SecurityHeadersFilter` parity (P1 promote → P0 cho v1.0.0 cutover gate).
  - Bucket D audit Explore phát hiện cả 6 candidate fail Criterion 4 (PR# missing) → spawn Bucket D-2 (Option 1) backfill PR# + verify code-ship. Kết quả 2 DONE / 4 PARTIAL.
  - Bucket C fix-time state-check: metric đã đổi từ Wave 41 PR #983 — symptom no-longer-present → flip DONE (Option B scope-cut).
  - Bucket B Vercel rate-limited, admin-merge per `admin-merge-discipline.md` §2 (Frontend Tests + E2E + Build green local + CI).
  - **Wall-clock:** ~30 phút plan-spawn-merge tổng (4 agent parallel longest ~17min Bucket B). Speedup ~10-15× vs ~3-5 ngày estimate.
  - **Streak:** 94 consecutive 0-clarification waves.
- **2026-05-11 (PLAN):** Plan drafted. Triggered by post-pending-legal scope cut (PR #1165) — pre-cutover queue cleared except P0 hardening items.
