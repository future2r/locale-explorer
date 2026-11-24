package name.ulbricht.locale.explorer.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import name.ulbricht.locale.explorer.text.Fact;

class TimeZonesTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Locale DE = Locale.forLanguageTag("de-DE");

    @Test
    void namesShowWinterAndSummer() {
        var rows = TimeZones.nameRows(BERLIN, 2026);
        assertEquals("MEZ  /  MESZ", rows.getFirst().value().apply(DE));
        assertEquals("Mitteleuropäische Normalzeit  /  Mitteleuropäische Sommerzeit", rows.get(1).value().apply(DE));
        assertEquals("Europe/Berlin", rows.get(6).value().apply(DE));
    }

    @Test
    void rulesAndTransitions() {
        var instant = Instant.parse("2026-11-24T12:00:00Z");
        var facts = TimeZones.ruleFacts(BERLIN, instant);
        assertEquals("+01:00", value(facts, "zone.rule.offset"));
        assertEquals("false", value(facts, "zone.rule.daylightSavings"));
        assertEquals("", value(facts, "zone.rule.shortId"));
        assertEquals("ECT", TimeZones.shortIds(ZoneId.of("Europe/Paris")));
        assertEquals("", value(TimeZones.ruleFacts(ZoneId.of("+05:30"), instant), "zone.rule.version"));
        assertEquals("", value(TimeZones.ruleFacts(ZoneId.of("UTC+01:00"), instant), "zone.rule.version"));

        var transitions = TimeZones.transitionFacts(BERLIN, instant, 2);
        assertEquals("zone.transition.gap", transitions.getFirst().labelKey());
        assertEquals("2027-03-28T02:00", transitions.getFirst().labelArgs().getFirst());
        assertEquals("+01:00 → +02:00  (PT1H)", transitions.getFirst().value());
        assertEquals("zone.transition.overlap", transitions.get(1).labelKey());
        assertTrue(transitions.get(1).code().contains(
                "nextTransition(Instant.parse(\"" + TimeZones.nextTransitions(BERLIN, instant, 1).getFirst().getInstant() + "\"))"),
                transitions.get(1).code());

        var examples = TimeZones.examples(BERLIN, instant);
        assertEquals(LocalDateTime.of(2027, 3, 28, 2, 30), examples.getFirst().dateTime());
        assertTrue(examples.getFirst().gap());
        assertFalse(examples.get(1).gap());
    }

    @Test
    void resolution() {
        var gap = TimeZones.resolutionFacts(LocalDateTime.of(2027, 3, 28, 2, 30), BERLIN);
        assertEquals("zone.kind.gap", gap.stream().filter(fact -> fact.labelKey().equals("zone.res.kind")).findFirst().orElseThrow().valueKey());
        assertEquals("", value(gap, "zone.res.validOffsets"));
        assertEquals("2027-03-28T03:30+02:00[Europe/Berlin]", value(gap, "zone.res.of"));

        var overlap = TimeZones.resolutionFacts(LocalDateTime.of(2027, 10, 31, 2, 30), BERLIN);
        assertEquals("+02:00, +01:00", value(overlap, "zone.res.validOffsets"));
        assertEquals("2027-10-31T02:30+01:00[Europe/Berlin]", value(overlap, "zone.res.later"));
    }

    @Test
    void classification() {
        assertTrue(TimeZones.hasDaylightSaving(BERLIN, 2026));
        assertFalse(TimeZones.hasDaylightSaving(ZoneId.of("Asia/Kolkata"), 2026));
        assertTrue(TimeZones.hasDaylightSaving(ZoneId.of("Africa/Casablanca"), 2026));
        assertTrue(TimeZones.hasDaylightSaving(ZoneId.of("Australia/Sydney"), 2026));
        assertFalse(TimeZones.hasDaylightSaving(ZoneOffset.ofHours(2), 2026));
        assertTrue(TimeZones.isUnusual(ZoneOffset.ofHoursMinutes(5, 45)));
        assertFalse(TimeZones.isUnusual(ZoneOffset.ofHours(-5)));
    }

    @Test
    void worldClockHighlightsUnusualOffsets() {
        var rows = TimeZones.worldClockRows(List.of(BERLIN, ZoneId.of("Asia/Kathmandu")), Instant.parse("2026-11-24T12:00:00Z"));
        assertTrue(rows.getFirst().value().apply(DE).contains("13:00:00 MEZ"));
        assertFalse(rows.getFirst().highlight().test(DE));
        assertTrue(rows.get(1).highlight().test(DE));
    }

    private static String value(List<Fact> facts, String key) {
        return facts.stream().filter(fact -> fact.labelKey().equals(key)).findFirst().orElseThrow().value();
    }
}
