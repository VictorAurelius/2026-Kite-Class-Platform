# GAP-088: Rollback Procedure Per Service

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (trước production)
**Domain:** Operations / DevOps
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** All deployed services

## Problem

Deploy xong → phát hiện bug. Không có documented rollback steps per service. Mỗi service có rollback strategy khác nhau:
- Frontend: Vercel/k8s rollback to previous image
- Backend: k8s rollback + check DB migration compatibility
- Database: migration KHÔNG rollback tự động (Flyway V-migrations one-way)
- Infrastructure: Terraform state rollback cần manual

## Proposed Fix

1. Tạo `documents/05-guides/rollback-procedures.md` per service:

   | Service | Rollback Method | Time | Risk |
   |---------|----------------|------|------|
   | kiteclass-frontend | k8s rollback to previous tag | 2 min | Low |
   | kiteclass-core | k8s rollback + verify DB compat | 5 min | Medium |
   | kitehub-gateway | k8s rollback | 2 min | Low |
   | kitehub-* (6 services) | k8s rollback per service | 2 min each | Low-Medium |
   | Database | Manual SQL undo script | 10-30 min | HIGH |
   | Infrastructure | `terraform plan` → manual review | 15 min | HIGH |

2. Tạo scripts: `scripts/rollback-service.sh <service> <version>`
3. Test rollback in staging trước production

## Acceptance Criteria

- [ ] Rollback doc exists per service type
- [ ] Rollback script exists (at least for k8s services)
- [ ] Database rollback: undo scripts for last 5 migrations
- [ ] At least 1 rollback drill completed
