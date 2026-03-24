# Technology Stack Reference

Complete technology stack for the Kite Class Platform with rationale for each choice.

## Backend

| Technology | Version | Purpose | Rationale |
|-----------|---------|---------|-----------|
| Java | 21 (LTS) | Primary language | Long-term support, virtual threads, pattern matching, strong typing |
| Spring Boot | 3.5.x | Application framework | Industry standard for microservices, auto-configuration, extensive ecosystem |
| Spring Cloud | 2024.x | Microservices infrastructure | Service discovery, config management, circuit breakers |
| Spring Security | 6.x | Authentication & authorization | OAuth2/JWT support, method-level security, CSRF protection |
| Spring Data JPA | 3.x | Data access layer | Repository pattern, query derivation, audit support |
| Hibernate | 6.x | ORM | Mature JPA implementation, lazy loading, caching |
| Lombok | 1.18.x | Boilerplate reduction | Reduces getter/setter/constructor code by ~60% |
| MapStruct | 1.6.x | Object mapping | Compile-time DTO mapping, type-safe, zero-runtime overhead |
| SpringDoc OpenAPI | 2.x | API documentation | Auto-generated Swagger/OpenAPI specs from annotations |

## Frontend

| Technology | Version | Purpose | Rationale |
|-----------|---------|---------|-----------|
| Next.js | 15 | React framework | App Router, SSR/SSG, API routes, image optimization |
| React | 19 | UI library | Component model, hooks, concurrent features |
| TypeScript | 5.7.x | Type-safe JavaScript | Compile-time error detection, IDE support, refactoring safety |
| Tailwind CSS | 3.4.x | Utility-first CSS | Rapid styling, consistent design, small bundle size |
| Shadcn UI | Latest | Component library | Accessible, customizable, copy-paste components (not npm dependency) |
| TanStack Query | 5.x | Server state management | Caching, background refetch, optimistic updates |
| TanStack Table | 8.x | Table management | Headless, sorting, filtering, pagination |
| Zustand | 5.x | Client state management | Lightweight, simple API, no boilerplate vs Redux |
| React Hook Form | 7.x | Form handling | Uncontrolled components, minimal re-renders, Zod integration |
| Zod | 3.x | Schema validation | TypeScript-first, runtime validation, form integration |
| Lucide React | 0.468 | Icons | Tree-shakeable, consistent style, large icon set |

## Database

| Technology | Version | Purpose | Rationale |
|-----------|---------|---------|-----------|
| PostgreSQL | 16 | Primary database | ACID compliance, JSON support, full-text search, mature ecosystem |
| Redis | 7 | Caching & sessions | In-memory speed, pub/sub, TTL support for rate limiting |
| Flyway | 10.x | Database migrations | Version-controlled schema changes, repeatable migrations |

## DevOps & Infrastructure

| Technology | Version | Purpose | Rationale |
|-----------|---------|---------|-----------|
| Docker | 24.x | Containerization | Consistent environments, isolation, reproducible builds |
| Docker Compose | 2.x | Local orchestration | Multi-service development, service dependencies, volumes |
| GitHub Actions | N/A | CI/CD pipeline | Native GitHub integration, matrix builds, artifact management |
| Terraform | 1.x | Infrastructure as code | Declarative cloud provisioning, state management, plan/apply workflow |
| Oracle Cloud | N/A | Cloud provider | Always-free tier (ARM instances), cost-effective for graduation project |

## AI Integration

| Technology | Version | Purpose | Rationale |
|-----------|---------|---------|-----------|
| Ollama | Latest | Local LLM inference | Privacy (no data leaves server), free, multiple model support |

## Testing

| Technology | Version | Purpose | Rationale |
|-----------|---------|---------|-----------|
| JUnit 5 | 5.11.x | Java unit testing | Parameterized tests, extensions, assertions |
| Mockito | 5.x | Java mocking | Clean API, annotation-based, argument matchers |
| Testcontainers | 1.20.x | Integration testing | Real PostgreSQL/Redis in Docker for tests |
| Vitest | 4.x | Frontend unit testing | Vite-native, fast HMR, Jest-compatible API |
| Testing Library | 16.x | Component testing | User-centric queries, accessibility-focused |
| Playwright | 1.58.x | E2E testing | Cross-browser, auto-wait, trace viewer |
| MSW | 2.x | API mocking (frontend) | Service worker interception, realistic network mocking |

## Architecture Decisions

### Why Microservices?
- **Domain isolation**: Each service owns its bounded context (subscription, billing, email, etc.)
- **Independent scaling**: Services can scale based on individual load patterns
- **Technology flexibility**: Services can evolve independently
- **Team scalability**: Clear ownership boundaries

### Why Next.js over plain React?
- **SSR/SSG**: SEO for public pages (landing, blog)
- **File-based routing**: Convention over configuration
- **API routes**: Backend-for-frontend pattern
- **Image optimization**: Automatic format conversion and lazy loading

### Why PostgreSQL over MySQL?
- **JSON columns**: Flexible metadata storage without schema changes
- **Full-text search**: Built-in search without Elasticsearch for simple cases
- **LISTEN/NOTIFY**: Real-time event notifications
- **Advanced indexing**: Partial indexes, expression indexes

### Why Zustand over Redux?
- **Simplicity**: Minimal boilerplate, no actions/reducers/dispatchers
- **Bundle size**: ~1KB vs ~7KB for Redux Toolkit
- **TypeScript**: First-class support without extra type definitions
- **Compatibility**: Works seamlessly with React 19 concurrent features
