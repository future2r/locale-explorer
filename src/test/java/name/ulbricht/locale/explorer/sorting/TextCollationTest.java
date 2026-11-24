package name.ulbricht.locale.explorer.sorting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class TextCollationTest {

    private static final Locale DE = Locale.forLanguageTag("de-DE");
    private static final Locale SV = Locale.forLanguageTag("sv-SE");
    private static final TextCollation.Strength TERTIARY = TextCollation.STRENGTHS.get(2);
    private static final TextCollation.Strength PRIMARY = TextCollation.STRENGTHS.getFirst();
    private static final TextCollation.Decomposition CANONICAL = TextCollation.DECOMPOSITIONS.get(1);

    @Test
    void swedishSortsUmlautsAfterZ() {
        var words = List.of("Zebra", "Äpfel", "Apfel");
        assertEquals(List.of("Apfel", "Äpfel", "Zebra"), TextCollation.sort(words, DE, TERTIARY, CANONICAL));
        assertEquals(List.of("Apfel", "Zebra", "Äpfel"), TextCollation.sort(words, SV, TERTIARY, CANONICAL));

        var rows = TextCollation.sortRows(words, TERTIARY, CANONICAL, DE);
        assertTrue(rows.get(1).highlight().test(SV));
        assertEquals("Zebra", rows.get(1).value().apply(SV));
    }

    @Test
    void equalWordsAreMarked() {
        var rows = TextCollation.sortRows(List.of("apfel", "Apfel"), PRIMARY, CANONICAL, DE);
        assertTrue(rows.get(1).value().apply(DE).startsWith("= "));
    }

    @Test
    void comparisonDependsOnStrength() {
        var rows = TextCollation.compareRows("Äpfel", "apfel", CANONICAL);
        assertTrue(rows.getFirst().value().apply(DE).startsWith("="));
        assertTrue(rows.get(1).value().apply(DE).startsWith(">"));
        assertEquals("false", TextCollation.plainComparisons("Äpfel", "apfel").get(2).value());
    }

    @Test
    void boundaries() {
        var text = "Dr. Müller kam. Er ging.";
        assertEquals(List.of("Dr. ", "Müller ", "kam. ", "Er ", "ging."),
                TextCollation.segments(text, TextCollation.Boundary.LINE, DE));
        var family = "👨‍👩‍👧";
        assertEquals(1, TextCollation.segments(family, TextCollation.Boundary.CHARACTER, DE).size());
    }

    @Test
    void normalization() {
        var facts = TextCollation.normalizationFacts("é");
        assertEquals(5, facts.size());
        assertTrue(facts.get(1).value().startsWith("é"));
        assertEquals(List.of("NFC", "false"), facts.get(1).labelArgs());
    }

    @Test
    void inputStartingWithPercentStaysPlainText() {
        var input = TextCollation.normalizationFacts("%d").getFirst();
        assertTrue(input.value().startsWith("%d"));
        assertEquals(null, input.valueKey());
    }
}
