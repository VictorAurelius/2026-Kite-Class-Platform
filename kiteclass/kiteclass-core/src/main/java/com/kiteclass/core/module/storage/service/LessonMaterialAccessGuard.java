package com.kiteclass.core.module.storage.service;

/**
 * Cross-module hook letting the storage download path enforce the LMS enrollment paywall
 * (BR-LMS-002) when a downloaded file is the material of a paid (non-trial) lesson.
 *
 * <p>GAP-1307: {@code StorageController.generateDownloadUrl} previously enforced only the
 * visibility model (PUBLIC / PRIVATE / TENANT). A material attached to a paid lesson but
 * stored at {@code TENANT} scope was therefore downloadable by any same-tenant student,
 * including students who never enrolled in (and never paid for) the course — bypassing the
 * paywall that {@code LessonAccessGuard} already enforces on the read/write LMS paths.
 *
 * <p><b>Defined in the storage module (dependency inversion):</b> the storage service depends
 * only on this interface, while the implementation lives in the LMS module where the
 * lesson/course/enrollment graph is owned. This keeps the storage module free of LMS imports.
 *
 * <p><b>Optional by design (GAP-1307 context-resilience):</b> {@code StorageServiceImpl}
 * injects this via {@code ObjectProvider} and treats a missing bean as "allow" — so a sliced
 * Spring context that scans storage but not LMS still loads (the earlier #2416 attempt hard-
 * required the bean, which failed {@code OpenApiSpecExportTest}-style full/sliced context loads).
 * The production full context always has the LMS-side implementation → the paywall is enforced.
 *
 * @author KiteClass Team
 * @since GAP-1307
 */
public interface LessonMaterialAccessGuard {

    /**
     * Verifies that the requester may download the given stored file when that file is the
     * material of a paid (non-trial) lesson.
     *
     * <p>The check is a no-op (access granted) for any of:
     * <ul>
     *   <li>{@code elevatedRole == true} — staff (teacher/owner/admin) are never paywalled;</li>
     *   <li>{@code requesterId} equals {@code uploaderId} — the uploader always keeps access;</li>
     *   <li>the file is not FK-linked to any lesson material (non-lesson file);</li>
     *   <li>every linked lesson is a trial/preview lesson (free content).</li>
     * </ul>
     *
     * <p>Otherwise, when the file backs a non-trial lesson, the requester MUST hold an ACTIVE
     * enrollment in the owning course; if not, access is denied (HTTP 403).
     *
     * @param uploadedFileId the {@code uploaded_files.id} of the file being downloaded — the
     *                       deterministic FK used to resolve backing lesson material
     * @param uploaderId     the user id that uploaded the file (exempt)
     * @param requesterId    the user id requesting the download
     * @param elevatedRole   true if the requester holds a staff role exempt from the paywall
     * @throws com.kiteclass.core.common.exception.PermissionDeniedException
     *         ({@code STUDENT_NOT_ENROLLED_IN_COURSE}) if the file backs a paid lesson the
     *         requester is not enrolled in
     */
    void verifyLessonMaterialDownloadAccess(Long uploadedFileId, Long uploaderId, Long requesterId, boolean elevatedRole);
}
