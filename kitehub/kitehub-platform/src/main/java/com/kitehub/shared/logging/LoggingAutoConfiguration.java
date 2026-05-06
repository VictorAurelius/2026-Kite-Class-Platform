package com.kitehub.shared.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-wires the shared logging components when their classpath dependencies are present.
 *
 * <p>Servlet-stack services (web MVC) get {@link TenantContextFilter} registered with
 * a high precedence (just above security filters). WebFlux services and non-web jars
 * skip the filter — they register the equivalent reactive component locally.</p>
 *
 * <p>RabbitMQ post-processor is provided as a {@link RabbitMQTenantInterceptor} bean
 * when Spring AMQP is on the classpath; consumers wire it onto their {@code RabbitTemplate}
 * or listener container as needed (no auto-attach to avoid fighting service-specific
 * RabbitMQ configuration).</p>
 */
@Configuration(proxyBeanMethods = false)
public class LoggingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    static class ServletConfig {

        @Bean
        @ConditionalOnMissingBean
        public TenantContextFilter tenantContextFilter() {
            return new TenantContextFilter();
        }

        @Bean
        public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
                TenantContextFilter filter) {
            FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>(filter);
            reg.addUrlPatterns("/*");
            // Run after Spring Security (default 0 + a few) so SecurityContext is populated.
            reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
            reg.setName("tenantContextFilter");
            return reg;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.amqp.core.MessagePostProcessor")
    static class AmqpConfig {

        @Bean
        @ConditionalOnMissingBean
        public RabbitMQTenantInterceptor rabbitMQTenantInterceptor() {
            return new RabbitMQTenantInterceptor();
        }
    }
}
