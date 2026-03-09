# File Organization Skill

## Purpose
Automatically determine correct location for files/folders trong KiteClass Platform project.

## Project Structure Rules

### Scripts (`scripts/`)
**What belongs here**:
- Development scripts (dev-*.sh, test-*.sh)
- Build scripts (docker-build.sh, docker-version.sh)
- Database scripts (seed-data.sh, init-admin.sh)
- Utility scripts (cleanup-*.sh, check-*.sh)

**Naming conventions**:
- dev-[action].sh: Development lifecycle (dev-start.sh, dev-rebuild.sh)
- test-[scope].sh: Testing scripts (test-local.sh)
- init-[resource].sh: Initialization scripts (init-admin.sh, init-minio.sh)
- cleanup-[resource].sh: Cleanup scripts (cleanup-testcontainers.sh)
- seed-[data].sh: Data seeding scripts (seed-data.sh)
- docker-*.sh: Docker-related utilities (docker-build.sh, docker-version.sh)
- check-*.sh: Validation scripts (check-problems.sh)

**Examples**: dev-docker.sh, docker-build.sh, cleanup-testcontainers.sh

---

### Docker Orchestration (Root)
**What belongs here**:
- docker-compose.*.yml: Main orchestration files
- .dockerignore: Docker ignore patterns

**Why root**: Referenced by all services, needs to be at top level

**Examples**: docker-compose.dev.yml, docker-compose.prod.yml

---

### Service-Specific Files (`kiteclass/[service]/`)
**What belongs here**:
- Dockerfile: Service-specific builds
- pom.xml / package.json: Dependency management
- application.yml: Service configuration
- checkstyle.xml: Code style rules
- src/: Source code

**Examples**:
- kiteclass/kiteclass-core/Dockerfile
- kiteclass/kiteclass-gateway/pom.xml
- kiteclass/kiteclass-frontend/package.json

---

### Nginx Configuration (`nginx/`)
**What belongs here**:
- nginx.conf: Main config
- *.conf: Additional configs (future: SSL certs, site configs)

**Examples**: nginx/nginx.conf, nginx/ssl/ (future)

---

### Documentation (`documents/`)
**What belongs here**:
- Technical docs: architecture, database, API design
- Planning docs: PR lists, implementation plans
- Research docs: competitive analysis, service analysis

**Structure**:
- `01-research/`: Architecture, services, technology
- `02-academic/`: Thesis materials
- `03-planning/`: Implementation plans, database, PRs
- `04-implementation/`: PR reviews, code analysis
- `06-diagrams/`: PlantUML diagrams
- `07-archived/`: Old/deprecated documents

**Examples**:
- documents/01-research/architecture/system-architecture-v4.md
- documents/03-planning/prs/02-core-prs.md

---

### GitHub Workflows (`.github/workflows/`)
**What belongs here**:
- CI/CD workflows: build, test, deploy
- Automation workflows: PR checks, code quality

**Examples**: .github/workflows/ci.yml, .github/workflows/codeql.yml

---

### Root-Level Documentation
**What belongs here**:
- README.md: Project overview
- DOCKER-BUILD-GUIDE.md: Docker build instructions
- TESTING-GUIDE.md: Testing instructions
- CURRENT-WORK.md: Work tracking

**Why root**: High discoverability, user-facing documentation

---

## Decision Algorithm

Given a file/folder, suggest location based on:

### 1. File Extension Analysis
- `.sh` → scripts/
- `.yml` (docker-compose*) → root
- `.yml` (other) → kiteclass/[service]/
- `.conf` (nginx*) → nginx/
- `.md` (docs) → documents/[category]/
- `.md` (guides) → root

### 2. Name Pattern Analysis
- `dev-*.sh` → scripts/
- `test-*.sh` → scripts/
- `docker-*.sh` → scripts/
- `init-*.sh` → scripts/
- `cleanup-*.sh` → scripts/
- `seed-*.sh` → scripts/
- `check-*.sh` → scripts/
- `docker-compose.*.yml` → root
- `*.conf` (nginx) → nginx/

### 3. Content Analysis (if available)
- Contains Spring Boot config → kiteclass/[service]/
- Contains npm scripts → kiteclass/kiteclass-frontend/
- Contains Docker build steps → kiteclass/[service]/Dockerfile
- Contains architecture diagrams → documents/06-diagrams/

### 4. User Intent (from file-type parameter)
- `script` → scripts/
- `config` → Check context (nginx → nginx/, service → kiteclass/)
- `docs` → documents/[category]/
- `workflow` → .github/workflows/

## Output Format

When suggesting location:
```
📍 Suggested Location: scripts/docker-backup.sh

Reasoning:
✅ File extension: .sh (script)
✅ Name pattern: docker-* (Docker-related script)
✅ Existing pattern: Other Docker scripts in scripts/
✅ Naming convention: Follows scripts/ naming pattern

Alternative considerations:
⚠️ Could be root, but scripts/ is more organized
⚠️ Ensure script is executable (chmod +x)

Recommendation: Move to scripts/ and update any references in:
- .github/workflows/*.yml
- README.md
- Other scripts
```

## Implementation Steps

When user confirms:
1. Move file: `git mv [source] [destination]`
2. Update permissions: `chmod +x [destination]` (if script)
3. Search for references: `grep -r "[filename]" .`
4. Update found references
5. Commit changes with descriptive message

## Examples

### Example 1: New backup script
```
Input: backup-db.sh (script)
Output: scripts/backup-db.sh
Reason: Matches scripts/ naming convention, utility script
```

### Example 2: New nginx SSL config
```
Input: ssl.conf (config, nginx)
Output: nginx/ssl.conf
Reason: nginx-related configuration
```

### Example 3: New architecture document
```
Input: v5-architecture.md (docs)
Output: documents/01-research/architecture/system-architecture-v5.md
Reason: Follows existing versioning pattern in architecture docs
```

### Example 4: New service
```
Input: notification-service/ (folder)
Output: kiteclass/kiteclass-notification/
Reason: Follows kiteclass/kiteclass-* naming pattern for services
```

## Anti-Patterns (What NOT to do)

❌ **Don't put scripts in root** (except docker-compose.*.yml)
❌ **Don't put service configs in root** (belongs in kiteclass/[service]/)
❌ **Don't mix concerns** (e.g., nginx config in kiteclass/)
❌ **Don't duplicate** (check if file type already has a location)
❌ **Don't break conventions** (follow existing naming patterns)

## Validation Checklist

Before confirming location:
- [ ] Does location follow existing pattern?
- [ ] Are similar files in the same location?
- [ ] Will this location require updating many references?
- [ ] Is the file easily discoverable in this location?
- [ ] Does naming convention match directory standards?

## Integration with Development Workflow

**When to use this skill**:
- Before creating a new file/folder
- When reviewing PR that adds new files
- When cleaning up project structure
- When onboarding new developers (teach project structure)

**Automation opportunity**:
- Pre-commit hook: Check if new files are in correct location
- PR template: Checklist item for file organization
- Documentation: Link to this skill in contributing guide
