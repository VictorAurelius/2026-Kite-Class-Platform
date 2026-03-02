# Invoice Module

Event-driven invoice management với auto-creation, financial calculations, installment plans, và refund workflows.

## Features

✅ **Auto Invoice Creation** - Event-driven invoice generation from enrollment
✅ **Financial Calculations** - Auto-calculate totals with @PrePersist
✅ **Adjustments** - Discounts, late fees, additional charges, refunds
✅ **Installment Plans** - 2-12 installments with approval workflow
✅ **Refund Management** - Refund request với admin approval
✅ **Multi-tenant** - Full tenant isolation support

## Architecture

### Event Flow
```
Enrollment Created → ENROLLMENT_CREATED Event
  → EnrollmentEventListener
  → Auto-create Invoice
  → INVOICE_CREATED Event (for future Payment Module)
```

### Entities

- **Invoice** - Main invoice entity with items and adjustments
- **InvoiceItem** - Line items (tuition, materials, fees)
- **InvoiceAdjustment** - Discounts, fees, refunds
- **InstallmentPlan** - Payment plan (2-12 installments)
- **Installment** - Individual installment
- **RefundRequest** - Refund workflow with approval

### Services

- **InvoiceService** - Invoice operations, late fees
- **InstallmentPlanService** - Plan creation, approval, payment
- **RefundRequestService** - Refund request workflow
- **InvoiceNumberGenerator** - Thread-safe sequence (INV-YYYY-NNNNNN)

## Business Rules

- **BR-INV-001**: Auto-generate invoice from enrollment
- **BR-INV-002**: Calculate total = subtotal + adjustments
- **BR-INV-003**: Track payment status (SENT → PARTIAL → PAID)
- **BR-INV-004**: balanceDue = total - amountPaid (computed)
- **BR-INV-005**: One invoice per enrollment
- **BR-INV-006**: Late fee calculation (0.1% per day overdue)

## API Endpoints

### Invoice
- `GET /api/v1/invoices/{id}` - Get invoice by ID
- `GET /api/v1/invoices?studentId={id}` - Get invoices by student
- `POST /api/v1/invoices/{id}/adjustments` - Apply adjustment
- `POST /api/v1/invoices/{id}/late-fees` - Calculate late fees
- `GET /api/v1/invoices/overdue` - Get overdue invoices
- `PUT /api/v1/invoices/{id}/cancel` - Cancel invoice

### Installment Plan
- `POST /api/v1/installment-plans` - Request plan
- `GET /api/v1/installment-plans/{id}` - Get plan by ID
- `PUT /api/v1/installment-plans/{id}/approve` - Approve plan (admin)
- `PUT /api/v1/installment-plans/{id}/reject` - Reject plan (admin)
- `POST /api/v1/installment-plans/installments/{id}/payment` - Record payment

### Refund Request
- `POST /api/v1/refund-requests` - Create refund request
- `GET /api/v1/refund-requests/{id}` - Get request by ID
- `PUT /api/v1/refund-requests/{id}/approve` - Approve (admin)
- `PUT /api/v1/refund-requests/{id}/reject` - Reject (admin)
- `POST /api/v1/refund-requests/{id}/process` - Process refund

## Local Testing

### Run Unit Tests
```bash
cd kiteclass/kiteclass-core
./mvnw test -Dtest=InvoiceNumberGeneratorTest
./mvnw test -Dtest=InvoiceServiceTest
```

### Run All Invoice Tests
```bash
./scripts/test-local.sh core
```

### Cleanup Testcontainers
```bash
./scripts/cleanup-testcontainers.sh
```

## Database Migration

**V12__create_invoice_extended_tables.sql**:
- Alters `invoices` table (adds enrollment_id, paid_at, deleted)
- Creates `invoice_adjustments` table
- Creates `installment_plans` table
- Creates `installments` table
- Creates `refund_requests` table

## Example Usage

### Create Enrollment (auto-creates invoice)
```bash
curl -X POST http://localhost:8081/api/v1/enrollments \
  -H "X-Tenant-Id: {tenantId}" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "classId": 1,
    "tuitionAmount": 1000.00,
    "discountPercent": 10
  }'

# Invoice auto-created via event listener
# INV-2026-000001 with 10% discount adjustment
```

### Apply Late Fee
```bash
curl -X POST http://localhost:8081/api/v1/invoices/1/late-fees \
  -H "X-Tenant-Id: {tenantId}"
```

### Request Installment Plan
```bash
curl -X POST http://localhost:8081/api/v1/installment-plans \
  -H "X-Tenant-Id: {tenantId}" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": 1,
    "numberOfInstallments": 4
  }'
```

## Code Structure

```
invoice/
├── controller/          # REST controllers (3)
├── dto/                 # Request/Response DTOs (10)
├── entity/              # JPA entities (6)
├── event/               # Application events (1)
├── listener/            # Event listeners (1)
├── mapper/              # MapStruct mapper (1)
├── repository/          # Spring Data repositories (3)
└── service/             # Business logic (4 services)
```

## Dependencies

- **Enrollment Module** - Event source for auto-creation
- **Class Module** - Class info for invoice items
- **Course Module** - Course info for descriptions
- **Student Module** - Student reference

## Future Enhancements

- **Payment Module** - Integration with payment gateways (PR 2.9)
- **Notifications** - Email/SMS for invoice events
- **Recurring Invoices** - Monthly/quarterly billing
- **Invoice Templates** - Customizable PDF generation

---

**PR**: #2.8
**Status**: Completed
**Coverage**: >= 80%
