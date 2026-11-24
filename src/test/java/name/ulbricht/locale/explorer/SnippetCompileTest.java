package name.ulbricht.locale.explorer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import name.ulbricht.locale.explorer.calendar.Chronologies;
import name.ulbricht.locale.explorer.calendar.WeekRules;
import name.ulbricht.locale.explorer.currency.Currencies;
import name.ulbricht.locale.explorer.datetime.DateTimeFormats;
import name.ulbricht.locale.explorer.datetime.DateTimeNames;
import name.ulbricht.locale.explorer.datetime.DateTimeParsing;
import name.ulbricht.locale.explorer.datetime.LegacyDateFormats;
import name.ulbricht.locale.explorer.datetime.TemporalKind;
import name.ulbricht.locale.explorer.lists.TextFormats;
import name.ulbricht.locale.explorer.locale.LocaleDetails;
import name.ulbricht.locale.explorer.locale.LocaleExtensions;
import name.ulbricht.locale.explorer.number.NumberFormats;
import name.ulbricht.locale.explorer.number.NumberOptions;
import name.ulbricht.locale.explorer.sorting.TextCollation;
import name.ulbricht.locale.explorer.system.SystemInfo;
import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.zone.TimeZones;

/// Compiles the Java code shown in the code panel, so that every snippet a user copies compiles.
///
/// The snippets are compiled with `javac` of the running JDK in a separate process.
/// `TextCollation.boundaryRows` is left out on purpose: its code elides the loop body.
class SnippetCompileTest {

    private static final List<Locale> LOCALES = List.of(
            Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("ar-EG"),
            Locale.forLanguageTag("ja-JP-u-ca-japanese"));

    private static final String IMPORTS = """
            import java.lang.management.*;
            import java.math.*;
            import java.nio.charset.*;
            import java.text.*;
            import java.time.*;
            import java.time.chrono.*;
            import java.time.format.*;
            import java.time.temporal.*;
            import java.time.zone.*;
            import java.util.*;
            import java.util.stream.*;
            """;

    @Test
    void snippetsCompile(@TempDir Path directory) throws IOException, InterruptedException {
        var snippets = new ArrayList<String>();
        var dateTime = ZonedDateTime.of(2026, 11, 24, 14, 35, 7, 0, ZoneId.of("Europe/Berlin"));
        var instant = dateTime.toInstant();
        var berlin = ZoneId.of("Europe/Berlin");

        var rows = new ArrayList<LocaleProperty>();
        rows.addAll(LocaleDetails.rows(Locale.GERMAN));
        rows.addAll(LocaleExtensions.previewRows(dateTime));
        for (var kind : TemporalKind.values()) {
            rows.addAll(DateTimeFormats.styleRows(dateTime, kind, true, true));
        }
        rows.addAll(DateTimeFormats.skeletonRows(dateTime, List.of("yMMMd", "jm")));
        rows.addAll(DateTimeFormats.patternRows(dateTime, "EEEE, d. MMMM y"));
        for (var parser : DateTimeParsing.Parser.standard()) {
            rows.addAll(DateTimeParsing.rows("24.11.26", parser, ResolverStyle.SMART, dateTime));
        }
        rows.addAll(LegacyDateFormats.rows(dateTime));
        rows.addAll(Chronologies.rows(LocalDate.of(2026, 11, 24)));
        rows.addAll(WeekRules.rows(LocalDate.of(2026, 11, 24)));
        rows.addAll(WeekRules.turnOfYearRows(2026, Locale.GERMAN));
        rows.addAll(TimeZones.nameRows(berlin, 2026));
        rows.addAll(TimeZones.worldClockRows(List.of(berlin, ZoneId.of("Asia/Kolkata")), instant));
        rows.addAll(NumberFormats.formatRows(new BigDecimal("-1234567.891"), NumberOptions.DEFAULT));
        rows.addAll(NumberFormats.symbolRows());
        rows.addAll(NumberFormats.patternRows(new BigDecimal("-1234567.891"), "#,##0.00", false));
        rows.addAll(NumberFormats.compactRows(1));
        rows.addAll(NumberFormats.numberingRows(new BigDecimal("1234.5")));
        for (var kind : NumberFormats.Kind.values()) {
            rows.addAll(NumberFormats.parseRows("1.234,5", kind, false, false));
        }
        rows.addAll(Currencies.formatRows(new BigDecimal("-1234.567"), Currency.getInstance("EUR"), 2));
        rows.addAll(Currencies.localeCurrencyRows(Locale.GERMAN));
        rows.addAll(Currencies.symbolRows(List.of("EUR", "USD")));
        rows.addAll(TextFormats.listRows(List.of("Äpfel", "Birnen")));
        rows.addAll(TextFormats.messageRows("{0} hat {1,number,integer} Dateien.", "Anna", dateTime, 3));
        rows.addAll(TextFormats.caseRows(List.of("title", "ǅ")));
        rows.addAll(TextFormats.formatRows(TextFormats.FORMAT_PATTERNS, -1234.5, dateTime, "Text", 42));
        rows.addAll(TextCollation.sortRows(List.of("a", "ä", "z"), TextCollation.STRENGTHS.getFirst(),
                TextCollation.DECOMPOSITIONS.getFirst(), Locale.GERMAN));
        rows.addAll(TextCollation.compareRows("a", "ä", TextCollation.DECOMPOSITIONS.getFirst()));
        for (var row : rows) {
            for (var locale : LOCALES) {
                snippets.add(row.code().apply(locale));
            }
        }

        var facts = new ArrayList<Fact>();
        facts.addAll(SystemInfo.runtimeFacts());
        facts.addAll(SystemInfo.defaultFacts());
        facts.addAll(SystemInfo.availabilityFacts(Locale.forLanguageTag("de-DE-u-nu-arab")));
        facts.addAll(TimeZones.ruleFacts(berlin, instant));
        facts.addAll(TimeZones.transitionFacts(berlin, instant, 3));
        facts.addAll(TimeZones.resolutionFacts(LocalDateTime.of(2027, 3, 28, 2, 30), berlin));
        facts.addAll(TextCollation.plainComparisons("a", "ä"));
        facts.addAll(TextCollation.normalizationFacts("é"));
        facts.forEach(fact -> snippets.add(fact.code()));

        for (var kind : DateTimeNames.Kind.values()) {
            for (var sample : kind.samples()) {
                snippets.add(kind.code(sample, DateTimeNames.STYLES.getFirst(), Locale.GERMAN));
            }
        }

        var source = new StringBuilder(IMPORTS).append("class Snippets {\n");
        for (var i = 0; i < snippets.size(); i++) {
            source.append("static void s").append(i).append("() throws Exception {\n")
                    .append(snippets.get(i)).append("\n}\n");
        }
        source.append("}\n");
        var file = directory.resolve("Snippets.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);

        var javac = Path.of(System.getProperty("java.home"), "bin", "javac").toString();
        var process = new ProcessBuilder(javac, "-encoding", "UTF-8", "-proc:none", "-d", directory.toString(), file.toString())
                .redirectErrorStream(true)
                .start();
        // reading the whole output waits until javac has ended
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        assertEquals(0, process.exitValue(), snippets.size() + " snippets, javac output:\n" + output);
    }

}
