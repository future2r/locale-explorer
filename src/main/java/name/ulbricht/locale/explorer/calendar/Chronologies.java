package name.ulbricht.locale.explorer.calendar;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// The calendar systems of `java.time`.
public final class Chronologies {

    private record Entry(Chronology chronology, String code) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry(IsoChronology.INSTANCE, "IsoChronology.INSTANCE"),
            new Entry(JapaneseChronology.INSTANCE, "JapaneseChronology.INSTANCE"),
            new Entry(ThaiBuddhistChronology.INSTANCE, "ThaiBuddhistChronology.INSTANCE"),
            new Entry(HijrahChronology.INSTANCE, "HijrahChronology.INSTANCE"),
            new Entry(MinguoChronology.INSTANCE, "MinguoChronology.INSTANCE"));

    private Chronologies() {
    }

    /// Returns two rows per chronology: the date in full style and its era, year and lengths.
    ///
    /// A date outside the range of a chronology (for example before Meiji 6 or outside the
    /// Hijrah tables) only turns the cells of that chronology into errors.
    ///
    /// @param date the date to convert
    /// @return the rows
    public static List<LocaleProperty> rows(LocalDate date) {
        var rows = new ArrayList<LocaleProperty>();
        var dateCode = "var date = LocalDate.parse(" + JavaCode.string(date.toString()) + ");";
        for (var entry : ENTRIES) {
            var chronology = entry.chronology();
            var label = chronology.getId() + (chronology.getCalendarType() == null ? "" : "  (" + chronology.getCalendarType() + ")");
            rows.add(LocaleProperty.literal(label,
                    locale -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                            .localizedBy(locale).withChronology(chronology).format(date),
                    locale -> JavaCode.withLocale(locale, dateCode,
                            "DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)",
                            "        .localizedBy(locale)",
                            "        .withChronology(" + entry.code() + ")",
                            "        .format(date);")));
            rows.add(LocaleProperty.of("calendar.chrono.info",
                    locale -> describe(chronology.date(date), locale),
                    locale -> JavaCode.withLocale(locale, dateCode,
                            "var chronoDate = " + entry.code() + ".date(date);",
                            "chronoDate.getEra().getDisplayName(TextStyle.FULL, locale);",
                            "chronoDate.get(ChronoField.YEAR_OF_ERA);",
                            "chronoDate.lengthOfMonth();",
                            "chronoDate.lengthOfYear();")));
        }
        return rows;
    }

    private static String describe(ChronoLocalDate date, java.util.Locale locale) {
        return date.getEra().getDisplayName(TextStyle.FULL, locale)
                + " · " + date.get(ChronoField.YEAR_OF_ERA)
                + " · " + date.lengthOfMonth()
                + " · " + date.lengthOfYear();
    }
}
