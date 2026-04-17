/**
 * MSW Server Setup for Node.js testing environment
 *
 * @author KiteClass Team
 * @since 3.8.0
 */

import { setupServer } from 'msw/node';
import { handlers } from './handlers';

// Setup requests interception using the given handlers
export const server = setupServer(...handlers);
