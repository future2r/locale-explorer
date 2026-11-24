package name.ulbricht.locale.explorer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.CodePoints.Mark;
import name.ulbricht.locale.explorer.text.CodePoints.Segment;

class CodePointsTest {

    @Test
    void segmentsSeparateSpecialCharacters() {
        assertEquals(List.of(
                        new Segment("−", Mark.MINUS_SIGN),
                        new Segment("1", null),
                        new Segment(" ", Mark.NARROW_NO_BREAK_SPACE),
                        new Segment("234,5", null),
                        new Segment(" ", Mark.NO_BREAK_SPACE),
                        new Segment("%", null)),
                CodePoints.segments("−1 234,5 %"));
    }

    @Test
    void segmentsJoinToTheOriginalText() {
        var text = "؜-١٬٢ x‏";
        var joined = String.join("", CodePoints.segments(text).stream().map(Segment::text).toList());
        assertEquals(text, joined);
    }

    @Test
    void ordinaryTextIsOneSegment() {
        assertEquals(List.of(new Segment("1,234.5", null)), CodePoints.segments("1,234.5"));
        assertEquals(List.of(), CodePoints.segments(""));
        assertFalse(CodePoints.hasSpecialCharacters("1 234-5"));
        assertTrue(CodePoints.hasSpecialCharacters("1‎"));
    }
}
