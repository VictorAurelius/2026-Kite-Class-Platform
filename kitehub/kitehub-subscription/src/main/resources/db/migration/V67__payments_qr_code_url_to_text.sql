-- KH-3 G2 walk (SePay real-transfer path): VietQRService stores the QR as a base64 PNG
-- data-URL (data:image/png;base64,iVBOR...) which is several KB long. The original
-- qr_code_url VARCHAR(500) truncates it (SQLState 22001 right-truncation) so the PENDING
-- payment insert fails and POST /api/platform/subscriptions returns 409. Widen to TEXT.
ALTER TABLE payments ALTER COLUMN qr_code_url TYPE TEXT;
