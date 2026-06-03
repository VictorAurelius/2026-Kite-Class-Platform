# GAP-905: `incidents.description`/`evidence_paths` BYTEA AES-GCM — key rotation flow

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC compliance)
**Affects:** `kiteclass-core` module compliance; `incidents` table BYTEA columns

## Problem

`incidents.description` + `evidence_paths` BYTEA mã hóa AES-256-GCM (`[IV(12) | ciphertext | auth_tag(16)]`). Decrypt CHỈ qua `AesGcmAttributeConverter`. Hệ quả:

- Debug/triage qua psql phải skip 2 cột (chỉ thấy title plaintext)
- Backup/restore PHẢI bao gồm key material (KMS / env). Mất key = data loss vĩnh viễn
- Search-by-content impossible
- Migration đổi key/algorithm cần data migration mã hóa lại từng row → tracked GAP-322b key rotation flow

Phase 1B GAP-322b mã hóa bucket MinIO (bucket-level encryption) — defense in depth.

## Proposed Fix

Implement GAP-322b key rotation flow (data migration re-encrypt all rows). Document backup/restore runbook bao gồm KMS key material. Add CI check key material backup invariant.

## Acceptance Criteria

- [ ] GAP-322b implementation (key rotation)
- [ ] Backup/restore runbook + KMS dependency documented
- [ ] CI check key material backup
- [ ] Reference cluster doc 07-compliance-audit §A3

## Discovered in

`documents/02-architecture/database/kiteclass/07-compliance-audit.md` §A3; sister GAP-322b
