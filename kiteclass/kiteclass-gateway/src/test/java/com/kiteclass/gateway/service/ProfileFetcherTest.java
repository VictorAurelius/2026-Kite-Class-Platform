package com.kiteclass.gateway.service;

import com.kiteclass.gateway.common.constant.UserType;
import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.service.dto.ParentProfileResponse;
import com.kiteclass.gateway.service.dto.StudentProfileResponse;
import com.kiteclass.gateway.service.dto.TeacherProfileResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link ProfileFetcher}.
 *
 * @author KiteClass Team
 * @since 1.8.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileFetcher Tests")
class ProfileFetcherTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @InjectMocks
    private ProfileFetcher profileFetcher;

    private StudentProfileResponse studentProfile;
    private ApiResponse<StudentProfileResponse> studentApiResponse;

    @BeforeEach
    void setUp() {
        studentProfile = new StudentProfileResponse(
                1L,
                "John Doe",
                "john@example.com",
                "0123456789",
                LocalDate.of(2010, 1, 15),
                "MALE",
                "https://example.com/avatar.jpg",
                "ACTIVE"
        );
        studentApiResponse = ApiResponse.success(studentProfile);
    }

    @Nested
    @DisplayName("Internal Staff Tests (ADMIN, STAFF)")
    class InternalStaffTests {

        @Test
        @DisplayName("Should return null for ADMIN userType")
        void shouldReturnNullForAdmin() {
            // when
            Object result = profileFetcher.fetchProfile(UserType.ADMIN, null);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient, never()).getStudent(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should return null for STAFF userType")
        void shouldReturnNullForStaff() {
            // when
            Object result = profileFetcher.fetchProfile(UserType.STAFF, null);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient, never()).getStudent(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("External User Tests (STUDENT, TEACHER, PARENT)")
    class ExternalUserTests {

        @Test
        @DisplayName("Should fetch student profile successfully")
        void shouldFetchStudentProfileSuccessfully() {
            // given
            when(coreServiceClient.getStudent(1L, "true"))
                    .thenReturn(studentApiResponse);

            // when
            Object result = profileFetcher.fetchProfile(UserType.STUDENT, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(StudentProfileResponse.class);
            StudentProfileResponse profile = (StudentProfileResponse) result;
            assertThat(profile.id()).isEqualTo(1L);
            assertThat(profile.name()).isEqualTo("John Doe");
            assertThat(profile.email()).isEqualTo("john@example.com");

            verify(coreServiceClient).getStudent(1L, "true");
        }

        @Test
        @DisplayName("Should return null for TEACHER (not implemented yet)")
        void shouldReturnNullForTeacher() {
            // when
            Object result = profileFetcher.fetchProfile(UserType.TEACHER, 1L);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient, never()).getTeacher(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should return null for PARENT (not implemented yet)")
        void shouldReturnNullForParent() {
            // when
            Object result = profileFetcher.fetchProfile(UserType.PARENT, 1L);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient, never()).getParent(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when referenceId is null for STUDENT")
        void shouldThrowExceptionWhenReferenceIdNullForStudent() {
            // when & then
            assertThatThrownBy(() -> profileFetcher.fetchProfile(UserType.STUDENT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ReferenceId is required for userType STUDENT");

            verify(coreServiceClient, never()).getStudent(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when referenceId is null for TEACHER")
        void shouldThrowExceptionWhenReferenceIdNullForTeacher() {
            // when & then
            assertThatThrownBy(() -> profileFetcher.fetchProfile(UserType.TEACHER, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ReferenceId is required for userType TEACHER");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when referenceId is null for PARENT")
        void shouldThrowExceptionWhenReferenceIdNullForParent() {
            // when & then
            assertThatThrownBy(() -> profileFetcher.fetchProfile(UserType.PARENT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ReferenceId is required for userType PARENT");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        private Request createDummyRequest() {
            return Request.create(
                    Request.HttpMethod.GET,
                    "/internal/students/999",
                    new HashMap<>(),
                    null,
                    new RequestTemplate()
            );
        }

        @Test
        @DisplayName("Should return null when profile not found (404)")
        void shouldReturnNullWhenProfileNotFound() {
            // given
            when(coreServiceClient.getStudent(999L, "true"))
                    .thenThrow(new FeignException.NotFound(
                            "Not found",
                            createDummyRequest(),
                            null,
                            null
                    ));

            // when
            Object result = profileFetcher.fetchProfile(UserType.STUDENT, 999L);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient).getStudent(999L, "true");
        }

        @Test
        @DisplayName("Should return null when Core service unavailable (503)")
        void shouldReturnNullWhenCoreServiceUnavailable() {
            // given
            when(coreServiceClient.getStudent(1L, "true"))
                    .thenThrow(new FeignException.ServiceUnavailable(
                            "Service unavailable",
                            createDummyRequest(),
                            null,
                            null
                    ));

            // when
            Object result = profileFetcher.fetchProfile(UserType.STUDENT, 1L);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient).getStudent(1L, "true");
        }

        @Test
        @DisplayName("Should return null when Core service has internal error (500)")
        void shouldReturnNullWhenCoreServiceHasInternalError() {
            // given
            when(coreServiceClient.getStudent(1L, "true"))
                    .thenThrow(new FeignException.InternalServerError(
                            "Internal server error",
                            createDummyRequest(),
                            null,
                            null
                    ));

            // when
            Object result = profileFetcher.fetchProfile(UserType.STUDENT, 1L);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient).getStudent(1L, "true");
        }

        @Test
        @DisplayName("Should return null for other FeignException (e.g., BadRequest 400)")
        void shouldReturnNullForOtherFeignException() {
            // given
            when(coreServiceClient.getStudent(1L, "true"))
                    .thenThrow(new FeignException.BadRequest(
                            "Bad request",
                            createDummyRequest(),
                            null,
                            null
                    ));

            // when
            Object result = profileFetcher.fetchProfile(UserType.STUDENT, 1L);

            // then
            assertThat(result).isNull();
            verify(coreServiceClient).getStudent(1L, "true");
        }
    }
}
