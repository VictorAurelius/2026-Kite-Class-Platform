# API Contract Tests

## What these tests verify

Contract tests verify that API **response schemas** remain stable — field names, types, HTTP status codes, and error formats. They do NOT test business logic (that belongs in unit/integration tests).

A failing contract test means a **breaking change** was introduced that would affect API consumers (KiteHub Frontend, KiteClass cross-service calls).

## What they check

- **Field presence**: required fields exist in JSON responses
- **Field types**: strings are strings, numbers are numbers, booleans are booleans
- **HTTP status codes**: correct codes for success, not-found, validation errors
- **Error format**: RFC 7807 ProblemDetail structure (`title`, `status`, `detail`)
- **Collection types**: arrays return arrays, maps return maps

## When to add a new contract test

- Any new endpoint added to a `*Controller.java`
- Any change to a `*Response.java` DTO (field added/removed/renamed)
- Any change to error handling in `GlobalExceptionHandler`

## How to run

```bash
# Run all contract tests
mvn test -pl kitehub/kitehub-subscription -Dtest="*ContractTest"

# Run specific controller contract
mvn test -pl kitehub/kitehub-subscription -Dtest="InstanceApiContractTest"
```

## Design decisions

- **`@WebMvcTest`** instead of `@SpringBootTest` — lightweight, no DB/queue/context needed
- **`@MockitoBean`** for all services — contract tests never touch real implementations
- **Schema assertions only** — use `jsonPath("$.field").isString()` not `.value("exact")`
- **One test class per controller** — mirrors the controller structure
