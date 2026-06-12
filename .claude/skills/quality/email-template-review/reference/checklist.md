# Email Template Review Checklist

Full 40-point checklist. Tick every item per template. Any FAIL in a MUST row blocks merge/send.

**Legend:** MUST = blocker; SHOULD = fix or file gap; NICE = opportunistic.

---

## 1. Brand & Visual (8 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 1.1 | Logo present in header (absolute HTTPS URL) | MUST | grep `th:src="${branding.logoUrl}"` + curl URL returns 200 |
| 1.2 | Primary + secondary brand colors applied | MUST | CSS uses `var(--brand-primary)` / `var(--brand-secondary)`; no hardcoded hex that overrides tenant theme |
| 1.3 | Fallback colors for branding null | MUST | Every `${branding?.X ?: '#default'}` has a sensible default for system/un-branded tenants |
| 1.4 | Font family web-safe stack | MUST | `font-family: Arial, sans-serif` or similar — custom webfonts fail in most clients |
| 1.5 | Display name matches tenant | MUST | `${branding?.displayName ?: 'KiteClass'}` used in title + sign-off |
| 1.6 | Contrast ratio ≥ 4.5:1 (body text) / ≥ 3:1 (large/CTA text) | SHOULD | WebAIM contrast checker on final rendered HTML |
| 1.7 | Hero image has alt text | SHOULD | Every `<img>` has `alt="..."` (accessibility + images-off fallback) |
| 1.8 | Max width 600px for desktop | MUST | `<body>` or wrapper has `max-width: 600px` |

## 2. Legal & Compliance (9 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 2.1 | Unsubscribe link (marketing/lifecycle emails) | MUST | `<a href="${unsubscribeUrl}">Hủy đăng ký / Unsubscribe</a>` in footer; URL must be 1-click per CAN-SPAM |
| 2.2 | `List-Unsubscribe` header in SMTP send | MUST | Check `EmailService` / `NotificationService` sends `List-Unsubscribe: <mailto:unsub@...>, <https://...>` header |
| 2.3 | Physical company address in footer | MUST | CAN-SPAM + Vietnam advertising law both require identifiable sender with postal address |
| 2.4 | Company legal name + business registration # | MUST | "Công ty TNHH ... — MSDN XXXXXXXXXX" — tenant may use their own, platform defaults to KiteClass parent co. |
| 2.5 | Privacy policy link | MUST | Footer link to current TOS/privacy version — version must match tenant's agreed version (not always latest) |
| 2.6 | `[QC]` prefix in subject for promotional emails | MUST (Vietnam) | Nghị định 91/2020 §11 — required for quảng cáo emails; transactional exempt |
| 2.7 | GDPR legal basis statement (if recipient in EU) | SHOULD | Footer: "You are receiving this because {legal basis: consent / contract / legitimate interest}" |
| 2.8 | Data retention notice (if applicable) | SHOULD | Trial-expired / data-retention-warning emails explicitly state deletion timeline + how to export |
| 2.9 | Copyright year current | NICE | `© 2026 KiteClass` — bump yearly |

## 3. Internationalization (5 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 3.1 | Vietnamese copy native-speaker reviewed | MUST | PR tagged with `lang-review-vn` label; reviewer not the original author |
| 3.2 | English fallback for non-VN users | MUST | Template accepts `${locale}` or ships `_en.html` variant; `EmailService` selects by tenant/user locale |
| 3.3 | No broken Vietnamese diacritics | MUST | UTF-8 rendering verified (no `????`, no `Ch??o` artifacts) — check SMTP Content-Transfer-Encoding = `quoted-printable` or `base64` |
| 3.4 | Date + currency formatted per locale | SHOULD | `${#dates.format(expiry, 'dd/MM/yyyy')}` for VN; `MM/dd/yyyy` for EN |
| 3.5 | No hardcoded English strings mixed into VN copy | SHOULD | grep for stray untranslated CTA labels like "Click here" |

## 4. Variables & Data (6 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 4.1 | Render preview with sample data | MUST | Use `reference/sample-data.md` blocks — every variable resolves, no `${...}` literal in final HTML |
| 4.2 | No PII leakage between tenants | MUST | Use `${tenantContext.displayName}` not `${currentUser.tenant.name}`; no shared static tenant refs |
| 4.3 | Null-safe on optional fields | MUST | Every `${branding.X}` has `?:` default; every `${user.phone}` guarded by `th:if` |
| 4.4 | XSS protection on user-supplied values | MUST | Use `th:text` (escaped); AVOID `th:utext` unless value is trusted system-generated HTML |
| 4.5 | URLs are absolute + HTTPS | MUST | `${loginUrl}`, `${unsubscribeUrl}`, `${branding.logoUrl}` all resolve to `https://...` — relative URLs break in Gmail/Outlook |
| 4.6 | Tenant-scoped URLs (no cross-tenant leak) | MUST | `${loginUrl}` = `https://{tenant-subdomain}.kitehub.me/login`, not a generic URL — or it exposes admin panel to wrong tenant |

## 5. Mobile & Responsive (5 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 5.1 | Renders at 320px width without horizontal scroll | MUST | Resize browser to 320px, verify layout |
| 5.2 | CTA button ≥ 44×44px tap target | MUST | Padding 12×30 is OK at desktop; verify mobile doesn't shrink below 44px |
| 5.3 | Font ≥ 14px body, ≥ 16px CTA | MUST | iOS Mail auto-enlarges below 14px which breaks layout |
| 5.4 | Single-column layout (no multi-column tables) | MUST | Outlook mobile strips media queries — single column safest |
| 5.5 | Dark-mode tested in Gmail iOS + Outlook | SHOULD | Headers + CTA still legible after auto-inversion; consider `<meta name="color-scheme" content="light only">` |

## 6. Client Compatibility (4 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 6.1 | Inline CSS applied (Premailer or equivalent) | MUST | `<style>` blocks work in Gmail web but fail in Outlook 2016+; run inliner before send |
| 6.2 | No unsupported CSS (grid, flex, advanced selectors) | MUST | Stick to tables + inline styles; no `display: grid`, no `:has()`, no custom props in Outlook |
| 6.3 | Images hosted on stable CDN (not inline base64 >100KB) | SHOULD | Gmail clips emails >102KB; large inline images kill deliverability |
| 6.4 | Tested in ≥ 3 clients (Gmail web, Gmail mobile, Outlook web) | MUST | Litmus/Email-on-Acid if available; else manual capture |

## 7. Tenant Isolation (3 items)

| # | Item | Level | How to check |
|---|------|-------|--------------|
| 7.1 | Template renders with branding=null (system email) | MUST | Defaults kick in, no `${branding.X}` raw text in HTML |
| 7.2 | Template renders with full tenant branding | MUST | All brand colors, logo, display name, contact email apply |
| 7.3 | `contactEmail` is per-tenant (not hardcoded) | MUST | `${branding?.contactEmail ?: 'support@kitehub.me'}` pattern — tenant's own support email used when set |

---

## Summary row per email

Add to PR comment after review:

```
Email: <template-name>.html
Class: transactional | lifecycle | marketing
Checklist score: XX/40 (MUST failures: 0)
Sign-offs: eng=@name, brand=@name, legal=@name
Preview: <link to rendered HTML or screenshot>
Clients tested: Gmail-web ✅ Gmail-iOS ✅ Outlook-web ✅
Status: PASS | FAIL (list failing items)
```

---

## Known current violations (as of 2026-04-20 audit)

For retrofit audit of existing 16 templates, expect these to fail:
- 2.1 Unsubscribe link (0/16 templates have it)
- 2.3 Physical address (0/16)
- 2.4 Legal entity name (0/16)
- 2.5 Privacy policy link (0/16)
- 2.6 `[QC]` prefix check (subject-level, not template)
- 3.2 EN fallback (0/16 have `_en.html` variant)
- 6.1 Inline CSS (uses `<style th:inline>` — works but fragile in Outlook)

File results in follow-up gap (do not block this skill's creation PR since skill is docs-only).
