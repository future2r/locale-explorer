package name.ulbricht.locale.explorer.text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/// Helpers around [Locale] lists.
public final class Locales {

    // Only locales that survive the round trip over their language tag: lists, code snippets and
    // preferences identify locales by tag, and no_NO_NY would come back as nn_NO, a second nn-NO.
    private static final List<Locale> AVAILABLE = Arrays.stream(Locale.getAvailableLocales())
            .filter(locale -> !locale.equals(Locale.ROOT))
            .filter(locale -> Locale.forLanguageTag(locale.toLanguageTag()).equals(locale))
            .sorted(Comparator.comparing(Locale::toLanguageTag))
            .toList();

    private Locales() {
    }

    /// Returns all available locales except [Locale#ROOT], sorted by language tag.
    ///
    /// @return an unmodifiable list of locales
    public static List<Locale> available() {
        return AVAILABLE;
    }

    /// Parses a comma-separated list of BCP 47 language tags.
    ///
    /// Blank entries, entries without a language and duplicates are dropped.
    ///
    /// @param tags the tags, for example `"en-US, ar-EG"`; may be `null`
    /// @return an unmodifiable list of locales in the given order
    public static List<Locale> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::strip)
                .filter(tag -> !tag.isEmpty())
                .map(Locale::forLanguageTag)
                .filter(locale -> !locale.getLanguage().isEmpty())
                .distinct()
                .toList();
    }

    /// Formats locales as a comma-separated list of BCP 47 language tags.
    ///
    /// @param locales the locales
    /// @return the tags, for example `"en-US,ar-EG"`
    public static String formatTags(List<Locale> locales) {
        return locales.stream()
                .filter(Objects::nonNull)
                .map(Locale::toLanguageTag)
                .collect(Collectors.joining(","));
    }

    /// Tells whether a locale is in a list of available locales.
    ///
    /// The lists of the JDK services contain locales without Unicode extensions, except the two
    /// legacy locales `ja-JP-u-ca-japanese-x-lvariant-JP` and `th-TH-u-nu-thai-x-lvariant-TH`.
    /// So the locale is looked up as it is and without its extensions.
    ///
    /// @param available the available locales, for example from `DateFormat.getAvailableLocales()`
    /// @param locale    the locale
    /// @return whether the locale or the locale without extensions is available
    public static boolean isAvailable(Collection<Locale> available, Locale locale) {
        return available.contains(locale) || available.contains(locale.stripExtensions());
    }

    /// The Java code of [#isAvailable] for the code panel.
    ///
    /// @param variable the name of the boolean variable the code declares
    /// @param call     the call returning the available locales, for example `DateFormat.getAvailableLocales()`
    /// @return the code, using a variable `locale`
    public static String isAvailableCode(String variable, String call) {
        return "var " + variable + " = Arrays.asList(" + call + ").contains(locale)\n"
                + "        || Arrays.asList(" + call + ").contains(locale.stripExtensions());";
    }
}
