/**
 * Teacher domain types.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { TeacherStatus } from './auth';

export interface Teacher {
  id: number;
  name: string;
  email: string;
  phone?: string;
  specialization?: string;
  qualifications?: string;
  hireDate?: string;
  status: TeacherStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeacherRequest {
  name: string;
  email: string;
  phone?: string;
  specialization?: string;
  qualifications?: string;
  hireDate?: string;
}

export interface UpdateTeacherRequest {
  name?: string;
  email?: string;
  phone?: string;
  specialization?: string;
  qualifications?: string;
  status?: TeacherStatus;
}

export interface TeacherSearchParams {
  query?: string;
  status?: TeacherStatus;
  specialization?: string;
  page?: number;
  size?: number;
  sort?: string;
}
