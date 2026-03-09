-- V3: Create payments table for VietQR payment processing
-- Reference: PR 4.6 - VietQR Payment Service

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    amount_vnd BIGINT NOT NULL,  -- Payment amount in Vietnamese Dong
    currency VARCHAR(3) DEFAULT 'VND' NOT NULL,
    payment_method VARCHAR(30) NOT NULL,  -- VIETQR, MOMO, VNPAY, BANK_TRANSFER
    status VARCHAR(20) NOT NULL,  -- PENDING, COMPLETED, FAILED, REFUNDED
    qr_code_url VARCHAR(500),  -- URL to generated QR code image
    transaction_id VARCHAR(100),  -- Bank transaction ID
    bank_code VARCHAR(20),  -- Bank code (e.g., VCB, TCB, MB)
    account_number VARCHAR(50),  -- Recipient account number
    account_name VARCHAR(200),  -- Recipient account name
    payment_content VARCHAR(500),  -- Payment description/note
    paid_at TIMESTAMP,  -- Actual payment timestamp
    refunded_at TIMESTAMP,  -- Refund timestamp (if applicable)
    refund_reason VARCHAR(500),  -- Reason for refund
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_payments_subscription ON payments(subscription_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);
CREATE INDEX idx_payments_created ON payments(created_at DESC);
CREATE INDEX idx_payments_deleted ON payments(deleted) WHERE deleted = false;

-- Check constraint for valid payment method
ALTER TABLE payments ADD CONSTRAINT chk_payment_method
    CHECK (payment_method IN ('VIETQR', 'MOMO', 'VNPAY', 'BANK_TRANSFER', 'MANUAL'));

-- Check constraint for valid status
ALTER TABLE payments ADD CONSTRAINT chk_payment_status
    CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED'));

-- Check constraint for valid currency
ALTER TABLE payments ADD CONSTRAINT chk_payment_currency
    CHECK (currency IN ('VND', 'USD'));

-- Check constraint for positive amount
ALTER TABLE payments ADD CONSTRAINT chk_payment_amount
    CHECK (amount_vnd > 0);

-- Comments for documentation
COMMENT ON TABLE payments IS 'Payment transactions for subscriptions';
COMMENT ON COLUMN payments.qr_code_url IS 'VietQR QR code image URL for bank transfer';
COMMENT ON COLUMN payments.transaction_id IS 'Unique transaction ID from payment gateway or bank';
COMMENT ON COLUMN payments.payment_content IS 'Payment description shown to user (e.g., "KITECLASS {instance_id}")';
COMMENT ON COLUMN payments.refund_reason IS 'Reason for refund (customer request, error, etc.)';
