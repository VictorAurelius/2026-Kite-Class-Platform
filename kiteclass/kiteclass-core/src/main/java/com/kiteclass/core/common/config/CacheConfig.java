package com.kiteclass.core.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration.
 *
 * <p>Configures Spring Cache with Redis backend:
 * <ul>
 *   <li>Default TTL: 1 hour</li>
 *   <li>Key serializer: String</li>
 *   <li>Value serializer: JSON (Jackson)</li>
 *   <li>Null values not cached</li>
 * </ul>
 *
 * <p>Usage in services:
 * <pre>
 * {@code @Cacheable(value = "students", key = "#id")}
 * public StudentDTO getStudentById(Long id) { ... }
 *
 * {@code @CacheEvict(value = "students", key = "#id")}
 * public void updateStudent(Long id, StudentDTO dto) { ... }
 *
 * {@code @CacheEvict(value = "students", allEntries = true)}
 * public void deleteStudent(Long id) { ... }
 * </pre>
 *
 * @author KiteClass Team
 * @since 2.2.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures Redis cache manager with default settings.
     *
     * @param connectionFactory Redis connection factory
     * @return configured RedisCacheManager
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // Default TTL: 1 hour
                .disableCachingNullValues()  // Don't cache null values
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
