/**
 * Student domain types.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { Gender, StudentStatus } from './auth';

export interface Student {
  id: number;
  name: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
  enrollmentDate?: string;
  status: StudentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStudentRequest {
  name: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
}

export interface UpdateStudentRequest {
  name?: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
  status?: StudentStatus;
}

export interface StudentSearchParams {
  query?: string;
  status?: StudentStatus;
  gender?: Gender;
  page?: number;
  size?: number;
  sort?: string;
}
