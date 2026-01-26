# Skill: Environment Setup

Hướng dẫn cài đặt môi trường phát triển KiteClass Platform.

## Mô tả

Tài liệu hướng dẫn setup môi trường dev:
- Prerequisites (tools, versions)
- Clone và cấu hình repositories
- Docker Compose cho local development
- IDE setup (IntelliJ, VS Code)
- Environment variables

## Trigger phrases

- "cài đặt môi trường"
- "setup environment"
- "docker compose"
- "chạy local"
- "development setup"

---

## Prerequisites

### Required Tools

| Tool | Version | Mục đích | Download |
|------|---------|----------|----------|
| **JDK** | 17+ | Backend runtime | [Adoptium](https://adoptium.net/) |
| **Node.js** | 20 LTS | Frontend runtime | [nodejs.org](https://nodejs.org/) |
| **pnpm** | 8+ | Package manager | `npm install -g pnpm` |
| **Docker** | 24+ | Containers | [docker.com](https://docker.com/) |
| **Docker Compose** | 2.20+ | Multi-container | Included with Docker |
| **Git** | 2.40+ | Version control | [git-scm.com](https://git-scm.com/) |

### Optional Tools

| Tool | Mục đích |
|------|----------|
| **IntelliJ IDEA** | Java IDE (Community hoặc Ultimate) |
| **VS Code** | Frontend IDE |
| **DBeaver** | Database GUI |
| **Postman** | API testing |
| **Redis Insight** | Redis GUI |

### Verify Installation

```bash
# Check versions
java --version       # Java 17+
node --version       # v20.x
pnpm --version       # 8.x
docker --version     # 24.x
docker compose version  # 2.20+
git --version        # 2.40+
```

---

## Repository Structure

```
kiteclass-platform/
├── kitehub/                    # SaaS management platform
│   ├── kitehub-backend/        # Spring Boot monolith
│   └── kitehub-frontend/       # Next.js
│
├── kiteclass/                  # Per-tenant instance
│   ├── kiteclass-gateway/      # Spring Cloud Gateway + User Service
│   ├── kiteclass-core/         # Core business service
│   ├── kiteclass-engagement/   # Gamification, Forum (optional)
│   ├── kiteclass-media/        # Video processing (optional)
│   └── kiteclass-frontend/     # Next.js
│
├── infrastructure/             # DevOps configs
│   ├── docker/                 # Docker compose files
│   ├── kubernetes/             # K8s manifests
│   └── scripts/                # Utility scripts
│
└── docs/                       # Documentation
```

---

## Quick Start (Docker Compose)

### 1. Clone Repository

```bash
git clone https://github.com/your-org/kiteclass-platform.git
cd kiteclass-platform
```

### 2. Start Infrastructure

```bash
# Start databases, cache, message queue
cd infrastructure/docker
docker compose -f docker-compose.infra.yml up -d

# Verify containers
docker compose -f docker-compose.infra.yml ps
```

### 3. Start Backend Services

```bash
# Option 1: Run all services with Docker
docker compose -f docker-compose.services.yml up -d

# Option 2: Run individual service locally (for development)
cd kiteclass/kiteclass-core
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Start Frontend

```bash
cd kiteclass/kiteclass-frontend
pnpm install
pnpm dev
```

### 5. Access Applications

| Application | URL |
|-------------|-----|
| Frontend | http://localhost:3000 |
| Gateway API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ UI | http://localhost:15672 (guest/guest) |
| PgAdmin | http://localhost:5050 (admin@local.dev/admin) |

---

## Docker Compose Files

### docker-compose.infra.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: kiteclass-postgres
    environment:
      POSTGRES_USER: kiteclass
      POSTGRES_PASSWORD: kiteclass123
      POSTGRES_DB: kiteclass_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U kiteclass"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: kiteclass-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: kiteclass-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: kiteclass
      RABBITMQ_DEFAULT_PASS: kiteclass123
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  pgadmin:
    image: dpage/pgadmin4
    container_name: kiteclass-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@local.dev
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
```

### docker-compose.services.yml

```yaml
version: '3.8'

services:
  gateway:
    build:
      context: ../../kiteclass/kiteclass-gateway
      dockerfile: Dockerfile
    container_name: kiteclass-gateway
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kiteclass_dev
      - SPRING_REDIS_HOST=redis
      - CORE_SERVICE_URL=http://core:8081
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

  core:
    build:
      context: ../../kiteclass/kiteclass-core
      dockerfile: Dockerfile
    container_name: kiteclass-core
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/kiteclass_dev
      - SPRING_REDIS_HOST=redis
      - SPRING_RABBITMQ_HOST=rabbitmq
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_started

  frontend:
    build:
      context: ../../kiteclass/kiteclass-frontend
      dockerfile: Dockerfile
    container_name: kiteclass-frontend
    environment:
      - NEXT_PUBLIC_API_URL=http://localhost:8080
    ports:
      - "3000:3000"
    depends_on:
      - gateway

networks:
  default:
    name: kiteclass-network
    external: true
```

---

## Environment Variables

### Backend (.env hoặc application-local.yml)

```yaml
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kiteclass_dev
SPRING_DATASOURCE_USERNAME=kiteclass
SPRING_DATASOURCE_PASSWORD=kiteclass123

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=kiteclass
SPRING_RABBITMQ_PASSWORD=kiteclass123

# JWT
JWT_SECRET=your-super-secret-key-min-512-bits-long-for-hs512
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# External Services (Optional)
ZALO_APP_ID=your-zalo-app-id
ZALO_APP_SECRET=your-zalo-secret
VNPAY_TMN_CODE=your-vnpay-code
VNPAY_HASH_SECRET=your-vnpay-secret
```

### Frontend (.env.local)

```bash
# API
NEXT_PUBLIC_API_URL=http://localhost:8080

# Auth
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-nextauth-secret

# Optional: Analytics
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX
```

---

## IDE Setup

### IntelliJ IDEA (Backend)

1. **Import Project**
   - File → Open → Chọn folder `kiteclass-core`
   - Import as Maven project

2. **Configure JDK**
   - File → Project Structure → SDKs → Add JDK 17

3. **Run Configuration**
   - Run → Edit Configurations → Add Spring Boot
   - Main class: `com.kiteclass.core.KiteclassCoreApplication`
   - Active profiles: `local`

4. **Plugins khuyên dùng**
   - Lombok
   - Spring Boot Assistant
   - Database Navigator

### VS Code (Frontend)

1. **Open Folder**
   - File → Open Folder → `kiteclass-frontend`

2. **Extensions khuyên dùng**
   ```json
   // .vscode/extensions.json
   {
     "recommendations": [
       "dbaeumer.vscode-eslint",
       "esbenp.prettier-vscode",
       "bradlc.vscode-tailwindcss",
       "prisma.prisma",
       "dsznajder.es7-react-js-snippets"
     ]
   }
   ```

3. **Settings**
   ```json
   // .vscode/settings.json
   {
     "editor.defaultFormatter": "esbenp.prettier-vscode",
     "editor.formatOnSave": true,
     "editor.codeActionsOnSave": {
       "source.fixAll.eslint": true
     }
   }
   ```

---

## Database Setup

### Create Database

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create user and database
CREATE USER kiteclass WITH PASSWORD 'kiteclass123';
CREATE DATABASE kiteclass_dev OWNER kiteclass;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE kiteclass_dev TO kiteclass;
```

### Run Migrations

```bash
# Flyway migrations run automatically on startup
# Or manually:
cd kiteclass/kiteclass-core
./mvnw flyway:migrate
```

### Seed Data

```bash
# Insert sample data
./mvnw spring-boot:run -Dspring-boot.run.arguments="--seed=true"
```

---

## Common Commands

### Backend

```bash
# Build
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Run with profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Generate OpenAPI spec
./mvnw springdoc-openapi:generate
```

### Frontend

```bash
# Install dependencies
pnpm install

# Development
pnpm dev

# Build
pnpm build

# Lint
pnpm lint

# Type check
pnpm type-check
```

### Docker

```bash
# Start all
docker compose up -d

# View logs
docker compose logs -f gateway

# Stop all
docker compose down

# Clean volumes
docker compose down -v
```

---

## Troubleshooting

### Port already in use

```bash
# Find process using port
lsof -i :8080
# or on Windows
netstat -ano | findstr :8080

# Kill process
kill -9 <PID>
```

### Docker network issues

```bash
# Create network if not exists
docker network create kiteclass-network

# Inspect network
docker network inspect kiteclass-network
```

### Database connection failed

```bash
# Check if PostgreSQL is running
docker compose ps postgres

# Check logs
docker compose logs postgres

# Connect manually
psql -h localhost -U kiteclass -d kiteclass_dev
```

### Java version mismatch

```bash
# Check JAVA_HOME
echo $JAVA_HOME

# Set correct Java version (macOS with brew)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

---

## Useful Scripts

### scripts/dev-start.sh

```bash
#!/bin/bash

echo "Starting KiteClass Development Environment..."

# Start infrastructure
cd infrastructure/docker
docker compose -f docker-compose.infra.yml up -d

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
until docker compose -f docker-compose.infra.yml exec -T postgres pg_isready -U kiteclass; do
  sleep 2
done

echo "Infrastructure ready!"
echo ""
echo "Next steps:"
echo "  1. cd kiteclass/kiteclass-core && ./mvnw spring-boot:run"
echo "  2. cd kiteclass/kiteclass-frontend && pnpm dev"
```

### scripts/dev-stop.sh

```bash
#!/bin/bash

echo "Stopping KiteClass Development Environment..."

cd infrastructure/docker
docker compose -f docker-compose.infra.yml down

echo "Done!"
```

## Actions

### Chạy lần đầu
```bash
chmod +x scripts/*.sh
./scripts/dev-start.sh
```

### Reset database
```bash
docker compose -f docker-compose.infra.yml down -v
docker compose -f docker-compose.infra.yml up -d
```
