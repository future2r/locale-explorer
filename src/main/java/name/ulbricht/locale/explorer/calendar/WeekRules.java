package name.ulbricht.locale.explorer.calendar;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// The week rules of a locale and the week numbers they produce.
public final class WeekRules {

    private WeekRules() {
    }

    /// Returns the rows of the week rules tab.
    ///
    /// @param date the date for which week numbers are computed
    /// @return the rows
    public static List<LocaleProperty> rows(LocalDate date) {
        var dateCode = "var date = LocalDate.parse(" + JavaCode.string(date.toString()) + ");";
        return List.of(
                LocaleProperty.of("calendar.rule.firstDay",
                        locale -> WeekFields.of(locale).getFirstDayOfWeek().getDisplayName(TextStyle.FULL, locale),
                        locale -> JavaCode.withLocale(locale,
                                "WeekFields.of(locale).getFirstDayOfWeek()",
                                "        .getDisplayName(TextStyle.FULL, locale);")),
                LocaleProperty.of("calendar.rule.minimalDays",
                        locale -> String.valueOf(WeekFields.of(locale).getMinimalDaysInFirstWeek()),
                        locale -> JavaCode.withLocale(locale, "WeekFields.of(locale).getMinimalDaysInFirstWeek();")),
                LocaleProperty.of("calendar.rule.standard", WeekRules::standardName,
                        locale -> JavaCode.withLocale(locale,
                                "WeekFields.of(locale).equals(WeekFields.ISO);",
                                "WeekFields.of(locale).equals(WeekFields.SUNDAY_START);")),
                LocaleProperty.of("calendar.rule.weekOfWeekBasedYear",
                        locale -> String.valueOf(date.get(WeekFields.of(locale).weekOfWeekBasedYear())),
                        locale -> week(locale, dateCode, "weekOfWeekBasedYear()")),
                LocaleProperty.of("calendar.rule.weekBasedYear",
                        locale -> String.valueOf(date.get(WeekFields.of(locale).weekBasedYear())),
                        locale -> week(locale, dateCode, "weekBasedYear()"))
                        .withHighlight(locale -> date.get(WeekFields.of(locale).weekBasedYear()) != date.getYear()),
                LocaleProperty.of("calendar.rule.weekOfYear",
                        locale -> String.valueOf(date.get(WeekFields.of(locale).weekOfYear())),
                        locale -> week(locale, dateCode, "weekOfYear()")),
                LocaleProperty.of("calendar.rule.weekOfMonth",
                        locale -> String.valueOf(date.get(WeekFields.of(locale).weekOfMonth())),
                        locale -> week(locale, dateCode, "weekOfMonth()")),
                LocaleProperty.of("calendar.rule.localDayOfWeek",
                        locale -> String.valueOf(date.get(WeekFields.of(locale).dayOfWeek())),
                        locale -> week(locale, dateCode, "dayOfWeek()")),
                LocaleProperty.of("calendar.rule.extensions", WeekRules::extensions,
                        locale -> JavaCode.withLocale(locale,
                                "locale.getUnicodeLocaleType(\"fw\");",
                                "locale.getUnicodeLocaleType(\"rg\");")));
    }

    /// Returns the rows of the turn-of-year tab: one row per day around New Year.
    ///
    /// @param year     the year that ends
    /// @param uiLocale the locale for the weekday in the label
    /// @return the rows
    public static List<LocaleProperty> turnOfYearRows(int year, Locale uiLocale) {
        var first = LocalDate.of(year, 12, 26);
        return Stream.iterate(first, date -> date.plusDays(1)).limit(10)
                .map(date -> {
                    var dateCode = "var date = LocalDate.parse(" + JavaCode.string(date.toString()) + ");";
                    return LocaleProperty.literal(
                                    date + "  " + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, uiLocale),
                                    locale -> {
                                        var fields = WeekFields.of(locale);
                                        return date.get(fields.weekOfWeekBasedYear()) + " / " + date.get(fields.weekBasedYear());
                                    },
                                    locale -> JavaCode.withLocale(locale, dateCode,
                                            "var fields = WeekFields.of(locale);",
                                            "date.get(fields.weekOfWeekBasedYear());",
                                            "date.get(fields.weekBasedYear());"))
                            .withHighlight(locale -> date.get(WeekFields.of(locale).weekBasedYear()) != date.getYear());
                })
                .toList();
    }

    static String standardName(Locale locale) {
        var fields = WeekFields.of(locale);
        if (fields.equals(WeekFields.ISO)) {
            return "ISO";
        }
        if (fields.equals(WeekFields.SUNDAY_START)) {
            return "SUNDAY_START";
        }
        return "";
    }

    private static String extensions(Locale locale) {
        return Stream.of("fw", "rg")
                .filter(key -> locale.getUnicodeLocaleType(key) != null)
                .map(key -> key + "-" + locale.getUnicodeLocaleType(key))
                .collect(Collectors.joining(" · "));
    }

    private static String week(Locale locale, String dateCode, String field) {
        return JavaCode.withLocale(locale, dateCode, "date.get(WeekFields.of(locale)." + field + ");");
    }
}
