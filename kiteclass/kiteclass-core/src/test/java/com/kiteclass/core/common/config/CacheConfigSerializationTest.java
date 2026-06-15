package com.kiteclass.core.common.config;

import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.common.constant.CourseStatus;
import com.kiteclass.core.module.course.entity.PricingModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * GAP-1421: the Redis cache value serializer must round-trip a JPA entity
 * (serialize → deserialize) without throwing. The @Cacheable getById on Course
 * returned HTTP 500 on cache HIT because the configured serializer wrote a JSON
 * value it could not read back ("missing type id property '@class'").
 */
class CacheConfigSerializationTest {

    private static Course sampleCourse() {
        Course c = Course.builder()
                .name("Toán 10")
                .code("MATH10-HK1")
                .status(CourseStatus.DRAFT)
                .pricingModel(PricingModel.PER_HOUR)
                .price(new BigDecimal("1500000.00"))
                .unitPrice(new BigDecimal("200000.00"))
                .durationWeeks(12)
                .totalSessions(24)
                .build();
        c.setId(26L);
        c.setInstanceId(UUID.randomUUID());
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        c.setCreatedBy(UUID.randomUUID());
        c.setDeleted(false);
        c.setVersion(0L);
        return c;
    }



    @Test
    @DisplayName("configured cache serializer round-trips a Course (no SerializationException)")
    void courseRoundTripsThroughCacheSerializer() {
        GenericJackson2JsonRedisSerializer serializer = CacheConfig.redisValueSerializer();
        Course course = sampleCourse();

        byte[] bytes = serializer.serialize(course);

        assertThatCode(() -> {
            Object back = serializer.deserialize(bytes);
            assertThat(back).isInstanceOf(Course.class);
            assertThat(((Course) back).getCode()).isEqualTo("MATH10-HK1");
        }).doesNotThrowAnyException();
    }
}
