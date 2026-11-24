package name.ulbricht.locale.explorer.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// A month laid out as weeks, following the week rules of a locale.
///
/// @param title    the localized month and year, for example `November 2026`
/// @param weekdays the narrow weekday names, starting with the first day of the week
/// @param weeks    the weeks of the month
public record MonthSheet(String title, List<String> weekdays, List<Week> weeks) {

    /// One row of the sheet.
    ///
    /// @param number the week of the week-based year, as a localized string
    /// @param days   seven days; days of the neighboring months are included
    public record Week(String number, List<Day> days) {
    }

    /// One day of the sheet.
    ///
    /// @param date         the date
    /// @param text         the day of month with the locale's digits
    /// @param inMonth      whether the day belongs to the month
    public record Day(LocalDate date, String text, boolean inMonth) {
    }

    /// Lays out a month for a locale.
    ///
    /// @param month  the month
    /// @param locale the locale; `-u-fw` and `-u-nu` are respected. The sheet is always
    ///               Gregorian, so a calendar extension such as `-u-ca-islamic` is ignored.
    /// @return the sheet
    public static MonthSheet of(YearMonth month, Locale locale) {
        var fields = WeekFields.of(locale);
        var dayFormat = isoFormatter("d", locale);
        var weekFormat = isoFormatter("w", locale);
        var title = isoFormatter("LLLL y", locale).format(month.atDay(1));

        var first = month.atDay(1).with(TemporalAdjusters.previousOrSame(fields.getFirstDayOfWeek()));
        var weekdays = new ArrayList<String>();
        for (var i = 0; i < 7; i++) {
            weekdays.add(first.plusDays(i).getDayOfWeek().getDisplayName(TextStyle.NARROW_STANDALONE, locale));
        }

        var weeks = new ArrayList<Week>();
        var start = first;
        while (!start.isAfter(month.atEndOfMonth())) {
            var days = new ArrayList<Day>();
            for (var i = 0; i < 7; i++) {
                var date = start.plusDays(i);
                days.add(new Day(date, dayFormat.format(date), YearMonth.from(date).equals(month)));
            }
            // The week number belongs to the week, so take it from its first day.
            weeks.add(new Week(weekFormat.format(start), days));
            start = start.plusWeeks(1);
        }
        return new MonthSheet(title, weekdays, weeks);
    }

    private static DateTimeFormatter isoFormatter(String pattern, Locale locale) {
        return DateTimeFormatter.ofPattern(pattern).localizedBy(locale).withChronology(IsoChronology.INSTANCE);
    }
}
