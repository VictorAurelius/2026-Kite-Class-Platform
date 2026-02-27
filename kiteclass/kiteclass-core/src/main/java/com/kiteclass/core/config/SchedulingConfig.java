package com.kiteclass.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduled tasks.
 *
 * <p>Enables Spring's @Scheduled annotation support for background jobs.
 *
 * <p>Scheduled jobs in the application:
 * <ul>
 *   <li>StorageCleanupScheduler - Marks expired uploads and cleans deleted files</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.10.1
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Spring will auto-detect @Scheduled methods
}
