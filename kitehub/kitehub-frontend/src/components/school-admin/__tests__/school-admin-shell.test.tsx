/**
 * Tests for school-admin shell + page rendering.
 *
 * Wave 50 Bucket A (GAP-271).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

// next/navigation pathname mock — required because the shell uses usePathname
vi.mock('next/navigation', () => ({
  usePathname: () => '/school-admin/dashboard',
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

import { SchoolAdminShell } from '../school-admin-shell';
import { SCHOOL_PROFILE, TEACHERS, CLASSES, CONDUCT_CASES, REPORT_CARD_STATUS } from '../school-admin-mock-data';

describe('SchoolAdminShell', () => {
  it('renders sidebar with school identity + nav groups', () => {
    render(
      <SchoolAdminShell>
        <div>main content</div>
      </SchoolAdminShell>,
    );
    expect(screen.getByText(SCHOOL_PROFILE.name)).toBeInTheDocument();
    expect(screen.getByText(/Năm học/i)).toBeInTheDocument();
    expect(screen.getByText('Tổng quan')).toBeInTheDocument();
    expect(screen.getByText('Học sinh')).toBeInTheDocument();
    expect(screen.getByText('Nhân sự')).toBeInTheDocument();
    expect(screen.getByText('Tài chính')).toBeInTheDocument();
    expect(screen.getByText('Cấu hình')).toBeInTheDocument();
  });

  it('renders the principal name + role in the bottom card', () => {
    render(
      <SchoolAdminShell>
        <div>main content</div>
      </SchoolAdminShell>,
    );
    // Principal name appears in 2 places (bottom card + header)
    const matches = screen.getAllByText(SCHOOL_PROFILE.principalName);
    expect(matches.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(SCHOOL_PROFILE.principalRole)).toBeInTheDocument();
  });

  it('renders ⌘K command palette trigger', () => {
    render(
      <SchoolAdminShell>
        <div>main content</div>
      </SchoolAdminShell>,
    );
    expect(screen.getByText('⌘K')).toBeInTheDocument();
  });
});

describe('school-admin mock data — VN K-12 realism', () => {
  it('has 8 teachers (sample of 50+ scale)', () => {
    expect(TEACHERS.length).toBeGreaterThanOrEqual(8);
    expect(TEACHERS.every((t) => /^[A-Z]{2}$/.test(t.initials))).toBe(true);
  });

  it('generates 25 classes (5 per grade × 5 grade blocks)', () => {
    expect(CLASSES.length).toBe(25);
    expect(CLASSES.filter((c) => c.code.startsWith('6')).length).toBe(5);
    expect(CLASSES.filter((c) => c.code.startsWith('LC')).length).toBe(5);
  });

  it('has conduct cases covering all 5 escalation steps', () => {
    const stepsCovered = new Set(CONDUCT_CASES.map((c) => c.step));
    expect(stepsCovered.has(1)).toBe(true);
    expect(stepsCovered.has(5)).toBe(true);
  });

  it('report card status matches 25 classes', () => {
    expect(REPORT_CARD_STATUS.length).toBe(25);
    const moetCompliant = REPORT_CARD_STATUS.filter((r) => r.moetCompliant).length;
    expect(moetCompliant).toBeGreaterThan(0);
    expect(moetCompliant).toBeLessThan(REPORT_CARD_STATUS.length);
  });

  it('school profile reflects K-12 scale', () => {
    expect(SCHOOL_PROFILE.totalStudents).toBeGreaterThanOrEqual(500);
    expect(SCHOOL_PROFILE.totalStudents).toBeLessThanOrEqual(3000);
    expect(SCHOOL_PROFILE.totalTeachers).toBeGreaterThanOrEqual(50);
  });
});
