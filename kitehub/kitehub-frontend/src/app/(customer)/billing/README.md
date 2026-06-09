# Billing Module

## Overview
Comprehensive subscription and billing management system for KiteHub Frontend.

## Features

### 1. Billing Overview (`/billing`)
- Current subscription plan display
- Billing cycle progress bar
- Auto-renewal status
- Pending tier changes notification
- Plan comparison grid (FREE, BASIC, PREMIUM, ENTERPRISE)

### 2. Upgrade/Downgrade Flow (`/billing/upgrade`)
- Multi-step wizard (Select → Confirm)
- Interactive tier selection
- Feature comparison
- Prorated charge calculation for upgrades
- Scheduled downgrades (effective at cycle end)

### 3. Payment Page (`/billing/payment/[id]`)
- VietQR code display
- QR code expiry countdown timer
- Auto-polling every 5s for PENDING payments
- Auto-redirect to billing page on COMPLETED
- Payment info with copy-to-clipboard
- Status transitions (PENDING → COMPLETED/FAILED/EXPIRED)

### 4. Payment History (`/billing/history`)
- DataTable with filtering and sorting
- Filter by status (ALL/COMPLETED/PENDING/FAILED/EXPIRED)
- Sort by date (newest/oldest)
- Transaction summary (total count and value)
- Quick navigation to payment details

## Components

### Layout Components
- `CurrentPlanCard.tsx` - Current subscription summary
- `PlanComparison.tsx` - Tier comparison grid
- `TierSelector.tsx` - Interactive tier selection
- `ChangeConfirmation.tsx` - Upgrade/downgrade confirmation
- `StepIndicator.tsx` - Multi-step progress indicator

### Payment Components
- `QRCodeDisplay.tsx` - QR code with countdown timer
- `PaymentInfo.tsx` - Transaction details with clipboard
- `PaymentStatusCard.tsx` - Status alerts
- `PaymentHistoryTable.tsx` - Payment list with filters

## Hooks

### Subscriptions (`use-subscriptions.ts`)
- `useActiveSubscription(instanceId)` - Get active subscription
- `useSubscriptionHistory(instanceId)` - Get subscription history
- `useUpgradeSubscription()` - Upgrade to higher tier
- `useDowngradeSubscription()` - Schedule downgrade

### Payments (`use-payments.ts`)
- `usePayment(paymentId)` - Get payment with auto-polling
- `usePaymentHistory(subscriptionId)` - Get payment history
- `useCreatePayment()` - Create new payment

## Utilities

### Pricing (`lib/pricing.ts`)
- `PLAN_DETAILS` - Tier configuration
- `calculateProration()` - Mid-cycle upgrade pricing
- `getDaysRemaining()` - Days until expiry
- `isUpgrade()` - Check tier change direction
- `formatPrice()` - Currency formatting

### Error Handling (`lib/error-handler.ts`)
- `handleApiError()` - Extract error messages
- `showErrorToast()` - Display error toast
- `showSuccessToast()` - Display success toast
- `isUnauthorizedError()` - Check 401 status
- `isValidationError()` - Check validation errors

### Validation (`lib/validation.ts`)
- Zod schemas for tier, billing cycle, payment method
- `validateTierChange()` - Tier change validation
- `validateAmount()` - Payment amount validation
- `validateExpiryDate()` - QR code expiry validation

## User Flows

### Upgrade Flow
1. User clicks "Thay đổi gói" on billing overview
2. Select higher tier (e.g., FREE → BASIC)
3. Review prorated charge and feature changes
4. Confirm → Creates payment with VietQR
5. Redirects to `/billing/payment/[id]`
6. User scans QR code or transfers manually
7. Page auto-polls every 5s for status
8. On COMPLETED: auto-redirect to `/billing?success=upgrade`

### Downgrade Flow
1. User clicks "Thay đổi gói" on billing overview
2. Select lower tier (e.g., PREMIUM → BASIC)
3. Review notice: change effective at cycle end
4. Confirm → Schedules downgrade (no payment needed)
5. Redirects to `/billing?success=downgrade`
6. Pending tier shown on CurrentPlanCard until cycle ends

### Payment History Flow
1. User clicks "Lịch sử" button on CurrentPlanCard
2. View all payments with filters
3. Filter by status or sort by date
4. Click "Xem" to view payment details
5. Navigates to `/billing/payment/[id]` for details

## API Integration

### Endpoints Used
- `GET /api/subscriptions/instance/{instanceId}/active` - Active subscription
- `GET /api/subscriptions/instance/{instanceId}` - Subscription history
- `PATCH /api/platform/subscriptions/{id}/upgrade` - Upgrade tier
- `PATCH /api/platform/subscriptions/{id}/downgrade` - Downgrade tier
- `POST /api/payments` - Create payment
- `GET /api/payments/{id}` - Payment details
- `GET /api/payments/subscription/{subscriptionId}` - Payment history

### Response Types
- Uses UUID strings for all IDs (not numbers)
- Backend fields: `priceVnd`, `startedAt`, `expiresAt`, `amountVnd`
- Frontend aligns with backend DTOs

## Styling

### Color Semantics
- **Green** - Upgrades, success, completed payments
- **Orange** - Downgrades, warnings
- **Blue** - Pending payments, info alerts
- **Red** - Failed payments, errors

### Responsive Design
- Mobile-first approach
- Grid layouts for tier comparison
- Single-column on mobile, multi-column on desktop
- Touch-friendly buttons and links

## Testing Checklist

- [ ] Upgrade flow creates payment
- [ ] Downgrade flow schedules tier change
- [ ] Payment auto-polling updates status
- [ ] QR countdown expires correctly
- [ ] Auto-redirect on payment success
- [ ] Filters work in payment history
- [ ] Copy-to-clipboard functions
- [ ] Mobile responsive layouts
- [ ] Error toast displays on API errors
- [ ] Sidebar navigation active states

## Future Enhancements

- [ ] Multiple payment methods (MOMO, VNPAY)
- [ ] Invoice generation and download
- [ ] Payment receipt emails
- [ ] Subscription analytics dashboard
- [ ] Usage-based billing
- [ ] Promo codes and discounts
- [ ] Team billing (multi-user accounts)
- [ ] Export payment history to CSV
