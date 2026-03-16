/**
 * Unit tests for authentication validation schemas.
 *
 * @since PR 5.9
 */

import { describe, it, expect } from 'vitest';
import { loginSchema, registerSchema } from '../auth';

describe('auth validations', () => {
  describe('loginSchema', () => {
    it('accepts valid login data', () => {
      const result = loginSchema.safeParse({
        email: 'user@example.com',
        password: 'password123',
      });
      expect(result.success).toBe(true);
    });

    it('rejects invalid email', () => {
      const result = loginSchema.safeParse({
        email: 'not-an-email',
        password: 'password123',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]?.message).toBe('Email không hợp lệ');
      }
    });

    it('rejects empty email', () => {
      const result = loginSchema.safeParse({
        email: '',
        password: 'password123',
      });
      expect(result.success).toBe(false);
    });

    it('rejects password shorter than 6 characters', () => {
      const result = loginSchema.safeParse({
        email: 'user@example.com',
        password: '12345',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]?.message).toBe('Mật khẩu tối thiểu 6 ký tự');
      }
    });

    it('accepts password exactly 6 characters', () => {
      const result = loginSchema.safeParse({
        email: 'user@example.com',
        password: '123456',
      });
      expect(result.success).toBe(true);
    });

    it('rejects missing fields', () => {
      const result = loginSchema.safeParse({});
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues.length).toBeGreaterThanOrEqual(2);
      }
    });
  });

  describe('registerSchema', () => {
    const validData = {
      organizationName: 'My School',
      subdomain: 'my-school',
      ownerEmail: 'owner@example.com',
      ownerPassword: 'Password1',
      confirmPassword: 'Password1',
    };

    it('accepts valid registration data', () => {
      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    describe('organizationName', () => {
      it('rejects name shorter than 2 characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          organizationName: 'A',
        });
        expect(result.success).toBe(false);
      });

      it('accepts name exactly 2 characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          organizationName: 'AB',
        });
        expect(result.success).toBe(true);
      });

      it('rejects name longer than 100 characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          organizationName: 'A'.repeat(101),
        });
        expect(result.success).toBe(false);
      });
    });

    describe('subdomain', () => {
      it('rejects subdomain shorter than 3 characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          subdomain: 'ab',
        });
        expect(result.success).toBe(false);
      });

      it('accepts subdomain exactly 3 characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          subdomain: 'abc',
        });
        expect(result.success).toBe(true);
      });

      it('rejects subdomain with uppercase letters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          subdomain: 'MySchool',
        });
        expect(result.success).toBe(false);
        if (!result.success) {
          const subdomainError = result.error.issues.find(i => i.path.includes('subdomain'));
          expect(subdomainError?.message).toContain('chữ thường');
        }
      });

      it('rejects subdomain with special characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          subdomain: 'my_school',
        });
        expect(result.success).toBe(false);
      });

      it('accepts subdomain with hyphens', () => {
        const result = registerSchema.safeParse({
          ...validData,
          subdomain: 'my-school-123',
        });
        expect(result.success).toBe(true);
      });

      it('accepts subdomain with numbers', () => {
        const result = registerSchema.safeParse({
          ...validData,
          subdomain: 'school2026',
        });
        expect(result.success).toBe(true);
      });
    });

    describe('ownerEmail', () => {
      it('rejects invalid email', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerEmail: 'invalid-email',
        });
        expect(result.success).toBe(false);
      });

      it('accepts valid email formats', () => {
        const validEmails = [
          'user@domain.com',
          'user.name@domain.co.vn',
          'user+tag@domain.org',
        ];

        validEmails.forEach(email => {
          const result = registerSchema.safeParse({
            ...validData,
            ownerEmail: email,
          });
          expect(result.success).toBe(true);
        });
      });
    });

    describe('ownerPassword', () => {
      it('rejects password shorter than 8 characters', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerPassword: 'Pass1',
          confirmPassword: 'Pass1',
        });
        expect(result.success).toBe(false);
      });

      it('rejects password without uppercase letter', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerPassword: 'password1',
          confirmPassword: 'password1',
        });
        expect(result.success).toBe(false);
        if (!result.success) {
          const pwError = result.error.issues.find(i => i.path.includes('ownerPassword'));
          expect(pwError?.message).toContain('chữ hoa');
        }
      });

      it('rejects password without number', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerPassword: 'Password',
          confirmPassword: 'Password',
        });
        expect(result.success).toBe(false);
        if (!result.success) {
          const pwError = result.error.issues.find(i => i.path.includes('ownerPassword'));
          expect(pwError?.message).toContain('số');
        }
      });

      it('accepts strong password', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerPassword: 'StrongPass123',
          confirmPassword: 'StrongPass123',
        });
        expect(result.success).toBe(true);
      });
    });

    describe('confirmPassword', () => {
      it('rejects when passwords do not match', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerPassword: 'Password1',
          confirmPassword: 'Password2',
        });
        expect(result.success).toBe(false);
        if (!result.success) {
          const confirmError = result.error.issues.find(i => i.path.includes('confirmPassword'));
          expect(confirmError?.message).toBe('Mật khẩu xác nhận không khớp');
        }
      });

      it('accepts when passwords match', () => {
        const result = registerSchema.safeParse({
          ...validData,
          ownerPassword: 'Password123',
          confirmPassword: 'Password123',
        });
        expect(result.success).toBe(true);
      });
    });
  });
});
