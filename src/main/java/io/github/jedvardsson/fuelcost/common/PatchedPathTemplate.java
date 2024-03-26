package io.github.jedvardsson.fuelcost.common;

import com.google.api.pathtemplate.PathTemplate;
import com.google.common.collect.ImmutableMap;
import org.springframework.web.util.UriUtils;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wrapper around the google PathTemplate to patch
 * faulty percent encoding of URI paths (<a href="https://www.rfc-editor.org/rfc/rfc3986">RFC 3986</a>).
 * The google implementation encodes path segments using {@link java.net.URLDecoder#decode(String, String)}
 * which implements the so-called `application/x-www-form-urlencoded` encoding which
 * <ol>
 *     <li>encodes more characters needed: see rule <code>segment-nz</code> of RFC 3986</li>
 *     <li>encodes <code>+</code> instead of <code>%20</code></li>
 * </ol>
 * What is more, the google implementation incorrectly handles encoding/decoding of path wildcards (<code>**</code>).
 * Paths (in this context) should be encoding using rule <code>path-rootless</code> of RFC 3986.
 */
public final class PatchedPathTemplate {

    private final PathTemplate template;

    private final Map<String, Codec> codecs;

    // lazy. Not in equals and hash code
    private String stringValue;

    private enum Codec {
        PATH {
            @Override
            public String encode(String value) {
                return decode(value);
            }

            @Override
            public String decode(String value) {
                return Arrays.stream(value.split("/")).map(SEGMENT::decode).map(SEGMENT::encode).collect(Collectors.joining("/"));
            }
        },
        SEGMENT {
            @Override
            public String encode(String value) {
                return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
            }

            @Override
            public String decode(String value) {
                return UriUtils.decode(value, StandardCharsets.UTF_8);
            }
        };
        public abstract String encode(String value);

        public abstract String decode(String value);
    }


    private PatchedPathTemplate(String template) {
        this.template = PathTemplate.createWithoutUrlEncoding(template);
        codecs = this.template.vars().stream().collect(Collectors.toMap(Function.identity(), v -> {
            String subTemplate = this.template.subTemplate(v).toString();
            boolean isPath = subTemplate.equals("**") || subTemplate.contains("/");
            return isPath ? Codec.PATH : Codec.SEGMENT;
        }));
    }


    public static PatchedPathTemplate create(String template) {
        return new PatchedPathTemplate(template);
    }

    public String instantiate(Map<String, String> values) {
        return instantiateEncoded(encode(values));
    }

    public String instantiate(String... keysAndValues) {
        return instantiateEncoded(encode(keysAndValues));
    }

    private String instantiateEncoded(Map<String, String> values) {
        return template.instantiate(values);
    }

    public Map<String, String> parse(String path) {
        Map<String, String> match = match(path);
        if (match == null) {
            throw new IllegalArgumentException(new Formatter().format("path '%s' does not match template '%s'", path, template).toString());
        }
        return match;
    }

    public boolean matches(String path) {
        return match(path) != null;
    }

    @Nullable
    public Map<String, String> match(String path) {
        Map<String, String> match = template.match(path);
        if (match == null) {
            return null;
        }
        return decode(match);
    }

    private Map<String, String> decode(Map<String, String> values) {
        return values.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> decode(e.getKey(), e.getValue())));
    }

    private String decode(String variable, String value) {
        Codec codec = codecs.get(variable);
        if (codec != null) {
            return codec.decode(value);
        }
        return value;
    }

    private Map<String, String> encode(Map<String, String> values) {
        return values.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> encode(e.getKey(), e.getValue())));
    }

    private Map<String, String> encode(String... keysAndValues) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            builder.put(key, encode(key, value));
        }
        return builder.build();
    }

    private String encode(String variable, String value) {
        Codec codec = codecs.get(variable);
        if (codec != null) {
            return codec.encode(value);
        }
        return value;
    }

    public String getPattern() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatchedPathTemplate that = (PatchedPathTemplate) o;
        return template.equals(that.template) && codecs.equals(that.codecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template, codecs);
    }

    @Override
    public String toString() {
        // lazy. May run once per thread. See String#hashCode
        String s = stringValue;
        if (stringValue == null) {
            s = template.toString().replaceAll("=\\*", "");
            stringValue = s;
        }
        return s;
    }
}
