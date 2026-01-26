package com.kiteclass.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for KiteClass Gateway Service.
 *
 * <p>KiteClass Gateway is responsible for:
 * <ul>
 *   <li>API Gateway - routing requests to Core Service</li>
 *   <li>User Management - CRUD operations for users</li>
 *   <li>Authentication - JWT-based login/logout</li>
 *   <li>Authorization - Role-based access control</li>
 *   <li>Rate Limiting - request throttling</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@SpringBootApplication
public class KiteclassGatewayApplication {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KiteclassGatewayApplication.class, args);
    }
}
