# GAP-660: Zalo OA fast-path — VN edu cohort trust signal (Phase 1 BETA, not 1.5)

**Status:** 🔵 OPEN
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

- [ ] Zalo OA `kitehub` (or founder fast-path) registered + verified
- [ ] OA profile complete (name + avatar + cover + about + support hours)
- [ ] SupportMenu component có "Liên hệ Zalo" item (depends GAP-656 B0)
- [ ] Mobile deep-link + desktop QR fallback verified trên both platforms
- [ ] Footer + email signatures cite Zalo OA link
- [ ] Beta-invite + staff-invite emails include Zalo OA CTA
- [ ] Banner copy "Zalo OA coming Phase 1.5+" REMOVED (now active)
- [ ] GAP-540 PARTIAL 80 → 95% updated

## Effort estimate

~0.5-1 wave bucket. Block: Step 1 (OA registration) needs DevOps + potentially business verification time. Fast-path founder personal OA unblocks Wave 98.

## Related

- **Parent audits:** outside-in F-NEW-1 + external benchmark B-NEW-2 (KiotViet/Haravan/Misa parity)
- **Sister gap:** GAP-540 PARTIAL 80% — this gap closes Zalo OA portion
- **Depends on:** GAP-656 B0 SupportMenu (UI surface)
- **Pair email mention:** GAP-657 (email layer) + GAP-659 (template content)
- **Future scope:** ZNS programmatic notifications Wave 99+
- **Wave 98 bucket:** B6
