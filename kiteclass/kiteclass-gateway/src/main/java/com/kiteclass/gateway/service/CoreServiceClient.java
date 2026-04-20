package com.kiteclass.gateway.service;

import com.kiteclass.gateway.common.dto.ApiResponse;
import com.kiteclass.gateway.service.dto.CreateStudentInternalRequest;
import com.kiteclass.gateway.service.dto.ParentProfileResponse;
import com.kiteclass.gateway.service.dto.StudentProfileResponse;
import com.kiteclass.gateway.service.dto.TeacherProfileResponse;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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
    private final String internalApiSecret;

    /**
     * Connect timeout for the Reactor Netty HTTP client (TCP handshake).
     * GAP-131 — without this the JVM default is infinite, allowing a slow Core
     * upstream to block worker threads indefinitely.
     */
    static final int CONNECT_TIMEOUT_MS = 5_000;

    /**
     * Response timeout (server's first byte after request sent). 30 s bounds
     * worst-case latency on internal Gateway → Core hops (GAP-131).
     */
    static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Constructs CoreServiceClient with WebClient configured for Core service.
     *
     * @param baseUrl Core service base URL from application properties
     * @param internalApiSecret Secret key for HMAC-SHA256 signature generation (must match Core service)
     */
    public CoreServiceClient(
            @Value("${core.service.url:http://localhost:8081}") String baseUrl,
            @Value("${internal.api.secret:changeme-in-production}") String internalApiSecret) {
        // GAP-131: Netty HTTP client with explicit connect + response timeouts.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(RESPONSE_TIMEOUT);

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.internalApiSecret = internalApiSecret;
    }

    /**
     * Generates HMAC-SHA256 signature for internal API authentication.
     *
     * @return Array containing [signature, timestamp] strings
     */
    private String[] generateInternalHeaders() {
        long timestamp = System.currentTimeMillis() / 1000;
        String timestampStr = String.valueOf(timestamp);
        String signature = new HmacUtils("HmacSHA256", internalApiSecret).hmacHex(timestampStr);
        return new String[]{signature, timestampStr};
    }

    /**
     * Fetches student profile from Core service.
     *
     * <p>Endpoint: GET /internal/students/{id}
     *
     * @param id Student ID (matches User.referenceId)
     * @return Mono of ApiResponse containing StudentProfileResponse
     * @throws WebClientResponseException.NotFound if student not found (404)
     * @throws WebClientResponseException.Forbidden if HMAC signature invalid (403)
     */
    public Mono<ApiResponse<StudentProfileResponse>> getStudent(Long id) {
        log.debug("Fetching student profile: id={}", id);

        String[] headers = generateInternalHeaders();
        return webClient.get()
                .uri("/internal/students/{id}", id)
                .header("X-Internal-Signature", headers[0])
                .header("X-Internal-Timestamp", headers[1])
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
     * @param id Teacher ID (matches User.referenceId)
     * @return Mono of ApiResponse containing TeacherProfileResponse
     * @throws WebClientResponseException.NotFound if teacher not found (404)
     * @throws WebClientResponseException.Forbidden if HMAC signature invalid (403)
     */
    public Mono<ApiResponse<TeacherProfileResponse>> getTeacher(Long id) {
        log.debug("Fetching teacher profile: id={}", id);

        String[] headers = generateInternalHeaders();
        return webClient.get()
                .uri("/internal/teachers/{id}", id)
                .header("X-Internal-Signature", headers[0])
                .header("X-Internal-Timestamp", headers[1])
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
     * @param id Parent ID (matches User.referenceId)
     * @return Mono of ApiResponse containing ParentProfileResponse
     * @throws WebClientResponseException.NotFound if parent not found (404)
     * @throws WebClientResponseException.Forbidden if HMAC signature invalid (403)
     */
    public Mono<ApiResponse<ParentProfileResponse>> getParent(Long id) {
        log.debug("Fetching parent profile: id={}", id);

        String[] headers = generateInternalHeaders();
        return webClient.get()
                .uri("/internal/parents/{id}", id)
                .header("X-Internal-Signature", headers[0])
                .header("X-Internal-Timestamp", headers[1])
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
     * @param request Student creation request data
     * @return Mono of ApiResponse containing created StudentProfileResponse
     * @throws WebClientResponseException.BadRequest if validation fails (400)
     * @throws WebClientResponseException.Forbidden if HMAC signature invalid (403)
     * @throws WebClientResponseException.Conflict if email already exists (409)
     * @since 1.8.0
     */
    public Mono<ApiResponse<StudentProfileResponse>> createStudent(
            CreateStudentInternalRequest request,
            String tenantId) {
        log.debug("Creating student in Core: email={}, tenantId={}", request.email(), tenantId);

        String[] headers = generateInternalHeaders();
        return webClient.post()
                .uri("/internal/students")
                .header("X-Internal-Signature", headers[0])
                .header("X-Internal-Timestamp", headers[1])
                .header("X-Tenant-Id", tenantId)  // Forward tenant ID to Core
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
     * @param id Student ID to delete
     * @return Mono<Void> - completes when deletion successful
     * @throws WebClientResponseException.NotFound if student not found (404)
     * @throws WebClientResponseException.Forbidden if HMAC signature invalid (403)
     * @since 1.8.0
     */
    public Mono<Void> deleteStudent(Long id) {
        log.debug("Deleting student in Core: id={}", id);

        String[] headers = generateInternalHeaders();
        return webClient.delete()
                .uri("/internal/students/{id}", id)
                .header("X-Internal-Signature", headers[0])
                .header("X-Internal-Timestamp", headers[1])
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
