# Docker Setup Guide - KiteClass Gateway

Complete guide for running KiteClass Gateway with Docker.

---

## Prerequisites

### Required Software

**1. Docker Desktop**
- Download: https://www.docker.com/products/docker-desktop
- Minimum version: 20.10+
- Ensure Docker daemon is running: `docker ps`

**2. Docker Compose**
- Included with Docker Desktop
- Minimum version: 2.0+
- Verify: `docker-compose --version`

### System Requirements

- **RAM:** Minimum 4GB, recommended 8GB+
- **Disk:** 5GB free space for images and volumes
- **OS:** Windows 10+, macOS 10.15+, or Linux

---

## Quick Start (5 Minutes)

### 1. Clone and Navigate

```bash
cd kiteclass/kiteclass-gateway
```

### 2. Create Environment File

```bash
cp .env.example .env
```

Edit `.env` and update values (optional for dev):
- JWT_SECRET (REQUIRED for production)
- Database credentials
- Redis configuration

### 3. Start Services

```bash
# Start all services (postgres, redis, gateway)
docker-compose up -d

# View logs
docker-compose logs -f gateway
```

### 4. Verify Services

```bash
# Check all containers are running
docker-compose ps

# Should show:
# kiteclass-gateway-postgres   Up (healthy)
# kiteclass-gateway-redis      Up (healthy)
# kiteclass-gateway            Up (healthy)
```

### 5. Test API

```bash
# Health check
curl http://localhost:8080/actuator/health

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@kiteclass.local","password":"Admin@123"}'
```

**Success!** Gateway is running with Docker.

---

## Development Mode

For active development with hot reload:

```bash
# Start with dev overrides
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up

# This enables:
# - Source code mounting (hot reload)
# - Debug port 5005
# - Verbose logging
# - Maven cache mounting
```

**Connect debugger:**
- Host: localhost
- Port: 5005
- Type: Remote JVM Debug

---

## Common Commands

### Service Management

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# Restart gateway only
docker-compose restart gateway

# View logs
docker-compose logs -f
docker-compose logs -f gateway  # Gateway only

# Check status
docker-compose ps
```

### Database Management

```bash
# Connect to PostgreSQL
docker exec -it kiteclass-gateway-postgres psql -U kiteclass -d kiteclass_dev

# Useful SQL commands
\dt                          # List tables
\d users                     # Describe users table
SELECT * FROM users;         # Query users
\q                           # Quit

# Backup database
docker exec kiteclass-gateway-postgres pg_dump -U kiteclass kiteclass_dev > backup.sql

# Restore database
docker exec -i kiteclass-gateway-postgres psql -U kiteclass kiteclass_dev < backup.sql
```

### Redis Management

```bash
# Connect to Redis
docker exec -it kiteclass-gateway-redis redis-cli

# Useful Redis commands
KEYS *                       # List all keys
GET refresh_token:123        # Get value
FLUSHALL                     # Clear all data
INFO                         # Server info
EXIT                         # Quit

# Monitor real-time commands
docker exec -it kiteclass-gateway-redis redis-cli MONITOR
```

### Container Management

```bash
# Rebuild gateway image
docker-compose build gateway

# Rebuild without cache
docker-compose build --no-cache gateway

# Remove all containers and volumes
docker-compose down -v

# View container stats
docker stats
```

---

## Troubleshooting

### Port Already in Use

**Error:** `Bind for 0.0.0.0:5432 failed: port is already allocated`

**Solution:**
```bash
# Find process using port
sudo lsof -i :5432  # macOS/Linux
netstat -ano | findstr :5432  # Windows

# Stop the process or change port in docker-compose.yml
ports:
  - "5433:5432"  # Use different host port
```

### Container Won't Start

**Error:** `container exited with code 1`

**Solution:**
```bash
# View logs for error details
docker-compose logs gateway

# Common issues:
# 1. Database not ready → Wait for healthcheck
# 2. Missing .env file → Copy from .env.example
# 3. Invalid JWT secret → Check .env
```

### Database Connection Failed

**Error:** `Connection refused` or `Connection timeout`

**Solution:**
```bash
# Check postgres is healthy
docker-compose ps

# If not healthy, check logs
docker-compose logs postgres

# Restart postgres
docker-compose restart postgres

# Wait for healthcheck
docker-compose up -d postgres
sleep 10  # Wait 10 seconds
```

### Out of Memory

**Error:** `Cannot allocate memory`

**Solution:**
```bash
# Increase Docker memory limit (Docker Desktop → Settings → Resources)
# Recommended: 4GB minimum, 8GB+ for production

# Or reduce Java heap size in Dockerfile
ENV JAVA_OPTS="-Xmx512m -Xms256m"
```

### Clean Start (Nuclear Option)

If nothing works, clean slate:

```bash
# Stop everything
docker-compose down -v

# Remove all images
docker rmi $(docker images 'kiteclass-gateway*' -q)

# Remove all volumes
docker volume prune -f

# Start fresh
docker-compose up -d --build
```

---

## Production Deployment

### Security Checklist

- [ ] Change JWT_SECRET to secure 512-bit key
- [ ] Use strong database password
- [ ] Enable Redis password: `command: redis-server --requirepass yourpassword`
- [ ] Use TLS/SSL for database connections
- [ ] Limit exposed ports (remove host bindings)
- [ ] Enable firewall rules
- [ ] Use Docker secrets for sensitive data

### Example Production docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets:
      - db_password
    # Don't expose port to host
    ports: []
    networks:
      - internal

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    # Don't expose port to host
    ports: []
    networks:
      - internal

  gateway:
    image: your-registry/kiteclass-gateway:1.0.0
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JWT_SECRET_FILE: /run/secrets/jwt_secret
    secrets:
      - jwt_secret
    ports:
      - "8080:8080"
    networks:
      - internal
      - external

secrets:
  db_password:
    external: true
  jwt_secret:
    external: true

networks:
  internal:
    internal: true
  external:
    driver: bridge
```

### Resource Limits

Add resource limits to prevent resource exhaustion:

```yaml
services:
  gateway:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 512M
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Start services
        run: |
          cd kiteclass/kiteclass-gateway
          docker-compose up -d postgres redis
          sleep 10  # Wait for services to be healthy

      - name: Run tests
        run: |
          cd kiteclass/kiteclass-gateway
          docker-compose run --rm gateway ./mvnw clean verify

      - name: Stop services
        run: |
          cd kiteclass/kiteclass-gateway
          docker-compose down -v
```

---

## Monitoring

### View Real-Time Logs

```bash
# All services
docker-compose logs -f

# Gateway only
docker-compose logs -f gateway

# Last 100 lines
docker-compose logs --tail=100 gateway

# With timestamps
docker-compose logs -f --timestamps gateway
```

### Health Checks

```bash
# Check all services
curl http://localhost:8080/actuator/health

# Detailed health info
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/health/liveness

# Database health
docker exec kiteclass-gateway-postgres pg_isready -U kiteclass

# Redis health
docker exec kiteclass-gateway-redis redis-cli ping
```

### Performance Monitoring

```bash
# Container stats (CPU, memory, network)
docker stats

# Detailed container info
docker inspect kiteclass-gateway

# Check disk usage
docker system df
```

---

## Backup and Restore

### Database Backup

```bash
# Full backup
docker exec kiteclass-gateway-postgres pg_dump -U kiteclass kiteclass_dev > backup_$(date +%Y%m%d).sql

# Schema only
docker exec kiteclass-gateway-postgres pg_dump -U kiteclass --schema-only kiteclass_dev > schema.sql

# Data only
docker exec kiteclass-gateway-postgres pg_dump -U kiteclass --data-only kiteclass_dev > data.sql
```

### Database Restore

```bash
# Restore from backup
docker exec -i kiteclass-gateway-postgres psql -U kiteclass kiteclass_dev < backup.sql

# Or with docker-compose
docker-compose exec -T postgres psql -U kiteclass kiteclass_dev < backup.sql
```

### Redis Backup

```bash
# Save snapshot
docker exec kiteclass-gateway-redis redis-cli SAVE

# Copy snapshot to host
docker cp kiteclass-gateway-redis:/data/dump.rdb ./redis-backup.rdb

# Restore snapshot
docker cp redis-backup.rdb kiteclass-gateway-redis:/data/dump.rdb
docker-compose restart redis
```

---

## FAQ

### Q: Do I need Docker for development?

**A:** No, but recommended. You can run PostgreSQL and Redis locally instead:
```bash
# Install locally (macOS)
brew install postgresql@15 redis

# Start services
brew services start postgresql@15
brew services start redis

# Update application.yml with localhost
```

### Q: Can I use Docker for tests?

**A:** Yes! Integration tests use Testcontainers which run PostgreSQL in Docker automatically.

### Q: How do I update the gateway image?

**A:**
```bash
# Pull latest code
git pull origin feature/gateway

# Rebuild image
docker-compose build gateway

# Restart with new image
docker-compose up -d gateway
```

### Q: Where are volumes stored?

**A:**
- **macOS:** `~/Library/Containers/com.docker.docker/Data/vms/0/`
- **Linux:** `/var/lib/docker/volumes/`
- **Windows:** `C:\ProgramData\Docker\volumes\`

View volumes:
```bash
docker volume ls
docker volume inspect kiteclass-gateway_postgres_data
```

### Q: How do I run specific profile?

**A:**
```bash
# In .env file
SPRING_PROFILES_ACTIVE=prod

# Or override in docker-compose
docker-compose run -e SPRING_PROFILES_ACTIVE=test gateway ./mvnw test
```

---

## References

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Redis Docker Image](https://hub.docker.com/_/redis)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)

---

## Support

**Issues?** Check:
1. Docker daemon is running: `docker ps`
2. Ports are available: `lsof -i :8080 :5432 :6379`
3. Disk space: `df -h`
4. Memory: `docker stats`
5. Logs: `docker-compose logs -f`

**Still stuck?** Open an issue with:
- `docker-compose logs`
- `docker-compose ps`
- `docker version`
- Error message

---

**Last Updated:** 2026-01-26
**Version:** 1.0.0
**Author:** VictorAurelius + Claude Sonnet 4.5
