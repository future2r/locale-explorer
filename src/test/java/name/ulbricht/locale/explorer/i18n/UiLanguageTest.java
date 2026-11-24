package name.ulbricht.locale.explorer.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class UiLanguageTest {

    @Test
    void fromLocaleMatchesLanguage() {
        assertEquals(UiLanguage.GERMAN, UiLanguage.fromLocale(Locale.forLanguageTag("de-AT")));
        assertEquals(UiLanguage.CHINESE_SIMPLIFIED, UiLanguage.fromLocale(Locale.forLanguageTag("zh-Hant-TW")));
        assertEquals(UiLanguage.ARABIC, UiLanguage.fromLocale(Locale.forLanguageTag("ar-EG")));
    }

    @Test
    void fromLocaleFallsBackToEnglish() {
        assertEquals(UiLanguage.ENGLISH, UiLanguage.fromLocale(Locale.forLanguageTag("ja-JP")));
    }

    @Test
    void fromTagRoundTrips() {
        for (var language : UiLanguage.values()) {
            assertEquals(Optional.of(language), UiLanguage.fromTag(language.tag()));
        }
        assertEquals(Optional.empty(), UiLanguage.fromTag(null));
        assertEquals(Optional.empty(), UiLanguage.fromTag("xx"));
    }

    @Test
    void onlyArabicIsRightToLeft() {
        for (var language : UiLanguage.values()) {
            assertEquals(language == UiLanguage.ARABIC, language.isRightToLeft(), language.name());
        }
    }

    @Test
    void nativeNamesStartUppercaseWhereTheScriptHasCase() {
        assertEquals("Français", UiLanguage.FRENCH.nativeName());
        assertEquals("Español", UiLanguage.SPANISH.nativeName());
        assertEquals("Deutsch", UiLanguage.GERMAN.nativeName());
        assertTrue(UiLanguage.CHINESE_SIMPLIFIED.nativeName().startsWith("中文"));
        assertFalse(UiLanguage.ARABIC.nativeName().isEmpty());
    }
}
