import { describe, it, expect } from 'vitest';
import { z } from 'zod';

// Extract the schema definition from course-form.tsx
const courseSchema = z.object({
  name: z.string().min(1, 'Tên môn học là bắt buộc'),
  code: z.string().min(1, 'Mã môn học là bắt buộc'),
  description: z.string().optional(),
  category: z.string().optional(),
  level: z.string().optional(),
  language: z.string().optional(),
  syllabus: z.string().optional(),
  objectives: z.string().optional(),
  prerequisites: z.string().optional(),
  targetAudience: z.string().optional(),
  durationWeeks: z.preprocess(
    (v) => (v === '' || v === undefined || v === null ? undefined : Number(v)),
    z.number().int().min(1, 'Thời lượng phải >= 1 tuần').optional()
  ),
  totalSessions: z.preprocess(
    (v) => (v === '' || v === undefined || v === null ? undefined : Number(v)),
    z.number().int().min(1, 'Số buổi phải >= 1').optional()
  ),
  price: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : Number(v)),
    z.number().min(0, 'Học phí phải >= 0').optional()
  ),
  coverImageUrl: z.preprocess(
    (v) => (v === '' || v === null || v === undefined ? undefined : v),
    z.string().regex(/^https?:\/\/.+/, 'URL ảnh không hợp lệ (phải bắt đầu với http:// hoặc https://)').optional()
  ),
});

describe('Course Form Validation - Optional Fields', () => {
  const baseValidData = {
    name: 'Test Course',
    code: 'TEST-101',
  };

  describe('durationWeeks', () => {
    it('should accept undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: undefined,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.durationWeeks).toBeUndefined();
      }
    });

    it('should accept null and convert to undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: null,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.durationWeeks).toBeUndefined();
      }
    });

    it('should accept empty string and convert to undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: '',
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.durationWeeks).toBeUndefined();
      }
    });

    it('should accept valid number', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: 12,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.durationWeeks).toBe(12);
      }
    });

    it('should reject negative number', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: -1,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('Thời lượng phải >= 1 tuần');
      }
    });

    it('should reject zero', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: 0,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('Thời lượng phải >= 1 tuần');
      }
    });
  });

  describe('totalSessions', () => {
    it('should accept undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        totalSessions: undefined,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.totalSessions).toBeUndefined();
      }
    });

    it('should accept null and convert to undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        totalSessions: null,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.totalSessions).toBeUndefined();
      }
    });

    it('should accept empty string and convert to undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        totalSessions: '',
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.totalSessions).toBeUndefined();
      }
    });

    it('should accept valid number', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        totalSessions: 24,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.totalSessions).toBe(24);
      }
    });

    it('should reject negative number', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        totalSessions: -5,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('Số buổi phải >= 1');
      }
    });

    it('should reject zero', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        totalSessions: 0,
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('Số buổi phải >= 1');
      }
    });
  });

  describe('coverImageUrl', () => {
    it('should accept undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: undefined,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.coverImageUrl).toBeUndefined();
      }
    });

    it('should accept null and convert to undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: null,
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.coverImageUrl).toBeUndefined();
      }
    });

    it('should accept empty string and convert to undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: '',
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.coverImageUrl).toBeUndefined();
      }
    });

    it('should accept valid HTTP URL', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: 'http://example.com/image.jpg',
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.coverImageUrl).toBe('http://example.com/image.jpg');
      }
    });

    it('should accept valid HTTPS URL', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: 'https://example.com/image.png',
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.coverImageUrl).toBe('https://example.com/image.png');
      }
    });

    it('should reject invalid URL without protocol', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: 'example.com/image.jpg',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('URL ảnh không hợp lệ');
      }
    });

    it('should reject invalid URL with wrong protocol', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        coverImageUrl: 'ftp://example.com/image.jpg',
      });
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('URL ảnh không hợp lệ');
      }
    });
  });

  describe('Combined optional fields', () => {
    it('should accept all optional fields as undefined', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: undefined,
        totalSessions: undefined,
        coverImageUrl: undefined,
      });
      expect(result.success).toBe(true);
    });

    it('should accept all optional fields as empty strings', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: '',
        totalSessions: '',
        coverImageUrl: '',
      });
      expect(result.success).toBe(true);
    });

    it('should accept mixed valid and undefined optional fields', () => {
      const result = courseSchema.safeParse({
        ...baseValidData,
        durationWeeks: 12,
        totalSessions: undefined,
        coverImageUrl: 'https://example.com/image.jpg',
      });
      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.durationWeeks).toBe(12);
        expect(result.data.totalSessions).toBeUndefined();
        expect(result.data.coverImageUrl).toBe('https://example.com/image.jpg');
      }
    });
  });
});
