package com.kiteclass.core.module.payroll.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.payroll.dto.PayrollConfigResponse;
import com.kiteclass.core.module.payroll.dto.PayrollPeriodResponse;
import com.kiteclass.core.module.payroll.entity.PayrollPeriod;
import com.kiteclass.core.module.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller for admin payroll views — Phase 1 (read-only).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET {@code /api/v1/admin/payroll/configs} — list teacher payroll configs</li>
 *   <li>GET {@code /api/v1/admin/payroll/periods} — list payroll periods (filterable)</li>
 *   <li>GET {@code /api/v1/admin/payroll/periods/{id}} — single period detail</li>
 * </ul>
 *
 * <p><b>Phase 2 (GAP-057b)</b> ships the {@code POST /run}, {@code POST
 * /periods/{id}/approve}, {@code POST /periods/{id}/pay}, payslip PDF
 * download, and bank export endpoints.
 *
 * @author KiteClass Team
 * @since 4.x (Wave 18a Bucket C)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/payroll")
@RequiredArgsConstructor
@Tag(name = "Admin / Payroll", description = "Phase 1 read-only payroll views")
public class PayrollController {

    private final PayrollService payrollService;

    /**
     * List teacher payroll configs (admin view).
     *
     * @param page page index (0-based)
     * @param size page size
     * @param sort sort string in {@code field,asc|desc} format (default: id,asc)
     * @return paged config DTOs
     */
    @GetMapping("/configs")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "List payroll configs",
            description = "Returns paged teacher payroll configs (Phase 1 read-only).")
    public ApiResponse<PageResponse<PayrollConfigResponse>> listConfigs(
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'id,asc')")
            @RequestParam(defaultValue = "id,asc") String sort) {
        log.debug("REST list payroll configs page={} size={} sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ApiResponse.success(payrollService.listConfigs(pageable));
    }

    /**
     * List payroll periods, optionally filtered by teacher and date range.
     *
     * @param teacherId optional teacher FK
     * @param startDate optional inclusive start filter (period.startDate &gt;= this)
     * @param endDate   optional inclusive end filter (period.endDate &lt;= this)
     * @param page page index
     * @param size page size
     * @param sort sort string
     * @return paged period DTOs
     */
    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "List payroll periods",
            description = "Returns paged payroll periods filterable by teacher + date range.")
    public ApiResponse<PageResponse<PayrollPeriodResponse>> listPeriods(
            @Parameter(description = "Teacher ID filter (optional)")
            @RequestParam(required = false) Long teacherId,
            @Parameter(description = "Start date filter (inclusive, ISO yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date filter (inclusive, ISO yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'startDate,desc')")
            @RequestParam(defaultValue = "startDate,desc") String sort) {
        log.debug("REST list payroll periods teacherId={} {}..{} sort={}",
                teacherId, startDate, endDate, sort);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ApiResponse.success(payrollService.listPeriods(teacherId, startDate, endDate, pageable));
    }

    /**
     * Single period detail view.
     *
     * @param id payroll period PK
     * @return PayrollPeriodResponse
     */
    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "Get payroll period by ID",
            description = "Returns a single payroll period detail (Phase 1 read-only).")
    public ApiResponse<PayrollPeriodResponse> getPeriod(
            @Parameter(description = "Period ID") @PathVariable Long id) {
        log.debug("REST get payroll period id={}", id);
        PayrollPeriod period = payrollService.getPeriodById(id);
        return ApiResponse.success(PayrollPeriodResponse.from(period));
    }

    /**
     * Parse sort string of the form "field,direction" (default ASC).
     */
    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
