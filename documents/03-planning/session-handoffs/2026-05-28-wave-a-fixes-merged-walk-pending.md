---
audience: dev
date: 2026-05-28
session-theme: Wave A pre-deploy — 5 fixes merged (parallel agents), walk + DONE-flip pending next session
status: complete
next-session-focus: Rebuild stack → RST walk 5 luồng → rst-html findings → flip gap DONE + sync
---

# Session handoff — Wave A 5 fixes merged, walk pending (2026-05-28)

## Đã ship session này (5 fix merged main, qua parallel Opus agents)

| PR | Gap | Scope | Service |
|---|---|---|---|
| #1935 | GAP-790 | Gateway `TenantResolver` cho staff-invitations + onboarding-progress + sweep route (tách route public-token cho by-token/accept) | kite-gateway |
| #1937 | GAP-791 + GAP-792 | Course nativeQuery tenant predicate + `@Cacheable` tenant key + sweep (TeacherRepository + Teacher/LeadServiceImpl) + re-enable `CourseClassCrudOwnerIT` (5/5 PASS) | kiteclass-core |
| #1938 | GAP-787 + GAP-793 | **Root cause thật**: kitehub-email thiếu `@RabbitListener` cho queue `email.send` → MỌI email giao dịch bị drop ở queue mode. Fix: `EmailEventListener` + `EmailQueueConsumerConfig` + `@Primary EmailProviderRouter` (interface `EmailSender`, route theo `email.provider`). Supersede #1936. | kitehub-email + subscription |
| #1939 | GAP-794 | Anonymous PDPL consent 401 — SecurityConfig permitAll trỏ `/consent/cookie` (không tồn tại) thay vì `/consent/record` + `/{visitorId}` thật. Fix permitAll đúng endpoint + 500→400 nit. Sweep bắt 2 dead matcher khác (public-config + payments/webhook → defer). | kitehub-subscription |

**Gaps CHƯA flip DONE** — đúng theo `feature-ship-runtime-walk-mandate.md`: code merged nhưng cần RST walk trên stack đã rebuild mới flip. Hiện GAP-790/791/792/787 OPEN; GAP-793/794 PARTIAL (80%).

## AWS email-prod đã làm rõ (quan trọng)

- AWS account **ACTIVE** (không suspended — GAP-612 note cũ đã outdated; collect-state query AWS OK đầu session). Stack chỉ đang **stopped** (startable qua `bash scripts/aws/start-stack.sh`).
- Secrets Manager có sẵn `kitehub/production/resend-api-key` + `ses-smtp-credentials`.
- SES = **sandbox** (`ProductionAccessEnabled: false`) + identity chưa verify → SES KHÔNG gửi được prod. **Resend là đường prod duy nhất** → fix routing GAP-793 đúng hướng + cần thiết.
- GAP-793 note AWS-stale đã sửa trong gap-status.csv (resend verify cần prod-equiv run + RESEND_API_KEY, không phải "AWS suspended").

## NEXT SESSION — bắt đầu từ đây (theo thứ tự)

1. **Rebuild gộp 1 lần** (stack local đang up, 13 service healthy):
   - `kite-gateway` (790) + `kiteclass-core` (791/792) + `kitehub-subscription` (794) + `kitehub-email` (793)
   - Dùng `bash kitehub/scripts/rebuild.sh <svc>` (verify script path); kiteclass-core có thể cần đường riêng.
2. **RST walk 5 luồng** trên stack đã rebuild → doc `documents/04-quality/audits/rst-html/2026-05-28-wave-a-pre-deploy-5-flow-walk.md` (format theo `2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md`: frontmatter + Bước-by-Bước + HTTP/DB/side-effect + bug class table). Catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4. 5 luồng:
   - (1) Anonymous signup → admin approve → email → set password → login
   - (2) Owner onboarding wizard (route onboarding-progress nay đã có TenantResolver)
   - (3) Course/Class CRUD cross-tenant (verify 791/792 fix: list không leak + cache không poison)
   - (4) Email delivery MailHog: staff-invite (GAP-787) **+ beta-invite (GAP-702)** — cùng consumer fix #1938, verify cả hai
   - (5) PDPL consent anonymous: `POST /api/v1/consent/record` + `GET /consent/{uuid}` không 401 (GAP-794)
   - Owner test cred: `owner.test@test.vn` / `Test@1234`, tenant `877dff9d-c354-4faf-8c44-3c17196dbf24`.
3. **Promote RST findings → E2E spec** per `e2e-rst-test-layer-boundary.md` (mỗi bug → E2E).
4. **Flip gap DONE** (chỉ khi walk pass) + sync 4-target (gap-status.csv + ROADMAP + wave-history + handoff).

## Follow-up đã ghi nhận (chưa làm)

- **Cache-record-serialization defect** (Agent B nêu): `GenericJackson2JsonRedisSerializer` không round-trip `CourseResponse` record khi same-key warm-reread → 500. Ngoài scope 792. File gap riêng.
- **2 sister dead SecurityConfig matchers** (Agent E sweep): `/api/v1/public-config/**` (thật `/api/platform/config`) + `/api/v1/payments/webhook` (thật `/api/platform/webhooks/payment`) → defer, platform-namespace gateway-rewrite scope.
- **Worktree husks: 4** (3 cũ `agent-adcae/aeb06/affe2` + `agent-acc02b66...` của Agent E) → `bash scripts/prune-merged-worktrees.sh --dry-run` rồi prune. Branch `feature/GAP-794-...` còn checkout trong worktree acc02b66 (xóa worktree trước rồi mới xóa branch).

## META rules shipped session này (qua agents, paired same-PR)
- `cross-flow-bug-class-sweep.md` (đã có từ session trước; agents áp dụng)
- Không thêm rule mới session này — chỉ áp dụng existing.

## Session note
- Lỗi coordinator: spawn 4 agent đầu KHÔNG truyền `isolation: "worktree"` → agents dùng chung shared checkout, contamination nhẹ (gateway file lẫn vào #1936/#1938 nhưng identical → git tự resolve, không hại). Agent E (thứ 5) đã truyền `isolation:"worktree"` đúng → sạch. **Lesson: luôn set `isolation:"worktree"` cho code-write agents.**
- Context session lên 86% do rule auto-load lặp (đọc file trong worktree re-inject ~20 rule). Walk tách session mới.
