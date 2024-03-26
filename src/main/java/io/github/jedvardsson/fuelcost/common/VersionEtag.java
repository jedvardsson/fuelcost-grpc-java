package io.github.jedvardsson.fuelcost.common;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionEtag {

    private final long version;

    private VersionEtag(long value) {
        this.version = value;
    }

    public static VersionEtag of(long version) {
        return new VersionEtag(version);
    }

    public static Optional<Long> parseOptionalVersion(String etag) {
        return parseOptional(etag).map(VersionEtag::version);
    }

    @Nullable
    public static Long tryParseVersion(String etag) {
        if (etag == null || etag.isEmpty()) {
            return null;
        }
        Matcher matcher = WEAK_ETAG_PATTERN.matcher(etag);
        if (matcher.matches()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                // fall-through
            }
        }
        return -1L;
    }

    public long version() {
        return version;
    }

    public VersionEtag increment() {
        return new VersionEtag(version + 1);
    }

    private static final Pattern WEAK_ETAG_PATTERN = Pattern.compile("^W/\"(\\d+)\"$");

    public static VersionEtag parse(String etag) {
        Matcher matcher = WEAK_ETAG_PATTERN.matcher(etag);
        if (matcher.matches()) {
            try {
                return VersionEtag.of(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException e) {
                // fall-through
            }
        }
        throw new IllegalArgumentException("Invalid or missing etag: " + etag);
    }

    public static Optional<VersionEtag> parseOptional(String etag) {
        return etag == null || etag.isEmpty() ? Optional.empty() : Optional.of(parse(etag));
    }

    public static String format(long version) {
        return of(version).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionEtag version = (VersionEtag) o;
        return this.version == version.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return "W/\"" + version + "\"";
    }
}
