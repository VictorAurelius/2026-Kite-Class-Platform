/**
 * MSW handlers for Beta Status endpoints (Wave 78 Bucket 0 Foundation).
 *
 * Schema: `documents/01-business/kitehub/beta-status/api-contract.md`
 *
 * Cross-layer foundation per `.claude/rules/contract-first-for-cross-layer.md`:
 * Bucket B (GAP-539) FE `/beta-status` route component will consume this
 * handler in unit tests before BE module lands.
 *
 * Endpoints covered:
 *   - GET /api/v1/beta-status (public markdown content, 5-min cache)
 *
 * Per-test overrides via `server.use(http.X(...))` in individual specs.
 *
 * @author KiteHub Team
 * @since Wave 78 Bucket 0
 */

import { http, HttpResponse } from 'msw';
import type { HttpHandler } from 'msw';

const DEFAULT_MARKDOWN = `# Trạng thái Beta KiteHub

## Cập nhật mới nhất

**2026-05-14:** Hệ thống hoạt động bình thường. Chào mừng cohort Phase 1 BETA!

## Kế hoạch bảo trì

Không có kế hoạch bảo trì nào trong 7 ngày tới.

## Cách báo lỗi

Gặp vấn đề? Vui lòng dùng nút "Feedback" hoặc gửi ticket qua "Liên hệ hỗ trợ" trong footer.
`;

export const betaStatusHandlers: HttpHandler[] = [
  // ---------------------------------------------------------------
  // GET /api/v1/beta-status (public, cached 5 min)
  // ---------------------------------------------------------------
  http.get('*/api/v1/beta-status', () => {
    return HttpResponse.json(
      {
        version: '2026-05-14-v1',
        lastUpdatedAt: '2026-05-14T07:00:00Z',
        contentMarkdown: DEFAULT_MARKDOWN,
        currentStatus: 'OPERATIONAL',
        knownIssues: [],
      },
      {
        status: 200,
        headers: {
          'Cache-Control': 'public, max-age=300',
        },
      }
    );
  }),
];
