/**
 * Test fixtures for attendance components.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import {
  AttendanceStatus,
  type Attendance,
  type AttendanceStatsResponse,
  type TodayClassSession,
  type AttendanceTrendPoint,
  type ClassAttendanceBreakdown,
} from '@/types/attendance';

export const mockAttendanceStats: AttendanceStatsResponse = {
  targetId: 1,
  targetType: 'STUDENT',
  totalSessions: 40,
  presentCount: 34,
  absentCount: 3,
  lateCount: 2,
  excusedCount: 1,
  makeupCount: 0,
  attendanceRate: 85.0,
};

export const mockHighAttendanceStats: AttendanceStatsResponse = {
  targetId: 1,
  targetType: 'STUDENT',
  totalSessions: 40,
  presentCount: 38,
  absentCount: 1,
  lateCount: 1,
  excusedCount: 0,
  makeupCount: 0,
  attendanceRate: 95.0,
};

export const mockLowAttendanceStats: AttendanceStatsResponse = {
  targetId: 1,
  targetType: 'STUDENT',
  totalSessions: 40,
  presentCount: 24,
  absentCount: 10,
  lateCount: 4,
  excusedCount: 2,
  makeupCount: 0,
  attendanceRate: 60.0,
};

export const mockAttendanceRecords: Attendance[] = [
  {
    id: 1,
    enrollmentId: 1,
    studentName: 'Nguyễn Văn A',
    sessionId: 1,
    sessionNumber: 1,
    status: AttendanceStatus.PRESENT,
    markedDate: '2026-03-01T09:00:00Z',
    markedBy: 1,
    markedByName: 'GV Trần B',
    notes: '',
    pointsAwarded: 10,
    createdAt: '2026-03-01T09:00:00Z',
    updatedAt: '2026-03-01T09:00:00Z',
  },
  {
    id: 2,
    enrollmentId: 1,
    studentName: 'Nguyễn Văn A',
    sessionId: 2,
    sessionNumber: 2,
    status: AttendanceStatus.ABSENT,
    markedDate: '2026-03-03T09:00:00Z',
    markedBy: 1,
    markedByName: 'GV Trần B',
    notes: 'Ốm',
    pointsAwarded: 0,
    createdAt: '2026-03-03T09:00:00Z',
    updatedAt: '2026-03-03T09:00:00Z',
  },
  {
    id: 3,
    enrollmentId: 1,
    studentName: 'Nguyễn Văn A',
    sessionId: 3,
    sessionNumber: 3,
    status: AttendanceStatus.LATE,
    markedDate: '2026-03-05T09:00:00Z',
    markedBy: 1,
    markedByName: 'GV Trần B',
    notes: 'Đến trễ 15 phút',
    pointsAwarded: 8,
    createdAt: '2026-03-05T09:00:00Z',
    updatedAt: '2026-03-05T09:00:00Z',
  },
  {
    id: 4,
    enrollmentId: 1,
    studentName: 'Nguyễn Văn A',
    sessionId: 4,
    sessionNumber: 4,
    status: AttendanceStatus.EXCUSED,
    markedDate: '2026-03-07T09:00:00Z',
    markedBy: 1,
    markedByName: 'GV Trần B',
    notes: 'Xin phép nghỉ',
    pointsAwarded: 10,
    createdAt: '2026-03-07T09:00:00Z',
    updatedAt: '2026-03-07T09:00:00Z',
  },
];

export const mockTodayClassSessions: TodayClassSession[] = [
  {
    sessionId: 1,
    sessionNumber: 5,
    classId: 1,
    className: 'Toán Lớp 10A',
    startTime: '2026-03-08T09:00:00Z',
    endTime: '2026-03-08T10:30:00Z',
    totalStudents: 30,
    attendanceMarked: false,
  },
  {
    sessionId: 2,
    sessionNumber: 3,
    classId: 2,
    className: 'Lý Lớp 11B',
    startTime: '2026-03-08T13:00:00Z',
    endTime: '2026-03-08T14:30:00Z',
    totalStudents: 25,
    attendanceMarked: true,
    presentCount: 23,
    absentCount: 2,
  },
  {
    sessionId: 3,
    sessionNumber: 8,
    classId: 3,
    className: 'Hóa Lớp 12C',
    startTime: '2026-03-08T15:00:00Z',
    endTime: '2026-03-08T16:30:00Z',
    totalStudents: 28,
    attendanceMarked: false,
  },
];

export const mockAttendanceTrends: AttendanceTrendPoint[] = [
  {
    date: '2026-02-01',
    attendanceRate: 85.0,
    presentCount: 17,
    totalSessions: 20,
  },
  {
    date: '2026-02-08',
    attendanceRate: 90.0,
    presentCount: 18,
    totalSessions: 20,
  },
  {
    date: '2026-02-15',
    attendanceRate: 87.5,
    presentCount: 35,
    totalSessions: 40,
  },
  {
    date: '2026-02-22',
    attendanceRate: 92.0,
    presentCount: 46,
    totalSessions: 50,
  },
  {
    date: '2026-03-01',
    attendanceRate: 88.0,
    presentCount: 44,
    totalSessions: 50,
  },
];

export const mockClassBreakdown: ClassAttendanceBreakdown[] = [
  {
    classId: 1,
    className: 'Toán Lớp 10A',
    teacherName: 'GV Trần B',
    totalSessions: 20,
    presentCount: 450,
    absentCount: 30,
    lateCount: 15,
    excusedCount: 5,
    attendanceRate: 90.0,
  },
  {
    classId: 2,
    className: 'Lý Lớp 11B',
    teacherName: 'GV Nguyễn C',
    totalSessions: 18,
    presentCount: 380,
    absentCount: 45,
    lateCount: 20,
    excusedCount: 5,
    attendanceRate: 84.4,
  },
  {
    classId: 3,
    className: 'Hóa Lớp 12C',
    teacherName: 'GV Lê D',
    totalSessions: 22,
    presentCount: 510,
    absentCount: 20,
    lateCount: 18,
    excusedCount: 12,
    attendanceRate: 91.1,
  },
];
