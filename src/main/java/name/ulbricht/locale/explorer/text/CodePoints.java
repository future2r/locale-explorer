package name.ulbricht.locale.explorer.text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/// Describes the code points of a string, to make invisible or look-alike characters visible.
public final class CodePoints {

    /// Kinds of characters that are invisible or look like other characters.
    public enum Mark {
        /// U+00A0 NO-BREAK SPACE.
        NO_BREAK_SPACE,
        /// U+202F NARROW NO-BREAK SPACE.
        NARROW_NO_BREAK_SPACE,
        /// Other special spaces: U+2009 THIN SPACE, U+2007 FIGURE SPACE.
        OTHER_SPACE,
        /// Invisible direction marks: U+200E LRM, U+200F RLM, U+061C ALM.
        DIRECTION_MARK,
        /// U+2212 MINUS SIGN, which looks like the hyphen-minus.
        MINUS_SIGN
    }

    /// A part of a text: either a run of ordinary characters or a single special character.
    ///
    /// @param text the characters
    /// @param mark the kind of special character, or `null` for ordinary text
    public record Segment(String text, Mark mark) {
    }

    private CodePoints() {
    }

    /// Lists the code points of a string in `U+XXXX` notation.
    ///
    /// @param text the text
    /// @return the code points separated by spaces, for example `U+0031 U+202F U+0032`
    public static String describe(String text) {
        return text.codePoints()
                .mapToObj(codePoint -> "U+%04X".formatted(codePoint))
                .collect(Collectors.joining(" "));
    }

    /// Lists the code points of a string together with their Unicode names.
    ///
    /// @param text the text
    /// @return one line per code point, for example `U+202F NARROW NO-BREAK SPACE`
    public static String describeWithNames(String text) {
        return text.codePoints()
                .mapToObj(codePoint -> "U+%04X %s".formatted(codePoint, nameOf(codePoint)))
                .collect(Collectors.joining("\n"));
    }

    /// Returns the kind of a special character.
    ///
    /// @param codePoint the code point
    /// @return the kind, or `null` for an ordinary character
    public static Mark markOf(int codePoint) {
        return switch (codePoint) {
            case 0x00A0 -> Mark.NO_BREAK_SPACE;
            case 0x202F -> Mark.NARROW_NO_BREAK_SPACE;
            case 0x2009, 0x2007 -> Mark.OTHER_SPACE;
            case 0x200E, 0x200F, 0x061C -> Mark.DIRECTION_MARK;
            case 0x2212 -> Mark.MINUS_SIGN;
            default -> null;
        };
    }

    /// Splits a text into runs of ordinary characters and single special characters.
    ///
    /// @param text the text
    /// @return the segments in text order; joined, they give the original text
    public static List<Segment> segments(String text) {
        var segments = new ArrayList<Segment>();
        var plain = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            var mark = markOf(codePoint);
            if (mark == null) {
                plain.appendCodePoint(codePoint);
                return;
            }
            if (!plain.isEmpty()) {
                segments.add(new Segment(plain.toString(), null));
                plain.setLength(0);
            }
            segments.add(new Segment(Character.toString(codePoint), mark));
        });
        if (!plain.isEmpty()) {
            segments.add(new Segment(plain.toString(), null));
        }
        return List.copyOf(segments);
    }

    /// Returns `true` if the text contains characters that [#segments] marks.
    ///
    /// @param text the text
    /// @return whether the text contains special characters
    public static boolean hasSpecialCharacters(String text) {
        return text.codePoints().anyMatch(codePoint -> markOf(codePoint) != null);
    }

    private static String nameOf(int codePoint) {
        var name = Character.getName(codePoint);
        return name == null ? "" : name;
    }
}
