package com.trainingsplan.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateAsNoonUtc() {
        return builder -> {
            builder.serializerByType(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider prov) throws IOException {
                    gen.writeString(value.atTime(12, 0).atOffset(ZoneOffset.UTC).toInstant().toString());
                }
            });
            builder.deserializerByType(LocalDate.class, new JsonDeserializer<LocalDate>() {
                @Override
                public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                    String text = p.getText();
                    if (text.contains("T")) {
                        return Instant.parse(text).atOffset(ZoneOffset.UTC).toLocalDate();
                    }
                    return LocalDate.parse(text);
                }
            });
        };
    }
}
