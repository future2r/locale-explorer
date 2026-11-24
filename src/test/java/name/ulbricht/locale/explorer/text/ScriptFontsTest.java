package name.ulbricht.locale.explorer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ScriptFontsTest {

    private static final Set<String> INSTALLED = Set.of("Ebrima", "Microsoft Yi Baiti");

    private final ScriptFonts windows = ScriptFonts.forOperatingSystem("Windows 11");

    @Test
    void ethiopicUsesEbrimaOnWindows() {
        var amharic = Locale.forLanguageTag("am-ET");
        assertEquals(Optional.of("Ebrima"), windows.familyFor(amharic.getDisplayName(amharic), INSTALLED::contains));
    }

    @Test
    void yiUsesYiBaiti() {
        var yi = Locale.forLanguageTag("ii-CN");
        assertEquals(Optional.of("Microsoft Yi Baiti"), windows.familyFor(yi.getDisplayName(yi), INSTALLED::contains));
    }

    @Test
    void coveredScriptsKeepTheDefaultFont() {
        assertEquals(Optional.empty(), windows.familyFor("Deutsch (Schweiz) 日本語 العربية", INSTALLED::contains));
        assertEquals(Optional.empty(), windows.familyFor(null, INSTALLED::contains));
    }

    @Test
    void missingFontsFallBackToTheNextCandidate() {
        assertEquals(Optional.of("Noto Sans Ethiopic"), windows.familyFor("ሰኞ", "Noto Sans Ethiopic"::equals));
        assertEquals(Optional.empty(), windows.familyFor("ሰኞ", _ -> false));
    }

    @Test
    void otherSystemsRelyOnTheirOwnFallback() {
        var linux = ScriptFonts.forOperatingSystem("Linux");
        assertEquals(Optional.empty(), linux.familyFor("ሰኞ", INSTALLED::contains));
    }
}
