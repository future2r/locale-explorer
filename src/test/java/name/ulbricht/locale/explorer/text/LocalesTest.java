package name.ulbricht.locale.explorer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalesTest {

    @Test
    void availableIsSortedAndWithoutRoot() {
        var locales = Locales.available();
        assertFalse(locales.isEmpty());
        assertFalse(locales.contains(Locale.ROOT));
        assertEquals(locales.stream().sorted(Comparator.comparing(Locale::toLanguageTag)).toList(), locales);
    }

    @Test
    void parseTagsSkipsBlankInvalidAndDuplicateEntries() {
        assertEquals(List.of(Locale.forLanguageTag("en-US"), Locale.forLanguageTag("ar-EG")),
                Locales.parseTags(" en-US, ,ar-EG,en-US, 123 "));
        assertTrue(Locales.parseTags(null).isEmpty());
        assertTrue(Locales.parseTags("  ").isEmpty());
    }

    @Test
    void formatAndParseRoundTrip() {
        var locales = List.of(Locale.forLanguageTag("de-CH-u-nu-latn"), Locale.forLanguageTag("zh-Hans-CN"));
        assertEquals(locales, Locales.parseTags(Locales.formatTags(locales)));
    }
}
