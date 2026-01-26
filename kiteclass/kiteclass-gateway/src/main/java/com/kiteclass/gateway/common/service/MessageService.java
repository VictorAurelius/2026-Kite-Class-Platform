package com.kiteclass.gateway.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for retrieving i18n messages.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get message by code with current locale.
     *
     * @param code message code
     * @return resolved message
     */
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }

    /**
     * Get message by code with parameters.
     *
     * @param code message code
     * @param args arguments for message formatting
     * @return resolved message with arguments
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
    }

    /**
     * Get message with specific locale.
     *
     * @param code   message code
     * @param locale target locale
     * @param args   arguments for message formatting
     * @return resolved message
     */
    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
