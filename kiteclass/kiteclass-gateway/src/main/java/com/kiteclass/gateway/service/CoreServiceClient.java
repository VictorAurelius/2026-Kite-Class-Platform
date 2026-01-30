package com.kiteclass.gateway.service;

import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.service.dto.CreateStudentInternalRequest;
import com.kiteclass.gateway.service.dto.ParentProfileResponse;
import com.kiteclass.gateway.service.dto.StudentProfileResponse;
import com.kiteclass.gateway.service.dto.TeacherProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Reactive client for Core Service internal APIs using WebClient.
 *
 * <p>Provides service-to-service communication between Gateway and Core services.
 * All endpoints require X-Internal-Request header for authentication.
 *
 * <h3>Configuration:</h3>
 * <pre>
 * # application.yml
 * core:
 *   service:
 *     url: http://localhost:8081  # Core service URL
 * </pre>
 *
 * <h3>Security:</h3>
 * <ul>
 *   <li>All requests include X-Internal-Request: true header</li>
 *   <li>Core service validates header via InternalRequestFilter</li>
 *   <li>Requests without header are rejected with 403 Forbidden</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 * @Autowired
 * private CoreServiceClient coreClient;
 *
 * // Fetch student profile
 * Mono<StudentProfileResponse> studentMono = coreClient.getStudent(studentId, "true")
 *     .map(ApiResponse::getData);
 * }
 * </pre>
 *
 * @see com.kiteclass.gateway.service.ProfileFetcher
 * @author KiteClass Team
 * @since 1.8.0
 */
@Service
@Slf4j
public class CoreServiceClient {

    private final WebClient webClient;

    /**
     * Constructs CoreServiceClient with WebClient configured for Core service.
     *
     * @param baseUrl Core service base URL from application properties
     */
    public CoreServiceClient(@Value("${core.service.url:http://localhost:8081}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Fetches student profile from Core service.
     *
     * <p>Endpoint: GET /internal/students/{id}
     *
     * @param id              Student ID (matches User.referenceId)
     * @param internalHeader  Must be "true" for authentication
     * @return Mono of ApiResponse containing StudentProfileResponse
     * @throws WebClientResponseException.NotFound if student not found (404)
     * @throws WebClientResponseException.Forbidden if header invalid (403)
     */
    public Mono<ApiResponse<StudentProfileResponse>> getStudent(Long id, String internalHeader) {
        log.debug("Fetching student profile: id={}", id);

        return webClient.get()
                .uri("/internal/students/{id}", id)
                .header("X-Internal-Request", internalHeader)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Client error fetching student {}: {}", id, response.statusCode());
                    return Mono.error(new WebClientResponseException(
                            "Student not found",
                            response.statusCode().value(),
                            response.statusCode().toString(),
                            null, null, null
                    ));
                })
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<StudentProfileResponse>>() {});
    }

    /**
     * Fetches teacher profile from Core service.
     *
     * <p>Endpoint: GET /internal/teachers/{id}
     *
     * <p><b>Note:</b> Teacher module not yet implemented in Core.
     * This method is a placeholder for future implementation.
     *
     * @param id              Teacher ID (matches User.referenceId)
     * @param internalHeader  Must be "true" for authentication
     * @return Mono of ApiResponse containing TeacherProfileResponse
     * @throws WebClientResponseException.NotFound if teacher not found (404)
     * @throws WebClientResponseException.Forbidden if header invalid (403)
     */
    public Mono<ApiResponse<TeacherProfileResponse>> getTeacher(Long id, String internalHeader) {
        log.debug("Fetching teacher profile: id={}", id);

        return webClient.get()
                .uri("/internal/teachers/{id}", id)
                .header("X-Internal-Request", internalHeader)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<TeacherProfileResponse>>() {});
    }

    /**
     * Fetches parent profile from Core service.
     *
     * <p>Endpoint: GET /internal/parents/{id}
     *
     * <p><b>Note:</b> Parent module not yet implemented in Core.
     * This method is a placeholder for future implementation.
     *
     * @param id              Parent ID (matches User.referenceId)
     * @param internalHeader  Must be "true" for authentication
     * @return Mono of ApiResponse containing ParentProfileResponse
     * @throws WebClientResponseException.NotFound if parent not found (404)
     * @throws WebClientResponseException.Forbidden if header invalid (403)
     */
    public Mono<ApiResponse<ParentProfileResponse>> getParent(Long id, String internalHeader) {
        log.debug("Fetching parent profile: id={}", id);

        return webClient.get()
                .uri("/internal/parents/{id}", id)
                .header("X-Internal-Request", internalHeader)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ParentProfileResponse>>() {});
    }

    /**
     * Creates a new student in Core service.
     *
     * <p>Endpoint: POST /internal/students
     *
     * <p>This is called during student registration flow:
     * <ol>
     *   <li>Gateway creates User record (without referenceId)</li>
     *   <li>Gateway calls this endpoint to create Student in Core</li>
     *   <li>Gateway updates User.referenceId with returned Student.id</li>
     * </ol>
     *
     * @param request         Student creation request data
     * @param internalHeader  Must be "true" for authentication
     * @return Mono of ApiResponse containing created StudentProfileResponse
     * @throws WebClientResponseException.BadRequest if validation fails (400)
     * @throws WebClientResponseException.Forbidden if header invalid (403)
     * @throws WebClientResponseException.Conflict if email already exists (409)
     * @since 1.8.0
     */
    public Mono<ApiResponse<StudentProfileResponse>> createStudent(
            CreateStudentInternalRequest request,
            String internalHeader) {
        log.debug("Creating student in Core: email={}", request.email());

        return webClient.post()
                .uri("/internal/students")
                .header("X-Internal-Request", internalHeader)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Client error creating student: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new WebClientResponseException(
                                    body,
                                    response.statusCode().value(),
                                    response.statusCode().toString(),
                                    null, null, null
                            )));
                })
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<StudentProfileResponse>>() {});
    }

    /**
     * Deletes a student in Core service (soft delete).
     *
     * <p>Endpoint: DELETE /internal/students/{id}
     *
     * <p>Called when a Gateway user account is deleted. Performs soft delete.
     *
     * @param id              Student ID to delete
     * @param internalHeader  Must be "true" for authentication
     * @return Mono<Void> - completes when deletion successful
     * @throws WebClientResponseException.NotFound if student not found (404)
     * @throws WebClientResponseException.Forbidden if header invalid (403)
     * @since 1.8.0
     */
    public Mono<Void> deleteStudent(Long id, String internalHeader) {
        log.debug("Deleting student in Core: id={}", id);

        return webClient.delete()
                .uri("/internal/students/{id}", id)
                .header("X-Internal-Request", internalHeader)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Client error deleting student {}: {}", id, response.statusCode());
                    return Mono.error(new WebClientResponseException(
                            "Student not found",
                            response.statusCode().value(),
                            response.statusCode().toString(),
                            null, null, null
                    ));
                })
                .bodyToMono(Void.class);
    }
}
