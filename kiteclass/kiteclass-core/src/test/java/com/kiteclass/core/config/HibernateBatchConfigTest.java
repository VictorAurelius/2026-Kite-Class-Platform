package com.kiteclass.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Hibernate batch settings are configured in
 * {@code src/main/resources/application.yml} per GAP-133.
 *
 * <p>Without {@code spring.jpa.properties.hibernate.jdbc.batch_size}, every
 * {@code persist()} issues its own JDBC INSERT round-trip — the bulk-import
 * path (GAP-051, ~10 000 rows) takes 40-60 s of pure round-trip overhead.
 *
 * <p>The test reads the production {@code application.yml} directly from disk
 * (NOT the test-profile override) so a missing/regressed property is caught at
 * unit-test time without booting a full Spring context.
 *
 * @since 4.5.0 (GAP-133 fix)
 */
@DisplayName("Hibernate JDBC batching — GAP-133")
class HibernateBatchConfigTest {

    private static final Properties PROD_PROPERTIES = loadProductionApplicationYml();

    private static Properties loadProductionApplicationYml() {
        Path prodYml = Paths.get("src/main/resources/application.yml");
        if (!Files.exists(prodYml)) {
            throw new IllegalStateException(
                    "Cannot locate production application.yml at " + prodYml.toAbsolutePath()
                            + " — test must be executed from the module root via `mvn test`.");
        }
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(prodYml.toFile()));
        yaml.afterPropertiesSet();
        Properties props = yaml.getObject();
        if (props == null) {
            throw new IllegalStateException("Failed to parse application.yml");
        }
        return props;
    }

    private String prop(String key) {
        Object value = PROD_PROPERTIES.get(key);
        return value == null ? null : value.toString();
    }

    @Test
    @DisplayName("hibernate.jdbc.batch_size MUST be ≥ 25 (recommended 50)")
    void batchSize_isConfigured() {
        String value = prop("spring.jpa.properties.hibernate.jdbc.batch_size");
        assertThat(value)
                .as("spring.jpa.properties.hibernate.jdbc.batch_size MUST be set in application.yml")
                .isNotNull();
        int batchSize = Integer.parseInt(value);
        assertThat(batchSize)
                .as("Hibernate JDBC batch_size must be ≥ 25 (recommended 50) to enable batching")
                .isGreaterThanOrEqualTo(25);
    }

    @Test
    @DisplayName("hibernate.order_inserts MUST be true")
    void orderInserts_isEnabled() {
        String value = prop("spring.jpa.properties.hibernate.order_inserts");
        assertThat(value).as("order_inserts must be set").isNotNull();
        assertThat(Boolean.parseBoolean(value))
                .as("hibernate.order_inserts must be true so batches group same-table inserts")
                .isTrue();
    }

    @Test
    @DisplayName("hibernate.order_updates MUST be true")
    void orderUpdates_isEnabled() {
        String value = prop("spring.jpa.properties.hibernate.order_updates");
        assertThat(value).as("order_updates must be set").isNotNull();
        assertThat(Boolean.parseBoolean(value)).isTrue();
    }

    @Test
    @DisplayName("hibernate.jdbc.batch_versioned_data MUST be true (BaseEntity uses @Version)")
    void batchVersionedData_isEnabled() {
        String value = prop("spring.jpa.properties.hibernate.jdbc.batch_versioned_data");
        assertThat(value).as("batch_versioned_data must be set").isNotNull();
        assertThat(Boolean.parseBoolean(value))
                .as("BaseEntity has @Version field — batching MUST be enabled for versioned data")
                .isTrue();
    }
}
