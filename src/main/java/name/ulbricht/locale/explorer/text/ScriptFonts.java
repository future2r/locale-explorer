package name.ulbricht.locale.explorer.text;

import java.lang.Character.UnicodeScript;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/// Picks a font family for scripts that the default UI font cannot display.
///
/// JavaFX renders text with the system UI font and its built-in fallback list. On Windows that
/// list (Segoe UI) has no glyphs for Ethiopic, Yi, Tifinagh or Adlam, so those characters show
/// up as boxes. Windows ships fonts for them, they are just not used automatically.
///
/// On other systems the font fallback of the operating system covers these scripts, so no
/// families are configured there.
public final class ScriptFonts {

    private static final Map<UnicodeScript, List<String>> WINDOWS = windowsFamilies();

    private final Map<UnicodeScript, List<String>> families;

    private ScriptFonts(Map<UnicodeScript, List<String>> families) {
        this.families = families;
    }

    /// Returns the font families for an operating system.
    ///
    /// @param osName the value of the system property `os.name`
    /// @return the font families, empty for systems with a complete font fallback
    public static ScriptFonts forOperatingSystem(String osName) {
        return osName != null && osName.startsWith("Windows")
                ? new ScriptFonts(WINDOWS)
                : new ScriptFonts(Map.of());
    }

    /// Returns the font family to use for a text.
    ///
    /// The first character whose script needs a special family decides. Other scripts in the
    /// same text, such as Latin, Chinese or Arabic, still use the font fallback of JavaFX (tried
    /// on Windows); only a second special script, for example Ethiopic next to Yi, would show
    /// as boxes, which is rare enough to ignore.
    ///
    /// @param text      the text, may be `null`
    /// @param installed tells whether a font family is installed
    /// @return the family, or empty if the default font is fine
    public Optional<String> familyFor(String text, Predicate<String> installed) {
        if (text == null || families.isEmpty()) {
            return Optional.empty();
        }
        return text.codePoints()
                .mapToObj(UnicodeScript::of)
                .distinct()
                .map(families::get)
                .filter(candidates -> candidates != null)
                .flatMap(List::stream)
                .filter(installed)
                .findFirst();
    }

    private static Map<UnicodeScript, List<String>> windowsFamilies() {
        var map = new EnumMap<UnicodeScript, List<String>>(UnicodeScript.class);
        for (var script : List.of(UnicodeScript.ETHIOPIC, UnicodeScript.TIFINAGH, UnicodeScript.ADLAM,
                UnicodeScript.NKO, UnicodeScript.VAI, UnicodeScript.OSMANYA)) {
            map.put(script, List.of("Ebrima", "Noto Sans " + displayName(script)));
        }
        map.put(UnicodeScript.YI, List.of("Microsoft Yi Baiti", "Noto Sans Yi"));
        map.put(UnicodeScript.MONGOLIAN, List.of("Mongolian Baiti", "Noto Sans Mongolian"));
        return Map.copyOf(map);
    }

    private static String displayName(UnicodeScript script) {
        return switch (script) {
            case NKO -> "NKo";
            default -> script.name().charAt(0) + script.name().substring(1).toLowerCase(Locale.ROOT);
        };
    }
}
