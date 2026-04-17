package com.kiteclass.core.module.storage.mapper;

import com.kiteclass.core.module.storage.dto.FileMetadataResponse;
import com.kiteclass.core.module.storage.dto.QuotaUsageResponse;
import com.kiteclass.core.module.storage.entity.StorageQuota;
import com.kiteclass.core.module.storage.entity.UploadedFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Storage entities and DTOs.
 *
 * <p>Provides mappings between:
 * <ul>
 *   <li>UploadedFile entity → FileMetadataResponse DTO</li>
 *   <li>StorageQuota entity → QuotaUsageResponse DTO</li>
 * </ul>
 *
 * <p>MapStruct generates implementation at compile time for type-safe mapping.
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Mapper(componentModel = "spring")
public interface StorageMapper {

    /**
     * Maps UploadedFile entity to FileMetadataResponse DTO.
     *
     * @param file the uploaded file entity
     * @return FileMetadataResponse DTO
     */
    FileMetadataResponse toMetadataResponse(UploadedFile file);

    /**
     * Maps StorageQuota entity to QuotaUsageResponse DTO.
     *
     * <p>Calls entity methods for calculated fields (remainingBytes, usagePercentage).
     *
     * @param quota the storage quota entity
     * @return QuotaUsageResponse DTO
     */
    @Mapping(target = "remainingBytes", expression = "java(quota.getRemainingBytes())")
    @Mapping(target = "usagePercentage", expression = "java(quota.getUsagePercentage())")
    QuotaUsageResponse toQuotaResponse(StorageQuota quota);
}
