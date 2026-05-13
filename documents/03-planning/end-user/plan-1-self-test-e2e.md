---
title: Plan 1 — Self-Test E2E Flow (follow-along guide + Playwright automation scaffold)
status: draft
created: 2026-05-13
updated: 2026-05-13
wave: 69
gaps: [GAP-372, GAP-480, GAP-370]
prs: []
---

> **Wave 69 scope (rescoped 2026-05-13):** Wave này deliver **tooling** chứ không phải execution. 2 deliverables:
> 1. **Follow-along guide** (file này — enhanced với evidence log + command snippets) — user tự manual run lần đầu
> 2. **Playwright spec scaffold** (`kitehub/kitehub-frontend/e2e/production-self-test/`) — automation cho lần re-run sau
>
> User self-execute Plan 1 lần đầu sau Wave 69 SHIPPED; bugs found → Plan 2+. Playwright spec ban đầu skipped-by-default; sau khi flow stable + selectors khớp thì opt-in cho CI hoặc manual on-demand.

# Plan 1 — Self-Test E2E Flow

**Wave thực thi:** Wave 69 (rescoped) — chạy self-test toàn bộ end-user flow trước khi mời người thật.
**Lý do tồn tại:** GAP-372 (invite mechanism) marked DONE per checkbox nhưng chưa từng có session test thật end-to-end trên production. Code path có thể vấp lỗi ở bất kỳ bước nào. Cần verify trước khi outreach cohort thực.
**Cohort:** chỉ `admin@kitehub.me` self-acting both Coordinator + Beta Tenant (dual role).

---

## 1. Goal

Trả lời 1 câu hỏi duy nhất: **"Một người dùng mới có thể đi từ landing page → request → approve → invite email → signup → tạo lớp đầu tiên — KHÔNG vấp lỗi nào không?"**

Output: pass/fail per bước + log bugs found + fix plan trước Plan 2 (real cohort).

---

## 2. Pre-requisites (đã satisfy hay chưa)

| Pre-req | Status |
|---|---|
| FE `kitehub.me` Vercel deploy 200 | ✅ verified Wave 68 |
| BE `api.kitehub.me/actuator/health` 200 | ✅ verified Wave 68 |
| Database seeded `admin@kitehub.me` PLATFORM_ADMIN | ✅ verified Wave 67 |
| ALB clean (no 502 drift) | ✅ verified Wave 68 GAP-501 |
| GAP-372 code path shipped (form + admin + email + token) | ✅ DONE marked |
| Personal email khác để self-test recipient | ⚠️ user provide |
| Status page (Instatus per ADR-027) | ⚠️ verify trạng thái live |
| Kênh feedback (form/email/Discord) | ⚠️ chốt path |

---

## 2.5 Setup trước khi chạy

### Tab/cửa sổ cần mở
- Browser tab 1: `https://kitehub.me/` (FE marketing)
- Browser tab 2: Gmail/email cá nhân thứ 2 (recipient cho invite email)
- Browser tab 3: AWS Console SES → Verified Identities (verify recipient nếu sandbox)
- Terminal 1: tail logs / DB query khi cần
- Notion/Markdown editor: ghi evidence log realtime (xem §5)

### Helper commands chạy parallel (terminal)

```bash
# Set credentials
export AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1

# Quick health re-check (run before mỗi bước nếu nghi ngờ)
curl -sS -o /dev/null -w "FE %{http_code} | API %{http_code}\n" https://kitehub.me/ https://api.kitehub.me/actuator/health

# SES verify identity cho recipient (Bước 4)
aws sesv2 create-email-identity --email-identity <recipient-email>
# → AWS gửi email tới <recipient>, user click link verify trong inbox đó

# Query DB qua SSM bastion (Bước 2/3/5 verify state)
aws ssm start-session --target i-05d7af46d01436b96 --document-name AWS-StartInteractiveCommand --parameters command="docker exec kite-postgres psql -U kite -d kitehub -c 'SELECT id, email, status, created_at FROM beta_access_request ORDER BY id DESC LIMIT 5;'"

# SES send statistics (Bước 4 verify email actually sent)
aws sesv2 get-account --query '{Sent:SendQuota.SentLast24Hours,Max:SendQuota.Max24HourSend}' --output table

# CloudWatch tail BE logs (background nếu cần debug)
aws logs tail /aws/ssm/kite-deploy --since 5m --follow
```

---

## 3. Scope — 7 bước E2E

### Bước 1 — Landing page

- [ ] Mở `https://kitehub.me/` → load thành công, không 4xx/5xx
- [ ] Hero + CTA hiển thị tiếng Việt đúng tone
- [ ] Click "Request Beta Access" → navigate `kitehub.me/request-beta-access`

### Bước 2 — Submit request form

- [ ] Form fields hiển thị: email, name, org, persona, source
- [ ] Validation client-side (email format, required fields)
- [ ] Submit form bằng email cá nhân thứ 2 (không phải admin@kitehub.me)
- [ ] Network call POST `/api/beta-access/request` → 200 hoặc 201
- [ ] UI show success state "Yêu cầu của bạn đã được ghi nhận..."
- [ ] DB row trong `beta_access_request` table tồn tại (verify qua admin endpoint hoặc psql)

### Bước 3 — Admin review + approve

- [ ] Login `admin@kitehub.me` vào `kitehub.me/admin` (hoặc subdomain admin nếu có)
- [ ] Navigate `/admin/beta-requests` — see new request từ Bước 2
- [ ] View request detail — persona/source/timestamp đúng
- [ ] Click "Approve" → BE call thành công
- [ ] DB row update: `status: APPROVED`, `approved_at` set, `invite_token` generated
- [ ] (Nếu SES sandbox không gửi được) — manual copy invite token + signup link

### Bước 4 — Invite email (path C1 hoặc C2)

**Path C1 — qua SES sandbox:**
- [ ] AWS Console SES → Create identity với email cá nhân thứ 2
- [ ] Click verification link trong inbox
- [ ] Re-trigger send invite từ admin dashboard (hoặc auto-send sau approve)
- [ ] Email received tại inbox cá nhân
- [ ] Email content: subject tiếng Việt, brand logo render, link signup có token, disclaimer beta period

**Path C2 — manual send:**
- [ ] Copy invite token + signup link từ admin dashboard
- [ ] Gửi qua Gmail cá nhân với template tay
- [ ] Verify link mở được, token còn valid (TTL chưa expire)

### Bước 5 — Signup với token

- [ ] Click signup link → navigate `kitehub.me/beta-signup?token=XXX`
- [ ] Token validate ở BE — nếu invalid → error message rõ; nếu valid → render signup form
- [ ] Complete signup form (password + confirm + ...) → submit
- [ ] BE provision tenant với beta-flag=true
- [ ] DB rows tạo: tenant entry, user entry, default config
- [ ] Auto-login hoặc redirect tới login page
- [ ] Đăng nhập thành công

### Bước 6 — Tạo lớp đầu tiên (core flow KiteClass)

- [ ] Dashboard load — header brand đúng, tenant name hiển thị
- [ ] Navigate "Create Class" → form mở
- [ ] Điền minimum fields: tên lớp, môn học, schedule
- [ ] Submit → class created → redirect class detail
- [ ] Add 1 student → student tạo thành công
- [ ] Add 1 buổi học → schedule entry tạo thành công

### Bước 7 — Logout + re-login

- [ ] Logout từ dashboard → redirect login page
- [ ] Re-login với credentials vừa tạo → session khôi phục
- [ ] Dashboard hiển thị data đã tạo (class + student + buổi học)

---

## 4. Pass/Fail Criteria

| Outcome | Action |
|---|---|
| 7/7 bước pass | Plan 1 SHIPPED. Wave 69 DONE. File Plan 2 (real cohort outreach) |
| 5-6/7 pass, các fail có workaround | Plan 1 PARTIAL. File gap cho mỗi fail. Wave 69 PARTIAL. Decide: fix trước Plan 2 hay accept với caveat |
| ≤4/7 pass | Plan 1 BLOCKED. Wave 69 không SHIPPED. File P0 gaps cho mọi fail. Pause beta cohort outreach cho đến khi fix |

---

## 5. Bugs/Findings Log

Mỗi bug phát hiện trong khi self-test:

```markdown
### Bug N — Bước X — [title]
- **Symptom:** [mô tả]
- **Repro:** [steps]
- **Expected:** [behavior]
- **Actual:** [behavior]
- **Severity:** P0/P1/P2
- **Filed as gap:** GAP-XXX
```

(Section này populate trong khi run, không pre-fill)

---

## 6. Acceptance Criteria

- [ ] Tất cả 7 bước có kết quả pass/fail rõ ràng
- [ ] Mỗi fail → file gap riêng + severity assessment
- [ ] Log evidence (screenshot/network/DB query) cho ≥3 bước critical (Bước 2, 3, 5)
- [ ] Plan 1 status flip → `complete` hoặc `partial` per §4 matrix
- [ ] Wave 69 closure PR cập nhật wave-history.jsonl + ROADMAP §🚀

---

## 7. Out-of-scope (track separately)

- Real cohort outreach + invite execution → Plan 2 (filed after Plan 1 ships)
- Rollback drill (`scripts/smoke-rollback-cycle.sh --execute`) → Plan 3 hoặc separate wave bucket
- SES production access re-submit → Plan 4 hoặc parallel passive action
- Status page Instatus setup verification → Plan 1 §2 pre-requisite check trước, nếu chưa setup thì file follow-up
- Feedback channel setup (form/email/Discord) → Plan 2 pre-requisite (real cohort cần kênh; self-test admin không cần)

---

## 8. Effort estimate

- Pre-requisite verification: ~30 min
- Self-test execution 7 bước: ~2-3h (slow để document evidence)
- Bug filing + severity assessment: ~30-60 min/bug
- Total best case (0 bugs): ~3h
- Total worst case (3+ bugs): ~6-8h

---

## 9. Related

- **Wave:** [`wave-2026-05-XX-69-...md`](../waves/) (sẽ tạo khi Wave 69 spawn)
- **Parent gap:** GAP-372 (invite mechanism — DONE marked, this plan verifies)
- **Sibling gaps:** GAP-480 (invite flow undefined — close khi plan ship), GAP-370 (SES decision affects Bước 4 path)
- **Reference:** [`../roadmap/release-1-deploy-plan.md`](../roadmap/release-1-deploy-plan.md) §2.3 Beta invite mechanism flow
- **Next plan:** Plan 2 (real cohort outreach) — only fileable sau khi Plan 1 SHIPPED

---

## 10. Log

- **2026-05-13:** Plan created. Triggered by user-flagged audit during Wave 68 closure: "GAP-372 code path shipped nhưng chưa từng test thật end-to-end". Wave 69 scope rebased from original "rollback drill + first beta invite + SES decision" → focus solo trên self-test E2E. Real cohort + rollback drill + SES re-submit move to Plan 2/3/4 hoặc parallel passive actions.
- **2026-05-13** (path fix pre-execution): User probed `kitehub.me/auth/request-beta-access` → 404. Root cause: Next.js App Router `(auth)` là **route group** (folder ngoặc đơn) — không thêm vào URL path. Source `src/app/(auth)/request-beta-access/page.tsx` deploy thành route `/request-beta-access`. Plan 1 + Playwright spec dùng path `/auth/*` sai → fixed. Live probe verified routes correct: `/request-beta-access` 200 (Bước 2), `/login` 200 (Bước 3+7), `/beta-signup` 200 (Bước 5), `/admin/beta-requests` 200 (Bước 3). `/register` 404 (source có nhưng deploy thiếu — separate finding ngoài scope Plan 1).
