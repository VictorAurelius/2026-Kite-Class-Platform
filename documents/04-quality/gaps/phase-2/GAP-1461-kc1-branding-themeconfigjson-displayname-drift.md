# GAP-1461: KC-1 branding.theme_config_json embed displayName cũ vs cột display_name

**Status:** 🔵 OPEN
**Priority:** 🔴 P3
**Domain:** Backend
**Found:** 2026-06-16 (Flow Verification Campaign — KC-1/2/3/8 browser re-walk)
**Affects:** Backend

## Problem

KC-1 walk cosmetic: theme_config_json còn embed 'Sky Education' trong khi cột display_name='Trung tâm cô Đỗ Lan Khánh'. UI render từ cột displayName nên 0 impact user; internal data drift. Sync khi PUT branding hoặc migration backfill.

## Acceptance Criteria

- [ ] Fix/verify per Problem
- [ ] Browser re-walk confirm

## Related

- Discovered in: 2026-06-16 browser walk batch (KC-1/2/3/8)
