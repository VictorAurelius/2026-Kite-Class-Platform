# Email Templates — Content Audit (Phase 1 BETA)

**Rule:** [`.claude/rules/docs-folder-structure.md`](../../../../../.claude/rules/docs-folder-structure.md)
**Audit scope:** 5 critical email types cho Phase 1 BETA flow (GAP-543, Wave 78 Bucket E).
**Created:** 2026-05-14

Audit notes cho 5 email templates customer-facing trong Phase 1 BETA. Mỗi note đánh giá 7 dimensions: tone, subject, body, CTA, footer, HTML render, plain-text fallback. Audit chỉ — không sửa content trong wave này (content rewrite = wave riêng).

---

## Directory Map

| File | Template gốc | Status sau audit |
|------|--------------|------------------|
| `welcome-audit.md` | `kitehub-email/.../templates/emails/welcome.html` | ✅ shipped, tone OK; subject thiếu cụ thể |
| `approve-tenant-audit.md` | (chưa có template) | ❌ template thiếu — file follow-up gap |
| `reset-password-audit.md` | (chưa có template) | ❌ template thiếu — file follow-up gap |
| `beta-invite-audit.md` | `kitehub-email/.../templates/emails/beta-invite.html` | ✅ shipped, tone OK; disclaimer rõ |
| `day-7-survey-audit.md` | (chưa có template, sync GAP-542) | ❌ template thiếu — file follow-up gap |

---

## File Placement Rules

- ✅ **Belongs here:** audit notes mỗi template (1 file / template); follow-up findings; tone consistency analysis.
- ❌ **Does NOT belong here:** template `.html` source (live tại `kitehub/kitehub-email/src/main/resources/templates/emails/`); template rendering logic; SMTP config (live tại `application.yml`).
- Naming: `{template-name}-audit.md`.

---

## Audit Rubric (7 dimensions per template)

1. **Tone** — friendly / formal / technical phù hợp context không.
2. **Subject line** — ≤50 chars, không PII trong subject, tiếng Việt natural.
3. **Body content** — Vietnamese tone đúng, không machine-translation awkwardness.
4. **CTA button** — text tiếng Việt, URL placeholder rõ (Thymeleaf `${var}`).
5. **Footer** — support email + /beta-status link (sync GAP-540 + GAP-539).
6. **HTML render** — preview qua Email-on-Acid hoặc Litmus (mention nếu chưa test).
7. **Plain-text fallback** — `.txt` paired với `.html` (RFC 2049 best practice).

---

## Summary Findings (5 templates)

| Template | Tone | Subject | PII leak | HTML render | Plain-text fallback | Verdict |
|----------|------|---------|----------|-------------|---------------------|---------|
| welcome | 8/10 friendly OK | ⚠️ thiếu cụ thể | ✅ no PII | ⚠️ chưa test cross-client | ❌ thiếu `.txt` | PARTIAL — content OK, plain-text gap |
| approve-tenant | N/A | N/A | N/A | N/A | N/A | ❌ TEMPLATE MISSING — file follow-up gap |
| reset-password | N/A | N/A | N/A | N/A | N/A | ❌ TEMPLATE MISSING — file follow-up gap |
| beta-invite | 9/10 friendly + disclaimer rõ | ✅ ≤50 chars | ✅ no PII | ⚠️ chưa test cross-client | ❌ thiếu `.txt` | PARTIAL — content excellent, plain-text gap |
| day-7-survey | N/A | N/A | N/A | N/A | N/A | ❌ TEMPLATE MISSING — sync GAP-542 |

**Aggregate:** 2/5 templates shipped + audit PARTIAL; 3/5 templates missing. PII leak in subject = 0 (an toàn). Plain-text fallback = 0/2 (cần bổ sung post wave 78).

---

## Follow-up gaps

- **GAP-543.1 (follow-up):** Tạo 3 template thiếu (approve-tenant, reset-password, day-7-survey) — wave 79 candidate.
- **GAP-543.2 (follow-up):** Generate `.txt` plain-text fallback cho 2 template đã ship (welcome, beta-invite) — RFC 2049 compliance.
- **GAP-543.3 (follow-up):** Cross-client HTML render test (Gmail web + Outlook web) sau khi 3 template thiếu được ship.

---

## Archive Policy

Audit notes giữ vĩnh viễn (historical record). Move to `documents/07-archived/email-templates-audits-YYYY/` khi:
- Template gốc đã được rewrite + re-audit (note cũ trở thành baseline).
- Audit > 180 ngày tuổi AND template không thay đổi (snapshot freezes).

---

## Related

- Rule: `dev-readable-doc-language.md` v1.0.1 — Vietnamese narrative + English identifier
- Rule: `pre-handoff-self-test-completeness.md` §2.3 — email-driven flow live verify
- Skill: `.claude/skills/quality/email-template-review/` — generic email review skill
- Gap: GAP-543 (parent), GAP-542 (day-7-survey scheduler), GAP-527 (E2E smoke), GAP-540 (support footer), GAP-539 (beta disclaimer)
