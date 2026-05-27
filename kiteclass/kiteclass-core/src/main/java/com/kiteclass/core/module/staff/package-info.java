/**
 * Staff onboarding module — token-based invitations for STAFF/TEACHER/MANAGER
 * role provisioning at tenant scope.
 *
 * <p>Wave meta-6 Bucket A MVP (GAP-772 + GAP-773 paired) delivers:
 * <ul>
 *   <li>{@link com.kiteclass.core.module.staff.entity.StaffInvitation} entity
 *       with soft delete + multi-tenant isolation via
 *       {@link com.kiteclass.core.common.entity.BaseEntity}.</li>
 *   <li>Owner-side issue + list + cancel endpoints (role-guarded ADMIN/OWNER).</li>
 *   <li>Public acceptance endpoint consumed by Gateway during
 *       {@code POST /api/v1/auth/register-staff/{token}}.</li>
 *   <li>V71 Flyway migration creating {@code staff_invitations} table.</li>
 * </ul>
 *
 * <p>Out of scope for MVP (deferred to follow-up waves):
 * <ul>
 *   <li>Email template with staff persona tone (GAP-659 pair).</li>
 *   <li>FE accept-invite route {@code /staff/accept-invite/[token]} (GAP-773).</li>
 *   <li>Owner-side invitations list page (FE).</li>
 *   <li>Integration tests with Testcontainers + RST→E2E paired specs.</li>
 *   <li>Audit logging via {@code Propagation.REQUIRES_NEW} per
 *       {@code .claude/rules/audit-service-isolation.md}.</li>
 *   <li>Scheduled sweeper transitioning PENDING → EXPIRED after TTL.</li>
 * </ul>
 *
 * <p>Pattern mirrors {@link com.kiteclass.core.module.parent} but without
 * student linkage — staff identity is tenant-scoped only; role binding
 * provisioned at Gateway.
 *
 * @since 2026-05-27
 */
package com.kiteclass.core.module.staff;
