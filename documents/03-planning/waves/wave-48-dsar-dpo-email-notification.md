---
title: Wave 48 — DSAR DPO Email Notification (GAP-353c-followup PARTIAL → DONE)
status: complete
outcome: shipped
created: 2026-05-09
updated: 2026-05-09
waves: [48]
gaps: [GAP-353c-followup-dpo-email-notification, GAP-353c]
---

# Wave 48 — DSAR DPO Email Notification

**Goal:** Thay log-line scaffold trong `DsarServiceImpl.notifyDpo` bằng `EmailServiceClient`-backed async dispatch (DPO inbox + requester acknowledgement), đóng GAP-353c từ 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2.
**Trigger:** Wave 48 candidate selection 2026-05-09 sau Wave 47 spawn; recon phát hiện `EmailServiceClient` đã tồn tại trong `kitehub-subscription` (`SubscriptionExpirationChecker` đã dùng) — blocker note "kitehub-email API not exposed" từ 2026-05-06 nay STALE.
**Estimated wall-clock:** ~60-90min (substantive editorial: 2 templates + service wiring + 2 unit tests, model: Opus medium effort).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA quality gate. PDPL Art 14 DSAR ticket persists + structured log emits regardless; missing chỉ là proactive DPO push notification + requester acknowledgement. Cải thiện signal-to-noise cho DPO operations (DPO không cần grep logs daily — push notification arrive trực tiếp inbox). Force multiplier: pattern reuse cho mọi DSAR right type subsequent (access/rectification/erasure/portability/restrict/object).

**Q2 (trade-offs):**
- **Direct REST `EmailController` call vs `EmailServiceClient` outbox-first:** `EmailServiceClient` đã ship outbox-first pattern per `design-patterns.md` §3.5.1 Exception A (`subscription_outbox` reliability net + best-effort `rabbitTemplate.convertAndSend` fast-path). Re-use precedent thay vì design lại. KC `EmailServiceClient` precedent cho 14 email types đã production.
- **Service-level vs DTO-level integration:** `DsarServiceImpl` injects `EmailServiceClient` đã pattern (precedent: `SubscriptionExpirationChecker` lines 79-82). Chỉ cần thêm 2 method shape: `sendDsarNewTicketDpoEmail(...)` + `sendDsarAcknowledgementEmail(...)`.
- **2 templates vs 1 template với conditional:** 2 templates (`dsar-new-ticket-dpo` + `dsar-acknowledgement-requester`) cleaner — different audience (DPO ops staff vs requester end-user), different tone (technical vs reassuring), different links (admin queue vs status check). Kế thừa template structure từ existing 15 templates trong `kitehub-email/src/main/resources/templates/emails/`.
- **`@Async` vs synchronous send:** `EmailServiceClient.dispatchEmail` đã async qua RabbitMQ outbox; ticket creation transaction commits ngay, email dispatch eventual consistency. Không cần thêm `@Async` annotation.

**Q3 (risks):**
- **Email failure must NOT roll back ticket creation** — outbox pattern mandatory per gap §"Acceptance Criteria" line 39. `EmailServiceClient.publishToQueue` đã wraps `rabbitTemplate.convertAndSend` trong try/catch → outbox row commits regardless → safe by precedent.
- **PII in logs/event payload** — `EmailEvent.variables` map containing `requesterEmail` + `nationalIdLast4` → outbox payload chứa PII. Mitigation: `logs-format-standard.md` §3.1 PII scrubber tự động mask in log output; outbox row trong DB encrypted at rest (per `secrets-management-runbook.md`). Verify scrubber regex catches `national_id`/`ccn` patterns.
- **`notifyDpo` audit log line preserved** — gap AC §"Acceptance Criteria" line 41: log line giữ làm "audit fallback (defense in depth)". Implement = log line CHẠY TRƯỚC dispatch, không thay thế. Pattern: emit log → dispatch email → cả hai shipped ngay cả khi email path fail.
- **Template Vietnamese-first vs English** — DSAR là legal/compliance flow; existing templates mix Vietnamese (e.g. `welcome.html` line 484: "Chào mừng bạn đến với KiteHub!") + English (e.g. `data-deleted.html` line 449: "Your KiteClass data has been deleted"). Vietnamese-first vì PDPL = VN regulation, requesters = VN users. Future i18n EN scaffold optional.
- **Idempotency** — `EmailServiceClient.alreadySentToday(instanceId, emailType, recipient)` check exists. DSAR ticket có UUID, không có instanceId → pass `null` (precedent: `sendTrialExpired(String, String)` line 125 passes `null`). Acceptable: nếu duplicate ticket submission cùng ngày cùng email + cùng rightType → expected behavior là dedup (per BR-PDPL-DSAR-003 implicit).

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-353c-followup-dpo-email-notification | bg-agent | est. 60-90min | ✅ single module (`kitehub-subscription`) + 2 templates trong `kitehub-email` |

Single-bucket wave — không cần Bucket 0 Foundation per `contract-first-for-cross-layer.md` §2 (no FE+BE coupling — backend-only + email templates same module-pair).

---

## 3. Scope (compact schema)

**Stake tier:** **LOW** (pattern reuse, precedent shipped + production-tested ×14 email types) → model: **Opus medium effort**.
**Cross-layer? NO** → skip Bucket 0 Foundation. Backend service wiring + 2 Thymeleaf templates only.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-353c-followup | 🟡 P2 | `kitehub-subscription/dsar/service/DsarServiceImpl.java` + `kitehub-subscription/client/EmailServiceClient.java` (extend) + `kitehub-email/src/main/resources/templates/emails/dsar-*.html` (2 new) + `DsarServiceImplTest.java` (extend) | single |

### Bucket A — DSAR DPO email integration

- Files (modified):
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/dsar/service/DsarServiceImpl.java`
    - Inject `EmailServiceClient` via `@RequiredArgsConstructor`
    - `notifyDpo(DsarTicket)` — keep log line (audit fallback per gap AC line 41), THEN dispatch 2 emails:
      1. `emailServiceClient.sendDsarNewTicketDpoEmail(ticketUuid, dpoEmail, dsarRightType, requesterEmail, slaDeadline)` — to DPO inbox
      2. `emailServiceClient.sendDsarAcknowledgementEmail(ticketUuid, requesterEmail, requesterName, dsarRightType, slaDeadline)` — to requester
    - Error handling: try/catch around each dispatch; log warn on failure; ticket transaction NOT rolled back per gap AC line 39
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java`
    - Add 2 new public methods following precedent (`sendTrialExpired` line 125 pattern):
      - `sendDsarNewTicketDpoEmail(UUID ticketUuid, String dpoEmail, DsarRightType rightType, String requesterEmail, LocalDateTime slaDeadline)` — `templateName="dsar-new-ticket-dpo"`, subject="[DSAR] Yêu cầu mới ({rightType}) — SLA hạn {sla}"
      - `sendDsarAcknowledgementEmail(UUID ticketUuid, String requesterEmail, String requesterName, DsarRightType rightType, LocalDateTime slaDeadline)` — `templateName="dsar-acknowledgement-requester"`, subject="Xác nhận yêu cầu DSAR — Mã ticket {uuid}"
    - Re-use existing `dispatchEmail(...)` private method (idempotency + outbox-first + admin toggle ALL pre-shipped)
    - `instanceId` = null (DSAR is not instance-bound; precedent `sendTrialExpired(String, String)` overload line 125)
  - `kitehub/kitehub-email/src/main/resources/templates/emails/dsar-new-ticket-dpo.html` (NEW)
    - Thymeleaf template, Vietnamese-first
    - Variables: `ticketUuid`, `rightType`, `requesterEmail`, `slaDeadline`, `dpoQueueUrl="https://kitehub.vn/admin/dsar"`
    - Body: structured ops alert layout (heading + table of fields + CTA button to queue)
  - `kitehub/kitehub-email/src/main/resources/templates/emails/dsar-acknowledgement-requester.html` (NEW)
    - Thymeleaf template, Vietnamese-first
    - Variables: `ticketUuid`, `requesterName`, `rightType`, `slaDeadline`, `statusCheckUrl="https://kitehub.vn/legal/data-rights/status?id={uuid}"`
    - Body: reassuring tone (heading + acknowledgement paragraph + ticket reference + SLA expectations + status link)
  - `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/dsar/service/DsarServiceImplTest.java`
    - Add 2 unit tests:
      - `submitRequest_dispatchesDpoEmail` — mock `EmailServiceClient`, verify `sendDsarNewTicketDpoEmail(...)` invoked với correct payload
      - `submitRequest_dispatchesRequesterAcknowledgement` — verify `sendDsarAcknowledgementEmail(...)` invoked
    - Add 1 resilience test:
      - `submitRequest_emailFailureDoesNotRollbackTicket` — mock `EmailServiceClient` to throw, verify ticket persisted via `repository.findByTicketUuid(...)`

- Files (created):
  - 2 Thymeleaf templates (above)

- Configuration:
  - `kitehub.dsar.dpo-email` config key (default: `dpo@kitehub.vn`) — read via `@Value` in `DsarServiceImpl`
  - Document trong `documents/01-business/kitehub/marketing/rules.md` extension: append `BR-PDPL-DSAR-006` cho dpo-email config

- Acceptance:
  - [ ] `EmailServiceClient` injected into `DsarServiceImpl` (constructor)
  - [ ] DPO email template `dsar-new-ticket-dpo.html` renders + dispatched on submit
  - [ ] Requester acknowledgement email template `dsar-acknowledgement-requester.html` renders + dispatched on submit
  - [ ] Email dispatch failure does not block ticket persistence (try/catch envelope; outbox is reliability net per `design-patterns.md` §3.5.1 Exception A)
  - [ ] Unit test verifies DPO email client invoked with correct payload (rightType + requesterEmail + slaDeadline)
  - [ ] Unit test verifies requester acknowledgement email client invoked
  - [ ] Resilience test verifies ticket persisted even if email dispatch throws
  - [ ] `notifyDpo` log line preserved as audit fallback (defense in depth per gap AC line 41)
  - [ ] `BR-PDPL-DSAR-006` added to `rules.md` per `business-logic-review.md` 5-attribute requirement (Source, Rationale, Reviewer, Compliance check, Review cadence)
  - [ ] `mvn -pl kitehub-subscription verify` PASS (full module verify with new tests)
  - [ ] GAP-353c-followup-dpo-email-notification flipped 🔵 OPEN → 🟢 DONE; GAP-353c parent flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `EmailServiceClient` (kitehub-subscription) | Java class | `find kitehub/kitehub-subscription/src/main/java -name "EmailServiceClient.java"` | 1 file at `client/EmailServiceClient.java` (699 lines) | ✅ exists |
| `EmailServiceClient.dispatchEmail` (private helper) | Java method | `grep -n "private void dispatchEmail" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java` | line 570 | ✅ exists |
| `EmailServiceClient.publishToQueue` outbox-first pattern | Java method | `grep -n "private void publishToQueue\|outbox is the reliability net" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java` | line 595 + Exception A marker comment line 618 | ✅ exists |
| `EmailController` REST endpoint | Java class | `grep -n "POST.*sendEmail\|@PostMapping" kitehub/kitehub-email/src/main/java/com/kitehub/email/controller/EmailController.java` | line 46 `sendEmail(@Valid @RequestBody EmailRequest)` | ✅ exists |
| Existing email template directory | filesystem | `ls kitehub/kitehub-email/src/main/resources/templates/emails/ \| wc -l` | 15 templates | ✅ exists (precedent for 2 new templates) |
| `DsarServiceImpl.notifyDpo` log-line scaffold | Java method | `grep -n "private void notifyDpo\|dsar.ticket.created" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/dsar/service/DsarServiceImpl.java` | lines 73-80 | ✅ exists |
| `DsarServiceImplTest` test class | Java class | `find kitehub/kitehub-subscription/src/test -name "DsarServiceImplTest.java"` | exists (verified Wave 26 Bucket A) | ✅ exists |
| `BR-PDPL-DSAR-001..005` business rules | rules.md | `grep -n "BR-PDPL-DSAR" documents/01-business/kitehub/marketing/rules.md` | rules shipped Wave 26 | ✅ exists (BR-006 to be appended) |
| `dsar-new-ticket-dpo.html` template | filesystem | `ls kitehub/kitehub-email/src/main/resources/templates/emails/dsar-new-ticket-dpo.html` | absent | 🆕 to-be-created (Bucket A owns) |
| `dsar-acknowledgement-requester.html` template | filesystem | `ls kitehub/kitehub-email/src/main/resources/templates/emails/dsar-acknowledgement-requester.html` | absent | 🆕 to-be-created (Bucket A owns) |
| `sendDsarNewTicketDpoEmail` method | Java method (to add) | `grep -n "sendDsarNewTicketDpoEmail" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java` | absent | 🆕 to-be-created (Bucket A owns) |
| `sendDsarAcknowledgementEmail` method | Java method (to add) | `grep -n "sendDsarAcknowledgementEmail" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/client/EmailServiceClient.java` | absent | 🆕 to-be-created (Bucket A owns) |

All ✅ existing references verified present (no `| head` truncation). All 🆕 to-be-created flagged with explicit owner = Bucket A. Per `audit-to-gap-pipeline.md` §2.6 → wave plan eligible to merge.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-subscription verify -P strict-warnings` (must include 3 new tests + existing tests pass) | `kitehub-ci.yml` test job |

Local verify on rebased HEAD per `admin-merge-discipline.md` §3 (kitehub module scope).

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- Single bucket → 1 background agent với `run_in_background: true`
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths only
- Coordinator merge sau agent completion
- Per `feedback_token_quota_spawn_timing.md` — spawn early; small scope (~60-90min) nên không lo context bloat
- Per `feedback_wave_plan_through_pr.md` — plan PR ships first, agent spawn sau khi plan merge

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:

- Bucket A PR updates GAP-353c-followup Log với "Wave 48 closure 2026-05-09" entry (🔵 OPEN → 🟢 DONE)
- Bucket A PR updates GAP-353c parent Log với "Wave 48 closes followup → parent gap fully closed" entry (🟡 PARTIAL → 🟢 DONE)
- Wave plan frontmatter `status: complete`
- ROADMAP §🚀 Next Action updated (remove GAP-353c-followup từ open list)
- `wave-history.jsonl` append (Rule 15: wave_id=48, gaps_closed, wall_clock, agent_id)
- `bash scripts/prune-merged-worktrees.sh --yes` sau merge
- **`## Release Plan Progress` section** — Phase 1 BETA: GAP-353c fully closed = 1 partial gap → done; PDPL Art 14 push notification flow active. Waves Remaining table không thay đổi (DSAR push-notification = quality enhancement, không phải MVP feature)
- Per `post-wave-audit-mandate.md` §2.4 domain-milestone deferral: this wave qualifies cho `backend-domain-{kitehub-subscription-dsar}` cluster milestone deferral nếu ≥1 wave nữa trong cluster planned; otherwise commit `DOMAIN_MILESTONE_AUDIT: backend-domain-kitehub-subscription-dsar` trailer + run audit suite

---

## 8. Log

- **2026-05-09 (closure — SHIPPED):** Wave 48 Bucket A SHIPPED in **~9min wall-clock** (vs 60-90min estimate; speedup 7-10×). PR **#1074** merged 2026-05-09 với `--admin` (Vercel rate-limit override per `admin-merge-discipline.md` §3 + ROADMAP precedent — all substantive CI green). 8 files changed, +528/-23 LOC.

  **Outcome:**
  - `EmailServiceClient` extended với 2 methods (`sendDsarNewTicketDpoEmail` + `sendDsarAcknowledgementEmail`) re-using outbox-first `dispatchEmail` precedent (line 595, `design-patterns.md` §3.5.1 Exception A)
  - 2 Thymeleaf templates created (`dsar-new-ticket-dpo.html` + `dsar-acknowledgement-requester.html`) — Vietnamese-first, DPO ops-alert + requester reassuring tone
  - `DsarServiceImpl.notifyDpo` wires both dispatches trong independent try/catch envelopes; audit log line preserved (defense in depth per AC line 41)
  - 3 unit tests added (dispatch verifications + resilience test for email-failure-doesn't-rollback-ticket); `DsarServiceImplTest`: 7 tests pass (4 original + 3 new)
  - `BR-PDPL-DSAR-006` (dpo-email config) added to `documents/01-business/kitehub/marketing/rules.md` với full 5-attribute review per `business-logic-review.md` §2
  - GAP-353c-followup-dpo-email-notification: 🔵 OPEN → 🟢 DONE (all 6 AC ticked)
  - **Cascade closure:** GAP-353c parent 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 — all 11/11 AC verified, last unchecked AC ("Email notification flow") satisfied by Wave 48
  
  **CI verification (PR #1074):**
  - Test KiteHub Subscription Service: PASS 1m0s ✅ (verifies agent's local mvn verify reproducible on CI runner)
  - Test KiteHub Email Service: PASS 58s ✅
  - Test KiteHub Admin/Branding/Gateway/Platform/Results: ALL PASS ✅
  - Code Quality + Secret Scanning: PASS ✅
  - Local mvn verify -P strict-warnings: BUILD SUCCESS, 455 tests, 0 failures
  - Vercel × 2: rate-limited 24h (environmental — kitehub-email templates aren't deployed via Vercel; ROADMAP precedent applies)
  
  **Discipline wins:**
  - Pre-flight verify confirmed plan §4 State-Check Evidence symbols still absent on main (no race condition with Wave 47 closure PR)
  - Pattern reuse via `dispatchEmail` outbox precedent — zero new infra, just compose existing primitives
  - 5-attribute business rule review for `BR-PDPL-DSAR-006` per `business-logic-review.md` §2 mandate
  - Cascade closure surfaced cleanly via `gap-done-discipline.md` §2 (last AC item check)
  
  Coordinator closure: ROADMAP §🚀 Next Action updated; `wave-history.jsonl` appended (outcome=shipped); worktree pruned.

- **2026-05-09 (draft):** Plan created. Wave 48 = single-bucket DSAR DPO email integration. Recon Wave 47 spawn-time (2026-05-09) phát hiện `EmailServiceClient` đã tồn tại trong `kitehub-subscription` (precedent `SubscriptionExpirationChecker`) → blocker note "kitehub-email API not exposed cross-module" từ GAP-353c-followup filing date 2026-05-06 nay STALE. Wave 48 reduces gap effort estimate 2-4h → 60-90min. Stake LOW (precedent reuse), Opus medium effort. Closes GAP-353c-followup → 🟢 DONE; cascade closes GAP-353c parent 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (last AC item satisfied).
