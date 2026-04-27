package com.kiteclass.core.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full Spring Boot context with an embedded Tomcat on a random port,
 * fetches the springdoc-openapi spec at {@code /api-docs}, asserts shape
 * invariants, and writes the JSON to {@code target/openapi-export/openapi.json}
 * so CI can upload it as an artifact for downstream consumers (FE codegen,
 * contract drift check).
 *
 * <p>Uses {@link WebEnvironment#RANDOM_PORT} + {@link TestRestTemplate} rather
 * than {@code @AutoConfigureMockMvc} because springdoc's {@code OpenApiResource}
 * MVC controllers register reliably only with a fully-initialized servlet
 * container; MockMvc bypasses some of that init (exhibited as 500
 * NoResourceFoundException on /api-docs).
 *
 * <p>Min path count is the floor — kiteclass-core has 34 controllers as of
 * 2026-04-27 and the spec should expose substantially more endpoints than
 * the threshold. If this drops, controllers were either deleted or stopped
 * being scanned.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "springdoc.api-docs.path=/api-docs"
)
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
class OpenApiSpecExportTest {

    /**
     * The production {@code application.yml} configures springdoc at {@code /api-docs};
     * the test {@code application.yml} (loaded in preference) doesn't carry that override,
     * so we re-assert it via {@code @SpringBootTest(properties=...)} to keep the test
     * checking the actual production path.
     */
    private static final int MIN_PATH_COUNT = 60;
    private static final Path OUTPUT_DIR = Paths.get("target", "openapi-export");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("openapi.json");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI spec exposes ≥60 paths and writes to target/openapi-export/openapi.json")
    void exportSpec() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api-docs", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("GET /api-docs returns 2xx (got %s)", response.getStatusCode())
                .isTrue();

        JsonNode root = objectMapper.readTree(response.getBody());

        assertThat(root.path("openapi").asText())
                .as("OpenAPI version field present")
                .startsWith("3.");

        JsonNode paths = root.path("paths");
        assertThat(paths.isObject())
                .as("paths object present")
                .isTrue();
        assertThat(paths.size())
                .as("path count floor (kiteclass-core has 34 controllers)")
                .isGreaterThanOrEqualTo(MIN_PATH_COUNT);

        Files.createDirectories(OUTPUT_DIR);
        Files.writeString(OUTPUT_FILE, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    }
}
