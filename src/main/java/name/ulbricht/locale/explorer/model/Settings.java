package name.ulbricht.locale.explorer.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import name.ulbricht.locale.explorer.i18n.UiLanguage;

/// A snapshot of the values that are kept between application runs.
///
/// @param uiLanguage     the language of the user interface
/// @param theme          the color theme
/// @param fontSize       the font size in pixels
/// @param mainLocale     the locale being examined
/// @param compareLocales further locales shown as additional columns
/// @param zone           the time zone for date-time examples
/// @param dateTime       the local date-time for examples
/// @param category       the selected category
public record Settings(UiLanguage uiLanguage, Theme theme, int fontSize, Locale mainLocale,
                       List<Locale> compareLocales, ZoneId zone, LocalDateTime dateTime,
                       Category category) {

    public Settings {
        compareLocales = List.copyOf(compareLocales);
    }
}
