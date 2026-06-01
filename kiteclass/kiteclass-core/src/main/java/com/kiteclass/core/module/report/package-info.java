/**
 * Report Module — Owner-dashboard analytics aggregations (GAP-775 Mảng B11).
 *
 * <h2>Module Overview</h2>
 * <p>Provides tenant-wide read-only analytics for the Owner dashboard: monthly
 * revenue (from completed payments) and monthly attendance present-rate. Distinct
 * from the {@code report-card} domain, which generates per-student K-12 PDF report
 * cards.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/v1/reports/revenue?months=N} — monthly revenue series + total</li>
 *   <li>{@code GET /api/v1/reports/attendance?months=N} — monthly present-rate series + overall</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <p>Both endpoints are Owner/admin-only via {@code @PreAuthorize("hasRole('ADMIN')")}
 * (OWASP A01 role gate — tenant-wide financial/operational aggregation). Tenant
 * isolation is enforced by the Hibernate {@code tenantFilter} on the underlying
 * {@code payments} / {@code attendance} tables.
 *
 * <h2>Localization</h2>
 * <p>Amounts are raw VND (no minor unit); the FE renders {@code 1.500.000đ} and
 * percentages {@code 92,5%} per {@code vn-localization-audit-checklist.md} §1.
 *
 * @author KiteClass Team
 * @since 2026-06-02 (GAP-775)
 */
package com.kiteclass.core.module.report;
