# Test Directory

Test utilities, mocks, and setup files.

## Structure

```
src/test/
├── setup.ts           # Vitest setup (imported automatically)
├── mocks/             # MSW handlers and mock data
│   ├── handlers.ts
│   └── server.ts
└── utils.tsx          # Test utilities (custom render, etc.)
```

## Test Files Location

Test files should be located in `src/__tests__/` mirroring the source structure:

```
src/__tests__/
├── components/        # Component tests
│   ├── shared/
│   ├── forms/
│   └── layout/
├── hooks/            # Hook tests
├── lib/              # Utility tests
└── stores/           # Store tests
```

## Running Tests

```bash
pnpm test              # Run all tests
pnpm test:watch        # Watch mode
pnpm test:coverage     # With coverage report
pnpm test:ui           # Open Vitest UI
```
