package com.kitehub.email.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Registers an additional Thymeleaf {@link ClassLoaderTemplateResolver} for
 * plain-text email templates (GAP-657 §Step 1 — Wave 98 Bucket B1).
 *
 * <p>Spring Boot's default Thymeleaf auto-configuration registers an HTML
 * resolver only. To resolve {@code .txt} sibling templates (e.g.
 * {@code emails/welcome.txt}) we register a second resolver registered AFTER
 * SpringTemplateEngine creation via {@link PostConstruct} — avoids constructor
 * circular dependency that would arise from declaring a resolver bean that
 * injects the engine.</p>
 *
 * <p>Pattern: when senders need BOTH bodies (multipart/alternative), the
 * renderer calls {@code templateEngine.process("emails/welcome", ctx)} for
 * HTML and {@code templateEngine.process("emails/welcome.txt", ctx)} for
 * plain text — both resolve against the same classpath root but different
 * suffix patterns.</p>
 *
 * @since Wave 98 Bucket B1 (GAP-657)
 */
@Configuration
public class EmailTemplateResolverConfig {

    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * Register the TEXT resolver post-construction (avoids constructor-time
     * circular dep with {@code SpringTemplateEngine}).
     */
    @PostConstruct
    public void registerTextResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        // Order = high number => low priority. HTML resolver (auto-config) runs first;
        // this one picks up requests with explicit .txt suffix in template name.
        resolver.setOrder(50);
        resolver.setCheckExistence(true);
        templateEngine.addTemplateResolver(resolver);
    }

    /**
     * RestTemplate used by {@link com.kitehub.email.service.ResendEmailService}
     * when {@code email.provider=resend}. Default Spring auto-config does not
     * register a RestTemplate bean unless {@code RestTemplateAutoConfiguration}
     * triggers; this explicit declaration ensures Resend integration compiles
     * + wires without auto-config dependency.
     *
     * @since Wave 98 Bucket B1 (GAP-657)
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate emailRestTemplate() {
        return new RestTemplate();
    }
}
