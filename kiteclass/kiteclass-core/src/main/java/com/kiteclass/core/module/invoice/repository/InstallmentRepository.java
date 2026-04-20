package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Installment} entity — enables indexed primary-key lookup
 * of a single installment without loading every {@link com.kiteclass.core.module.invoice.entity.InstallmentPlan}
 * row in the tenant.
 *
 * <p>Created for GAP-128 fix: {@code recordInstallmentPayment(Long installmentId, ...)}
 * previously did {@code installmentPlanRepository.findAll().stream().filter(...)} which
 * scaled O(plans × installments). The replacement uses {@code findById} (PK lookup) and
 * navigates to {@link Installment#getPlan()} via the existing
 * {@code @ManyToOne} on {@code installments.plan_id} (indexed by V12 migration).
 *
 * @author KiteClass Team
 * @since 4.5.0 (GAP-128 fix)
 */
@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
}
