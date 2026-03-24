# LMS (Learning Management System) — Business Rules

> Domain: `kiteclass/lms`
> Source: `LmsController`, `LessonProgressController`, `LmsService`, `LessonProgressService`

## Course Structure

| ID | Rule | Config Key |
|----|------|-----------|
| BR-LMS-001 | Guest (no X-User-Id) only sees trial lessons (`isTrial=true`). Paid lessons are hidden from course structure and inaccessible via detail endpoint. | — |
| BR-LMS-002 | Student (X-User-Id present) must have active enrollment to access paid lessons. Trial lessons are accessible without enrollment. | — |
| BR-LMS-003 | Course hierarchy is 3-tier: **Course > Module > Lesson**. A module belongs to exactly one course; a lesson belongs to exactly one module. | — |

## Module Rules

| ID | Rule | Config Key |
|----|------|-----------|
| BR-LMS-004 | Module `orderNumber` must be unique within the same course. Minimum value: 1. | — |
| BR-LMS-005 | Module `title` is required, max 200 characters. `description` is optional, max 5000 characters. | — |
| BR-LMS-006 | Only the **course owner** (teacher) can create, update, or delete modules. Verified via `X-Teacher-Id` header. | — |
| BR-LMS-007 | Cannot delete a module that still has lessons. Teacher must delete all lessons first. | — |

## Lesson Rules

| ID | Rule | Config Key |
|----|------|-----------|
| BR-LMS-008 | Lesson `orderNumber` must be unique within the same module. Minimum value: 1. | — |
| BR-LMS-009 | Lesson `title` is required (max 200). `content` optional (max 10000). `videoUrl` optional (max 500). `isTrial` defaults to `false`. `estimatedDuration` optional, in minutes (min 1). | — |
| BR-LMS-010 | Only the **course owner** can CRUD lessons. Same ownership check as modules (BR-LMS-006). | — |
| BR-LMS-011 | Update requests are partial — only provided fields are updated (null fields are ignored). | — |

## Learning Resource Rules

| ID | Rule | Config Key |
|----|------|-----------|
| BR-LMS-012 | Resource types: `VIDEO`, `PDF`, `SLIDE`, `AUDIO`, `LINK`, `CODE`, `OTHER` (enum `ResourceType`). | — |
| BR-LMS-013 | Resource `type` and `url` and `title` are required. `fileSize` is optional (in bytes, min 1). | — |
| BR-LMS-014 | `url` max 500 characters. `title` max 200 characters. | — |
| BR-LMS-015 | Only the **course owner** can add or delete resources. Same ownership check as modules (BR-LMS-006). | — |

## Progress Tracking Rules

| ID | Rule | Config Key |
|----|------|-----------|
| BR-LMS-016 | `completeLesson` is **idempotent** — calling multiple times is safe. Creates record if not exists, updates if exists. | — |
| BR-LMS-017 | Completing a lesson publishes `LessonCompletedEvent` for downstream processing (gamification, notifications). | — |
| BR-LMS-018 | Course progress formula: `progressPercent = (completedLessons / totalLessons) * 100`. | — |
| BR-LMS-019 | Student must be enrolled to complete paid lessons. Trial lessons can be completed without enrollment check. | — |
| BR-LMS-020 | `getLessonProgress` returns `null` if student has no progress record for that lesson. | — |

## Access Control Matrix

| Actor | Header | Modules | Trial Lessons | Paid Lessons | Progress | CRUD |
|-------|--------|---------|---------------|-------------|----------|------|
| Guest | (none) | View structure | View detail | Hidden | No | No |
| Student | X-User-Id | View structure | View detail | View if enrolled | Yes | No |
| Teacher | X-Teacher-Id | Full CRUD | Full CRUD | Full CRUD | No | Yes |
