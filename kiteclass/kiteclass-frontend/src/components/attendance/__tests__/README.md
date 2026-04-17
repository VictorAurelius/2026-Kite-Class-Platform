# Attendance Component Tests

Unit tests for attendance module components.

## Test Files

### 1. `attendance-form-row.test.tsx`
Tests for the AttendanceFormRow component.

**Coverage:**
- ✅ Renders student name correctly
- ✅ Displays current status in selector
- ✅ Calls onStatusChange callback
- ✅ Calls onNotesChange callback
- ✅ Displays notes value
- ✅ Shows all 5 attendance status options
- ✅ Has correct data attributes

### 2. `attendance-stats-cards.test.tsx`
Tests for the AttendanceStatsCards component.

**Coverage:**
- ✅ Renders all stat cards
- ✅ Displays correct values
- ✅ Shows/hides makeup card based on prop
- ✅ Handles zero values
- ✅ Applies correct color classes
- ✅ Renders correct number of cards

### 3. `attendance-calendar.test.tsx`
Tests for the AttendanceCalendar component.

**Coverage:**
- ✅ Renders calendar grid with weekday headers
- ✅ Navigation buttons (Previous, Next, Today)
- ✅ Displays current month name
- ✅ Calls onDateClick callback
- ✅ Shows attendance counts on dates
- ✅ Shows present/absent indicators
- ✅ Displays legend with color codes
- ✅ Month navigation functionality

## Running Tests

```bash
# Run all tests
npm test

# Run with coverage
npm test -- --coverage

# Run specific test file
npm test attendance-form-row.test.tsx

# Run in watch mode
npm test -- --watch
```

## Test Coverage Goals

- **Statements**: ≥80%
- **Branches**: ≥75%
- **Functions**: ≥80%
- **Lines**: ≥80%

## Writing New Tests

When adding new components, follow this pattern:

```tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { YourComponent } from '../your-component';

describe('YourComponent', () => {
  it('should render correctly', () => {
    render(<YourComponent />);
    expect(screen.getByText('Expected Text')).toBeInTheDocument();
  });

  it('should handle user interaction', () => {
    const mockCallback = jest.fn();
    render(<YourComponent onClick={mockCallback} />);

    const button = screen.getByRole('button');
    fireEvent.click(button);

    expect(mockCallback).toHaveBeenCalledTimes(1);
  });
});
```

## Testing Best Practices

1. **Test behavior, not implementation**
   - Focus on what the user sees and does
   - Avoid testing internal state or methods

2. **Use semantic queries**
   - Prefer `getByRole`, `getByLabelText`, `getByText`
   - Avoid `getByTestId` unless necessary

3. **Mock external dependencies**
   - Mock API calls, hooks, and third-party libraries
   - Keep tests isolated and fast

4. **Write descriptive test names**
   - Use "should" or "renders" for clarity
   - Be specific about what is being tested

5. **Clean up after tests**
   - Use `beforeEach` and `afterEach` for setup/cleanup
   - Clear mocks between tests

## Integration Tests

For integration tests that involve multiple components and hooks, see:
- `src/__tests__/integration/attendance-flow.test.tsx` (TODO)

## E2E Tests

End-to-end tests for the complete attendance workflow can be found in:
- `cypress/e2e/attendance.cy.ts` (TODO)

## CI/CD

Tests run automatically on:
- Every push to feature branches
- Every pull request
- Before deployment to staging/production

Minimum coverage requirements must be met for PR approval.
