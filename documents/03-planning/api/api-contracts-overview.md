# KiteClass API Contracts

**Version:** 1.0
**Created:** 2026-03-09
**Purpose:** Define API contract strategy for Frontend-Backend integration
**Status:** Design phase

---

## Table of Contents

1. [Overview](#overview)
2. [OpenAPI Specification Strategy](#openapi-specification-strategy)
3. [API Versioning Strategy](#api-versioning-strategy)
4. [Frontend API Bindings](#frontend-api-bindings)
5. [Contract Testing](#contract-testing)
6. [Documentation Generation](#documentation-generation)

---

## Overview

**Goal:** Document all REST APIs to enable seamless Frontend-Backend integration without manual coordination

**Key Benefits:**
- **Type Safety:** Auto-generated TypeScript types from OpenAPI spec
- **Contract Testing:** Verify backend fulfills frontend expectations (Pact)
- **Documentation:** Auto-generated API docs (Swagger UI)
- **Version Control:** Track breaking changes with API versioning

**Tools:**
- **Springdoc OpenAPI:** Generate OpenAPI 3.0 spec from Spring Boot code
- **openapi-typescript:** Generate TypeScript types from OpenAPI spec
- **Pact:** Consumer-driven contract testing
- **Swagger UI:** Interactive API documentation

---

## OpenAPI Specification Strategy

### Spring Boot Integration (Springdoc)

**Dependency (Maven):**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Configuration:**
```java
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("KiteClass Core API")
                .version("1.0")
                .description("REST API for KiteClass LMS - Student, Teacher, Course, Class management")
                .contact(new Contact()
                    .name("KiteClass Team")
                    .email("dev@kitehub.me")
                )
                .license(new License()
                    .name("Proprietary")
                )
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api-staging.kitehub.me").description("Staging"),
                new Server().url("https://api.kitehub.me").description("Production")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

### Auto-Generate OpenAPI JSON

**Endpoints:**
- OpenAPI Spec (JSON): `http://localhost:8080/v3/api-docs`
- OpenAPI Spec (YAML): `http://localhost:8080/v3/api-docs.yaml`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

**Export to Static File:**
```bash
# Core Service
curl http://localhost:8080/v3/api-docs \
  > documents/03-planning/api/core-api.json

# Gateway Service
curl http://localhost:9090/v3/api-docs \
  > documents/03-planning/api/gateway-api.json

# KiteHub Services
curl http://localhost:8081/v3/api-docs \
  > documents/03-planning/api/kitehub-subscription-api.json
```

### Annotate Controllers for Better Docs

**Example:**
```java
@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student management APIs")
public class StudentController {

    @Operation(
        summary = "Get all students",
        description = "Retrieve a paginated list of students in the current instance",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved students",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PageResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Forbidden - User does not have permission",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
                )
            )
        }
    )
    @GetMapping
    public ResponseEntity<PageResponse<StudentResponse>> getAllStudents(
        @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sort,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String direction
    ) {
        // Implementation
    }

    @Operation(
        summary = "Create a new student",
        description = "Create a new student record in the system",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Student creation data",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateStudentRequest.class),
                examples = @ExampleObject(
                    name = "Example Student",
                    value = """
                        {
                          "name": "Nguyễn Văn A",
                          "email": "nguyenvana@example.com",
                          "phone": "0912345678",
                          "dateOfBirth": "2005-05-15",
                          "address": "123 Main St, Hanoi"
                        }
                        """
                )
            )
        )
    )
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
        @Valid @RequestBody CreateStudentRequest request
    ) {
        // Implementation
    }
}
```

**Generated OpenAPI Spec:**
```json
{
  "paths": {
    "/api/v1/students": {
      "get": {
        "tags": ["Students"],
        "summary": "Get all students",
        "description": "Retrieve a paginated list of students in the current instance",
        "operationId": "getAllStudents",
        "parameters": [
          {
            "name": "page",
            "in": "query",
            "description": "Page number (0-indexed)",
            "schema": { "type": "integer", "default": 0 }
          }
        ],
        "responses": {
          "200": {
            "description": "Successfully retrieved students",
            "content": {
              "application/json": {
                "schema": { "$ref": "#/components/schemas/PageResponse" }
              }
            }
          }
        }
      }
    }
  }
}
```

---

## API Versioning Strategy

### URL-Based Versioning (Recommended)

**Format:** `/api/v{major}/{resource}`

**Examples:**
- `/api/v1/students` - Version 1 (current stable)
- `/api/v1/courses` - Version 1
- `/api/v2/invoices` - Version 2 (breaking changes)

**Why URL-based (not header-based):**
- ✅ Easy to test in browser/Postman
- ✅ Clear version in logs/monitoring
- ✅ Can cache different versions separately
- ✅ Simple routing in Spring Cloud Gateway

### Version Increment Policy

**Increment major version when:**
- Removing fields from response
- Changing field types (e.g., `string` → `number`)
- Removing endpoints
- Changing required fields

**Keep minor version when:**
- Adding new optional fields
- Adding new endpoints
- Deprecating (but not removing) fields

### Implementation

**Spring Boot Controller:**
```java
// Version 1 (current)
@RestController
@RequestMapping("/api/v1/students")
public class StudentControllerV1 {
    // Current implementation
}

// Version 2 (breaking change - new response format)
@RestController
@RequestMapping("/api/v2/students")
public class StudentControllerV2 {
    // New implementation with different response structure
}
```

### Deprecation Policy

**Timeline:**
1. **Announce deprecation:** Add `@Deprecated` annotation + OpenAPI `deprecated: true`
2. **Grace period:** 6 months (or 2 major releases)
3. **Remove old version:** After grace period

**Example:**
```java
@Deprecated
@Operation(deprecated = true, description = "Deprecated: Use /api/v2/students instead")
@GetMapping("/api/v1/students/{id}")
public ResponseEntity<StudentResponseV1> getStudentV1(@PathVariable Long id) {
    log.warn("Deprecated endpoint called: GET /api/v1/students/{}", id);
    // Implementation
}
```

**OpenAPI Spec:**
```json
{
  "paths": {
    "/api/v1/students/{id}": {
      "get": {
        "deprecated": true,
        "description": "Deprecated: Use /api/v2/students instead"
      }
    }
  }
}
```

---

## Frontend API Bindings

### TypeScript Type Generation

**Tool:** [openapi-typescript](https://github.com/drwpow/openapi-typescript)

**Installation:**
```bash
npm install --save-dev openapi-typescript
```

**Generate Types:**
```bash
# Generate from local OpenAPI spec
npx openapi-typescript documents/03-planning/api/core-api.json \
  -o src/types/api/core-api.types.ts

# Or generate from running service
npx openapi-typescript http://localhost:8080/v3/api-docs \
  -o src/types/api/core-api.types.ts
```

**Generated Types (Example):**
```typescript
// Auto-generated from OpenAPI spec
export interface paths {
  "/api/v1/students": {
    get: operations["getAllStudents"];
    post: operations["createStudent"];
  };
  "/api/v1/students/{id}": {
    get: operations["getStudentById"];
    put: operations["updateStudent"];
    delete: operations["deleteStudent"];
  };
}

export interface components {
  schemas: {
    StudentResponse: {
      id: number;
      name: string;
      email: string;
      phone?: string;
      dateOfBirth: string; // ISO 8601 date
      address?: string;
      status: "ACTIVE" | "INACTIVE" | "GRADUATED";
      createdAt: string; // ISO 8601 datetime
      updatedAt: string;
    };
    CreateStudentRequest: {
      name: string;
      email: string;
      phone?: string;
      dateOfBirth: string;
      address?: string;
    };
    PageResponse: {
      content: components["schemas"]["StudentResponse"][];
      totalElements: number;
      totalPages: number;
      size: number;
      number: number;
    };
    ErrorResponse: {
      error: string;
      message: string;
      timestamp: string;
    };
  };
}

export interface operations {
  getAllStudents: {
    parameters: {
      query: {
        page?: number;
        size?: number;
        sort?: string;
        direction?: "ASC" | "DESC";
      };
    };
    responses: {
      200: {
        content: {
          "application/json": components["schemas"]["PageResponse"];
        };
      };
      403: {
        content: {
          "application/json": components["schemas"]["ErrorResponse"];
        };
      };
    };
  };
  createStudent: {
    requestBody: {
      content: {
        "application/json": components["schemas"]["CreateStudentRequest"];
      };
    };
    responses: {
      201: {
        content: {
          "application/json": components["schemas"]["StudentResponse"];
        };
      };
      400: {
        content: {
          "application/json": components["schemas"]["ErrorResponse"];
        };
      };
    };
  };
}
```

### Type-Safe API Client (Next.js)

**Installation:**
```bash
npm install openapi-fetch
```

**Client Setup:**
```typescript
// src/lib/api-client.ts
import createClient from "openapi-fetch";
import type { paths } from "@/types/api/core-api.types";

const client = createClient<paths>({
  baseUrl: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
});

// Add auth interceptor
client.use({
  onRequest({ request }) {
    const token = localStorage.getItem("accessToken");
    if (token) {
      request.headers.set("Authorization", `Bearer ${token}`);
    }
    return request;
  },
  onResponse({ response }) {
    if (response.status === 401) {
      // Token expired, refresh or redirect to login
      window.location.href = "/login";
    }
    return response;
  },
});

export default client;
```

**Usage in Components:**
```typescript
// src/app/students/page.tsx
import client from "@/lib/api-client";
import { useEffect, useState } from "react";
import type { components } from "@/types/api/core-api.types";

type StudentResponse = components["schemas"]["StudentResponse"];
type PageResponse = components["schemas"]["PageResponse"];

export default function StudentsPage() {
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchStudents() {
      try {
        // Type-safe API call (autocomplete works!)
        const { data, error } = await client.GET("/api/v1/students", {
          params: {
            query: {
              page: 0,
              size: 20,
              sort: "createdAt",
              direction: "DESC",
            },
          },
        });

        if (error) {
          setError(error.message);
        } else {
          setStudents(data.content); // Type: StudentResponse[]
        }
      } catch (err) {
        setError("Failed to fetch students");
      } finally {
        setLoading(false);
      }
    }

    fetchStudents();
  }, []);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h1>Students</h1>
      <ul>
        {students.map((student) => (
          <li key={student.id}>
            {student.name} - {student.email}
          </li>
        ))}
      </ul>
    </div>
  );
}
```

**Benefits:**
- ✅ TypeScript autocomplete for all API endpoints
- ✅ Type checking for request/response payloads
- ✅ Compile-time errors if API contract changes
- ✅ No manual type definitions needed

### Automated Type Generation (CI/CD)

**GitHub Actions Workflow:**
```yaml
# .github/workflows/generate-api-types.yml
name: Generate API Types

on:
  push:
    branches:
      - main
    paths:
      - 'kiteclass-core/src/main/java/**'
      - 'kiteclass-gateway/src/main/java/**'

jobs:
  generate-types:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Build Core Service
        run: |
          cd kiteclass-core
          ./mvnw clean package -DskipTests

      - name: Start Core Service
        run: |
          cd kiteclass-core
          java -jar target/kiteclass-core-*.jar &
          sleep 30  # Wait for service to start

      - name: Generate OpenAPI Spec
        run: |
          curl http://localhost:8080/v3/api-docs \
            > documents/03-planning/api/core-api.json

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Generate TypeScript Types
        run: |
          cd kiteclass-frontend
          npx openapi-typescript ../documents/03-planning/api/core-api.json \
            -o src/types/api/core-api.types.ts

      - name: Commit Updated Types
        run: |
          git config user.name "github-actions"
          git config user.email "github-actions@github.com"
          git add kiteclass-frontend/src/types/api/*.ts
          git commit -m "chore: update API types from OpenAPI spec" || echo "No changes"
          git push
```

**Effect:** Every backend API change automatically generates new TypeScript types

---

## Contract Testing

### Pact (Consumer-Driven Contracts)

**Purpose:** Ensure backend API fulfills frontend expectations

**How it works:**
1. Frontend defines expected API behavior (Pact contract)
2. Frontend tests run and generate contract file
3. Backend verifies it can fulfill the contract

### Frontend (Consumer) Setup

**Installation:**
```bash
npm install --save-dev @pact-foundation/pact
```

**Define Contract (Jest Test):**
```typescript
// src/tests/student-api.pact.test.ts
import { PactV3 } from "@pact-foundation/pact";
import client from "@/lib/api-client";

const provider = new PactV3({
  consumer: "kiteclass-frontend",
  provider: "kiteclass-core",
});

describe("Student API Contract", () => {
  it("should return a list of students", async () => {
    await provider
      .given("there are 2 students in the database")
      .uponReceiving("a request for all students")
      .withRequest({
        method: "GET",
        path: "/api/v1/students",
        query: { page: "0", size: "20" },
        headers: { Authorization: "Bearer valid-token" },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": "application/json" },
        body: {
          content: [
            {
              id: 1,
              name: "Nguyễn Văn A",
              email: "nguyenvana@example.com",
              status: "ACTIVE",
            },
            {
              id: 2,
              name: "Trần Thị B",
              email: "tranthib@example.com",
              status: "ACTIVE",
            },
          ],
          totalElements: 2,
          totalPages: 1,
          size: 20,
          number: 0,
        },
      })
      .executeTest(async (mockServer) => {
        // Test frontend code against mock server
        const response = await fetch(
          `${mockServer.url}/api/v1/students?page=0&size=20`,
          {
            headers: { Authorization: "Bearer valid-token" },
          }
        );
        const data = await response.json();

        expect(response.status).toBe(200);
        expect(data.content).toHaveLength(2);
        expect(data.content[0].name).toBe("Nguyễn Văn A");
      });
  });
});
```

**Run Tests:**
```bash
npm test -- student-api.pact.test.ts
```

**Output:** Generates `pacts/kiteclass-frontend-kiteclass-core.json`

### Backend (Provider) Verification

**Dependency (Maven):**
```xml
<dependency>
    <groupId>au.com.dius.pact.provider</groupId>
    <artifactId>junit5spring</artifactId>
    <version>4.6.3</version>
    <scope>test</scope>
</dependency>
```

**Verification Test:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("kiteclass-core")
@PactFolder("../kiteclass-frontend/pacts")
public class StudentPactVerificationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("there are 2 students in the database")
    void setupStudentsState() {
        // Seed database with 2 students for this test
        studentRepository.save(new Student(1L, "Nguyễn Văn A", "nguyenvana@example.com"));
        studentRepository.save(new Student(2L, "Trần Thị B", "tranthib@example.com"));
    }
}
```

**Run Verification:**
```bash
./mvnw test -Dtest=StudentPactVerificationTest
```

**Result:**
- ✅ Pass: Backend fulfills frontend contract
- ❌ Fail: Backend response doesn't match frontend expectations (e.g., missing field, wrong type)

### CI/CD Integration

**Contract Broker (Pact Broker):**
- Centralized storage for Pact contracts
- Frontend publishes contracts after tests
- Backend verifies against published contracts

**Can-I-Deploy:**
```bash
# Check if frontend can deploy (backend verified its contract)
pact-broker can-i-deploy \
  --pacticipant kiteclass-frontend \
  --version $FRONTEND_VERSION \
  --to-environment production
```

---

## Documentation Generation

### Swagger UI (Interactive Docs)

**Access:** `http://localhost:8080/swagger-ui/index.html`

**Features:**
- Browse all endpoints
- Test API calls directly in browser
- View request/response schemas
- Download OpenAPI spec

**Customization:**
```yaml
# application.yml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
    displayRequestDuration: true
    defaultModelsExpandDepth: 2
  api-docs:
    path: /v3/api-docs
```

### ReDoc (Prettier Documentation)

**Alternative to Swagger UI** (more readable, less interactive)

**Dependency:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Access:** `http://localhost:8080/api-docs.html`

### Static HTML Export

**Use Case:** Share API docs without running server

**Tool:** [redoc-cli](https://github.com/Redocly/redoc)

**Generate:**
```bash
npx @redocly/cli build-docs documents/03-planning/api/core-api.json \
  -o docs/api/core-api.html
```

**Host on GitHub Pages:** Publish `docs/api/` folder for public API docs

---

## Best Practices

### 1. Keep OpenAPI Annotations Close to Code
- ✅ Annotate controllers directly (single source of truth)
- ❌ Don't maintain separate OpenAPI YAML files (gets out of sync)

### 2. Use Examples in Annotations
```java
@Schema(example = "nguyenvana@example.com")
private String email;
```
- Makes Swagger UI more useful
- Helps frontend developers understand expected formats

### 3. Document Error Responses
```java
@ApiResponse(responseCode = "400", description = "Invalid input",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "403", description = "Forbidden")
@ApiResponse(responseCode = "404", description = "Student not found")
```
- Frontend knows what errors to handle
- Prevents "500 Internal Server Error" surprises

### 4. Version Breaking Changes
- Don't modify existing endpoints in-place
- Create new versioned endpoint (`/api/v2/students`)
- Deprecate old endpoint with clear migration guide

### 5. Automate Type Generation
- CI/CD generates TypeScript types on every backend deploy
- Frontend catches API contract breaks at compile time
- No manual type definitions needed

---

## Summary

**API Contract Workflow:**

```
┌──────────────────────────────────────────────────────────┐
│ 1. BACKEND DEVELOPMENT                                   │
│    - Write Spring Boot controller                       │
│    - Add @Operation/@Schema annotations                 │
│    - Run service                                         │
└──────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ 2. OPENAPI GENERATION                                    │
│    - Springdoc auto-generates /v3/api-docs              │
│    - Export to core-api.json                            │
└──────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ 3. TYPESCRIPT TYPE GENERATION                            │
│    - openapi-typescript generates types                 │
│    - Commit to frontend repo                            │
└──────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ 4. FRONTEND DEVELOPMENT                                  │
│    - Import generated types                             │
│    - Use openapi-fetch for type-safe API calls          │
│    - TypeScript catches contract violations             │
└──────────────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│ 5. CONTRACT TESTING                                      │
│    - Frontend writes Pact tests                         │
│    - Backend verifies it fulfills contracts             │
│    - CI/CD enforces contract compliance                 │
└──────────────────────────────────────────────────────────┘
```

**Result:** Type-safe, contract-tested APIs with zero manual type definitions

---

## Related Documentation

- [KiteHub Implementation Plan](../implementation/kitehub-implementation-plan.md)
- [Core Service Implementation](../implementation/core-service-implementation.md)
- [Frontend Plan](../implementation/frontend-plan.md)

---

**Last Updated:** 2026-03-09
**Status:** Design complete, ready for implementation
