# GAP-020: Wizard State Persistence & Error Recovery

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / UX
**Detected:** 2026-04-14 (simulation)

## Problem

Wizard 6 bước — nếu tenant mất state giữa chừng = frustration + abandonment:

- ❌ Browser refresh → lose wizard progress
- ❌ Network disconnect during preview → state lost
- ❌ Không autosave draft
- ❌ Không "resume wizard" sau login lại
- ❌ Sau deploy fail → không có fallback state

Abandonment rate cao → tenant không complete branding → instance xấu → churn.

## Proposed Fix

### 1. Client-side Persistence (Zustand + localStorage)

```tsx
// kitehub-frontend/src/stores/branding-wizard-store.ts
export const useBrandingWizardStore = create(
  persist(
    (set, get) => ({
      currentStep: 0,
      logo: null,
      audience: null,
      tone: null,
      templateId: null,
      generatedResources: {},
      approvedResources: [],
      regenerateCount: 0,

      nextStep: () => set(state => ({ currentStep: state.currentStep + 1 })),
      reset: () => set(initialState),
    }),
    {
      name: 'branding-wizard',
      partialize: (state) => ({
        currentStep: state.currentStep,
        audience: state.audience,
        tone: state.tone,
        templateId: state.templateId,
        // Don't persist large objects (logo File, resource blobs)
      }),
    }
  )
);
```

### 2. Server-side Draft Persistence

```java
@Entity
public class BrandingWizardDraft {
  String draftId;  // UUID
  String tenantId;
  Integer currentStep;
  JsonObject wizardState;
  Timestamp updatedAt;
  Timestamp expiresAt;  // 7 days TTL
}
```

API:
- `POST /api/v1/branding/wizard/draft` — autosave
- `GET /api/v1/branding/wizard/draft/latest` — resume
- `DELETE /api/v1/branding/wizard/draft/{id}` — discard

### 3. Autosave Trigger Points

- After each step completion
- Before preview generation
- Every 30s during active session (debounced)

### 4. Resume Flow

Khi tenant login:
```tsx
const { draft, hasDraft } = useLatestWizardDraft();

if (hasDraft) {
  return <ResumeWizardBanner onResume={() => loadDraft(draft)} onDiscard={...} />;
}
```

### 5. Error Recovery

- Preview generation fail → retry 3x với exponential backoff
- Network disconnect → show offline banner, queue actions
- Deploy fail → state remains in REGENERATING, allow retry without losing wizard progress

## Acceptance Criteria

- [ ] Zustand store với persist middleware
- [ ] `BrandingWizardDraft` entity + API endpoints
- [ ] Autosave every 30s + on step change
- [ ] Resume banner on login if draft exists
- [ ] Draft TTL 7 days, cleanup job
- [ ] Offline banner + retry logic
- [ ] E2E test: start wizard → refresh browser → resume successfully
- [ ] E2E test: start wizard → logout → login → resume

## Dependencies

- GAP-013 (wizard UX) — this adds persistence layer

## Log

- 2026-04-14 — Identified via abandonment risk simulation
