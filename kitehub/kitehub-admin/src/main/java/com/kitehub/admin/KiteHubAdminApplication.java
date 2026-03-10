package com.kitehub.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * KiteHub Admin Service - Platform management and analytics.
 *
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = {
        "com.kitehub.admin",
        "com.kitehub.platform",
        "com.kitehub.subscription"
})
@EnableJpaRepositories(basePackages = {
        "com.kitehub.subscription.repository"
})
@EntityScan(basePackages = {
        "com.kitehub.platform.domain.entity"
})
public class KiteHubAdminApplication {

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KiteHubAdminApplication.class, args);
    }
}
