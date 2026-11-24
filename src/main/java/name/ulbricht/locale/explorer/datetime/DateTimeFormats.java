package name.ulbricht.locale.explorer.datetime;

import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Rows for localized format styles, skeletons and patterns.
public final class DateTimeFormats {

    /// The format styles from short to full.
    public static final List<FormatStyle> STYLES =
            List.of(FormatStyle.SHORT, FormatStyle.MEDIUM, FormatStyle.LONG, FormatStyle.FULL);

    /// A pattern letter combination for the reference table.
    ///
    /// @param pattern        the pattern
    /// @param descriptionKey the message key of its explanation
    public record Letter(String pattern, String descriptionKey) {
    }

    /// The pattern letters shown in the reference table.
    public static final List<Letter> LETTERS = List.of(
            new Letter("G", "letter.era"), new Letter("GGGG", "letter.era"),
            new Letter("u", "letter.year"), new Letter("y", "letter.yearOfEra"),
            new Letter("Y", "letter.weekBasedYear"),
            new Letter("QQQ", "letter.quarter"), new Letter("QQQQ", "letter.quarter"),
            new Letter("M", "letter.month"), new Letter("MMM", "letter.month"), new Letter("MMMM", "letter.month"),
            new Letter("L", "letter.monthStandalone"), new Letter("LLL", "letter.monthStandalone"),
            new Letter("LLLL", "letter.monthStandalone"),
            new Letter("w", "letter.weekOfYear"), new Letter("W", "letter.weekOfMonth"),
            new Letter("D", "letter.dayOfYear"),
            new Letter("E", "letter.dayOfWeek"), new Letter("EEEE", "letter.dayOfWeek"),
            new Letter("e", "letter.localDayOfWeek"), new Letter("c", "letter.localDayOfWeekStandalone"),
            new Letter("a", "letter.amPm"), new Letter("B", "letter.dayPeriod"), new Letter("BBBB", "letter.dayPeriod"),
            new Letter("h", "letter.hour12"), new Letter("H", "letter.hour24"),
            new Letter("z", "letter.zoneName"), new Letter("zzzz", "letter.zoneName"),
            new Letter("v", "letter.genericZoneName"), new Letter("vvvv", "letter.genericZoneName"),
            new Letter("O", "letter.localizedOffset"), new Letter("OOOO", "letter.localizedOffset"),
            new Letter("VV", "letter.zoneId"), new Letter("XXX", "letter.isoOffset"));

    private DateTimeFormats() {
    }

    /// Rows for the localized date and time styles.
    ///
    /// @param dateTime     the date-time to format
    /// @param kind         the type the date-time is converted to before formatting
    /// @param matrix       `true` for all combinations of date and time style, `false` for date
    ///                     and time separately
    /// @param withPatterns `true` to add a row with the pattern after each style
    /// @return the rows
    public static List<LocaleProperty> styleRows(ZonedDateTime dateTime, TemporalKind kind, boolean matrix,
                                                 boolean withPatterns) {
        var rows = new ArrayList<LocaleProperty>();
        if (matrix) {
            for (var dateStyle : STYLES) {
                for (var timeStyle : STYLES) {
                    addStyle(rows, dateTime, kind, dateStyle, timeStyle, withPatterns);
                }
            }
        } else {
            for (var dateStyle : STYLES) {
                addStyle(rows, dateTime, kind, dateStyle, null, withPatterns);
            }
            for (var timeStyle : STYLES) {
                addStyle(rows, dateTime, kind, null, timeStyle, withPatterns);
            }
        }
        return rows;
    }

    private static void addStyle(List<LocaleProperty> rows, ZonedDateTime dateTime, TemporalKind kind,
                                 FormatStyle dateStyle, FormatStyle timeStyle, boolean withPatterns) {
        String labelKey;
        List<String> args;
        String factory;
        if (dateStyle != null && timeStyle != null) {
            labelKey = "datetime.row.dateTime";
            args = List.of(dateStyle.name(), timeStyle.name());
            factory = "ofLocalizedDateTime(FormatStyle." + dateStyle + ", FormatStyle." + timeStyle + ")";
        } else if (dateStyle != null) {
            labelKey = "datetime.row.date";
            args = List.of(dateStyle.name());
            factory = "ofLocalizedDate(FormatStyle." + dateStyle + ")";
        } else {
            labelKey = "datetime.row.time";
            args = List.of(timeStyle.name());
            factory = "ofLocalizedTime(FormatStyle." + timeStyle + ")";
        }
        var formatter = dateStyle != null && timeStyle != null
                ? DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle)
                : dateStyle != null ? DateTimeFormatter.ofLocalizedDate(dateStyle) : DateTimeFormatter.ofLocalizedTime(timeStyle);
        var temporal = kind.from(dateTime);
        rows.add(LocaleProperty.of(labelKey, args,
                locale -> formatter.localizedBy(locale).format(temporal),
                locale -> code(dateTime, locale,
                        "DateTimeFormatter." + factory,
                        "        .localizedBy(locale)",
                        "        .format(" + kind.expression() + ");")));
        if (withPatterns) {
            var styleArgs = (dateStyle == null ? "null" : "FormatStyle." + dateStyle) + ", "
                    + (timeStyle == null ? "null" : "FormatStyle." + timeStyle);
            rows.add(LocaleProperty.of("datetime.row.pattern",
                    locale -> DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                            dateStyle, timeStyle, Chronology.ofLocale(locale), locale),
                    locale -> JavaCode.withLocale(locale,
                            "DateTimeFormatterBuilder.getLocalizedDateTimePattern(",
                            "        " + styleArgs + ", Chronology.ofLocale(locale), locale);")));
        }
    }

    /// Rows for localized skeletons, each followed by the resulting pattern.
    ///
    /// @param dateTime  the date-time to format
    /// @param skeletons the skeletons, for example `yMMMEd`
    /// @return the rows
    /// @see DateTimeFormatter#ofLocalizedPattern(String)
    public static List<LocaleProperty> skeletonRows(ZonedDateTime dateTime, List<String> skeletons) {
        var rows = new ArrayList<LocaleProperty>();
        for (var skeleton : skeletons) {
            rows.add(LocaleProperty.literal(skeleton,
                    locale -> DateTimeFormatter.ofLocalizedPattern(skeleton).localizedBy(locale).format(dateTime),
                    locale -> code(dateTime, locale,
                            "DateTimeFormatter.ofLocalizedPattern(" + JavaCode.string(skeleton) + ")",
                            "        .localizedBy(locale)",
                            "        .format(zonedDateTime);")));
            rows.add(LocaleProperty.of("datetime.row.pattern",
                    locale -> DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                            skeleton, Chronology.ofLocale(locale), locale),
                    locale -> JavaCode.withLocale(locale,
                            "DateTimeFormatterBuilder.getLocalizedDateTimePattern(",
                            "        " + JavaCode.string(skeleton) + ", Chronology.ofLocale(locale), locale);")));
        }
        return rows;
    }

    /// Rows for a custom pattern followed by the pattern letter reference.
    ///
    /// @param dateTime the date-time to format
    /// @param pattern  the custom pattern, or `null` to show only the reference
    /// @return the rows
    /// @see DateTimeFormatter#ofPattern(String)
    public static List<LocaleProperty> patternRows(ZonedDateTime dateTime, String pattern) {
        var rows = new ArrayList<LocaleProperty>();
        if (pattern != null && !pattern.isEmpty()) {
            rows.add(patternRow(dateTime, pattern, null));
        }
        for (var letter : LETTERS) {
            rows.add(patternRow(dateTime, letter.pattern(), letter.descriptionKey()));
        }
        return rows;
    }

    private static LocaleProperty patternRow(ZonedDateTime dateTime, String pattern, String descriptionKey) {
        var formatter = DateTimeFormatter.ofPattern(pattern);
        return LocaleProperty.literal(pattern, descriptionKey,
                locale -> formatter.localizedBy(locale).format(dateTime),
                locale -> code(dateTime, locale,
                        "DateTimeFormatter.ofPattern(" + JavaCode.string(pattern) + ")",
                        "        .localizedBy(locale)",
                        "        .format(zonedDateTime);"));
    }

    static String code(ZonedDateTime dateTime, Locale locale, String... lines) {
        var all = new ArrayList<String>();
        all.add(JavaCode.zonedDateTime(dateTime));
        all.addAll(List.of(lines));
        return JavaCode.withLocale(locale, all.toArray(String[]::new));
    }
}
