package la.moony.douban.sqlite;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

final class InstantUtils {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
        .appendFraction(ChronoField.NANO_OF_SECOND, 9, 9, true)
        .appendOffset("+HH:MM", "Z")
        .toFormatter()
        .withZone(ZoneOffset.UTC);

    private InstantUtils() {
    }

    static String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return FORMATTER.format(instant);
    }

    static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
