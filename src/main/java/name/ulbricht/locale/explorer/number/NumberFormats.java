package name.ulbricht.locale.explorer.number;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import name.ulbricht.locale.explorer.text.CodePoints;
import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Rows for the numbers category.
public final class NumberFormats {

    /// The kinds of formats offered for parsing.
    public enum Kind {
        NUMBER("NumberFormat.getNumberInstance(locale)"),
        INTEGER("NumberFormat.getIntegerInstance(locale)"),
        PERCENT("NumberFormat.getPercentInstance(locale)"),
        CURRENCY("NumberFormat.getCurrencyInstance(locale)"),
        COMPACT_SHORT("NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT)");

        private final String code;

        Kind(String code) {
            this.code = code;
        }

        /// Creates the format for a locale.
        ///
        /// @param locale the locale
        /// @return a new format
        public NumberFormat create(Locale locale) {
            return switch (this) {
                case NUMBER -> NumberFormat.getNumberInstance(locale);
                case INTEGER -> NumberFormat.getIntegerInstance(locale);
                case PERCENT -> NumberFormat.getPercentInstance(locale);
                case CURRENCY -> NumberFormat.getCurrencyInstance(locale);
                case COMPACT_SHORT -> NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT);
            };
        }

        /// Returns the Java expression that creates the format.
        ///
        /// @return the expression
        public String code() {
            return code;
        }
    }

    /// The values shown in the compact tab.
    public static final List<Long> COMPACT_VALUES = List.of(1L, 12L, 123L, 1234L, 12345L, 123456L,
            1234567L, 12345678L, 1200000000L, 1200000000000L, -1500L);

    /// Numbering systems offered in the digits tab, with the expected zero digit or `0` for
    /// algorithmic systems that have no zero digit.
    public static final Map<String, Integer> NUMBERING_SYSTEMS = orderedMap(
            "latn", 0x0030, "arab", 0x0660, "arabext", 0x06F0, "beng", 0x09E6, "deva", 0x0966,
            "fullwide", 0xFF10, "gujr", 0x0AE6, "guru", 0x0A66, "hanidec", 0x3007, "khmr", 0x17E0,
            "knda", 0x0CE6, "laoo", 0x0ED0, "mlym", 0x0D66, "mymr", 0x1040, "orya", 0x0B66,
            "tamldec", 0x0BE6, "telu", 0x0C66, "thai", 0x0E50, "tibt", 0x0F20, "hant", 0, "roman", 0);

    private NumberFormats() {
    }

    // --- Formats -----------------------------------------------------------------------------

    /// Returns the rows of the formats tab.
    ///
    /// @param value   the value to format
    /// @param options the options applied to each format
    /// @return the rows
    public static List<LocaleProperty> formatRows(Number value, NumberOptions options) {
        return List.of(
                formatRow("number.format.number", value, options, NumberFormat::getNumberInstance,
                        "NumberFormat.getNumberInstance(locale)"),
                // The integer instance keeps its own fraction digits (none); only the other options apply.
                formatRow("number.format.integer", value,
                        new NumberOptions(options.minimumIntegerDigits(), 0, 0, options.roundingMode(), options.grouping()),
                        NumberFormat::getIntegerInstance, "NumberFormat.getIntegerInstance(locale)"),
                formatRow("number.format.percent", value, options, NumberFormat::getPercentInstance,
                        "NumberFormat.getPercentInstance(locale)"),
                formatRow("number.format.scientific", value, options,
                        locale -> new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(locale)),
                        "new DecimalFormat(\"0.###E0\", DecimalFormatSymbols.getInstance(locale))"),
                formatRow("number.format.engineering", value, options,
                        locale -> new DecimalFormat("##0.###E0", DecimalFormatSymbols.getInstance(locale)),
                        "new DecimalFormat(\"##0.###E0\", DecimalFormatSymbols.getInstance(locale))"),
                formatRow("number.format.compactShort", value, options,
                        locale -> NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT),
                        "NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT)"));
    }

    private static LocaleProperty formatRow(String labelKey, Number value, NumberOptions options,
                                            Function<Locale, NumberFormat> factory, String factoryCode) {
        return LocaleProperty.of(labelKey,
                locale -> options.applyTo(factory.apply(locale)).format(value),
                locale -> JavaCode.withLocale(locale,
                        "var format = " + factoryCode + ";",
                        options.code(),
                        "format.format(" + NumberOptions.literal(value) + ");"));
    }

    // --- Symbols -----------------------------------------------------------------------------

    /// Returns the rows of the symbols tab.
    ///
    /// @return the rows
    public static List<LocaleProperty> symbolRows() {
        return List.of(
                symbol("getDecimalSeparator", symbols -> String.valueOf(symbols.getDecimalSeparator())),
                symbol("getGroupingSeparator", symbols -> String.valueOf(symbols.getGroupingSeparator())),
                symbol("getMonetaryDecimalSeparator", symbols -> String.valueOf(symbols.getMonetaryDecimalSeparator())),
                symbol("getMonetaryGroupingSeparator", symbols -> String.valueOf(symbols.getMonetaryGroupingSeparator())),
                symbol("getMinusSign", symbols -> String.valueOf(symbols.getMinusSign())),
                symbol("getPercent", symbols -> String.valueOf(symbols.getPercent())),
                symbol("getPerMill", symbols -> String.valueOf(symbols.getPerMill())),
                symbol("getExponentSeparator", DecimalFormatSymbols::getExponentSeparator),
                symbol("getInfinity", DecimalFormatSymbols::getInfinity),
                symbol("getNaN", DecimalFormatSymbols::getNaN),
                symbol("getZeroDigit", symbols -> String.valueOf(symbols.getZeroDigit())),
                symbol("getPatternSeparator", symbols -> String.valueOf(symbols.getPatternSeparator())),
                symbol("getCurrencySymbol", DecimalFormatSymbols::getCurrencySymbol),
                symbol("getInternationalCurrencySymbol", DecimalFormatSymbols::getInternationalCurrencySymbol));
    }

    private static LocaleProperty symbol(String method, Function<DecimalFormatSymbols, String> getter) {
        return LocaleProperty.literal(method + "()",
                locale -> {
                    var text = getter.apply(DecimalFormatSymbols.getInstance(locale));
                    return text + "   " + CodePoints.describe(text);
                },
                locale -> JavaCode.withLocale(locale, "DecimalFormatSymbols.getInstance(locale)." + method + "();"));
    }

    // --- Patterns ----------------------------------------------------------------------------

    /// Returns the rows of the patterns tab.
    ///
    /// @param value     the value to format with the custom pattern
    /// @param pattern   the custom pattern, or `null`
    /// @param localized `true` if the custom pattern uses the locale's symbols
    /// @return the rows
    /// @throws IllegalArgumentException if the custom pattern is invalid for a non-localized pattern
    public static List<LocaleProperty> patternRows(Number value, String pattern, boolean localized) {
        var rows = new ArrayList<LocaleProperty>();
        if (pattern != null && !pattern.isEmpty()) {
            if (!localized) {
                new DecimalFormat(pattern);
            }
            var apply = localized ? "applyLocalizedPattern" : "applyPattern";
            rows.add(LocaleProperty.literal(pattern,
                    locale -> customFormat(pattern, localized, locale).format(value),
                    locale -> JavaCode.withLocale(locale,
                            "var format = new DecimalFormat(\"\", DecimalFormatSymbols.getInstance(locale));",
                            "format." + apply + "(" + JavaCode.string(pattern) + ");",
                            "format.format(" + NumberOptions.literal(value) + ");")));
            var negated = negate(value);
            rows.add(LocaleProperty.literal(pattern + "  (" + negated + ")",
                    locale -> customFormat(pattern, localized, locale).format(negated),
                    locale -> JavaCode.withLocale(locale,
                            "var format = new DecimalFormat(\"\", DecimalFormatSymbols.getInstance(locale));",
                            "format." + apply + "(" + JavaCode.string(pattern) + ");",
                            "format.format(" + NumberOptions.literal(negated) + ");")));
        }
        for (var kind : List.of(Kind.NUMBER, Kind.INTEGER, Kind.PERCENT, Kind.CURRENCY)) {
            rows.add(LocaleProperty.of("number.pattern.toPattern", List.of(kind.name()),
                    locale -> ((DecimalFormat) kind.create(locale)).toPattern(),
                    locale -> JavaCode.withLocale(locale, "((DecimalFormat) " + kind.code() + ").toPattern();")));
            rows.add(LocaleProperty.of("number.pattern.toLocalizedPattern", List.of(kind.name()),
                    locale -> ((DecimalFormat) kind.create(locale)).toLocalizedPattern(),
                    locale -> JavaCode.withLocale(locale, "((DecimalFormat) " + kind.code() + ").toLocalizedPattern();")));
        }
        return rows;
    }

    private static DecimalFormat customFormat(String pattern, boolean localized, Locale locale) {
        var format = new DecimalFormat("", DecimalFormatSymbols.getInstance(locale));
        if (localized) {
            format.applyLocalizedPattern(pattern);
        } else {
            format.applyPattern(pattern);
        }
        return format;
    }

    private static Number negate(Number value) {
        return value instanceof BigDecimal decimal ? decimal.negate() : -value.doubleValue();
    }

    // --- Compact -----------------------------------------------------------------------------

    /// Returns the rows of the compact tab: each value in short and long style.
    ///
    /// @param maximumFractionDigits the maximum number of fraction digits
    /// @return the rows
    public static List<LocaleProperty> compactRows(int maximumFractionDigits) {
        return COMPACT_VALUES.stream()
                .map(value -> LocaleProperty.literal(String.valueOf(value),
                        locale -> compact(locale, NumberFormat.Style.SHORT, maximumFractionDigits).format(value)
                                + "   ·   " + compact(locale, NumberFormat.Style.LONG, maximumFractionDigits).format(value),
                        locale -> JavaCode.withLocale(locale,
                                "var format = NumberFormat.getCompactNumberInstance(locale, NumberFormat.Style.SHORT);",
                                "format.setMaximumFractionDigits(" + maximumFractionDigits + ");",
                                "format.format(" + value + "L);",
                                "// and the same with NumberFormat.Style.LONG")))
                .toList();
    }

    private static NumberFormat compact(Locale locale, NumberFormat.Style style, int maximumFractionDigits) {
        var format = NumberFormat.getCompactNumberInstance(locale, style);
        format.setMaximumFractionDigits(maximumFractionDigits);
        return format;
    }

    // --- Numbering systems -------------------------------------------------------------------

    /// Returns one row per numbering system: each locale with `-u-nu-…`.
    ///
    /// Rows are highlighted where the JDK ignores the numbering system.
    ///
    /// @param value the value to format
    /// @return the rows
    public static List<LocaleProperty> numberingRows(Number value) {
        return NUMBERING_SYSTEMS.entrySet().stream()
                .map(entry -> LocaleProperty.literal("-u-nu-" + entry.getKey(),
                                locale -> NumberFormat.getInstance(withNumbering(locale, entry.getKey())).format(value),
                                locale -> JavaCode.withLocale(withNumbering(locale, entry.getKey()),
                                        "NumberFormat.getInstance(locale).format(" + NumberOptions.literal(value) + ");",
                                        "DecimalFormatSymbols.getInstance(locale).getZeroDigit();"))
                        .withHighlight(locale -> !isSupported(locale, entry.getKey())))
                .toList();
    }

    /// Returns whether the JDK uses a numbering system for a locale.
    ///
    /// @param locale the locale
    /// @param system the numbering system, for example `arab`
    /// @return `true` if the zero digit is the one of the numbering system
    public static boolean isSupported(Locale locale, String system) {
        var expected = NUMBERING_SYSTEMS.getOrDefault(system, 0);
        return expected != 0 && DecimalFormatSymbols.getInstance(withNumbering(locale, system)).getZeroDigit() == expected;
    }

    private static Locale withNumbering(Locale locale, String system) {
        return new Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", system).build();
    }

    // --- Parsing -----------------------------------------------------------------------------

    /// Returns the rows of the parsing tab: the parsed value and the parse position.
    ///
    /// @param text        the text to parse
    /// @param kind        the format
    /// @param strict      whether the format parses strictly
    /// @param integerOnly whether only the integer part is parsed
    /// @return the rows
    public static List<LocaleProperty> parseRows(String text, Kind kind, boolean strict, boolean integerOnly) {
        Function<Locale, String> code = locale -> JavaCode.withLocale(locale,
                "var format = " + kind.code() + ";",
                "format.setStrict(" + strict + ");",
                "format.setParseIntegerOnly(" + integerOnly + ");",
                "var position = new ParsePosition(0);",
                "var number = format.parse(" + JavaCode.string(text) + ", position);",
                "position.getIndex();",
                "position.getErrorIndex();");
        return List.of(
                LocaleProperty.of("number.parse.result", locale -> {
                    var result = parse(text, kind, strict, integerOnly, locale);
                    return result.number() == null ? "–" : result.number() + "  (" + result.number().getClass().getSimpleName() + ")";
                }, code).withHighlight(locale -> parse(text, kind, strict, integerOnly, locale).partial()),
                LocaleProperty.of("number.parse.position", locale -> {
                    var result = parse(text, kind, strict, integerOnly, locale);
                    return "index " + result.index() + " · errorIndex " + result.errorIndex()
                            + (result.partial() ? " · \"" + text.substring(result.index()) + "\"" : "");
                }, code),
                LocaleProperty.of("number.parse.expected",
                        locale -> kind.create(locale).format(new BigDecimal("-1234.5")),
                        locale -> JavaCode.withLocale(locale, kind.code() + ".format(new BigDecimal(\"-1234.5\"));")));
    }

    /// The outcome of parsing.
    ///
    /// @param number     the parsed number, or `null`
    /// @param index      the index after the parsed text
    /// @param errorIndex the index of the error, or -1
    /// @param partial    `true` if only a part of the text was parsed
    public record ParseResult(Number number, int index, int errorIndex, boolean partial) {
    }

    /// Parses a text.
    ///
    /// @param text        the text
    /// @param kind        the format
    /// @param strict      whether the format parses strictly
    /// @param integerOnly whether only the integer part is parsed
    /// @param locale      the locale
    /// @return the outcome
    public static ParseResult parse(String text, Kind kind, boolean strict, boolean integerOnly, Locale locale) {
        var format = kind.create(locale);
        format.setStrict(strict);
        format.setParseIntegerOnly(integerOnly);
        var position = new ParsePosition(0);
        var number = format.parse(text, position);
        var partial = number != null && position.getIndex() < text.length();
        return new ParseResult(number, position.getIndex(), position.getErrorIndex(), partial);
    }

    private static Map<String, Integer> orderedMap(Object... keysAndValues) {
        var map = new java.util.LinkedHashMap<String, Integer>();
        for (var i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], (Integer) keysAndValues[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(map);
    }
}
