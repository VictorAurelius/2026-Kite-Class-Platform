package com.kiteclass.core.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        // Configure ObjectMapper with Java 8 date/time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable default typing to store @class type information in Redis
        // This prevents ClassCastException when deserializing cached objects
        // Use PROPERTY format to store @class as JSON property (not WRAPPER_ARRAY)
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );

        // Create serializer with configured ObjectMapper
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // Default TTL: 1 hour
                .disableCachingNullValues()  // Don't cache null values
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                // GAP-1357: enableStatistics() makes RedisCache expose hit/miss
                // counters so Spring Boot binds cache.gets{result=hit|miss} to
                // Micrometer → /actuator/prometheus (cache_gets_total).
                .enableStatistics()
                .build();
    }
}
