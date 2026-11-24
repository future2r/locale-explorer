package name.ulbricht.locale.explorer.model;

/// The color theme of the user interface.
public enum Theme {

    /// Follows the color scheme of the operating system.
    SYSTEM,
    LIGHT,
    DARK;

    /// Returns the key of the translated name.
    ///
    /// @return the message key
    public String messageKey() {
        return "theme." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /// Parses a stored name, falling back to [#SYSTEM].
    ///
    /// @param name the stored name, may be `null`
    /// @return the theme
    public static Theme parse(String name) {
        for (var theme : values()) {
            if (theme.name().equals(name)) {
                return theme;
            }
        }
        return SYSTEM;
    }
}
