package com.kiteclass.core.module.storage.constant;

import lombok.Getter;

/**
 * File type classification for uploaded files.
 *
 * <p>Categorizes uploaded files by content type for organization and validation:
 * <ul>
 *   <li>IMAGE: Image files (JPEG, PNG, GIF, etc.)</li>
 *   <li>DOCUMENT: Document files (PDF, Word, Excel, etc.)</li>
 *   <li>VIDEO: Video files (MP4, AVI, MOV, etc.)</li>
 *   <li>AUDIO: Audio files (MP3, WAV, etc.)</li>
 *   <li>OTHER: Other file types</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Getter
public enum FileType {

    IMAGE("Hình ảnh", "Image files"),
    DOCUMENT("Tài liệu", "Document files"),
    VIDEO("Video", "Video files"),
    AUDIO("Âm thanh", "Audio files"),
    OTHER("Khác", "Other file types");

    private final String displayNameVi;
    private final String description;

    FileType(String displayNameVi, String description) {
        this.displayNameVi = displayNameVi;
        this.description = description;
    }

    /**
     * Returns icon class for UI display.
     *
     * @return Font Awesome icon class
     */
    public String getIconClass() {
        return switch (this) {
            case IMAGE -> "fa-image";
            case DOCUMENT -> "fa-file-alt";
            case VIDEO -> "fa-video";
            case AUDIO -> "fa-music";
            case OTHER -> "fa-file";
        };
    }

    /**
     * Determines file type from MIME type.
     *
     * @param mimeType MIME type string
     * @return Corresponding FileType
     */
    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return OTHER;
        }

        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) {
            return IMAGE;
        } else if (type.startsWith("video/")) {
            return VIDEO;
        } else if (type.startsWith("audio/")) {
            return AUDIO;
        } else if (type.contains("pdf") || type.contains("document") ||
                   type.contains("word") || type.contains("excel") ||
                   type.contains("powerpoint") || type.contains("text")) {
            return DOCUMENT;
        }
        return OTHER;
    }
}
