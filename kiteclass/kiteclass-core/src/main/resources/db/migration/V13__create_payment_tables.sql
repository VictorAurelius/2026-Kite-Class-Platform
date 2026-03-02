-- V13: Create payment tables for payment gateway integration
-- Author: KiteClass Development Team
-- Date: 2026-03-02

-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,

    -- Payment identification
    payment_number VARCHAR(50) NOT NULL UNIQUE,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,

    -- References
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    installment_id BIGINT REFERENCES installments(id),

    -- Payment details
    amount DECIMAL(12, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- Gateway integration
    gateway_transaction_id VARCHAR(255),
    payment_url TEXT,
    qr_code_url TEXT,
    gateway_response JSONB,

    -- Receipt
    receipt_number VARCHAR(50),
    receipt_url TEXT,

    -- Timing
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE,

    -- Failure details
    failure_reason TEXT,

    -- Audit
    created_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_method CHECK (
        payment_method IN ('CASH', 'BANK_TRANSFER', 'MOMO', 'VNPAY', 'ZALOPAY', 'CREDIT_CARD')
    ),
    CONSTRAINT chk_payment_status CHECK (
        payment_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')
    ),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_instance ON payments(instance_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_installment ON payments(installment_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);
CREATE INDEX idx_payments_deleted ON payments(deleted) WHERE deleted = false;

COMMENT ON TABLE payments IS 'Payment records for invoice and installment payments';
COMMENT ON COLUMN payments.transaction_id IS 'Unique transaction ID for idempotency (prevents duplicate payments)';
COMMENT ON COLUMN payments.gateway_transaction_id IS 'Transaction ID from payment gateway (VNPay/MoMo/ZaloPay)';
COMMENT ON COLUMN payments.gateway_response IS 'Full JSON response from gateway for audit trail';

-- Webhook logs for audit trail
CREATE TABLE payment_webhook_logs (
    id BIGSERIAL PRIMARY KEY,
    instance_id UUID NOT NULL,
    payment_id BIGINT REFERENCES payments(id),
    gateway VARCHAR(50) NOT NULL,
    request_payload JSONB NOT NULL,
    signature VARCHAR(512),
    signature_valid BOOLEAN,
    processed BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_logs_payment ON payment_webhook_logs(payment_id);
CREATE INDEX idx_webhook_logs_gateway ON payment_webhook_logs(gateway);
CREATE INDEX idx_webhook_logs_created ON payment_webhook_logs(created_at);

COMMENT ON TABLE payment_webhook_logs IS 'Audit trail for payment gateway webhook callbacks';
COMMENT ON COLUMN payment_webhook_logs.signature_valid IS 'HMAC signature verification result (security check)';
COMMENT ON COLUMN payment_webhook_logs.processed IS 'Whether webhook was successfully processed (idempotency)';
