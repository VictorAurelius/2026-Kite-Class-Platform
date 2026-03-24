# Portable Skill — adapt {project} placeholders

# Business Docs — 3-Layer Structure

## Cau truc bat buoc

Moi business domain = 1 folder voi 3 files:

```
documents/01-business/{project}/{domain}/
├── rules.md          # Layer 1: Business Rules
├── use-cases.md      # Layer 2: Use Cases
└── api-contract.md   # Layer 3: API Contract
```

## Layer 1: rules.md (~50-80 lines)

**Doc gia:** BA, Product Owner, Tech Lead
**Muc dich:** SOURCE OF TRUTH cho business constraints

| Field | Format |
|-------|--------|
| Rule ID | `BR-{DOM}-{NNN}` (unique) |
| Config key | PHAI khop chinh xac voi application config |
| Max rules | 50 per domain |

Template: Rule table (ID, Rule, Detail, Config Key) + Status Lifecycle + Config YAML.

## Layer 2: use-cases.md (~80-120 lines)

**Doc gia:** FE + BE developers
**Muc dich:** Actor lam gi, system xu ly gi, FE hien thi gi

Each use case includes:
- **ID:** `UC-{DOM}-{NN}`
- **Actor / Precondition / Steps / Postcondition**
- **Errors:** Status code, condition, message, FE behavior
- **FE Notes:** Component, filter logic, confirm dialogs

**Rules:**
- Moi UC reference it nhat 1 `BR-xxx`
- Moi error path phai co FE behavior

## Layer 3: api-contract.md (~60-100 lines)

**Doc gia:** FE dev (call API), BE dev (implement API)
**Muc dich:** Contract chinh xac — endpoint, request, response, errors

Each endpoint includes:
- Use Case reference (`UC-{DOM}-{NN}`)
- Auth requirement + roles
- Request/response JSON (from actual DTOs)
- Error table (status, code, message)
- Query params if applicable

**Rules:**
- Moi endpoint reference `UC-xxx`
- Error codes khop voi ErrorCode enum trong code

## Verification Chain

```
rules.md     → use-cases.md   → api-contract.md → Controller     → Test
BR-{DOM}-001   UC-{DOM}-01      PUT /api/...     @PutMapping      @Test
```

Moi link phai traceable:
- Grep `BR-xxx` trong use-cases.md → phai tim thay
- Grep `UC-xxx` trong api-contract.md → phai tim thay
- Grep endpoint path trong Controller → phai ton tai
- Grep method name trong Test → phai co test

## Khi nao tao/update?

| Event | Action |
|-------|--------|
| Module moi | Tao folder + 3 files TRUOC khi code |
| Them use case | Update use-cases + api-contract + code + test trong CUNG PR |
| Doi business rule | Update rules + use-cases neu anh huong |
| Doi API | Update api-contract + use-cases neu FE behavior thay doi |

## Anti-patterns

- Chi co rules.md ma khong co use-cases.md → dev phai doan flow
- api-contract.md khong khop Controller → FE goi sai endpoint
- use-cases.md khong co error paths → dev quen handle errors
- Viet docs tu tuong tuong thay vi extract tu code → docs sai
- De tat ca 3 layers vao 1 file → kho tham chieu, file qua dai
