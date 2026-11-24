package name.ulbricht.locale.explorer.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MessagesTest {

    private static final String PATH = "/name/ulbricht/locale/explorer/i18n/";

    @ParameterizedTest
    @EnumSource(value = UiLanguage.class, names = "ENGLISH", mode = EnumSource.Mode.EXCLUDE)
    void everyLanguageHasExactlyTheBaseKeys(UiLanguage language) throws IOException {
        var base = load("messages.properties");
        var translated = load("messages_" + language.locale().getLanguage() + ".properties");

        assertEquals(new TreeSet<>(base.stringPropertyNames()),
                new TreeSet<>(translated.stringPropertyNames()), language.name());
    }

    @Test
    void englishBundleIsEmpty() throws IOException {
        assertTrue(load("messages_en.properties").isEmpty());
    }

    @Test
    void englishDoesNotFallBackToSystemLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            ResourceBundle.clearCache();
            assertEquals("Settings", Messages.bundle(UiLanguage.ENGLISH).getString("settings.title"));
        } finally {
            Locale.setDefault(previous);
            ResourceBundle.clearCache();
        }
    }

    @ParameterizedTest
    @EnumSource(UiLanguage.class)
    void messageFormatKeysAreValidPatterns(UiLanguage language) {
        var bundle = Messages.bundle(language);
        for (var key : bundle.keySet()) {
            var text = bundle.getString(key);
            if (text.contains("{0}")) {
                var formatted = new MessageFormat(text, language.locale()).format(new Object[] {"X", "Y", "Z", "W", "V"});
                assertTrue(formatted.contains("X"), key + " in " + language);
                assertTrue(!formatted.contains("{"), key + " in " + language);
            }
        }
    }

    @Test
    void chineseUsesTheChineseBundle() {
        assertEquals("设置", Messages.bundle(UiLanguage.CHINESE_SIMPLIFIED).getString("settings.title"));
    }

    private static Properties load(String name) throws IOException {
        var stream = MessagesTest.class.getResourceAsStream(PATH + name);
        assertNotNull(stream, name);
        var properties = new Properties();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
