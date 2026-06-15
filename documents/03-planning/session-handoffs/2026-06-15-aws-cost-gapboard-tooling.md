---
title: Session handoff — AWS cost-cut + gap-board host + GAP-1020 + tooling
date: 2026-06-15
scope: aws-cost / gap-board / meta-tooling
status: complete
---

# Session handoff — 2026-06-15 (chiều) AWS cost + gap-board + tooling

Phiên reactive (không phải wave): xử lý AWS bill-spike → tooling → fixes. **6 PR merged, 0 open PR** tại close.

## 1. Shipped (tất cả merged vào main)

| PR | Nội dung |
|---|---|
| #2430 | AWS cost-reduction sweep + `aws-cost-guard` rule + cost-watch (collect-state.sh) + runbook |
| #2431 | Spring Boot 3.5.14 → 3.5.15 (re-apply dependabot #2417/#2418 đã bị close) |
| #2432 | 4 Java warning fixes (LmsMapper / SubscriptionExpirationChecker / 2 test) |
| #2434 | GAP-1020 branding RLS GUC + tier server-side resolve (369 ITs PASS) → DONE |
| #2433 | Gap board: `render-gap-board.py` + `serve-gap-board.py` host + `kite-gap-board` compose service + click-view rendered + clickable refs |
| #2435 | `/end-session` slash command (trỏ skill có sẵn) |

## 2. AWS cost (chi tiết: `documents/05-guides/operations/aws-cost-and-credits-runbook.md`)

- **Sự cố:** June ~$40 tiền thật (credit cạn; root cause `docker-build-push.yml` push:main → ECR 206GB/2746 img).
- **Đã làm:** ECR prune → 0 · CloudWatch teardown (dashboard+15 alarm+5 loggroup, giữ CloudTrail) · CI deploy-only push (tag/dispatch).
- **Posture:** keep-stopped ~$15-18/mo. ⚠️ **RDS auto-start sau 7 ngày → chạy `bash scripts/aws/stop-stack.sh --force` ~mỗi tuần.** Giữ EIP `52.221.161.175` (DNS ổn định).
- **Open follow-up:** AWS Support courtesy-credit case **#178149494800729** ($40) — status Unassigned, ETA 1-3 ngày làm việc. Theo dõi Support Center.
- Memory: [[project-aws-cost-posture]].

## 3. Pickup (việc tiếp theo — KHÔNG urgent)

- Phiên này **đóng sạch, 0 PR pending** — không có việc bắt buộc.
- Flow Verification Campaign vẫn là sub-mode chính (per CLAUDE.md): 5 priority flow (SSO/LMS/RBAC/attendance/reports) đã pre-walk-hardened (fix trên main, stack rebuilt) — sẵn sàng human G2★ walk. Residual checks: `documents/04-quality/audits/persona-review/2026-06-15-pre-walk-5-priority-flows.md` (branch `audit/pre-walk-5-flows-2026-06-15`, chưa merge — chỉ là audit artifact).
- GAP-1406 (filed): 2 branding tier-header sister sites defer (gateway-strip đã đóng spoof).
- GAP-1405 (P2): storage create-path FK-set follow-up.

## 4. Background / stack state

- **Gap board host:** standalone server đã bị kill. Main giờ có `kite-gap-board` compose service → `bash kitehub/scripts/up.sh --profile full` (hoặc `infra-only`) tự lên board ở **http://localhost:8787**.
- **Docker stack G2:** 14 container healthy (KH :3001 / KC :3000), kc-core healthy (V87 attendance gate passed).
- **AWS:** stack STOPPED on-demand.

## 5. Known issues / lessons

- **Worktree churn:** main tree bị background-agent worktree switch branch 2 lần giữa phiên (đúng `worktree-only-branch-work` cảnh báo). Husk worktrees đã dọn ở end-session. Lesson: agent isolation worktree đôi khi đụng main tree — verify branch trước commit.
- Translation view-layer cho docs language-drift: user từ chối build (drift tồn tại; rule `dev-readable-doc-language` enforce doc mới).

## 6. Start next session

```bash
git fetch origin main && git log origin/main -1 --oneline   # expect 32c02e984 hoặc mới hơn
# /start-session loads MEMORY (project_aws_cost_posture) + handoff này
bash kitehub/scripts/up.sh --profile full   # nếu cần board + stack lên
```
