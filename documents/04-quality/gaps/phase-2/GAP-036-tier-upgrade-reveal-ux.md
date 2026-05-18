# GAP-036: Tier Upgrade UX (Reveal, Teaser, Unlock)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Product / Conversion
**Detected:** 2026-04-14 (simulation: Owner × Evolution × C9 Commercial)

## Problem

Khi tenant upgrade tier (Free→Pro, Pro→Premium), **không có celebration UX** reveal benefits:
- Premium templates locked → không show they exist (không tạo desire)
- Upgraded → không có "welcome to premium" experience
- Features unlocked: không highlighted
- Không teaser content để drive upgrade

Khác với GAP-017 (billing integration — functional) — đây là **conversion UX**.

## Proposed Fix

### 1. Teaser Premium Templates

```tsx
<TemplatePicker>
  {freeTemplates.map(t => <TemplateCard t={t} />)}

  <LockedTemplateCard>
    <img src={premiumTemplate.preview} className="blur" />
    <Lock icon />
    <Overlay>
      <Badge>PREMIUM</Badge>
      <p>Mở khóa 20+ templates đẹp hơn</p>
      <Button>Upgrade to PRO</Button>
    </Overlay>
  </LockedTemplateCard>
</TemplatePicker>
```

### 2. Feature Reveal on Upgrade

Sau khi upgrade successful:

```
Welcome to PRO! 🎉
┌─────────────────────────────────────┐
│ Bạn vừa mở khóa:                    │
│ ✓ 10 AI generations/day (was 3)    │
│ ✓ 20+ premium templates             │
│ ✓ Regenerate 10x/session            │
│ ✓ Priority queue (faster)           │
│                                     │
│ [Try new features now]              │
└─────────────────────────────────────┘
```

### 3. Tier Comparison Page

```
/pricing
Table with all features + checkmarks per tier
- "Currently on: FREE"
- [Upgrade to PRO] CTAs
```

### 4. In-app Upgrade Hints

Contextual nudges:
- Hit quota: "Upgrade PRO for 10x more"
- See locked template: "Unlock with PREMIUM"
- Regenerate limit: "Enterprise = unlimited"

Don't be spammy — max 1 nudge per session.

### 5. Downgrade Experience

Khi downgrade Premium → Pro:
- Show what's being lost
- Grace period to export premium-only assets
- Confirm understand consequences

### 6. Post-Upgrade Quality Boost

Re-run branding với higher-tier quality:
- "Would you like to regenerate with premium AI? (free this time)"
- Leverage new quota allocation

## Acceptance Criteria

- [ ] Locked template cards với blur + upgrade CTA
- [ ] Post-upgrade welcome modal với feature reveal
- [ ] Pricing table page với comparison
- [ ] Contextual nudges (rate-limited)
- [ ] Downgrade flow với grace period
- [ ] Post-upgrade quality boost option
- [ ] A/B test: reveal vs hide locked → conversion rate

## Dependencies

- GAP-017 (billing integration) — technical foundation
- GAP-026 (trial mechanics) — trial → paid upgrade

## Log

- 2026-04-14 — Conversion UX gap discovered
