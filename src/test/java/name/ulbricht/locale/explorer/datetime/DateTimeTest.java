package name.ulbricht.locale.explorer.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.LocaleProperty;

class DateTimeTest {

    private static final ZonedDateTime DATE_TIME =
            ZonedDateTime.of(2026, 11, 24, 14, 35, 7, 0, ZoneId.of("Europe/Berlin"));
    private static final Locale DE = Locale.forLanguageTag("de-DE");
    private static final Locale US = Locale.forLanguageTag("en-US");

    @Test
    void styleRowsSeparateAndMatrix() {
        assertEquals(8, DateTimeFormats.styleRows(DATE_TIME, TemporalKind.ZONED_DATE_TIME, false, false).size());
        assertEquals(16, DateTimeFormats.styleRows(DATE_TIME, TemporalKind.ZONED_DATE_TIME, false, true).size());
        assertEquals(16, DateTimeFormats.styleRows(DATE_TIME, TemporalKind.ZONED_DATE_TIME, true, false).size());
    }

    @Test
    void dateStylesAndPatterns() {
        var rows = DateTimeFormats.styleRows(DATE_TIME, TemporalKind.ZONED_DATE_TIME, false, true);
        assertEquals("24.11.26", rows.get(0).value().apply(DE));
        assertEquals("dd.MM.yy", rows.get(1).value().apply(DE));
        assertEquals("11/24/26", rows.get(0).value().apply(US));
        assertEquals("Dienstag, 24. November 2026", rows.get(6).value().apply(DE));
    }

    @Test
    void fullTimeNeedsAZone() {
        var rows = DateTimeFormats.styleRows(DATE_TIME, TemporalKind.LOCAL_DATE_TIME, false, false);
        var fullTime = rows.get(7);
        assertThrows(DateTimeException.class, () -> fullTime.value().apply(DE));
    }

    @Test
    void calendarExtensionIsRespected() {
        var rows = DateTimeFormats.styleRows(DATE_TIME, TemporalKind.ZONED_DATE_TIME, false, false);
        var japanese = rows.get(3).value().apply(Locale.forLanguageTag("ja-JP-u-ca-japanese"));
        assertTrue(japanese.contains("令和"), japanese);
    }

    @Test
    void skeletonsAndPatterns() {
        var skeleton = DateTimeFormats.skeletonRows(DATE_TIME, List.of("yMMMd"));
        assertEquals(2, skeleton.size());
        assertEquals("24. Nov. 2026", skeleton.get(0).value().apply(DE));

        var patterns = DateTimeFormats.patternRows(DATE_TIME, "EEEE");
        assertEquals("Dienstag", patterns.getFirst().value().apply(DE));
        assertEquals(DateTimeFormats.LETTERS.size() + 1, patterns.size());
    }

    @Test
    void standaloneMonthNamesDifferInPolish() {
        var november = LocalDate.of(2026, 11, 15);
        var polish = Locale.forLanguageTag("pl-PL");
        assertEquals("listopada", DateTimeNames.Kind.MONTHS.name(november, TextStyle.FULL, polish));
        assertEquals("listopad", DateTimeNames.Kind.MONTHS.name(november, TextStyle.FULL_STANDALONE, polish));
        assertEquals(12, DateTimeNames.Kind.MONTHS.samples().size());
        assertEquals(7, DateTimeNames.Kind.DAYS_OF_WEEK.samples().size());
        assertEquals(1, DateTimeNames.Kind.DAYS_OF_WEEK.number(DateTimeNames.Kind.DAYS_OF_WEEK.samples().getFirst()));
        var hijrah = Locale.forLanguageTag("en-u-ca-islamic-umalqura");
        var january = DateTimeNames.Kind.MONTHS.samples().getFirst();
        assertEquals(1, DateTimeNames.Kind.MONTHS.number(january));
        assertEquals("January", DateTimeNames.Kind.MONTHS.name(january, TextStyle.FULL, hijrah));
        var bce = DateTimeNames.Kind.ERAS.samples().getFirst();
        assertEquals("BC", DateTimeNames.Kind.ERAS.name(bce, TextStyle.SHORT, Locale.forLanguageTag("en-u-ca-japanese")));
    }

    @Test
    void parsingDependsOnLocale() {
        var shortDate = DateTimeParsing.Parser.standard().getFirst();
        assertEquals(LocalDate.of(2026, 11, 24), DateTimeParsing.parse("24.11.26", shortDate, ResolverStyle.SMART, DE));
        assertThrows(DateTimeParseException.class,
                () -> DateTimeParsing.parse("24.11.26", shortDate, ResolverStyle.SMART, US));
    }

    @Test
    void strictResolverRejectsInvalidDate() {
        var iso = DateTimeParsing.Parser.standard().stream()
                .filter(parser -> parser.labelKey().equals("ISO_LOCAL_DATE")).findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 2, 28), DateTimeParsing.parse("2026-02-30", iso, ResolverStyle.SMART, DE));
        assertThrows(DateTimeParseException.class,
                () -> DateTimeParsing.parse("2026-02-30", iso, ResolverStyle.STRICT, DE));
    }

    @Test
    void legacyTrapsShowDifferences() {
        var rows = LegacyDateFormats.rows(DATE_TIME);
        var u = value(rows, "\"u\"", DE);
        assertTrue(u.contains("java.time: 2026") && u.contains("java.text: 2"), u);
        var weekYear = value(rows, "\"YYYY-MM-dd\"", DE);
        assertTrue(weekYear.contains("java.time: 2026-12-29"), weekYear);
        assertTrue(weekYear.contains("java.text: 2026-12-29"), weekYear);
        assertTrue(value(rows, "\"YYYY-MM-dd\"", US).contains("2027-12-29"));
    }

    private static String value(List<LocaleProperty> rows, String labelStart, Locale locale) {
        return rows.stream().filter(row -> row.labelKey().startsWith(labelStart)).findFirst().orElseThrow()
                .value().apply(locale);
    }
}
