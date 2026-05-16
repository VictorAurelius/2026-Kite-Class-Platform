---
title: P3 Manager beta-invite email content audit (GAP-587)
status: complete
created: 2026-05-16
phase: Wave 86 docs-cluster closure
wave: 86
gaps: [GAP-587]
related_bucket: Bucket G (Resend production)
auditor: Solo dev coordinator
---

# P3 Manager Beta-Invite Email Content Audit (GAP-587)

## 1. Scope

Audit `beta-invite.html` template against P3 Manager-specific requirements:

1. Subject line cite P2 owner name + center name
2. Sender `support@kitehub.me` with reply-to = P2 owner email (P3 reply → P2 escalation chain)
3. Body opening cite P2 owner name + center context
4. Role disclosure explicit (Manager permissions có/không có)
5. Permission matrix link `/help/p3-manager/permissions`
6. Footer "Nếu chưa nhận lời mời, bạn có thể từ chối hoặc bỏ qua email này"

**File audited:** `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html` (current production — single shared template; P3-specific variant missing).

**Method:**
1. Read template line-by-line.
2. Verify whether P3-specific path exists (search `EmailServiceClient.java` for P3 send method).
3. Map findings to 6 P3-specific criteria.
4. Identify whether template fork OR template enhancement needed.

---

## 2. Current state — single template shared

**Finding:** Hiện tại `beta-invite.html` là **single shared template** dùng cho cả P1 / P2 / P3 cohort. KHÔNG có P3-specific template fork.

**Inference from template:**
- Variables available: `${orgName}` (works for P1 person OR P2 center name; ambiguous for P3 Manager)
- Variables MISSING for P3:
  - P2 owner name (e.g., "chị Hằng")
  - Center name (e.g., "Trung tâm Sky Education")
  - Role name (e.g., "Manager") + permission scope
  - Reply-to override (P3 email reply → P2 escalation)

**Send method (search):**
- `EmailServiceClient.java` shows `sendBetaInvite(...)` exists; P3-specific overload NOT found.
- P3 invite trigger code path NOT separated từ P1/P2 flow.

**Verdict:** ❌ **P3 invite email infrastructure missing** — both template fork + send method overload needed.

---

## 3. Per-criterion audit

### Criterion 1 — Subject line cite P2 owner + center

**Current state:** Template subject line 5:
```html
<title th:text="|Lời mời beta - ${branding?.displayName ?: 'KiteClass'}|">Lời mời beta</title>
```

Translation: "Beta invitation - KiteHub" — generic; no P2 owner OR center.

**P3 expectation:** Subject = `"[chị Hằng] mời bạn làm Manager tại [Trung tâm Sky Education]"`

**Verdict:** ❌ FAIL — subject doesn't surface P2 owner OR center for P3 cohort.

### Criterion 2 — Sender + reply-to override

**Current state:** Single sender `support@kitehub.me`; no reply-to override mechanism in template.

**P3 expectation:**
- `From: support@kitehub.me` (consistent)
- `Reply-To: chị Hằng <hang@sky-education.com>` (P2 owner email — set per-invite)

**Verdict:** ❌ FAIL — reply-to override not implemented. Wave 86 Bucket G ship Resend production setup may need to add `replyTo` parameter to Resend send API call.

### Criterion 3 — Body opening cite P2 owner + center + role

**Current state:** Template line 90: `Xin chào <strong th:text="${orgName}">Tổ chức</strong>,`

P3 expectation: body opening:
> "Chào anh Tâm, chị Hằng (chủ Trung tâm Anh ngữ Sky Education) đã mời anh làm Manager."

**Verdict:** ❌ FAIL — current `${orgName}` interpolation insufficient. P3 needs new variables `${inviterName}`, `${centerName}`, `${inviteeName}`, `${roleName}`.

### Criterion 4 — Role disclosure explicit

**Current state:** Template body (lines 96-102) is 3-step "complete signup" instruction — no role/permission disclosure.

**P3 expectation:** Body include explicit permission block:
> "Bạn có quyền: nhập điểm, điểm danh, xem lịch lớp"
> "Bạn KHÔNG có quyền: xóa lớp, sửa giá, xem doanh thu"

**Verdict:** ❌ FAIL — permission disclosure missing. Critical for P3 trust + clarity (per persona cell 4.1).

### Criterion 5 — Permission matrix link

**Current state:** No `/help/p3-manager/permissions` link in template.

**P3 expectation:** Body include link to permission matrix page.

**Page status:** ⏳ `/help/p3-manager/permissions` not yet shipped (P3 user manual = Wave 80+ Bucket F2 scope per `user-manual-content-standard.md`).

**Verdict:** ❌ FAIL — link absent (and target page pending).

### Criterion 6 — Footer "Nếu chưa nhận lời mời..."

**Current state:** Line 130:
> "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email — không có hành động nào được thực thi."

Translation: "If you didn't make this request, please ignore this email — no action will be performed."

**Verdict:** ⚠️ **PARTIAL** — semantically equivalent but tone is "you may have made a request" (P1/P2 self-signup framing). P3 framing needs adjustment:
> "Nếu bạn chưa từ chối lời mời, hoặc thấy thông tin không khớp, bạn có thể bỏ qua email này — chị Hằng sẽ không bị thông báo."

---

## 4. Verdict summary

| Criterion | Status | Action |
|---|---|---|
| 1. Subject cite P2 owner + center | ❌ FAIL | Template fork OR variable interpolation |
| 2. Sender + reply-to override | ❌ FAIL | EmailServiceClient + Resend API replyTo param |
| 3. Body opening P2 + center + role | ❌ FAIL | New template variables |
| 4. Role permission disclosure | ❌ FAIL | New permission block in template |
| 5. Permission matrix link | ❌ FAIL | Target page pending Wave 80+ |
| 6. Footer P3-framed | ⚠️ PARTIAL | Body text adjustment |

**Audit verdict:** ❌ **P3 invite email infrastructure incomplete** — 5/6 FAIL, 1/6 PARTIAL.

**Implementation scope:**
- **Phase A (Wave 87+):** Fork template `beta-invite-p3-manager.html` với P3-specific variables; add `sendP3ManagerInvite(...)` overload trong `EmailServiceClient.java` + `BetaAccessService.java`; add `replyTo` param to Resend API call.
- **Phase B (Wave 80+ Bucket F2):** Ship `/help/p3-manager/permissions` page; backfill template Criterion 5 link.

**Wave 86 docs-cluster scope:** Audit complete; implementation = Wave 87+ follow-up gap.

---

## 5. Wave 86 Bucket G interim mitigation

Since P3 invite infrastructure incomplete + Wave 86 cohort = 2 P1 tenants + 0 P3 (per ROADMAP Wave 86 cohort definition), **P3 invite path NOT exercised in Wave 86 Bucket G first sends.**

**Action:** When P3 invites become live (Wave 87+ when chị Hằng P2 invites anh Tâm P3), file follow-up gap to implement the 5 FAIL items before send.

**No urgency Wave 86** — P3 cohort = Wave 88+ per phase plan.

---

## 6. Recommended follow-up gap

File new gap `GAP-587b p3-invite-email-template-fork-implementation` with:
- **Priority:** P1 (gate P3 cohort onboarding)
- **Phase:** Phase 1 BETA
- **Trigger:** before first P3 Manager invite send
- **Scope:** Items §3 Criterion 1-6 implementation Phase A + Phase B.
- **Dependencies:** Wave 80+ Bucket F2 P3 user manual page; Wave 86 Bucket G Resend `replyTo` param support.

GAP-587 status flip OPEN → **PARTIAL** (audit complete; implementation deferred to GAP-587b).

---

## 7. References

- **GAP-587:** `documents/04-quality/gaps/GAP-587-p3-invite-email-content-audit.md`
- **Template:** `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html`
- **Send code:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java`
- **Persona audit:** `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.4 cell 4.1
- **Wave 86 plan:** §3 Bucket G G-AC5 + Bucket C C-AC2 (P3 first-login permission matrix)
- **User manual rule:** `.claude/rules/user-manual-content-standard.md` §3 P3 entry-point matrix
- **Sibling audit:** GAP-586 P1 audit (this same PR) — shares template; P1 fixes ship interim while P3 fork = Wave 87+ scope.
