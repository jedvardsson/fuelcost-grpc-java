package io.github.jedvardsson.fuelcost.common;

import java.util.function.Function;

public final class Arguments {

    private Arguments() {
    }

    public static long parseAccountId(String value, String field) {
        return parse(value, field, Long::parseLong);
    }

    public static long parseVehicleId(String value, String field) {
        return parse(value, field, Long::parseLong);
    }

    public static <V, T> T parse(V value, String field, Function<? super V, T> parser) {
        if (value == null || (value instanceof String s && s.isEmpty())) {
            throw new IllegalArgumentException("required field: " + field);
        }
        try {
            return parser.apply(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid field %s: %s".formatted(field, value));
        }
    }

    public static String requireNonEmpty(String s, String field) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException(field + ": must not be empty");
        }
        return s;
    }
}
