package com.kiteclass.core.module.payment.controller;

import com.kiteclass.core.module.payment.dto.gateway.MomoCallbackRequest;
import com.kiteclass.core.module.payment.dto.gateway.ZalopayCallbackRequest;
import com.kiteclass.core.module.payment.enums.PaymentMethod;
import com.kiteclass.core.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for payment gateway webhook callbacks.
 * These endpoints are PUBLIC (no authentication) - security via signature verification.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * VNPay payment gateway webhook callback.
     *
     * @param params webhook parameters from VNPay
     * @return success or error message
     */
    @GetMapping("/vnpay")
    public ResponseEntity<String> vnpayCallback(@RequestParam Map<String, String> params) {
        log.info("Received VNPay webhook callback");
        log.debug("VNPay params: {}", params);

        try {
            paymentService.processWebhookCallback(PaymentMethod.VNPAY, params);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("VNPay webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }

    /**
     * MoMo payment gateway webhook callback.
     *
     * @param request typed callback request from MoMo
     * @return JSON response with success/error message
     */
    @PostMapping("/momo")
    public ResponseEntity<Map<String, String>> momoCallback(@RequestBody MomoCallbackRequest request) {
        log.info("Received MoMo webhook callback");
        log.debug("MoMo params: {}", request);

        try {
            paymentService.processWebhookCallback(PaymentMethod.MOMO, request.toMap());
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (Exception e) {
            log.error("MoMo webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "error"));
        }
    }

    /**
     * ZaloPay payment gateway webhook callback.
     *
     * @param request typed callback request from ZaloPay
     * @return JSON response with return_code (1=success, 0=error)
     */
    @PostMapping("/zalopay")
    public ResponseEntity<Map<String, Integer>> zalopayCallback(@RequestBody ZalopayCallbackRequest request) {
        log.info("Received ZaloPay webhook callback");
        log.debug("ZaloPay params: {}", request);

        try {
            paymentService.processWebhookCallback(PaymentMethod.ZALOPAY, request.toMap());
            return ResponseEntity.ok(Map.of("return_code", 1));
        } catch (Exception e) {
            log.error("ZaloPay webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("return_code", 0));
        }
    }
}
