package com.kitehub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * KiteHub API Gateway Application.
 * <p>
 * Routes requests to platform services and KiteClass instances.
 *
 * @since 1.0
 */
@SpringBootApplication
@EntityScan(basePackages = "com.kitehub.platform.domain.entity")
@EnableJpaRepositories(basePackages = "com.kitehub.gateway.repository")
public class KiteHubGatewayApplication {

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KiteHubGatewayApplication.class, args);
    }
}
