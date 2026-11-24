package name.ulbricht.locale.explorer.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class JavaCodeTest {

    @Test
    void localeExpression() {
        assertEquals("Locale.forLanguageTag(\"de-CH-u-nu-latn\")",
                JavaCode.locale(Locale.forLanguageTag("de-CH-u-nu-latn")));
        assertEquals("Locale.ROOT", JavaCode.locale(Locale.ROOT));
    }

    @Test
    void stringLiteralEscapesInvisibleCharacters() {
        assertEquals("\"1\\u202F234 \\\"x\\\"\"", JavaCode.string("1 234 \"x\""));
    }

    @Test
    void stringLiteralUsesEscapeSequencesForLineBreaks() {
        // a unicode escape for CR would not compile inside a string literal
        assertEquals("\"a\\r\\nb\\tc\"", JavaCode.string("a\r\nb\tc"));
    }

    @Test
    void codePoints() {
        assertEquals("U+0031 U+202F U+1F600", CodePoints.describe("1 😀"));
        assertEquals("U+00DF LATIN SMALL LETTER SHARP S", CodePoints.describeWithNames("ß"));
    }
}
