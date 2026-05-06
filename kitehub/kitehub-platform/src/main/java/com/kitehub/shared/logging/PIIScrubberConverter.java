package com.kitehub.shared.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback converter that scrubs the formatted message via {@link PIIScrubber}.
 *
 * <p>Wire it in {@code logback-spring.xml}:</p>
 * <pre>
 * &lt;conversionRule conversionWord="pii"
 *                 converterClass="com.kitehub.shared.logging.PIIScrubberConverter"/&gt;
 * &lt;encoder&gt;
 *   &lt;pattern&gt;%d %-5level [%thread] %logger{36} - %pii%n&lt;/pattern&gt;
 * &lt;/encoder&gt;
 * </pre>
 *
 * <p>For JSON output via {@code LogstashEncoder}, the encoder's message field is
 * scrubbed via the {@code messagePattern} extension or via a structured-arg path
 * (see {@code logging-standard.md}). This converter remains the text-pattern fallback.</p>
 */
public class PIIScrubberConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return PIIScrubber.scrub(event.getFormattedMessage());
    }
}
