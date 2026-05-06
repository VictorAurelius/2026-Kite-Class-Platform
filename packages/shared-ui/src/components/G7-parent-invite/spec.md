# G7 ParentInvite — production spec (port from HTML proto)

**Source HTML proto:** `documents/02-architecture/design-system/ui_kits/components/G7-parent-invite/`
**Wave:** Wave 27 Bucket D (GAP-273 PARTIAL — 1/12 G* components)
**Persona:** Pa. Parent (recipient) + Admin (sender — this component)

---

## Scope of THIS port (sender variant only)

Phase 2 of GAP-273 ports component primitives. The HTML proto for G7 covers
both **sender admin** and **redeemer parent landing**. This port ships the
**sender variant** as a pure React component that any tenant admin surface
can embed:

- Email input (with inline regex validation)
- Channel toggle: ZALO_OA (default per `dossier/02` §5) ↔ EMAIL
- Send button with idle / sending / sent / error lifecycle
- Issued-token display + copy-to-clipboard fallback for out-of-band sharing
- Vietnamese-first labels per spec (`bạn` informal, `phuhuynh@gmail.com` placeholder)

The redeemer landing page (states `redemption-link.html`, `expired.html`,
`already-redeemed.html`, `success.html`) is intentionally OUT of this port —
those are page-level surfaces consumed by KC's `/parent-invite/[token]` route
and are tracked as Phase 3 in GAP-273. Pending invites list (sender) is also
deferred — this port surfaces the *single send* primitive that page wraps.

## State machine

```
idle ──[click send + valid email]──> sending
sending ──[onSend resolves]──> sent
sending ──[onSend rejects]──> error
error ──[edit + click send]──> sending
```

`idle` resumes from `error` automatically when the user edits the email so the
next click cleanly re-enters `sending`.

## Public API

See `types.ts`:

| Type | Purpose |
|------|---------|
| `InviteChannel` | `'EMAIL' \| 'ZALO_OA'` |
| `InviteState` | `'idle' \| 'sending' \| 'sent' \| 'error'` |
| `EmailValidationResult` | `{ ok: true } \| { ok: false; message } \| null` |
| `ParentInviteProps` | Component props (see types.ts for fields) |

`validateEmail(value)` is exported as a pure helper so callers can drive
their own forms with the same regex (`/^[^\s@]+@[^\s@]+\.[^\s@]+$/`).

## Channel default

`defaultChannel = 'ZALO_OA'` — Zalo OA reaches ~95% of VN parents per
`dossier/02-vietnamese-ux-musts.md` §5. Admins can toggle to EMAIL for the
~5% who don't have Zalo.

The Zalo radio dot uses Zalo brand blue (`#0068FF`) as a visual cue but the
primary CTA remains theme-coloured to avoid double-branding.

## Token copy fallback

`navigator.clipboard.writeText` is the happy path. When unavailable (older
browsers, http://, sandboxed iframes) the component selects the `<code>` block
contents so the user can press Ctrl+C manually. We do not throw — copy is a
nice-to-have UX affordance, the primary delivery is whichever channel was
selected.

## Accessibility

- Email `<input>` has explicit `<label htmlFor>` (not placeholder-only).
- Channel toggle: `<fieldset role="radiogroup" aria-labelledby>` containing two
  semantic `<input type="radio">` controls.
- Validation error: `aria-describedby` link from input + visible message.
- Sending state: `role="status"` + `aria-live="polite"`.
- Error state: `role="alert"` + `aria-live="assertive"` (drawing attention).
- Success panel: `role="status"` + `aria-live="polite"`.
- Copy button: `aria-label="Sao chép mã mời"` + dynamic "Đã sao chép" feedback.

## Dependencies

- React 18+ / 19+ (peerDependency, declared in `packages/shared-ui/package.json`).
- No new npm dependencies — built-in regex for email validation; inline SVG icons.
- Tailwind theme tokens (`bg-background`, `text-foreground`, `bg-primary`,
  `text-destructive`, etc.) — already covered by `kite-shared-tokens.css`.

## Testing

`__tests__/ParentInvite.test.tsx` covers:

1. Email validation pure logic (6 cases — empty / no-`@` / no-domain / whitespace / valid / plus-aliasing).
2. Idle render (input + radio-group + send button disabled).
3. Invalid email → error message + send disabled.
4. Channel toggle ZALO_OA ↔ EMAIL.
5. Send → sending → sent state with token render.
6. Copy-to-clipboard invokes `navigator.clipboard.writeText`.
7. Error state with `role="alert"`.
8. Vietnamese-first labels (informal `bạn`, real-feel placeholder).

## Out-of-scope (track separately under GAP-273)

| Feature | Where it lives |
|---------|----------------|
| Pending invites list (sender) | Phase 3 — full sender admin page |
| Redemption landing (`redemption-link.html`) | Phase 3 — KC `/parent-invite/[token]` page |
| Expired / already-redeemed / success states (redeem side) | Phase 3 — page-level |
| Zalo OA card preview (320×100 visual) | Phase 4 — design-system asset, not component primitive |
| 24h countdown ticker | Phase 3 — page-level (component issues a token, doesn't track expiry) |
