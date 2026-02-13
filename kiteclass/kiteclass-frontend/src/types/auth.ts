/**
 * Authentication and user types.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

export interface User {
  id: number;
  email: string;
  name: string;
  userType: UserType;
  referenceId?: string;
}

export enum UserType {
  ADMIN = 'ADMIN',
  STAFF = 'STAFF',
  TEACHER = 'TEACHER',
  PARENT = 'PARENT',
  STUDENT = 'STUDENT',
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  userType: UserType;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: number;
    email: string;
    name: string;
    roles: string[];
    profile?: StudentProfile | TeacherProfile | ParentProfile;
  };
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface StudentProfile {
  id: number;
  name: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
  enrollmentDate?: string;
  status: StudentStatus;
}

export interface TeacherProfile {
  id: number;
  name: string;
  email: string;
  phone?: string;
  specialization?: string;
  qualifications?: string;
  hireDate?: string;
  status: TeacherStatus;
}

export interface ParentProfile {
  id: number;
  name: string;
  email: string;
  phone?: string;
  relationship?: string;
}

export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER',
}

export enum StudentStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  GRADUATED = 'GRADUATED',
  SUSPENDED = 'SUSPENDED',
}

export enum TeacherStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ON_LEAVE = 'ON_LEAVE',
}
