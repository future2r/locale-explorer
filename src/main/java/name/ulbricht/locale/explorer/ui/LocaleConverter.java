package name.ulbricht.locale.explorer.ui;

import java.util.Locale;

import javafx.util.StringConverter;

/// Shows a locale as `tag · name`, with the name in the UI language.
///
/// When parsing, everything up to the first space is taken as the language tag, so both
/// `de-CH` and `de-CH · German (Switzerland)` work.
final class LocaleConverter extends StringConverter<Locale> {

    private static final String SEPARATOR = " · ";

    private final Locale displayLocale;

    LocaleConverter(Locale displayLocale) {
        this.displayLocale = displayLocale;
    }

    @Override
    public String toString(Locale locale) {
        if (locale == null) {
            return "";
        }
        return locale.toLanguageTag() + SEPARATOR + locale.getDisplayName(displayLocale);
    }

    @Override
    public Locale fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var tag = text.strip().split("\\s+", 2)[0];
        var locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? null : locale;
    }
}
