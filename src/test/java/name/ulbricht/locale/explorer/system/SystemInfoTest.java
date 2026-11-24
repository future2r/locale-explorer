package name.ulbricht.locale.explorer.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.Fact;

class SystemInfoTest {

    @Test
    void runtimeAndDefaults() {
        var runtime = SystemInfo.runtimeFacts();
        assertEquals(Runtime.version().toString(), runtime.getFirst().value());
        assertTrue(runtime.stream().anyMatch(fact -> "system.notAvailable".equals(fact.valueKey())));
        assertFalse(SystemInfo.defaultFacts().isEmpty());
    }

    @Test
    void availability() {
        var facts = SystemInfo.availabilityFacts(Locale.forLanguageTag("de-DE"));
        assertTrue(facts.getFirst().value().endsWith("de-DE ✓"), facts.getFirst().value());
        var withExtension = SystemInfo.availabilityFacts(Locale.forLanguageTag("de-DE-u-nu-arab")).getFirst().value();
        assertTrue(withExtension.endsWith(" ✓"), withExtension);
        var legacyThai = SystemInfo.availabilityFacts(Locale.forLanguageTag("th-TH-u-nu-thai-x-lvariant-TH")).getFirst().value();
        assertTrue(legacyThai.endsWith(" ✓"), legacyThai);
    }

    @Test
    void experimentsUseTheGivenDefaultsAndRestoreThePrevious() {
        var before = SystemInfo.Defaults.current();
        var turkish = new SystemInfo.Defaults(Locale.forLanguageTag("tr-TR"), Locale.forLanguageTag("tr-TR"),
                Locale.forLanguageTag("tr-TR"), TimeZone.getTimeZone("Asia/Tokyo"));
        var upper = SystemInfo.EXPERIMENTS.getFirst();
        assertEquals("TİTLE", turkish.evaluate(upper));
        assertEquals(before, SystemInfo.Defaults.current());
        var english = new SystemInfo.Defaults(Locale.US, Locale.US, Locale.US, TimeZone.getTimeZone("UTC"));
        assertEquals("TITLE", english.evaluate(upper));
        assertEquals("1234.5", english.evaluate(SystemInfo.EXPERIMENTS.get(3)));
        assertEquals("1234.5", turkish.evaluate(SystemInfo.EXPERIMENTS.get(3)));
    }

    @Test
    void originalValuesDoNotFollowChangedDefaults() {
        var upper = SystemInfo.EXPERIMENTS.getFirst();
        var before = SystemInfo.originalValue(upper);
        var defaults = SystemInfo.Defaults.current();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(before, SystemInfo.originalValue(upper));
        } finally {
            defaults.apply();
        }
        assertThrows(IllegalArgumentException.class,
                () -> SystemInfo.originalValue(new SystemInfo.Experiment("x", () -> "x")));
    }
}
