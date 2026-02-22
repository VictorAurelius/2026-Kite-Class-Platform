/**
 * Teacher domain types.
 * Matches backend TeacherResponse and CreateTeacherRequest DTOs.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { TeacherStatus } from './auth';

export interface Teacher {
  id: number;
  name: string;
  email: string;
  phoneNumber?: string;
  specialization?: string;
  bio?: string;
  qualification?: string;
  experienceYears?: number;
  avatarUrl?: string;
  status: TeacherStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeacherRequest {
  name: string;
  email: string;
  phoneNumber?: string;
  specialization?: string;
  bio?: string;
  qualification?: string;
  experienceYears?: number;
}

export interface UpdateTeacherRequest {
  name?: string;
  phoneNumber?: string;
  specialization?: string;
  bio?: string;
  qualification?: string;
  experienceYears?: number;
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
