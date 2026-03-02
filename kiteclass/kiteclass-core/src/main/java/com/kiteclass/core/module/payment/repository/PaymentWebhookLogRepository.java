package com.kiteclass.core.module.payment.repository;

import com.kiteclass.core.module.payment.entity.PaymentWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PaymentWebhookLog entity operations.
 *
 * @since 1.0.0
 */
@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    /**
     * Finds all webhook logs for a payment.
     *
     * @param paymentId payment ID
     * @return list of webhook logs
     */
    List<PaymentWebhookLog> findByPaymentId(Long paymentId);

    /**
     * Finds all webhook logs for a gateway.
     *
     * @param gateway gateway name (VNPAY, MOMO, ZALOPAY)
     * @return list of webhook logs
     */
    List<PaymentWebhookLog> findByGateway(String gateway);
}
