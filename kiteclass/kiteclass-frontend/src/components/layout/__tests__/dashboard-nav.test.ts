/**
 * Role-aware dashboard nav tests — Wave RBAC-Shell 1 Bucket B (GAP-1119).
 *
 * Verifies the OWNER/ADMIN full surface vs the STAFF operational subset, and the
 * hydration fallback (null role → owner view, never empty).
 *
 * @author KiteClass Team
 */

import { describe, it, expect } from 'vitest';
import { navItemsForRole, DASHBOARD_NAV } from '../dashboard-nav';
import { UserType } from '@/types/auth';

const titles = (role: Parameters<typeof navItemsForRole>[0]) =>
  navItemsForRole(role).map((i) => i.title);

// 'Phân quyền' (/admin/roles) removed from nav for Phase 1 BETA — GAP-1417
// (user_roles assign layer not wired into authz; deferred Phase 3 per GAP-1119).
const OWNER_ONLY = ['Giáo viên', 'Khóa học', 'Báo cáo', 'Bảng lương', 'Thương hiệu'];
const SHARED = ['Tổng quan', 'Học viên', 'Lớp học', 'Điểm danh', 'Học phí', 'Cài đặt'];

describe('navItemsForRole', () => {
  it('OWNER sees the full school-management surface', () => {
    const t = titles(UserType.OWNER);
    for (const item of [...SHARED, ...OWNER_ONLY]) {
      expect(t).toContain(item);
    }
    expect(t).toHaveLength(DASHBOARD_NAV.length);
  });

  it('ADMIN sees the same full surface as OWNER', () => {
    expect(titles(UserType.ADMIN)).toEqual(titles(UserType.OWNER));
  });

  it('STAFF sees only the operational subset (no owner governance)', () => {
    const t = titles(UserType.STAFF);
    expect(t).toEqual(SHARED);
    for (const ownerOnly of OWNER_ONLY) {
      expect(t).not.toContain(ownerOnly);
    }
  });

  it('TEACHER / PARENT / STUDENT get no dashboard nav (own shells)', () => {
    // These personas never render the (dashboard) sidebar — but verify the
    // explicit allow-lists exclude them so an accidental mount shows nothing
    // role-specific leaked from owner/staff.
    expect(DASHBOARD_NAV.every((i) => !i.roles.includes(UserType.TEACHER))).toBe(true);
    expect(DASHBOARD_NAV.every((i) => !i.roles.includes(UserType.PARENT))).toBe(true);
    expect(DASHBOARD_NAV.every((i) => !i.roles.includes(UserType.STUDENT))).toBe(true);
  });

  it('null role (hydration) falls back to the OWNER view — never empty', () => {
    expect(titles(null)).toEqual(titles(UserType.OWNER));
    expect(navItemsForRole(undefined).length).toBeGreaterThan(0);
  });

  it('does NOT expose the role-assign item in Phase 1 (gated — GAP-1417)', () => {
    // user_roles assign is not wired into authz; the /admin/roles page is a
    // read-only overview and must not be discoverable via the owner nav.
    expect(DASHBOARD_NAV.find((i) => i.title === 'Phân quyền')).toBeUndefined();
    expect(DASHBOARD_NAV.find((i) => i.href === '/admin/roles')).toBeUndefined();
  });
});
