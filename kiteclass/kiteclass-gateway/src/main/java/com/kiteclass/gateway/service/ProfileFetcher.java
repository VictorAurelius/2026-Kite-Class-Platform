package com.kiteclass.gateway.service;

import com.kiteclass.gateway.common.constant.UserType;
import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.service.dto.ParentProfileResponse;
import com.kiteclass.gateway.service.dto.StudentProfileResponse;
import com.kiteclass.gateway.service.dto.TeacherProfileResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for fetching user profiles from Core service.
 *
 * <p>Encapsulates the logic of fetching different profile types (Student/Teacher/Parent)
 * based on UserType and referenceId. Used during login to enrich authentication response.
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 * // In AuthService login()
 * Object profile = profileFetcher.fetchProfile(user.getUserType(), user.getReferenceId());
 * if (profile instanceof StudentProfileResponse student) {
 *     // Include student profile in LoginResponse
 * }
 * }
 * </pre>
 *
 * <h3>Error Handling:</h3>
 * <ul>
 *   <li>Returns null for ADMIN/STAFF (no Core profile)</li>
 *   <li>Returns null if Core service unavailable (logs warning)</li>
 *   <li>Returns null if profile not found (logs warning)</li>
 *   <li>Throws exception only for unexpected errors</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.8.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileFetcher {

    private static final String INTERNAL_HEADER = "true";

    private final CoreServiceClient coreServiceClient;

    /**
     * Fetches user profile from Core service based on userType and referenceId.
     *
     * <p>Profile Types:
     * <ul>
     *   <li>ADMIN, STAFF → Returns null (no Core entity)</li>
     *   <li>STUDENT → Returns StudentProfileResponse</li>
     *   <li>TEACHER → Returns TeacherProfileResponse</li>
     *   <li>PARENT → Returns ParentProfileResponse</li>
     * </ul>
     *
     * @param userType    Type of user (ADMIN, STAFF, TEACHER, PARENT, STUDENT)
     * @param referenceId ID of the Core entity (required for TEACHER/PARENT/STUDENT)
     * @return Profile object (StudentProfileResponse, TeacherProfileResponse, ParentProfileResponse, or null)
     * @throws IllegalArgumentException if referenceId is null for external user types
     */
    public Object fetchProfile(UserType userType, Long referenceId) {
        log.debug("Fetching profile for userType={}, referenceId={}", userType, referenceId);

        // Internal staff have no profile in Core service
        if (userType.isInternalStaff()) {
            log.debug("User is internal staff ({}), no profile to fetch", userType);
            return null;
        }

        // Validate referenceId for external users
        if (referenceId == null) {
            log.warn("ReferenceId is null for userType={}, cannot fetch profile", userType);
            throw new IllegalArgumentException(
                    "ReferenceId is required for userType " + userType
            );
        }

        // Fetch profile based on userType
        try {
            return switch (userType) {
                case STUDENT -> fetchStudentProfile(referenceId);
                case TEACHER -> fetchTeacherProfile(referenceId);
                case PARENT -> fetchParentProfile(referenceId);
                default -> {
                    log.warn("Unsupported userType={}, returning null", userType);
                    yield null;
                }
            };
        } catch (FeignException.NotFound e) {
            log.warn("Profile not found in Core service: userType={}, referenceId={}",
                    userType, referenceId);
            return null;
        } catch (FeignException.ServiceUnavailable | FeignException.InternalServerError e) {
            log.error("Core service unavailable or error: userType={}, referenceId={}",
                    userType, referenceId, e);
            return null;
        } catch (FeignException e) {
            log.error("Feign error fetching profile: userType={}, referenceId={}, status={}",
                    userType, referenceId, e.status(), e);
            return null;
        }
    }

    /**
     * Fetches student profile from Core service.
     *
     * @param studentId Student ID in Core service
     * @return StudentProfileResponse or null if not found
     */
    private StudentProfileResponse fetchStudentProfile(Long studentId) {
        log.debug("Fetching student profile: studentId={}", studentId);
        ApiResponse<StudentProfileResponse> response =
                coreServiceClient.getStudent(studentId, INTERNAL_HEADER).block();
        return response != null ? response.getData() : null;
    }

    /**
     * Fetches teacher profile from Core service.
     *
     * @param teacherId Teacher ID in Core service
     * @return TeacherProfileResponse or null if not found
     */
    private TeacherProfileResponse fetchTeacherProfile(Long teacherId) {
        log.debug("Fetching teacher profile: teacherId={}", teacherId);
        ApiResponse<TeacherProfileResponse> response =
                coreServiceClient.getTeacher(teacherId, INTERNAL_HEADER).block();
        return response != null ? response.getData() : null;
    }

    /**
     * Fetches parent profile from Core service.
     *
     * <p><b>Note:</b> Parent module not yet implemented in Core.
     * This method will return null until implementation is complete.
     *
     * @param parentId Parent ID in Core service
     * @return ParentProfileResponse or null if not found
     */
    private ParentProfileResponse fetchParentProfile(Long parentId) {
        log.debug("Fetching parent profile: parentId={}", parentId);
        log.warn("Parent module not yet implemented in Core service");
        // Will be enabled when Parent module is implemented
        // ApiResponse<ParentProfileResponse> response =
        //         coreServiceClient.getParent(parentId, INTERNAL_HEADER);
        // return response.getData();
        return null;
    }
}
