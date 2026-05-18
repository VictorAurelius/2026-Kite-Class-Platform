# Beta Access — Business Rules

**Domain:** Beta tenant invite mechanism (Wave 33 — GAP-372 + Wave 35 PDPL — GAP-385)
**Last verified:** 2026-05-18 (Wave 97 Bucket C — GAP-639 ABORTED enum sync)
**Config prefix:** `kitehub.beta`

This file documents the substantive business values for the beta-access flow. Each rule has the 5 attributes mandated by `.claude/rules/business-logic-review.md` §2.

---

## BR-BETA-001 — Explicit PDPL consent required at submit

- **Value:** `consentGiven` MUST be `true` at submit time. Server rejects `false`/`null`/missing with HTTP 400 + error code `BETA_CONSENT_REQUIRED`.
- **Source:** Luật Bảo vệ Dữ liệu Cá nhân (PDPL) 2023, Art 11 + Decree 13/2023/NĐ-CP, Art 17 — explicit, informed, freely-given consent for personal-data processing. Effective 2026-07-01 (hard deadline).
- **Rationale:** PDPL Art 11 mandates *explicit* consent prior to data collection. Pre-checked or implicit checkboxes are not compliant. The form collects email + name + orgName, all of which are personal data per Art 2(3). Consent token (`consentGiven=true` + `consent_at` timestamp) is the durable evidence trail required by Art 16 (data-subject rights enforcement).
- **Reviewer:** @nguyenvankiet (acting Legal scout + Compliance, solo-dev, 2026-05-08). Formal legal counsel review queued — see Phase 3 trigger in `documents/03-planning/roadmap/release-1-plan-2026.md`.
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 (explicit consent) + Art 16 (evidence retention via `consent_at`); FE checkbox unticked by default per Decree 13/2023/NĐ-CP Art 17.
- **Review cadence:** Annual + event-driven on PDPL implementing-decree publication. **Next review:** 2027-05-08 OR within 30 days of any new PDPL implementing-decree.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/BetaRequestDto.java` (Bucket B — Wave 35 GAP-385); `kitehub/kitehub-frontend/src/components/auth/BetaRequestForm.tsx` (Bucket B FE).

## BR-BETA-002 — Email uniqueness within active states

- **Value:** Only ONE active request per email at a time. "Active" = `PENDING` or `APPROVED`. Duplicate submit returns HTTP 409 + `BETA_DUPLICATE_EMAIL`.
- **Source:** Informed gut + Wave 33 design discussion (PR #802 BetaAccessService). No public competitor data point; rationale below stands without external data.
- **Rationale:** Allowing multiple PENDING rows per email creates coordinator confusion (which to approve?) and lets requesters game the queue. Once `REJECTED`, `ABORTED`, or `SIGNED_UP`, the email may resubmit (những trạng thái này là terminal — người dùng có thể đã giải quyết lý do bị từ chối hoặc gửi lại sau khi bị hủy tự động).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08).
- **Compliance check:** N/A — no PDPL / Consumer Protection trigger; uniqueness is a coordinator-UX optimization, not regulated.
- **Review cadence:** Quarterly. **Next review:** 2026-08-08. Event triggers: ≥10 duplicate-email complaints in any month.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java` (`submitRequest` duplicate check).

## BR-BETA-003 — PLATFORM_ADMIN role required for coordinator endpoints

- **Value:** `GET /api/v1/admin/beta-requests`, `POST /api/v1/admin/beta-requests/{id}/approve`, `POST /api/v1/admin/beta-requests/{id}/reject` MUST require authenticated user holding role `PLATFORM_ADMIN`. Unauthenticated → 401; authenticated non-admin → 403.
- **Source:** OWASP A01:2021 (Broken Access Control) + Wave 35 audit cluster finding GAP-384 (admin endpoints currently lack `@PreAuthorize`, only gateway-level guard which is bypassable from intra-mesh callers).
- **Rationale:** Defense-in-depth — gateway routing rules are not a security boundary inside the cluster. A single mesh misconfiguration or compromised neighbor service could reach these endpoints directly. Method-level `@PreAuthorize` enforces role at the controller, complementing (not replacing) the gateway. Phase 1 BETA launch cannot ship without this guard per `release-1-plan-2026.md` §3.
- **Reviewer:** @nguyenvankiet (acting Security lead + Product Owner, solo-dev, 2026-05-08).
- **Compliance check:** **Considered** — Luật An ninh mạng 2018 Art 26 (system access controls) and OWASP A01 baseline; no specific regulatory artifact mandates the exact mechanism, but the standard expects role-based access control at all layers.
- **Review cadence:** Quarterly. **Next review:** 2026-08-08. Event triggers: any beta-related security incident; addition of new admin endpoints.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java` (Bucket A — Wave 35 GAP-384 will add `@PreAuthorize`).

## BR-BETA-004 — Tự động hủy yêu cầu beta stale sau ngưỡng thời gian

- **Value:** PENDING request quá `kitehub.beta-access.abort-threshold-hours` giờ (mặc định `24`) không được coordinator xử lý sẽ bị tự động chuyển sang `ABORTED`. Row giữ lại trong DB cho audit trail; email được phép gửi lại yêu cầu mới (per BR-BETA-002).
- **Source:** Wave 92 — GAP-600 BetaRequestAbortCleanupScheduler implementation (quyết định thiết kế nội bộ nhằm ngăn coordinator queue bị ô nhiễm bởi PENDING rows cũ).
- **Rationale:** PENDING rows không giới hạn thời gian chiếm slot trong coordinator view, gây nhầm lẫn và queue pollution. Threshold 24h = 1× `kitehub.beta.invite-token-ttl-hours` — sau khi invite-token hết hạn, việc giữ PENDING request thêm không mang giá trị coordinator. Sau abort, email có thể resubmit per BR-BETA-002 (ABORTED là terminal state).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-18).
- **Compliance check:** N/A — internal queue management; không có trigger PDPL/Consumer Protection. Audit row được giữ lại per BR-BETA-001 (consent_at retained). ABORTED state không xóa personal data — chỉ thay đổi trạng thái request.
- **Review cadence:** Quarterly. **Next review:** 2026-08-18. Event triggers: coordinator phàn nàn về premature abort; yêu cầu điều chỉnh threshold; số lượng ABORTED requests > 20% tổng requests trong 1 tháng.
- **Code reference:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/scheduler/BetaRequestAbortCleanupScheduler.java` (Wave 92 — GAP-600).

---

## Config

| Key | Default | Purpose |
|-----|---------|---------|
| `kitehub.beta.invite-token-ttl-hours` | `24` | Approved-token expiry (existing Wave 33 config) |
| `kitehub.beta.allowed-personas` | `P1_SOLO_TEACHER,P2_CENTER_OWNER` | Phase 1 BETA scope (P3/P5 deferred to Phase 2/3) |
| `kitehub.beta-access.abort-threshold-hours` | `24` | Stale PENDING request auto-abort threshold (BR-BETA-004; Wave 92 GAP-600) |

These config keys are the source-of-truth values mirrored into `application.yml` per service.

---

## Related

- API contract: `documents/01-business/kitehub/beta-access/api-contract.md`
- Use cases: `documents/01-business/kitehub/beta-access/use-cases.md`
- Wave 35 plan: `documents/03-planning/waves/wave-2026-05-08-35-audit-p0-blockers-sprint.md`
- Rule: `.claude/rules/business-logic-review.md` (5-attribute requirement)
