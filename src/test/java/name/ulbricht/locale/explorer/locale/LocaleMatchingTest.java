package name.ulbricht.locale.explorer.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;
import java.util.Locale.FilteringMode;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LocaleMatchingTest {

    private static final List<Locale> CANDIDATES = List.of(
            Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("fr-FR"),
            Locale.forLanguageTag("fr-CH"), Locale.forLanguageTag("en-US"));

    @Test
    void lookupPicksBestAndFilterKeepsPriorityOrder() {
        var result = LocaleMatching.match("fr-CH, fr;q=0.9, en;q=0.8", CANDIDATES, FilteringMode.AUTOSELECT_FILTERING);

        assertEquals(3, result.ranges().size());
        assertEquals("fr-ch", result.ranges().getFirst().getRange());
        assertEquals(0.9, result.ranges().get(1).getWeight());
        assertEquals(Optional.of(Locale.forLanguageTag("fr-CH")), result.lookup());
        assertEquals(List.of(Locale.forLanguageTag("fr-CH"), Locale.forLanguageTag("fr-FR"),
                Locale.forLanguageTag("en-US")), result.filtered());
    }

    @Test
    void noMatch() {
        var result = LocaleMatching.match("ja", CANDIDATES, FilteringMode.AUTOSELECT_FILTERING);
        assertEquals(Optional.empty(), result.lookup());
        assertEquals(List.of(), result.filtered());
    }

    @Test
    void malformedListIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LocaleMatching.match("en;q=2", CANDIDATES, FilteringMode.AUTOSELECT_FILTERING));
    }
}
