package com.kitehub.shared.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Jackson serializer for {@link Redact}-annotated fields. Replaces the value with
 * {@code "<REDACTED>"} regardless of type.
 */
public class RedactSerializer extends StdSerializer<Object> {

    private static final String REDACTED = "<REDACTED>";

    public RedactSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(REDACTED);
    }
}
