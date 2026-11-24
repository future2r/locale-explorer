package name.ulbricht.locale.explorer.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Parses text with a localized formatter.
public final class DateTimeParsing {

    /// The formatters offered for parsing.
    ///
    /// @param labelKey  the message key of the name
    /// @param labelArgs arguments for the name
    /// @param formatter the formatter, not yet localized
    /// @param code      the Java expression that creates the formatter
    public record Parser(String labelKey, List<String> labelArgs, DateTimeFormatter formatter, String code) {

        /// Returns the parsers for the localized styles and ISO formats.
        ///
        /// @return the parsers
        public static List<Parser> standard() {
            var parsers = new java.util.ArrayList<Parser>();
            for (var style : DateTimeFormats.STYLES) {
                parsers.add(new Parser("datetime.row.date", List.of(style.name()),
                        DateTimeFormatter.ofLocalizedDate(style), "DateTimeFormatter.ofLocalizedDate(FormatStyle." + style + ")"));
            }
            for (var style : List.of(FormatStyle.SHORT, FormatStyle.MEDIUM)) {
                parsers.add(new Parser("datetime.row.time", List.of(style.name()),
                        DateTimeFormatter.ofLocalizedTime(style), "DateTimeFormatter.ofLocalizedTime(FormatStyle." + style + ")"));
            }
            parsers.add(new Parser("datetime.row.dateTime", List.of("SHORT", "SHORT"),
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT),
                    "DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)"));
            parsers.add(new Parser("ISO_LOCAL_DATE", List.of(), DateTimeFormatter.ISO_LOCAL_DATE, "DateTimeFormatter.ISO_LOCAL_DATE"));
            parsers.add(new Parser("ISO_DATE_TIME", List.of(), DateTimeFormatter.ISO_DATE_TIME, "DateTimeFormatter.ISO_DATE_TIME"));
            return parsers;
        }

        /// Creates a parser for a pattern.
        ///
        /// @param pattern the pattern
        /// @return the parser
        public static Parser ofPattern(String pattern) {
            return new Parser("datetime.parse.ownPattern", List.of(pattern), DateTimeFormatter.ofPattern(pattern),
                    "DateTimeFormatter.ofPattern(" + JavaCode.string(pattern) + ")");
        }

        /// Returns `true` if the label is a constant name rather than a message key.
        ///
        /// @return whether the label is literal
        public boolean literal() {
            return labelKey.startsWith("ISO_");
        }
    }

    private DateTimeParsing() {
    }

    /// Returns the rows for parsing a text: the parsed value and an example of the expected
    /// format.
    ///
    /// @param text     the text to parse
    /// @param parser   the formatter
    /// @param resolver the resolver style
    /// @param example  the date-time used for the example
    /// @return the rows
    public static List<LocaleProperty> rows(String text, Parser parser, ResolverStyle resolver, ZonedDateTime example) {
        return List.of(
                LocaleProperty.of("datetime.parse.result",
                        locale -> describe(parse(text, parser, resolver, locale)),
                        locale -> JavaCode.withLocale(locale,
                                parser.code(),
                                "        .localizedBy(locale)",
                                "        .withResolverStyle(ResolverStyle." + resolver + ")",
                                "        .parseBest(" + JavaCode.string(text) + ",",
                                "                ZonedDateTime::from, OffsetDateTime::from, LocalDateTime::from,",
                                "                LocalDate::from, LocalTime::from, YearMonth::from);")),
                LocaleProperty.of("datetime.parse.expected",
                        locale -> parser.formatter().localizedBy(locale).format(example),
                        locale -> DateTimeFormats.code(example, locale,
                                parser.code(),
                                "        .localizedBy(locale)",
                                "        .format(zonedDateTime);")));
    }

    /// Parses a text with the best matching `java.time` type.
    ///
    /// @param text     the text
    /// @param parser   the formatter
    /// @param resolver the resolver style
    /// @param locale   the locale
    /// @return the parsed value
    /// @throws java.time.format.DateTimeParseException if the text cannot be parsed
    public static TemporalAccessor parse(String text, Parser parser, ResolverStyle resolver, Locale locale) {
        return parser.formatter().localizedBy(locale).withResolverStyle(resolver).parseBest(text,
                ZonedDateTime::from, OffsetDateTime::from, LocalDateTime::from,
                LocalDate::from, LocalTime::from, YearMonth::from);
    }

    private static String describe(TemporalAccessor value) {
        return value + "  (" + value.getClass().getSimpleName() + ")";
    }
}
