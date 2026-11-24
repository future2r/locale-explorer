package name.ulbricht.locale.explorer.sorting;

import java.text.BreakIterator;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import name.ulbricht.locale.explorer.text.CodePoints;
import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;

/// Collation, text boundaries and normalization.
public final class TextCollation {

    /// Locales with notable sorting rules, used unless the toolbar locales are chosen.
    public static final List<Locale> SORT_LOCALES = Stream.of("de-DE", "sv-SE", "da-DK", "es-ES", "tr-TR", "cs-CZ", "fr-FR")
            .map(Locale::forLanguageTag).toList();

    /// A collator strength with its name.
    ///
    /// @param name  the constant name, for example `PRIMARY`
    /// @param value the value of the constant
    public record Strength(String name, int value) {
    }

    /// The collator strengths from weakest to strongest.
    public static final List<Strength> STRENGTHS = List.of(
            new Strength("PRIMARY", Collator.PRIMARY), new Strength("SECONDARY", Collator.SECONDARY),
            new Strength("TERTIARY", Collator.TERTIARY), new Strength("IDENTICAL", Collator.IDENTICAL));

    /// A decomposition mode with its name.
    ///
    /// @param name  the constant name
    /// @param value the value of the constant
    public record Decomposition(String name, int value) {
    }

    /// The decomposition modes.
    public static final List<Decomposition> DECOMPOSITIONS = List.of(
            new Decomposition("NO_DECOMPOSITION", Collator.NO_DECOMPOSITION),
            new Decomposition("CANONICAL_DECOMPOSITION", Collator.CANONICAL_DECOMPOSITION),
            new Decomposition("FULL_DECOMPOSITION", Collator.FULL_DECOMPOSITION));

    /// The kinds of text boundaries.
    public enum Boundary {
        CHARACTER, WORD, SENTENCE, LINE;

        BreakIterator create(Locale locale) {
            return switch (this) {
                case CHARACTER -> BreakIterator.getCharacterInstance(locale);
                case WORD -> BreakIterator.getWordInstance(locale);
                case SENTENCE -> BreakIterator.getSentenceInstance(locale);
                case LINE -> BreakIterator.getLineInstance(locale);
            };
        }

        String factory() {
            var name = name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
            return "BreakIterator.get" + name + "Instance(locale)";
        }
    }

    private TextCollation() {
    }

    private static Collator collator(Locale locale, Strength strength, Decomposition decomposition) {
        var collator = Collator.getInstance(locale);
        collator.setStrength(strength.value());
        collator.setDecomposition(decomposition.value());
        return collator;
    }

    private static String collatorCode(Locale locale, Strength strength, Decomposition decomposition, String... lines) {
        var all = new ArrayList<String>();
        all.add("var collator = Collator.getInstance(locale);");
        all.add("collator.setStrength(Collator." + strength.name() + ");");
        all.add("collator.setDecomposition(Collator." + decomposition.name() + ");");
        all.addAll(List.of(lines));
        return JavaCode.withLocale(locale, all.toArray(String[]::new));
    }

    // --- Sorting -----------------------------------------------------------------------------

    /// Sorts words with the collator of a locale.
    ///
    /// @param words         the words
    /// @param locale        the locale
    /// @param strength      the strength
    /// @param decomposition the decomposition mode
    /// @return the sorted words
    public static List<String> sort(List<String> words, Locale locale, Strength strength, Decomposition decomposition) {
        return words.stream().sorted(collator(locale, strength, decomposition)).toList();
    }

    /// Returns one row per position in the sorted list.
    ///
    /// The label shows the word at that position in the order of [String#compareTo], which
    /// compares UTF-16 units, so supplementary characters sort by their surrogates. A
    /// value starts with `=` if it is equal to the previous word at the given strength and is
    /// highlighted if it differs from the word at that position for the reference locale.
    ///
    /// @param words         the words
    /// @param strength      the strength
    /// @param decomposition the decomposition mode
    /// @param reference     the locale the other columns are compared with
    /// @return the rows
    public static List<LocaleProperty> sortRows(List<String> words, Strength strength, Decomposition decomposition,
                                                Locale reference) {
        var codePointOrder = words.stream().sorted().toList();
        var referenceOrder = sort(words, reference, strength, decomposition);
        var wordsCode = words.stream().map(JavaCode::string).reduce((a, b) -> a + ", " + b).orElse("");
        var rows = new ArrayList<LocaleProperty>();
        for (var i = 0; i < words.size(); i++) {
            var index = i;
            rows.add(LocaleProperty.literal((i + 1) + ".  " + codePointOrder.get(i),
                            locale -> {
                                var collator = collator(locale, strength, decomposition);
                                var sorted = words.stream().sorted(collator).toList();
                                var equal = index > 0 && collator.compare(sorted.get(index - 1), sorted.get(index)) == 0;
                                return (equal ? "= " : "") + sorted.get(index);
                            },
                            locale -> collatorCode(locale, strength, decomposition,
                                    "var words = new ArrayList<>(List.of(" + wordsCode + "));",
                                    "words.sort(collator);"))
                    .withHighlight(locale -> !sort(words, locale, strength, decomposition).get(index)
                            .equals(referenceOrder.get(index))));
        }
        return rows;
    }

    // --- Comparing ---------------------------------------------------------------------------

    /// Returns one row per strength with the comparison result of two texts, followed by the
    /// collation keys.
    ///
    /// @param first         the first text
    /// @param second        the second text
    /// @param decomposition the decomposition mode
    /// @return the rows
    public static List<LocaleProperty> compareRows(String first, String second, Decomposition decomposition) {
        var compareCode = "collator.compare(" + JavaCode.string(first) + ", " + JavaCode.string(second) + ");";
        var rows = new ArrayList<LocaleProperty>();
        for (var strength : STRENGTHS) {
            rows.add(LocaleProperty.literal(strength.name(),
                    locale -> symbol(collator(locale, strength, decomposition).compare(first, second)),
                    locale -> collatorCode(locale, strength, decomposition, compareCode)));
        }
        Function<String, LocaleProperty> keyRow = text -> LocaleProperty.literal("CollationKey " + JavaCode.string(text),
                locale -> HexFormat.ofDelimiter(" ").formatHex(
                        collator(locale, STRENGTHS.get(2), decomposition).getCollationKey(text).toByteArray()),
                locale -> collatorCode(locale, STRENGTHS.get(2), decomposition,
                        "collator.getCollationKey(" + JavaCode.string(text) + ").toByteArray();"));
        rows.add(keyRow.apply(first));
        rows.add(keyRow.apply(second));
        return rows;
    }

    /// Returns locale-independent comparisons of two texts.
    ///
    /// @param first  the first text
    /// @param second the second text
    /// @return the facts
    public static List<Fact> plainComparisons(String first, String second) {
        var a = JavaCode.string(first);
        var b = JavaCode.string(second);
        return List.of(
                Fact.of("sort.compareTo", symbol(first.compareTo(second)), a + ".compareTo(" + b + ");"),
                Fact.of("sort.compareToIgnoreCase", symbol(first.compareToIgnoreCase(second)), a + ".compareToIgnoreCase(" + b + ");"),
                Fact.of("sort.equalsIgnoreCase", String.valueOf(first.equalsIgnoreCase(second)), a + ".equalsIgnoreCase(" + b + ");"));
    }

    private static String symbol(int comparison) {
        return comparison < 0 ? "<  (" + comparison + ")" : comparison > 0 ? ">  (" + comparison + ")" : "=  (0)";
    }

    // --- Text boundaries ---------------------------------------------------------------------

    /// Splits a text at the boundaries of the given kind.
    ///
    /// @param text     the text
    /// @param boundary the kind of boundary
    /// @param locale   the locale
    /// @return the segments, including whitespace segments
    public static List<String> segments(String text, Boundary boundary, Locale locale) {
        var iterator = boundary.create(locale);
        iterator.setText(text);
        var segments = new ArrayList<String>();
        var start = iterator.first();
        for (var end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            segments.add(text.substring(start, end));
        }
        return segments;
    }

    /// Returns one row per kind of boundary and a row with the different lengths.
    ///
    /// @param text the text
    /// @return the rows
    public static List<LocaleProperty> boundaryRows(String text) {
        var rows = new ArrayList<LocaleProperty>();
        for (var boundary : Boundary.values()) {
            rows.add(LocaleProperty.literal(boundary.name(),
                    locale -> {
                        var segments = segments(text, boundary, locale).stream()
                                .filter(segment -> boundary == Boundary.CHARACTER || !segment.isBlank())
                                .toList();
                        return segments.size() + ":  " + String.join(" | ", segments.stream().map(String::strip).toList());
                    },
                    locale -> JavaCode.withLocale(locale,
                            "var iterator = " + boundary.factory() + ";",
                            "iterator.setText(" + JavaCode.string(text) + ");",
                            "for (var end = iterator.next(); end != BreakIterator.DONE; end = iterator.next()) { … }")));
        }
        rows.add(LocaleProperty.of("sort.lengths",
                locale -> "length() " + text.length() + "  ·  codePointCount " + text.codePointCount(0, text.length())
                        + "  ·  " + segments(text, Boundary.CHARACTER, locale).size(),
                locale -> JavaCode.withLocale(locale,
                        "var text = " + JavaCode.string(text) + ";",
                        "text.length();",
                        "text.codePointCount(0, text.length());",
                        "// number of BreakIterator.getCharacterInstance(locale) segments")));
        return rows;
    }

    // --- Normalization -----------------------------------------------------------------------

    /// Returns one fact per normalization form.
    ///
    /// @param text the text
    /// @return the facts
    public static List<Fact> normalizationFacts(String text) {
        var facts = new ArrayList<Fact>();
        facts.add(new Fact("sort.normalization.input", List.of(), describe(text), "var text = " + JavaCode.string(text) + ";"));
        for (var form : List.of(Normalizer.Form.NFC, Normalizer.Form.NFD, Normalizer.Form.NFKC, Normalizer.Form.NFKD)) {
            var normalized = Normalizer.normalize(text, form);
            facts.add(new Fact("sort.normalization.form", List.of(form.name(), String.valueOf(Normalizer.isNormalized(text, form))),
                    describe(normalized),
                    "Normalizer.normalize(" + JavaCode.string(text) + ", Normalizer.Form." + form + ");\n"
                            + "Normalizer.isNormalized(" + JavaCode.string(text) + ", Normalizer.Form." + form + ");"));
        }
        return facts;
    }

    private static String describe(String text) {
        return text + "     [" + text.length() + "]  " + CodePoints.describe(text);
    }
}
