package com.kiteclass.core.common.config;

import com.kiteclass.core.module.course.dto.CourseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * GAP-1421: the Redis cache value serializer must round-trip the types actually
 * cached. {@code @Cacheable("courses")} stores a {@link CourseResponse} — a Java
 * RECORD (final). With {@code DefaultTyping.NON_FINAL} the serializer wrote NO root
 * {@code @class} for final records, so the cache READ failed
 * ("missing type id property '@class'") → HTTP 500 on cache HIT. The fix types
 * final records too (round-trips). Course (entity) was the wrong subject — the
 * cached value is the DTO.
 */
class CacheConfigSerializationTest {

    private static CourseResponse sampleResponse() {
        return new CourseResponse(
                26L, "Lớp IELTS RW 6.5", "SKY-IELTS-RW", "desc", null, null, null,
                List.of(),                       // prerequisiteCourses
                null, 14L, 12, 24,
                new BigDecimal("1500000.00"),    // price
                "PER_HOUR",
                new BigDecimal("200000.00"),     // unitPrice
                "DRAFT", null, null, null,
                Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("configured cache serializer round-trips a CourseResponse record (no SerializationException)")
    void courseResponseRecordRoundTripsThroughCacheSerializer() {
        GenericJackson2JsonRedisSerializer serializer = CacheConfig.redisValueSerializer();
        CourseResponse dto = sampleResponse();

        byte[] bytes = serializer.serialize(dto);

        assertThatCode(() -> {
            Object back = serializer.deserialize(bytes);
            assertThat(back).isInstanceOf(CourseResponse.class);
            assertThat(((CourseResponse) back).code()).isEqualTo("SKY-IELTS-RW");
        }).doesNotThrowAnyException();
    }
}
