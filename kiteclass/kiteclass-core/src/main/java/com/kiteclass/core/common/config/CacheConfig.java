package com.kiteclass.core.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    /**
     * Value serializer for Redis cache entries. Extracted + package-visible so a
     * unit test can assert the round-trip (serialize → deserialize) the @Cacheable
     * path depends on — GAP-1421: the previous config wrote JSON it could not read
     * back ("missing type id '@class'") → HTTP 500 on cache HIT for cached entities.
     */
    static GenericJackson2JsonRedisSerializer redisValueSerializer() {
        // Configure ObjectMapper with Java 8 date/time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // GAP-1421: JPA entities expose computed getters (e.g. Course#isReadOnly())
        // that serialize as JSON properties but have no settable field — without
        // this the cache READ throws SerializationException ("Unrecognized field")
        // → HTTP 500 on cache HIT. Cache-only mapper, so REST request binding (which
        // SHOULD reject unknown fields) is unaffected.
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // GAP-1421 + Jackson-2.21 deprecation: store @class type info in Redis so the
        // exact concrete type round-trips, INCLUDING final DTO **records** (e.g.
        // CourseResponse). Previously this called
        // objectMapper.activateDefaultTyping(validator, DefaultTyping.EVERYTHING, ...),
        // but DefaultTyping.EVERYTHING is @Deprecated since Jackson 2.21, and its only
        // successor enum NON_FINAL_AND_ENUMS still SKIPS final types → final records
        // lose their root @class → cache READ throws "missing type id property
        // '@class'" → HTTP 500 on cache HIT. Spring Data Redis' own builder with
        // defaultTyping(true) installs a non-deprecated StdTypeResolverBuilder that
        // writes @class for ALL types (final records included) — same behaviour the
        // no-arg GenericJackson2JsonRedisSerializer ships, but layered onto our custom
        // mapper (JavaTimeModule + relaxed unknown-property handling). Cache-only
        // mapper, so REST request binding is unaffected.
        return GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper)
                .defaultTyping(true)
                .build();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = redisValueSerializer();

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
