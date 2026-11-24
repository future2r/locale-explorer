package name.ulbricht.locale.explorer.locale;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Currency;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Unicode locale extensions (`-u-…`) with suggested values and example rows that show their
/// effect.
///
/// All keys except `hc` are evaluated by the JDK. The hour cycle is offered anyway, to show
/// that `java.time` and `java.text` ignore it and keep the locale's own hour cycle.
public final class LocaleExtensions {

    /// The extension keys offered in the user interface, in display order.
    public static final List<String> KEYS = List.of("ca", "nu", "fw", "hc", "rg", "cu", "cf", "tz");

    private static final List<String> NUMBERING_SYSTEMS = List.of(
            "latn", "arab", "arabext", "beng", "deva", "fullwide", "gujr", "guru", "hanidec",
            "khmr", "knda", "laoo", "mlym", "mymr", "orya", "tamldec", "telu", "thai", "tibt");

    private static final List<String> TIME_ZONES = List.of(
            "utc", "deber", "gblon", "usnyc", "uslax", "brsao", "inccu", "cnsha", "jptyo", "ausyd");

    private LocaleExtensions() {
    }

    /// Returns suggested values for an extension key.
    ///
    /// Other values can be entered; the JDK ignores values it does not know.
    ///
    /// @param key one of [#KEYS]
    /// @return the suggestions
    public static List<String> suggestedValues(String key) {
        return switch (key) {
            case "ca" -> Chronology.getAvailableChronologies().stream()
                    .map(Chronology::getCalendarType)
                    .filter(type -> type != null)
                    .sorted()
                    .toList();
            case "nu" -> NUMBERING_SYSTEMS;
            case "fw" -> List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun");
            case "hc" -> List.of("h11", "h12", "h23", "h24");
            case "rg" -> Stream.of(Locale.getISOCountries())
                    .map(country -> country.toLowerCase(Locale.ROOT) + "zzzz")
                    .toList();
            case "cu" -> Currency.getAvailableCurrencies().stream()
                    .map(currency -> currency.getCurrencyCode().toLowerCase(Locale.ROOT))
                    .sorted()
                    .toList();
            case "cf" -> List.of("standard", "account");
            case "tz" -> TIME_ZONES;
            default -> List.of();
        };
    }

    /// Applies Unicode extension keywords to a locale.
    ///
    /// Keys with a blank value are removed; keys not in the map are kept as they are.
    ///
    /// @param base     the locale to start from
    /// @param keywords the keywords, key to type
    /// @return the resulting locale
    /// @throws IllformedLocaleException if a type is not well-formed
    public static Locale apply(Locale base, Map<String, String> keywords) {
        var builder = new Locale.Builder().setLocale(base);
        keywords.forEach((key, type) ->
                builder.setUnicodeLocaleKeyword(key, type == null || type.isBlank() ? null : type.strip()));
        return builder.build();
    }

    /// Returns rows that show the effect of extensions.
    ///
    /// @param dateTime the date-time to format
    /// @return the rows
    public static List<LocaleProperty> previewRows(ZonedDateTime dateTime) {
        return List.of(
                LocaleProperty.of("preview.dateFull",
                        locale -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).localizedBy(locale).format(dateTime),
                        locale -> JavaCode.withLocale(locale,
                                JavaCode.zonedDateTime(dateTime),
                                "DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)",
                                "        .localizedBy(locale)",
                                "        .format(zonedDateTime);")),
                LocaleProperty.of("preview.timeShort",
                        locale -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).localizedBy(locale).format(dateTime),
                        locale -> JavaCode.withLocale(locale,
                                JavaCode.zonedDateTime(dateTime),
                                "DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)",
                                "        .localizedBy(locale)",
                                "        .format(zonedDateTime);")),
                LocaleProperty.of("preview.firstDay",
                        locale -> WeekFields.of(locale).getFirstDayOfWeek().getDisplayName(TextStyle.FULL, locale),
                        locale -> JavaCode.withLocale(locale,
                                "WeekFields.of(locale).getFirstDayOfWeek()",
                                "        .getDisplayName(TextStyle.FULL, locale);")),
                LocaleProperty.of("preview.number",
                        locale -> NumberFormat.getInstance(locale).format(-1234567.891),
                        locale -> JavaCode.withLocale(locale,
                                "NumberFormat.getInstance(locale).format(-1234567.891);")),
                LocaleProperty.of("preview.currency",
                        locale -> NumberFormat.getCurrencyInstance(locale).format(-1234.5),
                        locale -> JavaCode.withLocale(locale,
                                "NumberFormat.getCurrencyInstance(locale).format(-1234.5);")));
    }
}
