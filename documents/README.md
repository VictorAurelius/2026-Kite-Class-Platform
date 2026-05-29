# KiteClass Documentation Map

Bản đồ tổng quan documents để navigate và update dễ dàng.

---

## 📍 Quick Navigation by Topic

| Topic | Files | Location |
|-------|-------|----------|
| **Architecture** | system-architecture-v4.md, service-architecture-revision-report.md, 04-architecture-full.puml | 07-archived/research/architecture/, 06-diagrams/plantuml/ |
| **Database** | database-design.md, database-migration-plan.md, 03-erd.puml | 03-planning/database/, 06-diagrams/plantuml/ |
| **Storage Service** | storage-service-design.md, 02-core-prs.md, frontend-plan.md | 03-planning/implementation/, 03-planning/prs/ |
| **API Design** | api-design.md | .claude/skills/ |
| **PR Planning** | 00-master-pr-index.md, 01-gateway-prs.md, 02-core-prs.md, 03-frontend-prs.md | 03-planning/prs/ |
| **Implementation** | kiteclass-implementation-plan.md, core-service-implementation.md, frontend-plan.md, gateway-implementation-plan.md | 03-planning/implementation/ |
| **Testing** | testing-strategy.md | 03-planning/testing/ |
| **Service Analysis** | service-use-cases-v3.md, media-service-analysis.md | 07-archived/research/services/ |

---

## 📁 File Index with Update Triggers

### 🔴 CRITICAL - Update cho mọi major change

#### `03-planning/database/database-design.md`
**Nội dung**: Schema design cho tất cả tables (Gateway, Core, Storage)
**Update khi**:
- Thêm table mới
- Thêm column vào table hiện có
- Thay đổi foreign key relationships
- Thêm indexes mới
**Related**: database-migration-plan.md, 03-erd.puml

#### `03-planning/database/database-migration-plan.md`
**Nội dung**: Flyway migration timeline (V1-V13+)
**Update khi**:
- Tạo migration mới
- Rollback hoặc fix migration
**Related**: database-design.md

#### `06-diagrams/plantuml/03-erd.puml`
**Nội dung**: Entity Relationship Diagram
**Update khi**:
- Thêm entity/table mới
- Thêm relationships
- Thay đổi cardinality
**Related**: database-design.md

#### `06-diagrams/plantuml/04-architecture-full.puml`
**Nội dung**: Full system architecture diagram
**Update khi**:
- Thêm service mới
- Thêm component trong service
- Thay đổi data flow
**Related**: system-architecture-v4.md

#### `.claude/skills/api-design.md`
**Nội dung**: REST API endpoint specifications
**Update khi**:
- Thêm API endpoint mới
- Thay đổi request/response schema
- Thêm error codes
**Related**: kiteclass-implementation-plan.md, 02-core-prs.md

---

### 🟡 HIGH - Update khi thêm features hoặc PRs

#### `03-planning/prs/00-master-pr-index.md`
**Nội dung**: Master list of all PRs (Gateway, Core, Frontend)
**Update khi**:
- Thêm PR mới vào bất kỳ service nào
- Đổi PR status (Pending → In Progress → Complete)
**Related**: 01-gateway-prs.md, 02-core-prs.md, 03-frontend-prs.md

#### `03-planning/prs/02-core-prs.md`
**Nội dung**: Core Service PRs (Student, Teacher, Course, Attendance, Storage, etc.)
**Update khi**:
- Thêm PR mới cho Core Service
- Thêm module mới (e.g., PR 2.10.1 Storage Service)
**Related**: 00-master-pr-index.md, core-service-implementation.md

#### `03-planning/prs/03-frontend-prs.md`
**Nội dung**: Frontend PRs (Pages, Components, Features)
**Update khi**:
- Thêm PR mới cho Frontend
- Thêm dependencies giữa Frontend ↔ Backend PRs
**Related**: 00-master-pr-index.md, frontend-plan.md

#### `03-planning/implementation/kiteclass-implementation-plan.md`
**Nội dung**: Master implementation plan với PR prompts
**Update khi**:
- Thêm PR prompt mới (detailed implementation steps)
- Update branch strategy
**Related**: 02-core-prs.md, 03-frontend-prs.md

#### `03-planning/implementation/core-service-implementation.md`
**Nội dung**: Core Service implementation details (entities, services, repositories)
**Update khi**:
- Thêm module mới vào Core Service
- Thêm cross-cutting concerns (như File Storage Integration)
**Related**: 02-core-prs.md, database-design.md

#### `03-planning/implementation/frontend-plan.md`
**Nội dung**: Frontend implementation plan (components, pages, hooks)
**Update khi**:
- Thêm PHASE mới (e.g., PHASE 7B File Upload Components)
- Thêm reusable components
**Related**: 03-frontend-prs.md

#### `03-planning/implementation/gateway-implementation-plan.md`
**Nội dung**: Gateway implementation plan (authentication, cross-service calls)
**Update khi**:
- Thêm cross-service integration
- Thêm authentication flows
**Related**: 01-gateway-prs.md

---

### 🟢 MEDIUM - Update khi có design decisions hoặc research findings

#### `07-archived/research/architecture/system-architecture-v4.md`
**Nội dung**: System architecture v4 overview (services, layers, data flow)
**Update khi**:
- Thêm layer mới (e.g., Storage layer)
- Thêm service mới
- Major architectural changes
**Related**: 04-architecture-full.puml, service-architecture-revision-report.md

#### `07-archived/research/architecture/service-architecture-revision-report.md`
**Nội dung**: Architecture decision records (ADRs)
**Update khi**:
- Thêm design decision mới (e.g., Storage Integration Decision)
- Revise architecture approach
**Related**: system-architecture-v4.md

#### `03-planning/implementation/storage-service-design.md`
**Nội dung**: Storage Service comprehensive design (3,623 lines)
**Update khi**:
- Thay đổi storage architecture
- Thêm file type mới
- Thay đổi quota tiers
**Related**: 02-core-prs.md, database-design.md, api-design.md

#### `07-archived/research/services/media-service-analysis.md`
**Nội dung**: Media Service analysis (video streaming, HLS, RTMP)
**Update khi**:
- Research video streaming solutions
- Evaluate Ant Media Server alternatives
**Related**: storage-service-design.md (shared object storage)

---

## 🔗 Cross-Reference Matrix

**Khi update file này** → **Cũng cần check/update files sau:**

| Source File | Must Check | Should Check |
|-------------|-----------|--------------|
| **database-design.md** | database-migration-plan.md, 03-erd.puml | core-service-implementation.md, storage-service-design.md |
| **database-migration-plan.md** | database-design.md | - |
| **03-erd.puml** | database-design.md | 04-architecture-full.puml |
| **04-architecture-full.puml** | system-architecture-v4.md | service-architecture-revision-report.md |
| **api-design.md** | kiteclass-implementation-plan.md | 02-core-prs.md, 03-frontend-prs.md |
| **02-core-prs.md** | 00-master-pr-index.md | core-service-implementation.md, kiteclass-implementation-plan.md |
| **03-frontend-prs.md** | 00-master-pr-index.md | frontend-plan.md |
| **storage-service-design.md** | database-design.md, api-design.md, 02-core-prs.md | frontend-plan.md, core-service-implementation.md, gateway-implementation-plan.md |
| **kiteclass-implementation-plan.md** | 02-core-prs.md, 03-frontend-prs.md | - |

---

## 📋 Update Checklist by Scenario

### ✅ Scenario: Thêm Service/Module Mới (e.g., Storage Service)

**Must Update** (8 files):
1. ✅ `database-design.md` - Add tables section
2. ✅ `database-migration-plan.md` - Add migration (e.g., V13)
3. ✅ `02-core-prs.md` - Add PR entry (e.g., PR 2.10.1)
4. ✅ `03-erd.puml` - Add entities
5. ✅ `04-architecture-full.puml` - Add service component
6. ✅ `api-design.md` - Add API endpoints
7. ✅ `system-architecture-v4.md` - Add service description
8. ✅ `service-architecture-revision-report.md` - Add design decision

**Should Update** (6 files):
9. ✅ `kiteclass-implementation-plan.md` - Add PR prompt
10. ✅ `core-service-implementation.md` - Add integration notes
11. ✅ `frontend-plan.md` - Add UI components (if applicable)
12. ✅ `gateway-implementation-plan.md` - Add cross-service notes
13. ✅ `03-frontend-prs.md` - Update dependencies
14. ✅ `00-master-pr-index.md` - Add to index

**Example**: PR 2.10.1 Storage Service (completed 2026-02-27) - all 14 files updated ✅

---

### ✅ Scenario: Thêm Frontend Feature/Component

**Must Update** (3 files):
1. `frontend-plan.md` - Add component/page section
2. `03-frontend-prs.md` - Add PR entry
3. `00-master-pr-index.md` - Add to index

**Should Update** (2 files):
4. `kiteclass-implementation-plan.md` - Add PR prompt (if major feature)
5. `api-design.md` - Add API endpoints (if new backend APIs needed)

---

### ✅ Scenario: Thêm Database Table/Migration

**Must Update** (3 files):
1. `database-design.md` - Add table schema
2. `database-migration-plan.md` - Add migration entry (V#)
3. `03-erd.puml` - Add entity và relationships

**Should Update** (3 files):
4. `04-architecture-full.puml` - Add data flow (if new component)
5. `core-service-implementation.md` - Add entity/repository notes
6. `02-core-prs.md` - Add PR (if significant change)

---

### ✅ Scenario: Thêm REST API Endpoint

**Must Update** (2 files):
1. `api-design.md` - Add endpoint specification
2. `kiteclass-implementation-plan.md` - Update PR prompt (if in scope)

**Should Update** (2 files):
3. `02-core-prs.md` - Add to PR features list
4. `frontend-plan.md` - Add API integration example

---

## 🗂️ File Directory Structure

```
documents/
├── 01-business/          (Business logic rules)
├── 02-architecture/      (Technical design)
│
├── 03-planning/
│   ├── database/
│   │   ├── database-design.md ⭐⭐⭐ (CRITICAL - Schema design)
│   │   └── database-migration-plan.md ⭐⭐⭐ (CRITICAL - Migrations)
│   │
│   ├── implementation/
│   │   ├── kiteclass-implementation-plan.md ⭐⭐ (Master plan with PR prompts)
│   │   ├── core-service-implementation.md ⭐⭐ (Core details)
│   │   ├── frontend-plan.md ⭐⭐ (Frontend details)
│   │   ├── gateway-implementation-plan.md ⭐ (Gateway details)
│   │   └── storage-service-design.md ⭐⭐ (Storage comprehensive design)
│   │
│   └── prs/
│       ├── 00-master-pr-index.md ⭐⭐ (Master PR list)
│       ├── 01-gateway-prs.md ⭐
│       ├── 02-core-prs.md ⭐⭐ (Core PRs)
│       └── 03-frontend-prs.md ⭐⭐ (Frontend PRs)
│
├── 04-quality/           (Audits, gap checks - was 05-qa-and-best-practices)
├── 05-guides/            (Deploy guides, Vietnamese docs)
│   ├── operations/       (Runbooks, monitoring)
│   └── vietnamese/       (Vietnamese guides)
│
├── 06-diagrams/plantuml/
│   ├── 03-erd.puml ⭐⭐⭐ (CRITICAL - ERD)
│   └── 04-architecture-full.puml ⭐⭐⭐ (CRITICAL - Architecture)
│
├── 07-archived/          (Old docs, reference only)
│   ├── research/         (was 01-research)
│   ├── academic/         (was 02-academic)
│   ├── implementation/   (was 04-implementation)
│   ├── compliance/       (was 06-compliance)
│   ├── logs/             (was 06-logs)
│   ├── early-ideas/
│   └── old-plans/
│
└── .claude/skills/
    └── api-design.md ⭐⭐⭐ (CRITICAL - API specs)
```

**Legend**:
- ⭐⭐⭐ = CRITICAL (update cho mọi major change)
- ⭐⭐ = HIGH (update khi thêm features/PRs)
- ⭐ = MEDIUM (update khi có design decisions)

---

## 🔍 Search by Keyword

| Keyword | Files |
|---------|-------|
| **Student** | database-design.md, 02-core-prs.md, core-service-implementation.md, 03-erd.puml |
| **Teacher** | database-design.md, 02-core-prs.md, core-service-implementation.md, 03-erd.puml |
| **Course** | database-design.md, 02-core-prs.md, core-service-implementation.md, 03-erd.puml |
| **Attendance** | database-design.md, 02-core-prs.md, core-service-implementation.md |
| **Storage/Files** | storage-service-design.md, database-design.md, 02-core-prs.md, frontend-plan.md, api-design.md |
| **Authentication** | gateway-implementation-plan.md, 01-gateway-prs.md, system-architecture-v4.md |
| **JWT** | gateway-implementation-plan.md, api-design.md |
| **Multi-tenant** | database-design.md, core-service-implementation.md, storage-service-design.md |
| **Migration** | database-migration-plan.md, database-design.md |
| **API** | api-design.md, 02-core-prs.md, kiteclass-implementation-plan.md |
| **Frontend** | frontend-plan.md, 03-frontend-prs.md, kiteclass-implementation-plan.md |
| **Testing** | testing-strategy.md, kiteclass-implementation-plan.md |
| **Redis** | core-service-implementation.md, gateway-implementation-plan.md |
| **S3/MinIO** | storage-service-design.md, media-service-analysis.md |
| **Video** | media-service-analysis.md, storage-service-design.md |

---

---

## 🗂️ File Organization

Để xác định vị trí đúng cho file/folder mới, sử dụng skill `/organize`:

```bash
# Usage
/organize <filename> [type]

# Examples
/organize backup-db.sh script
/organize redis.conf config
/organize v5-architecture.md docs
```

**Rules**:
- Scripts → `scripts/`
- Docker orchestration → root (docker-compose.*.yml)
- Service-specific → `kiteclass/[service]/`
- Documentation → `documents/[category]/`
- Nginx config → `nginx/`

**See**: `.claude/skills/organize.md` for detailed rules and decision algorithm.

---

## 📌 Key Documents (Start Here)

Nếu bạn mới bắt đầu hoặc cần overview:

1. **Architecture**: `07-archived/research/architecture/system-architecture-v4.md`
2. **Database**: `03-planning/database/database-design.md`
3. **Implementation**: `03-planning/implementation/kiteclass-implementation-plan.md`
4. **PRs**: `03-planning/prs/00-master-pr-index.md`
5. **API**: `.claude/skills/api-design.md`

Nếu bạn đang implement một PR cụ thể:

1. Check `00-master-pr-index.md` để tìm PR number
2. Check `02-core-prs.md` hoặc `03-frontend-prs.md` để xem dependencies
3. Check `kiteclass-implementation-plan.md` để xem PR prompt (detailed steps)
4. Check `database-design.md` để xem schema (nếu cần)
5. Check `api-design.md` để xem API endpoints (nếu cần)

---

**Last Updated**: 2026-05-29 (freshness review — folder index verified current)
