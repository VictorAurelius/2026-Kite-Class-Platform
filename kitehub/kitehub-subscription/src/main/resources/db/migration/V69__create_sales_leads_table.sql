-- GAP-1101: KiteHub PLATFORM sales lead capture (Enterprise "Liên hệ" CTA).
--
-- Distinct from kiteclass-core tenant-marketing leads/contact_messages domain
-- (student → center). THIS table is the KiteHub PLATFORM sales funnel:
-- prospective center owner → KiteHub sales about the Enterprise SaaS plan.
-- Endpoint: POST /api/platform/sales-leads (public, no auth — prospective
-- customer not logged in; rate-limited at gateway 2 req/sec/IP).
--
-- Schema mirror of SalesLead entity. Highest existing migration = V67; this = V68.
--
-- Columns:
--   id                — internal BIGSERIAL primary key
--   public_id         — UUID exposed to client (SalesLeadResponse.id)
--   full_name         — VARCHAR(200) NOT NULL (UTF-8 Vietnamese)
--   email             — VARCHAR(320) NOT NULL (RFC-5321 length)
--   phone             — VARCHAR(20) NOT NULL (VN sales contact channel)
--   organization_name — VARCHAR(200) NOT NULL (center name)
--   message           — TEXT nullable (free-text consultation request)
--   plan_interest     — VARCHAR(50) NOT NULL DEFAULT 'ENTERPRISE'
--   status            — VARCHAR(50) NOT NULL DEFAULT 'NEW' (NEW/CONTACTED/QUALIFIED/CLOSED)
--   client_ip         — VARCHAR(45) nullable (IPv6-safe, spam audit)
--   created_at        — TIMESTAMPTZ NOT NULL
--   updated_at        — TIMESTAMPTZ NOT NULL
--
-- Indexes:
--   - idx_sales_leads_status_created (status, created_at DESC) for admin queue
--   - idx_sales_leads_email_created (email, created_at DESC) for dedup lookup

CREATE TABLE IF NOT EXISTS sales_leads (
    id                BIGSERIAL PRIMARY KEY,
    public_id         UUID NOT NULL UNIQUE,
    full_name         VARCHAR(200) NOT NULL,
    email             VARCHAR(320) NOT NULL,
    phone             VARCHAR(20) NOT NULL,
    organization_name VARCHAR(200) NOT NULL,
    message           TEXT,
    plan_interest     VARCHAR(50) NOT NULL DEFAULT 'ENTERPRISE',
    status            VARCHAR(50) NOT NULL DEFAULT 'NEW',
    client_ip         VARCHAR(45),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sales_leads_status_created
    ON sales_leads (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sales_leads_email_created
    ON sales_leads (email, created_at DESC);

COMMENT ON TABLE sales_leads IS
    'GAP-1101 — KiteHub PLATFORM sales lead (Enterprise CTA). Distinct from '
    'kiteclass-core tenant-marketing leads (student->center). Public submit via '
    'POST /api/platform/sales-leads.';
