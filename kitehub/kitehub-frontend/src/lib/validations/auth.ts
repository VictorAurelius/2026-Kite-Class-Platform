import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
});

export type LoginFormData = z.infer<typeof loginSchema>;

export const registerSchema = z.object({
  organizationName: z.string().min(2, 'Tên tổ chức tối thiểu 2 ký tự').max(100),
  subdomain: z
    .string()
    .min(3, 'Subdomain tối thiểu 3 ký tự')
    .max(30)
    .regex(/^[a-z0-9-]+$/, 'Chỉ chấp nhận chữ thường, số và dấu gạch ngang'),
  ownerEmail: z.string().email('Email không hợp lệ'),
  ownerPassword: z
    .string()
    .min(8, 'Mật khẩu tối thiểu 8 ký tự')
    .regex(/[A-Z]/, 'Cần ít nhất 1 chữ hoa')
    .regex(/[0-9]/, 'Cần ít nhất 1 số'),
  confirmPassword: z.string(),
}).refine((data) => data.ownerPassword === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword'],
});

export type RegisterFormData = z.infer<typeof registerSchema>;
