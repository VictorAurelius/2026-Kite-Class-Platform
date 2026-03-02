package com.kiteclass.core.module.payment.service;

import com.kiteclass.core.module.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * Generates unique payment numbers with year-based sequence.
 * Pattern: PAY-{year}-{sequence}
 * Example: PAY-2026-000001
 *
 * Thread-safe via @Transactional.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class PaymentNumberGenerator {

    private final PaymentRepository paymentRepository;

    /**
     * Generates next payment number for tenant.
     *
     * @param instanceId tenant ID
     * @return generated payment number
     */
    @Transactional
    public String generate(UUID instanceId) {
        String year = String.valueOf(Year.now().getValue());
        String pattern = "PAY-" + year + "-%";

        List<String> latest = paymentRepository.findLatestPaymentNumber(
            instanceId, pattern, PageRequest.of(0, 1));

        int nextNumber = 1;
        if (!latest.isEmpty()) {
            String lastNumber = latest.get(0).substring(9); // "PAY-2026-" = 9 chars
            nextNumber = Integer.parseInt(lastNumber) + 1;
        }

        return String.format("PAY-%s-%06d", year, nextNumber);
    }
}
