package name.ulbricht.locale.explorer.text;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/// One row of a result table: a label and, for each locale column, a value and the Java code
/// that produces it.
///
/// @param labelKey       the message key of the label, or the label itself if `literal`
/// @param literal        `true` if the label is code and must not be translated
/// @param labelArgs      arguments for a translated label with placeholders
/// @param descriptionKey the message key of an explanation shown after the label, or `null`
/// @param value          computes the value for a locale; an empty string means "no value"
/// @param code           builds the Java code for a locale
/// @param highlight      decides per locale whether the value is highlighted, or `null` for never
public record LocaleProperty(String labelKey, boolean literal, List<String> labelArgs,
                             String descriptionKey, Function<Locale, String> value,
                             Function<Locale, String> code, Predicate<Locale> highlight) {

    /// Returns a copy of this row whose values are highlighted where the predicate holds.
    ///
    /// @param predicate decides per locale whether the value is highlighted
    /// @return the new row
    public LocaleProperty withHighlight(Predicate<Locale> predicate) {
        return new LocaleProperty(labelKey, literal, labelArgs, descriptionKey, value, code, predicate);
    }

    public LocaleProperty {
        labelArgs = List.copyOf(labelArgs);
    }

    /// Creates a row with a translated label.
    ///
    /// @param labelKey the message key
    /// @param value    the value function
    /// @param code     the code function
    /// @return the row
    public static LocaleProperty of(String labelKey, Function<Locale, String> value,
                                    Function<Locale, String> code) {
        return new LocaleProperty(labelKey, false, List.of(), null, value, code, null);
    }

    /// Creates a row with a translated label that has placeholders.
    ///
    /// @param labelKey  the message key
    /// @param labelArgs the arguments for the placeholders
    /// @param value     the value function
    /// @param code      the code function
    /// @return the row
    public static LocaleProperty of(String labelKey, List<String> labelArgs, Function<Locale, String> value,
                                    Function<Locale, String> code) {
        return new LocaleProperty(labelKey, false, labelArgs, null, value, code, null);
    }

    /// Creates a row whose label is shown as is, typically a method name or a pattern.
    ///
    /// @param label the label
    /// @param value the value function
    /// @param code  the code function
    /// @return the row
    public static LocaleProperty literal(String label, Function<Locale, String> value,
                                         Function<Locale, String> code) {
        return new LocaleProperty(label, true, List.of(), null, value, code, null);
    }

    /// Creates a row whose label is shown as is, followed by a translated explanation.
    ///
    /// @param label          the label
    /// @param descriptionKey the message key of the explanation
    /// @param value          the value function
    /// @param code           the code function
    /// @return the row
    public static LocaleProperty literal(String label, String descriptionKey, Function<Locale, String> value,
                                         Function<Locale, String> code) {
        return new LocaleProperty(label, true, List.of(), descriptionKey, value, code, null);
    }
}
