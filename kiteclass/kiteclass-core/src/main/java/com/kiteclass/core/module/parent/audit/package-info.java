/**
 * Per-read audit log skeleton for the parent portal (GAP-321b Phase 1B —
 * Wave 18b2 Bucket C).
 *
 * <p>Every parent-side facet endpoint (attendance / fees / conduct /
 * notifications, plus existing transcript) writes one row here on a
 * successful 200 response. PDPL Decree 13/2023 Art 16 + Luật Trẻ em 2016
 * Đ.21 require the operator to be able to demonstrate who accessed which
 * child's data when. The append-only row is the v1 minimum; richer fields
 * (IP, user agent, request id) and the 5-year retention sweeper are
 * deferred to GAP-321b.4 follow-up.
 */
package com.kiteclass.core.module.parent.audit;
