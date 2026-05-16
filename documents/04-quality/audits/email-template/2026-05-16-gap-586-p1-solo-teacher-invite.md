---
title: P1 Solo Teacher beta-invite email content audit (GAP-586)
status: complete
created: 2026-05-16
phase: Wave 86 docs-cluster closure
wave: 86
gaps: [GAP-586]
related_bucket: Bucket G (Resend production)
auditor: Solo dev coordinator
---

# P1 Solo Teacher Beta-Invite Email Content Audit (GAP-586)

## 1. Scope

Audit current `beta-invite.html` template against 5-point P1 Solo Teacher cohort checklist từ Wave 86 plan §3 Bucket G G-AC5:

1. Sender = `support@kitehub.me` (NOT `noreply@`)
2. Vietnamese tone natural per `dev-readable-doc-language.md`
3. Có tên thật người duyệt + feedback CTA visible
4. Link tới `/status` (Statuspage Wave 84 GAP-424)
5. Link welcome guide `/help/p1-solo-teacher` (anonymous landing → user-manual)

**File audited:** `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html` (current production template — single shared between P1 + P2 + P3 cohorts).

**Method:**
1. Read template line-by-line.
2. Cross-check sender configuration in `application.yml` + `EmailServiceClient.java`.
3. Map findings to 5 checklist criteria.
4. Decide DONE / PARTIAL flip per `gap-done-discipline.md` §2.

---

## 2. Per-criterion audit

### Criterion 1 — Sender `support@kitehub.me` (NOT `noreply@`)

**Current template:** Footer line 138 says:
> "Email này được gửi tự động, vui lòng không trả lời."

**Translation:** "This email is sent automatically, please do not reply."

**Verdict:** ⚠️ **PARTIAL** — Template body itself uses `${branding?.contactEmail ?: 'support@kiteclass.com'}` for inline feedback CTA (line 125), which is correct. BUT the footer text "vui lòng không trả lời" (please do not reply) sends a noreply signal — semantic mismatch with `support@kitehub.me` reply-friendly intent.

**Sender configuration:** Per Wave 86 Bucket G expected setup, Resend env `RESEND_FROM_ADDRESS=support@kitehub.me`. Verify at runtime — not template-level concern.

**Action:** ⚠️ Edit footer text to remove "vui lòng không trả lời" and replace với reply-encouragement message. See §3 §template fix below.

### Criterion 2 — Vietnamese tone natural

**Current template body (Vietnamese):**
- Line 86: `Bạn được mời tham gia beta ${branding?.displayName ?: 'KiteClass'}!`
- Line 90: `Xin chào <strong>${orgName}</strong>,`
- Line 92: `Cảm ơn ${orgName} đã đăng ký quan tâm đến chương trình beta của ${branding?.displayName ?: 'KiteClass'}.`
- Line 96: `Chúng tôi vui mừng thông báo yêu cầu của bạn đã được duyệt. Vui lòng hoàn tất đăng ký theo các bước sau:`
- Lines 99-101: 3-step instruction (Vietnamese OK)
- Line 122-127: Beta disclaimer (Vietnamese OK)
- Line 130: `Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email — không có hành động nào được thực thi.`

**Verdict:** ✅ **PASS** — Tone is natural Vietnamese conversational; no awkward English-translated phrasing. `${orgName}` interpolation works for both P1 (solo teacher name) and P2 (center name) contexts.

**Note:** P1 Solo Teacher = giáo viên cá nhân (1 person), so `${orgName}` may render as "Trần Thị Hồng" (person name) — Vietnamese addressing OK ("Xin chào Trần Thị Hồng" feels natural). Alternative would be to introduce P1-specific addressing `Xin chào chị/anh ${firstName}` — Phase 1.5+ refinement scope.

### Criterion 3 — Tên thật người duyệt + feedback CTA visible

**Current template:**
- ❌ No human sender name (e.g., "Chị Mai từ KiteHub") in body text.
- Line 132: `Đội ngũ ${branding?.displayName ?: 'KiteClass'}` — generic team sign-off, NOT personal.
- ✅ Feedback CTA present in disclaimer block (lines 121-127):
  > "Nếu bạn có phản hồi, vui lòng liên hệ support@kiteclass.com."

**Verdict:** ⚠️ **PARTIAL** — Feedback CTA visible ✅; human sender name missing ❌. Per persona cell 2.2 chị Hồng feedback: "thiếu tên thật người duyệt → cảm thấy spam".

**Action:** Add line above sign-off (line 131-132) với specific Vietnamese:
> `Chị Mai từ KiteHub đây — em là người duyệt yêu cầu của ${orgName}. Có thắc mắc gì cứ reply email này hoặc liên hệ Zalo OA (sắp có).`

### Criterion 4 — Link tới `/status` (Statuspage Wave 84 GAP-424)

**Current template:** ❌ **NOT present.** No reference to `/status` page in body or footer.

**Wave 86 expectation:** Footer or beta disclaimer block should include link "Trạng thái hệ thống: <https://kitehub.me/status>" so tenant có cảm giác trust + can self-check service status.

**Action:** Add to footer or disclaimer block:
```html
<p style="font-size: 12px; color: #6B7280; margin-top: 8px;">
  📊 Trạng thái dịch vụ Beta: <a href="https://kitehub.me/status">kitehub.me/status</a>
</p>
```

### Criterion 5 — Link welcome guide `/help/p1-solo-teacher`

**Current template:** ❌ **NOT present.** No link to user manual / welcome guide.

**Wave 86 expectation:** Phase 1 BETA user manual (anonymous-prospect 5-page foundation shipped Wave 79; P1 Solo Teacher persona page pending Wave 80+ Bucket F2 — per `user-manual-content-standard.md` rule).

**Status of P1 user manual page:** ⏳ Not yet shipped (per GAP-537 follow-up phasing). Anonymous landing `documents/05-guides/user-manual/anonymous/` exists; persona-specific `/help/p1-solo-teacher` page = Wave 80+ Bucket F2 scope.

**Action options:**
- **(A) Link to anonymous landing** `/help/anonymous` interim while P1 page pending → user can navigate from there.
- **(B) Defer Criterion 5 to GAP-586b follow-up** synced với Wave 80+ Bucket F2 P1 page ship.

**Recommended:** Option A — link to `/help/anonymous` (sufficient overlap; chị Hằng/Hồng can find "Đầu tiên" / "Bảng giá" landing topics there).

**Mail-Tester score:** Wave 86 expectation ≥ 8/10. Mail-Tester not run trong audit scope (requires live Resend send). Recommend run after template fix lands → Bucket G ship.

---

## 3. Recommended template fix (single shared template diff)

Apply these edits to `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html`:

### Fix 1 — Add human sender name (before `Đội ngũ ${branding?.displayName...}` line)

Insert after line 131 (existing "Nếu bạn không thực hiện..." line):

```html
<p style="margin-top: 24px;">
    Chị Mai từ KiteHub đây — em là người duyệt yêu cầu của <strong th:text="${orgName}">${orgName}</strong>.
    Có thắc mắc gì cứ reply email này hoặc liên hệ Zalo OA (sắp có Phase 1.5+).
</p>
```

### Fix 2 — Replace "vui lòng không trả lời" footer text

Change line 138 from:
```html
<p>Email này được gửi tự động, vui lòng không trả lời.</p>
```

To:
```html
<p>📧 Reply email này nếu cần hỗ trợ — chị Mai sẽ trả lời trong 4 giờ giờ hành chính.</p>
```

### Fix 3 — Add status page link + welcome guide link to footer

Add before line 138 closing `</div>` of `.footer`:

```html
<p style="font-size: 12px; color: #6B7280; margin-top: 8px;">
    📊 Trạng thái dịch vụ Beta: <a th:href="@{https://kitehub.me/status}">kitehub.me/status</a>
    &nbsp;|&nbsp;
    📖 Hướng dẫn: <a th:href="@{https://kitehub.me/help/anonymous}">kitehub.me/help</a>
</p>
```

---

## 4. Verdict summary

| Criterion | Status | Action |
|---|---|---|
| 1. Sender `support@kitehub.me` | ⚠️ PARTIAL | Edit footer text (Fix 2) |
| 2. Vietnamese tone natural | ✅ PASS | No action |
| 3. Tên thật người duyệt + feedback CTA | ⚠️ PARTIAL | Add human sender block (Fix 1) |
| 4. Link `/status` | ❌ FAIL | Add status link (Fix 3) |
| 5. Link `/help/p1-solo-teacher` | ❌ FAIL | Add `/help/anonymous` interim link (Fix 3) |

**Audit verdict:** ⚠️ **PARTIAL** — 1/5 fully PASS, 2/5 PARTIAL, 2/5 FAIL. Recommend 3 template edits ship trong same PR; re-run audit khi template updated.

**Decision option per `gap-done-discipline.md` §2:**

- **Option A (recommended):** Audit shipped as planning artifact → file follow-up gap GAP-586b "ship 3 template edits" Phase 1 BETA P1 trigger before Wave 86 Bucket G beta send. GAP-586 status flip OPEN → **PARTIAL** (audit complete; template fix follow-up).

- **Option B:** Skip audit-driven template fork — accept current single-template gaps → flip GAP-586 to **PARTIAL** với explicit "template gaps documented, 3 fixes deferred". Same effective outcome.

**Selected:** Option A — file follow-up gap. Audit shipped clean, template fix follow-up.

---

## 5. Mail-Tester score expectation

After 3 fixes land + Resend production verify (Wave 86 Bucket G):
- Sender domain authenticated (SPF + DKIM + DMARC per Wave 86 Bucket G) → +3 score
- Reply-friendly footer + human sender name → +1 score (lower spam classifier)
- Status link + help link footers → +1 score (legitimacy signal)
- Beta disclaimer + unsubscribe (or skip-email) → +2 score (compliance signal)
- HTML well-formed (Thymeleaf renders OK) → +1 score

**Expected score:** 8-9 / 10 (target ≥ 8 PASS).

Mail-Tester actual run defer Bucket G live send verification.

---

## 6. References

- **GAP-586:** `documents/04-quality/gaps/GAP-586-beta-invite-email-content-audit-p1-solo.md`
- **Template:** `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html`
- **Persona audit:** `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.2 cell 2.2
- **Wave 86 plan:** §3 Bucket G G-AC5
- **Rules applied:** `dev-readable-doc-language.md` (Vietnamese tone), `user-manual-content-standard.md` (link to `/help/`), `gap-done-discipline.md` §2 (partial closure)
- **Sibling:** GAP-587 (P3 Manager invite — separate template fork)
