-- GAP-882 (wave-p0-1 Bucket A) — canonical UPPERCASE invoice enum CHECK alignment.
--
-- Drift found (empirical state-check 2026-06-07 per audit-to-gap-pipeline.md §2.8):
--   (1) invoices.status  — ALREADY harmonized to UPPERCASE by V86__type_harmonize.sql
--       (CHECK = DRAFT/SENT/PARTIAL/PAID/OVERDUE/CANCELLED/REFUNDED, default DRAFT).
--       This migration RE-ASSERTS the constraint idempotently (defensive: catches any
--       lowercase rows that could have slipped in between V86 and now). No behavior change
--       on a clean chain where V86 already ran — DROP IF EXISTS + re-ADD is a no-op.
--   (2) invoice_items.item_type — VARCHAR(50) with NO CHECK constraint at all; legacy column
--       comment said lowercase ("tuition, material, other"). Java entity InvoiceItemType
--       persists UPPERCASE names (TUITION/MATERIALS/REGISTRATION_FEE/EXAM_FEE/OTHER) via
--       @Enumerated(EnumType.STRING). This migration adds the missing CHECK guard so the DB
--       enforces the same canonical set as the Java enum (item_type is nullable → NULL allowed).
--
-- Canonical source = Java enums (per design-patterns.md — entity is source of truth):
--   InvoiceStatus    = DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED, REFUNDED
--   InvoiceItemType  = TUITION, MATERIALS, REGISTRATION_FEE, EXAM_FEE, OTHER

-- =====================================================================
-- (1) invoices.status — defensive idempotent re-assertion (V86 already canonical)
-- =====================================================================
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_status;

-- Backfill any residual lowercase values → UPPERCASE (no-op on V86-harmonized data).
UPDATE invoices
SET status = CASE status
    WHEN 'draft' THEN 'DRAFT'
    WHEN 'pending' THEN 'SENT'
    WHEN 'partially_paid' THEN 'PARTIAL'
    WHEN 'paid' THEN 'PAID'
    WHEN 'overdue' THEN 'OVERDUE'
    WHEN 'cancelled' THEN 'CANCELLED'
    WHEN 'refunded' THEN 'REFUNDED'
    ELSE UPPER(status)
END
WHERE status IS NOT NULL
  AND status <> UPPER(status);

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_status
    CHECK (status IN ('DRAFT', 'SENT', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED'));

-- =====================================================================
-- (2) invoice_items.item_type — NEW canonical CHECK (was previously unconstrained)
-- =====================================================================
ALTER TABLE invoice_items DROP CONSTRAINT IF EXISTS chk_invoice_items_type;

-- Backfill any legacy lowercase values → UPPERCASE canonical names.
-- Explicit map for known legacy lowercase tokens (UPPER('material') would yield the
-- non-canonical 'MATERIAL'; map it to the plural enum value 'MATERIALS').
UPDATE invoice_items
SET item_type = CASE LOWER(item_type)
    WHEN 'tuition' THEN 'TUITION'
    WHEN 'material' THEN 'MATERIALS'
    WHEN 'materials' THEN 'MATERIALS'
    WHEN 'registration_fee' THEN 'REGISTRATION_FEE'
    WHEN 'exam_fee' THEN 'EXAM_FEE'
    WHEN 'other' THEN 'OTHER'
    ELSE UPPER(item_type)
END
WHERE item_type IS NOT NULL
  AND item_type <> UPPER(item_type);

ALTER TABLE invoice_items
    ADD CONSTRAINT chk_invoice_items_type
    CHECK (item_type IS NULL OR item_type IN ('TUITION', 'MATERIALS', 'REGISTRATION_FEE', 'EXAM_FEE', 'OTHER'));

COMMENT ON COLUMN invoice_items.item_type IS
    'UPPERCASE enum value aligned with InvoiceItemType (TUITION/MATERIALS/REGISTRATION_FEE/EXAM_FEE/OTHER). Nullable. CHECK added by V92 (GAP-882).';
