package name.ulbricht.locale.explorer.text;

import java.util.Locale;

/// Builds snippets of Java source code for the code panel.
public final class JavaCode {

    private JavaCode() {
    }

    /// Returns a Java expression that creates the locale.
    ///
    /// @param locale the locale
    /// @return for example `Locale.forLanguageTag("de-DE")`
    public static String locale(Locale locale) {
        if (Locale.ROOT.equals(locale)) {
            return "Locale.ROOT";
        }
        return "Locale.forLanguageTag(" + string(locale.toLanguageTag()) + ")";
    }

    /// Returns a declaration of a variable `locale` followed by the given statement lines.
    ///
    /// @param locale the locale assigned to the variable
    /// @param lines  further lines that may use `locale`
    /// @return the snippet
    public static String withLocale(Locale locale, String... lines) {
        var code = new StringBuilder("var locale = ").append(locale(locale)).append(';');
        for (var line : lines) {
            code.append('\n').append(line);
        }
        return code.toString();
    }

    /// Returns a declaration of a variable `zonedDateTime` with the given value.
    ///
    /// @param dateTime the value
    /// @return for example `var zonedDateTime = ZonedDateTime.parse("2026-11-24T14:35:07+01:00[Europe/Berlin]");`
    public static String zonedDateTime(java.time.ZonedDateTime dateTime) {
        return "var zonedDateTime = ZonedDateTime.parse(" + string(dateTime.toString()) + ");";
    }

    /// Returns a Java string literal.
    ///
    /// Line breaks, tabs and other common control characters use their escape sequences; all
    /// other characters outside printable ASCII are written as Unicode escapes, so that
    /// invisible characters stay visible in the code.
    ///
    /// @param text the text
    /// @return the literal including quotes
    public static String string(String text) {
        var literal = new StringBuilder("\"");
        text.chars().forEach(c -> {
            switch (c) {
                case '"' -> literal.append("\\\"");
                case '\\' -> literal.append("\\\\");
                case '\n' -> literal.append("\\n");
                // a unicode escape for CR or LF would end the literal, as escapes are translated first
                case '\r' -> literal.append("\\r");
                case '\t' -> literal.append("\\t");
                case '\b' -> literal.append("\\b");
                case '\f' -> literal.append("\\f");
                default -> {
                    if (c >= 0x20 && c < 0x7F) {
                        literal.append((char) c);
                    } else {
                        literal.append("\\u%04X".formatted(c));
                    }
                }
            }
        });
        return literal.append('"').toString();
    }
}
