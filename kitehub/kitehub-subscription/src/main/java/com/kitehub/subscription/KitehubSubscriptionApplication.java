package com.kitehub.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KiteHub Subscription Service main application.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.kitehub.platform.domain.entity", "com.kitehub.subscription.domain"})
@EnableJpaAuditing
@EnableScheduling
public class KitehubSubscriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(KitehubSubscriptionApplication.class, args);
    }
}
