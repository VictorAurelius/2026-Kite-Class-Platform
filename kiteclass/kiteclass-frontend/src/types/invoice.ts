export interface Invoice {
  id: number;
  invoiceNumber: string;
  studentId: number;
  classId: number;
  enrollmentId: number;
  status: InvoiceStatus;
  issueDate: string; // LocalDate from backend
  dueDate: string;
  periodStart: string;
  periodEnd: string;
  subtotal: number;
  discount: number;
  total: number;
  amountPaid: number;
  balanceDue: number;
  paidAt?: string;
  notes?: string;
  items: InvoiceItem[];
  adjustments: InvoiceAdjustment[];
  createdAt: string;
  updatedAt: string;
}

export enum InvoiceStatus {
  DRAFT = 'DRAFT',
  // GAP-1432: BE issues SENT + PARTIAL statuses; without them the FE status map
  // fell through and action buttons (record-payment etc.) never rendered.
  SENT = 'SENT',
  PENDING = 'PENDING',
  PARTIAL = 'PARTIAL',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED',
}

export interface InvoiceItem {
  id: number;
  type: InvoiceItemType;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  referenceId?: number;
}

export enum InvoiceItemType {
  TUITION = 'TUITION',
  MATERIAL = 'MATERIAL',
  REGISTRATION = 'REGISTRATION',
  EXAM = 'EXAM',
  OTHER = 'OTHER',
}

export interface InvoiceAdjustment {
  id: number;
  type: InvoiceAdjustmentType;
  description: string;
  amount: number;
  reason?: string;
}

export enum InvoiceAdjustmentType {
  DISCOUNT = 'DISCOUNT',
  LATE_FEE = 'LATE_FEE',
  PENALTY = 'PENALTY',
  WAIVER = 'WAIVER',
  OTHER = 'OTHER',
}

export interface CreateInvoiceRequest {
  studentId: number;
  classId?: number;
  issueDate?: string;
  dueDate: string;
  periodStart: string;
  periodEnd: string;
  notes?: string;
}

export interface ApplyAdjustmentRequest {
  type: InvoiceAdjustmentType;
  description: string;
  amount: number;
  reason?: string;
}

export interface InvoiceSearchParams {
  studentId?: number;
  status?: InvoiceStatus;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Batch monthly invoice (GAP-297).
 * Per api-contract.md §3.11/§3.12 — preview + confirm cho học phí hàng tháng.
 */

/** Một dòng line item trong preview batch hóa đơn tháng (per enrollment). */
export interface BatchInvoiceLineItem {
  enrollmentId: number;
  studentId: number;
  classId: number;
  classNameVi: string;
  tuitionAmount: number;
  discountPercent: number;
  proratedTuition: number;
  discountAmount: number;
  total: number;
  prorated: boolean;
  billableDays: number;
  daysInMonth: number;
}

/** Response của POST /api/v1/invoices/batch-generate (preview, KHÔNG persist). */
export interface BatchInvoicePreviewResponse {
  month: string; // yyyy-MM
  invoiceCount: number;
  totalRevenue: number;
  invoices: BatchInvoiceLineItem[];
}

/** Response của POST /api/v1/invoices/batch-confirm (persist + emit events). */
export interface BatchInvoiceConfirmResponse {
  month: string; // yyyy-MM
  createdCount: number;
  skippedCount: number;
  totalRevenue: number;
  createdInvoiceIds: number[];
}
