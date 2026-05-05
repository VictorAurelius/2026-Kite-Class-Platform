---
title: Wave 99.3 — fixture forward
status: draft
created: 2026-99-03
---

# Wave 99.3 — fixture forward-flagged

## 3. Scope

### Bucket A — fees + new component

Wire `Invoice.totalAmount` and add new `BR-FEES-002` rule.

## 4. State-Check Evidence

| Symbol | Type | Verification | Evidence | Verdict |
|---|---|---|---|---|
| `Invoice.totalAmount` | Java field | grep | 12 matches | ✅ exists |
| `BR-FEES-002` | Business rule | grep | 0 matches | 🆕 to-be-created (Bucket A) |

## 5. Verification Gates
