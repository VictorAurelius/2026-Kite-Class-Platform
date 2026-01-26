# Skill: Feature Development Checklist

Checklist phát triển feature cho dự án KiteClass Platform V3.1.

## Mô tả

Tài liệu checklist bắt buộc cho mỗi feature trước khi merge:
- Design mapping (database, API, UI)
- Coding standards và guidelines
- Comment guidelines
- Design patterns
- Testing requirements
- Warning policy
- Code review criteria

## Trigger phrases

- "checklist phát triển"
- "feature checklist"
- "coding standards"
- "trước khi merge"
- "code review"

## Files

| File | Path |
|------|------|
| Tài liệu chính | `documents/plans/feature-development-checklist.md` |

## Development Workflow

```
PLAN → CODE → TEST → REVIEW → MERGE
  │       │       │       │       │
  ▼       ▼       ▼       ▼       ▼
Check   Check   Check   Check   Check
Part 2  Part 3,4 Part 5  Part 6  Part 7
```

Mỗi phase phải PASS checklist mới được chuyển sang phase tiếp theo.

## Checklist Trước Khi Code

### Database Design Mapping
- [ ] Xác định tables liên quan trong database-design.md
- [ ] Review schema và columns cần thiết
- [ ] Xác định indexes cần thiết
- [ ] Xác định relationships (FK, constraints)
- [ ] Migration script đã được tạo

### API Design Mapping
- [ ] Xác định endpoints cần implement
- [ ] Xác định request/response DTOs
- [ ] Xác định error codes và messages
- [ ] OpenAPI spec đã được update

### UI Design Mapping
- [ ] Xác định components cần tạo
- [ ] Review Figma/wireframe
- [ ] Xác định state management

## Coding Standards

### Java/Spring Boot
- Package structure: controller, service, repository, dto, entity
- Naming: CamelCase cho class, camelCase cho methods/variables
- DTOs: Dùng Java Record
- Validation: @Valid, @NotNull, @Size
- Exception: Custom exceptions, @RestControllerAdvice

### TypeScript/React
- Component naming: PascalCase
- Hooks: use prefix (useAuth, useStudents)
- Types: Interface cho object shapes
- State: React Query cho server state, Zustand cho client state

## Comment Guidelines

### Khi nào cần comment
- Complex business logic
- Algorithm giải thích
- Workaround với reason
- TODO với ticket number

### Khi nào KHÔNG comment
- Self-explanatory code
- Redundant comments
- Commented-out code

## Design Patterns

### Backend Patterns
- Repository Pattern: Data access abstraction
- DTO Pattern: Tách entity và transfer objects
- Service Layer: Business logic isolation
- Builder Pattern: Complex object creation

### Frontend Patterns
- Container/Presentational: Logic vs UI separation
- Custom Hooks: Reusable logic
- Composition: Component composition over inheritance
- Provider Pattern: Context API

## Testing Requirements

### Unit Tests (Bắt buộc)
- Coverage >= 80%
- Happy path + Edge cases
- Mock external dependencies

### Integration Tests
- API endpoint testing
- Database interaction
- Authentication/Authorization

### E2E Tests (Quan trọng features)
- User flow testing
- Cross-browser (optional)

## Warning Policy

| Loại Warning | Xử lý |
|--------------|-------|
| Compilation warning | PHẢI sửa trước merge |
| Deprecated API | Phải có plan upgrade |
| Security warning | KHÔNG được merge |
| Performance warning | Review và document |

## Tiêu Chí Đạt/Không Đạt

### ĐẠT
- [ ] Tất cả checklist items completed
- [ ] Tests pass 100%
- [ ] No compilation warnings
- [ ] Code review approved
- [ ] Documentation updated

### KHÔNG ĐẠT
- Bất kỳ checklist item chưa complete
- Tests fail
- Security warnings
- No code review

## Actions

### Xem chi tiết coding standards
Đọc phần "3. Coding Standards" trong tài liệu.

### Xem comment guidelines
Đọc phần "4. Comment Guidelines" với ví dụ cụ thể.

### Xem test templates
Đọc phần "7. Test Script Requirements" và "12. Templates".
