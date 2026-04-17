# Business Docs — 3-Layer Structure

## Cấu trúc bắt buộc

Mỗi business domain = 1 folder với 3 files:

```
documents/01-business/{project}/{domain}/
├── rules.md          # Layer 1: Business Rules
├── use-cases.md      # Layer 2: Use Cases
└── api-contract.md   # Layer 3: API Contract
```

## Layer 1: rules.md

**Ai đọc:** BA, Product Owner, Tech Lead
**Mục đích:** SOURCE OF TRUTH cho business constraints

**Template:**
```markdown
# {Domain} — Business Rules

## Rules

| ID | Rule | Detail | Config Key |
|----|------|--------|-----------|
| BR-{DOM}-001 | Rule name | Mô tả cụ thể | `config.key` |

## Status Lifecycle

STATUS_A → STATUS_B → STATUS_C

## Config

```yaml
project:
  domain:
    key: value
```
```

**Rules:**
- Mỗi rule có ID duy nhất: `BR-{DOM}-{NNN}`
- Config key phải khớp CHÍNH XÁC với application.yml
- Max 50 rules per domain
- ~50-80 lines

## Layer 2: use-cases.md

**Ai đọc:** FE + BE developers
**Mục đích:** Hướng dẫn code — actor làm gì, system xử lý gì, FE hiển thị gì

**Template:**
```markdown
# {Domain} — Use Cases

### UC-{DOM}-{NN}: {Action Name}

**Actor:** Teacher / Admin / Student / System
**Precondition:** Điều kiện trước khi thực hiện

**Steps:**
1. FE: Hiển thị form/list/dialog với data gì
2. User: Nhập/chọn/click gì
3. System: Validate theo BR-{DOM}-{NNN}
4. System: Side effect (email, notification, audit log)
5. FE: Redirect/toast/update UI

**Postcondition:** Trạng thái sau khi hoàn thành

**Errors:**
| Code | Condition | Message | FE Behavior |
|------|-----------|---------|-------------|
| 400 | Validation fail | "..." | Show inline error |
| 403 | No permission | "..." | Redirect to 403 page |
| 409 | Conflict | "..." | Show conflict dialog |

**FE Notes:**
- Component nào? (Select, Search, Dialog)
- Filter logic? (ACTIVE only, exclude current)
- Confirm dialog khi nào?
```

**Rules:**
- Mỗi UC reference ít nhất 1 BR-xxx
- Mỗi error path phải có FE behavior
- ~80-120 lines

## Layer 3: api-contract.md

**Ai đọc:** FE dev (gọi API), BE dev (implement API)
**Mục đích:** Contract chính xác — endpoint, request, response, errors

**Template:**
```markdown
# {Domain} — API Contract

### {METHOD} /api/v1/{resource}

**Use Case:** UC-{DOM}-{NN}
**Auth:** Bearer token | Public
**Role:** ADMIN, TEACHER, OWNER

**Request:**
```json
{
  "field": "type — description"
}
```

**Response 2xx:**
```json
{
  "id": "number",
  "field": "type"
}
```

**Errors:**
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Field X is required" |
| 404 | NOT_FOUND | "Resource not found" |

**Query params:** `?page=0&size=20&sort=name,asc`
```

**Rules:**
- Mỗi endpoint reference UC-xxx
- Request/response JSON từ actual DTOs (không tự nghĩ)
- Error codes khớp với ErrorCode enum trong code
- ~60-100 lines

## Verification Chain

```
rules.md    → use-cases.md  → api-contract.md → Controller    → Test
BR-CRS-001    UC-CRS-02       PUT /api/...     @PutMapping     @Test
```

**Mỗi link phải traceable:**
- Grep `BR-xxx` trong use-cases.md → phải tìm thấy
- Grep `UC-xxx` trong api-contract.md → phải tìm thấy
- Grep endpoint path trong Controller → phải tồn tại
- Grep method name trong Test → phải có test

## Khi nào tạo/update?

| Event | Action |
|-------|--------|
| Module mới | Tạo folder + 3 files TRƯỚC khi code |
| Thêm use case | Update use-cases.md + api-contract.md + code + test trong CÙNG PR |
| Đổi business rule | Update rules.md + use-cases.md nếu ảnh hưởng |
| Đổi API | Update api-contract.md + use-cases.md nếu FE behavior thay đổi |

## Anti-patterns

- ❌ Chỉ có rules.md mà không có use-cases.md → dev phải đoán flow
- ❌ api-contract.md không khớp Controller → FE gọi sai endpoint
- ❌ use-cases.md không có error paths → dev quên handle errors
- ❌ Viết docs từ tưởng tượng thay vì extract từ code → docs sai
- ❌ Để tất cả 3 layers vào 1 file → khó tham chiếu, file quá dài
