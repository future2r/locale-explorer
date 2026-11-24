package name.ulbricht.locale.explorer.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.LocaleProperty;

class LocaleDetailsTest {

    private static final Locale UI = Locale.GERMAN;

    @Test
    void valuesForGermanGermany() {
        var locale = Locale.forLanguageTag("de-DE");
        assertEquals("de-DE", value("locale.prop.tag", locale));
        assertEquals("de_DE", value("toString()", locale));
        assertEquals("de · Deutsch", value("locale.prop.language", locale));
        assertEquals("", value("locale.prop.script", locale));
        assertEquals("DE · Deutschland", value("locale.prop.country", locale));
        assertEquals("deu / DEU", value("locale.prop.iso3", locale));
        assertEquals("LTR", value("locale.prop.direction", locale));
    }

    @Test
    void availabilityIgnoresExtensions() {
        var availability = LocaleDetails.availability(Locale.forLanguageTag("de-DE-u-nu-latn"));
        assertTrue(availability.contains("✓") && !availability.contains("✗"), availability);
    }

    @Test
    void arabicIsRightToLeft() {
        assertEquals("RTL", value("locale.prop.direction", Locale.forLanguageTag("ar-EG")));
    }

    @Test
    void extensionsAreListed() {
        var locale = Locale.forLanguageTag("de-DE-u-ca-japanese-nu-arab-x-test");
        assertEquals("u-ca-japanese-nu-arab · x-test", value("locale.prop.extensions", locale));
    }

    @Test
    void missingIso3CountryIsMarked() {
        assertEquals("deu / –", value("locale.prop.iso3", Locale.GERMAN));
    }

    @Test
    void availabilityListsAllServices() {
        var text = value("locale.prop.available", Locale.forLanguageTag("de-DE"));
        assertTrue(text.contains("DateFormat"), text);
        assertTrue(text.contains("BreakIterator"), text);
    }

    @Test
    void everyRowHasCodeThatNamesTheLocale() {
        var locale = Locale.forLanguageTag("fr-CA");
        for (var row : LocaleDetails.rows(UI)) {
            assertTrue(row.code().apply(locale).contains("\"fr-CA\""), row.labelKey());
        }
    }

    private static String value(String labelKey, Locale locale) {
        return LocaleDetails.rows(UI).stream()
                .filter(row -> row.labelKey().equals(labelKey))
                .map(LocaleProperty::value)
                .findFirst()
                .orElseThrow()
                .apply(locale);
    }
}
