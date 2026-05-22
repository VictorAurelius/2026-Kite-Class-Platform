package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.TranscriptResponse;
import com.kiteclass.core.module.parent.service.ParentTranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only transcript endpoint for the parent portal (GAP-321 Phase 1A).
 *
 * <p>Identity is carried on {@code X-User-Reference-Id} populated by the
 * Gateway from {@code users.reference_id} when {@code userType = PARENT}.
 * Authorization is delegated to {@link ParentTranscriptService} which enforces
 * the {@code ParentStudentLink} edge guard — see service javadoc.
 *
 * <p>Phase 1A scope: only transcripts. Five other facets (điểm danh / học phí /
 * hạnh kiểm / notifications / kỷ luật) deferred to GAP-321b.
 *
 * @author KiteClass Team
 * @since 2.18.0 (Wave 18b1 — GAP-321 Phase 1A)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Parent Transcript", description = "Parent-side transcript reads (GAP-321 Phase 1A)")
public class ParentTranscriptController {

    private final ParentTranscriptService parentTranscriptService;

    /**
     * Returns all transcripts for one of the parent's linked children.
     *
     * <p>Returns:
     * <ul>
     *   <li>200 + list of transcripts (possibly empty) — happy path.</li>
     *   <li>401 {@code AUTH_REQUIRED} — Gateway didn't forward parent id.</li>
     *   <li>403 {@code PARENT_NOT_LINKED} — no active link to {@code childId}.</li>
     * </ul>
     */
    @PreAuthorize("@authz.hasAccessToChild(#childId)")
    @GetMapping("/children/{childId}/transcript")
    @Operation(summary = "List transcripts for one of the parent's linked children",
            description = "BR-PARENT-PORTAL-001: 403 PARENT_NOT_LINKED if parent is not linked to the requested child. "
                    + "Per-resource authz via @authz.hasAccessToChild (OWASP A01 defense-in-depth) — Wave 105 Bucket E0.")
    public ResponseEntity<ApiResponse<List<TranscriptResponse>>> getChildTranscript(
            @PathVariable Long childId,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        List<TranscriptResponse> transcripts =
                parentTranscriptService.getTranscriptsForChild(id, childId);
        return ResponseEntity.ok(ApiResponse.success(transcripts));
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
