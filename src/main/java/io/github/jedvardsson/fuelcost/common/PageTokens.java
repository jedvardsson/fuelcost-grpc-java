package io.github.jedvardsson.fuelcost.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

public class PageTokens {

    private static final ObjectMapper MAPPER = SmileMapper.builder()
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .addModule(new ParameterNamesModule())
            .build();

    public static <T> Optional<T> parseOptional(String s, Class<T> type) {
        try {
            if (s == null || s.isEmpty()) {
                return Optional.empty();
            }
            byte[] bytes = Base64.getUrlDecoder().decode(s);
            return Optional.of(MAPPER.readValue(bytes, type));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid page token: " + s, e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String format(Object token) {
        try {
            byte[] bytes = MAPPER.writeValueAsBytes(token);
            return Base64.getUrlEncoder().encodeToString(bytes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
