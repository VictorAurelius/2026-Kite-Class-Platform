package com.kitehub.shared.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQTenantInterceptorTest {

    private final RabbitMQTenantInterceptor interceptor = new RabbitMQTenantInterceptor();

    @BeforeEach
    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void outboundCopiesMdcOntoMessageHeaders() {
        MDC.put(TenantContextFilter.MDC_TENANT_ID, "tenant-1");
        MDC.put(TenantContextFilter.MDC_TRACE_ID, "trace-2");
        Message msg = new Message(new byte[0], new MessageProperties());

        Message out = interceptor.postProcessMessage(msg);

        assertThat(out.getMessageProperties().getHeaders())
            .containsEntry(RabbitMQTenantInterceptor.HEADER_PREFIX
                + TenantContextFilter.MDC_TENANT_ID, "tenant-1")
            .containsEntry(RabbitMQTenantInterceptor.HEADER_PREFIX
                + TenantContextFilter.MDC_TRACE_ID, "trace-2");
    }

    @Test
    void inboundAppliesHeadersToMdc() {
        MessageProperties props = new MessageProperties();
        props.setHeader(RabbitMQTenantInterceptor.HEADER_PREFIX
            + TenantContextFilter.MDC_TENANT_ID, "tenant-9");
        props.setHeader(RabbitMQTenantInterceptor.HEADER_PREFIX
            + TenantContextFilter.MDC_TRACE_ID, "trace-9");
        Message msg = new Message(new byte[0], props);

        RabbitMQTenantInterceptor.applyToInbound(msg);

        assertThat(MDC.get(TenantContextFilter.MDC_TENANT_ID)).isEqualTo("tenant-9");
        assertThat(MDC.get(TenantContextFilter.MDC_TRACE_ID)).isEqualTo("trace-9");

        RabbitMQTenantInterceptor.clear();
        assertThat(MDC.get(TenantContextFilter.MDC_TENANT_ID)).isNull();
        assertThat(MDC.get(TenantContextFilter.MDC_TRACE_ID)).isNull();
    }

    @Test
    void emptyMdcProducesNoHeaders() {
        Message msg = new Message(new byte[0], new MessageProperties());
        Message out = interceptor.postProcessMessage(msg);
        assertThat(out.getMessageProperties().getHeaders())
            .doesNotContainKeys(
                RabbitMQTenantInterceptor.HEADER_PREFIX + TenantContextFilter.MDC_TENANT_ID,
                RabbitMQTenantInterceptor.HEADER_PREFIX + TenantContextFilter.MDC_TRACE_ID);
    }
}
