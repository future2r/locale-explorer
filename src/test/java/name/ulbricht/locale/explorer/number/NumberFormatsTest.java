package name.ulbricht.locale.explorer.number;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.CodePoints;
import name.ulbricht.locale.explorer.text.LocaleProperty;

class NumberFormatsTest {

    private static final Locale DE = Locale.forLanguageTag("de-DE");
    private static final Locale US = Locale.forLanguageTag("en-US");
    private static final Locale FR = Locale.forLanguageTag("fr-FR");
    private static final BigDecimal VALUE = new BigDecimal("-1234567.891");

    @Test
    void formatsWithOptions() {
        var rows = NumberFormats.formatRows(VALUE, NumberOptions.DEFAULT);
        assertEquals("-1.234.567,891", value(rows, "number.format.number", DE));
        assertEquals("-1,234,567.891", value(rows, "number.format.number", US));
        assertTrue(CodePoints.hasSpecialCharacters(value(rows, "number.format.number", FR)));

        var options = new NumberOptions(1, 2, 2, RoundingMode.DOWN, false);
        assertEquals("-1234567,89", value(NumberFormats.formatRows(VALUE, options), "number.format.number", DE));
    }

    @Test
    void halfEvenRounding() {
        var options = new NumberOptions(1, 0, 0, RoundingMode.HALF_EVEN, true);
        assertEquals("2", value(NumberFormats.formatRows(new BigDecimal("2.5"), options), "number.format.number", US));
        assertEquals("4", value(NumberFormats.formatRows(new BigDecimal("3.5"), options), "number.format.number", US));
    }

    @Test
    void specialValues() {
        assertTrue(Double.isNaN(NumberOptions.parseValue("NaN").doubleValue()));
        assertEquals(new BigDecimal("1E+21"), NumberOptions.parseValue(" 1e21 "));
        assertThrows(NumberFormatException.class, () -> NumberOptions.parseValue("x"));
        assertThrows(NumberFormatException.class, () -> NumberOptions.parseValue("1e999999999"));
        assertThrows(NumberFormatException.class, () -> NumberOptions.parseValue("1e-1001"));
        assertThrows(NumberFormatException.class, () -> NumberOptions.parseValue("1e309"));
        assertEquals(new BigDecimal("-1E+308"), NumberOptions.parseValue("-1e308"));
        assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), NumberOptions.parseValue(String.valueOf(Double.MAX_VALUE)));
        assertEquals(1000, NumberOptions.parseDecimal("0." + "1".repeat(1000)).scale());
    }

    @Test
    void symbolsShowCodePoints() {
        var decimal = NumberFormats.symbolRows().getFirst().value().apply(DE);
        assertEquals(",   U+002C", decimal);
    }

    @Test
    void customPatterns() {
        var rows = NumberFormats.patternRows(VALUE, "#,##0.00;(#,##0.00)", false);
        assertEquals("(1.234.567,89)", rows.getFirst().value().apply(DE));
        assertEquals("1.234.567,89", rows.get(1).value().apply(DE));
        assertThrows(IllegalArgumentException.class, () -> NumberFormats.patternRows(VALUE, "#,##0.00;;x'", false));

        var localized = NumberFormats.patternRows(VALUE, "#.##0,00", true);
        assertEquals("-1.234.567,89", localized.getFirst().value().apply(DE));
    }

    @Test
    void compact() {
        var rows = NumberFormats.compactRows(1);
        var million = rows.stream().filter(row -> row.labelKey().equals("1234567")).findFirst().orElseThrow();
        assertEquals("1.2M   ·   1.2 million", million.value().apply(US));
    }

    @Test
    void numberingSystems() {
        assertTrue(NumberFormats.isSupported(DE, "arab"));
        assertTrue(NumberFormats.isSupported(DE, "latn"));
        assertFalse(NumberFormats.isSupported(DE, "roman"));
        var arab = NumberFormats.numberingRows(new BigDecimal("12")).get(1);
        assertEquals("١٢", arab.value().apply(DE));
    }

    @Test
    void parsingStopsAtUnknownCharacters() {
        var lenient = NumberFormats.parse("1.234,5", NumberFormats.Kind.NUMBER, false, false, US);
        assertEquals(1.234, lenient.number().doubleValue());
        assertTrue(lenient.partial());
        assertEquals(5, lenient.index());

        var strict = NumberFormats.parse("1.234,5", NumberFormats.Kind.NUMBER, true, false, US);
        assertNull(strict.number());

        var german = NumberFormats.parse("1.234,5", NumberFormats.Kind.NUMBER, false, false, DE);
        assertEquals(1234.5, german.number().doubleValue());
        assertFalse(german.partial());
    }

    private static String value(List<LocaleProperty> rows, String key, Locale locale) {
        return rows.stream().filter(row -> row.labelKey().equals(key)).findFirst().orElseThrow().value().apply(locale);
    }
}
