# Branding Wizard — Use Cases

### UC-WIZ-01: New Tenant Runs Wizard End-to-End
- **Actor:** Tenant admin after signup
- **Steps:**
  1. Welcome → pick segment (K12/CENTER/UNIV/CORP/OTHER)
  2. Logo → optional upload OR skip
  3. Audience → tick ≥1 (students/parents/teachers/staff)
  4. Tone → pick 1 + (tier-gated) color/typography/density/prompt
  5. Template → pick from 6 previews filtered by segment
  6. Preview → review summary + live iframe + deploy
- **Postcondition:** saga triggered via submit; FSM transitions to submitting → done

### UC-WIZ-02: Regenerate After Preview
- **Precondition:** state=preview OR state=done, regenerateCount < limit
- **Steps:**
  1. User clicks 🔄 Regenerate
  2. FSM → submitting (regenerateCount++)
  3. Same submit flow; success → done; failure → error
- **Blocked:** quota exhausted → button disabled, counter shows destructive

### UC-WIZ-03: Submit Fails
- **Trigger:** backend returns non-2xx
- **FSM:** submitting → error (message captured in context)
- **UX:** alert-role banner with message + retry button + "← back to preview" escape

### UC-WIZ-04: Edit After Preview
- **Precondition:** state=preview
- **Steps:** click "← Chỉnh sửa" → FSM back to template → further BACK cascades

### UC-WIZ-05: Tier Upgrade Unlocks Fields
- **Trigger:** user's tier changes (out-of-band; not in wizard itself)
- **Effect on open wizard:** rerendering after tier change shows additional inputs; previously-entered values preserved (reducer merges patches)

### UC-WIZ-06: Enterprise Advanced Mode
- **Precondition:** tier === ENTERPRISE
- **Extra fields:** customPrompt, brandKeywords, bannedKeywords, preferredFonts, accessibilityLevel, supportedLanguages, brandValues
- **Notes:** BR-WIZ-004 — free-form prompt permitted only here; backend still composes final AI prompt

### UC-WIZ-07: Cold Start (No Auth)
- Not in scope — wizard page assumes authenticated user; unauthed request is redirected by existing middleware

## Log
- 2026-04-14 — Initial UCs
