---
title: Wave 99.1 — fixture good
status: draft
created: 2026-99-01
---

# Wave 99.1 — fixture good

## 3. Scope

### Bucket A — fees facet

Wire `Invoice.totalAmount` and `BR-FEES-001` into parent portal projection.

## 4. State-Check Evidence

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `Invoice.totalAmount` | Java field | grep | 12 matches | ✅ exists |
| `BR-FEES-001` | Business rule | grep | 3 matches | ✅ exists |

## 5. Verification Gates
