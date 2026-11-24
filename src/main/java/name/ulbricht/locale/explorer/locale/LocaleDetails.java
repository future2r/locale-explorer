package name.ulbricht.locale.explorer.locale;

import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.text.Locales;
import name.ulbricht.locale.explorer.text.TextDirection;

/// The rows of the "Details" tab of the locale category.
public final class LocaleDetails {

    private record Service(String name, Set<Locale> locales) {
    }

    private static final List<Service> SERVICES = List.of(
            new Service("DateFormat", Set.of(DateFormat.getAvailableLocales())),
            new Service("NumberFormat", Set.of(NumberFormat.getAvailableLocales())),
            new Service("Collator", Set.of(Collator.getAvailableLocales())),
            new Service("BreakIterator", Set.of(BreakIterator.getAvailableLocales())));

    private LocaleDetails() {
    }

    /// Returns the rows.
    ///
    /// @param uiLocale the locale of the user interface, used for display names
    /// @return the rows in display order
    public static List<LocaleProperty> rows(Locale uiLocale) {
        var ui = JavaCode.locale(uiLocale);
        return List.of(
                LocaleProperty.of("locale.prop.tag", Locale::toLanguageTag,
                        locale -> JavaCode.withLocale(locale, "locale.toLanguageTag();")),
                LocaleProperty.literal("toString()", Locale::toString,
                        locale -> JavaCode.withLocale(locale, "locale.toString();")),
                LocaleProperty.of("locale.prop.language",
                        locale -> codeAndName(locale.getLanguage(), locale.getDisplayLanguage(uiLocale)),
                        locale -> JavaCode.withLocale(locale, "locale.getLanguage();",
                                "locale.getDisplayLanguage(" + ui + ");")),
                LocaleProperty.of("locale.prop.script",
                        locale -> codeAndName(locale.getScript(), locale.getDisplayScript(uiLocale)),
                        locale -> JavaCode.withLocale(locale, "locale.getScript();",
                                "locale.getDisplayScript(" + ui + ");")),
                LocaleProperty.of("locale.prop.country",
                        locale -> codeAndName(locale.getCountry(), locale.getDisplayCountry(uiLocale)),
                        locale -> JavaCode.withLocale(locale, "locale.getCountry();",
                                "locale.getDisplayCountry(" + ui + ");")),
                LocaleProperty.of("locale.prop.variant",
                        locale -> codeAndName(locale.getVariant(), locale.getDisplayVariant(uiLocale)),
                        locale -> JavaCode.withLocale(locale, "locale.getVariant();",
                                "locale.getDisplayVariant(" + ui + ");")),
                LocaleProperty.of("locale.prop.iso3", LocaleDetails::iso3,
                        locale -> JavaCode.withLocale(locale, "locale.getISO3Language();",
                                "locale.getISO3Country();")),
                LocaleProperty.of("locale.prop.extensions", LocaleDetails::extensions,
                        locale -> JavaCode.withLocale(locale, "locale.getExtensionKeys();",
                                "locale.getUnicodeLocaleKeys();",
                                "locale.getUnicodeLocaleType(\"ca\");")),
                LocaleProperty.of("locale.prop.nameUi", locale -> locale.getDisplayName(uiLocale),
                        locale -> JavaCode.withLocale(locale, "locale.getDisplayName(" + ui + ");")),
                LocaleProperty.of("locale.prop.nameOwn", locale -> locale.getDisplayName(locale),
                        locale -> JavaCode.withLocale(locale, "locale.getDisplayName(locale);")),
                LocaleProperty.of("locale.prop.direction",
                        locale -> TextDirection.isRightToLeft(locale) ? "RTL" : "LTR",
                        locale -> "// No Java API: derived from the script subtag, the region or the language.\n"
                                + JavaCode.withLocale(locale, "locale.getScript();", "locale.getLanguage();")),
                LocaleProperty.of("locale.prop.available", LocaleDetails::availability,
                        locale -> JavaCode.withLocale(locale,
                                Locales.isAvailableCode("dateFormat", "DateFormat.getAvailableLocales()"),
                                Locales.isAvailableCode("numberFormat", "NumberFormat.getAvailableLocales()"),
                                Locales.isAvailableCode("collator", "Collator.getAvailableLocales()"),
                                Locales.isAvailableCode("breakIterator", "BreakIterator.getAvailableLocales()"))));
    }

    static String codeAndName(String code, String name) {
        if (code.isEmpty()) {
            return "";
        }
        return name.isEmpty() || name.equals(code) ? code : code + " · " + name;
    }

    static String iso3(Locale locale) {
        return safe(locale::getISO3Language) + " / " + safe(locale::getISO3Country);
    }

    static String extensions(Locale locale) {
        return locale.getExtensionKeys().stream()
                .sorted()
                .map(key -> key + "-" + locale.getExtension(key))
                .collect(Collectors.joining(" · "));
    }

    static String availability(Locale locale) {
        return SERVICES.stream()
                .map(service -> service.name() + (Locales.isAvailable(service.locales(), locale) ? " ✓" : " ✗"))
                .collect(Collectors.joining(" · "));
    }

    private static String safe(Supplier<String> supplier) {
        try {
            var value = supplier.get();
            return value.isEmpty() ? "–" : value;
        } catch (MissingResourceException _) {
            return "?";
        }
    }
}
