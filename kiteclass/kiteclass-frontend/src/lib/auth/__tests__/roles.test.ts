/**
 * Role model tests — KiteClass RBAC foundation (Wave RBAC-Shell 1 Bucket A, GAP-1122).
 *
 * Covers the role-name parity reconcile (Risk #3): BE emits several role
 * vocabularies (tenant-auth Option B `entity_type`, ADR-003 hierarchical Role
 * names, @PreAuthorize literals) that the FE normalizes into one canonical set
 * before any redirect / guard decision.
 *
 * @author KiteClass Team
 */

import { describe, it, expect } from 'vitest';
import { normalizeRole, roleHome, canAccess } from '../roles';
import { UserType } from '@/types/auth';

describe('normalizeRole', () => {
  it('passes through canonical KC roles', () => {
    expect(normalizeRole('OWNER')).toBe(UserType.OWNER);
    expect(normalizeRole('STAFF')).toBe(UserType.STAFF);
    expect(normalizeRole('TEACHER')).toBe(UserType.TEACHER);
    expect(normalizeRole('PARENT')).toBe(UserType.PARENT);
    expect(normalizeRole('STUDENT')).toBe(UserType.STUDENT);
    expect(normalizeRole('ADMIN')).toBe(UserType.ADMIN);
  });

  it('maps BE hierarchical role names (ADR-003 / V30) to FE canonical', () => {
    expect(normalizeRole('TENANT_OWNER')).toBe(UserType.OWNER);
    expect(normalizeRole('PLATFORM_ADMIN')).toBe(UserType.ADMIN);
    expect(normalizeRole('PRINCIPAL')).toBe(UserType.ADMIN);
    expect(normalizeRole('VICE_PRINCIPAL')).toBe(UserType.ADMIN);
    expect(normalizeRole('SUBJECT_TEACHER')).toBe(UserType.TEACHER);
    expect(normalizeRole('HOMEROOM_TEACHER')).toBe(UserType.TEACHER);
    expect(normalizeRole('ACCOUNTANT')).toBe(UserType.STAFF);
    expect(normalizeRole('RECEPTIONIST')).toBe(UserType.STAFF);
  });

  it('is case-insensitive and trims whitespace', () => {
    expect(normalizeRole('  teacher ')).toBe(UserType.TEACHER);
    expect(normalizeRole('Tenant_Owner')).toBe(UserType.OWNER);
  });

  it('returns null for unknown / empty role tokens', () => {
    expect(normalizeRole('GHOST')).toBeNull();
    expect(normalizeRole('')).toBeNull();
    expect(normalizeRole(null)).toBeNull();
    expect(normalizeRole(undefined)).toBeNull();
  });
});

describe('roleHome', () => {
  it('routes role-specific personas to their own home', () => {
    expect(roleHome(UserType.TEACHER)).toBe('/teacher');
    expect(roleHome(UserType.PARENT)).toBe('/parent');
    expect(roleHome(UserType.STUDENT)).toBe('/student');
  });

  it('routes owner/staff/admin to the shared dashboard shell', () => {
    expect(roleHome(UserType.OWNER)).toBe('/dashboard');
    expect(roleHome(UserType.STAFF)).toBe('/dashboard');
    expect(roleHome(UserType.ADMIN)).toBe('/dashboard');
  });
});

describe('canAccess', () => {
  it('allows when role is in the allow-list', () => {
    expect(canAccess(UserType.TEACHER, [UserType.TEACHER])).toBe(true);
    expect(canAccess(UserType.OWNER, [UserType.OWNER, UserType.ADMIN])).toBe(true);
  });

  it('denies when role is outside the allow-list (IDOR-by-navigation guard)', () => {
    expect(canAccess(UserType.TEACHER, [UserType.OWNER, UserType.ADMIN])).toBe(false);
    expect(canAccess(UserType.PARENT, [UserType.STUDENT])).toBe(false);
  });

  it('denies a null/undefined role', () => {
    expect(canAccess(null, [UserType.TEACHER])).toBe(false);
    expect(canAccess(undefined, [UserType.TEACHER])).toBe(false);
  });
});
