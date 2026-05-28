---
audience: dev
---

# GAP-800 — Email HTML MIME part serves plain-text content (TEXT resolver greedy-resolves suffix-less HTML calls)

**Status:** 🟢 DONE (2026-05-28, PR #1955 — 1-line resolver fix + live re-walk verified)
**Priority:** 🟠 P1
**Domain:** Backend / Email (kitehub-email template rendering)
**Found:** 2026-05-28 (Wave A 5-flow RST walk — Flow 1 email render, user browser-flagged "vẫn lỗi html")
**Affects:** EVERY templated email with both `.html` + `.txt` siblings — the HTML MIME part ships `.txt` content (no `<html>`/`<a href>` markup, non-clickable links). Systemic, not beta-invite-only.

## Problem

The beta-invite email's `text/html` MIME part renders the **plain-text (`.txt`) template content** instead of the styled `beta-invite.html`. In MailHog the html body is prose ("Kính gửi anh/chị... Liên kết đăng ký: https://..."), no HTML tags, link is plain text (not `<a href>`) → not clickable in HTML mail clients.

Distinct from:
- **GAP-797** (var-name drift — DONE): code `804832` + signup link now DO render (var-name reconciled). This gap is the REMAINING html-structure issue.
- **GAP-799** (cross-tenant uniqueness — unrelated).

Empirical (MailHog `walk-owner-001@test.local`, 2026-05-28): `text/html` part = `beta-invite.txt` content (greeting "Kính gửi anh/chị"), while `beta-invite.html` greeting is "Xin chào `<strong>`". The html template is correct styled HTML (DOCTYPE, 22 tags) — it's simply not being used for the html part.

## Root Cause

`EmailTemplateResolverConfig.java:44-54` registers a TEXT `ClassLoaderTemplateResolver` (`suffix=.txt`, `TemplateMode.TEXT`, `checkExistence=true`, `order=50`) **without `resolvablePatterns`**.

`EmailTemplateRenderer.renderHtmlWithFallback` renders HTML via `templateEngine.process("emails/beta-invite")` (suffix-less). With `checkExistence=true` and no `resolvablePatterns`, the TEXT resolver greedily appends `.txt` → `templates/emails/beta-invite.txt` exists → resolves in TEXT mode → returns `.txt` content as the "HTML" body. The intended HTML resolver never wins for the suffix-less call.

## Fix (1-line, shipped this PR)

`EmailTemplateResolverConfig`: `resolver.setResolvablePatterns(java.util.Set.of("*.txt"));` — confines the TEXT resolver to template names explicitly ending in `.txt`. Suffix-less HTML render calls (`emails/beta-invite`) fall through to the HTML resolver → render `.html`. Explicit `.txt` calls (`emails/beta-invite.txt`) still hit the TEXT resolver.

## Acceptance Criteria

- [x] TEXT resolver scoped to `*.txt` via `resolvablePatterns`
- [x] Re-send beta-invite → MailHog `text/html` part contains `<html>` + `<a href>` (real styled HTML, clickable link)
- [x] Plain-text `text/plain` part still renders correctly (`.txt` content — was always rendering; code present)
- [x] Systemic: fix is config-level (single resolver, no per-template branch) → uniform across all dual-sibling templates; beta-invite verified empirically as representative case

## Walk evidence (live re-walk per pre-handoff-self-test-completeness.md §3, 2026-05-28)

Rebuilt kitehub-email with fix; fresh signup→approve → beta-invite email to `gap800-verify-175121@test.local`, inspected MailHog `text/html` part:

| Signal | Before fix | After fix |
|---|---|---|
| html body length | 1312 chars | **7086 chars** |
| `<html>` tag | ❌ absent | ✅ present |
| `<a href>` clickable link | ❌ absent (plain-text URL) | ✅ `https://kitehub.me/signup/beta?code=121827` + mailto + Zalo |
| `<style>`/`<h1>` styled markup | ❌ absent (prose) | ✅ present |
| 6-digit code rendered | ✅ (GAP-797) | ✅ |

HTML email now renders styled HTML with clickable signup link. text/plain part unaffected.

## Related

- **GAP-797** (var-name drift — DONE; code/link now render)
- **GAP-702** (beta-invite delivery wiring — DONE)
- Wave A 5-flow walk `documents/04-quality/audits/rst-html/2026-05-28-wave-a-5-flow-walk.md` Flow 1
- `feature-ship-runtime-walk-mandate.md` (RST walk surfaced what audits + var-name fix missed)

## Log

- **2026-05-28:** Filed from Wave A 5-flow walk Flow 1. User browser-flagged "vẫn lỗi html" on beta-invite email. Investigation: html MIME part = `.txt` content; root cause = TEXT resolver missing `resolvablePatterns` → intercepts suffix-less HTML render calls. Systemic (all dual-sibling templates). 1-line fix `setResolvablePatterns("*.txt")`.
