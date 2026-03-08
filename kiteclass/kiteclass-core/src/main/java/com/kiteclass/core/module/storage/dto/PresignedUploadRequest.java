package com.kiteclass.core.module.storage.dto;

import com.kiteclass.core.module.storage.constant.AccessLevel;
import com.kiteclass.core.module.storage.constant.FileType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for generating presigned upload URL.
 *
 * <p>Contains file metadata for upload validation:
 * <ul>
 *   <li>fileName: Original filename (required, max 500 chars)</li>
 *   <li>fileSize: File size in bytes (required, min 1 byte)</li>
 *   <li>mimeType: MIME type (required, max 100 chars)</li>
 *   <li>fileType: File classification (IMAGE, DOCUMENT, VIDEO, AUDIO, OTHER)</li>
 *   <li>accessLevel: Access control (PUBLIC, PRIVATE, TENANT) - default PRIVATE</li>
 * </ul>
 *
 * @param fileName    Original filename from client
 * @param fileSize    File size in bytes
 * @param mimeType    MIME type (e.g., "image/jpeg", "application/pdf")
 * @param fileType    File type classification
 * @param accessLevel Access control level
 * @author KiteClass Team
 * @since 2.10.1
 */
public record PresignedUploadRequest(
    @NotBlank(message = "Tên file là bắt buộc")
    @Size(max = 500, message = "Tên file không được vượt quá 500 ký tự")
    String fileName,

    @NotNull(message = "Kích thước file là bắt buộc")
    @Min(value = 1, message = "Kích thước file phải lớn hơn 0")
    Long fileSize,

    @NotBlank(message = "Loại MIME là bắt buộc")
    @Size(max = 100, message = "Loại MIME không được vượt quá 100 ký tự")
    String mimeType,

    @NotNull(message = "Loại file là bắt buộc")
    FileType fileType,

    AccessLevel accessLevel
) {
    /**
     * Constructor with default access level.
     */
    public PresignedUploadRequest {
        if (accessLevel == null) {
            accessLevel = AccessLevel.PRIVATE;
        }
    }
}
