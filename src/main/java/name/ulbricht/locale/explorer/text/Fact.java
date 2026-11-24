package name.ulbricht.locale.explorer.text;

import java.util.List;

/// A locale-independent result: a label, a value and the Java code that produces it.
///
/// The value is either a plain text or, if `valueKey` is set, a message key to be translated.
/// Plain values are shown as they are, even if they look like a key.
///
/// @param labelKey  the message key of the label
/// @param labelArgs arguments for placeholders in the label
/// @param value     the plain value; ignored if `valueKey` is set
/// @param valueKey  the message key of a translated value, or `null`
/// @param code      the Java code
public record Fact(String labelKey, List<String> labelArgs, String value, String valueKey, String code) {

    public Fact {
        labelArgs = List.copyOf(labelArgs);
    }

    /// Creates a fact with a plain value.
    ///
    /// @param labelKey  the message key of the label
    /// @param labelArgs arguments for placeholders in the label
    /// @param value     the value
    /// @param code      the Java code
    public Fact(String labelKey, List<String> labelArgs, String value, String code) {
        this(labelKey, labelArgs, value, null, code);
    }

    /// Creates a fact whose label has no placeholders.
    ///
    /// @param labelKey the message key of the label
    /// @param value    the value
    /// @param code     the Java code
    /// @return the fact
    public static Fact of(String labelKey, String value, String code) {
        return new Fact(labelKey, List.of(), value, code);
    }

    /// Creates a fact whose value is translated.
    ///
    /// @param labelKey the message key of the label
    /// @param valueKey the message key of the value
    /// @param code     the Java code
    /// @return the fact
    public static Fact translated(String labelKey, String valueKey, String code) {
        return new Fact(labelKey, List.of(), "", valueKey, code);
    }
}
