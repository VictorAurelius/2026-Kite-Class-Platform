/**
 * Type definitions for G6 Invoice Detail panel.
 *
 * Mirrors `ui_kits/components/G6-invoice-detail/spec.md` Props block.
 * See also: `dossier/04-component-gaps.md` §G6 and the 5 HTML state files.
 */

export type InvoiceStatus =
  | 'PENDING_PAYMENT'
  | 'PARTIAL_PAID'
  | 'PAID'
  | 'OVERDUE'
  | 'VOID';

export type InvoiceState =
  | 'loading'
  | 'default'
  | 'pending'
  | 'overdue'
  | 'paid'
  | 'print-view';

/** A single billable line on an invoice (course fee, book, late fee, ...). */
export type InvoiceLineItem = {
  /** Title shown in the description column. */
  title: string;
  /** Quantity column value. */
  quantity: number;
  /** Unit price in VND (no decimal — VND has no fractional unit). */
  unitPrice: number;
  /** Computed line total = `quantity * unitPrice` (caller-precomputed). */
  lineTotal: number;
  /** Optional sub-text rendered under `title` (e.g. semester / pack / SKU). */
  meta?: string;
  /**
   * Optional intent flag to style the row (e.g. late-fee row paints destructive).
   * Component does NOT compute this — caller decides.
   */
  intent?: 'default' | 'destructive';
};

/** A single discount / scholarship row in the totals block. */
export type InvoiceDiscount = {
  label: string;
  /** Positive amount; rendered with a leading minus in the totals. */
  amount: number;
};

/** Optional VAT block (VN tax-invoice format only). */
export type InvoiceTaxBreakdown = {
  /**
   * Tax rate. Accepted both as decimal (0.08) or whole number (8).
   * `formatVNTax` normalizes both.
   */
  rate: number;
  /** Computed VAT amount in VND. */
  amount: number;
};

export type InvoiceStudent = {
  fullName: string;
  className: string;
};

export type InvoiceTenant = {
  name: string;
  address: string;
  /** VN business tax code ("Mã số thuế"). Optional for non-VAT invoices. */
  mst?: string;
};

export type InvoiceData = {
  /** Invoice number e.g. "KH-2026-04-001" (`KH` for KiteClass, `KHB` for KiteHub billing). */
  number: string;
  status: InvoiceStatus;
  issueDate: Date;
  dueDate: Date;
  /** Set when status === PAID or PARTIAL_PAID. */
  paidDate?: Date;
  items: InvoiceLineItem[];
  discounts: InvoiceDiscount[];
  vat?: InvoiceTaxBreakdown;
  /** Subtotal before discounts + VAT. */
  subtotal: number;
  /** Final total payable. */
  total: number;
  /** Outstanding balance (0 once fully paid). */
  balance: number;
  student: InvoiceStudent;
  tenant: InvoiceTenant;
  /** When true, renders the VN tax-invoice header block (Nghị định 123/2020/NĐ-CP). */
  isVATInvoice?: boolean;
};

export type InvoiceDetailProps = {
  invoice: InvoiceData;
  state: InvoiceState;
  /** Pending / overdue states render this CTA. */
  onPayNow?: () => void;
  /** Paid state renders "Tải biên lai" / "Tải PDF". */
  onDownloadPdf?: () => void;
  /** Paid state renders "Gửi qua email". */
  onSendEmail?: () => void;
  /**
   * Override the `lang` attribute applied to the wrapper. Defaults to `'vi'`.
   * `'en'` falls back to `'vi'` for v1 (Vietnamese-first per CLAUDE.md).
   */
  lang?: 'vi' | 'en';
};
