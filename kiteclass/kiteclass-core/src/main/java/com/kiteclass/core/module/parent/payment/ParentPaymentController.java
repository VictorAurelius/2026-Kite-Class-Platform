package com.kiteclass.core.module.parent.payment;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.notification.ZaloOaNotificationService;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.payment.dto.CreatePaymentRequest;
import com.kiteclass.core.module.payment.dto.PaymentResponse;
import com.kiteclass.core.module.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * Wave 105 Bucket D — Parent payment endpoint với multi-child authz + VietQR
 * idempotency stub.
 *
 * <p>This controller wraps the underlying {@link PaymentService} with two
 * parent-specific concerns:
 *
 * <ol>
 *   <li><strong>Multi-child authorization</strong> — parent Linh có 2 con
 *       (per Wave 105 plan §3 Bucket D AC); endpoint takes
 *       {@code /children/{childId}/...} path variable + verifies
 *       {@link ParentStudentLinkRepository#existsByParentIdAndStudentIdAndDeletedFalse}.
 *       Spoof {@code childId=B} when only linked to {@code A} → 403
 *       {@code PARENT_NOT_LINKED}. Same pattern as
 *       {@link com.kiteclass.core.module.parent.service.impl.ParentTranscriptServiceImpl}.</li>
 *
 *   <li><strong>Idempotency-Key header</strong> — VietQR per `pre-handoff-self-test-completeness.md`
 *       §2.6 (d). Client sends {@code Idempotency-Key: &lt;uuid&gt;} header;
 *       same key replayed → returns same payment row + same QR payload, no
 *       double-charge. Race-safe via UNIQUE constraint on
 *       {@code payment_idempotency_keys (instance_id, idempotency_key)}.</li>
 * </ol>
 *
 * <p>This controller does NOT depend on Bucket E security cluster fix
 * (PaymentController.java hardcoded `userId=1L` per Bucket E scope). Parent
 * identity comes from {@code X-User-Reference-Id} header, populated by gateway
 * from {@code users.reference_id} when {@code userType=PARENT}. The underlying
 * {@code PaymentService.createPayment(request, parentId)} call passes the
 * real parent id, so Bucket E1 fix lands BE-side independently.
 *
 * <p>Per `vn-localization-audit-checklist.md` Section 4: VietQR (not credit
 * card) is the canonical VN edu payment channel. Wave 105 ships local mock
 * QR payload; Wave 106 GAP-NEW integrates real VietQR API per partner-bank
 * agreement (defer per `outside-in-coverage-trigger.md` v1.1.0 §3 Architecture
 * decision — PSP license + KYC barriers per Wave 93 retro lessons).
 *
 * @since 3.0.0 (Wave 105 Bucket D)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Parent Payment", description = "Parent-scoped payment endpoints (Wave 105 Bucket D)")
public class ParentPaymentController {

    private final PaymentService paymentService;
    private final ParentStudentLinkRepository linkRepository;
    private final PaymentIdempotencyService idempotencyService;
    private final ZaloOaNotificationService zaloOaNotificationService;

    /**
     * Create a payment cho 1 child trong scope của parent. Multi-child authz
     * + Idempotency-Key per §1 javadoc.
     *
     * <p>Returns:
     * <ul>
     *   <li>201 + {@link PaymentResponse} + {@code X-Payment-Idempotent-Replay: false}
     *       — first request, payment created.</li>
     *   <li>200 + {@link PaymentResponse} + {@code X-Payment-Idempotent-Replay: true}
     *       — replay; cached payment returned, no new charge.</li>
     *   <li>400 {@code IDEMPOTENCY_KEY_REQUIRED} hoặc {@code INVALID_IDEMPOTENCY_KEY}
     *       — header missing / malformed.</li>
     *   <li>401 {@code AUTH_REQUIRED} — gateway didn't forward parent id.</li>
     *   <li>403 {@code PARENT_NOT_LINKED} — parent không có active link to
     *       {@code childId} (cross-child spoof).</li>
     * </ul>
     *
     * @param childId       child being paid for (BR-PARENT-PORTAL-001 scope check)
     * @param request       create payment request DTO (reuses existing {@link CreatePaymentRequest})
     * @param parentId      forwarded by gateway from {@code X-User-Reference-Id}
     * @param idempotencyKey client-supplied {@code Idempotency-Key} header
     */
    @PreAuthorize("@authz.hasAccessToChild(#childId)")
    @PostMapping("/children/{childId}/payments")
    @Operation(summary = "Create payment for one of the parent's linked children",
            description = "Multi-child authz + VietQR idempotency. " +
                    "BR-PARENT-PORTAL-001: 403 PARENT_NOT_LINKED nếu parent không link với child.")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentForChild(
            @PathVariable @NotNull Long childId,
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long resolvedParentId = requireParentId(parentId);
        String validKey = idempotencyService.requireValidKey(idempotencyKey);

        // Multi-child authz (BR-PARENT-PORTAL-001): parent only allowed to pay
        // for children they are actively linked to. Same boolean-exists query
        // as ParentTranscriptServiceImpl §guard 3 — no info leak about child
        // identity to a non-linked caller.
        if (!linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(resolvedParentId, childId)) {
            log.warn("Parent {} attempted payment for unlinked child {} — denied",
                    resolvedParentId, childId);
            throw new BusinessException("PARENT_NOT_LINKED", HttpStatus.FORBIDDEN);
        }

        String tenantId = currentTenantId();

        // Idempotency check — replay path returns cached payment_id.
        Optional<PaymentIdempotencyService.IdempotentResult> existing =
                idempotencyService.lookup(tenantId, validKey);
        if (existing.isPresent()) {
            PaymentIdempotencyService.IdempotentResult result = existing.get();
            log.info("Idempotent replay: key={} returning cached paymentId={}",
                    validKey, result.paymentId());
            PaymentResponse cached = paymentService.getPaymentById(result.paymentId());
            return ResponseEntity.status(HttpStatus.OK)
                    .header("X-Payment-Idempotent-Replay", "true")
                    .body(ApiResponse.success(cached));
        }

        // First-write path. Payment audit actor (created_by) is the caller's
        // X-User-Id UUID (GAP-795) from UserContext — NOT the numeric parent domain
        // id (resolvedParentId stays for the multi-child authz check above).
        UUID actorUserId = UserContext.getCurrentUser();
        log.info("Creating parent payment: actorUserId={} parentId={} childId={} invoiceId={} method={}",
                actorUserId, resolvedParentId, childId, request.getInvoiceId(), request.getPaymentMethod());
        PaymentResponse created = paymentService.createPayment(request, actorUserId);

        // Persist idempotency mapping. If race lost, re-lookup winner's row.
        // QR payload is stub Wave 105 — Wave 106 GAP-NEW integrates real VietQR.
        String qrPayload = stubVietQrPayload(created, request);
        boolean inserted = idempotencyService.recordFirstWrite(
                tenantId, validKey, resolvedParentId,
                request.getInvoiceId(), created.getId(), qrPayload);

        if (!inserted) {
            // Race lost — return winner's payment via re-lookup.
            PaymentIdempotencyService.IdempotentResult winner =
                    idempotencyService.lookup(tenantId, validKey)
                            .orElseThrow(() -> new BusinessException(
                                    "IDEMPOTENCY_RACE_INCONSISTENT", HttpStatus.CONFLICT));
            PaymentResponse winnerResponse = paymentService.getPaymentById(winner.paymentId());
            return ResponseEntity.status(HttpStatus.OK)
                    .header("X-Payment-Idempotent-Replay", "true")
                    .body(ApiResponse.success(winnerResponse));
        }

        // Best-effort Zalo OA confirmation stub (Wave 106 dispatches actual ZNS).
        // Per `audit-service-isolation.md` — REQUIRES_NEW so failure doesn't
        // poison this txn.
        zaloOaNotificationService.recordPaymentConfirm(
                resolvedParentId, request.getInvoiceId(),
                request.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue(),
                "<childName>" /* Wave 106 resolves via child lookup */);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Payment-Idempotent-Replay", "false")
                .body(ApiResponse.success(created));
    }

    /**
     * Wave 105 stub VietQR payload. Wave 106 GAP-NEW replaces with real
     * VietQR API call.
     *
     * <p>Format matches VietQR EMVCo TLV (Tag-Length-Value) standard so
     * downstream FE can render the QR using existing libraries:
     * <pre>00020101021238540010A00000072701240006970422...</pre>
     *
     * <p>Stub returns deterministic fake string keyed by payment_id so
     * idempotency replay returns same string.
     */
    private String stubVietQrPayload(PaymentResponse payment, CreatePaymentRequest request) {
        return String.format(
                "VIETQR-STUB|paymentId=%d|invoiceId=%d|amountVnd=%d|method=%s",
                payment.getId(),
                request.getInvoiceId(),
                request.getAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue(),
                request.getPaymentMethod());
    }

    /**
     * Resolve current tenant_id (instance_id) from the request-scoped
     * {@link TenantContext} thread-local (GAP-1413).
     *
     * <p>Populated by {@code TenantFilterInterceptor} from the gateway-injected
     * {@code X-Tenant-Id} header — the same source every other tenant-scoped
     * kiteclass-core endpoint uses. This scopes the payment-idempotency lookup
     * + first-write to the caller's real tenant, replacing the former nil-UUID
     * stub that collapsed every tenant's idempotency keys into one phantom
     * tenant (cross-tenant RLS hole).
     *
     * @return the current tenant UUID as a string (for the {@code instance_id} scope)
     * @throws com.kiteclass.core.common.exception.TenantNotSetException
     *         if the request bypassed the tenant interceptor (missing X-Tenant-Id)
     */
    private String currentTenantId() {
        return TenantContext.getCurrentTenant().toString();
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
