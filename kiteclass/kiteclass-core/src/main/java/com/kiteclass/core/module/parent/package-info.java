/**
 * Parent portal module — identity, invitation, and parent-child linking.
 *
 * <p>Wave 2 MVP (GAP-052a) delivers:
 * <ul>
 *   <li>Parent entity with soft delete + multi-tenant isolation via {@link
 *       com.kiteclass.core.common.entity.BaseEntity}.</li>
 *   <li>ParentStudentLink many-to-many join (a parent can have many children; a
 *       child can have multiple parents, e.g., father + mother).</li>
 *   <li>ParentInvitation token-based onboarding (24-hour TTL, public redemption).</li>
 *   <li>Self-service endpoints ({@code GET /api/v1/parent/me} and
 *       {@code GET /api/v1/parent/me/children}) with tenant + link enforcement.</li>
 *   <li>Internal endpoints consumed by {@code kiteclass-gateway} during login to
 *       populate the {@code linked_student_ids} JWT claim.</li>
 * </ul>
 *
 * <p>Wave 5 will layer on attendance / grade / invoice widgets, messaging, and
 * push notifications.
 *
 * @since 2.14.0
 */
package com.kiteclass.core.module.parent;
