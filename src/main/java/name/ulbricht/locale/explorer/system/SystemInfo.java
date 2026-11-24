package name.ulbricht.locale.explorer.system;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.zone.ZoneRulesProvider;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.Locales;

/// Information about the running JVM and its defaults.
public final class SystemInfo {

    private static final List<String> ENCODING_PROPERTIES =
            List.of("file.encoding", "native.encoding", "stdout.encoding", "stderr.encoding", "sun.jnu.encoding");

    private static final List<String> LOCALE_PROPERTIES = List.of(
            "user.language", "user.country", "user.script", "user.variant", "user.extensions",
            "user.language.format", "user.country.format", "user.language.display", "user.country.display",
            "user.timezone", "java.locale.providers", "java.locale.useOldISOCodes");

    private SystemInfo() {
    }

    // --- Runtime -----------------------------------------------------------------------------

    /// Returns facts about the runtime.
    ///
    /// @return the facts
    public static List<Fact> runtimeFacts() {
        var facts = new ArrayList<Fact>();
        facts.add(Fact.of("system.java", Runtime.version().toString(), "Runtime.version();"));
        facts.add(property("java.vendor"));
        facts.add(property("java.vm.name"));
        facts.add(property("javafx.runtime.version"));
        facts.add(Fact.of("system.os", System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " " + System.getProperty("os.arch"), "var os = System.getProperty(\"os.name\") + \" \" + System.getProperty(\"os.version\")\n"
                        + "        + \" \" + System.getProperty(\"os.arch\");"));
        facts.add(Fact.of("system.tzdata", ZoneRulesProvider.getVersions("UTC").lastKey(),
                "ZoneRulesProvider.getVersions(\"UTC\").lastKey();"));
        facts.add(Fact.translated("system.cldr", "system.notAvailable", "// no public API"));
        facts.add(Fact.translated("system.unicode", "system.notAvailable", "// no public API"));
        facts.add(property("java.locale.providers"));
        facts.add(Fact.of("system.arguments", String.join(" ", inputArguments()),
                "ManagementFactory.getRuntimeMXBean().getInputArguments();"));
        return facts;
    }

    private static List<String> inputArguments() {
        try {
            return ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (RuntimeException _) {
            return List.of();
        }
    }

    private static Fact property(String name) {
        var value = System.getProperty(name);
        return new Fact("system.literal", List.of(name), value == null ? "" : value,
                "System.getProperty(" + JavaCode.string(name) + ");");
    }

    // --- Defaults ----------------------------------------------------------------------------

    /// Returns the default locales, time zone and charsets.
    ///
    /// @return the facts
    public static List<Fact> defaultFacts() {
        var facts = new ArrayList<Fact>();
        facts.add(new Fact("system.literal", List.of("Locale.getDefault()"), Locale.getDefault().toLanguageTag(),
                "Locale.getDefault().toLanguageTag();"));
        for (var category : Locale.Category.values()) {
            facts.add(new Fact("system.literal", List.of("Locale.getDefault(" + category + ")"),
                    Locale.getDefault(category).toLanguageTag(), "Locale.getDefault(Locale.Category." + category + ").toLanguageTag();"));
        }
        facts.add(new Fact("system.literal", List.of("ZoneId.systemDefault()"), ZoneId.systemDefault().getId(),
                "ZoneId.systemDefault().getId();"));
        facts.add(new Fact("system.literal", List.of("TimeZone.getDefault()"), TimeZone.getDefault().getID(),
                "TimeZone.getDefault().getID();"));
        facts.add(new Fact("system.literal", List.of("Charset.defaultCharset()"), Charset.defaultCharset().name(),
                "Charset.defaultCharset();"));
        ENCODING_PROPERTIES.forEach(name -> facts.add(property(name)));
        LOCALE_PROPERTIES.forEach(name -> facts.add(property(name)));
        return facts;
    }

    // --- Availability ------------------------------------------------------------------------

    /// Returns how many locales and other values the JDK provides, and whether the main locale
    /// is among the locales.
    ///
    /// @param locale the main locale
    /// @return the facts
    public static List<Fact> availabilityFacts(Locale locale) {
        return List.of(
                available("Locale.getAvailableLocales()", Locale.getAvailableLocales(), locale),
                available("DateFormat.getAvailableLocales()", DateFormat.getAvailableLocales(), locale),
                available("DateFormatSymbols.getAvailableLocales()", DateFormatSymbols.getAvailableLocales(), locale),
                available("NumberFormat.getAvailableLocales()", NumberFormat.getAvailableLocales(), locale),
                available("DecimalFormatSymbols.getAvailableLocales()", DecimalFormatSymbols.getAvailableLocales(), locale),
                available("Collator.getAvailableLocales()", Collator.getAvailableLocales(), locale),
                available("BreakIterator.getAvailableLocales()", BreakIterator.getAvailableLocales(), locale),
                count("ZoneId.getAvailableZoneIds()", ".size()", ZoneId.getAvailableZoneIds().size()),
                count("Currency.getAvailableCurrencies()", ".size()", Currency.getAvailableCurrencies().size()),
                count("Locale.getISOCountries()", ".length", Locale.getISOCountries().length),
                count("Locale.getISOLanguages()", ".length", Locale.getISOLanguages().length));
    }

    private static Fact available(String call, Locale[] locales, Locale locale) {
        var contained = Locales.isAvailable(Set.of(locales), locale);
        return new Fact("system.literal", List.of(call), locales.length + "   ·   " + locale.toLanguageTag()
                + (contained ? " ✓" : " ✗"),
                JavaCode.withLocale(locale, Locales.isAvailableCode("available", call)));
    }

    /// A fact showing the size of a collection or array.
    ///
    /// @param call   the call returning the collection or array
    /// @param size   `.size()` for a collection, `.length` for an array
    /// @param count  the size
    /// @return the fact
    private static Fact count(String call, String size, int count) {
        return new Fact("system.literal", List.of(call), String.valueOf(count), "var count = " + call + size + ";");
    }

    // --- Changing the defaults ---------------------------------------------------------------

    /// A call that depends on the JVM defaults.
    ///
    /// @param call  the Java expression
    /// @param value computes the result with the current defaults
    public record Experiment(String call, Supplier<String> value) {

        /// Computes the result, turning exceptions into a readable text.
        ///
        /// @return the result
        public String evaluate() {
            try {
                return value.get();
            } catch (RuntimeException e) {
                return "⚠ " + e.getClass().getSimpleName();
            }
        }
    }

    private static final java.util.Date FIXED_DATE = java.util.Date.from(java.time.Instant.parse("2026-11-24T13:35:07Z"));

    /// Calls without an explicit locale or time zone, to compare their results under changed
    /// defaults. Most depend on the defaults; `String.valueOf` and `String.CASE_INSENSITIVE_ORDER`
    /// are included as controls that never change, because they are locale-independent.
    public static final List<Experiment> EXPERIMENTS = List.of(
            new Experiment("\"title\".toUpperCase()", () -> "title".toUpperCase()),
            new Experiment("\"TITLE\".toLowerCase()", () -> "TITLE".toLowerCase()),
            new Experiment("\"%.2f\".formatted(1234.5)", () -> "%.2f".formatted(1234.5)),
            new Experiment("String.valueOf(1234.5)", () -> String.valueOf(1234.5)),
            new Experiment("NumberFormat.getInstance().format(1234.5)", () -> NumberFormat.getInstance().format(1234.5)),
            new Experiment("DateFormat.getDateTimeInstance().format(date)",
                    () -> DateFormat.getDateTimeInstance().format(FIXED_DATE)),
            new Experiment("date.toString()", FIXED_DATE::toString),
            new Experiment("DateTimeFormatter.ofLocalizedDate(FULL).format(…)",
                    () -> java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL)
                            .format(FIXED_DATE.toInstant().atZone(ZoneId.systemDefault()))),
            new Experiment("Locale.GERMANY.getDisplayName()", () -> Locale.GERMANY.getDisplayName()),
            new Experiment("String.CASE_INSENSITIVE_ORDER", () -> String.valueOf(String.CASE_INSENSITIVE_ORDER.compare("I", "ı"))),
            new Experiment("Collator.getInstance().compare(\"ä\", \"z\")",
                    () -> String.valueOf(Collator.getInstance().compare("ä", "z"))));

    /// The JVM defaults captured when this class was loaded.
    public static final Defaults ORIGINAL = Defaults.current();

    /// The results of the experiments with the original defaults, computed once when this class
    /// was loaded. Showing them never has to switch the JVM defaults temporarily.
    private static final Map<Experiment, String> ORIGINAL_VALUES = EXPERIMENTS.stream()
            .collect(Collectors.toUnmodifiableMap(experiment -> experiment, Experiment::evaluate));

    /// Returns the result of an experiment with the defaults at application start.
    ///
    /// @param experiment one of [#EXPERIMENTS]
    /// @return the result
    /// @throws IllegalArgumentException if the experiment is not one of [#EXPERIMENTS]
    public static String originalValue(Experiment experiment) {
        var value = ORIGINAL_VALUES.get(experiment);
        if (value == null) {
            throw new IllegalArgumentException("Not one of the experiments: " + experiment.call());
        }
        return value;
    }

    /// A set of JVM defaults.
    ///
    /// @param locale   the default locale
    /// @param display  the default locale for display
    /// @param format   the default locale for formatting
    /// @param timeZone the default time zone
    public record Defaults(Locale locale, Locale display, Locale format, TimeZone timeZone) {

        /// Captures the current defaults.
        ///
        /// @return the defaults
        public static Defaults current() {
            return new Defaults(Locale.getDefault(), Locale.getDefault(Locale.Category.DISPLAY),
                    Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
        }

        /// Installs these defaults.
        public void apply() {
            Locale.setDefault(locale);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
            TimeZone.setDefault(timeZone);
        }

        /// Evaluates an experiment with these defaults and restores the previous defaults.
        ///
        /// @param experiment the experiment
        /// @return the result
        public String evaluate(Experiment experiment) {
            var previous = current();
            try {
                apply();
                return experiment.evaluate();
            } finally {
                previous.apply();
            }
        }
    }
}
