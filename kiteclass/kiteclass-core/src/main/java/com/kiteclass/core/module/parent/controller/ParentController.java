package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ChildSummaryResponse;
import com.kiteclass.core.module.parent.dto.ParentResponse;
import com.kiteclass.core.module.parent.service.ParentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Self-service endpoints for the authenticated parent.
 *
 * <p>Identity is carried on the {@code X-User-Reference-Id} header, which the
 * Gateway populates from {@code users.reference_id} whenever it dispatches a
 * request on behalf of a user with {@code userType = PARENT}. This keeps Core
 * free of any Gateway-side persistence (there is no User table here) while
 * still letting the service enforce "parent can only see own children".
 *
 * @since 2.14.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Parent Self-Service", description = "Parent portal endpoints (GAP-052a)")
public class ParentController {

    private final ParentService parentService;

    @GetMapping("/me")
    @Operation(summary = "Get current parent profile")
    public ResponseEntity<ApiResponse<ParentResponse>> getMe(
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        ParentResponse response = parentService.getParentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me/children")
    @Operation(summary = "List the current parent's linked children")
    public ResponseEntity<ApiResponse<List<ChildSummaryResponse>>> getMyChildren(
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        List<ChildSummaryResponse> children = parentService.getChildrenOfParent(id);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
