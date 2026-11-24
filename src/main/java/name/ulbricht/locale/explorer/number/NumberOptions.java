package name.ulbricht.locale.explorer.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

/// The options applied to every [NumberFormat] in the formats tab.
///
/// @param minimumIntegerDigits  the minimum number of integer digits
/// @param minimumFractionDigits the minimum number of fraction digits
/// @param maximumFractionDigits the maximum number of fraction digits
/// @param roundingMode          the rounding mode
/// @param grouping              whether grouping separators are used
public record NumberOptions(int minimumIntegerDigits, int minimumFractionDigits, int maximumFractionDigits,
                            RoundingMode roundingMode, boolean grouping) {

    /// The options a fresh number instance uses for the German locale.
    public static final NumberOptions DEFAULT = new NumberOptions(1, 0, 3, RoundingMode.HALF_EVEN, true);

    public NumberOptions {
        maximumFractionDigits = Math.max(maximumFractionDigits, minimumFractionDigits);
    }

    /// Applies the options to a format.
    ///
    /// @param format the format
    /// @param <F>    the type of the format
    /// @return the same format
    public <F extends NumberFormat> F applyTo(F format) {
        format.setMinimumIntegerDigits(minimumIntegerDigits);
        format.setMinimumFractionDigits(minimumFractionDigits);
        format.setMaximumFractionDigits(maximumFractionDigits);
        format.setRoundingMode(roundingMode);
        format.setGroupingUsed(grouping);
        return format;
    }

    /// Returns the Java statements that apply the options to a variable `format`.
    ///
    /// @return the statements, one per line
    public String code() {
        return String.join("\n",
                "format.setMinimumIntegerDigits(" + minimumIntegerDigits + ");",
                "format.setMinimumFractionDigits(" + minimumFractionDigits + ");",
                "format.setMaximumFractionDigits(" + maximumFractionDigits + ");",
                "format.setRoundingMode(RoundingMode." + roundingMode + ");",
                "format.setGroupingUsed(" + grouping + ");");
    }

    /// Parses a number for the examples: a decimal number, `NaN` or `±Infinity`.
    ///
    /// @param text the text
    /// @return a [BigDecimal], or a [Double] for the special values
    /// @throws NumberFormatException if the text is not a number
    public static Number parseValue(String text) {
        var trimmed = text.strip();
        return switch (trimmed) {
            case "NaN" -> Double.NaN;
            case "Infinity", "+Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            default -> parseDecimal(trimmed);
        };
    }

    /// Largest number of fraction digits accepted by [#parseDecimal].
    public static final int MAX_FRACTION_DIGITS = 1000;

    private static final BigDecimal LARGEST = BigDecimal.valueOf(Double.MAX_VALUE);

    /// Parses a decimal number whose magnitude is at most [Double#MAX_VALUE], with at most
    /// [#MAX_FRACTION_DIGITS] fraction digits. Very small values such as `1E-1000` are accepted;
    /// formats computing with `double` show them as 0.
    ///
    /// The limits keep a value such as `1e999999999` from producing a billion-digit text when it
    /// is formatted, and keep values within the range that formats computing with `double`,
    /// such as [java.text.CompactNumberFormat], can handle.
    ///
    /// @param text the text
    /// @return the number
    /// @throws NumberFormatException if the text is not a number, too large or too precise
    public static BigDecimal parseDecimal(String text) {
        var value = new BigDecimal(text.strip());
        if (value.abs().compareTo(LARGEST) > 0 || value.scale() > MAX_FRACTION_DIGITS) {
            throw new NumberFormatException("Out of range: " + text.strip());
        }
        return value;
    }

    /// Returns the Java literal of a value as used in the code panel.
    ///
    /// @param value the value
    /// @return for example `new BigDecimal("-1234567.891")`
    public static String literal(Number value) {
        if (value instanceof Double d) {
            return d.isNaN() ? "Double.NaN" : d > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
        }
        return "new BigDecimal(\"" + value + "\")";
    }
}
