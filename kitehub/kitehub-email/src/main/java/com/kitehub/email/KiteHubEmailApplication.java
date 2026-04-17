package com.kitehub.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * KiteHub Email Service - Email notifications via AWS SES.
 *
 * @since 1.0
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class KiteHubEmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiteHubEmailApplication.class, args);
    }
}
