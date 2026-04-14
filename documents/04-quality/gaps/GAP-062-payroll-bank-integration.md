# GAP-062: Payroll Bank Integration (Batch Transfer)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / Integration
**Persona blocked:** P3, P4, P5
**Detected:** 2026-04-14

## Problem

Sau khi calculate payroll (GAP-057), admin phải manually transfer mỗi teacher → slow + error.

## Proposed Fix

### Bank Integrations

VN banks common:
- Vietcombank, BIDV, Techcombank, VPBank, MB Bank
- Most support **bulk transfer file upload** (.csv/.xls standard)
- Some support API (enterprise)

### Export Format

Generate bank-compliant file per bank:
```csv
AccountNumber,AccountName,Amount,Description
1234567890,NGUYEN VAN A,15000000,Luong thang 04/2026
```

### Workflow

1. Approve payroll batch
2. Select bank
3. Generate file
4. Download + upload to bank portal
5. (Future: API integration for auto-send)
6. Reconcile successful transfers

## Acceptance Criteria

- [ ] Export format cho 5+ VN banks
- [ ] Batch approval workflow
- [ ] Reconciliation tracking
- [ ] Future: API integration roadmap

## Dependencies

- GAP-057 (payroll calculation)

## Log
- 2026-04-14 — Persona review
