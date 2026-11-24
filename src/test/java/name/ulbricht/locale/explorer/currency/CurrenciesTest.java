package name.ulbricht.locale.explorer.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.LocaleProperty;

class CurrenciesTest {

    private static final Locale DE = Locale.forLanguageTag("de-DE");
    private static final Locale US = Locale.forLanguageTag("en-US");
    private static final BigDecimal AMOUNT = new BigDecimal("-1234.567");

    @Test
    void formatsWithLocaleCurrency() {
        var rows = Currencies.formatRows(AMOUNT, null, null);
        assertEquals("-1.234,57 €", value(rows, "currency.format.standard", DE));
        assertEquals("($1,234.57)", value(rows, "currency.format.accounting", US));
        assertEquals("-1.234,57 EUR", value(rows, "currency.format.isoCode", DE));
        assertEquals("EUR", value(rows, "currency.format.used", DE));
    }

    @Test
    void fixedCurrencyAndDigits() {
        // setCurrency keeps the two fraction digits of the euro format.
        var yen = Currencies.formatRows(AMOUNT, Currency.getInstance("JPY"), null);
        assertEquals("-1.234,57 ¥", value(yen, "currency.format.standard", DE));
        var yenDigits = Currencies.formatRows(AMOUNT, Currency.getInstance("JPY"), Currencies.CURRENCY_DIGITS);
        assertEquals("-1.235 ¥", value(yenDigits, "currency.format.standard", DE));

        var fourDigits = Currencies.formatRows(AMOUNT, Currency.getInstance("USD"), 4);
        assertEquals("-$1,234.5670", value(fourDigits, "currency.format.standard", US));
        // Java does not apply the CLDR currency spacing, so there is no space after the code.
        assertEquals("-USD1,234.5670", value(fourDigits, "currency.format.isoCode", US));
    }

    @Test
    void localeWithoutCountryHasNoCurrency() {
        var rows = Currencies.localeCurrencyRows(Locale.GERMAN);
        assertThrows(IllegalArgumentException.class, () -> value(rows, "currency.locale.code", Locale.GERMAN));
        assertEquals("XXX", value(Currencies.formatRows(AMOUNT, null, null), "currency.format.used", Locale.GERMAN));
        assertEquals("Euro", value(rows, "currency.locale.nameUi", DE));
        assertEquals("978", value(rows, "currency.locale.numericCode", DE));
        assertEquals("CHF", value(rows, "currency.locale.code", Locale.forLanguageTag("de-DE-u-cu-chf")));
    }

    @Test
    void symbolsDifferBetweenEnglishVariants() {
        var usd = Currencies.symbolRows(List.of("USD")).getFirst();
        assertEquals("$", usd.value().apply(US));
        assertEquals("US$", usd.value().apply(Locale.forLanguageTag("en-CA")));
        assertFalse(usd.highlight().test(US));
        assertTrue(Currencies.all().size() > 200);
    }

    private static String value(List<LocaleProperty> rows, String key, Locale locale) {
        return rows.stream().filter(row -> row.labelKey().equals(key)).findFirst().orElseThrow().value().apply(locale);
    }
}
