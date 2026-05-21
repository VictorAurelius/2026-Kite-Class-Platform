---
gap_id: GAP-700
title: Email Architecture doc refresh post Wave 98 — ResendEmailService dormant→shipped + KiteClass routing gap call-out
status: OPEN
priority: P3
domain: Docs
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-21
last_updated: 2026-05-21
filed_by: Wave 102.9 session (post-Bucket-E user question on local Resend + KiteClass routing)
---

# GAP-700 — Email Architecture doc refresh post Wave 98

## Problem

`documents/02-architecture/email-architecture.md` (last-reviewed 2026-05-19) chứa 2 claim **STALE** so với code thực tế hôm nay:

1. **§2 "Current code wiring" row "ResendEmailService.java"** ghi `❌ KHÔNG tồn tại trong code` — nhưng file đã tồn tại từ Wave 98 Bucket B1 (GAP-657):
   - Path: `kitehub/kitehub-email/src/main/java/com/kitehub/email/service/ResendEmailService.java:43`
   - Implements `NotificationChannel`, `@ConditionalOnProperty(name = "email.provider", havingValue = "resend")`
   - Wave 98 stub — wires payload shape + HTTP POST tới `https://api.resend.com/emails` + fallback MOCK khi `resend.api-key` blank

2. **§3 Mermaid flowchart node "Resend NOT IN CODE YET"** — same stale claim; cần update sang "Resend wired Wave 98 GAP-657 stub; activates on `email.provider=resend` + `RESEND_API_KEY` set"

3. **§2 row "Resend domain DKIM/SPF in Cloudflare DNS"** ghi `⚠️ status chưa verify wired end-to-end` — cần verify trạng thái hiện tại (post-Wave-81 provisioning + post-AWS-restore Wave 102.x)

4. **MISSING coverage:** doc KHÔNG nhắc đến **KiteClass cross-product email routing gap** — `kiteclass-core/EmailService` interface có 5 method (sendSimpleEmail/sendHtmlEmail/sendTemplateEmail/sendContactNotification/sendLeadConfirmation) nhưng implementation duy nhất là `LoggingEmailService` (log-only, KHÔNG gọi sang kitehub-email service). Cross-product email path = silent gap. Tracked separately ở [[GAP-701]] nhưng doc cần cross-reference để future reader thấy.

## Root Cause

Wave 98 ship ResendEmailService nhưng KHÔNG đồng bộ `email-architecture.md` doc (per `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — code shipped + doc stale). Wave 99-102.8 series có docs-sync sweeps NHƯNG không catch email-architecture.md vì doc nằm trong `documents/02-architecture/` (out-of-scope của wave plan sync targets thường là gap-status.csv + ROADMAP + wave-history + session-handoff).

## Proposed Fix

Single docs-only PR refresh `documents/02-architecture/email-architecture.md`:

1. **§2 Current code wiring table** — flip 4 rows:
   - `ResendEmailService.java`: `❌ KHÔNG tồn tại` → `✅ Stub wired Wave 98 GAP-657 (HTTP POST api.resend.com + MOCK fallback khi api-key blank)`
   - `RESEND_API_KEY secret`: cite current state post-AWS-restore (cần verify với `aws secretsmanager describe-secret`)
   - `Resend domain DKIM/SPF`: cite current state (CF DNS records) — verify với `documents/05-guides/deploy/resend-provisioning-runbook.md` last-reviewed
   - `email.provider=resend override`: từ "runtime fail" → "stub works (MOCK fallback) + full SDK integration deferred Phase 1.5+"

2. **§3 Mermaid diagram** — update Resend node label từ `❌ NOT IN CODE YET` → `✅ Stub Wave 98 (HTTP api.resend.com)`; update Channel branch label

3. **NEW §4 "KiteClass cross-product email routing gap"** — section mới mô tả:
   - kiteclass-core có EmailService interface + LoggingEmailService (log-only)
   - KHÔNG có HTTP client gọi sang kitehub-email
   - `EMAIL_SERVICE_URL` env wired CHỈ cho kitehub-subscription (compose line 561), KHÔNG cho kiteclass-core
   - Tracked tại [[GAP-701]] cho Phase 1.5+ scope
   - Diagram phụ (Mermaid flowchart) show current state + future state với HTTP client

4. **§"Last Updated" + frontmatter `last-reviewed`** flip sang 2026-05-21

5. **Add §"Local dev: bật Resend" recipe** — `EMAIL_PROVIDER=resend` + `RESEND_API_KEY=re_xxx` qua `.env` + restart kitehub-email; document MOCK fallback khi api-key blank

## Acceptance Criteria

- [ ] `documents/02-architecture/email-architecture.md` §2 table — 4 rows flipped match code reality 2026-05-21
- [ ] §3 Mermaid diagram Resend node label updated (no longer "NOT IN CODE YET")
- [ ] NEW §4 KiteClass cross-product routing gap section added + cross-link [[GAP-701]]
- [ ] §"Last Updated" + frontmatter `last-reviewed` = 2026-05-21
- [ ] NEW §"Local dev: bật Resend" recipe section added
- [ ] Per `diagram-format-selection.md`: diagram giữ Mermaid format (NOT switch to PlantUML); fix Mermaid only, don't reformat
- [ ] Per `dev-readable-doc-language.md`: narrative Vietnamese, English identifier giữ nguyên
- [ ] Per `vn-localization-audit-checklist.md`: N/A (architecture doc — dev-facing, không tenant-facing)

## Wave target

**Recommended: docs-only PR THIS session (auto-merge per `docs-only-pr-auto-merge.md`)** — scope nhỏ (~1 file, ~30 line edit + 1 diagram update + 2 new sections); diff = `documents/02-architecture/**` thuần; auto-merge eligible. Có thể batch chung wave 102.9 closure PR hoặc ship riêng.

**Alternative: defer Wave 102.10** nếu wave 102.9 đã quá tải — gắn vào Wave 102.10 D bucket "email content + hardening" extension.

## Related

- Rule: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync (origin pattern — Wave 98 code shipped, doc stale)
- Rule: `post-merge-sync-completeness.md` §2 4-target sync (architecture docs NOT in 4-target list — gap class)
- Wave 98 Bucket B1: GAP-657 ResendEmailService stub ship
- [[GAP-701]]: KiteClass cross-product email HTTP integration (sister gap, paired session)
- Doc: `documents/02-architecture/email-architecture.md`
- Code: `kitehub/kitehub-email/src/main/java/com/kitehub/email/service/ResendEmailService.java:43`
- Code: `kitehub/kitehub-email/src/main/resources/application.yml:29`
- Code: `kitehub/docker-compose.kitehub.yml:469` (EMAIL_PROVIDER default)

## Log

- **2026-05-21 (Wave 102.9 session):** Gap created. Triggered bởi user question "ở local thì có gửi mail bằng resend được không, có routing được cho kiteclass không?" — investigation surfaced doc stale (4 claims outdated post-Wave-98). Filed in cùng session với sister gap [[GAP-701]] (kiteclass-core email wire). Status OPEN; fix scope ~1 file + 1 diagram + 2 new sections; docs-only PR auto-merge eligible. P3 priority — không functional blocking, chỉ docs hygiene blocks future reader from acting on stale info.
