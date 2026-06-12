-- GAP-1221: contact form public — email optional (phụ huynh VN quen để SĐT)
-- Validate format vẫn áp khi email có giá trị (bean validation @Email).
ALTER TABLE contact_messages ALTER COLUMN email DROP NOT NULL;
