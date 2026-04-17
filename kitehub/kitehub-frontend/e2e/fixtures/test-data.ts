/**
 * Test data fixtures for E2E tests.
 *
 * @since PR 5.10
 */

import { generateTestSubdomain, generateTestEmail } from '../utils/test-helpers';

/**
 * Generate fresh registration data for each test.
 */
export function createRegistrationData() {
  return {
    organizationName: 'Test Organization',
    subdomain: generateTestSubdomain(),
    email: generateTestEmail(),
    password: 'TestPassword123!',
  };
}

/**
 * Test user for login tests (requires pre-existing account).
 * Note: This user should exist in the test database.
 */
export const existingTestUser = {
  email: 'e2e-test@example.com',
  password: 'TestPassword123!',
};

/**
 * Invalid credentials for negative tests.
 */
export const invalidCredentials = {
  email: 'invalid@example.com',
  password: 'wrongpassword',
};
