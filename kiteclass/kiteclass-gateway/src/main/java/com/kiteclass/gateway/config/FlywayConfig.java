package com.kiteclass.gateway.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway configuration for database migrations.
 *
 * <p>Explicitly configures JDBC DataSource for Flyway when R2DBC is the primary datasource.
 * Spring Boot doesn't auto-configure JDBC DataSource when R2DBC is present, so we need to
 * provide it manually.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Configuration
public class FlywayConfig {

    /**
     * JDBC DataSource for Flyway migrations.
     *
     * <p>Uses HikariCP for connection pooling. Annotated with @FlywayDataSource
     * so Spring Boot's FlywayAutoConfiguration will use this for migrations.
     *
     * @return DataSource instance for Flyway
     */
    @Bean("flywayDataSource")
    @FlywayDataSource
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource flywayDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                "jdbc:postgresql://localhost:5432/kiteclass_dev"));
        config.setUsername(System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "kiteclass"));
        config.setPassword(System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "kiteclass123"));
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setPoolName("FlywayHikariPool");
        return new HikariDataSource(config);
    }
}
