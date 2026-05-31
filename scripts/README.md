# Shared Monorepo Scripts

Generic utilities shared across all projects in the monorepo.

## 📁 Structure

```
scripts/
├── test-local.sh                # Generic test runner with auto-cleanup
├── dev-docker.sh                # Docker Compose wrapper
├── cleanup-testcontainers.sh    # Testcontainers cleanup utility
└── README.md                    # This file
```

## 🛠️ Available Scripts

### test-local.sh
Run tests for any project with automatic Testcontainers cleanup.

**Usage:**
```bash
# KiteClass (default, backward compatible)
./scripts/test-local.sh [core|gateway|all]

# Specific project
./scripts/test-local.sh <project> [core|gateway|all]

# Examples
./scripts/test-local.sh core              # Test kiteclass-core
./scripts/test-local.sh gateway           # Test kiteclass-gateway
./scripts/test-local.sh all               # Test all kiteclass services
./scripts/test-local.sh kiteclass core    # Explicit project
./scripts/test-local.sh kitehub backend   # Future: test kitehub-backend
```

**Features:**
- Automatically cleans up Testcontainers after tests
- Runs cleanup even if tests fail (via trap)
- Colored output for better readability
- Backward compatible with existing usage

### dev-docker.sh
Docker Compose wrapper for development environments.

**Usage:**
```bash
./scripts/dev-docker.sh [command]

# Commands
./scripts/dev-docker.sh up        # Start all services
./scripts/dev-docker.sh down      # Stop all services
./scripts/dev-docker.sh build     # Build Docker images
./scripts/dev-docker.sh rebuild   # Rebuild and restart
./scripts/dev-docker.sh logs      # Show logs
./scripts/dev-docker.sh restart   # Restart services
./scripts/dev-docker.sh status    # Show container status
./scripts/dev-docker.sh clean     # Remove containers and volumes
```

**Configuration:**
```bash
# Default: kiteclass/docker-compose.dev.yml
./scripts/dev-docker.sh up

# Override with environment variable
COMPOSE_FILE=kitehub/docker-compose.dev.yml ./scripts/dev-docker.sh up
```

**Features:**
- Health checks for all services
- Colored output with status indicators
- Confirmation prompt for destructive operations

### cleanup-testcontainers.sh
Clean up orphaned Testcontainers (project-agnostic).

**Usage:**
```bash
./scripts/cleanup-testcontainers.sh
```

**When to use:**
- After interrupting tests with Ctrl+C
- When Testcontainers fail to auto-cleanup
- Before running tests to ensure clean state
- To free up system resources

**Identifies and removes:**
- Containers with label `org.testcontainers=true`
- Both running and stopped containers
- Automatically called by `test-local.sh`

## 📦 Project-Specific Scripts

Each project has its own scripts directory for project-specific utilities:

- **[KiteClass Scripts](../kiteclass/scripts/)** - KiteClass development tools
  - `init-admin.sh` - Initialize admin user
  - `seed-data.sh` - Seed sample data
  - `dev-start.sh` - Start services locally (without Docker)
  - `dev-rebuild.sh` - Rebuild and restart Docker services
  - `dev-status.sh` - Check service status
  - `dev-stop.sh` - Stop local services
  - `docker-build.sh` - Build Docker images with versioning
  - `docker-version.sh` - Show current Docker image versions
  - `check-problems.sh` - Check for compile/lint errors

- **[KiteHub Scripts](../kitehub/scripts/)** - KiteHub development tools
  - `up.sh` / `down.sh` - Start/stop KiteHub stack
  - `seed-data.sh` - Seed base accounts + sample data
  - `seed-demo-independent-teachers.sh` - Seed demo 3 giảng viên độc lập (cô Khánh THPT Pháp luật / cô Hà Toán Tiểu học FREE / thầy Nhì Hóa THCS PREMIUM) cho thesis Chương 3 + §4.2. Tự provision tenant qua `POST /api/auth/register`. Override `SEED_TENANTS="khanh ha nhi"`.

## 🔧 Design Principles

### Why Separate Shared Scripts?

1. **Reusability** - Same scripts work for all projects
2. **Maintainability** - Update once, benefit everywhere
3. **Consistency** - Same workflow across projects
4. **Simplicity** - No duplication of common utilities

### Backward Compatibility

Scripts maintain backward compatibility with existing usage:
- `test-local.sh` defaults to `kiteclass` project
- `dev-docker.sh` uses `COMPOSE_FILE` environment variable
- No breaking changes to current workflows

### Project-Specific vs Shared

**Use shared scripts for:**
- Generic operations (tests, docker, cleanup)
- Cross-project utilities
- Operations that work the same way everywhere

**Use project-specific scripts for:**
- Business logic (seed data, init admin)
- Project-specific workflows
- Service management within a project

## 📝 Examples

### Running Tests

```bash
# Quick test (default project)
./scripts/test-local.sh all

# Test specific service
./scripts/test-local.sh core

# Test different project
./scripts/test-local.sh kitehub backend
```

### Docker Development

```bash
# Start KiteClass (default)
./scripts/dev-docker.sh up

# Start KiteHub (future)
COMPOSE_FILE=kitehub/docker-compose.dev.yml ./scripts/dev-docker.sh up

# View logs
./scripts/dev-docker.sh logs

# Cleanup
./scripts/dev-docker.sh clean
```

### Cleanup Containers

```bash
# After interrupted tests
Ctrl+C  # Interrupt tests
./scripts/cleanup-testcontainers.sh

# Before starting fresh
./scripts/cleanup-testcontainers.sh
./scripts/test-local.sh all
```

## 🤝 Contributing

When adding new shared scripts:
1. Ensure they are truly generic (work for all projects)
2. Use parameters/environment variables for project-specific paths
3. Maintain backward compatibility when possible
4. Document usage in this README
5. Add colored output for better UX

---

**Last Updated**: 2026-05-29
