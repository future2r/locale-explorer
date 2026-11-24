package name.ulbricht.locale.explorer.lists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class TextFormatsTest {

    private static final Locale DE = Locale.forLanguageTag("de-DE");
    private static final Locale US = Locale.forLanguageTag("en-US");
    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.of(2026, 11, 24, 14, 35, 7, 0, ZoneId.of("Europe/Berlin"));

    @Test
    void itemsAreSplitAtBars() {
        assertEquals(List.of("a", "b c"), TextFormats.items(" a | | b c |"));
    }

    @Test
    void listFormats() {
        var rows = TextFormats.listRows(List.of("Äpfel", "Birnen", "Bananen"));
        assertEquals(9, rows.size());
        assertEquals("Äpfel, Birnen und Bananen", rows.getFirst().value().apply(DE));
        assertEquals("Äpfel, Birnen, and Bananen", rows.getFirst().value().apply(US));
        assertFalse(rows.getFirst().highlight().test(DE));
        var spanish = TextFormats.listRows(List.of("Ana", "Isabel")).getFirst().value().apply(ES);
        // Spanish grammar would require "e" before an i sound; Java does not apply that rule.
        assertEquals("Ana y Isabel", spanish);
    }

    @Test
    void emptyListHasNoRows() {
        assertEquals(List.of(), TextFormats.listRows(List.of()));
    }

    @Test
    void messageFormats() {
        var pattern = "{0} hat {2,choice,0#keine Dateien|1#eine Datei|1<{2,number,integer} Dateien} gelöscht.";
        var rows = TextFormats.messageRows(pattern, "Anna", DATE_TIME, 1234);
        assertEquals("Anna hat 1.234 Dateien gelöscht.", rows.getFirst().value().apply(DE));
        assertEquals("Anna hat eine Datei gelöscht.",
                TextFormats.messageRows(pattern, "Anna", DATE_TIME, 1).getFirst().value().apply(DE));
        assertThrows(IllegalArgumentException.class, () -> TextFormats.messageRows("{0", "x", DATE_TIME, 0));
    }

    @Test
    void turkishDottedI() {
        var rows = TextFormats.caseRows(List.of("title", "Straße"));
        assertEquals("TİTLE", rows.getFirst().value().apply(TR));
        assertTrue(rows.getFirst().highlight().test(TR));
        assertFalse(rows.getFirst().highlight().test(DE));
        assertEquals("STRASSE   [6 → 7]", rows.get(2).value().apply(DE));
    }

    @Test
    void stringFormat() {
        var rows = TextFormats.formatRows(List.of("%1$,.2f", "%2$tA", "%3$S", "%2$d"), -1234567.891, DATE_TIME, "title", 42);
        assertEquals("-1.234.567,89", rows.getFirst().value().apply(DE));
        assertEquals("Dienstag", rows.get(1).value().apply(DE));
        assertEquals("TİTLE", rows.get(2).value().apply(TR));
        assertThrows(java.util.IllegalFormatException.class, () -> rows.get(3).value().apply(DE));
    }

    @Test
    void formatErrorsAreReportedBeforeFormatting() {
        var dateTime = ZonedDateTime.of(2026, 11, 24, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        assertTrue(TextFormats.formatError("%.2f %s", 1.5, dateTime, "x", 3).isEmpty());
        assertTrue(TextFormats.formatError("%q", 1.5, dateTime, "x", 3).isPresent());
        assertTrue(TextFormats.formatError("%d", 1.5, dateTime, "x", 3).isPresent());
        assertTrue(TextFormats.formatError("%1$.999999999f", 1.5, dateTime, "x", 3).isPresent());
        assertTrue(TextFormats.formatError("%2000s", 1.5, dateTime, "x", 3).isPresent());
        assertTrue(TextFormats.formatError("%1000s", 1.5, dateTime, "x", 3).isEmpty());
    }
}
