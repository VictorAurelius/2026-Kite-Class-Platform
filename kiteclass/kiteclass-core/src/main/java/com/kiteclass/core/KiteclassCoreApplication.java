package com.kiteclass.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for KiteClass Core Service.
 *
 * <p>This service handles core business logic including:
 * <ul>
 *   <li>Student management</li>
 *   <li>Class and course management</li>
 *   <li>Attendance tracking</li>
 *   <li>Billing and invoicing</li>
 *   <li>Payment processing</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.1.0
 */
@SpringBootApplication
@EnableScheduling
public class KiteclassCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiteclassCoreApplication.class, args);
    }
}
