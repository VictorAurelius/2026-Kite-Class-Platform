# Branding Wizard — API Contract

## Frontend SPI

### `BrandingWizard` component

```tsx
<BrandingWizard
  tier="PRO"            // Tier enum from session
  tenantId="t-abc"
  slug="my-school"
/>
```

Mounts at route `/(dashboard)/branding/wizard`.

### `useBrandingWizard(tier, tenantId, slug)` hook

Returns:
```ts
{ state: WizardState; send: (e: WizardEvent) => void; submit: () => Promise<void> }
```

### Pure reducer

```ts
reducer(state: WizardState, event: WizardEvent): WizardState
```

100% pure; unit-tested via `__tests__/wizard-machine.test.ts`.

## Backend calls

The wizard submit hits Wave 3 Sub-PR 3.4 endpoints:

| Step | Method | Path | Body |
|------|--------|------|------|
| submit wizard | POST | /api/v1/instances | `{tenantId, slug, inputs}` |
| fetch status | GET | /api/v1/instances/{id} | — |

Live progress streaming via SSE endpoint (scaffold; concrete endpoint wired in follow-up).

## Types

See `src/components/branding/wizard/types.ts` — complete discriminated-union for
`WizardState`, `WizardEvent`, `Tier`, `Segment`, `BrandInputs`.

## Log
- 2026-04-14 — Initial contract
