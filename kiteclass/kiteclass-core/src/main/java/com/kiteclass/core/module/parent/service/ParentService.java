package com.kiteclass.core.module.parent.service;

import com.kiteclass.core.module.parent.dto.ChildSummaryResponse;
import com.kiteclass.core.module.parent.dto.ParentInternalResponse;
import com.kiteclass.core.module.parent.dto.ParentResponse;

import java.util.List;

/**
 * Read-side operations on the Parent aggregate for MVP Wave 2.
 *
 * <p>Scope is deliberately narrow — identity + children listing. Mutating flows
 * (invite, redeem, revoke) live on {@link ParentInvitationService}; dashboard
 * widgets (attendance, grades, invoices) arrive in Wave 5.
 *
 * @since 2.14.0
 */
public interface ParentService {

    /**
     * Loads the parent by primary key, applying the tenant filter + soft-delete
     * filter automatically.
     *
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if
     *         the parent does not exist in the current tenant.
     */
    ParentResponse getParentById(Long parentId);

    /**
     * Lists the parent's linked children, flattened into dashboard-friendly
     * summaries.
     *
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if
     *         the parent does not exist in the current tenant.
     */
    List<ChildSummaryResponse> getChildrenOfParent(Long parentId);

    /**
     * Internal projection used by the Gateway during login / profile refresh.
     * Includes the flattened {@code linkedStudentIds} for the JWT claim.
     *
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if
     *         the parent does not exist in the current tenant.
     */
    ParentInternalResponse getInternalParentView(Long parentId);
}
