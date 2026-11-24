package name.ulbricht.locale.explorer.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.IllformedLocaleException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LocaleExtensionsTest {

    private static final Locale DE = Locale.forLanguageTag("de-DE");

    @Test
    void appliesAndRemovesKeywords() {
        var keywords = new LinkedHashMap<String, String>();
        keywords.put("ca", "japanese");
        keywords.put("nu", " arab ");
        var extended = LocaleExtensions.apply(DE, keywords);
        assertEquals("de-DE-u-ca-japanese-nu-arab", extended.toLanguageTag());

        var removed = LocaleExtensions.apply(extended, Map.of("ca", ""));
        assertEquals("de-DE-u-nu-arab", removed.toLanguageTag());
    }

    @Test
    void rejectsIllFormedValues() {
        assertThrows(IllformedLocaleException.class, () -> LocaleExtensions.apply(DE, Map.of("ca", "x")));
    }

    @Test
    void suggestionsExistForEveryKey() {
        for (var key : LocaleExtensions.KEYS) {
            assertFalse(LocaleExtensions.suggestedValues(key).isEmpty(), key);
        }
        assertTrue(LocaleExtensions.suggestedValues("ca").contains("japanese"));
        assertTrue(LocaleExtensions.suggestedValues("rg").contains("chzzzz"));
        assertTrue(LocaleExtensions.suggestedValues("cu").contains("eur"));
    }

    @Test
    void previewShowsEffectOfExtensions() {
        var dateTime = ZonedDateTime.of(2026, 11, 24, 14, 35, 7, 0, ZoneId.of("Europe/Berlin"));
        var rows = LocaleExtensions.previewRows(dateTime);
        var number = rows.stream().filter(row -> row.labelKey().equals("preview.number")).findFirst().orElseThrow();
        var firstDay = rows.stream().filter(row -> row.labelKey().equals("preview.firstDay")).findFirst().orElseThrow();

        assertEquals("-1.234.567,891", number.value().apply(DE));
        assertTrue(number.value().apply(Locale.forLanguageTag("de-DE-u-nu-arab")).contains("١"));
        assertEquals("Montag", firstDay.value().apply(DE));
        assertEquals("Sonntag", firstDay.value().apply(Locale.forLanguageTag("de-DE-u-fw-sun")));
    }
}
