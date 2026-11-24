package name.ulbricht.locale.explorer.lists;

import java.text.ListFormat;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Rows for the "Lists, text" category: list formats, message formats, case mapping and
/// `String.format`.
public final class TextFormats {

    /// The locales used for case mapping unless the toolbar locales are chosen.
    public static final List<Locale> CASE_LOCALES = Stream.of("und", "de", "tr", "az", "lt", "el", "nl")
            .map(Locale::forLanguageTag).toList();

    /// The patterns always shown in the `String.format` tab.
    public static final List<String> FORMAT_PATTERNS = List.of(
            "%1$,.2f", "%1$.3e", "%1$(,.2f", "%1$+,15.2f|", "%2$tA", "%2$tB", "%2$tc", "%2$tF %2$tT",
            "%3$S", "%4$,d", "%4$08d");

    private TextFormats() {
    }

    /// Splits user input into list items.
    ///
    /// @param text items separated by `|`
    /// @return the trimmed, non-empty items
    public static List<String> items(String text) {
        return Arrays.stream(text.split("\\|")).map(String::strip).filter(item -> !item.isEmpty()).toList();
    }

    // --- ListFormat --------------------------------------------------------------------------

    /// Returns one row per combination of list type and style.
    ///
    /// A value is highlighted if [ListFormat#parse(String)] does not return the original items.
    ///
    /// @param items the items
    /// @return the rows; empty if there are no items, which [ListFormat#format(List)] rejects
    public static List<LocaleProperty> listRows(List<String> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        var rows = new ArrayList<LocaleProperty>();
        var itemsCode = items.stream().map(JavaCode::string).collect(Collectors.joining(", ", "List.of(", ")"));
        for (var type : ListFormat.Type.values()) {
            for (var style : ListFormat.Style.values()) {
                rows.add(LocaleProperty.of("list.row", List.of(type.name(), style.name()),
                                locale -> ListFormat.getInstance(locale, type, style).format(items),
                                locale -> JavaCode.withLocale(locale,
                                        "var format = ListFormat.getInstance(locale, ListFormat.Type." + type
                                                + ", ListFormat.Style." + style + ");",
                                        "var text = format.format(" + itemsCode + ");",
                                        "format.parse(text);"))
                        .withHighlight(locale -> !roundTrips(ListFormat.getInstance(locale, type, style), items)));
            }
        }
        return rows;
    }

    private static boolean roundTrips(ListFormat format, List<String> items) {
        try {
            return format.parse(format.format(items)).equals(items);
        } catch (java.text.ParseException | RuntimeException _) {
            return false;
        }
    }

    // --- MessageFormat -----------------------------------------------------------------------

    /// Returns the rows for a message pattern.
    ///
    /// @param pattern  the pattern
    /// @param name     argument 0
    /// @param dateTime argument 1, passed as [Date]
    /// @param count    argument 2
    /// @return the rows
    /// @throws IllegalArgumentException if the pattern is invalid
    public static List<LocaleProperty> messageRows(String pattern, String name, ZonedDateTime dateTime, long count) {
        new MessageFormat(pattern);
        var date = Date.from(dateTime.toInstant());
        Object[] arguments = {name, date, count};
        var code = "new MessageFormat(" + JavaCode.string(pattern) + ", locale)";
        var argumentsCode = "new Object[] {" + JavaCode.string(name) + ", Date.from(Instant.parse("
                + JavaCode.string(dateTime.toInstant().toString()) + ")), " + count + "L}";
        return List.of(
                LocaleProperty.of("message.result",
                        locale -> new MessageFormat(pattern, locale).format(arguments),
                        locale -> JavaCode.withLocale(locale, code + ".format(" + argumentsCode + ");")),
                LocaleProperty.of("message.toPattern",
                        locale -> new MessageFormat(pattern, locale).toPattern(),
                        locale -> JavaCode.withLocale(locale, code + ".toPattern();")),
                LocaleProperty.of("message.formats",
                        locale -> Arrays.stream(new MessageFormat(pattern, locale).getFormatsByArgumentIndex())
                                .map(format -> format == null ? "–" : format.getClass().getSimpleName())
                                .collect(Collectors.joining(" · ")),
                        locale -> JavaCode.withLocale(locale, code + ".getFormatsByArgumentIndex();")));
    }

    // --- Case mapping ------------------------------------------------------------------------

    /// Returns two rows per word: upper case and lower case.
    ///
    /// Values that differ from [Locale#ROOT] are highlighted; a changed length is shown in
    /// brackets.
    ///
    /// @param words the words
    /// @return the rows
    public static List<LocaleProperty> caseRows(List<String> words) {
        var rows = new ArrayList<LocaleProperty>();
        for (var word : words) {
            rows.add(caseRow(word, true));
            rows.add(caseRow(word, false));
        }
        return rows;
    }

    private static LocaleProperty caseRow(String word, boolean upper) {
        var method = upper ? "toUpperCase" : "toLowerCase";
        return LocaleProperty.literal(word + "  ·  " + method,
                        locale -> withLength(word, map(word, upper, locale)),
                        locale -> JavaCode.withLocale(locale, JavaCode.string(word) + "." + method + "(locale);"))
                .withHighlight(locale -> !map(word, upper, locale).equals(map(word, upper, Locale.ROOT)));
    }

    private static String map(String word, boolean upper, Locale locale) {
        return upper ? word.toUpperCase(locale) : word.toLowerCase(locale);
    }

    private static String withLength(String original, String mapped) {
        return mapped.length() == original.length()
                ? mapped
                : mapped + "   [" + original.length() + " → " + mapped.length() + "]";
    }

    // --- String.format -----------------------------------------------------------------------

    /// Largest width or precision accepted in a format string, see [#formatError].
    public static final int MAX_FORMAT_SIZE = 1000;

    /// A format specifier with optional argument index, flags, width and precision.
    private static final Pattern FORMAT_SPECIFIER =
            Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(<]*(\\d+)?(?:\\.(\\d+))?[a-zA-Z%]");

    /// Checks a format string against the arguments used by [#formatRows].
    ///
    /// @param pattern  the format string
    /// @param value    argument 1, a floating-point number
    /// @param dateTime argument 2
    /// @param text     argument 3
    /// @param number   argument 4, an integer
    /// @return the error message, or empty if the format string is valid for these arguments
    public static Optional<String> formatError(String pattern, double value, ZonedDateTime dateTime,
                                               String text, long number) {
        // A width or precision such as %.999999999f would build a giant string or run out of memory.
        var specifier = FORMAT_SPECIFIER.matcher(pattern);
        while (specifier.find()) {
            for (var group : List.of(1, 2)) {
                var digits = specifier.group(group);
                // leading zeros do not make a number larger: %.0001f has precision 1
                var significant = digits == null ? null : digits.replaceFirst("^0+(?=.)", "");
                if (significant != null && (significant.length() > 4 || Integer.parseInt(significant) > MAX_FORMAT_SIZE)) {
                    return Optional.of("width or precision above " + MAX_FORMAT_SIZE + ": " + specifier.group());
                }
            }
        }
        try {
            String.format(Locale.ROOT, pattern, value, dateTime, text, number);
            return Optional.empty();
        } catch (IllegalFormatException e) {
            return Optional.of(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        }
    }

    /// Returns one row per pattern for `String.format(locale, pattern, value, dateTime, text, number)`.
    ///
    /// @param patterns the patterns
    /// @param value    argument 1, a floating-point number
    /// @param dateTime argument 2
    /// @param text     argument 3
    /// @param number   argument 4, an integer
    /// @return the rows
    public static List<LocaleProperty> formatRows(List<String> patterns, double value, ZonedDateTime dateTime,
                                                  String text, long number) {
        Object[] arguments = {value, dateTime, text, number};
        var argumentsCode = value + ", zonedDateTime, " + JavaCode.string(text) + ", " + number + "L";
        return patterns.stream()
                .map(pattern -> LocaleProperty.literal(pattern,
                        locale -> String.format(locale, pattern, arguments),
                        locale -> JavaCode.withLocale(locale,
                                JavaCode.zonedDateTime(dateTime),
                                "String.format(locale, " + JavaCode.string(pattern) + ", " + argumentsCode + ");")))
                .toList();
    }
}
