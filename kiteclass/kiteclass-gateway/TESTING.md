# Testing Guide - KiteClass Gateway

## Prerequisites

### Install Java 17 (First time only)

Run the setup script to install Java 17 in WSL:

```bash
./setup-java.sh
```

After installation, reload your shell configuration:

```bash
source ~/.bashrc
```

Verify Java is installed:

```bash
java -version
# Should show: openjdk version "17.x.x"
```

## Running Tests

### Quick Start

Run all tests:

```bash
./run-tests.sh
```

### Specific Test Types

**Compile only:**
```bash
./run-tests.sh compile
```

**Unit tests only:**
```bash
./run-tests.sh unit
```

**Integration tests only:**
```bash
./run-tests.sh integration
```

**Specific test class:**
```bash
./run-tests.sh service      # UserServiceTest
./run-tests.sh controller   # UserControllerTest
./run-tests.sh repository   # UserRepositoryTest
```

**Full verification (compile + test + coverage):**
```bash
./run-tests.sh verify
```

**Coverage report:**
```bash
./run-tests.sh coverage
# Report: target/site/jacoco/index.html
```

## Test Structure

```
src/test/java/com/kiteclass/gateway/
├── module/user/
│   ├── service/UserServiceTest.java          # Unit tests (Mockito)
│   ├── controller/UserControllerTest.java    # WebFlux tests
│   └── repository/UserRepositoryTest.java    # Integration tests (Testcontainers)
└── testutil/
    └── UserTestDataBuilder.java              # Test data factory
```

## Test Coverage Goals

- **UserService**: ≥ 80% line coverage
- **UserController**: ≥ 70% line coverage
- **UserRepository**: ≥ 60% line coverage

## Manual Maven Commands

If you prefer using Maven directly:

```bash
# All tests
./mvnw test

# Compile only
./mvnw compile

# Clean and test
./mvnw clean test

# Full build with tests
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Run specific test
./mvnw test -Dtest=UserServiceTest

# Run specific test method
./mvnw test -Dtest=UserServiceTest#createUser_shouldCreateUserSuccessfully

# Debug mode
./mvnw test -X
```

## Troubleshooting

### Java not found

If you get "JAVA_HOME not defined" error:

```bash
./setup-java.sh
source ~/.bashrc
```

### Testcontainers issues

Testcontainers requires Docker. Ensure Docker Desktop is running:

```bash
docker ps
```

### Port conflicts

If tests fail due to port conflicts, check for running services:

```bash
# Check port 5432 (PostgreSQL)
netstat -tulpn | grep 5432

# Stop conflicting services
sudo systemctl stop postgresql
```

### Clean build

If tests behave unexpectedly, try a clean build:

```bash
./mvnw clean
rm -rf target/
./run-tests.sh
```

## CI/CD Integration

For GitHub Actions or other CI:

```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v3
  with:
    java-version: '17'
    distribution: 'temurin'

- name: Run tests
  run: ./mvnw clean verify

- name: Upload coverage
  uses: codecov/codecov-action@v3
  with:
    files: ./target/site/jacoco/jacoco.xml
```

## Test Data

Test data is created using `UserTestDataBuilder`:

```java
// Create test user
User user = UserTestDataBuilder.createUser(1L, "test@example.com", "Test User");

// Create request
CreateUserRequest request = UserTestDataBuilder.createUserRequest(
    "test@example.com",
    "Test User"
);

// Create role
Role role = UserTestDataBuilder.createRole(1L, "ADMIN", "Administrator");
```

## Database for Tests

- **Unit tests**: Mocked repositories (no database)
- **WebFlux tests**: Mocked services (no database)
- **Integration tests**: PostgreSQL in Docker (Testcontainers)

Testcontainers automatically:
- Pulls PostgreSQL Docker image
- Starts container before tests
- Runs migrations (Flyway)
- Cleans up after tests
