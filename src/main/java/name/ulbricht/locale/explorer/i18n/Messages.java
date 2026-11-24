package name.ulbricht.locale.explorer.i18n;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/// Access to the translated texts of the user interface.
///
/// The bundles are UTF-8 properties files next to this class. `messages.properties` holds the
/// English texts; `messages_en.properties` exists but is empty, so English never falls back to
/// the system locale's bundle.
public final class Messages {

    /// The base name of the resource bundle.
    public static final String BUNDLE = "name.ulbricht.locale.explorer.i18n.messages";

    private Messages() {
    }

    /// Loads the bundle for a UI language.
    ///
    /// @param language the language
    /// @return the bundle
    public static ResourceBundle bundle(UiLanguage language) {
        return ResourceBundle.getBundle(BUNDLE, language.locale());
    }

    /// Formats a message with [MessageFormat] in the bundle's locale.
    ///
    /// @param bundle    the bundle
    /// @param key       the key
    /// @param arguments the arguments
    /// @return the formatted message
    public static String format(ResourceBundle bundle, String key, Object... arguments) {
        return new MessageFormat(bundle.getString(key), bundle.getLocale()).format(arguments);
    }
}
