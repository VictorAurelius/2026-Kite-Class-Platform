# Quick Start — KiteClass Frontend

**Last Updated:** 2026-02-22
**Framework:** Next.js 15, TypeScript, Tailwind CSS, shadcn/ui

---

## 🎯 Current Status

- **Latest PR:** 3.6 (Course Management Pages) ✅ MERGED
- **Branch:** main
- **Tests:** ESLint clean, TypeScript strict, Playwright E2E pass
- **Next:** PR 3.7 — Class Management Pages (backend PR 2.5 ✅ ready)

---

## ✅ Completed PRs

| PR | Description | Status |
|----|-------------|--------|
| 3.1 | Project Setup (Next.js 15, Tailwind, shadcn/ui, React Query, Zustand) | ✅ |
| 3.2 | Auth Pages (Login, Forgot/Reset Password) | ✅ |
| 3.3 | Auth Implementation (JWT, token refresh, auth store) | ✅ |
| 3.4 | Student Management Pages (CRUD, search, pagination) | ✅ |
| 3.5 | Teacher Management Pages (CRUD, status management) | ✅ |
| 3.6 | Course Management Pages (CRUD, Publish/Archive lifecycle) | ✅ |

**Frontend Progress:** 6/13 PRs (46%)

---

## 🚀 Next Priority

**PR 3.7: Class Management Pages**
- Backend: PR 2.5 Class Module ✅ merged
- Pages: list, new, detail (with Start/Complete/Cancel actions), edit
- Schedule & Sessions view
- Class code generation UI

---

## 📊 Pages Inventory

| Route | Description | Status |
|-------|-------------|--------|
| `/login` | Login page | ✅ |
| `/forgot-password` | Forgot password | ✅ |
| `/reset-password` | Reset password | ✅ |
| `/dashboard` | Dashboard placeholder | ✅ |
| `/students` | Student list | ✅ |
| `/students/new` | Create student | ✅ |
| `/students/[id]` | Student detail | ✅ |
| `/students/[id]/edit` | Edit student | ✅ |
| `/teachers` | Teacher list | ✅ |
| `/teachers/new` | Create teacher | ✅ |
| `/teachers/[id]` | Teacher detail | ✅ |
| `/teachers/[id]/edit` | Edit teacher | ✅ |
| `/courses` | Course list | ✅ |
| `/courses/new` | Create course | ✅ |
| `/courses/[id]` | Course detail + Publish/Archive | ✅ |
| `/courses/[id]/edit` | Edit course | ✅ |
| `/classes/*` | Class management | ⏳ PR 3.7 |

---

## 🛠️ Dev Commands

```bash
cd kiteclass/kiteclass-frontend

# Install dependencies (first time)
pnpm install --frozen-lockfile

# Run dev server
pnpm dev  # http://localhost:3000

# Type check
node_modules/.bin/tsc --noEmit

# Lint
node_modules/.bin/next lint

# Build (production check)
node_modules/.bin/next build
```

---

## 📁 Project Structure

```
src/
├── app/
│   ├── (auth)/          # Login, forgot/reset password
│   └── (dashboard)/     # All authenticated pages
│       ├── students/
│       ├── teachers/
│       ├── courses/
│       └── ...
├── components/
│   ├── common/          # DataTable, SearchInput, StatusBadge, etc.
│   ├── forms/           # FormInput, FormSelect, FormTextarea, StudentForm, TeacherForm, CourseForm
│   ├── layout/          # DashboardLayout, Sidebar, Header
│   └── tables/columns/  # student-columns, teacher-columns, course-columns
├── hooks/               # use-students, use-teachers, use-courses
├── lib/api/             # students.ts, teachers.ts, courses.ts + api-client.ts
└── types/               # student.ts, teacher.ts, course.ts, auth.ts, api.ts
```

---

## 🔑 Key Patterns

### API + Hook pattern (follow for all modules)
```ts
// lib/api/teachers.ts — thin API wrapper
export const teachersApi = { getAll, getById, create, update, delete }

// hooks/use-teachers.ts — React Query + toast
export function useTeachers(params) { return useQuery(...) }
export function useCreateTeacher() { return useMutation({ onSuccess: toast }) }
```

### Form pattern
```tsx
// components/forms/teacher-form.tsx
// - useForm + zodResolver for validation
// - FormInput / FormSelect / FormTextarea components
// - isEditing prop for status selector
// - isSubmitting disables all fields
```

### Table columns pattern
```tsx
// components/tables/columns/teacher-columns.tsx
// - ColumnDef<Teacher>[]
// - StatusBadge with Vietnamese labels
// - Actions: Eye (detail) + Pencil (edit) + Trash2 (delete, conditionally)
```
