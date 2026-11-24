package name.ulbricht.locale.explorer.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.LocaleProperty;

class CalendarTest {

    private static final Locale DE = Locale.forLanguageTag("de-DE");
    private static final Locale US = Locale.forLanguageTag("en-US");
    private static final Locale EG = Locale.forLanguageTag("ar-EG");

    @Test
    void weekRules() {
        var rows = WeekRules.rows(LocalDate.of(2026, 11, 24));
        assertEquals("Montag", value(rows, "calendar.rule.firstDay", DE));
        assertEquals("4", value(rows, "calendar.rule.minimalDays", DE));
        assertEquals("ISO", value(rows, "calendar.rule.standard", DE));
        assertEquals("SUNDAY_START", value(rows, "calendar.rule.standard", US));
        assertEquals("", value(rows, "calendar.rule.standard", EG));
        assertEquals("48", value(rows, "calendar.rule.weekOfWeekBasedYear", DE));
        assertEquals("Sunday", value(rows, "calendar.rule.firstDay", Locale.forLanguageTag("en-GB-u-fw-sun")));
        assertEquals("fw-sun", value(rows, "calendar.rule.extensions", Locale.forLanguageTag("en-GB-u-fw-sun")));
    }

    @Test
    void turnOfYearHighlightsDiffringWeekYears() {
        var rows = WeekRules.turnOfYearRows(2026, Locale.GERMAN);
        assertEquals(10, rows.size());
        var dec29 = rows.get(3);
        assertTrue(dec29.labelKey().startsWith("2026-12-29"));
        assertEquals("53 / 2026", dec29.value().apply(DE));
        assertEquals("1 / 2027", dec29.value().apply(US));
        assertFalse(dec29.highlight().test(DE));
        assertTrue(dec29.highlight().test(US));
    }

    @Test
    void monthSheetFollowsFirstDayOfWeek() {
        var german = MonthSheet.of(YearMonth.of(2026, 11), DE);
        assertEquals("November 2026", german.title());
        assertEquals("M", german.weekdays().getFirst());
        assertEquals(LocalDate.of(2026, 10, 26), german.weeks().getFirst().days().getFirst().date());
        assertEquals("44", german.weeks().getFirst().number());

        var us = MonthSheet.of(YearMonth.of(2026, 11), US);
        assertEquals("S", us.weekdays().getFirst());
        assertEquals(LocalDate.of(2026, 11, 1), us.weeks().getFirst().days().getFirst().date());

        var egypt = MonthSheet.of(YearMonth.of(2026, 11), EG);
        assertEquals(LocalDate.of(2026, 10, 31), egypt.weeks().getFirst().days().getFirst().date());
        assertEquals("٣١", egypt.weeks().getFirst().days().getFirst().text());
    }

    @Test
    void monthSheetIsGregorianEvenWithACalendarExtension() {
        var sheet = MonthSheet.of(YearMonth.of(2026, 11), Locale.forLanguageTag("en-u-ca-islamic-umalqura"));
        assertEquals("November 2026", sheet.title());
        var first = sheet.weeks().stream().flatMap(week -> week.days().stream())
                .filter(day -> day.date().equals(LocalDate.of(2026, 11, 1))).findFirst().orElseThrow();
        assertEquals("1", first.text());
    }

    @Test
    void chronologies() {
        var rows = Chronologies.rows(LocalDate.of(2026, 11, 24));
        assertEquals(10, rows.size());
        assertTrue(rows.get(2).value().apply(Locale.forLanguageTag("ja-JP")).contains("令和8年"));
        assertTrue(rows.get(4).value().apply(Locale.forLanguageTag("th-TH")).contains("2569"));
    }

    @Test
    void dateOutsideAChronologyOnlyBreaksItsCells() {
        var rows = Chronologies.rows(LocalDate.of(1850, 1, 1));
        var japan = Locale.forLanguageTag("ja-JP");
        assertThrows(DateTimeException.class, () -> rows.get(3).value().apply(japan));
        assertTrue(rows.get(1).value().apply(japan).contains("1850"));
    }

    private static String value(List<LocaleProperty> rows, String key, Locale locale) {
        return rows.stream().filter(row -> row.labelKey().equals(key)).findFirst().orElseThrow().value().apply(locale);
    }
}
