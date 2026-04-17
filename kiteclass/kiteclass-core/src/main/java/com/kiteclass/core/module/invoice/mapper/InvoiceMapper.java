package com.kiteclass.core.module.invoice.mapper;

import com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse;
import com.kiteclass.core.module.invoice.dto.InstallmentResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceAdjustmentResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceItemResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceResponse;
import com.kiteclass.core.module.invoice.dto.RefundRequestResponse;
import com.kiteclass.core.module.invoice.entity.Installment;
import com.kiteclass.core.module.invoice.entity.InstallmentPlan;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceAdjustment;
import com.kiteclass.core.module.invoice.entity.InvoiceItem;
import com.kiteclass.core.module.invoice.entity.RefundRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * MapStruct mapper for Invoice entities and DTOs.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    /**
     * Converts Invoice entity to response DTO.
     *
     * @param invoice the invoice entity
     * @return invoice response DTO
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(invoice.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(invoice.getUpdatedAt()))")
    InvoiceResponse toResponse(Invoice invoice);

    /**
     * Converts list of InvoiceItem entities to response DTOs.
     *
     * @param items list of invoice items
     * @return list of invoice item response DTOs
     */
    List<InvoiceItemResponse> toItemResponseList(List<InvoiceItem> items);

    /**
     * Converts InvoiceItem entity to response DTO.
     *
     * @param item the invoice item entity
     * @return invoice item response DTO
     */
    InvoiceItemResponse toItemResponse(InvoiceItem item);

    /**
     * Converts list of InvoiceAdjustment entities to response DTOs.
     *
     * @param adjustments list of adjustments
     * @return list of adjustment response DTOs
     */
    List<InvoiceAdjustmentResponse> toAdjustmentResponseList(List<InvoiceAdjustment> adjustments);

    /**
     * Converts InvoiceAdjustment entity to response DTO.
     *
     * @param adjustment the adjustment entity
     * @return adjustment response DTO
     */
    InvoiceAdjustmentResponse toAdjustmentResponse(InvoiceAdjustment adjustment);

    /**
     * Converts InstallmentPlan entity to response DTO.
     *
     * @param plan the installment plan entity
     * @return installment plan response DTO
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(plan.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(plan.getUpdatedAt()))")
    InstallmentPlanResponse toPlanResponse(InstallmentPlan plan);

    /**
     * Converts list of Installment entities to response DTOs.
     *
     * @param installments list of installments
     * @return list of installment response DTOs
     */
    List<InstallmentResponse> toInstallmentResponseList(List<Installment> installments);

    /**
     * Converts Installment entity to response DTO.
     *
     * @param installment the installment entity
     * @return installment response DTO
     */
    InstallmentResponse toInstallmentResponse(Installment installment);

    /**
     * Converts RefundRequest entity to response DTO.
     *
     * @param request the refund request entity
     * @return refund request response DTO
     */
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(request.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(request.getUpdatedAt()))")
    RefundRequestResponse toRefundResponse(RefundRequest request);

    /**
     * Converts Instant to LocalDateTime.
     *
     * @param instant the instant to convert
     * @return LocalDateTime in system default timezone
     */
    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
