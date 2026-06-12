package com.kiteclass.core.common.config;

import com.kiteclass.core.module.branding.events.BrandingDeployedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * G1 walk 2026-06-12 regression guard (GAP-1213): producer kitehub-branding gửi
 * {@code __TypeId__} = FQN class phía KH ({@code com.kitehub.branding.outbox.BrandingDeployedEvent})
 * — class KHÔNG tồn tại trong classpath kiteclass-core. Trước fix, converter ném
 * {@code ClassNotFoundException} → container reject + DROP message TRƯỚC KHI listener chạy
 * → landing không bao giờ đổi theme dù SSE báo "Triển khai thành công".
 *
 * <p>Fix = {@code RabbitConfig.jsonMessageConverter()} idClassMapping FQN producer → record local.
 * Test này tái lập đúng message của incident (payload + header thật từ walk log 08:41:22).</p>
 */
class RabbitConfigWireFormatTest {

    private final MessageConverter converter = new RabbitConfig().jsonMessageConverter();

    @Test
    @DisplayName("GAP-1213: __TypeId__ FQN kitehub → map về BrandingDeployedEvent local (không ClassNotFound)")
    void kitehubTypeIdHeaderResolvesToLocalRecord() {
        String payload = "{\"tenantId\":\"e8ff87e1-69fc-4842-a263-7385c68b4ffb\","
                + "\"slug\":\"sky-education\",\"frontendUrl\":\"http://localhost:3000/?tenant=sky-education\","
                + "\"primaryColor\":\"#f97316\",\"secondaryColor\":\"#1B4965\",\"accentColor\":\"#84cc16\","
                + "\"logoUrl\":null,\"brandingVersion\":1,\"deployedAt\":\"2026-06-12T08:41:22Z\"}";
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setContentEncoding("UTF-8");
        props.setHeader("__TypeId__", "com.kitehub.branding.outbox.BrandingDeployedEvent");

        Object converted = converter.fromMessage(
                new Message(payload.getBytes(StandardCharsets.UTF_8), props));

        assertThat(converted).isInstanceOf(BrandingDeployedEvent.class);
        BrandingDeployedEvent event = (BrandingDeployedEvent) converted;
        assertThat(event.tenantId()).isEqualTo("e8ff87e1-69fc-4842-a263-7385c68b4ffb");
        assertThat(event.brandingVersion()).isEqualTo(1);
        assertThat(event.primaryColor()).isEqualTo("#f97316");
    }
}
