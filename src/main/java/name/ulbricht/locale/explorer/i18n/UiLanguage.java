package name.ulbricht.locale.explorer.i18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import name.ulbricht.locale.explorer.text.TextDirection;

/// The languages the user interface is available in.
///
/// This is independent of the locales being examined.
public enum UiLanguage {

    GERMAN("de"),
    ENGLISH("en"),
    FRENCH("fr"),
    SPANISH("es"),
    CHINESE_SIMPLIFIED("zh-Hans"),
    ARABIC("ar");

    private final Locale locale;

    UiLanguage(String tag) {
        this.locale = Locale.forLanguageTag(tag);
    }

    /// Returns the locale used to load the messages and to display locale names.
    ///
    /// @return the locale
    public Locale locale() {
        return locale;
    }

    /// Returns the language tag, for example `zh-Hans`.
    ///
    /// @return the tag
    public String tag() {
        return locale.toLanguageTag();
    }

    /// Returns the name of the language in the language itself, for example `Français`.
    ///
    /// @return the native name
    public String nativeName() {
        var name = locale.getDisplayName(locale);
        if (name.isEmpty()) {
            return tag();
        }
        var first = name.codePointAt(0);
        var upper = new String(Character.toChars(first)).toUpperCase(locale);
        return upper + name.substring(Character.charCount(first));
    }

    /// Returns `true` if the language is written right to left.
    ///
    /// @return whether the layout must be mirrored
    public boolean isRightToLeft() {
        return TextDirection.isRightToLeft(locale);
    }

    /// Finds the UI language for a stored tag.
    ///
    /// @param tag the tag, may be `null`
    /// @return the matching language, if any
    public static Optional<UiLanguage> fromTag(String tag) {
        return Arrays.stream(values())
                .filter(language -> language.tag().equalsIgnoreCase(tag))
                .findFirst();
    }

    /// Picks the UI language that fits a locale best, falling back to [#ENGLISH].
    ///
    /// Chinese in Traditional script also maps to simplified Chinese, the only Chinese
    /// variant offered.
    ///
    /// @param locale the locale, typically the system's display locale
    /// @return the best matching language
    public static UiLanguage fromLocale(Locale locale) {
        return Arrays.stream(values())
                .filter(language -> language.locale.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(ENGLISH);
    }
}
