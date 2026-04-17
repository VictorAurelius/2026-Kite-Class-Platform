/**
 * User preferences API module.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { apiClient } from '@/lib/api-client';
import type { UserPreferences, UpdatePreferencesRequest } from '@/types/preferences';

const BASE_URL = '/api/v1/users/me/preferences';

export const preferencesApi = {
  /**
   * Get current user preferences
   */
  get: async (): Promise<UserPreferences> => {
    const { data } = await apiClient.get<UserPreferences>(BASE_URL);
    return data;
  },

  /**
   * Update user preferences (partial update)
   */
  update: async (request: UpdatePreferencesRequest): Promise<UserPreferences> => {
    const { data } = await apiClient.patch<UserPreferences>(BASE_URL, request);
    return data;
  },
};
