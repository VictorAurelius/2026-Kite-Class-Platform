# GAP-1371: Ops-readiness baseline carry-forward stale — GAP-144 + GAP-612 đã DONE nhưng vẫn bị cite là P0 carry

**Status:** 🟢 DONE
**Priority:** 🟢 P3
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — audit-hygiene)
**Resolved:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`)
**Affects:** `.claude/rules/output-review-mandate.md` §3 row "Ops readiness" + ops-readiness audit cross-refs

## Problem

`output-review-mandate.md` §3 matrix row "Ops readiness" hiện ghi: `⚠️ 77/100 C+ (2026-05-18, Wave 94c) ... 3 P0 carry GAP-257/144/612`. Nhưng:

- **GAP-144** (Alertmanager production receivers) → **DONE** Wave 86 Bucket H (SNS-direct adaptation; `gap-status.csv` 100% closed).
- **GAP-612** (AWS account suspension) → **DONE** Wave aws-restore-1 2026-05-26 (account restored; live smoke HTTP 200).

→ Chỉ còn GAP-257 (restore drill) là P0 carry thật. Việc cite 2 gap đã đóng làm "P0 carry" gây audit accuracy drift — reader/audit kế tiếp đánh giá sai posture (tưởng alerting routing + AWS availability vẫn FAIL trong khi đã fix). Task brief của audit này cũng kế thừa state cũ ("AWS SUSPENDED") từ stale ref.

Đây là class doc-sync (audit baseline ref không theo kịp gap closure). Audit 2026-06-14 này KHÔNG được phép sửa `output-review-mandate.md` (scope = report + gaps + 2 CSV), nên track bằng gap để sync ở PR sau.

## Proposed Fix

Update `output-review-mandate.md` §3 row "Ops readiness" → score 78/100 C+ (2026-06-14) + carry-forward đúng = chỉ GAP-257 (restore drill thật) còn P0; GAP-144 + GAP-612 đã DONE. Cân nhắc bổ sung note nhỏ trong audit-to-gap-pipeline: baseline carry list phải re-check gap-status.csv trước khi cite.

## Acceptance Criteria

- [x] `output-review-mandate.md` §3 "Ops readiness" row reflect score 78/100 + carry-forward chính xác — row updated → `78/100 C+ PARTIAL FAIL (2026-06-14, ops-readiness full audit); 1 P0 carry GAP-257; GAP-144 + GAP-612 no longer P0 carry`.
- [x] Audit refs gần đây không cite GAP-144/612 là "P0 carry" nữa — the §3 row (the canonical reference cited by the audit task brief) is corrected; the 2026-06-14 ops-readiness audit report itself already records the correction (audits-index.csv note "KEY CORRECTION: baseline 3 P0 carry stale").

## Resolution (2026-06-15)

Synced the stale baseline carry-forward in `.claude/rules/output-review-mandate.md` §3 row "Ops readiness": was `⚠️ 77/100 C+ (2026-05-18, Wave 94c) ... 3 P0 carry GAP-257/144/612` → now `⚠️ 78/100 C+ PARTIAL FAIL (2026-06-14, ops-readiness full audit) ... 1 P0 carry GAP-257; GAP-144 alerting (DONE Wave 86) + GAP-612 AWS restore (DONE Wave aws-restore-1) no longer P0 carry`. Verified against `gap-status.csv`: GAP-144 + GAP-612 both DONE; only GAP-257 (restore drill) remains a P0 carry. (The 2026-06-14 audit was scope-barred from editing this rule — hence this follow-up gap, now resolved.)

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (OPS-010).
- GAP-144 (DONE Wave 86), GAP-612 (DONE Wave aws-restore-1), GAP-257 (restore drill — vẫn P0 carry thật).
- `audit-to-gap-pipeline.md` (carry-forward accuracy), `gap-done-discipline.md` (gap closure sync).
