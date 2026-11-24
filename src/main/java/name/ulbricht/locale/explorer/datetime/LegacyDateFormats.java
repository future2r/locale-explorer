package name.ulbricht.locale.explorer.datetime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Compares `java.text` (DateFormat, SimpleDateFormat) with `java.time`.
public final class LegacyDateFormats {

    /// A date close to the turn of the year, where week-based years differ from calendar years.
    public static final LocalDate TURN_OF_YEAR = LocalDate.of(2026, 12, 29);

    private LegacyDateFormats() {
    }

    /// Returns the comparison rows.
    ///
    /// @param dateTime the date-time to format
    /// @return the rows
    public static List<LocaleProperty> rows(ZonedDateTime dateTime) {
        var rows = new ArrayList<LocaleProperty>();
        var date = Date.from(dateTime.toInstant());
        var zone = TimeZone.getTimeZone(dateTime.getZone());
        for (var style : DateTimeFormats.STYLES) {
            var legacyStyle = legacyStyle(style);
            rows.add(LocaleProperty.of("datetime.legacy.timeApi", List.of(style.name()),
                    locale -> DateTimeFormatter.ofLocalizedDate(style).localizedBy(locale).format(dateTime),
                    locale -> DateTimeFormats.code(dateTime, locale,
                            "DateTimeFormatter.ofLocalizedDate(FormatStyle." + style + ")",
                            "        .localizedBy(locale)",
                            "        .format(zonedDateTime);")));
            rows.add(LocaleProperty.of("datetime.legacy.textApi", List.of(style.name()),
                    locale -> {
                        var format = DateFormat.getDateInstance(legacyStyle, locale);
                        format.setTimeZone(zone);
                        return format.format(date);
                    },
                    locale -> DateTimeFormats.code(dateTime, locale,
                            "var format = DateFormat.getDateInstance(DateFormat." + style + ", locale);",
                            "format.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));",
                            "format.format(Date.from(zonedDateTime.toInstant()));")));
        }
        var turnOfYear = TURN_OF_YEAR.atStartOfDay(dateTime.getZone());
        rows.add(trap("u", "datetime.legacy.trap.u", dateTime));
        rows.add(trap("YYYY-MM-dd", "datetime.legacy.trap.weekYear", turnOfYear));
        rows.add(trap("yyyy-MM-dd", "datetime.legacy.trap.year", turnOfYear));
        return rows;
    }

    private static LocaleProperty trap(String pattern, String descriptionKey, ZonedDateTime dateTime) {
        var date = Date.from(dateTime.toInstant());
        var zone = TimeZone.getTimeZone(dateTime.getZone());
        return LocaleProperty.literal("\"" + pattern + "\"  " + dateTime.toLocalDate(), descriptionKey,
                locale -> {
                    var legacy = new SimpleDateFormat(pattern, locale);
                    legacy.setTimeZone(zone);
                    var modern = DateTimeFormatter.ofPattern(pattern).localizedBy(locale).format(dateTime);
                    return "java.time: " + modern + "   ·   java.text: " + legacy.format(date);
                },
                locale -> DateTimeFormats.code(dateTime, locale,
                        "DateTimeFormatter.ofPattern(" + JavaCode.string(pattern) + ").localizedBy(locale).format(zonedDateTime);",
                        "var legacy = new SimpleDateFormat(" + JavaCode.string(pattern) + ", locale);",
                        "legacy.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));",
                        "legacy.format(Date.from(zonedDateTime.toInstant()));"));
    }

    private static int legacyStyle(FormatStyle style) {
        return switch (style) {
            case SHORT -> DateFormat.SHORT;
            case MEDIUM -> DateFormat.MEDIUM;
            case LONG -> DateFormat.LONG;
            case FULL -> DateFormat.FULL;
        };
    }
}
