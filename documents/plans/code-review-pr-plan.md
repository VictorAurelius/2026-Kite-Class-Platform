# Code Review PR Plan (Existing Code Only)

**Version:** 1.0
**Date:** 2026-01-30
**Purpose:** Review và fix code ĐÃ ĐƯỢC IMPLEMENT hiện tại

**⚠️ LƯU Ý:** Plan này chỉ cover code HIỆN CÓ, KHÔNG cover features chưa implement

---

## Executive Summary

### Current Code Status

**Backend:**
- ❌ **NO CODE** - Backend chưa được implement
- Không có entities, services, repositories, controllers

**Frontend:**
- ✅ **BASIC SETUP** - Next.js 14 + shadcn/ui
- Có: Basic structure, UI components (shadcn/ui library)
- Không có: Business logic components, API integration, state management

**Testing:**
- ❌ **NO TESTS** - Không có unit tests, integration tests, E2E tests

**CI/CD:**
- ❌ **NO CI/CD** - Không có GitHub Actions workflows

---

## Scope of Review

### What We CAN Review (Code Exists)

1. ✅ **Frontend Structure** (Next.js setup, folder organization)
2. ✅ **UI Components** (shadcn/ui components quality)
3. ⚠️ **Testing Setup** (CẦN ADD - chưa có)
4. ⚠️ **Linting/Formatting** (CẦN VERIFY - có thể chưa setup đủ)
5. ⚠️ **CI/CD** (CẦN ADD - chưa có)

### What We CANNOT Review (No Code)

- ❌ Backend code (chưa có)
- ❌ Multi-tenant security (chưa có backend)
- ❌ Payment system (chưa implement)
- ❌ Trial system (chưa implement)
- ❌ Feature detection (chưa implement)
- ❌ AI Branding (chưa implement)

---

## PR Plan for Existing Code

**Total PRs:** 6 PRs (chỉ review/fix code hiện có)
**Timeline:** 1 week
**Team:** 1 Frontend Developer

---

### PR 1: Frontend Structure & Code Quality Review

**Branch:** `refactor/frontend-structure-review`
**Assignee:** Frontend Developer
**Effort:** 0.5 day
**Priority:** 🟡 MEDIUM

#### Scope

Review và cải thiện frontend structure hiện tại:
- Next.js 14 App Router setup
- Folder structure organization
- TypeScript configuration
- Tailwind CSS configuration

#### Files to Review

```
kiteclass-frontend/
├── src/
│   ├── app/
│   │   ├── layout.tsx          # Root layout
│   │   └── page.tsx            # Home page (basic)
│   └── components/
│       └── ui/                 # shadcn/ui components (22 files)
├── tailwind.config.ts
├── tsconfig.json
├── next.config.js
└── package.json
```

#### Review Checklist

**Project Structure:**
- [ ] Next.js App Router setup correct
- [ ] Folder structure follows best practices
- [ ] TypeScript strict mode enabled
- [ ] ESLint configured properly
- [ ] Prettier configured properly

**Code Quality:**
- [ ] No TypeScript errors
- [ ] No ESLint warnings
- [ ] Code follows Next.js conventions
- [ ] Tailwind CSS classes optimized

**Configuration:**
- [ ] `tsconfig.json` has strict settings
- [ ] `next.config.js` has proper settings (images, env vars)
- [ ] `tailwind.config.ts` follows design system

#### Expected Changes

- Fix any linting issues
- Add missing ESLint rules
- Optimize Tailwind config
- Update `next.config.js` for production

#### Success Criteria

- [ ] No TypeScript errors
- [ ] No ESLint warnings
- [ ] Code quality score ≥ A
- [ ] Structure follows Next.js best practices

---

### PR 2: Add Testing Infrastructure

**Branch:** `test/add-frontend-testing-setup`
**Assignee:** Frontend Developer
**Effort:** 1 day
**Priority:** 🔴 HIGH

#### Scope

Setup testing infrastructure cho frontend (hiện không có tests)

#### What to Add

**Testing Tools:**
```json
{
  "devDependencies": {
    "vitest": "^1.0.0",
    "@testing-library/react": "^14.0.0",
    "@testing-library/user-event": "^14.0.0",
    "@testing-library/jest-dom": "^6.0.0",
    "jsdom": "^23.0.0",
    "@vitejs/plugin-react": "^4.0.0"
  }
}
```

**Configuration Files:**

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      statements: 80,
      branches: 75,
      functions: 80,
      lines: 80,
    },
  },
});
```

```typescript
// src/test/setup.ts
import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
});
```

**Sample Tests for UI Components:**

```typescript
// src/components/ui/button.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from './button';

describe('Button', () => {
  it('should render button with text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('should call onClick when clicked', async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();

    render(<Button onClick={onClick}>Click me</Button>);

    await user.click(screen.getByText('Click me'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('should be disabled when disabled prop is true', () => {
    render(<Button disabled>Click me</Button>);
    expect(screen.getByText('Click me')).toBeDisabled();
  });
});
```

#### Checklist

- [ ] Vitest installed and configured
- [ ] React Testing Library installed
- [ ] Test setup file created
- [ ] Sample tests added for ≥3 UI components
- [ ] Coverage reporting configured
- [ ] `npm run test` script works
- [ ] `npm run test:coverage` script works

#### Success Criteria

- [ ] Testing infrastructure complete
- [ ] Sample tests passing
- [ ] Coverage report generated
- [ ] Documentation updated

---

### PR 3: Add Component Tests for UI Library

**Branch:** `test/ui-components-tests`
**Assignee:** Frontend Developer
**Effort:** 1.5 days
**Priority:** 🟡 MEDIUM
**Depends On:** PR 2

#### Scope

Write tests cho tất cả shadcn/ui components hiện có

#### Components to Test (22 components)

```
src/components/ui/
├── alert.tsx
├── avatar.tsx
├── badge.tsx
├── button.tsx
├── calendar.tsx
├── card.tsx
├── checkbox.tsx
├── dialog.tsx
├── dropdown-menu.tsx
├── form.tsx
├── input.tsx
├── label.tsx
├── popover.tsx
├── select.tsx
├── separator.tsx
├── sheet.tsx
├── skeleton.tsx
├── table.tsx
├── tabs.tsx
├── toast.tsx
├── toaster.tsx
└── tooltip.tsx
```

#### Testing Requirements

**Per Component:**
- [ ] Render test (component displays correctly)
- [ ] Props test (all props work as expected)
- [ ] Interaction test (onClick, onChange, etc.)
- [ ] Accessibility test (ARIA attributes, keyboard navigation)
- [ ] Edge cases test (empty state, error state, disabled state)

**Example:**

```typescript
// src/components/ui/input.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Input } from './input';

describe('Input', () => {
  it('should render input with placeholder', () => {
    render(<Input placeholder="Enter text" />);
    expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
  });

  it('should update value on user input', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(<Input onChange={onChange} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'Hello');

    expect(input).toHaveValue('Hello');
    expect(onChange).toHaveBeenCalled();
  });

  it('should be disabled when disabled prop is true', () => {
    render(<Input disabled />);
    expect(screen.getByRole('textbox')).toBeDisabled();
  });

  it('should have proper ARIA attributes', () => {
    render(<Input aria-label="Email input" />);
    expect(screen.getByRole('textbox')).toHaveAccessibleName('Email input');
  });
});
```

#### Test Coverage Goal

- **Target:** ≥85% coverage for UI components
- **Minimum:** 3 tests per component
- **Total:** ~70-80 tests expected

#### Success Criteria

- [ ] All 22 components have tests
- [ ] Coverage ≥85% for `src/components/ui/`
- [ ] All tests passing
- [ ] Accessibility tests included

---

### PR 4: Add Linting & Formatting Enforcement

**Branch:** `chore/eslint-prettier-strict`
**Assignee:** Frontend Developer
**Effort:** 0.5 day
**Priority:** 🟡 MEDIUM

#### Scope

Enforce strict linting và formatting rules

#### ESLint Configuration

```javascript
// .eslintrc.json
{
  "extends": [
    "next/core-web-vitals",
    "plugin:@typescript-eslint/recommended",
    "plugin:@typescript-eslint/recommended-requiring-type-checking",
    "prettier"
  ],
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "project": "./tsconfig.json"
  },
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/no-explicit-any": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "react/prop-types": "off",
    "react/react-in-jsx-scope": "off"
  }
}
```

#### Prettier Configuration

```javascript
// .prettierrc.js
module.exports = {
  semi: true,
  trailingComma: 'es5',
  singleQuote: true,
  printWidth: 100,
  tabWidth: 2,
  useTabs: false,
  arrowParens: 'always',
  endOfLine: 'lf',
};
```

#### Checklist

- [ ] ESLint strict rules configured
- [ ] Prettier configured
- [ ] All existing code passes linting
- [ ] Format all files with Prettier
- [ ] Add `lint` and `format` scripts to package.json
- [ ] Add pre-commit hook (Husky + lint-staged)

#### Success Criteria

- [ ] No ESLint errors
- [ ] No ESLint warnings (or explicitly ignored)
- [ ] All files formatted with Prettier
- [ ] Pre-commit hook prevents bad code

---

### PR 5: Setup CI/CD for Frontend

**Branch:** `cicd/frontend-pipeline`
**Assignee:** Frontend Developer
**Effort:** 1 day
**Priority:** 🔴 HIGH
**Depends On:** PR 2, PR 4

#### Scope

Setup GitHub Actions workflows cho frontend

#### Workflows to Create

**1. Frontend Tests Workflow**

```yaml
# .github/workflows/frontend-tests.yml
name: Frontend Tests

on:
  pull_request:
    paths:
      - 'kiteclass/kiteclass-frontend/**'
  push:
    branches: [main, develop]
    paths:
      - 'kiteclass/kiteclass-frontend/**'

defaults:
  run:
    working-directory: kiteclass/kiteclass-frontend

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: kiteclass/kiteclass-frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Run linter
        run: npm run lint

      - name: Run tests
        run: npm run test:coverage

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./coverage/lcov.info
          flags: frontend
          token: ${{ secrets.CODECOV_TOKEN }}
```

**2. Frontend Build Workflow**

```yaml
# .github/workflows/frontend-build.yml
name: Frontend Build

on:
  pull_request:
    paths:
      - 'kiteclass/kiteclass-frontend/**'

defaults:
  run:
    working-directory: kiteclass/kiteclass-frontend

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: kiteclass/kiteclass-frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Build
        run: npm run build

      - name: Check build size
        run: |
          du -sh .next/static/chunks
          # Fail if bundle > 500KB
```

**3. Code Quality Workflow**

```yaml
# .github/workflows/code-quality.yml
name: Code Quality

on: [pull_request]

defaults:
  run:
    working-directory: kiteclass/kiteclass-frontend

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm ci

      - name: Check TypeScript
        run: npx tsc --noEmit

      - name: Check formatting
        run: npx prettier --check .
```

#### Branch Protection Rules

```yaml
Required status checks:
  - Frontend Tests
  - Frontend Build
  - Code Quality

Required reviews: 1

Require branches to be up to date: true
```

#### Success Criteria

- [ ] All 3 workflows created and working
- [ ] Branch protection configured
- [ ] Coverage reporting to Codecov
- [ ] Quality gates enforced

---

### PR 6: Documentation & Developer Guide

**Branch:** `docs/frontend-developer-guide`
**Assignee:** Frontend Developer
**Effort:** 0.5 day
**Priority:** 🟢 LOW

#### Scope

Document frontend code structure và developer setup

#### Documents to Create

**1. Frontend README**

```markdown
# KiteClass Frontend

## Tech Stack
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- shadcn/ui

## Getting Started
\`\`\`bash
cd kiteclass/kiteclass-frontend
npm install
npm run dev
\`\`\`

## Available Scripts
- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run lint` - Run ESLint
- `npm run format` - Format with Prettier
- `npm run test` - Run tests
- `npm run test:coverage` - Run tests with coverage

## Project Structure
\`\`\`
src/
├── app/              # Next.js App Router pages
├── components/
│   └── ui/          # shadcn/ui components
├── lib/             # Utility functions
└── test/            # Test utilities
\`\`\`

## Code Quality
- TypeScript strict mode
- ESLint with strict rules
- Prettier for formatting
- Vitest + React Testing Library for testing
- 85%+ test coverage required
```

**2. Contributing Guide**

```markdown
# Contributing to Frontend

## Before You Start
1. Read the code quality standards
2. Ensure tests are passing locally
3. Follow the PR template

## Development Workflow
1. Create feature branch from `develop`
2. Make changes with tests
3. Run `npm run lint && npm run test`
4. Create PR with descriptive title
5. Wait for CI/CD checks to pass
6. Request review

## Testing Requirements
- All new components must have tests
- Coverage must be ≥85%
- Tests must include accessibility checks
```

#### Success Criteria

- [ ] README.md complete
- [ ] CONTRIBUTING.md complete
- [ ] Code examples documented
- [ ] Developer setup guide clear

---

## Summary

### Total PRs: 6

| PR | Description | Effort | Priority |
|----|-------------|--------|----------|
| PR 1 | Frontend Structure Review | 0.5 day | 🟡 MEDIUM |
| PR 2 | Add Testing Infrastructure | 1 day | 🔴 HIGH |
| PR 3 | Add Component Tests | 1.5 days | 🟡 MEDIUM |
| PR 4 | Linting & Formatting | 0.5 day | 🟡 MEDIUM |
| PR 5 | Setup CI/CD | 1 day | 🔴 HIGH |
| PR 6 | Documentation | 0.5 day | 🟢 LOW |

**Total Effort:** 5 days (1 Frontend Developer)
**Timeline:** 1 week

---

### Merge Order

```
PR 1: Frontend Structure Review
  ↓
PR 2: Add Testing Infrastructure
  ↓
PR 4: Linting & Formatting
  ↓ ↓
PR 3: Component Tests     PR 5: CI/CD Setup
  ↓                         ↓
  └────────→ PR 6: Documentation
```

---

## Success Criteria

**After All PRs Merged:**

- ✅ Frontend structure reviewed and optimized
- ✅ Testing infrastructure complete
- ✅ All UI components have tests (≥85% coverage)
- ✅ Linting and formatting enforced
- ✅ CI/CD pipeline operational
- ✅ Documentation complete

**NOT Included (No Code to Review):**

- ❌ Backend code review (no backend code)
- ❌ Multi-tenant security review (no backend)
- ❌ Payment system review (not implemented)
- ❌ Feature detection review (not implemented)
- ❌ E2E tests (no business logic to test)

---

## Next Steps After This Plan

**After completing these 6 PRs:**

1. ✅ Frontend code quality established
2. ⚠️ **THEN** implement business logic (Payment, Trial, Feature Detection)
3. ⚠️ **THEN** use `quality-implementation-full-plan.md` for full implementation

**Reference:**
- Current plan: `code-review-pr-plan.md` (6 PRs, review existing code)
- Full implementation: `quality-implementation-full-plan.md` (18 PRs, implement features)

---

**Plan Version:** 1.0 (Existing Code Only)
**Created:** 2026-01-30
**Scope:** Frontend only (no backend code exists)
**Total Effort:** 5 days (1 developer)
