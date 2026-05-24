package com.kiteclass.core.module.payment.record.repository;

import com.kiteclass.core.module.payment.record.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    /**
     * Cross-tenant safe lookup: fetches all payment records for a given invoice
     * within a specific tenant (instanceId). Prevents OWASP A01 cross-tenant leak.
     */
    List<PaymentRecord> findByInvoiceIdAndInstanceId(Long invoiceId, UUID instanceId);

    /**
     * Internal admin use only — list all payments for an invoice without tenant filter.
     * Callers MUST enforce instanceId check at service layer before exposing results.
     */
    List<PaymentRecord> findAllByInvoiceId(Long invoiceId);
}
