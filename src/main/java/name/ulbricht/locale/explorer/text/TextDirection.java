package name.ulbricht.locale.explorer.text;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Derives the writing direction of a [Locale].
///
/// Java has no public API for this, so the direction is derived from the script subtag if
/// present, otherwise from the usual script of the language in the region for a selection of
/// regions where it differs, otherwise from the language alone.
public final class TextDirection {

    private static final Set<String> RTL_SCRIPTS = Set.of(
            "Adlm", "Arab", "Hebr", "Mand", "Nkoo", "Rohg", "Samr", "Syrc", "Thaa");

    private static final Set<String> RTL_LANGUAGES = Set.of(
            "ar", "ckb", "dv", "fa", "he", "iw", "ks", "lrc", "mzn", "nqo", "ps", "sd", "syr",
            "ug", "ur", "yi");

    /// A selection of language and region combinations whose usual script (CLDR likely
    /// subtags) has a different direction than that of the language alone, for example Punjabi
    /// in Pakistan written in Arabic script or Sindhi in India written in Devanagari.
    private static final Map<String, String> REGIONAL_SCRIPTS = Map.ofEntries(
            Map.entry("pa-PK", "Arab"),
            Map.entry("sd-IN", "Deva"),
            Map.entry("uz-AF", "Arab"),
            Map.entry("az-IR", "Arab"),
            Map.entry("az-IQ", "Arab"),
            Map.entry("kk-CN", "Arab"),
            Map.entry("ky-CN", "Arab"),
            Map.entry("tk-AF", "Arab"),
            Map.entry("tk-IR", "Arab"),
            Map.entry("ms-CC", "Arab"),
            Map.entry("ug-KZ", "Cyrl"),
            Map.entry("ug-MN", "Cyrl"));

    private TextDirection() {
    }

    /// Returns `true` if text in the given locale is written right to left.
    ///
    /// @param locale the locale to check
    /// @return whether the locale is written right to left
    public static boolean isRightToLeft(Locale locale) {
        var script = locale.getScript();
        if (script.isEmpty()) {
            script = REGIONAL_SCRIPTS.getOrDefault(locale.getLanguage() + "-" + locale.getCountry(), "");
        }
        if (!script.isEmpty()) {
            return RTL_SCRIPTS.contains(script);
        }
        return RTL_LANGUAGES.contains(locale.getLanguage());
    }
}
