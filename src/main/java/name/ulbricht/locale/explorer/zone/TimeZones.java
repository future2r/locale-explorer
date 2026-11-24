package name.ulbricht.locale.explorer.zone;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import name.ulbricht.locale.explorer.text.Fact;
import name.ulbricht.locale.explorer.text.JavaCode;
import name.ulbricht.locale.explorer.text.LocaleProperty;
import name.ulbricht.locale.explorer.time.LocalTimeResolution;

/// Names, rules and transitions of time zones.
public final class TimeZones {

    /// A local date-time near a transition that is worth trying.
    ///
    /// @param dateTime the local date-time
    /// @param gap      `true` if it lies in a gap, `false` if in an overlap
    public record Example(LocalDateTime dateTime, boolean gap) {
    }

    private static final Map<String, String> SHORT_ID_BY_ZONE = ZoneId.SHORT_IDS.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey,
                    (first, second) -> first + ", " + second, TreeMap::new));

    private TimeZones() {
    }

    // --- Names -------------------------------------------------------------------------------

    /// Returns the rows of the names tab.
    ///
    /// Each value shows the name in January and, if different, in July, so that standard and
    /// daylight saving names appear.
    ///
    /// @param zone the zone
    /// @param year the year of the two sample dates
    /// @return the rows
    public static List<LocaleProperty> nameRows(ZoneId zone, int year) {
        var january = ZonedDateTime.of(year, 1, 15, 12, 0, 0, 0, zone);
        var july = ZonedDateTime.of(year, 7, 15, 12, 0, 0, 0, zone);
        var rows = new ArrayList<LocaleProperty>();
        for (var pattern : List.of("z", "zzzz", "v", "vvvv", "O", "OOOO", "VV")) {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            rows.add(LocaleProperty.literal(pattern,
                    locale -> both(formatter.localizedBy(locale).format(january), formatter.localizedBy(locale).format(july)),
                    locale -> JavaCode.withLocale(locale,
                            "var zone = ZoneId.of(" + JavaCode.string(zone.getId()) + ");",
                            "var formatter = DateTimeFormatter.ofPattern(" + JavaCode.string(pattern) + ").localizedBy(locale);",
                            "formatter.format(ZonedDateTime.of(" + year + ", 1, 15, 12, 0, 0, 0, zone)); // January",
                            "formatter.format(ZonedDateTime.of(" + year + ", 7, 15, 12, 0, 0, 0, zone)); // July")));
        }
        for (var style : List.of(TextStyle.FULL, TextStyle.SHORT, TextStyle.NARROW)) {
            rows.add(LocaleProperty.literal("getDisplayName(" + style + ")",
                    locale -> zone.getDisplayName(style, locale),
                    locale -> JavaCode.withLocale(locale,
                            "ZoneId.of(" + JavaCode.string(zone.getId()) + ").getDisplayName(TextStyle." + style + ", locale);")));
        }
        return rows;
    }

    private static String both(String first, String second) {
        return first.equals(second) ? first : first + "  /  " + second;
    }

    // --- Rules -------------------------------------------------------------------------------

    /// Returns the rules of a zone at an instant.
    ///
    /// @param zone    the zone
    /// @param instant the instant
    /// @return the facts
    public static List<Fact> ruleFacts(ZoneId zone, Instant instant) {
        var rules = zone.getRules();
        var zoneCode = "var rules = ZoneId.of(" + JavaCode.string(zone.getId()) + ").getRules();\n"
                + "var instant = Instant.parse(" + JavaCode.string(instant.toString()) + ");\n";
        return List.of(
                Fact.of("zone.rule.offset", rules.getOffset(instant).getId(), zoneCode + "rules.getOffset(instant);"),
                Fact.of("zone.rule.standardOffset", rules.getStandardOffset(instant).getId(),
                        zoneCode + "rules.getStandardOffset(instant);"),
                Fact.of("zone.rule.daylightSavings", String.valueOf(rules.isDaylightSavings(instant)),
                        zoneCode + "rules.isDaylightSavings(instant);"),
                Fact.of("zone.rule.daylightSavingsAmount", rules.getDaylightSavings(instant).toString(),
                        zoneCode + "rules.getDaylightSavings(instant);"),
                Fact.of("zone.rule.fixed", String.valueOf(rules.isFixedOffset()), zoneCode + "rules.isFixedOffset();"),
                Fact.of("zone.rule.transitionRules", String.valueOf(rules.getTransitionRules().size()),
                        zoneCode + "rules.getTransitionRules().size();"),
                Fact.of("zone.rule.shortId", SHORT_ID_BY_ZONE.getOrDefault(zone.getId(), ""),
                        "ZoneId.SHORT_IDS.entrySet().stream()\n"
                                + "        .filter(entry -> entry.getValue().equals(" + JavaCode.string(zone.getId()) + "))\n"
                                + "        .map(Map.Entry::getKey)\n"
                                + "        .sorted()\n"
                                + "        .collect(Collectors.joining(\", \"));"),
                Fact.of("zone.rule.version", rulesVersion(zone),
                        "// throws ZoneRulesException for zones without tzdb rules, such as offsets\n"
                                + "ZoneRulesProvider.getVersions(" + JavaCode.string(zone.getId()) + ").lastKey();"));
    }

    /// Returns the tzdb version of the rules of a zone.
    ///
    /// @param zone the zone
    /// @return the version, or an empty text for zones without tzdb rules, such as `+05:30`
    static String rulesVersion(ZoneId zone) {
        try {
            return ZoneRulesProvider.getVersions(zone.getId()).lastKey();
        } catch (ZoneRulesException _) {
            return "";
        }
    }

    /// Returns the next transitions of a zone.
    ///
    /// @param zone    the zone
    /// @param instant the instant to start from
    /// @param count   the maximum number of transitions
    /// @return the facts, one per transition
    public static List<Fact> transitionFacts(ZoneId zone, Instant instant, int count) {
        var facts = new ArrayList<Fact>();
        var from = instant;
        for (var transition : nextTransitions(zone, instant, count)) {
            // each row gets the call that returns exactly its transition
            var code = "var rules = ZoneId.of(" + JavaCode.string(zone.getId()) + ").getRules();\n"
                    + "var transition = rules.nextTransition(Instant.parse(" + JavaCode.string(from.toString()) + "));\n"
                    + "transition.getDateTimeBefore();\ntransition.getDateTimeAfter();\ntransition.isGap();\n"
                    + "transition.getOffsetBefore();\ntransition.getOffsetAfter();\ntransition.getDuration();";
            from = transition.getInstant();
            facts.add(new Fact(transition.isGap() ? "zone.transition.gap" : "zone.transition.overlap",
                    List.of(transition.getDateTimeBefore().toString(), transition.getDateTimeAfter().toString()),
                    transition.getOffsetBefore() + " → " + transition.getOffsetAfter()
                            + "  (" + transition.getDuration() + ")",
                    code));
        }
        return facts;
    }

    /// Returns the next transitions of a zone.
    ///
    /// @param zone    the zone
    /// @param instant the instant to start from
    /// @param count   the maximum number of transitions
    /// @return the transitions
    public static List<ZoneOffsetTransition> nextTransitions(ZoneId zone, Instant instant, int count) {
        var transitions = new ArrayList<ZoneOffsetTransition>();
        var rules = zone.getRules();
        var transition = rules.nextTransition(instant);
        while (transition != null && transitions.size() < count) {
            transitions.add(transition);
            transition = rules.nextTransition(transition.getInstant());
        }
        return transitions;
    }

    /// Returns local date-times in the next gaps and overlaps of a zone.
    ///
    /// @param zone    the zone
    /// @param instant the instant to start from
    /// @return the examples
    public static List<Example> examples(ZoneId zone, Instant instant) {
        return nextTransitions(zone, instant, 4).stream()
                .map(transition -> {
                    var half = transition.getDuration().abs().dividedBy(2);
                    var start = transition.isGap() ? transition.getDateTimeBefore() : transition.getDateTimeAfter();
                    return new Example(start.plus(half), transition.isGap());
                })
                .toList();
    }

    /// Returns whether a zone observes daylight saving time at some point in a year.
    ///
    /// Every offset in effect during the year is checked, so zones whose daylight saving time
    /// does not differ between January and July (such as `Africa/Casablanca`, whose offset is the
    /// same on both dates because it changes only around Ramadan) count
    /// too, while a one-off change of the standard offset does not.
    ///
    /// @param zone the zone
    /// @param year the year
    /// @return whether the zone observes daylight saving time in that year
    public static boolean hasDaylightSaving(ZoneId zone, int year) {
        var rules = zone.getRules();
        var start = Year.of(year).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var end = Year.of(year + 1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (rules.isDaylightSavings(start)) {
            return true;
        }
        for (var transition = rules.nextTransition(start);
                transition != null && transition.getInstant().isBefore(end);
                transition = rules.nextTransition(transition.getInstant())) {
            if (rules.isDaylightSavings(transition.getInstant())) {
                return true;
            }
        }
        return false;
    }

    /// Returns whether an offset is not a whole number of hours.
    ///
    /// @param offset the offset
    /// @return `true` for offsets such as +05:30 or +05:45
    public static boolean isUnusual(ZoneOffset offset) {
        return offset.getTotalSeconds() % 3600 != 0;
    }

    /// Returns the deprecated short IDs that map to a zone.
    ///
    /// @param zone the zone
    /// @return the short IDs, for example `ECT`, or an empty string
    public static String shortIds(ZoneId zone) {
        return SHORT_ID_BY_ZONE.getOrDefault(zone.getId(), "");
    }

    // --- Gap and overlap ---------------------------------------------------------------------

    /// Describes how a local date-time is resolved in a zone.
    ///
    /// @param dateTime the local date-time
    /// @param zone     the zone
    /// @return the facts
    public static List<Fact> resolutionFacts(LocalDateTime dateTime, ZoneId zone) {
        var rules = zone.getRules();
        var offsets = rules.getValidOffsets(dateTime);
        var resolution = LocalTimeResolution.of(dateTime, zone);
        var zoned = resolution.resolved();
        var transition = rules.getTransition(dateTime);
        var code = "var dateTime = LocalDateTime.parse(" + JavaCode.string(dateTime.toString()) + ");\n"
                + "var zone = ZoneId.of(" + JavaCode.string(zone.getId()) + ");\n";
        var kind = "zone.kind." + resolution.kind().name().toLowerCase(Locale.ROOT);
        var facts = new ArrayList<Fact>();
        facts.add(Fact.translated("zone.res.kind", kind, code + "zone.getRules().getValidOffsets(dateTime).size();"));
        facts.add(Fact.of("zone.res.validOffsets", offsets.stream().map(ZoneOffset::getId).collect(Collectors.joining(", ")),
                code + "zone.getRules().getValidOffsets(dateTime);"));
        facts.add(Fact.of("zone.res.of", zoned.toString(), code + "ZonedDateTime.of(dateTime, zone);"));
        facts.add(Fact.of("zone.res.earlier", zoned.withEarlierOffsetAtOverlap().toString(),
                code + "ZonedDateTime.of(dateTime, zone).withEarlierOffsetAtOverlap();"));
        facts.add(Fact.of("zone.res.later", zoned.withLaterOffsetAtOverlap().toString(),
                code + "ZonedDateTime.of(dateTime, zone).withLaterOffsetAtOverlap();"));
        facts.add(Fact.of("zone.res.instant", zoned.toInstant().toString(),
                code + "ZonedDateTime.of(dateTime, zone).toInstant();"));
        facts.add(Fact.of("zone.res.transition", transition == null ? "" : transition.toString(),
                code + "zone.getRules().getTransition(dateTime);"));
        return facts;
    }

    // --- World clock -------------------------------------------------------------------------

    /// Returns one row per zone with the instant in that zone, formatted per locale.
    ///
    /// @param zones   the zones
    /// @param instant the instant
    /// @return the rows
    public static List<LocaleProperty> worldClockRows(List<ZoneId> zones, Instant instant) {
        var formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.LONG);
        return zones.stream()
                .map(zone -> {
                    var zoned = instant.atZone(zone);
                    return LocaleProperty.literal(zone.getId() + "  " + zoned.getOffset().getId(),
                                    locale -> formatter.localizedBy(locale).format(zoned),
                                    locale -> JavaCode.withLocale(locale,
                                            "var instant = Instant.parse(" + JavaCode.string(instant.toString()) + ");",
                                            "DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.LONG)",
                                            "        .localizedBy(locale)",
                                            "        .format(instant.atZone(ZoneId.of(" + JavaCode.string(zone.getId()) + ")));"))
                            .withHighlight(_ -> isUnusual(zoned.getOffset()));
                })
                .toList();
    }

    /// Returns the long generic name of a zone, for lists.
    ///
    /// @param zone   the zone
    /// @param locale the display locale
    /// @return the name
    public static String displayName(ZoneId zone, Locale locale) {
        return zone.getDisplayName(TextStyle.FULL, locale);
    }
}
