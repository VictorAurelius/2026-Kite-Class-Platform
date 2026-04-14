# Instance Lifecycle — Use Cases

### UC-INST-01: Initiate Provisioning
- **Actor:** Tenant onboarding flow (system)
- **Trigger:** tenant.created event (or admin manual create)
- **Steps:**
  1. Input: tenantId, slug
  2. System: validate slug unique (BR-INST-001)
  3. System: create FrontendInstance with status=NOT_STARTED
  4. System: transition NOT_STARTED → INITIALIZING (records initializing_at)
- **Postcondition:** Instance persisted, status=INITIALIZING

### UC-INST-02: Infrastructure Ready
- **Actor:** Infrastructure provisioning worker
- **Trigger:** infrastructure.ready event (DB schema, storage bucket, DNS created)
- **Steps:**
  1. Load instance by id
  2. System: transition INITIALIZING → GENERATING
- **Postcondition:** Branding pipeline may now enqueue

### UC-INST-03: Branding Pipeline Completed
- **Actor:** Branding executor (GAP-008 worker)
- **Steps:**
  1. Load instance by id
  2. System: set frontendUrl (if provided)
  3. System: transition GENERATING|REGENERATING → DEPLOYED
  4. System: increment brandingVersion
- **Postcondition:** Instance live; FE may notify via webhook (later wave)

### UC-INST-04: Rebrand
- **Actor:** Tenant admin / system auto-trigger
- **Preconditions:** instance.status = DEPLOYED
- **Steps:**
  1. Admin clicks "Rebrand"
  2. System: transition DEPLOYED → REGENERATING
  3. Branding pipeline runs; on success → UC-INST-03
- **Notes:** Existing branding remains live until new version deploys

### UC-INST-05: Handle Failure
- **Actor:** System (any pipeline stage)
- **Steps:**
  1. Pipeline reports error with reason string
  2. System: transition INITIALIZING|GENERATING|REGENERATING → FAILED
  3. System: record failureReason, increment retryCount
- **Postcondition:** Instance FAILED; admin may retry (UC-INST-06)

### UC-INST-06: Retry After Failure
- **Actor:** Admin (or scheduled auto-retry)
- **Preconditions:** status=FAILED AND retryCount < MAX_RETRIES
- **Steps:**
  1. Admin clicks "Retry"
  2. System: transition FAILED → INITIALIZING (clears failureReason)
  3. Pipeline resumes from UC-INST-02
- **Errors:** retryCount >= MAX_RETRIES → 409 "MAX_RETRIES exceeded"

### UC-INST-07: Reject Invalid Transition
- **Actor:** System (defensive)
- **Description:** Any caller attempting a status not in `allowedTransitions()` for current status gets `IllegalStateException` → HTTP 409.
- **Example:** attempting to deploy from NOT_STARTED.

## Log
- 2026-04-14 — Initial UCs
