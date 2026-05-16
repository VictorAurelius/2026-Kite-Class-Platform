---
title: Admin Resend Bounce Visibility + Impersonate Read-Only Spec (GAP-589)
status: complete-spec
created: 2026-05-16
phase: Wave 86 docs-cluster closure
wave: 86
gaps: [GAP-589]
related_bucket: Bucket H (incident response)
auditor: Solo dev coordinator
artifact_type: spec / planning doc (implementation = Wave 87+)
---

# Admin Resend Bounce Visibility + Impersonate Read-Only — Spec (GAP-589)

## 1. Scope + status

**Scope:** Spec admin dashboard features cho Mai (Platform Admin) incident-response workflows trong Phase 1 BETA cohort:

1. **Email delivery visibility panel** — aggregate Resend webhook bounces vào internal admin dashboard.
2. **Impersonate read-only mode** — admin xem tenant data nhưng KHÔNG mutation, với audit log mỗi session.
3. **Permission guard FE** — disable mọi mutation button khi impersonate session active.

**Status:** ⚠️ **PARTIAL — spec planning shipped (this PR); implementation = Wave 87+ follow-up.**

**Lý do PARTIAL:** GAP-589 H-AC13 requires FE + BE + audit log + webhook handler — full implementation > docs-cluster scope. Wave 86 docs-cluster ship spec; Wave 87+ ship code.

---

## 2. Requirements summary (from GAP-589 §Proposed Fix + §AC)

### 2.1 Resend bounce webhook handler

- **Endpoint:** `POST /api/v1/admin/webhooks/resend` (Resend webhook signed payload).
- **Events subscribed:** `email.bounced`, `email.complained`, `email.delivery_delayed`.
- **Signature verify:** HMAC-SHA256 với `RESEND_WEBHOOK_SECRET` (env). Reject unsigned/invalid với 401.
- **Persistence:** `email_send_audit` table:
  - `id BIGSERIAL PK`
  - `provider_message_id VARCHAR(255)` — Resend `email_id`
  - `recipient_email VARCHAR(255)` — scrubbed per logs-format-standard.md §3 khi log
  - `event_type ENUM('sent','delivered','bounced','complained','delayed')`
  - `bounce_reason VARCHAR(500)` — Resend `bounce.message` (eg. "hard_bounce: mailbox does not exist")
  - `cohort_tag VARCHAR(50)` — tenant cohort (e.g., "wave-86-p1-solo", "wave-86-p2-owner")
  - `tenant_id BIGINT NULL FK → tenants.id`
  - `event_timestamp TIMESTAMPTZ NOT NULL`
  - `received_at TIMESTAMPTZ DEFAULT now()`
  - `raw_payload JSONB` — full webhook body for debug

### 2.2 Admin "Email Delivery" tab

- **Route:** `kitehub-frontend/src/app/(admin)/admin/email-delivery/page.tsx`
- **API:** `GET /api/v1/admin/email-delivery?cohort=<tag>&status=<event>&from=<ts>&to=<ts>&page=N&size=20`
- **Table columns:**
  - Timestamp (vi format `dd/MM/yyyy HH:mm`)
  - Recipient (full email — admin-only scope; scrub trong logs per format-standard)
  - Status badge (sent green / bounced red / complained orange / delayed yellow)
  - Bounce reason (truncated 80 chars, full hover)
  - Cohort tag
  - Resend button (POST `/api/v1/admin/email-delivery/{id}/resend`)
- **Filters:** cohort dropdown + status checkboxes + date range picker
- **Pagination:** 20 rows / page, max 1000 rows visible per query
- **Authorization:** require `PLATFORM_ADMIN` role; reject `READ_ONLY` impersonate session

### 2.3 Impersonate read-only endpoint

- **Endpoint:** `POST /api/v1/admin/impersonate`
  - Request body: `{ tenantId: int, userId: int, reason: string (required, min 10 chars), durationMinutes: int (default 15, max 60) }`
  - Response: `{ impersonationToken: string (JWT), expiresAt: timestamp, audit_id: int }`
- **JWT claims:**
  ```json
  {
    "sub": "<target_user_id>",
    "tenant_id": "<target_tenant_id>",
    "role": "READ_ONLY",
    "impersonator": "<admin_user_id>",
    "impersonator_email": "<admin_email>",
    "audit_id": "<audit_log_id>",
    "iat": <issued_at>,
    "exp": <issued_at + 15m>
  }
  ```
- **Mutation guard:** Spring Security `@PreAuthorize` interceptor checks `role != 'READ_ONLY'` cho mọi `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` endpoints. Throws 403 nếu attempt mutate while `role == 'READ_ONLY'`.
- **Audit log table:** `admin_impersonation_audit`:
  - `id BIGSERIAL PK`
  - `admin_user_id BIGINT NOT NULL FK → users.id`
  - `target_tenant_id BIGINT NOT NULL FK → tenants.id`
  - `target_user_id BIGINT NOT NULL FK → users.id`
  - `reason VARCHAR(500) NOT NULL` — required justification (e.g., "tenant report bug #123 cannot login")
  - `session_start TIMESTAMPTZ NOT NULL DEFAULT now()`
  - `session_end TIMESTAMPTZ NULL` — set khi token expires hoặc admin click "End impersonation"
  - `actions_logged JSONB` — array of endpoints visited during session
- **Immutable:** per `V60__immutable_admin_audit_logs.sql` Wave 85 Bucket H pattern (PDPL Art 11 tamper-proof).

### 2.4 FE permission guard (button disable)

- **Hook:** `useImpersonationContext()` returns `{ isImpersonating: boolean, role: string, originalAdminEmail: string }`.
- **Component pattern:**
  ```tsx
  const { isImpersonating } = useImpersonationContext();
  <Button disabled={isImpersonating} title={isImpersonating ? 'Read-only mode — không thể thực hiện' : ''}>
    Lưu
  </Button>
  ```
- **Banner:** persistent top banner red "🔒 Bạn đang impersonate <tenant.name> — read-only mode. <End session> button"

### 2.5 Wave 86 Bucket H runbook reference

- **Add section to `incident-response-runbook.md`:** "When tenant reports bug — debug path via impersonate":
  1. Verify tenant identity via support ticket.
  2. Admin login dashboard → Tenants page → find tenant → "Hỗ trợ" button.
  3. Fill reason field (required, min 10 chars).
  4. Click "Bắt đầu phiên hỗ trợ" → 15-min read-only token issued.
  5. Reproduce bug; document findings.
  6. Click "End session" hoặc wait token expire.
  7. Audit log auto-captured.

---

## 3. Acceptance criteria mapping

| AC (from GAP-589) | Spec covers | Status |
|---|---|---|
| Resend webhook handler shipped + bounce events persisted | §2.1 schema + endpoint defined | ⏳ Implementation Wave 87+ |
| Admin "Email Delivery" tab live với filter + resend | §2.2 columns + filters + pagination | ⏳ Implementation Wave 87+ |
| Impersonate read-only endpoint shipped + JWT enforces role check | §2.3 endpoint + JWT claims + guard | ⏳ Implementation Wave 87+ |
| FE mutation buttons disabled khi impersonate active | §2.4 hook + Button pattern | ⏳ Implementation Wave 87+ |
| Audit log every impersonate session với reason | §2.3 `admin_impersonation_audit` schema | ⏳ Implementation Wave 87+ |
| Wave 86 Bucket H runbook reference này cho incident response | §2.5 runbook section | ⏳ Add section to incident-response-runbook.md Wave 87+ |

---

## 4. Implementation phasing (Wave 87+ follow-up)

| Phase | Scope | Owner | ETA |
|---|---|---|---|
| **Phase 1 — Webhook + persistence** | §2.1 schema + endpoint + Resend dashboard webhook subscribe | BE | Wave 87 (after Wave 86 Bucket G Resend production verify) |
| **Phase 2 — Admin "Email Delivery" tab** | §2.2 FE page + GET API | FE + BE | Wave 87+ |
| **Phase 3 — Impersonate flow** | §2.3 endpoint + JWT + audit log + §2.4 FE guard | BE + FE + DB migration | Wave 88+ (higher security risk; needs review) |
| **Phase 4 — Runbook + training** | §2.5 incident-response-runbook section + admin walkthrough | Solo dev | Same wave as Phase 3 |

**Wave 86 docs-cluster** ships **Phase 0** — spec only. Concrete implementation tickets file Wave 87+ planning.

---

## 5. Security considerations

- **Impersonate JWT:** must use SEPARATE signing secret (not main JWT secret) — minimize blast radius if leak.
- **Audit log immutability:** per V60 Wave 85 pattern, no DELETE/UPDATE allowed — append-only.
- **Reason field requirement:** prevent "casual snooping" — admin must articulate reason. Pattern frequency >3/day per admin → quarterly retro.
- **Notification to tenant:** ⚠️ Phase 1.5+ scope — consider sending tenant email "Your account was accessed by support at <time> for reason <reason>". Phase 1 BETA defer (5-cohort, manual trust).
- **Mutation guard:** double-layer protection — Spring Security annotation + FE button disable. Defense-in-depth.

---

## 6. References

- **GAP-589:** `documents/04-quality/gaps/GAP-589-admin-resend-bounce-visibility-impersonate-readonly.md`
- **Persona audit:** `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.5 cell 5.5
- **Bucket H reference:** Wave 86 plan §3 Bucket H AC H-AC13
- **PDPL pattern:** `V60__immutable_admin_audit_logs.sql` (Wave 85 Bucket H) — reuse pattern cho `admin_impersonation_audit` table.
- **Runbook target:** `documents/05-guides/operations/incident-response-runbook.md` (add §"Tenant debug via impersonate" section Wave 87+)

---

## 7. Conclusion

GAP-589 spec planning complete. Implementation Wave 87+ multi-phase. GAP-589 status flip OPEN → PARTIAL (spec shipped; H-AC13 implementation deferred). Follow-up gap file Wave 87 planning:

- `GAP-XXX wave-87-resend-bounce-webhook-handler` (Phase 1 P1)
- `GAP-XXX wave-87-admin-email-delivery-tab` (Phase 2 P2)
- `GAP-XXX wave-87-admin-impersonate-readonly` (Phase 3 P1 — security review required)
