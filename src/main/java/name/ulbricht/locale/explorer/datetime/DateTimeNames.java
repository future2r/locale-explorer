package name.ulbricht.locale.explorer.datetime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import name.ulbricht.locale.explorer.text.JavaCode;

/// Localized names of months, days of week, eras, AM/PM markers and quarters.
public final class DateTimeNames {

    /// The text styles shown as columns, each followed by its standalone form.
    public static final List<TextStyle> STYLES = List.of(
            TextStyle.FULL, TextStyle.FULL_STANDALONE,
            TextStyle.SHORT, TextStyle.SHORT_STANDALONE,
            TextStyle.NARROW, TextStyle.NARROW_STANDALONE);

    /// The kinds of names.
    public enum Kind {
        MONTHS(ChronoField.MONTH_OF_YEAR, "ChronoField.MONTH_OF_YEAR"),
        DAYS_OF_WEEK(ChronoField.DAY_OF_WEEK, "ChronoField.DAY_OF_WEEK"),
        ERAS(ChronoField.ERA, "ChronoField.ERA"),
        AM_PM(ChronoField.AMPM_OF_DAY, "ChronoField.AMPM_OF_DAY"),
        QUARTERS(IsoFields.QUARTER_OF_YEAR, "IsoFields.QUARTER_OF_YEAR");

        private final TemporalField field;
        private final String fieldCode;

        Kind(TemporalField field, String fieldCode) {
            this.field = field;
            this.fieldCode = fieldCode;
        }

        /// Returns one sample value for each name.
        ///
        /// @return the samples in natural order
        public List<TemporalAccessor> samples() {
            return switch (this) {
                case MONTHS -> IntStream.rangeClosed(1, 12)
                        .mapToObj(month -> (TemporalAccessor) LocalDate.of(2026, month, 15)).toList();
                // 2026-11-23 is a Monday.
                case DAYS_OF_WEEK -> IntStream.range(0, 7)
                        .mapToObj(day -> (TemporalAccessor) LocalDate.of(2026, 11, 23).plusDays(day)).toList();
                case ERAS -> List.of(LocalDate.of(-100, 1, 1), LocalDate.of(2026, 1, 1));
                case AM_PM -> List.of(LocalTime.of(9, 0), LocalTime.of(15, 0));
                case QUARTERS -> IntStream.rangeClosed(0, 3)
                        .mapToObj(quarter -> (TemporalAccessor) LocalDate.of(2026, quarter * 3 + 2, 15)).toList();
            };
        }

        /// Returns the number of the sample within its field, for example the month number.
        ///
        /// @param sample the sample
        /// @return the number
        public long number(TemporalAccessor sample) {
            return sample.getLong(field);
        }

        /// Returns the localized name.
        ///
        /// @param sample the sample
        /// @param style  the text style
        /// @param locale the locale; numbering systems are respected, calendar extensions are not,
        ///               so the name always belongs to the ISO field value shown as number
        /// @return the name
        public String name(TemporalAccessor sample, TextStyle style, Locale locale) {
            return new DateTimeFormatterBuilder().appendText(field, style).toFormatter(locale)
                    .localizedBy(locale).withChronology(IsoChronology.INSTANCE).format(sample);
        }

        /// Returns the Java code that produces a name.
        ///
        /// @param sample the sample whose name is shown
        /// @param style  the text style
        /// @param locale the locale
        /// @return the code
        public String code(TemporalAccessor sample, TextStyle style, Locale locale) {
            return JavaCode.withLocale(locale,
                    "var temporal = " + sample.getClass().getSimpleName() + ".parse(" + JavaCode.string(sample.toString()) + ");",
                    "new DateTimeFormatterBuilder()",
                    "        .appendText(" + fieldCode + ", TextStyle." + style + ")",
                    "        .toFormatter(locale)",
                    "        .localizedBy(locale)",
                    "        .withChronology(IsoChronology.INSTANCE)",
                    "        .format(temporal);");
        }
    }

    private DateTimeNames() {
    }
}
