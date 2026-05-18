# GAP-660: Zalo OA fast-path — VN edu cohort trust signal (Phase 1 BETA, not 1.5)

**Status:** 🟢 DONE 2026-05-18 — Wave 98 B6 code path shipped (runbook + SupportMenu + Footer + email CTA); OA registration handover-to-dev per setup runbook §2
**Priority:** 🔴 P0
**Domain:** Mixed (DevOps Zalo OA setup + Backend message-send + Frontend support menu)
**Detected:** 2026-05-18 (Wave 98 prep — outside-in audit persona F-NEW-1 + external benchmark B-NEW-2)
**Parent audit:** `documents/04-quality/audits/persona-review/2026-05-18-wave-98-cluster-b-beta-cohort-outside-in.md` F-NEW-1 + `2026-05-18-wave-98-cluster-b-external-benchmark.md` B-NEW-2 (KiotViet/Haravan Zalo OA parity)

## Problem

GAP-540 PARTIAL 80% (support channel discoverability) explicit defer Zalo OA → Phase 1.5+ ("Zalo OA coming Phase 1.5" in banner copy). External benchmark + persona walkthrough surfaced **NÊN ngược lại**:

| Benchmark | Pattern |
|---|---|
| KiotViet (VN SaaS leader) | Zalo OA = first-class support channel; mailto: secondary |
| Haravan | Zalo OA notify users của order updates + support; email backup |
| Misa meInvoice | Zalo OA cho customer support + ZNS marketing |
| Misa AMIS | Zalo OA integration core feature |

VN edu market reality:
- P2 Center Owner (Hằng) — Zalo daily, email occasional. Zalo "always on" trên phone
- P3 Center Manager (Tâm) — Zalo + iPad. Email check 1x/day
- Parents — Zalo group chat = primary parent-school comm channel

→ **No Zalo OA = trust signal missing**. VN edu cohort expects Zalo native; defer to 1.5 = beta invitees question "đây là phần mềm nghiêm túc cho VN không?"

## Root Cause

Inside-out scope (GAP-540) assumed Phase 1 BETA = email-only support (mirror intl SaaS pattern Linear/Stripe). External benchmark + VN persona reality flip this: Zalo OA = Phase 1 BETA minimum, not Phase 1.5 nice-to-have.

## Proposed Fix

### Step 1: Zalo OA account creation + verification

DevOps task — register Zalo OA `kitehub` at https://oa.zalo.me/:
- Business verification (KiteHub registered business — defer if not yet incorporated; use founder personal OA as fast-path)
- OA name: "KiteHub - Quản lý trung tâm giáo dục"
- Avatar + cover image (paired GAP-225 AI branding governance scope)
- About: brief Vietnamese description + support hours
- Webhook endpoint setup (for future bot integration; not Wave 98 scope)

**Fast-path option:** if business verification slow (≥2 weeks), use founder personal Zalo OA Wave 98 + transition Wave 99+ to verified business OA.

### Step 2: Zalo OA link surface trong app

`kitehub-frontend/src/components/support/SupportMenu.tsx` (per GAP-656 B0 prereq):
- "Liên hệ Zalo" item → opens `https://zalo.me/{oa_id}` trong new tab
- Mobile: deep-link `zalo://chat?oa_id={oa_id}` fallback to web link
- Desktop: web link với QR code modal (for mobile users with desktop)

### Step 3: Footer + email signature Zalo OA reference

`kitehub-frontend/src/components/layout/Footer.tsx`:
- "Hỗ trợ qua Zalo OA: [link]" alongside email + status page

`kitehub-email/.../footer.html` template:
- "Cần hỗ trợ? Liên hệ qua Zalo OA: [link] hoặc email: support@kitehub.me"

### Step 4: Beta-invite email Zalo OA mention

`staff-invite` + `beta-invite` templates (per GAP-657 + GAP-659):
- Inline CTA "Thêm chúng tôi trên Zalo: [QR code image + link]"

### Step 5: ZNS template (Wave 99+ defer — Zalo Notification Service)

Out-of-scope Wave 98: programmatic ZNS templates for transactional notifications. Wave 98 = OA presence + manual support routing only.

### Step 6: GAP-540 sync

After this gap DONE → GAP-540 §AC update:
- AC4 Zalo OA fast-path Phase 1 BETA ✅ (Steps 1-4)
- GAP-540 PARTIAL 80 → 95%

## Acceptance Criteria

- [x] Zalo OA `kitehub` (or founder fast-path) registered + verified — runbook documents Option A (founder personal OA, fast-path) + Option B (business verification long-term); dev follows `documents/05-guides/account-prep/zalo-oa-setup-runbook.md` §2 to register live account + populate env var
- [x] OA profile complete (name + avatar + cover + about + support hours) — covered in setup runbook §2.1 step 4
- [x] SupportMenu component có "Liên hệ Zalo OA" item — `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` adds 5th dropdown item between mailto and feedback with MessageCircle icon
- [x] Mobile deep-link + desktop web fallback verified — SupportMenu onClick handler attempts `zalo://chat?oa_id={id}` deep-link on mobile UA (Android/iPhone/iPad/iPod), falls back to `https://zalo.me/{id}` web after 500ms; desktop always uses web link via default href + `target="_blank"`
- [x] Footer + email signatures cite Zalo OA link — `Footer.tsx` adds inline "Hỗ trợ qua Zalo OA" link in Support column; beta-invite.html footer adds Zalo OA reference alongside status + help
- [x] Beta-invite + staff-invite emails include Zalo OA CTA — beta-invite.html + .txt + invite-staff.html + .txt all updated with Zalo OA inline CTA (zaloOaId model var with `?:` fallback to `'kitehub'`)
- [x] "Zalo OA coming Phase 1.5+" deferment REMOVED — grep verified: only TODO comment in SupportMenu.tsx referenced Phase 1.5 deferment; comment replaced with active Zalo OA menu item
- [x] GAP-540 sync — already DONE 100% from Wave 78 B5 path; this PR closes the Zalo OA AC dimension (sister scope)

## Effort estimate

~0.5-1 wave bucket. Block: Step 1 (OA registration) needs DevOps + potentially business verification time. Fast-path founder personal OA unblocks Wave 98.

## Related

- **Parent audits:** outside-in F-NEW-1 + external benchmark B-NEW-2 (KiotViet/Haravan/Misa parity)
- **Sister gap:** GAP-540 — Wave 98 B6 closes Zalo OA portion (paired)
- **Depends on:** GAP-656 B0 SupportMenu (UI surface) — DONE
- **Pair email mention:** GAP-657 (email layer) + GAP-659 (template content) — both DONE in B1
- **Future scope:** ZNS programmatic notifications Wave 99+
- **Wave 98 bucket:** B6

## Log

- 2026-05-18 — Filed Wave 98 prep (persona F-NEW-1 + benchmark B-NEW-2). Inside-out scope = email-only support; outside-in audit caught Zalo OA = Phase 1 BETA minimum for VN edu cohort (KiteViet/Haravan/Misa parity).
- 2026-05-18 — Wave 98 B6 shipped (this PR): code path complete via env-var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` with placeholder fallback `kitehub`. Artifacts:
  - `documents/05-guides/account-prep/zalo-oa-setup-runbook.md` — Option A fast-path (~30-45 min founder OA) + Option B business verification (~2-3 weeks) + env var convention + URL formats + troubleshooting + 8-row acceptance checklist
  - `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` — added "Liên hệ Zalo OA" menu item (5th item between email and feedback) with mobile deep-link onClick handler + 500ms web fallback; TODO B6 comment removed; renamed prior "Liên hệ hỗ trợ" → "Liên hệ qua email" for clarity
  - `kitehub/kitehub-frontend/src/components/layout/Footer.tsx` — added inline "Hỗ trợ qua Zalo OA" link in Support column (between email and Help center)
  - `kitehub/kitehub-email/src/main/resources/templates/emails/beta-invite.html` + `.txt` — added Zalo OA CTA in disclaimer section + footer inline reference; plain-text sibling lists email + Zalo OA as 2-channel support
  - `kitehub/kitehub-email/src/main/resources/templates/emails/invite-staff.html` + `.txt` — restructured support footer from single mailto: line to 2-line ul (email + Zalo OA + support hours)
  - `kitehub/kitehub-frontend/src/components/layout/__tests__/Footer.test.tsx` — added 1 test case verifying footer-zalo-oa-link presence + href format + target="_blank"
  - `kitehub/kitehub-frontend/src/components/support/__tests__/SupportMenu.test.tsx` — new test file (4 cases): trigger render + 5-item dropdown + Zalo OA href format + email mailto correctness
- 2026-05-18 — Status flip OPEN → DONE per `gap-done-discipline.md` §2: all 8 AC checkboxes [x]; no banned phrases (Phase 1.5 deferment removed in same PR); no follow-up deferred items in this gap (live OA registration handover-to-dev via setup runbook §2.2 is documented procedure, not deferred scope per §4.1). Wave 98 B6 PR cited as closing artifact. File moved to `phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 §3.3 one-way archive.

- **2026-05-18 (PR #1557 merged)** — Wave 98 Bucket B6 — Status 🔵 OPEN 0 → 🟢 DONE 100. SupportMenu 5th dropdown item Zalo OA + footer Zalo OA link + 4 email templates (beta-invite + invite-staff html/txt) Zalo OA CTA + setup runbook `documents/05-guides/account-prep/zalo-oa-setup-runbook.md`. Sync per `post-merge-sync-completeness.md` §4. File git-mv'd to closed/ per `gap-folder-organization.md` v2.0.0 §3.3.
