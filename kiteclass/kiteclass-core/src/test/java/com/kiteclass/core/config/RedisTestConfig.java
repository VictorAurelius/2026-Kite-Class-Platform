package com.kiteclass.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for integration tests.
 *
 * <p>Configures RedisTemplate with proper Jackson ObjectMapper that supports Java 8 date/time types.
 * This fixes serialization issues with LocalDate, LocalDateTime, etc. when caching DTOs in Redis.
 *
 * <p>Problem: Default Redis serialization in test context doesn't include JSR310 (Java Time) module,
 * causing SerializationException when trying to cache objects with LocalDate fields.
 *
 * <p>Solution: Register JavaTimeModule explicitly and disable timestamp serialization to use
 * ISO-8601 date format instead.
 *
 * @author KiteClass Team
 * @since 2.3.2
 */
@TestConfiguration
public class RedisTestConfig {

    /**
     * Configures RedisTemplate with Jackson serialization that supports Java 8 date/time.
     *
     * @param connectionFactory the Redis connection factory from TestContainers
     * @return configured RedisTemplate with proper serialization
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure ObjectMapper with Java 8 date/time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Use Jackson serializer with configured ObjectMapper
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setDefaultSerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }
}
